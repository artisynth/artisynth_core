/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.util.Random;

import maspack.matrix.ImproperStateException;
import maspack.matrix.Matrix;
import maspack.matrix.MatrixNd;
import maspack.matrix.SVDecomposition;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.util.RandomGenerator;

/**
 * Computes a non-negative matrix factorization (NMF) of a non-negative
 * {@code m X n} matrix {@code V}:
 * <pre>
 * V ~= W H,   W &gt;= 0,  H &gt;= 0
 * </pre>
 * where {@code W} is {@code m X k} and {@code H} is {@code k X n}, with
 * {@code k} (the number of <i>factors</i>) typically much smaller than either
 * {@code m} or {@code n}. The factorization minimizes the Frobenius norm of
 * the residual {@code V - W H}, optionally with an L2 (Tikhonov)
 * regularization on {@code W} and {@code H}.
 *
 * <p>A common application is the extraction of <i>muscle synergies</i> from
 * muscle excitation data, in which case the columns of {@code V} contain the
 * excitations for each of {@code m} muscles at each of {@code n} time samples,
 * the columns of {@code W} contain the {@code k} synergy vectors, and the rows
 * of {@code H} contain the time-varying activation coefficient for each
 * synergy.
 *
 * <p>Two update methods are available, selected by {@link #setUpdate}:
 *
 * <ul> <li>{@link Update#ANLS} (the default) performs alternating
 * non-negative least squares: with {@code W} fixed, each column of {@code H}
 * is the solution to a non-negative least squares (NNLS) problem, and with
 * {@code H} fixed, each row of {@code W} is likewise. Each NNLS problem is
 * solved <i>exactly</i>, as a {@code k X k} linear complementarity problem
 * (LCP), using {@link DantzigLCPSolver}. This is exact block coordinate
 * descent: the residual decreases monotonically, and since {@code k} is
 * usually small the {@code k X k} solves are very fast.
 *
 * <li>{@link Update#MULTIPLICATIVE} uses the multiplicative update rules of
 * Lee and Seung. These are simpler but converge more slowly, and because a
 * factor entry which reaches zero can never become positive again, they can
 * stall at a non-stationary point. Provided mainly for comparison.  </ul>
 *
 * <p>Because the NMF problem is not convex, the result depends on the initial
 * values of {@code W} and {@code H}, which are set according to {@link
 * #setInitialization}. The default is {@link Init#NNDSVD}, the non-negative
 * double singular value decomposition of Boutsidis and Gallopoulos, which is
 * deterministic and generally converges faster than a random start.
 * Alternatively, {@link Init#RANDOM} may be used together with {@link
 * #setNumRestarts}, in which case the factorization is repeated and the result
 * with the lowest residual is retained.
 *
 * <p>The factorization is not unique: for any positive diagonal {@code D},
 * {@code W H = (W D)(inv(D) H)}. The factors are therefore ordered by
 * decreasing contribution and then normalized, as specified by {@link
 * #setNormalization}, so that results are comparable between runs. The default
 * scales each column of {@code W} so that its largest entry is 1.
 *
 * <p>Sample usage:
 * <pre>
 *    NMFactorization nmf = new NMFactorization();
 *    nmf.factor (V, 4);
 *    MatrixNd W = nmf.getW();  // synergy vectors
 *    MatrixNd H = nmf.getH();  // activation coefficients
 *    double vaf = nmf.getVAF();
 * </pre>
 *
 * <p>References:
 *
 * <p>D. D. Lee and H. S. Seung, "Algorithms for non-negative matrix
 * factorization", NIPS 13, 2001.
 *
 * <p>C. Boutsidis and E. Gallopoulos, "SVD based initialization: A head start
 * for nonnegative matrix factorization", Pattern Recognition 41(4), 2008.
 *
 * <p>H. Kim and H. Park, "Nonnegative matrix factorization based on
 * alternating nonnegativity constrained least squares", SIAM J. Matrix
 * Analysis and Applications 30(2), 2008.
 */
public class NMFactorization {

   /**
    * Method used to update the factors at each iteration.
    */
   public enum Update {

      /**
       * Alternating non-negative least squares, with each least squares
       * problem solved exactly as a {@code k X k} LCP.
       */
      ANLS,

      /**
       * Multiplicative update rules of Lee and Seung.
       */
      MULTIPLICATIVE
   };

