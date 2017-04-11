package maspack.geometry;

import java.util.*;
import maspack.matrix.*;
import maspack.util.*;

public class Polygon3dCalc {

   private static double DOUBLE_PREC = 2.0e-16;
   private static double EPS = 1000*DOUBLE_PREC;

   /**
    * Decsribes the dominant axis-aligned plane uses for computations.
    */
   public enum Plane { YZ, ZX, XY };

   protected Plane myPlane;
   protected double myInvNrmMax;
   double myDtol;
   double myNrmX;
   double myNrmY;
   double myNrmZ;
   public boolean debug;

   public Polygon3dCalc () {
   }

   public Polygon3dCalc (Vector3d nrm, double dtol) {
      setPlane (nrm);
      myDtol = dtol;
   }

   public void setPlane (Vector3d nrm) {
      switch (nrm.maxAbsIndex()) {
         case 0: {
            myPlane = Plane.YZ;
            myInvNrmMax = 1/nrm.x;
            break;
         }
         case 1: {
            myPlane = Plane.ZX;
            myInvNrmMax = 1/nrm.y;
            break;
         }
         case 2: {
            myPlane = Plane.XY;
            myInvNrmMax = 1/nrm.z;
            break;
         }
      }            
      myNrmX = nrm.x;
      myNrmY = nrm.y;
      myNrmZ = nrm.z;
   }

   public double computeArea (Vector3d p0, Vector3d p1, Vector3d p2) {
      double a;
      switch (myPlane) {
         case YZ: {
            double d10y = p1.y - p0.y;
            double d10z = p1.z - p0.z;
            double d20y = p2.y - p0.y;
            double d20z = p2.z - p0.z;
            a = d10y*d20z - d10z*d20y;
            break;
         }
         case ZX: {
            double d10z = p1.z - p0.z;
            double d10x = p1.x - p0.x;
            double d20z = p2.z - p0.z;
            double d20x = p2.x - p0.x;
            a = d10z*d20x - d10x*d20z;
            break;
         }
         case XY: {
            double d10x = p1.x - p0.x;
            double d10y = p1.y - p0.y;
            double d20x = p2.x - p0.x;
            double d20y = p2.y - p0.y;
            a = d10x*d20y - d10y*d20x;
            break;
         }
         default: {
            throw new InternalErrorException (
               "Plane has not been initialized");
         }
      }
      return a*0.5*myInvNrmMax;
   }

   String planeStr (Vector3d v, String fmtStr) {
      NumberFormat fmt = new NumberFormat (fmtStr);
      switch (myPlane) {
         case YZ: {
            return fmt.format(v.y) + " " + fmt.format(v.z);
         }
         case ZX: {
            return fmt.format(v.z) + " " + fmt.format(v.x);
         }
         case XY: {
            return fmt.format(v.x) + " " + fmt.format(v.y);
         }
      }
      return "";
   }

   /**
    * Returns < 0 for a right turn, > 0 for a left turn, and 0 for no turn.
    */
   double turn (Vector3d a, Vector3d b) {
      double turn;
      switch (myPlane) {
         case YZ: {
            turn = (a.y*b.z - a.z*b.y)*myInvNrmMax;
            break;
         }
         case ZX: {
            turn = (a.z*b.x - a.x*b.z)*myInvNrmMax;
            break;
         }
         case XY: {
            turn = (a.x*b.y - a.y*b.x)*myInvNrmMax;
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented plane type "+myPlane);
         }
      }
      return turn;
   }

   public boolean leftTurn (Vector3d p0, Vector3d p1, Vector3d p2, double tol) {
      return computeArea (p0, p1, p2) > tol;
   }

   public boolean rightTurn (Vector3d p0, Vector3d p1, Vector3d p2, double tol) {
      return computeArea (p0, p1, p2) < -tol;
   }

   public boolean vertexIsReflex (Vertex3dNode node) {
      Vertex3dNode prev = node.prev;
      Vertex3dNode next = node.next;      
      return rightTurn (prev.vtx.pnt, node.vtx.pnt, next.vtx.pnt, 0);
   }

   public boolean insideTriangle (
      Vector3d px, Vector3d p0, Vector3d p1, Vector3d p2) {
      return (!rightTurn (p0, p1, px, 0) &&
              !rightTurn (p1, p2, px, 0) &&
              !rightTurn (p2, p0, px, 0));
   }

   public boolean segmentsOverlap (
      Vector3d p0, Vector3d p1, Vector3d p2, double atol) {

      double area012 = computeArea (p0, p1, p2);
      if (Math.abs(area012) <= atol) {
         // segments are coliner; check dot product
         Vector3d d01 = new Vector3d();
         Vector3d d12 = new Vector3d();
         d01.sub (p1, p0);
         d12.sub (p2, p1);
         return (d01.dot(d12) < 0);
      }
      else {
         return false;
      }
   }

   // public boolean insideTriangle (
   //    Vector3d px, Vector3d p0, Vector3d p1, Vector3d p2) {
   //    return (leftTurn (p0, p1, px) &&
   //            leftTurn (p1, p2, px) &&
   //            leftTurn (p2, p0, px));
   // }

