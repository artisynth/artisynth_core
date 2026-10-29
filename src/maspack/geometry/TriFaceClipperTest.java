package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

/**
 * Test class for {@link TriFaceClipper}.
 *
 * <p>Clipping results are checked against an independent reference
 * implementation ({@link RefRegion}), which finds the parameter interval of a
 * segment lying within the region by intersecting {@code [0,1]} with the four
 * half spaces that bound the region. The reference implementation is itself
 * anchored by a sampling test which classifies points along the segment
 * directly, and by explicit tests on a canonical triangle for which the region
 * is known by inspection.
 */
public class TriFaceClipperTest extends UnitTest {

   private static final double EPS = 1e-12;

   // tolerance for comparing parametric clipping values and distances
   private static final double TOL = 1e-9;

   // Reference intervals whose length is less than this are considered
   // ill-conditioned, and the corresponding cases are ignored by the random
   // tests. The same margin is applied to intervals which are empty.
   private static final double MIN_INTERVAL = 1e-6;

   // number of samples used by the sampling test
   private static final int NSAMPLES = 64;

   /**
    * Independent reference description of the upper and lower regions of a
    * triangular face.
    */
   private static class RefRegion {

      Point3d[] myPnts;       // triangle points
      Vector3d myNrm;         // face normal
      Vector3d[] myEdgeNrms;  // outward facing normals of the edge planes

      RefRegion (Point3d p0, Point3d p1, Point3d p2) {
         myPnts = new Point3d[] {
            new Point3d(p0), new Point3d(p1), new Point3d(p2) };
         Vector3d u = new Vector3d();
         Vector3d v = new Vector3d();
         u.sub (p1, p0);
         v.sub (p2, p0);
         myNrm = new Vector3d();
         myNrm.cross (u, v);
         myNrm.normalize();
         myEdgeNrms = new Vector3d[3];
         for (int i=0; i<3; i++) {
            Vector3d edge = new Vector3d();
            edge.sub (myPnts[(i+1)%3], myPnts[i]);
            Vector3d nrm = new Vector3d();
            nrm.cross (edge, myNrm);
            nrm.normalize();
            myEdgeNrms[i] = nrm;
         }
      }

      /**
       * Signed distance of {@code p} from the face plane.
       */
      double faceDistance (Point3d p) {
         Vector3d del = new Vector3d();
         del.sub (p, myPnts[0]);
         return myNrm.dot (del);
      }

      /**
       * Returns {@code true} if {@code p} lies within the region indicated by
       * {@code side}, to within a tolerance {@code tol}.
       */
      boolean isInRegion (Point3d p, int side, double tol) {
         if (side*faceDistance(p) < -tol) {
            return false;
         }
         Vector3d del = new Vector3d();
         for (int i=0; i<3; i++) {
            del.sub (p, myPnts[i]);
            if (myEdgeNrms[i].dot (del) > tol) {
               return false;
            }
         }
         return true;
      }

      /**
       * Intersects the interval {@code lohi} with the set of {@code s} values
       * for which {@code sign*nrm.dot(p(s)-base) >= 0}, where {@code p(s) =
       * (1-s) p0 + s p1}. Returns {@code true} if the constraint is violated
       * for all values of {@code s}.
       */
      private boolean intersectInterval (
         double[] lohi, Vector3d nrm, Point3d base,
         Point3d p0, Point3d p1, int sign) {

         Vector3d del = new Vector3d();
         del.sub (p0, base);
         double a = sign*nrm.dot(del);
         del.sub (p1, base);
         double b = sign*nrm.dot(del);
         if (a == b) {
            // segment is parallel to the plane
            return (a < 0);
         }
         // constraint value is zero at s
         double s = a/(a-b);
         if (b > a) {
            // constraint value increases with s
            if (s > lohi[0]) {
               lohi[0] = s;
            }
         }
         else {
            if (s < lohi[1]) {
               lohi[1] = s;
            }
         }
         return false;
      }

      /**
       * Returns the parameter interval of the segment {@code p0}-{@code p1}
       * which lies within the region indicated by {@code side}. If the
       * intersection is empty, returns {@code [1,0]}.
       */
      double[] clipInterval (Point3d p0, Point3d p1, int side) {
         double[] lohi = new double[] { 0, 1 };
         boolean empty = intersectInterval (
            lohi, myNrm, myPnts[0], p0, p1, side);
         for (int i=0; i<3; i++) {
            empty |= intersectInterval (
               lohi, myEdgeNrms[i], myPnts[i], p0, p1, -1);
         }
         if (empty || lohi[0] > lohi[1]) {
            return new double[] { 1, 0 };
         }
         return lohi;
      }

      /**
       * Returns the maximum unsigned distance from the face of the segment
       * {@code p0}-{@code p1} clipped to the region indicated by {@code side},
       * or -1 if the clipped segment is empty.
       */
      double maxClippedDistance (Point3d p0, Point3d p1, int side) {
         double[] lohi = clipInterval (p0, p1, side);
         if (lohi[0] > lohi[1]) {
            return -1;
         }
         Point3d ps0 = new Point3d();
         Point3d ps1 = new Point3d();
         ps0.combine (1-lohi[0], p0, lohi[0], p1);
         ps1.combine (1-lohi[1], p0, lohi[1], p1);
         return Math.max (
            Math.abs(faceDistance(ps0)), Math.abs(faceDistance(ps1)));
      }

      /**
       * Returns the maximum unsigned distance from the face of the edges of
       * the triangle {@code p0}, {@code p1}, {@code p2}, clipped to the region
       * indicated by {@code side}, or -1 if all the edges are clipped away.
       */
      double maxClippedDistance (
         Point3d p0, Point3d p1, Point3d p2, int side) {
         double maxd = maxClippedDistance (p0, p1, side);
         maxd = Math.max (maxd, maxClippedDistance (p1, p2, side));
         maxd = Math.max (maxd, maxClippedDistance (p2, p0, side));
         return maxd;
      }

      /**
       * Returns {@code true} if the point {@code p}, which is assumed to lie
       * in the plane of the triangle {@code q0}, {@code q1}, {@code q2}, also
       * lies within that triangle, to within a tolerance {@code tol}. Returns
       * {@code false} if the triangle is degenerate.
       */
      private boolean isInTriangle (
         Point3d q0, Point3d q1, Point3d q2, Point3d p, double tol) {
         Vector3d u = new Vector3d();
         Vector3d v = new Vector3d();
         u.sub (q1, q0);
         v.sub (q2, q0);
         Vector3d nrm = new Vector3d();
         nrm.cross (u, v);
         double area = nrm.norm();
         if (area == 0) {
            return false;
         }
         nrm.scale (1/area);
         Point3d[] q = new Point3d[] { q0, q1, q2 };
         Vector3d edge = new Vector3d();
         Vector3d del = new Vector3d();
         Vector3d xprod = new Vector3d();
         for (int i=0; i<3; i++) {
            edge.sub (q[(i+1)%3], q[i]);
            del.sub (p, q[i]);
            xprod.cross (edge, del);
            if (xprod.dot (nrm) < -tol*area) {
               return false;
            }
         }
         return true;
      }

