/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import java.io.IOException;
import java.util.Random;

import maspack.matrix.ImproperSizeException;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix6dBase;
import maspack.matrix.Matrix6dBlock;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;

/**
 * Spatial inertia used to compute the dynamics of a rigid body. The spatial
 * inertia M is a 6 x 6 matrix which relates the spatial acceleration of a rigid
 * body to its applied spatial force, according to
 * 
 * <pre>
 * [ f ]   [  m I     - m [c]    ] [ dv ]
 * [   ] = [                     ] [    ]
 * [ t ]   [ m [c]  J - m [c][c] ] [ dw ]
 * </pre>
 * 
 * The left-hand vector is a wrench describing the spatial force, composed of a
 * translational force <i>f</i> and a moment <i>m</i>. The right-hand vector
 * is a twist describing the spatial acceleration, composed of a translational
 * acceleration <i>dv</i> and an angular acceleration <i>dw</i>. The
 * components of the inertia matrix itself are the body mass <i>m</i>, the
 * center of mass position <i>c</i>, and the rotational inertia <i>J</i> with
 * respect to the center of mass. [c] denotes the 3 x 3 cross-product matrix
 * formed from the components of c and I is the identity matrix.
 * 
 * <p>
 * An inertia is easy to invert given the inverse of J:
 * 
 * <pre>
 *              
 *       [               -1            -1  ]
 *  -1   [  1/m I - [c] J  [c]    [c] J    ]
 * M   = [                                 ]
 *       [         -1                 -1   ]
 *       [       -J  [c]             J     ]
 * </pre>
 * 
 * If the inertia's coordinate frame is located at the center of mass, then c =
 * 0 and the M and it's inverse are both block diagonal.
 * 
 * <p>
 * This class will typically be used to compute the acceleration of a rigid body
 * in response to applied forces. In doing this, it is generally necessary to
 * consider the coriolis forces which arise as a result of the body's rotation.
 * Coriolis forces can be computed using either
 * {@link #coriolisForce coriolisForce} or
 * {@link #bodyCoriolisForce bodyCoriolisForce}, depending on whether the
 * body's velocity is stored in a fixed frame or in one which moves with the
 * body.
 * 
 * <h3><a name="fixedframe">Fixed-frame usage example</a></h3>
 * 
 * In this situation, the body's velocity is stored in some fixed "world" frame,
 * which we transform into body coordinates for purposes of computing the
 * acceleration. We use this acceleration to update the velocity and
 * acceleration using a simple symplectic Euler integration step.
 * 
 * <pre>
 * SpatialInertia M; // spatial inertia for the body
 * RigidTransform Xbw; // transform from body to world coordinates
 * Twist vw; // body velocity in world coordinates
 * Twist vb; // body velocity in body coordinates
 * Wrench fx; // external force (world coordinates)
 * Wrench fc; // coriolis force (body coordinates)
 * Wrench fb; // total applied force in body coordinates
 * Twist acc; // spatial accelertion (body coodinates)
 * double h; // time step for integrator
 * 
 * vb.inverseTransform (Xbw.R, vw); // rotate vw to body coordinates
 * fb.inverseTransform (Xbw.R, fx); // rotate fx to body coordinates
 * M.coriolisForce (fc, vb); // compute fixed-frame coriolis force
 * fb.add (fc); // add coriolis force to body force
 * M.mulInverse (acc, fb); // solve fb = M acc
 * // integrate using symplectic Euler step
 * vb.scaledAdd (h, acc, vb);
 * vw.transform (Xbw.R, vb); // rotate vb back to world coordinates
 * vb.extrapolateTranform (Xbw, h); // use vb to update Xbw
 * </pre>
 * 
 * <h3><a name="bodyframe">Body-frame usage example</a></h3>
 * 
 * In this situation, the body's velocity is stored with respect to the body
 * frame, and hence is assumed to be <i>moving with it</i>. This means that
 * coriolis forces are computed differently:
 * 
 * <pre>
 * SpatialInertia M; // spatial inertia for the body
 * RigidTransform Xbw; // transform from body to world coordinates
 * Twist vb; // body velocity in body coordinates
 * Wrench fx; // external force (world coordinates)
 * Wrench fc; // coriolis force (body coordinates)
 * Wrench fb; // total applied force in body coordinates
 * Twist acc; // spatial accelertion (body coodinates)
 * double h; // time step for integrator
 * 
 * fb.inverseTransform (Xbw.R, fx); // rotate fx to body coordinates
 * M.bodyCoriolisForce (fc, vb); // compute body coriolis force
 * fb.add (fc); // add coriolis force to body force
 * M.mulInverse (acc, fb); // solve fb = M acc
 * // integrate using symplectic Euler step
 * vb.scaledAdd (h, acc, vb);
 * vb.extrapolateTranform (Xbw, h); // use vb to update Xbw
 * </pre>
 * 
 * <p>
 * The difference between this example and the previous one is quite subtle. In
 * the first example, vb is first computed by rotating vw into body coordinates
 * using the inverse of Xbw.R. It is then updated using the body acceleration,
 * and then rotated back into vw using Xbw.R. After that happens, Xbw is then
 * itself updated using vb. Now, at the beginning of the next step, when vb is
 * recomputed from vw, we will be using a different (updated) value of Xbw.R,
 * and so vb will be different from the value of vb that was computed at the end
 * of the previous step. In otherwords, vb depends on Xbw.R, and so changes in
 * Xbw.R cause vb to vary, even if vw is constant. In the body-frame example, we
 * don't transform between vb and vw, but the variation still exists, and is
 * instead accounted for varying the coriolis forces.
 */
