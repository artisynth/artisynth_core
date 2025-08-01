package maspack.function;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.util.DoubleHolder;
import maspack.util.Clonable;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.InternalErrorException;
import maspack.matrix.VectorNd;

public class ScaledDiff1Function1x1 extends Diff1Function1x1Base {

   double myScale;
   Diff1Function1x1 myFxn;
   boolean myFxnConstant = false;
   
   public ScaledDiff1Function1x1() {
   }

   public ScaledDiff1Function1x1 (double s, Diff1Function1x1 fxn) {
      myScale = s;
      setFunction (fxn);
   }
   
   public double getScale() {
      return myScale;
   }
   
   public void setScale (double s) {
      myScale = s;
   }
   
   public Diff1Function1x1 getFunction() {
      return myFxn;
   }
   
   public void setFunction (Diff1Function1x1 fxn) {
      myFxn = fxn;
      if (fxn instanceof ConstantFunction1x1) {
         myFxnConstant = true;
      }
   }

   public int inputSize() {
      return 1;
   }
   
   public double eval (double x) {
      return myScale*myFxn.eval (x);
   }

   /**
    * Override this here in case function is called with a zero-sized vector
    * {@code in} and the underlying function is constant.
    */
   public double eval (VectorNd in) {
      if (myFxnConstant) {
         return myScale*myFxn.eval(0);
      }
      else {
         return myScale*myFxn.eval (in.get(0));
      }
   }

   /**
    * {@inheritDoc}
    */
   public double eval (DoubleHolder dval, double x) {
      double value = myScale*myFxn.eval (dval, x);
      if (dval != null) {
         dval.value *= myScale;
      }
      return value;
   }

   /**
    * Override this here in case function is called with zero-sized vectors
    * {@code in} and/or {@code deriv} and the underlying function is constant.
    */
   public double eval (VectorNd deriv, VectorNd in) {
      if (myFxnConstant) {
         double d = 0;
         if (deriv != null) {
            if (deriv.size() != 1) {
               deriv.setSize (1);
            }
            deriv.set (0, d);
         }
         return d;
      }
      else {
         return super.eval (deriv, in);
      }
   }

   /**
    * Override this here in case function is called with zero-sized vectors
    * {@code in} and/or {@code deriv} and the underlying function is constant.
    */
   public void evalDeriv (VectorNd deriv, VectorNd in) {
      if (myFxnConstant) {
         if (deriv.size() > 0) {
            deriv.set (0, 0);
         }
      }
      else {
         super.evalDeriv (deriv, in);
      }
   }

   public boolean isWritable() {
      return true;
   }
 
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myFxn = null;
      myScale = 0;
      myFxnConstant = false;
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord ("scale")) {
            rtok.scanToken ('=');
            myScale = rtok.scanNumber();
         }
         else if (rtok.tokenIsWord ("function")) {
            rtok.scanToken ('=');
            setFunction (FunctionUtils.scan (rtok, Diff1Function1x1.class));
         }
         else {
            throw new IOException (
               "Unexpect token or attribute name: " + rtok);
         }
      }
   }

   public void write (
      PrintWriter pw, NumberFormat fmt, Object ref) throws IOException {
      
      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.println ("scale=" + fmt.format (myScale));
      pw.print ("function=");
      FunctionUtils.write (pw, myFxn, fmt);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public ScaledDiff1Function1x1 clone() {
      ScaledDiff1Function1x1 fxn = (ScaledDiff1Function1x1)super.clone();
      if (myFxn instanceof Clonable) {
         try {
            setFunction ((Diff1Function1x1)((Clonable)myFxn).clone());
         }
         catch (Exception e) {
            throw new InternalErrorException ("Can't clone " + getClass());
         }
      }
      return fxn;
   }

}