   /**
    * Method used to initialize the factors.
    */
   public enum Init {

      /**
       * Non-negative double singular value decomposition (Boutsidis and
       * Gallopoulos). Deterministic. If the requested number of factors
       * exceeds {@code min(m,n)}, {@link #RANDOM} is used instead.
       */
      NNDSVD,

      /**
       * Random values uniformly distributed within the range of the entries
       * of {@code V}.
       */
      RANDOM
   };

   /**
    * Normalization applied to the factors once the factorization is complete.
    */
   public enum Normalization {

      /**
       * No normalization is applied.
       */
      NONE,

      /**
       * Each column of {@code W} is scaled so that its maximum entry is 1,
       * with the corresponding row of {@code H} scaled inversely.
       */
      UNITY_MAX,

      /**
       * Each column of {@code W} is scaled to unit Euclidean norm, with the
       * corresponding row of {@code H} scaled inversely.
       */
      UNITY_NORM
   };

   /**
    * Default value for the convergence tolerance.
    */
   public static final double DEFAULT_TOLERANCE = 1e-8;

   /**
    * Default value for the maximum number of iterations.
    */
   public static final int DEFAULT_MAX_ITERATIONS = 500;

   /**
    * Relative ridge term added to the diagonal of the {@code k X k} matrices
    * formed by the ANLS update, to guard against singularity when a factor
    * degenerates to zero. This is applied in addition to any regularization
    * requested with {@link #setRegularization}.
    */
   protected static final double RIDGE_EPS = 1e-12;

   /**
    * Value used by the multiplicative update to guard against division by
    * zero.
    */
   protected static final double DIVIDE_EPS = 1e-16;

   protected double myTol = DEFAULT_TOLERANCE;
   protected int myMaxIterations = DEFAULT_MAX_ITERATIONS;
   protected double myLambda = 0;
   protected Update myUpdate = Update.ANLS;
   protected Init myInit = Init.NNDSVD;
   protected Normalization myNormalization = Normalization.UNITY_MAX;
   protected int myNumRestarts = 1;
   protected Random myRandom = null;

   // results
   protected MatrixNd myW;
   protected MatrixNd myH;
   protected double myResidual;
   protected double myVNorm;
   protected int myIterationCount;
   protected boolean myInitializedP = false;

   // working storage
   protected MatrixNd myV = new MatrixNd();
   protected MatrixNd myWtW = new MatrixNd();
   protected MatrixNd myWtV = new MatrixNd();
   protected MatrixNd myHHt = new MatrixNd();
   protected MatrixNd myVHt = new MatrixNd();
   protected MatrixNd myPrd = new MatrixNd();
   protected MatrixNd myRes = new MatrixNd();
   // ridged copy of the k X k matrix used by the ANLS solves; kept separate so
   // that myWtW and myHHt remain unmodified for the residual computation
   protected MatrixNd myKK = new MatrixNd();
   protected VectorNd myQvec = new VectorNd();
   protected VectorNd myZvec = new VectorNd();
   protected VectorNi myState = new VectorNi();
   protected DantzigLCPSolver mySolver;

   protected int myNumRows;
   protected int myNumCols;
   protected int myNumFactors;

   /**
    * Creates a new, uninitialized NMFactorization.
    */
   public NMFactorization() {
   }

   /**
    * Creates a new NMFactorization and uses it to factor the matrix {@code V}
    * into {@code numFactors} factors.
    *
    * @param V matrix to be factored. Must be non-negative.
    * @param numFactors number of factors
    */
   public NMFactorization (Matrix V, int numFactors) {
      factor (V, numFactors);
   }

   /**
    * Queries the convergence tolerance for this factorization.
    *
    * @return convergence tolerance
    * @see #setTolerance
    */
   public double getTolerance() {
      return myTol;
   }

   /**
    * Sets the convergence tolerance for this factorization. Iteration stops
    * when the decrease in the relative residual (as returned by {@link
    * #getResidual}) between successive iterations falls below this value. The
    * default value is {@link #DEFAULT_TOLERANCE}.
    *
    * @param tol convergence tolerance
    */
   public void setTolerance (double tol) {
      myTol = tol;
   }

   /**
    * Queries the maximum number of iterations for this factorization.
    *
    * @return maximum number of iterations
    * @see #setMaxIterations
    */
   public int getMaxIterations() {
      return myMaxIterations;
   }

