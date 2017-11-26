/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;


import java.awt.Color;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.*;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.Matrix3d;
import maspack.geometry.DistanceGrid.TetID;
import maspack.geometry.DistanceGrid.TetDesc;
import maspack.render.PointLineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.Shading;
import maspack.util.StringHolder;
import maspack.util.DoubleHolder;
import maspack.util.ArraySupport;
import maspack.util.QuadraticSolver;
import maspack.util.InternalErrorException;

/**
 * Worker class that performs computations specifically related to the
 * intersection of a plane and the surface of a quadratically interpolated
 * distance grid.
 */
public class DistanceGridSurfCalc {

   DistanceGrid myGrid;

   public enum PlaneType {
      YZ,
      ZX,
      XY
   };

   public DistanceGridSurfCalc (DistanceGrid grid) {
      myGrid = grid;
   }

   protected static HashMap<TetFace,TetDesc> myFaceTets;
   protected static HashMap<TetEdge,TetDesc[]> myEdgeTets;
   protected static ArrayList<TetDesc[]> myNodeTets;

   public static class TetFeature {
   }

   public static class TetNode extends TetFeature {
      int myNode0;

      public TetNode (int node0) {
         myNode0 = node0;
      }

      public int getNode() {
         return myNode0;
      }  

      public String toString() {
         return ""+myNode0;
      }
   }

   public static class TetEdge extends TetFeature {
      int myNode0;
      int myNode1;

      TetEdge (int node0, int node1) {
         if (node0 > node1) {
            myNode0 = node1;
            myNode1 = node0;
         }
         else {
            myNode0 = node0;
            myNode1 = node1;
         }
      }

      public boolean equals (Object obj) {
         if (obj instanceof TetEdge) {
            TetEdge edge = (TetEdge)obj;
            return edge.myNode0 == myNode0 && edge.myNode1 == myNode1;
         }
         else {
            return false;
         }
      }

      public int hashCode() {
         return myNode0 + 11*myNode1;
      }

      public String toString() {
         return "" + myNode0 + "-" + myNode1;
      }
   }

   private static class TetFace extends TetFeature {
      int myNode0;
      int myNode1;
      int myNode2;

      TetFace (int node0, int node1, int node2) {
         Vector3i nodes = new Vector3i (node0, node1, node2);
         int minIdx = nodes.minAbsIndex();
         myNode0 = nodes.get(minIdx);
         myNode1 = nodes.get((minIdx+1)%3);
         myNode2 = nodes.get((minIdx+2)%3);
      }

      public boolean equals (Object obj) {
         if (obj instanceof TetFace) {
            TetFace edge = (TetFace)obj;
            return (edge.myNode0 == myNode0 &&
                    edge.myNode1 == myNode1 &&
                    edge.myNode2 == myNode2);
         }
         else {
            return false;
         }
      }

      public int hashCode() {
         return myNode0 + 11*myNode1 + 71*myNode2;
      }

      public String toString() {
         return "" + myNode0 + "-" + myNode1 + "-" + myNode2;
      }
   }

   int findRefVertex (Vector3i xyzi) {
      for (int i=0; i<DistanceGrid.myBaseQuadCellXyzi.length; i++) {
         if (xyzi.equals(DistanceGrid.myBaseQuadCellXyzi[i])) {
            return i;
         }
      }
      return -1;
   }

   protected Vector3i createEdgeNode (Vector3i v0, Vector3i v1) {
      Vector3i en = new Vector3i();
      en.x = (v0.x+v1.x)/2;
      en.y = (v0.y+v1.y)/2;
      en.z = (v0.z+v1.z)/2;
      return en;
   }

   void maybeAddFace (
      HashMap<TetFace,TetDesc> faceTets,
      Vector3i vi0, Vector3i vi1, Vector3i vi2, TetDesc tdesc) {
      
      int ref0, ref1, ref2;
      if ((ref0=findRefVertex (vi0)) != -1 &&
          (ref1=findRefVertex (vi1)) != -1 &&
          (ref2=findRefVertex (vi2)) != -1) {
         TetFace tface = new TetFace (ref0, ref2, ref1);
         if (faceTets.get (tdesc) != null) {
            System.out.println (
               "Error: face "+tface+" already assigned tet " +
               faceTets.get (tface));
         }
         faceTets.put (tface, tdesc);
      }
   }

   void maybeAddEdge (
      HashMap<TetEdge,HashSet<TetDesc>> edgeTets,
      Vector3i vi0, Vector3i vi1, TetDesc tdesc) {
      
      int ref0, ref1;
      if ((ref0=findRefVertex (vi0)) != -1 &&
          (ref1=findRefVertex (vi1)) != -1) {
         TetEdge tedge = new TetEdge (ref0, ref1);
         HashSet<TetDesc> tets = edgeTets.get(tedge);
         if (tets == null) {
            tets = new HashSet<TetDesc>();
            edgeTets.put (tedge, tets);
         }
         if (tets.contains(tdesc)) {
            System.out.println (
               "Error: edge "+tedge+" already assigned tet " + tdesc);
         }
         tets.add (tdesc);
      }
   }

   private void createConnectivity () {
      myFaceTets = new HashMap<TetFace,TetDesc>();

      ArrayList<HashSet<TetDesc>> nodeTets = new ArrayList<HashSet<TetDesc>>();
      for (int i=0; i<8; i++) {
         nodeTets.add (new HashSet<TetDesc>());
      }
      HashMap<TetEdge,HashSet<TetDesc>> edgeTets =
         new HashMap<TetEdge,HashSet<TetDesc>>();

      for (int i=-1; i<=1; i++) {
         for (int j=-1; j<=1; j++) {
            for (int k=-1; k<=1; k++) {
               for (TetID tetId : TetID.values()) {
                  TetDesc tdesc =
                     new TetDesc (new Vector3i (i, j, k), tetId);
                  Vector3i[] verts = tdesc.getVertices ();
                  for (int vi=0; vi<verts.length; vi++) {
                     int refVtx = findRefVertex (verts[vi]);
                     if (refVtx != -1) {
                        nodeTets.get(refVtx).add (tdesc);
                     }
                  }
                  maybeAddFace (myFaceTets, verts[0], verts[1], verts[2], tdesc);
                  maybeAddFace (myFaceTets, verts[0], verts[2], verts[3], tdesc);
                  maybeAddFace (myFaceTets, verts[2], verts[1], verts[3], tdesc);
                  maybeAddFace (myFaceTets, verts[1], verts[0], verts[3], tdesc);

                  maybeAddEdge (edgeTets, verts[0], verts[1], tdesc);
                  maybeAddEdge (edgeTets, verts[1], verts[2], tdesc);
                  maybeAddEdge (edgeTets, verts[2], verts[0], tdesc);
                  maybeAddEdge (edgeTets, verts[0], verts[3], tdesc);
                  maybeAddEdge (edgeTets, verts[1], verts[3], tdesc);
                  maybeAddEdge (edgeTets, verts[2], verts[3], tdesc);
               }
            }
         }
      }

      myNodeTets = new ArrayList<TetDesc[]>();
      for (int i=0; i<nodeTets.size(); i++) {
         myNodeTets.add (nodeTets.get(i).toArray(new TetDesc[0]));
      }

      myEdgeTets = new HashMap<TetEdge,TetDesc[]>();
      for (TetEdge edge : edgeTets.keySet()) {
         myEdgeTets.put (edge, edgeTets.get(edge).toArray(new TetDesc[0]));
      }
   }

   private void createConnectivityIfNecessary() {
      if (myFaceTets == null) {
         createConnectivity();
      }
   }

