package artisynth.core.opensim.components;

import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.spatialmotion.*;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import maspack.properties.*;
import maspack.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.JointCoordinateHandle;
import artisynth.core.opensim.customjoint.OpenSimCustomJoint;

/**
 * Handle to obtain a specified coordinate value from a joint.
 */
public class CoordinateHandle implements HasProperties {
   String myName;
   JointCoordinateHandle myJCH;
   //artisynth.core.mechmodels.JointBase myJoint;
   //int myIdx;
   
   private static final double RTOD = 180.0/Math.PI;
   private static final double DTOR = Math.PI/180.0;

   public static PropertyList myProps =
      new PropertyList (CoordinateHandle.class);

   static {
      myProps.add (
         "value getValueDeg setValueDeg",
         "coordinate value", 0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public CoordinateHandle (String name, JointCoordinateHandle jch) {
      myName = name;
      myJCH = jch;
   }

   public String getName() {
      return myName;
   }

   public JointCoordinateHandle getJCH() {
      return myJCH;
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public double getValue() {
      return myJCH.getValue();
   }

   public void setValue (double value) {
      myJCH.setValue (value);
   }

   public double getValueDeg() {
      return myJCH.getValueDeg();
   }

   public void setValueDeg(double value) {
      myJCH.setValueDeg (value);
   }

   public Range getValueRange() {
      return myJCH.getValueRangeDeg();
   }

   public static CoordinateHandle createFromJointSet (
      String coordName, ModelComponentMap componentMap) {

      JointCoordinateHandle jch =
         createJCHFromJointSet (coordName, componentMap);
      if (jch == null) {
         System.out.println (
            "WARNING: cannot find joint coordinate " + coordName);
         return null;
      }
      return new CoordinateHandle (coordName, jch);
   }

   public static JointCoordinateHandle createJCHFromJoint (
      String coordName, JointBase joint, ModelComponentMap componentMap) {

      ModelComponent comp = componentMap.get (joint);
      if (!(comp instanceof artisynth.core.mechmodels.JointBase)) {
         System.out.println (
            "WARNING: joint component not found for "+joint.getName());
         return null;
      }
      artisynth.core.mechmodels.JointBase jointComp =
         (artisynth.core.mechmodels.JointBase)comp;
      int cidx = jointComp.getCoordinateIndex (coordName);
      if (cidx != -1) {
         return new JointCoordinateHandle (jointComp, cidx);
      }
      else {
         return null;
      }
   }

   public static JointCoordinateHandle createJCHFromJointSet (
      String coordName, ModelComponentMap componentMap) {

      JointSet joints = componentMap.getJointSet();
      for (JointBase joint : joints.objects()) {
         JointCoordinateHandle jch = createJCHFromJoint (
            coordName, joint, componentMap);
         if (jch != null) {
            return jch;
         }
      }
      System.out.println ("WARNING: cannot find joint coordinate " + coordName);
      return null;
   }

   public static JointCoordinateHandle createJCHFromBodySet (
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
         JointCoordinateHandle jch = createJCHFromJoint (
            coordName, joint, componentMap);
         if (jch != null) {
            return jch;
         }
      }
      System.out.println ("WARNING: cannot find joint coordinate " + coordName);
      return null;
   }

   public static JointCoordinateHandle createJCH (
      String path, OpenSimObject ref, ModelComponentMap componentMap) {

      int sidx = path.lastIndexOf ('/');
      if (sidx == -1) {
         if (componentMap.getJointSet() != null) {
            return createJCHFromJointSet (path, componentMap);
         }
         else if (componentMap.getBodySet() != null) {
            return createJCHFromBodySet (path, componentMap);
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

      JointCoordinateHandle jch = createJCHFromJoint (
         coordName, joint, componentMap);
      if (jch != null) {
         return jch;
      }
      else {
         System.out.println (
            "WARNING: coordinate "+coordName+" not found in joint "+jointPath);
         return null;
      }
   }

}
