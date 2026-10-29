package artisynth.core.inverse;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.util.*;
import maspack.solvers.DantzigQPSolver;
import maspack.solvers.DantzigQPSolver.Status;

/**
 * Solves a quadratic program given a set of cost and constraint terms.
 *
 * <p>The underlying {@link DantzigQPSolver} reports a {@link Status}, and when
 * that status is not {@link Status#SOLVED} it leaves the solution vector
 * untouched. The caller is then handed a vector of zeros that is
 * indistinguishable from a legitimate answer. {@link #getStatus} exposes the
 * status so the two can be told apart.
 *
 * <p>Optionally the solver also checks the point it gets back against the
 * program it handed over, and re-solves without the equality constraints when
 * the two disagree. Two conditions trigger that retry:
 *
 * <OL>
 * <LI>a solve that did not succeed, or returned a point containing NaN</LI>
 * <LI>a solve that returned a point far enough outside the inequality set</LI>
 * </OL>
 *
 * Far enough means above {@link #getBoundsThreshold}, since a small excursion
 * is pulled back by the next step on its own and only a large one does not.
 * The retry demotes every equality term to a cost term, so its targets are
 * approached rather than met, and {@link #getFallbackCount} and
 * {@link #getFallbackTimes} record where that happened.
 *
 * <p>The retry is off by default, leaving the behaviour of an existing model
 * unchanged. It matters once the targets are equality constraints rather than
 * cost terms, where a single infeasible or ill-conditioned step would
 * otherwise stall a whole simulation.
 *
 * @author John Lloyd, with the equality retry contributed by Alexander Denk
 */
public class QPSolver {

   /*
    * Default solver
    */
   DantzigQPSolver mySolver = new DantzigQPSolver();
   String myQPTestCaseFile = null; // "frameQP.txt";
   int myQPTestCaseCnt = 200;
   int myQPCnt = 0;
   FunctionTimer timer = new FunctionTimer();

   /**
    * Whether a failed solve is retried with its equality constraints demoted
    * to cost terms. Off, so an existing model behaves as before.
    */
   public static final boolean DEFAULT_FALLBACK_ENABLED = false;
   /**
    * How far outside the inequality set a returned point has to be before the
    * step is retried.
    */
   public static final double DEFAULT_BOUNDS_THRESHOLD = 1.0;
   /**
    * Absolute tolerance on the inequality constraints, used to decide whether
    * a retry repaired the step.
    */
   public static final double DEFAULT_BOUNDS_TOLERANCE = 1e-8;
   /**
    * Factor applied to the cost terms in the retry, relative to the equality
    * terms demoted alongside them. Negative leaves the program's own balance
    * in place, which is the behaviour when nothing is configured. Below one it
    * buys a closer approach to the demoted targets at the price of more
    * effort; above one, the other way.
    */
   public static final double DEFAULT_FALLBACK_COST_SCALE = -1;
   /**
    * Retries reported individually before switching to a running count.
    */
   public static final int DEFAULT_MAX_REPORTS = 20;

   protected boolean myFallbackEnabled = DEFAULT_FALLBACK_ENABLED;
   protected double myBoundsThreshold = DEFAULT_BOUNDS_THRESHOLD;
   protected double myBoundsTol = DEFAULT_BOUNDS_TOLERANCE;
   protected double myFallbackCostScale = DEFAULT_FALLBACK_COST_SCALE;
   protected int myMaxReports = DEFAULT_MAX_REPORTS;

   protected Status myStatus = Status.SOLVED;
   protected int mySolveCount;
   protected int myFallbackCount;
   protected int myUnrecoveredCount;
   protected int myFrozenTriggers;
   protected int myViolationTriggers;
   protected double myWorstEqResidual;
   protected double myWorstBoundsViolation;
   protected ArrayList<Double> myFallbackTimes = new ArrayList<> ();

   // The program as last assembled. Held in fields rather than locals so the
   // checks and the retry can read it without assembling it a second time.
   protected MatrixNd myQ = new MatrixNd ();
   protected VectorNd myP = new VectorNd ();
   protected MatrixNd myA = new MatrixNd ();
   protected VectorNd myB = new VectorNd ();
   protected MatrixNd myAeq = new MatrixNd ();
   protected VectorNd myBeq = new VectorNd ();
   // Scratch for the residual checks, sized on first use and reused.
   protected VectorNd myRes = new VectorNd ();

