/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector4d;

/**
 * Implements a NURBS curve
 */
public class NURBSCurve2d extends NURBSCurveBase {

   /**
    * Creates an empty NURBS curve.
    */
   public NURBSCurve2d() {
      super();
   }

   /**
    * Creates a NURBS curve that is a copy of another curve.
    */
   public NURBSCurve2d (NURBSCurve2d curve) {
      this();
      set (curve);   
   }

   /**
    * Creates an open or closed NURBS curve with a specified degree, knots, and
    * control points. For more information on these arguments, see
    * {@link #set(int,int,Vector4d[],double[]) set}.
    * 
    * @param d
    * degree of the curve
    * @param type
    * curve type, which must be either {@link #OPEN OPEN} or
    * {@link #CLOSED CLOSED}.
    * @param knots
    * knot values
    * @param ctrlPnts
    * control points
    * @see #set(int,int,Vector4d[],double[])
    * @throws IllegalArgumentException
    * if constraints on the arguments are violated
    */
   public NURBSCurve2d (int d, int type, Vector4d[] ctrlPnts, double[] knots) {
      this();
      set (d, type, ctrlPnts, knots);
   }

   /**
    * Returns a set of points evaluated along the curve at intervals which are
    * evenly spaced with respect to the curve parameter.
    * 
    * @param npnts
    * number of points to create
    * @return array of evaluated points
    */
   public Point2d[] evalPoints (int npnts) {
      Point2d[] pnts = new Point2d[npnts];
      Point3d pnt3 = new Point3d();
      if (npnts == 1) {
         eval (pnt3, myUstart);
         pnts[0] = new Point2d(pnt3.x, pnt3.y);
      }
      for (int i = 0; i < npnts; i++) {
         eval (pnt3, myUstart + (myUend - myUstart) * i / (npnts - 1));
         pnts[i] = new Point2d(pnt3.x, pnt3.y);
      }
      return pnts;
   }

   /**
    * For closed curves, return 1 if it is oriented counter-clockwise and -1 if
    * clockwise. Returns 0 is the curve is open, or the orientation can't be
    * determined because of self-intersection.
    */
   public int getOrientation() {
      if (!myClosedP) {
         return 0;
      }
      int numc = numControlPoints();
      double area = 0;
      Vector4d p0 = getControlPoint (numc-1);
      for (int i=0; i<numc; i++) {
         Vector4d p1 = getControlPoint (i);
         area += (p1.x-p0.x)*(p1.y+p0.y);
         p0 = p1;
      }
      if (area == 0) {
         return 0;
      }
      else {
         return area < 0 ? 1 : -1;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (int d, int type, Vector4d[] ctrlPnts, double[] knots) {
      super.set (d, type, ctrlPnts, knots);
      // make sure that the z values of the control points are set to zero
      for (int i=0; i<numControlPoints(); i++) {
         myCtrlPnts.get(i).z = 0;
      }
   }

   public void addControlPoint (Vector4d pnt, double knotSpacing) {
      super.addControlPoint (pnt, knotSpacing);
      // make sure that the z value of the control point is set to zero
      myCtrlPnts.get(numControlPoints()-1).z = 0;
   }

   /**
    * {@inheritDoc}
    */
   public void eval (Point3d pnt, double u) {
      int numc = numControlPoints();
      if (numc == 0) {
         throw new IllegalStateException (
            "curve does not contain control points");
      }
      int k = getKnotIndex (u);
      double[] bvals = new double[this.myDegree + 1];
      basisValues (bvals, k, u);

      pnt.setZero();
      double w = 0;

      for (int i = 0; i <= myDegree; i++) {
         Vector4d cpnt = myCtrlPnts.get(getCtrlIndex (k, i, numc));
         double wb = cpnt.w * bvals[i];
         pnt.x += wb * cpnt.x;
         pnt.y += wb * cpnt.y;
         w += wb;
      }
      pnt.z = 0;
      pnt.scale (1 / w);
   }

   /**
    * Finds a u value for a given curve point within a specified interval of the
    * curve. If the point is not actually on the specified interval, this
    * routine attempts to return the u value for the nearest curve point. It
    * should be noted that one point may correspond to multiple u values.
    * 
    * <p>
    * This routine uses an iterative golden section search, and so is not
    * particularly fast.
    * 
    * @param pnt
    * point to search for
    * @param umin
    * minimum u valu for the interval
    * @param umax
    * maximum u valu for the interval
    * @return u value for the point closest to pnt within the interval.
    */
   public double findPoint (Point2d pnt, double umin, double umax) {
      double[] dist = new double[1];
      double[] uinterval = new double[3];
      Point3d pnt3 = new Point3d (pnt.x, pnt.y, 0);

      if (bracket (dist, uinterval, pnt3, umin, umax)) {
         return golden (dist, uinterval, pnt3, dist[0]);
      }
      else {
         Point3d pntu = new Point3d();
         eval (pntu, umin);
         double dmin = pntu.distance (pnt3);

         eval (pntu, umax);
         double dmax = pntu.distance (pnt3);

         return (dmin < dmax ? umin : umax);
      }
   }

   public void set (NURBSCurve2d curve) {
      super.set (curve);
   }

}
