package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class LinearMaterial extends LinearMaterialBase {

   public static PropertyList myProps =
      new PropertyList (LinearMaterial.class, LinearMaterialBase.class);

   protected static double DEFAULT_NU = 0.33;
   protected static double DEFAULT_E = 500000;

   private double myNu = DEFAULT_NU;
   private double myE = DEFAULT_E;

   PropertyMode myNuMode = PropertyMode.Inherited;
   PropertyMode myEMode = PropertyMode.Inherited;

   static {
      myProps.addInheritable (
         "YoungsModulus:Inherited", "Youngs modulus", DEFAULT_E, "[0,inf]");
      myProps.addInheritable (
         "PoissonsRatio:Inherited", "Poissons ratio", DEFAULT_NU, "[-1,0.5]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public LinearMaterial (){
      this(DEFAULT_E, DEFAULT_NU, DEFAULT_COROTATED);
   }

   public LinearMaterial (double E, double nu) {
      this (E, nu, /*corotated=*/true);
   }

   public LinearMaterial (double E, double nu, boolean corotated) {
      setYoungsModulus (E);
      setPoissonsRatio (nu);
      setCorotated (corotated);
   }

   public synchronized void setPoissonsRatio (double nu) {
      myNu = nu;
      myNuMode =
         PropertyUtils.propagateValue (this, "PoissonsRatio", myNu, myNuMode);
      notifyHostOfPropertyChange();
   }

   public double getPoissonsRatio() {
      return myNu;
   }

   public void setPoissonsRatioMode (PropertyMode mode) {
      myNuMode =
         PropertyUtils.setModeAndUpdate (this, "PoissonsRatio", myNuMode, mode);
   }

   public PropertyMode getPoissonsRatioMode() {
      return myNuMode;
   }

   public synchronized void setYoungsModulus (double E) {
      myE = E;
      myEMode =
         PropertyUtils.propagateValue (this, "YoungsModulus", myE, myEMode);
      notifyHostOfPropertyChange();
   }

   public double getYoungsModulus() {
      return myE;
   }

   public void setYoungsModulusMode (PropertyMode mode) {
      myEMode =
         PropertyUtils.setModeAndUpdate (this, "YoungsModulus", myEMode, mode);
   }

   public PropertyMode getYoungsModulusMode() {
      return myEMode;
   }

   /** 
    * @deprecated
    * 
    * Computes the Cauchy stress from Cauchy strain and adds it to and existing
    * stress.
    * 
    * @param sigma value to which stress should be added
    * @param Eps Cauchy stress
    * @param R (optional) Co-Rotation matrix, if any
    */
   public void addStress (
      SymmetricMatrix3d sigma, SymmetricMatrix3d Eps, Matrix3dBase R) {

      // save for the rotated case
      double m00 = sigma.m00;
      double m11 = sigma.m11;
      double m22 = sigma.m22;
      double m01 = sigma.m01;
      double m02 = sigma.m02;
      double m12 = sigma.m12;

      if (R != null) {
         sigma.setZero();
      }

      // lam and mu are the first and second Lame parameters
      double lam = myE*myNu/((1+myNu)*(1-2*myNu));
      double mu = myE/(2*(1+myNu));

      double lamtrEps = lam*Eps.trace();
      sigma.scaledAdd (2*mu, Eps);
      sigma.m00 += lamtrEps;
      sigma.m11 += lamtrEps;
      sigma.m22 += lamtrEps;

      if (R != null) {
         sigma.mulLeftAndTransposeRight (R);

         sigma.m00 += m00;
         sigma.m11 += m11;
         sigma.m22 += m22;

         sigma.m01 += m01;
         sigma.m02 += m02;
         sigma.m12 += m12;

         sigma.m10 += m01;
         sigma.m20 += m02;
         sigma.m21 += m12;
      }
   }
   
   @Override
   protected void multiplyC(SymmetricMatrix3d sigma, SymmetricMatrix3d eps) {
    
      // lam and mu are the first and second Lame parameters
      double lam = myE*myNu/((1+myNu)*(1-2*myNu));
      double mu = myE/(2*(1+myNu));

      // convert sigma from strain to stress
      // sigma = 2*mu*eps + lamda*trace(eps)*I
      double lamtrEps = lam*(eps.m00+eps.m11+eps.m22);
      
      double m00 = 2*mu*eps.m00 + lamtrEps;
      double m11 = 2*mu*eps.m11 + lamtrEps;
      double m22 = 2*mu*eps.m22 + lamtrEps;
      double m01 = 2*mu*eps.m01;
      double m02 = 2*mu*eps.m02;
      double m12 = 2*mu*eps.m12;
      
      sigma.set(m00, m11, m22, m01, m02, m12);
   }
   
   @Override
   protected void getC(Matrix6d C) {
    
      //      // lam and mu are the first and second Lame parameters
      //      double lam = myE*myNu/((1+myNu)*(1-2*myNu));
      //      double mu = myE/(2*(1+myNu));
      //      D.setZero();
      //      D.m00 = lam + 2*mu;
      //      D.m01 = lam;
      //      D.m02 = lam;
      //      D.m10 = lam;
      //      D.m11 = lam + 2*mu;
      //      D.m12 = lam;
      //      D.m20 = lam;
      //      D.m21 = lam;
      //      D.m22 = lam + 2*mu;
      //      D.m33 = mu;
      //      D.m44 = mu;
      //      D.m55 = mu;
      
      double a = myE / (1+ myNu);  // 2 mu
      double dia = (1 - myNu) / (1 - 2 * myNu) * a;
      double mu = 0.5 * a;
      double off = myNu / (1 - 2 * myNu) * a;

      C.m00 = dia; C.m01 = off; C.m02 = off; C.m03 = 0;   C.m04 = 0;   C.m05 = 0;
      C.m10 = off; C.m11 = dia; C.m12 = off; C.m13 = 0;   C.m14 = 0;   C.m15 = 0;
      C.m20 = off; C.m21 = off; C.m22 = dia; C.m23 = 0;   C.m24 = 0;   C.m25 = 0;
      C.m30 = 0;   C.m31 = 0;   C.m32 = 0;   C.m33 = mu;  C.m34 = 0;   C.m35 = 0;
      C.m40 = 0;   C.m41 = 0;   C.m42 = 0;   C.m43 = 0;   C.m44 = mu;  C.m45 = 0;
      C.m50 = 0;   C.m51 = 0;   C.m52 = 0;   C.m53 = 0;   C.m54 = 0;   C.m55 = mu;
   }

   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def, 
      Matrix3d Q, FemMaterial baseMat) {
      
      // XXX linear isotropic materials are invariant under rotation
      getC(D);
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof LinearMaterial)) {
         return false;
      }
      LinearMaterial linm = (LinearMaterial)mat;
      if (myNu != linm.myNu ||
          myE != linm.myE) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public LinearMaterial clone() {
      LinearMaterial mat = (LinearMaterial)super.clone();
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         setYoungsModulus (myE/s);
      }
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         setYoungsModulus (myE*s);
      }
   }
   
}
