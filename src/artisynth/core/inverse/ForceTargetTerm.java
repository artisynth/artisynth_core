/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.util.ArrayList;
import java.util.Arrays;

import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.util.TimeBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;

/**
 * Reaction force error term for the TrackingController
 * 
 * @author Ian Stavness, Benedikt Sagl
 *
 */
public class ForceTargetTerm extends LeastSquaresTermBase {

   public static final double DEFAULT_WEIGHT = 1d;
   
   boolean debug = false;
   boolean enabled = true;

   protected TrackingController myController; // controller to which this term is associated
   protected MechSystemBase myMech;    // mech system, used to compute forces
      
   // protected ArrayList<ForceTargetComponent> myForceSources;                    // ONE LIST!!!! + seperate weigh variables; uses lambda + target lambda for error
   protected ArrayList<ForceTarget> myForceTargets;            //NEW INTERFACE? + in seperated variables??? or use the same ones?
   protected VectorNd myTargetWgts = null; // size of myTargetForceSize
   protected ArrayList<Double> myTargetWeights;                                                                     //NEW SECOND WEIGHT VARIABLE FOR FORCES?
 
   protected VectorNd myTargetLam = null;
 
   protected SparseBlockMatrix myForJacobian = null;
   protected VectorNd myTargetFor;
   protected int myTargetForSize;

   public static boolean DEFAULT_NORMALIZE_H = false;
   protected boolean normalizeH = DEFAULT_NORMALIZE_H;
   
   public static PropertyList myProps =
      new PropertyList(ForceTargetTerm.class, LeastSquaresTermBase.class);

