package maspack.numerics;

import maspack.util.*;
import maspack.function.*;

/**
 * Defines some functions that can be used for testing 1d root solvers.
 */
public class RootSolverTestBase extends UnitTest {

   protected class CosFxn implements Diff1Function1x1 {

      public double eval (double x) {
        return eval (null, x);
      }

      public double eval (DoubleHolder df, double x) {
         if (df != null) {
            df.value = -Math.sin(x);
         }
         return Math.cos(x);
      }
   }

   protected class CubicFxn implements Diff1Function1x1 {

      double myA3;
      double myA2;
      double myA1;
      double myA0;

      CubicFxn (double a3, double a2, double a1, double a0) {
         myA3 = a3;
         myA2 = a2;
         myA1 = a1;
         myA0 = a0;
      }

      public double eval (double x) {
         return eval (null, x);
      }

      public double eval (DoubleHolder df, double x) {
         if (df != null) {
            df.value = (3*myA3*x + 2*myA2)*x + myA1;
         }
         return ((myA3*x + myA2)*x + myA1)*x + myA0;
      }
   }

   protected class QuarticFxn implements Diff1Function1x1 {

      double myA4;
      double myA3;
      double myA2;
      double myA1;
      double myA0;

      QuarticFxn (double a4, double a3, double a2, double a1, double a0) {
         myA4 = a4;
         myA3 = a3;
         myA2 = a2;
         myA1 = a1;
         myA0 = a0;
      }

      public double eval (double x) {
         return eval (null, x);
      }

      public double eval (DoubleHolder df, double x) {
         if (df != null) {
            df.value = ((4*myA4*x + 3*myA3)*x + 2*myA2)*x + myA1;
         }
         return (((myA4*x + myA3)*x + myA2)*x + myA1)*x + myA0;
      }
   }
}
