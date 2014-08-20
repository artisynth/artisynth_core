package artisynth.core.materials;

import maspack.matrix.*;
import maspack.properties.*;

/**
 * A generic muscle material
 */
public class BlemkerAxialMuscle extends AxialMuscleMaterial {

   /*
    * default muscle parameters from Blemker et al., 2005, J Biomech, 38:657-665  
    */
   protected static double DEFAULT_MAX_LENGTH = 1.4; // \lambda^* 
   protected static double DEFAULT_OPT_LENGTH = 1.0; // \lambda_{ofl} 
   protected static double DEFAULT_MAX_FORCE = 3e5; // \sigma_{max} 
   protected static double DEFAULT_EXP_STRESS_COEFF = 0.05; // P_1 
   protected static double DEFAULT_UNCRIMPING_FACTOR = 6.6; // P_2 
   
   protected double myMaxLength = DEFAULT_MAX_LENGTH;
   protected double myOptLength = DEFAULT_OPT_LENGTH;
   protected double myMaxForce = DEFAULT_MAX_FORCE;
   protected double myExpStressCoeff = DEFAULT_EXP_STRESS_COEFF;
   protected double myUncrimpingFactor = DEFAULT_UNCRIMPING_FACTOR;

   protected double myP3;
   protected double myP4;
   protected boolean myP3P4Valid = false;

   protected PropertyMode myMaxLengthMode = PropertyMode.Inherited;
   protected PropertyMode myOptLengthMode = PropertyMode.Inherited;
   protected PropertyMode myMaxForceMode = PropertyMode.Inherited;
   protected PropertyMode myExpStressCoeffMode = PropertyMode.Inherited;
   protected PropertyMode myUncrimpingFactorMode = PropertyMode.Inherited;

   protected Vector3d myTmp = new Vector3d();
   protected Matrix3d myMat = new Matrix3d();

   // Set this true to keep dF/dl continuous at
   // lam = lamOpt, at the expense of slightly negative forces for lam < lamOpt
   protected static boolean myZeroForceBelowLenOptP = true;

   public BlemkerAxialMuscle() {
      super();
   }

   public BlemkerAxialMuscle (
      double maxLen, double optLen, double maxForce, double expStress,
      double uncrimp) {
      this();
      setMaxLength (maxLen);
      setOptLength (optLen);
      setMaxForce (maxForce);
      setExpStressCoeff (expStress);
      setUncrimpingFactor (uncrimp);
   }

   public static PropertyList myProps =
      new PropertyList (BlemkerAxialMuscle.class, AxialMuscleMaterial.class);   

