/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;
import maspack.geometry.Vertex3d;
import java.util.*;

public class MeshFactoryTest {

   public void checkMesh (String methodName, PolygonalMesh mesh) {

      mesh.checkConsistency();
      if (!mesh.isManifold()) {
         throw new TestException (
            methodName + " returns non-manifold mesh");
      }
      if (!mesh.isClosed()) {
         throw new TestException (
            methodName + " returns non-closed mesh");
      }
      mesh.triangulate();
      mesh.checkConsistency();
      if (!mesh.isManifold()) {
         throw new TestException (
            "Triangulation of "+methodName+" is non-manifold");
      }
      if (!mesh.isClosed()) {
         throw new TestException (
            "Triangulation of "+methodName+" is non-close");
      }
   }

   public void test() {
      PolygonalMesh mesh;

      checkMesh ("createQuadBox",
                 MeshFactory.createQuadBox (1, 2, 3));
      checkMesh ("createCylinder",
                 MeshFactory.createCylinder (/* radius= */1, /* height= */2, 8));
      checkMesh ("createPointedCylinder",
                 MeshFactory.createPointedCylinder (
                    /* radius= */1, /* height= */2, /* tiph= */1, 8));
      checkMesh ("createQuadSphere",
                 MeshFactory.createQuadSphere (1.0, /* nslices= */6, 0, 0, 0));
   }
   
   void checkVolume (String name, PolygonalMesh mesh, double vol, double tol) {
      double v = mesh.computeVolume();
      if (Math.abs(v-vol) > tol) {
         throw new TestException (
            name + ": volume is " + v + ", expected " + vol);
      }
   }

   public void testBand() {
      PolygonalMesh mesh;
      double[] line = new double[] { 0, 0, 10, 0 };
      double[] square = new double[] { 0, 0, 10, 0, 10, 10, 0, 10 };
      int np = 100;
      double[] circle = new double[2*np];
      for (int i=0; i<np; i++) {
         double ang = 2*Math.PI*i/np;
         circle[2*i] = 5*Math.cos(ang);
         circle[2*i+1] = 5*Math.sin(ang);
      }
      double[] arc = Arrays.copyOf (circle, np);

      // straight line, capped: volume = len*wy*wz
      mesh = MeshFactory.createQuadBand (line, 0.7, 1, 2, 0.3, false, true);
      checkVolume ("createQuadBand(line)", mesh, 20, 1e-10);
      checkMesh ("createQuadBand(line)", mesh);
      // square loop centered on the curve
      mesh = MeshFactory.createQuadBand (square, 10, 1, 2, 0, true, false);
      checkVolume ("createQuadBand(square)", mesh, 80, 1e-10);
      checkMesh ("createQuadBand(square)", mesh);
      // square loop with offset; y points inward for a CCW curve
      mesh = MeshFactory.createQuadBand (square, 10, 1, 2, 0.5, true, false);
      checkVolume ("createQuadBand(square,offset)", mesh, 72, 1e-10);
      checkMesh ("createQuadBand(square,offset)", mesh);
      // circular loop
      mesh = MeshFactory.createQuadBand (circle, 0.5, 1, 2, 0, true, false);
      checkVolume ("createQuadBand(circle)", mesh, 2*Math.PI*5*2, 0.1);
      checkMesh ("createQuadBand(circle)", mesh);
      // open arc (49/100 of the circle), capped
      mesh = MeshFactory.createQuadBand (arc, 0.5, 1, 2, 0, false, true);
      checkVolume ("createQuadBand(arc)", mesh, 0.49*2*Math.PI*5*2, 0.1);
      checkMesh ("createQuadBand(arc)", mesh);
      // triangulated version
      mesh = MeshFactory.createBand (circle, 0.5, 1, 2, 0, true, false);
      if (!mesh.isTriangular()) {
         throw new TestException ("createBand returns non-triangular mesh");
      }
      checkVolume ("createBand(circle)", mesh, 2*Math.PI*5*2, 0.1);
      checkMesh ("createBand(circle)", mesh);
   }

   ArrayList<Point3d> toPoints3d (double[] xy) {
      ArrayList<Point3d> pnts = new ArrayList<>();
      for (int i=0; i<xy.length/2; i++) {
         pnts.add (new Point3d (xy[2*i], xy[2*i+1], 0));
      }
      return pnts;
   }

