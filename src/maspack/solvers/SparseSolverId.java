package maspack.solvers;

import java.util.ArrayList;
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
    * Sparse LDL^T and LU solver from Apple's Accelerate framework (Arm-based
    * MacOS only). Unsymmetric matrices require macOS 15.5 or later; on earlier
    * systems, only symmetric matrices are supported.
    */
   Accelerate (true, Matrix.SYMMETRIC),

   /**
    * Conjugate gradient
    */
   ConjugateGradient (false, Matrix.SPD);

   private boolean myIsDirect = false;
   private int myMatrixType = 0;

   private static ArrayList<SparseSolverId> myAvailableDirects;

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
      if (this == Accelerate && AccelerateSolver.isLUAvailable()) {
         // unsymmetric matrices need Accelerate's LU (macOS 15.5 or later)
         return Matrix.INDEFINITE;
      }
      return myMatrixType;
   }

   /**
    * Queries if this solver type is compatible with the indicated matrix type.
    *
    * @param matrixType matrix type to check
    * @return {@code true} if the matrix type is compatible
    */
   public boolean isCompatible (int matrixType) {
      switch (getMatrixType()) {
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
    * the first solver in the list returned by {@link #getAvailableDirectSolvers}.
    *
    * @return default direct solver
    */
   public static synchronized SparseSolverId getDefaultDirectSolver() {
      return getAvailableDirectSolvers()[0];
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
         case Accelerate: {
            return new AccelerateSolver();
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

   /**
    * Returns all the available direct solvers that should be available for the
    * current platform. (Note that it is still possible a given solver might
    * not actually be available because of trouble accessingor loading the
    * native libraries.)
    */
   public static SparseSolverId[] getAvailableDirectSolvers() {
      if (myAvailableDirects == null) {
         myAvailableDirects = new ArrayList<>();
         String osname = System.getProperty ("os.name");
         String osarch = System.getProperty ("os.arch");

         if (osname.equals ("Darwin") ||
             osname.startsWith ("Mac")) {
            if (osarch.equals ("aarch64")) {
               myAvailableDirects.add (SparseSolverId.Accelerate);
               myAvailableDirects.add (SparseSolverId.Mumps);
            }
            else {
               myAvailableDirects.add (SparseSolverId.Pardiso);
               //myAvailableDirects.add (SparseSolverId.Mumps);
            }
         }
         else {
            myAvailableDirects.add (SparseSolverId.Pardiso);
            myAvailableDirects.add (SparseSolverId.Mumps);
         }
      }
      return myAvailableDirects.toArray (new SparseSolverId[0]);
   }

   /**
    * Finds a direct solver id by name (ignoring case) for the current
    * platform. Returns {@code null} if the solver is not available.
    */
   public static SparseSolverId findAvailableDirectSolver (String name) {
      for (SparseSolverId id : getAvailableDirectSolvers()) {
         if (id.name().equals(name) && id.isDirect()) {
            return id;
         }
      }
      return null;
   }   

   /**
    * Queries whether a specified direct solver should be available for the
    * current platform.
    */
   public static boolean isDirectSolverAvailable (SparseSolverId solverId) {
      for (SparseSolverId id : getAvailableDirectSolvers()) {
         if (id == solverId && id.isDirect()) {
            return true;
         }
      }
      return false;
   }   
}
