/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.util.TimeBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.PointStyle;

/**
 * Force error term for the TrackingController
 * 
 * @author Ian Stavness, Benedikt Sagl
 *
 */
public class ForceTargetTerm extends LeastSquaresTermBase {

   public static final boolean DEFAULT_USE_PD_CONTROL = false;
   boolean usePDControl = DEFAULT_USE_PD_CONTROL;
   boolean debug = false;
   boolean enabled = true;
   double CONSTR_FOR_SIZE=1;

   protected MechSystemBase myMech;    // mech system, used to compute forces
   protected TrackingController myController; // controller to which this term is associated
      
  // protected ArrayList<ForceTargetComponent> myForceSources;                    // ONE LIST!!!! + seperate weigh variables; uses lambda + target lambda for error
   protected ArrayList<ForceTarget> myForceTargets;            //NEW INTERFACE? + in seperated variables??? or use the same ones?
   protected VectorNd myForTargetWgts;
      protected ArrayList<Double> myTargetForceWeights;                                                                     //NEW SECOND WEIGHT VARIABLE FOR FORCES?
  
   protected RenderProps targetRenderProps;
   protected RenderProps sourceRenderProps;
 
   protected VectorNd myTargetLam = null;
 
   
   //double[] values={0,1,0,0};                  //////////////////////////////////////////////////////////////////////////
  // protected MatrixNd myForJacobian = new MatrixNd (1, 4, values);   
   protected SparseBlockMatrix myForJacobian = null;
   protected VectorNd myTargetFor;
   protected int myTargetForSize;
   

   public static boolean DEFAULT_NORMALIZE_H = false;
   protected boolean normalizeH = DEFAULT_NORMALIZE_H;
   
   public static double DEFAULT_Kd = 1.0;
   protected double Kd = DEFAULT_Kd;

   public static double DEFAULT_Kp = 100;
   protected double Kp = DEFAULT_Kp;
   
   static double[] val={1,1};
   public static VectorNd DEFAULT_WEIGHTS= new VectorNd (val);
   
   private static final int POINT_ENTRY_SIZE = 3;
   private static final int FRAME_POS_SIZE = 6;
   private static final int FRAME_VEL_SIZE = 7;

   protected VectorNd myCurrentVel = null;

   public static PropertyList myProps =
      new PropertyList(ForceTargetTerm.class, LeastSquaresTermBase.class);

   static {
      myProps.add("targetWeights", "Weights for each target", DEFAULT_WEIGHTS);
      myProps.add(
         "Kd", "derivative gain", DEFAULT_Kd);
      myProps.add(
         "Kp", "proportional gain", DEFAULT_Kd);
      myProps.add(
         "normalizeH", "normalize contribution by frobenius norm",                              
         DEFAULT_NORMALIZE_H);
      //myProps.add ("MyTargetForce","force targets", DEFAULT_FT);
      myProps.addReadOnly (
         "derr", "derivative error at current timestep");
      myProps.addReadOnly (
         "perr", "proporational error at current timestep");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public ForceTargetTerm (TrackingController trackingController) {
      super();
      myMech = trackingController.getMech();
      myController = trackingController;
      myForceTargets = new ArrayList<ForceTarget>();
      
  
      myTargetForceWeights = new ArrayList<Double>();
      initTargetRenderProps();
      initSourceRenderProps();
   }

   public void updateTarget(double t0, double t1) {
           updateTargetForce();
     
   }
   
   public void updateTargetForce()
   {
      if (myTargetFor == null || myTargetFor.size() != myTargetForSize)
         myTargetFor = new VectorNd(myTargetForSize);
      double[] buf = myTargetFor.getBuffer();
      
      
      int idx = 0;
      for (int i = 0; i < myForceTargets.size(); i++) {
         ForceTarget target = myForceTargets.get(i);
         VectorNd lambda = target.getTargetLambda ();
       
            buf[i] = lambda.get (0);   //work only for planar constraints
        
      }
//   System.out.println(myTargetFor);
   /*for (int i=0; i<myForceTargets.size();i++)
      {
         myTargetFor.
         double[] val={1};
         myForTargetWgts= new VectorNd (val);
      }*/
   
   }
   
 /*  public void updateForceError()
   {
      int idx = 0;
      for (int i = 0; i < myForceTargets.size(); i++)
      {
         ForceTarget target = myForceTargets.get(i);
         idx = calcForError (forceError, idx, target);
      }
   }
   */
   /*private int calcForError(VectorNd forError, int idx, ForceTarget target) { // forError muss nur int sien, weil nur 1 lambda pro constraint oder?
     VectorNd error=new VectorNd(target.getTargetLambda());
      error.sub (myMotionForceTerm.getLambda());    // from constraint
     
      
     
   }*/
   
  

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      // if (myMech != null && myMech instanceof MechModel) {
      // ((MechModel)myMech).setDynamicsEnabled(enabled);
      // }
   }

