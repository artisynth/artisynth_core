/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.util.*;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;
import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.PointList;
import artisynth.core.renderables.MeshCurve.Interpolation;

/**
 * Unit tests for MeshCurve.
 */
public class MeshCurveTest extends UnitTest {

   private static final double EPS = 1e-10;

   // marker locations (in mesh coordinates) for curves on a plane at z = 0
   private static final double[][] PLANE_PNTS = new double[][] {
      { -1.5, -1.0 },
      {  0.5, -1.2 },
      {  1.2,  0.7 },
      { -0.3,  1.4 },
      { -1.4,  0.2 },
   };

   /**
    * Creates a curve on a 4 x 4 plane at z = 0, with the mesh body located
    * at pose {@code TMW}.
    */
   MeshCurve createPlaneCurve (
      RigidTransform3d TMW, Interpolation interp, boolean closed, double res) {
      FixedMeshBody body =
         new FixedMeshBody (MeshFactory.createPlane (4.0, 4.0, 8, 8));
      body.setPose (TMW);
      MeshCurve curve = new MeshCurve (body);
      curve.setResolution (res);
      curve.setInterpolation (interp);
      curve.setClosed (closed);
      for (double[] xy : PLANE_PNTS) {
         Point3d pos = new Point3d (xy[0], xy[1], 0);
         pos.transform (TMW);
         curve.addMarker (pos);
      }
      return curve;
   }

   /**
    * Returns a point (in mesh coordinates) on a non-planar closed path on the
    * unit sphere, as a function of the parameter {@code j}, with period 5.
    */
   Point3d spherePoint (double j) {
      double a = 2*Math.PI*j/5;
      Point3d p = new Point3d (Math.cos(a), Math.sin(a), 0.3*Math.sin(2*a));
      p.normalize();
      return p;
   }

   /**
    * Creates a curve on a unit icosahedral sphere, with the mesh body located
    * at pose {@code TMW}, and markers located at {@code spherePoint(j)} for
    * each {@code j} in {@code idxs}.
    */
   MeshCurve createSphereCurve (
      RigidTransform3d TMW, Interpolation interp, boolean closed,
      double res, int... idxs) {
      FixedMeshBody body =
         new FixedMeshBody (MeshFactory.createIcosahedralSphere (1.0, 4));
      body.setPose (TMW);
      MeshCurve curve = new MeshCurve (body);
      curve.setResolution (res);
      curve.setInterpolation (interp);
      curve.setClosed (closed);
      for (int j : idxs) {
         Point3d pos = spherePoint (j);
         pos.transform (TMW);
         curve.addMarker (pos);
      }
      return curve;
   }

   MeshCurve createSphereCurve (
      RigidTransform3d TMW, Interpolation interp, boolean closed) {
      return createSphereCurve (TMW, interp, closed, 0.05, 0, 1, 2, 3, 4);
   }

   void checkPoint (String msg, Point3d result, Point3d check, double eps) {
      if (result.distance (check) > eps) {
         throw new TestException (
            msg + ": result=" + result + ", expected " + check);
      }
   }

   void checkVector (String msg, Vector3d result, Vector3d check, double eps) {
      if (result.distance (check) > eps) {
         throw new TestException (
            msg + ": result=" + result + ", expected " + check);
      }
   }

