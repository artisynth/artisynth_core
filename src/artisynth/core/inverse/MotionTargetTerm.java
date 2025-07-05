/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ReferenceComp;
import artisynth.core.modelbase.ReferenceListBase;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.util.TimeBase;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SVDecomposition;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.PointStyle;
import maspack.spatialmotion.Twist;

/**
 * Cost term that minimizes the tracking error for one or more motion targets.
 * Can also be employed as a constraint term.
 * 
 * @author Ian Stavness, Antonio Sanchez, John E Lloyd
 * 
 * TODO Fix the jacobian
 * TODO See if a similar issue exists for frame-based tracking
 *
 */
public class MotionTargetTerm extends LeastSquaresTermBase {

   boolean reportHvRank = false;
   boolean debug = false;
   // Avoids recomputation of the velocity Jacobian. This actually gives
   // incorrect results and is provided for comparison with legacy code only.
   public static boolean keepVelocityJacobianConstant = false;

   public static final boolean DEFAULT_LEGACY_CONTROL = false;
   public boolean myLegacyControl = DEFAULT_LEGACY_CONTROL;

   // property attributes

   public static final boolean DEFAULT_USE_PD_CONTROL = false;
   protected boolean usePDControl = DEFAULT_USE_PD_CONTROL;
   
   public static double DEFAULT_CHASE_TIME = 0.01;
   protected double myChaseTime = DEFAULT_CHASE_TIME;

   public static double DEFAULT_Kd = 100.0;
   protected double Kd = DEFAULT_Kd;

   public static double DEFAULT_Kp = 10000;
   protected double Kp = DEFAULT_Kp;

//   public static final boolean DEFAULT_USE_DELTA_ACTIVATIONS = false;
//   protected boolean useDeltaAct = DEFAULT_USE_DELTA_ACTIVATIONS;

   // other attributes
   
   protected RenderProps targetRenderProps;
   protected RenderProps sourceRenderProps;

   protected PointList<TargetPoint> myTargetPoints;
   protected ReferenceListBase<Point,SourcePointReference> mySourcePoints;
   protected RenderableComponentList<TargetFrame> myTargetFrames;
   protected ReferenceListBase<Frame,SourceFrameReference> mySourceFrames;

   // quantities used in the computation which are allocated on demand

   protected SparseBlockMatrix myVelJacobian = null;
   protected VectorNd myTrackingVel = new VectorNd();
   protected VectorNd mySourceVel = new VectorNd();
   protected VectorNd myPosError = new VectorNd();
   protected VectorNd myVelError = new VectorNd();
   protected int myTargetVelSize;
   
   protected VectorNd myVbar = new VectorNd();
   protected MatrixNd myHv = new MatrixNd();

   protected VectorNd myEffectiveVbar = new VectorNd();
   protected MatrixNd myEffectiveHv = new MatrixNd();

   public static PropertyList myProps =
      new PropertyList(MotionTargetTerm.class, LeastSquaresTermBase.class);

