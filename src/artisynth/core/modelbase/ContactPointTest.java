/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

import java.util.Iterator;

/**
 * Tests the methods in ContactPoint; specifically, the ones that reconstruct
 * vertex weights, and computeContactArea().
 */
public class ContactPointTest extends UnitTest {

   private static double EPS = 1e-13;

   public void test() {
      int cnt = 1000;
      for (int i=0; i<cnt; i++) {
         Point3d p1 = new Point3d();
         Point3d p2 = new Point3d();
         Point3d p3 = new Point3d();

         p1.setRandom ();
         p2.setRandom ();
         p3.setRandom ();

         switch (i%4) {
            case 1: {
               // project to y-z plane
               p1.x = p2.x = p3.x = 0;
               break;
            }
            case 2: {
               // project to x-z plane
               p1.y = p2.y = p3.y = 0;
               break;
            }
            case 3: {
               // project to x-y plane
               p1.z = p2.z = p3.z = 0;
               break;
            }
         }
         
         Vertex3d v1 = new Vertex3d(p1);
         Vertex3d v2 = new Vertex3d(p2);
         Vertex3d v3 = new Vertex3d(p3);

         testForEdge (v1, v2, 0);
         testForEdge (v1, v2, 1);
         testForEdge (v1, v2, 0.1234);
         testForEdge (v1, v2, 0.003);
         testForEdge (v1, v2, 0.9998);

         testForTriangle (v1, v2, v3, 1, 0);
         testForTriangle (v1, v2, v3, 0, 0);
         testForTriangle (v1, v2, v3, 0, 1);
         testForTriangle (v1, v2, v3, 0.456, 0.2214);
         testForTriangle (v1, v2, v3, 0.1, 0.7);               
      }
      testContactArea();
   }

   void checkResult (ContactPoint cp, Vertex3d[] vtxs, double[] wgts) {

      if (vtxs.length != cp.myVtxs.length) {
         throw new TestException (
            ""+cp.myVtxs.length+" computed vertices vs. "+vtxs.length);
      }
      for (int i=0; i<vtxs.length; i++) {
         if (cp.myVtxs[i] != vtxs[i]) {
            throw new TestException ("Unequal vertices at " + i);
         }
         if (Math.abs(cp.myWgts[i]-wgts[i]) > EPS) {
            throw new TestException (
               "Unequal weights at " + i +
               ": "+cp.myWgts[i]+", expected "+wgts[i]);
         }
      }
   }

   void testForEdge (
      Vertex3d v1, Vertex3d v2, double s1) {

      Vertex3d[] vtxs = new Vertex3d[] { v1, v2 };
      double[] wgts = new double[] { s1, 1-s1};

      Point3d p0 = new Point3d();
      for (int i=0; i<vtxs.length; i++) {
         p0.scaledAdd (wgts[i], vtxs[i].pnt);
      }
      ContactPoint cp = new ContactPoint (p0);
      cp.setForEdge (v1, v2);
      checkResult (cp, vtxs, wgts);
   }

   void testForTriangle (
      Vertex3d v1, Vertex3d v2, Vertex3d v3, double s1, double s2) {

      Vertex3d[] vtxs = new Vertex3d[] { v1, v2, v3 };
      double[] wgts = new double[] { s1, s2, 1-s1-s2};

      Point3d p0 = new Point3d();
      for (int i=0; i<vtxs.length; i++) {
         p0.scaledAdd (wgts[i], vtxs[i].pnt);
      }
      ContactPoint cp = new ContactPoint (p0);
      cp.setForTriangle (v1, v2, v3);
      checkResult (cp, vtxs, wgts);
   }

   /**
    * Computes, in world coordinates, the area-weighted normal of a triangular
    * face (i.e., the face normal scaled by the face area).
    */
   Vector3d faceAreaVector (Face face) {
      HalfEdge he = face.firstHalfEdge();
      Point3d p0 = he.getTail().getWorldPoint();
      Point3d p1 = he.getHead().getWorldPoint();
      Point3d p2 = he.getNext().getHead().getWorldPoint();
      Vector3d u = new Vector3d();
      Vector3d v = new Vector3d();
      Vector3d avec = new Vector3d();
      u.sub (p1, p0);
      v.sub (p2, p0);
      avec.cross (u, v);
      avec.scale (0.5);
      return avec;
   }

