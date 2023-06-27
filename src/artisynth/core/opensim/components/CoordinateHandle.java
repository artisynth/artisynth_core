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
         "value getNatValue setNatValue",
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

   public double getNatValue() {
      return myJCH.getNatValue();
   }

   public void setNatValue(double value) {
      myJCH.setNatValue (value);
   }

   public Range getValueRange() {
      return myJCH.getNatValueRange();
   }

   // public static CoordinateHandle createFromJoint (
   //    String coordName, JointBase joint, int cidx,
   //    ModelComponentMap componentMap) {


   //    CoordinateHandle handle = new CoordinateHandle();
   //    ModelComponent comp = componentMap.get (joint);
   //    if (!(comp instanceof artisynth.core.mechmodels.JointBase)) {
   //       System.out.println (
   //          "WARNING: joint component not found for "+joint.getName());
   //       return null;
   //    }
   //    handle.myJoint = (artisynth.core.mechmodels.JointBase)comp;
   //    if (handle.myJoint.numCoordinates() <= cidx) {
   //       System.out.println (
   //          "WARNING: joint component "+ handle.myJoint +
   //          " does not have coordinate "+cidx+" for name " + coordName);
   //       return null;
   //    }
   //    handle.myIdx = cidx;
   //    handle.myName = coordName;
   //    return handle;
   // }

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
      String coordName, JointBase joint, int cidx,
      ModelComponentMap componentMap) {

      JointCoordinateHandle handle = new JointCoordinateHandle();
      ModelComponent comp = componentMap.get (joint);
      if (!(comp instanceof artisynth.core.mechmodels.JointBase)) {
         System.out.println (
            "WARNING: joint component not found for "+joint.getName());
         return null;
      }
      artisynth.core.mechmodels.JointBase jointComp =
         (artisynth.core.mechmodels.JointBase)comp;
      if (jointComp.numCoordinates() <= cidx) {
         System.out.println (
            "WARNING: joint component "+ jointComp +
            " does not have coordinate "+cidx+" for name " + coordName);
         return null;
      }
      //handle.myName = coordName;      
      return new JointCoordinateHandle (jointComp, cidx);
   }

   public static JointCoordinateHandle createJCHFromJointSet (
      String coordName, ModelComponentMap componentMap) {

      JointSet joints = componentMap.getJointSet();
      for (JointBase joint : joints.objects()) {
         int cidx = 0;
         for (Coordinate c : joint.getCoordinateArray()) {
            if (c.getName().equals (coordName)) {
               return createJCHFromJoint (coordName, joint, cidx, componentMap);
            }
            cidx++;
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
         int cidx = 0;
         for (Coordinate c : joint.getCoordinateArray()) {
            if (c.getName().equals (coordName)) {
               return createJCHFromJoint (coordName, joint, cidx, componentMap);
            }
            cidx++;
         }
      }
      System.out.println ("WARNING: cannot find joint coordinate " + coordName);
      return null;
   }

   public static JointCoordinateHandle createJCH (
      String path, OpenSimObject ref, ModelComponentMap componentMap) {
      JointCoordinateHandle handle = new JointCoordinateHandle();
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
      ModelComponent comp = componentMap.get (joint);
      if (!(comp instanceof artisynth.core.mechmodels.JointBase)) {
         System.out.println (
            "WARNING: joint component not found for "+joint.getName());
         return null;
      }
      int cidx = 0;
      for (Coordinate c : joint.getCoordinateArray()) {
         if (c.getName().equals (coordName)) {
            artisynth.core.mechmodels.JointBase jointComp =
               (artisynth.core.mechmodels.JointBase)comp;
            if (jointComp.numCoordinates() <= cidx) {
               System.out.println (
                  "WARNING: joint component "+ jointComp +
                  " does not have coordinate "+cidx+" for name " + coordName);
               return null;
            }
            return new JointCoordinateHandle (jointComp, cidx);
         }
         cidx++;
      }
      System.out.println (
         "WARNING: coordinate "+coordName+" not found in joint "+jointPath);
      return null;
   }

}