   void printConnectivity() {
      
      System.out.println ("Nodes:");
      for (int i=0; i<8; i++) {
         TetDesc[] tdescs = myNodeTets.get(i);
         System.out.print (" "+i+"("+tdescs.length+") : ");
         for (TetDesc tdesc : tdescs) {
            System.out.print (tdesc + " ");
         }
         System.out.println ("");
      }

      System.out.println ("Edges:");
      for (TetEdge edge : myEdgeTets.keySet()) {
         System.out.print (" "+edge+": ");
         for (TetDesc tdesc : myEdgeTets.get(edge)) {
            System.out.print (tdesc + " ");
         }
         System.out.println ("");
      }
      
      System.out.println ("Faces:");
      for (TetFace face : myFaceTets.keySet()) {
         System.out.println (
            " "+face+": " + myFaceTets.get(face));
      }
   }         

   public static class TetPlaneIntersection {
      private static final double EPS = 1e-12;

      TetDesc myTetDesc;
      public int myNumSides;
      int myN0;
      int myN1;
      int myN2;
      int myN3;

      double myS0;
      double myS1;
      double myS2;
      double myS3;

      public Point3d myP0;
      public Point3d myP1;
      public Point3d myP2;
      public Point3d myP3;

      int myCCW; // 1 or -1 if the intersection is CCW or CW WRT the plane

      public TetPlaneIntersection () {
         myP0 = new Point3d();
         myP1 = new Point3d();
         myP2 = new Point3d();
         myP3 = new Point3d();
      }

      double triS (int i) {
         switch (i%3) {
            case 0: return myS1;
            case 1: return myS2;
            case 2: return myS3;
            default: {
               // can't get here
               return 0;
            }
         }         
      }

      int triN (int i) {
         switch (i%3) {
            case 0: return myN1;
            case 1: return myN2;
            case 2: return myN3;
            default: {
               // can't get here
               return 0;
            }
         }         
      }

     TetFeature getTriangleFeature (double s, int ei) {
         if (s <= EPS) {
            double s1 = triS(ei);
            // must lie on edge myN0, N(ei)
            if (s1 <= EPS) {
               return new TetNode (myN0);
            }
            else if (s1 >= 1-EPS) {
               return new TetNode (triN(ei));
            }
            else {
               return new TetEdge (myN0, triN(ei));
            }
         }
         else if (s >= 1-EPS) {
            double s2 = triS(ei+1);
            // must lie on edge myN0, N(ei+1)
            if (s2 <= EPS) {
               return new TetNode (myN0);
            }
            else if (s2 >= 1-EPS) {
               return new TetNode (triN(ei+1));
            }
            else {
               return new TetEdge (myN0, triN(ei+1));
            }
         }
         else {
            double s1 = triS(ei);
            double s2 = triS(ei+1);
            if (s1 >= 1-EPS && s2 >= 1-EPS) {
               // lies on edge N(ei), N(ei+1)
               return new TetEdge (triN(ei), triN(ei+1));
            }
            else {
               // lies on face myN0,  N(ei+1), N(ei)
               return new TetFace (myN0, triN(ei+1), triN(ei));
            }
         }
      }

      double quadS (int i) {
         switch (i%4) {
            case 0: return myS0;
            case 1: return myS1;
            case 2: return myS2;
            case 3: return myS3;
            default: {
               // can't get here
               return 0;
            }
         }         
      }

      int quadN (int i) {
         switch (i%4) {
            case 0: return myN0;
            case 1: return myN1;
            case 2: return myN2;
            case 3: return myN3;
            default: {
               // can't get here
               return 0;
            }
         }         
      }

      TetFeature getQuadFeature (double s, int ei) {
         if (s <= EPS) {
            // must lie on edge N(ei), N(ei+1)
            double s1 = quadS(ei+1);
            if (s1 <= EPS) {
               return new TetNode (quadN(ei));
            }
            else if (s1 >= 1-EPS) {
               return new TetNode (quadN(ei+1));
            }
            else {
               return new TetEdge (quadN(ei), quadN(ei+1));
            }
         }
         else if (s >= 1-EPS) {
            // must lie on edge N(ei+1), N(ei+2)
            double s2 = quadS(ei+2);
            if (s2 <= EPS) {
               return new TetNode (quadN(ei+1));
            }
            else if (s2 >= 1-EPS) {
               return new TetNode (quadN(ei+2));
            }
            else {
               return new TetEdge (quadN(ei+1), quadN(ei+2));
            }
         }
         else {
            double s1 = quadS(ei+1);
            if (s1 >= 1-EPS) {
               // then s2 must be close to 0, with intersection at N(ei+1)
               return new TetNode (quadN(ei+1));
            }
            else {
               int n0 = quadN(ei);
               int n1 = quadN(ei+1);
               int n2 = quadN(ei+2);
               if (n0 == myN0 || n0 == myN2) {
                  return new TetFace (n0, n1, n2);
               }
               else {
                  // need to flip
                  return new TetFace (n0, n2, n1);
               }
            }
         }
      }

      TetFeature getFeature (double s, int ei) {
         if (myNumSides == 3) {
            return getTriangleFeature (s, ei);
         }
         else if (myNumSides == 4) {
            return getQuadFeature (s, ei);
         }
         else {
            throw new InternalErrorException (
               "Uninitialized myNumSides field of TetPlaneIntersection");
         }
      }

      int getNode (int nidx) {
         switch (nidx) {
            case 0: return myN0;
            case 1: return myN1;
            case 2: return myN2;
            case 3: return myN3;
            default: {
               throw new InternalErrorException (
                  "nidx=" + nidx + ", must be in the range [0,3]");
            }
         }
      }

      boolean clipRange (
         double[] rng, DoubleHolder edgeS, int ccw, 
         double ax, double ay, double bx, double by, double cx, double cy) {

         boolean clippedUpper = false;
         double aXb = ax*by - ay*bx;
         if (aXb != 0) {
            // double s = (cx*by - cy*bx)/aXb;
            // if (s >= 0.0 && s <= 1.0) {
               double t = (cx*ay - cy*ax)/aXb;
               if (ccw*aXb > 0) {
                  // solution is lower bound
                  if (t > rng[0]) {
                     rng[0] = t;
                  }
               }
               else {
                  // solution is upper bound
                  if (t < rng[1]) {
                     rng[1] = t;
                     edgeS.value = (cx*by - cy*bx)/aXb;
                     clippedUpper = true;
                  }
               }
               //}
         }
         return clippedUpper;
      }

      int intersectRay (
         double[] rng,
         DoubleHolder edgeS, Point3d q, Vector3d dir,
         PlaneType planeType, int planeSgn) {
         
         Point3d[] pi = new Point3d[] {
            myP0, myP1, myP2, myP3};

         int[] ni;
         if (myNumSides == 4) {
            ni = new int[] { myN0, myN1, myN2, myN3};
         }
         else {
            ni = new int[] { myN1, myN2, myN3};
         }

         Point3d p0 = pi[0];
         int upperIdx = -1;
         int ccw = planeSgn*myCCW;
         for (int i=0; i<myNumSides; i++) {
            Point3d p1 = i<myNumSides-1 ? pi[i+1] : pi[0];
            switch (planeType) {
               case YZ: {
                  if (clipRange (
                         rng, edgeS, ccw, p1.y-p0.y, p1.z-p0.z,
                         dir.y, dir.z, q.y-p0.y, q.z-p0.z)) {
                     upperIdx = i;
                  }
                  break;
               }
               case ZX: {
                  if (clipRange (
                         rng, edgeS, ccw, p1.z-p0.z, p1.x-p0.x,
                         dir.z, dir.x, q.z-p0.z, q.x-p0.x)) {
                     upperIdx = i;
                  }
                  break;
               }
               case XY: {
                  if (clipRange (
                         rng, edgeS, ccw, p1.x-p0.x, p1.y-p0.y,
                         dir.x, dir.y, q.x-p0.x, q.y-p0.y)) {
                     upperIdx = i;
                  }
                  break;
               }
            }
            p0 = p1;
         }
         return upperIdx;
      }

