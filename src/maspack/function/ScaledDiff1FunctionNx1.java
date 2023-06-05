package maspack.function;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.util.Scannable;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.IndentingPrintWriter;
import maspack.util.Scan;
import maspack.util.Write;
import maspack.matrix.VectorNd;

public class ScaledDiff1FunctionNx1 implements Diff1FunctionNx1, Scannable {

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
            myFxn = Scan.scanClassAndObject (rtok, ref, Diff1FunctionNx1.class);
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
      if (myFxn instanceof Scannable) {
         // XXX
         pw.print ("function=");
         Write.writeClassAndObject (pw, (Scannable)myFxn, fmt, null);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

}
