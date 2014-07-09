package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class CubicHyperelastic extends IncompressibleMaterial {
   protected static PropertyList myProps =
      new PropertyList (CubicHyperelastic.class, IncompressibleMaterial.class);
   
   protected static double DEFAULT_G10 = 150000;
   protected static double DEFAULT_G20 = 0;
   protected static double DEFAULT_G30 = 0;

   private double myG10 = DEFAULT_G10;
   private double myG20 = DEFAULT_G20;
   private double myG30 = DEFAULT_G30;

   PropertyMode myG10Mode = PropertyMode.Inherited;
   PropertyMode myG30Mode = PropertyMode.Inherited;
   PropertyMode myG20Mode = PropertyMode.Inherited;

   private SymmetricMatrix3d myB;
   private SymmetricMatrix3d myTmp;

   static {
      myProps.addInheritable (
         "G10:Inherited", "G10 parameter", DEFAULT_G10);
      myProps.addInheritable (
         "G20:Inherited", "G20 parameter", DEFAULT_G20);
      myProps.addInheritable (
	 "G30:Inherited", "G30 parameter", DEFAULT_G30);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public CubicHyperelastic (){
      myB = new SymmetricMatrix3d();
      myTmp = new SymmetricMatrix3d();
   }

   public CubicHyperelastic (
      double G10, double G20, double G30, double kappa) {
      this();
      setG10 (G10);
      setG20 (G20);
      setG30 (G30);
      setBulkModulus (kappa);
   }

   public synchronized void setG10 (double nu) {
      myG10 = nu;
      myG10Mode =
         PropertyUtils.propagateValue (this, "G10", myG10, myG10Mode);
      notifyHostOfPropertyChange();
   }

   public double getG10() {
      return myG10;
   }

   public void setG10Mode (PropertyMode mode) {
      myG10Mode =
         PropertyUtils.setModeAndUpdate (this, "G10", myG10Mode, mode);
   }

   public PropertyMode getG10Mode() {
      return myG10Mode;
   }


   public synchronized void setG20 (double nu) {
      myG20 = nu;
      myG20Mode =
         PropertyUtils.propagateValue (this, "G20", myG20, myG20Mode);
      notifyHostOfPropertyChange();
   }

   public double getG20() {
      return myG20;
   }

   public void setG20Mode (PropertyMode mode) {
      myG20Mode =
         PropertyUtils.setModeAndUpdate (this, "G20", myG20Mode, mode);
   }

   public PropertyMode getG20Mode() {
      return myG20Mode;
   }

   
   public synchronized void setG30 (double nu) {
      myG30 = nu;
      myG30Mode =
         PropertyUtils.propagateValue (this, "G30", myG30, myG30Mode);
      notifyHostOfPropertyChange();
   }

   public double getG30() {
      return myG30;
   }

   public void setG30Mode (PropertyMode mode) {
      myG30Mode =
         PropertyUtils.setModeAndUpdate (this, "G30", myG30Mode, mode);
   }

   public PropertyMode getG30Mode() {
      return myG30Mode;
   }


   public double computeDeviatoricEnergy (Matrix3dBase Cdev) {
      double I1 = Cdev.trace();
      double I1_3 = I1-3;
      double W = (myG10*I1_3 +
                  myG20*I1_3*I1_3 + 
                  myG30*I1_3*I1_3*I1_3);
      return W;
   }
      
   public void computeStress (
      SymmetricMatrix3d sigma, SolidDeformation def, Matrix3d Q,
      FemMaterial baseMat) {

      double J = def.getDetF();
      double avgp = def.getAveragePressure();

      // calculate deviatoric left Cauchy-Green tensor
      def.computeDevLeftCauchyGreen(myB);

      // Invariants of B (= invariants of C)
      // Note that these are the invariants of Btilde, not of B!
      double I1 = myB.trace();

      //
      // W = G10*(I1-3) + G20*(I1-3)^2 + G30*(I1-3)^3
      //
      // Wi = dW/dIi
      double W1 = myG10 + myG20*2*(I1-3) + myG30*3*(I1-3)*(I1-3);
      // ---

      // calculate T = F*dW/dC*Ft
      // T = F*dW/dC*Ft
      // 
      //   mat3ds T = B*(W1 + W2*I1) - B2*W2;
      sigma.scale (W1, myB);

      // calculate stress: s = pI + (2/J)dev[T]
      // 
      // mat3ds s = mat3dd(def.avgp) + T.dev()*(2.0/J);

      sigma.deviator();
      sigma.scale (2.0/J);

      sigma.m00 += avgp;
      sigma.m11 += avgp;
      sigma.m22 += avgp;
   }

   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def, 
      Matrix3d Q, FemMaterial baseMat) {

      double J = def.getDetF();
      double Ji = 1.0/J;

      // calculate deviatoric left Cauchy-Green tensor
      def.computeDevLeftCauchyGreen(myB);

      // Invariants of B (= invariants of C)
      double I1 = myB.trace();

      // --- TODO: put strain energy derivatives here ---
      //
      // W = G10*(I1-3) + G20*(I1-3)^2 + G30*(I1-3)^3
      //
      // Wi = dW/dIi
      // Wij = d2W/dIidIj
      double W1;
      double W11;

      W1 = myG10 + myG20*2*(I1-3) + myG30*3*(I1-3)*(I1-3);
      W11 = 2*myG20 + + myG30*6*(I1-3);

      // parameters as defined in John Lloyd's "FEM notes" paper:

      double w2 = W11;

      double wc1 = (w2)*I1;
      
      double wcc = wc1*I1;
      double w0 = W1*I1;

      // ---

      // calculate dWdC:C
      // double WC = W1*I1;;

      // mean pressure
      double p = def.getAveragePressure();      


      D.setZero();

      TensorUtils.addScaledIdentityProduct (D, p + 4.0/9.0*Ji*(wcc-w0));
      TensorUtils.addScaledIdentity (D, -2*p + 4.0/3.0*Ji*w0);

      myTmp.deviator (stress);
      TensorUtils.addSymmetricTensorProduct (
         D, -2.0/3.0, myTmp, SymmetricMatrix3d.IDENTITY);

      TensorUtils.addTensorProduct (D, w2*4.0*Ji, myB);

      myTmp.scale (wc1, myB);  
      TensorUtils.addSymmetricTensorProduct (
         D, -4.0/3.0*Ji,myTmp,SymmetricMatrix3d.IDENTITY);

      D.setLowerToUpper();
      
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof CubicHyperelastic)) {
         return false;
      }
      CubicHyperelastic mrm = (CubicHyperelastic)mat;
      if (myG10 != mrm.myG10 ||
          myG30 != mrm.myG30 ||
          myG20 != mrm.myG20) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public CubicHyperelastic clone() {
      CubicHyperelastic mat = (CubicHyperelastic)super.clone();
      mat.myB = new SymmetricMatrix3d();
      mat.myTmp = new SymmetricMatrix3d();
      return mat;
   }

   public static void main (String[] args) {
      CubicHyperelastic mat = new CubicHyperelastic();

      SolidDeformation def = new SolidDeformation();
      Matrix3d Q = new Matrix3d();
      def.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      mat.setG10 (1.2);
      mat.setG30 (3.5);
      mat.setG20 (1.9);
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
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         setG10 (myG10/s);
         setG20 (myG20/s);
         setG30 (myG30/s);
      }
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         setG10 (myG10*s);
         setG20 (myG20*s);
         setG30 (myG30*s);
      }
   }


}
