package maspack.solvers;

import maspack.matrix.Matrix;

/**
 * Identifies the general-purpose sparse solvers available in this package.
 */
public enum SparseSolverId {
   /**
    * Intel MKL implementation of Pardiso, originally developed by Olaf Schenk
    p*/
   Pardiso (true, Matrix.INDEFINITE),

   /**
    * Umfpack, from SuiteSparse, developed by Tim Davis et al.
    */
   Umfpack (true, Matrix.INDEFINITE),

   /**
    * Conjugate gradient
    */
   ConjugateGradient (false, Matrix.SPD);

   private boolean myIsDirect = false;
   private int myMatrixType = 0;

   SparseSolverId (boolean isDirect, int matrixType) {
      myIsDirect = isDirect;
      myMatrixType = matrixType;
   }

   /**
    * Queries whether the solver is direct.
    *
    * @return {@code true} if the solver is direct
    */
   public boolean isDirect() {
      return myIsDirect;
   }

   public int getMatrixType() {
      return myMatrixType;
   }

   /**
    * Queries if this solver type is compatible with the indicated matrix type.
    *
    * @param matrixType matrix type to check
    * @return {@code true} if the matrix type is compatible
    */

   public boolean isCompatible (int matrixType) {
      switch (myMatrixType) {
         case Matrix.INDEFINITE: {
            return true;
         }
         case Matrix.SYMMETRIC: {
            return ((matrixType & Matrix.SYMMETRIC) != 0);
         }
         case Matrix.SPD: {
            return (matrixType == Matrix.SPD);
         }
         default: {
            throw new UnsupportedOperationException (
               "Unknown solver matrix type " + myMatrixType);
         }
      }
   }

   /**
    * Creates and returns the solver for this type, if it represents a direct
    * solver. Otherwise, returns {@code null}.
    * 
    * @return new direct solver for this type, or {@code null}
    */
   public DirectSolver createDirectSolver() {
      switch (this) {
         case Pardiso: {
            return new PardisoSolver();
         }
         case Umfpack: {
            return new UmfpackSolver();
         }
         default: {
            return null;
         }
      }
   }
   
   /**
    * Creates and returns the solver for this type, if it represents an
    * iterative solver. Otherwise, returns {@code null}.
    * 
    * @return new iterative solver for this type, or {@code null}
    */
   public IterativeSolver createIterativeSolver() {
      switch (this) {
         case ConjugateGradient: {
            return new CGSolver();
         }
         default: {
            return null;
         }
      }
   }
   

}
