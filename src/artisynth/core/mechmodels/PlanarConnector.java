/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.Map;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderable;
import maspack.spatialmotion.PlanarCoupling;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.StructureChangeEvent;

/**
 * Implements a 5 DOF connector in which the origin of C is constrained
 * to lie on the x-y plane of D, and C is otherwise free to rotate.
 */
public class PlanarConnector extends BodyConnector 
   implements CopyableComponent {
   private double myPlaneSize;
   private static final double defaultPlaneSize = 1;

   public static PropertyList myProps =
      new PropertyList (PlanarConnector.class, BodyConnector.class);

   protected static VectorNd ZERO_VEC1 = new VectorNd(1);   

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createRenderProps (host);
      props.setFaceStyle (Renderer.FaceStyle.FRONT_AND_BACK);
      return props;
   }

   static {
      myProps.addReadOnly (
         "activation getPlanarActivation", "activation of planar constraint");
      myProps.add (
         "unilateral isUnilateral *", "unilateral constraint flag", false);
      myProps.add ("planeSize * *", "renderable size of the plane", null);
      myProps.addReadOnly (
         "engaged", "true if the coupling's constraint engaged");
      myProps.get ("renderProps").setDefaultValue (defaultRenderProps(null));
      myProps.get ("compliance").setDefaultValue (ZERO_VEC1);
      myProps.get ("damping").setDefaultValue (ZERO_VEC1);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Vector3d getNormal() {
      return ((PlanarCoupling)myCoupling).getNormal();
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      myPlaneSize = defaultPlaneSize;
      setRenderProps (defaultRenderProps (null));
   }

   /**
    * Queries the size used to render this connector's plane as a square.
    *
    * @return size used to render the plane
    */
   public double getPlaneSize() {
      return myPlaneSize;
   }

   /**
    * Sets the size used to render this connector's plane as a square.
    *
    * @param size used to render the plane
    */
   public void setPlaneSize (double size) {
      myPlaneSize = size;
   }

   /**
    * Gets the current plane normal, in world coordinates. Also
    * return the plane offset.
    * 
    * @param nrm returns the plane normal
    */
   public double getPlaneNormal (Vector3d nrm) {
      RigidTransform3d TDW = new RigidTransform3d();
      getCurrentTDW (TDW);
      nrm.transform (TDW, ((PlanarCoupling)myCoupling).getNormal());
      return nrm.dot (TDW.p);
   }
   
   private void initializeCoupling() {
      setCoupling (new PlanarCoupling());
   }

   public int getEngaged() {
      return myCoupling.getConstraint(0).getEngaged();
   }

   /**
    * Creates a {@code PlanarConnector} which is not attached to any bodies.
    * It can subsequently be connected using one of the {@code setBodies} or
    * {@code set} methods.
    */
   public PlanarConnector() {
      myTransformDGeometryOnly = true;
      setDefaultValues();
      initializeCoupling();
   }

   /**
    * Creates a {@code PlanarConnector} connecting two rigid bodies, {@code
    * bodyA} and {@code bodyB}. If A and B describe the coordinate frames of
    * {@code bodyA} and {@code bodyB}, and then {@code pCA} gives the origin of
    * C with respect to A and {@code TDB} gives the pose of D with respect to
    * B.
    *
    * @param bodyA rigid body A
    * @param pCA origin of C with respect to A, as seen in A
    * @param bodyB rigid body B (or {@code null})
    * @param TDB transform from frame D to body frame B
    */
   public PlanarConnector (
      RigidBody bodyA, Vector3d pCA, RigidBody bodyB, RigidTransform3d TDB) {
      this();
      set (bodyA, pCA, bodyB, TDB);
   }

   /**
    * Creates a {@code PlanarConnector} connecting a single rigid body, {@code
    * bodyA}, to ground. If A describes the coordinate frame of {@code bodyA},
    * then {@code pCA} gives the origin of C with respect to A and {@code TDW}
    * gives the pose of D with respect to world.
    *
    * @param bodyA rigid body A
    * @param pCA origin of C with respect to A, as seen in A
    * @param TDW transform from frame D to world coordinates
    */
   public PlanarConnector (RigidBody bodyA, Vector3d pCA, RigidTransform3d TDW) {
      this();
      set (bodyA, pCA, TDW);
   }
   
   /**
    * Creates a {@code PlanarConnector} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from connector frames D and C to world
    */
   public PlanarConnector (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TDW);
   }   

   /**
    * Sets this PlanarConnectorX to connect two rigid bodies. The first body
    * (A) is the one in which the contact point is fixed, while the second body
    * (B) is the one in which the plane is fixed.
    * 
    * @param bodyA
    * first rigid body
    * @param pCA
    * location of contact point relative to body A
    * @param bodyB
    * second rigid body
    * @param TDB
    * plane coordinate frame with respect to body B. The plane normal is given
    * by the z axis of this frame, and the plane's origin is given by TDB.p
    */
   public void set (
      RigidBody bodyA, Vector3d pCA, RigidBody bodyB, RigidTransform3d TDB) {
      RigidTransform3d TCA = new RigidTransform3d();
      TCA.p.set (pCA);
      setBodies (bodyA, TCA, bodyB, TDB);
   }

   /**
    * Sets this PlanarConnectorX to connect a rigid body with the world frame.
    * The contact point is fixed in the body frame, while the plane is fixed in
    * the world frame.
    * 
    * @param bodyA
    * rigid body
    * @param pCA
    * location of contact point relative to body
    * @param TDW
    * plane coordinate frame with respect to the world. The plane normal is
    * given by the z axis of this frame, and the plane's origin is given by
    * TDB.p
    */
   public void set (RigidBody bodyA, Vector3d pCA, RigidTransform3d TDW) {
      RigidTransform3d TCA = new RigidTransform3d();
      TCA.p.set (pCA);
      setBodies (bodyA, TCA, null, TDW);
   }

   public void set (RigidBody bodyA, Point3d pCA, Vector3d worldPlaneNormal) {
      RigidTransform3d TDW = new RigidTransform3d ();
      Point3d pCW = new Point3d();
      pCW.transform (bodyA.getPose (), pCA);
      TDW.p.set(pCW);
      TDW.R.setZDirection (worldPlaneNormal);
      set (bodyA, pCA, TDW);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      super.updateBounds (pmin, pmax);
      updateXYSquareBounds (pmin, pmax, getCurrentTDW(), myPlaneSize);
      Vector3d p = getCurrentTDW().p;
      p.updateBounds (pmin, pmax);
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   // public void prerender (RenderList list) {
   //    super.prerender (list);
   //    RigidTransform3d TCW = getCurrentTCW();
   //    myRenderCoords[0] = (float)TCW.p.x;
   //    myRenderCoords[1] = (float)TCW.p.y;
   //    myRenderCoords[2] = (float)TCW.p.z;
   // }

   public static void updateXYSquareBounds (
      Vector3d pmin, Vector3d pmax, RigidTransform3d TDW, double size) {

      if (size > 0) {
         // create square verticea and transform them to world
         Point3d vtx0 = new Point3d ( size/2,  size/2, 0);
         Point3d vtx1 = new Point3d (-size/2,  size/2, 0);
         Point3d vtx2 = new Point3d (-size/2, -size/2, 0);
         Point3d vtx3 = new Point3d ( size/2, -size/2, 0);
         vtx0.transform (TDW);
         vtx1.transform (TDW);
         vtx2.transform (TDW);
         vtx3.transform (TDW);
         
         // use the verties to update bounds
         vtx0.updateBounds (pmin, pmax);
         vtx1.updateBounds (pmin, pmax);
         vtx2.updateBounds (pmin, pmax);
         vtx3.updateBounds (pmin, pmax);
      }
   }

   public static void renderXYSquare (
      Renderer renderer, RenderProps props,
      RigidTransform3d TDW, double size, boolean selected) {
      
      if (size > 0) {
         // save and set render properties
         Shading savedShading = renderer.setPropsShading (props);
         FaceStyle savedFaceStyle = renderer.getFaceStyle ();
         renderer.setFaceStyle (props.getFaceStyle());
         renderer.setFaceColoring (props, selected);
         
         // create square verticea and transform them to world
         Point3d vtx0 = new Point3d ( size/2,  size/2, 0);
         Point3d vtx1 = new Point3d (-size/2,  size/2, 0);
         Point3d vtx2 = new Point3d (-size/2, -size/2, 0);
         Point3d vtx3 = new Point3d ( size/2, -size/2, 0);
         vtx0.transform (TDW);
         vtx1.transform (TDW);
         vtx2.transform (TDW);
         vtx3.transform (TDW);
         
         // use the verties to draw a square:
         Vector3d nrm = new Vector3d (TDW.R.m02, TDW.R.m12, TDW.R.m22);
         renderer.beginDraw (DrawMode.TRIANGLE_STRIP);
         renderer.setNormal (nrm.x, nrm.y, nrm.z);
         renderer.addVertex (vtx1);
         renderer.addVertex (vtx2);
         renderer.addVertex (vtx0);
         renderer.addVertex (vtx3);
         renderer.endDraw();
         
         // restore render properties
         renderer.setShading (savedShading);
         renderer.setFaceStyle (savedFaceStyle);
      }
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      renderXYSquare (
         renderer, myRenderProps, myRenderFrameD, myPlaneSize, isSelected());
      Vector3d p = myRenderFrameC.p;
      float[] pnt = new float[] { (float)p.x, (float)p.y,(float) p.z };
      renderer.drawPoint (myRenderProps, pnt, isSelected());
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myPlaneSize *= s;
   }

   public double getPlanarActivation() {
      // planar connector is defined by only one constraint
      return super.getActivation (0);
   }

   public boolean isUnilateral() {
      return ((PlanarCoupling)myCoupling).isUnilateral();
   }

   public void setUnilateral (boolean unilateral) {
      if (isUnilateral() != unilateral) {
         ((PlanarCoupling)myCoupling).setUnilateral (unilateral);
         myStateVersion++;
         notifyParentOfChange (StructureChangeEvent.defaultEvent);
      }
   }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PlanarConnector copy = (PlanarConnector)super.copy (flags, copyMap);
      copy.initializeCoupling();
      copy.setPlaneSize (myPlaneSize);
      copy.setUnilateral (isUnilateral());
      return copy;
   }

}
