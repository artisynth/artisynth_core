/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import artisynth.core.modelbase.ScanTest;
import artisynth.core.mechmodels.MultiPointSpring.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;

public class MultiPointSpringTest extends UnitTest {

   /**
    * Special ConditionalPoint class for testing conditional path point code.
    */
   private class TestCondPoint extends Point implements ConditionalPoint {

      boolean myActive = false;

      TestCondPoint (Point3d pos, boolean active) {
         super (pos);
         setActive (active);
      }

      void setActive (boolean active) {
         myActive = active;
      }

      public boolean isPointActive() {
         return myActive;
      }
   }

   private void zeroForces (ArrayList<Point> pnts) {
      for (int i=0; i<pnts.size(); i++) {
         pnts.get(i).zeroForces();
      }
   }

   private void getForce (VectorNd fvec, ArrayList<Point> pnts) {
      double[] buf = fvec.getBuffer();
      int idx = 0;
      for (int i=0; i<pnts.size(); i++) {
         Vector3d f = pnts.get(i).getForce();
         buf[idx++] = f.x;
         buf[idx++] = f.y;
         buf[idx++] = f.z;
      }
   }

   private void getPos (VectorNd xvec, ArrayList<Point> pnts) {
      double[] x = xvec.getBuffer();
      int idx = 0;
      for (Point p : pnts) {
         idx = p.getPosState (x, idx);
      }
   }

   private void setPos (VectorNd xvec, ArrayList<Point> pnts) {
      double[] x = xvec.getBuffer();
      int idx = 0;
      for (Point p : pnts) {
         idx = p.setPosState (x, idx);
      }
   }

   private void getVel (VectorNd vvec, ArrayList<Point> pnts) {
      double[] v = vvec.getBuffer();
      int idx = 0;
      for (Point p : pnts) {
         idx = p.getVelState (v, idx);
      }
   }

   private void setVel (VectorNd vvec, ArrayList<Point> pnts) {
      double[] v = vvec.getBuffer();
      int idx = 0;
      for (Point p : pnts) {
         idx = p.setVelState (v, idx);
      }
   }

   private MatrixNd numericPosJacobian (
      MultiPointSpring spring, ArrayList<Point> pnts) {

      int size = 3*pnts.size();
      VectorNd f0 = new VectorNd (size);
      VectorNd f = new VectorNd (size);
      VectorNd x0 = new VectorNd (size);
      VectorNd x = new VectorNd (size);
      VectorNd dfdx = new VectorNd (size);

      MatrixNd K = new MatrixNd (size, size);

      zeroForces (pnts);
      spring.applyForces (0);
      getForce (f0, pnts);
      getPos (x0, pnts);

      double delx = 1e-8;

      for (int j=0; j<size; j++) {
         x.set (x0);
         x.set (j, x.get(j)+delx);
         setPos (x, pnts);
         zeroForces (pnts);
         spring.applyForces (0);
         getForce (f, pnts);
         dfdx.sub (f, f0);
         dfdx.scale (1/delx);
         K.setColumn (j, dfdx); 
      }
      return K;
   }

   private MatrixNd numericVelJacobian (
      MultiPointSpring spring, ArrayList<Point> pnts) {

      int size = 3*pnts.size();
      VectorNd f0 = new VectorNd (size);
      VectorNd f = new VectorNd (size);
      VectorNd v0 = new VectorNd (size);
      VectorNd v = new VectorNd (size);
      VectorNd dfdv = new VectorNd (size);

      MatrixNd K = new MatrixNd (size, size);

      zeroForces (pnts);
      spring.applyForces (0);
      getForce (f0, pnts);
      getVel (v0, pnts);

      double delv = 1e-8;

      for (int j=0; j<size; j++) {
         v.set (v0);
         v.set (j, v.get(j)+delv);
         setVel (v, pnts);
         zeroForces (pnts);
         spring.applyForces (0);
         getForce (f, pnts);
         dfdv.sub (f, f0);
         dfdv.scale (1/delv);
         K.setColumn (j, dfdv); 
      }
      return K;
   }

   double compareMatrices (SparseBlockMatrix M, MatrixNd Mcheck) {
      MatrixNd Mdense = new MatrixNd (M);
      Mdense.sub (Mcheck);
      double normErr = Mdense.infinityNorm();
      double normCheck = Mcheck.infinityNorm();
      if (normErr == 0 && normCheck == 0) {
         return 0;
      }
      else {
         return normErr/normCheck;
      }
   }

