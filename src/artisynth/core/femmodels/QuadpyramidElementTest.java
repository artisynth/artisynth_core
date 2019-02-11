/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;
import maspack.util.*;

public class QuadpyramidElementTest extends UnitTest {

   QuadpyramidElement myElem;

   private static int NUM_NODES = QuadpyramidElement.NUM_NODES;

   public QuadpyramidElementTest () {
      FemNode3d[] nodes = new FemNode3d[NUM_NODES];
      double[] coords = QuadpyramidElement.myNodeCoords;
      for (int i=0; i<NUM_NODES; i++) {
         nodes[i] = new FemNode3d (coords[i*3], coords[i*3+1], coords[i*3+2]);
      }
      myElem = new QuadpyramidElement (nodes);
   }

   void testShapes (Vector3d coords) {

      double s = coords.x;
      double t = coords.y;
      double r = coords.z;
      double q = (1-r)/2;

      double[] nodeCoords = QuadpyramidElement.myNodeCoords;

      for (int j=0; j<NUM_NODES; j++) {

         double s0 = s*nodeCoords[j*3+0];
         double t0 = t*nodeCoords[j*3+1];
         double nexpected;

         if (j < 4) {
            nexpected = q*(1+s0)*(1+t0)*(-1+q*s0+q*t0)/4;
         }
         else {
            switch (j) {
               case 4: {
                  nexpected = (1-q)*(1-2*q);
                  break;
               }
                  
               case 5:
               case 7: {
                  nexpected = q*q*(1+t0)*(1-s*s)/2;
                  break;
               }
               case 6:
               case 8:{
                  nexpected = q*q*(1+s0)*(1-t*t)/2;
                  break;
               }
               case 9:
               case 10:
               case 11:
               case 12:{
                  nexpected = q*(1-q)*(1+s0+t0+s0*t0);
                  break;
               }
               default:{
                  throw new InternalErrorException ("node "+j+" unimplemented");
               }
            }
         }
         
         double n = myElem.getN (j, coords);
         //         double nexpected = (i == j ? 1 : 0);
         if (Math.abs(n-nexpected) > 1e-12) {
            throw new TestException (
               "coords "+coords+", node "+j+": shape="+n+
               ", nexpected "+nexpected);
         }
         double h = 1e-8;
         Vector3d dNdsExpected = new Vector3d();
         Vector3d coordsmod = new Vector3d();
         for (int k=0; k<3; k++) {
            coordsmod.set (coords);
            coordsmod.set (k, coords.get(k)+h);
            dNdsExpected.set (k, (myElem.getN(j, coordsmod)-n)/h);
         }
         Vector3d dNds = new Vector3d();
         myElem.getdNds (dNds, j, coords);
         Vector3d diff = new Vector3d();
         diff.sub (dNds, dNdsExpected);
         if (diff.norm() > 1e-7) {
            throw new TestException (
               "coords "+coords+", node "+j+": dNds="+dNds+
               ", expected "+dNdsExpected);
         }
      }
   }

   public void test () {
      double[] coords = QuadpyramidElement.myNodeCoords;
      // test shape functions
      for (int i=0; i<coords.length/3; i++) {
         testShapes (new Vector3d (coords[i*3], coords[i*3+1], coords[i*3+2]));
      }
      coords = myElem.getIntegrationCoords();
      for (int i=0; i<coords.length/4; i++) {
         // multiply i by 4 since integration coords also include a weight field
         testShapes (new Vector3d (coords[i*4], coords[i*4+1], coords[i*4+2]));
      }
      for (int i=0; i<100; i++) {
         Vector3d vec = new Vector3d();
         vec.setRandom();
         testShapes (vec);
      }
      
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      QuadpyramidElementTest tester = new QuadpyramidElementTest();
      tester.runtest();
   }
}