   /**
    * Returns true if pp lies on or inside the corner region for the indicated
    * node
    */
   private boolean insideNodeCorner (PolyNode node, Vector3d pp, double atol) {

      Point3d p0 = node.prev.vtx.pnt;
      Point3d p1 = node.vtx.pnt;
      Point3d p2 = node.next.vtx.pnt;

      if (leftTurn (p0, p1, p2, 0)) {
         // corner is convex; pp is not inside if it forms a right turn with
         // either edge
         return (!rightTurn (p0, p1, pp, atol) && !rightTurn (p1, p2, pp, atol));
      }
      else {
         // corner is concave; pp is not inside if it forms a right turn with
         // both edges
         return (!rightTurn (p0, p1, pp, atol) || !rightTurn (p1, p2, pp, atol));
      }
   }

   public boolean segmentsIntersect (
      PolyNode node0, PolyNode node1,
      PolyNode nodep, Vertex3dNode nodeh, double atol) {
      
      boolean debug = false;

      if (node0 == nodep) {
         // if (segmentsOverlap (
         //        node1.vtx.pnt, node0.vtx.pnt, nodeh.vtx.pnt, atol)) {
         //    return true;
         // }
         if (node0.opposite != null) {
            return insideNodeCorner (node0.opposite, nodeh.vtx.pnt, atol);
         }
         else {
            return false;
         }
      }
      else if (node0.opposite == nodep) {
         if (segmentsOverlap (
                node1.vtx.pnt, node0.opposite.vtx.pnt, nodeh.vtx.pnt, atol)) {
            return true;
         }
         else {
            return insideNodeCorner (node0, nodeh.vtx.pnt, atol);
         }
      }
      else if (node1 == nodep) {
         if (debug) {
            System.out.println ("FOUND op=" + node1.opposite.vtx.getIndex());
         }      
         // if (segmentsOverlap (
         //        node0.vtx.pnt, node1.vtx.pnt, nodeh.vtx.pnt, atol)) {
         //    return true;
         // }
         if (node1.opposite != null) {
            return insideNodeCorner (node1.opposite, nodeh.vtx.pnt, atol);
         }
         else {
            return false;
         }
      }
      else if (node1 == nodep.opposite) {
         if (segmentsOverlap (
                node0.vtx.pnt, node1.opposite.vtx.pnt, nodeh.vtx.pnt, atol)) {
            return true;
         }
         else {
            return insideNodeCorner (node1, nodeh.vtx.pnt, atol);
         }
      }
      else {
         if (debug) {
            System.out.println ("REG");
         }
         return segmentsIntersect (
            node0.vtx.pnt, node1.vtx.pnt, nodep.vtx.pnt, nodeh.vtx.pnt,
            atol, debug);
      }
   }

   public boolean segmentsIntersect (
      Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3,
      double atol, boolean debug) {

      double area012 = computeArea (p0, p1, p2);
      double area013 = computeArea (p0, p1, p3);
      if ((area012 < -atol && area013 < -atol) || 
          (area012 > atol && area013 > atol)) {
         // p2 and p3 both of the same side of (p0,p1), so no intersection
         if (debug) {
            System.out.println ("same side");
         }
         return false;
      }
      if (Math.abs(area012) >= atol || Math.abs(area013) >= atol) {
         double area023 = computeArea (p0, p2, p3);
         double area123 = computeArea (p1, p2, p3);
         if (debug) {
            System.out.println ("area023*area123 = "+area023*area123);
         }
         return area023*area123 <= 0;
      }
      else {
         // line segments are almost colinear, so compute overlap using dot
         // products instead
         Vector3d del01 = new Vector3d();
         Vector3d r2 = new Vector3d();
         Vector3d r3 = new Vector3d();
         del01.sub (p1, p0);
         r2.sub (p2, p1);
         r3.sub (p3, p1);
         if (r2.dot (del01) > 0 && r3.dot (del01) > 0) {
            return false;
         }
         r2.sub (p2, p0);
         r3.sub (p3, p0);
         if (r2.dot (del01) < 0 && r3.dot (del01) < 0) {
            return false;
         }
         else {
            return true;
         }
      }
   }

   private void appendTriangle (ArrayList<Vertex3d> triVtxs, Vertex3dNode node) {
      Vertex3dNode prev = node.prev;
      Vertex3dNode next = node.next;
      // if (node.vtx.getIndex()==1 &&
      //     next.vtx.getIndex()==1 && 
      //     prev.vtx.getIndex()==1) {
      //    (new Throwable()).printStackTrace(); 
      // }

      triVtxs.add (node.vtx);
      triVtxs.add (next.vtx);
      triVtxs.add (prev.vtx);
   }

   public static int numOutsideTests = 0;
   public static int numOutsideAmbiguous = 0;

   /**
    * Return 1 if pn is inside (to the left of) the directed line segment
    * defined by p0-p1, 0 if it is outside, and -1 if this cannot
    * be determined within atol.
    */
   int insideEdge (Point3d pn, Point3d p0, Point3d p1, double atol) {

      double area;

      area = computeArea (p0, p1, pn);
      if (area < -atol) {
         // definitely a right turn
         return 0;
      }
      else if (area > atol) {
         // definitely a left turn
         return 1;
      }
      else {
         return -1;
      }
   }

   /**
    * Return 1 if pn is inside the triangle defined by p0, p1, p2, 0 if it is
    * outside, and -1 if this cannot be determined within atol.
    */
   int insideTriangle (
      Point3d pn, Point3d p0, Point3d p1, Point3d p2, double atol) {

      int res01 = insideEdge (pn, p0, p1, atol);
      if (res01 == 0) {
         return 0;
      }
      int res12 = insideEdge (pn, p1, p2, atol);
      if (res12 == 0) {
         return 0;
      }
      int res20 = insideEdge (pn, p2, p0, atol);
      if (res20 == 0) {
         return 0;
      }
      if (res01 == 1 && res12 == 1 && res20 == 1) {
         return 1;
      }
      else {
         return -1;
      }
   }

