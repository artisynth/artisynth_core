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

public class DelaunayInterpolator {

   int myDimen = -1;

   // structures for 3D interpolation
   BVTree myBVTree;
   PolygonalMesh myHullMesh;
   ArrayList<HalfEdge> my2DHullEdges;
   int[] my1DIndices;
   double[] my1DCoords;

   RigidTransform3d myReduceX;

   private static class Tri implements Boundable {
      Point3d myP0, myP1, myP2;
      int myIdx0, myIdx1, myIdx2;

      public Tri () {
      }

      public Tri (Point3d[] pnts, int idx0, int idx1, int idx2) {
         this();
         setPoints (pnts, idx0, idx1, idx2);
      }

      public void setPoints (
         Point3d[] pnts, int idx0, int idx1, int idx2) {
         myP0 = pnts[idx0];
         myP1 = pnts[idx1];
         myP2 = pnts[idx2];
         myIdx0 = idx0;
         myIdx1 = idx1;
         myIdx2 = idx2;
      }

      public boolean getWeights (double[] weights, Point3d pnt) {

         double m00 = myP1.x - myP0.x;
         double m10 = myP1.y - myP0.y;
         double m01 = myP2.x - myP0.x;
         double m11 = myP2.y - myP0.y;

         double denom = m00*m11 - m10*m01;

         double dx = pnt.x - myP0.x;
         double dy = pnt.y - myP0.y;

         double w1 = ( m11*dx - m01*dy)/denom;
         double w2 = (-m10*dx + m00*dy)/denom;
         
         double w0 = 1 - w1 - w2;

         if (w0 >= 0 && w1 >= 0 && w2 >= 0) {
            weights[0] = w0;
            weights[1] = w1;
            weights[2] = w2;
            return true;
         }
         else {
            return false;
         }
      }

      public void getIndices (int[] idxs) {
         idxs[0] = myIdx0;
         idxs[1] = myIdx1;
         idxs[2] = myIdx2;
      }

      public void updateBounds (Vector3d min, Vector3d max) {
         myP0.updateBounds (min, max);
         myP1.updateBounds (min, max);
         myP2.updateBounds (min, max);
      }

      public void computeCentroid (Vector3d centroid) {
         centroid.set (myP0);
         centroid.add (myP1);
         centroid.add (myP2);
         centroid.scale (1/3.0);
      }

      public double computeCovariance (Matrix3d C) {
         return -1;
      }
      
      // implementation of IndexedPointSet

      public int numPoints() {
         return 3;
      }

      public Point3d getPoint (int idx) {
         switch (idx) {
            case 0: return myP0;
            case 1: return myP1;
            case 2: return myP2;
            default: {
               throw new ArrayIndexOutOfBoundsException ("idx=" + idx);
            }
         }
      }

   }


   private static class Tet implements Boundable {
      Point3d myP0, myP1, myP2, myP3;
      int[] myIndices;

      // directions for the four faces. true and false indicate counter-clockwise
      // and clockwise with respect to the outer facing normal
      boolean myFaceDir012;
      boolean myFaceDir013;
      boolean myFaceDir023;
      boolean myFaceDir123;

      public Tet () {
      }

      public Tet (Point3d[] pnts, int idx0, int idx1, int idx2, int idx3) {
         this();
         setPoints (pnts, idx0, idx1, idx2, idx3);
      }

      public void setPoints (
         Point3d[] pnts, int i0, int i1, int i2, int i3) {

         myIndices = new int[] { i0, i1, i2, i3 };
         ArraySort.sort (myIndices);            

         int j0 = myIndices[0];
         int j1 = myIndices[1];
         int j2 = myIndices[2];
         int j3 = myIndices[3];

         myFaceDir012 = computeFaceDir (j3, j0, j1, j2, i0, i1, i2, i3);
         myFaceDir013 = computeFaceDir (j2, j0, j1, j3, i0, i1, i2, i3);
         myFaceDir023 = computeFaceDir (j1, j0, j2, j3, i0, i1, i2, i3);
         myFaceDir123 = computeFaceDir (j0, j1, j2, j3, i0, i1, i2, i3);

         myP0 = pnts[myIndices[0]];
         myP1 = pnts[myIndices[1]];
         myP2 = pnts[myIndices[2]];
         myP3 = pnts[myIndices[3]];
      }
      
