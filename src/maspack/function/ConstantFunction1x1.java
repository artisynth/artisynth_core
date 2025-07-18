/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.util.DoubleHolder;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.matrix.VectorNd;

public class ConstantFunction1x1 extends Diff1Function1x1Base {

   double c;
   
   public ConstantFunction1x1() {
   }
   
   public ConstantFunction1x1(double c) {
      setVal(c);
   }
   
   public double getVal() {
      return c;
   }
   
   public void setVal(double val) {
      c = val;
   }
   
   public double eval(double in) {
      return c;
   }

   /**
    * Override this here in case function is called zero-sized vector {@code
    * in.}
    */
   public double eval (VectorNd in) {
      return c;
   }

   public double eval (DoubleHolder deriv, double in) {
      if (deriv != null) {
         deriv.value = 0;
      }
      return 0;
   }

   /**
    * Override this here in case function is called with a zero-sized vector
    * {@code in}.
    */
   public double eval (VectorNd deriv, VectorNd in) {
      if (deriv != null) {
         if (deriv.size() != 1) {
            deriv.setSize (1);
         }
         deriv.set (0, 0);
         return 0;
      }
      else {
         return 0;
      }
   }

   /**
    * Override this here in case function is called with a zero-sized vectors
    * {@code in} and/or {@code deriv}.
    */
   public void evalDeriv (VectorNd deriv, VectorNd in) {
      if (deriv.size() > 0) {
         deriv.set (0, 0);
      }
   }

   public boolean equals (ConstantFunction1x1 fxn) {
      return c == fxn.c;
   }
 
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      c = rtok.scanNumber();
      rtok.scanToken (']');
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref) {
      pw.println ("[ "+fmt.format(c)+" ]");
   }

   public ConstantFunction1x1 clone() {
      return (ConstantFunction1x1)super.clone();
   }

}
