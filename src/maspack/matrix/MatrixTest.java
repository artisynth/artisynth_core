/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;
import java.util.LinkedList;
import java.io.*;

import maspack.util.*;
import maspack.matrix.Matrix.Partition;

class MatrixTest extends UnitTest {
   static double DOUBLE_PREC = 2.220446049250313e-16;
   static double EPSILON = 10 * DOUBLE_PREC;

   Exception eActual;
   Exception eExpected;

   Random randGen = RandomGenerator.get();

   Matrix MRsave = new MatrixNd (1, 1);
   MatrixNd MX = new MatrixNd (1, 1);
   VectorNd vx = new VectorNd(0);

   double mulTol = 0;

   private double getExpected (double value, int i, int j) {
      if (isReadOnly (i, j)) {
         return getReadOnly (i, j);
      }
      else {
         return value;
      }
   }

   protected double getReadOnly (int i, int j) {
      return 0;
   }

   protected boolean isReadOnly (int i, int j) {
      return false;
   }

   MatrixNd readOnlyFields = new MatrixNd (1, 1);

   boolean equals (Matrix MR, Matrix M1) {
      return false;
   }

   boolean epsilonEquals (Matrix MR, Matrix M1, double tol) {
      return false;
   }

   void add (Matrix MR, Matrix M1, Matrix M2) {
   }

   void add (Matrix MR, Matrix M1) {
   }

   void sub (Matrix MR, Matrix M1, Matrix M2) {
   }

   void sub (Matrix MR, Matrix M1) {
   }

   void mul (Matrix MR, Matrix M1) {
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
   }

   void mulTranspose (Matrix MR, Matrix M1) {
   }

   void mulTransposeRight (Matrix MR, Matrix M1, Matrix M2) {
   }

   void mulTransposeLeft (Matrix MR, Matrix M1, Matrix M2) {
   }

   void mulTransposeBoth (Matrix MR, Matrix M1, Matrix M2) {
   }

   void invert (Matrix MR) {
   }

   void invert (Matrix MR, Matrix M1) {
   }

   void mulInverse (Matrix MR) {
   }

   void mulInverse (Matrix MR, Matrix M1) {
   }

   void mulInverseRight (Matrix MR, Matrix M1, Matrix M2) {
   }

   void mulInverseLeft (Matrix MR, Matrix M1, Matrix M2) {
   }

   void mulInverseBoth (Matrix MR, Matrix M1, Matrix M2) {
   }

   void mulAdd (Matrix MR, Matrix M1, Matrix M2) {
   }
      
   void transpose (Matrix MR) {
   }

   void transpose (Matrix MR, Matrix M1) {
   }

   void negate (Matrix MR) {
   }

   void negate (Matrix MR, Matrix M1) {
   }

   void scale (Matrix MR, double s) {
   }

   void scale (Matrix MR, double s, Matrix M1) {
   }

   void scaledAdd (Matrix MR, double s, Matrix M1) {
   }

   void scaledAdd (Matrix MR, double s, Matrix M1, Matrix M2) {
   }

   void set (Matrix MR, Matrix M1) {
   }

   void setZero (Matrix MR) {
   }

   void setDiagonal (Matrix MR, double[] diagValues) {
   }

   void setIdentity (Matrix MR) {
   }

   double oneNorm (Matrix M1) {
      return 0;
   }

   double infinityNorm (Matrix M1) {
      return 0;
   }

   double frobeniusNorm (Matrix M1) {
      return 0;
   }

   static void checkExceptions (Exception actual, Exception expected) {
      if (expected != null) {
         if (actual == null ||
             !actual.toString().equals (expected.toString())) {
            throw new TestException ("Expected exception "
            + expected.toString() + ",\nGot "
            + (actual == null ? "null" : actual.toString()));
         }
      }
      else if (actual != null) {
         actual.printStackTrace();
         throw new TestException ("Unexpected exception " + actual.toString());
      }
   }

   private String elementFailMessage (String msg, int i, int j) {
      return (msg + "(" + i + "," + j + ") failed:");
   }

   void checkResult (String msg, VectorNd vec, VectorNd check, double tol) {
       if (!vec.epsilonEquals (check, tol)) {
          VectorNd ve = new VectorNd (vec);
          ve.sub (check);
          if (msg.length() > 0) {
             msg += ": ";
          }
          throw new TestException (
             msg + "expected result:\n" + check.toString ("%10.5f") +
             "\nactual result:\n" + vec.toString ("%10.5f") + "\nmax err: " +
             + ve.infinityNorm() + ", tol=" + tol);
       }
   }

   void checkResult (String msg, Matrix M, Matrix Mchk, double tol) {
      if (!M.epsilonEquals (Mchk, tol)) {
          MatrixNd ME = new MatrixNd (M);
          MatrixNd MC = new MatrixNd (Mchk);
          ME.sub (MC);
          ME.absolute();
          if (msg.length() > 0) {
             msg += ": ";
          }
          NumberFormat fmt = new NumberFormat ("%10.5f");
          throw new TestException (msg + 
             "Expected result:\n" + MC.toString (fmt)
             + "Actual result:\n" + M.toString (fmt) + "\n" + "max err: "
             + ME.maxElement() + ", tol=" + tol);
       }
   }

   void checkResult (
      Matrix MR, Matrix MC, Exception eactual, Exception eexpected,
      double epsilon) {
      checkExceptions (eactual, eexpected);
      if (eexpected != null) {
         return;
      }
      double tol = 0;
      if (epsilon != 0) {
         tol = MR.frobeniusNorm() * epsilon;
      }
      MR.checkConsistency();
      checkResult ("", MR, MC, tol);
   }

   void checkVectorResult (VectorNd vr, double epsilon) {

      checkExceptions (eActual, eExpected);
      if (eExpected != null) {
         eActual = null;
         return;
      }
      double tol = 0;
      if (epsilon != 0) {
         tol = vx.norm()*epsilon;
      }
      checkResult ("", vr, vx, tol);
      eActual = null;
   }

   protected void saveResult (Matrix MR) {
      eActual = null;
      if (MR instanceof Clonable) {
         try {
            MRsave = (Matrix)((Clonable)MR).clone();
            return;
         }
         catch (CloneNotSupportedException e) {
            // will fall through and will catch the other way
         }
      }
      MRsave = new MatrixNd (MR);
   }

   protected void checkAndRestoreResult (Matrix MR) {
      checkResult (MR, MX, eActual, eExpected, 0);
      MR.set (MRsave);
      eActual = null;
   }

   protected void restoreResult (Matrix MR) {
      MR.set (MRsave);
      eActual = null;
   }

   protected void checkAndRestoreResult (Matrix MR, double epsilon) {
      checkResult (MR, MX, eActual, eExpected, epsilon);
      restoreResult (MR);
   }

   void testSetZero (Matrix MR) {
      MRsave.set (MR);
      setZero (MR);
      for (int i = 0; i < MR.rowSize(); i++) {
         for (int j = 0; j < MR.colSize(); j++) {
            if (MR.get (i, j) != 0) {
               throw new TestException (elementFailMessage ("setZero", i, j));
            }
         }
      }
      MR.checkConsistency();
      MR.set (MRsave);
   }

   void testSetDiagonal (Matrix MR, double[] diagValues) {
      MRsave.set (MR);
      setDiagonal (MR, diagValues);
      for (int i = 0; i < MR.rowSize(); i++) {
         for (int j = 0; j < MR.colSize(); j++) {
            if (MR.get (i, j) != (i == j ? diagValues[i] : 0)) {
               throw new TestException (elementFailMessage ("setZero", i, j));
            }
         }
      }
      MR.checkConsistency();
      MR.set (MRsave);
   }

   void testSetIdentity (Matrix MR) {
      MRsave.set (MR);
      setIdentity (MR);
      for (int i = 0; i < MR.rowSize(); i++) {
         for (int j = 0; j < MR.colSize(); j++) {
            if (MR.get (i, j) != (i == j ? 1 : 0)) {
               throw new TestException (
                  elementFailMessage ("setIdentity", i, j));
            }
         }
      }
      MR.checkConsistency();
      MR.set (MRsave);
   }

   protected MatrixNd createRandomValues (Matrix MR, boolean symmetric) {

      MatrixNd MM = new MatrixNd (MR.rowSize(), MR.colSize());

      if (MR instanceof SparseBlockMatrix) {
         ((SparseBlockMatrix)MR).setRandomValues (-0.5, 0.5, randGen, symmetric);
         MM.set (MR);
      }
      else {
         MM.setRandom (-0.5, 0.5, randGen);

         for (int i=0; i<MR.rowSize(); i++) {
            int jbegin = symmetric ? i : 0;
            for (int j=jbegin; j<MR.colSize(); j++) {
               if ((randGen.nextInt() % 2) == 0) {
                  MM.set (i, j, 0);
               }
               if (j > i && symmetric) {
                  MM.set (j, i, MM.get (i, j));
               }
            }
         }
         for (int i=0; i<MR.rowSize(); i++) {
            for (int j=0; j<MR.colSize(); j++) {
               if (isReadOnly (i, j)) {
                  MM.set (i, j, MR.get (i, j));
               }
            }
         }
      }
      return MM;
   }

