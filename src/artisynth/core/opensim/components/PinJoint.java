package artisynth.core.opensim.components;

import java.util.ArrayList;

import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.opensim.components.Coordinate.MotionType;
import maspack.matrix.RigidTransform3d;

public class PinJoint extends JointBase {

   @Override
   public PinJoint clone () {
      return (PinJoint)super.clone ();
   }

   @Override
   public artisynth.core.mechmodels.JointBase createJoint (
      RigidBody parent, RigidTransform3d TJP, 
      RigidBody child, RigidTransform3d TJC) {

      HingeJoint rj = new HingeJoint (
         child, getJointTransformInChild(), parent, getJointTransformInParent());
      
      // get coordinate and set values
      ArrayList<Coordinate> cs = getCoordinateArray ();
      if (cs != null && cs.size () > 0) {
         
         // find first rotational coordinate
         Coordinate coord = cs.get (0);
         for (Coordinate c : cs) {
            if (c.getMotionType () == MotionType.ROTATIONAL) {
               coord = c;
               break;
            }
         }
         
         // set angle and bounds
         if (coord.getClamped ()) {
            rj.setThetaRange (coord.getRange ());
         }
         rj.setTheta (coord.getDefaultValue ());
      }
      
      rj.setName (getName());
      
      return rj;
   }
   
}
