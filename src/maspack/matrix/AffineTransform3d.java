/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.ArrayList;

import maspack.util.NumberFormat;

/**
 * A 4 x 4 matrix which implements general 3D affine transformations. The matrix
 * has the specific form
 * 
 * <pre>
 *     [  A   p  ]
 * M = [         ]
 *     [  0   1  ]
 * </pre>
 * 
 * where A is a 3 x 3 matrix and p is a 3-vector. In homogeneous coordinates,
 * this implements an affine transform of the form <br>
 * A v + b <br>
 * 
 * <p>
 * In this class, the fields A and p are exposed, and users can manipulate them
 * as desired. This allows us to minimize the number of methds in the
 * AffineTransform3d class itself.
 */

public class AffineTransform3d extends AffineTransform3dBase {
   private static final long serialVersionUID = 42L;

   /**
    * Global identity transform. Should not be modified.
    */
   public static final AffineTransform3d IDENTITY = new AffineTransform3d();

   /**
    * Specifies a string representation of this transformation as a 3 x 4 matrix
    * (i.e., with the 4th row ommitted).
    */
   public static final int MATRIX_3X4_STRING = 1;

   /**
    * Specifies a string representation of this transformation as a 4 x 4
    * matrix.
    */
   public static final int MATRIX_4X4_STRING = 2;
   
   /**
    * Matrix component.
    */
   public final Matrix3d A;

   /**
    * Vector component.
    */
   public final Vector3d p;

   /**
    * Creates an AffineTransform3d and initializes it to the identity.
    */
   public AffineTransform3d () {
      A = new Matrix3d();
      p = new Vector3d();
      M = A;
      b = p;
      setIdentity();
   }

   /**
    * Creates an AffineTransform3d and initializes its components to the
    * specified values.
    * 
    * @param A
    * value for the A matrix
    * @param p
    * value for the p vector
    */
   public AffineTransform3d (Matrix3d A, Vector3d p) {
      this.A = new Matrix3d(A);
      this.p = new Vector3d(p);
      M = this.A;
      b = this.p;
   }

   public AffineTransform3d (AffineTransform3dBase X) {
      this.A = new Matrix3d(X.M);
      this.p = new Vector3d(X.b);
      M = this.A;
      b = this.p;
   }

   public AffineTransform3d (
      double m00, double m01, double m02, double px,
      double m10, double m11, double m12, double py, 
      double m20, double m21, double m22, double pz) {
      
      this.A = new Matrix3d(m00, m01, m02, m10, m11, m12, m20, m21, m22);
      this.p = new Vector3d(px, py, pz);
      M = this.A;
      b = this.p;
   }

   /**
    * Multiplies this transformation transformation X and places the result in
    * this transformation.
    * 
    * @param X
    * right-hand transformation
    */
   public void mul(AffineTransform3dBase X) {
      super.mul(this, X);
   }

   /**
    * {@inheritDoc}
    */
   public void mul(AffineTransform3dBase X1, AffineTransform3dBase X2) {
      super.mul(X1, X2);
   }

