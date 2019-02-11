/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

class NumberFormatTest {
   NumberFormat fmt = new NumberFormat();
   StringBuffer sbuf = new StringBuffer (100);

   String preContents;
   int preLen;

   public NumberFormatTest() {
      preContents = "this came before";
      preLen = preContents.length();
   }

   public static void test (String fmtStr, int val, String check)
      throws Exception {
      NumberFormat fmt = new NumberFormat (fmtStr);
      String res = fmt.format (val);
      if (!check.equals (res)) {
         throw new Exception ("expected: " + check + "\n" + "     got: " + res);
      }
   }

   public static void test (String fmtStr, double val, String check)
      throws Exception {
      NumberFormat fmt = new NumberFormat (fmtStr);
      String res = fmt.format (val);
      if (!check.equals (res)) {
         throw new Exception ("\nexpected: " + check + "\n" + "     got: "
         + res);
      }
   }

   private void checkResults (String result, String check) throws Exception {
      if (!result.equals (check)) {
         throw new Exception ("\nexpected result: '" + check + "'"
         + "\n     got result: '" + result + "'");
      }
   }

   public void format (long val, String fmtStr, String check) throws Exception {
      String result;

      set (fmtStr, null);
      result = fmt.format (val);
      checkResults (result, check);

      sbuf.setLength (0);
      sbuf.append (preContents);
      fmt.format (val, sbuf);
      result = sbuf.substring (preLen);
      checkResults (result, check);
      if (!preContents.equals (sbuf.substring (0, preLen))) {
         throw new Exception ("Corrupted contents of string buffer"
         + "\nShould be: '" + preContents + "'" + "\n  but are: '"
         + sbuf.substring (0, preLen) + "'");
      }
   }

   public void format (int val, String fmtStr, String check) throws Exception {
      String result;

      set (fmtStr, null);
      result = fmt.format (val);
      checkResults (result, check);
   }

   public void format (double val, String fmtStr, String check)
      throws Exception {
      String result;

      set (fmtStr, null);
      result = fmt.format (val);
      checkResults (result, check);

      sbuf.setLength (0);
      sbuf.append (preContents);
      fmt.format (val, sbuf);
      result = sbuf.substring (preLen);
      checkResults (result, check);
      if (!preContents.equals (sbuf.substring (0, preLen))) {
         throw new Exception ("Corrupted contents of string buffer"
         + "\nShould be: '" + preContents + "'" + "\n  but are: '"
         + sbuf.substring (0, preLen) + "'");
      }
   }

   public void set (String fmtStr) throws Exception {
      set (fmtStr, null);
   }

   public void set (String fmtStr, String errorCheck) throws Exception {
      String errorMsg = null;
      try {
         fmt.set (fmtStr);
      }
      catch (Exception e) {
         errorMsg = e.getMessage();
      }
      if (errorCheck != null) {
         if (errorMsg == null || !errorCheck.equals (errorMsg)) {
            throw new Exception ("\nexpected exception: " + errorCheck
            + "\ngot: " + (errorMsg != null ? errorMsg : "None"));
         }
      }
      else {
         if (errorMsg != null) {
            throw new Exception ("\nunexpected exception: " + errorMsg);
         }
      }
      if (errorCheck == null) {
         NumberFormat newFmt = null;
         try {
            newFmt = new NumberFormat (fmt.toString());
         }
         catch (Exception e) {
            e.printStackTrace();
            throw new Exception (
               "\nError recreating format from string representation:"
               + "\nOriginal toString() result: " + fmt.toString());
         }
         if (!newFmt.equals (fmt)) {
            throw new Exception (
               "\nCannot recreate format from string representation:"
               + "\nOriginal fmtStr: " + fmtStr
               + "\nOriginal toString() result: " + fmt.toString()
               + "\nOriginal format:\n" + fmt.toLongString()
               + "\nRecreated toString() result: " + newFmt.toString()
               + "\nRecreated format:\n" + newFmt.toLongString());
         }
      }
   }

