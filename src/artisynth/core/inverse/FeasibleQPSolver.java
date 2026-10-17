package artisynth.core.inverse;

import java.util.ArrayList;
import java.util.List;

import artisynth.core.mechmodels.ExcitationComponent;
import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.util.DoubleInterval;

/**
 * A {@link QPSolver} that checks the point it gets back against the program it
 * handed over, and re-solves without the equality constraints when the two
 * disagree. Two conditions trigger a retry:
 * <p>
 * <OL>
 * <LI>a solve that returned nothing.</LI>
 * <LI>a solve that returned a point far enough outside the bounds</LI>
 * </OL>
 * Far enough means above the {@code DEFAULT_BOUNDS_THRESHOLD}, since a small
 * excursion is pulled back by the next step on its own. Only large deviations
 * don't recover. The retry demotes every equality term to a cost term, where
 * {@link #getFallbackCount} and {@link #getFallbackTimes} record the solver
 * health.
 * <p>
 * 
 * @author Alexander Denk Copyright (c) 2026
 * <p>
 * University of Duisburg-Essen
 * <p>
 * Chair of Mechanics and Robotics
 * <p>
 * alexander.denk@uni-due.de
 */
public class FeasibleQPSolver extends QPSolver {
   // Relative tolerance on the equality residual
   public static final double DEFAULT_EQUALITY_TOLERANCE = 1e-9;
   // Absolute tolerance on the excitation bounds
   public static final double DEFAULT_BOUNDS_TOLERANCE = 1e-8;
   // How far outside its bounds a returned point has to be before re-solving.
   public static final double DEFAULT_BOUNDS_THRESHOLD = 1.0;
   // Below this the solution counts as identically zero
   public static final double DEFAULT_ZERO_TOLERANCE = 1e-12;
   // Fallbacks reported individually before switching to a running count
   public static final int DEFAULT_MAX_REPORTS = 20;
   /**
    * Effort level used for the fallback solve, expressed as the penalty on a
    * unit-weighted exciter relative to the normalized motion term. Negative by
    * default, meaning the controller's own level is left in place. Set a
    * positive value to trade the other way.
    */
   public static final double DEFAULT_FALLBACK_Q_MUSCLE = -1;

   protected TrackingController myController;
   protected double myEqTol = DEFAULT_EQUALITY_TOLERANCE;
   protected double myBoundsTol = DEFAULT_BOUNDS_TOLERANCE;
   protected int myMaxReports = DEFAULT_MAX_REPORTS;
   protected boolean myEnabledP = true;
   protected double myFallbackQMuscle = DEFAULT_FALLBACK_Q_MUSCLE;
   protected double myBoundsThreshold = DEFAULT_BOUNDS_THRESHOLD;
   protected double myZeroTol = DEFAULT_ZERO_TOLERANCE;

   protected int mySolveCount;
   protected int myFallbackCount;
   protected int myUnrecoveredCount;
   protected int myFrozenTriggers;
   protected int myViolationTriggers;
   protected double myWorstEqResidual;
   protected double myWorstBoundsViolation;
   protected ArrayList<Double> myFallbackTimes = new ArrayList<> ();

   // Scratch, sized on first use and reused: the check runs every step.
   protected MatrixNd myAeq = new MatrixNd ();
   protected VectorNd myBeq = new VectorNd ();
   protected VectorNd myRes = new VectorNd ();

