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

public class PolygonalMeshTest extends MeshTestBase {

   private ArrayList<Vector3d> createNormals (double[] coords) {
      ArrayList<Vector3d> normals = new ArrayList<Vector3d>();
      for (int i=0; i<coords.length-2; i+=3) {
         Vector3d n = new Vector3d (coords[i], coords[i+1], coords[i+2]);
         n.normalize();
         normals.add (n);
      }
      return normals;
   }

   private ArrayList<float[]> createColors (float[][] rgbaVals) {
      ArrayList<float[]> colors = new ArrayList<float[]>();
      for (int i=0; i<rgbaVals.length; i++) {
         colors.add (rgbaVals[i]);
      }
      return colors;
   }

   private void checkNormals (
      ArrayList<Vector3d> nrmls, ArrayList<Vector3d> check) {
      if ((nrmls==null) != (check==null)) {
          throw new TestException (
             "Normals unequal. nrmls=" + nrmls + ", check=" + check);
      }
      if (nrmls != null) {
         if (nrmls.size() != check.size()) {
            throw new TestException (
               "Normals unequal. Expected "+check.size()+", got "+nrmls.size());
         }
         for (int i=0; i<nrmls.size(); i++) {
            if (!nrmls.get(i).epsilonEquals (check.get(i), 1e-8)) {
               throw new TestException (
                  "Normal "+i+" unequal. Expected:\n"+check.get(i)+
                  "\nGot:\n"+nrmls.get(i));
            }
         }
      }
   }

   private void checkColor (float[] color, float[] check) {
      if (!Arrays.equals (color, check)) {
         throw new TestException (
            "Expected color:\n"+toStr(check)+
            "\nGot:\n"+toStr(color));
      }
   }                                                 

   private void checkColors (
      ArrayList<float[]> colors, ArrayList<float[]> check) {
      if ((colors==null) != (check==null)) {
          throw new TestException (
             "Colors unequal. colors=" + colors + ", check=" + check);
      }
      if (colors != null) {
         if (colors.size() != check.size()) {
            throw new TestException (
               "Colors unequal. Expected "+check.size()+", got "+colors.size());
         }
         for (int i=0; i<colors.size(); i++) {
            if (!Arrays.equals (colors.get(i), check.get(i))) {
               throw new TestException (
                  "Color "+i+" unequal. Expected:\n"+toStr(check.get(i))+
                  "\nGot:\n"+toStr(colors.get(i)));
            }
         }
      }
   }

   private String toStr (int[] indices) {
      if (indices == null) {
         return "null";
      }
      else {
         StringBuilder sb = new StringBuilder();
         sb.append ("[");
         for (int i=0; i<indices.length; i++) {
            sb.append (" "+indices[i]);
         }
         sb.append (" ]");
         return sb.toString();
      }
   }

   private String toStr (float[] colors) {
      if (colors == null) {
         return "null";
      }
      else {
         StringBuilder sb = new StringBuilder();
         sb.append ("[");
         for (int i=0; i<colors.length; i++) {
            sb.append (" "+colors[i]);
         }
         sb.append (" ]");
         return sb.toString();
      }
   }