   boolean outsideEdge (
      Vertex3dNode n, Vertex3dNode vn0, Vertex3dNode vn1,
      double atol, boolean debug) {
      double area;

      numOutsideTests++;
      Point3d pn = n.vtx.pnt;
      Point3d p0 = vn0.vtx.pnt;
      Point3d p1 = vn1.vtx.pnt;

      area = computeArea (p0, p1, pn);

      if (area < -atol) {
         // definitely a right turn
         if (debug) {
            System.out.println ("    right turn");
         }
         return true;
      }
      else if (area > atol) {
         // definitely a left turn
         if (debug) {
            System.out.println ("    left turn");
         }
         return false;
      }
      else {
         // XXX need to check n.prev == null 

         // Ambiguous. Try to see if any of the edges attached to n are
         // partially inside the edge

         boolean inside = false;

         Vector3d del01 = new Vector3d();
         Vector3d r0 = new Vector3d();
         Vector3d r1 = new Vector3d();
         del01.sub (p1, p0);
         r0.sub (pn, p0);
         r1.sub (pn, p1);

         if (debug) {
            System.out.println ("    ambiguous:");
         }
         if (r0.dot (del01) > 0 && r1.dot (del01) < 0) {
            // point lies on (p0, p1), so if either vertex adjacent to n
            // is inside, we are not outside edge
            if (debug) {
               System.out.println ("      on edge");
            }
            if (leftTurn (p0, p1, n.prev.vtx.pnt, 0)) {
               if (debug) {
                  System.out.println ("      prev inside");
               }
               inside = true;
            }
            if (leftTurn (p0, p1, n.next.vtx.pnt, 0)) {
               if (debug) {
                  System.out.println ("      next inside");
               }
               inside = true;
            }
         }
         else if (r0.dot (del01) == 0 || r1.dot (del01) == 0) {
            numOutsideAmbiguous++;
         }
         return !inside;
         //return area > 0;
      }
   }

   boolean insideNodeTriangleOld (
      Vertex3dNode n, 
      Vertex3dNode vn0, Vertex3dNode vn1, Vertex3dNode vn2,
      double atol, boolean debug) {

      Point3d pn = n.vtx.pnt;
      double area = computeArea (vn2.vtx.pnt, vn0.vtx.pnt, pn);
      if (area < -atol) {
         if (debug) {
            System.out.println ("  outside 20");
         }
         return false;
      }
      if (debug) {
         System.out.println ("  testing 01:");
      }
      if (outsideEdge (n, vn0, vn1, atol, debug)) {
         if (debug) {
            System.out.println ("  outside 01");
         }
         return false;
      }
      if (debug) {
         System.out.println ("  testing 12:");
      }
      if (outsideEdge (n, vn1, vn2, atol, debug)) {
         if (debug) {
            System.out.println ("  outside 12");
         }
         return false;
      }
      return true;
   }

   boolean insideNodeTriangle (
      Vertex3dNode n, 
      Vertex3dNode vn0, Vertex3dNode vn1, Vertex3dNode vn2,
      double atol, boolean debug) {

      Point3d pn = n.vtx.pnt;
      Point3d p0 = vn0.vtx.pnt;
      Point3d p1 = vn1.vtx.pnt;
      Point3d p2 = vn2.vtx.pnt;

      // test the edges of the triangle
      int inside01 = insideEdge (pn, p0, p1, atol);
      if (debug) {
         System.out.println ("  inside01=" + inside01);
      }
      if (inside01 == 0) {
         return false;
      }
      int inside12 = insideEdge (pn, p1, p2, atol);
      if (debug) {
         System.out.println ("  inside12=" + inside12);
      }
      if (inside12 == 0) {
         return false;
      }
      int inside20 = insideEdge (pn, p2, p0, atol);
      if (debug) {
         System.out.println ("  inside20=" + inside20);
      }
      if (inside20 == 0) {
         return false;
      }
      if (inside01 == 1 && inside12 == 1) {
         return true;
      }
      boolean inside = false;
      if (inside01 == -1 && inside12 == -1) {
         if (debug) {
            System.out.println ("  near p1:");
         }
         // n is on or very close to p1. Return inside if either n.prev or
         // n.next is inside the cone associated with p1
         if (leftTurn (p0, p1, n.prev.vtx.pnt, 0) &&
             leftTurn (p1, p2, n.prev.vtx.pnt, 0)) {
            if (debug) {
               System.out.println ("  prev inside cone");
            }
            return true;
         }
         if (leftTurn (p0, p1, n.next.vtx.pnt, 0) &&
             leftTurn (p1, p2, n.next.vtx.pnt, 0)) {
            if (debug) {
               System.out.println ("  next inside cone");
            }
            return true;
         }
         return false;
      }
      else if (inside01 == -1) {
         if (debug) {
            System.out.println ("  near 01:");
         }
         if (inside20 == 1) { 
            // n is near the segment 0-1. Return inside if either n.prev or n.next
            // is also inside the segment.
            if (leftTurn (p0, p1, n.prev.vtx.pnt, 0) ||
                leftTurn (p0, p1, n.next.vtx.pnt, 0)) {
               inside = true;
               if (debug) {
                  System.out.println ("  prev or next inside 01");
               }
            }
         }
      }
      else if (inside12 == -1) {
         if (debug) {
            System.out.println ("  near 12:");
         }
         if (inside20 == 1) {
            // n is near the segment 1-2. Return inside if either n.prev or n.next
            // is also inside the segment.
            if (leftTurn (p1, p2, n.prev.vtx.pnt, 0) ||
                leftTurn (p1, p2, n.next.vtx.pnt, 0)) {
               inside = true;
               if (debug) {
                  System.out.println ("  prev or next inside 12");
               }
            }
         }
      }
      return inside;
   }

