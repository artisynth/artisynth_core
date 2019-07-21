package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class IncompNeoHookeanMaterial extends IncompressibleMaterialBase {

   public static FunctionPropertyList myProps =
      new FunctionPropertyList (IncompNeoHookeanMaterial.class,
                        IncompressibleMaterialBase.class);

   protected static double DEFAULT_G = 150000;

   private double myG = DEFAULT_G;
   PropertyMode myGMode = PropertyMode.Inherited;
   ScalarFieldPointFunction myGFunction = null;

   private SymmetricMatrix3d myB;

   static {
      myProps.addInheritableWithFunction (
         "shearModulus:Inherited", "shear modulus", DEFAULT_G);
   }

   public FunctionPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public IncompNeoHookeanMaterial (){
      myB = new SymmetricMatrix3d();
   }

   public IncompNeoHookeanMaterial (double E, double kappa) {
      myB = new SymmetricMatrix3d();
      setShearModulus (E);
      setBulkModulus (kappa);
   }

   public synchronized void setShearModulus (double G) {
      myG = G;
      myGMode =
         PropertyUtils.propagateValue (this, "shearModulus", myG, myGMode);
      notifyHostOfPropertyChange();
   }

   public double getShearModulus() {
      return myG;
   }

   public void setShearModulusMode (PropertyMode mode) {
      myGMode =
         PropertyUtils.setModeAndUpdate (this, "shearModulus", myGMode, mode);
   }

   public PropertyMode getShearModulusMode() {
      return myGMode;
   }

   public double getShearModulus (FieldPoint dp) {
      if (myGFunction == null) {
         return getShearModulus();
      }
      else {
         return myGFunction.eval (dp);
      }
   }

   public ScalarFieldPointFunction getShearModulusFunction() {
      return myGFunction;
   }
      
   public void setShearModulusFunction (ScalarFieldPointFunction func) {
      myGFunction = func;
      notifyHostOfPropertyChange();
   }
   
   public void setShearModulusField (
      ScalarField field, boolean useRestPos) {
      myGFunction = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getShearModulusField () {
      return FieldUtils.getFieldFromFunction (myGFunction);
   }

   public void computeDevStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {
      
      double J = def.getDetF();

      computeLeftCauchyGreen (myB,def);

      double G = getShearModulus (def);
      double muJ = G/Math.pow(J, 5.0/3.0);
      double diagTerm = -muJ*(myB.m00 + myB.m11 + myB.m22)/3.0;

      sigma.scale (muJ, myB);
      sigma.m00 += diagTerm;
      sigma.m11 += diagTerm;
      sigma.m22 += diagTerm;

      if (D != null) {
         double Ib = myB.m00+myB.m11+myB.m22;
         D.setZero();
         TensorUtils.addScaledIdentityProduct (D, 2/9.0*muJ*Ib);
         TensorUtils.addScaledIdentity (D, 2/3.0*muJ*Ib);
         TensorUtils.addSymmetricTensorProduct (
            D, -2/3.0*muJ, myB, SymmetricMatrix3d.IDENTITY);
         D.setLowerToUpper();         
      }
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof IncompNeoHookeanMaterial)) {
         return false;
      }
      IncompNeoHookeanMaterial stvk = (IncompNeoHookeanMaterial)mat;
      if (myG != stvk.myG) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public IncompNeoHookeanMaterial clone() {
      IncompNeoHookeanMaterial mat = (IncompNeoHookeanMaterial)super.clone();
      mat.myB = new SymmetricMatrix3d();
      return mat;
   }

   public static void main (String[] args) {
      IncompNeoHookeanMaterial mat = new IncompNeoHookeanMaterial();

      Matrix3d Q = new Matrix3d();
      
      DeformedPointBase dpnt = new DeformedPointBase();
      dpnt.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));
     
      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      mat.setShearModulus (10);      
      mat.computeStressAndTangent (sig, D, dpnt, Q, 1.0, null);

      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      System.out.println ("D=\n" + D.toString ("%12.6f"));

   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         setShearModulus (myG/s);
      }
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         setShearModulus (myG*s);
      }
   }
}
