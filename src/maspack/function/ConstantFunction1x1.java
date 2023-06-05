/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.util.Scannable;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.DoubleHolder;

public class ConstantFunction1x1 implements Diff1Function1x1, Scannable {

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

   public double eval (DoubleHolder deriv, double in) {
      if (deriv != null) {
         deriv.value = 0;
      }
      return 0;
   }

   public boolean isWritable() {
      return true;
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

}