   boolean isEarVertex (
      Vertex3dNode node, ArrayList<PolyNode> reflex, 
      Vertex3dList poly, double atol) {
      Vertex3dNode prev = node.prev;
      Vertex3dNode next = node.next;

      Point3d p0 = prev.vtx.pnt;
      Point3d p1 = node.vtx.pnt;
      Point3d p2 = next.vtx.pnt;
      for (Vertex3dNode r : reflex) {
         Point3d pr = r.vtx.pnt;
         if (pr != p0 && pr != p2) {
            boolean inside = insideNodeTriangle (
               r, prev, node, next, atol, false);
            if (false) {
               boolean insideOld = insideNodeTriangleOld (
                  r, prev, node, next, atol, false);
               if (insideOld != inside) {
                  System.out.println (
                     "XXX insideOld=" + insideOld + " inside=" + inside);
                  System.out.println (
                     " r=" + planeStr(r.vtx.pnt,"%18.14f"));
                  System.out.println (
                     " prev=" + planeStr(r.prev.vtx.pnt,"%18.14f"));
                  System.out.println (
                     " next=" + planeStr(r.next.vtx.pnt,"%18.14f"));
                  System.out.println (
                     "p0=" + planeStr(prev.vtx.pnt,"%18.14f"));
                  System.out.println (
                     "p1=" + planeStr(node.vtx.pnt,"%18.14f"));
                  System.out.println (
                     "p2=" + planeStr(next.vtx.pnt,"%18.14f"));
                  System.out.println ("insideNodeTriangleOld:");
                  insideNodeTriangleOld (r, prev, node, next, atol, true);
                  System.out.println ("insideNodeTriangle:");
                  insideNodeTriangle (r, prev, node, next, atol, true);
               }
            }
            if (inside) {
               return false;
            }
         }
      }
      return true;
   }

   private class PolyNode extends Vertex3dNode {
//      /Vertex3dNode rnode;
      boolean isReflex;
      double maxCos; // cosine of the node triangle with max abs value
      double minSin; // minimum absolute sine of the node triangle
      boolean isEar;
      // oposite node, which will exist if this node is one of the end nodes of
      // a "bridge" edge connecting an original polygon with an internal hole.
      PolyNode opposite;

      PolyNode (Vertex3d vtx) {
         super (vtx);
         maxCos = -2;
         minSin = -1;
         isEar = false;
      }

      /**
       * Compute the cosine with the maximum absolute value among the three
       * angles of the triangle associated with this vertex and its adjacent
       * vertices.
       */
      double computeMaxCos () {
         
         Point3d p0 = prev.vtx.pnt;
         Point3d p1 = vtx.pnt;
         Point3d p2 = next.vtx.pnt;

         Vector3d u01 = new Vector3d();
         Vector3d u12 = new Vector3d();
         Vector3d u20 = new Vector3d();

         u01.sub (p1, p0);
         u01.normalize();
         u12.sub (p2, p1);
         u12.normalize();
         u20.sub (p0, p2);
         u20.normalize();

         double maxCos = u20.dot(u01);
         double c = u01.dot(u12);
         if (Math.abs(c) > Math.abs(maxCos)) {
            maxCos = c;
         }
         c = u12.dot(u20);
         if (Math.abs(c) > Math.abs(maxCos)) {
            maxCos = c;
         }
         if (maxCos > 1.0) {
            maxCos = 1.0;
         }
         else if (maxCos < -1.0) {
            maxCos = -1.0;
         }
         return maxCos;
      }

      /**
       * Computes the minimum sine value associated with the angles of the
       * triangle associated with this vertex and its adjacent vertices.
       */
      double computeMinSin () {

         Point3d p0 = prev.vtx.pnt;
         Point3d p1 = vtx.pnt;
         Point3d p2 = next.vtx.pnt;

         Vector3d u01 = new Vector3d();
         Vector3d u12 = new Vector3d();
         Vector3d u20 = new Vector3d();

         u01.sub (p1, p0);
         u01.normalize();
         u12.sub (p2, p1);
         u12.normalize();
         u20.sub (p0, p2);
         u20.normalize();

         Vector3d xprod = new Vector3d();

         xprod.cross (u20,u01);
         double minSin = xprod.norm();
         xprod.cross (u01,u12);
         double s = xprod.norm();
         if (s < minSin) {
            minSin = s;
         }
         xprod.cross (u12, u20);
         s = xprod.norm();
         if (s < minSin) {
            minSin = s;
         }

         // look also at the nodes pb and pa which follow p2 and preceed p0,
         // respectively, and compute the angles that p2-pb and pa-p0 make
         // with the line segment p2-p0. It is assumed that the polygon
         // has at least four vertices, so that pb != p0 and pa != p2.

         Vector3d u2b = u12; // reuse storage
         Vector3d ua0 = u01; // reuse storage

         u2b.sub (next.next.vtx.pnt, p2);
         if (u2b.dot (u20) > 0) {
            // only care if the angle 0-2-b is acute
            u2b.normalize();
            xprod.cross (u2b, u20);
            s = xprod.norm();
            if (s < minSin) {
               minSin = s;
            }
         }

         ua0.sub (p0, prev.prev.vtx.pnt);
         if (ua0.dot (u20) > 0) {
            // only care if the angle a-0-2 is obtuse            
            ua0.normalize();
            xprod.cross (u20, ua0);
            s = xprod.norm();
            if (s < minSin) {
               minSin = s;
            }
         }

         return minSin;
      }

