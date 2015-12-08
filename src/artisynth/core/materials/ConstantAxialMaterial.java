package artisynth.core.materials;

import maspack.properties.*;

public class ConstantAxialMaterial extends AxialMaterial {

   protected static double DEFAULT_FORCE = 0;

   protected double myForce = DEFAULT_FORCE; // Force

   protected PropertyMode myForceMode = PropertyMode.Inherited;
   protected PropertyMode myDampingMode = PropertyMode.Inherited;

   public static PropertyList myProps =
      new PropertyList (ConstantAxialMaterial.class, AxialMaterial.class);

   static {
      myProps.add (
         "force", "constant force magnitude", DEFAULT_FORCE );
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getForce() {
      return myForce;
   }

   public void setForce (double force) {
      myForce = force;
   }

   public ConstantAxialMaterial (){
   }

   public ConstantAxialMaterial (double f) {
      setForce (f);
   }

   public double computeF (
      double l, double ldot, double l0, double excitation) {
      return myForce;
   }

   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {
      return 0; 
   }

   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {
      return 0;
   }

   public boolean isDFdldotZero() {
      return true;
   }

   public boolean equals (AxialMaterial mat) {
      if (!(mat instanceof ConstantAxialMaterial)) {
         return false;
      }
      ConstantAxialMaterial linm = (ConstantAxialMaterial)mat;
      if (myForce != linm.myForce) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public ConstantAxialMaterial clone() {
      ConstantAxialMaterial mat = (ConstantAxialMaterial)super.clone();
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
   }

   @Override
   public void scaleMass (double s) {
      super.scaleMass (s);
      myForce *= s;
   }
}