      void updateRange (
         double[] rng, double ax, double ay, double bx, double by) {

         double aXb = ax*by - ay*bx;
         if (aXb < rng[0]) {
            rng[0] = aXb;
         }
         else if (aXb > rng[1]) {
            rng[1] = aXb;
         }
      }

      double tanIntersectOverlap (
         Point3d q, Vector3d grad, PlaneType planeType) {

         double[] rng = new double[2];

         switch (planeType) {
            case YZ: {
               updateRange (rng, -grad.z, grad.y, myP0.y-q.y, myP0.z-q.z);
               updateRange (rng, -grad.z, grad.y, myP1.y-q.y, myP1.z-q.z);
               updateRange (rng, -grad.z, grad.y, myP2.y-q.y, myP2.z-q.z);
               if (myNumSides == 4) {
                  updateRange (rng, -grad.z, grad.y, myP3.y-q.y, myP3.z-q.z);
               }
               break;
            }
            case ZX: {
               updateRange (rng, -grad.x, grad.z, myP0.z-q.z, myP0.x-q.x);
               updateRange (rng, -grad.x, grad.z, myP1.z-q.z, myP1.x-q.x);
               updateRange (rng, -grad.x, grad.z, myP2.z-q.z, myP2.x-q.x);
               if (myNumSides == 4) {
                  updateRange (rng, -grad.x, grad.z, myP3.z-q.z, myP3.x-q.x);
               }
               break;
            }
            case XY: {
               updateRange (rng, -grad.y, grad.x, myP0.x-q.x, myP0.y-q.y);
               updateRange (rng, -grad.y, grad.x, myP1.x-q.x, myP1.y-q.y);
               updateRange (rng, -grad.y, grad.x, myP2.x-q.x, myP2.y-q.y);
               if (myNumSides == 4) {
                  updateRange (rng, -grad.y, grad.x, myP3.x-q.x, myP3.y-q.y);
               }
               break;
            }
         }
         if (rng[0]*rng[1] < 0) {
            return Math.min(Math.abs(rng[0]), Math.abs(rng[1]));
         }
         else {
            return -Math.min(Math.abs(rng[0]), Math.abs(rng[1]));
         }
      }
   }

   void intersectTriangle (
      TetPlaneIntersection isect,
      int n0, double d0, Point3d p0, int n1, double d1, Point3d p1, 
      int n2, double d2, Point3d p2, int n3, double d3, Point3d p3) {
      
      double EPS = 1e-8;

      double s1 = d0/(d0-d1);
      double s2 = d0/(d0-d2);
      double s3 = d0/(d0-d3);

      int numsmall = 0;
      if (s1 <= EPS) {
         numsmall++;
      }
      if (s2 <= EPS) {
         numsmall++;
      }
      if (s3 <= EPS) {
         numsmall++;
      }
      if (numsmall > 1) {
         isect.myNumSides = 0;
      }
      isect.myN0 = n0;
      isect.myP0.combine (1-s1, p0, s1, p1);
      isect.myN1 = n1;
      isect.myS1 = s1;
      isect.myP1.combine (1-s2, p0, s2, p2);
      isect.myN2 = n2;
      isect.myS2 = s2;
      isect.myP2.combine (1-s3, p0, s3, p3);
      isect.myN3 = n3;
      isect.myS3 = s3;
      isect.myNumSides = 3;
      isect.myCCW = d0 < 0 ? 1 : -1;
   }

   void intersectQuad (
      TetPlaneIntersection isect,
      int n0, double d0, Point3d p0, int n1, double d1, Point3d p1, 
      int n2, double d2, Point3d p2, int n3, double d3, Point3d p3) {
      
      double EPS = 1e-8;

      double s1 = d0/(d0-d1);
      double s2 = d1/(d1-d2);
      double s3 = d2/(d2-d3);
      double s0 = d3/(d3-d0);

      if (s1 <= EPS) {
         if (s3 <= EPS) {
            // quad meets at points p0 and p2, so is degenerate
            isect.myNumSides = 0;
         }
      }
      else if (s2 <= EPS) {
         if (s0 < EPS) {
            // quad meets at points p1 and p3, so is degenerate
            isect.myNumSides = 0;
         }
      }
      isect.myN0 = n0;
      isect.myS0 = s0;
      isect.myP0.combine (1-s1, p0, s1, p1);
      isect.myN1 = n1;
      isect.myS1 = s1;
      isect.myP1.combine (1-s2, p1, s2, p2);
      isect.myN2 = n2;
      isect.myS2 = s2;
      isect.myP2.combine (1-s3, p2, s3, p3);
      isect.myN3 = n3;
      isect.myS3 = s3;
      isect.myP3.combine (1-s0, p3, s0, p0);
      isect.myNumSides = 4;
      isect.myCCW = d0 > 0 ? 1 : -1;
   }

   public boolean intersectTetAndPlane (
      TetPlaneIntersection isect, TetDesc tdesc, Point3d[] vpnts, Plane plane) {

      Point3d p0 = vpnts[0];
      Point3d p1 = vpnts[1];
      Point3d p2 = vpnts[2];
      Point3d p3 = vpnts[3];

      double d0 = plane.distance(vpnts[0]);
      double d1 = plane.distance(vpnts[1]);
      double d2 = plane.distance(vpnts[2]);
      double d3 = plane.distance(vpnts[3]);

      int[] nodes = tdesc.myTetId.getNodes();
      int n0 = nodes[0];
      int n1 = nodes[1];
      int n2 = nodes[2];
      int n3 = nodes[3];
      
      int sgn0 = (d0 >= 0 ? 1 : -1);
      int sgn1 = (d1 >= 0 ? 1 : -1);
      int sgn2 = (d2 >= 0 ? 1 : -1);
      int sgn3 = (d3 >= 0 ? 1 : -1);

      if (sgn0*sgn1 == 1) {
         if (sgn0*sgn2 == 1) {
            if (sgn0*sgn3 == 1) {
               // + + +   nothing
               isect.myNumSides = 0;
            }
            else {
               // + + -   3-0 3-1 3-2
               intersectTriangle (
                  isect, n3, d3, p3, n0, d0, p0, n1, d1, p1, n2, d2, p2);
            }
         }
         else {
            if (sgn0*sgn3 == 1) {
               // + - +   2-0 2-3 2-1
               intersectTriangle (
                  isect, n2, d2, p2, n0, d0, p0, n3, d3, p3, n1, d1, p1);
            }
            else {
               // + - -   0-3 3-1 1-2 2-0
               intersectQuad (
                  isect, n0, d0, p0, n3, d3, p3, n1, d1, p1, n2, d2, p2);
            }
         }
      }
      else {
         if (sgn0*sgn2 == 1) {
            if (sgn0*sgn3 == 1) {
               // - + +   1-0 1-2 1-3
               intersectTriangle (
                  isect, n1, d1, p1, n0, d0, p0, n2, d2, p2, n3, d3, p3);
            }
            else {
               // - + -   0-1 1-2 2-3 3-0
               intersectQuad (
                  isect, n0, d0, p0, n1, d1, p1, n2, d2, p2, n3, d3, p3);
            }
         }
         else {
            if (sgn0*sgn3 == 1) {
               // - - +   0-2 2-3 3-1 1-0
               intersectQuad (
                  isect, n0, d0, p0, n2, d2, p2, n3, d3, p3, n1, d1, p1);
            }
            else {
               // - - -   0-1 0-3 0-2
               intersectTriangle (
                  isect, n0, d0, p0, n1, d1, p1, n3, d3, p3, n2, d2, p2);
            }
         } 
      }
      if (isect.myNumSides != 0) {
         isect.myTetDesc = tdesc;
         return true;
      }
      else {
         return false;
      }
   }

