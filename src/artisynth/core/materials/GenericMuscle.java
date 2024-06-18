package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

/**
 * A generic muscle material
 */
public class GenericMuscle extends MuscleMaterial {

   protected static double DEFAULT_MAX_STRESS = 3e4; // \sigma_{max} arbitrary 
   
   /*
    * default muscle parameters from Blemker et al., 2005, J Biomech, 38:657-665  
    */
   protected static double DEFAULT_MAX_LAMBDA = 1.4; // \lambda^* 
   protected static double DEFAULT_EXP_STRESS_COEFF = 0.05; // P_1 
   protected static double DEFAULT_UNCRIMPING_FACTOR = 6.6; // P_2 

   // choose defautl fibre modulus to give derivative continuity at lamd =
   // lamdMax
   protected static double DEFAULT_FIBRE_MODULUS =
       DEFAULT_EXP_STRESS_COEFF*DEFAULT_UNCRIMPING_FACTOR*Math.exp (
          DEFAULT_UNCRIMPING_FACTOR*(DEFAULT_MAX_LAMBDA-1));      

   protected double myFibreModulus = DEFAULT_FIBRE_MODULUS;
   protected boolean myFibreModulusValidP = false;
   protected double myMaxLambda = DEFAULT_MAX_LAMBDA;
   protected double myMaxStress = DEFAULT_MAX_STRESS;
   protected double myExpStressCoeff = DEFAULT_EXP_STRESS_COEFF;
   protected double myUncrimpingFactor = DEFAULT_UNCRIMPING_FACTOR;

   //protected PropertyMode myFibreModulusMode = PropertyMode.Inherited;
   protected PropertyMode myMaxLambdaMode = PropertyMode.Inherited;
   protected PropertyMode myMaxStressMode = PropertyMode.Inherited;
   protected PropertyMode myExpStressCoeffMode = PropertyMode.Inherited;
   protected PropertyMode myUncrimpingFactorMode = PropertyMode.Inherited;

   protected ScalarFieldComponent myMaxLambdaField = null;
   protected ScalarFieldComponent myMaxStressField = null;
   protected ScalarFieldComponent myExpStressCoeffField = null;
   protected ScalarFieldComponent myUncrimpingFactorField = null;

   // dependent coefficients
   protected double myP3;
   protected double myP4;
   protected double myWpasOff; 
   protected boolean myDepCoefsValid = false;
   // if true, dependent coefficents depend one or more field values and so
   // must always be evaluated.
   protected boolean myDepCoefsTransient = false;

   protected Vector3d myTmp = new Vector3d();
   protected Matrix3d myMat = new Matrix3d();

   // Set this true to keep the tangent matrix continuous (and symmetric) at
   // lam = lamOpt, at the expense of slightly negative forces for lam < lamOpt
   protected static boolean myZeroForceBelowLamOptP = false;

   public GenericMuscle() {
      super();
   }

   public GenericMuscle (
      double maxLambda, double maxStress, double expStressCoeff,
      double uncrimpingFactor) {
      this();
      setMaxLambda (maxLambda);
      setMaxStress (maxStress);
      setExpStressCoeff (expStressCoeff);
      setUncrimpingFactor (uncrimpingFactor);
   }

   public static FieldPropertyList myProps =
      new FieldPropertyList (GenericMuscle.class, MuscleMaterial.class);   

   static {
      myProps.addInheritableWithField (
         "maxLambda", "maximum stress for straightened fibres",
         DEFAULT_MAX_LAMBDA, "%.8g");
      myProps.addInheritableWithField (
         "maxStress", "maximum isometric stress", DEFAULT_MAX_STRESS);
      myProps.addInheritableWithField (
         "expStressCoeff", "exponential stress coefficient",
         DEFAULT_EXP_STRESS_COEFF);
      myProps.addInheritableWithField (
         "uncrimpingFactor", "fibre uncrimping factor",
         DEFAULT_UNCRIMPING_FACTOR);
      myProps.addReadOnly (
         "fibreModulus", "modulus of straightened fibres");
   }

   public FieldPropertyList getAllPropertyInfo() {
      return myProps;
   }

