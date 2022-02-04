package maspack.solvers;

import maspack.matrix.*;
import maspack.util.*;

/**
 * General interface for linear complementarity problem (LCP) solvers.  An LCP
 * is defined by the linear system
 * <pre>
 * w = M z + q
 * </pre>
 * and solving it entails finding w and z subject to the constraints
 * <pre>
 * w &gt;= 0, z &gt;= 0, w z = 0
 * </pre>
 * 
 * Different solvers do this in different ways with varying restrictions on the
 * matrix {@code M}. Some solves are also able to solve bounded linear
 * complementarity problems (BLCPs), which take the form
 * <pre>
 * w = M z + q = wpos - wneg,
 * 0 &le; hi - z perp wneg &ge; 0,
 * 0 &le; z - lo perp wpos &ge; 0
 * </pre>
 * 
 * <p> Full details on the solution of LCPs can be found in <i>The Linear
 * Complementarity Problem</i>, by Cottle, Pang, and Stone, in the review paper
 * ``Algorithms for Linear Complementarity Problems'', by Joaquim Judice
 * (1994), and in Claude Lacoursiere's Ph.D. thesis. <i>Ghosts and Machines:
 * Regularized Variational Methods for Interactive Simulations of Multibodies
 * with Dry Frictional Contact</i>.
 */
public interface LCPSolver {

   public static final double INF = Double.POSITIVE_INFINITY;

   /**
    * Solver return status
    */
   public enum Status {

      /**
       * A solution was found.
       */
      SOLVED,

      /**
       * Solver was unable to find a solution. This may mean either that a
       * solution does not exist, or that a solution does exist but the solver
       * is incapable of finding it.
       */
      NO_SOLUTION,

      /**
       * Iteration limit was exceeded.
       */
      ITERATION_LIMIT_EXCEEDED,

      /**
       * A numeric error was detected in the solution.
       */
      NUMERIC_ERROR,

      /**
       * Solution method for the requested LCP problem type is not implemented.
       */
      UNIMPLEMENTED
   }

   /**
    * Indicates that a variable is non-basic and at its lower bound.
    */
   public static int W_VAR_LOWER = 0;
   
   /**
    * Indicates that a variable is non-basic and at its upper bound.
    */
   public static int W_VAR_UPPER = 1;
   
   /**
    * Indicates that a variable is basic.
    */
   public static int Z_VAR = 2;
   
   /**
    * Returns a character representation of a state variable.
    *
    * @param sval state value to convert
    * @return character representation
    */
   public static char stateToChar (int sval) {
      switch (sval) {
         case Z_VAR: {
            return 'Z';
         }
         case W_VAR_LOWER: {
            return 'L';
         }
         case W_VAR_UPPER: {
            return 'U';
         }
         default: {
            return '?';
         }
      }
   }

   /**
    * Returns a string representation of a state vector.
    *
    * @param state state vector to convert to string
    * @return string representation of the state
    */
   public static String stateToString (VectorNi state) {
      return stateToString (state.getBuffer(), state.size());
   }

   /**
    * Returns a string representation of a state vector.
    *
    * @param state state vector to convert to string
    * @return string representation of the state
    */
   public static String stateToString (VectorNi state, int size) {
      return stateToString (state.getBuffer(), size);
   }

   /**
    * Returns a string representation of a state vector.
    *
    * @param state state vector to convert to string
    * @param size number of entries of {@code state} to convert
    * @return string representation of the state
    */
   public static String stateToString (int[] state, int size) {
      StringBuilder sb = new StringBuilder ();
      for (int i = 0; i < size; i++) {
         sb.append (stateToChar (state[i]));
      }
      return sb.toString ();
   }