   /**
    * Solves the quadratic program of the form:
    * <pre>
    *   min { 1/2 x^T Q x + x^T p }
    *   A x &gt;= b,
    *   Aeq x = beq
    * </pre>
    * and returns the solution vector. Q and P are assembled
    * form the cost termss, while A, b, Aeq and beq are assembled
    * from the constraint terms, with inequality terms contributing
    * to A and b and equality terms contributing to Aeq and beq.
    *
    * @param costTerms terms used to assemble Q and p
    * @param constraintTerms terms used to assemble A, b, Aeq and beq.
    * @param size size of the program
    * @param t0 time step start time
    * @param t1 time step end time
    *
    * @return solution to the program
    */
   public VectorNd solve (
      List<QPCostTerm> costTerms,
      List<QPConstraintTerm> constraintTerms,
      int size, double t0, double t1) {

      assemble (costTerms, constraintTerms, size, t0, t1);

      VectorNd x = new VectorNd (size);
      mySolveCount++;
      myStatus = solveProgram (x, true);
      if (myStatus != Status.SOLVED) {
         System.err.println (
            "InverseSolve failed: solver status = "+myStatus.toString ());
      }
      if (!myFallbackEnabled) {
         return x;
      }

      // A solve that did not succeed leaves x untouched, so the zeros it
      // carries are not an answer. NaN is checked separately because an
      // infinity norm skips it rather than propagating it.
      boolean frozen = (myStatus != Status.SOLVED) || containsNaN (x);
      double boundsRes = inequalityViolation (x);
      // A small excursion recovers on its own; only a large one does not.
      boolean inadmissible = boundsRes > myBoundsThreshold;
      if (!frozen && !inadmissible) {
         return x;
      }
      double eqRes = equalityResidual (x);
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

      // Retry with the equality block folded into the objective. Scaling the
      // assembled cost, rather than reaching into any one term, keeps this
      // independent of which terms the program was built from.
      if (myFallbackCostScale >= 0) {
         myQ.scale (myFallbackCostScale);
         myP.scale (myFallbackCostScale);
      }
      addEqualitiesToCost (constraintTerms, t0, t1);

      VectorNd xf = new VectorNd (size);
      Status fbStatus = solveProgram (xf, false);
      double fbBoundsRes = inequalityViolation (xf);
      if (fbStatus != Status.SOLVED || containsNaN (xf)
      || fbBoundsRes > myBoundsTol) {
         myUnrecoveredCount++;
         System.err.println (
            "QPSolver: retry at t=" + t1 + " did not recover (status "
            + fbStatus + ", inequality violation " + fbBoundsRes + ")");
      }
      return xf;
   }

   /**
    * Assembles Q, p, A, b, Aeq and beq from the given terms, leaving them in
    * the corresponding fields.
    *
    * @param costTerms terms used to assemble Q and p
    * @param constraintTerms terms used to assemble A, b, Aeq and beq
    * @param size size of the program
    * @param t0 time step start time
    * @param t1 time step end time
    */
   protected void assemble (
      List<QPCostTerm> costTerms,
      List<QPConstraintTerm> constraintTerms,
      int size, double t0, double t1) {

      int numEq = 0;
      int numIneq = 0;
      if (constraintTerms != null) {
         for (QPConstraintTerm term : constraintTerms) {
            if (term.isEnabled()) {
               if (term.getType() == QPConstraintTerm.Type.EQUALITY) {
                  numEq += term.numConstraints (size);
               }
               else if (term.getType() == QPConstraintTerm.Type.INEQUALITY) {
                  numIneq += term.numConstraints (size);
               }
            }
         }
      }

      // Resized and cleared rather than reallocated: the assembly runs every
      // step, and the terms below add into these rather than overwrite them.
      myQ.setSize (size, size);
      myQ.setZero ();
      myP.setSize (size);
      myP.setZero ();
      myA.setSize (numIneq, size);
      myA.setZero ();
      myB.setSize (numIneq);
      myB.setZero ();
      myAeq.setSize (numEq, size);
      myAeq.setZero ();
      myBeq.setSize (numEq);
      myBeq.setZero ();

      // collect all cost terms
      for (QPCostTerm term : costTerms) {
         if (term.isEnabled ()) {
            term.getQP (myQ,myP,t0,t1);
         }
      }
      int rowEq = 0;
      int rowIneq = 0;

      // collect all constraint terms
      if (constraintTerms != null) {
         for (QPConstraintTerm term : constraintTerms) {
            if (term.isEnabled()) {
               if (term.getType() == QPConstraintTerm.Type.EQUALITY) {
                  rowEq += term.getTerm (myAeq, myBeq, rowEq, t0, t1);
               }
               else if (term.getType() == QPConstraintTerm.Type.INEQUALITY) {
                  rowIneq += term.getTerm (myA, myB, rowIneq, t0, t1);
               }
            }
         }
      }
   }

