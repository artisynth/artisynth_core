package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

/**
 * Base material for both isotropic and anisotropic linear materials
 * @author Antonio
 *
 */
public abstract class LinearMaterialBase extends FemMaterial {

   public static PropertyList myProps =
      new PropertyList (LinearMaterialBase.class, FemMaterial.class);

   protected static boolean DEFAULT_COROTATED = true;

   private boolean myCorotated = DEFAULT_COROTATED;
   PropertyMode myCorotatedMode = PropertyMode.Inherited;

   static {
      myProps.addInheritable (
         "corotated:Inherited isCorotated",
         "apply corotation", DEFAULT_COROTATED);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected LinearMaterialBase (){
   }
   
   protected LinearMaterialBase(boolean corotated) {
      myCorotated = corotated;
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
    */
   protected abstract void multiplyC(SymmetricMatrix3d sigma, SymmetricMatrix3d eps);
   
   /**
    * Populates the spatial stiffness tensor C
    * @param C
    */
   protected abstract void getC(Matrix6d C);
   
   public void computeStress (
      SymmetricMatrix3d sigma, SolidDeformation def, Matrix3d Q,
      FemMaterial baseMat) {

      RotationMatrix3d R = def.getR();
      Matrix3d F = def.getF();

      // cauchy strain, rotated if necessary
      if (myCorotated) {
         if (R == null) {
            R = computeRotation(F, sigma);
         } else {
            // remove rotation from F
            sigma.mulTransposeLeftSymmetric(R, F);
         }
      } else {
         sigma.setSymmetric (F);
      }
      sigma.m00 -= 1;
      sigma.m11 -= 1;
      sigma.m22 -= 1;
      
      multiplyC(sigma, sigma);

      // rotate stress back to original frame
      if (isCorotated()) {
         sigma.mulLeftAndTransposeRight (R);
      }
   }
   
   @Override
   public void computeTangent(
      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def, Matrix3d Q,
      FemMaterial baseMat) {

      // get spatial stiffness tensor
      getC(D);
      
      // rotate if corotated
      if (isCorotated()) {

         // need to rotate this tensor from linear frame into material one
         RotationMatrix3d R = def.getR();
         if (R == null) {
            Matrix3d F = def.getF();
            R = computeRotation(F, null);
         }

         // R rotates from material frame to the spatial one. Transpose
         // of R rotates from spatial frame to material one.
         TensorUtils.unrotateTangent (D, D, R);
      }
      
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
