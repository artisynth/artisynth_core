package artisynth.core.materials;


public abstract class AxialMaterial extends MaterialBase {

   static Class<?>[] mySubClasses = new Class[] {
      LinearAxialMaterial.class,
      ConstantAxialMuscle.class,
      LinearAxialMuscle.class,
      PeckAxialMuscle.class,
      PaiAxialMuscle.class,
      BlemkerAxialMuscle.class,
      SimpleAxialMuscle.class,
      LigamentAxialMaterial.class
   };

   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }

   protected void notifyHostOfPropertyChange() {
      // stub for future use
   }

   public abstract double computeF (
      double l, double ldot, double l0, double excitation);

   public abstract double computeDFdl (
      double l, double ldot, double l0, double excitation);

   public abstract double computeDFdldot (
      double l, double ldot, double l0, double excitation);

   /** 
    * Returns true if computeDFdldot() always returns zero. For a linear
    * spring, this simply means the damping is zero.  When dFdldot is always 0,
    * then stiffness matrix will be symmetric.
    */
   public abstract boolean isDFdldotZero();

   public boolean equals (AxialMaterial mat) {
      return true;
   }

   public boolean equals (Object obj) {
      if (obj instanceof AxialMaterial) {
         return true;
      }
      else {
         return false;
      }
   }

   public AxialMaterial clone() {
      AxialMaterial mat = (AxialMaterial)super.clone();
      return mat;
   }
}