      /**
       * Returns the <i>exact</i> maximum unsigned distance from the face of
       * the triangle {@code p0}, {@code p1}, {@code p2} clipped to the region
       * indicated by {@code side}, or -1 if no part of the triangle lies in
       * the region.
       *
       * <p>The clipped triangle is a convex polygon over which the distance
       * from the face varies affinely, so the maximum is attained at one of
       * its vertices. These vertices are enumerated directly, using a
       * different approach from the polygon clipping employed by {@link
       * TriFaceClipper#maxClippedDistance(Point3d,Point3d,Point3d,int)}:
       * they consist of (a) triangle
       * vertices lying in the region, (b) points at which the triangle's edges
       * cross one of the four planes bounding the region, and (c) points at
       * which the three vertical corner lines of the region pierce the
       * triangle.
       */
      double exactMaxClippedDistance (
         Point3d p0, Point3d p1, Point3d p2, int side) {

         double tol = TOL;
         double maxd = -1;
         Point3d[] q = new Point3d[] { p0, p1, p2 };

         // (a) triangle vertices
         for (int i=0; i<3; i++) {
            maxd = updateMax (maxd, q[i], side, tol);
         }
         // (b) crossings of the triangle edges with the bounding planes
         Point3d px = new Point3d();
         for (int i=0; i<3; i++) {
            Point3d pa = q[i];
            Point3d pb = q[(i+1)%3];
            for (int j=0; j<4; j++) {
               // signed distances to the plane, positive on the inside
               double da, db;
               if (j == 0) {
                  da = side*faceDistance (pa);
                  db = side*faceDistance (pb);
               }
               else {
                  da = -planeDistance (j-1, pa);
                  db = -planeDistance (j-1, pb);
               }
               if ((da < 0) != (db < 0)) {
                  double s = da/(da-db);
                  px.combine (1-s, pa, s, pb);
                  maxd = updateMax (maxd, px, side, tol);
               }
            }
         }
         // (c) corner lines of the region piercing the triangle
         Vector3d u = new Vector3d();
         Vector3d v = new Vector3d();
         u.sub (p1, p0);
         v.sub (p2, p0);
         Vector3d qnrm = new Vector3d();
         qnrm.cross (u, v);
         if (qnrm.norm() > 0) {
            qnrm.normalize();
            double dot = qnrm.dot (myNrm);
            if (dot != 0) {
               Vector3d del = new Vector3d();
               for (int i=0; i<3; i++) {
                  del.sub (myPnts[i], p0);
                  double t = -qnrm.dot(del)/dot;
                  px.scaledAdd (t, myNrm, myPnts[i]);
                  if (isInTriangle (p0, p1, p2, px, tol)) {
                     maxd = updateMax (maxd, px, side, tol);
                  }
               }
            }
         }
         return maxd;
      }

      /**
       * Signed distance of {@code p} from the plane of edge {@code i}, with
       * positive values lying outside the region.
       */
      private double planeDistance (int i, Point3d p) {
         Vector3d del = new Vector3d();
         del.sub (p, myPnts[i]);
         return myEdgeNrms[i].dot (del);
      }

      /**
       * Updates {@code maxd} with the distance of {@code p} from the face, if
       * {@code p} lies in the region indicated by {@code side}.
       */
      private double updateMax (double maxd, Point3d p, int side, double tol) {
         if (isInRegion (p, side, tol)) {
            return Math.max (maxd, Math.abs (faceDistance (p)));
         }
         else {
            return maxd;
         }
      }
   }

   /**
    * Returns the triangle points of a canonical right triangle in the x-y
    * plane, with vertices at the origin, (1,0,0) and (0,1,0).
    */
   private Point3d[] canonicalTriangle() {
      return new Point3d[] {
         new Point3d (0, 0, 0),
         new Point3d (1, 0, 0),
         new Point3d (0, 1, 0) };
   }

   private Point3d[] transform (Point3d[] pnts, RigidTransform3d T) {
      Point3d[] xpnts = new Point3d[pnts.length];
      for (int i=0; i<pnts.length; i++) {
         xpnts[i] = new Point3d (pnts[i]);
         xpnts[i].transform (T);
      }
      return xpnts;
   }

   private Point3d pointOnSegment (Point3d p0, Point3d p1, double s) {
      Point3d p = new Point3d();
      p.combine (1-s, p0, s, p1);
      return p;
   }

   /**
    * Checks the reference region implementation itself, by sampling points
    * along the segment {@code p0}-{@code p1} and verifying that those which
    * are classified as being inside the region correspond to the parameter
    * interval computed by the reference.
    */
   private void checkRefInterval (
      RefRegion ref, Point3d p0, Point3d p1, int side, String msg) {

      double[] lohi = ref.clipInterval (p0, p1, side);
      double len = p0.distance (p1);
      double tol = 1e-8*(len == 0 ? 1 : len);
      for (int i=0; i<=NSAMPLES; i++) {
         double s = i/(double)NSAMPLES;
         if (s < lohi[0]-MIN_INTERVAL ||
             s > lohi[1]+MIN_INTERVAL ||
             (s > lohi[0]+MIN_INTERVAL && s < lohi[1]-MIN_INTERVAL)) {
            // sample is not close to an interval end point, so its
            // classification should be unambiguous
            boolean inside = ref.isInRegion (
               pointOnSegment (p0, p1, s), side, tol);
            boolean insideChk = (s >= lohi[0] && s <= lohi[1]);
            if (inside != insideChk) {
               throw new TestException (
                  msg + ": sample point at s=" + s + " is " +
                  (inside ? "inside" : "outside") + " the region, but the " +
                  "reference interval is [" + lohi[0] + "," + lohi[1] + "]");
            }
         }
      }
   }

   /**
    * Checks {@code clipSegment()} and the segment based {@code
    * maxClippedDistance()} against the reference implementation. Returns
    * {@code false} if the case was ignored because it is ill-conditioned.
    */
   private boolean checkClipSegment (
      TriFaceClipper clipper, RefRegion ref,
      Point3d p0, Point3d p1, int side, String msg) {

      double[] chk = ref.clipInterval (p0, p1, side);
      boolean emptyChk = (chk[0] > chk[1]);
      if (emptyChk) {
         // ignore if the interval is only just empty
         if (chk[0]-chk[1] < MIN_INTERVAL) {
            return false;
         }
      }
      else {
         // ignore if the interval is only just non-empty
         if (chk[1]-chk[0] < MIN_INTERVAL) {
            return false;
         }
      }
      checkRefInterval (ref, p0, p1, side, msg);

      double[] svals = new double[2];
      Point3d ps0 = new Point3d();
      Point3d ps1 = new Point3d();
      boolean res = clipper.clipSegment (svals, ps0, ps1, p0, p1, side);
      if (res == emptyChk) {
         throw new TestException (
            msg + ": clipSegment() returned " + res + ", expected " + !res);
      }
      // result should be the same if the optional arguments are null
      if (clipper.clipSegment (null, null, null, p0, p1, side) != res) {
         throw new TestException (
            msg + ": clipSegment() result differs when arguments are null");
      }
      if (!res) {
         checkEquals (msg+": svals", svals, new double[] {1, 0}, 0);
      }
      else {
         checkEquals (msg+": svals", svals, chk, TOL);
         checkEquals (
            msg+": ps0", ps0, pointOnSegment (p0, p1, svals[0]), TOL);
         checkEquals (
            msg+": ps1", ps1, pointOnSegment (p0, p1, svals[1]), TOL);
      }
      // check the maximum clipped distance for the same segment
      double maxd = clipper.maxClippedDistance (p0, p1, side);
      double maxdChk = ref.maxClippedDistance (p0, p1, side);
      checkEquals (msg+": maxClippedDistance", maxd, maxdChk, TOL);
      return true;
   }

   /**
    * Compares a computed maximum clipped distance {@code maxd} against an
    * expected value {@code chk}. Disagreements about whether the region is
    * intersected at all are ignored if the intersection is within tolerance of
    * being empty, since these are ill-conditioned.
    */
   private void checkMaxDistance (String msg, double maxd, double chk) {
      if ((maxd < 0) != (chk < 0)) {
         if (Math.max (maxd, chk) > MIN_INTERVAL) {
            throw new TestException (
               msg+" returns "+maxd+", expected "+chk);
         }
      }
      else {
         checkEquals (msg, maxd, chk, TOL);
      }
   }