   static {
      myProps.add(
         "legacyControl", "use old control methods",
         DEFAULT_LEGACY_CONTROL);
      myProps.add (
         "chaseTime", 
         "if not using PD control, desired time for catching the target",
         DEFAULT_CHASE_TIME);
      myProps.add(
         "usePDControl", "use PD controller for motion term",
         DEFAULT_USE_PD_CONTROL);
//      myProps.add(
//         "useDeltaActivations", "solve for excitations incrementally",
//         DEFAULT_USE_DELTA_ACTIVATIONS);
      myProps.add(
         "Kd", "derivative gain", DEFAULT_Kd);
      myProps.add(
         "Kp", "proportional gain", DEFAULT_Kd);
      myProps.addReadOnly (
         "velocityError", "velocity error at current timestep");
      myProps.addReadOnly (
         "positionError", "position error at current timestep");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MotionTargetTerm () {
      this (null);
   }
   
   public MotionTargetTerm (TrackingController controller) {

      // list of target points that store/show the location of motion targets
      // for points
      myTargetPoints =
         new PointList<TargetPoint> (TargetPoint.class, "targetPoints");
      add (myTargetPoints);

      // list of source points
      mySourcePoints = new ReferenceListBase<>(
         SourcePointReference.class, "sourcePoints");
      mySourcePoints.setNavpanelVisibility (NavpanelVisibility.VISIBLE);
      mySourcePoints.setEditable (false);
      add (mySourcePoints);

      // list of target points that store/show the location of motion targets
      // for bodies
      myTargetFrames =
         new RenderableComponentList<TargetFrame> (
            TargetFrame.class, "targetFrames");
      add (myTargetFrames);

      // list of source frames
      mySourceFrames = new ReferenceListBase<>(
         SourceFrameReference.class, "sourceFrames");
      mySourceFrames.setNavpanelVisibility (NavpanelVisibility.VISIBLE);
      mySourceFrames.setEditable (false);
      add (mySourceFrames);

      initTargetRenderProps (controller);
      initSourceRenderProps();
   }

   public void setTargetsPointRadius (double rad) {
      RenderProps.setPointRadius (myTargetPoints, rad);
   }
   
   public PointList<TargetPoint> getTargetPoints() {
      return myTargetPoints;
   }

   public RenderableComponentList<TargetFrame> getTargetFrames() {
      return myTargetFrames;
   }

   public void updateTarget (double t0, double t1) {
      if (!isEnabled()) {
         return;
      }
      updateSizes();
      // round because results are very sensitive to h and we want to keep them
      // identical to earlier results when t0, t1 where given as nsec integers
      double h = TimeBase.round(t1 - t0);
      updatePositionError (myPosError);

      if (myLegacyControl) {
         if (usePDControl) { 
            updateVelocityError (myVelError);
            myTrackingVel.scale (Kp/h, myPosError);
            myTrackingVel.scaledAdd (Kd, myVelError);
         }
         else {
            myTrackingVel.scale (1/h, myPosError);
         }
      }
      else {
         updateSourceVelocity (mySourceVel);
         if (usePDControl) { 
            updateVelocityError (myVelError);
            //myTrackingVel.scale (1.0, mySourceVel);
            updateTargetVelocity (myTrackingVel);
            myTrackingVel.scaledAdd (h*Kp, myPosError);
            myTrackingVel.scaledAdd (h*Kd, myVelError);
         }
         else {
            //myTrackingVel.scale (1/myChaseTime, myPosError);
            updateTargetVelocity (myTrackingVel);
            myTrackingVel.scaledAdd (1/myChaseTime, myPosError);
         }
         //System.out.println (" tvel=  " + myTrackingVel.toString("%12.8f"));         
      }
      storeTargetVelocity (myTrackingVel);
      
   }
   
   public double getPositionError () {
      return myPosError.norm();
   }
   
   public double getVelocityError () {
      return myVelError.norm();
   }
   
   private boolean sourceAndTargetSizesConsistent() {
      return (myTargetPoints.size() == mySourcePoints.size() &&
              myTargetFrames.size() == mySourceFrames.size());
   }

   private void updatePositionError(VectorNd perror) {
      assert sourceAndTargetSizesConsistent(); // paranoid

      perror.setSize (getTargetVelSize());
      int idx = 0;
      for (int i=0; i<myTargetPoints.size(); i++) {
         idx = calcPosError (
            perror, idx, getSourcePoint(i), myTargetPoints.get(i));
      }
      for (int i=0; i<myTargetFrames.size(); i++) {
         idx = calcPosError (
            perror, idx, getSourceFrame(i), myTargetFrames.get(i));
      }
   }

   private int calcPosError (
      VectorNd posError, int idx, Point source, Point target) {
      ptmp.sub(target.getPosition(), source.getPosition());
      
      if (debug) {
         System.out.println("pos_error = "
            + ptmp.toString("%g"));
      }
      
      double[] buf = posError.getBuffer ();
      buf[idx++] = ptmp.x;
      buf[idx++] = ptmp.y;
      buf[idx++] = ptmp.z;
      return idx;
   }

   private int calcPosError (
      VectorNd posError, int idx, Frame source, Frame target) {
      Xtmp.mulInverseLeft(source.getPose(), target.getPose());
      veltmp.v.set(Xtmp.p);
      double rad = Xtmp.R.getAxisAngle(veltmp.w);
      veltmp.w.scale (rad);
  
      double[] buf = posError.getBuffer ();
      buf[idx++] = veltmp.v.x;
      buf[idx++] = veltmp.v.y;
      buf[idx++] = veltmp.v.z;
      buf[idx++] = veltmp.w.x;
      buf[idx++] = veltmp.w.y;
      buf[idx++] = veltmp.w.z;
      return idx;
   }
   
   public void updateVelocityError(VectorNd verror) {
      assert sourceAndTargetSizesConsistent(); // paranoid

      verror.setSize (getTargetVelSize());
      int idx = 0;
      for (int i=0; i<myTargetPoints.size(); i++) {
         idx = calcVelError (
            verror, idx, getSourcePoint(i), myTargetPoints.get(i));
      }
      for (int i=0; i<myTargetFrames.size(); i++) {
         idx = calcVelError (
            verror, idx, getSourceFrame(i), myTargetFrames.get(i));
      }
   }

   public void updateSourceVelocity (VectorNd vel) {
      assert sourceAndTargetSizesConsistent(); // paranoid

      vel.setSize (getTargetVelSize());
      double[] vbuf = vel.getBuffer ();
      int idx = 0;
      for (int i=0; i<myTargetPoints.size(); i++) {
         idx = getSourcePoint(i).getVelState (vbuf, idx);
      }
      for (int i=0; i<myTargetFrames.size(); i++) {
         idx = getSourceFrame(i).getVelState (vbuf, idx);
      }
   }

   public void updateTargetVelocity (VectorNd vel) {
      assert sourceAndTargetSizesConsistent(); // paranoid

      vel.setSize (getTargetVelSize());
      double[] vbuf = vel.getBuffer ();
      int idx = 0;
      for (int i=0; i<myTargetPoints.size(); i++) {
         idx = myTargetPoints.get(i).getVelState (vbuf, idx);
      }
      for (int i=0; i<myTargetFrames.size(); i++) {
         idx = myTargetFrames.get(i).getVelState (vbuf, idx);
      }
   }

   public void storeTargetVelocity (VectorNd vel) {
      assert sourceAndTargetSizesConsistent(); // paranoid

      double[] vbuf = vel.getBuffer ();
      int idx = 0;
      for (int i=0; i<myTargetPoints.size(); i++) {
         Vector3d tvel = new Vector3d();
         tvel.x = vbuf[idx++];
         tvel.y = vbuf[idx++];
         tvel.z = vbuf[idx++];
         myTargetPoints.get(i).setTargetVelocity (tvel);
      }
      for (int i=0; i<myTargetFrames.size(); i++) {
         Twist tvel = new Twist();
         tvel.v.x = vbuf[idx++];
         tvel.v.y = vbuf[idx++];
         tvel.v.z = vbuf[idx++];
         tvel.w.x = vbuf[idx++];
         tvel.w.y = vbuf[idx++];
         tvel.w.z = vbuf[idx++];
         myTargetFrames.get(i).setTargetVelocity (tvel);
      }
   }

   private int calcVelError (
      VectorNd velError, int idx, Point source, Point target) {
      vtmp.sub(target.getVelocity(), source.getVelocity());

      double[] buf = velError.getBuffer ();
      buf[idx++] = vtmp.x;
      buf[idx++] = vtmp.y;
      buf[idx++] = vtmp.z;
      return idx;
   }

   private int calcVelError (
      VectorNd velError, int idx, Frame source, Frame target) {
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
   
   VectorNd prevTargetPos = new VectorNd();
   VectorNd prevPositionErr = new VectorNd();
   Frame tmpFrame = new Frame ();
   Point tmpPoint = new Point ();
   
   double[] tmpBuf = new double[3];

   public void initTargetRenderProps (TrackingController controller) {
      targetRenderProps = new RenderProps();
      targetRenderProps.setDrawEdges(true);
      targetRenderProps.setFaceStyle(FaceStyle.NONE);
      targetRenderProps.setLineColor(Color.CYAN);
      targetRenderProps.setLineWidth(2);
      targetRenderProps.setPointColor(Color.CYAN);
      targetRenderProps.setPointStyle(PointStyle.SPHERE);
      // don't set target point radius, so that it can be inherited
      
      myTargetPoints.setRenderProps (targetRenderProps);
      myTargetFrames.setRenderProps (targetRenderProps);
   }
   
   public void initSourceRenderProps() {
      sourceRenderProps = new RenderProps();
      sourceRenderProps.setDrawEdges(true);
      sourceRenderProps.setFaceStyle(FaceStyle.NONE);
      sourceRenderProps.setLineColor(Color.CYAN);
      sourceRenderProps.setLineWidth(2);
      sourceRenderProps.setPointColor(Color.CYAN);
      sourceRenderProps.setPointStyle(PointStyle.SPHERE);
   }

   
   /**
    * Adds a target to the term for trajectory error
    * @param source
    * @param weight
    * @return the created target body or point
    */
   private MotionTargetComponent doAddTarget (
      MotionTargetComponent source, double weight) {
      
      if (source instanceof Point) {
         return addPointTarget ((Point)source, weight);
      }
      else if (source instanceof Frame) {
         return addFrameTarget ((Frame)source, weight);
      }
      else {
         throw new IllegalArgumentException (
            "target type "+source+" not supported");
      }
   }
   
   /**
    * Removes a target to the term for trajectory error
    * @param source
    */
   protected boolean removeTarget(MotionTargetComponent source) {
      if (source instanceof Point) {
         int idx = mySourcePoints.indexOf ((Point)source);
         if (idx == -1) {
            return false;
         }
         mySourcePoints.remove (idx);
         myTargetPoints.remove (idx);
      }
      else if (source instanceof Frame) {
         int idx = mySourceFrames.indexOf ((Frame)source);
         if (idx == -1) {
            return false;
         }
         mySourceFrames.remove (idx);
         myTargetFrames.remove (idx);
      }
      else {
         throw new IllegalArgumentException (
            "unsupported target type " + source);
      }
      
      // set target matrix null, so that it is recreated on demand
      // XXX should be updated on a change event...
      myVelJacobian = null;

      // Main.getMain().getInverseManager().configureTargetProbes();
      // mySolver.resetVariables();
      return true;
   }

   private int numTargets() {
      return myTargetPoints.size() + myTargetFrames.size();
   }

   VectorNd collectTargetWeights() {
      VectorNd weights = new VectorNd (numTargets());
      int k = 0;
      for (TargetPoint tp : myTargetPoints) {
         weights.set (k++, tp.getWeight());
      }
      for (TargetFrame tf : myTargetFrames) {
         weights.set (k++, tf.getWeight());
      }
      return weights;
   }

   VectorNd collectAllWeights() {
      VectorNd weights = new VectorNd (getTargetVelSize());
      int k = 0;
      for (TargetPoint tp : myTargetPoints) {
         Vector3d subw = tp.getSubWeights();
         double w = tp.getWeight();
         weights.set (k++, w*subw.x);
         weights.set (k++, w*subw.y);
         weights.set (k++, w*subw.z);
      }
      for (TargetFrame tf : myTargetFrames) {
         VectorNd subw = tf.getSubWeights();
         double w = tf.getWeight();
         for (int i=0; i<6; i++) {
            weights.set (k++, w*subw.get(i));
         }
      }
      return weights;
   }
   
   /**
    * @deprecated Use {@link #addPointTarget(Point,double)} or 
    * {@link #addFrameTarget(Frame,double)} instead.
    */
   public MotionTargetComponent addTarget (
      MotionTargetComponent source, double weight) {
      return doAddTarget(source, weight);
   }
   
   /**
    * @deprecated Use {@link #addPointTarget(Point)} or 
    * {@link #addFrameTarget(Frame)} instead.
    */
   public MotionTargetComponent addTarget(MotionTargetComponent target) {
      return doAddTarget(target, 1d);
   }

   /**
    * Adds a point target to track
    * @param source point in the model you wish to drive to a target position
    * @param weight used in error tracking
    * @return the created point that will be used as a target
    */
   public TargetPoint addPointTarget (Point source, double weight) {
      TargetPoint targetPoint = addTargetPoint((Point)source, myTargetPoints);
      targetPoint.setWeight (weight);
      myTargetPoints.add (targetPoint);
      mySourcePoints.addReference ((Point)source);
      // set target matrix null, so that it is recreated on demand
      // XXX should be updated on a change event...
      myVelJacobian = null;
      return targetPoint;
   }
   
   /**
    * Adds a point target to track with weight 1
    * @param source point in the model you wish to drive to a target
    * position
    * @return the created point that will be used as a target
    */
   public TargetPoint addPointTarget (Point source) {
      return addPointTarget(source, 1d);
   }

   /**
    * Adds a frame target to track
    * @param source frame in the model you wish to drive to a target position
    * @param weight used in error tracking
    * @return the created frame that will be used as a target
    */
   public TargetFrame addFrameTarget (Frame source, double weight) {
      TargetFrame targetFrame =
         addTargetFrame((Frame)source, myTargetFrames);
      targetFrame.setWeight (weight);
      myTargetFrames.add (targetFrame);
      mySourceFrames.addReference ((Frame)source);
      // set target matrix null, so that it is recreated on demand
      // XXX should be updated on a change event...
      myVelJacobian = null;
      return targetFrame;         
   }
   
   /**
    * Adds a frame target to track with weight 1
    * @param source frame in the model you wish to drive to a target position
    * @return the created frame that will be used as a target
    */
   public TargetFrame addFrameTarget (Frame source) {
      return addFrameTarget(source, 1d);
   }

   /**
    * Removes targets
    */
   public void clearTargets() {
      mySourcePoints.removeAll();
      myTargetPoints.removeAll();
      mySourceFrames.removeAll();
      myTargetFrames.removeAll();
   }

   /**
    * Creates and adds a target point
    * 
    * @param source point source to drive to target
    * @return the created target point
    */
   private TargetPoint addTargetPoint (Point source, ComponentList targetList) {
      TargetPoint tpnt = new TargetPoint();
      tpnt.setName(TrackingController.makeTargetName ("p", source, targetList));
      tpnt.setState(source);

      if (source.getRenderProps ()!=null) {
         RenderProps.setSphericalPoints (
            tpnt, source.getRenderProps ().getPointRadius (), Color.CYAN);
      }

      return tpnt;
   }
   
   /**
    * Creates and adds a target frame, returning the created frame to track
    * 
    * @param source to drive toward target 
    * @return the created target frame
    */
   private TargetFrame addTargetFrame (
      Frame source, ComponentList<?> targetList) {

      TargetFrame tframe = new TargetFrame();
      tframe.setPose (source.getPose ());
      tframe.setName (
        TrackingController.makeTargetName ("rb", source, targetList));
      tframe.setState(source);

      // add mesh to TargetFrame
      PolygonalMesh mesh = null;
      if (source instanceof RigidBody) {
         RigidBody sourcebody = (RigidBody)source;
         if ((mesh = sourcebody.getSurfaceMesh()) != null) {
            tframe.setSurfaceMesh (
               mesh.clone (), sourcebody.getSurfaceMeshComp().getFileName());
            tframe.setRenderProps (sourcebody.getRenderProps());
            RenderProps.setDrawEdges (tframe, true);
            RenderProps.setFaceStyle (tframe, FaceStyle.NONE);
         }
      }
      return tframe;
      
   }

   /**
    * Returns list of target points/frames
    */
   public ArrayList<MotionTargetComponent> getTargets() {
      ArrayList<MotionTargetComponent> targets = new ArrayList<>();
      targets.addAll (myTargetPoints);
      targets.addAll (myTargetFrames);
      return targets;
   }
   
   /**
    * Returns list of source points/frames that are part of the model
    * that will move to the targets
    */
   public ArrayList<MotionTargetComponent> getSources() {
      ArrayList<MotionTargetComponent> sources = new ArrayList<>();
      for (ReferenceComp<Point> pr : mySourcePoints) {
         sources.add (pr.getReference());
      }
      for (ReferenceComp<Frame> fr : mySourceFrames) {
         sources.add (fr.getReference());
      }
      return sources;
   }

   public Point getSourcePoint (int idx) {
      return mySourcePoints.get(idx).getReference();
   }

   public Frame getSourceFrame (int idx) {
      return mySourceFrames.get(idx).getReference();
   }

   Vector3d vtmp = new Vector3d();
   Point3d ptmp = new Point3d();
   RigidTransform3d Xtmp = new RigidTransform3d();
   Twist veltmp = new Twist();

   private int getTargetVelSize() {
      return 3*myTargetPoints.size() + 6*myTargetFrames.size();
   }

   private void updateSizes() {
      int vsize = 0;
      for (TargetPoint pt : myTargetPoints) {
         vsize += pt.getVelStateSize();
      }
      for (TargetFrame ft : myTargetFrames) {
         vsize += ft.getVelStateSize();
      }
      myTargetVelSize = vsize;
      myTrackingVel.setSize (vsize);
   }

   public VectorNd getTargetVel(double t0, double t1) {
      updateTarget (t0, t1);
      return myTrackingVel;
   }

   /**
    * Returns the velocity Jacobian, creates if null
    * 
    * XXX This Jacobian needs to be re-computed at each time step
    * OR it needs to be transformed to global co-ordinates so that the
    * tracking controller can use it properly, since it does not change
    * as the model moves
    */
   private SparseBlockMatrix getVelocityJacobian (MechSystemBase mech) {
      // Again, keepVelocityJacobianConstant should be false, unless set true
      // for comparison with legacy code
      if (myVelJacobian == null || !keepVelocityJacobianConstant) {
         createVelocityJacobian (mech);
      }
      return myVelJacobian;
   }

   private void createVelocityJacobian (MechSystemBase mech) {
      myVelJacobian = mech.createVelocityJacobian();

      int i = 0;
      for (ReferenceComp<Point> pr : mySourcePoints) {
         pr.getReference().addTargetJacobian(myVelJacobian, i++);
      }
      for (ReferenceComp<Frame> pr : mySourceFrames) {
         pr.getReference().addTargetJacobian(myVelJacobian, i++);
      }

      // fold attachments into targets on dynamic components 
      // (same as constraint jacobians)
      mech.reduceVelocityJacobian(myVelJacobian);
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
         // updateHb now called in QPSolver
         //updateHb (controller, t0, t1);
         A.setSubMatrix(rowoff, 0, myEffectiveHv);
         b.setSubVector(rowoff, myEffectiveVbar);
         rowoff += myEffectiveHv.rowSize();
      }
      return rowoff;
   }      

   public void updateHb (TrackingController controller, double t0, double t1) {
      double h = TimeBase.round(t1 - t0);

      updateTarget(t0, t1); // set myTargetVel
      
      //VectorNd vbar = new VectorNd (myTargetVelSize);
      int numex = controller.numExciters();
      
      VectorNd v0 = new VectorNd (myTargetVelSize);
      myVbar.setSize (myTargetVelSize);
      myHv.setSize (myTargetVelSize, numex);
      SparseBlockMatrix Jm = getVelocityJacobian(controller.getMech());
      
      VectorNd u0 = controller.getU0();
      Jm.mul(v0, u0, Jm.rowSize(), u0.size());
      VectorNd Hv_j = new VectorNd (myTargetVelSize);
      for (int j=0; j<numex; j++) {
         Jm.mul(Hv_j, controller.getHuCol(j), Jm.rowSize (), u0.size());
         myHv.setColumn (j, Hv_j);
      }
      
      myVbar.sub (myTrackingVel, v0);
      if (controller.getDebug ()) {
         System.out.println (
            "(MotionTargetTerm)");
         System.out.println (
            "\tmyTargetVel: " + myTrackingVel.toString ("%.3f"));
         System.out.println (
            "\tV0: " + v0.toString ("%.3f"));
         System.out.println (
            "\tvbar: " + myVbar.toString ("%.3f"));
      }

      if (controller.getNormalizeCostTerms()) {
         double fn = 1.0 / myHv.frobeniusNorm ();
         myHv.scale (fn);
         myVbar.scale (fn);
      }
      
      if (controller.getUseTimestepScaling()) {
         // makes it independent of the time step
         myHv.scale(1/h);      
         myVbar.scale(1/h); 
      }

      VectorNd weights = collectAllWeights();
      myHv.mulDiagonalLeft (weights);
      mulElements(myVbar,weights,myVbar);

      if (myWeight >= 0) {
          myHv.scale(myWeight);
          myVbar.scale(myWeight);
      }
      //System.out.println (" Hv=\n" + myHv.toString ("%12.8f"));
      //System.out.println (" vbar=  " + myVbar.toString ("%12.8f"));
      if (myHv.rowSize() > 0) {
         
         SVDecomposition svd = null;
         if (reportHvRank || getType() == QPConstraintTerm.Type.EQUALITY) {
            svd = new SVDecomposition();
            svd.factor (myHv, SVDecomposition.FULL_UV);
         }
         if (reportHvRank) {
            System.out.println (
               "Hv size=" + myHv.getSize() + " rank=" + svd.rank(1e-10));
         }
         if (getType() == QPConstraintTerm.Type.EQUALITY) {
            int rank = svd.rank(1e-10);
            if (rank < myTargetVelSize) {
               // need to reduce the number of equations
               System.out.println ("rank=" + rank + "; reducing");
               myEffectiveVbar = new VectorNd(myTargetVelSize);
               myEffectiveHv.setSize (rank, numex);
               VectorNd sig = svd.getS();
               MatrixNd V = svd.getV();
               MatrixNd U = svd.getU();
               VectorNd Vcol = new VectorNd (numex);
               for (int i=0; i<rank; i++) {
                  V.getColumn (i, Vcol);
                  Vcol.scale (sig.get(i));
                  myEffectiveHv.setRow (i, Vcol);
               }
               U.mulTranspose (myEffectiveVbar, myVbar);
               myEffectiveVbar.setSize (rank);
            }
            else {
               myEffectiveVbar = myVbar;
               myEffectiveHv = myHv;
            }
         }
      }
   }
   
   public MatrixNd getH() {
      return myHv;
   }

   public VectorNd getB() {
      return myVbar;
   }

//   /**
//    * Fills <code>H</code> and <code>b</code> with this motion term
//    * In contrast to <code>getTerm</code>, this method does not
//    * recompute the values.
//    */
//   public void reGetTerm(MatrixNd H, VectorNd b) {
//      b.set (myVbar);
//      H.set (myHv);
//   }

   /**
    * Weight used to scale the contribution of this term in the quadratic
    * optimization problem
    */
   @Override
   public void setWeight(double w) {
      super.setWeight(w);
   }

   /**
    * Sets weights for targets.  This allows you to weight more heavily the
    * points you deem to be more important.  If wgts.size() equals the number
    * of targets, each target will use a single weight.  If wgts.size() equals
    * the full size of the target velocity vector, then this directly scales
    * the rows of <code>H</code> (allows you to scale x,y,z components of
    * velocity separately).
    * 
    * @param weights weights vector
    */
   public void setTargetWeights (VectorNd weights) {
      
      if (weights.size() == numTargets()) {
         int k = 0;
         for (TargetPoint tp : myTargetPoints) {
            tp.setWeight (weights.get(k++));
         }
         for (TargetFrame tf : myTargetFrames) {
            tf.setWeight (weights.get(k++));
         }
      }
      else if (weights.size() == getTargetVelSize()) {
         int k = 0;
         for (TargetPoint tp : myTargetPoints) {
            Vector3d subw = new Vector3d();
            subw.x = weights.get(k++);
            subw.y = weights.get(k++);
            subw.z = weights.get(k++);
            tp.setSubWeights (subw);
            tp.setWeight (1.0);
         }
         for (TargetFrame tf : myTargetFrames) {
            VectorNd subw = new VectorNd(6);
            for (int i=0; i<6; i++) {
               subw.set (i, weights.get(k++));
            }
            tf.setSubWeights (subw);
            tf.setWeight (1.0);
         }
      }
      else {
         throw new IllegalArgumentException (
            "Weights vector size should equal number of targets or"+
            "target velocity size");
      }
   }
   
   /**
    * Gets weights for targets. If weights.size() equals the number of targets,
    * returns the set of single weights used per target.  If weights.size() equals
    * the full size of the target velocity vector, then returns the direct
    * scaling vector.
    * 
    * @param weights weights vector
    */
   public void getTargetWeights(VectorNd weights) {
      
      if (weights.size() == numTargets()) {
         weights.set (collectTargetWeights());
      } 
      else if (weights.size() == getTargetVelSize()) {
         weights.set(collectAllWeights());
      }
      else {
         throw new IllegalArgumentException (
            "Weights vector size should equal number of targets or"+
            "target velocity size");
      }
   }

   /**
    * Render props for targets
    */
   public RenderProps getTargetRenderProps() {
      return targetRenderProps;
   }

   /**
    * Sets render props for the target points/frames
    */
   public void setTargetRenderProps(RenderProps rend) {
      targetRenderProps.set (rend);

      myTargetPoints.setRenderProps (targetRenderProps);
      myTargetFrames.setRenderProps (targetRenderProps);
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

      for (ReferenceComp<Point> pr : mySourcePoints) {
         pr.getReference().setRenderProps(sourceRenderProps);
      }
      for (ReferenceComp<Frame> fr : mySourceFrames) {
         fr.getReference().setRenderProps(sourceRenderProps);
      }
   }

   /**
    * Sets the chase time. This is the time over which the controller tries to
    * make the source catch up to the target, if not using PD control. Default
    * value is 0.01. The chase time should generally by {@code >=} the
    * simulation step size.
    *
    * @param t new chase time
    */
   public void setChaseTime (double t) {
      myChaseTime = t;
   }
   
   /**
    * Queries the chase time. See {@link #setChaseTime} for details.
    *
    * @return chase time
    */
   public double getChaseTime() {
      return myChaseTime;
   }

   /**
    * Proportional gain for PD controller
    */
   public void setKp(double k) {
      Kp = k;
   }
   
   /**
    * Returns the Proportional gain for PD controller.  See {@link
    * #setKp(double)}.
    * @return proportional error gain
    */
   public double getKp() {
      return Kp;
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
   

   @Override
   public void getQP (MatrixNd Q, VectorNd p, double t0, double t1) {
      TrackingController controller = getController();
      if (controller != null) {  
         // updateHb now called in QPSolver
         //updateHb (controller, t0, t1);
         computeAndAddQP (Q, p, myHv, myVbar);
      }
   }

   @Override
   public int numConstraints(int qpsize) {
      if (getType() == QPConstraintTerm.Type.EQUALITY) {
         return myEffectiveHv.rowSize();
      }
      else {
         return getTargetVelSize();
      }
   }
   
   public void setUsePDControl(boolean usePD) {
      usePDControl = usePD;
   }

   public boolean getUsePDControl() {
      return usePDControl;
   }

   public void setLegacyControl(boolean enable) {
      myLegacyControl = enable;
   }

   public boolean getLegacyControl() {
      return myLegacyControl;
   }
}