   /**
    * Creates a state vector from a string representation
    *
    * @param str string representation of the state
    * @return state vector
    */
   public static VectorNi stringToState (String str) {
      VectorNi state = new VectorNi (str.length());
      for (int i=0; i<str.length(); i++) {
         char c = str.charAt(i);
         switch (str.charAt(i)) {
            case 'Z':
            case 'z': {
               state.set (i, Z_VAR);
               break;
            }
            case 'L':
            case 'l': {
               state.set (i, W_VAR_LOWER);
               break;
            }
            case 'H':
            case 'h': {
               state.set (i, W_VAR_UPPER);
               break;
            }
            default: {
               throw new IllegalArgumentException (
                  "Unknown character '" + c +
                  "'; characters must be one of 'ZHLzhl'");
            }
         }
      }
      return state;
   }

   /**
    * Set all elements in the specified state vector to {@link #W_VAR_LOWER}.
    * 
    * @param state state vector to clear
    */
   public static void clearState (VectorNi state) {
      for (int i=0; i<state.size(); i++) {
         state.set (i, W_VAR_LOWER);
      }
   }
   
   /**
    * Solves the LCP
    * 
    * <pre>
    * w = M z + q
    * </pre>
    * where M is a matrix whose characteristics should match the capabilities
    * of the solver.
    * 
    * @param z
    * returns the solution for z
    * @param state optional argument, which if non-{@code null}, returns the
    * state vector resulting from the LCP solve. For solvers capable of warm
    * starts (i.e., if {@link #isWarmStartSupported} returns {@code true}), the
    * value of this vector on input will also specify the initial starting
    * state.
    * @param M
    * system matrix
    * @param q
    * system vector
    * @return Status of the solution.
    */
   public Status solve (VectorNd z, VectorNi state, MatrixNd M, VectorNd q);

   /**
    * If {@link #isBLCPSupported} returns {@code true}, solves the BLCP
    * <pre>
    * w = M z + q = wpos - wneg,
    * 0 &le; hi - z perp wneg &ge; 0,
    * 0 &le; z - lo perp wpos &ge; 0,
    * </pre>
    * where M is a matrix whose characteristics should match the capabilities
    * of the solver. If BLCPs are not supported, then this method returns
    * {@link Status#UNIMPLEMENTED}.
    *
    * @param z
    * returns the solution for z
    * @param w
    * returns the solution for w
    * @param state optional argument, which if non-{@code null}, returns the
    * state vector resulting from the LCP solve. For solvers capable of warm
    * starts (i.e., if {@link #isWarmStartSupported} returns {@code true}), the
    * value of this vector on input will also specify the initial starting
    * state.
    * @param M
    * system matrix
    * @param q
    * system vector
    * @param lo
    * lower bounds for the z variables
    * @param hi
    * upper bounds for the z variables
    * @param nub
    * number of unbounded variables (corresponding to bilateral constraints).
    * If {@code nub > 0}, then the first {@code nub} entries of {@code lo}
    * and {@code hi} must be negative and positive infinity, respectively.
    * @return Status of the solution.
    */
   public Status solve (
      VectorNd z, VectorNd w, VectorNi state, MatrixNd M, VectorNd q, 
      VectorNd lo, VectorNd hi, int nub);

   /**
    * Queries whether this solver supported the solution of bounded linear
    * complementarity problems (BLCPs).
    *
    * @return {@code true} if BLCPs are supported.
    */
   public boolean isBLCPSupported();

   /**
    * Queries whether this solver supports warm starts.
    *
    * @return {@code true} if warm starts are supported.
    */
   public boolean isWarmStartSupported();

   /**
    * Returns the number of iterations that were used in the most recent
    * solution operation.
    * 
    * @return iteration count for last solution
    */
   public int getIterationCount ();

   /**
    * Returns the number of pivots that were used in the most recent solution
    * operation. For solvers that support block pivoting and have block
    * pivoting enabled, this will usually exceed the number of
    * iterations. Otherwise, this number should be roughly equal to the number
    * of iterations.
    * 
    * @return pivot count for last solution
    */
   public int getPivotCount();

   /**
    * Returns the number of pivots that were attempted and rejected because
    * they would have resulted in a basis matrix that was not SPD.
    */
   public int numFailedPivots ();

   /**
    * Returns the numeric tolerance used to determine complementarity for the
    * most recent solution. These tolerances may be updated dynamically during
    * the solution.
    */
   public double getLastSolveTol ();

}