   public PlaneType computeBCoefs (
      double[] b, Vector3d r, double[] c, Plane planeCell) {

      double rx, ry, rz;
      Vector3d nrmCell = planeCell.normal;
      double off = planeCell.offset;

      PlaneType planeType;
      switch (nrmCell.maxAbsIndex()) {
         case 0: {
            rx = nrmCell.y/nrmCell.x;
            ry = nrmCell.z/nrmCell.x;
            rz = off/nrmCell.x;

            b[0] = c[1] + c[0]*rx*rx - c[5]*rx;
            b[1] = c[2] + c[0]*ry*ry - c[4]*ry;
            b[2] = c[3] + 2*c[0]*rx*ry - c[5]*ry - c[4]*rx;
            b[3] = c[7] - 2*c[0]*rz*rx + c[5]*rz - c[6]*rx;
            b[4] = c[8] - 2*c[0]*rz*ry + c[4]*rz - c[6]*ry;
            b[5] = c[9] + c[0]*rz*rz + c[6]*rz;

            planeType = PlaneType.YZ;
            break;
         }
         case 1: {
            rx = nrmCell.z/nrmCell.y;
            ry = nrmCell.x/nrmCell.y;
            rz = off/nrmCell.y;


            b[0] = c[2] + c[1]*rx*rx - c[3]*rx;
            b[1] = c[0] + c[1]*ry*ry - c[5]*ry;
            b[2] = c[4] + 2*c[1]*rx*ry - c[3]*ry - c[5]*rx;
            b[3] = c[8] - 2*c[1]*rz*rx + c[3]*rz - c[7]*rx;
            b[4] = c[6] - 2*c[1]*rz*ry + c[5]*rz - c[7]*ry;
            b[5] = c[9] + c[1]*rz*rz + c[7]*rz;

            planeType = PlaneType.ZX;
            break;
         }
         case 2: {
            rx = nrmCell.x/nrmCell.z;
            ry = nrmCell.y/nrmCell.z;
            rz = off/nrmCell.z;

            b[0] = c[0] + c[2]*rx*rx - c[4]*rx;
            b[1] = c[1] + c[2]*ry*ry - c[3]*ry;
            b[2] = c[5] + 2*c[2]*rx*ry - c[4]*ry - c[3]*rx;
            b[3] = c[6] - 2*c[2]*rz*rx + c[4]*rz - c[8]*rx;
            b[4] = c[7] - 2*c[2]*rz*ry + c[3]*rz - c[8]*ry;
            b[5] = c[9] + c[2]*rz*rz + c[8]*rz;

            planeType = PlaneType.XY;
            break;
         }
         default: {
            throw new InternalErrorException (
               "Vector3d.maxAbsIndex() returned value "+nrmCell.maxAbsIndex());
         }
      }
      if (r != null) {
         r.x = rx;
         r.y = ry;
         r.z = rz;
      }
      return planeType;
   }

   public boolean findQuadSurfaceTangent (
      Point3d pt, Point3d p0, Point3d pa, Vector3d nrm) {

      // transform p0, pa and nrm into grid local coordinates
      Point3d p0Loc = new Point3d();
      myGrid.myLocalToWorld.inverseTransformPnt (p0Loc, p0);
      Point3d paLoc = new Point3d();
      myGrid.myLocalToWorld.inverseTransformPnt (paLoc, pa);
      Vector3d nrmLoc = new Vector3d(nrm);
      myGrid.myLocalToWorld.inverseTransformCovec (nrmLoc, nrm);
      nrmLoc.normalize();

      //System.out.println (
      //    "findSurfaceTangent nrmLoc=" + nrmLoc.toString("%8.3f"));

      // generate a plane from the normal and p0
      Plane planeLoc = new Plane (nrmLoc, p0Loc);

      // Dynamic array to store tet/plane intersections
      ArrayList<TetPlaneIntersection> isects =
         new ArrayList<TetPlaneIntersection>();
      isects.add (new TetPlaneIntersection()); // will need at least one

      // temporary storage for computed tet vertex coordinates
      Point3d[] vpnts = new Point3d[] {
         new Point3d(), new Point3d(), new Point3d(), new Point3d() };      

      int icnt = 0;
      int maxIters = 2;
      int code = NONE;
      TetDesc tdesc = null;
      TanSearchInfo sinfo = new TanSearchInfo();
      int sgnDotGrad = 0;
      Point3d ps = new Point3d(p0Loc);

      Vector3d grad = new Vector3d();
      double d = myGrid.getQuadDistanceAndGradient (grad, null, p0Loc);
      if (d == DistanceGrid.OUTSIDE_GRID) {
         // shouldn't happen - p0 should be inside by definition
         //System.out.println ("OUTSIDE");
         return false;
      }
      grad.scaledAdd (-grad.dot(nrmLoc), nrmLoc); // project grad into plane
      grad.normalize();
      
      Vector3i vxyz = new Vector3i();
      Vector3d xyz = new Vector3d();
      if (myGrid.getQuadCellCoords (
         xyz, vxyz, p0Loc, myGrid.myQuadGridToLocal) == -1) {
         // shouldn't happen - grid should have enough margin to prevent this
         //System.out.println ("NO QUAD CELL");
         return false;
      }
      tdesc =
         new TetDesc (vxyz, TetID.findSubTet (xyz.x, xyz.y, xyz.z));      
      getVertexCoords (vpnts, tdesc);
      TetPlaneIntersection isect = isects.get(0);
      if (!intersectTetAndPlane (isect, tdesc, vpnts, planeLoc)) {
         // shouldn't happen - p0 should be on the plane
         //System.out.println ("NOT ON PLANE");
         return false;
      }

      PlaneType planeType = null;
      Vector3d nrmCell = new Vector3d();
      myGrid.myGridToLocal.inverseTransformCovec (nrmCell, planeLoc.normal);
      nrmCell.normalize();
      //transformNormalToCell (nrmCell, planeLoc.normal);
      switch (nrmCell.maxAbsIndex()) {
         case 0: planeType = PlaneType.YZ; break;
         case 1: planeType = PlaneType.ZX; break;
         case 2: planeType = PlaneType.XY; break;
      }
      int planeSgn = (nrmCell.get(nrmCell.maxAbsIndex()) > 0 ? 1 : -1);

      double[] srng = new double[] { -Double.MAX_VALUE, Double.MAX_VALUE };
      DoubleHolder edgeS = new DoubleHolder();
      int edgeIdx = isect.intersectRay (
         srng, edgeS, p0Loc, grad, planeType, planeSgn);
      if (edgeIdx == -1) {
         // shouldn't happen - p0 should be in the tet
         //System.out.println ("NOT IN TET");
         return false;
      }
      TetFeature lastFeat = isect.getFeature (edgeS.value, edgeIdx);

      HashSet<TetDesc> visitedTets = new HashSet<TetDesc>();
      visitedTets.add (tdesc);

      while (findSurfaceIntersectionInTet (
                ps, isect, srng, p0Loc, grad, planeLoc) == CONTINUE) {
          int ntets = getFeatureAdjacentTets (
             isects, tdesc, lastFeat, planeLoc, visitedTets);
          TetPlaneIntersection ibest = null;
          double bestLen = 0;
          int bestEdgeIdx = -1;
          double bestS = -1;          
          for (int k=0; k<ntets; k++) {
             isect = isects.get(k);
             double[] irng =
                new double[] { -Double.MAX_VALUE, Double.MAX_VALUE };
             edgeIdx = isect.intersectRay (
                irng, edgeS, p0Loc, grad, planeType, planeSgn);
             if (edgeIdx != -1) {
                double ilen = irng[1]-irng[0];
                if (ilen > bestLen) {
                   bestLen = ilen;
                   ibest = isect;
                   bestEdgeIdx = edgeIdx;
                   bestS = edgeS.value;
                   srng[0] = irng[0];
                   srng[1] = irng[1];
                }
             }
          }
          if (bestEdgeIdx == -1) {
             return false;
          }
          else {
             lastFeat = ibest.getFeature (bestS, bestEdgeIdx);
             isect = ibest;
             tdesc = isect.myTetDesc;
             visitedTets.add (tdesc);
          }
      }

      // System.out.println ("  intersection ps=" + ps.toString("%8.5f"));
      // System.out.println ("               p0=" + p0Loc.toString("%8.5f"));

      d = myGrid.getQuadDistanceAndGradient (grad, null, ps);
      if (d == DistanceGrid.OUTSIDE_GRID) {
         // shouldn't happen - ps should be inside by definition
         return false;
      }
      grad.scaledAdd (-grad.dot(nrmLoc), nrmLoc); // project grad into plane
      Vector3d dela0 = new Vector3d();
      dela0.sub (ps, paLoc);
      sgnDotGrad = (dela0.dot(grad) > 0 ? 1 : -1);       
      
      //System.out.println ("findSurfaceTangent(A) for " + tdesc);
      code = findSurfaceTangent (
         pt, sinfo, isect, null, sgnDotGrad, paLoc, planeLoc);
      if (code == DONE) {
         // tangent found
         myGrid.myLocalToWorld.transformPnt (pt, pt);
         //System.out.println ("  found pt=" + pt.toString ("%8.3f"));
         return true;
      }

      visitedTets.clear();
      visitedTets.add (tdesc);

      while (code != NONE) {
         int ntets = getFeatureAdjacentTets (
            isects, tdesc, sinfo.lastFeat, planeLoc, visitedTets);
         TetPlaneIntersection ibest = null;
         //System.out.println ("ntets= " + ntets);
         if (ntets > 1) {
            // find the tet that best intersects the current curve tangent
            double maxLen = -1;
            int maxTet = -1;
            for (int k=0; k<ntets; k++) {
               double len = isects.get(k).tanIntersectOverlap (
                  pt, sinfo.lastGrad, planeType);
               if (len > maxLen) {
                  maxLen = len;
                  maxTet = k;
               }
            }
            ibest = isects.get(maxTet);
         }
         else if (ntets > 0) {
            ibest = isects.get(0);
         }
         else {
            System.out.println ("  Couldn't find tangent");
            code = NONE;
            continue;
         }
         tdesc = ibest.myTetDesc;
         visitedTets.add (tdesc);
         //System.out.println ("findSurfaceTangent(B) for " + tdesc);
         code = findSurfaceTangent (
            pt, sinfo, ibest, pt, sgnDotGrad, paLoc, planeLoc);
         int tidx = 0;
         while (code == NONE && tidx < ntets) {
            isect = isects.get(tidx++);
            tdesc = isect.myTetDesc;
            visitedTets.add (tdesc);         
            code = findSurfaceTangent (
               pt, sinfo, isect, pt, sgnDotGrad, paLoc, planeLoc);
         }
         if (code == DONE) {
            myGrid.myLocalToWorld.transformPnt (pt, pt);
            //System.out.println ("  found pt=" + pt.toString ("%8.3f"));
            return true;
         }
      }
      myGrid.myLocalToWorld.transformPnt (pt, ps);
      //System.out.println ("  fallback pt=" + pt.toString ("%8.3f"));
      return false;
   }

