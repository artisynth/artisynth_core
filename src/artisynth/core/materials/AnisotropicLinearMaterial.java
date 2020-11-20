package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.DenseMatrix;
import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix6dBase;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.VectorNd;

public class AnisotropicLinearMaterial extends LinearMaterialBase {

   private static Matrix6d DEFAULT_STIFFNESS_TENSOR = createIsotropicStiffness(LinearMaterial.DEFAULT_E, 
      LinearMaterial.DEFAULT_NU);
   private static VectorNd DEFAULT_STIFFNESS_TENSOR_VEC = toRowMajor(DEFAULT_STIFFNESS_TENSOR);

   private Matrix6d myC;  // anisotropic stiffness matrix

   public static FunctionPropertyList myProps =
      new FunctionPropertyList (AnisotropicLinearMaterial.class, LinearMaterialBase.class);

   static {
      myProps.add (
         "stiffnessTensor getRasterizedStiffnessTensor setRasterizedStiffnessTensor", 
         "6x6 anisotropic stiffness tensor,"
         + " in row-major form", DEFAULT_STIFFNESS_TENSOR_VEC, "D36");
   }

   public FunctionPropertyList getAllPropertyInfo() {
      return myProps;
   }

   private static VectorNd toRowMajor(DenseMatrix A) {
      VectorNd vec = new VectorNd(A.rowSize()*A.colSize());
      int idx = 0;
      for (int i=0; i<A.rowSize(); ++i) {
         for (int j=0; j<A.colSize(); ++j) {
            vec.set(idx++, A.get(i, j));
         }
      }
      return vec;
   }
   
   public static Matrix6d createIsotropicStiffness(double E, double nu) {
      Matrix6d D = new Matrix6d();
      double lam = E*nu/((1+nu)*(1-2*nu));
      double mu = E/(2*(1+nu));
      D.m00 = lam + 2*mu;
      D.m01 = lam;
      D.m02 = lam;
      D.m10 = lam;
      D.m11 = lam + 2*mu;
      D.m12 = lam;
      D.m20 = lam;
      D.m21 = lam;
      D.m22 = lam + 2*mu;
      D.m33 = mu;
      D.m44 = mu;
      D.m55 = mu;
      return D;
   }

   public AnisotropicLinearMaterial () {
      this(LinearMaterial.DEFAULT_E, LinearMaterial.DEFAULT_NU, LinearMaterial.DEFAULT_COROTATED);
   }

   public AnisotropicLinearMaterial (double E, double nu) {
      this (E, nu, DEFAULT_COROTATED);
   }

   public AnisotropicLinearMaterial (double E, double nu, boolean corotated) {
      super(corotated);
      setStiffnessTensor(createIsotropicStiffness(E, nu));
   }

   public AnisotropicLinearMaterial (Matrix6dBase C) {
      this(C, DEFAULT_COROTATED);
   }

   public AnisotropicLinearMaterial (Matrix6dBase C, boolean corotated) {
      this.myC = new Matrix6d(C);
      setCorotated(corotated);
   }

   public Matrix6d getStiffnessTensor() {
      return myC;
   }
   
   public VectorNd getRasterizedStiffnessTensor() {
      return toRowMajor(myC);
   }
   
   public void setRasterizedStiffnessTensor(VectorNd C) {
      int idx =  0;
      for (int i=0; i<myC.rowSize(); ++i) {
         for (int j=0; j<myC.colSize(); ++j) {
            myC.set(i, j, C.get(idx++));
         }
      }
      notifyHostOfPropertyChange("stiffness tensor");
   }

   public void setStiffnessTensor(Matrix6d C) {
      if (this.myC == null) {
         this.myC = new Matrix6d(C);
      } else {
         this.myC.set(C);
      }
      notifyHostOfPropertyChange("stiffness tensor");
   }

   @Override
   protected void multiplyC(SymmetricMatrix3d sigma, SymmetricMatrix3d eps, DeformedPoint defp) {
    
      double e00 = eps.m00;
      double e11 = eps.m11;
      double e22 = eps.m22;
      double e01 = eps.m01;
      double e02 = eps.m02;
      double e12 = eps.m12;

      // multiply
      double s00 = myC.m00*e00 + myC.m01*e11 + myC.m02*e22 +
         2*myC.m03*e01 + 2*myC.m04*e12 + 2*myC.m05*e02;
      double s11 = myC.m10*e00 + myC.m11*e11 + myC.m12*e22 +
         2*myC.m13*e01 + 2*myC.m14*e12 + 2*myC.m15*e02;
      double s22 = myC.m20*e00 + myC.m21*e11 + myC.m22*e22 +
         2*myC.m23*e01 + 2*myC.m24*e12 + 2*myC.m25*e02;
      double s01 = myC.m30*e00 + myC.m31*e11 + myC.m32*e22 +
         2*myC.m33*e01 + 2*myC.m34*e12 + 2*myC.m35*e02;
      double s12 = myC.m40*e00 + myC.m41*e11 + myC.m42*e22 +
         2*myC.m43*e01 + 2*myC.m44*e12 + 2*myC.m45*e02;
      double s02 = myC.m50*e00 + myC.m51*e11 + myC.m52*e22 +
         2*myC.m53*e01 + 2*myC.m54*e12 + 2*myC.m55*e02;
      
      sigma.set(s00, s11, s22, s01, s02, s12);      
   }

   @Override
   protected void getC(Matrix6d C, DeformedPoint defp) {
      C.set(myC);
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof AnisotropicLinearMaterial)) {
         return false;
      }
      AnisotropicLinearMaterial linm = (AnisotropicLinearMaterial)mat;
      if (!myC.equals(linm)) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public AnisotropicLinearMaterial clone() {
      AnisotropicLinearMaterial mat = (AnisotropicLinearMaterial)super.clone();
      mat.myC = myC.clone();
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         myC.scale(1.0/s);
      }
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         myC.scale(s);
      }
   }

}