   /**
    * Checks the triangle based {@code maxClippedDistance()} against the exact
    * reference implementation, and verifies that it bounds the edge based
    * estimate from above.
    */
   private void checkMaxTriangleDistance (
      TriFaceClipper clipper, RefRegion ref,
      Point3d p0, Point3d p1, Point3d p2, int side, String msg) {

      double maxdChk = ref.exactMaxClippedDistance (p0, p1, p2, side);
      double maxd = clipper.maxClippedDistance (p0, p1, p2, side);
      checkMaxDistance (msg+": maxClippedDistance(tri)", maxd, maxdChk);
      // the edge based estimate is a lower bound on the exact value
      double maxdEdge = ref.maxClippedDistance (p0, p1, p2, side);
      check (
         msg+": edge based estimate "+maxdEdge+
         " exceeds exact value "+maxd,
         maxdEdge <= maxd + TOL);
   }

   /**
    * Tests the region predicate used by the reference implementation against
    * points whose location is known by inspection, and checks that the clipper
    * agrees about whether or not a point lies inside the region.
    */
   public void testRegionPredicate() {
      Point3d[] tri = canonicalTriangle();
      // points inside the triangle, points outside it, and the associated
      // check values
      Point3d[] pnts = new Point3d[] {
         new Point3d (0.25, 0.25, 0),   // inside, on the face
         new Point3d (0.0, 0.0, 0),     // inside, at a corner
         new Point3d (0.5, 0.5, 0),     // inside, on an edge
         new Point3d (2.0, 2.0, 0),     // outside, beyond edge 1
         new Point3d (-0.1, 0.5, 0),    // outside, beyond edge 2
         new Point3d (0.5, -0.1, 0),    // outside, beyond edge 0
      };
      boolean[] insideChk = new boolean[] {
         true, true, true, false, false, false };
      // points which lie exactly on one of the edge planes; their
      // classification is ambiguous once round-off is introduced by a
      // transform
      boolean[] onEdgeBoundary = new boolean[] {
         false, true, true, false, false, false };

      RigidTransform3d T = new RigidTransform3d();
      for (int k=0; k<4; k++) {
         if (k > 0) {
            T.setRandom();
         }
         Point3d[] xtri = transform (tri, T);
         RefRegion ref = new RefRegion (xtri[0], xtri[1], xtri[2]);
         TriFaceClipper clipper = new TriFaceClipper (xtri[0], xtri[1], xtri[2]);
         Vector3d nrm = new Vector3d();
         nrm.set (0, 0, 1);
         nrm.transform (T);
         checkEquals ("face normal", ref.myNrm, nrm, EPS);

         for (int i=0; i<pnts.length; i++) {
            for (double h : new double[] { -1.0, -0.25, 0, 0.25, 1.0 }) {
               if (k > 0 && (onEdgeBoundary[i] || (insideChk[i] && h == 0))) {
                  // point lies exactly on the region boundary, so skip it
                  // when the configuration has been transformed
                  continue;
               }
               Point3d p = new Point3d (pnts[i]);
               p.scaledAdd (h, new Vector3d (0, 0, 1));
               p.transform (T);
               for (int side : new int[] { 1, -1 }) {
                  boolean chk = insideChk[i] && (side*h >= 0);
                  boolean res = ref.isInRegion (p, side, EPS);
                  if (res != chk) {
                     throw new TestException (
                        "isInRegion() for point "+pnts[i]+" at height "+h+
                        ", side "+side+" returns "+res+", expected "+chk);
                  }
                  // the clipper should agree, as tested using a degenerate
                  // segment
                  boolean clipRes = clipper.clipSegment (
                     null, null, null, p, p, side);
                  if (clipRes != chk) {
                     throw new TestException (
                        "clipSegment() for degenerate segment at "+pnts[i]+
                        ", height "+h+", side "+side+" returns "+clipRes+
                        ", expected "+chk);
                  }
               }
            }
         }
      }
   }

   /**
    * Tests a single clipping case whose result is known by inspection. The
    * test is repeated with the whole configuration transformed by random rigid
    * transforms, since clipping results should be invariant under these.
    *
    * @param msg identifies the test
    * @param tri points of the clipping face
    * @param p0 first segment point
    * @param p1 second segment point
    * @param side region to clip to
    * @param svalsChk expected parameter interval, or {@code [1,0]} if the
    * segment is expected to be completely clipped
    * @param maxdChk expected maximum clipped distance
    */
   private void testCase (
      String msg, Point3d[] tri, Point3d p0, Point3d p1, int side,
      double[] svalsChk, double maxdChk) {
      testCase (msg, tri, p0, p1, side, svalsChk, maxdChk, /*numTrans=*/4);
   }

   /**
    * Tests a single clipping case whose result is known by inspection, as
    * described for {@link #testCase}. {@code numTrans} specifies the number of
    * random transforms which should be applied to the configuration. This
    * should be set to 0 for configurations in which the segment touches the
    * boundary of the region exactly, since the classification of such
    * configurations is ambiguous once round-off is introduced.
    */
   private void testCase (
      String msg, Point3d[] tri, Point3d p0, Point3d p1, int side,
      double[] svalsChk, double maxdChk, int numTrans) {

      RigidTransform3d T = new RigidTransform3d();
      for (int k=0; k<=numTrans; k++) {
         if (k > 0) {
            T.setRandom();
         }
         Point3d[] xtri = transform (tri, T);
         Point3d[] xseg = transform (new Point3d[] { p0, p1 }, T);
         RefRegion ref = new RefRegion (xtri[0], xtri[1], xtri[2]);
         TriFaceClipper clipper = new TriFaceClipper (xtri[0], xtri[1], xtri[2]);

         boolean emptyChk = (svalsChk[0] > svalsChk[1]);

         double[] svals = new double[2];
         Point3d ps0 = new Point3d();
         Point3d ps1 = new Point3d();
         boolean res = clipper.clipSegment (
            svals, ps0, ps1, xseg[0], xseg[1], side);
         if (res == emptyChk) {
            throw new TestException (
               msg + ": clipSegment() returned " + res + ", expected " + !res);
         }
         checkEquals (msg+": svals", svals, svalsChk, TOL);
         if (res) {
            checkEquals (
               msg+": ps0", ps0,
               pointOnSegment (xseg[0], xseg[1], svalsChk[0]), TOL);
            checkEquals (
               msg+": ps1", ps1,
               pointOnSegment (xseg[0], xseg[1], svalsChk[1]), TOL);
         }
         checkEquals (
            msg+": maxClippedDistance",
            clipper.maxClippedDistance (xseg[0], xseg[1], side), maxdChk, TOL);

         // the reference implementation should agree
         checkEquals (
            msg+": reference svals",
            ref.clipInterval (xseg[0], xseg[1], side), svalsChk, TOL);
         checkEquals (
            msg+": reference maxClippedDistance",
            ref.maxClippedDistance (xseg[0], xseg[1], side), maxdChk, TOL);
         checkRefInterval (ref, xseg[0], xseg[1], side, msg);
      }
   }

