/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.awt.Color;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.ArrayList;

import maspack.geometry.PolygonalMesh;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.PointStyle;
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
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.util.TimeBase;

/**
 * Motion tracking error term for the TrackingController
 * 
 * @author Ian Stavness, Antonio Sanchez
 *
 */
public class MotionTargetTerm extends LeastSquaresTermBase {

   public static final double DEFAULT_WEIGHT = 1d;
   
   public static final boolean DEFAULT_USE_PD_CONTROL = false;
   protected boolean usePDControl = DEFAULT_USE_PD_CONTROL;

   public static final boolean DEFAULT_DELTA_ACTIVATIONS = false;
   protected boolean useDeltaAct = DEFAULT_DELTA_ACTIVATIONS;
   
   public static boolean DEFAULT_USE_TIMESTEP_SCALING = false;
   protected boolean useTimestepScaling = DEFAULT_USE_TIMESTEP_SCALING;

   public static boolean DEFAULT_USE_KKT_FACTORANDSOLVE = false;
   protected boolean useKKTFactorAndSolve = DEFAULT_USE_KKT_FACTORANDSOLVE;

   public static boolean DEFAULT_USE_TRAPEZOIDAL_SOLVER = false;
   protected boolean useTrapezoidalSolver = DEFAULT_USE_TRAPEZOIDAL_SOLVER;

   public static boolean DEFAULT_NORMALIZE_H = false;
   protected boolean normalizeH = DEFAULT_NORMALIZE_H;
   
   boolean debug = false;
   boolean enabled = true;

   protected TrackingController myController;
   protected MechSystemBase myMech;    // mech system, used to compute forces
//   protected MotionTerm myMotionTerm;
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

   
   public static double DEFAULT_Kd = 1.0;
   protected double Kd = DEFAULT_Kd;

   public static double DEFAULT_Kp = 100;
   protected double Kp = DEFAULT_Kp;
   
   private static final int POINT_POS_SIZE = 3;
   private static final int POINT_VEL_SIZE = 3;
   private static final int FRAME_POS_SIZE = 7;
   private static final int FRAME_VEL_SIZE = 6;

   protected VectorNd myCurrentVel = null;
   

   public static PropertyList myProps =
      new PropertyList(MotionTargetTerm.class, LeastSquaresTermBase.class);

