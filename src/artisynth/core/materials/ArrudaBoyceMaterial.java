package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class ArrudaBoyceMaterial extends IncompressibleMaterialBase {

   public static FieldPropertyList myProps =
      new FieldPropertyList (
         ArrudaBoyceMaterial.class, IncompressibleMaterialBase.class);

   protected static double DEFAULT_MU = 1000.0;
   private double myMu = DEFAULT_MU;
   PropertyMode myMuMode = PropertyMode.Inherited;
   ScalarFieldComponent myMuField = null;

   protected static double DEFAULT_LAMBA_MAX = 2.0;
   private double myLambdaMax = DEFAULT_LAMBA_MAX;
   PropertyMode myLambdaMaxMode = PropertyMode.Inherited;
   ScalarFieldComponent myLambdaMaxField = null;

   // C coefficients
   private static double[] myC = new double[] {
      1/2.0, 1/20.0, 11/1050.0, 19/7000.0, 519/673750};

   static {
      myProps.addInheritableWithField (
         "mu:Inherited", "Initial modulus", DEFAULT_MU);
      myProps.addInheritableWithField (
         "lambdaMax:Inherited",
         "Number of links in the chain", DEFAULT_LAMBA_MAX);
   }

   public FieldPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ArrudaBoyceMaterial (){
   }

   public ArrudaBoyceMaterial (double mu, double lmax, double kappa) {
      setMu (mu);
      setLambdaMax (lmax);
      setBulkModulus (kappa);
   }

   // --- mu ----

   public synchronized void setMu (double mu) {
      myMu = mu;
      myMuMode =
         PropertyUtils.propagateValue (this, "mu", myMu, myMuMode);
      notifyHostOfPropertyChange();
   }

   public double getMu() {
      return myMu;
   }

   public void setMuMode (PropertyMode mode) {
      myMuMode =
         PropertyUtils.setModeAndUpdate (this, "mu", myMuMode, mode);
   }

   public PropertyMode getMuMode() {
      return myMuMode;
   }

   public double getMu (FemFieldPoint dp) {
      if (myMuField == null) {
         return getMu();
      }
      else {
         return myMuField.getValue (dp);
      }
   }

   public ScalarFieldComponent getMuField() {
      return myMuField;
   }
      
   public void setMuField (ScalarFieldComponent func) {
      myMuField = func;
      notifyHostOfPropertyChange();
   }

   // --- lambdaMax ----

   public synchronized void setLambdaMax (double lmax) {
      myLambdaMax = lmax;
      myLambdaMaxMode =
         PropertyUtils.propagateValue (
            this, "lambdaMax", myLambdaMax, myLambdaMaxMode);
      notifyHostOfPropertyChange();
   }

   public double getLambdaMax() {
      return myLambdaMax;
   }

   public void setLambdaMaxMode (PropertyMode mode) {
      myLambdaMaxMode =
         PropertyUtils.setModeAndUpdate (
            this, "lambdaMax", myLambdaMaxMode, mode);
   }

   public PropertyMode getLambdaMaxMode() {
      return myLambdaMaxMode;
   }

   public double getLambdaMax (FemFieldPoint dp) {
      if (myLambdaMaxField == null) {
         return getLambdaMax();
      }
      else {
         return myLambdaMaxField.getValue (dp);
      }
   }

   public ScalarFieldComponent getLambdaMaxField() {
      return myLambdaMaxField;
   }
      
   public void setLambdaMaxField (ScalarFieldComponent func) {
      myLambdaMaxField = func;
      notifyHostOfPropertyChange();
   }

   public void computeDevStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {

      // get coefficients
      double mu = getMu(def);
      double lmax = getLambdaMax(def);
      
      double J = def.getDetF();

      SymmetricMatrix3d B = new SymmetricMatrix3d();
      computeDevLeftCauchyGreen (B,def);

      double I1 = B.trace();
      double f = I1/lmax;
      double sum = 5*myC[4];
      for (int i=3; i>=0; i--) {
         sum = sum*f + (i+1)*myC[i];
      }
      double W1 = mu*sum;
      sigma.scale (W1, B);
      sigma.deviator();
      sigma.scale (2/J);

      if (D != null) {
         double Jinv = 1/J;

         sum = f*(f*(f*10*myC[4] + 6*myC[3]) + 3*myC[2]) + myC[1];
         double W11 = 2*mu*sum/lmax;

         double WC = W1*I1;
         double CWWC = W11*I1*I1;

         SymmetricMatrix3d WCCxC = new SymmetricMatrix3d();
         WCCxC.scale (I1*W11, B);
         
         D.setZero();

         // D = BxB*(W11*4.0*Ji) - dyad1s(WCCxC, I)*(4.0/3.0*Ji)
         TensorUtils.addTensorProduct (D, W11*4.0*Jinv, B);
         TensorUtils.addSymmetricIdentityProduct (D, -4/3.0*Jinv, WCCxC);
         // D += IxI*(4.0/9.0*Ji*CWWC);
         TensorUtils.addScaledIdentityProduct (D, 4.0/9.0*Jinv*CWWC);
         // D += (I4 - IxI/3.0)*(4.0/3.0*Ji*WC)
         TensorUtils.addScaledIdentity (D, 4.0/3*Jinv*WC);
         TensorUtils.addScaledIdentityProduct (D, -4.0/9*Jinv*WC);
         //  D +=  dyad1s(devs, I)*(-2.0/3.0)
         TensorUtils.addSymmetricIdentityProduct (D, -2/3., sigma);
         D.setLowerToUpper();         
      }
   }

   public double computeDevStrainEnergy (
      DeformedPoint def, Matrix3d Q, double excitation, 
      MaterialStateObject state) {

      // get coefficients
      double mu = getMu(def);
      double lmax = getLambdaMax(def);

      // compute the invariants
      SymmetricMatrix3d Cdev = new SymmetricMatrix3d();
      computeDevRightCauchyGreen (Cdev, def);
      double I1 = Cdev.trace();

      double invLmax = 1/lmax;
      double lmaxFrac = 1;
      double powI1 = I1;
      double pow3 = 3;
      double sum = 0;
      for (int i=0; i<myC.length; i++) {
         sum += myC[i]*lmaxFrac*(powI1-pow3);
         powI1 *= I1;
         pow3 *= 3;
         lmaxFrac *= invLmax;
      }
      return mu*sum;
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof ArrudaBoyceMaterial)) {
         return false;
      }
      ArrudaBoyceMaterial abm = (ArrudaBoyceMaterial)mat;
      if (myMu != abm.myMu || myLambdaMax != abm.myLambdaMax) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public ArrudaBoyceMaterial clone() {
      ArrudaBoyceMaterial mat = (ArrudaBoyceMaterial)super.clone();
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         myMu /= s;         
      }
      // N is unitless
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         myMu *= s;
      }
      // N is unitless
   }
}
