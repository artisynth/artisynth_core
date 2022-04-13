package maspack.solvers;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import java.util.Random;

import maspack.solvers.LCPSolver.Status;

/**
 * Base class for LCP solver tests
 */
public abstract class LCPSolverTestBase extends UnitTest {

   protected static double DOUBLE_PREC = 2.220446049250313e-16;
   private Random myRandom;
   protected boolean myTimingRequested = false;

   protected static double INF = Double.POSITIVE_INFINITY;

   public abstract LCPSolver getSolver();

   protected long myTotalPivots = 0;
   protected long myTotalIters = 0;
   protected boolean myMaskPivotCounting = false;
   protected double myMaxSolveTol = 0;

   public void clearPivotCount() {
      myTotalPivots = 0;
      myTotalIters = 0;
      myMaxSolveTol = 0;
   }

   public void printPivotCount(String msg, int ntests) {
      double n = ntests;
      System.out.printf (
         "%s pivots=%g iters=%g tol=%g\n",
         msg, myTotalPivots/n, myTotalIters/n, myMaxSolveTol);
   }

   public void printAndClearPivotCount(String msg, int ntests) {
      printPivotCount(msg, ntests);
      clearPivotCount();
   }

   protected boolean isTimingRequested() {
      return myTimingRequested;
   }

   protected LCPSolverTestBase() {
      maspack.util.RandomGenerator.setSeed (0x1234);
      myRandom = RandomGenerator.get();
   }
   
   protected void setStateToWLower (VectorNi state) {
      for (int i=0; i<state.size(); i++) {
         state.set (i, LCPSolver.W_VAR_LOWER);
      }
   }

   public void testSpecial (double[] Mvals, double[] qvals) {
      int size = qvals.length;
      MatrixNd M = new MatrixNd (size, size);
      VectorNd q = new VectorNd (size);
      VectorNd lo = new VectorNd (size);
      VectorNd hi = new VectorNd (size);
      for (int i = 0; i < size; i++) {
         lo.set (i, -INF);
         hi.set (i, 0);
      }

      M.set (Mvals);
      q.set (qvals);
      VectorNd z = new VectorNd(size);
      VectorNd w = new VectorNd(size);

      testSolver (z, null, M, q, Status.SOLVED);
   }

   public void testSpecial (
      double[] Mvals, double[] qvals, double[] loVals, double[] hiVals, int nub) {
      int size = qvals.length;
      MatrixNd M = new MatrixNd (size, size);
      VectorNd q = new VectorNd (size);
      VectorNd lo = new VectorNd (size);
      VectorNd hi = new VectorNd (size);

      for (int i = 0; i < size; i++) {
         lo.set (i, loVals[i]);
         hi.set (i, hiVals[i]);
      }
      VectorNd z = new VectorNd(size);
      VectorNd w = new VectorNd(size);

      M.set (Mvals);
      q.set (qvals);

      testSolver (z, w, null, M, q, lo, hi, nub, size, Status.SOLVED);
   }

   public void testSolver (
      VectorNd z, VectorNi state, MatrixNd M, VectorNd q, Status expectedStatus) {
      Status status;

      int[] statebuf;
      if (state != null) {
         state.setSize (q.size());
      }
      else {
         state = new VectorNi (q.size());
      }
      statebuf = state.getBuffer();

      LCPSolver solver = getSolver();
      setStateToWLower (state);
      status = solver.solve (z, state, M, q);
      if (!myMaskPivotCounting) {
         myTotalPivots += solver.getPivotCount(); 
         myTotalIters += solver.getIterationCount(); 
         myMaxSolveTol = Math.max (2*solver.getLastSolveTol(), myMaxSolveTol);
      }
      if (expectedStatus == Status.SOLVED &&
          status != expectedStatus) { // perturb the problem
         int cnt = 10;
         int k = 0;
         double mag = q.infinityNorm();
         do {
            VectorNd fuzz = new VectorNd (q.size());
            fuzz.setRandom (-mag * 1e-13, mag * 1e-13);
            q.add (fuzz);
            setStateToWLower (state);
            status = solver.solve (z, state, M, q);
            mag *= 10;
            k++;
         }
         while (k < cnt && status != Status.SOLVED);
         if (!mySilentP) {
            System.out.println ("random retry level " + k);
         }
      }
      if (status != expectedStatus) {
         System.out.println ("M=\n" + M.toString ("%g"));
         System.out.println ("q=\n" + q.toString ("%g"));
         throw new TestException ("solver returned " + status + ", expected "
         + expectedStatus);
      }
      if (status == Status.SOLVED) { // check the solution
         checkLCPSolution (z, M, q, state, solver.getLastSolveTol());
         if (solver.isWarmStartSupported()) {
            // make sure if we restart with the same state we solve in one
            // iteration and 0 pivots
            VectorNi state0 = new VectorNi(state);
            VectorNd z0 = new VectorNd(z);
            status = solver.solve (z, state, M, q);
            if (status != Status.SOLVED) {
               throw new TestException ("Warm resolve failed");
            }
            int niters = solver.getIterationCount();
            int npivs = solver.getPivotCount();
            if (niters > 1 || npivs > 0) {
               throw new TestException (
                  "Warm resolve required "+niters+
                  " iterations and "+npivs+" pivots");
            }
            if (!vectorsEqual (z, z0, 1e-8)) {
                throw new TestException ("Warm resolve produced different z");
            }
            if (!state.equals (state0)) {
               throw new TestException ("Warm resolve produced state");
            }
         }
      }
   }