      /** 
       * Returns true if the integer cycle i0, i1, i2 has the same direction as
       * j0, j1, j2.
       * 
       * @param
       * @return
       */
      private boolean directionsEqual (
         int i0, int i1, int i2, int j0, int j1, int j2) {
         
         if (i0 == j0) {
            return i1 == j1;
         }
         else if (i0 == j1) {
            return i1 == j2;
         }
         else { // i0 == j2
            return i1 == j0;
         }
      }

      private boolean computeFaceDir (
         int j0, int j1, int j2, int j3, int i0, int i1, int i2, int i3) {
         
         if (j0 == i0) {
            return directionsEqual (j1, j2, j3, i1, i2, i3);
         }
         else if (j0 == i1) {
            return directionsEqual (j1, j2, j3, i0, i3, i2);
         }
         else if (j0 == i2) {
            return directionsEqual (j1, j2, j3, i0, i1, i3);
         }
         else { // j0 == i3
            return directionsEqual (j1, j2, j3, i0, i2, i1);
         }
         
      }

      private boolean faceOrientation (int idx0, int idx1, int idx2) {
         if (idx0 < idx1) {
            if (idx0 < idx2) {
               // idx0 is the lowest index
               return idx1 < idx2;
            }
            else {
               // ordering is 2, 0, 1
               return true;
            }
         }
         else {
            if (idx1 < idx2) {
               return idx2 < idx0;
            }
            else {
               // ordering is 2, 1, 0
               return false;
            }
         }
      }

      private double faceDot (
         Point3d pnt, Point3d p1, Point3d p2, Point3d p3,
         boolean counterClockwise) {

         double d12x = p2.x - p1.x;
         double d12y = p2.y - p1.y;
         double d12z = p2.z - p1.z;

         double d13x = p3.x - p1.x;
         double d13y = p3.y - p1.y;
         double d13z = p3.z - p1.z;

         // crsx = d12 X d13
         double crsx = d12y*d13z - d12z*d13y;
         double crsy = d12z*d13x - d12x*d13z;
         double crsz = d12x*d13y - d12y*d13x;

         double prod = crsx*(pnt.x-p1.x) + crsy*(pnt.y-p1.y) + crsz*(pnt.z-p1.z);

         if (prod == 0) {
            return prod;
         }
         else {
            return counterClockwise ? prod : -prod;
         }
      }

      public boolean isInside (Point3d pnt) {

         return (faceDot (pnt, myP0, myP1, myP2, myFaceDir012) <= 0 &&
                 faceDot (pnt, myP0, myP1, myP3, myFaceDir013) <= 0 &&
                 faceDot (pnt, myP0, myP2, myP3, myFaceDir023) <= 0 &&
                 faceDot (pnt, myP1, myP2, myP3, myFaceDir123) <= 0);
      }

      public void getWeightsAndIndices (
         double[] weights, int[] indices, Point3d pnt, Matrix3d Atmp) {

         Vector3d del = new Vector3d();

         Atmp.m00 = myP1.x - myP0.x;
         Atmp.m10 = myP1.y - myP0.y;
         Atmp.m20 = myP1.z - myP0.z;

         Atmp.m01 = myP2.x - myP0.x;
         Atmp.m11 = myP2.y - myP0.y;
         Atmp.m21 = myP2.z - myP0.z;

         Atmp.m02 = myP3.x - myP0.x;
         Atmp.m12 = myP3.y - myP0.y;
         Atmp.m22 = myP3.z - myP0.z;

         Atmp.invert();
         del.sub (pnt, myP0);
         del.mul (Atmp, del);
         
         double w0 = 1 - del.x - del.y - del.z;
         weights[0] = w0;
         weights[1] = del.x;
         weights[2] = del.y;
         weights[3] = del.z;

         indices[0] = myIndices[0];
         indices[1] = myIndices[1];
         indices[2] = myIndices[2];
         indices[3] = myIndices[3];
      }

      public void updateBounds (Vector3d min, Vector3d max) {
         myP0.updateBounds (min, max);
         myP1.updateBounds (min, max);
         myP2.updateBounds (min, max);
         myP3.updateBounds (min, max);
      }

      public void computeCentroid (Vector3d centroid) {
         centroid.set (myP0);
         centroid.add (myP1);
         centroid.add (myP2);
         centroid.add (myP3);
         centroid.scale (0.25);
      }
      
      public double computeCovariance (Matrix3d C) {
         return -1;
      }

      // implementation of IndexedPointSet

      public int numPoints() {
         return 4;
      }

