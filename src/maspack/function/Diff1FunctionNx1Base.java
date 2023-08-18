package maspack.function;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.matrix.VectorNd;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scannable;
import maspack.util.Clonable;
import maspack.util.InternalErrorException;

/**
 * Base class for Diff1FunctionNx1 that implements Scannable
 */
public abstract class Diff1FunctionNx1Base
   implements Diff1FunctionNx1, Scannable, Clonable {

   /**
    * {@inheritDoc}
    */
   public abstract double eval (VectorNd in);
   
   /**
    * {@inheritDoc}
    */
   public abstract double eval (VectorNd deriv, VectorNd in);
  
   public boolean isWritable() {
      return true;
   } 

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      rtok.scanToken (']');
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref) throws
      IOException {
      pw.println ("[ ]");
   }

   public Diff1FunctionNx1Base clone() {
      try {
         return (Diff1FunctionNx1Base)super.clone();
      }
      catch (Exception e) {
         throw new InternalErrorException ("Can't clone " + getClass());
      }
   }

}
