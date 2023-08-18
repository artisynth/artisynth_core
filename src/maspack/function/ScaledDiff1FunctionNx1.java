package maspack.function;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.matrix.VectorNd;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Clonable;
import maspack.util.InternalErrorException;

public class ScaledDiff1FunctionNx1 extends Diff1FunctionNx1Base {

   double myScale;
   Diff1FunctionNx1 myFxn;
   
   public ScaledDiff1FunctionNx1() {
   }

   public ScaledDiff1FunctionNx1 (double s, Diff1FunctionNx1 fxn) {
      myScale = s;
      myFxn = fxn;
   }
   
   public double getScale() {
      return myScale;
   }
   
   public void setScale (double s) {
      myScale = s;
   }
   
   public Diff1FunctionNx1 getFunction() {
      return myFxn;
   }
   
   public void setFunction (Diff1FunctionNx1 fxn) {
      myFxn = fxn;
   }

   public int inputSize() {
      return myFxn.inputSize();
   }
   
   public double eval (VectorNd in) {
      return myScale*myFxn.eval (in);
   }

   /**
    * {@inheritDoc}
    */
   public double eval (VectorNd deriv, VectorNd in) {
      double value = myScale*myFxn.eval (deriv, in);
      if (deriv != null) {
         deriv.scale (myScale);
      }
      return value;
   }

   public boolean isWritable() {
      return true;
   }
 
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myFxn = null;
      myScale = 0;
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord ("scale")) {
            rtok.scanToken ('=');
            myScale = rtok.scanNumber();
         }
         else if (rtok.tokenIsWord ("function")) {
            rtok.scanToken ('=');
            myFxn = FunctionUtils.scan (rtok, Diff1FunctionNx1.class);
         }
         else {
            throw new IOException (
               "Unexpect token or attribute name: " + rtok);
         }
      }
      rtok.scanToken (']');
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

   public ScaledDiff1FunctionNx1 clone() {
      ScaledDiff1FunctionNx1 fxn = (ScaledDiff1FunctionNx1)super.clone();
      if (myFxn instanceof Clonable) {
         try {
            fxn.myFxn = (Diff1FunctionNx1)((Clonable)myFxn).clone();
         }
         catch (Exception e) {
            throw new InternalErrorException ("Can't clone " + getClass());
         }
      }
      return fxn;
   }

}