   /**
    * Sets the maximum number of iterations for this factorization. The default
    * value is {@link #DEFAULT_MAX_ITERATIONS}.
    *
    * @param maxi maximum number of iterations
    */
   public void setMaxIterations (int maxi) {
      myMaxIterations = maxi;
   }

   /**
    * Queries the regularization term for this factorization.
    *
    * @return regularization term
    * @see #setRegularization
    */
   public double getRegularization() {
      return myLambda;
   }

   /**
    * Sets an L2 regularization term {@code lambda} for this factorization, so
    * that the quantity being minimized becomes
    * <pre>
    * ||V - W H||^2 + lambda (||W||^2 + ||H||^2)
    * </pre>
    * where all norms are Frobenius norms. This is used only by the {@link
    * Update#ANLS} update. Regularization can improve conditioning when factors
    * are nearly collinear, at the cost of a slightly larger residual. The
    * default value is 0.
    *
    * @param lambda regularization term. Must not be negative.
    */
   public void setRegularization (double lambda) {
      if (lambda < 0) {
         throw new IllegalArgumentException (
            "regularization must not be negative");
      }
      myLambda = lambda;
   }

   /**
    * Queries the update method used by this factorization.
    *
    * @return update method
    * @see #setUpdate
    */
   public Update getUpdate() {
      return myUpdate;
   }

   /**
    * Sets the update method used by this factorization. The default value is
    * {@link Update#ANLS}.
    *
    * @param update update method
    */
   public void setUpdate (Update update) {
      myUpdate = update;
   }

   /**
    * Queries the initialization method used by this factorization.
    *
    * @return initialization method
    * @see #setInitialization
    */
   public Init getInitialization() {
      return myInit;
   }

   /**
    * Sets the initialization method used by this factorization. The default
    * value is {@link Init#NNDSVD}.
    *
    * @param init initialization method
    */
   public void setInitialization (Init init) {
      myInit = init;
   }

   /**
    * Queries the normalization applied to the factors.
    *
    * @return factor normalization
    * @see #setNormalization
    */
   public Normalization getNormalization() {
      return myNormalization;
   }

   /**
    * Sets the normalization applied to the factors once the factorization is
    * complete. The default value is {@link Normalization#UNITY_MAX}.
    *
    * @param normalization factor normalization
    */
   public void setNormalization (Normalization normalization) {
      myNormalization = normalization;
   }

   /**
    * Queries the number of restarts used by this factorization.
    *
    * @return number of restarts
    * @see #setNumRestarts
    */
   public int getNumRestarts() {
      return myNumRestarts;
   }

   /**
    * Sets the number of times the factorization should be restarted, with the
    * result having the lowest residual being retained. Since the NMF problem
    * is not convex, restarting can help avoid poor local minima. Restarts
    * after the first use random initialization (regardless of the setting of
    * {@link #setInitialization}), since repeating a deterministic
    * initialization would simply repeat the same result. The default value is
    * 1.
    *
    * @param nrestarts number of restarts. Must be at least 1.
    */
   public void setNumRestarts (int nrestarts) {
      if (nrestarts < 1) {
         throw new IllegalArgumentException (
            "number of restarts must be at least 1");
      }
      myNumRestarts = nrestarts;
   }

   /**
    * Queries the random generator used for random initialization.
    *
    * @return random generator, or {@code null} if the default generator
    * of {@link RandomGenerator} is being used
    * @see #setRandomGenerator
    */
   public Random getRandomGenerator() {
      return myRandom;
   }

   /**
    * Sets the random generator used for random initialization. If set to
    * {@code null} (the default), the generator supplied by {@link
    * RandomGenerator} is used. Setting an explicitly seeded generator makes
    * randomly initialized factorizations repeatable.
    *
    * @param generator random generator to use, or {@code null}
    */
   public void setRandomGenerator (Random generator) {
      myRandom = generator;
   }

