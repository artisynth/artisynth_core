/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.io.*;

import maspack.util.NumberFormat;
import maspack.util.InternalErrorException;
import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;

import java.util.*;

import java.lang.reflect.Array;

public abstract class LemkeSolverBase {
   public static final int SOLVED = 1;
   public static final int UNBOUNDED_RAY = 2;
   public static final int PIVOT_LIMIT_EXCEEDED = 3;

   public static final int BASIC = 1;
   public static final int NEW = 3;

   protected static double DOUBLE_PREC = 2.2204460492503131e-16;
   protected static double WORKING_PREC = 100 * DOUBLE_PREC;
   protected double epsilon; // XXX determine this

   public static final double AUTO_EPSILON = -1.0;

   protected int minRatioMethod = BASIC;

   public static final int SHOW_BASIS = 0x01;
   public static final int SHOW_COLS = 0x02;
   public static final int SHOW_LEXICO_COLS = 0x06;
   public static final int SHOW_STATS = 0x08;

   protected int debug = 0;

   public static final int Z_VAR = 0x8000;
   public static final int W_VAR = 0x0000;
   public static final int Z0 = Z_VAR | 0x0;

   public static int AUTOMATIC_PIVOT_LIMIT = -1;

   protected int pivotLimit = AUTOMATIC_PIVOT_LIMIT;

   public int getPivotLimit() {
      return pivotLimit;
   }

   public void setPivotLimit (int limit) {
      pivotLimit = limit;
   }

   protected static class Variable {
      String name;
      int type;
      boolean isBasic;
      Variable complement;
      int col;
      int idx;

      boolean isW() {
         return (type & Z_VAR) == 0;
      }

      boolean isZ() {
         return (type & Z_VAR) != 0;
      }

      boolean isZ0() {
         return complement == null;
      }

      void setIndex (int i) {
         idx = i;
      }

      void init (int type, Variable complement) {
         if ((type & Z_VAR) != 0) {
            isBasic = false;
         }
         else {
            isBasic = true;
         }
         col = idx;
         this.complement = complement;
         this.type = type;
      }

      void set (Variable var) {
         name = var.name;
         type = var.type;
         isBasic = var.isBasic;
         complement = var.complement;
         col = var.col;
      }

      String getName() {
         if (name != null) {
            return name;
         }
         else if (isZ()) {
            if (complement == null) {
               return "z0";
            }
            else {
               return "z" + (idx + 1);
            }
         }
         else {
            return "w" + (idx + 1);
         }
      }
   }

   protected double[] qvNew = new double[0];
   protected boolean[] pivotIsRejected = new boolean[0];
   protected int[] candidates = new int[0];
   protected double[] iv = new double[0];

   protected Object growObjectArray (
      Object oldArray, int length, Class classType) {
      int oldLength = Array.getLength (oldArray);
      Object newArray = Array.newInstance (classType, length);

      for (int i = 0; i < oldLength; i++) {
         Array.set (newArray, i, Array.get (oldArray, i));
      }
      for (int i = oldLength; i < length; i++) {
         try {
            Array.set (newArray, i, classType.newInstance());
         }
         catch (Exception e) {
            throw new InternalErrorException (
               "Can't grow object array: "+e.getMessage());
         }
      }
      return newArray;
   }

   protected int cumulativePivotCnt = 0;

   protected void allocateSpace (int num) {
      if (qvNew.length < num) {
         qvNew = new double[num];
         pivotIsRejected = new boolean[num];
         candidates = new int[num];
         iv = new double[num];
      }
   }

   protected void clearRejectedPivots (int n) {
      for (int i = 0; i < n; i++) {
         pivotIsRejected[i] = false;
      }
   }

   protected void rejectPivot (int s) {
      pivotIsRejected[s] = true;
   }

   // 
   // Items relating to cycle checking ...
   //

   protected boolean cycleCheckingEnabled = false;
   protected int maxCycleCheckSize = 64;
   protected int maxBasisHistory = 512;

   public boolean cycleCheckingEnabled() {
      return cycleCheckingEnabled;
   }

   public void setCycleChecking (boolean enable) {
      cycleCheckingEnabled = enable;
   }

   public int getPivotCount() {
      return cumulativePivotCnt;
   }

   public void resetPivotCount() {
      cumulativePivotCnt = 0;
   }

