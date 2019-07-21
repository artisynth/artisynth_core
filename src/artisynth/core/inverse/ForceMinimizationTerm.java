/**
 * Copyright (c) 2019, by the Authors: John E. Lloyd, Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.util.ArrayList;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.MinimizableForceComponent;
import artisynth.core.mechmodels.Point;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.spatialmotion.Twist;

/**
 * Force minimization term for the TrackingController
 * 
 * @author John E. Lloyd, Ian Stavness, Antonio Sanchez
 */
public class ForceMinimizationTerm extends LeastSquaresTermBase {

   public static final double DEFAULT_WEIGHT = 1d;
   
   public static final boolean DEFAULT_DELTA_ACTIVATIONS = false;
   protected boolean useDeltaAct = DEFAULT_DELTA_ACTIVATIONS;
   
   public static boolean DEFAULT_USE_KKT_FACTORANDSOLVE = false;
   protected boolean useKKTFactorAndSolve = DEFAULT_USE_KKT_FACTORANDSOLVE;

   public static boolean DEFAULT_USE_TRAPEZOIDAL_SOLVER = false;
   protected boolean useTrapezoidalSolver = DEFAULT_USE_TRAPEZOIDAL_SOLVER;

   public static boolean DEFAULT_NORMALIZE_H = false;
   protected boolean normalizeH = DEFAULT_NORMALIZE_H;
   
   boolean debug = false;
   public boolean debugHf = false;   

   protected TrackingController myController;
   protected MechSystemBase myMech;    // mech system, used to compute forces
//   protected MotionTerm myMotionTerm;

   private class ForceCompInfo {
      MinimizableForceComponent myComp;
      VectorNd myWeights;
      boolean myStaticOnly;

      ForceCompInfo (
         MinimizableForceComponent comp, boolean staticOnly) {
         myComp = comp;
         myStaticOnly = staticOnly;
      }
      
      void setWeights (double weight) {
         myWeights = new VectorNd(myComp.getMinForceSize());
         for (int i=0; i<myWeights.size(); i++) {
            myWeights.set (i, weight);
         }
      }
      
      void setWeights (VectorNd weights) {
         myWeights = new VectorNd(myComp.getMinForceSize());
         for (int i=0; i<myWeights.size(); i++) {
            myWeights.set (i, weights.get(i));
         }
      }
   }

   protected ArrayList<ForceCompInfo> myForceComps;

   protected SparseBlockMatrix myJacobian = null;
   protected int myTotalForceSize;
   protected int myTargetPosSize;
   // size of myTotalForceSize, weights for system
   protected VectorNd myTargetWgts = null;   

   protected VectorNd myCurrentVel = null;
   

   public static PropertyList myProps =
      new PropertyList(ForceMinimizationTerm.class, LeastSquaresTermBase.class);

