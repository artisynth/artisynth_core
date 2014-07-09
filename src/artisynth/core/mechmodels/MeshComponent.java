/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Map;

import maspack.geometry.MeshBase;
import maspack.geometry.Vertex3d;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.properties.PropertyList;
import maspack.render.GLRenderer;
import maspack.render.MeshRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import artisynth.core.util.TransformableGeometry;

/**
 * Contains information about a mesh, including the mesh itself, and it's
 * possible file name and transformation with respect to the original file
 * definition.
 */
public class MeshComponent extends RenderableComponentBase
implements TransformableGeometry, ScalableUnits {

   protected MeshInfo myMeshInfo;

   public static PropertyList myProps = new PropertyList(
      MeshComponent.class, RenderableComponentBase.class);

   static {
      myProps.add(
         "renderProps * *", "render properties for this component",
         createDefaultRenderProps());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MeshComponent(String name) {
      this();
      setName(name);
   }

   public MeshComponent () {
      myMeshInfo = new MeshInfo();
   }

   public MeshComponent (
      MeshBase mesh, String fileName, AffineTransform3dBase X) {
      this();
      setMesh (mesh, fileName, X);
   }

   protected void setMeshFromInfo () {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setFixed (true);
      }
      if (myRenderProps != null) {
         myRenderProps.clearMeshDisplayList();
      }
   }

   public void setMesh(MeshBase mesh, String fileName, AffineTransform3dBase X) {
      myMeshInfo.set (mesh, fileName, X);
      setMeshFromInfo ();
   }

   public void setMesh(MeshBase mesh, String fileName) {
      setMesh (mesh, fileName, null);
   }

   public void setMesh(MeshBase mesh) {
      setMesh (mesh, null, null);
   }

   public Vertex3d getVertex (int idx) {
      return getMesh().getVertex (idx);
   }

   public int numVertices() {
      return myMeshInfo.numVertices();
   }

   public MeshBase getMesh() {
      // myMeshInfo will be null if getMesh() is called via setDefaultValues()
      // before construction has finished.
      if (myMeshInfo != null) {
         return myMeshInfo.getMesh();
      }
      else {
         return null;
      }
   }

   public boolean transformGeometry(
      AffineTransform3dBase X, RigidTransform3d Xpose,
      AffineTransform3d Xlocal) {
      return myMeshInfo.transformGeometry (X, Xpose, Xlocal);
   }

   public RenderProps createRenderProps() {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         RenderProps props = mesh.createRenderProps(this);
         return props;
      }
      else {
         return RenderProps.createMeshProps(this);
      }
   }

   private static RenderProps createDefaultRenderProps() {
      RenderProps mr = new MeshRenderProps();
      return mr;
   }

   public void setDefaultValues() {
      setRenderProps(createDefaultRenderProps());
   }

   @Override
   public void prerender(RenderList list) {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.saveRenderInfo();
         if (!mesh.isFixed() && mesh.myUseDisplayList) {
            mesh.clearDisplayList(myRenderProps);
         }
      }
   }

   public void render(
      GLRenderer renderer, RenderProps props, int flags) {
      myMeshInfo.render (renderer, props, flags);
   }

   @Override
   public void render(GLRenderer renderer, int flags) {
      if (isSelected() || isParentOrGrandParentSelected()) {
         flags |= GLRenderer.SELECTED;
      }
      render(renderer, getRenderProps(), flags);
   }

   protected boolean isParentOrGrandParentSelected() {
      CompositeComponent parent = getParent();
      if (parent != null) {
         if (parent.isSelected()) {
            return true;
         }
         parent = parent.getParent();
         if (parent != null) {
            return parent.isSelected();
         }
      }
      return false;
   }

   @Override
   public void updateBounds(Point3d pmin, Point3d pmax) {
      getMesh().updateBounds(pmin, pmax);
   }

   public void updatePosition (int flags) {
      MeshBase mesh = getMesh();
      mesh.notifyVertexPositionsModified();
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {

      // write mesh info first so that the mesh will be read before renderProps;
      // this is necessary because the mesh determines what type of renderProps
      // should be instantiated
      myMeshInfo.write (pw, fmt);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "mesh")) {
         myMeshInfo.scan (rtok);  
         setMeshFromInfo();
         return true;
      }
      rtok.pushBack();
      // scan properties
      return super.scanItem (rtok, tokens);
   }

   public void scaleDistance (double s) {
      myMeshInfo.myMesh.scale (s);      
   }

   public void scaleMass (double s) {
   }

   public void transformGeometry (AffineTransform3dBase X) {
      transformGeometry (X, this, 0);
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {

      if ((flags & TransformableGeometry.SIMULATING) != 0) {
         return;
      }
      myMeshInfo.transformGeometry (X);
      if (myRenderProps != null) {
         myRenderProps.clearMeshDisplayList();
      }
   }

   public void updateSlavePos () {
   }


   public MeshComponent copy(int flags, Map<ModelComponent,ModelComponent> copyMap) {
      MeshComponent comp = (MeshComponent)super.copy (flags, copyMap);

      comp.myMeshInfo = myMeshInfo.copy();

      return comp;
   }
   
}
