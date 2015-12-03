/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Vertex3d;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.GLRenderer;
import maspack.render.RenderProps;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.Collidable.Collidability;

public class RigidMeshComp extends MeshComponent 
   implements PointAttachable, HasSurfaceMesh, CollidableBody {

   public static boolean DEFAULT_PHYSICAL = true;

   protected boolean physical = DEFAULT_PHYSICAL;

   protected static final Collidability DEFAULT_COLLIDABILITY =
      Collidability.ALL;   
   protected Collidability myCollidability = DEFAULT_COLLIDABILITY;

   public static PropertyList myProps = new PropertyList(
      RigidMeshComp.class, MeshComponent.class);

   static {
      myProps.add("physical isPhysical setPhysical", "", DEFAULT_PHYSICAL);
      myProps.add (
         "collidable", 
         "sets the collidability of the mesh", DEFAULT_COLLIDABILITY);
   }
   
   public RigidMeshComp() {
      super();
      physical = DEFAULT_PHYSICAL;
   }
   
   public RigidMeshComp(String name) {
      this();
      setName(name);
   }
   
   public RigidMeshComp (
      MeshBase mesh, String fileName, AffineTransform3dBase X) {
      this();
      setMesh (mesh, fileName, X);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public boolean isPhysical() {
      return physical;
   }

   public void setPhysical(boolean set) {
      physical = set;
   }
   
   @Override
   public void render (GLRenderer renderer, RenderProps props, int flags) {

      if (renderer.isSelecting()) {
         renderer.beginSelectionQuery (0);
      }
      super.render (renderer, props, flags);
      if (renderer.isSelecting()) {
         renderer.endSelectionQuery ();
      }
   }
   
   public RigidBody getRigidBody() {
      CompositeComponent gp = getGrandParent();
      if (gp instanceof RigidBody) {
         return (RigidBody)gp;
      }
      return null;
   }
   
   @Override
   public RigidMeshComp copy(int flags,
      Map<ModelComponent,ModelComponent> copyMap) {

      RigidMeshComp rmc = (RigidMeshComp)super.copy(flags, copyMap);
      rmc.physical = physical;
      
      return rmc;
   }
   
   // Pullable interface
   private static class PullableOrigin {
      RigidBody rb;
      Point3d bodyPnt;
      public PullableOrigin(RigidBody rb, Point3d pnt) {
         this.rb = rb;
         this.bodyPnt = new Point3d(pnt);
         this.bodyPnt.inverseTransform(rb.getPose());
      }
   }
   
//   @Override
//   public boolean isPullable() {
//      MeshBase mb = getMesh();
//      if (mb instanceof PolygonalMesh) {
//         CompositeComponent gp = getGrandParent();
//         if (gp instanceof RigidBody) {
//            return true;
//         }
//      }
//      return false;
//   }
//
//   @Override
//   public Object getOriginData (Point3d origin, Vector3d dir) {
//
//      RigidBody rb = (RigidBody)getGrandParent();
//      PullableOrigin data = null;
//
//      Point3d pnt = BVFeatureQuery.nearestPointAlongRay (
//         (PolygonalMesh)getMesh(), origin, dir);
//      if (pnt != null) {
//         data = new PullableOrigin(rb, pnt);
//      }
//      return data;
//   }
//
//   @Override
//   public Point3d getOriginPoint(Object data) {
//      
//      PullableOrigin orig = (PullableOrigin)data;
//      Point3d pnt = new Point3d(orig.bodyPnt);
//      pnt.transform (orig.rb.getPose());
//      return pnt;
//   }
//
//   @Override
//   public double getPointRenderRadius() {
//      return 0;
//   }
//
//   @Override
//   public void applyForce(Object orig, Vector3d force) {
//      PullableOrigin origin = (PullableOrigin)orig;
//      origin.rb.applyForce(origin.bodyPnt, force);
//   }
//   

   @Override
   public int numSelectionQueriesNeeded() {
      return 1;   // trigger so we can add a rigid body
   }
   
   @Override
   public void getSelection(LinkedList<Object> list, int qid) {
      CompositeComponent gp = getGrandParent();
      if (gp instanceof RigidBody) {
         list.addLast(getGrandParent());
      }
      list.addLast(this);
   }
   
   @Override
   public PolygonalMesh getCollisionMesh () {
      MeshBase mesh = getMesh();
      if (mesh instanceof PolygonalMesh) {
         return (PolygonalMesh)mesh;
      }
      return null;
   }

   @Override
   public double getMass () {
      return getRigidBody().getMass();
   }

   @Override
   public Collidability getCollidable () {
      getSurfaceMesh(); // build surface mesh if necessary
      return myCollidability;
   }

   public void setCollidable (Collidability c) {
      if (myCollidability != c) {
         myCollidability = c;
         notifyParentOfChange (new StructureChangeEvent (this));
      }
   }

   @Override
   public boolean isDeformable () {
      RigidBody rb = getRigidBody();
      if (rb == null) {
         throw new IllegalStateException (
            "RigidMeshComp not associated with a rigid body");
      }
      return rb.getVelStateSize() > 6;
   }
   
   public void getVertexMasters (List<ContactMaster> mlist, Vertex3d vtx) {
      RigidBody rb = getRigidBody();
      if (rb == null) {
         throw new IllegalStateException (
            "RigidMeshComp not associated with a rigid body");
      }
      mlist.add (new ContactMaster (rb, 1));
   }
   
   public boolean containsContactMaster (CollidableDynamicComponent comp) {
      return comp == getRigidBody();    
   }   
   
//   public boolean requiresContactVertexInfo() {
//      return false;
//   }
   
   public boolean allowCollision (
      ContactPoint cpnt, Collidable other, Set<Vertex3d> attachedVertices) {
      return true;
   }

   public PointFrameAttachment createPointAttachment (Point pnt) {
      
      if (getGrandParent() instanceof RigidBody) {
         RigidBody rb = (RigidBody)getGrandParent();
         return rb.createPointAttachment (pnt);
      }
      else {
         return null;
      }
   }

   public PolygonalMesh getSurfaceMesh() {
      if (getMesh() instanceof PolygonalMesh) {
         return (PolygonalMesh)getMesh();
      }
      else {
         return null;
      }
   }
   
   public int numSurfaceMeshes() {
      return getSurfaceMesh() != null ? 1 : 0;
   }
   
   public PolygonalMesh[] getSurfaceMeshes() {
      return MeshComponent.createSurfaceMeshArray (getSurfaceMesh());
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      if ((flags & TransformableGeometry.TG_SIMULATING) == 0) {
         if (myMeshInfo.transformGeometryAndPose (gtr, null)) {
            if (myRenderProps != null) {
               myRenderProps.clearMeshDisplayList();
            }
         }
      }
      else {
         MeshBase mesh = myMeshInfo.getMesh();
         mesh.setMeshToWorld (getRigidBody().getPose());
      }
   }   
   
}