   public void testSolver (
      VectorNd z, VectorNd w, VectorNi state, MatrixNd M, VectorNd q, 
      VectorNd lo, VectorNd hi, int nub, int size, Status expectedStatus) {
      Status status;
      
      if (state == null) {
         state = new VectorNi(size);
      }
      else {
         state.setSize (size);
      }

      LCPSolver solver = getSolver();
      status = solver.solve (z, w, state, M, q, lo, hi, nub);
      if (!myMaskPivotCounting) {
         myTotalPivots += solver.getPivotCount(); 
         myTotalIters += solver.getIterationCount(); 
         myMaxSolveTol = Math.max (solver.getLastSolveTol(), myMaxSolveTol);
      }
      if (expectedStatus == Status.SOLVED &&
          status != expectedStatus) { // perturb the problem
         int cnt = 1;
         int k = 0;
         double mag = q.infinityNorm();
         do {
            VectorNd fuzz = new VectorNd (q.size());
            fuzz.setRandom (-mag * 1e-13, mag * 1e-13);
            q.add (fuzz);
            status = solver.solve (z, w, state, M, q, lo, hi, nub);
            mag *= 10;
            k++;
         }
         while (k < cnt && status != Status.SOLVED);
         System.out.println ("random retry level " + k);
      }
      if (status != expectedStatus) { // System.out.println ("M=\n" +
                                       // M.toString("%10.6f"));
         // System.out.println ("q=\n" + q.toString("%10.6f"));
         System.out.println ("M=\n" + M.toString ("%16.12f"));
         System.out.println ("q=\n" + q.toString ("%16.12f"));
         System.out.println ("z=\n" + z.toString ("%16.12f"));
         System.out.println ("w=\n" + w.toString ("%16.12f"));
         System.out.println ("lo=\n" + lo.toString ("%16.12f"));
         System.out.println ("hi=\n" + hi.toString ("%16.12f"));
         System.out.println ("nub=" + nub);
         System.out.println ("state=" + LCPSolver.stateToString(state));

         throw new TestException ("solver returned " + status + ", expected "
         + expectedStatus);
      }
      if (status == Status.SOLVED) { // check the solution

         double mag = 0;
         for (int i=0; i<size; i++) {
            mag = Math.max (mag, Math.abs (z.get(i)));
            mag = Math.max (mag, Math.abs (w.get(i)));
         }
         double basetol = DOUBLE_PREC*mag*100;
         double tol = Math.max(solver.getLastSolveTol(), basetol);
         //tol = 1e-8;
         
         checkBLCPSolution (z, w, M, q, lo, hi, nub, state, tol);
         if (solver.isWarmStartSupported()) {
            // make sure if we restart with the same state we solve in one
            // iteration and 0 pivots
            VectorNi state0 = new VectorNi(state);
            VectorNd z0 = new VectorNd(z);
            VectorNd w0 = new VectorNd(w);
            status = solver.solve (z, w, state, M, q, lo, hi, nub);
            if (status != Status.SOLVED) {
               throw new TestException ("Warm resolve failed");
            }
            int niters = solver.getIterationCount();
            int npivs = solver.getPivotCount();
            if (niters > 1 || npivs > 0) {
               throw new TestException (
                  "Warm resolve required "+niters+
                  " iterations and "+npivs+" pivots");
            }
            if (!vectorsEqual (z, z0, 1e-8)) {
               System.out.println ("z=" +z);
               System.out.println ("z0=" +z0);
               VectorNd err = new VectorNd();
               err.sub (z, z0);
               System.out.println ("err=" + err.norm()/z0.norm());
               throw new TestException ("Warm resolve produced different z");
            }
            if (!vectorsEqual (w, w0, 1e-8)) {
               System.out.println ("w=" +w);
               System.out.println ("w0=" +w0);
               VectorNd err = new VectorNd();
               err.sub (w, w0);
               System.out.println ("err=" + err.norm()/w0.norm());
               throw new TestException ("Warm resolve produced different w");
            }
            if (!state.equals (state0)) {
               throw new TestException ("Warm resolve produced state");
            }
         }
      }
   }

   boolean vectorsEqual (VectorNd v1, VectorNd v2, double tol) {
      double mag = (v1.norm()+v2.norm())/2;
      if (mag > tol) {
         tol = mag*tol;
      }
      return v1.epsilonEquals (v2, tol);
   }

   /**
    * Create a test case involving multi-point contact of a box on a plane. The
    * angle of the plane normal relative to the horizontal is ang.
    * @param ang angle that the applied force makes with the plane
    * @param regularize if {@code true}, regularize the constraints
    */
    public void testPlanarContact (double ang, boolean regularize) {
      double mass = 4.0;
      SpatialInertia Inertia =
         SpatialInertia.createBoxInertia (mass, 2.0, 1.0, 2.0);
      Point3d[] pnts =
         new Point3d[] { new Point3d (1.0, -0.5, 1.0),
                        new Point3d (1.0, -0.5, -1.0),
                        new Point3d (-2.0, -0.5, -1.0),
                        new Point3d (-1.0, -0.5, 1.0),
                        new Point3d (0.0, -0.5, 1.5),
         };
      int nump = pnts.length;

      Wrench[] constraints = new Wrench[nump];
      for (int i = 0; i < pnts.length; i++) {
         Wrench NT = new Wrench();
         NT.f.set (Vector3d.Y_UNIT);
         NT.m.cross (pnts[i], NT.f);
         constraints[i] = NT;
      }

      Twist vel0 = new Twist (Math.sin (ang), -Math.cos (ang), 0, 0, 0, 0);
      vel0.scale (9.8);

      Twist tw = new Twist();
      Wrench wr = new Wrench();

      MatrixNd M = new MatrixNd (nump, nump);
      VectorNd q = new VectorNd (nump);
      VectorNd z = new VectorNd (nump);

      buildLCP (M, q, Inertia, vel0, constraints, 0, null, 0);
      if (regularize) {
         // choose the compliance c to correspond to penetration of 0.001 under
         // f = 9.8 m, so that c = 0.001/(9.8 m), and then set Rn = c/h^2, with
         // h = 0.01L
         double Rn = (0.001)/(9.8*mass)/(0.01*0.01);
         // Create an M with no regularization, and M z = q to get a z value
         // from which we can then set bn = - R z, ensuring that velocities in
         // the constraint directions are zero.

         SVDecomposition svd = new SVDecomposition(M);
         svd.solve (z, q, 1e-12);
         q.scaledAdd (Rn, z);
         // add RN to M
         for (int i=0; i<nump; i++) {
            M.add (i, i, Rn);
         }
      }

      testSolver (z, null, M, q, Status.SOLVED);
      // compute constraint-adjusted velocity
      Twist vel = new Twist (vel0);
      for (int i = 0; i < nump; i++) {
         wr.scale (z.get(i), constraints[i]);
         Inertia.mulInverse (tw, wr);
         vel.add (tw);
      }
      for (int i = 0; i < nump; i++) {
         double dot = constraints[i].dot(vel);
         if (Math.abs (dot) > 1e-8) {
            throw new TestException (
               "velocity along constraint direction "+i+" is not 0");
         }
      }
   }