      /**
       * Returns the cosine with the maximum absolute value among the three
       * angles of the triangle associated with this vertex and its adjacent
       * vertices. Note the cosine value itself is signed, so that a positive
       * value indicates an acute angle and a negative valuea obtuse angle.
       */
      double getMaxCos() {
         if (maxCos == -2) {
            maxCos = computeMaxCos();
         }
         return maxCos;
      }            

      double getMinSin() {
         if (minSin == -1) {
            minSin = computeMinSin();
         }
         return minSin;
      }            

      double updateMinSin() {
         minSin = computeMinSin();
         return minSin;
      }

      void clearMaxCos() {
         maxCos = -2;
         minSin = -1;
      }

   }

   private class PolyComparator implements Comparator<PolyNode> {
      public int compare (PolyNode v1, PolyNode v2) {

         if (false) {
            double maxCos1 = Math.abs(v1.getMaxCos());
            double maxCos2 = Math.abs(v2.getMaxCos());
            if (maxCos1 < maxCos2) {
               return -1;
            }
            else if (maxCos1 == maxCos2) {
               return 0;
            }
            else {
               return 1;
            }
         }
         else {
            double minSin1 = v1.getMinSin();
            double minSin2 = v2.getMinSin();
            if (minSin1 > minSin2) {
               return -1;
            }
            else if (minSin1 == minSin2) {
               return 0;
            }
            else {
               return 1;
            }
         }
      }
   }

   private void updateReflex (PolyNode prev, ArrayList<PolyNode> reflex) {
      if (prev.isReflex) {
         // node is reflex - see if it still is 
         if (!vertexIsReflex (prev)) {
            //System.out.println ("  changed to convex: "+prev.vtx.getIndex());
            reflex.remove (prev);
            prev.isReflex = false;
         }
      }
   }

   private void removeNode (
      PolyNode node, Vertex3dList poly,
      PriorityQueue<PolyNode> ears, ArrayList<PolyNode> reflex) {

      poly.remove (node);
      if (node.isEar) {
         ears.remove (node);
      }
      else {
         reflex.remove (node);
      }
   }

   private void updateEars (
      PolyNode node, PriorityQueue<PolyNode> ears,
      ArrayList<PolyNode> reflex, Vertex3dList poly, double atol) {

      if (!node.isReflex) {
         node.clearMaxCos();
         boolean wasEar = true;
         if (node.isEar) {
            ears.remove (node);
            node.isEar = false;
         }
         if (isEarVertex (node, reflex, poly, atol)) {
            ears.add (node);
            node.isEar = true;
            if (debug) {
               System.out.println (
                  " added ear: "+node.vtx.getIndex() + " " + node.getMinSin());
            }
            
         }
         else if (wasEar) {
            if (debug) {
               System.out.println (
                  "  removed ear: "+node.vtx.getIndex() +
                  " " + node.getMinSin());
            }
         }
      }
   }


   public void triangulate (
      ArrayList<Vertex3d> triVtxs,
      Iterable<Vertex3d> vtxs, List<? extends Iterable<Vertex3d>> holes) {

      Vertex3dList poly = new Vertex3dList(/*closed=*/true);
      for (Vertex3d vtx : vtxs) {
         poly.add (new PolyNode(vtx));
      }
      ArrayList<Vertex3dList> holePolys = null;
      if (holes != null) {
         holePolys = new ArrayList<Vertex3dList>(holes.size());
         for (Iterable<Vertex3d> hole : holes) {
            Vertex3dList holePoly = new Vertex3dList(/*closed=*/true);
            for (Vertex3d vtx : hole) {
               holePoly.add (new PolyNode(vtx));
            }
            holePolys.add (holePoly);
         }
      }
      triangulate (triVtxs, poly, holePolys);
   }

   public boolean isConvex (Vertex3dList poly) {
      if (!poly.isClosed()) {
         return false;
      }
      if (poly.size() < 4) {
         return true;
      }
      Vertex3dNode node = poly.getNode(0);
      Point3d p0 = node.vtx.pnt;
      node = node.next;
      Point3d p1 = node.vtx.pnt;
      node = node.next;
      Point3d p2 = node.vtx.pnt;
      Vertex3dNode node2 = node;
      do {
         if (rightTurn (p0, p1, p2, 0)) {
            return false;
         }
         p0 = p1;
         p1 = p2;
         node = node.next;
         p2 = node.vtx.pnt;
      }
      while (node != node2);
      return true;
   }
      