   /**
    * Tests clipping for cases whose results are known by inspection.
    */
   public void testSpecificCases() {
      Point3d[] tri = canonicalTriangle();

      double[] noClip = new double[] { 0, 1 };
      double[] allClip = new double[] { 1, 0 };

      // segment entirely inside the upper region
      testCase (
         "inside upper", tri,
         new Point3d (0.2, 0.2, 1), new Point3d (0.3, 0.3, 2), 1, noClip, 2.0);
      // and hence entirely outside the lower region
      testCase (
         "inside upper, lower test", tri,
         new Point3d (0.2, 0.2, 1), new Point3d (0.3, 0.3, 2), -1,
         allClip, -1);
      // segment entirely inside the lower region
      testCase (
         "inside lower", tri,
         new Point3d (0.2, 0.2, -1), new Point3d (0.3, 0.3, -2), -1,
         noClip, 2.0);

      // segment crossing the face plane
      testCase (
         "crossing face plane, upper", tri,
         new Point3d (0.25, 0.25, -1), new Point3d (0.25, 0.25, 3), 1,
         new double[] { 0.25, 1 }, 3.0);
      testCase (
         "crossing face plane, lower", tri,
         new Point3d (0.25, 0.25, -1), new Point3d (0.25, 0.25, 3), -1,
         new double[] { 0, 0.25 }, 1.0);

      // segment crossing two of the edge planes
      testCase (
         "crossing edge planes 1 and 2", tri,
         new Point3d (-1, 0.25, 1), new Point3d (1, 0.25, 1), 1,
         new double[] { 0.5, 0.875 }, 1.0);
      // same segment lying in the face plane: should be clipped identically
      // for both sides, with zero distance
      testCase (
         "in face plane, upper", tri,
         new Point3d (-1, 0.25, 0), new Point3d (1, 0.25, 0), 1,
         new double[] { 0.5, 0.875 }, 0, /*numTrans=*/0);
      testCase (
         "in face plane, lower", tri,
         new Point3d (-1, 0.25, 0), new Point3d (1, 0.25, 0), -1,
         new double[] { 0.5, 0.875 }, 0, /*numTrans=*/0);

      // segment exiting through edge plane 0
      testCase (
         "crossing edge plane 0", tri,
         new Point3d (0.25, 0.25, 1), new Point3d (0.25, -1, 1), 1,
         new double[] { 0, 0.2 }, 1.0);

      // segment entirely outside the region, laterally
      testCase (
         "outside laterally", tri,
         new Point3d (2, 2, 1), new Point3d (3, 3, 1), 1, allClip, -1);

      // degenerate (zero length) segments
      testCase (
         "zero length inside", tri,
         new Point3d (0.25, 0.25, 0.5), new Point3d (0.25, 0.25, 0.5), 1,
         noClip, 0.5);
      testCase (
         "zero length outside", tri,
         new Point3d (0.25, 0.25, 0.5), new Point3d (0.25, 0.25, 0.5), -1,
         allClip, -1);

      // segments which just touch the face plane, with the rest of the
      // segment lying in the opposite region. These should clip to a single
      // point, regardless of the order of the end points.
      testCase (
         "touching face plane at p0", tri,
         new Point3d (0.25, 0.25, 0), new Point3d (0.25, 0.25, -1), 1,
         new double[] { 0, 0 }, 0, /*numTrans=*/0);
      testCase (
         "touching face plane at p1", tri,
         new Point3d (0.25, 0.25, -1), new Point3d (0.25, 0.25, 0), 1,
         new double[] { 1, 1 }, 0, /*numTrans=*/0);
      testCase (
         "touching face plane at p0, lower", tri,
         new Point3d (0.25, 0.25, 0), new Point3d (0.25, 0.25, 1), -1,
         new double[] { 0, 0 }, 0, /*numTrans=*/0);

      // segment which just touches an edge plane
      testCase (
         "touching edge plane 0", tri,
         new Point3d (0.5, 0, 1), new Point3d (0.5, -1, 1), 1,
         new double[] { 0, 0 }, 1.0, /*numTrans=*/0);

      // segment passing exactly through a triangle vertex, from outside
      testCase (
         "touching vertex", tri,
         new Point3d (-1, 1, 1), new Point3d (1, -1, 1), 1,
         new double[] { 0.5, 0.5 }, 1.0, /*numTrans=*/0);

      // triangle based tests
      RefRegion ref = new RefRegion (tri[0], tri[1], tri[2]);
      TriFaceClipper clipper = new TriFaceClipper (tri[0], tri[1], tri[2]);

      // triangle entirely inside the upper region
      checkEquals (
         "maxClippedDistance for enclosed triangle",
         clipper.maxClippedDistance (
            new Point3d (0.1, 0.1, 1), new Point3d (0.2, 0.1, 2),
            new Point3d (0.1, 0.2, 3), 1), 3.0, TOL);
      // same triangle, lower region
      checkEquals (
         "maxClippedDistance for enclosed triangle, lower",
         clipper.maxClippedDistance (
            new Point3d (0.1, 0.1, 1), new Point3d (0.2, 0.1, 2),
            new Point3d (0.1, 0.2, 3), -1), -1.0, TOL);
      // triangle straddling the face plane
      checkEquals (
         "maxClippedDistance for straddling triangle",
         clipper.maxClippedDistance (
            new Point3d (0.1, 0.1, -1), new Point3d (0.2, 0.1, 2),
            new Point3d (0.1, 0.2, 3), 1), 3.0, TOL);
      checkMaxTriangleDistance (
         clipper, ref,
         new Point3d (0.1, 0.1, -1), new Point3d (0.2, 0.1, 2),
         new Point3d (0.1, 0.2, 3), 1, "straddling triangle");

      // A triangle whose interior covers the region but whose edges lie
      // outside it. Since the triangle is clipped as an area, this is handled
      // exactly, even though none of the edges enter the region.
      Point3d[] covering = new Point3d[] {
         new Point3d (-10, -10, 1), new Point3d (10, -10, 1),
         new Point3d (0, 10, 1) };
      checkEquals (
         "maxClippedDistance for triangle covering the region",
         clipper.maxClippedDistance (
            covering[0], covering[1], covering[2], 1), 1.0, TOL);
      checkEquals (
         "edge based estimate for triangle covering the region",
         ref.maxClippedDistance (
            covering[0], covering[1], covering[2], 1), -1.0, TOL);
      checkMaxTriangleDistance (
         clipper, ref, covering[0], covering[1], covering[2], 1, "covering");
   }

   /**
    * Tests clipping for randomly generated triangles and segments.
    */
   public void testRandom() {
      int numTested = 0;
      int numClipped = 0;
      int numPartial = 0;

      for (int i=0; i<2000; i++) {
         Point3d[] tri = new Point3d[3];
         for (int j=0; j<3; j++) {
            tri[j] = new Point3d();
            tri[j].setRandom (-1, 1);
         }
         // reject nearly degenerate triangles
         Vector3d u = new Vector3d();
         Vector3d v = new Vector3d();
         Vector3d xprod = new Vector3d();
         u.sub (tri[1], tri[0]);
         v.sub (tri[2], tri[0]);
         xprod.cross (u, v);
         if (xprod.norm() < 0.1) {
            continue;
         }
         RefRegion ref = new RefRegion (tri[0], tri[1], tri[2]);
         TriFaceClipper clipper = new TriFaceClipper (tri[0], tri[1], tri[2]);
         // check that set() gives the same result as the constructor
         TriFaceClipper clipperx = new TriFaceClipper();
         clipperx.set (tri[0], tri[1], tri[2]);

         Point3d p0 = new Point3d();
         Point3d p1 = new Point3d();
         Point3d p2 = new Point3d();
         p0.setRandom (-2, 2);
         p1.setRandom (-2, 2);
         p2.setRandom (-2, 2);

         for (int side : new int[] { 1, -1 }) {
            String msg = "random test " + i + ", side " + side;
            if (checkClipSegment (clipper, ref, p0, p1, side, msg)) {
               numTested++;
               double[] chk = ref.clipInterval (p0, p1, side);
               if (chk[0] > chk[1]) {
                  numClipped++;
               }
               else if (chk[0] != 0 || chk[1] != 1) {
                  numPartial++;
               }
            }
            checkClipSegment (clipperx, ref, p0, p1, side, msg+" (set)");
            checkMaxTriangleDistance (clipper, ref, p0, p1, p2, side, msg);
         }
      }
      // make sure the tests covered a reasonable variety of situations
      check ("random tests: too few cases tested", numTested > 1000);
      check ("random tests: too few completely clipped cases", numClipped > 50);
      check ("random tests: too few partially clipped cases", numPartial > 50);
   }