   static {
      myProps.add("useTimestepScaling", "flag for scaling motion term H and vbar by 1/h", 
         DEFAULT_USE_TIMESTEP_SCALING);
      myProps.add("useKKTFactorAndSolve", "flag for re-factoring at each internal KKT solve", 
         DEFAULT_USE_KKT_FACTORANDSOLVE);
      myProps.add(
         "usePDControl * *", "use PD controller for motion term",
         MotionTargetTerm.DEFAULT_USE_PD_CONTROL);
      myProps.add(
         "normalizeH", "normalize contribution by frobenius norm",
         DEFAULT_NORMALIZE_H);
      myProps.add(
         "Kd", "derivative gain", DEFAULT_Kd);
      myProps.add(
         "Kp", "proportional gain", DEFAULT_Kd);
      myProps.addReadOnly (
         "derr", "derivative error at current timestep");
      myProps.addReadOnly (
         "perr", "proporational error at current timestep");
      myProps.add("targetWeights", "Weights for each target", null);

   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public MotionTargetTerm (TrackingController controller) {
      this(controller, DEFAULT_WEIGHT);
   }
   
   public MotionTargetTerm (TrackingController controller, double weight) {
      super(weight);
      myController = controller;
      myMech = myController.getMech();
//      if (useDeltaAct)
//         myMotionTerm = new MotionTermDeltaAct (controller);
//      else
//         myMotionTerm = new MotionTerm(myController);
         
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
      if (usePDControl) {       
         if (t0 == 0) { // XXX need better way to reset
            prevTargetPos = null;
         }
         interpolateTargetVelocity(h);
         updatePositionError ();
         updateVelocityError ();
         if (myTargetVel == null || myTargetVel.size() != myTargetVelSize) {
            myTargetVel = new VectorNd(myTargetVelSize);
         }
         myTargetVel.scale (Kp/h, postionError);
         myTargetVel.scaledAdd (Kd, velocityError);
      }
      else {
         setTargetVelocityFromPositionError(h);
         // updateTargetPosAndVel(h);
         updateTargetVelocityVec(); // set myTargetVel
         
         /* Teun edit: This seems to be an old line of code from before PD control was implemented
         myTargetVel.scale (Kd); // Peter edit, IAN -- this should use be Kp
         */
      }
   }
   
   public VectorNd postionError = new VectorNd();
   public VectorNd velocityError = new VectorNd();
   
   public void updatePositionError() {
      assert myTargets.size() == mySources.size(); // paranoid

      if (postionError == null || postionError.size() != myTargetVelSize) {
         postionError = new VectorNd(myTargetVelSize);
      }

      int idx = 0;
      for (int i = 0; i < myTargets.size(); i++) {
         MotionTargetComponent target = myTargets.get(i);
         if (target instanceof Point) {
            idx = calcPosError (postionError, idx, (Point)mySources.get(i), (Point)target);
         } else if (target instanceof Frame) {
            idx = calcPosError (postionError, idx, (Frame)mySources.get(i), (Frame)target);
         }
      }
   }

   private int calcPosError(VectorNd posError, int idx, Point source, Point target) {
      ptmp.sub(target.getPosition(), source.getPosition());
      
      if (debug) {
//         System.out.println("source_pos = "
//            + source.getPosition().toString("%g"));
//         System.out.println("target_pos = "
//            + target.getPosition().toString("%g"));
         System.out.println("pos_error = "
            + ptmp.toString("%g"));
      }
      
      double[] buf = posError.getBuffer ();
      buf[idx++] = ptmp.x;
      buf[idx++] = ptmp.y;
      buf[idx++] = ptmp.z;
      return idx;
   }

   private int calcPosError(VectorNd posError, int idx, Frame source, Frame target) {
      Xtmp.mulInverseLeft(source.getPose(), target.getPose());
      veltmp.v.set(Xtmp.p);
      double rad = Xtmp.R.getAxisAngle(veltmp.w);
//      veltmp.w.scale(rad / h);
  
      double[] buf = posError.getBuffer ();
      buf[idx++] = veltmp.v.x;
      buf[idx++] = veltmp.v.y;
      buf[idx++] = veltmp.v.z;
      buf[idx++] = veltmp.w.x;
      buf[idx++] = veltmp.w.y;
      buf[idx++] = veltmp.w.z;
      return idx;
   }
   
   public void updateVelocityError() {
      assert myTargets.size() == mySources.size(); // paranoid

      if (velocityError == null || velocityError.size() != myTargetVelSize) {
         velocityError = new VectorNd(myTargetVelSize);
      }

      int idx = 0;
      for (int i = 0; i < myTargets.size(); i++) {
         MotionTargetComponent target = myTargets.get(i);
         if (target instanceof Point) {
            idx = calcVelError (velocityError, idx, (Point)mySources.get(i), (Point)target);
         } else if (target instanceof Frame) {
            idx = calcVelError (velocityError, idx, (Frame)mySources.get(i), (Frame)target);
         }
      }
   }

   private int calcVelError(VectorNd velError, int idx, Point source, Point target) {
      vtmp.sub(target.getVelocity(), source.getVelocity());
      
      if (debug) {
//         System.out.println("source_vel = "
//            + source.getVelocity().toString("%g"));
//         System.out.println("target_vel = "
//            + target.getVelocity().toString("%g"));
         System.out.println("vel_error = "
            + vtmp.toString("%g"));
      }

      double[] buf = velError.getBuffer ();
      buf[idx++] = vtmp.x;
      buf[idx++] = vtmp.y;
      buf[idx++] = vtmp.z;
      return idx;
   }

   private int calcVelError(VectorNd velError, int idx, Frame source, Frame target) {
      veltmp.sub (target.getVelocity (), source.getVelocity ());
      
      double[] buf = velError.getBuffer ();
      buf[idx++] = veltmp.v.x;
      buf[idx++] = veltmp.v.y;
      buf[idx++] = veltmp.v.z;
      buf[idx++] = veltmp.w.x;
      buf[idx++] = veltmp.w.y;
      buf[idx++] = veltmp.w.z;
      return idx;
   }
   
   VectorNd prevTargetPos = null;
   VectorNd diffTargetPos = null;
   Frame tmpFrame = new Frame ();
   Point tmpPoint = new Point ();
   
   /*
    * calculate target velocity by differencing current and last target positions
    */
   private void interpolateTargetVelocity(double h) {
      assert myTargets.size() == mySources.size(); // paranoid
      updateTargetPos (h);
      if (prevTargetPos == null || prevTargetPos.size () != myTargetPosSize) {
         prevTargetPos = new VectorNd(myTargetPos);
      }
      
      double[] prevPosBuf = prevTargetPos.getBuffer ();
      int idx = 0;

      for (int i = 0; i < myTargets.size(); i++) {
         MotionTargetComponent target = myTargets.get(i);
         if (target instanceof Point) {
            idx = tmpPoint.setPosState (prevPosBuf, idx);
            interpolateTargetVelocityFromPositions(
               tmpPoint, (Point)target, h);
         } else if (target instanceof Frame) {
            idx = tmpFrame.setPosState (prevPosBuf, idx);
            interpolateTargetVelocityFromPositions(
               tmpFrame, (Frame)target, h);
         }
      }
      
      prevTargetPos.set (myTargetPos);
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
      targetRenderProps.setFaceStyle(FaceStyle.NONE);
      targetRenderProps.setLineColor(Color.CYAN);
      targetRenderProps.setLineWidth(2);
      targetRenderProps.setPointColor(Color.CYAN);
      targetRenderProps.setPointStyle(PointStyle.SPHERE);
      // set target point radius explicitly
      targetRenderProps.setPointRadius (myController.targetsPointRadius);
      
      myController.targetPoints.setRenderProps (targetRenderProps);
      myController.targetFrames.setRenderProps (targetRenderProps);
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
         myTargetVelSize += POINT_VEL_SIZE;
         myTargetPosSize += POINT_POS_SIZE;
         target = addTargetPoint((Point)source);
      }
      else if (source instanceof Frame) {
         myTargetVelSize += FRAME_VEL_SIZE;
         myTargetPosSize += FRAME_POS_SIZE;
         target = addTargetFrame((RigidBody)source);
      }
      
      myTargetWeights.add(weight);
      updateWeightsVector();

      // set target matrix null, so that it is recreated on demand
      // XXX should be updated on a change event...
      myVelJacobian = null;

      // Main.getMain().getInverseManager().configureTargetProbes();
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
         myTargetVelSize -= POINT_VEL_SIZE;
         myTargetPosSize -= POINT_POS_SIZE;
         removeTargetPoint((Point)myTargets.get (idx)); // remove ref particle created in doAddTarget
      }
      else if (source instanceof Frame) {
         myTargetVelSize -= FRAME_VEL_SIZE;
         myTargetPosSize -= FRAME_POS_SIZE;
         removeTargetFrame((Frame)myTargets.get (idx)); // remove ref body created in doAddTarget
      }
      
