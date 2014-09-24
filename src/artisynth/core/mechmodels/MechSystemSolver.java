/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3x1;
import maspack.matrix.Matrix6d;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixNd;
import maspack.matrix.PolarDecomposition3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.solvers.CGSolver;
import maspack.solvers.DirectSolver;
import maspack.solvers.IterativeSolver;
import maspack.solvers.IterativeSolver.ToleranceType;
import maspack.solvers.KKTSolver;
import maspack.solvers.PardisoSolver;
import maspack.solvers.UmfpackSolver;
import maspack.spatialmotion.Twist;
import maspack.util.FunctionTimer;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.util.ArtisynthIO;

/**
 * Implements implicit integration for MechSystem
 */
public class MechSystemSolver {
   MechSystem mySys;
   RigidBodySolver myRBSolver;

   public boolean profileKKTSolveTime = false;
   public boolean profileWholeSolve = false;

   //public static boolean useStiffnessPosProjection = true;
   public static boolean useVelProjection = true;
   public static boolean useGlobalFriction = true;
   public static boolean useFictitousJacobianForces = true;
   //public static boolean updateForcesAtStepEnd = true;

   private boolean myUpdateForcesAtStepEnd = false;

   // mass matrix stuff

   private int myMassVersion = -1;
   private double myMassTime = -1;
   private int myInverseMassVersion = -1;
   private double myInverseMassTime = -1;
   private boolean myMassConstantP = true;
   private SparseNumberedBlockMatrix myMass;    
   private VectorNd myMassForces;
   private SparseNumberedBlockMatrix myInverseMass;
   private VectorNd myBf = new VectorNd();

   // bilateral constraints

   SparseBlockMatrix myGT;
   private int myGsize = -1;
   private ConstraintInfo[] myGInfo = new ConstraintInfo[0];
   private VectorNd myGdot = new VectorNd();
   private VectorNd myRg = new VectorNd();
   private VectorNd myBg = new VectorNd();
   private VectorNd myLam = new VectorNd();
   private int myGTVersion = -1;

   // unilateral constraints

   SparseBlockMatrix myNT;
   private int myNsize = -1;
   private ConstraintInfo[] myNInfo = new ConstraintInfo[0];
   private VectorNd myNdot = new VectorNd();
   private VectorNd myRn = new VectorNd();
   private VectorNd myBn = new VectorNd();
   private VectorNd myThe = new VectorNd();

   // friction constraints

   SparseBlockMatrix myDT;
   private int myDsize = -1;
   VectorNd myBd = new VectorNd();
   VectorNd myPhi = new VectorNd();
   FrictionInfo[] myFrictionInfo = new FrictionInfo[0];

   protected PardisoSolver pardiso;
   protected UmfpackSolver umfpack;
   protected DirectSolver myDirectSolver;
   protected IterativeSolver myIterativeSolver;

   // auxiliary vectors for integrators

   private VectorNd myQ = new VectorNd (0); // active position
   private VectorNd myDqdt = new VectorNd (0); // active position derivative
   private VectorNd myU = new VectorNd (0); // active velocity
   private VectorNd myUtmp = new VectorNd (0); // active velocity temp
   private VectorNd myDudt = new VectorNd (0); // active velocity derivative

   // extra vectors for RungeKutta
   private VectorNd myQtmp = new VectorNd (0);
   private VectorNd myDqdtAvg = new VectorNd (0);
   private VectorNd myDudtAvg = new VectorNd (0);

   private VectorNd myQpar = new VectorNd(); // parametric positions
   private VectorNd myUpar = new VectorNd(); // parametric velocities
   private VectorNd myUpar0 = new VectorNd(); // initial parametric velocities
   private VectorNd myFpar = new VectorNd(); // parametric forces
   private VectorNd myFparC = new VectorNd (0); // parametric coriolis forces 

   private SparseNumberedBlockMatrix mySolveMatrix;
   private int mySolveMatrixVersion = -1;
   //private SparseNumberedBlockMatrix myKKTSolveMatrix;
   //private int myKKTSolveMatrixVersion = -1;

   private VectorNd myB = new VectorNd (0);
   private VectorNd myC = new VectorNd (0); // coriolis forces
   private VectorNd myF = new VectorNd (0); // forces
   private VectorNd myFcon = new VectorNd (0); // constraint forces

   // used in computing force residuals
   private VectorNd myDelV = new VectorNd (0);
   private VectorNd myFx = new VectorNd (0);

   // solver analysis versions

   private int myRegSolveMatrixVersion = -1;
   private int myKKTSolveMatrixVersion = -1;
   private int myKKTGTVersion = -1;
   private int myConMassVersion = -1;
   private int myConGTVersion = -1;

   public static boolean myDefaultHybridSolveP = false;
   private static int myHybridSolveTol = 10;
   //   private static boolean useBodyCoordsForExplicit = true;
   public static boolean profileConstraintSolves = false;
   private boolean myHybridSolveP = false;

   int myStateSizeVersion = -1;
   int myParametricPosSize = 0;
   int myParametricVelSize = 0;
   int myActivePosSize = 0;
   int myActiveVelSize = 0;
   int myDoubleAuxSize = 0;
   int myIntegerAuxSize = 0;
   //int myNumComponents = 0;
   int myNumActive = 0;
   int myNumParametric = 0;

   VectorNd myVel = new VectorNd();
   //VectorNd myPos = new VectorNd();

   public void setUpdateForcesAtStepEnd (boolean enable) {
      myUpdateForcesAtStepEnd = enable;
   }

   public boolean getUpdateForcesAtStepEnd() {
      return myUpdateForcesAtStepEnd;
   }

   public static enum MatrixSolver {
      Pardiso, Umfpack, ConjugateGradient, None
   }

   public static enum Integrator {
      ForwardEuler,
      SymplecticEuler,
      RungeKutta4,
      BackwardEuler,
      ConstrainedBackwardEuler,
      FullBackwardEuler,
      Trapezoidal
      //      BridsonMarino
   }

   private boolean integratorIsImplicit (Integrator integrator) {
      return (integrator == Integrator.BackwardEuler ||
         integrator == Integrator.ConstrainedBackwardEuler ||
         integrator == Integrator.Trapezoidal);
   }

   /** 
    * Indicates the method by which positions should be stabilized.
    */
   public enum PosStabilization {
      GlobalMass,
      GlobalStiffness
   }

   public boolean getHybridSolve () {
      return myHybridSolveP;
   }

   public void setHybridSolve (boolean enable) {
      myHybridSolveP = enable;
   }

   PardisoSolver myPardisoSolver;
   UmfpackSolver myUmfpackSolver;
   KKTSolver myKKTSolver;
   KKTSolver myConSolver;

   MatrixSolver myMatrixSolver = MatrixSolver.None;
   Integrator myIntegrator = Integrator.SymplecticEuler;
   boolean myComplianceSupported = false;
   double myTol = 0.01;
   ToleranceType myTolType = ToleranceType.RelativeResidual;
   int myMaxIterations = 20;
   boolean myUseDirectSolver = true;
   PosStabilization myStabilization = PosStabilization.GlobalMass;

   public void setParametricTargets (double s, double h) {
      // assumes that updateStateSizes() has been called
      mySys.getParametricVelState (myUpar0);
      mySys.getParametricVelTarget (myUpar, s, h);
      mySys.getParametricPosTarget (myQpar, s, h);
      mySys.setParametricVelState (myUpar);
      mySys.setParametricPosState (myQpar);
   }   

   public void computeParametricForces (double h) {
      int velSize = myActiveVelSize;
      int parVelSize = myParametricVelSize;
      if (parVelSize > 0) {
         myUpar0.sub (myUpar, myUpar0);
         myUpar0.scale (1/h);
         setSubVector (myFpar, myMassForces, velSize, parVelSize);
         myFpar.negate();
         myMass.mulAdd (
            myFpar, myUpar0, velSize, parVelSize, velSize, parVelSize);
         mySys.setParametricForces (myFpar);
      }
   }

   public void updateMassMatrix (double t) {
      // assumes that updateStateSizes() has been called
      int version = mySys.getStructureVersion();
      if (version != myMassVersion) {

         myMass = new SparseNumberedBlockMatrix();
         myMassForces = new VectorNd (myMass.rowSize());
         myMassConstantP = mySys.buildMassMatrix (myMass);
         mySys.getMassMatrix (myMass, myMassForces, t);
         myMassVersion = version;
         myMassTime = t;
      }
      else if (t == -1 || myMassTime != t) {
         mySys.getMassMatrix (myMass, myMassForces, t);
         myMassTime = t;
      }
   }

   /** 
    * Creates and returns a sparse block matrix consisting of the current
    * active portion of the mass matrix. Used mainly for exporting
    * information for analysis.
    *
    * @param t current time
    * @return current active mass matrix
    */
   public SparseBlockMatrix createActiveMassMatrix (double t) {
      updateMassMatrix (t);
      int nactive = mySys.numActiveComponents();
      return myMass.createSubMatrix (nactive, nactive);
   }

   protected boolean updateInverseMassMatrix (double t) {
      // assumes that updateMassMatrix has been called.
      boolean structureChanged = false;
      int version = mySys.getStructureVersion();
      if (version != myInverseMassVersion) {

         myInverseMass = new SparseNumberedBlockMatrix();
         mySys.buildMassMatrix (myInverseMass);
         mySys.getInverseMassMatrix (myInverseMass, myMass);
         myInverseMassVersion = version;
         myInverseMassTime = t;
         structureChanged = true;
      }
      else if (t == -1 || myInverseMassTime != t) {
         mySys.getInverseMassMatrix (myInverseMass, myMass);
         myInverseMassTime = t;
      }
      return structureChanged;
   }

   protected void getActiveVelDerivative (VectorNd dvdt, VectorNd f) {
      // assumes updateMassMatrix and updateInverseMassMatrix have been called
      mySys.getActiveForces (f);
      f.add (myMassForces);
      myInverseMass.mul (dvdt, f, myActiveVelSize, myActiveVelSize);
   }

