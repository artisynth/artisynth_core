package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

/**
 * Utility methods for geometric computation.
 */
public class GeometryUtils {

   private static final double INF = Double.POSITIVE_INFINITY;
   private static final double EPS = 1e-14;

   static boolean debug = false;

   /**
    * Finds the characteristic length of a collection of points.
    *
    * @param pnts points to query
    * @return characteristic length
    */
   static public double getCharacteristicLength (Collection<Point3d> pnts) {
      Vector3d min = new Vector3d (INF, INF, INF);
      Vector3d max = new Vector3d (-INF, -INF, -INF);
      for (Point3d p : pnts) {
         p.updateBounds (min, max);
      }
      Vector3d diag = new Vector3d();
      diag.sub (max, min);
      return diag.norm()/2;
   }

   /**
    * Finds the point on a polyline that is a specified distance {@code dist}
    * from a prescribed point {@code p0}, within machine precision. The
    * polyline is specified by a list of vertices {@code vtxs}. Point locations
    * on the polyline are described by a non-negative parameter {@code r},
    * which takes the form
    * <pre>
    * r = k + s
    * </pre>
    * where {@code k} is the index of a polyline vertex, and {@code s} is
    * a scalar parameter in the range [0,1] that specifies the location along
    * the interval between vertices {@code k} and {@code k+1}. If the
    * polyline is open, the search for the distance point begins at a location
    * specified by {@code r0} and locations before {@code r0} are ignored. If
    * the polyline is closed, {@code r0} is ignored.
    *
    * <p>If found, the method will return the point's location parameter {@code
    * r}, as well as the point's value in the optional parameter {@code pr} if
    * it is not {@code null}. If the point is not found, the method will return
    * -1.
    *
    * <p>It is assumed that no two adjacent vertices on the polyline are
    * identical within machine precision. If they are, an exception will be
    * thrown.
    *
    * @param pr if not {@code null}, returns the distance point, if found
    * @param vtxs list of vertices defining the polyline 
    * @param closed {@code true} if the polyline is closed
    * @param p0 point with respect to which distance should be determined
    * @param dist desired distance from {@code p0}
    * @param r0 for open polylines, a non-negative scalar giving the location
    * on the polyline where the search should start. This should be a
    * non-negative scalar whose value is less than or equal to the number of
    * polyline intervals, which will be {@code vtxs.size()-1}.
    * @return location of the distance point on the polyline, if
    * found, or -1.
    */
   static public double findPointAtDistance (
      Point3d pr, List<Point3d> vtxs, boolean closed,
      Point3d p0, double dist, double r0) {

      double tol = EPS*getCharacteristicLength(vtxs);

      int numi = (closed ? vtxs.size() : vtxs.size()-1); // number of intervals
      if (!closed) {
         if (r0 < 0) {
            throw new IllegalArgumentException ("r0 must not be negative");
         }
         else if (r0 > numi) {
            throw new IllegalArgumentException (
               "r0 exceeds the number of polyline intervals");
         }
      }
      else {
         r0 = 0;
      }
      if (vtxs.size() == 0) {
         return -1;
      }
      else if (vtxs.size() == 1) {
         if (vtxs.get(0).distance (p0) == dist) {
            if (pr != null) {
               pr.set (vtxs.get(0));
            }
            return 0;
         }
         else {
            return -1;
         }
      }
      else {
         int k0 = (int)r0;
         double s0 = r0 - k0;
         Vector3d vecBA = new Vector3d();
         Vector3d vecA0 = new Vector3d();
         Point3d ps = new Point3d();
         int kmax = closed ? vtxs.size()-1 : vtxs.size()-2;
         for (int ka=k0; ka<=kmax; ka++) {
            int kb = (ka+1)%vtxs.size();
            Point3d pa = vtxs.get(ka);
            Point3d pb = vtxs.get(kb);
            // check first point
            Point3d pstart;
            if (s0 > 0) {
               ps.combine (1-s0, pa, s0, pb);
               pstart = ps;
            }
            else {
               pstart = pa;
            }
            if (Math.abs(pstart.distance(p0)-dist) <= tol) {
               if (pr != null) {
                  pr.set (pstart);
               }
               return ka + s0;
            }
            // point lies in the interval ka, kb.  Defining equation is a
            // quadratic.
            if (debug) System.out.printf ("checking %d %d s0=%g\n", ka, kb, s0);
            vecBA.sub (pb, pa);
            vecA0.sub (pa, p0);
            double a = vecBA.dot(vecBA);
            if (a == 0) {
               throw new IllegalArgumentException (
                  "Polyline points at " + ka + " and " + kb +
                  " are identical within machine precision");
            }
            double b = 2*vecBA.dot(vecA0);
            double c = vecA0.dot(vecA0) - dist*dist;
            double[] roots = new double[2];
            int numr = QuadraticSolver.getRoots (roots, a, b, c, s0, 1);
            if (debug) System.out.println ("numr=" + numr);
            double s;
            if (numr > 0) {
               // take the first root, regardless
               s = roots[0];
               if (pr != null) {
                  pr.combine (1-s, pa, s, pb);
               }
               return ka + s;
            }
            else {
               // Might be close to a multiple root solution. See if the
               // quadratic has a minimum in the interval, and if it does, if
               // the distance at that minimum is within tolerance.
               s = -b/(2*a);
               if (s >= s0 && s <= 1) {
                  ps.combine (1-s, pa, s, pb);
                  if (Math.abs(ps.distance(p0)-dist) <= tol) {
                     if (pr != null) {
                        pr.set (ps);
                     }
                     return ka + s;
                  }
               }
            }
            s0 = 0;
         }
         if (!closed) {
            Point3d pb = vtxs.get(vtxs.size()-1);
            if (Math.abs(pb.distance(p0)-dist) <= tol) {
               if (pr != null) {
                  pr.set (pb);
               }
               return vtxs.size()-1;
            }
         }
         return -1;
      }
   }