   // BEGIN parameter accessors

   // fibreModulus

   protected double computeFibreModulus (
      double expStressCoeff, double uncrimpingFactor, double maxLambda) {
      return (expStressCoeff*uncrimpingFactor*
              Math.exp (uncrimpingFactor*(maxLambda-1)));
   }
   
   public double getFibreModulus () {
      if (!myFibreModulusValidP) {
         myFibreModulus = computeFibreModulus (
            myExpStressCoeff, myUncrimpingFactor, myMaxLambda);
         myFibreModulusValidP = true; 
      }
      return myFibreModulus;
   }

   // maxLambda
   
   public synchronized void setMaxLambda (double maxLambda) {
      myMaxLambda = maxLambda;
      myMaxLambdaMode =
         PropertyUtils.propagateValue (
            this, "maxLambda", myMaxLambda, myMaxLambdaMode);
      notifyHostOfPropertyChange();
      myFibreModulusValidP = false;
   }

   public double getMaxLambda() {
      return myMaxLambda;
   }

   public void setMaxLambdaMode (PropertyMode mode) {
      myMaxLambdaMode =
         PropertyUtils.setModeAndUpdate (
            this, "maxLambda", myMaxLambdaMode, mode);
   }

   public PropertyMode getMaxLambdaMode() {
      return myMaxLambdaMode;
   }

   public double getMaxLambda (FemFieldPoint dp) {
      if (myMaxLambdaField == null) {
         return getMaxLambda();
      }
      else {
         return myMaxLambdaField.getValue (dp);
      }
   }

   public ScalarFieldComponent getMaxLambdaField() {
      return myMaxLambdaField;
   }
      
   public void setMaxLambdaField (ScalarFieldComponent func) {
      myMaxLambdaField = func;
      myDepCoefsTransient = updateDepCoefsTransient();
      notifyHostOfPropertyChange();
   }

   // maxStress

   public synchronized void setMaxStress (double maxStress) {
      myMaxStress = maxStress;
      myMaxStressMode =
         PropertyUtils.propagateValue (
            this, "maxStress", myMaxStress, myMaxStressMode);
      notifyHostOfPropertyChange();
   }

   public double getMaxStress() {
      return myMaxStress;
   }

   public void setMaxStressMode (PropertyMode mode) {
      myMaxStressMode =
         PropertyUtils.setModeAndUpdate (
            this, "maxStress", myMaxStressMode, mode);
   }

   public PropertyMode getMaxStressMode() {
      return myMaxStressMode;
   }

   public double getMaxStress (FemFieldPoint dp) {
      if (myMaxStressField == null) {
         return getMaxStress();
      }
      else {
         return myMaxStressField.getValue (dp);
      }
   }

   public ScalarFieldComponent getMaxStressField() {
      return myMaxStressField;
   }
      
   public void setMaxStressField (ScalarFieldComponent func) {
      myMaxStressField = func;
      notifyHostOfPropertyChange();
   }

   // expStressCoeff

   public synchronized void setExpStressCoeff (double coeff) {
      myExpStressCoeff = coeff;
      myExpStressCoeffMode =
         PropertyUtils.propagateValue (
            this, "expStressCoeff", myExpStressCoeff, myExpStressCoeffMode);
      notifyHostOfPropertyChange();
      myFibreModulusValidP = false;
   }

   public double getExpStressCoeff() {
      return myExpStressCoeff;
   }

   public void setExpStressCoeffMode (PropertyMode mode) {
      myExpStressCoeffMode =
         PropertyUtils.setModeAndUpdate (
            this, "expStressCoeff", myExpStressCoeffMode, mode);
   }

   public PropertyMode getExpStressCoeffMode() {
      return myExpStressCoeffMode;
   }

   public double getExpStressCoeff (FemFieldPoint dp) {
      if (myExpStressCoeffField == null) {
         return getExpStressCoeff();
      }
      else {
         return myExpStressCoeffField.getValue (dp);
      }
   }

   public ScalarFieldComponent getExpStressCoeffField() {
      return myExpStressCoeffField;
   }
      
