package artisynth.core.mechmodels;

import java.util.ArrayList;

import maspack.matrix.*;

/**
 * Class that applies both mass and length scaling to a stiffness matrix.  Mass
 * scaling is applied to all blocks. Length scaling is applied to blocks
 * associated with frames that involve either angular or moment terms.
 */
public class StiffnessMatrixScaler implements SolveMatrixModifier {

   double myMassScale;
   double myLengthScale;

   public StiffnessMatrixScaler (double massScale, double lengthScale) {
      myMassScale = massScale;
      myLengthScale = lengthScale;
   }

   /**
    * {@inheritDoc} 
    */
   public void modify (
      SparseBlockMatrix K, VectorNd f, ArrayList<DynamicComponent> comps) {

      if (f != null && f.size() != K.rowSize()) {
         throw new IllegalArgumentException (
            "f size "+f.size()+" incompatible with K size "+K.getSize());
      }
      if (comps != null && comps.size() != K.numBlockRows()) {
         throw new IllegalArgumentException (
            "comps size "+comps.size()+" incompatible with K block size "+
            K.numBlockRows() + " X " + K.numBlockCols());
      }

      // scale the stiffness matrix
      for (int bi=0; bi<K.numBlockRows(); bi++) {
         MatrixBlock blk = K.firstBlockInRow (bi);
         while (blk != null) {
            int bj = blk.getBlockCol();
            blk.scale (myMassScale);
            if (comps != null) {
               if (comps.get(bi) instanceof Frame &&
                   comps.get(bj) instanceof Frame &&
                   blk instanceof Matrix6d) {
                  Matrix6d mblk = (Matrix6d)blk;
                  Matrix3d M = new Matrix3d();
                  mblk.getSubMatrix03 (M);
                  M.scale (myLengthScale);
                  mblk.setSubMatrix03 (M);
                  mblk.getSubMatrix30 (M);
                  M.scale (myLengthScale);
                  mblk.setSubMatrix30 (M);
                  mblk.getSubMatrix33 (M);
                  M.scale (myLengthScale*myLengthScale);
                  mblk.setSubMatrix33 (M);
               }
               else if (comps.get(bi) instanceof Frame &&
                        blk instanceof Matrix6x3) {
                  Matrix6x3 mblk = (Matrix6x3)blk;
                  Matrix3d M = new Matrix3d();
                  mblk.getSubMatrix30 (M);
                  M.scale (myLengthScale);
                  mblk.setSubMatrix30 (M);
               }
               else if (comps.get(bj) instanceof Frame &&
                        blk instanceof Matrix3x6) {
                  Matrix3x6 mblk = (Matrix3x6)blk;
                  Matrix3d M = new Matrix3d();
                  mblk.getSubMatrix03 (M);
                  M.scale (myLengthScale);
                  mblk.setSubMatrix03 (M);
               }
            }
            blk = blk.next();
         }
      }
      // scale the force vector
      if (f != null) {
         f.scale (myMassScale);
         if (comps != null) {
            for (int bi=0; bi<comps.size(); bi++) {
               if (comps.get(bi) instanceof Frame) {
                  int k = K.getBlockRowOffset(bi) + 3;
                  for (int j=0; j<3; j++) {
                     f.set (k+j, f.get(k+j)*myLengthScale);
                  }
               }
            }
         }
      }
   }
}
