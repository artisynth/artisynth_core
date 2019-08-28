/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Random;

import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A symmetric 3 x 3 matrix with the elements stored as explicit fields.
 */
public class SymmetricMatrix3d extends Matrix3dBase {
   private static final long serialVersionUID = 1L;
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double tol = 100 * DOUBLE_PREC;

   /**
    * Global identity matrix. Should not be modified.
    */
   public static final SymmetricMatrix3d IDENTITY =
      new SymmetricMatrix3d(1, 1, 1, 0, 0, 0);

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final SymmetricMatrix3d ZERO = new SymmetricMatrix3d();

   /**
    * Creates a symmetric matrix and initializes it to zero.
    */
   public SymmetricMatrix3d() {
   }

   /**
    * Creates a symmetric matrix and initializes its elements from an array of
    * doubles. Values in the array should be stored using row-major order, so
    * that element <code>(i,j)</code> is stored at location
    * <code>i*colSize()+j</code>. Only the diagonal and upper off-diagonal
    * values are used; the lower off-diagonal elements are copied from the upper
    * off-diagonal elements.
    * 
    * @param vals
    * array from which values are copied
    */
   public SymmetricMatrix3d (double[] vals) {
      set (vals);
   }

   /**
    * Creates a symmetric matrix and initializes its elements from the diagonal
    * and upper off-diagonal elements of M.
    * 
    * @param M
    * matrix whose values are to be copied.
    */
   public SymmetricMatrix3d (Matrix3dBase M) {
      set (M);
   }

   /**
    * Creates a symmetric matrix and initializes its elements from the specified
    * diagonal and uppper off-diagonal values.
    * 
    * @param m00
    * element (0,0)
    * @param m11
    * element (1,1)
    * @param m22
    * element (2,2)
    * @param m01
    * element (0,1)
    * @param m02
    * element (0,2)
    * @param m12
    * element (1,2)
    */
   public SymmetricMatrix3d (double m00, double m11, double m22, double m01,
   double m02, double m12) {
      set (m00, m11, m22, m01, m02, m12);
   }

   /**
    * Sets the elements of this matrix from the the diagonal and upper
    * off-diagonal elements of M.
    * 
    * @param M
    * matrix whose values are to be copied.
    */
   public void set (Matrix3dBase M) {
      this.m00 = M.m00;
      this.m11 = M.m11;
      this.m22 = M.m22;
      this.m01 = this.m10 = M.m01;
      this.m02 = this.m20 = M.m02;
      this.m12 = this.m21 = M.m12;
   }

   /**
    * Sets the elements of this matrix from an array of doubles. The elements in
    * the array should be stored using row-major order, so that element
    * <code>(i,j)</code> is stored at location <code>i*colSize()+j</code>.
    * Only the diagonal and upper off-diaginal values are used; the lower
    * off-diagonal elements are copied from the upper off-diagonal elements.
    * 
    * @param vals
    * array from which values are copied
    */
   public void set (double[] vals) {
      this.m00 = vals[0 * 3 + 0];
      this.m11 = vals[1 * 3 + 1];
      this.m22 = vals[2 * 3 + 2];
      this.m01 = this.m10 = vals[0 * 3 + 1];
      this.m02 = this.m20 = vals[0 * 3 + 2];
      this.m12 = this.m21 = vals[1 * 3 + 2];
   }