   public void setExpStressCoeffField (ScalarFieldComponent func) {
      myExpStressCoeffField = func;
      myDepCoefsTransient = updateDepCoefsTransient();
      notifyHostOfPropertyChange();
   }

   // uncrimpingFactor

   public synchronized void setUncrimpingFactor (double factor) {
      myUncrimpingFactor = factor;
      myUncrimpingFactorMode =
         PropertyUtils.propagateValue (
            this, "uncrimpingFactor", myUncrimpingFactor, myUncrimpingFactorMode);
      notifyHostOfPropertyChange();
      myFibreModulusValidP = false;
   }

   public double getUncrimpingFactor() {
      return myUncrimpingFactor;
   }

   public void setUncrimpingFactorMode (PropertyMode mode) {
      myUncrimpingFactorMode =
         PropertyUtils.setModeAndUpdate (
            this, "uncrimpingFactor", myUncrimpingFactorMode, mode);
   }

   public PropertyMode getUncrimpingFactorMode() {
      return myUncrimpingFactorMode;
   }

   public double getUncrimpingFactor (FemFieldPoint dp) {
      if (myUncrimpingFactorField == null) {
         return getUncrimpingFactor();
      }
      else {
         return myUncrimpingFactorField.getValue (dp);
      }
   }

   public ScalarFieldComponent getUncrimpingFactorField() {
      return myUncrimpingFactorField;
   }
      
   public void setUncrimpingFactorField (ScalarFieldComponent func) {
      myUncrimpingFactorField = func;
      myDepCoefsTransient = updateDepCoefsTransient();
      notifyHostOfPropertyChange();
   }

   // END parameter accessors

   /** 
    * Stress is computed from
    * <pre>
    * 2*W4*I4/J*(a (x) a - 1/3 I )
    * </pre>
    */
   private void addStress (
      Matrix3dBase sig, double J, double I4, double W4, Vector3d a) {
      
      // Stress is computed as
      //
      // 2*W4*I4/J*( a (x) a - 1/3 I )
      //

      double T00 = a.x*a.x;
      double T01 = a.x*a.y;
      double T02 = a.x*a.z;

      double T11 = a.y*a.y;
      double T12 = a.y*a.z;
      double T22 = a.z*a.z;

      double c = 2.0*W4*I4/J;

      // add to stress:

      sig.m00 += c*(T00 - 1/3.0);
      sig.m11 += c*(T11 - 1/3.0);
      sig.m22 += c*(T22 - 1/3.0);
      sig.m01 += c*T01;
      sig.m12 += c*T12;
      sig.m02 += c*T02;

      sig.m10 = sig.m01;
      sig.m20 = sig.m02;
      sig.m21 = sig.m12;
   }

   /** 
    * Stress is computed from
    * <pre>
    * 2*W4*I4/J*(a (x) a - 1/3 I )
    * </pre>
    */
   private void setStress (
      Matrix3dBase sig, double J, double I4, double W4, Vector3d a) {
      
      // Stress is computed as
      //
      // 2*W4*I4/J*( a (x) a - 1/3 I )
      //

      double T00 = a.x*a.x;
      double T01 = a.x*a.y;
      double T02 = a.x*a.z;

      double T11 = a.y*a.y;
      double T12 = a.y*a.z;
      double T22 = a.z*a.z;

      double c = 2.0*W4*I4/J;

      // add to stress:

      sig.m00 = c*(T00 - 1/3.0);
      sig.m11 = c*(T11 - 1/3.0);
      sig.m22 = c*(T22 - 1/3.0);
      sig.m01 = c*T01;
      sig.m12 = c*T12;
      sig.m02 = c*T02;

      sig.m10 = sig.m01;
      sig.m20 = sig.m02;
      sig.m21 = sig.m12;
   }

   private boolean updateDepCoefsTransient() {
      myDepCoefsValid = false;
      return (myMaxLambdaField != null ||
              myExpStressCoeffField != null ||
              myUncrimpingFactorField != null);
   }