   /**
    * Factors the non-negative {@code m X n} matrix {@code V} into the product
    * of an {@code m X k} matrix {@code W} and a {@code k X n} matrix {@code
    * H}, both non-negative, where {@code k} is given by {@code numFactors}.
    *
    * @param V matrix to be factored. All entries must be non-negative.
    * @param numFactors number {@code k} of factors. Must be at least 1 and no
    * greater than {@code min(m,n)}.
    */
   public void factor (Matrix V, int numFactors) {
      myInitializedP = false;

      int m = V.rowSize();
      int n = V.colSize();
      if (numFactors < 1) {
         throw new IllegalArgumentException (
            "number of factors must be at least 1");
      }
      if (numFactors > Math.min (m, n)) {
         throw new IllegalArgumentException (
            "number of factors "+numFactors+
            " exceeds min matrix dimension "+Math.min(m,n));
      }
      myV.set (V);
      double[] vbuf = myV.getBuffer();
      int vw = myV.getBufferWidth();
      int vb = myV.getBufferBase();
      for (int i=0; i<m; i++) {
         for (int j=0; j<n; j++) {
            if (!(vbuf[i*vw+j+vb] >= 0)) {
               // '!(x >= 0)' also catches NaN
               throw new IllegalArgumentException (
                  "matrix entry ("+i+","+j+") is negative or NaN");
            }
         }
      }
      myNumRows = m;
      myNumCols = n;
      myNumFactors = numFactors;
      myVNorm = myV.frobeniusNorm();

      myWtW.setSize (numFactors, numFactors);
      myWtV.setSize (numFactors, n);
      myHHt.setSize (numFactors, numFactors);
      myVHt.setSize (m, numFactors);
      myQvec.setSize (numFactors);
      myZvec.setSize (numFactors);
      myState.setSize (numFactors);

      MatrixNd W = new MatrixNd (m, numFactors);
      MatrixNd H = new MatrixNd (numFactors, n);

      MatrixNd bestW = null;
      MatrixNd bestH = null;
      double bestRes = Double.POSITIVE_INFINITY;
      int bestCnt = 0;

      for (int restart=0; restart<myNumRestarts; restart++) {
         Init init = (restart == 0 ? myInit : Init.RANDOM);
         initializeFactors (W, H, init);
         double res = doFactor (W, H);
         if (res < bestRes) {
            bestRes = res;
            bestW = new MatrixNd (W);
            bestH = new MatrixNd (H);
            bestCnt = myIterationCount;
         }
      }
      myW = bestW;
      myH = bestH;
      myResidual = bestRes;
      myIterationCount = bestCnt;
      orderFactors (myW, myH);
      normalizeFactors (myW, myH);
      myInitializedP = true;
   }

   /**
    * Performs the iteration, starting from the values currently in {@code W}
    * and {@code H}, and returns the resulting relative residual. Also sets
    * myIterationCount.
    */
   protected double doFactor (MatrixNd W, MatrixNd H) {
      if (mySolver == null && myUpdate == Update.ANLS) {
         mySolver = new DantzigLCPSolver();
         mySolver.setWarmStartEnabled (true);
      }
      double prevRes = Double.POSITIVE_INFINITY;
      double res = Double.POSITIVE_INFINITY;
      int icnt = 0;
      while (icnt < myMaxIterations) {
         if (myUpdate == Update.ANLS) {
            updateH_ANLS (W, H);
            updateW_ANLS (W, H);
         }
         else {
            updateH_Mul (W, H);
            updateW_Mul (W, H);
         }
         icnt++;
         res = computeResidual (W, H);
         if (prevRes - res <= myTol*Math.max (res, 1.0)) {
            break;
         }
         prevRes = res;
      }
      myIterationCount = icnt;
      return res;
   }

   /**
    * Computes the relative residual {@code ||V - W H||/||V||}.
    *
    * <p>The product {@code W H} is formed explicitly. It is tempting instead
    * to use the identity
    * <pre>
    * ||V - W H||^2 = ||V||^2 - 2 trace(W^T (V H^T)) + trace((W^T W)(H H^T))
    * </pre>
    * which costs only {@code O((m+n) k^2)} and can reuse {@code V H^T} and
    * {@code H H^T} from the update of {@code W}. However, as the factorization
    * converges the three terms cancel, limiting the accuracy of the result to
    * about {@code sqrt(machine epsilon)}, or roughly {@code 1e-8} in a
    * relative sense. That is enough to stall the convergence test on problems
    * which are close to exactly factorable. Forming the product costs {@code
    * O(m n k)}, but this is the same order as the {@code W^T V} and {@code V
    * H^T} products already formed by each update.
    */
   protected double computeResidual (MatrixNd W, MatrixNd H) {
      if (myVNorm == 0) {
         return 0;
      }
      myRes.mul (W, H);
      myRes.sub (myV);
      return myRes.frobeniusNorm()/myVNorm;
   }

