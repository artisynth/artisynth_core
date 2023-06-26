package maspack.function;

import maspack.util.RandomGenerator;
import maspack.util.ScanTest;
import maspack.util.TestException;
import maspack.util.UnitTest;

public class PolyFunction1x1Test extends UnitTest {
   
   public void testLinear (double a1, double a0) {
      LinearFunction1x1 fxn = new LinearFunction1x1 (a1, a0);

      double EPS = 1e-14;
      for (int i=0; i<10; i++) {
         double x = RandomGenerator.nextDouble (-1.0, 1.0);
         double ychk = a1*x + a0;
         double dchk = a1;
         checkEquals ("eval linear", fxn.eval (x), ychk, EPS);
         checkEquals ("evalDeriv linear", fxn.evalDeriv (x), dchk, EPS);
      }
      testWriteAndScan (fxn);
   }

   public void testQuadratic (double a2, double a1, double a0) {
      QuadraticFunction1x1 fxn = new QuadraticFunction1x1 (a2, a1, a0);

      double EPS = 1e-14;
      for (int i=0; i<10; i++) {
         double x = RandomGenerator.nextDouble (-1.0, 1.0);
         double ychk = (a2*x + a1)*x + a0;
         double dchk = 2*a2*x + a1;
         checkEquals ("eval mquadratic", fxn.eval (x), ychk, EPS);
         checkEquals ("evalDeriv quadratic", fxn.evalDeriv (x), dchk, EPS);
      }
      testWriteAndScan (fxn);
   }

   public void testCubic (double a3, double a2, double a1, double a0) {
      CubicFunction1x1 fxn = new CubicFunction1x1 (a3, a2, a1, a0);

      double EPS = 1e-14;
      for (int i=0; i<10; i++) {
         double x = RandomGenerator.nextDouble (-1.0, 1.0);
         double ychk = ((a3*x + a2)*x + a1)*x + a0;
         double dchk = (3*a3*x + 2*a2)*x + a1;
         checkEquals ("eval cubic", fxn.eval (x), ychk, EPS);
         checkEquals ("evalDeriv cubic", fxn.evalDeriv (x), dchk, EPS);
      }
      testWriteAndScan (fxn);
   }

   public void testWriteAndScan (PolyFunction1x1Base fxn) {
      PolyFunction1x1Base newfxn =
         (PolyFunction1x1Base)ScanTest.testWriteAndScanWithClass (
            fxn, null, "%g");
      if (!fxn.equals (newfxn)) {
         throw new TestException (
            "written-scanned function not equal to original");
      }      
   }

   public void test() {
      testLinear (3.1, 4.5);
      testQuadratic (1.5, -7, 6.7);
      testCubic (2.3, 1.5, -7, 6.7);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      PolyFunction1x1Test tester = new PolyFunction1x1Test();
      tester.runtest();
   }
}
