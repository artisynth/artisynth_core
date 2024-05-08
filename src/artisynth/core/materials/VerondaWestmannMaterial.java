package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class VerondaWestmannMaterial extends IncompressibleMaterialBase {

   public static FieldPropertyList myProps =
      new FieldPropertyList (
         VerondaWestmannMaterial.class, IncompressibleMaterialBase.class);

   protected static double DEFAULT_C1 = 1000.0;
   private double myC1 = DEFAULT_C1;
   PropertyMode myC1Mode = PropertyMode.Inherited;
   ScalarFieldComponent myC1Field = null;

   protected static double DEFAULT_C2 = 10.0;
   private double myC2 = DEFAULT_C2;
   PropertyMode myC2Mode = PropertyMode.Inherited;
   ScalarFieldComponent myC2Field = null;

   static {
      myProps.addInheritableWithField (
         "C1:Inherited", "First VW coefficient", DEFAULT_C1);
      myProps.addInheritableWithField (
         "C2:Inherited", "Second VW coefficient", DEFAULT_C2);
   }

   public FieldPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public VerondaWestmannMaterial (){
   }

   public VerondaWestmannMaterial (double c1, double c2, double kappa) {
      setC1 (c1);
      setC2 (c2);
      setBulkModulus (kappa);
   }

   // --- C1 ----

   public synchronized void setC1 (double c1) {
      myC1 = c1;
      myC1Mode =
         PropertyUtils.propagateValue (this, "C1", myC1, myC1Mode);
      notifyHostOfPropertyChange();
   }

   public double getC1() {
      return myC1;
   }

   public void setC1Mode (PropertyMode mode) {
      myC1Mode =
         PropertyUtils.setModeAndUpdate (this, "C1", myC1Mode, mode);
   }

   public PropertyMode getC1Mode() {
      return myC1Mode;
   }

   public double getC1 (FemFieldPoint dp) {
      if (myC1Field == null) {
         return getC1();
      }
      else {
         return myC1Field.getValue (dp);
      }
   }

   public ScalarFieldComponent getC1Field() {
      return myC1Field;
   }
      
   public void setC1Field (ScalarFieldComponent func) {
      myC1Field = func;
      notifyHostOfPropertyChange();
   }

   // --- C2 ----

   public synchronized void setC2 (double c2) {
      myC2 = c2;
      myC2Mode =
         PropertyUtils.propagateValue (this, "C2", myC2, myC2Mode);
      notifyHostOfPropertyChange();
   }

   public double getC2() {
      return myC2;
   }

   public void setC2Mode (PropertyMode mode) {
      myC2Mode =
         PropertyUtils.setModeAndUpdate (this, "C2", myC2Mode, mode);
   }

   public PropertyMode getC2Mode() {
      return myC2Mode;
   }

   public double getC2 (FemFieldPoint dp) {
      if (myC2Field == null) {
         return getC2();
      }
      else {
         return myC2Field.getValue (dp);
      }
   }

   public ScalarFieldComponent getC2Field() {
      return myC2Field;
   }
      
   public void setC2Field (ScalarFieldComponent func) {
      myC2Field = func;
      notifyHostOfPropertyChange();
   }

   public void computeDevStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {

      // get coefficients
      double c1 = getC1(def);
      double c2 = getC2(def);
      
      double J = def.getDetF();

      SymmetricMatrix3d B = new SymmetricMatrix3d();
      SymmetricMatrix3d B2 = new SymmetricMatrix3d();
      computeDevLeftCauchyGreen (B,def);
      B2.mulTransposeLeft (B); // compute B*B

      double I1 = B.trace();
      double I2 = 0.5*(I1*I1 - B2.trace());

      double W1 = c1*c2*Math.exp(c2*(I1-3));
      double W2 = -0.5*c1*c2;

      sigma.scale (W1 + W2*I1, B);
      sigma.scaledAdd (-W2, B2);
      sigma.deviator();
      sigma.scale (2/J);

      if (D != null) {
         double W11 = c2*W1;
         double WC = W1*I1 + 2*W2*I2;
         double CWWC = W11*I1*I1 + 2*I2*W2;
         double Jinv = 1/J;

         SymmetricMatrix3d WCCxC = new SymmetricMatrix3d();
         WCCxC.scale (I1*(W11 + W2), B);
         WCCxC.scaledAdd (-W2, B2);
         
         D.setZero();

         // D = BxB*((W11 + W2)*4.0*Ji) - B4*(W2*4.0*Ji);
         TensorUtils.addTensorProduct (D, (W11 + W2)*4.0*Jinv, B);
         TensorUtils.addTensorProduct4 (D, -W2*4*Jinv, B);

         // D += -dyad1s(WCCxC, I)*(4.0/3.0*Ji) + IxI*(4.0/9.0*Ji*CWWC);
         TensorUtils.addSymmetricIdentityProduct (D, -4/3.0*Jinv, WCCxC);
         TensorUtils.addScaledIdentityProduct (D, 4.0/9.0*Jinv*CWWC);

         // D += (I4 - IxI/3.0)*(4.0/3.0*Ji*WC)
         TensorUtils.addScaledIdentity (D, 4.0/3*Jinv*WC);
         TensorUtils.addScaledIdentityProduct (D, -4.0/9*Jinv*WC);
         // D +=  dyad1s(devs, I)*(-2.0/3.0)
         TensorUtils.addSymmetricIdentityProduct (D, -2/3.0, sigma);
         D.setLowerToUpper();         
      }
   }

   public double computeDevStrainEnergy (
      DeformedPoint def, Matrix3d Q, double excitation, 
      MaterialStateObject state) {

      // get coefficients
      double c1 = getC1(def);
      double c2 = getC2(def);

      // compute the invariants
      SymmetricMatrix3d Cdev = new SymmetricMatrix3d();
      SymmetricMatrix3d Cdev2 = new SymmetricMatrix3d();
      computeDevRightCauchyGreen (Cdev, def);
      double I1 = Cdev.trace();
      Cdev2.mulTransposeLeft (Cdev);
      double I2 = 0.5*(I1*I1 - Cdev2.trace());

      double exp = Math.exp (c2*(I1-3));
      double W = c1*(exp-1) - c1*c2*(I2-3)/2.0;
      return W;
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof VerondaWestmannMaterial)) {
         return false;
      }
      VerondaWestmannMaterial vwm = (VerondaWestmannMaterial)mat;
      if (myC1 != vwm.myC1 || myC2 != vwm.myC2) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public VerondaWestmannMaterial clone() {
      VerondaWestmannMaterial mat = (VerondaWestmannMaterial)super.clone();
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         myC1 /= s;         
      }
      // C2 is unitless
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         myC1 *= s;
      }
      // C2 is unitless
   }
}
