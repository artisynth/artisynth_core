/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.awt.Color;
import java.util.ArrayList;

import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.RenderProps.Faces;
import maspack.render.RenderProps.PointStyle;
import maspack.spatialmotion.Twist;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.FrameState;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.MotionTarget.TargetActivity;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.PointState;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.util.TimeBase;

/**
 * Tracking error term for the TrackingController
 * 
 * @author Ian Stavness, with modifications by Antonio Sanchez
 *
 */
public class MotionTargetTerm extends LeastSquaresTermBase {

   boolean debug = false;
   boolean enabled = true;

   protected MechSystemBase myMech;    // mech system, used to compute forces
   protected TrackingController myController; // controller to which this term is associated
   protected MotionTerm myMotionTerm;
   protected ArrayList<MotionTargetComponent> mySources;
   protected ArrayList<MotionTargetComponent> myTargets;
   protected ArrayList<Double> myTargetWeights; // one weight per target
   protected RenderProps targetRenderProps;
   protected RenderProps sourceRenderProps;

   protected SparseBlockMatrix myVelJacobian = null;
   protected VectorNd myTargetVel = null;
   protected VectorNd myTargetPos = null;
   protected int myTargetVelSize;
   protected int myTargetPosSize;

   protected VectorNd myTargetWgts = null;   // size of myTargetVelSize, weights for system
   public static boolean DEFAULT_NORMALIZE_H = false;
   protected boolean normalizeH = DEFAULT_NORMALIZE_H;
   
   public static double DEFAULT_Kd = 1.0;
   protected double Kd = DEFAULT_Kd;

   public static double DEFAULT_Kp = 1.0;
   protected double Kp = DEFAULT_Kp;
   
   private static final int POINT_ENTRY_SIZE = 3;
   private static final int FRAME_POS_SIZE = 6;
   private static final int FRAME_VEL_SIZE = 7;

   protected VectorNd myCurrentVel = null;

   public static PropertyList myProps =
      new PropertyList(MotionTargetTerm.class, LeastSquaresTermBase.class);

   static {
      myProps.add("targetWeights", "Weights for each target", null);
      myProps.add(
         "Kd", "derivative gain", DEFAULT_Kd);
      myProps.add(
         "Kp", "proportional gain", DEFAULT_Kd);
      myProps.add(
         "normalizeH", "normalize contribution by frobenius norm",
         DEFAULT_NORMALIZE_H);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public MotionTargetTerm (TrackingController trackingController) {
      super();
      myMech = trackingController.getMech();
      myController = trackingController;
      myMotionTerm = new MotionTerm(trackingController);
      mySources = new ArrayList<MotionTargetComponent>();
      myTargets = new ArrayList<MotionTargetComponent>();
      myTargetWeights = new ArrayList<Double>();
      
      initTargetRenderProps();
      initSourceRenderProps();
   }

   public void updateTarget(double t0, double t1) {
      if (!isEnabled()) {
         return;
      }
      // round because results are very sensitive to h and we want to keep them
      // identical to earlier results when t0, t1 where given as nsec integers
      double h = TimeBase.round(t1 - t0);

      // position error
//      double perr = 
      
      
      // OLD
      interpolateTargetVelocity(h);
      // updateTargetPosAndVel(h);
      updateTargetVelocity();
   }

   public void dispose() {
      System.out.println("motion target dispose()");
      myMotionTerm.dispose();
   }

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
      targetRenderProps.setFaceStyle(Faces.NONE);
      targetRenderProps.setLineColor(Color.CYAN);
      targetRenderProps.setLineWidth(2);
      targetRenderProps.setPointColor(Color.CYAN);
      targetRenderProps.setPointStyle(PointStyle.SPHERE);
      // set target point radius explicitly
      targetRenderProps.setPointRadius (myMotionTerm.myController.targetsPointRadius);
      
      myController.targetPoints.setRenderProps (targetRenderProps);
      myController.targetFrames.setRenderProps (targetRenderProps);
   }
   