   /**
    * Create a test case involving a single point contact on a plane with
    * friction. The angle of the plane surface relative to the horizontal is
    * ang, and the friction coefficient is mu.
    */
   public void testSinglePointFrictionContact (double ang, double mu) {
      double h = 1.0; // time step
      double mass = 1.0;
      double theEst = Math.abs (9.8 * h * Math.cos (ang));
      Vector3d nrml = new Vector3d (Math.sin (ang), Math.cos (ang), 0);
      Vector3d dir0 = new Vector3d (Math.cos (ang), -Math.sin (ang), 0);
      Vector3d dir1 = new Vector3d (0, 0, 1);
      Vector3d vel0 = new Vector3d();
      vel0.scale (9.8 * Math.sin (ang), dir0);

      MatrixNd M = new MatrixNd (3, 3);
      VectorNd q = new VectorNd (3);
      VectorNd z = new VectorNd (3);
      VectorNd w = new VectorNd (3);
      VectorNd lo = new VectorNd (3);
      VectorNd hi = new VectorNd (3);

      M.set (0, 0, nrml.dot (nrml) / mass);
      M.set (0, 1, nrml.dot (dir0) / mass);
      M.set (0, 2, nrml.dot (dir1) / mass);

      M.set (1, 0, dir0.dot (nrml) / mass);
      M.set (1, 1, dir0.dot (dir0) / mass);
      M.set (1, 2, dir0.dot (dir1) / mass);

      M.set (2, 0, dir1.dot (nrml) / mass);
      M.set (2, 1, dir1.dot (dir0) / mass);
      M.set (2, 2, dir1.dot (dir1) / mass);

      q.set (0, nrml.dot (vel0));
      q.set (1, dir0.dot (vel0));
      q.set (2, dir1.dot (vel0));

      lo.set (0, 0);
      lo.set (1, -theEst * mu);
      lo.set (2, -theEst * mu);

      hi.set (0, INF);
      hi.set (1, theEst * mu);
      hi.set (2, theEst * mu);

      testSolver (z, w, null, M, q, lo, hi, 0, 3, Status.SOLVED);

      Vector3d vel = new Vector3d (vel0);
      vel.scaledAdd (z.get (0), nrml);
      vel.scaledAdd (z.get (1), dir0);
      vel.scaledAdd (z.get (2), dir1);

      if (Math.abs (z.get (0)) > 1e-8 || Math.abs (z.get (2)) > 1e-8) {
         throw new TestException ("Only z(1) should be non-zero");
      }
      if (ang <= Math.atan (mu)) {
         if (!vel.epsilonEquals (Vector3d.ZERO, 1e-8)) {
            throw new TestException ("velocity should be 0 with ang="
            + Math.toDegrees (ang));
         }
      }
      else {
         if (vel.epsilonEquals (Vector3d.ZERO, 1e-8)) {
            throw new TestException ("velocity should be non-zero with ang="
            + Math.toDegrees (ang));
         }
         if (Math.abs (Math.abs (z.get (1)) - theEst * mu) > 1e-8) {
            throw new TestException ("friction force not on the cone");
         }
      }
   }

   /**
    * Create a test case involving multi-point contact of a box on a plane with
    * friction. The angle of the plane normal relative to the horizontal is
    * ang, and the friction coefficient is mu.
    * @param ang angle that the applied force makes with the plane
    * @param mu friction coefficient
    * @param regularize if {@code true}, regularize the constraints
    */
   public void testPlanarFrictionContact (
      double ang, double mu, boolean regularize) {
      double mass = 4.0;
      SpatialInertia Inertia =
         SpatialInertia.createBoxInertia (mass, 2.0, 1.0, 2.0);
      Point3d[] pnts =
         new Point3d[] {
            new Point3d (1.0, -0.5, 1.0),
            new Point3d (1.0, -0.5, -1.0),
            new Point3d (-2.0, -0.5, -1.0),
         };
      int nump = pnts.length;
      int numc = 3 * nump;

      Wrench[] contact = new Wrench[nump];
      Wrench[] friction = new Wrench[2*nump];
      for (int i = 0; i < nump; i++) {
         Wrench NT = new Wrench();
         NT.f.set (Vector3d.Y_UNIT);
         NT.m.cross (pnts[i], NT.f);
         contact[i] = NT;

         Wrench DT = new Wrench();
         DT.f.set (Vector3d.X_UNIT);
         DT.m.cross (pnts[i], DT.f);
         friction[2*i] = DT;

         DT = new Wrench();
         DT.f.set (Vector3d.Z_UNIT);
         DT.m.cross (pnts[i], DT.f);
         friction[2*i + 1] = DT;
      }

      Twist vel0 = new Twist (Math.sin (ang), -Math.cos (ang), 0, 0, 0, 0);
      vel0.scale (9.8);


      MatrixNd M = new MatrixNd ();
      VectorNd q = new VectorNd ();

      buildLCP (M, q, Inertia, vel0, contact, 0, null, 0);
      VectorNd z = new VectorNd (nump);
      // solve contact only problem to estimate contact forces
      CholeskyDecomposition chol = new CholeskyDecomposition();
      chol.factor (M);
      q.negate();
      chol.solve (z, q);

      VectorNd lo = new VectorNd (numc);
      VectorNd hi = new VectorNd (numc);

      double Rd = 0;
      if (regularize) {
         // only need to regularize Rd, since N is not redundant. 
         Rd = 1e-6;
      }

      for (int i = 0; i < nump; i++) {
         lo.set (i, 0);
         hi.set (i, INF);

         lo.set (nump + 2*i, -z.get(i) * mu);
         hi.set (nump + 2*i, z.get(i) * mu);
         lo.set (nump + 2*i + 1, -z.get(i) * mu);
         hi.set (nump + 2*i + 1, z.get(i) * mu);
      }
      z.setSize (numc);
      VectorNd w = new VectorNd (numc);

      buildLCP (M, q, Inertia, vel0, contact, 0, friction, Rd);
      testSolver (z, w, null, M, q, lo, hi, 0, numc, Status.SOLVED);

      // compute constraint-adjusted velocity
      Twist vel = new Twist (vel0);
      Wrench wr = new Wrench();
      Twist tw = new Twist();
      for (int i=0; i<nump; i++) {
         wr.scale (z.get(i), contact[i]);
         Inertia.mulInverse (tw, wr);
         vel.add (tw);
      }
      for (int i=0; i<2*nump; i++) {
         wr.scale (z.get(nump+i), friction[i]);
         Inertia.mulInverse (tw, wr);
         vel.add (tw);
      }

      for (int i=0; i<nump; i++) {
         if (Math.abs(contact[i].dot(vel)) > 1e-8) {
            throw new TestException (
               "velocity along constraint direction "+i+" is not 0");
         }
      }
      if (ang <= Math.atan (mu)) {
         double tol = Math.max (1e-8, 10*Rd);
         if (!vel.epsilonEquals (Twist.ZERO, tol)) {
            System.out.println ("vel=" + vel);
            throw new TestException ("velocity should be 0 with ang="
            + Math.toDegrees (ang));
         }
      }
      else {
         if (vel.epsilonEquals (Twist.ZERO, 1e-8)) {
            throw new TestException ("velocity should be non-zero with ang="
            + Math.toDegrees (ang));
         }
      }
   }

