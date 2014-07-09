/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.util.*;
import maspack.matrix.*;

/**
 * Class to calculate distances between a 2D Quadratic Bezier B-spline curve
 * and a point in the plane.
 *
 * <p> Special thanks to Olivier Besson for providing the actual distance
 * calculations:
 * http://blog.gludion.com/2009/08/distance-to-quadratic-bezier-curve.html
 */
public class QuadBezierDistance2d {

   private static double INF = Double.POSITIVE_INFINITY;

   private class AABB {

      Vector4d p0;
      Vector4d p1;
      Vector4d p2;

      double xmin;
      double ymin;
      double xmax;
      double ymax;

      double dsqrMin;
      double dsqrMax;

      AABB (NURBSCurve2d curve, int cidx) {
         int numc = curve.numControlPoints();
         p0 = curve.getControlPoint (cidx);
         // control point indices may wrap, so we account for that 
         cidx = ((cidx+1)%numc);
         p1 = curve.getControlPoint (cidx);
         cidx = ((cidx+1)%numc);
         p2 = curve.getControlPoint (cidx);
         computeBounds();
      }

      void computeBounds() {

         xmin = p0.x;
         ymin = p0.y;
         xmax = p0.x;
         ymax = p0.y;

         if (p1.x < xmin) {
            xmin = p1.x;
         }
         else if (p1.x > xmax) {
            xmax = p1.x;
         }
         if (p1.y < ymin) {
            ymin = p1.y;
         }
         else if (p1.y > ymax) {
            ymax = p1.y;
         }
         if (p2.x < xmin) {
            xmin = p2.x;
         }
         else if (p2.x > xmax) {
            xmax = p2.x;
         }
         if (p2.y < ymin) {
            ymin = p2.y;
         }
         else if (p2.y > ymax) {
            ymax = p2.y;
         }
      }

      double computeDistance (Vector2d near, Vector2d pnt, int orientation) {
         double d = QuadBezierDistance2d.this.computeDistance (
            near, pnt, p0, p1, p2);
         if (orientation == 1) {
            return d;
         }
         else if (orientation == -1) {
            return -d;
         }
         else {
            return Math.abs(d);
         }
      }

      void bracketDistanceSquared (Vector2d pnt) {
         double x = pnt.x;
         double y = pnt.y;
         if (x <= xmin) {
            if (y <= ymin) {
               myP.set (xmin, ymin);
               dsqrMin = myP.distanceSquared (pnt);
            }
            else if (y < ymax) {
               dsqrMin = (xmin-x)*(xmin-x);
            }
            else {
               myP.set (xmin, ymax);
               dsqrMin = myP.distanceSquared (pnt);
            }
         }
         else if (x < xmax) {
            if (y <= ymin) {
               dsqrMin = (ymin-y)*(ymin-y);
            }
            else if (y < ymax) {
               dsqrMin = 0; 
            }
            else {
               dsqrMin = (y-ymax)*(y-ymax);
            }
         }
         else {
            if (y <= ymin) {
               myP.set (xmax, ymin);
               dsqrMin = myP.distanceSquared (pnt);
            }
            else if (y < ymax) {
               dsqrMin = (x-xmax)*(x-xmax);
            }
            else {
               myP.set (xmax, ymax);
               dsqrMin = myP.distanceSquared (pnt);
            }
         }

         if (x < (xmin+xmax)/2) {
            if (y < (ymin+ymax)/2) {
               myP.set (xmax, ymax);
            }
            else {
               myP.set (xmax, ymin);
            }
         }
         else {
            if (y < (ymin+ymax)/2) {
               myP.set (xmin, ymax);
            }
            else {
               myP.set (xmin, ymin);
            }
         }
         dsqrMax = myP.distanceSquared (pnt);
      }
   }

   NURBSCurve2d myCurve;
   int myOrientation;
   AABB[] myAABBs;
   Vector2d myA;
   Vector2d myB;
   Vector2d myMp;   
   Vector2d myP;
   Vector2d myU;
   Vector2d myMinP;
   Vector2d myMaxP;
   double[] myRoots;

   double myXmin;
   double myYmin;
   double myXmax;
   double myYmax;

   public QuadBezierDistance2d() {
      myA = new Vector2d();
      myB = new Vector2d();
      myMp = new Vector2d();
      myP = new Vector2d();
      myU = new Vector2d();
      myMinP = new Vector2d();
      myMaxP = new Vector2d();
      myRoots = new double[3];
   }