   /**
    * Checks that linear curves on a plane have exactly the expected points,
    * normals, marker point indices and length.
    */
   public void testLinearPlane() {
      double res = 0.1;
      RigidTransform3d TMW = new RigidTransform3d();
      TMW.setRandom();
      for (boolean closed : new boolean[] { false, true }) {
         MeshCurve curve =
            createPlaneCurve (TMW, Interpolation.LINEAR, closed, res);
         String tag = "linear plane, closed=" + closed;

         // build expected local points
         ArrayList<Point3d> pnts = new ArrayList<>();
         int nmkrs = PLANE_PNTS.length;
         int numi = closed ? nmkrs : nmkrs-1;
         int[] mkrIdxs = new int[nmkrs];
         double len = 0;
         for (int i=0; i<numi; i++) {
            Point3d p0 = new Point3d (PLANE_PNTS[i][0], PLANE_PNTS[i][1], 0);
            int i1 = (i+1)%nmkrs;
            Point3d p1 = new Point3d (PLANE_PNTS[i1][0], PLANE_PNTS[i1][1], 0);
            double d = p0.distance (p1);
            len += d;
            int nsegs = (int)Math.ceil (d/res);
            mkrIdxs[i] = pnts.size();
            for (int k=0; k<nsegs; k++) {
               Point3d p = new Point3d();
               p.combine (1-k/(double)nsegs, p0, k/(double)nsegs, p1);
               pnts.add (p);
            }
            if (!closed && i == numi-1) {
               mkrIdxs[i1] = pnts.size();
               pnts.add (p1);
            }
         }
         checkEquals (tag+": numPoints", curve.numPoints(), pnts.size());
         checkEquals (tag+": length", curve.getLength(), len, EPS);
         List<Point3d> lpnts = curve.getLocalPoints();
         List<Vector3d> lnrms = curve.getLocalNormals();
         Vector3d zdir = new Vector3d (0, 0, 1);
         for (int k=0; k<pnts.size(); k++) {
            checkPoint (tag+": local point "+k, lpnts.get(k), pnts.get(k), EPS);
            checkVector (tag+": local normal "+k, lnrms.get(k), zdir, EPS);
         }
         for (int i=0; i<nmkrs; i++) {
            checkEquals (
               tag+": marker "+i+" point index",
               curve.getMarkers().get(i).myPntIdx, mkrIdxs[i]);
         }
         // spacing should not exceed the resolution
         for (int k=0; k<pnts.size()-1; k++) {
            check (tag+": spacing exceeds resolution",
                   curve.getPoint(k).distance(curve.getPoint(k+1)) <= res+EPS);
         }
      }
   }

   /**
    * Checks that world points and normals are consistent with local ones,
    * including after the mesh body is moved.
    */
   public void testTransforms() {
      RigidTransform3d TMW = new RigidTransform3d();
      TMW.setRandom();
      MeshCurve curve = createSphereCurve (TMW, Interpolation.NATURAL_SPLINE, true);
      FixedMeshBody body = (FixedMeshBody)curve.getMeshComp();
      for (int pass=0; pass<2; pass++) {
         if (pass == 1) {
            // move the body; local points should not change
            TMW.setRandom();
            body.setPose (TMW);
         }
         List<Point3d> lpnts = curve.getLocalPoints();
         List<Vector3d> lnrms = curve.getLocalNormals();
         List<Point3d> wpnts = curve.getPoints();
         List<Vector3d> wnrms = curve.getNormals();
         checkEquals ("num world points", wpnts.size(), lpnts.size());
         checkEquals ("num world normals", wnrms.size(), lnrms.size());
         for (int k=0; k<lpnts.size(); k++) {
            Point3d pw = new Point3d (lpnts.get(k));
            pw.transform (TMW);
            Vector3d nw = new Vector3d (lnrms.get(k));
            nw.transform (TMW);
            checkPoint ("world point "+k, wpnts.get(k), pw, EPS);
            checkPoint ("getPoint "+k, curve.getPoint(k), pw, EPS);
            checkVector ("world normal "+k, wnrms.get(k), nw, EPS);
            checkVector ("getNormal "+k, curve.getNormal(k), nw, EPS);
         }
      }
   }