   /**
    * Sets the values of this matrix from the specified diagonal and uppper
    * off-diagonal values.
    * 
    * @param m00
    * element (0,0)
    * @param m11
    * element (1,1)
    * @param m22
    * element (2,2)
    * @param m01
    * element (0,1)
    * @param m02
    * element (0,2)
    * @param m12
    * element (1,2)
    */
   public void set (
      double m00, double m11, double m22, double m01, double m02, double m12) {
      this.m00 = m00;
      this.m11 = m11;
      this.m22 = m22;
      this.m01 = this.m10 = m01;
      this.m02 = this.m20 = m02;
      this.m12 = this.m21 = m12;
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean invert() {
      return invert(this);
   }
   
   /**
    * {@inheritDoc}
    */
   public double fastInvert (Matrix3dBase M) {
      return super.fastInvert(M);
   }

   /**
    * {@inheritDoc}
    */
   public boolean invert(Matrix3dBase M) {
      return super.invert(M);
   }

   /** 
    * Sets this matrix to the symmetric part of M, defined by
    * <pre>
    * 1/2 (M + M')
    * </pre>
    * 
    * @param M matrix to take symmetric part of
    */   
   public void setSymmetric (Matrix3dBase M) {
      m00 = M.m00;
      m11 = M.m11;
      m22 = M.m22;

      m01 = 0.5*(M.m01+M.m10);
      m02 = 0.5*(M.m02+M.m20);
      m12 = 0.5*(M.m12+M.m21);

      m10 = m01;
      m20 = m02;
      m21 = m12;
   }

   /**
    * Computes M T M', where T is this matrix, and M' is the transpose of M, and
    * places the result in this matrix.
    * 
    * @param M
    * matrix to multiply by
    */
   public void mulLeftAndTransposeRight (Matrix3dBase M) {
      Matrix3dBase X = M;
      if (M == this) {
         X = new Matrix3d (M);
      }
      mul (X, this);
      mulTranspose (X);
      if (M == this) {
         set (X);
      }
   }

   /**
    * Computes M' T M, where T is this matrix, and M' is the transpose of M, and
    * places the result in this matrix.
    * 
    * @param M
    * matrix to multiply by
    */
   public void mulTransposeLeftAndRight (Matrix3dBase M) {
      Matrix3dBase X = M;
      if (M == this) {
         X = new Matrix3d (M);
      }
      mulTransposeLeft (X, this);
      mul (X);
      if (M == this) {
         set (X);
      }
   }

   /**
    * Computes M D M', where D is a diagonal matrix given be a vector, and
    * places the result in this matrix.
    * 
    * @param M left (and right) matrix to multiply diagonal by
    * @param diag diagonal matrix values for D
    */
   public void mulDiagTransposeRight (Matrix3dBase M, Vector3d diag) {
      Matrix3dBase X = M;
      if (M == this) {
         X = new Matrix3d (M);
      }
      m00 = X.m00*diag.x;
      m10 = X.m10*diag.x;
      m20 = X.m20*diag.x;

      m01 = X.m01*diag.y;
      m11 = X.m11*diag.y;
      m21 = X.m21*diag.y;

      m02 = X.m02*diag.z;
      m12 = X.m12*diag.z;
      m22 = X.m22*diag.z;
      mulTransposeRight (this, X);
      if (M == this) {
         set (X);
      }
   }

   /**
    * Computes M' M, where M is a supplied matrix and M' is its transpose, and
    * places the (symmetric) result in this matrix.
    * 
    * @param M
    * matrix to form product from
    */
   public void mulTransposeLeft (Matrix3dBase M) {
      double tmp00 = M.m00 * M.m00 + M.m10 * M.m10 + M.m20 * M.m20;
      double tmp01 = M.m00 * M.m01 + M.m10 * M.m11 + M.m20 * M.m21;
      double tmp02 = M.m00 * M.m02 + M.m10 * M.m12 + M.m20 * M.m22;

      double tmp11 = M.m01 * M.m01 + M.m11 * M.m11 + M.m21 * M.m21;
      double tmp12 = M.m01 * M.m02 + M.m11 * M.m12 + M.m21 * M.m22;

      double tmp22 = M.m02 * M.m02 + M.m12 * M.m12 + M.m22 * M.m22;

      m00 = tmp00;
      m01 = tmp01;
      m02 = tmp02;

      m10 = tmp01;
      m11 = tmp11;
      m12 = tmp12;

      m20 = tmp02;
      m21 = tmp12;
      m22 = tmp22;
   }

   /**
    * Computes M M', where M is a supplied matrix and M' is its transpose, and
    * places the (symmetric) result in this matrix.
    * 
    * @param M
    * matrix to form product from
    */
   public void mulTransposeRight (Matrix3dBase M) {

      double tmp00 = M.m00 * M.m00 + M.m01 * M.m01 + M.m02 * M.m02;
      double tmp01 = M.m00 * M.m10 + M.m01 * M.m11 + M.m02 * M.m12;
      double tmp02 = M.m00 * M.m20 + M.m01 * M.m21 + M.m02 * M.m22;

      double tmp11 = M.m10 * M.m10 + M.m11 * M.m11 + M.m12 * M.m12;
      double tmp12 = M.m10 * M.m20 + M.m11 * M.m21 + M.m12 * M.m22;

      double tmp22 = M.m20 * M.m20 + M.m21 * M.m21 + M.m22 * M.m22;

      m00 = tmp00;
      m01 = tmp01;
      m02 = tmp02;

      m10 = tmp01;
      m11 = tmp11;
      m12 = tmp12;

      m20 = tmp02;
      m21 = tmp12;
      m22 = tmp22;
   }
   
   /**
    * Computes the symmetric product (AB + B'A')/2 and places the result in this matrix
    * @param A left matrix
    * @param B right matrix
    */
   public void mulSymmetric(Matrix3dBase A, Matrix3dBase B) {
      
      m00 = A.m02*B.m20+A.m01*B.m10+A.m00*B.m00;
      m01 = (A.m02*B.m21+A.m12*B.m20+A.m01*B.m11+A.m11*B.m10+A.m00*B.m01+A.m10*B.m00)/2;
      m02 = (A.m02*B.m22+A.m22*B.m20+A.m01*B.m12+A.m21*B.m10+A.m00*B.m02+A.m20*B.m00)/2;
      
      m11 = A.m12*B.m21+A.m11*B.m11+A.m10*B.m01;
      m12 = (A.m12*B.m22+A.m22*B.m21+A.m11*B.m12+A.m21*B.m11+A.m10*B.m02+A.m20*B.m01)/2;
      
      m22 = A.m22*B.m22+A.m21*B.m12+A.m20*B.m02;
         
      // symmetric part
      m10 = m01;
      m20 = m02;
      m21 = m12;
   }
   
   /**
    * Computes the symmetric product (A'B + B'A)/2 and places the result in this matrix
    * @param A left matrix
    * @param B right matrix
    */
   public void mulTransposeLeftSymmetric(Matrix3dBase A, Matrix3dBase B) {
    
      m00 = A.m20*B.m20+A.m10*B.m10+A.m00*B.m00;
      m01 = (A.m20*B.m21+A.m21*B.m20+A.m10*B.m11+A.m11*B.m10+A.m00*B.m01+A.m01*B.m00)/2;
      m02 = (A.m20*B.m22+A.m22*B.m20+A.m10*B.m12+A.m12*B.m10+A.m00*B.m02+A.m02*B.m00)/2;
      
      m11 = A.m21*B.m21+A.m11*B.m11+A.m01*B.m01;
      m12 = (A.m21*B.m22+A.m22*B.m21+A.m11*B.m12+A.m12*B.m11+A.m01*B.m02+A.m02*B.m01)/2;
      
      m22 = A.m22*B.m22+A.m12*B.m12+A.m02*B.m02;
      
      // symmetric part
      m10 = m01;
      m20 = m02;
      m21 = m12;
   }
   
   /**
    * Computes the symmetric product (AB' + BA')/2 and places the result in this matrix
    * @param A left matrix
    * @param B right matrix
    */
   public void mulTransposeRightSymmetric(Matrix3dBase A, Matrix3dBase B) {
      
      m00 = A.m02*B.m02+A.m01*B.m01+A.m00*B.m00;
      m01 = (A.m02*B.m12+A.m01*B.m11+A.m00*B.m10+A.m12*B.m02+A.m11*B.m01+A.m10*B.m00)/2;
      m02 = (A.m02*B.m22+A.m01*B.m21+A.m00*B.m20+A.m22*B.m02+A.m21*B.m01+A.m20*B.m00)/2;

      m11 = A.m12*B.m12+A.m11*B.m11+A.m10*B.m10;
      m12 = (A.m12*B.m22+A.m11*B.m21+A.m10*B.m20+A.m22*B.m12+A.m21*B.m11+A.m20*B.m10)/2;

      m22 = A.m22*B.m22+A.m21*B.m21+A.m20*B.m20;
         
      // symmetric part
      m10 = m01;
      m20 = m02;
      m21 = m12;
   }

   /**
    * Adds matrix M1 to M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   public void add (SymmetricMatrix3d M1, SymmetricMatrix3d M2) {
      super.add (M1, M2);
   }

   /**
    * Adds this matrix to M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   public void add (SymmetricMatrix3d M1) {
      super.add (M1);
   }

   /**
    * Subtracts matrix M1 from M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   public void sub (SymmetricMatrix3d M1, SymmetricMatrix3d M2) {
      super.sub (M1, M2);
   }

   /**
    * Subtracts this matrix from M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   public void sub (SymmetricMatrix3d M1) {
      super.sub (M1);
   }

   /**
    * Scales the elements of this matrix by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s) {
      super.scale (s, this);
   }

   /**
    * Scales the elements of matrix M1 by <code>s</code> and places the
    * results in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled
    */
   public void scale (double s, SymmetricMatrix3d M1) {
      super.scale (s, M1);
   }

   /**
    * Computes s M1 + M2 and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled
    * @param M2
    * matrix to be added
    */
   public void scaledAdd(double s, SymmetricMatrix3d M1, SymmetricMatrix3d M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;
      m02 = s * M1.m02 + M2.m02;

      m11 = s * M1.m11 + M2.m11;
      m12 = s * M1.m12 + M2.m12;

      m22 = s * M1.m22 + M2.m22;

      m10 = m01;
      m20 = m02;
      m21 = m12;
   }

   /**
    * Computes s M1 and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled and added
    */

   public void scaledAdd(double s, SymmetricMatrix3d M1) {
      m00 += s * M1.m00;
      m01 += s * M1.m01;
      m02 += s * M1.m02;

      m11 += s * M1.m11;
      m12 += s * M1.m12;

      m22 += s * M1.m22;

      m10 = m01;
      m20 = m02;
      m21 = m12;
   }

   /**
    * Sets this matrix to the dyad
    * <pre>
    * a a^T.
    * </pre>
    * 
    * @param a dyad vector
    */
   public void dyad (Vector3d a) {
      outerProduct (a, a);
   }

   /**
    * Adds the dyad
    * <pre>
    * a a^T
    * </pre>
    * to this matrix.
    * 
    * @param a dyad vector
    */
   public void addDyad (Vector3d a) {
      addOuterProduct (a, a);
   }

   /**
    * Adds the scaled dyad
    * <pre>
    * s a a^T
    * </pre>
    * to this matrix.
    *
    * @param s scaling factor
    * @param a dyad vector
    */
   public void addScaledDyad (double s, Vector3d a) {
      addScaledOuterProduct (s, a, a);
   }

   /** 
    * Sets this matrix to the symmetric dyad
    * <pre>
    * a b^T + b^T a.
    * </pre>
    * 
    * @param a first dyad vector
    * @param b second dyad vector
    */
   public void symmetricDyad (Vector3d a, Vector3d b) {
      outerProduct (a, b);
      addOuterProduct (b, a);
   }

   /** 
    * Adds the symmetric dyad
    * <pre>
    * a b^T + b^T a
    * </pre>
    * to this matrix.
    * 
    * @param a first dyad vector
    * @param b second dyad vector
    */
   public void addSymmetricDyad (Vector3d a, Vector3d b) {
      addOuterProduct (a, b);
      addOuterProduct (b, a);
   }

   /** 
    * Adds the scaled symmetric dyad
    * <pre>
    * s (a b^T + b^T a)
    * </pre>
    * to this matrix.
    *
    * @param s scaling factor
    * @param a first dyad vector
    * @param b second dyad vector
    */
   public void addScaledSymmetricDyad (double s, Vector3d a, Vector3d b) {
      addScaledOuterProduct (s, a, b);
      addScaledOuterProduct (s, b, a);
   }

   /**
    * Sets this matrix to the negative of M1.
    * 
    * @param M1
    * matrix to negate
    */
   public void negate (SymmetricMatrix3d M1) {
      super.negate (M1);
   }

   /** 
    * Sets this matrix to the deviator of a given symmetric matrix,
    * where the deviator is defined by
    * <pre>
    *    M - trace(M)/3 I
    * </pre>
    * 
    * @param M matrix to compute the deviator for
    */
   public void deviator (SymmetricMatrix3d M) {
      double traceDiv3 = (M.m00 + M.m11 + M.m22)/3;
      m00 = M.m00 - traceDiv3;
      m11 = M.m11 - traceDiv3;
      m22 = M.m22 - traceDiv3;

      m01 = M.m01;
      m02 = M.m02;
      m12 = M.m12;

      m10 = M.m01;
      m20 = M.m02;
      m21 = M.m12;
   }

   /** 
    * Sets this matrix to its deviator.
    * 
    * @see #deviator(SymmetricMatrix3d)
    */
   public void deviator() {
      double traceDiv3 = (m00 + m11 + m22)/3;
      m00 -= traceDiv3;
      m11 -= traceDiv3;
      m22 -= traceDiv3;
   }

   /**
    * Sets the elements of this matrix to zero.
    */
   public void setZero() {
      super.setZero();
   }

   /**
    * Sets the elements of this matrix to uniformly distributed random values in
    * the range -0.5 (inclusive) to 0.5 (exclusive). Matrix symmetry is
    * preserved.
    */
   public void setRandom() {
      super.setRandom (-0.5, 0.5);
   }

   /**
    * Sets the elements of this matrix to uniformly distributed random values in
    * a specified range. Matrix symmetry is preserved.
    */
   public void setRandom (double lower, double upper) {
      super.setRandom (lower, upper);
      m10 = m01;
      m20 = m02;
      m21 = m12;
   }

   /**
    * Sets the elements of this matrix to uniformly distributed random values in
    * a specified range, using a supplied random number generator. Matrix
    * symmetry is preserved.
    */
   public void setRandom (double lower, double upper, Random generator) {
      super.setRandom (lower, upper, generator);
      m10 = m01;
      m20 = m02;
      m21 = m12;
   }

   /**
    * {@inheritDoc}
    */
   public void setDiagonal (Vector3d diag) {
      super.setDiagonal (diag);
   }

   /**
    * {@inheritDoc}
    */
   public void setDiagonal (double[] vals) {
      super.setDiagonal (vals);
   }

   private Vector3d m0;
   private Vector3d m1;
   private Vector3d m2;

   private Vector3d u0;
   private Vector3d u1;
   private Vector3d u2;

   private Vector3d vx;

   private void allocateSVDWorkSpace() {
      m0 = new Vector3d();
      m1 = new Vector3d();
      m2 = new Vector3d();

      u0 = new Vector3d();
      u1 = new Vector3d();
      u2 = new Vector3d();

      if (vx == null) {
         vx = new Vector3d();
      }
   }

   public void getEigenValues (double[] eig) {
      if (vx == null) {
         vx = new Vector3d();
      }
      getEigenValues (vx);
      eig[0] = vx.x;
      eig[1] = vx.y;
      eig[2] = vx.z;
   }

   /**
    * Quickly computes the eigenvalues of this symmetric matrix, as described
    * for {@link #getEigenValues(Vector3d,Matrix3dBase) getEigenValues}.
    * 
    * @param eig
    * resulting eigenvalues
    */
   public void getEigenValues (Vector3d eig) {
      getEigenValues (eig, null);
   }

   /**
    * Quickly computes the eigenvalues of this symmetric matrix. These are
    * computed from the cube roots of the characteristic polynomial, which is
    * fast at the expense of some accuracy.
    * 
    * @param eig
    * resulting eigenvalues
    * @param V
    * corresponding eigenvectors (optional)
    */
   public void getEigenValues_old (Vector3d eig, Matrix3dBase V) {
      // set up the coefficients of the cubic:
      double m01sqr = m01 * m01;
      double m02sqr = m02 * m02;
      double m12sqr = m12 * m12;

      double a1 = -(m00 + m11 + m22);
      double a2 = m00 * m22 + m11 * m22 + m00 * m11 - m01sqr - m02sqr - m12sqr;
      double a3 =
         m00 * m12sqr + m11 * m02sqr + m22 * m01sqr - 2 * m01 * m02 * m12 - m00
         * m11 * m22;
      double q = (a1 * a1 - 3 * a2) / 9;

      if (q < 0) // q could be slightly negative due to round off
      {
         q = 0;
      }
      double r = ((2 * a1 * a1 - 9 * a2) * a1 + 27 * a3) / 54;
      double q3rd = q * q * q;
      double theta;

      if (q3rd - r * r <= 0) { // then just assume r^2 == q^3
         theta = (r < 0 ? Math.PI : 0);
      }
      else {
         double arg = r / Math.sqrt (q3rd);
         if (arg > 1.0) {
            arg = 1.0;
         }
         else if (arg < -1.0) {
            arg = -1.0;
         }
         theta = Math.acos (arg);
      }
      double s = -2 * Math.sqrt (q);
      eig.x = s * Math.cos (theta / 3) - a1 / 3;
      eig.y = s * Math.cos ((theta + 2 * Math.PI) / 3) - a1 / 3;
      eig.z = s * Math.cos ((theta + 4 * Math.PI) / 3) - a1 / 3;

      if (V == null) { // no need to compute eigen vectors, so return;
         return;
      }

      if (m0 == null) {
         allocateSVDWorkSpace();
      }
      // Given the eigenvalues, we now need to solve for the
      // eigenvectors. The eigenvector associated with the
      // most distinct eigenvalue should be the best-conditioned
      // one to solve for, so we start with that.

      // sort the eigenvalues values from largest to smallest
      eig.sort();

      // set lam0 to be the most distinct eigenvalue, and
      // set lam1 to be the farthest eigenvalue from lam0
      double lam0, lam1, lam2;
      if ((eig.x - eig.y) >= (eig.y - eig.z)) { // first eigenvalue is most
                                                // distinct
         lam0 = eig.x;
         lam1 = eig.z;
      }
      else { // last eigenvalue is most distinct
         lam0 = eig.z;
         lam1 = eig.x;
      }

      double epsilon = 3 * DOUBLE_PREC * 1.01 * eig.oneNorm();

      // set [ m0 m1 m2 ] = (M - lam0 I).
      m0.x = m00 - lam0;
      m1.x = m01;
      m2.x = m02;
      m0.y = m01;
      m1.y = m11 - lam0;
      m2.y = m12;
      m0.z = m02;
      m1.z = m12;
      m2.z = m22 - lam0;

      // [ m0 m1 m2 ] should be singular, and so should define
      // a subspace of dimension 2 or less. Any vector perpendicular
      // to this subspace is an eigenvector associated with lam0.
      // To find a perpendicular, we start with the column that
      // has the largest norm, and then examine the cross products
      // with the adjacent columns.
      double max, lenSqr;
      Vector3d mx = m0;
      max = m0.normSquared();
      if ((lenSqr = m1.normSquared()) > max) {
         max = lenSqr;
         mx = m1;
      }
      if ((lenSqr = m2.normSquared()) > max) {
         max = lenSqr;
         mx = m2;
      }
      if (max <= epsilon * epsilon) { // then the subspace has dimension 0 and
                                       // any vector will do
         u0.set (1, 0, 0);
      }
      else {
         if (mx == m0) {
            bestPerpVector (u0, m0, m1, m2, epsilon);
         }
         else if (mx == m1) {
            bestPerpVector (u0, m1, m0, m2, epsilon);
         }
         else // mx == m2
         {
            bestPerpVector (u0, m2, m0, m1, epsilon);
         }
      }

      // System.out.println ("u0=" + u0);
      // Given the first eigenvector u0, we construct vectors u1
      // and u2 to be mutually perpendicular to each other
      // and to u0. These will then be rotated about u0
      // to find the correct eigenvectors.
      perpVector (u2, u0);
      u1.cross (u2, u0);

      // System.out.println ("u1=" + u1);
      // System.out.println ("u2=" + u2);

      // Now, we have
      // T [ lam0 0 0 ]
      // [u0 u1 u2] M [u0 u1 u2] = [ 0 a00 a01 ]
      // [ 0 a10 a11 ]
      // 
      // and so we wish to diagonalize the lower 2x2 submatrix using
      // T
      // [ c s ] [a00 a01] [ c s ]
      // [-s c ] [a10 a11] [-s c ]
      //
      double a00, a01, a11;
      double tau, t, c;

      // compute m1 = M u1
      m1.x = m00 * u1.x + m01 * u1.y + m02 * u1.z;
      m1.y = m10 * u1.x + m11 * u1.y + m12 * u1.z;
      m1.z = m20 * u1.x + m21 * u1.y + m22 * u1.z;

      // compute m2 = M u2
      m2.x = m00 * u2.x + m01 * u2.y + m02 * u2.z;
      m2.y = m10 * u2.x + m11 * u2.y + m12 * u2.z;
      m2.z = m20 * u2.x + m21 * u2.y + m22 * u2.z;

      a00 = u1.dot (m1);
      a01 = u1.dot (m2);
      a11 = u2.dot (m2);

      // System.out.println (a00 + " " + a01 + " " + a11);

      if (Math.abs (a01) <= epsilon) {
         c = 1;
         s = 0;
      }
      else {
         tau = (a11 - a00) / (2 * a01);
         // System.out.println ("tau=" + tau);
         t = 1 / (Math.abs (tau) + Math.sqrt (1 + tau * tau));
         if (tau < 0) {
            t = -t;
         }
         c = 1 / Math.sqrt (1 + t * t);
         s = t * c;
      }
      // System.out.println ("c=" + c + " s=" + s);

      // Compute the corresponding eigenvectors m1 and m2,
      // and recompute the corresponding eigenvalues

      m1.combine (c, u1, -s, u2);
      m2.combine (s, u1, c, u2);
      lam1 = c * c * a00 - 2 * s * c * a01 + s * s * a11;
      lam2 = c * c * a11 + 2 * s * c * a01 + s * s * a00;
      // lam1 = c*c*a00 - s*s*a11;
      // lam2 = c*c*a11 - s*s*a00;
      // System.out.println ("lam1=" + lam1 + " lam2=" + lam2);
      m0.set (u0);

      // The eigenvectors corresponding to
      // lam0, lam1, lam2 are now m0, m1, m2
      // 
      // Resort these in order in descreasing absolute value

      Vector3d[] vecs = { m0, m1, m2 };
      double[] lams = { lam0, lam1, lam2 };

      for (int i = 0; i < 2; i++) {
         for (int j = i + 1; j < 3; j++) {
            if (Math.abs (lams[j]) > Math.abs (lams[i])) {
               double l = lams[j];
               lams[j] = lams[i];
               lams[i] = l;
               Vector3d v = vecs[j];
               vecs[j] = vecs[i];
               vecs[i] = v;
            }
         }
      }

      V.m00 = vecs[0].x;
      V.m10 = vecs[0].y;
      V.m20 = vecs[0].z;
      V.m01 = vecs[1].x;
      V.m11 = vecs[1].y;
      V.m21 = vecs[1].z;
      V.m02 = vecs[2].x;
      V.m12 = vecs[2].y;
      V.m22 = vecs[2].z;

      eig.set (lams[0], lams[1], lams[2]);
   }

   private void perpVector (Vector3d vr, Vector3d v0) {
      // find a vector perpendicular to v0
      int i = v0.minAbsIndex();
      if (i == 0) {
         vr.x = 0;
         vr.y = v0.z;
         vr.z = -v0.y;
      }
      else if (i == 1) {
         vr.x = -v0.z;
         vr.y = 0;
         vr.z = v0.x;
      }
      else {
         vr.x = v0.y;
         vr.y = -v0.x;
         vr.z = 0;
      }
      vr.normalize();
   }

   private void bestPerpVector (
      Vector3d vr, Vector3d v0, Vector3d v1, Vector3d v2, double eps) {
      vr.cross (v0, v1);
      vx.cross (v0, v2);
      if (vx.normSquared() > vr.normSquared()) {
         vr.set (vx);
      }
      if (vr.normSquared() <= eps * eps) { // then the subspace has dimension
                                             // 1, and we just find
         // a good vector perp. to v0
         perpVector (vr, v0);
      }
      else {
         vr.normalize();
      }
   }

   /**
    * Quickly computes the singular value decomposition U S V' of this symmetric
    * matrix. This routine works by solving a cubic equation to obtain the
    * eigenvalues, and so is around 5 times faster than the corresponding
    * routine in <code>javax.vecmath</code>, though possibly at the expense
    * of some precision.
    * 
    * @param U
    * if non-null, used to store the first orthogonal matrix in the
    * decomposition.
    * @param sig
    * required parameter, used to store the 3 singular values of the matrix,
    * sorted from largest to smallest.
    * @param V
    * if non-null, used to store the second orthogonal matrix in the
    * decomposition (note that the returned value is V and not V').
    */
   public void getSVD (Matrix3dBase U, Vector3d sig, Matrix3dBase V) {
      if (m0 == null) {
         allocateSVDWorkSpace();
      }

      if (U == null && V == null) { // we only want the singular values, so just
                                    // get these from the
         // eigenvalues
         getEigenValues (sig);
         sig.sortAbsolute();
         sig.absolute();
         return;
      }
      else { // get both the eigenvalues and the eigenvectors
         if (U == null) {
            U = new Matrix3d();
         }
         getEigenValues (sig, U);

         // Take the absolute value of the eigenvectors to
         // get the singular values. The columns of V
         // are equal to the columns of U, except that they
         // are negated if the corresponding eigenvalue is
         // negative.

         if (sig.x < 0) {
            sig.x = -sig.x;
            if (V != null) {
               V.m00 = -U.m00;
               V.m10 = -U.m10;
               V.m20 = -U.m20;
            }
         }
         else {
            if (V != null) {
               V.m00 = U.m00;
               V.m10 = U.m10;
               V.m20 = U.m20;
            }
         }
         if (sig.y < 0) {
            sig.y = -sig.y;
            if (V != null) {
               V.m01 = -U.m01;
               V.m11 = -U.m11;
               V.m21 = -U.m21;
            }
         }
         else {
            if (V != null) {
               V.m01 = U.m01;
               V.m11 = U.m11;
               V.m21 = U.m21;
            }
         }
         if (sig.z < 0) {
            sig.z = -sig.z;
            if (V != null) {
               V.m02 = -U.m02;
               V.m12 = -U.m12;
               V.m22 = -U.m22;
            }
         }
         else {
            if (V != null) {
               V.m02 = U.m02;
               V.m12 = U.m12;
               V.m22 = U.m22;
            }
         }
      }
   }

   private final double ABS (double x) {
      return x >= 0 ? x : -x;
   }

   private void QRstep_3x3 (
      Matrix3dBase T, Matrix3dBase U, SVDecomposition3d svd) {
      double d, mu;
      double CS, CC, SS;

      double T_m01, T_m02;
      double T_m11, T_m12;

      d = (T.m11 - T.m22) / 2;
      if (d >= 0) {
         mu = T.m22 - T.m12 * T.m12 / (d + Math.sqrt (d * d + T.m12 * T.m12));
      }
      else {
         mu = T.m22 - T.m12 * T.m12 / (d - Math.sqrt (d * d + T.m12 * T.m12));
      }
      svd.givens (T.m00 - mu, T.m01);
      CC = svd.C * svd.C;
      CS = svd.C * svd.S;
      SS = svd.S * svd.S;

      T_m01 = (CC - SS) * T.m01 + CS * (T.m00 - T.m11);
      T_m11 = CC * T.m11 + 2 * CS * T.m01 + SS * T.m00;
      T_m02 = -svd.S * T.m12;
      T_m12 = svd.C * T.m12;
      T.m00 = CC * T.m00 - 2 * CS * T.m01 + SS * T.m11;

      // svd.rotateCols01 (T);
      // svd.rotateRows01 (T);
      if (U != null) {
         svd.rotateCols01 (U);
      }
      svd.givens (T_m01, T_m02);
      CC = svd.C * svd.C;
      CS = svd.C * svd.S;
      SS = svd.S * svd.S;

      T.m01 = svd.C * T_m01 - svd.S * T_m02;
      T.m11 = CC * T_m11 - 2 * CS * T_m12 + SS * T.m22;
      T.m12 = CC * T_m12 + CS * (T_m11 - T.m22) - SS * T_m12;
      T.m22 = SS * T_m11 + 2 * CS * T_m12 + CC * T.m22;

      // svd.rotateCols12 (T);
      // svd.rotateRows12 (T);
      if (U != null) {
         svd.rotateCols12 (U);
      }
   }

   private void QRstep_lower (
      Matrix3dBase T, Matrix3dBase U, SVDecomposition3d svd) {
      double d, mu;
      double CC, CS, SS;

      d = (T.m11 - T.m22) / 2;
      if (d >= 0) {
         mu = T.m22 - T.m12 * T.m12 / (d + Math.sqrt (d * d + T.m12 * T.m12));
      }
      else {
         mu = T.m22 - T.m12 * T.m12 / (d - Math.sqrt (d * d + T.m12 * T.m12));
      }
      svd.givens (T.m11 - mu, T.m12);
      CC = svd.C * svd.C;
      CS = svd.C * svd.S;
      SS = svd.S * svd.S;

      double T_m11 = T.m11;

      T.m11 = CC * T_m11 - 2 * CS * T.m12 + SS * T.m22;
      T.m22 = SS * T_m11 + 2 * CS * T.m12 + CC * T.m22;

      // svd.rotateCols12 (T);
      // svd.rotateRows12 (T);
      if (U != null) {
         svd.rotateCols12 (U);
      }
   }

   private void QRstep_upper (
      Matrix3dBase T, Matrix3dBase U, SVDecomposition3d svd) {
      double d, mu;
      double CC, CS, SS;

      d = (T.m00 - T.m11) / 2;
      if (d >= 0) {
         mu = T.m11 - T.m01 * T.m01 / (d + Math.sqrt (d * d + T.m01 * T.m01));
      }
      else {
         mu = T.m11 - T.m01 * T.m01 / (d - Math.sqrt (d * d + T.m01 * T.m01));
      }
      svd.givens (T.m00 - mu, T.m01);
      CC = svd.C * svd.C;
      CS = svd.C * svd.S;
      SS = svd.S * svd.S;

      double T_m00 = T.m00;

      T.m00 = CC * T_m00 - CS * (T.m01 + T.m01) + SS * T.m11;
      T.m11 = CC * T.m11 + CS * (T.m01 + T.m01) + SS * T_m00;

      // svd.rotateCols01 (T);
      // svd.rotateRows01 (T);
      if (U != null) {
         svd.rotateCols01 (U);
      }
   }

   public void getEigenValues (Vector3d eig, Matrix3dBase U) {

      Matrix3d T = new Matrix3d();
      SVDecomposition3d svd = new SVDecomposition3d();
      
      double SS, CC, CS;

      svd.givens (m01, m02);
      CC = svd.C * svd.C;
      CS = svd.C * svd.S;
      SS = svd.S * svd.S;

      T.m00 = m00;
      T.m01 = svd.C * m01 - svd.S * m02;
      T.m11 = CC * m11 - 2 * CS * m12 + SS * m22;
      T.m12 = CC * m12 + CS * (m11 - m22) - SS * m12;
      T.m22 = SS * m11 + 2 * CS * m12 + CC * m22;

      // svd.rotateCols12 (T);
      // svd.rotateRows12 (T);
      if (U != null) {
         U.m00 = 1;
         U.m10 = 0;
         U.m20 = 0;
         U.m01 = 0;
         U.m11 = svd.C;
         U.m21 = -svd.S;
         U.m02 = 0;
         U.m12 = svd.S;
         U.m22 = svd.C;
      }
      while (ABS (T.m12) > tol * (ABS (T.m11) + ABS (T.m22))) {
         if (ABS (T.m01) > tol * (ABS (T.m00) + ABS (T.m11))) {
            QRstep_3x3 (T, U, svd);
         }
         else {
            QRstep_lower (T, U, svd);
            eig.set (T.m00, T.m11, T.m22);
            return;
         }

      }
      QRstep_upper (T, U, svd);
      eig.set (T.m00, T.m11, T.m22);
   }

   /**
    * Computes the Cholesky decomposition of this matrix, which is assumed to be
    * positive definite. This decomposition takes the form <br>
    * M = L L' <br>
    * where M is the original matrix, L is a lower-triangular matrix and L'
    * denotes its transpose. symmetric matrix.
    * 
    * @param L
    * returns the lower triangular part of the decomposition
    * @throws IllegalArgumentException
    * if the matrix is not positive definite.
    */
   public void getCholesky (Matrix3d L) {
      double tmp;
      // get an approximate norm
      double anorm = 0;

      if (m00 < 0 || m11 < 0 || m22 < 0) {
         throw new IllegalArgumentException (
            "Matrix not symmetric positive definite");
      }
      anorm = m00;
      if (m11 > anorm) {
         anorm = m11;
      }
      if (m22 > anorm) {
         anorm = m22;
      }

      L.set (this);

      tmp = Math.sqrt (L.m00);
      if (anorm + tmp == anorm) {
         throw new IllegalArgumentException (
            "Matrix not symmetric positive definite");
      }
      else {
         L.m00 /= tmp;
         L.m10 /= tmp;
         L.m20 /= tmp;
      }

      L.m11 -= L.m10 * L.m10;
      L.m21 -= L.m20 * L.m10;

      if (L.m11 < 0) {
         throw new IllegalArgumentException (
            "Matrix not symmetric positive definite");
      }
      else {
         tmp = Math.sqrt (L.m11);
         if (anorm + tmp == anorm) {
            throw new IllegalArgumentException (
               "Matrix not symmetric positive definite");
         }
         else {
            L.m11 /= tmp;
            L.m21 /= tmp;
         }
      }

      L.m22 -= (L.m20 * L.m20 + L.m21 * L.m21);

      if (L.m22 < 0) {
         throw new IllegalArgumentException (
            "Matrix not symmetric positive definite");
      }
      else {
         tmp = Math.sqrt (L.m22);
         if (anorm + tmp == anorm) {
            throw new IllegalArgumentException (
               "Matrix not symmetric positive definite");
         }
         else {
            L.m22 /= tmp;
         }
      }

      L.m01 = L.m02 = L.m12 = 0;
   }

   public void writeAsVector (PrintWriter pw, NumberFormat fmt)
      throws IOException {

      pw.print (fmt.format(m00)+" ");
      pw.print (fmt.format(m11)+" ");
      pw.print (fmt.format(m22)+" ");
      pw.print (fmt.format(m01)+" ");
      pw.print (fmt.format(m02)+" ");
      pw.print (fmt.format(m12));
   }
   
   public void scanAsVector (ReaderTokenizer rtok) throws IOException {
      boolean bracketed = false;
      if (rtok.nextToken() == '[') {
         bracketed = true;
      }
      else {
         rtok.pushBack();
      }
      m00 = rtok.scanNumber();
      m11 = rtok.scanNumber();
      m22 = rtok.scanNumber();
      m01 = rtok.scanNumber();
      m02 = rtok.scanNumber();
      m12 = rtok.scanNumber();
      m10 = m01;
      m20 = m02;
      m21 = m12;
      if (bracketed) {
         rtok.scanToken (']');
      }
   }
   
   @Override
   public SymmetricMatrix3d clone () {
      return (SymmetricMatrix3d)super.clone ();
   }

}
