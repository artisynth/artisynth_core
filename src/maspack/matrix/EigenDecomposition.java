/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 *
 * Some methods in this class were taken from the EigenvalueDecomposition class
 * of Jama (http://math.nist.gov/javanumerics/jama), which is in the public
 * domain.
 */
package maspack.matrix;

/**
 * Forms the eigenvalue decomposition of an n X n matrix M, such that
 * <pre>
 * M V = V D
 * </pre>
 * where V is an n X n eigenvector matrix and D is an n X n
 * matrix formed from the eigenvalues.
 *
 * <p>If M is symmetric,
 * then D will be a diagonal matrix formed from the (real-valued)
 * eigenvalues, and V will be orthogonal.
 *
 * <p>If M is non-symmetric,
 * then the eigenvalues may also include complex conjugate pairs
 * (a + b i), (a - b i), in which case D will be block-diagonal, with each
 * complex conjugate pair forming a block
 * <pre>
 * [  a  b ]
 * [ -b  a ]
 * </pre>
 * If v1 and v2 are the columns of V corresponding to this block, then the
 * eigenvectors corresponding to (a + b i), (a - b i) are (a v1 - b v2 i) and
 * (b v1 + a v2 i). V is no longer guaranteed to be orthogonal, and may also
 * be singular.
 *
 * The methods <code>hqr2</code> and <code>tlr2</code> in this class were
 * taken from Jama (http://math.nist.gov/javanumerics/jama).
 */
public class EigenDecomposition {

   // double precision 
   public static final double EPS = 2.220446049250313e-16;

   public static final double zero = 0.0;
   public static final double one = 1.0;

   private int mySize; // dimension of the problem

   enum State {
      UNSET, SET_SYMMETRIC, SET_UNSYMMETRIC
   }

   private State state = State.UNSET;

   private double maxabs; // maximum absolute value of eigenvalues
   private double minabs; // minimum absolute value of eigenvalues
   private boolean singular;

   private double[] eig;
   private double[] img;
   private double[] beta = new double[0];
   private double[] buf = new double[0];
   private MatrixNd V_ = new MatrixNd (0, 0);
   
   private VectorNd eigr;
   private VectorNd eigi;

   private VectorNd vtmp = new VectorNd (0);
   private VectorNd xtmp = new VectorNd (0);
   private VectorNd btmp = new VectorNd (0);

   boolean debug = false;

   /**
    * Specifies that the symmetric part of M, or 1/2 (M+M^T), is to be
    * factored.
    */
   public static final int SYMMETRIC = 0x1;

   /**
    * Specifies that the eigenvector matrix V should not be computed.
    */
   public static final int OMIT_V = 0x2;

   /**
    * The default iteration limit for computing the SVD.
    */
   public static final int DEFAULT_ITERATION_LIMIT = 10;

   private int iterLimit = DEFAULT_ITERATION_LIMIT;

   /**
    * Gets the iteration limit for computations. The actual number of
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
    * Sets the iteration limit for computations. The actual number of
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
    * Creates a EigenDecomposition and initializes it for the matrix M.
    * 
    * @param M
    * matrix to perform the SVD on
    */
   public EigenDecomposition (Matrix M) {
      factor (M);
   }

   /**
    * Creates a EigenDecomposition, sets the computation flags, and
    * initializes it for the matrix M.
    * 
    * @param M
    * matrix to perform the Eigen decomposition on
    * @param flags
    * flags associated with computation
    */
   public EigenDecomposition (Matrix M, int flags) {
      factor (M, flags);
   }

   /**
    * Creates an uninitialized EigenDecomposition.
    */
   public EigenDecomposition() {
   }

   boolean setMatrixBuffer (Matrix M, boolean symmetric) {
      boolean isSymmetric = true;
      int n = M.rowSize();
      buf = new double[n*n];
      if (M instanceof MatrixNd) {
         MatrixNd Mn = (MatrixNd)M;
         double[] mbuf = Mn.buf;
         int mw = Mn.width;
         for (int i=0; i<n; i++) {
            buf[i*n+i] = mbuf[i*mw+i+Mn.base];
            for (int j=i+1; j<n; j++) {
               double uval = mbuf[i*mw + j + Mn.base];
               double lval = mbuf[j*mw + i + Mn.base];
               if (symmetric) {
                  lval = uval = (uval+lval)/2;
               }
               else if (lval != uval) {
                  isSymmetric = false;
               }
               buf[i*n+j] = uval;
               buf[j*n+i] = lval;
            }
         }
      }
      else {
         for (int i=0; i<n; i++) {
            buf[i*n+i] = M.get (i, i);
            for (int j=i+1; j<n; j++) {
               double uval = M.get (i, j);
               double lval = M.get (j, i);
               if (symmetric) {
                  lval = uval = (uval+lval)/2;
               }
               else if (lval != uval) {
                  isSymmetric = false;
               }
               buf[i*n+j] = uval;
               buf[j*n+i] = lval;
            }
         }
      }
      return isSymmetric;
   }

   /**
    * Performs an eigen decomposition on the symmetric part of the Matrix M.
    * The symmetric part is computed as 1/2 (M + M^T).
    * 
    * @param M
    * matrix to perform the decomposition on
    */
   public void factorSymmetric (Matrix M) {
      factor (M, SYMMETRIC);
   }
    
   /**
    * Performs an eigen decomposition on the symmetric part of the Matrix M.
    * The symmetric part is computed as 1/2 (M + M^T). Computation of V is
    * omitted if {@link #OMIT_V} is set in <code>flags</code>.
    * 
    * @param M
    * matrix to perform the decomposition on
    * @param flags
    * flags to control the decomposition
    */
   public void factorSymmetric (Matrix M, int flags) {
      factor (M, flags | SYMMETRIC);
   }
    
   /**
    * Performs an eigen decomposition on the Matrix M.  A symmetric
    * decomposition is performed if M is found to be exactly
    * symmetric. However, if M is supposed to be symmetric but is not exactly
    * so because of round-off error, then {@link #factorSymmetric} should be
    * used instead.
    * 
    * @param M
    * matrix to perform the decomposition on
    */
   public void factor (Matrix M) {
      factor (M, /*flags=*/0);
   }

   /**
    * Performs an eigen decomposition on the Matrix M. Computation of V is
    * omitted if {@link #OMIT_V} is set in <code>flags</code>.  A symmetric
    * decomposition is performed on the symmetric part of M (i.e., 1/2 (M+M^T)
    * if {@link #SYMMETRIC} is set in <code>flags</code>. A symmetric
    * decomposition will also be permformed if M is found to be exactly
    * symmetric. However, if M is supposed to be symmetric but is
    * not exactly so because of round-off error, then either the
    * {@link #SYMMETRIC} flag should be specified or
    * {@link #factorSymmetric} should be used instead.
    * 
    * @param M
    * matrix to perform the decomposition on
    * @param flags
    * flags to control the decomposition
    */
   public void factor (Matrix M, int flags) {
      if (M.rowSize() != M.colSize()) {
         throw new IllegalArgumentException (
            "Matrix M is not square");
      }
      mySize = M.rowSize();
      int n = mySize;
      
      eigr = new VectorNd (n);
      eigi = new VectorNd (n);
      eig = eigr.getBuffer();
      img = eigi.getBuffer();

      if ((flags & OMIT_V) == 0) {
         V_ = new MatrixNd (n, n);
      }
      else {
         V_ = null;
      }

      boolean symmetric = setMatrixBuffer (M, (flags & SYMMETRIC) != 0);

      vtmp.setSize(n);
      xtmp.setSize(n);
      btmp.setSize(n);

      if (symmetric) {
         double[] sub = btmp.getBuffer(); // use to store sub-diagonal
         tridiag (n, eig, sub, buf, V_);
         if (V_ != null) {
            V_.transpose();
         }
         tql2 (eig, sub, n, V_, iterLimit*n);    
         state = State.SET_SYMMETRIC;
      }
      else {
         upperhessen (n, buf, V_);
         if (V_ != null) {
            V_.transpose();
         }
         hqr2 (eig, img, buf, n, V_, iterLimit*n);
         state = State.SET_UNSYMMETRIC;
      }
      
      maxabs = Double.NEGATIVE_INFINITY;
      minabs = Double.POSITIVE_INFINITY;
      for (int i=0; i<n; i++) {
         double val = (symmetric ? Math.abs(eig[i]) : pythag (eig[i], img[i]));
         if (val > maxabs) {
            maxabs = val;
         }
         if (val < minabs) {
            minabs = val;
         }
      }
      singular = (maxabs == 0 || minabs/maxabs < EPS);
   }

   public void get (Vector e, DenseMatrix V) {
      get (e, null, V);
   }

   /**
    * Gets the eigenvalues and eigenvectors associated with the decomposition.
    *
    * @param er
    * If not <code>null</code>, returns the real part of the eigenvalues
    * @param ei
    * If not <code>null</code>, returns the imaginary part of the eigenvalues
    * @param V
    * If not <code>null</code>, returns the eigenvectors
    * @throws ImproperStateException
    * if this decomposition is uninitialized, or if V is non-null but V
    * was not computed
    * @throws ImproperSizeException
    * if eig or V are not of the proper dimension and cannot be resized.
    */
   public void get (Vector er, Vector ei, DenseMatrix V) {
      int n = mySize;
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decomposition not initialized");
      }
      if (V != null && (V.rowSize() != n || V.colSize() != n)) {
         if (!V.isFixedSize()) {
            V.setSize (n, n);
         }
         else {
            throw new ImproperSizeException (
               "V is "+V.getSize()+"; should be "+n+"x"+n);
         }
      }
      if (V != null && V_ == null) {
         throw new ImproperStateException ("V requested but was not computed");
      }
      if (er != null && er.size() < n) {
         if (!er.isFixedSize()) {
            er.setSize (n);
         }
         else {
            throw new ImproperSizeException (
               "eig has size "+er.size()+"; must be at least "+n);
         }
      }
      if (ei != null && ei.size() < n) {
         if (!ei.isFixedSize()) {
            ei.setSize (n);
         }
         else {
            throw new ImproperSizeException (
               "eig has size "+ei.size()+"; must be at least "+n);
         }
      }
      if (V != null) {
         V.set (V_);
      }
      if (er != null) {
         er.set (eig);
      }
      if (ei != null) {
         ei.set (img);
      }
   }

   /**
    * Returns the current eigenvector matrix V associated with this
    * decomposition. If V was not computed, returns
    * <code>null</code>. Subsequent factorizations will cause a different V to
    * be created. The returned matrix should not be modified if any subsequent
    * calls are to be made which depend on V (including solve and inverse
    * methods).
    *
    * @return current V matrix
    * @throws ImproperStateException
    * if this EigenDecomposition is uninitialized
    */
   public MatrixNd getV() {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decomposition not initialized");
      }
      return V_;
   }

   /**
    * Returns the real parts of the current eigenvalues associated with this
    * decomposition. Subsequent factorizations will cause a different vector to
    * be created. The returned vector should not be modified if any subsequent
    * calls are to be made which depend on it (including solve and inverse
    * methods).
    *
    * @return real parts of the current eigenvalues
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    */
   public VectorNd getEigReal() {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decompostion not initialized");
      }
      return eigr;
   }

   /**
    * Returns the imaginary parts of the current eigenvalues associated with
    * this decomposition. Subsequent factorizations will cause a different
    * vector to be created. The returned vector should not be modified if any
    * subsequent calls are to be made which depend on it (including solve and
    * inverse methods).
    *
    * @return imaginary parts of the current eigenvalues
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    */
   public VectorNd getEigImag() {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decompostion not initialized");
      }
      return eigi;
   }

   protected boolean pairIsConjugate (
      VectorNd er, VectorNd ei, int i, double tol) {
      
      double ar = er.get(i);
      double br = er.get(i+1);
      double ai = ei.get(i);
      double bi = ei.get(i+1);
      if (ai == 0 || bi == 0) {
         return false;
      }
      else {
         return (Math.abs(ar-br) <= tol && Math.abs(ai+bi) <= tol);
      }
   }

   /**
    * Returns the D matrix associated with this decomposition.
    *
    * @param D matrix to return D in
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException if D is not n X n and cannot be resized
    */
   public void getD (MatrixNd D) {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decompostion not initialized");
      }
      int n = mySize; 
      if (D != null && (D.rowSize() != n || D.colSize() != n)) {
         if (!D.isFixedSize()) {
            D.setSize (n, n);
         }
         else {
            throw new ImproperSizeException (
               "D is "+D.getSize()+"; should be "+n+"x"+n);
         }
      }
      if (state == State.SET_SYMMETRIC) {
         D.setDiagonal (eigr);
      }
      else {
         double tol = 100*maxabs*EPS;
         for (int i=0; i<n; i++) {
            if (i <n-1 && pairIsConjugate (eigr, eigi, i, tol)) {
               D.set (i, i, eigr.get(i));
               D.set (i+1, i, eigi.get(i+1));
               D.set (i, i+1, eigi.get(i));
               D.set (i+1, i+1, eigr.get(i+1));
               i++;
            }
            else {
               D.set (i, i, eigr.get(i));
            }
         }
      }
   } 

   /**
    * Returns the number of imaginary eigenvalues. For symmetric matrices, this
    * number is always 0. For unsymmetric matrices, the number is estimated by
    * identifying the the number of complex conjugate pairs whose magnitude
    * exceeds an appropriate number tolerance.
    *
    * @return number of imaginary eigenvalues
    */
   public int numEigImag () {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decomposition not initialized");
      }
      int num = 0;
      if (state == State.SET_UNSYMMETRIC) {
         int n = mySize;
         double tol = 100*maxabs*EPS;
         for (int i=0; i<n; i++) {
            if (i <n-1 && pairIsConjugate (eigr, eigi, i, tol)) {
               i++;
               num += 2;
            }
         }
      }
      return num;
   }

   public VectorNd getComplexPairs() {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decomposition not initialized");
      }
      VectorNd ceigs = new VectorNd();
      if (state == State.SET_UNSYMMETRIC) {
         int n = mySize;
         double tol = 100*maxabs*EPS;
         for (int i=0; i<n; i++) {
            if (i <n-1 && pairIsConjugate (eigr, eigi, i, tol)) {
               ceigs.append (eigr.get(i));
               ceigs.append (eigi.get(i));
               i++;
            }
         }
      }
      return ceigs;
   }

   /**
    * Returns the minimum absolute value of all the eigenvalues.
    *
    * @return minimum absolute value of all eigenvalues
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    */
   public double getMinAbsEig() {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decompostion not initialized");
      }
      return minabs;
   }

   /**
    * Returns the maximum absolute value of all the eigenvalues.
    *
    * @return maximum absolute value of all eigenvalues
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    */
   public double getMaxAbsEig() {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decompostion not initialized");
      }
      return maxabs;
   }

   /**
    * Convenience method that creates an EigenDecomposition, factors it for the
    * matrix M, and stores the resulting eigenvalues and vectors into the
    * corresponding arguments. If the argument V is specified as
    * <code>null</code>, then the eigenvector matrix will not be computed.
    *
    * @param er
    * If not <code>null</code>, returns the real part of the eigenvalues
    * @param ei
    * If not <code>null</code>, returns the imaginary part of the eigenvalues
    * @param V
    * If not <code>null</code>, returns the eigenvectors
    * @param M matrix to be factored
    * @return the resulting EigenDecomposition
    */
   public static EigenDecomposition factor (
      Vector er, Vector ei, DenseMatrix V, Matrix M) {
      int flags = 0;
      if (V == null) {
         flags |= OMIT_V;
      }
      EigenDecomposition evd = new EigenDecomposition (M, flags);
      evd.get (er, ei, V);
      return evd;
   }

   /**
    * Convenience method that creates an EigenDecomposition, factors it using a
    * symmetric factorization for the matrix M, and stores the resulting
    * eigenvalues and vectors into the corresponding arguments. If the argument
    * V is specified as <code>null</code>, then the eigenvector matrix will not
    * be computed.
    *
    * @param er
    * If not <code>null</code>, returns the (real) eigenvalues
    * @param V
    * If not <code>null</code>, returns the eigenvectors
    * @param M matrix to be factored
    * @return the resulting EigenDecomposition
    */
   public static EigenDecomposition factorSymmetric (
      Vector er, DenseMatrix V, Matrix M) {
      int flags = SYMMETRIC;
      if (V == null) {
         flags |= OMIT_V;
      }
      EigenDecomposition evd = new EigenDecomposition (M, flags);
      evd.get (er, null, V);
      return evd;
   }

   /**
    * Computes the condition number of the original matrix M associated with
    * this decomposition. This method only works if the decomposition was
    * symmetric.
    * 
    * @return condition number
    * @throws ImproperStateException
    * if this decomposition is uninitialized or unsymmetric
    */
   public double condition() {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decomposition not initialized");
      }
      else if (state == State.SET_UNSYMMETRIC) {
         throw new ImproperStateException (
            "condition() only implemented for symmetric factorizations");
      }
      return maxabs/minabs;
   }

   /**
    * Computes the 2-norm of the original matrix M associated with this
    * decomposition. This method only works if the decomposition was symmetric.
    * 
    * @return 2-norm
    * @throws ImproperStateException
    * if this decomposition is uninitialized or unsymmetric
    */
   public double norm() {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decomposition not initialized");
      }
      else if (state == State.SET_UNSYMMETRIC) {
         throw new ImproperStateException (
            "norm() only implemented for symmetric factorizations");
      }      
      return maxabs;
   }

   /**
    * Computes the determinant of the original matrix M associated with this
    * decomposition.
    * 
    * @return determinant
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    */
   public double determinant() {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decomposition not initialized");
      }
      // determinant is just the product of the eigenvalues
      double real = 1;
      double imag = 0;
      for (int i=0; i<mySize; i++) {
         double re = real;
         double im = imag;
         real = re*eig[i] - im*img[i];
         imag = re*img[i] + im*eig[i];
      }
      // final imaginary part will be zero
      return real;
   }

   /**
    * Solves the linear equation <br> M x = b <br> where M is the original
    * matrix associated with this decomposition, and x and b are vectors.  This
    * method only works if the decomposition was symmetric and if V was
    * computed.
    * 
    * @param x
    * unknown vector to solve for
    * @param b
    * constant vector
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized or unsymmetric, or if V was not
    * computed.
    * @throws ImproperSizeException
    * if b does not have a size compatible with M, or if x does not have a size
    * compatible with M and cannot be resized.
    */
   public boolean solve (VectorNd x, VectorNd b) {
      int n = mySize;
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decomposition not initialized");
      }
      else if (state == State.SET_UNSYMMETRIC) {
         throw new ImproperStateException (
            "solve() only implemented for symmetric factorizations");
      }
      if (V_ == null) {
         throw new ImproperStateException (
            "V was not computed in this decomposition");
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
      vtmp.setSize (n);
      vtmp.mulTranspose (V_, b);
      for (int i = 0; i < n; i++) {
         vtmp.buf[i] /= eig[i];
      }
      x.mul (V_, vtmp);
      return !singular;
   }

   /**
    * Solves the linear equation <br> M X = B <br> where M is the original
    * matrix associated with this decomposition, and X and B are matrices.
    * This method only works if the decomposition was symmetric and if V was
    * computed.
    * 
    * @param X
    * unknown matrix to solve for
    * @param B
    * constant matrix
    * @return false if M is singular (within working precision) and true
    * otherwise.
    * @throws ImproperStateException
    * if this decomposition is uninitialized or unsymmetric, or if V was not
    * computed.
    * @throws ImproperSizeException
    * if B has a different number of rows than M, or if X has a different number
    * of rows than M or a different number of columns than B and cannot be
    * resized.
    */
   public boolean solve (MatrixNd X, MatrixNd B) {
      int n = mySize;
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decomposition not initialized");
      }
      else if (state == State.SET_UNSYMMETRIC) {
         throw new ImproperStateException (
            "solve() only implemented for symmetric factorizations");
      }
      if (V_ == null) {
         throw new ImproperStateException (
            "V was not computed in this decomposition");
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
      vtmp.setSize (n);
      xtmp.setSize (n);
      btmp.setSize (n);
      for (int j = 0; j < X.ncols; j++) {
         B.getColumn (j, btmp);
         vtmp.mulTranspose (V_, btmp);
         for (int i = 0; i < vtmp.size; i++) {
            vtmp.buf[i] /= eig[i];
         }
         xtmp.mul (V_, vtmp);
         X.setColumn (j, xtmp);
      }
      return !singular;
   }

   /**
    * Computes the inverse of the original matrix M associated this
    * decomposition, and places the result in MI. This method only works if the
    * decomposition was symmetric and if V was computed.
    * 
    * @param MI
    * matrix in which the inverse is stored
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized or unsymmetric, or if V was not
    * computed.
    * @throws ImproperSizeException
    * If MI does not have the same size as M and cannot be
    * resized.
    */
   public boolean inverse (MatrixNd MI) {
      int n = mySize;
      if (state == State.UNSET) {
         throw new ImproperStateException ("Decomposition not initialized");
      }
      else if (state == State.SET_UNSYMMETRIC) {
         throw new ImproperStateException (
            "inverse() only implemented for symmetric factorizations");
      }
      if (V_ == null) {
         throw new ImproperStateException (
            "V was not computed in this decomposition");
      }
      if (MI.nrows != MI.ncols || MI.nrows != n) {
         if (MI.isFixedSize()) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            MI.setSize (n, n);
         }
      }
      vtmp.setSize (n);
      xtmp.setSize (n);
      for (int j = 0; j < n; j++) {
         V_.getRow (j, vtmp);
         for (int i = 0; i < vtmp.size; i++) {
            vtmp.buf[i] /= eig[i];
         }
         xtmp.mul (V_, vtmp);
         MI.setColumn (j, xtmp);
      }
      return !singular;
   }


   protected static double pythag (double a, double b) {
      return Math.sqrt (a*a + b*b);
   }

   protected static double pythag2 (double a, double b) {
      return a*a + b*b;
   }

   protected static final double fabs (double a) {
      return a >= 0 ? a : -a;
   }

   protected static final double SIGN (double a, double b) {
      return b >= 0 ? fabs(a) : -fabs(a);
   }

   /**
    * Overwrites v with its householder vector and returns beta
    */
   double houseVector (double[] v, int i0, int iend) {
      double x0 = v[i0];
      VectorNd x = new VectorNd (iend-i0);
      for (int i=i0; i<iend; i++) {
         x.set (i-i0, v[i]);
      }
      double sigma = 0;
      for (int i=i0+1; i<iend; i++) {
         sigma += v[i]*v[i];
      }
      v[i0] = 1;
      if (sigma == 0) {
         return 0;
         //return x0 >= 0 ? 0 : -2;
      }
      else {
         double mu = Math.sqrt(x0*x0 + sigma);
         double v0;
         if (x0 <= 0) {
            v0 = x0-mu;
         }
         else {
            v0 = -sigma/(x0+mu);
         }
         double beta = 2*v0*v0/(sigma+v0*v0);
         for (int i=i0+1; i<iend; i++) {
            v[i] /= v0;
         }
         return beta;
      }
   }

   public void tridiag (
      int n, double[] d, double[] e, double[] buf, MatrixNd Z) {

      double[] v = vtmp.getBuffer();
      double[] w = xtmp.getBuffer();

      double[] zbuf = null;
      if (Z != null) {
         Z.setIdentity();
         zbuf = Z.getBuffer();
      }
      for (int k=0; k<n-2; k++) {
         double mag = 0;
         for (int i=k+1; i<n; i++) {
            double x = buf[i*n+k];
            v[i] = x;
            mag += x*x;
         }
         mag = Math.sqrt(mag);
         double beta = houseVector (v, k+1, n);
         for (int i=k+1; i<n; i++) {
            double sum = 0;
            for (int j=k+1; j<n; j++) {
               sum += buf[i*n+j]*v[j];
            }
            w[i] = beta*sum;
         }
         double s = 0;
         for (int i=k+1; i<n; i++) {
            s += w[i]*v[i];
         }
         s *= beta/2;
         for (int i=k+1; i<n; i++) {
            w[i] -= s*v[i];
         }
         buf[(k+1)*n+k] = mag;
         buf[k*n+(k+1)] = mag;
         for (int i=k+1; i<n; i++) {
            // buf[i*n+k] = v[i]; store vector
            for (int j=k+1; j<n; j++) {
               buf[i*n+j] -= (v[i]*w[j] + w[i]*v[j]);
            }
         }
         if (zbuf != null) {
            for (int j=1; j<n; j++) {
               double sum = 0;
               for (int i=k+1; i<n; i++) {
                  sum += zbuf[i*n+j]*v[i];
               }
               w[j] = beta*sum;
            }
            for (int i=k+1; i<n; i++) {
               for (int j=1; j<n; j++) {
                  zbuf[i*n+j] -= v[i]*w[j];
               }
            }
         }
      }
      for (int i=0; i<n; i++) {
         d[i] = buf[i*n+i];
         if (i > 0) {
            e[i] = buf[i*n+i-1];
         }
      }
   }

   public static int tqli(double[] d, double[] e, int n, MatrixNd Z) {
      /**
         QL algorithm with implicit shifts, to determine the eigenvalues and
         eigenvectors of a real, sym- metric, tridiagonal matrix, or of a real,
         symmetric matrix previously reduced by tred2 Sec 11.2. On input, d[1..n]
         contains the diagonal elements of the tridiagonal matrix. On output,
         it returns the eigenvalues. The vector e[1..n] inputs the subdiagonal
         elements of the tridiagonal matrix, with e[1] arbitrary. On output e
         is destroyed. When finding only the eigenvalues, several lines may be
         omitted, as noted in the comments. If the eigenvectors of a
         tridiagonal matrix are de- sired, the matrix z[1..n][1..n] is input as
         the identity matrix. If the eigenvectors of a matrix that has been
         reduced by tred2 are required, then z is input as the matrix output by
         tred2 .  In either case, the k th column of z returns the normalized
         eigenvector corresponding to d[k] . */

      int m,l,iter,i,k;
      double s,r,p,g,f,dd,c,b;
      for (i=1;i<n;i++) e[i-1]=e[i];
      //Convenient to renumber the elements of e.
      e[n-1]=0.0;
      for (l=0;l<n;l++) {
         iter=0;
         do {
            for (m=l;m<n-1;m++) {
               //Look for a single small subdiagonal element to split the matrix
               dd=fabs(d[m])+fabs(d[m+1]);
               if ((double)(fabs(e[m])+dd) == dd) break;
            }
            if (m != l) {
               if (iter++ == 30*n) {
                  return 1;
               }
               g=(d[l+1]-d[l])/(2.0*e[l]); // Form shift
               r=pythag(g,1.0);
               g=d[m]-d[l]+e[l]/(g+SIGN(r,g)); // This is d m - k s .
               s=c=1.0;
               p=0.0; 

               for (i=m-1;i>=l;i--) {
                  // A plane rotation as in the original QL, followed by Givens
                  // rotations to restore tridiagonal form.
                  f=s*e[i];
                  b=c*e[i];
                  e[i+1]=(r=pythag(f,g));
                  if (r == 0.0) {
                     //Recover from underflow.
                     d[i+1] -= p;
                     e[m]=0.0;
                     break;
                  }
                  s=f/r;
                  c=g/r;
                  g=d[i+1]-p;
                  r=(d[i]-g)*s+2.0*c*b;
                  d[i+1]=g+(p=s*r);
                  g=c*r-b;
                  /* Next loop can be omitted if eigenvectors not wanted*/
                  if (Z != null) {
                     double[] z = Z.getBuffer();
                     int w = Z.getBufferWidth();
                     for (k=0;k<Z.rowSize();k++) {
                        // Form eigenvectors.
                        f=z[k*w+i+1];
                        z[k*w+i+1]=s*z[k*w+i]+c*f;
                        z[k*w+i]=c*z[k*w+i]-s*f;
                     }
                  }
               }
               if (r == 0.0 && i >= l) continue;
               d[l] -= p;
               e[l]=g;
               e[m]=0.0;
            }
         } while (m != l);
      }
      return 0;
   }

   public void upperhessen (
      int n, double[] buf, MatrixNd Z) {

      vtmp.setSize(n);
      xtmp.setSize(n);
      double[] v = vtmp.getBuffer();
      double[] w = xtmp.getBuffer();

      if (Z != null) {
         Z.setIdentity();
      }
      MatrixNd B = new MatrixNd();
      B.setBuffer (n, n, buf, n);

      beta = new double[n-2];
      for (int k=0; k<n-2; k++) {
         beta[k] = QRDecomposition.rowHouseReduce (B, k, k+1, v, w);
         QRDecomposition.housePostMul (B, k+1, beta[k], v, w);
         if (Z != null) {
            QRDecomposition.housePreMul (Z, k+1, beta[k], v, w);
         }
      }
   }

   /**
    * Computes the eigenvalues and eigenvectors of a symmetric tridiagonal matrix
    * This code was taken from Jama (http://math.nist.gov/javanumerics/jama).
    * where it was derived from the Algol procedures tql2, by
    * Bowdler, Martin, Reinsch, and Wilkinson, Handbook for
    * Auto. Comp., Vol.ii-Linear Algebra, and the corresponding
    * Fortran subroutine in EISPACK.
    *
    * @param d diagonal elements of the matrix
    * @param e off-diagonal elements of the matrix
    * @param n matrix size
    * @param V if non-null, used to return the eigenvalues.
    * @param maxit iteration limit. If -1, 30*n will be commonly used.
    */
   public static int tql2 (
      double[] d, double[] e, int n, MatrixNd V, int maxit) {

      if (n < 0) {
         throw new IllegalArgumentException ("n is negative ("+n+")");
      }
      if (d.length < n) {
         throw new IllegalArgumentException (
            "d has length "+d.length+", must be at least "+n);
      }
      if (e.length < n) {
         throw new IllegalArgumentException (
            "e has length "+e.length+", must be at least "+n);
      }
      if (V != null) {
         if (V.colSize() != n) {
            throw new IllegalArgumentException (
               "V has "+V.colSize()+" columns; must have "+n);
         }
      }
      if (n == 0) {
         return 0;
      }
      if (n == 1) {
         if (V != null) {
            V.set (0, 0, 1.0);
         }
         return 0;
      }

      if (maxit <= 0) {
         maxit = 30*n;
      }

      double[] v = null;
      int vw = 0;
      if (V != null) {
         v = V.getBuffer();
         vw = V.getBufferWidth();
      }

      for (int i = 1; i < n; i++) {
         e[i-1] = e[i];
      }
      e[n-1] = 0.0;
   
      double f = 0.0;
      double tst1 = 0.0;
      for (int l = 0; l < n; l++) {

         // Find small subdiagonal element
   
         tst1 = Math.max(tst1,Math.abs(d[l]) + Math.abs(e[l]));
         int m = l;
         while (m < n) {
            if (Math.abs(e[m]) <= EPS*tst1) {
               break;
            }
            m++;
         }
   
         // If m == l, d[l] is an eigenvalue,
         // otherwise, iterate.
   
         if (m > l) {
            int iter = 0;
            do {
               iter = iter + 1;
               if (iter > maxit) {
                  return 1;
               }
               // Compute implicit shift
   
               double g = d[l];
               double p = (d[l+1] - g) / (2.0 * e[l]);
               double r = pythag(p,1.0);
               if (p < 0) {
                  r = -r;
               }
               d[l] = e[l] / (p + r);
               d[l+1] = e[l] * (p + r);
               double dl1 = d[l+1];
               double h = g - d[l];
               for (int i = l+2; i < n; i++) {
                  d[i] -= h;
               }
               f = f + h;
   
               // Implicit QL transformation.
   
               p = d[m];
               double c = 1.0;
               double c2 = c;
               double c3 = c;
               double el1 = e[l+1];
               double s = 0.0;
               double s2 = 0.0;
               for (int i = m-1; i >= l; i--) {
                  c3 = c2;
                  c2 = c;
                  s2 = s;
                  g = c * e[i];
                  h = c * p;
                  r = pythag(p,e[i]);
                  e[i+1] = s * r;
                  s = e[i] / r;
                  c = p / r;
                  p = c * d[i] - s * g;
                  d[i+1] = h + s * (c * g + s * d[i]);
   
                  // Accumulate transformation.
                  if (V != null) {
                     for (int k=0; k<V.rowSize(); k++) {
                        // Form eigenvectors.
                        h = v[k*vw+i+1];
                        v[k*vw+i+1] = s*v[k*vw+i] + c*h;
                        v[k*vw+i] = c*v[k*vw+i] - s*h;
                     }                     
                  }
               }
               p = -s * s2 * c3 * el1 * e[l] / dl1;
               e[l] = s * p;
               d[l] = c * p;
   
               // Check for convergence.
   
            } while (Math.abs(e[l]) > EPS*tst1);
         }
         d[l] = d[l] + f;
         e[l] = 0.0;
      }

      // code to round very small eigenvalues to zero
      // double max = -1;
      // for (int i=0; i<n; i++) {
      //    double abs = Math.abs(d[i]);
      //    if (abs > max) {
      //       max = abs;
      //    }
      // }
      // double tol = max*EPS;
      // for (int i=0; i<n; i++) {
      //    if (Math.abs(d[i]) < tol) {
      //       d[i] = 0;
      //    }
      // }
     
      // Sort eigenvalues and corresponding vectors.
   
      for (int i = 0; i < n-1; i++) {
         int k = i;
         double p = d[i];
         for (int j = i+1; j < n; j++) {
            if (d[j] < p) {
               k = j;
               p = d[j];
            }
         }
         if (k != i) {
            d[k] = d[i];
            d[i] = p;
            if (V != null) {
               for (int j = 0; j < V.rowSize(); j++) {
                  p = v[j*vw+i];
                  v[j*vw+i] = v[j*vw+k];
                  v[j*vw+k] = p;
               }
            }
         }
      }
      return 0;
   }

   // Complex scalar division.

   private transient double cdivr, cdivi;
   private void cdiv(double xr, double xi, double yr, double yi) {
      double r,d;
      if (Math.abs(yr) > Math.abs(yi)) {
         r = yi/yr;
         d = yr + r*yi;
         cdivr = (xr + r*xi)/d;
         cdivi = (xi - r*xr)/d;
      } else {
         r = yr/yi;
         d = yi + r*yr;
         cdivr = (r*xr + xi)/d;
         cdivi = (r*xi - xr)/d;
      }
   }

   /**
    * Computes the eigenvalues and eigenvectors of an upper Hessenburg matrix.
    * This code was taken from Jama (http://math.nist.gov/javanumerics/jama).
    * where it was derived from the Algol procedures hqr2, by Martin and
    * Wilkinson, Handbook for Auto. Comp., Vol.ii-Linear Algebra, and the
    * corresponding Fortran subroutine in EISPACK.
    */
   private int hqr2 (
      double[] d, double[] e, double[] buf, int nn, MatrixNd V, int maxit) {

      double[] vbuf = null;
      int vw = 0;
      int hw = nn;

      if (V != null) {
         vbuf = V.buf;
         vw = V.width;
      }

      // Initialize

      int n = nn-1;
      int low = 0;
      int high = nn-1;
      double eps = Math.pow(2.0,-52.0);
      double exshift = 0.0;
      double p=0,q=0,r=0,s=0,z=0,t,w,x,y;

      // Store roots isolated by balanc and compute matrix norm
   
      double norm = 0.0;
      for (int i = 0; i < nn; i++) {
         if (i < low | i > high) {
            d[i] = buf[i*hw+i];
            e[i] = 0.0;
         }
         for (int j = Math.max(i-1,0); j < nn; j++) {
            norm = norm + Math.abs(buf[i*hw+j]);
         }
      }
   
      // Outer loop over eigenvalue index
   
      int iter = 0;
      while (n >= low) {
   
         // Look for single small sub-diagonal element
   
         int l = n;
         while (l > low) {
            s = Math.abs(buf[(l-1)*hw+l-1]) + Math.abs(buf[l*hw+l]);
            if (s == 0.0) {
               s = norm;
            }
            if (Math.abs(buf[l*hw+l-1]) < eps * s) {
               break;
            }
            l--;
         }
       
         // Check for convergence
         // One root found

         if (l == n) {
            buf[n*hw+n] = buf[n*hw+n] + exshift;
            d[n] = buf[n*hw+n];
            e[n] = 0.0;
            n--;
            iter = 0;
   
         // Two roots found
   
         } else if (l == n-1) {
            w = buf[n*hw+n-1] * buf[(n-1)*hw+n];
            p = (buf[(n-1)*hw+n-1] - buf[n*hw+n]) / 2.0;
            q = p * p + w;
            z = Math.sqrt(Math.abs(q));
            buf[n*hw+n] = buf[n*hw+n] + exshift;
            buf[(n-1)*hw+n-1] = buf[(n-1)*hw+n-1] + exshift;
            x = buf[n*hw+n];
   
            // Real pair
   
            if (q >= 0) {
               if (p >= 0) {
                  z = p + z;
               } else {
                  z = p - z;
               }
               d[n-1] = x + z;
               d[n] = d[n-1];
               if (z != 0.0) {
                  d[n] = x - w / z;
               }
               e[n-1] = 0.0;
               e[n] = 0.0;
               x = buf[n*hw+n-1];
               s = Math.abs(x) + Math.abs(z);
               p = x / s;
               q = z / s;
               r = Math.sqrt(p * p+q * q);
               p = p / r;
               q = q / r;
   
               // Row modification
   
               for (int j = n-1; j < nn; j++) {
                  z = buf[(n-1)*hw+j];
                  buf[(n-1)*hw+j] = q * z + p * buf[n*hw+j];
                  buf[n*hw+j] = q * buf[n*hw+j] - p * z;
               }
   
               // Column modification
   
               for (int i = 0; i <= n; i++) {
                  z = buf[i*hw+n-1];
                  buf[i*hw+n-1] = q * z + p * buf[i*hw+n];
                  buf[i*hw+n] = q * buf[i*hw+n] - p * z;
               }
   
               // Accumulate transformations
   
               if (vbuf != null) {
                  for (int i = low; i <= high; i++) {
                     z = vbuf[i*vw+n-1];
                     vbuf[i*vw+n-1] = q * z + p * vbuf[i*vw+n];
                     vbuf[i*vw+n] = q * vbuf[i*vw+n] - p * z;
                  }
               }
   
            // Complex pair
   
            } else {
               d[n-1] = x + p;
               d[n] = x + p;
               e[n-1] = z;
               e[n] = -z;
            }
            n = n - 2;
            iter = 0;
   
         // No convergence yet
   
         } else {
   
            // Form shift
   
            x = buf[n*hw+n];
            y = 0.0;
            w = 0.0;
            if (l < n) {
               y = buf[(n-1)*hw+n-1];
               w = buf[n*hw+n-1] * buf[(n-1)*hw+n];
            }
   
            // Wilkinson's original ad hoc shift
   
            if (iter == 10) {
               exshift += x;
               for (int i = low; i <= n; i++) {
                  buf[i*hw+i] -= x;
               }
               s = Math.abs(buf[n*hw+n-1]) + Math.abs(buf[(n-1)*hw+n-2]);
               x = y = 0.75 * s;
               w = -0.4375 * s * s;
            }

            // MATLAB's new ad hoc shift

            if (iter == 30) {
                s = (y - x) / 2.0;
                s = s * s + w;
                if (s > 0) {
                    s = Math.sqrt(s);
                    if (y < x) {
                       s = -s;
                    }
                    s = x - w / ((y - x) / 2.0 + s);
                    for (int i = low; i <= n; i++) {
                       buf[i*hw+i] -= s;
                    }
                    exshift += s;
                    x = y = w = 0.964;
                }
            }
   
            iter = iter + 1;   // (Could check iteration count here.)
            if (iter > maxit) {
               return 1;
            }

            // Look for two consecutive small sub-diagonal elements
   
            int m = n-2;
            while (m >= l) {
               z = buf[m*hw+m];
               r = x - z;
               s = y - z;
               p = (r * s - w) / buf[(m+1)*hw+m] + buf[m*hw+m+1];
               q = buf[(m+1)*hw+m+1] - z - r - s;
               r = buf[(m+2)*hw+m+1];
               s = Math.abs(p) + Math.abs(q) + Math.abs(r);
               p = p / s;
               q = q / s;
               r = r / s;
               if (m == l) {
                  break;
               }
               if (Math.abs(buf[m*hw+m-1]) * (Math.abs(q) + Math.abs(r)) <
                  eps * (Math.abs(p) * (Math.abs(buf[(m-1)*hw+m-1]) + Math.abs(z) +
                  Math.abs(buf[(m+1)*hw+m+1])))) {
                     break;
               }
               m--;
            }
   
            for (int i = m+2; i <= n; i++) {
               buf[i*hw+i-2] = 0.0;
               if (i > m+2) {
                  buf[i*hw+i-3] = 0.0;
               }
            }
   
            // Double QR step involving rows l:n and columns m:n
   

            for (int k = m; k <= n-1; k++) {
               boolean notlast = (k != n-1);
               if (k != m) {
                  p = buf[k*hw+k-1];
                  q = buf[(k+1)*hw+k-1];
                  r = (notlast ? buf[(k+2)*hw+k-1] : 0.0);
                  x = Math.abs(p) + Math.abs(q) + Math.abs(r);
                  if (x == 0.0) {
                      continue;
                  }
                  p = p / x;
                  q = q / x;
                  r = r / x;
               }

               s = Math.sqrt(p * p + q * q + r * r);
               if (p < 0) {
                  s = -s;
               }
               if (s != 0) {
                  if (k != m) {
                     buf[k*hw+k-1] = -s * x;
                  } else if (l != m) {
                     buf[k*hw+k-1] = -buf[k*hw+k-1];
                  }
                  p = p + s;
                  x = p / s;
                  y = q / s;
                  z = r / s;
                  q = q / p;
                  r = r / p;
   
                  // Row modification
   
                  for (int j = k; j < nn; j++) {
                     p = buf[k*hw+j] + q * buf[(k+1)*hw+j];
                     if (notlast) {
                        p = p + r * buf[(k+2)*hw+j];
                        buf[(k+2)*hw+j] = buf[(k+2)*hw+j] - p * z;
                     }
                     buf[k*hw+j] = buf[k*hw+j] - p * x;
                     buf[(k+1)*hw+j] = buf[(k+1)*hw+j] - p * y;
                  }
   
                  // Column modification
   
                  for (int i = 0; i <= Math.min(n,k+3); i++) {
                     p = x * buf[i*hw+k] + y * buf[i*hw+k+1];
                     if (notlast) {
                        p = p + z * buf[i*hw+k+2];
                        buf[i*hw+k+2] = buf[i*hw+k+2] - p * r;
                     }
                     buf[i*hw+k] = buf[i*hw+k] - p;
                     buf[i*hw+k+1] = buf[i*hw+k+1] - p * q;
                  }
   
                  // Accumulate transformations
   
                  if (vbuf != null) {
                     for (int i = low; i <= high; i++) {
                        p = x * vbuf[i*vw+k] + y * vbuf[i*vw+k+1];
                        if (notlast) {
                           p = p + z * vbuf[i*vw+k+2];
                           vbuf[i*vw+k+2] = vbuf[i*vw+k+2] - p * r;
                        }
                        vbuf[i*vw+k] = vbuf[i*vw+k] - p;
                        vbuf[i*vw+k+1] = vbuf[i*vw+k+1] - p * q;
                     }
                  }
               }  // (s != 0)
            }  // k loop
         }  // check convergence
      }  // while (n >= low)
      
      // Backsubstitute to find vectors of upper triangular form

      if (norm == 0.0) {
         return 0;
      }
   
      for (n = nn-1; n >= 0; n--) {
         p = d[n];
         q = e[n];
   
         // Real vector
   
         if (q == 0) {
            int l = n;
            buf[n*hw+n] = 1.0;
            for (int i = n-1; i >= 0; i--) {
               w = buf[i*hw+i] - p;
               r = 0.0;
               for (int j = l; j <= n; j++) {
                  r = r + buf[i*hw+j] * buf[j*hw+n];
               }
               if (e[i] < 0.0) {
                  z = w;
                  s = r;
               } else {
                  l = i;
                  if (e[i] == 0.0) {
                     if (w != 0.0) {
                        buf[i*hw+n] = -r / w;
                     } else {
                        buf[i*hw+n] = -r / (eps * norm);
                     }
   
                  // Solve real equations
   
                  } else {
                     x = buf[i*hw+i+1];
                     y = buf[(i+1)*hw+i];
                     q = (d[i] - p) * (d[i] - p) + e[i] * e[i];
                     t = (x * s - z * r) / q;
                     buf[i*hw+n] = t;
                     if (Math.abs(x) > Math.abs(z)) {
                        buf[(i+1)*hw+n] = (-r - w * t) / x;
                     } else {
                        buf[(i+1)*hw+n] = (-s - y * t) / z;
                     }
                  }
   
                  // Overflow control
   
                  t = Math.abs(buf[i*hw+n]);
                  if ((eps * t) * t > 1) {
                     for (int j = i; j <= n; j++) {
                        buf[j*hw+n] = buf[j*hw+n] / t;
                     }
                  }
               }
            }
   
         // Complex vector
   
         } else if (q < 0) {
            int l = n-1;

            // Last vector component imaginary so matrix is triangular
   
            if (Math.abs(buf[n*hw+n-1]) > Math.abs(buf[(n-1)*hw+n])) {
               buf[(n-1)*hw+n-1] = q / buf[n*hw+n-1];
               buf[(n-1)*hw+n] = -(buf[n*hw+n] - p) / buf[n*hw+n-1];
            } else {
               cdiv(0.0,-buf[(n-1)*hw+n],buf[(n-1)*hw+n-1]-p,q);
               buf[(n-1)*hw+n-1] = cdivr;
               buf[(n-1)*hw+n] = cdivi;
            }
            buf[n*hw+n-1] = 0.0;
            buf[n*hw+n] = 1.0;
            for (int i = n-2; i >= 0; i--) {
               double ra,sa,vr,vi;
               ra = 0.0;
               sa = 0.0;
               for (int j = l; j <= n; j++) {
                  ra = ra + buf[i*hw+j] * buf[j*hw+n-1];
                  sa = sa + buf[i*hw+j] * buf[j*hw+n];
               }
               w = buf[i*hw+i] - p;
   
               if (e[i] < 0.0) {
                  z = w;
                  r = ra;
                  s = sa;
               } else {
                  l = i;
                  if (e[i] == 0) {
                     cdiv(-ra,-sa,w,q);
                     buf[i*hw+n-1] = cdivr;
                     buf[i*hw+n] = cdivi;
                  } else {
   
                     // Solve complex equations
   
                     x = buf[i*hw+i+1];
                     y = buf[(i+1)*hw+i];
                     vr = (d[i] - p) * (d[i] - p) + e[i] * e[i] - q * q;
                     vi = (d[i] - p) * 2.0 * q;
                     if (vr == 0.0 & vi == 0.0) {
                        vr = eps * norm * (Math.abs(w) + Math.abs(q) +
                        Math.abs(x) + Math.abs(y) + Math.abs(z));
                     }
                     cdiv(x*r-z*ra+q*sa,x*s-z*sa-q*ra,vr,vi);
                     buf[i*hw+n-1] = cdivr;
                     buf[i*hw+n] = cdivi;
                     if (Math.abs(x) > (Math.abs(z) + Math.abs(q))) {
                        buf[(i+1)*hw+n-1] = (-ra - w * buf[i*hw+n-1] + q * buf[i*hw+n]) / x;
                        buf[(i+1)*hw+n] = (-sa - w * buf[i*hw+n] - q * buf[i*hw+n-1]) / x;
                     } else {
                        cdiv(-r-y*buf[i*hw+n-1],-s-y*buf[i*hw+n],z,q);
                        buf[(i+1)*hw+n-1] = cdivr;
                        buf[(i+1)*hw+n] = cdivi;
                     }
                  }
   
                  // Overflow control

                  t = Math.max(Math.abs(buf[i*hw+n-1]),Math.abs(buf[i*hw+n]));
                  if ((eps * t) * t > 1) {
                     for (int j = i; j <= n; j++) {
                        buf[j*hw+n-1] = buf[j*hw+n-1] / t;
                        buf[j*hw+n] = buf[j*hw+n] / t;
                     }
                  }
               }
            }
         }
      }

      if (vbuf != null) {
         // Vectors of isolated roots
            for (int i = 0; i < nn; i++) {
            if (i < low | i > high) {
               for (int j = i; j < nn; j++) {
                  vbuf[i*vw+j] = buf[i*hw+j];
               }
            }
         }
         // Back transformation to get eigenvectors of original matrix
         for (int j = nn-1; j >= low; j--) {
            for (int i = low; i <= high; i++) {
               z = 0.0;
               for (int k = low; k <= Math.min(j,high); k++) {
                  z = z + vbuf[i*vw+k] * buf[k*hw+j];
               }
               vbuf[i*vw+j] = z;
            }
         }
      }

      return 0;      
   }

   public static void main (String[] args) {
      MatrixNd M = new MatrixNd (2,2);
      EigenDecomposition ed = new EigenDecomposition(M);
      System.out.println ("eigs=" + ed.getEigReal());

   }
}