   /**
    * Tests error handling.
    */
   public void testErrors() {
      Point3d[] tri = canonicalTriangle();

      // methods should fail if the clipper is uninitialized
      final TriFaceClipper clipper = new TriFaceClipper();
      checkForException (
         new ImproperStateException(), new ExceptionTest() {
            public void run() {
               clipper.clipSegment (
                  null, null, null,
                  new Point3d(0,0,0), new Point3d(1,1,1), 1);
            }
         });
      checkForException (
         new ImproperStateException(), new ExceptionTest() {
            public void run() {
               clipper.maxClippedDistance (
                  new Point3d(0,0,0), new Point3d(1,1,1), 1);
            }
         });
      checkForException (
         new ImproperStateException(), new ExceptionTest() {
            public void run() {
               clipper.maxClippedDistance (
                  new Point3d(0,0,0), new Point3d(1,1,1),
                  new Point3d(1,0,1), 1);
            }
         });

      // degenerate triangles should be rejected: set() should return false
      // and leave the clipper uninitialized
      Point3d[][] degenerateTris = new Point3d[][] {
         // collinear points
         { new Point3d(0,0,0), new Point3d(1,0,0), new Point3d(2,0,0) },
         // repeated points
         { new Point3d(1,2,3), new Point3d(1,2,3), new Point3d(4,5,6) },
         { new Point3d(1,2,3), new Point3d(4,5,6), new Point3d(1,2,3) },
         // all points the same
         { new Point3d(1,2,3), new Point3d(1,2,3), new Point3d(1,2,3) },
      };
      for (Point3d[] dtri : degenerateTris) {
         final TriFaceClipper dclipper = new TriFaceClipper();
         // initialize to a proper triangle first, to make sure that a failed
         // set() clears the initialization
         dclipper.set (tri[0], tri[1], tri[2]);
         check ("clipper should be initialized", dclipper.isInitialized());
         check (
            "set() should return false for degenerate triangle",
            dclipper.set (dtri[0], dtri[1], dtri[2]) == false);
         check (
            "clipper should be uninitialized after degenerate set()",
            !dclipper.isInitialized());
         checkForException (
            new ImproperStateException(), new ExceptionTest() {
               public void run() {
                  dclipper.maxClippedDistance (
                     new Point3d(0,0,0), new Point3d(1,1,1), 1);
               }
            });
         // constructing from a degenerate triangle should do the same
         check (
            "constructed clipper should be uninitialized",
            !(new TriFaceClipper (dtri[0], dtri[1], dtri[2])).isInitialized());
      }

      // side must be non-zero
      final TriFaceClipper tclipper =
         new TriFaceClipper (tri[0], tri[1], tri[2]);
      checkForIllegalArgumentException (new ExceptionTest() {
         public void run() {
            tclipper.clipSegment (
               null, null, null,
               new Point3d(0.25,0.25,1), new Point3d(0.25,0.25,2), 0);
         }
      });
      checkForIllegalArgumentException (new ExceptionTest() {
         public void run() {
            tclipper.maxClippedDistance (
               new Point3d(0.25,0.25,1), new Point3d(0.25,0.25,2), 0);
         }
      });
      checkForIllegalArgumentException (new ExceptionTest() {
         public void run() {
            tclipper.maxClippedDistance (
               new Point3d(0.25,0.25,1), new Point3d(0.25,0.25,2),
               new Point3d(0.5,0.25,2), 0);
         }
      });
      final Face triFace =
         createTriangleMesh (tri[0], tri[1], tri[2]).getFace(0);
      checkForIllegalArgumentException (new ExceptionTest() {
         public void run() {
            tclipper.maxClippedDistance (triFace, 0, false);
         }
      });
      checkForIllegalArgumentException (new ExceptionTest() {
         public void run() {
            tclipper.maxClippedDistance (triFace.firstHalfEdge(), 0);
         }
      });

      // non-triangular faces should be rejected
      PolygonalMesh mesh = new PolygonalMesh();
      mesh.addVertex (new Point3d (0, 0, 0));
      mesh.addVertex (new Point3d (1, 0, 0));
      mesh.addVertex (new Point3d (1, 1, 0));
      mesh.addVertex (new Point3d (0, 1, 0));
      final Face quad = mesh.addFace (new int[] { 0, 1, 2, 3 });
      checkForIllegalArgumentException (new ExceptionTest() {
         public void run() {
            new TriFaceClipper (quad);
         }
      });
      checkForIllegalArgumentException (new ExceptionTest() {
         public void run() {
            tclipper.maxClippedDistance (quad, 1, false);
         }
      });
   }

   /**
    * Creates a mesh containing a single triangle.
    */
   private PolygonalMesh createTriangleMesh (Point3d p0, Point3d p1, Point3d p2) {
      PolygonalMesh mesh = new PolygonalMesh();
      mesh.addVertex (new Point3d (p0));
      mesh.addVertex (new Point3d (p1));
      mesh.addVertex (new Point3d (p2));
      mesh.addFace (new int[] { 0, 1, 2 });
      return mesh;
   }

   /**
    * Returns the world coordinate points of the vertices of a triangular
    * face, starting with the tail of its first half edge.
    */
   private Point3d[] getFacePoints (Face face) {
      Point3d[] pnts = new Point3d[3];
      HalfEdge he = face.firstHalfEdge();
      for (int i=0; i<3; i++) {
         pnts[i] = new Point3d (he.getTail().getWorldPoint());
         he = he.getNext();
      }
      return pnts;
   }

