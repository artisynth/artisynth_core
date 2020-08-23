/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.util.TimeBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;

/**
 * Cost term that minimizes the tracking error for the constraint forces of one
 * or more rigid body connectors. Can also be employed as a constraint term.
 * 
 * @author Ian Stavness, Benedikt Sagl
 * 
 * TODO Fix the jacobian
 * TODO See if a similar issue exists for frame-based tracking
 */
public class ForceTargetTerm extends LeastSquaresTermBase {

   public static final double DEFAULT_WEIGHT = 1d;
   
   boolean debug = false;
   boolean enabled = true;

   // other attributes

   protected RenderableComponentList<ForceTarget> myForceTargets;
  
   // quantities used in the computation which are allocated on demand

   protected SparseBlockMatrix myForJacobian = null;
   protected VectorNd myTargetForce = new VectorNd();
   protected int myTargetForceSize;

   protected MatrixNd myHc = new MatrixNd();
   protected VectorNd myCbar = new VectorNd();

   public static PropertyList myProps =
      new PropertyList(ForceTargetTerm.class, LeastSquaresTermBase.class);

   static {
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public ForceTargetTerm () {
      super();
      myForceTargets =
         new RenderableComponentList<ForceTarget>(
            ForceTarget.class, "forceTargets");
      add (myForceTargets);
   }
   
   public ForceTargetTerm (String name) {
      this ();
      setName (name);
   }
   
   public VectorNd getTargetForce (double t0, double t1) {
      double h=t1-t0;
      updateTargetForce (t0, t1); 
      return myTargetForce;
   }
   
   public void updateTargetForce (double t0, double t1) {
      if (!isEnabled ()) {
         return;
      }
      myTargetForceSize = numTargetForces();
      myTargetForce.setSize (myTargetForceSize);
      double[] buf = myTargetForce.getBuffer ();

      int idx = 0;
      for (ForceTarget target : myForceTargets) {
         VectorNd lambda = target.getTargetLambda ();
         idx = lambda.get (buf, idx);
      }
   }

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   double[] tmpBuf = new double[3];
   
   /**
    * XXX This Jacobian needs to be re-computed at each time step
    * OR it needs to be transformed to global co-ordinates so that the
    * tracking controller can use it properly, since it does not change
    * as the model moves
    */
   public SparseBlockMatrix getForceJacobian (MechModel mech) {
      createForceJacobian(mech);
      return myForJacobian;
   }

   private void createForceJacobian (MechModel mech) {
      if (debug) {
         SparseBlockMatrix GT = new SparseBlockMatrix ();
         VectorNd dg = new VectorNd ();
         mech.getBilateralConstraints (GT, dg);
         System.out.println ("num con = " + mech.bodyConnectors ().size ());
         System.out.println (GT.colSize ());
         System.out.println (GT.rowSize ());
         System.out.println (GT.getSize ());
         System.out.println (GT.numBlocks ());
         System.out.println (GT.getBlock (0, 0));
         System.out.println (GT.getBlock (0, 1));
         System.out.println (GT.getBlock (0, 2));
         System.out.println (GT.getBlock (1, 0));
      }
      
      // find the number of bilateral constraints for each connector
      int[] connectorSizes = new int[mech.bodyConnectors().size ()];
      int[] targetToConnectorMap = new int[myForceTargets.size()];

      int targetIdx = 0;
      for (ForceTarget ft : myForceTargets) {
         int connectorIdx = 0;
         for (BodyConnector connector : mech.bodyConnectors ()) {
            
            if (debug) {
               System.out.println(connector.getName());
               System.out.println(ft.getName());
            }

            if (ft.getConnector ()==connector) {
               targetToConnectorMap[targetIdx] = connectorIdx;
               targetIdx++;
            }
            
            if (connector.isEnabled () == true) {
               if (debug) {
                  System.out.println (
                     connector.numBilateralConstraints ());
               }
               connectorSizes[connectorIdx] =
                  connector.numBilateralConstraints ();
               connectorIdx++;
            }
         }
      }
      
      myForJacobian = new SparseBlockMatrix (new int[0], connectorSizes);
      for (int i = 0; i < myForceTargets.size (); i++) {
         ForceTarget target = myForceTargets.get (i);
         // TODO: non-enabled connectors should not add to Jacobian -- need to fix
         target.addForceJacobian (myForJacobian, i, targetToConnectorMap[i]);
      }

      if (debug) {
         System.out.println("Jc = "+myForJacobian);
      }
   }

   /**
    * Adds a target to track the reaction force of the specified body connector
    * @param con body connector to track
    * @return the component used to keep track of this force target
    */
   public ForceTarget addForceTarget(BodyConnector con) {
      return addForceTarget (con, 1d);
   }
   
   /**
    * Adds a target to track the reaction force of the specified body connector
    * @param con body connector to track
    * @param weight used in error tracking
    * @return the component used to keep track of this force target
    */
   public ForceTarget addForceTarget(BodyConnector con, double weight) {
      return addForceTarget (
         con, new VectorNd(con.numBilateralConstraints ()), weight);
   }
   
   /**
    * Adds a target to track the reaction force of the specified body connector
    * @param con body connector to track
    * @param targetLambda target reaction force for the connector
    * @return the component used to keep track of this force target
    */
   public ForceTarget addForceTarget(BodyConnector con, 
      VectorNd targetLambda) {   
      return addForceTarget (con, targetLambda, 1d);
   }
   
   /**
    * Adds a target to track the reaction force of the specified body connector
    * @param con body connector to track
    * @param targetLambda target reaction force for the connector
    * @param weight used in error tracking
    * @return the component used to keep track of this force target
    */
   public ForceTarget addForceTarget(BodyConnector con, 
      VectorNd targetLambda, double weight) {   
      ForceTarget forceTarget = new ForceTarget(con, targetLambda);
      forceTarget.setName (
         TrackingController.makeTargetName("f", con, myForceTargets));
      forceTarget.setWeight (weight);
      myForceTargets.add (forceTarget);
      return forceTarget;
   }
   
   /**
    * Removes a target to the term for reaction force error
    * @param source
    */
   protected boolean removeForceTarget(ForceTarget forceTarget) {
      return myForceTargets.remove (forceTarget);
   }
   
   /**
    * Removes all force targets
    */
   public void clearTargets () {
      myForceTargets.clear ();
      myTargetForceSize = 0;
   }

   private int numTargets() {
      return myForceTargets.size();
   }
   
   VectorNd collectTargetWeights() {
      VectorNd weights = new VectorNd (numTargets());
      int k = 0;
      for (ForceTarget target : myForceTargets) {
         weights.set (k++, target.getWeight());
      }
      return weights;
   }

   private int numTargetForces() {
      int numf = 0;
      for (ForceTarget target : myForceTargets) {
         numf += target.numBilateralConstraints();
      }
      return numf;
   }

   VectorNd collectAllWeights() {
      VectorNd weights = new VectorNd (numTargetForces());
      int k = 0;
      for (ForceTarget target : myForceTargets) {
         double w = target.getWeight();
         for (int i=0; i<target.numBilateralConstraints(); i++) {
            weights.set (k++, w);
         }
      }
      return weights;
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
         A.setSubMatrix(rowoff, 0, myHc);
         b.setSubVector(rowoff, myCbar);
         rowoff += myHc.rowSize();
      }
      return rowoff;
   }

