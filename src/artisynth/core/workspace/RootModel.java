/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.workspace;

import java.awt.Container;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JMenuItem;

import maspack.matrix.AxisAngle;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.NumericalException;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.IsRenderable;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.GL.GLViewer;
import maspack.util.Disposable;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.IntHolder;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Round;
import maspack.util.Write;
import maspack.util.DataBuffer;
import maspack.util.PathFinder;
import artisynth.core.util.TimeBase;
import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeListener;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ComponentListView;
import artisynth.core.modelbase.ComponentState;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CompositeState;
import artisynth.core.modelbase.Controller;
import artisynth.core.modelbase.HasState;
import artisynth.core.modelbase.Model;
import artisynth.core.modelbase.ModelAgent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.Monitor;
import artisynth.core.modelbase.NumericState;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.RenderableModelBase;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.Traceable;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.probes.Probe;
import artisynth.core.probes.TracingProbe;
import artisynth.core.probes.WayPoint;
import artisynth.core.probes.WayPointProbe;
import artisynth.core.renderables.IsRenderableHolder;
import artisynth.core.util.*;

/**
 * RootModel is the top-most model of an ArtiSynth model hierarchy. It contains
 * a list of models, plus a number of other workspace components such as
 * probes, controller, monitors, and control panels.
 */
