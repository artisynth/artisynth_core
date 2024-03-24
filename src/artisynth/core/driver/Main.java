/**
 * copyright (c) 2017, by the Authors: John E Lloyd (UBC) and ArtiSynth
 * Team Members.  Elliptic selection added by Doga Tekin (ETH).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.*;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.DoubleHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import artisynth.core.driver.ModelScriptInfo.InfoType;
import artisynth.core.driver.Scheduler.Action;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.Timeline;
import artisynth.core.gui.editorManager.EditorManager;
import artisynth.core.gui.editorManager.TransformComponentsCommand;
import artisynth.core.gui.editorManager.UndoManager;
import artisynth.core.gui.jythonconsole.ArtisynthJythonConsole;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.gui.timeline.TimelineController;
import artisynth.core.inverse.InverseManager;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.CollisionManager.ColliderType;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HasSurfaceMesh;
import artisynth.core.mechmodels.MechSystem;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.MechSystemSolver;
import artisynth.core.mechmodels.MechSystemSolver.PosStabilization;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeListener;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.HasCoordinateFrame;
import artisynth.core.modelbase.Model;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.ModelBase;
import artisynth.core.modelbase.OrientedTransformableGeometry;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.moviemaker.MovieMaker;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.probes.Probe;
import artisynth.core.probes.WayPoint;
import artisynth.core.probes.WayPointProbe;
import artisynth.core.util.AliasTable;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.ArtisynthPath;
import maspack.util.ClassAliases;
import artisynth.core.util.MatlabInterface;
import artisynth.core.workspace.AddMarkerTool;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.PullController;
import artisynth.core.workspace.RenderProbe;
import artisynth.core.workspace.RootModel;
import artisynth.core.workspace.Workspace;
import artisynth.core.renderables.VertexComponent;
import maspack.geometry.ConstrainedTranslator3d;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.GeometryTransformer.UndoState;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.Vector3d;
import maspack.render.Dragger3dAdapter;
import maspack.render.Dragger3dBase;
import maspack.render.Dragger3dEvent;
import maspack.render.GraphicsInterface;
import maspack.render.Renderable;
import maspack.render.Renderer.HighlightStyle;
import maspack.render.RotatableScaler3d;
import maspack.render.Rotator3d;
import maspack.render.Translator3d;
import maspack.render.Transrotator3d;
import maspack.render.GL.GLSupport;
import maspack.render.GL.GLSupport.GLVersionInfo;
import maspack.render.GL.GLViewer;
import maspack.render.Viewer;
import maspack.widgets.GuiUtils;
import maspack.render.GL.GLViewerFrame;
import maspack.solvers.PardisoSolver;
import maspack.solvers.SparseSolverId;
import maspack.util.ClassFinder;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.Logger;
import maspack.util.FunctionTimer;
import maspack.util.Logger.LogLevel;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.widgets.MouseBindings;
import maspack.widgets.PropertyWindow;
import maspack.widgets.RenderPropsDialog;
import maspack.widgets.ViewerKeyListener;
import maspack.widgets.ViewerToolBar;
import maspack.widgets.GuiUtils.RelativeLocation;

/**
 * main class for running the ArtiSynth application. Puts all the pieces
 * together.
 */
public class Main implements DriverInterface, ComponentChangeListener {

   public static boolean myUseConfigDir = true;
   public static boolean myUseRootModelManager = true;

   /**
    * private data members for the driver interface 
    */
   protected File myModelDir;
   protected File myProbeDir;
   protected File myConfigDir; // directory containing configuration files

   protected MainFrame myFrame;
   protected MenuBarHandler myMenuBarHandler;
   protected GenericKeyHandler myKeyHandler;
   protected static Main myMain;
   protected Scheduler myScheduler;
   protected EditorManager myEditorManager;
   protected UndoManager myUndoManager;
   protected MovieMaker myMovieMaker;
   public static LogLevel DEFAULT_LOG_LEVEL = LogLevel.WARN;
   protected Logger myLogger;

   // preferences
   protected PreferencesManager myPreferencesManager;
   protected ViewerPrefs myViewerPrefs;
   protected ViewerOpenGLPrefs myViewerOpenGLPrefs;
   protected ViewerGridPrefs myViewerGridPrefs;
   protected InteractionPrefs myInteractionPrefs;
   protected InteractionSettings myInteractionSettings;
   protected LayoutPrefs myLayoutPrefs;
   protected SimulationPrefs mySimulationPrefs;
   protected SimulationSettings mySimulationSettings;
   protected MousePrefs myMousePrefs;
   protected MoviePrefs myMoviePrefs;
   protected MaintenancePrefs myMaintenancePrefs;

   // startup model
   protected StartupModel myStartupModel;

   public static int DEFAULT_VIEWER_WIDTH = 720;
   public static int DEFAULT_VIEWER_HEIGHT = 540;
   public static int DEFAULT_SCREEN_LOC_X = 10;
   public static int DEFAULT_SCREEN_LOC_Y = 10;
   protected int myViewerWidth;
   protected int myViewerHeight;
   protected int myScreenLocX;
   protected int myScreenLocY;

   protected static boolean myRunningUnderMatlab = false;

   // root model declared
   protected RootModelManager myRootModelManager;
   protected RootModelUpdateThread myRootModelUpdateThread;
   protected Workspace myWorkspace = null;

   public static boolean DEFAULT_TIMELINE_VISIBLE = true;
   public static int DEFAULT_TIMELINE_WIDTH = 800;
   public static int DEFAULT_TIMELINE_HEIGHT = 400;

   protected Timeline myTimeline;
   protected boolean myTimelinePreviouslyVisible = false;
   public static double DEFAULT_TIMELINE_RANGE = -1; // -1 means automatic
   protected double myDefaultTimelineRange = DEFAULT_TIMELINE_RANGE;
   public static RelativeLocation DEFAULT_TIMELINE_LOCATION =
      RelativeLocation.BELOW;
   protected RelativeLocation myTimelineLocation = DEFAULT_TIMELINE_LOCATION;
   
   public static double DEFAULT_FRAME_RATE = 20.0;
   protected double myFrameRate = DEFAULT_FRAME_RATE;

   protected String myErrMsg;
   protected GLViewer myViewer;
   protected SelectionManager mySelectionManager = null;

   public static boolean DEFAULT_JYTHON_FRAME_VISIBLE = false;
   protected ArtisynthJythonConsole myJythonConsole = null;
   protected JFrame myJythonFrame = null;
   protected boolean myJythonPreviouslyVisible = false;
   public static RelativeLocation DEFAULT_JYTHON_LOCATION =
      RelativeLocation.BELOW;
   protected RelativeLocation myJythonLocation = DEFAULT_JYTHON_LOCATION;
   protected ViewerManager myViewerManager;
   //protected AliasTable myDemoModels;
   protected ModelScriptHistory myModelScriptHistory = null;  // storage for model history
   protected boolean myFocusStealingMaskedP = false;

   protected final static String PROJECT_NAME = "ArtiSynth";
   protected MatlabInterface myMatlabConnection;

   protected String myModelName;
   protected ModelScriptInfo myLastLoadInfo; // for re-loading a model with the same parameters
   protected double myMaxStep = ModelBase.DEFAULT_MAX_STEP_SIZE;
   protected boolean disposed = false;

   static GraphicsInterface DEFAULT_GRAPHICS = GraphicsInterface.GL3;
   protected GraphicsInterface myGraphics = DEFAULT_GRAPHICS;

   static LookAndFeel DEFAULT_LOOK_AND_FEEL = LookAndFeel.DEFAULT;
   protected LookAndFeel myLookAndFeel = DEFAULT_LOOK_AND_FEEL;

   protected String myModelSaveFormat = "%g"; // "%.8g";

   private Vector3d myVec = new Vector3d();

   protected boolean myUndoTransformsWithInverse = false;

   public Vector3d getVector() {
      return myVec;
   }

   public void setVector (Vector3d v) {
      myVec.set (v);
   }

   public enum SelectionMode {
      Scale,
      Select,
      EllipticSelect,
      Translate,
      Rotate,
      ConstrainedTranslate,
      Transrotate,
      Pull,
      AddComponent, 
      AddMarker
   }

   public enum LookAndFeel {
      DEFAULT,
      METAL,
      SYSTEM;

      // like valueOf() but returns null if no match
      public static LookAndFeel fromString (String str) {
         try {
            return valueOf (str);
         }
         catch (Exception e) {
            return null;
         }
      }

   };

   /** 
    * Describes different ways to determine the frame for a manipulator.
    */
   public enum ManipulatorFrameSpec {
      LOCAL,
      WORLD;

      private static ManipulatorFrameSpec[] vals = values();
      public ManipulatorFrameSpec next() {
        return vals[(this.ordinal()+1) % vals.length];
      }      
   };

   /**
    * Information about offests and resizing for a manipulator frame.
    */
   class DraggerFrameInfo {
      RigidTransform3d myTOFF = new RigidTransform3d();
      double mySizeScale = 1;
   }

   private SelectionMode mySelectionMode;
   public static boolean DEFAULT_ARTICULATED_TRANSFORMS = false;
   private boolean myArticulatedTransformsP = DEFAULT_ARTICULATED_TRANSFORMS;
   public static boolean DEFAULT_INIT_DRAGGERS_IN_WORLD = false;
   private boolean myInitDraggersInWorldCoordsP = DEFAULT_INIT_DRAGGERS_IN_WORLD;
   public static boolean DEFAULT_ALIGN_DRAGGERS_TO_POINTS = false;
   private boolean myAlignDraggersToPointsP = DEFAULT_ALIGN_DRAGGERS_TO_POINTS;

   private Translator3d translator3d = new Translator3d();
   private Transrotator3d transrotator3d = new Transrotator3d();
   private RotatableScaler3d scalar3d = new RotatableScaler3d();
   private Rotator3d rotator3d = new Rotator3d();
   private ConstrainedTranslator3d constrainedTranslator3d =
      new ConstrainedTranslator3d();
   private Dragger3dBase currentDragger;
   private LinkedList<ModelComponent> myDraggableComponents =
      new LinkedList<ModelComponent>();
   private HashMap<ModelComponent,DraggerFrameInfo>
      myDraggerFrameInfoMap = new HashMap<>();

   /**
    * Filter to determine classes which belong to artisynth_core.
    */
   private static class CoreFilter implements ClassAliases.ClassFilter {
      
      public boolean isValid (Class<?> cls) {
         if (cls.getPackage() != null) {
            String packName = cls.getPackage().getName();
            return (packName.startsWith ("artisynth.core") ||
                    packName.startsWith ("maspack") ||
                    packName.startsWith ("java") ||
                    packName.startsWith ("javax"));
         }
         else {
            return true;
         }
      }
   }
   
//   public enum ViewerMode {
//      FrontView, TopView, SideView, BottomView, RightView, BackView
//   }
//
//   private ViewerMode viewerMode;

   // protected boolean myOrthographicP;
   // private double cpuLoad = .8;

   private File myProbeFile = null;
   private File myModelFile = null;
   private boolean mySaveWayPointData = false;
   private boolean mySaveCoreOnly = false;

   public static void setRunningUnderMatlab (boolean underMatlab) {
      myRunningUnderMatlab = underMatlab;
   }

   public static boolean isRunningUnderMatlab () {
      return myRunningUnderMatlab;
   }

   public void setUndoTransformsWithInverse (boolean enable) {
      myUndoTransformsWithInverse = enable;
   }

   public boolean getUndoTransformsWithInverse () {
      return myUndoTransformsWithInverse;
   }

   // MovieOptions movieOptions = new MovieOptions();

   public SelectionManager getSelectionManager() {
      return mySelectionManager;
   }

   /**
    * get the key bindings from a file
    * 
    * @return string with keybindings
    */
   public String getKeyBindings() {
      return GenericKeyHandler.getKeyBindings();
   }

   public void setErrorMessage (String msg) {
      myErrMsg = msg;
   }

   public void clearErrorMessage () {
      myErrMsg = null;
   }

   public String getErrorMessage() {
      return myErrMsg;
   }

   public GLViewer getViewer() {
      return myViewer;
   }

   public MainFrame getMainFrame() {
      return myFrame;
   }

   public JFrame getFrame() {
      return myFrame;
   }

   public PreferencesManager getPreferencesManager() {
      return myPreferencesManager;
   }

   public void createPreferencesManager() {
      File prefFile = ArtisynthPath.getConfigFile ("settings/preferences");
      if (prefFile != null) {
         PreferencesManager manager = new PreferencesManager(prefFile);

         myViewerPrefs = new ViewerPrefs (this);
         myViewerOpenGLPrefs = new ViewerOpenGLPrefs (myViewerManager);
         myViewerGridPrefs = new ViewerGridPrefs (myViewerManager);
         myViewerManager.setPreferences (myViewerPrefs);

         myInteractionSettings = new InteractionSettings (myMain);
         myInteractionPrefs = new InteractionPrefs (myInteractionSettings);
         myInteractionSettings.setPreferences (myInteractionPrefs);

         myLayoutPrefs = new LayoutPrefs(this);

         mySimulationSettings = new SimulationSettings();
         mySimulationPrefs = new SimulationPrefs(mySimulationSettings);
         mySimulationSettings.setPreferences (mySimulationPrefs);

         myMousePrefs = new MousePrefs(this);
         myMoviePrefs = new MoviePrefs(getMovieMaker());

         myMaintenancePrefs = new MaintenancePrefs (this);

         manager.addProps ("Viewer", "Viewer", myViewerPrefs);
         manager.addProps ("Viewer.Grid", "ViewerGrid", myViewerGridPrefs);
         manager.addProps ("Viewer.OpenGL", "ViewerOpenGL", myViewerOpenGLPrefs);
         manager.addProps (
            "Interaction", "Interaction", myInteractionPrefs);
         manager.addProps ("Layout", "Layout", myLayoutPrefs);
         manager.addProps ("Simulation", "Simulation", mySimulationPrefs);
         manager.addProps ("Mouse", "Mouse", myMousePrefs);
         manager.addProps ("Movies", "Movies", myMoviePrefs);
         manager.addProps ("Maintenance", "Maintenance", myMaintenancePrefs);
         manager.loadOrCreate();
         myPreferencesManager = manager;
      }
   }

   void createStartupModel() {
      File configFile = ArtisynthPath.getConfigFile ("settings/startupModel");
      if (configFile != null) {
         StartupModel config = new StartupModel (configFile);
         config.loadOrCreate();
         myStartupModel = config;
      }
   }

   void createModelScriptHistory() {
      File historyFile = null;
      if (historyFilename.value != null) {
         historyFile = new File (historyFilename.value);
      }
      if (historyFile == null) {
         historyFile = ArtisynthPath.getConfigFile ("cache/modelScriptHistory");
      }
      if (historyFile != null) {
         ModelScriptHistory history = new ModelScriptHistory (historyFile);
         history.loadOrCreate();
         myModelScriptHistory = history;
      }      
   }
      

//   // changed from protected to public for cubee
//   public String[] getDemoNames() {
//      return myDemoModels.getAliases();
//   }

   public String getDemoClassName (String classNameOrAlias) {
      String name =
         myRootModelManager.findModelFromSimpleName (classNameOrAlias);
      if (name != null) {
         return name;
      }
      else {
         try {
            ClassFinder.forName (classNameOrAlias, false);
         }
         catch (ClassNotFoundException e) {
            return null;
         }
         return classNameOrAlias;
      }
   }

//   protected boolean isDemoName (String name) {
//      return myDemoModels.containsAlias (name);
//   }

//   protected boolean isDemoClassName(String classNameOrAlias) {
//      if (myDemoModels.containsAlias (classNameOrAlias)) {
//         return true;
//      }
//      return myDemoModels.containsName (classNameOrAlias);
//   }

   public GraphicsInterface getGraphics() {
      return myGraphics;
   }   

   public LookAndFeel getLookAndFeel() {
      return myLookAndFeel;
   }   

   /**
    * Returns the current model name. This is either the name of the root model,
    * or the command or file name associated with it.
    * 
    * @return current model name.
    */
   public String getModelName() {
      return myModelName;
   }

   public Main() {
      this (PROJECT_NAME, /*createGui=*/true);
   }

   String getModelMenuFilename() {
      return modelMenuFilename.value;
   }
   
   String getScriptMenuFilename() {
      return scriptMenuFilename.value;
   }
   
   String getDemoFilename() {
      return demoFilename.value;
   }
   
   public ModelScriptHistory getModelScriptHistory() {
      return myModelScriptHistory;
   }

   private String getScriptFileName (File file, Pattern pattern) {
      if (!file.canRead()) {
         return null;
      }
      
      BufferedReader reader = null;
      try {
         reader = new BufferedReader (new FileReader (file));
         String firstLine = reader.readLine();
         Matcher matcher = pattern.matcher (firstLine);
         if (matcher.matches()) {
            return matcher.group(1);
         }
         else {
            return null;
         }
      }
      catch (Exception e) {
         return null;
      } finally {
         closeQuietly(reader);
      }
   }
   
   private void closeQuietly(Reader reader) {
      if (reader != null) {
         try {
            reader.close();
         } catch (Exception e) {
         }
      }
   }
   private static class MainFrameConstructor implements Runnable {
      Main myMain;
      String myName;
      int myWidth;
      int myHeight;
      public MainFrameConstructor(String windowName, Main main, int width, int height) {
         myMain = main;
         myName = windowName;
         myWidth = width;
         myHeight = height;
      }

      @Override
      public void run() {
         myMain.myFrame = new MainFrame (myName, myMain, myWidth, myHeight);
         myMain.myFrame.setLocation (myMain.myScreenLocX, myMain.myScreenLocY);
      }
   }
   
   private static class ViewerResizer implements Runnable {
      MainFrame myFrame;
      int myWidth;
      int myHeight;
      
      public ViewerResizer(MainFrame frame, int width, int height) {
         myFrame = frame;
         myWidth = width;
         myHeight = height;
      }
      
      public void run() {
         myFrame.setViewerSize (myWidth, myHeight);
      };
   }