   public void updateStateSizes () {

      int version = mySys.getStructureVersion();
      if (myStateSizeVersion != version) {

         myActivePosSize = mySys.getActivePosStateSize();
         myActiveVelSize = mySys.getActiveVelStateSize();

         myParametricPosSize = mySys.getParametricPosStateSize();
         myParametricVelSize = mySys.getParametricVelStateSize();

         //myNumComponents = mySys.numDynamicComponents();
         //myComponentSizes = new int[myNumComponents];
         //VectorNi sizes = new VectorNi();
         //mySys.getComponentSizes (myComponentSizes);
         //mySys.getComponentSizes (sizes);
         //myComponentSizes = Arrays.copyOf (sizes.getBuffer(), sizes.size());


         myQ.setSize (myActivePosSize);
         myU.setSize (myActiveVelSize);
         myUtmp.setSize (myActiveVelSize);

         myF.setSize (myActiveVelSize);
         if (myUpdateForcesAtStepEnd) {
            myFcon.setSize (myActiveVelSize);
         }

         myQpar.setSize (myParametricPosSize);
         myUpar.setSize (myParametricVelSize);
         myUpar0.setSize (myParametricVelSize);
         myFpar.setSize (myParametricVelSize);

         myVel.setSize (myActiveVelSize);
         //myPos.setSize (myActivePosSize);

         myStateSizeVersion = version;

         myNumActive = mySys.numActiveComponents();
         myNumParametric = mySys.numParametricComponents();
      }
   }

   private void ensureFrictionCapacity (int cap) {
      if (myFrictionInfo.length < cap) {
         int oldcap = myFrictionInfo.length;
         myFrictionInfo = Arrays.copyOf (myFrictionInfo, cap);
         for (int i=oldcap; i<cap; i++) {
            myFrictionInfo[i] = new MechSystem.FrictionInfo();
         }
      }
   }

   private void ensureGInfoCapacity (int cap) {
      if (myGInfo.length < cap) {
         int oldcap = myGInfo.length;
         myGInfo = Arrays.copyOf (myGInfo, cap);
         for (int i=oldcap; i<cap; i++) {
            myGInfo[i] = new MechSystem.ConstraintInfo();
         }
      }
   }

   private void ensureNInfoCapacity (int cap) {
      if (myNInfo.length < cap) {
         int oldcap = myNInfo.length;
         myNInfo = Arrays.copyOf (myNInfo, cap);
         for (int i=oldcap; i<cap; i++) {
            myNInfo[i] = new MechSystem.ConstraintInfo();
         }
      }
   }

   private Integrator getIntegratorForSolver (
      Integrator integrator, MatrixSolver solver) {
      Integrator result = integrator;
      switch (integrator) {
         case BackwardEuler: {
            if (solver != MatrixSolver.Umfpack &&
               solver != MatrixSolver.Pardiso) {
               return Integrator.SymplecticEuler;
            }
            break;
         }
         case Trapezoidal:
         case FullBackwardEuler:
         case ConstrainedBackwardEuler: {
            if (solver != MatrixSolver.Pardiso) {
               if (solver == MatrixSolver.Umfpack) {
                  return Integrator.BackwardEuler;
               }
               else {
                  return Integrator.SymplecticEuler;
               }
            }
         }
         default: {
         }
      }
      return result;
   }

   RigidBody getBody () {
      if (mySys instanceof MechModel) {
         MechModel mech = ((MechModel)mySys);
         RigidBody bod = mech.rigidBodies().get ("link2");
         if (bod != null) {
            return bod;
         }
         bod = mech.rigidBodies().get ("link1");
         if (bod != null) {
            return bod;
         }
      }
      return null;
   }

   RigidBodyConnector getConnector () {
      if (mySys instanceof MechModel) {
         MechModel mech = ((MechModel)mySys);
         RigidBodyConnector con = mech.rigidBodyConnectors().get ("joint2");
         if (con != null) {
            return con;
         }
         con = mech.rigidBodyConnectors().get ("joint1");
         if (con != null) {
            return con;
         }
      }
      return null;
   }

   public void setIntegrator (Integrator integrator) {
      // myIntegrator = getIntegratorForSolver (integrator, myMatrixSolver);
      // if (myIntegrator != integrator) {
      //    System.out.println (
      //       "Warning: cannot use "+integrator+
      //       " with matrix solver "+myMatrixSolver+", substituting "+myIntegrator);
      // }
      myIntegrator = integrator;
      switch (integrator) {
         case ConstrainedBackwardEuler:
         case Trapezoidal:
         case FullBackwardEuler: {
            myComplianceSupported = true;
            break;
         }
         default: {
            myComplianceSupported = true;
            break;
         }
      }
      mySolveMatrix = null;
      //myKKTSolveMatrix = null;
   }

   public void setIterativeSolver (IterativeSolver solver) {
      solver.setMaxIterations (myMaxIterations);
      solver.setToleranceType (myTolType);
      solver.setTolerance (myTol);
      myIterativeSolver = solver;
   }

   public Integrator getIntegrator() {
      return myIntegrator;
   }

   public double getTolerance() {
      return myTol;
   }

   public void setTolerance (double tol) {
      myTol = tol;
      if (myIterativeSolver != null) {
         myIterativeSolver.setTolerance (tol);
      }
   }

   public ToleranceType getToleranceType() {
      return myTolType;
   }

   public void setToleranceType (ToleranceType type) {
      myTolType = type;
      if (myIterativeSolver != null) {
         myIterativeSolver.setToleranceType (type);
      }
   }

   public void setMaxIterations (int max) {
      myMaxIterations = max;
      if (myIterativeSolver != null) {
         myIterativeSolver.setMaxIterations (max);
      }
   }

   public int getMaxIterations() {
      return myMaxIterations;
   }

   public void setMatrixSolver (MatrixSolver solver) {
      // if (getIntegratorForSolver (myIntegrator, solver) != myIntegrator) {
      //    System.out.println (
      //       "Warning: cannot use "+myIntegrator+
      //       " with matrix solver "+solver);
      //    return;
      // }
      if (solver != myMatrixSolver) {
         switch (solver) {
            case Pardiso: {
               if (!PardisoSolver.isAvailable()) {
                  System.out.println ("Pardiso unavailable");
                  return;
               }
               break;
            }
            case Umfpack: {
               if (!UmfpackSolver.isAvailable()) {
                  System.out.println ("Umfpack unavailable");
                  return;
               }
               break;
            }
            case ConjugateGradient: {
               setIterativeSolver (new CGSolver());
               break;
            }
            case None: {
               break;
            }
            default: {
               System.out.println ("Unknown solver " + solver);
               solver = MatrixSolver.None;
               break;
            }
         }
         mySolveMatrix = null;
         //myKKTSolveMatrix = null;
         myMatrixSolver = solver;
      }
   }

   public void setStabilization (PosStabilization stabilization) {
      myStabilization = stabilization;
   }

   public PosStabilization getStabilization () {
      return myStabilization;
   }

   /** 
    * Make sure that the current solver matches the one specified by
    * myMatrixSolver.
    */   
   private void updateSolver () {
      switch (myMatrixSolver) {
         case Pardiso: {
            if (myPardisoSolver == null) {
               myPardisoSolver = new PardisoSolver();
            }
            myDirectSolver = myPardisoSolver;
            myUseDirectSolver = true;
            break;
         }
         case Umfpack: {
            if (myUmfpackSolver == null) {
               myUmfpackSolver = new UmfpackSolver();
            }
            myDirectSolver = myUmfpackSolver;
            myUseDirectSolver = true;
            break;
         }
         case ConjugateGradient: {
            if (!(myIterativeSolver instanceof CGSolver)) {
               setIterativeSolver (new CGSolver());
            }
            myUseDirectSolver = false;
            break;
         }
         case None: {
            myUseDirectSolver = false;
            break;
         }
         default: {
            System.out.println ("Unknown solver " + myMatrixSolver);
            myUseDirectSolver = false;
            break;
         }
      }
   }

   public boolean isPardisoAvailable () {
      return PardisoSolver.isAvailable();
   }

   public boolean hasMatrixSolver (MatrixSolver solver) {
      switch (solver) {
         case Pardiso: {
            return PardisoSolver.isAvailable();
         }
         case Umfpack: {
            return UmfpackSolver.isAvailable();
         }
         case ConjugateGradient: {
            return true;
         }
         case None: {
            return true;
         }
         default: {
            System.out.println ("Unknown solver " + solver);
            return false;
         }
      }
   }

   public MatrixSolver getMatrixSolver() {
      return myMatrixSolver;
   }

   private void initializeSolvers() {
      System.out.println ("Initialize solver");
      if (PardisoSolver.isAvailable()) {
         setMatrixSolver (MatrixSolver.Pardiso);
      }
      // Umfpack no longer supported ...
      // else if (UmfpackSolver.isAvailable()) {
      //    setMatrixSolver (MatrixSolver.Umfpack);
      // }
   }

   /** 
    * Create a new MechSystem solver for a specified MechSystem.
    */
   public MechSystemSolver (MechSystem system) {
      mySys = system;
      myRBSolver = new RigidBodySolver (system);
      setIntegrator (Integrator.SymplecticEuler);
      setHybridSolve (myDefaultHybridSolveP);
      initializeSolvers();
   }

   /** 
    * Create a new MechSystem solver for a specified MechSystem,
    * with settings imported from an existing MechSystemSolver.
    */
   public MechSystemSolver (MechSystem system, MechSystemSolver solver) {
      this (system);
      setHybridSolve (solver.getHybridSolve());
      setIntegrator (solver.getIntegrator());
      setMatrixSolver (solver.getMatrixSolver());
   }

   public void nonDynamicSolve (double t0, double t1, StepAdjustment stepAdjust) {
      updateStateSizes();
      setParametricTargets (1, t1-t0);
   }

   public void solve (double t0, double t1, StepAdjustment stepAdjust) {
      if (profileWholeSolve) {
         timerStart();
      }
      updateStateSizes();
      updateMassMatrix (t0);
      setParametricTargets (1, t1-t0);

      if (myUpdateForcesAtStepEnd) {
         myFcon.setZero();
      }
      switch (myIntegrator) {
         case ForwardEuler: {
            forwardEuler (t0, t1, stepAdjust);
            break;
         }
         case RungeKutta4: {
            rungeKutta4 (t0, t1, stepAdjust);
            break;
         }
         case SymplecticEuler: {
            symplecticEuler (t0, t1, stepAdjust);
            break;
         }
         case BackwardEuler: {
            backwardEuler (t0, t1, stepAdjust);
            break;
         }
         case ConstrainedBackwardEuler: {
            constrainedBackwardEuler (t0, t1, stepAdjust);
            break;
         }
         case FullBackwardEuler: {
            fullBackwardEuler (t0, t1, stepAdjust);
            break;
         }
         case Trapezoidal: {
            trapezoidal (t0, t1, stepAdjust);
            break;
         }
         default: {
            throw new UnsupportedOperationException ("Integrator "
               + myIntegrator + " not supported");
         }
      }
      if (myUpdateForcesAtStepEnd) {
         updateActiveForces (t0, t1);
         computeParametricForces(t1-t0);
      }
      if (profileWholeSolve) {
         timerStop ("wholeSolve");
      }
   }

