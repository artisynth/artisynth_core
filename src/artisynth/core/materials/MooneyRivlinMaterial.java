package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class MooneyRivlinMaterial extends IncompressibleMaterialBase {
   protected static FunctionPropertyList myProps =
      new FunctionPropertyList (
         MooneyRivlinMaterial.class, IncompressibleMaterialBase.class);
   
   /**
    * With regard to the typical two-parameter (c1, c2) Mooney-Rivlin material,
    * we have c1 = C10 and c2 = C01.
    */

   protected static boolean usePhi = true;
   
   protected static double DEFAULT_C10 = 150000;
   protected static double DEFAULT_C01 = 0;
   protected static double DEFAULT_C20 = 0;
   protected static double DEFAULT_C02 = 0;
   protected static double DEFAULT_C11 = 0;
   protected static double DEFAULT_JLIMIT = 0;

   private double myC10 = DEFAULT_C10;
   private double myC01 = DEFAULT_C01;
   private double myC11 = DEFAULT_C11;
   private double myC20 = DEFAULT_C20;
   private double myC02 = DEFAULT_C02;
   private double myJLimit = DEFAULT_JLIMIT;

   PropertyMode myC10Mode = PropertyMode.Inherited;
   PropertyMode myC01Mode = PropertyMode.Inherited;
   PropertyMode myC11Mode = PropertyMode.Inherited;
   PropertyMode myC20Mode = PropertyMode.Inherited;
   PropertyMode myC02Mode = PropertyMode.Inherited;
   PropertyMode myJLimitMode = PropertyMode.Inherited;

   ScalarFieldPointFunction myC10Function = null;
   ScalarFieldPointFunction myC01Function = null;
   ScalarFieldPointFunction myC11Function = null;
   ScalarFieldPointFunction myC20Function = null;
   ScalarFieldPointFunction myC02Function = null;

   private SymmetricMatrix3d myB;
   private SymmetricMatrix3d myB2;
   private SymmetricMatrix3d myTmp;

   // storage for phi and it's first two derivatives
   private double[] myPhiVals = new double[3];

   static {
      myProps.addInheritableWithFunction (
         "C10:Inherited", "C10 parameter", DEFAULT_C10);
      myProps.addInheritableWithFunction (
         "C01:Inherited", "C01 parameter", DEFAULT_C01);
      myProps.addInheritableWithFunction (
         "C11:Inherited", "C11 parameter", DEFAULT_C11);
      myProps.addInheritableWithFunction (
         "C20:Inherited", "C20 parameter", DEFAULT_C20);
      myProps.addInheritableWithFunction (
         "C02:Inherited", "C02 parameter", DEFAULT_C02);
      myProps.addInheritable (
         "JLimit:Inherited",
         "value of J below which incompressiblity is regularized",
         DEFAULT_JLIMIT);
   }

   public FunctionPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MooneyRivlinMaterial (){
      myB = new SymmetricMatrix3d();
      myB2 = new SymmetricMatrix3d();
      myTmp = new SymmetricMatrix3d();
   }

   public MooneyRivlinMaterial (
      double c10, double c01, double c11, double c20, double c02,
      double kappa) {
      this();
      setC10 (c10);
      setC01 (c01);
      setC11 (c11);
      setC20 (c20);
      setC02 (c02);
      setBulkModulus (kappa);
   }

   // BEGIN parameter accessors

   // C10 

   public synchronized void setC10 (double c10) {
      myC10 = c10;
      myC10Mode =
         PropertyUtils.propagateValue (this, "C10", myC10, myC10Mode);
      notifyHostOfPropertyChange();
   }

   public double getC10() {
      return myC10;
   }

   public void setC10Mode (PropertyMode mode) {
      myC10Mode =
         PropertyUtils.setModeAndUpdate (this, "C10", myC10Mode, mode);
   }

   public PropertyMode getC10Mode() {
      return myC10Mode;
   }

   public double getC10 (FieldPoint dp) {
      return (myC10Function == null ? getC10() : myC10Function.eval (dp));
   }

   public ScalarFieldPointFunction getC10Function() {
      return myC10Function;
   }
      
   public void setC10Function (ScalarFieldPointFunction func) {
      myC10Function = func;
      notifyHostOfPropertyChange();
   }
   
   public void setC10Field (
      ScalarField field, boolean useRestPos) {
      myC10Function = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getC10Field () {
      return FieldUtils.getFieldFromFunction (myC10Function);
   }

   // C01

   public synchronized void setC01 (double c01) {
      myC01 = c01;
      myC01Mode =
         PropertyUtils.propagateValue (this, "C01", myC01, myC01Mode);
      notifyHostOfPropertyChange();
   }

   public double getC01() {
      return myC01;
   }

   public void setC01Mode (PropertyMode mode) {
      myC01Mode =
         PropertyUtils.setModeAndUpdate (this, "C01", myC01Mode, mode);
   }

   public PropertyMode getC01Mode() {
      return myC01Mode;
   }

   public double getC01 (FieldPoint dp) {
      return (myC01Function == null ? getC01() : myC01Function.eval (dp));
   }

   public ScalarFieldPointFunction getC01Function() {
      return myC01Function;
   }
      
   public void setC01Function (ScalarFieldPointFunction func) {
      myC01Function = func;
      notifyHostOfPropertyChange();
   }
   
   public void setC01Field (
      ScalarField field, boolean useRestPos) {
      myC01Function = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getC01Field () {
      return FieldUtils.getFieldFromFunction (myC01Function);
   }

   // C11

   public synchronized void setC11 (double c11) {
      myC11 = c11;
      myC11Mode =
         PropertyUtils.propagateValue (this, "C11", myC11, myC11Mode);
      notifyHostOfPropertyChange();
   }

   public double getC11() {
      return myC11;
   }

   public void setC11Mode (PropertyMode mode) {
      myC11Mode =
         PropertyUtils.setModeAndUpdate (this, "C11", myC11Mode, mode);
   }

   public PropertyMode getC11Mode() {
      return myC11Mode;
   }

   public double getC11 (FieldPoint dp) {
      return (myC11Function == null ? getC11() : myC11Function.eval (dp));
   }

   public ScalarFieldPointFunction getC11Function() {
      return myC11Function;
   }
      
   public void setC11Function (ScalarFieldPointFunction func) {
      myC11Function = func;
      notifyHostOfPropertyChange();
   }
   
   public void setC11Field (
      ScalarField field, boolean useRestPos) {
      myC11Function = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getC11Field () {
      return FieldUtils.getFieldFromFunction (myC11Function);
   }

   // C20

   public synchronized void setC20 (double c20) {
      myC20 = c20;
      myC20Mode =
         PropertyUtils.propagateValue (this, "C20", myC20, myC20Mode);
      notifyHostOfPropertyChange();
   }

   public double getC20() {
      return myC20;
   }

   public void setC20Mode (PropertyMode mode) {
      myC20Mode =
         PropertyUtils.setModeAndUpdate (this, "C20", myC20Mode, mode);
   }

   public PropertyMode getC20Mode() {
      return myC20Mode;
   }

   public double getC20 (FieldPoint dp) {
      return (myC20Function == null ? getC20() : myC20Function.eval (dp));
   }

   public ScalarFieldPointFunction getC20Function() {
      return myC20Function;
   }
      
   public void setC20Function (ScalarFieldPointFunction func) {
      myC20Function = func;
      notifyHostOfPropertyChange();
   }
   
   public void setC20Field (
      ScalarField field, boolean useRestPos) {
      myC20Function = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getC20Field () {
      return FieldUtils.getFieldFromFunction (myC20Function);
   }

   // C02

   public synchronized void setC02 (double c02) {
      myC02 = c02;
      myC02Mode =
         PropertyUtils.propagateValue (this, "C02", myC02, myC02Mode);
      notifyHostOfPropertyChange();
   }

   public double getC02() {
      return myC02;
   }

   public void setC02Mode (PropertyMode mode) {
      myC02Mode =
         PropertyUtils.setModeAndUpdate (this, "C02", myC02Mode, mode);
   }

   public PropertyMode getC02Mode() {
      return myC02Mode;
   }

   public double getC02 (FieldPoint dp) {
      return (myC02Function == null ? getC02() : myC02Function.eval (dp));
   }

   public ScalarFieldPointFunction getC02Function() {
      return myC02Function;
   }
      
   public void setC02Function (ScalarFieldPointFunction func) {
      myC02Function = func;
      notifyHostOfPropertyChange();
   }
   
   public void setC02Field (
      ScalarField field, boolean useRestPos) {
      myC02Function = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getC02Field () {
      return FieldUtils.getFieldFromFunction (myC02Function);
   }

   // JLimit

   public synchronized void setJLimit (double JLimit) {
      if (myJLimit != JLimit) {
         myJLimit = JLimit;
         
      }
      myJLimitMode =
         PropertyUtils.propagateValue (this, "JLimit", myJLimit, myJLimitMode);
      notifyHostOfPropertyChange();
   }

   public double getJLimit() {
      return myJLimit;
   }

   public void setJLimitMode (PropertyMode mode) {
      myJLimitMode =
         PropertyUtils.setModeAndUpdate (this, "JLimit", myJLimitMode, mode);
   }

   public PropertyMode getJLimitMode() {
      return myJLimitMode;
   }

   // END parameter accessors

   public void computePhiVals (double[] vals, double J) {
      if (myJLimit > 0 && J < myJLimit) {
         double phi0 = 1/Math.pow (myJLimit, 2.0/3.0);
         double dphi0 = -(2.0/3.0)*phi0/myJLimit;
         double ddphi0 = -(5.0/3.0)*dphi0/myJLimit;
         double delJ = J - myJLimit;
         vals[0] = (0.5*ddphi0*delJ + dphi0)*delJ + phi0;
         vals[1] = ddphi0*delJ + dphi0;
         vals[2] = ddphi0;
      }
      else {
         double phi = 1/Math.pow (J, 2.0/3.0);
         double dphi = -(2.0/3.0)*phi/J;
         vals[0] = phi;
         vals[1] = dphi;
         vals[2] = -(5.0/3.0)*dphi/J;
      }
   }

   public double computeDeviatoricEnergy (Matrix3dBase Cdev) {
      double I1 = Cdev.trace();
      myTmp.mulTransposeLeft (Cdev);
      double I2 = 0.5*(I1*I1 - myTmp.trace());
      double I1_3 = I1-3;
      double I2_3 = I2-3;
      double W = (myC10*I1_3 + myC01*I2_3 + myC11*I1_3*I2_3 +
                  myC20*I1_3*I1_3 + myC02*I2_3*I2_3);
      return W;
   }

   public void computeDevStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {   

      // Methods and naming conventions follow the paper "Finite element
      // implementation of incompressible, isotropic hyperelasticity", by
      // Weiss, Makerc, and Govindjeed, Computer Methods in Applied Mechanical
      // Engineering, 1996.

      double J = def.getDetF();

      computePhiVals (myPhiVals, J);
      double phi = myPhiVals[0];
      double dphi = myPhiVals[1];

      computeLeftCauchyGreen(myB,def);
      // scale to compute deviatoric part; use phi in place of pow(J,-2/3);
      myB.scale (phi);
      myB2.mulTransposeLeft (myB); // compute B*B

      double c10 = getC10(def);
      double c01 = getC01(def);
      double c11 = getC11(def);
      double c20 = getC20(def);
      double c02 = getC02(def);
      
      double I1 = myB.trace();
      double I2 = 0.5*(I1*I1 - myB2.trace());

      double W1 = c10 + c11*(I2-3) + c20*2*(I1-3);
      double W2 = c01 + c11*(I1-3) + c02*2*(I2-3);

      sigma.scale (W1 + W2*I1, myB);
      sigma.scaledAdd (-W2, myB2, sigma);

      double dev = (dphi/phi)*sigma.trace();
      sigma.scale (2.0/J);
      sigma.m00 += dev;
      sigma.m11 += dev;
      sigma.m22 += dev;

      if (D != null) {

         double Ji = 1.0/J;
         double ddphi = myPhiVals[2];

         double W11 = 2*c20;
         double W12 = c11;
         double W22 = 2*c02;

         // parameters as defined in John Lloyd's "FEM notes" paper:

         double w1 = -W2;
         double w2 = W11 + 2*W12*I1 + W2 + W22*I2*I2;
         double w3 = W12 + W22*I1;
         double w4 = W22;

         double wc1 = (w2 - W12 + W22*I1)*I1;
         double wc2 = -(W12 + W22*I1 - W22*I1*I1 + 2*W22*I2 + W2);
      
         double wcc = wc1*I1 + wc2*(I1*I1-2*I2);
         double w0 = W1*I1 + 2*W2*I2;

         double p = def.getAveragePressure();      

         D.setZero();

         double zeta = ((dphi+J*ddphi)*w0 + J*dphi*dphi/phi*(wcc-2*w0))/phi;
         double r = dphi/phi;
         TensorUtils.addScaledIdentityProduct (D, zeta);
         TensorUtils.addScaledIdentity (D, -2*r*w0);
      
         TensorUtils.addSymmetricTensorProduct (
            D, J*r, sigma, SymmetricMatrix3d.IDENTITY);

         TensorUtils.addTensorProduct4 (D, w1*4.0*Ji, myB);
         TensorUtils.addTensorProduct (D, w2*4.0*Ji, myB);
         TensorUtils.addSymmetricTensorProduct (D, w3*4.0*Ji, myB, myB2);
         TensorUtils.addTensorProduct (D, w4*4.0*Ji, myB2);

         myTmp.scale (wc1, myB);  
         myTmp.scaledAdd (wc2, myB2);
         TensorUtils.addSymmetricTensorProduct (
            D, 2*r,myTmp,SymmetricMatrix3d.IDENTITY);

         D.setLowerToUpper();
      }
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof MooneyRivlinMaterial)) {
         return false;
      }
      MooneyRivlinMaterial mrm = (MooneyRivlinMaterial)mat;
      if (myC10 != mrm.myC10 ||
          myC01 != mrm.myC01 ||
          myC11 != mrm.myC11 ||
          myC20 != mrm.myC20 ||
          myC02 != mrm.myC02) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public MooneyRivlinMaterial clone() {
      MooneyRivlinMaterial mat = (MooneyRivlinMaterial)super.clone();
      mat.myB = new SymmetricMatrix3d();
      mat.myB2 = new SymmetricMatrix3d();
      mat.myTmp = new SymmetricMatrix3d();
      return mat;
   }

   public static void main (String[] args) {
      MooneyRivlinMaterial mat = new MooneyRivlinMaterial();

      Matrix3d Q = new Matrix3d();

      DeformedPointBase dpnt = new DeformedPointBase();
      dpnt.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      mat.setC10 (1.2);
      mat.setC01 (2.4);
      mat.setC11 (3.5);
      mat.setC02 (2.6);
      mat.setC20 (1.9);
      mat.setBulkModulus (0.0);
      mat.computeStressAndTangent (sig, D, dpnt, Q, 1.0, null);

      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      System.out.println ("D=\n" + D.toString ("%12.6f"));

   }

   @Override
      public boolean isInvertible () {
      return (myJLimit > 0 && super.isInvertible());
   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         setC10 (myC10/s);
         setC01 (myC01/s);
         setC11 (myC11/s);
         setC20 (myC20/s);
         setC02 (myC02/s);
      }
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         setC10 (myC10*s);
         setC01 (myC01*s);
         setC11 (myC11*s);
         setC20 (myC20*s);
         setC02 (myC02*s);
      }
   }


}