   /**
    * Checks spline and linear curves on a sphere: points should lie on the
    * mesh, normals should be approximately radial, spacing should be close
    * to the resolution, and interpolating curves should pass through the
    * markers.
    */
   public void testSphereCurves() {
      double res = 0.05;
      RigidTransform3d TMW = new RigidTransform3d();
      TMW.setRandom();
      for (Interpolation interp : Interpolation.values()) {
         for (boolean closed : new boolean[] { false, true }) {
            String tag = interp + " sphere, closed=" + closed;
            MeshCurve curve = createSphereCurve (TMW, interp, closed);
            List<Point3d> lpnts = curve.getLocalPoints();
            List<Vector3d> lnrms = curve.getLocalNormals();
            int np = lpnts.size();
            check (tag+": too few points", np > 10);
            for (int k=0; k<np; k++) {
               Point3d p = lpnts.get(k);
               double r = p.norm();
               check (tag+": point "+k+" not on mesh, r="+r,
                      r > 0.98 && r < 1+EPS);
               Vector3d n = lnrms.get(k);
               checkEquals (tag+": normal "+k+" magnitude", n.norm(), 1, 1e-8);
               check (tag+": normal "+k+" not radial", n.dot(p)/r > 0.99);
            }
            // Spacing can exceed the resolution because points are projected
            // onto the surface after being computed in 3D space, which
            // increases their spacing on convex surfaces.
            double maxSpacing = 1.5*res;
            int numi = closed ? np : np-1;
            for (int k=0; k<numi; k++) {
               double d = lpnts.get(k).distance (lpnts.get((k+1)%np));
               check (tag+": spacing "+d+" at "+k+" too large", d < maxSpacing);
            }
            if (interp != Interpolation.B_SPLINE) {
               for (MeshMarker mkr : curve.getMarkers()) {
                  checkPoint (
                     tag+": marker point", curve.getPoint(mkr.myPntIdx),
                     mkr.getPosition(), 1e-6);
               }
            }
            if (!closed) {
               // open curves should start and end at the end markers
               PointList<MeshMarker> mkrs = curve.getMarkers();
               checkPoint (
                  tag+": first point", curve.getPoint(0),
                  mkrs.get(0).getPosition(), 1e-6);
               checkPoint (
                  tag+": last point", curve.getPoint(np-1),
                  mkrs.get(mkrs.size()-1).getPosition(), 1e-6);
            }
         }
      }
   }

   /**
    * Checks that, without projection onto the mesh, curve points are spaced
    * no further apart than the resolution. For spline curves, this relies on
    * intervals being sampled uniformly in arc length.
    */
   public void testUnprojectedSpacing() {
      double res = 0.05;
      RigidTransform3d TMW = new RigidTransform3d();
      TMW.setRandom();
      for (Interpolation interp : Interpolation.values()) {
         for (boolean closed : new boolean[] { false, true }) {
            String tag = interp + " unprojected, closed=" + closed;
            MeshCurve curve = createSphereCurve (TMW, interp, closed);
            curve.setProjectToMesh (false);
            List<Point3d> lpnts = curve.getLocalPoints();
            int np = lpnts.size();
            int numi = closed ? np : np-1;
            for (int k=0; k<numi; k++) {
               double d = lpnts.get(k).distance (lpnts.get((k+1)%np));
               check (tag+": spacing "+d+" at "+k+" exceeds resolution",
                      d <= 1.01*res);
            }
         }
      }
   }

