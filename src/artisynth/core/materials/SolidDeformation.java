package artisynth.core.materials;

import maspack.matrix.*;

/**
 * Contains information about local 3D solid deformation, to be
 * used by solid materials for computing stress and tangent matrices.
 */
public class SolidDeformation {

   Matrix3d myF;    // deformation gradient
   double myDetF;   // determinant of the deformation gradient
   double myP;      // local pressure
   Matrix3dBase myRot;          // local rotation (if stiffness warping)

   public SolidDeformation() {
      myF = new Matrix3d();
      myDetF = 0;
      myP = 0;
      myRot = null;
   }

   /**
    * Returns the deformation gradient. Should not be modified.
    */
   public Matrix3d getF() {
      return myF;
   }

   /**
    * Sets the deformation gradient.
    */
   public void setF (Matrix3d F) {
      myF.set (F);
      myDetF = F.determinant();
   }

   /**
    * Sets the deformation gradient by multiplying together J and invJ0.
    */
   public void setF (Matrix3d J, Matrix3d invJ0) {
      myF.mul (J, invJ0);
      myDetF = myF.determinant();
   }

   /**
    * Returns the determinant of the deformation gradient.
    */
   public double getDetF() {
      return myDetF;
   }

   /**
    * Returns the average pressure.
    */
   public double getAveragePressure() {
      return myP;
   }

   /**
    * Set the average pressure.
    */
   public void setAveragePressure (double p) {
      myP = p;
   }

   /**
    * Sets a local rotation, for use if stiffness warping
    * @param R rotation matrix
    */
   public void setR(Matrix3dBase R) {
      myRot = R;
   }
   
   /**
    * Gets a local rotation, for use if stiffness warping
    * @return rotation matrix
    */
   public Matrix3dBase getR() {
      return myRot;
   }

   /**
    * Computes the right Cauchy-Green tensor from the deformation gradient.
    */
   public void computeRightCauchyGreen (SymmetricMatrix3d C) {
      C.mulTransposeLeft (myF);
   }

   /**
    * Computes the left Cauchy-Green tensor from the deformation gradient.
    */
   public void computeLeftCauchyGreen (SymmetricMatrix3d B) {
      B.mulTransposeRight (myF);
   }

   /**
    * Computes the right deviatoric Cauchy-Green tensor from the deformation
    * gradient.
    */
   public void computeDevRightCauchyGreen (SymmetricMatrix3d CD) {
      CD.mulTransposeLeft (myF);
      CD.scale (Math.pow(myDetF, -2.0/3.0));
   }

   /**
    * Computes the left deviatoric Cauchy-Green tensor from the deformation
    * gradient.
    */
   public void computeDevLeftCauchyGreen (SymmetricMatrix3d BD) {
      BD.mulTransposeRight (myF);
      BD.scale (Math.pow(myDetF, -2.0/3.0));
   }

}