   protected SparseMatrixCell[] extractCells (Matrix MR) {
      LinkedList<SparseMatrixCell> cellList = new LinkedList<SparseMatrixCell>();
      for (int i=0; i<MR.rowSize(); i++) {
         for (int j=0; j<MR.colSize(); j++) {
            double value = MR.get (i, j);
            if (value != 0) {
               cellList.add (new SparseMatrixCell (i, j, value));
            }
         }
      }
      return cellList.toArray (new SparseMatrixCell[0]);
   }

   protected int[] extractIndices (SparseMatrixCell[] cells) {
      int[] indices = new int[2*cells.length];
      for (int k=0; k<cells.length; k++) {
         indices[k*2  ] = cells[k].i;
         indices[k*2+1] = cells[k].j;
      }
      return indices;         
   }

   protected double[] extractValues (SparseMatrixCell[] cells) {
      double[] values = new double[cells.length];
      for (int k=0; k<cells.length; k++) {
         values[k] = cells[k].value;
      }
      return values;
   }

   void checkResult (Matrix MR, Matrix MC, String msg) {
      if (!MR.equals (MC)) {
         NumberFormat fmt = new NumberFormat("%9.6f");
         System.out.println ("Expected\n" + MC.toString (fmt));
         System.out.println ("Got\n" + MR.toString(fmt));
         MatrixNd C = new MatrixNd (MC);
         MatrixNd R = new MatrixNd (MR);
         R.sub (C);
         System.out.println ("Diff\n" + R.toString(fmt));
         throw new TestException (msg + " failed");
      }
   }

   MatrixNd reduce (Matrix MC, int nrows, int ncols, Partition part) {
      MatrixNd MT = new MatrixNd (MC);
      for (int i=0; i<MC.rowSize(); i++) {
         for (int j=0; j<MC.colSize(); j++) {
            if (i >= nrows || j >= ncols ||
                (part == Partition.UpperTriangular && i >= ncols) ||
                (part == Partition.LowerTriangular && j >= nrows)) {
               MT.set (i, j, 0);
            }
            else if ((part == Partition.UpperTriangular && j > i) ||
                     (part == Partition.LowerTriangular && i > j)) {
               if (j < nrows && i < ncols) {
                  MT.set (j, i, MC.get (i, j));
               }
            }
         }
      }
      return MT;
   }    

   boolean hasReadonly (Matrix MR) {
      for (int i=0; i<MR.rowSize(); i++) {
         for (int j=0; j<MR.colSize(); j++) {
            if (isReadOnly (i, j)) {
               return true;
            }
         }
      }
      return false;
   }

   void checkNNZValues (int nnz, int nnv) {
   }      

   void testCRS (
      Matrix MR, double[] values, int[] indices, int nrows, int ncols,
      Partition part) {

      int nvals = MR.numNonZeroVals();
      int[] rowOffs = new int[MR.rowSize()+1];
      int[] cols = new int[nvals];
      double[] vals = new double[nvals];

      if (nrows <= 0 || ncols <= 0) {
         return;
      }
      
      String msg = ("setCRS test, "+MR.rowSize()+"x"+MR.colSize()+
                    " matrix, "+part);
      if (MR.rowSize() != nrows || MR.colSize() != ncols) {
         msg += " (reduced to "+nrows+"x"+ncols+")";
      }

      MatrixNd MC = new MatrixNd(MR);
      MC.set (values, indices, values.length);
      MC = reduce (MC, nrows, ncols, part);
      int nnz = MR.numNonZeroVals (part, nrows, ncols);
      int nni = MR.getCRSIndices (cols, rowOffs, part, nrows, ncols);
      int nnv = MR.getCRSValues (vals, part, nrows, ncols);
      if (nnz != nni) {
         System.out.println (MR);
         throw new TestException (
            "Nonzeros "+nni+" from getIndices != nonzeros "+nnz+
            " from numNonZeroVals, "+nrows+"x"+ncols+" case");
      }
      if (nnz != nnv) {
         throw new TestException (
            "Nonzeros "+nnv+" from getIndices != nonzeros "+nnz+
            " from numNonZeroVals, "+nrows+"x"+ncols+" case");
      }
      if (hasReadonly(MR) &&
          (nrows!=MR.rowSize() || ncols!=MR.colSize() || part!=Partition.Full)) {
         // can't set MR to read back values, so use a dummy
         MatrixNd MX = new MatrixNd(MR);
         MX.setCRSValues (vals, cols, rowOffs, nnz, nrows, part);
         checkResult (MX, MC, msg);
      }
      else {
         MR.setCRSValues (vals, cols, rowOffs, nnz, nrows, part);
         MR.checkConsistency();
         checkResult (MR, MC, msg);
      }
      MR.set (values, indices, values.length);
   }

   void testCCS (
      Matrix MR, double[] values, int[] indices, int nrows, int ncols,
      Partition part) {

      int nvals = MR.numNonZeroVals();
      int[] colOffs = new int[MR.colSize()+1];
      int[] rows = new int[nvals];
      double[] vals = new double[nvals];

      if (nrows <= 0 || ncols <= 0) {
         return;
      }
      
      String msg = ("setCCS test, "+MR.rowSize()+"x"+MR.colSize()+
                    " matrix, "+part);
      if (MR.rowSize() != nrows || MR.colSize() != ncols) {
         msg += " (reduced to "+nrows+"x"+ncols+")";
      }

      MatrixNd MC = new MatrixNd(MR);
      MC.set (values, indices, values.length);
      MC = reduce (MC, nrows, ncols, part);

      int nnz = MR.numNonZeroVals (part, nrows, ncols);
      int nni = MR.getCCSIndices (rows, colOffs, part, nrows, ncols);
      int nnv = MR.getCCSValues (vals, part, nrows, ncols);
      if (nnz != nni) {
         throw new TestException (
            "Nonzeros "+nni+" from getIndices != nonzeros "+nnz+
            " from numNonZeroVals, "+nrows+"x"+ncols+" case");
      }
      if (nnz != nnv) {
         throw new TestException (
            "Nonzeros "+nnv+" from getIndices != nonzeros "+nnz+
            " from numNonZeroVals, "+nrows+"x"+ncols+" case");
      }
      if (hasReadonly(MR) &&
          (nrows!=MR.rowSize() || ncols!=MR.colSize() || part!=Partition.Full)) {
         // can't set MR to read back values, so use a dummy
         MatrixNd MX = new MatrixNd(MR);
         MX.setCCSValues (vals, rows, colOffs, nnz, ncols, part);
         checkResult (MX, MC, msg);
      }
      else {
         MR.setCCSValues (vals, rows, colOffs, nnz, ncols, part);
         MR.checkConsistency();
         checkResult (MR, MC, msg);
      }
      MR.set (values, indices, values.length);
   }

   void testMulVecBlk (
      SparseBlockMatrix M, int rb0, int nbr, int cb0, int nbc) {
               
      int r0 = M.getBlockRowOffset (rb0);
      int nr = M.getBlockRowOffset (rb0+nbr) - r0;
      int c0 = M.getBlockColOffset (cb0);
      int nc = M.getBlockColOffset (cb0+nbc) - c0;


      testMulVec (M, r0, nr, c0, nc);
      testMulTransposeVec (M, r0, nr, c0, nc);
   }