   /**
    * Checks getArcLength(), getPointAt() and getNormalAt(), including
    * clipping for open curves and wrapping for closed curves.
    */
   public void testArcLength() {
      RigidTransform3d TMW = new RigidTransform3d();
      TMW.setRandom();
      for (Interpolation interp : Interpolation.values()) {
         for (boolean closed : new boolean[] { false, true }) {
            String tag = interp + " arc length, closed=" + closed;
            MeshCurve curve = createSphereCurve (TMW, interp, closed);
            int np = curve.numPoints();
            double L = curve.getLength();
            int maxIdx = closed ? np : np-1;
            checkEquals (tag+": first arc length", curve.getArcLength(0), 0.0);
            checkEquals (
               tag+": last arc length", curve.getArcLength(maxIdx), L, EPS);
            double sum = 0;
            for (int k=0; k<maxIdx; k++) {
               sum += curve.getPoint(k).distance (curve.getPoint((k+1)%np));
               checkEquals (
                  tag+": arc length "+(k+1), curve.getArcLength(k+1), sum, EPS);
            }
            try {
               curve.getArcLength (maxIdx+1);
               throw new TestException (
                  tag+": no exception for out of range index");
            }
            catch (IllegalArgumentException e) {
               // expected
            }
            for (int k=0; k<np; k++) {
               double len = curve.getArcLength(k);
               checkPoint (
                  tag+": point at "+k, curve.getPointAt(len),
                  curve.getPoint(k), EPS);
               Vector3d nrm = curve.getNormal(k);
               nrm.normalize();
               checkVector (
                  tag+": normal at "+k, curve.getNormalAt(len), nrm, EPS);
               // midpoint of the interval
               if (k < maxIdx) {
                  double lmid = (len + curve.getArcLength(k+1))/2;
                  Point3d pmid = new Point3d();
                  pmid.combine (
                     0.5, curve.getPoint(k), 0.5, curve.getPoint((k+1)%np));
                  checkPoint (
                     tag+": midpoint "+k, curve.getPointAt(lmid), pmid, EPS);
                  checkEquals (
                     tag+": midpoint normal magnitude",
                     curve.getNormalAt(lmid).norm(), 1.0, EPS);
               }
            }
            if (closed) {
               double l0 = 0.37*L;
               Point3d p0 = curve.getPointAt (l0);
               Vector3d n0 = curve.getNormalAt (l0);
               for (double l : new double[] { l0+L, l0-L, l0+3*L, l0-2*L }) {
                  checkPoint (tag+": wrapped point", curve.getPointAt(l), p0, EPS);
                  checkVector (
                     tag+": wrapped normal", curve.getNormalAt(l), n0, EPS);
               }
               checkPoint (
                  tag+": point at L", curve.getPointAt(L), curve.getPoint(0), EPS);
               checkPoint (
                  tag+": point at tiny negative", curve.getPointAt(-1e-18),
                  curve.getPoint(0), EPS);
            }
            else {
               checkPoint (
                  tag+": clipped end", curve.getPointAt(L+1),
                  curve.getPoint(np-1), EPS);
               checkPoint (
                  tag+": clipped start", curve.getPointAt(-1),
                  curve.getPoint(0), EPS);
            }
         }
      }
   }

   /**
    * Checks the tangent, which should be a unit vector along the curve.
    */
   public void testTangents() {
      RigidTransform3d TMW = new RigidTransform3d();
      TMW.setRandom();
      for (boolean closed : new boolean[] { false, true }) {
         MeshCurve curve =
            createPlaneCurve (TMW, Interpolation.LINEAR, closed, 0.1);
         int np = curve.numPoints();
         // tangent at the start of the first interval (away from corners)
         Vector3d dir = new Vector3d();
         dir.sub (curve.getPoint(2), curve.getPoint(1));
         dir.normalize();
         checkVector ("tangent", curve.getTangent(1), dir, EPS);
         for (int k=0; k<np; k++) {
            Vector3d tan = curve.getTangent(k);
            checkEquals ("tangent magnitude", tan.norm(), 1.0, EPS);
            check ("tangent not normal to surface",
                   Math.abs(tan.dot(curve.getNormal(k))) < EPS);
         }
      }
   }

