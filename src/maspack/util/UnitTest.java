/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.IOException;

import maspack.matrix.Vector;
import maspack.matrix.*;
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

   public void checkEquals (String msg, VectorNi result, VectorNi check) {
      if (!result.equals(check)) {
         throw new TestException (
            msg + " =\n" + result.toString() +
            ", expected\n" + check.toString());
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

   public void checkEquals (String msg, int result, int check) {
      if (result != check) {
         throw new TestException (
            msg + " = " + result + ", expected " + check);
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
   
   public void printUsageAndExit (String msg) {
      System.out.println ("Usage: "+this.getClass()+" "+msg);
      System.exit(1);
   }


   protected interface ExceptionTest {
      void run();
   }

   protected void checkForException (
      Exception chk, ExceptionTest test) {
      try {
         test.run();
      }
      catch (Exception e) {
         if (e.getClass() != chk.getClass()) {
            throw new TestException (
               "Expecting \""+chk.getClass().getName()+
               "\", got \""+e.getClass().getName()+"\"");
         }
         if (chk.getMessage() != null) {
            if (e.getMessage() == null ||
                !chk.getMessage().equals(e.getMessage())) {
               throw new TestException (
                  "Expecting \""+chk+"\", got \""+e+"\"");
            }
         }
         return;
      }
      throw new TestException (
         "Expecting exception "+chk.getClass().getName());
   }

   protected void checkForIllegalArgumentException (ExceptionTest test) {
      checkForException (
         new IllegalArgumentException(), test);
   }

   protected void checkForIllegalArgumentException (
      String msg, ExceptionTest test) {
      checkForException (
         new IllegalArgumentException(msg), test);
   }

}
