package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

/**
 * Implements a Yeoh hyperelastic material with up to five coefficients.
 */
public class YeohMaterial extends IncompressibleMaterialBase {
   protected static FieldPropertyList myProps =
      new FieldPropertyList (YeohMaterial.class,IncompressibleMaterialBase.class);
   
   protected static double DEFAULT_C1 = 150000;
   protected static double DEFAULT_C2 = 0;
   protected static double DEFAULT_C3 = 0;
   protected static double DEFAULT_C4 = 0;
   protected static double DEFAULT_C5 = 0;

   private double myC1 = DEFAULT_C1;
   private double myC2 = DEFAULT_C2;
   private double myC3 = DEFAULT_C3;
   private double myC4 = DEFAULT_C4;
   private double myC5 = DEFAULT_C5;

   PropertyMode myC1Mode = PropertyMode.Inherited;
   PropertyMode myC2Mode = PropertyMode.Inherited;
   PropertyMode myC3Mode = PropertyMode.Inherited;
   PropertyMode myC4Mode = PropertyMode.Inherited;
   PropertyMode myC5Mode = PropertyMode.Inherited;

   ScalarFieldComponent myC1Field = null;
   ScalarFieldComponent myC2Field = null;
   ScalarFieldComponent myC3Field = null;
   ScalarFieldComponent myC4Field = null;
   ScalarFieldComponent myC5Field = null;

   private SymmetricMatrix3d myB;
   private SymmetricMatrix3d myTmp;

   static {
      myProps.addInheritableWithField (
         "C1:Inherited", "C1 parameter", DEFAULT_C1);
      myProps.addInheritableWithField (
         "C2:Inherited", "C2 parameter", DEFAULT_C2);
      myProps.addInheritableWithField (
	 "C3:Inherited", "C3 parameter", DEFAULT_C3);
      myProps.addInheritableWithField (
	 "C4:Inherited", "C4 parameter", DEFAULT_C4);
      myProps.addInheritableWithField (
	 "C5:Inherited", "C5 parameter", DEFAULT_C5);
   }

   public FieldPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public YeohMaterial (){
      myB = new SymmetricMatrix3d();
      myTmp = new SymmetricMatrix3d();
   }

   public YeohMaterial (
      double c1, double c2, double c3, double kappa) {
      this();
      setC1 (c1);
      setC2 (c2);
      setC3 (c3);
      setBulkModulus (kappa);
   }

   public YeohMaterial (
      double c1, double c2, double c3,
      double c4, double c5, double kappa) {
      this();
      setC1 (c1);
      setC2 (c2);
      setC3 (c3);
      setC4 (c4);
      setC5 (c5);
      setBulkModulus (kappa);
   }

   public synchronized void setC1 (double c1) {
      myC1 = c1;
      myC1Mode =
         PropertyUtils.propagateValue (this, "C1", myC1, myC1Mode);
      notifyHostOfPropertyChange();
   }

   // begin C accessors

   // C1

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

   // C2

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

   // C3
   
   public synchronized void setC3 (double c3) {
      myC3 = c3;
      myC3Mode =
         PropertyUtils.propagateValue (this, "C3", myC3, myC3Mode);
      notifyHostOfPropertyChange();
   }

   public double getC3() {
      return myC3;
   }

   public void setC3Mode (PropertyMode mode) {
      myC3Mode =
         PropertyUtils.setModeAndUpdate (this, "C3", myC3Mode, mode);
   }

   public PropertyMode getC3Mode() {
      return myC3Mode;
   }

   public double getC3 (FemFieldPoint dp) {
      if (myC3Field == null) {
         return getC3();
      }
      else {
         return myC3Field.getValue (dp);
      }
   }

   public ScalarFieldComponent getC3Field() {
      return myC3Field;
   }
      
   public void setC3Field (ScalarFieldComponent func) {
      myC3Field = func;
      notifyHostOfPropertyChange();
   }

   // C4
   
   public synchronized void setC4 (double c4) {
      myC4 = c4;
      myC4Mode =
         PropertyUtils.propagateValue (this, "C4", myC4, myC4Mode);
      notifyHostOfPropertyChange();
   }

   public double getC4() {
      return myC4;
   }

   public void setC4Mode (PropertyMode mode) {
      myC4Mode =
         PropertyUtils.setModeAndUpdate (this, "C4", myC4Mode, mode);
   }

   public PropertyMode getC4Mode() {
      return myC4Mode;
   }