   public FeasibleQPSolver (TrackingController controller) {
      myController = controller;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public VectorNd solve (
      List<QPCostTerm> costTerms, List<QPConstraintTerm> constraintTerms,
      int size, double t0, double t1) {

      VectorNd x = super.solve (costTerms, constraintTerms, size, t0, t1);
      mySolveCount++;
      if (!myEnabledP || constraintTerms == null) {
         return x;
      }
      double eqRes = equalityResidual (constraintTerms, x, size, t0, t1);
      double boundsRes = boundsViolation (x);
      // A solve that returned nothing is the frozen step
      boolean frozen =
         x.infinityNorm () <= myZeroTol
         && myBeq.infinityNorm () > myEqTol * Math.max (1.0, myBeq.infinityNorm ());
      // A solve that returned a point far enough outside the bounds
      boolean inadmissible = boundsRes > myBoundsThreshold;
      if (!frozen && !inadmissible) {
         return x;
      }
      if (frozen) {
         myFrozenTriggers++;
      }
      else {
         myViolationTriggers++;
      }

      myFallbackCount++;
      myFallbackTimes.add (t1);
      myWorstEqResidual = Math.max (myWorstEqResidual, eqRes);
      myWorstBoundsViolation = Math.max (myWorstBoundsViolation, boundsRes);
      report (t1, eqRes, boundsRes);

      VectorNd xf;
      L2RegularizationTerm l2 = myController.getL2RegularizationTerm ();
      double savedWeight = (l2 != null) ? l2.getWeight () : 0;
      try {
         if (l2 != null && myFallbackQMuscle >= 0) {
            l2.setWeight (myFallbackQMuscle * exciterWeightTrace ());
         }
         xf =
            super.solve (
               demoteEqualities (costTerms, constraintTerms),
               inequalitiesOnly (constraintTerms), size, t0, t1);
      }
      finally {
         // Restored whatever happens: the next step is attempted as a
         // constrained solve again, and it must see the configured level.
         if (l2 != null) {
            l2.setWeight (savedWeight);
         }
      }
      // Check bound violation and report if so, since the fallback program has
      // no equalities.
      if (boundsViolation (xf) > myBoundsTol) {
         myUnrecoveredCount++;
         System.err
            .println (
               "FeasibleQPSolver: fallback at t=" + t1
               + " is still out of bounds by " + boundsViolation (xf));
      }
      return xf;
   }

   /**
    * Sum of the exciter regularization weights, which is what the L2 term
    * divides by. Read fresh rather than cached: a driver may reweight the
    * exciters between runs, and a stale trace would silently rescale the
    * fallback.
    */
   protected double exciterWeightTrace () {
      double trace = 0;
      for (int i = 0; i < myController.numExciters (); i++) {
         trace += myController.getExcitationWeight (myController.getExciter (i));
      }
      return trace;
   }

   /**
    * Largest absolute residual of the equality block at {@code x}. Also leaves
    * the assembled right hand side in {@link #myBeq} so the caller can scale
    * the tolerance by its size.
    */
   protected double equalityResidual (
      List<QPConstraintTerm> constraintTerms, VectorNd x, int size, double t0,
      double t1) {

      int nrows = 0;
      for (QPConstraintTerm term : constraintTerms) {
         if (term.isEnabled () && term.getType () == QPTerm.Type.EQUALITY) {
            nrows += term.numConstraints (size);
         }
      }
      myAeq.setSize (nrows, size);
      myBeq.setSize (nrows);
      if (nrows == 0) {
         return 0;
      }
      int row = 0;
      for (QPConstraintTerm term : constraintTerms) {
         if (term.isEnabled () && term.getType () == QPTerm.Type.EQUALITY) {
            row = term.getTerm (myAeq, myBeq, row, t0, t1);
         }
      }
      myRes.setSize (nrows);
      myAeq.mul (myRes, x);
      myRes.sub (myBeq);
      return myRes.infinityNorm ();
   }

   /**
    * How far {@code x} falls outside the excitation bounds. The controller
    * runs incrementally, so the variable is the change in excitation and the
    * bounds shift with the value already reached.
    */
   protected double boundsViolation (VectorNd x) {
      boolean incremental = myController.getComputeIncrementally ();
      double worst = 0;
      for (int i = 0; i < myController.numExciters (); i++) {
         ExcitationComponent ex = myController.getExciter (i);
         DoubleInterval bounds = myController.getExcitationBounds (ex);
         double offset = incremental ? myController.getExcitation (i) : 0;
         double v = x.get (i);
         worst =
            Math
               .max (
                  worst,
                  Math
                     .max (
                        bounds.getLowerBound () - offset - v,
                        v - (bounds.getUpperBound () - offset)));
      }
      return worst;
   }

   /**
    * The cost list with every equality term appended. Terms that do not also
    * implement {@link QPCostTerm} cannot contribute a quadratic form and are
    * left out, which drops their constraint rather than misusing them.
    */
   protected List<QPCostTerm> demoteEqualities (
      List<QPCostTerm> costTerms, List<QPConstraintTerm> constraintTerms) {

      ArrayList<QPCostTerm> costs = new ArrayList<> (costTerms);
      for (QPConstraintTerm term : constraintTerms) {
         if (term.isEnabled () && term.getType () == QPTerm.Type.EQUALITY
         && term instanceof QPCostTerm) {
            costs.add ((QPCostTerm)term);
         }
      }
      return costs;
   }

   /**
    * The constraint list with the equality terms removed.
    */
   protected List<QPConstraintTerm> inequalitiesOnly (
      List<QPConstraintTerm> constraintTerms) {

      ArrayList<QPConstraintTerm> cons = new ArrayList<> ();
      for (QPConstraintTerm term : constraintTerms) {
         if (term.getType () != QPTerm.Type.EQUALITY) {
            cons.add (term);
         }
      }
      return cons;
   }

   private void report (double t1, double eqRes, double boundsRes) {
      if (myFallbackCount <= myMaxReports) {
         System.out
            .println (
               "FeasibleQPSolver: fallback at t=" + t1 + " (equality residual "
               + eqRes + ", bounds violation " + boundsRes + ")");
         if (myFallbackCount == myMaxReports) {
            System.out
               .println (
                  "FeasibleQPSolver: further fallbacks counted but not "
                  + "reported individually.");
         }
      }
   }

   // ---------------------------- Accessors ----------------------------

   /**
    * Clears the counters. Called at the start of a simulation so the numbers
    * describe one run rather than the life of the object.
    */
   public void reset () {
      mySolveCount = 0;
      myFallbackCount = 0;
      myUnrecoveredCount = 0;
      myFrozenTriggers = 0;
      myViolationTriggers = 0;
      myWorstEqResidual = 0;
      myWorstBoundsViolation = 0;
      myFallbackTimes.clear ();
   }

   /**
    * @return number of solves attempted since the last {@link #reset}.
    */
   public int getSolveCount () {
      return mySolveCount;
   }

   /**
    * @return number of solves that were re-run without their equality
    * constraints.
    */
   public int getFallbackCount () {
      return myFallbackCount;
   }

   /**
    * @return number of fallbacks that were themselves out of bounds. Anything
    * other than zero means the run contains steps this class could not
    * repair.
    */
   public int getUnrecoveredCount () {
      return myUnrecoveredCount;
   }

   /**
    * @return share of solves that fell back, in percent. The number that says
    * whether a run tracked its targets as constraints or as costs.
    */
   public double getFallbackPercent () {
      return mySolveCount == 0 ? 0 : 100.0 * myFallbackCount / mySolveCount;
   }

   /**
    * @return simulation times at which a fallback was taken.
    */
   public double[] getFallbackTimes () {
      double[] times = new double[myFallbackTimes.size ()];
      for (int i = 0; i < times.length; i++) {
         times[i] = myFallbackTimes.get (i);
      }
      return times;
   }

   public double getWorstEqualityResidual () {
      return myWorstEqResidual;
   }

   public double getWorstBoundsViolation () {
      return myWorstBoundsViolation;
   }

   public boolean isEnabled () {
      return myEnabledP;
   }

   /**
    * Disables the check, leaving the plain solver behaviour. Provided so a run
    * can be repeated without the fallback for comparison.
    */
   public void setEnabled (boolean enable) {
      myEnabledP = enable;
   }

   public double getEqualityTolerance () {
      return myEqTol;
   }

   public void setEqualityTolerance (double tol) {
      myEqTol = tol;
   }

   public double getBoundsTolerance () {
      return myBoundsTol;
   }

   /**
    * @return how far outside its bounds a point must be to trigger a retry.
    * See {@link #DEFAULT_BOUNDS_THRESHOLD}.
    */
   public double getBoundsThreshold () {
      return myBoundsThreshold;
   }

   /**
    * Sets the excursion size that counts as damage. Zero re-solves on any
    * violation, which is the behaviour before the threshold was introduced.
    *
    * @param mag excursion magnitude
    */
   public void setBoundsThreshold (double mag) {
      myBoundsThreshold = mag;
   }

   /**
    * @return retries taken because the solve returned nothing
    */
   public int getFrozenTriggers () {
      return myFrozenTriggers;
   }

   /**
    * @return retries taken because the solve returned an inadmissible point
    */
   public int getViolationTriggers () {
      return myViolationTriggers;
   }

   /**
    * @return effort level used for the fallback solve, as the penalty on a
    * unit-weighted exciter. See {@link #DEFAULT_FALLBACK_Q_MUSCLE}.
    */
   public double getFallbackQMuscle () {
      return myFallbackQMuscle;
   }

   /**
    * Sets the effort level for the fallback solve. A negative value leaves
    * the controller's own level in place, which reproduces the behaviour
    * before this was separated out.
    *
    * @param q penalty on a unit-weighted exciter
    */
   public void setFallbackQMuscle (double q) {
      myFallbackQMuscle = q;
   }

   /**
    * @return L2 weight the fallback would use right now, given the live
    * exciter weights
    */
   public double getFallbackL2Weight () {
      return myFallbackQMuscle * exciterWeightTrace ();
   }

   public void setBoundsTolerance (double tol) {
      myBoundsTol = tol;
   }
}
