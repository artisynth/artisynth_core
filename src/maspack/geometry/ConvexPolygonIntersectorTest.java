/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;
import java.io.*;

public class ConvexPolygonIntersectorTest {
   static public final double EPS = 1e-7;

   static public final double DOUBLE_PREC = 2.220446049250313e-16;

   static public final double TEST_TOL = 1e-10;

   private ConvexPolygon2d handlerPoly = new ConvexPolygon2d();

   private class IntersectHandler implements ConvexPolygonIntersector.Listener {
      Point2d chk = new Point2d();

      public void output (
         Point2d pnt, Vertex2d p, Vertex2d q, double plam, double qlam) {
         handlerPoly.appendVertex (new Vertex2d (pnt));
         if (p != null) {
            chk.interpolate (p.prev.pnt, plam, p.pnt);
            if (!chk.epsilonEquals (pnt, TEST_TOL)) {
               System.out.println ("Bad interpolation info for p:");
               System.exit (1);
            }
         }
         if (q != null) {
            chk.interpolate (q.prev.pnt, qlam, q.pnt);
            if (!chk.epsilonEquals (pnt, TEST_TOL)) {
               System.out.println ("Bad interpolation info for q:");
               System.exit (1);
            }
         }
      }
   }

   static private class LineIsec {
      double[] lam = new double[2];
      int n;

      boolean epsilonEquals (LineIsec isec, double eps) {
         if (n != isec.n) {
            return false;
         }
         for (int i = 0; i < n; i++) {
            if (Math.abs (lam[i] - isec.lam[i]) > eps) {
               return false;
            }
         }
         return true;
      }

      public String toString() {
         String s = "" + n + ": ";
         for (int i = 0; i < n; i++) {
            if (lam[i] == Double.POSITIVE_INFINITY) {
               s += "+I";
            }
            else if (lam[i] == Double.NEGATIVE_INFINITY) {
               s += "-I";
            }
            else {
               s += lam[i];
            }
            if (i < n - 1) {
               s += " ";
            }
         }
         return s;
      }

      void scan (ReaderTokenizer rtok) throws IOException {
         double nval = rtok.scanNumber();
         if (nval != 0 && nval != 1 && nval != 2) {
            throw new IOException ("'0:', '1:', or '2:' expected");
         }
         n = (int)nval;
         rtok.nextToken();
         if (rtok.ttype != ':') {
            throw new IOException ("':' expected");
         }
         for (int i = 0; i < n; i++) {
            rtok.nextToken();
            if (rtok.tokenIsNumber()) {
               lam[i] = rtok.nval;
            }
            else if (rtok.ttype == rtok.TT_WORD && rtok.sval.equals ("-I")) {
               lam[i] = Double.NEGATIVE_INFINITY;
            }
            else if (rtok.ttype == rtok.TT_WORD && rtok.sval.equals ("+I")) {
               lam[i] = Double.POSITIVE_INFINITY;
            }
            else {
               throw new IOException ("number or +I or -I expected");
            }
         }
      }
   }

   public static final int DO_QUIT = 0;
   public static final int DO_ADD = 1;
   public static final int DO_ISECT = 2;
   public static final int DO_RESET = 3;
   public static final int DO_INTERSECT = 4;

   static ConvexPolygon2d workPoly = new ConvexPolygon2d();
   static LineIsec workIsec = new LineIsec();
   static ConvexPolygon2d resultStub = new ConvexPolygon2d();
   static ConvexPolygon2d xformTestPoly = new ConvexPolygon2d();

   ConvexPolygonIntersector intersector;

   int cmd;
   int line;
   // Line2d hp;
   Point2d lineQ = new Point2d();
   Vector2d lineU = new Vector2d();
   ConvexPolygon2d testPoly;
   ConvexPolygon2d testPolyRef;
   LineIsec testIsec;
   ConvexPolygon2d poly1;
   ConvexPolygon2d poly2;

   ConvexPolygonIntersectorTest() {
      testPoly = new ConvexPolygon2d();
      testIsec = new LineIsec();
      poly1 = new ConvexPolygon2d();
      poly2 = new ConvexPolygon2d();
      intersector = new ConvexPolygonIntersector();
   }

   ConvexPolygon2d getNamedPoly (String name) {
      if (name.equals ("poly1")) {
         return (poly1);
      }
      else if (name.equals ("poly2")) {
         return (poly2);
      }
      else if (name.equals ("res")) {
         return (resultStub);
      }
      else {
         return (null);
      }
   }

