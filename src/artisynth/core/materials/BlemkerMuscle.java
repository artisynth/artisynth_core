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
public class BlemkerMuscle extends MuscleMaterial {

   /*
    * default muscle parameters from Blemker et al., 2005, J Biomech, 38:657-665  
    */
   protected static double DEFAULT_MAX_LAMBDA = 1.4; // \lambda^* 
   protected static double DEFAULT_OPT_LAMBDA = 1.0; // \lambda_{ofl} 
   protected static double DEFAULT_MAX_STRESS = 3e5; // \sigma_{max} 
   protected static double DEFAULT_EXP_STRESS_COEFF = 0.05; // P_1 
   protected static double DEFAULT_UNCRIMPING_FACTOR = 6.6; // P_2 
   
   protected double myMaxLambda = DEFAULT_MAX_LAMBDA;
   protected double myOptLambda = DEFAULT_OPT_LAMBDA;
   protected double myMaxStress = DEFAULT_MAX_STRESS;
   protected double myExpStressCoeff = DEFAULT_EXP_STRESS_COEFF;
   protected double myUncrimpingFactor = DEFAULT_UNCRIMPING_FACTOR;

   protected double myP3;
   protected double myP4;
   protected boolean myP3P4Valid = false;

   protected PropertyMode myMaxLambdaMode = PropertyMode.Inherited;
   protected PropertyMode myOptLambdaMode = PropertyMode.Inherited;
   protected PropertyMode myMaxStressMode = PropertyMode.Inherited;
   protected PropertyMode myExpStressCoeffMode = PropertyMode.Inherited;
   protected PropertyMode myUncrimpingFactorMode = PropertyMode.Inherited;

   protected Vector3d myTmp = new Vector3d();
   protected Matrix3d myMat = new Matrix3d();

   // Set this true to keep the tangent matrix continuous (and symmetric) at
   // lam = lamOpt, at the expense of slightly negative forces for lam < lamOpt
   protected static boolean myZeroForceBelowLamOptP = true;

   public BlemkerMuscle() {
      super();
   }

   public BlemkerMuscle (
      double maxLam, double optLam, double maxStress, double expStress,
      double uncrimp) {
      this();
      setMaxLambda (maxLam);
      setOptLambda (optLam);
      setMaxStress (maxStress);
      setExpStressCoeff (expStress);
      setUncrimpingFactor (uncrimp);
   }

   public static PropertyList myProps =
      new PropertyList (BlemkerMuscle.class, MuscleMaterial.class);   

