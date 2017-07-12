/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Constructs the singular value decomposition (SVD) of a 3x3 matrix. This takes
 * the form <br>
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
public class SVDecomposition3d {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double tol = 100 * DOUBLE_PREC;

   private double sgndet;
   private double sig0, sig1, sig2;

   private Matrix3d U_;
   private Matrix3d V_;
   private Matrix3d B;

   protected double S;
   protected double C;

   protected boolean myCanonical = true;

   /**
    * The iteration limit for computing the SVD.
    */
   public static final int DEFAULT_ITERATION_LIMIT = 10;

   private int iterLimit = DEFAULT_ITERATION_LIMIT;

   /**
    * Ensure that singular values are positive and sorted from largest to
    * smallest. The default value for this is <code>true</code>. If
    * <code>false</code>, then no effort will be made to ensure that the
    * values are positive or sorted.
    */
   public void setCanonical (boolean enable) {
      myCanonical = enable;
   }

   public boolean isCanonical() {
      return myCanonical;
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
    * Creates an SVDecomposition with the matrices U and V, and initializes it
    * to the SVD for the matrix M. Either U and V or V may be omitted if the
    * user does not desire these values.
    * 
    * @param U
    * optional U matrix for this SVD
    * @param V
    * optional V matrix for this SVD
    * @param M
    * matrix to perform the SVD on
    */
   public SVDecomposition3d (Matrix3d U, Matrix3d V, Matrix3dBase M) {
      this.U_ = U;
      this.V_ = V;
      factor (M);
   }

   /**
    * Creates an SVDecomposition with the matrices U and V. Either U and V or V
    * may be omitted if the user does not desire these values.
    * 
    * @param U
    * optional U matrix for this SVD
    * @param V
    * optional V matrix for this SVD
    */
   public SVDecomposition3d (Matrix3d U, Matrix3d V) {
      this.U_ = U;
      this.V_ = V;
   }

   /**
    * Creates an SVDecomposition with U and V internally allocated and
    * initializes it to the SVD for the matrix M.
    * 
    * @param M
    * matrix to perform the SVD on
    */
   public SVDecomposition3d (Matrix3dBase M) {
      this();
      factor (M);
   }

   /**
    * Creates an uninitialized SVDecomposition with U and V internally
    * allocated.
    */
   public SVDecomposition3d() {
      this (new Matrix3d(), new Matrix3d());
   }

   protected void givens (double a, double b) {
      double tau;

      if (b == 0) {
         C = 1;
         S = 0;
      }
      else {
         double absB = (b >= 0 ? b : -b);
         double absA = (a >= 0 ? a : -a);
         if (absB > absA) {
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

   /**
    * Post-multiplies columns 0 and 1 of Q by the rotation
    * 
    * <pre>
    *    [  C  S  ]
    *    [        ]
    *    [ -S  C  ]
    * </pre>
    */
   protected void rotateCols01 (Matrix3dBase Q) {
      double mx0, mx1;

      mx0 = Q.m00;
      mx1 = Q.m01;
      Q.m00 = C * mx0 - S * mx1;
      Q.m01 = S * mx0 + C * mx1;

      mx0 = Q.m10;
      mx1 = Q.m11;
      Q.m10 = C * mx0 - S * mx1;
      Q.m11 = S * mx0 + C * mx1;

      mx0 = Q.m20;
      mx1 = Q.m21;
      Q.m20 = C * mx0 - S * mx1;
      Q.m21 = S * mx0 + C * mx1;
   }

   /**
    * Post-multiplies columns 0 and 2 of Q by the rotation
    * 
    * <pre>
    *    [  C  S  ]
    *    [        ]
    *    [ -S  C  ]
    * </pre>
    */
   protected void rotateCols02 (Matrix3dBase Q) {
      double mx0, mx2;

      mx0 = Q.m00;
      mx2 = Q.m02;
      Q.m00 = C * mx0 - S * mx2;
      Q.m02 = S * mx0 + C * mx2;

      mx0 = Q.m10;
      mx2 = Q.m12;
      Q.m10 = C * mx0 - S * mx2;
      Q.m12 = S * mx0 + C * mx2;

      mx0 = Q.m20;
      mx2 = Q.m22;
      Q.m20 = C * mx0 - S * mx2;
      Q.m22 = S * mx0 + C * mx2;
   }

   /**
    * Post-multiplies columns 1 and 0 of Q by the rotation
    * 
    * <pre>
    *    [  C  S  ]
    *    [        ]
    *    [ -S  C  ]
    * </pre>
    */
   protected void rotateCols10 (Matrix3dBase Q) {
      double mx0, mx1;

      mx0 = Q.m00;
      mx1 = Q.m01;
      Q.m00 = C * mx0 + S * mx1;
      Q.m01 = -S * mx0 + C * mx1;

      mx0 = Q.m10;
      mx1 = Q.m11;
      Q.m10 = C * mx0 + S * mx1;
      Q.m11 = -S * mx0 + C * mx1;

      mx0 = Q.m20;
      mx1 = Q.m21;
      Q.m20 = C * mx0 + S * mx1;
      Q.m21 = -S * mx0 + C * mx1;
   }

   /**
    * Post-multiplies columns 1 and 2 of Q by the rotation
    * 
    * <pre>
    *    [  C  S  ]
    *    [        ]
    *    [ -S  C  ]
    * </pre>
    */
   protected void rotateCols12 (Matrix3dBase Q) {
      double mx1, mx2;

      mx1 = Q.m01;
      mx2 = Q.m02;
      Q.m01 = C * mx1 - S * mx2;
      Q.m02 = S * mx1 + C * mx2;

      mx1 = Q.m11;
      mx2 = Q.m12;
      Q.m11 = C * mx1 - S * mx2;
      Q.m12 = S * mx1 + C * mx2;

      mx1 = Q.m21;
      mx2 = Q.m22;
      Q.m21 = C * mx1 - S * mx2;
      Q.m22 = S * mx1 + C * mx2;
   }

   /**
    * Post-multiplies columns 2 and 1 of Q by the rotation
    * 
    * <pre>
    *    [  C  S  ]
    *    [        ]
    *    [ -S  C  ]
    * </pre>
    */
   protected void rotateCols21 (Matrix3dBase Q) {
      double mx1, mx2;

      mx1 = Q.m01;
      mx2 = Q.m02;
      Q.m01 = C * mx1 + S * mx2;
      Q.m02 = -S * mx1 + C * mx2;

      mx1 = Q.m11;
      mx2 = Q.m12;
      Q.m11 = C * mx1 + S * mx2;
      Q.m12 = -S * mx1 + C * mx2;

      mx1 = Q.m21;
      mx2 = Q.m22;
      Q.m21 = C * mx1 + S * mx2;
      Q.m22 = -S * mx1 + C * mx2;
   }

   /**
    * Post-multiplies columns 2 and 0 of Q by the rotation
    * 
    * <pre>
    *    [  C  S  ]
    *    [        ]
    *    [ -S  C  ]
    * </pre>
    */
   protected void rotateCols20 (Matrix3dBase Q) {
      double mx0, mx2;

      mx0 = Q.m00;
      mx2 = Q.m02;
      Q.m00 = C * mx0 + S * mx2;
      Q.m02 = -S * mx0 + C * mx2;

      mx0 = Q.m10;
      mx2 = Q.m12;
      Q.m10 = C * mx0 + S * mx2;
      Q.m12 = -S * mx0 + C * mx2;

      mx0 = Q.m20;
      mx2 = Q.m22;
      Q.m20 = C * mx0 + S * mx2;
      Q.m22 = -S * mx0 + C * mx2;
   }

   /**
    * Pre-multiplies rows 0 and 1 of Q by the rotation
    * 
    * <pre>
    *    [  C  -S  ]
    *    [         ]
    *    [  S   C  ]
    * </pre>
    */
   protected void rotateRows01 (Matrix3dBase Q) {
      double m0x, m1x;

      m0x = Q.m00;
      m1x = Q.m10;
      Q.m00 = C * m0x - S * m1x;
      Q.m10 = S * m0x + C * m1x;

      m0x = Q.m01;
      m1x = Q.m11;
      Q.m01 = C * m0x - S * m1x;
      Q.m11 = S * m0x + C * m1x;

      m0x = Q.m02;
      m1x = Q.m12;
      Q.m02 = C * m0x - S * m1x;
      Q.m12 = S * m0x + C * m1x;
   }

   /**
    * Pre-multiplies rows 0 and 2 of Q by the rotation
    * 
    * <pre>
    *    [  C  -S  ]
    *    [         ]
    *    [  S   C  ]
    * </pre>
    */
   protected void rotateRows02 (Matrix3dBase Q) {
      double m0x, m2x;

      m0x = Q.m00;
      m2x = Q.m20;
      Q.m00 = C * m0x - S * m2x;
      Q.m20 = S * m0x + C * m2x;

      m0x = Q.m01;
      m2x = Q.m21;
      Q.m01 = C * m0x - S * m2x;
      Q.m21 = S * m0x + C * m2x;

      m0x = Q.m02;
      m2x = Q.m22;
      Q.m02 = C * m0x - S * m2x;
      Q.m22 = S * m0x + C * m2x;
   }

   /**
    * Pre-multiplies rows 1 and 2 of Q by the rotation
    * 
    * <pre>
    *    [  C  -S  ]
    *    [         ]
    *    [  S   C  ]
    * </pre>
    */
   protected void rotateRows12 (Matrix3dBase Q) {
      double m1x, m2x;

      m1x = Q.m10;
      m2x = Q.m20;
      Q.m10 = C * m1x - S * m2x;
      Q.m20 = S * m1x + C * m2x;

      m1x = Q.m11;
      m2x = Q.m21;
      Q.m11 = C * m1x - S * m2x;
      Q.m21 = S * m1x + C * m2x;

      m1x = Q.m12;
      m2x = Q.m22;
      Q.m12 = C * m1x - S * m2x;
      Q.m22 = S * m1x + C * m2x;
   }

   /**
    * Returns a sutiable norm for a bidiagonal matrix.
    * 
    */
   protected double bidiagNorm (Matrix3d B) {
      // use the infinity norm for now
      // returns the largest row sum of the absolute value\
      double max, sum;
      max = ABS (B.m00) + ABS (B.m01);
      sum = ABS (B.m11) + ABS (B.m12);
      if (sum > max) {
         max = sum;
      }
      sum = ABS (B.m22);
      if (sum > max) {
         max = sum;
      }
      return max;
   }

   protected void reorderUandV (int col0, int col1, int col2) {
      if (U_ != null) {
         U_.permuteColumns (col0, col1, col2);
      }
      if (V_ != null) {
         V_.permuteColumns (col0, col1, col2);
      }
   }

   private void sortResults() {
      double tmp0, tmp1, tmp2;

      sgndet = 1;

      if (myCanonical && B.m00 < 0) {
         if (V_ != null) {
            V_.m00 = -V_.m00;
            V_.m10 = -V_.m10;
            V_.m20 = -V_.m20;
         }
         sgndet = -sgndet;
         tmp0 = -B.m00;
      }
      else {
         tmp0 = B.m00;
      }
      if (myCanonical && B.m11 < 0) {
         if (V_ != null) {
            V_.m01 = -V_.m01;
            V_.m11 = -V_.m11;
            V_.m21 = -V_.m21;
         }
         sgndet = -sgndet;
         tmp1 = -B.m11;
      }
      else {
         tmp1 = B.m11;
      }
      if (myCanonical && B.m22 < 0) {
         if (V_ != null) {
            V_.m02 = -V_.m02;
            V_.m12 = -V_.m12;
            V_.m22 = -V_.m22;
         }
         sgndet = -sgndet;
         tmp2 = -B.m22;
      }
      else {
         tmp2 = B.m22;
      }

      if (myCanonical) { // sort singular values

         if (tmp0 >= tmp1) {
            if (tmp1 >= tmp2) // order is 0, 1, 2, nothing to do
            {
               sig0 = tmp0;
               sig1 = tmp1;
               sig2 = tmp2;
            }
            else if (tmp0 >= tmp2) // order is 0, 2, 1
            {
               sig0 = tmp0;
               sig1 = tmp2;
               sig2 = tmp1;
               reorderUandV (0, 2, 1);
            }
            else // order is 2, 0, 1
            {
               sig0 = tmp2;
               sig1 = tmp0;
               sig2 = tmp1;
               reorderUandV (2, 0, 1);
            }
         }
         else {
            if (tmp0 >= tmp2) // order is 1, 0, 2
            {
               sig0 = tmp1;
               sig1 = tmp0;
               sig2 = tmp2;
               reorderUandV (1, 0, 2);
            }
            else if (tmp1 >= tmp2) // order is 1, 2, 0
            {
               sig0 = tmp1;
               sig1 = tmp2;
               sig2 = tmp0;
               reorderUandV (1, 2, 0);
            }
            else // order is 2, 1, 0
            {
               sig0 = tmp2;
               sig1 = tmp1;
               sig2 = tmp0;
               reorderUandV (2, 1, 0);
            }
         }
      }
      else {
         sig0 = tmp0;
         sig1 = tmp1;
         sig2 = tmp2;
      }
   }

   /**
    * Peforms an SVD on the Matrix3dBase M.
    * 
    * @param M
    * matrix to perform the SVD on
    */
   public void factor (Matrix3dBase M) {
      // initialize the calculation

      if (B == null) {
         B = new Matrix3d();
      }
      B.set (M);
      bidiagonalize (B);
      // get infinity norm of bidiagonalized matrix
      double anorm = bidiagNorm (B);
      if (anorm == 0) {
         sig0 = 0;
         sig1 = 0;
         sig2 = 0;
         U_.setIdentity();
         V_.setIdentity();
         return;
      }

      int icnt = 0;

      int maxIter = iterLimit * 3;

      // System.out.println ("B=\n" + B.toString("%24g"));

      while (ABS (B.m12) > tol * (ABS (B.m11) + ABS (B.m22))) {
         if (B.m11 + anorm == anorm) {
            B.m11 = 0;
         }
         if (B.m11 != 0 && B.m01 != 0) { // use full 3x3 matrix
            if (anorm + B.m00 == anorm) {
               B.m00 = 0;
               // System.out.println ("zeroRow 3x3");
               zeroRow_3x3();
            }
            else {
               takeStep_3x3();
               // System.out.println ("takeStep 3x3");
            }
         }
         else { // use lower right 2x2 matrix
            if (B.m11 == 0) {
               // System.out.println ("zeroRow lower 2x2");
               zeroRow_lower_2x2();
            }
            else {
               // System.out.println ("takeStep lower 2x2");
               takeStep_lower_2x2();
            }
         }

         // System.out.println ("B=\n" + B.toString("%24g"));
         if (++icnt >= maxIter) {
            throw new NumericalException (
               "SVD failed to converge after " + icnt + " iterations");
         }
      }
      B.m12 = 0;

      while (ABS (B.m01) > tol * (ABS (B.m00) + ABS (B.m11))) {
         if (anorm + B.m00 == anorm) {
            B.m00 = 0;
            zeroRow_upper_2x2();
         }
         else {
            takeStep_upper_2x2();
         }
         if (++icnt >= maxIter) {
            throw new NumericalException (
               "SVD failed to converge after " + icnt + " iterations");
         }
      }
      sortResults();

   }
   
   /**
    * Peforms an SVD on the Matrix3dBase M, without throwing an exception
    * max iterations being surpassed
    * 
    * @param M
    * matrix to perform the SVD on
    * @return true if successful, false otherwise
    */
   public boolean factorSafe (Matrix3dBase M) {
      // initialize the calculation

      if (B == null) {
         B = new Matrix3d();
      }
      B.set (M);
      bidiagonalize (B);
      // get infinity norm of bidiagonalized matrix
      double anorm = bidiagNorm (B);
      if (anorm == 0) {
         sig0 = 0;
         sig1 = 0;
         sig2 = 0;
         U_.setIdentity();
         V_.setIdentity();
         return true;
      }

      int icnt = 0;

      int maxIter = iterLimit * 3;

      // System.out.println ("B=\n" + B.toString("%24g"));

      boolean converged = true;
      while (ABS (B.m12) > tol * (ABS (B.m11) + ABS (B.m22))) {
         if (B.m11 + anorm == anorm) {
            B.m11 = 0;
         }
         if (B.m11 != 0 && B.m01 != 0) { // use full 3x3 matrix
            if (anorm + B.m00 == anorm) {
               B.m00 = 0;
               // System.out.println ("zeroRow 3x3");
               zeroRow_3x3();
            }
            else {
               takeStep_3x3();
               // System.out.println ("takeStep 3x3");
            }
         }
         else { // use lower right 2x2 matrix
            if (B.m11 == 0) {
               // System.out.println ("zeroRow lower 2x2");
               zeroRow_lower_2x2();
            }
            else {
               // System.out.println ("takeStep lower 2x2");
               takeStep_lower_2x2();
            }
         }

         // System.out.println ("B=\n" + B.toString("%24g"));
         if (++icnt >= maxIter) {
            converged = false;
            break;
         }
      }
      B.m12 = 0;

      while (ABS (B.m01) > tol * (ABS (B.m00) + ABS (B.m11))) {
         if (anorm + B.m00 == anorm) {
            B.m00 = 0;
            zeroRow_upper_2x2();
         }
         else {
            takeStep_upper_2x2();
         }
         if (++icnt >= maxIter) {
            converged = false;
         }
      }
      sortResults();
      return converged;
   }

   /**
    * Zero the top row of the full 3x3 matrix.
    */
   private void zeroRow_3x3() {
      double al1, ak1, ak2;

      al1 = B.m01;
      ak1 = B.m11;
      givens (ak1, al1);
      if (U_ != null) {
         rotateCols10 (U_);
      }
      B.m11 = ak1 * C - al1 * S;
      ak2 = B.m12;
      B.m12 = ak2 * C;
      al1 = ak2 * S;
      ak1 = B.m22;
      givens (ak1, al1);
      if (U_ != null) { // rotateCols20 (U_);
         rotateCols20 (U_);
      }
      B.m22 = ak1 * C - al1 * S;
      B.m01 = 0;
   }

   /**
    * Zero the top row of the lower 2x2 diagonal matrix.
    */
   private void zeroRow_lower_2x2() {
      double al1, ak1;

      al1 = B.m12;
      ak1 = B.m22;
      givens (ak1, al1);
      if (U_ != null) {
         rotateCols21 (U_);
      }
      B.m22 = ak1 * C - al1 * S;
      B.m12 = 0;
   }

   /**
    * Zero the top row of the upper 2x2 diagonal matrix.
    */
   private void zeroRow_upper_2x2() {
      double al1, ak1;

      al1 = B.m01;
      ak1 = B.m11;
      givens (ak1, al1);
      if (U_ != null) {
         rotateCols10 (U_);
      }
      B.m11 = ak1 * C - al1 * S;
      B.m01 = 0;
   }

   /**
    * Apply a Golub Kahan SVD step to the upper 2x2 diagonal matrix.
    */
   private void takeStep_upper_2x2() {
      double tmm, tmn, tnn;
      double d, mu;

      tmm = B.m00 * B.m00;
      tmn = B.m00 * B.m01;
      tnn = B.m01 * B.m01 + B.m11 * B.m11;

      d = (tmm - tnn) / 2;
      mu = tnn - tmn * tmn / (d + SGN (d) * Math.sqrt (d * d + tmn * tmn));

      double B_m00, B_m01;
      double B_m10, B_m11;

      givens (B.m00 * B.m00 - mu, B.m00 * B.m01);
      B_m00 = C * B.m00 - S * B.m01;
      B_m10 = -S * B.m11;
      B_m01 = S * B.m00 + C * B.m01;
      B_m11 = +C * B.m11;
      if (V_ != null) {
         rotateCols01 (V_);
      }
      givens (B_m00, B_m10);
      B.m00 = C * B_m00 - S * B_m10;
      B.m01 = C * B_m01 - S * B_m11;
      B.m11 = S * B_m01 + C * B_m11;
      if (U_ != null) {
         rotateCols01 (U_);
      }
   }

   /**
    * Apply a Golub Kahan SVD step to the lower 2x2 diagonal matrix.
    */
   private void takeStep_lower_2x2() {
      double tmm, tmn, tnn;
      double d, mu;

      tmm = B.m11 * B.m11;
      tmn = B.m11 * B.m12;
      tnn = B.m12 * B.m12 + B.m22 * B.m22;

      d = (tmm - tnn) / 2;
      mu = tnn - tmn * tmn / (d + SGN (d) * Math.sqrt (d * d + tmn * tmn));

      double B_m11, B_m12;
      double B_m21, B_m22;

      givens (B.m11 * B.m11 - mu, B.m11 * B.m12);
      B_m11 = C * B.m11 - S * B.m12;
      B_m21 = -S * B.m22;
      B_m12 = S * B.m11 + C * B.m12;
      B_m22 = C * B.m22;
      if (V_ != null) {
         rotateCols12 (V_);
      }
      givens (B_m11, B_m21);
      B.m11 = C * B_m11 - S * B_m21;
      B.m12 = C * B_m12 - S * B_m22;
      B.m22 = S * B_m12 + C * B_m22;
      if (U_ != null) {
         rotateCols12 (U_);
      }
   }

   /**
    * Apply a Golub Kahan SVD step to the full 3x3 matrix.
    */
   private void takeStep_3x3() {
      double tmm, tmn, tnn;
      double d, mu;

      // There are three possibilities here:
      // ib == 0 and nn == 2
      // ib == 0 and nn == 3
      // ib == 1 and nn == 2

      double A_m00, A_m01, A_m02;
      double A_m11, A_m12;
      double A_m22;

      A_m00 = B.m00;
      A_m01 = B.m01;
      A_m11 = B.m11;
      A_m12 = B.m12;
      A_m22 = B.m22;

      tmm = A_m11 * A_m11 + A_m01 * A_m01;
      tmn = A_m11 * A_m12;
      tnn = A_m12 * A_m12 + A_m22 * A_m22;

      d = (tmm - tnn) / 2;
      mu = tnn - tmn * tmn / (d + SGN (d) * Math.sqrt (d * d + tmn * tmn));

      double B_m00, B_m01;
      double B_m10, B_m11, B_m12;
      double B_m21, B_m22;

      givens (A_m00 * A_m00 - mu, A_m00 * A_m01);
      B_m00 = C * A_m00 - S * A_m01;
      B_m10 = -S * A_m11;
      B_m01 = S * A_m00 + C * A_m01;
      B_m11 = C * A_m11;
      if (V_ != null) {
         rotateCols01 (V_);
      }
      givens (B_m00, B_m10);
      B.m00 = C * B_m00 - S * B_m10;
      A_m01 = C * B_m01 - S * B_m11;
      A_m02 = -S * A_m12;
      A_m11 = S * B_m01 + C * B_m11;
      A_m12 = C * A_m12;
      if (U_ != null) {
         rotateCols01 (U_);
      }
      givens (A_m01, A_m02);
      B.m01 = C * A_m01 - S * A_m02;
      B_m11 = C * A_m11 - S * A_m12;
      B_m21 = -S * A_m22;
      B_m12 = S * A_m11 + C * A_m12;
      B_m22 = C * A_m22;
      if (V_ != null) {
         rotateCols12 (V_);
      }
      givens (B_m11, B_m21);
      B.m11 = C * B_m11 - S * B_m21;
      B.m12 = C * B_m12 - S * B_m22;
      B.m22 = S * B_m12 + C * B_m22;
      if (U_ != null) {
         rotateCols12 (U_);
      }
   }

//   /**
//    * Apply a Golub Kahan SVD step to the full 3x3 matrix.
//    */
//   private void takeStep_xxx() {
//      double tmm, tmn, tnn;
//      double d, mu;
//
//      tmm = B.m11 * B.m11 + B.m01 * B.m01;
//      tmn = B.m11 * B.m12;
//      tnn = B.m12 * B.m12 + B.m22 * B.m22;
//
//      d = (tmm - tnn) / 2;
//      mu = tnn - tmn * tmn / (d + SGN (d) * Math.sqrt (d * d + tmn * tmn));
//
//      givens (B.m00 * B.m00 - mu, B.m00 * B.m01);
//      rotateCols01 (B);
//      if (V_ != null) {
//         rotateCols01 (V_);
//      }
//      givens (B.m00, B.m10);
//      rotateRows01 (B);
//      if (U_ != null) {
//         rotateCols01 (U_);
//      }
//      givens (B.m01, B.m02);
//      rotateCols12 (B);
//      if (V_ != null) {
//         rotateCols12 (V_);
//      }
//      givens (B.m11, B.m21);
//      rotateRows12 (B);
//      if (U_ != null) {
//         rotateCols12 (U_);
//      }
//   }

   /**
    * Sets the U matrix associated with this SVD. If a previous U matrix has
    * been set, its contents will be copied into this new matrix. Setting the
    * matrix to null will disable the calculation of U.
    * 
    * @param U
    * new U matrix for this SVD
    */
   public void setU (Matrix3d U) {
      if (this.U_ != null) {
         U.set (this.U_);
      }
      this.U_ = U;
   }

   /**
    * Returns the U matrix associated with this SVD, or null if no such matrix
    * has been set.
    * 
    * @return U matrix
    */
   public Matrix3d getU() {
      return U_;
   }

   /**
    * Sets the V matrix associated with this SVD. If a previous V matrix has
    * been set, its contents will be copied into this new matrix. Setting the
    * matrix to null will disable the calculation of V.
    * 
    * @param V
    * new V matrix for this SVD
    */
   public void setV (Matrix3d V) {
      if (this.V_ != null) {
         V.set (this.V_);
      }
      this.V_ = V;
   }

   /**
    * Returns the V matrix associated with this SVD, or null if no such matrix
    * has been set.
    * 
    * @return V matrix
    */
   public Matrix3d getV() {
      return V_;
   }

   /**
    * Gets the singular values associated with this SVD.
    * 
    * @param svec
    * vector giving the diagonal elements of S
    */
   public void getS (Vector3d svec) {
      svec.set (sig0, sig1, sig2);
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
      if (B == null) {
         throw new ImproperStateException ("SVD not initialized");
      }
      return sig0 / sig2;
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
      if (B == null) {
         throw new ImproperStateException ("SVD not initialized");
      }
      return sig0;
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
      if (B == null) {
         throw new ImproperStateException ("SVD not initialized");
      }
      return sgndet * (sig0 * sig1 * sig2);
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
    * if this decomposition is uninitialized, or if U and V are not present
    */
   public boolean solve (Vector3d x, Vector3d b) {
      if (B == null) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (U_ == null || V_ == null) {
         throw new ImproperStateException ("U and V not present");
      }
      //System.out.println ("sig=" + sig0+" "+sig1+" "+sig2+"\n");
      x.mulTranspose (U_, b);
      x.x /= sig0;
      x.y /= sig1;
      x.z /= sig2;
      x.mul (V_, x);
      //System.out.println ("x=" + x);
      return sig2 != 0;
   }

   /**
    * Computes the inverse of the original matrix M associated this SVD, and
    * places the result in R.
    * 
    * @param R
    * matrix in which the inverse is stored
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized, or if U and V are not present
    */
   public boolean inverse (Matrix3d R) {
      if (B == null) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (U_ == null || V_ == null) {
         throw new ImproperStateException ("U and V not present");
      }
      R.transpose (U_);
      R.mulRows (1 / sig0, 1 / sig1, 1 / sig2);
      R.mul (V_, R);
      return sig2 != 0;
   }
   
   /**
    * Computes the pseudo inverse of the original matrix M associated this SVD, and
    * places the result in R.
    * 
    * @param R
    * matrix in which the inverse is stored
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized, or if U and V are not present
    */
   public boolean pseudoInverse (Matrix3d R) {
      if (B == null) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (U_ == null || V_ == null) {
         throw new ImproperStateException ("U and V not present");
      }
      R.transpose (U_);
      
      double s0=0, s1=0, s2=0;
      if (sig0 != 0) {
         s0 = 1.0/sig0;
      }
      if (sig1 != 0) {
         s1 = 1.0/sig1;
      }
      if (sig2 != 0) {
         s2 = 1.0/sig2;
      }
      R.mulRows (s0, s1, s2);
      R.mul (V_, R);
      return sig2 != 0;
   }
   
   public Vector3d[] getNullSpace(double eps) {
      
      if (B == null) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (U_ == null || V_ == null) {
         throw new ImproperStateException ("U and V not present");
      }
      
      // count how many null vectors
      int nullCount=0;
      if (Math.abs(sig0)<eps) {
         nullCount++;
      }
      if (Math.abs(sig1)<eps) {
         nullCount++;
      }
      if (Math.abs(sig2)<eps) {
         nullCount++;
      }
      
      Vector3d nullSpace[] = new Vector3d[nullCount];
      
      nullCount = 0;
      if (Math.abs(sig0)<eps) {
         nullSpace[nullCount] = new Vector3d(); 
         V_.getColumn(0, nullSpace[nullCount]);
         nullCount++;
      }
      if (Math.abs(sig1)<eps) {
         nullSpace[nullCount] = new Vector3d(); 
         V_.getColumn(1, nullSpace[nullCount]);
         nullCount++;
      }
      if (Math.abs(sig2)<eps) {
         nullSpace[nullCount] = new Vector3d(); 
         V_.getColumn(2, nullSpace[nullCount]);
         nullCount++;
      }
      
      return nullSpace;
      
   }
   
   public Vector3d[] getRangeSpace(double eps) {
      if (B == null) {
         throw new ImproperStateException ("SVD not initialized");
      }
      if (U_ == null || V_ == null) {
         throw new ImproperStateException ("U and V not present");
      }
            
      // count how many null vectors
      int rangeCount=0;
      if (Math.abs(sig0)>eps) {
         rangeCount++;
      }
      if (Math.abs(sig1)>eps) {
         rangeCount++;
      }
      if (Math.abs(sig2)>eps) {
         rangeCount++;
      }
      
      Vector3d rangeSpace[] = new Vector3d[rangeCount];
      
      rangeCount = 0;
      if (Math.abs(sig0)<eps) {
         rangeSpace[rangeCount] = new Vector3d(); 
         V_.getColumn(0, rangeSpace[rangeCount]);
         rangeCount++;
      }
      if (Math.abs(sig1)<eps) {
         rangeSpace[rangeCount] = new Vector3d(); 
         V_.getColumn(1, rangeSpace[rangeCount]);
         rangeCount++;
      }
      if (Math.abs(sig2)<eps) {
         rangeSpace[rangeCount] = new Vector3d(); 
         V_.getColumn(2, rangeSpace[rangeCount]);
         rangeCount++;
      }
      
      return rangeSpace;
      
   }
   
   public Vector3d[] getNullSpace() {
      return getNullSpace(0);
   }
   
   public Vector3d[] getRangeSpace() {
      return getRangeSpace(0);
   }

   private final double SGN (double x) {
      return x >= 0 ? 1 : -1;
   }

   private final double ABS (double x) {
      return x >= 0 ? x : -x;
   }

   // /**
   // * Computes P A, where P is the Householder matrix for
   // * the vector (1 v1 v2), and returns the result in this
   // * matrix.
   // */
   // private void rowHouseMul (double v1, double v2, Matrix3d A)
   // {
   // double beta = -2/(1 + v1*v1 + v2*v2);
   // double w0 = beta*(A.m00 + A.m10*v1 + A.m20*v2);
   // double w1 = beta*(A.m01 + A.m11*v1 + A.m21*v2);
   // double w2 = beta*(A.m02 + A.m12*v1 + A.m22*v2);

   // A.m00 += w0;
   // A.m10 += v1*w0;
   // A.m20 += v2*w0;

   // A.m01 += w1;
   // A.m11 += v1*w1;
   // A.m21 += v2*w1;

   // A.m02 += w2;
   // A.m12 += v1*w2;
   // A.m22 += v2*w2;
   // }

   // /**
   // * Computes A P, where P is a Householder matrix for the vector (1 v1)
   // * that operates on the right 3x2 submatrix of A, and returns the
   // * result in this matrix.
   // */
   // private void colHouseMul (Matrix3d A, double v2)
   // {
   // double beta = -2/(1 + v2*v2);
   // double w0 = beta*(A.m01 + A.m02*v2);
   // double w1 = beta*(A.m11 + A.m12*v2);
   // double w2 = beta*(A.m21 + A.m22*v2);

   // A.m01 += w0;
   // A.m11 += w1;
   // A.m21 += w2;

   // A.m02 += w0*v2;
   // A.m12 += w1*v2;
   // A.m22 += w2*v2;
   // }

   // /**
   // * Computes P A, where P is a Householder matrix for the vector (1 v1)
   // * that operates on the lower-right 2x2 submatrix of A, and returns the
   // * result in this matrix.
   // */
   // private void rowHouseMul (double v2, Matrix3d A)
   // {
   // double beta = -2/(1 + v2*v2);
   // double w0 = beta*(A.m10 + A.m20*v2);
   // double w1 = beta*(A.m11 + A.m21*v2);
   // double w2 = beta*(A.m12 + A.m22*v2);

   // A.m10 += w0;
   // A.m20 += v2*w0;

   // A.m11 += w1;
   // A.m21 += v2*w1;

   // A.m12 += w2;
   // A.m22 += v2*w2;
   // }

   // private void bidiagonalize_old (Matrix3d B)
   // {
   // double v1, v2;

   // // compute HouseHolder vector to reduce B00 - B20
   // double mu = Math.sqrt(B.m00*B.m00 + B.m10*B.m10 + B.m20*B.m20);
   // v1 = B.m10;
   // v2 = B.m20;
   // if (mu != 0)
   // { double beta = B.m00 + (B.m00 >= 0 ? mu : -mu);
   // v1 /= beta;
   // v2 /= beta;
   // }
   // // and reduce ...
   // rowHouseMul (v1, v2, B);
   // if (U_ != null)
   // { U_.setIdentity();
   // rowHouseMul (v1, v2, U_);
   // }

   // // compute HouseHolder vector to reduce B01 - B02
   // mu = Math.sqrt(B.m01*B.m01 + B.m02*B.m02);
   // v2 = B.m02;
   // if (mu != 0)
   // { double beta = B.m01 + (B.m01 >= 0 ? mu : -mu);
   // v2 /= beta;
   // }
   // // and reduce ...
   // colHouseMul (B, v2);
   // if (V_ != null)
   // { V_.setIdentity();
   // colHouseMul (V_, v2);
   // }

   // // compute HouseHolder vector to reduce B11 - B21
   // mu = Math.sqrt(B.m11*B.m11 + B.m21*B.m21);
   // v2 = B.m21;
   // if (mu != 0)
   // { double beta = B.m11 + (B.m11 >= 0 ? mu : -mu);
   // v2 /= beta;
   // }
   // // and reduce ...
   // rowHouseMul (v2, B);
   // if (U_ != null)
   // { rowHouseMul (v2, U_);
   // U_.transpose();
   // }

   // // Just to make sure :-)
   // B.m10 = 0;
   // B.m20 = 0;
   // B.m21 = 0;
   // B.m02 = 0;
   // }

   private void bidiagonalize (Matrix3d B) {
      double C1, S1;
      givens (B.m00, B.m20);
      rotateRows02 (B);
      C1 = C;
      S1 = S;
      givens (B.m00, B.m10);
      rotateRows01 (B);
      if (U_ != null) {
         U_.m00 = C1 * C;
         U_.m10 = -S;
         U_.m20 = -S1 * C;

         U_.m01 = C1 * S;
         U_.m11 = C;
         U_.m21 = -S1 * S;

         U_.m02 = S1;
         U_.m12 = 0;
         U_.m22 = C1;
         // rotateCols01 (U_);
      }
      givens (B.m01, B.m02);
      rotateCols12 (B);
      if (V_ != null) {
         V_.m00 = 1;
         V_.m10 = 0;
         V_.m20 = 0;

         V_.m01 = 0;
         V_.m11 = C;
         V_.m21 = -S;

         V_.m02 = 0;
         V_.m12 = S;
         V_.m22 = C;
         // rotateCols12 (V_);
      }
      givens (B.m11, B.m21);
      rotateRows12 (B);
      if (U_ != null) {
         rotateCols12 (U_);
      }

      // Just to make sure :-)
      B.m10 = 0;
      B.m20 = 0;
      B.m21 = 0;
      B.m02 = 0;
   }

   /**
    * Factors a 3 x 3 matrix F into a right polar decomposition
    * <pre>
    * F = R P
    * </pre>
    * where R is a rotation matrix (with determinant 1) and P is a
    * symmetric matrix.
    */
   public void polarDecomposition (
      Matrix3dBase R, Matrix3d P, Matrix3dBase F) {

      Vector3d sig = new Vector3d();
      if (!doPolarDecomposition (R, sig, F)) {
         if (P != null) {
            P.set (F);
         }
      }
      else {
         if (P != null) {
            // place the symmetric part in P
            P.set (getV());
            P.mulCols (sig);
            P.mulTransposeRight (P, getV());
         }
      }
   }

   /**
    * Factors a 3 x 3 matrix F into a right polar decomposition
    * <pre>
    * F = R P
    * </pre>
    * where R is a rotation matrix (with determinant 1) and P is a
    * symmetric matrix.
    */
   public void polarDecomposition (
      Matrix3dBase R, SymmetricMatrix3d P, Matrix3dBase F) {

      Vector3d sig = new Vector3d();
      if (doPolarDecomposition (R, sig, F)) {
         if (P != null) {
            // place the symmetric part in P
            P.mulDiagTransposeRight (getV(), sig);
         }
      }
   }

   /**
    * Factors a 3 x 3 matrix F into a left polar decomposition
    * <pre>
    * F = P R
    * </pre>
    * where R is a rotation matrix (with determinant 1) and P is a
    * symmetric matrix.
    */
   public void leftPolarDecomposition (
      Matrix3d P, Matrix3dBase R, Matrix3dBase F) {

      Vector3d sig = new Vector3d();
      if (!doPolarDecomposition (R, sig, F)) {
         if (P != null) {
            P.set (F);
         }
      }
      else {
         if (P != null) {
            // place the symmetric part in P
            P.set (getU());
            P.mulCols (sig);
            P.mulTransposeRight (P, getU());
         }
      }
   }

//   /**
//    * Factors a 3 x 3 matrix F into a left polar decomposition
//    * <pre>
//    * F = P R
//    * </pre>
//    * where R is a rotation matrix (with determinant 1) and P is a
//    * symmetric matrix.
//    */
//   public void leftPolarDecomposition (
//      SymmetricMatrix3d P, Matrix3dBase R, Matrix3dBase F) {
//
//      Vector3d sig = new Vector3d();
//      if (doPolarDecomposition (R, sig, F)) {
//         if (P != null) {
//            // place the symmetric part in P
//            P.mulDiagTransposeRight (getU(), sig);
//         }
//      }
//   }

   private boolean doPolarDecomposition (
      Matrix3dBase R, Vector3d sig, Matrix3dBase F) {

      try {
         factor (F);
      }
      catch (Exception e) {
         System.out.println (
            "SVDecomposition3d.polarDecomposition: F=\n" + F.toString ("%g"));
         if (R != null) {
            R.setIdentity();
         }
         return false;
      }
      Matrix3d U = getU();
      Matrix3d V = getV();
      getS (sig);

      double detU = U.orthogonalDeterminant();
      double detV = V.orthogonalDeterminant();
      
      if (detV * detU < 0) { /* then one is negative and the other positive */
         // negate last column of U. Note that the effect of this
         // is identical to negating the last column of V.
         U.m02 = -U.m02;
         U.m12 = -U.m12;
         U.m22 = -U.m22;  
         // flip the sign of the last singular value
         sig.z = -sig.z;
         // do this for the internal value as well
         sig2 = -sig2;
      }

      if (R != null) {
         R.mulTransposeRight (U, V);
      }
      return true;
   }

}