      public Point3d getPoint (int idx) {
         switch (idx) {
            case 0: return myP0;
            case 1: return myP1;
            case 2: return myP2;
            case 3: return myP2;
            default: {
               throw new ArrayIndexOutOfBoundsException ("idx=" + idx);
            }
         }
      }


   }

   public DelaunayInterpolator () {
   }

   public DelaunayInterpolator (Point3d[] pnts) {
      setPoints (pnts);
   }


   public void setPoints (Point3d[] pnts) {

      int nump = pnts.length;
      double[] pntCoords = new double[3*nump];
      for (int i=0; i<nump; i++) {
         pntCoords[3*i  ] = pnts[i].x;
         pntCoords[3*i+1] = pnts[i].y;
         pntCoords[3*i+2] = pnts[i].z;
      }
      setPoints (pntCoords);
   }

   public void setPoints (double[] pntCoords) { 

      TetgenTessellator tessellator = new TetgenTessellator();
      myDimen = -1;
      int nump = pntCoords.length/3;
      int dimen = tessellator.buildFromPoints (pntCoords);

      switch (dimen) {
         case -1: {
            throw new IllegalArgumentException (
               "Cannot create tesellation from specified "+nump+" points");
         }
         case 0: {
            break;
         }
         case 1: {
            create1DInterpolationStructure (tessellator);
            break;
         }
         case 2: {
            create2DInterpolationStructure (tessellator);
            break;
         }
         case 3: {
            create3DInterpolationStructure (tessellator);
            break;
         }
      }
      myDimen = dimen;
   }

   private void create1DInterpolationStructure (TetgenTessellator tessellator) {

      my1DIndices = tessellator.get1DIndices();
      my1DCoords = tessellator.get1DCoords();

      myReduceX = new RigidTransform3d (tessellator.getReductionTransform());
   }

