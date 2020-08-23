/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.InternalErrorException;

/**
 * Half-edge for 3D dimensional polyhedral objects. A half-edge is a directed
 * edge between two counter-clockwise adjacent vertices of a face. Two adjacent
 * half-edges together define a single edge.
 */
public class HalfEdge extends Feature implements Boundable {

   // private static final double DOUBLE_PREC = 2.220446049250313e-16;

   /**
    * Flag indicating that a HalfEdge corresponds to the edge of
    * an underlying ``element''. Used in FEM applications.
    */
   public static final int ELEM_EDGE = 0x400;
   
   public Vertex3d head;
   public Vertex3d tail;
   public HalfEdge opposite;
   HalfEdge next;
   Face face;
   //Vector3d u;
   //double uLength;
   boolean uOppositeP; // u points from head to tail
   boolean myHardP = false; // edge should be treated as 'hard' for rendering

   // /*
   //  * These two flags are used to designate as primary one of every pair of
   //  * half-edges between the same two vertices. This is necessary to simplify
   //  * the collision code -- firstly to avoid doing redundant face-edge
   //  * intersection tests, and secondly for identity consistency of edges in
   //  * collision analysis of mesh features.
   //  */
   // boolean isPrimarySet = false;
   // boolean isPrimary;

   public boolean isHard() {
      return myHardP;
   }

   public void setHard (boolean hard) {
      myHardP = hard;
      head.hedgesSorted = false;
      tail.hedgesSorted = false;
   }

   public HalfEdge() {
      super (HALF_EDGE);
      //u = new Vector3d();
   }
   
   /**
    * Creates a new half-edge with a specified head vertex, tail vertex, and
    * face.
    * 
    * @param head
    * head vertex
    * @param tail
    * head vertex
    * @param face
    * associated face
    */
   public HalfEdge (Vertex3d head, Vertex3d tail, Face face) {
      super (HALF_EDGE);
      this.head = head;
      this.tail = tail;
      this.face = face;
      this.opposite = null;
      //createDirectionVector (head.pnt, tail.pnt);
      this.uOppositeP = false;
      this.next = null;
   }

   /**
    * Creates a new half-edge with a specified head vertex, opposite half-edge,
    * and face. The head vertex is assumed to correspond to the tail of the
    * opposite half-edge. Both half-edges are set to be each other's opposites.
    * 
    * @param head
    * head vertex
    * @param opp
    * opposite half-edge
    * @param face
    * associated face
    */
   public HalfEdge (Vertex3d head, HalfEdge opp, Face face) {
      super (HALF_EDGE);
      this.head = head;
      this.tail = opp.head;
      this.face = face;
      this.opposite = opp;
      opp.opposite = this;
      //this.u = opp.u;
      //this.uLength = -opp.uLength;
      this.uOppositeP = true;
      this.next = null;
   }

   /**
    * Sets the next half-edge adjacent to this one (in a counter-clockwise
    * sense).
    * 
    * @param heNext
    * next half-edge
    */
   public void setNext (HalfEdge heNext) {
      this.next = heNext;
   }

   /**
    * Creates a pair of adjacent half-edges that together define a line segment
    * connecting two vertices.
    * 
    * @param v1
    * first vertex on the line segment
    * @param v2
    * second vertex on the line segment
    * @return the half edge whose head is the first vertex
    */
   public static HalfEdge createLineSegment (Vertex3d v1, Vertex3d v2) {
      HalfEdge heHead = new HalfEdge (v1, v2, null);
      HalfEdge heTail = new HalfEdge (v2, heHead, null);
      heHead.next = heTail;
      heTail.next = heHead;
      return heHead;
   }

//   void createDirectionVector (Point3d head, Point3d tail) {
//      u = new Vector3d();
//      u.sub (head, tail);
//      uLength = u.norm();
//      u.scale (1 / uLength);
//   }

   // void setDirectionVector (Vector3d u, double l)
   // {
   // this.u = u;
   // this.uLength = l;
   // }

//   public Vector3d getU() {
//      return u;
//   }
   
   /**
    * Computes a unit vector for the edge associated with this half-edge,
    * and returns the edge's length. The direction runs from the tail to the 
    * head if the half-edge is primary (i.e., {@link #isPrimary()}
    * returns <code>true</code>), and from the head to the tail otherwise.
    * If the length of the edge is 0, all elements of the vector are
    * set to 0.
    * 
    * @param u
    * returns the edge unit vector
    */
   public double computeEdgeUnitVec (Vector3d u) {
      if (uOppositeP) {
         u.sub (tail.pnt, head.pnt);
      }
      else {
         u.sub (head.pnt, tail.pnt);
      }
      double len = u.norm();
      u.scale (1/len);
      return len;
   }

