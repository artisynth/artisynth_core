/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.*;

public class JythonMatrixSupport {
   protected static String myFmtStr = "%g";

   public static void setFormat (String fmtStr) {
      VectorBase.setDefaultFormat (fmtStr);
      MatrixBase.setDefaultFormat (fmtStr);
      myFmtStr = fmtStr;
   }

   public static String getFormat() {
      return myFmtStr;
   }

   /* === VectorNd routines === */

   public static VectorNd vNCopy (VectorNd v1) {
      return new VectorNd (v1);
   }

   public static VectorNd vNNegate (VectorNd v1) {
      VectorNd tmp = new VectorNd (v1);
      tmp.negate();
      return tmp;
   }

   public static VectorNd vNAbs (VectorNd v1) {
      VectorNd tmp = new VectorNd (v1);
      tmp.absolute();
      return tmp;
   }

   public static VectorNd vNAdd (VectorNd v1, VectorNd v2) {
      VectorNd res = new VectorNd (v2);
      res.add (v1);
      return res;
   }

   public static VectorNd vNAdd (VectorNd v1, VectorBase v2) {
      VectorNd res = new VectorNd (v2);
      res.add (v1);
      return res;
   }

   public static VectorNd vNAdd (VectorNd v1, MatrixBase M2) {
      VectorNd res = new VectorNd (M2);
      res.add (v1);
      return res;
   }

   public static VectorNd vNSub (VectorNd v1, VectorNd v2) {
      VectorNd res = new VectorNd (v2);
      res.sub (v1, res);
      return res;
   }

   public static VectorNd vNSub (VectorNd v1, VectorBase v2) {
      VectorNd res = new VectorNd (v2);
      res.sub (v1, res);
      return res;
   }

   public static VectorNd vNSub (VectorNd v1, MatrixBase M2) {
      VectorNd res = new VectorNd (M2);
      res.sub (v1, res);
      return res;
   }

   public static VectorNd vNMul (VectorNd v1, double s) {
      VectorNd res = new VectorNd (v1);
      res.scale (s);
      return res;
   }

   public static VectorNd vNDiv (VectorNd v1, double s) {
      VectorNd res = new VectorNd (v1);
      res.scale (1 / s);
      return res;
   }

   public static void vNIAdd (VectorNd v1, VectorNd v2) {
      v1.add (v2);
   }

   public static void vNIAdd (VectorNd v1, VectorBase v2) {
      VectorNd tmp = new VectorNd (v2);
      v1.add (tmp);
   }

   public static void vNIAdd (VectorNd v1, MatrixBase M2) {
      VectorNd tmp = new VectorNd (M2);
      v1.add (tmp);
   }

   public static void vNISub (VectorNd v1, VectorNd v2) {
      v1.sub (v2);
   }

   public static void vNISub (VectorNd v1, VectorBase v2) {
      VectorNd tmp = new VectorNd (v2);
      v1.sub (tmp);
   }

   public static void vNISub (VectorNd v1, MatrixBase M2) {
      VectorNd tmp = new VectorNd (M2);
      v1.sub (tmp);
   }

   public static void vNIMul (VectorNd v1, double s) {
      v1.scale (s);
   }

   public static void vNIDiv (VectorNd v1, double s) {
      v1.scale (1 / s);
   }

   public static VectorNd vNMul (VectorNd v1, VectorNd v2) {
      VectorNd res = new VectorNd (v2);
      res.sub (v1, res);
      return res;
   }

   public static VectorNd vNMul (VectorNd v1, VectorBase v2) {
      VectorNd res = new VectorNd (v2);
      res.sub (v1, res);
      return res;
   }

   public static VectorNd vNMul (VectorNd v1, MatrixBase M2) {
      VectorNd res = new VectorNd (M2);
      res.sub (v1, res);
      return res;
   }

   public static Vector3d v3Add (Vector3d v1, Vector3d v2) {
      Vector3d res = new Vector3d (v2);
      res.add (v1);
      return res;
   }

   public static VectorNd v3Add (Vector3d v1, VectorBase v2) {
      VectorNd res = new VectorNd (v2);
      if (res.size() != 3) {
         throw new ImproperSizeException ("argument sizes do not conform");
      }
      double[] buf = res.getBuffer();
      buf[0] += v1.x;
      buf[1] += v1.y;
      buf[2] += v1.z;
      return res;
   }

   public static Vector2d v2Add (Vector2d v1, Vector2d v2) {
      Vector2d res = new Vector2d (v2);
      res.add (v1);
      return res;
   }

   public static VectorNd v2Add (Vector2d v1, VectorBase v2) {
      VectorNd res = new VectorNd (v2);
      if (res.size() != 2) {
         throw new ImproperSizeException ("argument sizes do not conform");
      }
      double[] buf = res.getBuffer();
      buf[0] += v1.x;
      buf[1] += v1.y;
      return res;
   }

   public static VectorNd v2Add (Vector2d v1, MatrixBase m2) {
      if (m2.colSize() != 1 || m2.rowSize() != 2) {
         throw new ImproperSizeException ("argument sizes do not conform");
      }
      VectorNd res = new VectorNd (2);
      double[] buf = res.getBuffer();
      buf[0] = v1.x + m2.get (0, 0);
      buf[1] = v1.y + m2.get (1, 0);
      return res;
   }

