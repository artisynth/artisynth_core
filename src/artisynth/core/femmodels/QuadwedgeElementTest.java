/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;
import maspack.util.*;

public class QuadwedgeElementTest extends UnitTest {

   QuadwedgeElement myElem;

   public QuadwedgeElementTest () {
      FemNode3d[] nodes = new FemNode3d[15];
      double[] coords = QuadwedgeElement.myNodeCoords;
      for (int i=0; i<15; i++) {
         nodes[i] = new FemNode3d (coords[i*3], coords[i*3+1], coords[i*3+2]);
      }
      myElem = new QuadwedgeElement (nodes);
   }

   void testShapes (Vector3d coords) {

      double s1 = coords.x;
      double s2 = coords.y;
      double r = coords.z;

      double[] nodeCoords = QuadwedgeElement.myNodeCoords;

      for (int j=0; j<15; j++) {

         double r0 = r*nodeCoords[j*3+2];
         double s0 = 1-s1-s2;

         double nexpected;

         switch (j) {
            case 0:
            case 3:
               nexpected = -s0*(1+r0)*(2*s1+2*s2-r0)/2;
               break;
            case 1:
            case 4:
               nexpected = s1*(1+r0)*(2*s1+r0-2)/2;
               break;
            case 2:
            case 5:
               nexpected = s2*(1+r0)*(2*s2+r0-2)/2;
               break;
            case 6:
            case 9:
               nexpected = 2*s1*s0*(1+r0);
               break;
            case 7:
            case 10:
               nexpected = 2*s1*s2*(1+r0);
               break;
            case 8:
            case 11:
               nexpected = 2*s2*s0*(1+r0);
               break;
            case 12:
               nexpected = s0*(1-r*r);
               break;
            case 13:
               nexpected = s1*(1-r*r);
               break;
            case 14:
               nexpected = s2*(1-r*r);
               break;
            default:{
               throw new InternalErrorException ("node "+j+" unimplemented");
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
      double[] coords = QuadwedgeElement.myNodeCoords;
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
      QuadwedgeElementTest tester = new QuadwedgeElementTest();
      tester.runtest();
   }
}
