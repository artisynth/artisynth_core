package maspack.collision;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;

public class NearestPolygon3dFeature {

   private enum Plane {
      YZ, ZX, XY };

   Point3d myPX;
   Plane myPlane;
   int mySign;
   double myDtol;

   double mySegDist = Double.POSITIVE_INFINITY;
   double myPntDist = Double.POSITIVE_INFINITY;
   Point3d myPA;
   Point3d myPB;
   Point3d myPC;

   Point3d myP0;      
   Point3d myPL;

   Point3d myStart0;
   Point3d myStart1;

   Vector3d myNrm;

   boolean myLastMinWasP0 = false;
   boolean myLastMinWasStart = false;

   int myOutsideIfClockwise = -1;

   public NearestPolygon3dFeature() {
      myNrm = new Vector3d();
   }

   public void init (Point3d px, Vector3d nrm, double dtol) {
      switch (nrm.maxAbsIndex()) {
         case 0: {
            myPlane = Plane.YZ;
            mySign = nrm.x < 0 ? -1 : 1;
            break;
         }
         case 1: {
            myPlane = Plane.ZX;
            mySign = nrm.y < 0 ? -1 : 1;
            break;
         }
         case 2: {
            myPlane = Plane.XY;
            mySign = nrm.z < 0 ? -1 : 1;
            break;
         }
      }      
      myDtol = dtol;
      myPX = new Point3d(px);
      myNrm.set (nrm);
      myPA = null;
      myPB = null;
      myPC = null;
      mySegDist = Double.POSITIVE_INFINITY;
      myPntDist = Double.POSITIVE_INFINITY;
   }

   /**
    * returns < 0 for a right turn, > 0 for a left turn, and 0 for no turn.
    */
   double turn (Vector3d a, Vector3d b) {
      switch (myPlane) {
         case YZ: {
            return (a.y*b.z - a.z*b.y)/myNrm.x;
         }
         case ZX: {
            return (a.z*b.x - a.x*b.z)/myNrm.y;
         }
         case XY: {
            return (a.x*b.y - a.y*b.x)/myNrm.z;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented plane type "+myPlane);
         }
      }
      // Vector3d xprod = new Vector3d();
      // xprod.cross (a, b);
      // return xprod.dot (myNrm);
   }

   public boolean hasStarted() {
      return myP0 != null;
   }

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

   public Point3d getPoint() {
      return myPX;
   }

   public double getDistance() {
      if (myPB == null) {
         return myPntDist;
      }
      else {
         return mySegDist;
      }
   }

   public void start (Point3d p0) {
      //System.out.println ("start " + p0.toString("%16.12f"));
      myPntDist = myPX.distance (p0);
      mySegDist = Double.POSITIVE_INFINITY;
      myPA = p0;
      myPB = null;
      myPC = null;
      myP0 = p0;
      myPL = null;
      myLastMinWasStart = false;
      myLastMinWasP0 = false;
      myStart0 = p0;
      myStart1 = null;
   }

   public void restart (Point3d p0) {
      //System.out.println ("restart " + p0.toString("%16.12f"));
      if (myPB == null) {
         double d = myPX.distance (p0);
         if (d < myPntDist) {
            myPA = p0;
            myPntDist = d;
         }
      }
      myP0 = p0;
      myPL = null;
      myLastMinWasStart = false;
      myLastMinWasP0 = false;
      myStart0 = p0;
      myStart1 = null;
   }

   private void setNearestVertex (
      Point3d pa, Point3d pb, Point3d pc, double turnABC) {
      myPA = pa;
      myPB = pb;
      myPC = pc;
      if (turnABC < 0) {
         myOutsideIfClockwise = 1;
      }
      else if (turnABC > 0) {
         myOutsideIfClockwise = 0;
      }
   }

   private void setNearestSegment (Point3d pa, Point3d pb, double turnX) {
      myPA = pa;
      myPB = pb;
      myPC = null;
      if (turnX > 0) {
         myOutsideIfClockwise = 1;
      }
      else if (turnX < 0) {
         myOutsideIfClockwise = 0;
      }
   }

   private boolean hasNearestSegment () {
      return myPA != null && myPC == null;
   }

   private boolean hasNearestVertex () {
      return myPC != null;
   }

   public int isOutside (boolean clockwise) {
      if (myOutsideIfClockwise == -1) {
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
            return myOutsideIfClockwise;
         }
         else {
            return myOutsideIfClockwise == 0 ? 1 : 0;         
         }
      }
   }

   public void advance (Point3d p1) {
      //System.out.println ("advance " + p1.toString("%16.12f"));
      if (myP0 == null) {
         throw new IllegalStateException (
            "start() must be called before advance()");
      }
      if (p1.distance (myP0) <= myDtol) {
         return; // ignore unless we have moved a certain amount
      }
      Vector3d dir01 = new Vector3d();
      Vector3d perp = new Vector3d();
      dir01.sub (p1, myP0);
      perp.cross (dir01, myNrm);

      Vector3d r0 = new Vector3d();      
      Vector3d r1 = new Vector3d();

      r0.sub (myPX, myP0);
      r1.sub (myPX, p1);

      if (myStart1 == null) {
         myStart1 = p1;
      }
      double d;
      double turnR0 = turn (perp, r0);
      if (turnR0 < 0) {
         d = r0.norm();
         if (myPL == null && d < mySegDist) {
            //System.out.println ("setNearest1 d=" + d);
            setNearestSegment (myP0, p1, turn (dir01, r0));
            myLastMinWasStart = true;
            mySegDist = d;
         }
         else {
            // can't improve on distance, but can identify a vertex distance
            if (myLastMinWasP0) {
               Vector3d dirAB = new Vector3d();
               dirAB.sub (myP0, myPL);
               setNearestVertex (myPL, myP0, p1, turn (dirAB, dir01));
            }
         }
         myLastMinWasP0 = false;
      }
      else {
         double turnR1 = turn (perp, r1);
         if (turnR1 > 0) {
            d = r1.norm();
            if (d < mySegDist) {
               //System.out.println ("setNearest2 d=" + d);
               setNearestSegment (myP0, p1, turn (dir01, r1));
               myLastMinWasStart = false;
               myLastMinWasP0 = true;
               mySegDist = d;
            }
            else {
               myLastMinWasP0 = false;
            }
         }
         else {
            double turnX = turn(dir01,r0);
            d = Math.abs (turnX/dir01.norm());
            if (d < mySegDist || myLastMinWasP0) {
               //System.out.println ("setNearest3 d=" + d);
               setNearestSegment (myP0, p1, turnX);
               myLastMinWasStart = false;
               mySegDist = d;
            }
            myLastMinWasP0 = false;
         }
      }
      myPL = myP0;
      myP0 = p1;
   }

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
      // mySegDist would fail in favor of p0, p1.
      if (myLastMinWasStart) {
         Vector3d dirAB = new Vector3d();
         Vector3d dirBC = new Vector3d();
         dirAB.sub (myStart0, myPL);
         dirBC.sub (myStart1, myStart0);
         setNearestVertex (myPL, myStart0, myStart1, turn(dirAB, dirBC));
      }
      if (myPL == myStart1) {
         // only two points, so inside/outside is undefined
         myOutsideIfClockwise = -1;
      }
   }
   
   public void print (String msg) {
      if (msg != null) {
         System.out.println (msg);
      }
      System.out.println ("  nverts=" + numVertices());
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