public class RootModel extends RenderableModelBase
   implements Disposable {
   // DependencyClosure[] myClosures = null;

   protected boolean debugNextAdvanceTime = false;

   // Set this to true to test save-and-restore state before each model advance
   // step. (This will take some time, so you normally want it to be false).
   public static boolean testSaveAndRestoreState = false;

   LinkedList<ComponentChangeListener> myComponentListeners;
   protected ComponentList<Model> myModels;
   protected LinkedHashMap<Model,ModelInfo> myModelInfo;
   protected ComponentList<ControlPanel> myControlPanels;
   protected RenderableComponentList<Probe> myInputProbes;
   protected RenderableComponentList<Probe> myOutputProbes;
   protected RenderableComponentList<RenderableComponent> myRenderables;
   protected LinkedHashSet<Traceable> myTraceSet;
   protected WayPointProbe myWayPoints = null;
   protected ComponentList<Controller> myControllers;
   protected ComponentList<Monitor> myMonitors;

   // model info structures produced by last call to getInitialState()
   protected ArrayList<ModelInfo> myInitialInfos;

   // in development: specifies start time for root model simulation
   protected double myStartTime;

   // flag to stop advancing - which we need if we are in the midst of 
   // lots of small adaptive steps
   protected boolean myStopAdvance = false;

   // flag to tell scheduler to stop simulation
   protected boolean myStopRequest = false;

   protected boolean myModelInfoValid = false;
   private ModelInfo myRootInfo;
   protected static boolean use125Stepping = true;

   protected boolean myAdaptiveStepping = DEFAULT_ADAPTIVE_STEPPING;
   protected double myMinStepSize = DEFAULT_MIN_STEP_SIZE;

   private static final Point3d DEFAULT_VIEWER_CENTER = new Point3d();
   private static final Point3d DEFAULT_VIEWER_EYE = new Point3d (0, -1, 0);
   private static final AxisAngle DEFAULT_VIEW_ORIENTATION = 
      new AxisAngle(0,0,0,0);
   private static final double DEFAULT_MIN_STEP_SIZE = 1e-7;
   private static final double DEFAULT_MAX_STEP_SIZE = 0.01;
   private static final boolean DEFAULT_ADAPTIVE_STEPPING = false;

   AxisAngle myDefaultViewOrientation = 
      new AxisAngle (DEFAULT_VIEW_ORIENTATION);
   
   GLViewer myMainViewer;
   
   private JFrame myControlPanelsFrame;
   private JTabbedPane myControlPanelTabs;

   protected class ModelInfo {
      Model model;
      CompositeState state;
      LinkedList<Controller> controllers;
      LinkedList<Monitor> monitors;
      LinkedList<Probe> inputProbes;
      LinkedList<Probe> outputProbes;
      // Last state created by each component. Supplied to each component when
      // asked to create a new state, so as to provide sizing hints
      HashMap<HasState,ComponentState> lastStateMap;
      double h; // current step size
      double maxStepSize; // current effective mass step size
      double lasts; // last return value from advance
      int successCnt;
      int failedIncreaseCnt;
      boolean attemptingIncrease;

      // state-bearing components created during last call to getInitialState()
      ArrayList<ModelComponent> initialStateComps;

      ModelInfo (Model m) {
         controllers = new LinkedList<Controller>();
         monitors = new LinkedList<Monitor>();
         inputProbes = new LinkedList<Probe>();
         outputProbes = new LinkedList<Probe>();
         lastStateMap = new HashMap<HasState,ComponentState>();
         model = m;
         clear();
      }

      void clear() {
         inputProbes.clear();
         controllers.clear();
         monitors.clear();
         outputProbes.clear();
         lastStateMap.clear();
         maxStepSize = getEffectiveMaxStepSize();
         h = maxStepSize;
         lasts = 1;
         successCnt = 0;
         failedIncreaseCnt = 0;
         attemptingIncrease = false;
      }
      
      void createState() {
         state = createModelAndControllersState();
      }

      CompositeState createModelAndControllersState() {
         return new CompositeState();
      }

      CompositeState createFullState () {
         return new CompositeState();
      }
      
      private boolean maybeGetSubstate (
         CompositeState state, ModelComponent comp) {
         if (comp.hasState() && comp instanceof HasState) {
            HasState c = (HasState)comp;
            ComponentState prevState = lastStateMap.get(c);
            ComponentState substate = c.createState(prevState);
            if (state.isAnnotated()) {
               substate.setAnnotated(true);
            }            
            lastStateMap.put(c, substate);
            c.getState (substate);
            state.addState (substate);
            return true;
         }
         else {
            return false;
         }
      }

      private boolean maybeGetInitialSubState (
         CompositeState state, ModelComponent comp,
         HashMap<Object,ComponentState> stateMap) {

         if (comp.hasState() && comp instanceof HasState) {
            HasState c = (HasState)comp;
            ComponentState prevState = lastStateMap.get(c);
            ComponentState substate = c.createState(prevState);
            if (state.isAnnotated()) {
               substate.setAnnotated(true);
            }
            lastStateMap.put(c, substate);
            c.getInitialState (substate, stateMap.get(comp));
            state.addState (substate);
            //state.addComponent (comp);
            initialStateComps.add (comp);
            return true;
         }
         else {
            return false;
         }
      }

      private void doGetModelAndControllersState (CompositeState state) {
         for (Controller ctl : controllers) {
            maybeGetSubstate (state, ctl);
         }
         if (model == RootModel.this) {
            ComponentState substate = RootModel.this.createRootState();
            RootModel.this.getRootState (substate);
            state.addState (substate);
         }
         else {
            maybeGetSubstate (state, model);
         }       
      }

      void getModelAndControllersState (CompositeState state) {
         state.clear();
         doGetModelAndControllersState (state);
      }

      void getFullState (CompositeState state) {
         state.clear();
         doGetModelAndControllersState (state);
         for (Monitor mon : monitors) {
            maybeGetSubstate (state, mon);
         }  
         for (Probe prb : inputProbes) {
            maybeGetSubstate (state, prb);
         }         
         for (Probe prb : outputProbes) {
            maybeGetSubstate (state, prb);
         }         
      }

      void getInitialState (CompositeState newstate, CompositeState oldstate) {
         HashMap<Object,ComponentState> stateMap =
         new HashMap<Object,ComponentState>();

         if (oldstate != null) {
            if (initialStateComps == null) {
               throw new IllegalStateException (
                  "initialStateComps not initialized");
            }
            if (initialStateComps.size() != oldstate.numSubStates()) {
               throw new InternalErrorException (
                  "Oldstate has "+initialStateComps.size()+" components and "+
                  oldstate.numSubStates()+" substates");
            }
            for (int i=0; i<initialStateComps.size(); i++) {
               stateMap.put (initialStateComps.get(i), oldstate.getState(i));
            }
         }
         
         initialStateComps = new ArrayList<ModelComponent>();
         for (Controller ctl : controllers) {
            maybeGetInitialSubState (newstate, ctl, stateMap);
         }
         if (model == RootModel.this) {
            ComponentState substate = RootModel.this.createRootState();
            RootModel.this.getRootState (substate);
            newstate.addState (substate);
            //newstate.addComponent (RootModel.this);
            initialStateComps.add (RootModel.this);
         }
         else {
            maybeGetInitialSubState (newstate, model, stateMap);
         }       
         
         for (Monitor mon : monitors) {
            maybeGetInitialSubState (newstate, mon, stateMap);
         }  
         for (Probe prb : inputProbes) {
            maybeGetInitialSubState (newstate, prb, stateMap);
         }         
         for (Probe prb : outputProbes) {
            maybeGetInitialSubState (newstate, prb, stateMap);
         } 
      }

      int setModelAndControllersState (CompositeState state) {
         if (state.numSubStates() < this.state.numSubStates()) {
            throw new InternalErrorException (
               "state has only "+state.numSubStates()+" substates, "+
               this.state.numSubStates()+" required");
         }
         int idx = 0;
         for (Controller ctl : controllers) {
            if (ctl.hasState()) {
               ctl.setState (state.getState(idx++));
            }
         }
         if (model == RootModel.this) {
            RootModel.this.setRootState (state.getState(idx++));            
         }
         else if (model.hasState()) {
            model.setState (state.getState(idx++));
         }       
         return idx;
      }

      void setFullState (CompositeState state) {
         int idx = setModelAndControllersState (state);
         for (Monitor mon : monitors) {
            if (mon.hasState()) {
               mon.setState (state.getState(idx++));
            }
         } 
         for (Probe prb : inputProbes) {
            if (prb.hasState()) {
               if (idx >= state.numSubStates()) {
                  System.out.println ("num substates=" + state.numSubStates());
                  System.out.println ("idx = " + idx);
               }
               prb.setState (state.getState(idx++));
            }
         }
         for (Probe prb : outputProbes) {
            if (prb.hasState()) {
               prb.setState (state.getState(idx++));
            }
         }
      }

      double getEffectiveMaxStepSize() {
         double modelMax = model.getMaxStepSize();
         double rootMax = getMaxStepSize();
         if (modelMax == -1 || modelMax >= rootMax) {
            return rootMax;
         }
         else {
            return modelMax;
         }
      }

      double getNextAdvanceTime (double t0, double t1) {
         double hmax = getEffectiveMaxStepSize();
         if (hmax != maxStepSize) {
            maxStepSize = hmax;
            if (debugNextAdvanceTime) {
               System.out.println (
                  "NextAdvanceTime: reducing h and maxStepSize to hmax: " + hmax);
            }
            h = hmax;
         }
         else if (h > hmax) {
            if (debugNextAdvanceTime) {
               System.out.println (
                  "NextAdvanceTime: reducing h to hmax: " + hmax);
            }
            h = hmax;
         }
         double te = nextProbeEvent (outputProbes, t0);
         if (TimeBase.compare (te, t1) < 0) {
            t1 = te;
         }
         attemptingIncrease = false; // should be false, just being paranoid
         if (myAdaptiveStepping) {
            if (h < hmax && TimeBase.compare (t1-t0, 2*h) >= 0) {
               // see if we can increase the step size
               // don't look at lasts for now.
               if (successCnt > failedIncreaseCnt) {
                  h = TimeBase.round (increaseStepSize (h, hmax));
                  System.out.println ("t0=" + t0 + ", > step " + h);
                  attemptingIncrease = true;
               }
            }
         }
         if (TimeBase.compare (t1-t0, h) > 0) {
            t1 = t0 + h;
         }
         return TimeBase.round (t1);
      }

      double reduceAdvanceTime (
         double s, double t0, double t1, String diagnostic) {

         successCnt = 0;
         if (attemptingIncrease) {
            failedIncreaseCnt++;
            attemptingIncrease = false;
         }
         // if s is not unspecified, limit it to 0.1
         if (s != 0) {
            s = Math.max (s, 0.1);
         }
         // need to reduce step size
         if (TimeBase.compare (t1-t0, h) < 0) {
            // if tb - ta is less than h, reduce s even more:
            s *= (t1-t0)/h;
         }
         h = reduceStepSize (h, s, getEffectiveMaxStepSize());
         if (h < getMinStepSize()) {
            String msg =
               "adaptive step size fell below minimum of " + getMinStepSize();
            if (diagnostic != null) {
               msg += "; caused by " + diagnostic;
            }
            throw new NumericalException (msg);
         }
         h = TimeBase.round (h);
         System.out.println ("t0=" + t0 + ", < step " + h);
         return TimeBase.round (t0 + h);
      }

      protected void updateStepInfo (double s) {
         successCnt++;
         lasts = s;
         if (attemptingIncrease) {  
            attemptingIncrease = false;
            failedIncreaseCnt = 0;
         }
      }

      protected double reduceStepSize (double h, double s, double hmax) {
         double hr = h/hmax;
         double a = 0.9;
         if (s != 0 && s < a) {
            a = s;
         }
         if (use125Stepping) {
            hr = Round.down125 (a*hr);
         }
         else {
            hr = Round.downPow2 (a*hr);
         }
         return TimeBase.round (hr*hmax);
      }

      protected double increaseStepSize (double h, double hmax) {
         double hr = h/hmax;
         double a = 1.1;
         // ignore s for now ...
         if (use125Stepping) {
            hr = Round.up125 (a*hr);
         }
         else {
            hr = Round.upPow2 (a*hr);
         }
         return TimeBase.round (hr*hmax);
      }

      protected int getDStateSize() {
         return 2;
      }

      protected int getZStateSize() {
         return 2;
      }

      protected void getState (DataBuffer data) {
         data.zput (successCnt);
         data.zput (failedIncreaseCnt);
         data.dput (h);
         data.dput (maxStepSize); // not sure we need to save this ...
         data.dput (lasts);
      }

      protected void setState (DataBuffer data) {
         successCnt = data.zget();
         failedIncreaseCnt = data.zget();
         h = data.dget();
         maxStepSize = data.dget();
         lasts = data.dget();
      }

   }

   private static boolean myFocusableP = true;

   public static void setFocusable (boolean focusable) {
      myFocusableP = focusable;
   }

   public static boolean isFocusable() {
      return myFocusableP;
   }

   public void build (String[] args) throws IOException {
   }

   /**
    * Returns a text string giving a short description of this model.
    * 
    * @return text description of this model

    */

   public String getAbout() {
      return null;
   }

   public static PropertyList myProps =
      new PropertyList (RootModel.class, RenderableModelBase.class);

   static {
      myProps.add (
         "viewerCenter", "viewer center of attention",
         DEFAULT_VIEWER_CENTER, "NW");
      myProps.add (
         "viewerEye", "viewer eye location",
         DEFAULT_VIEWER_EYE, "NW");
      myProps.add (
         "defaultViewOrientation", "default eye-to-world rotation transform",
         DEFAULT_VIEW_ORIENTATION);
      myProps.add (
         "minStepSize",
         "minimum allowed step size of adaptive stepping", DEFAULT_MIN_STEP_SIZE);
      myProps.add (
         "adaptiveStepping",
         "enables/disables adaptive step sizing", DEFAULT_ADAPTIVE_STEPPING);
      // remove and replace maxStepSize to redefine default value and range
      myProps.remove ("maxStepSize");
      myProps.add (
         "maxStepSize", "maximum step size for this component (seconds)",
         DEFAULT_MAX_STEP_SIZE, "(0,inf]");
   }

   @Override
   public void setDefaultValues() {
      super.setDefaultValues();
      myDefaultViewOrientation = new AxisAngle (DEFAULT_VIEW_ORIENTATION);
   }

   public boolean getAdaptiveStepping() {
      return myAdaptiveStepping;
   }
   
   public void setAdaptiveStepping (boolean enable) {
      myAdaptiveStepping = enable;
   }
   
   public double getMinStepSize() {
      return myMinStepSize;
   }
   
   public void setMinStepSize (double step) {
      myMinStepSize = step;
   }
   
   public void setMaxStepSize (double step) {
      if (step <= 0) {
         throw new IllegalArgumentException (
            "step size must be positive");
      }
      super.setMaxStepSize (step);
      componentChanged (new PropertyChangeEvent (this, "maxStepSize"));
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   /**
    * Returns true if a given property name is a property of the
    * RootModel class proper. This allows us to determine if it
    * is safe to use a given RootModel property when saving a model
    * using only core components.
    */
   public static boolean isBaseProperty (String name) {
      return myProps.get(name) != null;
   }

   /**
    * Empty constructor, for creating a basic class instance
    */
   public RootModel() {
      this (null);
   }

   /**
    * Constructor used to build the model
    * @param name the name of the new model
    */
   public RootModel (String name) {
      super (name);
      myComponentListeners = new LinkedList<ComponentChangeListener>();
      myModels = new ComponentList<Model> (Model.class, "models", "m");
      myMonitors = new ComponentList<Monitor> (Monitor.class, "monitors", "mo");
      myControllers =
         new ComponentList<Controller> (Controller.class, "controllers", "c");
      myControlPanels =
         new ComponentList<ControlPanel> (
            ControlPanel.class, "controlPanels", "c");
      myInputProbes =
         new RenderableComponentList<Probe> (Probe.class, "inputProbes", "i");
      myOutputProbes =
         new RenderableComponentList<Probe> (Probe.class, "outputProbes", "o");
      myRenderables = new RenderableComponentList<RenderableComponent>(
         RenderableComponent.class, "renderables", "r");
      
      addFixed (myModels);
      addFixed (myControllers);
      addFixed (myMonitors);
      addFixed (myControlPanels);
      addFixed (myInputProbes);
      addFixed (myOutputProbes);
      addFixed (myRenderables);
      
      myWayPoints = new WayPointProbe (this);
      myWayPoints.setName ("WayPoints");
      myTraceSet = new LinkedHashSet<Traceable>();

      myModelInfo = new LinkedHashMap<Model,ModelInfo>();
      
      myMaxStepSize = DEFAULT_MAX_STEP_SIZE;
   }

   public ComponentListView<Model> models() {
      return myModels;
   }

   public void addModel (Model model) {
      if (model == null) {
         throw new IllegalArgumentException ("model is null");
      }
      myModels.add (model);
   }

   public boolean removeModel (Model model) {
      return myModels.remove (model);
   }
   
   public void removeAllModels () {
      myModels.removeAll();
   }
 
   public void addMonitor (Monitor monitor) {
      if (monitor.getModel() != null) {
         if (!myModels.contains (monitor.getModel())) {
            throw new IllegalArgumentException (
               "Model not contained within RootModel");
         }
      }
      myMonitors.add (monitor);
   }

   public void addController (Controller controller) {
      if (controller.getModel() != null) {
         // find as subcomponent
         Model model = controller.getModel ();
         // root parent
         CompositeComponent parent = model.getParent ();
         CompositeComponent grandParent = parent.getParent ();
         while (grandParent != null) {
            parent = grandParent;
            grandParent = parent.getParent ();
         }
         if (parent != this) {
            throw new IllegalArgumentException ("Model not contained within RootModel");
         }
         //         if (!myModels.contains (controller.getModel())) {
         //            throw new IllegalArgumentException (
         //               "Model not contained within RootModel");
         //         }
      }
      myControllers.add (controller);
   }

   public IsRenderableHolder addRenderable(Renderable renderable) {
      IsRenderableHolder holder = new IsRenderableHolder(renderable);
      addRenderable(holder);
      return holder;
   }
   
   public boolean removeRenderable(Renderable renderable) {
      for (RenderableComponent rc : myRenderables) {
         if (rc instanceof IsRenderableHolder) {
            IsRenderableHolder holder = (IsRenderableHolder)rc;
            if (renderable == holder.getRenderable()) {
               removeRenderable(holder);
               return true;
            }
         }
      }
      return false;
   }
   
   public void addRenderable (RenderableComponent comp) {
      if (comp == null) {
         return;
      }
      myRenderables.add(comp);
   }
   
   public boolean removeRenderable(RenderableComponent comp) {
      if (comp == null) {
         return false;
      }
      return myRenderables.remove(comp);
   }
   
   public void clearRenderables() {
      myRenderables.clear();
   }
   
   public RenderableComponentList<RenderableComponent> renderables() {
      return myRenderables;
   }
   
   public GLViewer getMainViewer() {
      if (myMainViewer == null) {
         // XXX hack in case this is called inside the RootModel constructor
         // instead of inside the build method
         return Main.getMain().getViewer();
      }
      else {
         return myMainViewer;
      }
   }

   public void setMainViewer (GLViewer v) {
      myMainViewer = v;
   }

   public JFrame getMainFrame() {
      if (Main.getMain() != null) {
         return Main.getMain().getMainFrame();
      }
      else {
         return null;
      }
   }

   public void setViewerCenter (Point3d c) {
      GLViewer viewer = getMainViewer();
      if (viewer != null) {
         viewer.setCenter (c);
      }
   }

   public Point3d getViewerCenter() {
      GLViewer viewer = getMainViewer();
      if (viewer != null) {
         return viewer.getCenter();
      }
      else {
         return new Point3d (DEFAULT_VIEWER_CENTER);
      }
   }

   public void setViewerEye (Point3d e) {
      GLViewer viewer = getMainViewer();
      if (viewer != null) {
         viewer.setEye (e);
      }
   }

   public Point3d getViewerEye() {
      GLViewer viewer = getMainViewer();
      if (viewer != null) {
         return viewer.getEye();
      }
      else {
         return new Point3d (DEFAULT_VIEWER_EYE);
      }
   }

   public void setViewerUp (Vector3d up) {
      GLViewer viewer = getMainViewer();
      if (viewer != null) {
         viewer.setUpVector (up);
      }
   }

   public Vector3d getViewerUp () {
      GLViewer viewer = getMainViewer();
      if (viewer != null) {
         return viewer.getUpVector ();
      }
      else {
         return Vector3d.Z_UNIT;
      }
   }

   /**
    * Obtains the default orientation that should be used for viewing this
    * model. A value equal to
    * {@link AxisAngle#ZERO} indicates that no orientation is specified.
    * 
    * @return default rotational transform from eye to world coordinates
    */
   public AxisAngle getDefaultViewOrientation() {
      return myDefaultViewOrientation;
   }

   /**
    * Sets the default orientation that should be used for viewing
    * this model to <code>REW</code>. Setting a value equal to
    * {@link AxisAngle#ZERO} indicates
    * that no orientation is specified and so the viewer should
    * use its default view. 
    * 
    * @param REW rotational transform from eye to world coordinates
    */
   public void setDefaultViewOrientation (AxisAngle REW) {
      if (!myDefaultViewOrientation.equals (REW)) {
         myDefaultViewOrientation.set (REW);
         componentChanged (
            new PropertyChangeEvent (this, "defaultViewOrientation"));

      }
   }

   /**
    * Sets the default orientation that should be used for viewing this model
    * to <code>REW</code>, where {@code REW} is specified as an {@link
    * AxisAlignedRotation}. Typical values are {@code AxisAlignedRotation.X_Y}
    * (y axis pointing up), and {@code AxisAlignedRotation.X_Z} (z axis
    * pointing up). If {@code REW} is passed as {@code null}, then no
    * orientation is specified and so the viewer will use its default view.
    * 
    * @param REW rotational transform from eye to world coordinates
    */
   public void setDefaultViewOrientation (AxisAlignedRotation REW) {
      AxisAngle axisAng = new AxisAngle();
      if (REW == null) {
         axisAng.set (0, 0, 0, 0);
      }
      else {
         axisAng.set (REW.getAxisAngle());
      }
      setDefaultViewOrientation (axisAng);
   }

   public void addMonitor (Monitor monitor, Model model) {
      if (model != null) {
         if (!myModels.contains (model)) {
            throw new IllegalArgumentException (
               "Model not contained within RootModel");
         }
      }
      monitor.setModel (model);
      myMonitors.add (monitor);
   }

   public boolean removeMonitor (Monitor monitor) {
      return myMonitors.remove (monitor);
   }
   
   public void removeAllMonitors () {
      myMonitors.removeAll();
   }
   
   public ComponentListView<Monitor> getMonitors() {
      return myMonitors;
   }

   public void addController (Controller controller, Model model) {
      if (model != null) {
         if (!myModels.contains (model)) {
            throw new IllegalArgumentException (
               "Model not contained within RootModel");
         }
      }
      controller.setModel (model);
      myControllers.add (controller);
   }

   public boolean removeController (Controller controller) {
      return myControllers.remove (controller);
   }
   
   public void removeAllControllers () {
      myControllers.removeAll();
   }
   
   public ComponentListView<Controller> getControllers() {
      return myControllers;
   }

   protected void locateControlPanel (ControlPanel panel) {
      JFrame frame = getMainFrame();
      if (frame != null) {
         java.awt.Point loc = frame.getLocation();
         panel.setLocation (loc.x + frame.getWidth(), loc.y);
      }
   }

   public void addControlPanel (ControlPanel panel) {
      myControlPanels.add (panel);
      locateControlPanel (panel);
   }

   public void addControlPanel (ControlPanel panel, int idx) {
      myControlPanels.add (panel, idx);
      locateControlPanel (panel);
   }

   public boolean removeControlPanel (ControlPanel panel) {
      return myControlPanels.remove (panel);
   }

   public void removeAllControlPanels() {
      myControlPanels.removeAll();
   }

   public ComponentListView<ControlPanel> getControlPanels() {
      return myControlPanels;
   }

   public ControlPanel loadControlPanel (String filename) {
      ControlPanel panel = null;
      try {
         panel =
            (ControlPanel)ComponentUtils.loadComponent (
               new File (filename), this, ControlPanel.class);
      }
      catch (Exception e) {
         System.out.println (
            "Error reading control panel file "+filename+
            ", error="+ e.getMessage());
      }

      if (panel != null && panel.numWidgets() > 0) {
         //panel.pack();
         //panel.setVisible (true);
         myControlPanels.add (panel);
         locateControlPanel (panel);
      }

      return panel;
   }

   public void addInputProbe (Probe probe) {
      if (!probe.isInput()) {
         throw new IllegalArgumentException ("probe is not an input probe");
      }
      myInputProbes.add (probe);
   }

   public void addInputProbe (Probe probe, int idx) {
      if (!probe.isInput()) {
         throw new IllegalArgumentException ("probe is not an input probe");
      }
      myInputProbes.add (probe, idx);
   }

   public boolean removeInputProbe (Probe probe) {
      return myInputProbes.remove (probe);
   }

   public void removeAllInputProbes() {
      myInputProbes.removeAll();
   }

   public ComponentList<Probe> getInputProbes() {
      return myInputProbes;
   }

   /**
    * In development: specifies the simulation start time for a root model.
    */
   public double getStartTime() {
      return myStartTime;
   }

   /**
    * In development: specifies the simulation start time for a root model.
    */
   public void setStartTime (double time) {
      myStartTime = time;
   }

   /**
    * Convenience routine to add a tracing probe to this RootModel. The probe is
    * created for a specified trace of a Traceable component. Start and stop
    * times are given in seconds. The probe's update interval is set to the
    * maximum step size of this RootModel, and the render interval is set to 50
    * msec.
    * 
    * @param traceable
    * component to be traced
    * @param traceName
    * name of the trace
    * @param startTime
    * start time (seconds)
    * @param stopTime
    * stop time (seconds)
    * @return created tracing probe
    */
   public TracingProbe addTracingProbe (
      Traceable traceable, String traceName, double startTime, double stopTime) {
      TracingProbe probe = TracingProbe.create (traceable, traceName);
      probe.setStartTime (startTime);
      probe.setStopTime (stopTime);
      probe.setUpdateInterval (getMaxStepSize());
      probe.setRenderInterval (0.05);
      addOutputProbe (probe);
      return probe;
   }

   public void addOutputProbe (Probe probe) {
      if (probe.isInput()) {
         throw new IllegalArgumentException ("probe is an input probe");
      }
      myOutputProbes.add (probe);
   }

   public void addOutputProbe (Probe probe, int idx) {
      if (probe.isInput()) {
         throw new IllegalArgumentException ("probe is an input probe");
      }
      myOutputProbes.add (probe, idx);
   }

   public boolean removeOutputProbe (Probe probe) {
      return myOutputProbes.remove (probe);
   }

   public void removeAllOutputProbes() {
      myOutputProbes.removeAll();
   }

   public RenderableComponentList<Probe> getOutputProbes() {
      return myOutputProbes;
   }

   public boolean hasTracingProbes() {
      for (Probe p : myOutputProbes) {
         if (p instanceof TracingProbe) {
            return true;
         }
      }
      return false;
   }

   public void setTracingProbesVisible (boolean visible) {
      for (Probe p : myOutputProbes) {
         if (p instanceof TracingProbe) {
            RenderProps.setVisible ((TracingProbe)p, visible);
         }
      }
   }

   // WS
   public WayPointProbe getWayPoints() {
      return myWayPoints;
   }

   private double getTime() {
      return Main.getMain().getTime();
   }

//   private void setTime (double time) {
//      Main.getMain().getScheduler().setInitialTime(time);
//      initialize (time);
//   }

   // WS
   public void addWayPoint (WayPoint way) {
      if (way.getTime() != 0) {
         myWayPoints.add (way);
         // set the state if we can. Don't bother if main or myRootInfo not set
         // up yet
         if (Main.getMain() != null && myRootInfo != null) {
            if (way.getTime() == getTime()) {
               way.setState (this);
            }
         }
         componentChanged (new StructureChangeEvent(myWayPoints));
      }
   }

   public WayPoint addWayPoint (double t) {
      if (t != 0) {
         WayPoint way = new WayPoint (t);
         addWayPoint (way);
         return way;
      }
      else {
         return null;
      }
   }

   public WayPoint addBreakPoint (double t) {
      if (t != 0) {
         WayPoint way = new WayPoint (t);
         way.setBreakPoint(true);
         addWayPoint (way);
         return way;
      }
      else {
         return null;
      }
   }

   // WS
   public boolean removeWayPoint (WayPoint way) {
      if (myWayPoints.remove (way)) {
         componentChanged (new StructureChangeEvent(myWayPoints));
         return true;
      }
      else {
         return false;
      }
   }

   // WS
   public WayPoint getWayPoint (double t) {
      return myWayPoints.get (t);
   }

   public void removeAllWayPoints() {
      myWayPoints.clear();
      componentChanged (new StructureChangeEvent(myWayPoints));
   }

   public TracingProbe getTracingProbe (Traceable tr, String propName) {
      for (Probe p : myOutputProbes) {
         if (p instanceof TracingProbe) {
            TracingProbe tp = (TracingProbe)p;
            if (tp.isTracing (tr, propName)) {
               return tp;
            }
         }
      }
      return null;
   }

   public void enableTracing (Traceable tr) {
      if (getTracingProbe (tr, "position") == null) {
         addTracingProbe (tr, "position", 0, 10);
         rerender();
      }
   }

   public boolean isTracing (Traceable tr) {
      return getTracingProbe (tr, "position") != null;
   }

   public boolean disableTracing (Traceable tr) {
      TracingProbe tp = getTracingProbe (tr, "position");
      if (tp != null) {
         removeOutputProbe (tp);
         rerender();
         return true;
      }
      else {
         return false;
      }
   }

   public void clearTracing (Traceable tr) {
      TracingProbe tp = getTracingProbe (tr, "position");
      if (tp != null) {
         tp.getNumericList().clear();
         tp.setData (tp.getStartTime());
         tp.setData (tp.getStopTime());
         rerender();
      }
   }

   public LinkedList<TracingProbe> getTracingProbes() {
      LinkedList<TracingProbe> tprobes = new LinkedList<TracingProbe>();
      for (Probe p : myOutputProbes) {
         if (p instanceof TracingProbe) {
            TracingProbe tp = (TracingProbe)p;
            if (tp.isTracing (null, "position") &&
                tp.getHost (0) instanceof Traceable) {
               tprobes.add (tp);
            }
         }
      }
      return tprobes;
   }

   public void disableAllTracing() {
      Collection<TracingProbe> tprobes = getTracingProbes();
      for (TracingProbe tp : tprobes) {
         removeOutputProbe (tp);
      }
      rerender();
   }

   public void clearTraces() {
      Collection<TracingProbe> tprobes = getTracingProbes();
      for (TracingProbe tp : tprobes) {
         tp.getNumericList().clear();
         tp.setData (tp.getStartTime());
         tp.setData (tp.getStopTime());
      }
      rerender();
   }

   public Collection<Traceable> getTraceSet() {
      Collection<TracingProbe> tprobes = getTracingProbes();
      ArrayList<Traceable> traceSet = new ArrayList<Traceable>();
      for (TracingProbe tp : tprobes) {
         traceSet.add ((Traceable)tp.getHost (0));
      }
      return traceSet;
   }

   public int getNumTraceables() {
      return myTraceSet.size();
   }

   public void clear() {
      myControllers.removeAll();
      myModels.removeAll();
      myMonitors.removeAll();
      myControlPanels.removeAll();
      myInputProbes.removeAll();
      myOutputProbes.removeAll();
      myRenderables.removeAll();
   }

   // implementations for Renderable

   public void prerender (RenderList list) {
      for (Controller c : myControllers) {
         if (c instanceof Renderable) {
            list.addIfVisible ((Renderable)c);
         }
      }
      for (Model m : myModels) {
         if (m instanceof Renderable) {
            list.addIfVisible ((Renderable)m);
         }
      }
      for (Monitor m : myMonitors) {
         if (m instanceof Renderable) {
            list.addIfVisible ((Renderable)m);
         }
      }
      list.addIfVisible (myOutputProbes);
      list.addIfVisible (myInputProbes);
      list.addIfVisible (myRenderables);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      for (Model m : myModels) {
         if (m instanceof Renderable) {
            ((Renderable)m).updateBounds (pmin, pmax);
         }
      }
      myOutputProbes.updateBounds (pmin, pmax);
      myRenderables.updateBounds(pmin, pmax);
   }

   public void render (Renderer renderer, int flags) {
      // no actual rendering; all-subcomponents render themselves
      // after being assembled in the RenderList
   }

   public void rerender() {
      Main.getMain().rerender();
   }

   /**
    * {@inheritDoc}
    */
   public void initialize (double t) {
      if (!myModelInfoValid) {
         updateModelInfo();
         myModelInfoValid = true;
      }
      for (Probe p : myInputProbes) {
         p.initialize(t);
      }
      for (Controller c : myControllers) {
         c.initialize(t);
      }
      for (Iterator<Model> it = myModels.iterator(); it.hasNext();) {
         it.next().initialize (t);
      }
      for (Monitor m : myMonitors) {
         m.initialize(t);
      }
      for (Probe p : myOutputProbes) {
         p.initialize(t);
      }
   }

   /**
    * Attach this root model to a driver program
    * 
    * @param driver
    * Interface giving access to the frame and viewer
    */
   public void attach (DriverInterface driver) {
   }

   /**
    * Detach this root model from a driver program.
    *
    * @param driver
    * Interface giving access to the frame and viewer
    */
   public void detach (DriverInterface driver) {
   }

   public void addComponentChangeListener (ComponentChangeListener l) {
      myComponentListeners.add (l);
   }

   public boolean removeComponentChangeListener (ComponentChangeListener l) {
      return myComponentListeners.remove (l);
   }

   private void fireComponentChangeListeners (ComponentChangeEvent e) {
      // clone the listener list in case some of the listeners
      // want to remove themselves from the list
      @SuppressWarnings("unchecked")
      LinkedList<ComponentChangeListener> listeners =
         (LinkedList<ComponentChangeListener>)myComponentListeners.clone();

      for (ComponentChangeListener l : listeners) {
         l.componentChanged (e);
      }
   }

   public void componentChanged (ComponentChangeEvent e) {
      // no need to notify parent since there is none
      if (e.getCode() == ComponentChangeEvent.Code.STRUCTURE_CHANGED) {
         synchronized (this) {
            // invalidate modelInfo if component is unknown, or the RootModel
            // or one of it's children.
            int level = -1;
            ModelComponent c = e.getComponent();
            while (c != null && level < 2) {
               c = c.getParent();
               level++;
            }
            if (level < 2) {
               myModelInfoValid = false;
            }
         }
      }
      // if called in constructor, myControlPanels might still be null ...
      if (myControlPanels != null) {
//         for (ControlPanel panel : myControlPanels) {
//            panel.removeStalePropertyWidgets();
//         }
         fireComponentChangeListeners (e);
      }
   }

   public void notifyStructureChanged (Object comp) {
      synchronized (this) {
         myModelInfoValid = false;
      }
      super.notifyStructureChanged (comp);
   }

   private ModelInfo getModelInfo (ModelAgent agent)  {

      ModelInfo info = null;
      Model model = agent.getModel();
      if (model != null) {
         info = myModelInfo.get (model);
      }
      if (info == null) {
         info = myRootInfo;
      }
      return info;
   }

   private void updateModelInfo() {

      if (myRootInfo == null) {
         // want to only allocate this once, since we use it to reference state
         // values via a hashmap
         myRootInfo = new ModelInfo (this);
      }
      
      // rebuild modelinfo, removing info for deleted models
      // and adding info for new models.
      LinkedHashMap<Model,ModelInfo> newinfo =
         new LinkedHashMap<Model,ModelInfo>();
      for (int i = 0; i < myModels.size(); i++) {
         // add model info for any new models
         Model model = myModels.get(i);
         ModelInfo info = myModelInfo.get (model);
         if (info != null) {
            info.clear();
         }
         else {
            info = new ModelInfo(model);
         }
         newinfo.put (model, info);
      }
      myModelInfo = newinfo;

      myRootInfo.clear();
      for (int i = 0; i < myMonitors.size(); i++) {
         Monitor mon = myMonitors.get(i);
         ModelInfo info = getModelInfo (mon);
         info.monitors.add (mon);
      }
      for (int i = 0; i < myControllers.size(); i++) {
         Controller ctl = myControllers.get(i);
         ModelInfo info = getModelInfo (ctl);
         info.controllers.add (ctl);
      }
      for (int i = 0; i < myInputProbes.size(); i++) {
         Probe p = myInputProbes.get(i);         
         ModelInfo info = getModelInfo (p);
         info.inputProbes.add (p);
      }
      for (int i = 0; i < myOutputProbes.size(); i++) {
         Probe p = myOutputProbes.get(i);
         ModelInfo info = getModelInfo (p);
         info.outputProbes.add (p);
      }
      for (ModelInfo info : myModelInfo.values()) {
         info.createState ();
      }
      myRootInfo.createState();
      myRootInfo.outputProbes.add (myWayPoints);
   }
   
   public boolean hasState() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public CompositeState createState (
      ComponentState prevState) {
      if (!myModelInfoValid) {
         updateModelInfo();
         myModelInfoValid = true;
      }      
      // state is a composite state for every model plus a numeric state
      // for the root model itself
      CompositeState state = new CompositeState();
      for (ModelInfo info : myModelInfo.values()) {
         state.addState (info.createFullState());
      }
      state.addState (myRootInfo.createFullState());
      return state;
   }

   protected ComponentState createRootState() {
      // Create local state to store adaptive integration info for all
      // the models
      int numMods = myModelInfo.size();
      int dsize = numMods*myRootInfo.getDStateSize();
      int zsize = numMods*myRootInfo.getZStateSize();
      return new NumericState(zsize, dsize);
   }

   /**
    * {@inheritDoc}
    */
   public void setState (ComponentState state) {
      if (!(state instanceof CompositeState)) {
         throw new IllegalArgumentException ("state is not a CompositeState");
      }
      CompositeState newState = (CompositeState)state;
      if (newState.numSubStates() != myModels.size()+1) {
         throw new IllegalArgumentException (
            "new state has "+newState.numSubStates()+
            " sub-states vs. "+(myModels.size()+1));
      }
      int k = 0;
      for (ModelInfo info : myModelInfo.values()) {
         info.setFullState ((CompositeState)newState.getState(k++));
      }
      myRootInfo.setFullState ((CompositeState)newState.getState(k++));      
   }
   
   public void resetInitialState() {
      myWayPoints.resetInitialState();
      rerender();
   }
   
   public void invalidateInitialState() {
      myWayPoints.invalidateInitialState();
   }
   
   /**
    * {@inheritDoc}
    */
   public void getState (ComponentState state) {
      if (!(state instanceof CompositeState)) {
         throw new IllegalArgumentException ("state is not a CompositeState");
      }
      CompositeState substate;
      CompositeState saveState = (CompositeState)state;
      saveState.clear();
      for (ModelInfo info : myModelInfo.values()) {
         substate = new CompositeState(saveState.isAnnotated());
         info.getFullState (substate);
         saveState.addState (substate);
      }
      substate = new CompositeState(saveState.isAnnotated());
      myRootInfo.getFullState (substate);
      saveState.addState (substate);
   }
   
   public void getInitialState (
      ComponentState newstate, ComponentState oldstate) {

      if (!(newstate instanceof CompositeState)) {
         throw new IllegalArgumentException (
            "newstate is not a CompositeState");
      }
      CompositeState saveState = (CompositeState)newstate;
      CompositeState substate;

      HashMap<ModelInfo,CompositeState> stateMap =
         new HashMap<ModelInfo,CompositeState>();

      if (oldstate != null) {
         if (!(oldstate instanceof CompositeState)) {
            throw new IllegalArgumentException (
               "oldstate is not a CompositeState");
         }
         if (myInitialInfos == null) {
            throw new IllegalStateException (
               "initialInfos not initialized");
         }
         CompositeState ostate = (CompositeState)oldstate;
         if (myInitialInfos.size() != ostate.numSubStates()) {
            throw new IllegalArgumentException (
               "oldstate has "+myInitialInfos.size()+" components vs. "+
               ostate.numSubStates()+" substates");
         }
         for (int k=0; k<myInitialInfos.size(); k++) {
            ModelInfo info = myInitialInfos.get(k);
            stateMap.put (info, (CompositeState)ostate.getState(k));
         }
      }
      saveState.clear();
      myInitialInfos = new ArrayList<ModelInfo>();
      for (ModelInfo info : myModelInfo.values()) {
         substate = new CompositeState();
         info.getInitialState (substate, stateMap.get(info));
         saveState.addState (substate);
         myInitialInfos.add (info);
      }
      substate = new CompositeState();
      myRootInfo.getInitialState (substate, stateMap.get(myRootInfo));
      saveState.addState (substate);
      myInitialInfos.add (myRootInfo);
   }

   protected void getRootState (ComponentState state) {
      if (!(state instanceof NumericState)) {
         throw new IllegalArgumentException ("state is not a NumericState");
      }
      // Get local state, including adaptive integration info for all models
      NumericState rootState = (NumericState)state;
      rootState.resetOffsets();      
      for (ModelInfo info : myModelInfo.values()) {
         info.getState (rootState);
      }
   }

   protected void setRootState (ComponentState state) {
      if (!(state instanceof NumericState)) {
         throw new IllegalArgumentException ("state is not a NumericState");
      }
      // Set local state, including adaptive integration info for all models
      NumericState rootState = (NumericState)state;
      rootState.resetOffsets();
      for (ModelInfo info : myModelInfo.values()) {
         info.setState (rootState);
      }
   }

   /**
    * Convenience method that creates and returns a {@link CompositeState}
    * containing the current state of the root model.
    * 
    * @param annotated specifies if the state should be annotated
    * @return current state of the root model
    */
   public CompositeState getState (boolean annotated) {
      CompositeState state = createState (null);
      if (annotated) {
         state.setAnnotated (true);
      }
      getState (state);
      return state;      
   }
   
   public StepAdjustment advance (
      double t0, double t1, int flags) {

      synchronized (this) {
         if (!myModelInfoValid) {
            updateModelInfo();
            myModelInfoValid = true;
         }
      }
      doadvance (t0, t1, flags);
      return null;
   }

   public synchronized void applyInputProbes (List<Probe> list, double t) {
      for (Probe p : list) {
         if (p.isActive() && 
             TimeBase.compare (p.getStartTime(), t) <= 0 && 
             TimeBase.compare (p.getStopTime(), t) >= 0) {
            p.apply (t);
         }
      }
   }

   public synchronized void applyControllers (
      List<Controller> list, double t0, double t1) {

      for (Controller c : list) {
         if (c.isActive()) {
            c.apply (t0, t1);
         }
      }
   }

   public synchronized void applyMonitors (
      List<Monitor> list, double t0, double t1) {

      for (Monitor m : list) {
         if (m.isActive()) {
            m.apply (t0, t1);
         }
      }
   }

   public synchronized void applyOutputProbes (
      List<Probe> list, double t1, ModelInfo info) {

      // see if t1 coincides with the model's max step size
      double maxStep = info.model.getMaxStepSize();
      boolean coincidesWithStep =
         (maxStep != -1 && TimeBase.modulo (t1, maxStep) == 0);  

      for (Probe p : list) {
         if (!p.isActive() ||
             TimeBase.compare (t1, p.getStartTime()) < 0 ||
             TimeBase.compare (t1, p.getStopTime()) > 0) {
            continue;
         }
         if (p.isEventTime(t1) || 
             (coincidesWithStep && p.getUpdateInterval() < 0)) {
            p.apply (t1);
         }
      }
   }

   private double nextProbeEvent (List<Probe> probes, double t0) {
      double te = Double.MAX_VALUE;
      for (Probe p : probes) {
         
         double ta = p.nextEventTime (t0);
         if (ta != -1 && ta < te) {
            if (debugNextAdvanceTime) {
               System.out.println (
                  "NextAdvanceTime: probe reducing te to " + ta +
                  ", " + p.getName() + " " + p);
            }
            te = ta;
         }
      }
      return te;
   }

   public double getNextAdvanceTime (
      List<Probe> probes, double stepSize, double t0, double t1) {

      // nextStepTime is the next time after t0 lying on a step boundary
      double nextStepTime = t0 + (stepSize-TimeBase.modulo(t0,stepSize));
      if (TimeBase.compare (nextStepTime, t1) < 0) {
         t1 = nextStepTime;
      }
      double te = nextProbeEvent (probes, t0);
      if (TimeBase.compare (te, t1) < 0) {
         t1 = te;
      }
      return TimeBase.round (t1);
   }

   private double getRecommendedScaling (StepAdjustment adj) {
      return adj != null ? adj.getScaling() : 1;
   }
   
   /** 
    * This is used by the scheduler to interrupts the current call to advance
    * and cause state to be restored to that of the start time for the advance.
    */
   public synchronized void stopAdvance() {
      myStopAdvance = true;
   }

   /**
    * If set true, tells the scheduler to stop simulating this root model.
    * Will be set to false by the scheduler when simulation is started.
    *
    * @param req if <code>true</code>, requests a simulation stop
    */
   public void setStopRequest (boolean req) {
      myStopRequest = req;
   }

   public boolean getStopRequest() {
      return myStopRequest;
   }
   
   protected void advanceModel (
      ModelInfo info, double t0, double t1, int flags) {

      double ta = t0;
      if (t0 == 0) {
         applyOutputProbes (info.outputProbes, t0, info);
      }
      while (ta < t1) {
         double s;
         synchronized (this) {
            info.getModelAndControllersState (info.state);
         }
         if (testSaveAndRestoreState) {  
            // test save-and-restore of model state 
            CompositeState fullState = info.createFullState();
            fullState.setAnnotated(true);
            CompositeState testState = info.createFullState();
            testState.setAnnotated(true);
            info.getFullState (fullState);
            info.setFullState (fullState);
            info.getFullState (testState);
            if (!testState.equals (fullState, null)) {
               throw new InternalErrorException (
                  "Error: save/restore state test failed");
            }  
         }
         
         double tb = info.getNextAdvanceTime (ta, t1);
         do {
            synchronized (this) {
               StepAdjustment adj;
               //info.model.setDefaultInputs (ta, tb);
               adj = info.model.preadvance (ta, tb, flags);
               s = getRecommendedScaling (adj);
               if (s >= 1) {
                  applyInputProbes (info.inputProbes, tb);
                  applyControllers (info.controllers, ta, tb);
                  adj = info.model.advance (ta, tb, flags);
                  s = getRecommendedScaling (adj);
               }
               if (myAdaptiveStepping && s < 1) {
                  tb = info.reduceAdvanceTime (
                     s, ta, tb, adj.getMessage());
                  info.setModelAndControllersState (info.state);
                  info.model.initialize (ta);
               }
            }
         }
         while (myAdaptiveStepping && s < 1 && !myStopAdvance);
         if (!(myAdaptiveStepping && s < 1)) {
            // then we have advanced to tb:
            info.updateStepInfo (s);
            applyMonitors (info.monitors, ta, tb);
            applyOutputProbes (info.outputProbes, tb, info);
            ta = tb;
         }
      }
   }

   protected void doadvance (double t0, double t1, int flags) {

      if (myWayPoints.isEventTime (t0)) {
         flags |= Model.STATE_IS_VOLATILE;
      }
      double ta = t0;
      if (t0 == 0) {
         applyOutputProbes (myRootInfo.outputProbes, t0, myRootInfo);
      }
      while (ta < t1 && !myStopRequest) {
         double tb = getNextAdvanceTime (
            myRootInfo.outputProbes, getMaxStepSize(), ta, t1);
         //setDefaultInputs (ta, tb);
         applyInputProbes (myRootInfo.inputProbes, tb);
         applyControllers (myRootInfo.controllers, ta, tb);
         for (Model m : myModels) {
            advanceModel (myModelInfo.get(m), ta, tb, flags);
         }
         applyMonitors (myRootInfo.monitors, ta, tb);
         applyOutputProbes (myRootInfo.outputProbes, tb, myRootInfo);
         ta = tb;
      }
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      
      // XXX - may not want to write the working directory to the model file
      // should be changed to "probe directory"
      pw.println ("workingDir="
                  + Write.getQuotedString (ArtisynthPath.getWorkingDirPath()));
      
      super.writeItems (pw, fmt, ancestor);
      // XXX - write way points, may want to have special purpose routine to
      // write
      // probes and waypoints. Probe file should have same format as model file
      // see Workspace.writeProbes()
      int savedWayPointFlags = myWayPoints.getWriteFlags();
      if (getTime() != 0 && savedWayPointFlags == 0) {
         myWayPoints.setWriteFlags (WayPointProbe.WRITE_FIRST_STATE);
      }
      pw.println ("waypoints=");
      myWayPoints.write (pw, fmt, this);
      if (getTime() != 0) {
         pw.println ("time=" + fmt.format(getTime()));
      }
      myWayPoints.setWriteFlags (savedWayPointFlags);
   }
   
   protected static void writeItems (
      RootModel root, PrintWriter pw, NumberFormat fmt)
      throws IOException {
      
      // XXX - may not want to write the working directory to the model file
      // should be changed to "probe directory"
      pw.println ("workingDir="
                  + Write.getQuotedString (ArtisynthPath.getWorkingDirPath()));
      
      myProps.writeNonDefaultProps (root, pw, fmt, root);
      if (!root.myComponents.getZeroBasedNumbering()) {
         pw.println ("zeroBasedNumbering=false");
      }
      root.myComponents.writeComponents (pw, fmt, root);     
 
      // XXX - write way points, may want to have special purpose routine to
      // write
      // probes and waypoints. Probe file should have same format as model file
      // see Workspace.writeProbes()
      int savedWayPointFlags = root.myWayPoints.getWriteFlags();
      if (root.getTime() != 0 && savedWayPointFlags == 0) {
         root.myWayPoints.setWriteFlags (WayPointProbe.WRITE_FIRST_STATE);
      }
      pw.println ("waypoints=");
      root.myWayPoints.write (pw, fmt, root);
      if (root.getTime() != 0) {
         pw.println ("time=" + fmt.format(root.getTime()));
      }
      root.myWayPoints.setWriteFlags (savedWayPointFlags);
   }
   
   /**
    * Special write method which allows us to write a root model using
    * only the information known to the RootModel class. This is 
    * used when writing components using core compatibility.
    * 
    * @param root model to write
    * @param pw print writer to write the model to
    * @param fmt format for floating point values
    */
   public static void write (RootModel root, PrintWriter pw, NumberFormat fmt) 
      throws IOException {

      IndentingPrintWriter.printOpening (pw, "[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      writeItems (root, pw, fmt);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");     
   }

   public void scanProbes (ReaderTokenizer rtok) throws IOException {
      Workspace.scanProbes (rtok, this);
   }

//   // WS
//   public void writeProbes (PrintWriter pw, NumberFormat fmt)
//      throws IOException {
//      Workspace.writeProbes (pw, fmt, this);
//   }
//
   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      super.scan (rtok, ref);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      
      rtok.nextToken();
      if (scanAttributeName (rtok, "workingDir")) {
         String dirName = rtok.scanQuotedString('"');
         File workingDir = new File (dirName);
         if (workingDir.exists() && workingDir.isDirectory()) {
            ArtisynthPath.setWorkingDir (workingDir);
         }
         return true;
      }
      else if (scanAttributeName (rtok, "waypoints")) {
         tokens.add (new StringToken ("waypoints", rtok.lineno()));
         myWayPoints.scan (rtok, tokens);
         return true;
      }
      else if (scanAttributeName (rtok, "time")) {
         myStartTime = rtok.scanNumber();
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (ScanWriteUtils.postscanAttributeName (tokens, "waypoints")) {
         myWayPoints.postscan (tokens, ancestor);
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
      for (ControlPanel cp : myControlPanels) {
         cp.pack();
         cp.setVisible (true);
      }
   }

   public void dispose() {
      for (Model m : myModels) {
         m.dispose();
      }
      for (Controller c : myControllers) {
         c.dispose();
      }
      for (Monitor m : myMonitors) {
         m.dispose();
      }
      // This dispose code not needed since dispose will now be called
      // be the control panel remove handler
      // for (ControlPanel cp : myControlPanels) {
      //    cp.dispose();
      // }
      myControlPanels.removeAll();
      
      if (myControlPanelsFrame != null) {
         myControlPanelsFrame.setVisible (false);
         myControlPanelTabs.removeAll ();
      }
   }

   public void setWayPointChecking (boolean enable) {
      myWayPoints.setCheckState (enable);
   }
   
   public boolean getWayPointChecking () {
      return myWayPoints.getCheckState();
   }
   
   // This check stuff is for checking repeatability by comparing
   // data with that computed on a previous run

   private BufferedReader myCheckReader = null;
   private PrintWriter myCheckWriter = null;
   private boolean myCheckEnable = false;

   public boolean isCheckEnabled() {
      return myCheckEnable;
   }

   public void setCheckEnabled (boolean enable) {
      myCheckEnable = enable;
   }

   private boolean openCheckFiles() {
      try {
         String rootName = getName();
         File file = new File (rootName + "_state.txt");
         if (file.exists()) {
            myCheckReader = new BufferedReader (new FileReader (file));
            System.out.println ("CHECK STATE BEGIN, checking");
         }
         else {
            myCheckWriter =
               new PrintWriter (new BufferedWriter (new FileWriter (file)));
            System.out.println ("CHECK STATE BEGIN, writing");
         }
      }
      catch (Exception e) {
         System.out.println ("Check error: " + e.getMessage());
         return false;
      }
      return true;
   }

   public void checkWrite (String str) {
      if (!myCheckEnable) {
         return;
      }
      if (myCheckReader == null && myCheckWriter == null) {
         if (!openCheckFiles()) {
            myCheckEnable = false;
            return;
         }
      }
      try {
         if (myCheckWriter != null) {
            myCheckWriter.println (str);
            myCheckWriter.flush();
         }
         else {
            String check = myCheckReader.readLine();
            if (check == null) {
               System.out.println ("CHECK FINISHED, time "+getTime());
               myCheckEnable = false;
               return;
            }
            else if (!check.equals (str)) {
               System.out.println ("CHECK FAILED, time "+getTime());
               System.out.println ("original:");
               System.out.println (check);
               System.out.println ("current:");
               System.out.println (str);
               myCheckEnable = false;
            }
         }
      }
      catch (Exception e) {
         System.out.println ("Check error: " + e.getMessage());
         myCheckEnable = false;
      }
   }

   protected void createControlPanelsFrame() {
      myControlPanelsFrame = new JFrame (myName + ": Control panels");
      myControlPanelTabs = new JTabbedPane();
      myControlPanelsFrame.setContentPane (myControlPanelTabs);
   }

   public void mergeAllControlPanels (boolean combine) {
      JFrame frame = getMainFrame();
      if (frame == null) {
         // ArtiSynth is running in batch mode, so do nothing
         return;
      }
      if (myControlPanelsFrame == null) {
         createControlPanelsFrame();
      }
      if (!myControlPanels.isEmpty ()) {         
         for (ControlPanel panel : myControlPanels) {
            Container contentPane = panel.getFrame ().getContentPane ();
            
            if (combine) {
               myControlPanelTabs.addTab (
                  panel.getFrame().getTitle(), contentPane);
            }
            else {
               panel.getFrame ().setContentPane (contentPane);
            }
            
            panel.setVisible (!combine);
         }
         
         myControlPanelsFrame.pack ();
         
         if (myControlPanelTabs.getTabCount () == 0) {
            myControlPanelsFrame.setVisible (false);
         }
         else if (!myControlPanelsFrame.isVisible ()) {
            Point loc = frame.getLocation();
            myControlPanelsFrame.setLocation (loc.x + frame.getWidth(), loc.y);
            myControlPanelsFrame.setVisible (true);
         }
      }
   }
   
   public JTabbedPane getControlPanelTabs() {
      return myControlPanelTabs;
   }

   public void mergeControlPanel (boolean combine, ControlPanel panel) {
      JFrame frame = getMainFrame();
      if (frame == null) {
         // ArtiSynth is running in batch mode, so do nothing
         return;
      }
      if (myControlPanelsFrame == null) {
         createControlPanelsFrame();
      }
      Container contentPane = panel.getFrame ().getContentPane ();
      
      if (combine) {
         myControlPanelTabs.addTab (panel.getFrame().getTitle(), contentPane);
      }
      else {
         panel.getFrame ().setContentPane (contentPane);
      }
      
      panel.setVisible (!combine);
      
      myControlPanelsFrame.pack ();
      
      if (myControlPanelTabs.getTabCount () == 0) {
         myControlPanelsFrame.setVisible (false);
      }
      else if (!myControlPanelsFrame.isVisible ()) {
         Point loc = frame.getLocation();
         myControlPanelsFrame.setLocation (loc.x + frame.getWidth(), loc.y);
         myControlPanelsFrame.setVisible (true);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void notifyParentOfChange (ComponentChangeEvent e) {
      // no parent, so just call component change:
      componentChanged (e);
   }

   /**
    * Find the most immediate RootModel, if any, that is an ancestor of a 
    * specified component.
    *
    * @param comp component to seek RootModel for
    * @return Most immediate RootModel ancestor, or <code>null</code>
    */
   public static RootModel getRoot (ModelComponent comp) {
      while (comp != null) {
         if (comp instanceof RootModel) {
            return (RootModel)comp;
         }
         comp = comp.getParent();
      }
      return null;      
   }

   /**
    * Queries if a specified component has a RootModel as an ancestor.
    *
    * @param comp component to query 
    * @return true if <code>comp</code> has a RootModel as an ancestor.
    */
   public static boolean hasRoot (ModelComponent comp) {
      return getRoot (comp) != null;
   }

   /**
    * Finds and returns the path name of the source directory for this RootModel.
    *
    * @return path name for the root model source directory
    */
   public String findSourceDir() {
      return PathFinder.findSourceDir (this);
   }

   /**
    * Finds and returns the path name of a file whose location is specified
    * relative to the source directory for this RootModel. This is created by
    * appending {\tt relpath} to the path name returned by {@link
    * #findSourceDir}.
    *
    * @param relpath path giving the location of the file relative to
    * the root model source directory 
    * @return path name for the specified file
    */
   public String getSourceRelativePath (String relpath) {
      return PathFinder.getSourceRelativePath (this, relpath);
   }
}
