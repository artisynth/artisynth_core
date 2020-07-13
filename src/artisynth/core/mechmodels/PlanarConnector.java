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
import maspack.spatialmotion.PlanarCoupling;
import maspack.spatialmotion.RigidBodyConstraint;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.StructureChangeEvent;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class PlanarConnector extends BodyConnector 
   implements CopyableComponent {
   private double myPlaneSize;
   private static final double defaultPlaneSize = 1;

   private Point3d[] myRenderVtxs;
   private float[] myRenderCoords = new float[3];

   public static PropertyList myProps =
      new PropertyList (PlanarConnector.class, BodyConnector.class);

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createPointFaceProps (null);
      props.setFaceStyle (Renderer.FaceStyle.FRONT_AND_BACK);
      return props;
   }

   protected static VectorNd ZERO_VEC = new VectorNd(1);

   static {
      myProps.addReadOnly (
         "activation getPlanarActivation", "activation of planar constraint");
      myProps.add (
         "unilateral isUnilateral *", "unilateral constraint flag", false);
      myProps.add ("planeSize * *", "renderable size of the plane", null);
      myProps.add (
         "renderProps * *", "renderer properties", defaultRenderProps (null));
      myProps.add (
         "compliance", "compliance for each constraint", ZERO_VEC);
      myProps.add (
         "damping", "damping for each constraint", ZERO_VEC);
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

   public double getPlaneSize() {
      return myPlaneSize;
   }

   public void setPlaneSize (double len) {
      myPlaneSize = len;
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
      myCoupling = new PlanarCoupling ();
      myCoupling.setBreakSpeed (1e-8);
      myCoupling.setBreakAccel (1e-8);
      myCoupling.setContactDistance (1e-8);
   }
   public RigidBodyConstraint getConstraint() {
      return myCoupling.getConstraint (0); // only one constraint for a PlanarConnector
   }

   public PlanarConnector() {
      myTransformDGeometryOnly = true;
      myRenderVtxs = new Point3d[4];
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i] = new Point3d();
      }
      setDefaultValues();
      initializeCoupling();
   }

   public PlanarConnector (RigidBody bodyA, Vector3d pCA, RigidBody bodyB,
   RigidTransform3d XPB) {
      this();
      set (bodyA, pCA, bodyB, XPB);
   }

   public PlanarConnector (RigidBody bodyA, Vector3d pCA, RigidTransform3d XPW) {
      this();
      set (bodyA, pCA, XPW);
   }
   
   public void set(RigidBody bodyA, Point3d pCA, Vector3d worldPlaneNormal) {
      RigidTransform3d XPW = new RigidTransform3d ();
      Point3d pCW = new Point3d();
      pCW.transform (bodyA.getPose (), pCA);
      XPW.p.set(pCW);
      XPW.R.setZDirection (worldPlaneNormal);
      set (bodyA, pCA, XPW);
   }

   /**
    * Sets this PlanarConnectorX to connect two rigid bodies. The first body (A)
    * is the one in which the contact point is fixed, while the second body (B)
    * is the one in which the plane is fixed.
    * 
    * @param bodyA
    * first rigid body
    * @param pCA
    * location of contact point relative to body A
    * @param bodyB
    * second rigid body
    * @param XPB
    * plane coordinate frame with respect to body B. The plane normal is given
    * by the z axis of this frame, and the plane's origin is given by XPB.p
    */
   public void set (
      RigidBody bodyA, Vector3d pCA, RigidBody bodyB, RigidTransform3d XPB) {
      RigidTransform3d TCA = new RigidTransform3d();
      TCA.p.set (pCA);
      setBodies (bodyA, TCA, bodyB, XPB);
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
    * @param XPW
    * plane coordinate frame with respect to the world. The plane normal is
    * given by the z axis of this frame, and the plane's origin is given by
    * XPB.p
    */
   public void set (RigidBody bodyA, Vector3d pCA, RigidTransform3d XPW) {
      RigidTransform3d TCA = new RigidTransform3d();
      TCA.p.set (pCA);
      setBodies (bodyA, TCA, null, XPW);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      computeRenderVtxs (getCurrentTDW());
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i].updateBounds (pmin, pmax);
      }
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void prerender (RenderList list) {
      RigidTransform3d TCW = getCurrentTCW();
      myRenderCoords[0] = (float)TCW.p.x;
      myRenderCoords[1] = (float)TCW.p.y;
      myRenderCoords[2] = (float)TCW.p.z;
   }

   public void render (Renderer renderer, int flags) {
      Vector3d nrm = new Vector3d (0, 0, 1);
      RigidTransform3d TDW = getCurrentTDW();

      computeRenderVtxs (TDW);
      nrm.transform (TDW);
      
      RenderProps props = myRenderProps;

      Shading savedShading = renderer.setPropsShading (props);
      renderer.setFaceColoring (props, isSelected());
      renderer.setFaceStyle (props.getFaceStyle());

      renderer.beginDraw (DrawMode.TRIANGLE_STRIP);
      renderer.setNormal (nrm.x, nrm.y, nrm.z);
      renderer.addVertex (myRenderVtxs[3]);
      renderer.addVertex (myRenderVtxs[0]);
      renderer.addVertex (myRenderVtxs[2]);
      renderer.addVertex (myRenderVtxs[1]);
      renderer.endDraw();

      renderer.setShading (savedShading);
      renderer.setFaceStyle (FaceStyle.FRONT); // set default
      renderer.drawPoint (myRenderProps, myRenderCoords, isSelected());
   }

   protected void computeRenderVtxs (RigidTransform3d TDW) {
      RotationMatrix3d RPD = new RotationMatrix3d();
      RPD.setZDirection (((PlanarCoupling)myCoupling).getNormal());
      myRenderVtxs[0].set (myPlaneSize / 2, myPlaneSize / 2, 0);
      myRenderVtxs[1].set (-myPlaneSize / 2, myPlaneSize / 2, 0);
      myRenderVtxs[2].set (-myPlaneSize / 2, -myPlaneSize / 2, 0);
      myRenderVtxs[3].set (myPlaneSize / 2, -myPlaneSize / 2, 0);
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i].transform (RPD);
         myRenderVtxs[i].transform (TDW);
      }
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myPlaneSize *= s;
      myRenderProps.scaleDistance (s);
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
         // myUnilateralP[0] = unilateral;
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
      copy.setRenderProps (getRenderProps());
      //copy.setBodies (copy.myBodyA, getTCA(), copy.myBodyB, getTDB());
      return copy;
   }

}