   public static Vector4d v4Add (Vector4d v1, Vector4d v2) {
      Vector4d res = new Vector4d (v2);
      res.add (v1);
      return res;
   }

   public static VectorNd v4Add (Vector4d v1, VectorBase v2) {
      VectorNd res = new VectorNd (v2);
      if (res.size() != 4) {
         throw new ImproperSizeException ("argument sizes do not conform");
      }
      double[] buf = res.getBuffer();
      buf[0] += v1.x;
      buf[1] += v1.y;
      buf[2] += v1.z;
      buf[3] += v1.w;
      return res;
   }

   // public static VectorNd vNAdd (VectorNd v1, VectorObject v2)
   // {
   // VectorNd res;
   // if (v2 instanceof VectorNd)
   // { res = new VectorNd((VectorNd)v2);
   // }
   // else
   // { res = new VectorNd(v2);
   // }
   // res.add (v1);
   // return res;
   // }

   // public static VectorNd vNAdd (VectorNd v1, Vector2d v2)
   // {
   // VectorNd res = new VectorNd(v2);
   // res.add (v1);
   // return res;
   // }

   // public static VectorNd vNAdd (VectorNd v1, Vector3d v2)
   // {
   // VectorNd res = new VectorNd(v2);
   // res.add (v1);
   // return res;
   // }

   // public static VectorNd vNAdd (VectorNd v1, Vector4d v2)
   // {
   // VectorNd res = new VectorNd(v2);
   // res.add (v1);
   // return res;
   // }

   public static VectorNd vNScale (VectorNd v1, double s) {
      VectorNd res = new VectorNd (v1);
      res.scale (s);
      return res;
   }

   public static Object vOMul (VectorBase v1, VectorBase v2) {
      int rsize, csize, ksize;

      if (v1.isRowVector()) {
         rsize = 1;
         ksize = v1.size();
      }
      else {
         rsize = v1.size();
         ksize = 1;
      }
      if (v2.isRowVector()) {
         csize = v2.size();
         if (ksize != 1) {
            throw new ImproperSizeException();
         }
      }
      else {
         csize = 1;
         if (ksize != v2.size()) {
            throw new ImproperSizeException();
         }
      }
      if (rsize == 1 && csize == 1) {
         double sum = 0;
         for (int k = 0; k < ksize; k++) {
            sum += v1.get (k) * v2.get (k);
         }
         return new Double (sum);
      }
      else {
         MatrixNd res = new MatrixNd (rsize, csize);
         for (int i = 0; i < rsize; i++) {
            for (int j = 0; j < csize; j++) {
               res.set (i, j, v1.get (i) * v2.get (j));
            }
         }
         return res;
      }
   }

   public static Object mOMul (MatrixBase m1, MatrixBase m2) {
      MatrixNd res = new MatrixNd (m1.rowSize(), m2.colSize());
      if (m1.colSize() != m2.rowSize()) {
         throw new ImproperSizeException();
      }
      double[] buf = res.getBuffer();
      for (int i = 0; i < res.nrows; i++) {
         for (int j = 0; j < res.ncols; j++) {
            double sum = 0;
            for (int k = 0; k < m1.colSize(); k++) {
               sum += m1.get (i, k) * m2.get (k, j);
            }
            buf[i * res.ncols + j] = sum;
         }
      }
      if (res.nrows == 1 && res.ncols == 1) {
         return new Double (buf[0]);
      }
      else {
         return res;
      }
   }

   public static Object mOMul (MatrixBase m1, VectorBase v2) {
      if (v2.isRowVector()) {
         MatrixNd res = new MatrixNd (m1.rowSize(), v2.size());
         if (m1.colSize() != 1) {
            throw new ImproperSizeException();
         }
         double[] buf = res.getBuffer();
         for (int i = 0; i < res.nrows; i++) {
            for (int j = 0; j < res.ncols; j++) {
               buf[i * res.ncols + j] = m1.get (i, 0) * v2.get (j);
            }
         }
         if (res.nrows == 1 && res.ncols == 1) {
            return new Double (buf[0]);
         }
         else {
            return res;
         }
      }
      else {
         VectorNd res = new VectorNd (m1.rowSize());
         if (m1.colSize() != v2.size()) {
            throw new ImproperSizeException();
         }
         double[] buf = res.getBuffer();
         for (int i = 0; i < res.size; i++) {
            double sum = 0;
            for (int j = 0; j < v2.size(); j++) {
               sum += m1.get (i, j) * v2.get (j);
            }
            buf[i] = sum;
         }
         if (res.size == 1) {
            return new Double (buf[0]);
         }
         else {
            return res;
         }
      }
   }

   public static MatrixNd mOInvert (MatrixBase M) {
      MatrixNd res = new MatrixNd (M);
      res.invert();
      return res;
   }

   public static MatrixNd mOTranspose (MatrixBase M) {
      MatrixNd res = new MatrixNd (M);
      res.transpose();
      return res;
   }

   public static VectorNd mOTranspose (VectorBase v) {
      VectorNd res = new VectorNd (v);
      res.setRowVector (!v.isRowVector());
      return res;
   }

   public static String toStr (VectorBase v) {
      return v.toString (new NumberFormat(myFmtStr));
   }

   public static String toStr (MatrixBase M) {
      return M.toString (new NumberFormat(myFmtStr));
   }

}
