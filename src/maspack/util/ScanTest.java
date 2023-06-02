/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Provides a method to test the scan/write methods of a Scannable object
 */
public class ScanTest {

   /**
    * Tests the scan and write methods of an object by writing the object,
    * scanning it, writing it again, and making sure that the second write
    * output equals the first.
    */
   public static void testWriteAndScan (
      Scannable s, Object ref, String fmtStr) {
      testWriteAndScan (s, ref, fmtStr, /*useClassTag=*/false);
   }
   
   /**
    * Tests the scan and write methods of an object by writing the object,
    * preceded by its class name, and then using this to scan a new instance of
    * the object. The new object is then written again and the outout is
    * checked to see that it matched that of the first. The new object
    * instance is returned.
    */
   public static Scannable testWriteAndScanWithClass (
      Scannable s, Object ref, String fmtStr) {
      return testWriteAndScan (s, ref, fmtStr, /*useClassTag=*/true);
   }

   private static Scannable testWriteAndScan (
      Scannable s, Object ref, String fmtStr, boolean useClassTag) {

      NumberFormat fmt;
      if (fmtStr == null) {
         fmt = new NumberFormat ("%g");
      }
      else {
         fmt = new NumberFormat (fmtStr);
      }
      StringWriter sw;
      PrintWriter pw;
         
      sw = new StringWriter();
      pw = new IndentingPrintWriter (sw);
      try {
         if (useClassTag) {
            Write.writeClassAndObject (pw, s, fmt, ref);
         }
         else {
            s.write (pw, fmt, ref);
         }
      }
      catch (Exception e) {
         throw new TestException (
            "exception during first write: " + e);
      }
      pw.flush();
      String str1 = sw.toString();

      ReaderTokenizer rtok = new ReaderTokenizer (new StringReader (str1));
      rtok.wordChars ("./$");
      try {
         if (useClassTag) {
            s = Scan.scanClassAndObject (rtok, ref, s.getClass());
         }
         else {
            s.scan (rtok, ref);
         }
      }
      catch (Exception e) {
         throw new TestException (
            "exception during scan: " + e);
      }
      sw = new StringWriter();
      pw = new IndentingPrintWriter (sw);
      try {
         if (useClassTag) {
            Write.writeClassAndObject (pw, s, fmt, ref);
         }
         else {
            s.write (pw, fmt, ref);
         }
      }
      catch (Exception e) {
         throw new TestException (
            "exception during second write: " + e);
      }
      pw.flush();
      String str2 = sw.toString();
      if (!str1.equals (str2)) {
         System.out.println ("first output:");
         System.out.println (str1);
         System.out.println ("second output:");
         System.out.println (str2);
         throw new TestException (
            "write-scan-write does not reproduce the same output");
      }
      return s;
   }

   /**
    * Writes a Scannable to a string using its write method and returns the
    * string.
    */
   public static String writeToString (Scannable s, String fmtStr, Object ref) {
      StringWriter sw;
      PrintWriter pw;
         
      sw = new StringWriter();
      pw = new IndentingPrintWriter (sw);
      try {
         s.write (pw, new NumberFormat(fmtStr), ref);
      }
      catch (Exception e) {
         throw new TestException (
            "exception during write: " + e);
      }
      pw.flush();
      return sw.toString();
   }

   /**
    * Reads a Scannable from a string using its scan method.
    */
   public static void scanFromString (Scannable s, Object ref, String str) {
      ReaderTokenizer rtok = new ReaderTokenizer (new StringReader (str));
      rtok.wordChars ("./$");
      try {
         s.scan (rtok, ref);
      }
      catch (Exception e) {
         throw new TestException (
            "exception during scan: " + e);
      }
   }   
}