   public void test (int numPnts) {
      test (numPnts, null);
   }

   public void test (int numPnts, int[] passiveSegs) {
      // need to have zero damping to get Jacobian to pass
      MultiPointSpring spring = new MultiPointSpring(1, 2, 3);
      ArrayList<Point> pnts = new ArrayList<Point>();
      Point3d pos = new Point3d();
      Vector3d vel = new Vector3d();
      for (int i=0; i<numPnts; i++) {
         Point pnt = new Particle ();
         pos.setRandom();
         pnt.setPosition (pos);
         vel.setRandom();
         pnt.setVelocity (vel);
         pnts.add (pnt);
         spring.addPoint (pnt);
      }
      if (passiveSegs != null) {
         for (int idx : passiveSegs) {
            if (idx < numPnts-1) {
               spring.setSegmentPassive (idx, true);
            }
         }
      }
      int[] blkSizes = new int[numPnts];
      for (int i=0; i<numPnts; i++) {
         pnts.get(i).setSolveIndex (i);
         blkSizes[i] = 3;
      }
      SparseNumberedBlockMatrix M = new SparseNumberedBlockMatrix (blkSizes);
      spring.addSolveBlocks (M);
      spring.addPosJacobian (M, 1);

      //System.out.println ("K=\n" + M.toString("%8.3f"));
      MatrixNd Kcheck = numericPosJacobian (spring, pnts);
      double err = compareMatrices (M, Kcheck);
      if (err > 2e-7) {
         throw new TestException (
            "Numeric position Jacobian differs from analytic by " + err);
      }
      //System.out.println ("pos err = " + err);

      M.setZero();
      spring.addVelJacobian (M, 1);
      //System.out.println ("D=\n" + M.toString("%8.3f"));
      MatrixNd Dcheck = numericVelJacobian (spring, pnts);
      err = compareMatrices (M, Dcheck);
      if (err > 1e-7) {
         throw new TestException (
            "Numeric velocity Jacobian differs from analytic by " + err);
      }
      //System.out.println ("vel err = " + err);

      // test scan and write. Create a MechModel to use as a reference object
      // for the points
      MechModel mech = new MechModel ("test");
      for (Point p : pnts) {
         mech.addParticle ((Particle)p);
      }
      testWriteScan (spring, mech);
   }

   int getNumSegmentKnots (MultiPointSpring spr, int specIdx, int nextIdx) {
      int numk = 0;
      for (int idx=specIdx; idx<nextIdx; idx++) {
         SegmentSpec spec = spr.getSegmentSpec (idx);
         if (spec.getNumKnots() > 0) {
            numk += spec.getNumKnots();
         }
         else {
            numk += spr.getWrapKnotDensity();
         }
      }
      return numk;
   }

   boolean isSegmentWrappable (MultiPointSpring spr, int specIdx, int nextIdx) {
      for (int idx=specIdx; idx<nextIdx; idx++) {
         SegmentSpec spec = spr.getSegmentSpec (idx);
         if (spec.isWrappable()) {
            return true;
         }
      }
      return false;
   }

   boolean isSegmentPassive (MultiPointSpring spr, int specIdx, int nextIdx) {
      for (int idx=specIdx; idx<nextIdx; idx++) {
         SegmentSpec spec = spr.getSegmentSpec (idx);
         if (!spec.isPassive()) {
            return false;
         }
      }
      return true;
   }

