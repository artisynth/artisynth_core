/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.*;

public class PolarDecomposition3d {

   private SVDecomposition3d mySvd;
   private RotationMatrix3d myR;
   private Matrix3d myP;
   private Vector3d mySig;

   private enum State {
      Undefined,
         LeftFactorization,
         RightFactorization };
   
   private State myState = State.Undefined;

   public PolarDecomposition3d() {
      mySvd = new SVDecomposition3d();
      myR = new RotationMatrix3d();
      myP = new Matrix3d();
      mySig = new Vector3d();
   }

   public PolarDecomposition3d (Matrix3dBase M) {
      this();
      factor (M);
   }

   public void factor (Matrix3dBase M) {
      mySvd.polarDecomposition (myR, myP, M);
      mySvd.getS (mySig);
      myState = State.RightFactorization;
   }

   public void factorLeft (Matrix3dBase M) {
      mySvd.leftPolarDecomposition (myP, myR, M);
      mySvd.getS (mySig);
      myState = State.LeftFactorization;
   }

   // public void setFast (Matrix3dBase A) {
   //    AA.mulTransposeLeft (A);
   //    AA.getEigenValues (e, V);
   //    P.set (V);
   //    P.mulDiagonalRight (Math.sqrt (e.x), Math.sqrt (e.y), Math.sqrt (e.z));
   //    P.mulTransposeRight (P, V);
   //    Q.mulInverseRight (A, P);
   // }

   // public void setLeftFast (Matrix3dBase A) {
   //    AA.mulTransposeRight (A);
   //    AA.getEigenValues (e, V);
   //    P.set (V);
   //    P.mulDiagonalRight (Math.sqrt (e.x), Math.sqrt (e.y), Math.sqrt (e.z));
   //    P.mulTransposeRight (P, V);
   //    Q.mulInverseLeft (P, A);
   // }

   // public void normalizeQ() {
   //    double len = Math.sqrt (Q.m00 * Q.m00 + Q.m10 * Q.m10 + Q.m20 * Q.m20);
   //    Q.m00 /= len;
   //    Q.m10 /= len;
   //    Q.m20 /= len;

   //    double dot = Q.m00 * Q.m01 + Q.m10 * Q.m11 + Q.m20 * Q.m21;
   //    Q.m01 -= dot * Q.m00;
   //    Q.m11 -= dot * Q.m10;
   //    Q.m21 -= dot * Q.m20;

   //    len = Math.sqrt (Q.m01 * Q.m01 + Q.m11 * Q.m11 + Q.m21 * Q.m21);
   //    Q.m01 /= len;
   //    Q.m11 /= len;
   //    Q.m21 /= len;

   //    double ax = Q.m10 * Q.m21 - Q.m20 * Q.m11;
   //    double ay = Q.m20 * Q.m01 - Q.m00 * Q.m21;
   //    double az = Q.m00 * Q.m11 - Q.m10 * Q.m01;
   //    if (ax * Q.m20 + ay * Q.m21 + az * Q.m22 < 0) {
   //       Q.m02 = -ax;
   //       Q.m12 = -ay;
   //       Q.m22 = -az;
   //    }
   //    else {
   //       Q.m02 = ax;
   //       Q.m12 = ay;
   //       Q.m22 = az;
   //    }
   // }

   // public boolean isQRightHanded() {
   //    double ax = Q.m10 * Q.m21 - Q.m20 * Q.m11;
   //    double ay = Q.m20 * Q.m01 - Q.m00 * Q.m21;
   //    double az = Q.m00 * Q.m11 - Q.m10 * Q.m01;
   //    return (ax * Q.m20 + ay * Q.m21 + az * Q.m22 > 0);
   // }

   // public void set (Matrix3dBase A) {
   //    setFast (A);
   //    normalizeQ();
   //    P.mulTransposeLeft (Q, A);
   // }

   // public void setLeft (Matrix3dBase A) {
   //    setLeftFast (A);
   //    normalizeQ();
   //    P.mulTransposeRight (A, Q);
   // }

   public RotationMatrix3d getR() {
      return myR;
   }

   public void getR (Matrix3dBase R) {
      R.set (myR);
   }

   public Matrix3d getP() {
      return myP;        
   }

   public void getP (Matrix3dBase P) {
      P.set (myP);
   }

   public Matrix3d getV() {
      if (myState == State.LeftFactorization) {
         return mySvd.getU();
      }
      else {
         return mySvd.getV();
      }
   }

   public Vector3d getSig() {
      return mySig;
   }

   public void getSig (Vector3d sig) {
      sig.set (mySig);
   }   

}
