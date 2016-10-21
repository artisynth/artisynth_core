package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

/**
 * NOTE: This is a non-standard version of the St. Venant-Kirchoff model, which computes:
 * sigma = 1/J (lambda trace(E)-mu)B+mu/J*B^2,
 * in line with FEBio's recommendation
 *
 */
public class StVenantKirchoffMaterial extends FemMaterial {

   public static PropertyList myProps =
      new PropertyList (StVenantKirchoffMaterial.class, FemMaterial.class);

   protected static double DEFAULT_NU = 0.33;
   protected static double DEFAULT_E = 500000;

   private double myNu = DEFAULT_NU;
   private double myE = DEFAULT_E;

   PropertyMode myNuMode = PropertyMode.Inherited;
   PropertyMode myEMode = PropertyMode.Inherited;

   private SymmetricMatrix3d myB;
   private SymmetricMatrix3d myB2;

   static {
      myProps.addInheritable (
         "YoungsModulus:Inherited", "Youngs modulus", DEFAULT_E);
      myProps.addInheritable (
         "PoissonsRatio:Inherited", "Poissons ratio", DEFAULT_NU);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public StVenantKirchoffMaterial (){
      myB = new SymmetricMatrix3d();
      myB2 = new SymmetricMatrix3d();
   }

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

   public void computeStress (
      SymmetricMatrix3d sigma, SolidDeformation def, Matrix3d Q,
      FemMaterial baseMat) {

      double J = def.getDetF();

      // express constitutive law in terms of Lama parameters
      double G = myE/(2*(1+myNu)); // bulk modulus
      double lam = (myE*myNu)/((1-2*myNu)*(1+myNu));
      double mu = G;

      def.computeLeftCauchyGreen (myB);

      double tr = 0.5*(myB.m00 + myB.m11 + myB.m22 - 3);

      myB2.mulTransposeLeft (myB); // myB2 = B*B

      sigma.scale ((lam*tr-mu)/J, myB);
      sigma.scaledAdd (mu/J, myB2);
   }

   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def, 
      Matrix3d Q, FemMaterial baseMat) {

      double J = def.getDetF();

      def.computeLeftCauchyGreen (myB);

      // express constitutive law in terms of Lama parameters
      double G = myE/(2*(1+myNu)); // bulk modulus
      double lam = (myE*myNu)/((1-2*myNu)*(1+myNu));
      double mu = G;

      D.setZero();
      TensorUtils.addTensorProduct (D, lam/J, myB, myB);
      TensorUtils.addSymmetricTensorProduct4 (D, mu/J, myB, myB);
      D.setLowerToUpper();
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
      mat.myB = new SymmetricMatrix3d();
      mat.myB2 = new SymmetricMatrix3d();
      return mat;
   }

   public static void main (String[] args) {
      StVenantKirchoffMaterial mat = new StVenantKirchoffMaterial();

      SolidDeformation def = new SolidDeformation();
      Matrix3d Q = new Matrix3d();
      def.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      mat.setYoungsModulus (10);      
      mat.computeStress (sig, def, Q, null);
      mat.computeTangent (D, sig, def, Q, null);

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


}
