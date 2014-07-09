package artisynth.core.materials;

import maspack.properties.*;

public class SpringAxialMuscle extends LinearAxialMaterial {

   protected static double DEFAULT_MAX_FORCE = 1;

   protected double myMaxForce = DEFAULT_MAX_FORCE; // maxForce
   protected PropertyMode myMaxForceMode = PropertyMode.Inherited;

   public static PropertyList myProps =
      new PropertyList (SpringAxialMuscle.class, LinearAxialMaterial.class);


   static {
      myProps.addInheritable (
         "maxForce", "force at unit excitation", DEFAULT_MAX_FORCE );
   }

   public SpringAxialMuscle () {
      super();
   }

   public SpringAxialMuscle (double k, double d, double maxf) {
      super();
      // assume that maxForce = k * maxLength, and set maxLength accordingly
      setMaxForce (maxf);
      setStiffness (k);
      setDamping (d);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getMaxForce() {
      return myMaxForce;
   }

   public synchronized void setMaxForce (double E) {
      myMaxForce = E;
      myMaxForceMode =
         PropertyUtils.propagateValue (
            this, "maxForce", myMaxForce, myMaxForceMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getMaxForceMode() {
      return myMaxForceMode;
   }

   public void setMaxForceMode (PropertyMode mode) {
      myMaxForceMode =
         PropertyUtils.setModeAndUpdate (
            this, "maxForce", myMaxForceMode, mode);
   }

   public double computeF(double l, double ldot, double l0, double ex) {
      return myStiffness*(l-l0) + myMaxForce*ex + myDamping*ldot;
   }

   public double computeDFdl(double l, double ldot, double l0, double ex) {
      return myStiffness; 
   }

   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {
      return myDamping;
   }

   public boolean isDFdldotZero() {
      return myDamping == 0;
   }
}