   void buildLCP (
      MatrixNd M, VectorNd q, SpatialInertia inertia, Twist vel0,
      Wrench[] contact, double Rn, Wrench[] friction, double Rd) {

      int nc = contact.length;
      int nf = (friction != null ? friction.length : 0);
      int n = nc+nf;
      Wrench[] constraints = new Wrench[n];
      for (int i=0; i<nc; i++) {
         constraints[i] = contact[i];
      }
      for (int i=0; i<nf; i++) {
         constraints[nc+i] = friction[i];
      }
      M.setSize (n, n);
      q.setSize (n);
      Twist tw = new Twist();
      for (int i=0; i<n; i++) {
         for (int j=0; j<n; j++) {
            inertia.mulInverse (tw, constraints[j]);
            M.set (i, j, constraints[i].dot (tw));
         }
         q.set (i, constraints[i].dot (vel0));
      }
      for (int i=0; i<nc; i++) {
         M.add (i, i, Rn);
      }
      for (int i=nc; i<nc+nf; i++) {
         M.add (i, i, Rd);
      }
   }

   static void buildLCP (
      MatrixNd A, VectorNd q,
      SparseBlockMatrix M, VectorNd bm,
      SparseBlockMatrix NT, VectorNd Rn, VectorNd bn,
      SparseBlockMatrix DT, VectorNd Rd, VectorNd bd) {

      CholeskyDecomposition chol = new CholeskyDecomposition(M);

      int sizeN = (NT != null ? NT.colSize() : 0);
      int sizeD = (DT != null ? DT.colSize() : 0);

      int n = sizeN + sizeD;
      A.setSize (n, n);
      q.setSize (n);

      VectorNd coli = new VectorNd (M.rowSize());
      VectorNd colj = new VectorNd (M.rowSize());

      VectorNd[] cols = new VectorNd[n];

      for (int i=0; i<n; i++) {
         VectorNd col = new VectorNd(6);
         if (i < sizeN) {
            NT.getColumn (i, col);
         }
         else {
            DT.getColumn (i-sizeN, col);
         }
         cols[i] = col;
      }

      // compute A
      VectorNd sol = new VectorNd(6);
      for (int j=0; j<n; j++) {
         chol.solve (sol, cols[j]);
         for (int i=0; i<n; i++) {
            A.set (i, j, cols[i].dot(sol));
            if (i == j) {
               if (i < sizeN) {
                  if (Rn != null) {
                     A.add (i, i, Rn.get(i));
                  }
               }
               else {
                  if (Rd != null) {
                     A.add (i, i, Rd.get(i-sizeN));
                  }
               }
            }
         }
      }

      // compute q
      chol.solve (sol, bm);
      for (int i=0; i<n; i++) {
         q.set (i, cols[i].dot(sol));
         if (i < sizeN) {
            if (bn != null) {
               q.add (i, -bn.get(i));
            }
         }
         else {
            if (bd != null) {
               q.add (i, -bd.get(i-sizeN));
            }
         }
      }
   }

   static void createPegInHoleProblem (
      SparseBlockMatrix M, VectorNd bm,
      SparseBlockMatrix NT, VectorNd Rn,
      SparseBlockMatrix DT, VectorNd Rd,
      int nz, // number of contact rings along z
      int nr, // Number of contacts on each ring
      Vector3d force,
      boolean regularize) {

      if (M.numBlockRows() != 1 || M.getBlockRowSize(0) != 6 ||
          NT.numBlockRows() != 1 || NT.getBlockRowSize(0) != 6 ||
          DT.numBlockRows() != 1 || DT.getBlockRowSize(0) != 6) {
         throw new IllegalArgumentException (
            "M, NT and NT must have 0 columns and 1 block row of size 6");
      }

      // Contacts are arranged on rings placed uniformly along the z axis.
      double rad = 0.05; // radius of the peg
      double len = 0.20; // length of the peg

      double mass = 2.0;
      SpatialInertia Inertia =
         SpatialInertia.createCylinderInertia (mass, rad, len);

      M.addBlock (0, 0, Inertia);

      int numc = nz*nr; // number of contact constraints
      int numf = 2*numc; // number of friction constraints

      Wrench wr = new Wrench();
      for (int i=0; i<nz; i++) {
         double z = -len/2 + i*(len/(nz-1));
         for (int j=0; j<nr; j++) {
            int k = i*nr + j;
            double ang = j*2*Math.PI/nr;
            double c = Math.cos(ang);
            double s = Math.sin(ang);

            Point3d pnt = new Point3d (rad*c, rad*s, z);

            Matrix6x1Block Nblk = new Matrix6x1Block();
            wr.f.set (-c, -s, 0); // normal
            wr.m.cross (pnt, wr.f);
            Nblk.setColumn (0, wr);
            NT.addBlock (0, k, Nblk);

            Matrix6x2Block Dblk = new Matrix6x2Block();
            wr.f.set (-s, c, 0); // radial tangent
            wr.m.cross (pnt, wr.f);
            Dblk.setColumn (0, wr);
            wr.f.set (Vector3d.Z_UNIT); 
            wr.m.cross (pnt, wr.f);
            Dblk.setColumn (1, wr);
            DT.addBlock (0, k, Dblk);
         }
      }

      //Twist vel0 = new Twist ();
      Wrench fw = new Wrench (force, new Vector3d());
      //Inertia.mulInverse (vel0, fw);
      bm.set (fw);

      Rn.setSize (numc);
      Rd.setSize (numf);
      if (regularize) {
         // choose the compliance c to correspond to penetration of 0.001 under
         // a force of 10 m, so that c = 0.001/(10 m), and then set Rn = c/h^2,
         // with h = 0.01
         double rn = (0.001)/(10*mass)/(0.01*0.01);
         double rd = 1e-2;
         Rn.setAll (rn);
         Rd.setAll (rd);
      }
   }