   /**
    * Multiplies this transformation by the inverse of transformation X and
    * places the result in this transformation.
    * 
    * @param X
    * right-hand transformation
    * @return false if X is singular
    */
   public boolean mulInverse(AffineTransform3dBase X) {
      return super.mulInverseRight(this, X);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseRight(
      AffineTransform3dBase X1, AffineTransform3dBase X2) {
      return super.mulInverseRight(X1, X2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseLeft(
      AffineTransform3dBase X1, AffineTransform3dBase X2) {
      return super.mulInverseLeft(X1, X2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseBoth(
      AffineTransform3dBase X1, AffineTransform3dBase X2) {
      return super.mulInverseBoth(X1, X2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean invert() {
      return super.invert();
   }

   /**
    * {@inheritDoc}
    */
   public boolean invert(AffineTransform3dBase X) {
      return super.invert(X);
   }

   /**
    * Sets this affine transform to the rigid body transform described by X.
    * 
    * @param X
    * rigid body transform to copy
    */
   public void set(RigidTransform3d X) {
      M.set(X.M);
      b.set(X.b);
   }

   /**
    * Sets A to diagonal scaling matrix.
    * 
    * @param sx
    * x scale factor
    * @param sy
    * y scale factor
    * @param sz
    * z scale factor
    */
   public void setScaling(double sx, double sy, double sz) {
      A.m00 = sx;
      A.m10 = 0;
      A.m20 = 0;

      A.m01 = 0;
      A.m11 = sy;
      A.m21 = 0;

      A.m02 = 0;
      A.m12 = 0;
      A.m22 = sz;
   }



   /**
    * Scales the columns of A by the specified amounts. This is equivalent to
    * post-multiplying A by a diagonal matrix. Note that the resulting scaling
    * effect will be in addition to any previous scaling effect.
    * 
    * @param sx
    * x scale factor
    * @param sy
    * y scale factor
    * @param sz
    * z scale factor
    */
   public void applyScaling(double sx, double sy, double sz) {
      A.m00 *= sx;
      A.m10 *= sx;
      A.m20 *= sx;

      A.m01 *= sy;
      A.m11 *= sy;
      A.m21 *= sy;

      A.m02 *= sz;
      A.m12 *= sz;
      A.m22 *= sz;
   }

   /**
    * Factors the A matrix of this transformation into rotation, scale, and
    * shear components, so that
    * 
    * <pre>
    * A = R S X
    * </pre>
    * 
    * where R is a right-handed rotation matrix, S is a diagonal scaling matrix,
    * and X is a shear matrix of the form
    * 
    * <pre>
    *     [ 1   xy  xz ]
    * X = [ 0   1   yz ]
    *     [ 0   0   1  ]
    * </pre>
    * 
    * @param R
    * if non-null, returns the rotation matrix
    * @param scale
    * if non-null, returns the diagonal components of S
    * @param shear
    * if non-null, returns the components xy, xz, yz of X
    */
   public void factorA(RotationMatrix3d R, Vector3d scale, Vector3d shear) {

      double EPS = 1e-10;
      if (R == null) {
         R = new RotationMatrix3d();
      }

      // start by doing a QR decomposition of A. This could be done much more
      // efficiently if we unrolled the compution for the 3x3 case ...
      // MatrixNd Q = new MatrixNd(3,3);
      // MatrixNd U = new MatrixNd(3,3);
      // QRDecomposition QR = new QRDecomposition();

      // QR.factor (A);
      // QR.get (Q, U);
      // R.set (Q);

      // System.out.println ("Q=\n" + Q.toString("%10.6f"));
      // System.out.println ("U=\n" + U.toString("%10.6f"));

      Matrix3d U = new Matrix3d();

      A.factorQR(R, U); // Note that because of names, Q R is R U here ...

      double sx = U.m00;
      double sy = U.m11;
      double sz = U.m22;

      double xy = U.m01 / sx;
      double xz = U.m02 / sx;
      double yz = U.m12 / sy;

      // try to keep scale factors positive. If det(R) < 0,
      // we will fix this later
      if (sx < 0) {
         sx = -sx;
         R.m00 = -R.m00;
         R.m10 = -R.m10;
         R.m20 = -R.m20;
      }
      if (sy < 0) {
         sy = -sy;
         R.m01 = -R.m01;
         R.m11 = -R.m11;
         R.m21 = -R.m21;
      }
      if (sz < 0) {
         sz = -sz;
         R.m02 = -R.m02;
         R.m12 = -R.m12;
         R.m22 = -R.m22;
      }

      if (Math.abs(sx - 1) < EPS) {
         sx = 1;
      }
      if (Math.abs(sy - 1) < EPS) {
         sy = 1;
      }
      if (Math.abs(sz - 1) < EPS) {
         sz = 1;
      }

      if (Math.abs(xy) < EPS) {
         xy = 0;
      }
      if (Math.abs(xz) < EPS) {
         xz = 0;
      }
      if (Math.abs(yz) < EPS) {
         yz = 0;
      }

      if (R.determinant() < 0) {
         // negate last column of R and sz
         R.m02 = -R.m02;
         R.m12 = -R.m12;
         R.m22 = -R.m22;
         sz = -sz;
      }
      if (scale != null) {
         scale.set(sx, sy, sz);
      }
      if (shear != null) {
         shear.set(xy, xz, yz);
      }
   }

   /**
    * Sets the A matrix of this transformation from rotation, scale, and shear
    * components, so that
    * 
    * <pre>
    * A = R S X
    * </pre>
    * 
    * where R is a right-handed rotation matrix, S is a diagonal scaling matrix,
    * and X is a shear matrix of the form
    * 
    * <pre>
    *     [ 1  xy  xz ]
    * X = [ 0   1  yz ]
    *     [ 0   0  1  ]
    * </pre>
    * 
    * @param R
    * the rotation matrix
    * @param scale
    * diagonal components of S
    * @param shear
    * components xy, xz, yz of X
    */
   public void setA(RotationMatrix3d R, Vector3d scale, Vector3d shear) {
      double xy = shear.x;
      double xz = shear.y;
      double yz = shear.z;

      A.set(scale.x, scale.x * xy, scale.x * xz,
         0, scale.y, scale.y * yz,
         0, 0, scale.z);
      A.mul(R, A);
   }

   /**
    * Creates an AffineTransform3d which performs scaling along the x, y, and z
    * axes.
    * 
    * @param sx
    * x scale factor
    * @param sy
    * y scale factor
    * @param sz
    * z scale factor
    */
   public static AffineTransform3d createScaling(
      double sx, double sy, double sz) {
      AffineTransform3d X = new AffineTransform3d();
      X.applyScaling(sx, sy, sz);
      return X;
   }

   /**
    * Creates an AffineTransform3d which performs uniform scaling along all
    * axes.
    * 
    * @param s
    * scale factor
    */
   public static AffineTransform3d createScaling(double s) {
      return createScaling(s, s, s);
   }

   // /**
   // * Sets this affine transform to contain only the shearing and scaling
   // * aspects of some other affine transform X.
   // */
   // public void setShearScale (AffineTransform3dBase X)
   // {
   // PolarDecomposition3d PD = new PolarDecomposition3d();
   // PD.setLeft (X.getMatrix());

   // RigidTransform3d Xpose = new RigidTransform3d();
   // Matrix3d P = new Matrix3d();

   // PD.getQ(Xpose.R);
   // PD.getP(P);
   // if (!PD.isQRightHanded())
   // { Xpose.R.negate();
   // P.negate();
   // }
   // System.out.println ("P=\n" + P.toString ("%8.3f"));
   // Xpose.R.mul (Xpose.R, myState.XFrameToWorld.R);
   // Xpose.p.mulAdd (
   // X.getMatrix(), myState.XFrameToWorld.p, X.getOffset());
   // myState.setPose (Xpose);

   // AffineTransform3d Xlocal = new AffineTransform3d();
   // Xlocal.A.mulTransposeLeft (Xpose.R, P);
   // Xlocal.A.mul (Xpose.R);

   // System.out.println ("Xlocal:\n" + Xlocal);

   // }

   public void setRandom() {
      A.setRandom();
      p.setRandom();
   }

   @Override
   public AffineTransform3d copy() {
      return new AffineTransform3d(this.A, this.p);
   }
   
   /**
    * Sets this affine transform to one that provides the best fit of q to p in
    * the least-squares sense: p ~ X q
    * 
    * @param p
    * set of target 3d points
    * @param q
    * set of input 3d points
    */
   public void fit(ArrayList<Point3d> p, ArrayList<Point3d> q)
      throws ImproperSizeException {
      assert p.size() == q.size();
      int n = p.size();
      if (n < 4)
         throw new ImproperSizeException("4 or more data points required");

      MatrixNd Q = new MatrixNd(n, 4);
      MatrixNd P = new MatrixNd(n, 3);
      MatrixNd A = new MatrixNd(4, 3);
      QRDecomposition qr = new QRDecomposition();

      for (int i = 0; i < n; i++) {
         Q.setRow(
            i, new double[] { q.get(i).x, q.get(i).y, q.get(i).z, 1.0 });
         P.setRow(i, p.get(i));
      }

      qr.factor(Q);
      qr.solve(A, P);
      A.transpose();
      A.getSubMatrix(0, 0, this.M);
      this.b.set(A.get(0, 3), A.get(1, 3), A.get(2, 3));
   }

   /**
    * Sets this affine transform to one that provides the best fit of q to p in
    * the least-squares sense: p ~ X q, where X is a rigid transformation with a
    * single scale factor
    * 
    * @param p
    * set of target 3d points
    * @param q
    * set of input 3d points
    * @param scale
    * allow scaling
    */
   public void fitRigid(ArrayList<Point3d> p, ArrayList<Point3d> q, boolean scale)
      throws ImproperSizeException {
      assert p.size() == q.size();

      int n = p.size();
      if (n < 3)
         throw new ImproperSizeException("3 or more data points required");

      MatrixNd Q = new MatrixNd(n, 3);
      MatrixNd P = new MatrixNd(n, 3);
      MatrixNd A = new MatrixNd(3, 3);
      Matrix3d U = new Matrix3d();
      Matrix3d V = new Matrix3d();
      Vector3d vec = new Vector3d();
      Vector3d qmean = new Vector3d();
      Vector3d pmean = new Vector3d();
      SVDecomposition svd = new SVDecomposition();

      for (int i = 0; i < n; i++) {
         qmean.add(q.get(i));
         pmean.add(p.get(i));
      }
      qmean.scale(1.0 / n);
      pmean.scale(1.0 / n);

      double sq = 0;
      for (int i = 0; i < n; i++) {
         vec.sub(q.get(i), qmean);
         Q.setRow(i, vec);
         sq += vec.normSquared();

         vec.sub(p.get(i), pmean);
         P.setRow(i, vec);
      }
      sq /= n;

      A.mulTransposeLeft(P, Q);
      A.scale(1.0 / n);
      svd.factor(A);
      svd.get(U, vec, V);

      double detU = U.orthogonalDeterminant();
      double detV = V.orthogonalDeterminant();
      if (detV * detU < 0) { /* then one is negative and the other positive */
         if (detV < 0) { /* negative last column of V */
            V.m02 = -V.m02;
            V.m12 = -V.m12;
            V.m22 = -V.m22;
            vec.z = -vec.z;
         }
         else /* detU < 0 */
         { /* negative last column of U */
            U.m02 = -U.m02;
            U.m12 = -U.m12;
            U.m22 = -U.m22;
            vec.z = -vec.z;
         }
      }

      this.A.mulTransposeRight(U, V);
      double s = (vec.x + vec.y + vec.z) / sq;
      
      if (scale) {
         this.A.scale(s, this.A);
      }

      AffineTransform3d trans = new AffineTransform3d(this.A, new Vector3d(0,0,0));
      qmean.transform(trans);
      this.p.sub(pmean, qmean);

   }
   
   private static void mulDiagRight(DenseMatrixBase A, Vector B) {
      
      for (int i=0; i<A.rowSize(); i++) {
         for (int j=0; j<A.colSize(); j++) {
            A.set(i, j, A.get(i,j)*B.get(j));
         }
      }
   }

   /**
    * Sets this affine transform to one that provides the best fit of q to p in
    * the least-squares sense: p ~ X q, where X has orthogonal columns, 
    * corresponding to a rigid transform with three scale factors (one for each 
    * dimension).  Uses an iterative procedure, stops when the scale factors
    * change less than a given threshold
    * 
    * @param p
    * set of target 3d points
    * @param q
    * set of input 3d points
    * @param threshold
    * algorithm stops when ||s_{i} - s_{i-1}||/||s_{i-1}|| {@code <} threshold
    * @return
    * 3-vector containing the scale factors
    */
   public Vector3d fitOrthogonal(ArrayList<Point3d> p, ArrayList<Point3d> q, double threshold) {
   
      int n = p.size();
      if (n < 4)
         throw new ImproperSizeException("4 or more data points required");

      MatrixNd Q = new MatrixNd(n, 3);
      MatrixNd P = new MatrixNd(n, 3);
      MatrixNd A = new MatrixNd(3, 3);
      Matrix3d U = new Matrix3d();
      Matrix3d V = new Matrix3d();
      Vector3d vec = new Vector3d();
      Vector3d qmean = new Vector3d();
      Vector3d pmean = new Vector3d();
      SVDecomposition svd = new SVDecomposition();

      for (int i = 0; i < n; i++) {
         qmean.add(q.get(i));
         pmean.add(p.get(i));
      }
      qmean.scale(1.0 / n);
      pmean.scale(1.0 / n);

      for (int i = 0; i < n; i++) {
         vec.sub(q.get(i), qmean);
         Q.setRow(i, vec);
         vec.sub(p.get(i), pmean);
         P.setRow(i, vec);
      }
      
      
      // original estimate of diagonal matrix
      Vector3d S = new Vector3d(1,1,1);
      MatrixNd QS = new MatrixNd();
      MatrixNd PtQS = new MatrixNd();
      MatrixNd PtQ = new MatrixNd();
      MatrixNd QtQ = new MatrixNd();
      MatrixNd RtPtQ = new MatrixNd(3,3);
      Matrix3d R = new Matrix3d();
      Vector3d qRow = new Vector3d();
      Vector3d rRow = new Vector3d(); 
      
      PtQ.mulTransposeLeft(P, Q);
      QtQ.mulTransposeLeft(Q,Q);

      // initial guess
      
      
      // loop until convergence
      double diff = 1.0;
      while(diff > threshold) {
         
         // orthogonal polar decomposition
         QS.set(Q);
         mulDiagRight(QS, S);
         PtQS.mulTransposeLeft(P, QS);
         A.set(PtQS);
         A.scale(1.0 / n);
         svd.factor(A);
         svd.get(U, vec, V);
         R.mulTransposeRight(U, V);
         
         RtPtQ.setSubMatrix(0, 0, R);
         RtPtQ.transpose();
         RtPtQ.mul(PtQ);
         
         // estimate scale factors
         diff = 0;
         double sNorm = S.norm();
         for (int i=0; i<3; i++) {
            QtQ.getRow(i, qRow);
            RtPtQ.getRow(i, rRow);
            double s = qRow.dot(rRow)/qRow.dot(qRow);
            diff += (S.get(i)-s)*(S.get(i)-s);
            S.set(i, s);
         }
         diff = Math.sqrt(diff)/sNorm;
         
      }
      
      // set transformation matrix
      this.A.set(R);
      mulDiagRight(this.A,S);   // scale
      
      AffineTransform3d trans = new AffineTransform3d(this.A, new Vector3d(0,0,0));
      qmean.transform(trans);
      
      this.p.sub(pmean, qmean);
      
      return S;
      
   }  
   
   /**
    * Sets this affine transform to one that provides the best fit of q to p in
    * the least-squares sense: p ~ X q, where X has orthogonal columns, 
    * corresponding to a rigid transform with three scale factors (one for each 
    * dimension)
    * 
    * @param p
    * set of target 3d points
    * @param q
    * set of input 3d points
    * @return
    * a 3-vector containing the scale factors
    */
   public Vector3d fitOrthogonal(ArrayList<Point3d> p, ArrayList<Point3d> q)
      throws ImproperSizeException {
      assert p.size() == q.size();

      return fitOrthogonal(p, q, 1e-10);

   }
   
   /**
    * Returns a string representation of this transformation as a 4 x 4 matrix.
    */
   public String toString() {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, new NumberFormat ("%g"), MATRIX_4X4_STRING);
   }

   /**
    * Returns a string representation of this transformation as a 4 x 4 matrix,
    * with each number formatted according to a supplied numeric format.
    * 
    * @param numberFmtStr
    * numeric format string (see {@link NumberFormat NumberFormat})
    */
   public String toString (String numberFmtStr) {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, new NumberFormat (numberFmtStr), MATRIX_4X4_STRING);
   }

   /**
    * Returns a specified string representation of this transformation, with
    * each number formatted according to the a supplied numeric format.
    * 
    * @param numberFmtStr
    * numeric format string (see {@link NumberFormat NumberFormat})
    * @param outputCode
    * desired representation, which should be either
    * {@link #MATRIX_4X4_STRING MATRIX_4X4_STRING}, or
    * {@link #MATRIX_3X4_STRING MATRIX_3X4_STRING}
    */
   public String toString (String numberFmtStr, int outputCode) {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, new NumberFormat (numberFmtStr), outputCode);
   }

   /**
    * Returns a specified string representation of this transformation, with
    * each number formatted according to the a supplied numeric format.
    * 
    * @param numberFmt
    * numeric format
    * @param outputCode
    * desired representation, which should be either
    * {@link #MATRIX_4X4_STRING MATRIX_4X4_STRING}, or
    * {@link #MATRIX_3X4_STRING MATRIX_3X4_STRING}
    */
   public String toString (NumberFormat numberFmt, int outputCode) {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, numberFmt, outputCode);
   }

   String toString (StringBuffer sbuf, NumberFormat numberFmt, int outputCode) {
      sbuf.setLength (0);
      if (outputCode == MATRIX_3X4_STRING || outputCode == MATRIX_4X4_STRING) {
         sbuf.append ("[ ");
         for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
               numberFmt.format (A.get (i, j), sbuf);
               sbuf.append (' ');
            }
            numberFmt.format (p.get (i), sbuf);
            if (i < 2 || outputCode == MATRIX_4X4_STRING) {
               sbuf.append ("\n  ");
            }
         }
         if (outputCode == MATRIX_4X4_STRING) {
            for (int j = 0; j < 3; j++) {
               numberFmt.format (0, sbuf);
               sbuf.append (' ');
            }
            numberFmt.format (1, sbuf);
         }
         sbuf.append (" ]");
      }
      else {
         throw new IllegalArgumentException ("Unknown display format");
      }
      return sbuf.toString();
   }

}