   /**
    * Tests the methods which take {@link Face} and {@link HalfEdge}
    * arguments, including the use of world coordinates.
    */
   public void testFaceMethods() {
      Point3d[] tri = canonicalTriangle();

      RigidTransform3d TFW = new RigidTransform3d(); // face mesh to world
      RigidTransform3d TTW = new RigidTransform3d(); // test mesh to world

      for (int k=0; k<5; k++) {
         if (k > 0) {
            TFW.setRandom();
            TTW.setRandom();
         }
         PolygonalMesh faceMesh = createTriangleMesh (tri[0], tri[1], tri[2]);
         faceMesh.setMeshToWorld (TFW);
         Face face = faceMesh.getFace(0);
         Point3d[] xtri = getFacePoints (face);

         TriFaceClipper clipper = new TriFaceClipper (face);
         TriFaceClipper chkClipper =
            new TriFaceClipper (xtri[0], xtri[1], xtri[2]);
         RefRegion ref = new RefRegion (xtri[0], xtri[1], xtri[2]);

         // create a test triangle in a separate mesh, with its own
         // mesh-to-world transform
         Point3d[] tpnts = new Point3d[] {
            new Point3d (0.2, 0.1, 1.0),
            new Point3d (1.5, 0.1, -0.5),
            new Point3d (0.1, 1.2, 0.5) };
         PolygonalMesh testMesh =
            createTriangleMesh (tpnts[0], tpnts[1], tpnts[2]);
         testMesh.setMeshToWorld (TTW);
         Face testFace = testMesh.getFace(0);
         Point3d[] xtpnts = getFacePoints (testFace);

         for (int side : new int[] { 1, -1 }) {
            String msg = "face test " + k + ", side " + side;
            // clipping against a face should equal clipping against its world
            // coordinate points
            checkClipSegment (
               clipper, ref, xtpnts[0], xtpnts[1], side, msg);

            // half edge method should agree with the segment method
            HalfEdge he = testFace.firstHalfEdge();
            double maxd = -1;
            for (int i=0; i<3; i++) {
               Point3d pa = new Point3d (he.getTail().getWorldPoint());
               Point3d pb = new Point3d (he.getHead().getWorldPoint());
               double d = clipper.maxClippedDistance (he, side);
               checkEquals (
                  msg+": maxClippedDistance(he)", d,
                  clipper.maxClippedDistance (pa, pb, side), TOL);
               checkEquals (
                  msg+": maxClippedDistance(he)", d,
                  ref.maxClippedDistance (pa, pb, side), TOL);
               maxd = Math.max (maxd, d);
               he = he.getNext();
            }
            // the edge based reference should equal the maximum over the
            // edges
            checkEquals (
               msg+": reference maxClippedDistance",
               ref.maxClippedDistance (
                  xtpnts[0], xtpnts[1], xtpnts[2], side), maxd, TOL);
            // the point based method clips the triangle as an area, and so
            // bounds the edge based maximum from above
            checkMaxTriangleDistance (
               clipper, ref, xtpnts[0], xtpnts[1], xtpnts[2], side, msg);
            double maxdTri = clipper.maxClippedDistance (
               xtpnts[0], xtpnts[1], xtpnts[2], side);
            check (
               msg+": maxClippedDistance(pnts) below the edge based maximum",
               maxdTri >= maxd - TOL);
            // the face method should give the same result as the point based
            // method applied to the face's world coordinate points
            checkMaxDistance (
               msg+": maxClippedDistance(face)",
               clipper.maxClippedDistance (testFace, side, false), maxdTri);
            checkMaxDistance (
               msg+": maxClippedDistance(face)",
               chkClipper.maxClippedDistance (testFace, side, false), maxdTri);
         }
      }
   }

   /**
    * Returns the maximum clipped distance over the edges of {@code tri}. This
    * is a lower bound on the maximum for the triangle as a whole.
    */
   private double maxEdgeDistance (
      TriFaceClipper clipper, Face tri, int side) {
      double maxd = -1;
      HalfEdge he = tri.firstHalfEdge();
      for (int i=0; i<3; i++) {
         maxd = Math.max (maxd, clipper.maxClippedDistance (he, side));
         he = he.getNext();
      }
      return maxd;
   }

   /**
    * Returns the index, with respect to the first half edge, of the half edge
    * of {@code tri} whose opposite face is {@code prev}, or -1 if there is no
    * such half edge.
    */
   private int indexOfSharedEdge (Face tri, Face prev) {
      HalfEdge he = tri.firstHalfEdge();
      for (int i=0; i<3; i++) {
         if (he.getOppositeFace() == prev) {
            return i;
         }
         he = he.getNext();
      }
      return -1;
   }

   /**
    * Returns the index, with respect to the first half edge, of {@code vtx}
    * within the vertices of {@code tri}, or -1 if {@code vtx} is not a vertex
    * of {@code tri}.
    */
   private int indexOfVertex (Face tri, Vertex3d vtx) {
      HalfEdge he = tri.firstHalfEdge();
      for (int i=0; i<3; i++) {
         if (he.getTail() == vtx) {
            return i;
         }
         he = he.getNext();
      }
      return -1;
   }

   /**
    * Returns the vertex of {@code tri} at index {@code idx} with respect to
    * the tail of its first half edge.
    */
   private Vertex3d getVertex (Face tri, int idx) {
      HalfEdge he = tri.firstHalfEdge();
      for (int i=0; i<idx; i++) {
         he = he.getNext();
      }
      return he.getTail();
   }

   /**
    * Rotates the entries of an index list by {@code r} places. The resulting
    * face is the same, but its first half edge is different.
    */
   private int[] rotate (int[] idxs, int r) {
      int[] res = new int[idxs.length];
      for (int i=0; i<idxs.length; i++) {
         res[i] = idxs[(i+r)%idxs.length];
      }
      return res;
   }

   // face most recently passed to maxClippedDistance(Face,int,boolean), and
   // hence the face against which the clipper will test for adjacency
   private Face myPrevFace;

   // statistics for the adjacency tests
   private int myNumAdjacentTests;
   private int myNumNonTrivialTests;

   /**
    * Checks {@code maxClippedDistance(Face,int,boolean)} for the triangle
    * {@code tri}, whose result should be the exact maximum distance for the
    * triangle regardless of whether coordinates are reused from the previously
    * tested triangle. {@code chkClipper} is a separate clipper, set to the
    * same face, which is used for the cross checks so that the reuse state of
    * {@code clipper} is not disturbed. If {@code tri} is adjacent to the
    * previously tested triangle, the relative placement of the shared edge is
    * recorded in {@code configs}. Returns the computed distance.
    */
   private double checkFaceDistance (
      TriFaceClipper clipper, TriFaceClipper chkClipper, RefRegion ref,
      Face tri, int side, boolean checkAdjacent,
      HashSet<String> configs, String msg) {

      boolean adjacent = false;
      if (checkAdjacent && myPrevFace != null) {
         int k = indexOfSharedEdge (tri, myPrevFace);
         if (k != -1) {
            adjacent = true;
            // record the index of the shared edge within tri, together with
            // the index, within the previous triangle, of the tail vertex of
            // that edge
            int j = indexOfVertex (myPrevFace, getVertex (tri, k));
            check (
               msg+": shared vertex not found in previous triangle", j != -1);
            if (configs != null) {
               configs.add (k+","+j);
            }
         }
      }
      double maxd = clipper.maxClippedDistance (tri, side, checkAdjacent);
      myPrevFace = tri;

      // reusing coordinates must not change the result
      checkMaxDistance (
         msg+": maxClippedDistance(face) without reuse",
         chkClipper.maxClippedDistance (tri, side, false), maxd);
      // and the result should be the exact distance for the triangle
      Point3d[] xpnts = getFacePoints (tri);
      checkMaxDistance (
         msg+": maxClippedDistance(face)", maxd,
         ref.exactMaxClippedDistance (xpnts[0], xpnts[1], xpnts[2], side));
      // which bounds the maximum over the edges from above
      check (
         msg+": maxClippedDistance(face) below the edge based maximum",
         maxd >= maxEdgeDistance (chkClipper, tri, side) - TOL);

      if (adjacent) {
         myNumAdjacentTests++;
         if (maxd != -1) {
            myNumNonTrivialTests++;
         }
      }
      return maxd;
   }

   /**
    * Creates a mesh vertex at a random location in the vicinity of the
    * canonical triangle.
    */
   private void addRandomVertex (PolygonalMesh mesh) {
      Point3d p = new Point3d();
      p.setRandom (-0.5, 1.5);
      p.z = RandomGenerator.nextDouble (-1.0, 1.0);
      mesh.addVertex (p);
   }

