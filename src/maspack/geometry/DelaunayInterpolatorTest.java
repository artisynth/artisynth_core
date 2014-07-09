/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

public class DelaunayInterpolatorTest {

   DelaunayInterpolator myInterpolator = new DelaunayInterpolator();
   Point3d[] myPoints;
   String myPointsName;
   Random myRand = RandomGenerator.get();

   // single point
   double[] coords0 = new double[] {
      1, 2, 3
   };

   // points in a line
   double[] coordsLine = new double[] {
      1, 0, 0,
      2, 0, 0,
      4, 0, 0
   };

   // simple triangle
   double[] coordsTri = new double[] {
      0, 0, 0,
      1, 0, 0,
      1, 2, 0
   };

   // more complex polygon
   double[] coordsPoly = new double[] {
      0, 0, 0,
      1, 0, 0,
      1, 2, 0,
      -1, 3, 0,
      -0.5, 1, 0,
   };

   // unit tet
   double[] coordsTet = new double[] {
      0, 0, 0,
      1, 0, 0,
      0, 1, 0,
      0, 0, 1,
   };

   // double tet
   double[] coordsDoubleTet = new double[] {
      0, 0, 0,
      1, 0, 0,
      0, 1, 0,
      0, 0, 1,
     -1, 1e-12, 1e-12
   };

   // unit cube
   double[] coordsCube = new double[] {
      0, 0, 1,
      1, 0, 1,
      1, 1, 1,
      0, 1, 1,
      0, 0, 0,
      1, 0, 0,
      1, 1, 0,
      0, 1, 0,
   };

   void setPoints (double[] coords, String name) {
      int nump = coords.length/3;
      myPoints = new Point3d[nump];
      for (int i=0; i<nump; i++) {
         myPoints[i] = new Point3d (coords[3*i], coords[3*i+1], coords[3*i+2]);
      }
      myInterpolator.setPoints (myPoints);
      myPointsName = name;
   }

   boolean checkWeightsAndIndices (
      double[] weights, int[] indices,
      double[] weightsCheck, int[] indicesCheck) {

      HashMap<Integer,Double> weightMap = new HashMap<Integer,Double>();

      boolean undefinedIndexFound = false;
      for (int i=0; i<4; i++) {
         if (indices[i] != -1) {
            if (undefinedIndexFound) {
               System.out.println ("Not all null indices placed at the end");
               return false;
            }
            if (weightMap.get(indices[i]) != null) {
               System.out.println ("Index "+indices[i]+" is repeated");
               return false;
            }
            weightMap.put (indices[i], weights[i]);
         }
         else {
            if (weights[i] != 0) {
               System.out.println ("Inactive weights not 0");
               return false;
            }
            undefinedIndexFound = true;
         }
      }
      
      boolean failed = false;
      for (int i=0; i<4; i++) {
         if (indicesCheck[i] != -1) {
            Double w = weightMap.get(indicesCheck[i]);
            if (w == null) {
               if (weightsCheck[i] != 0) {
                  failed = true;
                  break;
               }
            }
            else {
               if (Math.abs(w.doubleValue()-weightsCheck[i]) > 1e-10) {
                  failed = true;
                  break;
               }
               weightMap.remove(indicesCheck[i]);
            }
         }
      }
      // check that any other weights are effectively 0 ...
      if (!weightMap.isEmpty()) {
         for (Double w : weightMap.values()) {
            if (Math.abs(w.doubleValue()) > 1e-10) {
               failed = true;
            }
         }
      }
      if (failed) {
         System.out.println ("Weights and/or indices differ.");
         return false;
      }
      else {
         return true;
      }
   }

   void test (double x, double y, double z, int i0, double w0) {
      test (x, y, z, i0, w0, -1, 0, -1, 0, -1, 0);
   }

   void test (double x, double y, double z, 
              int i0, double w0, int i1, double w1) {
      test (x, y, z, i0, w0, i1, w1, -1, 0, -1, 0);
   }

   void test (double x, double y, double z, 
              int i0, double w0, int i1, double w1, int i2, double w2) {
      test (x, y, z, i0, w0, i1, w1, i2, w2, -1, 0);
   }

