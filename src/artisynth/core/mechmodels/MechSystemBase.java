/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.*;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Matrix;
import maspack.matrix.MatrixNd;
import maspack.matrix.MatrixNdBlock;
import maspack.matrix.MatrixBlock;
import maspack.matrix.NumericalException;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderableUtils;
import maspack.solvers.SparseSolverId;
import maspack.util.DataBuffer;
import maspack.util.IntHolder;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.FunctionTimer;
import maspack.util.Range;
import maspack.util.EnumRange;
import artisynth.core.mechmodels.MechSystemSolver.PosStabilization;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.*;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.TimeBase;

public abstract class MechSystemBase extends RenderableModelBase
   implements MechSystemModel {

   public static boolean mySaveForcesAsState = true;
   public static boolean myParametricsInSystemMatrix = true;
   //public static boolean myZeroForcesInPreadvance = true;

   protected int myStructureVersion = 0;
   // flag indicating that the state resulting from the current 
   // advance will be saved
   protected boolean myStateWillBeSaved = false;

   public static boolean useAllDynamicComps = true;
   protected ArrayList<DynamicComponent> myAllDynamicComponents;
   protected ArrayList<DynamicComponent> myDynamicComponents;
   protected ArrayList<MotionTargetComponent> myParametricComponents;
   protected ArrayList<DynamicAttachment> myAttachments;
   protected ArrayList<DynamicAttachment> myActiveAttachments;
   protected ArrayList<DynamicAttachment> myParametricAttachments;
   protected ArrayList<Constrainer> myConstrainers;
   protected ArrayList<ForceEffector> myForceEffectors;
   protected ArrayList<HasNumericState> myAuxStateComponents;
   protected ArrayList<HasSlaveObjects> mySlaveObjectComponents;

   protected VectorNd myInitialForces = new VectorNd();

   int[] myDynamicSizes;
   int mySystemSize;
   protected int myNumComponents;
   protected int myNumActive;
   protected int myNumAttached;
   protected int myNumParametric;

   protected int myActiveVelStateSize;
   protected int myActivePosStateSize;
   protected int myAttachedVelStateSize;
   protected int myAttachedPosStateSize;
   protected int myParametricVelStateSize;
   protected int myParametricPosStateSize;

   protected MechSystemSolver mySolver;
   protected DynamicAttachmentWorker myAttachmentWorker;

   protected static boolean DEFAULT_DYNAMICS_ENABLED = true;
   protected static boolean DEFAULT_PROFILING = false;
   protected static boolean DEFAULT_UPDATE_FORCES_AT_STEP_END = false;

   private boolean myUpdateForcesAtStepEnd = DEFAULT_UPDATE_FORCES_AT_STEP_END;
   PropertyMode myUpdateForcesAtStepEndMode = PropertyMode.Inherited;   

   SparseBlockMatrix myMassMatrix;   

   protected static PosStabilization myDefaultStabilization =
      PosStabilization.GlobalMass;
   //protected PosStabilization myStabilization = myDefaultStabilization;

   protected boolean myDynamicsEnabled = DEFAULT_DYNAMICS_ENABLED; 
   protected boolean myProfilingP = DEFAULT_PROFILING;
   protected int myProfilingCnt = 0;

   protected static Integrator DEFAULT_INTEGRATOR =
      Integrator.ConstrainedBackwardEuler;
   protected Integrator myIntegrator = DEFAULT_INTEGRATOR;

   protected static SparseSolverId DEFAULT_MATRIX_SOLVER =
      SparseSolverId.Pardiso;
   // define a separate default matrix solver that can be overridden
   protected static SparseSolverId myDefaultMatrixSolver =
      DEFAULT_MATRIX_SOLVER;
   protected SparseSolverId myMatrixSolver = DEFAULT_MATRIX_SOLVER;

   protected boolean myInsideAdvanceP = false;
   protected double myAvgSolveTime;
   protected StepAdjustment myStepAdjust;

   String myPrintState = null;
   PrintWriter myPrintStateWriter = null;
   double myPrintInterval = -1;
   double myLastPrintTime = 0;

   // objects for projecting position constraints
   //KKTSolver myPosSolver = new KKTSolver();
   VectorNd myRg = new VectorNd(0);
   VectorNd myBg = new VectorNd(0);
   VectorNd myRn = new VectorNd(0);
   VectorNd myBn = new VectorNd(0);

   VectorNi myBilateralSizes = new VectorNi(100);
   VectorNi myUnilateralSizes = new VectorNi(100);

   private double myPenetrationLimit = -1;

   public static PropertyList myProps =
      new PropertyList (MechModel.class, RenderableModelBase.class);

   /**
    * Special class to save/restore constraint forces as state
    */
   public class ConstraintForceStateSaver implements HasNumericState {

      public boolean hasState() {
         return true;
      }

      public void getInitialState (NumericState nstate) {
         if (false) {
            int numf = getNumBilateralForces() + getNumUnilateralForces();
            nstate.zput (numf);
            int di = nstate.dsize();
            int dsize = di+numf;
            nstate.dsetSize (dsize);
            double[] dbuf = nstate.dbuffer();
            while (di < dsize) {
               dbuf[di++] = 0;
            }
         }
         else {
            // just set numf to -1, meaning that all forces will be set to 0
            nstate.zput (-1);
         }
         if (nstate.hasDataFrames()) {
            nstate.addDataFrame (this);
         }
      }

      public void getState (DataBuffer data) {
         int numf = getNumBilateralForces() + getNumUnilateralForces();
         data.zput (numf);
         int di = data.dsize();
         data.dsetSize (di+numf);
         // create special vector to access the state ...
         VectorNd dvec = new VectorNd();
         dvec.setBuffer (data.dsize(), data.dbuffer());
         for (int i=0; i<myConstrainers.size(); i++) {
            Constrainer c = myConstrainers.get(i);
            di = c.getBilateralForces (dvec, di);
            di = c.getUnilateralForces (dvec, di);
         }         
      }

      public void setState (DataBuffer data) {
         int chkf = getNumBilateralForces() + getNumUnilateralForces();
         int numf = data.zget();
         if (numf != -1 && numf != chkf) {
            throw new IllegalArgumentException (
               "number of impulse forces is "+numf+", expecting "+chkf);
         }

         // numf == -1 means all forces should be set to 0
         
         // create special vector to access the state ...
         VectorNd dvec = new VectorNd();
         int di = 0;
         if (numf != -1) {
            dvec.setBuffer (data.dsize(), data.dbuffer());
            di = data.doffset();
         }
         else {
            dvec.setSize (chkf);
         }
         for (int i=0; i<myConstrainers.size(); i++) {
            Constrainer c = myConstrainers.get(i);
            di = c.setBilateralForces (dvec, 1.0, di);
            di = c.setUnilateralForces (dvec, 1.0, di);
         }
         if (numf != -1) {
            data.dsetOffset (di);
         }
      }
   }

   ConstraintForceStateSaver myConstraintForceStateSaver =
      new ConstraintForceStateSaver();

   static {
      myProps.add (
         "dynamicsEnabled", "enable dynamics", DEFAULT_DYNAMICS_ENABLED);
      myProps.add (
         "penetrationLimit", 
         "collision penetration limit for step reduction", -1);
      myProps.addInheritable (
         "updateForcesAtStepEnd",
         "update forces values at the end of each step", 
         DEFAULT_UPDATE_FORCES_AT_STEP_END);
      myProps.add (
         "profiling", "print step time and computation time", DEFAULT_PROFILING);
      myProps.add ("integrator", "integration method", DEFAULT_INTEGRATOR);
      myProps.add ("matrixSolver", "matrix solver", DEFAULT_MATRIX_SOLVER);

   }

   public void setPenetrationLimit (double lim) {
      if (lim != myPenetrationLimit) {
         myPenetrationLimit = lim;
      }
   }

   public double getPenetrationLimit() {
      if (myPenetrationLimit == -1) {
         double radius = RenderableUtils.getRadius (this);
         if (radius != 0) {
            myPenetrationLimit = 0.05*radius;
         }
      }
      return myPenetrationLimit;
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      setDynamicsEnabled (DEFAULT_DYNAMICS_ENABLED);
      setPenetrationLimit (-1);
      setProfiling (DEFAULT_PROFILING);
      myUpdateForcesAtStepEnd = DEFAULT_UPDATE_FORCES_AT_STEP_END;
      myUpdateForcesAtStepEndMode = PropertyMode.Inherited;
      // mySolver will be null if setDefaultValues() called from constructor
      if (mySolver != null) {
         setStabilization (myDefaultStabilization);
         mySolver.setUpdateForcesAtStepEnd (DEFAULT_UPDATE_FORCES_AT_STEP_END);
      }
      setMatrixSolver (myDefaultMatrixSolver);
      setIntegrator (DEFAULT_INTEGRATOR);
   }

   public boolean getDynamicsEnabled() {
      return myDynamicsEnabled;
   }

   public void setDynamicsEnabled (boolean enable) {
      myDynamicsEnabled = enable;
   }

   private void allocateSolver (MechSystemSolver oldSolver) {
      if (oldSolver != null) {
         mySolver = new MechSystemSolver (this, oldSolver);
      }
      else {
         mySolver = new MechSystemSolver (this);
         mySolver.setStabilization (getStabilization());
         mySolver.setUpdateForcesAtStepEnd (getUpdateForcesAtStepEnd());
         mySolver.setIntegrator (getIntegrator());
         mySolver.setMatrixSolver (getMatrixSolver());
      }
   }

   public MechSystemBase (String name) {
      super (name);
      setMatrixSolver (myDefaultMatrixSolver);
      setIntegrator (DEFAULT_INTEGRATOR);     
      allocateSolver (/*oldSolver=*/null);
      myAttachmentWorker = new DynamicAttachmentWorker();
      //setStabilization (myDefaultStabilization);
      //setUpdateForcesAtStepEnd (DEFAULT_UPDATE_FORCES_AT_STEP_END);
   }

   /**
    * Returns the topmost MechSystem, if any, that is associated with
    * a specific component. 
    * 
    * @param comp component to start with
    * @return topmost MechSystem on or above <code>comp</code>, or
    * <code>null</code> if there is none.
    */
   public static MechSystem topMechSystem (ModelComponent comp) {
      MechSystem mech = null;
      while (comp != null) {
         if (comp instanceof MechSystem) {
            mech = (MechSystem) comp;
         }
         comp=comp.getParent();
      }
      return mech;
   }
   
   public MechSystemSolver getSolver() {
      return mySolver;
   }

   public SparseBlockMatrix createVelocityJacobian() {
      updateDynamicComponentLists();
      return new SparseBlockMatrix (new int[0], myDynamicSizes);
   }

   public void reduceVelocityJacobian (SparseBlockMatrix J) {
      updateDynamicComponentLists();
      if (getAttachments().size() > 0 && !J.isVerticallyLinked()) {
	 J.setVerticallyLinked(true);
      }
      for (DynamicAttachment a : getAttachments()) {
         myAttachmentWorker.reduceRowMatrix (a, J);
      }
   }

   public int getNumUnilateralForces () {
      myUnilateralSizes.setSize (0);
      getUnilateralConstraintSizes (myUnilateralSizes);
      return myUnilateralSizes.sum();
   }      


   public void getUnilateralConstraintSizes (VectorNi sizes) {
      updateForceComponentList();
      for (int i=0; i<myConstrainers.size(); i++) {
         myConstrainers.get(i).getUnilateralSizes (sizes);
      }      
   }

   public void getUnilateralConstraints (SparseBlockMatrix NT, VectorNd dn) {

      if (NT.numBlockRows() != 0 || NT.numBlockCols() != 0) {
         throw new IllegalArgumentException (
            "On entry, NT should be empty with zero size");
      }
      updateForceComponentList();
      myUnilateralSizes.setSize (0);
      getUnilateralConstraintSizes (myUnilateralSizes);
      NT.setColCapacity (myUnilateralSizes.size());
      NT.addRows (myDynamicSizes, myDynamicSizes.length);
      if (dn != null) {
         dn.setSize (myUnilateralSizes.sum());
      }
      int idx = 0;
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).addUnilateralConstraints (
            NT, dn, idx);
      }
      for (DynamicAttachment a : getAttachments()) {
         myAttachmentWorker.reduceConstraints (a, NT, dn, false);
      }
      // need this for now - would be good to get rid of it:
      NT.setVerticallyLinked (true);
   }

   public int getNumBilateralForces () {
      myBilateralSizes.setSize (0);
      getBilateralConstraintSizes (myBilateralSizes);
      return myBilateralSizes.sum();
   }      

   public void getBilateralConstraintSizes (VectorNi sizes) {
      updateForceComponentList();
      for (int i=0; i<myConstrainers.size(); i++) {
         myConstrainers.get(i).getBilateralSizes (sizes);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isBilateralStructureConstant() {
      // assume true - override if 
      return true;
   }
   
   public void getBilateralConstraints (SparseBlockMatrix GT, VectorNd dg) {

      if (GT.numBlockRows() != 0 || GT.numBlockCols() != 0) {
         throw new IllegalArgumentException (
            "On entry, GT should be empty with zero size");
      }
      updateForceComponentList();
      updateDynamicComponentLists();
      myBilateralSizes.setSize (0);
      getBilateralConstraintSizes (myBilateralSizes);
      GT.setColCapacity (myBilateralSizes.size());
      GT.addRows (myDynamicSizes, myDynamicSizes.length);
      if (dg != null) {
         dg.setSize (myBilateralSizes.sum());
      }

      IntHolder changeCnt = new IntHolder(0);
      int idx = 0;
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).addBilateralConstraints (
            GT, dg, idx);
      }      
      for (DynamicAttachment a : getAttachments()) {
         myAttachmentWorker.reduceConstraints (a, GT, dg, false);
      }
      // need this for now - would be good to get rid of it:
      GT.setVerticallyLinked (true);
   }

   public void getBilateralInfo (ConstraintInfo[] ginfo) {
      updateForceComponentList();
      int idx = 0;
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).getBilateralInfo (ginfo, idx);
      }
   }

   public void setBilateralForces (VectorNd lam, double s) {
      setBilateralForces (lam, s, 0);
   }         

   public int setBilateralForces (VectorNd lam, double s, int idx) {
      updateForceComponentList();
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).setBilateralForces (lam, s, idx);
      }
      return idx;
   }

   public void getBilateralForces (VectorNd lam) {
      updateForceComponentList();
      getBilateralForces (lam, 0);
   }
   
   public int getBilateralForces (VectorNd lam, int idx) {
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).getBilateralForces (lam, idx);
      }
      return idx;
   }

   public void getUnilateralInfo (ConstraintInfo[] ninfo) {
      updateForceComponentList();
      int idx = 0;
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).getUnilateralInfo (ninfo, idx);
      }
   }

   public void setUnilateralForces (VectorNd the, double s) {
      setUnilateralForces (the, s, 0);
   }         

   public int setUnilateralForces (VectorNd the, double s, int idx) {
      updateForceComponentList();
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).setUnilateralForces (the, s, idx);
      }
      return idx;
   }

   public void getUnilateralForces (VectorNd the) {
      updateForceComponentList();
      getUnilateralForces (the, 0);
   }         

   public int getUnilateralForces (VectorNd the, int idx) {
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).getUnilateralForces (the, idx);
      }
      return idx;
   }

   public int maxFrictionConstraintSets () {
      updateForceComponentList();
      int max = 0;
      for (int i=0; i<myConstrainers.size(); i++) {
         max += myConstrainers.get(i).maxFrictionConstraintSets();
      }      
      return max;
   }

   // Called from the top level
   public void getFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo) {

      if (DT.numBlockRows() != 0 || DT.numBlockCols() != 0) {
         throw new IllegalArgumentException (
            "On entry, DT should be empty with zero size");
      }
      updateForceComponentList();
      DT.addRows (myDynamicSizes, myDynamicSizes.length);
      int idx = 0;
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).addFrictionConstraints (DT, finfo, idx);
      }      
      //idxh.value = addFrictionConstraints (DT, finfo, idxh.value);
      for (DynamicAttachment a : getAttachments()) {
         myAttachmentWorker.reduceConstraints (a, DT, null, false);
      }
   }
   
   public boolean updateConstraints (
      double t, StepAdjustment stepAdjust, int flags) {

      updateForceComponentList();
      double maxpen = 0;
      boolean hasConstraints = false;
      for (int i=0; i<myConstrainers.size(); i++) {
         double pen = myConstrainers.get(i).updateConstraints (t, flags);
         if (pen >= 0) {
            hasConstraints = true;
            if (pen > maxpen) {
               maxpen = pen;
            }
         }
      } 
      double penlimit = getPenetrationLimit();
      if (penlimit > 0 && maxpen > penlimit && stepAdjust != null) {
         stepAdjust.recommendAdjustment (
            0.5 /*penlimit/maxpen*/, "contact penetration exceeds "+penlimit);
      }
      return hasConstraints;
   }
   

   public int getStructureVersion() {
      return myStructureVersion;
   }

   /**
    * Returns in <code>sizes</code> the number of (velocity) degrees of freedom
    * for the dynamic components in the system. The results are arranged
    * according to each component's solveIndex. If the length of
    * <code>sizes</code> is less than the number of dynamic components, the
    * results are truncated.
    *
    * @param dofs returns the number of DOFs for each dynamic component
    */
   public void getDynamicDOFs (int[] dofs) {
      updateDynamicComponentLists();
      int max = Math.min (dofs.length, myDynamicSizes.length);
      for (int i=0; i<max; i++) {
         dofs[i] = myDynamicSizes[i];
      }
   }

   public static void placeDynamicComponent (
      List<DynamicComponent> active,
      List<DynamicComponent> attached, 
      List<DynamicComponent> parametric,
      DynamicComponent d) {

      if (d.isActive()) {
         active.add (d);
      }
      else if (d.isAttached()) {
         attached.add (d);
      }
      else {
         parametric.add (d);
      }
   }

   /**
    * Returns a list of the dynamic components in this model. Used for
    * debugging only. Must not be modified.
    */
   public ArrayList<DynamicComponent> getDynamicComponents() {
      return myDynamicComponents;
   }

   protected void updateDynamicComponentLists() {

      if (myDynamicComponents == null) {
         myDynamicComponents = new ArrayList<DynamicComponent>();
         ArrayList<DynamicComponent> active =
            new ArrayList<DynamicComponent>();
         ArrayList<DynamicComponent> attached =
            new ArrayList<DynamicComponent>();
         ArrayList<DynamicComponent> parametric =
            new ArrayList<DynamicComponent>();
         if (useAllDynamicComps) {
            myAllDynamicComponents = new ArrayList<DynamicComponent>();
            getDynamicComponents (myAllDynamicComponents);
            for (DynamicComponent c : myAllDynamicComponents) {
               placeDynamicComponent (active, attached, parametric, c);
            }
         }
         else {
            getDynamicComponents (active, attached, parametric);
         }

         myNumActive = active.size();
         myNumAttached = attached.size();

         myParametricVelStateSize = 0;
         myParametricPosStateSize = 0;
         myParametricComponents = new ArrayList<MotionTargetComponent>();
         for (DynamicComponent c : parametric) {
            if (c instanceof MotionTargetComponent) {
               myParametricPosStateSize += c.getPosStateSize();
               myParametricVelStateSize += c.getVelStateSize();
               myParametricComponents.add ((MotionTargetComponent)c);
            }
         }
         myNumParametric = myParametricComponents.size();
         
         if (myParametricsInSystemMatrix) {
            myDynamicSizes = new int[myNumActive+myNumAttached+myNumParametric];
         }
         else {
            myDynamicSizes = new int[myNumActive+myNumAttached];
         }
            
         myActiveVelStateSize = 0;
         myActivePosStateSize = 0;
         myAttachedVelStateSize = 0;
         myAttachedPosStateSize = 0;

         int idx = 0;
         for (DynamicComponent c : active) {
            myDynamicComponents.add (c);
            c.setSolveIndex (idx);
            myDynamicSizes[idx++] = c.getVelStateSize();
            myActivePosStateSize += c.getPosStateSize();
            myActiveVelStateSize += c.getVelStateSize();
         }
         for (DynamicComponent c : myParametricComponents) {
            if (MechModel.myParametricsInSystemMatrix) {
               myDynamicComponents.add (c);
               c.setSolveIndex (idx);
               myDynamicSizes[idx++] = c.getVelStateSize();
            }
            else {
               c.setSolveIndex (-1);
            }
         }
         for (DynamicComponent c : attached) {
            myDynamicComponents.add (c);
            c.setSolveIndex (idx);
            myDynamicSizes[idx++] = c.getVelStateSize();
            myAttachedPosStateSize += c.getPosStateSize();
            myAttachedVelStateSize += c.getVelStateSize();
         }
         mySystemSize = 0;
         for (int i=0; i<myDynamicSizes.length; i++) {
            mySystemSize += myDynamicSizes[i];
         }
         myNumComponents = myDynamicSizes.length;
      }
   }

   protected void updateForceComponentList() {
      // Build new constrainer and force effector lists if necessary.
      // Create using temporary lists just in case clearCachedData() gets
      // called while the lists are being built.

      ArrayList<Constrainer> newConstrainers = null;
      ArrayList<ForceEffector> newForceEffectors = null;      
      if (myConstrainers == null) {
         newConstrainers = new ArrayList<Constrainer>();
         getConstrainers (newConstrainers, 0);
      }
      if (myForceEffectors == null) {
         newForceEffectors = new ArrayList<ForceEffector>();
         getForceEffectors (newForceEffectors, 0);
      }
      if (newConstrainers != null) {
         myConstrainers = newConstrainers;
      }
      if (newForceEffectors != null) {
         myForceEffectors = newForceEffectors;
      }
   }

   protected void updateAuxStateComponentList() {
      if (myAuxStateComponents == null) {
         // XXX getAuxStateComponents might trigger a FEM surface build, which
         // will in turn trigger a structure change that will call
         // clearCachedData(). So, we need to make sure
         // updateAuxStateComponentList() is called before any other updates
         // are called.
         ArrayList<HasNumericState> list = new ArrayList<HasNumericState>();
         getAuxStateComponents (list, 0);
         myAuxStateComponents = list;
      }
   }

   /**
    * Should be overridden in subclasses to return all the HasSlaveObjects
    * components within this model.
    * @param comps HasSlaveObjects components should be added to this list
    */
   public void getSlaveObjectComponents (List<HasSlaveObjects> comps, int level) {
   }
   
   protected void updateSlaveObjectComponentList() {
      if (mySlaveObjectComponents == null) {
         mySlaveObjectComponents = new ArrayList<HasSlaveObjects>();
         getSlaveObjectComponents (mySlaveObjectComponents, 0);
      }
   }

   /** 
    * {@inheritDoc}
    */
   public void getActivePosState (VectorNd q) {
      updateDynamicComponentLists();
      q.setSize (myActivePosStateSize);
      getActivePosState (q, 0);
   }

   protected int getActivePosState (VectorNd q, int idx) {
      double[] buf = q.getBuffer();
      for (int i=0; i<myNumActive; i++) {
         idx = myDynamicComponents.get(i).getPosState (buf, idx);
      }
      return idx;
   }

   /** 
    * {@inheritDoc}
    */
   public void getActiveVelState (VectorNd u) {
      updateDynamicComponentLists();
      u.setSize (myActiveVelStateSize);
      getActiveVelState (u, 0);
   }

   protected int getActiveVelState (VectorNd u, int idx) {
      double[] buf = u.getBuffer();
      for (int i=0; i<myNumActive; i++) {
         idx = myDynamicComponents.get(i).getVelState (buf, idx);
      }
      return idx;
   }

   public int getActiveVelState (VectorNd u, int idx, boolean bodyCoords) {
      updateDynamicComponentLists();
      double[] buf = u.getBuffer();
      for (int i=0; i<myNumActive; i++) {
         DynamicComponent c = myDynamicComponents.get(i);
         if (c instanceof RigidBody) {
            if (bodyCoords) {
               idx = ((RigidBody)c).getBodyVelState (buf, idx);
            }
            else {
               idx = ((RigidBody)c).getWorldVelState (buf, idx);
            }
         }
         else {
            idx = c.getVelState (buf, idx);
         }
      }
      return idx;
   }

   /** 
    * {@inheritDoc}
    */
   public void setActivePosState (VectorNd q) {
      setActivePosState (q, 0);
   }
   
   protected int setActivePosState (VectorNd q, int idx) {
      updateDynamicComponentLists();
      double[] buf = q.getBuffer();
      for (int i=0; i<myNumActive; i++) {
         idx = myDynamicComponents.get(i).setPosState (buf, idx);
      }
      updateAttachmentPos (getActiveAttachments());
      updateSlavePos();
      updateAttachmentVel (getActiveAttachments()); // AVEL
      updateSlaveVel(); // AVEL
      return idx;
   }

   public void addActivePosImpulse (VectorNd x, double h, VectorNd v) {
      updateDynamicComponentLists();
      double[] xbuf = x.getBuffer();
      double[] vbuf = v.getBuffer();
      int xidx = 0;
      int vidx = 0;
      for (int i=0; i<myNumActive; i++) {
         DynamicComponent d = myDynamicComponents.get(i);
         d.addPosImpulse (xbuf, xidx, h, vbuf, vidx);
         xidx += d.getPosStateSize();
         vidx += d.getVelStateSize();
      }
   }

   /** 
    * {@inheritDoc}
    */
   public void setActiveVelState (VectorNd u) {
      setActiveVelState (u, 0);
   }


   protected int setActiveVelState (VectorNd u, int idx) {
      updateDynamicComponentLists();
      double[] buf = u.getBuffer();
      for (int i=0; i<myNumActive; i++) {
         idx = myDynamicComponents.get(i).setVelState (buf, idx);
      }
      updateAttachmentVel (getActiveAttachments());
      updateSlaveVel();
      //updateVelState();
      return idx;
   }

   /** 
    * {@inheritDoc}
    */
   public int getActivePosStateSize() {
      updateDynamicComponentLists();
      return myActivePosStateSize;
   }

   /** 
    * {@inheritDoc}
    */
   public int getActiveVelStateSize() {
      updateDynamicComponentLists();
      return myActiveVelStateSize;
   }

   public void getActivePosDerivative (VectorNd dxdt, double t) {
      updateDynamicComponentLists();
      dxdt.setSize (myActivePosStateSize);
      double[] buf = dxdt.getBuffer();
      int idx = 0;
      for (int i=0; i<myNumActive; i++) {
         idx = myDynamicComponents.get(i).getPosDerivative (buf, idx);
      }
   } 

   /**
    * Returns a list of all the active dynamic components, which
    * collectively determine the values returned by {@link #getActiveForces},
    * {@link #getActivePosState}, etc. The returned list is a copy and
    * may be modified.
    *  
    * @return list of all the active dynamic components.
    */
   public ArrayList<DynamicComponent> getActiveDynamicComponents() {
      ArrayList<DynamicComponent> comps = new ArrayList<>();
      for (int i=0; i<myNumActive; i++) {
         comps.add (myDynamicComponents.get(i));
      }  
      return comps;
   }
   
   /** 
    * {@inheritDoc}
    */
   public void getActiveForces (VectorNd f) {
      updateDynamicComponentLists();
      f.setSize (myActiveVelStateSize);
      //updateForcesIfNecessary (t);
      double[] buf = f.getBuffer();
      int idx = 0;
      for (int i=0; i<myNumActive; i++) {
         idx = myDynamicComponents.get(i).getForce (buf, idx);
      }      
   }

   /** 
    * {@inheritDoc}
    */
   public void setActiveForces (VectorNd f) {
      updateDynamicComponentLists();
      double[] buf = f.getBuffer();
      int idx = 0;
      for (int i=0; i<myNumActive; i++) {
         idx = myDynamicComponents.get(i).setForce (buf, idx);
      }      
   }

   public void getForces (VectorNd f) {
      updateDynamicComponentLists();
      f.setSize (
         myActiveVelStateSize+myAttachedVelStateSize+myParametricVelStateSize);
      double[] buf = f.getBuffer();
      int idx = 0;
      for (int i=0; i<myDynamicComponents.size(); i++) {
         idx = myDynamicComponents.get(i).getForce (buf, idx);
      }      
   }

   public void setForces (VectorNd f) {
      updateDynamicComponentLists();
      double[] buf = f.getBuffer();
      int idx = 0;
      for (int i=0; i<myDynamicComponents.size(); i++) {
         idx = myDynamicComponents.get(i).setForce (buf, idx);
      }      
   }

   /** 
    * {@inheritDoc}
    */
   public void getParametricForces (VectorNd f) {
      updateDynamicComponentLists();
      f.setSize (myParametricVelStateSize);
      double[] buf = f.getBuffer();
      int idx = 0;
      for (int i=myNumActive; i<myNumActive+myNumParametric; i++) {
         idx = myDynamicComponents.get(i).getForce (buf, idx);
      }      
   }

   /** 
    * {@inheritDoc}
    */
   public void setParametricForces (VectorNd f) {
      updateDynamicComponentLists();
      double[] buf = f.getBuffer();
      int idx = 0;
      for (int i=myNumActive; i<myNumActive+myNumParametric; i++) {
         idx = myDynamicComponents.get(i).setForce (buf, idx);
      }      
   }

   public synchronized String getPrintState() {
      return myPrintState;
   }

   public synchronized void setPrintState (String fmt, double interval) {
      myPrintState = fmt;
      myPrintInterval = interval;
      myLastPrintTime = 0;
   }

   public void setPrintState (String fmt) {
      setPrintState (fmt, -1);
   }

   public synchronized PrintWriter openPrintStateFile (String fileName)
      throws IOException {
      if (myPrintStateWriter != null) {
         myPrintStateWriter.close();
      }
      myPrintStateWriter = new PrintWriter (
         new BufferedWriter (new FileWriter (fileName)));
      return myPrintStateWriter;
   }

   public synchronized PrintWriter reopenPrintStateFile (String fileName)
      throws IOException {
      if (myPrintStateWriter != null) {
         myPrintStateWriter.close();
      }
      myPrintStateWriter = new PrintWriter (
         new BufferedWriter (new FileWriter (fileName, /*append=*/true)));
      return myPrintStateWriter;
   }

   public synchronized void closePrintStateFile () throws IOException {
      if (myPrintStateWriter != null) {
         myPrintStateWriter.close();
      }
   }

   public synchronized void writePrintStateHeader (String description) {
      
      updateDynamicComponentLists();
      if (myPrintStateWriter == null) {
         System.out.println ("TEST \""+description+"\"");
         System.out.print ("comps: [");
      }
      else {
         myPrintStateWriter.println ("TEST \""+description+"\"");
         myPrintStateWriter.print ("comps: [");
      }
      for (int i=0; i<myNumActive; i++) {
         DynamicComponent c = myDynamicComponents.get(i);
         String symbol = null;
         if (c.getPosStateSize() == 3) {
            // XXX HACK - shouldn't assume posStateSize == 3 means a point
            symbol = "P";
         }
         else if (c instanceof Frame) {
            symbol = "F";
            if (c.getPosStateSize() > 7) {
               symbol += " R" + (c.getPosStateSize()-7);
            }
         }
         else {
            throw new UnsupportedOperationException (
               "printState not supported for " + c.getClass());
         }
         if (myPrintStateWriter == null) {
            System.out.print (" " + symbol);
         }
         else {
            myPrintStateWriter.print (" " + symbol);
         }           
      }
      if (myPrintStateWriter == null) {
         System.out.println (" ]");
      }
      else {
         myPrintStateWriter.println (" ]");
         myPrintStateWriter.flush();
      }
   }

   private synchronized void printState (String fmt, double t) {
      if (myPrintInterval != -1) {
         // reset last print time if necessary
         if (TimeBase.compare (t, myLastPrintTime) < 0) {         
            myLastPrintTime = ((int)(t/myPrintInterval))*myPrintInterval;
         }
      }
      if (myPrintInterval == -1 ||
          TimeBase.compare (t, myLastPrintTime+myPrintInterval) >= 0) {

         updateDynamicComponentLists();
         VectorNd x = new VectorNd (myActivePosStateSize);
         VectorNd v = new VectorNd (myActiveVelStateSize);
         getActivePosState (x, 0);
         // Hack: get vel in body coords until data is converted ...
         getActiveVelState (v, 0, /*bodyCoords=*/false);
         if (myPrintStateWriter == null) {
            System.out.println ("t="+t+":");
            System.out.println ("v: " + v.toString (fmt));
            System.out.println ("x: " + x.toString (fmt));
         }
         else {
            myPrintStateWriter.println ("t="+t+":");
            myPrintStateWriter.println ("v: " + v.toString (fmt));
            myPrintStateWriter.println ("x: " + x.toString (fmt));
            myPrintStateWriter.flush();
         }
         if (myPrintInterval != -1) {
            myLastPrintTime += myPrintInterval;
         }
      }
   }

   protected void advanceState (double t0, double t1) {
      updateAuxStateComponentList();
      for (int i=0; i<myAuxStateComponents.size(); i++) {
         myAuxStateComponents.get(i).advanceState (t0, t1);
      }
   }      

   public StepAdjustment preadvance (double t0, double t1, int flags) {
      advanceState (t0, t1);
      // zero forces
      updateDynamicComponentLists();
      //FunctionTimer timer = new FunctionTimer();
      //timer.start();
      for (int i=0; i<myDynamicComponents.size(); i++) {
         myDynamicComponents.get(i).zeroForces();
      }
      //timer.stop();
      //System.out.println ("preadvance=" + timer.result(1));
      return null;
   }

   private String getName (Object obj) {
      if (obj instanceof ModelComponent) {
         return ComponentUtils.getPathName ((ModelComponent)obj);
      }
      else {
         return obj.toString();
      }
   }

   public StepAdjustment advance (double t0, double t1, int flags) {

      myInsideAdvanceP = true;
      StepAdjustment stepAdjust = new StepAdjustment();
      collectInitialForces();

      double solveTime = 0;
      if (myProfilingP) {
         solveTime = System.nanoTime();
      }      

      if (!myDynamicsEnabled) {
         mySolver.nonDynamicSolve (t0, t1, stepAdjust);
         recursivelyFinalizeAdvance (null, t0, t1, flags, 0);
      }
      else {
         if (t0 == 0 && myPrintState != null) {
            printState (myPrintState, 0);
         }
         recursivelyPrepareAdvance (t0, t1, flags, 0);
         // Force solver to perform an analyze step on its bilateral constraint
         // matrices if state is volatile, the bilateral constraint structure
         // is not constant, and t0 != 0. This is to ensure precise numeric
         // repeatability: if the orginal advance didn't require an analyze
         // step but the restored state did, there may be small (machine
         // precision level) differences in the results. t0 == 0 is ignored
         // because it is assume an analyze step will be performed there
         // regardless
         if ((flags & Model.STATE_IS_VOLATILE) != 0 && t0 != 0 &&
             !isBilateralStructureConstant()) {
            mySolver.forceBilateralAnalysis();
         }
         mySolver.solve (t0, t1, stepAdjust);
         //FunctionTimer timer = new FunctionTimer();
         //timer.start();
         DynamicComponent c = checkVelocityStability();
         if (c instanceof DynamicComponent) {
            throw new NumericalException (
               "Unstable velocity detected, component " + getName(c));
         }
         else if (c != null) {
            throw new NumericalException (
               "Unstable velocity detected, dynamic agent " + c);
         }
         recursivelyFinalizeAdvance (stepAdjust, t0, t1, flags, 0);
         //timer.stop();
         //System.out.println ("finalize " + timer.result(1));
         if (myPrintState != null) {
            printState (myPrintState, t1);
         }
      }

      if (myProfilingP) {
         solveTime = System.nanoTime() - solveTime;
         int cnt = myProfilingCnt++;
         myAvgSolveTime = (cnt*myAvgSolveTime + solveTime)/(cnt+1);
         System.out.println (
            "T1=" + t1 + " avgSolveTime=" + myAvgSolveTime/1e6 + " ms");
      }
      myInsideAdvanceP = false;
      return stepAdjust;
   }

   public ComponentState createState (
      ComponentState prevState) {
      NumericState state;
      if (prevState instanceof NumericState) {
         NumericState last = (NumericState)prevState;
         // use old state to set capacity. Make the capacity a 10%
         // bigger, just in case
         int dcap = (int)(1.1*last.dsize());
         int zcap = (int)(1.1*last.zsize());
         state = new NumericState (zcap, dcap, 0);
       }
      else {
         state = new NumericState (1000, 1000, 0);
      }
      return state;
   }

   private class DynamicStateOffsets {
      int posOff;
      int velOff;

      DynamicStateOffsets (int poff, int voff) {
         posOff = poff;
         velOff = voff;
      }
   }

   private HashMap<DynamicComponent,DynamicStateOffsets>
      getDynamicCompOffsets (
         ArrayList<Object> comps, int idx, int numc, int off) {

      HashMap<DynamicComponent,DynamicStateOffsets> map =
         new HashMap<DynamicComponent,DynamicStateOffsets>();

      int posOff = off;
      // calculate velOff first
      int velOff = off;
      for (int i=0; i<numc; i++) {
         DynamicComponent c = (DynamicComponent)comps.get(idx++);
         velOff += c.getPosStateSize();
      }
      for (int i=0; i<numc; i++) {
         DynamicComponent c = (DynamicComponent)comps.get(idx++);
         map.put (c, new DynamicStateOffsets (posOff, velOff));
         posOff += c.getPosStateSize();
         velOff += c.getVelStateSize();
      }
      return map;
   }

   // NEWX
   public void setState (ComponentState pstate) {
      if (!(pstate instanceof NumericState)) {
         throw new IllegalArgumentException ("pstate not a NumericState");
      }
      NumericState state = (NumericState)pstate;
      state.resetOffsets();

      updateAuxStateComponentList();
      updateDynamicComponentLists();
      updateForceComponentList();

      int chk = state.zget();
      if (chk != 0x1234) {
         System.out.println (
            "zoffset=" + state.zoffset() +
            " size=" + state.dsize() + " " + state.zsize());
         throw new IllegalArgumentException (
            "state checksum is "+chk+", expecting "+0x1234);
      }
      int numDynComps = state.zget();
      int numAuxStateComps = state.zget();

      if (useAllDynamicComps) {
         if (numDynComps != myAllDynamicComponents.size()) {
            throw new IllegalArgumentException (
               "state contains "+numDynComps+" dynamic components, "+
               "expecting "+myAllDynamicComponents.size());
         }
         if (numAuxStateComps != myAuxStateComponents.size()) {
            throw new IllegalArgumentException (
               "number of AuxState components is "+numAuxStateComps+
               ", expecting "+myAuxStateComponents.size());
         }
         for (DynamicComponent c : myAllDynamicComponents) {
            c.setState (state);
         }
         updateSlavePos();
         updateSlaveVel();
      }
      else {
         if (numDynComps != myNumActive+myNumParametric) {
            throw new IllegalArgumentException (
               "state contains "+numDynComps+" active & parametric components, "+
               "expecting "+(myNumActive+myNumParametric));
         }
         if (numAuxStateComps != myAuxStateComponents.size()) {
            throw new IllegalArgumentException (
               "number of AuxState components is "+numAuxStateComps+
               ", expecting "+myAuxStateComponents.size());
         }

         for (int i=0; i<myNumActive+myNumParametric; i++) {
            DynamicComponent c = myDynamicComponents.get(i);
            c.setState (state);
         }
         updatePosState(); // do we need?
         updateVelState(); // do we need?
      }
      
//      state.dskip (di);

      // setting aux state must be done here because it may change the number
      // of bilateral and unilateral forces expected by the constrainers
      for (int i=0; i<myAuxStateComponents.size(); i++) {
         myAuxStateComponents.get(i).setState (state);
      }
      myConstraintForceStateSaver.setState (state);
   }
   
   // NEWX
   public void getState (ComponentState pstate) {
      if (!(pstate instanceof NumericState)) {
         throw new IllegalArgumentException ("pstate not a NumericState");
      }
      NumericState state = (NumericState)pstate;
      state.clear();

      // get the required sizes
      updateAuxStateComponentList();
      updateDynamicComponentLists();
      updateForceComponentList();

      int numb = getNumBilateralForces();
      int numu = getNumUnilateralForces();

      state.zput (0x1234);
      if (useAllDynamicComps) {
         state.zput (myAllDynamicComponents.size());
         state.zput (myAuxStateComponents.size());
         if (state.hasDataFrames()) {
            state.addDataFrame (null);
         }
         for (DynamicComponent c : myAllDynamicComponents) {
            state.getState (c);
         }
      }
      else {
         state.zput (myNumActive+myNumParametric);
         state.zput (myAuxStateComponents.size());
         if (state.hasDataFrames()) {
            state.addDataFrame (null);
         }

         for (int i=0; i<myNumActive+myNumParametric; i++) {
            DynamicComponent c = myDynamicComponents.get(i);
            state.getState (c);
         }
      }
      
      updateAuxStateComponentList();
      for (int i=0; i<myAuxStateComponents.size(); i++) {
         state.getState (myAuxStateComponents.get(i));
      }
      state.getState (myConstraintForceStateSaver);
   }
 
   public void getInitialState (
      ComponentState newstate, ComponentState oldstate) {

      if (!(newstate instanceof NumericState)) {
         throw new IllegalArgumentException ("newstate not a NumericState");
      }
      NumericState nstate = (NumericState)newstate;
      nstate.setHasDataFrames (true);
      nstate.clear();

      updateAuxStateComponentList();
      updateDynamicComponentLists();
      updateForceComponentList();

      HashMap<HasNumericState,NumericState.DataFrame> compMap = 
         new HashMap<HasNumericState,NumericState.DataFrame>();

      NumericState ostate = null;
      if (oldstate != null) {
         if (!(oldstate instanceof NumericState)) {
            throw new IllegalArgumentException ("oldstate not a NumericState");
         }
         ostate = (NumericState)oldstate;
         if (!ostate.hasDataFrames()) {
            throw new IllegalArgumentException ("oldstate does not have frames");
         }
         ostate.resetOffsets();
         int chk = ostate.zget();
         if (chk != 0x1234) {
            throw new IllegalArgumentException (
               "oldstate checksum is "+chk+", expecting "+0x1234);
         }
         for (int k=0; k<ostate.numDataFrames(); k++) {
            NumericState.DataFrame frame = ostate.getDataFrame(k);
            compMap.put (frame.getComp(), frame);
         }
      }
      

      if (useAllDynamicComps) {
         nstate.zput (0x1234);
         nstate.zput (myAllDynamicComponents.size());
         nstate.zput (myAuxStateComponents.size());
         // specify -1 constrainers, to cause forces to be zeroed

         nstate.addDataFrame (null);

         for (DynamicComponent c : myAllDynamicComponents) {
            NumericState.DataFrame frame = compMap.get(c);
            if (frame != null && frame.getVersion() == c.getStateVersion()) {
               nstate.getState (frame, ostate);
            }
            else {
               nstate.getState (c);
            }
         }
      }
      else {
         nstate.zput (0x1234);
         nstate.zput (myNumActive+myNumParametric);
         nstate.zput (myAuxStateComponents.size());
         // specify -1 constrainers, to cause forces to be zeroed

         nstate.addDataFrame (null);

         for (int i=0; i<myNumActive+myNumParametric; i++) {
            HasNumericState c = myDynamicComponents.get(i);
            NumericState.DataFrame frame = compMap.get(c);
            if (frame != null && frame.getVersion() == c.getStateVersion()) {
               nstate.getState (frame, ostate);
            }
            else {
               nstate.getState (c);
            }
         }
      }
      
      for (HasNumericState c : myAuxStateComponents) {
         NumericState.DataFrame frame = compMap.get(c);
         if (frame != null && frame.getVersion() == c.getStateVersion()) {
            nstate.getState (frame, ostate);
         }
         else {
            nstate.getState (c);
         }
      }
      myConstraintForceStateSaver.getInitialState (nstate);
      
//      For debugging state frame errors:
//
//      System.out.println (
//         "new initial state, num frames=" + nstate.numDataFrames() +
//         ", state=" + nstate.hashCode());
//      try {
//         PrintWriter pw =
//            ArtisynthIO.newIndentingPrintWriter ("stateComps.txt");
//         for (int i=0; i<nstate.numDataFrames(); i++) {
//            HasNumericState comp = nstate.getDataFrame(i).getComp();
//            if (comp instanceof ModelComponent) {
//               pw.println (ComponentUtils.getPathName ((ModelComponent)comp));
//            }
//            else if (comp != null) {
//               pw.println (comp);
//            }
//            else {
//               pw.println ("null");
//            }
//         }
//         pw.close();
//      }
//      catch (Exception e) {
//         e.printStackTrace(); 
//      }
   }

   public void initialize (double t) {
      if (t == 0) {
         myAvgSolveTime = 0;
         myProfilingCnt = 0;
      }
      updatePosState();
      updateVelState();
      collectInitialForces();
      recursivelyInitialize (t, 0);
   }

   public void recursivelyInitialize (double t, int level) {
      if (level == 0) {
         if (t == 0) {
            clearCachedData(null);
            updateDynamicComponentLists();
            updateForceComponentList();
            for (int i=0; i<myNumParametric; i++) {
               myParametricComponents.get(i).resetTargets();
            }
            updateForces (t);
         }
         //updateForces (t);
      }
   }

   public void recursivelyPrepareAdvance (
      double t0, double t1, int flags, int level) {
   }  

   public void recursivelyFinalizeAdvance (
      StepAdjustment stepAdjust, double t0, double t1, int flags, int level) {
   }  

   public void setProfiling (boolean enable) {
      if (myProfilingP != enable) {
         if (enable) {
            myAvgSolveTime = 0;
            myProfilingCnt = 0;
         }
         myProfilingP = enable;
      }
   }

   public boolean getProfiling() {
      return myProfilingP;
   }

   public static void setDefaultMatrixSolver (SparseSolverId solverType) {
      if (!solverType.isCompatible (Matrix.SYMMETRIC)) {
         throw new IllegalArgumentException (
            "Solver "+solverType+" will not solve symmetric indefinite matrices");
      }
      if (!solverType.isDirect()) {
         throw new IllegalArgumentException (
            "Solver "+solverType+" is not a direct solver");
      }
      myDefaultMatrixSolver = solverType;
   }
   
   public static SparseSolverId getDefaultMatrixSolver() {
      return myDefaultMatrixSolver;
   }

   public void setMatrixSolver (SparseSolverId method) {
      myMatrixSolver = method;
      if (mySolver != null) {
         mySolver.setMatrixSolver (method);
         myMatrixSolver = mySolver.getMatrixSolver();
      }
   }

   public SparseSolverId getMatrixSolver() {
      return myMatrixSolver;
   }

   public Range getMatrixSolverRange() {
      return new EnumRange<SparseSolverId>(
         SparseSolverId.class, new SparseSolverId[] {
            SparseSolverId.Pardiso,
            SparseSolverId.Umfpack });
   }

   public void setIntegrator (Integrator integrator) {
      myIntegrator = integrator;
      if (mySolver != null) {
         mySolver.setIntegrator (integrator);
         if (mySolver.getIntegrator() != integrator) {
            myIntegrator = mySolver.getIntegrator();
         }
      }
   }

   public Integrator getIntegrator () {
      return myIntegrator;
   }

   protected void clearCachedData (ComponentChangeEvent e) {
      myDynamicComponents = null;
      myAttachments = null;
      myActiveAttachments = null;
      myParametricAttachments = null;
      myConstrainers = null;
      myForceEffectors = null;
      myAuxStateComponents = null;
      mySlaveObjectComponents = null;
      myMassMatrix = null;
      myStructureVersion++; // maybe this should go elsewhere?
   }

   public int numActiveComponents() {
      updateDynamicComponentLists();
      return myNumActive;
   }

   public int numParametricComponents() {
      updateDynamicComponentLists();
      return myNumParametric;
   }

  public int numAttachedComponents() {
     updateDynamicComponentLists();
     return myNumAttached;
  }

   /** 
    * Returns the transpose of the attachment constraint matrix.  This is used
    * only for debugging.
    *
    * @return transposed attachment constraint matrix
    */
   public SparseBlockMatrix getAttachmentConstraints () {
      updateDynamicComponentLists();
      // figure out the column sizes
      ArrayList<DynamicAttachment> attachments = getAttachments();
      int[] colSizes = new int[attachments.size()];
      for (int k=0; k<attachments.size(); k++) {
         DynamicComponent slave = attachments.get(k).getSlave();
         int bj = attachments.size()-1-k;
         colSizes[bj] = myDynamicSizes[slave.getSolveIndex()];
      }
      SparseBlockMatrix GT = new SparseBlockMatrix (myDynamicSizes, colSizes);
      for (int k=0; k<attachments.size(); k++) {
         int bj = attachments.size()-1-k;
         DynamicAttachment a = attachments.get(k);
         int ssize = colSizes[bj];
         DynamicComponent[] masters = a.getMasters();
         DynamicComponent slave = a.getSlave();
         MatrixNdBlock sblk = new MatrixNdBlock (ssize, ssize);
         sblk.setIdentity();
         GT.addBlock (slave.getSolveIndex(), bj, sblk);
         for (int i=0; i<masters.length; i++) {
            int bi = masters[i].getSolveIndex();
            int msize = myDynamicSizes[bi];
            MatrixNdBlock mblk = new MatrixNdBlock (msize, ssize);
            a.mulSubGTM (mblk, sblk, i);
            mblk.negate();
            GT.addBlock (bi, bj, mblk);
         }
      }
      return GT;
   }

   protected static final int HAS_ACTIVE_MASTERS = 0x01;
   protected static final int HAS_PARAMETRIC_MASTERS = 0x02;

   /**
    * Recursively determines whether the underlying master components
    * for an attachment are active, parametric, or both.
    */
   protected int findMasterDisposition (DynamicAttachment a, int disp) {
      for (DynamicComponent m : a.getMasters()) {
         if (m.isActive()) {
            disp |= HAS_ACTIVE_MASTERS;
         }
         else if (m.isParametric()) {
            disp |= HAS_PARAMETRIC_MASTERS;
         }
         else {
            if (!m.isAttached()) {
               throw new InternalErrorException (
                  "master component "+m+" for slave "+a.getSlave()+
                  " is neither active, parametric, or attached");
            }
            else {
               disp |= findMasterDisposition (m.getAttachment(), disp);
            }
         }
      }
      return disp;
   }
   
   protected void updateAttachmentLists() {
      LinkedList<DynamicAttachment> list =
         new LinkedList<DynamicAttachment>();
      getAttachments (list, 0);
      myAttachments = myAttachmentWorker.createOrderedList (list);
      Collections.reverse (myAttachments);
      myActiveAttachments = new ArrayList<DynamicAttachment>();
      myParametricAttachments = new ArrayList<DynamicAttachment>();
      // create lists of attachments controlled by active and parametric
      // components
      for (DynamicAttachment a : myAttachments) {
         int disp = findMasterDisposition (a, 0);
         if ((disp & HAS_ACTIVE_MASTERS) != 0) {
            myActiveAttachments.add (a);
         }
         if ((disp & HAS_PARAMETRIC_MASTERS) != 0) {
            myParametricAttachments.add (a);
         }
      }
   }
   
   protected ArrayList<DynamicAttachment> getAttachments() {
      if (myAttachments == null) {
         updateAttachmentLists();
      }
      return myAttachments;
   }

   protected ArrayList<DynamicAttachment> getActiveAttachments() {
      if (myActiveAttachments == null) {
         updateAttachmentLists();
      }
      return myActiveAttachments;
   }

   protected ArrayList<DynamicAttachment> getParametricAttachments() {
      if (myParametricAttachments == null) {
         updateAttachmentLists();
      }
      return myParametricAttachments;
   }

   public VectorNd getAttachmentDerivatives() {
      updateDynamicComponentLists();
      ArrayList<DynamicAttachment> attachments = getAttachments();
      int csize = 0;
      for (int k=0; k<attachments.size(); k++) {
         DynamicAttachment a = attachments.get(k);
         csize += myDynamicSizes[a.getSlave().getSolveIndex()];
      }
      VectorNd g = new VectorNd (csize);
      double[] gbuf = g.getBuffer();
      int goff = csize;
      for (int k=0; k<attachments.size(); k++) {
         DynamicAttachment a = attachments.get(k);
         int bj = attachments.size()-1-k;
         goff -= myDynamicSizes[a.getSlave().getSolveIndex()];
         a.getDerivative (gbuf, goff);
      }
      return g;
   }

   // Called from the top level
   public void addAttachmentJacobian (SparseNumberedBlockMatrix S, VectorNd f) {
      //System.out.println ("addAttachmentJacobian");
      updateDynamicComponentLists();
      //FunctionTimer timer = new FunctionTimer();
      //timer.start();
      boolean[] reduced = new boolean[S.numBlockRows()];
      int i = 0;
      for (DynamicAttachment a : getAttachments()) {
         myAttachmentWorker.addAttachmentJacobian (a, S, f, reduced);
         i++;
      }
      //timer.stop();
      //System.out.println ("addAttachmentJacobian " + timer.result(1));
   }
   
   // Called from the top level
   public void addAttachmentSolveBlocks (SparseNumberedBlockMatrix S) {
      boolean[] reduced = new boolean[S.numBlockRows()];
      for (DynamicAttachment a : getAttachments()) {
         myAttachmentWorker.addSolveBlocks (a, S, reduced);
      }
   }

   // Called from the top level
   public void updateAttachmentPos() {
      updateAttachmentPos (getAttachments());
   }
   
   protected void updateAttachmentPos(ArrayList<DynamicAttachment> alist) {
      for (int i=alist.size()-1; i>=0; i--) {
         alist.get(i).updatePosStates();
      }
   }
   
   // Called from the top level
   public void updateAttachmentVel() {
      updateAttachmentVel (getAttachments());
   }
   
   protected void updateAttachmentVel (ArrayList<DynamicAttachment> alist) {
      for (int i=alist.size()-1; i>=0; i--) {
         alist.get(i).updateVelStates();
      }
   }
 
   private void updateSlavePos() {
      updateSlaveObjectComponentList();
      for (int i=0; i<mySlaveObjectComponents.size(); i++) {
         mySlaveObjectComponents.get(i).updateSlavePos();
      }       
   }
   
   private void updateSlaveVel() {
      updateSlaveObjectComponentList();
      for (int i=0; i<mySlaveObjectComponents.size(); i++) {
         mySlaveObjectComponents.get(i).updateSlaveVel();
      }       
   }
   
   public void updatePosState() {
      updateAttachmentPos();
      updateSlavePos();
   }

   public void updateVelState() {
      updateAttachmentVel();
      updateSlaveVel();
   }

   // Called from the top level
   public void applyAttachmentForces() {
      for (DynamicAttachment a : getAttachments()) {
         a.applyForces();
      }
   }

   public boolean buildMassMatrix (SparseNumberedBlockMatrix M) {

      updateDynamicComponentLists();
      if (M.numBlockRows() != 0 || M.numBlockCols() != 0) {
         throw new IllegalArgumentException (
            "On entry, M should be empty with zero size");
      }
      M.addRows (myDynamicSizes, myDynamicSizes.length);
      M.addCols (myDynamicSizes, myDynamicSizes.length);
      boolean isConstant = true;
      int bi;
      for (int i=0; i<myDynamicComponents.size(); i++) {
         DynamicComponent c = myDynamicComponents.get(i);
         if ((bi = c.getSolveIndex()) != -1) {
            M.addBlock (bi, bi, c.createMassBlock());
            isConstant &= c.isMassConstant();
         }
      }
      addGeneralMassBlocks (M);
      // XXX add attachment solve blocks
      return isConstant;
   }

   private boolean checkMatrixSize (SparseBlockMatrix M) {
      return (M.numBlockRows() == myNumComponents && 
              M.numBlockCols() == myNumComponents &&
              M.rowSize() == mySystemSize &&
              M.colSize() == mySystemSize);
   }

   public void getMassMatrix (SparseNumberedBlockMatrix M, VectorNd f, double t) {
      updateDynamicComponentLists();
      if (!checkMatrixSize (M)) {
         throw new IllegalArgumentException (
            "M improperly sized; perhaps not created with buildMassMatrix()?");
      }
      if (f != null) {
         f.setSize (mySystemSize);
      } else {
         f = new VectorNd(mySystemSize); // XXX it seems some components require it to exist
      }
      //FunctionTimer timer = new FunctionTimer();
      for (int i=0; i<myDynamicComponents.size(); i++) {
         myDynamicComponents.get(i).resetEffectiveMass();
      }
      //timer.start();
      for (DynamicAttachment a : getAttachments()) {
         a.addMassToMasters ();
      }
      //timer.stop();
      //System.out.println ("add mass to masters " + timer.result(1));
      getMassMatrixValues (M, f, t);
      // TODO - need to fix this for non-block diagonal mass matrices:
      // for (DynamicAttachment a : getOrderedAttachments()) {
      //    a.reduceMass (M, f);
      // } 
   }

   public void getInverseMassMatrix (
      SparseBlockMatrix Minv, SparseBlockMatrix M) {
      updateDynamicComponentLists();
      if (!checkMatrixSize (Minv)) {
         throw new IllegalArgumentException (
            "Minv improperly sized; perhaps not created with buildMassMatrix()?");
      }         
      if (!checkMatrixSize (M)) {
         throw new IllegalArgumentException (
            "M improperly sized; perhaps not created with buildMassMatrix()?");
      }         
      for (int i=0; i<myNumActive; i++) {
         DynamicComponent c = myDynamicComponents.get(i);
         int bi;
         if ((bi = c.getSolveIndex()) != -1) {
            c.getInverseMass (Minv.getBlock (bi, bi), M.getBlock (bi, bi));
         }
      }
   }

   public void buildSolveMatrix (SparseNumberedBlockMatrix S) {
      updateDynamicComponentLists();
      
      if (S.numBlockRows() != 0 || S.numBlockCols() != 0) {
         throw new IllegalArgumentException (
            "On entry, S should be empty with zero size");
      }
      S.addRows (myDynamicSizes, myDynamicSizes.length);
      S.addCols (myDynamicSizes, myDynamicSizes.length);
      S.setVerticallyLinked (true);
      for (int i=0; i<myDynamicComponents.size(); i++) {
         DynamicComponent c = myDynamicComponents.get(i);
         if (c.getSolveIndex() != -1) {
            c.addSolveBlock (S);
         }
      }
      addGeneralMassBlocks (S);
      addGeneralSolveBlocks (S);
      addAttachmentSolveBlocks (S);
   }

   public static PosStabilization getDefaultStabilization () {
      return myDefaultStabilization;
   }

   public static void setDefaultStabilization (PosStabilization stablizer) {
      myDefaultStabilization = stablizer;
   }

   public PosStabilization getStabilization () {
      return mySolver.getStabilization();
   }

   public void setStabilization (PosStabilization stablizer) {
      mySolver.setStabilization (stablizer);
   }

   public void setUpdateForcesAtStepEnd (boolean enable) {
      myUpdateForcesAtStepEnd = enable;
      myUpdateForcesAtStepEndMode =
      PropertyUtils.propagateValue(
         this, "updateForcesAtStepEnd",
         myUpdateForcesAtStepEnd, myUpdateForcesAtStepEndMode);
      if (mySolver != null) {
         mySolver.setUpdateForcesAtStepEnd (enable);
      }
   }

   public boolean getUpdateForcesAtStepEnd() {
      return myUpdateForcesAtStepEnd;
   }

   public void setUpdateForcesAtStepEndMode (PropertyMode mode) {
      myUpdateForcesAtStepEndMode =
      PropertyUtils.setModeAndUpdate(
         this, "updateForcesAtStepEnd", myUpdateForcesAtStepEndMode, mode);
   }

   public PropertyMode getUpdateForcesAtStepEndMode() {
      return myUpdateForcesAtStepEndMode;
   }

   /** 
    * {@inheritDoc}
    */
   public int getParametricPosStateSize() {
      updateDynamicComponentLists();
      return myParametricPosStateSize;
   }
      
   /** 
    * {@inheritDoc}
    */
   public void getParametricPosTarget (VectorNd q, double s, double h) {
      updateDynamicComponentLists();
      q.setSize (myParametricPosStateSize);
      double[] buf = q.getBuffer();
      int idx = 0;
      for (int i=0; i<myNumParametric; i++) {
         idx = myParametricComponents.get(i).getTargetPos (buf, s, h, idx);
      }
   }
   
   /** 
    * {@inheritDoc}
    */
   public void getParametricPosState (VectorNd q) {
      updateDynamicComponentLists();
      q.setSize (myParametricPosStateSize);
      getParametricPosState (q, 0);
   }

   protected int getParametricPosState (VectorNd q, int idx) {
      double[] buf = q.getBuffer();
      for (int i=0; i<myNumParametric; i++) {
         idx = myParametricComponents.get(i).getPosState (buf, idx);
      }
      return idx;
   }      

   /** 
    * {@inheritDoc}
    */
   public void setParametricPosState (VectorNd q) {
      updateDynamicComponentLists();
      setParametricPosState (q, 0);
   }

   protected int setParametricPosState (VectorNd q, int idx) {
      double[] buf = q.getBuffer();
      for (int i=0; i<myNumParametric; i++) {
         idx = myParametricComponents.get(i).setPosState (buf, idx);
      }
      if (getParametricPosStateSize() > 0) {
         updateAttachmentPos (getParametricAttachments());
         updateSlavePos();
         updateAttachmentVel (getParametricAttachments()); // AVEL
         updateSlaveVel(); // AVEL
      }
      return idx;
   }      

   /** 
    * {@inheritDoc}
    */
   public int getParametricVelStateSize() {
      updateDynamicComponentLists();
      return myParametricVelStateSize;
   }
      
   /** 
    * {@inheritDoc}
    */
   public void getParametricVelTarget (VectorNd u, double s, double h) {
      updateDynamicComponentLists();
      u.setSize (myParametricVelStateSize);
      double[] buf = u.getBuffer();
      int idx = 0;
      for (int i=0; i<myNumParametric; i++) {
         idx = myParametricComponents.get(i).getTargetVel (buf, s, h, idx);
      }
   }
   
   /** 
    * {@inheritDoc}
    */
   public void getParametricVelState (VectorNd u) {
      updateDynamicComponentLists();
      u.setSize (myParametricVelStateSize);
      getParametricVelState (u, 0);
   }

   protected int getParametricVelState (VectorNd u, int idx) {
      double[] buf = u.getBuffer();
      for (int i=0; i<myNumParametric; i++) {
         idx = myParametricComponents.get(i).getVelState (buf, idx);
      }
      return idx;
   }      

   /** 
    * {@inheritDoc}
    */
   public void setParametricVelState (VectorNd q) {
      setParametricVelState (q, 0);
   }

   protected int setParametricVelState (VectorNd u, int idx) {
      updateDynamicComponentLists();
      double[] buf = u.getBuffer();
      for (int i=0; i<myNumParametric; i++) {
         idx = myParametricComponents.get(i).setVelState (buf, idx);
      }
      if (getParametricVelStateSize() > 0) {
         updateAttachmentVel (getParametricAttachments());
         updateSlaveVel();
      }
      return idx;
   }      

   protected VectorNd myParametricTarget = new VectorNd();

   public MechSystemBase copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      MechSystemBase msb = (MechSystemBase)super.copy (flags, copyMap);

      // Being overly thorough here since many of these variables will be
      // reset anyway by updateDynamicComponents()
      msb.myDynamicComponents = null;
      msb.myParametricComponents = null;
      msb.myAttachments = null;
      msb.myActiveAttachments = null;
      msb.myParametricAttachments = null;
      msb.myConstrainers = null;
      msb.myDynamicSizes = null;
      msb.myNumActive = 0;
      msb.myNumAttached = 0;
      msb.myNumParametric = 0;
      msb.myActiveVelStateSize = 0;
      msb.myActivePosStateSize = 0;
      msb.myParametricVelStateSize = 0;
      msb.myParametricPosStateSize = 0;
      msb.myProfilingP = myProfilingP;

      msb.setUpdateForcesAtStepEndMode (myUpdateForcesAtStepEndMode);
      if (myUpdateForcesAtStepEndMode == PropertyMode.Explicit) {
         msb.setUpdateForcesAtStepEnd (myUpdateForcesAtStepEnd);
      }      

      msb.myMassMatrix = null;

      //msb.myStabilization = myStabilization;
      msb.myDynamicsEnabled = myDynamicsEnabled;

      msb.allocateSolver (mySolver);
      //msb.myPosSolver = new KKTSolver();
      msb.myRg = new VectorNd(0);
      msb.myBg = new VectorNd(0);
      msb.myRn = new VectorNd(0);
      msb.myBn = new VectorNd(0);

      msb.setIntegrator (myIntegrator);
      msb.setMatrixSolver (myMatrixSolver);

      return msb;
   }

   public void printActiveStiffness() {
      printActiveStiffness ("%.6g");
   }

   public void printActiveStiffness (String fmtStr) {
      MatrixNd K = new MatrixNd (getActiveStiffnessMatrix());
      System.out.println ("K=\n" + K.toString (fmtStr));
   }

   public SparseBlockMatrix getActiveStiffnessMatrix () {
      updatePosState();
      return mySolver.createActiveStiffnessMatrix(1);
   }

   public void printActiveMass() {
      printActiveMass ("%.6g");
   }

   public void printActiveMass (String fmtStr) {
      MatrixNd M = new MatrixNd (getActiveMassMatrix());
      System.out.println ("M=\n" + M.toString (fmtStr));
   }

   public SparseBlockMatrix getActiveMassMatrix () {
      updatePosState();
      return mySolver.createActiveMassMatrix(0);
   }

   public void writeStiffnessMatrix (
      String fileName, double h, Matrix.WriteFormat matfmt) throws IOException {
      updatePosState();
      SparseBlockMatrix K = mySolver.createActiveStiffnessMatrix(h);
      PrintWriter pw = ArtisynthIO.newIndentingPrintWriter (fileName);
      int size = getActiveVelStateSize();
      NumberFormat fmt = new NumberFormat ("%g");
      K.write (pw, fmt, matfmt, size, size);
      pw.close();
   }

   public void writeStiffnessMatrix (
      String fileName, double h) throws IOException {
      writeStiffnessMatrix (fileName, h, Matrix.WriteFormat.MatrixMarket);
   }                                        

   public void writeMassMatrix (String fileName, Matrix.WriteFormat matfmt)
      throws IOException {      
      updatePosState();
      PrintWriter pw = ArtisynthIO.newIndentingPrintWriter (fileName);
      SparseBlockMatrix M = mySolver.createActiveMassMatrix (0);
      NumberFormat fmt = new NumberFormat ("%g");
      M.write (pw, fmt, matfmt);
      pw.close();
   }

   public void writeMassMatrix (String fileName)
      throws IOException {
      writeMassMatrix (fileName, Matrix.WriteFormat.MatrixMarket);
   }

   public void writeBilateralConstraintMatrix (
      String fileName, Matrix.WriteFormat matfmt) throws IOException {
      PrintWriter pw = ArtisynthIO.newIndentingPrintWriter (fileName);
      SparseBlockMatrix GT = mySolver.createActiveBilateralMatrix (0);
      NumberFormat fmt = new NumberFormat ("%g");
      GT.write (pw, fmt, matfmt);
      pw.close();
   }

   public void writeBilateralConstraintMatrix (String fileName)
      throws IOException {
      writeBilateralConstraintMatrix (fileName, Matrix.WriteFormat.MatrixMarket);
   }

   public void collectInitialForces () {
      //FunctionTimer timer = new FunctionTimer();
      //timer.start();
      updateDynamicComponentLists();
      updateForceComponentList();
      // add external forces
      for (int i=0; i<myDynamicComponents.size(); i++) {
         myDynamicComponents.get(i).applyExternalForces();
      }
      //timer.stop();
      getForces (myInitialForces);

      //System.out.println ("  collect " + timer.result(1));
   }

   public void updateForces (double t) {
      updateDynamicComponentLists();
      updateForceComponentList();
      // initialize the forces by adding the initial forces that 
      // were collected at the beginning of the advance. 
      // We only do this if updateForces is being called from *within*
      // the advance method, since otherwise myInitialForces won't be properly
      // set.
      if (myInsideAdvanceP) {
         setForces (myInitialForces);
      }
      else {
         // we zero forces, then apply external forces. This is done
         // in two passes since applying external forces to component A
         // may cause forces to be applied to other components to which
         // A is implicitly attached (such as frames associated with points).
         for (int i=0; i<myDynamicComponents.size(); i++) {
            myDynamicComponents.get(i).zeroForces();
         }
         for (int i=0; i<myDynamicComponents.size(); i++) {
            myDynamicComponents.get(i).applyExternalForces();
         }
      }
      for (int i=0; i<myForceEffectors.size(); i++) {
         myForceEffectors.get(i).applyForces (t);
      }
      applyAttachmentForces();
   }

   public void addPosJacobian (
      SparseNumberedBlockMatrix S, VectorNd f, double s) {
      updateDynamicComponentLists();
      updateForceComponentList();
      if (!checkMatrixSize (S)) {
         throw new IllegalArgumentException (
            "S improperly sized; perhaps not created with buildSolveMatrix()?");
      }
      if (f != null) {
         f.setSize (mySystemSize);
         f.setZero();
      }
      for (int i=0; i<myForceEffectors.size(); i++) {
         myForceEffectors.get(i).addPosJacobian (S, s);
      }
      addAttachmentJacobian(S, f);
   }   

   public void addVelJacobian (
      SparseNumberedBlockMatrix S, VectorNd f, double s) {
      updateDynamicComponentLists();
      updateForceComponentList();
      if (!checkMatrixSize (S)) {
         throw new IllegalArgumentException (
            "S improperly sized; perhaps not created with buildSolveMatrix()?");
      }
      if (f != null) {
         f.setSize (mySystemSize);
         f.setZero();
      }
      for (int i=0; i<myForceEffectors.size(); i++) {
         myForceEffectors.get(i).addVelJacobian (S, s);
      }
      addAttachmentJacobian(S, f);
   }    

   public void addGeneralMassBlocks (SparseNumberedBlockMatrix M) {
      // do nothing if mass matrix is block diagonal
   }

   public void addGeneralSolveBlocks (SparseNumberedBlockMatrix M) {
      updateForceComponentList();
      int numb = M.numBlocks();
      for (int i=0; i<myForceEffectors.size(); i++) {
         myForceEffectors.get(i).addSolveBlocks (M);
      }
   }

   public double getActiveMass() {
      double m = 0;
      updateDynamicComponentLists();      
      // restrict mass to active components, esp. since mass for parametric
      // components maybe arbitrary
      for (int i=0; i<myNumActive; i++) {
         m += myDynamicComponents.get(i).getMass(0);
      }
      return m;
   }

   public int getSolveMatrixType() {
      updateForceComponentList();
      int type = Matrix.SPD;
      for (int i=0; i<myForceEffectors.size(); i++) {
         type &= myForceEffectors.get(i).getJacobianType();
      }
      return type;
   }

   protected <T> void recursivelyGetLocalComponents (
      CompositeComponent comp, List<T> list, Class<T> type) {

      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (!(c instanceof MechSystemModel)) {
            if (type.isAssignableFrom(c.getClass())) {
               T t = type.cast(c);      // checked cast
               list.add (t);
            }
            // sometimes a component can be a of type T
            // with sub-components also of type T
            if (c instanceof CompositeComponent) {
               recursivelyGetLocalComponents ((CompositeComponent)c, list, type);
            }
         }
      }
   }

   /**
    * Like recursivelyGetLocalComponents, but don't descend into components of
    * type T
    */
   protected <T> void recursivelyGetTopLocalComponents (
      CompositeComponent comp, List<T> list, Class<T> type) {

      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (!(c instanceof MechSystemModel)) {
            if (type.isAssignableFrom(c.getClass())) {
               T t = type.cast(c);      // checked cast
               list.add (t);
            }
            else if (c instanceof CompositeComponent) {
               recursivelyGetLocalComponents ((CompositeComponent)c, list, type);
            }
         }
      }
   }

   private static MechSystemBase getTopMechSystem (ModelComponent comp) {
      MechSystemBase sys = null;
      CompositeComponent cc;
      for (cc=comp.getParent(); cc!=null; cc=cc.getParent()) {
         if (cc instanceof MechSystemBase) {
            sys = (MechSystemBase)cc;
         }
      }
      return sys;
   }

   static class UpdateAttachmentsAction implements TransformGeometryAction {

      public void transformGeometry (
         GeometryTransformer gtr, TransformGeometryContext context, int flags) {

         // Do an updatePosState() on all top-most MechSystems, if any are
         // found.
         HashSet<MechSystemBase> systems = new HashSet<MechSystemBase>();
         for (TransformableGeometry tg : context.getTransformables()) {
            if (tg instanceof ModelComponent) {
               MechSystemBase sys = getTopMechSystem ((ModelComponent)tg);
               if (sys != null) {
                  systems.add (sys);
               }
            }
         }
         for (MechSystemBase sys : systems) {
            sys.updateAttachmentPos();
         }
      }
   }
   
   static UpdateAttachmentsAction myAttachmentsPosAction = 
      new UpdateAttachmentsAction();
}