   public static void main (String[] args) {
      NumberFormatTest tester = new NumberFormatTest();

      try {
         tester.set ("%7.3f");

         tester.set ("%", "Format string terminates with '%'");
         tester.set (
            "%7.", "'.' in conversion spec not followed by precision value");
         tester.set (
            "%7.3q", "Conversion character 'q' not one of 'diouxXeEfgaA'");
         tester.set ("%7", "Format string ends prematurely");
         tester.set (
            "%7f%7f", "Format string has more than one conversion spec");

         tester.format (7.1, "%7.3f", "  7.100");
         tester.format (7.1, "%g", "7.1");
         tester.format (7.1, "%10g", "       7.1");
         tester.format (7.1, "A %7.3f B", "A   7.100 B");
         tester.format (123, "A %7.3f B", "A 123.000 B");
         tester.format (123.456, "A %7.3f B", "A 123.456 B");
         tester.format (123.45678, "A %7.3f B", "A 123.457 B");
         tester.format (-123.45678, "A %7.3f B", "A -123.457 B");
         tester.format (123.0045678, "A %7.3f B", "A 123.005 B");
         tester.format (-123.0045678, "A %7.3f B", "A -123.005 B");
         tester.format (123456.0045678, "A %7.3f B", "A 123456.005 B");
         tester.format (-12345.0045678, "A %7.3f B", "A -12345.005 B");

         tester.format (0.0000001, "%8.4f", "  0.0000");
         tester.format (9.25185853854297E-18, "%8.4f", "  0.0000");
         tester.format (0.000051, "%8.4f", "  0.0001");
         tester.format (0.00004, "%8.4f", "  0.0000");
         tester.format (9.25185853854297E-5, "%8.4f", "  0.0001");
         tester.format (9.25185853854297E-6, "%8.4f", "  0.0000");

         double a = 1.2816119594740609E-5;
         double x = 1.23456789012;
         double y = 123;
         double z = 1.2345e30;
         double w = 1.02;
         double u = 1.234e-5;
         double v = 29.999999999999996;

         // test fixed point conversion:

         tester.format (0.0, "%6.1f", "   0.0");
         tester.format (0.0, "%6.0f", "     0");
         tester.format (0.0, "%1.0f", "0");

         tester.format (1.0, "%8.3f", "   1.000");
         tester.format (1.0, "%6.1f", "   1.0");
         tester.format (1.0, "%6.0f", "     1");
         tester.format (1.0, "%1.0f", "1");
         tester.format (1.0, "% 1.0f", " 1");
         tester.format (1.0, "%+2.0f", "+1");
         tester.format (1.0, "%-2.0f", "1 ");
         tester.format (1.0, "%+#2.0f", "+1.");
         tester.format (1.0, "%-+#3.0f", "+1.");
         tester.format (1.0, "%-+#4.0f", "+1. ");
         tester.format (1.0, "% -+#4.0f", "+1. ");

         tester.format (-1.0, "%8.3f", "  -1.000");
         tester.format (-1.0, "%6.1f", "  -1.0");
         tester.format (-1.0, "%6.0f", "    -1");
         tester.format (-1.0, "%1.0f", "-1");
         tester.format (-1.0, "% 1.0f", "-1");
         tester.format (-1.0, "%+2.0f", "-1");
         tester.format (-1.0, "%-2.0f", "-1");
         tester.format (-1.0, "%+#2.0f", "-1.");
         tester.format (-1.0, "%-+#3.0f", "-1.");
         tester.format (-1.0, "%-+#4.0f", "-1. ");
         tester.format (-1.0, "% -+#4.0f", "-1. ");

         // test out rounding

         tester.format (99.9995, "%7.4f", "99.9995");
         tester.format (99.9994, "%7.3f", " 99.999");
         tester.format (99.99950001, "%7.3f", "100.000");
         tester.format (99.9996, "%7.3f", "100.000");

         // now do some meatier things ...

         tester.format (0.5, "b=|%8.4f|\n", "b=|  0.5000|\n");
         tester.format (123.541, "|%8.4f|", "|123.5410|");
         tester.format (123444.541, "|%8.4f|", "|123444.5410|");
         tester.format (123.541, "|%6.2f|", "|123.54|");
         tester.format (0.000003, "|%6.2f|", "|  0.00|");
         tester.format (a, "a=|%22.20f|", "a=|0.00001281611959474061|");
         tester.format (v, "v=|%10.3f|", "v=|    30.000|");

         tester.format (w, "w=|%4.2f|", "w=|1.02|");
         tester.format (-w, "-w=|%4.2f|", "-w=|-1.02|");
         tester.format (v, "v=|%10.3f|", "v=|    30.000|");
         tester.format (x, "x=|%f|", "x=|1.234568|");
         tester.format (u, "u=|%20f|", "u=|            0.000012|");
         tester.format (x, "x=|% .5f|", "x=| 1.23457|");
         tester.format (w, "w=|%20.5f|", "w=|             1.02000|");
         tester.format (x, "x=|%020.5f|", "x=|00000000000001.23457|");
         tester.format (x, "x=|%+20.5f|", "x=|            +1.23457|");
         tester.format (x, "x=|%+020.5f|", "x=|+0000000000001.23457|");
         tester.format (x, "x=|% 020.5f|", "x=| 0000000000001.23457|");
         tester.format (y, "y=|%#+20.5f|", "y=|          +123.00000|");
         tester.format (y, "y=|%-+20.5f|", "y=|+123.00000          |");
         tester.format (
            z, "z=|%20.5f|", "z=|1234500000000000000000000000000.00000|");

         // %e tests

         tester.format (0.0, "%e", "0.000000e+00");
         tester.format (0.0, "%E", "0.000000E+00");
         tester.format (-0.0, "%e", "-0.000000e+00");
         tester.format (-0.0, "%E", "-0.000000E+00");
         tester.format (x, "x=|%e|", "x=|1.234568e+00|");
         tester.format (9.999, "|%5.3e|", "|9.999e+00|");
         tester.format (9.999, "|%4.2e|", "|1.00e+01|");
         tester.format (u, "u=|%20e|", "u=|        1.234000e-05|");
         tester.format (x, "x=|% .5e|", "x=| 1.23457e+00|");
         tester.format (w, "w=|%20.5e|", "w=|         1.02000e+00|");
         tester.format (x, "x=|%020.5e|", "x=|0000000001.23457e+00|");
         tester.format (x, "x=|%+20.5e|", "x=|        +1.23457e+00|");
         tester.format (x, "x=|%+020.5e|", "x=|+000000001.23457e+00|");
         tester.format (x, "x=|% 020.5e|", "x=| 000000001.23457e+00|");
         tester.format (y, "y=|%#+20.5e|", "y=|        +1.23000e+02|");
         tester.format (y, "y=|%-+20.5e|", "y=|+1.23000e+02        |");

         // old %g tests

         // tester.format (0.0, "%g", "0");
         // tester.format (0.0, "%#g", "0.00000");
         // tester.format (-0.0, "%g", "-0");
         // tester.format (-0.0, "%#g", "-0.00000");
         // tester.format (x, "x=|%g|", "x=|1.23457|");
         // tester.format (z, "z=|%g|", "z=|1.2345e+30|");
         // tester.format (w, "w=|%g|", "w=|1.02|");
         // tester.format (u, "u=|%g|", "u=|1.234e-05|");
         // tester.format (y, "y=|%.2g|", "y=|1.2e+02|");
         // tester.format (y, "y=|%#.2g|", "y=|1.2e+02|");
         // tester.format (0.0, "|%.0g|", "|0|");
         // tester.format (123.0, "|%.0g|", "|1e+02|");
         // tester.format (123.0, "|%.1g|", "|1e+02|");
         // tester.format (123.0, "|%.2g|", "|1.2e+02|");
         // tester.format (123.0, "|%.2G|", "|1.2E+02|");
         // tester.format (123.0, "|%.3g|", "|123|");
         // tester.format (123.0, "|%#.3g|", "|123.|");

         // %g tests

         tester.format (0.0, "%g", "0.0");
         tester.format (0.0, "%#g", "0.0");
         tester.format (-0.0, "%g", "-0.0");
         tester.format (-0.0, "%#g", "-0.0");
         tester.format (x, "x=|%g|", "x=|1.23456789012|");
         tester.format (x, "x=|%14g|", "x=| 1.23456789012|");
         tester.format (x, "x=|%015g|", "x=|001.23456789012|");
         tester.format (x, "x=|%0-15g|", "x=|1.23456789012  |");
         tester.format (x, "x=|%12g|", "x=|1.23456789012|");
         tester.format (z, "z=|%g|", "z=|1.2345E30|");
         tester.format (w, "w=|%g|", "w=|1.02|");
         tester.format (u, "u=|%g|", "u=|1.234E-5|");
         tester.format (0.0, "|%.0g|", "|0|");
         tester.format (123.0, "|%.0g|", "|1e+02|");
         tester.format (123.0, "|%+.0g|", "|+1e+02|");
         tester.format (123.0, "|% .0g|", "| 1e+02|");
         tester.format (123.0, "|%.1g|", "|1e+02|");
         tester.format (123.0, "|%9.2g|", "|  1.2e+02|");
         tester.format (123.0, "|%.3g|", "|123|");
         tester.format (123.0, "|%#.3g|", "|123.|");
         tester.format (1.23, "|%.3g|", "|1.23|");

         // %a tests

         tester.format (0.0, "%a", "0x0p+0");
         tester.format (0.0, "%A", "0X0P+0");
         tester.format (-0.0, "%a", "-0x0p+0");
         tester.format (-0.0, "%A", "-0X0P+0");
         tester.format (x, "x=|%a|", "x=|0x1.3c0ca428c1d2bp+0|");
         tester.format (x, "x=|%A|", "x=|0X1.3C0CA428C1D2BP+0|");
         tester.format (9.999, "|%5.3a|", "|0x1.3ffp+3|");
         tester.format (9.999, "|%4.2a|", "|0x1.40p+3|");
         tester.format (u, "u=|%20a|", "u=|0x1.9e0fcaf9380fcp-17|");
         tester.format (u, "u=|%23a|", "u=|  0x1.9e0fcaf9380fcp-17|");
         tester.format (x, "x=|% .5a|", "x=| 0x1.3c0cap+0|");
         tester.format (u, "w=|%20.5a|", "w=|       0x1.9e0fdp-17|");
         tester.format (u, "w=|%020.5a|", "w=|00000000x1.9e0fdp-17|");
         tester.format (u, "w=|%+20.5a|", "w=|      +0x1.9e0fdp-17|");
         tester.format (u, "w=|%+020.5a|", "w=|+0000000x1.9e0fdp-17|");
         tester.format (u, "w=|% 020.5a|", "w=| 0000000x1.9e0fdp-17|");
         tester.format (u, "x=|%#+20.5a|", "x=|      +0x1.9e0fdp-17|");
         tester.format (u, "x=|%-+20.5a|", "x=|+0x1.9e0fdp-17      |");
         tester.format (0.0, "x=|%#a|", "x=|0x0.p+0|");
         tester.format (255.0, "x=|%a|", "x=|0x1.fep+7|");
         tester.format (255.0, "x=|%.1a|", "x=|0x1.0p+8|");
         tester.format (2047.0, "x=|%.2a|", "x=|0x1.00p+11|");
         tester.format (2043.0, "x=|%.2a|", "x=|0x1.ffp+10|");

         // %d tests

         int d = 0xCAFE;

         tester.format (-1, "%d", "-1");
         tester.format (-1, "%3d", " -1");
         tester.format (-1, "% 3d", " -1");
         tester.format (-1, "%03d", "-01");
         tester.format (0, "%d", "0");
         tester.format (0, "%3d", "  0");
         tester.format (0, "%+3d", " +0");
         tester.format (0, "%+03d", "+00");
         tester.format (d, "d = |%d|\n", "d = |51966|\n");
         tester.format (d, "d=|%20d|", "d=|               51966|");
         tester.format (d, "d=|%020d|", "d=|00000000000000051966|");
         tester.format (d, "d=|%+20d|", "d=|              +51966|");
         tester.format (d, "d=|% 020d|", "d=| 0000000000000051966|");
         tester.format (d, "d=|% +020d|", "d=|+0000000000000051966|");
         tester.format (d, "d=|%-20d|", "d=|51966               |");
         tester.format (d, "d=|%20.8d|", "d=|            00051966|");
         tester.format (d, "d=|%020.8d|", "d=|            00051966|");
         tester.format (0, "d=|%020.0d|", "d=|                    |");
         tester.format (0, "d=|%+020.0d|", "d=|                   +|");
         tester.format (0, "d=|%+-020.0d|", "d=|+                   |");

         // %x tests

         tester.format (d, "d=|%x|", "d=|cafe|");
         tester.format (d, "d=|%20X|", "d=|                CAFE|");
         tester.format (d, "d=|%#20x|", "d=|              0xcafe|");
         tester.format (d, "d=|%020X|", "d=|0000000000000000CAFE|");
         tester.format (d, "d=|%20.8x|", "d=|            0000cafe|");
         tester.format (d, "d=|%020.8x|", "d=|            0000cafe|");
         tester.format (d, "d=|%o|", "d=|145376|");
         tester.format (0xfffffffffffffffL, "d=|%x|", "d=|fffffffffffffff|");
         tester.format (-1L, "d=|%x|", "d=|ffffffffffffffff|");
         tester.format (-1, "d=|%x|", "d=|ffffffff|");
         tester.format (0, "d=|%020.0x|", "d=|                    |");
         tester.format (0, "d=|%+020.0x|", "d=|                    |");
         tester.format (0, "d=|%+-020.0x|", "d=|                    |");

         // %u tests

         tester.format (d, "u=|%u|", "u=|51966|");
         tester.format (d, "u=|%20u|", "u=|               51966|");
         tester.format (d, "u=|%#20u|", "u=|               51966|");
         tester.format (d, "u=|%020u|", "u=|00000000000000051966|");
         tester.format (d, "u=|%20.8u|", "u=|            00051966|");
         tester.format (d, "u=|%020.8u|", "u=|            00051966|");
         tester.format (
            0xffffffffffffffffL, "u=|%u|", "u=|18446744073709551615|");
         tester.format (-123L, "u=|%u|", "u=|18446744073709551493|");
         tester.format (-123, "u=|%u|", "u=|4294967173|");
         tester.format (-1, "u=|%u|", "u=|4294967295|");
         tester.format (0, "d=|%020.0u|", "d=|                    |");
         tester.format (0, "d=|%+020.0u|", "d=|                    |");
         tester.format (0, "d=|%+-020.0u|", "d=|                    |");

         // %o tests

         tester.format (d, "d=|%020o|", "d=|00000000000000145376|");
         tester.format (d, "d=|%#20o|", "d=|             0145376|");
         tester.format (d, "d=|%#020o|", "d=|00000000000000145376|");
         tester.format (d, "d=|%20.12o|", "d=|        000000145376|");
         tester.format (d, "d=|%020.12o|", "d=|        000000145376|");
         tester.format (0, "d=|%020.0d|", "d=|                    |");
         tester.format (0, "d=|%+020.0o|", "d=|                    |");
         tester.format (0, "d=|%+-020.0o|", "d=|                    |");

         tester.format (0, "|%o|", "|0|");

         double nz = -0.0;
         double pz = 0.0;

         // exceptional tests

         tester.format (1 / pz, ">>%g<<", ">>Inf<<");
         tester.format (1 / pz, ">>%7g<<", ">>    Inf<<");
         tester.format (1 / nz, ">>%7g<<", ">>   -Inf<<");
         tester.format (pz / nz, ">>%7g<<", ">>    NaN<<");
         tester.format (pz / nz, ">>%+7g<<", ">>    NaN<<");
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