   /**
    * Computes a unit vector in the direction of this edge, and returns
    * the edge's length. The direction is defined as running from the tail to
    * the head. If the length of the edge is 0, all elements of the vector are
    * set to 0.
    * 
    * @param u
    * returns the normalized direction vector
    */
   public void computeUnitVec (Vector3d u) {
      u.x = head.pnt.x - tail.pnt.x;
      u.y = head.pnt.y - tail.pnt.y;
      u.z = head.pnt.z - tail.pnt.z;
      double mag = Math.sqrt (u.x * u.x + u.y * u.y + u.z * u.z);
      if (mag != 0) {
         u.x /= mag;
         u.y /= mag;
         u.z /= mag;
      }
      else {
         u.setZero();
      }
   }

   /**
    * Returns the face associated with this half-edge.
    * 
    * @return associated face
    */
   public Face getFace() {
      return face;
   }

   /**
    * Returns an index for this half-edge, computed as
    * <pre>
    * 3 * faceIdx + edgeNum
    * </pre>
    * where {@code faceIdx} is the index of its face, and {@code edgeNum} is
    * the edge number with respect to the face (with {@code
    * face.firstHalfEdge()} corresponding to 0).
    */
   public int getIndex() {
      int edgeNum = 0;
      HalfEdge he = face.he0;
      do {
         if (he == this) {
            return 3*face.getIndex() + edgeNum;
         }
         edgeNum++;
         he = he.next;
      }
      while (he != face.he0);
      throw new InternalErrorException (
         "HalfEdge not found in its face");
   }

   /**
    * Returns the face opposite to this half-edge, if any
    * 
    * @return opposite face, or <code>null</code>.
    */
   public Face getOppositeFace() {
      if (opposite != null) {
         return opposite.face;
      }
      else {
         return null;
      }
   }

   /**
    * Returns the last hard edge (if any) incident on this half-edge's head
    * vertex, relative to this half-edge in a counter-clockwise direction about
    * the vertex. If there is no hard-edge incident on the vertex, null is
    * returned.
    */
   public HalfEdge lastHardEdge () {
      if (myHardP) {
         return this;
      }
      HalfEdge lastHard = null;
      HalfEdge he = next.opposite;
      int cnt = 0;
      while (he != null && he != this) {
         if (he.myHardP) {
            lastHard = he;
         }
         he = he.next.opposite;
         cnt++;
      }
      return lastHard;      
   }

   /**
    * Computes a normal for the head vertex of this half-edge, by averaging
    * all the surrounding face normals starting with the face corresponding
    * to this half edge, and ending when either a hard edge is encountered,
    * or when all faces have been traversed. Face traversal proceeds in
    * a clockwise direction about the vertex.
    */
   public void computeVertexNormal (Vector3d nrm, boolean useRenderNormals) {

      nrm.setZero();
      if (useRenderNormals) {
         nrm.add (face.getRenderNormal());
         HalfEdge he = next.opposite;
         while (he != null && he != this && !he.myHardP) {
            nrm.add (he.face.getRenderNormal());
            he = he.next.opposite;
         }
      }
      else {
         // average face normals:
         // nrm.add (face.getNormal());
         // HalfEdge he = next.opposite;
         // while (he != null && he != this && !he.myHardP) {
         //    nrm.add (he.face.getNormal());
         //    he = he.next.opposite;
         // }

         Vector3d xprod = new Vector3d();         

         xprod.angleWeightedCross (tail.pnt, head.pnt, next.head.pnt);

         // average edge cross products:
         nrm.angleWeightedCrossAdd (tail.pnt, head.pnt, next.head.pnt);
         HalfEdge he = next.opposite;
         while (he != null && he != this && !he.myHardP) {
            xprod.angleWeightedCross (he.tail.pnt, he.head.pnt, he.next.head.pnt);
            nrm.angleWeightedCrossAdd (
               he.tail.pnt, he.head.pnt, he.next.head.pnt);
            he = he.next.opposite;
         }
      }
      nrm.normalize();
   }