   Exception getMulErr (
      Matrix M, VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {

      int rowf = r0+nr;
      int colf = c0+nc;

      if (v1.size() < nc) {
         return new ImproperSizeException (
            "v1 size "+v1.size()+" < column size "+nc);
      }
      if (vr != null && vr.size() < nr) {
         return new ImproperSizeException (
            "vr size "+vr.size()+" < row size "+nr);
      }
      if (colf > M.colSize() || rowf > M.rowSize()) {
         return new ImproperSizeException (
            "Specified submatrix "+submatrixStr(r0,nr,c0,nc)+
            " incompatible with "+M.rowSize()+"x"+M.colSize()+" matrix");
      }    
      return null;
   }

   Exception getMulTransposeErr (
      Matrix M, VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {

      int rowf = r0+nr;
      int colf = c0+nc;

      if (v1.size() < nc) {
         return new ImproperSizeException (
            "v1 size "+v1.size()+" < column size "+nc);
      }
      if (vr != null && vr.size() < nr) {
         return new ImproperSizeException (
            "vr size "+vr.size()+" < row size "+nr);
      }
      if (colf > M.rowSize() || rowf > M.colSize()) {
         return new ImproperSizeException (
            "Specified submatrix "+submatrixStr(r0,nr,c0,nc)+
            " incompatible with transpose of "+M.rowSize()+"x"+M.colSize()+
            " matrix");
      }    
      return null;
   }

   void checkMulErr (Matrix M, VectorNd vr, VectorNd v1) {
      Exception err = null;
      try {
         M.mul (vr, v1);
      }
      catch (Exception e) {
         err = e;
      }
      checkExceptions (
         err, getMulErr (M, null, v1, 0, M.rowSize(), 0, M.colSize()));
   }

   void checkMulErr (
      Matrix M, VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {
      Exception err = null;
      try {
         M.mul (vr, v1, r0, nr, c0, nc);
      }
      catch (Exception e) {
         err = e;
      }
      checkExceptions (err, getMulErr (M, null, v1, r0, nr, c0, nc));
   }

   void checkMulAddErr (Matrix M, VectorNd vr, VectorNd v1) {
      Exception err = null;
      try {
         M.mul (vr, v1);
      }
      catch (Exception e) {
         err = e;
      }
      checkExceptions (
         err, getMulErr (M, vr, v1, 0, M.rowSize(), 0, M.colSize()));
   }

   void checkMulAddErr (
      Matrix M, VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {
      Exception err = null;
      try {
         M.mul (vr, v1, r0, nr, c0, nc);
      }
      catch (Exception e) {
         err = e;
      }
      checkExceptions (
         err, getMulErr (M, vr, v1, r0, nr, c0, nc));
   }

   void checkMulTransposeErr (Matrix M, VectorNd vr, VectorNd v1) {
      Exception err = null;
      try {
         M.mulTranspose (vr, v1);
      }
      catch (Exception e) {
         err = e;
      }
      checkExceptions (
         err, getMulTransposeErr (M, null, v1, 0, M.colSize(), 0, M.rowSize()));
   }

   void checkMulTransposeErr (
      Matrix M, VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {
      Exception err = null;
      try {
         M.mulTranspose (vr, v1, r0, nr, c0, nc);
      }
      catch (Exception e) {
         err = e;
      }
      checkExceptions (err, getMulTransposeErr (M, null, v1, r0, nr, c0, nc));
   }

   void checkMulTransposeAddErr (Matrix M, VectorNd vr, VectorNd v1) {
      Exception err = null;
      try {
         M.mulTransposeAdd (vr, v1);
      }
      catch (Exception e) {
         err = e;
      }
      checkExceptions (
         err, getMulTransposeErr (M, vr, v1, 0, M.colSize(), 0, M.rowSize()));
   }

   void checkMulTransposeAddErr (
      Matrix M, VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {
      Exception err = null;
      try {
         M.mulTransposeAdd (vr, v1, r0, nr, c0, nc);
      }
      catch (Exception e) {
         err = e;
      }
      checkExceptions (err, getMulTransposeErr (M, vr, v1, r0, nr, c0, nc));
   }

   void testMulVecReg (
      Matrix M, int r0, int nr, int c0, int nc) {

      testMulVec (M, r0, nr, c0, nc);
      testMulTransposeVec (M, r0, nr, c0, nc);
   }

   void testMulVec (
      Matrix M, int r0, int nr, int c0, int nc) {

      VectorNd y = new VectorNd (nr);
      VectorNd ys = new VectorNd (nr-1);
      VectorNd yx = new VectorNd (nr+2);
      VectorNd ysave = new VectorNd (nr);
      VectorNd ycheck = new VectorNd (nr);
      VectorNd ycheckx = new VectorNd (nr+2);
      VectorNd x = new VectorNd (nc);
      VectorNd xx = new VectorNd (nc+2);
      VectorNd xs = new VectorNd (nc-1);
      x.setRandom();
      xx.setSubVector (0, x);

      for (int i=r0; i<r0+nr; i++) {
         double sum = 0;
         for (int j=c0; j<c0+nc; j++) {
            sum += M.get(i,j)*x.get (j-c0);
         }
         ycheck.set (i-r0, sum);
      }
      ysave.set (ycheck);
      ycheckx.setSubVector (0, ycheck);
      if (r0 == 0 && c0 == 0) {
         M.mul (y, x, nr, nc);
         checkResult ("mul", y, ycheck, 1e-10);
         y.setSize(nr-1);
         M.mul (y, x, nr, nc);
         checkResult ("mul", y, ycheck, 1e-10);
         if (nr == nc) {
            y.set (x);
            M.mul (y, y, nr, nc);
            checkResult ("mul", y, ycheck, 1e-10);
         }
         M.mul (yx, xx, nr, nc);
         checkResult ("mul", yx, ycheckx, 1e-10);
         if (nr == M.rowSize() && nc == M.colSize()) {
            M.mul (y, x);
            checkResult ("mul", y, ycheck, 1e-10);
            y.setSize (nr-1);
            M.mul (y, x);
            checkResult ("mul", y, ycheck, 1e-10);
            if (nr == nc) {
               y.set (x);
               M.mul (y, y);
               checkResult ("mul", y, ycheck, 1e-10);
            }
            M.mul (yx, xx);
            checkResult ("mul", yx, ycheckx, 1e-10);
         }
      }
      M.mul (y, x, r0, nr, c0, nc);
      checkResult ("mul", y, ycheck, 1e-10);
      y.setSize (nr-1);
      M.mul (y, x, r0, nr, c0, nc);
      checkResult ("mul", y, ycheck, 1e-10);
      if (nr == nc) {
         y.set (x);
         M.mul (y, y, r0, nr, c0, nc);
         checkResult ("mul", y, ycheck, 1e-10);
      }
      M.mul (yx, xx, r0, nr, c0, nc);
      checkResult ("mul", yx, ycheckx, 1e-10);
      ycheck.add (ycheck);
      ycheckx.setSubVector (0, ycheck);
      
      if (r0 == 0 && c0 == 0) {
         y.set (ysave);
         M.mulAdd (y, x, nr, nc);
         checkResult ("mulAdd", y, ycheck, 1e-10);
         y.setSubVector (0, ysave);
         M.mulAdd (yx, xx, nr, nc);
         checkResult ("mulAdd", yx, ycheckx, 1e-10);
         if (nr == M.rowSize() && nc == M.colSize()) {
            y.set (ysave);
            M.mulAdd (y, x);
            checkResult ("mulAdd", y, ycheck, 1e-10);
            yx.setSubVector (0, ysave);
            M.mulAdd (yx, xx);
            checkResult ("mulAdd", yx, ycheckx, 1e-10);
         }
      }
      y.set (ysave);
      M.mulAdd (y, x, r0, nr, c0, nc);
      checkResult ("mulAdd", y, ycheck, 1e-10);      
      yx.setSubVector (0, ysave);
      M.mulAdd (yx, xx, r0, nr, c0, nc);
      checkResult ("mulAdd", yx, ycheckx, 1e-10);

      // now test errors
      int nrx = M.rowSize() - r0 + 1;
      int ncx = M.colSize() - c0 + 1;
      VectorNd ymax = new VectorNd (nrx);
      VectorNd xmax = new VectorNd (ncx);
      checkMulErr (M, y, xs);
      checkMulErr (M, ymax, x, r0, nrx, c0, nc);
      checkMulErr (M, y, xmax, r0, nr, c0, ncx);
      checkMulAddErr (M, y, xs);
      checkMulAddErr (M, ys, x);
      checkMulAddErr (M, ymax, x, r0, nrx, c0, nc);
      checkMulAddErr (M, y, xmax, r0, nr, c0, ncx);
   }

   void testMulTransposeVec (
      Matrix M, int r0, int nr, int c0, int nc) {

      VectorNd y = new VectorNd (nr);
      VectorNd ys = new VectorNd (nr-1);
      VectorNd yx = new VectorNd (nr+2);
      VectorNd ysave = new VectorNd (nr);
      VectorNd ycheck = new VectorNd (nr);
      VectorNd ycheckx = new VectorNd (nr+2);
      VectorNd x = new VectorNd (nc);
      VectorNd xx = new VectorNd (nc+2);
      VectorNd xs = new VectorNd (nc-1);
      x.setRandom();
      xx.setSubVector (0, x);

      for (int i=r0; i<r0+nr; i++) {
         double sum = 0;
         for (int j=c0; j<c0+nc; j++) {
            sum += M.get(j,i)*x.get(j-c0);
         }
         ycheck.set (i-r0, sum);
      }
      ysave.set (ycheck);
      ycheckx.setSubVector (0, ycheck);

      if (r0 == 0 && c0 ==0) {
         M.mulTranspose (y, x, nr, nc);
         checkResult ("mulTranspose", y, ycheck, 1e-10);
         y.setSize (nr-1);
         M.mulTranspose (y, x, nr, nc);
         checkResult ("mulTranspose", y, ycheck, 1e-10);
         if (nr == nc) {
            y.set (x);
            M.mulTranspose (y, y, nr, nc);
            checkResult ("mulTranspose", y, ycheck, 1e-10);
         }
         M.mulTranspose (yx, xx, nr, nc);
         checkResult ("mulTranspose", yx, ycheckx, 1e-10);
         if (nr == M.rowSize() && nc == M.colSize()) {
            M.mulTranspose (y, x);
            checkResult ("mulTranspose", y, ycheck, 1e-10);
            y.setSize (nr-1);            
            M.mulTranspose (y, x);
            checkResult ("mulTranspose", y, ycheck, 1e-10);
            if (nr == nc) {
               y.set (x);
               M.mulTranspose (y, y);
               checkResult ("mulTranspose", y, ycheck, 1e-10);
            }
            M.mulTranspose (yx, xx);
            checkResult ("mulTranspose", yx, ycheckx, 1e-10);
         }
      }
      M.mulTranspose (y, x, r0, nr, c0, nc);
      checkResult ("mulTranspose", y, ycheck, 1e-10);
      y.setSize (nr-1);
      M.mulTranspose (y, x, r0, nr, c0, nc);
      checkResult ("mulTranspose", y, ycheck, 1e-10);
      if (nr == nc) {
         y.set (x);
         M.mulTranspose (y, y, r0, nr, c0, nc);
         checkResult ("mulTranspose", y, ycheck, 1e-10);
      }
      M.mulTranspose (yx, xx, r0, nr, c0, nc);
      checkResult ("mulTranspose", yx, ycheckx, 1e-10);

      ycheck.add (ycheck);
      ycheckx.setSubVector (0, ycheck);

      if (r0 == 0 && c0 == 0) {
         y.set (ysave);
         M.mulTransposeAdd (y, x, nr, nc);
         checkResult ("mulTransposeAdd", y, ycheck, 1e-10);
         y.setSubVector (0, ysave);
         M.mulTransposeAdd (yx, xx, nr, nc);
         checkResult ("mulTransposeAdd", yx, ycheckx, 1e-10);
         if (nr == M.rowSize() && nc == M.colSize()) {
            y.set (ysave);
            M.mulTransposeAdd (y, x);
            checkResult ("mulTransposeAdd", y, ycheck, 1e-10);
            yx.setSubVector (0, ysave);
            M.mulTransposeAdd (yx, xx);
            checkResult ("mulTransposeAdd", yx, ycheckx, 1e-10);
         }
      }
      y.set (ysave);
      M.mulTransposeAdd (y, x, r0, nr, c0, nc);
      checkResult ("mulTransposeAdd", y, ycheck, 1e-10);      
      yx.setSubVector (0, ysave);
      M.mulTransposeAdd (yx, xx, r0, nr, c0, nc);
      checkResult ("mulTransposeAdd", yx, ycheckx, 1e-10);      

      // now test errors
      int nrx = M.colSize() - r0 + 1;
      int ncx = M.rowSize() - c0 + 1;
      VectorNd ymax = new VectorNd (nrx);
      VectorNd xmax = new VectorNd (ncx);
      checkMulTransposeErr (M, y, xs);
      checkMulTransposeErr (M, ymax, x, r0, nrx, c0, nc);
      checkMulTransposeErr (M, y, xmax, r0, nr, c0, ncx);
      checkMulTransposeAddErr (M, y, xs);
      checkMulTransposeAddErr (M, ys, x);
      checkMulTransposeAddErr (M, ymax, x, r0, nrx, c0, nc);
      checkMulTransposeAddErr (M, y, xmax, r0, nr, c0, ncx);

   }

   // private void testMulTransposeBlk (
   //    SparseBlockMatrix M, int rb0, int nbr, int cb0, int nbc) {
               
   //    int r0 = M.getBlockColOffset (rb0);
   //    int nr = M.getBlockColOffset (rb0+nbr) - r0;
   //    int c0 = M.getBlockRowOffset (cb0);
   //    int nc = M.getBlockRowOffset (cb0+nbc) - c0;

   //    testMulTransposeVec (M, r0, nr, c0, nc);
   // }

   // private void testMulTransposeVec (
   //    Matrix M, int r0, int nr, int c0, int nc) {

   //    VectorNd y = new VectorNd (nr);
   //    VectorNd ycheck = new VectorNd (nr);
   //    VectorNd ysave = new VectorNd (nr);
   //    VectorNd x = new VectorNd (nc);
   //    x.setRandom();

   //    for (int i=r0; i<r0+nr; i++) {
   //       double sum = 0;
   //       for (int j=c0; j<c0+nc; j++) {
   //          sum += M.get(j,i)*x.get(j-c0);
   //       }
   //       ycheck.set (i-r0, sum);
   //    }

   //    if (r0 == 0 && c0 ==0) {
   //       M.mulSubMatrixTranspose (y, x, nr, nc);
   //       checkResult ("mulTranspose", y, ycheck, 1e-10);
   //    }
   //    M.mulSubMatrixTranspose (y, x, r0, nr, c0, nc);
   //    checkResult ("mulTranspose", y, ycheck, 1e-10);
   // }

   void testMulVector (Matrix MR, int vrlen, int v1len, int nr, int nc) {

      VectorNd v1, vr;

      v1 = new VectorNd (v1len);
      v1.setRandom (-0.5, 0.5, randGen);
      vr = new VectorNd (vrlen);

      eExpected = mulVectorCheck (vx, MR, vr, v1, 0, nr, 0, nc);
      try {
         MR.mul (vr, v1, nr, nc);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkVectorResult (vr, EPSILON);

      eExpected = mulVectorCheck (vx, MR, v1, v1, 0, nr, 0, nc);
      try {
         MR.mul (v1, v1, nr, nc);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkVectorResult (v1, EPSILON);
      
      if (nr == MR.rowSize() && nc == MR.colSize()) {
         eExpected = mulVectorCheck (vx, MR, vr, v1);
         try {
            MR.mul (vr, v1);
         }
         catch (Exception e) {
            eActual = e;
         }
         checkVectorResult (vr, EPSILON);
      }

      v1 = new VectorNd (v1len);
      v1.setRandom (-0.5, 0.5, randGen);
      vr = new VectorNd (vrlen);
      vr.setRandom (-0.5, 0.5, randGen);

      eExpected = mulAddVectorCheck (vx, MR, vr, v1, 0, nr, 0, nc);
      try {
         MR.mulAdd (vr, v1, nr, nc);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkVectorResult (vr, EPSILON);
      
      eExpected = mulAddVectorCheck (vx, MR, v1, v1, 0, nr, 0, nc);
      try {
         MR.mulAdd (v1, v1, nr, nc);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkVectorResult (v1, EPSILON);
   }

   void testMulTransposeVector (Matrix MR, int vrlen, int v1len, int nr, int nc) {

      VectorNd v1, vr;

      v1 = new VectorNd (v1len);
      v1.setRandom (-0.5, 0.5, randGen);
      vr = new VectorNd (vrlen);

      eExpected = mulTransposeVectorCheck (vx, MR, vr, v1, 0, nr, 0, nc);
      try {
         MR.mulTranspose (vr, v1, nr, nc);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkVectorResult (vr, EPSILON);

      eExpected = mulTransposeVectorCheck (vx, MR, v1, v1, 0, nr, 0, nc);
      try {
         MR.mulTranspose (v1, v1, nr, nc);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkVectorResult (v1, EPSILON);

      if (nr == MR.colSize() && nc == MR.rowSize()) {
         eExpected = mulTransposeVectorCheck (vx, MR, vr, v1);
         try {
            MR.mulTranspose (vr, v1);
         }
         catch (Exception e) {
            eActual = e;
         }
         checkVectorResult (vr, EPSILON);
      }
   }

   protected void jumbleCells (SparseMatrixCell[] cells) {
      for (int i=0; i<cells.length; i++) {
         int k = randGen.nextInt (cells.length);
         int l = randGen.nextInt (cells.length);
         if (k != l) {
            SparseMatrixCell tmp = cells[k];
            cells[k] = cells[l];
            cells[l] = tmp;
         }
      }
   }

   void testMulVec (Matrix M) {
      int nrows = M.rowSize();
      int ncols = M.colSize();
      int maxrc = Math.min(nrows, ncols);
      for (int n = 1; n <= maxrc; n++) {
         testMulVecReg (M, 0, n, 0, n);
         testMulVecReg (M, 0, n, 0, maxrc);
         testMulVecReg (M, 0, maxrc, 0, n);
         if (n < maxrc) {
            testMulVecReg (M, 1, n, 0, n);            
            testMulVecReg (M, 0, n, 1, n);            
            testMulVecReg (M, 1, n, 1, n);            
         }
         if (n < maxrc-1) {
            testMulVecReg (M, 2, n, 0, n);            
            testMulVecReg (M, 2, n, 1, n);            
            testMulVecReg (M, 1, n, 2, n);            
            testMulVecReg (M, 0, n, 2, n);            
         }
      }      
   }

   void testMulVecBlk (SparseBlockMatrix M) {
      int maxb = Math.min (M.numBlockRows(), M.numBlockCols());
      for (int nb = 1; nb <= maxb; nb++) {
         testMulVecBlk (M, 0, nb, 0, nb);
         testMulVecBlk (M, 0, nb, 0, maxb);
         testMulVecBlk (M, 0, maxb, 0, nb);
         if (nb < maxb) {
            testMulVecBlk (M, 1, nb, 0, nb);            
            testMulVecBlk (M, 0, nb, 1, nb);            
            testMulVecBlk (M, 1, nb, 1, nb);            
         }
         if (nb < maxb-1) {
            testMulVecBlk (M, 2, nb, 0, nb);            
            testMulVecBlk (M, 2, nb, 1, nb);            
            testMulVecBlk (M, 1, nb, 2, nb);            
            testMulVecBlk (M, 0, nb, 2, nb);            
         }
      }
   }

   void testEquals (DenseMatrixBase M1, DenseMatrixBase M2) {
      MatrixNd M1save = new MatrixNd (M1);
      MatrixNd M2save = new MatrixNd (M2);

      M1.setRandom();

      double EPS = 1e-14;
      if (!equals (M1, M1)) {
         throw new TestException ("matrix not equal to itself");
      }
      if (!epsilonEquals (M1, M1, 0)) {
         throw new TestException (
            "matrix not epsilon equal to itself with EPS = 0");
      }
      M2.setRandom();
      for (int i=0; i<M2.rowSize(); i++) {
         for (int j=0; j<M2.colSize(); j++) {
            M2.set (i, j, M1.get(i,j)+EPS*M2.get(i,j));
         }
      }
      if (!epsilonEquals (M1, M2, EPS)) {
         throw new TestException (
            "matrix not epsilon equal to small perturbation");
      }
      if (epsilonEquals (M1, M2, 0.0001*EPS)) {
         throw new TestException (
            "matrix epsilon equal to small perturbation with very small EPS");
      }
      M2.set (M1);
      // set random entry to NaN
      int i = RandomGenerator.nextInt (0, M1.rowSize()-1);
      int j = RandomGenerator.nextInt (0, M1.colSize()-1);
      M2.set (i, j, 0.0/0.0);
      if (equals (M1, M2)) {
         throw new TestException ("matrix equal to matrix containing NaN");
      }
      if (epsilonEquals (M1, M2, EPS)) {
         throw new TestException (
            "matrix epsilon equal to matrix containing NaN");
      }      
      M1.set (M1save);
      M2.set (M2save);
   }
         

   void testGeneric (Matrix MR) {

      if (MR instanceof DenseMatrix) {
         testDenseSetAndGet ((DenseMatrix)MR);
      }

      saveResult (MR);

      if (MR instanceof MatrixBase) {
         // test equals and epsilonEquals         
         double EPS = 1e-15;
         MatrixBase MB = (MatrixBase)MR;
         MB.setRandom();

         if (!MB.equals (MB)) {
            throw new TestException ("Matrix not equal to itself");
         }
         if (!MB.epsilonEquals (MB, 0)) {
            throw new TestException (
               "Matrix not epsilonEqual to itself with epsilon 0");
         }
         // create a similar matrix to check epsilonEquals
         MatrixNd MX = new MatrixNd (MB);
         MatrixNd MP = new MatrixNd (MB.rowSize(), MB.colSize());
         MP.setRandom ();
         MX.scaledAdd (EPS, MP);
         if (!MB.epsilonEquals (MX, EPS)) {
            throw new TestException (
               "Matrix not epsilonEqual to itself with small perturbation");
         }
      }

      SparseMatrixCell[] cells;
      int[] indices;
      double[] values;
      
      int nr = MR.rowSize();
      int nc = MR.colSize();

      MatrixNd MC = createRandomValues (MR, /*symmetric=*/false);
      cells = extractCells (MC);
      jumbleCells (cells);

      indices = extractIndices (cells);
      values = extractValues (cells);

      MR.set (values, indices, values.length);
      MR.checkConsistency();
      
      checkResult (MR, MC, "set(vals,indices,nvals)");

      // XXX hack for SparseBlockMatrix
      int rdec = 2;
      int cdec = 2;
      if (MR instanceof SparseBlockMatrix) {
         SparseBlockMatrix BM = (SparseBlockMatrix)MR;
         rdec = BM.getBlockRowSize (BM.getBlockRow (BM.rowSize()-1));
         cdec = BM.getBlockColSize (BM.getBlockCol (BM.colSize()-1));
      }
      
      testCRS (MR, values, indices, nr, nc, Partition.Full);
      testCRS (MR, values, indices, nr-rdec, nc, Partition.Full);
      testCRS (MR, values, indices, nr, nc-cdec, Partition.Full);
      testCRS (MR, values, indices, nr-rdec, nc-cdec, Partition.Full);

      testCRS (MR, values, indices, nr, nc, Partition.UpperTriangular);
      testCRS (MR, values, indices, nr-rdec, nc, Partition.UpperTriangular);
      testCRS (MR, values, indices, nr, nc-cdec, Partition.UpperTriangular);
      testCRS (MR, values, indices, nr-rdec, nc-cdec, Partition.UpperTriangular);

      testCCS (MR, values, indices, nr, nc, Partition.Full);
      testCCS (MR, values, indices, nr-rdec, nc, Partition.Full);
      testCCS (MR, values, indices, nr, nc-cdec, Partition.Full);
      testCCS (MR, values, indices, nr-rdec, nc-cdec, Partition.Full);

      testCCS (MR, values, indices, nr, nc, Partition.LowerTriangular);
      testCCS (MR, values, indices, nr-rdec, nc, Partition.LowerTriangular);
      testCCS (MR, values, indices, nr, nc-cdec, Partition.LowerTriangular);
      testCCS (MR, values, indices, nr-rdec, nc-cdec, Partition.LowerTriangular);

      if (MR instanceof SparseBlockMatrix) {
         testMulVecBlk ((SparseBlockMatrix)MR);
      }
      else {
         testMulVec (MR);
      }

      // testMulVector (MR, nr, nc, nr, nc);
      // testMulVector (MR, nr-rdec, nc, nr-rdec, nc);
      // testMulVector (MR, nr, nc-cdec, nr, nc-cdec);
      // testMulVector (MR, nr-rdec, nc-cdec, nr-rdec, nc-cdec);
      // testMulVector (MR, nr, nc, nr+1, nc);
      // testMulVector (MR, nr, nc, nr, nc+1);
      // testMulVector (MR, nr-1, nc-1, nr, nc);

      // testMulTransposeVector (MR, nc, nr, nc, nr);
      // testMulTransposeVector (MR, nc-cdec, nr, nc-cdec, nr);
      // testMulTransposeVector (MR, nc, nr-rdec, nc, nr-rdec);
      // testMulTransposeVector (MR, nc-cdec, nr-rdec, nc-cdec, nr-rdec);
      // testMulTransposeVector (MR, nc, nr, nc+1, nr);
      // testMulTransposeVector (MR, nc, nr, nc, nr+1);
      // testMulTransposeVector (MR, nc-1, nr-1, nc, nr);

      restoreResult (MR);
   }

   void testDenseSetAndGet (DenseMatrix MR) {
      
      int nrows = MR.rowSize();
      int ncols = MR.colSize();
      Vector vrow = new VectorNd (ncols);
      Vector vcol = new VectorNd (nrows);
      double[] setBuf = new double[nrows * ncols];
      double[] getBuf = new double[nrows * ncols];
      double[] altBuf = new double[nrows * ncols];

      for (int i = 0; i < altBuf.length; i++) {
         altBuf[i] = randGen.nextDouble();
      }
      MR.set (altBuf);
      MR.get (getBuf);
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            if (getBuf[i * ncols + j] != getExpected (
                   altBuf[i * ncols + j], i, j)) {
               throw new TestException (
                  elementFailMessage ("set(double[])", i, j));
            }
         }
      }
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            double value = randGen.nextDouble();
            MR.set (i, j, value);
            if (MR.get (i, j) != getExpected (value, i, j)) {
               System.out.println ("get(i,j)=" + MR.get (i, j));
               System.out.println ("value=" + value);
               System.out.println ("MR=\n"
               + MR.toString (new NumberFormat ("%6.3f")));
               throw new TestException (elementFailMessage ("get/set", i, j));
            }
            setBuf[i * ncols + j] = value;
         }
      }
      MR.get (getBuf);
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            if (getBuf[i * ncols + j] != getExpected (
               setBuf[i * ncols + j], i, j)) {
               throw new TestException (elementFailMessage ("set", i, j));
            }
         }
      }
      MR.set (altBuf);
      for (int i = 0; i < nrows; i++) {
         double[] buf = new double[ncols];
         for (int j = 0; j < ncols; j++) {
            buf[j] = setBuf[i * ncols + j];
         }
         MR.setRow (i, buf);
         MR.getRow (i, buf);
         for (int j = 0; j < ncols; j++) {
            if (buf[j] != getExpected (setBuf[i * ncols + j], i, j)) {
               throw new TestException ("set/getRow(" + i + ",double[]) failed");
            }
         }
         MR.getRow (i, buf, 0);
         for (int j = 0; j < ncols; j++) {
            if (buf[j] != getExpected (setBuf[i * ncols + j], i, j)) {
               throw new TestException ("getRow(" + i + ",double[],off) failed");
            }
         }
      }
      MX.set (MR);
      if (!MR.equals (MX)) {
         throw new TestException ("setRow(*,double[]) failed");
      }
      MR.set (altBuf);
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            vrow.set (j, setBuf[i * ncols + j]);
         }
         MR.setRow (i, vrow);
         MR.getRow (i, vrow);
         for (int j = 0; j < ncols; j++) {
            if (vrow.get (j) != getExpected (setBuf[i * ncols + j], i, j)) {
               throw new TestException ("set/getRow(" + i + ",Vector) failed");
            }
         }
      }
      if (!MR.equals (MX)) {
         throw new TestException ("setRow(*,Vector) failed");
      }
      MR.set (altBuf);
      for (int j = 0; j < ncols; j++) {
         double[] buf = new double[nrows];
         for (int i = 0; i < nrows; i++) {
            buf[i] = setBuf[i * ncols + j];
         }
         MR.setColumn (j, buf);
         MR.getColumn (j, buf);
         for (int i = 0; i < nrows; i++) {
            if (buf[i] != getExpected (setBuf[i * ncols + j], i, j)) {
               throw new TestException ("set/getColumn(" + j
               + ",double[]) failed");
            }
         }
         MR.getColumn (j, buf, 0);
         for (int i = 0; i < nrows; i++) {
            if (buf[i] != getExpected (setBuf[i * ncols + j], i, j)) {
               throw new TestException ("getColumn(" + j
               + ",double[],off) failed");
            }
         }
      }
      if (!MR.equals (MX)) {
         throw new TestException ("setColumn(*,double[]) failed");
      }
      MR.set (altBuf);
      for (int j = 0; j < ncols; j++) {
         for (int i = 0; i < nrows; i++) {
            vcol.set (i, setBuf[i * ncols + j]);
         }
         MR.setColumn (j, vcol);
         MR.getColumn (j, vcol);
         for (int i = 0; i < nrows; i++) {
            if (vcol.get (i) != getExpected (setBuf[i * ncols + j], i, j)) {
               throw new TestException ("set/getColumn(" + j
               + ",Vector) failed");
            }
         }
      }
      if (!MR.equals (MX)) {
         throw new TestException ("setColumn(*,Vector) failed");
      }
      MR.set (MX);
      MR.get (getBuf);
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            if (getBuf[i * ncols + j] != getExpected (
               setBuf[i * ncols + j], i, j)) {
               throw new TestException ("set(Matrix) failed");
            }
         }
      }

   }

   void testGet (Matrix MR) {

      int nrows = MR.rowSize();
      int ncols = MR.colSize();
      Vector vrow = new VectorNd (ncols);
      Vector vcol = new VectorNd (nrows);
      double[] getBuf = new double[nrows * ncols];

      MR.get (getBuf);
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            if (MR.get (i, j) != getBuf[i*ncols+j]) {
               System.out.println ("get(i,j)=" + MR.get (i, j));
               System.out.println ("get(buf) value=" + getBuf[i*nrows+j]);
               throw new TestException (elementFailMessage ("get", i, j));
            }
         }
      }
      for (int i = 0; i < nrows; i++) {
         double[] buf = new double[ncols];
         MR.getRow (i, buf);
         for (int j = 0; j < ncols; j++) {
            if (buf[j] != getExpected (getBuf[i*ncols+j], i, j)) {
               throw new TestException ("getRow("+i+",double[]) failed");
            }
         }
         MR.getRow (i, buf, 0);
         for (int j = 0; j < ncols; j++) {
            if (buf[j] != getExpected (getBuf[i*ncols+j], i, j)) {
               throw new TestException ("getRow("+i+",double[],off) failed");
            }
         }
         MR.getRow (i, vrow);
         for (int j = 0; j < ncols; j++) {
            if (vrow.get (j) != getExpected (getBuf[i*ncols+j], i, j)) {
               throw new TestException ("getRow(" + i + ",Vector) failed");
            }
         }
      }
      for (int j = 0; j < ncols; j++) {
         double[] buf = new double[nrows];
         MR.getColumn (j, buf);
         for (int i = 0; i < nrows; i++) {
            if (buf[i] != getExpected (getBuf[i*ncols+j], i, j)) {
               throw new TestException ("getColumn(" + j + ",double[]) failed");
            }
         }
         MR.getColumn (j, buf, 0);
         for (int i = 0; i < nrows; i++) {
            if (buf[i] != getExpected (getBuf[i*ncols+j], i, j)) {
               throw new TestException ("getColumn(" +j+",double[],off) failed");
            }
         }
         MR.getColumn (j, vcol);
         for (int i = 0; i < nrows; i++) {
            if (vcol.get (i) != getExpected (getBuf[i*ncols+j], i, j)) {
               throw new TestException ("getColumn(" + j + ",Vector) failed");
            }
         }
      }
   }