   protected void forwardEuler (double t0, double t1, StepAdjustment stepAdjust) {
      // boolean useBodyCoords = useBodyCoordsForExplicit;
      double h = t1 - t0;

      int velSize = myActiveVelSize;
      int posSize = myActivePosSize;

      myF.setSize (velSize);
      myU.setSize (velSize);
      myDudt.setSize (velSize);
      myQ.setSize (posSize);
      myDqdt.setSize (posSize);

      mySys.updateConstraints (t0, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t0);

      updateInverseMassMatrix (t0);

      mySys.getActiveVelState (myU);
      getActiveVelDerivative (myDudt, myF);
      mySys.getActivePosState (myQ);

      mySys.addActivePosImpulse (myQ, h, myU);
      mySys.setActivePosState (myQ);

      myU.scaledAdd (h, myDudt, myU);
      mySys.setActiveVelState (myU);         

      applyVelCorrection (myU, t0, t1);

      applyPosCorrection (myQ, myUtmp, t1, stepAdjust);
   }

   double maxErr = 0;

   private void debugMessagesForArticulatedDeformableBody1() {

      RigidBody bod = getBody ();
      RigidBodyConnector con = getConnector ();
      DeformableBody defbod = null;
      if (bod instanceof DeformableBody) {
         defbod = (DeformableBody)bod;
      }
      RigidTransform3d XCA0 = new RigidTransform3d ();
      RigidTransform3d XGA = new RigidTransform3d ();
      PolarDecomposition3d polar = new PolarDecomposition3d();
      Twist cvel = new Twist();

      if (bod != null) {
         if (con != null) {         
            XGA.set (con.getTGA());
         }
         if (defbod != null) {
            defbod.computeUndeformedFrame (XCA0, polar, XGA);
         }
         System.out.println ("bodyVelA=" + bod.getVelocity().toString("%13.9f"));
         if (defbod != null) {
            System.out.println ("elasVelA=" + defbod.getElasticVel().toString("%13.9f"));
         }
         //System.out.println ("XGA=\n" + XGA.toString ("%13.9f"));
         bod.getBodyVelocity (cvel);
         cvel.inverseTransform (XGA);
         if (defbod != null) {
            Twist velx = new Twist(); 
            defbod.computeDeformedFrameVel (velx, polar, XGA);
            System.out.println ("velx=" + velx.toString ("%13.9f"));
            cvel.add (velx);
         }
         System.out.println ("velA=" + cvel.toString ("%13.9f"));
      }
   }

   private void debugMessagesForArticulatedDeformableBody2() {

      RigidBody bod = getBody ();
      RigidBodyConnector con = getConnector ();
      DeformableBody defbod = null;
      if (bod instanceof DeformableBody) {
         defbod = (DeformableBody)bod;
      }
      RigidTransform3d XCA0 = new RigidTransform3d ();
      RigidTransform3d XGA = new RigidTransform3d ();
      PolarDecomposition3d polar = new PolarDecomposition3d();
      Twist cvel = new Twist();

      // begin deformable body constraint debug 
      if (bod != null) {
         if (con != null) {         
            XGA.set (con.getTGA());
         }
         if (defbod != null) {
            defbod.computeUndeformedFrame (XCA0, polar, XGA);
         }
         System.out.println ("bodyVelB=" + bod.getVelocity().toString("%13.9f"));
         if (defbod != null) {
            System.out.println ("elasVelB=" + defbod.getElasticVel().toString("%13.9f"));
         }
         bod.getBodyVelocity (cvel);
         cvel.inverseTransform (XGA);
         if (defbod != null) {
            Twist velx = new Twist(); 
            defbod.computeDeformedFrameVel (velx, polar, XGA);
            cvel.add (velx);
         }
         System.out.println ("velB=" + cvel.toString ("%13.9f"));
      }
      // end deformable body constraint debug 
   }

   protected void symplecticEuler (double t0, double t1, StepAdjustment stepAdjust) {
      double h = t1 - t0;


      int velSize = myActiveVelSize;
      int posSize = myActivePosSize;

      myF.setSize (velSize);
      myU.setSize (velSize);
      myDudt.setSize (velSize);
      myQ.setSize (posSize);
      myDqdt.setSize (posSize);

      mySys.updateConstraints (t0, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t0);

      updateInverseMassMatrix (t0);

      mySys.getActiveVelState (myU);
      getActiveVelDerivative (myDudt, myF);

      mySys.getActivePosState (myQ);

      //System.out.println ("\nNEW");

      myU.scaledAdd (h, myDudt, myU);
      mySys.setActiveVelState (myU);         

      //debugMessagesForArticulatedDeformableBody1();

      // begin deformable body constraint debug 
      // end deformable body constraint debug 

      applyVelCorrection (myU, t0, t1);

      //debugMessagesForArticulatedDeformableBody2();

      mySys.addActivePosImpulse (myQ, h, myU);
      mySys.setActivePosState (myQ);

      applyPosCorrection (myQ, myUtmp, t1, stepAdjust);
   }

   protected void rungeKutta4 (double t0, double t1, StepAdjustment stepAdjust) {
      double h = t1 - t0;
      double th = (t0 + t1) / 2;

      int velSize = myActiveVelSize;
      int posSize = myActivePosSize;

      myF.setSize (velSize);
      myU.setSize (velSize);
      myUtmp.setSize (velSize);
      myDudt.setSize (velSize);
      myDudtAvg.setSize (velSize);

      myQ.setSize (posSize);
      myQtmp.setSize (posSize);
      myDqdt.setSize (posSize);
      myDqdtAvg.setSize (posSize);

      mySys.updateConstraints (t0, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t0);

      mySys.getActivePosState (myQ);
      mySys.getActiveVelState (myU);
      mySys.getActivePosDerivative (myDqdtAvg, t0);
      updateInverseMassMatrix (t0);
      getActiveVelDerivative (myDudtAvg, myF);

      // k2 term

      myQtmp.scaledAdd (h / 2, myDqdtAvg, myQ);
      //xTmp.set (xVec);
      //mySys.addActivePosImpulse (xTmp, h/2, vVec);
      myUtmp.scaledAdd (h / 2, myDudtAvg, myU);
      mySys.setActivePosState (myQtmp);
      mySys.setActiveVelState (myUtmp);
      updateMassMatrix (-1);
      updateInverseMassMatrix (-1);
      mySys.updateForces (th);

      mySys.getActivePosDerivative (myDqdt, th);
      getActiveVelDerivative (myDudt, myF);
      myDqdtAvg.scaledAdd (2, myDqdt, myDqdtAvg);
      myDudtAvg.scaledAdd (2, myDudt, myDudtAvg);

      // k3 term

      myQtmp.scaledAdd (h / 2, myDqdt, myQ);
      //xTmp.set (xVec);
      //mySys.addActivePosImpulse (xTmp, h/2, vTmp);
      myUtmp.scaledAdd (h / 2, myDudt, myU);
      mySys.setActivePosState (myQtmp);
      mySys.setActiveVelState (myUtmp);

      updateMassMatrix (-1);
      updateInverseMassMatrix (-1);
      mySys.updateForces (th);

      mySys.getActivePosDerivative (myDqdt, th);
      getActiveVelDerivative (myDudt, myF);
      VectorNd dvdtNew = new VectorNd (myDudt);

      dvdtNew.sub (myDudt);
      myDqdtAvg.scaledAdd (2, myDqdt, myDqdtAvg);
      myDudtAvg.scaledAdd (2, myDudt, myDudtAvg);

      // k4 term

      myQtmp.scaledAdd (h, myDqdt, myQ);
      //xTmp.set (xVec);
      //mySys.addActivePosImpulse (xTmp, h, vTmp);
      myUtmp.scaledAdd (h, myDudt, myU);
      mySys.setActivePosState (myQtmp);
      mySys.setActiveVelState (myUtmp);
      updateMassMatrix (-1);
      updateInverseMassMatrix (-1);
      mySys.updateForces (t1);

      mySys.getActivePosDerivative (myDqdt, t1);
      getActiveVelDerivative (myDudt, myF);
      myDqdtAvg.add (myDqdt);
      myDudtAvg.add (myDudt);

      //dxdtAvg.scale (1/6.0);
      //dvdtAvg.scale (1/6.0);


      myUtmp.scaledAdd (h/6, myDudtAvg, myU);
      myQtmp.scaledAdd (h/6, myDqdtAvg, myQ);
      //xTmp.set (xVec);
      //mySys.addActivePosImpulse (xTmp, h, vTmp);

      mySys.setActivePosState (myQtmp);
      mySys.setActiveVelState (myUtmp);         
      applyVelCorrection (myUtmp, t0, t1);

      applyPosCorrection (myQtmp, myUtmp, t1, stepAdjust);
   }

   FunctionTimer timer = new FunctionTimer();

   private void timerStart() {
      timer.start();
   }

   private void timerStop(String msg) {
      timer.stop();
      System.out.println (msg + ": " + timer.result(1));
   }

   // begin timing code for the solver
   FunctionTimer factorTimer = new FunctionTimer();
   FunctionTimer solveTimer = new FunctionTimer();
   int timeCnt = 0;
   int maxCnt = 5;

   // end timing code for solver

   private void doDirectSolve (VectorNd x, SparseBlockMatrix M, VectorNd b) {
      if (myHybridSolveP && myDirectSolver.hasAutoIterativeSolving()) {
         myDirectSolver.autoFactorAndSolve (x, b, myHybridSolveTol);
      }
      else {
         myDirectSolver.factor();
         myDirectSolver.solve (x, b);
      }
   }

   public void mulActiveInertias (VectorNd b, VectorNd v) {
      //assumes that updateMassMatrix() has been called
      updateStateSizes();
      b.setZero();
      myMass.mulAdd (b, v, myActiveVelSize, myActiveVelSize);
   }

   public void addActiveMassMatrix (
      MechSystem sys, SparseBlockMatrix S, double t) {
      // assumes that updateMassMatrix() has been called
      for (int bi=0; bi<sys.numActiveComponents(); bi++) {
         S.getBlock(bi, bi).add (myMass.getBlock(bi, bi));
      }
   }

   public void addMassForces (VectorNd f, double t) {
      updateStateSizes();
      updateMassMatrix (t);
      f.add (myMassForces);
   }