   public QuadBezierDistance2d (NURBSCurve2d curve) {
      this();
      setCurve (curve);
   }

   public void setCurve (NURBSCurve2d curve) {
      if (curve.getDegree() != 2) {
         throw new IllegalArgumentException ("Curve must have degree 2");
      }
      if (!curve.isBSpline()) {
         throw new IllegalArgumentException ("Curve must be a B-spline");
      }
      if (curve.isBezier()) {
         myCurve = curve;
      }
      else {
         myCurve = new NURBSCurve2d (curve);
         myCurve.convertToBezier();
      }
      int numc = myCurve.numControlPoints();
      int cidx = myCurve.getCtrlIndex(1, 0, numc);
      // create the AABBs
      myAABBs = new AABB[numc/2];
      for (int i=0; i<myAABBs.length; i++) {
         AABB aabb = new AABB (myCurve, cidx);
         myAABBs[i] = aabb;
         cidx = ((cidx+2)%numc);
      }
      updateGlobalBoundingBox();
      myOrientation = curve.getOrientation();
   }

   protected void updateGlobalBoundingBox() {
      
      // build the AABBs and the overall bounding box

      myXmin = INF;
      myXmax = -INF;
      myYmin = INF;
      myYmax = -INF;
      for (int i=0; i<myAABBs.length; i++) {
         AABB aabb = myAABBs[i];
         if (aabb.xmin < myXmin) {
            myXmin = aabb.xmin;
         }
         if (aabb.xmax > myXmax) {
            myXmax = aabb.xmax;
         }
         if (aabb.ymin < myYmin) {
            myYmin = aabb.ymin;
         }
         if (aabb.ymax > myYmax) {
            myYmax = aabb.ymax;
         }
      }
   }

   public double computeMaxCurvature (){
      System.out.println ("computing");
      if (myCurve == null) {
         throw new IllegalStateException (
            "Curve has not been set");
      }
      int numc = myCurve.numControlPoints();
      int cidx = myCurve.getCtrlIndex(1, 0, numc);
      double kmaxmax = 0;
      for (int i=0; i<numc/2; i++) {
         Vector4d p0 = myCurve.getControlPoint (cidx);
         cidx = ((cidx+1)%numc);
         Vector4d p1 = myCurve.getControlPoint (cidx);
         cidx = ((cidx+1)%numc);
         Vector4d p2 = myCurve.getControlPoint (cidx);
         cidx = ((cidx+1)%numc);
         double kmax = computeMaxCurvature (p0, p1, p2);
         if (kmax > kmaxmax) {
            kmaxmax = kmax;
         }
      }
      return kmaxmax;
   }

   private void evalPoint (
      Vector2d p, double t, Vector4d p0, Vector4d p1, Vector4d p2) {

      double a = (1-t)*(1-t);
      double b = 2*t*(1-t);
      double c = t*t;

      p.x = a*p0.x + b*p1.x + c*p2.x;
      p.y = a*p0.y + b*p1.y + c*p2.y;
   }

   private void evalTangent (
      Vector2d u, double t, Vector4d p0, Vector4d p1, Vector4d p2) {

      double a = -2*(1-t);
      double b = 2*(1-2*t);
      double c = 2*t;

      u.x = a*p0.x + b*p1.x + c*p2.x;
      u.y = a*p0.y + b*p1.y + c*p2.y;
   }