   /**
    * Create a test case involving peg-in-hole contact with many contact
    * points and friction.
    * @param nz number of rings of contact points along z
    * @param nr number of contact points about each ring
    * @param force to apply to the center of the peg
    * @param mu friction coefficient
    * @param regularize if {@code true}, regularize the constraints
    */
   public void testPegInHoleContact (
      int nz, int nr, Vector3d force, double mu, boolean regularize) {

      SparseBlockMatrix M = new SparseBlockMatrix (new int[]{6}, new int[0]);
      SparseBlockMatrix NT = new SparseBlockMatrix (new int[]{6}, new int[0]);
      SparseBlockMatrix DT = new SparseBlockMatrix (new int[]{6}, new int[0]);
      VectorNd bm = new VectorNd();
      VectorNd Rn = new VectorNd();
      VectorNd Rd = new VectorNd();

      createPegInHoleProblem (
         M, bm, NT, Rn, DT, Rd, nz, nr, force, regularize);

      int numc = NT.colSize();
      VectorNd bn = new VectorNd(numc);
      VectorNd z = new VectorNd (numc);
      VectorNi state = new VectorNi (numc);

      MatrixNd A = new MatrixNd();
      VectorNd q = new VectorNd();

      // first solve a contact only problem to estimate contact force

      buildLCP (A, q, M, bm, NT, Rn, bn, null, null, null);


      myMaskPivotCounting = true;
      testSolver (z, state, A, q, Status.SOLVED);
      myMaskPivotCounting = false;

      int nnz = 0;
      for (int i=0; i<state.size(); i++) {
         if (state.get(i) == LCPSolver.Z_VAR) {
            nnz++;
         }
      }
      int n = numc + 2*nnz;
      
      z.setSize (n);
      VectorNd w = new VectorNd (n);
      VectorNd lo = new VectorNd (n);
      VectorNd hi = new VectorNd (n);
      SparseBlockMatrix DTZ = new SparseBlockMatrix (new int[]{6}, new int[0]);
      VectorNd Rdz = new VectorNd ();

      int k = 0;
      for (int i=0; i<numc; i++) {
         lo.set (i, 0);
         hi.set (i, INF);
         if (state.get(i) == LCPSolver.Z_VAR) {
            double zval = z.get(i);

            MatrixBlock blk =  DT.getBlock (0, i).clone();
            DTZ.addBlock (0, k/2, blk);
            lo.set (numc + k, -zval*mu);
            hi.set (numc + k, zval*mu);
            Rdz.append (Rd.get(2*i));
            k++;
            lo.set (numc + k, -zval*mu);
            hi.set (numc + k, zval*mu);
            Rdz.append (Rd.get(2*i+1));
            k++;
         }
      }
      VectorNd bd = new VectorNd (Rdz.size());

      buildLCP (A, q, M, bm, NT, Rn, bn, DTZ, Rdz, bd);
      MurtyLCPSolver murty = new MurtyLCPSolver();
      state.setSize (n);
      Status status = murty.solve (z, w, state, A, q, lo, hi, 0);
      check ("status != SOLVED", status == Status.SOLVED);
      // checkBLCPSolution (
      //    z, w, A, q, lo, hi, 0, state, murty.getLastSolveTol());
                         

      testSolver (z, w, state, A, q, lo, hi, 0, n, Status.SOLVED);
      //System.out.println (LCPSolver.stateToString(state));
   }

   public void testPegInHoleContactOld (
      Vector3d force, double mu, boolean regularize) {

      // Contacts are arranged on rings placed uniformly along the z axis.
      double rad = 0.05; // radius of the peg
      double len = 0.20; // length of the peg
      int nz = 5;        // number of contact rings along z
      int nr = 7;        // number of contacts on each ring

      double mass = 2.0;
      SpatialInertia Inertia =
         SpatialInertia.createCylinderInertia (mass, rad, len);

      int numc = nz*nr; // number of contact constraints
      int numf = 2*numc; // number of friction constraints

      Wrench[] contact = new Wrench[numc];
      Wrench[] friction = new Wrench[numf];

      double Rn = 0;
      double Rd = 0;
      if (regularize) {
         // choose the compliance c to correspond to penetration of 0.001 under
         // |f|, so that c = 0.001/|f|, and then set Rn = c/h^2, with h = 0.01
         Rn = (0.001)/force.norm()/(0.01*0.01);
         Rd = 1e-6;
      }

      for (int i=0; i<nz; i++) {
         double z = -len/2 + i*(len/(nz-1));
         for (int j=0; j<nr; j++) {
            int k = i*nr + j;
            double ang = j*2*Math.PI/nr;
            double c = Math.cos(ang);
            double s = Math.sin(ang);

            Point3d pnt = new Point3d (rad*c, rad*s, z);
            
            Wrench NT = new Wrench();
            NT.f.set (-c, -s, 0); // normal
            NT.m.cross (pnt, NT.f);
            contact[k] = NT;
            
            Wrench DT = new Wrench();
            DT.f.set (-s, c, 0); // radial tangent
            DT.m.cross (pnt, DT.f);
            friction[2*k] = DT;
            
            DT = new Wrench();
            DT.f.set (Vector3d.Z_UNIT); 
            DT.m.cross (pnt, DT.f);
            friction[2*k + 1] = DT;           
         }
      }

      Twist vel0 = new Twist ();
      Wrench fw = new Wrench (force, new Vector3d());
      Inertia.mulInverse (vel0, fw);

      // first solve a contact only problem to estimate contact force
      MatrixNd M = new MatrixNd ();
      VectorNd q = new VectorNd ();

      buildLCP (M, q, Inertia, vel0, contact, Rn, null, 0);

      VectorNd z = new VectorNd (numc);
      VectorNi state = new VectorNi (numc);
      myMaskPivotCounting = true;
      testSolver (z, state, M, q, Status.SOLVED);
      myMaskPivotCounting = false;

      int nnz = 0;
      for (int i=0; i<state.size(); i++) {
         if (state.get(i) == LCPSolver.Z_VAR) {
            nnz++;
         }
      }
      int n = numc + 2*nnz;
      
      z.setSize (n);
      VectorNd w = new VectorNd (n);
      VectorNd lo = new VectorNd (n);
      VectorNd hi = new VectorNd (n);
      Wrench[] fcons = new Wrench[2*nnz];

      int k = 0;
      for (int i=0; i<state.size(); i++) {
         lo.set (i, 0);
         hi.set (i, INF);
         if (state.get(i) == LCPSolver.Z_VAR) {
            double zval = z.get(i);

            fcons[k] = friction[2*i];
            lo.set (numc + k, -zval*mu);
            hi.set (numc + k, zval*mu);
            k++;
            fcons[k] = friction[2*i+1];
            lo.set (numc + k, -zval*mu);
            hi.set (numc + k, zval*mu);
            k++;
         }
      }
      buildLCP (M, q, Inertia, vel0, contact, Rn, fcons, Rd);
      testSolver (z, w, state, M, q, lo, hi, 0, n, Status.SOLVED);
      //System.out.println (LCPSolver.stateToString(state));
   }

