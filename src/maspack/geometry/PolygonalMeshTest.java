/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.SpatialInertia;
import maspack.geometry.Vertex3d;

public class PolygonalMeshTest {

   private String idxsToStr (int[] idxs) {
      String str = "[";
      for (int i=0; i<idxs.length; i++) {
         str += " " + idxs[i];
      }
      return str + " ]";
   }

   private boolean compareIdxs (int[] res, int[] chk) {
      if (res.length != chk.length) {
         return false;
      }
      int k=0;
      for (k=0; k<chk.length; k++) {
         if (res[0] == chk[k]) {
            break;
         }
      }
      if (k == chk.length) {
         return false;
      }
      for (int i=1; i<res.length; i++) {
         if (++k == chk.length) {
            k = 0;
         }
         if (res[i] != chk[k]) {
            return false;
         }
      }
      return true;
   }

   private void compareMeshes (PolygonalMesh mesh, PolygonalMesh comp) {
      if (mesh.getNumVertices() != mesh.getNumVertices()) {
         throw new TestException (
            "mesh has "+mesh.getNumVertices()+
            " vertices, expecting "+comp.getNumVertices());
      }
      for (int i=0; i<mesh.getNumVertices(); i++) {
         Vertex3d vr = mesh.getVertices().get(i);
         Vertex3d vc = comp.getVertices().get(i);
         if (!vr.pnt.epsilonEquals (vc.pnt, 1e-10)) {
            throw new TestException (
               "mesh vertex "+i+" is "+vr.pnt+", expecting "+vc.pnt);
         }
      }
      if (mesh.getNumFaces() != mesh.getNumFaces()) {
         throw new TestException (
            "mesh has "+mesh.getNumFaces()+
            " faces, expecting "+comp.getNumFaces());
      }
      for (int i=0; i<mesh.getNumFaces(); i++) {
         Face fr = mesh.getFaces().get(i);
         Face fc = comp.getFaces().get(i);
         int[] ridxs = fr.getVertexIndices();
         int[] cidxs = fc.getVertexIndices();
         if (!compareIdxs (ridxs, cidxs)) {
            throw new TestException (
               "mesh face "+i+" has indices "+idxsToStr(ridxs)+
               ", expecting "+idxsToStr(cidxs));
         }
      }
   }

   PolygonalMesh createMesh (String str) throws IOException {
      PolygonalMesh mesh = new PolygonalMesh();
      mesh.read (new StringReader (str));
      return mesh;
   }         

   public void squareTest() {
      PolygonalMesh mesh = new PolygonalMesh();
      Vertex3d vt0 = new Vertex3d (new Point3d (0, 0, 0));
      Vertex3d vt1 = new Vertex3d (new Point3d (1, 0, 0));
      Vertex3d vt2 = new Vertex3d (new Point3d (1, 1, 0));
      Vertex3d vt3 = new Vertex3d (new Point3d (0, 1, 0));
      mesh.addVertex (vt0);
      mesh.addVertex (vt1);
      mesh.addVertex (vt2);
      mesh.addVertex (vt3);
      mesh.addFace (new int[] { 0, 1, 3 });
      mesh.addFace (new int[] { 1, 2, 3 });

      mesh.checkConsistency();
   }
   
   String mergeTest1 = new String (
      "v 0 0.99 -0.02\n" +
      "v 1 0 0\n" + 
      "v 0 1 0\n" +
      "v -1 0 0\n" + 
      "v 0 -1 0\n" + 
      "v 1 1.5 0\n" + 
      "v -1 1.5 0\n" +
      "f 1 2 3\n" +
      "f 1 3 4\n" +
      "f 1 4 5\n" +
      "f 1 5 2\n" +
      "f 3 2 6\n" +
      "f 3 6 7\n" +
      "f 3 7 4\n");

   String mergeCheck1 = new String (
      "v 0 0.995 -0.01\n" +
      "v 1 0 0\n" + 
      "v -1 0 0\n" + 
      "v 0 -1 0\n" + 
      "v 1 1.5 0\n" + 
      "v -1 1.5 0\n" +
      "f 1 3 4\n" +
      "f 1 4 2\n" +
      "f 1 2 5\n" +
      "f 1 5 6\n" +
      "f 1 6 3\n");

   String mergeTest2 = new String (
      "v 0 0 0\n" +
      "v 1 0 0 \n" +
      "v 0 1 0 \n" +
      "v -1 0 0\n" +
      "v 0 -1 0\n" +
      "v 0.03 0.03 0\n" +
      "v -0.03 0.03 0\n" +
      "f 1 2 6\n" +
      "f 2 3 6\n" +
      "f 6 3 7\n" +
      "f 1 6 7\n" +
      "f 1 7 4\n" +
      "f 7 3 4 \n" +
      "f 5 2 1\n" +
      "f 5 1 4\n");

   String mergeCheck2 = new String (
      "v 0 0.02 0\n" +
      "v 1 0 0 \n" +
      "v 0 1 0 \n" +
      "v -1 0 0\n" +
      "v 0 -1 0\n" +
      "f 2 3 1\n" +
      "f 1 3 4\n" +
      "f 5 2 1\n" +
      "f 5 1 4\n");

