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
import maspack.matrix.NumericalException;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderableUtils;
import maspack.util.DataBuffer;
import maspack.util.IntHolder;
import maspack.util.NumberFormat;
import artisynth.core.mechmodels.MechSystemSolver.PosStabilization;
import artisynth.core.modelbase.*;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.TimeBase;

public abstract class MechSystemBase extends RenderableModelBase
   implements MechSystemModel {

   public static boolean myParametricsInSystemMatrix = true;
   //public static boolean myZeroForcesInPreadvance = true;

   protected int myStructureVersion = 0;

   // last time step associated with bilateral impulses. Initialize to 1
   // to ensure constrainer.setBilateralImpulses is never called with h = 0
   protected double myLastBilateralH = 1;
   // last time step associated with unilateral impulses. Initialize to 1
   // to ensure constrainer.setUnilateralImpulses is never called with h = 0
   protected double myLastUnilateralH = 1;
   
   protected ArrayList<DynamicComponent> myDynamicComponents;
   protected ArrayList<MotionTargetComponent> myParametricComponents;
   protected ArrayList<DynamicAttachment> myOrderedAttachments;
   protected ArrayList<Constrainer> myConstrainers;
   protected ArrayList<ForceEffector> myForceEffectors;
   protected ArrayList<HasAuxState> myAuxStateComponents;
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
   protected boolean myInsideAdvanceP = false;
   protected long mySolveTime;
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
//   private int myMassConstraintCnt = 0;
//   private int myGTConstraintCnt = 0;

   public static PropertyList myProps =
      new PropertyList (MechModel.class, RenderableModelBase.class);

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
      }
   }

   public MechSystemBase (String name) {
      super (name);
      allocateSolver (/*oldSolver=*/null);
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
   
   public boolean hasParameterizedType() {
      return false;
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
      if (getOrderedAttachments().size() > 0 && !J.isVerticallyLinked()) {
	 J.setVerticallyLinked(true);
      }
      for (DynamicAttachment a : getOrderedAttachments()) {
         a.reduceRowMatrix (J);
      }
   }

   public int getNumUnilateralImpulses () {
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
      for (DynamicAttachment a : getOrderedAttachments()) {
         a.reduceConstraints (NT, dn);
      }
      // need this for now - would be good to get rid of it:
      NT.setVerticallyLinked (true);
   }

   public int getNumBilateralImpulses () {
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

   public int getBilateralConstraints (SparseBlockMatrix GT, VectorNd dg) {

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
      
      for (DynamicAttachment a : getOrderedAttachments()) {
         a.reduceConstraints (GT, dg);
      }
      // need this for now - would be good to get rid of it:
      GT.setVerticallyLinked (true);
      return 0;
   }

   public void getBilateralInfo (ConstraintInfo[] ginfo) {
      updateForceComponentList();
      int idx = 0;
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).getBilateralInfo (ginfo, idx);
      }
   }

   public void setBilateralImpulses (VectorNd lam, double h) {
      setBilateralImpulses (lam, h, 0);
   }         

   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      updateForceComponentList();
      myLastBilateralH = h;
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).setBilateralImpulses (lam, h, idx);
      }
      return idx;
   }

   public void getBilateralImpulses (VectorNd lam) {
      updateForceComponentList();
      getBilateralImpulses (lam, 0);
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).getBilateralImpulses (lam, idx);
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

   public void setUnilateralImpulses (VectorNd the, double h) {
      setUnilateralImpulses (the, h, 0);
   }         

   public int setUnilateralImpulses (VectorNd the, double h, int idx) {
      updateForceComponentList();
      myLastUnilateralH = h;
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).setUnilateralImpulses (the, h, idx);
      }
      return idx;
   }

   public void getUnilateralImpulses (VectorNd the) {
      updateForceComponentList();
      getUnilateralImpulses (the, 0);
   }         

   public int getUnilateralImpulses (VectorNd the, int idx) {
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).getUnilateralImpulses (the, idx);
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
      for (DynamicAttachment a : getOrderedAttachments()) {
         a.reduceConstraints (DT, null);
      }
   }
   
   public void updateConstraints (
      double t, StepAdjustment stepAdjust, int flags) {

      updateForceComponentList();
      double maxpen = 0;
      for (int i=0; i<myConstrainers.size(); i++) {
         double pen = myConstrainers.get(i).updateConstraints (t, flags);
         if (pen > maxpen) {
            maxpen = pen;
         }
      } 
      double penlimit = getPenetrationLimit();
      if (penlimit > 0 && maxpen > penlimit) {
         stepAdjust.recommendAdjustment (
            0.5 /*penlimit/maxpen*/, "contact penetration exceeds "+penlimit);
      }
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
    */
   public void getDynamicSizes (int[] sizes) {
      updateDynamicComponentLists();
      int max = Math.min (sizes.length, myDynamicSizes.length);
      for (int i=0; i<max; i++) {
         sizes[i] = myDynamicSizes[i];
      }
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
         getDynamicComponents (active, attached, parametric);
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
         ArrayList<HasAuxState> list = new ArrayList<HasAuxState>();
         getAuxStateComponents (list, 0);
         myAuxStateComponents = list;
      }
   }

   /**
    * Should be overridden in subclasses to return all the HasSlaveObjects '
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
      updatePosState();
      updateVelState();
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
      updateVelState();
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

   public synchronized PrintWriter openPrintStateFile (String name)
      throws IOException {
      if (myPrintStateWriter != null) {
         myPrintStateWriter.close();
      }
      myPrintStateWriter = new PrintWriter (
         new BufferedWriter (new FileWriter (name)));
      return myPrintStateWriter;
   }

   public synchronized PrintWriter reopenPrintStateFile (String name)
      throws IOException {
      if (myPrintStateWriter != null) {
         myPrintStateWriter.close();
      }
      myPrintStateWriter = new PrintWriter (
         new BufferedWriter (new FileWriter (name, /*append=*/true)));
      return myPrintStateWriter;
   }

   public synchronized void closePrintStateFile () throws IOException {
      if (myPrintStateWriter != null) {
         myPrintStateWriter.close();
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

   private void checkState() {
//      updateDynamicComponents();
//      RootModel root = Main.getRootModel();
//      if (root.isCheckEnabled()) {
//         VectorNd x = new VectorNd (myActivePosStateSize);
//         VectorNd v = new VectorNd (myActiveVelStateSize);
//         getActivePosState (x, 0);
//         getActiveVelState (v, 0);
//         root.checkWrite ("v: " + v.toString ("%g"));
//         root.checkWrite ("x: " + x.toString ("%g"));
//      }
   }

   
   void advanceAuxState (double t0, double t1) {
      updateAuxStateComponentList();
      for (int i=0; i<myAuxStateComponents.size(); i++) {
         myAuxStateComponents.get(i).advanceAuxState (t0, t1);
      }
   }      

   public StepAdjustment preadvance (double t0, double t1, int flags) {
      advanceAuxState (t0, t1);
      // zero forces
      updateDynamicComponentLists();
      for (int i=0; i<myDynamicComponents.size(); i++) {
         myDynamicComponents.get(i).zeroForces();
      }
      return null;
   }

   public StepAdjustment advance (double t0, double t1, int flags) {

      myInsideAdvanceP = true;
      if (myProfilingP) {
         mySolveTime = System.nanoTime();
      }      
      StepAdjustment stepAdjust = new StepAdjustment();
      collectInitialForces();

      if (t0 == 0) {
         updateForces (t0);
      }

      if (!myDynamicsEnabled) {
         mySolver.nonDynamicSolve (t0, t1, stepAdjust);
         recursivelyFinalizeAdvance (null, t0, t1, flags, 0);
      }
      else {
         if (t0 == 0 && myPrintState != null) {
            printState (myPrintState, 0);
         }
         checkState();
         mySolver.solve (t0, t1, stepAdjust);
         DynamicComponent c = checkVelocityStability();
         if (c != null) {
            throw new NumericalException (
               "Unstable velocity detected, component " +
               ComponentUtils.getPathName (c));
         }
         recursivelyFinalizeAdvance (stepAdjust, t0, t1, flags, 0);
         if (myPrintState != null) {
            printState (myPrintState, t1);
         }
      }

      if (myProfilingP) {
         mySolveTime = System.nanoTime() - mySolveTime;
         System.out.println (
            "T1=" + t1 + " solveTime=" + mySolveTime/1e6);
      }
      myInsideAdvanceP = false;
      return stepAdjust;
   }

//   public void increaseAuxStateOffsets (DataBuffer data) {
//      updateAuxStateComponentList();
//      for (int i=0; i<myAuxStateComponents.size(); i++) {
//         myAuxStateComponents.get(i).increaseAuxStateOffsets (
//            data, StateContext.CURRENT);
//      }
//   }

   public void getAuxState (DataBuffer data) {
      updateAuxStateComponentList();
      for (int i=0; i<myAuxStateComponents.size(); i++) {
         myAuxStateComponents.get(i).getAuxState (data);
      }
   }

   public void setAuxState (DataBuffer data) {
      updateAuxStateComponentList();
      for (int i=0; i<myAuxStateComponents.size(); i++) {
         myAuxStateComponents.get(i).setAuxState (data);
      }
   }

   public ComponentState createState (ComponentState prevState) {
      if (prevState instanceof NumericState) {
         NumericState last = (NumericState)prevState;
         // use old state to set capacity. Make the capacity a 10%
         // bigger, just in case
         int dcap = (int)(1.1*last.dsize());
         int zcap = (int)(1.1*last.zsize());
         return new NumericState (dcap, zcap, 0);
       }
      else {
         return new NumericState (1000, 1000, 0);
      }
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

   private void checkState (ComponentState state, String name) {
      if (!(state instanceof NumericState)) {
         throw new IllegalArgumentException (name+" not a NumericState");
      }
      NumericState nstate = (NumericState)state;
      if (nstate.osize() == 0) {
         throw new IllegalArgumentException (name+" does not contain components");
      }
      if (nstate.zpeek (0) != 0x1234) {
         throw new IllegalArgumentException (
            "checksum for "+name+" is "+nstate.zpeek(0)+", expecting "+0x1234);
      }
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
      int numConstrainers = state.zget();

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
      if (numConstrainers != -1 && numConstrainers != myConstrainers.size()) {
         throw new IllegalArgumentException (
            "number of constrainers is "+numConstrainers+
            ", expecting "+myConstrainers.size());
      }

      int di = 0;
      double[] dbuf = state.dbuffer();
      for (int i=0; i<myNumActive+myNumParametric; i++) {
         DynamicComponent c = myDynamicComponents.get(i);
         int size = c.getPosStateSize() + c.getVelStateSize();
         // XXX should check size here
         di = c.setPosState (dbuf, di);
         di = c.setVelState (dbuf, di);
      }      
      updatePosState();
      updateVelState();
      state.dskip (di);

      // setting aux state must be done here because it may change the number
      // of bilateral and unilateral impulses expected by the constrainers
      setAuxState (state);

      // Hack to make sure Andrew Ho's earlier version way point data still
      // reads.  In that earlier version, numConstrainers == 0 meant that
      // myLastBilateralH and myLastUnilateralH were not stored. 
      if (state.doffset() == state.dsize() && numConstrainers == 0) {
         numConstrainers = -1;
      }

      // numConstrainers == -1 indicates initial state
      if (numConstrainers == -1) {
         myLastBilateralH = 1;
         myLastUnilateralH = 1;
         for (int i=0; i<myConstrainers.size(); i++) {
            myConstrainers.get(i).zeroImpulses();
         }
      }
      else {
         myLastBilateralH = state.dget();
         myLastUnilateralH = state.dget();
         // create special vector to access the state ...
         VectorNd dvec = new VectorNd();
         dvec.setBuffer (state.dsize(), state.dbuffer());
         di = state.doffset();
         for (int i=0; i<myConstrainers.size(); i++) {
            Constrainer c = myConstrainers.get(i);
            di = c.setBilateralImpulses (dvec, myLastBilateralH, di);
            di = c.setUnilateralImpulses (dvec, myLastUnilateralH, di);
         }
         state.dsetOffset (di);
      }

   }

   // NEWX
   public void getState (ComponentState pstate) {
      long t0 = System.nanoTime();

      if (!(pstate instanceof NumericState)) {
         throw new IllegalArgumentException ("pstate not a NumericState");
      }
      NumericState state = (NumericState)pstate;
      state.clear();

      // get the required sizes
      updateAuxStateComponentList();
      updateDynamicComponentLists();
      updateForceComponentList();


      int numb = getNumBilateralImpulses();
      int numu = getNumUnilateralImpulses();

      state.zput (0x1234);
      state.zput (myNumActive+myNumParametric);
      state.zput (myAuxStateComponents.size());
      state.zput (myConstrainers.size());

      for (int i=0; i<myNumActive+myNumParametric; i++) {
         DynamicComponent c = myDynamicComponents.get(i);
         int size = c.getVelStateSize()+c.getPosStateSize();
         int di = state.dsize();
         state.dsetSize (di+size);
         double[] dbuf = state.dbuffer();
         di = c.getPosState (dbuf, di);
         di = c.getVelState (dbuf, di);
      }      
      getAuxState (state);

      state.dput (myLastBilateralH);
      state.dput (myLastUnilateralH);
      int di = state.dsize();
      state.dsetSize (di+numb+numu);
      // create special vector to access the state ...
      VectorNd dvec = new VectorNd();
      dvec.setBuffer (state.dsize(), state.dbuffer());
      for (int i=0; i<myConstrainers.size(); i++) {
         Constrainer c = myConstrainers.get(i);
         di = c.getBilateralImpulses (dvec, di);
         di = c.getUnilateralImpulses (dvec, di);
      }

      long t1 = System.nanoTime();
      //System.out.println ("getState=" + (t1-t0)*1e-6 + "msec");
   }
 
   public void getInitialState (
      ComponentState newstate, ComponentState oldstate) {

      if (!(newstate instanceof NumericState)) {
         throw new IllegalArgumentException ("newstate not a NumericState");
      }
      NumericState nstate = (NumericState)newstate;
      nstate.clear();

      updateAuxStateComponentList();
      updateDynamicComponentLists();
      updateForceComponentList();

      HashMap<DynamicComponent,DataBuffer> dynCompMap =
         new HashMap<DynamicComponent,DataBuffer>();

      HashMap<HasAuxState,DataBuffer> auxCompMap =
         new HashMap<HasAuxState,DataBuffer>();

      if (oldstate != null) {
         if (!(oldstate instanceof NumericState)) {
            throw new IllegalArgumentException ("oldstate not a NumericState");
         }
         NumericState ostate = (NumericState)oldstate;
         ostate.resetOffsets();

         int chk = ostate.zget();
         if (chk != 0x1234) {
            throw new IllegalArgumentException (
               "oldstate checksum is "+chk+", expecting "+0x1234);
         }
         int numOldDynComps = ostate.zget();
         int numOldAuxStateComps = ostate.zget();
         int numOldConstrainers = ostate.zget();

         for (int i=0; i<numOldDynComps; i++) {
            DynamicComponent c = (DynamicComponent)ostate.oget();
            DataBuffer data = new DataBuffer ();
            data.setBuffersAndOffsets (ostate);
            dynCompMap.put (c, data);           
            ostate.dskip (c.getPosStateSize()+c.getVelStateSize());
         }

         for (int i=0; i<numOldAuxStateComps; i++) {
            HasAuxState c = (HasAuxState)ostate.oget();
            DataBuffer data = new DataBuffer ();
            data.setBuffersAndOffsets (ostate);
            auxCompMap.put (c, data);
            c.skipAuxState (ostate);
         }
      }
      
//      // add object information: active and parametric components,
//      // aux state components, and constrainers
//      for (int i=0; i<myNumActive+myNumParametric; i++) {
//         nstate.oput (myDynamicComponents.get(i));
//      }      
//      nstate.oputs (myAuxStateComponents);
      
      nstate.zput (0x1234);
      nstate.zput (myNumActive+myNumParametric);
      nstate.zput (myAuxStateComponents.size());
      // specify -1 constrainers, to cause impulses to be zeroed
      nstate.zput (-1);

      //double[] dbufNew = nstate.dbuffer();
      for (int i=0; i<myNumActive+myNumParametric; i++) {
         DynamicComponent c = myDynamicComponents.get(i);
         nstate.oput (c);
         DataBuffer data = dynCompMap.get (c);
         int size = c.getVelStateSize()+c.getPosStateSize();
         if (data != null) {
            nstate.putData (data, size, 0);
         }
         else {
            int di = nstate.dsize();
            nstate.dsetSize (di + size);
            double[] dbuf = nstate.dbuffer();
            di = c.getPosState (dbuf, di);
            di = c.getVelState (dbuf, di);
         }
      }

      for (HasAuxState c : myAuxStateComponents) {
         nstate.oput (c);
         c.getInitialAuxState (nstate, auxCompMap.get (c));
      }
   }

   public void initialize (double t) {
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
            for (int i=0; i<myNumParametric; i++) {
               myParametricComponents.get(i).resetTargets();
            }
         }
         updateForces (t);
      }
   }

   public void recursivelyFinalizeAdvance (
      StepAdjustment stepAdjust, double t0, double t1, int flags, int level) {
   }  

   public void setProfiling (boolean enable) {
      myProfilingP = enable;
   }

   public boolean getProfiling() {
      return myProfilingP;
   }
   
   protected void clearCachedData (ComponentChangeEvent e) {
      myDynamicComponents = null;
      myOrderedAttachments = null;
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
    * Returns the transpose of the attachment constraint matrix, along with the
    * vector of derivative offsets. This is used only for debugging.
    */
   public SparseBlockMatrix getAttachmentConstraints () {
      updateDynamicComponentLists();
      // figure out the column sizes
      ArrayList<DynamicAttachment> attachments = getOrderedAttachments();
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
            a.mulSubGT (mblk, sblk, i);
            mblk.negate();
            GT.addBlock (bi, bj, mblk);
         }
      }
      return GT;
   }

   protected ArrayList<DynamicAttachment> getOrderedAttachments() {
      if (myOrderedAttachments == null) {
         LinkedList<DynamicAttachment> list =
            new LinkedList<DynamicAttachment>();
         getAttachments (list, 0);
         myOrderedAttachments = DynamicAttachment.createOrderedList (list);
         Collections.reverse (myOrderedAttachments);
      }
      return myOrderedAttachments;
   }

   public VectorNd getAttachmentDerivatives() {
      updateDynamicComponentLists();
      ArrayList<DynamicAttachment> attachments = getOrderedAttachments();
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

      boolean[] reduced = new boolean[S.numBlockRows()];
      for (DynamicAttachment a : getOrderedAttachments()) {
         a.addAttachmentJacobian (S, f, reduced);
      }
   }

   // Called from the top level
   public void addAttachmentSolveBlocks (SparseNumberedBlockMatrix S) {
      boolean[] reduced = new boolean[S.numBlockRows()];
      for (DynamicAttachment a : getOrderedAttachments()) {
         a.addSolveBlocks (S, reduced);
      }
   }

   // Called from the top level
   public void updateAttachmentPos() {
      ArrayList<DynamicAttachment> list = getOrderedAttachments();
      for (int i=list.size()-1; i>=0; i--) {
         list.get(i).updatePosStates();
      }
   }

   public void updatePosState() {
      //recursivelyUpdatePosState (0);
      updateAttachmentPos();
      updateSlaveObjectComponentList();
      for (int i=0; i<mySlaveObjectComponents.size(); i++) {
         mySlaveObjectComponents.get(i).updateSlavePos();
      } 
   }

   public void updateVelState() {
      updateAttachmentVel();
      updateSlaveObjectComponentList();
      for (int i=0; i<mySlaveObjectComponents.size(); i++) {
         mySlaveObjectComponents.get(i).updateSlaveVel();
      }
   }

   // Called from the top level
   public void updateAttachmentVel() {
      ArrayList<DynamicAttachment> list = getOrderedAttachments();
      for (int i=list.size()-1; i>=0; i--) {
         list.get(i).updateVelStates();
      }
   }

   // Called from the top level
   public void applyAttachmentForces() {
      for (DynamicAttachment a : getOrderedAttachments()) {
         a.applyForces();
      }
   }

   public boolean buildMassMatrix (SparseBlockMatrix M) {

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

   public void getMassMatrix (SparseBlockMatrix M, VectorNd f, double t) {
      updateDynamicComponentLists();
      if (!checkMatrixSize (M)) {
         throw new IllegalArgumentException (
            "M improperly sized; perhaps not created with buildMassMatrix()?");
      }
      if (f != null) {
         f.setSize (mySystemSize);
      }           
      for (int i=0; i<myDynamicComponents.size(); i++) {
         myDynamicComponents.get(i).resetEffectiveMass();
      }      
      for (DynamicAttachment a : getOrderedAttachments()) {
         a.addMassToMasters ();
      }
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
         // inefficient - should limit to parametric attachments
         updatePosState();
         updateVelState();
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
         // inefficient - should limit to parametric attachments
         updateVelState();
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
      msb.myOrderedAttachments = null;
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

      return msb;
   }

   public void printActiveStiffness ()
      throws IOException {
      MatrixNd K = new MatrixNd (getActiveStiffness());
      System.out.println ("K=\n" + K.toString ("%8.3f"));
   }

   public SparseBlockMatrix getActiveStiffness () {
      updatePosState();
      return mySolver.createActiveStiffnessMatrix(1);
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
      updateDynamicComponentLists();
      updateForceComponentList();
      // add external forces
      for (int i=0; i<myDynamicComponents.size(); i++) {
         myDynamicComponents.get(i).applyExternalForces();
      }
      getForces (myInitialForces);
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

   public void addGeneralMassBlocks (SparseBlockMatrix M) {
      // do nothing if mass matrix is block diagonal
   }

   public void addGeneralSolveBlocks (SparseNumberedBlockMatrix M) {
      updateForceComponentList();
      for (int i=0; i<myForceEffectors.size(); i++) {
         myForceEffectors.get(i).addSolveBlocks (M);
      }
//      addAttachmentSolveBlocks(M);
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