   /**
    * Computes the dot product between a vector v1 and a non-normalized vector
    * in the direction of this half-edge.
    * 
    * @param v1
    * vector to dot with direction vector
    * @return dot product
    */
   public double dotDirection (Vector3d v1) {
      double ux = head.pnt.x - tail.pnt.x;
      double uy = head.pnt.y - tail.pnt.y;
      double uz = head.pnt.z - tail.pnt.z;
      return ux * v1.x + uy * v1.y + uz * v1.z;
   }

//   /**
//    * Computes the dot product between a vector v1 and a unit vector in the
//    * direction of this half-edge.
//    * 
//    * @param v1
//    * vector to dot with unit vector
//    * @return dot product
//    */
//   public double dot (Vector3d v1) {
//      if (!uOppositeP) {
//         return u.dot (v1);
//      }
//      else {
//         return -u.dot (v1);
//      }
//   }

//   /**
//    * Computes the dot product between a specified vector and a unit vector in
//    * the direction of this half-edge.
//    * 
//    * @param x
//    * first coordinate of the specified vector
//    * @param y
//    * second coordinate of the specified vector
//    * @param z
//    * third coordinate of the specified vector
//    * @return dot product
//    */
//   public double dot (double x, double y, double z) {
//      double prod = u.x * x + u.y * y + u.z * z;
//      if (!uOppositeP) {
//         return prod;
//      }
//      else {
//         return -prod;
//      }
//   }

//   /**
//    * Computes the cross product the unit vector in the direction of
//    * this half-edge with the unit vector in the direction of another
//    * half-egde, and adds the result to <code>res</code>.
//    *
//    * @param res
//    * Result is added to this vector
//    * @param he
//    * Half-edge to compute cross product with
//    */
//   public void crossAdd (Vector3d res, HalfEdge he) {
//      Vector3d u2 = he.u;
//      if (uOppositeP != he.uOppositeP) {
//         // negate the result
//         res.x += u2.y*u.z - u2.z*u.y;
//         res.y += u2.z*u.x - u2.x*u.z;
//         res.z += u2.x*u.y - u2.y*u.x;
//      }
//      else {
//         res.x += u.y*u2.z - u.z*u2.y;
//         res.y += u.z*u2.x - u.x*u2.z;
//         res.z += u.x*u2.y - u.y*u2.x;
//      }
//   }

//   /**
//    * Computes the cross product the unit vector in the direction of
//    * this half-edge with the unit vector in the direction of another
//    * half-egde, and places the result in <code>res</code>.
//    *
//    * @param res
//    * Result is placed in this vector
//    * @param he
//    * Half-edge to compute cross product with
//    */
//   public void cross (Vector3d res, HalfEdge he) {
//      Vector3d u2 = he.u;
//      if (uOppositeP != he.uOppositeP) {
//         // negate the result
//         res.x = u2.y*u.z - u2.z*u.y;
//         res.y = u2.z*u.x - u2.x*u.z;
//         res.z = u2.x*u.y - u2.y*u.x;
//      }
//      else {
//         res.x = u.y*u2.z - u.z*u2.y;
//         res.y = u.z*u2.x - u.x*u2.z;
//         res.z = u.x*u2.y - u.y*u2.x;
//      }
//   }

   public void addAngleWeightedNormal (Vector3d res, HalfEdge he) {

      Point3d p0 = tail.pnt;
      Point3d p1 = head.pnt;
      Point3d p2 = he.head.pnt;

      double u1x = p1.x-p0.x;
      double u1y = p1.y-p0.y;
      double u1z = p1.z-p0.z;
      double mag1 = Math.sqrt (u1x*u1x + u1y*u1y + u1z*u1z);
      u1x /= mag1;
      u1y /= mag1;
      u1z /= mag1;

      double u2x = p2.x-p1.x;
      double u2y = p2.y-p1.y;
      double u2z = p2.z-p1.z;
      double mag2 = Math.sqrt (u2x*u2x + u2y*u2y + u2z*u2z);
      u2x /= mag2;
      u2y /= mag2;
      u2z /= mag2;

      double x = u1y*u2z - u1z*u2y;
      double y = u1z*u2x - u1x*u2z;
      double z = u1x*u2y - u1y*u2x;

      double sin = Math.sqrt (x*x + y*y + z*z);
      double cos = -(u1x*u2x + u1y*u2y + u1z*u2z);

      double w = Math.atan2 (sin, cos)/sin;
      res.x += w*x;
      res.y += w*y;
      res.z += w*z;
   }

