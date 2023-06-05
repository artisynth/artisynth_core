package maspack.function;

import maspack.util.*;
import maspack.matrix.*;

public class LinearFunctionNx1Test extends UnitTest {

   void testWriteAndScan (LinearFunctionNx1 fxn) {
      LinearFunctionNx1 newfxn =
         (LinearFunctionNx1)ScanTest.testWriteAndScanWithClass (
            fxn, null, "%g");
      if (!fxn.equals (newfxn)) {
         throw new TestException (
            "written-scanned function not equal to original");
      }      
   }

   void test (LinearFunctionNx1 fxn) {
      double[] c = fxn.getCoefficients();
      VectorNd x = new VectorNd(fxn.inputSize());
      VectorNd deriv = new VectorNd();
      x.setRandom();
      double val = fxn.eval (x);
      fxn.evalDeriv (deriv, x);

      VectorNd derivChk = new VectorNd (c);
      derivChk.setSize (x.size()); // drop last coefficient
      double valChk = derivChk.dot(x) + c[c.length-1];

      checkEquals ("value", val, valChk);
      checkEquals ("deriv", deriv, derivChk);
      
      testWriteAndScan (fxn);
   }
   
   public void test() {
      test (new LinearFunctionNx1 (4, -3));
      test (new LinearFunctionNx1 (new double[] { 1, 2, 3 }));
      test (new LinearFunctionNx1 (new double[] { -3, 4, 5, 5.3 }));
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      LinearFunctionNx1Test tester = new LinearFunctionNx1Test();
      tester.runtest();
   }
}