   protected int findTangentPoints (
      Vector2d[] pnts, double b[], double pax, double pay) {

      double ax = (b[3] + 2*b[0]*pax + b[2]*pay);
      double ay = (b[4] + 2*b[1]*pay + b[2]*pax);
      double ab = (2*b[5] + b[3]*pax + b[4]*pay);

      double[] roots = new double[2];
      int nr;
      if (Math.abs(ax) >= Math.abs(ay)) {
         // x = r0 y + r1,  r0 = -ay/ax, r1 = -ab/ax
         double r0 = -ay/ax;
         double r1 = -ab/ax;
         double aa = b[0]*r0*r0 + b[2]*r0 + b[1];
         double bb = 2*b[0]*r0*r1 + b[2]*r1 + b[3]*r0 + b[4];
         double cc = b[0]*r1*r1 + b[3]*r1 + b[5];
         nr = QuadraticSolver.getRoots (roots, aa, bb, cc);
         for (int i=0; i<nr; i++) {
            double y = roots[i];
            pnts[i].y = y;
            pnts[i].x = r0*y + r1;
         }
      }
      else {
         // y = r0 x + r1,  r0 = -ax/ay, r1 = -ab/ay
         double r0 = -ax/ay;
         double r1 = -ab/ay;
         double aa = b[1]*r0*r0 + b[2]*r0 + b[0];
         double bb = 2*b[1]*r0*r1 + b[2]*r1 + b[4]*r0 + b[3];
         double cc = b[1]*r1*r1 + b[4]*r1 + b[5];
         nr = QuadraticSolver.getRoots (roots, aa, bb, cc);
         for (int i=0; i<nr; i++) {
            double x = roots[i];
            pnts[i].x = x;
            pnts[i].y = r0*x + r1;
         }
      }
      return nr;
   }


   public static class TanSearchInfo {
      TetFeature lastFeat;
      Vector3d lastGrad;

      TanSearchInfo() {
         lastGrad = new Vector3d();
      }            
   }