   void testActivityChange (
      MultiPointSpring spr, TestCondPoint[] cpnts, int[] activePnts) {

      for (int j=0; j<cpnts.length; j++) {
         cpnts[j].setActive (false);
      }
      for (int i=0; i<activePnts.length; i++) {
         cpnts[activePnts[i]-1].setActive (true);
      }
      spr.maybeUpdateForConditionals();
      checkEquals (
         "num segments after change", spr.numSegments(), activePnts.length+1);
      ArrayList<Segment> segs = spr.getSegments();
      SegmentSpec lastSpec = spr.getSegmentSpec(spr.numSegmentSpecs());
      for (int i=0; i<segs.size(); i++) {
         Segment seg = segs.get(i);
         int specIdx;
         if (i == 0) {
            specIdx = 0;
         }
         else {
            specIdx = activePnts[i-1];
         }
         Point pntA;
         int nextIdx;
         if (i < segs.size()-1) {
            nextIdx = segs.get(i+1).mySpecIdx;
         }
         else {
            nextIdx = spr.numSegmentSpecs();
         }
         pntA = spr.getSegmentSpec(nextIdx).myPntB;
         checkEquals (
            "seg "+i+" specIdx", seg.mySpecIdx, specIdx);
         checkEquals (
            "seg "+i+" pnt.x", seg.myPntB.getPosition().x, (double)specIdx);
         if (pntA != seg.myPntA) {
            throw new TestException (
               "seg "+i+": pntA corresponds to " + seg.myPntA.getPosition().x +
               ", expecting " + pntA.getPosition().x);
         }
         checkEquals (
            "seg "+i+" wrappable", seg instanceof WrapSegment,
            isSegmentWrappable (spr, specIdx, nextIdx));
         checkEquals (
            "seg "+i+" passive", seg.isPassive(),
            isSegmentPassive (spr, specIdx, nextIdx));
         if (seg instanceof WrapSegment) {
            WrapSegment wseg = (WrapSegment)seg;
            checkEquals (
               "seg "+i+" numKnots", wseg.numKnots(),
               getNumSegmentKnots (spr, specIdx, nextIdx));
         }
      }
   }

   void testWriteScan (MultiPointSpring spr, MechModel mech) {
      ScanTest.testScanAndWrite (spr, mech, null);
   }

   void checkSegments (MultiPointSpring spr, int... checks) {
      if (checks.length % 3 != 0) {
         throw new IllegalArgumentException (
            "checks.length=" + checks.length + "; must be a multiple of 3");
      }
      ArrayList<Segment> segs = spr.getSegments();
      checkEquals ("number of segments", segs.size(), checks.length/3);
      for (int i=0; i<segs.size(); i++) {
         Segment seg = segs.get(i);
         int pntNum = checks[3*i];
         int flags = checks[3*i+1];
         int numk = checks[3*i+2];
         checkEquals ("seg "+i+" point number", seg.myPntB.getNumber(), pntNum);
         checkEquals ("seg "+i+" flags", seg.myFlags, flags);
         if (seg instanceof WrapSegment) {
            WrapSegment wseg = (WrapSegment)seg;
            checkEquals ("seg "+i+" num knots", wseg.myNumKnots, numk);
         }
         else {
            checkEquals ("seg "+i+" num knots", 0, numk);
         }
      }
   }

   void testSpecEditting() {
      // Create mechmodel to store the points for write/scan testing
      MechModel mech = new MechModel();

      int W = SegmentBase.SEG_WRAPPABLE;
      int P = SegmentBase.SEG_PASSIVE;
      int WP = (W | P);

      // create the points that will be uised to populate the spring
      Point[] pnts = new Point[7];
      for (int i=0; i<pnts.length; i++) {
         pnts[i] = new Point(new Point3d (i, 0, 0));
         mech.addPoint (pnts[i]);
      }

      MultiPointSpring spr = new MultiPointSpring();

      // empty spring
      checkSegments (spr);

      // simple one segment 
      spr.addPoint (pnts[0]);
      spr.addPoint (pnts[1]);
      checkSegments (spr, 0, 0x0, 0);

      // one segment wrappable and passive
      spr.setSegmentWrappable (0, 0, null);
      spr.setSegmentPassive (0, true);
      checkSegments (spr, 0, WP, 50);

      // remove and replace first point
      spr.removePoint (pnts[0]);
      checkSegments (spr);
      // these will only take effect when we add *after* the last point
      spr.setSegmentWrappable (30);
      spr.setSegmentPassive ();
      spr.addPoint (0, pnts[2]);
      checkSegments (spr, 2, 0, 0);

      spr.addPoint (pnts[3]);
      checkSegments (spr, 2, 0, 0,  1, WP, 30);

      spr.setSegmentWrappable (0);
      spr.addPoint (pnts[4]);
      checkSegments (spr, 2, 0, 0,  1, WP, 30,  3, W, 50);

      spr.removePoint (pnts[1]);
      checkSegments (spr, 2, 0, 0,  3, W, 50);

      spr.setAllSegmentsWrappable(0);
      checkSegments (spr, 2, W, 50,  3, W, 50);

      spr.clearPoints();
      checkSegments (spr);
   }