   public void initSourceRenderProps() {
      sourceRenderProps = new RenderProps();
      sourceRenderProps.setDrawEdges(true);
      sourceRenderProps.setFaceStyle(Faces.NONE);
      sourceRenderProps.setLineColor(Color.CYAN);
      sourceRenderProps.setLineWidth(2);
      sourceRenderProps.setPointColor(Color.CYAN);
      sourceRenderProps.setPointStyle(PointStyle.SPHERE);
      // modRenderProps.setAlpha(0.5);
   }

   
   /**
    * Adds a target to the term for trajectory error
    * @param source
    * @param weight
    * @return the created target body or point
    */
   private MotionTargetComponent doAddTarget(MotionTargetComponent source, 
      double weight) {
      
      mySources.add(source);
//      myController.sourcePoints.add (source);
      
      source.setTargetActivity(TargetActivity.None);
      MotionTargetComponent target = null;
      
      if (source instanceof Point) {
         myTargetVelSize += POINT_ENTRY_SIZE;
         myTargetPosSize += POINT_ENTRY_SIZE;
         target = addTargetPoint((Point)source);
      }
      else if (source instanceof Frame) {
         myTargetVelSize += FRAME_POS_SIZE;
         myTargetPosSize += FRAME_VEL_SIZE;
         target = addTargetFrame((RigidBody)source);
      }
      
      myTargetWeights.add(weight);
      updateWeightsVector();

      // set target matrix null, so that it is recreated on demand
      // XXX should be updated on a change event...
      myVelJacobian = null;

      // Main.getInverseManager().configureTargetProbes();
      // mySolver.resetVariables();
      return target;
   }
   
   /**
    * Removes a target to the term for trajectory error
    * @param source
    */
   protected void removeTarget(MotionTargetComponent source) {
      int idx = mySources.indexOf (source);
      if (idx == -1) {
         return;
      }
      
      if (source instanceof Point) {
         myTargetVelSize -= POINT_ENTRY_SIZE;
         myTargetPosSize -= POINT_ENTRY_SIZE;
         removeTargetPoint((Point)myTargets.get (idx)); // remove ref particle created in doAddTarget
      }
      else if (source instanceof Frame) {
         myTargetVelSize -= FRAME_POS_SIZE;
         myTargetPosSize -= FRAME_VEL_SIZE;
         removeTargetFrame((Frame)myTargets.get (idx)); // remove ref body created in doAddTarget
      }
      
      myTargetWeights.remove (idx);
      mySources.remove (idx);
      myTargets.remove (idx);
      updateWeightsVector();

      // set target matrix null, so that it is recreated on demand
      // XXX should be updated on a change event...
      myVelJacobian = null;

      // Main.getInverseManager().configureTargetProbes();
      // mySolver.resetVariables();
   }

   private void updateWeightsVector() {
      
      myTargetWgts = new VectorNd(myTargetVelSize);
      
      int idx = 0;
      for (int t = 0; t< mySources.size(); t++) {
         MotionTargetComponent target = mySources.get(t);
         double w = myTargetWeights.get(t);
         
         if (target instanceof Point) {
            for (int i=0; i< POINT_ENTRY_SIZE; i++) {
               myTargetWgts.set(idx++, w);
            }
         } else if (target instanceof Frame) {
            for (int i=0; i< FRAME_VEL_SIZE; i++) {
               myTargetWgts.set(idx++, w);
            }
         }
      }
      
   }
   
   /**
    * Adds a target to track
    * @param source point or frame on the model you wish to drive to
    *        a target position
    * @param weight used in error tracking
    * @return the created point or frame that will be used as a target
    */
   public MotionTargetComponent addTarget(MotionTargetComponent source, double weight) {
      return doAddTarget(source, weight);
   }
   
   /**
    * Adds a target to track
    * @param target point or frame on the model you wish to drive to
    *        a target position
    * @return the created point or frame that will be used as a target
    */
   public MotionTargetComponent addTarget(MotionTargetComponent target) {
      return doAddTarget(target, 1.0);
   }

