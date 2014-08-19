/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.LinkedList;
import java.util.Map;

import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.GLRenderer;
import maspack.render.MouseRayEvent;
import maspack.render.RenderProps;
import artisynth.core.gui.editorManager.EditorUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.workspace.PullController.Pullable;

public class RigidMesh extends MeshComponent implements Pullable, Collidable {

   public static boolean DEFAULT_PHYSICAL = true;
   protected boolean physical = DEFAULT_PHYSICAL;
   
   public static PropertyList myProps = new PropertyList(
      RigidMesh.class, MeshComponent.class);

   static {
      myProps.add("physical isPhysical setPhysical", "", DEFAULT_PHYSICAL);
   }
   
   public RigidMesh() {
      super();
      physical = DEFAULT_PHYSICAL;
   }
   
   public RigidMesh(String name) {
      this();
      setName(name);
   }
   
   public RigidMesh (
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
   public RigidMesh copy(int flags,
      Map<ModelComponent,ModelComponent> copyMap) {

      RigidMesh rmc = (RigidMesh)super.copy(flags, copyMap);
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
   
   @Override
   public boolean isPullable() {
      MeshBase mb = getMesh();
      if (mb instanceof PolygonalMesh) {
         CompositeComponent gp = getGrandParent();
         if (gp instanceof RigidBody) {
            return true;
         }
      }
      return false;
   }

   @Override
   public Object getOriginData(MouseRayEvent ray) {
      RigidBody rb = (RigidBody)getGrandParent();
      PullableOrigin origin = null;

      Point3d pnt = EditorUtils.intersectWithMesh ((PolygonalMesh)getMesh(), ray);
      if (pnt != null) {
         origin = new PullableOrigin(rb, pnt);
      }
      return origin;
   }

   @Override
   public Point3d getOriginPoint(Object data) {
      
      PullableOrigin orig = (PullableOrigin)data;
      Point3d pnt = new Point3d(orig.bodyPnt);
      pnt.transform (orig.rb.getPose());
      return pnt;
   }

   @Override
   public double getPointRenderRadius() {
      return 0;
   }

   @Override
   public void applyForce(Object orig, Vector3d force) {
      PullableOrigin origin = (PullableOrigin)orig;
      origin.rb.applyForce(origin.bodyPnt, force);
   }
   

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
   
   // Collidable implementation
   @Override
   public CollisionData createCollisionData () {
      RigidBody rb = getRigidBody();
      if (rb != null) {
         PolygonalMesh cmesh = getCollisionMesh ();
         if (cmesh != null) {
            return new RigidBodyCollisionData(rb, getCollisionMesh());
         }
      }
      return null;
   }

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
   public boolean isCollidable () {
      CollisionData cdata = createCollisionData ();
      if (cdata != null) {
         return true;
      }
      return false;
   }
   
}
