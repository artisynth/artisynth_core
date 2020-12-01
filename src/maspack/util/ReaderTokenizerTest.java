/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.*;

public class ReaderTokenizerTest {
   private static final int WORD = StreamTokenizer.TT_WORD;
   private static final int NUMBER = StreamTokenizer.TT_NUMBER;
   private static final int EOL = StreamTokenizer.TT_EOL;
   private static final int EOF = StreamTokenizer.TT_EOF;
   private static final int TOKEN = 0;
   private static final int QUOTE = 1;

   private class Record {
      String sval;
      int ttype;
      int lineno;

      Record (String str, int type, int line) {
         sval = str;
         ttype = type;
         lineno = line;
      }

      public String toString() {
         String s = "";
         switch (ttype) {
            case WORD: {
               s = sval;
               break;
            }
            case NUMBER: {
               s = "n=" + sval;
               break;
            }
            case TOKEN: {
               s = "'" + sval + "'";
               break;
            }
            case QUOTE: {
               s = sval;
               break;
            }
            case EOL: {
               s = "EOL";
               break;
            }
            case EOF: {
               s = "EOF";
               break;
            }
         }
         return "Token[" + s + "], line " + lineno;
      }
   };

   private String testString1 =
      " bar foobar // a bunch of commented stuff\n" + " [ foo /* some more \n"
      + " commented stuff */ wordword *bing+ \n" + "eolword\n"
      + "aaa+/**/*/**/*/$xx#stuff\n"
      + "'abc' \"foo\" '' \"/**/\" \"\\\"\" '\\A\\\t\\477' \n"
      + "\"\\29\\22A\\3333\"  'foo\n \"\6\n '\06\n '\006' \n"
      + "12.34 0.0001 -067.33 -0.00345 -.34 -45 12.5679 99.\n"
      + "0 0xABCDEF 0xdeadbeef 0x123 0x0 0x4\n";

   private Record[] testResults1 =
      new Record[] { new Record ("bar", WORD, 1),
                    new Record ("foobar", WORD, 1), new Record ("[", TOKEN, 2),
                    new Record ("foo", WORD, 2),
                    new Record ("wordword", WORD, 3),
                    new Record ("*", TOKEN, 3), new Record ("bing", WORD, 3),
                    new Record ("+", TOKEN, 3),
                    new Record ("eolword", WORD, 4),
                    new Record ("aaa", WORD, 5), new Record ("+", TOKEN, 5),
                    new Record ("*", TOKEN, 5), new Record ("*", TOKEN, 5),
                    new Record ("/", TOKEN, 5), new Record ("$", TOKEN, 5),
                    new Record ("xx", WORD, 5), new Record ("abc", QUOTE, 6),
                    new Record ("foo", QUOTE, 6), new Record ("", QUOTE, 6),
                    new Record ("/**/", QUOTE, 6), new Record ("\"", QUOTE, 6),
                    new Record ("A\t\0477", QUOTE, 6),
                    new Record ("\0029\022A\3333", QUOTE, 7),
                    new Record ("foo", QUOTE, 7),
                    new Record ("\006", QUOTE, 8),
                    new Record ("\006", QUOTE, 9),
                    new Record ("\006", QUOTE, 10),

                    new Record ("12.34", NUMBER, 11),
                    new Record ("1.0E-4", NUMBER, 11),
                    new Record ("-67.33", NUMBER, 11),
                    new Record ("-0.00345", NUMBER, 11),
                    new Record ("-0.34", NUMBER, 11),
                    new Record ("-45", NUMBER, 11),
                    new Record ("12.5679", NUMBER, 11),
                    new Record ("99.0", NUMBER, 11),

                    new Record ("0", NUMBER, 12),
                    new Record ("0xabcdef", NUMBER, 12),
                    new Record ("0xdeadbeef", NUMBER, 12),
                    new Record ("0x123", NUMBER, 12),
                    new Record ("0x0", NUMBER, 12),
                    new Record ("0x4", NUMBER, 12),

      };

   private String testString2 =
      "- -. 7.hhh 8.0yy id97.88\n"
      + "1e3 1E4 1e-4 1.5e-5 7.8E+9 -1.4536267e-12 .5e- .8E 9E+\n"
      + "inf -inf Inf -Inf i in -in infg Infinity -Infinity Infin -Infini\n"
      + "i0 in0 Infinityb -Infinityx\n";

