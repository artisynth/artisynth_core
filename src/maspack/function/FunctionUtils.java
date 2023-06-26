package maspack.function;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scan;
import maspack.util.Scannable;
import maspack.util.Write;

/**
 * Provides I/O methods for functions.
 */
public class FunctionUtils {

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
