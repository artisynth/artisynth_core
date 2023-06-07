package artisynth.core.opensim.components;

import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.spatialmotion.*;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import maspack.properties.*;
import maspack.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.opensim.customjoint.OpenSimCustomJoint;

/**
 * Handle to obtain a specified coordinate value from a joint.
 */
public class CoordinateHandle implements HasProperties {
   String myName;
   artisynth.core.mechmodels.JointBase myJoint;
   int myIdx;
   
   private static final double RTOD = 180.0/Math.PI;
   private static final double DTOR = Math.PI/180.0;

   public static PropertyList myProps =
      new PropertyList (CoordinateHandle.class);

   static {
      myProps.add (
         "value getValueNat setValueNat",
         "coordinate value", 0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public String getName() {
      return myName;
   }

   public artisynth.core.mechmodels.JointBase getJoint() {
      return myJoint;
   }

   public int getIndex() {
      return myIdx;
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public double getValue() {
      return myJoint.getCoordinate (myIdx);
   }

   public void setValue (double value) {
      myJoint.setCoordinate (myIdx, value);
   }

   public Range getValueRange() {
      DoubleInterval range;
      if (myJoint.getCoordinateMotionType(myIdx) == MotionType.ROTARY) {
         range = myJoint.getCoordinateRangeDeg(myIdx);
         if (range.getLowerBound() < -180) {
            range.setLowerBound (-180);
         }
         if (range.getUpperBound() > 180) {
            range.setUpperBound (180);
         }
      }
      else {
         range = myJoint.getCoordinateRange(myIdx);
      }
      return range;
   }

   public double getValueNat() {
      double value = getValue();
      if (myJoint.getCoordinateMotionType(myIdx) == MotionType.ROTARY) {
         value *= RTOD;
      }
      return value;
   }

   public void setValueNat(double value) {
      if (myJoint.getCoordinateMotionType(myIdx) == MotionType.ROTARY) {
         value *= DTOR;
      }
      setValue (value);
   }

   public double getSpeed() {
      return myJoint.getCoordinateSpeed (myIdx);
   }

   public void applyForce (double f) {
      myJoint.applyCoordinateForce (myIdx, f);
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      myJoint.addCoordinateSolveBlocks (M, myIdx);
   }

   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      myJoint.addCoordinateVelJacobian (M, myIdx, s);
   }

   public MotionType getMotionType() {
      return myJoint.getCoordinateMotionType (myIdx);
   }

   public Wrench getWrench() {
      return myJoint.getCoupling().getCoordinateWrench (myIdx);
   }

   public static CoordinateHandle createFromJoint (
      String coordName, JointBase joint, int cidx,
      ModelComponentMap componentMap) {

      CoordinateHandle handle = new CoordinateHandle();
      ModelComponent comp = componentMap.get (joint);
      if (!(comp instanceof artisynth.core.mechmodels.JointBase)) {
         System.out.println (
            "WARNING: joint component not found for "+joint.getName());
         return null;
      }
      handle.myJoint = (artisynth.core.mechmodels.JointBase)comp;
      if (handle.myJoint.numCoordinates() <= cidx) {
         System.out.println (
            "WARNING: joint component "+ handle.myJoint +
            " does not have coordinate "+cidx+" for name " + coordName);
         return null;
      }
      handle.myIdx = cidx;
      handle.myName = coordName;
      return handle;
   }

   public static CoordinateHandle createFromJointSet (
      String coordName, ModelComponentMap componentMap) {

      JointSet joints = componentMap.getJointSet();
      for (JointBase joint : joints.objects()) {
         int cidx = 0;
         for (Coordinate c : joint.getCoordinateArray()) {
            if (c.getName().equals (coordName)) {
               return createFromJoint (coordName, joint, cidx, componentMap);
            }
            cidx++;
         }
      }
      System.out.println ("WARNING: cannot find joint coordinate " + coordName);
      return null;
   }

   public static CoordinateHandle createFromBodySet (
      String coordName, ModelComponentMap componentMap) {

      BodySet bodies = componentMap.getBodySet();
      for (Body body : bodies.objects()) {
         JointBase joint = null;
         if (body.getJoint() != null) {
            joint = body.getJoint().getJoint();
         }
         if (joint == null) {
            continue;
         }
         int cidx = 0;
         for (Coordinate c : joint.getCoordinateArray()) {
            if (c.getName().equals (coordName)) {
               return createFromJoint (coordName, joint, cidx, componentMap);
            }
            cidx++;
         }
      }
      System.out.println ("WARNING: cannot find joint coordinate " + coordName);
      return null;
   }

   public static CoordinateHandle create (
      String path, OpenSimObject ref, ModelComponentMap componentMap) {
      CoordinateHandle handle = new CoordinateHandle();
      int sidx = path.lastIndexOf ('/');
      if (sidx == -1) {
         if (componentMap.getJointSet() != null) {
            return createFromJointSet (path, componentMap);
         }
         else if (componentMap.getBodySet() != null) {
            return createFromBodySet (path, componentMap);
         }
         else {
            System.out.println ("WARNING: joint set not specified");
            return null;
         }
      }

      String coordName = path.substring (sidx+1, path.length());
      String jointPath = path.substring (0, sidx);
      
      JointBase joint = (JointBase)componentMap.findObjectByPathOrName (
         JointBase.class, ref, jointPath);
      if (joint == null) {
         System.out.println ("WARNING: can't find joint for "+jointPath);
         return null;
      }
      ModelComponent comp = componentMap.get (joint);
      if (!(comp instanceof artisynth.core.mechmodels.JointBase)) {
         System.out.println (
            "WARNING: joint component not found for "+joint.getName());
         return null;
      }
      int cidx = 0;
      for (Coordinate c : joint.getCoordinateArray()) {
         if (c.getName().equals (coordName)) {
            handle.myJoint = (artisynth.core.mechmodels.JointBase)comp;
            if (handle.myJoint.numCoordinates() <= cidx) {
               System.out.println (
                  "WARNING: joint component "+ handle.myJoint +
                  " does not have coordinate "+cidx+" for name " + coordName);
               return null;
            }
            handle.myIdx = cidx;
            handle.myName = coordName;
            return handle;
         }
         cidx++;
      }
      System.out.println (
         "WARNING: coordinate "+coordName+" not found in joint "+jointPath);
      return null;
   }
}