   /**
    * Tests the coordinate reuse optimization of {@code
    * maxClippedDistance(Face,int,boolean)}.
    */
   public void testAdjacency() {
      Point3d[] tri = canonicalTriangle();
      TriFaceClipper clipper = new TriFaceClipper (tri[0], tri[1], tri[2]);
      TriFaceClipper chkClipper = new TriFaceClipper (tri[0], tri[1], tri[2]);
      RefRegion ref = new RefRegion (tri[0], tri[1], tri[2]);
      myPrevFace = null;

      HashSet<String> configs = new HashSet<>();
      myNumAdjacentTests = 0;
      myNumNonTrivialTests = 0;

      // Pairs of adjacent triangles, with the vertex ordering of each triangle
      // rotated in all possible ways so as to exercise all the relative
      // placements of the shared edge.
      for (int r0=0; r0<3; r0++) {
         for (int r1=0; r1<3; r1++) {
            for (int cnt=0; cnt<8; cnt++) {
               PolygonalMesh mesh = new PolygonalMesh();
               for (int i=0; i<4; i++) {
                  addRandomVertex (mesh);
               }
               // triangles (0,1,2) and (1,0,3) share the edge between
               // vertices 0 and 1
               Face f0 = mesh.addFace (rotate (new int[] {0, 1, 2}, r0));
               Face f1 = mesh.addFace (rotate (new int[] {1, 0, 3}, r1));

               for (int side : new int[] { 1, -1 }) {
                  String msg =
                     "adjacency test ("+r0+","+r1+","+cnt+"), side "+side;
                  checkFaceDistance (
                     clipper, chkClipper, ref, f0, side, true, configs,
                     msg+", first triangle");
                  // f1 is adjacent to f0, so the coordinates of the
                  // vertices of the shared edge should be reused
                  checkFaceDistance (
                     clipper, chkClipper, ref, f1, side, true, configs,
                     msg+", second triangle");
                  check (
                     msg+": triangles are not adjacent",
                     indexOfSharedEdge (f1, f0) != -1);
                  // repeating the test with checkAdjacent=false should use all
                  // three edges
                  checkFaceDistance (
                     clipper, chkClipper, ref, f1, side, false, configs,
                     msg+", checkAdjacent=false");
               }
            }
         }
      }
      checkEquals (
         "number of shared edge placements tested", configs.size(), 9);

      // Test a chain of adjacent triangles, arranged as a fan about a central
      // vertex. Each triangle is adjacent to its predecessor, so this checks
      // that the vertex coordinates saved by each call are correctly matched
      // to the vertices of the next one.
      int numTris = 8;
      for (int cnt=0; cnt<10; cnt++) {
         PolygonalMesh mesh = new PolygonalMesh();
         addRandomVertex (mesh); // central vertex
         for (int i=0; i<=numTris; i++) {
            double ang = 2*Math.PI*i/(double)(numTris+1);
            mesh.addVertex (
               new Point3d (
                  0.3 + 0.8*Math.cos(ang), 0.3 + 0.8*Math.sin(ang),
                  RandomGenerator.nextDouble (-1.0, 1.0)));
         }
         ArrayList<Face> faces = new ArrayList<>();
         for (int i=0; i<numTris; i++) {
            // rotate the index list so that the shared edge appears at
            // different places within each face
            faces.add (mesh.addFace (rotate (new int[] {0, i+1, i+2}, i%3)));
         }
         for (int side : new int[] { 1, -1 }) {
            String msg = "fan test "+cnt+", side "+side;
            myPrevFace = null; // start the chain fresh
            for (int i=0; i<numTris; i++) {
               checkFaceDistance (
                  clipper, chkClipper, ref, faces.get(i), side, true, configs,
                  msg+", triangle "+i);
               if (i > 0) {
                  check (
                     msg+": triangles "+(i-1)+" and "+i+" are not adjacent",
                     indexOfSharedEdge (faces.get(i), faces.get(i-1)) != -1);
               }
            }
         }
      }

      // Check that a triangle which is not adjacent to the previously tested
      // triangle falls through to the general case.
      for (int cnt=0; cnt<10; cnt++) {
         PolygonalMesh mesh = new PolygonalMesh();
         for (int i=0; i<6; i++) {
            addRandomVertex (mesh);
         }
         Face f0 = mesh.addFace (new int[] {0, 1, 2});
         Face f1 = mesh.addFace (new int[] {3, 4, 5});
         check (
            "test faces should not be adjacent",
            indexOfSharedEdge (f1, f0) == -1);
         for (int side : new int[] { 1, -1 }) {
            String msg = "non-adjacent test "+cnt+", side "+side;
            checkFaceDistance (
               clipper, chkClipper, ref, f0, side, true, configs, msg);
            checkFaceDistance (
               clipper, chkClipper, ref, f1, side, true, configs, msg);
         }
      }

      check (
         "adjacency tests: too few cases with a non-empty intersection",
         myNumNonTrivialTests > myNumAdjacentTests/4);

      testCoordinateReuseInvalidation();
   }

   /**
    * Checks that saved vertex coordinates are not reused when they are no
    * longer valid: either because the clipper has been set to a different
    * face, or because the vertex positions have changed and the caller has
    * indicated this by setting {@code checkAdjacent} to {@code false}.
    */
   private void testCoordinateReuseInvalidation() {
      Point3d[] triA = canonicalTriangle();
      // a second face, with a different position and orientation, so that
      // coordinates computed for one are meaningless for the other
      Point3d[] triB = new Point3d[] {
         new Point3d (0.3, 0.2, 0.4),
         new Point3d (0.3, 1.1, -0.2),
         new Point3d (1.2, 0.2, 0.7) };

      for (int cnt=0; cnt<20; cnt++) {
         PolygonalMesh mesh = new PolygonalMesh();
         for (int i=0; i<3; i++) {
            addRandomVertex (mesh);
         }
         Face tri = mesh.addFace (new int[] {0, 1, 2});

         for (int side : new int[] { 1, -1 }) {
            String msg = "reuse invalidation test "+cnt+", side "+side;

            // Setting the clipper to a new face must discard everything left
            // over from the old one. The clipper is used several times first,
            // so that all of its point buffers hold stale values.
            TriFaceClipper clipper =
               new TriFaceClipper (triA[0], triA[1], triA[2]);
            for (int i=0; i<4; i++) {
               clipper.maxClippedDistance (tri, side, true);
            }
            clipper.set (triB[0], triB[1], triB[2]);
            TriFaceClipper chkClipper =
               new TriFaceClipper (triB[0], triB[1], triB[2]);
            checkMaxDistance (
               msg+": maxClippedDistance(face) after set()",
               clipper.maxClippedDistance (tri, side, true),
               chkClipper.maxClippedDistance (tri, side, false));

            // Moving a vertex and then requesting no reuse must give the same
            // result as a clipper which has no saved coordinates at all.
            clipper.maxClippedDistance (tri, side, true);
            Vertex3d vtx = tri.firstHalfEdge().getTail();
            Point3d pos = new Point3d (vtx.getPosition());
            pos.setRandom (-0.5, 1.5);
            vtx.setPosition (pos);
            checkMaxDistance (
               msg+": maxClippedDistance(face) after moving a vertex",
               clipper.maxClippedDistance (tri, side, false),
               new TriFaceClipper (
                  triB[0], triB[1], triB[2]).maxClippedDistance (
                     tri, side, false));
         }
      }
   }