   /**
    * Hands the assembled program to the underlying solver and returns its
    * status.
    *
    * @param x returns the solution
    * @param withEqualities whether the assembled equality block is imposed.
    * False takes the inequality-only path, which is what the retry needs and
    * leaves Aeq and beq intact for inspection.
    */
   protected Status solveProgram (VectorNd x, boolean withEqualities) {
      try {
         if (!withEqualities || myAeq.rowSize() == 0) {
            // System.out.println ("Q=\n" + myQ.toString ("%16.12f"));
            // System.out.println ("P=\n" + myP.toString ("%16.12f"));
            // System.out.println ("A=\n" + myA.toString ("%12.5f"));
            // System.out.println ("b=\n" + myB.toString ("%12.5f"));
            FunctionTimer timer = null;
            if (myQPTestCaseFile != null) {
               if (myQPCnt == myQPTestCaseCnt) {
                  timer = new FunctionTimer();
                  timer.start();
               }
            }
            Status status = mySolver.solve (x,myQ,myP,myA,myB);
            if (myQPTestCaseFile != null) {
               if (myQPCnt == myQPTestCaseCnt) {
                  timer.stop();
                  System.out.println ("inverse solve time: "+timer.result(1));
                  writeQP (myQPTestCaseFile, myQ, myP, myA, myB, x);
               }
               myQPCnt++;
            }
            return status;
         }
         else {
            return mySolver.solve (x, myQ, myP, myA, myB, myAeq, myBeq);
         }
      }
      catch (Exception e) {
         e.printStackTrace();
         return Status.NUMERIC_ERROR;
      }
   }

   /**
    * Adds the quadratic form of every enabled equality term to the assembled
    * cost, which is what turns a constraint into a target. Terms that do not
    * also implement {@link QPCostTerm} cannot contribute a quadratic form and
    * are left out, which drops their constraint rather than misusing it.
    *
    * @param constraintTerms terms whose equality members are demoted
    * @param t0 time step start time
    * @param t1 time step end time
    */
   protected void addEqualitiesToCost (
      List<QPConstraintTerm> constraintTerms, double t0, double t1) {

      if (constraintTerms == null) {
         return;
      }
      for (QPConstraintTerm term : constraintTerms) {
         if (term.isEnabled () && term.getType () == QPTerm.Type.EQUALITY
         && term instanceof QPCostTerm) {
            ((QPCostTerm)term).getQP (myQ, myP, t0, t1);
         }
      }
   }

   /**
    * Largest absolute residual of the assembled equality block at {@code x},
    * or zero if the program has no equality constraints.
    *
    * @param x point at which to evaluate the residual
    * @return infinity norm of Aeq x - beq
    */
   protected double equalityResidual (VectorNd x) {
      if (myAeq.rowSize () == 0) {
         return 0;
      }
      myRes.setSize (myAeq.rowSize ());
      myAeq.mul (myRes, x);
      myRes.sub (myBeq);
      return myRes.infinityNorm ();
   }

   /**
    * How far {@code x} falls outside the assembled inequality set, as the
    * largest violation of {@code A x >= b}. Where the inequalities are the
    * excitation bounds, this is the excursion past the nearest bound in
    * excitation units.
    *
    * @param x point at which to evaluate the violation
    * @return largest entry of b - A x, or zero if none is positive
    */
   protected double inequalityViolation (VectorNd x) {
      if (myA.rowSize () == 0) {
         return 0;
      }
      myRes.setSize (myA.rowSize ());
      myA.mul (myRes, x);
      double worst = 0;
      for (int i = 0; i < myRes.size (); i++) {
         worst = Math.max (worst, myB.get (i) - myRes.get (i));
      }
      return worst;
   }

   /**
    * Whether any entry of {@code x} is NaN. Checked explicitly because an
    * infinity norm skips NaN rather than propagating it, so a solution that
    * is only partly NaN would otherwise pass every magnitude test.
    *
    * @param x vector to check
    * @return true if any entry is NaN
    */
   protected static boolean containsNaN (VectorNd x) {
      for (int i = 0; i < x.size (); i++) {
         if (Double.isNaN (x.get (i))) {
            return true;
         }
      }
      return false;
   }

   private void report (double t1, double eqRes, double boundsRes) {
      if (myFallbackCount <= myMaxReports) {
         System.out.println (
            "QPSolver: equalities dropped at t=" + t1 + " (equality residual "
            + eqRes + ", inequality violation " + boundsRes + ")");
         if (myFallbackCount == myMaxReports) {
            System.out.println (
               "QPSolver: further retries counted but not reported "
               + "individually.");
         }
      }
   }

