package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix6d;
import maspack.matrix.SVDecomposition;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.RotationMatrix3d;

public class TransverseLinearMaterial extends LinearMaterialBase {
   
   static {
      FemMaterial.registerSubclass(TransverseLinearMaterial.class);
   }
   
   private static Vector2d DEFAULT_YOUNGS_MODULUS =
      new Vector2d(LinearMaterial.DEFAULT_E, LinearMaterial.DEFAULT_E);
   private static Vector2d DEFAULT_POISSONS_RATIO =
      new Vector2d(LinearMaterial.DEFAULT_NU, LinearMaterial.DEFAULT_NU);
   private static double DEFAULT_SHEAR_MODULUS =
      LinearMaterial.DEFAULT_E/2/(1+LinearMaterial.DEFAULT_NU);

   private static Vector3d DEFAULT_DIRECTION = new Vector3d(0, 0, 1);
   private Vector3d myDirection = new Vector3d(DEFAULT_DIRECTION);

   // anisotropic stiffness matrix, cached when parameters are not field-bound
   private Matrix6d myC;
   private Vector2d myE;        // radial, z-axis young's modulus
   private double myG;          // shear modulus
   private Vector2d myNu;       // radial, z-axis Poisson's ratio
   // volatile, since myC may be lazily built from multiple threads, and
   // setting this publishes it
   private volatile boolean stiffnessValid;

   // private VectorFieldPointFunction<Vector2d> myEFunction = null;
   // private ScalarFieldPointFunction myGFunction = null;
   // private VectorFieldPointFunction<Vector3d> myDirectionFunction = null;
   
   private VectorFieldComponent<Vector2d> myEField = null;
   private ScalarFieldComponent myGField = null;
   private VectorFieldComponent<Vector3d> myDirectionField = null;
   
   public static FieldPropertyList myProps =
      new FieldPropertyList (
         TransverseLinearMaterial.class, LinearMaterialBase.class);

   static {
      myProps.addWithField (
         "youngsModulus", "radial and z-axis Young's modulus,",
         DEFAULT_YOUNGS_MODULUS);
      myProps.addWithField (
         "shearModulus", "radial-to-z-axis shear modulus,",
         DEFAULT_SHEAR_MODULUS);
      myProps.add (
         "poissonsRatio", "radial and z-axis Young's modulus,",
         DEFAULT_POISSONS_RATIO);
      myProps.addWithField (
         "direction", "anisotropic direction", DEFAULT_DIRECTION);
   }

   public FieldPropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a new TransverseLinearMaterial with default parameter values.
    */
   public TransverseLinearMaterial () {
      this (DEFAULT_YOUNGS_MODULUS, DEFAULT_SHEAR_MODULUS,
            DEFAULT_POISSONS_RATIO, LinearMaterial.DEFAULT_COROTATED);
   }

   /**
    * Creates a new TransverseLinearMaterial with the specified Young's moduli,
    * shear modulus, Poisson's ratios, and corotated flag.
    *
    * @param E radial and axial Young's moduli
    * @param G radial-to-axial shear modulus
    * @param nu radial and axial Poisson's ratios
    * @param corotated if {@code true}, use the corotated linear formulation
    */
   public TransverseLinearMaterial (
      Vector2d E, double G, Vector2d nu, boolean corotated) {
      super(corotated);
      
      myE = new Vector2d(E);
      myG = G;
      myNu = new Vector2d(nu);
      myC = null;
      stiffnessValid = false;
   }

   public Matrix6d getStiffnessTensor() {
      return getStiffness (/*defp=*/null);
   }

   /**
    * Computes the stiffness tensor for the given Young's moduli, shear
    * modulus and anisotropic direction.
    *
    * @param C returns the stiffness tensor
    * @param E radial and axial Young's moduli
    * @param G shear modulus
    * @param dir anisotropic direction
    */
   protected void computeStiffnessTensor (
      Matrix6d C, Vector2d E, double G, Vector3d dir) {
      
      Matrix6d invC = new Matrix6d();
      
      invC.m00 = 1.0/E.x;
      invC.m01 = -myNu.x/E.x;
      invC.m02 = -myNu.y/E.y;
      
      invC.m10 = invC.m01;
      invC.m11 = invC.m00;
      invC.m12 = invC.m02;
      
      invC.m20 = invC.m02;
      invC.m21 = invC.m20;
      invC.m22 = 1.0/E.y;
      
      invC.m33 = 2*(1+myNu.x)/E.x;
      invC.m44 = 1.0/G;
      invC.m55 = invC.m44;
      
      SVDecomposition svd = new SVDecomposition(invC);
      svd.pseudoInverse(C);
      if (!dir.equals (DEFAULT_DIRECTION)) {
         RotationMatrix3d R = new RotationMatrix3d();
         R.setZDirection (dir);
         TensorUtils.rotateTangent (C, C, R);
      }
      
      //      Matrix6d C = AnisotropicLinearMaterial.createIsotropicStiffness((myE.x + myE.y)/2, (myNu.x + myNu.y)/2);
      //      Matrix6d Cinv = new Matrix6d();
      //      svd.factor(C);
      //      svd.pseudoInverse(Cinv);
      //      
      //      if (!C.epsilonEquals(myC, 1e-6)) {
      //         System.out.println("Hmm...");
      //         System.out.println(C);
      //         System.out.println(" vs ");
      //         System.out.println(myC);
      //      }
   }
   