   static {
      myProps.addInheritable ("expStressCoeff", "exponential stress coefficient",
                   DEFAULT_EXP_STRESS_COEFF);
      myProps.addInheritable ("uncrimpingFactor", "fibre uncrimping factor",
                   DEFAULT_UNCRIMPING_FACTOR);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public synchronized void setMaxLength (double maxLength) {
      myP3P4Valid = false;
      myMaxLength = maxLength;
      myMaxLengthMode =
         PropertyUtils.propagateValue (
            this, "maxLength", myMaxLength, myMaxLengthMode);
      notifyHostOfPropertyChange();
   }

   public double getMaxLength() {
      return myMaxLength;
   }

   public void setMaxLengthMode (PropertyMode mode) {
      myMaxLengthMode =
         PropertyUtils.setModeAndUpdate (
            this, "maxLength", myMaxLengthMode, mode);
   }

   public PropertyMode getMaxLengthMode() {
      return myMaxLengthMode;
   }

   public synchronized void setOptLength (double optLength) {
      myP3P4Valid = false;
      myOptLength = optLength;
      myOptLengthMode =
         PropertyUtils.propagateValue (
            this, "optLength", myOptLength, myOptLengthMode);
      notifyHostOfPropertyChange();
   }

   public double getOptLength() {
      return myOptLength;
   }

   public void setOptLengthMode (PropertyMode mode) {
      myOptLengthMode =
         PropertyUtils.setModeAndUpdate (
            this, "optLength", myOptLengthMode, mode);
   }

   public PropertyMode getOptLengthMode() {
      return myOptLengthMode;
   }

   public synchronized void setMaxForce (double maxForce) {
      myMaxForce = maxForce;
      myMaxForceMode =
         PropertyUtils.propagateValue (
            this, "maxForce", myMaxForce, myMaxForceMode);
      notifyHostOfPropertyChange();
   }

   public double getMaxForce() {
      return myMaxForce;
   }

   public void setMaxForceMode (PropertyMode mode) {
      myMaxForceMode =
         PropertyUtils.setModeAndUpdate (
            this, "maxForce", myMaxForceMode, mode);
   }

   public PropertyMode getMaxForceMode() {
      return myMaxForceMode;
   }

   public synchronized void setExpStressCoeff (double coeff) {
      myP3P4Valid = false;
      myExpStressCoeff = coeff;
      myExpStressCoeffMode =
         PropertyUtils.propagateValue (
            this, "expStressCoeff", myExpStressCoeff, myExpStressCoeffMode);
      notifyHostOfPropertyChange();
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
      myP3P4Valid = false;
      myUncrimpingFactor = factor;
      myUncrimpingFactorMode =
         PropertyUtils.propagateValue (
            this, "uncrimpingFactor", myUncrimpingFactor, myUncrimpingFactorMode);
      notifyHostOfPropertyChange();
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

   private final double square (double x) {
      return x*x;
   }

   public double computeF (
	      double l, double ldot, double l0, double excitation) {
 
      double lenOpt = myOptLength;
      double lenRat = l/lenOpt;

      double lenUpperLim = 1.6*lenOpt;
      double lenLowerLim = 0.4*lenOpt;

      double fact = 0;
      double fpas = 0;

      double P1 = myExpStressCoeff;
      double P2 = myUncrimpingFactor;

      if (!myP3P4Valid) {
         myP3 = P1*P2*Math.exp(P2*(myMaxLength/lenOpt-1));
         myP4 = P1*(Math.exp(P2*(myMaxLength/lenOpt-1))-1) - myP3*myMaxLength/lenOpt;
         myP3P4Valid = true;
      }

      if (myZeroForceBelowLenOptP && l <= lenOpt) {
         fpas = 0;
      }
      else if (l <= myMaxLength) {
         fpas = P1*(Math.exp(P2*(lenRat-1))-1);
      }
      else {
         fpas = myP3*lenRat + myP4;
      }

      if (l <= 0.6*lenOpt) {
         fact = 9*square(lenRat-0.4);
      }
      else if (l < 1.4*lenOpt) {
         fact = 1-4*square(1-lenRat);
      }
      else {
         fact = 9*square(lenRat-1.6);
      }

      // zero band, recommended by FEBio
      if (l < lenLowerLim || l > lenUpperLim) {
         fact = 0;
      }

      // calculate total fiber force
      return myMaxForce*(fpas + excitation*fact);
   }

   public double computeDFdl (
	      double l, double ldot, double l0, double excitation) {
      

      double P1 = myExpStressCoeff;
      double P2 = myUncrimpingFactor;
      double lenOpt = myOptLength;
      double lenRat = l/lenOpt;

      double lenUpperLim = 1.6*lenOpt;
      double lenLowerLim = 0.4*lenOpt;
      
      double dfactDl = 0;
      double dfpasDl = 0;

      if (!myP3P4Valid) {
         myP3 = P1*P2*Math.exp(P2*(myMaxLength/lenOpt-1));
         myP4 = P1*(Math.exp(P2*(myMaxLength/lenOpt-1))-1) - myP3*myMaxLength/lenOpt;
         myP3P4Valid = true;
      }

      if (myZeroForceBelowLenOptP && l <= lenOpt) {
         dfpasDl = 0;
      }
      else if (l <= myMaxLength) {
         dfpasDl = P1*P2/lenOpt*Math.exp(P2*(lenRat-1));
      }
      else {
         dfpasDl = myP3/lenOpt;
      }

      if (l <= 0.6*lenOpt) {
         dfactDl = 18*(lenRat-0.4)/lenOpt;
      }
      else if (l < 1.4*lenOpt) {
         dfactDl = 8*(1-lenRat)/lenOpt;
      }
      else {
         dfactDl = 18*(lenRat-1.6)/lenOpt;
      }

      // zero band, recommended by FEBio
      if (l < lenLowerLim || l > lenUpperLim) {
         dfactDl = 0;
      }

      return myMaxForce*(dfpasDl + excitation*dfactDl);
   }
   
   public double computeDFdldot (
	      double l, double ldot, double l0, double excitation) {
      return 0;
   }

   public boolean isDFdldotZero () {
      return true;
   }

   public boolean equals (AxialMaterial mat) {
      if (!(mat instanceof BlemkerAxialMuscle)) {
         return false;
      }
      BlemkerAxialMuscle mrm = (BlemkerAxialMuscle)mat;
      if (myMaxLength != mrm.myMaxLength ||
          myOptLength != mrm.myOptLength ||
          myMaxForce != mrm.myMaxForce ||
          myExpStressCoeff != mrm.myExpStressCoeff ||
          myUncrimpingFactor != mrm.myUncrimpingFactor) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public BlemkerAxialMuscle clone() {
      BlemkerAxialMuscle mat = (BlemkerAxialMuscle)super.clone();
      mat.myTmp = new Vector3d();
      mat.myMat = new Matrix3d();
      return mat;
   }

   @Override
   public void scaleDistance(double s) {
      if (s != 1) {
         super.scaleDistance(s);
         setMaxForce (myMaxForce/s);
      }
   }

   @Override
   public void scaleMass(double s) {
      if (s != 1) {
         super.scaleMass(s);
         setMaxForce (myMaxForce*s);
      }
   }
   
}
