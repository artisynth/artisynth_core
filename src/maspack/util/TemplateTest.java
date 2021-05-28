package maspack.util;

import maspack.util.*;
import maspack.matrix.*;

public class TemplateTest extends UnitTest {

   public void test() {
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      TemplateTest tester = new TemplateTest();
      tester.runtest();
   }

}
