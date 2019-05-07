package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;

/**
 * Implements a mesh consisting of a set of polylines.
 */
public class PolylineMeshTest extends UnitTest {
   
   public void testTiming() {
      PolylineMesh mesh = new PolylineMesh();
      
      double l = 10;
      int numv = 10000;
      int[] idxs = new int[numv];
      for (int i=0; i<numv; i++) {
         mesh.addVertex (i*l/(numv-1), 0, 0);         
         idxs[i] = i;
      }
      mesh.addLine (idxs);
       double m = 1.23;

      int cnt = 1000;
      FunctionTimer timer = new FunctionTimer();
      SpatialInertia M;
      for (int k=0; k<cnt; k++) {
         M = mesh.createInertia (m, MassDistribution.LENGTH);
      }
      timer.start();
      for (int k=0; k<cnt; k++) {
         M = mesh.createInertia (m, MassDistribution.LENGTH);
      }
      timer.stop();
      System.out.println ("time=" + timer.result(cnt));
   }

   public void testCreateInertia() {

      // test inertia is for a single line of length l emanating from the
      // origin
      RigidTransform3d XMW = new RigidTransform3d();
      XMW.setRandom();

      PolylineMesh mesh = new PolylineMesh();
      
      double l = 10;
      int numv = 100;
      int[] idxs = new int[numv];
      for (int i=0; i<numv; i++) {
         mesh.addVertex (i*l/(numv-1), 0, 0);         
         idxs[i] = i;
      }
      mesh.addLine (idxs);
      mesh.transform (XMW);

      double m = 1.23;
      SpatialInertia M = mesh.createInertia (m, MassDistribution.LENGTH);
      
      SpatialInertia Mcheck = new SpatialInertia();
      SymmetricMatrix3d J = new SymmetricMatrix3d();
      J.m11 = m*l*l/12;
      J.m22 = m*l*l/12;
      Mcheck.set (m, J, new Point3d(l/2, 0, 0));
      Mcheck.transform (XMW);

      checkEquals ("createdInertia", M, Mcheck, 1e-10);
   }

   public void test() {
      testCreateInertia();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      PolylineMeshTest tester = new PolylineMeshTest();
      tester.runtest();
   }

}
