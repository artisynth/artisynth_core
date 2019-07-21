package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class FullBlemkerMuscle extends MuscleMaterial {

   /*
    * default muscle parameters from Blemker et al., 2005, J Biomech, 38:657-665  
    */
   protected static double DEFAULT_MAX_LAMBDA = 1.4; // \lambda^* 
   protected static double DEFAULT_OPT_LAMBDA = 1; // \lambda_{ofl} 
   protected static double DEFAULT_MAX_STRESS = 3e5; // \sigma_{max} 
   protected static double DEFAULT_EXP_STRESS_COEFF = 0.05; // P_1 
   protected static double DEFAULT_UNCRIMPING_FACTOR = 6.6; // P_2 
   protected static double DEFAULT_G1 = 0; //5e2;
   protected static double DEFAULT_G2 = 0; //5e2;
   
   protected double myMaxLambda = DEFAULT_MAX_LAMBDA;
   protected double myOptLambda = DEFAULT_OPT_LAMBDA;
   protected double myMaxStress = DEFAULT_MAX_STRESS;
   protected double myExpStressCoeff = DEFAULT_EXP_STRESS_COEFF;
   protected double myUncrimpingFactor = DEFAULT_UNCRIMPING_FACTOR;
   protected double myG1 = DEFAULT_G1;
   protected double myG2 = DEFAULT_G2;

   protected double myP3;
   protected double myP4;
   protected boolean myP3P4Valid = false;

   protected PropertyMode myMaxLambdaMode = PropertyMode.Inherited;
   protected PropertyMode myOptLambdaMode = PropertyMode.Inherited;
   protected PropertyMode myMaxStressMode = PropertyMode.Inherited;
   protected PropertyMode myExpStressCoeffMode = PropertyMode.Inherited;
   protected PropertyMode myUncrimpingFactorMode = PropertyMode.Inherited;
   protected PropertyMode myG1Mode = PropertyMode.Inherited;
   protected PropertyMode myG2Mode = PropertyMode.Inherited;

   protected ScalarFieldPointFunction myMaxLambdaFunction = null;
   protected ScalarFieldPointFunction myOptLambdaFunction = null;
   protected ScalarFieldPointFunction myMaxStressFunction = null;
   protected ScalarFieldPointFunction myExpStressCoeffFunction = null;
   protected ScalarFieldPointFunction myUncrimpingFactorFunction = null;
   protected ScalarFieldPointFunction myG1Function = null;
   protected ScalarFieldPointFunction myG2Function = null;

   protected SymmetricMatrix3d myB = new SymmetricMatrix3d();
   protected SymmetricMatrix3d myB2 = new SymmetricMatrix3d();
   protected Vector3d myTmp = new Vector3d();
   protected SymmetricMatrix3d myMat = new SymmetricMatrix3d();

   // Set this true to keep the tangent matrix continuous (and symmetric) at
   // lam = lamOpt, at the expense of slightly negative forces for lam < lamOpt
   protected static boolean myZeroForceBelowLamOptP = false;

   public FullBlemkerMuscle() {
      super();
   }

   public FullBlemkerMuscle (
      double maxLam, double optLam, double maxStress, double expStress,
      double uncrimp, double g1, double g2) {
      this();
      setMaxLambda (maxLam);
      setOptLambda (optLam);
      setMaxStress (maxStress);
      setExpStressCoeff (expStress);
      setUncrimpingFactor (uncrimp);
      setG1 (g1);
      setG2 (g2);
   }

   public static FunctionPropertyList myProps =
      new FunctionPropertyList (FullBlemkerMuscle.class, MuscleMaterial.class);   

   static {
      myProps.addInheritableWithFunction (
         "maxLambda", "maximum stretch for straightened fibres",
         DEFAULT_MAX_LAMBDA, "%.8g");
      myProps.addInheritableWithFunction (
         "optLambda", "optimal stretch for straightened fibres",
         DEFAULT_OPT_LAMBDA, "%.8g");
      myProps.addInheritableWithFunction (
         "maxStress", "maximum isometric stress", DEFAULT_MAX_STRESS);
      myProps.addInheritableWithFunction (
         "expStressCoeff", "exponential stress coefficient",
         DEFAULT_EXP_STRESS_COEFF);
      myProps.addInheritableWithFunction (
         "uncrimpingFactor", "fibre uncrimping factor",
         DEFAULT_UNCRIMPING_FACTOR);
      myProps.addInheritableWithFunction (
         "G1", "along-fibre shear", DEFAULT_G1);
      myProps.addInheritableWithFunction (
         "G2", "cross-fibre shear", DEFAULT_G2);
   }

   public FunctionPropertyList getAllPropertyInfo() {
      return myProps;
   }

   // BEGIN parameter accessors

   // maxLambda

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

   public double getMaxLambda (FieldPoint dp) {
      if (myMaxLambdaFunction == null) {
         return getMaxLambda();
      }
      else {
         return myMaxLambdaFunction.eval (dp);
      }
   }

   public ScalarFieldPointFunction getMaxLambdaFunction() {
      return myMaxLambdaFunction;
   }
      
   public void setMaxLambdaFunction (ScalarFieldPointFunction func) {
      myMaxLambdaFunction = func;
      notifyHostOfPropertyChange();
   }
   
   public void setMaxLambdaField (
      ScalarField field, boolean useRestPos) {
      myMaxLambdaFunction = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getMaxLambdaField () {
      return FieldUtils.getFieldFromFunction (myMaxLambdaFunction);
   }

   // optLambda

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

   public double getOptLambda (FieldPoint dp) {
      if (myOptLambdaFunction == null) {
         return getOptLambda();
      }
      else {
         return myOptLambdaFunction.eval (dp);
      }
   }

   public ScalarFieldPointFunction getOptLambdaFunction() {
      return myOptLambdaFunction;
   }
      
   public void setOptLambdaFunction (ScalarFieldPointFunction func) {
      myOptLambdaFunction = func;
      notifyHostOfPropertyChange();
   }
   
   public void setOptLambdaField (
      ScalarField field, boolean useRestPos) {
      myOptLambdaFunction = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getOptLambdaField () {
      return FieldUtils.getFieldFromFunction (myOptLambdaFunction);
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

   public double getMaxStress (FieldPoint dp) {
      if (myMaxStressFunction == null) {
         return getMaxStress();
      }
      else {
         return myMaxStressFunction.eval (dp);
      }
   }

   public ScalarFieldPointFunction getMaxStressFunction() {
      return myMaxStressFunction;
   }
      
   public void setMaxStressFunction (ScalarFieldPointFunction func) {
      myMaxStressFunction = func;
      notifyHostOfPropertyChange();
   }
   
   public void setMaxStressField (
      ScalarField field, boolean useRestPos) {
      myMaxStressFunction = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getMaxStressField () {
      return FieldUtils.getFieldFromFunction (myMaxStressFunction);
   }

   // expStressCoeff

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

   public double getExpStressCoeff (FieldPoint dp) {
      if (myExpStressCoeffFunction == null) {
         return getExpStressCoeff();
      }
      else {
         return myExpStressCoeffFunction.eval (dp);
      }
   }

   public ScalarFieldPointFunction getExpStressCoeffFunction() {
      return myExpStressCoeffFunction;
   }
      
   public void setExpStressCoeffFunction (ScalarFieldPointFunction func) {
      myExpStressCoeffFunction = func;
      notifyHostOfPropertyChange();
   }
   
   public void setExpStressCoeffField (
      ScalarField field, boolean useRestPos) {
      myExpStressCoeffFunction = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getExpStressCoeffField () {
      return FieldUtils.getFieldFromFunction (myExpStressCoeffFunction);
   }

   // uncrimpingFactor

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

   public double getUncrimpingFactor (FieldPoint dp) {
      if (myUncrimpingFactorFunction == null) {
         return getUncrimpingFactor();
      }
      else {
         return myUncrimpingFactorFunction.eval (dp);
      }
   }

   public ScalarFieldPointFunction getUncrimpingFactorFunction() {
      return myUncrimpingFactorFunction;
   }
      
   public void setUncrimpingFactorFunction (ScalarFieldPointFunction func) {
      myUncrimpingFactorFunction = func;
      notifyHostOfPropertyChange();
   }
   
   public void setUncrimpingFactorField (
      ScalarField field, boolean useRestPos) {
      myUncrimpingFactorFunction = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getUncrimpingFactorField () {
      return FieldUtils.getFieldFromFunction (myUncrimpingFactorFunction);
   }

   // G1

   public synchronized void setG1 (double G1) {
      myG1 = G1;
      myG1Mode =
         PropertyUtils.propagateValue (this, "G1", myG1, myG1Mode);
      notifyHostOfPropertyChange();
   }

   public double getG1() {
      return myG1;
   }

   public void setG1Mode (PropertyMode mode) {
      myG1Mode =
         PropertyUtils.setModeAndUpdate (
            this, "G1", myG1Mode, mode);
   }

   public PropertyMode getG1Mode() {
      return myG1Mode;
   }

   public double getG1 (FieldPoint dp) {
      if (myG1Function == null) {
         return getG1();
      }
      else {
         return myG1Function.eval (dp);
      }
   }

   public ScalarFieldPointFunction getG1Function() {
      return myG1Function;
   }
      
   public void setG1Function (ScalarFieldPointFunction func) {
      myG1Function = func;
      notifyHostOfPropertyChange();
   }
   
   public void setG1Field (
      ScalarField field, boolean useRestPos) {
      myG1Function = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getG1Field () {
      return FieldUtils.getFieldFromFunction (myG1Function);
   }

   // G2

   public synchronized void setG2 (double G2) {
      myG2 = G2;
      myG2Mode =
         PropertyUtils.propagateValue (this, "G2", myG2, myG2Mode);
      notifyHostOfPropertyChange();
   }

   public double getG2() {
      return myG2;
   }

   public void setG2Mode (PropertyMode mode) {
      myG2Mode =
         PropertyUtils.setModeAndUpdate (
            this, "G2", myG2Mode, mode);
   }

   public PropertyMode getG2Mode() {
      return myG2Mode;
   }

   public double getG2 (FieldPoint dp) {
      if (myG2Function == null) {
         return getG2();
      }
      else {
         return myG2Function.eval (dp);
      }
   }

   public ScalarFieldPointFunction getG2Function() {
      return myG2Function;
   }
      
   public void setG2Function (ScalarFieldPointFunction func) {
      myG2Function = func;
      notifyHostOfPropertyChange();
   }
   
   public void setG2Field (
      ScalarField field, boolean useRestPos) {
      myG2Function = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getG2Field () {
      return FieldUtils.getFieldFromFunction (myG2Function);
   }

   // END parameter accessors

   private static double BIG_CRITERIA = 67108865.0;

   private double acosh (double x) {
      if (x < 1) {
	x = -1;			/* NaN */
      }
      else if (x == 1) {
	return 0;
      }
      else if (x > BIG_CRITERIA) {
         // bigger than this and sqrt(x^2-1) equals x to numeric precision
	x += x;
      }
      else {
	x += Math.sqrt((x + 1) * (x - 1));
      }
      return Math.log(x);
   }

   private final double square (double x) {
      return x*x;
   }

   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Vector3d dir0, double excitation, MaterialStateObject state) {

      double maxLambda = getMaxLambda(def);
      double optLambda = getOptLambda(def);
      double maxStress = getMaxStress(def);
      double g1 = getG1(def);
      double g2 = getG2(def);
      
      Vector3d a = myTmp;
      def.getF().mul (a, dir0);
      double mag = a.norm();
      double J = def.getDetF();
      double lamd;
      a.scale (1/mag);
      lamd = mag*Math.pow(J, -1.0/3.0);

      double I4 = lamd*lamd;

      // BEGIN G1, G2

      double I1 = 0;
      double I2 = 0;
      double I5 = 0;

       // calculate deviatoric left Cauchy-Green tensor
      computeDevLeftCauchyGreen(myB,def);     
      
      // calculate square of B
      myB2.mulTransposeLeft (myB);
      Vector3d Ba = new Vector3d();
      myB.mul (Ba, a);

      // Invariants of deviatoric part of B
      I1 = myB.trace();
      I2 = 0.5*(I1*I1 - myB2.trace());      
      I5 = I4*Ba.dot(a);

      // calculate new invariants
      double g = I5/(I4*I4) - 1;
      double b1 = (g > 0 ? Math.sqrt(g) : 0);
	
      double b2 = acosh(0.5*(I1*I4 - I5)/lamd);

      // set beta and ksi to their limit values
      double beta = 1.0;
      double ksi = -1.0/3.0;

      // calculate omage (w)
      double w = 0.5*(I1*I4 - I5)/lamd;
      
      // if w not equals unity, we can calculate beta and ksi
      if (w > 1.0001) {
         beta = b2/Math.sqrt(w*w-1);
         ksi = (1.0/(w*w-1))*(1 - w*b2/Math.sqrt(w*w-1));
      }

      // calculate derivatives for F1
      double F1D4 = -2*g1*(I5/(I4*I4*I4));
      double F1D5 = g1/(I4*I4);

      // calculate derivatives for F2
      double F2D1 =  g2*beta*lamd;
      double F2D4 =  g2*beta*(I1*I4 + I5)*0.5*Math.pow(I4, -1.5);
      double F2D5 = -g2*beta/lamd;

      // END G1, G2

      // calculate derivatives for F3
      // these terms are proposed to fix the zero-stress problem
      //double F3D4 = 9.0*m_G3*0.125*Math.log(I4)/I4;

      double P1 = getExpStressCoeff(def);
      double P2 = getUncrimpingFactor(def);
      double lofl = optLambda;

      double Fa = 0, Fp = 0;

      if (!myP3P4Valid) {
         myP3 = P1*P2*Math.exp(P2*(maxLambda/lofl-1));
         myP4 = P1*(Math.exp(P2*(maxLambda/lofl-1))-1) - myP3*maxLambda/lofl;
         myP3P4Valid = true;
      }

      // calculate passive fiber force
      if (myZeroForceBelowLamOptP && lamd <= lofl) {
	 // Fp is zero if less than lofl
	 Fp = 0;
      } 
      else if (lamd < maxLambda) {
         Fp = P1 * (Math.exp(P2 * (lamd / lofl - 1)) - 1);
      } 
      else {
         Fp = myP3 * lamd / lofl + myP4;
      }

      // calculate active fiber force
      if ((lamd <= 0.4*lofl) || (lamd >= 1.6*lofl)) {
         // FEBio has added this part to make sure that 
         // Fa is zero outside the range [0.4, 1.6] *lofl
         Fa = 0;
      }
      else {
         if (lamd <= 0.6*lofl) {
            Fa = 9*square(lamd/lofl - 0.4);
         }
         else if (lamd >= 1.4*lofl) {
            Fa = 9*square(lamd/lofl - 1.6);
         }
         else if ((lamd >= 0.6*lofl) && (lamd <= 1.4*lofl)) {
            Fa = 1 - 4*square(1 - lamd/lofl);
         }
      }

      // calculate total fiber force
      double FfDl = maxStress*(Fp + excitation*Fa)/lofl;
      double FfD4  = 0.5*FfDl/lamd;
      
      double W1, W2, W4, W5;
      
      W1 = F2D1;
      W2 = 0;
      W4 = F1D4 + F2D4 + FfD4;
      W5 = F1D5 + F2D5;

      myMat.scale (W1 + W2*I1, myB);
      myMat.scaledAdd (-W2, myB2, myMat);
      myMat.addScaledDyad (I4*W4, a);
      myMat.addScaledSymmetricDyad (I4*W5, Ba, a);
      myMat.deviator();
      myMat.scale (2.0/J);

      sigma.set (myMat);

      if (D != null) {
         double Ji = 1/J;

         // -- A. matrix contribution --
         // calculate derivatives for F1

         double F1D44 = 6*g1*(I5/(I4*I4*I4*I4));
         double F1D45 = -2*g1/(I4*I4*I4);

         // calculate derivatives for F2

         double F2D11 = ksi*g2*I4*0.5;
         double F2D44 =
            2.0*g2*ksi*Math.pow(0.25*(I1*I4+I5)/Math.pow(I4, 1.5), 2) -
            g2*beta*(0.25*(I1*I4 + 3*I5) /Math.pow(I4, 2.5));
         double F2D55 = 0.5*g2*ksi/I4;
         double F2D14 = g2*beta*0.5/lamd + g2*ksi*(I1*I4+I5)*0.25/I4;
         double F2D15 = -0.5*g2*ksi;
         double F2D45 =
            g2*beta*0.5*Math.pow(I4, -1.5) -
            g2*ksi*0.25*(I1*I4+I5)/(I4*I4);

         double FaDl = 0, FpDl;

         // calculate passive fiber force
         if (myZeroForceBelowLamOptP && lamd <= lofl) {
            // Fp is zero if less than lofl
            FpDl = 0;
         }
         else if (lamd < maxLambda) {
            FpDl = P1 * P2 * Math.exp(P2 * (lamd / lofl - 1)) / lofl;
         } 
         else {
            FpDl = myP3 / lofl;
         }

         // calculate active fiber force
         if ((lamd <= 0.4*lofl) || (lamd >= 1.6*lofl)) {
            // FEBio has added this part to make sure that 
            // Fa is zero outside the range [0.4, 1.6] *lofl
            FaDl = 0;
         }
         else {
            if (lamd <= 0.6*lofl) {
               FaDl = 18*(lamd/lofl - 0.4)/lofl;
            }
            else if (lamd >= 1.4*lofl) {
               FaDl = 18*(lamd/lofl - 1.6)/lofl;
            }
            else if ((lamd >= 0.6*lofl) && (lamd <= 1.4*lofl)) {
               FaDl = 8*(1 - lamd/lofl)/lofl;
            }
         }

         // calculate total fiber force

         double FfDll = maxStress*(FpDl + excitation*FaDl)/lofl;
         double FfD44 = 0.25*(FfDll - FfDl/lamd)/I4;

         // calculate second derivatives
         double W11, W12, W22, W14, W24, W15, W25, W44, W45, W55;

         W11 = F2D11;
         W12 = 0;
         W22 = 0;
         W14 = F2D14;
         W24 = 0;
         W15 = F2D15;
         W25 = 0;
         W44 = F1D44 + F2D44 + FfD44; // + F3D44
         W45 = F1D45 + F2D45;
         W55 = F2D55;

         // System.out.printf ("W1=%g W2=%g W4=%g W5=%g\n", W1, W2, W4, W5);
         // System.out.printf ("W11=%g W12=%g W22=%g W14=%g W24=%g\n",
         //                    W11, W12, W22, W14, W24);
         // System.out.printf ("W15=%g W25=%g W44=%g W45=%g W55=%g\n",
         //                    W15, W25, W44, W45, W55);

         // calculate dWdC:C
         double WCC = W1*I1 + 2*W2*I2 + W4*I4 + 2*W5*I5;

         // calculate C:d2WdCdC:C
         double CW2CCC =
            (W11*I1 + W12*I1*I1 + W2*I1 + 2*W12*I2 + 2*W22*I1*I2 +
             W14*I4 + W24*I1*I4 + 2*W15*I5 + 2*W25*I1*I5)*I1 -
            (W12*I1 + 2*W22*I2 + W2 + W24*I4 + 2*W25*I5)*(I1*I1 - 2*I2) +
            (W14*I1 + 2*W24*I2 + W44*I4 + 2*W45*I5)*I4 +
            (W15*I1 + 2*W25*I2 + W45*I4 + 2*W55*I5)*2*I5 +
            2*W5*I5;

         SymmetricMatrix3d AA = new SymmetricMatrix3d();
         SymmetricMatrix3d AB = new SymmetricMatrix3d();
         SymmetricMatrix3d WCCC = myMat;

         AA.dyad (a);
         AB.symmetricDyad (a, Ba);

         WCCC.scale (
            W11*I1 + W12*I1*I1 + W2*I1 + 2*W12*I2 + 2*W22*I1*I2 +
            W14*I4 + W24*I1*I4 + 2*W15*I5 + 2*W25*I1*I5, myB);
         WCCC.scaledAdd (
            -(W12*I1 + 2*W22*I2 + W2 + W24*I4 + 2*W25*I5), myB2, WCCC);
         WCCC.scaledAdd (
            (W14*I1 + 2*W24*I2 + W44*I4 + 2*W45*I5)*I4, AA, WCCC);
         WCCC.scaledAdd (
            (W15*I1 + 2*W25*I2 + W45*I4 + 2*W55*I5 + W5)*I4, AB, WCCC);

         D.setZero();
         TensorUtils.addTensorProduct (
            D, (W11 + 2.0*W12*I1 + W2 + W22*I1*I1)*4*Ji, myB);
         TensorUtils.addSymmetricTensorProduct (D, -(W12+W22*I1)*4*Ji, myB, myB2);
         TensorUtils.addTensorProduct (D, W22*4*Ji, myB2);
         TensorUtils.addSymmetricTensorProduct (D, (W14+W24*I1)*I4*4*Ji, myB, AA);
         TensorUtils.addSymmetricTensorProduct (D, (W15+W25*I1)*I4*4*Ji, myB, AB);
         TensorUtils.addSymmetricTensorProduct (D, (-W24*I4)*4*Ji, myB2, AA);

         TensorUtils.addTensorProduct (D, (W44*I4*I4)*4*Ji, AA);
         TensorUtils.addSymmetricTensorProduct (D, (W45*I4)*I4*4*Ji, AA, AB);
         TensorUtils.addTensorProduct (D, (W55)*I4*I4*4*Ji, AB);
         TensorUtils.addSymmetricTensorProduct4 (D, W5*I4*4*Ji, AA, myB);
        
         TensorUtils.addScaledIdentityProduct (D, 4/9.0*Ji*(CW2CCC-WCC));
         WCCC.scale (-4/3.0*Ji);
         TensorUtils.addSymmetricIdentityProduct (D, WCCC);
         TensorUtils.addScaledIdentity (D, 4/3.0*Ji*WCC);

         // compute stress (in myMat) due to this material 
         myMat.scale (W1 + W2*I1, myB);
         myMat.scaledAdd (-W2, myB2, myMat);

         myMat.scaledAdd (I4*W4, AA);
         myMat.scaledAdd (I4*W5, AB);
         myMat.deviator();
         myMat.scale (2.0/J);
        
         //myMat.set (def.getStrain());
         //myMat.deviator();
         myMat.scale (-2.0/3.0);
         TensorUtils.addSymmetricIdentityProduct (D, myMat);

         D.setLowerToUpper();
      }
   }

   public double computeStretch (Vector3d dir0, DeformedPoint def) {
      Vector3d a = myTmp;
      def.getF().mul(a, dir0);
      double mag = a.norm();
      double J = def.getDetF();
      return mag * Math.pow(J, -1.0 / 3.0);
   }

   public boolean equals (MuscleMaterial mat) {
      if (!(mat instanceof FullBlemkerMuscle)) {
         return false;
      }
      FullBlemkerMuscle mrm = (FullBlemkerMuscle)mat;
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

   public FullBlemkerMuscle clone() {
      FullBlemkerMuscle mat = (FullBlemkerMuscle)super.clone();
      mat.myTmp = new Vector3d();
      mat.myMat = new SymmetricMatrix3d();
      mat.myB = new SymmetricMatrix3d();
      mat.myB2 = new SymmetricMatrix3d();
      return mat;
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

   /**
    * Computes the left deviatoric Cauchy-Green tensor from the deformation
    * gradient.
    */
   public void computeDevLeftCauchyGreen (
      SymmetricMatrix3d BD, DeformedPoint def) {
      BD.mulTransposeRight (def.getF());
      BD.scale (Math.pow(def.getDetF(), -2.0/3.0));
   }
   
}
