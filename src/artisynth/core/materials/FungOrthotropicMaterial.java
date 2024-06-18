package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class FungOrthotropicMaterial extends IncompressibleMaterialBase {

   public static FieldPropertyList myProps =
      new FieldPropertyList (FungOrthotropicMaterial.class, IncompressibleMaterialBase.class);

   protected static double DEFAULT_MU1 = 1000.0;
   protected static double DEFAULT_MU2 = 1000.0;
   protected static double DEFAULT_MU3 = 1000.0;
   protected static double DEFAULT_LAM11 = 2000.0;
   protected static double DEFAULT_LAM22 = 2000.0;
   protected static double DEFAULT_LAM33 = 2000.0;
   protected static double DEFAULT_LAM12 = 2000;
   protected static double DEFAULT_LAM23 = 2000;
   protected static double DEFAULT_LAM31 = 2000;
   protected static double DEFAULT_C  = 1500.0;

   private double myMu1 = DEFAULT_MU1; 
   private double myMu2 = DEFAULT_MU2; 
   private double myMu3 = DEFAULT_MU3; 
   private double myLam11 = DEFAULT_LAM11; 
   private double myLam22 = DEFAULT_LAM22; 
   private double myLam33 = DEFAULT_LAM33; 
   private double myLam12 = DEFAULT_LAM12; 
   private double myLam23 = DEFAULT_LAM23; 
   private double myLam31 = DEFAULT_LAM31; 
   private double myC = DEFAULT_C; 

   private double[]   mu = new double[3]; 
   private double[][] lam = new double[3][3]; 

   PropertyMode myMu1Mode = PropertyMode.Inherited;
   PropertyMode myMu2Mode = PropertyMode.Inherited;
   PropertyMode myMu3Mode = PropertyMode.Inherited;
   PropertyMode myLam11Mode = PropertyMode.Inherited;
   PropertyMode myLam22Mode = PropertyMode.Inherited;
   PropertyMode myLam33Mode = PropertyMode.Inherited;
   PropertyMode myLam12Mode = PropertyMode.Inherited;
   PropertyMode myLam23Mode = PropertyMode.Inherited;
   PropertyMode myLam31Mode = PropertyMode.Inherited;
   PropertyMode myCMode  = PropertyMode.Inherited;

   ScalarFieldComponent myMu1Field = null;
   ScalarFieldComponent myMu2Field = null;
   ScalarFieldComponent myMu3Field = null;
   ScalarFieldComponent myLam11Field = null;
   ScalarFieldComponent myLam22Field = null;
   ScalarFieldComponent myLam33Field = null;
   ScalarFieldComponent myLam12Field = null;
   ScalarFieldComponent myLam23Field = null;
   ScalarFieldComponent myLam31Field = null;
   ScalarFieldComponent myCField  = null;

   static {
      myProps.addInheritableWithField (
         "mu1:Inherited", "mu1", DEFAULT_MU1, "[0,inf]");
      myProps.addInheritableWithField (
         "mu2:Inherited", "mu2", DEFAULT_MU2, "[0,inf]");
      myProps.addInheritableWithField (
         "mu3:Inherited", "mu3", DEFAULT_MU3, "[0,inf]");
      myProps.addInheritableWithField (
         "lam11:Inherited", "lam11", DEFAULT_LAM11, "[0,inf]");
      myProps.addInheritableWithField (
         "lam22:Inherited", "lam22", DEFAULT_LAM22, "[0,inf]");
      myProps.addInheritableWithField (
         "lam33:Inherited", "lam33", DEFAULT_LAM33, "[0,inf]");
      myProps.addInheritableWithField (
         "lam12:Inherited", "lam12", DEFAULT_LAM12, "[0,1]");
      myProps.addInheritableWithField (
         "lam23:Inherited", "lam23", DEFAULT_LAM23, "[0,1]");
      myProps.addInheritableWithField (
         "lam31:Inherited", "lam31", DEFAULT_LAM31, "[0,1]");
      myProps.addInheritableWithField (
         "C:Inherited", "C", DEFAULT_C, "[0,inf]");
   }

   public FieldPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FungOrthotropicMaterial () {
   }

   public FungOrthotropicMaterial (
      double mu1, double mu2, double mu3, double l11, double l22, 
      double l33, double l12, double l23, double l31, double C, double kappa) {
      this();
      setMu1 (mu1);
      setMu2 (mu2);
      setMu3 (mu3);
      setLam11 (l11);
      setLam22 (l22);
      setLam33 (l33);
      setLam12 (l12);
      setLam23 (l23);
      setLam31 (l31);
      setC (C);
      setBulkModulus (kappa);
   }

   public synchronized void setMu1 (double mu1) {
      myMu1 = mu1;
      myMu1Mode =
         PropertyUtils.propagateValue (this, "mu1", myMu1, myMu1Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu2 (double mu2) {
      myMu2 = mu2;
      myMu2Mode =
         PropertyUtils.propagateValue (this, "mu2", myMu2, myMu2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu3 (double mu3) {
      myMu3 = mu3;
      myMu3Mode =
         PropertyUtils.propagateValue (this, "mu3", myMu3, myMu3Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setLam11 (double L11) {
      myLam11 = L11;
      myLam11Mode =
         PropertyUtils.propagateValue (this, "lam11", myLam11, myLam11Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setLam22 (double L22) {
      myLam22 = L22;
      myLam22Mode =
         PropertyUtils.propagateValue (this, "lam22", myLam22, myLam22Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setLam33 (double L33) {
      myLam33 = L33;
      myLam33Mode =
         PropertyUtils.propagateValue (this, "lam33", myLam33, myLam33Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setLam12 (double L12) {
      myLam12 = L12;
      myLam12Mode =
         PropertyUtils.propagateValue (this, "lam12", myLam12, myLam12Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setLam23 (double L23) {
      myLam23 = L23;
      myLam23Mode =
         PropertyUtils.propagateValue (this, "lam23", myLam23, myLam23Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setLam31 (double L31) {
      myLam31 = L31;
      myLam31Mode =
         PropertyUtils.propagateValue (this, "lam31", myLam31, myLam31Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setC (double C) {
      myC = C;
      myCMode =
         PropertyUtils.propagateValue (this, "C", myC, myCMode);
      notifyHostOfPropertyChange();
   }

   public double getMu1() {
      return myMu1;
   }

   public double getMu2() {
      return myMu2;
   }

   public double getMu3() {
      return myMu3;
   }

   public double getLam11() {
      return myLam11;
   }

   public double getLam22() {
      return myLam22;
   }

   public double getLam33() {
      return myLam33;
   }

   public double getLam12() {
      return myLam12;
   }

   public double getLam23() {
      return myLam23;
   }

   public double getLam31() {
      return myLam31;
   }

   public double getC() {
      return myC;
   }

   public void setMu1Mode (PropertyMode mode) {
      myMu1Mode =
         PropertyUtils.setModeAndUpdate (this, "mu1", myMu1Mode, mode);
   }

   public void setMu2Mode (PropertyMode mode) {
      myMu2Mode =
         PropertyUtils.setModeAndUpdate (this, "mu2", myMu2Mode, mode);
   }

   public void setMu3Mode (PropertyMode mode) {
      myMu3Mode =
         PropertyUtils.setModeAndUpdate (this, "mu3", myMu3Mode, mode);
   }

   public void setLam11Mode (PropertyMode mode) {
      myLam11Mode =
         PropertyUtils.setModeAndUpdate (this, "lam11", myLam11Mode, mode);
   }

   public void setLam22Mode (PropertyMode mode) {
      myLam22Mode =
         PropertyUtils.setModeAndUpdate (this, "lam22", myLam22Mode, mode);
   }

   public void setLam33Mode (PropertyMode mode) {
      myLam33Mode =
         PropertyUtils.setModeAndUpdate (this, "lam33", myLam33Mode, mode);
   }

   public void setLam12Mode (PropertyMode mode) {
      myLam12Mode =
         PropertyUtils.setModeAndUpdate (this, "lam12", myLam12Mode, mode);
   }

   public void setLam23Mode (PropertyMode mode) {
      myLam23Mode =
         PropertyUtils.setModeAndUpdate (this, "lam23", myLam23Mode, mode);
   }

   public void setLam31Mode (PropertyMode mode) {
      myLam31Mode =
         PropertyUtils.setModeAndUpdate (this, "lam31", myLam31Mode, mode);
   }
   public void setCMode (PropertyMode mode) {
      myCMode =
         PropertyUtils.setModeAndUpdate (this, "C", myCMode, mode);
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

   public PropertyMode getLam11Mode() {
      return myLam11Mode;
   }

   public PropertyMode getLam22Mode() {
      return myLam22Mode;
   }

   public PropertyMode getLam33Mode() {
      return myLam33Mode;
   }

   public PropertyMode getLam12Mode() {
      return myLam12Mode;
   }

   public PropertyMode getLam23Mode() {
      return myLam23Mode;
   }

   public PropertyMode getLam31Mode() {
      return myLam31Mode;
   }

   public PropertyMode getCMode() {
      return myCMode;
   }

   public double getMu1 (FemFieldPoint dp) {
      if (myMu1Field == null) {
         return getMu1();
      }
      else {
         return myMu1Field.getValue (dp);
      }
   }

   public double getMu2 (FemFieldPoint dp) {
      if (myMu2Field == null) {
         return getMu2();
      }
      else {
         return myMu2Field.getValue (dp);
      }
   }

   public double getMu3 (FemFieldPoint dp) {
      if (myMu3Field == null) {
         return getMu3();
      }
      else {
         return myMu3Field.getValue (dp);
      }
   }

   public double getLam11 (FemFieldPoint dp) {
      if (myLam11Field == null) {
         return getLam11();
      }
      else {
         return myLam11Field.getValue (dp);
      }
   }

   public double getLam22 (FemFieldPoint dp) {
      if (myLam22Field == null) {
         return getLam22();
      }
      else {
         return myLam22Field.getValue (dp);
      }
   }

   public double getLam33 (FemFieldPoint dp) {
      if (myLam33Field == null) {
         return getLam33();
      }
      else {
         return myLam33Field.getValue (dp);
      }
   }

   public double getLam12 (FemFieldPoint dp) {
      if (myLam12Field == null) {
         return getLam12();
      }
      else {
         return myLam12Field.getValue (dp);
      }
   }

   public double getLam23 (FemFieldPoint dp) {
      if (myLam23Field == null) {
         return getLam23();
      }
      else {
         return myLam23Field.getValue (dp);
      }
   }

   public double getLam31 (FemFieldPoint dp) {
      if (myLam31Field == null) {
         return getLam31();
      }
      else {
         return myLam31Field.getValue (dp);
      }
   }

   public double getC (FemFieldPoint dp) {
      if (myCField == null) {
         return getC();
      }
      else {
         return myCField.getValue (dp);
      }
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
      
   public ScalarFieldComponent getLam11Field() {
      return myLam11Field;
   }
      
   public ScalarFieldComponent getLam22Field() {
      return myLam22Field;
   }
      
   public ScalarFieldComponent getLam33Field() {
      return myLam33Field;
   }
      
   public ScalarFieldComponent getLam12Field() {
      return myLam12Field;
   }
      
   public ScalarFieldComponent getLam23Field() {
      return myLam23Field;
   }
      
   public ScalarFieldComponent getLam31Field() {
      return myLam31Field;
   }
      
   public ScalarFieldComponent getCField() {
      return myCField;
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
   
   public void setLam11Field (ScalarFieldComponent func) {
      myLam11Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setLam22Field (ScalarFieldComponent func) {
      myLam22Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setLam33Field (ScalarFieldComponent func) {
      myLam33Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setLam12Field (ScalarFieldComponent func) {
      myLam12Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setLam23Field (ScalarFieldComponent func) {
      myLam23Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setLam31Field (ScalarFieldComponent func) {
      myLam31Field = func;
      notifyHostOfPropertyChange();
   }
   
   public void setCField (ScalarFieldComponent func) {
      myCField = func;
      notifyHostOfPropertyChange();
   }

   public void computeDevStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {   

      sigma.setZero();

      double[] K = new double[3];
      double[] L = new double[3];
      
      Vector3d[] a0 = {new Vector3d(), new Vector3d(), new Vector3d()};
      Vector3d[] a  = {new Vector3d(), new Vector3d(), new Vector3d()};
      
      Vector3d vtmp = new Vector3d();
      
      // Matrix3d Q = Matrix3d.IDENTITY;
      // if (dt.getFrame() != null) {
      //   Q = dt.getFrame(); 
      // }
      SymmetricMatrix3d[] A  = {
         new SymmetricMatrix3d(),
         new SymmetricMatrix3d(),
         new SymmetricMatrix3d()};
      
      Matrix3d tmpMatrix  = new Matrix3d();
      Matrix3d tmpMatrix2 = new Matrix3d();
      SymmetricMatrix3d tmpSymmMatrix  = new SymmetricMatrix3d();

      // sigma value before applying deviator; used to compute D
      SymmetricMatrix3d sigtmp = null;

      // Evaluate Lame coefficients
      mu[0] = getMu1(def);
      mu[1] = getMu2(def);
      mu[2] = getMu3(def);
      
      lam[0][0] = getLam11(def);
      lam[0][1] = getLam12(def);
      lam[0][2] = getLam31(def);
      lam[1][0] = lam[0][1];
      lam[1][1] = getLam22(def); 
      lam[1][2] = getLam23(def);
      lam[2][0] = lam[0][2];
      lam[2][1] = lam[1][2];
      lam[2][2] = getLam33(def);

      double c = getC(def);
      
      double J = def.getDetF();

      SymmetricMatrix3d C = new SymmetricMatrix3d();
      SymmetricMatrix3d C2 = new SymmetricMatrix3d();
      SymmetricMatrix3d B = new SymmetricMatrix3d();
      // Calculate deviatoric left Cauchy-Green tensor
      computeDevLeftCauchyGreen(B,def);

      // Calculate deviatoric right Cauchy-Green tensor
      computeDevRightCauchyGreen(C,def);

      // calculate square of C
      C2.mulTransposeLeft (C);

      Matrix3d mydevF = new Matrix3d(def.getF());
      mydevF.scale(Math.pow(J,-1.0 / 3.0));

      for (int i=0; i<3; i++) {
         // Copy the texture direction in the reference configuration to a0
         a0[i].x = Q.get(0,i);
         a0[i].y = Q.get(1,i);
         a0[i].z = Q.get(2,i);

         vtmp.mul(C,a0[i]);
         K[i] = a0[i].dot(vtmp);

         vtmp.mul(C2,a0[i]);
         L[i] = a0[i].dot(vtmp);

         a[i].mul(mydevF,a0[i]);
         a[i].scale(Math.pow(K[i], -0.5));

         A[i].set(a[i].x*a[i].x, a[i].y*a[i].y, a[i].z*a[i].z, 
            a[i].x*a[i].y, a[i].x*a[i].z, a[i].y*a[i].z);

      }

      // Evaluate exp(Q)
      double eQ = 0.0;
      for (int i=0; i<3; i++) {
         eQ += 2.0*mu[i]*(L[i]-2.0*K[i]+1.0);
         for (int j=0; j<3; j++)
            eQ += lam[i][j]*(K[i]-1.0)*(K[j]-1.0);
      }
      eQ = Math.exp(eQ/(4.*c));

      // Evaluate the stress
      SymmetricMatrix3d bmi = new SymmetricMatrix3d(); 
      bmi.sub(B,SymmetricMatrix3d.IDENTITY);
      for (int i=0; i<3; i++) {
         //       s += mu[i]*K[i]*(A[i]*bmi + bmi*A[i]);
         tmpMatrix.mul(A[i], bmi);
         tmpMatrix2.mul(bmi,A[i]);
         tmpMatrix.add(tmpMatrix2);
         tmpMatrix.scale(mu[i]*K[i]);
         tmpSymmMatrix.setSymmetric(tmpMatrix);

         sigma.add(tmpSymmMatrix);

         for (int j=0; j<3; j++) {
            // s += lam[i][j]*((K[i]-1)*K[j]*A[j]+(K[j]-1)*K[i]*A[i])/2.;
            sigma.scaledAdd(lam[i][j]/2.0*(K[i]-1.0)*K[j], A[j]);
            sigma.scaledAdd(lam[i][j]/2.0*(K[j]-1.0)*K[i], A[i]);
         }
      }
      sigma.scale(eQ / (2.0 * J));

      // do we need this?
      sigma.m10 = sigma.m01;
      sigma.m20 = sigma.m02;
      sigma.m21 = sigma.m12;

      if (D != null) {
         // save sigma before deviator is applied
         sigtmp = new SymmetricMatrix3d (sigma);
      }

      sigma.deviator();

      if (D != null) {

         D.setZero();
      
         for (int i=0; i<3; i++) {
            addTensorProduct4(D, mu[i]*K[i], A[i], B);
            for (int j=0; j<3; j++) {
               TensorUtils.addSymmetricTensorProduct (
                  D, lam[i][j]*K[i]*K[j]/2.0, A[i], A[j]);
            }
         }
         D.scale(eQ / J);
         TensorUtils.addTensorProduct (D, 2*J/c/eQ, sigtmp);

         double traceD = TensorUtils.symmetricTrace (D);
         TensorUtils.addSymmetricIdentityDot (D, -1.0/3, D);
         TensorUtils.addScaledIdentityProduct (D, traceD/9.0);

         TensorUtils.addScaledIdentity (D, 2*sigtmp.trace()/3);
         TensorUtils.addScaledIdentityProduct (D, -2.0*sigtmp.trace()/9);
         TensorUtils.addSymmetricIdentityProduct (D, -2/3.0, sigma);

         D.setLowerToUpper();
      }
   }

   public double computeDevStrainEnergy (
      DeformedPoint def, Matrix3d Q, double excitation, 
      MaterialStateObject state) {

      double[] K = new double[3];
      double[] L = new double[3];

      // break out Lame coefficients
      mu[0] = getMu1(def);
      mu[1] = getMu2(def);
      mu[2] = getMu3(def);
      
      lam[0][0] = getLam11(def);
      lam[0][1] = getLam12(def);
      lam[0][2] = getLam31(def);
      lam[1][0] = lam[0][1];
      lam[1][1] = getLam22(def); 
      lam[1][2] = getLam23(def);
      lam[2][0] = lam[0][2];
      lam[2][1] = lam[1][2];
      lam[2][2] = getLam33(def);

      double c = getC(def);

      SymmetricMatrix3d C = new SymmetricMatrix3d();
      SymmetricMatrix3d C2 = new SymmetricMatrix3d();
      // Calculate deviatoric right Cauchy-Green tensor
      computeDevRightCauchyGreen(C,def);

      // calculate square of C
      C2.mulTransposeLeft (C);

      Vector3d a = new Vector3d();
      Vector3d vtmp = new Vector3d();
      for (int i=0; i<3; i++) {
         // Copy the texture direction in the reference configuration to a0
         a.x = Q.get(0,i);
         a.y = Q.get(1,i);
         a.z = Q.get(2,i);

         vtmp.mul(C,a);
         K[i] = a.dot(vtmp);

         vtmp.mul(C2,a);
         L[i] = a.dot(vtmp);
      }

      // Evaluate exp(Q)
      double eQ = 0.0;
      for (int i=0; i<3; i++) {
         eQ += 2.0*mu[i]*(L[i]-2.0*K[i]+1.0);
         for (int j=0; j<3; j++)
            eQ += lam[i][j]*(K[i]-1.0)*(K[j]-1.0);
      }
      eQ = Math.exp(eQ/(4.*c));

      return c*(eQ-1)/2;
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof FungOrthotropicMaterial)) {
         return false;
      }
      FungOrthotropicMaterial fung = (FungOrthotropicMaterial)mat;
      if (myMu1 != fung.myMu1 ||
         myMu2  != fung.myMu2 ||
         myMu3  != fung.myMu3 ||
         myLam11 != fung.myLam11 ||
         myLam22 != fung.myLam22 ||
         myLam33 != fung.myLam33 ||
         myLam12 != fung.myLam12 ||
         myLam23 != fung.myLam23 ||
         myLam31 != fung.myLam31 ||
         myC != fung.myC) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public FungOrthotropicMaterial clone() {
      FungOrthotropicMaterial mat = (FungOrthotropicMaterial)super.clone();
      return mat;
   }

   public static void main (String[] args) {
      FungOrthotropicMaterial mat = new FungOrthotropicMaterial();

      RotationMatrix3d R = new RotationMatrix3d();
      R.setRpy (1, 2, 3);
      Matrix3d Q = new Matrix3d();
      Q.set (R);
      
      DeformedPointBase dpnt = new DeformedPointBase();
      dpnt.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));
   
      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      // mat.computeStress (sig, pt, dt, null);
      // System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      // mat.computeTangent (D, sig, pt, dt, null);
      // System.out.println ("D=\n" + D.toString ("%12.6f"));

      mat.computeStressAndTangent (sig, D, dpnt, Q, 1.0, null);
      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      System.out.println ("D=\n" + D.toString ("%12.6f"));
   }

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myMu1  /= s;
      myMu2  /= s;
      myMu3  /= s;
      myLam11 /= s;
      myLam22 /= s;
      myLam33 /= s;
      myLam12 /= s;
      myLam23 /= s;
      myLam31 /= s;
      myC  /= s;
   }

   @Override
   public void scaleMass (double s) {
      super.scaleMass (s);
      myMu1  *= s;
      myMu2  *= s;
      myMu3  *= s;
      myLam11 *= s;
      myLam22 *= s;
      myLam33 *= s;
      myLam12 *= s;
      myLam23 *= s;
      myLam31 *= s;
      myC  *= s;
   }

   public static Matrix6d ddots (Matrix6d a, Matrix6d b) {
      Matrix6d c = new Matrix6d();

      c.m00 = 2*(a.m00*b.m00 + a.m01*b.m01 + a.m02*b.m02 + 2*a.m03*b.m03 
         + 2*a.m04*b.m04 + 2*a.m05*b.m05);

      c.m01 = a.m00*b.m01 + a.m11*b.m01 + a.m01*(b.m00 + b.m11) + a.m12*b.m02 
      + a.m02*b.m12 + 2*a.m13*b.m03 + 2*a.m03*b.m13 + 2*a.m14*b.m04 
      + 2*a.m04*b.m14 + 2*a.m15*b.m05 + 2*a.m05*b.m15;

      c.m02 = a.m12*b.m01 + a.m00*b.m02 + a.m22*b.m02 + a.m01*b.m12 
      + a.m02*(b.m00 + b.m22) + 2*a.m23*b.m03 + 2*a.m03*b.m23 + 2*a.m24*b.m04 
      + 2*a.m04*b.m24 + 2*a.m25*b.m05 + 2*a.m05*b.m25;

      c.m03 = a.m13*b.m01 + a.m23*b.m02 + a.m00*b.m03 + 2*a.m33*b.m03 
      + a.m01*b.m13 + a.m02*b.m23 + a.m03*(b.m00 + 2*b.m33) 
      + 2*a.m34*b.m04 + 2*a.m04*b.m34 + 2*a.m35*b.m05 + 2*a.m05*b.m35;

      c.m04 = a.m14*b.m01 + a.m24*b.m02 + 2*a.m34*b.m03 + a.m00*b.m04 
      + 2*a.m44*b.m04 + a.m01*b.m14 + a.m02*b.m24 + 2*a.m03*b.m34 
      + a.m04*(b.m00 + 2*b.m44) + 2*a.m45*b.m05 + 2*a.m05*b.m45;

      c.m05 = a.m15*b.m01 + a.m25*b.m02 + 2*a.m35*b.m03 + 2*a.m45*b.m04 
      + a.m00*b.m05 + 2*a.m55*b.m05 + a.m01*b.m15 + a.m02*b.m25 
      + 2*a.m03*b.m35 + 2*a.m04*b.m45 + a.m05*(b.m00 + 2*b.m55);

      c.m11 = 2*(a.m01*b.m01 + a.m11*b.m11 + a.m12*b.m12 + 2*a.m13*b.m13 
         + 2*a.m14*b.m14 + 2*a.m15*b.m15);

      c.m12 = a.m02*b.m01 + a.m01*b.m02 + a.m11*b.m12 + a.m22*b.m12 
      + a.m12*(b.m11 + b.m22) + 2*a.m23*b.m13 + 2*a.m13*b.m23 + 2*a.m24*b.m14 
      + 2*a.m14*b.m24 + 2*a.m25*b.m15 + 2*a.m15*b.m25;

      c.m13 = a.m03*b.m01 + a.m23*b.m12 + a.m01*b.m03 + a.m11*b.m13 
      + 2*a.m33*b.m13 + a.m12*b.m23 + a.m13*(b.m11 + 2*b.m33) + 2*a.m34*b.m14 
      + 2*a.m14*b.m34 + 2*a.m35*b.m15 + 2*a.m15*b.m35;

      c.m14 = a.m04*b.m01 + a.m24*b.m12 + 2*a.m34*b.m13 + a.m01*b.m04 
      + a.m11*b.m14 + 2*a.m44*b.m14 + a.m12*b.m24 + 2*a.m13*b.m34 
      + a.m14*(b.m11 + 2*b.m44) + 2*a.m45*b.m15 + 2*a.m15*b.m45;

      c.m15 = a.m05*b.m01 + a.m25*b.m12 + 2*a.m35*b.m13 + 2*a.m45*b.m14 
      + a.m01*b.m05 + a.m11*b.m15 + 2*a.m55*b.m15 + a.m12*b.m25 
      + 2*a.m13*b.m35 + 2*a.m14*b.m45 + a.m15*(b.m11 + 2*b.m55);

      c.m22 = 2*(a.m02*b.m02 + a.m12*b.m12 + a.m22*b.m22 + 2*a.m23*b.m23 
         + 2*a.m24*b.m24 + 2*a.m25*b.m25);

      c.m23 = a.m03*b.m02 + a.m13*b.m12 + a.m23*b.m22 + a.m02*b.m03 
      + a.m12*b.m13 + a.m22*b.m23 + 2*a.m33*b.m23 + 2*a.m23*b.m33 
      + 2*a.m34*b.m24 + 2*a.m24*b.m34 + 2*a.m35*b.m25 + 2*a.m25*b.m35;

      c.m24 = a.m04*b.m02 + a.m14*b.m12 + a.m24*b.m22 + 2*a.m34*b.m23 
      + a.m02*b.m04 + a.m12*b.m14 + a.m22*b.m24 + 2*a.m44*b.m24 + 2*a.m23*b.m34 
      + 2*a.m24*b.m44 + 2*a.m45*b.m25 + 2*a.m25*b.m45;

      c.m25 = a.m05*b.m02 + a.m15*b.m12 + a.m25*b.m22 + 2*a.m35*b.m23 
      + 2*a.m45*b.m24 + a.m02*b.m05 + a.m12*b.m15 + a.m22*b.m25 
      + 2*a.m55*b.m25 + 2*a.m23*b.m35 + 2*a.m24*b.m45 + 2*a.m25*b.m55;

      c.m33 = 2*(a.m03*b.m03 + a.m13*b.m13 + a.m23*b.m23 + 2*a.m33*b.m33 
         + 2*a.m34*b.m34 + 2*a.m35*b.m35);

      c.m34 = a.m04*b.m03 + a.m14*b.m13 + a.m24*b.m23 + 2*a.m34*b.m33 
      + a.m03*b.m04 + a.m13*b.m14 + a.m23*b.m24 + 2*a.m33*b.m34 
      + 2*a.m44*b.m34 + 2*a.m34*b.m44 + 2*a.m45*b.m35 + 2*a.m35*b.m45;

      c.m35 = a.m05*b.m03 + a.m15*b.m13 + a.m25*b.m23 + 2*a.m35*b.m33 
      + 2*a.m45*b.m34 + a.m03*b.m05 + a.m13*b.m15 + a.m23*b.m25 
      + 2*a.m33*b.m35 + 2*a.m55*b.m35 + 2*a.m34*b.m45 + 2*a.m35*b.m55;

      c.m44 = 2*(a.m04*b.m04 + a.m14*b.m14 + a.m24*b.m24 
         + 2*a.m34*b.m34 + 2*a.m44*b.m44 + 2*a.m45*b.m45);

      c.m45 = a.m05*b.m04 + a.m15*b.m14 + a.m25*b.m24 
      + 2*a.m35*b.m34 + 2*a.m45*b.m44 + a.m04*b.m05 + a.m14*b.m15 
      + a.m24*b.m25 + 2*a.m34*b.m35 + 2*a.m44*b.m45 + 2*a.m55*b.m45 
      + 2*a.m45*b.m55;

      c.m55 = 2*(a.m05*b.m05 + a.m15*b.m15 + a.m25*b.m25 
         + 2*a.m35*b.m35 + 2*a.m45*b.m45 + 2*a.m55*b.m55);

      return c;
   }

   //-----------------------------------------------------------------------------
   // (a dyad4s b)_ijkl = s (a_ik b_jl + a_il b_jk)/2 +  s (b_ik a_jl + b_il a_jk)/2
   public static void addTensorProduct4(Matrix6d c, double s, Matrix3dBase A, Matrix3dBase B)
   {

      double a00 = s*A.m00;
      double a11 = s*A.m11;
      double a22 = s*A.m22;
      double a01 = s*A.m01;
      double a02 = s*A.m02;
      double a12 = s*A.m12;

      double b00 = B.m00;
      double b11 = B.m11;
      double b22 = B.m22;
      double b01 = B.m01;
      double b02 = B.m02;
      double b12 = B.m12;

      c.m00 += 2.0*a00*b00;
      c.m01 += 2.0*a01*b01;
      c.m02 += 2.0*a02*b02;
      c.m03 += a01*b00 + a00*b01;
      c.m04 += a02*b01 + a01*b02;
      c.m05 += a02*b00 + a00*b02;

      c.m11 += 2.0*a11*b11;
      c.m12 += 2.0*a12*b12;
      c.m13 += a11*b01 + a01*b11;
      c.m14 += a12*b11 + a11*b12;
      c.m15 += a12*b01 + a01*b12;

      c.m22 += 2.0*a22*b22;
      c.m23 += a12*b02 + a02*b12;
      c.m24 += a22*b12 + a12*b22;
      c.m25 += a22*b02 + a02*b22;

      c.m33 += 0.5*(a11*b00 + 2.0*a01*b01 + a00*b11);
      c.m34 += 0.5*(a12*b01 + a11*b02 + a02*b11 + a01*b12);
      c.m35 += 0.5*(a12*b00 + a02*b01 + a01*b02 + a00*b12);

      c.m44 += 0.5*(a22*b11 + 2.0*a12*b12 + a11*b22);
      c.m45 += 0.5*(a22*b01 + a12*b02 + a02*b12 + a01*b22);

      c.m55 += 0.5*(a22*b00 + 2.0*a02*b02 + a00*b22);

   }
}