   /**
    * Computes the cross product v2 x v1, where this half edge and another vector directed
    * from p2 to the head of this half-edge, and then sets the length
    * of the resulting vector to equal the angle * 
   public void angleWeightedNormal (Vector3d res, Point3d p2) {

      Point3d p0 = tail.pnt;
      Point3d p1 = head.pnt;
      Point3d p2 = he.head.pnt;

      double u1x = p1.x-p0.x;
      double u1y = p1.y-p0.y;
      double u1z = p1.z-p0.z;
      double mag1 = Math.sqrt (u1x*u1x + u1y*u1y + u1z*u1z);
      u1x /= mag1;
      u1y /= mag1;
      u1z /= mag1;

      double u2x = p2.x-p1.x;
      double u2y = p2.y-p1.y;
      double u2z = p2.z-p1.z;
      double mag2 = Math.sqrt (u2x*u2x + u2y*u2y + u2z*u2z);
      u2x /= mag2;
      u2y /= mag2;
      u2z /= mag2;

      double x = u1y*u2z - u1z*u2y;
      double y = u1z*u2x - u1x*u2z;
      double z = u1x*u2y - u1y*u2x;

      double sin = Math.sqrt (x*x + y*y + z*z);
      double cos = -(u1x*u2x + u1y*u2y + u1z*u2z);

      double w = Math.atan2 (sin, cos)/sin;
      res.x = w*x;
      res.y = w*y;
      res.z = w*z;
   }

//   /**
//    * Method doesn't seem to work - there seems to be some sort of
//    * inconsistency with the uOppositeP booleans.
//    */
//   public void addAngleWeightedNormalOld (Vector3d res, HalfEdge he) {
//
//      Vector3d u2 = he.u;
//      double x = u.y*u2.z - u.z*u2.y;
//      double y = u.z*u2.x - u.x*u2.z;
//      double z = u.x*u2.y - u.y*u2.x;
//
//      double sin = Math.sqrt (x*x + y*y + z*z);
//      double cos = u.x*u2.x + u.y*u2.y + u.z*u2.z;
//
//      if (uOppositeP != he.uOppositeP) {
//         double w = Math.atan2 (sin, cos)/sin;
//         res.x -= w*x;
//         res.y -= w*y;
//         res.z -= w*z;
//      }
//      else {
//         double w = Math.atan2 (sin, -cos)/sin;
//         res.x += w*x;
//         res.y += w*y;
//         res.z += w*z;
//      }
//   }

//   /**
//    * Method doesn't seem to work - there seems to be some sort of
//    * inconsistency with the uOppositeP booleans.
//    */
//   public void angleWeightedNormalOld (Vector3d res, HalfEdge he) {
//
//      Vector3d u2 = he.u;
//      double x = u.y*u2.z - u.z*u2.y;
//      double y = u.z*u2.x - u.x*u2.z;
//      double z = u.x*u2.y - u.y*u2.x;
//
//      double sin = Math.sqrt (x*x + y*y + z*z);
//      double dot = u.x*u2.x + u.y*u2.y + u.z*u2.z;
//
//      if (uOppositeP != he.uOppositeP) {
//         double w = Math.atan2 (sin, dot)/sin;
//         res.x = -w*x;
//         res.y = -w*y;
//         res.z = -w*z;
//      }
//      else {
//         double w = Math.atan2 (sin, -dot)/sin;
//         res.x = w*x;
//         res.y = w*y;
//         res.z = w*z;
//      }
//   }

   /**
    * Returns the length of this half-edge.
    * 
    * @return half-edge length
    */
   public double length() {
      return head.pnt.distance (tail.pnt);
      // return uLength >= 0 ? uLength : -uLength;
   }

   /**
    * Returns the length squared of this half-edge.
    * 
    * @return length squared
    */
   public double lengthSquared() {
      double ux = head.pnt.x - tail.pnt.x;
      double uy = head.pnt.y - tail.pnt.y;
      double uz = head.pnt.z - tail.pnt.z;
      return ux * ux + uy * uy + uz * uz;
   }

//   public double uSign() {
//      return uLength >= 0 ? 1 : -1;
//   }

//   /**
//    * Computes the dot product of (v1 X u) and v2, where u is a unit vector in
//    * the direction of this half-edge.
//    * 
//    * @param v1
//    * first vector
//    * @param v2
//    * second vector
//    * @return dot product
//    */
//   public double sideProduct (Vector3d v1, Vector3d v2) {
//      double x = v1.y * u.z - v1.z * u.y;
//      double y = v1.z * u.x - v1.x * u.z;
//      double z = v1.x * u.y - v1.y * u.x;
//
//      double dot = v2.x * x + v2.y * y + v2.z * z;
//
//      if (uOppositeP) {
//         return -dot;
//      }
//      else {
//         return dot;
//      }
//   }

