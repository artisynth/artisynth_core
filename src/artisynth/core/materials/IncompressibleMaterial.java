package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.InternalErrorException;

public class IncompressibleMaterial extends FemMaterial {

   public static final double DEFAULT_KAPPA = 100000;
   public static final BulkPotential DEFAULT_BULK_POTENTIAL =
      BulkPotential.QUADRATIC;

   public enum BulkPotential {
      QUADRATIC,
      LOGARITHMIC
         };

   private double myKappa = DEFAULT_KAPPA; // bulk modulus
   PropertyMode myKappaMode = PropertyMode.Inherited;
   protected BulkPotential myBulkPotential = DEFAULT_BULK_POTENTIAL;
   PropertyMode myBulkPotentialMode = PropertyMode.Inherited;

   public IncompressibleMaterial() {
      super();
   }
   
   public IncompressibleMaterial (double kappa) {
      this();
      setBulkModulus (kappa);
   }
   
   public static PropertyList myProps =
      new PropertyList(IncompressibleMaterial.class, FemMaterial.class);

   static {
      myProps.addInheritable (
         "bulkModulus:Inherited", "Bulk modulus", DEFAULT_KAPPA);
      myProps.addInheritable (
         "bulkPotential:Inherited", "Incompressibility potential function",
         DEFAULT_BULK_POTENTIAL);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public synchronized void setBulkModulus (double nu) {
      myKappa = nu;
      myKappaMode =
         PropertyUtils.propagateValue (this, "bulkModulus", myKappa, myKappaMode);
      notifyHostOfPropertyChange();
   }

   public double getBulkModulus() {
      return myKappa;
   }

   public void setBulkModulusMode (PropertyMode mode) {
      myKappaMode =
         PropertyUtils.setModeAndUpdate (this, "bulkModulus", myKappaMode, mode);
   }

   public PropertyMode getBulkModulusMode() {
      return myKappaMode;
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

   public double getEffectiveModulus (double J) {
      switch (myBulkPotential) {
         case QUADRATIC: {
            return myKappa;
         }
         case LOGARITHMIC: {
            return myKappa*(1-Math.log(J))/(J*J);
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented potential " + myBulkPotential);
         }
      }
   }

   public double getEffectivePressure (double J) {
      switch (myBulkPotential) {
         case QUADRATIC: {
            return myKappa*(J-1);
         }
         case LOGARITHMIC: {
            return myKappa*(Math.log(J))/J;
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

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof IncompressibleMaterial)) {
         return false;
      }
      IncompressibleMaterial imat = (IncompressibleMaterial)mat;
      if (myKappa != imat.myKappa) {
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
   }

   public void computeStress (
      SymmetricMatrix3d sigma, SolidDeformation def, Matrix3d Q,
      FemMaterial baseMat) {

      double avgp = def.getAveragePressure();
      sigma.setZero();
      sigma.m00 += avgp;
      sigma.m11 += avgp;
      sigma.m22 += avgp;
   }
   
   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def, 
      Matrix3d Q, FemMaterial baseMat) {

      // mean pressure
      double p = def.getAveragePressure();      
      D.setZero();

      TensorUtils.addScaledIdentityProduct (D, p);
      TensorUtils.addScaledIdentity (D, -2*p);
      D.setLowerToUpper();
   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         setBulkModulus (myKappa/s);
      }
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         setBulkModulus (myKappa*s);
      }
   }

   @Override
   public boolean isInvertible() {
      // right now, true only with a quadratic bulk potential,
      // and for IncompressibleMaterial specifically; not any
      // of the base classes
      return (getClass() == IncompressibleMaterial.class &&
              myBulkPotential == BulkPotential.QUADRATIC);
   }

}
   
   
