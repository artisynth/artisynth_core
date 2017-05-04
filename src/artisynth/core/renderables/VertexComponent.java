/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScalableUnits;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.MeshBase;
import maspack.geometry.Vertex3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;

public class VertexComponent extends RenderableComponentBase implements 
   TransformableGeometry, ScalableUnits {
   Vertex3d myVertex;
   float[] myRenderCoords = new float[3];
   
   public static PropertyList myProps =
      new PropertyList (VertexComponent.class, ModelComponentBase.class);

   static {
      myProps.add ("renderProps * *", "render properties", null);
      myProps.add ("position * *", "position state", Point3d.ZERO, "%.8g");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public VertexComponent(Vertex3d vtx) {
      myVertex = vtx;
   }
   
   public Vertex3d getVertex() {
      return myVertex;
   }

   @Override
   public void prerender(RenderList list) {
      Point3d rp = myVertex.getWorldPoint ();
      myRenderCoords[0] = (float)rp.x;
      myRenderCoords[1] = (float)rp.y;
      myRenderCoords[2] = (float)rp.z;
   }
   
   @Override
   public void render(Renderer renderer, int flags) {
      
      RenderProps rprops = getRenderProps();
      if (rprops == null) {
         return;
      }
      
      if (rprops.isVisible()) {
         try {
            renderer.drawPoint (myRenderProps, myRenderCoords, isSelected());
         }
         catch (Exception e) {
            System.out.println ("WARNING: VertexComponent.render failed: "+e);
            System.out.println ("myRenderProps=" + myRenderProps);
            System.out.println ("myRenderCoords=" + myRenderCoords);
            System.out.println ("point=" + ComponentUtils.getPathName (this));
         }
      }
   }
   
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      MeshBase m = myVertex.getMesh();
      if (m == null || m.getMeshToWorld().isIdentity()) { 
         gtr.transformPnt (myVertex.pnt);
      } else {
         //  relative transform
         myVertex.pnt.transform(m.getMeshToWorld());
         gtr.transformPnt (myVertex.pnt);
         myVertex.pnt.inverseTransform(m.getMeshToWorld());  
      }
      notifyVertexPositionModified();
   }
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   @Override
   public void scaleDistance(double s) {
      myVertex.pnt.scale(s);
      notifyVertexPositionModified();
   }

   @Override
   public void scaleMass(double s) {
      // nothing
   }

   public float[] getRenderCoords() {
      return myRenderCoords;
   }
   
   @Override
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      myVertex.getWorldPoint().updateBounds(pmin, pmax);
   }
   
   public Point3d getPosition() {
      return myVertex.getWorldPoint();
   }
   
   public void setPosition(Point3d pnt) {
      
      MeshBase mesh = myVertex.getMesh();
      if (mesh == null || mesh.meshToWorldIsIdentity()) {
         myVertex.setPosition(pnt);
      } else {
         Point3d pos = new Point3d(pnt);
         pos.inverseTransform (mesh.XMeshToWorld);
         myVertex.setPosition(pos);
      }
      notifyVertexPositionModified();
   }

   private void notifyVertexPositionModified () {
      if (myVertex.getMesh() != null) {
         myVertex.getMesh().notifyVertexPositionsModified();
      }
      if (getParent() instanceof VertexList) {
         ((VertexList)getParent()).invalidateRenderObject();
      }
   }

}