   /**
    * Removes targets
    */
   public void clearTargets() {
      mySources.clear();
      myTargets.clear();
      myTargetWeights.clear();
      myTargetWgts = null;
      myTargetVelSize = 0;
      myController.targetPoints.clear ();
      myController.targetFrames.clear ();
   }

   /**
    * Creates and adds a target point
    * 
    * @param source point source to drive to target
    * @return the created target point
    */
   private TargetPoint addTargetPoint(Point source) {
      TargetPoint tpnt = new TargetPoint();
      tpnt.setName((source.getName() != null ? source.getName() : String.format(
         "p%d", source.getNumber())) + "_ref");
      tpnt.setState(source);
      tpnt.setTargetActivity(TargetActivity.PositionVelocity);
      myTargets.add(tpnt);

      myController.targetPoints.add (tpnt);

      return tpnt;
   }

   private void removeTargetPoint(Point target) {
      if (target instanceof TargetPoint) {
         myController.targetPoints.remove (target);
      }
      else {
         System.err.println("Warning target is not TargetPoint");
      }
   }
   
   
   
   /**
    * Creates and adds a target frame, returning the created frame
    * to track
    * 
    * @param source to drive toward target 
    * @return the created target frame
    */
   private TargetFrame addTargetFrame(RigidBody source)
   {
      TargetFrame tframe = new TargetFrame();
      tframe.setName((source.getName() != null ? source.getName() : String.format(
         "rb%d", source.getNumber())) + "_ref");

      tframe.setState(source);
      tframe.setTargetActivity(TargetActivity.PositionVelocity);
      tframe.setAxisLength(1.0);

      myTargets.add(tframe);

      // XXX add mesh to TargetFrame
//      if (source.getMesh() != null) {
//         tframe.setMesh(new PolygonalMesh(source.getMesh()),
//            source.getMeshFileName());
//      }
      
      myController.targetFrames.add (tframe);
      return tframe;
   }
   
   private void removeTargetFrame(Frame target) {
      if (target instanceof TargetFrame) {
         myController.targetFrames.remove (target);
      }
      else {
         System.err.println("Warning target is not TargetFrame");
      }
   }

   /**
    * Returns list of target points/frames
    */
   public ArrayList<MotionTargetComponent> getTargets() {
      return myTargets;
   }

   /**
    * Returns list of source points/frames that are part of the model
    * that will move to the targets
    */
   public ArrayList<MotionTargetComponent> getSources() {
      return mySources;
   }

   /*
    * the reference velocity is updated based on the current model position and
    * the target reference position
    */
   private void interpolateTargetVelocity(double h) {
      assert myTargets.size() == mySources.size(); // paranoid

      for (int i = 0; i < myTargets.size(); i++) {
         MotionTargetComponent target = myTargets.get(i);
         if (target instanceof Point) {
            interpolateTargetVelocity(
               (Point)mySources.get(i), (Point)target, h);
         } else if (target instanceof Frame) {
            interpolateTargetVelocity(
               (Frame)mySources.get(i), (Frame)target, h);
         }
      }
   }

   Point3d ptmp = new Point3d();
   RigidTransform3d Xtmp = new RigidTransform3d();
   Twist veltmp = new Twist();

   private Vector3d
      interpolateTargetVelocity(Point current, Point target, double h) {

      ptmp.sub(target.getPosition(), current.getPosition());
      ptmp.scale(1d / h);
      ptmp.scale(Kd);       // Peter's edit
      target.setVelocity(ptmp);

      if (debug) {
         System.out.println("targetPos = "
            + target.getPosition().toString("%g"));
         System.out.println("currentPos = "
            + current.getPosition().toString("%g"));
         System.out.println("targetVel = "
            + target.getVelocity().toString("%g"));
      }
      return ptmp;
   }

   private Twist
      interpolateTargetVelocity(Frame current, Frame target, double h) {
      Xtmp.mulInverseLeft(current.getPose(), target.getTargetPose());
      veltmp.v.scale(1d / h, Xtmp.p);
      double rad = Xtmp.R.getAxisAngle(veltmp.w);
      veltmp.w.scale(rad / h);
      veltmp.scale(Kd);        // Peter's edit; should we use a different constant to scale rotational velocities?
      target.setVelocity(veltmp);
      return veltmp;
   }