   static {
      myProps.addInheritable (
         "maxLambda", "maximum stretch for straightened fibres",
         DEFAULT_MAX_LAMBDA, "%.8g");
      myProps.addInheritable (
         "optLambda", "optimal stretch for straightened fibres",
         DEFAULT_OPT_LAMBDA, "%.8g");
      myProps.addInheritable (
         "maxStress", "maximum isometric stress", DEFAULT_MAX_STRESS);
      myProps.addInheritable ("expStressCoeff", "exponential stress coefficient",
                   DEFAULT_EXP_STRESS_COEFF);
      myProps.addInheritable ("uncrimpingFactor", "fibre uncrimping factor",
                   DEFAULT_UNCRIMPING_FACTOR);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public synchronized void setMaxLambda (double maxLambda) {
      myP3P4Valid = false;
      myMaxLambda = maxLambda;
      myMaxLambdaMode =
         PropertyUtils.propagateValue (
            this, "maxLambda", myMaxLambda, myMaxLambdaMode);
      notifyHostOfPropertyChange();
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

   public synchronized void setOptLambda (double optLambda) {
      myP3P4Valid = false;
      myOptLambda = optLambda;
      myOptLambdaMode =
         PropertyUtils.propagateValue (
            this, "optLambda", myOptLambda, myOptLambdaMode);
      notifyHostOfPropertyChange();
   }

   public double getOptLambda() {
      return myOptLambda;
   }

   public void setOptLambdaMode (PropertyMode mode) {
      myOptLambdaMode =
         PropertyUtils.setModeAndUpdate (
            this, "optLambda", myOptLambdaMode, mode);
   }

   public PropertyMode getOptLambdaMode() {
      return myOptLambdaMode;
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

   private void addStress (
      Matrix3dBase sig, double J, double I4, double W4, Vector3d a) {

      double T00 = a.x*a.x;
      double T01 = a.x*a.y;
      double T02 = a.x*a.z;

      double T11 = a.y*a.y;
      double T12 = a.y*a.z;
      double T22 = a.z*a.z;

      // trace of T/3
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

   private void setStress (
      Matrix3dBase sig, double J, double I4, double W4, Vector3d a) {

      double T00 = a.x*a.x;
      double T01 = a.x*a.y;
      double T02 = a.x*a.z;

      double T11 = a.y*a.y;
      double T12 = a.y*a.z;
      double T22 = a.z*a.z;

      // trace of T/3
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

   private final double square (double x) {
      return x*x;
   }

   public void computeStress (
      SymmetricMatrix3d sigma, double excitation, Vector3d dir0,
      SolidDeformation def, FemMaterial baseMat) {
      
      Vector3d a = myTmp;
      def.getF().mul (a, dir0);
      double mag = a.norm();
      a.scale (1/mag);
      double J = def.getDetF();
      // note that lam is the dilational component of lam 
      double lam = mag*Math.pow(J, -1.0/3.0);
      double I4 = lam*lam;

      double lamOpt = myOptLambda;
      double lamRat = lam/myOptLambda;
      double lamMax = myMaxLambda; 

      double lamUpperLim = 1.6*lamOpt;
      double lamLowerLim = 0.4*lamOpt;

      double fact = 0;
      double fpas = 0;

      double P1 = myExpStressCoeff;
      double P2 = myUncrimpingFactor;

      if (!myP3P4Valid) {
         myP3 = P1*P2*Math.exp(P2*(myMaxLambda/lamOpt-1));
         myP4 = P1*(Math.exp(P2*(myMaxLambda/lamOpt-1))-1) - myP3*myMaxLambda/lamOpt;
         myP3P4Valid = true;
      }

      if (myZeroForceBelowLamOptP && lam <= lamOpt) {
         fpas = 0;
      }
      else if (lam <= lamMax) {
         fpas = P1*(Math.exp(P2*(lamRat-1))-1);
      }
      else {
         fpas = myP3*lamRat + myP4;
      }

      if (lam <= 0.6*lamOpt) {
         fact = 9*square(lamRat-0.4);
      }
      else if (lam < 1.4*lamOpt) {
         fact = 1-4*square(1-lamRat);
      }
      else {
         fact = 9*square(lamRat-1.6);
      }

      // zero band, recommended by FEBio
      if (lam < lamLowerLim || lam > lamUpperLim) {
         fact = 0;
      }

      double dfdl = myMaxStress*(fpas + excitation*fact)/lamOpt;
      double W4 = 0.5*dfdl/lam;

      setStress (sigma, J, I4, W4, a);
   }

   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, double excitation, Vector3d dir0, 
      SolidDeformation def, FemMaterial baseMat) {

      Vector3d a = myTmp;
      def.getF().mul (a, dir0);
      double mag = a.norm();
      a.scale (1/mag);
      double J = def.getDetF();
      // note that lam is the dilational component of lam 
      double lam = mag*Math.pow(J, -1.0/3.0);
      double I4 = lam*lam;

      double lamOpt = myOptLambda;
      double lamRat = lam/myOptLambda;
      double lamMax = myMaxLambda; 

      double lamUpperLim = 1.6*lamOpt;
      double lamLowerLim = 0.4*lamOpt;

      double fact = 0;
      double fpas = 0;
      double dfactDl = 0;
      double dfpasDl = 0;

      double P1 = myExpStressCoeff;
      double P2 = myUncrimpingFactor;

      if (!myP3P4Valid) {
         myP3 = P1*P2*Math.exp(P2*(myMaxLambda/lamOpt-1));
         myP4 = P1*(Math.exp(P2*(myMaxLambda/lamOpt-1))-1) - myP3*myMaxLambda/lamOpt;
         myP3P4Valid = true;
      }

      if (myZeroForceBelowLamOptP && lam <= lamOpt) {
         fpas = 0;
         dfpasDl = 0;
      }
      else if (lam <= lamMax) {
         double expTerm = Math.exp(P2*(lamRat-1));
         fpas = P1*(expTerm-1);
         dfpasDl = P1*P2/lamOpt*expTerm;
      }
      else {
         fpas = myP3*lamRat + myP4;
         dfpasDl = myP3/lamOpt;
      }

      if (lam <= 0.6*lamOpt) {
         fact = 9*square(lamRat-0.4);
         dfactDl = 18*(lamRat-0.4)/lamOpt;
      }
      else if (lam < 1.4*lamOpt) {
         fact = 1-4*square(1-lamRat);
         dfactDl = 8*(1-lamRat)/lamOpt;
      }
      else {
         fact = 9*square(lamRat-1.6);
         dfactDl = 18*(lamRat-1.6)/lamOpt;
      }

      // zero band, recommended by FEBio
      if (lam < lamLowerLim || lam > lamUpperLim) {
         fact = 0;
         dfactDl = 0;
      }

      double FfDl = myMaxStress*(fpas + excitation*fact)/lamOpt;
      double W4 = 0.5*FfDl/lam;

      double FfDll = myMaxStress*(dfpasDl + excitation*dfactDl)/lamOpt;
      double W44 = 0.25*(FfDll - FfDl/lam)/I4;

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

   public double computeStretch (Vector3d dir0, SolidDeformation def) {
      Vector3d dir = myTmp;
      def.getF().mul(dir, dir0);
      double mag = dir.norm();
      double J = def.getDetF();
      return mag * Math.pow(J, -1.0 / 3.0);
   }

   public boolean equals (MuscleMaterial mat) {
      if (!(mat instanceof BlemkerMuscle)) {
         return false;
      }
      BlemkerMuscle mrm = (BlemkerMuscle)mat;
      if (myMaxLambda != mrm.myMaxLambda ||
          myOptLambda != mrm.myOptLambda ||
          myMaxStress != mrm.myMaxStress ||
          myExpStressCoeff != mrm.myExpStressCoeff ||
          myUncrimpingFactor != mrm.myUncrimpingFactor) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public BlemkerMuscle clone() {
      BlemkerMuscle mat = (BlemkerMuscle)super.clone();
      mat.myTmp = new Vector3d();
      mat.myMat = new Matrix3d();
      return mat;
   }

   public static void main (String[] args) {
      BlemkerMuscle mat = new BlemkerMuscle();

      SolidDeformation def = new SolidDeformation();
      def.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();
      FemMaterial baseMat = new MooneyRivlinMaterial();

      Vector3d a = new Vector3d (1, 0, 0);
      //a.setRandom();
      mat.computeStress (sig, 1.0, a, def, baseMat);
      //def.setStress (sig);
      mat.computeTangent (D, sig, 1.0, a, def, baseMat);

      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      System.out.println ("D=\n" + D.toString ("%12.6f"));
   }

   @Override
   public void scaleDistance(double s) {
      if (s != 1) {
         super.scaleDistance(s);
         setMaxStress (myMaxStress/s);
      }
   }

   @Override
   public void scaleMass(double s) {
      if (s != 1) {
         super.scaleMass(s);
         setMaxStress (myMaxStress*s);
      }
   }
   
}