   public static void checkBLCPSolution (
      VectorNd z, VectorNd w, MatrixNd M, VectorNd q,
      VectorNd lo, VectorNd hi, int nub, VectorNi state, double tol) {
      
      TestException failException = null;
      int n = z.size();
      VectorNd wcheck = new VectorNd (n);
      wcheck.mul (M, z);
      wcheck.add (q);

      for (int i = 0; i < n; i++) {
         if (Math.abs (w.get(i) - wcheck.get(i)) > tol) {
            throw new TestException (
               "w["+i+"]=" + w.get(i) + ", expected " + wcheck.get(i) +
                  ", tol=" + tol);
         }
      }
      checkBLCPSolution (z, w, lo, hi, nub, state, tol);
   }

   public static void checkBLCPSolution (
      VectorNd z, VectorNd w, VectorNd lo,
      VectorNd hi, int nub, VectorNi state, double tol) {
      
      TestException failException = null;
      int n = z.size();

      for (int i = 0; i < n; i++) {
         double l = lo.get(i);
         double h = hi.get(i);
         double zval = z.get(i);
         double wval = w.get(i);

         if (z.get(i) < -tol + l || z.get(i) > tol + h) {
            failException =
               new TestException (
                  "z[" + i + "]=" + z.get(i) 
                  + " is out of bounds " + l + "," + h + ", tol=" + tol);
            break;
         }
         if (state.get(i) == LCPSolver.Z_VAR) {
            if (zval < -tol + l || zval > tol + h) {
               failException =
                  new TestException (
                     "z[" + i + "]=" + zval
                     + " is out of bounds " + l + "," + h + ", tol=" + tol);
               break;
            }
            if (Math.abs (wval) > tol) {
               failException =
                  new TestException (
                     "w[" + i + "]=" + wval
                     + " should be zero, tol=" + tol);
               break;
            }
         }
         else if (state.get(i) == LCPSolver.W_VAR_LOWER) {
            if (Math.abs (zval - l) > tol) {
               failException =
                  new TestException (
                     "z[" + i + "]=" + zval
                     + " should be at lower bound " + l + ", tol=" + tol); 
               break;
            }
            if (wval < -tol) {
               failException =
                  new TestException (
                     "w[" + i + "]=" + wval
                     + " should be positive, tol=" + tol);
               break;
            }
         }
         else if (state.get(i) == LCPSolver.W_VAR_UPPER) {
            if (Math.abs (zval - h) > tol) {
               failException =
                  new TestException (
                     "z[" + i + "]=" + zval
                     + " should be at upper bound " + h + ", tol=" + tol);
               break;
            }
            if (wval > tol) {
               failException =
                  new TestException (
                     "w[" + i + "]=" + wval
                     + " should be negative, tol=" + tol);
               break;
            }
         }
         else {
            failException =
               new TestException (
                  "state[" + i + "]=" + state.get(i)
                  + " is unknown");
            break;
         }
      }
      if (failException != null) {
         System.out.println ("z=\n" + z.toString ("%12.8f"));
         System.out.println ("w=\n" + w.toString ("%12.8f"));
         System.out.println ("lo=\n" + lo.toString ("%12.8f"));
         System.out.println ("hi=\n" + hi.toString ("%12.8f"));
         System.out.println ("nub=" + nub);
         System.out.println ("state=" + LCPSolver.stateToString(state));
         throw failException;
      }
   }

   public static void checkLCPSolution (
      VectorNd z, MatrixNd M, VectorNd q, VectorNi state,
      double solverTol) {
      
      TestException failException = null;
      int n = z.size();
      VectorNd w = new VectorNd (n);
      w.mul (M, z);
      w.add (q);
      double mag = 0;
      for (int i = 0; i < n; i++) {
         mag = Math.max (mag, Math.abs (z.get(i)));
         mag = Math.max (mag, Math.abs (w.get(i)));
      }
      double basetol = DOUBLE_PREC*mag*100;
      double tol = Math.max(2*solverTol, basetol);

      for (int i = 0; i < n; i++) {
         if (z.get(i) < -tol) {
            failException =
               new TestException ("z("+i+")="+z.get(i)+", tol="+tol);
         }
         else if (w.get(i) < -tol) {
            failException =
               new TestException ("w("+i+")="+w.get(i)+", tol="+tol);
         }
         if (state.get(i) == LCPSolver.Z_VAR && w.get(i) > tol) {
            failException =
               new TestException ("w > 0 for z basic");
         }
         if (state.get(i) != LCPSolver.Z_VAR && z.get(i) > tol) {
            failException =
               new TestException ("z > 0 when not basic");
         }
      }
      if (failException != null) {
         System.out.println ("M=\n" + M.toString ("%12.8f"));
         System.out.println ("q=\n" + q.toString ("%12.8f"));
         System.out.println ("z=\n" + z.toString ("%12.8f"));
         System.out.println ("w=\n" + w.toString ("%12.8f"));
         System.out.println (
            "state=" + LCPSolver.stateToString (state));
         throw failException;
      }
   }

   public static void checkLCPSolutionX (
      VectorNd z, MatrixNd M, VectorNd q, int nub, VectorNi state, double tol) {
      
      TestException failException = null;
      int n = z.size();
      VectorNd w = new VectorNd (n);
      w.mul (M, z);
      w.add (q);

      for (int i = 0; i < n; i++) {

         double zval = z.get(i);
         double wval = w.get(i);

         if (zval < -tol) {
            failException = new TestException (
               "z[" + i + "]=" + zval+ " is < 0");
            break;
         }
         if (wval < -tol) {
            failException = new TestException (
               "w[" + i + "]=" + wval + " is < 0");
            break;
         }
         if (state.get(i) == LCPSolver.Z_VAR) {
            if (Math.abs (wval) > tol) {
               failException =
                  new TestException (
                     "w[" + i + "]=" + wval + " should be 0");
               break;
            }
         }
         else if (state.get(i) == LCPSolver.W_VAR_LOWER) {
            if (zval > tol) {
               failException =
                  new TestException (
                     "z[" + i + "]=" + zval + " should be 0");
               break;
            }
         }
         else {
            failException =
               new TestException (
                  "Illegal state (code "+state.get(i)+") at i=" + i);
         }
      }
      if (failException != null) {
         System.out.println ("M=\n" + M.toString ("%12.8f"));
         System.out.println ("q=\n" + q.toString ("%12.8f"));
         System.out.println ("z=\n" + z.toString ("%12.8f"));
         System.out.println ("w=\n" + w.toString ("%12.8f"));
         System.out.println ("state=" + LCPSolver.stateToString(state));
         throw failException;
      }
   }