   public void setViewerSize (int w, int h) {
      // execute in AWT thread to prevent deadlock
      if (!SwingUtilities.isEventDispatchThread()) {
         try {
            SwingUtilities.invokeAndWait(new ViewerResizer(myFrame, w, h));
         } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
         }
      }
      else {
         myFrame.setViewerSize (w, h);
      }
   }
   
//   public void setViewerSize (Dimension size) {
//      // execute in AWT thread to prevent deadlock
//      setViewerSize (size.width, size.height);
//   }

   public Dimension getViewerSize() {
      return new Dimension (
         myViewer.getScreenWidth(), myViewer.getScreenHeight());
   }

   private void initializeRootModelManager() {
      myRootModelManager = new RootModelManager();
      File cacheFile = ArtisynthPath.getConfigFile ("cache/RootModels");
      if (cacheFile != null) {
         if (cacheFile.exists()) {
            if (!cacheFile.canRead()) {
               System.out.println (
                  "WARNING: root model cache file "+cacheFile+" not readable");
            }
            else {
               myRootModelManager.setCacheFile (cacheFile);
               myRootModelManager.readCacheFile();
            }
         }
         else {
            // try to create a cache file
            System.out.println (
               "Creating root model cache file "+cacheFile);
            myRootModelManager.setCacheFile (cacheFile);
            myRootModelManager.loadPackage ("artisynth.demos");
            myRootModelManager.loadPackage ("artisynth.models");
            myRootModelManager.writeCacheFile();
         }
      }
   }

   class RootModelUpdateThread extends Thread {

      public void run() {
         FunctionTimer timer = new FunctionTimer();
         timer.start();
         myRootModelManager.updateModelSet();
         if (!myRootModelManager.compareCacheToMain()) {
            // cache is different than main
            myRootModelManager.writeCacheFile();
//            if (myMenuBarHandler != null) {
//               myMenuBarHandler.updateDemosMenu(); 
//            }
         }
         if (myMenuBarHandler != null) {
            myMenuBarHandler.updateDemosMenu(); 
         }        
         timer.stop();
         // System.out.println (
         //    "Root model update thread finished, "+timer.result(1));
      }
   }

   RootModelUpdateThread getRootModelUpdateThread() {
      return myRootModelUpdateThread;
   }

   /**
    * Creates a new Main instance
    * 
    * @param windowName name of window frame
    * @param createGui whether or not to create the GUI
    */
   public Main (String windowName, boolean createGui) {

      myMain = this;

      // create viewer manager now, and even if there is no gui, because the
      // preferences manager wants one
      myViewerManager = new ViewerManager(createGui);
      
      if (myUseConfigDir) {
         // set user config directory and preferences manager if possible
         createPreferencesManager();
         createStartupModel();
      }

      if (myUseRootModelManager) {
         initializeRootModelManager();
      }
      
      setClassAliases();

      GraphicsInterface gi = null;
      if (createGui) {

         // set the Swing look and feel if necessary
         LookAndFeel laf = myLayoutPrefs.getLookAndFeel();
         if (!"".equals(lookAndFeel.value)) {
            // graphics specified on the command line
            LookAndFeel cmdlaf =
               LookAndFeel.fromString (lookAndFeel.value);
            if (cmdlaf == null) {
               System.out.println (
                  "Unknown look and feel '"+lookAndFeel.value+"'; using "+laf);
            }
            else {
               laf = cmdlaf;
            }
         }
         if (laf != LookAndFeel.DEFAULT) {
            try {
               switch (laf) {
                  case METAL: {
                     UIManager.setLookAndFeel(
                        UIManager.getCrossPlatformLookAndFeelClassName());
                     break;
                  }
                  case SYSTEM: {
                     UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
                     break;
                  }
                  // case FLATLAF: {
                  //    UIManager.setLookAndFeel(
                  //       new com.formdev.flatlaf.FlatLightLaf());
                  // }
                  default: {
                     System.out.println (
                        "WARNING: Uknown look and feel '"+laf+"', ignoring");
                  }
               }
            }
            catch( Exception ex ) {
               ex.printStackTrace(); 
               System.err.println(
                  "ERROR: Failed to initialize look and feel '"+laf+"'");
            }
         }
         myLookAndFeel = laf;

         gi = myViewerPrefs.getGraphics();
         if (!"".equals(graphicsInterface.value)) {
            // graphics specified on the command line
            GraphicsInterface cmdgi =
               GraphicsInterface.fromString (graphicsInterface.value);
            if (cmdgi == null) {
               System.out.println (
                  "Unknown graphics '"+graphicsInterface.value+"'; using "+gi);
            }
            else {
               gi = cmdgi;
            }
         }
         gi = GraphicsInterface.checkAvailability (gi);
         if (gi == null) {
            // if graphics not supported and there is no substitute
            System.exit(1); 
         }
      }
      myGraphics = gi;
      
      myEditorManager = new EditorManager (this);
      myUndoManager = new UndoManager();

      // need to create selection manager before MainFrame, because
      // some things in MainFrame will assume it exists
      mySelectionManager = new SelectionManager();


      if (createGui) {
         ToolTipManager.sharedInstance().setLightWeightPopupEnabled (false);

         myViewerWidth = myLayoutPrefs.getViewerWidth();
         if (viewerWidth.value != -1) {
            myViewerWidth = viewerWidth.value;
         }
         myViewerHeight = myLayoutPrefs.getViewerHeight();
         if (viewerHeight.value != -1) {
            myViewerHeight = viewerHeight.value;
         }        
         myScreenLocX = myLayoutPrefs.getScreenLocX();
         if (screenLocX.value != -1) {
            myScreenLocX = screenLocX.value;
         }
         myScreenLocY = myLayoutPrefs.getScreenLocY();
         if (screenLocY.value != -1) {
            myScreenLocY = screenLocY.value;
         }
         // execute in AWT thread to prevent deadlock
         try {
            SwingUtilities.invokeAndWait (
               new MainFrameConstructor(
                  windowName, this, myViewerWidth, myViewerHeight));
         } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
         }
         
         myMenuBarHandler = myFrame.getMenuBarHandler();
         mySelectionManager.setNavPanel (myFrame.getNavPanel());
         myFrame.getNavPanel().setSelectionManager (mySelectionManager);

         myKeyHandler = new GenericKeyHandler(this);
         
         //myFrame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
         myViewer = myFrame.getViewer();
         myViewer.addRenderListener (myMenuBarHandler);
         myMovieMaker.setViewer (myViewer);

         // set stored viewer preferences
         myViewerPrefs.applyToCurrent();

         // set viewerManager default overrides from the command line
         if (bgColor[0] != -1f) {
            myViewerManager.setBackgroundColor (
               new Color(bgColor[0], bgColor[1], bgColor[2]));
         }

         if (axialView.value != null) {
            if (axialView.value.equals ("xy")) {
               myViewerManager.setDefaultAxialView (AxisAlignedRotation.X_Y);
            }
            else if (axialView.value.equals ("xz")) {
               myViewerManager.setDefaultAxialView (AxisAlignedRotation.X_Z);
            }
            else {
               System.out.println (
                  "WARNING: Unknown axial view: " + axialView.value);
            }
         }

         myViewerManager.setDefaultOrthographicView (orthographic.value);
         myViewerManager.setDefaultDrawGrid (drawGrid.value);
         myViewerManager.setDefaultDrawAxes (drawAxes.value);
         myViewerManager.setDefaultAxisLength (axisLength.value);

         // mouse preferences
         myMousePrefs.applyToCurrent();
         if (mousePrefs.value != null) {
            // override bindings
            setMouseBindings (mousePrefs.value);
         }
         
         myViewerManager.addViewer (myViewer);

         initializeViewer (myViewer);

         setSelectionMode (SelectionMode.Select);

         addSelectionListener (new SelectionHandler());

         Dragger3dHandler draggerHandler = new Dragger3dHandler();
         translator3d.addListener (draggerHandler);
         scalar3d.addListener (draggerHandler);
         rotator3d.addListener (draggerHandler);
         transrotator3d.addListener (draggerHandler);
         constrainedTranslator3d.addListener (draggerHandler);

         myViewerManager.addDragger (translator3d);
         myViewerManager.addDragger (scalar3d);
         myViewerManager.addDragger (rotator3d);
         myViewerManager.addDragger (transrotator3d);
         myViewerManager.addDragger (constrainedTranslator3d);
         
         //myFrame.getGLPanel().setSize (myViewerWidth, myViewerHeight);

         myPullController = new PullController (mySelectionManager);
         myAddMarkerHandler = new AddMarkerTool (mySelectionManager);

         if (myUseRootModelManager && myRootModelManager.hasCache()) {
            myRootModelUpdateThread = new RootModelUpdateThread();
            myRootModelUpdateThread.start();
         }

         createModelScriptHistory();
      }

      // update movie maker settings with command line options
      myMoviePrefs.applyToCurrent();
      if (movieMethod.value != null) {
         MovieMaker.Method method = myMovieMaker.getMethod (movieMethod.value);
         if (method != null) {
            myMovieMaker.setMethod (method);
         }
         else {
            System.out.println (
               "WARNING: option '-movieMethod': unknown method '" +
               movieMethod.value+"'");
         }
      }
      if (movieFrameRate.value != -1) {
         try {
            myMovieMaker.setFrameRate (movieFrameRate.value);
         }
         catch (Exception e) {
            System.out.println (
               "WARNING: option '-movieFrameRate': illegal rate " +
               movieFrameRate.value);
         }
      }
      createWorkspace();
   }

   /**
    * Returns a logger to be used by this main application.  If this instance of Main does not have
    * an associated logger, then the system logger is returned.
    * @return a logger to use by this instance of the main application
    */
   public Logger getLogger() {
      if (myLogger == null) {
         return Logger.getSystemLogger();
      }
      return myLogger;
   }
   
   /**
    * Sets an application-specific logger
    * @param logger logger to use in this main application
    */
   public void setLogger(Logger logger) {
      myLogger = logger;
   }
   
   /**
    * @return the current log level
    */
   public LogLevel getLogLevel() {
      return getLogger().getLogLevel();
   }
   
   /**
    * Sets the current log level for this application.  If no logger is
    * specified using {@link #setLogger(Logger)}, then the system logger is
    * cloned and assigned in order to set the log level.
    * @param level log level
    */
   public void setLogLevel(LogLevel level) {
      if (myLogger == null) {
         try {
            myLogger = Logger.getSystemLogger().clone();
         } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return;
         }
      }
      myLogger.setLogLevel(level);
   }
   
   /**
    * to set the timeline visible or not
    * 
    * @param visible -
    * boolean set the timeline visible
    */

   public void setTimelineVisible (boolean visible) {
      if (visible) {
         if (myTimeline == null)
            createTimeline(); //TODO

         myTimeline.setVisible (true);
      }
      else {
         if (myTimeline != null) {
            myTimeline.setVisible (false);
         }
      }
      // locate the timeline if not previously visible
      if (visible && !myTimelinePreviouslyVisible) {
         // set whether timeline is to the right
         RelativeLocation loc = myLayoutPrefs.getTimelineLocation();
         if (timelineLocation.value != null) {
            RelativeLocation cmdloc =
               RelativeLocation.fromString (timelineLocation.value);
            if (cmdloc == null) {
               System.out.println (
                  "Unknown timeline location '"+timelineLocation.value+
                  "'; using "+loc);
            }
            else {
               loc = cmdloc;
            }
         }
         setTimelineLocation (loc);
         myTimelinePreviouslyVisible = true;
      }
   }

   /**
    * Returns the scale factor used to control the visualized simulation
    * speed in the viewer. See {@link setRealTimeScaling}.
    */
   public double getRealTimeScaling() {
      double scaling = myScheduler.getRealTimeScaling ();
      if (!myScheduler.getRealTimeAdvance ()) {
         scaling = -1;
      } 
      return scaling;
   }

   /**
    * Sets the scale factor used to control the visualized simulation speed in
    * the viewer, subject to computational constraints. The default value is 1,
    * which means that the viewer tries to show the simulation in real
    * time. Values {@code >} 1 will speed up the simulation, while values
    * {@code <} 1 will slow it down. A value {@code <=} 0 will cause the
    * simulation to be displayed as fast as possible.
    */
   public void setRealTimeScaling (double scaling) {
      if (scaling != getRealTimeScaling()) {
         if (scaling <= 0) {
            setRealTimeAdvance (false);
         }
         else {
            setRealTimeAdvance (true);
            myScheduler.setRealTimeScaling (scaling);
         }
         if (myMenuBarHandler != null) {
            myMenuBarHandler.updateRealTimeWidgets();
         }
      }
   }
   
   /**
    * Queries whether real-time model advancement is enabled.
    * 
    * @return {@code true} if real-time model advancement is enabled
    * @see #setRealTimeAdvance
    */
   public boolean getRealTimeAdvance() {
      return myScheduler.getRealTimeAdvance();
   }
   
   /**
    * Enables or disables real-time model advancement. If enabled,
    * the model simulation will be slowed down (if necessary) so
    * that the simulation does proceed faster than real time.
    * 
    * @param enable if {@code true}, enables real-time model advancement
    */
   public void setRealTimeAdvance (boolean enable) {
      if (enable != myScheduler.getRealTimeAdvance()) {
         myScheduler.setRealTimeAdvance (enable);
         if (myMenuBarHandler != null) {
            myMenuBarHandler.updateRealTimeWidgets();
         }
      }
   }

   public void setFrameRate (double val) {
      if (val < 0) {
         throw new IllegalArgumentException ("frame rate must not be negative");
      }
      if (myScheduler != null) {
         RenderProbe renderProbe = myScheduler.getRenderProbe();
         if (renderProbe != null) {
            renderProbe.setUpdateInterval (
               val == 0 ? Double.POSITIVE_INFINITY : 1/val);
         }
      }
      myFrameRate = val;
   }

   public double getFrameRate() {
      return myFrameRate;
   }

   private AxisAlignedRotation getDefaultAxialView (RootModel root) {
      if (root != null) {
         AxisAngle REW = root.getDefaultViewOrientation();
         if (!REW.equals (new AxisAngle(0, 0, 0, 0))) {
            return AxisAlignedRotation.getNearest (new RotationMatrix3d(REW));
         }
      }
      return myViewerManager.getDefaultAxialView();
   }

   public GLViewerFrame createViewerFrame() {
      GLViewerFrame frame =
         new GLViewerFrame (myViewer, PROJECT_NAME, 400, 400);

      GLViewer viewer = frame.getViewer();
      // ViewerToolBar toolBar = new ViewerToolBar(viewer, this);

      AxisAlignedRotation axialView = getDefaultAxialView(getRootModel());
      myViewerManager.addViewer (viewer);
      ViewerToolBar toolBar = 
         new ViewerToolBar (viewer, /*addGridPanel=*/true);
      frame.getContentPane().add (toolBar, BorderLayout.PAGE_START);
      viewer.setAxialView (axialView);
      initializeViewer (viewer);
      frame.pack();
      frame.setVisible (true);
      return frame;
   }

   public void initializeViewer (GLViewer viewer) {
      // ContextMouseListener contextListener = new ContextMouseListener();

      // keyHandler.attachGLViewer(viewer);

      // ArtiSynth specific key listener:
      viewer.addKeyListener (myKeyHandler);
      // generic key listener for viewer-only stuff:
      viewer.addKeyListener (new ViewerKeyListener(viewer));
      viewer.addSelectionListener (
         mySelectionManager.getViewerSelectionHandler());
      viewer.setSelectionFilter (
         mySelectionManager.getViewerSelectionFilter());

      // set the cursor on the canvas based on the current selection mode
      if (mySelectionMode == SelectionMode.AddComponent) {
         viewer.setScreenCursor (
            Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
      }
      else {
         viewer.setScreenCursor (getDefaultCursor());
      }
      myViewerManager.resetViewer (viewer);
   }

   void centerViewOnSelection () {

      Point3d center = new Point3d ();
      Point3d pmin = new Point3d (inf, inf, inf);
      Point3d pmax = new Point3d (-inf, -inf, -inf);

      int n = 0;
      ModelComponent firstComp = null;

      for (ModelComponent sel : mySelectionManager.getCurrentSelection()) {
         if (sel instanceof Renderable &&
             !ComponentUtils.isAncestorSelected(sel)) {
            if (firstComp == null) {
               firstComp = sel;
            }
            ((Renderable)sel).updateBounds (pmin, pmax);
            n++;
         }
      }
      if (n > 0) {
         if (n == 1 && firstComp instanceof HasCoordinateFrame) {
            RigidTransform3d X = new RigidTransform3d();
            ((HasCoordinateFrame)firstComp).getPose (X);
            center.set (X.p);
         }
         else {
            center.add (pmin, pmax);
            center.scale (0.5);
         }      
         myViewer.setCenter (center);
         rerender();
      }
   }

   void executeJythonInit (Object fileOrUrl) {
      InputStream input = null;
      try {
         if (fileOrUrl instanceof URL) {
            input = ((URL)fileOrUrl).openStream();
         }
         else // assume a file
            input = new FileInputStream ((File)fileOrUrl);
      }
      catch (Exception e) {
         System.out.println ("ERROR: cannot open initialization "+fileOrUrl);
      }
      try {
         System.out.println ("Initializing from "+fileOrUrl+" ...");
         myJythonConsole.execfile (input, fileOrUrl.toString());
      }
      catch (Exception e) {
         System.out.println (
            "Error in "+fileOrUrl+": "+e.getMessage());
      }
   }

   /**
    * Creates a Jython console frame.
    */
   void createJythonConsole (boolean guiBased) {
      String initFileName = "scripts/jythonInit.py";

      if (guiBased) {
         myJythonConsole = ArtisynthJythonConsole.createFrameConsole();
         myJythonFrame = myJythonConsole.getFrame();
      }
      else {
         myJythonConsole = ArtisynthJythonConsole.createTerminalConsole();
      }
      
      myJythonConsole.setMain (this);
      File stdFile = ArtisynthPath.getHomeRelativeFile (initFileName, ".");
      if (!stdFile.canRead()) {
         System.out.println (
            "WARNING: cannot find $ARTISYNTH_HOME/"+initFileName);
         stdFile = null;
      }
      else {
         executeJythonInit (stdFile);
      }
//      File[] files = ArtisynthPath.findFiles (initFileName);
//      if (files != null) {
//         for (int i=files.length-1; i>=0; i--) {
//            if (stdFile == null ||
//               !ArtisynthPath.filesAreTheSame (files[i], stdFile)) {
//               executeJythonInit (files[i]);
//            }
//         }
//      }
   }

   void setJythonFrameVisible (boolean visible) {
      if (visible) {
         boolean created = false;
         if (myJythonFrame == null) {
            createJythonConsole(/*guiBased=*/true);
            created = true;
         }
         myJythonFrame.setVisible(true);
         if (created) {
            GuiUtils.locateBelow (myJythonFrame, myFrame);
         }
      }
      else {
         if (myJythonFrame != null) {
            myJythonFrame.setVisible(false);
         }
      }
      // locate the frame if not previously visible
      if (visible && !myJythonPreviouslyVisible) {
         // set whether timeline is to the right
         RelativeLocation loc = myLayoutPrefs.getJythonLocation();
         if (jythonLocation.value != null) {
            RelativeLocation cmdloc =
               RelativeLocation.fromString (jythonLocation.value);
            if (cmdloc == null) {
               System.out.println (
                  "Unknown Jython location '"+jythonLocation.value+
                  "'; using "+loc);
            }
            else {
               loc = cmdloc;
            }
         }
         setJythonLocation (loc);
         myJythonPreviouslyVisible = true;
      }
   }

   JFrame getJythonFrame() {
      return myJythonFrame;
   }

   public ArtisynthJythonConsole getJythonConsole() {
      return myJythonConsole;
   }

   /**
    * Returns a file handle for any Jythin script file that is currently
    * executing.
    */
   public File getScriptFile() {
      return myJythonConsole.getScriptFile();
   }

   public boolean isSimulating() {
      return getScheduler().isPlaying();
   }

   public void reset() {
      myScheduler.reset();
   }

   public boolean rewind() {
      return myScheduler.rewind();
   }

   public void play() {
      if (!myScheduler.isPlaying()) {
         myScheduler.play();
      }
   }

   public void play(double time) {
      if (!myScheduler.isPlaying()) {
         myScheduler.play(time);
      }
   }

   public void playAndWait(double time) {
      play (time);
      waitForStop();
   }

   public void pause() {
      myScheduler.pause();
   }

   public void waitForStop () {
      while (getScheduler().isPlaying()) {
         try {
            Thread.sleep (10);
         }
         catch (InterruptedException e) {
            break;
         }
      }
   }
   
   public void waitForStart () {
      while (!getScheduler().isPlaying()) {
         try {
            Thread.sleep (10);
         }
         catch (InterruptedException e) {
            break;
         }
      }
   }
   
   public Exception getSimulationException() {
      return myScheduler.getLastException();
   }

   public void step() {
      myScheduler.step();
   }
   
   public void stopAll() {
      if (myJythonConsole != null &&
          myJythonConsole.requestInterrupt()) {
         while (myJythonConsole.interruptRequestPending()) {
            try { Thread.sleep (50);
            }
            catch (InterruptedException e) {
               // ignore
            }
         }
         maskFocusStealing (false);
      }
      myScheduler.pause();
   }      
 
   public boolean forward() {
      return myScheduler.fastForward();
   }

   public double getTime() {
      return myScheduler.getTime();
   }

   public WayPoint addWayPoint (double t) {
      WayPoint way = new WayPoint (t, false);
      getRootModel().addWayPoint (way);
      //getTimeline().addWayPointFromRoot (way);
      return way;
   }

   public WayPoint addBreakPoint (double t) {
      WayPoint brk = new WayPoint (t, true);
      getRootModel().addWayPoint (brk);
      //getTimeline().addWayPointFromRoot (brk);
      return brk;
   }

   public WayPoint getWayPoint (double t) {
      return getRootModel().getWayPoint (t);
   }

   public void setMaxStep (double sec) {
      if (myMaxStep != sec) {
         doSetMaxStep (sec);
         RootModel root = getRootModel();
         if (root != null) {
            root.setMaxStepSize (myMaxStep);
         }
      }
   }

   private void doSetMaxStep (double sec) {
      myMaxStep = sec;
      if (myMenuBarHandler != null) {
         myMenuBarHandler.updateStepDisplay ();
      }
   }

   public double getMaxStep () {
      return myMaxStep;
   }

   public boolean removeWayPoint (WayPoint way) {
      RootModel root = getRootModel();
      if (root != null) {
         return root.removeWayPoint (way);
      }
      else {
         return false;
      }
      // if (getRootModel().removeWayPoint (way)) {
      //    getTimeline().removeWayPointFromRoot (way);
      //    return true;
      // }
      // else {
      //    return false;
      // }
   }

   public boolean removeWayPoint (double t) {
      WayPoint way = getWayPoint (t);
      if (way != null) {
         return removeWayPoint (way);
      }
      else {
         return false;
      }
   }

   public void clearWayPoints () {
      if (getRootModel() != null) {
         getRootModel().removeAllWayPoints();
      }
   }

   public void delay (double sec) {
      try {
         Thread.sleep ((int)(sec*1000));
      }
      catch (InterruptedException e) {
      }  
   }      

   /**
    * Returns the default timeline range value. See {@link
    * #setDefaultTimelineRange}.
    */
   double getDefaultTimelineRange() {
      return myDefaultTimelineRange;
   }

   /**
    * Sets the timeline range value that is used to initialize the visible part
    * of the timeline when a model is loaded. A value <= 0 implies that the
    * range will be set automatically.
    */
   void setDefaultTimelineRange (double sec) {
      myDefaultTimelineRange = sec;
      if (myTimeline != null) {
         myTimeline.setVisibleRange (sec);
      }
   }

   /**
    * Queries the default location where the timeline should be located
    * relative to the main frame.
    */
   RelativeLocation getTimelineLocation() {
      return myTimelineLocation;
   }

   /**
    * Sets the default location where the timeline should be located relative
    * to the main frame.
    */
   void setTimelineLocation (RelativeLocation loc) {
      myTimelineLocation = loc;
      GuiUtils.locateRelative (myTimeline, myFrame, loc);
   }

   /**
    * Queries the default location where the Jython console should be located
    * relative to the main frame.
    */
   RelativeLocation getJythonLocation() {
      return myJythonLocation;
   }

   /**
    * Sets the default location where the Jython console should be located
    * relative to the main frame.
    */
   void setJythonLocation (RelativeLocation loc) {
      myJythonLocation = loc;
      if (myJythonFrame != null) {
         GuiUtils.locateRelative (myJythonFrame, myFrame, loc);
      }
   }

   /**
    * create the timeline
    */
   private void createTimeline() {
      
      if (getScheduler().isPlaying()) {
         getScheduler().stopRequest();
         waitForStop();
      }
      
      TimelineController timeline = new TimelineController ("Timeline", this);
      
      
      mySelectionManager.addSelectionListener(timeline);
      timeline.setMultipleSelectionMask (
         myViewer.getMouseHandler().getMultipleSelectionMask());
      
      myTimeline = timeline;

      // set the timeline size and default range
      int width = myLayoutPrefs.getTimelineWidth();
      if (timelineWidth.value != -1) {
         width = timelineWidth.value;
      }
      int height = myLayoutPrefs.getTimelineHeight();
      if (timelineHeight.value != -1) {
         height = timelineHeight.value;
      }
      myDefaultTimelineRange = myLayoutPrefs.getTimelineRange();
      if (timelineRange.value != -2) { // -2 means "undefined"
         myDefaultTimelineRange = timelineRange.value;
      }
      if (myDefaultTimelineRange <= 0) {
         myDefaultTimelineRange = DEFAULT_TIMELINE_RANGE;
      }
      myTimeline.setSize (width, height);
      myTimeline.setVisibleRange (myDefaultTimelineRange);

      if (getWorkspace() != null) {
         myTimeline.requestResetAll();
      }
   }

   boolean isTimelineVisible() {
      return myTimeline != null && myTimeline.isVisible();
   }

   private void initializeSimulationPrefs () {
      // maximum step size
      double stepSize = mySimulationPrefs.getMaxStepSize();
      if (maxStep.value != -1) {
         stepSize = maxStep.value;
      }
      ModelBase.setDefaultMaxStepSize (stepSize);

      // position stabilization
      PosStabilization stab = mySimulationPrefs.getStabilization();
      if (posCorrection.value != null) {
         PosStabilization s = PosStabilization.fromString (posCorrection.value);
         if (s == null) {
            System.out.println (
               "WARNING: option '-posCorrection': unknown method '"+
               posCorrection.value+"'");
         }
         else {
            stab = s;
         }
      }
      MechSystemBase.setDefaultStabilization (stab);

      // collider type
      ColliderType ctype = mySimulationPrefs.getColliderType();
      if (useAjlCollision.value) {
         ctype = ColliderType.AJL_CONTOUR;
      }
      CollisionManager.setDefaultColliderType (ctype);
      
      // implicit friction
      boolean implicitFriction = mySimulationPrefs.getUseImplicitFriction();
      if (useImplicitFriction.value) {
         implicitFriction = useImplicitFriction.value;
      }
      MechSystemBase.setDefaultUseImplicitFriction (implicitFriction);
      
      // whether or not to about when elements are inverted
      boolean abortOnInversion = mySimulationPrefs.getAbortOnInvertedElements();
      if (abortOnInvertedElems.value) {
         abortOnInversion = true;
      }
      FemModel3d.abortOnInvertedElems = abortOnInversion;

      // hybrid solves
      boolean hybridSolves = mySimulationPrefs.getHybridSolvesEnabled();
      if (disableHybridSolves.value) {
         hybridSolves = false;
      }
      MechSystemSolver.myDefaultHybridSolveP = hybridSolves;

      // number of CPU threads to use for the solver
      int numThreads = mySimulationPrefs.getNumSolverThreads();
      if (numSolverThreads.value != -1) {
         numThreads = numSolverThreads.value;
      }
      PardisoSolver.setDefaultNumThreads(numThreads);
      
      // display ill-conditioned solves
      mySimulationSettings.setShowIllConditionedSolves (
         mySimulationPrefs.getShowIllConditionedSolves());

      // settings that are not in preferences:

      // no-incompress-damping is probably obsolete:
      FemModel3d.noIncompressStiffnessDamping = noIncompressDamping.value;

      // matrix solver
      if (matrixSolver.value != null) {
         MechSystemBase.setDefaultMatrixSolver (
            SparseSolverId.valueOf (matrixSolver.value));
      }
   }      

   private void initializeMaintenancePrefs () {

      // log level
      LogLevel level = myMaintenancePrefs.getLogLevel();
      if (logLevel.value != null) {
         LogLevel lev = LogLevel.find(logLevel.value);
         if (lev == null) {
            System.out.println (
               "WARNING: option '-logLevel': unknown level '"+
               logLevel.value+"'");
         }
         else {
            level = lev;
         }
      }
      setLogLevel (level);

      // test save/restore state
      boolean testSaveRestore = myMaintenancePrefs.getTestSaveRestoreState();
      if (testSaveRestoreState.value) {
         testSaveRestore = true;
      }
      RootModel.setTestSaveRestoreState (testSaveRestore);
   }      

   private void initializeInteractionPrefs () {

      // rendering frame rate
      double frameRate = myInteractionPrefs.getFrameRate();
      if (framesPerSecond.value != -1) {
         frameRate = framesPerSecond.value;
      }
      setFrameRate (frameRate);

      // use of articulated transforms
      boolean articulatedTrans = myInteractionPrefs.getArticulatedTransforms();
      if (useArticulatedTransforms.value) {
         articulatedTrans = true;
      }
      setArticulatedTransformsEnabled (articulatedTrans);
      
      setRealTimeScaling (myInteractionPrefs.getRealTimeScaling());
      setInitDraggersInWorldCoords (
         myInteractionPrefs.getInitDraggersInWorld());
      setAlignDraggersToPoints (
         myInteractionPrefs.getAlignDraggersToPoints());
      myInteractionSettings.setNavigationPanelLines (
         myInteractionPrefs.getNavigationPanelLines());
   }      

   public void start (ArrayList<String> modelArgs) {

      myScheduler = new Scheduler(this);

      initializeSimulationPrefs();
      initializeMaintenancePrefs();

      if (myFrame != null) {

         // is timeline visible?
         boolean isTimelineVisible = myLayoutPrefs.isTimelineVisible();
         if (timelineHidden.value) {
            isTimelineVisible = false;
         }
         else if (timelineVisible.value) {
            isTimelineVisible = true;
         }
         
         // Prevent deadlock by AWT thread
         try {
            SwingUtilities.invokeAndWait( new Runnable() {
               @Override
               public void run() {
                  myFrame.pack();
                  myFrame.setVisible (true);
                  myFrame.setViewerSize (myViewerWidth, myViewerHeight);
                  createTimeline (); //TODO               
               }
            });
         } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
         }

         // create render probe
         RenderProbe renderProbe = new RenderProbe (this, 1/DEFAULT_FRAME_RATE);
         getScheduler().setRenderProbe (renderProbe);

         if (myTimeline instanceof TimelineController) {
            myScheduler.addListener ((TimelineController)myTimeline);
         }
         myScheduler.addListener (myMenuBarHandler);

         if (isTimelineVisible) {
            setTimelineVisible (true);
         }

         // is Jython console visible?
         boolean isJythonVisible = myLayoutPrefs.isJythonFrameVisible();
         if (startWithJython.value) {
            isJythonVisible = true;
         }
         if (isJythonVisible) {
            setJythonFrameVisible (true);
         }

         if (orthographic.value) {
            getViewer().setOrthographicView (true);
         }
         
         initializeInteractionPrefs();
      }
      else {
         myScheduler.setRealTimeAdvance (false); // no need if no viewer
         if (startWithJython.value) {
            createJythonConsole (/*useGui=*/false);
         }
      }

      if (System.getProperty ("os.name").contains ("Windows")) {
         fixPardisoThreadCountHack(); // XXX see function docs
      }
      
      verifyNativeLibraries (updateLibs.value);

      File waypointsFile = null;
      if (wayPointsFile.value != null) {
         waypointsFile = new File(wayPointsFile.value);
         if (!waypointsFile.canRead()) {
            System.out.println (
               "ERROR: waypoints file" + waypointsFile +
               " does not exist or is not readable");
            waypointsFile = null;
         }
      }

      // If there is a model to load at startup, load the model

      ModelScriptInfo modelInfo = null;
      boolean modelSpecified = false;
      if (modelName.value != null) {
         if (modelName.value.equalsIgnoreCase ("null") ||
             modelName.value.equalsIgnoreCase ("none")) {
            // model explicitly not specified. Leave modelInfo = null.
            modelSpecified = true;
         }
         else {
            // First see if name corresponds to a file
            File file = new File (modelName.value);
            if (file.exists() && file.canRead()) {
               modelInfo = new ModelScriptInfo (
                  InfoType.FILE, file.getAbsolutePath(), file.getName(), null);
               modelSpecified = true;
            }
            // otherwise, try to determine the model from the class name or alias
            else {
               String className = getDemoClassName (modelName.value);
               if (className == null) {
                  System.out.println (
                     "No class associated with model name " + modelName.value);
               }
               else {
                  modelInfo = new ModelScriptInfo (
                     InfoType.CLASS, className,
                     RootModelManager.getLeafName (modelName.value),
                     createArgArray(modelArgs));
                  modelSpecified = true;
               }
            }
         }
      }
      // if no model specified, use the startup model, if
      // any, unless a script or taskManager has been specified
      if (!modelSpecified &&
          scriptFile.value == null && taskManagerClassName.value == null) {
         modelInfo = myStartupModel.getModelInfo();
         if (waypointsFile == null) {
            waypointsFile = myStartupModel.getWaypointsFile();
         }
      }
      if (modelInfo != null) {
         // load the model
         if (!loadModel (modelInfo)) {
            if (myFrame != null) {
               GuiUtils.showError (myFrame, myMain.getErrorMessage());
            }
            else {
               System.out.println (
                  "Error loading model "+modelInfo.getClassNameOrFile());
            }
            modelInfo = null;
         }
      }
      if (modelInfo == null) {
         setRootModel (new RootModel(), null);
      }
      else {
         if (waypointsFile != null) {
            try {
               loadWayPoints (waypointsFile);
            }
            catch (IOException e) {
               String errMsg =
                  "Error loading waypoints "+waypointsFile+"\n"+e.getMessage();
               if (myFrame != null) {
                  GuiUtils.showError (myFrame, errMsg);
               }
               else {
                  System.out.println (errMsg);
               }
            }
         }
      }
   }

   public RootModel getRootModel() {
      if (myWorkspace != null) {
         return myWorkspace.getRootModel();
      }
      else {
         return null;
      }
   }

   public void clearRootModel() {
      if (myWorkspace != null) {
         myWorkspace.cancelRenderRequests();
      }
      myModelName = null;
      myLastLoadInfo = null;
      mySaveWayPointData = false;
      mySaveCoreOnly = false;
      RootModel rootModel = getRootModel();
      if (rootModel != null) {
         rootModel.detach (this);
         rootModel.removeComponentChangeListener (this);
         rootModel.setMainViewer (null); // just for completeness
         rootModel.removeController (myPullController); // 
         rootModel.dispose();
      }
      if (myAddMarkerHandler != null) {
         myAddMarkerHandler.setDefaultHandler ();  // reset handler to default
      }
      if (mySelectionManager != null) {
         mySelectionManager.clearSelections();
      }
      if (myUndoManager != null) {
         myUndoManager.clearCommands();
      }
      if (myWorkspace !=null) {
         myWorkspace.removeDisposables();
      }
      ArtisynthPath.setWorkingFolder (null);
      // myWorkspace.getWayPoints().clear();
      // myWorkspace.clearInputProbes();
      // myWorkspace.clearOutputProbes();
      if (myViewerManager != null) {
         myViewerManager.clearRenderables();
      }
      if (getWorkspace() != null) {
         getWorkspace().setRootModel (null);
      }
      if (myMenuBarHandler != null) {
         myMenuBarHandler.clearModelInfoFrame();
      }
      
      // Sanchez, July 11, 2013
      // Remove model and force repaint to clean the display.  
      // This is done so we can render
      // objects like an HUD while a new model is loading
      if (myViewerManager != null) {
         myViewerManager.clearRenderables();
         myViewerManager.render();         // refresh the rendering lists
      }
      myDraggerFrameInfoMap.clear();
      
      // free up as much space as possible before loading next model
      System.gc();
      System.runFinalization();
   }

   /**
    * Gets the data associated with a numeric input probe and returns it as a
    * 2-dimensional array of doubles. The identity of the probe is indicated by
    * a string that gives either it's name or number within the current root
    * model's list of input probes. The method returns null if the specified
    * probe cannot be found or if it is not a numeric probe.
    *
    * <p> This is primarily intended as a convenience method for extracting
    * probe data into other applications (such as Matlab).
    *
    * @param nameOrNumber name or number of the probe in question
    * @return probe's current data, or <code>null</code> if the probe is not
    * found
    */
   public double[][] getInputProbeData (String nameOrNumber) {
      RootModel root = getRootModel();
      if (root != null) {
         Probe probe = root.getInputProbes().findComponent (nameOrNumber);
         if (probe instanceof NumericProbeBase) {
            return ((NumericProbeBase)probe).getValues();
         }
      }
      return null;
   }
    
   /**
    * Sets the data associated with a numeric input probe. The data is supplied
    * as a 2-dimensional array of double, and the identity of the probe is
    * indicated by a string that gives either it's name or number within the
    * current root model's list of input probes. The method returns false if the
    * specified probe cannot be found or if it is not a numeric probe.
    *
    * <p> This is primarily intended as a convenience method for setting
    * probe data from other applications (such as Matlab).
    *
    * @param nameOrNumber name or number of the probe in question
    * @param data new data to set inside the probe
    * @return <code>false</code> if the probe is not found
    */
   public boolean setInputProbeData (String nameOrNumber, double[][] data) {
      RootModel root = getRootModel();
      if (root != null) {
         Probe probe = root.getInputProbes().findComponent (nameOrNumber);
         if (probe instanceof NumericProbeBase) {
            ((NumericProbeBase)probe).setValues(data);
            return true;
         }
      }
      return false;
   }
   
   /**
    * Gets the data associated with a numeric output probe and returns it as a
    * 2-dimensional array of doubles. The identity of the probe is indicated by
    * a string that gives either it's name or number within the current root
    * model's list of output probes. The method returns null if the specified
    * probe cannot be found or if it is not a numeric probe.
    *
    * <p> This is primarily intended as a convenience method for extracting
    * probe data into other applications (such as Matlab).
    *
    * @param nameOrNumber name or number of the probe in question
    * @return probe's current data, or <code>null</code> if the probe is not
    * found
    */
   public double[][] getOutputProbeData (String nameOrNumber) {
      RootModel root = getRootModel();
      if (root != null) {
         Probe probe = root.getOutputProbes().findComponent (nameOrNumber);
         if (probe instanceof NumericProbeBase) {
            return ((NumericProbeBase)probe).getValues();
         }
      }
      return null;
   }
    
   /**
    * Sets the data associated with a numeric output probe. The data is supplied
    * as a 2-dimensional array of double, and the identity of the probe is
    * indicated by a string that gives either it's name or number within the
    * current root model's list of output probes. The method returns false if the
    * specified probe cannot be found or if it is not a numeric probe.
    *
    * <p> This is primarily intended as a convenience method for setting
    * probe data from other applications (such as Matlab).
    *
    * @param nameOrNumber name or number of the probe in question
    * @param data new data to set inside the probe
    * @return <code>false</code> if the probe is not found
    */
   public boolean setOutputProbeData (String nameOrNumber, double[][] data) {
      RootModel root = getRootModel();
      if (root != null) {
         Probe probe = root.getOutputProbes().findComponent (nameOrNumber);
         if (probe instanceof NumericProbeBase) {
            ((NumericProbeBase)probe).setValues(data);
            return true;
         }
      }
      return false;
   }
   
   /** 
    * Returns the first MechSystem, if any, among the models of the current
    * RootModel.
    */
   private Model findMechSystem (RootModel root) {
      for (Model m : root.models()) {
         if (m instanceof MechSystem) {
            return m;
         }
      }
      return null;
   }

   private void updateApplicationMenuPresent (RootModel root) {
      myMenuBarHandler.setApplicationMenuEnabled (
            myMenuBarHandler.getApplicationMenuItems (root) != null);
   }

   public void setRootModel (
      RootModel newRoot, String modelName) {
      
      if (modelName == null) {
         modelName = "Null model";
      }
      myModelName = modelName;
      //CompositeUtils.testPaths (newRoot);

      double maxStepSize = newRoot.getMaxStepSize();
      if (maxStepSize == -1) {
         // if RootModel maxStepSize is undefined, set it to the default value
         maxStepSize = ModelBase.getDefaultMaxStepSize();
         newRoot.setMaxStepSize (maxStepSize);
      }
      doSetMaxStep (maxStepSize);

      // need to explicitly set number since root model doesn't have a parent
      newRoot.setNumber (0);
      newRoot.setFocusable (!myFocusStealingMaskedP);
      getWorkspace().setRootModel (newRoot);
      // mainViewer should already be set if constructed with build() method:
      newRoot.setMainViewer (myViewer); 
      if (myViewer != null) {
         myViewer.clearClipPlanes();
      }

      if (myFrame != null) {
         myMenuBarHandler.enableShowPlay();

         if (mySelectionMode == SelectionMode.Pull) {
            // add pull controller here, before nav panel is update            
            myPullController.clear();
            myPullController.setRootModelDefaults (newRoot);
            newRoot.addController (
               myPullController, findMechSystem(newRoot));
         }

         if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater (new Runnable() {
                  public void run() {
                     myFrame.updateNavBar();
                     myMenuBarHandler.clearClipPlaneControls ();
                  }
               });
         }
         else {
            myFrame.updateNavBar();
            myMenuBarHandler.clearClipPlaneControls ();
         }

         // reset all the viewers
         myViewerManager.clearRenderables();
         
         AxisAlignedRotation axialView = getDefaultAxialView(newRoot);
         myViewerManager.resetViewers (axialView);

         // remove the selected item in the selectComponentPanel otherwise the
         // text for that selected component remains when a new model is loaded
         myFrame.getSelectCompPanelHandler().clear();
         myFrame.getSelectCompPanelHandler().setComponentFilter (null);
         myFrame.setBaseTitle ("ArtiSynth " + modelName);
      }

      // model scheduler initialization called within initialize
      getScheduler().initialize(newRoot.getStartTime());
      if (myMovieMaker != null) {
         myMovieMaker.updateForNewRootModel (modelName, newRoot);
      }

      // reset the timeline.
      if (myTimeline != null) {
         myTimeline.requestResetAll();
      }

      if (myFrame != null) {
         getWorkspace().setViewerManager (myViewerManager);
      }

      if (myFrame != null) {
         updateApplicationMenuPresent (newRoot);

         // notify the frame root model is loaded
         myFrame.notifyRootModelLoaded();
         myWorkspace.rerender();
         
         myTimeline.setVisibleRange (myDefaultTimelineRange);
      }
      //
      // Add this now since we don't want Main.componentChanged() being called
      // while the model is being built. 
      // 
      newRoot.addComponentChangeListener (this);

      // Component change listener is now active, and so will be called in
      // response to any change events that are dispatched within attach().
      newRoot.attach (this);
      
      // If the root model has a task manager, start it
      if (newRoot.getTaskManager() != null) {
         TaskManager tm = newRoot.getTaskManager();
         tm.setMain (this);
         tm.start();
      }
   }

   private class LoadModelRunner implements Runnable {
      ModelScriptInfo myModelInfo;
      boolean mySaveToHistory;
      boolean myStatus = false;

      LoadModelRunner (ModelScriptInfo modelInfo, boolean saveToHistory) {
         myModelInfo = modelInfo;
         mySaveToHistory = saveToHistory;
      }

      public void run() {
         myStatus = loadModel (myModelInfo, mySaveToHistory);
      }

      public boolean getStatus() {
         return myStatus;
      }
   }

   private boolean runInSwing (Runnable runnable) {
      try {
         SwingUtilities.invokeAndWait (runnable);
      }
      catch (InvocationTargetException ex) {
         if (ex.getCause() != null) {
            ex.getCause().printStackTrace();
         }
         else {
            ex.printStackTrace();
         }
         return false;
      }
      catch (InterruptedException e) {
         // assume this won't happen?
      }  
      return true;
   }

   protected RootModel createRootModel (
      Class<?> demoClass, String modelName, String[] args) {

      // reset in case previous model set this to false:
      ModelComponentBase.enforceUniqueNames = true;
      if (args == null) {
         args = new String[0];
      }
      Throwable err = null;
      try {
         RootModel newRoot = null;
         Method method = demoClass.getMethod ("build", String[].class);
         if (demoClass == RootModel.class ||
             method.getDeclaringClass() != RootModel.class) {
            Constructor<?> constructor = demoClass.getConstructor();
            newRoot = (RootModel)constructor.newInstance();
            newRoot.setName (modelName);
            newRoot.setMainViewer (myViewer); 
            newRoot.build (args);
         }
         else {
            System.out.println (
               "constructing model with legacy constructor method ...");
            Constructor<?> constructor = 
               demoClass.getConstructor (String.class);
            newRoot = (RootModel)constructor.newInstance (modelName);
         }
         // make sure all referenced components are part of the hierarchy
         ComponentUtils.checkReferenceContainment (newRoot, newRoot);
         return newRoot;
      }
      catch (Exception e) {
         err = e;
      }
      catch (Error e) {
         err = e;
      }
      if (err != null) {
         myErrMsg = 
            "Model class "+demoClass.getName()+" cannot be instantiated";
         if (err.getMessage() != null) {
            myErrMsg += ": \n" + err.getMessage();
         }
         err.printStackTrace();
      }
      return null;
   }
   
   // Load model from a file. Used by script commands.
   public boolean loadModelFile (File file) {
      ModelScriptInfo modelInfo = new ModelScriptInfo (
         InfoType.FILE, file.getAbsolutePath(), file.getName(), null);
      return loadModel (modelInfo, /*saveToHistory=*/false);
   }

   // Load model from a class. Used by script commands.
   public boolean loadModel(
      String className, String modelName, String[] modelArgs) {

      String shortName = className;
      int rdotIdx = shortName.lastIndexOf('.');
      if (rdotIdx != -1) {
         shortName = shortName.substring (rdotIdx+1);
      }
      if (modelName.equals (shortName)) {
         shortName = modelName;
      }
      ModelScriptInfo modelInfo = 
         new ModelScriptInfo(InfoType.CLASS, className, shortName, modelArgs);
      return loadModel (modelInfo, /*saveToHistory=*/false);
   }

   
   public boolean loadModel (ModelScriptInfo info) {
      return loadModel (info, /*saveToHistory=*/true);
   }
   
   // Entry-point for loading models.
   public boolean loadModel (ModelScriptInfo info, boolean saveToHistory) {

      // stop any existing model execution      
      myScheduler.pause();
      
      // If we are not in the AWT event thread, switch to that thread
      // and load the model there. We do this because the model building
      // code may create and access GUI components (such as ControlPanels)
      // and any such code must execute in the event thread. Typically,
      // we will not be in the event thread if loadModel is called from
      // the Jython console.
      if (myViewer != null && !SwingUtilities.isEventDispatchThread()) {
         LoadModelRunner runner = new LoadModelRunner (info, saveToHistory);
         if (!runInSwing (runner)) {
            return false;
         }
         else {
            return runner.getStatus();
         }
      }
      
      boolean success = false;
      clearErrorMessage();
      
      if (info.getType() == InfoType.FILE) {
         try {
            success = loadModelFromFile (new File(info.getClassNameOrFile()));
         }
         catch (IOException e) {
            myErrMsg = "Model file can't be loaded";
            if (e.getMessage() != null) {
               myErrMsg += ":\n" + e.getMessage();
            }
            e.printStackTrace();
            return false;
         }
      }
      else {
         success = loadModelFromClass (
            info.getClassNameOrFile(), info.getShortName(), info.getArgs());
      }
      
      if (success) {
         if (saveToHistory && myModelScriptHistory != null) {
            myModelScriptHistory.update (
               info, new Date(System.currentTimeMillis()));
            myModelScriptHistory.save ();
         }
         myLastLoadInfo = info;
      }
      else if (getRootModel() == null) {
         // set an empty root model
         setRootModel (new RootModel(), null);
      }
      
      return success;
   }
   
   private boolean loadModelFromFile (File file) throws IOException {

      // instantiate the root model described in the file
      RootModel newRoot = null;
      ReaderTokenizer rtok = ArtisynthIO.newReaderTokenizer (file);
      if (rtok.nextToken() == ReaderTokenizer.TT_WORD) {
         try {
            newRoot = (RootModel)ClassAliases.newInstance ( 
               rtok.sval, RootModel.class);
            myModelFile = file;
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
         if (newRoot == null) {
            throw new IOException ("cannot create instance of " + rtok.sval);
         }
      }
      else {
         rtok.pushBack();
         newRoot = new RootModel();
      }

      // clear existing root model. Do this before creating the new model
      // to reduce memory stress in case both models are large.
      clearRootModel();

      // scan the model in from the file
      long t0 = System.nanoTime();
      ScanWriteUtils.scanfull (rtok, newRoot, newRoot);
      long t1 = System.nanoTime();
      System.out.println (
         "Scanned "+file.length()+" byte file in "+((t1-t0)*1e-9) + " sec");
      rtok.close();
      // allow the model to reinitialize references, etc.
      newRoot.postscanInitialize();

      String modelName = file.getName();
      setRootModel (newRoot, modelName);
      return true;
   }
   
   private boolean loadModelFromClass (
      String className, String modelName, String[] modelArgs) {

      Class<?> demoClass = null;
      RootModel newRoot = null;
      try {
         demoClass = ClassFinder.forName (className, false);
      }
      catch (ClassNotFoundException e) {
         myErrMsg = "class " + className + " not found";
         e.printStackTrace();
         return false;
      }
      catch (Exception e) {
         if (className==null) {
            myErrMsg = "Model " + modelName + " cannot be initialized";
         } else {
            myErrMsg = "Model class " + className + " cannot be initialized";
         }
         e.printStackTrace();
         return false;
      }

      // if the model name equals the class name, adjust the model name
      // to remove dots since they aren't allowed in names
      if (className.equals (modelName)) {
         modelName = demoClass.getSimpleName();
      }
      else if (modelName.contains(".")) {
         String[] splitNames = modelName.split("\\.");
         modelName = splitNames[splitNames.length-1];  // get last item
      }
      
      // clear existing root model. Do this before creating the new model
      // to reduce memory stress in case both models are large.
      clearRootModel();

      newRoot = createRootModel (demoClass, modelName, modelArgs);
      if (newRoot == null) {
         return false;
      }
      try {
         setRootModel (newRoot, modelName);
      }
      catch (Exception e) {
         myErrMsg = 
            "Model class "+demoClass.getName()+" cannot be instantiated";
         myErrMsg += ": \n" + e.toString();
         e.printStackTrace();
         return false;
      }
      return true;
   }

   public ViewerManager getViewerManager() {
      return myViewerManager;
   }

   public ArrayList<MouseBindings> getAllMouseBindings() {
      ArrayList<MouseBindings> allBindings = new ArrayList<MouseBindings>();
      allBindings.add (MouseBindings.ThreeButton);
      allBindings.add (MouseBindings.TwoButton);
      allBindings.add (MouseBindings.OneButton);
      allBindings.add (MouseBindings.Laptop);
      allBindings.add (MouseBindings.Mac);
      allBindings.add (MouseBindings.Kees);
      return allBindings;
   }

   /**
    * set the mouse bindings
    * 
    * @param bindingsName name of the preferred mouse bindings
    */
   public void setMouseBindings (String bindingsName) {

      getLogger().info ("Attempting to set mouse bindings to '"+bindingsName+"'");
      MouseBindings bindings = null;
      if (bindingsName.equalsIgnoreCase("Default")) {
         bindings = MouseBindings.Default;
      }
      else {
         ArrayList<MouseBindings> allBindings = getAllMouseBindings();
         for (int i=0; i<allBindings.size(); i++) {
            if (bindingsName.equalsIgnoreCase(allBindings.get(i).getName())) {
               bindings = allBindings.get(i);
               break;
            }
         }
      }
      if (bindings == null) {
         System.out.println ("unknown mouse bindings: " + bindingsName);
      }
      else {
         setMouseBindings (bindings);
      }
   }

   public void setMouseBindings (MouseBindings bindings) {
      if (!bindings.equals (myViewerManager.getMouseBindings())) {
         myViewerManager.setMouseBindings (bindings);
         if (myMenuBarHandler != null) {
            myMenuBarHandler.maybeUpdateMouseSettingsDialog();
         }
      }
   }

   public MouseBindings getMouseBindings () {
      return myViewerManager.getMouseBindings ();
   }

   public MouseBindings getEffectiveMouseBindings () {
      return myViewerManager.getEffectiveMouseBindings ();
   }

   public double getMouseWheelZoomScale() {
      return myViewerManager.getMouseWheelZoomScale();
   }

   public void setMouseWheelZoomScale (double scale) {
      if (scale != myViewerManager.getMouseWheelZoomScale()) {
         myViewerManager.setMouseWheelZoomScale (scale);
         if (myMenuBarHandler != null) {
            myMenuBarHandler.maybeUpdateMouseSettingsDialog();
         }
      }
   }

   /**
    * rerender all viewers and update all widgets
    */
   public void rerender() {
      if (myFrame != null && myWorkspace != null) {
         myWorkspace.rerender();
      }
   }

   /**
    * update all widgets
    */
   public void rewidgetUpdate() {
      if (myWorkspace != null) {
         myWorkspace.rewidgetUpdate();
      }
   }

   protected static IntHolder viewerWidth = new IntHolder(-1);
   protected static IntHolder viewerHeight = new IntHolder(-1);
   protected static IntHolder screenLocX = new IntHolder(-1);
   protected static IntHolder screenLocY = new IntHolder(-1);
   protected static BooleanHolder printHelp = new BooleanHolder (false);
   protected static BooleanHolder yup = new BooleanHolder (false);
   protected static BooleanHolder drawAxes = new BooleanHolder (false);
   protected static BooleanHolder drawGrid = new BooleanHolder (false);
   protected static StringHolder axialView = new StringHolder();
   protected static BooleanHolder orthographic = new BooleanHolder (false);
   protected static BooleanHolder timelineHidden = new BooleanHolder (false);
   protected static BooleanHolder timelineVisible = new BooleanHolder (false);
   protected static BooleanHolder startWithJython = new BooleanHolder (false);
   protected static StringHolder jythonLocation = new StringHolder();
   protected static StringHolder timelineLocation = new StringHolder();
   protected static IntHolder timelineWidth = new IntHolder (-1);
   protected static IntHolder timelineHeight = new IntHolder (-1);
   protected static BooleanHolder printOptions = new BooleanHolder (false);
   protected static IntHolder zoom = new IntHolder (-1);
   protected static DoubleHolder timelineRange = new DoubleHolder (-2);
   protected static DoubleHolder axisLength = new DoubleHolder (-1);
   protected static DoubleHolder framesPerSecond = new DoubleHolder (-1);
   protected static DoubleHolder maxStep = new DoubleHolder (-1);
   protected static StringHolder modelName = new StringHolder();
   protected static BooleanHolder play = new BooleanHolder();
   protected static DoubleHolder playFor = new DoubleHolder();
   protected static BooleanHolder exitOnBreak = new BooleanHolder();
   protected static BooleanHolder updateLibs = new BooleanHolder();
   protected static StringHolder demoFilename = new StringHolder();
   protected static StringHolder modelMenuFilename = new StringHolder();
   protected static StringHolder scriptMenuFilename = new StringHolder();
   protected static StringHolder historyFilename = new StringHolder();
   protected static StringHolder configFolder = new StringHolder();
   protected static StringHolder scriptsFilename =
      new StringHolder (".artisynthScripts");
   protected static StringHolder scriptFile = 
         new StringHolder(); 
   protected static StringHolder wayPointsFile = 
         new StringHolder(); 
   protected static StringHolder taskManagerClassName = 
         new StringHolder(); 

   protected static BooleanHolder abortOnInvertedElems =
      new BooleanHolder (false);
   protected static BooleanHolder disableHybridSolves =
      new BooleanHolder (false);
   protected static IntHolder numSolverThreads =
      new IntHolder (-1);
   protected static StringHolder posCorrection = new StringHolder ();

   protected static BooleanHolder noIncompressDamping =
      new BooleanHolder (false);
   // protected static BooleanHolder useOldTimeline = 
   //    new BooleanHolder (false);
   protected static BooleanHolder useAjlCollision =
      new BooleanHolder (false);
   protected static BooleanHolder useImplicitFriction =
      new BooleanHolder (false);
   protected static BooleanHolder useArticulatedTransforms =
      new BooleanHolder (false);
   protected static BooleanHolder noGui = new BooleanHolder (false);
   protected static IntHolder glVersion = new IntHolder (-1);
   protected static StringHolder graphicsInterface = new StringHolder("");
   protected static StringHolder lookAndFeel = new StringHolder("");
   protected static BooleanHolder useGLJPanel = new BooleanHolder (true);
   protected static StringHolder logLevel = 
      new StringHolder(null);
   protected static BooleanHolder testSaveRestoreState =
      new BooleanHolder (false);

   protected static DoubleHolder movieFrameRate = new DoubleHolder (-1);
   protected static StringHolder movieMethod = new StringHolder ();

   protected static IntHolder flags = new IntHolder();

   protected static StringHolder mousePrefs = new StringHolder(); // "kees"

   protected static float[] bgColor = new float[] { -1f, -1f, -1f};       
   protected static BooleanHolder openMatlab = new BooleanHolder(false);

   protected static StringHolder matrixSolver = new StringHolder();
   protected static StringHolder testModelLoading = new StringHolder();

   // Dimension getViewerSize() {
   //    if (myViewer != null) {
   //       return myViewer.getCanvas().getSize();
   //    }
   //    else {
   //       return null;
   //    }
   // }

   private void verifyNativeLibraries (boolean update) {

      LibraryInstaller installer = new LibraryInstaller();
      File libFile = ArtisynthPath.getHomeRelativeFile ("lib/LIBRARIES", ".");
      if (!libFile.canRead()) {
         // error will have been printed by launcher, so no need to do so here
         libFile = null;
      }
      else {
         try {
            installer.readLibs (libFile);
         }
         catch (Exception e) {
            // error will have been printed by launcher, so no need to do so here
            libFile = null;
         }
      }
      if (libFile != null) {
         int status = 0;
         try {
            status = installer.verifyNativeLibs (update);
         }
         catch (Exception e) {
            if (installer.isConnectionException (e)) {
               System.out.println (e.getMessage());
            }
            else {
               e.printStackTrace(); 
            }
            System.exit(1);
         }
         if (status == -1) {
            System.out.println (
               "ERROR: can't find or install all required native libraries");
            System.exit(1); 
         }
      }
   }

   private static class QuitOnBreakListener implements SchedulerListener {

      private Main myMain = null;

      public QuitOnBreakListener (Main main) {
         myMain = main;
      }

      public void schedulerActionPerformed (Scheduler.Action action) {
         if (action == Action.Stopped) {
            myMain.quit();
         }
      }
   }

   /**
    * On Windows, we have sometimes seen that Pardiso getNumThreads() needs to
    * be called early, or otherwise the maximum number of threads returned by
    * mkl_get_max_threads() becomes fixed at 1. In particular, we seem to have
    * to do this before models are loaded.
    */
   private static void fixPardisoThreadCountHack() {
      PardisoSolver solver = new PardisoSolver();
      solver.getNumThreads();
      solver.dispose();
   }

   /**
    * Checks the supplied program arguments in <code>pargs</code> starting
    * at the supplied index to see if what follows is an argument list
    * delimited by '[' and ']'. If lone square brackets are desired as
    * actual arguments, they must be escaped with a back-slash (i.e. "\[" 
    * and "\]").  If an argument contains only backslashes and is terminated
    * by a square bracket, then one of the backslashes is removed (i.e. 
    * "\\[" becomes "\[", "\\\[" becomes "\\[", etc...).  
    * 
    * @param pargs program arguments
    * @param idx starting index
    * @param argList list of arguments to populate if found
    * @return next index to process, equal to <code>idx</code> if no arguments are found
    */
   private static int maybeCollectArgs (
      String[] pargs, int idx, List<String> argList) {
      
      // no more following arguments
      if (idx >= pargs.length) {
         return idx;
      }
      
      String arg = pargs[idx].trim();
      if ("[".equals(arg)) {
         while (++idx < pargs.length) {
            arg = pargs[idx].trim();
            if ("]".equals(arg)) {
               return idx+1;
            } else if (arg.endsWith("]") || arg.endsWith("[")) {
               boolean allbs = true;
               for (int i=arg.length()-1; i-->0; ) {
                  if (arg.charAt(i) != '\\') {
                     allbs = false;
                  }
               }
               if (allbs) {
                  arg = arg.substring(1);
               }
            }
            argList.add(arg);
         }
         
         // never found closing bracket
         throw new RuntimeException("Argument list not closed");
      }
      return idx;
   }

   private void setClassAliases() {
      ClassAliases.addPackageAliases (
         "artisynth.core", ".*", ModelComponent.class);

      ClassAliases.addAlias (
         "Tet", "artisynth.core.femmodels.TetElement");
      ClassAliases.addAlias (
         "Hex", "artisynth.core.femmodels.HexElement");
      ClassAliases.addAlias (
         "Wedge", "artisynth.core.femmodels.WedgeElement");
      ClassAliases.addAlias (
         "Pyramid", "artisynth.core.femmodels.PyramidElement");
      ClassAliases.addAlias (
         "Quadtet", "artisynth.core.femmodels.QuadtetElement");
      ClassAliases.addAlias (
         "Quadhex", "artisynth.core.femmodels.QuadhexElement");
      ClassAliases.addAlias (
         "Quadwedge", "artisynth.core.femmodels.QuadwedgeElement");
      ClassAliases.addAlias (
         "Quadpyramid", "artisynth.core.femmodels.QuadpyramidElement");

      ClassAliases.addAlias ("VectorGrid", "maspack.geometry.VectorGrid");     
   }
   
   /**
    * the main entry point
    * 
    * @param args command line arguments
    */
   public static void main (String[] args) {
      
      // some interfaces (like Matlab) may pass args in as null
      if (args == null) {
         args = new String[0];
      }
      
      ArgParser parser = new ArgParser ("java artisynth.core.driver.Main", false);
      parser.addOption ("-help %v #prints help message", printHelp);
      parser.addOption ("-width %d #viewer width (pixels)", viewerWidth);
      parser.addOption ("-height %d #viewer height (pixels)", viewerHeight);
      parser.addOption ("-screenx %d #screen x location (pixels)", screenLocX);
      parser.addOption ("-screeny %d #screen y location (pixels)", screenLocY);
      parser.addOption (
         "-bgColor %fX3 #background color (3 rgb values, 0 to 1)", bgColor);
      parser.addOption (
         "-maxStep %f{(0,1e100]} #maximum time for a single step (sec)", maxStep);
      parser.addOption ("-drawAxes %v #draw coordinate axes", drawAxes);
      parser.addOption ("-drawGrid %v #draw grid", drawGrid);
      parser.addOption ("-axisLength %f #coordinate axis length", axisLength);
      parser.addOption ("-play %v #play model immediately", play);
      parser.addOption (
         "-playFor %f #play model immediately for x seconds", playFor);
      parser.addOption (
         "-exitOnBreak %v #exit artisynth when playing stops", exitOnBreak);
       parser.addOption (
          "-demoFile %s #demo menu file (e.g. demoModels.txt)", demoFilename);
      parser.addOption(
         "-modelMenu %s #model menu file (e.g. modelMenu.xml)",
         modelMenuFilename);
      parser.addOption(
         "-scriptMenu %s #script menu file (e.g. modelMenu.xml)",
         scriptMenuFilename);
      parser.addOption(
         "-historyFile %s #model history file (e.g. .history)", historyFilename);
      parser.addOption (
         "-scriptsFile %s #script file (e.g. .artisynthModels)",scriptsFilename);
      parser.addOption (
         "-configFolder %s #folder for configuration info", configFolder);
      parser.addOption (
         "-mousePrefs %s #kees for pure mouse controls", mousePrefs);
      parser.addOption ("-ortho %v #use orthographic viewing", orthographic);
      parser.addOption (
         "-axialView %s{xy,xz} #initial view of x-y or x-z axes", axialView);
      parser.addOption (
         "-timelineHidden %v #hide the timeline at startup", timelineHidden);
      parser.addOption (
         "-timelineVisible %v #show the timeline at startup", timelineVisible);
      parser.addOption (
         "-showJythonConsole %v{true} #create jython console on startup",
         startWithJython);
      parser.addOption (
         "-jythonLocation %s{CENTER,LEFT,RIGHT,ABOVE,BELOW} "+
         "#initial location of the Jython console",
         jythonLocation);
      parser.addOption (
         "-timelineWidth %d #width of the timeline", timelineWidth);
      parser.addOption (
         "-timelineHeight %d #height of the timeline", timelineHeight);
      parser.addOption (
         "-timelineLocation %s{CENTER,LEFT,RIGHT,ABOVE,BELOW} "+
         "#initial location of the timeline",
         timelineLocation);
      parser.addOption ("-fps %f{[0,1e100]}#frames per second", framesPerSecond);
      // parser.addOption ("-fullscreen %v #full screen renderer", fullScreen);
      // parser.addOption("-yup %v #initialize viewer with Y-axis up", yup);
      parser.addOption (
         "-timelineZoom %d #zoom level for timeline (no longer supported)", zoom);
      parser.addOption (
         "-timelineRange %f{[-1,1e100]} #initial time range for the timeline",
         timelineRange);
      parser.addOption ("-options %v #print options only", printOptions);

      parser.addOption (
         "-abortOnInvertedElems %v #abort on nonlinear element inversion",
         abortOnInvertedElems);
      parser.addOption (
         "-disableHybridSolves %v #disable hybrid linear solves",
         disableHybridSolves);
      parser.addOption (
         "-matrixSolver %s{Pardiso,Umfpack} #default matrix solver",
         matrixSolver);
      parser.addOption (
         "-numSolverThreads %d #number of threads to use for linear solver",
         numSolverThreads);
      parser.addOption (
         "-posCorrection %s{GlobalMass,GlobalStiffness} "+
            "#position correction mode",
            posCorrection);

      parser.addOption (
         "-noIncompressDamping %v #ignore incompress stiffness for damping",
         noIncompressDamping);
      // parser.addOption ("-useOldTimeline %v #use old timeline", useOldTimeline);
      parser.addOption ("-useAjlCollision" +
         "%v #use AJL collision detection", useAjlCollision);
      parser.addOption ("-useImplicitFriction" +
         "%v #whether to use implicit friction with implicit integrators",
         useImplicitFriction);
      parser.addOption (
         "-useArticulatedTransforms %v #enforce articulation " +
            "constraints with transformers", useArticulatedTransforms);
      parser.addOption (
         "-updateLibs %v #update libraries from ArtiSynth server", updateLibs);
      parser.addOption ("-noGui %v #run ArtiSynth without the GUI", noGui);
      parser.addOption (
         "-openMatlabConnection %v " +
         "#open a MATLAB connection if possible", openMatlab);
      parser.addOption (
         "-GLVersion %d{2,3} " +
         "#version of openGL for graphics (replaced with \n" +
         "                        '-graphics {GL2,GL3}')",
         glVersion);
      parser.addOption (
         "-graphics %s{GL2,GL3} " + 
         "#graphics interface for rendering (e.g., GL3)", graphicsInterface);
      parser.addOption (
         "-lookAndFeel %s{METAL,SYSTEM,DEFAULT} " +
         "#look and feel for the UI (e.g., METAL, SYSTEM)", lookAndFeel);
      parser.addOption (
         "-useGLJPanel %v " +
         "#use GLJPanel for creating the openGL viewer", useGLJPanel);
      parser.addOption (
         "-useGLCanvas %v{false} " +
         "#use GLJCanvas for creating the openGL viewer", useGLJPanel);
      parser.addOption("-logLevel %s", logLevel);
      parser.addOption (
         "-testSaveRestoreState %v #test save/restore state when running models",
         testSaveRestoreState);

      parser.addOption (
         "-movieFrameRate %f{(0,1e100)} #frame rate to use when making movies",
         movieFrameRate);
      parser.addOption (
         "-movieMethod %s #method to use when making movies",
         movieMethod);
      parser.addOption (
         "-waypoints %s # specifies a waypoints file to load",
         wayPointsFile);
      parser.addOption (
         "-testModelLoading %s " +
         "#test loading of all models in the given package", testModelLoading);
      
      Locale.setDefault(Locale.CANADA);

      URL initUrl = ArtisynthPath.findResource (".artisynthInit");
      if (initUrl == null) {
         // Removed warning message for now
         //System.out.println (".artisynthInit not found");
      }
      else {
         InputStream is = null;
         try {
            is = initUrl.openStream();
         }
         catch (Exception e) { // do nothing if we can't open
            System.out.println (".artisynthInit not found");
         }
         if (is != null) {
            try {
               args = ArgParser.prependArgs (new InputStreamReader (is), args);
            }
            catch (Exception e) {
               System.err.println ("Error reading init file " + initUrl);
               System.err.println ("  "+e);
            }
         }
      }
      if (System.getProperty ("file.separator").equals ("\\")) {
         // then we are running windows, so set noerasebackground to
         // try and remove flicker bug
         System.setProperty ("sun.awt.noerasebackground", "true");
      }
      else {
         System.setProperty("awt.useSystemAAFontSettings","on"); 
      }

      // Separate program arguments from model arguments introduced by -M
      ArrayList<String> progArgs = new ArrayList<String>();
      ArrayList<String> modelArgs = null;
      ArrayList<String> scriptArgs = null;
      ArrayList<String> taskManagerArgs = null;
      
      for (String arg : args) {
         if (modelArgs != null) {
            modelArgs.add (arg);
         }
         else {
            if (arg.equals ("-M")) {
               modelArgs = new ArrayList<String>();
            }
            else {
               progArgs.add (arg);
            }
         }
      }

      // Match arguments one at a time so we can avoid exitOnError if we are
      // running inside matlab
      String[] pargs = progArgs.toArray(new String[0]);
      int lastSwitch = -1;
      int idx = 0;
      while (idx < pargs.length) {
         try {
            int pidx = idx;
            // check for list of arguments:
            if ("-model".equals(pargs[pidx])) {
               if (idx+1 == pargs.length) {
                  System.err.println (
                     "Error: option -model expects additional argument");
                  return;
               }
               modelName.value = pargs[++idx];
               modelArgs = new ArrayList<String>();
               int nidx = maybeCollectArgs(pargs, ++idx, modelArgs);
               if (nidx == idx) {
                  modelArgs = null;
               }
               idx = nidx;
            }
            else if ("-script".equals(pargs[pidx])) {
               if (idx+1 == pargs.length) {
                  System.err.println (
                     "Error: option -script expects additional argument");
                  return;
               }
               scriptFile.value = pargs[++idx];
               scriptArgs = new ArrayList<String>();
               int nidx = maybeCollectArgs(pargs, ++idx, scriptArgs);
               if (nidx == idx) {
                  scriptArgs = null;
               }
               idx = nidx;
            }
            else if ("-taskManager".equals(pargs[pidx])) {
               if (idx+1 == pargs.length) {
                  System.err.println (
                     "Error: option -taskManager expects additional argument");
                  return;
               }
               taskManagerClassName.value = pargs[++idx];
               taskManagerArgs = new ArrayList<String>();
               int nidx = maybeCollectArgs(pargs, ++idx, taskManagerArgs);
               if (nidx == idx) {
                  taskManagerArgs = null;
               }
               idx = nidx;
            }
            else {
               idx = parser.matchArg (pargs, idx);
            }
            
            String unmatched;
            if ((unmatched=parser.getUnmatchedArgument()) != null) {
               
               boolean valid = false;
               if (!valid) {
                  System.err.println (
                     "Unrecognized argument: " + unmatched +
                     "\nUse -help for help information");
                  return;
               }
            } else {
               lastSwitch = pidx;
            }
         }
         catch (Exception e) {
            System.err.println (
               "Error parsing options: "+ e);
            return;
         }
      }
      
      // parser.matchAllArgs (progArgs.toArray(new String[0]));
      if (printOptions.value || printHelp.value) {
         System.out.println (parser.getOptionsMessage (2));
         System.out.println (
"  -model <string> [ <string>... ]\n" +
"                        name of model to start, with optional arguments\n" +
"                        delimited by square brackets\n" +
"  -script <string> [ <string>... ]\n" +
"                        script to run immediately, with optional arguments\n" +
"                        delimited by square brackets\n" +
"  -taskManager <className> [ <string>... ]\n" +
"                        name of task manager class to run immediately, with\n" +
"                        optional arguments delimited by square brackets\n");
         return;
      }

      if (useGLJPanel.value == true) {
         maspack.render.GL.GLViewer.useGLJPanel = true;
      }

      if (noGui.value == true) {
         viewerWidth.value = -1;
      }
      if (glVersion.value != -1) {
         System.out.println (
            "Option '-GLVersion X' is no longer supported. "+
            "Use '-graphics GL2' or '-graphics GL3' instead"); 
         System.exit(1);
      }
      if (zoom.value != -1) {
         System.out.println (
            "Option '-timelineZoom <num>' is no longer supported. "+
            "Use '-timelineRange <maxtime>' instead"); 
         System.exit(1);
      }
      if (configFolder.value != null) {
         File dir = new File(configFolder.value);
         if (!dir.isDirectory()) {
            System.out.println (
               "WARNING: configFolder "+dir+" is not valid folder; ignoring");
         }
         else {
            ArtisynthPath.setConfigFolder (dir);
         }
      }
      Main m = new Main (PROJECT_NAME, !noGui.value);

      if (m.myFrame != null) {
         //m.myViewerManager.setBackgroundColor (
         //   new Color (bgColor[0], bgColor[1], bgColor[2]));
         //m.myViewer.setBackgroundColor (bgColor[0], bgColor[1], bgColor[2]);
         // XXX this should be done in the Main constructor, but needs
         // to be done here instead because of sizing effects
         m.myMenuBarHandler.initToolbar();
      }
      m.start (modelArgs);

      if (exitOnBreak.value) {
         m.myScheduler.addListener (new QuitOnBreakListener(m));
      }
      if (playFor.value > 0) {
         m.play (playFor.value);
      }
      else if (play.value) {
         m.play ();
      }
      
      if (scriptFile.value != null && taskManagerClassName.value != null) {
         System.out.println (
"Cannot specify both a script (option '-script') and a task manager (option \n"+
"'-taskManager' at the same time");
         System.exit(1);
      }
      else if (scriptFile.value != null) {
         File file = new File (scriptFile.value);
         ModelScriptInfo info = new ModelScriptInfo (
            InfoType.SCRIPT, file.getAbsolutePath(), file.getName(),
            scriptArgs == null ? null : scriptArgs.toArray(new String[0]));
         m.runScript (info);
      }
      else if (taskManagerClassName.value != null) {
         m.runTaskManager (taskManagerClassName.value, taskManagerArgs);
      }
      if (m.myJythonConsole != null && m.myJythonFrame == null) {
         m.myJythonConsole.interact ();
      }
      if (openMatlab.value) {
         m.openMatlabConnection();
      }

      if (testModelLoading.value != null) {
         m.testModelLoading(testModelLoading.value);
      }
   }

   /**
    * List of models that cause ArtiSynth to crash, due to memory exhaustion,
    * calls to exit, or lack of GUI access. These should be avoided by
    * testModelLoading.
    */
   private String[] myBadModels = new String[] {
      "artisynth.models.jawTongue.AirwaySkinDemo",
      "artisynth.models.tubesounds.VTNTDemo",
      "artisynth.models.face.MeshEdit",
      "artisynth.models.jawTongue.BadinDataDemo",
      "artisynth.models.palate.RegisteredSoftPalate",
      "artisynth.models.palate.SoftPalate_spring",
      "artisynth.models.palate.SoftPalate_springPeter",
      "artisynth.models.template.HybridFemFactoryDemo",
   };

   /**
    * Returns true if a model is in the "bad model" list.
    */
   private boolean isBadModel (String className) {
      for (String s : myBadModels) {
         if (s.equals (className)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Tests whether all models in a given package can be loaded.  Used for
    * package testing and maintenance.
    */
   private void testModelLoading (String packageName) {
      delay (1.0); // not sure we need this
      ArrayList<String> allModels =
         myRootModelManager.findModels (
            packageName,
            RootModelManager.RECURSIVE |
            RootModelManager.INCLUDE_HIDDEN);
      for (String className : allModels) {
         if (!isBadModel (className)) {
            String modelName = className;
            System.out.println ("MODEL " + className);
            int lastDot = modelName.lastIndexOf ('.');
            if (lastDot != -1) {
               modelName = modelName.substring (lastDot+1);
            }
            if (!loadModelFromClass (className, modelName, new String[0])) {
               System.out.println ("LOAD FAILED: "+className);
            }
            else {
               System.out.println ("LOAD OK: " +  className);
               //ForceScalingUtils.findForceScaling (getRootModel(), false);
            }
            //System.console().readLine();
         }
      }
   }

   private static String[] createArgArray (ArrayList<String> args) {
      if (args == null) {
         return new String[0];
      }
      else {
         return args.toArray(new String[0]);
      }
   }

   void runScript (ModelScriptInfo info) {
      runScript (info, /*saveToHistory=*/true);
   }

   void runScript (ModelScriptInfo info, boolean saveToHistory) {

      if (info.getType() != InfoType.SCRIPT) {
         throw new InternalErrorException (
            "Incorrect info type: " + info.getType());
      }
      if (myFrame != null) {
         setJythonFrameVisible(true);
      }

      stopAll(); // stop any executing code

      File file = new File (info.getClassNameOrFile());
      String errMsg = null;
      if (!file.canRead()) {
         errMsg = "Can't locate or read script file '"+file+"'";
      }
      else {
         if (myJythonConsole == null) {
            createJythonConsole (/*guiBased=*/myFrame != null);
         }
         try {
            myJythonConsole.executeScript (
               file.getAbsolutePath(), info.getArgs());
         }
         catch (Exception e) {
            e.printStackTrace(); 
            errMsg = "Error executing script '"+file+"':\n" + e;
         }
      }

      if (errMsg != null) {
         if (myFrame != null) {
            GuiUtils.showError (myFrame, errMsg);
         }
         else {
            System.out.println (errMsg);
         }
      }
      else {
         if (saveToHistory && myModelScriptHistory != null) {
            myModelScriptHistory.update (info, new Date(System.currentTimeMillis()));
            myModelScriptHistory.save ();
         }
      }
   }

   void runTaskManager (String className, ArrayList<String> args) {

      Class taskClass = null;
      try {
         taskClass = ClassFinder.forName (className, true);
      }
      catch (Exception e) {
         System.out.println (
            "Cannot locate task manager class '" + className + "'");
         System.exit(1); 
      }
      Object managerObj = null;
      try {
         managerObj = taskClass.newInstance();
      }
      catch (Exception e) {
         System.out.println (
            "Cannot instantiate task manager class '" + className + "'");
         System.exit(1); 
      }
      if (!(managerObj instanceof TaskManager)) {
         System.out.println (
"Requested task manager '" + className + "' not an instance of TaskManager");
         System.exit(1);
      }
      TaskManager taskManager = (TaskManager)managerObj;
      taskManager.setMain (this);
      taskManager.setArgs (createArgArray (args));
      taskManager.start();
   }
   
   /**
    * get the root model, static method for the entire program to reference to,
    * so do not pass root model around, because its stored in main and could be
    * accessed using this method
    * 
    * @return workspace object
    */

   public Workspace getWorkspace() {
      return myWorkspace;
   }

   public void createWorkspace() {
      if (myWorkspace == null) {
         myWorkspace = new Workspace (this);
      }
   }

   public static Main getMain() {
      return myMain;
   }

   /**
    * For internal use only; be careful!!
    */
   public static void setMain (Main main) {
      myMain = main;
   }

   /**
    * Get the Scheduler
    * 
    * @return scheduler
    */
   public Scheduler getScheduler() {
      return myScheduler;
   }

   /**
    * Get the EditorManager
    * 
    * @return EditorManager
    */
   public EditorManager getEditorManager() {
      return myEditorManager;
   }

   /**
    * Get the RootModelManager
    * 
    * @return scheduler
    */
   public RootModelManager getRootModelManager() {
      return myRootModelManager;
   }

   public UndoManager getUndoManager() {
      return myUndoManager;
   }

   /**
    * get the timeline controller
    * 
    * @return timeline controller
    */

   public Timeline getTimeline() {
      return myTimeline;
   }

   public File getModelFile() {
      return myModelFile;
   }

   public boolean getSaveWayPointData() {
      return mySaveWayPointData;
   }

   public void setSaveWayPointData (boolean save) {
      mySaveWayPointData = save;
   }

   public boolean getSaveCoreOnly() {
      return mySaveCoreOnly;
   }

   public void setSaveCoreOnly (boolean save) {
      mySaveCoreOnly = save;
   }

   public void reloadModel() throws IOException {
      if (myLastLoadInfo != null) {
         RootModel root = getRootModel();
         if (root != null) {
            loadModel(myLastLoadInfo);
         }
      }
   }

   boolean modelIsLoaded() {
      return myLastLoadInfo != null;
   }

   public String getModelSaveFormat () {
      return myModelSaveFormat;
   }

   public void setModelSaveFormat (String fmtStr) {
      // create a NumberFormat to ensure fmtStr is syntactically correct
      // NumberFormat fmt = new NumberFormat (fmtStr);
      myModelSaveFormat = fmtStr;
   }

   public int saveModelFile (File file) throws IOException {
      return saveModelFile (file, myModelSaveFormat, false, false);
   }

   /**
    * Saves the current root model to a file. If {@code saveWayPointData} is
    * {@code true}, then waypoint data is saved as well (otherwise, waypoints
    * are still saved but <i>without</i> their data).  If {@code
    * coreCompsOnly} is {@code true}, then only components which are part
    * of {@code artisynth_core} are saved (with the root model itself being
    * saved simply as an instance of {@code RootModel}).  Such models can then
    * be loaded by any ArtiSynth installation, without requiring access to the
    * original application code.

    * @param file file to which the model should be saved
    * @param fmtStr format for floating point data. If {@code null},
    * then the default format returned by {@link #getModelSaveFormat} is
    * used instead.
    * @param saveWayPointData save waypoint data along with any waypoints
    * @param coreCompsOnly save only components which are part of
    * {@code artisynth_core}
    * @return if {@code coreCompsOnly} is {@code true}, returns the
    * number of components which were not saved; otherwise, returns 0.
    * @throws IOException if an I/O error is encountered
    */
   public int saveModelFile (
      File file, String fmtStr, 
      boolean saveWayPointData, boolean coreCompsOnly) throws IOException {
      
      RootModel root = getRootModel();
      if (root != null) {
         return saveRootModel (
            file, fmtStr, root, saveWayPointData, coreCompsOnly);
      }
      else {
         return 0;
      }
   }

   public int saveRootModel (
      File file, String fmtStr, RootModel root,
      boolean saveWayPointData, boolean coreCompsOnly) throws IOException {
      
      int numRemoved = 0;
      LinkedList<ModelComponent> incomp = null;
      if (saveWayPointData) {
         root.getWayPoints().setWriteFlags (WayPointProbe.WRITE_ALL_STATES);
      }
      if (coreCompsOnly) {
         ClassAliases.setClassFilter (new CoreFilter());
         if (!ClassAliases.isClassValid(root)) {
            root.setWritable (false);
         }
         incomp = ComponentUtils.markInvalidSubcomps (root);
         numRemoved = incomp.size();
      }
      IndentingPrintWriter pw = new IndentingPrintWriter (file);
      try {
         if (fmtStr == null) {
            fmtStr = getModelSaveFormat();
         }
         NumberFormat fmt = new NumberFormat(fmtStr);
         if (coreCompsOnly) {
            pw.print ("RootModel ");
            RootModel.write (root, pw, fmt);
         }
         else {
            pw.print (ScanWriteUtils.getClassTag (root));
            pw.print (" ");             
            root.write (pw, fmt, root);
         }
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         pw.close();
         if (coreCompsOnly) {
            ComponentUtils.unmarkInvalidSubcomps (incomp);
            ClassAliases.setClassFilter (null);
         }          
         if (saveWayPointData) {
            root.getWayPoints().setWriteFlags (0);
         }
      }
      myModelFile = file;
      mySaveWayPointData = saveWayPointData;
      mySaveCoreOnly = coreCompsOnly;
      return numRemoved;
   }

   /**
    * Saves a component (and its subcomponents, if any) to a file.
    *
    * <p>If {@code coreCompsOnly} is {@code true}, then only subcomponents
    * which are part of {@code artisynth_core} are saved. The resulting file
    * can then be loaded by any ArtiSynth installation, without requiring
    * access to the original application code. If {@code coreCompsOnly} is
    * {@code true} and the {@code comp} itself is not part of {@code
    * artisynth_core}, then no action is taken and the method returns -1.
    *
    * @param file file to which the component should be saved
    * @param fmtStr format for floating point data.If {@code null},
    * then the default format returned by {@link #getModelSaveFormat} is
    * used instead.
    * @param comp component to save
    * @param coreCompsOnly save only subcomponents which are part of {@code
    * artisynth_core}
    * @return if {@code coreCompsOnly} is {@code true}, returns
    * the number of subcomponents which were not saved, or -1 if
    * {@code comp} itself was not saved; otherwise, returns 0.
    * @throws IOException if an I/O error is encountered
    */
   public int saveComponent (
      File file, String fmtStr, ModelComponent comp, 
      boolean coreCompsOnly, ModelComponent ancestor)
      throws IOException {

      if (comp instanceof RootModel) {
         return saveRootModel (
            file, fmtStr, (RootModel)comp, false, coreCompsOnly);
      }
      IndentingPrintWriter pw = null;
      int numRemoved = 0;
      LinkedList<ModelComponent> incomp = null;
      if (coreCompsOnly) {
         ClassAliases.setClassFilter (new CoreFilter());
         if (!ClassAliases.isClassValid(comp)) {
            ClassAliases.setClassFilter (null);
            return -1;
         }
         incomp = ComponentUtils.markInvalidSubcomps (comp);
         numRemoved = incomp.size();
      }
      try {
         pw = ArtisynthIO.newIndentingPrintWriter (file);
         if (fmtStr == null) {
            fmtStr = getModelSaveFormat();
         }
         ScanWriteUtils.writeComponent (
            pw, new NumberFormat(fmtStr), comp, ancestor);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (coreCompsOnly) {
            ComponentUtils.unmarkInvalidSubcomps (incomp);
            ClassAliases.setClassFilter (null);
         }    
         if (pw != null) {
            pw.close();
         }
      }
      return numRemoved;
   }

   /**
    * get the file with probes
    * 
    * @return file with probe data
    */
   public File getProbesFile() {
      return myProbeFile;
   }

   /**
    * set the file with probes
    * 
    * @param file with probe data
    */
   public void setProbesFile (File file) {
      myProbeFile = file;
   }

   /**
    * load the probes into the model
    * 
    * @param file file containing probe information
    * @throws IOException if an I/O or syntax error occurred
    */
   public boolean loadProbesFile (File file) throws IOException {
      if (getWorkspace() == null) {
         return false;
      }
      ReaderTokenizer rtok = ArtisynthIO.newReaderTokenizer (file);
      getWorkspace().scanProbes (rtok);
      rtok.close();

      if (myTimeline != null) {
         myTimeline.requestResetAll();

         // ============================================================
         // Check the model zoom level and set the timeline zoom
         // accordingly

         myTimeline.updateTimeDisplay (0);
         myTimeline.updateComponentSizes();
         myTimeline.setVisibleRange (myDefaultTimelineRange);
      }
      return true;
   }

   /**
    * to save the probes file
    * 
    * @param file probe information file
    * @throws IOException if an I/O error occurred
    */
   public boolean saveProbesFile (File file) throws IOException {
      if (getWorkspace() == null) {
         return false;
      }
      IndentingPrintWriter pw = ArtisynthIO.newIndentingPrintWriter (file);
      getWorkspace().writeProbes (pw, null);
      return true;
   }

   /**
    * get the file for the wayPoints, if any
    * 
    * @return file with wayPoint data
    */
   public File getWayPointsFile() {
      RootModel root = getRootModel();
      if (root != null) {
         return root.getWayPoints().getAttachedFile();
      }
      else {
         return null;
      }
   }

   protected void setWayPointsFile (File file) throws IOException {
      if (getRootModel() != null) {
         WayPointProbe wayPoints = getRootModel().getWayPoints();
         String relOrAbsPath = ArtisynthPath.getRelativeOrAbsolutePath (
            ArtisynthPath.getWorkingDir(), file);
         wayPoints.setAttachedFileName (relOrAbsPath);
      }
   }

   /**
    * Save the waypoints, in binary form, to the specified file. Unless an
    * exception is thrown, the file is then stored as default waypoints file.
    *
    * @param file file to which waypoints should be saved
    */
   public void saveWayPoints (File file) throws IOException {
      if (getRootModel() != null) {
         WayPointProbe wayPoints = getRootModel().getWayPoints();
         String oldFileName = wayPoints.getAttachedFileName();
         setWayPointsFile (file);
         try {
            wayPoints.save();
         }
         catch (IOException e) {
            wayPoints.setAttachedFileName (oldFileName);
            throw e;
         }
      }
   }

   /**
    * Loads waypoints from the specified file. Unless an exception is thrown,
    * the file is then stored as default waypoints file.  The new waypoint data
    * overlays any existing waypoints but does not delete them. To completely
    * replace the existing waypoints, one should call {@link #clearWayPoints}
    * first.
    *
    * @param file file from which additional waypoints should be loaded
    */
   public void loadWayPoints (File file) throws IOException {
      if (getRootModel() != null) {
         WayPointProbe wayPoints = getRootModel().getWayPoints();
         String oldFileName = wayPoints.getAttachedFileName();
         setWayPointsFile (file);
         try {
            wayPoints.load();
         }
         catch (IOException e) {
            wayPoints.setAttachedFileName (oldFileName);
            throw e;
         }
         if (myTimeline != null) {
            myTimeline.refreshWayPoints(getRootModel());
         }
         myMain.rerender();
      }
   }

   public void dispose() {
      if (!disposed) {
         clearRootModel();
         if (myFrame != null) {
            myFrame.dispose ();
            myFrame = null;
         }
         if (myTimeline != null) {
            myTimeline.dispose ();
            myTimeline = null;
         }
         if (myScheduler != null) {
            myScheduler.dispose();
            myScheduler = null;
         }
         if (myJythonConsole != null) {
            myJythonConsole.dispose();
            myJythonConsole = null;
         }
         // potentially save model history
         if (myModelScriptHistory != null) {
            myModelScriptHistory.save();
         }
         disposed = true;
      }
   }

   public boolean isDisposed() {
      return disposed;
   }

   public void quit () {
      dispose();
      exit (0);
   }

   /**
    * add the selection listener
    */

   public void addSelectionListener (SelectionListener l) {
      mySelectionManager.addSelectionListener (l);
   }

   /**
    * remove the selection listener
    */

   public void removeSelectionListener (SelectionListener l) {
      mySelectionManager.removeSelectionListener (l);
   }

   public void addSelected (LinkedList<ModelComponent> items) {
      mySelectionManager.addSelected (items);
   }

   public void removeSelected (LinkedList<ModelComponent> items) {
      mySelectionManager.removeSelected (items);
   }

   private boolean stateInvalidated (
      RootModel root, ComponentChangeEvent e) {

      if (e.getComponent() == root.getWayPoints()) {
         return false;
      }
      else {
         return e.stateChanged();
      }
   }         

   public void componentChanged (ComponentChangeEvent e) {
      RootModel root = getRootModel();
      
      boolean stateInvalidated = false;
      if (root != null) {
         stateInvalidated = stateInvalidated (root, e);
         if (stateInvalidated) {
            root.getWayPoints().invalidateAfterTime(0);
         }
      }
      if (e.getCode() == ComponentChangeEvent.Code.STRUCTURE_CHANGED ||
         e.getCode() == ComponentChangeEvent.Code.DYNAMIC_ACTIVITY_CHANGED) {
         ModelComponent c = e.getComponent();
         if (myFrame != null && c != null &&
             (c == root || c.getParent() == root)) {
            updateApplicationMenuPresent (root);
         }
         if (myTimeline != null) {
            if (root != null && c == root.getWayPoints()) {
               myTimeline.requestUpdateWidgets();
            }
            else {
               // update timeline display regardless
               myTimeline.requestUpdateDisplay();
            }
         }
         if (e.getCode() == ComponentChangeEvent.Code.STRUCTURE_CHANGED) {
            if (c != null && c instanceof CompositeComponent && myFrame != null) {
               myFrame.getNavPanel().updateStructure (c);
               if (root != null &&
                   (c == root.getInputProbes() || c == root.getOutputProbes())) {
                  myTimeline.updateProbes(root);
               }
            }
            if (!getScheduler().isPlaying()) {
               rerender();
            }
         }
      }
      else if (e.getCode() == ComponentChangeEvent.Code.NAME_CHANGED) {
         ModelComponent c = e.getComponent();
         if (myFrame != null) {
            if (c.getParent() != null) {
               myFrame.getNavPanel().updateStructure (c.getParent());
            }
            else {
               myFrame.getNavPanel().updateStructure (c);
            }
         }
      }
      else if (e.getCode() == ComponentChangeEvent.Code.PROPERTY_CHANGED) {
         PropertyChangeEvent pe = (PropertyChangeEvent)e;
         ModelComponent c = e.getComponent();
         if (c == getRootModel()) {
            if (pe.getPropertyName().equals ("maxStepSize")) {
               doSetMaxStep (getRootModel().getMaxStepSize());
            }
            else if (pe.getPropertyName().equals ("defaultViewOrientation")) {
               if (myViewerManager != null) {
                  myViewerManager.resetViewers (
                     getDefaultAxialView(getRootModel()));
               }
            }
         }
         else if (pe.getPropertyName().startsWith ("navpanel")) {
            if (myFrame != null) {
               myFrame.getNavPanel().updateStructure (e.getComponent());
            }
         }
      }
      else if (e.getCode() == ComponentChangeEvent.Code.GEOMETRY_CHANGED) {
         if (!getScheduler().isPlaying()) {
            rerender();
         }
      }
      // John Lloyd, May 2017: We used to invalidate the initial state only
      // if we were not simulating. However, it seems to make sense to do
      // this even if we are simulating.
      // 
      // if (invalidateWaypoints)  && !myScheduler.isPlaying()) {
      if (stateInvalidated) {
         if (root != null) {
            root.invalidateInitialState();
         }
      }
   }

   public boolean getInitDraggersInWorldCoords () {
      return myInitDraggersInWorldCoordsP;
   }

   public void setInitDraggersInWorldCoords (boolean enable) {
      myInitDraggersInWorldCoordsP = enable;
   }

   public boolean getAlignDraggersToPoints() {
      return myAlignDraggersToPointsP;
   }

   public void setAlignDraggersToPoints(boolean enable) {
      myAlignDraggersToPointsP = enable; 
   }

   public boolean getArticulatedTransformsEnabled () {
      return myArticulatedTransformsP;
   }

   public void setArticulatedTransformsEnabled (boolean enable) {
      myArticulatedTransformsP = enable;
   }

   public SelectionMode getSelectionMode() {
      return mySelectionMode;
   }

   private PullController myPullController;

   public PullController getPullController() {
      return myPullController;
   }

   private AddMarkerTool myAddMarkerHandler;
   
   AddMarkerTool getAddMarkerHandler() {
      return myAddMarkerHandler;
   }

   Cursor getDefaultCursor() {
      return Cursor.getDefaultCursor();
   }

   /**
    * Set the current selection mode. Also set the display of the selection
    * buttons.
    * 
    * @param selectionMode selection mode
    */
   public void setSelectionMode (SelectionMode selectionMode) {
      if (mySelectionMode != selectionMode) {
         // NOTE: If we setSelectionEnabled(false) then marker points can only be
         // added to previously selected rigid bodies, if we do not then a marker
         // point will be added to whichever rigid body is clicked on
         if (selectionMode == SelectionMode.AddComponent) {
            myViewerManager.setCursor (
               Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
            // myViewerManager.setSelectionEnabled(false);
         }
         else if (selectionMode == SelectionMode.EllipticSelect) {
            myViewerManager.setCursor (null);
         }
         else {
            myViewerManager.setCursor (getDefaultCursor());
            // myREnderDriver.setSelectionEnabled(true);
         }

         if (mySelectionMode == SelectionMode.AddMarker) {
            // switching out of a marker selection ...
            myViewerManager.setSelectOnPress (false);
            mySelectionManager.removeSelectionListener (myAddMarkerHandler);
            myViewerManager.removeMouseListener (myAddMarkerHandler);
         }
         else if (mySelectionMode == SelectionMode.Pull) {
            // switching out of a Pull selection ...
            myViewerManager.setSelectOnPress (false);
            RootModel root = getRootModel();
            if (root != null) {
               root.removeController (myPullController);
            }
            mySelectionManager.removeSelectionListener (myPullController);
            //myViewerManager.removeRenderable (myPullController);
            myViewerManager.removeMouseListener (myPullController);
         }

         if (selectionMode == SelectionMode.Pull) {
            // switching into a Pull selection ...
            myViewerManager.setSelectOnPress (true);
            myViewerManager.addMouseListener (myPullController);
            //myViewerManager.addRenderable (myPullController);
            mySelectionManager.clearSelections();
            mySelectionManager.addSelectionListener (myPullController);
            RootModel root = getRootModel();
            if (root != null) {
               myPullController.setRootModelDefaults (root);
               root.addController (myPullController, findMechSystem(root));
            }
         }
         else if (selectionMode == SelectionMode.AddMarker) {
            // switching into a marker selection ...
            myViewerManager.setSelectOnPress (true);
            myViewerManager.addMouseListener (myAddMarkerHandler);
            mySelectionManager.clearSelections();
            mySelectionManager.addSelectionListener (myAddMarkerHandler);
         }

         if (selectionMode == SelectionMode.EllipticSelect) {
            myViewerManager.setEllipticSelection (true);
         }
         else {
            myViewerManager.setEllipticSelection (false);
         }
         mySelectionMode = selectionMode;

         if (myMenuBarHandler.modeSelectionToolbar != null) {
            SelectionToolbar toolbar =
               (SelectionToolbar)myMenuBarHandler.modeSelectionToolbar;
          toolbar.setSelectionButtons();
         }

         if (myWorkspace != null) {
            rerender();
         }

         setDragger();
      }
   }

//   public void setViewerMode (ViewerMode viewerMode) {
//      this.viewerMode = viewerMode;
//   }

//   public ViewerMode getViewerMode() {
//      return viewerMode;
//   }

   private class SelectionHandler implements SelectionListener {

      public void selectionChanged (SelectionEvent e) {

         if (mySelectionMode != SelectionMode.Pull) {
            setDragger();
         }
      }
   }

   private class TransformAction implements Runnable {

      TransformComponentsCommand myTransformCmd;
      GeometryTransformer myGt;

      TransformAction (GeometryTransformer gtr, TransformComponentsCommand cmd) {
         myTransformCmd = cmd;
         myGt = gtr;
      }

      public void run() {
         myTransformCmd.transform (myGt);
      }
   }

   private class Dragger3dHandler extends Dragger3dAdapter {

      TransformComponentsCommand myTransformCmd;
      GeometryTransformer myGtr;

      /**
       * Modify the transform so that it will act with respect to the dragger's
       * current position. Otherwise, rotation and scaling will act about the
       * world origin. This is done by transforming to dragger coordinates,
       * applying the dragger transform, and transforming back.
       */
      void centerTransform (
         AffineTransform3dBase X, RigidTransform3d XDraggerToWorld) {
         RigidTransform3d XWorldToDragger = new RigidTransform3d();
         XWorldToDragger.invert (XDraggerToWorld);
         if (X instanceof AffineTransform3d) {
            AffineTransform3d XA = (AffineTransform3d)X;
            XA.mul (XA, XWorldToDragger);
            XA.mul (XDraggerToWorld, XA);
         }
         else {
            RigidTransform3d XR = (RigidTransform3d)X;
            XR.mul (XR, XWorldToDragger);
            XR.mul (XDraggerToWorld, XR);
         }
      }

      private AffineTransform3dBase getIncrementalTransform (Dragger3dEvent e) {
         Dragger3dBase dragger = (Dragger3dBase)e.getSource();
         AffineTransform3dBase transform =
            e.getIncrementalTransform().copy();
         if (e.getSource() instanceof Rotator3d ||
            e.getSource() instanceof Transrotator3d ||
            e.getSource() instanceof Translator3d ||
            e.getSource() instanceof RotatableScaler3d) {
            centerTransform (transform, dragger.getDraggerToWorld());
         }
         return transform;
      }

      private AffineTransform3dBase getOverallTransform (Dragger3dEvent e) {
         Dragger3dBase dragger = (Dragger3dBase)e.getSource();
         AffineTransform3dBase transform = e.getTransform().copy();
         if (e.getSource() instanceof Rotator3d ||
            e.getSource() instanceof Transrotator3d || 
            e.getSource() instanceof Translator3d || 
            e.getSource() instanceof RotatableScaler3d) {
            centerTransform (transform, dragger.getDraggerToWorld());
         }
         return transform;
      }

      private String getDraggerName (Dragger3dEvent e) {
         Dragger3dBase dragger = (Dragger3dBase)e.getSource();
         if (dragger instanceof Translator3d ||
            dragger instanceof ConstrainedTranslator3d) {
            return "translation";
         }
         else if (dragger instanceof Rotator3d) {
            return "rotation";
         }
         else if (dragger instanceof RotatableScaler3d) {
            return "scaling";
         }
         else if (dragger instanceof Transrotator3d) {
            return "transrotation";
         }
         else {
            throw new InternalErrorException (
               "transform command unimplemented for " + dragger.getClass());
         }
      }         

      @SuppressWarnings("unchecked")
      private void createTransformCommand (Dragger3dEvent e) {
         
         // indicate that dragger is responsible for transforming, and 
         // allow auto-flipping of orientation for negative-determinant transforms
         String name = getDraggerName(e);
         int flags = TransformableGeometry.TG_DRAGGER | OrientedTransformableGeometry.OTG_AUTOFLIP;
         if (myArticulatedTransformsP && name != "scaling") {
            flags |= TransformableGeometry.TG_ARTICULATED;
         }
         if (isSimulating()) {
            flags |= TransformableGeometry.TG_SIMULATING;
         }
         myGtr = GeometryTransformer.create (getIncrementalTransform (e)); 
         myTransformCmd =
            new TransformComponentsCommand (
               name, (LinkedList<ModelComponent>)myDraggableComponents.clone(),
               myGtr, flags);
         if (!myUndoTransformsWithInverse) {
            myGtr.setUndoState (UndoState.SAVING);
            myTransformCmd.setUndoWithInverse (false);
         }
      }

      private void requestExecution (GeometryTransformer gtr) {
         if (!myScheduler.requestAction (
                new TransformAction (gtr, myTransformCmd))) {
            myTransformCmd.transform (gtr);
         }
      }

      public void draggerBegin (Dragger3dEvent e) {
         createTransformCommand (e);
         requestExecution(myGtr);
      }

      public void draggerMove (Dragger3dEvent e) {
         GeometryTransformer gtr = 
            GeometryTransformer.create(getIncrementalTransform (e));
         requestExecution (gtr);
      }

      public void draggerEnd (Dragger3dEvent e) {
         GeometryTransformer gtr = 
            GeometryTransformer.create(getIncrementalTransform (e));
         requestExecution (gtr);
         if (myUndoTransformsWithInverse) {
            myTransformCmd.setTransformer (
               GeometryTransformer.create(getOverallTransform (e)));
         }
         else {
            myTransformCmd.setTransformer (myGtr);
         }
         myUndoManager.addCommand (myTransformCmd);
      }
      
      public void draggerRepositioned (Dragger3dEvent e) {
         if (e.getTransform() instanceof RigidTransform3d &&
            myDraggableComponents.size() == 1) {

            ModelComponent comp = myDraggableComponents.get(0);
            RigidTransform3d TDW = currentDragger.getDraggerToWorld();
            RigidTransform3d TCW = new RigidTransform3d();
            computeDraggerToWorld (
               TCW, myDraggableComponents, currentDragger);

            RigidTransform3d TOFF = new RigidTransform3d();
            TOFF.mulInverseLeft (TCW, TDW);
            DraggerFrameInfo finfo = myDraggerFrameInfoMap.get (comp);
            if (finfo == null) {
               finfo = new DraggerFrameInfo();
               myDraggerFrameInfoMap.put (comp, finfo);
            }
            finfo.myTOFF.set (TOFF);
         }
      }
   }

   private static final double inf = Double.POSITIVE_INFINITY;

   /**
    * Given a collection of points, set the dragger-to-world transform to their
    * centroid. Also, if getAlignDraggersToPoints() and
    * getInitDraggersInWorldCoords() return true and false, respectively, then
    * if the points are colinear or coplanar, align the dragger orientation
    * with either the line or plane normal. Otherwise, align the dragger
    * orientation with world.
    */
   private void estimatePoseFromPoints (
      RigidTransform3d TDW, ArrayList<Point3d> positions) {
      
      Vector3d centroid = new Vector3d();
      for (Point3d p : positions) {
         centroid.add (p);
      }
      centroid.scale (1.0/positions.size());
      TDW.p.set (centroid);
      if (getAlignDraggersToPoints() && !getInitDraggersInWorldCoords()) {
         // compute covariance wrt the centroid and use SVD to test
         // for colinear or coplanar.
         Matrix3d C = new Matrix3d();
         Vector3d vec = new Vector3d();
         for (Point3d p : positions) {
            vec.sub (p, centroid);
            C.addOuterProduct (vec, vec);
         }
         SVDecomposition3d svd = new SVDecomposition3d();
         svd.factor (C);
         Vector3d sig = new Vector3d();
         svd.getS (sig);
         Matrix3d U = new Matrix3d (svd.getU());
         double tol = 1e-8*C.frobeniusNorm();
         Vector3d dir = null;
         if (Math.abs(sig.y) < tol) {
            // colinear, along the first column of U
            dir = new Vector3d();
            U.getColumn (0, dir);
         }
         else if (Math.abs(sig.z) < tol) {
            // coplanar, perpendicular to the third column of U
            dir = new Vector3d();
            U.getColumn (2, dir);
         }
         if (dir != null) {
            // Align the orientation with the line or plane direction.  Do this
            // by realigning the axis closest to the direction.
            double maxdot = Double.NEGATIVE_INFINITY;
            int nearestAxis = -1;
            for (int i=0; i<3; i++) {
               Vector3d axis = new Vector3d();
               axis.set (i, 1);
               double dot = Math.abs (axis.dot(dir));
               if (dot > maxdot) {
                  maxdot = dot;
                  nearestAxis = i;
               }
            }
            switch (nearestAxis) {
               case 0: {
                  TDW.R.setXDirection (dir);
                  break;
               }
               case 1: {
                  TDW.R.setYDirection (dir);
                  break;
               }
               case 2: {
                  TDW.R.setZDirection (dir);
                  break;
               }
               default: {
                  TDW.R.setIdentity();
               }
            }
            return;            
         }
      }
      TDW.R.setIdentity();
   }

   protected double computeDraggerToWorld (
      RigidTransform3d TDW, List<ModelComponent> draggables,
      Dragger3dBase dragger) {

      double radius = 0;
      boolean frameDetermined = false;

      if (dragger != null) {
         TDW.set (dragger.getDraggerToWorld());
      }
      DraggerFrameInfo finfo = null;
      if (draggables.size() == 1) {
         finfo = myDraggerFrameInfoMap.get (draggables.get(0));
      }
      if (draggables.size() == 1 &&
          draggables.get(0) instanceof HasCoordinateFrame) {
         // there is only one component and it has a coordinate frame
         HasCoordinateFrame hasFrame = (HasCoordinateFrame)draggables.get(0);
         hasFrame.getPose (TDW);
         if (finfo != null) {
            TDW.mul (finfo.myTOFF);
         }
         if (dragger == null && getInitDraggersInWorldCoords()) {
            TDW.R.setIdentity();
         }
         frameDetermined = true;
      }
      else if (draggables.size() > 1) {
         // if there are multiple draggables that are points, determine the
         // frame from the point positions
         ArrayList<Point3d> positions = new ArrayList<>();
         for (ModelComponent c : draggables) {
            if (c instanceof Point) {
               positions.add (((Point)c).getPosition());
            }
            else if (c instanceof VertexComponent) {
               positions.add (((VertexComponent)c).getWorldPosition());
            }
            else {
               positions = null;
               break;
            }
         }
         if (positions != null) {
            estimatePoseFromPoints (TDW, positions);
            frameDetermined = true;
         }
      }
      if (dragger == null || !frameDetermined) {
         // need to compute bounds if there is no dragger (to determine
         // radius), or if we haven't computed a frame yet (to determine the
         // frame).
         Point3d pmin = new Point3d (inf, inf, inf);
         Point3d pmax = new Point3d (-inf, -inf, -inf);
         for (ModelComponent c : draggables) {
            ((Renderable)c).updateBounds (pmin, pmax);
         }
         radius = pmin.distance (pmax);
         if (!frameDetermined) {
            TDW.p.add (pmin, pmax);
            TDW.p.scale (0.5);
            if (finfo != null) {
               TDW.mul (finfo.myTOFF);
            }
         }
      }
      if (finfo != null) {
         radius *= finfo.mySizeScale;
      }
      return radius;
   } 

   /**
    * Called to update the current dragger position.
    */
   public void updateDragger() {
      // John Lloyd: Disabled as of Nov 2017. We do not change the
      // dragger position after a drag is completed.
      
      // if (currentDragger != null && !currentDragger.isDragging()) {
      //    if (myDraggableComponents.size() > 0) {
      //       RigidTransform3d TDW = new RigidTransform3d();
      //       computeDraggerToWorld (TDW, myDraggableComponents, currentDragger);
      //       currentDragger.setDraggerToWorld (TDW);
      //    }
      // }
   }

   public void resetDraggerFrame (ManipulatorFrameSpec mode) {
      if (currentDragger != null && !currentDragger.isDragging()) {
         switch (mode) {
            case LOCAL: {
               RigidTransform3d TDW = new RigidTransform3d();
               computeDraggerToWorld (
                  TDW, myDraggableComponents, currentDragger);
               currentDragger.setDraggerToWorld (TDW);
               rerender();
               break;
            }
            case WORLD: {
               currentDragger.setDraggerToWorld (RigidTransform3d.IDENTITY);
               rerender();
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented frame mode "+mode);
            }
         }
      }
   }
   
   public void clearDraggerFrameInfo() {
      if (currentDragger != null && 
          !currentDragger.isDragging()) {
         if (myDraggableComponents.size() == 1) {
            ModelComponent comp = myDraggableComponents.get(0);
            myDraggerFrameInfoMap.remove (comp);               
         }
         RigidTransform3d TDW = new RigidTransform3d();
         computeDraggerToWorld (TDW, myDraggableComponents, null);
         currentDragger.setDraggerToWorld (TDW);
         currentDragger.setSizeScale (1);
         rerender();
      }
   }

   public void scaleDraggerSize (double s) {
      if (currentDragger != null) {
         double sizeScale = s*currentDragger.getSizeScale();
         currentDragger.setSizeScale (sizeScale);
         if (myDraggableComponents.size() == 1) {
            ModelComponent comp = myDraggableComponents.get(0);
            DraggerFrameInfo finfo = myDraggerFrameInfoMap.get (comp);
            if (finfo == null) {
               finfo = new DraggerFrameInfo();
               myDraggerFrameInfoMap.put (comp, finfo);
            }
            finfo.mySizeScale = sizeScale;
         }
         rerender();
      }
   }

   public void flipDraggerAxesForward () {
      if (currentDragger != null) {
         RigidTransform3d TDW =
            new RigidTransform3d(currentDragger.getDraggerToWorld());
         RigidTransform3d TDE = new RigidTransform3d();
         TDE.mul (getViewer().getViewMatrix(), TDW);
         
         final int FLIP_X = 0x1;
         final int FLIP_Y = 0x2;
         final int FLIP_Z = 0x4;

         int flipCode = 0;
         if (TDE.R.m20 < 0) flipCode |= FLIP_X;
         if (TDE.R.m21 < 0) flipCode |= FLIP_Y;
         if (TDE.R.m22 < 0) flipCode |= FLIP_Z;
         if (flipCode == 0) {
            return;
         }
         RigidTransform3d TFLIP = new RigidTransform3d();

         System.out.println ("flipCode=" + flipCode);
         switch (flipCode) {
            case FLIP_X: {
               TFLIP.R.set (0, 0, -1,  0, 1, 0,  1, 0, 0);
               break;
            }
            case FLIP_Y: {
               TFLIP.R.set (0, 1, 0,  -1, 0, 0,  0, 0, 1);
               break;
            }
            case FLIP_Z: {
               TFLIP.R.set (1, 0, 0,  0, 0, 1,  0, -1, 0);
               break;
            }
            case FLIP_X | FLIP_Y: {
               TFLIP.R.set (-1, 0, 0,  0, -1, 0,  0, 0, 1);
               break;
            }
            case FLIP_Y | FLIP_Z: {
               TFLIP.R.set (1, 0, 0,  0, -1, 0,  0, 0, -1);
               break;
            }
            case FLIP_Z | FLIP_X: {
               TFLIP.R.set (-1, 0, 0,  0, 1, 0,  0, 0, -1);
               break;
            }
            case FLIP_X | FLIP_Y | FLIP_Z: {
               TFLIP.R.set (-1, 0, 0,  0, 0, -1,  0, -1, 0);
               break;
            }
         }
         TDW.mul (TFLIP);
         currentDragger.setDraggerToWorld (TDW);
         if (myDraggableComponents.size() == 1) {
            // store the offset 
            ModelComponent comp = myDraggableComponents.get(0);
            DraggerFrameInfo finfo = myDraggerFrameInfoMap.get (comp);
            if (finfo == null) {
               finfo = new DraggerFrameInfo();
               myDraggerFrameInfoMap.put (comp, finfo);
            }
            finfo.myTOFF.mul (TFLIP);
         }
         rerender();
      }
   }

   private void setDragger() {
      translator3d.setVisible (false);
      transrotator3d.setVisible (false);
      scalar3d.setVisible (false);
      rotator3d.setVisible (false);
      constrainedTranslator3d.setVisible (false);
      constrainedTranslator3d.setMesh (null);
      currentDragger = null;

      myDraggableComponents.clear();

      if (mySelectionMode != SelectionMode.Select &&
          mySelectionMode != SelectionMode.EllipticSelect &&
          mySelectionMode != SelectionMode.AddMarker &&
          mySelectionMode != SelectionMode.Pull) {
         Point3d pmin = new Point3d (inf, inf, inf);
         Point3d pmax = new Point3d (-inf, -inf, -inf);

         int n = 0;

         for (ModelComponent sel : mySelectionManager.getCurrentSelection()) {
            if (sel instanceof Renderable &&
               sel instanceof TransformableGeometry &&
               !ComponentUtils.isAncestorSelected(sel)) {
               myDraggableComponents.add (sel);
               ((Renderable)sel).updateBounds (pmin, pmax);
               n++;
            }
         }
         
         if (n > 0) {
            RigidTransform3d TDW = new RigidTransform3d();
            double radius =
               computeDraggerToWorld (TDW, myDraggableComponents, null);

            // set a minimum radius to about 1/6 of the viewer window width
            radius =
               Math.max (radius,
                  myViewer.distancePerPixel (myViewer.getCenter())
                  * myViewer.getScreenWidth() / 6);

            if (mySelectionMode == SelectionMode.Translate) {
               translator3d.setVisible (true);
               translator3d.setDraggerToWorld (TDW);
               translator3d.setSize (radius);
               currentDragger = translator3d;
            }
            else if (mySelectionMode == SelectionMode.Transrotate) {
               transrotator3d.setVisible (true);
               transrotator3d.setDraggerToWorld (TDW);
               transrotator3d.setSize (radius);
               currentDragger = transrotator3d;
            }
            else if (mySelectionMode == SelectionMode.Scale) {
               scalar3d.setVisible (true);
               scalar3d.setDraggerToWorld (TDW);
               scalar3d.setSize (radius);
               currentDragger = scalar3d;
            }
            else if (mySelectionMode == SelectionMode.Rotate) {
               rotator3d.setVisible (true);
               rotator3d.setDraggerToWorld (TDW);
               rotator3d.setSize (radius);
               currentDragger = rotator3d;
            }
            else if (mySelectionMode == SelectionMode.ConstrainedTranslate) {
               PolygonalMesh mesh = null;

               for (Object sel : mySelectionManager.getCurrentSelection()) {
                  if (sel instanceof FrameMarker) {
                     FrameMarker frameMarker = (FrameMarker)sel;
                     Point3d p = new Point3d (frameMarker.getPosition());
                     constrainedTranslator3d.setLocation (p);
                     Frame frame = frameMarker.getFrame();

                     if (frame instanceof RigidBody) {
                        mesh = ((RigidBody)frame).getSurfaceMesh();
                     }
                     break;
                  }
               }
               
               if (mesh == null) {
                  // no framemarkers, look for anything else attached to a mesh
                  for (ModelComponent sel : mySelectionManager.getCurrentSelection()) {
                     
                     // look for a HasSurfaceMesh parent
                     ModelComponent parent = sel.getParent ();
                     while (parent != null && !(parent instanceof HasSurfaceMesh)) {
                        parent = parent.getParent ();
                     }
                     
                     if (parent != null) {
                        HasSurfaceMesh sm = (HasSurfaceMesh)parent;
                        mesh = sm.getSurfaceMesh ();
                        
                        if (sel instanceof Point) {
                           constrainedTranslator3d.setLocation (((Point)sel).getPosition ());
                        }                  
                     }
                  }
               }

               if (mesh != null) {
                  constrainedTranslator3d.setSize (radius);
                  constrainedTranslator3d.setMesh (mesh);
                  constrainedTranslator3d.setVisible (true);
                  currentDragger = constrainedTranslator3d;
               }
            }
         }
      }
   }

   private class RestoreSelectionHighlightingHandler extends WindowAdapter {
      public void windowClosed (WindowEvent e) {
         myViewerManager.setSelectionHighlightStyle (
            HighlightStyle.COLOR);
         myViewerManager.render();
      }
   }

   RestoreSelectionHighlightingHandler myRestoreSelectionHighlightingHandler =
      new RestoreSelectionHighlightingHandler();

   /**
    * Register a property window with the main program. This will cause a
    * listener to be added so that a rerender request is issued whenever
    * property values change. It will also set the current root model as a
    * synchronization object, and ensure that the window is deleted when the
    * root model changes.
    *
    * <p>If the window is a render props dialog, handlers will be added to
    * ensure that viewer selection coloring is disabled while the dialog is
    * open.
    */
   public void registerWindow (PropertyWindow w) {
      if (myWorkspace != null) {
         myWorkspace.registerWindow (w);
      }
      if (w instanceof RenderPropsDialog) {
         // disable selection highlighting while the window is active
         if (myViewerManager.getSelectionHighlightStyle() ==
             HighlightStyle.COLOR) {
            myViewerManager.setSelectionHighlightStyle (HighlightStyle.NONE);
            myViewerManager.render();
            ((RenderPropsDialog)w).addWindowListener (
               myRestoreSelectionHighlightingHandler);
         }
      }
   }

   public void deregisterWindow (PropertyWindow w) {
      if (myWorkspace != null) {
         myWorkspace.deregisterWindow (w);
      }
   }

   /** 
    * For diagnostic purposes.
    */
   public LinkedList<PropertyWindow> getPropertyWindows() {
      if (myWorkspace != null) {
         return myWorkspace.getPropertyWindows();
      }
      else {
         return null;
      }
   }

   public MovieMaker getMovieMaker() {
      if (myMovieMaker == null) {
         try {
            myMovieMaker = new MovieMaker (getViewer ());
         }
         catch (Exception e) {
            e.printStackTrace(); 
            throw new InternalErrorException ("Cannot create movie maker");
         }
      }
      return myMovieMaker;
   }

   public void setModelDirectory (File dir) {
      if (!dir.isDirectory()) {
         throw new IllegalArgumentException (
            "Specified directory does not exist or is not a directory");
      }
      myModelDir = dir;
   }

   public File getModelDirectory() {
      if (myModelDir == null) {
         myModelDir = ArtisynthPath.getWorkingDir();
      }
      return myModelDir;
   }

   public void setProbeDirectory (File dir) {
      if (!dir.isDirectory()) {
         throw new IllegalArgumentException (
            "Specified directory does not exist or is not a directory");
      }
      myProbeDir = dir;
   }

   public File getProbeDirectory() {
      if (myProbeDir == null) {
         myProbeDir = ArtisynthPath.getWorkingDir();
      }
      return myProbeDir;
   }

   public void arrangeControlPanels (RootModel root) {
      if (myFrame != null) {
         java.awt.Point loc = myFrame.getLocation();
         int locx = loc.x + myFrame.getWidth();
         int locy = loc.y;

         int maxWidth = 0;
         for (ControlPanel panel : root.getControlPanels()) {
            panel.pack();
            panel.setLocation (locx, locy);
            if (panel.getWidth() > maxWidth) {
               maxWidth = panel.getWidth();
            }
            locy += panel.getHeight();
         }
         for (ControlPanel panel : root.getControlPanels()) {
            if (panel.getWidth() < maxWidth) {
               panel.setSize (maxWidth, panel.getHeight());
            }
            panel.setVisible (true);
         }
      }
   }

   private void delay (int msec) {
      try {
         Thread.sleep (1000);
      }
      catch (Exception e) {
      }
   }

   public void screenShot(String filename) {
      getMovieMaker().grabScreenShot(filename);
   }

   /** 
    * Attempts to prevent artisynth form stealing focus when it
    * pops up windows, etc, especially while running a script.
    */   
   public void maskFocusStealing (boolean enable) {
      if (enable != myFocusStealingMaskedP) {
         RootModel.setFocusable (!enable);
         if (myTimeline != null) {
            myTimeline.setFocusableWindowState (!enable);
         }
         if (myFrame != null) {
            myFrame.setFocusableWindowState (!enable);
         }
         if (myJythonFrame != null) {
            myJythonFrame.setFocusableWindowState (!enable);
         }
         myFocusStealingMaskedP = enable;
      }
   }

   /** 
    * Have our own exit method so that if we're running under matlab, we don't
    * actually exit.
    */
   static public void exit (int code) {
      File testForFglrxDriver = new File ("/var/lib/dkms/fglrx/8.911");
      if (testForFglrxDriver.exists()) {
         // this version of the FGLRX driver has a bug that causes the JVM to
         // crash in XQueryExtension on exit, so instead we exit directly.
         System.out.println ("Fglrx driver detected; exiting directly");
         PardisoSolver solver = new PardisoSolver();
         solver.systemExit (code);
      }
      else if (myRunningUnderMatlab) {
         //throw new IllegalArgumentException ("Quit");
      }
      else {
         System.exit (code);
      }
   }

   public boolean closeMatlabConnection () {
      if (myMatlabConnection != null) {
         myMatlabConnection.dispose();
         myMatlabConnection = null;
         return true;
      }
      else {
         return false;
      }
   }      

   public MatlabInterface openMatlabConnection () {
      if (!hasMatlabConnection()) {
         try {
            myMatlabConnection = MatlabInterface.create();
         }
         catch (Exception e) {
            System.out.println (
               "Error connecting to MATLAB: " + e);
            myMatlabConnection = null;
         }
      }
      return myMatlabConnection;
   }

   public MatlabInterface getMatlabConnection() {
      if (myMatlabConnection != null && !myMatlabConnection.isConnected()) {
         closeMatlabConnection();
      }
      return myMatlabConnection;
   }

   public boolean hasMatlabConnection() {
      if (myMatlabConnection == null) {
         return false;
      }
      else if (!myMatlabConnection.isConnected()) {
         closeMatlabConnection();
         return false;
      }
      else {
         return true;
      }
   }

   public void printAllThreads() {
      Set<Thread> threads = Thread.getAllStackTraces().keySet();
      System.out.println ("num threads=" + threads.size());
      for (Thread thr : threads) {
         System.out.println (thr.getClass());
      }
   }

   public void printClassPath() {
      URL[] urls = null;
      if (getClass().getClassLoader() instanceof URLClassLoader) {
         URLClassLoader ucl = (URLClassLoader)getClass().getClassLoader();
         System.out.println ("ArtiSynth class path:");
         for (URL url : ucl.getURLs()) {
            System.out.println (" " + url);
         }
      }
      else {
         System.out.println (
            "Can't print class path: " +
            "expected ClassLoader to be a URLClassLoader but instead it is "+
            getClass().getClassLoader().getClass());
            
      }
   }

   public String getHomeDir() {
      return ArtisynthPath.getHomeDir();
   }
   
}