   ArrayList<Vector3d> constantNormals (int num, Vector3d nrm) {
      ArrayList<Vector3d> nrms = new ArrayList<>();
      for (int i=0; i<num; i++) {
         nrms.add (new Vector3d (nrm));
      }
      return nrms;
   }

   /**
    * Checks that the 3D band, created from a planar curve transformed by X,
    * equals the 2D band transformed by X.
    */
   void checkBandVs2d (
      String name, double[] xy, RigidTransform3d X,
      double wx, double wy, double wz, double yoffset,
      boolean closed, boolean capped) {

      PolygonalMesh mesh2d = MeshFactory.createQuadBand (
         xy, wx, wy, wz, yoffset, closed, capped);
      mesh2d.transform (X);
      ArrayList<Point3d> pnts = toPoints3d (xy);
      for (Point3d p : pnts) {
         p.transform (X);
      }
      Vector3d nrm = new Vector3d (0, 0, 1);
      nrm.transform (X);
      PolygonalMesh mesh3d = MeshFactory.createQuadBand (
         pnts, constantNormals (pnts.size(), nrm),
         wx, wy, wz, yoffset, 0, closed, capped);
      if (mesh3d.numVertices() != mesh2d.numVertices() ||
          mesh3d.numFaces() != mesh2d.numFaces()) {
         throw new TestException (
            name + ": vertex/face counts differ from 2D version");
      }
      for (int i=0; i<mesh3d.numVertices(); i++) {
         Point3d p3 = mesh3d.getVertex(i).pnt;
         Point3d p2 = mesh2d.getVertex(i).pnt;
         if (p3.distance (p2) > 1e-10) {
            throw new TestException (
               name + ": vertex "+i+" is "+p3+", expected "+p2);
         }
      }
      for (int i=0; i<mesh3d.numFaces(); i++) {
         int[] idxs3 = mesh3d.getFace(i).getVertexIndices();
         int[] idxs2 = mesh2d.getFace(i).getVertexIndices();
         if (!Arrays.equals (idxs3, idxs2)) {
            throw new TestException (
               name + ": face "+i+" differs from 2D version");
         }
      }
   }

