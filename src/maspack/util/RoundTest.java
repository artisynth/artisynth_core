/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

public class RoundTest extends UnitTest {

   static final double DOUBLE_PREC = 1e-15;

   void checkResult125 (String msg, double val, double res, double chk) {
      checkResult (msg, val, res, chk, DOUBLE_PREC*Math.abs(val));

      // check that the results are idempotent
      double xxx;
      xxx = Round.near125 (res);
      checkResult ("redo near125", res, xxx, res, 0);
      xxx = Round.up125 (res);
      checkResult ("redo up125", res, xxx, res, 0);
      xxx = Round.down125 (res);
      checkResult ("redo down125", res, xxx, res, 0);
   }

   void checkResultPow2 (String msg, double val, double res, double chk) {
      checkResult (msg, val, res, chk, DOUBLE_PREC*Math.abs(val));

      // check that the results are idempotent
      double xxx;
      xxx = Round.nearPow2 (res);
      checkResult ("redo nearPow2", res, xxx, res, 0);
      xxx = Round.upPow2 (res);
      checkResult ("redo upPow2", res, xxx, res, 0);
      xxx = Round.downPow2 (res);
      checkResult ("redo downPow2", res, xxx, res, 0);
   }

   void checkResult (String msg, double val, double res, double chk, double tol) {
      if (Math.abs (res-chk) > tol) {
         throw new TestException (msg+"("+val+"): got "+res+", expecting "+chk);
      }
   }

   void testSingle125 (double prev, double val, double next, double eps) {

      double valh = val + eps*(next-val);
      double vall = val - eps*(val-prev);

      double res;

      res = Round.near125 (valh);
      checkResult125 ("near125", valh, res, val);

      res = Round.up125 (valh);
      checkResult125 ("up125", valh, res, eps == 0 ? val : next);

      res = Round.down125 (valh);
      checkResult125 ("down125", valh, res, val);

      res = Round.near125 (vall);
      checkResult125 ("near125", vall, res, val);

      res = Round.up125 (vall);
      checkResult125 ("up125", vall, res, val);

      res = Round.down125 (vall);
      checkResult125 ("down125", vall, res, eps == 0 ? val : prev);


      res = Round.near125 (-valh);
      checkResult125 ("near125", -valh, res, -val);

      res = Round.up125 (-valh);
      checkResult125 ("up125", -valh, res, -val);

      res = Round.down125 (-valh);
      checkResult125 ("down125", -valh, res, eps == 0 ? -val : -next);

      res = Round.near125 (-vall);
      checkResult125 ("near125", -vall, res, -val);

      res = Round.up125 (-vall);
      checkResult125 ("up125", -vall, res, eps == 0 ? -val : -prev);

      res = Round.down125 (-vall);
      checkResult125 ("down125", -vall, res, -val);
   }

   void testSinglePow2 (double prev, double val, double next, double eps) {

      double valh = val + eps*(next-val);
      double vall = val - eps*(val-prev);

      double res;

      res = Round.nearPow2 (valh);
      checkResultPow2 ("nearPow2", valh, res, val);

      res = Round.upPow2 (valh);
      checkResultPow2 ("upPow2", valh, res, eps == 0 ? val : next);

      res = Round.downPow2 (valh);
      checkResultPow2 ("downPow2", valh, res, val);

      res = Round.nearPow2 (vall);
      checkResultPow2 ("nearPow2", vall, res, val);

      res = Round.upPow2 (vall);
      checkResultPow2 ("upPow2", vall, res, val);

      res = Round.downPow2 (vall);
      checkResultPow2 ("downPow2", vall, res, eps == 0 ? val : prev);


      res = Round.nearPow2 (-valh);
      checkResultPow2 ("nearPow2", -valh, res, -val);

      res = Round.upPow2 (-valh);
      checkResultPow2 ("upPow2", -valh, res, -val);

      res = Round.downPow2 (-valh);
      checkResultPow2 ("downPow2", -valh, res, eps == 0 ? -val : -next);

      res = Round.nearPow2 (-vall);
      checkResultPow2 ("nearPow2", -vall, res, -val);

      res = Round.upPow2 (-vall);
      checkResultPow2 ("upPow2", -vall, res, eps == 0 ? -val : -prev);

      res = Round.downPow2 (-vall);
      checkResultPow2 ("downPow2", -vall, res, -val);
   }

   public void test125 (int nmin, int nmax) {
      for (int n = nmin; n <= nmax; n++) {
         double exp = Math.pow (10, n);
         exp = 1;
         if (n > 0) {
            for (int i=0; i<n; i++) {
               exp *= 10;
            }
         }
         else if (n < 0) {
            for (int i=0; i<-n; i++) {
               exp /= 10;
            }
         }
         testSingle125 (0.5*exp, 1*exp, 2*exp, 0);
         testSingle125 (1*exp, 2*exp, 5*exp, 0);
         testSingle125 (2*exp, 5*exp, 10*exp, 0);
         testSingle125 (0.5*exp, 1*exp, 2*exp, 0.4);
         testSingle125 (1*exp, 2*exp, 5*exp, 0.4);
         testSingle125 (2*exp, 5*exp, 10*exp, 0.4);
         testSingle125 (0.5*exp, 1*exp, 2*exp, 1e-14);
         testSingle125 (1*exp, 2*exp, 5*exp, 1e-14);
         testSingle125 (2*exp, 5*exp, 10*exp, 1e-14);
      }
   }

   public void testPow2 (int nmin, int nmax) {
      for (int n = nmin; n <= nmax; n++) {
         double exp = Math.pow (2, n);
         testSingle125 (0.5*exp, exp, 2*exp, 0);
         testSingle125 (0.5*exp, exp, 2*exp, 0.4);
         testSingle125 (0.5*exp, exp, 2*exp, 1e-14);
      }
   }

   public void test() {
      test125 (-10, 10);
   }

   public static void main (String[] args) {
      RoundTest tester = new RoundTest();
      tester.runtest();
   }

}
