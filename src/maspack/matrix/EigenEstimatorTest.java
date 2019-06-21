package maspack.matrix;

import maspack.matrix.EigenEstimator.Ordering;
import maspack.util.RandomGenerator;
import maspack.util.TestException;
import maspack.util.UnitTest;

public class EigenEstimatorTest extends UnitTest {

   public static final double EPS = 20000*2.220446049250313e-16;

   EigenEstimator estimator = new EigenEstimator();

   protected void orderResults (
      Ordering which, int nev, VectorNd echk, MatrixNd Vchk) {

      int n = echk.size();

      Ordering order = (which != Ordering.BE ? which : Ordering.LA);
      EigenEstimator.dsesrt (order, n, echk.buf, n, Vchk.buf, Vchk.width);
      if (which == Ordering.BE) {
         int nswap = (nev+1)/2;
         for (int k=0; k<nswap; k++) {
            int i = nev/2 + k;
            int j = n-nswap + k;
            echk.exchangeElements (i, j);
            Vchk.exchangeColumns (i, j);
         }
      }
      else if (which == Ordering.LM || which == Ordering.LA) {
         for (int k=0; k<nev; k++) {
            echk.exchangeElements (k, n-nev+k);
            Vchk.exchangeColumns (k, n-nev+k);
         }
      }
      else {
         for (int k=0; k<nev; k++) {
            echk.exchangeElements (k, n-1-k);
            Vchk.exchangeColumns (k, n-1-k);
         }
      }
   }

   int cnt = 0;

   public void testMatrix (Matrix M, int nev, Ordering which) {

      int n = M.rowSize();
      VectorNd echk = new VectorNd (n);
      MatrixNd Vchk = new MatrixNd (n, n);

      EigenDecomposition evd = new EigenDecomposition();
      evd.factorSymmetric (M);
      evd.get (echk, Vchk);

      // if (cnt == 0) {
      //    System.out.println ("evd=\n" + echk.toString ("%8.3f"));
      //    System.out.println ("V=\n" + Vchk.toString ("%8.3f"));
      //    cnt++;
      // }

      System.out.println ("EIG=" + new VectorNd((evd.getEigReal())));

      orderResults (which, nev, echk, Vchk);
      echk.setSize (nev);
      Vchk.setSize (n, nev);

      // System.out.println ("Expecting for " + which);
      // System.out.println ("eig=\n" + echk.toString ("%8.3f"));
      // System.out.println ("V=\n" + Vchk.toString ("%8.3f"));

      MatrixNd V = new MatrixNd (n, nev);
      VectorNd e = new VectorNd(nev);

      int nconv = estimator.eigs (e, V, nev, which, M);
      if (nconv < 0) {
         throw new TestException ("eigs returned with error " +nconv);
      }
      if (nconv != nev) {
         System.out.println ("Only computed "+nconv+" eigenvalues:");
         System.out.println (e+"\nExpected "+nev+":\n"+echk);
      }
      
      e.setSize (nconv);
      V.setSize (n, nconv);

      //e.set (1, 2);

      // System.out.println ("Got: " + which);
      // System.out.println ("eig=\n" + e.toString ("%8.3f"));
      // System.out.println ("V=\n" + V.toString ("%8.3f"));

      double tol = evd.getMaxAbsEig()*EPS;
      if (!e.epsilonEquals (echk, tol)) {
         System.out.println ("Computed eigenvalues:\n"+e+"\nExpected:\n"+echk);
         System.out.println ("Tolerance=" + tol);
         throw new TestException ("Computed eigenvalues don't match");
      }
      // now check the eigenvectors
      // System.out.println ("V\n");
      // EigenEstimator.printMatrix (n, nev, V);
      // System.out.println ("Vchk\n");
      // EigenEstimator.printMatrix (n, n, Vchk);
      


      VectorNd vec = new VectorNd (n);
      VectorNd chk = new VectorNd (n);
      VectorNd err = new VectorNd (n);
      for (int j=0; j<nev; j++) {
         V.getColumn (j, vec);
         M.mul (chk, vec);
         vec.scale (e.get(j));
         err.sub (vec, chk);
         if (err.norm() > 1e-10*vec.norm()) {
            System.out.println ("e=" + e.get(j));
            System.out.println ("M*v=" + chk);
            System.out.println ("v*e=" + vec);
            throw new TestException (
               "Eigenvector "+j+" does not match, err="+err.norm()+
               " tol="+EPS*vec.norm());
         }
      }
   }

   public void testMatrix (Matrix M, int nev) {

      testMatrix (M, nev, Ordering.LA);
      testMatrix (M, nev, Ordering.SA);
   }
   
   public void test (int n, int nev) {
      MatrixNd M = new MatrixNd (n, n);
      M.setRandom(); 
      M.mulTranspose (M);
      if (n == 6) {
         try {
            M.write ("bar.txt", "#comment", Matrix.WriteFormat.CRS);
         }
         catch (Exception e) {
         }
      }
      testMatrix (M, nev, Ordering.LA);
      //testMatrix (M, nev, Ordering.SA);
   }

   public void test (int nev, double[] eigs) {
      int n = eigs.length;
      MatrixNd M = new MatrixNd (n, n);
      MatrixNd U = new MatrixNd (n, n);
      U.setRandomOrthogonal();
      M.set (U);
      M.mulDiagonalRight (new VectorNd(eigs));
      M.mulTranspose (U);
      try {
         M.write ("sing.txt", "#comment", Matrix.WriteFormat.CRS);
      }
      catch (Exception e) {
      }
      testMatrix (M, nev);
   }

   public void test() {

      MatrixNd M = new MatrixNd (6, 6);
      M.set (new double[] {
            1, 0, 4, 0, -1, 2,
            0, 2, 3, 0, -1, 1,
            4, 3, 3, -1, 1, 5,
            0, 0,-1, 4, -2, 0,
            -1,-1,1,-2, 5,  3,
            2, 1, 5, 0, 3,  6
         });            

      //test (2, new double[] {123, 45, 2, 1, 0.1, 0.01, 0.01});
      test (3, new double[] {123, 45, 2, 1, 1, 1, 0.1, 0.01, 0.01, 0.01});
      test (2, new double[] {123, 45, 2, 1, 0.01, 0});
      test (2, new double[] {123, 45, 2, 1, 0.0000001, 0});

      testMatrix (M, 1);
      testMatrix (M, 2);
      testMatrix (M, 3);


      test (2, new double[] {3, 2, 1, 0.0000001, 0, 0});
      test (2, new double[] {123, 60, 20, 1.2, 0.000001, 0, -0.000001, -20});
      test (3, new double[] {123, 60, 20, 1.2, 0.000001, 0, -0.000001, -20});
      test (4, new double[] {200, 200, 101, 50, 40, 0.00001, 0, 0,
                             -0.00002, -10, -10, -20});  

      for (int i=0; i<10; i++) {
         test (6, 2);
         test (10, 3);
         test (12, 4);
         test (16, 4);
         test (20, 5);
         test (25, 4);
      }
      
   }

   public static void main (String[] args) {
      EigenEstimatorTest tester = new EigenEstimatorTest();

      RandomGenerator.setSeed (0x1234);

      tester.runtest();
   }
}