   private Record[] testResults2 =
      new Record[] { new Record ("-", TOKEN, 1), new Record ("-", TOKEN, 1),
                    new Record (".", TOKEN, 1), new Record ("7.0", NUMBER, 1),
                    new Record ("hhh", WORD, 1), new Record ("8.0", NUMBER, 1),
                    new Record ("yy", WORD, 1), new Record ("id97", WORD, 1),
                    new Record ("0.88", NUMBER, 1),

                    new Record ("1000.0", NUMBER, 2),
                    new Record ("10000.0", NUMBER, 2),
                    new Record ("1.0E-4", NUMBER, 2),
                    new Record ("1.5E-5", NUMBER, 2),
                    new Record ("7.8E9", NUMBER, 2),
                    new Record ("-1.4536267E-12", NUMBER, 2),
                    new Record ("0.5", NUMBER, 2), new Record ("e", WORD, 2),
                    new Record ("-", TOKEN, 2), new Record ("0.8", NUMBER, 2),
                    new Record ("E", WORD, 2), new Record ("9", NUMBER, 2),
                    new Record ("E", WORD, 2), new Record ("+", TOKEN, 2),

                    new Record ("Infinity", NUMBER, 3),
                    new Record ("-Infinity", NUMBER, 3),
                    new Record ("Infinity", NUMBER, 3),
                    new Record ("-Infinity", NUMBER, 3),
                    new Record ("i", WORD, 3), new Record ("in", WORD, 3),
                    new Record ("-", TOKEN, 3), new Record ("in", WORD, 3),
                    new Record ("infg", WORD, 3),
                    new Record ("Infinity", NUMBER, 3),
                    new Record ("-Infinity", NUMBER, 3),
                    new Record ("Infin", WORD, 3), new Record ("-", TOKEN, 3),
                    new Record ("Infini", WORD, 3), new Record ("i0", WORD, 4),
                    new Record ("in0", WORD, 4),
                    new Record ("Infinityb", WORD, 4),
                    new Record ("-", TOKEN, 4),
                    new Record ("Infinityx", WORD, 4), };

   private String testString3 =
      "__FOO\n /* \n */ BAD // stuff\n # more stuff\n";

   private Record[] testResults3 =
      new Record[] { new Record ("__FOO", WORD, 1), new Record ("", EOL, 2),
                    new Record ("BAD", WORD, 3), new Record ("", EOL, 4),
                     // EOL is now skipped - see ReaderTokenizer line 1820
                     //new Record ("", EOL, 5),
                    new Record ("", EOF, 5), };

//   private String testString4 = "777E 0xdeadL 1e-8msec 0.8H 0.8 ";
//
//   private Record[] testResults4 =
//      new Record[] { new Record ("777E", NUMBER, 1),
//                    new Record ("0xdeadL", NUMBER, 1),
//                    new Record ("1.0E-8msec", NUMBER, 1),
//                    new Record ("0.8H", NUMBER, 1),
//                    new Record ("0.8", NUMBER, 1) };

   private String testString5 = "axialSprings/x-1y-1z-1";

   private Record[] testResults5 =
      new Record[] { new Record ("axialSprings/x-1y-1z-1", WORD, 1) };


   private Record[] testResults5x =
      new Record[] { new Record ("axialSprings/x", WORD, 1),
                     new Record ("-1", NUMBER, 1),
                     new Record ("y", WORD, 1),
                     new Record ("-1", NUMBER, 1),
                     new Record ("z", WORD, 1),
                     new Record ("-1", NUMBER, 1) };


   private void runTest (Record[] results, ReaderTokenizer rtok)
      throws IOException {
      for (int i = 0; i < results.length; i++) {
         rtok.nextToken();
         if (!rtok.toString().equals (results[i].toString())) {
            throw new TestException ("ReaderTokenizer failed:\n" + "Expecting "
            + results[i].toString() + "\n" + "Got " + rtok.toString());
         }
      }
   }