   private VectorNd myVel = new VectorNd();

   public VectorNd getCurrentVel() {
      if (myVel.size() != myMech.getActiveVelStateSize()) {
         myVel.setSize(myMech.getActiveVelStateSize());
      }
      myMech.getActiveVelState(myVel);
      return myVel;
   }

   double[] tmpBuf = new double[3];

   public void initTargetRenderProps() {
      targetRenderProps = new RenderProps();
      targetRenderProps.setDrawEdges(true);
      targetRenderProps.setFaceStyle(FaceStyle.NONE);
      targetRenderProps.setLineColor(Color.CYAN);
      targetRenderProps.setLineWidth(2);
      targetRenderProps.setPointColor(Color.CYAN);
      targetRenderProps.setPointStyle(PointStyle.SPHERE);
      // set target point radius explicitly
//      targetRenderProps.setPointRadius (myController.targetsPointRadius);
      
      myController.targetPoints.setRenderProps (targetRenderProps);
      myController.targetFrames.setRenderProps (targetRenderProps);
      
      // RENDERPROPS FOR FORCE TARGET????
   }
   
   public void initSourceRenderProps() {
      sourceRenderProps = new RenderProps();
      sourceRenderProps.setDrawEdges(true);
      sourceRenderProps.setFaceStyle(FaceStyle.NONE);
      sourceRenderProps.setLineColor(Color.CYAN);
      sourceRenderProps.setLineWidth(2);
      sourceRenderProps.setPointColor(Color.CYAN);
      sourceRenderProps.setPointStyle(PointStyle.SPHERE);
      // modRenderProps.setAlpha(0.5);
   }

   
   
   public SparseBlockMatrix getForceJacobian() {
      if (myForJacobian == null) {
         createForceJacobian();
      }
      return myForJacobian;
   }

   private void createForceJacobian() {
      MechModel mechMod = (MechModel)myMech;
    int[] target_idx= new int [myForceTargets.size()];
  
    int idx=0;
    int cons_ind=0;
    System.out.println(mechMod.bodyConnectors ().size());
    SparseBlockMatrix GT=new SparseBlockMatrix ();
    VectorNd dg= new VectorNd();
    mechMod.getBilateralConstraints (GT, dg);
    System.out.println(GT.colSize ());
    System.out.println(GT.rowSize ());
    System.out.println(GT.getSize ());
    System.out.println(GT.numBlocks ());
    System.out.println(GT.getBlock (0, 0));
    System.out.println(GT.getBlock (0, 1));
    System.out.println(GT.getBlock (0, 2));
    System.out.println(GT.getBlock (1, 0));
    int[] constraint_blocksize=new int[mechMod.bodyConnectors ().size()];
    for (int i=0; i < myForceTargets.size(); i++)
    {
       cons_ind=0;
       
       for(int j =0; j<mechMod.bodyConnectors ().size(); j++)
       {
          System.out.println(mechMod.bodyConnectors ().get (j).getName());
          if(mechMod.bodyConnectors ().get (j).getName()==myForceTargets.get (i).getName())
          {
             target_idx[idx]=cons_ind;
             idx++;
                
          }
          if(mechMod.bodyConnectors ().get (j).isEnabled ()==true)
          {
          System.out.println(mechMod.bodyConnectors ().get (j).numBilateralConstraints ());
          constraint_blocksize[cons_ind]=mechMod.bodyConnectors ().get (j).numBilateralConstraints ();
          cons_ind++;
          }
          
       }
     /*  if(mechMod.bodyConnectors ().get (i).isEnabled ()==true)
       {
         // dynsize[i]=1;
          
       }*/
    }
    int[] dynsize=new int[cons_ind];
    Arrays.fill (dynsize, 1);
      myForJacobian= new SparseBlockMatrix (new int[0], constraint_blocksize);
      

      for (int i = 0; i < myForceTargets.size(); i++) {
         ForceTarget target = myForceTargets.get(i);
         target.addForceJacobian(myForJacobian, i,target_idx[i]);
      }

      // fold attachments into targets on dynamic components (same as constraint
      // jacobians)
    //  myMech.reduceVelocityJacobian(myVelJacobian);
   }
   /**
    * Removes a target to the term for trajectory error
    * @param source
    */
   protected void removeTarget(MotionTargetComponent source) {
       
      //TO DO!!!!
      
   }
   
   /*
   
   
   Implement REMOVE method
   
   
   
   */
   public void addForceTarget(BodyConnector con) {
      addForceTarget (con, new VectorNd (con.numBilateralConstraints ()));
   }

   
   public void addForceTarget(BodyConnector con, VectorNd lam)
   {
      myForceTargets.add (new ForceTarget(lam,con));
      double weight=1;
      myTargetForceWeights.add(weight);
      myTargetForSize += con.numBilateralConstraints ();
   }  
   
