package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix6d;
import maspack.matrix.SVDecomposition;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector2d;

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
   
   private Matrix6d myC;        // anisotropic stiffness matrix
   private Vector2d myE;        // radial, z-axis young's modulus
   private double myG;          // shear modulus
   private Vector2d myNu;       // radial, z-axis Poisson's ratio
   private boolean stiffnessValid;

   private VectorFieldPointFunction<Vector2d> myEFunction = null;
   private ScalarFieldPointFunction myGFunction = null;
   
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
    * Set Young's modulus, xy-plane (radial) and along z-axis (axial)
    * @param E young's modulus, E.x radial, E.y axial 
    */
   public void setYoungsModulus(Vector2d E) {
      setYoungsModulus(E.x, E.y);
   }
   
   /**
    * Get Young's modulus
    * @return Young's modulus
    */
   public Vector2d getYoungsModulus() {
      return myE;
   }
   
   /**
    * Sets the Youngs modulus
    * @param radial along xy-plane
    * @param axial along z-axis
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
    * Sets the shear modulus between xy and z
    * @param G shear modulus Gxz=Gyz
    */
   public void setShearModulus(double G) {
      myG = G;
      stiffnessValid = false;
      notifyHostOfPropertyChange("shearModulus");
   }
   
   /**
    * Gets the shear modulus betwen xy and z
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
    * Sets Poisson's ratio (nu_xy, nu_xz=nu_yz) 
    * @param nu poisson's ratio
    */
   public void setPoissonsRatio(Vector2d nu) {
      setPoissonsRatio(nu.x, nu.y);
   }
   
   /**
    * Returns Poisson's ratio (nu_xy, nu_xz=nu_yz) 
    * @return poisson's ratio
    */
   public Vector2d getPoissonsRatio() {
      return myNu;
   }
   
   /**
    * Sets Poisson's ratio
    * @param nuxy in-plane ratio
    * @param nuxz out-of-plane ratio
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

      // perform multiplication
      double m00 = myC.m00*eps.m00 + myC.m01*eps.m11 + myC.m02*eps.m22 +
         2*myC.m03*eps.m01 + 2*myC.m04*eps.m12 + 2*myC.m05*eps.m02;
      double m11 = myC.m10*eps.m00 + myC.m11*eps.m11 + myC.m12*eps.m22 +
         2*myC.m13*eps.m01 + 2*myC.m14*eps.m12 + 2*myC.m15*eps.m02;
      double m22 = myC.m20*eps.m00 + myC.m21*eps.m11 + myC.m22*eps.m22 +
         2*myC.m23*eps.m01 + 2*myC.m24*eps.m12 + 2*myC.m25*eps.m02;
      double m01 = myC.m30*eps.m00 + myC.m31*eps.m11 + myC.m32*eps.m22 +
         2*myC.m33*eps.m01 + 2*myC.m34*eps.m12 + 2*myC.m35*eps.m02;
      double m12 = myC.m40*eps.m00 + myC.m41*eps.m11 + myC.m42*eps.m22 +
         2*myC.m43*eps.m01 + 2*myC.m44*eps.m12 + 2*myC.m45*eps.m02;
      double m02 = myC.m50*eps.m00 + myC.m51*eps.m11 + myC.m52*eps.m22 +
         2*myC.m53*eps.m01 + 2*myC.m54*eps.m12 + 2*myC.m55*eps.m02;

      sigma.set(m00, m11, m22, m01, m02, m12);
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

}
