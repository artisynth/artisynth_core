/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;
import maspack.solvers.PardisoSolver;
import maspack.matrix.VectorNd;
import maspack.matrix.Matrix;

public class PardisoThreadTest {

   private static boolean sym = false;

   private static boolean debug = true;

   private static int nDia;

   private static double nonZeroVactor = 0.8;

   public static class SparseElement {
      public int i;

      public int j;

      public double value;
   };

   public static int sparse_2_pardiso (
      Vector<SparseElement> SM, int numElements, double ra[], int ia[],
      int ja[]) {
      int index, raI = 0, iaI = 0, jaI = 0;
      int lastRow = -1;
      int smSize = numElements;

      /* For each element in the sparce matrix */
      /* All elements are going to be added to RA */
      /*
       * The j entry for the sparse element will be stored in ja, with a +1
       * offset as pardiso starts with a 1,1 coordinate system
       */
      /* If we have moved to a new row the value of i will be stored in ia */

      for (index = 0; index < smSize; index++) {
         if (SM.get (index).value * SM.get (index).value > 0.00000001) // value
         // is
         // non-zero
         // if(true)
         {
            if (sym == false) {
               ra[raI++] = SM.get (index).value;
               if (SM.get (index).i > lastRow) {
                  ia[iaI++] = (raI);
                  lastRow = SM.get (index).i;
               }
               ja[jaI++] = SM.get (index).j + 1;
            }

            if ((sym == true) && (SM.get (index).i <= SM.get (index).j)) {

               /*
                * Check to see if a zero diagonal element may need to be
                * inserted
                */
               if ((SM.get (index).i < SM.get (index).j)
               && (SM.get (index - 1).j < SM.get (index - 1).i)) {
                  if (debug)
                     System.out.println ("need to insert zero in diagonal ["
                     + SM.get (index).i + SM.get (index).i + "]");
                  ra[raI++] = 0;
                  if (SM.get (index).i > lastRow) {
                     ia[iaI++] = (raI);
                     lastRow = SM.get (index).i;
                  }
                  ja[jaI++] = SM.get (index).j + 1;
               }

               ra[raI++] = SM.get (index).value;
               if (SM.get (index).i > lastRow) {
                  ia[iaI++] = (raI);
                  lastRow = SM.get (index).i;
               }
               ja[jaI++] = SM.get (index).j + 1;
            }

         }
      }

      ia[iaI] = raI + 1; // add dummy terminator to ja
      return raI;

      // System.out.println("[");
      //	
      // for(int i = 0; i < raI; i++)
      // {
      // System.out.print(ra[i] +",");
      // if (i%200 == 199)
      // System.out.println();
      // }
      // System.out.println("]");
   }

   public static int importSparseMatrix (
      String filename, Vector<SparseElement> sparseMatrix, Integer nDim) {
      String line;
      int i = 0;
      SparseElement temp;
      BufferedReader in;
      int dimension = 0;
      ;

      try {
         in = new BufferedReader (new FileReader (filename));

         while ((line = in.readLine()) != null) {
            StringTokenizer st = new StringTokenizer (line);
            temp = new SparseElement();
            if (st != null && st.hasMoreTokens()) // to drop a new line at
            // the end of file
            {
               temp.i = Integer.parseInt (st.nextToken());
               temp.j = Integer.parseInt (st.nextToken());
               temp.value = Double.parseDouble (st.nextToken());
               sparseMatrix.add (temp);
               i++;
            }
            dimension = temp.i > dimension ? temp.i : dimension;
         }
      }
      catch (NumberFormatException e) {
         e.printStackTrace();
      }
      catch (IOException e) {
         e.printStackTrace();
      }

      nDia = dimension + 1;
      return i;
   }

   public static void main (String[] args) {

      Vector<SparseElement> sparseMatrix = new Vector<SparseElement>();
      int pardisoMatSize = 0, pardisoNumVals = 5;
      double ra[] = null;
      int ia[] = null;
      int ja[] = null;

      VectorNd bsol = new VectorNd (0);
      VectorNd vsol = new VectorNd (0);

      pardisoNumVals =
         importSparseMatrix ("lib/Xmat325.txt", sparseMatrix, pardisoMatSize);

      pardisoMatSize = nDia;
      ia = new int[pardisoMatSize + 2];
      ra = new double[(int)(nonZeroVactor * pardisoNumVals)];
      ja = new int[(int)(nonZeroVactor * pardisoNumVals)];

      sym = true;
      pardisoNumVals =
         sparse_2_pardiso (sparseMatrix, pardisoNumVals, ra, ia, ja);

      // int size = pardisoNumVals;
      // System.out.println("[");
      // for (int i = 0; i < size; i++)
      // {
      // System.out.print(ra[i] +",");
      // if (i%10 == 9)
      // System.out.println();
      // }
      // System.out.println("]");

      PardisoSolver pardiso = new PardisoSolver();

      System.out.println ("pardisoMatSize:" + pardisoMatSize);
      System.out.println ("pardisoNumVals:" + pardisoNumVals);
      System.out.println ("ra.length:" + ra.length);
      System.out.println ("ia.length:" + ia.length);
      System.out.println ("ja.length:" + ja.length);
      vsol.setSize (pardisoMatSize);
      bsol.setSize (pardisoMatSize);
      for (int i = 0; i < pardisoMatSize; i++) {
         bsol.set (i, i % 4);
      }

      pardiso.analyze (ra, ja, ia, pardisoMatSize, Matrix.SYMMETRIC);

      BusyThread busy = new BusyThread();
      busy.start();

      for (int i = 0; i < 1000000; i++) {
         System.out.println (i + " factoring ...");
         pardiso.factor (ra);
         System.out.println ("solving ...");
         pardiso.solve (vsol.getBuffer(), bsol.getBuffer());
      }

   }
}

class BusyThread extends Thread {
   public void run() {
      while (true) {
         System.out.println ("busy busy ...");
         try {
            Thread.sleep (5);
         }
         catch (Exception e) {
         }
      }
   }
}