   static {
      myProps.add("targetWeights", "Weights for each target", null);
//      myProps.add ("MyTargetForce","force targets", DEFAULT_FT);
      myProps.add(
         "normalizeH", "normalize contribution by frobenius norm",                              
         DEFAULT_NORMALIZE_H);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public ForceTargetTerm (TrackingController controller) {
      this(controller, DEFAULT_WEIGHT);
   }
   
   public ForceTargetTerm (TrackingController controller, double weight) {
      super();
      myMech = controller.getMech();
      myController = controller;
      myForceTargets = new ArrayList<ForceTarget>();  
      myTargetWeights = new ArrayList<Double>();
   }
   
   public void updateTargetForce (double t0, double t1) {
      if (!isEnabled ()) {
         return;
      }
      if (myTargetFor == null || myTargetFor.size () != myTargetForSize)
         myTargetFor = new VectorNd (myTargetForSize);
      double[] buf = myTargetFor.getBuffer ();

      int idx = 0;
      for (int i = 0; i < myForceTargets.size (); i++) {
         ForceTarget target = myForceTargets.get (i);
         VectorNd lambda = target.getTargetLambda ();
         lambda.get (buf, idx);
         idx += lambda.size ();
      }
//       System.out.println("targetForce = "+myTargetFor);
   }

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   double[] tmpBuf = new double[3];

   
   public SparseBlockMatrix getForceJacobian() {
      if (myForJacobian == null) {
         createForceJacobian();
      }
      return myForJacobian;
   }

   private void createForceJacobian () {
      MechModel mechMod = (MechModel)myMech;
      SparseBlockMatrix GT = new SparseBlockMatrix ();
      VectorNd dg = new VectorNd ();
      mechMod.getBilateralConstraints (GT, dg);
      if (debug) {
         System.out.println ("num con = " + mechMod.bodyConnectors ().size ());
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
      int[] connectorSizes = new int[mechMod.bodyConnectors ().size ()];
      int[] targetToConnectorMap = new int[myForceTargets.size ()];

      int targetIdx = 0;
      for (ForceTarget ft : myForceTargets) {
         int connectorIdx = 0;
         for (BodyConnector connector : mechMod.bodyConnectors ()) {
            
            if (debug) {
               System.out.println(connector.getName());
               System.out.println(ft.getName());
            }

            if (ft.getConnector ()==connector) {
               targetToConnectorMap[targetIdx] = connectorIdx;
               targetIdx++;
            }
            
            if (connector.isEnabled () == true) {
               if (debug) { System.out.println (
                  connector.numBilateralConstraints ()); }
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
      myTargetWeights.add(weight);
      myTargetForSize += con.numBilateralConstraints ();
      myForceTargets.add (forceTarget);
      myController.targetForces.add (forceTarget);
      return forceTarget;
   }
   
   /**
    * Removes a target to the term for reaction force error
    * @param source
    */
   protected void removeForceTarget(ForceTarget forceTarget) {
      if (myForceTargets.contains (forceTarget)) {
         myTargetForSize -=
            forceTarget.getConnector ().numBilateralConstraints ();
         myForceTargets.remove (forceTarget);
         myController.targetForces.remove (forceTarget);
      }
   }
   
   /**
    * Removes all force targets
    */
   public void clearTargets () {
      myForceTargets.clear ();
      myTargetWeights.clear ();
      myTargetWgts = null;
      myTargetForSize = 0;
      myController.targetForces.clear ();
   }
   
   private void updateWeightsVector () {

      myTargetWgts = new VectorNd (myForceTargets.size ());

      int idx = 0;
      for (int t = 0; t < myForceTargets.size (); t++) {
         double w = myTargetWeights.get (t);
         int targetSize = myForceTargets.get (t).getTargetLambda ().size ();
         for (int i = 0; i < targetSize; i++) {
            myTargetWgts.set (idx++, w);
         }
      }
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
      double h = TimeBase.round(t1 - t0);
      
//      System.out.println("using pre-comp data");

      updateTargetForce (t0, t1); // set myTargetForce
      
      VectorNd cbar = new VectorNd (myTargetForSize);
      // XXX do useTimestepScaling, useNormalizeH on targetVel
      cbar.sub (myTargetFor, myController.getData ().getC0 ());
      
      MatrixNd Hc = new MatrixNd (myTargetForSize, myController.numExcitations ());
      Hc.set (myController.getData ().getHc ());
      
      if (myController.getData ().normalizeH) {
         double fn = 1.0 / Hc.frobeniusNorm ();
         Hc.scale (fn);
         cbar.scale (fn);
      }
      
      if (myController.getData ().useTimestepScaling) { // makes it independent of the time step
         Hc.scale(1/h);      
         cbar.scale(1/h); 
      }
      
      // apply weights
      if (myTargetWgts != null) {
         MotionForceInverseData.diagMul(myTargetWgts,Hc,Hc);
         MotionForceInverseData.pointMul(myTargetWgts,cbar,cbar);
      }
      if (myWeight >= 0) {
          Hc.scale(myWeight);
          cbar.scale(myWeight);
       }

      H.setSubMatrix(rowoff, 0, Hc);
      b.setSubVector(rowoff, cbar);

      if (myController.isDebugTimestep (t0, t1)) {
         System.out.println("tcon "+myTargetFor);
         System.out.println("Jc "+getForceJacobian ());
         System.out.println("Hc "+Hc);
         System.out.println("cbar "+cbar);
      }
      
      return rowoff + Hc.rowSize();
   }

   /**
    * Weight used to scale the contribution of this term in the quadratic optimization problem
    */
   @Override
   public void setWeight (double w) {
      super.setWeight (w);
   }

   public void setTargetWeights (VectorNd wgts) {

      if (wgts.size () == myForceTargets.size ()) {
         myTargetWeights.clear ();
         for (int i = 0; i < myForceTargets.size (); i++) {
            myTargetWeights.add (wgts.get (i));
         }
         updateWeightsVector ();
      }

   }
   
   public void getTargetWeights (VectorNd out) {

      if (out.size () == myForceTargets.size ()) {
         for (int i = 0; i < myForceTargets.size (); i++) {
            out.set (i, myTargetWeights.get (i));
         }
      }
   }

   public VectorNd getTargetWeights () {
      VectorNd out = new VectorNd (myForceTargets.size ());
      getTargetWeights (out);
      return out;
   }

   
   /**
    * Sets whether or not to normalize the contribution to <code>H</code>
    * and <code>b</code> by the Frobenius norm of this term's <code>H</code> block.
    * This is for scaling purposes when damping is important.  If set to false,
    * then the damping term's scale will depend on the time and spatial scales. 
    * However, if set to true, we will likely scale this term differently every
    * time step.
    * 
    * @param set
    */
   public void setNormalizeH(boolean set) {
      myController.getData().normalizeH = set;
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
   public int getRowSize () {
      return myTargetForSize;
   }

   @Override
   protected void compute (double t0, double t1) {
      getTerm (H,f,0,t0,t1);
   }

   public ArrayList<ForceTarget> getForceTargets () {
      return myForceTargets;
   }
}
