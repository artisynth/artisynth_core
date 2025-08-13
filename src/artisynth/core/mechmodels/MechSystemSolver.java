/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import maspack.spatialmotion.FrictionInfo;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.modelbase.*;
import artisynth.core.util.ArtisynthIO;
import maspack.function.Function1x1;
import maspack.matrix.EigenDecomposition;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix3x1;
import maspack.matrix.Matrix3x2;
import maspack.matrix.Matrix6x1;
import maspack.matrix.Matrix6x2;
import maspack.matrix.Matrix6dBase;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixNd;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseBlockSignature;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.solvers.CGSolver;
import maspack.solvers.DirectSolver;
import maspack.solvers.IterativeSolver;
import maspack.solvers.IterativeSolver.ToleranceType;
import maspack.solvers.KKTSolver;
import maspack.solvers.PardisoSolver;
import maspack.solvers.UmfpackSolver;
import maspack.solvers.LCPSolver;
import maspack.solvers.SparseSolverId;
import maspack.solvers.MurtyMechSolver;
import maspack.numerics.BrentRootSolver;
import maspack.numerics.GoldenSectionSearch;
import maspack.spatialmotion.FrictionInfo;
import maspack.util.FunctionTimer;
import maspack.util.DataBuffer;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;

/**
 * Implements implicit integration for MechSystem
 */
public class MechSystemSolver {
   MechSystem mySys;
   RigidBodySolver myRBSolver;

   /**
    * Flag for KKTSolverFactorAndSolve methods: do not update the system with
    * the computed velocities and constraint forces.
    */
   public static int NO_SYS_UPDATE = 0x02;
   
   /**
    * Flag for KKTSolverFactorAndSolve methods: indicates trapezoidal 
    * integration
    */
   public static int TRAPEZOIDAL = 0x04;

   public boolean profileKKTSolveTime = false;
   public boolean profileWholeSolve = false;
   public boolean profileConstrainedBE = false;
   public boolean profileImplicitFriction = false;
   // always updating friction causes inverseMassMatrix updates
   public boolean alwaysProjectFriction = true;

   public boolean myMurtyVelSolveRebuild = false;
   public boolean myUseImplicitFriction = false;
   
   public static boolean useFictitousJacobianForces = true;
   // always do an analysis phase before KKTsolves. Only used for testing
   public static boolean myAlwaysAnalyze = false;
   
   private boolean myUpdateForcesAtStepEnd = false;
   private boolean computeKKTResidual = false;

   // mass matrix stuff

   private int myMassVersion = -1;
   private double myMassTime = -1;
   private int myInverseMassVersion = -1;
   private double myInverseMassTime = -1;
   private boolean myMassConstantP = true;
   private boolean myInverseMassConstantP = true;
   private SparseNumberedBlockMatrix myMass;  
   private VectorNd myMassForces;
   private SparseNumberedBlockMatrix myInverseMass;
   private VectorNd myBf = new VectorNd();

   // bilateral constraints

   SparseBlockMatrix myGT;
   SparseBlockSignature myGTSignature;
   private int myGsize = -1;
   private ConstraintInfo[] myGInfo = new ConstraintInfo[0];
   private VectorNd myGdot = new VectorNd();
   private VectorNd myRg = new VectorNd();
   private VectorNd myBg = new VectorNd();
   private VectorNd myLam = new VectorNd();
   private int myGTSystemVersion = -1;
   private boolean myGTVersionValid = false;
   private int myGTVersion = -1;

   // unilateral constraints

   SparseBlockMatrix myNT;
   private int myNsize = -1;
   private ConstraintInfo[] myNInfo = new ConstraintInfo[0];
   private VectorNd myNdot = new VectorNd();
   private VectorNd myRn = new VectorNd();
   private VectorNd myBn = new VectorNd();
   private VectorNd myThe = new VectorNd();
   private int myNTSystemVersion = -1;

   // friction constraints

   SparseBlockMatrix myDT;
   private int myDSize = -1;
   VectorNd myRd = new VectorNd();
   VectorNd myBd = new VectorNd();
   VectorNd myPhi = new VectorNd();
   ArrayList<FrictionInfo> myFrictionInfo = new ArrayList<>();
   private int myFrictionIters = 1;
   private int myNumFrictionEntries = 0;
   private int myFullDSize = 0;

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

   private VectorNd myW = new VectorNd (0); // aux variables
   private VectorNd myDwdt = new VectorNd (0); // aux variable derivatives
   private VectorNd myWtmp = new VectorNd (0); // temp aux variables

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
   private int myStaticKKTVersion = -1;
   private boolean myForceAnalyzeInMurtySolver = true;

   public static boolean DEFAULT_HYBRID_SOLVES_ENABLED = true;
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
   int myAuxVarSize = 0;
   int myNumActive = 0;
   int myNumParametric = 0;

   FunctionTimer myKKTTimer = new FunctionTimer();
   int myKKTCnt = 0;
   FunctionTimer mySolveTimer = new FunctionTimer();
   int mySolveCnt = 0;

   VectorNd myVel = new VectorNd();
   //VectorNd myPos = new VectorNd();

   public static double myT1; // for debugging
   
   public void setUpdateForcesAtStepEnd (boolean enable) {
      myUpdateForcesAtStepEnd = enable;
   }

   public boolean getUpdateForcesAtStepEnd() {
      return myUpdateForcesAtStepEnd;
   }

   public static boolean getAlwaysAnalyze() {
      return myAlwaysAnalyze;
   }

   public static void setAlwaysAnalyze (boolean enable) {
      myAlwaysAnalyze = enable;
   }

   /**
    * Queries whether hybrid sparse matrix solves are enabled by default.
    * @return {@code true} if hybrid solves are enabled
    */
   public static boolean getHybridSolvesEnabled() {
      return myDefaultHybridSolveP;
   }

   /**
    * Set hybrid sparse matrix solves to be enabled or disabled by default.
    * @param enable if {@code true}, enables hybrid solves
    */
   public static void setHybridSolvesEnabled (boolean enable) {
      myDefaultHybridSolveP = enable;
   }

   public static enum Integrator {
      ForwardEuler,
      BackwardEuler,
      SymplecticEuler,
      RungeKutta4,
      ConstrainedForwardEuler,
      // SymplecticEulerX,
      ConstrainedBackwardEuler,
      FullBackwardEuler,
      Trapezoidal,
      //      BridsonMarino
      //      Trapezoidal2,
      StaticIncrementalStep,
      StaticIncremental,
      StaticLineSearch
   }

   private boolean integratorIsImplicit (Integrator integrator) {
      return (integrator == Integrator.BackwardEuler ||
         integrator == Integrator.ConstrainedBackwardEuler ||
         integrator == Integrator.Trapezoidal
         );
   }

   /** 
    * Indicates the method by which positions should be stabilized.
    */
   public enum PosStabilization {
      /**
       * Use only the mass matrix. Good for less stiff systems.
       */
      GlobalMass,

      /**
       * Use the combined mass-stiffness matrix. More accurate
       * and stable, but more computationally expensive.
       */
      GlobalStiffness,

      /**
       * No stabilization. Usually used only for debugging or testing.
       */
      None;

      // like valueOf() but returns null if no match
      public static PosStabilization fromString (String str) {
         try {
            return valueOf (str);
         }
         catch (Exception e) {
            return null;
         }
      }
   }

   public boolean getHybridSolve () {
      return myHybridSolveP;
   }

   public void setHybridSolve (boolean enable) {
      myHybridSolveP = enable;
      if (myMurtySolver != null) {
         myMurtySolver.setHybridSolves (enable);
      }
   }

   public boolean getUseImplicitFriction () {
      return myUseImplicitFriction;
   }

   public void setUseImplicitFriction (boolean enable) {
      myUseImplicitFriction = enable;
   }
   
   public boolean usingImplicitFriction() {
      return (myUseImplicitFriction && 
              (myIntegrator == Integrator.ConstrainedBackwardEuler ||
               myIntegrator == Integrator.Trapezoidal ||
               myIntegrator == Integrator.FullBackwardEuler));
   }

   public int getFrictionIterations () {
      return myFrictionIters;
   }

   public void setFrictionIterations (int num) {
      myFrictionIters = num;
   }

   PardisoSolver myPardisoSolver;
   UmfpackSolver myUmfpackSolver;
   KKTSolver myKKTSolver;
   KKTSolver myConSolver;
   KKTSolver myStaticSolver;
   MurtyMechSolver myMurtySolver;

   private SparseSolverId myMatrixSolver = SparseSolverId.Pardiso;
   Integrator myIntegrator = Integrator.SymplecticEuler;
   boolean myComplianceSupported = true; // true for default integrator
   double myTol = 0.01;
   ToleranceType myTolType = ToleranceType.RelativeResidual;
   int myMaxIterations = 20;
   boolean myUseDirectSolver = true;
   PosStabilization myStabilization = PosStabilization.GlobalMass;
   
