/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC) and ArtiSynth
 * Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import javax.swing.*;

import maspack.collision.SurfaceMeshCollider;
import maspack.geometry.ConstrainedTranslator3d;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.GeometryTransformer.UndoState;
import maspack.graph.Tree;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.GL.GLMouseAdapter;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewer.GLVersion;
import maspack.render.GL.GLViewerFrame;
import maspack.render.Renderer.SelectionHighlighting;
import maspack.util.*;
import maspack.solvers.PardisoSolver;
import maspack.widgets.ButtonMasks;
import maspack.widgets.PropertyWindow;
import maspack.widgets.RenderPropsDialog;
import maspack.widgets.ViewerToolBar;
import maspack.widgets.ViewerKeyListener;
import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.DoubleHolder;
import argparser.IntHolder;
import argparser.StringHolder;
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
import artisynth.core.mechmodels.MechSystemSolver.PosStabilization;
import artisynth.core.modelbase.*;
import artisynth.core.probes.WayPoint;
import artisynth.core.probes.Probe;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.moviemaker.MovieMaker;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.PullController;
import artisynth.core.workspace.RenderProbe;
import artisynth.core.workspace.RootModel;
import artisynth.core.workspace.Workspace;
import artisynth.core.driver.ModelInfo.ModelType;
import artisynth.core.driver.Scheduler.Action;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;

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
   protected InverseManager myInverseManager;
   protected MovieMaker myMovieMaker;

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
   protected ModelHistory myModelHistory = null;              // storage for model history
   protected AliasTable myScripts;
   protected final static String PROJECT_NAME = "ArtiSynth";
   protected MatlabInterface myMatlabConnection;

   protected String myModelName;
   protected String[] myModelArgs;    // command-line supplied arguments
   protected ModelInfo lastModelInfo; // for re-loading a model with the same parameters
   protected int myFlags = 0;
   protected double myMaxStep = -1;
   protected boolean disposed = false;

   protected GLVersion myGLVersion = GLVersion.GL2;

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
      Translate,
      Rotate,
      ConstrainedTranslate,
      Transrotate,
      Pull,
      AddComponent
   }

   private SelectionMode mySelectionMode;
   private boolean myArticulatedTransformsP = true;
   private boolean myInitDraggersInWorldCoordsP = false;