   /**
    * This algorithim should work for 3d curves as well.
    */
   public double computeDistance (
      Vector2d near, Vector2d pnt, Vector4d p0, Vector4d p1, Vector4d p2) {
      
      myA.x = p1.x-p0.x;
      myA.y = p1.y-p0.y;
      myB.x = p2.x-p1.x-myA.x;
      myB.y = p2.y-p1.y-myA.y;
      myMp.x = p0.x-pnt.x;
      myMp.y = p0.y-pnt.y;

      double a = myB.dot(myB);
      double b = 3*myA.dot(myB);
      double c = 2*myA.dot(myA) + myMp.dot(myB);
      double d = myMp.dot(myA);

      int numr = CubicSolver.getRoots (myRoots, a, b, c, d, 0, 1);
      
      double tmin = 0;
      myMinP.set (p0.x, p0.y);
      double dsqrMin = myMinP.distanceSquared (pnt);
      myP.set (p2.x, p2.y);
      double dsqr = myP.distanceSquared (pnt);
      if (dsqr < dsqrMin) {
         dsqrMin = dsqr;
         myMinP.set (myP);
         tmin = 1;
      }
      for (int i=0; i<numr; i++) {
         double t = myRoots[i];
         evalPoint (myP, t, p0, p1, p2);
         dsqr = myP.distanceSquared(pnt);
         if (dsqr < dsqrMin) {
            myMinP.set (myP);
            dsqrMin = dsqr;
            tmin = t;
         }
      }
      if (near != null) {
         near.set (myMinP);
      }
      evalTangent (myU, tmin, p0, p1, p2);
      double dist = Math.sqrt (dsqrMin);
      // adjust the sign according to the orientation. Assume a
      // counterclockwise orientation; this can be corrected globally
      myP.sub (pnt, myMinP);
      if (myU.cross (myP) > 0) {
         // negative values correspond to being "inside"
         return -dist;
      }
      else {
         return dist;
      }
   }

   /**
    * Returns true if <code>pnt</code> is inside a circle centered
    * at <code>center</code> with radius <code>r</code>.
    */
   private boolean isInsideCircle (Vector4d pnt, Vector2d center, double r) {

      double dx = pnt.x-center.x;
      double dy = pnt.y-center.y;
      return Math.sqrt (dx*dx + dy*dy) <= r;
   }

   private final double cube (double x) {
      return x*x*x;
   }

   private static final double EPS = 1e-10;

   /**
    * Computes the maximum curvature for a single Bezier segment.
    * Taken from
    * http://algorithmist.wordpress.com/2010/12/01/quad-bezier-curvature/
    */
   public double computeMaxCurvature (Vector4d p0, Vector4d p1, Vector4d p2) {

      Vector2d p01 = new Vector2d();
      Vector2d p02 = new Vector2d();
      Vector2d pm = new Vector2d();

      p01.set (p1.x-p0.x, p1.y-p0.y);
      p02.set (p2.x-p0.x, p2.y-p0.y);

      double r = p02.norm()/4;
      double area = Math.abs(p01.cross(p02)/2);
      if (area < r*r*EPS) {
         return 0;
      }
      pm.set (0.75*p0.x+0.25*p2.x, 0.75*p0.y+0.25*p2.y);
      boolean p1IsInside1 = isInsideCircle (p0, pm, r);
      pm.set (0.25*p0.x+0.75*p2.x, 0.25*p0.y+0.75*p2.y);
      boolean p1IsInside2 = isInsideCircle (p0, pm, r);
      
      double kappa;

      if (!p1IsInside1 && !p1IsInside2) {
         Vector2d p1m = new Vector2d();
         pm.set (0.5*p0.x+0.5*p2.x, 0.5*p0.y+0.5*p2.y);
         p1m.set (pm.x-p1.x, pm.y-p1.y);
         kappa = cube(p1m.norm())/(area*area);
         System.out.println ("not inside " + (1/kappa));
      }
      else {
         Vector2d p12 = new Vector2d();
         p12.set (p2.x-p1.x, p2.y-p1.y);
         kappa = Math.max (area/cube(p01.norm()), area/cube(p12.norm()));
         System.out.println ("inside " + (1/kappa));
      }
      return kappa;
   }