   public void testBand3d() {
      PolygonalMesh mesh;
      double[] line = new double[] { 0, 0, 10, 0 };
      double[] square = new double[] { 0, 0, 10, 0, 10, 10, 0, 10 };
      int np = 100;
      double[] circle = new double[2*np];
      for (int i=0; i<np; i++) {
         double ang = 2*Math.PI*i/np;
         circle[2*i] = 5*Math.cos(ang);
         circle[2*i+1] = 5*Math.sin(ang);
      }
      double[] arc = Arrays.copyOf (circle, np);

      // planar curves in an arbitrary orientation should reproduce the
      // transformed 2D version
      RigidTransform3d X = new RigidTransform3d();
      X.setRandom();
      checkBandVs2d ("line", line, X, 0.7, 1, 2, 0.3, false, true);
      checkBandVs2d ("square", square, X, 10, 1, 2, 0.5, true, false);
      checkBandVs2d ("circle", circle, X, 0.5, 1, 2, 0, true, false);
      checkBandVs2d ("arc", arc, X, 0.5, 1, 2, 0.2, false, true);

      // z offset on a straight line
      ArrayList<Point3d> lpnts = toPoints3d (line);
      ArrayList<Vector3d> lnrms = constantNormals (2, new Vector3d(0,0,1));
      mesh = MeshFactory.createQuadBand (
         lpnts, lnrms, 0.7, 1, 2, 0.3, 0.4, false, true);
      checkVolume ("createQuadBand3d(line,zoffset)", mesh, 20, 1e-10);
      Point3d pmin = new Point3d();
      Point3d pmax = new Point3d();
      mesh.getLocalBounds (pmin, pmax);
      if (Math.abs(pmin.z+0.6) > 1e-10 || Math.abs(pmax.z-1.4) > 1e-10) {
         throw new TestException (
            "createQuadBand3d(line,zoffset): z bounds are "+
            pmin.z+", "+pmax.z+"; expected -0.6, 1.4");
      }
      checkMesh ("createQuadBand3d(line,zoffset)", mesh);

      // non-planar closed loop (saddle), normals radially outward
      ArrayList<Point3d> spnts = new ArrayList<>();
      ArrayList<Vector3d> snrms = new ArrayList<>();
      int ns = 200;
      double slen = 0;
      for (int i=0; i<ns; i++) {
         double ang = 2*Math.PI*i/ns;
         spnts.add (new Point3d (
            5*Math.cos(ang), 5*Math.sin(ang), 1.5*Math.cos(2*ang)));
         snrms.add (new Vector3d (Math.cos(ang), Math.sin(ang), 0));
         if (i > 0) {
            slen += spnts.get(i).distance (spnts.get(i-1));
         }
      }
      slen += spnts.get(ns-1).distance (spnts.get(0));
      mesh = MeshFactory.createQuadBand (
         spnts, snrms, 0.5, 1, 0.5, 0, 0, true, false);
      // twisting of the frame makes the side quads non-planar, so the
      // computed volume depends on the triangulation and is only approximate
      checkVolume ("createQuadBand3d(saddle)", mesh, slen*0.5, 0.2);
      checkMesh ("createQuadBand3d(saddle)", mesh);

      // open helix, capped, normals along z (not orthogonal to the curve)
      ArrayList<Point3d> hpnts = new ArrayList<>();
      ArrayList<Vector3d> hnrms = new ArrayList<>();
      int nh = 300;
      double hlen = 0;
      for (int i=0; i<nh; i++) {
         double ang = 4*Math.PI*i/(nh-1);
         hpnts.add (new Point3d (
            5*Math.cos(ang), 5*Math.sin(ang), ang/2));
         hnrms.add (new Vector3d (0, 0, 1));
         if (i > 0) {
            hlen += hpnts.get(i).distance (hpnts.get(i-1));
         }
      }
      mesh = MeshFactory.createQuadBand (
         hpnts, hnrms, 0.5, 1, 2, 0, 0.1, false, true);
      // torsion makes the side quads non-planar, so the computed volume
      // depends on the triangulation and is only approximate
      checkVolume ("createQuadBand3d(helix)", mesh, hlen*2, 1.0);
      checkMesh ("createQuadBand3d(helix)", mesh);

      // triangulated version
      mesh = MeshFactory.createBand (
         spnts, snrms, 0.5, 1, 0.5, 0, 0, true, false);
      if (!mesh.isTriangular()) {
         throw new TestException ("createBand3d returns non-triangular mesh");
      }
      checkVolume ("createBand3d(saddle)", mesh, slen*0.5, 0.2);
      checkMesh ("createBand3d(saddle)", mesh);

      // mismatched number of normals
      try {
         MeshFactory.createQuadBand (
            lpnts, constantNormals (3, new Vector3d(0,0,1)),
            0.7, 1, 2, 0, 0, false, true);
         throw new TestException (
            "createQuadBand3d: no exception for mismatched normals");
      }
      catch (IllegalArgumentException e) {
         // expected
      }
   }

   public void testMaxMin() {
      PolygonalMesh mesh = new PolygonalMesh();
      //mesh = MeshFactory.createSphere (10, 10);
      mesh = MeshFactory.createSphere (1.0, 4);
      Point3d meshMax = new Point3d();
      Point3d meshMin = new Point3d();
      mesh.getLocalBounds (meshMin, meshMax);
      
      
      for (Vertex3d v : mesh.myVertices) {
         if (v.pnt.x < meshMin.x) {
            throw new TestException ("Mesh's minimum is incorrect");
         }
         if (v.pnt.x > meshMax.x) {
            throw new TestException ("Mesh's maximum is incorrect");
         }
         if (v.pnt.y < meshMin.y) {
            throw new TestException ("Mesh's minimum is incorrect");
         }
         if (v.pnt.y > meshMax.y) {
            throw new TestException ("Mesh's maximum is incorrect");
         }
         if (v.pnt.z < meshMin.z) {
            throw new TestException ("Mesh's minimum is incorrect");
         }
         if (v.pnt.z > meshMax.z) {
            throw new TestException ("Mesh's maximum is incorrect");
         }
      }
   }
   
   
   public static void main (String[] args) {
      MeshFactoryTest tester = new MeshFactoryTest();
      try {
         tester.test();
         tester.testMaxMin();
         tester.testBand();
         tester.testBand3d();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