   /**
    * Adds a ridge term to the diagonal of the {@code k X k} matrix {@code M}
    * formed by an ANLS update, consisting of the requested regularization plus
    * a small relative term to guard against singularity.
    */
   protected void addRidge (MatrixNd M) {
      int k = M.rowSize();
      double trace = 0;
      for (int i=0; i<k; i++) {
         trace += M.get (i, i);
      }
      M.addDiagonal (myLambda + RIDGE_EPS*trace/k);
   }

   /**
    * Clamps any negative entries of {@code z} to zero. The LCP solution is
    * non-negative by construction, but basic variables are computed from a
    * Cholesky solve and so can emerge very slightly negative through roundoff.
    */
   protected void clampNonNegative (VectorNd z) {
      for (int i=0; i<z.size(); i++) {
         if (z.get(i) < 0) {
            z.set (i, 0);
         }
      }
   }

   /**
    * With {@code W} fixed, sets each column {@code h} of {@code H} to the
    * solution of the non-negative least squares problem
    * <pre>
    * min ||W h - v||^2,  h &gt;= 0
    * </pre>
    * where {@code v} is the corresponding column of {@code V}. The optimality
    * conditions for this are the {@code k X k} LCP
    * <pre>
    * w = (W^T W) h - W^T v,   w &gt;= 0,  h &gt;= 0,  w h = 0
    * </pre>
    */
   protected void updateH_ANLS (MatrixNd W, MatrixNd H) {
      myWtW.mulTransposeLeft (W, W);
      myKK.set (myWtW);
      addRidge (myKK);
      myWtV.mulTransposeLeft (W, myV);
      myState.setZero();
      for (int j=0; j<myNumCols; j++) {
         myWtV.getColumn (j, myQvec);
         myQvec.negate();
         if (mySolver.solve (
                myZvec, myState, myKK, myQvec) == LCPSolver.Status.SOLVED) {
            clampNonNegative (myZvec);
            H.setColumn (j, myZvec);
         }
         // otherwise leave the column at its previous value
      }
   }

   /**
    * With {@code H} fixed, sets each row {@code w} of {@code W} to the
    * solution of the non-negative least squares problem
    * <pre>
    * min ||H^T w - v||^2,  w &gt;= 0
    * </pre>
    * where {@code v} is the corresponding row of {@code V}. As in {@link
    * #updateH_ANLS}, this is a {@code k X k} LCP, here with matrix {@code H
    * H^T} and offset {@code -(V H^T)} row.
    */
   protected void updateW_ANLS (MatrixNd W, MatrixNd H) {
      myHHt.mulTransposeRight (H, H);
      myKK.set (myHHt);
      addRidge (myKK);
      myVHt.mulTransposeRight (myV, H);
      myState.setZero();
      for (int i=0; i<myNumRows; i++) {
         myVHt.getRow (i, myQvec);
         myQvec.negate();
         if (mySolver.solve (
                myZvec, myState, myKK, myQvec) == LCPSolver.Status.SOLVED) {
            clampNonNegative (myZvec);
            W.setRow (i, myZvec);
         }
         // otherwise leave the row at its previous value
      }
   }

   /**
    * Multiplicative update of {@code H}: {@code H <- H .* (W^T V)./(W^T W H)}.
    */
   protected void updateH_Mul (MatrixNd W, MatrixNd H) {
      myWtW.mulTransposeLeft (W, W);
      myWtV.mulTransposeLeft (W, myV);
      myPrd.mul (myWtW, H);
      double[] hbuf = H.getBuffer();
      double[] nbuf = myWtV.getBuffer();
      double[] dbuf = myPrd.getBuffer();
      int hw = H.getBufferWidth();
      int nw = myWtV.getBufferWidth();
      int dw = myPrd.getBufferWidth();
      int hb = H.getBufferBase();
      int nb = myWtV.getBufferBase();
      int db = myPrd.getBufferBase();
      for (int i=0; i<myNumFactors; i++) {
         for (int j=0; j<myNumCols; j++) {
            hbuf[i*hw+j+hb] *=
               nbuf[i*nw+j+nb]/(dbuf[i*dw+j+db] + DIVIDE_EPS);
         }
      }
   }

