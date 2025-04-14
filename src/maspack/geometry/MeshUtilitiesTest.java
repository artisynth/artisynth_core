package maspack.geometry;

import java.io.*;

import maspack.util.*;
import maspack.matrix.*;

public class MeshUtilitiesTest extends UnitTest {

   double EPS = 1e-10;

   String subdivide0 = new String (
      "v 0.6739512754 0.416524795 0\n" +
      "v -0.6739512754 0.416524795 0\n" +
      "v 0.6739512754 -0.416524795 0\n" +
      "v -0.6739512754 -0.416524795 0\n" +
      "v 0.416524795 0 0.6739512754\n" +
      "v 0.416524795 0 -0.6739512754\n" +
      "v -0.416524795 0 0.6739512754\n" +
      "v -0.416524795 0 -0.6739512754\n" +
      "v 0 0.6739512754 0.416524795\n" +
      "v 0 -0.6739512754 0.416524795\n" +
      "v 0 0.6739512754 -0.416524795\n" +
      "v 0 -0.6739512754 -0.416524795\n" +
      "v 0.4587939735 0.4587939735 0.4587939735\n" +
      "v 0.4587939735 0.4587939735 -0.4587939735\n" +
      "v 0.4587939735 -0.4587939735 0.4587939735\n" +
      "v 0.4587939735 -0.4587939735 -0.4587939735\n" +
      "v -0.4587939735 0.4587939735 0.4587939735\n" +
      "v -0.4587939735 0.4587939735 -0.4587939735\n" +
      "v -0.4587939735 -0.4587939735 0.4587939735\n" +
      "v -0.4587939735 -0.4587939735 -0.4587939735\n" +
      "v 0.2835502695 0.7423442429 0\n" +
      "v -0.2835502695 0.7423442429 0\n" +
      "v 0.2835502695 -0.7423442429 0\n" +
      "v -0.2835502695 -0.7423442429 0\n" +
      "v 0.7423442429 0 0.2835502695\n" +
      "v 0.7423442429 0 -0.2835502695\n" +
      "v -0.7423442429 0 0.2835502695\n" +
      "v -0.7423442429 0 -0.2835502695\n" +
      "v 0 0.2835502695 0.7423442429\n" +
      "v 0 -0.2835502695 0.7423442429\n" +
      "v 0 0.2835502695 -0.7423442429\n" +
      "v 0 -0.2835502695 -0.7423442429\n" +
      "f 13 5 25\n" +
      "f 14 11 21\n" +
      "f 15 10 23\n" +
      "f 16 6 26\n" +
      "f 17 9 22\n" +
      "f 18 8 28\n" +
      "f 19 7 27\n" +
      "f 20 12 24\n" +
      "f 21 9 13\n" +
      "f 22 11 18\n" +
      "f 23 12 16\n" +
      "f 24 10 19\n" +
      "f 25 1 13\n" +
      "f 26 3 16\n" +
      "f 27 4 19\n" +
      "f 28 2 18\n" +
      "f 29 5 13\n" +
      "f 30 7 19\n" +
      "f 31 8 18\n" +
      "f 32 6 16\n" +
      "f 13 1 21\n" +
      "f 13 9 29\n" +
      "f 14 1 26\n" +
      "f 14 6 31\n" +
      "f 15 3 25\n" +
      "f 15 5 30\n" +
      "f 16 3 23\n" +
      "f 16 12 32\n" +
      "f 17 2 27\n" +
      "f 17 7 29\n" +
      "f 18 2 22\n" +
      "f 18 11 31\n" +
      "f 19 4 24\n" +
      "f 19 10 30\n" +
      "f 20 4 28\n" +
      "f 20 8 32\n" +
      "f 21 1 14\n" +
      "f 21 11 22\n" +
      "f 22 2 17\n" +
      "f 22 9 21\n" +
      "f 23 3 15\n" +
      "f 23 10 24\n" +
      "f 24 4 20\n" +
      "f 24 12 23\n" +
      "f 25 5 15\n" +
      "f 25 3 26\n" +
      "f 26 6 14\n" +
      "f 26 1 25\n" +
      "f 27 7 17\n" +
      "f 27 2 28\n" +
      "f 28 8 20\n" +
      "f 28 4 27\n" +
      "f 29 9 17\n" +
      "f 29 7 30\n" +
      "f 30 10 15\n" +
      "f 30 5 29\n" +
      "f 31 11 14\n" +
      "f 31 6 32\n" +
      "f 32 12 20\n" +
      "f 32 8 31\n");