   private void updateDepCoefs(DeformedPoint def) {
      double lamMax = getMaxLambda(def);
      double P1 = getExpStressCoeff(def);
      double P2 = getUncrimpingFactor(def);

      myP3 = computeFibreModulus (P1, P2, lamMax);
      double maxExpTerm = Math.exp(P2*(lamMax-1));
      myP4 = P1*(maxExpTerm-1) - myP3*lamMax;
      myWpasOff = 
         computeExpWp (lamMax, def) -
         (myP3*lamMax + myP4*Math.log(lamMax));
      if (!myDepCoefsTransient) {
         myDepCoefsValid = true;
      }
   }

   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Vector3d dir0, double excitation, MaterialStateObject state) {

      // Methods and naming conventions follow the paper "Finite element
      // implementation of incompressible, isotropic hyperelasticity", by
      // Weiss, Makerc, and Govindjeed, Computer Methods in Applied Mechanical
      // Engineering, 1996.

      double maxLambda = getMaxLambda(def);
      double maxStress = getMaxStress(def);
      
      Vector3d a = myTmp;
      def.getF().mul (a, dir0);
      double mag = a.norm();
      if (mag == 0.0) {
         sigma.setZero();
         if (D != null) {
            D.setZero();
         }
         return;
      }
      a.scale (1/mag);
      double J = def.getDetF();
      double lamd = mag*Math.pow(J, -1.0/3.0);
      double I4 = lamd*lamd;

      double W4 = 0;
      double Fp = 0;

      double P1 = getExpStressCoeff(def);
      double P2 = getUncrimpingFactor(def);
      double expTerm = 0;

      if (myZeroForceBelowLamOptP && lamd < 1) {
         Fp = 0;
      }
      else if (lamd < maxLambda) {
         expTerm = Math.exp(P2*(lamd-1)); 
         Fp = P1*(expTerm-1)/lamd;
      } 
      else {
         if (!myDepCoefsValid) {
            updateDepCoefs(def);
         }
         Fp = (myP3*lamd + myP4)/lamd;
      }

      W4 = 0.5*(Fp+excitation*maxStress)/lamd;
      
      setStress (sigma, J, I4, W4, a);

      if (D != null) {
         double FpDl = 0;

         if (myZeroForceBelowLamOptP && lamd < 1) {
            FpDl = 0;
         }
         else if (lamd < maxLambda) {
            FpDl = P1/lamd*(P2*expTerm - (expTerm-1)/lamd);
         }
         else {
            FpDl = -myP4/(lamd*lamd);
         }

         double W44 = 0.5*(0.5*FpDl - W4)/(lamd*lamd);

         double w0 = W4*I4;
         double wa = W44*I4*I4;

         D.setZero();
         //
         // compute -2/3 (dev sigma (X) I)' - 4 wa/(3J) (a (X) a (X) I)'
         //
         myMat.outerProduct (a, a);
         myMat.scale (2*wa/J); // will be scaled again by -2/3 below
         addStress (myMat, J, I4, W4, a);
         myMat.scale (-2/3.0);
         TensorUtils.addSymmetricIdentityProduct (D, myMat);
         TensorUtils.addScaledIdentity (D, 4/3.0*w0/J);
         TensorUtils.addScaledIdentityProduct (D, 4/9.0*(wa-w0)/J);
         TensorUtils.addScaled4thPowerProduct (D, 4*wa/J, a);         
      }
   }

   @Override
   public double computeStrainEnergyDensity (
      DeformedPoint def, Vector3d dir0, double excitation, 
      MaterialStateObject state) {

      // Methods and naming conventions follow the paper "Finite element
      // implementation of incompressible, isotropic hyperelasticity", by
      // Weiss, Makerc, and Govindjeed, Computer Methods in Applied Mechanical
      // Engineering, 1996.

      double maxLambda = getMaxLambda(def);
      
      Vector3d a = myTmp;
      def.getF().mul (a, dir0);
      double mag = a.norm();
      if (mag == 0.0) {
         return 0;
      }
      a.scale (1/mag);
      double J = def.getDetF();
      double lamd = mag*Math.pow(J, -1.0/3.0);

      // strain energy density currently computed only for the passive force.
      double Wpas;
      if (myZeroForceBelowLamOptP && lamd < 1) {
         Wpas = 0;
      }
      else if (lamd < maxLambda) {
         Wpas = computeExpWp (lamd, def);
      } 
      else {
         if (!myDepCoefsValid) {
            updateDepCoefs(def);
         }
         Wpas = myP3*lamd + myP4*Math.log(lamd) + myWpasOff;
      }
      return Wpas;
   }
   
   @Override
   public boolean hasSymmetricTangent() {
      return true;
   }

   public boolean equals (MuscleMaterial mat) {
      if (!(mat instanceof GenericMuscle)) {
         return false;
      }
      GenericMuscle mrm = (GenericMuscle)mat;
      if (myMaxLambda != mrm.myMaxLambda ||
          myMaxStress != mrm.myMaxStress ||
          myExpStressCoeff != mrm.myExpStressCoeff ||
          myUncrimpingFactor != mrm.myUncrimpingFactor) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public GenericMuscle clone() {
      GenericMuscle mat = (GenericMuscle)super.clone();
      mat.myTmp = new Vector3d();
      mat.myMat = new Matrix3d();
      return mat;
   }

   @Override
   public void scaleDistance(double s) {
      if (s != 1) {
         super.scaleDistance(s);
         setMaxStress (myMaxStress/s);
         setExpStressCoeff (myExpStressCoeff/s);
         myFibreModulusValidP = false;
      }            
   }

   @Override
   public void scaleMass(double s) {
      if (s != 1) {
         super.scaleMass(s);
         setMaxStress (myMaxStress*s);
         setExpStressCoeff (myExpStressCoeff*s);
         myFibreModulusValidP = false;
      }
   }

   /**
    * Computes the strain energy density within the exponential force segment
    * (lam in [0, maxLambda]).  This integral requires the exponential integral
    * function, which we don't have, so we compute it numerically. That is easy
    * to do because the function is very well behaved over the interval, and so
    * we just apply Simpson's rule over a few segments.
    */
   private double computeExpWp (double lamd, DeformedPoint def) {

      double maxLambda = getMaxLambda(def);
      double maxStress = getMaxStress(def);
      double P1 = getExpStressCoeff(def);
      double P2 = getUncrimpingFactor(def);

      if (lamd == 1) {
         return 0;
      }

      int nsegs = 6; // 6 segments gives us 3 Simpson segments
      double dx = (lamd-1)/nsegs;
      double x = 1;
      double f0 = 0; // function value at l = 1
      double Wp = 0;
      for (int i=0; i<nsegs; i+=2) {
         x += dx;
         double f1 = (Math.exp(P2*(x-1))-1)/x;
         x += dx;
         double f2 = (Math.exp(P2*(x-1))-1)/x;
         Wp += dx/3*(f0+4*f1+f2);
         f0 = f2;
      }
      Wp *= P1;
      return Wp;
   }

   private double computeFp (double lamd) {

      double maxLambda = getMaxLambda();
      double maxStress = getMaxStress();

      double P1 = getExpStressCoeff();
      double P2 = getUncrimpingFactor();
      double c5 = getFibreModulus();

      double Fp = 0;

      if (myZeroForceBelowLamOptP && lamd < 1) {
         Fp = 0;
      }
      else {
         if (lamd <= maxLambda) {
            double expTerm = Math.exp(P2*(lamd-1)); 
            Fp = P1*(expTerm-1)/lamd;
         } 
         if (lamd >= maxLambda) {
            double maxExpTerm = Math.exp(P2*(maxLambda-1));
            double c6 = P1*(maxExpTerm-1) - c5*maxLambda;
            Fp = (c5*lamd + c6)/lamd;
         }
      }
      return Fp;
   }
   
   public static void main(String[] args) {
      GenericMuscle mus = new GenericMuscle();

      for (int i=0; i<=10; i++) {
         double lam = 1.0 + i*0.05;
         System.out.printf (
            "lam=%g Fp=%g Wp=%g\n",
            lam, mus.computeFp(lam), mus.computeExpWp(lam,null));
      }
   }
   
}