   /**
    * Tests the triangle based {@code maxClippedDistance()} on triangles which
    * are large enough to span the region without any of their edges entering
    * it. These are the cases which an edge based estimate misses, and so they
    * are checked explicitly here in addition to being covered by the random
    * tests.
    */
   public void testAreaClipping() {
      Point3d[] tri = canonicalTriangle();
      RefRegion ref = new RefRegion (tri[0], tri[1], tri[2]);
      TriFaceClipper clipper = new TriFaceClipper (tri[0], tri[1], tri[2]);

      // large horizontal triangle at a height of 2, covering the whole face
      Point3d p0 = new Point3d (-10, -10, 2);
      Point3d p1 = new Point3d ( 20, -10, 2);
      Point3d p2 = new Point3d (-10,  20, 2);
      checkEquals (
         "covering triangle: edge based estimate",
         ref.maxClippedDistance (p0, p1, p2, 1), -1.0);
      checkEquals (
         "covering triangle: maxClippedDistance(tri)",
         clipper.maxClippedDistance (p0, p1, p2, 1), 2.0, EPS);
      checkEquals (
         "covering triangle, lower region",
         clipper.maxClippedDistance (p0, p1, p2, -1), -1.0);

      // tilted covering triangle, with z = 1 + (x+10)/10. The maximum lies
      // above the face vertex (1,0,0), which is the region corner of greatest
      // height, and so is missed entirely by the edge based method.
      p0 = new Point3d (-10, -10, 1);
      p1 = new Point3d ( 20, -10, 4);
      p2 = new Point3d (-10,  20, 1);
      checkEquals (
         "tilted covering triangle: edge based estimate",
         ref.maxClippedDistance (p0, p1, p2, 1), -1.0);
      checkEquals (
         "tilted covering triangle: maxClippedDistance(tri)",
         clipper.maxClippedDistance (p0, p1, p2, 1), 1+11/10.0, EPS);
      checkMaxTriangleDistance (clipper, ref, p0, p1, p2, 1, "tilted");

      // triangle perpendicular to the face, which projects onto a line
      // segment in the plane of the face
      p0 = new Point3d (0.25, 0.25, -1);
      p1 = new Point3d (0.25, 0.25,  3);
      p2 = new Point3d (0.35, 0.35,  1);
      checkEquals (
         "perpendicular triangle: maxClippedDistance(tri)",
         clipper.maxClippedDistance (p0, p1, p2, 1), 3.0, EPS);
      checkMaxTriangleDistance (clipper, ref, p0, p1, p2, 1, "perpendicular");
      checkMaxTriangleDistance (clipper, ref, p0, p1, p2, -1, "perpendicular");

      // Triangle lying in the plane of the face and overlapping it. It lies
      // on the boundary of both regions, and so the distance is 0 for either
      // side.
      p0 = new Point3d (0.1, 0.1, 0);
      p1 = new Point3d (0.4, 0.1, 0);
      p2 = new Point3d (0.1, 0.4, 0);
      checkEquals (
         "coplanar triangle, upper region",
         clipper.maxClippedDistance (p0, p1, p2, 1), 0.0, EPS);
      checkEquals (
         "coplanar triangle, lower region",
         clipper.maxClippedDistance (p0, p1, p2, -1), 0.0, EPS);
      // a coplanar triangle which does not overlap the face is still rejected
      p0 = new Point3d (-1.0, -1.0, 0);
      p1 = new Point3d (-0.5, -1.0, 0);
      p2 = new Point3d (-1.0, -0.5, 0);
      checkEquals (
         "coplanar triangle outside the face",
         clipper.maxClippedDistance (p0, p1, p2, 1), -1.0);

      // triangle lying entirely within the upper region
      p0 = new Point3d (0.2, 0.2, 1);
      p1 = new Point3d (0.3, 0.2, 2);
      p2 = new Point3d (0.2, 0.3, 4);
      checkEquals (
         "contained triangle: maxClippedDistance(tri)",
         clipper.maxClippedDistance (p0, p1, p2, 1), 4.0, EPS);

      // Triangle with a single vertex lying exactly on the plane of edge 0
      // (the x axis), with the other two vertices outside it. The clipped
      // region is that vertex alone, and so points lying exactly on a
      // clipping plane must be retained.
      p0 = new Point3d (0.5,  0, 5);
      p1 = new Point3d (0.2, -1, 0.1);
      p2 = new Point3d (0.8, -1, 0.1);
      checkEquals (
         "vertex on edge plane: maxClippedDistance(tri)",
         clipper.maxClippedDistance (p0, p1, p2, 1), 5.0, EPS);
      checkMaxTriangleDistance (clipper, ref, p0, p1, p2, 1, "vertex on edge");

      // Same, but with the triangle also extending into the region, so that
      // the vertex on the plane is a proper polygon vertex rather than the
      // whole result.
      p1 = new Point3d (0.5, 0.4, 0.1);
      checkEquals (
         "vertex on edge plane 2: maxClippedDistance(tri)",
         clipper.maxClippedDistance (p0, p1, p2, 1), 5.0, EPS);
      checkMaxTriangleDistance (clipper, ref, p0, p1, p2, 1, "vertex on edge 2");

      // triangle lying entirely outside the region, off to one side
      p0 = new Point3d (5, 5, 1);
      p1 = new Point3d (6, 5, 1);
      p2 = new Point3d (5, 6, 1);
      checkEquals (
         "outside triangle: maxClippedDistance(tri)",
         clipper.maxClippedDistance (p0, p1, p2, 1), -1.0);

      // Random covering triangles, formed by scaling a triangle about the
      // centroid of the face until it covers the face. Results should be
      // invariant under a rigid transform applied to both triangles.
      int numCovering = 0;
      for (int i=0; i<500; i++) {
         Point3d[] ftri = new Point3d[3];
         Point3d[] qtri = new Point3d[3];
         for (int j=0; j<3; j++) {
            ftri[j] = new Point3d();
            ftri[j].setRandom (-1, 1);
            qtri[j] = new Point3d();
            qtri[j].setRandom (-1, 1);
         }
         Vector3d u = new Vector3d();
         Vector3d v = new Vector3d();
         Vector3d xprod = new Vector3d();
         u.sub (ftri[1], ftri[0]);
         v.sub (ftri[2], ftri[0]);
         xprod.cross (u, v);
         if (xprod.norm() < 0.1) {
            continue;
         }
         // expand the query triangle about the centroid of the face
         Point3d cent = new Point3d();
         cent.add (ftri[0], ftri[1]);
         cent.add (ftri[2]);
         cent.scale (1/3.0);
         for (int j=0; j<3; j++) {
            qtri[j].scale (10.0);
            qtri[j].add (cent);
         }
         RigidTransform3d T = new RigidTransform3d();
         T.setRandom();

         TriFaceClipper cl = new TriFaceClipper (ftri[0], ftri[1], ftri[2]);
         RefRegion rf = new RefRegion (ftri[0], ftri[1], ftri[2]);
         for (int side : new int[] { 1, -1 }) {
            String msg = "covering test "+i+", side "+side;
            double maxd = cl.maxClippedDistance (qtri[0], qtri[1], qtri[2], side);
            if (maxd > rf.maxClippedDistance (qtri[0], qtri[1], qtri[2], side)+TOL) {
               numCovering++;
            }
            checkMaxTriangleDistance (
               cl, rf, qtri[0], qtri[1], qtri[2], side, msg);

            // check invariance under a rigid transform
            Point3d[] ftriX = transform (ftri, T);
            Point3d[] qtriX = transform (qtri, T);
            TriFaceClipper clX =
               new TriFaceClipper (ftriX[0], ftriX[1], ftriX[2]);
            double maxdX =
               clX.maxClippedDistance (qtriX[0], qtriX[1], qtriX[2], side);
            checkEquals (msg+": transformed", maxdX, maxd, TOL);
         }
      }
      check (
         "covering tests: too few cases where the exact value exceeds the "+
         "edge based estimate", numCovering > 100);
   }

   public void test() {
      testRegionPredicate();
      testSpecificCases();
      testRandom();
      testAreaClipping();
      testFaceMethods();
      testAdjacency();
      testErrors();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      TriFaceClipperTest tester = new TriFaceClipperTest();
      tester.runtest();
   }
}
