/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Class to produce the polar decompositions of a 3x3 matrix. The
 * <i>right</i> polar decomposition of a matrix M is produced by the
 * {@link #factor} method and is given by 
 * <pre>
 * M = Q P
 * </pre>
 * where <code>Q</code> is orthogonal and <code>P</code> is symmetric
 * positive definite (or indefinite if <code>M</code> is singular).
 * Note that <code>Q</code> is not necessarily right-handed. It is
 * possible to define an alternative decomposition,
 * <pre>
 * M = R H
 * </pre>
 * where <code>R</code> is a right-handed rotation matrix and
 * <code>H</code> is symmetric indefinite. Alternatively, if
 * <code>Q</code> is not right-handed, one can make it so by
 * negating one of its axes, using 
 * <pre>
 * Q' = Q N
 * </pre>
 * where <code>N</code> is a diagonal matrix whose elements are
 * either 1 or -1 and whose determinant is -1. At present, this
 * class chooses <code>N</code> with a single -1 at the same
 * location as the diagonal element of <code>Q</code> that
 * is nearest to 1. The idea here is to flip the single axis
 * that is <i>least</i> affected by <code>Q</code>.
 * 
 * <p>
 * The <i>left</i> polar decomposition is produced by
 * the {@link #factorLeft} method and is given by 
 * <pre>
 * M = P Q
 * </pre>
 * with the alternative decomposition,
 * <pre>
 * M = H R
 * </pre>
 * Note that the <code>Q</code> and <code>R</code> matrices associated
 * with the right and left factorizations are identical, whereas
 * the <code>P</code> and <code>H</code> matrices are not.
 */
public class PolarDecomposition3d {

   private SVDecomposition3d mySvd;
   private Matrix3d myU;
   private Matrix3d myV;
   private RotationMatrix3d myR;
   private Matrix3d myQ;
   private Matrix3d myP;
   private Vector3d mySig;
   private boolean myRightHandedP;

   private enum State {
      Undefined,
         LeftFactorization,
         RightFactorization };
   
   private State myState = State.Undefined;

   public PolarDecomposition3d() {
      myU = new Matrix3d();
      myV = new Matrix3d();
      mySig = new Vector3d();
      myQ = new Matrix3d();
      mySvd = new SVDecomposition3d (myU, myV);
      myR = new RotationMatrix3d();
      myP = new Matrix3d();
   }

   public PolarDecomposition3d (Matrix3dBase M) {
      this();
      factor (M);
   }

   //
   // Form the symmetric product
   //
   //       [ s0       ]
   // P = V [    s1    ] V^T
   //       [       s2 ]
   //

   private void symmetricProduct (
      Matrix3d P, Matrix3dBase V, double s0, double s1, double s2) {

      P.m00 = s0*V.m00*V.m00 + s1*V.m01*V.m01 + s2*V.m02*V.m02;
      P.m11 = s0*V.m10*V.m10 + s1*V.m11*V.m11 + s2*V.m12*V.m12;
      P.m22 = s0*V.m20*V.m20 + s1*V.m21*V.m21 + s2*V.m22*V.m22;

      P.m01 = s0*V.m00*V.m10 + s1*V.m01*V.m11 + s2*V.m02*V.m12;
      P.m02 = s0*V.m00*V.m20 + s1*V.m01*V.m21 + s2*V.m02*V.m22;
      P.m12 = s0*V.m10*V.m20 + s1*V.m11*V.m21 + s2*V.m12*V.m22;

      P.m10 = P.m01;
      P.m20 = P.m02;
      P.m21 = P.m12;
   }
   
   /**
    * Adds a scaled outer product to a matrix. The outer product
    * is formed from the single vector <code>(vx, vy, vz)</code> and 
    * takes the form
    * <pre>
    *      T
    * s v v
    * </pre>
    */
   protected void addSymmetricProduct (
      Matrix3d M, double s, double vx, double vy, double vz) {

      double svx = s*vx;
      double svy = s*vy;
      double svz = s*vz;
      
      double op01 = vx*svy;
      double op02 = vx*svz;
      double op12 = vy*svz;
      
      M.m00 += vx*svx;
      M.m11 += vy*svy;
      M.m22 += vz*svz;

      M.m01 += op01;
      M.m02 += op02;
      M.m12 += op12;
      
      M.m10 += op01;
      M.m20 += op02;
      M.m21 += op12;
   }  

   
   private void setFromRotation (RotationMatrix3d R) {
      myQ.set (R);
      myR.set (myQ);
      myP.set (Matrix3d.IDENTITY);
      mySig.set (1, 1, 1);
      myRightHandedP = true;
   }

   private void factorInternal (Matrix3dBase M) {
      mySvd.factor (M);
      mySvd.getS (mySig);
      myQ.mulTransposeRight (myU, myV);
      if (myQ.orthogonalDeterminant() < 0) {
         // Set R = U V^T with the last column of U negated. Note that
         // this has the same effect as negating the last column of V.
         myU.m02 = -myU.m02;
         myU.m12 = -myU.m12;
         myU.m22 = -myU.m22;  
         myR.mulTransposeRight (myU, myV);
         myU.m02 = -myU.m02;
         myU.m12 = -myU.m12;
         myU.m22 = -myU.m22;        
         myRightHandedP = false;
      }
      else {
         myR.set (myQ);
         myRightHandedP = true;
      }
   }

   public void factor (Matrix3dBase M) {
      if (M instanceof RotationMatrix3d) {
         setFromRotation ((RotationMatrix3d)M);
      }
      else {
         factorInternal (M);
         symmetricProduct (myP, myV, mySig.x, mySig.y, mySig.z);
      }
      myState = State.RightFactorization;
   }

   public void factorLeft (Matrix3dBase M) {
      if (M instanceof RotationMatrix3d) {
         setFromRotation ((RotationMatrix3d)M);
      }
      else {
         factorInternal (M);
         symmetricProduct (myP, myU, mySig.x, mySig.y, mySig.z);
      }
      myState = State.LeftFactorization;
   }

   public RotationMatrix3d getR() {
      return myR;
   }

   public void getR (Matrix3dBase R) {
      R.set (myR);
   }

   public Matrix3d getQ () {
      return myQ;
   }

   public void getQ (Matrix3d Q) {
      Q.set (myQ);
   }

   public void getH (Matrix3d H) {
      H.set (myP);
      if (!myRightHandedP) {
         if (myState == State.LeftFactorization) {
            addSymmetricProduct (H, -2*mySig.z, myU.m02, myU.m12, myU.m22);
         }
         else {
            addSymmetricProduct (H, -2*mySig.z, myV.m02, myV.m12, myV.m22);
         }
      }
   }

   public Matrix3d getP() {
      return myP;        
   }

   public void getP (Matrix3d P) {
      P.set (myP);
   }

   public Matrix3d getV() {
      if (myState == State.LeftFactorization) {
         return myU;
      }
      else {
         return myV;
      }
   }

   public void getSig (Vector3d sig) {
      if (myRightHandedP) {
         sig.set (mySig);
      } 
      else {
         sig.set (mySig.x, mySig.y, -mySig.z);
      }
   }
   
   public boolean isRightHanded() {
      return myRightHandedP;
   }
   
   /**
    * Return the diagonal elements of the matrix <code>N</code> which
    * is used to flip rows or columns of <code>Q</code> (or some 
    * product thereof) in the event that <code>Q</code> is not right-handed.
    * If <code>det(Q) == 1</code>, then <code>N</code> is the identity 
    * matrix. Otherwise, if <code>det(Q) = -1</code>, then <code>N</code> 
    * flips the column corresponding to the diagonal element of <code>Q</code>
    * which is nearest to 1.
    *  
    * @param Ndiag returns the diagonal elements of <code>N</code>.
    */
   public void getN (Vector3d Ndiag) {
      Ndiag.set (Vector3d.ONES);
      if (!myRightHandedP) {
         Ndiag.set (getMaxQDiagIndex(), -1);
      }
   }
   
   /**
    * Returns the index of the maximum diagonal element of <code>Q</code>.
    * This is used to create the matrix <code>N</code> which is
    * returned by {@link #getN}.
    *  
    * @return index of the maximum diagonal element of <code>Q</code>.
    */  
   public int getMaxQDiagIndex() {
      int i = 2;
      double maxd = myQ.m22;
      if (myQ.m11 > maxd) {
         maxd = myQ.m11;
         i = 1;
      }
      if (myQ.m00 > maxd) {
         maxd = myQ.m00;
         i = 0;
      }
      return i;
   }
   
   /**
    * Flip an axis of R in order to make it right-handed. The
    * axis chosen is one that was transformed *least* by Q,
    * which will correspond to a diagonal Q element nearest to 1. 
    * 
    * @param R unitary matrix 
    */
   public void makeRightHanded (RotationMatrix3d R) {
      int i = 2;
      double maxd = myQ.m22;
      if (myQ.m11 > maxd) {
         maxd = myQ.m11;
         i = 1;
      }
      if (myQ.m00 > maxd) {
         maxd = myQ.m00;
         i = 0;
      }
      if (i == 0) {
         // negate column 0
         R.m00 = -R.m00; R.m10 = -R.m10; R.m20 = -R.m20;
      }
      else if (i == 1) {
         // negate column 1
         R.m01 = -R.m01; R.m11 = -R.m11; R.m21 = -R.m21;
      }
      else {
         // negate column 2
         R.m02 = -R.m02; R.m12 = -R.m12; R.m22 = -R.m22;
      }
   }

}