   /**
    * Triangulation based roughly on "FIST: Fast Industrial Strength
    * Triangulation of Polygons", by Martin Held.
    */
   public boolean triangulate (
      ArrayList<Vertex3d> triVtxs, Vertex3dList poly, List<Vertex3dList> holes) {

      Vertex3dList copy = new Vertex3dList(/*closed=*/true);
      for (Vertex3dNode node : poly) {
         copy.add (new PolyNode (node.vtx));
      }

      // if (debug) {
      //    for (Vertex3dNode node : poly) {
      //       System.out.println ("  " + node.vtx.pnt.toString("%8.3f"));
      //    }
      //    System.out.println ("convex=" + isConvex(poly));
      // }

      double atol = myDtol; // myDtol*myDtol;
      if (holes != null && holes.size() > 0) {
         mergeHoles (copy, holes, atol);
      }

      triVtxs.ensureCapacity (triVtxs.size()+3*(copy.size()-2));
      //triVtxs.clear();

      if (copy.size() < 4) {
         appendTriangle (triVtxs, copy.getNode(0)); 
         return true;
      }
      if (copy.size() == 4 && isConvex (poly)) {

         PolyNode node0 = (PolyNode)copy.getNode(0);
         PolyNode node1 = (PolyNode)node0.next;
         double sin301 = node0.getMinSin();
         double sin012 = node1.getMinSin();

         if (sin301 > sin012) {
            appendTriangle (triVtxs, node0);
            appendTriangle (triVtxs, node1.next);
         }
         else {
            appendTriangle (triVtxs, node1);
            appendTriangle (triVtxs, node0.prev);
         }
      }
      else {
         ArrayList<PolyNode> reflex = new ArrayList<PolyNode>();
         PriorityQueue<PolyNode> ears =
            new PriorityQueue<PolyNode>(copy.size(), new PolyComparator());

         // iterate through all the vertices and find the ones which are
         // reflexive and which ones are ears.
         ArrayList<PolyNode> convex = new ArrayList<PolyNode>(copy.size());
         for (Vertex3dNode node : copy) {
            PolyNode pnode = (PolyNode)node;
            if (vertexIsReflex (node)) {
               reflex.add (pnode);
               pnode.isReflex = true;
            }
            else {
               convex.add (pnode);
            }
         }
         // now iterate through all the convex vertices and find which
         // ones are ears
         for (PolyNode pnode : convex) {
            if (isEarVertex (pnode, reflex, copy, atol)) {
               if (debug) {
                  System.out.println (
                     " isEar " + pnode.vtx.getIndex() +
                     " " + pnode.getMinSin());
               }
               pnode.isEar = true;
               ears.add (pnode);
            }
         }
         // Next, removes ears one by one, removing the best conditioned ones
         // first. As each ear is removed, the two remaining vertices are
         // updated so see if they are reflexive and if they are ears.
         while (copy.size() >= 4) {
            PolyNode ear = ears.poll();
            if (ear == null) {
               System.out.println ("poly has no ears");
               for (Vertex3dNode node : poly) {
                  System.out.println (
                     " " + node.vtx.pnt.toString());
               }
               System.out.println ("dtol=" + myDtol);
               System.out.println ("nrm=" + myNrmX+" "+myNrmY+" "+myNrmZ);
               return false;
            }
            if (debug) {
               System.out.println (
                  " clip " + ear.vtx.getIndex() +
                  " " + ear.vtx.pnt.toString("%12.7f") + " minsin=" + 
                  ear.getMinSin());
            }

            appendTriangle (triVtxs, ear);
            PolyNode prev = (PolyNode)ear.prev;
            PolyNode next = (PolyNode)ear.next;
            copy.remove (ear);

            // In situations where the polygon contains a "pinch point", the
            // ear clipping may reduce to a situation where removing the ear
            // amounts to removing a single isolated triangle.  This should
            // always be associated with a minimum sin near 0, since the ear
            // removal itself will cause the triangle to collapse to a line
            // segment.  The first two cases below identify such situations,
            // upon which the remaining two vertices of the triangle are also
            // removed from the polygon and the associated with the pinch point
            // is updated.
            if (ear.getMinSin() < EPS &&
                prev.vtx.pnt.equals (next.next.vtx.pnt)) {
               // triangle is isolated with the pinch point at 'prev'
               if (debug) {
                  System.out.println (
                     " removed isolated (prev) at "+ear.vtx.getIndex());
               }
               PolyNode node = next;
               next = (PolyNode)node.next;
               removeNode (node, copy, ears, reflex);
               node = next;
               next = (PolyNode)node.next;
               removeNode (node, copy, ears, reflex);
               updateReflex (prev, reflex);
               updateEars (prev, ears, reflex, copy, atol);              
            }
            else if (ear.getMinSin() < EPS &&
                     next.vtx.pnt.equals (prev.prev.vtx.pnt)) {
               // triangle is isolated with the pinch point at 'next'
               if (debug) {
                  System.out.println (
                     " removed isolated (next) at "+ear.vtx.getIndex());
               }
               PolyNode node = prev;
               prev = (PolyNode)node.prev;
               removeNode (node, copy, ears, reflex);
               node = prev;
               prev = (PolyNode)node.prev;
               removeNode (node, copy, ears, reflex);
               updateReflex (next, reflex);
               updateEars (next, ears, reflex, copy, atol);              
            }
            else {
               // normal case
               updateReflex (prev, reflex);
               updateReflex (next, reflex);
               updateEars (prev, ears, reflex, copy, atol);
               updateEars (next, ears, reflex, copy, atol);
               //nodes two away on either side might have their minSinValues
               //changed if they are ears; if so, recompute and reinsert into the
               //priority queue
               PolyNode pprev =(PolyNode)prev.prev;
               if (pprev.isEar &&
                   Math.abs(pprev.getMinSin()-pprev.updateMinSin()) > EPS) {
                  if (debug) {
                     System.out.println (
                        " reset ear: "+pprev.vtx.getIndex()+
                        " "+pprev.getMinSin());
                  }
                  ears.remove (pprev);
                  ears.add (pprev);
               }
               PolyNode nnext =(PolyNode)next.next;
               if (nnext != pprev && nnext.isEar &&
                   Math.abs(nnext.getMinSin()-nnext.updateMinSin()) > EPS) {
                  if (debug) {
                     System.out.println (
                        " reset ear: "+nnext.vtx.getIndex()+
                        " "+nnext.getMinSin());
                  }
                  ears.remove (nnext);
                  ears.add (nnext);
               }
            }
         }
         appendTriangle (triVtxs, copy.getNode(0));
      }
      return true;
   }      

