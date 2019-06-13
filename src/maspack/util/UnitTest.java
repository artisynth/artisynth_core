/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.IOException;

import maspack.matrix.Vector;
import maspack.matrix.Matrix;

/**
 * Base unit test class
 */
public class UnitTest {

   protected boolean mySilentP = false;

   public boolean getSilent() {
      return mySilentP;
   }

   public void setSilent (boolean silent) {
      mySilentP = silent;
   }

   public void check (String msg, boolean test) {
      if (!test) {
         throw new TestException ("Check failed: " + msg);
      }
   }

   public void checkEquals (String msg, Object result, Object check) {
      if (!result.equals(check)) {
         throw new TestException (
            msg + " = " + result.toString() +
            ", expected " + check.toString());
      }
   }

   public void checkEquals (String msg, Vector result, Vector check) {
      if (!result.equals(check)) {
         throw new TestException (
            msg + " =\n" + result.toString() +
            ", expected\n" + check.toString());
      }
   }

   public void checkEquals (String msg, Vector result, Vector check, double eps) {
      if (!result.epsilonEquals(check, eps)) {
         throw new TestException (
            msg + " =\n" + result.toString() +
            ", expected\n" + check.toString() + ", eps=" + eps);
      }
   }

   public void checkNormedEquals (
      String msg, Vector result, Vector check, double tol) {
      double eps = tol*(result.norm()+check.norm())/2;
      if (!result.epsilonEquals(check, eps)) {
         throw new TestException (
            msg + " =\n" + result.toString() +
            ", expected\n" + check.toString() + ", eps=" + eps);
      }
   }

   public void checkEquals (String msg, Matrix result, Matrix check) {
      if (!result.equals(check)) {
         throw new TestException (
            msg + " =\n" + result.toString() +
            ", expected\n" + check.toString());
      }
   }

   public void checkEquals (String msg, Matrix result, Matrix check, double eps) {
      if (!result.epsilonEquals(check, eps)) {
         throw new TestException (
            msg + " =\n" + result.toString() +
            ", expected\n" + check.toString() + "\neps=" + eps);
      }
   }

   public void checkNormedEquals (
      String msg, Matrix result, Matrix check, double tol) {
      double eps = tol*(result.frobeniusNorm()+check.frobeniusNorm())/2;
      if (!result.epsilonEquals(check, eps)) {
         throw new TestException (
            msg + " =\n" + result.toString() +
            ", expected\n" + check.toString() + "\neps=" + eps);
      }
   }         

   public void checkEquals (String msg, double result, double check, double eps) {
      if (Math.abs(result-check) > eps) {
         throw new TestException (
            msg + " = " + result + ", expected " + check + ", eps=" + eps);
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