   void test (double x, double y, double z, 
              int i0, double w0, int i1, double w1,
              int i2, double w2, int i3, double w3) {
      
      double[] weightsCheck = new double[] { w0, w1, w2, w3 };
      int[] indicesCheck = new int[] { i0, i1, i2, i3 };

      Point3d pnt = new Point3d (x, y, z);

      dotest (pnt, indicesCheck, weightsCheck);

      int numRandom = 100;

      if (numRandom > 0) {
         for (int k=0; k<numRandom; k++) {
            RigidTransform3d X = new RigidTransform3d();
            
            X.R.setRandom();
            X.p.setRandom();

            Point3d[] pntsx = new Point3d[myPoints.length];
            for (int i=0; i<myPoints.length; i++) {
               pntsx[i] = new Point3d (myPoints[i]);
               pntsx[i].transform (X);
            }
            Point3d pntx = new Point3d (pnt);
            pntx.transform (X);

            myInterpolator.setPoints (pntsx);
            dotest (pntx, indicesCheck, weightsCheck);            
         }
         myInterpolator.setPoints (myPoints);
      }
   }

   void dotest (Point3d pnt, int[] indicesCheck, double[] weightsCheck) {

      double[] weights = new double[4];
      int[] indices = new int[4];

      myInterpolator.getInterpolation (weights, indices, pnt);

      if (!checkWeightsAndIndices (weights, indices, weightsCheck,indicesCheck)) {
         System.out.printf (
            "point set '%s', point %g %g %g\n", myPointsName, pnt.x,pnt.y, pnt.z);
         System.out.print ("Expected: ");
         for (int k=0; k<4; k++) {
            System.out.printf ("(%d, %g) ", indicesCheck[k], weightsCheck[k]);
         }
         System.out.println ("");
         System.out.print ("Got: ");
         for (int k=0; k<4; k++) {
            System.out.printf ("(%d, %g) ", indices[k], weights[k]);
         }
         System.out.println ("");
         throw new TestException();
      }
   }