   private void updateWeightsVector() {
      
      myForTargetWgts = new VectorNd(myForceTargets.size());
      
      int idx = 0;
      for (int t = 0; t< myForceTargets.size(); t++) {
         ForceTarget target = myForceTargets.get(t);
         double w = myTargetForceWeights.get(t);
         
         myForTargetWgts.set(idx++,w);
      }
    
      
      
   }
   
   
   /**
    * Adds a target to track
    * @param source point or frame on the model you wish to drive to
    *        a target position
    * @param weight used in error tracking
    * @return the created point or frame that will be used as a target
    */
      
   /**
    * Adds a target to track
    * @param target point or frame on the model you wish to drive to
    *        a target position
    * @return the created point or frame that will be used as a target
    */
 
   /**
    * Removes targets
    */
   public void clearTargets() {
     
      myForceTargets.clear();
     
      myTargetForceWeights.clear();
      
      myController.targetPoints.clear ();
      myController.targetFrames.clear ();
      myForceTargets.clear();
      myTargetForceWeights.clear ();
   }

   private void updateModelVelocity() {
      int n = myMech.getActiveVelStateSize();
      if (myCurrentVel == null || myCurrentVel.size() != n)
         myCurrentVel = new VectorNd(n);
      myMech.getActiveVelState(myCurrentVel);
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

      updateTarget(t0, t1); // set myTargetVel
      updateModelVelocity(); // set myCurrentVel
//      fixTargetPositions();   // XXX not sure why this is needed
      
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
      if (myForTargetWgts != null) {
         MotionForceInverseData.diagMul(myForTargetWgts,Hc,Hc);
         MotionForceInverseData.pointMul(myForTargetWgts,cbar,cbar);
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
      

      
      
      
//      return myForceTerm.getTerm(H, b, rowoff, t0, t1,
//          myCurrentVel, myTargetFor, myForTargetWgts,  getForceJacobian(), normalizeH);
      
      
   }
   
   
  /* public VectorNd getMyTargetForce()
   {
      return myForceTargets.get(0).getMyTargetForce ();
   }
   
   public void setMyTargetForce(VectorNd tarlam)
   {

     
      myForceTargets.get (0).setMyTargetForce (tarlam);
         updateTargetForce ();
         
   }*/


   /**
    * Weight used to scale the contribution of this term in the quadratic optimization problem
    */
   @Override
   public void setWeight(double w) {
      super.setWeight(w);
   }

   public RenderProps getTargetRenderProps() {
      return targetRenderProps;
   }
   public void setTargetWeights(VectorNd wgts) {
      
      if (wgts.size() == myForceTargets.size()) {
         myTargetForceWeights.clear();
         for (int i=0; i<myForceTargets.size(); i++) {
            myTargetForceWeights.add(wgts.get(i));
         }
         updateWeightsVector();
      }
      
   }
   
   /**
    * Proportional gain for PD controller
    * 
    */
   public void setKp(double k) {
      Kp = k;
   }
   
   /**
    * Returns the Proportional gain for PD controller.  See {@link #setKp(double)}.
    * @return proportional error gain
    */
   public double getKp() {
      return Kp;
   }
   public void setKd(double k) {
      Kd = k;
   }
   public double getKd() {
      return Kd;
   }
   public double getPerr() {
      // updated in getTerm()
//      updatePositionError ();
      return 0;
   }
   
   public double getDerr() {
      // updated in getTerm()
//      updateVelocityError ();
      return 0;
   }
   public void getTargetWeights(VectorNd out) {
      
      if (out.size() == myForceTargets.size()) {
         for (int i=0; i<myForceTargets.size(); i++) {
            out.set(i, myTargetForceWeights.get(i));
            }
         }
         }
   public VectorNd getTargetWeights() {
      VectorNd out = new VectorNd(myForceTargets.size());
      getTargetWeights(out);
      return out;
   }

   /**
    * Render props for the sources
    */
   public RenderProps getSourceRenderProps() {
      return sourceRenderProps;
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
      normalizeH = set;
   }
   
   /**
    * Returns whether or not we're normalizing this term's contribution to
    * <code>H</code> and <code>b</code>.  See {@link #setNormalizeH(boolean)} 
    * @return true if we're normalizing this term's contribution to
    * <code>H</code> and <code>b</code>
    */
   public boolean getNormalizeH() {
      return normalizeH;
   }

   @Override
   public int getRowSize () {
      return myTargetForSize;
   }

   @Override
   protected void compute (double t0, double t1) {
      getTerm (H,f,0,t0,t1);
      
   }

  /* public void setForJacobian(double[] val)
   {
      //myForJacobian= new MatrixNd (1, 4, val);
      myForJacobian= new MatrixNd (1,  val.length, val);
   }*/
   public ArrayList<ForceTarget> getForceTargets()
   {
      return myForceTargets;
   }
}
