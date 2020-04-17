package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;

/**
 * QPTerm that adds to either the inequality or equality constraints of
 * a quadratic program.
 */
public interface QPConstraintTerm extends QPTerm {

   /**
    * Appends the constraints for this term to the constraint matrix A and
    * offset vector b. For inequality constraints, A and b define
    * the constraints
    * <pre>
    * A x &gt;= b
    * </pre>
    * while for equality constraints, they define the constraints
    * <pre>
    * A x = b.
    * </pre>
    * 
    * @param A quadratic program constraint matrix
    * @param b quadratic program offset vector
    * @param rowoff row offset within A and b where the constraints should be
    * added
    * @param t0 time at start of step
    * @param t1 time at end of step
    */
   public int getTerm (MatrixNd A, VectorNd b, int rowoff, double t0, double t1);

   /**
    * Returns the number of constraints associated with this term.
    * @param qpsize size of the quadratic program. For the tracking
    * controller, this is the number of excitation values being used,
    */
   public int numConstraints (int qpsize);
   

}