   private void addBoundaryEdges (ArrayList<HalfEdge> edges, Face face) {
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         if (he.opposite == null) {
            edges.add (he);
         }
         he = he.getNext();
      }
      while (he != he0);
   }
   
   private void create2DInterpolationStructure (TetgenTessellator tessellator) {

      int[] fidxs = tessellator.getHullFaces();
      Point3d[] pnts = tessellator.getPoints();
      int numFaces = fidxs.length/3;
      Boundable[] facelist = new Boundable[numFaces];
      for (int k=0; k<fidxs.length; k += 3) {
         facelist[k/3] = new Tri (
            pnts, fidxs[k], fidxs[k+1], fidxs[k+2]);
      }
      AABBTree aabbTree = new AABBTree();
      aabbTree.build (facelist, numFaces);
      myBVTree = aabbTree;

      PolygonalMesh mesh = new PolygonalMesh();
      for (int i=0; i<pnts.length; i++) {
         pnts[i].z = 0;
         mesh.addVertex (pnts[i]);
      }
      // Tetgen's hull faces are clockwise, but since in this case thay
      // are facing down into the plane, they will be counter-clockwise
      // when seen from +z.
      int[] idxs = new int[3];
      for (int k=0; k<fidxs.length; k += 3) {
         idxs[0] = fidxs[k];
         idxs[1] = fidxs[k+1];
         idxs[2] = fidxs[k+2];
         mesh.addFace (idxs);
      }
      my2DHullEdges = new ArrayList<HalfEdge>();
      ArrayList<Face> faces = mesh.getFaces();
      for (int i=0; i<faces.size(); i++) {
         addBoundaryEdges (my2DHullEdges, faces.get(i));
      }
      myReduceX = new RigidTransform3d (tessellator.getReductionTransform());
   }
   
   private void create3DInterpolationStructure (TetgenTessellator tessellator) {

      int[] tidxs = tessellator.getTets();
      Point3d[] pnts = tessellator.getPoints();
      int numTets = tidxs.length/4;
      Boundable[] tetlist = new Boundable[numTets];
      for (int k=0; k<tidxs.length; k += 4) {
         tetlist[k/4] = new Tet (
            pnts, tidxs[k], tidxs[k+1], tidxs[k+2], tidxs[k+3]);
      }
      AABBTree aabbTree = new AABBTree();
      aabbTree.build (tetlist, numTets);
      myBVTree = aabbTree;

      myHullMesh = new PolygonalMesh();
      for (int i=0; i<pnts.length; i++) {
         myHullMesh.addVertex (pnts[i]);
      }
      int[] hullIdxs = tessellator.getHullFaces();
      int[] idxs = new int[3];
      for (int k=0; k<hullIdxs.length; k += 3) {
         idxs[0] = hullIdxs[k];
         idxs[1] = hullIdxs[k+1];
         idxs[2] = hullIdxs[k+2];
         myHullMesh.addFace (idxs);
      }
   }

   private void get1DInterpolation (double[] weights, int[] idxs, Point3d pnt) {

      Point3d pnt1D = new Point3d();
      pnt1D.inverseTransform (myReduceX, pnt);
      double x = pnt1D.x;

      int nump = my1DCoords.length;
      int idx0 = -1;
      int idx1 = -1;
      for (int i=0; i<nump; i++) {
         idx1 = my1DIndices[i];
         if (x <= my1DCoords[idx1]) {
            if (idx0 == -1) {
               weights[0] = 1;
               idxs[0] = idx1;
            }
            else {
               double x0 = my1DCoords[idx0];
               double x1 = my1DCoords[idx1];
               weights[1] = (x-x0)/(x1-x0);
               weights[0] = 1 - weights[1];
               idxs[0] = idx0;
               idxs[1] = idx1;
            }
            return;
         }
         idx0 = idx1;
      }
      weights[0] = 1;
      idxs[0] = idx1;
   }

   private void getEdgeWeightsAndIndices (
      double[] weights, int[] indices, HalfEdge he, Point3d pnt) {

      Point3d phead = he.getHead().pnt;
      Point3d ptail = he.getTail().pnt;

      double ux = phead.x - ptail.x;
      double uy = phead.y - ptail.y;

      double dx = pnt.x - ptail.x;
      double dy = pnt.y - ptail.y;

      double dot = dx*ux + dy*uy;
      if (dot < 0) {
         indices[0] = he.getTail().getIndex();
         weights[0] = 1;
         return;
      }
      double ulenSqr = ux*ux + uy*uy;
      if (dot > ulenSqr) {
         indices[0] = he.getHead().getIndex();
         weights[0] = 1;
      }
      else {
         weights[1] = dot/ulenSqr;
         weights[0] = 1 - weights[1];
         indices[0] = he.getTail().getIndex();
         indices[1] = he.getHead().getIndex();
      }
   }

   private double distSquaredToEdge (HalfEdge he, Point3d pnt) {
      Point3d phead = he.getHead().pnt;
      Point3d ptail = he.getTail().pnt;

      double ux = phead.x - ptail.x;
      double uy = phead.y - ptail.y;

      double dx, dy, dot, cross;

      // check head first
      dx = pnt.x - phead.x;
      dy = pnt.y - phead.y;
      dot = dx*ux + dy*uy;
      if (dot > 0) {
         return dx*dx + dy*dy;
      }
      // now check tail
      dx = pnt.x - ptail.x;
      dy = pnt.y - ptail.y;
      dot = dx*ux + dy*uy;
      if (dot < 0) {
         return dx*dx + dy*dy;
      }
      // closest to edge proper:
      cross = ux*dy - uy*dx;
      return cross*cross/(ux*ux + uy*uy);
   }

   private void get2DInterpolation (double[] weights, int[] idxs, Point3d pnt) {
      Point3d pnt2D = new Point3d();
      pnt2D.inverseTransform (myReduceX, pnt);
      pnt2D.z = 0;

      ArrayList<BVNode> nodes = new ArrayList<BVNode> (16);
      myBVTree.intersectPoint (nodes, pnt2D);
      if (nodes.size() > 0) {
         for (BVNode n : nodes) {
            Boundable[] elements = n.getElements();
            for (int i=0; i<elements.length; i++) {
               Tri tri = (Tri)elements[i];
               if (tri.getWeights (weights, pnt2D)) {
                  tri.getIndices (idxs);
                  return;
               }
            }
         }
      }
      // pnt is not inside any of the tets - must look for its projection
      // on the convex hull boundary
      HalfEdge closestHe = null;
      double minDsqr = Double.POSITIVE_INFINITY;
      for (int i=0; i<my2DHullEdges.size(); i++) {
         HalfEdge he = my2DHullEdges.get(i);
         double dsqr = distSquaredToEdge (he, pnt2D);
         if (dsqr < minDsqr) {
            minDsqr = dsqr;
            closestHe = he;
         }
      }
      getEdgeWeightsAndIndices (weights, idxs, closestHe, pnt2D);
   }
   
   private void get3DInterpolation (double[] weights, int[] idxs, Point3d pnt) {
      Matrix3d A = new Matrix3d();

      ArrayList<BVNode> nodes = new ArrayList<BVNode> (16);
      myBVTree.intersectPoint (nodes, pnt);
      // System.out.println ("num nodes " + nodes.size());
      if (nodes.size() > 0) {
         for (BVNode n : nodes) {
            Boundable[] elements = n.getElements();
            for (int i=0; i<elements.length; i++) {
               Tet tet = (Tet)elements[i];
               if (tet.isInside (pnt)) {
                  tet.getWeightsAndIndices (weights, idxs, pnt, A);
                  return;
               }
            }
         }
      }
      // pnt is not inside any of the tets - must look on the surface.
      Point3d res = new Point3d();
      Vector2d coords = new Vector2d();
      Face face = BVFeatureQuery.getNearestFaceToPoint (
         res, coords, myHullMesh, pnt);
      //Face face = obbt.nearestFace (pnt, null, res, coords, new TriangleIntersector());
      if (face == null) {
         throw new InternalErrorException (
            "Unable to locate nearest face on convex hull");
      }
      weights[0] = 1 - coords.x - coords.y;
      weights[1] = coords.x;
      weights[2] = coords.y;

      idxs[0] = face.getVertex(0).getIndex();
      idxs[1] = face.getVertex(1).getIndex();
      idxs[2] = face.getVertex(2).getIndex();
   }
   
   public void getInterpolation (double[] weights, int[] idxs, Point3d pnt) {

      // default values for weights and indices in the range [1-3],
      // which may be undefined:
      weights[1] = 0;
      weights[2] = 0;
      weights[3] = 0;
      idxs[1] = -1;
      idxs[2] = -1;
      idxs[3] = -1;

      switch (myDimen) {
         case -1: {
            throw new IllegalStateException (
               "Interpolator is not properly initialized");
         }
         case 0: {
            weights[0] = 1;
            idxs[0] = 0;
            break;
         }
         case 1: {
            get1DInterpolation (weights, idxs, pnt);
            break;
         }
         case 2: {
            get2DInterpolation (weights, idxs, pnt);
            break;
         }
         case 3: {
            get3DInterpolation (weights, idxs, pnt);
            break;
         }
      }
   }

   private static Point3d createTetPoint (
      Point3d[] pnts, double w0, double w1, double w2, double w3) {

      Point3d pnt = new Point3d();
      pnt.scale (w0, pnts[0]);
      pnt.scaledAdd (w1, pnts[1], pnt);
      pnt.scaledAdd (w2, pnts[2], pnt);
      pnt.scaledAdd (w3, pnts[3], pnt);
      return pnt;
   }

   public static void main (String[] args) {
      Point3d[] pnts0 = new Point3d[5];
      Point3d[] pntsx = new Point3d[5];

      pnts0[0] = new Point3d();
      pnts0[1] = new Point3d(1, 0, 0);
      pnts0[2] = new Point3d(0, 1, 0);
      pnts0[3] = new Point3d(0, 0, 1);
      pnts0[4] = new Point3d(1, 1, 1);

      for (int i=0; i<5; i++) {
         pntsx[i] = new Point3d();
      }

      RandomGenerator.setSeed (0x1234);
      Random randGen = RandomGenerator.get();
      int numTrials = 1000000;

      AffineTransform3d X = new AffineTransform3d();
      for (int k=0; k<numTrials; k++) {

         X.A.setRandom();
         X.A.mulTranspose (X.A);
         X.p.setRandom();

         double w1 = randGen.nextDouble()/2;
         double w2 = randGen.nextDouble()/2;
         double w3 = 1 - w1 - w2;
         Point3d pnt = createTetPoint (pnts0, 0, w1, w2, w3);

         pnt.transform (X);

         for (int i=0; i<5; i++) {
            pntsx[i].transform (X, pnts0[i]);
         }

         DelaunayInterpolator.Tet tet1 = new DelaunayInterpolator.Tet (
            pntsx, 0, 1, 2, 3);
         DelaunayInterpolator.Tet tet2 = new DelaunayInterpolator.Tet (
            pntsx, 4, 2, 1, 3);

         boolean insideTet1 = tet1.isInside (pnt);
         boolean insideTet2 = tet2.isInside (pnt);

         if (!insideTet1 && !insideTet2) {
            System.out.println ("NOT OK");
         }
         else {
            System.out.println ("OK");
         }
         
      }
   }
}