   String mergeTest3 = new String (
      "v 0 0 0 \n" +
      "v 0 0 0\n" +
      "v 0 0 0\n" +
      "v 0 0 0\n" +
      "v 1 0 0\n" +
      "v 1 1 0\n" +
      "v 0.5 1 0\n" +
      "v -0.5 1 0\n" +
      "v -1 1 0\n" +
      "v -1 0 0\n" +
      "v -1 -1 0\n" +
      "v 0 -1 0\n" +
      "v 1 -1 0\n" +
      "f 1 2 3 4\n" +
      "f 1 5 6 7 2\n" +
      "f 2 7 8 3\n" +
      "f 3 8 9 4\n" +
      "f 4 9 10\n" +
      "f 4 10 11 12\n" +
      "f 4 12 1\n" +
      "f 1 12 13 \n" +
      "f 1 13 5\n");

   String mergeCheck3 = new String (
      "v 0 0 0\n" +
      "v 1 0 0\n" +
      "v 1 1 0\n" +
      "v 0.5 1 0\n" +
      "v -0.5 1 0\n" +
      "v -1 1 0\n" +
      "v -1 0 0\n" +
      "v -1 -1 0\n" +
      "v 0 -1 0\n" +
      "v 1 -1 0\n" +
      "f 1 2 3 4\n" +
      "f 1 4 5\n" +
      "f 1 5 6\n" +
      "f 1 6 7\n" +
      "f 1 7 8 9\n" +
      "f 1 9 10\n" +
      "f 1 10 2\n");

   String mergeTest4 = new String (
      "v 0 0 0 \n" +
      "v 1 0 0\n" +
      "v 0 1 0\n" +
      "v 0 1 0\n" +
      "v 0 0 0\n" +
      "v -1 1 0\n" +
      "v -1 1 0\n" +
      "v -1 0 0\n" +
      "v -1 0 0\n" +
      "v -1 0 0\n" +
      "v -1 -1 0\n" +
      "v 0 -1 0\n" +
      "f 1 2 3\n" +
      "f 1 3 4\n" +
      "f 1 4 6\n" +
      "f 1 6 7 5\n" +
      "f 5 7 8\n" +
      "f 5 8 9\n" +
      "f 5 9 10\n" +
      "f 5 10 11 12\n" +
      "f 5 12 1\n" +
      "f 1 12 2\n");

   String mergeCheck4 = new String (
      "v 0 0 0 \n" +
      "v 1 0 0\n" +
      "v 0 1 0\n" +
      "v -1 1 0\n" +
      "v -1 0 0\n" +
      "v -1 -1 0\n" +
      "v 0 -1 0\n" +
      "f 1 2 3\n" +
      "f 1 3 4\n" +
      "f 1 4 5\n" +
      "f 1 5 6 7\n" +
      "f 1 7 2\n");

   private void checkMerge (String result, String check) throws IOException {

      PolygonalMesh mesh = createMesh (result);
      PolygonalMesh comp = createMesh (check);

      mesh.mergeCloseVertices (0.1);
      mesh.checkConsistency();
      compareMeshes (mesh, comp);
   }      

   public void mergeTest() {

      try {
         checkMerge (mergeTest1, mergeCheck1);
         checkMerge (mergeTest2, mergeCheck2);
         checkMerge (mergeTest3, mergeCheck3);
         checkMerge (mergeTest4, mergeCheck4);
      }
      catch (IOException e) {
         throw new TestException ("Unexpected IOException: " + e);
      }
   }

   private void checkInertia (
      SpatialInertia M, SpatialInertia Mcheck, double tol) {
      if (!M.epsilonEquals (Mcheck, tol)) {
         throw new TestException (
            "Computed inertia:\n" + M + "\nExpected:\n" + Mcheck);
      }
   }

   private void checkTranslatedInertia (
      PolygonalMesh mesh, SpatialInertia M, double density,
      double cx, double cy, double cz, double tol) {

      PolygonalMesh tmesh = new PolygonalMesh (mesh);
      tmesh.transform (new RigidTransform3d (cx, cy, cz));
      SpatialInertia MT = tmesh.createInertia (density);
      SpatialInertia MTcheck = new SpatialInertia ();
      Point3d com = new Point3d();
      M.getCenterOfMass (com);
      com.add (new Vector3d (cx, cy, cz));
      MTcheck.set (M.getMass(), M.getRotationalInertia(), com);
      checkInertia (MT, MTcheck, tol);
   }

   public void inertiaTest() {
      
      SpatialInertia M = new SpatialInertia();
      SpatialInertia Mcheck = new SpatialInertia();
      double density = 3;
      double tol = 1e-7;

      PolygonalMesh ellipsoid = MeshFactory.createSphere (2.0, 12);
      ellipsoid.scale (3, 2, 1);
      M = ellipsoid.createInertia(density);
      Mcheck = new SpatialInertia (
         537.41531629, 1980.71973353, 3943.79254142, 5103.98930052);
      checkInertia (M, Mcheck, tol);
      checkTranslatedInertia (ellipsoid, M, density, 1, 2, 3, tol);

      PolygonalMesh torus = MeshFactory.createTorus (2.0, 1.0, 24, 16);
      M = torus.createInertia(density);
      Mcheck = new SpatialInertia (
         114.10071614, 294.63934170, 294.63934170, 533.67589197);
      checkInertia (M, Mcheck, tol);
      checkTranslatedInertia (torus, M, density, 1, 2, 3, tol);
   }

   public void test() throws TestException {
      squareTest();
      mergeTest();      
      inertiaTest();
   }

   public static void main (String[] args) {
      PolygonalMeshTest tester = new PolygonalMeshTest();
      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