      myTargetWeights.remove (idx);
      mySources.remove (idx);
      myTargets.remove (idx);
      updateWeightsVector();

      // set target matrix null, so that it is recreated on demand
      // XXX should be updated on a change event...
      myVelJacobian = null;

      // Main.getMain().getInverseManager().configureTargetProbes();
      // mySolver.resetVariables();
   }

   private void updateWeightsVector() {
      
      myTargetWgts = new VectorNd(myTargetVelSize);
      
      int idx = 0;
      for (int t = 0; t< mySources.size(); t++) {
         MotionTargetComponent target = mySources.get(t);
         double w = myTargetWeights.get(t);
         
         if (target instanceof Point) {
            for (int i=0; i< POINT_VEL_SIZE; i++) {
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
      return doAddTarget(target, 1d);
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
      tframe.setPose (source.getPose ());
      tframe.setName((source.getName() != null ? source.getName() : String.format(
         "rb%d", source.getNumber())) + "_ref");

      tframe.setState(source);
      tframe.setTargetActivity(TargetActivity.PositionVelocity);
      tframe.setAxisLength(1.0);

      myTargets.add(tframe);

      // add mesh to TargetFrame
      if (source.getMesh() != null) {
         tframe.setMesh(new PolygonalMesh(source.getMesh()),
            source.getMeshFileName());
         tframe.setRenderProps (source.getRenderProps ());
         RenderProps.setDrawEdges (tframe, true);
         RenderProps.setFaceStyle (tframe, FaceStyle.NONE);
      }
      
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
   private void setTargetVelocityFromPositionError(double h) {
      assert myTargets.size() == mySources.size(); // paranoid

      for (int i = 0; i < myTargets.size(); i++) {
         MotionTargetComponent target = myTargets.get(i);
         if (target instanceof Point) {
            interpolateTargetVelocityFromPositions(
               (Point)mySources.get(i), (Point)target, h);
         } else if (target instanceof Frame) {
            interpolateTargetVelocityFromPositions(
               (Frame)mySources.get(i), (Frame)target, h);
         }
      }
   }

   Vector3d vtmp = new Vector3d();
   Point3d ptmp = new Point3d();
   RigidTransform3d Xtmp = new RigidTransform3d();
   Twist veltmp = new Twist();

   private Vector3d
      interpolateTargetVelocityFromPositions(Point current, Point target, double h) {

      ptmp.sub(target.getPosition(), current.getPosition());
      ptmp.scale(1d / h);
      target.setVelocity(ptmp);

      if (debug) {
         System.out.println("interpolated_target_vel = "
            + target.getVelocity().toString("%g"));
      }
      return ptmp;
   }

   private Twist
      interpolateTargetVelocityFromPositions(Frame current, Frame target, double h) {
      Xtmp.mulInverseLeft(current.getPose(), target.getPose()); // XXX check that mul order is correct
      veltmp.v.scale(1d / h, Xtmp.p);
      double rad = Xtmp.R.getAxisAngle(veltmp.w);
      veltmp.w.scale(rad / h);
      target.setVelocity(veltmp);
      return veltmp;
   }

   PointState tmpPointState = new PointState();
   FrameState tmpFrameState = new FrameState();

   private void updateTargetVelocityVec() {
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

   private void updateTargetPos(double h) {
      if (myTargetPos == null || myTargetPos.size() != myTargetPosSize)
         myTargetPos = new VectorNd(myTargetPosSize);

      double[] posBuf = myTargetPos.getBuffer();
      int idx = 0;
      for (int i = 0; i < myTargets.size(); i++) {
         idx = myTargets.get(i).getPosState (posBuf, idx);
      }
   }
   
   private void updateTargetVel(double h) {
      if (myTargetVel == null || myTargetVel.size() != myTargetVelSize)
         myTargetVel = new VectorNd(myTargetVelSize);

      double[] velBuf = myTargetVel.getBuffer();
      int idx = 0;
      for (int i = 0; i < myTargets.size(); i++) {
         idx = myTargets.get(i).getVelState (velBuf, idx);
      }
   }

   private void updateModelVelocity() {
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

      // fold attachments into targets on dynamic components 
      // (same as constraint jacobians)
      myMech.reduceVelocityJacobian(myVelJacobian);
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

      updateTarget(t0, t1); // set myTargetVel
      updateModelVelocity(); // set myCurrentVel
//      fixTargetPositions();   // XXX not sure why this is needed
      
      VectorNd vbar = new VectorNd (myTargetVelSize);
      // XXX do useTimestepScaling, useNormalizeH on targetVel

      vbar.sub (myTargetVel, myController.getData ().getV0 ());
      
      MatrixNd Hv = new MatrixNd (myTargetVelSize, myController.numExcitations ());
      Hv.set (myController.getData ().getHv ());
      
      if (myController.getData ().normalizeH) {
         double fn = 1.0 / Hv.frobeniusNorm ();
         Hv.scale (fn);
         vbar.scale (fn);
      }
      
      if (myController.getData ().useTimestepScaling) { // makes it independent of the time step
         Hv.scale(1/h);      
         vbar.scale(1/h); 
      }
      
   // apply weights
      if (myTargetWgts != null) {
         MotionForceInverseData.diagMul(myTargetWgts,Hv,Hv);
         MotionForceInverseData.pointMul(myTargetWgts,vbar,vbar);
      }
      if (myWeight >= 0) {
          Hv.scale(myWeight);
          vbar.scale(myWeight);
       }

      H.setSubMatrix(rowoff, 0, Hv);
      b.setSubVector(rowoff, vbar);
      
      return rowoff + Hv.rowSize();

      
//      return myMotionTerm.getTerm(H, b, rowoff, t0, t1,
//         myTargetVel, myCurrentVel, myTargetWgts, getVelocityJacobian());
   }
   

   /**
    * Fills <code>H</code> and <code>b</code> with this motion term
    * In contrast to <code>getTerm</code>, this method does not
    * recompute the values.
    */
   public void reGetTerm(MatrixNd H, VectorNd b) {
      // XXX do useTimestepScaling, useNormalizeH on targetVel
      b.sub (myTargetVel, myController.getData ().getV0 ());
      H.set (myController.getData ().getHv ());
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
   
   public double getPerr() {
      // updated in getTerm()
//      updatePositionError ();
      return postionError.norm ();
   }
   
   public double getDerr() {
      // updated in getTerm()
//      updateVelocityError ();
      return velocityError.norm ();
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
   protected void compute (double t0, double t1) {
      getTerm (H,f,0,t0,t1);
   }
   
   @Override
   public int getRowSize() {
      return getTargetVelSize();
   }
   
   
   public boolean getUseKKTFactorAndSolve () {
      return myController.getData().useKKTFactorAndSolve;
   }

   public void setUseKKTFactorAndSolve (boolean useKKTFactorAndSolve) {
      myController.getData().useKKTFactorAndSolve = useKKTFactorAndSolve;
   }
   
   public boolean getUseTimestepScaling () {
      return myController.getData().useTimestepScaling;
   }

   public void setUseTimestepScaling (boolean useTimestepScaling) {
      myController.getData().useTimestepScaling = useTimestepScaling;
   }

   public void setUsePDControl(boolean usePD) {
      usePDControl = usePD;
   }

   public boolean getUsePDControl() {
      return usePDControl;
   }


}
