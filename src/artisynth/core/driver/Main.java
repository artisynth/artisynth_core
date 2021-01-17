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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.DoubleHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import artisynth.core.driver.ModelInfo.ModelType;
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
import maspack.geometry.ConstrainedTranslator3d;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.GeometryTransformer.UndoState;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.render.Dragger3dAdapter;
import maspack.render.Dragger3dBase;
import maspack.render.Dragger3dEvent;
import maspack.render.Renderable;
import maspack.render.Renderer.HighlightStyle;
import maspack.render.RotatableScaler3d;
import maspack.render.Rotator3d;
import maspack.render.Translator3d;
import maspack.render.Transrotator3d;
import maspack.render.GL.GLSupport;
import maspack.render.GL.GLSupport.GLVersionInfo;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewer.GLVersion;
import maspack.render.GL.GLViewerFrame;
import maspack.solvers.PardisoSolver;
import maspack.solvers.SparseSolverId;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.Logger;
import maspack.util.Logger.LogLevel;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.widgets.MouseBindings;
import maspack.widgets.PropertyWindow;
import maspack.widgets.RenderPropsDialog;
import maspack.widgets.ViewerKeyListener;
import maspack.widgets.ViewerToolBar;

/**
 * the main class for artisynth
 * 
 */
public class Main implements DriverInterface, ComponentChangeListener {

   /**
    * private data members for the driver interface 
    */
   protected File myModelDir;
   protected File myProbeDir;

   protected MainFrame myFrame;
   protected MenuBarHandler myMenuBarHandler;
   protected GenericKeyHandler myKeyHandler;
   protected static Main myMain;
   protected Scheduler myScheduler;
   protected EditorManager myEditorManager;
   protected UndoManager myUndoManager;
   protected MovieMaker myMovieMaker;
   protected Logger myLogger;

   protected static boolean myRunningUnderMatlab = false;

   // root model declared
   protected Workspace myWorkspace = null;
   protected Timeline myTimeline;
   protected boolean timeLineRight = false;
   protected boolean doubleTime = false;
   protected String myErrMsg;
   protected GLViewer myViewer;
   protected SelectionManager mySelectionManager = null;
   protected ArtisynthJythonConsole myJythonConsole = null;
   protected JFrame myJythonFrame = null;
   protected ViewerManager myViewerManager;
   protected AliasTable myDemoModels;
   protected ModelHistory myModelHistory = null;  // storage for model history
   protected boolean myFocusStealingMaskedP = false;

   protected AliasTable myScripts;
   protected final static String PROJECT_NAME = "ArtiSynth";
   protected MatlabInterface myMatlabConnection;
   protected AxisAngle myDefaultViewOrientation = 
      new AxisAngle(AxisAngle.ROT_X_90);

   protected String myModelName;
   protected String[] myModelArgs;    // command-line supplied arguments
   protected ModelInfo lastModelInfo; // for re-loading a model with the same parameters
   protected int myFlags = 0;
   protected double myMaxStep = -1;
   protected boolean disposed = false;

   protected GLVersion myGLVersion = GLVersion.GL3;

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

   private SelectionMode mySelectionMode;
   private boolean myArticulatedTransformsP = true;
   private boolean myInitDraggersInWorldCoordsP = false;

   private Translator3d translator3d = new Translator3d();
   private Transrotator3d transrotator3d = new Transrotator3d();
   private RotatableScaler3d scalar3d = new RotatableScaler3d();
   private Rotator3d rotator3d = new Rotator3d();
   private ConstrainedTranslator3d constrainedTranslator3d =
      new ConstrainedTranslator3d();
   private Dragger3dBase currentDragger;
   private LinkedList<ModelComponent> myDraggableComponents =
      new LinkedList<ModelComponent>();


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

   // changed from protected to public for cubee
   public String[] getDemoNames() {
      return myDemoModels.getAliases();
   }

   public String getDemoClassName (String classNameOrAlias) {
      String name = myDemoModels.getName (classNameOrAlias);
      if (name != null) {
         return name;
      }
      else {
         try {
            Class.forName (classNameOrAlias);
         }
         catch (ClassNotFoundException e) {
            return null;
         }
         return classNameOrAlias;
      }
   }

   protected boolean isDemoName (String name) {
      return myDemoModels.containsAlias (name);
   }

   protected boolean isDemoClassName(String classNameOrAlias) {
      if (myDemoModels.containsAlias (classNameOrAlias)) {
         return true;
      }
      return myDemoModels.containsName (classNameOrAlias);
   }

   public String[] getScriptNames() {
      return myScripts.getAliases();
   }

   public String getScriptName (String alias) {
      return myScripts.getName (alias);
   }

   public GLVersion getGLVersion() {
      return myGLVersion;
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
      this (PROJECT_NAME, 800, 600, GLVersion.GL3);
   }

   /**
    * read the demo names from demosFile
    */
   private void readDemoNames(String filename) {
      URL url = ArtisynthPath.findResource (filename);
      if (url == null) {
         System.out.println ("Warning: demosFile: " + filename
                             + " not found");
      }
      else {
         try {
            myDemoModels = new AliasTable (url);
         }
         catch (Exception e) {
            System.out.println ("Warning: error reading demosFile: "
                                + filename);
            System.out.println (e.getMessage());
         }
      }
      if (myDemoModels == null) {
         myDemoModels = new AliasTable(); // default: an empty alias table
      }
   }

   String getDemosFilename() {
      return demosFilename.value;
   }
   
   String getDemosMenuFilename() {
      return demosMenuFilename.value;
   }
   