   void writeQP (
      String fileName, MatrixNd Q, VectorNd f,
      MatrixNd A, VectorNd b, VectorNd x) {

      try {
         PrintWriter pw =
            new PrintWriter (new FileWriter ("frameQP.txt"));
         pw.printf ("size: %d nc: %d\n", Q.colSize(), A.rowSize());
         pw.println ("Q:");
         NumberFormat fmt = new NumberFormat ("%g");
         Q.write (pw, fmt);
         pw.println ("f:");
         f.write (pw, fmt);
         pw.println ("\nA:");
         A.write (pw, fmt);
         pw.println ("b:");
         b.write (pw, fmt);
         pw.println ("\nx:");
         x.write (pw, fmt);
         pw.println ("");
         pw.close();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   // ---------------------------- Accessors ----------------------------

   /**
    * Clears the counters, so they describe one simulation rather than the
    * life of the object.
    */
   public void reset () {
      myStatus = Status.SOLVED;
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
    * @return status of the most recent solve. Anything other than
    * {@link Status#SOLVED} means the solution vector was never written, so
    * the values it carries are not an answer.
    */
   public Status getStatus () {
      return myStatus;
   }

   /**
    * @return whether a failed solve is retried with its equality constraints
    * demoted to cost terms. See {@link #DEFAULT_FALLBACK_ENABLED}.
    */
   public boolean getFallbackEnabled () {
      return myFallbackEnabled;
   }

   /**
    * Enables the retry. Off by default, which leaves the plain solver
    * behaviour.
    *
    * @param enable whether to retry a failed solve without its equalities
    */
   public void setFallbackEnabled (boolean enable) {
      myFallbackEnabled = enable;
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
    * @return number of retries that were themselves inadmissible. Anything
    * other than zero means the run contains steps the retry could not
    * repair.
    */
   public int getUnrecoveredCount () {
      return myUnrecoveredCount;
   }

   /**
    * @return share of solves that were retried, in percent. The number that
    * says whether a run met its equality targets or merely approached them.
    */
   public double getFallbackPercent () {
      return mySolveCount == 0 ? 0 : 100.0 * myFallbackCount / mySolveCount;
   }

   /**
    * @return times at which the equality constraints were dropped.
    */
   public double[] getFallbackTimes () {
      double[] times = new double[myFallbackTimes.size ()];
      for (int i = 0; i < times.length; i++) {
         times[i] = myFallbackTimes.get (i);
      }
      return times;
   }

   /**
    * @return retries taken because the solve did not succeed
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
    * @return worst equality residual seen at a retry
    */
   public double getWorstEqualityResidual () {
      return myWorstEqResidual;
   }

   /**
    * @return worst inequality violation seen at a retry
    */
   public double getWorstBoundsViolation () {
      return myWorstBoundsViolation;
   }

   /**
    * @return how far outside the inequality set a point must be to trigger a
    * retry. See {@link #DEFAULT_BOUNDS_THRESHOLD}.
    */
   public double getBoundsThreshold () {
      return myBoundsThreshold;
   }

   /**
    * Sets the excursion size that counts as damage. Zero retries on any
    * violation.
    *
    * @param mag excursion magnitude
    */
   public void setBoundsThreshold (double mag) {
      myBoundsThreshold = mag;
   }

   /**
    * @return tolerance below which a retry counts as admissible
    */
   public double getBoundsTolerance () {
      return myBoundsTol;
   }

   /**
    * Sets the tolerance below which a retry counts as admissible.
    *
    * @param tol absolute tolerance on the inequality constraints
    */
   public void setBoundsTolerance (double tol) {
      myBoundsTol = tol;
   }

   /**
    * @return factor applied to the cost terms in the retry. See
    * {@link #DEFAULT_FALLBACK_COST_SCALE}.
    */
   public double getFallbackCostScale () {
      return myFallbackCostScale;
   }

   /**
    * Sets the effort level for the retry, as a factor on the cost terms
    * relative to the equality terms demoted alongside them. A negative value
    * leaves the program's own balance in place.
    *
    * @param scale factor on the cost terms
    */
   public void setFallbackCostScale (double scale) {
      myFallbackCostScale = scale;
   }

   /**
    * @return number of retries reported individually before switching to a
    * running count
    */
   public int getMaxReports () {
      return myMaxReports;
   }

   /**
    * Sets how many retries are reported individually.
    *
    * @param num number of individually reported retries
    */
   public void setMaxReports (int num) {
      myMaxReports = num;
   }
}
