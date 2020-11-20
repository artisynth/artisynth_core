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

   private Matrix6d myC;        // anisotropic stiffness matrix
   private Vector2d myE;        // radial, z-axis young's modulus
   private double myG;          // shear modulus
   private Vector2d myNu;       // radial, z-axis Poisson's ratio
   private boolean stiffnessValid;

   private VectorFieldPointFunction<Vector2d> myEFunction = null;
   private ScalarFieldPointFunction myGFunction = null;
   private VectorFieldPointFunction<Vector3d> myDirectionFunction = null;
   
   public static FunctionPropertyList myProps =
      new FunctionPropertyList (
         TransverseLinearMaterial.class, LinearMaterialBase.class);

   static {
      myProps.addWithFunction (
         "youngsModulus", "radial and z-axis Young's modulus,",
         DEFAULT_YOUNGS_MODULUS);
      myProps.addWithFunction (
         "shearModulus", "radial-to-z-axis shear modulus,",
         DEFAULT_SHEAR_MODULUS);
      myProps.add (
         "poissonsRatio", "radial and z-axis Young's modulus,",
         DEFAULT_POISSONS_RATIO);
      myProps.addWithFunction (
         "direction", "anisotropic direction", DEFAULT_DIRECTION);
   }

   public FunctionPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public TransverseLinearMaterial () {
      this (DEFAULT_YOUNGS_MODULUS, DEFAULT_SHEAR_MODULUS,
            DEFAULT_POISSONS_RATIO, LinearMaterial.DEFAULT_COROTATED);
   }

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
      maybeUpdateStiffness (/*defp=*/null);
      return myC;
   }
   
   protected void updateStiffnessTensor (Vector2d E, double G) {
      
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
      if (myC == null) {
         myC = new Matrix6d();
      }
      svd.pseudoInverse(myC);
      
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
   
   public Vector2d getYoungsModulus (FieldPoint dp) {
      if (myEFunction == null) {
         return getYoungsModulus();
      }
      else {
         return myEFunction.eval (dp);
      }
   }

   public VectorFieldPointFunction<Vector2d> getYoungsModulusFunction() {
      return myEFunction;
   }
      
   public void setYoungsModulusFunction (
      VectorFieldPointFunction<Vector2d> func) {
      myEFunction = func;
      notifyHostOfPropertyChange();
   }
   
   public void setYoungsModulusField (
      VectorField<Vector2d> field, boolean useRestPos) {
      myEFunction = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public VectorField<Vector2d> getYoungsModulusField () {
      return FieldUtils.getFieldFromFunction (myEFunction);
   }

   /**
    * Sets the anisotropic direction. Default is the z axis.
    * 
    * @param dir new anisotropic direction
    */
   public void setDirection (Vector3d dir) {
      myDirection.set (dir);
   }
   
   /**
    * Gets the anisotropic direction.
    * 
    * @return anisotropic direction (should not be modified)
    */
   public Vector3d getDirection() {
      return myDirection;
   }
   
   public Vector3d getDirection (FieldPoint dp) {
      if (myDirectionFunction == null) {
         return getDirection();
      }
      else {
         return myDirectionFunction.eval (dp);
      }
   }

   public VectorFieldPointFunction<Vector3d> getDirectionFunction() {
      return myDirectionFunction;
   }
      
   public void setDirectionFunction (
      VectorFieldPointFunction<Vector3d> func) {
      myDirectionFunction = func;
      notifyHostOfPropertyChange();
   }
   
   public void setDirectionField (
      VectorField<Vector3d> field, boolean useRestPos) {
      myDirectionFunction = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public VectorField<Vector3d> getDirectionField () {
      return FieldUtils.getFieldFromFunction (myDirectionFunction);
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

   public double getShearModulus (FieldPoint dp) {
      if (myGFunction == null) {
         return getShearModulus();
      }
      else {
         return myGFunction.eval (dp);
      }
   }

   public ScalarFieldPointFunction getShearModulusFunction() {
      return myGFunction;
   }
      
   public void setShearModulusFunction (ScalarFieldPointFunction func) {
      myGFunction = func;
      notifyHostOfPropertyChange();
   }
   
   public void setShearModulusField (
      ScalarField field, boolean useRestPos) {
      myGFunction = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getShearModulusField () {
      return FieldUtils.getFieldFromFunction (myGFunction);
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
   
   protected void maybeUpdateStiffness(DeformedPoint defp) {
      boolean functionalParams = (myEFunction != null || myGFunction != null);
      if (!stiffnessValid || functionalParams) {
         Vector2d E;
         double G;
         if (defp != null) {
            E = getYoungsModulus(defp);
            G = getShearModulus(defp);
         }
         else {
            // no deformed point. Use explicit parameter settings
            E = getYoungsModulus();
            G = getShearModulus();           
         }
         updateStiffnessTensor (E, G);
         Vector3d dir = getDirection (defp);
         if (!dir.equals (DEFAULT_DIRECTION)) {
            RotationMatrix3d R = new RotationMatrix3d();
            R.setZDirection (dir);
            TensorUtils.rotateTangent (myC, myC, R);
         }
      }
      if (!functionalParams) {
         stiffnessValid = true;
      }
   }
   
   @Override
   protected void multiplyC (
      SymmetricMatrix3d sigma, SymmetricMatrix3d eps, DeformedPoint defp) {
      
      // update stiffness
      maybeUpdateStiffness (defp);

      double e00 = eps.m00;
      double e11 = eps.m11;
      double e22 = eps.m22;
      double e01 = eps.m01;
      double e02 = eps.m02;
      double e12 = eps.m12;

      // perform multiplication
      double s00 = myC.m00*e00 + myC.m01*e11 + myC.m02*e22 +
         2*myC.m03*e01 + 2*myC.m04*e12 + 2*myC.m05*e02;
      double s11 = myC.m10*e00 + myC.m11*e11 + myC.m12*e22 +
         2*myC.m13*e01 + 2*myC.m14*e12 + 2*myC.m15*e02;
      double s22 = myC.m20*e00 + myC.m21*e11 + myC.m22*e22 +
         2*myC.m23*e01 + 2*myC.m24*e12 + 2*myC.m25*e02;
      double s01 = myC.m30*e00 + myC.m31*e11 + myC.m32*e22 +
         2*myC.m33*e01 + 2*myC.m34*e12 + 2*myC.m35*e02;
      double s12 = myC.m40*e00 + myC.m41*e11 + myC.m42*e22 +
         2*myC.m43*e01 + 2*myC.m44*e12 + 2*myC.m45*e02;
      double s02 = myC.m50*e00 + myC.m51*e11 + myC.m52*e22 +
         2*myC.m53*e01 + 2*myC.m54*e12 + 2*myC.m55*e02;

      sigma.set(s00, s11, s22, s01, s02, s12);
   }
   
   @Override
   protected void getC(Matrix6d C, DeformedPoint defp) {
      maybeUpdateStiffness(defp);
      C.set(myC);
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof TransverseLinearMaterial)) {
         return false;
      }
      TransverseLinearMaterial linm = (TransverseLinearMaterial)mat;
      if (!myC.equals(linm)) {
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
}
