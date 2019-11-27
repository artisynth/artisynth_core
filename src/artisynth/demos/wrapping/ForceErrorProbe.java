package artisynth.demos.wrapping;

import java.util.ArrayList;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.interpolation.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MultiPointSpring.WrapSegment;
import artisynth.core.probes.*;

/**
 * Probe that estimates the error in the reaction forces imparted
 * onto a rigid body by a wrappable MultiPointSpring. This is done
 * by comparing these forces against those computed via an exact
 * solution method.
 * 
 * @author John E. Lloyd
 */
public class ForceErrorProbe extends NumericMonitorProbe {
   
   RigidBody myBody;
   ExactWrappedSpring myExactSpring;
   MultiPointSpring mySpring;
   boolean myUseMomentError = true;

   double myMaxF = 0;

   public ForceErrorProbe (
      RigidBody body, MultiPointSpring spring, ExactWrappedSpring exactSpring,
      String fileName, double startTime, double stopTime, double interval) {

      super (1, fileName, startTime, stopTime, interval);
      myBody = body;
      mySpring = spring;
      myExactSpring = exactSpring;
   }

   public void setUseMomentError (boolean enable) {
      myUseMomentError = enable;
   }

   public boolean getUseMomentError () {
      return myUseMomentError;
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
         RigidTransform3d TWW = myBody.getPose();
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
            RigidTransform3d TWW = myBody.getPose();
            Vector3d mA = getABMoment (TWW, sprPA, fA);
            Vector3d mB = getABMoment (TWW, sprPB, fB);
            wr.m.add (mA, mB);
         }
      }
      return wr;
   }

   public double getMaxForce () {
      return myMaxF;
   }         

   public void apply (double t) {
      NumericList list = getNumericList();
      NumericListKnot knot;
      if (t == 0) {        myMaxF = 0;
         knot=list.getFirst();
         if (knot != null) {
            knot.v.setZero();
         }
      }
      super.apply (t);
      if (t == getStopTime()) {
         // scale the final results 
         double maxErr = 0;
         double avgErr = 0;

         for (knot=list.getFirst(); knot!=null; knot=knot.getNext()) {
            double ferr = knot.v.get(0)/myMaxF;
            knot.v.set (0, ferr);
            avgErr += ferr;
            if (ferr > maxErr) {
               maxErr = ferr;
            }
         }
         avgErr /= list.getNumKnots();
         System.out.printf (
            "Exact solution error: max=%g avg=%g\n", maxErr, avgErr);
      }
   }

   public void generateData (VectorNd vec, double t, double trel) {
      Wrench we = getExactForce();

      Twist tw = new Twist();
      SpatialInertia M = new SpatialInertia();
      myBody.getInertia(M);

      M.mulInverse (tw, we);
      myMaxF = Math.max (myMaxF, Math.sqrt(we.dot(tw)));

      we.sub (getWrapForce());
      M.mulInverse (tw, we);
      vec.set (0, Math.sqrt(we.dot(tw)));
   }

}
