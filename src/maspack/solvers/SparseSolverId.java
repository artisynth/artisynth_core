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
    * MUMPS (MUltifrontal Massively Parallel sparse direct Solver), developed
    * by CERFACS, CNRS, ENS Lyon, INP Toulouse, Inria, Mumps Technologies and
    * the University of Bordeaux.
    */
   Mumps (true, Matrix.INDEFINITE),

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

   // availability of Pardiso and MUMPS, determined once when first needed,
   // since each PardisoSolver/MumpsSolver.isAvailable() call creates a
   // test solver
   private static Boolean myPardisoAvailable = null;
   private static Boolean myMumpsAvailable = null;
   private static SparseSolverId myDefaultDirectSolver = null;
   private static boolean myPardisoFallbackWarned = false;

   private static synchronized boolean pardisoAvailable() {
      if (myPardisoAvailable == null) {
         myPardisoAvailable = PardisoSolver.isAvailable();
      }
      return myPardisoAvailable;
   }

   private static synchronized boolean mumpsAvailable() {
      if (myMumpsAvailable == null) {
         myMumpsAvailable = MumpsSolver.isAvailable();
      }
      return myMumpsAvailable;
   }

   /**
    * Returns the default direct solver for the current platform. This is
    * Pardiso if it is available, and otherwise MUMPS, if that is
    * available. Pardiso is not available on platforms without Intel MKL,
    * such as Arm-based MacOS. If neither is available, Pardiso is
    * returned, so that attempts to use it produce the usual error.
    *
    * @return default direct solver
    */
   public static synchronized SparseSolverId getDefaultDirectSolver() {
      if (myDefaultDirectSolver == null) {
         if (pardisoAvailable()) {
            myDefaultDirectSolver = Pardiso;
         }
         else if (mumpsAvailable()) {
            myDefaultDirectSolver = Mumps;
         }
         else {
            myDefaultDirectSolver = Pardiso;
         }
      }
      return myDefaultDirectSolver;
   }

   /**
    * Creates and returns the solver for this type, if it represents a direct
    * solver. Otherwise, returns {@code null}. If Pardiso is requested but is
    * not available on this platform, and MUMPS is, then a MUMPS solver is
    * returned instead, with a warning printed the first time this happens.
    *
    * @return new direct solver for this type, or {@code null}
    */
   public DirectSolver createDirectSolver() {
      switch (this) {
         case Pardiso: {
            if (!pardisoAvailable() && mumpsAvailable()) {
               synchronized (SparseSolverId.class) {
                  if (!myPardisoFallbackWarned) {
                     System.out.println (
                        "Warning: Pardiso is not available on this " +
                        "platform; using MUMPS instead");
                     myPardisoFallbackWarned = true;
                  }
               }
               return new MumpsSolver();
            }
            return new PardisoSolver();
         }
         case Mumps: {
            return new MumpsSolver();
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
