package maspack.collision;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;

/**
 * Given one more 3D planar polygons in a common plane, this class calculates
 * and stores their nearest feature to a point. This will be either a vertex or
 * an edge. 
 *
 * <p>The method is used as follows:
 *
 *<pre>
 *{@code
 * Point3d px;   // point to find nearest feature for
 * Vector3d nrm; // normal of the plane containing the polygon
 * double dtol;  // distance tolerance. polygon vertices whose distance to
 *               // a previous vertex is <= dotl will be ignored
 *
 * // to calculate nearest feature of a single polygon               
 * NearestPolygon3dFeature nfeat = new NearestPolygon3dFeature();
 * nfeat.init (px, nrm, dtol);
 * for (each point p on the polygon) {
 *    nfeat.advance (p);
 * }
 * nfeat.close(); // call only if the polygon is closed
 *
 * // to calculate nearest feature for multiple polygons
 * NearestPolygon3dFeature nfeat = new NearestPolygon3dFeature();
 * nfeat.init (px, nrm, dtol);
 * for (each polygon) {
 *    nfeat.restart(); // don't need to call if previous polygon was closed
 *    for (each point p on the polygon) {
 *       nfeat.advance (p);
 *    }
 *    nfeat.close(); // call only if the polygon is closed
 * }
 *}
 *</pre>
 *
 * Once the calculation is done, information about the feature can be queried
 * with the following methods:
 *
 *<pre>
 *{@code
 * int numVertices();              // number of vertices defining the feature
 * Point3d getVertext(int k);      // the k-th feature vertex
 * Point3d getNearestPoint();      // nearest point on the polygon(s)
 * double getDistance();           // distance to the feature
 * boolean isOutside(boolean clockwise); // is the point outside the polygon
 *}
 *</pre>
 */
public class NearestPolygon3dFeature {

   /**
    * Decsribes the dominant axis-aligned plane uses for computations.
    */
   private enum Plane {
      YZ, ZX, XY };

   Point3d myPX;    // point to compute nearest feature for
   Plane myPlane;   // dominant axis-aligned plane uses for computations
   double myNrmMax; // maximum component of plane normal
   double myDtol;   // distance tolerance

   double myEdgeDist = Double.POSITIVE_INFINITY; // distance to nearest edge
   double myVtxDist = Double.POSITIVE_INFINITY; // distance to nearest solo vertex

   Point3d myPA;    // first feature vertex
   Point3d myPB;    // second feature vertex
   Point3d myPC;    // third feature vertex

   Point3d myP0;    // current polygon vertex
   Point3d myPL;    // last polygon vertex

   Point3d myStart0;// first vertex for the current polygon
   Point3d myStart1;// second vertex for the current polygon

   boolean myLastMinWasP0 = false; // current nearest feature found at p0
   boolean myLastMinWasStart = false; // current nearest feature found at start

   // for edge features, 1 indicates point is outside if polygon is clockwise 
   int myOutsideEdgeIfClockwise = -1;

   /**
    * Creates a new NearestPolygon3dFeature.
    */
   public NearestPolygon3dFeature() {
   }

   /**
    * Initializes this NearestPolygon3dFeature to input one or more polygons
    * 
    * @param px point to compute nearest feature for
    * @param nrm normal of plane containing the polygons
    * @param dtol distance tolerance. Polygonal vertices whose distance
    * to the previous vertex is {@code <=} dtol will be ignored.
    */
   public void init (Point3d px, Vector3d nrm, double dtol) {
      switch (nrm.maxAbsIndex()) {
         case 0: {
            myPlane = Plane.YZ;
            myNrmMax = nrm.x;
            break;
         }
         case 1: {
            myPlane = Plane.ZX;
            myNrmMax = nrm.y;
            break;
         }
         case 2: {
            myPlane = Plane.XY;
            myNrmMax = nrm.z;
            break;
         }
      }      
      myDtol = dtol;
      myPX = new Point3d(px);
      myPA = null;
      myPB = null;
      myPC = null;
      myEdgeDist = Double.POSITIVE_INFINITY;
      myVtxDist = Double.POSITIVE_INFINITY;
   }

   /**
    * Returns < 0 for a right turn, > 0 for a left turn, and 0 for no turn.
    */
   double turn (Vector3d a, Vector3d b) {
      double turn;
      switch (myPlane) {
         case YZ: {
            turn = (a.y*b.z - a.z*b.y)/myNrmMax;
            break;
         }
         case ZX: {
            turn = (a.z*b.x - a.x*b.z)/myNrmMax;
            break;
         }
         case XY: {
            turn = (a.x*b.y - a.y*b.x)/myNrmMax;
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented plane type "+myPlane);
         }
      }
      return turn;
   }

