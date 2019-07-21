package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.InternalErrorException;

public abstract class IncompressibleMaterialBase extends FemMaterial {

   public static final double DEFAULT_BULK_MODULUS = 100000;
   public static final BulkPotential DEFAULT_BULK_POTENTIAL =
      BulkPotential.QUADRATIC;

   public enum BulkPotential {
      QUADRATIC,
      LOGARITHMIC
   };

   private double myBulkModulus = DEFAULT_BULK_MODULUS; // bulk modulus
   PropertyMode myBulkModulusMode = PropertyMode.Inherited;
   ScalarFieldPointFunction myBulkModulusFunction = null;
   protected BulkPotential myBulkPotential = DEFAULT_BULK_POTENTIAL;
   PropertyMode myBulkPotentialMode = PropertyMode.Inherited;

   public static FunctionPropertyList myProps =
      new FunctionPropertyList (
         IncompressibleMaterialBase.class, FemMaterial.class);

   static {
      myProps.addInheritableWithFunction (
         "bulkModulus:Inherited", "Bulk modulus", DEFAULT_BULK_MODULUS);
      myProps.addInheritable (
         "bulkPotential:Inherited", "Incompressibility potential function",
         DEFAULT_BULK_POTENTIAL);
   }

   public FunctionPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public synchronized void setBulkModulus (double nu) {
      myBulkModulus = nu;
      myBulkModulusMode =
         PropertyUtils.propagateValue (
            this, "bulkModulus", myBulkModulus, myBulkModulusMode);
      notifyHostOfPropertyChange();
   }

   public double getBulkModulus() {
      return myBulkModulus;
   }

   public void setBulkModulusMode (PropertyMode mode) {
      myBulkModulusMode =
         PropertyUtils.setModeAndUpdate (
            this, "bulkModulus", myBulkModulusMode, mode);
   }

   public PropertyMode getBulkModulusMode() {
      return myBulkModulusMode;
   }
   
   public double getBulkModulus (FieldPoint dp) {
      if (myBulkModulusFunction == null) {
         return getBulkModulus();
      }
      else {
         return myBulkModulusFunction.eval (dp);
      }
   }

   public ScalarFieldPointFunction getBulkModulusFunction() {
      return myBulkModulusFunction;
   }
      
   public void setBulkModulusFunction (ScalarFieldPointFunction func) {
      myBulkModulusFunction = func;
      notifyHostOfPropertyChange();
   }
   
   public void setBulkModulusField (
      ScalarField field, boolean useRestPos) {
      myBulkModulusFunction = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public ScalarField getBulkModulusField () {
      return FieldUtils.getFieldFromFunction (myBulkModulusFunction);
   }

   public synchronized void setBulkPotential (BulkPotential potential) {
      myBulkPotential = potential;
      myBulkPotentialMode =
         PropertyUtils.propagateValue (
            this, "bulkPotential", myBulkPotential, myBulkPotentialMode);
      notifyHostOfPropertyChange();
   }

   public BulkPotential getBulkPotential() {
      return myBulkPotential;
   }

   public void setBulkPotentialMode (PropertyMode mode) {
      myBulkPotentialMode =
         PropertyUtils.setModeAndUpdate (
            this, "bulkPotential", myBulkPotentialMode, mode);
   }

   public PropertyMode getBulkPotentialMode() {
      return myBulkPotentialMode;
   }

   /**
    * Returns the effective bulk modulus for a given nominal (linear) bulk
    * modulus K and Jacobian determinant J. The effective and linear 
    * modulus will differ in cases when the potential function is
    * non-quadratic.
    * 
    * @param K nominal (linear) bulk modulus
    * @param J Jacobian determinant
    * @return effective bulk modulus
    */
   public double getEffectiveModulus (double K, double J) {
      switch (myBulkPotential) {
         case QUADRATIC: {
            return K;
         }
         case LOGARITHMIC: {
            return K*(1-Math.log(J))/(J*J);
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented potential " + myBulkPotential);
         }
      }
   }

   /**
    * Returns the effective pressure for a given nominal (linear) bulk
    * modulus K and Jacobian determinant J.
    * 
    * @param K nominal (linear) bulk modulus
    * @param J Jacobian determinant
    * @return effective pressure 
    */
   public double getEffectivePressure (double K, double J) {
      switch (myBulkPotential) {
         case QUADRATIC: {
            return K*(J-1);
         }
         case LOGARITHMIC: {
            return K*(Math.log(J))/J;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented potential " + myBulkPotential);
         }
      }
   }

   public boolean isIncompressible() {
      return true;
   }

   public IncompressibleMaterialBase getIncompressibleComponent() {
      return this;
   }
   
   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof IncompressibleMaterialBase)) {
         return false;
      }
      IncompressibleMaterialBase imat = (IncompressibleMaterialBase)mat;
      if (myBulkModulus != imat.myBulkModulus) {
         return false;
      }
      if (myBulkPotential != imat.myBulkPotential) {
         return false;
      }
      return super.equals (mat);
   }

   // Sanchez, March 27, 2013
   // useful for separating incompressibility stuff from computeStressAndStiffness() function
   public void computePressureStress(SymmetricMatrix3d sigma, double p) {
      sigma.setZero();
      sigma.m00 = p;
      sigma.m11 = p;
      sigma.m22 = p;
   }
   
   public void addPressureStress(SymmetricMatrix3d sigma, double p) {
      sigma.m00 += p;
      sigma.m11 += p;
      sigma.m22 += p;
   }

   public void computePressureTangent(Matrix6d D, double p) {
      D.setZero();
      TensorUtils.addScaledIdentityProduct (D, p);
      TensorUtils.addScaledIdentity (D, -2*p);
      D.setLowerToUpper();
   }
   
   public void addPressureTangent(Matrix6d D, double p) {
      TensorUtils.addScaledIdentityProduct (D, p);
      TensorUtils.addScaledIdentity (D, -2*p);

      /*
         D.m00 += - p;
         D.m11 += - p;
         D.m22 += - p;

         D.m01 += p;
         D.m02 += p;
         D.m12 += p;
      
         D.m33 += - p;
         D.m44 += - p;
         D.m55 += - p;
      */
   }

   public abstract void computeDevStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state);

   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {

      computeDevStressAndTangent (sigma, D, def, Q, excitation, state);
      double p = def.getAveragePressure();

      addPressureStress (sigma, p);
      if (D != null) {
         addPressureTangent (D, p);
      }
   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         setBulkModulus (myBulkModulus/s);
      }
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         setBulkModulus (myBulkModulus*s);
      }
   }
}
   
   
