/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;

import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemModel;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystem;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.MechSystemModel;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ComponentListImpl;
import artisynth.core.modelbase.ComponentState;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ControllerBase;
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.NumericState;
import artisynth.core.modelbase.ReferenceListBase;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.WeightedReferenceComp;
import artisynth.core.util.ScanToken;
import artisynth.core.workspace.RootModel;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.util.ArrayListView;
import maspack.util.DoubleInterval;
import maspack.util.ListView;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Inverse controller for computing muscle activations based on
 * a set of motion and force trajectories, along with with other constraints.
 * <p>
 * 
 * Terminology: <br>
 * <table summary="">
 * <tbody>
 *    <tr><td>"Targets"</td><td>are trajectories of points/frames that we
 *    wish our model to follow</td></tr>
 *    <tr><td>"Sources"</td><td>are the points/frames in the
 *    model we want to follow the target trajectories</td></tr>
 *    <tr><td>"Excitation"</td><td>we actually mean, "activation", since we
 *    are currently ignoring the excitation dynamics</td></tr>
 * </tbody>
 * </table>
 * <p>         
 *              
 * Papers:<br>
 * - Ian Stavness, JE Lloyd, and SS Fels. Automatic Prediction of Tongue Muscle 
 * Activations Using a Finite Element Model. Journal of Biomechanics, 
 * 45(16):2841-2848, 2012.<br>
 * 
 * - Ian Stavness, AG Hannam, JE Lloyd, and SS Fels. Predicting muscle patterns 
 * for hemimandibulectomy models. Computer Methods in Biomechanics and 
 * Biomedical Engineering, 13(4):483-491, 2010.
 * 
 * 
 * @author Ian Stavness, with modifications by Antonio Sanchez
 *
 */