   protected void setRandomBounds (VectorNd lo, VectorNd hi, int nub, int size) {
      for (int i=0; i<size; i++) {
         if (i < nub) {
            lo.set (i, -INF);
            hi.set (i, INF);
         }
         else {
            switch (myRandom.nextInt (3)) {
               case 0: {
                  lo.set (i, 0);
                  hi.set (i, INF);
                  break;
               }
               case 1: {
                  lo.set (i, -1);
                  hi.set (i, 1);
                  break;
               }
               case 2: {
                  lo.set (i, -INF);
                  hi.set (i, 0);
                  break;
               }
            }
         }
      }
   }

   protected void createRandomBLCP (
      MatrixNd M, VectorNd q, VectorNd lo, VectorNd hi,
      int nub, int size, boolean semiDefinite) {

      M.setSize (size, size);
      q.setSize (size);
      VectorNd z = new VectorNd (size);
      VectorNd x = new VectorNd (size);
      VectorNd y = new VectorNd (size);
      VectorNd w = new VectorNd (size);

      int maxz; // max number of variables to set to Z basic, excluding nub
      if (semiDefinite) {
         MatrixNd N = new MatrixNd (size, size/2);
         N.setRandom();
         M.mulTransposeRight (N, N);
         if (nub > size/4) {
            nub = size/4;
         }
         maxz = myRandom.nextInt (size/2-nub+1);
      }
      else {
         M.setRandom();
         M.mulTransposeRight (M, M);
         if (nub > size/2) {
            nub = size/2;
         }
         maxz = myRandom.nextInt (size-nub+1);
      }
      CholeskyDecomposition chol = new CholeskyDecomposition (0);
      double tol = 1e-9;
      VectorNd col = new VectorNd(size);
      int numz = 0;
      int[] zidxs = new int[size];
      int[] state = new int[size];
      for (int j=0; j<nub; j++) {
         col.setSize (numz+1);
         for (int i=0; i<numz; i++) {
            col.set (i, M.get (zidxs[i], j));
         }
         col.set (numz, M.get (j, j));
         if (chol.addRowAndColumn (col, tol)) {
            zidxs[numz++] = j;
            state[j] = LCPSolver.Z_VAR;
         }
         else {
            nub = numz;
            System.out.println ("ADD col failed");
            break;
         }
      }
      int[] rperm = MatrixTest.randomPermutation (size-nub);
      int[] perm = new int[size];
      for (int i=0; i<size; i++) {
         if (i < nub) {
            perm[i] = i;
         }
         else {
            perm[i] = rperm[i-nub]+nub;
         }
      }
      for (int j=nub; j<size && numz<maxz+nub; j++) {
         col.setSize (numz+1);
         for (int i=0; i<numz; i++) {
            col.set (i, M.get (zidxs[i], perm[j]));
         }
         col.set (numz, M.get (perm[j], perm[j]));
         if (chol.addRowAndColumn (col, tol)) {
            zidxs[numz++] = perm[j];
            state[perm[j]] = LCPSolver.Z_VAR;
         }
         else {
            System.out.println ("ADD col failed");
         }
      }
      for (int i=0; i<size; i++) {
         if (state[i] == 0) {
            if (lo.get(i) == -INF) {
               // W_VAR_LOWER not possible
               state[i] = LCPSolver.W_VAR_UPPER;
               z.set (i, hi.get(i));             
            }
            else if (hi.get(i) == INF) {
               // W_VAR_UPPER not possible
               state[i] = LCPSolver.W_VAR_LOWER;
               z.set (i, lo.get(i));             
            }
            else {
               if (myRandom.nextBoolean()) {
                  state[i] = LCPSolver.W_VAR_LOWER;
                  z.set (i, lo.get(i));
               }
               else {
                  state[i] = LCPSolver.W_VAR_UPPER;
                  z.set (i, hi.get(i));
               }
            }
         }
         else { // z state
            if (lo.get(i) == -INF && hi.get(i) == INF) {
               z.set (i, RandomGenerator.nextDouble (-1,1)); 
            }
            else if (hi.get(i) == INF) {
               z.set (i, lo.get(i) + myRandom.nextDouble());
            }
            else if (lo.get(i) == -INF) {
               z.set (i, hi.get(i) - myRandom.nextDouble());
            }
            else {
               z.set (i, RandomGenerator.nextDouble (lo.get(i), hi.get(i)));
            }
         }
      }

      q.setRandom ();
      M.mul (w, z);
      w.add (q);
      for (int i=0; i<size; i++) {
         if (state[i] == LCPSolver.W_VAR_LOWER) {
            if (w.get(i) < 0) {
               q.set (i, q.get(i) - 2*w.get(i));
            }
         }
         else if (state[i] == LCPSolver.W_VAR_UPPER) {
            if (w.get(i) > 0) {
               q.set (i, q.get(i) - 2*w.get(i));
            }
         }
         else if (state[i] == LCPSolver.Z_VAR) {
            q.set (i, q.get(i)-w.get(i));
         }
      }
      M.mul (w, z);
      w.add (q);

      //checkBLCPSolution (z, w, M, q, lo, hi, state, tol);
   }


