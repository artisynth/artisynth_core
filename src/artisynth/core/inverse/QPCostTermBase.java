package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.util.InternalErrorException;

/**
 * Base implementation for a QPCostTerm.
 */
public abstract class QPCostTermBase extends QPTermBase 
   implements QPCostTerm {

   /**
    * {@inheritDoc}
    */
   @Override
   public Type getType() {
      return Type.COST;
   }
   
   public QPCostTermBase() {
      this(DEFAULT_WEIGHT);
   }
   
   public QPCostTermBase(double weight) {
      super (weight);
   }
   
   /**
    * Adds to the quadratic cost components Q and p according to
    * <pre>
    * Q += H^T H
    * p -= H^T b
    * </pre>
    */
   protected void computeAndAddQP (
      MatrixNd Q, VectorNd p, MatrixNd H, VectorNd b) {
      int size = Q.rowSize();
      if (H.colSize() != size) {
         throw new InternalErrorException (
            "H column size = "+H.colSize()+", expected " +size); 
      }
      MatrixNd Qadd = new MatrixNd (size, size);
      VectorNd Psub = new VectorNd (size);
      Qadd.mulTransposeLeft (H,H);
      Q.add (Qadd);
      Psub.mulTranspose (H, b);
      p.sub (Psub);
   }
}