   public void updateHb (TrackingController controller, double t0, double t1) {
      double h = TimeBase.round(t1 - t0);

      updateTargetForce (t0, t1); // set myTargetForce
      
      int numex = controller.numExciters();
      VectorNd c0 = new VectorNd (myTargetForceSize);
      myCbar.setSize (myTargetForceSize);
      myHc.setSize (myTargetForceSize, numex);
      VectorNd Hc_j = new VectorNd (myTargetForceSize);
      SparseBlockMatrix Jc = getForceJacobian((MechModel)controller.getMech());

      Jc.mul (c0, controller.getLam0());
      
      for (int j=0; j<numex; j++) {
         Jc.mul (Hc_j, controller.getHlamCol(j));
         myHc.setColumn (j, Hc_j);
      }
      
      // scale cbar by h -- Benedikt
      myCbar.set (myTargetForce);
         
      myCbar.scale (h);
      // XXX do useTimestepScaling, useNormalizeH on targetVel
      myCbar.sub (c0);
      
      if (controller.getNormalizeH()) {
         double fn = 1.0 / myHc.frobeniusNorm ();
         myHc.scale (fn);
         myCbar.scale (fn);
      }
      
      if (controller.getUseTimestepScaling()) {
         // makes it independent of the time step
         myHc.scale(1/h);      
         myCbar.scale(1/h); 
      }
      
      // apply weights
      VectorNd weights = collectAllWeights();
      myHc.mulDiagonalLeft(weights);
      mulElements(myCbar,weights,myCbar);

      if (myWeight >= 0) {
          myHc.scale(myWeight);
          myCbar.scale(myWeight);
       }
   }

   public MatrixNd getH() {
      return myHc;
   }

   public VectorNd getB() {
      return myCbar;
   }

   /**
    * Weight used to scale the contribution of this term in the quadratic
    * optimization problem
    */
   @Override
   public void setWeight (double w) {
      super.setWeight (w);
   }

   public void setTargetWeights (VectorNd weights) {
      if (weights.size () == numTargets()) {
         int k = 0;
         for (ForceTarget target : myForceTargets) {
            target.setWeight (weights.get(k++));
         }        
      }
      else {
         throw new IllegalArgumentException (
            "Weights vector size=" + weights.size() +
            "; should equal number of force targets "+numTargets());
      }
   }
   
   public void getTargetWeights (VectorNd weights) {
      weights.setSize (numTargets());
      int k = 0;
      for (ForceTarget target : myForceTargets) {
         weights.set (k++, target.getWeight());
      }        
   }

   public VectorNd getTargetWeights () {
      VectorNd weights = new VectorNd (numTargets());
      getTargetWeights (weights);
      return weights;
   }
   
   @Override
   public int numConstraints (int qpsize) {
      return numTargetForces();
   }

   @Override
   public void getQP (MatrixNd Q, VectorNd p, double t0, double t1) {
      TrackingController controller = getController();
      if (controller != null) {         
         updateHb (controller, t0, t1);
         computeAndAddQP (Q, p, myHc, myCbar);
      }
   }

   public ComponentList<ForceTarget> getForceTargets () {
      return myForceTargets;
   }

   /**
    * {@inheritDoc}
    */
   public void connectToHierarchy (CompositeComponent hcomp) {
      if (isInternal() && getParent() == hcomp && getController() != null) {
         TrackingController tcon = (TrackingController)getParent();
         tcon.myForceTerm = this;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      if (isInternal() && getParent() == hcomp && getController() != null) {
         TrackingController tcon = (TrackingController)getParent();
         tcon.myForceTerm = null;
      }
   }

}