   /** 
    * Computes
    * <pre>
    * vr += v1(off:off+size-1)
    * </pre>
    */   
   public void addSubVector (VectorNd vr, VectorNd v1, int off, int size) {
      double[] bufr = vr.getBuffer();
      double[] buf1 = v1.getBuffer();
      for (int i=0; i<size; i++) {
         bufr[i] += buf1[i+off];
      }
   }

   /** 
    * Sets
    * <pre>
    * vr = v1(off:off+size-1)
    * </pre>
    */   
   public void setSubVector (VectorNd vr, VectorNd v1, int off, int size) {
      double[] bufr = vr.getBuffer();
      double[] buf1 = v1.getBuffer();
      for (int i=0; i<size; i++) {
         bufr[i] = buf1[i+off];
      }
   }

   public void backwardEuler (double t0, double t1, StepAdjustment stepAdjust) {
      updateSolver();

      if (myMatrixSolver == MatrixSolver.None) {
         throw new UnsupportedOperationException (
            "MatrixSolver cannot be 'None' for this integrator");
      }
      double h = t1 - t0;
      // timer.start();

      int velSize = myActiveVelSize;
      int posSize = myActivePosSize;

      updateSolveMatrixStructure();

      myB.setSize (velSize);
      //myV.setSize (velSize);
      myF.setSize (velSize);
      // collects coriolis forces for attachments
      myC.setSize (mySolveMatrix.rowSize()); 
      //myU.setSize (velSize);
      myDudt.setSize (velSize);
      myQ.setSize (posSize);
      myDqdt.setSize (posSize);

      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t1);

      // b = M v
      mySys.getActiveVelState (myU);
      mulActiveInertias (myB, myU);

      mySys.getActiveForces (myF);
      //System.out.println ("myF=" + myF);
      myF.add (myMassForces);
      myB.scaledAdd (h, myF, myB);

      mySolveMatrix.setZero();
      myC.setZero ();
      mySys.addVelJacobian (mySolveMatrix, myC, -h);
      if (useFictitousJacobianForces) {
         myB.scaledAdd (h, myC);
      }

      // b += Jv v
      mySolveMatrix.mulAdd (myB, myU, velSize, velSize);

      myC.setZero ();
      mySys.addPosJacobian (mySolveMatrix, myC, -h * h);
      if (useFictitousJacobianForces) {
         myB.scaledAdd (h, myC);
      }

      addActiveMassMatrix (mySys, mySolveMatrix, t1);

      int n = myB.size();

      if (mySolveMatrixVersion != myRegSolveMatrixVersion) {
         myRegSolveMatrixVersion = mySolveMatrixVersion;
         int matrixType = mySys.getSolveMatrixType();
         if (velSize != 0) {
            if (myUseDirectSolver) {
               myDirectSolver.analyze (
                  mySolveMatrix, velSize, matrixType);
            }
            else {
               if (!myIterativeSolver.isCompatible (matrixType)) {
                  throw new UnsupportedOperationException (
                     "Matrix cannot be solved by the chosen iterative solver");
               }
            }
         }
      }
      if (velSize != 0) {
         if (myUseDirectSolver) {
            doDirectSolve (myU, mySolveMatrix, myB);
         }
         else {
            myIterativeSolver.solve (myU, mySolveMatrix, myB);
         }
      }

      mySys.setActiveVelState (myU);         
      applyVelCorrection (myU, t0, t1);

      mySys.getActivePosState (myQ);
      mySys.addActivePosImpulse (myQ, h, myU);
      mySys.setActivePosState (myQ);