   /**
    * Computes the dot product of (v1 X dir) and v2, where dir is the vector
    * from the head to the tail of this half-edge.
    * 
    * @param v1
    * first vector
    * @param v2
    * second vector
    * @return dot product
    */
   public double sideProductDirection (Vector3d v1, Vector3d v2) {
      double ux = head.pnt.x - tail.pnt.x;
      double uy = head.pnt.y - tail.pnt.y;
      double uz = head.pnt.z - tail.pnt.z;

      double x = v1.y * uz - v1.z * uy;
      double y = v1.z * ux - v1.x * uz;
      double z = v1.x * uy - v1.y * ux;

      return v2.x * x + v2.y * y + v2.z * z;
   }

//   private double sidePointProduct (Point3d p, Vector3d n) {
//      double vx = p.x - head.pnt.x;
//      double vy = p.y - head.pnt.y;
//      double vz = p.z - head.pnt.z;
//
//      double x = vy * u.z - vz * u.y;
//      double y = vz * u.x - vx * u.z;
//      double z = vx * u.y - vy * u.x;
//
//      double dot = n.x * x + n.y * y + n.z * z;
//      if (uOppositeP) {
//         return -dot;
//      }
//      else {
//         return dot;
//      }
//   }

//   /**
//    * Returns true if the point p is inside the plane defined by u and the
//    * normal vector n.
//    * 
//    * @param p
//    * point to test
//    * @param n
//    * normal vector
//    */
//   public boolean isInside (Point3d p, Vector3d n) {
//      return sidePointProduct (p, n) < 0;
//   }
//
//   /**
//    * Returns true if the point p is outside the plane defined by u and the
//    * normal vector n.
//    */
//   public boolean isOutside (Point3d p, Vector3d n) {
//      return sidePointProduct (p, n) > 0;
//   }

//   public void extrapolate (Vector3d vr, double s, Vector3d v1) {
//      if (!uOppositeP) {
//         vr.scaledAdd (s, u, v1);
//      }
//      else {
//         vr.scaledAdd (-s, u, v1);
//      }
//   }
//
//   public void extrapolate (Vector3d vr, double s) {
//      if (!uOppositeP) {
//         vr.scaledAdd (s, u, head.pnt);
//      }
//      else {
//         vr.scaledAdd (-s, u, head.pnt);
//      }
//   }

   public final Vertex3d getHead() {
      return head;
   }

   public final HalfEdge getNext() {
      return next;
   }

   public final Vertex3d getTail() {
      return tail;
      // HalfEdge hePrev = null;
      // HalfEdge he = this;
      // do
      // { hePrev = he;
      // if ((he = he.next) == null)
      // { return null;
      // }
      // }
      // while (he != this);
      // return hePrev.head;
   }

   // /**
   // * Computes the nearest points between the two lines which
   // * lie along these half edges, and returns the result in
   // * drec. The method returns true if the nearest points
   // * are both contained within their respective segments.
   // *
   // * @param drec returns closest point information
   // * @param he segment describing second line
   // * @param forceOntoSegment forces the results to
   // * lie on their respective segments
   // */
   // public boolean lineDistance (
   // DistanceRecord drec, HalfEdge he, boolean forceOntoSegment)
   // {
   // Vector3d dhh = drec.pnt0; // use drec.pnt0 as scatch space
   //
   // dhh.sub (he.head.pnt, head.pnt);
   //
   // // System.out.println ("");
   //
   // double dhhU0 = dot(dhh);
   // double dhhU1 = he.dot(dhh);
   // double u0U1 = uSign()*he.dot(u);
   // double l0 = length();
   // double l1 = he.length();
   // double dhtU0 = dhhU0 - l1*u0U1;
   // // double dthU1 = dhhU1 + l0*u0U1;
   //
   // // System.out.println ("dhhU0=" + dhhU0);
   // // System.out.println ("dhhU1=" + dhhU1);
   // // System.out.println ("u1=" + he.u);
   // // System.out.println ("dhh=" + dhh);
   // // System.out.println ("dhtU0=" + dhtU0);
   // // System.out.println ("dthU1=" + dthU1);
   // // System.out.println ("u0U1=" + u0U1);
   // // System.out.println ("l0=" + l0);
   // // System.out.println ("l1=" + l1);
   //
   // drec.setFeatures (this, he);
   // double sinSqr = 1-u0U1*u0U1;
   // double lam0, lam1;
   // if (sinSqr == 0)
   // { // then edges are parallel; choose the mid overlap point
   // double max = 0;
   // double min = -l0;
   // if (u0U1 > 0)
   // { // edges face same direction
   // if (dhhU0 < 0)
   // { max = dhhU0;
   // }
   // if (dhtU0 > min)
   // { min = dhtU0;
   // }
   // lam0 = (max+min)/2;
   // lam1 = lam0-dhhU0;
   // }
   // else
   // { // edges face opposite directions
   // if (dhtU0 < 0)
   // { max = dhtU0;
   // }
   // if (dhhU0 > min)
   // { min = dhhU0;
   // }
   // lam0 = (max+min)/2;
   // lam1 = dhhU0-lam0;
   // }
   // }
   // else
   // { lam0 = ( dhhU0 - dhhU1*u0U1)/sinSqr;
   // lam1 = (-dhhU1 + dhhU0*u0U1)/sinSqr;
   // }
   // boolean onSegments = false;
   // if (forceOntoSegment)
   // { if (lam0 > 0)
   // { lam0 = 0;
   // }
   // else if (lam0 < -l0)
   // { lam0 = -l0;
   // }
   // if (lam1 > 0)
   // { lam1 = 0;
   // }
   // else if (lam1 < -l1)
   // { lam1 = -l1;
   // }
   // onSegments = true;
   // }
   // else
   // { onSegments = (lam0 <= 0 && lam0 >= -l0 &&
   // lam1 <= 0 && lam1 >= -l1);
   // }
   // extrapolate (drec.pnt0, lam0);
   // he.extrapolate (drec.pnt1, lam1);
   // drec.computeDistanceAndNormal();
   // return onSegments;
   // }

