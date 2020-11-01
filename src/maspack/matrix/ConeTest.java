package maspack.matrix;

import maspack.util.*;

public class ConeTest {

   public boolean test (int numc) {
      Vector3d nrm = new Vector3d();
      nrm.setRandom();

      MatrixNd NT = new MatrixNd(numc,3);
      for (int i=0; i<numc; i++) {
         Vector3d row = new Vector3d();
         do {
            row.setRandom();
         }
         while (row.dot(nrm) < 0);
         row.normalize();
         NT.setRow (i, row);
      }
      QRDecomposition qr = new QRDecomposition();
      qr.factorWithPivoting (NT);
      VectorNd wn = new VectorNd (numc);
      qr.leftSolve (wn, nrm);
      boolean positive = true;
      for (int i=0; i<numc; i++) {
         if (wn.get(i) < 0) {
            positive = false;
            break;
         }
      }
      if (!positive) {
         System.out.println ("NT=\n" + NT.toString ("%12.9f"));
         System.out.println ("nrm=\n" + nrm.toString ("%12.9f"));
         System.out.println ("wn=\n" + wn.toString ("%12.9f"));
         System.exit(1); 
      }
      
      return positive;
   }

   public void test() {
      int numt = 1000;
      int nump = 0;
      for (int i=0; i<numt; i++) {
         if (test(3)) {
            nump++;
         }
      }
      System.out.println ("nump=" + nump);
   }

   public static void main (String[] args) {
      ConeTest tester = new ConeTest();
      RandomGenerator.setSeed (0x1234);
      tester.test();
   }
}
