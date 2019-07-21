package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class NeoHookeanMaterial extends FemMaterial {

   public static FunctionPropertyList myProps =
      new FunctionPropertyList (NeoHookeanMaterial.class, FemMaterial.class);

   protected static double DEFAULT_NU = 0.33;
   protected static double DEFAULT_E = 500000;

   private double myNu = DEFAULT_NU;
   private double myE = DEFAULT_E;
   private ScalarFieldPointFunction myEFunc;

   PropertyMode myNuMode = PropertyMode.Inherited;
   PropertyMode myEMode = PropertyMode.Inherited;

   private SymmetricMatrix3d myB;
   //private SymmetricMatrix3d myB2;

   static {
      myProps.addInheritableWithFunction (
         "YoungsModulus:Inherited", "Youngs modulus", DEFAULT_E);
      myProps.addInheritable (
         "PoissonsRatio:Inherited", "Poissons ratio", DEFAULT_NU, "[-1,0.5]");
   }

   public FunctionPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public NeoHookeanMaterial (){
      myB = new SymmetricMatrix3d();
      //myB2 = new SymmetricMatrix3d();
   }
   
   public NeoHookeanMaterial(double E, double nu) {
      this();
      setYoungsModulus(E);
      setPoissonsRatio(nu);
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

   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {

      double J = def.getDetF();

      // express constitutive law in terms of Lama parameters
      double E = getYoungsModulus(def);
      double G = E/(2*(1+myNu)); // bulk modulus
      double lam = (E*myNu)/((1-2*myNu)*(1+myNu));
      double mu = G;
      
      computeLeftCauchyGreen (myB,def);

      sigma.scale (mu/J, myB);
      double diagTerm = (lam*Math.log(J)-mu)/J;
      sigma.m00 += diagTerm;
      sigma.m11 += diagTerm;
      sigma.m22 += diagTerm;

      if (D != null) {
         D.setZero();
         TensorUtils.addScaledIdentityProduct (D, lam/J);
         TensorUtils.addScaledIdentity (D, 2*(mu-lam*Math.log(J))/J);
         D.setLowerToUpper();
      }
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof NeoHookeanMaterial)) {
         return false;
      }
      NeoHookeanMaterial stvk = (NeoHookeanMaterial)mat;
      if (myNu != stvk.myNu ||
          myE != stvk.myE) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public NeoHookeanMaterial clone() {
      NeoHookeanMaterial mat = (NeoHookeanMaterial)super.clone();
      mat.myB = new SymmetricMatrix3d();
      //mat.myB2 = new SymmetricMatrix3d();
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

   public static void main (String[] args) {
      NeoHookeanMaterial mat = new NeoHookeanMaterial();

      Matrix3d Q = new Matrix3d();

      DeformedPointBase dpnt = new DeformedPointBase();
      dpnt.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      mat.setYoungsModulus (10);      
      mat.computeStressAndTangent (sig, D, dpnt, Q, 0.0, null);

      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      System.out.println ("D=\n" + D.toString ("%12.6f"));
   }

}
