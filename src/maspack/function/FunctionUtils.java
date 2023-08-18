package maspack.function;

import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;

import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scan;
import maspack.util.Scannable;
import maspack.util.Write;
import maspack.util.DoubleInterval;

/**
 * Provides I/O methods for functions.
 */
public class FunctionUtils {

   public static void writeValues (
      PrintWriter pw, Function1x1 fxn, double x0, double x1, int npnts,
      String fmtStr) throws IOException {
      
      NumberFormat fmt = new NumberFormat(fmtStr);
      double delx = (npnts > 1 ? (x1-x0)/(npnts-1) : (x1-x0));
      for (int i=0; i<npnts; i++) {
         double x = x0 + i*delx;
         double y = fxn.eval (x);
         pw.println ("" + fmt.format(x) + " " + fmt.format(y));
      }
   }

   public static void writeValues (
      File file, Function1x1 fxn, double x0, double x1, int npnts,
      String fmtStr) throws IOException {
      
      PrintWriter pw = null;
      try {
         pw = new PrintWriter (new BufferedWriter (new FileWriter (file)));
         writeValues (pw, fxn, x0, x1, npnts, fmtStr);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (pw != null) {
            pw.close();
         }
      }
   }

   public static <T> void write (
      PrintWriter pw, T fxn, NumberFormat fmt) throws IOException {
      if (!(fxn instanceof Scannable)) {
         System.out.println (
            "WARNING: function class " + fxn.getClass() +
            " not Scannable; file may not read properly");
      }
      Write.writeClassAndObject (pw, fxn, fmt, null);
   }

   public static <T> T scan (
      ReaderTokenizer rtok, Class<T> classType) throws IOException {
      T fxn = Scan.scanClassAndObject (rtok, null, classType);
      if (!(fxn instanceof Scannable)) {
         System.out.println (
            "WARNING: function class " + fxn.getClass() +
            " not Scannable; file may not read properly");
      }
      return fxn;
   }
}
