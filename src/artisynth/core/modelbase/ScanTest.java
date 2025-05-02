/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import artisynth.core.util.*;
import artisynth.core.modelbase.ScanWriteUtils.ClassInfo;
import maspack.util.*;

import java.io.*;
import java.util.*;

/**
 * Provides a method to test the scan/write methods of a Scannable object
 */
public class ScanTest {

   public static boolean showOutput = false;

   static String getClassTag (PostScannable mc) {
      String classTag;
      if (mc instanceof ParameterizedClass &&
          ((ParameterizedClass)mc).hasParameterizedType()) {
         classTag = ScanWriteUtils.getParameterizedClassTag (
            mc, ((ParameterizedClass)mc).getParameterType());
      }
      else {
         classTag = ScanWriteUtils.getClassTag (mc);
      }
      return classTag;
   }

   /**
    * Tests the scan and write methods of an object by writing the object,
    * scanning it, writing it again, and making sure that the second write
    * output equals the first.
    */
   public static PostScannable testScanAndWrite (
      PostScannable mc, CompositeComponent ref, String fmtStr) {

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
         String classTag = getClassTag (mc);
         pw.print (classTag+" ");
         mc.write (pw, fmt, ref);
      }
      catch (Exception e) {
         throw new TestException (
            "exception during first write: " + e);
      }
      pw.flush();
      String str1 = sw.toString();
      if (showOutput) {
         System.out.println (str1);
      }
      
      ReaderTokenizer rtok = new ReaderTokenizer (new StringReader (str1));
      Deque<ScanToken> tokens = new ArrayDeque<ScanToken>();
      rtok.wordChars ("./$");
      PostScannable nc = null;
      // create a new model component
      try {
         ClassInfo<?> classInfo =
            ScanWriteUtils.scanClassInfo (rtok, PostScannable.class);
         nc = (PostScannable)ScanWriteUtils.newComponent (
            rtok, classInfo, /*warnOnly=*/false);      
         // if (mc instanceof ParameterizedClass) {
         //    Class<?> ptype = ((ParameterizedClass)mc).getParameterType();
         //    nc = mc.getClass().getDeclaredConstructor(
         //       ptype.getClass()).newInstance(ptype);
         // }
         // else {
         //    nc = mc.getClass().newInstance();
         // }
      }
      catch (Exception e) {
         System.out.println (str1);
         throw new TestException (
            "unable to create new instance of "+mc.getClass(), e);
      }
      try {
         nc.scan (rtok, tokens);
         nc.postscan (tokens, ref);
      }
      catch (Exception e) {
         e.printStackTrace();
         throw new TestException (
            "exception during scan: " + e);
      }
      sw = new StringWriter();
      pw = new IndentingPrintWriter (sw);
      try {
         String classTag = getClassTag (nc);
         pw.print (classTag+" ");
         nc.write (pw, fmt, ref);
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
      return nc;
   }
}
