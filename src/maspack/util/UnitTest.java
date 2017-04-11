/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.IOException;

import maspack.matrix.Vector;

/**
 * Base unit test class
 */
public class UnitTest {

   public void check (String msg, boolean test) {
      if (!test) {
         throw new TestException ("Check failed: " + msg);
      }
   }

   public void checkEquals (String msg, Object result, Object check) {
      if (!result.equals(check)) {
         throw new TestException (
            msg + result.toString() +
            ", expected " + check.toString());
      }
   }

   public void checkEquals (String msg, Vector result, Vector check) {
      if (!result.equals(check)) {
         throw new TestException (
            msg + result.toString() +
            ", expected " + check.toString());
      }
   }

   public void checkEquals (String msg, Vector result, Vector check, double eps) {
      if (!result.epsilonEquals(check, eps)) {
         throw new TestException (
            msg + result.toString() +
            ", expected " + check.toString() + ", eps=" + eps);
      }
   }

   public void test() throws IOException {
   }

   public void runtest() {
      try {
         test();
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1); 
      }
      System.out.println ("\nPassed\n");      
   }      

}
