package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

/**
 * NOTE: This is a non-standard version of the St. Venant-Kirchoff model, which computes:
 * sigma = 1/J (lambda trace(E)-mu)B+mu/J*B^2,
 * in line with FEBio's recommendation
 *
 */
public class StVenantKirchoffMaterial extends FemMaterial {

   public static FieldPropertyList myProps =
      new FieldPropertyList (StVenantKirchoffMaterial.class, FemMaterial.class);

   protected static double DEFAULT_NU = 0.33;
   protected static double DEFAULT_E = 500000;

   private double myNu = DEFAULT_NU;
   private double myE = DEFAULT_E;
   //private ScalarFieldPointFunction myEFunc;
   private ScalarFieldComponent myEField;

   PropertyMode myNuMode = PropertyMode.Inherited;
   PropertyMode myEMode = PropertyMode.Inherited;


   static {
      myProps.addInheritableWithField (
         "YoungsModulus:Inherited", "Youngs modulus", DEFAULT_E);
      myProps.addInheritable (
         "PoissonsRatio:Inherited", "Poissons ratio", DEFAULT_NU);
   }

   public FieldPropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a new StVenantKirchoffMaterial with default parameter values.
    */
   public StVenantKirchoffMaterial (){
   }

   /**
    * Creates a new StVenantKirchoffMaterial with the specified Young's modulus
    * and Poisson's ratio.
    *
    * @param E Young's modulus
    * @param nu Poisson's ratio
    */
   public StVenantKirchoffMaterial (double E, double nu) {
      this();
      setYoungsModulus (E);
      setPoissonsRatio (nu);
   }

   public synchronized void setPoissonsRatio (double nu) {
      myNu = nu;
      myNuMode =
         PropertyUtils.propagateValue (this, "PoissonsRatio", myNu, myNuMode);
      notifyHostOfPropertyChange();
   }

   public double getPoissonsRatio() {
      return myNu;
   }

   public void setPoissonsRatioMode (PropertyMode mode) {
      myNuMode =
         PropertyUtils.setModeAndUpdate (this, "PoissonsRatio", myNuMode, mode);
   }

   public PropertyMode getPoissonsRatioMode() {
      return myNuMode;
   }

   public synchronized void setYoungsModulus (double E) {
      myE = E;
      myEMode =
         PropertyUtils.propagateValue (this, "YoungsModulus", myE, myEMode);
      notifyHostOfPropertyChange();
   }

   public double getYoungsModulus() {
      return myE;
   }

   public void setYoungsModulusMode (PropertyMode mode) {
      myEMode =
         PropertyUtils.setModeAndUpdate (this, "YoungsModulus", myEMode, mode);
   }

   public PropertyMode getYoungsModulusMode() {
      return myEMode;
   }

   public double getYoungsModulus (FemFieldPoint dp) {
      return (myEField == null ? getYoungsModulus() : myEField.getValue (dp));
   }

   public ScalarFieldComponent getYoungsModulusField() {
      return myEField;
   }
      
   public void setYoungsModulusField (ScalarFieldComponent field) {
      myEField = field;
      notifyHostOfPropertyChange();
   }

   /**
    * {@inheritDoc}
    */
   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {
      // temporaries allocated locally for thread safety
      SymmetricMatrix3d B = new SymmetricMatrix3d();
      SymmetricMatrix3d B2 = new SymmetricMatrix3d();

      double J = def.getDetF();

      // express constitutive law in terms of Lama parameters
      double E = getYoungsModulus(def);
      
      double G = E/(2*(1+myNu)); // bulk modulus
      double lam = (E*myNu)/((1-2*myNu)*(1+myNu));
      double mu = G;

      computeLeftCauchyGreen (B,def);

      double tr = 0.5*(B.m00 + B.m11 + B.m22 - 3);

      B2.mulTransposeLeft (B); // B2 = B*B

      sigma.scale ((lam*tr-mu)/J, B);
      sigma.scaledAdd (mu/J, B2);

      if (D != null) {
         D.setZero();
         TensorUtils.addTensorProduct (D, lam/J, B, B);
         TensorUtils.addSymmetricTensorProduct4 (D, mu/J, B, B);
         D.setLowerToUpper();         
      }
   }

   public double computeStrainEnergyDensity (
      DeformedPoint def, Matrix3d Q, double excitation, 
      MaterialStateObject state) {

      double E = getYoungsModulus(def);

      double G = E/(2*(1+myNu)); // bulk modulus
      double lam = (E*myNu)/((1-2*myNu)*(1+myNu));
      double mu = G;

      SymmetricMatrix3d C = new SymmetricMatrix3d();
      SymmetricMatrix3d C2 = new SymmetricMatrix3d();

      computeRightCauchyGreen (C, def);
      C2.mul (C, C);

      double trC = C.trace();
      double trC2 = C2.trace();

      double trE = (trC-3)/2;
      double trE2 = (trC2 - 2*trC + 3)/4;

      return lam*(trE*trE)/2 + mu*trE2;
   }
   
   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof StVenantKirchoffMaterial)) {
         return false;
      }
      StVenantKirchoffMaterial stvk = (StVenantKirchoffMaterial)mat;
      if (myNu != stvk.myNu ||
          myE != stvk.myE) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public StVenantKirchoffMaterial clone() {
      StVenantKirchoffMaterial mat = (StVenantKirchoffMaterial)super.clone();
      return mat;
   }

   public static void main (String[] args) {
      StVenantKirchoffMaterial mat = new StVenantKirchoffMaterial();

      Matrix3d Q = new Matrix3d();
      
      DeformedPointBase dpnt = new DeformedPointBase();
      dpnt.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));
      
      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      mat.setYoungsModulus (10);      
      mat.computeStressAndTangent (sig, D, dpnt, Q, 0.0, null);

      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      System.out.println ("D=\n" + D.toString ("%12.6f"));
   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         setYoungsModulus (myE/s);
      }
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         setYoungsModulus (myE*s);
      }
   }



   @Override
   public boolean isThreadSafe() {
      // subclasses must explicitly declare themselves thread safe
      return getClass() == StVenantKirchoffMaterial.class;
   }
}