   double myStaticTikhonov = -1;  // tikhonov regularization parameter for static solves
   double myStaticTol = 1e-8;    // static solver tolerance (small displacement value per element)
   int myStaticIncrements = 20;  // number of load increments for static solve

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
      else if (!myMassConstantP && (t == -1 || myMassTime != t)) {
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
         myInverseMassConstantP = mySys.buildMassMatrix (myInverseMass);
         mySys.getInverseMassMatrix (myInverseMass, myMass);
         myInverseMassVersion = version;
         myInverseMassTime = t;
         structureChanged = true;
      }
      else if (!myInverseMassConstantP && (t == -1 || myInverseMassTime != t)) {
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

         myQ.setSize (myActivePosSize); // positions
         myU.setSize (myActiveVelSize); // velociies
         myUtmp.setSize (myActiveVelSize); // temp velocities
         myF.setSize (myActiveVelSize); // forces

         if (myUpdateForcesAtStepEnd) {
            myFcon.setSize (myActiveVelSize);
         }

         myQpar.setSize (myParametricPosSize); // parametric positions
         myUpar.setSize (myParametricVelSize); // parametric velocities
         myUpar0.setSize (myParametricVelSize); // initial parametric velocities
         myFpar.setSize (myParametricVelSize); // parametric forces

         // auxiliary variables
         myAuxVarSize = mySys.getAuxVarStateSize();
         myW.setSize (myAuxVarSize);
         myWtmp.setSize (myAuxVarSize);
         myDwdt.setSize (myAuxVarSize);

         myVel.setSize (myActiveVelSize);

         myStateSizeVersion = version;

         myNumActive = mySys.numActiveComponents();
         myNumParametric = mySys.numParametricComponents();
      }
   }

   private int updateExplicitSolveStateSizes() {
      myDudt.setSize (myActiveVelSize);
      myDqdt.setSize (myActivePosSize);
      return myActiveVelSize;
   }

   private int updateImplicitSolveStateSizes() {
      myB.setSize (myActiveVelSize);
      myFparC.setSize (myParametricVelSize);
      return myActiveVelSize;
   }

   private int updateStaticSolveStateSizes() {
      myB.setSize (myActiveVelSize);
      myFx.setSize (myParametricVelSize);
      return myActiveVelSize;
   }

   private void ensureFrictionCapacity (
      ArrayList<FrictionInfo> finfo, int cap) {
      while (finfo.size() < cap) {
         finfo.add (new FrictionInfo());
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
   
   public void setIntegrator (Integrator integrator) {
      if (myIntegrator != integrator) {
         switch (integrator) {
            case ConstrainedBackwardEuler:
            case Trapezoidal:
            case FullBackwardEuler: {
               myComplianceSupported = true;
               break;
            }
            case StaticIncrementalStep:
            case StaticIncremental:
            case StaticLineSearch:
               myComplianceSupported = false;
               break;
            default: {
               myComplianceSupported = true;
               break;
            }
         }
         mySolveMatrix = null;
         resetMatrixVersions();
         myIntegrator = integrator;
      }
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

   public void setMatrixSolver (SparseSolverId solver) {
      if (solver != myMatrixSolver) {
         switch (solver) {
            case Pardiso: 
            case Umfpack: {
               break;
            }
            default: {
               System.out.println ("Matrix solver "+solver+" not supported");
               return;
            }
         }
         mySolveMatrix = null;
         //myKKTSolveMatrix = null;
         myMatrixSolver = solver;
         disposeSolvers(); // remove existing solvers
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
         default: {
            throw new InternalErrorException (
               "Unknown solver " + myMatrixSolver);
         }
      }
   }

   public SparseSolverId getMatrixSolver() {
      return myMatrixSolver;
   }

   /** 
    * Create a new MechSystem solver for a specified MechSystem.
    */
   public MechSystemSolver (MechSystem system) {
      mySys = system;
      //myRBSolver = new RigidBodySolver (system);
      setIntegrator (Integrator.SymplecticEuler);
      setHybridSolve (myDefaultHybridSolveP);
      if (system instanceof MechSystemBase) {
         setMatrixSolver (((MechSystemBase)system).getMatrixSolver());
      }
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
      setUseImplicitFriction (solver.getUseImplicitFriction());
   }

   public void nonDynamicSolve (double t0, double t1, StepAdjustment stepAdjust) {
      updateStateSizes();
      setParametricTargets (1, t1-t0);
   }

   private void singleStepAuxComponents (double t0, double t1) {
      mySys.advanceAuxState (t0, t1);
      if (myAuxVarSize > 0) {
         mySys.getAuxVarState (myW);
         mySys.getAuxVarDerivative (myDwdt);
         myWtmp.scaledAdd (t1-t0, myDwdt, myW);
         mySys.setAuxVarState (myWtmp);
      }
   }

   private void subStepAuxComponents (
      DataBuffer auxState0, double wgt, VectorNd dwdtAvg, 
      double t0, double t1, double endWgt) {
      auxState0.resetOffsets();
      mySys.setAuxAdvanceState (auxState0);
      mySys.advanceAuxState (t0, t1);
      if (myAuxVarSize > 0) {
         mySys.getAuxVarDerivative (myDwdt);
         dwdtAvg.scaledAdd (wgt, myDwdt);
         if (endWgt == 0) {
            myWtmp.scaledAdd (t1-t0, myDwdt, myW); 
            mySys.setAuxVarState (myWtmp);
         }
         else {
            // last sub-step, advance w by weighted dwdtAvg 
            myWtmp.scaledAdd ((t1-t0)/endWgt, dwdtAvg, myW); 
            mySys.setAuxVarState (myWtmp);
         }
      }
   }

   private void advanceAuxVar (double h, VectorNd dwdt) {
   }

   public void solve (
      double t0, double t1, StepAdjustment stepAdjust) {

      myT1 = t1;
      if (profileWholeSolve) {
         mySolveTimer.restart();
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
         case ConstrainedForwardEuler: {
            constrainedForwardEuler (t0, t1, stepAdjust);
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
         case StaticIncrementalStep: {
            staticIncrementalStep (t0, t1, 1.0/myStaticIncrements, stepAdjust);
            break;
         }
         case StaticIncremental: {
            staticIncremental (t0, t1, myStaticIncrements, stepAdjust);
            break;
         }
         case StaticLineSearch: {
            staticLineSearch (t0, t1, stepAdjust);
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
         mySolveTimer.stop();
         mySolveCnt++;
         if ((mySolveCnt%100) == 0) {
            System.out.println ("wholeSolve " + mySolveTimer.result(mySolveCnt));
         }
      }
      myForceAnalyzeInMurtySolver = false;
      //System.out.println ("t1=" + t1);
   }

   protected void forwardEuler (double t0, double t1, StepAdjustment stepAdjust) {
      // boolean useBodyCoords = useBodyCoordsForExplicit;
      double h = t1 - t0;

      updateExplicitSolveStateSizes();

      singleStepAuxComponents (t0, t1);
      mySys.updateForces (t0);
      updateInverseMassMatrix (t0);

      getActiveVelDerivative (myDudt, myF);
      mySys.getActiveVelState (myU);
      mySys.getActivePosState (myQ);
      mySys.addActivePosImpulse (myQ, h, myU);
      mySys.setActivePosState (myQ);
      myU.scaledAdd (h, myDudt, myU);
      mySys.setActiveVelState (myU);         

      mySys.updateConstraints (t0, null, MechSystem.UPDATE_CONTACTS);
      applyVelCorrection (myU, t0, t1);

      applyPosCorrection (
         myQ, myUtmp, t1, stepAdjust);
   }

   //return constraint forces
   public VectorNd getLambda()
   {
      return myLam;  
   }

   double maxErr = 0;

   protected void printComp (String msg, VectorNd vec, int off, int size) {
      VectorNd sub = new VectorNd(size);
      vec.getSubVector (off, sub);
      System.out.println (msg + sub);
   }

   boolean debug = false;

   protected void symplecticEuler (
      double t0, double t1, StepAdjustment stepAdjust) {
      double h = t1 - t0;

      updateExplicitSolveStateSizes();

      singleStepAuxComponents (t0, t1);

      mySys.updateForces (t0);
      updateInverseMassMatrix (t0);

      getActiveVelDerivative (myDudt, myF);
      mySys.getActiveVelState (myU);
      myU.scaledAdd (h, myDudt, myU);
      mySys.setActiveVelState (myU);

      mySys.updateConstraints (t0, null, MechSystem.UPDATE_CONTACTS);
      applyVelCorrection (myU, t0, t1);

      mySys.getActivePosState (myQ);
      mySys.addActivePosImpulse (myQ, h, myU);
      mySys.setActivePosState (myQ);
      applyPosCorrection (
         myQ, myUtmp, t1, stepAdjust);
   }

   /**
    * Experimental version of Symplectic Euler that integrates
    * velocity constraints directly into the velocitys solve.
    */
   protected void symplecticEulerX (
      double t0, double t1, StepAdjustment stepAdjust) {
      double h = t1 - t0;

      updateExplicitSolveStateSizes();

      singleStepAuxComponents (t0, t1);

      mySys.updateConstraints (t0, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t0);

      mySys.getActiveVelState (myU);
      mySys.getActiveForces (myF);
      constrainedVelSolve (myU, myF, t0, t1);
      mySys.setActiveVelState (myU);         
     
      if (updateAndProjectFrictionConstraints (myU, t0, h)) {
         mySys.setActiveVelState (myU);
      }

      mySys.getActivePosState (myQ);
      mySys.addActivePosImpulse (myQ, h, myU);
      mySys.setActivePosState (myQ);
      applyPosCorrection (myQ, myUtmp, t1, stepAdjust);
   }

   protected void rungeKutta4 (double t0, double t1, StepAdjustment stepAdjust) {
      double h = t1 - t0;
      double th = (t0 + t1) / 2;

      updateExplicitSolveStateSizes();
      myDudtAvg.setSize (myActiveVelSize);
      myQtmp.setSize (myActivePosSize);
      myDqdtAvg.setSize (myActivePosSize);

      // initialize objects for aux state
      VectorNd wtmp = null;
      VectorNd dwdtAvg = null;
      DataBuffer auxState0 = new DataBuffer();
      mySys.getAuxAdvanceState (auxState0);
      if (myAuxVarSize > 0) {
         wtmp = new VectorNd (myAuxVarSize);
         dwdtAvg = new VectorNd (myAuxVarSize);
      }

      // step by h/2, using derivatives k1 evaluated at t0

      singleStepAuxComponents (t0, th);
      if (dwdtAvg != null) {
         dwdtAvg.set (myDwdt);
      }
      mySys.updateForces (t0);
      mySys.getActivePosState (myQ);
      mySys.getActiveVelState (myU);
      updateInverseMassMatrix (t0);
      mySys.getActivePosDerivative (myDqdtAvg, t0); // k1 derivatives
      getActiveVelDerivative (myDudtAvg, myF);
      myQtmp.scaledAdd (h/2, myDqdtAvg, myQ);
      myUtmp.scaledAdd (h/2, myDudtAvg, myU);
      mySys.setActivePosState (myQtmp);
      mySys.setActiveVelState (myUtmp);
      updateMassMatrix (-1);
      updateInverseMassMatrix (-1);

      // step by h/2 again, using derivatives k2 evaluated at t0 + h/2

      subStepAuxComponents (auxState0, 2, dwdtAvg, t0, th, /*endWgt=*/0);

      mySys.updateForces (th);
      mySys.getActivePosDerivative (myDqdt, th); // k2 derivatives
      getActiveVelDerivative (myDudt, myF);
      myDqdtAvg.scaledAdd (2, myDqdt, myDqdtAvg);
      myDudtAvg.scaledAdd (2, myDudt, myDudtAvg);
      myQtmp.scaledAdd (h/2, myDqdt, myQ);
      myUtmp.scaledAdd (h/2, myDudt, myU);
      mySys.setActivePosState (myQtmp);
      mySys.setActiveVelState (myUtmp);
      updateMassMatrix (-1);
      updateInverseMassMatrix (-1);

      // step by h, using derivatives k3 evaluated at t0 + h/2

      subStepAuxComponents (auxState0, 2, dwdtAvg, t0, t1, /*endWgt=*/0);

      mySys.updateForces (th);
      mySys.getActivePosDerivative (myDqdt, th); // k3 derivatives
      getActiveVelDerivative (myDudt, myF);
      myDqdtAvg.scaledAdd (2, myDqdt, myDqdtAvg);
      myDudtAvg.scaledAdd (2, myDudt, myDudtAvg);
      myQtmp.scaledAdd (h, myDqdt, myQ);
      myUtmp.scaledAdd (h, myDudt, myU);
      mySys.setActivePosState (myQtmp);
      mySys.setActiveVelState (myUtmp);
      updateMassMatrix (-1);
      updateInverseMassMatrix (-1);

      // final step by h, using average derivative (k1 + 2 k2 + 2 k3 + k4)/6.
      // where k4 is the derivative evaluated at t1

      subStepAuxComponents (auxState0, 1, dwdtAvg, t0, t1, /*endWgt=*/6);

      mySys.updateForces (t1);
      mySys.getActivePosDerivative (myDqdt, t1); // k4 derivatives
      getActiveVelDerivative (myDudt, myF);
      myDqdtAvg.add (myDqdt);
      myDudtAvg.add (myDudt);
      myUtmp.scaledAdd (h/6, myDudtAvg, myU);
      myQtmp.scaledAdd (h/6, myDqdtAvg, myQ);
      mySys.setActivePosState (myQtmp);
      mySys.setActiveVelState (myUtmp);         

      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);

      applyVelCorrection (myUtmp, t0, t1);
      applyPosCorrection (
         myQtmp, myUtmp, t1, stepAdjust);
   }

   private void timerStart (FunctionTimer timer) {
      timer.start();
   }

   private void timerStop (String msg, FunctionTimer timer) {
      timer.stop();
      System.out.println (msg + ": " + timer.result(1));
   }

   private void timerStopStart (String msg, FunctionTimer timer) {
      timer.stop();
      System.out.println (msg + ": " + timer.result(1));
      timer.start();
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
      MechSystem sys, SparseBlockMatrix S) {
      // assumes that updateMassMatrix() has been called
      int nactive = sys.numActiveComponents();
      S.add (myMass, nactive, nactive);
   }

   public void addScaledMassForces (VectorNd f, double s, double t) {
      updateStateSizes();
      updateMassMatrix (t);
      f.scaledAdd (s, myMassForces);
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

      boolean analyze = myAlwaysAnalyze;

      double h = t1 - t0;
      // timer.start();

      int vsize = updateImplicitSolveStateSizes();
      updateSolveMatrixStructure();
      myC.setSize (mySolveMatrix.rowSize()); 

      singleStepAuxComponents (t0, t1);
      mySys.updateForces (t1);

      // b = M v
      mySys.getActiveVelState (myU);
      mulActiveInertias (myB, myU);

      mySys.getActiveForces (myF);
      myF.add (myMassForces);
      myB.scaledAdd (h, myF, myB);

      mySolveMatrix.setZero();
      myC.setZero ();
      mySys.addVelJacobian (mySolveMatrix, myC, -h);
      if (useFictitousJacobianForces) {
         myB.scaledAdd (h, myC);
      }

      //MatrixNd S = new MatrixNd(3, 3);
      //mySolveMatrix.getSubMatrix (248*3, 248*3, S);
      //System.out.println ("SV=\n" + S.toString("%g"));

      // b += Jv v
      mySolveMatrix.mulAdd (myB, myU, vsize, vsize);

      myC.setZero ();
      mySys.addPosJacobian (mySolveMatrix, myC, -h * h);
      if (useFictitousJacobianForces) {
         myB.scaledAdd (h, myC);
      }

      //mySolveMatrix.getSubMatrix (0, 0, S);
      //System.out.println ("SP=\n" + S.toString("%g"));

      addActiveMassMatrix (mySys, mySolveMatrix);

      //mySolveMatrix.writeToFileCRS ("solveMat_foo.txt", "%g");

      if (mySolveMatrixVersion != myRegSolveMatrixVersion) {
         analyze = true;
      }
      if (analyze) {
         myRegSolveMatrixVersion = mySolveMatrixVersion;
         int matrixType = mySys.getSolveMatrixType();
         if (vsize != 0) {
            if (myUseDirectSolver) {
               myDirectSolver.analyze (
                  mySolveMatrix, vsize, matrixType);
            }
            else {
               if (!myIterativeSolver.isCompatible (matrixType)) {
                  throw new UnsupportedOperationException (
                     "Matrix cannot be solved by the chosen iterative solver");
               }
            }
         }
      }
      if (vsize != 0) {
         if (myUseDirectSolver) {
            doDirectSolve (myU, mySolveMatrix, myB);
         }
         else {
            myIterativeSolver.solve (myU, mySolveMatrix, myB);
         }
      }

      mySys.setActiveVelState (myU); 
      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);

      applyVelCorrection (myU, t0, t1);
      mySys.getActivePosState (myQ);

      mySys.addActivePosImpulse (myQ, h, myU);
      mySys.setActivePosState (myQ);

      applyPosCorrection (
         myQ, myUtmp, t1, stepAdjust);
   }

   private static PrintWriter myLogWriter;

   static FunctionTimer timerX = new FunctionTimer();
   static boolean solveModePrinted = false;

   protected void updateSolveMatrixStructure () {
      // assumes that updateStateSizes() has been called
      if (mySolveMatrix == null || mySys.getStructureVersion() != mySolveMatrixVersion) {
         mySolveMatrixVersion = mySys.getStructureVersion();
         mySolveMatrix = new SparseNumberedBlockMatrix();
         mySys.buildSolveMatrix (mySolveMatrix);
      }
   }

   void printArray (String msg, int[] array) {
      System.out.print (msg);
      for (int i=0; i<array.length; i++) {
         System.out.print (" " + array[i]);
      }
      System.out.println ("");      
   }
   
   private int getGTVersion() {
      // compute GT version on demand
      if (!myGTVersionValid) {
         // compute GT version from GT matrix signature
         SparseBlockSignature sig = myGT.getSignature();
         if (myGTSignature == null) {
            myGTVersion++;
         }
         else {
            boolean structureChanged = !myGTSignature.equals (sig);
            if (structureChanged) {
               myGTVersion++;
            }
         }
         myGTSignature = sig;
         myGTVersionValid = true;
      }
      return myGTVersion;
   }
   
   public void updateConstraintMatrices(double h, boolean includeFriction) {
      updateBilateralConstraintMatrix();
      updateUnilateralConstraintMatrix();
      if (includeFriction) {
         updateFrictionConstraints (h, /*prune=*/false);
      }
   }

   /**
    * Updates the bilateral constraint matrix, whose transpose is given by GT
    */
   protected void updateBilateralConstraintMatrix () {
      // assumes that updateStateSizes() has been called
      if (myGTSystemVersion != mySys.getStructureVersion()) {
         myGTSystemVersion = mySys.getStructureVersion();
      }
      myGT = new SparseBlockMatrix ();
      mySys.getBilateralConstraints (myGT, myGdot);
      // need to check  to see if structure of GT has changed
      myGTVersionValid = false;
      myGsize = myGT.colSize();
      ensureGInfoCapacity (myGsize);
      myRg.setSize (myGsize);
      myBg.setSize (myGsize);
      myLam.setSize (myGsize);
   }

   /**
    * Updates the unilateral constraint matrix, whose transpose is given by NT
    */
   protected void updateUnilateralConstraintMatrix () {
      // assumes that updateStateSizes() has been called
      if (myNTSystemVersion != mySys.getStructureVersion()) {
         myNTSystemVersion = mySys.getStructureVersion();
      }
      myNT = new SparseBlockMatrix ();
      mySys.getUnilateralConstraints (myNT, myNdot);
      myNsize = myNT.colSize();
      ensureNInfoCapacity (myNsize);
      myRn.setSize (myNsize);
      myBn.setSize (myNsize);
      myThe.setSize (myNsize);
   }

   protected void updateUnilateralConstraintsNorm () {
      // assumes that updateStateSizes() has been called
      myNT = new SparseBlockMatrix ();
      mySys.getUnilateralConstraints (myNT, myNdot);
      myNsize = myNT.colSize();
      ensureNInfoCapacity (myNsize);
      myRn.setSize (myNsize);
      myBn.setSize (myNsize);
      myThe.setSize (myNsize);
   }

   /**
    * Updates the friction constraint matrix, whose transpose is given by
    * DT, along with the associated friction constraint information.
    */
   protected boolean updateFrictionConstraints (double h, boolean prune) {
      // assumes that updateStateSizes() has been called
      myDT = new SparseBlockMatrix ();

      int fmax = mySys.maxFrictionConstraintSets();
      ensureFrictionCapacity (myFrictionInfo, fmax);
      int numf = mySys.getFrictionConstraints (myDT, myFrictionInfo, prune);
      myFullDSize = 0;
      for (int k=0; k<numf; k++) {
         myFullDSize += myFrictionInfo.get(k).blockSize;
      }
      myNumFrictionEntries = numf;
      int sizeD = myDT.colSize();
      myDSize = sizeD;
      
      myRd.setSize (sizeD);
      myBd.setSize (sizeD);
      myPhi.setSize (sizeD);
      
      if (sizeD > 0) {
         myBd.setZero();
         myRd.setZero();
      }

      int k = 0;
      for (int i=0; i<numf; i++) {
         FrictionInfo info = myFrictionInfo.get(i);
         if (info.blockIdx != -1) {
            double rd = 0;
            if (info.stictionCompliance > 0) {
               rd = info.stictionCompliance/(h*h);
               myRd.set (k, rd); 
               myBd.set (k++, info.stictionDisp0/h);
               if (info.blockSize == 2) {
                  myRd.set (k, rd);                
                  myBd.set (k++, info.stictionDisp1/h);
               }
               System.out.printf (
                  " disp0=%g disp1=%g\n", h*myBd.get(k-2), h*myBd.get(k-1));
                  
            }
            else if (info.stictionCreep > 0) {
               rd = info.stictionCreep/h;
               myRd.set (k++, rd);               
               if (info.blockSize == 2) {
                  myRd.set (k++, rd);                                 
               }
            }
         }
      }
      // compute friction offsets
      // XXX think we need to add Rd to this, but ignore since Rd assumed small
      if (sizeD > 0) {
         boolean vectorSet = false;
         if (myParametricVelSize > 0) {
            myDT.mulTransposeAdd (
               myBd, myUpar, 0, sizeD, myActiveVelSize, myParametricVelSize);
            vectorSet = true;
         }
         myBd.negate();
      }
      return alwaysProjectFriction ? true : sizeD > 0;
   }

   boolean warnIssued = false;

   private void warnFriction (String msg) {
      if (!warnIssued) {
         System.out.println ("CHECK FRICTION: " + msg);
         warnIssued = true;
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
    * 
    * @param vel returns the computed velocity
    * @param fpar if useFictitousJacobianForces is true, returns fictitious 
    * Jacobian forces for parametric components
    * @param bf right side offset
    * @param btmp temporary vector
    * @param vel0 right side velocity
    * @param h interval time step
    * @param flags NO_SYS_UPDATE
    */
   public void KKTFactorAndSolve (
      VectorNd vel, VectorNd fpar, VectorNd bf, VectorNd btmp, VectorNd vel0, 
      double h, int flags) {

      KKTFactorAndSolve (
         vel, fpar, bf, btmp, vel0, h, -h, -h*h, -h, 0, flags);
   }

   // Setting crsFileName will cause the KKT system matrix and RHS
   // to be logged in CCS format.
   private String crsFileName = null; //"testData.txt";
   private PrintWriter crsWriter = null;
   private boolean crsOmitDiag = false;

   private void setBilateralOffsets (double h, double dotscale) {
      if (myGsize > 0) {
         mySys.getBilateralInfo (myGInfo);
         double[] gdot = myGdot.getBuffer();
         double[] Rbuf = myRg.getBuffer();
         double[] gbuf = myBg.getBuffer();
         for (int i=0; i<myGsize; i++) {
            ConstraintInfo gi = myGInfo[i];
            if (myComplianceSupported && gi.compliance > 0) {
               if (gi.force != 0) {
                  double alpha = 1/(0.5*h/gi.compliance + gi.damping);
                  Rbuf[i] = alpha/h;
                  gbuf[i] -= alpha*gi.force;
               }
               else {
                  double s = 1/(0.5*h+gi.damping*gi.compliance);
                  Rbuf[i] = s*gi.compliance/h;
                  gbuf[i] -= s*gi.dist;
               }
            }
            else {
               Rbuf[i] = 0;
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
            if (myComplianceSupported && ni.compliance > 0) {
               if (ni.force != 0) {
                  double alpha = 1/(0.5*h/ni.compliance + ni.damping);
                  Rbuf[i] = alpha/h;
                  nbuf[i] -= alpha*ni.force;
               }
               else {
                  double s = 1/(0.5*h+ni.damping*ni.compliance);
                  Rbuf[i] = s*ni.compliance/h;
                  nbuf[i] -= s*ni.dist;
               }
            }
            else {
               Rbuf[i] = 0;
            }
            nbuf[i] -= dotscale*ndot[i];
         }
      }
   }

   void printEigenValues (Matrix S, int size, String msg, String fmt) {
      MatrixNd SD = new MatrixNd (size, size);
      S.getSubMatrix (0, 0, SD);
      EigenDecomposition ED =
         new EigenDecomposition (
            SD, EigenDecomposition.OMIT_V | EigenDecomposition.SYMMETRIC);
      VectorNd eig = new VectorNd (size);
      ED.get (eig, null, null);
      System.out.println (msg + eig.toString (fmt));
   }

   void checkMurtySolverStatus (LCPSolver.Status status, String stageStr) {
      int nfail = myMurtySolver.numFailedPivots();
      String solveStr = "implicit friction solve ("+stageStr+" stage)";
      if (status != LCPSolver.Status.SOLVED) {
         System.out.println (
            "WARNING: "+solveStr+" failed: " + status + ".");
      }
      else if (nfail > 0) {
         System.out.println (
            "WARNING: "+nfail+" failed pivots in "+solveStr + ".");
      }
      if (status != LCPSolver.Status.SOLVED || nfail > 0) {
         System.out.println (
            "Try using or increasing contact/constraint compliance.");            
      }
   }

   void showContactSolverTiming() {
      if ((myKKTCnt%50) == 0) {
         MurtyMechSolver cs = myMurtySolver;
         double analyzeFactorTime =
            (cs.getAvgAnalyzeTime()*cs.getTotalAnalyzeCount() +
             cs.getAvgFactorTime()*cs.getTotalFactorCount())/myKKTCnt;
         System.out.printf (
            "Contact solver: analyze/factor=%g solve=%g "+
            "nsolves=%g analyzeCnt=%d\n",
            analyzeFactorTime,
            cs.getAvgSolveTime(),
            cs.getTotalSolveCount()/(double)myKKTCnt,
            cs.getTotalAnalyzeCount());
         System.out.println (
            "Total solve time (usec): " +
            myMurtySolverTimer.getTimeUsec()/myKKTCnt);
         
      }
   }

   FunctionTimer myMurtySolverTimer = new FunctionTimer();

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
    * 
    * @param vel returns the computed velocity
    * @param fpar if useFictitousJacobianForces is true, returns fictitious 
    * Jacobian forces for parametric components
    * @param bf right side offset
    * @param btmp temporary vector
    * @param vel0 right side velocity
    * @param h interval time step - used to scale constraint offsets and 
    * impulses
    * @param a0 left side df/dv coefficient
    * @param a1 left side df/dx coefficient
    * @param a2 right side df/dv coefficient
    * @param a3 right side df/dx coefficient
    * @param flags NO_SYS_UPDATE, TRAPEZOIDAL
    */
   public void KKTFactorAndSolve (
      VectorNd vel, VectorNd fpar, VectorNd bf, VectorNd btmp, VectorNd vel0,
      double h, double a0, double a1, double a2, double a3, 
      int flags) {

      myKKTCnt++;
      boolean updateSystem = ((flags & NO_SYS_UPDATE) == 0);
      boolean trapezoidal = ((flags & TRAPEZOIDAL) != 0);
      
      if (profileKKTSolveTime) {
         myKKTTimer.start();
      }
      //FunctionTimer timer = new FunctionTimer();
      // assumes that updateMassMatrix() has been called
      updateStateSizes();

      int velSize = myActiveVelSize;
      boolean analyze = (myAlwaysAnalyze);
      int contactSolverFlags = 0; // MurtySparseContactSolver.NT_INACTIVE;
      if (analyze) {
         contactSolverFlags = MurtyMechSolver.REBUILD_A;
      }
      if (myMurtyVelSolveRebuild) {
         contactSolverFlags = MurtyMechSolver.REBUILD_A;
         myMurtyVelSolveRebuild = false;
      }
      if (myForceAnalyzeInMurtySolver) {
         contactSolverFlags |= (MurtyMechSolver.REBUILD_A);
      }
      updateSolveMatrixStructure();
      if (myKKTSolveMatrixVersion != mySolveMatrixVersion) {
         myKKTSolveMatrixVersion = mySolveMatrixVersion;
         analyze = true;
      }

      SparseNumberedBlockMatrix S = mySolveMatrix;      

      S.setZero();

      if (a0 != 0 && a1 != 0) {
         // add implicit integration terms
         myC.setSize (S.rowSize());
         myC.setZero();

         mySys.addVelJacobian (S, myC, a0);
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
      }

      addActiveMassMatrix (mySys, S);
      if (velSize > 0 && myParametricVelSize > 0) {
         S.mulTranspose (
            btmp, myUpar, 0, velSize, velSize, myParametricVelSize);
         bf.sub (btmp);
      }

      boolean implicitFriction = usingImplicitFriction();
      if (implicitFriction) {
         initMurtySolverIfNecessary();
      }
      else {
         if (myKKTSolver == null) {
            myKKTSolver = new KKTSolver(myMatrixSolver);
         }
      }
      
      if (profileKKTSolveTime) {
         timerStopStart ("    KKT solve: build matrix", myKKTTimer);
      }     

      //updateBilateralConstraintMatrix ();

      if (myKKTGTVersion != getGTVersion()) {
         analyze = true;
         myKKTGTVersion = getGTVersion();
      }

      if (myGsize > 0) {
         boolean vectorSet = false;
         myBg.setZero();
         if (myParametricVelSize > 0) {
            myGT.mulTransposeAdd (
               myBg, myUpar, 0, myGsize, velSize, myParametricVelSize);
            vectorSet = true;
         }
         if (vectorSet) {
            myBg.negate(); // move to rhs
         }
      }
      // a0 is assumed to be negative, which moves myGdot over to the rhs
      double dotscale = (a0 != 0 ? -a0 : h);
      setBilateralOffsets (h, dotscale);

      //updateUnilateralConstraintMatrix ();

      if (myNsize > 0) {
         boolean vectorSet = false;
         myBn.setZero();
         if (myParametricVelSize > 0) {
            myNT.mulTranspose (
               myBn, myUpar, 0,myNsize, velSize, myParametricVelSize);
            vectorSet = true;
         }
         if (vectorSet) {
            myBn.negate(); // move to rhs
         }
      }
      
      // a0 is assumed to be negative, which moves myNdot over to the rhs
      setUnilateralOffsets (h, dotscale);

      // get these in case we are doing hybrid solves and they are needed to
      // help with a warm start
      mySys.getBilateralForces (myLam);
      mySys.getUnilateralForces (myThe);
      
      // convert forces to impulses:
      myLam.scale (h);
      myThe.scale (h);

      if (implicitFriction) {
         // XXX set friction offsets?
         mySys.getFrictionForces (myPhi);
         myPhi.scale (h);
      }

      if (profileKKTSolveTime) {
         timerStop ("    KKT solve: update constraints", myKKTTimer);
      }     

      if (!solveModePrinted) {
         String msg = (myHybridSolveP ? "hybrid solves" : "direct solves");
         if (mySys.getSolveMatrixType() == Matrix.INDEFINITE) {
            msg += ", unsymmetric matrix";
         }
         else {
            msg += ", symmetric matrix";
         }
         System.out.println (msg);
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
         if (profileKKTSolveTime|profileImplicitFriction) {
            System.out.printf (
               "    KKT solve: M=%d G=%d N=%d\n",
               S.rowSize(), myGT.colSize(), myNT.colSize());
         }
         if (implicitFriction) {
            if (profileKKTSolveTime|profileImplicitFriction) {
               timerStart (myKKTTimer);
            }
            myMurtySolverTimer.restart();
            // get state
            int sizeN = (myNT != null ? myNT.colSize() : 0);
            int sizeD = (myDT != null ? myDT.colSize() : 0);
            VectorNi stateN = new VectorNi(sizeN);
            VectorNi stateD = new VectorNi(sizeD);
            mySys.getUnilateralState (stateN, 0);
            getFrictionState (stateD, 0);
            LCPSolver.Status status = myMurtySolver.solve (
               vel, myLam, myThe, myPhi, 
               S, velSize, bf, myKKTSolveMatrixVersion, myGT, myRg, myBg, 
               myNT, myRn, myBn, stateN, myDT, myRd, myBd, stateD,
               myFrictionInfo, myFrictionIters, contactSolverFlags); 
            checkMurtySolverStatus (status, "velocity");
            // XXX hack for when NT_INACTIVE is set in solve call
            for (int i=0; i<sizeN; i++) {
               if (stateN.get(i) == LCPSolver.W_VAR_UPPER) {
                  stateN.set(i, LCPSolver.W_VAR_LOWER);
               }
            }
            mySys.setUnilateralState (stateN, 0);
            setFrictionState (stateD, 0);                   
            if (profileKKTSolveTime|profileImplicitFriction) {
               timerStop ("    KKT solve: contact solve", myKKTTimer);
            }
            myMurtySolverTimer.stop();
            //showContactSolverTiming();
         }
         else {
            if (analyze) {
               if (profileKKTSolveTime|profileImplicitFriction) {
                  timerStart (myKKTTimer);
               }
               myKKTSolver.analyze (
                  S, velSize, myGT, myRg, mySys.getSolveMatrixType());
               if (profileKKTSolveTime|profileImplicitFriction) {
                  timerStop ("    KKT solve: analyze", myKKTTimer);
               }
            }
            if (myHybridSolveP && !analyze && myNT.colSize() == 0) {
               if (profileKKTSolveTime|profileImplicitFriction) {
                  timerStart (myKKTTimer);
               }
               myKKTSolver.factorAndSolve (
                  S, velSize, myGT, myRg, vel, myLam, bf, myBg, myHybridSolveTol);
               if (profileKKTSolveTime|profileImplicitFriction) {
                  timerStop ("    KKT solve: factorAndSolve(hybrid)", myKKTTimer);
               }
            }
            else {
               if (profileKKTSolveTime|profileImplicitFriction) {
                  timerStart (myKKTTimer);
               }
               myKKTSolver.factor (S, velSize, myGT, myRg, myNT, myRn);
               myKKTSolver.solve (vel, myLam, myThe, bf, myBg, myBn);
               if (profileKKTSolveTime|profileImplicitFriction) {
                  timerStop ("    KKT solve: factor and solve", myKKTTimer);
               }
               // MatrixNd MS = new MatrixNd (velSize, velSize);
               // MatrixNd GT = new MatrixNd (velSize, myGT.colSize());
               // S.getSubMatrix (0, 0, MS);
               // System.out.println ("MS=\n" + MS.toString("%15.8f"));
               // myGT.getSubMatrix (0, 0, GT);
               // System.out.println ("GT=\n" + GT.toString ("%15.8f"));
               // System.out.println ("Rg=\n" + myRg.toString ("%15.8f"));
               // System.out.println ("Bg=\n" + myBg.toString ("%15.8f"));
               // System.out.println ("bf=\n" + bf.toString ("%15.8f"));
               // System.out.println ("vel=\n" + vel.toString ("%15.8f"));
               // System.out.println ("lam=\n" + myLam.toString ("%15.8f"));
            }
         }
         if (profileKKTSolveTime) {
            timerStart (myKKTTimer);
         }
         if (updateSystem) {
            mySys.setBilateralForces (myLam, 1/h);
            mySys.setUnilateralForces (myThe, 1/h);
            if (implicitFriction) {
               setFrictionForces (myPhi, 1/h);
            }
            mySys.setActiveVelState (vel);
         }
         if (computeKKTResidual && !implicitFriction) {
            double res = myKKTSolver.residual (
               S, velSize, myGT, myRg, myNT, myRn, 
               vel, myLam, myThe, bf, myBg, myBn);
            System.out.println (
               "vel residual ("+velSize+","+myGT.colSize()+","+
                  myNT.colSize()+"): " + res);
         }
    
         //System.out.println ("bg=" + myBg);
         if (crsWriter != null && !implicitFriction) {
            String msg = 
               "# KKTsolve M="+velSize+" G="+myGT.colSize()+
               " N="+myNT.colSize()+(analyze ? " ANALYZE" : "");
            System.out.println (msg);
            try {
               crsWriter.println (msg);
               myKKTSolver.printLinearProblem (
                  crsWriter, bf, myBg, "%g", crsOmitDiag);
            }
            catch (Exception e) {
               e.printStackTrace(); 
               crsWriter = null;
               crsFileName = null;
            }
         }
      }

      if (myLogWriter != null) {
         writeSystemToLog (vel, bf, velSize);
      }
      
      if (profileKKTSolveTime) {
         timerStop("    KKT solve: end stuff", myKKTTimer);
      }
   }
   
   protected void maybeAccumulateConstraintForces () {
      if (myUpdateForcesAtStepEnd) {
         int velSize = myActiveVelSize;
         if (myGsize > 0) {
            myGT.mulAdd (myFcon, myLam, velSize, myGsize);
         }
         if (myNsize > 0) {
            myNT.mulAdd (myFcon, myThe, velSize, myNsize);
         }
         if (usingImplicitFriction() && myDSize > 0) {
            myDT.mulAdd (myFcon, myPhi, velSize, myDSize);
         }
      }     
   }
   
   /**
    * Populates bg with the bilateral constraint deviation, delta_g
    * Assumes constaints and active position are updated
    * @param bg
    */
   private void getBilateralDeviation(VectorNd bg) {
      if (myGsize > 0 ) {
         mySys.getBilateralInfo (myGInfo);
         bg.setZero();
         double[] gbuf = bg.getBuffer();
         for (int i=0; i<myGsize; i++) {
            gbuf[i] = -myGInfo[i].dist;
         }
      }
   }

   /**
    * Populates bg with the unilateral constraint deviation, delta_n
    * Assumes constaints and active position are updated
    * @param bg
    */
   private void getUnilateralDeviation(VectorNd bn) {
      if (myNsize > 0 ) {
         mySys.getUnilateralInfo (myNInfo);
         bn.setZero();
         double[] gbuf = bn.getBuffer();
         for (int i=0; i<myNsize; i++) {
            gbuf[i] = -myNInfo[i].dist;
         }
      }
   }
   
   /** 
    * Solves a static KKT system of the form
    * <pre>{@code
    * -df/dx*Delta(x) -G^T*lambda - N^T*theta = f
    * G*Delta(x) + g = 0, N*Delta(x) + n >= 0
    * }</pre>
    *
    * @param u returned displacement Delta(x)
    * @param bf right-hand side net force
    * @param beta scale factor for any additional forces such as fictitious forces
    * @param btmp temporary vector
    */
   public void KKTStaticFactorAndSolve (
      VectorNd u, VectorNd bf, 
      double beta, VectorNd btmp) {

      updateStateSizes();

      int velSize = myActiveVelSize;

      boolean analyze = myAlwaysAnalyze;

      updateSolveMatrixStructure();
      if (myStaticKKTVersion != mySolveMatrixVersion) {
         myStaticKKTVersion = mySolveMatrixVersion;
         analyze = true;
      }

      SparseNumberedBlockMatrix S = mySolveMatrix;      

      S.setZero();
      
      // add tikhonov regularization factor
      if (myStaticTikhonov > 0) {
         // identity
         for (int i=0; i<S.numBlockRows(); ++i) {
            MatrixBlock bi = S.getBlock(i, i);
            for (int j=0; j<bi.rowSize(); ++j) {
               bi.set(j,j, myStaticTikhonov);
            }
         }
      }
      
      myC.setZero();
      mySys.addPosJacobian (S, myC, -1);
      if (useFictitousJacobianForces && beta != 0) {
         bf.scaledAdd (beta, myC);
      }
      
      if (myStaticSolver == null) {
         myStaticSolver = new KKTSolver(myMatrixSolver);
      }

      updateConstraintMatrices (0, false);

      if (myKKTGTVersion != getGTVersion()) {
         analyze = true;
         myKKTGTVersion = getGTVersion();
      }
      // bilateral offsets
      // setBilateralOffsets (h, -a0); // -a0);
      // myVel.setSize (velSize);
      getBilateralDeviation(myBg);
      myRg.setZero();

      getUnilateralDeviation (myBn);
      
      // get these in case we are doing hybrid solves and they are needed to
      // help with a warm start
      mySys.getBilateralForces (myLam);
      mySys.getUnilateralForces (myThe);

      if (!solveModePrinted) {
         String msg = (myHybridSolveP ? "hybrid solves" : "direct solves");
         if (mySys.getSolveMatrixType() == Matrix.INDEFINITE) {
            msg += ", unsymmetric matrix";
         }
         else {
            msg += ", symmetric matrix";
         }
         System.out.println (msg);
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
         u.setZero();
         if (analyze) {
            myStaticSolver.analyze (
               S, velSize, myGT, myRg, mySys.getSolveMatrixType());
         }
         if (myHybridSolveP && !analyze && myNT.colSize() == 0) {
            myStaticSolver.factorAndSolve (
               S, velSize, myGT, myRg, u, myLam, bf, myBg, myHybridSolveTol);
         }
         else {
            myStaticSolver.factor (S, velSize, myGT, myRg, myNT, myRn);
            // int nperturbed = myStaticSolver.getNumNonZerosInFactors();
            myStaticSolver.solve (u, myLam, myThe, bf, myBg, myBn);
         }
         if (computeKKTResidual) {
            double res = myStaticSolver.residual (
               S, velSize, myGT, myRg, myNT, myRn, 
               u, myLam, myThe, bf, myBg, myBn);
            System.out.println (
               "vel residual ("+velSize+","+myGT.colSize()+","+
                  myNT.colSize()+"): " + res);
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
               myStaticSolver.printLinearProblem (
                  crsWriter, bf, myBg, "%g", crsOmitDiag);
            }
            catch (Exception e) {
               e.printStackTrace(); 
               crsWriter = null;
               crsFileName = null;
            }
         }
      }

      if (myLogWriter != null) {
         writeSystemToLog (u, bf, velSize);
      }

      mySys.setBilateralForces (myLam, 1);
      mySys.setUnilateralForces (myThe, 1);
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

   private void writeSystemToLog (VectorNd vel, VectorNd bf, int vsize) {

      try {
         NumberFormat fmt = new NumberFormat("%g");
         myLogWriter.println ("M("+vsize+"x"+vsize+")=[");
         mySolveMatrix.write (
            myLogWriter, fmt, Matrix.WriteFormat.SYMMETRIC_CRS, vsize, vsize);
         myLogWriter.println ("];");
         myLogWriter.println ("GT("+vsize+"x"+myGT.colSize()+")=[");
         myGT.write (myLogWriter, fmt, Matrix.WriteFormat.CRS,
                     vsize, myGT.colSize()); 
         myLogWriter.println ("];");
         myLogWriter.println ("NT("+vsize+"x"+myNT.colSize()+")=[");
         myNT.write (myLogWriter, fmt, Matrix.WriteFormat.CRS,
                     vsize, myNT.colSize());
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

   /** 
    * Solves the KKT system given a new right hand side <code>bf</code>. It is
    * assumed that KKTFactorAndSolve() has already been called once, and that
    * the initial velocity <code>vel0</code> is unchanged from that
    * call. The updated value for <code>bf</code> should be based
    * on the value returned by KKTFactorAndSolve(), as described below.
    *
    * <p>Recall that on input to KKTFactorAndSolve(), <code>bf</code> is
    * assumed to be of the form
    * <pre>
    * bf = M vel0 + h f1
    * </pre>
    * where <code>M</code> is the mass matrix, <code>h</code> is the time step,
    * and <code>f1</code> are the force values. On return, <code>bf</code> is
    * modified by adding values that depend on <code>vel0</code> and the
    * position and velocity Jacobians of the system. These additional values
    * are needed on subsequent calls to KKTSolve(). Therefore, when using
    * KKTSolve() to compute velocities for different force values
    * <code>f2</code>, one should use the <code>bf</code> returned by
    * KKTFactorAndSolve() and simply adjust it to reflect <code>f2</code>
    * instead of <code>f1</code>:
    * <pre>
    * bf1 = M vel0 + h f1
    * KKTFactorAndSolve (vel1, bf1, btmp, vel0, ...)
    * bf2 = bf1 - h f1 + h f2
    * KKTSolve (vel2, myLam, the, bf2)
    * </pre>
    */
   public void KKTSolve (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd bf) {

      int velSize = mySys.getActiveVelStateSize();
      if (velSize != vel.size()) {
         throw new IllegalStateException (
            "Velocity size != current active velocity state size");
      }
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
      if (usingImplicitFriction()) {
         if (myMurtySolver == null || !myMurtySolver.isAMatrixFactored()) {
            throw new IllegalStateException (
               "KKTFactorAndSolve must be called prior to KKTSolve");
         }
      }
      else {
         if (myKKTSolver == null || !myKKTSolver.isFactored()) {
            throw new IllegalStateException (
               "KKTFactorAndSolve must be called prior to KKTSolve");
         }
      }

      lam.setSize (myGT.colSize());
      if (myNT != null) {
         the.setSize (myNT.colSize());
      }
      else {
         the.setSize (0);
      }
      if (velSize != 0) {
         if (usingImplicitFriction()) {
            myMurtySolver.resolveMG (vel, lam, bf, myBg);
         }
         else {
            myKKTSolver.solve (vel, lam, the, bf, myBg, myBn);
         }
      }
   }

   protected boolean updateAndProjectFrictionConstraints (
      VectorNd vel, double t0, double h) {
      // BEGIN project friction constraints
      if (updateFrictionConstraints(h, /*prune=*/true)) {
         projectFrictionConstraints (vel, t0, h);
         return true;
      }
      else {
         return false;
      }
   }
   
   void setFrictionForces (VectorNd phi, double s) {
      if (myFullDSize != myDSize) {
         VectorNd fullphi = new VectorNd(myFullDSize);
         int i = 0;
         int j = 0;
         for (int k=0; k<myNumFrictionEntries; k++) {
            int blkSize = myFrictionInfo.get(k).blockSize;
            if (myFrictionInfo.get(k).blockIdx != -1) {
               if (blkSize == 1) {
                  fullphi.set (i++, phi.get(j++));
               }
               else if (blkSize == 2) {
                  fullphi.set (i++, phi.get(j++));
                  fullphi.set (i++, phi.get(j++));               
               }
            }
            else {
               i += blkSize;
            }
         }
         mySys.setFrictionForces (fullphi, s);
      }
      else {
         mySys.setFrictionForces (phi, s);
      }
   }

   void getFrictionForces (VectorNd phi) {
      if (myFullDSize != myDSize) {
         VectorNd fullphi = new VectorNd(myFullDSize);
         mySys.getFrictionForces (fullphi);
         int i = 0;
         int j = 0;
         for (int k=0; k<myNumFrictionEntries; k++) {
            int blkSize = myFrictionInfo.get(k).blockSize;
            if (myFrictionInfo.get(k).blockIdx != -1) {
               if (blkSize == 1) {
                  phi.set (j++, fullphi.get(i++));
               }
               else if (blkSize == 2) {
                  phi.set (j++, fullphi.get(i++));
                  phi.set (j++, fullphi.get(i++));               
               }
            }
            else {
               i += blkSize;
            }
         }
      }
      else {
         mySys.getFrictionForces (phi);
      }
   }

   void setFrictionState (VectorNi state, int startIdx) {
      if (myFullDSize != myDSize) {
         VectorNi fullstate = new VectorNi(myFullDSize);
         int i = 0;
         int j = startIdx;
         for (int k=0; k<myNumFrictionEntries; k++) {
            int blkSize = myFrictionInfo.get(k).blockSize;
            if (myFrictionInfo.get(k).blockIdx != -1) {
               if (blkSize == 1) {
                  fullstate.set (i++, state.get(j++));
               }
               else if (blkSize == 2) {
                  fullstate.set (i++, state.get(j++));
                  fullstate.set (i++, state.get(j++));               
               }
            }
            else {
               i += blkSize;
            }
         }
         mySys.setFrictionState (fullstate, 0);
      }
      else {
         mySys.setFrictionState (state, startIdx);
      }
   }

   void getFrictionState (VectorNi state, int startIdx) {
      if (myFullDSize != myDSize) {
         VectorNi fullstate = new VectorNi(myFullDSize);
         mySys.getFrictionState (fullstate, 0);
         int i = 0;
         int j = startIdx;
         for (int k=0; k<myNumFrictionEntries; k++) {
            int blkSize = myFrictionInfo.get(k).blockSize;
            if (myFrictionInfo.get(k).blockIdx != -1) {
               if (blkSize == 1) {
                  state.set (j++, fullstate.get(i++));
               }
               else if (blkSize == 2) {
                  state.set (j++, fullstate.get(i++));
                  state.set (j++, fullstate.get(i++));               
               }
            }
            else {
               i += blkSize;
            }
         }
      }
      else {
         mySys.getFrictionState (state, startIdx);
      }
   }

   boolean prevIndicesChanged (VectorNi idxs) {
      for (int i=0; i<idxs.size(); i++) {
         if (idxs.get(i) != i) {
            return true;
         }
      }
      return false;
   }

   boolean prevIndicesEmpty (VectorNi idxs) {
      for (int i=0; i<idxs.size(); i++) {
         if (idxs.get(i) != -1) {
            return false;
         }
      }
      return true;
   }

   protected void projectFrictionConstraints (
      VectorNd vel, double t0, double h) {
      
      // assumes that updateMassMatrix() has been called
      int version = (myAlwaysAnalyze ? -1 : getGTVersion());
      if (myRBSolver == null) {
         myRBSolver = new RigidBodySolver (mySys);
      }
      int sizeN = (myNT != null ? myNT.colSize() : 0);
      int sizeD = (myDT != null ? myDT.colSize() : 0);
      VectorNi state = new VectorNi(sizeN + sizeD);
      mySys.getUnilateralState (state, 0);
      getFrictionState (state, sizeN);
      myRBSolver.updateStructure (myMass, myGT, version);
      myRBSolver.projectFriction (
         myMass, myGT, myNT, myDT,
         myRg, myBg, myRn, myBn, myRd, myBd, myFrictionInfo,
         state, vel, myLam, myThe, myPhi);

      mySys.setUnilateralState (state, 0);
      setFrictionState (state, sizeN);        

      // do a Gauss-Siedel project on remaining friction constraints:

      int[] RBDTmap = myRBSolver.getDTMap();
      if (RBDTmap != null) {
         int[] DTmap = new int[myDT.numBlockCols()-RBDTmap.length];
         // compute DT map to be the list of all block columns of DT whose
         // constraints were *not* solved for by the rigid body solver.
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
         // apply remaining friction constraints in the DT map
         for (i=0; i<DTmap.length; i++) {
            // XXX
            FrictionInfo info = myFrictionInfo.get(DTmap[i]);
            double phiMax;
            boolean bilateral = ((info.flags & FrictionInfo.BILATERAL) != 0);
            if ((info.flags & FrictionInfo.BILATERAL) != 0) {
               phiMax = info.getMaxFriction (myLam);
            }
            else {
               phiMax = info.getMaxFriction (myThe);
            }
            int bj = DTmap[i];
            projectSingleFrictionConstraint (
               myPhi, vel, myDT, myBd, bj, phiMax, bilateral,
                  /*ignore rigid bodies=*/true);
         }
      }
      
      setFrictionForces (myPhi, 1/h);
      if (myUpdateForcesAtStepEnd) {
         int velSize = myActiveVelSize;
         if (myDSize > 0) {
            myDT.mulAdd (myFcon, myPhi, velSize, myDSize);
         }
      }
   }

   private void setVec (VectorNd vec, Matrix6x1 D) {
      vec.set (0, D.m00);
      vec.set (1, D.m10);
      vec.set (2, D.m20);
      vec.set (3, D.m30);
      vec.set (4, D.m40);
      vec.set (5, D.m50);
   }
   
   private void setVec (VectorNd vec, Matrix6x2 D, int col) {
      if (col == 0) {
         vec.set (0, D.m00);
         vec.set (1, D.m10);
         vec.set (2, D.m20);
         vec.set (3, D.m30);
         vec.set (4, D.m40);
         vec.set (5, D.m50);
      }
      else {
         vec.set (0, D.m01);
         vec.set (1, D.m11);
         vec.set (2, D.m21);
         vec.set (3, D.m31);
         vec.set (4, D.m41);
         vec.set (5, D.m51);         
      }
   }
   
   private void projectSingleFrictionConstraint (
      VectorNd phiVec, VectorNd vel, SparseBlockMatrix DT, VectorNd bd, int bj,
      double phiMax, boolean bilateral, boolean ignoreRigidBodies) {

      int nactive = mySys.numActiveComponents();
      int nparam = mySys.numParametricComponents();

      double[] vbuf = vel.getBuffer();
      double[] pbuf = myUpar.getBuffer();

      int ndirs = DT.getBlockColSize(bj);
      int joff = DT.getBlockColOffset(bj);

      double[] vtbuf = new double[ndirs];
      double[] dmd = new double[ndirs];
      double[] phi = new double[ndirs];
      
      // allocate these in case we need them below
      VectorNd d6 = new VectorNd(6);
      VectorNd d6prod = new VectorNd(6);
      
      for (MatrixBlock blk=DT.firstBlockInCol(bj); blk != null; blk=blk.down()) {
         int bi = blk.getBlockRow();
         if (bi < nactive) {
            blk.mulTransposeAdd (vtbuf, 0, vbuf, DT.getBlockRowOffset(bi));
            if (blk instanceof Matrix3x1) {
               Matrix3dBase Minv = (Matrix3dBase)myInverseMass.getBlock(bi,bi);
               // assume inverse mass matrix in uniformly diadgonal
               double minv = Minv.m00;
               Matrix3x1 D = (Matrix3x1)blk;
               dmd[0] += (minv*D.m00*D.m00 + minv*D.m10*D.m10 + minv*D.m20*D.m20);
            }
            else if (blk instanceof Matrix3x2) {
               Matrix3dBase Minv = (Matrix3dBase)myInverseMass.getBlock(bi,bi);
               // assume inverse mass matrix in uniformly diadgonal
               double minv = Minv.m00;
               Matrix3x2 D = (Matrix3x2)blk;
               dmd[0] += minv*(D.m00*D.m00 + D.m10*D.m10 + D.m20*D.m20);
               dmd[1] += minv*(D.m01*D.m01 + D.m11*D.m11 + D.m21*D.m21);
            }
            else if (bilateral && blk instanceof Matrix6x1) {
               Matrix6dBase Minv = (Matrix6dBase)myInverseMass.getBlock(bi,bi);
               Matrix6x1 D = (Matrix6x1)blk;
               setVec (d6, D);
               Minv.mul (d6prod, d6);
               dmd[0] = d6prod.dot (d6);
            }
            else if (bilateral && blk instanceof Matrix6x2) {
               Matrix6dBase Minv = (Matrix6dBase)myInverseMass.getBlock(bi,bi);
               Matrix6x2 D = (Matrix6x2)blk;
               setVec (d6, D, 0);
               Minv.mul (d6prod, d6);
               dmd[0] = d6prod.dot (d6);  
               setVec (d6, D, 1);
               Minv.mul (d6prod, d6);
               dmd[1] = d6prod.dot (d6);                
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
      for (int k=0; k<ndirs; k++) {
         double vt = vtbuf[k] - bd.get(joff+k);
         // prevent division by zero
         if (dmd[k] == 0) {
            dmd[k] = 1e-16;
         }
         if (vt > dmd[k]*phiMax) {
            phi[k] = -phiMax;
         }
         else if (vt < -dmd[k]*phiMax) {
            phi[k] = phiMax;
         }
         else {
            phi[k] = -vt/dmd[k];
         }
         phiVec.set (joff+k, phi[k]);
      }
      for (MatrixBlock blk=DT.firstBlockInCol(bj); blk != null; blk=blk.down()) {
         int bi = blk.getBlockRow();
         if (bi >= nactive) {
            break;
         }
         int ioff = DT.getBlockRowOffset(bi);
         if (blk instanceof Matrix3x1) {
            Matrix3dBase Minv = (Matrix3dBase)myInverseMass.getBlock(bi,bi);
            // assume inverse mass matrix in uniformly diadgonal
            double minv = Minv.m00;
            Matrix3x1 D = (Matrix3x1)blk;
            vbuf[ioff  ] += minv*(D.m00*phi[0]);
            vbuf[ioff+1] += minv*(D.m10*phi[0]);
            vbuf[ioff+2] += minv*(D.m20*phi[0]);
         }
         else if (blk instanceof Matrix3x2) {
            Matrix3dBase Minv = (Matrix3dBase)myInverseMass.getBlock(bi,bi);
            // assume inverse mass matrix in uniformly diadgonal
            double minv = Minv.m00;
            Matrix3x2 D = (Matrix3x2)blk;
            vbuf[ioff  ] += minv*(D.m00*phi[0]+D.m01*phi[1]);
            vbuf[ioff+1] += minv*(D.m10*phi[0]+D.m11*phi[1]);
            vbuf[ioff+2] += minv*(D.m20*phi[0]+D.m21*phi[1]);
         }
         else if (bilateral && blk instanceof Matrix6x1) {
            Matrix6dBase Minv = (Matrix6dBase)myInverseMass.getBlock(bi,bi);
            Matrix6x1 D = (Matrix6x1)blk;
            setVec (d6, D);
            d6.scale (phi[0]);
            Minv.mul (d6prod, d6);
            vbuf[ioff  ] += d6prod.get(0);
            vbuf[ioff+1] += d6prod.get(1);
            vbuf[ioff+2] += d6prod.get(2);
            vbuf[ioff+3] += d6prod.get(3);
            vbuf[ioff+4] += d6prod.get(4);
            vbuf[ioff+5] += d6prod.get(5);
         }
         else if (bilateral && blk instanceof Matrix6x2) {
            Matrix6dBase Minv = (Matrix6dBase)myInverseMass.getBlock(bi,bi);
            Matrix6x2 D = (Matrix6x2)blk;
            setVec (d6, D, 0);
            d6prod.scale (phi[0], d6);
            setVec (d6, D, 1);
            d6prod.scaledAdd (phi[1], d6);
            Minv.mul (d6prod, d6prod);
            vbuf[ioff  ] += d6prod.get(0);
            vbuf[ioff+1] += d6prod.get(1);
            vbuf[ioff+2] += d6prod.get(2);
            vbuf[ioff+3] += d6prod.get(3);
            vbuf[ioff+4] += d6prod.get(4);
            vbuf[ioff+5] += d6prod.get(5);
         }
         else if (!ignoreRigidBodies) {
            // XXX implement
         }
      }
   }

   protected void computeVelCorrections (VectorNd vel, double t0, double t1) {

      double h = t1-t0;
      boolean analyze = myAlwaysAnalyze;

      // assumes that updateMassMatrix() has been called
      int velSize = myActiveVelSize;
      if (velSize == 0) {
         return;
      }            
      if (myConSolver == null) {
         myConSolver = new KKTSolver(myMatrixSolver);
      }
      updateConstraintMatrices (h, false);
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

      if (myConMassVersion != myMassVersion || myConGTVersion != getGTVersion()) {
         analyze = true;
      }
      if (analyze) {
         myConSolver.analyze (myMass, velSize, myGT, myRg, Matrix.SPD);
         myConMassVersion = myMassVersion;
         myConGTVersion = getGTVersion();
      }
      // get these in case (at some future point) they are needed for warm
      // startin the solve
      mySys.getBilateralForces (myLam);
      mySys.getUnilateralForces (myThe);
      // convert forces to impulses:
      myLam.scale (h);
      myThe.scale (h);

      myConSolver.factor (myMass, velSize, myGT, myRg, myNT, myRn);
      myConSolver.solve (vel, myLam, myThe, myBf, myBg, myBn);
      if (computeKKTResidual) {
         double res = myConSolver.residual (
            myMass, velSize, myGT, myRg, myNT, myRn, 
            vel, myLam, myThe, myBf, myBg, myBn);
         System.out.println (
            "vel cor residual ("+velSize+","+myGT.colSize()+","+
               myNT.colSize()+"): " + res);
      }
      //vel.set (myVel);

      mySys.setBilateralForces (myLam, 1/h);
      mySys.setUnilateralForces (myThe, 1/h);

      maybeAccumulateConstraintForces();
   }

   protected void constrainedVelSolve (
      VectorNd vel, VectorNd f, double t0, double t1) {

      // assumes that updateMassMatrix() has been called
      double h = t1-t0;
      boolean analyze = myAlwaysAnalyze;

      int velSize = myActiveVelSize;
      if (velSize == 0) {
         return;
      }            
      if (myConSolver == null) {
         myConSolver = new KKTSolver(myMatrixSolver);
      }
      updateConstraintMatrices (h, false);

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
      }
      else {
         myBn.setZero();
      }
      setUnilateralOffsets (h, 0);

      myBf.setSize (velSize);
      if (myParametricVelSize > 0) {
         myMass.mul (
            myBf, myUpar, 0, velSize, velSize, myParametricVelSize);
         myBf.negate(); // move to rhs
      }
      else {
         myBf.setZero();
      }
      myBf.scaledAdd (h, myMassForces);
      myBf.scaledAdd (h, f);

      myMass.mulAdd (myBf, vel, velSize, velSize);
      if (myConMassVersion != myMassVersion || myConGTVersion != getGTVersion()) {
         analyze = true;
      }
      if (analyze) {
         myConSolver.analyze (myMass, velSize, myGT, myRg, Matrix.SPD);
         myConMassVersion = myMassVersion;
         myConGTVersion = getGTVersion();
      }
      // get these in case (at some future point) they are needed for warm
      // startin the solve
      mySys.getBilateralForces (myLam);
      mySys.getUnilateralForces (myThe);
      // convert forces to impulses
      myLam.scale (h);
      myThe.scale (h);

      myConSolver.factor (myMass, velSize, myGT, myRg, myNT, myRn);
      myConSolver.solve (vel, myLam, myThe, myBf, myBg, myBn);

      if (computeKKTResidual) {
         double res = myConSolver.residual (
            myMass, velSize, myGT, myRg, myNT, myRn, 
            vel, myLam, myThe, myBf, myBg, myBn);
         System.out.println (
            "vel cor residual ("+velSize+","+myGT.colSize()+","+
               myNT.colSize()+"): " + res);
      }
      //vel.set (myVel);
      mySys.setBilateralForces (myLam, 1/h);
      mySys.setUnilateralForces (myThe, 1/h);

      maybeAccumulateConstraintForces();
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
//      if (myConSolver == null) {
//         myConSolver = new KKTSolver(myMatrixSolver);
//      }
      updateConstraintMatrices (0, false);
      myVel.setSize (velSize);
      if (myGsize > 0 || myNsize > 0) {

         //mySys.getBilateralOffsets (myRg, myBg, 0, MechSystem.POSITION_MODE);
         mySys.getBilateralInfo (myGInfo);
         double[] gbuf = myBg.getBuffer();
         for (int i=0; i<myGsize; i++) {
            gbuf[i] = -myGInfo[i].dist;
         }
         // don't zero - compliance might be needed if G has redundancies
         //myRg.setZero();

         //mySys.getUnilateralOffsets (myRn, myBn, 0, MechSystem.POSITION_MODE);
         mySys.getUnilateralInfo (myNInfo);
         double[] nbuf = myBn.getBuffer();
         for (int i=0; i<myNsize; i++) {
            nbuf[i] = -myNInfo[i].dist;
         }
         // don't zero - compliance might be needed if N has redundancies
         //myRn.setZero();

         myVel.setZero();
         myLam.setZero();
         myThe.setZero();

         int version = (myAlwaysAnalyze ? -1 : getGTVersion());
         if (myRBSolver == null) {
            myRBSolver = new RigidBodySolver (mySys);
         }
         myRBSolver.updateStructure (myMass, myGT, version);
         if (myRBSolver.projectPosition (myMass,
            myGT, myNT, myBg, myBn, myVel, myLam, myThe)) {
            mySys.addActivePosImpulse (pos, 1, myVel);            
            return true;
         }
      }
      return false;
   }

   protected void computeMassPosCorrection (VectorNd vel, int velSize, double t) {
      boolean analyze = myAlwaysAnalyze;
      if (myConMassVersion != myMassVersion || myConGTVersion != getGTVersion()) {
         analyze = true;
      }
      if (analyze) {
         myConSolver.analyze (myMass, velSize, myGT, myRg, Matrix.SPD);
         myConMassVersion = myMassVersion;
         myConGTVersion = getGTVersion();
      }
      myConSolver.factor (myMass, velSize, myGT, myRg, myNT, myRn);
      myConSolver.solve (vel, myLam, myThe, myBf, myBg, myBn);
      if (computeKKTResidual) {
         double res = myConSolver.residual (
            myMass, velSize, myGT, myRg, myNT, myRn, 
            vel, myLam, myThe, myBf, myBg, myBn);
         System.out.println (
            "mass pos cor residual ("+velSize+","+myGT.colSize()+","+
               myNT.colSize()+"): " + res);
      }
   }

   /**
    * Used with implicit friction:
    */
   protected void computeImplicitPosCorrection (
      VectorNd vel, int velSize, double t) {
      boolean analyze = myAlwaysAnalyze;
      int solveFlags = (analyze ? MurtyMechSolver.REBUILD_A : 0);
      if (myForceAnalyzeInMurtySolver) {
         solveFlags |= (MurtyMechSolver.REBUILD_A);
      }
      updateSolveMatrixStructure();
      if (myKKTSolveMatrixVersion != mySolveMatrixVersion) {
         myKKTSolveMatrixVersion = mySolveMatrixVersion;
         analyze = true;
      }
      SparseNumberedBlockMatrix S = mySolveMatrix;
      if (t == 0) {
         S.setZero();
         mySys.addVelJacobian (S, null, -1);
         mySys.addPosJacobian (S, null, -1);
         addActiveMassMatrix (mySys, S);
      }
      if (myMurtySolver == null) {
         initMurtySolverIfNecessary();
         analyze = true;
      }
      if (t == 0.01) {
         analyze = true;
      }
      int sizeN = (myNT != null ? myNT.colSize() : 0);
      VectorNi stateN = new VectorNi(sizeN); 
      mySys.getUnilateralState (stateN, 0);
      FunctionTimer timer = new FunctionTimer();
      // System.out.println (
      //    "contact state in:  " + LCPSolver.stateToString(state));
      //timer.start();
      myMurtySolverTimer.restart();
      //myMurtySolver.setDebug (MurtySparseContactSolver.SHOW_PIVOTS);
      //System.out.println ("POS sizeG=" + myGsize + " sizeN=" + sizeN+":");
      LCPSolver.Status status = myMurtySolver.contactSolve (
         vel, myLam, myThe, S, velSize, myBf, myKKTSolveMatrixVersion, 
         myGT, myRg, myBg, myNT, myRn, myBn, stateN, solveFlags);
      // System.out.println ("    "+myMurtySolver.getAConsString());
      // System.out.println ("  stateN=" + stateN);
      // System.out.println ("  the=" + myThe);
      myMurtySolverTimer.stop();
      if (status != LCPSolver.Status.SOLVED) {
         System.out.println ("WARNING: contact solve failed, "+status);
      }
      checkMurtySolverStatus (status, "pos correction");
      //timer.stop();
      //System.out.println ("contact solve: " + timer.result(1));
      mySys.setUnilateralState (stateN, 0);
      // System.out.println (
      //    "contact state out: " + LCPSolver.stateToString(state) + "\n");
      // System.out.println (
      //    "A matrix: " + myMurtySolver.getAConsString());

    }

   protected void computeStiffnessPosCorrection (
      VectorNd vel, int velSize, double t) {
      boolean analyze = myAlwaysAnalyze;
      updateSolveMatrixStructure();
      if (myKKTSolveMatrixVersion != mySolveMatrixVersion) {
         myKKTSolveMatrixVersion = mySolveMatrixVersion;
         analyze = true;
      }
      SparseNumberedBlockMatrix S = mySolveMatrix;
      S.setZero();
      // John Lloyd: removed Apr 2025. Probably not needed since this this is
      // essentially a static correction.
      //mySys.addVelJacobian (S, null, -1);
      mySys.addPosJacobian (S, null, -1);
      addActiveMassMatrix (mySys, S);
      if (myKKTSolver == null) {
         myKKTSolver = new KKTSolver(myMatrixSolver);
         analyze = true;
      }
      if (myKKTGTVersion != getGTVersion()) {
         analyze = true;
         myKKTGTVersion = getGTVersion();
      }
      if (analyze) {
         myKKTSolver.analyze (
            S, velSize, myGT, myRg, mySys.getSolveMatrixType());
      }
      if (myHybridSolveP && !analyze && myNT.colSize() == 0) {
         myKKTSolver.factorAndSolve (
            S, velSize, myGT, myRg, vel, myLam, myBf, myBg, myHybridSolveTol);
      }
      else {
         myKKTSolver.factor (S, velSize, myGT, myRg, myNT, myRn);
         myKKTSolver.solve (vel, myLam, myThe, myBf, myBg, myBn);
      }
      if (computeKKTResidual) {
         double res = myKKTSolver.residual (
            S, velSize, myGT, myRg, myNT, myRn, 
            vel, myLam, myThe, myBf, myBg, myBn);
         System.out.println (
            "stiffness pos cor residual ("+velSize+","+myGT.colSize()+","+
               myNT.colSize()+"): " + res);
      }
   }

   protected void applyVelCorrection (VectorNd vel, double t0, double t1) {
      double h = t1-t0;
      computeVelCorrections (vel, t0, t1);
      mySys.setActiveVelState (vel);

      if (updateAndProjectFrictionConstraints (vel, t0, h)) {
         mySys.setActiveVelState (vel);
      }
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
      VectorNd pos, VectorNd vel, double t, StepAdjustment stepAdjust) {
      
      boolean hasConstraints = mySys.updateConstraints (
         t, stepAdjust, /*flags=*/MechSystem.COMPUTE_CONTACTS);
      if (hasConstraints) {
         updateMassMatrix (-1);
         if (computePosCorrections (pos, vel, t)) {
            mySys.setActivePosState (pos);
         }
      }
   }

   protected boolean computePosCorrections (
      VectorNd pos, VectorNd vel, double t) {

      boolean correctionNeeded = false;
      // assumes that updateMassMatrix() has been called
      int velSize = myActiveVelSize;
      if (velSize == 0) {
         return false;
      }            
      if (myConSolver == null) {
         myConSolver = new KKTSolver(myMatrixSolver);
      }
      updateConstraintMatrices (0, false);

      // myVel.setSize (velSize);
      if (myGsize > 0 || myNsize > 0) {
         boolean allConstraintsCompliant = true;
         mySys.getBilateralInfo (myGInfo);
         double[] Rbuf = myRg.getBuffer();
         double[] gbuf = myBg.getBuffer();
         for (int i=0; i<myGsize; i++) {
            ConstraintInfo gi = myGInfo[i];
            if (!myComplianceSupported || gi.compliance == 0) {
               Rbuf[i] = 0;
               gbuf[i] = -gi.dist;
               allConstraintsCompliant = false;
            }
            else {
               // set right side to zero, since corrections for compliant
               // constraints are handled in the velocity solve. Need
               // compliance term set to handle redundancy
               Rbuf[i] = gi.compliance;
               gbuf[i] = 0;
            }
         }
         //myRg.setZero();

         //mySys.getUnilateralOffsets (myRn, myBn, 0, MechSystem.POSITION_MODE);
         mySys.getUnilateralInfo (myNInfo);
         Rbuf = myRn.getBuffer();
         double[] nbuf = myBn.getBuffer();
         for (int i=0; i<myNsize; i++) {
            ConstraintInfo ni = myNInfo[i];
            if (!myComplianceSupported || ni.compliance == 0) {
               Rbuf[i] = 0;
               nbuf[i] = -ni.dist;
               allConstraintsCompliant = false;
            }
            else {
               Rbuf[i] = ni.compliance;
               nbuf[i] = 0;
            }
         }
         // only need to do the correction if some constraints are non-compliant
         if (!allConstraintsCompliant && 
              myStabilization != PosStabilization.None) {
            correctionNeeded = true;
            //myRg.setZero();
            //myRn.setZero();

            //System.out.println ("bn=" + myBn);
            myBf.setSize (velSize);
            myBf.setZero();
            if (usingImplicitFriction()) {
               computeImplicitPosCorrection (vel, velSize, t);
            }
            else if (myStabilization == PosStabilization.GlobalStiffness &&
                     integratorIsImplicit (myIntegrator)) {
               computeStiffnessPosCorrection (vel, velSize, t);
            }
            else {
               computeMassPosCorrection (vel, velSize, t);
            }
         }
      }
      if (correctionNeeded) {
         mySys.addActivePosImpulse (pos, 1, vel);
      }
      return correctionNeeded;
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

   ModelComponent findComponent () {
      MechSystemBase sys = (MechSystemBase)mySys;
      return sys.findComponent ("models/fem0/nodes/39");
   }

   void printCompVel (String msg) {
      ModelComponent c = findComponent();
      if (c != null) {
         Point n = (Point)c;
         System.out.println (msg + " " + n.getVelocity());
      }
   }

   public void constrainedForwardEuler (
      double t0, double t1, StepAdjustment stepAdjust) {

      double h = t1 - t0;

      updateImplicitSolveStateSizes();

      singleStepAuxComponents (t0, t1);

      // update constraints and forces appropriately for time t1.
      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t1);

      // b = M u
      mySys.getActiveVelState (myU);
      mulActiveInertias (myB, myU);
      // b += h f 
      mySys.getActiveForces (myF);
      myF.add (myMassForces);
      myB.scaledAdd (h, myF, myB);

      updateConstraintMatrices (h, usingImplicitFriction());
      
      int solveFlags = 0;
      // setting a0 ... a3 = 0 in KKTFactorAndSolve disables
      // adding the implicit terms to the solve
      KKTFactorAndSolve (
         myUtmp, myFparC, myB, /*tmp=*/myF, myU, 
         h, 0, 0, 0, 0, solveFlags);

      maybeAccumulateConstraintForces();
      
      if (!usingImplicitFriction() && 
          updateFrictionConstraints (h, /*prune=*/true)) {
         projectFrictionConstraints (myUtmp, t0, h);
         mySys.setActiveVelState (myUtmp);
      }

      // update positions: q += h u
      mySys.getActivePosState (myQ);
      mySys.addActivePosImpulse (myQ, h, myUtmp);
      mySys.setActivePosState (myQ);

      // apply position correction using updated constraints and contact
      applyPosCorrection (myQ, myUtmp, t1, stepAdjust);
   }

   public void constrainedBackwardEuler (
      double t0, double t1, StepAdjustment stepAdjust) {

      double h = t1 - t0;

      updateImplicitSolveStateSizes();

      singleStepAuxComponents (t0, t1);

      FunctionTimer timer = null;
      if (profileConstrainedBE) {
         timer = new FunctionTimer();
         timer.start();
      }

      // update constraints and forces appropriately for time t1.
      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);
      if (profileConstrainedBE) {
         timer.stop();
         System.out.println ("  updateConstraints=" + timer.result(1));
         timer.start();
      }
      mySys.updateForces (t1);
      if (profileConstrainedBE) {
         timer.stop();
         System.out.println ("  updateForces=" + timer.result(1));
         timer.start();
      }

      // b = M u
      mySys.getActiveVelState (myU);
      mulActiveInertias (myB, myU);
      // b += h f 
      mySys.getActiveForces (myF);
      myF.add (myMassForces);
      myB.scaledAdd (h, myF, myB);

      updateConstraintMatrices (h, usingImplicitFriction());
      
      int solveFlags = 0;
      KKTFactorAndSolve (
         myUtmp, myFparC, myB, /*tmp=*/myF, myU, h, solveFlags);
      if (profileConstrainedBE) {
         timer.stop();
         System.out.println ("  KKT solve " + timer.result(1));
         timer.start();
      }
      maybeAccumulateConstraintForces();
      
      // store velocities in system
      //mySys.setActiveVelState (myUtmp);
      if (profileConstrainedBE) {
         timer.stop();
         System.out.println ("  setActiveVel " + timer.result(1));
      }

      // apply friction as a correction to the velocity
      if (profileConstrainedBE) {
         timer.start();
      }
      if (!usingImplicitFriction() && 
          updateFrictionConstraints (h, /*prune=*/true)) {
         projectFrictionConstraints (myUtmp, t0, h);
         mySys.setActiveVelState (myUtmp);
      }
      if (profileConstrainedBE) {
         timer.stop();
         System.out.println ("  friction " + timer.result(1));
      }

      // update positions: q += h u
      mySys.getActivePosState (myQ);
      mySys.addActivePosImpulse (myQ, h, myUtmp);

      if (profileConstrainedBE) {
         timer.start();
      }
      mySys.setActivePosState (myQ);

      if (profileConstrainedBE) {
         timer.stop();
         System.out.println ("  setActivePos " + timer.result(1));
         timer.start();
      }
      // apply position correction using updated constraints and contact
      applyPosCorrection (myQ, myUtmp, t1, stepAdjust);
      if (profileConstrainedBE) {
         timer.stop();
         System.out.println ("  posCorrection=" + timer.result(1));
      }

      //checkStiffnessMatrix();
   }

   private double computeForceResidual (
      double t0, double t1, VectorNd btmp, int velSize) {

      double h = t1 - t0;
      myDelV.setSize (velSize);
      myFx.setSize (velSize);

      myDelV.sub (myU, myUtmp);
      mulActiveInertias (myFx, myDelV);
      myGT.mulAdd (myFx, myLam, velSize, myGT.colSize());
      if (myNT != null) {
         myNT.mulAdd (myFx, myThe, velSize, myNT.colSize());
      }
      if (velSize > 0 && myParametricVelSize > 0) {
         mySolveMatrix.mulTranspose (
            btmp, myUpar, 0, velSize, velSize, myParametricVelSize);
         myFx.sub (btmp);
      }
      myFx.scale (-1/h);

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

   public void fullBackwardEuler (
      double t0, double t1, StepAdjustment stepAdjust) {

      double h = t1 - t0;

      int vsize = updateImplicitSolveStateSizes();
      VectorNd xVec0 = new VectorNd (myQ.size());

      singleStepAuxComponents (t0, t1);

      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t1);

      // b = M v
      mySys.getActiveVelState (myU);
      mulActiveInertias (myB, myU);
      // b += h f
      mySys.getActiveForces (myF);
      myF.add (myMassForces);
      myB.scaledAdd (h, myF, myB);

      updateConstraintMatrices(h, usingImplicitFriction());
      int solveFlags = 0;
      KKTFactorAndSolve (myUtmp, myFparC, myB, /*tmp=*/myF, myU, h, solveFlags);

      mySys.getActivePosState (xVec0);
      myQ.set (xVec0);
      mySys.addActivePosImpulse (myQ, h, myUtmp);
      mySys.setActivePosState (myQ);

      mySys.updateForces (t1);
      double fres = computeForceResidual (t0, t1, /*tmp=*/myF, vsize);

      double FRES_TOL = 1e-8;
      int MAX_ITER = 5;

      int iter = 0;

      //System.out.println ("vel=" + myUtmp.toString ("%g"));
      //System.out.printf ("fres[%d]=%g\n", 0, fres);

      if (fres > FRES_TOL) {
         VectorNd velk = new VectorNd (vsize);
         while (fres > FRES_TOL && iter < MAX_ITER) {
            velk.set (myUtmp);

            // no need to update mass matrix 
            mulActiveInertias (myB, myU);
            mySys.getActiveForces (myF);
            // XXX mass forces need to be updated?
            myF.add (myMassForces);
            myB.scaledAdd (h, myF, myB);

            updateConstraintMatrices(h, usingImplicitFriction());
            KKTFactorAndSolve (
               myUtmp, myFparC, myB, /*tmp=*/myF, velk, h,
               -h, -h*h, -h, -h*h, solveFlags);

            myQ.set (xVec0);
            mySys.addActivePosImpulse (myQ, h, myUtmp);
            mySys.setActivePosState (myQ);

            mySys.updateForces (t1);
            fres = computeForceResidual (t0, t1, /*tmp=*/myF, vsize);

            iter++;
            //System.out.println ("vel=" + myUtmp.toString ("%10.4f"));
            //System.out.printf ("fres[%d]=%g\n", iter, fres);
         }
      }
      maybeAccumulateConstraintForces();
      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);
      if (!usingImplicitFriction() && 
          updateFrictionConstraints (h, /*prune=*/true)) {
         projectFrictionConstraints (myUtmp, t0, h);
         mySys.setActiveVelState (myUtmp);
      }
      // if (updateAndProjectFrictionConstraints (myUtmp, -1, h)) {
      //    mySys.setActiveVelState (myUtmp);
      // }

      applyPosCorrection (
         myQ, myUtmp, t1, stepAdjust);
   }

   public void trapezoidal (double t0, double t1, StepAdjustment stepAdjust) {
 
      double h = t1 - t0;

      updateImplicitSolveStateSizes();

      singleStepAuxComponents (t0, t1);

      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t1);

      // b = M v
      mySys.getActiveVelState (myU);
      mulActiveInertias (myB, myU);
      // b += h f
      mySys.getActiveForces (myF);
      myF.add (myMassForces);
      myB.scaledAdd (h, myF, myB);

      //System.out.println ("  myB: " + myB);
      
      int solveFlags = TRAPEZOIDAL;
      updateConstraintMatrices(h, usingImplicitFriction());
      // Antonio derived an alternate form for trapezoidal integration
      // which set a0 ... a3 as -h, -h*h/w, -h, h*h/2, but it is
      // unclear how this was justified.
      KKTFactorAndSolve (
         myUtmp, myFparC, myB, /*tmp=*/myF, myU, 
         h, -h/2, -h*h/4, -h/2, h*h/4, solveFlags);

      maybeAccumulateConstraintForces();

      if (!usingImplicitFriction() && 
          updateFrictionConstraints(h, /*prune=*/true)) {
         projectFrictionConstraints (myUtmp, t0, h);
         mySys.setActiveVelState (myUtmp);
      }

      mySys.getActivePosState (myQ);
      mySys.addActivePosImpulse (myQ, h/2, myUtmp);
      mySys.addActivePosImpulse (myQ, h/2, myU);
      mySys.setActivePosState (myQ);

      applyPosCorrection (myQ, myUtmp, t1, stepAdjust);
   }
   
   /**
    * Applies Tiknonov regularization for static solves, minimizing
    * 
    * <pre>
    * L(dx) = W(x+dx) + eps*|dx|^2
    * </pre>
    * where W is the energy potential function (including constraints) for the
    * static system.  If eps is less than zero, then we try to estimate the optimal
    * parameter based on the Frobenius norm of the stiffness matrix.
    * 
    * @param eps tikhonov regularization factor
    */
   public void setStaticTikhonovFactor(double eps) {
      myStaticTikhonov = eps;
   }

   public double getStaticTikhonovFactor() {
      return myStaticTikhonov;
   }
   
   /**
    * Sets the number of load increments to use with the {@link Integrator#StaticIncremental}
    * integrator.
    * 
    * @param iters number of load increments
    */
   public void setStaticIncrements(int iters) {
      myStaticIncrements = Math.max(iters, 1);
   }
   
   /**
    * Returns the number of load increments being used with the
    * {@link Integrator#StaticIncremental}
    * integrator.
    * 
    * @return number of load increments for {@link Integrator#StaticIncremental}
    */
   public int getStaticIncrements() {
      return myStaticIncrements;
   }
   
   /**
    * Scales forces and constraints down by alpha, and solves the adjusted problem
    * @param t0 start time for solve
    * @param t1 stop time for solve
    * @param alpha step factor
    * @param stepAdjust step adjustment description
    */
   public void staticIncrementalStep (
      double t0, double t1, double alpha, StepAdjustment stepAdjust) {

      updateStaticSolveStateSizes();
      myFx.setZero();

      singleStepAuxComponents (t0, t1);

      // compute our new constraints and forces at time t1
      myUtmp.setZero();
      mySys.setActiveVelState(myUtmp);
      mySys.setParametricVelState(myFx);
      mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);
      mySys.updateForces (t1);

      // set of forces (mix of internal and external)
      mySys.getActiveForces (myB);
      // myB.scale(alpha);
      
      // solve for direction with full forces
      KKTStaticFactorAndSolve(myUtmp, myB, 1, /*tmp=*/myF);

      // take a fractional step with factor alpha
      mySys.getActivePosState (myQ);
      mySys.addActivePosImpulse (myQ, alpha, myUtmp);
      mySys.setActivePosState (myQ);
      
      // XXX Position correction 
      //     maybe only required if alpha is small and if error is sufficiently large
      //     and constraints active
      mySys.updateConstraints (t1, null, MechSystem.COMPUTE_CONTACTS);
      if (myGsize > 0 || myNsize > 0) {
         myB.setZero();  // zero-out forces
         KKTStaticFactorAndSolve(myUtmp, myB, 0 /*no forces*/, /*tmp=*/myF);
         
         // add final position correction
         mySys.addActivePosImpulse (myQ, 1, myUtmp);
         mySys.setActivePosState (myQ);
      }
      maybeAccumulateConstraintForces();
   }
   
   public void staticIncremental (
      double t0, double t1, int nincrements, StepAdjustment stepAdjust) {

      updateStaticSolveStateSizes();

      singleStepAuxComponents (t0, t1);

      // zero out velocities
      myUtmp.setZero();
      mySys.setActiveVelState(myUtmp);
      myFx.setZero();
      mySys.setParametricVelState(myFx);
   
      // current state
      mySys.getActivePosState (myQ);
      
      // apply load in increments
      for (int i=0; i<nincrements; ++i) {
         double alpha = (i+1)*1.0/nincrements;
      
         // compute our new constraints and forces at time t1
         mySys.updateConstraints (t1, stepAdjust, MechSystem.COMPUTE_CONTACTS);
         mySys.updateForces (t1);
         // set of forces (mix of internal and external)
         mySys.getActiveForces (myB);
         
         // scale down residual force
         myB.scale(alpha);
         KKTStaticFactorAndSolve(myUtmp, myB, alpha, /*tmp=*/myF);  // enforces constraints for reduced load
   
         mySys.addActivePosImpulse (myQ, 1, myUtmp);  // move full amount
         mySys.setActivePosState (myQ);
         
      }
      maybeAccumulateConstraintForces();
   }

   /**
    * Implementation of Brent's root-finding method, guarantees that the final call
    * to func is at the root
    * @param a   left-side of interval
    * @param fa  function evaluated at a
    * @param b   right-side of interval
    * @param fb  function evaluated at b
    * @param eps  tolerance for interval [a,b]
    * @param feps tolerance for function value |f| {@code <} feps considered root
    * @param func function to evaluate
    * @return root
    */
   public static double brentRootFinder(double a, double fa, double b, double fb, 
      double eps, double feps, Function1x1 func) {
      
      if (fa*fb > 0) {
         return a-1; // invalid result
      }
      
      double tmp;
      if (Math.abs(fa) <= Math.abs(fb)) {
         // swap around
         tmp = a;
         a = b;
         b = tmp;
         tmp = fa;
         fa = fb;
         fb = tmp;
      }
      
      boolean flag = true;
      
      double c = a;
      double fc = fa;
      double d = 0;
      
      // current guess
      double s = b;
      double fs = fb;
      
      // check for termination
      if (Math.abs(b-a) <= eps || Math.abs(fs) <= feps) {
         // call function at b
         fs = func.eval(b);
         return b;
      }
      
      do {
         if (fa != fc && fb != fc) {
            // inverse quadratic interpolation
            s = a*fb*fc/(fa-fb)/(fa-fc) + b*fa*fc/(fb-fa)/(fb-fc)+c*fa*fb/(fc-fa)/(fc-fb);
         } else {
            // secant
            s = b-fb*(b-a)/(fb-fa);
         }
         
         double l = (3*a+b)/4;
         double r = b;
         if (    (l < r && (s < l || s > r) )   
              || (l >= r && (s < r || s > l) )               // condition 1, s not between (3a+b)/4 and b
              || (flag && Math.abs(s-b) >= Math.abs(b-c)/2)  // condition 2
              || (!flag && Math.abs(s-b) >= Math.abs(c-d)/2) // condition 3
              || (flag && Math.abs(b-c) < eps)               // condition 4 
              || (!flag && Math.abs(c-d) < eps)) {           // condition 5
            s = (a+b)/2;    // bisection
            flag = true;
         } else {
            flag = false;
         }
         
         fs = func.eval(s);
         d = c;
         c = b;
         
         // next interval
         if (fa*fs < 0) {
            b = s;
            fb = fs;
         } else {
            a = s;
            fa = fs;
         }
         
         // maybe swap a and b
         if (Math.abs(fa) < Math.abs(fb)) {
            tmp = a;
            a = b;
            b = tmp;
            tmp = fa;
            fa = fb;
            fb = tmp;
         }
      } while ( Math.abs(b-a) > eps && Math.abs(fs) > feps);
      return s;
   }
   
   /**
    * Implementation of a modified Golden section search for minimizing |f(s)|, 
    * guaranteeing that the final call to func is at the returned minimum 
    * 
    * @param a   left-side of search interval
    * @param fa  f(a)
    * @param b   right-side of search interval
    * @param fb  f(b)
    * @param eps   tolerance for interval [a,b]
    * @param feps
    * tolerance for function evaluation, {@code |f(s)| <} feps considered a root
    * @param func  function to evaluate
    * @return function minimizer
    */
   public static double modifiedGoldenSection (
      double a, double fa, double b, double fb, 
      double eps, double feps, Function1x1 func) {
      
      // absolute values
      double afa = Math.abs(fa);
      double afb = Math.abs(fb);
      
      // initial solution guess as minimum of end-points
      double s;
      double fs;
      double afs;
      if (afa <= afb) {
         s = a;
         fs = fa;
         afs = afa;
      } else {
         s = b;
         fs = fb;
         afs = afb;
      }
      
      // intermediate values
      double c, d, fc, fd, afc, afd;
      final double g = 1.61803398875;   // golden ratio
      
      boolean callNeeded = true;        // whether we need to make a final call to the function 
      
      while ( b-a > eps && afs > feps) {
         if (fa*fb <= 0) {
            // return brentRootFinder (a, fa, b, fb, eps, feps, func);
            return BrentRootSolver.findRoot (func, a, fa, b, fb, eps, feps);
         }
         
         c = b + (a-b)/g;

         fc = func.eval(c);
         afc = Math.abs(fc);
         
         if ( fc*fa < 0) {
            //return brentRootFinder(a, fa, c, fc, eps, feps, func);
            return BrentRootSolver.findRoot (func, a, fa, c, fc, eps, feps);
         } else if (afc >= afa) {
            if (afa > afb) {
               // start again in range [c,b]
               a = c;
               fa = fc;
               afa = afc;
            } else {
               // start again in range [a,c]
               b = c;
               fb = fc;
               afb = afc;
            }
            callNeeded = true;  // c is not minimum
         } else if (afc >= afb) {
            // start again in range [c, b]
            a = c;
            fa = fc;
            afa = afc;
            callNeeded = true;  // c is not minimum
         } else {
   
            // fc guaranteed to be below both fa & fb
            d = a + (b-a)/g;
            fd = func.eval(d);
            afd = Math.abs(fd);
            
            if ( fc * fd < 0 ) {
               //return brentRootFinder(a, fa, c, fc, eps, feps, func);
               return BrentRootSolver.findRoot (func, a, fa, c, fc, eps, feps);
            } else if ( afc <= afd ) {
               // c below a and d
               b = d; 
               fb = fd;
               afb = afd;
               s = c;
               fs = fc;
               afs = afc;
               callNeeded = true;  // will need to re-call at c
            } else {
               // d below c and b
               a = c;
               fa = fc;
               afa = afc;
               s = d;
               fs = fd;
               afs = afd;
               callNeeded = false;  // d is minimum
            }
         }
      }
      // final function call, if needed
      if (callNeeded) {
         fs = func.eval(s);
      }
      
      return s;
   }
   
   /**
    * Function to evaluate residual energy
    */
   private static class ResidualEnergyFunction implements Function1x1 {
 
      double time;
      double cenergy; // energy from constraints
      MechSystem sys; // mech system
      VectorNd q0;    // starting position
      VectorNd u;     // incremental displacement vector
      VectorNd qtmp;  // temporary storage for position
      VectorNd ftmp;  // temporary storage for forces
      
      public ResidualEnergyFunction(double time, 
         MechSystem sys,
         VectorNd q0, VectorNd u, 
         VectorNd qtmp, VectorNd ftmp) {
         this.time = time;
         this.sys = sys;
         this.q0 = q0;
         this.u = u;
         this.qtmp = qtmp;
         this.ftmp = ftmp;
      }
      
      public void setConstraintEnergy(double e) {
         cenergy = e;
      }
      
      @Override
      public double eval(double alpha) {
         qtmp.set(q0);
         sys.addActivePosImpulse (qtmp, alpha, u);
         sys.setActivePosState(qtmp);
         sys.updateForces(time);
         sys.getActiveForces (ftmp);

         return ftmp.dot(u)+alpha*cenergy;  // residual energy
      }
      
   }
   
   public void staticLineSearch (
      double t0, double t1, StepAdjustment stepAdjust) {

      int vsize = updateStaticSolveStateSizes();
      myQtmp.setSize(myQ.size());

      singleStepAuxComponents (t0, t1);

      double FRES_TOL = 1e-8;
      int MAX_ITER = 100;

      int iter = 0;

      //System.out.println ("vel=" + myUtmp.toString ("%g"));
      //System.out.printf ("fres[%d]=%g\n", 0, fres);

      // zero-out velocity
      myU.setZero();
      mySys.setActiveVelState(myU);
      myFx.setZero();
      mySys.setParametricVelState(myFx);
      
      boolean first = true;
      double fres = FRES_TOL+1;
      double f0 = 1;
      double utol = -1;

      ResidualEnergyFunction Ra = new ResidualEnergyFunction(t1, mySys, myQtmp, myU, myQ, myF);
      
      while (fres > FRES_TOL && iter < MAX_ITER) {

         // compute our new constraints and forces at time t1
         mySys.updateConstraints (t1, null, MechSystem.UPDATE_CONTACTS);
         mySys.updateForces (t1);
   
         // set of forces (mix of internal and external)
         mySys.getActiveForces (myB);
         
         // original RHS force (for checking convergence)
         if (first) {
            f0 = myB.norm();  // original residual force
         }
         
         // solve for direction
         KKTStaticFactorAndSolve(myU, myB, 1, /*tmp=*/myF);
         if (first) {
            // expected norm of converged tolerance
            utol = myStaticTol*Math.sqrt((double)vsize);
         }
            
         // initial state
         mySys.getActivePosState (myQtmp);
         
         // set state at q + Delta u
         myQ.set(myQtmp);
         mySys.addActivePosImpulse(myQ, 1, myU);
         mySys.setActivePosState(myQ);
         
         // check if converged within tolerance
         if (myU.norm() <= utol) {
            // compute contacts
            mySys.updateConstraints (t1, null, MechSystem.COMPUTE_CONTACTS);
            break;
         } 
         
         // energy due to constraints
         double cenergy = 0;            
         myF.setZero();
         if (myGsize > 0) {
            mySys.getBilateralForces(myLam);
            myGT.mulAdd(myF, myLam, vsize, myGsize);
         }
         if (myNsize > 0) {
            mySys.getUnilateralForces(myThe);
            myNT.mulAdd(myF, myThe, vsize, myNsize);
         }
         cenergy = myF.dot(myU);
         Ra.setConstraintEnergy(cenergy);
         
         // update forces at new position
         double R0 = myB.dot(myU);          // energy at alpha=0
         mySys.updateForces (t1);
         mySys.getActiveForces(myF);        // new forces at alpha=1
         double R1 = myF.dot(myU)+cenergy;  // energy at alpha=1
         
         // use modified Golden section search to find optimal alpha
         // guaranteed to call R(alpha) last, which will populate myF and myQ
         double alpha = GoldenSectionSearch.minimize (
            Ra, 0, R0, 1, R1, 1e-5, 0.75*Math.abs(R0));
         
         if (alpha == 0) {
            break;
         }
         if (first) {
            first = false;
         }
         
         // current active forces now in myF, add constraint forces
         if (myGsize > 0) {
            myLam.scale(alpha);
            myGT.mulAdd (myF, myLam, vsize, myGT.colSize());
         }
         if (myNsize > 0) {
            myThe.scale(alpha);
            myNT.mulAdd (myF, myThe, vsize, myNT.colSize());
         }
         
         // final norm of residual forces
         double f1 = myF.norm();
         if (f1 < 1e-16) {
            fres = 0;
         } else {
            fres = f1/f0;
         }
         
         //         System.out.println("alpha=" + alpha);
         //         System.out.println("u=" + myU.toString("%.2f"));
         
         // XXX Position correction 
         //     maybe only apply if alpha is small and if error is sufficiently large
         mySys.updateConstraints (t1, null, MechSystem.COMPUTE_CONTACTS);
         if (myGsize > 0 || myNsize > 0) {
            myB.setZero();  // zero-out forces
            KKTStaticFactorAndSolve(myU, myB, 0 /*no forces*/, /*tmp=*/myF);
            // myU should now move to constraint
            // add final position correction
            mySys.addActivePosImpulse (myQ, 1, myU);
            mySys.setActivePosState (myQ);
         }
         ++iter;
      }
      maybeAccumulateConstraintForces();
      
      // System.out.println("exiting static solve");
   }

   public void checkStiffnessMatrix() {
      SparseBlockMatrix M = createActiveStiffnessMatrix(-1);
      EigenDecomposition eig = new EigenDecomposition (
         M, EigenDecomposition.SYMMETRIC);

      double minEig = eig.getEigReal().minElement();
      double maxEig = eig.getEigReal().maxElement();
      if (minEig < -1e-8*M.frobeniusNorm()) {
         int num = 0;
         for (int i=0; i<M.rowSize(); i++) {
            if (eig.getEigReal().get(i) < -1e-8) {
               num++;
            }
         }
         System.out.printf (
            " stiffnessMatrix not SPD: minEig %g, maxEig=%g\n",
            minEig, maxEig);
      }
      else {
         System.out.println ("S=\n" + (new MatrixNd(M)).toString("%10.5f"));
         System.out.printf (
            " stiffnessMatrix OK\n");
      }
   }

   public SparseBlockMatrix createActiveStiffnessMatrix (double h) {
      updateStateSizes();
      mySys.updateForces (0);
      SparseNumberedBlockMatrix S = new SparseNumberedBlockMatrix();
      mySys.buildSolveMatrix (S);
      mySys.addPosJacobian (S, null, h);
      int nactive = mySys.numActiveComponents();
      return S.createSubMatrix (nactive, nactive);
   }

   public SparseBlockMatrix createActiveDampingMatrix (double h) {
      updateStateSizes();
      mySys.updateForces (0);
      SparseNumberedBlockMatrix S = new SparseNumberedBlockMatrix();
      mySys.buildSolveMatrix (S);
      mySys.addVelJacobian (S, null, h);
      int nactive = mySys.numActiveComponents();
      return S.createSubMatrix (nactive, nactive);
   }

   public SparseBlockMatrix createActiveBilateralMatrix (double t) {
      updateStateSizes();
      updateBilateralConstraintMatrix ();
      int nactive = mySys.numActiveComponents();
      return myGT.createSubMatrix (nactive, myGT.numBlockCols());
   }

   /**
    * Forces the solver to perform an analysis step, during the next
    * solve, for any matrix components involving bilateral constraints
    */
   protected void forceBilateralAnalysis() {
      //myRegSolveMatrixVersion = -1;
      //myKKTSolveMatrixVersion = -1;
      myKKTGTVersion = -1;
      //myConMassVersion = -1;
      myConGTVersion = -1;
      //myStaticKKTVersion = -1;
      if (myRBSolver != null) {
         myRBSolver.resetBilateralVersion();
      }
      myForceAnalyzeInMurtySolver = true;
   }

   /**
    * Reset the versions for the various solve matrices, forcing analysis steps
    * to be reperformed. Used when changing integrators.
    */
   private void resetMatrixVersions() {
      myKKTGTVersion = -1;
      myStaticKKTVersion = -1;
      myConGTVersion = -1;
      myConMassVersion = -1;
      myGTVersion = -1;
      myGTVersionValid = false;
      myGTSignature = null;
      myGTSystemVersion = -1;
      myNTSystemVersion = -1;
      mySolveMatrixVersion = -1;
      myRegSolveMatrixVersion = -1;
      myKKTSolveMatrixVersion = -1;      
      if (myRBSolver != null) {
         myRBSolver.resetBilateralVersion();
      }
   }

   /**
    * Returns the solve matrix. For debugging and testing only.
    */
   public SparseBlockMatrix getSolveMatrix() {
      return mySolveMatrix;
   }
    
   private void disposeSolvers() {
      if (myPardisoSolver != null) {
         myPardisoSolver.dispose();
         myPardisoSolver = null;
      }
      if (myKKTSolver != null) {
         myKKTSolver.dispose();
         myKKTSolver = null;
      }
      if (myMurtySolver != null) {
         myMurtySolver.dispose();
         myMurtySolver = null;
      }
      if (myStaticSolver != null) {
         myStaticSolver.dispose();
         myStaticSolver = null;
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

   public void dispose() {
      disposeSolvers();
   }

   public void finalize() {
      dispose();
   }

   public void initialize() {
      if (myUseImplicitFriction) {
         initMurtySolverIfNecessary();
      }
      if (myMurtySolver != null) {
         myMurtySolver.initialize();
         myMurtySolverTimer.reset();
         myKKTCnt = 0;
      }
      if (myKKTSolver != null) {
         myKKTSolver.initialize();
      }
      if (myConSolver != null) {
         myConSolver.initialize();
      }
      if (myStaticSolver != null) {
         myStaticSolver.initialize();
      }
      forceBilateralAnalysis();
   }
   
   private void initMurtySolverIfNecessary() {
      if (myMurtySolver == null) {
         myMurtySolver = new MurtyMechSolver();
         myMurtySolver.setHybridSolves (myHybridSolveP);
      }
   }
   /*
     How and where constraints are updated in the various integrators:

     Rg, bg, Rn, bn updated in setBilateralOffsets() and
     setUnilateralOffsets(), call from:

     computeVelCorrections()
        applyVelCorrections()
           backwardEuler()
           forwardEuler()
           symplecticEuler()
           rungeKutta4()

     constraintedVelSolve()
        symplecticEulerX()

     KKTFactorAndSolve()
        fullBackwardEuler()
        trapezoidal()
        constrainedBackwardEuler()

     Rg is zeroed in KKTStaticFactorAndSolve()

     myBg and myBn are also set in 

     computeRigidBodyPosCorrections()
        projectRigidBodyPosConstraints()
           MechModel.projectRigidPositionConstraints()

     computePosCorrections()
        applyPosCorrections()
           backwardEuler()
           forwardEuler()
           symplecticEuler()
           rungeKutta4()
           symplecticEulerX()
           fullBackwardEuler()
           trapezoidal()
           constrainedBackwardEuler()
           
        projectPosConstraints()
           MechModel.preadvance()

     GT and NT updated in updateBilateral/UnilateralConstraints, called from:

     computePosCorrections()
     computeRigidBodyPosCorrections()
     computeVelCorrections()
     constrainedVelSolve()
     KKTFactorAndSolve()
     KKTStaticFactorAndSolve()

    */ 
}