   /**
    * Returns the number of polygon vertices associated with this feature:
    *
    * <ul>
    * <li>0: no feature is currently defined
    * <li>1: feature is a single polygon vertex; no edges have been encountered
    * <li>2: feature is an edge
    * <li>3: feature is the vertex indexed by 1, with 0 and 2 indexing
    * the previous and following vertices
    * </ul>
    * 
    * @return number of feature vertices
    */
   public int numVertices() {
      if (myPA == null) {
         return 0;
      }
      else if (myPB == null) {
         return 1;
      }
      else if (myPC == null) {
         return 2;
      }
      else {
         return 3;
      }
   }

   /**
    * Returns the <code>idx</code>-th polygon vertex associated with this
    * feature.
    * @param idx vertex index. Should be less than the value returned by
    * {@link #numVertices}.
    * @return <code>idx</code>-th polygon vertex. Value is a reference
    * and should not be modified.
    */
   public Point3d getVertex (int idx) {
      switch (idx) {
         case 0: return myPA;
         case 1: return myPB;
         case 2: return myPC;
         default: {
            throw new IndexOutOfBoundsException (
               "Index "+idx+" not in the range [0,2]");
         }
      }
   }

   /**
    * Returns the reference point for which the nearest feature is determined.
    *
    * @return nearest feature reference point
    */
   public Point3d getPoint() {
      return myPX;
   }

   /**
    * Returns the distance to the nearest feature, or -1 if no feature has been
    * determined yet.
    * 
    * @return distance to the nearest feature.
    */
   public double getDistance() {
      if (myPA == null) {
         return -1;
      }
      else if (myPB == null) {
         return myVtxDist;
      }
      else {
         return myEdgeDist;
      }
   }
   
   /**
    * Returns the nearest point on the nearest fearure, or <code>null</code>
    * if no feature has been determined yet.
    *
    * @return nearest point on the nearest feature
    */
   public Point3d getNearestPoint() {
      switch (numVertices()) {
         case 0: {
            return null;
         }
         case 1: {
            return new Point3d(myPA);
         }
         case 2: {
            double s = LineSegment.projectionParameter (myPA, myPB, myPX);
            Point3d pnt = new Point3d();
            pnt.combine (1-s, myPA, s, myPB);
            return pnt;
         }
         case 3: {
            return new Point3d(myPC);
         }
         default: {
            throw new InternalErrorException (
               "Unexpected number of vertices: " + numVertices());
         }
      }
   }

   /**
    * Marks the start of a new sequence of input polygons. Previous feature
    * information is discarded.
    */
   public void start () {
      myVtxDist = Double.POSITIVE_INFINITY;
      myEdgeDist = Double.POSITIVE_INFINITY;
      myPA = null;
      myPB = null;
      myPC = null;
      restart ();
   }

   /**
    * Marks the start of a new polygon in the current sequence of input
    * polygons. Previous feature information is kept.  There is no need to call
    * this method if the previous polygon was terminated using {@link #close}.
    */
   public void restart () {
      myP0 = null;
      myPL = null;
      myLastMinWasStart = false;
      myLastMinWasP0 = false;
      myStart0 = null;
      myStart1 = null;
   }

   private void setNearestVertex (
      Point3d pa, Point3d pb, Point3d pc) {
      myPA = pa;
      myPB = pb;
      myPC = pc;
   }

   private void setNearestEdge (Point3d pa, Point3d pb, double turnX) {
      myPA = pa;
      myPB = pb;
      myPC = null;
      if (turnX > 0) {
         myOutsideEdgeIfClockwise = 1;
      }
      else if (turnX < 0) {
         myOutsideEdgeIfClockwise = 0;
      }
   }

   /**
    * Determines if the reference point is ''outside'' its nearest feature,
    * based on whether the polygons are assumed to be oriented clockwise or
    * counter-clockwise.

    * @param clockwise if <code>true</code>, indicates that the polygons are
    * oriented clockwise
    * @return <code>true</code> if the reference point is outside
    * its nearest feature
    */
   public int isOutside (boolean clockwise) {
      if (myOutsideEdgeIfClockwise == -1) {
         return -1;
      }
      else if (myPC != null) {
         Vector3d dirX = new Vector3d();
         dirX.sub (myPX, myPB);
         Vector3d dirAB = new Vector3d();
         Vector3d dirBC = new Vector3d();
         dirAB.sub (myPB, myPA);
         dirBC.sub (myPC, myPB);

         boolean convex;
         boolean insideAB;
         boolean insideBC;

         if (clockwise) {
            convex = turn(dirAB,dirBC) <= 0;
            insideAB = turn(dirAB, dirX) <= 0;
            insideBC = turn(dirBC, dirX) <= 0;
         }
         else {
            convex = turn(dirBC,dirAB) <= 0;
            insideAB = turn(dirX, dirAB) <= 0;
            insideBC = turn(dirX, dirBC) <= 0;
         }
         if (convex) {
            // inside if inside both segments,
            // so outside if outside either segment
            return (!insideAB || !insideBC) ? 1 : 0;
         }
         else {
            // inside if inside either segments, so
            // outside if outside both segments
            return (!insideAB && !insideBC) ? 1 : 0;
         }         
      }
      else {
         if (clockwise) {
            return myOutsideEdgeIfClockwise;
         }
         else {
            return myOutsideEdgeIfClockwise == 0 ? 1 : 0;         
         }
      }
   }

