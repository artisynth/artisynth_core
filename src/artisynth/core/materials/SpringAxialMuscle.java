package artisynth.core.materials;

import maspack.properties.*;

public class SpringAxialMuscle extends LinearAxialMaterial {

   protected static double DEFAULT_MAX_FORCE = 1;

   protected double myMaxForce = DEFAULT_MAX_FORCE; // max force
   protected PropertyMode myMaxForceMode = PropertyMode.Inherited;

   public static PropertyList myProps =
      new PropertyList (SpringAxialMuscle.class, LinearAxialMaterial.class);

   static {
      myProps.addInheritable (
         "maxForce", "excitation force gain", DEFAULT_MAX_FORCE );
   }

   public SpringAxialMuscle () {
      super();
   }

   public SpringAxialMuscle (double k, double d, double maxf) {
      super();
      setStiffness (k);
      setDamping (d);
      setMaxForce (maxf);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getMaxForce() {
      return myMaxForce;
   }

   public synchronized void setMaxForce (double max) {
      myMaxForce = max;
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

   public double computeF (
      double l, double ldot, double l0, double ex) {
      return myStiffness*(l-l0) + myDamping*ldot + myMaxForce*ex;
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

   public boolean equals (AxialMaterial mat) {
      if (!(mat instanceof SpringAxialMuscle)) {
         return false;
      }
      SpringAxialMuscle sam = (SpringAxialMuscle)mat;
      if (myStiffness != sam.myStiffness ||
          myMaxForce != sam.myMaxForce ||
          myDamping != sam.myDamping) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public SpringAxialMuscle clone() {
      SpringAxialMuscle mat = (SpringAxialMuscle)super.clone();
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myMaxForce *= s;
   }

   @Override
   public void scaleMass (double s) {
      super.scaleMass (s);
      myStiffness *= s;
      myMaxForce *= s;
      myDamping *= s;
   }
}


