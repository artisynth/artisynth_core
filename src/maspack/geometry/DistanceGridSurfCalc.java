/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import maspack.geometry.DistanceGrid.TetDesc;
import maspack.geometry.DistanceGrid.TetID;
import maspack.matrix.Matrix3d;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.QuadraticSolver;
import maspack.util.ReaderTokenizer;

/**
 * Worker class that performs computations specifically related to the
 * intersection of a plane and the surface of a quadratically interpolated
 * distance grid.
 */
public class DistanceGridSurfCalc {

   DistanceGrid myGrid;
   // buffer of tet/plane intersection objects for use in computations
   ArrayList<TetPlaneIntersection> myIsects;
   // hash set to keep track of tets visited during search
   HashSet<TetDesc> myVisitedTets;

   public enum PlaneType {
      YZ,
      ZX,
      XY
   };

   /**
    * Defines a 2D plane (Y/Z, Z/X, or X/Y) in which certain types of plane
    * computations can be performed for greater efficiency.
    */
   private static class ComputePlane {
      PlaneType type;
      int sign;
   }

   /**
    * Used to write out a surface tangent problem for debugging purposes.
    */
   public static class TangentProblem {
      public Point3d myP0;
      public Point3d myPa;
      public Vector3d myNrm;
      public DistanceGrid myGrid;

      public TangentProblem() {
         myP0 = new Point3d();
         myPa = new Point3d();
         myNrm = new Vector3d();
      }

      public TangentProblem (
         Point3d p0, Point3d pa, Vector3d nrm, DistanceGrid grid) {
         this();
         set (p0, pa, nrm, grid);
      }

      public void set (Point3d p0, Point3d pa, Vector3d nrm, DistanceGrid grid) {
         myP0.set (p0);
         myPa.set (pa);
         myNrm.set (nrm);
         myGrid = grid;
      }

      public void scan (ReaderTokenizer rtok, Object ref) throws IOException {

         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            if (rtok.ttype != ReaderTokenizer.TT_WORD) {
               throw new IOException ("Expected attribute name, "+rtok);
            }      
            if (rtok.sval.equals ("p0")) {
               rtok.scanToken ('=');
               myP0.scan (rtok);
            }
            else if (rtok.sval.equals ("pa")) {
               rtok.scanToken ('=');
               myPa.scan (rtok);
            }
            else if (rtok.sval.equals ("nrm")) {
               rtok.scanToken ('=');
               myNrm.scan (rtok);
            }
            else if (rtok.sval.equals ("grid")) {
               rtok.scanToken ('=');
               myGrid = new DistanceGrid();
               myGrid.scan (rtok, null);
            }
         }
      }

      public void write (String fileName, String fmt) {
         try {
            PrintWriter pw =
               new IndentingPrintWriter (
                  new PrintWriter (
                     new BufferedWriter (new FileWriter (new File(fileName)))));
            write (pw, new NumberFormat (fmt), null);
            pw.close();
         }
         catch (Exception e) {
            System.out.println (e);
         }
      }

      public void write (PrintWriter pw, NumberFormat fmt, Object ref)
         throws IOException {

         pw.print ("[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         pw.print ("p0=");
         myP0.write (pw, fmt, /*brackets=*/true);
         pw.print ("\npa=");
         myPa.write (pw, fmt, /*brackets=*/true);
         pw.print ("\nnrm=");
         myNrm.write (pw, fmt, /*brackets=*/true);
         pw.print ("\ngrid=");
         myGrid.write (pw, fmt, null);
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }

      public static TangentProblem scan (String fileName) {
         TangentProblem prob = null;
         try {
            prob = new TangentProblem();            
            ReaderTokenizer rtok =
               new ReaderTokenizer (
                  new BufferedReader (new FileReader (new File(fileName))));
            prob.scan (rtok, null);
            rtok.close();
         }
         catch (Exception e) {
            e.printStackTrace();
            prob = null;
         }
         return prob;
      }


   }

   public DistanceGridSurfCalc (DistanceGrid grid) {
      myGrid = grid;
      // allocate buffer of tet/plane intersection objects for use in
      // computations. Will be grown on demand.
      myIsects = new ArrayList<TetPlaneIntersection>();
      myVisitedTets = new HashSet<TetDesc>();
   }

   TetPlaneIntersection getIsect (int idx) {
      while (myIsects.size() <= idx) {
         myIsects.add (new TetPlaneIntersection());
      }
      return myIsects.get (idx);      
   }

   protected static HashMap<TetFace,TetDesc> myFaceTets;
   protected static HashMap<TetEdge,TetDesc[]> myEdgeTets;
   protected static ArrayList<TetDesc[]> myNodeTets;
   protected static Matrix3d[] myTetBarycentricMats;

   protected int myDebug = 0;

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

   private void createTetBarycentricMats() {
      Matrix3d[] mats = new Matrix3d[6];
      Vector3d v = new Vector3d();
      for (TetID id : TetID.values()) {
         Matrix3d M = new Matrix3d();
         int[] nodeNums = id.getNodes();
         // base node is known to be at zero
         for (int j=0; j<3; j++) {
            v.set (DistanceGrid.myBaseQuadCellXyzi[nodeNums[j+1]]);
            v.scale (0.5);
            M.setColumn (j, v);
         }
         M.invert();
         mats[id.intValue()] = M;
      }
      myTetBarycentricMats = mats;
   }