   public double getC4 (FemFieldPoint dp) {
      if (myC4Field == null) {
         return getC4();
      }
      else {
         return myC4Field.getValue (dp);
      }
   }

   public ScalarFieldComponent getC4Field() {
      return myC4Field;
   }
      
   public void setC4Field (ScalarFieldComponent func) {
      myC4Field = func;
      notifyHostOfPropertyChange();
   }

   // C5
   
   public synchronized void setC5 (double c5) {
      myC5 = c5;
      myC5Mode =
         PropertyUtils.propagateValue (this, "C5", myC5, myC5Mode);
      notifyHostOfPropertyChange();
   }

   public double getC5() {
      return myC5;
   }

   public void setC5Mode (PropertyMode mode) {
      myC5Mode =
         PropertyUtils.setModeAndUpdate (this, "C5", myC5Mode, mode);
   }

   public PropertyMode getC5Mode() {
      return myC5Mode;
   }

   public double getC5 (FemFieldPoint dp) {
      if (myC5Field == null) {
         return getC5();
      }
      else {
         return myC5Field.getValue (dp);
      }
   }

   public ScalarFieldComponent getC5Field() {
      return myC5Field;
   }
      
   public void setC5Field (ScalarFieldComponent func) {
      myC5Field = func;
      notifyHostOfPropertyChange();
   }

   // end C accessors

   // public double computeDeviatoricEnergy (Matrix3dBase Cdev) {

   //    double I1 = Cdev.trace();
   //    double I1_3 = I1-3;
   //    double W = (myC1*I1_3 +
   //                myC2*I1_3*I1_3 + 
   //                myC3*I1_3*I1_3*I1_3);
   //    return W;
   // }
      
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
      // W = C1*(I1-3) + C2*(I1-3)^2 + C3*(I1-3)^3
      //
      // Wi = dW/dIi
      double C1 = getC1(def);
      double C2 = getC2(def);
      double C3 = getC3(def);
      double C4 = getC4(def);
      double C5 = getC5(def);

      double I1_3 = (I1-3);
      
      double W1 = (((5*C5*I1_3 + 4*C4)*I1_3 + 3*C3)*I1_3 + 2*C2)*I1_3 + C1;
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
         double W11 = ((20*C5*I1_3 + 12*C4)*I1_3 + 6*C3)*I1_3 + 2*C2;

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

   public double computeDevStrainEnergy (
      DeformedPoint def, Matrix3d Q, double excitation, 
      MaterialStateObject state) {

      double C1 = getC1(def);
      double C2 = getC2(def);
      double C3 = getC3(def);
      double C4 = getC4(def);
      double C5 = getC5(def);

      SymmetricMatrix3d Cdev = new SymmetricMatrix3d();
      computeDevRightCauchyGreen (Cdev, def);
      double I1 = Cdev.trace();
      double I1_3 = I1-3;
      double W = ((((C5*I1_3 + C4)*I1_3 + C3)*I1_3 + C2)*I1_3 + C1)*I1_3;
      return W;
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof YeohMaterial)) {
         return false;
      }
      YeohMaterial mrm = (YeohMaterial)mat;
      if (myC1 != mrm.myC1 ||
          myC2 != mrm.myC2 ||
          myC3 != mrm.myC3 ||
          myC4 != mrm.myC4 ||
          myC5 != mrm.myC5) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public YeohMaterial clone() {
      YeohMaterial mat = (YeohMaterial)super.clone();
      mat.myB = new SymmetricMatrix3d();
      mat.myTmp = new SymmetricMatrix3d();
      return mat;
   }

   public static void main (String[] args) {
      YeohMaterial mat = new YeohMaterial();

      Matrix3d Q = new Matrix3d();

      DeformedPointBase dpnt = new DeformedPointBase();
      dpnt.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));
     
      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      mat.setC1 (1.2);
      mat.setC3 (3.5);
      mat.setC2 (1.9);
      mat.setBulkModulus (0.0);
      mat.computeStressAndTangent (sig, D, dpnt, Q, 1.0, null);

      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      System.out.println ("D=\n" + D.toString ("%12.6f"));

   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         setC1 (myC1/s);
         setC2 (myC2/s);
         setC3 (myC3/s);
         setC4 (myC4/s);
         setC5 (myC5/s);
      }
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         setC1 (myC1*s);
         setC2 (myC2*s);
         setC3 (myC3*s);
         setC4 (myC4*s);
         setC5 (myC5*s);
      }
   }


}
