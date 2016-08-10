package maspack.collision;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;

public class NearestPolygon3dSegment {

   private enum Plane {
      YZ, ZX, XY };

   Point3d myPX;
   Plane myPlane;
   int mySign;
   double myDtol;

   double myPntDist = Double.POSITIVE_INFINITY;
   Point3d myPN;

   double mySegDist = Double.POSITIVE_INFINITY;
   Point3d myPA;
   Point3d myPB;
   Point3d myPC;

   double myS;
   Point3d myP0;      
   Point3d myPL;

   Vector3d myNrm;
   Vector3d myR0 = new Vector3d();
   Vector3d myLastDir = new Vector3d();
   boolean myLastMinWasP0 = false;
   boolean myLastMinWasStart = false;

   public NearestPolygon3dSegment() {
      myR0 = new Vector3d();
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
   }

   /**
    * returns < 0 for a right turn, > 0 for a left turn, and 0 for no turn.
    */
   double turn (Vector3d a, Vector3d b) {
      Vector3d xprod = new Vector3d();
      xprod.cross (a, b);
      return xprod.dot (myNrm);
   }

   public void start (Point3d p0) {
      myPntDist = myPX.distance (p0);  
      myPN = p0;

      myPA = null;
      myPB = null;
      myPC = null;
      myP0 = p0;
      myPL = null;
      myLastMinWasStart = false;
      myLastMinWasP0 = false;
      myR0.sub (myPX, p0);
   }

   public void restart (Point3d p0) {
      double d = myPX.distance (p0);
      if (d < myPntDist) {
         myPntDist = d;
         myPN = p0;
      }
      myP0 = p0;
      myPL = null;
      myLastMinWasStart = false;
      myLastMinWasP0 = false;
      myR0.sub (myPX, p0);
   }

   private void setNearestVertex (Point3d pa, Point3d pb, Point3d pc) {
      myPA = pa;
      myPB = pb;
      myPC = pc;
   }

   private void setNearestSegment (Point3d pa, Point3d pb) {
      myPA = pa;
      myPB = pb;
      myPC = null;
   }

   private boolean hasNearestSegment () {
      return myPA != null && myPC == null;
   }

   private boolean hasNearestVertex () {
      return myPC != null;
   }

   public void advance (Point3d p1) {
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
      
      Vector3d r1 = new Vector3d();
      r1.sub (myPX, p1);

      myLastMinWasP0 = false;

      double d;
      double turnR0 = turn (perp, myR0);
      if (turnR0 < 0) {
         d = myR0.norm();
         if (myPL == null) {
            setNearestSegment (myP0, p1);
            myLastMinWasStart = true;
            mySegDist = d;
         }
         else {
            // can't improve on distance, but can identify a vertex distance
            if (myLastMinWasP0) {
               double turnDir = turn (myLastDir, dir01);
               if (turnDir*turn (dir01, myR0) < 0) {
                  // turning away from side that px is on, so we have
                  // a vertex situation
                  setNearestVertex (myPL, myP0, p1);
               }
            }
         }
      }
      else {
         double turnR1 = turn (perp, r1);
         if (turnR1 > 0) {
            d = r1.norm();
            if (d < mySegDist) {
               setNearestSegment (myP0, p1);
               myLastMinWasP0 = true;
               myLastMinWasStart = false;
               mySegDist = d;
            }
         }
         else {
            d = Math.abs (turn (dir01, myR0)/dir01.norm());
            if (d < mySegDist) {
               setNearestSegment (myP0, p1);
               myLastMinWasStart = false;
               mySegDist = d;
            }
         }
      }
      myR0.set (r1);
      myLastDir.set (dir01);
      myPL = myP0;
      myP0 = p1;
   }

   public void close (Point3d p1) {
      if (myPL == null) {
         throw new IllegalStateException (
            "advance() must be called before close()");
      }
      // only thing to decide upon closing with whether the first point
      // corresponds to a vertex. This can only be true if the closest distance
      // corresponds to the segment p0, p1. It can't correspond to the previous
      // segment (pp, p0), because if px is in the Voronoi region for p0, d <
      // mySegDist would fail in favor of p0, p1.
      if (myLastMinWasStart) {
         setNearestVertex (myPL, myP0, p1);
      }
   }
}