   /**
    * Finds the nearest point on a polyline to a prescribed point {@code
    * p0}. The polyline is specified by a list of vertices {@code
    * vtxs}. Locations on the polyline are described by a non-negative
    * parameter {@code r}, which takes the form
    * <pre>
    * r = k + s
    * </pre>
    * where {@code k} is the index of a polyline vertex, and {@code s}
    * is a scalar parameter in the range [0,1] that specifies the location
    * along the interval between vertices {@code k} and {@code k+1}.  If the
    * polyline is open, the search for the nearest point begins at a location
    * specified by {@code r0} and locations before {@code r0} are ignored. If
    * the polyline is closed, {@code r0} is ignored. The nearest point may not be
    * unique.
    *
    * <p>The method will return the nearest point's location parameter {@code
    * r}, as well as the point's value in the optional parameter {@code pr} if
    * it is not {@code null}. If the polyline is empty, both {@code r} and
    * {@code pr} at set to 0.
    *
    * <p>It is assumed that no two adjacent vertices on the polyline are
    * identical within machine precision. If they are, an exception will be
    * thrown.
    *
    * @param pr if not {@code null}, returns the nearest point
    * @param vtxs list of vertices defining the polyline 
    * @param closed {@code true} if the polyline is closed
    * @param p0 point for which the nearest point should be determined
    * @param r0 for open polylines, a non-negative scalar giving the location
    * on the polyline where the search should start. This should be a
    * non-negative scalar whose value is less than or equal to the number of
    * polyline intervals, which will be {@code vtxs.size()-1}.
    * @return location of the nearest point on the polyline
    */
   static public double findNearestPoint (
      Point3d pr, List<Point3d> vtxs, boolean closed,
      Point3d p0, double r0) {

      int numi = (closed ? vtxs.size() : vtxs.size()-1); // number of intervals
      if (!closed) {
         if (r0 < 0) {
            throw new IllegalArgumentException ("r0 must not be negative");
         }
         else if (r0 > numi) {
            throw new IllegalArgumentException (
               "r0 exceeds the number of polyline intervals");
         }
      }
      else {
         r0 = 0;
      }
      if (vtxs.size() == 0) {
         if (pr != null) {
            pr.setZero();
         }
         return 0;
      }
      else if (vtxs.size() == 1) {
         if (pr != null) {
            pr.set (vtxs.get(0));
         }
         return 0;
      }
      else {
         // test all intervals and find the nearest location
         int k0 = (int)r0;
         double s0 = r0-k0;
         double rn = 0;
         double mind = INF;
         int kmax = closed ? vtxs.size()-1 : vtxs.size()-2;
         Vector3d vecBA = new Vector3d();
         Vector3d vecA0 = new Vector3d();
         Point3d ps = new Point3d();
         Point3d pn = new Point3d();
         for (int ka=k0; ka<=kmax; ka++) {
            int kb = (ka+1)%vtxs.size();
            Point3d pa = vtxs.get(ka);
            Point3d pb = vtxs.get(kb);
            vecBA.sub (pb, pa);
            vecA0.sub (pa, p0);
            double lenSqrBA = vecBA.dot(vecBA);
            if (lenSqrBA == 0) {
               throw new IllegalArgumentException (
                  "Polyline points at " + ka + " and " + kb +
                  " are identical within machine precision");
            }
            double s = -vecBA.dot(vecA0)/lenSqrBA;
            double d;
            if (s <= s0) {
               d = pa.distance (p0);
               s = s0;
            }
            else if (s < 1) {
               ps.combine (1-s, pa, s, pb);
               d = ps.distance (p0);
            }
            else {
               d = pb.distance (p0);
               s = 1;
            }
            if (d < mind) {
               mind = d;
               rn = ka + s;
               if (rn == vtxs.size()) {
                  rn = 0;
               }
            }
            s0 = 0;
         }
         if (pr != null) {
            int ka = (int)rn;
            double s = rn-ka;
            if (s > 0) {
               int kb = (ka+1)%vtxs.size();
               pr.combine (1-s, vtxs.get(ka), s, vtxs.get(kb));
            }
            else {
               pr.set (vtxs.get(ka));
            }
         }
         return rn;
      }
   }
}