   String quadricCollapse0 = new String (
      "v 0.8682657235 -8.774377706e-12 0.05684318826\n" +
      "v -0.41990988 0.679428458 0.259518578\n" +
      "v 0.1769167028 -0.3002762357 -0.8368263971\n" +
      "v 0.6352482313 0.1910792913 -0.5276011504\n" +
      "v 0.2615297739 0.8449196133 0.1170693933\n" +
      "v -0.6267791347 6.622670202e-12 -0.6284465742\n" +
      "v -0.8656596633 0 0\n" +
      "v -7.295236190e-12 -0.8367739383 0.1349961521\n" +
      "v -0.2731671647 -0.1447816846 0.8210371283\n" +
      "v 0.4840428942 -0.2674675306 0.6178938301\n" +
      "f 5 10 1\n" +
      "f 3 1 8\n" +
      "f 3 5 4\n" +
      "f 8 1 10\n" +
      "f 6 7 2\n" +
      "f 6 5 3\n" +
      "f 9 10 5\n" +
      "f 6 3 8\n" +
      "f 9 8 10\n" +
      "f 2 5 6\n" +
      "f 7 9 2\n" +
      "f 8 7 6\n" +
      "f 9 7 8\n" +
      "f 4 1 3\n" +
      "f 9 5 2\n" +
      "f 5 1 4\n");

   String subdivide1 = new String (
      "v 0.1759259259 1.077235610e-17 0.1759259259\n" +
      "v -0.1759259259 1.077235610e-17 0.1759259259\n" +
      "v -0.1759259259 -1.077235610e-17 -0.1759259259\n" +
      "v 0.1759259259 -1.077235610e-17 -0.1759259259\n" +
      "v -0.06481481481 3.968762775e-18 0.06481481481\n" +
      "v 0.06481481481 -3.968762775e-18 -0.06481481481\n" +
      "v -0.08333333333 -5.102694996e-18 -0.08333333333\n" +
      "v 0.2314814815 -3.968762775e-18 -0.06481481481\n" +
      "v 0.2314814815 3.968762775e-18 0.06481481481\n" +
      "v 0.06481481481 1.417415277e-17 0.2314814815\n" +
      "v -0.06481481481 1.417415277e-17 0.2314814815\n" +
      "v -0.2314814815 3.968762775e-18 0.06481481481\n" +
      "v -0.2314814815 -3.968762775e-18 -0.06481481481\n" +
      "v 0.08333333333 5.102694996e-18 0.08333333333\n" +
      "v -0.06481481481 -1.417415277e-17 -0.2314814815\n" +
      "v 0.06481481481 -1.417415277e-17 -0.2314814815\n" +
      "f 7 6 14\n" +
      "f 8 6 4\n" +
      "f 10 5 14\n" +
      "f 12 5 2\n" +
      "f 14 5 7\n" +
      "f 15 6 7\n" +
      "f 7 5 13\n" +
      "f 15 7 3\n" +
      "f 9 6 8\n" +
      "f 1 14 9\n" +
      "f 11 5 10\n" +
      "f 2 5 11\n" +
      "f 13 5 12\n" +
      "f 3 7 13\n" +
      "f 14 6 9\n" +
      "f 10 14 1\n" +
      "f 16 6 15\n" +
      "f 4 6 16\n");

   String merged0 = new String(
      "v -0.5 0.25 -0.25\n" +
      "v -0.5 -0.25 -0.25\n" +
      "v -0.5 0.25 0.25\n" +
      "v -0.5 -0.25 0.25\n" +
      "v 0.5 0.25 -0.25\n" +
      "v 0.5 -0.25 -0.25\n" +
      "v 0.5 0.25 0.25\n" +
      "v 0.5 -0.25 0.25\n" +
      "f 3 7 5 1\n" +
      "f 6 8 4 2\n" +
      "f 5 6 2 1\n" +
      "f 4 8 7 3\n" +
      "f 2 4 3 1\n" +
      "f 7 8 6 5\n");

   private PolygonalMesh createMesh (String str) {
      PolygonalMesh mesh = new PolygonalMesh();
      try {
         mesh.read (new StringReader (str), /*zeroIndexed*/false);
      }
      catch (IOException e) {
         System.out.println ("ERROR: can't create mesh from string:\n" + str);
         e.printStackTrace(); 
         System.exit(1); 
      }
      return mesh;
   }         

   void testDivideAndDecimate() {
      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (0.5, 0);

      PolygonalMesh check = createMesh (subdivide0);
      MeshUtilities.sqrt3Subdivide (mesh, 1);
      if (!mesh.epsilonEquals (check, EPS)) {
         throw new TestException (
            "sqrt3Subdivide - mesh does not equal check");
      }

      check = createMesh (quadricCollapse0);
      MeshUtilities.quadricEdgeCollapse (mesh, /*numHalfEdges*/22);
      if (!mesh.epsilonEquals (check, EPS)) {
         throw new TestException (
            "quadricCollapse - mesh does not equal check");
      }

      mesh = MeshFactory.createPlane (0.5, 0.5);
      mesh.transform (new RigidTransform3d (0,0,0, 0,0,Math.PI/2));
      check = createMesh (subdivide1);
      MeshUtilities.sqrt3Subdivide (mesh, 1);
      MeshUtilities.sqrt3Subdivide (mesh, 1);
      if (!mesh.epsilonEquals (check, EPS)) {
         throw new TestException (
            "sqrt3Subdivide - mesh does not equal check");
      }
   }

   public void test() {
      testDivideAndDecimate();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      MeshUtilitiesTest tester = new MeshUtilitiesTest();
      tester.runtest();
   }

}