   private void checkIndices (int[] indices, int[] check) {
      if (!Arrays.equals (indices, check)) {
         throw new TestException (
            "Indices unequal. Expected:\n" + toStr(check) +
            "\nGot:\n" + toStr(indices));
      }
   }      

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
      if (mesh.numVertices() != mesh.numVertices()) {
         throw new TestException (
            "mesh has "+mesh.numVertices()+
            " vertices, expecting "+comp.numVertices());
      }
      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vr = mesh.getVertices().get(i);
         Vertex3d vc = comp.getVertices().get(i);
         if (!vr.pnt.epsilonEquals (vc.pnt, 1e-10)) {
            throw new TestException (
               "mesh vertex "+i+" is "+vr.pnt+", expecting "+vc.pnt);
         }
      }
      if (mesh.numFaces() != mesh.numFaces()) {
         throw new TestException (
            "mesh has "+mesh.numFaces()+
            " faces, expecting "+comp.numFaces());
      }
      for (int i=0; i<mesh.numFaces(); i++) {
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

   PolygonalMesh createMesh (String str, boolean zeroIndexed) {
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

      PolygonalMesh mesh = createMesh (result, false);
      PolygonalMesh comp = createMesh (check, false);

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

      RigidTransform3d XMW = new RigidTransform3d();
      XMW.setRandom();

      double w = 3.0;
      double h = 2.0;
      double m = 1.23;
      Mcheck.set (m, m*h*h/12, m*w*w/12, m*(h*h+w*w)/12);
      Mcheck.transform (XMW);

      PolygonalMesh plate = MeshFactory.createRectangle (w, h, 10, 10, false);
      plate.transform (XMW);
      M = plate.createAreaInertia(m);
      checkInertia (M, Mcheck, tol);

      plate = MeshFactory.createQuadRectangle (w, h, 10, 10);
      plate.transform (XMW);
      M = plate.createAreaInertia(m);
      checkInertia (M, Mcheck, tol);

      SymmetricMatrix3d J = new SymmetricMatrix3d();
      double l = 2*w+2*h;
      double mw = m*w/l;
      double mh = m*h/l;
      J.m00 = mh*h*h/6 + mw*h*h/2;
      J.m11 = mw*w*w/6 + mh*w*w/2;
      J.m22 = 0.5*(mh*w*w+mw*h*h) + (mh*h*h+mw*w*w)/6;
      Mcheck.set (m, J);
      Mcheck.transform (XMW);
      plate = MeshFactory.createQuadRectangle (w, h, 1, 1);
      plate.transform (XMW);
      M = plate.createEdgeLengthInertia(m);
      checkInertia (M, Mcheck, tol);      

      double d = Math.sqrt(h*h+w*w);
      l = 2*w+2*h+d;
      double md = m*d/l;
      mw = m*w/l;
      mh = m*h/l;
      J.m00 = mh*h*h/6 + mw*h*h/2 + md*h*h/12;
      J.m01 = md*h*w/12;
      J.m10 = md*h*w/12;
      J.m11 = mw*w*w/6 + mh*w*w/2 + md*w*w/12;
      J.m22 = 0.5*(mh*w*w+mw*h*h) + (mh*h*h+mw*w*w)/6 + md*d*d/12;
      Mcheck.set (m, J);
      Mcheck.transform (XMW);
      plate = MeshFactory.createRectangle (w, h, 1, 1, false);
      plate.transform (XMW);
      M = plate.createEdgeLengthInertia(m);
      checkInertia (M, Mcheck, tol);       

   }


   String cubeObj = new String (
      "v 1 0 1\n" +
      "v 1 1 1\n" + 
      "v 0 1 1\n" +
      "v 0 0 1\n" + 
      "v 1 0 0\n" + 
      "v 1 1 0\n" + 
      "v 0 1 0\n" + 
      "v 0 0 0\n" + 
      "f 1 2 3\n" +
      "f 0 1 3\n" +
      "f 0 4 1\n" +
      "f 1 4 5\n" +
      "f 4 7 6 5\n" +
      "f 2 6 7 3\n" +
      "f 1 5 6 2\n" +
      "f 0 3 7 4\n");

   double[] cubeNormals = new double[] {
      1, -1, 1, 
      1, 1, 1,
      -1, 1, 1,
      -1, -1, 1,
      1, -1, -1, 
      1, 1, -1,
      -1, 1, -1,
      -1, -1, -1 };

   int[] indicesCheck = new int[] {
      1, 2, 3,  0, 1, 3,  0, 4, 1,  1, 4, 5,  4, 7, 6, 5,  
      2, 6, 7, 3,   1, 5, 6, 2,   0, 3, 7, 4 };         

   double[] hardNormals = new double[] {
      1, -1, 1,  // v0
      1, 0, 1,   // v1
      0, 1, 0,
      -1, 0, 1,  // v2
      0, 1, 0,
      -1, -1, 1, // v3
      1, -1, -1, // v4
      1, 0, -1,  // v5
      0, 1, 0,  
      -1, 0, -1, // v6
      0, 1, 0,
      -1, -1, -1 // v7
   };

   int[] hardIndicesCheck = new int[] {
      1, 3, 5,  0, 1, 5,  0, 6, 1,  1, 6, 7,  6, 11, 9, 7,  
      3, 9, 11, 5,   2, 8, 10, 4,   0, 5, 11, 6 };         

   double[] openNormals = new double[] {
      1, -1, 1,  // v0
      1, 0, 1,   // v1
      0, 1, 0,
      -1, 0, 0,  // v2
      0, 1, 0,   //
      -1, -1, 0.5, // v3
      0.5, -1, -1, // v4
      0, 0, -1,  // v5
      0, 1, 0,
      -1, 0, -1, // v6
      0, 1, 0,
      -1, -1, -1 // v7
   };

   int[] openIndicesCheck = new int[] {
      0, 1, 5,  0, 6, 1,   6, 11, 9, 7,  
      3, 9, 11, 5,   2, 8, 10, 4,   0, 5, 11, 6 };         

   public void hardEdgeNormalTest() throws IOException {

      PolygonalMesh mesh = createMesh (cubeObj, true);
      
      ArrayList<Vector3d> nrmls = mesh.getNormals();
      int[] indices = mesh.getNormalIndices();

      // check the regular, auto-created normals
      checkNormals (mesh.getNormals(), createNormals (cubeNormals));
      checkIndices (mesh.getNormalIndices(), indicesCheck);
      checkIndices (indices, mesh.createVertexIndices());

      check ("hasAutoNormalCreation=true", mesh.hasAutoNormalCreation());
      check ("getMultipleAutoNormals=true", mesh.getMultipleAutoNormals());
      check ("hasExplicitNormals=false", !mesh.hasExplicitNormals());
      check ("numHardEdges=0", mesh.numHardEdges()==0);

      // try explicitly setting normals to null:
      mesh.setNormals (null, null);
      checkNormals (mesh.getNormals(), null);
      checkIndices (mesh.getNormalIndices(), null);
      check ("hasExplicitNormals=true", mesh.hasExplicitNormals());

      mesh.clearNormals();
      // should be back to normal
      checkNormals (mesh.getNormals(), createNormals (cubeNormals));
      checkIndices (mesh.getNormalIndices(), indicesCheck);

      // now set two hard edges - that shouldn't affect anything
      // since vertices will have no more than one
      mesh.setHardEdge (1, 2, true);
      mesh.setHardEdge (6, 5, true);
      check ("numHardEdges=2", mesh.numHardEdges()==2);
      checkNormals (mesh.getNormals(), createNormals (cubeNormals));
      checkIndices (mesh.getNormalIndices(), indicesCheck);

      // now set redunant hard edges - that shouldn't affect anything
      mesh.setHardEdge (2, 1, true);
      mesh.setHardEdge (2, 1, true);
      mesh.setHardEdge (5, 6, true);
      check ("numHardEdges=2", mesh.numHardEdges()==2);

      // remove the hard edges
      mesh.setHardEdge (2, 1, false);
      mesh.setHardEdge (5, 6, false);
      check ("numHardEdges=0", mesh.numHardEdges()==0);

      check ("getWriteNormals=false", !mesh.getWriteNormals());

      // now set hard edges all around the top
      mesh.setHardEdge (2, 1, true);
      mesh.setHardEdge (5, 6, true);
      mesh.setHardEdge (5, 1, true);
      mesh.setHardEdge (2, 6, true);
      check ("numHardEdges=4", mesh.numHardEdges()==4);
      // this should result in different normals

      checkNormals (mesh.getNormals(), createNormals (hardNormals));
      checkIndices (mesh.getNormalIndices(), hardIndicesCheck);
      // check that updating normals gives the same answer
      mesh.autoUpdateNormals();
      checkNormals (mesh.getNormals(), createNormals (hardNormals));

      check ("getWriteNormals=true", mesh.getWriteNormals());

      // write out the file with edges, read it back, and see
      // that the normals and edge counts are the same
      StringWriter sw = new StringWriter();

      mesh.write (new PrintWriter(sw), new NumberFormat("%g"), false);
      PolygonalMesh readmesh = createMesh (sw.toString(), false);

      check ("readmesh.numHardEdges=4", readmesh.numHardEdges()==4);
      check ("readmesh.hasExplicitNormals=false",
                   !readmesh.hasExplicitNormals());
      check ("hasHardEdge (2,1)", readmesh.hasHardEdge (2, 1));
      check ("hasHardEdge (5,6)", readmesh.hasHardEdge (5, 6));
      check ("hasHardEdge (5,1)", readmesh.hasHardEdge (5, 1));
      check ("hasHardEdge (2,6)", readmesh.hasHardEdge (2, 6));

      checkNormals (readmesh.getNormals(), createNormals (hardNormals));
      checkIndices (readmesh.getNormalIndices(), hardIndicesCheck);

      mesh.setMultipleAutoNormals (false);
      // should now ignore hard edges when computing normals
      checkNormals (mesh.getNormals(), createNormals (cubeNormals));
      checkIndices (mesh.getNormalIndices(), indicesCheck);

      // check that updating normals gives the same answer
      mesh.autoUpdateNormals(); 
      checkNormals (mesh.getNormals(), createNormals (cubeNormals));

      // remove a face with a hard edge and check the count
      mesh.removeFace (mesh.getFace(0));

      check ("numHardEdges=3", mesh.numHardEdges()==3);
      mesh.removeFace (mesh.getFace(2));
      check ("numHardEdges=3", mesh.numHardEdges()==2);

      // check normals with face2 removed
      mesh.setMultipleAutoNormals (true);
      checkNormals (mesh.getNormals(), createNormals (openNormals));
      checkIndices (mesh.getNormalIndices(), openIndicesCheck);
   }

   float[] red = new float[] { 1f, 0f, 0f, 1f };
   float[] green = new float[] { 0f, 1f, 0f, 1f };
   float[] blue = new float[] { 0f, 0f, 1f, 1f };
   float[] cyan = new float[] { 0f, 1f, 1f, 1f };
   float[] yellow = new float[] { 1f, 1f, 0f, 1f };
   float[] magenta = new float[] { 1f, 0f, 1f, 1f };
   float[] pink = new float[] { 1f, 0.5f, 0.5f, 1f };
   float[] grey = new float[] { 0.5f, 0.5f, 0.5f, 1f };
   float[] black = new float[] { 0f, 0f, 0f, 1f };
   float[] white = new float[] { 1f, 1f, 1f, 1f };

   float[][] cubeColors = new float[][] {
      red, green, blue, cyan, yellow, magenta, pink, grey };

   public void setColorsTest() throws IOException {

      PolygonalMesh mesh = createMesh (cubeObj, true);

      // test vertex-based coloring
      mesh.setVertexColoringEnabled();
      check ("numColors=8", mesh.numColors()==8);
      check ("vertexColoringEnabled=true", mesh.getVertexColoringEnabled());
      checkIndices (mesh.getColorIndices(), mesh.createVertexIndices());
      check ("numIndices=28", mesh.getColorIndices().length==28);

      for (int i=0; i<mesh.numColors(); i++) {
         mesh.setColor (i, cubeColors[i]);
      }

      Face f0 = mesh.getFace(0);
      Face f3 = mesh.getFace(3);
      Face f6 = mesh.getFace(6);
      Face f4 = mesh.getFace(4);

      mesh.removeFace (f0);
      mesh.removeFace (f3);
      check ("numColors=8", mesh.numColors()==8);
      checkIndices (mesh.getColorIndices(), mesh.createVertexIndices());
      check ("numIndices=22", mesh.getColorIndices().length==22);

      mesh.removeFace (f6);
      mesh.removeFace (f4);
      mesh.removeVertex (mesh.getVertex(5));
      check ("numColors=7", mesh.numColors()==7);
      checkColors (
         mesh.getColors(),
         createColors (
            new float[][] {red, green, blue, cyan, yellow, pink, grey}));

      mesh.addVertex (4, 4, 4);
      checkColors (
         mesh.getColors(),
         createColors (
            new float[][] {red, green, blue, cyan, yellow, pink, grey, grey}));

      checkIndices (mesh.getColorIndices(), mesh.createVertexIndices());
      check ("numIndices=14", mesh.getColorIndices().length==14);

      mesh.clearColors();
      checkColors (mesh.getColors(), null);

      mesh = createMesh (cubeObj, true);

      mesh.setFeatureColoringEnabled();
      check ("featureColoringEnabled=true", mesh.getFeatureColoringEnabled());
      check ("vertexColoringEnabled=false", !mesh.getVertexColoringEnabled());
      check ("numColors=8", mesh.numColors()==8);
      checkIndices (mesh.getColorIndices(), mesh.createFeatureIndices());
      check ("numIndices=28", mesh.getColorIndices().length==28);

      for (int i=0; i<mesh.numColors(); i++) {
         mesh.setColor (i, cubeColors[i]);
      }

      f0 = mesh.getFace(0);
      f3 = mesh.getFace(3);
      f6 = mesh.getFace(6);
      f4 = mesh.getFace(4);

      mesh.removeFace (f0);
      mesh.removeFace (f3);
      check ("numColors=6", mesh.numColors()==6);
      checkIndices (mesh.getColorIndices(), mesh.createFeatureIndices());
      check ("numIndices=22", mesh.getColorIndices().length==22);

      mesh.removeFace (f6);
      mesh.removeFace (f4);
      mesh.removeVertex (mesh.getVertex(5));
      check ("numColors=4", mesh.numColors()==4);
      checkColors (
         mesh.getColors(),
         createColors (
            new float[][] {green, blue, magenta, grey}));

      mesh.addFace (new int[] { 3, 4, 1 });
      checkColors (
         mesh.getColors(),
         createColors (
            new float[][] {green, blue, magenta, grey, grey}));

      checkIndices (mesh.getColorIndices(), mesh.createFeatureIndices());
      check ("numIndices=17", mesh.getColorIndices().length==17);

      mesh.setColor (2, new float[] {1f, 0f, 0f, 0.5f});
      checkColor (mesh.getColor (2), new float[] {1f, 0f, 0f, 0.5f});
      mesh.setColor (2, new float[] {1f, 0f, 0f});
      checkColor (mesh.getColor (2), new float[] {1f, 0f, 0f, 1f});
   }

   String triangleStar = new String (
      "v 2.0 0.0 0.0\n" +
      "v 2.0 1.0 0.0\n" +
      "v 0.0 1.0 0.0\n" +
      "v -2.0 1.0 0.0\n" +
      "v -2.0 0.0 0.0\n" +
      "v -2.0 -1.0 0.0\n" +
      "v 0.0 -1.0 0.0\n" +
      "v 2.0 -1.0 0.0\n" +
      "v 0.0 0.0 1.0\n" +
      "f 8 0 1\n" +
      "f 8 1 2\n" +
      "f 8 2 3\n" +
      "f 8 3 4\n" +
      "f 8 4 5\n" +
      "f 8 5 6\n" +
      "f 8 6 7\n" +
      "f 8 7 0\n");

   private void checkHedges (Vertex3d vtx, int[] idxs) {
      VectorNi check = new VectorNi (idxs);
      VectorNi taili = new VectorNi ();
      HalfEdgeNode node;
      for (node=vtx.getIncidentHedges(); node!=null; node=node.next) {
         taili.append (node.he.tail.idx);
      }
      if (check.size() != taili.size() || !check.equals (taili)) {
         throw new TestException (
            "hedges incorrectly sorted. Expected:\n" + check +
            "\nGot:\n" + taili);
      }
   }

   private void testIncidentHedgeSorting() {

      PolygonalMesh mesh = createMesh (triangleStar, /*zeroIndexed=*/true);
      
      checkHedges (mesh.getVertex(8), new int[] { 1, 0, 7, 6, 5, 4, 3, 2 });

      mesh.setHardEdge (8, 0, true);
      mesh.setHardEdge (8, 3, true);
      checkHedges (mesh.getVertex(8), new int[] { 3, 2, 1, 0, 7, 6, 5, 4 });

      mesh.setHardEdge (8, 0, false);
      mesh.setHardEdge (8, 3, false);
      mesh.setHardEdge (8, 5, true);
      checkHedges (mesh.getVertex(8), new int[] { 5, 4, 3, 2, 1, 0, 7, 6 });

      Face f5 = mesh.getFace(5);
      Face f2 = mesh.getFace(2);
      Face f1 = mesh.getFace(1);
      Face f0 = mesh.getFace(0);

      mesh.removeFace (f5);
      checkHedges (mesh.getVertex(8), new int[] { 5, 4, 3, 2, 1, 0, 7 });

      mesh.removeFace (f2);
      mesh.removeFace (f1);
      checkHedges (mesh.getVertex(8), new int[] { 1, 0, 7, 5, 4 });

      mesh.removeFace (f0);
      checkHedges (mesh.getVertex(8), new int[] { 5, 4, 0, 7 });

      mesh.addFace (new int[] { 8, 1, 2});
      checkHedges (mesh.getVertex(8), new int[] { 5, 4, 0, 7, 2 });
      
      mesh.addFace (new int[] { 8, 0, 1});
      checkHedges (mesh.getVertex(8), new int[] { 5, 4, 2, 1, 0, 7 });

      // start again, this time adding an extra non-manifold face
      mesh = createMesh (triangleStar, /*zeroIndexed=*/true);
      mesh.addVertex (2, 2, 0);
      mesh.addFace (new int[] { 8, 1, 9 });

      checkHedges (mesh.getVertex(8), new int[] { 1, 0, 7, 6, 5, 4, 3, 2, 9 });
   }

   private void checkUniqueness (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {
      // make sure all the components of two meshes are unique. This is
      // to ensure that copying worked properly
      for (int i=0; i<mesh0.numVertices(); i++) {
         if (mesh0.getVertex (i) == mesh1.getVertex (i)) {
            throw new TestException (
               "vertex " + i + " is shared");
         }
      }
      for (int i=0; i<mesh0.numNormals(); i++) {
         if (mesh0.getNormal (i) == mesh1.getNormal (i)) {
            throw new TestException (
               "normal " + i + " is shared");
         }
      }
      for (int i=0; i<mesh0.numColors(); i++) {
         if (mesh0.getColor (i) == mesh1.getColor (i)) {
            throw new TestException (
               "color " + i + " is shared");
         }
      }
      for (int i=0; i<mesh0.numTextureCoords(); i++) {
         if (mesh0.getTextureCoords (i) == mesh1.getTextureCoords (i)) {
            throw new TestException (
               "textureCoord " + i + " is shared");
         }
      }
      for (int i=0; i<mesh0.numFaces(); i++) {
         if (mesh0.getFace (i) == mesh1.getFace (i)) {
            throw new TestException (
               "face " + i + " is shared");
         }
      }
   }

   private void testCopy() {
      PolygonalMesh mesh = createMesh (cubeObj, /*zeroIndexed=*/true);

      PolygonalMesh copy = mesh.copy();

      check ("cube copy", mesh.epsilonEquals (copy, 0));
      checkUniqueness (mesh, copy);

      mesh.setVertexColoringEnabled();
      for (int i=0; i<mesh.numColors(); i++) {
         mesh.setColor (i, cubeColors[i]);
      }
      mesh.setHardEdge (1, 2, true);
      mesh.setHardEdge (2, 6, true);
      mesh.setHardEdge (1, 5, true);
      mesh.setHardEdge (5, 6, true);
      mesh.getNormals(); // make sure normals are created

      copy = mesh.copy();

      check ("cube copy with hard edges & colors", mesh.epsilonEquals (copy, 0));
      checkUniqueness (mesh, copy);
   }

   protected PolygonalMesh createNewMesh() {
      return new PolygonalMesh();
   }

   private void testWriteRead() {
      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (1.2, 2);

      testWriteRead (mesh, ".obj", true);
      testWriteRead (mesh, ".obj");
      testWriteRead (mesh, ".ply");
      testWriteRead (mesh, ".gts");
      // STL check fails right now because STL files are written with
      // lower precision
      //testWriteRead (mesh, ".stl");
   }

   public void test() throws TestException, IOException {
      squareTest();
      mergeTest();      
      inertiaTest();
      setColorsTest();
      testCopy();
      testWriteRead();

      // John Lloyd, Mar 3, 2021: hard edge checks only work
      // when Vertex3d.groupHalfEdgesByHardEdge = true
      Vertex3d.groupHalfEdgesByHardEdge = true;
      hardEdgeNormalTest();
      testIncidentHedgeSorting();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      PolygonalMeshTest tester = new PolygonalMeshTest();
      tester.runtest();
   }
}