   /**
    * Reads model history information
    */
   private void readModelHistory(String filename) {
      
      // no history
      if (filename == null || "".equals(filename)) {
         return;
      }

      File file = ArtisynthPath.findFile(filename);
      if (file == null) {
         // System.out.println ("Warning: history file '" + filename + "' not found");
         myModelHistory = new ModelHistory();
      }
      else {
         historyFilename.value = file.getAbsolutePath(); // store for future use
         try {
            myModelHistory = new ModelHistory();  
            myModelHistory.read(file);
            
         } catch (Exception e) {
            System.out.println ("Warning: error reading history file: "
               + filename);
            System.out.println (e.getMessage());
            myModelHistory = null;          // ensure it's null
         }
      }
   }
   
   public ModelHistory getModelHistory() {
      if (myModelHistory == null) {
         myModelHistory = new ModelHistory ();
      }
      return myModelHistory;
   }

   /**
    * Write history information
    */
   private void saveModelHistory(String filename) {
      // no history
      if (filename == null || "".equals(filename)) {
         return;
      }
      
      try {
         myModelHistory.save(new File(filename));
         historyFilename.value = filename;  // store for future reference in loading
      } catch (Exception e) {
         System.out.println ("Warning: error reading history file: "
            + filename);
         System.out.println (e.getMessage());
         myModelHistory = null;          // ensure it's null
      }
      
   }

   public void addDemoName(String alias, String className) {
      myDemoModels.addEntry(alias, className);      
   }

   public void removeDemoName(String alias) {
      myDemoModels.removeEntry(alias);
   }

   public void removeDemoClass(String className) {
      String alias = myDemoModels.getAlias(className);
      if (alias != null) {
         removeDemoName(alias);
      }
   }

