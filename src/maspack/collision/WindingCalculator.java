package maspack.collision;

import maspack.matrix.*;
import maspack.util.*;

/**
 * Computes the winding number for a point with respect to a planar 3D
 * polygon. The algorithm is based on Algorithm 6 of "The Point in Polygon
 * Problem for Arbitrary Polygons", by Hormann and Agathos.
 *
 * For simple polygons, if the winding number is 0, the point is outside the
 * polygon. If the point is inside, the winding number will be -1 or 1,
 * depending on whether the polygon surrounds the point in a clockwise or
 * counter-clockwise direction.
 */
public class WindingCalculator {

   private enum Plane {
      YZ, ZX, XY };

   Point3d myPnt;
   Vector3d myRay;
   Plane myPlane;
   int myNum;
   int mySign;

   boolean myLastTop;
   double myLastDot;
   double myLastDx;
   double myLastDy;

   public WindingCalculator () {
      myPnt = new Point3d();
      myRay = new Vector3d();
   }

   public void init (Vector3d ray, Vector3d nrm) {
      myRay.set (ray);
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
   }

   public int getNumber() {
      return myNum*mySign;
   }

   public void start (Point3d pnt, Point3d px) {
      restart (pnt, px);
      myNum = 0;
   }

   public void restart (Point3d pnt, Point3d px) {
      myPnt.set (pnt);
      switch (myPlane) {
         case YZ: {
            startYZ (px);
            break;
         }
         case ZX: {
            startZX (px);
            break;
         }
         case XY: {
            startXY (px);
            break;
         }
         default: {
            throw new InternalErrorException ("Unimplemented plane " + myPlane);
         }
      }
   }

   public void advance (Point3d px) {
      switch (myPlane) {
         case YZ: {
            advanceYZ (px);
            break;
         }
         case ZX: {
            advanceZX (px);
            break;
         }
         case XY: {
            advanceXY (px);
            break;
         }
         default: {
            throw new InternalErrorException ("Unimplemented plane " + myPlane);
         }
      }
   }

   void startYZ (Point3d px) {
      double dx = px.y-myPnt.y;
      double dy = px.z-myPnt.z;

      myLastTop = ((myRay.y*dy - myRay.z*dx) >= 0);
      myLastDot = (myRay.y*dx + myRay.z*dy);
      myLastDx = dx;
      myLastDy = dy;
   }

   void advanceYZ (Point3d px) {
      double dx = px.y-myPnt.y;
      double dy = px.z-myPnt.z;

      boolean top = ((myRay.y*dy - myRay.z*dx) >= 0);
      double dot = (myRay.y*dx + myRay.z*dy);
      if (top != myLastTop) {
         boolean fromBelow = top;
         if (myLastDot >= 0) { // last point on or in right half plane
            if (dot > 0) {     // point in right half plane
               myNum += (fromBelow ? 1 : -1);
            }
            else {
               double det = (myLastDx*dy-myLastDy*dx);
               if ((det > 0) == fromBelow) { // right_crossing
                  myNum += (fromBelow ? 1 : -1);
               }
            }
         }
         else {
            if (dot > 0) {    // point in right half plane
               double det = (myLastDx*dy-myLastDy*dx);
               if ((det > 0) == fromBelow) { // right_crossing
                  myNum += (fromBelow ? 1 : -1);
               }
            }
         }
      }
      myLastTop = top;
      myLastDot = dot;
      myLastDx = dx;
      myLastDy = dy;
   }

   void startZX (Point3d px) {
      double dx = px.z-myPnt.z;
      double dy = px.x-myPnt.x;

      myLastTop = ((myRay.z*dy - myRay.x*dx) >= 0);
      myLastDot = (myRay.z*dx + myRay.x*dy);
      myLastDx = dx;
      myLastDy = dy;
   }

   void advanceZX (Point3d px) {
      double dx = px.z-myPnt.z;
      double dy = px.x-myPnt.x;

      boolean top = ((myRay.z*dy - myRay.x*dx) >= 0);
      double dot = (myRay.z*dx + myRay.x*dy);
      if (top != myLastTop) {
         boolean fromBelow = top;
         if (myLastDot >= 0) { // last point on or in right half plane
            if (dot > 0) {     // point in right half plane
               myNum += (fromBelow ? 1 : -1);
            }
            else {
               double det = (myLastDx*dy-myLastDy*dx);
               if ((det > 0) == fromBelow) { // right_crossing
                  myNum += (fromBelow ? 1 : -1);
               }
            }
         }
         else {
            if (dot > 0) {    // point in right half plane
               double det = (myLastDx*dy-myLastDy*dx);
               if ((det > 0) == fromBelow) { // right_crossing
                  myNum += (fromBelow ? 1 : -1);
               }
            }
         }
      }
      myLastTop = top;
      myLastDot = dot;
      myLastDx = dx;
      myLastDy = dy;
   }

   void startXY (Point3d px) {
      double dx = px.x-myPnt.x;
      double dy = px.y-myPnt.y;

      myLastTop = ((myRay.x*dy - myRay.y*dx) >= 0);
      myLastDot = (myRay.x*dx + myRay.y*dy);
      myLastDx = dx;
      myLastDy = dy;
   }

   void advanceXY (Point3d px) {
      double dx = px.x-myPnt.x;
      double dy = px.y-myPnt.y;

      boolean top = ((myRay.x*dy - myRay.y*dx) >= 0);
      double dot = (myRay.x*dx + myRay.y*dy);
      if (top != myLastTop) {
         boolean fromBelow = top;
         if (myLastDot >= 0) { // last point on or in right half plane
            if (dot > 0) {     // point in right half plane
               myNum += (fromBelow ? 1 : -1);
            }
            else {
               double det = (myLastDx*dy-myLastDy*dx);
               if ((det > 0) == fromBelow) { // right_crossing
                  myNum += (fromBelow ? 1 : -1);
               }
            }
         }
         else {
            if (dot > 0) {    // point in right half plane
               double det = (myLastDx*dy-myLastDy*dx);
               if ((det > 0) == fromBelow) { // right_crossing
                  myNum += (fromBelow ? 1 : -1);
               }
            }
         }
      }
      myLastTop = top;
      myLastDot = dot;
      myLastDx = dx;
      myLastDy = dy;
   }

}
