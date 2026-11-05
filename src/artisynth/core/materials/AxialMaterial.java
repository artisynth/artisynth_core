package artisynth.core.materials;


public abstract class AxialMaterial extends MaterialBase {

   static Class<?>[] mySubClasses = new Class[] {
      LinearAxialMaterial.class,
      ConstantAxialMaterial.class,
      ConstantAxialMuscle.class,
      LinearAxialMuscle.class,
      PeckAxialMuscle.class,
      PaiAxialMuscle.class,
      BlemkerAxialMuscle.class,
      SimpleAxialMuscle.class,
      LigamentAxialMaterial.class,
      MasoudMillardLAM.class,
      UWLigamentMaterial.class,
      Hill3ElemMuscleRigidTendon.class,
      Blankevoort1991AxialLigament.class,
      Millard2012AxialMuscle.class,
      Millard2012AxialTendon.class,
      Millard2012AxialLigament.class,
      Thelen2003AxialMuscle.class,
      Thelen2003AxialTendon.class
   };

   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }

   protected void notifyHostOfPropertyChange() {
      // stub for future use
   }

   /**
    * Computes and returns the axial spring tension, as a function of
    * length, length time derivative, rest length, and excitation.
    * 
    * @param l spring length
    * @param ldot spring length time derivative
    * @param l0 spring rest length
    * @param excitation excitation value (varying from 0 to 1)
    * @return spring tension
    */
   public abstract double computeF (
      double l, double ldot, double l0, double excitation);
   
   /**
    * Computes and returns the derivative of the axial spring tension
    * with respect to the length.
    * 
    * @param l spring length
    * @param ldot spring length time derivative
    * @param l0 spring rest length
    * @param excitation excitation value (varying from 0 to 1)
    * @return tension derivative with respect to length
    */
   public abstract double computeDFdl (
      double l, double ldot, double l0, double excitation);
   
   /**
    * Computes and returns the derivative of the axial spring tension
    * with respect to the length time derivative.
    * 
    * @param l spring length
    * @param ldot spring length time derivative
    * @param l0 spring rest length
    * @param excitation excitation value (varying from 0 to 1)
    * @return tension derivative with respect to length time derivative
    */
   public abstract double computeDFdldot (
      double l, double ldot, double l0, double excitation);

   /** 
    * Returns true if computeDFdldot() always returns zero. For a linear
    * spring, this simply means the damping is zero.  When dFdldot is always 0,
    * then stiffness matrix will be symmetric.
    */
   public abstract boolean isDFdldotZero();

   /**
    * Enables or disables quasi-static excitation response for this
    * material. This is a transient mode, used by the inverse tracking
    * controller when forming its excitation response matrices, and is not
    * saved as part of the material. While enabled, the force and its
    * derivatives are computed assuming that internal states through which
    * excitation acts (such as the fibre length of a compliant tendon muscle)
    * reach equilibrium within the time step. Evaluations made in this mode
    * must not alter the material's numeric state.
    *
    * <p>The default implementation does nothing, which is appropriate for
    * materials without such internal states.
    *
    * <p>This feature is still experimental.
    *
    * @param enable if {@code true}, enables quasi-static response
    */
   public void setQuasiStaticResponse (boolean enable) {
   }

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

