package artisynth.core.materials;

import maspack.properties.*;

public class SimpleAxialMuscle extends LinearAxialMaterial {

   protected static double DEFAULT_MAX_FORCE = 1;
   protected double myMaxForce = DEFAULT_MAX_FORCE; // max force
   protected PropertyMode myMaxForceMode = PropertyMode.Inherited;

   protected static double DEFAULT_BLEND_INTERVAL = 0;
   protected double myBlendInterval = DEFAULT_BLEND_INTERVAL;

   protected static boolean DEFAULT_UNILATERAL = false;
   protected boolean myUnilateral = DEFAULT_UNILATERAL;

   public static PropertyList myProps =
      new PropertyList (SimpleAxialMuscle.class, LinearAxialMaterial.class);

   static {
      myProps.addInheritable (
         "maxForce", "excitation force gain", DEFAULT_MAX_FORCE );
      myProps.add (
         "blendInterval",
         "interval over which stiffness is ramped to its maximum value",
         DEFAULT_BLEND_INTERVAL);
      myProps.add (
         "unilateral",
         "if true, passive force is applied only when l > l0",
         DEFAULT_UNILATERAL);
   }

   public SimpleAxialMuscle () {
      super();
   }

   public SimpleAxialMuscle (double stiffness, double damping, double fmax) {
      super();
      setStiffness (stiffness);
      setDamping (damping);
      setMaxForce (fmax);
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

   public boolean getUnilateral() {
      return myUnilateral;
   }

   public void setUnilateral (boolean enable) {
      myUnilateral = enable;
   }

   public double getBlendInterval() {
      return myBlendInterval;
   }

   public void setBlendInterval (double tau) {
      myBlendInterval = tau;
   }

   private double sgn (double x) {
      return x < 0 ? -1 : 1;
   }

   public double computeF (
      double l, double ldot, double l0, double ex) {
      double dl;
      if (myUnilateral && l < l0) {
         dl = 0;
      }
      else {
         dl = l-l0;
      }
      double Felastic = 0;
      double tau = myBlendInterval;
      if (tau > 0) {
         double a = myStiffness/tau;
         if (Math.abs(dl) < tau) {
            Felastic = sgn(dl)*a*dl*dl/2;
         }
         else {
            Felastic = sgn(dl)*(a*tau*tau/2 + (Math.abs(dl)-tau)*myStiffness);
         }
      }
      else {
         Felastic = myStiffness*dl;
      }
      return Felastic + myDamping*ldot + myMaxForce*ex;
   }

   public double computeDFdl(double l, double ldot, double l0, double ex) {
      double dl;
      if (myUnilateral && l < l0) {
         dl = 0;
      }
      else {
         dl = l-l0;
      }
      double tau = myBlendInterval;
      if (tau > 0 && Math.abs(dl) < tau) {
         double a = myStiffness/tau;
         return a*Math.abs(dl);
      }
      else {
         return myStiffness;
      }
   }

   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {
      return myDamping;
   }

   public boolean isDFdldotZero() {
      return myDamping == 0;
   }

   public boolean equals (AxialMaterial mat) {
      if (!(mat instanceof SimpleAxialMuscle)) {
         return false;
      }
      SimpleAxialMuscle sam = (SimpleAxialMuscle)mat;
      if (myStiffness != sam.myStiffness ||
          myMaxForce != sam.myMaxForce ||
          myDamping != sam.myDamping) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public SimpleAxialMuscle clone() {
      SimpleAxialMuscle mat = (SimpleAxialMuscle)super.clone();
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



