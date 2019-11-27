package artisynth.demos.wrapping;

import java.util.ArrayList;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.MultiPointSpring.WrapSegment;
import artisynth.core.probes.NumericMonitorProbe;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.Wrench;

/**
 * Probe that computes the reaction moments produced on a body
 * by both a wrappable MultiPointSpring and an exact wrappping solution.
 *  
 * @author John E. Lloyd
 */
public class ExactMomentProbe extends NumericMonitorProbe {
   
   Frame myFrame;
   ExactWrappedSpring myExactSpring;
   MultiPointSpring mySpring;

   public ExactMomentProbe (
      Frame frame, MultiPointSpring spring, ExactWrappedSpring exactSpring,
      String fileName, double startTime, double stopTime, double interval) {

      super (6, fileName, startTime, stopTime, interval);
      myFrame = frame;
      mySpring = spring;
      myExactSpring = exactSpring;
   }


   private Vector3d getABForce (Point3d p0, Point3d p1, double F) {
      Vector3d f = new Vector3d();
      f.sub (p1, p0);
      f.scale (F/f.norm());
      return f;
   }

   private Vector3d getABMoment (RigidTransform3d TWW, Point3d p, Vector3d f) {
      Vector3d ploc = new Vector3d();
      ploc.sub (p, TWW.p);
      Vector3d m = new Vector3d();
      m.cross (ploc, f);
      return m;
   }

   public Wrench getExactMoment() {
      ArrayList<Point3d> ABpnts = myExactSpring.getAllABPoints();
      Wrench wr = new Wrench();
      if (ABpnts.size() == 2) {
         double F = myExactSpring.getForce();
         Point3d sprP0 = mySpring.getPoint(0).getPosition();
         Point3d sprPN = mySpring.getPoint(mySpring.numPoints()-1).getPosition();
         Vector3d fA = getABForce (ABpnts.get(0), sprP0, F);
         Vector3d fB = getABForce (ABpnts.get(1), sprPN, F);
         wr.f.add (fA, fB);
         RigidTransform3d TWW = myFrame.getPose();
         Vector3d mA = getABMoment (TWW, ABpnts.get(0), fA);
         Vector3d mB = getABMoment (TWW, ABpnts.get(1), fB);
         wr.m.add (mA, mB);
      }
      return wr;
   }

   public Wrench getWrapForce() {
      Wrench wr = new Wrench();
      if (mySpring.getSegment(0) instanceof WrapSegment) {
         WrapSegment wseg = (WrapSegment)mySpring.getSegment(0);
         if (wseg.numABPoints() == 2) {
            double F = mySpring.computeF (
               mySpring.getActiveLength(),
               mySpring.getActiveLengthDot());
            Point3d sprP0 = mySpring.getPoint(0).getPosition();
            Point3d sprPA = wseg.getABPoint(0).getPosition();
            Point3d sprPB = wseg.getABPoint(1).getPosition();
            Point3d sprPN = mySpring.getPoint(1).getPosition();
            Vector3d fA = getABForce (sprPA, sprP0, F);
            Vector3d fB = getABForce (sprPB, sprPN, F);
            wr.f.add (fA, fB);
            RigidTransform3d TWW = myFrame.getPose();
            Vector3d mA = getABMoment (TWW, sprPA, fA);
            Vector3d mB = getABMoment (TWW, sprPB, fB);
            wr.m.add (mA, mB);
         }
      }
      return wr;
   }

   public void apply (double t) {
      super.apply (t);
   }

   public void generateData (VectorNd vec, double t, double trel) {
      Wrench we = getExactMoment();
      Wrench wr = getWrapForce();

      vec.set (0, we.m.x);
      vec.set (1, we.m.y);
      vec.set (2, we.m.z);

      vec.set (3, wr.m.x);
      vec.set (4, wr.m.y);
      vec.set (5, wr.m.z);
   }

}