   public void randomBLCPTests (int ntests, int msize, boolean semiDefinite) {

      MatrixNd M = new MatrixNd (msize, msize);
      VectorNd q = new VectorNd (msize);
      VectorNd z = new VectorNd (msize);
      VectorNd w = new VectorNd (msize);
      VectorNd hi = new VectorNd (msize);
      VectorNd lo = new VectorNd (msize);
      int nub = 0;

      VectorNd x = new VectorNd (msize); // seed for cone projection

      System.out.println ("semiDefinite=" + semiDefinite);
      
      if (!semiDefinite) {
         for (int i = 0; i < ntests; i++) {
            if (i < ntests/2) {
               nub = 0;
            }
            else {
               nub = myRandom.nextInt (msize/2);
            }
            setRandomBounds (lo, hi, nub, msize);
            createRandomBLCP (M, q, lo, hi, nub, msize, /*SPSD=*/false);
            testSolver (
               z, w, null, M, q, lo, hi, nub, msize, LCPSolver.Status.SOLVED);
         }
      }
      else {
         MatrixNd N = new MatrixNd (msize, msize - msize / 2);
         x.setSize (msize - msize / 2);
      
         for (int i = 0; i < ntests; i++) {
            if (i < ntests/2) {
               nub = 0;
            }
            else {
               nub = myRandom.nextInt (msize/4);
            }
            setRandomBounds (lo, hi, nub, msize);
            createRandomBLCP (M, q, lo, hi, nub, msize, /*SPSD=*/true);
            testSolver (
               z, w, null, M, q, lo, hi, nub, msize, LCPSolver.Status.SOLVED);
         }
      }
   }

   public void randomTests (int ntests, int msize, boolean semiDefinite) {

      MatrixNd M = new MatrixNd (msize, msize);
      VectorNd q = new VectorNd (msize);
      VectorNd z = new VectorNd (msize);

      VectorNd x = new VectorNd (msize); // seed for cone projection
      
      System.out.println ("semiDefinite="+semiDefinite);
      
      if (!semiDefinite) {
         for (int i = 0; i < ntests; i++) {
         
            M.setRandom();
            x.setRandom();
            q.mul (M, x);
            M.mulTransposeRight (M, M);
            testSolver (z, null, M, q, LCPSolver.Status.SOLVED);
         }
      }
      else {
         MatrixNd N = new MatrixNd (msize, msize - msize / 2);
         x.setSize (msize - msize / 2);

         for (int i = 0; i < ntests; i++) {
            N.setRandom();
            x.setRandom();
            q.mul (N, x);
            M.mulTransposeRight (N, N);
            testSolver (z, null, M, q, LCPSolver.Status.SOLVED);
         }
      }
   }

   public void simpleContactTests (boolean regularize) {
      for (double ang=0; ang<Math.toRadians(45); ang += Math.toRadians(5)) {
         testPlanarContact (ang, regularize);
      }
      if (getSolver().isBLCPSupported()) {
         for (double ang=0; ang<Math.toRadians(45); ang += Math.toRadians(5)) {
            testSinglePointFrictionContact (ang, 0.3);
         }
         for (double ang=0; ang<Math.toRadians(45); ang += Math.toRadians(5)) {
            testPlanarFrictionContact (ang, 0.3, regularize);
         }
      }
   }

   public void pegInHoleContactTests (
      int nz, int nr, int ntests, boolean regularize) {
      for (int i=0; i<ntests; i++) {
         Vector3d force = new Vector3d();
         force.setRandom();
         double mu = RandomGenerator.nextDouble (0.2, 1.0);
         testPegInHoleContact (nz, nr, force, mu, regularize);
      }
   }

   public void randomTiming (int ntests, int msize) {

      int numPivots = 0;
      int numFailedPivots = 0;

      MatrixNd M = new MatrixNd (msize, msize);
      VectorNd q = new VectorNd (msize);
      VectorNd z = new VectorNd (msize);
      VectorNd x = new VectorNd (msize); // seed for cone projection

      VectorNd w = new VectorNd (msize);
      VectorNd hi = new VectorNd (msize);
      VectorNd lo = new VectorNd (msize);

      FunctionTimer timer = new FunctionTimer();

      for (int i = 0; i < ntests; i++) {
         M.setRandom();
         x.setRandom();
         q.mul (M, x);
         M.mulTransposeRight (M, M);
         timer.restart();
         getSolver().solve (z, /*state=*/null, M, q);
         timer.stop();
         numPivots += getSolver().getIterationCount();
         numFailedPivots += getSolver().numFailedPivots();
      }

      MatrixNd N = new MatrixNd (msize, msize - msize / 2);
      x.setSize (msize - msize / 2);

      for (int i = 0; i < ntests; i++) {
         N.setRandom();
         x.setRandom();
         q.mul (N, x);
         M.mulTransposeRight (N, N);
         timer.restart();
         getSolver().solve (z, /*state=*/null, M, q);
         timer.stop();
         numPivots += getSolver().getIterationCount();
         numFailedPivots += getSolver().numFailedPivots();
      }
      System.out.printf (
         "LCP averages for size %d: time=%s pivots=%g failed pivots=%g\n",
         msize, timer.result (2*ntests),
         numPivots/(double)ntests, numFailedPivots/(double)ntests);

      if (!getSolver().isBLCPSupported()) {
         return;
      }
      
      timer.reset();
      numPivots = 0;
      numFailedPivots = 0;

      for (int i = 0; i < ntests; i++) {
         int nub = 0;
         setRandomBounds (lo, hi, nub, msize);
         createRandomBLCP (M, q, lo, hi, nub, msize, /*SPSD=*/false);
         timer.restart();
         getSolver().solve (z, w, /*state=*/null, M, q, lo, hi, nub);
         timer.stop();
         numPivots += getSolver().getIterationCount();
         numFailedPivots += getSolver().numFailedPivots();
      }

      for (int i = 0; i < ntests; i++) {
         int nub = 0;
         setRandomBounds (lo, hi, nub, msize);
         createRandomBLCP (M, q, lo, hi, nub, msize, /*SPSD=*/true);
         timer.restart();
         getSolver().solve (z, w, /*state=*/null, M, q, lo, hi, nub);
         timer.stop();
         numPivots += getSolver().getIterationCount();
         numFailedPivots += getSolver().numFailedPivots();
      }
      System.out.printf (
         "BLCP averages for size %d: time=%s pivots=%g failed pivots=%g\n",
         msize, timer.result (2*ntests),
         numPivots/(double)ntests, numFailedPivots/(double)ntests);

   }

   protected void printUsageAndExit (int code) {
      System.out.println (
         "Usage: java "+getClass()+" [-verbose] [-timing] [-help]");
      System.exit (code); 
   }   

   protected void runtiming() {
      randomTiming (10000, 50);
   }

   protected void parseArgs (String[] args) {
      
      setSilent (true);
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-verbose")) {
            setSilent (false);
         }
         else if (args[i].equals ("-help")) {
            printUsageAndExit (0);
         }
         else if (args[i].equals ("-timing")) {
            myTimingRequested = true;
         }
         else {
            printUsageAndExit (1);
         }
      }
   }   

}
