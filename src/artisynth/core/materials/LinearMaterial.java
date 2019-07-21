package artisynth.core.materials;

import artisynth.core.modelbase.*;
import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class LinearMaterial extends LinearMaterialBase {

   public static FunctionPropertyList myProps =
      new FunctionPropertyList (LinearMaterial.class, LinearMaterialBase.class);

   protected static double DEFAULT_NU = 0.33;
   protected static double DEFAULT_E = 500000;

   private double myNu = DEFAULT_NU;
   private double myE = DEFAULT_E;
   private ScalarFieldPointFunction myEFunc;

   PropertyMode myNuMode = PropertyMode.Inherited;
   PropertyMode myEMode = PropertyMode.Inherited;

   static {
      myProps.addInheritableWithFunction (
         "YoungsModulus:Inherited", "Youngs modulus", DEFAULT_E, "[0,inf]");
      myProps.addInheritable (
         "PoissonsRatio:Inherited", "Poissons ratio", DEFAULT_NU, "[-1,0.5]");
   }

   public FunctionPropertyList getAllPropertyInfo() {
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

   public double getYoungsModulus (FieldPoint dp) {
      return (myEFunc == null ? getYoungsModulus() : myEFunc.eval (dp));
   }

   public ScalarFieldPointFunction getYoungsModulusFunction() {
      return myEFunc;
   }
      
   public void setYoungsModulusFunction (ScalarFieldPointFunction func) {
      myEFunc = func;
      notifyHostOfPropertyChange();
   }
   
   public void setYoungsModulusField (
      ScalarField field, boolean useRestPos) {
      myEFunc = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getYoungsModulusField () {
      return FieldUtils.getFieldFromFunction (myEFunc);
   }

   @Override
   protected void multiplyC(
      SymmetricMatrix3d sigma, SymmetricMatrix3d eps, DeformedPoint defp) {
    
      double E = getYoungsModulus(defp);
      // lam and mu are the first and second Lame parameters
      double lam = E*myNu/((1+myNu)*(1-2*myNu));
      double mu = E/(2*(1+myNu));

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
   protected void getC (Matrix6d C, DeformedPoint defp) {

      double E = getYoungsModulus(defp);
      double a = E / (1+ myNu);  // 2 mu
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