   /**
    * Checks findNearestPoint(), findPointAtLocation(), findPointAtDistance()
    * and findNearestMarkerInterval() for curves on a plane.
    */
   public void testQueries() {
      RigidTransform3d TMW = new RigidTransform3d();
      TMW.setRandom();
      for (boolean closed : new boolean[] { false, true }) {
         String tag = "queries, closed=" + closed;
         MeshCurve curve =
            createPlaneCurve (TMW, Interpolation.LINEAR, closed, 0.1);
         int np = curve.numPoints();
         PointList<MeshMarker> mkrs = curve.getMarkers();
         int nmkrs = mkrs.size();
         // in-plane direction perpendicular to the first interval
         Vector3d u = new Vector3d();
         u.sub (mkrs.get(1).getPosition(), mkrs.get(0).getPosition());
         u.normalize();
         Vector3d perp = new Vector3d();
         perp.cross (curve.getNormal(0), u);

         // nearest point to a point displaced from the first interval
         double l = 0.83;
         Point3d pc = curve.getPointAt (l);
         Point3d p0 = new Point3d();
         p0.scaledAdd (0.05, perp, pc);
         Point3d pr = new Point3d();
         double r = curve.findNearestPoint (pr, p0, 0);
         checkPoint (tag+": nearest point", pr, pc, EPS);
         Point3d ploc = new Point3d();
         curve.findPointAtLocation (ploc, r);
         checkPoint (tag+": point at location", ploc, pc, EPS);
         checkPoint (
            tag+": location at curve point",
            curvePointAtLocation (curve, 3.0), curve.getPoint(3), EPS);

         // point at distance from the first point, within the first interval
         if (!closed) {
            Point3d pd = new Point3d();
            double rd = curve.findPointAtDistance (
               pd, curve.getPoint(0), 0.75, 0);
            check (tag+": distance point not found", rd >= 0);
            checkEquals (
               tag+": distance", pd.distance(curve.getPoint(0)), 0.75, EPS);
            checkPoint (
               tag+": distance point", pd, curve.getPointAt(0.75), EPS);
         }

         // nearest marker intervals
         int numi = closed ? nmkrs : nmkrs-1;
         for (int i=0; i<numi; i++) {
            Point3d pm = new Point3d();
            pm.combine (
               0.5, mkrs.get(i).getPosition(),
               0.5, mkrs.get((i+1)%nmkrs).getPosition());
            checkEquals (
               tag+": nearest marker interval "+i,
               curve.findNearestMarkerInterval (pm), i);
         }
         if (!closed) {
            // points beyond the ends of an open curve
            Point3d pb = new Point3d();
            pb.scaledAdd (-0.2, u, mkrs.get(0).getPosition());
            checkEquals (
               tag+": interval before start",
               curve.findNearestMarkerInterval (pb), -1);
            Vector3d v = curve.getTangent (np-1);
            pb.scaledAdd (0.2, v, mkrs.get(nmkrs-1).getPosition());
            checkEquals (
               tag+": interval after end",
               curve.findNearestMarkerInterval (pb), numi);
         }
      }
   }

   private Point3d curvePointAtLocation (MeshCurve curve, double r) {
      Point3d p = new Point3d();
      curve.findPointAtLocation (p, r);
      return p;
   }

   /**
    * Checks that the curve is updated when markers or properties change.
    */
   public void testUpdates() {
      RigidTransform3d TMW = new RigidTransform3d();
      TMW.setRandom();
      MeshCurve curve =
         createSphereCurve (TMW, Interpolation.LINEAR, false, 0.05, 0, 1, 2);
      int np0 = curve.numPoints();
      double len0 = curve.getLength();

      Point3d pos = spherePoint (3);
      pos.transform (TMW);
      MeshMarker mkr = curve.addMarker (pos);
      check ("addMarker did not update curve", curve.numPoints() > np0);
      check ("addMarker did not update length", curve.getLength() > len0);

      int np1 = curve.numPoints();
      curve.setResolution (0.025);
      check ("setResolution did not update curve",
             curve.numPoints() > 1.8*(np1-1));
      curve.setResolution (0.05);
      checkEquals ("resolution reset", curve.numPoints(), np1);

      curve.setClosed (true);
      check ("setClosed did not update curve", curve.numPoints() > np1);
      curve.setClosed (false);
      checkEquals ("setClosed reset", curve.numPoints(), np1);

      curve.setInterpolation (Interpolation.NATURAL_SPLINE);
      check ("setInterpolation did not update curve",
             curve.getLength() != curve.getArcLength(0) &&
             curve.getLength() > len0);
      curve.setInterpolation (Interpolation.LINEAR);

      curve.removeMarker (mkr);
      checkEquals ("removeMarker numPoints", curve.numPoints(), np0);
      checkEquals ("removeMarker length", curve.getLength(), len0, EPS);

      curve.clearMarkers();
      checkEquals ("clearMarkers numPoints", curve.numPoints(), 0);
      checkEquals ("clearMarkers length", curve.getLength(), 0.0);
      check ("getPointAt not null for empty curve", curve.getPointAt(0) == null);
      check ("getNormalAt not null for empty curve",
             curve.getNormalAt(0) == null);

      // a single marker produces no curve points
      curve.addMarker (pos);
      checkEquals ("single marker numPoints", curve.numPoints(), 0);
   }