   /**
    * Sets the anisotropic Young's modulus. {@code x} gives the radial
    * component in the plane perpendicular to the anisotropic direction,
    * while {@code y} gives the axial component along the direction.
    * 
    * @param E anisotropic Young's modulus
    */
   public void setYoungsModulus(Vector2d E) {
      setYoungsModulus(E.x, E.y);
   }
   
   /**
    * Gets the anisotropic Young's modulus.
    * @return Young's modulus
    */
   public Vector2d getYoungsModulus() {
      return myE;
   }
   
   /**
    * Sets the anisotropic Young's modulus.
    * 
    * @param radial component in the plane perpendicular to the anisotropic
    * direction
    * @param axial component along the anisotropic direction
    */
   public void setYoungsModulus(double radial, double axial) {
      myE.set(radial, axial);
      stiffnessValid = false;
      notifyHostOfPropertyChange("youngsModulus");
   }
   
   public Vector2d getYoungsModulus (FemFieldPoint dp) {
      return myEField == null ? getYoungsModulus() : myEField.getValue (dp);
   }

   public VectorFieldComponent<Vector2d> getYoungsModulusField() {
      return myEField;
   }
      
   public void setYoungsModulusField (VectorFieldComponent<Vector2d> field) {
      myEField = field;
      notifyHostOfPropertyChange();
   }

   /**
    * Sets the anisotropic direction. Default is the z axis. The specified
    * direction will be normalized.
    * 
    * @param dir new anisotropic direction
    */
   public void setDirection (Vector3d dir) {
      myDirection.set (dir);
      myDirection.normalize();
   }
   
   /**
    * Gets the anisotropic direction.
    * 
    * @return anisotropic direction (should not be modified)
    */
   public Vector3d getDirection() {
      return myDirection;
   }
   
   public Vector3d getDirection (FemFieldPoint dp) {
      return (myDirectionField == null ?
              getDirection() : myDirectionField.getValue (dp));
   }

   public VectorFieldComponent<Vector3d> getDirectionField() {
      return myDirectionField;
   }
      
   public void setDirectionField (VectorFieldComponent<Vector3d> field) {
      myDirectionField = field;
      notifyHostOfPropertyChange();
   }

   /**
    * Sets the shear modulus between the anisotropic direction and the plane
    * perpendicular to it.
    *
    * @param G shear modulus Gxz=Gyz
    */
   public void setShearModulus(double G) {
      myG = G;
      stiffnessValid = false;
      notifyHostOfPropertyChange("shearModulus");
   }
   
   /**
    * Gets the shear modulus between the anisotropic direction and the plane
    * perpendicular to it.
    *
    * @return shear modulus Gxz=Gyz
    */
   public double getShearModulus() {
      return myG;
   }

   public double getShearModulus (FemFieldPoint dp) {
      return myGField == null ? getShearModulus() : myGField.getValue(dp);
   }

   public ScalarFieldComponent getShearModulusField() {
      return myGField;
   }
      
   public void setShearModulusField (ScalarFieldComponent field) {
      myGField = field;
      notifyHostOfPropertyChange();
   }
   
   /**
    * Sets the anisotropic Poisson's ratio. {@code x} gives the radial
    * component in the plane perpendicular to the anisotropic direction,
    * while {@code y} gives the axial component along the direction.
    * 
    * @param nu anisotropic Poisson's ratio
    */
   public void setPoissonsRatio(Vector2d nu) {
      setPoissonsRatio(nu.x, nu.y);
   }
   
   /**
    * Returns the anisotropic Poisson's ratio (nu_xy, nu_xz=nu_yz)
    * 
    * @return anisotropic Poisson's ratio
    */
   public Vector2d getPoissonsRatio() {
      return myNu;
   }
   