   public boolean findQuadSurfaceIntersection (
      Point3d pi, Point3d p0, Point3d pa, Vector3d nrm) {

      // transform p0, pa and nrm into grid local coordinates
      Point3d p0Loc = new Point3d();
      myGrid.myLocalToWorld.inverseTransformPnt (p0Loc, p0);
      Point3d paLoc = new Point3d();
      myGrid.myLocalToWorld.inverseTransformPnt (paLoc, pa);
      Vector3d nrmLoc = new Vector3d(nrm);
      myGrid.myLocalToWorld.inverseTransformCovec (nrmLoc, nrm);
      nrmLoc.normalize();

      // generate a plane from the normal and p0
      Plane planeLoc = new Plane (nrmLoc, p0Loc);
      
      // project paLoc onto the plane
      planeLoc.project (paLoc, paLoc);

      // Dynamic array to store tet/plane intersections
      ArrayList<TetPlaneIntersection> isects =
         new ArrayList<TetPlaneIntersection>();
      isects.add (new TetPlaneIntersection()); // will need at least one

      // temporary storage for computed tet vertex coordinates
      Point3d[] vpnts = new Point3d[] {
         new Point3d(), new Point3d(), new Point3d(), new Point3d() };      

      int icnt = 0;
      int maxIters = 2;
      int code = NONE;
      TetDesc tdesc = null;
      TanSearchInfo sinfo = new TanSearchInfo();
      int sgnDotGrad = 0;
      Point3d ps = new Point3d(p0Loc);

      Vector3d dirLoc = new Vector3d();
      dirLoc.sub (paLoc, p0Loc);
      if (dirLoc.normSquared() == 0) {
         return false;
      }
      dirLoc.normalize();
      
      Vector3i vxyz = new Vector3i();
      Vector3d xyz = new Vector3d();
      if (myGrid.getQuadCellCoords (
         xyz, vxyz, p0Loc, myGrid.myQuadGridToLocal) == -1) {
         // shouldn't happen - grid should have enough margin to prevent this
         return false;
      }
      tdesc =
         new TetDesc (vxyz, TetID.findSubTet (xyz.x, xyz.y, xyz.z));      
      getVertexCoords (vpnts, tdesc);
      TetPlaneIntersection isect = isects.get(0);
      if (!intersectTetAndPlane (isect, tdesc, vpnts, planeLoc)) {
         // shouldn't happen - p0 should be on the plane
         return false;
      }

      PlaneType planeType = null;
      Vector3d nrmCell = new Vector3d();
      myGrid.myGridToLocal.inverseTransformCovec (nrmCell, planeLoc.normal);
      nrmCell.normalize();
      //transformNormalToCell (nrmCell, planeLoc.normal);
      switch (nrmCell.maxAbsIndex()) {
         case 0: planeType = PlaneType.YZ; break;
         case 1: planeType = PlaneType.ZX; break;
         case 2: planeType = PlaneType.XY; break;
      }
      int planeSgn = (nrmCell.get(nrmCell.maxAbsIndex()) > 0 ? 1 : -1);

      double[] srng = new double[] { -Double.MAX_VALUE, Double.MAX_VALUE };
      DoubleHolder edgeS = new DoubleHolder();
      int edgeIdx = isect.intersectRay (
         srng, edgeS, p0Loc, dirLoc, planeType, planeSgn);
      if (edgeIdx == -1) {
         // shouldn't happen - p0 should be in the tet
         return false;
      }
      TetFeature lastFeat = isect.getFeature (edgeS.value, edgeIdx);

      HashSet<TetDesc> visitedTets = new HashSet<TetDesc>();
      visitedTets.add (tdesc);
      
      while (findSurfaceIntersectionInTet (
                pi, isect, srng, p0Loc, dirLoc, planeLoc) == CONTINUE) {
          int ntets = getFeatureAdjacentTets (
             isects, tdesc, lastFeat, planeLoc, visitedTets);
          TetPlaneIntersection ibest = null;
          double bestLen = 0;
          int bestEdgeIdx = -1;
          double bestS = -1;          
          for (int k=0; k<ntets; k++) {
             isect = isects.get(k);
             double[] irng =
                new double[] { -Double.MAX_VALUE, Double.MAX_VALUE };
             edgeIdx = isect.intersectRay (
                irng, edgeS, p0Loc, dirLoc, planeType, planeSgn);
             if (edgeIdx != -1) {
                double ilen = irng[1]-irng[0];
                if (ilen > bestLen) {
                   bestLen = ilen;
                   ibest = isect;
                   bestEdgeIdx = edgeIdx;
                   bestS = edgeS.value;
                   srng[0] = irng[0];
                   srng[1] = irng[1];
                }
             }
          }
          if (bestEdgeIdx == -1) {
             return false;
          }
          else {
             lastFeat = ibest.getFeature (bestS, bestEdgeIdx);
             isect = ibest;
             tdesc = isect.myTetDesc;
             visitedTets.add (tdesc);
          }
      }
      myGrid.myLocalToWorld.transformPnt (pi, pi);
      return true;
   }

    private int intersectSurfaceAndRay (
      double[] roots, double[] b,
      double px, double dx, double py, double dy, double smin, double smax) {

      double aa =
         b[0]*dx*dx + b[1]*dy*dy + b[2]*dx*dy;
      double bb =
         2*b[0]*px*dx + 2*b[1]*py*dy + b[2]*(px*dy + dx*py) + b[3]*dx + b[4]*dy;
      double cc =
         b[0]*px*px + b[1]*py*py + b[2]*px*py + b[3]*px + b[4]*py + b[5];

      return QuadraticSolver.getRoots (roots, aa, bb, cc, smin, smax);
   }

   private final int NONE = 0;
   private final int CONTINUE = 1;
   private final int DONE = 2;

   int findSurfaceIntersectionInTet (
      Point3d pi, TetPlaneIntersection isect,
      double[] srng, Point3d p0, Vector3d dir, Plane plane) {

      if (srng[0] >= srng[1]) {
         return CONTINUE;
      }

      double[] c = new double[10];
      TetDesc tdesc = isect.myTetDesc;
      myGrid.computeQuadCoefs (c, tdesc);         

      Point3d p0Cell = new Point3d();
      transformToQuadCell (p0Cell, p0, tdesc);
      Vector3d dirCell = new Vector3d();
      transformToQuadCell (dirCell, dir, tdesc);
      Plane planeCell = new Plane();
      transformToQuadCell (planeCell, plane, tdesc);

      double[] b = new double[6];
      Vector3d r = new Vector3d();
      PlaneType planeType = computeBCoefs (b, r, c, planeCell);
      
      double s = findSurfaceIntersectionCell (
         tdesc, b, planeType, srng, p0Cell, dirCell);
      if (s != -1) {
         pi.scaledAdd (s, dir, p0);
         return DONE;
      }
      else {
         return CONTINUE;
      }
   }

