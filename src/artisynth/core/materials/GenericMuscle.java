package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
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

   public static PropertyList myProps =
      new PropertyList (GenericMuscle.class, MuscleMaterial.class);   

   static {
      myProps.addInheritable (
         "maxLambda", "maximum stress for straightened fibres",
         DEFAULT_MAX_LAMBDA, "%.8g");
      myProps.addInheritable (
         "maxStress", "maximum isometric stress", DEFAULT_MAX_STRESS);
      myProps.addInheritable ("expStressCoeff", "exponential stress coefficient",
                   DEFAULT_EXP_STRESS_COEFF);
      myProps.addInheritable ("uncrimpingFactor", "fibre uncrimping factor",
                   DEFAULT_UNCRIMPING_FACTOR);
      myProps.addReadOnly (
         "fibreModulus", "modulus of straightened fibres");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   // public synchronized void setFibreModulus (double optLam) {
   //    myFibreModulus = optLam;
   //    myFibreModulusMode =
   //       PropertyUtils.propagateValue (
   //          this, "fibreModulus", myFibreModulus, myFibreModulusMode);
   //    notifyHostOfPropertyChange();
   // }

   public double getFibreModulus () {
      if (!myFibreModulusValidP) {
         myFibreModulus =
            myExpStressCoeff*myUncrimpingFactor*Math.exp (
               myUncrimpingFactor*(myMaxLambda-1));      
         myFibreModulusValidP = true; 
      }
      return myFibreModulus;
   }

   // public double getFibreModulus() {
   //    return myFibreModulus;
   // }

   // public void setFibreModulusMode (PropertyMode mode) {
   //    myFibreModulusMode =
   //       PropertyUtils.setModeAndUpdate (
   //          this, "fibreModulus", myFibreModulusMode, mode);
   // }

   // public PropertyMode getFibreModulusMode() {
   //    return myFibreModulusMode;
   // }

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

   /** 
    * Stress is computed from
    * <pre>
    * 2*W4*I4/J*(a (x) a - 1/3 I )
    * </pre>
    * 
    * @param
    * @return
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
    * 
    * @param
    * @return
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

   public void computeStress (
      SymmetricMatrix3d sigma, double excitation, Vector3d dir0,
      SolidDeformation def, FemMaterial baseMat) {
      
      // Methods and naming conventions follow the paper "Finite element
      // implementation of incompressible, isotropic hyperelasticity", by
      // Weiss, Makerc, and Govindjeed, Computer Methods in Applied Mechanical
      // Engineering, 1996.

      Vector3d dir = myTmp;
      def.getF().mul (dir, dir0);
      double mag = dir.norm();
      dir.scale (1/mag);
      double J = def.getDetF();
      double lamd = mag*Math.pow(J, -1.0/3.0);
      double I4 = lamd*lamd;

      double W4 = 0;
      double Fp = 0;

      double P1 = myExpStressCoeff;
      double P2 = myUncrimpingFactor;
      double c5 = getFibreModulus();

      if (myZeroForceBelowLamOptP && lamd < 1) {
          Fp = 0;
      }
      else if (lamd < myMaxLambda) {
         double expTerm = Math.exp(P2*(lamd-1)); 
         Fp = P1*(expTerm-1)/lamd;
      } 
      else {
         double maxExpTerm = Math.exp(P2*(myMaxLambda-1)); 
         double c6 = P1*(maxExpTerm-1) - c5*myMaxLambda;
         Fp = (c5*lamd + c6)/lamd;
      }

      W4 = 0.5*(Fp+excitation*myMaxStress)/lamd;
      
      setStress (sigma, J, I4, W4, dir);
   }

   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, double excitation, Vector3d dir0, 
      SolidDeformation def, FemMaterial baseMat) {

      Vector3d a = myTmp;
      def.getF().mul (a, dir0);
      double lam = a.norm();
      a.scale (1/lam);
      double J = def.getDetF();
      double lamd = lam*Math.pow(J, -1.0/3.0);
      double I4 = lamd*lamd;

      double W4 = 0;
      double W44 = 0;
      double Fp = 0;
      double FpDl = 0;

      double P1 = myExpStressCoeff;
      double P2 = myUncrimpingFactor;
      double c5 = getFibreModulus();
      
      if (myZeroForceBelowLamOptP && lamd < 1) {
         Fp = 0;
         FpDl = 0;
      }
      else if (lamd < myMaxLambda) {
         double expTerm = Math.exp(P2*(lamd - 1));
         Fp  = P1*(expTerm-1)/lamd;
         FpDl = P1/lamd*(P2*expTerm - (expTerm-1)/lamd);
      }
      else {
         double maxExpTerm = Math.exp(P2*(myMaxLambda-1)); 
         double c6 = P1*(maxExpTerm-1) - c5*myMaxLambda;
         Fp = (c5*lamd + c6)/lamd;
         FpDl = -c6/(lamd*lamd);
      }

      W4  = 0.5*(Fp+myMaxStress*excitation)/lamd;
      W44 = 0.5*(0.5*FpDl - W4)/(lamd*lamd);

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
   
}