   String getPolyName (ConvexPolygon2d poly) {
      if (poly == poly1) {
         return ("poly1");
      }
      else if (poly == poly2) {
         return ("poly2");
      }
      else if (poly != null) {
         return ("res");
      }
      else {
         return (null);
      }
   }

   void readTestPoly (ReaderTokenizer rtok) throws IOException {
      rtok.nextToken();
      if (rtok.ttype != rtok.TT_WORD) {
         throw new IOException ("polygon name expected");
      }
      testPolyRef = getNamedPoly (rtok.sval);
      if (testPolyRef == null) {
         throw new IOException ("bogus polygon name " + rtok.sval);
      }
      if (testPolyRef == resultStub) {
         testPoly.scan (rtok);
      }
      else {
         testPoly.set (testPolyRef);
      }
   }

   public void scanRecord (ReaderTokenizer rtok) throws IOException {
      rtok.nextToken();
      line = rtok.lineno();
      if (rtok.ttype == ReaderTokenizer.TT_EOF) {
         cmd = DO_QUIT;
         return;
      }
      else if (rtok.ttype != ReaderTokenizer.TT_WORD) {
         throw new IOException ("Command expected");
      }
      if (rtok.sval.equals ("ADD")) {
         cmd = DO_ADD;
         lineQ.scan (rtok);
         lineU.scan (rtok);
         testPoly.scan (rtok);
      }
      else if (rtok.sval.equals ("INTERSECT")) {
         cmd = DO_INTERSECT;
         poly2.scan (rtok);
         readTestPoly (rtok);
      }
      else if (rtok.sval.equals ("ISECT")) {
         cmd = DO_ISECT;
         lineQ.scan (rtok);
         lineU.scan (rtok);
         testIsec.scan (rtok);
      }
      else if (rtok.sval.equals ("RESET")) {
         workPoly.scan (rtok);
         poly1.set (workPoly);
         cmd = DO_RESET;
      }
      else {
         throw new IOException ("Unknown command " + rtok.sval);
      }
   }

