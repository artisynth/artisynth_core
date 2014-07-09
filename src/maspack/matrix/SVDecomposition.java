/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

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
 */
public class SVDecomposition {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private int m, n;
   private int mind; // min (m,n)
   private int maxd; // max (m,n)

   private boolean computeU = false;
   private boolean computeV = false;
   private int flags = 0;

   private double tol = 100 * DOUBLE_PREC;

   private double sigmax;
   private double sigmin;
   private double sigdet;

   private boolean initialized = false;
   private boolean transposedSolution = false;

   private double[] buf = new double[0];
   private double[] sig = new double[0];
   private double[] vec = new double[0];
   private double[] wec = new double[0];
   private int[] sigIndices = new int[0];

   private MatrixNd U_ = new MatrixNd (0, 0);
   private MatrixNd V_ = new MatrixNd (0, 0);
   private MatrixNd B_ = new MatrixNd (0, 0);

   private VectorNd vtmp = new VectorNd (0);
   private VectorNd btmp = new VectorNd (0);
   private VectorNd xtmp = new VectorNd (0);

   private double S;
   private double C;

   // private MatrixNd SubMat = new MatrixNd(0,0);
   private SubMatrixNd SubMat = new SubMatrixNd();

   /**
    * Specifies that the matrix U should not be computed.
    */
   public static final int OMIT_U = 0x1;

   /**
    * Specifies that the matrix V should not be computed.
    */
   public static final int OMIT_V = 0x2;

   /**
    * The default iteration limit for computing the SVD.
    */
   public static final int DEFAULT_ITERATION_LIMIT = 10;

   private int iterLimit = DEFAULT_ITERATION_LIMIT;

   /**
    * Sets the flags associated with SVD computation. The flags presently
    * supported include {@link #OMIT_V OMIT_V} and {@link #OMIT_U OMIT_U}.
    * 
    * @param flags
    * an or-ed combination of flags.
    * @see #getFlags
    */
   public void setFlags (int flags) {
      computeU = ((flags & OMIT_U) == 0);
      computeV = ((flags & OMIT_V) == 0);
      this.flags = flags;
   }

   /**
    * Gets the flags associated with SVD computation.
    * 
    * @return flags
    * @see #setFlags
    */
   public int getFlags() {
      return this.flags;
   }

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
    * Creates an SVDecomposition and initializes it to the SVD for the matrix M.
    * 
    * @param M
    * matrix to perform the SVD on
    */
   public SVDecomposition (Matrix M) {
      setFlags (0);
      factor (M);
   }

   /**
    * Creates an SVDecomposition, sets the computation flags, and initializes it
    * to the SVD for the matrix M.
    * 
    * @param M
    * matrix to perform the SVD on
    * @param flags
    * flags associated with SVD computation
    * @see #setFlags
    */
   public SVDecomposition (Matrix M, int flags) {
      setFlags (flags);
      factor (M);
   }

   /**
    * Creates an uninitialized SVDecomposition with the specified computation
    * flags.
    * 
    * @param flags
    * flags associated with SVD computation
    * @see #setFlags
    */
   public SVDecomposition (int flags) {
      setFlags (flags);
   }

