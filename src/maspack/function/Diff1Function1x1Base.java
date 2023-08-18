package maspack.function;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.util.DoubleHolder;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scannable;
import maspack.util.Clonable;
import maspack.util.InternalErrorException;

/**
 * Base class for Diff1Function1x1 that implements Scannable
 */
public abstract class Diff1Function1x1Base
   implements Diff1Function1x1, Scannable, Clonable {

   /**
    * {@inheritDoc}
    */
   public abstract double eval (double in);
   
   /**
    * {@inheritDoc}
    */
   public abstract double eval (DoubleHolder deriv, double in);
  
   public boolean isWritable() {
      return true;
   } 

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      rtok.scanToken (']');
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      pw.println ("[ ]");
   }

   public Diff1Function1x1Base clone() {
      try {
         return (Diff1Function1x1Base)super.clone();
      }
      catch (Exception e) {
         throw new InternalErrorException ("Can't clone " + getClass());
      }
   }
}