      applyPosCorrection (myQ, myUtmp, t1, stepAdjust);
   }

   private int mySolveChangeCnt;
   private int myGTChangeCnt;

   private void printSysMatrix (SparseBlockMatrix S, String fmt, int size) {
      MatrixNd SS = new MatrixNd (size, size);
      for (int i=0; i<size; i++) {
         for (int j=0; j<size; j++) {
            SS.set (i, j, S.get(i,j));
         }
      }
      System.out.println ("S=[\n" + SS.toString ("%12.7f"));
      System.out.println ("]");
   }

   private static PrintWriter myLogWriter;


   static FunctionTimer timerX = new FunctionTimer();
   static boolean solveModePrinted = false;

   protected void updateSolveMatrixStructure () {
      // assumes that updateStateSizes() has been called
      if (mySys.getStructureVersion() != mySolveMatrixVersion) {
         mySolveMatrixVersion = mySys.getStructureVersion();
         mySolveMatrix = new SparseNumberedBlockMatrix();
         mySys.buildSolveMatrix (mySolveMatrix);
      }
   }

   protected void updateBilateralConstraints (double t) {
      // assumes that updateStateSizes() has been called
      SparseBlockMatrix oldGT = myGT;
      myGT = new SparseNumberedBlockMatrix ();
      int version = mySys.getBilateralConstraints (myGT, myGdot);
      // if (version != myGTVersion) {
      //    myGTVersion = version;
      // }
      // XXX Sanchez, Jun 23, 2014
      // Collision bug??  Need better way to check structure change
      // since blocks are reused when constructing myGT
      if (oldGT == null || !myGT.structureEquals (oldGT)) {
         myGTVersion++;
      }
      myGsize = myGT.colSize();
      ensureGInfoCapacity (myGsize);
      myRg.setSize (myGsize);
      myBg.setSize (myGsize);
      myLam.setSize (myGsize);
   }

   protected void updateUnilateralConstraints (double t) {
      // assumes that updateStateSizes() has been called
      myNT = new SparseNumberedBlockMatrix ();
      mySys.getUnilateralConstraints (myNT, myNdot);
      myNsize = myNT.colSize();
      ensureNInfoCapacity (myNsize);
      myRn.setSize (myNsize);
      myBn.setSize (myNsize);
      myThe.setSize (myNsize);
   }

   protected void updateFrictionConstraints () {
      // assumes that updateStateSizes() has been called
      myDT = new SparseNumberedBlockMatrix ();

      int fmax = mySys.maxFrictionConstraintSets();
      ensureFrictionCapacity (fmax);
      mySys.getFrictionConstraints (myDT, myFrictionInfo);
      int sizeD = myDT.colSize();
      myBd.setSize (sizeD);
      myPhi.setSize (sizeD);

      // compute friction offsets
      if (sizeD > 0 && myParametricVelSize > 0) {
         myDT.mulTranspose (
            myBd, myUpar, 0, sizeD, myActiveVelSize, myParametricVelSize);
         myBd.negate(); // move to rhs
      }
      else {
         myBd.setZero();
      }        

   }

   /** 
    * Solves a KKT system in which the Jacobian augmented M matrix and
    * and force vectors are given by
    * <pre>
    * M' = M - h df/dv - h^2 df/dx 
    *
    * bf' = bf + (-h df/dv) vel0
    * </pre>
    *
    * When used to solve for velocities in an implicit integrator, then
    * on input, bf is assumed to be given by
    * <pre>
    * bf = M vel0 + h f
    * </pre>
    * where h is the time step and f is the generalized forces, while
    * on output bf is modified to include the Jacobian terms described
    * above.
    * 
    * XXX question about which time do we want? t0 or t1? Right
    * now it is set to t1; setting it to t0 seems to cause a
    * small difference in the inverse tongue model.
    */
   public void KKTFactorAndSolve (
      VectorNd vel, VectorNd fpar, VectorNd bf, VectorNd btmp, VectorNd vel0, 
      double t, double h, boolean velocitySolve) {

      KKTFactorAndSolve (
         vel, fpar, bf, btmp, vel0, t, h, -h, -h*h, -h, 0, velocitySolve);
   }

   // Setting crsFileName will cause the KKT system matrix and RHS
   // to be logged in CCS format.
   private String crsFileName = null; // "testData.txt";
   private PrintWriter crsWriter = null;
   private boolean crsOmitDiag = true;

   private void setBilateralOffsets (double h, double dotscale) {

      if (myGsize > 0) {
         mySys.getBilateralInfo (myGInfo);
         double[] gdot = myGdot.getBuffer();
         double[] Rbuf = myRg.getBuffer();
         double[] gbuf = myBg.getBuffer();
         for (int i=0; i<myGsize; i++) {
            ConstraintInfo gi = myGInfo[i];
            if (gi.compliance > 0) {
               double s = 1/(0.5*h+gi.damping*gi.compliance);
               Rbuf[i] = s*gi.compliance/h;
               gbuf[i] -= s*gi.dist;
            }
            gbuf[i] -= dotscale*gdot[i];
            //System.out.println ("gbuf=" + gbuf[i]);
         }      
      }
   }

   private void setUnilateralOffsets (double h, double dotscale) {

      if (myNsize > 0) {
         mySys.getUnilateralInfo (myNInfo);
         double[] ndot = myNdot.getBuffer();
         double[] Rbuf = myRn.getBuffer();
         double[] nbuf = myBn.getBuffer();
         for (int i=0; i<myNsize; i++) {
            ConstraintInfo ni = myNInfo[i];
            if (ni.compliance > 0) {
               double s = 1/(0.5*h+ni.damping*ni.compliance);
               Rbuf[i] = s*ni.compliance/h;
               nbuf[i] -= s*ni.dist;
            }
            nbuf[i] -= dotscale*ndot[i];
         }
      }
   }

   /** 
    * Solves a KKT system in which the Jacobian augmented M matrix and
    * and force vectors are given by
    * <pre>
    * M' = M + a0 df/dv + a1 df/dx 
    *
    * bf' = bf + (a2 df/dv + a3 df/dx) vel0
    * </pre>
    * It is assumed that a0 and a1 are both non-zero. It is also assumed that
    * the a0 = -alpha h, where h is the step size and alpha indicates the
    * propertion of implicitness for the solve; i.e., for regular backward
    * euler, alpha=1, while for trapezoidal solves, alpha = 0.5;
    *
    * When used to solve for velocities in an implicit integrator, then
    * on input, bf is assumed to be given by
    * <pre>
    * bf = M vel0 + h f
    * </pre>
    * where h is the time step and f is the generalized forces, while
    * on output bf is modified to include the Jacobian terms described
    * above.
    */
   public void KKTFactorAndSolve (
      VectorNd vel, VectorNd fpar, VectorNd bf, VectorNd btmp, VectorNd vel0,
      double t, double h, double a0, double a1, double a2, double a3,
      boolean velocitySolve) {

      // assumes that updateMassMatrix() has been called
      updateStateSizes();

      int velSize = myActiveVelSize;

      boolean analyze = false;

      updateSolveMatrixStructure();
      if (myKKTSolveMatrixVersion != mySolveMatrixVersion) {
         myKKTSolveMatrixVersion = mySolveMatrixVersion;
         analyze = true;
      }
      SparseNumberedBlockMatrix S = mySolveMatrix;      

      S.setZero();
      myC.setSize (S.rowSize());
      myC.setZero();
      mySys.addVelJacobian (S, myC, a0);
      //System.out.println ("myC=" + myC);
      if (useFictitousJacobianForces) {
         bf.scaledAdd (-a0, myC);
         if (fpar != null && myParametricVelSize > 0) {
            setSubVector (fpar, myC, velSize, myParametricVelSize);
         }
      }
      if (vel0 != null) {
         double alpha = a2/a0 - a3/a1;
         S.mul (btmp, vel0, velSize, velSize);
         bf.scaledAdd (alpha, btmp);
      }
      myC.setZero();
      mySys.addPosJacobian (S, myC, a1);
      if (useFictitousJacobianForces) {
         bf.scaledAdd (-a0, myC);
         if (fpar != null && myParametricVelSize > 0) {
            addSubVector (fpar, myC, velSize, myParametricVelSize);
         }
      }
      if (vel0 != null && a3 != 0) {
         double beta = a3/a1;
         S.mul (btmp, vel0, velSize, velSize);
         bf.scaledAdd (beta, btmp);
      }

      addActiveMassMatrix (mySys, S, t);

      if (velSize > 0 && myParametricVelSize > 0) {
         S.mulTranspose (
            btmp, myUpar, 0, velSize, velSize, myParametricVelSize);
         bf.sub (btmp);
      }

      if (myKKTSolver == null) {
         myKKTSolver = new KKTSolver();
      }

      updateBilateralConstraints (t);
      if (myKKTGTVersion != myGTVersion) {
         analyze = true;
         myKKTGTVersion = myGTVersion;
      }

      if (!velocitySolve) {
         throw new InternalErrorException (
            "KKTFactorAndSolve now only does velocity solves");
      }
      if (myGsize > 0 && myParametricVelSize > 0) {
         myGT.mulTranspose (
            myBg, myUpar, 0, myGsize, velSize, myParametricVelSize);
         myBg.negate(); // move to rhs
      }
      else {
         myBg.setZero();
      }
      // a0 is assumed to be negative, which moves myGdot over to the rhs
      //myBg.scaledAdd (a0, myGdot);
      setBilateralOffsets (h, -a0);

      updateUnilateralConstraints (t);

      if (myNsize > 0 && myParametricVelSize > 0) {
         myNT.mulTranspose (
            myBn, myUpar, 0,myNsize, velSize, myParametricVelSize);
         myBn.negate(); // move to rhs
      }
      else {
         myBn.setZero();
      }
      // a0 is assumed to be negative, which moves myGdot over to the rhs
      setUnilateralOffsets (h, -a0);

      myLam.setSize (myGT.colSize());
      if (myNT != null) {
         myThe.setSize (myNT.colSize());
      }
      else {
         myThe.setSize (0);
      }
      if (t == 0) {
         // set these to zero in case we are doing hybrid solves
         myThe.setZero();
         myLam.setZero();
      }

      if (!solveModePrinted) {
         System.out.println (myHybridSolveP ? "hybrid solves" : "direct solves");
         solveModePrinted = true;
      }            

      if (crsWriter == null && crsFileName != null) {
         try {
            crsWriter = ArtisynthIO.newIndentingPrintWriter (crsFileName);
         }
         catch (Exception e) {
            crsFileName = null;
         }
      }

      if (velSize != 0) {
         if (vel0 != null) {
            // set vel to vel0 in case the solver needs a warm start
            vel.set (vel0);
         }
         if (analyze) {
            myKKTSolver.analyze (S, velSize, myGT, mySys.getSolveMatrixType());
         }
         if (myHybridSolveP && !analyze &&
            (myNT == null || myNT.colSize() == 0)) {
            if (profileKKTSolveTime) {
               timerStart();
            }
            myKKTSolver.factorAndSolve (
               S, velSize, myGT, myRg, vel, myLam, bf, myBg, myHybridSolveTol);
            if (profileKKTSolveTime) {
               timerStop ("KKTsolve(hybrid)");
            }
         }
         else {
            if (profileKKTSolveTime) {
               timerStart();
            }
            myKKTSolver.factor (S, velSize, myGT, myRg, myNT, myRn);
            myKKTSolver.solve (vel, myLam, myThe, bf, myBg, myBn);
            if (profileKKTSolveTime) {
               timerStop ("KKTsolve");
            }
         }
         //System.out.println ("bg=" + myBg);
         //System.out.println ("S=\n" + S);
         if (crsWriter != null) {
            String msg = 
               "# KKTsolve M="+velSize+" G="+myGT.colSize()+
               " N="+myNT.colSize()+(analyze ? " ANALYZE" : "");
            System.out.println (msg);
            try {
               crsWriter.println (msg);
               myKKTSolver.printLinearProblem (
                  crsWriter, bf, myBg, "%.6g", crsOmitDiag);
            }
            catch (Exception e) {
               e.printStackTrace(); 
               crsWriter = null;
               crsFileName = null;
            }

         }
      }

      mySys.setBilateralImpulses (myLam, h);
      mySys.setUnilateralImpulses (myThe, h);
      if (myUpdateForcesAtStepEnd) {
         if (myGsize > 0) {
            myGT.mulAdd (myFcon, myLam, velSize, myGsize);
         }
         if (myNsize > 0) {
            myNT.mulAdd (myFcon, myThe, velSize, myNsize);
         }
      }


      if (myLogWriter != null) {
         try {
            NumberFormat fmt = new NumberFormat("%g");
            myLogWriter.println ("M("+velSize+"x"+velSize+")=[");
            S.write (myLogWriter, fmt, Matrix.WriteFormat.SYMMETRIC_CRS,
               velSize, velSize);
            myLogWriter.println ("];");
            myLogWriter.println ("GT("+velSize+"x"+myGT.colSize()+")=[");
            myGT.write (myLogWriter, fmt, Matrix.WriteFormat.CRS,
               velSize, myGT.colSize()); 
            myLogWriter.println ("];");
            myLogWriter.println ("NT("+velSize+"x"+myNT.colSize()+")=[");
            myNT.write (myLogWriter, fmt, Matrix.WriteFormat.CRS,
               velSize, myNT.colSize());
            myLogWriter.println ("];");
            myLogWriter.println ("bf=[");
            bf.write (myLogWriter, fmt);
            myLogWriter.println ("];");
            myLogWriter.println ("myBg=[");
            myBg.write (myLogWriter, fmt);
            myLogWriter.println ("];");
            myLogWriter.println ("myBn=[");
            myBn.write (myLogWriter, fmt);
            myLogWriter.println ("];");
            myLogWriter.println ("vel=[");
            vel.write (myLogWriter, fmt);
            myLogWriter.println ("];");
            myLogWriter.println ("myLam=[");
            myLam.write (myLogWriter, fmt);
            myLogWriter.println ("];");
            myLogWriter.println ("myThe=[");
            myThe.write (myLogWriter, fmt);
            myLogWriter.println ("];");
            myLogWriter.println ("");        
            myLogWriter.flush();
            System.out.println ("logging");
         }
         catch (IOException e) {
            e.printStackTrace();
            myLogWriter = null;
         }
      }
   }

   public void setCrsFileName(String name) {
      if (crsWriter != null) {
         crsWriter.close();
      }
      crsFileName = name;
      crsWriter = null;
   }

   public static void setLogWriter(PrintWriter writer) {
      if (myLogWriter != null) {
         myLogWriter.close();
      }
      myLogWriter = writer;
   }

   /** 
    * Solves the KKT system given a new right hand side bf. It is assumed that
    * KKTFactorAndSolve has already been called once, and that the initial
    * velocity vel0 is unchanged from that call. Recall that on input
    * to KKTFactorAndSolve, bf is assumed to be of the form
    * <pre>
    * bf = M vel0 + f1
    * </pre>
    * while on output it is modified to include Jacobian terms that
    * depend on vel0. Thus if one want to use KKTSolve to compute
    * velocities based on different force values f2, one should
    * use a calling sequence similar to the following:
    * <pre>
    * bf1 = M vel0 + f1
    * KKTFactorAndSolve (vel1, bf1, btmp, vel0, ...)
    * bf2 = bf1 - f1 + f2
    * KKTSolve (vel2, myLam, the, bf2)
    * </pre>
    */
   public void KKTSolve (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd bf) {

      if (myKKTSolver == null || !myKKTSolver.isFactored()) {
         throw new IllegalStateException (
            "KKTFactorAndSolve must be called prior to KKTSolve");
      }
      int velSize = mySys.getActiveVelStateSize();
      if (velSize != vel.size()) {
         throw new IllegalStateException (
            "Velocity size != current active velocity state size");
      }
      //SparseBlockMatrix S = mySys.getSolveMatrix (MechSystem.FULL_MATRIX);
      //if (S != myKKTSolveMatrix) {
      //   throw new IllegalStateException ("Solve matrix has changed");
      //}
      if (myKKTSolveMatrixVersion != mySys.getStructureVersion()) {
         throw new IllegalStateException ("Solve structure has changed");
      }

      if (myGT.colSize() != myBg.size()) {
         throw new IllegalStateException (
            "Number of bilateral offsets != number of bilateral constraints");
      }
      if (myNT != null && myNT.colSize() != myBn.size()) {
         throw new IllegalStateException (
            "Number of unilateral offsets != number of unilateral constraints");
      }

      lam.setSize (myGT.colSize());
      if (myNT != null) {
         the.setSize (myNT.colSize());
      }
      else {
         the.setSize (0);
      }
      if (velSize != 0) {
         myKKTSolver.solve (vel, lam, the, bf, myBg, myBn);
      }
   }

   private double projectSingleFrictionConstraint (
      VectorNd vel, SparseBlockMatrix DT, int bj, double phiMax, double doff,
      boolean ignoreRigidBodies) {

      int nactive = mySys.numActiveComponents();
      int nparam = mySys.numParametricComponents();

      int nlimit = nactive;
      if (MechSystemBase.myParametricsInSystemMatrix) {
         nlimit += nparam;
      }

      Vector3d d = new Vector3d();
      Vector3d r = new Vector3d();

      double[] vbuf = vel.getBuffer();
      double[] pbuf = myUpar.getBuffer();
      double[] vtbuf = new double[1];
      double phi;

      double dmd = 0;
      for (MatrixBlock blk=DT.firstBlockInCol(bj); blk != null; blk=blk.down()) {
         int bi = blk.getBlockRow();
         if (bi < nactive) {
            blk.mulTransposeAdd (vtbuf, 0, vbuf, DT.getBlockRowOffset(bi));
            if (blk instanceof Matrix3x1) {
               ((Matrix3x1)blk).get (d);
               // XXX assumes that corresponding block is Matrix3d
               Matrix3d Minv = (Matrix3d)myInverseMass.getBlock(bi,bi);
               Minv.mul (r, d);
               dmd += r.dot(d);
            }
            else if (!ignoreRigidBodies) {
               // XXX implement
            }
         }
         else if (MechSystemBase.myParametricsInSystemMatrix &&
            bi < nactive + nparam) {
            int poff = DT.getBlockRowOffset(bi) - myActiveVelSize;
            blk.mulTransposeAdd (vtbuf, 0, pbuf, poff);
         }
      }
      double vt = vtbuf[0] - doff;
      // prevent division by zero
      if (dmd == 0) {
         dmd = 1e-16;
      }
      //System.out.println (" vtMag=" + vt + " dmd=" + dmd + " phiMax="+phiMax);
      if (vt > dmd*phiMax) {
         phi = -phiMax;
      }
      else if (vt < -dmd*phiMax) {
         phi = phiMax;
      }
      else {
         phi = -vt/dmd;
      }
      VectorNd dvec = new VectorNd();
      for (MatrixBlock blk=DT.firstBlockInCol(bj); blk != null; blk=blk.down()) {
         int bi = blk.getBlockRow();
         if (bi >= nactive) {
            break;
         }
         if (blk.rowSize() == 3) {
            dvec.setSize (3);
            blk.getColumn (0, dvec);
            MatrixBlock Minv = myInverseMass.getBlock(bi,bi);
            dvec.scale (phi);
            Minv.mulAdd (vbuf, DT.getBlockRowOffset(bi), dvec.getBuffer(), 0);
         }
         else if (!ignoreRigidBodies) {
            // XXX implement
         }
      }
      return phi;
   }

   private void printArray (String name, int[] array) {
      if (array == null) {
         System.out.println (name + "=null");
      }
      else {
         System.out.print (name + "=[ ");
         for (int i=0; i<array.length; i++) {
            System.out.print (array[i] + " ");
         }
         System.out.println ("]");
      }
   }

   protected void projectFrictionConstraints (VectorNd vel, double t0) {
      // BEGIN project friction constraints
      updateFrictionConstraints();
      // assumes that updateMassMatrix() has been called
      myRBSolver.updateStructure (myMass, myGT);

      myRBSolver.projectFriction (
         myMass, myGT, myNT, myDT,
         myRg, myBg, myRn, myBn, myBd, myFrictionInfo, vel, myLam, myThe, myPhi);

      // do a Gauss-Siedel project on remaining friction constraints:
      // 
      // 1. Determine remaining constraints:

      int[] RBDTmap = myRBSolver.getDTMap();
      if (RBDTmap != null) {
         int[] DTmap = new int[myDT.numBlockCols()-RBDTmap.length];
         int i = 0;
         int k = 0;
         for (int bj=0; bj<myDT.numBlockCols(); bj++) {
            if (k < RBDTmap.length && RBDTmap[k] == bj) {
               k++;
            }
            else {
               DTmap[i++] = bj;
            }
         }
         if (i != DTmap.length) {
            throw new InternalErrorException ("inconsistent DTmap");
         }
         updateInverseMassMatrix (t0);
         for (i=0; i<DTmap.length; i++) {
            FrictionInfo info = myFrictionInfo[DTmap[i]];
            double phiMax;
            if ((info.flags & FrictionInfo.BILATERAL) != 0) {
               phiMax = info.mu*myLam.get(info.contactIdx);
            }
            else {
               phiMax = info.mu*myThe.get(info.contactIdx);
            }
            int bj = DTmap[i];
            int j = myDT.getBlockColOffset(bj);
            double doff = myBd.get(j);
            double phi = projectSingleFrictionConstraint (
               vel, myDT, bj, phiMax, doff, /*ignore rigid bodies=*/true);
            myPhi.set (j, phi);
         }
      }

      if (myUpdateForcesAtStepEnd) {
         int velSize = myActiveVelSize;
         if (myDsize > 0) {
            myDT.mulAdd (myFcon, myPhi, velSize, myDsize);
         }
      }
   }

   protected void computeVelCorrections (VectorNd vel, double t0, double t1) {

      double h = t1-t0;
      // assumes that updateMassMatrix() has been called
      int velSize = myActiveVelSize;
      if (velSize == 0) {
         return;
      }            
      if (myConSolver == null) {
         myConSolver = new KKTSolver();
      }
      updateBilateralConstraints (t1);
      updateUnilateralConstraints (t1);
      if (myGsize == 0 && myNsize == 0) {
         // no constraints, so no solve needed
         return;
      }
      if (myGsize > 0 && myParametricVelSize > 0) {
         myGT.mulTranspose (
            myBg, myUpar, 0, myGsize, velSize, myParametricVelSize);
         myBg.negate(); // move to rhs                                          
      }
      else {
         myBg.setZero();
      }
      setBilateralOffsets (h, 0);

      if (myNsize > 0 && myParametricVelSize > 0) {
         myNT.mulTranspose (
            myBn, myUpar, 0, myNsize, velSize, myParametricVelSize);
         myBn.negate(); // move to rhs                                          

         // MatrixNd N = new MatrixNd();
         // N.set (myNT);
         // N.transpose ();
         // System.out.println ("N=\n" + N);
         // System.out.println ("myBn=" + myBn);
      }
      else {
         myBn.setZero();
      }
      setUnilateralOffsets (h, 0);

      //mySys.getBilateralOffsets (myRg, myBg, 0, MechSystem.VELOCITY_MODE);
      //mySys.getUnilateralOffsets (myRn, myBn, 0, MechSystem.VELOCITY_MODE);
      myBf.setSize (velSize);
      //myVel.setSize (velSize);
      // TODO: need to add fictitous forces
      myMass.mul (myBf, vel, velSize, velSize);
      if (myConMassVersion != myMassVersion || myConGTVersion != myGTVersion) {
         myConSolver.analyze (myMass, velSize, myGT, Matrix.SPD);
         myConMassVersion = myMassVersion;
         myConGTVersion = myGTVersion;
      }
      myConSolver.factor (myMass, velSize, myGT, myRg, myNT, myRn);
      myConSolver.solve (vel, myLam, myThe, myBf, myBg, myBn);
      //vel.set (myVel);
      mySys.setBilateralImpulses (myLam, h);
      mySys.setUnilateralImpulses (myThe, h);

      if (myUpdateForcesAtStepEnd) {
         if (myGsize > 0) {
            myGT.mulAdd (myFcon, myLam, velSize, myGsize);
         }
         if (myNsize > 0) {
            myNT.mulAdd (myFcon, myThe, velSize, myNsize);
         }
      }

   }

   public void projectPosConstraints (double t) {
      updateStateSizes();
      updateMassMatrix (t);

      VectorNd q = new VectorNd (myActivePosSize);
      VectorNd u = new VectorNd (myActiveVelSize);
      StepAdjustment stepAdjust = new StepAdjustment();

      mySys.updateConstraints (
         t, stepAdjust, /*flags=*/MechSystem.COMPUTE_CONTACTS);
      mySys.getActivePosState (q);
      computePosCorrections (q, u, t);
      mySys.setActivePosState (q);
      // mySys.updateConstraints (
      //    t, stepAdjust, /*flags=*/MechSystem.UPDATE_CONTACTS);

      // need to force an update of the mass matrix, since a subsequent
      // call to updateMassMatrix(t) wouldn't work.
      updateMassMatrix (-1);
   }

   protected boolean computeRigidBodyPosCorrections (VectorNd pos, double t) {
      // assumes that updateMassMatrix() has been called
      int velSize = myActiveVelSize;
      if (velSize == 0) {
         return false;
      }            
      if (myConSolver == null) {
         myConSolver = new KKTSolver();
      }
      updateBilateralConstraints (t);
      updateUnilateralConstraints (t);
      myVel.setSize (velSize);
      if (myGsize > 0 || myNsize > 0) {

         //mySys.getBilateralOffsets (myRg, myBg, 0, MechSystem.POSITION_MODE);
         mySys.getBilateralInfo (myGInfo);
         double[] gbuf = myBg.getBuffer();
         for (int i=0; i<myGsize; i++) {
            gbuf[i] = -myGInfo[i].dist;
         }
         myRg.setZero();

         //mySys.getUnilateralOffsets (myRn, myBn, 0, MechSystem.POSITION_MODE);
         mySys.getUnilateralInfo (myNInfo);
         double[] nbuf = myBn.getBuffer();
         for (int i=0; i<myNsize; i++) {
            nbuf[i] = -myNInfo[i].dist;
         }
         myRn.setZero();

         myVel.setZero();
         myLam.setZero();
         myThe.setZero();

         myRBSolver.updateStructure (myMass, myGT);
         if (myRBSolver.projectPosition (myMass,
            myGT, myNT, myBg, myBn, myVel, myLam, myThe)) {
            mySys.addActivePosImpulse (pos, 1, myVel);            
            return true;
         }
      }
      return false;
   }

   protected void computeMassPosCorrection (VectorNd vel, int velSize) {
      if (myConMassVersion != myMassVersion || myConGTVersion != myGTVersion) {
         myConSolver.analyze (myMass, velSize, myGT, Matrix.SPD);
         myConMassVersion = myMassVersion;
         myConGTVersion = myGTVersion;
      }
      myConSolver.factor (myMass, velSize, myGT, myRg, myNT, myRn);
      myConSolver.solve (vel, myLam, myThe, myBf, myBg, myBn);
   }

   protected void computeStiffnessPosCorrection (VectorNd vel, int velSize) {
      boolean analyze = false;
      updateSolveMatrixStructure();
      if (myKKTSolveMatrixVersion != mySolveMatrixVersion) {
         myKKTSolveMatrixVersion = mySolveMatrixVersion;
         analyze = true;
      }
      SparseNumberedBlockMatrix S =  mySolveMatrix;
      S.setZero();
      mySys.addVelJacobian (S, null, -1);
      mySys.addPosJacobian (S, null, -1);
      addActiveMassMatrix (mySys, S, 0);
      if (myKKTSolver == null) {
         myKKTSolver = new KKTSolver();
         analyze = true;
      }
      if (myKKTGTVersion != myGTVersion) {
         analyze = true;
         myKKTGTVersion = myGTVersion;
      }
      if (analyze) {
         myKKTSolver.analyze (S, velSize, myGT, mySys.getSolveMatrixType());
      }
      if (myHybridSolveP && !analyze &&
         (myNT == null || myNT.colSize() == 0)) {
         myKKTSolver.factorAndSolve (
            S, velSize, myGT, myRg, vel, myLam, myBf, myBg, myHybridSolveTol);
      }
      else {
         myKKTSolver.factor (S, velSize, myGT, myRg, myNT, myRn);
         myKKTSolver.solve (vel, myLam, myThe, myBf, myBg, myBn);
      }
   }

   protected void applyVelCorrection (VectorNd vel, double t0, double t1) {
      computeVelCorrections (vel, t0, t1);
      mySys.setActiveVelState (vel);

      projectFrictionConstraints (vel, t1);
      mySys.setActiveVelState (vel);
   }

   protected void updateActiveForces (double t0, double t1) {

      double h = t1-t0;
      mySys.updateForces (t1);
      mySys.getActiveForces (myF);
      // add accumulated constraint forces to the active forces
      myF.scaledAdd (1/h, myFcon);
      mySys.setActiveForces (myF);      
   }

   protected void applyPosCorrection (
      VectorNd pos, VectorNd vel,
      double t, StepAdjustment stepAdjust) {

      updateMassMatrix (-1);
      mySys.updateConstraints (
         t, stepAdjust, /*flags=*/MechSystem.COMPUTE_CONTACTS);
      computePosCorrections (pos, vel, t);

      mySys.setActivePosState (pos);
      // mySys.updateConstraints (
      //    t, stepAdjust, /*flags=*/MechSystem.UPDATE_CONTACTS);

      //mySys.updateForces (t1, stepAdjust);
   }

   protected void computePosCorrections (VectorNd pos, VectorNd vel, double t) {

      boolean correctionNeeded = false;
      // assumes that updateMassMatrix() has been called
      int velSize = myActiveVelSize;
      if (velSize == 0) {
         return;
      }            
      if (myConSolver == null) {
         myConSolver = new KKTSolver();
      }
      updateBilateralConstraints (t);
      updateUnilateralConstraints (t);
      // myVel.setSize (velSize);
      if (myGsize > 0 || myNsize > 0) {
         boolean allConstraintsCompliant = true;
         mySys.getBilateralInfo (myGInfo);
         double[] gbuf = myBg.getBuffer();
         for (int i=0; i<myGsize; i++) {
            ConstraintInfo gi = myGInfo[i];
            if (!myComplianceSupported || gi.compliance == 0) {
               gbuf[i] = -myGInfo[i].dist;
               allConstraintsCompliant = false;
            }
            else {
               gbuf[i] = 0;
            }
         }
         myRg.setZero();

         //mySys.getUnilateralOffsets (myRn, myBn, 0, MechSystem.POSITION_MODE);
         mySys.getUnilateralInfo (myNInfo);
         double[] nbuf = myBn.getBuffer();
         for (int i=0; i<myNsize; i++) {
            ConstraintInfo ni = myNInfo[i];
            if (!myComplianceSupported || ni.compliance == 0) {
               nbuf[i] = -myNInfo[i].dist;
               allConstraintsCompliant = false;
            }
            else {
               nbuf[i] = 0;
            }
         }

         // only need to do the correction if some constraints are non-compliant
         if (!allConstraintsCompliant) {
            correctionNeeded = true;
            myRg.setZero();
            myRn.setZero();

            //System.out.println ("bn=" + myBn);
            myBf.setSize (velSize);
            myBf.setZero();
            if (myStabilization == PosStabilization.GlobalStiffness &&
               integratorIsImplicit (myIntegrator)) {
               computeStiffnessPosCorrection (vel, velSize);
            }
            else {
               computeMassPosCorrection (vel, velSize);
            }
         }
      }
      if (correctionNeeded) {
         mySys.addActivePosImpulse (pos, 1, vel);
      }
   }

   public void projectRigidBodyPosConstraints (double t) {
      updateStateSizes();
      updateMassMatrix (t);

      VectorNd q = new VectorNd (myActivePosSize);

      StepAdjustment steppingInfo = new StepAdjustment();

      mySys.updateConstraints (
         t, steppingInfo, /*flags=*/MechSystem.COMPUTE_CONTACTS);
      mySys.getActivePosState (q);

      if (computeRigidBodyPosCorrections (q, t)) {
         mySys.setActivePosState (q);
         // mySys.updateConstraints (
         //    t, steppingInfo,  /*flags=*/MechSystem.UPDATE_CONTACTS);
      }
   } 

   private void rotate (Matrix6d MR, Matrix6d M1, RotationMatrix3d R) {
      Matrix6d RR = new Matrix6d();
      RR.setSubMatrix00 (R);
      RR.setSubMatrix33 (R);
      MR.mul (RR, M1);
      MR.mulTransposeRight (MR, RR);
   }

   private RotationMatrix3d[] getRBW() {
      RotationMatrix3d[] RBW = new RotationMatrix3d[2*myNumActive];
      MechSystemBase base = (MechSystemBase)mySys;
      for (int i=0; i<myNumActive; i++) {
         DynamicComponent c = base.myDynamicComponents.get(i);
         RotationMatrix3d R = new RotationMatrix3d(((RigidBody)c).getPose().R);
         RBW[i*2+0] = R;         
         RBW[i*2+1] = R;
      }
      return RBW;
   }

   private RotationMatrix3d[] getRWB() {
      RotationMatrix3d[] RWB = new RotationMatrix3d[2*myNumActive];
      MechSystemBase base = (MechSystemBase)mySys;
      for (int i=0; i<myNumActive; i++) {
         DynamicComponent c = base.myDynamicComponents.get(i);
         RotationMatrix3d R = new RotationMatrix3d(((RigidBody)c).getPose().R);
         R.transpose();
         RWB[i*2+0] = R;         
         RWB[i*2+1] = R;
      }
      return RWB;
   }

   private MatrixNd preRotate (MatrixNd M1, RotationMatrix3d[] R) {
      int nbr = R.length;
      MatrixNd MR = new MatrixNd (M1.rowSize(), M1.colSize());
      Vector3d v3 = new Vector3d();
      for (int bi=0; bi<nbr; bi++) {
         for (int j=0; j<M1.colSize(); j++) {
            v3.x = M1.get(bi*3+0, j);
            v3.y = M1.get(bi*3+1, j);
            v3.z = M1.get(bi*3+2, j);
            v3.transform (R[bi]);
            MR.set (bi*3+0, j, v3.x);
            MR.set (bi*3+1, j, v3.y);
            MR.set (bi*3+2, j, v3.z);
         }
      }
      return MR;
   }

   private MatrixNd fullRotate (MatrixNd M1, RotationMatrix3d[] R) {
      int nbr = R.length;
      MatrixNd MT = new MatrixNd (M1.rowSize(), M1.colSize());
      MatrixNd MR = new MatrixNd (M1.rowSize(), M1.colSize());
      Vector3d v3 = new Vector3d();
      for (int bi=0; bi<nbr; bi++) {
         for (int j=0; j<M1.colSize(); j++) {
            v3.x = M1.get(bi*3+0, j);
            v3.y = M1.get(bi*3+1, j);
            v3.z = M1.get(bi*3+2, j);
            v3.transform (R[bi]);
            MT.set (bi*3+0, j, v3.x);
            MT.set (bi*3+1, j, v3.y);
            MT.set (bi*3+2, j, v3.z);
         }
      }
      for (int bj=0; bj<nbr; bj++) {
         for (int i=0; i<M1.rowSize(); i++) {
            v3.x = MT.get(i, bj*3+0);
            v3.y = MT.get(i, bj*3+1);
            v3.z = MT.get(i, bj*3+2);
            v3.transform (R[bj]);
            MR.set (i, bj*3+0, v3.x);
            MR.set (i, bj*3+1, v3.y);
            MR.set (i, bj*3+2, v3.z);
         }
      }
      return MR;
   }

   private VectorNd preRotate (VectorNd vec, RotationMatrix3d R) {
      int nblks = vec.size()/3;
      RotationMatrix3d[] Rlist = new RotationMatrix3d[nblks];
      for (int i=0; i<nblks; i++) {
         Rlist[i] = R;
      }
      return preRotate (vec, Rlist);
   }         

   private VectorNd preRotate (VectorNd vec, RotationMatrix3d[] R) {
      int nbr = R.length;
      VectorNd vr = new VectorNd (vec.size());
      Vector3d v3 = new Vector3d();
      for (int bi=0; bi<nbr; bi++)  {
         v3.x = vec.get(bi*3+0);
         v3.y = vec.get(bi*3+1);
         v3.z = vec.get(bi*3+2);
         v3.transform (R[bi]);
         vr.set (bi*3+0, v3.x);
         vr.set (bi*3+1, v3.y);
         vr.set (bi*3+2, v3.z);
      }
      return vr;
   }

   private void rotate (VectorNd vr, VectorNd v1, RotationMatrix3d R) {
      Vector3d v3 = new Vector3d();
      v3.x = v1.get(0);
      v3.y = v1.get(1);
      v3.z = v1.get(2);
      v3.transform (R);
      vr.set (0, v3.x);
      vr.set (1, v3.y);
      vr.set (2, v3.z);
      v3.x = v1.get(3);
      v3.y = v1.get(4);
      v3.z = v1.get(5);
      v3.transform (R);
      vr.set (3, v3.x);
      vr.set (4, v3.y);
      vr.set (5, v3.z);
   }

   private void printTvel (String name, VectorNd vel, String fmt) {
      System.out.print (name + " ");
      for (int i=0; i<myNumActive; i++) {
         Vector3d vt =
            new Vector3d (vel.get(i*6  ), vel.get(i*6+1), vel.get(i*6+2));
         System.out.print (vt.toString (fmt));
         if (i < myNumActive-1) {
            System.out.print (" ");
         }
      }
      System.out.println ("");
   }

   // protected void computeImplicitParametricForces (VectorNd vel, VectorNd pfict) {
   //    // back solve for parametric forces for an implicit integrator
   //    int velSize = myActiveVelSize;
   //    int parVelSize = myParametricVelSize;
   //    if (parVelSize > 0) {
   //       mySys.getParametricForces (myFpar, 0);
   //       mySolveMatrix.mulAdd (
   //          myFpar, vel, velSize, parVelSize, 0, velSize);
   //       mySolveMatrix.mulAdd (
   //          myFpar, myUpar, velSize, parVelSize, velSize, parVelSize);
   //       myFpar.add (pfict);
   //       mySys.setParametricForces (myFpar, 0);
   //    }
   // }

   // protected void computeExplicitParametricForces (VectorNd vel) {
   //    // back solve for parametric forces for an implicit integrator
   //    int velSize = myActiveVelSize;
   //    int parVelSize = myParametricVelSize;
   //    if (parVelSize > 0) {
   //       mySys.getParametricForces (myFpar, 0);
   //       myMassMatrix.mulAdd (
   //          myFpar, myUpar, velSize, parVelSize, velSize, parVelSize);
   //       mySys.setParametricForces (myFpar, 0);
   //    }
   // }

   public void constrainedBackwardEuler (
      double t0, double t1, StepAdjustment stepAdjust) {

      if (myMatrixSolver == MatrixSolver.None) {
         throw new UnsupportedOperationException (
            "MatrixSolver cannot be 'None' for this integrator");
      }
      double h = t1 - t0;

      int velSize = myActiveVelSize;
      int posSize = myActivePosSize;

      myB.setSize (velSize);
      myUtmp.setSize (velSize);
      myF.setSize (velSize);
      myU.setSize (velSize);
      myQ.setSize (posSize);
      //dxdtVec.setSize (posSize);
      myFparC.setSize (myParametricVelSize);

      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t1);

      // b = M v

      mySys.getActiveVelState (myU);
      mulActiveInertias (myB, myU);
      mySys.getActiveForces (myF);
      myF.add (myMassForces);
      myB.scaledAdd (h, myF, myB);

      KKTFactorAndSolve (
         myUtmp, myFparC, myB,
         /*tmp=*/myF, myU, t1, h, /*velocitySolve=*/true);

      mySys.setActiveVelState (myUtmp);

      if (useGlobalFriction) {
         projectFrictionConstraints (myUtmp, t1);
         mySys.setActiveVelState (myUtmp);
      }

      // back solve for parametric forces
      //computeImplicitParametricForces (myUtmp, myFparC);

      mySys.getActivePosState (myQ);

      mySys.addActivePosImpulse (myQ, h, myUtmp);

      mySys.setActivePosState (myQ);


      applyPosCorrection (myQ, myUtmp, t1, stepAdjust);
   }

   private double computeForceResidual (double t0, double t1, int velSize) {
      double h = t1 - t0;
      myDelV.setSize (velSize);
      myFx.setSize (velSize);

      myDelV.sub (myU, myUtmp);
      mulActiveInertias (myFx, myDelV);
      myGT.mulAdd (myFx, myLam, velSize, myGT.colSize());
      if (myNT != null) {
         myNT.mulAdd (myFx, myThe, velSize, myNT.colSize());
      }
      myFx.scale (-1/h);

      double fxMag = myFx.norm();
      mySys.getActiveForces (myF);
      // XXX mass forces need to be updated?
      myF.add (myMassForces);
      myFx.sub (myF);
      double resMag = myFx.norm();
      double fMag = myF.norm();
      if (fMag < 1e-8) {
         return 0;
      }
      else {
         return resMag/fMag;
      }
   }

   public void fullBackwardEuler (double t0, double t1, StepAdjustment stepAdjust) {
      if (myMatrixSolver == MatrixSolver.None) {
         throw new UnsupportedOperationException (
            "MatrixSolver cannot be 'None' for this integrator");
      }
      double h = t1 - t0;

      int velSize = myActiveVelSize;
      int posSize = myActivePosSize;

      VectorNd xVec0 = new VectorNd (posSize);

      myB.setSize (velSize);
      myUtmp.setSize (velSize);
      myF.setSize (velSize);
      myU.setSize (velSize);
      myQ.setSize (posSize);
      //dxdtVec.setSize (posSize);
      myFparC.setSize (myParametricVelSize);

      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t1);

      // b = M v

      mySys.getActiveVelState (myU);
      mulActiveInertias (myB, myU);
      mySys.getActiveForces (myF);
      myF.add (myMassForces);
      myB.scaledAdd (h, myF, myB);

      KKTFactorAndSolve (
         myUtmp, myFparC, myB, /*tmp=*/myF, myU, t1, h, /*velocitySolve=*/true);

      mySys.setActiveVelState (myUtmp);
      mySys.getActivePosState (xVec0);
      //mySys.getActivePosDerivative (dxdtVec, t1, 0);
      //xVec.scaledAdd (h, dxdtVec, xVec0);
      myQ.set (xVec0);
      mySys.addActivePosImpulse (myQ, h, myUtmp);
      mySys.setActivePosState (myQ);

      double fres = computeForceResidual (t0, t1, velSize);

      double FRES_TOL = 0.0001;
      int MAX_ITER = 5;
      int iter = 0;

      //System.out.println ("vel=" + myUtmp.toString ("%10.4f"));
      System.out.printf ("fres[%d]=%g\n", 0, fres);

      if (fres > FRES_TOL) {
         VectorNd velk = new VectorNd (velSize);
         while (fres > FRES_TOL && iter < MAX_ITER) {
            velk.set (myUtmp);

            mulActiveInertias (myB, myU);
            mySys.getActiveForces (myF);
            // XXX mass forces need to be updated?
            myF.add (myMassForces);
            myB.scaledAdd (h, myF, myB);

            KKTFactorAndSolve (
               myUtmp, myFparC, myB, /*tmp=*/myF, velk, t1, h,
               -h, -h*h, -h, -h*h, /*velocitySolve=*/true);

            mySys.setActiveVelState (myUtmp);
            //mySys.getActivePosDerivative (dxdtVec, t1, 0);
            //xVec.scaledAdd (h, dxdtVec, xVec0);
            myQ.set (xVec0);
            mySys.addActivePosImpulse (myQ, h, myUtmp);
            mySys.setActivePosState (myQ);

            fres = computeForceResidual (t0, t1, velSize);

            iter++;

            //System.out.println ("vel=" + myUtmp.toString ("%10.4f"));
            System.out.printf ("fres[%d]=%g\n", iter, fres);
         }
      }
      System.out.println ("iter=" + iter);

      //computeImplicitParametricForces (myUtmp, myFparC);

      projectFrictionConstraints (myUtmp, t1);
      mySys.setActiveVelState (myUtmp);

      applyPosCorrection (myQ, myUtmp, t1, stepAdjust);
   }

   public void trapezoidal (double t0, double t1, StepAdjustment stepAdjust) {
      if (myMatrixSolver == MatrixSolver.None) {
         throw new UnsupportedOperationException (
            "MatrixSolver cannot be 'None' for this integrator");
      }
      double h = t1 - t0;

      int velSize = myActiveVelSize;
      int posSize = myActivePosSize;

      myB.setSize (velSize);
      myUtmp.setSize (velSize);
      myF.setSize (velSize);
      myU.setSize (velSize);
      myQ.setSize (posSize);
      myFparC.setSize (myParametricVelSize);
      //dxdtVec.setSize (posSize);
      //dxdtVec0.setSize (posSize);

      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t1);

      // b = M v
      mySys.getActiveVelState (myU);
      mulActiveInertias (myB, myU);
      mySys.getActiveForces (myF);
      myF.add (myMassForces);
      myB.scaledAdd (h, myF, myB);

      KKTFactorAndSolve (
         myUtmp, myFparC, myB, /*tmp=*/myF, myU, 
         t1, h, -h/2, -h*h/4, -h/2, h*h/4, /*velocitySolve=*/true);

      //mySys.getActivePosDerivative (dxdtVec0, t0, 0);

      mySys.setActiveVelState (myUtmp);
      if (useGlobalFriction) {
         projectFrictionConstraints (myUtmp, t1);
         mySys.setActiveVelState (myUtmp);
      }
      //      else {
      //         mySys.projectVelConstraints (t0, t1);
      //      }

      // back solve for parametric forces
      //computeImplicitParametricForces (myUtmp, myFparC);

      mySys.getActivePosState (myQ);
      //mySys.getActivePosDerivative (dxdtVec, t1, 0);
      //xVec.scaledAdd (h/2, dxdtVec0, xVec);
      //xVec.scaledAdd (h/2, dxdtVec, xVec);
      mySys.addActivePosImpulse (myQ, h/2, myUtmp);
      mySys.addActivePosImpulse (myQ, h/2, myU);
      mySys.setActivePosState (myQ);

      applyPosCorrection (myQ, myUtmp, t1, stepAdjust);
   }

   public SparseBlockMatrix createActiveStiffnessMatrix (double h) {
      updateStateSizes();
      if (mySolveMatrixVersion != mySys.getStructureVersion()) {
         mySolveMatrixVersion = mySys.getStructureVersion();
         mySolveMatrix =
            new SparseNumberedBlockMatrix();
         mySys.buildSolveMatrix (mySolveMatrix);
      }
      mySolveMatrix.setZero();
      mySys.addPosJacobian (mySolveMatrix, null, h);
      int nactive = mySys.numActiveComponents();
      return mySolveMatrix.createSubMatrix (nactive, nactive);
   }

   public SparseBlockMatrix createActiveBilateralMatrix (double t) {
      updateStateSizes();
      updateBilateralConstraints (t);
      int nactive = mySys.numActiveComponents();
      return myGT.createSubMatrix (nactive, myGT.numBlockCols());
   }

   private MatrixNd buildA (
      SparseBlockMatrix S, int sizeS, SparseBlockMatrix GT, int sizeG) {

      MatrixNd A = new MatrixNd (sizeS+sizeG, sizeS+sizeG);
      for (int i=0; i<sizeS; i++) {
         for (int j=0; j<sizeS; j++) {
            A.set (i, j, S.get(i,j));
         }
         for (int j=0; j<sizeG; j++) {
            A.set (i, j+sizeS, GT.get(i, j));
            A.set (j+sizeS, i, GT.get(i, j));
         }
      }
      return A;
   }

   public void dispose() {
      if (myPardisoSolver != null) {
         myPardisoSolver.dispose();
         myPardisoSolver = null;
      }
      if (myKKTSolver != null) {
         myKKTSolver.dispose();
         myKKTSolver = null;
      }
      if (myConSolver != null) {
         myConSolver.dispose();
         myConSolver = null;
      }
      if (myUmfpackSolver != null) {
         myUmfpackSolver.dispose();
         myUmfpackSolver = null;
      }
      if (myRBSolver != null) {
         myRBSolver.dispose();
         myRBSolver = null;
      }
   }

   public void finalize() {
      dispose();
   }

}
