package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class MooneyRivlinMaterial extends IncompressibleMaterial {
   protected static PropertyList myProps =
      new PropertyList (MooneyRivlinMaterial.class, IncompressibleMaterial.class);
   
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

   private SymmetricMatrix3d myB;
   private SymmetricMatrix3d myB2;
   private SymmetricMatrix3d myTmp;

   // storage for phi and it's first two derivatives
   private double[] myPhiVals = new double[3];

   static {
      myProps.addInheritable (
         "C10:Inherited", "C10 parameter", DEFAULT_C10);
      myProps.addInheritable (
         "C01:Inherited", "C01 parameter", DEFAULT_C01);
      myProps.addInheritable (
         "C11:Inherited", "C11 parameter", DEFAULT_C11);
      myProps.addInheritable (
         "C20:Inherited", "C20 parameter", DEFAULT_C20);
      myProps.addInheritable (
         "C02:Inherited", "C02 parameter", DEFAULT_C02);
      myProps.addInheritable (
         "JLimit:Inherited",
         "value of J below which incompressiblity is regularized",
         DEFAULT_JLIMIT);
   }

   public PropertyList getAllPropertyInfo() {
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
      

   public void computeStress (
      SymmetricMatrix3d sigma, SolidDeformation def, Matrix3d Q,
      FemMaterial baseMat) {

      // Methods and naming conventions follow the paper "Finite element
      // implementation of incompressible, isotropic hyperelasticity", by
      // Weiss, Makerc, and Govindjeed, Computer Methods in Applied Mechanical
      // Engineering, 1996.

      double J = def.getDetF();
      double avgp = def.getAveragePressure();

      computePhiVals (myPhiVals, J);
      double phi = myPhiVals[0];
      double dphi = myPhiVals[1];

      def.computeLeftCauchyGreen(myB);
      // scale to compute deviatoric part; use phi in place of pow(J,-2/3);
      myB.scale (phi);

      myB2.mulTransposeLeft (myB); // compute B*B

      double I1 = myB.trace();
      double I2 = 0.5*(I1*I1 - myB2.trace());

      double W1 = myC10 + myC11*(I2-3) + myC20*2*(I1-3);
      double W2 = myC01 + myC11*(I1-3) + myC02*2*(I2-3);

      sigma.scale (W1 + W2*I1, myB);
      sigma.scaledAdd (-W2, myB2, sigma);

      if (usePhi) {
         double dev = (dphi/phi)*sigma.trace();
         sigma.scale (2.0/J);
         sigma.m00 += dev;
         sigma.m11 += dev;
         sigma.m22 += dev;
      }
      else {
         sigma.deviator();
         sigma.scale (2.0/J);
      }

      sigma.m00 += avgp;
      sigma.m11 += avgp;
      sigma.m22 += avgp;
   }

   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def, 
      Matrix3d Q, FemMaterial baseMat) {

      double J = def.getDetF();
      double Ji = 1.0/J;

      computePhiVals (myPhiVals, J);
      double phi = myPhiVals[0];
      double dphi = myPhiVals[1];
      double ddphi = myPhiVals[2];

      def.computeLeftCauchyGreen(myB);
      // scale to compute deviatoric part; use phi in place of pow(J,-2/3);
      myB.scale (phi);
      myB2.mulTransposeLeft (myB);

      double I1 = myB.trace();
      double I2 = 0.5*(I1*I1 - myB2.trace());

      double W1, W2;
      double W11, W12, W22;

      W1 = myC10 + myC11*(I2-3) + myC20*2*(I1-3);
      W2 = myC01 + myC11*(I1-3) + myC02*2*(I2-3);

      W11 = 2*myC20;
      W12 = myC11;
      W22 = 2*myC02;

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

      if (usePhi) {
         double zeta = ((dphi+J*ddphi)*w0 + J*dphi*dphi/phi*(wcc-2*w0))/phi;
         double r = dphi/phi;
         TensorUtils.addScaledIdentityProduct (D, p + zeta);
         TensorUtils.addScaledIdentity (D, -2*p - 2*r*w0);

         myTmp.set (stress);
         // remove pressure from diagonal to obtain the deviatoric stress
         myTmp.m00 -= p;
         myTmp.m11 -= p;       
         myTmp.m22 -= p;
      
         TensorUtils.addSymmetricTensorProduct (
            D, J*r, myTmp, SymmetricMatrix3d.IDENTITY);

         TensorUtils.addTensorProduct4 (D, w1*4.0*Ji, myB);
         TensorUtils.addTensorProduct (D, w2*4.0*Ji, myB);
         TensorUtils.addSymmetricTensorProduct (D, w3*4.0*Ji, myB, myB2);
         TensorUtils.addTensorProduct (D, w4*4.0*Ji, myB2);

         myTmp.scale (wc1, myB);  
         myTmp.scaledAdd (wc2, myB2);
         TensorUtils.addSymmetricTensorProduct (
            D, 2*r,myTmp,SymmetricMatrix3d.IDENTITY);
      }
      else {
         TensorUtils.addScaledIdentityProduct (D, p + 4.0/9.0*Ji*(wcc-w0));
         TensorUtils.addScaledIdentity (D, -2*p + 4.0/3.0*Ji*w0);

         myTmp.deviator (stress);
         TensorUtils.addSymmetricTensorProduct (
            D, -2.0/3.0, myTmp, SymmetricMatrix3d.IDENTITY);

         TensorUtils.addTensorProduct4 (D, w1*4.0*Ji, myB);
         TensorUtils.addTensorProduct (D, w2*4.0*Ji, myB);
         TensorUtils.addSymmetricTensorProduct (D, w3*4.0*Ji, myB, myB2);
         TensorUtils.addTensorProduct (D, w4*4.0*Ji, myB2);

         myTmp.scale (wc1, myB);  
         myTmp.scaledAdd (wc2, myB2);
         TensorUtils.addSymmetricTensorProduct (
            D, -4.0/3.0*Ji,myTmp,SymmetricMatrix3d.IDENTITY);
      }

      D.setLowerToUpper();
      
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

      SolidDeformation def = new SolidDeformation();
      Matrix3d Q = new Matrix3d();
      def.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      mat.setC10 (1.2);
      mat.setC01 (2.4);
      mat.setC11 (3.5);
      mat.setC02 (2.6);
      mat.setC20 (1.9);
      mat.setBulkModulus (0.0);
      mat.computeStress (sig, def, Q, null);
      //pt.setStress (sig);
      mat.computeTangent (D, sig, def, Q, null);

      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      System.out.println ("D=\n" + D.toString ("%12.6f"));

   }

   public boolean isIncompressible() {
      return true;
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
