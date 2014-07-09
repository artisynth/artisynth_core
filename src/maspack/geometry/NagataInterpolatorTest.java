/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;

public class NagataInterpolatorTest {

   NagataInterpolator myInterp = new NagataInterpolator();

   private int INSIDE = NagataInterpolator.INSIDE;

   private int VERTEX_1 = NagataInterpolator.VERTEX_1;
   private int VERTEX_2 = NagataInterpolator.VERTEX_2;
   private int VERTEX_3 = NagataInterpolator.VERTEX_3;

   private int EDGE_1 = NagataInterpolator.EDGE_1;
   private int EDGE_2 = NagataInterpolator.EDGE_2;
   private int EDGE_3 = NagataInterpolator.EDGE_3;

   private double TOL = 1e-14;

   private void testClipCoords (
      double x, double y, double xchk, double ychk, int codeChk) {

      Vector2d svec = new Vector2d (x, y);
      int code = myInterp.clipCoords (svec);
      if (code != codeChk) {
         throw new TestException (
            "clipCoords("+x+","+y+"): code="+code+", expected "+codeChk);
      }
      if (Math.abs(svec.x-xchk) > TOL || Math.abs(svec.y-ychk) > TOL) {
         throw new TestException (
            "clipCoords("+x+","+y+"): new s=("+svec.x+","+svec.y+
            "), expected ("+xchk+","+ychk+")");
      }
      if (Math.abs(svec.y-ychk) > TOL) {
         throw new TestException (
            "clipCoords("+x+","+y+"): new y="+svec.y+", expected "+ychk);
      }
   }

   private void testGradientIsAdmissible (
      double dx, double dy, int code, boolean chk) {

      Vector2d grad = new Vector2d (dx, dy);
      boolean status = myInterp.gradientIsAdmissible (grad, code);
      if (status != chk) {
         throw new TestException (
            "gradientIsAdmissible("+dx+","+dy+","+code+")="+status+
            ", expecting "+chk);
      }
   }

   public void testClipCoords () {
      testClipCoords (0, 0, 0, 0, VERTEX_1);
      testClipCoords (-0.6, .6, 0, 0, VERTEX_1);
      testClipCoords (-0.599999, .600001, 0.000001, 0.000001, EDGE_3);
      testClipCoords (-0.6, .599999, 0, 0, VERTEX_1);
      testClipCoords (0, -.6, 0, 0, VERTEX_1);
      testClipCoords (0.000001, -.6, 0.000001, 0, EDGE_1);
      testClipCoords (-0.000001, -.6, 0, 0, VERTEX_1);
      testClipCoords (-.6, -.6, 0, 0, VERTEX_1);

      testClipCoords (1, 0, 1, 0, VERTEX_2);
      testClipCoords (2, 0, 1, 0, VERTEX_2);
      testClipCoords (2, 0.000001, 1, 0.000001, EDGE_2);
      testClipCoords (2, -0.000001, 1, 0, VERTEX_2);
      testClipCoords (1, -.6, 1, 0, VERTEX_2);
      testClipCoords (0.99999, -.6, 0.99999, 0, EDGE_1);
      testClipCoords (1.000001, -.6, 1, 0, VERTEX_2);
      testClipCoords (1.5, -.5, 1, 0, VERTEX_2);

      testClipCoords (1, 1, 1, 1, VERTEX_3);
      testClipCoords (1.6, 1, 1, 1, VERTEX_3);
      testClipCoords (1.6, 1.000001, 1, 1, VERTEX_3);
      testClipCoords (1.6, 0.999999, 1, 0.999999, EDGE_2);
      testClipCoords (1.6, 1.6, 1, 1, VERTEX_3);
      testClipCoords (0.6, 1.4, 1, 1, VERTEX_3);
      testClipCoords (0.6, 1.400001, 1, 1, VERTEX_3);
      testClipCoords (0.599999, 1.399999, 0.999999, 0.999999, EDGE_3);

      testClipCoords (0.1, -0.6, 0.1, 0, EDGE_1);
      testClipCoords (0.1, 0, 0.1, 0, EDGE_1);
      testClipCoords (1e-8, 0, 1e-8, 0, EDGE_1);
      testClipCoords (1e-8, -1e-8, 1e-8, 0, EDGE_1);
      testClipCoords (1e-8, 0.5e-8, 1e-8, 0.5e-8, INSIDE);

      testClipCoords (1.0, 0.3, 1.0, 0.3, EDGE_2);
      testClipCoords (0.999999, 0.3, 0.999999, 0.3, INSIDE);
      testClipCoords (1.000001, 0.3, 1.0, 0.3, EDGE_2);

      testClipCoords (0.3, 0.3, 0.3, 0.3, EDGE_3);
      testClipCoords (0.299999, 0.300001, 0.3, 0.3, EDGE_3);
      testClipCoords (0.3, 0.299999, 0.3, 0.299999, INSIDE);
      testClipCoords (-0.6, .6, 0, 0, VERTEX_1);

    }

   private void testGradientIsAdmissible () {

      testGradientIsAdmissible (-0.1, 0, VERTEX_1, false);
      testGradientIsAdmissible (-0.1, -0.000001, VERTEX_1, false);
      testGradientIsAdmissible (-0.1, -0.1, VERTEX_1, false);
      testGradientIsAdmissible (-0.1, -0.099999, VERTEX_1, true);
      testGradientIsAdmissible (0, -0.1, VERTEX_1, false);
      testGradientIsAdmissible (0.1, -0.1, VERTEX_1, false);
      testGradientIsAdmissible (-0.1, -0.05, VERTEX_1, true);

      testGradientIsAdmissible (0, -0.1, VERTEX_2, false);
      testGradientIsAdmissible (0.00001, -0.1, VERTEX_2, true);
      testGradientIsAdmissible (0.1, 0, VERTEX_2, false);
      testGradientIsAdmissible (0.1, -0.000001, VERTEX_2, true);
      testGradientIsAdmissible (0.1, -0.1, VERTEX_2, true);
      testGradientIsAdmissible (-0.1, 0.1, VERTEX_2, false);

      testGradientIsAdmissible (0, 0.1, VERTEX_3, false);
      testGradientIsAdmissible (0.000001, 0.1, VERTEX_3, true);
      testGradientIsAdmissible (0.1, 0.1, VERTEX_3, false);
      testGradientIsAdmissible (0.099999, 0.1, VERTEX_3, false);
      testGradientIsAdmissible (0.05, 0.1, VERTEX_3, true);
      testGradientIsAdmissible (-0.05, -0.1, VERTEX_3, false);

      testGradientIsAdmissible (0.0, -1.0, EDGE_1, true);
      testGradientIsAdmissible (0.0, 1.0, EDGE_1, false);

      testGradientIsAdmissible (1.0, 0.0, EDGE_2, true);
      testGradientIsAdmissible (-1.0, 0.0, EDGE_2, false);

      testGradientIsAdmissible (-1.0, 1.0, EDGE_3, true);
      testGradientIsAdmissible (1.0, -1.0, EDGE_3, false);
      testGradientIsAdmissible (-1.0, -1.0, EDGE_3, false);
      testGradientIsAdmissible (-1.00001, -1.0, EDGE_3, true);
      testGradientIsAdmissible (1.0, 1.00001, EDGE_3, true);

   }

   public void test() {
      testClipCoords();
   }

   public static void main (String[] args) {

      NagataInterpolatorTest tester = new NagataInterpolatorTest();

      try {
         tester.test ();
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1); 
      }
      
      System.out.println ("\nPassed\n");
   }

}