   /**
    * Sets the anisotropic Poisson's ratio.
    * 
    * @param nuxy component in the plane perpendicular to the anisotropic
    * direction
    * @param nuxz component along the anisotropic direction
    */
   public void setPoissonsRatio(double nuxy, double nuxz) {
      myNu.set(nuxy, nuxz);
      stiffnessValid = false;
      notifyHostOfPropertyChange("poissonsRatio");
   }
   
   /**
    * Returns the stiffness tensor for a given deformed point. If any
    * parameters are bound to fields, the tensor is computed for the point
    * and returned in a new matrix. Otherwise, a cached tensor is returned,
    * which is built lazily if necessary. This method is thread safe (for the
    * cached case, provided the parameters are not changed concurrently).
    *
    * @param defp deformed point, or {@code null} to use the explicit
    * parameter settings
    * @return stiffness tensor (should not be modified)
    */
   protected Matrix6d getStiffness (DeformedPoint defp) {
      boolean functionalParams =
         (myEField != null || myGField != null || myDirectionField != null);
      if (functionalParams) {
         Matrix6d C = new Matrix6d();
         if (defp != null) {
            computeStiffnessTensor (
               C, getYoungsModulus(defp), getShearModulus(defp),
               getDirection(defp));
         }
         else {
            // no deformed point. Use explicit parameter settings
            computeStiffnessTensor (
               C, getYoungsModulus(), getShearModulus(), getDirection());
         }
         return C;
      }
      if (!stiffnessValid) {
         // lazy update is synchronized since it may occur in multiple threads
         synchronized (this) {
            if (!stiffnessValid) {
               // build in a new matrix, so that the old one (which may still
               // be referenced) is not modified
               Matrix6d C = new Matrix6d();
               computeStiffnessTensor (
                  C, getYoungsModulus(), getShearModulus(), getDirection());
               myC = C;
               stiffnessValid = true; // publishes myC
            }
         }
      }
      return myC;
   }
   
   @Override
   protected void multiplyC (
      SymmetricMatrix3d sigma, SymmetricMatrix3d eps, DeformedPoint defp) {
      
      Matrix6d C = getStiffness (defp);

      double e00 = eps.m00;
      double e11 = eps.m11;
      double e22 = eps.m22;
      double e01 = eps.m01;
      double e02 = eps.m02;
      double e12 = eps.m12;

      // perform multiplication
      double s00 = C.m00*e00 + C.m01*e11 + C.m02*e22 +
         2*C.m03*e01 + 2*C.m04*e12 + 2*C.m05*e02;
      double s11 = C.m10*e00 + C.m11*e11 + C.m12*e22 +
         2*C.m13*e01 + 2*C.m14*e12 + 2*C.m15*e02;
      double s22 = C.m20*e00 + C.m21*e11 + C.m22*e22 +
         2*C.m23*e01 + 2*C.m24*e12 + 2*C.m25*e02;
      double s01 = C.m30*e00 + C.m31*e11 + C.m32*e22 +
         2*C.m33*e01 + 2*C.m34*e12 + 2*C.m35*e02;
      double s12 = C.m40*e00 + C.m41*e11 + C.m42*e22 +
         2*C.m43*e01 + 2*C.m44*e12 + 2*C.m45*e02;
      double s02 = C.m50*e00 + C.m51*e11 + C.m52*e22 +
         2*C.m53*e01 + 2*C.m54*e12 + 2*C.m55*e02;

      sigma.set(s00, s11, s22, s01, s02, s12);
   }
   
   @Override
   protected void getC(Matrix6d C, DeformedPoint defp) {
      C.set (getStiffness (defp));
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof TransverseLinearMaterial)) {
         return false;
      }
      TransverseLinearMaterial linm = (TransverseLinearMaterial)mat;
      if (!myE.equals (linm.myE) ||
          !myNu.equals (linm.myNu) ||
          myG != linm.myG ||
          !myDirection.equals (linm.myDirection)) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public TransverseLinearMaterial clone() {
      TransverseLinearMaterial mat = (TransverseLinearMaterial)super.clone();
      mat.myC = null;
      mat.stiffnessValid = false;
      mat.myE = myE.clone();
      mat.myNu = myNu.clone();
      mat.myG = myG;
      
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         myE.scale(1.0/s);
         myG = myG/s;
         stiffnessValid = false;
      }
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         myE.scale(s);
         myG = myG*s;
         stiffnessValid = false;
      }
   }

   // XXX scan/write for functions

   @Override
   public boolean isThreadSafe() {
      // subclasses must explicitly declare themselves thread safe
      return getClass() == TransverseLinearMaterial.class;
   }
}