public class SpatialInertia extends Matrix6dBlock
   implements java.io.Serializable {
   private static final long serialVersionUID = 1L;
   protected Point3d com;
   protected double mass;
   // J is the rotational inertia WRT the center of mass
   protected SymmetricMatrix3d J;
   protected Matrix3d L;

   protected double sqrtMass;
   protected Twist myTmpT;

   private enum Decomp {
      None, Zero, SPD
   }

   public static SpatialInertia ZERO = new SpatialInertia();
   public static SpatialInertia UNIT = new SpatialInertia(1.0, 1.0, 1.0, 1.0);

   // State of the decomposition of J
   protected Decomp JDecomp = Decomp.None;

   // componentUpdateNeeded means that one or more of the matrix elements
   // associated with this inertia have been changed and so com, J, and/or
   // sqrtMass need to be updated to reflect that.
   protected boolean componentUpdateNeeded = false;

   /**
    * Specifies a string representation of this spatial inertia consisting of a
    * mass, a center of mass point, and a 3 x 3 rotational inertia matrix with
    * respect to the center of mass.
    */
   public static final int MASS_INERTIA_STRING = 1;

   /**
    * Specifies a string representation of this spatial inertia as a 6 x 6
    * matrix.
    */
   public static final int MATRIX_STRING = 2;

   private void setJ (SymmetricMatrix3d newJ) {
      J.set (newJ);
      JDecomp = Decomp.None;
   }

   private void setJ (
      double m00, double m11, double m22, double m01, double m02, double m12) {
      J.set (m00, m11, m22, m01, m02, m12);
      JDecomp = Decomp.None;
   }

   private void scaleJ (double s, SymmetricMatrix3d M) {
      J.scale (s, M);
      JDecomp = Decomp.None;
   }

   private void setJZero() {
      J.setZero();
      JDecomp = Decomp.None;
   }

   /**
    * Creates a new spatial inertia with a value of zero.
    */
   public SpatialInertia() {
      myTmpT = new Twist(); // temporary storage vector
      J = new SymmetricMatrix3d();
      L = new Matrix3d();
      com = new Point3d();
   }

   /**
    * Creates a new spatial inertia with the same values as an existing one.
    * 
    * @param inertia
    * spatial inertia whose values are copied
    */
   public SpatialInertia (SpatialInertia inertia) {
      this();
      set (inertia);
   }

   /**
    * Creates a new spatial inertia with a specific mass and a diagonal
    * rotational inertia with the indicated element values. The center of mass
    * is set to zero.
    * 
    * @param m
    * mass
    * @param Jxx
    * rotational inertia about x
    * @param Jyy
    * rotational inertia about y
    * @param Jzz
    * rotational inertia about z
    */
   public SpatialInertia (double m, double Jxx, double Jyy, double Jzz) {
      this();
      set (m, Jxx, Jyy, Jzz);
   }

   /**
    * Sets a single element of this spatial inertia. The matrix structure is
    * enforced, so that zero elements are unaffected and the necessary
    * symmetries are preserved. The mass, center of mass, and rotational inertia
    * elements are also updated.
    * 
    * @param i
    * element row index
    * @param j
    * element column index
    * @param val
    * element value
    */
   public void set (int i, int j, double val) {
      super.set (i, j, val);
      componentUpdateNeeded = true;
   }

   public void set (double[] vals) {
      super.set (vals);
      componentUpdateNeeded = true;
   }

   public void set (Matrix M) {
      super.set (M);
      componentUpdateNeeded = true;
   }

   /**
    * Invalidate the center of mass and inertia components of this spatial
    * inertia. Should be called after the entry fields (e.g., m00, m01, etc)
    * are externally modified.
    */
   public void invalidateComponents() {
      componentUpdateNeeded = true;
   }

   /**
    * {@inheritDoc}
    */
   public double get (int i, int j) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      return super.get (i, j);
   }

   /**
    * Adds m [com] [com] to M and places the result in the lower right
    * 3x3 block of this inertia matrix
    */
   private void addScaledComCom (
      SymmetricMatrix3d M, double m, Vector3d com) {
      m33 = M.m00 - m * (com.z * com.z + com.y * com.y);
      m44 = M.m11 - m * (com.z * com.z + com.x * com.x);
      m55 = M.m22 - m * (com.y * com.y + com.x * com.x);

      m34 = M.m01 + m * com.x * com.y;
      m35 = M.m02 + m * com.x * com.z;
      m45 = M.m12 + m * com.z * com.y;

      m43 = m34;
      m53 = m35;
      m54 = m45;
   }

   /**
    * Adds m [com] [com] to the lower right 3x3 block of this inertia matrix
    */
   private void addScaledComCom (double m, Vector3d com) {
      m33 -= m * (com.z * com.z + com.y * com.y);
      m44 -= m * (com.z * com.z + com.x * com.x);
      m55 -= m * (com.y * com.y + com.x * com.x);

      m34 += m * com.x * com.y;
      m35 += m * com.x * com.z;
      m45 += m * com.z * com.y;

      m43 = m34;
      m53 = m35;
      m54 = m45;
   }

   private void setJMassComCom (
      double m00, double m11, double m22, double m01, double m02, double m12) {

      m33 = m00;
      m44 = m11;
      m55 = m22;

      m34 = m01;
      m35 = m02;
      m45 = m12;

      m43 = m34;
      m53 = m35;
      m54 = m45;
   }

   private void factorJ() {
      if (J.m00 == 0 && J.m11 == 0 && J.m22 == 0 &&
          J.m01 == 0 && J.m02 == 0 && J.m12 == 0) {
         L.setZero();
         JDecomp = Decomp.Zero;
      }
      try {
         J.getCholesky (L);
      }
      catch (Exception e) {
         throw new IllegalStateException (
            "Rotational inertia is not symmetric positive definite");
      }
      JDecomp = Decomp.SPD;
   }

   public void updateComponents() {
      mass = m00;
      if (mass == 0) {
         com.setZero();
      }
      else {
         com.x = m15 / mass;
         com.y = -m05 / mass;
         com.z = m04 / mass;
      }
      J.m00 = m33 - mass * (com.z * com.z + com.y * com.y);
      J.m11 = m44 - mass * (com.z * com.z + com.x * com.x);
      J.m22 = m55 - mass * (com.y * com.y + com.x * com.x);
      J.m01 = m34 + mass * com.x * com.y;
      J.m02 = m35 + mass * com.x * com.z;
      J.m12 = m45 + mass * com.z * com.y;
      J.m10 = J.m01;
      J.m20 = J.m02;
      J.m21 = J.m12;
      JDecomp = Decomp.None;
      sqrtMass = Math.sqrt (mass);
      // make sure matrix is symmetric and has correct structure
      setMassDiagonal (m00);
      setMassCom (m15, -m05, m04);
      m43 = m34;
      m53 = m35;
      m54 = m45;
      componentUpdateNeeded = false;
   }

   private void setMassDiagonal (double mass) {
      m00 = mass;
      m11 = mass;
      m22 = mass;
      m01 = m02 = 0;
      m10 = m12 = 0;
      m20 = m21 = 0;
   }

   private void setMassCom (double mcx, double mcy, double mcz) {
      m03 = 0;
      m04 = mcz;
      m05 = -mcy;
      m13 = -mcz;
      m14 = 0;
      m15 = mcx;
      m23 = mcy;
      m24 = -mcx;
      m25 = 0;

      m30 = 0;
      m40 = mcz;
      m50 = -mcy;
      m31 = -mcz;
      m41 = 0;
      m51 = mcx;
      m32 = mcy;
      m42 = -mcx;
      m52 = 0;
   }      

   private void updateMatrixElements() {
      setMassDiagonal (mass);
      setMassCom (mass*com.x, mass*com.y, mass*com.z);
      addScaledComCom (J, -mass, com);
   }

   /**
    * Reads the contents of this spatial inertia from a ReaderTokenizer. There
    * are two allowed formats, each of which is delimited by square brackets.
    * 
    * <p>
    * The first format is a set of 13 numbers giving, in order, the mass, center
    * of mass, and rotational inertia (in row-major order).
    * 
    * <p>
    * The second format format is a set of 36 numbers giving all elements of the
    * matrix. In interpreting these numbers, matrix structure is preserved, so
    * that zero elements remain zero and the necessary symmetries are
    * maintained.
    * 
    * @param rtok
    * ReaderTokenizer from which to read the inertia. Number parsing should be
    * enabled.
    * @throws IOException
    * if an I/O error occured or if the inertia description is not consistent
    * with one of the above formats.
    */
   public void scan (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');

      double[] values = new double[36];
      int nvals = 0;

      rtok.nextToken();
      if (rtok.tokenIsNumber()) {
         do {
            values[nvals++] = rtok.nval;
            rtok.nextToken();
         }
         while (nvals < values.length && rtok.tokenIsNumber());
         if (rtok.ttype != ']') {
            if (rtok.tokenIsNumber()) {
               throw new IOException ("Too many numeric values, line "
               + rtok.lineno());
            }
            else {
               throw new IOException ("']' expected, got " + rtok.tokenName()
               + ", line " + rtok.lineno());
            }
         }
         else if (nvals != 36 && nvals != 13) {
            throw new IOException ("Unexpected number of numeric values ("
            + nvals + "), line " + rtok.lineno());
         }
         else if (nvals == 13) {
            mass = values[0];
            sqrtMass = Math.sqrt (mass);
            com.set (values[1], values[2], values[3]);
            setJ (
               values[4], values[8], values[12], values[5], values[6],
               values[9]);
            updateMatrixElements();
         }
         else if (nvals == 36) {
            setMassDiagonal (values[0]);
            setMassCom (values[11], -values[5], values[4]);
            setJMassComCom (
               values[21], values[28], values[35], values[22], values[23],
               values[29]);

            updateComponents();
         }
      }
   }

   /**
    * Returns a string representation of this spatial inertia as a {@link
    * #MATRIX_STRING MATRIX_STRING}.
    */
   public String toString() {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, new NumberFormat ("%g"), MATRIX_STRING);
   }

   /**
    * Returns a string representation of this spatial inertia as a {@link
    * #MATRIX_STRING MATRIX_STRING}, with each number formatted according to a
    * supplied numeric format.
    * 
    * @param numberFmtStr
    * numeric format string (see {@link NumberFormat NumberFormat})
    */
   public String toString (String numberFmtStr) {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, new NumberFormat (numberFmtStr), MATRIX_STRING);
   }

   /**
    * Returns a specified string representation of this spatial inertia, with
    * each number formatted according to the a supplied numeric format.
    * 
    * @param numberFmtStr
    * numeric format string (see {@link NumberFormat NumberFormat})
    * @param outputCode
    * desired representation, which should be either
    * {@link #MASS_INERTIA_STRING MASS_INERTIA_STRING} or
    * {@link #MATRIX_STRING MATRIX_STRING}
    */
   public String toString (String numberFmtStr, int outputCode) {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, new NumberFormat (numberFmtStr), outputCode);
   }

   /**
    * Returns a specified string representation of this spatial inertia, with
    * each number formatted according to the a supplied numeric format.
    * 
    * @param numberFmt
    * numeric format
    * @param outputCode
    * desired representation, which should be either
    * {@link #MASS_INERTIA_STRING MASS_INERTIA_STRING} or
    * {@link #MATRIX_STRING MATRIX_STRING}
    */
   public String toString (NumberFormat numberFmt, int outputCode) {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, numberFmt, outputCode);
   }

   String toString (StringBuffer sbuf, NumberFormat numberFmt, int outputCode) {
      sbuf.setLength (0);
      if (outputCode == MATRIX_STRING) {
         sbuf.append ("[");
         for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
               sbuf.append (' ');
               numberFmt.format (get (i, j), sbuf);
            }
            if (i < 5) {
               sbuf.append ("\n ");
            }
         }
         sbuf.append (" ]");
      }
      else if (outputCode == MASS_INERTIA_STRING) {
         sbuf.append ("[ ");
         numberFmt.format (mass, sbuf);
         sbuf.append (' ');
         numberFmt.format (com.x, sbuf);
         sbuf.append (' ');
         numberFmt.format (com.y, sbuf);
         sbuf.append (' ');
         numberFmt.format (com.z, sbuf);
         sbuf.append ("\n ");
         for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
               sbuf.append (' ');
               numberFmt.format (J.get (i, j), sbuf);
            }
            if (i < 2) {
               sbuf.append ("\n ");
            }
         }

         sbuf.append ("\n]");
      }
      else {
         throw new IllegalArgumentException ("Unknown display format");
      }
      return sbuf.toString();
   }

   /**
    * Gets the rotational inertia (with respect to the center of mass) for this
    * spatial inertia.
    * 
    * @param J
    * returns the rotational inertia
    */
   public void getRotationalInertia (SymmetricMatrix3d J) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      J.set (this.J);
   }

   /**
    * Gets the rotational interia (with respect to the center of mass) for this
    * spatial inertia. The returned is an internal reference and should not be
    * modified. Also, its value may not be updated immediately following
    * subsequent modifications to this spatial inertia.
    * 
    * @return the rotational inertia
    */
   public SymmetricMatrix3d getRotationalInertia() {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      return J;
   }

   /**
    * Gets the rotational interia, offset by the center of mass, for this
    * spatial inertia. The returned is an internal reference and should not be
    * modified.
    * 
    * @return the offset rotational inertia
    */
   public SymmetricMatrix3d getOffsetRotationalInertia() {
      return new SymmetricMatrix3d (m33, m44, m55, m34, m35, m45);
   }

   /** Sets the rotational interia for this spatial inertia.
    * 
    * @param J
    * rotational inertia (with respect to the center of mass)
    */
   public void setRotationalInertia (SymmetricMatrix3d J) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      setJ (J);
      addScaledComCom (this.J, -mass, com);
   }

   /**
    * Sets the rotational interia for this spatial inertia, given the diagonal
    * and upper off-diagonal elements
    * 
    * @param J00
    * element (0,0)
    * @param J11
    * element (1,1)
    * @param J22
    * element (2,2)
    * @param J01
    * element (0,1)
    * @param J02
    * element (0,2)
    * @param J12
    * element (1,2)
    */
   public void setRotationalInertia (
      double J00, double J11, double J22, double J01, double J02, double J12) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      setJ (J00, J11, J22, J01, J02, J12);
      addScaledComCom (this.J, -mass, com);
   }

   /**
    * Gets the center of mass for this spatial inertia.
    * 
    * @param com
    * returns the center of mass
    */
   public void getCenterOfMass (Vector3d com) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      com.set (this.com);
   }

   /**
    * Gets the center of mass for this spatial inertia. The returned value is an
    * internal reference and should not be modified. Also, its value may not be
    * updated immediately following subsequent modifications to this spatial
    * inertia.
    * 
    * @return center of mass
    */
   public Point3d getCenterOfMass() {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      return com;
   }

   /** 
    * Transforms this inertia into a different frame and returns the rotated
    * value. The rotation is specified by a rotation matrix that transforms
    * from the current frame into the target frame.
    * 
    * @param M returns the rotated value
    * @param R rotation matrix describing the rotation
    */
   public void getRotated (Matrix6d M, RotationMatrix3d R) {

      if (componentUpdateNeeded) {
         updateComponents();
      }

      M.m00 = mass;
      M.m11 = mass;
      M.m22 = mass;

      M.m01 = 0;
      M.m02 = 0;
      M.m12 = 0;

      M.m10 = 0;
      M.m20 = 0;
      M.m21 = 0;

      Vector3d tmp = myTmpT.v; // use myTmpT.v as temporary storage
      tmp.transform (R, com);
      tmp.scale (mass);

      M.m04 =  tmp.z;
      M.m05 = -tmp.y;
      M.m15 =  tmp.x;
      
      M.m40 =  tmp.z;
      M.m50 = -tmp.y;
      M.m51 =  tmp.x;
      
      M.m03 =  0;
      M.m14 =  0;
      M.m25 =  0;

      M.m31 = -tmp.z;
      M.m32 =  tmp.y;
      M.m42 = -tmp.x;
      
      M.m13 = -tmp.z;
      M.m23 =  tmp.y;
      M.m24 = -tmp.x;

      M.m30 =  0;
      M.m41 =  0;
      M.m52 =  0;

      double J00 = m33*R.m00 + m34*R.m01 + m35*R.m02;
      double J01 = m33*R.m10 + m34*R.m11 + m35*R.m12;
      double J02 = m33*R.m20 + m34*R.m21 + m35*R.m22;

      double J10 = m43*R.m00 + m44*R.m01 + m45*R.m02;
      double J11 = m43*R.m10 + m44*R.m11 + m45*R.m12;
      double J12 = m43*R.m20 + m44*R.m21 + m45*R.m22;

      double J20 = m53*R.m00 + m54*R.m01 + m55*R.m02;
      double J21 = m53*R.m10 + m54*R.m11 + m55*R.m12;
      double J22 = m53*R.m20 + m54*R.m21 + m55*R.m22;

      M.m33 = R.m00*J00 + R.m01*J10 + R.m02*J20;
      M.m34 = R.m00*J01 + R.m01*J11 + R.m02*J21;
      M.m35 = R.m00*J02 + R.m01*J12 + R.m02*J22;

      M.m43 = M.m34;
      M.m44 = R.m10*J01 + R.m11*J11 + R.m12*J21;
      M.m45 = R.m10*J02 + R.m11*J12 + R.m12*J22;

      M.m53 = M.m35;
      M.m54 = M.m45;
      M.m55 = R.m20*J02 + R.m21*J12 + R.m22*J22;

      if (M instanceof SpatialInertia) {
         ((SpatialInertia)M).componentUpdateNeeded = true;
      }
      
   }

   /**
    * Sets the center of mass for this spatial inertia.
    * 
    * @param com
    * center of mass
    */
   public void setCenterOfMass (Point3d com) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      this.com.set (com);
      setMassCom (mass*com.x, mass*com.y, mass*com.z);
      addScaledComCom (J, -mass, com);
   }

   /**
    * Sets the center of mass for this spatial inertia.
    * 
    * @param x
    * center of mass x coordinate
    * @param y
    * center of mass y coordinate
    * @param z
    * center of mass z coordinate
    */
   public void setCenterOfMass (double x, double y, double z) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      this.com.set (x, y, z);
      setMassCom (mass*com.x, mass*com.y, mass*com.z);
      addScaledComCom (J, -mass, com);
   }

   /**
    * Gets the mass for this spatial inertia.
    * 
    * @return mass
    */
   public double getMass() {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      return mass;
   }

   /**
    * Sets the mass for this spatial inertia.
    * 
    * @param m
    * mass
    */
   public void setMass (double m) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      mass = m;
      sqrtMass = Math.sqrt (mass);
      setMassDiagonal (mass);
      setMassCom (mass*com.x, mass*com.y, mass*com.z);
      addScaledComCom (J, -mass, com);
   }

   /**
    * Sets this spatial inertia to be identical to be identical to another one.
    * 
    * @param M
    * spatial inertia to be copied
    */
   public void set (Matrix6d M) {
      set ((Matrix6dBase)M);
   }

   /**
    * Sets this spatial inertia to be identical to be identical to another one.
    * 
    * @param M
    * spatial inertia to be copied
    */
   public void set (Matrix6dBase M) {
      super.set (M);
      updateComponents();
      Vector3d massCom = new Vector3d();
      massCom.scale (mass, com);
   }

   /**
    * Sets this spatial inertia to be identical to be identical to another one.
    * 
    * @param M
    * spatial inertia to be copied
    */
   public void set (SpatialInertia M) {
      super.set (M);
      mass = M.mass;
      sqrtMass = M.sqrtMass;
      com.set (M.com);
      J.set (M.J);
      L.set (M.L);
      componentUpdateNeeded = M.componentUpdateNeeded;
      JDecomp = M.JDecomp;
   }
   
   /**
    * Sets this spatial inertia to have a specific mass and rotational inertia.
    * The center of mass is set to zero.
    * 
    * @param m
    * mass
    * @param J
    * rotational inertia (with respect to the center of mass)
    */
   public void set (double m, SymmetricMatrix3d J) {
      this.mass = m;
      sqrtMass = Math.sqrt (mass);
      setJ (J);
      this.com.setZero();
      updateMatrixElements();
   }

   /**
    * Sets this spatial inertia to have a specific mass, rotational inertia, and
    * center of mass.
    * 
    * @param m
    * mass
    * @param J
    * rotational inertia (with respect to the center of mass)
    * @param com
    * center of mass
    */
   public void set (double m, SymmetricMatrix3d J, Point3d com) {
      this.mass = m;
      sqrtMass = Math.sqrt (mass);
      setJ (J);
      this.com.set (com);
      updateMatrixElements();
   }

   /**
    * Sets this spatial inertia to correspond to a point mass at a specific
    * point. The resulting inertia will not be invertible.
    * 
    * @param m
    * mass
    * @param com
    * center of mass
    */
   public void setPointMass (double m, Point3d com) {
      this.mass = m;
      sqrtMass = Math.sqrt (mass);
      setJZero();
      this.com.set (com);
      updateMatrixElements();
   }

   /**
    * Sets this spatial inertia to have a specific mass and a diagonal
    * rotational inertia with the indicated element values. The center of mass
    * is set to zero.
    * 
    * @param m
    * mass
    * @param Jxx
    * rotational inertia about x
    * @param Jyy
    * rotational inertia about y
    * @param Jzz
    * rotational inertia about z
    */
   public void set (double m, double Jxx, double Jyy, double Jzz) {
      this.mass = m;
      sqrtMass = Math.sqrt (mass);
      setJ (Jxx, Jyy, Jzz, 0, 0, 0);
      this.com.setZero();
      updateMatrixElements();
   }

   /**
    * Sets this spatial inertia to correspond to the center of a sphere of
    * uniform density.
    * 
    * @param m
    * mass
    * @param r
    * sphere radius
    */
   public void setSphere (double m, double r) {
      setEllipsoid (m, r, r, r);
   }

   /**
    * Sets this spatial inertia to correspond to the center of an axis-aligned
    * ellispoid of uniform density.
    * 
    * @param m
    * mass
    * @param a
    * semi-axis length in the x direction
    * @param b
    * semi-axis length in the y direction
    * @param c
    * semi-axis length in the z direction
    */
   public void setEllipsoid (double m, double a, double b, double c) {
      this.mass = m;
      sqrtMass = Math.sqrt (mass);
      double s = m / 5;
      setJ (
         s * (b * b + c * c), s * (a * a + c * c), s * (a * a + b * b), 0, 0, 0);
      this.com.setZero();
      updateMatrixElements();

   }

   /**
    * Sets this spatial inertia to correspond to the center of an axis-aligned
    * box of uniform density.
    * 
    * @param m
    * mass
    * @param wx
    * width along the x direction
    * @param wy
    * width along the y direction
    * @param wz
    * width along the z direction
    */
   public void setBox (double m, double wx, double wy, double wz) {
      this.mass = m;
      sqrtMass = Math.sqrt (mass);
      double s = m / 12;
      setJ (s * (wy * wy + wz * wz), s * (wx * wx + wz * wz), s
      * (wx * wx + wy * wy), 0, 0, 0);
      this.com.setZero();
      updateMatrixElements();
   }

   /**
    * Sets this spatial inertia to correspond to the center of a cylinder of
    * uniform density aligned with the z axis.
    * 
    * @param m
    * mass
    * @param r
    * radius
    * @param l
    * length
    */
   public void setCylinder (double m, double r, double l) {
      this.mass = m;
      sqrtMass = Math.sqrt (mass);
      double xy = m * (l * l + 3 * r * r) / 12;
      setJ (xy, xy, m * r * r / 2, 0, 0, 0);
      this.com.setZero();
      updateMatrixElements();
   }