   /**
    * Multiplicative update of {@code W}: {@code W <- W .* (V H^T)./(W H H^T)}.
    */
   protected void updateW_Mul (MatrixNd W, MatrixNd H) {
      myHHt.mulTransposeRight (H, H);
      myVHt.mulTransposeRight (myV, H);
      myPrd.mul (W, myHHt);
      double[] wbuf = W.getBuffer();
      double[] nbuf = myVHt.getBuffer();
      double[] dbuf = myPrd.getBuffer();
      int ww = W.getBufferWidth();
      int nw = myVHt.getBufferWidth();
      int dw = myPrd.getBufferWidth();
      int wb = W.getBufferBase();
      int nb = myVHt.getBufferBase();
      int db = myPrd.getBufferBase();
      for (int i=0; i<myNumRows; i++) {
         for (int j=0; j<myNumFactors; j++) {
            wbuf[i*ww+j+wb] *=
               nbuf[i*nw+j+nb]/(dbuf[i*dw+j+db] + DIVIDE_EPS);
         }
      }
   }

   /**
    * Sets initial values for {@code W} and {@code H}.
    */
   protected void initializeFactors (MatrixNd W, MatrixNd H, Init init) {
      if (init == Init.NNDSVD) {
         initializeNNDSVD (W, H);
      }
      else {
         initializeRandom (W, H);
      }
   }

   protected Random getRandom() {
      return myRandom != null ? myRandom : RandomGenerator.get();
   }

   /**
    * Sets {@code W} and {@code H} to random values uniformly distributed
    * within the range of the entries of {@code V}, scaled so that the product
    * {@code W H} has roughly the same magnitude as {@code V}.
    */
   protected void initializeRandom (MatrixNd W, MatrixNd H) {
      Random rand = getRandom();
      double avg = Math.sqrt (meanValue()/myNumFactors);
      if (avg == 0) {
         avg = 1.0;
      }
      setRandom (W, 2*avg, rand);
      setRandom (H, 2*avg, rand);
   }

   private void setRandom (MatrixNd M, double max, Random rand) {
      double[] buf = M.getBuffer();
      int w = M.getBufferWidth();
      int b = M.getBufferBase();
      for (int i=0; i<M.rowSize(); i++) {
         for (int j=0; j<M.colSize(); j++) {
            buf[i*w+j+b] = max*rand.nextDouble();
         }
      }
   }

   /**
    * Returns the mean of the entries of {@code V}.
    */
   protected double meanValue() {
      double sum = 0;
      double[] vbuf = myV.getBuffer();
      int vw = myV.getBufferWidth();
      int vb = myV.getBufferBase();
      for (int i=0; i<myNumRows; i++) {
         for (int j=0; j<myNumCols; j++) {
            sum += vbuf[i*vw+j+vb];
         }
      }
      return sum/(myNumRows*myNumCols);
   }

   /**
    * Initializes {@code W} and {@code H} using the non-negative double
    * singular value decomposition (NNDSVD) of Boutsidis and Gallopoulos. The
    * leading singular triplet of {@code V} is non-negative (by
    * Perron-Frobenius) and is used directly; each subsequent triplet is split
    * into its positive and negative parts, and whichever part has the larger
    * outer product norm is retained.
    *
    * <p>Zero entries are replaced by a small positive value (the "NNDSVDa"
    * variant), since the multiplicative update can never make a zero entry
    * positive.
    */
   protected void initializeNNDSVD (MatrixNd W, MatrixNd H) {
      SVDecomposition svd = new SVDecomposition();
      svd.factor (myV);
      MatrixNd U = svd.getU();
      MatrixNd Vsvd = svd.getV();
      VectorNd sig = svd.getS();

      int k = myNumFactors;
      VectorNd u = new VectorNd (myNumRows);
      VectorNd v = new VectorNd (myNumCols);
      VectorNd up = new VectorNd (myNumRows);
      VectorNd un = new VectorNd (myNumRows);
      VectorNd vp = new VectorNd (myNumCols);
      VectorNd vn = new VectorNd (myNumCols);

      for (int j=0; j<k; j++) {
         U.getColumn (j, u);
         Vsvd.getColumn (j, v);
         double s = sig.get(j);
         if (j == 0) {
            // leading triplet is non-negative up to an overall sign
            positivePart (up, u);
            positivePart (un, u, /*negate=*/true);
            positivePart (vp, v);
            positivePart (vn, v, /*negate=*/true);
            if (up.norm()*vp.norm() >= un.norm()*vn.norm()) {
               setFactorPair (W, H, j, up, vp, s);
            }
            else {
               setFactorPair (W, H, j, un, vn, s);
            }
         }
         else {
            positivePart (up, u);
            positivePart (un, u, /*negate=*/true);
            positivePart (vp, v);
            positivePart (vn, v, /*negate=*/true);
            double mp = up.norm()*vp.norm();
            double mn = un.norm()*vn.norm();
            if (mp >= mn) {
               setFactorPair (W, H, j, up, vp, s*mp);
            }
            else {
               setFactorPair (W, H, j, un, vn, s*mn);
            }
         }
      }
      // replace zeros with a small positive value
      double fill = 0.01*meanValue();
      if (fill == 0) {
         fill = 1e-8;
      }
      fillZeros (W, fill);
      fillZeros (H, fill);
   }