   public void test() throws IOException {
      ReaderTokenizer rtok =
         new ReaderTokenizer (new StringReader (testString1));
      rtok.slashSlashComments (true);
      rtok.slashStarComments (true);
      rtok.commentChar ('#');
      rtok.ordinaryChar ('/');
      runTest (testResults1, rtok);

      rtok.setReader (new StringReader (testString2));
      runTest (testResults2, rtok);

      rtok.eolIsSignificant (true);
      rtok.wordChar ('_');
      rtok.setReader (new StringReader (testString3));
      runTest (testResults3, rtok);

      rtok.parseNumbers (false);
      int dsave = rtok.getCharSetting ('-');
      rtok.wordChar ('-');
      rtok.wordChar ('/');
      rtok.setReader (new StringReader (testString5));
      runTest (testResults5, rtok);

      rtok.parseNumbers (true);
      rtok.setCharSetting ('-', dsave);
      rtok.setReader (new StringReader (testString5));
      runTest (testResults5x, rtok);

      // rtok.parseNumericExtensions(true);
      // rtok.setReader (new StringReader (testString4));
      // runTest (testResults4, rtok);

      // StreamTokenizer stok = new StreamTokenizer (
      // new StringReader (testString1));
      // stok.slashSlashComments (true);
      // stok.slashStarComments (true);
      // stok.commentChar ('#');
      // stok.ordinaryChar ('/');
      // for (int i=0; i<testResults1.length; i++)
      // { stok.nextToken();
      // if (!stok.toString().equals (testResults1[i].toString()))
      // { throw new TestException (
      // "StreamTokenizer failed:\n" +
      // "Expecting " + testResults1[i].toString() + "\n"+
      // "Got " + stok.toString());
      // }
      // }

      // test output = input for %g
      int ntrys = 100000;
      NumberFormat fmt = new NumberFormat ("%g");
      for (int i=0; i<ntrys; i++) {
         double a = RandomGenerator.nextDouble ();
         int exp = RandomGenerator.nextInt (-20, 20);
         if (exp != 0) {
            a *= Math.pow (10, exp);
         }
         String str = fmt.format (a);
         rtok.setReader (new StringReader (str));
         double b = rtok.scanNumber();
         if (a != b) {
            throw new TestException ("Scanned "+b+", expected "+a);
         }

         float fa = (float)a;
         str = fmt.format ((float)a);
         rtok.setReader (new StringReader (str));
         float fb = (float)rtok.scanNumber();
         if (fa != fb) {
            throw new TestException ("Scanned "+fb+", expected "+fa);
         }       
      }
      

   }

   public void timing() {
      int cnt = 100000;
      StringBuffer sbuf = new StringBuffer (cnt * 80);
      for (int i = 0; i < cnt; i++) {
         sbuf.append (" foo bar batman /* hhh hh */ * jangle= // stuf\n");
         sbuf.append (" 12.456 8.999 0.0567 -7.888 0.9\n");
      }
      long t0, t1;

      System.out.println ("" + (2 * cnt) + " lines:");

      for (int i = 0; i < 2; i++) {
         ReaderTokenizer rtok =
            new ReaderTokenizer (new StringReader (sbuf.toString()));
         rtok.slashSlashComments (true);
         rtok.slashStarComments (true);

         t0 = System.currentTimeMillis();
         try {
            while (rtok.nextToken() != ReaderTokenizer.TT_EOF)
               ;
         }
         catch (Exception e) {
            e.printStackTrace();
         }
         t1 = System.currentTimeMillis();
         System.out.println ("ReaderTokenizer, " + (t1 - t0) + " msec");

         StreamTokenizer stok =
            new StreamTokenizer (new StringReader (sbuf.toString()));
         stok.slashSlashComments (true);
         stok.slashStarComments (true);

         t0 = System.currentTimeMillis();
         try {
            while (stok.nextToken() != StreamTokenizer.TT_EOF)
               ;
         }
         catch (Exception e) {
            e.printStackTrace();
         }
         t1 = System.currentTimeMillis();
         System.out.println ("StreamTokenizer, " + (t1 - t0) + " msec");
      }

   }

   public static void main (String[] args) {
      boolean doTiming = false;

      RandomGenerator.setSeed (0x1234);
      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.err.println ("Usage: java ReaderTokenizerTest [-timing]");
            System.exit (1);
         }
      }
      ReaderTokenizerTest tester = new ReaderTokenizerTest();
      try {
         if (doTiming) {
            tester.timing();
         }
         else {
            tester.test();
         }
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