   /**
    * Checks curves with coincident adjacent markers: no coincident adjacent
    * curve points should be generated, marker point indices should be valid,
    * and queries should not fail.
    */
   public void testCoincidentMarkers() {
      int[][] patterns = new int[][] {
         { 0, 1, 1, 2, 3 },  // duplicate in the middle
         { 0, 0, 1, 2, 3 },  // duplicate at the start
         { 0, 1, 2, 3, 0 },  // last equals first
         { 0, 1, 2, 2, 2 },  // repeated at the end
         { 0, 0, 0, 0 },     // all coincident
         { 0, 0 },
      };
      RigidTransform3d TMW = new RigidTransform3d();
      TMW.setRandom();
      for (Interpolation interp : Interpolation.values()) {
         for (boolean closed : new boolean[] { false, true }) {
            for (int[] pat : patterns) {
               String tag = interp+", closed="+closed+" "+Arrays.toString(pat);
               MeshCurve curve =
                  createSphereCurve (TMW, interp, closed, 0.05, pat);
               int np = curve.numPoints();
               check (tag+": no points", np >= 1);
               int numi = closed ? np : np-1;
               if (np > 1) {
                  for (int k=0; k<numi; k++) {
                     double d = curve.getPoint(k).distance (
                        curve.getPoint((k+1)%np));
                     check (tag+": coincident points at "+k, d > EPS);
                  }
               }
               else {
                  checkEquals (tag+": length", curve.getLength(), 0.0);
               }
               for (MeshMarker mkr : curve.getMarkers()) {
                  int k = mkr.myPntIdx;
                  check (tag+": bad point index "+k, k >= 0 && k < np);
                  if (interp != Interpolation.B_SPLINE) {
                     checkPoint (
                        tag+": marker point", curve.getPoint(k),
                        mkr.getPosition(), 1e-6);
                  }
                  mkr.getPose (new RigidTransform3d());
               }
               if (np > 1) {
                  curve.findPointAtDistance (
                     null, curve.getPoint(0), 0.1, 0);
                  curve.findNearestMarkerInterval (curve.getPoint(np/2));
               }
               double L = curve.getLength();
               check (tag+": getPointAt", curve.getPointAt (0.3*L) != null);
               checkEquals (
                  tag+": getNormalAt magnitude",
                  curve.getNormalAt(0.3*L).norm(), 1.0, EPS);
            }
         }
      }
      // a spline that loops between coincident markers should still be
      // sampled, so that its length exceeds that of the linear curve
      MeshCurve lcurve = createSphereCurve (
         TMW, Interpolation.LINEAR, true, 0.05, 0, 1, 1, 2, 3);
      MeshCurve scurve = createSphereCurve (
         TMW, Interpolation.NATURAL_SPLINE, true, 0.05, 0, 1, 1, 2, 3);
      check ("natural spline loop not sampled",
             scurve.getLength() > lcurve.getLength());
   }

   public void test() {
      testLinearPlane();
      testTransforms();
      testSphereCurves();
      testUnprojectedSpacing();
      testArcLength();
      testTangents();
      testQueries();
      testUpdates();
      testCoincidentMarkers();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      MeshCurveTest tester = new MeshCurveTest();
      tester.runtest();
   }
}