   // /**
   // * Computes the distance and closest points between this half edge
   // * and the vertex vtx, and returns this information in drec.
   // *
   // * @param drec returns closest point information
   // * @param vtx vertex to compute distance to
   // */
   // public void distance (DistanceRecord drec, Vertex3d vtx)
   // {
   // Vector3d dv = drec.pnt0; // use drec.pnt0 as scatch space
   //         
   // dv.sub (vtx.pnt, head.pnt);
   // double udot = dot(dv);
   // if (udot >= 0)
   // { // nearest point is the head
   // drec.setFeatures (head, vtx);
   // drec.pnt0.set (head.pnt);
   // }
   // else if (udot <= -length())
   // { // nearest point is the tail
   // drec.setFeatures (getTail(), vtx);
   // extrapolate (drec.pnt0, -length());
   // }
   // else
   // { // nearest point is between the head and the tail
   // drec.setFeatures (this, vtx);
   // extrapolate (drec.pnt0, udot);
   // }
   // drec.pnt1.set (vtx.pnt);
   // drec.computeDistanceAndNormal();
   // }

   // /**
   // * Computes the distance and closest points between this half edge
   // * and the half edge he, and returns this information in drec.
   // *
   // * @param drec returns closest point information
   // * @param he half edge to compute distance to
   // */
   // public void distance (DistanceRecord drec, HalfEdge he)
   // {
   // Vector3d dhh = drec.pnt0; // use drec.pnt0 as scatch space
   //
   // dhh.sub (he.head.pnt, head.pnt);
   //
   // double dhhU0 = dot(dhh);
   // double dhhU1 = he.dot(dhh);
   // double u0U1 = uSign()*he.dot(u);
   // double l0 = length();
   // double l1 = he.length();
   // double dhtU0 = dhhU0 - l1*u0U1;
   // double dthU1 = dhhU1 + l0*u0U1;
   //
   // // System.out.println ("");
   //
   // // System.out.println ("dhhU0=" + dhhU0);
   // // System.out.println ("dhhU1=" + dhhU1);
   // // System.out.println ("dhtU0=" + dhtU0);
   // // System.out.println ("dthU1=" + dthU1);
   // // System.out.println ("u0U1=" + u0U1);
   //
   // if (dhhU0 >= 0 && dhhU1 <= 0)
   // { drec.setFeatures (head, he.head);
   // drec.pnt0.set (head.pnt);
   // drec.pnt1.set (he.head.pnt);
   // }
   // else if (dhtU0 >= 0 && dhhU1 >= l1)
   // { drec.setFeatures (head, he.getTail());
   // drec.pnt0.set (head.pnt);
   // he.extrapolate (drec.pnt1, -l1);
   // }
   // else if (dhhU0 <= -l0 && dthU1 <= 0)
   // { drec.setFeatures (getTail(), he.head);
   // extrapolate (drec.pnt0, -l0);
   // drec.pnt1.set (he.head.pnt);
   // }
   // else if (dhtU0 <= -l0 && dthU1 >= l1)
   // { drec.setFeatures (getTail(), he.getTail());
   // extrapolate (drec.pnt0, -l0);
   // he.extrapolate (drec.pnt1, -l1);
   // }
   // else if (dhhU1 >= 0 && dhhU1 <= l1 &&
   // dhhU0 - u0U1*dhhU1 >= 0)
   // { drec.setFeatures (head, he);
   // drec.pnt0.set (head.pnt);
   // he.extrapolate (drec.pnt1, -dhhU1);
   // }
   // else if (dthU1 >= 0 && dthU1 <= l1 &&
   // dhhU0 + l0 - u0U1*dthU1 <= 0)
   // { drec.setFeatures (getTail(), he);
   // extrapolate (drec.pnt0, -l0);
   // he.extrapolate (drec.pnt1, -dthU1);
   // }
   // else if (dhhU0 >= -l0 && dhhU0 <= 0 &&
   // u0U1*dhhU0 - dhhU1 >= 0)
   // { drec.setFeatures (this, he.head);
   // extrapolate (drec.pnt0, dhhU0);
   // drec.pnt1.set (he.head.pnt);
   // }
   // else if (dhtU0 >= -l0 && dhtU0 <= 0 &&
   // u0U1*dhtU0 - dhhU1 + l1 <= 0)
   // { drec.setFeatures (this, he.getTail());
   // extrapolate (drec.pnt0, dhtU0);
   // he.extrapolate (drec.pnt1, -l1);
   // }
   // else
   // { lineDistance (drec, he, /*forceOntoSegment=*/true);
   // }
   // if (drec.feature0.type != Feature.HALF_EDGE ||
   // drec.feature1.type != Feature.HALF_EDGE)
   // { drec.computeDistanceAndNormal();
   // }
   // }

//   /**
//    * Checks that a specified point is within the Voronoi region associated with
//    * this HalfEdge.
//    */
//   public boolean voronoiCheck (Point3d pnt) {
//      if (opposite != null && opposite.face != null) {
//         return !opposite.isInside (pnt, opposite.face.getNormal());
//      }
//      return true;
//   }

//   /**
//    * Updates u and ulength values if the vertices have changed.
//    */
//   public void updateU() {
//      if (!uOppositeP) {
//         u.sub (head.pnt, tail.pnt);
//         uLength = u.norm();
//         u.scale (1 / uLength);
//      }
//      else {
//         u.sub (tail.pnt, head.pnt);
//         uLength = -u.norm();
//         u.scale (-1 / uLength);
//      }
//      if (opposite != null) {
//         opposite.uLength = -uLength;
//      }
//   }