   void test() {

      setPoints (coords0, "one point");
      test (1, 2, 3,      0, 1);
      test (0, 0, 0,      0, 1);

      setPoints (coordsLine, "line");
      test ( .99, 4, 6,   0, 1);
      test (1.99, 4, 6,   0, 0.01, 1, 0.99);
      test (2.50, 0, 7,   1, 0.75, 2, 0.25);
      test (4.01,-5, 3,   2, 1);

      setPoints (coordsTri, "tri");
      test (1, 1, 0,      1, 0.5, 2, 0.5);
      test (1.1, 1, 0,    1, 0.5, 2, 0.5);
      test (0, 0, 0,      0, 1);
      test (1, 0, 0,      1, 1);
      test (1, 2, 0,      2, 1);
      test (-1, -1, 0,    0, 1);
      test (2, -1, 0,     1, 1);
      test (2, 3, 0,      2, 1);
      test (0.4, 0, 0,    0, 0.6, 1, 0.4);
      test (0.4, -0.1, 0, 0, 0.6, 1, 0.4);
      test (0.5, 0.5, 0,  0, 0.5, 1, 0.25, 2, 0.25);
      test (0.9, 0.2, 0,  0, 0.1, 1, 0.8, 2, 0.1);

      setPoints (coordsPoly, "poly");

      // check inside all triangles
      test (.05, .5, 0,     0, .2, 1, .3, 4, .5);
      test (0.1, 1.2, 0,    1, .1, 2, .3, 4, .6);
      test (-.35, 1.8, 0,   2, .2, 3, .3, 4, .5);

      // check all points
      test (0, 0, 0,      0, 1);
      test (1, 0, 0,      1, 1);
      test (1, 2, 0,      2, 1);
      test (-1, 3, 0,    3, 1);
      test (-0.5, 1, 0,   4, 1);

      // check all edges
      test (.9, 0, 0,     0, .1, 1, .9);
      test (1, 1.4, 0,    1, .3, 2, .7);
      test (-.6, 2.8, 0,  2, .2, 3, .8);
      test (-.65, 1.6, 0, 3, .3, 4, .7);
      test (-.15, .3, 0,  0, .7, 4, .3);

      // check offsets from edges
      test (.9, -1, 0,     0, .1, 1, .9);
      test (2, 1.4, 0,    1, .3, 2, .7);
      test (0.4, 4.8, 0,  2, .2, 3, .8);
      test (-1.65, 1.35, 0, 3, .3, 4, .7);
      test (-2.15, -.7, 0,  0, .7, 4, .3);

      // check voronoi regions for points
      test (-.1, -1, 0,      0, 1);
      test (2, -1, 0,      1, 1);
      test (2, 3, 0,      2, 1);
      test (-2, 4, 0,    3, 1);
      test (-1.5, 0.5, 0,   4, 1);

      setPoints (coordsTet, "tet");
      test (0.25, 0.25, 0.25,   0, 0.25, 1, 0.25, 2, 0.25, 3, 0.25);
      test (0.1, 0.2, 0.3,      0, 0.4, 1, 0.1, 2, 0.2, 3, 0.3);
      
      test (0, 0, 0,            0, 1, 1, 0, 2, 0, 3, 0);
      test (1, 0, 0,            0, 0, 1, 1, 2, 0, 3, 0);
      test (0, 1, 0,            0, 0, 1, 0, 2, 1, 3, 0);
      test (0, 0, 1,            0, 0, 1, 0, 2, 0, 3, 1);
      test (1, 1, 1,            0, 0, 1, 1/3., 2, 1/3., 3, 1/3.);

      test (-.1, -.1, -.1,      0, 1, 1, 0, 2, 0, 3, 0);
      test (1.1, 0, 0,          0, 0, 1, 1, 2, 0, 3, 0);
      test (0, 1.1, 0,          0, 0, 1, 0, 2, 1, 3, 0);
      test (0, 0, 1.1,          0, 0, 1, 0, 2, 0, 3, 1);

      test (.1, .1, 0,          0, .8, 1, .1, 2, .1, 3, 0);
      test (0, .1, .1,          0, .8, 1, 0, 2, .1, 3, .1);
      test (.1, 0, .1,          0, .8, 1, .1, 2, 0, 3, .1);
      test (.1, .1, .1,          0, .7, 1, .1, 2, .1, 3, .1);

      test (.1, .1, -1e-8,      0, .8, 1, .1, 2, .1, 3, 0);
      test (-1e-8, .1, .1,      0, .8, 1, 0, 2, .1, 3, .1);
      test (.1, -1e-8, .1,      0, .8, 1, .1, 2, 0, 3, .1);
      test (.1, .1, .1,         0, .7, 1, .1, 2, .1, 3, .1);

      setPoints (coordsDoubleTet, "doubleTet");

      test (0.25, 0.25, 0.25,   0, 0.25, 1, 0.25, 2, 0.25, 3, 0.25);
      test (0.1, 0.2, 0.3,      0, 0.4, 1, 0.1, 2, 0.2, 3, 0.3);
      test (-.1, 0.2, 0.3,      0, 0.4, 4, 0.1, 2, 0.2, 3, 0.3);

      test (0, 0, 0,            0, 1, 1, 0, 2, 0, 3, 0);
      test (1, 0, 0,            0, 0, 1, 1, 2, 0, 3, 0);
      test (0, 1, 0,            0, 0, 1, 0, 2, 1, 3, 0);
      test (0, 0, 1,            0, 0, 1, 0, 2, 0, 3, 1);
      test (-1, 0, 0,           0, 0, 4, 1, 2, 0, 3, 0);
      test (-1, 1, 1,           0, 0, 4, 1/3., 2, 1/3., 3, 1/3.);

      test (0, -.1, -.1,        0, 1, 1, 0, 2, 0, 3, 0);
      test (1.1, 1.1, 1.1,      0, 0, 1, 1/3., 2, 1/3., 3, 1/3.);
      test (-1.1, 1.1, 1.1,     0, 0, 4, 1/3., 2, 1/3., 3, 1/3.);

      test (1.1, 0, 0,          0, 0, 1, 1, 2, 0, 3, 0);
      test (0, 1.1, 0,          0, 0, 1, 0, 2, 1, 3, 0);
      test (0, 0, 1.1,          0, 0, 1, 0, 2, 0, 3, 1);
      test (-1.1, 0, 0,         0, 0, 4, 1, 2, 0, 3, 0);

      test (.1, .1, 0,          0, .8, 1, .1, 2, .1, 3, 0);
      test (0, .1, .1,          0, .8, 1, 0, 2, .1, 3, .1);
      test (.1, 0, .1,          0, .8, 1, .1, 2, 0, 3, .1);
      test (.1, .1, .1,         0, .7, 1, .1, 2, .1, 3, .1);

      test (-.1, .1, 0,         0, .8, 4, .1, 2, .1, 3, 0);
      test (-.1, 0, .1,         0, .8, 4, .1, 2, 0, 3, .1);
      test (-.1, .1, .1,        0, .7, 4, .1, 2, .1, 3, .1);

      test (.1, .1, -1e-8,      0, .8, 1, .1, 2, .1, 3, 0);
      test (.1, -1e-8, .1,      0, .8, 1, .1, 2, 0, 3, .1);
      test (-.1, .1, -1e-8,     0, .8, 4, .1, 2, .1, 3, 0);
      test (-.1, -1e-8, .1,     0, .8, 4, .1, 2, 0, 3, .1);
   }

   public static void main (String[] args) {
      DelaunayInterpolatorTest tester = new DelaunayInterpolatorTest();

      RandomGenerator.setSeed (0x1234);

      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit(1); 
      }
      System.out.println ("\nPassed\n");
   }
   
}




