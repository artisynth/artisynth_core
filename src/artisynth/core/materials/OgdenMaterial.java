package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class OgdenMaterial extends IncompressibleMaterialBase {

   public static FieldPropertyList myProps =
      new FieldPropertyList (OgdenMaterial.class, IncompressibleMaterialBase.class);

   protected static double DEFAULT_MU1    = 300000.0;
   protected static double DEFAULT_MU2    = 0.0;
   protected static double DEFAULT_MU3    = 0.0;
   protected static double DEFAULT_MU4    = 0.0;
   protected static double DEFAULT_MU5    = 0.0;
   protected static double DEFAULT_MU6    = 0.0;
   protected static double DEFAULT_ALPHA1 = 2;
   protected static double DEFAULT_ALPHA2 = 2;
   protected static double DEFAULT_ALPHA3 = 2;
   protected static double DEFAULT_ALPHA4 = 2;
   protected static double DEFAULT_ALPHA5 = 2;
   protected static double DEFAULT_ALPHA6 = 2;

   private int NMAX         = 6;
   private double[] myMu    = {
      DEFAULT_MU1, DEFAULT_MU2, DEFAULT_MU3,
      DEFAULT_MU4, DEFAULT_MU5, DEFAULT_MU6} ;
   private double[] myAlpha = {
      DEFAULT_ALPHA1, DEFAULT_ALPHA2, DEFAULT_ALPHA3,
      DEFAULT_ALPHA4, DEFAULT_ALPHA5, DEFAULT_ALPHA6} ;

   PropertyMode myMu1Mode    = PropertyMode.Inherited;
   PropertyMode myMu2Mode    = PropertyMode.Inherited;
   PropertyMode myMu3Mode    = PropertyMode.Inherited;
   PropertyMode myMu4Mode    = PropertyMode.Inherited;
   PropertyMode myMu5Mode    = PropertyMode.Inherited;
   PropertyMode myMu6Mode    = PropertyMode.Inherited;
   PropertyMode myAlpha1Mode = PropertyMode.Inherited;
   PropertyMode myAlpha2Mode = PropertyMode.Inherited;
   PropertyMode myAlpha3Mode = PropertyMode.Inherited;
   PropertyMode myAlpha4Mode = PropertyMode.Inherited;
   PropertyMode myAlpha5Mode = PropertyMode.Inherited;
   PropertyMode myAlpha6Mode = PropertyMode.Inherited;

   ScalarFieldComponent myMu1Field    = null;
   ScalarFieldComponent myMu2Field    = null;
   ScalarFieldComponent myMu3Field    = null;
   ScalarFieldComponent myMu4Field    = null;
   ScalarFieldComponent myMu5Field    = null;
   ScalarFieldComponent myMu6Field    = null;
   ScalarFieldComponent myAlpha1Field = null;
   ScalarFieldComponent myAlpha2Field = null;
   ScalarFieldComponent myAlpha3Field = null;
   ScalarFieldComponent myAlpha4Field = null;
   ScalarFieldComponent myAlpha5Field = null;
   ScalarFieldComponent myAlpha6Field = null;

   private SymmetricMatrix3d myB;
   private SymmetricMatrix3d myB2;
   private SymmetricMatrix3d myTmp;
   
   static {
      myProps.addInheritableWithField (
         "Alpha1:Inherited", "Alpha1", DEFAULT_ALPHA1);
      myProps.addInheritableWithField (
         "Alpha2:Inherited", "Alpha2", DEFAULT_ALPHA2);
      myProps.addInheritableWithField (
         "Alpha3:Inherited", "Alpha3", DEFAULT_ALPHA3);
      myProps.addInheritableWithField (
         "Alpha4:Inherited", "Alpha4", DEFAULT_ALPHA4);
      myProps.addInheritableWithField (
         "Alpha5:Inherited", "Alpha5", DEFAULT_ALPHA5);
      myProps.addInheritableWithField (
         "Alpha6:Inherited", "Alpha6", DEFAULT_ALPHA6);
      myProps.addInheritableWithField (
         "Mu1:Inherited", "Mu1", DEFAULT_MU1);
      myProps.addInheritableWithField (
         "Mu2:Inherited", "Mu2", DEFAULT_MU2);
      myProps.addInheritableWithField (
         "Mu3:Inherited", "Mu3", DEFAULT_MU3);
      myProps.addInheritableWithField (
         "Mu4:Inherited", "Mu4", DEFAULT_MU4);
      myProps.addInheritableWithField (
         "Mu5:Inherited", "Mu5", DEFAULT_MU5);
      myProps.addInheritableWithField (
         "Mu6:Inherited", "Mu6", DEFAULT_MU6);
   }

   public FieldPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public OgdenMaterial () {
      myB   = new SymmetricMatrix3d();
      myB2  = new SymmetricMatrix3d();
      myTmp = new SymmetricMatrix3d();
   }
   
   public OgdenMaterial (double[] mu, double[] alpha, double kappa) {
      this();
      setMu1 (mu[0]);
      setMu2 (mu[1]);
      setMu3 (mu[2]);
      setMu4 (mu[3]);
      setMu5 (mu[4]);
      setMu6 (mu[5]);
      setAlpha1 (alpha[0]);
      setAlpha2 (alpha[1]);
      setAlpha3 (alpha[2]);
      setAlpha4 (alpha[3]);
      setAlpha5 (alpha[4]);
      setAlpha6 (alpha[5]);
      setBulkModulus (kappa);
   }

   public synchronized void setMu1 (double mu) {
      myMu[0] = mu;
      myMu1Mode =
         PropertyUtils.propagateValue (this, "Mu1", myMu[0], myMu1Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu2 (double mu) {
      myMu[1] = mu;
      myMu2Mode =
         PropertyUtils.propagateValue (this, "Mu2", myMu[1], myMu2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu3 (double mu) {
      myMu[2] = mu;
      myMu2Mode =
         PropertyUtils.propagateValue (this, "Mu2", myMu[2], myMu2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu4 (double mu) {
      myMu[3] = mu;
      myMu2Mode =
         PropertyUtils.propagateValue (this, "Mu2", myMu[3], myMu2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu5 (double mu) {
      myMu[4] = mu;
      myMu2Mode =
         PropertyUtils.propagateValue (this, "Mu2", myMu[4], myMu2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu6 (double mu) {
      myMu[5] = mu;
      myMu6Mode =
         PropertyUtils.propagateValue (this, "Mu6", myMu[5], myMu6Mode);
      notifyHostOfPropertyChange();
   }

   public double getMu1() {
      return myMu[0];
   }

   public double getMu2() {
      return myMu[1];
   }

   public double getMu3() {
      return myMu[2];
   }
   
   public double getMu4() {
      return myMu[3];
   }
   
   public double getMu5() {
      return myMu[4];
   }
   
   public double getMu6() {
      return myMu[5];
   }
   
   public void setMu1Mode (PropertyMode mode) {
      myMu1Mode =
         PropertyUtils.setModeAndUpdate (this, "Mu1", myMu1Mode, mode);
   }

   public void setMu2Mode (PropertyMode mode) {
      myMu2Mode =
         PropertyUtils.setModeAndUpdate (this, "Mu2", myMu2Mode, mode);
   }

   public void setMu3Mode (PropertyMode mode) {
      myMu3Mode =
         PropertyUtils.setModeAndUpdate (this, "Mu3", myMu3Mode, mode);
   }

   public void setMu4Mode (PropertyMode mode) {
      myMu4Mode =
         PropertyUtils.setModeAndUpdate (this, "Mu4", myMu4Mode, mode);
   }

   public void setMu5Mode (PropertyMode mode) {
      myMu5Mode =
         PropertyUtils.setModeAndUpdate (this, "Mu5", myMu5Mode, mode);
   }

   public void setMu6Mode (PropertyMode mode) {
      myMu6Mode =
         PropertyUtils.setModeAndUpdate (this, "Mu6", myMu6Mode, mode);
   }

   public PropertyMode getMu1Mode() {
      return myMu1Mode;
   }

   public PropertyMode getMu2Mode() {
      return myMu2Mode;
   }

   public PropertyMode getMu3Mode() {
	  return myMu3Mode;
   }
   
   public PropertyMode getMu4Mode() {
	  return myMu4Mode;
   }

   public PropertyMode getMu5Mode() {
	  return myMu5Mode;
   }

   public PropertyMode getMu6Mode() {
	  return myMu6Mode;
   }

   public double getMu1 (FemFieldPoint dp) {
      return (myMu1Field == null ? getMu1() : myMu1Field.getValue(dp));
   }

   public double getMu2 (FemFieldPoint dp) {
      return (myMu2Field == null ? getMu2() : myMu2Field.getValue(dp));
   }

   public double getMu3 (FemFieldPoint dp) {
      return (myMu3Field == null ? getMu3() : myMu3Field.getValue(dp));
   }

   public double getMu4 (FemFieldPoint dp) {
      return (myMu4Field == null ? getMu4() : myMu4Field.getValue(dp));
   }

   public double getMu5 (FemFieldPoint dp) {
      return (myMu5Field == null ? getMu5() : myMu5Field.getValue(dp));
   }

   public double getMu6 (FemFieldPoint dp) {
      return (myMu6Field == null ? getMu6() : myMu6Field.getValue(dp));
   }

   public ScalarFieldComponent getMu1Field() {
      return myMu1Field;
   }

   public ScalarFieldComponent getMu2Field() {
      return myMu2Field;
   }

   public ScalarFieldComponent getMu3Field() {
      return myMu3Field;
   }

   public ScalarFieldComponent getMu4Field() {
      return myMu4Field;
   }

   public ScalarFieldComponent getMu5Field() {
      return myMu5Field;
   }

   public ScalarFieldComponent getMu6Field() {
      return myMu6Field;
   }

   public void setMu1Field (ScalarFieldComponent func) {
      myMu1Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setMu2Field (ScalarFieldComponent func) {
      myMu2Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setMu3Field (ScalarFieldComponent func) {
      myMu3Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setMu4Field (ScalarFieldComponent func) {
      myMu4Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setMu5Field (ScalarFieldComponent func) {
      myMu5Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setMu6Field (ScalarFieldComponent func) {
      myMu6Field = func;
      notifyHostOfPropertyChange();
   }

   public synchronized void setAlpha1 (double alpha) {
      myAlpha[0] = alpha;
      myAlpha1Mode =
         PropertyUtils.propagateValue (this, "Alpha1", myAlpha[0], myAlpha1Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setAlpha2 (double alpha) {
      myAlpha[1] = alpha;
      myAlpha2Mode =
         PropertyUtils.propagateValue (this, "Alpha2", myAlpha[1], myAlpha2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setAlpha3 (double alpha) {
      myAlpha[2] = alpha;
      myAlpha3Mode =
         PropertyUtils.propagateValue (this, "Alpha3", myAlpha[2], myAlpha3Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setAlpha4 (double alpha) {
      myAlpha[3] = alpha;
      myAlpha4Mode =
         PropertyUtils.propagateValue (this, "Alpha4", myAlpha[3], myAlpha4Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setAlpha5 (double alpha) {
      myAlpha[4] = alpha;
      myAlpha5Mode =
         PropertyUtils.propagateValue (this, "Alpha5", myAlpha[4], myAlpha5Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setAlpha6 (double alpha) {
      myAlpha[5] = alpha;
      myAlpha6Mode =
         PropertyUtils.propagateValue (this, "Alpha6", myAlpha[5], myAlpha6Mode);
      notifyHostOfPropertyChange();
   }

   public double getAlpha1() {
      return myAlpha[0];
   }

   public double getAlpha2() {
      return myAlpha[1];
   }

   public double getAlpha3() {
      return myAlpha[2];
   }

   public double getAlpha4() {
      return myAlpha[3];
   }

   public double getAlpha5() {
      return myAlpha[4];
   }

   public double getAlpha6() {
      return myAlpha[5];
   }

   public void setAlpha1Mode (PropertyMode mode) {
      myAlpha1Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha1", myAlpha1Mode, mode);
   }

   public void setAlpha2Mode (PropertyMode mode) {
      myAlpha2Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha2", myAlpha2Mode, mode);
   }

   public void setAlpha3Mode (PropertyMode mode) {
      myAlpha3Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha3", myAlpha3Mode, mode);
   }
   
   public void setAlpha4Mode (PropertyMode mode) {
      myAlpha4Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha4", myAlpha4Mode, mode);
   }
   
   public void setAlpha5Mode (PropertyMode mode) {
      myAlpha5Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha5", myAlpha5Mode, mode);
   }
   
   public void setAlpha6Mode (PropertyMode mode) {
     myAlpha6Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha6", myAlpha6Mode, mode);
   }

   public PropertyMode getAlpha1Mode() {
      return myAlpha1Mode;
   }
   
   public PropertyMode getAlpha2Mode() {
      return myAlpha2Mode;
   }
	   
   public PropertyMode getAlpha3Mode() {
      return myAlpha3Mode;
   }
	   
   public PropertyMode getAlpha4Mode() {
      return myAlpha4Mode;
   }
	   
   public PropertyMode getAlpha5Mode() {
      return myAlpha5Mode;
   }
	   
   public PropertyMode getAlpha6Mode() {
      return myAlpha6Mode;
   }
	   
   public double getAlpha1 (FemFieldPoint dp) {
      return (myAlpha1Field == null ? getAlpha1() : myAlpha1Field.getValue(dp));
   }

   public double getAlpha2 (FemFieldPoint dp) {
      return (myAlpha2Field == null ? getAlpha2() : myAlpha2Field.getValue(dp));
   }

   public double getAlpha3 (FemFieldPoint dp) {
      return (myAlpha3Field == null ? getAlpha3() : myAlpha3Field.getValue(dp));
   }

   public double getAlpha4 (FemFieldPoint dp) {
      return (myAlpha4Field == null ? getAlpha4() : myAlpha4Field.getValue(dp));
   }

   public double getAlpha5 (FemFieldPoint dp) {
      return (myAlpha5Field == null ? getAlpha5() : myAlpha5Field.getValue(dp));
   }

   public double getAlpha6 (FemFieldPoint dp) {
      return (myAlpha6Field == null ? getAlpha6() : myAlpha6Field.getValue(dp));
   }

   public ScalarFieldComponent getAlpha1Field() {
      return myAlpha1Field;
   }

   public ScalarFieldComponent getAlpha2Field() {
      return myAlpha2Field;
   }

   public ScalarFieldComponent getAlpha3Field() {
      return myAlpha3Field;
   }

   public ScalarFieldComponent getAlpha4Field() {
      return myAlpha4Field;
   }

   public ScalarFieldComponent getAlpha5Field() {
      return myAlpha5Field;
   }

   public ScalarFieldComponent getAlpha6Field() {
      return myAlpha6Field;
   }

   public void setAlpha1Field (ScalarFieldComponent func) {
      myAlpha1Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setAlpha2Field (ScalarFieldComponent func) {
      myAlpha2Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setAlpha3Field (ScalarFieldComponent func) {
      myAlpha3Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setAlpha4Field (ScalarFieldComponent func) {
      myAlpha4Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setAlpha5Field (ScalarFieldComponent func) {
      myAlpha5Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setAlpha6Field (ScalarFieldComponent func) {
      myAlpha6Field = func;
      notifyHostOfPropertyChange();
   }

   public final double sqr (double x) {
      return x*x;
   }

   // See JC Simo and RL Taylor, Quasi-Incompressible Finite Elasticity in
   // Principal Stretches.  Continuum Basis and Numerical Algorithms, Comp
   // Methods Appl Mech Eng, 85, 273-310, 1991
   public void computeDevStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {

      double J = def.getDetF();

      sigma.setZero();

      // Calculate Deviatoric left Cauchy-Green tensor
      computeDevLeftCauchyGreen(myB,def);

      Vector3d lam = new Vector3d();
      Vector3d lamSqr  = new Vector3d();
      Matrix3d principalDirection = new Matrix3d();

      double mu[] = new double[NMAX];
      mu[0] = getMu1(def);
      mu[1] = getMu2(def);
      mu[2] = getMu3(def);
      mu[3] = getMu4(def);
      mu[4] = getMu5(def);
      mu[5] = getMu6(def);
      
      double alpha[] = new double[NMAX];
      alpha[0] = getAlpha1(def);
      alpha[1] = getAlpha2(def);
      alpha[2] = getAlpha3(def);
      alpha[3] = getAlpha4(def);
      alpha[4] = getAlpha5(def);
      alpha[5] = getAlpha6(def);
      
      // Calculate principal stretches and principal directions
      // Eigenvalues of B are squares of the principal stretches
      myB.getEigenValues(lamSqr, principalDirection);
      for ( int i=0; i<3; i++) {
         lam.set(i, Math.sqrt(lamSqr.get(i)) );
      }

      Vector3d dir = new Vector3d();
      SymmetricMatrix3d douter0 = new SymmetricMatrix3d();
      principalDirection.getColumn (0, dir);
      douter0.dyad (dir);
      SymmetricMatrix3d douter1 = new SymmetricMatrix3d();
      principalDirection.getColumn (1, dir);
      douter1.dyad (dir);
      SymmetricMatrix3d douter2 = new SymmetricMatrix3d();
      principalDirection.getColumn (2, dir);
      douter2.dyad (dir);
      SymmetricMatrix3d[] douter = new SymmetricMatrix3d[] {
         douter0, douter1, douter2};

      double[][] lampows = new double[3][NMAX];
      for (int i=0; i<3; i++) {
         for (int n=0; n<NMAX; n++) {
            if (mu[n] != 0) {
               lampows[i][n] = Math.pow(lam.get(i), alpha[n]);
            }
         }
      }
      double[] pstress = new double[3];

      // Calculate principal stresses
      for (int i=0; i<3; i++) {
         for (int n=0; n<NMAX; n++) {
            if (mu[n] != 0) {
               pstress[i] += mu[n]*(lampows[i][n]-1)/(J*alpha[n]);
            }
         }
         sigma.scaledAdd (pstress[i], douter[i]);
      }
      double traceSig = sigma.trace();
      sigma.deviator();

      if (D != null) {

         SymmetricMatrix3d dc = new SymmetricMatrix3d();
         SymmetricMatrix3d ec = new SymmetricMatrix3d();

         for (int j=0; j<3; j++) {
            double sum = 0;
            for (int n=0; n<NMAX; n++) {
               if (mu[n] != 0) {
                  sum += mu[n]/alpha[n]*((alpha[n]-2)*lampows[j][n]+2)/J;
               }
            }
            dc.set (j, j, sum);
            for (int i=j+1; i<3; i++) {
               double lsqr_i = lamSqr.get(i);
               double lsqr_j = lamSqr.get(j);
               double denom = lsqr_i-lsqr_j;
               if (Math.abs (denom) > 1e-8) {
                  ec.set (i, j, 2*(lsqr_j*pstress[i]-lsqr_i*pstress[j])/denom);
               }
               else {
                  sum = 0;
                  for (int n=0; n<NMAX; ++n) {
                     if (mu[n] != 0) {
                        sum += mu[n]/alpha[n]*((alpha[n]-2)*lampows[j][n]+2)/J;
                     }
                  }
                  ec.set (i, j, sum);
               }
            }
         }

         D.setZero();
         for (int j=0; j<3; j++) {
            TensorUtils.addTensorProduct (D, dc.get(j,j), douter[j]);
            for (int i=j+1; i<3; i++) {
               TensorUtils.addSymmetricTensorProduct (
                  D, dc.get(i,j), douter[i], douter[j]);
               TensorUtils.addSymmetricTensorProduct4 (
                  D, ec.get(i,j), douter[i], douter[j]);
            }
         } 

         double traceD = TensorUtils.symmetricTrace(D);
         TensorUtils.addSymmetricIdentityDot (D, -1.0/3, D);
         TensorUtils.addScaledIdentityProduct (D, traceD/9.0);
         TensorUtils.addScaledIdentity (D, 2*traceSig/3);
         TensorUtils.addScaledIdentityProduct (D, -2*traceSig/9);
         TensorUtils.addSymmetricIdentityProduct (D, -2/3.0, sigma);
         
         D.setLowerToUpper();
      }
   }

   public double computeDevStrainEnergy (
      DeformedPoint def, Matrix3d Q, double excitation, 
      MaterialStateObject state) {
      
      // Calculate Deviatoric left Cauchy-Green tensor
      computeLeftCauchyGreen(myB,def);

      Vector3d principalStretch   = new Vector3d();
      Vector3d principalStretch2  = new Vector3d();
      Vector3d principalStretchDev = new Vector3d();

      double mu[] = new double[NMAX];
      mu[0] = getMu1(def);
      mu[1] = getMu2(def);
      mu[2] = getMu3(def);
      mu[3] = getMu4(def);
      mu[4] = getMu5(def);
      mu[5] = getMu6(def);
      
      double alpha[] = new double[NMAX];
      alpha[0] = getAlpha1(def);
      alpha[1] = getAlpha2(def);
      alpha[2] = getAlpha3(def);
      alpha[3] = getAlpha4(def);
      alpha[4] = getAlpha5(def);
      alpha[5] = getAlpha6(def);
      
      // Calculate principal stretches and principal directions
      // Eigenvalues of B are squares of the principal stretches
      myB.getEigenValues(principalStretch2, null);
      for ( int i=0; i<3; i++) {
         principalStretch.set(i, Math.sqrt(principalStretch2.get(i)) );
      }
      double J = def.getDetF();
      principalStretchDev.scale (Math.pow(J, -1.0 / 3.0), principalStretch);
      
      double W = 0;
      for (int n=0; n<NMAX; n++) {
         if (mu[n] != 0) {
            double sum = 0;
            sum += Math.pow(principalStretchDev.x, alpha[n]);
            sum += Math.pow(principalStretchDev.y, alpha[n]);
            sum += Math.pow(principalStretchDev.z, alpha[n]);
            sum -= 3;
            W += mu[n]/sqr(alpha[n])*sum;
         }
      }
      return W;
   }

  public boolean equals (FemMaterial mat) {
      if (!(mat instanceof OgdenMaterial)) {
         return false;
      }
      OgdenMaterial stvk = (OgdenMaterial)mat;
      if (myMu != stvk.myMu ||
          myAlpha != stvk.myAlpha) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public OgdenMaterial clone() {
      OgdenMaterial mat = (OgdenMaterial)super.clone();
      mat.myB = new SymmetricMatrix3d();
      return mat;
   }

   public static void main (String[] args) {
      OgdenMaterial mat = new OgdenMaterial();

      Matrix3d Q = new Matrix3d();

      DeformedPointBase dpnt = new DeformedPointBase();
      dpnt.setF (new Matrix3d (1.1, 0.1, 0.2, 0.3, 0.8, 0.23, 0.3, 0.1, 1.5));
     
      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();
            
      mat.computeStressAndTangent (sig, D, dpnt, Q, 1.0, null);
      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      System.out.println ("D=\n" + D.toString ("%12.6f"));
   }

   @Override
      public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         setMu1 (myMu[0]/s);
         setMu2 (myMu[1]/s);
         setMu3 (myMu[2]/s);
         setMu4 (myMu[3]/s);
         setMu5 (myMu[4]/s);
         setMu6 (myMu[5]/s);
      }
   }

   @Override
      public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         setMu1 (myMu[0]*s);
         setMu2 (myMu[1]*s);
         setMu3 (myMu[2]*s);
         setMu4 (myMu[3]*s);
         setMu5 (myMu[4]*s);
         setMu6 (myMu[5]*s);
      }
   }
   
}
