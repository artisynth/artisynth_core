/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.Point3d;
import maspack.matrix.Vector4d;

/**
 * Implements a NURBS curve
 */
public class NURBSCurve3d extends NURBSCurveBase {

   /**
    * Creates an empty NURBS curve.
    */
   public NURBSCurve3d() {
      super();
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
    * @param ctrlPnts
    * control points
    * @param knots
    * knot values
    * @see #set(int,int,Vector4d[],double[])
    * @throws IllegalArgumentException
    * if constraints on the arguments are violated
    */
   public NURBSCurve3d (int d, int type, Vector4d[] ctrlPnts, double[] knots) {
      super();
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
   public Point3d[] evalPoints (int npnts) {
      Point3d[] pnts = new Point3d[npnts];
      if (npnts == 1) {
         pnts[0] = new Point3d();
         eval (pnts[0], myUstart);
      }
      for (int i = 0; i < npnts; i++) {
         pnts[i] = new Point3d();
         eval (pnts[i], myUstart + (myUend - myUstart) * i / (npnts - 1));
      }
      return pnts;
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
         pnt.z += wb * cpnt.z;
         w += wb;
      }
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
   public double findPoint (Point3d pnt, double umin, double umax) {
      double[] dist = new double[1];
      double[] uinterval = new double[3];

      if (bracket (dist, uinterval, pnt, umin, umax)) {
         return golden (dist, uinterval, pnt, dist[0]);
      }
      else {
         Point3d pntu = new Point3d();
         eval (pntu, umin);
         double dmin = pntu.distance (pnt);

         eval (pntu, umax);
         double dmax = pntu.distance (pnt);

         return (dmin < dmax ? umin : umax);
      }
   }

   public void set (NURBSCurve3d curve) {
      super.set (curve);
   }

}