   public double getHullDistance (
      Point2d pnt, Vector4d p0, Vector4d p1, Vector4d p2) {

      double d2x = p2.x - p1.x;
      double d2y = p2.y - p1.y;
      double d1x = p1.x - p0.x;
      double d1y = p1.y - p0.y;
      double d0x = p0.x - p2.x;
      double d0y = p0.y - p2.y;

      // orientation == 1 means edges run counter clockwise
      double orientation = (d1y*d2x - d1x*d2y > 0 ? 1 : -1);

      double dx = pnt.x - p0.x;
      double dy = pnt.y - p0.y;
      double xprod = d1y*dx-d1x*dy;
      if (xprod*orientation >= 0) {
         // point is outside edge 01
         if (d1x*dx + d1y*dy <= 0) {
            myP.set (p0.x, p0.y);
            return myP.distance (pnt);
         }
         dx = pnt.x - p1.x;
         dy = pnt.y - p1.y;
         if (d1x*dx + d1y*dy >= 0) {
            myP.set (p1.x, p1.y);
            return myP.distance (pnt);
         }
         else {
            return xprod/Math.sqrt (d1x*d1x+d1y*d1y);
         }
      }
      dx = pnt.x - p1.x;
      dy = pnt.y - p1.y;
      xprod = d2y*dx-d2x*dy;
      if (xprod*orientation >= 0) {
         // point is outside edge 12
         if (d2x*dx + d2y*dy <= 0) {
            myP.set (p1.x, p1.y);
            return myP.distance (pnt);
         }
         dx = pnt.x - p2.x;
         dy = pnt.y - p2.y;
         if (d2x*dx + d2y*dy >= 0) {
            myP.set (p2.x, p2.y);
            return myP.distance (pnt);
         }
         else {
            return xprod/Math.sqrt (d2x*d2x+d2y*d2y);
         }
      }
      dx = pnt.x - p2.x;
      dy = pnt.y - p2.y;
      xprod = d0y*dx-d0x*dy;
      if (xprod*orientation >= 0) {
         // point is outside edge 20
         if (d0x*dx + d0y*dy <= 0) {
            myP.set (p2.x, p2.y);
            return myP.distance (pnt);
         }
         dx = pnt.x - p0.x;
         dy = pnt.y - p0.y;
         if (d0x*dx + d0y*dy >= 0) {
            myP.set (p0.x, p0.y);
            return myP.distance (pnt);
         }
         else {
            return xprod/Math.sqrt (d0x*d0x+d0y*d0y);
         }
      }
      // point is on or inside triangle
      return 0;
   }

   public double computeDistance (Vector2d near, Vector2d pnt, double maxd) {

      double maxdSqr = maxd*maxd;

      double minmin = Double.POSITIVE_INFINITY;
      double minmax = Double.POSITIVE_INFINITY;

      for (int i=0; i<myAABBs.length; i++) {
         AABB aabb = myAABBs[i];
         aabb.bracketDistanceSquared (pnt);
         if (aabb.dsqrMin < minmin) {
            minmin = aabb.dsqrMin;
         }
         if (aabb.dsqrMax < minmax) {
            minmax = aabb.dsqrMax;
         }
      }

      if (near != null) {
         near.set (pnt);
      }
      double mind = maxd;
      Vector2d tmp = null;
      if (near != null) {
         tmp = new Vector2d();
      }
      for (int i=0; i<myAABBs.length; i++) {
         AABB aabb = myAABBs[i];
         if (aabb.dsqrMin < maxdSqr && aabb.dsqrMin <= minmax) {
            double d = aabb.computeDistance (tmp, pnt, myOrientation);
            if (Math.abs(d) < Math.abs(mind)) {
               mind = d;
               if (near != null) {
                  near.set (tmp);
               }
            }
         }
      }
      return mind; 
   }

   public double computeInteriorDistance (Vector2d near, Vector2d pnt) {

      if (!myCurve.isClosed()) {
         throw new IllegalArgumentException (
            "Interior distance is only defined for closed curves");
      }

      if (near != null) {
         near.set (pnt);
      }
      // check the overall bounding box first
      if (pnt.x <= myXmin || pnt.x >= myXmax ||
          pnt.y <= myYmin || pnt.y >= myYmax) {
         return 0;
      }

      double minmax = Double.POSITIVE_INFINITY;

      for (int i=0; i<myAABBs.length; i++) {
         AABB aabb = myAABBs[i];
         aabb.bracketDistanceSquared (pnt);
         if (aabb.dsqrMax < minmax) {
            minmax = aabb.dsqrMax;
         }
      }

      double mind = INF;

      Vector2d tmp = null;
      if (near != null) {
         tmp = new Vector2d();
      }
      for (int i=0; i<myAABBs.length; i++) {
         AABB aabb = myAABBs[i];
         if (aabb.dsqrMin <= minmax) {
            double d = aabb.computeDistance (tmp, pnt, myOrientation);
            if (Math.abs(d) < Math.abs(mind)) {
               mind = d;
               if (near != null) {
                  near.set (tmp);
               }
            }
         }
      }
      if (mind >= 0) {
         if (near != null) {
            near.set (pnt);
         }
         return 0;
      }
      else {
         return mind;
      }
   }

}
