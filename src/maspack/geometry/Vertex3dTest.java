package maspack.geometry;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;

public class Vertex3dTest extends UnitTest {

   String testPoly0 = new String (
      "v 0 0 0\n" +
      "v 1 0 0\n" + 
      "v 1 1 0\n" + 
      "v 0 1 0\n" + 
      "v 0 0 1\n" + 
      "f 0 3 2 1\n" +
      "f 0 1 4\n" +
      "f 1 2 4\n" +
      "f 2 3 4\n" +
      "f 3 0 4\n");

   String testPoly1 = new String (
      "v 0 0 0\n" +
      "v 1 0 0\n" + 
      "v 1 1 0\n" + 
      "v -1 1 0\n" + 
      "v 0.5 0.5 1\n" + 
      "f 0 3 2 1\n" +
      "f 0 1 4\n" +
      "f 1 2 4\n" +
      "f 2 3 4\n" +
      "f 3 0 4\n");

   private PolygonalMesh createMesh (String str, boolean zeroIndexed) {
      PolygonalMesh mesh = new PolygonalMesh();
      try {
         mesh.read (new StringReader (str), zeroIndexed);
      }
      catch (IOException e) {
         System.out.println ("ERROR: can't create mesh from string:\n" + str);
         e.printStackTrace(); 
         System.exit(1); 
      }
      return mesh;
   }         

   /**
    * Computes the angle between he and he.next
    */
   private double halfEdgeAngle (HalfEdge he) {
      Vector3d u0 = new Vector3d();
      Vector3d u1 = new Vector3d();
      Vector3d xprod = new Vector3d();
      u0.sub (he.tail.getPosition(), he.head.getPosition());
      u1.sub (he.next.head.getPosition(), he.next.tail.getPosition());
      xprod.cross (u0, u1);
      return Math.atan2 (xprod.norm(), u0.dot(u1));
   }

   /**
    * Computes the cross product between he and he.next
    */
   private Vector3d halfEdgeCrossProduct (HalfEdge he) {
      Vector3d xprod = new Vector3d();
      Vector3d u0 = new Vector3d();
      Vector3d u1 = new Vector3d();
      u0.sub (he.head.getPosition(), he.tail.getPosition());
      u1.sub (he.next.head.getPosition(), he.next.tail.getPosition());
      xprod.cross (u0, u1);
      return xprod;
   }

   void testNormalComputation () {
      PolygonalMesh mesh;
      RigidTransform3d TMW = new RigidTransform3d();
      mesh = createMesh (testPoly0, /*zeroIndexed*/true);
      testNormalComputation (mesh);
      TMW.setRandom();
      mesh.setMeshToWorld (TMW);
      testNormalComputation (mesh);
      mesh = createMesh (testPoly1, /*zeroIndexed*/true);
      testNormalComputation (mesh);
      TMW.setRandom();
      mesh.setMeshToWorld (TMW);
      testNormalComputation (mesh);
   }

   void testNormalComputation (PolygonalMesh mesh) {
      double EPS = 1e-12;
      Vector3d facenrm = new Vector3d();

      for (Vertex3d v : mesh.getVertices()) {
         Iterator<HalfEdge> it = v.getIncidentHalfEdges();
         Vector3d regNrm = new Vector3d();
         Vector3d areaNrm = new Vector3d();
         Vector3d angNrm = new Vector3d();
         Vector3d worldNrm = new Vector3d();
         Vector3d xprodChk = new Vector3d();
         while (it.hasNext()) {
            HalfEdge he = it.next();
            he.face.computeNormal(facenrm);
            regNrm.add (facenrm);
            areaNrm.scaledAdd (he.face.computeArea(), facenrm);
            angNrm.scaledAdd (halfEdgeAngle (he), facenrm);
            xprodChk.add (halfEdgeCrossProduct (he));
         }
         regNrm.normalize();
         areaNrm.normalize();
         angNrm.normalize();
         worldNrm.transform (mesh.getMeshToWorld(), regNrm);

         // System.out.println ("vertex  " + v.getIndex());
         // System.out.println (" reg   " + regNrm.toString("%8.5f"));
         // System.out.println (" area  " + areaNrm.toString("%8.5f"));
         // System.out.println (" ang   " + angNrm.toString("%8.5f"));
         // System.out.println (" world " + worldNrm.toString("%8.5f"));

         Vector3d nrm = new Vector3d();
         Vector3d xprod = new Vector3d();
         v.computeNormal (nrm);
         checkEquals ("vertex "+v.getIndex()+" regular normal", nrm, regNrm, EPS);
         v.computeAreaWeightedNormal (nrm);
         checkEquals ("vertex "+v.getIndex()+" area normal", nrm, areaNrm, EPS);
         v.computeAngleWeightedNormal (nrm);
         checkEquals ("vertex "+v.getIndex()+" ang normal", nrm, angNrm, EPS);
         v.computeWorldNormal (nrm);
         checkEquals ("vertex "+v.getIndex()+" world normal", nrm, worldNrm, EPS);
         v.sumEdgeCrossProducts (xprod);
         checkEquals ("vertex "+v.getIndex()+" edge xprod", xprod, xprodChk, EPS);
      }
   }

   public void test() {
      testNormalComputation();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      Vertex3dTest tester = new Vertex3dTest();
      tester.runtest();
   }

}
