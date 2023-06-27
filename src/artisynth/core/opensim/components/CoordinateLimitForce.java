package artisynth.core.opensim.components;

import java.io.*;

import artisynth.core.modelbase.ModelComponent;
import artisynth.core.mechmodels.JointLimitForce;
import artisynth.core.mechmodels.JointCoordinateHandle;
import maspack.util.DoubleInterval;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;

public class CoordinateLimitForce extends ForceBase {

   String coordinate;
   double upper_stiffness;
   double upper_limit;
   double lower_stiffness;
   double lower_limit;
   double damping;
   double transition;
   
   public CoordinateLimitForce() {
      coordinate = null;
   }
   
   public void setUpperLimit (double lim) {
      upper_limit = lim;
   }
   
   public double getUpperLimit () {
      return upper_limit;
   }
   
   public void setUpperStiffness (double k) {
      upper_stiffness = k;
   }
   
   public double getUpperStiffness () {
      return upper_stiffness;
   }
   
   public void setLowerLimit (double lim) {
      lower_limit = lim;
   }
   
   public double getLowerLimit () {
      return lower_limit;
   }
   
   public void setLowerStiffness (double k) {
      lower_stiffness = k;
   }
   
   public double getLowerStiffness () {
      return lower_stiffness;
   }
   
   public void setDamping (double d) {
      damping = d;
   }
   
   public double getDamping () {
      return damping;
   }
   
   public void setTransition (double d) {
      transition = d;
   }
   
   public double getTransition () {
      return transition;
   }
   
   public String getCoordinate() {
      return coordinate;
   }
   
   public void setCoordinate(String coord) {
      coordinate = coord;
   }

   public JointLimitForce createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      JointCoordinateHandle ch =
         CoordinateHandle.createJCH (coordinate, this, componentMap);
      if (ch == null) {
         return null;
      }
      double kupper = upper_stiffness;
      double klower = lower_stiffness;
      double dupper = damping;
      double dlower = dupper;

      // adjust upper and lower damping to reflect proper critcal damping
      if (kupper != 0 && klower != 0) {
         if (kupper > klower) {
            dlower *= Math.sqrt(klower/kupper);
         }
         else if (klower > kupper) {
            dupper *= Math.sqrt(kupper/klower);            
         }
      }    
      JointLimitForce jlf = new JointLimitForce (getName(), ch);
      jlf.setUpper (upper_limit, kupper, dupper, transition);
      jlf.setLower (lower_limit, klower, dlower, transition);
      return jlf;
   }

   @Override
   public CoordinateLimitForce clone () {
      CoordinateLimitForce cpp = (CoordinateLimitForce)super.clone ();
      cpp.setCoordinate (coordinate);
      return cpp;
   }
}