   public void setPivotCount (int cnt) {
      cumulativePivotCnt = cnt;
   }

   public void setDebug (int code) {
      debug = code;
   }

   private int makeZeroList (
      int[] candidates, double[] mv, double[] qv, int nr, double maxQ) {
      double tol0 = Math.max (epsilon, maxQ * WORKING_PREC);
      int numCand = 0;
      System.out.println ("adding zeros");
      for (int k = 0; k < nr; k++) {
         if (qv[k] < tol0) {
            candidates[numCand++] = k;
            System.out.println ("k=" + k + " qv=" + qv[k]);
         }
      }
      System.out.println (" num candidates=" + numCand++);
      return numCand;
   }

   private int minRatio (
      int[] candidates, int numc, double[] mv, double[] qv, int z_i,
      boolean tie, boolean takeFirst)

   {
      double minRatio = Double.POSITIVE_INFINITY;
      int imin = -1;

      for (int k = 0; k < numc; k++) {
         int i = candidates[k];
         double ratio = -qv[i] / mv[i];
         if (qv[i] > 0 || tie) {
            if (ratio < minRatio) {
               minRatio = ratio;
               imin = i;
            }
         }
         else {
            minRatio = 0;
            imin = i;
         }
      }

      if (imin != -1 && takeFirst) {
         candidates[0] = imin;
         return 1;
      }

      int newc = 0;
      for (int k = 0; k < numc; k++) {
         int i = candidates[k];
         double ratio = -qv[i] / mv[i];
         if (Math.abs (ratio - minRatio) <= epsilon || (!tie && qv[i] <= 0)) {
            candidates[newc++] = i;
            if (i == z_i) {
               candidates[0] = i;
               return 1;
            }
         }
      }
      return newc;
   }

   private int minRatioX (
      int[] candidates, int numc, double[] mv, double[] qv, int z_i,
      boolean tie, boolean takeFirst, double qerrorEst)

   {
      int minIdx = candidates[0];
      double minRatio = -(qv[minIdx] + qerrorEst) / mv[minIdx];

      for (int k = 1; k < numc; k++) {
         int idx = candidates[k];
         if (qv[idx] + qerrorEst + mv[idx] * minRatio < 0) {
            minRatio = -(qv[idx] + qerrorEst) / mv[idx];
            minIdx = idx;
         }
      }

      if (takeFirst) {
         candidates[0] = minIdx;
         return 1;
      }

      int newc = 0;
      for (int k = 0; k < numc; k++) {
         int idx = candidates[k];
         if (qv[idx] + mv[idx] * minRatio < qerrorEst) {
            candidates[newc++] = idx;
            if (idx == z_i) {
               candidates[0] = idx;
               return 1;
            }
         }
      }
      return newc;
   }

   protected abstract boolean wIsBasic (int j);

   protected abstract void getBasisColumn (double[] iv, int j);

   protected abstract Variable[] getBasicVars();

   protected abstract Variable getWzVar();

   private boolean[] getDisplayedRows (int[] candidates, int numc, int numv) {
      boolean[] displayRow = new boolean[numv];
      for (int i = 0; i < numc; i++) {
         displayRow[candidates[i]] = true;
      }
      return displayRow;
   }

   private int initCandidates (
      double[] mv, double[] qv, int numv, boolean initial) {
      int numc = 0;
      if (initial) {
         for (int i = 0; i < numv; i++) {
            if (qv[i] < -epsilon) {
               candidates[numc++] = i;
            }
         }
      }
      else {
         for (int i = 0; i < numv; i++) {
            if (!pivotIsRejected[i] && mv[i] < -epsilon) {
               candidates[numc++] = i;
            }
         }
      }
      return numc;
   }

