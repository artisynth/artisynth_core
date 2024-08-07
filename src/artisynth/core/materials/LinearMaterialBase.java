package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

/**
 * Base material for both isotropic and anisotropic linear materials
 * @author Antonio
 *
 */
public abstract class LinearMaterialBase extends FemMaterial {

   public static FieldPropertyList myProps =
      new FieldPropertyList (LinearMaterialBase.class, FemMaterial.class);

   protected static boolean DEFAULT_COROTATED = true;

   private boolean myCorotated = DEFAULT_COROTATED;
   PropertyMode myCorotatedMode = PropertyMode.Inherited;

   static {
      myProps.addInheritable (
         "corotated:Inherited isCorotated",
         "apply corotation", DEFAULT_COROTATED);
   }

   public FieldPropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected LinearMaterialBase (){
   }
   
   protected LinearMaterialBase(boolean corotated) {
      setCorotated (corotated);
   }

   @Override
   public boolean isInvertible() {
      return true;
   }   

   @Override
   public boolean isLinear() {
      return true;
   }

   public synchronized void setCorotated (boolean enable) {
      myCorotated = enable;
      myCorotatedMode =
         PropertyUtils.propagateValue (this, "corotated", myCorotated, myCorotatedMode);
      notifyHostOfPropertyChange();
   }

   @Override
   public boolean isCorotated() {
      return myCorotated;
   }

   public void setCorotatedMode (PropertyMode mode) {
      myCorotatedMode =
         PropertyUtils.setModeAndUpdate (this, "corotated", myCorotatedMode, mode);
   }

   public PropertyMode getCorotatedMode() {
      return myCorotatedMode;
   }

   private SVDecomposition3d mySVD = null;
   protected RotationMatrix3d computeRotation(Matrix3d F, SymmetricMatrix3d P) {
      if (mySVD == null) {
         mySVD = new SVDecomposition3d();
      }
      RotationMatrix3d R = new RotationMatrix3d();
      mySVD.polarDecomposition (R, P, F);
      return R;
   }
   
   /**
    * Multiplies strain by the spatial stiffness tensor C to compute the
    * stress tensor.  Code must be written so that sigma and eps can be
    * the same matrix
    * 
    * @param sigma stress
    * @param eps   Cauchy strain
    * @param defp TODO
    */
   protected abstract void multiplyC (
      SymmetricMatrix3d sigma, SymmetricMatrix3d eps, DeformedPoint defp);
   
   /**
    * Populates the spatial stiffness tensor C
    * @param C
    * @param defp TODO
    */
   protected abstract void getC(Matrix6d C, DeformedPoint defp);
   
   /**
    * {@inheritDoc}
    */
   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {

      RotationMatrix3d R = def.getR();
      Matrix3d F = def.getF();

      // cauchy strain, rotated if necessary
      if (myCorotated) {
         if (R == null) {
            R = computeRotation(F, sigma);
         }
         else {
            // remove rotation from F
            sigma.mulTransposeLeftSymmetric(R, F);
         }
      }
      else {
         sigma.setSymmetric (F);
      }
      sigma.m00 -= 1;
      sigma.m11 -= 1;
      sigma.m22 -= 1;
      
      multiplyC (sigma, sigma, def);

      // rotate stress back to original frame
      if (isCorotated()) {
         sigma.mulLeftAndTransposeRight (R);
      }

      if (D != null) {
         // get spatial stiffness tensor
         getC(D, def);
      
         // rotate if corotated
         if (isCorotated()) {
            // R rotates from material frame to the spatial one. Transpose
            // of R rotates from spatial frame to material one.
            TensorUtils.unrotateTangent (D, D, R);
         }
      }
   }
   
   public double computeStrainEnergyDensity (
      DeformedPoint def, Matrix3d Q, double excitation, 
      MaterialStateObject state) {

      RotationMatrix3d R = def.getR();
      Matrix3d F = def.getF();

      SymmetricMatrix3d eps = new SymmetricMatrix3d();

      // cauchy strain, rotated if necessary
      if (myCorotated) {
         if (R == null) {
            R = computeRotation(F, eps);
         }
         else {
            // remove rotation from F
            eps.mulTransposeLeftSymmetric(R, F);
         }
      }
      else {
         eps.setSymmetric (F);
      }
      eps.m00 -= 1;
      eps.m11 -= 1;
      eps.m22 -= 1;

      // unroll into voigt vector to multiply by C
      VectorNd evec = new VectorNd(6);
      VectorNd svec = new VectorNd(6);
      evec.set (0, eps.m00);
      evec.set (1, eps.m11);
      evec.set (2, eps.m22);
      evec.set (3, 2*eps.m01);
      evec.set (4, 2*eps.m12);
      evec.set (5, 2*eps.m02);

      Matrix6d C = new Matrix6d();
      // get spatial stiffness tensor
      getC(C, def);
      C.mul(svec,evec);
       // no need to rotate sigma since computation is being done in co-rotated
      // frame
      return svec.dot(evec)/2;
   }
   
   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof LinearMaterialBase)) {
         return false;
      }
      LinearMaterialBase linm = (LinearMaterialBase)mat;
      if (myCorotated != linm.myCorotated) {
         return false;
      } else {
         return super.equals (mat);
      }
   }

   public LinearMaterialBase clone() {
      LinearMaterialBase mat = (LinearMaterialBase)super.clone();
      return mat;
   }

}