   int findSurfaceTangent (
      Point3d pt, TanSearchInfo sinfo, TetPlaneIntersection isect,
      Point3d pold, int sgnDotGrad, Point3d pa, Plane plane) {

      double[] c = new double[10];
      TetDesc tdesc = isect.myTetDesc;
      myGrid.computeQuadCoefs (c, tdesc);         

      //System.out.println ("  checking " + tdesc); 

      Point3d paCell = new Point3d();
      transformToQuadCell (paCell, pa, tdesc);
      Plane planeCell = new Plane();
      transformToQuadCell (planeCell, plane, tdesc);
      double[] b = new double[6];
      Vector3d r = new Vector3d();
      PlaneType planeType = computeBCoefs (b, r, c, planeCell);

      if (pold != null) {
         Point3d pc = new Point3d();
         Vector3d grad = new Vector3d();
         Vector3d dela0 = new Vector3d();
         transformToQuadCell (pc, pold, tdesc);
         // compute gradient and project it into the plane
         myGrid.computeQuadGradient (
            grad, c, pc.x, pc.y, pc.z, myGrid.myQuadGridToLocal);
         grad.scaledAdd (-grad.dot(plane.normal), plane.normal);
         dela0.sub (pold, pa);
         int newSgnDotGrad = (dela0.dot(grad) > 0 ? 1 : -1);
         if (newSgnDotGrad*sgnDotGrad < 0) {
            // sign change, so accept pold
            pt.set (pold);
            return DONE;
         }
      }
      
      if (findSurfaceTangentCell (
             pt, tdesc, b, r, planeType, paCell)) {
         transformFromQuadCell (pt, pt, tdesc);
         return DONE;
      }
      else {
         // find the intersection points, if any, of the curve with each
         // of the boundary edges
         Point3d[] ip = new Point3d[] {
            isect.myP0, isect.myP1, isect.myP2, isect.myP3 };         

         Point3d pl0 = ip[0];
         Point3d pl1 = null;
         Point3d pc0 = new Point3d();
         Point3d pc1 = new Point3d();
         transformToQuadCell (pc0, pl0, tdesc);

         double EPS = 1e-12;
         double[] svals = new double[2];
         int bestEdgeIdx = -1; // index of edge containing the best next point
         double bestDistToA = -1; 
         double bestS = -1; 
         for (int i=0; i<isect.myNumSides; i++) {
            pl1 = i<isect.myNumSides-1 ? ip[i+1] : ip[0];
            transformToQuadCell (pc1, pl1, tdesc);

            int nr = 0;
            switch (planeType) {
               case YZ: {
                  nr = intersectSurfaceAndRay (
                     svals, b, pc0.y, pc1.y-pc0.y, pc0.z, pc1.z-pc0.z, 0.0, 1.0);
                  break;
               }
               case ZX: {
                  nr = intersectSurfaceAndRay (
                     svals, b, pc0.z, pc1.z-pc0.z, pc0.x, pc1.x-pc0.x, 0.0, 1.0);
                  break;
               }
               case XY: {
                  nr = intersectSurfaceAndRay (
                     svals, b, pc0.x, pc1.x-pc0.x, pc0.y, pc1.y-pc0.y, 0.0, 1.0);
                  break;
               }
            }
            if (nr > 0) {
               Point3d pi = new Point3d();
               Vector3d grad = new Vector3d();
               Vector3d dela0 = new Vector3d();
               for (int j=0; j<nr; j++) {
                  // compute gradient at the intersection point, and
                  // project it into the plane
                  pi.combine (1-svals[j], pc0, svals[j], pc1);
                  myGrid.computeQuadGradient (
                     grad, c, pi.x, pi.y, pi.z, myGrid.myQuadGridToLocal);
                  grad.scaledAdd (-grad.dot(plane.normal), plane.normal);
                  pi.combine (1-svals[j], pl0, svals[j], pl1);
                  dela0.sub (pi, pa);
                  if (pold != null &&
                      pi.distance (pold) <= myGrid.getRadius()*EPS) {
                     // same as initial point, ignore
                     continue;
                  }
                  if (dela0.dot(grad)*sgnDotGrad < 0) {
                     // tangent direction switched sides; can't
                     // be part of the same curve section, ignore
                     continue;
                  }
                  double distToA = pa.distance (pi);
                  // if sgnDotGrad > 0, want to move towards pa.
                  // otherwise, want to move away
                  if (bestEdgeIdx == -1 ||
                      (sgnDotGrad > 0 && distToA < bestDistToA) ||
                      (sgnDotGrad < 0 && distToA > bestDistToA)) {
                     bestEdgeIdx = i;
                     bestDistToA = distToA;
                     bestS = svals[j];
                     sinfo.lastGrad.set (grad);
                     pt.set (pi);
                  }
               }
            }
            pl0 = pl1;
            pc0.set (pc1);
         }
         if (bestEdgeIdx != -1) {
            // need to find the feature associated with this point
            sinfo.lastFeat = isect.getFeature (bestS, bestEdgeIdx);
            //System.out.println ("setting feat=" + sinfo.lastFeat);
            return CONTINUE;
         }
         else {
            return NONE;
         }
      }
   }

   protected int getFeatureAdjacentTets (
      ArrayList<TetPlaneIntersection> isects, 
      TetDesc tdesc, TetFeature feat, Plane plane,
      HashSet<TetDesc> visited) {
      
      createConnectivityIfNecessary();

      Point3d[] vpnts = new Point3d[] {
         new Point3d(), new Point3d(), new Point3d(), new Point3d() };

      if (feat instanceof TetFace) {
         TetDesc adjDesc = myFaceTets.get((TetFace)feat);
         TetDesc adesc = new TetDesc(adjDesc);
         adesc.addOffset (tdesc);
         if (myGrid.inRange(adesc) && !visited.contains(adesc)) {
            getVertexCoords (vpnts, adesc);
            if (isects.size() == 0) {
               isects.add (new TetPlaneIntersection());
            }
            TetPlaneIntersection isect = isects.get(0);
            if (intersectTetAndPlane (isect, adesc, vpnts, plane)) {
               return 1;
            }
         }
         return 0;
      }
      else {
         TetDesc[] adjDescs = null;
         if (feat instanceof TetNode) {
            adjDescs = myNodeTets.get(((TetNode)feat).getNode());
         }
         else { // feat instanceof TetEdge
            adjDescs = myEdgeTets.get((TetEdge)feat);
         }
         int numi = 0;
         for (int i=0; i<adjDescs.length; i++) {
            TetDesc adesc = new TetDesc(adjDescs[i]);
            adesc.addOffset (tdesc);
            if (myGrid.inRange(adesc) && !visited.contains(adesc)) {
               // for node-adjacent tets, only allow those whose quad hex is
               // different from that of tdesc
               if ((feat instanceof TetNode && !adesc.cellEquals(tdesc)) ||
                   (feat instanceof TetEdge && !adesc.equals(tdesc))) {
                  getVertexCoords (vpnts, adesc);
                  if (isects.size() == numi) {
                     isects.add (new TetPlaneIntersection());
                  }
                  TetPlaneIntersection isect = isects.get(numi);
                  if (intersectTetAndPlane (isect, adesc, vpnts, plane)) {
                     numi++;
                  }
               }
            }
         }
         return numi;
      }
   }

   protected int intersectCurveWithTetBoundary (
      Vector2d[] pnts, double[] b, Vector3d r, int minCoord, int maxCoord) {

      return 0;
   }