   public String pointList() {
      return head.getWorldPoint() + " " + tail.getWorldPoint();
   }

   /*
    * Each edge is represented by two half-edges. Define one of them as primary.
    * In all collision code, we ignore non-primary half-edges. Then we need only
    * consider one unique half-edge associated with a pair of vertices.
    */
   public boolean isPrimary() {
      return !uOppositeP;
   }

   public HalfEdge getPrimary() {
      return uOppositeP ? opposite : this;
   }

   public void setPrimary (boolean primary) {
      if (primary != isPrimary()) {
         //u.negate();
         uOppositeP = !uOppositeP;
      }
   }            

//   /*
//    * Test for intersection using adaptive exact arithmetic and SOS tiebreaking.
//    */
//   public boolean robustIntersectionWithFace (
//      Face aFace, MeshIntersectionPoint mip) {
//      Vertex3d v = aFace.he0.tail;
//      if (v == head)
//         return false;
//      if (v == tail)
//         return false;
//      v = aFace.he0.head;
//      if (v == head)
//         return false;
//      if (v == tail)
//         return false;
//      v = aFace.he0.next.head;
//      if (v == head)
//         return false;
//      if (v == tail)
//         return false;
//      // aFace.updateWorldCoordinates();
//
//      if (!RobustPreds.intersectEdgeFace (this, aFace, mip))
//         return false;
//      mip.edge = this;
//      mip.face = aFace;
//      return true;
//   }

//   /*
//    * Test using an approximate test that does not use exact arithmetic. If it
//    * returns true, the result should be checked with exact arithmetic. The
//    * approximate test is assumed to be significantly faster, but this has not
//    * been verified by measurement. Should try getting rid of the
//    * aFace.isPointInside test and going straight to exact result.
//    */
//   public boolean intersectionWithFace (Face aFace) {
//      Vertex3d v = aFace.he0.tail;
//      if (v == head)
//         return false;
//      if (v == tail)
//         return false;
//      v = aFace.he0.head;
//      if (v == head)
//         return false;
//      if (v == tail)
//         return false;
//      v = aFace.he0.next.head;
//      if (v == head)
//         return false;
//      if (v == tail)
//         return false;
//
//      // aFace.updateWorldCoordinates();
//      Vector3d n = aFace.getWorldNormal();
//      Point3d w = aFace.he0.head.getWorldPoint();
//      Point3d hp = head.getWorldPoint();
//      double h = (w.x - hp.x) * n.x + (w.y - hp.y) * n.y + (w.z - hp.z) * n.z;
//      Point3d tp = tail.getWorldPoint();
//      double t = (w.x - tp.x) * n.x + (w.y - tp.y) * n.y + (w.z - tp.z) * n.z;
//      if ((h < 0 && t < 0) || (h > 0 && t > 0)) {
//         return false;
//      }
//      t = Math.abs (t);
//      double lambda = t / (t + Math.abs (h));
//      double x = (hp.x - tp.x) * lambda + tp.x;
//      double y = (hp.y - tp.y) * lambda + tp.y;
//      double z = (hp.z - tp.z) * lambda + tp.z;
//      return aFace.isPointInside (x, y, z);
//   }

   public void updateBounds (Vector3d min, Vector3d max) {
      tail.pnt.updateBounds (min, max);
      head.pnt.updateBounds (min, max);
   }

   public void computeCentroid (Vector3d p) {
      p.set (tail.pnt);
      p.add (head.pnt);
      p.scale (0.5);
   }

