package artisynth.core.materials;

import maspack.properties.*;

public class LinearAxialMaterial extends AxialMaterial {

   protected static double DEFAULT_STIFFNESS = 0;
   protected static double DEFAULT_DAMPING = 0;
   protected static double DEFAULT_REST_LENGTH = 0;   

   protected double myStiffness = DEFAULT_STIFFNESS; // stiffness
   protected double myDamping = DEFAULT_DAMPING; // damping

   protected PropertyMode myStiffnessMode = PropertyMode.Inherited;
   protected PropertyMode myDampingMode = PropertyMode.Inherited;

   public static PropertyList myProps =
      new PropertyList (LinearAxialMaterial.class, AxialMaterial.class);

   static {
      myProps.addInheritable (
         "stiffness", "linear stiffness",DEFAULT_STIFFNESS );
      myProps.addInheritable (
         "damping", "linear damping", DEFAULT_DAMPING);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getStiffness() {
      return myStiffness;
   }

   public synchronized void setStiffness (double E) {
      myStiffness = E;
      myStiffnessMode =
         PropertyUtils.propagateValue (
            this, "stiffness", myStiffness, myStiffnessMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getStiffnessMode() {
      return myStiffnessMode;
   }

   public void setStiffnessMode (PropertyMode mode) {
      myStiffnessMode =
         PropertyUtils.setModeAndUpdate (
            this, "stiffness", myStiffnessMode, mode);
   }

   public double getDamping() {
      return myDamping;
   }

   public synchronized void setDamping (double d) {
      myDamping = d;
      myDampingMode =
         PropertyUtils.propagateValue (
            this, "damping", myDamping, myDampingMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getDampingMode() {
      return myDampingMode;
   }

   public void setDampingMode (PropertyMode mode) {
      myDampingMode =
         PropertyUtils.setModeAndUpdate (
            this, "damping", myDampingMode, mode);
   }

   public LinearAxialMaterial (){
   }

   public LinearAxialMaterial (double k, double d) {
      setStiffness (k);
      setDamping (d);
   }
   

   public double computeF (
      double l, double ldot, double l0, double excitation) {
      return myStiffness*(l-l0) + myDamping*ldot;
   }

   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {
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
      if (!(mat instanceof LinearAxialMaterial)) {
         return false;
      }
      LinearAxialMaterial linm = (LinearAxialMaterial)mat;
      if (myStiffness != linm.myStiffness ||
          myDamping != linm.myDamping) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public LinearAxialMaterial clone() {
      LinearAxialMaterial mat = (LinearAxialMaterial)super.clone();
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
   }

   @Override
   public void scaleMass (double s) {
      super.scaleMass (s);
      myStiffness *= s;
      myDamping *= s;
   }
}