   public void nearestFeature (
      Polygon3dFeature feat, Point3d px, Vertex3dList poly, int side) {
      
      if (feat.getDistance() == -1) {
         feat.myVtxDist = Double.POSITIVE_INFINITY;
         feat.myEdgeDist = Double.POSITIVE_INFINITY;
         feat.myPA = null;
         feat.myPB = null;
         feat.myPC = null;
      }
      if (poly.size() == 1) {
         // only a single point, so update vtxDistance
         Point3d p0 = poly.get(0).pnt;
         double d = px.distance (p0);
         if (d < feat.myVtxDist) {
            feat.myPA = p0;
            feat.myVtxDist = d;
            // only one point, so inside/outside undefined
            feat.myOutsideIfClockwise = -1; 
         }
         return;
      }
      Vertex3dNode firstNode = poly.getNode(0);
      Vertex3dNode node0 = firstNode;
      Vertex3dNode node1 = node0.next;

      Vector3d r0 = new Vector3d();      
      Vector3d r1 = new Vector3d();
      Vector3d dir01 = new Vector3d();
      Vector3d dir12 = new Vector3d();

      do {
         Point3d p1 = node1.vtx.pnt;
         Point3d p0 = node0.vtx.pnt;

         dir01.sub (p1, p0);

         r0.sub (px, p0);
         r1.sub (px, p1);

         if (dir01.dot (r1) > 0) {
            if (node1.next != null) {
               Point3d p2 = node1.next.vtx.pnt;
               dir12.sub (p2, p1);
               if (dir12.dot (r1) < 0) {
                  double turn012 = turn(dir01,dir12);
                  if (side == 0 || side*turn012 < 0) {
                     double d = r1.norm();
                     if (d < feat.myEdgeDist) {
                        feat.myEdgeDist = d;
                        feat.setNearestVertex (p0, p1, p2, turn012 < 0);
                     }
                  }
               }
            }
            else {
               double d = r1.norm();
               if (d < feat.myEdgeDist) {
                  feat.setNearestEdge (p0, p1, turn (dir01, r1) > 0);
                  feat.myEdgeDist = d;
               }
            }
         }
         else if (dir01.dot (r0) < 0) {
            if (node0.prev == null) {
               double d = r0.norm();
               if (d < feat.myEdgeDist) {
                  feat.setNearestEdge (p0, p1, turn (dir01, r0) > 0);
                  feat.myEdgeDist = d;
               }
            }
         }
         else {
            // point is along the current line segment (p0, p1)
            double area01X = turn(dir01,r0);
            if (side == 0 || side*area01X > 0) {
               double d = Math.abs (area01X/dir01.norm());
               if (d < feat.myEdgeDist) {
                  feat.setNearestEdge (p0, p1, area01X > 0);
                  feat.myEdgeDist = d;
               }
            }
         }
         node0 = node1;
         node1 = node1.next;
      }
      while (node1 != null && node0 != firstNode);

      if (poly.size() == 2 && poly.isClosed()) {
         // only two points, so inside/outside undefined
         feat.myOutsideIfClockwise = -1; 
      }
   }

   public boolean intersectsSegment (
      Point3d q0, Point3d q1, Vertex3dList poly, double atol) {

      Vertex3dNode firstNode = poly.getNode(0);
      Vertex3dNode node0 = firstNode;
      Vertex3dNode node1 = node0.next;

      do {
         if (segmentsIntersect (
                node0.vtx.pnt, node1.vtx.pnt, q0, q1, atol, false)) {
            return true;
         }
         node0 = node1;
         node1 = node1.next;
      }
      while (node1 != null && node0 != firstNode);
      return false;
   }

   private class HoleInfo {
      Vertex3dList myHole;
      double myLeftDirDot;
      ArrayList<Vertex3dNode> myLeftSortedNodes;

      HoleInfo (
         Vertex3dList hole, double leftDirDot,
         ArrayList<Vertex3dNode> leftSortedNodes) {

         myHole = hole;
         myLeftDirDot = leftDirDot;
         myLeftSortedNodes = leftSortedNodes;
      }

      boolean hasNext() {
         return !myLeftSortedNodes.isEmpty();
      }

      Vertex3dNode removeNext() {
         if (myLeftSortedNodes.isEmpty()) {
            return null;
         }
         else {
            return myLeftSortedNodes.remove(myLeftSortedNodes.size()-1);
         }
      }         
   }

   private class HoleComparator implements Comparator<HoleInfo> {
      public int compare (HoleInfo info0, HoleInfo info1) {
         double dot0 = info0.myLeftDirDot;
         double dot1 = info1.myLeftDirDot;
         if (dot0 < dot1) {
            return -1;
         }
         else if (dot0 == dot1) {
            return 0;
         }
         else {
            return 1;
         }
      }
   }