   /**
    * Find the nearest feature on a tet to a point p0. eps is a tolerance 
    * (in the range [0,1]) used to check the barycentric coordinates
    * of p0 to see if it is actually close to a feature. If it
    * is not, this method returns null.
    */
   TetFeature findNearestFeature (TetDesc desc, Point3d p0Loc, double eps) {
      // we find the nearest feature by computing the barycentric coordinates
      // of p0Loc for the tet. If we are close to a feature, one or
      // of these coordinates must be close to zero. The barycentric
      // coordinate calculation is done with respect to grid coordinates.

      // verts gives the coordinates of each tet nodes in grid coordinates

      if (myTetBarycentricMats == null) {
         createTetBarycentricMats();
      }
      // convert p0Loc to quad grid coordinates, and the multiply by the
      // barycentric conversion matrix to get barycentric coordinates s1, s2,
      // s3. s0 is then given by s0 = 1 - s1 - s2 - s2.
      Point3d pc = new Point3d();
      Vector3d sv = new Vector3d();
      transformToQuadCell (pc, p0Loc, desc);
      myTetBarycentricMats[desc.myTetId.intValue()].mul (sv, pc);
      
      // code is a bit code describing which coordinates are close to 0
      int code = 0;
      if (Math.abs (1-sv.get(0)-sv.get(1)-sv.get(2)) < eps) { 
         code |= 0x01;
      }
      if (Math.abs (sv.get(0)) < eps) {
         code |= 0x02;
      }
      if (Math.abs (sv.get(1)) < eps) {
         code |= 0x04;
      }
      if (Math.abs (sv.get(2)) < eps) {
         code |= 0x08;
      }
      int[] nodes = desc.myTetId.getNodes();
      switch (code) {
         case 0x01: {
            return new TetFace (nodes[1], nodes[3], nodes[2]);
         }
         case 0x02: {
            return new TetFace (nodes[0], nodes[2], nodes[3]);
         }
         case 0x04: {
            return new TetFace (nodes[0], nodes[3], nodes[1]);
         }
         case 0x08: {
            return new TetFace (nodes[0], nodes[1], nodes[2]);
         }
         case 0x03: return new TetEdge (nodes[2], nodes[3]);
         case 0x05: return new TetEdge (nodes[1], nodes[3]);
         case 0x09: return new TetEdge (nodes[1], nodes[2]);
         case 0x06: return new TetEdge (nodes[0], nodes[3]);
         case 0x0a: return new TetEdge (nodes[0], nodes[2]);
         case 0x0c: return new TetEdge (nodes[0], nodes[1]);
         
         case 0x0e: return new TetNode (nodes[0]);
         case 0x0d: return new TetNode (nodes[1]);
         case 0x0b: return new TetNode (nodes[2]);
         case 0x07: return new TetNode (nodes[3]);
         
         default: {
            return null;
         }
      }
   }
   
