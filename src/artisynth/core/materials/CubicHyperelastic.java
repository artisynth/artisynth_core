package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class CubicHyperelastic extends IncompressibleMaterialBase {
   protected static FunctionPropertyList myProps =
      new FunctionPropertyList (CubicHyperelastic.class,IncompressibleMaterialBase.class);
   
   protected static double DEFAULT_G10 = 150000;
   protected static double DEFAULT_G20 = 0;
   protected static double DEFAULT_G30 = 0;

   private double myG10 = DEFAULT_G10;
   private double myG20 = DEFAULT_G20;
   private double myG30 = DEFAULT_G30;
   PropertyMode myG10Mode = PropertyMode.Inherited;
   PropertyMode myG30Mode = PropertyMode.Inherited;
   PropertyMode myG20Mode = PropertyMode.Inherited;
   ScalarFieldPointFunction myG10Function = null;
   ScalarFieldPointFunction myG30Function = null;
   ScalarFieldPointFunction myG20Function = null;

   private SymmetricMatrix3d myB;
   private SymmetricMatrix3d myTmp;

   static {
      myProps.addInheritableWithFunction (
         "G10:Inherited", "G10 parameter", DEFAULT_G10);
      myProps.addInheritableWithFunction (
         "G20:Inherited", "G20 parameter", DEFAULT_G20);
      myProps.addInheritableWithFunction (
	 "G30:Inherited", "G30 parameter", DEFAULT_G30);
   }

   public FunctionPropertyList getAllPropertyInfo() {
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

   // begin G accessors

   // G10

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

   public double getG10 (FieldPoint dp) {
      if (myG10Function == null) {
         return getG10();
      }
      else {
         return myG10Function.eval (dp);
      }
   }

   public ScalarFieldPointFunction getG10Function() {
      return myG10Function;
   }
      
   public void setG10Function (ScalarFieldPointFunction func) {
      myG10Function = func;
      notifyHostOfPropertyChange();
   }
   
   public void setG10Field (
      ScalarField field, boolean useRestPos) {
      myG10Function = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getG10Field () {
      return FieldUtils.getFieldFromFunction (myG10Function);
   }

   // G20

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

   public double getG20 (FieldPoint dp) {
      if (myG20Function == null) {
         return getG20();
      }
      else {
         return myG20Function.eval (dp);
      }
   }

   public ScalarFieldPointFunction getG20Function() {
      return myG20Function;
   }
      
   public void setG20Function (ScalarFieldPointFunction func) {
      myG20Function = func;
      notifyHostOfPropertyChange();
   }
   
   public void setG20Field (
      ScalarField field, boolean useRestPos) {
      myG20Function = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getG20Field () {
      return FieldUtils.getFieldFromFunction (myG20Function);
   }

   // G30
   
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

   public double getG30 (FieldPoint dp) {
      if (myG30Function == null) {
         return getG30();
      }
      else {
         return myG30Function.eval (dp);
      }
   }

   public ScalarFieldPointFunction getG30Function() {
      return myG30Function;
   }
      
   public void setG30Function (ScalarFieldPointFunction func) {
      myG30Function = func;
      notifyHostOfPropertyChange();
   }
   
   public void setG30Field (
      ScalarField field, boolean useRestPos) {
      myG30Function = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getG30Field () {
      return FieldUtils.getFieldFromFunction (myG30Function);
   }

   // end G accessors

   public double computeDeviatoricEnergy (Matrix3dBase Cdev) {
      double I1 = Cdev.trace();
      double I1_3 = I1-3;
      double W = (myG10*I1_3 +
                  myG20*I1_3*I1_3 + 
                  myG30*I1_3*I1_3*I1_3);
      return W;
   }
      
   public void computeDevStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) { 

      double J = def.getDetF();

      // calculate deviatoric left Cauchy-Green tensor
      computeDevLeftCauchyGreen(myB,def);

      // Invariants of B (= invariants of C)
      // Note that these are the invariants of Btilde, not of B!
      double I1 = myB.trace();

      //
      // W = G10*(I1-3) + G20*(I1-3)^2 + G30*(I1-3)^3
      //
      // Wi = dW/dIi
      double G10 = getG10(def);
      double G20 = getG20(def);
      double G30 = getG30(def);
      
      double W1 = G10 + G20*2*(I1-3) + G30*3*(I1-3)*(I1-3);
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

      if (D != null) {
         double Ji = 1.0/J;

         // Wij = d2W/dIidIj
         double W11 = 2*G20 + + G30*6*(I1-3);

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

         TensorUtils.addScaledIdentityProduct (D, 4.0/9.0*Ji*(wcc-w0));
         TensorUtils.addScaledIdentity (D, 4.0/3.0*Ji*w0);

         myTmp.deviator (sigma); // need to call this???
         TensorUtils.addSymmetricTensorProduct (
            D, -2.0/3.0, myTmp, SymmetricMatrix3d.IDENTITY);

         TensorUtils.addTensorProduct (D, w2*4.0*Ji, myB);

         myTmp.scale (wc1, myB);  
         TensorUtils.addSymmetricTensorProduct (
            D, -4.0/3.0*Ji,myTmp,SymmetricMatrix3d.IDENTITY);

         D.setLowerToUpper();
      }
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

      Matrix3d Q = new Matrix3d();

      DeformedPointBase dpnt = new DeformedPointBase();
      dpnt.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));
     
      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      mat.setG10 (1.2);
      mat.setG30 (3.5);
      mat.setG20 (1.9);
      mat.setBulkModulus (0.0);
      mat.computeStressAndTangent (sig, D, dpnt, Q, 1.0, null);

      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      System.out.println ("D=\n" + D.toString ("%12.6f"));

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