   /**
    * Sets column {@code j} of {@code W} and row {@code j} of {@code H} from
    * the unit vectors {@code u} and {@code v}, scaled so that their outer
    * product has magnitude {@code s}.
    */
   private void setFactorPair (
      MatrixNd W, MatrixNd H, int j, VectorNd u, VectorNd v, double s) {
      double unrm = u.norm();
      double vnrm = v.norm();
      if (unrm == 0 || vnrm == 0 || s <= 0) {
         // degenerate; leave as zero and let fillZeros() handle it
         for (int i=0; i<myNumRows; i++) {
            W.set (i, j, 0);
         }
         for (int i=0; i<myNumCols; i++) {
            H.set (j, i, 0);
         }
         return;
      }
      double scale = Math.sqrt (s);
      for (int i=0; i<myNumRows; i++) {
         W.set (i, j, scale*u.get(i)/unrm);
      }
      for (int i=0; i<myNumCols; i++) {
         H.set (j, i, scale*v.get(i)/vnrm);
      }
   }

   private void positivePart (VectorNd res, VectorNd v1) {
      positivePart (res, v1, false);
   }

   private void positivePart (VectorNd res, VectorNd v1, boolean negate) {
      for (int i=0; i<v1.size(); i++) {
         double x = (negate ? -v1.get(i) : v1.get(i));
         res.set (i, x > 0 ? x : 0);
      }
   }

   private void fillZeros (MatrixNd M, double fill) {
      double[] buf = M.getBuffer();
      int w = M.getBufferWidth();
      int b = M.getBufferBase();
      for (int i=0; i<M.rowSize(); i++) {
         for (int j=0; j<M.colSize(); j++) {
            if (buf[i*w+j+b] == 0) {
               buf[i*w+j+b] = fill;
            }
         }
      }
   }

   /**
    * Reorders the factors by decreasing contribution, as measured by the
    * product of the norms of each column of {@code W} and the corresponding
    * row of {@code H}. This does not change the product {@code W H}.
    */
   protected void orderFactors (MatrixNd W, MatrixNd H) {
      int k = myNumFactors;
      double[] mag = new double[k];
      VectorNd w = new VectorNd (myNumRows);
      VectorNd h = new VectorNd (myNumCols);
      for (int j=0; j<k; j++) {
         W.getColumn (j, w);
         H.getRow (j, h);
         mag[j] = w.norm()*h.norm();
      }
      int[] perm = new int[k];
      for (int j=0; j<k; j++) {
         perm[j] = j;
      }
      // insertion sort into order of decreasing magnitude
      for (int j=1; j<k; j++) {
         int idx = perm[j];
         double m = mag[idx];
         int i = j-1;
         while (i >= 0 && mag[perm[i]] < m) {
            perm[i+1] = perm[i];
            i--;
         }
         perm[i+1] = idx;
      }
      boolean changed = false;
      for (int j=0; j<k; j++) {
         if (perm[j] != j) {
            changed = true;
            break;
         }
      }
      if (changed) {
         MatrixNd Wnew = new MatrixNd (myNumRows, k);
         MatrixNd Hnew = new MatrixNd (k, myNumCols);
         for (int j=0; j<k; j++) {
            W.getColumn (perm[j], w);
            H.getRow (perm[j], h);
            Wnew.setColumn (j, w);
            Hnew.setRow (j, h);
         }
         W.set (Wnew);
         H.set (Hnew);
      }
   }