   PointState tmpPointState = new PointState();
   FrameState tmpFrameState = new FrameState();

   private void syncPosAndVelState() {
      for (int i = 0; i < mySources.size(); i++) {
         MotionTargetComponent target = mySources.get(i);
         // ComponentState tmpState = (target instanceof
         // Point)?tmpPointState:tmpFrameState;
         // target.getState(tmpState);
         // myRefTargets.get(i).setState(tmpState);
         myTargets.get(i).setState(target);
      }
   }

   private void updateTargetVelocity() {
      if (myTargetVel == null || myTargetVel.size() != myTargetVelSize)
         myTargetVel = new VectorNd(myTargetVelSize);
      double[] buf = myTargetVel.getBuffer();
      int idx = 0;
      for (int i = 0; i < myTargets.size(); i++) {
         MotionTargetComponent target = myTargets.get(i);
         if (target instanceof Point) {
            Vector3d vel = ((Point)target).getVelocity();
            buf[idx++] = vel.x;
            buf[idx++] = vel.y;
            buf[idx++] = vel.z;
         }
         else if (target instanceof Frame) {
            Twist vel = ((Frame)target).getVelocity();
            buf[idx++] = vel.v.x;
            buf[idx++] = vel.v.y;
            buf[idx++] = vel.v.z;
            buf[idx++] = vel.w.x;
            buf[idx++] = vel.w.y;
            buf[idx++] = vel.w.z;
         }
      }
   }

   private void updateTargetPosAndVel(double h) {
      if (myTargetVel == null || myTargetVel.size() != myTargetVelSize)
         myTargetVel = new VectorNd(myTargetVelSize);

      if (myTargetPos == null || myTargetPos.size() != myTargetPosSize)
         myTargetPos = new VectorNd(myTargetPosSize);

      double[] velBuf = myTargetVel.getBuffer();
      double[] posBuf = myTargetPos.getBuffer();
      int velIdx = 0, posIdx = 0;
      for (int i = 0; i < myTargets.size(); i++) {
         velIdx = myTargets.get(i).getTargetVel(velBuf, 1, h, velIdx);
         posIdx = myTargets.get(i).getTargetPos(posBuf, 1, h, posIdx);

      }
   }

   private void updateCurrentVel() {
      int n = myMech.getActiveVelStateSize();
      if (myCurrentVel == null || myCurrentVel.size() != n)
         myCurrentVel = new VectorNd(n);
      myMech.getActiveVelState(myCurrentVel);
   }

   public int getTargetVelSize() {
      return myTargetVelSize;
   }

   private void invalidateStressIfFem() {
      FemModel3d fem = null;
      if (myMech instanceof MechModel) {
         if (((MechModel)myMech).models().size() > 0 &&
            ((MechModel)myMech).models().get(0) instanceof FemModel3d) {
            fem = (FemModel3d)((MechModel)myMech).models().get(0);
         }
      }
      else if (myMech instanceof FemModel3d) {
         fem = (FemModel3d)myMech;
      }

      if (fem != null) {
         fem.invalidateStressAndStiffness();
      }
   }

   /**
    * Returns the velocity Jacobian, creates if null
    */
   public SparseBlockMatrix getVelocityJacobian() {
      if (myVelJacobian == null) {
         createVelocityJacobian();
      }
      return myVelJacobian;
   }

   private void createVelocityJacobian() {
      myVelJacobian = myMech.createVelocityJacobian();

      for (int i = 0; i < mySources.size(); i++) {
         MotionTargetComponent target = mySources.get(i);
         target.addTargetJacobian(myVelJacobian, i);
      }

      // fold attachments into targets on dynamic components (same as constraint
      // jacobians)
      myMech.reduceVelocityJacobian(myVelJacobian);
   }