   public AliasTable getDemoTable() {
      return myDemoModels;
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

   /**
    * read the script names
    */
   private void readScriptNames() {
      URL url = ArtisynthPath.findResource (scriptsFilename.value);
      if (url == null) {
         System.out.println ("Warning: scriptsFile: " + scriptsFilename.value
            + " not found");
      }
      else {
         try {
            myScripts = new AliasTable (url);
         }
         catch (FileNotFoundException e) {
         }
         catch (Exception e) {
            System.out.println ("Warning: error reading scriptsFile: "
               + scriptsFilename.value);
            System.out.println (e.getMessage());
         }
      }
      if (myScripts == null) {
         myScripts = new AliasTable(); // default: an empty alias table
      }
      File[] files = ArtisynthPath.findFilesMatching (".*\\.py");
      Pattern pattern = null;
      try {
         pattern = Pattern.compile (".*ArtisynthScript:\\s*\"([^\"]+)\".*");
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1); 
      }
      for (int i=0; i<files.length; i++) {
         String scriptName = getScriptFileName (files[i], pattern);
         if (scriptName != null) {
            myScripts.addEntry (scriptName, files[i].getName());
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
         myMain.myFrame.setLocation(10, 10); // stay away from multiple monitor divide
         //myMain.myFrame.setFocusTraversalKeysEnabled(false);
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
      try {
         SwingUtilities.invokeAndWait(new ViewerResizer(myFrame, w, h));
      } catch (InvocationTargetException | InterruptedException e) {
         e.printStackTrace();
      }
   }
   
   public void setViewerSize (Dimension size) {
      // execute in AWT thread to prevent deadlock
      setViewerSize (size.width, size.height);
   }

   public Dimension getViewerSize() {
      return myViewer.getCanvas().getSize();
   }
   
   /**
    * Creates the new window frame
    * 
    * @param windowName name of window
    * @param width width in pixels
    * @param height height in pixels
    */
   public Main (String windowName, int width, int height, GLVersion glVersion) {
      myMain = this;
      
      setClassAliases();

      // check if GL3 version is supported
      if (width > 0 && glVersion == GLVersion.GL3) {
         GLVersionInfo vinfo = GLSupport.getMaxGLVersionSupported();
         if ( (vinfo.getMajorVersion() < myGLVersion.getMajorVersion()) ||
            ((vinfo.getMajorVersion() == myGLVersion.getMajorVersion()) && 
               (vinfo.getMinorVersion() < myGLVersion.getMinorVersion()))) {
            System.err.println("WARNING: " + glVersion.toString() + " is not supported on this system.");
            System.err.println("     Required: OpenGL " + glVersion.getMajorVersion() + "." + glVersion.getMinorVersion());
            System.err.println("     Available: OpenGL " + vinfo.getMajorVersion() + "." + vinfo.getMinorVersion());
            glVersion = GLVersion.GL2;
         }
      }
      myGLVersion = glVersion;
      
      if (demosFilename.value != null) {
         System.out.println ("reading demos files " + demosFilename.value);
         readDemoNames(demosFilename.value);
      } else {
         myDemoModels = new AliasTable(); // default: an empty alias table
      }
      readScriptNames();
      
      // potentially read model history
      if (historyFilename.value != null) {
         readModelHistory(historyFilename.value);
      }
      
      myEditorManager = new EditorManager (this);
      myUndoManager = new UndoManager();

      // need to create selection manager before MainFrame, because
      // some things in MainFrame will assume it exists
      mySelectionManager = new SelectionManager();


      if (width > 0) {
         ToolTipManager.sharedInstance().setLightWeightPopupEnabled (false);

         // execute in AWT thread to prevent deadlock
         try {
            SwingUtilities.invokeAndWait(new MainFrameConstructor(windowName, this, width, height));
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

         myViewerManager = new ViewerManager (myViewer);

         myViewerManager.setDefaultOrthographic (orthographic.value);
         myViewerManager.setDefaultDrawGrid (drawGrid.value);
         myViewerManager.setDefaultDrawAxes (drawAxes.value);
         myViewerManager.setDefaultAxisLength (axisLength.value);

         AxisAngle REW = getDefaultViewOrientation(getRootModel());
         myViewer.setDefaultAxialView (
            AxisAlignedRotation.getNearest (new RotationMatrix3d(REW)));
         initializeViewer (myViewer, REW);

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
         
         myFrame.getGLPanel().setSize (width, height);

         myPullController = new PullController (mySelectionManager);
         myAddMarkerHandler = new AddMarkerTool (mySelectionManager);
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
      myScheduler.setRealTimeAdvance (enable);
   }

   public void setFrameRate (double val) {
      if (val < 0) {
         throw new IllegalArgumentException ("frame rate must not be negative");
      }
      double secs;
      if (val == 0) {
         secs = Double.POSITIVE_INFINITY;
      }
      else {
         secs = 1/val;
      }
      getScheduler().getRenderProbe().setUpdateInterval (secs);
      framesPerSecond.value = val;
   }

   public double getFrameRate() {
      return framesPerSecond.value;
   }

   private AxisAngle getDefaultViewOrientation (RootModel root) {
      if (root != null) {
         AxisAngle REW = root.getDefaultViewOrientation();
         if (!REW.equals (new AxisAngle(0, 0, 0, 0))) {
            return REW;
         }
      }
      return myDefaultViewOrientation;
   }
   
   void setDefaultViewOrientation (AxisAngle REW) {
      myDefaultViewOrientation = new AxisAngle(REW);
   }

   public GLViewerFrame createViewerFrame() {
      GLViewerFrame frame =
         new GLViewerFrame (myViewer, PROJECT_NAME, 400, 400);

      GLViewer viewer = frame.getViewer();
      // ViewerToolBar toolBar = new ViewerToolBar(viewer, this);

      AxisAngle REW = getDefaultViewOrientation(getRootModel());
      myViewerManager.addViewer (viewer);
      ViewerToolBar toolBar = 
         new ViewerToolBar (viewer, /*addGridPanel=*/true);
      frame.getContentPane().add (toolBar, BorderLayout.PAGE_START);
      viewer.setDefaultAxialView (
         AxisAlignedRotation.getNearest (new RotationMatrix3d(REW)));
      initializeViewer (viewer, REW);
      frame.setVisible (true);
      return frame;
   }

   public void initializeViewer (GLViewer viewer, AxisAngle REW) {
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
         viewer.getCanvas().setCursor (
            Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
      }
      else {
         viewer.getCanvas().setCursor (getDefaultCursor());
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
         System.out.println ("Error: cannot open initialization "+fileOrUrl);
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
            "Warning: cannot find $ARTISYNTH_HOME/"+initFileName);
         stdFile = null;
      }
      else {
         executeJythonInit (stdFile);
      }
      File[] files = ArtisynthPath.findFiles (initFileName);
      if (files != null) {
         for (int i=files.length-1; i>=0; i--) {
            if (stdFile == null ||
               !ArtisynthPath.filesAreTheSame (files[i], stdFile)) {
               executeJythonInit (files[i]);
            }
         }
      }

   }

   public ArtisynthJythonConsole getJythonConsole() {
      return myJythonConsole;
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
   
   public Exception getSimulationException() {
      return myScheduler.getLastException();
   }

   public void step() {
      myScheduler.step();
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
      // this value mirrors rootModel.getMaxStepSize() ...
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
    * create the timeline
    * 
    */
   private void createTimeline() {
      
      if (getScheduler().isPlaying()) {
         getScheduler().stopRequest();
         System.out.println("waiting for stop");
         waitForStop();
      }
      
      TimelineController timeline = new TimelineController ("Timeline", this);
      
      
      mySelectionManager.addSelectionListener(timeline);
      timeline.setMultipleSelectionMask (
         myViewer.getMouseHandler().getMultipleSelectionMask());
      
      myTimeline = timeline;
      //}

      // add window listener to the timeline to catch window close events
      myTimeline.addWindowListener (new WindowAdapter() {
         public void windowClosed (WindowEvent e) {
         }

         public void windowClosing (WindowEvent e) {
            myMenuBarHandler.setTimelineVisible (false);
         }
      });

      // set the timeline frame sizes
      if (doubleTime) {
         myTimeline.setSize (800, 800);
      }
      else {
         myTimeline.setSize (800, 400);
      }

      if (timeLineRight) {
         myTimeline.setLocation (
            myFrame.getX()+myFrame.getWidth(), myFrame.getY());
      }
      else {
         myTimeline.setLocation (
            myFrame.getX(), myFrame.getY()+myFrame.getHeight());
      }

      // Check the model zoom level and set the timeline zoom
      // accordingly

      myTimeline.setZoomLevel (zoom.value);

      //       myTimeline.setSingleStepTime (
      //          TimeBase.secondsToTicks (stepSize.value));

      if (getWorkspace() != null) {
         myTimeline.requestResetAll();
      }
   }

   public void start (
      boolean startWithTimeline, boolean timeLineAllignedRight,
      boolean loadLargeTimeline) {

      timeLineRight = timeLineAllignedRight;
      doubleTime = loadLargeTimeline;

      myScheduler = new Scheduler(this);
      setMaxStep (maxStep.value);

      if (myFrame != null) {
         
         // Prevent deadlock by AWT thread
         try {
            SwingUtilities.invokeAndWait( new Runnable() {
               @Override
               public void run() {
                  myFrame.pack();
                  myFrame.setVisible (true);
                  createTimeline (); //TODO               
               }
            });
         } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
         }

         RenderProbe renderProbe =
            new RenderProbe (this, 1/framesPerSecond.value);
         // renderProbe.setMovieOptions(movieOptions);
         getScheduler().setRenderProbe (renderProbe);

         
         if (myTimeline instanceof TimelineController) {
            myScheduler.addListener ((TimelineController)myTimeline);
         }
         myScheduler.addListener (myMenuBarHandler);

         if (startWithTimeline) {
            myTimeline.setVisible (true);
            myMenuBarHandler.setTimelineVisible (true);
         }
         else {
            myMenuBarHandler.setTimelineVisible (false);
         }

         if (startWithJython.value) {
            myMenuBarHandler.setJythonConsoleVisible (true);
         }
      }
      else {
         myScheduler.setRealTimeAdvance (false); // no need if no viewer
         if (startWithJython.value) {
            createJythonConsole (/*useGui=*/false);
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
      myWorkspace.cancelRenderRequests();
      myModelName = null;
      myModelFile = null;
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
      mySelectionManager.clearSelections();
      myUndoManager.clearCommands();
      myWorkspace.removeDisposables();
      ArtisynthPath.setWorkingDir (null);
      // myWorkspace.getWayPoints().clear();
      // myWorkspace.clearInputProbes();
      // myWorkspace.clearOutputProbes();
      if (myViewerManager != null) {
         myViewerManager.clearRenderables();
      }
      getWorkspace().setRootModel (null);
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
      RootModel newRoot, String modelName, String[] modelArgs) {
      
      myModelName = modelName;
      if (modelArgs != null) {
         myModelArgs = Arrays.copyOf (modelArgs, modelArgs.length);
      }
      else {
         myModelArgs = null;
      }
      //CompositeUtils.testPaths (newRoot);

      double maxStepSize = newRoot.getMaxStepSize();
      if (maxStepSize == -1) {
         // if RootModel maxStepSize is undefined, set it to the default value
         maxStepSize = maxStep.value;
         newRoot.setMaxStepSize (myMaxStep);
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
         myViewerManager.resetViewers (getDefaultViewOrientation(newRoot));
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
         
         myTimeline.automaticProbesZoom();

      }
      //
      // Add this now since we don't want Main.componentChanged() being called
      // while the model is being built. 
      // 
      newRoot.addComponentChangeListener (this);

      // Component change listener is now active, and so will be called in
      // response to any change events that are dispatched within attach().
      newRoot.attach (this);
   }

   private class LoadModelRunner implements Runnable {
      String myModelName;
      String[] myModelArgs;
      String myClassName;
      boolean myStatus = false;

      LoadModelRunner (String className, String modelName, String[] modelArgs) {
         myClassName = className;
         myModelName = modelName;
         myModelArgs = modelArgs;
      }

      public void run() {
         myStatus = doLoadModel (myClassName, myModelName, myModelArgs);
      }

      public boolean getStatus() {
         return myStatus;
      }
   }

   private class LoadModelFileRunner implements Runnable {
      File myFile;
      boolean myStatus = false;

      LoadModelFileRunner (File file) {
         myFile = file;
      }

      public void run() {
         try {
            myStatus = loadModelFile(myFile);
         } catch (IOException e) {
            e.printStackTrace();
            myStatus = false;
         }
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
      try {
         RootModel newRoot = null;
         Method method = demoClass.getMethod ("build", String[].class);
         if (demoClass == RootModel.class ||
             method.getDeclaringClass() != RootModel.class) {
            //System.out.println (
            // "constructing model with build method ...");
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
         myErrMsg = " class " + demoClass.getName() + " cannot be instantiated";
         if (e.getMessage() != null) {
            myErrMsg += ": \n" + e.getMessage();
         }
         e.printStackTrace();
         return null;
      }
   }

   // Entry-point for loading model.  If successful, add to history
   // if available.
   public boolean loadModel(ModelInfo info) {
     
      boolean success = false;
      
      if (info.getType() == ModelType.FILE) {
         try {
            success = loadModelFile(new File(info.getClassNameOrFile()));
         } catch (IOException e) {
            e.printStackTrace();
            return false;
         }
      } else {
         success = doLoadModel(info.getClassNameOrFile(), info.getShortName(),
            info.getArgs());
      }
      
      if (success) {
         if (myModelHistory != null) {
            myModelHistory.update(info, 
               new Date(System.currentTimeMillis()));
            if (historyFilename.value != null) {
               try {
                  // XXX save every time?  Can't seem to get it to save on exit
                  myModelHistory.save(new File(historyFilename.value));
               } catch (IOException e) {
                  // e.printStackTrace ();
               }
            }
            if (myMenuBarHandler != null) {
               myMenuBarHandler.updateHistoryMenu();
            }
         }
         lastModelInfo = info;
      }
      
      return success;
   }
   
   // Entry-point for loading model.  If successful, add to history
   // if available.
   public boolean loadModel(
      String className, String modelName, String[] modelArgs) {

      return loadModel(new ModelInfo(className, modelName, modelArgs));
   }
   
   private boolean doLoadModel (
      String className, String modelName, String[] modelArgs) {

      // If we are not in the AWT event thread, switch to that thread
      // and build the model there. We do this because the model building
      // code may create and access GUI components (such as ControlPanels)
      // and any such code must execute in the event thread. Typically,
      // we will not be in the event thread if loadModel is called from
      // the Jython console.
      if (myViewer != null && !SwingUtilities.isEventDispatchThread()) {
         LoadModelRunner runner =
            new LoadModelRunner (className, modelName, modelArgs);
         if (!runInSwing (runner)) {
            return false;
         }
         else {
            return runner.getStatus();
         }
      }
      Class<?> demoClass = null;
      RootModel newRoot = null;
      try {
         demoClass = Class.forName (className);
      }
      catch (ClassNotFoundException e) {
         myErrMsg = "class " + className + " not found";
         e.printStackTrace();
         return false;
      }
      catch (Exception e) {
         if (className==null) {
            myErrMsg = " model " + modelName + " cannot be initialized";
         } else {
            myErrMsg = " class " + className + " cannot be initialized";
         }
         e.printStackTrace();
         return false;
      }

      // if the model name equals the class name, adjust the model name
      // to remove dots since they aren't allowed in names
      if (className.equals (modelName)) {
         modelName = demoClass.getSimpleName();
      } else if (modelName.contains(".")) {
         String[] splitNames = modelName.split("\\.");
         modelName = splitNames[splitNames.length-1];  // get last item
      }
      
      clearRootModel();
      // Sanchez, July 11, 2013
      // Remove model and force repaint to clean the display.  
      // This is done so we can render
      // objects like an HUD while a new model is loading
      if (myViewerManager != null) {
         myViewerManager.clearRenderables();
         myViewerManager.render();         // refresh the rendering lists
      }
            
      // getWorkspace().getWayPoints().clear();
      int numLoads = 1; // set to a large number for testing memory leaks
      for (int i = 0; i < numLoads; i++) {
         newRoot = createRootModel (demoClass, modelName, modelArgs);
         if (newRoot == null) {
            // load empty model since some state info from existing model 
            // has been cleared, causing it to crash
            doLoadModel (
               "artisynth.core.workspace.RootModel", "EmptyModel", modelArgs);
            if (myViewerManager != null) {
               myViewerManager.render();
            }
            return false;
         }
         setRootModel (newRoot, modelName, modelArgs);
         if (numLoads > 1) {
            System.out.println ("load instance " + i);
         }
      }

      if (myFrame != null) {
         // Sanchez, July 11, 2013
         // force repaint again, updating viewer bounds to reflect
         // new renderables
         myViewerManager.render();      // set external render lists

         // when a model is loaded reset the viewer so no view is selected
         //setViewerMode (null);

         // remove the selected item in the selectComponentPanel otherwise the
         // text for that selected component remains when a new model is loaded
         myFrame.getSelectCompPanelHandler().clear();
         myFrame.getSelectCompPanelHandler().setComponentFilter (null);
         myFrame.setBaseTitle ("ArtiSynth " + modelName);
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
      ArrayList<MouseBindings> allBindings = getAllMouseBindings();
      for (int i=0; i<allBindings.size(); i++) {
         if (bindingsName.equalsIgnoreCase(allBindings.get(i).getName())) {
            bindings = allBindings.get(i);
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
      myViewerManager.setMouseBindings (bindings);
   }

   public MouseBindings getMouseBindings () {
      return myViewerManager.getMouseBindings ();
   }

   public double getMouseWheelZoomScale() {
      return myViewerManager.getMouseWheelZoomScale();
   }

   public void setMouseWheelZoomScale (double scale) {
      myViewerManager.setMouseWheelZoomScale (scale);
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

   protected static IntHolder viewerWidth = new IntHolder(720);
   protected static IntHolder viewerHeight = new IntHolder(540);
   protected static BooleanHolder printHelp = new BooleanHolder (false);
   protected static BooleanHolder fullScreen = new BooleanHolder (false);
   protected static BooleanHolder yup = new BooleanHolder (false);
   protected static BooleanHolder drawAxes = new BooleanHolder (false);
   protected static BooleanHolder drawGrid = new BooleanHolder (false);
   protected static StringHolder axialView = new StringHolder("xz");
   protected static BooleanHolder orthographic = new BooleanHolder (false);
   protected static BooleanHolder startWithTimeline = new BooleanHolder (true);
   protected static BooleanHolder startWithJython = new BooleanHolder (false);
   protected static BooleanHolder timelineRight = new BooleanHolder (false);
   protected static BooleanHolder largeTimeline = new BooleanHolder (false);
   protected static BooleanHolder printOptions = new BooleanHolder (false);
   protected static IntHolder zoom = new IntHolder (1);
   protected static DoubleHolder axisLength = new DoubleHolder (-1);
   protected static DoubleHolder framesPerSecond = new DoubleHolder (20);
   protected static DoubleHolder maxStep = new DoubleHolder (0.01);
   protected static StringHolder modelName = new StringHolder();
   protected static BooleanHolder play = new BooleanHolder();
   protected static DoubleHolder playFor = new DoubleHolder();
   protected static BooleanHolder exitOnBreak = new BooleanHolder();
   protected static BooleanHolder updateLibs = new BooleanHolder();
   protected static StringHolder demosFilename = new StringHolder();
   protected static StringHolder demosMenuFilename =
      new StringHolder("demoMenu.xml");
   protected static StringHolder historyFilename = new StringHolder(
      ArtisynthPath.getCacheDir () + "/.history");
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
   protected static StringHolder posCorrection =
      new StringHolder ("Default");

   protected static BooleanHolder noIncompressDamping =
      new BooleanHolder (false);
   // protected static BooleanHolder useOldTimeline = 
   //    new BooleanHolder (false);
   protected static BooleanHolder useSignedDistanceCollider = 
      new BooleanHolder (false);
   protected static BooleanHolder useAjlCollision =
      new BooleanHolder (false);
   protected static BooleanHolder useBodyVelsInSolve =
      new BooleanHolder (false);
   protected static BooleanHolder useArticulatedTransforms =
      new BooleanHolder (false);
   protected static BooleanHolder noGui = new BooleanHolder (false);
   protected static IntHolder glVersion = new IntHolder (3);
   protected static BooleanHolder useGLJPanel = new BooleanHolder (true);
   protected static StringHolder logLevel = 
      new StringHolder(Logger.LogLevel.WARN.toString());
   protected static BooleanHolder testSaveRestoreState =
      new BooleanHolder (false);

   protected static DoubleHolder movieFrameRate = new DoubleHolder (-1);
   protected static StringHolder movieMethod = new StringHolder ();

   protected static IntHolder flags = new IntHolder();

   protected static StringHolder mousePrefs = new StringHolder(); // "kees"

   protected static float[] bgColor = new float[3];
   protected static BooleanHolder openMatlab = new BooleanHolder(false);

   protected static StringHolder matrixSolver = new StringHolder();

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
         boolean allOK = true;
         try {
            allOK = installer.verifyNativeLibs (update);
         }
         catch (Exception e) {
            System.out.println ("Main");
            if (installer.isConnectionException (e)) {
               System.out.println (e.getMessage());
            }
            else {
               e.printStackTrace(); 
            }
            System.exit(1);
         }
         if (!allOK) {
            System.out.println (
               "Error: can't find or install all required native libraries");
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
      parser.addOption ("-width %d #width (pixels)", viewerWidth);
      parser.addOption ("-height %d #height (pixels)", viewerHeight);
      parser.addOption (
         "-bgColor %fX3 #background color (3 rgb values, 0 to 1)", bgColor);
      parser.addOption (
         "-maxStep %f #maximum time for a single step (sec)", maxStep);
      parser.addOption ("-drawAxes %v #draw coordinate axes", drawAxes);
      parser.addOption ("-drawGrid %v #draw grid", drawGrid);
      parser.addOption ("-axisLength %f #coordinate axis length", axisLength);
      parser.addOption ("-play %v #play model immediately", play);
      parser.addOption (
         "-playFor %f #play model immediately for x seconds", playFor);
      parser.addOption (
         "-exitOnBreak %v #exit artisynth when playing stops", exitOnBreak);
      parser.addOption (
         "-demosFile %s #demo file (e.g. .demoModels)", demosFilename);
      parser.addOption(
         "-demosMenu %s #demo menu file (e.g. .demoMenu.xml)", demosMenuFilename);
      parser.addOption(
         "-historyFile %s #model history file (e.g. .history)", historyFilename);
      parser.addOption (
         "-scriptsFile %s #script file (e.g. .artisynthModels)",scriptsFilename);
      parser.addOption (
         "-mousePrefs %s #kees for pure mouse controls", mousePrefs);
      parser.addOption ("-ortho %v #use orthographic viewing", orthographic);
      parser.addOption (
         "-axialView %s{xy,xz} #initial view of x-y or x-z axes", axialView);
      parser.addOption (
         "-noTimeline %v{false} #do not start with a timeline",
         startWithTimeline);
      parser.addOption (
         "-showJythonConsole %v{true} #create jython console on startup",
         startWithJython);
      parser.addOption (
         "-largeTimeline %v{true} #start with vertically expanded timeline",
         largeTimeline);
      parser.addOption (
         "-timelineRight %v{true} #start with a timeline alligned to the right",
         timelineRight);
      parser.addOption ("-fps %f#frames per second", framesPerSecond);
      parser.addOption ("-fullscreen %v #full screen renderer", fullScreen);
      // parser.addOption("-yup %v #initialize viewer with Y-axis up", yup);
      parser.addOption ("-timelineZoom %d #zoom level for timeline", zoom);
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
         "-posCorrection %s{Default,GlobalMass,GlobalStiffness} "+
            "#position correction mode",
            posCorrection);

      parser.addOption (
         "-noIncompressDamping %v #ignore incompress stiffness for damping",
         noIncompressDamping);
      // parser.addOption ("-useOldTimeline %v #use old timeline", useOldTimeline);
      parser.addOption (
         "-useSignedDistanceCollider %v "+
         "#use SignedDistanceCollider where possible", 
         useSignedDistanceCollider);
      parser.addOption ("-useAjlCollision" +
         "%v #use AJL collision detection", useAjlCollision);
      parser.addOption ("-useBodyVelsInSolve" +
         "%v #use body velocities for dynamic solves", useBodyVelsInSolve);
      parser.addOption (
         "-useArticulatedTransforms %v #enforce articulation " +
            "constraints with transformers", useArticulatedTransforms);
      parser.addOption (
         "-updateLibs %v #update libraries from ArtiSynth server", updateLibs);
      parser.addOption ("-flags %x #flag bits passed to the application", flags);
      parser.addOption ("-noGui %v #run ArtiSynth without the GUI", noGui);
      parser.addOption (
         "-openMatlabConnection %v " +
         "#open a MATLAB connection if possible", openMatlab);
      parser.addOption (
         "-GLVersion %d{2,3} " + "#version of openGL for graphics", glVersion);
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
         "-movieFrameRate %f #frame rate to use when making movies",
         movieFrameRate);
      parser.addOption (
         "-movieMethod %s #method to use when making movies",
         movieMethod);
      parser.addOption (
         "-waypoints %s # specifies a waypoints file to load",
         wayPointsFile);
      
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
               System.err.println (e.getMessage());
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
               modelName.value = pargs[++idx];
               modelArgs = new ArrayList<String>();
               int nidx = maybeCollectArgs(pargs, ++idx, modelArgs);
               if (nidx == idx) {
                  modelArgs = null;
               }
               idx = nidx;
            }
            else if ("-script".equals(pargs[pidx])) {
               scriptFile.value = pargs[++idx];
               scriptArgs = new ArrayList<String>();
               int nidx = maybeCollectArgs(pargs, ++idx, scriptArgs);
               if (nidx == idx) {
                  scriptArgs = null;
               }
               idx = nidx;
            }
            else if ("-taskManager".equals(pargs[pidx])) {
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
               "Error parsing options: "+ e.getMessage());
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

      // Set system logger level
      Logger.getSystemLogger().setLogLevel(LogLevel.find(logLevel.value));
      
      MechSystemSolver.myDefaultHybridSolveP = !disableHybridSolves.value;
      if (numSolverThreads.value > 0) {
         PardisoSolver.setDefaultNumThreads (numSolverThreads.value);
      }

      if (matrixSolver.value != null) {
         MechSystemBase.setDefaultMatrixSolver (
            SparseSolverId.valueOf (matrixSolver.value));
      }
      
      FemModel3d.abortOnInvertedElems = abortOnInvertedElems.value;
      //      if (posCorrection.value.equals ("Default")) {
      //         MechSystemBase.setDefaultStabilization (PosStabilization.Default);
      //      }
      if (posCorrection.value.equals("GlobalMass")) {
         MechSystemBase.setDefaultStabilization (PosStabilization.GlobalMass);
      }
      else if (posCorrection.value.equals("GlobalStiffness")) {
         MechSystemBase.setDefaultStabilization (PosStabilization.GlobalStiffness);
      }
      FemModel3d.noIncompressStiffnessDamping = noIncompressDamping.value;

      if (useAjlCollision.value) {
         CollisionManager.setDefaultColliderType (ColliderType.AJL_CONTOUR);
      }
      if (testSaveRestoreState.value) {
         RootModel.testSaveAndRestoreState = true;
      }

      if (useGLJPanel.value == true) {
         maspack.render.GL.GLViewer.useGLJPanel = true;
      }

      if (noGui.value == true) {
         viewerWidth.value = -1;
      }
      GLVersion glv = (glVersion.value == 3 ? GLVersion.GL3 : GLVersion.GL2);
      Main m = new Main (
         PROJECT_NAME, viewerWidth.value, viewerHeight.value, glv);

      m.setArticulatedTransformsEnabled (useArticulatedTransforms.value);
      if (axialView.value.equals ("xy")) {
         m.setDefaultViewOrientation (AxisAngle.IDENTITY);
      }
      else if (axialView.value.equals ("xz")) {
         m.setDefaultViewOrientation (AxisAngle.ROT_X_90);
      }
      else {
         throw new InternalErrorException (
            "Unknown axial view: " + axialView.value);
      }

      if (m.myFrame != null) {
         m.myViewerManager.setBackgroundColor (
            new Color (bgColor[0], bgColor[1], bgColor[2]));
         //m.myViewer.setBackgroundColor (bgColor[0], bgColor[1], bgColor[2]);
         // XXX this should be done in the Main constructor, but needs
         // to be done here instead because of sizing effects
         m.myMenuBarHandler.initToolbar();

         if (movieMethod.value != null) {
            MovieMaker movieMaker = m.getMovieMaker();
            try {
               movieMaker.setMethod (movieMethod.value);
            }
            catch (Exception e) {
               System.out.println (
                  "Warning: unknown movie making method " + movieMethod.value);
            }
         }
         if (movieFrameRate.value != -1) {
            MovieMaker movieMaker = m.getMovieMaker();
            try {
               movieMaker.setFrameRate (movieFrameRate.value);
            }
            catch (Exception e) {
               System.out.println (
                  "Warning: illegal movie frame rate " + movieFrameRate.value);
            }
         }
      }

      if (mousePrefs.value != null && m.myViewer != null) {
         m.setMouseBindings (mousePrefs.value);
      }
      m.setFlags (flags.value);
      if (useBodyVelsInSolve.value) {
         Frame.dynamicVelInWorldCoords = false;
      }

      m.start (
         startWithTimeline.value, timelineRight.value, largeTimeline.value);
      // need to set viewer size here, after it has become visible,
      // because setting it earlier can cause incorrect results     
      if (m.myFrame != null) {
         m.setViewerSize (viewerWidth.value, viewerHeight.value);
      }

      if (System.getProperty ("os.name").contains ("Windows")) {
         fixPardisoThreadCountHack(); // XXX see function docs
      }
      
      m.verifyNativeLibraries (updateLibs.value);

      // we put.setOrthographicView *after* start because otherwise it sets up
      // some sort of race condition when trying to set the
      // perspectve/orthogonal menu item while we are setting
      // the whole frame to be visible
      if (m.myFrame != null && orthographic.value) {
         m.getViewer().setOrthographicView (true);
      }

      // XXX store model arguments for future use?
      if (modelArgs != null) {
         m.myModelArgs = modelArgs.toArray(new String[modelArgs.size()]);
      }
      
      if (modelName.value != null) {
         // load the specified model. See first if the name corresponds to a file
         File file = new File (modelName.value);
         if (file.exists() && file.canRead()) {
            try {
               m.loadModelFile (file);
            }
            catch (IOException e) {
               System.out.println (
                  "Error reading or loading model file " + modelName.value);
               e.printStackTrace(); 
               m.setRootModel (new RootModel(), null, null);
            }
         }
         // otherwise, try to determine the model from the class name or alias
         else {
            String className = m.getDemoClassName (modelName.value);
            if (className == null) {
               System.out.println ("No class associated with model name "
                                   + modelName.value);
               m.setRootModel (new RootModel(), null, null);
            }
            else {
               String name = modelName.value;
               if (name.indexOf ('.') != -1) {
                  name = name.substring (name.lastIndexOf ('.') + 1);
                  if (name.length() == 0) {
                     name = "Unknown";
                  }
               }
               // load the model
               ModelInfo mi = new ModelInfo (
                  className, name, createArgArray(modelArgs));
               m.loadModel (mi);
            }
         }
      }
      else {
         m.setRootModel (new RootModel(), null, null);
      }

      if (wayPointsFile.value != null) {
         try {
            m.loadWayPoints (new File(wayPointsFile.value));
         }
         catch (IOException e) {
            e.printStackTrace(); 
         }
      }

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
         m.runScriptFile (scriptFile.value, scriptArgs);
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
   }

   private static String[] createArgArray (ArrayList<String> args) {
      if (args == null) {
         return new String[0];
      }
      else {
         return args.toArray(new String[0]);
      }
   }

   void runScriptFile (String fileName, ArrayList<String> args) {
         
      if (myFrame != null) {
         myMenuBarHandler.runScript(fileName, createArgArray(args));
      }
      else {
         if (myJythonConsole == null) {
            createJythonConsole (/*guiBased=*/false);
         }
         try {
            myJythonConsole.executeScript (fileName, createArgArray(args));
         }
         catch (Exception e) {
            System.out.println (
               "Error executing script '"+fileName+"':");
            System.out.println (e);
         }
      }
   }

   void runTaskManager (String className, ArrayList<String> args) {

      Class taskClass = null;
      try {
         taskClass = Class.forName (className);
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

   public int getFlags() {
      return myFlags;
   }

   public void setFlags(int flags) {
      myFlags = flags;
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

   public boolean loadModelFile (File file) throws IOException {
      // If we are not in the AWT event thread, switch to that thread
      // and build the model there. We do this because the model building
      // code may create and access GUI components (such as ControlPanels)
      // and any such code must execute in the event thread. Typically,
      // we will not be in the event thread if loadModel is called from
      // the Jython console.
      if (myViewer != null && !SwingUtilities.isEventDispatchThread()) {
         LoadModelFileRunner runner = new LoadModelFileRunner (file);
         if (!runInSwing (runner)) {
            return false;
         }
         else {
            return runner.getStatus();
         }
      }

      RootModel newRoot = null;
      clearRootModel();
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
      // getWorkspace().getWayPoints().clear();
      long t0 = System.nanoTime();
      ScanWriteUtils.scanfull (rtok, newRoot, newRoot);
      long t1 = System.nanoTime();
      System.out.println ("File scan time: " + ((t1-t0)*1e-9) + " sec");
      System.out.println ("File size: " + file.length());
      //System.out.println ("queue size=" + ModelComponentBase.scanQueueSize());
      rtok.close();
      
//      WayPoint way0 = get(0);
//      if (way0.isValid()) {
//         // waypoint contains scanned state information. Need to add
//         // augmentation information to this state.
//         CompositeState scannedState = way0.getState();
//         way0.setState (myRootModel);
//         way0.getState().set (scannedState);
//      }
      
      String modelName = newRoot.getName();
      if (modelName == null) { // use file name with extension stripped off
         modelName = file.getName();
         int dotIdx = modelName.indexOf ('.');
         if (dotIdx != -1) {
            modelName = modelName.substring (0, dotIdx);
         }
      }
      setRootModel (newRoot, modelName, null);
      return true;
   }
   
   public void reloadModel() throws IOException {
      if (myModelFile != null) {
         loadModelFile (myModelFile);
      }
      else if (lastModelInfo != null) {
         RootModel root = getRootModel();
         if (root != null) {
            Class<?> rootClass = root.getClass();
            String name = myModelName;
            if (name == null) {
               name = root.getName();
            }
            if (name == null) {
               name = "ArtiSynth";
            }
            loadModel(lastModelInfo);
         }
      }
   }

   boolean modelIsLoaded() {
      return myModelFile != null || lastModelInfo != null;
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
         myTimeline.automaticProbesZoom();
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
         System.out.println ("waypoints file path=" + relOrAbsPath);
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
         if (historyFilename.value != null) {
            saveModelHistory(historyFilename.value);
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
                     getDefaultViewOrientation(getRootModel()));
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

   PullController getPullController() {
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

      @SuppressWarnings("unchecked")
      private void createTransformCommand (Dragger3dEvent e) {
         Dragger3dBase dragger = (Dragger3dBase)e.getSource();
         String name = null;
         if (dragger instanceof Translator3d ||
            dragger instanceof ConstrainedTranslator3d) {
            name = "translation";
         }
         else if (dragger instanceof Rotator3d) {
            name = "rotation";
         }
         else if (dragger instanceof RotatableScaler3d) {
            name = "scaling";
         }
         else if (dragger instanceof Transrotator3d) {
            name = "transrotation";
         }
         else {
            throw new InternalErrorException (
               "transform command unimplemented for " + dragger.getClass());
         }
         
         // indicate that dragger is responsible for transforming, and 
         // allow auto-flipping of orientation for negative-determinant transforms
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
   }

   private static final double inf = Double.POSITIVE_INFINITY;

   protected double computeDraggerToWorld (
      RigidTransform3d TDW, List<ModelComponent> draggables,
      Dragger3dBase dragger) {

      double radius = 0;
      if (dragger != null) {
         TDW.set (dragger.getDraggerToWorld());
      }
      HasCoordinateFrame singleCompWithFrame = null;
      if (draggables.size() == 1 &&
          draggables.get(0) instanceof HasCoordinateFrame) {
         singleCompWithFrame = (HasCoordinateFrame)draggables.get(0);
      }
      Point3d pmin = null;
      Point3d pmax = null;
      if (dragger == null || singleCompWithFrame == null) {
         // need to compute bounds if there is no dragger (to determine
         // radius), or if there is no single component with a frame (to
         // determine the transform).
         pmin = new Point3d (inf, inf, inf);
         pmax = new Point3d (-inf, -inf, -inf);
         for (ModelComponent c : draggables) {
            ((Renderable)c).updateBounds (pmin, pmax);
         }
         radius = pmin.distance (pmax);
      }
      if (singleCompWithFrame != null) {
         singleCompWithFrame.getPose (TDW);
         if (dragger == null && getInitDraggersInWorldCoords()) {
            TDW.R.setIdentity();
         }
      }
      else {
         TDW.p.add (pmin, pmax);
         TDW.p.scale (0.5);
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
               "Error connecting to MATLAB: " + e.getMessage());
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