public class TrackingController extends ControllerBase
   implements CompositeComponent, RenderableComponent {

   // set to handle the motion term as a hard constraint
   boolean handleMotionTargetAsConstraint = false;

   // ========= property attributes =========

   public static boolean DEFAULT_ENABLED = true;
   boolean enabled = DEFAULT_ENABLED;

   // Limit per-step change in excitations
   public static double DEFAULT_MAX_EXCITATION_JUMP = 1;
   protected double maxExcitationJump = DEFAULT_MAX_EXCITATION_JUMP;    

   // Controls rendering visibility of sources and targets
   public static boolean DEFAULT_TARGETS_VISIBLE = true;
   protected boolean targetsVisible = true;
   public static boolean DEFAULT_SOURCES_VISIBLE = true;
   protected boolean sourcesVisible = true;

   public static final double DEFAULT_PROBE_DURATION = 1.0;
   double myProbeDuration = DEFAULT_PROBE_DURATION;

   public static final double DEFAULT_PROBE_INTERVAL = -1;
   double myProbeUpdateInterval = DEFAULT_PROBE_INTERVAL;

   // enable debug messages
   public static final boolean DEFAULT_DEBUG = false;
   protected boolean debug = false;

   // divide H and b terms of motion and force target terms by the time step
   public static boolean DEFAULT_USE_TIMESTEP_SCALING = false;
   protected boolean myUseTimestepScaling = DEFAULT_USE_TIMESTEP_SCALING;

   // factor KKT system when solving for velocity response of each excitation   
   public static boolean DEFAULT_USE_KKT_FACTORIZATION = false;
   protected boolean myUseKKTFactorization = DEFAULT_USE_KKT_FACTORIZATION;

   // normalize H and b terms of motion and force target terms
   public static boolean DEFAULT_NORMALIZE_H = false;
   protected boolean myNormalizeH = DEFAULT_NORMALIZE_H;

   // default bounds for the excitation values
   public static DoubleInterval DEFAULT_EXCITATION_BOUNDS =
      new DoubleInterval ("[-inf,inf])");
   private DoubleInterval myExcitationBounds =
      new DoubleInterval(DEFAULT_EXCITATION_BOUNDS);
   private PropertyMode myExcitationBoundsMode = PropertyMode.Inherited;

   private static boolean DEFAULT_COMPUTE_INCREMENTALLY = false;
   private boolean myComputeIncrementally = DEFAULT_COMPUTE_INCREMENTALLY;

   // ========= other parameter attributes =========

   // 1 for true, 0 for false, -1 for automatic
   public static int DEFAULT_USE_TRAPEZOIDAL_SOLVER = -1;
   protected int myUseTrapezoidalSolver = DEFAULT_USE_TRAPEZOIDAL_SOLVER;

   private static double DEFAULT_TARGETS_POINT_RADIUS = 1d;
   protected double targetsPointRadius = DEFAULT_TARGETS_POINT_RADIUS;

   protected MechSystemBase myMech;
   protected QPSolver myQPSolver;
   //protected MotionForceInverseData myMotionForceData;
   protected ExcitationResponse myExcitationResponse;

   protected MotionTargetTerm myMotionTerm = null;  // contains target information
   protected ForceTargetTerm myForceTerm = null;  // contains target information
   // contains target information
   protected ForceEffectorTerm myForceEffectorTerm = null;  
   protected DampingTerm myDampingTerm = null;

   protected L2RegularizationTerm myRegularizationTerm = null;
   protected BoundsTerm myBoundsTerm = null;
   //protected NonuniformBoundsTerm myOffsetBoundsTerm = null;
   
   // contains child components and implements CompositeComponent methods
   protected ComponentListImpl<ModelComponent> myComponents;
   
   // list of exciter components
   protected ReferenceListBase<ExcitationComponent,ExciterComp> myExciters;

   /*
    * Weights used to emphasize or de-emphasize certain excitation components,
    * by altering the component regularization term associated with that
    * excitation component.
    */
   protected VectorNd excitationRegularizationWeights;
   public static final double DEFAULT_EXCITATION_REGULARIZATION_WEIGHT = 1.0;
   
   protected VectorNd myExcitations = new VectorNd();    // computed excitatios
   // previous, for damping terms:
   protected VectorNd prevExcitations = new VectorNd(); 
   // initial, in case non-zero start (again, for damping)
   protected VectorNd initExcitations = new VectorNd();  



   // ========== Begin property definitions and methods ==========

   public static PropertyList myProps =
      new PropertyList(TrackingController.class, ControllerBase.class);

   static {
      myProps.add("renderProps", "render properties", null);
      //myProps.add("enabled isEnabled", "enable/disable controller", true);
      myProps.add(
         "maxExcitationJump", "maximum excitation step",
         DEFAULT_MAX_EXCITATION_JUMP, "[0,1]");
      myProps.add(
         "targetsVisible", "allow showing or hiding of motion targets",
         true);
      myProps.add(
         "sourcesVisible", "allow showing or hiding of motion markers",
         true);
      myProps.add(
         "probeDuration", "duration of inverse managed probes",
         DEFAULT_PROBE_DURATION);
      myProps.add(
         "probeUpdateInterval", "update interval of inverse managed probes",
         DEFAULT_PROBE_INTERVAL);
      myProps.add (
         "debug", "enables output of debug info to the console",
         DEFAULT_DEBUG);
      myProps.add(
         "useTimestepScaling", "flag for scaling motion term H and vbar by 1/h", 
         DEFAULT_USE_TIMESTEP_SCALING);
      myProps.add(
         "useKKTFactorization",
         "flag for re-factoring at each internal KKT solve",
         DEFAULT_USE_KKT_FACTORIZATION);
      myProps.add(
         "normalizeH", "normalize contribution by frobenius norm",
         DEFAULT_NORMALIZE_H);
      myProps.addInheritable (
         "excitationBounds", "bounds for the computed excitations",
         DEFAULT_EXCITATION_BOUNDS);
      myProps.add(
         "computeIncrementally",
         "compute excitations incrementally at each time step",
         DEFAULT_COMPUTE_INCREMENTALLY);
   }

   /**
    * {@inheritDoc}
    */
   public PropertyList getAllPropertyInfo() {
     return myProps;
   }

   /**
    * Queries whether or not this controller is enabled.
    *
    * @return {@code true} if this controller is enabled
    */
   public boolean isEnabled() {
      return enabled;
   }

//   /**
//    * Sets whether or not this controller is enabled.
//    *
//    * @param enabled if {@code true}, enables this controller
//    */
//   public void setEnabled(boolean enabled) {
//      this.enabled = enabled;
//   }

   /**
    * Queries the maximum jump in excitation values permitted between steps.
    *
    * @return maximum jump in excitation values
    */
   public double getMaxExcitationJump() {
      return maxExcitationJump;
   }

   /**
    * Sets the maximum jump in excitation values permitted between steps.  The
    * default value is 1, which generally corresponds to no limit since
    * excitations are usually bounded in the interval {@code [0,1]}.
    *
    * @param maxJump maximum jump in excitation values
    */
   public void setMaxExcitationJump (double maxJump) {
      maxExcitationJump = maxJump;
   }

   /**
    * Queries whether the motion targets are being rendered.
    *
    * @return {@code true} if motion target rendering is enabled
    */
   public boolean getTargetsVisible() {
      return targetsVisible;
   }

   /**
    * Enables rendering of the motion targets. Target rendering is enabled by
    * default.
    *
    * @param show if {@code true}, enables motion target rendering
    */
   public void setTargetsVisible (boolean show) {
      ArrayList<MotionTargetComponent> moTargetParticles =
         myMotionTerm.getTargets();
      for (MotionTargetComponent p : moTargetParticles) {
         if (p instanceof RenderableComponent) {
            RenderProps.setVisible((RenderableComponent)p, show);
         }
      }
      targetsVisible = show;
   }

   /**
    * Queries whether the motion sources are being rendered.
    *
    * @return {@code true} if motion source rendering is enabled
    */
   public boolean getSourcesVisible() {
      return sourcesVisible;
   }

   /**
    * Enables rendering of the motion sources. Source rendering is enabled by
    * default.
    *
    * @param show if {@code true}, enables motion source rendering
    */
   public void setSourcesVisible(boolean show) {
      for (MotionTargetComponent p : myMotionTerm.getSources()) {
         if (p instanceof RenderableComponent) {
            RenderProps.setVisible((RenderableComponent)p, show);
         }
      }
      sourcesVisible = show;
   }

   /**
    * Queries the probe duration used by {@link #createProbes(RootModel)} and
    * {@link #createProbesAndPanel(RootModel)}.
    *
    * @return probe duration used when creating probes
    */
   public double getProbeDuration() {
      return myProbeDuration;
   }
   
   /**
    * Sets the probe duration used by {@link #createProbes(RootModel)} and
    * {@link #createProbesAndPanel(RootModel)}.
    *
    * @param duration probe duration to be used for subsequently created probes
    */
   public void setProbeDuration(double duration) {
      myProbeDuration = duration;
   }

   /**
    * Queries the output probe update interval used by {@link
    * #createProbes(RootModel)} and {@link #createProbesAndPanel(RootModel)}.
    *
    * @return output probe update interval
    */
   public double getProbeUpdateInterval() {
      return myProbeUpdateInterval;
   }

   /**
    * Sets the output probe update interval used by {@link
    * #createProbes(RootModel)} and {@link #createProbesAndPanel(RootModel)}.
    * A value of {@code -1} causes the probes to be updated at the
    * current simulation rate.
    *
    * @param h new output probe update interval
    */
   public void setProbeUpdateInterval(double h) {
      myProbeUpdateInterval = h;
   }
   

   /**
    * Queries whether or not debug messages are enabled.
    *
    * @return {@code true} if debug messages are enabled
    */
   public boolean getDebug() {
      return debug;
   }

   /**
    * Enables the printing of debug messages. This is disabled by default.
    *
    * @param enable if {@code true}, enables debug messages
    */
   public void setDebug(boolean enable) {
      debug = enable;
   }

   /**
    * Queries whether or not timestep scaling is enabled, as
    * described in {@link #setUseTimestepScaling(boolean)}.
    * 
    * @return {@code true} if timestep scaling is enabled
    */
   public boolean getUseTimestepScaling () {
      return myUseTimestepScaling;
   }

   /**
    * Enables timestep scaling. If enabled, then the {@code H} matrix and
    * {@code b} vector of the motion and force tracking terms are scaled by
    * {@code 1/h}, where {@code h} is the simulation step size, thus making
    * these terms independent of the step size.
    * <p>
    * Timestep scaling is disabled by default.
    * 
    * @param enable if {@code true}, enables timestep scaling
    */
   public void setUseTimestepScaling (boolean enable) {
      myUseTimestepScaling = enable;
   }

   /**
    * Queries whether or not excitations are computed incrementally at each
    * time step, as described in {@link #setComputeIncrementally(boolean)}.
    * 
    * @return {@code true} if excitations are computed incrementally
    */
   public boolean getComputeIncrementally () {
      return myComputeIncrementally;
   }

   /**
    * Enables incremental computation. If enabled, then excitations are
    * computed incrementatlly (as opposed to holistically) at each time step.
    * The various QPTerms used by the controller should adjust their
    * contributions to the quadratic program to reflect an incemental
    * computation instead of a holistic one.  Incremental computation is
    * disabled by default.
    * 
    * @param enable if {@code true}, enables incremental computation
    */
   public void setComputeIncrementally (boolean enable) {
      if (myComputeIncrementally != enable) {
         myComputeIncrementally = enable;
      }
   }

   /**
    * Queries whether or not KKT factorization is enabled, as described in
    * {@link #setUseKKTFactorization(boolean)}.
    *
    * @return {@code true} if KKT factorization is enabled
    */
   public boolean getUseKKTFactorization () {
      return myUseKKTFactorization;
   }

   /**
    * Enables or disables KKT factorization. If enabled, the KKT system of the
    * mechanical model being controlled is refactored when calculating each
    * excitation's velocity response. This increases computation time but gives
    * more accurate and stable results.
    *
    * <p> KKT factorization is disabled by default.
    * 
    * @param enable if {@code true}, enables KKT factorization
    */
   public void setUseKKTFactorization (boolean enable) {
      myUseKKTFactorization = enable;
   }
   
   /**
    * Queries whether or not H normalization is enabled, as described in {@link
    * #setNormalizeH(boolean)}.
    *
    * @return {@code true} if H normalization is enabled
    */
   public boolean getNormalizeH() {
      return myNormalizeH;
   }

   /**
    * Enabled H normalization for any motion and force target terms.  This
    * involves normalizing the {@code H} matrix and {@code b} vector for these
    * terms by the Frobenius norm of the {@code H} matrix.
    * <p>
    * H normalization is disabled by default.
    * 
    * @param enable if <code>true</code>, enables normalization
    */
   public void setNormalizeH (boolean enable) {
      myNormalizeH = enable;
   }
   
   /**
    * Queries the current default bounds for the excitation values.
    *
    * @return default excitation bounds
    */
   public DoubleInterval getExcitationBounds () {
      return myExcitationBounds;
   }

   /**
    * Sets the default bounds for the excitation values. By default, the bounds
    * are set to {@code [-inf,inf]}, correspnding to no effective bounds.
    *
    * @param bounds default excitation bounds
    */
   public void setExcitationBounds (DoubleInterval bounds) {
      myExcitationBounds.set (bounds);
      myExcitationBoundsMode = PropertyUtils.propagateValue (
         this, "excitationBounds", bounds, myExcitationBoundsMode);
   }

   /**
    * Sets the default bounds for the excitation values. Same functionality as
    * {@link #setExcitationBounds(DoubleInterval)}.
    *
    * @param lower lower excitation bound 
    * @param upper upper excitation bound 
    */
   public void setExcitationBounds (double lower, double upper) {
      setExcitationBounds (new DoubleInterval(lower, upper));
   }

   /**
    * Queries the property inheritance mode for the excitation bounds.
    *
    * @return property inheritance mode for excitations
    */
   public PropertyMode getExcitationBoundsMode() {
      return myExcitationBoundsMode;
   }

   /**
    * Sets the property inheritance mode for the excitation bounds.
    *
    * @param mode property inheritance mode for excitations
    */
   public void setExcitationBoundsMode (PropertyMode mode) {
      myExcitationBoundsMode =
         PropertyUtils.setModeAndUpdate (
            this, "excitationBounds", myExcitationBoundsMode, mode);
   }
   
   // ========== Begin other parameter methods ==========
   
   /**
    * Queries the use of a trapezoidal solver for setting up the excitation
    * solve. See {@link #setUseTrapezoidalSolver}.
    *
    * @return code indicating the use of a trapezoidal solver
    */
   public int getUseTrapezoidalSolver () {
      return myUseTrapezoidalSolver;
   }

   /**
    * Sets when to use a trapezoidal solver for setting up the excitation
    * solve. {@code code = 1} means always, {@code code = 0} means never, and
    * {@code code = -1} means automatic (match the solver being used by the
    * underlying mechanical system).
    *
    * @param code controls when to use a trapezoidal solver
    */
   public void setUseTrapezoidalSolver (int code) {
      myUseTrapezoidalSolver = code;
   }


   /**
    * Returns whether the controller should use a trapezoidal solver in solving
    * for the incremental velocity changes associated with each excitation.
    * This depends on the setting of the {@code useTrapezoidalSolver} property,
    * or if that property has a value of -1, the integrator being used by the
    * underlying mechanical system.
    *
    * @return whether the controller should use a trapezoidal solver
    */
   protected boolean useTrapezoidalSolver() {
      switch (getUseTrapezoidalSolver()) {
         case 0: {
            return false;
         }
         case 1: {
            return true;
         }
         default: {
            if (myMech instanceof MechModel) {
               return
                  ((MechModel)myMech).getIntegrator()==Integrator.Trapezoidal;
            }
            else {
               return false;
            }
         }
      }
   }

   /**
    * Queries if recomputation of the velocity Jacobian has been disabled, as
    * described in {@link #setKeepVelocityJacobianConstant(boolean)}.
    * 
    * @return {@code true} if velocity Jacobian recomputation is disabled
    */
   public boolean getKeepVelocityJacobianConstant() {
      return myMotionTerm.keepVelocityJacobianConstant;
   }

   /**
    * Disables recomputation of the velocity Jacobian. This actually gives
    * incorrect results and is provided for comparison with legacy code only.
    *
    * @param enable if {@code true}, disables velocity Jacobian recomputation
    */
   public void setKeepVelocityJacobianConstant (boolean enable) {
      myMotionTerm.keepVelocityJacobianConstant = enable;
   }

   /**
    * Set the mechanical system being controlled. This is intended for internal
    * use; applications will normally specify the mechanical system in the
    * constructor.
    *
    * @param mech mechanical system being controlled
    */
   public void setMech (MechSystemBase mech) {
      setModel (mech);
      myMech = mech;
   }

   /**
    * Queries the mechanical system being controlled.
    *
    * @return mechanical system being controlled.
    */
   public MechSystemBase getMech() {
      return myMech;
   }
   
   /**
    * Queries the integrator used by the mechanical system.
    *
    * @return integrator used by the mechanical system, or {@code null} if
    * there is no system.
    */
   public Integrator getIntegrator() {
      if (myMech instanceof MechModel)
         return ((MechModel)myMech).getIntegrator();
      else 
         return null;
   }
   
   // ========== Begin constructors ==========

   /**
    * Creates an empty tracking controller for scanning purposes.
    */
   public TrackingController () {
      super();
      initComponents();
   }

   /**
    * Creates a tracking controller for a given mechanical system.
    * 
    * @param m mech system, typically your "MechModel"
    */
   public TrackingController (MechSystemBase m) {
      this(m, null);
   }

   /**
    * Creates and names a tracking controller for a given mechanical system
    * 
    * @param m mech system, typically your "MechModel"
    * @param name name to give the controller
    */
   public TrackingController (MechSystemBase m, String name) {
      super();
      setMech(m);
      setName(name);

      initComponents();
   }

   protected void initComponents() {
      
      myQPSolver = new QPSolver();
      
      myComponents =
         new ComponentListImpl<ModelComponent> (ModelComponent.class, this);

      // list of excitations that store the computed excitations from the
      // tracking simulation
      myExciters =
         new ReferenceListBase<> (ExciterComp.class, "exciters");
      // always show this component, even if it's empty:
      myExciters.setNavpanelVisibility (NavpanelVisibility.ALWAYS);
      myExciters.setFixed (true);
      add (myExciters);

      myMotionTerm = new MotionTargetTerm (this);
      myMotionTerm.setName ("motionTerm");
      myMotionTerm.setFixed (true);
      myMotionTerm.setInternal (true);
      add (myMotionTerm);
      if (handleMotionTargetAsConstraint) {
         myMotionTerm.setType (QPTerm.Type.EQUALITY);
         // must have a regularization term in the cost function if the 
         // motion term is constraint
         addL2RegularizationTerm();
      }
      excitationRegularizationWeights = new VectorNd();
      
      //myMotionForceData = new MotionForceInverseData (this);
      myExcitationResponse = new ExcitationResponse (this);
      
      setExcitationBounds(0d, 1d);
      myBoundsTerm = new BoundsTerm ("boundsTerm");
      //myCostFunction.addInequalityConstraint (myBoundsTerm);
      myBoundsTerm.setFixed (true);
      myBoundsTerm.setInternal (true);
      add (myBoundsTerm);
   }

   // ========== Begin InverseManager methods ==========

   /**
    * Convenience method that clears all existing probes and then creates a set
    * of probes for controlling and monitoring the tracking controller.  The
    * probes start at time 0 and have a duration given by the controller's
    * {@code probeDuration} property, while output probes are given an update
    * interval specified by the controller's {@code probeUpdateInterval}
    * property.
    *
    * <p>The probes are created using the {@link InverseManager}.
    * 
    * @param root root model to which the probes should be added
    * @see #getProbeDuration
    * @see #getProbeUpdateInterval
    */
   public void createProbes (RootModel root) {
      InverseManager.resetProbes (
         root, this, getProbeDuration(), getProbeUpdateInterval());
   }
   
   /**
    * Convenience method that clears all existing probes and then creates a set
    * of probes for controlling and monitoring the tracking controller. A
    * description of the created probes is given in the documentation header
    * for {@link InverseManager}.
    *
    * <p>The probes start at time 0 and stop at the time indicated by {@code
    * duration}. The output probes are updated at the specified {@code
    * interval}; if specified as {@code -1}, then the output probes are updated
    * at the current simulation sample rate.
    *
    * <p>The probes are created using the {@link InverseManager}.
    *
    * @param root root model to which the probes should be added
    * @param duration time duration for each probe
    * @param interval update interval for the output probes
    */
   public void createProbes (RootModel root, double duration, double interval) {
      InverseManager.resetProbes (root, this, duration, interval);
   }
   
   /**
    * Convenience method that creates a control panel for the tracking
    * controller.
    *
    * @param root root model to which the panel should be added
    */
   public void createPanel (RootModel root) {
      InverseManager.addInversePanel (root, this);
   }
   
   /**
    * Convenience method that creates both a set of probes and a control panel
    * for the tracking controller, combining both {@link
    * #createProbes(RootModel)} and {@link #createPanel}.
    * 
    * @param root root model to which the probes and panel should be added
    */
   public void createProbesAndPanel (RootModel root) {
      InverseManager.resetProbes (
         root, this, getProbeDuration(), getProbeUpdateInterval()); 
      InverseManager.addInversePanel (root, this);
   }

   /**
    * Convenience method that creates both a set of probes and a control panel
    * for the tracking controller, combining both {@link
    * #createProbes(RootModel,double,double)} and {@link #createPanel}.
    * 
    * @param root root model to which the probes and panel should be added
    * @param duration time duration for each probe
    * @param interval update interval for the output probes
    */
   public void createProbesAndPanel (
      RootModel root, double duration, double interval) {
      InverseManager.resetProbes (root, this, duration, interval);
      InverseManager.addInversePanel (root, this);
   }
   
//   public void setTargetPositionFilename(String filename) {
//      Main.getMain ().getInverseManager ().setTargetPositionFilename (filename);
//   }
//   
   // ========== Begin internal methods ==========
   
   private void recursivelyInvalidateFEMStresses(ModelComponent model) {
      // recursively look for FEM models
      if (model instanceof FemModel) {
         ((FemModel)model).invalidateStressAndStiffness ();
      } 
      else if (model instanceof ComponentList<?>) {
         for (ModelComponent mc : (ComponentList<?>)model) {
            recursivelyInvalidateFEMStresses (mc);
         }
      }
   }
   
   protected void invalidateFEMStresses(MechSystemModel model) {
      recursivelyInvalidateFEMStresses (model);
   }

   protected void updateCostTerms(double t0, double t1) {
      if (t0 == 0) { // XXX need better way to zero excitations on reset
         //myCostFunction.setSize (numExciters());
         myExcitations = new VectorNd (numExciters());
      }
      double h = t1-t0;
      
      prevExcitations.set(myExcitations);
      //myMotionForceData.update(t0, t1);
      myExcitationResponse.update(t0, t1);
   }
   
   /**
    * Applies the controller, estimating and setting the next
    * set of muscle activations
    */
   public void apply(double t0, double t1) {
//      System.out.println("dt = "+(t1-t0)+"     h = "+ TimeBase.round(t1 - t0));
      if (getDebug()) {
         System.out.println ("\n--- t = " + t1 + " ---"); // cleans up the console
      }

//      if (!isEnabled()) {
//         return;
//      }

      VectorNd savedForces = new VectorNd();
      myMech.getForces (savedForces);

      updateCostTerms (t0, t1);

      // collect cost and constraint terms from among the child components
      ArrayList<QPCostTerm> costs = new ArrayList<>();
      ArrayList<QPConstraintTerm> constraints = new ArrayList<>();
      costs.addAll (getCostTerms());
      constraints.addAll (getInequalityConstraints());
      constraints.addAll (getEqualityConstraints());

      // solve for the excitations, given the cost and constraint terms
      VectorNd x = myQPSolver.solve (costs, constraints, numExciters(), t0, t1);

      if (myComputeIncrementally) {

         // VectorNd deltaActivations = myCostFunction.solve (t0, t1);
         // myExcitations.add (deltaActivations);
         myExcitations.add (x);
         if (getDebug()) {
            System.out.println (
               "da = [" + x.toString ("%.4f") + "];");
         }
      }
      else {
         // myExcitations.set (myCostFunction.solve (t0, t1));
         myExcitations.set (x);
      }

      setExcitations(myExcitations, 0);
      myMech.setForces (savedForces);
   }

   /**
    * Clears all terms and disposes storage
    */
   public void dispose() {
//      System.out.println("tracking controller dispose()");
      myMotionTerm = null;
      myRegularizationTerm = null;
      myBoundsTerm = null;
      //myCostFunction.dispose();
//      targetPoints.clear ();
//      remove (targetPoints);
//      targetFrames.clear ();
//      remove (targetFrames);
//      sourcePoints.clear ();
//      remove (sourcePoints);
//      sourceFrames.clear ();
//      remove (sourceFrames);
      
      myExciters.clear();

      excitationRegularizationWeights = null;
//      upperExcitationBounds.clear ();
//      lowerExcitationBounds.clear ();
   }

   /**
    * Updates the force values in the model for a given set of
    * excitation values. 
    * 
    * @param t time at which forces are set
    * @param forces if non-null, returns the active forces
    * @param excitations input muscle excitations
    */
   void updateForces(double t, VectorNd forces, VectorNd excitations) {
      setExcitations(excitations, /* idx= */0);
      myMech.updateForces (t);
      if (forces != null) {
         myMech.getActiveForces(forces);
      }
   }

   /**
    * Updates constraints in the mech system at time t, including
    * contacts
    */
   void updateConstraints(double t) {
      myMech.updateConstraints(t, null, MechSystem.UPDATE_CONTACTS);
   }

   // ========== Begin exciter methods ==========

   /**
    * Adds an excitation component to the set of exciters that can be used by
    * the controller.
    * 
    * @param ex exciter to add
    */
   public void addExciter (ExcitationComponent ex) {
      addExciter (DEFAULT_EXCITATION_REGULARIZATION_WEIGHT, ex);
   }
   
   /**
    * Adds an excitation component to the set of exciters that can be used by
    * the controller.
    * 
    * @param weight regularization weight to be applied to the exciter
    * @param ex exciter to add
    */
   public void addExciter(double weight, ExcitationComponent ex) {
      myExciters.add (new ExciterComp (ex, weight));
      // keep size of myExcitations synced with number of exciters
      myExcitations.append (0); 
      
      if (ex instanceof MultiPointMuscle) {
         MultiPointMuscle m = (MultiPointMuscle)ex;
         if (m.getExcitationColor() == null) {
            RenderProps.setLineColor(m, Color.WHITE);
            m.setExcitationColor(Color.RED);
         }
      }
      else if (ex instanceof Muscle) {
         Muscle m = (Muscle)ex;
         if (m.getExcitationColor() == null) {
            RenderProps.setLineColor(m, Color.WHITE);
            m.setExcitationColor(Color.RED);
         }
      }
   }
   
   /**
    * Returns the idx-th excitation component used by the controller.
    *
    * @param idx index of the desired exciter 
    * @return idx-th excitation component
    */
   public ExcitationComponent getExciter (int idx) {
      return myExciters.get(idx).getReference();
   }

   /**
    * Returns the number of excitation components used by the controller.
    *
    * @return number of exciters
    */
   public int numExciters() {
      return myExciters.size();
   }

   /**
    * Removes all exciters
    */
   public void clearExciters() {
      myExciters.clear();
      myExcitations.setSize(0);
   }

   /**
    * Gets the list of the excitation components used by the inverse routine
    *
    * @return list of excitation components
    */
   public ListView<ExcitationComponent> getExciters() {
      ArrayListView<ExcitationComponent> view = new ArrayListView<>();
      for (WeightedReferenceComp<ExcitationComponent> ecomp : myExciters) {
         view.add (ecomp.getReference());
      }
      return view;
   }
   

   /** 
    * Sets the excitation bounds for a specific excitation component. By
    * default, bounds for a specific exciter are inherited from the controller.
    *
    * @param ex excitation component to set bounds for
    * @param lower lower excitation bound
    * @param upper upper excitation bound
    */
   public void setExcitationBounds (
      ExcitationComponent ex, double lower, double upper) {
      int idx = myExciters.indexOfReference (ex);
      if (idx != -1) {
         myExciters.get(idx).setExcitationBounds (
            new DoubleInterval (lower, upper));
      }
   }

   /**
    * Sets excitations provided in the <code>ex</code> vector starting at index
    * <code>idx</code>.
    *
    * @param ex vector of excitations to use
    * @param idx start index
    * @return current
    * index in the <code>ex</code> buffer <code>idx+numExciters()</code>
    */
   public int setExcitations(VectorNd ex, int idx) {
      if (myExcitations.size() < numExciters())  {
         myExcitations.setSize (numExciters());
      }
      double[] buf = ex.getBuffer();
      for (WeightedReferenceComp<ExcitationComponent> ecomp : myExciters) {
         ecomp.getReference().setExcitation(buf[idx++]);
      }
      // Stresses in FEM models may depend on excitations
      invalidateFEMStresses (myMech);
      return idx;
   }

   /**
    * Fills the supplied <code>ex</code> vector with current excitation
    * values starting at index <code>idx</code>
    * @param ex vector of excitations to fill
    * @param idx starting index
    * @return next index to use <code>idx+numExciters()</code>
    */
   public int getExcitations (VectorNd ex, int idx) {
      double[] buf = ex.getBuffer();
      for (int i = 0; i < numExciters(); i++) {
         buf[idx++] = getExciter(i).getNetExcitation ();
      } 
      return idx;
   }

   /**
    * Returns a vector containing all the current excitation values.  The
    * vector has size {@code numExciters()} and must not be modified.
    *
    * @return vector of current excitation values
    */
   public VectorNd getExcitations () {
      // resize, just in case
      if (myExcitations.size() != numExciters()) {
         myExcitations.setSize (numExciters());
      }
      return myExcitations;
   }

   /**
    * Queries the current excitation value of the <code>k</code>-th
    * exciter. {@code k} must be in the range {@code 0} to {@code
    * numExciters()-1}.
    *
    * @param k index of the excitation value to query
    * @return {@code k}-th excitation value
    */
   public double getExcitation (int k) {
      return getExciter(k).getNetExcitation();
   }

   /**
    * Initialize the controller with the current excitations in the
    * model, allows for non-zero starting excitations
    */
   public void initializeExcitations() {
      for (int i=0; i<myExciters.size (); i++) {
         double val = myExciters.get(i).getReference().getExcitation ();
         prevExcitations.set(i,val);
         myExcitations.set(i,val);
         initExcitations.set(i,val);
      }
   }
   
   /**
    * Sets initial excitations to the supplied values.
    * 
    * @param init initial excitation values
    */
   public void setInitialExcitations(VectorNd init) {
      if (init.size() != myExcitations.size()) {
         throw new IllegalArgumentException("Wrong number of excitations");
      }
      initExcitations.set(init);
   }
   
   // ========== Begin motion term methods ==========

   /**
    * Returns the standard motion term for this controller, responsible for
    * motion tracking.
    * 
    * @return standard motion term
    */
   public MotionTargetTerm getMotionTargetTerm() {
      return myMotionTerm;
   }
  
   /**
    * Adds a source for the tracking and creates a corresponding target point
    * or frame object
    * @param source point or frame to track
    * @return the created target point/frame
    */
   public MotionTargetComponent addMotionTarget(MotionTargetComponent source) {
      if (sourcesVisible) {
         if (source instanceof RenderableComponent) {
            RenderProps.setVisible((RenderableComponent)source, true);
         }
      }
      return myMotionTerm.addTarget(source);
   }
   
   public void removeMotionTarget(MotionTargetComponent source) {
      myMotionTerm.removeTarget(source);
   }

   /**
    * Adds a source for the tracking and creates a corresponding target point
    * or frame object
    * @param source point or frame to track
    * @param weight the weight by which to scale this target's contribution
    * @return the created target point/frame
    */
   public MotionTargetComponent addMotionTarget (
      MotionTargetComponent source, double weight) {
      if (sourcesVisible) {
         if (source instanceof RenderableComponent) {
            RenderProps.setVisible((RenderableComponent)source, true);
         }
      }
      return myMotionTerm.addTarget(source, weight);
   }

   /**
    * Sets the sphere radius used for rendering target points.
    *
    * @param radius sphere radius for rendering target points
    */
   public void setTargetsPointRadius(double radius) {
      if (myMotionTerm != null) {
         myMotionTerm.setTargetsPointRadius(radius);
      }
      targetsPointRadius = radius;
   }

   /**
    * Returns the motion target components. These are created internally by the
    * controller, and are used to store the target positions that their
    * corresponding source components should follow.
    * 
    * @return list of motion target components. Should not be modified.
    */
   public ArrayList<MotionTargetComponent> getMotionTargets() {
      if (myMotionTerm == null) {
         return null;
      }
      return myMotionTerm.getTargets();
   }

   /**
    * Returns the motion source components. These are the model components that
    * should follow the target positions contained in their corresponding
    * target components.
    * 
    * @return list of motion source components. Should not be modified.
    */
   public ArrayList<MotionTargetComponent> getMotionSources() {
      if (myMotionTerm == null) {
         return null;
      }
      return myMotionTerm.getSources();
   }

   /**
    * Sets the render properties used for rendering target and source components.
    * 
    * @param targets render properties for target components
    * @param sources render properties for source components
    */
   public void setMotionRenderProps(RenderProps targets, RenderProps sources) {
      if (myMotionTerm != null) {
         myMotionTerm.setTargetRenderProps(targets);
         myMotionTerm.setSourceRenderProps(sources);
      }
   }

   /**
    * Returns a vector of the individual weights for each motion target
    * component.
    *
    * @return weights for each motion target
    */
   public VectorNd getMotionTargetWeights() {
      if (myMotionTerm == null) {
         return null;
      }
      return myMotionTerm.collectTargetWeights ();
   }
   
   /**
    * Sets the weight for a specific motion target.
    * 
    * @param comp motion target whose weight is to be set
    * @param w weight
    */
   public void setMotionTargetWeight (MotionTargetComponent comp, double w) {
      if (myMotionTerm == null) {
         return;
      }
      VectorNd wvec = myMotionTerm.collectTargetWeights ();
      int idx = myMotionTerm.getTargets ().indexOf (comp);
      if (idx == -1) {
         idx = myMotionTerm.getSources ().indexOf (comp);
      }
      if (idx != -1 && idx < wvec.size ()) {
         wvec.set (idx, w);
         myMotionTerm.setTargetWeights (wvec);
      }
   }
   
   /**
    * Returns a list of all points which are being used as motion targets.
    *
    * @return list of motion target points
    */
   public PointList<TargetPoint> getTargetPoints() {
      return myMotionTerm.getTargetPoints();
   }

   // ========== Begin other standard term methods ==========

  /**
    * Returns the standard excitation bounds term for this controller.
    * 
    * @return standard excitation bounds term
    */
   public BoundsTerm getBoundsTerm() {
      return myBoundsTerm;
   }
   
   /**
    * Adds a standard L2 regularization term to this controller, if it is not
    * already present. Returns a reference to the term regardless.
    * 
    * @return standard L2 regularization term
    */
   public L2RegularizationTerm addL2RegularizationTerm() {
      if (myRegularizationTerm == null) {
         L2RegularizationTerm rterm =
            new L2RegularizationTerm("L2RegularizationTerm");
         rterm.setInternal (true);
         add (rterm);
      }
      return myRegularizationTerm;
   }
   
   /**
    * Adds a standard L2 regularization term to this controller, if it is not
    * already present. Sets the weight of the term and returns a reference to
    * it regardless.
    *
    * @param w weight for the L2 regularization term
    */
   public L2RegularizationTerm addL2RegularizationTerm(double w) {
      L2RegularizationTerm l2reg = addL2RegularizationTerm ();
      l2reg.setWeight (w);
      return l2reg;
   }

   /**
    * Returns the standard L2 regularization term, or {@code null} if it is not
    * present.
    * 
    * @return standard L2 regularization term or {@code null}
    */
   public L2RegularizationTerm getL2RegularizationTerm() {
      return myRegularizationTerm;
   }

   /**
    * Removes the standard L2 regularization term if it is present.
    * 
    * @return {@code true} if the standard L2 regularization term was removed
    */
   public boolean removeL2RegularizationTerm() {
      if (myRegularizationTerm != null) {
         remove (myRegularizationTerm);
         return true;
      }
      else {
         return false;
      }
   }   

   /**
    * Adds a standard force target term to this controller, if it is not
    * already present. Returns a reference to the term regardless.
    * 
    * @return standard force target term
    */
   public ForceTargetTerm addForceTargetTerm () {
      if (myForceTerm == null) {
         ForceTargetTerm fterm = new ForceTargetTerm("forceTerm");
         fterm.setInternal (true);
         add (fterm);
      }
      return myForceTerm;
   }
   
   /**
    * Returns the standard force target term, or {@code null} if it is not
    * present.
    * 
    * @return standard force target term or {@code null}
    */
   public ForceTargetTerm getForceTargetTerm() {
      return myForceTerm;
   }
   
   /**
    * Removes the standard force target term if it is present.
    * 
    * @return {@code true} if the standard force target term was removed
    */   
   public boolean removeForceTargetTerm () {
      if (myForceTerm != null) {
         remove (myForceTerm);
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Adds a standard force effector term to this controller, if it is not
    * already present. Returns a reference to the term regardless.
    * 
    * @return standard force effector term
    */
   public ForceEffectorTerm addForceEffectorTerm () {
      if (myForceEffectorTerm == null) {
         ForceEffectorTerm fterm = new ForceEffectorTerm("forceMinTerm");
         fterm.setInternal (true);
         add (fterm);
      }
      return myForceEffectorTerm;
   }
   
   /**
    * Returns the standard force effector term, or {@code null} if it is not
    * present.
    * 
    * @return standard force effector term or {@code null}
    */
   public ForceEffectorTerm getForceEffectorTerm() {
      return myForceEffectorTerm;
   }
   
   /**
    * Removes the standard force effector term if it is present.
    * 
    * @return {@code true} if the standard force effector term was removed
    */ 
   public boolean removeForceEffectorTerm () {
      if (myForceEffectorTerm != null) {
         remove (myForceEffectorTerm);
         return true;
      }
      else {
         return false;
      }
   }
   
   /**
    * Adds a standard damping term to this controller, if it is not already
    * present. Returns a reference to the term regardless.
    * 
    * @return standard damping term
    */
   public DampingTerm addDampingTerm() {
      if (myDampingTerm == null) {
         DampingTerm dterm = new DampingTerm ("dampingTerm");
         dterm.setInternal (true);
         add (dterm);
      }
      return myDampingTerm;
   }

   /**
    * Adds a standard damping term to this controller, if it is not already
    * present. Sets the weight of the term and returns a reference to it
    * regardless.
    *
    * @param w weight for the damping term
    */
   public DampingTerm addDampingTerm(double w) {
      addDampingTerm();
      myDampingTerm.setWeight (w);
      return myDampingTerm;
   }

   /**
    * Returns the standard damping term, or {@code null} if it is not present.
    * 
    * @return standard damping term or {@code null}
    */
   public DampingTerm getDampingTerm () {
      return myDampingTerm;
   }

   /**
    * Removes the standard damping term if it is present.
    * 
    * @return {@code true} if the standard damping term was removed
    */
   public boolean removeDampingTerm() {
      if (myDampingTerm != null) {
         remove (myDampingTerm);
         return true;
      }
      else {
         return false;
      }
   }   

   /**
    * Adds both L2 and damping regularization terms with supplied weights
    */
   public void addRegularizationTerms(Double w_l2norm, Double w_damping) {
      if (w_l2norm != null) {
         addL2RegularizationTerm(w_l2norm);
      }

      if (w_damping != null) {
         addDampingTerm(w_damping);
      }
   }

   // ========== Begin general cost and constraint term methods ==========

   private boolean termIsInternal (QPTerm term) {
      if (term instanceof QPTermBase) {
         return ((QPTermBase)term).isInternal();
      }
      else {
         return false;
      }
   }

   /**
    * Adds a constraint term to this controller.
    * 
    * @param term constraint term to add
    */
   public void addConstraintTerm (QPConstraintTerm term) {
      if (termIsInternal(term)) {
         throw new IllegalArgumentException (
            "Constraint term is a standard internal term");
      }
      add (term);
   }

   /**
    * Removes a constraint term from this controller. This should not be one of
    * the standard constraint terms, such as the bounds term, which is
    * permanent.
    * 
    * @param term constraint term to remove
    * @return {@code true} if the term was present
    */
   public boolean removeConstraintTerm(QPConstraintTerm term) {
      if (termIsInternal(term)) {
         throw new IllegalArgumentException (
            "Constraint term is a standard internal term");
      }
      return remove (term);
   }

   /**
    * Adds a cost term to this controller.
    * 
    * @param term cost term to add
    */
   public void addCostTerm (QPCostTerm term) {
      if (termIsInternal(term)) {
         throw new IllegalArgumentException (
            "Cost term is a standard internal term");
      }
      add (term);
   }

   /**
    * Remove a cost term from this controller. This should not be one of the
    * standard cost terms, such as the motion target, L2 regularization, force
    * target, force effector or damping terms, which are either permanent or
    * have their own remove methods.
    * 
    * @param term cost term to remove
    * @return {@code true} if the term was present
    */
   public boolean removeCostTerm(QPCostTerm term) {
      if (termIsInternal(term)) {
         throw new IllegalArgumentException (
            "Cost term is a standard internal term");
      }
      return remove (term);
   }

   /**
    * Retrieves a list of all costs terms, including the standard ones.
    *
    * @return list of cost terms
    */
   public ArrayList<QPCostTerm> getCostTerms() {
      ArrayList<QPCostTerm> terms = new ArrayList<>();
      for (int i=0; i<numComponents(); i++) {
         ModelComponent mc = get(i);
         if (mc instanceof QPCostTerm) {
            QPCostTerm term = (QPCostTerm)mc;
            if (term.getType() == QPTerm.Type.COST) {
               terms.add (term);
            }
         }
      }
      return terms;
   }
   
   /**
    * Retrieves a list of all equality constraint terms, including the standard
    * ones.
    *
    * @return list of equality constraint terms
    */
   public ArrayList<QPConstraintTerm> getEqualityConstraints() {
      ArrayList<QPConstraintTerm> terms = new ArrayList<>();
      for (int i=0; i<numComponents(); i++) {
         ModelComponent mc = get(i);
         if (mc instanceof QPConstraintTerm) {
            QPConstraintTerm term = (QPConstraintTerm)mc;
            if (term.getType() == QPTerm.Type.EQUALITY) {
               terms.add (term);
            }
         }
      }
      return terms;
   }

   /**
    * Retrieves a list of all inequality constraint terms, including the
    * standard ones.
    *
    * @return list of inequality constraint terms
    */
   public ArrayList<QPConstraintTerm> getInequalityConstraints() {
      ArrayList<QPConstraintTerm> terms = new ArrayList<>();
      for (int i=0; i<numComponents(); i++) {
         ModelComponent mc = get(i);
         if (mc instanceof QPConstraintTerm) {
            QPConstraintTerm term = (QPConstraintTerm)mc;
            if (term.getType() == QPTerm.Type.INEQUALITY) {
               terms.add (term);
            }
         }
      }
      return terms;
   }
   

   // ========== Begin rendering methods ==========

   @Override
   public void prerender (RenderList list) {
      super.prerender (list);
      recursivelyPrerender (this, list);
   }
   
   protected void recursivelyPrerender (
      CompositeComponent comp, RenderList list) {

       for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof Renderable) {
            list.addIfVisible ((Renderable)c);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyPrerender ((CompositeComponent)c, list);
         }
      }     
   }
   
   // ========== Begin CompositeComponent implementation ==========

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (String nameOrNumber) {
      return myComponents.get (nameOrNumber);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (int idx) {
      return myComponents.get (idx);
   } 

   /**
    * {@inheritDoc}
    */
   public ModelComponent getByNumber (int num) {
      return myComponents.getByNumber (num);
   }

   /**
    * {@inheritDoc}
    */
   public int numComponents() {
      return myComponents.size();
   }

   /**
    * {@inheritDoc}
    */
   public int indexOf (ModelComponent comp) {
      return myComponents.indexOf (comp);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent findComponent (String path) {
      return ComponentUtils.findComponent (this, path);
   }

   /**
    * {@inheritDoc}
    */
   public int getNumberLimit() {
      return myComponents.getNumberLimit();
   }

   /**
    * {@inheritDoc}
    */
   public NavpanelDisplay getNavpanelDisplay() {
      return NavpanelDisplay.NORMAL;
   }

   /**
    * {@inheritDoc}
    */
   public void componentChanged (ComponentChangeEvent e) {
      myComponents.componentChanged (e);
      notifyParentOfChange (e);
   }

   /**
    * {@inheritDoc}
    */
   public void updateNameMap (
      String newName, String oldName, ModelComponent comp) {
      myComponents.updateNameMap (newName, oldName, comp);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hierarchyContainsReferences() {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   protected void add (ModelComponent comp) {
      myComponents.add (comp);
   }
   
   /**
    * {@inheritDoc}
    */
   protected boolean remove (ModelComponent comp) {
      return myComponents.remove (comp);
   }

   // write/scan implementation assuming a known set of child components with
   // set names and types:

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      if (targetsPointRadius != DEFAULT_TARGETS_POINT_RADIUS) {
         pw.println ("targetsPointRadius=" + fmt.format (targetsPointRadius));
      }
      pw.println ("useTrapezoidalSolver=" + getUseTrapezoidalSolver());
      myComponents.writeComponents (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      if (super.scanItem (rtok, tokens)) {
         return true;
      }
      rtok.nextToken();
      if (scanAttributeName (rtok, "targetsPointRadius")) {
         setTargetsPointRadius (rtok.scanNumber());
         return true;
      }
      else if (scanAttributeName (rtok, "useTrapezoidalSolver")) {
         setUseTrapezoidalSolver (rtok.scanInteger());
         return true;
      }
      else if (myComponents.scanAndStoreComponent (rtok, tokens)) {
         return true;
      }
      rtok.pushBack();
      return false;
   }

   /**
    * {@inheritDoc}
    */      
   public void scan (
         ReaderTokenizer rtok, Object ref) throws IOException {
      //
      // any required prescan code goes here  ...
      //
      super.scan (rtok, ref);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (myComponents.postscanComponent (tokens, ancestor)) {
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   /**
    * {@inheritDoc}
    */      
   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      //
      // any required postscan code goes here ...
      //
      myMech = (MechModel)getModel();
   }

   // ========== End CompositeComponent implementation ==========

   /* ---- Reimplementation of HasState to store excitations --- */

   /**
    * {@inheritDoc}
    */      
   public boolean hasState() {
      return true;
   }
   
   /**
    * {@inheritDoc}
    */   
   public ComponentState createState (
      ComponentState prevState) {
      // store excitations in a NumericState
      return new NumericState (1, numExciters());
   }
   
   /**
    * {@inheritDoc}
    */   
   public void getState (ComponentState state) {
      NumericState nstate = castToNumericState(state);
      nstate.resetOffsets();
      getExcitationState (nstate);
      // save point and frame targets as state, mainly for rendering purposes:
      for (Point p : myMotionTerm.myTargetPoints) {
         p.getState (nstate);
      }
      for (Frame f : myMotionTerm.myTargetFrames) {
         f.getState (nstate);
      }
   }

   /**
    * Stores the most recently computed excitation values in {@code nstate}.
    */
   private void getExcitationState (NumericState nstate) {
      // Store number of excitation values
      int numex = numExciters();
      nstate.zput (numex);
      for (int i=0; i<numex; i++) {
         double exval = 0;
         // In some cases, myExcitations may have a size < numExciters().
         // Just store a 0 in that case.
         if (i < myExcitations.size()) {
            exval = myExcitations.get(i);
         }
         nstate.dput (exval);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void setState (ComponentState state) {
      NumericState nstate = castToNumericState(state);
      nstate.resetOffsets();
      // Get number of stored excitation values
      setExcitationState (nstate);
      for (Point p : myMotionTerm.myTargetPoints) {
         p.setState (nstate);
      }
      for (Frame f : myMotionTerm.myTargetFrames) {
         f.setState (nstate);
      }
   }

   /**
    * Loads the most recently computed excitation values from {@code nstate}.
    */
   private void setExcitationState (NumericState nstate) {
     int numStoredExcitationValues = nstate.zget();
      for (int i=0; i<numStoredExcitationValues; i++) {
         double val = nstate.dget();
         // If numExciters() has changed, we may have more
         // value than we need. Just ignore extra values.
         if (i < myExcitations.size()) {
            myExcitations.set (i, val);
         }
      }
      setExcitations (myExcitations, 0);
   }

   /**
    * {@inheritDoc}
    */
   public void getInitialState (
      ComponentState newstate, ComponentState oldstate) {

      NumericState nstate = castToNumericState(newstate);
      nstate.setHasDataFrames (true);
      nstate.clear();

      HashMap<HasNumericState,NumericState.DataFrame> compMap = 
         new HashMap<HasNumericState,NumericState.DataFrame>();

      NumericState ostate = null;
      if (oldstate != null) {
         ostate = castToNumericState(oldstate);
         if (!ostate.hasDataFrames()) {
            throw new IllegalArgumentException ("oldstate does not have frames");
         }
         ostate.resetOffsets();
         for (int k=0; k<ostate.numDataFrames(); k++) {
            NumericState.DataFrame frame = ostate.getDataFrame(k);
            compMap.put (frame.getComp(), frame);
         }        
      }
      getExcitationState (nstate);
      nstate.addDataFrame (null);
      
      for (Point p : myMotionTerm.myTargetPoints) {
         NumericState.DataFrame frame = compMap.get(p);
         if (frame != null && frame.getVersion() == p.getStateVersion()) {
            nstate.getState (frame, ostate);
         }
         else {
            nstate.getState (p);
         }        
      }
      for (Frame f : myMotionTerm.myTargetFrames) {
         NumericState.DataFrame frame = compMap.get(f);
         if (frame != null && frame.getVersion() == f.getStateVersion()) {
            nstate.getState (frame, ostate);
         }
         else {
            nstate.getState (f);
         }        
      }
   }

   /**
    * Creates a name for a target component to be added to a list of target
    * components. The name is nominally formed by appending "_ref" to the
    * name or number of the associated source component, with additional
    * measures taken to ensure that this name is unique within the target list.
    */
   static String makeTargetName (
      String prefix, ModelComponent source, CompositeComponent targetList) {

      String name = null;
      if (source.getName() != null) {
         name = source.getName() + "_ref";
      }
      else if (source.getNumber() != -1) {
         name = prefix + source.getNumber() + "_ref";
      }
      if (name != null) {
         CompositeComponent ancestor= source.getParent();
         while (targetList.get(name) != null && ancestor != null) {
            // name is not unique, so prepend it with ancestor's name
            if (ancestor.getName() != null) {
               name = ancestor.getName() + "_" + name;
            }
            else if (ancestor.getNumber() != -1) {
               name = ancestor.getNumber() + "_" + name;
            }
            else {
               return null;
            }
         }
      }
      return name;
   }

   /**
    * Returns the most recently computed u0 vector for the excitation response.
    * See {@link ExcitationResponse} for details.
    * 
    * @return u0 vector for the excitation response. Should not be modified.
    */
   public VectorNd getU0() {
      return myExcitationResponse.getU0();
   }
   
   /**
    * Returns the {@code j}-th column of most recently computed Hu matrix for
    * the excitation response.  See {@link ExcitationResponse} for details.
    *
    * @param j requested column from the Hu matrix
    * @return {@code j}-th column of Hu for the excitation response. Should not
    * be modified.
    */
   public VectorNd getHuCol (int j) {
      return myExcitationResponse.getHuCol(j);
   }
   
   /**
    * Returns the most recently computed lam0 vector for the excitation
    * response.  See {@link ExcitationResponse} for details.
    * 
    * @return lam0 vector for the excitation response. Should not be modified.
    */
   public VectorNd getLam0() {
      return myExcitationResponse.getLam0();
   }
   
   /**
    * Returns the {@code j}-th column of most recently computed Hlam matrix for
    * the excitation response.  See {@link ExcitationResponse} for details.
    *
    * @param j requested column from the Hlam matrix
    * @return {@code j}-th column of Hlam for the excitation response. Should not
    * be modified.
    */
   public VectorNd getHlamCol (int j) {
      return myExcitationResponse.getHlamCol (j);
   }
}