//   /**
//    * Sets this spatial inertia to correspond to a uniform-density closed volume
//    * defined by a polygonal mesh. The code for this was taken from vclip, by
//    * Brian Mirtich. See "Fast and Accurate Computation of Polyhedral Mass
//    * Properties," Brian Mirtich, journal of graphics tools, volume 1, number 2,
//    * 1996.
//    * 
//    * @param mesh
//    * closed polygonal mesh
//    * @param density
//    * density of the object
//    * @return volume volume of the mesh
//    */
//   public double setClosedVolume (PolygonalMesh mesh, double density) {
//      Vector3d mov1 = new Vector3d();
//      Vector3d mov2 = new Vector3d();
//      Vector3d pov = new Vector3d();
//
//      double vol = mesh.computeVolumeIntegrals (mov1, mov2, pov);
//
//      Point3d cov = new Point3d();
//      cov.scale (1.0 / vol, mov1); // center of volume
//
//      mass = vol;
//      setMassDiagonal (mass);
//      setMassCom (vol * cov.x, vol * cov.y, vol * cov.z);
//      setJMassComCom ((mov2.y + mov2.z), // - (mov1.y+mov1.z)/vol,
//         (mov2.x + mov2.z), // - (mov1.x+mov1.z)/vol,
//         (mov2.x + mov2.y), // - (mov1.x+mov1.y)/vol,
//         -pov.z, -pov.y, -pov.x);
//      updateComponents();
//
//      scaleMass (density);
//      return vol;
//   }

   /**
    * Sets this spatial inertia to zero.
    */
   public void setZero() {
      this.mass = 0;
      this.sqrtMass = 0;
      this.setJZero();
      this.com.setZero();
      super.setZero();
   }

   /**
    * Sets the components of this spatial inertia to uniformly distributed
    * random values in the range -0.5 (inclusive) to 0.5 (exclusive). The
    * components include the mass, center of mass, and rotational inertia. The
    * results are adjusted to ensure that the mass is positive and the
    * rotational inertia matrix is positive definite.
    */
   public void setRandom() {
      setRandom (-0.5, 0.5, RandomGenerator.get());
   }

   /**
    * Sets the components of this spatial inertia to uniformly distributed
    * random values in a specified range. The components include the mass,
    * center of mass, and rotational inertia. The results are adjusted to ensure
    * that the mass is positive and the rotational inertia matrix is positive
    * definite.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    */
   public void setRandom (double lower, double upper) {
      setRandom (lower, upper, RandomGenerator.get());
   }

   /**
    * Sets the components of this spatial inertia to uniformly distributed
    * random values in a specified range, using a supplied random number
    * generator. The components include the mass, center of mass, and rotational
    * inertia. The results are adjusted to ensure that the mass is positive and
    * the rotational inertia matrix is positive definite.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    * @param generator
    * random number generator
    */
   public void setRandom (double lower, double upper, Random generator) {
      double range = upper - lower;
      mass = 0;
      while (mass == 0) {
         mass = Math.abs (generator.nextDouble() * range + lower);
      }
      com.setRandom (lower, upper, generator);
      RotationMatrix3d U = new RotationMatrix3d();
      U.setRandom();
      L.setZero(); // use L to form J
      for (int i = 0; i < 3; i++) {
         double elem = 0;
         while (elem == 0) {
            elem = Math.abs (generator.nextDouble() * range + lower);
         }
         L.set (i, i, elem);
      }
      L.mul (U, L);
      L.mulTranspose (U);
      J.set (L);
      updateMatrixElements();
   }

   /**
    * Adds this spatial inertia to M1 and places the result in this spatial
    * inertia.
    * 
    * @param M1
    * right-hand inertia
    */
   public void add (SpatialInertia M1) {
      add (this, M1);
   }

   /**
    * Adds spatial inertia M1 to M2 and places the result in this spatial
    * inertia.
    * 
    * @param M1
    * left-hand spatial inertia
    * @param M2
    * right-hand spatial inertia
    */
   public void add (SpatialInertia M1, SpatialInertia M2) {
      if (M1.componentUpdateNeeded) {
         M1.updateComponents();
      }
      if (M2.componentUpdateNeeded) {
         M2.updateComponents();
      }
      mass = M1.mass + M2.mass;
      super.add (M1, M2);
      updateComponents();
   }

   /**
    * Adds a point mass at a specified point to this inertia. Note that
    * subtracting a point mass can be achieved by specifying a negative mass.
    * 
    * @param m
    * Mass of the point being added
    * @param pos
    * Point position relative to this inertia's coordinate frame
    */
   public void addPointMass (double m, Vector3d pos) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      mass += m;
      setMassDiagonal (mass);
      setMassCom (m15+m*pos.x, -m05+m*pos.y, m04+m*pos.z);
      addScaledComCom (-m, pos);
      updateComponents();
   }

   /**
    * Adds the inertia of a line segment to this inertia. The line segment is
    * assumed to have uniform density and is defined by two points relative to
    * this inertia's coordinate frame.
    *
    * @param m line segment mass
    * @param p0 first line segment point
    * @param p1 second line segment point
    */
   public void addLineSegmentInertia (double m, Vector3d p0, Vector3d p1) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      // compute center of mass
      Vector3d com = new Vector3d();
      com.add (p0, p1);
      com.scale (0.5);

      // add mass an center of mass components to the inertia
      mass += m;
      setMassDiagonal (mass);
      setMassCom (m15+m*com.x, -m05+m*com.y, m04+m*com.z);

      // compute and add the inertia tensor components. In evaluating these
      // integrals, we assume that a point on the line segment is defined
      // parametrically by
      //
      // p = a + s b
      //
      // where a = p0, b = (p1 - p0), and s is a scalar in the interval [0,1].

      double ax = p0.x;
      double ay = p0.y;
      double az = p0.z;

      double bx = p1.x - ax;
      double by = p1.y - ay;
      double bz = p1.z - az;

      // intxx, intxy, etc. denote the integrals of x*x, x*y, etc. over the domain:

      double intxx = m*(ax*ax + ax*bx + bx*bx/3);
      double intyy = m*(ay*ay + ay*by + by*by/3);
      double intzz = m*(az*az + az*bz + bz*bz/3);

      double intxy = m*(ax*ay + 0.5*(ax*by+ay*bx) + bx*by/3);
      double intyz = m*(ay*az + 0.5*(ay*bz+az*by) + by*bz/3);
      double intzx = m*(az*ax + 0.5*(az*bx+ax*bz) + bz*bx/3);

      m33 += (intyy + intzz);
      m44 += (intzz + intxx);
      m55 += (intxx + intyy);

      m34 -= intxy;
      m35 -= intzx;
      m45 -= intyz;

      m43 = m34;
      m53 = m35;
      m54 = m45;

      updateComponents();
   }

   /**
    * Adds the inertia of a triangle to this inertia. The triangle is
    * assumed to have uniform density and is defined by two points relative to
    * this inertia's coordinate frame.
    *
    * @param m triangle mass
    * @param p0 first triangle point
    * @param p1 second triangle point
    * @param p2 third triangle point
    */
   public void addTriangleInertia (
      double m, Vector3d p0, Vector3d p1, Vector3d p2) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      // compute center of mass
      Vector3d com = new Vector3d();
      com.add (p0, p1);
      com.add (p2);
      com.scale (1/3.0);

      // add mass an center of mass components to the inertia
      mass += m;
      setMassDiagonal (mass);
      setMassCom (m15+m*com.x, -m05+m*com.y, m04+m*com.z);

      // compute and add the inertia tensor components. In evaluating these
      // integrals, we assume that a point on the triangle is defined
      // parametrically by
      //
      // p = a + s1 b + s2 c 
      //
      // where a = p0, b = (p1 - p0), c = (p2 - p0), and s1 and s2 are scalars
      // in the intervals [0,1-s2] and [0,1].
      //
      // The inertia terms are then defined by product integrals of the form:
      //
      // int_xy = \rho \int_0^1 ( \int_0^{1-s2} x y ds1) ds2
      //
      // Note also that in terms of mass m, \rho = m/A, where A is the area of
      // the domain, which for s1, s2 is 1/2, and so
      //
      // int_xy = 2 m \int_0^1 ( \int_0^{1-s2} x y ds1) ds2

      double ax = p0.x;
      double ay = p0.y;
      double az = p0.z;

      double bx = p1.x - ax;
      double by = p1.y - ay;
      double bz = p1.z - az;

      double cx = p2.x - ax;
      double cy = p2.y - ay;
      double cz = p2.z - az;

      // intxx, intxy, etc. denote the integrals for x*x, x*y, etc., multiplied
      // by \rho = 2 m

      double intxx = m*(ax*ax + 2*(ax*bx+ax*cx)/3 + (bx*bx+bx*cx+cx*cx)/6);
      double intyy = m*(ay*ay + 2*(ay*by+ay*cy)/3 + (by*by+by*cy+cy*cy)/6);
      double intzz = m*(az*az + 2*(az*bz+az*cz)/3 + (bz*bz+bz*cz+cz*cz)/6);

      double intxy = m*(ax*ay + (ax*by+ay*bx+ax*cy+ay*cx)/3 +
                        (bx*by+cx*cy)/6 + (bx*cy+by*cx)/12);
      double intyz = m*(ay*az + (ay*bz+az*by+ay*cz+az*cy)/3 +
                        (by*bz+cy*cz)/6 + (by*cz+bz*cy)/12);
      double intzx = m*(az*ax + (az*bx+ax*bz+az*cx+ax*cz)/3 +
                        (bz*bx+cz*cx)/6 + (bz*cx+bx*cz)/12);

      m33 += (intyy + intzz);
      m44 += (intzz + intxx);
      m55 += (intxx + intyy);

      m34 -= intxy;
      m35 -= intzx;
      m45 -= intyz;

      m43 = m34;
      m53 = m35;
      m54 = m45;

      updateComponents();
   }

   /**
    * Subtracts this spatial inertia from M1 and places the result in this
    * spatial inertia.
    * 
    * @param M1
    * right-hand inertia
    * @throws IllegalArgumentException
    * if the resulting mass is negative or the rotational inertia is not
    * positive definite.
    */
   public void sub (SpatialInertia M1) {
      sub (this, M1);
   }

   /**
    * Subtracts spatial inertia M1 from M2 and places the result in this spatial
    * inertia.
    * 
    * @param M1
    * left-hand spatial inertia
    * @param M2
    * right-hand spatial inertia
    * @throws IllegalArgumentException
    * if the resulting mass is negative or the rotational inertia is not
    * positive definite.
    */
   public void sub (SpatialInertia M1, SpatialInertia M2) {
      if (M1.componentUpdateNeeded) {
         M1.updateComponents();
      }
      if (M2.componentUpdateNeeded) {
         M2.updateComponents();
      }
      mass = M1.mass - M2.mass;
      super.sub (M1, M2);
      updateComponents();
   }

   /**
    * Scales the spatial inertia M1 by <code>s</code> and places the results
    * in this spatial inertia.
    * 
    * @param s
    * scaling factor
    * @param M1
    * spatial inertia to be scaled
    * @throws IllegalArgumentException
    * if s is negative
    */
   public void scale (double s, SpatialInertia M1) {
      if (M1.componentUpdateNeeded) {
         M1.updateComponents();
      }
      if (s < 0) {
         throw new IllegalArgumentException ("negative scale factor");
      }
      scaleJ (s, M1.J);
      mass = s * M1.mass;
      sqrtMass = Math.sqrt (mass);
      updateMatrixElements();
   }

   /**
    * Scales this spatial inertia by <code>s</code> in place.
    * 
    * @param s
    * scaling factor
    * @throws IllegalArgumentException
    * if s is negative
    */
   public void scale (double s) {
      scale (s, this);
   }

   /**
    * Computes the inverse of this spatial inertia matrix. The result matrix is
    * resized if necessary.
    * 
    * @param MI
    * returns the inverse matrix
    * @throws ImproperSizeException
    * if M1 is not 6 x 6 and has a fixed size
    */
   public void getInverse (MatrixNd MI) throws ImproperSizeException {
      if (MI.rowSize() != 6 || MI.colSize() != 6) {
         if (MI.isFixedSize()) {
            throw new ImproperSizeException (
               "Result matrix has fixed size and is not 6 x 6");
         }
         else {
            MI.setSize (6, 6);
         }
      }
      if (componentUpdateNeeded) {
         updateComponents();
      }
      Twist tw = new Twist();
      Wrench wr = new Wrench();
      for (int j = 0; j < 6; j++) {
         wr.setZero();
         wr.set (j, 1);
         mulInverse (tw, wr);
         MI.setColumn (j, tw);
      }
   }

   /**
    * Adds a point mass at a specified point to a 6x6 matrix representing a
    * spatial inertia. Note that subtracting a point mass can be achieved by
    * specifying a negative mass.
    *
    * @param M
    * Matrix containing the inertia. Must be a Matrix6d or a MatrixNd
    * with size at least 6x6.
    * @param m
    * Mass of the point being added
    * @param pos
    * Point position relative to this inertia's coordinate frame
    */
   public static void addPointMass (Matrix M, double m, Vector3d pos) {   

      double cx = pos.x;
      double cy = pos.y;
      double cz = pos.z;

      double mcx = m*cx;
      double mcy = m*cy;
      double mcz = m*cz;

      if (M instanceof Matrix6dBase) {
         Matrix6dBase M6 = (Matrix6dBase)M;

         M6.m00 += m;
         M6.m11 += m;
         M6.m22 += m;

         M6.m33 += mcy*cy + mcz*cz; 
         M6.m44 += mcx*cx + mcz*cz;
         M6.m55 += mcx*cx + mcy*cy;

         M6.m34 -= mcx*cy;
         M6.m35 -= mcx*cz;
         M6.m45 -= mcy*cz;
         M6.m43 -= mcx*cy;
         M6.m53 -= mcx*cz;
         M6.m54 -= mcy*cz;

         M6.m04 += mcz;
         M6.m05 -= mcy;
         M6.m15 += mcx;
         M6.m13 -= mcz;
         M6.m23 += mcy;
         M6.m24 -= mcx;

         M6.m40 += mcz;
         M6.m50 -= mcy;
         M6.m51 += mcx;
         M6.m31 -= mcz;
         M6.m32 += mcy;
         M6.m42 -= mcx;
      }
      else if (M instanceof MatrixNd) {
         MatrixNd MN = (MatrixNd)M;

         if (MN.rowSize() < 6 || M.colSize() < 6) {
            throw new IllegalArgumentException (
               "M must be a Matrix6dBase or a MatrixNd with size at least 6x6");
         }

         MN.add (0, 0,  m);
         MN.add (1, 1,  m);
         MN.add (2, 2,  m);

         MN.add (3, 3,  mcy*cy + mcz*cz);
         MN.add (4, 4,  mcx*cx + mcz*cz);
         MN.add (5, 5,  mcx*cx + mcy*cy);

         MN.add (3, 4, -mcx*cy);
         MN.add (3, 5, -mcx*cz);
         MN.add (4, 5, -mcy*cz);
         MN.add (4, 3, -mcx*cy);
         MN.add (5, 3, -mcx*cz);
         MN.add (5, 4, -mcy*cz);

         MN.add (0, 4,  mcz);
         MN.add (0, 5, -mcy);
         MN.add (1, 5,  mcx);
         MN.add (1, 3, -mcz);
         MN.add (2, 3,  mcy);
         MN.add (2, 4, -mcx);

         MN.add (4, 0,  mcz);
         MN.add (5, 0, -mcy);
         MN.add (5, 1,  mcx);
         MN.add (3, 1, -mcz);
         MN.add (3, 2,  mcy);
         MN.add (4, 2, -mcx);
      }
      else {
         throw new IllegalArgumentException (
            "M must be a Matrix6dBase or a MatrixNd with size at least 6x6");
      }

      if (M instanceof SpatialInertia) {
         ((SpatialInertia)M).componentUpdateNeeded = true;
      }
   }

   /** 
    * Computes the inverse of a spatial inertia, stored in M, and
    * return the result in MI.
    * 
    * @param MI Matrix returning the spatial inertia
    * @param M Inertia to invert
    */
   public static void invert (Matrix6d MI, Matrix6d M) {

      double m = M.m00;

      double mcx = +M.m15;
      double mcy = -M.m05;
      double mcz = +M.m04;

      double cx = mcx/m;
      double cy = mcy/m;
      double cz = mcz/m;

      Vector3d c = new Vector3d (cx, cy, cz);

      Matrix3d X = new Matrix3d ();

      // start by setting X to J

      X.m00 = M.m33 - mcy*cy - mcz*cz; 
      X.m11 = M.m44 - mcx*cx - mcz*cz;
      X.m22 = M.m55 - mcx*cx - mcy*cy;

      X.m01 = M.m34 + mcx*cy;
      X.m02 = M.m35 + mcx*cz;
      X.m12 = M.m45 + mcy*cz;

      X.m10 = X.m01;
      X.m20 = X.m02;
      X.m21 = X.m12;

      // invert to get J^{-1}
      X.invert();

      // set lower right block to J^{-1}
      MI.m33 = X.m00;
      MI.m44 = X.m11;
      MI.m55 = X.m22;

      MI.m34 = X.m01;
      MI.m35 = X.m02;
      MI.m45 = X.m12;

      MI.m43 = X.m01;
      MI.m53 = X.m02;
      MI.m54 = X.m12;

      // set upper right block to [c] J^{-1}

      X.crossProduct (c, X);

      MI.m03 = X.m00;
      MI.m04 = X.m01;
      MI.m05 = X.m02;

      MI.m13 = X.m10;
      MI.m14 = X.m11;
      MI.m15 = X.m12;

      MI.m23 = X.m20;
      MI.m24 = X.m21;
      MI.m25 = X.m22;

      // set lower left block to ([c] J^{-1})^T

      MI.m30 = X.m00;
      MI.m40 = X.m01;
      MI.m50 = X.m02;

      MI.m31 = X.m10;
      MI.m41 = X.m11;
      MI.m51 = X.m12;

      MI.m32 = X.m20;
      MI.m42 = X.m21;
      MI.m52 = X.m22;

      // set upper left block to 1/m I - [c] J^{-1} [c]

      X.crossProduct (X, c);

      MI.m00 = -X.m00 + 1/m;
      MI.m01 = -X.m01;
      MI.m02 = -X.m02;

      MI.m10 = -X.m10;
      MI.m11 = -X.m11 + 1/m;
      MI.m12 = -X.m12;

      MI.m20 = -X.m20;
      MI.m21 = -X.m21;
      MI.m22 = -X.m22 + 1/m;
   }

   private void solveU (Vector3d v, Vector3d b) {
      if (JDecomp == Decomp.None) {
         factorJ();
      }
      if (JDecomp == Decomp.Zero) {
         throw new IllegalStateException ("Rotational inertia is zero");
      }
      double z = b.z / L.m22;
      double y = (b.y - L.m21 * z) / L.m11;
      double x = (b.x - L.m10 * y - L.m20 * z) / L.m00;

      v.x = x;
      v.y = y;
      v.z = z;
   }

   private void solveL (Vector3d v, Vector3d b) {
      if (JDecomp == Decomp.None) {
         factorJ();
      }
      if (JDecomp == Decomp.Zero) {
         throw new IllegalStateException ("Rotational inertia is zero");
      }
      double x = b.x / L.m00;
      double y = (b.y - L.m10 * x) / L.m11;
      double z = (b.z - L.m21 * y - L.m20 * x) / L.m22;

      v.x = x;
      v.y = y;
      v.z = z;
   }

   private void solveJ (Vector3d v, Vector3d b) {
      if (JDecomp == Decomp.None) {
         factorJ();
      }
      if (JDecomp == Decomp.Zero) {
         throw new IllegalStateException ("Rotational inertia is zero");
      }
      double x = b.x / L.m00;
      double y = (b.y - L.m10 * x) / L.m11;
      double z = (b.z - L.m21 * y - L.m20 * x) / L.m22;

      v.z = z / L.m22;
      v.y = (y - L.m21 * v.z) / L.m11;
      v.x = (x - L.m10 * v.y - L.m20 * v.z) / L.m00;
   }

   /**
    * Multiplies a twist by this spatial inertia and places the result in a
    * wrench:
    * 
    * <pre>
    *   wrr = M tw1
    * </pre>
    * 
    * @param wrr
    * result wrench
    * @param tw1
    * twist to multiply
    */
   public void mul (Wrench wrr, Twist tw1) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      wrr.f.crossAdd (tw1.w, com, tw1.v);
      wrr.f.scale (mass);
      J.mul (wrr.m, tw1.w);
      wrr.m.crossAdd (com, wrr.f, wrr.m);
   }

   /**
    * Multiplies a wrench by the inverse of this spatial inertia and places the
    * result in a twist.
    * 
    * <pre>
    *         -1 
    *  twr = M   wr1
    * </pre>
    * 
    * @param twr
    * result twist
    * @param wr1
    * wrench to multiply
    */
   public void mulInverse (Twist twr, Wrench wr1) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      Vector3d vtmp = new Vector3d();
      vtmp.crossAdd (wr1.f, com, wr1.m);
      solveJ (twr.w, vtmp);
      twr.v.scale (1 / mass, wr1.f);
      twr.v.crossAdd (com, twr.w, twr.v);
   }

   /**
    * Multiplies a spatial vector by the inverse of this spatial inertia and
    * places the result in another spatial vector.
    * 
    * <pre>
    *           -1    
    *  svr  =  M   sv1
    * </pre>
    * 
    * @param svr
    * result
    * @param sv1
    * spatial vector to multiply
    */
   public void mulInverse (SpatialVector svr, SpatialVector sv1) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      Vector3d vtmp = new Vector3d();
      vtmp.crossAdd (sv1.a, com, sv1.b);
      solveJ (svr.b, vtmp);
      svr.a.scale (1 / mass, sv1.a);
      svr.a.crossAdd (com, svr.b, svr.a);
   }

   /**
    * Multiplies a spatial vector by the right Cholesky factor of this spatial
    * inertia. In other words, if M = G G' is the Cholesky factorization of this
    * spatial inertia, then this routine computes svr = G' sv1.
    * 
    * @param svr
    * result
    * @param sv1
    * spatial vector to multiply
    */
   public void mulRightFactor (SpatialVector svr, SpatialVector sv1) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      if (JDecomp == Decomp.None) {
         factorJ();
      }
      svr.a.crossAdd (sv1.b, com, sv1.a);
      svr.a.scale (sqrtMass);
      svr.b.x = L.m00 * sv1.b.x + L.m10 * sv1.b.y + L.m20 * sv1.b.z;
      svr.b.y = L.m11 * sv1.b.y + L.m21 * sv1.b.z;
      svr.b.z = L.m22 * sv1.b.z;
   }

   /**
    * Multiplies a twist by the inverse of the right Cholesky factor of this
    * spatial inertia. In other words, if M = G G' is the Cholesky factorization
    * of this spatial inertia, then this routine computes svr = inv (G') sv1.
    * 
    * @param svr
    * result twist
    * @param sv1
    * twist to multiply
    */
   public void mulRightFactorInverse (SpatialVector svr, SpatialVector sv1) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      solveU (svr.b, sv1.b);
      svr.a.scale (1 / sqrtMass, sv1.a);
      svr.a.crossAdd (com, svr.b, svr.a);
   }

   /**
    * Multiplies a spatial vector by the left Cholesky factor of this spatial
    * inertia. In other words, if M = G G' is the Cholesky factorization of this
    * spatial inertia, then this routine computes svr = G sv1.
    * 
    * @param svr
    * result
    * @param sv1
    * spatial vector to multiply
    */
   public void mulLeftFactor (SpatialVector svr, SpatialVector sv1) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      if (JDecomp == Decomp.None) {
         factorJ();
      }
      svr.a.scale (sqrtMass, sv1.a);
      svr.b.z = L.m20 * sv1.b.x + L.m21 * sv1.b.y + L.m22 * sv1.b.z;
      svr.b.y = L.m10 * sv1.b.x + L.m11 * sv1.b.y;
      svr.b.x = L.m00 * sv1.b.x;
      svr.b.crossAdd (com, svr.a, svr.b);
   }

   /**
    * Multiplies a spatial vector by the inverse of the left Cholesky factor of
    * this spatial inertia. In other words, if M = G G' is the Cholesky
    * factorization of this spatial inertia, then this routine computes svr =
    * inv (G) sv1.
    * 
    * @param svr
    * result
    * @param sv1
    * spatial vector to multiply
    */
   public void mulLeftFactorInverse (SpatialVector svr, SpatialVector sv1) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      svr.b.crossAdd (sv1.a, com, sv1.b);
      solveL (svr.b, svr.b);
      svr.a.scale (1 / sqrtMass, sv1.a);
   }

   /**
    * Computes the coriolis forces induced by a given fixed-frame velocity
    * acting on this spatial inertia. Both the force and velocity are given in
    * the same coordinate frame as this inertia. The velocity is assumed to be
    * fixed with respect to this frame, as opposed to moving with the body. For
    * the latter case, one should use {@link #bodyCoriolisForce
    * bodyCoriolisForce} instead.
    * 
    * @param wr
    * returns the coriolis forces
    * @param tw
    * velocity inducing the corioilis force.
    * @see #bodyCoriolisForce
    */
   public void coriolisForce (Wrench wr, Twist tw) {
      // Standard Newton-Euler formulation for coriolis force:
      //
      // tw.w X (tw.w X mass com)
      //
      // tw.w X JmassComCom tw.w
      //
      wr.f.cross (tw.w, com);
      wr.f.scale (mass);
      wr.f.cross (tw.w, wr.f);
      Vector3d w = tw.w;
      wr.m.x = m33*w.x + m34*w.y + m35*w.z;
      wr.m.y = m43*w.x + m44*w.y + m45*w.z;
      wr.m.z = m53*w.x + m54*w.y + m55*w.z;
      wr.m.cross (tw.w, wr.m);
   }

   /**
    * Computes the coriolis forces induced by a given body-coordinate velocity
    * acting on this spatial inertia. Both the force and velocity are given in
    * the same coordinate frame as this inertia. The velocity is assumed to be
    * moving with respect to this frame, as opposed to being fixed. For the
    * latter case, one should use {@link #coriolisForce coriolisForce} instead.
    * 
    * @param wr
    * returns the coriolis forces
    * @param tw
    * velocity inducing the corioilis force.
    * @see #coriolisForce
    */
   public void bodyCoriolisForce (Wrench wr, Twist tw) {
      // This computes
      //
      // f = -tw.w X f'
      //
      // m = -tw.v X f' - tw.w X m'
      //
      // where
      //
      // [ f' ]     [ v ]
      // [    ] = M [   ]
      // [ m' ]     [ w ]
      //
      mul (wr, tw);
      wr.m.cross (tw.w, wr.m);
      wr.m.crossAdd (tw.v, wr.f, wr.m);
      wr.f.cross (tw.w, wr.f);
   }

   /**
    * Transforms this inertia into a new coordinate frame, given a spatial
    * transformation matrix.
    * 
    * @param X
    * spatial transform from the current frame into the new frame
    */
   public void transform (RigidTransform3d X) {
      com.transform (X);
      J.mulLeftAndTransposeRight (X.R);
      JDecomp = Decomp.None;
      updateMatrixElements();
   }

   /**
    * Transforms this inertia into a new coordinate frame, given an inverse
    * spatial transformation matrix.
    * 
    * @param X
    * spatial transform from the new frame into the current frame
    */
   public void inverseTransform (RigidTransform3d X) {
      com.inverseTransform (X);
      J.mulTransposeLeftAndRight (X.R);
      JDecomp = Decomp.None;
      updateMatrixElements();
   }

   /**
    * Rotates this inertia into a new coordinate frame given by a rotation
    * matrix.
    * 
    * @param R
    * rotation transform from the current frame into the new frame
    */
   public void transform (RotationMatrix3d R) {
      com.transform (R);
      J.mulLeftAndTransposeRight (R);
      JDecomp = Decomp.None;
      updateMatrixElements();
   }

   /**
    * Rotates this inertia into a new coordinate frame, given by the inverse of
    * a rotation matrix.
    * 
    * @param R
    * rotation transform from the new frame into the current frame
    */
   public void inverseTransform (RotationMatrix3d R) {
      com.inverseTransform (R);
      J.mulTransposeLeftAndRight (R);
      JDecomp = Decomp.None;
      updateMatrixElements();
   }

   /**
    * Scale the distance units associated with this SpatialInertia.
    * 
    * @param s
    * scaling factor
    */
   public void scaleDistance (double s) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      this.com.scale (s);
      setMassCom (mass*com.x, mass*com.y, mass*com.z);
      scaleJ (s * s, J);
      addScaledComCom (J, -mass, com);
   }

   /**
    * Scale the mass units associated with this SpatialInertia. This will affect
    * both the lump mass and the moment of inertia.
    * 
    * @param s
    * scaling factor
    */
   public void scaleMass (double s) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      mass *= s;
      sqrtMass = Math.sqrt (mass);
      setMassDiagonal (mass);
      setMassCom (mass*com.x, mass*com.y, mass*com.z);
      scaleJ (s, J);
      addScaledComCom (J, -mass, com);
   }

   /**
    * Computes the effective mass of this inertia along a specific twist
    * direction. This is done by the formula
    * 
    * <pre>
    *              -1   T  -1
    *    m = ( tw M   tw  )
    * </pre>
    * 
    * where M is this spatial inertia and tw is the twist direction, which is
    * treated as a row vector.
    * 
    * @param wr
    * direction along which effective mass is to be computed
    * @return directed mass
    */
   public double directedMass (Wrench wr) {
      if (componentUpdateNeeded) {
         updateComponents();
      }
      mulInverse (myTmpT, wr);
      return 1 / myTmpT.dot (wr);
   }

   /**
    * Creates a new spatial inertia corresponding to the center of a sphere of
    * uniform density.
    * 
    * @param m
    * mass
    * @param r
    * sphere radius
    */
   public static SpatialInertia createSphereInertia (double m, double r) {
      return createEllipsoidInertia (m, r, r, r);
   }

   /**
    * Creates a new spatial inertia corresponding to the center of an
    * axis-aligned box of uniform density.
    * 
    * @param m
    * mass
    * @param wx
    * width along the x direction
    * @param wy
    * width along the y direction
    * @param wz
    * width along the z direction
    */
   public static SpatialInertia createBoxInertia (
      double m, double wx, double wy, double wz) {
      SpatialInertia M = new SpatialInertia();
      M.setBox (m, wx, wy, wz);
      return M;
   }

   /**
    * Creates a new spatial inertia corresponding to the center of a cylinder of
    * uniform density aligned with the z axis.
    * 
    * @param m
    * mass
    * @param r
    * radius
    * @param l
    * length
    */
   public static SpatialInertia createCylinderInertia (
      double m, double r, double l) {
      SpatialInertia M = new SpatialInertia();
      M.setCylinder (m, r, l);
      return M;
   }

   /**
    * Creates a new spatial inertia corresponding to the center of an
    * axis-aligned ellispoid of uniform density.
    * 
    * @param m
    * mass
    * @param a
    * semi-axis length in the x direction
    * @param b
    * semi-axis length in the y direction
    * @param c
    * semi-axis length in the z direction
    */
   public static SpatialInertia createEllipsoidInertia (
      double m, double a, double b, double c) {
      SpatialInertia M = new SpatialInertia();
      M.setEllipsoid (m, a, b, c);
      return M;
   }

   /**
    * Add to a rotational inertia the effect of a point mass at location r.
    * This is equivalent to computing
    *
    * J = J - m [c] [c]
    */
   public static void addPointRotationalInertia (
      Matrix3dBase J, double m, Vector3d r) {
      
      double rx2 = r.x*r.x;
      double ry2 = r.y*r.y;
      double rz2 = r.z*r.z;

      J.m00 += m*(rz2+ry2);
      J.m01 -= m*(r.x*r.y);
      J.m02 -= m*(r.x*r.z);

      J.m11 += m*(rz2+rx2);
      J.m12 -= m*(r.y*r.z);

      J.m22 += m*(rx2+ry2);

      J.m10 = J.m01;
      J.m20 = J.m02;
      J.m21 = J.m12;
   }
   
   public boolean equals (Matrix6dBase M1) {
      return super.equals (M1);
   }
   
   /**
    * For debugging: compares all the fields of M with the this
    * inertia, including the component fields that depend on the
    * matrix entries.
    * 
    * @param M inertia to compare with
    * @return true if all fields of M 
    */
   public boolean fullEquals (SpatialInertia M) {
      if (!equals(M)) {
         return false;
      }
      if (mass != M.mass) {
         return false;
      }
      if (sqrtMass != M.sqrtMass) {
         return false;
      }
      if (!com.equals (M.com)) {
         return false;
      }
      if (!J.equals (M.J)) {
         return false;
      }
      if (!L.equals (M.L)) {
         return false;
      }
      if (componentUpdateNeeded != M.componentUpdateNeeded) {
         return false;
      }
      if (JDecomp != M.JDecomp) {
         return false;
      }
      return true;
   }

   /**
    * For debugging: creates a string showing this inertia and all its fields
    * 
    * @param fmt numeric format
    * @return string representation of the inertia and all its components
    */
   public String toStringAll (String fmt) {
      StringBuilder sbuild = new StringBuilder();
      sbuild.append (toString(fmt)+"\n");
      sbuild.append ("mass="+mass+"\n");
      sbuild.append ("com="+com.toString(fmt)+"\n");
      sbuild.append ("J=\n"+J.toString(fmt));
      sbuild.append ("L=\n"+L.toString(fmt));
      sbuild.append ("componentUpdateNeeded=" + componentUpdateNeeded+"\n");
      sbuild.append ("JDecomp=" + JDecomp);
      return sbuild.toString();      
   }
   
//   /**
//    * Creates a new spatial inertia corresponding to a closed volume with a
//    * specified density.
//    * 
//    * @param mesh
//    * closed mesh
//    * @param density
//    * density of the volume
//    */
//   public static SpatialInertia createClosedVolumeInertia (
//      PolygonalMesh mesh, double density) {
//      SpatialInertia M = new SpatialInertia();
//      M.setClosedVolume (mesh, density);
//      return M;
//   }

}
