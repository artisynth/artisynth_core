/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import maspack.util.DoubleHolder;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public abstract class PolyFunction1x1Base extends Diff1Function1x1Base {

   double[] myA = new double[0];
   
   public PolyFunction1x1Base() {
   }
   
   protected void setCoefficients (double... coefs) {
      myA = new double[coefs.length];
      for (int i=0; i<coefs.length; i++) {
         myA[i] = coefs[i];
      }
   }

   public double[] getCoefficents() {
      return myA;
   }
   
   public abstract int numCoefficients();

   public double getA (int idx) {
      if (idx >= myA.length) {
         throw new IllegalArgumentException (
            "'idx' is " + idx +
            "; must be < num coefficients (" + numCoefficients());
      }      
      return myA[idx];
   }

   public void setA (int idx, int a) {
      if (idx >= myA.length) {
         throw new IllegalArgumentException (
            "'idx' is " + idx +
            "; must be < num coefficients (" + numCoefficients());
      }      
      myA[idx] = a;
   }
   
   public double eval (double x) {
      if (myA.length == 0) {
         return 0;
      }
      else if (myA.length == 1) {
         return myA[0];
      }
      else {
         double value = 0;
         for (int i=myA.length-1; i>=0; i--) {
            value = value*x + myA[i];
         }
         return value;
      }
   }

   public double evalDeriv (double x) {
      if (myA.length <= 1) {
         return 0;
      }
      else if (myA.length == 2) {
         return myA[1];
      }
      else {
         double value = 0;
         for (int i=myA.length-1; i>=1; i--) {
            value = value*x + i*myA[i];
         }
         return value;
      }
   }

   public double eval (DoubleHolder deriv, double x) {
      if (deriv != null) {
         deriv.value = evalDeriv (x);
      }
      return eval (x);
   }

   public boolean equals (PolyFunction1x1Base fxn) {
      for (int i=0; i<myA.length; i++) {
         if (myA[i] != fxn.myA[i]) {
            return false;
         }
      }
      return true;
   }
 
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      ArrayList<Double> alist = new ArrayList<>();
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsNumber()) {
            throw new IOException (
               "expected coefficient value, got " + rtok);
         }
         alist.add (rtok.nval);
      }
      if (alist.size() != numCoefficients()) {
         throw new IOException (
            "read " + alist.size() + " coefficients, expected " +
            numCoefficients()+", line "+rtok.lineno());
      }
      myA = new double[alist.size()];
      for (int i=0; i<alist.size(); i++) {
         myA[myA.length-1-i] = alist.get(i);
      }
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref) {
      pw.print ("[ ");
      for (int i=myA.length-1; i>=0; i--) {
         pw.print (fmt.format(myA[i])+" ");
      }
      pw.println ("]");
   }

}