   public boolean test (boolean check) {
      switch (cmd) {
         case DO_ADD: {
            // if (!workPoly.isConsistent())
            // { System.out.println (
            // "Error: ADD, near line " + line + ": bogus input");
            // System.out.println ("Got: " + workPoly.toString("%9.4f"));
            // return false;
            // }
            // workPoly.intersect (hp);
            // if (check)
            // {
            // if (!workPoly.isConsistent())
            // { System.out.println (
            // "Error ADD, near line " + line + ": bogus result");
            // System.out.println (
            // "Wanted: " + testPoly.toString("%9.4f"));
            // System.out.println (
            // " Got: " + workPoly.toString("%9.4f"));
            // return false;
            // }
            // if (!workPoly.epsilonEquals (testPoly, TEST_TOL))
            // { System.out.println ("Error ADD, near line "+line + ":");
            // System.out.println (
            // "Wanted: " + testPoly.toString("%9.4f"));
            // System.out.println (
            // " Got: " + workPoly.toString("%9.4f"));
            // return false;
            // }
            // }
            // else
            // { System.out.println (workPoly.toString("%9.4f"));
            // }
            break;
         }
         case DO_ISECT: {
            workIsec.n =
               intersector.intersectLine (workIsec.lam, workPoly, lineQ, lineU);
            if (check) {
               if (!workIsec.epsilonEquals (testIsec, EPS)) {
                  System.out.println ("Error in ISECT near line " + line + ":");
                  System.out.println ("Wanted: " + testIsec);
                  System.out.println ("   Got: " + workIsec);
                  return false;
               }
            }
            else {
               System.out.println (workIsec);
            }
            break;
         }
         case DO_INTERSECT: {
            ConvexPolygon2d resultPoly;
            if (check) {
               ConvexPolygon2d poly1Save = new ConvexPolygon2d();
               ConvexPolygon2d poly2Save = new ConvexPolygon2d();
               ConvexPolygon2d xformTestPoly = new ConvexPolygon2d();
               double ang = Math.PI / 3;

               poly1Save.set (poly1);
               poly2Save.set (poly2);

               if (!poly1.isConsistent()) {
                  System.out.println (
                     "Error: INTERSECT, near line " + line + ": bogus input 1");
                  System.out.println ("   Got: " + poly1.toString ("%9.4f"));
                  return false;
               }
               if (!poly2.isConsistent()) {
                  System.out.println (
                     "Error: INTERSECT, near line " + line + ": bogus input 2");
                  System.out.println ("   Got: " + poly2.toString ("%9.4f"));
                  return false;
               }

               // #if 1
               IntersectHandler handler = new IntersectHandler();
               intersector.addListener (handler);

               for (int i = 0; i < poly1.numVertices(); i++) {
                  if (i > 0)
                     poly1.shiftVertices (1);
                  for (int j = 0; j < poly2.numVertices(); j++) {
                     if (j > 0)
                        poly2.shiftVertices (1);

                     handlerPoly.clear();
                     resultPoly = intersector.intersect (poly1, poly2);
                     if (!resultPoly.epsilonEquals (testPoly, TEST_TOL)) {
                        System.out.println (
                           "Error in INTERSECT (" + i + "," + j +
                           ") near line " + line + ":");
                        System.out.println (
                           "Wanted: " + getPolyName (testPolyRef));
                        System.out.println (testPoly.toString ("%9.4f"));
                        System.out.println (
                           "   Got: " + getPolyName (resultPoly));
                        System.out.println (resultPoly.toString ("%9.4f"));
                        System.out.println (
                           "poly1\n" + poly1.toString ("%9.4f"));
                        System.out.println (
                           "poly2\n" + poly2.toString ("%9.4f"));
                        return false;
                     }
                     if (!handlerPoly.equals (resultPoly)) {
                        System.out.println ("INTERSECT (" + i + "," + j + "):");
                        System.out.println (
                           "resultPoly and HandlerPoly are different");
                        System.out.println (
                           "resultPoly:\n" + resultPoly.toString ("%9.4f"));
                        System.out.println (
                           "handlerPoly:\n" + handlerPoly.toString ("%9.4f"));
                     }

                     if (!resultPoly.isConsistent()) {
                        System.out.println (
                           "Error: INTERSECT, near line " + line +
                           ": bogus result");
                        return false;
                     }
                  }
               }

               // poly1.intersect (poly2);
               // if (!poly1.epsilonEquals (testPoly, TEST_TOL))
               // { System.out.println (
               // "Error in self INTERSECT near line " + line + ":");
               // System.out.println (
               // "Wanted: " + getPolyName(testPolyRef));
               // System.out.println (
               // " Got: " + getPolyName(poly1));
               // System.out.println (poly1.toString("%9.4f"));
               // return false;
               // }
               RigidTransform2d X = new RigidTransform2d (0, 0, ang);

               xformTestPoly.set (testPoly);
               xformTestPoly.transform (X);

               poly1.set (poly1Save);
               poly1.transform (X);

               poly2.set (poly2Save);
               poly2.transform (X);

               handlerPoly.clear();
               resultPoly = intersector.intersect (poly1, poly2);

               if (!resultPoly.epsilonEquals (xformTestPoly, TEST_TOL)) {
                  System.out.println ("Error in xform INTERSECT near line " +
                                      line + ":");
                  System.out.println ("Wanted: " + getPolyName (testPolyRef));
                  System.out.println (xformTestPoly.toString ("%9.4f"));
                  System.out.println ("   Got: " + getPolyName (resultPoly));
                  System.out.println (resultPoly.toString ("%9.4f"));
                  System.out.println ("poly1\n" + poly1.toString ("%9.4f"));
                  System.out.println ("poly2\n" + poly2.toString ("%9.4f"));
                  return false;
               }
               if (!handlerPoly.equals (resultPoly)) {
                  System.out.println ("xform INTERSECT:");
                  System.out.println (
                     "resultPoly and HandlerPoly are different");
                  System.out.println (
                     "resultPoly:\n" + resultPoly.toString ("%9.4f"));
                  System.out.println (
                     "handlerPoly:\n" + handlerPoly.toString ("%9.4f"));
               }
               intersector.removeListener (handler);

               poly1.set (poly1Save);
               poly2.set (poly2Save);
            }
            else {
               resultPoly = intersector.intersect (poly1, poly2);
               System.out.println (resultPoly.toString ("%9.4f"));
            }
            break;
         }
      }
      return true;
   }

   public static void main (String[] args) {
      try {
         String testFileName = "ConvexPolygonTest.txt";
         ReaderTokenizer rtok =
            new ReaderTokenizer (new FileReader (testFileName));

         rtok.commentChar ('#');
         ConvexPolygonIntersectorTest tester =
            new ConvexPolygonIntersectorTest();
         while (true) {
            do {
               try {
                  tester.scanRecord (rtok);
               }
               catch (IOException ioe) {
                  System.out.println (
                     "Error reading file, line " + rtok.lineno());
                  ioe.printStackTrace();
                  System.exit (1);
               }
               if (tester.cmd == DO_QUIT) {
                  System.out.println ("\nPassed\n");
                  System.exit (0);
               }
            }
            while (tester.cmd == DO_RESET);
            if (!tester.test (/* check= */true)) {
               System.exit (1);
            }
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

}