   /**
    * Checks computeContactArea() for every vertex and edge of a mesh, using a
    * random normal. The expected value is one third of the projected area of
    * each face adjacent to the vertex or edge.
    */
   void testContactArea (PolygonalMesh mesh) {
      Vector3d nrml = new Vector3d();
      nrml.setRandom();
      nrml.normalize();

      double tol = EPS*mesh.getRadius()*mesh.getRadius();
      for (Vertex3d vtx : mesh.getVertices()) {
         Vector3d asum = new Vector3d();
         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         while (it.hasNext()) {
            asum.add (faceAreaVector (it.next().getFace()));
         }
         double expected = Math.abs(nrml.dot(asum))/3;
         ContactPoint cp = new ContactPoint (vtx);
         double area = cp.computeContactArea (nrml);
         if (Math.abs(area-expected) > tol) {
            throw new TestException (
               "Vertex "+vtx.getIndex()+": area="+area+
               ", expected "+expected);
         }
      }
      for (Face face : mesh.getFaces()) {
         HalfEdge he = face.firstHalfEdge();
         do {
            if (he.isPrimary()) {
               Vector3d asum = faceAreaVector (he.getFace());
               if (he.opposite != null) {
                  asum.add (faceAreaVector (he.opposite.getFace()));
               }
               double expected = Math.abs(nrml.dot(asum))/3;
               ContactPoint cp = new ContactPoint (
                  new Point3d(he.getHead().getWorldPoint()), he, 0.5);
               double area = cp.computeContactArea (nrml);
               if (Math.abs(area-expected) > tol) {
                  throw new TestException (
                     "Edge "+he.vertexStr()+": area="+area+
                     ", expected "+expected);
               }
            }
            he = he.getNext();
         }
         while (he != face.firstHalfEdge());
      }
   }

   /**
    * For a flat mesh, with the normal set to the mesh normal, check that the
    * vertex areas, and also the edge areas, both sum to the mesh area.
    */
   void testContactAreaPartition (PolygonalMesh mesh, Vector3d nrml) {
      double totalArea = 0;
      for (Face face : mesh.getFaces()) {
         totalArea += face.computeArea();
      }
      double vtxSum = 0;
      for (Vertex3d vtx : mesh.getVertices()) {
         vtxSum += new ContactPoint (vtx).computeContactArea (nrml);
      }
      double edgeSum = 0;
      for (Face face : mesh.getFaces()) {
         HalfEdge he = face.firstHalfEdge();
         do {
            if (he.isPrimary()) {
               ContactPoint cp = new ContactPoint (
                  new Point3d(he.getHead().getWorldPoint()), he, 0.5);
               edgeSum += cp.computeContactArea (nrml);
            }
            he = he.getNext();
         }
         while (he != face.firstHalfEdge());
      }
      double tol = EPS*mesh.numFaces()*totalArea;
      if (Math.abs(vtxSum-totalArea) > tol) {
         throw new TestException (
            "Summed vertex areas = "+vtxSum+", expected "+totalArea);
      }
      if (Math.abs(edgeSum-totalArea) > tol) {
         throw new TestException (
            "Summed edge areas = "+edgeSum+", expected "+totalArea);
      }
   }

   public void testContactArea() {
      int cnt = 20;
      for (int i=0; i<cnt; i++) {
         RigidTransform3d TMW = new RigidTransform3d();
         if (i > 0) {
            // i == 0 tests an identity mesh-to-world transform
            TMW.setRandom();
         }
         // flat open plane: also check the partition property
         PolygonalMesh plane = MeshFactory.createPlane (2.0, 1.5, 6, 4);
         plane.setMeshToWorld (TMW);
         Vector3d nrml = new Vector3d();
         nrml.transform (TMW.R, Vector3d.Z_UNIT);
         testContactAreaPartition (plane, nrml);
         testContactArea (plane);

         // perturbed open plane
         for (Vertex3d vtx : plane.getVertices()) {
            vtx.pnt.z = 0.2*RandomGenerator.nextDouble (-1, 1);
         }
         plane.notifyVertexPositionsModified();
         testContactArea (plane);

         // closed mesh
         PolygonalMesh sphere = MeshFactory.createIcosahedralSphere (1.3, 1);
         sphere.setMeshToWorld (TMW);
         testContactArea (sphere);
      }

      // points not associated with a vertex or edge should return -1
      PolygonalMesh plane = MeshFactory.createPlane (1.0, 1.0, 2, 2);
      Face face = plane.getFace(0);
      Point3d cent = new Point3d();
      face.computeCentroid (cent);
      ContactPoint cp =
         new ContactPoint (cent, face, new Vector2d(0.3, 0.3));
      checkEquals ("area for face point",
                   cp.computeContactArea (Vector3d.Z_UNIT), -1.0);
      cp = new ContactPoint (new Point3d (0.1, 0.2, 0));
      checkEquals ("area for no-vertex point",
                   cp.computeContactArea (Vector3d.Z_UNIT), -1.0);
      // edge whose vertices are not connected
      cp = new ContactPoint (new Point3d());
      cp.setForEdge (plane.getVertex(0), plane.getVertex(8));
      checkEquals ("area for unconnected edge",
                   cp.computeContactArea (Vector3d.Z_UNIT), -1.0);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      ContactPointTest tester = new ContactPointTest();
      tester.runtest();
   }
}