   /**
    * Applies the normalization specified by {@link #setNormalization}. This
    * does not change the product {@code W H}.
    */
   protected void normalizeFactors (MatrixNd W, MatrixNd H) {
      if (myNormalization == Normalization.NONE) {
         System.out.println("No normalization applied");
         return;
      }
      VectorNd w = new VectorNd (myNumRows);
      VectorNd h = new VectorNd (myNumCols);
      for (int j=0; j<myNumFactors; j++) {
         W.getColumn (j, w);
         double scale;
         if (myNormalization == Normalization.UNITY_MAX) {
            scale = w.maxElement();
         }
         else {
            scale = w.norm();
         }
         if (scale > 0) {
            H.getRow (j, h);
            w.scale (1/scale);
            h.scale (scale);
            W.setColumn (j, w);
            H.setRow (j, h);
         }
      }
   }

   /**
    * Returns the {@code m X k} factor {@code W}. The returned matrix is
    * internal to this factorization and should not be modified.
    *
    * @return factor {@code W}
    */
   public MatrixNd getW() {
      checkInitialized();
      return myW;
   }

   /**
    * Returns the {@code k X n} factor {@code H}. The returned matrix is
    * internal to this factorization and should not be modified.
    *
    * @return factor {@code H}
    */
   public MatrixNd getH() {
      checkInitialized();
      return myH;
   }

   /**
    * Returns the number of factors {@code k} for this factorization.
    *
    * @return number of factors
    */
   public int getNumFactors() {
      checkInitialized();
      return myNumFactors;
   }

   /**
    * Returns the relative residual {@code ||V - W H||/||V||} of this
    * factorization, where the norms are Frobenius norms.
    *
    * @return relative residual
    */
   public double getResidual() {
      checkInitialized();
      return myResidual;
   }

   /**
    * Returns the variance accounted for (VAF) by this factorization, defined
    * by
    * <pre>
    * VAF = 1 - ||V - W H||^2/||V||^2
    * </pre>
    * This is the measure conventionally used to select the number of factors
    * when extracting muscle synergies.
    *
    * @return variance accounted for
    */
   public double getVAF() {
      checkInitialized();
      return 1.0 - myResidual*myResidual;
   }

   /**
    * Returns the number of iterations used by this factorization. If restarts
    * were requested, this is the number used by the restart which produced the
    * retained result.
    *
    * @return number of iterations
    */
   public int getIterationCount() {
      checkInitialized();
      return myIterationCount;
   }

   /**
    * Returns the reconstruction {@code W H} of the matrix which was factored.
    *
    * @return reconstruction of the factored matrix
    */
   public MatrixNd getReconstruction() {
      checkInitialized();
      MatrixNd P = new MatrixNd (myNumRows, myNumCols);
      P.mul (myW, myH);
      return P;
   }

   private void checkInitialized() {
      if (!myInitializedP) {
         throw new ImproperStateException ("NMFactorization not initialized");
      }
   }

   /**
    * Convenience method that factors {@code V} for each number of factors from
    * {@code minFactors} to {@code maxFactors} and returns the resulting VAF
    * values, as described for {@link #getVAF}. The number of synergies to use
    * is conventionally taken as the smallest for which the VAF exceeds some
    * threshold (typically 0.9 to 0.95).
    *
    * @param V matrix to be factored. Must be non-negative.
    * @param minFactors minimum number of factors. Must be at least 1 and no
    * greater than {@code maxFactors}.
    * @param maxFactors maximum number of factors
    * @return VAF values, with the value for {@code k} factors stored at
    * location {@code k-minFactors}
    */
   public double[] computeVAFs (Matrix V, int minFactors, int maxFactors) {
      if (minFactors < 1) {
         throw new IllegalArgumentException (
            "minFactors must be at least 1");
      }
      if (minFactors > maxFactors) {
         throw new IllegalArgumentException (
            "minFactors "+minFactors+" exceeds maxFactors "+maxFactors);
      }
      double[] vafs = new double[maxFactors-minFactors+1];
      for (int k=minFactors; k<=maxFactors; k++) {
         factor (V, k);
         vafs[k-minFactors] = getVAF();
      }
      return vafs;
   }
}