   void testScan (Matrix MR, String input, Matrix M1) {

      saveResult (MR);
      eExpected = null;
      MX.set (M1);
      try {
         MR.scan (new ReaderTokenizer (new StringReader (input)));
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
      // now try with input we get from the matrix itself
      try {
         MR.scan (new ReaderTokenizer (new StringReader ("[" + M1.toString()
         + "]")));
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   void testScan (Matrix MR, String input, Exception ee) {
      saveResult (MR);
      eExpected = ee;
      MX.set (MR);
      try {
         MR.scan (new ReaderTokenizer (new StringReader (input)));
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   void testAdd (Matrix MR, Matrix M1, Matrix M2) {
      saveResult (MR);
      eExpected = addCheck (MX, MR, M1, M2);
      try {
         add (MR, M1, M2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
      eExpected = addCheck (MX, MR, MR, M1);
      try {
         add (MR, M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   void testScaledAdd (Matrix MR, double s, Matrix M1, Matrix M2) {
      saveResult (MR);
      eExpected = scaledAddCheck (MX, MR, s, M1, M2);
      try {
         scaledAdd (MR, s, M1, M2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
      eExpected = scaledAddCheck (MX, MR, s, M1, MR);
      try {
         scaledAdd (MR, s, M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   void testSub (Matrix MR, Matrix M1, Matrix M2) {
      saveResult (MR);
      eExpected = subCheck (MX, MR, M1, M2);
      try {
         sub (MR, M1, M2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
      eExpected = subCheck (MX, MR, MR, M1);
      try {
         sub (MR, M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   void testMul (Matrix MR, Matrix M1, Matrix M2) {
      saveResult (MR);
      eExpected = mulCheck (MX, MR, M1, M2);
//       if (MR.rowSize() == 20) {
//          System.out.println (((SparseMatrixCRS)MR).getCapacity());
//       }
      try {
         mul (MR, M1, M2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR, mulTol);
      eExpected = mulCheck (MX, MR, MR, M1);
      try {
         mul (MR, M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR, mulTol);
   }

   void testMulTranspose (Matrix MR, Matrix M1, Matrix M2) {
      saveResult (MR);

      eExpected = mulTransposeRightCheck (MX, MR, M1, M2);
      try {
         mulTransposeRight (MR, M1, M2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR, mulTol);

      eExpected = mulTransposeRightCheck (MX, MR, MR, M1);
      try {
         mulTranspose (MR, M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR, mulTol);

      eExpected = mulTransposeLeftCheck (MX, MR, M1, M2);
      try {
         mulTransposeLeft (MR, M1, M2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR, mulTol);

      eExpected = mulTransposeBothCheck (MX, MR, M1, M2);
      try {
         mulTransposeBoth (MR, M1, M2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR, mulTol);
   }

   void testInvert (Matrix MR, Matrix M1) {
      double cond = 1;
      MatrixNd M1inv = new MatrixNd (M1.rowSize(), M1.colSize());
      if (M1.rowSize() == M1.colSize()) {
         LUDecomposition lu = new LUDecomposition (M1);
         lu.inverse (M1inv);
         cond = M1inv.frobeniusNorm() * M1.frobeniusNorm();
      }
      saveResult (MR);
      eExpected = invertCheck (MX, MR, M1, M1inv);
      try {
         invert (MR, M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR, cond * EPSILON);
      MR.set (M1);
      try {
         invert (MR);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR, cond * EPSILON);
   }

   void testNorms (Matrix M1) {
      double expectedNorm = MatrixBase.computeOneNorm (M1);
      if (M1.oneNorm() != expectedNorm) {
         throw new TestException ("oneNorm: expected " + expectedNorm
         + ", got " + M1.oneNorm());
      }
      expectedNorm = MatrixBase.computeInfinityNorm (M1);
      if (M1.infinityNorm() != expectedNorm) {
         throw new TestException ("infinityNorm: expected " + expectedNorm
         + ", got " + M1.infinityNorm());
      }
      expectedNorm = MatrixBase.computeFrobeniusNorm (M1);
      if (Math.abs (M1.frobeniusNorm() - expectedNorm) > EPSILON
      * expectedNorm) {
         throw new TestException ("frobeniusNorm: expected " + expectedNorm
         + ", got " + M1.frobeniusNorm());
      }
   }

   Exception invertCheck (MatrixNd MX, Matrix MR, Matrix M1, Matrix M1inv) {
      if (M1.rowSize() != M1.colSize()) {
         return new ImproperSizeException ("matrix must be square");
      }
      if (MR.rowSize() != M1.rowSize() || MR.colSize() != M1.colSize()) {
         if (MR.isFixedSize()) {
            return new ImproperSizeException ("Incompatible dimensions");
         }
      }
      MX.set (M1inv);
      return null;
   }

   void testMulInverse (Matrix MR, Matrix M1, Matrix M2) {
      saveResult (MR);
      double cond1 = 1;
      double cond2 = 1;
      MatrixNd M1inv = new MatrixNd (M1.rowSize(), M1.colSize());
      if (M1.rowSize() == M1.colSize()) {
         LUDecomposition lu = new LUDecomposition (M1);
         lu.inverse (M1inv);
         cond1 = M1.frobeniusNorm() * M1inv.frobeniusNorm();
      }
      MatrixNd M2inv = new MatrixNd (M2.rowSize(), M2.colSize());
      if (M2.rowSize() == M2.colSize()) {
         LUDecomposition lu = new LUDecomposition (M2);
         lu.inverse (M2inv);
         cond2 = M2.frobeniusNorm() * M2inv.frobeniusNorm();
      }

      eExpected = mulInverseRightCheck (MX, MR, M1, M2, M2inv);
      try {
         mulInverseRight (MR, M1, M2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR, cond2 * EPSILON);

      eExpected = mulInverseRightCheck (MX, MR, MR, M1, M1inv);
      try {
         mulInverse (MR, M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR, cond1 * EPSILON);

      eExpected = mulInverseLeftCheck (MX, MR, M1, M2, M1inv);
      try {
         mulInverseLeft (MR, M1, M2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR, cond1 * EPSILON);

      eExpected = mulInverseBothCheck (MX, MR, M1, M2, M1inv, M2inv);
      try {
         mulInverseBoth (MR, M1, M2);
      }
      catch (Exception e) {
         eActual = e;
      }
      // System.out.println ("cond " + cond1 + " " + cond2);
      checkAndRestoreResult (MR, cond1 * cond2 * EPSILON);
   }

   void testTranspose (Matrix MR, Matrix M1) {

      saveResult (MR);
      eExpected = transposeCheck (MX, MR, M1);
      try {
         transpose (MR, M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);

      eExpected = transposeCheck (MX, MR, MR);
      try {
         transpose (MR);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   void testNegate (Matrix MR, Matrix M1) {
      saveResult (MR);
      eExpected = scaleCheck (MX, MR, M1, -1);
      try {
         negate (MR, M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);

      eExpected = scaleCheck (MX, MR, MR, -1);
      try {
         negate (MR);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   void testScale (Matrix MR, double s, Matrix M1) {
      saveResult (MR);
      eExpected = scaleCheck (MX, MR, M1, s);
      try {
         scale (MR, s, M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);

      eExpected = scaleCheck (MX, MR, MR, s);
      try {
         scale (MR, s);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   void testSet (Matrix MR, Matrix M1) {
      saveResult (MR);
      eExpected = scaleCheck (MX, MR, M1, 1);
      try {
         set (MR, M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   void testMulAdd (Matrix MR, Matrix M1, Matrix M2) {

      MatrixNd M0 = new MatrixNd (MR);
      M0.setRandom();
      MR.set (M0);
      MatrixNd MRchk = new MatrixNd (MR);
      MatrixNd M1N = new MatrixNd (M1);
      MatrixNd M2N = new MatrixNd (M2);

      if (M1 != MR) {
         M1N.setRandom();
         M1.set (M1N);
      }
      else {
         M1N.set (MR);
      }
      
      if (M2 != MR) {
         M2N.setRandom();
         M2.set (M2N);
      }
      else {
         M2N.set (MR);
      }

      MatrixNd T = new MatrixNd (MR);
      T.mul (M1N, M2N);
      MRchk.add (T);

      mulAdd (MR, M1, M2);
      if (!MR.epsilonEquals (MRchk, 1e-10)) {
         throw new TestException (
            "testMulAdd: result:\n" + MR + "Expected\n" + MRchk);
      }
      MR.set (M0);
      mulAdd (MR, M1N, M2N);
      if (!MR.epsilonEquals (MRchk, 1e-10)) {
         throw new TestException (
            "testMulAdd: result:\n" + MR + "Expected\n" + MRchk);
      }
   }

   void testMulAdd (Matrix MR) {

      int nr = MR.rowSize();
      int nc = MR.colSize();

      for (int nk=1; nk<=8; nk++) {
         MatrixBlock M1 = MatrixBlockBase.alloc (nr, nk);
         MatrixBlock M2 = MatrixBlockBase.alloc (nk, nc);
         testMulAdd (MR, M1, M2);
         if (nk == nc) {
            testMulAdd (MR, MR, M2);
         }
         if (nk == nr) {
            testMulAdd (MR, M1, MR);
         }
         if (nk == nc && nk == nr) {
            testMulAdd (MR, MR, MR);
         }
      }
   }

   private Exception checkAddSizes (Matrix MR, Matrix M1, Matrix M2) {
      if ((M1.rowSize() != M2.rowSize()) || (M1.colSize() != M2.colSize())) {
         return new ImproperSizeException ("Incompatible dimensions");
      }
      if ((MR.rowSize() != M1.rowSize()) || (MR.colSize() != M1.colSize())) {
         if (MR.isFixedSize()) {
            return new ImproperSizeException ("Incompatible dimensions");
         }
      }
      return null;
   }

   private static String submatrixStr (int r0, int nr, int c0, int nc) {
      return "("+r0+":"+(r0+nr-1)+","+c0+":"+(c0+nc-1)+")";
   }

   static Exception mulVectorCheck (
      VectorNd vx, Matrix MR, VectorNd vr, VectorNd v1) {

      if (v1.size() < MR.colSize()) {
         return new ImproperSizeException (
            "v1 size "+v1.size()+" < column size "+MR.colSize());
      }
      return mulVectorCheck (vx, MR, vr, v1, 0, MR.rowSize(), 0, MR.colSize());
   }

   static Exception mulVectorCheck (
      VectorNd vx, Matrix MR, VectorNd vr, VectorNd v1,
      int r0, int nr, int c0, int nc) {

      int rowf = r0+nr;
      int colf = c0+nc;

      if (colf > MR.colSize() || rowf > MR.rowSize()) {
         return new ImproperSizeException (
            "Specified submatrix "+submatrixStr(r0,nr,c0,nc)+
            " incompatible with "+MR.rowSize()+"x"+MR.colSize()+" matrix");
      }
      if (v1.size() < nc) {
         return new ImproperSizeException (
            "v1 size "+v1.size()+" < column size "+nc);
      }
      VectorNd vt = new VectorNd (nr);
      for (int i=r0; i<rowf; i++) {
         double sum = 0;
         for (int j=c0; j<colf; j++) {
            sum += MR.get(i,j)*v1.get(j-c0);
         }
         vt.set (i-r0, sum);
      }
      vx.set (vt);
      return null;
   }

   Exception mulAddVectorCheck (
      VectorNd vx, Matrix MR, VectorNd vr, VectorNd v1) {

      if (v1.size() != MR.colSize()) {
         return new ImproperSizeException (
            "v1 size "+v1.size()+" != column size "+MR.colSize());
      }
      if (vr.size() != MR.rowSize()) {
         return new ImproperSizeException (
            "vr size "+vr.size()+" != row size "+MR.rowSize());
      }
      return mulVectorCheck (vx, MR, vr, v1, 0, MR.rowSize(), 0, MR.colSize());
   }

   Exception mulAddVectorCheck (
      VectorNd vx, Matrix MR, VectorNd vr, VectorNd v1,
      int r0, int nr, int c0, int nc) {

      int rowf = r0+nr;
      int colf = c0+nc;

      if (colf > MR.colSize() || rowf > MR.rowSize()) {
         return new ImproperSizeException (
            "Specified submatrix "+submatrixStr(r0,nr,c0,nc)+
            " incompatible with "+MR.rowSize()+"x"+MR.colSize()+" matrix");
      }
      if (v1.size() < nc) {
         return new ImproperSizeException (
            "v1 size "+v1.size()+" < column size "+nc);
      }
      if (vr.size() < nr) {
         return new ImproperSizeException (
            "vr size "+vr.size()+" < row size "+nr);
      }
      VectorNd vt = new VectorNd (nr);
      for (int i=r0; i<rowf; i++) {
         double sum = 0;
         for (int j=c0; j<colf; j++) {
            sum += MR.get(i,j)*v1.get(j-c0);
         }
         vt.set (i-r0, sum);
      }
      for (int i=0; i<nr; i++) {
         vt.add (i, vr.get(i));
      }
      vx.set (vt);
      return null;
   }

   static Exception mulTransposeVectorCheck (
      VectorNd vx, Matrix MR, VectorNd vr, VectorNd v1) {

      if (v1.size() < MR.rowSize()) {
         return new ImproperSizeException (
            "v1 size "+v1.size()+" < column size "+MR.rowSize());
      }
      return mulTransposeVectorCheck (
         vx, MR, vr, v1, 0, MR.colSize(), 0, MR.rowSize());
   }

   static Exception mulTransposeVectorCheck (
      VectorNd vx, Matrix MR, VectorNd vr, VectorNd v1,
      int r0, int nr, int c0, int nc) {

      // rowf and colf are with respect to the *transposed* matrix
      int rowf = r0+nr;
      int colf = c0+nc;

      if (colf > MR.rowSize() || rowf > MR.colSize()) {
         return new ImproperSizeException (
            "Specified submatrix "+submatrixStr(r0,nr,c0,nc)+
            " incompatible with transpose of "+MR.rowSize()+"x"+MR.colSize()+
            " matrix");
      }
      if (v1.size() < nc) {
         return new ImproperSizeException (
            "v1 size "+v1.size()+" < nc "+nc);
      }
      VectorNd vt = new VectorNd (nr);
      for (int j=r0; j<rowf; j++) {
         double sum = 0;
         for (int i=c0; i<colf; i++) {
            sum += MR.get(i,j)*v1.get(i-c0);
         }
         vt.set (j-r0, sum);
      }
      vx.set (vt);
      return null;
   }

   Exception addCheck (MatrixNd MX, Matrix MR, Matrix M1, Matrix M2) {
      Exception e = checkAddSizes (MR, M1, M2);
      if (e != null) {
         return e;
      }
      double[] buf = new double[M1.rowSize() * M1.colSize()];
      for (int i = 0; i < M1.rowSize(); i++) {
         for (int j = 0; j < M1.colSize(); j++) {
            buf[i * M1.colSize() + j] = M1.get (i, j) + M2.get (i, j);
         }
      }
      MatrixNd MC = new MatrixNd (M1.rowSize(), M1.colSize(), buf);
      MX.set (MC);
      return null;
   }

   Exception scaledAddCheck (
      MatrixNd MX, Matrix MR, double s, Matrix M1, Matrix M2) {
      Exception e = checkAddSizes (MR, M1, M2);
      if (e != null) {
         return e;
      }
      double[] buf = new double[M1.rowSize() * M1.colSize()];
      for (int i = 0; i < M1.rowSize(); i++) {
         for (int j = 0; j < M1.colSize(); j++) {
            buf[i * M1.colSize() + j] = s * M1.get (i, j) + M2.get (i, j);
         }
      }
      MatrixNd MC = new MatrixNd (M1.rowSize(), M1.colSize(), buf);
      MX.set (MC);
      return null;
   }

   Exception subCheck (MatrixNd MX, Matrix MR, Matrix M1, Matrix M2) {
      Exception e = checkAddSizes (MR, M1, M2);
      if (e != null) {
         return e;
      }
      double[] buf = new double[M1.rowSize() * M1.colSize()];
      for (int i = 0; i < M1.rowSize(); i++) {
         for (int j = 0; j < M1.colSize(); j++) {
            buf[i * M1.colSize() + j] = M1.get (i, j) - M2.get (i, j);
         }
      }
      MatrixNd MC = new MatrixNd (M1.rowSize(), M1.colSize(), buf);
      MX.set (MC);
      return null;
   }

   Exception scaleCheck (MatrixNd MX, Matrix MR, Matrix M1, double scale) {
      Exception e = checkAddSizes (MR, M1, M1);
      if (e != null) {
         return e;
      }
      double[] buf = new double[M1.rowSize() * M1.colSize()];
      for (int i = 0; i < M1.rowSize(); i++) {
         for (int j = 0; j < M1.colSize(); j++) {
            buf[i * M1.colSize() + j] = scale * M1.get (i, j);
         }
      }
      MatrixNd MC = new MatrixNd (M1.rowSize(), M1.colSize(), buf);
      MX.set (MC);
      return null;
   }

   private double[] mulBuf = null;

   private Exception mulInit (
      Matrix MR, int nrows1, int ncols1, int nrows2, int ncols2) {
      if (ncols1 != nrows2) {
         return new ImproperSizeException ("Incompatible dimensions");
      }
      if (MR.rowSize() != nrows1 || MR.colSize() != ncols2) {
         if (MR.isFixedSize()) {
            return new ImproperSizeException ("Incompatible dimensions");
         }
      }
      mulBuf = new double[nrows1 * ncols2];
      return null;
   }

   //
   // private void mulFinish (Matrix MR, int nrows1, int ncols2)
   // {
   // if ((MR.rowSize() != nrows1) ||
   // (MR.colSize() != ncols2))
   // { MR.setSize (nrows1, ncols2);
   // }
   // MR.set (mulBuf);
   // }

   private void doMul (Matrix MR, Matrix M1, Matrix M2) {
      for (int i = 0; i < M1.rowSize(); i++) {
         for (int j = 0; j < M2.colSize(); j++) {
            double sum = 0;
            for (int k = 0; k < M1.colSize(); k++) {
               sum += M1.get (i, k) * M2.get (k, j);
            }
            mulBuf[i * M2.colSize() + j] = sum;
         }
      }
      if ((MR.rowSize() != M1.rowSize()) || (MR.colSize() != M2.colSize())) {
         MR.setSize (M1.rowSize(), M2.colSize());
      }
      MatrixNd MC = new MatrixNd (MR.rowSize(), MR.colSize(), mulBuf);
      MR.set (MC);
   }

   private void doTranspose (MatrixNd MR, Matrix M1) {
      for (int i = 0; i < MR.rowSize(); i++) {
         for (int j = 0; j < MR.colSize(); j++) {
            MR.set (i, j, M1.get (j, i));
         }
      }
   }

   Exception mulCheck (MatrixNd MX, Matrix MR, Matrix M1, Matrix M2) {
      Exception e =
         mulInit (MR, M1.rowSize(), M1.colSize(), M2.rowSize(), M2.colSize());
      if (e != null) {
         return e;
      }
      doMul (MX, M1, M2);
      return null;
   }

   Exception mulTransposeLeftCheck (
      MatrixNd MX, Matrix MR, Matrix M1, Matrix M2) {
      Exception e =
         mulInit (MR, M1.colSize(), M1.rowSize(), M2.rowSize(), M2.colSize());
      if (e != null) {
         return e;
      }
      MatrixNd MT1 = new MatrixNd (M1.colSize(), M1.rowSize());
      doTranspose (MT1, M1);
      doMul (MX, MT1, M2);
      return null;
   }

   Exception mulTransposeRightCheck (
      MatrixNd MX, Matrix MR, Matrix M1, Matrix M2) {
      Exception e =
         mulInit (MR, M1.rowSize(), M1.colSize(), M2.colSize(), M2.rowSize());
      if (e != null) {
         return e;
      }
      MatrixNd MT2 = new MatrixNd (M2.colSize(), M2.rowSize());
      doTranspose (MT2, M2);
      doMul (MX, M1, MT2);
      return null;
   }

   Exception mulTransposeBothCheck (
      MatrixNd MX, Matrix MR, Matrix M1, Matrix M2) {
      Exception e =
         mulInit (MR, M1.colSize(), M1.rowSize(), M2.colSize(), M2.rowSize());
      if (e != null) {
         return e;
      }
      MatrixNd MT1 = new MatrixNd (M1.colSize(), M1.rowSize());
      doTranspose (MT1, M1);
      MatrixNd MT2 = new MatrixNd (M2.colSize(), M2.rowSize());
      doTranspose (MT2, M2);
      doMul (MX, MT1, MT2);
      return null;
   }

   Exception mulInverseLeftCheck (
      MatrixNd MX, Matrix MR, Matrix M1, Matrix M2, Matrix M1inv) {
      if (M1.rowSize() != M1.colSize()) {
         return new ImproperSizeException ("matrix must be square");
      }
      Exception e =
         mulInit (MR, M1.rowSize(), M1.colSize(), M2.rowSize(), M2.colSize());
      if (e != null) {
         return e;
      }
      doMul (MX, M1inv, M2);
      return null;
   }

   Exception mulInverseRightCheck (
      MatrixNd MX, Matrix MR, Matrix M1, Matrix M2, Matrix M2inv) {
      Exception e =
         mulInit (MR, M1.rowSize(), M1.colSize(), M2.colSize(), M2.rowSize());
      if (e != null) {
         return e;
      }
      doMul (MX, M1, M2inv);
      return null;
   }

   Exception mulInverseBothCheck (
      MatrixNd MX, Matrix MR, Matrix M1, Matrix M2, Matrix M1inv, Matrix M2inv) {
      Exception e =
         mulInit (MR, M1.colSize(), M1.rowSize(), M2.colSize(), M2.rowSize());
      if (e != null) {
         return e;
      }
      doMul (MX, M1inv, M2inv);
      return null;
   }

   Exception transposeCheck (MatrixNd MX, Matrix MR, Matrix M1) {
      double[] buf = new double[M1.colSize() * M1.rowSize()];
      for (int i = 0; i < M1.rowSize(); i++) {
         for (int j = 0; j < M1.colSize(); j++) {
            buf[j * M1.rowSize() + i] = M1.get (i, j);
         }
      }
      if ((MR.rowSize() != M1.colSize()) || (MR.colSize() != M1.rowSize())) {
         if (MR.isFixedSize()) {
            return new ImproperSizeException ("Incompatible dimensions");
         }
      }
      MatrixNd MC = new MatrixNd (M1.colSize(), M1.rowSize(), buf);
      MX.set (MC);
      return null;
   }

   public static int[] randomPermutation (int size) {
      boolean[] used = new boolean[size];
      int[] perm = new int[size];

      for (int i = 0; i < size; i++) {
         int n = 0;
         if (i < size - 1) {
            n = RandomGenerator.get().nextInt (size - i);
         }
         int j = 0;
         for (int k = 0; k < used.length; k++) {
            if (!used[k]) {
               if (j++ == n) {
                  used[k] = true;
                  perm[i] = k;
               }
            }
         }
      }
      return perm;
   }
}
