/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.util.Map;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.spatialmotion.RollPitchCoupling;
import maspack.util.DoubleInterval;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class RollPitchJoint extends JointBase implements CopyableComponent {
  
   private static DoubleInterval DEFAULT_ANGLE_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private DoubleInterval myRollRange = new DoubleInterval(DEFAULT_ANGLE_RANGE);
   private DoubleInterval myPitchRange = new DoubleInterval(DEFAULT_ANGLE_RANGE);

   public static PropertyList myProps =
      new PropertyList (RollPitchJoint.class, JointBase.class);

   static {
      myProps.add (
         "roll", "joint roll angle (degrees)", 0, "%8.3f 1E [-360,360]");
      myProps.add (
         "pitch", "joint pitch angle (degrees)", 0, "%8.3f 1E [-360,360]");
      myProps.add (
         "rollRange", "range for roll", DEFAULT_ANGLE_RANGE, "%8.3f 1E");
      myProps.add (
         "pitchRange", "range for pitch", DEFAULT_ANGLE_RANGE, "%8.3f 1E");
      myProps.add (
         "compliance", "compliance for each constraint", ZERO_VEC);
      myProps.add (
         "damping", "damping for each constraint", ZERO_VEC);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public double[] getRollPitchRad() {
      RigidTransform3d TGD = null;
      if (attachmentsInitialized()) {
         // initialize TGD to TCD; it will get projected to TGD within
         TGD = new RigidTransform3d();
         getCurrentTCD (TGD);
      }            
      double[] angs = new double[2];
      ((RollPitchCoupling)myCoupling).getRollPitch (angs, TGD);
      return angs;
   }

   public void setRollPitchRad (double[] angs) {
      RigidTransform3d TGD = null;
      if (isConnectedToBodies()) {
         TGD = new RigidTransform3d();
      }      
      ((RollPitchCoupling)myCoupling).setRollPitch (TGD, angs);
      if (TGD != null) {
         // if we are connected to the hierarchy, adjust the poses of the
         // attached bodies appropriately.         
         adjustPoses (TGD);
      }
   }

   public RollPitchJoint() {
      setDefaultValues();
      myCoupling = new RollPitchCoupling ();
      myCoupling.setBreakSpeed (1e-8);
      myCoupling.setBreakAccel (1e-8);
      myCoupling.setContactDistance (1e-8);
   }

   public RollPitchJoint (RigidBody bodyA, RigidTransform3d TCA,
                          RigidTransform3d TDW) {
      this();
      setBodies (bodyA, TCA, null, TDW);
   }

   public RollPitchJoint (RigidBody bodyA, RigidTransform3d TCA,
                          RigidBody bodyB, RigidTransform3d XDB) {
      this();
      setBodies (bodyA, TCA, bodyB, XDB);
   }
   
   public RollPitchJoint (RigidBody bodyA, RigidBody bodyB, RigidTransform3d XJointWorld) {
      this();
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      
      TCA.mulInverseLeft(bodyA.getPose(), XJointWorld);
      XDB.mulInverseLeft(bodyB.getPose(), XJointWorld);
      
      setBodies(bodyA, TCA, bodyB, XDB);
      
   }
   
   public RollPitchJoint (ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TFW) {
      this();
      setBodies(bodyA, bodyB, TFW);
      
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      RigidTransform3d TFW = getCurrentTCW();
      TFW.p.updateBounds (pmin, pmax);
   }

   public double getRoll () {
      return Math.toDegrees (getRollPitchRad()[0]);
   }

   public void setRoll (double roll) {
      roll = myRollRange.makeValid (roll);
      double[] angs = getRollPitchRad();
      angs[0] = Math.toRadians (roll);
      setRollPitchRad (angs);
   }
   
   public DoubleInterval getRollRange () {
      return myRollRange;
   }

   public void setRollRange (DoubleInterval range) {
      RollPitchCoupling coupling = (RollPitchCoupling)myCoupling;
      coupling.setRollRange (
         Math.toRadians (range.getLowerBound()),
         Math.toRadians (range.getUpperBound()));
      myRollRange.set (range);
      if (isConnectedToBodies()) {
         // if we are connected to the hierarchy, might have to update theta
         double roll = getRoll();
         double clipped = myRollRange.clipToRange (roll);
         if (clipped != roll) {
            setRoll (clipped);
         }
      }      
   }

   public void setRollRange (double min, double max) {
      setRollRange (new DoubleInterval (min, max));
   }

   public double getPitch () {
      return Math.toDegrees (getRollPitchRad()[1]);
   }

   public void setPitch (double pitch) {
      pitch = myPitchRange.makeValid (pitch);
      double[] angs = getRollPitchRad();
      angs[1] = Math.toRadians (pitch);
      setRollPitchRad (angs);
   }

   public DoubleInterval getPitchRange () {
      return myPitchRange;
   }

   public void setPitchRange (DoubleInterval range) {
      RollPitchCoupling coupling = (RollPitchCoupling)myCoupling;
      coupling.setPitchRange (
         Math.toRadians (range.getLowerBound()),
         Math.toRadians (range.getUpperBound()));
      myPitchRange.set (range);
      if (isConnectedToBodies()) {
         // if we are connected to the hierarchy, might have to update theta
         double pitch = getPitch();
         double clipped = myPitchRange.clipToRange (pitch);
         if (clipped != pitch) {
            setPitch (clipped);
         }
      }      
   }

   public void setPitchRange (double min, double max) {
      setPitchRange (new DoubleInterval (min, max));
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      float[] coords =
         new float[] { (float)myRenderFrameD.p.x, (float)myRenderFrameD.p.y,
                      (float)myRenderFrameD.p.z };
      renderer.drawPoint (myRenderProps, coords, isSelected());
   }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      RollPitchJoint copy = (RollPitchJoint)super.copy (flags, copyMap);
      copy.myCoupling = new RollPitchCoupling ();
      copy.setAxisLength (myAxisLength);
      copy.setRenderProps (getRenderProps());
      //copy.setBodies (copy.myBodyA, getTCA(), copy.myBodyB, getTDB());
      return copy;
   }

}