   private class NodeDistanceComparator implements Comparator<Vertex3dNode> {
      Vector3d myP0;

      NodeDistanceComparator (Vector3d p0) {
         myP0 = p0;
      }

      public int compare (Vertex3dNode node0, Vertex3dNode node1) {
         
         double dist0 = node0.vtx.pnt.distance (myP0);
         double dist1 = node1.vtx.pnt.distance (myP0);
         if (dist0 < dist1) {
            return -1;
         }
         else if (dist0 == dist1) {
            return 0;
         }
         else {
            return 1;
         }
      }
   }

   private class LeftDistanceComparator implements Comparator<Vertex3dNode> {
      Vector3d myDir;

      LeftDistanceComparator (Vector3d dir) {
         myDir = new Vector3d(dir);
      }

      public int compare (Vertex3dNode node0, Vertex3dNode node1) {
         Vector3d del01 = new Vector3d();
         del01.sub (node1.vtx.pnt, node0.vtx.pnt);
         double dot = del01.dot (myDir);
         if (dot < 0) {
            return -1;
         }
         else if (dot == 0) {
            return 0;
         }
         else {
            return 1;
         }
      }
   }

   public void mergeHoles (
      Vertex3dList poly, List<Vertex3dList> holes, double atol) {

      // Note: this method assumes that all the nodes in poly are instances of
      // PolyNode

      // Based on Held, 2001: "FIST: Fast Industrial-Strength Triangulation of
      // Polygons".

      Vertex3dNode firstNode = poly.getNode(0);
      Point3d pf = firstNode.vtx.pnt;

      Vector3d nrm = new Vector3d (myNrmX, myNrmY, myNrmZ);
      nrm.normalize();
      Vector3d dir = new Vector3d (1, 0, 0);
      dir.scaledAdd (-dir.dot(nrm), nrm);

      ArrayList<HoleInfo> holeInfos = new ArrayList<HoleInfo>();
      Vector3d del0p = new Vector3d();
      for (Vertex3dList hole : holes) {
         ArrayList<Vertex3dNode> leftSortedNodes =
            new ArrayList<Vertex3dNode>();
         for (Vertex3dNode node : hole) {
            leftSortedNodes.add (node);
         }
         Collections.sort (leftSortedNodes, new LeftDistanceComparator (dir));
         Vertex3dNode leftNode = leftSortedNodes.get(leftSortedNodes.size()-1);
         del0p.sub (leftNode.vtx.pnt, pf);
         holeInfos.add (
            new HoleInfo (hole, del0p.dot(dir), leftSortedNodes));
      }
      Collections.sort (holeInfos, new HoleComparator());

      ArrayList<PolyNode> leftPolyNodes = new ArrayList<PolyNode>();
      for (HoleInfo info : holeInfos) {
         boolean found = false;
         while (!found && info.hasNext()) {
            // usually we find a bridge to the hole on the first try
            leftPolyNodes.clear();
            Vertex3dNode holeNode = info.removeNext();
            Vector3d ph = holeNode.vtx.pnt;
            for (Vertex3dNode node : poly) {
               del0p.sub (node.vtx.pnt, ph);
               double dot = del0p.dot (dir);
               if (dot <= 0) {
                  leftPolyNodes.add ((PolyNode)node);
               }
            }
            Collections.sort (leftPolyNodes, new NodeDistanceComparator (ph));

            int k = 0;
            while (k<leftPolyNodes.size()) {
               PolyNode polyNode = leftPolyNodes.get(k);
               Point3d pp = polyNode.vtx.pnt;
               // now see if the rest of the poly intersects the bridge segment
               boolean intersects = false;
               for (Vertex3dNode node : poly) {
                  if (segmentsIntersect (
                         (PolyNode)node, (PolyNode)node.next,
                         polyNode, holeNode, atol)) {
                     intersects = true;
                  }
               }
               if (!intersects) {
                  found = true;
                  if (debug) {
                     System.out.println ("found "+polyNode.vtx.getIndex() + 
                                         " "+holeNode.vtx.getIndex());
                  }
                  PolyNode newPolyNode = new PolyNode (polyNode.vtx);
                  newPolyNode.opposite = polyNode;
                  polyNode.opposite = newPolyNode;
                  poly.addBefore (newPolyNode, polyNode);
                  PolyNode newHoleNode = new PolyNode (holeNode.vtx);
                  poly.addBefore (newHoleNode, polyNode);
                  Vertex3dNode node = holeNode;
                  do {
                     node = node.next;
                     PolyNode newNode = new PolyNode (node.vtx);
                     if (node == holeNode) {
                        newNode.opposite = newHoleNode;
                        newHoleNode.opposite = newNode;
                     }
                     poly.addBefore (newNode, polyNode);
                  }
                  while (node != holeNode);
                  break;
               }
               k++;
            }
         }
         if (!found) {
            System.out.println (
               "Polygon3dCalc.triangulate WARNING: hole "+
               holeInfos.indexOf(info)+ " could not be merged");
         }
      }
      if (debug) {
         System.out.println ("merged:");
         for (Vertex3dNode node : poly) {
            System.out.println (
               " "+node.vtx.getIndex() + "  " + node.vtx.pnt.toString("%16.12f"));
         }
         System.out.println ("");
      }
   }      

}