   public int lexicoMinRatioTest (
      double[] mv, double[] qv, int numv, int z_i, boolean initial) {
      int blocking_i = -1;
      boolean[] displayRow = null; // rows to print for debug

      double qerrorEst;
      if (conditionEstimateAvailable()) {
         qerrorEst = DOUBLE_PREC * numv * getConditionEstimate();
      }
      else {
         qerrorEst = epsilon;
      }

      boolean printCols = (debug & SHOW_COLS) != 0;
      boolean printLexicoCols = (debug & SHOW_LEXICO_COLS) == SHOW_LEXICO_COLS;

      int numc = initCandidates (mv, qv, numv, initial);
      if (numc == 0) {
         if (printCols) {
            printMinRatioInfo (mv, qv, numv, z_i, -1, null, 0);
         }
         return -1;
      }
      numc =
         minRatioX (candidates, numc, mv, qv, z_i, initial, initial, qerrorEst);

      if (numc > 1) {
         if (printLexicoCols) {
            printMinRatioInfo (mv, qv, numv, z_i, -1, candidates, numc);
            displayRow = getDisplayedRows (candidates, numc, numv);
         }
         for (int j = 0; j < numv && numc > 1; j++) {
            if (wIsBasic (j)) { // then iv is simply e(iv_j)
               numc = minRatioElementaryTest (candidates, j, numc);
               if ((debug & SHOW_LEXICO_COLS) != 0) {
                  for (int i = 0; i < numv; i++) {
                     iv[i] = (i == j ? 1 : 0);
                  }
               }
            }
            else {
               getBasisColumn (iv, j);
               for (int i = 0; i < numv; i++) {
                  iv[i] = -iv[i];
               }
               numc =
                  minRatioX (
                     candidates, numc, mv, iv, -1, true, false, qerrorEst);
            }
            if (printLexicoCols) {
               int b_i = (numc == 1 || j == numv - 1) ? candidates[0] : -1;
               System.out.println ("TIE break, basis column " + j + " :");
               printMinRatioInfo (
                  mv, iv, numv, z_i, b_i, candidates, numc, displayRow);
               displayRow = getDisplayedRows (candidates, numc, numv);
            }
         }
         if (numc > 1) {
            System.out.println ("Warning: lexicoMinRatio finished with " + numc
            + " candidates");
         }
      }
      blocking_i = candidates[0];

      if (printCols && displayRow == null) {
         numc = initCandidates (mv, qv, numv, initial);
         numc = minRatio (candidates, numc, mv, qv, z_i, initial, initial);
         printMinRatioInfo (mv, qv, numv, z_i, blocking_i, candidates, numc);
      }
      return blocking_i;
   }

   public LemkeSolverBase() {
   }

   /**
    * Performs a minimun ratio test on an elementary vector e(ei); i.e., e(i) =
    * 0 if i != ei and e(ei) = 1.
    * 
    * All we do in this case is eliminate any node from the list whose index is
    * ei.
    */
   protected int minRatioElementaryTest (int[] candidates, int ei, int numCand) {
      int k;
      for (k = 0; k < numCand; k++) {
         if (candidates[k] == ei) {
            break;
         }
      }
      if (k == numCand) {
         return numCand;
      }
      else {
         while (k < numCand - 1) {
            candidates[k] = candidates[k + 1];
            k++;
         }
         return numCand - 1;
      }
   }

   private boolean isCandidate (int i, int[] candidates, int numCand) {
      for (int k = 0; k < numCand; k++) {
         if (candidates[k] == i) {
            return true;
         }
      }
      return false;
   }

   protected void printBasis (Variable driveVar) {
      System.out.println ("Basis: " + basisString (driveVar));
   }

   protected String basisString (Variable driveVar) {
      return "";
   }

   protected void printMinRatioInfo (
      double[] mv, double[] qv, int numv, int z_i, int blocking_i,
      int[] candidates, int numc) {
      printMinRatioInfo (mv, qv, numv, z_i, blocking_i, candidates, numc, null);
   }

   private int[] getDisplayIndices (
      Variable[] basicVars, int numv, boolean[] displayRow, int z_i) {
      int[] idxs;

      if (displayRow == null) {
         idxs = new int[numv];
         for (int i = 0; i < numv; i++) {
            idxs[i] = i;
         }
      }
      else {
         int numdisp = 0;
         for (int i = 0; i < numv; i++) {
            if (displayRow[i]) {
               numdisp++;
            }
         }
         idxs = new int[numdisp];
         int k = 0;
         for (int i = 0; i < numv; i++) {
            if (displayRow[i]) {
               idxs[k++] = i;
            }
         }
      }

      // // bubble sort indices by variable index
      // for (int i=0; i<idxs.length-1; i++)
      // { for (int j=i+1; j<idxs.length; j++)
      // { int idx_i = basicVars[idxs[i]].idx;
      // int idx_j = basicVars[idxs[j]].idx;
      // if (basicVars[idxs[i]].isZ())
      // { idx_i = getWzVar().idx;
      // }
      // if (basicVars[idxs[j]].isZ())
      // { idx_j = getWzVar().idx;
      // }
      // if (idx_i > idx_j)
      // { int k = idxs[i]; idxs[i] = idxs[j]; idxs[j] = k;
      // }
      // }
      // }
      return idxs;
   }