  /**
    * Computes covariance of this half edge and returns its length.
    * <p>
    * The formula was determined by substituting the
    * the parametric form for x
    * <pre>
    * x = (1-s) p0 + s p1
    * </pre>
    * into the general formula for C
    * <pre>
    * C = \int_V \rho x x^T dV
    * </pre>
    * and evaluating the integral over the s interval [0,1].
    * 
    * @param C 
    * returns the covariance
    * @return length of the half edge
    */
   public double computeCovariance (Matrix3d C) {
      return CovarianceUtils.computeLineSegmentCovariance (C, tail.pnt, head.pnt);
   }
   
   // implementation of IndexedPointSet

   public int numPoints() {
      return 2;
   }

   public Point3d getPoint (int idx) {
      switch (idx) {
         case 0: return tail.pnt;
         case 1: return head.pnt;
         default: {
            throw new ArrayIndexOutOfBoundsException ("idx=" + idx);
         }
      }
   }
   
   /**
    * Returns a string identifying this HalfEdge using the indices of
    * its tail and head vertices.
    * 
    * @return vertex-based identifying string
    */
   public String vertexStr() {
      return "[ " + tail.getIndex() + " " + head.getIndex() + " ]";
   }
   
   /**
    * Returns a string identifying this HalfEdge using the indices of
    * its primary and opposite faces.
    * 
    * @return face-based identifying string
    */
   public String faceStr() {
      String str = face.getIndex()+"-";
      if (opposite != null) {
         str += opposite.face.getIndex();
      }
      else {
         str += "NULL";
      }
      return str;
   }

   /**
    * Returns <code>true</code> if this HalfEdge and its opposite connects
    * faces <code>f0</code> and <code>f1</code>.
    */
   public boolean connectsFaces (Face f0, Face f1) {
      if (opposite == null) {
         return false;
      }
      return (face == f0 && opposite.face == f1 ||
              face == f1 && opposite.face == f0);
   }
   
   /**
    * Computes the projection parameter of a point <code>px</code>
    * with respect to a line defined by points <code>p0</code> and
    * <code>p1</code>. This is the value <i>s</i> such that
    * <pre>
    * pp = (1-s) p0 + s p1
    * </pre>
    * gives the projection of <code>px-p0</code> onto the line. If
    * <code>p0</code> and <code>p1</code> are identical, the
    * method returns positive infinity.
    *
    * @param p0 first point defining the line
    * @param p1 second point defining the libe
    * @param px point for which the project parameter should be computed
    * @return parameter s which projects px onto the line
    */
   public static double projectionParameter (
      Point3d p0, Point3d p1, Point3d px) {
      
      Vector3d del10 = new Vector3d();      
      Vector3d delx0 = new Vector3d();      

      del10.sub (p1, p0);
      delx0.sub (px, p0);
      double len10Sqr = del10.normSquared();
      if (len10Sqr == 0) {
         return Double.POSITIVE_INFINITY;
      }
      else {
         return del10.dot(delx0)/len10Sqr;
      }      
   }
   
   public double getProjectionParameter(Point3d pnt) {
      Point3d p0 = tail.getWorldPoint();
      Point3d p1 = head.getWorldPoint();
      return projectionParameter(p0, p1, pnt);
   }
   //   
   //   @Override
   //   public void nearestPoint(Point3d nearest, Point3d pnt) {
   //      Point3d p0 = tail.getWorldPoint();
   //      Point3d p1 = head.getWorldPoint();
   //      
   //      double s = projectionParameter(p0, p1, pnt);
   //      if (s >= 1.0) {
   //         nearest.set(p0);
   //      }
   //      else if (s <= 0) {
   //         nearest.set(p1);
   //      }
   //      else {
   //         nearest.combine (1-s, p0, s, p1);
   //      }
   //   }

   @Override
   public void nearestPoint(Point3d nearest, Point3d pnt) {
      Point3d p0 = tail.getWorldPoint();
      Point3d p1 = head.getWorldPoint();
     
      double ux = p1.x - p0.x;
      double uy = p1.y - p0.y;
      double uz = p1.z - p0.z;
      
      double dx, dy, dz;

      dx = pnt.x - p1.x;
      dy = pnt.y - p1.y;
      dz = pnt.z - p1.z;
      double dot = dx*ux + dy*uy + dz*uz;
      if (dot >= 0) {
         nearest.set(p1);
      }
      dx = pnt.x - p0.x;
      dy = pnt.y - p0.y;
      dz = pnt.z - p0.z;
      dot = dx*ux + dy*uy + dz*uz;
      if (dot <= 0) {
         nearest.set(p0);
      }
      else {
         double umagSqr = ux*ux + uy*uy + uz*uz;
         double s = dot/umagSqr;
         pnt.interpolate(p0, s, p1);
      }
   }

}