   /**
    * Creates an uninitialized SVDecomposition.
    */
   public SVDecomposition() {
      setFlags (0);
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
      // [ c s ]
      // [ ]
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
      if (sig.length < mind) {
         sig = new double[mind];
         sigIndices = new int[mind];
      }
      if (vec.length < maxd) {
         vec = new double[maxd];
         wec = new double[maxd];
      }
      int maxIter = mind * iterLimit;
      vtmp.setSize (mind);
      btmp.setSize (m);
      xtmp.setSize (n);
      if (computeU) {
         U_.setSize (m, mind);
         // U_ = new MatrixNd (m, mind);
         U_.setIdentity();
      }
      if (computeV) {
         V_.setSize (n, mind);
         V_ = new MatrixNd (n, mind);
         V_.setIdentity();
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

      if (computeU) {
         houseRowAccum (U_, buf, m, n);
         // System.out.println ("U_=[\n" + U_.toString("%8.4f") + "]");
      }
      if (computeV) {
         houseColAccum (V_, buf, n);
         V_.transpose();
      }

      // MatrixNd B = new MatrixNd (m, n);
      // for (int i=0; i<mind; i++)
      // { B.set (i, i, buf[i*n+i]);
      // if (i<mind-1)
      // { B.set (i, i+1, buf[i*n+i+1]);
      // }
      // }

      // System.out.println ("U=[\n" + U_.toString("%9.6f") + "]");
      // System.out.println ("B=[\n" + B.toString("%9.6f") + "]");
      // System.out.println ("V=[\n" + V_.toString("%9.6f") + "]");
      // System.out.println ("*****");

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
            if (ABS (buf[(i) * w + i + 1]) < tol
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

      {
         double s;

         sigdet = (n > 1 ? -1 : 1);
         for (j = 0; j < n; j++) {
            s = buf[j * n + j];
            sigdet *= s;
            if (s < 0) {
               s = -s;
               if (computeV) {
                  int vw = V_.width;
                  for (i = 0; i < n; i++) {
                     V_.buf[i * vw + j] = -V_.buf[i * vw + j];
                  }
               }
            }
            sig[j] = s;
            sigIndices[j] = j;
         }

         // bubble sort
         for (i = 0; i < n - 1; i++) {
            for (j = i + 1; j < n; j++) {
               if (sig[i] < sig[j]) {
                  double tmp = sig[i];
                  sig[i] = sig[j];
                  sig[j] = tmp;
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
            U_.permuteColumns (sigIndices);
            V_.permuteColumns (sigIndices);
         }
         sigmin = sig[mind - 1];
         sigmax = sig[0];

         // sigmin = Double.POSITIVE_INFINITY;
         // sigmax = Double.NEGATIVE_INFINITY;
         // // if n > 1 then there are an odd number of Householder
         // // reflections, with net determinant -1
         // sigdet = (n > 1 ? -1 : 1);
         // for (j=0; j<n; j++)
         // { s = buf[j*n+j];
         // sigdet *= s;
         // if (s < 0)
         // { s = -s;
         // if (computeV)
         // { for (i=0; i<n; i++)
         // { V_.buf[i*n+j] = -V_.buf[i*n+j];
         // }
         // }
         // }
         // if (s > sigmax)
         // { sigmax = s;
         // }
         // if (s < sigmin)
         // { sigmin = s;
         // }
         // sig[j] = s;
         // }
      }
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
         if (computeU) {
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
         if (computeV) {
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
         if (computeU) {
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
    * left-hand orthogonal matrix
    * @param svec
    * vector giving the diagonal elements of S
    * @param V
    * right-hand orthogonal matrix (note that this is V, and not it's transpose
    * V').
    * @throws ImproperStateException
    * if this SVDecomposition is uninitialized
    * @throws ImproperSizeException
    * if U, svec, or V are not of the proper dimension and cannot be resized.
    */
   public void get (DenseMatrix U, Vector svec, DenseMatrix V) {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (U != null && (U.rowSize() != m || U.colSize() != mind)) {
         if (!U.isFixedSize()) {
            U.setSize (m, mind);
         }
         else {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
      }
      if (V != null && (V.rowSize() != n || V.colSize() != mind)) {
         if (!V.isFixedSize()) {
            V.setSize (n, mind);
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
    * Factors a matrix M and returns its eigen decomposition:
    * <pre>
    * M = U E U^T
    * </pre>
    * where U is orthogonal and E is a diagonal matrix of eigenvalues. It is
    * assumed that M is symmetric. If M is not symmetric, then a symmetric matrix
    * is formed from
    * <pre>
    * 1/2 (M + M')
    * </pre>
    *
    * @param M matrix to be factored
    * @param U
    * left-hand orthogonal matrix (optional, may be null)
    * @return eigenvalues for the matrix
    * @throws IllegalArgumentException 
    * if M is not square
    * @throws ImproperSizeException
    * if U is not of the proper dimension and cannot be resized.
    */
   public VectorNd getEigenValues (DenseMatrix M, DenseMatrix U) {
      if (M.rowSize() != M.colSize()) {
         throw new IllegalArgumentException ("M is not square");
      }
      if (!M.isSymmetric(0)) {
         int size = M.rowSize();
         MatrixNd MS = new MatrixNd (size, size);
         for (int i=0; i<size; i++) {
            for (int j=i; j<size; j++) {
               if (i == j) {
                  MS.set (i, j, M.get(i, j));
               }
               else {
                  double val = (M.get(i,j) + M.get(j,i))/2;
                  MS.set (i, j, val);
                  MS.set (j, i, val);
               }
            }
         }
         factor (MS);
      }
      else {
         factor (M);
      }
      return getEigenValues (U);
   }

   /**
    * Gets the eigen decomposition for the currently factore (symmetric) matrix M:
    * <pre>
    * M = U E U^T
    * </pre>
    * where U is orthogonal and E is a diagonal matrix of eigenvalues. It is
    * assumed that <code>factor(M)</code> was called previously, and
    * that M is symmetric.
    * 
    * @param U
    * left-hand orthogonal matrix (optional, may be null)
    * @return eigenvalues for the matrix
    * @throws ImproperStateException
    * if this SVDecomposition does not contain a factored matrix, or if
    * that matrix is not square
    * @throws ImproperSizeException
    * if U is not of the proper dimension and cannot be resized.
    */
   public VectorNd getEigenValues (DenseMatrix U) {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (m != n) {
         throw new ImproperStateException ("Original M matrix not square");
      }
      VectorNd evec = new VectorNd (m);
      if (U != null && (U.rowSize() != m || U.colSize() != mind)) {
         if (!U.isFixedSize()) {
            U.setSize (m, mind);
         }
         else {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
      }
      if (U != null) {
         U.set (U_);
      }
      double[] ubuf = U_.getBuffer();
      double[] vbuf = V_.getBuffer();
      for (int j=0; j<n; j++) {
         double dot = 0;
         // U(:,j) should equal -V(:,j) or V(:,j). Determine which
         // by taking the dot product
         for (int i=0; i<n; i++) {
            dot += ubuf[i*n+j]*vbuf[i*n+j];
         }
         evec.set (j, dot >= 0 ? sig[j] : -sig[j]);
      }
      boolean sort = true;
      if (sort) {
         double[] etmp = new double[n];
         double[] vtmp = new double[n];
         int[] perm = new int[n];
         int kf = 0;
         int ke = n-1;
         for (int j=0; j<n; j++) {
            if (evec.get(j) < 0) {
               perm[ke] = j;
               etmp[ke] = evec.get(j);
               if (U != null) {
                  U_.getColumn (j, vtmp);
                  U.setColumn (ke, vtmp);
               }
               ke--;
            }
            else {
               perm[kf] = j;
               etmp[kf] = evec.get(j);
               if (U != null) {
                  U_.getColumn (j, vtmp);
                  U.setColumn (kf, vtmp);
               }
               kf++;
            }
         }
         evec.set (etmp);
      }
      return evec;
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

   /**
    * Solves the linear equation <br>
    * M x = b <br>
    * where M is the original matrix associated with this SVD, and x and b are
    * vectors.
    * 
    * @param x
    * unknown vector to solve for
    * @param b
    * constant vector
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if b does not have a size compatible with M, or if x does not have a size
    * compatible with M and cannot be resized.
    */
   public boolean solve (VectorNd x, VectorNd b) {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (b.size() != n) {
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
      vtmp.mulTranspose (U_, b);
      for (int i = 0; i < vtmp.size; i++) {
         vtmp.buf[i] /= sig[i];
      }
      x.mul (V_, vtmp);
      return sigmin != 0;
   }

   /**
    * Solves the linear equation <br>
    * M X = B <br>
    * where M is the original matrix associated with this SVD, and X and B are
    * matrices.
    * 
    * @param X
    * unknown matrix to solve for
    * @param B
    * constant matrix
    * @return false if M is singular (within working precision) and true
    * otherwise.
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if B has a different number of rows than M, or if X has a different number
    * of rows than M or a different number of columns than B and cannot be
    * resized.
    */
   public boolean solve (MatrixNd X, MatrixNd B) {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (B.rowSize() != n) {
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
      for (int j = 0; j < X.ncols; j++) {
         B.getColumn (j, btmp);
         vtmp.mulTranspose (U_, btmp);
         for (int i = 0; i < vtmp.size; i++) {
            vtmp.buf[i] /= sig[i];
         }
         xtmp.mul (V_, vtmp);
         X.setColumn (j, xtmp);
      }
      return sigmin != 0;
   }

   /**
    * Computes the inverse of the original matrix M associated this SVD, and
    * places the result in R.
    * 
    * @param R
    * matrix in which the inverse is stored
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if M is not square, or if R does not have the same size as M and cannot be
    * resized.
    */
   public boolean inverse (MatrixNd R) {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
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
            vtmp.buf[i] /= sig[i];
         }
         xtmp.mul (V_, vtmp);
         R.setColumn (j, xtmp);
      }
      return sigmin != 0;
   }

   /**
    * Computes the psuedo inverse of the original matrix M associated this SVD,
    * and places the result in R.
    * 
    * @param R
    * matrix in which the inverse is stored
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if M is not square, or if R does not have the same size as M and cannot be
    * resized.
    */
   public boolean pseudoInverse (MatrixNd R) {
      if (!initialized) {
         throw new ImproperStateException ("SVD not initialized");
      }
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
            if (sig[i] != 0) {
               vtmp.buf[i] /= sig[i];
            } else {
               vtmp.buf[i] = 0;  // multiply by 0
            }
         }
         xtmp.mul (V_, vtmp);
         R.setColumn (j, xtmp);
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
      // int w = n;
      int firstCol;

      // SubMat.setBuffer (m, n, Q.buf);
      SubMat.setDimensions (0, 0, m, n, Q);

      if (m > n) {
         firstCol = n - 1;
         // SubMat.makeSubMatrix (n, n, m-n, 0);
         SubMat.resetDimensions (n, n, m - n, 0);
      }
      else {
         firstCol = n - 2;
         // SubMat.makeSubMatrix (n-1, n-1, m-n+1, 1);
         SubMat.resetDimensions (n - 1, n - 1, m - n + 1, 1);
      }
      for (j = firstCol; j >= 0; j--) {
         vec[0] = 1;
         for (k = 1; k < m - j; k++) {
            vec[k] = buf[(j + k) * n + j];
         }
         // SubMat.makeSubMatrix (-1, -1, SubMat.nrows+1, SubMat.ncols+1);
         SubMat.resetDimensions (-1, -1, SubMat.nrows + 1, SubMat.ncols + 1);
         // SubMat.rowHouseMul (vec, wec);
         SubMat.rowHouseMul (vec, wec);
         // houseRow (&Q[j*Qw+j], v, m-j, n-j, Qw);
      }
      SubMat.clear();
   }

   private void houseColAccum (MatrixNd Q, double[] buf, int n) {
      // given a set of householder column transforms P0 P1 ... Pn-1 = Q
      // stored implictly in buf, this routine computes Q^T

      int j, k;
      // int w = n;

      // SubMat.setBuffer (n, n, Q.buf);
      // SubMat.makeSubMatrix (n-1, n-1, 1, 1);
      SubMat.setDimensions (n - 1, n - 1, 1, 1, Q);

      for (j = n - 3; j >= 0; j--) {
         vec[0] = 1;
         for (k = j + 2; k < n; k++) {
            vec[k - j - 1] = buf[j * n + k];
         }
         // SubMat.makeSubMatrix (-1, -1, SubMat.nrows+1, SubMat.ncols+1);
         // SubMat.colHouseMul (vec, wec);
         SubMat.resetDimensions (-1, -1, SubMat.nrows + 1, SubMat.ncols + 1);
         SubMat.colHouseMul (vec, wec);
      }
      SubMat.clear();
   }

   private void bidiagonalize (MatrixNd B) {
      int nrows = B.nrows;
      int ncols = B.ncols;

      // SubMat.setBuffer (nrows, ncols, buf);
      SubMat.setDimensions (0, 0, nrows, ncols, B);

      int columnLimit = (nrows == ncols ? ncols - 1 : ncols);
      for (int j = 0; j < columnLimit; j++) {
         SubMat.rowHouseReduce (vec, wec, true);
         if (j < columnLimit - 1) {
            // SubMat.makeSubMatrix (0, 1, SubMat.nrows, SubMat.ncols-1);
            SubMat.resetDimensions (0, 1, SubMat.nrows, SubMat.ncols - 1);
            if (j < ncols - 2) {
               SubMat.colHouseReduce (vec, wec, true);
            }
            // SubMat.makeSubMatrix (1, 0, SubMat.nrows-1, SubMat.ncols);
            SubMat.resetDimensions (1, 0, SubMat.nrows - 1, SubMat.ncols);
         }
      }
      SubMat.clear();
   }

}