   /**
    * Provides a new vertex for the current input polygon.
    * 
    * @param pv position of the polygon vertex.
    */
   public void advance (Point3d pv) {
      
      if (myP0 == null) {
         // this is the first point in the sequence
         myStart0 = pv;
         myP0 = pv;
         if (myPB == null) {
            double d = myPX.distance (pv);
            if (d < myVtxDist) {
               myPA = pv;
               myVtxDist = d;
            }
         }
         return;
      }
      if (pv.distance (myP0) <= myDtol) {
         return; // ignore unless we have moved a certain amount
      }
      Vector3d dir01 = new Vector3d();
      dir01.sub (pv, myP0);

      Vector3d r0 = new Vector3d();      
      Vector3d r1 = new Vector3d();

      r0.sub (myPX, myP0);
      r1.sub (myPX, pv);

      if (myStart1 == null) {
         myStart1 = pv;
      }
      double d;
      double turnR0 = r0.dot(dir01);
      if (turnR0 < 0) {
         d = r0.norm();
         if (myPL == null && d < myEdgeDist) {
            //System.out.println ("setNearest1 d=" + d);
            setNearestEdge (myP0, pv, turn (dir01, r0));
            myLastMinWasStart = true;
            myEdgeDist = d;
         }
         else {
            // can't improve on distance, but can identify a vertex distance
            if (myLastMinWasP0) {
               Vector3d dirAB = new Vector3d();
               dirAB.sub (myP0, myPL);
               setNearestVertex (myPL, myP0, pv);
            }
         }
         myLastMinWasP0 = false;
      }
      else {
         double turnR1 = r1.dot(dir01);
         if (turnR1 > 0) {
            d = r1.norm();
            if (d < myEdgeDist) {
               //System.out.println ("setNearest2 d=" + d);
               setNearestEdge (myP0, pv, turn (dir01, r1));
               myLastMinWasStart = false;
               myLastMinWasP0 = true;
               myEdgeDist = d;
            }
            else {
               myLastMinWasP0 = false;
            }
         }
         else {
            double turnX = turn(dir01,r0);
            d = Math.abs (turnX/dir01.norm());
            if (d < myEdgeDist || myLastMinWasP0) {
               //System.out.println ("setNearest3 d=" + d);
               setNearestEdge (myP0, pv, turnX);
               myLastMinWasStart = false;
               myEdgeDist = d;
            }
            myLastMinWasP0 = false;
         }
      }
      myPL = myP0;
      myP0 = pv;
   }

   /**
    * Closes off the current input polygon and calls {@link #restart}.
    */
   public void close () {

      if (myPL == null) {
         throw new IllegalStateException (
            "advance() must be called before close()");
      }
      advance (myStart0);
      //System.out.println ("close");
      // only thing to decide upon closing with whether the first point
      // corresponds to a vertex. This can only be true if the closest distance
      // corresponds to the segment p0, p1. It can't correspond to the previous
      // segment (pp, p0), because if px is in the Voronoi region for p0, d <
      // myEdgeDist would fail in favor of p0, p1.
      if (myLastMinWasStart) {
         Vector3d dirAB = new Vector3d();
         Vector3d dirBC = new Vector3d();
         dirAB.sub (myStart0, myPL);
         dirBC.sub (myStart1, myStart0);
         setNearestVertex (myPL, myStart0, myStart1);
      }
      if (myPL == myStart1) {
         // only two points, so inside/outside is undefined
         myOutsideEdgeIfClockwise = -1;
      }
      restart();
   }
   
   /**
    * Prints out a description of this feature.
    * 
    * @param msg optional message line
    */
   public void print (String msg) {
      if (msg != null) {
         System.out.println (msg);
      }
      String desc;
      switch (numVertices()) {
         case 0: desc = "UNDEFINED"; break;
         case 1: desc = "VERTEX"; break;
         case 2: desc = "EDGE"; break;
         case 3: desc = "VERTEX"; break;
         default: {
            throw new InternalErrorException (
               "numVertices=" + numVertices() + ", should be between 0 and 3");
         }
      }
      System.out.println (desc + ", nverts=" + numVertices());
      if (numVertices() > 0) {
         System.out.println ("  pa=" + getVertex(0).toString ("%12.8f"));
      }
      if (numVertices() > 1) {
         System.out.println ("  pb=" + getVertex(1).toString ("%12.8f"));
      }
      if (numVertices() > 2) {
         System.out.println ("  pc=" + getVertex(2).toString ("%12.8f"));
      }
   }
   
}
