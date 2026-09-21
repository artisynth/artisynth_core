/*
 * Hybrid direct/iterative solves, shared by the MUMPS and Pardiso JNI
 * wrappers.
 *
 * The most recent factorization, of values V0, is used as a preconditioner M
 * for solving A x = b with the current values A = V1, which are needed only
 * for matrix-vector products. This pays off when V1 is close to V0, since a
 * preconditioner solve (a forward/backward substitution) costs much less
 * than a factorization.
 *
 * A solver wrapper derives from HybridSolver, implements precondSolve(), and
 * registers its CRS structure and current-value storage with
 * setIterativeStructure() once the matrix has been analyzed.
 */
#ifndef HYBRID_SOLVE_H
#define HYBRID_SOLVE_H

// methods for HybridSolver::iterativeSolve(). Derived classes may add
// methods numbered from HYBRID_NUM_METHODS.
#define HYBRID_GMRES       0
#define HYBRID_CGS         1
#define HYBRID_NUM_METHODS 2

class HybridSolver {

  private:

   int myIterSize;            // matrix size, or 0 if no structure is set
   int myIterNumVals;         // number of stored values
   const int *myIterRowOffs;  // 1-based CRS row offsets (size+1)
   const int *myIterColIdxs;  // 1-based CRS column indices
   double *myIterCurVals;     // current values, owned by the derived class
   int myIterSymmetric;       // if true, only the upper triangle is stored
   void *myCsrHandle;         // MKL sparse_matrix_t wrapping the CRS arrays
   double *myIterX;           // x and b storage for callers that copy
   double *myIterB;           // their data in (see getIterativeBuffers())
   int myIterVecSize;         // allocated size of myIterX and myIterB

   int gmresSolve (
      double* x, const double* b, double tol, int maxSolves, int restart);
   int cgsSolve (double* x, const double* b, double tol, int maxSolves);

  protected:

   double *myWork;            // workspace
   long long myMaxWorkSize;
   int myLastIterativeSolves;
   double myLastIterativeResidual;
   // timing of the most recent iterative solve, in seconds
   double myIterTotalTime;
   double myIterSolveTime;    // preconditioner solves
   double myIterMatVecTime;   // matrix-vector products

   // Registers the CRS structure and current-value storage. The arrays are
   // referenced, not copied, and must remain valid until
   // clearIterativeStructure() is called, which must be done before any of
   // them are reallocated or freed.
   void setIterativeStructure (
      int size, int numVals, const int* rowOffs, const int* colIdxs,
      double* curVals, int symmetric);
   void clearIterativeStructure();
   void releaseIterativeWork();

   int ensureWork (long long size);
   int matVec (double* y, const double* x);
   double trueResidual (double* r, const double* x, const double* b);
   int timedPrecondSolve (double* z, const double* r);

   // Applies the preconditioner z = M^{-1} r, using the most recent
   // factorization. Returns a negative value on error.
   virtual int precondSolve (double* z, const double* r) = 0;

   // Called before and after the iterations, to set up and restore solver
   // settings. If beginIterations() returns a negative value, the solve
   // fails and endIterations() is not called.
   virtual int beginIterations();
   virtual void endIterations();

   // Performs the iterations for the indicated method, setting
   // myLastIterativeSolves and myLastIterativeResidual. Returns the number
   // of iterations on success, or <= 0 on failure. May be overridden to add
   // methods.
   virtual int iterate (
      int method, double* x, const double* b, double tol, int maxSolves,
      int restart);

   static double wallTime();
   static double norm2 (const double* a, int n);

  public:

   HybridSolver();
   virtual ~HybridSolver();

   // Solves the matrix with values vals iteratively, using the most recent
   // factorization as a preconditioner. method is one of HYBRID_GMRES or
   // HYBRID_CGS (or a method added by a derived class); tol is the required
   // relative residual ||b - A x||/||b||; maxSolves limits the number of
   // preconditioner solves; restart is the GMRES restart length. Returns
   // the number of iterations on success, or 0 on failure.
   int iterativeSolve (
      const double* vals, double* x, const double* b, double tol,
      int method, int maxSolves, int restart);

   // Returns native storage that a caller (e.g. a JNI wrapper) can fill
   // before calling iterativeSolve(), so that no external arrays need to be
   // held during the solve: vals is the current-value storage registered
   // with setIterativeStructure() (getIterativeNumVals() entries), and x and
   // b have getIterativeSize() entries. Passing these to iterativeSolve()
   // avoids any further copying. Returns a negative value if no structure is
   // set or allocation fails.
   int getIterativeBuffers (double** vals, double** x, double** b);
   int getIterativeSize();
   int getIterativeNumVals();

   int getLastIterativeSolves();
   double getLastIterativeResidual();

   // Returns the total, preconditioner solve and matrix product times (in
   // seconds) of the most recent iterative solve in times[0], times[1] and
   // times[2]. The remainder of the total is vector operations and setup.
   void getLastIterativeTimes (double* times);
};

#endif