   /**
    * Returns complete size of velocity vector (not number of targets)
    */
   public int getTargetSize() {
      return getTargetVelSize();
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
      updateTarget(t0, t1); // set myTargetVel
      updateCurrentVel(); // set myCurrentVel
//      fixTargetPositions();   // XXX not sure why this is needed
      return myMotionTerm.getTerm(H, b, rowoff, t0, t1,
         myTargetVel, myCurrentVel, myTargetWgts, getVelocityJacobian(), normalizeH);
    
   }
   
   private void fixTargetPositions() {
      for (MotionTargetComponent comp : myTargets) {
         if (comp instanceof Point) {
            Point p = (Point)comp;
            p.setPosition(p.getTargetPosition());
            p.setVelocity(p.getTargetVelocity());
         } else if (comp instanceof Frame) {
            Frame f = (Frame)comp;
            f.setVelocity(f.getTargetVelocity());
            f.setPose(f.getTargetPose());
         }
      }
   }


   /**
    * Weight used to scale the contribution of this term in the quadratic optimization problem
    */
   @Override
   public void setWeight(double w) {
      super.setWeight(w);
      myMotionTerm.setWeight(myWeight);
   }

   /**
    * Sets weights for targets.  This allows you to weight more heavily the points
    * you deem to be more important.  If wgts.size() equals the number of targets, each target
    * will use a single weight.  If wgts.size() equals the full size of the target velocity
    * vector, then this directly scales the rows of <code>H</code> (allows you to scale x,y,z
    * components of velocity separately).
    * 
    * @param wgts weights vector
    */
   public void setTargetWeights(VectorNd wgts) {
      
      if (wgts.size() == mySources.size()) {
         myTargetWeights.clear();
         for (int i=0; i<mySources.size(); i++) {
            myTargetWeights.add(wgts.get(i));
         }
         updateWeightsVector();
      } else if (wgts.size() == myTargetVelSize) {
         myTargetWgts.set(wgts);
      }
      
   }
   
   /**
    * Gets weights for targets. If wgts.size() equals the number of targets, returns the
    * set of single weights used per target.  If wgts.size() equals the full size of the 
    * target velocity vector, then returns the direct scaling vector.
    * 
    * @param out weights vector
    */
   public void getTargetWeights(VectorNd out) {
      
      if (out.size() == mySources.size()) {
         for (int i=0; i<mySources.size(); i++) {
            out.set(i, myTargetWeights.get(i));
         }
      } else if (out.size() == myTargetVelSize) {
         out.set(myTargetWgts);
      }
   }

   /**
    * Render props for targets
    */
   public RenderProps getTargetRenderProps() {
      return targetRenderProps;
   }

   /**
    * Returns target weights, one per target point.  Used by properties so can be get/set
    * through interface
    */
   public VectorNd getTargetWeights() {
      VectorNd out = new VectorNd(mySources.size());
      getTargetWeights(out);
      return out;
   }
   
   /**
    * Sets render props for the target points/frames
    */
   public void setTargetRenderProps(RenderProps rend) {
      targetRenderProps.set (rend);

      myController.targetPoints.setRenderProps (targetRenderProps);
      myController.targetFrames.setRenderProps (targetRenderProps);
   }

   /**
    * Render props for the sources
    */
   public RenderProps getSourceRenderProps() {
      return sourceRenderProps;
   }

   /**
    * Sets render props for sources
    */
   public void setSourceRenderProps(RenderProps rend) {
      sourceRenderProps.set(rend);

      for (MotionTargetComponent p : mySources) {
         if (p instanceof Point) {
            ((Point)p).setRenderProps(sourceRenderProps);
         } else if (p instanceof Frame) {
            ((Frame)p).setRenderProps(sourceRenderProps);
         }
      }
   }
   
   /**
    * Derivative gain for PD controller
    * 
    */
   public void setKd(double k) {
      Kd = k;
   }
   
   /**
    * Returns the Derivative gain for PD controller.  See {@link #setKd(double)}.
    * @return derivative error gain
    */
   public double getKd() {
      return Kd;
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

}