   static {
      myProps.add("useKKTFactorAndSolve", "flag for re-factoring at each internal KKT solve", 
         DEFAULT_USE_KKT_FACTORANDSOLVE);
      myProps.add(
         "normalizeH", "normalize contribution by frobenius norm",
         DEFAULT_NORMALIZE_H);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public ForceMinimizationTerm (TrackingController controller) {
      this(controller, DEFAULT_WEIGHT);
   }
   
   public ForceMinimizationTerm (TrackingController controller, double weight) {
      super(weight);
      myController = controller;
      myMech = myController.getMech();
//      if (useDeltaAct)
//         myMotionTerm = new MotionTermDeltaAct (controller);
//      else
//         myMotionTerm = new MotionTerm(myController);
         
      myForceComps = new ArrayList<ForceCompInfo>();
   }
   
   public VectorNd myTotalForce = new VectorNd();

   public void getTotalForce (VectorNd totalForce) {
      int idx = 0;
      VectorNd minf = new VectorNd();
      for (ForceCompInfo finfo : myForceComps) {
         int fsize = finfo.myComp.getMinForceSize();
         minf.setSize (fsize);
         finfo.myComp.getMinForce (minf, finfo.myStaticOnly);
         totalForce.setSubVector (idx, minf);
         idx += fsize;
      }
   }

   public void updateTotalForce() {

      if (myTotalForce == null || myTotalForce.size() != myTotalForceSize) {
         myTotalForce = new VectorNd(myTotalForceSize);
      }
      getTotalForce (myTotalForce);
   }

   VectorNd prevTargetPos = null;
   VectorNd diffTargetPos = null;
   Frame tmpFrame = new Frame ();
   Point tmpPoint = new Point ();

   double[] tmpBuf = new double[3];

   /**
    * Adds a force component to the term for force minimization
    * @param fcomp
    * @param weight
    */
   private ForceCompInfo doAddForce (
      MinimizableForceComponent fcomp, boolean staticOnly) {

      ForceCompInfo finfo = new ForceCompInfo (fcomp, staticOnly);
      myForceComps.add (finfo);
      myTotalForceSize += fcomp.getMinForceSize();

      // set target matrix null, so that it is recreated on demand
      // XXX should be updated on a change event...
      myJacobian = null;
      return finfo;
   }
   
   /**
    * Removes a target to the term for trajectory error
    * @param source
    */
   protected void removeForce (MinimizableForceComponent fcomp) {

      int idx = -1;
      for (int k=0; k<myForceComps.size(); k++) {
         if (myForceComps.get(k).myComp == fcomp) {
            idx = k;
            break;
         }
      }
      if (idx == -1) {
         return;
      }
      
      myForceComps.remove (idx);
      myTotalForceSize -= fcomp.getMinForceSize();
      updateWeightsVector();

      // set target matrix null, so that it is recreated on demand
      // XXX should be updated on a change event...
      myJacobian = null;

      // Main.getMain().getInverseManager().configureTargetProbes();
      // mySolver.resetVariables();
   }

   private void updateWeightsVector() {
      
      myTargetWgts = new VectorNd(myTotalForceSize);
      
      int idx = 0;
      for (ForceCompInfo finfo : myForceComps) {
         for (int k=0; k<finfo.myComp.getMinForceSize(); k++) {
            myTargetWgts.set(idx++, finfo.myWeights.get(k));
         }
      }
   }
   
   /**
    * Adds a force component whose force should be minimized
    * @param fcomp force component 
    * @param weight used in the minimization
    * @param staticOnly {@code true} if only static forces should be minimized
    */
   public void addForce (
      MinimizableForceComponent fcomp, double weight, boolean staticOnly) {
      ForceCompInfo finfo = doAddForce (fcomp, staticOnly);
      finfo.setWeights (weight);
      updateWeightsVector();
   }
   
   /**
    * Adds a force component whose force should be minimized
    * @param fcomp force component 
    * @param weight used in the minimization
    */
   public void addForce (MinimizableForceComponent fcomp, double weight) {
      addForce (fcomp, weight, /*staticOnly=*/true);
   }
   
   /**
    * Adds a force component whose force should be minimized
    * @param fcomp force component
    */
   public void addForce (MinimizableForceComponent fcomp) {
      addForce (fcomp, /*weight=*/1.0, /*staticOnly=*/true);
   }
   
   /**
    * Adds a force component whose force should be minimized
    * @param fcomp force component 
    * @param weights used in the minimization
    * @param staticOnly {@code true} if only static forces should be minimized
    */
   public void addForce (
      MinimizableForceComponent fcomp, VectorNd weights, boolean staticOnly) {
      if (weights.size() < fcomp.getMinForceSize()) {
         throw new IllegalArgumentException (
            "size of weights less than "+fcomp.getMinForceSize()+
            " required for force component");
      }
      ForceCompInfo finfo = doAddForce (fcomp, staticOnly);
      finfo.setWeights (weights);
      updateWeightsVector();
   }

   /**
    * Removes targets
    */
   public void clearTargets() {
      myForceComps.clear();
      myTargetWgts = null;
      myTotalForceSize = 0;
   }

   /**
    * Returns list of target points/frames
    */
   public ArrayList<MinimizableForceComponent> getForces() {
      ArrayList<MinimizableForceComponent> fcomps =
         new ArrayList<MinimizableForceComponent>(myForceComps.size());
      for (ForceCompInfo finfo : myForceComps) {
         fcomps.add (finfo.myComp);
      }
      return fcomps;
   }

   Vector3d vtmp = new Vector3d();
   Point3d ptmp = new Point3d();
   RigidTransform3d Xtmp = new RigidTransform3d();
   Twist veltmp = new Twist();

   public int getModelVelSize() {
      return myMech.getActiveVelStateSize();
   }

   public int getTotalForceSize() {
      return myTotalForceSize;
   }
   
   /**
    * Returns the velocity Jacobian, creates if null
    * 
    * XXX This Jacobian needs to be re-computed at each time step
    * OR it needs to be transformed to global co-ordinates so that the
    * tracking controller can use it properly, since it does not change
    * as the model moves
    */
   public SparseBlockMatrix getStiffnessJacobian (double h) {
      // Again, keepVelocityJacobianConstant should be false, unless set true
      // for comparison with legacy code
      if (myJacobian == null) {
         createVelocityJacobian(h);
      }
      return myJacobian;
   }
   
   protected SparseBlockMatrix createJacobian() {
      return myMech.createVelocityJacobian();
   }
   
   protected void addPosJacobian (SparseBlockMatrix J, double s) {
      int bi = 0;
      for (ForceCompInfo finfo : myForceComps) {
         bi = finfo.myComp.addMinForcePosJacobian (
            J, s, finfo.myStaticOnly, bi);
      }
      // fold attachments into targets on dynamic components 
      // (same as constraint jacobians)
      myMech.reduceVelocityJacobian(J);     
   }

   protected void addVelJacobian (SparseBlockMatrix J, double s) {
      int bi = 0;
      for (ForceCompInfo finfo : myForceComps) {
         bi = finfo.myComp.addMinForceVelJacobian (J, s, bi);
      }
      // fold attachments into targets on dynamic components 
      // (same as constraint jacobians)
      myMech.reduceVelocityJacobian(J);     
   }

   private void createVelocityJacobian (double h) {
      myJacobian = myMech.createVelocityJacobian();

      int bi = 0;
      for (ForceCompInfo finfo : myForceComps) {
         bi = finfo.myComp.addMinForcePosJacobian (
            myJacobian, h, finfo.myStaticOnly, bi);
      }

      // fold attachments into targets on dynamic components 
      // (same as constraint jacobians)
      myMech.reduceVelocityJacobian(myJacobian);
   }
   
   public boolean isStaticOnly() {
      for (ForceCompInfo finfo : myForceComps) {
         if (!finfo.myStaticOnly) {
            return false;
         }
      }
      return true;
   }

   /**
    * Fills <code>H</code> and <code>b</code> with this motion term
    * @param H LHS matrix to fill
    * @param b RHS vector to fill
    * @param rowoff row offset to start filling term
    * @param t0 starting time of time step
    * @param t1 ending time of time step
    * @return next row offset
    */
   public int getTerm(
      MatrixNd H, VectorNd b, int rowoff, double t0, double t1) {

      updateTotalForce(); // set myTotalForce
      
      VectorNd f = new VectorNd (myTotalForceSize);

      f.sub (myTotalForce, myController.getData().getF0 ());
      
      if (myController.getDebug ()) {
         System.out.println ("(MinimizeForceTerm)");
         System.out.println ("\tmyTotalForce: " + myTotalForce.toString ("%.3f"));
         System.out.println ("\tV0: " + myController.getData ().getV0 ().toString ("%.3f"));
         System.out.println ("\tf: " + f.toString ("%.3f"));
      }
      
      MatrixNd Hf = new MatrixNd (
         myTotalForceSize, myController.numExcitations());
      Hf.set (myController.getData ().getHf ());

      if (myController.getData ().normalizeH) {
         double fn = 1.0 / Hf.frobeniusNorm ();
         Hf.scale (fn);
         f.scale (fn);
      }
      
      // apply weights
      if (myTargetWgts != null) {
         MotionForceInverseData.diagMul(myTargetWgts,Hf,Hf);
         MotionForceInverseData.pointMul(myTargetWgts,f,f);
      }
      if (myWeight >= 0) {
         Hf.scale(myWeight);
         f.scale(myWeight);
      }
      H.setSubMatrix(rowoff, 0, Hf);
      b.setSubVector(rowoff, f);
      
      return rowoff + Hf.rowSize();
   }

   /**
    * Fills <code>H</code> and <code>b</code> with this motion term
    * In contrast to <code>getTerm</code>, this method does not
    * recompute the values.
    */
   public void reGetTerm(MatrixNd H, VectorNd b) {
      // XXX do useTimestepScaling, useNormalizeH on targetVel
      b.sub (myTotalForce, myController.getData ().getV0 ());
      H.set (myController.getData ().getHv ());
   }
   
   /**
    * Weight used to scale the contribution of this term in the quadratic
    * optimization problem
    */
   @Override
   public void setWeight(double w) {
      super.setWeight(w);
   }

   /**
    * Sets weights for a force component.  This allows you to weight more
    * heavily the forces you deem to be more important.
    *
    * @param idx force component index
    * @param weights vector of weights
    */
   public void setTargetWeights(int idx, VectorNd weights) {
      if (idx < 0 || idx >= myForceComps.size()) {
         throw new IndexOutOfBoundsException (
            "force component index "+idx+" out of bounds [0,"+
            myForceComps.size()+"]");
      }
      myForceComps.get(idx).setWeights (weights);
      updateWeightsVector();
   }
   
   /**
    * Sets overall weight for a force component.  This allows you to weight
    * more heavily the forces you deem to be more important.
    *
    * @param idx force component index
    * @param weight weight factor
    */
   public void setTargetWeights(int idx, double weight) {
      if (idx < 0 || idx >= myForceComps.size()) {
         throw new IndexOutOfBoundsException (
            "force component index "+idx+" out of bounds [0,"+
            myForceComps.size()+"]");
      }
      myForceComps.get(idx).setWeights (weight);
      updateWeightsVector();
   }
   
   /**
    * Returns target weights, one per force.  Used by properties so can
    * be get/set through interface
    */
   public VectorNd getTargetWeights(int idx) {
      if (idx < 0 || idx >= myForceComps.size()) {
         throw new IndexOutOfBoundsException (
            "force component index "+idx+" out of bounds [0,"+
            myForceComps.size()+"]");
      }
      return new VectorNd (myForceComps.get(idx).myWeights);
   }
   
   /**
    * Sets whether or not to normalize the contribution to <code>H</code> and
    * <code>b</code> by the Frobenius norm of this term's <code>H</code> block.
    * This is for scaling purposes when damping is important.  If set to false,
    * then the damping term's scale will depend on the time and spatial scales.
    * However, if set to true, we will likely scale this term differently every
    * time step.
    * 
    * @param enable if <code>true</code>, enables normalization
    */
   public void setNormalizeH(boolean enable) {
      myController.getData().normalizeH = enable;
   }
   
   /**
    * Returns whether or not we're normalizing this term's contribution to
    * <code>H</code> and <code>b</code>.  See {@link #setNormalizeH(boolean)}
    * @return true if we're normalizing this term's contribution to
    * <code>H</code> and <code>b</code>
    */
   public boolean getNormalizeH() {
      return myController.getData().normalizeH;
   }

   @Override
   protected void compute (double t0, double t1) {
      getTerm (H,f,0,t0,t1);
   }
   
   @Override
   public int getRowSize() {
      return getTotalForceSize();
   }
   
   public boolean getUseKKTFactorAndSolve () {
      return myController.getData().useKKTFactorAndSolve;
   }

   public void setUseKKTFactorAndSolve (boolean useKKTFactorAndSolve) {
      myController.getData().useKKTFactorAndSolve = useKKTFactorAndSolve;
   }
}