//   public enum ViewerMode {
//      FrontView, TopView, SideView, BottomView, RightView, BackView
//   }
//
//   private ViewerMode viewerMode;

   // protected boolean myOrthographicP;
   // private double cpuLoad = .8;

   private File myProbeFile = null;
   private File myModelFile = null;

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
      this (PROJECT_NAME, 800, 600, GLVersion.GL2);
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
         System.out.println ("Warning: history file '" + filename + "' not found");
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
   
   /**
    * to create the new window frame
    * 
    * @param windowName
    * @param width
    * @param height
    */

   public Main (String windowName, int width, int height, GLVersion glVersion) {
      myMain = this;

      myGLVersion = glVersion;

      if (demosFilename.value != null) {
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
      myInverseManager = new InverseManager(this);

      // need to create selection manager before MainFrame, becuase
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

         AxisAngle REW = getDefaultViewOrientation();
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
         
         // execute in AWT thread to prevent deadlock
         try {
            SwingUtilities.invokeAndWait(new ViewerResizer(myFrame, width, height));
         } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
         }
         
         myPullController = new PullController (mySelectionManager);
      }
      createWorkspace();
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

   private AxisAngle getDefaultViewOrientation () {
      AxisAngle REW = AxisAngle.ROT_X_90;
      if (getRootModel() != null) {
         REW = getRootModel().getDefaultViewOrientation();
      }
      return REW;
   }

   public GLViewerFrame createViewerFrame() {
      GLViewerFrame frame =
         new GLViewerFrame (myViewer, PROJECT_NAME, 400, 400);

      GLViewer viewer = frame.getViewer();
      // ViewerToolBar toolBar = new ViewerToolBar(viewer, this);

      AxisAngle REW = getDefaultViewOrientation();
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
         viewer.getCanvas().setCursor (Cursor.getDefaultCursor());
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

   public void rewind() {
      myScheduler.rewind();
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

   public void step() {
      myScheduler.step();
   }

   public void forward() {
      myScheduler.fastForward();
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
      
      TimelineController timeline = 
         new TimelineController ("Timeline", this, myViewer);
      
      
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
      RootModel rootModel = getRootModel();
      if (rootModel != null) {
         rootModel.detach (this);
         rootModel.removeComponentChangeListener (this);
         rootModel.setMainViewer (null); // just for completeness
         rootModel.removeController (myPullController); // 
         rootModel.dispose();
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

      //newRoot.addComponentChangeListener (this);
      getWorkspace().setRootModel (newRoot);
      // mainViewer should already be set if constructed with build() method:
      newRoot.setMainViewer (myViewer); 
      myViewer.clearClipPlanes();

      if (myFrame != null) {
         myMenuBarHandler.enableShowPlay();
         myMenuBarHandler.updateModelButtons();

         if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater (new Runnable() {
                  public void run() {
                     myFrame.updateNavBar();
                  }
               });
         }
         else {
            myFrame.updateNavBar();
         }

         // reset all the viewers
         myViewerManager.clearRenderables();
         myViewerManager.resetViewers (newRoot.getDefaultViewOrientation());
      }

      // model scheduler initialization called within initialize
      getScheduler().initialize();
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

         if (mySelectionMode == SelectionMode.Pull) {
            myPullController.clear();
            newRoot.addController (
               myPullController, findMechSystem(newRoot));
         }
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
            try {
               // XXX save every time?  Can't seem to get it to save on exit
               myModelHistory.save(new File(historyFilename.value));
            } catch (IOException e) {
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
     
      //      boolean success = doLoadModel(className, modelName, modelArgs);
      //      if (success) {
      //         if (myModelHistory != null) {
      //            myModelHistory.update(new ModelInfo(className, modelName, modelArgs), new Date());
      //            // XXX save every time?  Can't seem to get it to save on exit
      //            try {
      //               myModelHistory.save(new File(historyFilename.value));
      //            } catch (IOException e) {
      //            }
      //         }
      //      }
      //      
      //      return success;
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
         modelName = splitNames[splitNames.length-1];	// get last item
      }
      
      clearRootModel();
      // Sanchez, July 11, 2013
      // Remove model and force repaint to clean the display.  
      // This is done so we can render
      // objects like an HUD while a new model is loading
      if (myViewerManager != null) {
         myViewerManager.clearRenderables();
         myViewerManager.render();	    // refresh the rendering lists
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
         myViewerManager.render();	 // set external render lists

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

   /**
    * set the mouse bindings
    * 
    * @param prefs
    */
   public void setMouseBindings (String prefs) {

      System.out.println ("Setting mouse bindings to '"+prefs+"'");
      GLMouseAdapter mouse = (GLMouseAdapter)myViewer.getMouseHandler();
      if (mouse == null) {
         mouse = new GLMouseAdapter(myViewer);
         myViewer.setMouseHandler(mouse);
      }
      
      if (prefs.equalsIgnoreCase ("kees")) {
         
         mouse.setRotateButtonMask (InputEvent.BUTTON1_DOWN_MASK);
         mouse.setTranslateButtonMask (
            InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
         mouse.setZoomButtonMask (
            InputEvent.BUTTON1_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
         
         mouse.setSelectionButtonMask (
            InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
         mouse.setMultipleSelectionMask(InputEvent.CTRL_DOWN_MASK);
         // mouse.setDragSelectionMask(InputEvent.SHIFT_DOWN_MASK);
         
         mouse.setDraggerConstrainMask(MouseEvent.SHIFT_DOWN_MASK);
         mouse.setDraggerDragMask(InputEvent.BUTTON1_DOWN_MASK);
         mouse.setDraggerRepositionMask(InputEvent.BUTTON1_DOWN_MASK | 
            InputEvent.ALT_DOWN_MASK);
      }
      else if (prefs.equalsIgnoreCase("laptop")) {
         mouse.setLaptopConfig();
      }
      else if (prefs.equalsIgnoreCase ("mac")) {
         
         // setup button masks for macbook trackpad
         ButtonMasks.setContextMenuMask (
            (InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)); 
         
         // right mouse button = CTRL + BUTTON
         mouse.setRotateButtonMask ( // middle mouse = ALT + BUTTON
            InputEvent.BUTTON1_DOWN_MASK | InputEvent.ALT_DOWN_MASK); 
         mouse.setTranslateButtonMask (
            InputEvent.BUTTON1_DOWN_MASK | InputEvent.ALT_DOWN_MASK |
            InputEvent.SHIFT_DOWN_MASK);
         mouse.setZoomButtonMask (
            InputEvent.BUTTON1_DOWN_MASK | InputEvent.ALT_DOWN_MASK |
            InputEvent.META_DOWN_MASK);
         
         mouse.setSelectionButtonMask(InputEvent.BUTTON1_DOWN_MASK);
         mouse.setMultipleSelectionMask ((InputEvent.META_DOWN_MASK));
         // mouse.setDragSelectionMask(InputEvent.SHIFT_DOWN_MASK);
         
         mouse.setDraggerConstrainMask(MouseEvent.SHIFT_DOWN_MASK);
         mouse.setDraggerDragMask(InputEvent.BUTTON1_DOWN_MASK);
         mouse.setDraggerRepositionMask(InputEvent.BUTTON1_DOWN_MASK 
            | InputEvent.CTRL_DOWN_MASK);
         
      } else if (prefs.equalsIgnoreCase("default")) {
         
         mouse.setRotateButtonMask(InputEvent.BUTTON2_DOWN_MASK);
         mouse.setTranslateButtonMask(
            InputEvent.BUTTON2_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
         mouse.setZoomButtonMask(
            InputEvent.BUTTON2_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
         
         mouse.setSelectionButtonMask(InputEvent.BUTTON1_DOWN_MASK);
         mouse.setMultipleSelectionMask(InputEvent.CTRL_DOWN_MASK);
         // mouse.setDragSelectionMask(InputEvent.SHIFT_DOWN_MASK);
         
         mouse.setDraggerConstrainMask(MouseEvent.SHIFT_DOWN_MASK);
         mouse.setDraggerDragMask(InputEvent.BUTTON1_DOWN_MASK);
         mouse.setDraggerRepositionMask(InputEvent.BUTTON1_DOWN_MASK 
            | InputEvent.CTRL_DOWN_MASK);
         
      }
      else {
         System.out.println ("unknown mouse bindings: " + prefs);
         System.out.println ("unknown mouse bindings: " + prefs);
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

   protected static BooleanHolder printHelp = new BooleanHolder (false);
   protected static BooleanHolder fullScreen = new BooleanHolder (false);
   protected static BooleanHolder yup = new BooleanHolder (false);
   protected static BooleanHolder drawAxes = new BooleanHolder (false);
   protected static BooleanHolder drawGrid = new BooleanHolder (false);
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
   protected static StringHolder historyFilename = new StringHolder();
   protected static StringHolder scriptsFilename =
      new StringHolder (".artisynthScripts");
   protected static StringHolder scriptFile = 
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
   protected static IntHolder glVersion = new IntHolder (2);

   protected static IntHolder flags = new IntHolder();

   protected static StringHolder mousePrefs = new StringHolder(); // "kees"
   public static final String [] mousePrefsOptions = {"default", "laptop", "kees", "mac"};

   protected static float[] bgColor = new float[3];
   protected static BooleanHolder openMatlab = new BooleanHolder(false);

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
    * the main entry point
    * 
    * @param args
    */
   public static void main (String[] args) {
      IntHolder width = new IntHolder (750);
      IntHolder height = new IntHolder (500);

      // some interfaces (like Matlab) may pass args in as null
      if (args == null) {
         args = new String[0];
      }

      ArgParser parser = new ArgParser ("java artisynth.core.driver.Main", false);
      parser.addOption ("-help %v #prints help message", printHelp);
      parser.addOption ("-width %d #width (pixels)", width);
      parser.addOption ("-height %d #height (pixels)", height);
      parser.addOption (
         "-bgColor %fX3 #background color (3 rgb values, 0 to 1)", bgColor);
      parser.addOption (
         "-maxStep %f #maximum time for a single step (sec)", maxStep);
      parser.addOption ("-drawAxes %v #draw coordinate axes", drawAxes);
      parser.addOption ("-drawGrid %v #draw grid", drawGrid);
      parser.addOption ("-axisLength %f #coordinate axis length", axisLength);
      parser.addOption ("-model %s #name of model to start with", modelName);
      parser.addOption ("-script %s #script to run immediately", scriptFile);
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
//      // no longer need to load librarypath
//      try {
//         JVMInfo.appendDefaultLibraryPath();
//      }
//      catch (Exception e) {
//         System.out.println ("Error loading library path");
//         e.printStackTrace();
//      }
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
      ArrayList<String> modelArgs = new ArrayList<String>();
      boolean modelArgsFound=false;
      for (String arg : args) {
         if (modelArgsFound) {
            modelArgs.add (arg);
         }
         else {
            if (arg.equals ("-M")) {
               modelArgsFound=true;
            }
            else {
               progArgs.add (arg);
            }
         }
      }
      // System.out.println ("progArgs: " + progArgs.size());
      // for (String a : progArgs) {
      //    System.out.println (" " + a);
      // }
      // System.out.println ("modelArgs: " + modelArgs.size());
      // for (String a : modelArgs) {
      //    System.out.println (" " + a);
      // }

      // Match arguments one at a time so we can avoid exitOnError if we are
      // running inside matlab
      String[] pargs = progArgs.toArray(new String[0]);
      int idx = 0;
      while (idx < pargs.length) {
         try {
            idx = parser.matchArg (pargs, idx);
            String unmatched;
            if ((unmatched=parser.getUnmatchedArgument()) != null) {
               System.out.println (
                  "Unrecognized argument: " + unmatched +
                  "\nUse -help for help information");
               return;
            }
         }
         catch (Exception e) {
            System.out.println (
               "Error parsing options: "+ e.getMessage());
            return;
         }
      }

      // parser.matchAllArgs (progArgs.toArray(new String[0]));
      if (printOptions.value || printHelp.value) {
         System.out.println (parser.getOptionsMessage (2));
         return;
      }

      MechSystemSolver.myDefaultHybridSolveP = !disableHybridSolves.value;
      if (numSolverThreads.value > 0) {
         PardisoSolver.setDefaultNumThreads (numSolverThreads.value);
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
      CollisionHandler.useSignedDistanceCollider =
         useSignedDistanceCollider.value;
      SurfaceMeshCollider.useAjlCollision = useAjlCollision.value;

      if (noGui.value == true) {
         width.value = -1;
      }
      GLVersion glv = (glVersion.value == 3 ? GLVersion.GL3 : GLVersion.GL2);
      Main m = new Main (PROJECT_NAME, width.value, height.value, glv);

      m.setArticulatedTransformsEnabled (useArticulatedTransforms.value);

      if (m.myFrame != null) {
         m.myViewer.setBackgroundColor (bgColor[0], bgColor[1], bgColor[2]);
         // XXX this should be done in the Main constructor, but needs
         // to be done here instead because of sizing effects
         m.myMenuBarHandler.initToolbar();
         m.myFrame.setViewerSize (width.value, height.value);
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
      if (modelArgsFound) {
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
                  className, name, modelArgs.toArray(new String[0]));
               m.loadModel (mi);
            }
         }
      }
      else {
         m.setRootModel (new RootModel(), null, null);
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
      
      if (scriptFile.value != null) {
         if (m.myFrame != null) {
            m.myMenuBarHandler.runScript(scriptFile.value);
         }
         else {
            if (m.myJythonConsole == null) {
               m.createJythonConsole (/*guiBased=*/false);
            }
            try {
               m.myJythonConsole.executeScript (scriptFile.value);
            }
            catch (Exception e) {
               System.out.println (
                  "Error executing script '"+scriptFile.value+"':");
               System.out.println (e);
            }
         }
      }
      if (m.myJythonConsole != null && m.myJythonFrame == null) {
         m.myJythonConsole.interact ();
      }
      if (openMatlab.value) {
         m.openMatlabConnection();
      }
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

   public InverseManager getInverseManager() {
      return myInverseManager;
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
   
   public boolean loadModelFile (File file) throws IOException {
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
      } else {
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

   public String getModelSaveFormat () {
      return myModelSaveFormat;
   }

   public void setModelSaveFormat (String fmtStr) {
      // create a NumberFormat to ensure fmtStr is syntactically correct
      // NumberFormat fmt = new NumberFormat (fmtStr);
      myModelSaveFormat = fmtStr;
   }

   public void saveModelFile (File file) throws IOException {
      saveModelFile (file, myModelSaveFormat);
   }

   public void saveModelFile (File file, String fmtStr) throws IOException {
      RootModel root = getRootModel();
      if (root != null) {
         IndentingPrintWriter pw = ArtisynthIO.newIndentingPrintWriter (file);
         if (!root.getClass().isAssignableFrom (RootModel.class)) {
            pw.println (ClassAliases.getAliasOrName (root.getClass()));
         }
         root.write (pw, new NumberFormat (fmtStr), root);
         pw.close();
      }
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
    * load the probes into the model
    * 
    * @param file
    * @throws IOException
    */
   public boolean loadProbesFile (File file) throws IOException {
      if (getWorkspace() == null) {
         return false;
      }
      ReaderTokenizer rtok = ArtisynthIO.newReaderTokenizer (file);
      getWorkspace().scanProbes (rtok);
      rtok.close();

      myProbeFile = file;

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
    * to save the probes file
    * 
    * @param file
    * @throws IOException
    */
   public boolean saveProbesFile (File file) throws IOException {
      if (getWorkspace() == null) {
         return false;
      }
      IndentingPrintWriter pw = ArtisynthIO.newIndentingPrintWriter (file);
      getWorkspace().writeProbes (pw, null);
      myProbeFile = file;
      return true;
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

   private boolean changeAffectsWaypoints (
      RootModel root, ComponentChangeEvent e) {

      // XXX there ought to be a cleaner way to do this ...
      ModelComponent comp = e.getComponent();
      if (comp == root.getWayPoints() ||
          (e instanceof StructureChangeEvent &&
           !((StructureChangeEvent)e).stateIsChanged())) {
         return false;
      }
      else {
         return true;
      }
   }         

   public void componentChanged (ComponentChangeEvent e) {
      if (e.getCode() == ComponentChangeEvent.Code.STRUCTURE_CHANGED ||
         e.getCode() == ComponentChangeEvent.Code.DYNAMIC_ACTIVITY_CHANGED) {
         boolean invalidateWaypoints = false;
         RootModel root = getRootModel();
         ModelComponent c = e.getComponent();
         if (myFrame != null && c != null &&
             (c == root || c.getParent() == root)) {
            updateApplicationMenuPresent (root);
         }
         if (root != null) {
            invalidateWaypoints = changeAffectsWaypoints(root, e);
            if (invalidateWaypoints) {
               root.getWayPoints().invalidateAfterTime(0);
            }
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
                  myTimeline.updateProbes();
               }
            }
            if (!getScheduler().isPlaying()) {
               rerender();
            }
         }
         if (invalidateWaypoints && !myScheduler.isPlaying()) {
            myScheduler.invalidateInitialState();
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
                     getRootModel().getDefaultViewOrientation());
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
         // getWorkspace().getWayPoints().invalidateAll();
         // myTimeline.requestUpdateDisplay();
         if (!getScheduler().isPlaying()) {
            rerender();
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

   /**
    * Set the current selection mode. Also set the display of the selection
    * buttons.
    * 
    * @param selectionMode
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
         else {
            myViewerManager.setCursor (Cursor.getDefaultCursor());
            // myREnderDriver.setSelectionEnabled(true);
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
               root.addController (myPullController, findMechSystem(root));
            }
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
            e.getIncrementalTransform().clone();
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
         AffineTransform3dBase transform = e.getTransform().clone();
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
         int flags = 0;
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

   private Translator3d translator3d = new Translator3d();
   private Transrotator3d transrotator3d = new Transrotator3d();
   private RotatableScaler3d scalar3d = new RotatableScaler3d();
   private Rotator3d rotator3d = new Rotator3d();
   private ConstrainedTranslator3d constrainedTranslator3d =
      new ConstrainedTranslator3d();
   private Dragger3dBase currentDragger;
   private LinkedList<ModelComponent> myDraggableComponents =
      new LinkedList<ModelComponent>();

   private static final double inf = Double.POSITIVE_INFINITY;

   /**
    * Called to update the current dragger position.
    */
   public void updateDragger() {
      if (currentDragger != null && !currentDragger.isDragging()) {
         if (myDraggableComponents.size() > 0) {
            Point3d dragCenter = new Point3d();
            if (myDraggableComponents.size() == 1 &&
                myDraggableComponents.get (0) instanceof HasCoordinateFrame) {
               RigidTransform3d X = new RigidTransform3d();
               ((HasCoordinateFrame)myDraggableComponents.get(0)).getPose (X);
               dragCenter.set (X.p);
               currentDragger.setPosition (dragCenter);
               //currentDragger.setDraggerToWorld (X);               
            }
            else {
               Point3d pmin = new Point3d (inf, inf, inf);
               Point3d pmax = new Point3d (-inf, -inf, -inf);
               for (ModelComponent c : myDraggableComponents) {
                  ((Renderable)c).updateBounds (pmin, pmax);
               }
               dragCenter.add (pmin, pmax);
               dragCenter.scale (0.5);
               currentDragger.setPosition (dragCenter);
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
      Point3d dragCenter = new Point3d();

      if (mySelectionMode != SelectionMode.Select &&
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
            RotationMatrix3d R = new RotationMatrix3d();
            ModelComponent onlyComponent = null;
            if (n == 1) {
               onlyComponent = myDraggableComponents.get(0);
            }
            if (onlyComponent instanceof HasCoordinateFrame &&
                !getInitDraggersInWorldCoords()) {
               RigidTransform3d X = new RigidTransform3d();
               ((HasCoordinateFrame)onlyComponent).getPose (X);
               dragCenter.set (X.p);
               R.set (X.R);
            }
            else {
               dragCenter.add (pmin, pmax);
               dragCenter.scale (0.5);
            }
            double radius = pmin.distance (pmax);

            // set a minium radius to about 1/6 of the viewer window width
            radius =
               Math.max (radius,
                  myViewer.distancePerPixel (myViewer.getCenter())
                  * myViewer.getWidth() / 6);

            if (mySelectionMode == SelectionMode.Translate) {
               translator3d.setVisible (true);
               translator3d.setDraggerToWorld (new RigidTransform3d (
                  dragCenter, R));
               translator3d.setSize (radius);
               currentDragger = translator3d;
            }
            else if (mySelectionMode == SelectionMode.Transrotate) {
               transrotator3d.setVisible (true);
               transrotator3d.setDraggerToWorld (new RigidTransform3d (
                  dragCenter, R));
               transrotator3d.setSize (radius);
               currentDragger = transrotator3d;
            }
            else if (mySelectionMode == SelectionMode.Scale) {
               scalar3d.setVisible (true);
               scalar3d.setDraggerToWorld (new RigidTransform3d (
                  dragCenter, R));
               scalar3d.setSize (radius);
               currentDragger = scalar3d;
            }
            else if (mySelectionMode == SelectionMode.Rotate) {
               rotator3d.setVisible (true);
               rotator3d.setDraggerToWorld (new RigidTransform3d (
                  dragCenter, R));
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
                        mesh = ((RigidBody)frame).getMesh();
                     }
                     break;
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
         myViewerManager.setSelectionHighlighting (
            SelectionHighlighting.Color);
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
         if (myViewerManager.getSelectionHighlighting() ==
             SelectionHighlighting.Color) {
            myViewerManager.setSelectionHighlighting (SelectionHighlighting.None);
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

}
