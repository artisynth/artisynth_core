package maspack.function;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.util.DoubleHolder;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class ScaledDiff1Function1x1 extends Diff1Function1x1Base {

   double myScale;
   Diff1Function1x1 myFxn;
   
   public ScaledDiff1Function1x1() {
   }

   public ScaledDiff1Function1x1 (double s, Diff1Function1x1 fxn) {
      myScale = s;
      myFxn = fxn;
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
   }

   public int inputSize() {
      return 1;
   }
   
   public double eval (double x) {
      return myScale*myFxn.eval (x);
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
            myFxn = FunctionUtils.scan (rtok, Diff1Function1x1.class);
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

}
