package artisynth.demos.wrapping;

import java.util.ArrayList;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.interpolation.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MultiPointSpring.WrapSegment;
import artisynth.core.probes.*;

/**
 * Probe that computes the translational reaction forces produced on a body
 * by both a wrappable MultiPointSpring and an exact wrappping solution.
 *  
 * @author John E. Lloyd
 */
public class ExactForceProbe extends NumericMonitorProbe {
   
   Frame myFrame;
   ExactWrappedSpring myExactSpring;
   MultiPointSpring mySpring;

   public ExactForceProbe (
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

   public Wrench getExactForce() {
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

   private void integrate (
      Vector3d pos, Vector3d vel, Vector3d f0, Vector3d f1, double h) {

      Vector3d velInc = new Vector3d();

      velInc.add (f0, f1);
      velInc.scale (0.5*h);

      pos.scaledAdd (0.5*h, velInc);
      pos.scaledAdd (h, vel);

      vel.add (velInc);      
   }

   protected Vector3d getForce (NumericListKnot knot) {
      Vector3d f = new Vector3d();
      f.set (knot.v.get(3), knot.v.get(4), knot.v.get(5));
      return f;
   }

   protected Vector3d getExactForce (NumericListKnot knot) {
      Vector3d f = new Vector3d();
      f.set (knot.v.get(0), knot.v.get(1), knot.v.get(2));
      return f;
   }

   public void apply (double t) {
      super.apply (t);

      if (t == getStopTime()) {
         NumericList list = getNumericList();
         NumericListKnot knot;

         // integrate the results
         Vector3d vel = new Vector3d();
         Vector3d pos = new Vector3d();
         Vector3d exactVel = new Vector3d();
         Vector3d exactPos = new Vector3d();

         NumericListKnot prev = null;
         for (knot=list.getFirst(); knot!=null; knot=knot.getNext()) {
            if (prev != null) {
               double h = knot.t - prev.t;
               integrate (
                  exactPos, exactVel,
                  getExactForce(prev), getExactForce(knot), h);
               integrate (
                  pos, vel,
                  getForce(prev), getForce(knot), h);
            }
            prev = knot;
         }
         System.out.printf (
            "estimated vel error: %g  estimated pos error: %g\n",
            vel.distance(exactVel)/exactVel.norm(),
            pos.distance(exactPos)/exactPos.norm());
      }

   }

   public void generateData (VectorNd vec, double t, double trel) {
      Wrench we = getExactForce();
      Wrench wr = getWrapForce();

      vec.set (0, we.f.x);
      vec.set (1, we.f.y);
      vec.set (2, we.f.z);

      vec.set (3, wr.f.x);
      vec.set (4, wr.f.y);
      vec.set (5, wr.f.z);
   }

}
