package maspack.geometry.io;

import java.io.IOException;
import java.io.StringReader;

import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.util.TestException;
import maspack.util.TestSupport;
import maspack.util.ReaderTokenizer;

/**
 * Tests the WavefrontReader class
 */
public class WavefrontReaderTest {
   void test (String str, WavefrontReader check) throws IOException {
      WavefrontReader wfr = new WavefrontReader(new StringReader (str));
      wfr.parse ();
      wfr.close ();
      if (!wfr.equals (check)) {
         System.out.println (str);
         System.out.println ("Expected:\n" + check.toString());
         System.out.println ("Got:\n" + wfr.toString());
         throw new TestException ("check not equal to result");
      }
   }

   void testError (String str, Exception eExpected) {
      WavefrontReader wfr = new WavefrontReader(new StringReader (str));
      Exception eActual = null;
      try {
         wfr.parse ();
         wfr.close ();
      }
      catch (Exception e) {
         eActual = e;
      }
      TestSupport.checkExceptions (eActual, eExpected);
   }

   public static void main (String[] args) {
      WavefrontReader check = new WavefrontReader((ReaderTokenizer)null);
      WavefrontReaderTest tester = new WavefrontReaderTest();

      WavefrontReader.Curve testCurve = new WavefrontReader.Curve();
      WavefrontReader.Surface testSurface = new WavefrontReader.Surface();

      try {
         check.clear();
         check.set (
            new Vector4d[] { new Vector4d (0, 2, 2, 1),
                             new Vector4d (0, 0, 2, 1),
                             new Vector4d (2, 0, 2, 1),
                             new Vector4d (2, 2, 2, 1),
                             new Vector4d (0, 2, 0, 3) }, null, null,
                             new WavefrontReader.Face[] {
                                new WavefrontReader.Face (
                                   new int[] { 0, 1, 2, 3 }, null,
                                   null, 6),
                                new WavefrontReader.Face (
                                   new int[] { 2, 3, 4 }, null,
                                   null, 7) }, null, null);
         tester.test ("v 0.0 2.0 2.0 \n" + "v 0.0 0.0 2.0 \n" +
                      "v 2.0 0.0 2.0 \n" + "v 2.0 2.0 2.0 \n" +
                      "v 0.0 2.0 0.0 3 \n" + "f 1 2 3 4 \n" + "f 3 4 5 \n",
                      check);

         check.clear();
         check.set (
            new Vector4d[] { new Vector4d (0, 2, 2, 1),
                            new Vector4d (2, 0, 2, 1),
                            new Vector4d (2, 2, 2, 1),
                            new Vector4d (0, 2, 0, 3),
                            new Vector4d (2, 2, 0, 2),
                            new Vector4d (3, 2, 0, 1) },
            new Vector3d[] { new Vector3d (0.1, 0.2, 0.3),
                            new Vector3d (0, 0, 0.1), new Vector3d (2, 2, 2) },
            new Vector3d[] { new Vector3d (1, 2, 3), new Vector3d (4, 5, 6),
                            new Vector3d (7, 8, 9), },
            new WavefrontReader.Face[] {
                                        new WavefrontReader.Face (
                                           new int[] { 0, 1, 2, 3 },
                                           new int[] { 2, 1, 1, 0 }, null, 13),
                                        new WavefrontReader.Face (
                                           new int[] { 0, 1, 2, 3 }, null,
                                           new int[] { 2, 1, 1, 0 }, 14),
                                        new WavefrontReader.Face (
                                           new int[] { 0, 1, 2, 3 },
                                           new int[] { 1, 2, 2, 0 },
                                           new int[] { 2, 1, 1, 0 }, 15),
                                        new WavefrontReader.Face (
                                           new int[] { 3, 4, 5 },
                                           new int[] { 1, 2, 2 },
                                           new int[] { 2, 1, 1 }, 16),
                                        new WavefrontReader.Face (
                                           new int[] { 3, 4, 5 },
                                           new int[] { 1, 2, 2 },
                                           new int[] { 2, 1, 1 }, 17),
                                        new WavefrontReader.Face (
                                           new int[] { 3, 4, 5 },
                                           new int[] { 1, 2, 2 },
                                           new int[] { 2, 1, 1 }, 18) }, null,
            null);
         tester.test (
            "v 0.0 2.0 2.0 \n" + "vn 1 2 3 \n" + "v 2.0 0.0 2.0 \n" +
            "vt 0.1 0.2 0.3 \n" + "vn 4 5 6 \n" + "vn 7 8 9 \n" +
            "vt 0 0 0.1 \n" + "v 2.0 2.0 2.0 \n"+"v 0.0 2.0 0.0 3 \n" +
            "vt 2 2 2 \n" + "v 2.0 2.0 0.0 2 \n" + "v 3.0 2.0 0.0 \n" +
            "f 1/3 2/2 3/2 4/1 \n" + "f 1//3 2//2 3//2 4//1 \n" +
            "f 1/2/3 2/3/2 3/3/2 4/1/1\n" +
            "f -3/2/-1 -2/3/-2 -1/3/-2\n" +
            "f -3/-2/-1 -2/-1/-2 -1/-1/-2 \n" + "f -3/-2/-1 \\ \n" +
            "  -2/-1/-2 \\ \n" + "  -1/-1/-2 \n", check);

         tester.testError (
            "v 0.0 2.0 2.0 \n" + "v 0.0 2.0 2.0 foo \n",
            new IOException ("vertex w coordinate expected, line 2"));

         tester.testError (
            "v 0.0 2.0 2.0 \n" + "v 0.0 2.0 \n",
            new IOException ("vertex coordinate expected, line 2"));

         tester.testError (
            "v 0.0 2.0 2.0 \n" + "vn 0.0 2.0 1 \n" + "vn 0.0 \n",
            new IOException ("normal coordinate expected, line 3"));

         tester.testError (
            "v 0.0 2.0 2.0 \n" + "vt 0.0 \n" + "vt \n",
            new IOException ("texture vertex u coordinate expected, line 3"));

         tester.testError (
            "v 0.0 2.0 2.0 \n" + "vt 0.0 \n" + "vt 0.0 3 \n" +
            "vt 1 foo \n",
            new IOException ("texture vertex v coordinate expected, line 4"));

         tester.testError (
            "v 0.0 2.0 2.0 \n" + "vt 0.0 \n" + "vt 1 2 foo \n",
            new IOException ("texture vertex w coordinate expected, line 3"));

         tester.testError (
            "v 1 2 3 \n" + "deg 3 3\n" + "deg 4\n" + "deg foo\n",
            new IOException ("u curve degree expected, line 4"));

         tester.testError (
            "v 1 2 \\ 3 \n",
            new IOException (
               "Line continuation token '\\' not at end of line, line 1"));

         tester.testError (
            "v 1 2 3 \n" + "deg 3 3\n" + "deg 4\n" +
            "deg 2 foo\n",
            new IOException ("v curve degree expected, line 4"));

         tester.testError (
            "deg 3 3.2\n",
            new IOException ("v curve degree expected, line 1"));

         tester.testError (
            "curv 1\n",
            new IOException ("u start and end values expected, line 1"));

         tester.testError (
            "curv foo\n",
            new IOException ("u start and end values expected, line 1"));

         tester.testError (
            "deg 2\n" + "curv 1 2 1 2 3 \n" + "curv 1 2 1\n",
            new IOException (
               "unexpected keyword 'curv' between curv/surf and end, line 3"));

         tester.testError (
            "deg 2\n" + "curv 1 2 1 2 3 \n" + "end \n" +
            "surf 1 2 1 b\n",
            new IOException (
               "u and v start and end values expected, line 4"));

         tester.testError (
            "deg 2\n" + "curv 1 2 1 2 3 \n" + "parm u 1 2 boo\n" +
            "end \n",
            new IOException ("knot point expected, line 3"));

         tester.testError (
            "deg 2\n" + "v 1 2 3\n" + "v 3 4 5\n" + "v 6 7 8 \n" +
            "curv 0 1 -2 -3 -1\n" + "end \n" + "curv 0 1 -2.4 -3 -1\n",
            new IOException ("control point index expected, line 7"));

         tester.testError (
            "deg 2\n" + "v 1 2 3\n" + "v 3 4 5\n" + "v 6 7 8 \n" +
            "curv 0 1 -2 -4 -1\n" + "end \n",
            new IOException ("relative index out of range, line 5"));

         tester.testError (
            "v 1 2 3\n" + "v 3 4 5\n" + "v 6 7 8 \n" +
            "vn 0.1 0.2 0.3\n" + "vn 0.4 0.5 0.6\n" + "vn 0.7 0.8 0.9\n" +
            "vt 1\n" + "vt 2\n" + "vt 3\n" + "f 1/-1/-1 2/-2/-2 3/-3/-3\n" +
            "f 1/-1/-1 2/-4/-2 3/-3/-3\n",
            new IOException ("relative index out of range, line 11"));

         tester.testError (
            "v 1 2 3\n" + "v 3 4 5\n" + "v 6 7 8 \n" +
            "vn 0.1 0.2 0.3\n" + "vn 0.4 0.5 0.6\n" + "vn 0.7 0.8 0.9\n" +
            "vt 1\n" + "vt 2\n" + "vt 3\n" + "f 1/-1/-1 2/-2/-2 3/-3/-3\n" +
            "f 1/-1/-1 2/-2/-2 3/-3/-5\n",
            new IOException ("relative index out of range, line 11"));

         tester.testError (
            "f 1 2/2\n",
            new IOException ("unexpected '/', line 1"));

         tester.testError (
            "f 1//2 2/2/3\n",
            new IOException ("unexpected texture index, line 1"));

         tester.testError (
            "f 1/2 2/2/3\n",
            new IOException ("unexpected '/', line 1"));

         tester.testError (
            "f 1/2/3 2/2/ \n",
            new IOException ("normal index expected, line 1"));

         tester.testError (
            "f 1/2/3 2/2/bb \n",
            new IOException ("normal index expected, line 1"));

         tester.testError (
            "deg 2 2\n" + "curv 0 1 1 1.2 3\n",
            new IOException ("control point index expected, line 2"));

         tester.testError (
            "deg 2 2\n" + "curv 0 1 1 bar 3\n",
            new IOException ("control point index expected, line 2"));

         tester.testError (
            "deg 2 2\n" + "curv 0 1 1 2 3\n" + "parm y 1 2 3\n",
            new IOException ("u keyword expected, line 3"));

         tester.testError (
            "deg 2 2\n" + "curv 0 1 1 2 3\n" + "parm v 1 2 3\n",
            new IOException (
               "v keyword inappropriate for curve construct, line 3"));

         tester.testError (
            "deg 2 2\n" + "surf 0 1 1 2 3\n" + "parm y 1 2 3\n",
            new IOException ("u or v keyword expected, line 3"));

         tester.testError (
            "curv 0.1 0.9 1 2 3 4 5\n",
            new IOException ("degree not specified for curv, line 1"));

         tester.testError (
            "surf 0 1 2 3 1 1\n",
            new IOException ("u degree not specified for surf, line 1"));

         tester.testError (
            "deg 2\n" + "surf 0 1 2 3 1 1\n",
            new IOException ("v degree not specified for surf, line 2"));

         check.clear();
         testCurve.set (
            WavefrontReader.BSPLINE, true, 2, false, new double[] { 1.1, 1.2,
                                                                   2.2 },
            new int[] { 0, 1, 2, 3, 4 }, 0.1, 0.9, 2);
         check.set (
            null, null, null, null, new WavefrontReader.Curve[] { testCurve },
            null);
         tester.test ("deg 2 2\n" + "curv 0.1 0.9 1 2 3 4 5\n" +
                      "parm u 1.1 1.2 2.2\n" + "end\n", check);

         check.clear();
         testCurve.set (
            WavefrontReader.BSPLINE, true, 3, true, new double[] {},
            new int[] { 1, 0, 2, 3, 4 }, 0.1, 0.9, 4);
         check.set (
            new Vector4d[] { new Vector4d (0, 2, 2, 1),
                            new Vector4d (2, 0, 2, 1), }, null, null, null,
            new WavefrontReader.Curve[] { testCurve }, null);
         tester.test ("v 0 2 2 1\n" + "v 2 0 2\n" + "deg 3 2\n" +
                      "curv 0.1 0.9 -1 -2 3 4 5\n" + "parm u closed\n" +
                      "end\n", check);

         check.clear();
         testSurface.setGen (
            WavefrontReader.BSPLINE, true, new int[] { 0, 1, 2, 3, 4 }, 2);
         testSurface.setu (4, false, new double[] { 1.1, 1.2, 2.2 }, 0.1, 0.9);
         testSurface.setv (3, false, new double[] { 3, 4, 5 }, 1.1, 1.9);
         check.set (
            null, null, null, null, null,
            new WavefrontReader.Surface[] { testSurface });
         tester.test ("deg 4 3\n" + "surf 0.1 0.9 1.1 1.9 1 2 3 4 5\n" +
                      "parm u 1.1 1.2 2.2\n" + "parm v 3 4 5 \n" + 
                      "end\n", check);

         check.clear();
         testSurface.setGen (
            WavefrontReader.BSPLINE, true, new int[] { 0, 1, 1, 0 }, 5);
         testSurface.setu (4, true, new double[] { 1.1, 1.2, 2.2 }, 0.1, 0.9);
         testSurface.setv (3, true, new double[] {}, 1.1, 1.9);
         check.set (
            new Vector4d[] { new Vector4d (0, 2, 2, 1),
                            new Vector4d (2, 0, 2, 1), }, null, null, null,
            null, new WavefrontReader.Surface[] { testSurface });
         tester.test ("v 0 2 \\ \n" + "  2 1\n" + "v 2 0 2\n" + "deg 4 3\n" +
                      "surf 0.1 \\ \n" + "    0.9 1.1 1.9 1 \\ \n" +
                      "    2 -1 -2\n" + "parm u closed 1.1 \\ \n" +
                      "   1.2 2.2\n" + "parm \\ \n" +
                      "  v closed \n" + "end\n", check);

      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }

      System.out.println ("\nPassed\n");

   }
}