   private int getMaxNameLength (Variable[] vars, int numv) {
      int maxNameLength = 0;
      for (int i = 0; i < numv; i++) {
         if (vars[i].getName().length() > maxNameLength) {
            maxNameLength = vars[i].getName().length();
         }
      }
      return maxNameLength;
   }

   protected void printQv (String msg, double[] qv, int numv) {
      Variable[] basicVars = getBasicVars();
      int maxNameLength = getMaxNameLength (basicVars, numv);

      NumberFormat ffmt = new NumberFormat ("%24g");
      StringBuffer sbuf = new StringBuffer (256);

      if (msg != null) {
         System.out.println (msg);
      }
      for (int i = 0; i < numv; i++) {
         sbuf.setLength (0);
         sbuf.append ("  ");
         sbuf.append (basicVars[i].getName());
         while (sbuf.length() < maxNameLength + 2) {
            sbuf.insert (2, ' ');
         }
         sbuf.append ("   ");
         sbuf.append (ffmt.format (qv[i]));
         System.out.println (sbuf.toString());
      }
   }

   protected void printPredictedQv (
      String msg, double[] mv, double[] qv, int s, Variable driveVar, int numv) {
      Variable[] basicVars = getBasicVars();
      int maxNameLength = getMaxNameLength (basicVars, numv);

      NumberFormat ffmt = new NumberFormat ("%24g");
      StringBuffer sbuf = new StringBuffer (256);
      double ratio = -qv[s] / mv[s];

      if (msg != null) {
         System.out.println (msg);
      }
      for (int i = 0; i < numv; i++) {
         sbuf.setLength (0);
         sbuf.append ("  ");
         if (i == driveVar.idx) {
            sbuf.append ("z0");
         }
         else if (i == s) {
            sbuf.append (driveVar.getName());
         }
         else {
            sbuf.append (basicVars[i].getName());
         }
         while (sbuf.length() < maxNameLength + 2) {
            sbuf.insert (2, ' ');
         }
         sbuf.append ("   ");
         if (i == s) {
            sbuf.append (ffmt.format (ratio));
         }
         else {
            sbuf.append (ffmt.format (qv[i] + ratio * mv[i]));
         }
         System.out.println (sbuf.toString());
      }
   }

   protected void printMinRatioInfo (
      double[] mv, double[] qv, int numv, int z_i, int blocking_i,
      int[] candidates, int numc, boolean[] displayRow) {
      Variable[] basicVars = getBasicVars();

      int idxs[] = getDisplayIndices (basicVars, numv, displayRow, z_i);
      boolean[] isCandidate = new boolean[numv];

      if (candidates != null) {
         for (int i = 0; i < numc; i++) {
            isCandidate[candidates[i]] = true;
         }
      }

      NumberFormat ifmt = new NumberFormat ("%3d");
      NumberFormat ffmt = new NumberFormat ("%24g");
      StringBuffer sbuf = new StringBuffer (256);

      if (blocking_i != -1) {
         System.out.println ("blocking variable="
         + basicVars[blocking_i].getName());
      }

      int maxNameLength = getMaxNameLength (basicVars, numv);

      for (int k = 0; k < idxs.length; k++) {
         int i = idxs[k];
         sbuf.setLength (0);
         sbuf.append (basicVars[i].isZ() ? "z " : "  ");
         sbuf.append (basicVars[i].getName());
         while (sbuf.length() < maxNameLength + 2) {
            sbuf.insert (2, ' ');
         }
         sbuf.append (' ');

         sbuf.append (isCandidate[i] ? '*' : ' ');
         sbuf.append (i == blocking_i ? "XX " : "   ");
         sbuf.append (ffmt.format (mv[i]));
         sbuf.append (ffmt.format (qv[i]));
         sbuf.append (ffmt.format (-qv[i] / mv[i]));
         System.out.println (sbuf.toString());
      }
   }

   protected boolean conditionEstimateAvailable() {
      return false;
   }

   protected double getConditionEstimate() {
      return 1;
   }

   public double getEpsilon() {
      return epsilon;
   }
}