   protected boolean findSurfaceTangentInTet (
      Point3d ptLoc, TetDesc tdesc, 
      Point3d paLoc, Plane planeLoc) {
      // XXX convert normal, off, and pa to tet coordinates

      Plane planeCell = new Plane();
      Point3d paCell = new Point3d();
      transformToQuadCell (planeCell, planeLoc, tdesc);
      transformToQuadCell (paCell, paLoc, tdesc);

      double[] c = new double[10];
      double[] b = new double[6];
      myGrid.computeQuadCoefs (c, tdesc);
      PlaneType planeType;

      Vector2d[] pnts = new Vector2d[] { new Vector2d(), new Vector2d() };

      Vector3d r = new Vector3d();
      planeType = computeBCoefs (b, r, c, planeCell);
      double x = 0, y = 0, z = 0;
      boolean found = false;

      switch (planeType) {
         case YZ: {
            int nr = findTangentPoints (pnts, b, paCell.y, paCell.z);
            for (int i=0; i<nr; i++) {
               y = pnts[i].x;
               z = pnts[i].y;
               x = r.z - r.x*y - r.y*z;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  found = true;
                  break;
               }
            }
            break;
         }
         case ZX: {

            int nr = findTangentPoints (pnts, b, paCell.z, paCell.x);
            for (int i=0; i<nr; i++) {
               z = pnts[i].x;
               x = pnts[i].y;
               y = r.z - r.x*z - r.y*x;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  found = true;
                  break;
               }
            }
            break;
         }
         case XY: {
            int nr = findTangentPoints (pnts, b, paCell.x, paCell.y);
            for (int i=0; i<nr; i++) {
               x = pnts[i].x;
               y = pnts[i].y;
               z = r.z - r.x*x - r.y*y;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  found = true;
                  break;
               }
            }
            break;
         }
      }
      if (found) {
         //System.out.println ("dist=" + computeDist (c, x, y, z));
         //System.out.println ("perp=" + computePerp (c, x, y, z, paCell));
         transformFromQuadCell (ptLoc, x, y, z, tdesc);
         return true;
      }
      else {
         return false;
      }
   }

   protected boolean findSurfaceTangentCell (
      Point3d ptCell, TetDesc tdesc,
      double[] b, Vector3d r, PlaneType planeType,
      Point3d paCell) {
      // XXX convert normal, off, and pa to tet coordinates

      Vector2d[] pnts = new Vector2d[] { new Vector2d(), new Vector2d() };

      double x = 0, y = 0, z = 0;

      switch (planeType) {
         case YZ: {
            int nr = findTangentPoints (pnts, b, paCell.y, paCell.z);
            for (int i=0; i<nr; i++) {
               y = pnts[i].x;
               z = pnts[i].y;
               x = r.z - r.x*y - r.y*z;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  ptCell.set (x, y, z);
                  return true;
               }
            }
            break;
         }
         case ZX: {
            int nr = findTangentPoints (pnts, b, paCell.z, paCell.x);
            for (int i=0; i<nr; i++) {
               z = pnts[i].x;
               x = pnts[i].y;
               y = r.z - r.x*z - r.y*x;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  ptCell.set (x, y, z);
                  return true;
               }
            }
            break;
         }
         case XY: {
            int nr = findTangentPoints (pnts, b, paCell.x, paCell.y);
            for (int i=0; i<nr; i++) {
               x = pnts[i].x;
               y = pnts[i].y;
               z = r.z - r.x*x - r.y*y;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  ptCell.set (x, y, z);
                  return true;
               }
            }
            break;
         }
      }
      return false;
   }

   protected double findSurfaceIntersectionCell (
      TetDesc tdesc,
      double[] b, PlaneType planeType,
      double[] srng, Point3d pc, Vector3d dc) {
      // XXX convert normal, off, and pa to tet coordinates

      double[] svals = new double[2];
      double smax = Double.MAX_VALUE;
      double x = 0, y = 0, z = 0;

      int nr = 0;
      switch (planeType) {
         case YZ: {
            nr = intersectSurfaceAndRay (
               svals, b, pc.y, dc.y, pc.z, dc.z, srng[0], srng[1]);
            break;
         }
         case ZX: {
            nr = intersectSurfaceAndRay (
               svals, b, pc.z, dc.z, pc.x, dc.x, srng[0], srng[1]);
            break;
         }
         case XY: {
            nr = intersectSurfaceAndRay (
               svals, b, pc.x, dc.x, pc.y, dc.y, srng[0], srng[1]);
            break;
         }
      }
      if (nr > 0) {
         // choose one nearest the ray origin
         return svals[nr-1];
      }
      else {
         return -1;
      }
   }

   // public void transformNormalToCell (Vector3d nc, Vector3d nl) {
   //    if (nc != nl) {
   //       nc.set (nl);
   //    }
   //    nc.x *= myGrid.myCellWidths.x;
   //    nc.y *= myGrid.myCellWidths.y;
   //    nc.z *= myGrid.myCellWidths.z;
   //    nc.normalize();
   // }

   // for debugging
   private double computePerp (
      double[] c, double x, double y, double z, Vector3d p) {

      Vector3d grad = new Vector3d();

      grad.x = 2*c[0]*x + c[4]*z + c[5]*y + c[6];
      grad.y = 2*c[1]*y + c[3]*z + c[5]*x + c[7];
      grad.z = 2*c[2]*z + c[3]*y + c[4]*x + c[8];
      Vector3d diff = new Vector3d(x, y, z);
      diff.sub (p);
      return diff.dot(grad)/(grad.norm()*diff.norm());
   }

   // for debugging
   private double computePlanePerp (
      double[] b, double x, double y, double px, double py) {

      Vector2d grad = new Vector2d();

      grad.x = 2*b[0]*x + b[2]*y + b[3];
      grad.y = 2*b[1]*y + b[2]*x + b[4];
      Vector2d diff = new Vector2d(x, y);
      diff.x -= px;
      diff.y -= py;
      return diff.dot(grad)/(grad.norm()*diff.norm());
   }

   // for debugging
   private double computePlaneDist (
      double[] b, double x, double y) {

      return b[0]*x*x + b[1]*y*y + b[2]*x*y + b[3]*x + b[4]*y + b[5];
   }

   public void transformToQuadCell (Point3d pc, Point3d pl, TetDesc tdesc) {
      // transform from local to regular grid coords, then to quad cell
      myGrid.myQuadGridToLocal.inverseTransformPnt (pc, pl); 
      pc.x = pc.x-tdesc.myCXi;
      pc.y = pc.y-tdesc.myCYj;
      pc.z = pc.z-tdesc.myCZk;     
//      myGrid.myGridToLocal.inverseTransformPnt (pc, pl); 
//      pc.x = 0.5*(pc.x-tdesc.myXi);
//      pc.y = 0.5*(pc.y-tdesc.myYj);
//      pc.z = 0.5*(pc.z-tdesc.myZk);
   }

   public void transformToQuadCell (Vector3d vc, Vector3d v1, TetDesc tdesc) {
      vc.scale (0.5, v1); // transform to regular grid coords
      myGrid.myGridToLocal.inverseTransformVec (vc, vc);
   }

   public void transformFromQuadCell (
      Point3d pl, double x, double y, double z, TetDesc tdesc) {

      // transform to regular grid coords, then transform to local coords
//      pl.set (tdesc.myXi+2*x, tdesc.myYj+2*y, tdesc.myZk+2*z);
//      myGrid.myGridToLocal.transformPnt (pl, pl);
      pl.set (tdesc.myCXi+x, tdesc.myCYj+y, tdesc.myCZk+z);
      myGrid.myQuadGridToLocal.transformPnt (pl, pl);
   }

   public void transformFromQuadCell (
      Point3d pl, Point3d pc, TetDesc tdesc) {
      transformFromQuadCell (pl, pc.x, pc.y, pc.z, tdesc);
   }

   public void transformToQuadCell (Plane pc, Plane pl, TetDesc tdesc) {
      Point3d p = new Point3d();
      p.scale (pl.offset, pl.normal); // point on the untransformed plane
      // normal transform to quad grid same as transform to regular grid:
      myGrid.myGridToLocal.inverseTransformCovec (pc.normal, pl.normal);
      pc.normal.normalize();
      // transform ref point to quad cell to recompute the offset
      transformToQuadCell (p, p, tdesc);
      pc.offset = p.dot(pc.normal);
//      if (pc != pl) {
//         pc.set (pl);
//      }
//      Point3d pbase = new Point3d(tdesc.myXi, tdesc.myYj, tdesc.myZk);
//      myGrid.myGridToLocal.transformPnt (pbase, pbase);
//      pc.offset -= pc.normal.dot (pbase);
//      //myGrid.myGridToLocal.inverseTransformNormal (pc.normal, pc.normal);
//      pc.normal.x *= (2*myGrid.myCellWidths.x);
//      pc.normal.y *= (2*myGrid.myCellWidths.y);
//      pc.normal.z *= (2*myGrid.myCellWidths.z);
//      // normalize
//      double mag = pc.normal.norm();
//      pc.normal.scale (1/mag);
//      pc.offset /= mag;         
   }

   private void transformFromGrid (
      Vector3d ploc, double x, double y, double z) {
      myGrid.myGridToLocal.transformPnt (ploc, new Point3d(x, y, z));
   }

   public void getVertexCoords (Point3d[] v, TetDesc tdesc) {
      double xi = 2*tdesc.myCXi;
      double yj = 2*tdesc.myCYj;
      double zk = 2*tdesc.myCZk;

      transformFromGrid (v[0], xi, yj, zk);
      
      int[] offs = tdesc.myTetId.getMiddleNodeOffsets();
      
      transformFromGrid (v[1], xi+offs[0], yj+offs[1], zk+offs[2]);
      transformFromGrid (v[2], xi+offs[3], yj+offs[4], zk+offs[5]);
      transformFromGrid (v[3], xi+2, yj+2, zk+2);
   }

   public static void main (String[] args) {
      DistanceGridSurfCalc calc =
         new DistanceGridSurfCalc (null);

      calc.createConnectivity();
      calc.printConnectivity();
   }

}
