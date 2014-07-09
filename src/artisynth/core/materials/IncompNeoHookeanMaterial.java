package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class IncompNeoHookeanMaterial extends IncompressibleMaterial {

   public static PropertyList myProps =
      new PropertyList (IncompNeoHookeanMaterial.class,
                        IncompressibleMaterial.class);

   protected static double DEFAULT_G = 150000;

   private double myG = DEFAULT_G;

   PropertyMode myGMode = PropertyMode.Inherited;

   private SymmetricMatrix3d myB;

   static {
      myProps.addInheritable (
         "shearModulus:Inherited", "shear modulus", DEFAULT_G);
   }

   public PropertyList getAllPropertyInfo() {
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

   public synchronized void setShearModulus (double E) {
      myG = E;
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

   public void computeStress (
      SymmetricMatrix3d sigma, SolidDeformation def, Matrix3d Q,
      FemMaterial baseMat) {

      double J = def.getDetF();
      double p = def.getAveragePressure();

      def.computeLeftCauchyGreen (myB);

      double muJ = myG/Math.pow(J, 5.0/3.0);
      double diagTerm = -muJ*(myB.m00 + myB.m11 + myB.m22)/3.0 + p;

      sigma.scale (muJ, myB);
      sigma.m00 += diagTerm;
      sigma.m11 += diagTerm;
      sigma.m22 += diagTerm;
   }

   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def, 
      Matrix3d Q, FemMaterial baseMat) {

      double J = def.getDetF();

      def.computeLeftCauchyGreen (myB);

      double Ib = myB.m00+myB.m11+myB.m22;
      double muJ = myG/Math.pow(J, 5.0/3.0);
      double p = def.getAveragePressure();

      D.setZero();

      TensorUtils.addScaledIdentityProduct (D, p+ 2/9.0*muJ*Ib);
      TensorUtils.addScaledIdentity (D, -2*p + 2/3.0*muJ*Ib);
      TensorUtils.addSymmetricTensorProduct (
         D, -2/3.0*muJ, myB, SymmetricMatrix3d.IDENTITY);
      D.setLowerToUpper();
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

      SolidDeformation def = new SolidDeformation();
      Matrix3d Q = new Matrix3d();
      def.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      mat.setShearModulus (10);      
      mat.computeStress (sig, def, Q, null);
      mat.computeTangent (D, sig, def, Q, null);

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
