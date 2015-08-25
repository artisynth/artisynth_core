/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import artisynth.core.util.*;
import maspack.util.*;

import java.io.*;
import java.util.*;

/**
 * Provides a method to test the scan/write methods of a Scannable object
 */
public class ScanTest {

   /**
    * Tests the scan and write methods of an object by writing the object,
    * scanning it, writing it again, and making sure that the second write
    * output equals the first.
    */
   public static void testScanAndWrite (
      ModelComponent s, CompositeComponent ref, String fmtStr) {

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
         s.write (pw, fmt, ref);
      }
      catch (Exception e) {
         throw new TestException (
            "exception during first write: " + e);
      }
      pw.flush();
      String str1 = sw.toString();

      ReaderTokenizer rtok = new ReaderTokenizer (new StringReader (str1));
      Deque<ScanToken> tokens = new ArrayDeque<ScanToken>();
      rtok.wordChars ("./$");
      try {
         s.scan (rtok, tokens);
         s.postscan (tokens, ref);
      }
      catch (Exception e) {
         throw new TestException (
            "exception during scan: " + e);
      }
      sw = new StringWriter();
      pw = new IndentingPrintWriter (sw);
      try {
         s.write (pw, fmt, ref);
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
   }
}