   /**
    * Find the index number (in the range [0-7]) for a corner
    * node of a hex cell, as specified in terms of x,y,z index offsets.
    */
   static int findRefVertex (Vector3i xyzi) {
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

   static void maybeAddFace (
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

   static void maybeAddEdge (
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

   private static void createConnectivity () {
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
                     new TetDesc (new Vector3i (2*i, 2*j, 2*k), tetId);
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

   private static void createConnectivityIfNecessary() {
      if (myFaceTets == null) {
         createConnectivity();
      }
   }

   public static ArrayList<TetDesc[]> getNodeTets() {
      createConnectivityIfNecessary();
      return myNodeTets;
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

      TetDesc myTetDesc;
      public int myNumSides;

      // corners of the tet plane intersection - up to four, stored
      // in local coordinates
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

     /**
       * Returns true if a point p0 (given in local coordinates)
       * is inside this intersection.
       */
      boolean pointIsInside (Point3d pt, ComputePlane cplane) {

         Point3d[] pi = new Point3d[] {
            myP0, myP1, myP2, myP3};

         Point3d p0 = pi[0];
         int ccw = cplane.sign*myCCW;
         for (int i=0; i<myNumSides; i++) {
            Point3d p1 = i<myNumSides-1 ? pi[i+1] : pi[0];
            double r1x, rpx, r1y, rpy;
            switch (cplane.type) {
               case YZ: {
                  r1x = p1.y - p0.y;
                  r1y = p1.z - p0.z;
                  rpx = pt.y - p0.y;
                  rpy = pt.z - p0.z;
                  break;
               }
               case ZX: {
                  r1x = p1.z - p0.z;
                  r1y = p1.x - p0.x;
                  rpx = pt.z - p0.z;
                  rpy = pt.x - p0.x;
                  break;
               }
               case XY: {
                  r1x = p1.x - p0.x;
                  r1y = p1.y - p0.y;
                  rpx = pt.x - p0.x;
                  rpy = pt.y - p0.y;
                  break;
               }
               default: {
                  throw new InternalErrorException (
                     "Unknown plane type " + cplane.type);
               }
            }
            if ((r1x*rpy - r1y*rpx)*ccw < 0) {
               // then not an inside turn, so point is not inside
               return false;
            }
            p0 = p1;
         }
         return true;
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
      
      private double triArea (Point3d p0, Point3d p1, Point3d p2) {
         Vector3d del01 = new Vector3d();
         Vector3d del02 = new Vector3d();
         Vector3d xprod = new Vector3d();
         del01.sub (p1, p0);
         del02.sub (p2, p0);
         xprod.cross (del01, del02);
         return xprod.norm();         
      }
      
      double computeArea() {
         if (myNumSides == 3) {
            return triArea (myP0, myP1, myP2);
         }
         else if (myNumSides == 4) {
            return triArea (myP0, myP1, myP2) + triArea (myP0, myP2, myP3);
         }
         else {
            return 0;
         }
      }
   }

   void intersectTriangle (
      TetPlaneIntersection isect, TetDesc tdesc,
      Point3d[] vpnts, double[] dist, int i0, int i1, int i2, int i3) {
      
      int[] nodes = tdesc.myTetId.getNodes();

      int n0 = nodes[i0];
      Point3d p0 = vpnts[i0];
      double d0 = dist[i0];
      
      double EPS = 1e-8;

      double s1 = d0/(d0-dist[i1]);
      double s2 = d0/(d0-dist[i2]);
      double s3 = d0/(d0-dist[i3]);

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
      isect.myP0.combine (1-s1, p0, s1, vpnts[i1]);
      isect.myP1.combine (1-s2, p0, s2, vpnts[i2]);
      isect.myP2.combine (1-s3, p0, s3, vpnts[i3]);
      isect.myNumSides = 3;
      isect.myCCW = d0 < 0 ? 1 : -1;
   }

   void intersectQuad (
      TetPlaneIntersection isect, TetDesc tdesc,
      Point3d[] vpnts, double[] dist, int i0, int i1, int i2, int i3) {
      
      int[] nodes = tdesc.myTetId.getNodes();

      double d0 = dist[i0];
      double d1 = dist[i1];
      double d2 = dist[i2];
      double d3 = dist[i3];
      
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
      isect.myP0.combine (1-s1, vpnts[i0], s1, vpnts[i1]);
      isect.myP1.combine (1-s2, vpnts[i1], s2, vpnts[i2]);
      isect.myP2.combine (1-s3, vpnts[i2], s3, vpnts[i3]);
      isect.myP3.combine (1-s0, vpnts[i3], s0, vpnts[i0]);
      isect.myNumSides = 4;
      isect.myCCW = d0 > 0 ? 1 : -1;
   }

   public boolean intersectTetAndPlane (
      TetPlaneIntersection isect, TetDesc tdesc, Point3d[] vpnts, Plane plane) {

      double[] dist = new double[4];

      dist[0] = plane.distance(vpnts[0]);
      dist[1] = plane.distance(vpnts[1]);
      dist[2] = plane.distance(vpnts[2]);
      dist[3] = plane.distance(vpnts[3]);

      return doIntersectTetAndPlane (isect, tdesc, vpnts, dist, plane);
   }      

   public boolean doIntersectTetAndPlane (
      TetPlaneIntersection isect, TetDesc tdesc, Point3d[] vpnts,
      double[] dist, Plane plane) {

      int sgn0 = (dist[0] >= 0 ? 1 : -1);
      int sgn1 = (dist[1] >= 0 ? 1 : -1);
      int sgn2 = (dist[2] >= 0 ? 1 : -1);
      int sgn3 = (dist[3] >= 0 ? 1 : -1);
      
      if (sgn0*sgn1 == 1) {
         if (sgn0*sgn2 == 1) {
            if (sgn0*sgn3 == 1) {
               // + + +   nothing
               isect.myNumSides = 0;
            }
            else {
               // + + -   3-0 3-1 3-2
               intersectTriangle (isect, tdesc, vpnts, dist, 3, 0, 1, 2);
            }
         }
         else {
            if (sgn0*sgn3 == 1) {
               // + - +   2-0 2-3 2-1
               intersectTriangle (isect, tdesc, vpnts, dist, 2, 0, 3, 1);
            }
            else {
               // + - -   0-3 3-1 1-2 2-0
               intersectQuad (isect, tdesc, vpnts, dist, 0, 3, 1, 2);
            }
         }
      }
      else {
         if (sgn0*sgn2 == 1) {
            if (sgn0*sgn3 == 1) {
               // - + +   1-0 1-2 1-3
               intersectTriangle (isect, tdesc, vpnts, dist, 1, 0, 2, 3);
            }
            else {
               // - + -   0-1 1-2 2-3 3-0
               intersectQuad (isect, tdesc, vpnts, dist, 0, 1, 2, 3);
            }
         }
         else {
            if (sgn0*sgn3 == 1) {
               // - - +   0-2 2-3 3-1 1-0
               intersectQuad (isect, tdesc, vpnts, dist, 0, 2, 3, 1);
            }
            else {
               // - - -   0-1 0-3 0-2
               intersectTriangle (isect, tdesc, vpnts, dist, 0, 1, 3, 2);
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
      Vector3d nrmLoc = new Vector3d(nrm);
      myGrid.myLocalToWorld.inverseTransformCovec (nrmLoc, nrm);
      nrmLoc.normalize();

      // generate a plane from the normal and p0
      Plane planeLoc = new Plane (nrmLoc, p0Loc); // NEED

      // find the distance of p0 to the surface, along with the associated
      // gradient direction (grad).
      Vector3d grad = new Vector3d();
      double d = myGrid.getQuadDistanceAndGradient (grad, null, p0Loc);
      if (d == DistanceGrid.OUTSIDE_GRID) {
         // shouldn't happen - p0 should be inside by definition
         if (myDebug > 0) {
            System.out.println ("Not found: p0 outside the grid");
         }
         return false;
      }
      planeLoc.projectVector (grad, grad); // project grad into plane
      grad.normalize();
      //grad.negate();

      if (myDebug > 0) {
         System.out.println ("d=" + d + " grad=" + grad);
      }

      Vector3d nrmCell = new Vector3d();
      myGrid.myGridToLocal.inverseTransformCovec (nrmCell, nrmLoc);
      nrmCell.normalize();
      ComputePlane cplane = new ComputePlane();
      switch (nrmCell.maxAbsIndex()) {
         case 0: cplane.type = PlaneType.YZ; break;
         case 1: cplane.type = PlaneType.ZX; break;
         case 2: cplane.type = PlaneType.XY; break;
      }
      cplane.sign = (nrmCell.get(nrmCell.maxAbsIndex()) > 0 ? 1 : -1);     

      Point3d psLoc = new Point3d();
      TetDesc tdesc = findQuadSurfaceIntersectionLoc (psLoc, p0Loc, grad);
      if (tdesc == null) {
         // fallback - set pt to p0
         pt.set (p0);
         return false;
      }

      // need to obtain a plane intersection for tdesc
      
      TetPlaneIntersection isect =
         getTetPlaneIntersection (tdesc, psLoc, planeLoc, cplane);
      // tdesc might have changed
      if (isect == null) {
         if (myDebug > 0) {
            System.out.println ("Not found: ps tet does not intersect plane");
         }
         return false;
      }
      // tdesc might have changed
      tdesc = isect.myTetDesc;     

      // Get the surface gradient at the surface intersection point ps
      d = myGrid.getQuadDistanceAndGradient (grad, null, psLoc);
      if (d == DistanceGrid.OUTSIDE_GRID) {
         // shouldn't happen - ps should be inside by definition
         if (myDebug > 0) {
            System.out.println ("Not found: ps is outside grid");
         }
         return false;
      }
      planeLoc.projectVector (grad, grad); // project grad into plane
      Vector3d dela0 = new Vector3d();
      Point3d paLoc = new Point3d();
      myGrid.myLocalToWorld.inverseTransformPnt (paLoc, pa);
      dela0.sub (psLoc, paLoc);
      // set sgnDotGrad to 1 or -1 depending on whether the gradient is
      // pointing away or towards pa along the line (pa, ps).
      int sgnDotGrad = (dela0.dot(grad) > 0 ? 1 : -1);       

      if (myDebug > 0) {
         System.out.println (
            "found ps="+psLoc.toString("%g")+" tet="+tdesc+" sgnDotGrad=" +
            sgnDotGrad);
         Vector3d del = new Vector3d();
         del.sub (psLoc, p0Loc);
         System.out.println ("p0=" + p0Loc + " del=" + del);
         Point3d pg = new Point3d();
         myGrid.myQuadGridToLocal.inverseTransformPnt (pg, psLoc);
         System.out.println (
            "ps in quad grid coords=" + pg.toString("%g"));
         System.out.println (
            "pa=" + pa);
      }

      TanSearchInfo sinfo = new TanSearchInfo();
      int code = findSurfaceTangent (
         pt, sinfo, isect, null, sgnDotGrad, paLoc, planeLoc);
      if (code == DONE) {
         // tangent found
         myGrid.myLocalToWorld.transformPnt (pt, pt);
         if (myDebug > 0) {
            System.out.println (
               "Found (initial ps): pt=" + pt.toString ("%10.5f"));
         }
         return true;
      }
      if (code == NONE && myDebug > 0) {
         System.out.println ("Not found: no continuation point");
      }

      myVisitedTets.clear();
      myVisitedTets.add (tdesc);

      while (code != NONE) {
         int ntets = getFeatureAdjacentTets (
            tdesc.getBaseVertex(null), tdesc, sinfo.lastFeat,
            planeLoc, myVisitedTets);
         TetPlaneIntersection ibest = null;
         if (myDebug > 0) {
            System.out.println (
               "checking "+ntets+" adjacent tets for feature " + sinfo.lastFeat);
            System.out.print ("  ");
            for (int k=0; k<ntets; k++) {
               System.out.print (getIsect(k).myTetDesc + " ");
            }
            System.out.println ("");
         }
         if (ntets > 1) {
            // find the tet that best intersects the current curve tangent
            double maxLen = -1;
            int maxTet = -1;
            for (int k=0; k<ntets; k++) {
               double len = getIsect(k).tanIntersectOverlap (
                  pt, sinfo.lastGrad, cplane.type);
               if (len > maxLen) {
                  maxLen = len;
                  maxTet = k;
               }
            }
            ibest = getIsect(maxTet);
         }
         else if (ntets > 0) {
            ibest = getIsect(0);
         }
         else {
            if (myDebug > 0) {
               System.out.println (
                  "Not found: no adjacent tets intersect plane");
            }
            code = NONE;
            continue;
         }
         tdesc = ibest.myTetDesc;
         myVisitedTets.add (tdesc);
         code = findSurfaceTangent (
            pt, sinfo, ibest, pt, sgnDotGrad, paLoc, planeLoc);
         int tidx = 0;
         while (code == NONE && tidx < ntets) {
            isect = getIsect(tidx++);
            if (isect != ibest) {
               tdesc = isect.myTetDesc;
               myVisitedTets.add (tdesc);         
               code = findSurfaceTangent (
                  pt, sinfo, isect, pt, sgnDotGrad, paLoc, planeLoc);
            }
         }
         if (code == DONE) {
            myGrid.myLocalToWorld.transformPnt (pt, pt);
            if (myDebug > 0) {
               System.out.println ("Found: pt=" + pt.toString ("%10.5f"));
            }
            return true;
         }
         else if (code == NONE && myDebug > 0) {
            System.out.println (
               "Not found: no continuation point in any adjacent tet");
         }
      }
      myGrid.myLocalToWorld.transformPnt (pt, psLoc);
      return false;
   }

   protected static int findTangentPoints (
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

   TetPlaneIntersection getTetPlaneIntersection (
      TetDesc tdesc, Point3d p0Loc, Plane plane, ComputePlane cplane) {
      // temporary storage for computed tet vertex coordinates
      Point3d[] vpnts = new Point3d[] {
         new Point3d(), new Point3d(), new Point3d(), new Point3d() };      

      // get the local coordinates of the tet corners, and use these
      // to intersect the tet with the plane
      getVertexCoords (vpnts, tdesc);
      TetPlaneIntersection isect = getIsect(0);
      if (!intersectTetAndPlane (isect, tdesc, vpnts, plane) ||
          !isect.pointIsInside (p0Loc, cplane)) {
         // Finding the tet/plane intersection might fail in some cases because
         // of roundoff error, if p0 is close to a tet boundary feature (node,
         // edge, or face)
         TetFeature feat = findNearestFeature (tdesc, p0Loc, 1e-10);
         TetPlaneIntersection ibest = null;
         if (feat != null) {
            // If p0 is in fact near a tet boundary feature, find all the tets
            // adjacent to that feature which also intersect the plane.
            int ntets = getFeatureAdjacentTets (
               tdesc.getBaseVertex(null), tdesc, feat, plane, null);
            double bestArea = 0;
            // if there is more than one adjacent tet, choose the one whose
            // plane intersection has the largest area            
            for (int k=0; k<ntets; k++) {
               TetPlaneIntersection isectk = getIsect(k);
               if (isectk.pointIsInside (p0Loc, cplane)) {
                  double area = isectk.computeArea();
                  if (area > bestArea) {
                     ibest = isectk;
                     bestArea = area;
                  }
               }
            }
         }
         if (ibest == null) {
            return null;
         }
         else {
            if (myDebug > 0) {
               System.out.println (
                  "Choosing alternate tet: " + ibest.myTetDesc);
            }
            return ibest;
         }
      }
      return isect;
   }


   /**
    * Find the nearest quad surface intersection to p0, in the direction dir,
    * and return the result in ps. It is assumed that dir lies in the
    * plane. Input and output parameters are all given in grid local
    * coordinates. The method returns a descriptor of the tet/plane
    * intersection for the tet containing ps, unless ps is not found, in which
    * case null is returned.
    */
   public TetDesc findQuadSurfaceIntersectionLoc (
      Point3d ps, Point3d p0, Vector3d dir) {

      TetDesc tdesc = null;
      
      // find the quadratic cell containing p0
      Vector3i vxyz = new Vector3i();
      Vector3d xyz = new Vector3d();
      if (myGrid.getQuadCellCoords (
         xyz, vxyz, p0, myGrid.myQuadGridToLocal) == -1) {
         // shouldn't happen - grid should have enough margin to prevent this
         if (myDebug > 0) {
            System.out.println ("Not found: no quad cell found for p0");
         }
         return null;
      }
      // find the tet containing p0 within the quadratic cell
      tdesc = new TetDesc (vxyz, TetID.findSubTet (xyz.x, xyz.y, xyz.z));

      // now transform p0 and dir to quad grid coordinates
      Point3d p0Quad = new Point3d();      
      Vector3d dirQuad = new Vector3d();
      myGrid.myQuadGridToLocal.inverseTransformPnt (p0Quad, p0);
      myGrid.myQuadGridToLocal.inverseTransformVec (dirQuad, dir);

      if (myDebug > 0) {
         System.out.println ("looking for ps starting from " + tdesc);
      }

      // Find the interval (srng[0], srng[1]) within which the ray (p0, dir)
      // intersects tet/plane intersection defined by isect. This interval is
      // used to seed the surface intersection search. The edge of the
      // intersection boundary which clips the upper value of the range is
      // returned in edgeIdx, and the intersection parameter along that edge is
      // placed in edgeS.
      double[] srng = new double[] { 0, Double.MAX_VALUE };
      tdesc.clipLineSegment (srng, p0Quad, dirQuad);
      if (srng[0] > srng[1]) {
         // shouldn't happen - p0 should be in the tet
         if (myDebug > 0) {
            System.out.println (
               "Not found: p0 tet does not intersect ray (p0,dir)");
         }
         return null;
      }
      // Find the tet boundary feature associated with the upper bound of the
      // ray intersection. This will be used to search for adjacent tets in
      // case the surface intersection does not occur within the current tet.
      Point3d px = new Point3d();
      px.scaledAdd (srng[1], dir, p0);
      //TetFeature lastFeat = isect.getFeature (edgeS.value, edgeIdx);
      TetFeature lastFeat = findNearestFeature (tdesc, px, 1e-10);

      myVisitedTets.clear();
      myVisitedTets.add (tdesc);

      // Starting at p0, and following along the direction of dir, find the
      // first point ps at which (p0, dir) intersects the quadratic surface.
      while (findSurfaceIntersectionInTet (
                ps, tdesc, srng, p0Quad, dirQuad) == CONTINUE) {
         // A return value of CONTINUE means that the surface intersection was
         // not found in the current tet, and that we should look for the
         // intersection in adjacent tets.
         ArrayList<TetDesc> adescs = new ArrayList<TetDesc>();
         int ntets = getFeatureAdjacentTets (
            adescs, tdesc.getBaseVertex(null), 
            tdesc, lastFeat, myGrid, myVisitedTets);
         TetDesc tbest = null;
         double bestLen = 0;
         if (myDebug > 0) {
            System.out.println (
               "checking "+ntets+" adjacent tets for feature " + lastFeat);
            System.out.print ("  ");
            for (int k=0; k<ntets; k++) {
               System.out.print (adescs.get(k) + " ");
            }
            System.out.println ("");
         }
         // From among the adjacent tets, choose the one whose tet/plane
         // intersection intersects the ray over the longest length.
         for (int k=0; k<ntets; k++) {
            TetDesc adesc = adescs.get(k);
            double[] irng =
               new double[] { 0, Double.MAX_VALUE };
            adesc.clipLineSegment (irng, p0Quad, dirQuad);
            double ilen = irng[1]-irng[0];
            if (myDebug > 0) {
               System.out.println ("    ilen=" + ilen);
            }
            if (ilen > bestLen) {
               bestLen = ilen;
               tbest = adesc;
               srng[0] = irng[0];
               srng[1] = irng[1];
            }
         }
         if (tbest == null) {
            if (myDebug > 0) {
               System.out.println (
                  "Not found: no adjacent tets intersect the ray");
            }
            return null;
         }
         else {
            px.scaledAdd (srng[1], dir, p0);
            //lastFeat = ibest.getFeature (bestS, bestEdgeIdx);
            lastFeat = findNearestFeature (tbest, px, 1e-10);
            tdesc = tbest;
            myVisitedTets.add (tdesc);
         }
      }
      myGrid.myQuadGridToLocal.transformPnt (ps, ps);
      return tdesc;
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
      Vector3d dirLoc = new Vector3d();
      dirLoc.sub (paLoc, p0Loc);
      if (dirLoc.normSquared() == 0) {
         return false;
      }
      dirLoc.normalize();

      Point3d psLoc = new Point3d();
      TetDesc tdesc = 
         findQuadSurfaceIntersectionLoc (psLoc, p0Loc, dirLoc);
      if (tdesc == null) {
         return false;
      }
      else {
         myGrid.myLocalToWorld.transformPnt (pi, psLoc);
         return true;
      }
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

   /**
    * See if the ray (p0, dir) intersects the quadratic surface of a specific
    * tet/plane intersection (isect) within the interval defined by [srng[0],
    * srng[1]]. If it does, place the intersection point in pi and return
    * DONE. Otherwise, return CONTINUE.
    */
   int findSurfaceIntersectionInTet (
      Point3d pi, TetDesc tdesc,
      double[] srng, Point3d p0, Vector3d dir) {

      if (srng[0] >= srng[1]) {
         return CONTINUE;
      }

      // compute the coeficients c that describe the surface within the tet,
      // and use these to compute the coeficients c that describe the surface
      // curve within the plane
      double[] a = new double[10];
      myGrid.computeQuadCoefs (a, tdesc);         

      double px = p0.x-tdesc.myCXi/2;
      double py = p0.y-tdesc.myCYj/2;
      double pz = p0.z-tdesc.myCZk/2;
      
      double dx = dir.x;
      double dy = dir.y;
      double dz = dir.z;

      double aa = (a[0]*dx*dx + a[1]*dy*dy + a[2]*dz*dz +
                   a[3]*dy*dz + a[4]*dx*dz + a[5]*dx*dy);
      double bb = (2*(a[0]*px*dx + a[1]*py*dy + a[2]*pz*dz) +
                   a[3]*(py*dz+pz*dy) + a[4]*(px*dz+pz*dx) + a[5]*(px*dy+py*dx) +
                   a[6]*dx + a[7]*dy + a[8]*dz);
      double cc = (a[0]*px*px + a[1]*py*py + a[2]*pz*pz + a[3]*py*pz +
                   a[4]*px*pz + a[5]*px*py + a[6]*px + a[7]*py + a[8]*pz + a[9]);

      double[] roots = new double[2];
      int nr = QuadraticSolver.getRoots (roots, aa, bb, cc, srng[0], srng[1]);
      if (nr > 0) {
         // always take the value nearest to p0
         pi.scaledAdd (roots[0], dir, p0);

         if (myDebug > 0) {
            px = pi.x-tdesc.myCXi/2;
            py = pi.y-tdesc.myCYj/2;
            pz = pi.z-tdesc.myCZk/2;
            double phi = (
               a[0]*px*px + a[1]*py*py + a[2]*pz*pz +
               a[3]*py*pz + a[4]*pz*px + a[5]*px*py +
               a[6]*px + a[7]*py + a[8]*pz + a[9]);
            System.out.println (
               "   sx=" + roots[0] + " nr=" + nr + " phi=" + phi);
         }
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


      Point3d paCell = new Point3d();
      transformToQuadCell (paCell, pa, tdesc);

      if (myDebug > 0) {
         System.out.println ("  checking for tangent in " + tdesc); 
         System.out.println ("  pa=" + pa);
      }

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
            if (myDebug > 0) {
               System.out.println (
                  "    grad direction change; using entry point"); 
            }
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
         if (myDebug > 0) {
            System.out.println (
               "    isect=" + isect.myTetDesc + " numSides=" + isect.myNumSides);
         }
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
            if (myDebug > 0) {
               System.out.println (
                  "    nr=" + nr +
                  " pc0=" + pc0.toString("%10.5f") +
                  " pc1=" + pc1.toString("%10.5f"));
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
                     if (myDebug > 0) {
                        System.out.println ("      same as initial point");
                     }
                     // same as initial point, ignore
                     continue;
                  }
                  if (dela0.dot(grad)*sgnDotGrad < 0) {
                     // tangent direction switched sides; can't
                     // be part of the same curve section, ignore
                     if (myDebug > 0) {
                        System.out.println ("      tangent dir switched sides");
                     }
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
                     if (myDebug > 0) {
                        System.out.println (
                           "      best edge, bestS=" + bestS);
                     }
                     pt.set (pi);
                  }
               }
            }
            pl0 = pl1;
            pc0.set (pc1);
         }
         if (bestEdgeIdx != -1) {
            // need to find the feature associated with this point
            //sinfo.lastFeat = isect.getFeature (bestS, bestEdgeIdx);
            sinfo.lastFeat = findNearestFeature (isect.myTetDesc, pt, 1e-10);
            return CONTINUE;
         }
         else {
            return NONE;
         }
      }
   }

   protected int getFeatureAdjacentTets (
      Vector3i vidx0, TetDesc tdesc, TetFeature feat,
      Plane plane, HashSet<TetDesc> visited) {

      createConnectivityIfNecessary();

      Point3d[] vpnts = new Point3d[] {
         new Point3d(), new Point3d(), new Point3d(), new Point3d() };

      if (feat instanceof TetFace) {
         TetDesc adjDesc = myFaceTets.get((TetFace)feat);
         TetDesc adesc = new TetDesc(adjDesc);
         adesc.addVertexOffset (vidx0);
         if (myGrid.inRange(adesc) &&
             (visited == null || !visited.contains(adesc))) {
            getVertexCoords (vpnts, adesc);
            TetPlaneIntersection isect = getIsect(0);
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
            adesc.addVertexOffset (vidx0);
            if (myGrid.inRange(adesc) &&
                (visited == null || !visited.contains(adesc))) {
               // if tdesc != null, filter out the tet if:
               // a) feature is edge and tdesc == adesc;
               // b) feature is node and vidx0 is in the same quad hex
               if (tdesc != null) {
                  if (feat instanceof TetEdge) {
                     if (adesc.equals(tdesc)) {
                        continue;
                     }
                  }
                  else if (feat instanceof TetNode) {
                     if (adesc.baseVertexEquals(vidx0)) {
                        continue;
                     }
                  }
               }
               getVertexCoords (vpnts, adesc);
               TetPlaneIntersection isect = getIsect(numi);
               if (intersectTetAndPlane (isect, adesc, vpnts, plane)) {
                  numi++;
               }
//               if ((feat instanceof TetNode && !adesc.baseVertexEquals(vidx0)) ||
//                   (feat instanceof TetEdge && !adesc.equals(tdesc))) {
//                  getVertexCoords (vpnts, adesc);
//                  TetPlaneIntersection isect = getIsect(numi);
//                  if (intersectTetAndPlane (isect, adesc, vpnts, plane)) {
//                     numi++;
//                  }
//               }
            }
         }
         return numi;
      }
   }

   protected static int getFeatureAdjacentTets (
      ArrayList<TetDesc> adescs, Vector3i vidx0, TetDesc tdesc,
      TetFeature feat, DistanceGrid grid, HashSet<TetDesc> visited) {
      
      createConnectivityIfNecessary();

      if (feat instanceof TetFace) {
         TetDesc adjDesc = myFaceTets.get((TetFace)feat);
         TetDesc adesc = new TetDesc(adjDesc);
         adesc.addVertexOffset (vidx0);
         if (grid.inRange(adesc) && 
             (visited == null || !visited.contains(adesc))) {
            adescs.add (adesc);
            return 1;
         }
         else {
            return 0;
         }
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
            adesc.addVertexOffset (vidx0);
            if (grid.inRange(adesc) && 
                (visited == null || !visited.contains(adesc))) {
               // if tdesc != null, filter out the tet if:
               // a) feature is edge and tdesc == adesc;
               // b) feature is node and vidx0 is in the same quad hex
               if (tdesc != null) {
                  if (feat instanceof TetEdge) {
                     if (adesc.equals(tdesc)) {
                        continue;
                     }
                  }
                  else if (feat instanceof TetNode) {
                     if (adesc.baseVertexEquals(vidx0)) {
                        continue;
                     }
                  }
               }
               adescs.add (adesc);
               numi++;
               //  if ((tdesc == null) ||
               //     (feat instanceof TetNode && !adesc.baseVertexEquals(vidx0)) ||
               //     (feat instanceof TetEdge && !adesc.equals(tdesc))) {
               //       adescs.add (adesc);
               //       numi++;
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

   /**
    * Looks for a surface tangent solution within a specific tetrahedron.
    */
   protected boolean findSurfaceTangentCell (
      Point3d ptCell, TetDesc tdesc,
      double[] b, Vector3d r, PlaneType planeType,
      Point3d paCell) {
      // XXX convert normal, off, and pa to tet coordinates

      Vector2d[] pnts = new Vector2d[] { new Vector2d(), new Vector2d() };

      double x = 0, y = 0, z = 0;
      // EPS allows the potential solution to be slightly outside the tet,
      // to accomodate numeric errors
      double EPS = 1e-12;

      switch (planeType) {
         case YZ: {
            int nr = findTangentPoints (pnts, b, paCell.y, paCell.z);
            for (int i=0; i<nr; i++) {
               y = pnts[i].x;
               z = pnts[i].y;
               x = r.z - r.x*y - r.y*z;
               if (myDebug > 0) {
                  System.out.println ("      x,y,z=" + x + " " + y + " " + z);
               }
               if (tdesc.myTetId.isInside (x, y, z, EPS)) {
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
               if (tdesc.myTetId.isInside (x, y, z, EPS)) {
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
               if (tdesc.myTetId.isInside (x, y, z, EPS)) {
                  ptCell.set (x, y, z);
                  return true;
               }
            }
            break;
         }
      }
      return false;
   }

   /**
    * Intersect a ray (pc, dc) (in quad cell coordinates) with a quadratic
    * surface curve. If the ray intersects the curve at some point s within the
    * interval (srng[0], srng[1]), return the resulting s value. If there
    * are multiple intersections, return the one nearest to the ray
    * origin. If there is no intersection, return -1.
    */
   protected double findSurfaceIntersectionCell (
      TetDesc tdesc,
      double[] b, PlaneType planeType,
      double[] srng, Point3d pc, Vector3d dc) {
      // XXX convert normal, off, and pa to tet coordinates

      double[] svals = new double[2];

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
      pc.x = pc.x-tdesc.myCXi/2;
      pc.y = pc.y-tdesc.myCYj/2;
      pc.z = pc.z-tdesc.myCZk/2;     
   }

   public void transformToQuadCell (Vector3d vc, Vector3d v1, TetDesc tdesc) {
      vc.scale (0.5, v1); // transform to regular grid coords
      myGrid.myGridToLocal.inverseTransformVec (vc, vc);
   }

   public void transformFromQuadCell (
      Point3d pl, double x, double y, double z, TetDesc tdesc) {

      // transform to regular grid coords, then transform to local coords
      pl.set (tdesc.myCXi/2+x, tdesc.myCYj/2+y, tdesc.myCZk/2+z);
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
   }

   private void transformFromGrid (
      Vector3d ploc, double x, double y, double z) {
      myGrid.myGridToLocal.transformPnt (ploc, new Point3d(x, y, z));
   }

   /**
    * Compute the locations (in local coordinates) of the nodes of the
    * indicated tet.
    */
   public void getVertexCoords (Point3d[] v, TetDesc tdesc) {
      double xi = tdesc.myCXi;
      double yj = tdesc.myCYj;
      double zk = tdesc.myCZk;

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

   public int getDebug () {
      return myDebug;
   }

   public void setDebug (int level) {
      myDebug = level;
   }

}
