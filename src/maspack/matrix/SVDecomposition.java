/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Constructs the singular value decomposition (SVD) of a matrix. This takes the
 * form <br>
 * M = U S V' <br>
 * where M is the original matrix, U and V are orthogonal matrices, V' indicates
 * the transpose of V, and S is a diagonal matrix. Once an SVD has been
 * constructed, it can be used to perform various computations related to M,
 * such as solving equations, computing the determinant, or estimating the
 * condition number.
 * 
 * <p>
 * Providing a separate class for the SVD allows an application to perform such
 * decompositions repeatedly without having to reallocate temporary storage
 * space.
 * 
 * <p> Note: by default, this performs a "thin" SVD, where U and V are not
 * necessarily square matrices if the input is not square.  To enable a full
 * SVD, such as when necessary for computing null-spaces of non-square
 * matrices, then factor the matrix using the flag {@link #FULL_UV}.
 */
public class SVDecomposition {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private int m, n;
   private int mind; // min (m,n)
   private int maxd; // max (m,n)

   private double etol = 100 * DOUBLE_PREC;

   private double sigmax;
   private double sigmin;
   private double sigdet;

   private boolean initialized = false;
   private boolean transposedSolution = false;

   private double[] buf = new double[0];
   private double[] vec = new double[0];
   private double[] wec = new double[0];
   private int[] sigIndices = new int[0];
   // col and row beta hold the beta values for the HouseHolder vectors
   // used to bidiagonalize the matrix
   private double[] colBeta = new double[0];
   private double[] rowBeta = new double[0];

   private VectorNd sig = new VectorNd();
   private MatrixNd U_ = new MatrixNd (0, 0);
   private MatrixNd V_ = new MatrixNd (0, 0);
   private MatrixNd B_ = new MatrixNd (0, 0);

   private VectorNd vtmp = new VectorNd (0);
   private VectorNd btmp = new VectorNd (0);
   private VectorNd xtmp = new VectorNd (0);

   private double S;
   private double C;

   /**
    * Specifies that matrix U should not be computed.
    */
   public static final int OMIT_U = 0x1;

   /**
    * Specifies that matrix V should not be computed.
    */
   public static final int OMIT_V = 0x2;
   
   /**
    * Specifies that neither matrix U nor matrix V should not be computed.
    */
   public static final int OMIT_UV = OMIT_U | OMIT_V;
   
   /**
    * Specifies to compute the full SVD decomposition, otherwise only a
    * 'thin' decomposition is computed for non-square matrices
    */
   public static final int FULL_UV = 0x04;

   /**
    * The default iteration limit for computing the SVD.
    */
   public static final int DEFAULT_ITERATION_LIMIT = 10;

   private int iterLimit = DEFAULT_ITERATION_LIMIT;

   /**
    * Gets the iteration limit for SVD computations. The actual number of
    * iterations allowed is this limit value times the minimum dimension of the
    * matrix.
    * 
    * @return iteration limit
    * @see #setIterationLimit
    */
   public int getIterationLimit() {
      return iterLimit;
   }

   /**
    * Sets the iteration limit for SVD computations. The actual number of
    * iterations allowed is this limit value times the minimum dimension of the
    * matrix.
    * 
    * 
    * @param lim
    * iteration limit
    * @see #getIterationLimit
    */
   public void setIterationLimit (int lim) {
      iterLimit = lim;
   }

   /**
    * Creates an SVDecomposition and initializes it to the SVD for the matrix
    * M.
    * 
    * @param M
    * matrix to perform the SVD on
    */
   public SVDecomposition (Matrix M) {
      factor (M);
   }

   /**
    * Creates an SVDecomposition and initializes it to the SVD for the matrix
    * M. Computation of U and/or V may be omitted by specifying the flags
    * {@link #OMIT_U} and/or {@link #OMIT_V}.
    * 
    * @param M
    * matrix to perform the SVD on
    * @param flags
    * flags controlling the factorization
    */
   public SVDecomposition (Matrix M, int flags) {
      factor (M, flags);
   }

   /**
    * Creates an uninitialized SVDecomposition.
    */
   public SVDecomposition() {
   }

   private void givens (double a, double b) {
      double tau;

      if (b == 0) {
         C = 1;
         S = 0;
      }
      else {
         if (ABS (b) > ABS (a)) {
            tau = -a / b;
            S = 1 / Math.sqrt (1 + tau * tau);
            C = S * tau;
         }
         else {
            tau = -b / a;
            C = 1 / Math.sqrt (1 + tau * tau);
            S = C * tau;
         }
      }
   }

   private void givensCol (MatrixNd Q, int k, int l) {
      // post-multiples A by a Givens rotation
      // 
      // [ c s  ]
      // [      ]
      // [ -s c ]
      //

      double[] buf = Q.buf;
      int w = Q.width;
      double ak, al;
      int i;

      for (i = 0; i < Q.nrows; i++) {
         ak = buf[(i) * w + k];
         al = buf[(i) * w + l];
         buf[(i) * w + k] = ak * C - al * S;
         buf[(i) * w + l] = ak * S + al * C;
      }
   }

   /**
    * Peforms an SVD on the Matrix M. 
    * 
    * @param M
    * matrix to perform the SVD on
    */
   public void factor (Matrix M) {
      factor (M, /*flags=*/0);
   }

   /**
    * Peforms an SVD on the Matrix M. Computation of the matrices U and/or V
    * may be omitted by specifying the flags {@link #OMIT_U} and/or {@link
    * #OMIT_V}.
    * 
    * @param M
    * matrix to perform the SVD on
    * @param
    * flags controlling the factorization.
    */
   public void factor (Matrix M, int flags) {
      // initialize the calculation

      m = M.rowSize();
      n = M.colSize();

      if (buf.length < m * n) {
         buf = new double[m * n];
      }
      if (m < n) {
         mind = m;
         maxd = n;
      }
      else {
         mind = n;
         maxd = m;
      }
      int sigLen = (flags & FULL_UV) == 0 ? mind : maxd;
      if (sigIndices.length < sigLen) {
         sigIndices = new int[sigLen];
      }
      sig = new VectorNd(mind);
      double[] sbuf = sig.getBuffer();
      if (vec.length < maxd) {
         vec = new double[maxd];
         wec = new double[maxd];
      }
      int maxIter = mind * iterLimit;
      vtmp.setSize (mind);
      btmp.setSize (m);
      xtmp.setSize (n);
      if ((flags & OMIT_U) == 0) {
         if( (flags & FULL_UV) == 0) {
            U_ = new MatrixNd (m, mind);
         } else {
            U_ = new MatrixNd (m, m);
         }
         U_.setIdentity();
      }
      else {
         U_ = null;
      }
      if ((flags & OMIT_V) == 0) {
         if ((flags & FULL_UV) == 0) {
            V_ = new MatrixNd (n, mind);
         } else {
            V_ = new MatrixNd (n, n);
         }
         V_.setIdentity();
      }
      else {
         V_ = null;
      }
      
      if (m >= n) {
         M.get (buf);
         transposedSolution = false;
      }
      else { // set buffer to the transpose of M
         if (M instanceof MatrixNd) {
            MatrixNd X = (MatrixNd)M;
            double[] mbuf = X.buf;
            for (int i = 0; i < n; i++) {
               for (int j = 0; j < m; j++) {
                  buf[i * m + j] = mbuf[j * X.width + i + X.base];
               }
            }
         }
         else {
            for (int i = 0; i < n; i++) {
               for (int j = 0; j < m; j++) {
                  buf[i * m + j] = M.get (j, i);
               }
            }
         }
         swapUandV();
         transposedSolution = true;
      }
      B_.setBuffer (m, n, buf, n);
      bidiagonalize (B_);

      if (U_ != null) {
         houseRowAccum (U_, buf, m, n);
      }
      if (V_ != null) {
         houseColAccum (V_, buf, n);
         V_.transpose();
      }

      // estimate matrix norm
      double anorm = 0;
      for (int i = 0; i < n - 1; i++) {
         anorm = MAX (anorm, ABS (buf[i * m + i]));
         anorm = MAX (anorm, ABS (buf[i * m + i + 1]));
      }
      anorm = MAX (anorm, ABS (buf[(n - 1) * m + (n - 1)]));

      int kd; // starting index of lower diagonal
      int kb; // starting index of lowest full-bidiagonal
      int icnt = 0;
      int i, j, w;

      w = n;
      kd = n;
      while (kd != 0) {
         // zero small super diagonal terms
         for (i = 0; i < kd - 1; i++) {
            if (ABS (buf[(i) * w + i + 1]) < etol
            * (ABS (buf[(i) * w + i]) + ABS (buf[(i + 1) * w + i + 1]))) {
               buf[(i) * w + i + 1] = 0;
            }
         }
         while (kd > 0) {
            if (kd > 1 && buf[(kd - 2) * w + kd - 1] != 0) {
               break;
            }
            kd--;
         }
         if (kd > 0) {
            kb = kd - 2;
            // zerod = 0;
            // if (n == 3 && m == 3)
            // { Matrix3d B = new Matrix3d();
            // B.set (buf);
            // System.out.println ("B=\n" + B.toString("%24g"));
            // }
            // while (kb > 0 && buf[(kb)*w+kb] != 0 && buf[(kb-1)*w+kb] != 0)
            // { kb--;
            // }
            // if (anorm + buf[(kb)*w+kb] == anorm)
            // { buf[(kb)*w+kb] = 0;

            while (kb > 0) {
               if (anorm + buf[(kb) * w + kb] == anorm) // buf(kb,kb) == 0 ?
               {
                  buf[(kb) * w + kb] = 0;
                  break;
               }
               else if (buf[(kb - 1) * w + kb] == 0) {
                  break;
               }
               else {
                  kb--;
               }
            }
            if (kb == 0 && anorm + buf[(kb) * w + kb] == anorm) {
               buf[(kb) * w + kb] = 0;
            }
            if (buf[(kb) * w + kb] == 0) {
               zeroRow (kb * w + kb, kb, kd - kb - 1, n);
            }
            else {
               takeStep (kb * w + kb, kb, kd - kb, n);
            }
         }
         if (++icnt >= maxIter) {
            throw new NumericalException (
               "SVD failed to converge after " + icnt + " iterations");
         }
      }

      double s;

      sigdet = 1;
      // start by setting the sign of the determinant according to
      // the number of non-zero householder reflections
      if (n > 1) {
         for (j=0; j<n-1; j++) {
            if (colBeta[j] != 0) {
               sigdet = -sigdet;
            }
         }
         for (i=0; i<n-2; i++) {
            if (rowBeta[i] != 0) {
               sigdet = -sigdet;
            }
         }
      }
      for (j = 0; j < n; j++) {
         s = buf[j * n + j];
         sigdet *= s;
         if (s < 0) {
            s = -s;
            if (V_ != null) {
               int vw = V_.width;
               for (i = 0; i < n; i++) {
                  V_.buf[i * vw + j] = -V_.buf[i * vw + j];
               }
            }
         }
         sbuf[j] = s;
         sigIndices[j] = j;
      }
      for (j = n; j < sigLen; ++j) {
         sigIndices[j] = j;
      }

      // bubble sort
      for (i = 0; i < n - 1; i++) {
         for (j = i + 1; j < n; j++) {
            if (sbuf[i] < sbuf[j]) {
               double tmp = sbuf[i];
               sbuf[i] = sbuf[j];
               sbuf[j] = tmp;
               int idx = sigIndices[i];
               sigIndices[i] = sigIndices[j];
               sigIndices[j] = idx;
            }
         }
      }

      // check to see if sort was necessary
      boolean orderChanged = false;
      for (j = 0; j < n; j++) {
         if (sigIndices[j] != j) {
            orderChanged = true;
            break;
         }
      }
      if (orderChanged) { // System.out.println ("order changed");
         if (U_ != null) {
            U_.permuteColumns (sigIndices);
         }
         if (V_ != null) {
            V_.permuteColumns (sigIndices);
         }
      }
      sigmin = sbuf[mind - 1];
      sigmax = sbuf[0];

      if (transposedSolution) {
         swapUandV();
      }
      initialized = true;
   }

   private void zeroRow (int base, int ib, int p, int w) {
      // A has the following form,
      //	
      // |< p >|< q >|
      // 0 X
      // X X
      // X X
      // X
      // X
      // X
      // X
      //
      // and so we want to pre-multiply it by a series
      // of givens rotations so as to zero the top row.
      // It is assumed that p is non-zero.
      //
      // At each step, the critical elements of the
      // iteration are identified as follows:
      //
      // + 0 al1 al2
      // . . ==
      // X X ak1 ak2
      //

      // System.out.println ("zeroRow " + base + " " + ib+" "+p+" "+w);

      double al1, ak1, ak2;
      double al1next;
      int k;

      al1next = buf[(0) * w + 1 + base];
      for (k = 1; k <= p; k++) {
         al1 = al1next;
         ak1 = buf[(k) * w + k + base];
         givens (ak1, al1);
         if (U_ != null) {
            givensCol (U_, k + ib, ib);
         }
         buf[(k) * w + k + base] = ak1 * C - al1 * S;
         if (k < p) {
            ak2 = buf[(k) * w + k + 1 + base];
            buf[(k) * w + k + 1 + base] = ak2 * C;
            al1next = ak2 * S;
         }
      }
      buf[(0) * w + 1 + base] = 0;
   }

   private void swapUandV() {
      MatrixNd T;
      int d;

      d = n;
      n = m;
      m = d;

      T = U_;
      U_ = V_;
      V_ = T;
   }

   private void takeStep (int base, int ib, int nn, int w) {
      double bmm, bmn, bnn, blm;
      double tmm, tmn, tnn;
      double b11, b12;
      double d, mu;
      // double y, z;
      int k;

      blm = (nn >= 3 ? buf[(nn - 3) * w + nn - 2 + base] : 0);
      bmm = buf[(nn - 2) * w + nn - 2 + base];
      bmn = buf[(nn - 2) * w + nn - 1 + base];
      bnn = buf[(nn - 1) * w + nn - 1 + base];

      tmm = blm * blm + bmm * bmm;
      tmn = bmm * bmn;
      tnn = bmn * bmn + bnn * bnn;

      b11 = buf[(0) * w + 0 + base];
      b12 = buf[(0) * w + 1 + base];

      d = (tmm - tnn) / 2;
      mu = tnn - tmn * tmn / (d + SGN (d) * Math.sqrt (d * d + tmn * tmn));

      // y = b11*b11-mu;
      // z = b11*b12;

      // At every step of the iteration, we are concerned with the
      // following elements:
      //
      // bk01 bk02 M(k-1,k) M(k-1,k+1)
      // bk11 bk12 bk13 == M(k,k) M(k,k+1) M(k,k+2)
      // bk21 bk22 bk23 M(k+1,k) M(k+1,k+1) M(k+1,k+2)
      // 
      // The situtation at the beginning of the loop, with k=0,
      // followed by the first and second givens multiplies, looks like:
      //
      // - - - - - -
      // X X 0 X X 0 X X +
      // 0 X X + X X 0 X X
      //
      // The situtation for subsequent k>0 looks like:
      //
      // X + X 0 X 0
      // X X 0 X X 0 X X +
      // 0 X X + X X 0 X X
      // 
      // and the situation for k == nn-2 looks like
      //	   
      // X + X 0 X 0
      // X X - X X - X X -
      // 0 X - + X - 0 X -

      double bk01, bk02;
      double bk11, bk12, bk13;
      double bk21, bk22, bk23;
      double btmp;

      bk01 = bk02 = bk21 = 0;
      bk11 = buf[(0) * w + 0 + base];
      bk12 = buf[(0) * w + 1 + base];
      bk22 = buf[(1) * w + 1 + base];
      if (ib + 2 >= w) {
         bk23 = 0;
      }
      else {
         bk23 = buf[(1) * w + 2 + base];
      }

      for (k = 0; k < nn - 1; k++) {
         if (k == 0) {
            givens (b11 * b11 - mu, b11 * b12);
            // printf ("mu=%18.15e %18.15e %18.15e\n", mu, b11*b11-mu, b11*b12);
         }
         else {
            givens (bk01, bk02);
         }
         // printf ("%d A: C=%18.15e S=%18.15e\n", k, C, S);
         // printf ("bk11=%13.8e bk12=%13.8e\n", bk11, bk12);
         bk01 = C * bk01 - S * bk02;
         btmp = C * bk11 - S * bk12;
         // printf ("btmp=%13.8e\n", btmp);
         bk21 = -S * bk22;
         bk12 = S * bk11 + C * bk12;
         bk22 = C * bk22;
         bk11 = btmp;
         if (V_ != null) {
            givensCol (V_, ib + k, ib + k + 1);
         }

         if (k != 0) {
            buf[(k - 1) * w + k + base] = bk01;
         }
         givens (bk11, bk21);
         // printf ("%d B: C=%18.15e S=%18.15e\n", k, C, S);
         // printf ("bk11=%13.8e bk21=%13.8e\n", bk11, bk21);
         bk11 = C * bk11 - S * bk21;
         btmp = C * bk12 - S * bk22;
         bk13 = -S * bk23;
         bk22 = S * bk12 + C * bk22;
         bk23 = +C * bk23;
         bk12 = btmp;
         if (U_ != null) {
            givensCol (U_, ib + k, ib + k + 1);
         }

         buf[(k) * w + k + base] = bk11;
         buf[(k) * w + k + 1 + base] = bk12;
         buf[(k + 1) * w + k + 1 + base] = bk22;

         if (k == nn - 2) {
            buf[(k) * w + k + 1 + base] = bk12;
            buf[(k + 1) * w + k + 1 + base] = bk22;
         }
         else {
            bk01 = bk12;
            bk02 = bk13;
            bk11 = bk22;
            bk12 = bk23;
            bk22 = buf[(k + 2) * w + k + 2 + base];
            if (k < nn - 3) // was k < nn-2
            {
               bk23 = buf[(k + 2) * w + k + 3 + base];
               if ((k + 2) * w + k + 3 + base >= m * n) {
                  System.out.println ("!!! " + bk23);
               }
            }
            else {
               bk23 = 0;
            }
         }
      }
   }

   /**
    * Gets the matrices associated with the SVD.
    * 
    * @param U
    * If not <code>null</code>, returns the left-hand orthogonal matrix
    * @param svec
    * If not <code>null</code>, returns the diagonal elements of S
    * @param V
    * If not <code>null</code>, returns the right-hand orthogonal matrix
    * (note that this is V, and not it's transpose V^T).
    * @throws ImproperStateException
    * if this SVDecomposition is uninitialized, or if either U or V are requested
    * but were not computed
    * @throws ImproperSizeException
    * if U, svec, or V are not of the proper dimension and cannot be resized.
    */
   public void get (DenseMatrix U, Vector svec, DenseMatrix V) {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (V != null && V_ == null) {
         throw new ImproperStateException (
            "V requested but was not computed in the decomposition");
      }
      if (U != null && U_ == null) {
         throw new ImproperStateException (
            "U requested but was not computed in the decomposition");
      }
      
      if (U != null && (U.rowSize() != m || U.colSize() != U_.colSize ())) {
         if (!U.isFixedSize()) {
            U.setSize (m, U_.colSize ());
         }
         else {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
      }
      if (V != null && (V.rowSize() != n || V.colSize() != V_.colSize ())) {
         if (!V.isFixedSize()) {
            V.setSize (n, V_.colSize ());
         }
         else {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
      }
      if (svec != null && svec.size() != mind) {
         if (!svec.isFixedSize()) {
            svec.setSize (mind);
         }
         else {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
      }
      if (U != null) {
         U.set (U_);
      }
      if (V != null) {
         V.set (V_);
      }
      if (svec != null) {
         svec.set (sig);
      }
   }

   /**
    * Returns the current U matrix associated with this decomposition. If U was
    * not computed, returns <code>null</code>. Subsequent factorizations will
    * cause a different U to be created. The returned matrix should not be
    * modified if any subsequent calls are to be made which depend on U
    * (including solve and inverse methods).
    *
    * @return current U matrix
    * @throws ImproperStateException
    * if this SVDecomposition is uninitialized
    */
   public MatrixNd getU() {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      return U_;
   }

   /**
    * Returns the current V matrix associated with this decomposition. If V was
    * not computed, returns <code>null</code>. Subsequent factorizations will
    * cause a different V to be created. The returned matrix should not be
    * modified if any subsequent calls are to be made which depend on V
    * (including solve and inverse methods).
    *
    * @return current V matrix
    * @throws ImproperStateException
    * if this SVDecomposition is uninitialized
    */
   public MatrixNd getV() {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      return V_;
   }

   /**
    * Returns the current singular values associated with this
    * decomposition. Subsequent factorizations will cause a different vector to
    * be created. The returned vector should not be modified if any subsequent
    * calls are to be made which depend on it (including solve and inverse
    * methods).
    *
    * @return current singular values
    * @throws ImproperStateException
    * if this SVDecomposition is uninitialized
    */
   public VectorNd getS() {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      return sig;
   }

   /**
    * Convenience method that creates an SVDecomposition, factors it for the
    * matrix M, and stores the resulting U, S and V values into the
    * corresponding arguments. If the arguments U and/or V are specified
    * as <code>null</code>, then U and/or V will not be computed.
    *
    * @param U
    * If not <code>null</code>, returns the left-hand orthogonal matrix
    * @param svec
    * If not <code>null</code>, returns the diagonal elements of S
    * @param V
    * If not <code>null</code>, returns the right-hand orthogonal matrix
    * (note that this is V, and not it's transpose V^T).
    * @param M matrix to be factored
    * @return the resulting SVDecomposition
    */
   public static SVDecomposition factor (
      DenseMatrix U, Vector svec, DenseMatrix V, Matrix M) {
      int flags = 0;
      if (U == null) {
         flags |= OMIT_U;
      }
      if (V == null) {
         flags |= OMIT_V;
      }
      SVDecomposition svd = new SVDecomposition (M, flags);
      svd.get (U, svec, V);
      return svd;
   }

   /**
    * Computes the condition number of the original matrix M associated with
    * this SVD. This is simply the absolute value of the ratio of the maximum
    * and minimum singular values.
    * 
    * @return condition number
    * @throws ImproperStateException
    * if this SVDecomposition is uninitialized
    */
   public double condition() {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      return sigmax / sigmin;
   }

   /**
    * Computes the 2-norm of the original matrix M associated with this SVD.
    * This is simply the absolute value of the maximum singular value.
    * 
    * @return 2-norm
    * @throws ImproperStateException
    * if this SVDecomposition is uninitialized
    */
   public double norm() {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      return sigmax;
   }

   /**
    * Computes the determinant of the original matrix M associated with this
    * SVD.
    * 
    * @return determinant
    * @throws ImproperStateException
    * if this SVDecomposition is uninitialized
    */
   public double determinant() {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (m != n) {
         throw new ImproperSizeException ("Matrix not square");
      }
      return sigdet;
   }

   private void checkForUandV() {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (V_ == null) {
         throw new ImproperStateException (
            "V was not computed in the decomposition");
      }
      if (U_ == null) {
         throw new ImproperStateException (
            "U was not computed in the decomposition");
      }
   }

   /**
    * Solves the linear equation <br>
    * M x = b <br>
    * where M is the original matrix associated with this SVD, and x and b are
    * vectors. If M has size {@code m X n} with {@code m > n}, then the system
    * is overdetermined and solution with the minimum least square error is
    * computed. If {@code m < n}, then the system is underdetermined
    * and the minimum norm solution is computed.
    *
    * <p>This method assumes that M has full rank (i.e., that the minimum
    * singular value is {@code > 0}). If it does not, then the method returns
    * false and the solution will likely contain infinite values. To handle
    * situations where M does not have full rank, one should use {@link
    * #solve(Vector,Vector,double)} instead.
    * 
    * @param x
    * unknown vector to solve for
    * @param b
    * constant vector
    * @return false if M does not have full rank
    * @throws ImproperStateException
    * if this decomposition is uninitialized, or if U or V were not computed
    * @throws ImproperSizeException
    * if b does not have a size compatible with M, or if x does not have a size
    * compatible with M and cannot be resized.
    */
   public boolean solve (Vector x, Vector b) {
      return solve (x, b, /*tol=*/-1);
   }

   /**
    * Solves the linear equation <br>
    * M x = b <br>
    * where M is the original matrix associated with this SVD, and x and b are
    * vectors. If M has size {@code m X n} with {@code m > n}, then the system
    * is overdetermined and solution with the minimum least square error is
    * computed. If {@code m < n}, then the system is underdetermined
    * and the minimum norm solution is computed.
    *
    * <p>To handle situations where {@code M} is rank deficient, the
    * calculation ignores singular values whose value is less than or equal to
    * {@code tol*sigmax}, where {@code tol} is a specified tolerance and
    * {@code*sigmax} is the maximum singular value.  Results that would
    * otherwise be obtained by dividing by these values are instead set to
    * zero, resulting in pseudoinverse solutions. Specifying a negative value for
    * {@code tol} removes this behavior, so that the resulting solution will be
    * identical to #solve(VectorNd,VectorNd)} and {@code false} will be
    * returned if M does not have full rank.
    * 
    * @param x
    * unknown vector to solve for
    * @param b
    * constant vector
    * @param tol 
    * solution tolerance
    * @return false if {@code tol} is negative and M does not have full rank
    * @throws ImproperStateException
    * if this decomposition is uninitialized, or if U or V were not computed
    * @throws ImproperSizeException
    * if b does not have a size compatible with M, or if x does not have a size
    * compatible with M and cannot be resized.
    */
   public boolean solve (Vector x, Vector b, double tol) {
      checkForUandV();
      if (b.size() != m) {
         throw new ImproperSizeException ("improper size for b");
      }
      if (x.size() != n) {
         if (x.isFixedSize()) {
            throw new ImproperSizeException ("improper size for x");
         }
         else {
            x.setSize (n);
         }
      }
      int r = rank(tol);
      btmp.set (b);
      U_.mulTranspose (vtmp, btmp, r, m);
      for (int i = 0; i < r; i++) {
         vtmp.buf[i] /= sig.get(i);
      }
      V_.mul (xtmp, vtmp, n, r);
      x.set (xtmp);
      return !(tol < 0 && sigmin == 0);
   }

   /**
    * Solves the linear equation <br>
    * M X = B <br>
    * where M is the original matrix associated with this SVD, and X and B are
    * matrices. If M has size {@code m X n} with {@code m > n}, then the system
    * is overdetermined and solution with the minimum least square error is
    * computed. If {@code m < n}, then the system is underdetermined
    * and the minimum norm solution (which respect to each column of
    * X) is computed.
    * 
    * <p>This method assumes that M has full rank (i.e., that the minimum
    * singular value is {@code > 0}). If it does not, then the method returns
    * false and the solution will likely contain infinite values. To handle
    * situations where M does not have full rank, one should use {@link
    * #solve(DenseMatrix,DenseMatrix,double)} instead.
    * 
    * @param X
    * unknown matrix to solve for
    * @param B
    * constant matrix
    * @return false if M does not have full rank
    * @throws ImproperStateException
    * if this decomposition is uninitialized, or if U or V were not computed
    * @throws ImproperSizeException
    * if B has a different number of rows than M, or if the size of X
    * is incompatible with B or M and cannot be resized.
    */
   public boolean solve (DenseMatrix X, DenseMatrix B) {
      return solve (X, B, /*tol=*/-1);
   }

   /**
    * Solves the linear equation <br>
    * M X = B <br>
    * where M is the original matrix associated with this SVD, and X and B are
    * matrices. If M has size {@code m X n} with {@code m > n}, then the system
    * is overdetermined and solution with the minimum least square error is
    * computed. If {@code m < n}, then the system is underdetermined
    * and the minimum norm solution (which respect to each column of
    * X) is computed.
    * 
    * <p>To handle situations where {@code M} is rank deficient, the
    * calculation ignores singular values whose value is less than or equal to
    * {@code tol*sigmax}, where {@code tol} is a specified tolerance and
    * {@code*sigmax} is the maximum singular value.  Results that would
    * otherwise be obtained by dividing by these values are instead set to
    * zero, resulting in pseudoinverse solutions. Specifying a negative value for
    * {@code tol} removes this behavior, so that the resulting solution will be
    * identical to #solve(VectorNd,VectorNd)} and {@code false} will be
    * returned if M does not have full rank.
    * 
    * @param X
    * unknown matrix to solve for
    * @param B
    * constant matrix
    * @param tol 
    * solution tolerance
    * @return false if {@code tol} is negative and M does not have full rank
    * of M is zero
    * @throws ImproperStateException
    * if this decomposition is uninitialized, or if U or V were not computed
    * @throws ImproperSizeException
    * if B has a different number of rows than M, or if the size of X
    * is incompatible with B or M and cannot be resized.
    */
   public boolean solve (DenseMatrix X, DenseMatrix B, double tol) {
      checkForUandV();
      if (B.rowSize() != m) {
         throw new ImproperSizeException ("improper size for B");
      }
      if (X.colSize() != B.colSize() || X.rowSize() != n) {
         if (X.isFixedSize()) {
            throw new ImproperSizeException ("improper size for X");
         }
         else {
            X.setSize (n, B.colSize());
         }
      }
      int r = rank(tol);
      for (int j = 0; j < X.colSize(); j++) {
         B.getColumn (j, btmp);
         U_.mulTranspose (vtmp, btmp, r, m);
         for (int i = 0; i < r; i++) {
            vtmp.buf[i] /= sig.get(i);
         }
         V_.mul (xtmp, vtmp, n, r);
         X.setColumn (j, xtmp);
      }
      return !(tol < 0 && sigmin == 0);
   }

   // /**
   //  * Computes a left solution to the linear equation <br>
   //  * x M = b <br>
   //  * where M is the original matrix associated with this SVD, and x and b are
   //  * row vectors.
   //  * 
   //  * @param x
   //  * unknown vector to solve for
   //  * @param b
   //  * constant vector
   //  * @return false if the minimum singular of M value is zero
   //  * @throws ImproperStateException
   //  * if this decomposition is uninitialized, or if U or V were not computed
   //  * @throws ImproperSizeException
   //  * if b does not have a size compatible with M, or if x does not have a size
   //  * compatible with M and cannot be resized.
   //  */
   // public boolean leftSolve (VectorNd x, VectorNd b) {
   //    return leftSolve (x, b, /*tol=*/-1);
   // }

   // /**
   //  * Computes a left solution to the linear equation <br>
   //  * x M = b <br>
   //  * where M is the original matrix associated with this SVD, and x and b are
   //  * row vectors.
   //  *
   //  * <p>The calculation ignores singular values whose value is less than or
   //  * equal to {@code tol*sigmax}, where {@code tol} is a specified tolerance
   //  * and {@code*sigmax} is the maximum singular value.  Results that would
   //  * otherwise be obtained by dividing by these values are instead set to
   //  * zero. This allows pseudosolutions to be obtained when {@code M} is rank
   //  * deficient. Specifying a negative value for {@code tol} ensures that all
   //  * singular values will be used.
   //  * 
   //  * @param x
   //  * unknown vector to solve for
   //  * @param b
   //  * constant vector
   //  * @param tol 
   //  * solution tolerance
   //  * @return false if {@code tol} is negative and the minimum singular value
   //  * of M is zero
   //  * @throws ImproperStateException
   //  * if this decomposition is uninitialized, or if U or V were not computed
   //  * @throws ImproperSizeException
   //  * if b does not have a size compatible with M, or if x does not have a size
   //  * compatible with M and cannot be resized.
   //  */
   // public boolean leftSolve (VectorNd x, VectorNd b, double tol) {
   //    checkForUandV();
   //    if (b.size() != n) {
   //       throw new ImproperSizeException ("improper size for b");
   //    }
   //    if (x.size() != m) {
   //       if (x.isFixedSize()) {
   //          throw new ImproperSizeException ("improper size for x");
   //       }
   //       else {
   //          x.setSize (m);
   //       }
   //    }
   //    int r = rank(tol);
   //    V_.mulTranspose (vtmp, b, r, n);
   //    for (int i = 0; i < r; i++) {
   //       vtmp.buf[i] /= sig.get(i);
   //    }
   //    U_.mul (x, vtmp, m, r);
   //    return !(tol < 0 && sigmin == 0);
   // }

   // /**
   //  * Computes a left solution to the linear equation <br>
   //  * X M = B <br>
   //  * where M is the original matrix associated with this SVD, and X and B are
   //  * matrices.
   //  * 
   //  * @param X
   //  * unknown matrix to solve for
   //  * @param B
   //  * constant matrix
   //  * @return false if the minimum singular value of M is zero
   //  * @throws ImproperStateException
   //  * if this decomposition is uninitialized, or if U or V were not computed
   //  * @throws ImproperSizeException
   //  * if B has a different number of columns than M, or if the size of X
   //  * is incompatible with B or M and cannot be resized.
   //  */
   // public boolean leftSolve (MatrixNd X, MatrixNd B) {
   //    return leftSolve (X, B, /*tol=*/-1);
   // }

   // /**
   //  * Computes a left solution to the linear equation <br>
   //  * M X = B <br>
   //  * where M is the original matrix associated with this SVD, and X and B are
   //  * matrices.
   //  * 
   //  * <p>The calculation ignores singular values whose value is less than or
   //  * equal to {@code tol*sigmax}, where {@code tol} is a specified tolerance
   //  * and {@code*sigmax} is the maximum singular value.  Results that would
   //  * otherwise be obtained by dividing by these values are instead set to
   //  * zero. This allows pseudosolutions to be obtained when {@code M} is rank
   //  * deficient. Specifying a negative value for {@code tol} ensures that all
   //  * singular values will be used.
   //  * 
   //  * @param X
   //  * unknown matrix to solve for
   //  * @param B
   //  * constant matrix
   //  * @param tol 
   //  * solution tolerance
   //  * @return false if {@code tol} is negative and the minimum singular value
   //  * of M is zero
   //  * @throws ImproperStateException
   //  * if this decomposition is uninitialized, or if U or V were not computed
   //  * @throws ImproperSizeException
   //  * if B has a different number of columns than M, or if the size of X
   //  * is incompatible with B or M and cannot be resized.
   //  */
   // public boolean leftSolve (MatrixNd X, MatrixNd B, double tol) {
   //    checkForUandV();
   //    if (B.colSize() != n) {
   //       throw new ImproperSizeException ("improper size for B");
   //    }
   //    if (X.rowSize() != B.rowSize() || X.colSize() != m) {
   //       if (X.isFixedSize()) {
   //          throw new ImproperSizeException ("improper size for X");
   //       }
   //       else {
   //          X.setSize (B.rowSize(), m);
   //       }
   //    }
   //    int r = rank(tol);
   //    for (int i = 0; i < X.nrows; i++) {
   //       B.getRow (i, btmp);
   //       V_.mulTranspose (vtmp, btmp, r, n);
   //       for (int j = 0; j < r; j++) {
   //          vtmp.buf[j] /= sig.get(j);
   //       }
   //       U_.mul (xtmp, vtmp, m, r);
   //       X.setRow (i, xtmp);
   //    }
   //    return !(tol < 0 && sigmin == 0);
   // }

   /**
    * Computes the inverse of the original matrix M associated this SVD, and
    * places the result in R.
    * 
    * @param R
    * matrix in which the inverse is stored
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized, or if U or V were not computed
    * @throws ImproperSizeException
    * if M is not square, or if R does not have the same size as M and cannot be
    * resized.
    */
   public boolean inverse (MatrixNd R) {
      checkForUandV();
      if (m != n) {
         throw new ImproperSizeException ("Matrix not square");
      }
      if (R.nrows != R.ncols || R.nrows != n) {
         if (R.isFixedSize()) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            R.setSize (n, n);
         }
      }
      for (int j = 0; j < n; j++) {
         U_.getRow (j, vtmp);
         for (int i = 0; i < vtmp.size; i++) {
            vtmp.buf[i] /= sig.get(i);
         }
         xtmp.mul (V_, vtmp);
         R.setColumn (j, xtmp);
      }
      return sigmin != 0;
   }

   /**
    * Computes the pseudo inverse of the original matrix M associated this SVD,
    * and places the result in R.
    * 
    * @param R
    * matrix in which the inverse is stored
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized, or if U or V were not computed
    * @throws ImproperSizeException
    * if R does not have the same size as M and cannot be resized.
    */
   public boolean pseudoInverse (MatrixNd R) {
      if (R.nrows != n || R.ncols != m) {
         if (R.isFixedSize()) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            R.setSize (n, m);
         }
      }
      return pseudoInverse((DenseMatrix)R);
   }
   
   /**
    * Computes the pseudo inverse of the original matrix M associated this SVD,
    * and places the result in R.
    * 
    * @param R
    * matrix in which the inverse is stored
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized, or if U or V were not computed
    * @throws ImproperSizeException
    * if R does not have the same size as M and cannot be resized.
    */
   public boolean pseudoInverse (DenseMatrix R) {
      checkForUandV();
      //      if (m != n) {
      //         throw new ImproperSizeException ("Matrix not square");
      //      }
      if (R.rowSize() != n && R.colSize() != m) {
         if (R.isFixedSize ()) {
            throw new ImproperSizeException ("Incompatible dimensions");
         } else {
            R.setSize (n, m);
         }
      }
      
      int vsize = V_.colSize ();
      VectorNd su = new VectorNd(vsize);
      VectorNd r = new VectorNd(n);
      
      for (int j = 0; j < m; j++) {
         for (int i = 0; i < mind; i++) {
            if (sig.get(i) > 0) {
               su.buf[i] = U_.get (j, i) / sig.get(i);
            } else {
               su.buf[i] = 0;  // multiply by 0
            }
         }
         for (int i = mind; i < vsize; i++) {
            su.buf[i] = 0;     // additional zeroes
         }
         
         r.mul (V_, su);
         R.setColumn (j, r);
      }
      return sigmin != 0;
   }
   
   private final double SGN (double x) {
      return x >= 0 ? 1 : -1;
   }

   private final double ABS (double x) {
      return x >= 0 ? x : -x;
   }

   private final double MAX (double x, double y) {
      return x >= y ? x : y;
   }

   private void houseRowAccum (MatrixNd Q, double[] buf, int m, int n) {
      // given a set of householder row transforms Pn-1 Pn-2 ... P0 = Q
      // stored implictly in buf, this routine computes Q^T

      int j, k;
      int firstCol = (m > n ? n-1 : n-2);
      for (j=firstCol; j >= 0; j--) {
         vec[j] = 1;
         for (k = j+1; k < m; k++) {
            vec[k] = buf[k*n + j];
         }
         QRDecomposition.housePreMul (Q, j, colBeta[j], vec, wec);
      }
   }

   private void houseColAccum (MatrixNd Q, double[] buf, int n) {
      // given a set of householder column transforms P0 P1 ... Pn-1 = Q
      // stored implictly in buf, this routine computes Q^T
      int i, k;
      for (i = n - 3; i >= 0; i--) {
         vec[i+1] = 1;
         for (k=i+2; k<n; k++) {
            vec[k] = buf[i*n + k];
         }
         QRDecomposition.housePostMul (Q, i+1, rowBeta[i], vec, wec);
      }
   }

   private void bidiagonalize (MatrixNd B) {
      int nrows = B.nrows;
      int ncols = B.ncols;


      int columnLimit = (nrows == ncols ? ncols - 1 : ncols);
      colBeta = new double[ncols]; // only need columnLimit
      rowBeta = new double[ncols]; // only need max(0,ncols-2)
      for (int j = 0; j < columnLimit; j++) {
         colBeta[j] = QRDecomposition.rowHouseReduce (B, j, j, vec, wec);
         if (j < columnLimit - 1) {
            if (j < ncols - 2) {
               rowBeta[j]=QRDecomposition.colHouseReduce (B, j, j+1, vec, wec);
            }
         }
      }
   }

   /**
    * Estimates the rank of the original matrix used to form this
    * decomposition, based on a supplied tolerance {@code tol}. The rank is
    * estimated by counting all singular values whose value is greater than
    * {@code smax*tol}, where {@code smax} is the maximim singular value.
    *
    * @param tol tolerance for estimating the rank
    * @return estimated rank of the orginal matrix
    * @throws ImproperStateException
    * if this SVDecomposition is uninitialized
    */
   public int rank (double tol) {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      // start at the end of sig since that may be faster
      int k = sig.size()-1;
      while (k>=0 && sig.get(k) <= tol*sigmax) {
         k--;
      }
      return k+1;
   }

}