   void testConditionalUpdating() {
      // create a multipoint spring with conditional path points spaced 1.0
      // apart.

      // Create mechmodel to store the points for write/scan testing
      MechModel mech = new MechModel();

      // test activity change with not wrapping segments, but with some
      // segments set to be passive
      TestCondPoint[] cpnts = new TestCondPoint[] {
         new TestCondPoint (new Point3d(1, 0, 0), false),
         new TestCondPoint (new Point3d(2, 0, 0), false),
         new TestCondPoint (new Point3d(3, 0, 0), false),
         new TestCondPoint (new Point3d(4, 0, 0), false),
         new TestCondPoint (new Point3d(5, 0, 0), false),
      };
      Point p0 = new Point(new Point3d (0, 0, 0));
      Point p6 = new Point(new Point3d (6, 0, 0));
      mech.addPoint (p0);
      for (TestCondPoint cp : cpnts) {
         mech.addPoint (cp);
      }
      mech.addPoint (p6);

      MultiPointSpring spr = new MultiPointSpring();
      mech.addMultiPointSpring (spr);
      spr.addPoint (p0);
      spr.addPoint (cpnts[0]);
      spr.setSegmentPassive();
      spr.addPoint (cpnts[1]);
      spr.setSegmentPassive();
      spr.addPoint (cpnts[2]);
      spr.addPoint (cpnts[3]);
      spr.addPoint (cpnts[4]);
      spr.setSegmentPassive();
      spr.addPoint (p6);

      testActivityChange (spr, cpnts, new int[] {1, 2, 3, 4, 5});
      testWriteScan (spr, mech);
      for (int k=0; k<100; k++) {
         testActivityChange (
            spr, cpnts, RandomGenerator.randomSubsequence(1, cpnts.length));
         testWriteScan (spr, mech);
      }

      // now test with all segments wrappable
      spr = new MultiPointSpring();
      mech.addMultiPointSpring (spr);
      spr.addPoint (p0);
      spr.setSegmentWrappable (0);
      spr.addPoint (cpnts[0]);
      spr.setSegmentWrappable (0);
      spr.addPoint (cpnts[1]);
      spr.setSegmentWrappable (30);
      spr.addPoint (cpnts[2]);
      spr.setSegmentWrappable (30);
      spr.addPoint (cpnts[3]);
      spr.setSegmentWrappable (0);
      spr.addPoint (cpnts[4]);
      spr.setSegmentWrappable (30);
      spr.addPoint (p6);

      testActivityChange (spr, cpnts, new int[] {1, 2, 3, 4, 5});
      testWriteScan (spr, mech);
      for (int k=0; k<100; k++) {
         testActivityChange (
            spr, cpnts, RandomGenerator.randomSubsequence(1, cpnts.length));
         testWriteScan (spr, mech);
      }

      // est with only some segments wrappable
      spr = new MultiPointSpring();
      mech.addMultiPointSpring (spr);
      spr.addPoint (p0);
      spr.setSegmentWrappable (0);
      spr.addPoint (cpnts[0]);
      spr.setSegmentWrappable (0);
      spr.addPoint (cpnts[1]);
      spr.addPoint (cpnts[2]);
      spr.addPoint (cpnts[3]);
      spr.addPoint (cpnts[4]);
      spr.setSegmentWrappable (30);
      spr.addPoint (p6);

      testActivityChange (spr, cpnts, new int[] {1, 2, 3, 4, 5});
      testWriteScan (spr, mech);
      for (int k=0; k<100; k++) {
         testActivityChange (
            spr, cpnts, RandomGenerator.randomSubsequence(1, cpnts.length));
         testWriteScan (spr, mech);
      }
   }

   public void test() {
      //tester.test(0); // force and Jacobians should be 0
      //tester.test(1); // force and Jacobians should be 0

      testSpecEditting();

      // test(2);
      // test(3);
      // test(4);
      // test(5);
      // test(2, new int[] {0});               
      // test(4, new int[] {0, 1});               
      // test(5, new int[] {0, 2, 4});   

      // testConditionalUpdating();
   }

   public static void main (String[] args) {
      MultiPointSpringTest tester = new MultiPointSpringTest();
      // have to set myIgnoreCoriolisInJacobian to false since otherwise
      // non-symmetric Jacobian terms will not be computed and the Jacobian
      // will not match the numeric check
      PointSpringBase.myIgnoreCoriolisInJacobian = false;
      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }
}
