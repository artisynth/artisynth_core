package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;

/**
 * QPTerm that adds to the cost components Q and p of a quadratic program
 * that minimizes 
 * <pre>
 *    1/2 x^T Q x + p^T x
 * </pre>
 * subject to equality and inequality constraints.
 */
public interface QPCostTerm extends QPTerm {
   
   /**
    * Adds the contribution of this term to the quadratic cost
    * components Q and p.
    * 
    * @param Q quadratic term
    * @param p proportional term
    * @param t0 time at start of step
    * @param t1 time at end of step
    */
   public void getQP(MatrixNd Q, VectorNd p, double t0, double t1);
}
