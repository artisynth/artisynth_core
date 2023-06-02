package maspack.function;

import maspack.util.*;

public class ConstantFunction1x1Test extends UnitTest {
   
   public void test() {
      ConstantFunction1x1 fxn = new ConstantFunction1x1 (12.3);
      ConstantFunction1x1 newfxn =
         (ConstantFunction1x1)ScanTest.testWriteAndScanWithClass (
            fxn, null, "%g");
      if (!fxn.equals (newfxn)) {
         throw new TestException (
            "written-scanned function not equal to original");
      }      
   }

   public static void main (String[] args) {
      ConstantFunction1x1Test tester = new ConstantFunction1x1Test();
      tester.runtest();
   }
}
