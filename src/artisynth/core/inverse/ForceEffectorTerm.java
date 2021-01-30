/**
 * Copyright (c) 2019, by the Authors: John E. Lloyd, Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.ForceTargetComponent;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.matrix.MatrixNd;
import maspack.util.*;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.spatialmotion.Twist;

/**
 * Cost term that controls the forces of one or more force effectors.  Can
 * also be employed as a constraint term.
 * 
 * @author John E. Lloyd, Ian Stavness, Antonio Sanchez
 */
public class ForceEffectorTerm extends LeastSquaresTermBase {

   boolean debug = false;
   public boolean debugHf = false;   

   protected ComponentList<ForceEffectorTarget> myForceTargets;

   protected SparseBlockMatrix myJacobian = null;
   protected MatrixNd myHf = new MatrixNd();
   protected VectorNd myFbar = new VectorNd();

   // current force error: currentTotalForce - currentForceTarget
   public VectorNd myForceError = new VectorNd();

   public static PropertyList myProps =
      new PropertyList(ForceEffectorTerm.class, LeastSquaresTermBase.class);

   static {
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ForceEffectorTerm () {
      myForceTargets = new ComponentList<>(
         ForceEffectorTarget.class, "forceTargets");
      add (myForceTargets);     
   }
   
   public ForceEffectorTerm (String name) {
      this ();
      setName (name);
   }
   
   public void getForceError (VectorNd totalForce) {
      int idx = 0;
      VectorNd minf = new VectorNd();
      for (ForceEffectorTarget mtarg : myForceTargets) {
         ForceTargetComponent fcomp = mtarg.getForceComp();
         int fsize = fcomp.getForceSize();
         minf.setSize (fsize);
         fcomp.getForce (minf, mtarg.myStaticOnly);
         minf.sub (mtarg.getTargetForce());
         totalForce.setSubVector (idx, minf);
         idx += fsize;
      }
   }

   private void updateForceError() {
      myForceError.setSize (getTotalForceSize());
      getForceError (myForceError);
   }

   VectorNd prevTargetPos = null;
   VectorNd diffTargetPos = null;
   Frame tmpFrame = new Frame ();
   Point tmpPoint = new Point ();

   double[] tmpBuf = new double[3];

   /**
    * Adds a force component to the term for force control
    * @param fcomp
    * @param staticOnly
    */
   private ForceEffectorTarget doAddForce (
      ForceTargetComponent fcomp, boolean staticOnly) {

      ForceEffectorTarget mtarg =
         new ForceEffectorTarget (fcomp, staticOnly);
      mtarg.setName (
         TrackingController.makeTargetName ("f", fcomp, myForceTargets));
         
      myForceTargets.add (mtarg);

      // set target matrix null, so that it is recreated on demand
      // XXX should be updated on a change event...
      myJacobian = null;
      return mtarg;
   }
   
   /**
    * Removes a target to the term for trajectory error
    * @param source
    */
   protected void removeForce (ForceTargetComponent fcomp) {

      int idx = -1;
      for (int k=0; k<myForceTargets.size(); k++) {
         if (myForceTargets.get(k).getForceComp() == fcomp) {
            idx = k;
            break;
         }
      }
      if (idx == -1) {
         return;
      }
      
      myForceTargets.remove (idx);

      // set target matrix null, so that it is recreated on demand
      // XXX should be updated on a change event...
      myJacobian = null;
   }

   VectorNd collectAllWeights() {
      VectorNd weights = new VectorNd (getTotalForceSize());
      int k = 0;
      for (ForceEffectorTarget mtarg : myForceTargets) {
         double w = mtarg.getWeight();
         VectorNd subWeights = mtarg.getSubWeights();
         for (int i=0; i<subWeights.size(); i++) {
            weights.set (k++, w*subWeights.get(i));
         }
      }
      return weights;
   }
   
   /**
    * Adds a force component whose force should be controlled
    * @param fcomp force component 
    * @param weight used for the component
    * @param staticOnly {@code true} if only static forces should be controlled
    * @return ForceEffectorTarget for managing the target forces
    */
   public ForceEffectorTarget addForce (
      ForceTargetComponent fcomp, double weight, boolean staticOnly) {
      ForceEffectorTarget mtarg = doAddForce (fcomp, staticOnly);
      mtarg.setWeight (weight);
      return mtarg;
   }
   
   /**
    * Adds a force component whose force should be controlled
    * @param fcomp force component 
    * @param weight used for the component
    * @return ForceEffectorTarget for managing the target forces
    */
   public ForceEffectorTarget addForce (
      ForceTargetComponent fcomp, double weight) {
      return addForce (fcomp, weight, /*staticOnly=*/true);
   }
   
   /**
    * Adds a force component whose force should be controlled
    * @param fcomp force component
    * @return ForceEffectorTarget for managing the target forces
    */
   public ForceEffectorTarget addForce (ForceTargetComponent fcomp) {
      return addForce (fcomp, /*weight=*/1.0, /*staticOnly=*/true);
   }
   
   /**
    * Adds a force component whose force should be controlled
    * @param fcomp force component 
    * @param weights used for the component
    * @param staticOnly {@code true} if only static forces should be controlled
    * @return ForceEffectorTarget for managing the target forces
    */
   public ForceEffectorTarget addForce (
      ForceTargetComponent fcomp, VectorNd weights, boolean staticOnly) {
      if (weights.size() < fcomp.getForceSize()) {
         throw new IllegalArgumentException (
            "size of weights less than "+fcomp.getForceSize()+
            " required for force component");
      }
      ForceEffectorTarget mtarg = doAddForce (fcomp, staticOnly);
      mtarg.setSubWeights (weights);
      return mtarg;
   }

   /**
    * Removes targets
    */
   public void clearTargets() {
      myForceTargets.clear();
   }

   /**
    * Returns list of target points/frames
    */
   public ArrayList<ForceTargetComponent> getForces() {
      ArrayList<ForceTargetComponent> fcomps =
         new ArrayList<ForceTargetComponent>(myForceTargets.size());
      for (ForceEffectorTarget mtarg : myForceTargets) {
         fcomps.add (mtarg.getForceComp());
      }
      return fcomps;
   }

   Vector3d vtmp = new Vector3d();
   Point3d ptmp = new Point3d();
   RigidTransform3d Xtmp = new RigidTransform3d();
   Twist veltmp = new Twist();

   private int getTotalForceSize() {
      int fsize = 0;
      for (ForceEffectorTarget mtarg : myForceTargets) {
         fsize += mtarg.getForceComp().getForceSize();
      }
      return fsize;
   }
   
   private SparseBlockMatrix createJacobian (MechSystemBase mech) {
      return mech.createVelocityJacobian();
   }
   
   private void addPosJacobian (
      SparseBlockMatrix J, MechSystemBase mech, double s) {
      int bi = 0;
      for (ForceEffectorTarget mtarg : myForceTargets) {
         bi = mtarg.getForceComp().addForcePosJacobian (
            J, s, mtarg.myStaticOnly, bi);
      }
      // fold attachments into targets on dynamic components 
      // (same as constraint jacobians)
      mech.reduceVelocityJacobian(J);     
   }

   private void addVelJacobian (
      SparseBlockMatrix J, MechSystemBase mech, double s) {
      int bi = 0;
      for (ForceEffectorTarget mtarg : myForceTargets) {
         bi = mtarg.getForceComp().addForceVelJacobian (J, s, bi);
      }
      // fold attachments into targets on dynamic components 
      // (same as constraint jacobians)
      mech.reduceVelocityJacobian(J);     
   }
   
   public boolean isStaticOnly() {
      for (ForceEffectorTarget mtarg : myForceTargets) {
         if (!mtarg.myStaticOnly) {
            return false;
         }
      }
      return true;
   }

   /**
    * Fills <code>H</code> and <code>b</code> with this motion term
    * @param A LHS matrix to fill
    * @param b RHS vector to fill
    * @param rowoff row offset to start filling term
    * @param t0 starting time of time step
    * @param t1 ending time of time step
    * @return next row offset
    */
   public int getTerm(
      MatrixNd A, VectorNd b, int rowoff, double t0, double t1) {

      TrackingController controller = getController();
      if (controller != null) {
         updateHb (controller, t0, t1);
         A.setSubMatrix(rowoff, 0, myHf);
         b.setSubVector(rowoff, myFbar);
         rowoff += myHf.rowSize();
      }
      return rowoff;
   }

   public void updateHb (TrackingController controller, double t0, double t1) {

      double h = TimeBase.round(t1 - t0);

      updateForceError(); // set myForceError
      
      int fsize = myForceError.size();
      int numex = controller.numExciters();

      myFbar.setSize (fsize);
      myHf.setSize (fsize, numex);
      VectorNd f0 = new VectorNd(fsize);
      VectorNd Hf_j = new VectorNd(fsize);

      VectorNd u0 = controller.getU0();
      int velSize = u0.size();
      VectorNd curVel = new VectorNd(velSize);
      controller.getMech().getActiveVelState (curVel);

      boolean useTrapezoidal = controller.useTrapezoidalSolver();
      MechSystemBase mech = controller.getMech();
      SparseBlockMatrix Jf = createJacobian (mech);
      if (!isStaticOnly() || useTrapezoidal) {
         if (!isStaticOnly()) {
            addVelJacobian (Jf, mech, -1.0);
         }
         if (useTrapezoidal) {
            addPosJacobian (Jf, mech, h/2);
         }
         Jf.mul (f0, curVel, Jf.rowSize(), velSize);
         f0.negate();
      }
      addPosJacobian (Jf, mech, -h);

      Jf.mulAdd (f0, u0, Jf.rowSize (), velSize);
      for (int j=0; j<numex; j++) {
         Jf.mul (Hf_j, controller.getHuCol(j), Jf.rowSize (), velSize);
         myHf.setColumn (j, Hf_j.getBuffer());          
      }

      myFbar.sub (myForceError, f0);
      
      if (controller.getDebug ()) {
         System.out.println ("(ForceEffectorTerm)");
         System.out.println ("\\tmyForceError: " + myForceError.toString ("%.3f"));
         System.out.println ("\tf: " + myFbar.toString ("%.3f"));
      }

      if (controller.getNormalizeH()) {
         double fn = 1.0 / myHf.frobeniusNorm ();
         myHf.scale (fn);
         myFbar.scale (fn);
      }
      
      // apply weights
      VectorNd weights = collectAllWeights();
      myHf.mulDiagonalLeft (weights);
      mulElements(myFbar,weights,myFbar);

      if (myWeight >= 0) {
         myHf.scale(myWeight);
         myFbar.scale(myWeight);
      }
   }

   public MatrixNd getH() {
      return myHf;
   }

   public VectorNd getB() {
      return myFbar;
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
   public void setTargetWeights (int idx, VectorNd weights) {
      if (idx < 0 || idx >= myForceTargets.size()) {
         throw new IndexOutOfBoundsException (
            "force component index "+idx+" out of bounds [0,"+
            myForceTargets.size()+"]");
      }
      myForceTargets.get(idx).setSubWeights (weights);
   }
   
   /**
    * Sets overall weight for a force component.  This allows you to weight
    * more heavily the forces you deem to be more important.
    *
    * @param idx force component index
    * @param weight weight factor
    */
   public void setTargetWeights(int idx, double weight) {
      if (idx < 0 || idx >= myForceTargets.size()) {
         throw new IndexOutOfBoundsException (
            "force component index "+idx+" out of bounds [0,"+
            myForceTargets.size()+"]");
      }
      myForceTargets.get(idx).setWeight (weight);
   }
   
   /**
    * Returns target weights, one per force.  Used by properties so can
    * be get/set through interface
    */
   public VectorNd getTargetWeights(int idx) {
      if (idx < 0 || idx >= myForceTargets.size()) {
         throw new IndexOutOfBoundsException (
            "force component index "+idx+" out of bounds [0,"+
            myForceTargets.size()+"]");
      }
      return new VectorNd (myForceTargets.get(idx).getSubWeights());
   }
   
   @Override
   public void getQP (MatrixNd Q, VectorNd p, double t0, double t1) {
      TrackingController controller = getController();
      if (controller != null) {     
         updateHb (controller, t0, t1);
         computeAndAddQP (Q, p, myHf, myFbar);
      }
   }
   
   @Override
   public int numConstraints(int qpsize) {
      return getTotalForceSize();
   }
   
   /**
    * {@inheritDoc}
    */
   public void connectToHierarchy (CompositeComponent hcomp) {
      if (isInternal() && getParent() == hcomp && getController() != null) {
         TrackingController tcon = (TrackingController)getParent();
         tcon.myForceEffectorTerm = this;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      if (isInternal() && getParent() == hcomp && getController() != null) {
         TrackingController tcon = (TrackingController)getParent();
         tcon.myForceEffectorTerm = null;
      }
   }

}
