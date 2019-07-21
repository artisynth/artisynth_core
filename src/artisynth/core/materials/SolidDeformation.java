package artisynth.core.materials;

import maspack.matrix.*;
import artisynth.core.modelbase.FieldPoint;

/**
 * Contains information about local 3D solid deformation, to be
 * used by solid materials for computing stress and tangent matrices.
 */
public class SolidDeformation implements DeformedPoint {

   Matrix3d myF;            // deformation gradient
   double myDetF;           // determinant of the deformation gradient
   double myP;              // local pressure
   RotationMatrix3d myRot;  // local rotation (if stiffness warping)
   
   public int availableInfo() {
      return 0;
   }
   
   public SolidDeformation() {
      myF = new Matrix3d();
      clear();
   }
   
   public void clear() {
      myF.setZero();
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
      if (R == null) {
         myRot = null;
         return;
      }
      
      if (myRot == null) {
         myRot = new RotationMatrix3d();
         
      }
      myRot.set(R);;
   }
   
   /**
    * Gets a local rotation, for use if stiffness warping
    * @return rotation matrix
    */
   public RotationMatrix3d getR() {
      return myRot;
   }

   public Point3d getRestPos() {
      return new Point3d();
   }
   
   public Point3d getSpatialPos() {
      return new Point3d();
   }
   
   public int getElementType() {
      return -1;
   }
         
   public int getElementNumber() {
      return -1;
   }
         
   public int getElementSubIndex() {
      return -1;
   }
         
   public int getPointIndex() {
      return -1;
   }

   public double[] getNodeWeights() {
      return null;
   }
   
   public int[] getNodeNumbers() {
      return null;
   }
   
}
