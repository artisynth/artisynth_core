package artisynth.core.gui.timeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.io.*;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import maspack.properties.Property;
import maspack.util.InternalErrorException;
import maspack.util.StringHolder;
import maspack.widgets.BooleanSelector;
import maspack.widgets.DoubleField;
import maspack.widgets.GuiUtils;
import maspack.widgets.IntegerField;
import maspack.widgets.OptionPanel;
import maspack.widgets.PropertyDialog;
import maspack.widgets.PropertyPanel;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueCheckListener;
import artisynth.core.driver.GenericKeyHandler;
import artisynth.core.driver.Main;
import artisynth.core.driver.MainFrame;
import artisynth.core.driver.SchedulerListener;
import artisynth.core.driver.Scheduler;
import artisynth.core.gui.Timeline;
import artisynth.core.gui.editorManager.ProbeEditor;
import artisynth.core.gui.probeEditor.NumericProbeEditor;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.NumericProbeDisplayLarge;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.probes.Probe;
import artisynth.core.probes.WayPoint;
import artisynth.core.probes.WayPointProbe;
import artisynth.core.util.TimeBase;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.RootModel;

public class TimelineController extends Timeline 
   implements SchedulerListener, SelectionListener {

   /**
     Component organization. Components with a (*) have their own paint methods.
     
     TimeToolbar myToolBar
     JSplitPane mainSplitPane
        JScrollPane workspaceScrollPane
           JPanel workspacePane
              JPanel masterPane             master track control
              JPanel probesPane             track information
        JScrollPane timelineScrollPane
           TimelinePane timelinePane(*)
              Timescale timescale           time scale and cursor
              WayPointTrack wayTrack(*)     displays waypoints
              ProbeTrack probeTrack(*)      displays all probes
   */
   private static final long serialVersionUID = 1L;
   private static final int SPLIT_DIVIDER_SIZE = 1;
   private static final int SCROLL_OFFSET = 75;

   protected ArrayList<Track> myInTracks = new ArrayList<Track>();
   protected ArrayList<Track> myOutTracks = new ArrayList<Track>();
   protected ArrayList<WayPointInfo> wayInfos = new ArrayList<WayPointInfo>();
   protected ArrayList<NumericProbeEditor> myProbeEditors =
      new ArrayList<NumericProbeEditor>();
   protected LinkedHashMap<Probe,ProbeInfo> myProbeMap =
      new LinkedHashMap<Probe,ProbeInfo>();
   
   //private SelectionListener mySelectionListener;
   
   private int timelineDuration;
   private int zoomLevel;
   private boolean activeWayPointExists;
   private boolean activeProbeExists;
   
   private Cursor currentCursor;
   protected Timescale timescale;

   private JPanel workspacePane;
   private TimelinePane timelinePane;
   private TimeToolbar myToolBar;
   private JScrollPane timelineScrollPane;
   private JFileChooser myWayPointFileChooser;
   
   private JPanel probesPane;
   private JPanel masterPane;
   private ProbeTrack probeTrack;
   private Track currentTrack = null;
   protected ArrayList<ArrayList<Track>> groupedTracks =
      new ArrayList<ArrayList<Track>>();
   protected ArrayList<Track> selectedTracks = new ArrayList<Track>();
   protected ArrayList<ProbeInfo> selectedProbes = new ArrayList<ProbeInfo>();
   
   private int firstIndex = 0;
   private int lastIndex = 0;
   
   private WayPointTrack wayTrack;
   private JToggleButton muteToggle;
   private JToggleButton expandToggle;
   
   private Line2D myWayShadow;
   private JSplitPane mainSplitPane;
   //private JPopupMenu myPopup;
   //private JMenuItem mySaveWayPointsItem;
   //private JMenuItem myReloadWayPointsItem;
   private WayPointTrackListener wayTrackListener;

   // these are the different levels of GUI update that can be requested:
   private static final int REFRESH_DISPLAY = 0x01;
   private static final int REFRESH_WIDGETS = 0x02;
//   private static final int REFRESH_COMPONENTS = 0x04;
   private static final int REFRESH_CURSOR = 0x08;

   private static final int UPDATE_DISPLAY = REFRESH_DISPLAY;
   private static final int UPDATE_WIDGETS =
      (UPDATE_DISPLAY | REFRESH_WIDGETS);
//   private static final int UPDATE_COMPONENTS =
//      (UPDATE_WIDGETS | REFRESH_COMPONENTS);

   protected Main myMain;
   protected Scheduler myScheduler;
   
   /**
    * Creates the timeline
    */
   public TimelineController (String title, Main main) {
      myMain = main;
      myScheduler = main.getScheduler();
      
      fieldsInitialization();
      panesDisplayInitialization();      
      displaySetup();
      //popupInitialization();
      
      GenericKeyHandler keyHandler = new GenericKeyHandler(myMain);
      workspacePane.addKeyListener (keyHandler);
      timelinePane.addKeyListener (keyHandler);
      timelineScrollPane.addKeyListener (keyHandler);
      myToolBar.addKeyListener (keyHandler);
      mainSplitPane.addKeyListener (keyHandler);
      addKeyListener (keyHandler);

      myToolBar.updateToolbarState(main.getRootModel());
      
      //mySelectionListener = timelineSelectionHandler;

      setTitle (title);
      pack();
   }

   private JMenuItem addMenuItem (JPopupMenu menu, String label) {
      return addMenuItem (menu, label, true);
   }

   private JMenuItem addMenuItem (
      JPopupMenu menu, String label, boolean enabled) {
      JMenuItem item = new JMenuItem (label);
      item.addActionListener (wayTrackListener);
      item.setActionCommand (label);
      item.setEnabled (enabled);
      menu.add (item);
      return item;
   }


   private JPopupMenu createPopupMenu () {
      JPopupMenu menu = new JPopupMenu();

      boolean hasRootModel = (myMain.getRootModel() != null);
      boolean hasProbeFile = (myMain.getProbesFile() != null);
      boolean hasWaypointFile = 
         myMain.getRootModel().getWayPoints().hasAttachedFile();

      addMenuItem (menu, "Add waypoint here");
      addMenuItem (menu, "Add breakpoint here");
      addMenuItem (menu, "Add waypoint(s) ...");
      addMenuItem (menu, "Disable all breakpoints");
      addMenuItem (menu, "Delete waypoints", hasRootModel);

      menu.addSeparator();

      addMenuItem (menu, "Load probes ...", hasRootModel);
      addMenuItem (menu, "Save probes", hasRootModel && hasProbeFile);
      addMenuItem (menu, "Save probes as ...", hasRootModel);
      addMenuItem (menu, "Save output probe data", hasRootModel);
      
      menu.addSeparator();

      addMenuItem (menu, "Save waypoints", hasRootModel && hasWaypointFile);
      addMenuItem (menu, "Save waypoints as ...", hasRootModel);
      addMenuItem (menu, "Load waypoints ...", hasRootModel);
      addMenuItem (menu, "Reload waypoints", hasRootModel && hasWaypointFile); 

      menu.addSeparator();

      addMenuItem (menu, "Add input track");
      addMenuItem (menu, "Add output track");

      return menu;
   }      
        
   
   // private void popupInitialization() {
   //    JMenuItem addInTrack = new JMenuItem ("Add input track");
   //    addInTrack.addActionListener (wayTrackListener);
   //    addInTrack.setActionCommand ("Add input track");

   //    JMenuItem addOutTrack = new JMenuItem ("Add output track");
   //    addOutTrack.addActionListener (wayTrackListener);
   //    addOutTrack.setActionCommand ("Add output track");

   //    JMenuItem addWayHere = new JMenuItem ("Add waypoint here");
   //    addWayHere.addActionListener (wayTrackListener);
   //    addWayHere.setActionCommand ("Add waypoint here");

   //    JMenuItem addBreakHere = new JMenuItem ("Add breakpoint here");
   //    addBreakHere.addActionListener (wayTrackListener);
   //    addBreakHere.setActionCommand ("Add breakpoint here");

   //    JMenuItem addWay = new JMenuItem ("Add waypoint(s) ...");
   //    addWay.addActionListener (wayTrackListener);
   //    addWay.setActionCommand ("Add waypoint(s) ...");

   //    JMenuItem disableAllBreakPoints =
   //       new JMenuItem ("Disable all breakpoints");
   //    disableAllBreakPoints.addActionListener (wayTrackListener);
   //    disableAllBreakPoints.setActionCommand ("Disable all breakpoints");

   //    JMenuItem deleteWayPoints =
   //       new JMenuItem ("Delete waypoints");
   //    deleteWayPoints.addActionListener (wayTrackListener);
   //    deleteWayPoints.setActionCommand ("Delete waypoints");
      
   //    mySaveWayPointsItem =
   //       new JMenuItem ("Save waypoints");
   //    mySaveWayPointsItem.addActionListener (wayTrackListener);
   //    mySaveWayPointsItem.setActionCommand ("Save waypoints");
      
   //    JMenuItem saveWayPointsAs =
   //       new JMenuItem ("Save waypoints as ...");
   //    saveWayPointsAs.addActionListener (wayTrackListener);
   //    saveWayPointsAs.setActionCommand ("Save waypoints as ...");

   //    myReloadWayPointsItem =
   //       new JMenuItem ("Reload waypoints");
   //    myReloadWayPointsItem.addActionListener (wayTrackListener);
   //    myReloadWayPointsItem.setActionCommand ("Reload waypoints");
      
   //    JMenuItem loadWayPointsFrom =
   //       new JMenuItem ("Load waypoints ...");
   //    loadWayPointsFrom.addActionListener (wayTrackListener);
   //    loadWayPointsFrom.setActionCommand ("Load waypoints");

   //    myPopup.add (addWayHere);
   //    myPopup.add (addBreakHere);
   //    myPopup.add (addWay);
   //    //      myPopup.add (addBreak);
   //    myPopup.add (disableAllBreakPoints);
   //    myPopup.addSeparator ();
   //    myPopup.add (mySaveWayPointsItem);
   //    myPopup.add (saveWayPointsAs);
   //    myPopup.add (loadWayPointsFrom);
   //    myPopup.add (myReloadWayPointsItem);
   //    myPopup.add (deleteWayPoints);
   //    myPopup.addSeparator ();
   //    myPopup.add (addInTrack);
   //    myPopup.add (addOutTrack);
   // }
   
   // private void popupUpdate() {
   //    boolean hasWayPointFile = 
   //       myMain.getRootModel().getWayPoints().hasAttachedFile();
   //    mySaveWayPointsItem.setEnabled (hasWayPointFile);
   //    myReloadWayPointsItem.setEnabled (hasWayPointFile);
   // } 
   

   private void displaySetup () {
      timescale.addChangeListener (new ChangeListener() {
         public void stateChanged (ChangeEvent e) {
            double cursorTime = timescale.getTimescaleCursorTime();

            // If the simulation has stopped and the timescale is
            // adjusted manually, then the restart time must be changed
            if (!myScheduler.isPlaying() && 
                  timescale.isTimeManuallyDragged()) {
               // If the model is not dynamic, the scheduler time can
               // be reset to any time corresponding to the timeline cursor
               if (!myMain.getWorkspace().rootModelHasState()) {
                  myScheduler.setTime (cursorTime);
               }
               // Else only reset the scheduler time to the closest valid 
               // waypoint when the cursor is dragged past a valid waypoint
               else {
                  setRestartTime (TimelineConstants.RESTART_BY_CURSOR_DRAG);
               }
            }
            // See if the duration of the timeline has to be extended
            if (cursorTime >= 0.9*timescale.getMaximumTime()) {
               extendTimeline();
            }
            if (timescale.isTimeManuallyDragged()) {
               requestUpdateWidgets();
            }
         }
      });

      timelineScrollPane.getHorizontalScrollBar().addAdjustmentListener (
         new AdjustmentListener() {
            public void adjustmentValueChanged (AdjustmentEvent e) {
               requestUpdateDisplay ();
            }
         });      
      
      timelineScrollPane.setViewportView (timelinePane);
      JScrollPane workspaceScrollPane = new JScrollPane (workspacePane);
      
      timelineScrollPane.setVerticalScrollBarPolicy (
         ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

      // Set the workspace scrollpane not to show vertical scroll bar
      workspaceScrollPane.setVerticalScrollBarPolicy (
         JScrollPane.VERTICAL_SCROLLBAR_NEVER);
      workspaceScrollPane.setHorizontalScrollBarPolicy (
         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

      // Synchronize scrollbars of workspace and timeline panels
      timelineScrollPane.getVerticalScrollBar().setModel (
         workspaceScrollPane.getVerticalScrollBar().getModel());

      mainSplitPane = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, 
         workspaceScrollPane, timelineScrollPane);

      mainSplitPane.setDividerLocation (GuiStorage.TIMELINE_PANEL_WIDTH);
      mainSplitPane.setDividerSize (SPLIT_DIVIDER_SIZE);
      mainSplitPane.setContinuousLayout (true);
      mainSplitPane.addPropertyChangeListener ("dividerLocation", 
         new PropertyChangeListener() {
            public void propertyChange (PropertyChangeEvent pc_evt) {
               updateComponentSizes();
            }
      });

      Container contentPane = getContentPane();
      contentPane.add (myToolBar, BorderLayout.PAGE_START);
      contentPane.add (mainSplitPane);

      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      setLocation (screenSize.width / 6, screenSize.height / 6);
      setSize (screenSize.width * 1 / 2, screenSize.height * 1 / 4);

      updateComponentSizes();
   }

   /**
    * Responsible for initializing all of the class variables and building the
    * panels for the timeline
    */
   private void fieldsInitialization() {
      zoomLevel = TimelineConstants.DEFAULT_ZOOM_LEVEL;
      timelineDuration = TimelineConstants.DEFAULT_DURATION;

      //isExecutingStep = false;
      activeWayPointExists = false;
      activeProbeExists = false;

      currentCursor = Cursor.getDefaultCursor();

      timescale = new Timescale (this, timelineDuration, zoomLevel);
      timelinePane = new TimelinePane (this);

      workspacePane = new JPanel();
      wayTrack = new WayPointTrack();
      probeTrack = new ProbeTrack();
      probesPane = new JPanel();
      masterPane = new JPanel();
      
      timelineScrollPane = new JScrollPane();    
      
      myWayShadow = new Line2D.Float();
      
      wayTrackListener = new WayPointTrackListener();      
      //myPopup = new JPopupMenu();
      muteToggle = new JToggleButton (GuiStorage.MUTE_TRACK_ICON);
      expandToggle = new JToggleButton (GuiStorage.EXPAND_TRACK_ICON);
   }

   private void panesDisplayInitialization() {
      if (myToolBar == null) {
         myToolBar = new TimeToolbar (this);
      }
      
      probesPane.setLayout (new BoxLayout (probesPane, BoxLayout.Y_AXIS));
      probesPane.setAlignmentX (JComponent.LEFT_ALIGNMENT);
      probesPane.setBackground (GuiStorage.COLOR_MODEL_WORKSPACE_NORMAL);
      
      masterPane.setLayout (new BoxLayout (masterPane, BoxLayout.X_AXIS));
      masterPane.setAlignmentX (JComponent.LEFT_ALIGNMENT);
      masterPane.setBackground (GuiStorage.COLOR_MODEL_WORKSPACE_NORMAL);
      
      Dimension size = new Dimension (
         GuiStorage.TIMELINE_PANEL_WIDTH, GuiStorage.TRACK_NORMAL_HEIGHT);
      masterPane.setSize (size);
      masterPane.setMinimumSize (size);
      masterPane.setMaximumSize (size);
      masterPane.setPreferredSize (size);
      
      muteToggle.setSelectedIcon (GuiStorage.UNMUTE_TRACK_ICON);
      muteToggle.addActionListener (new ActionListener() {
         public void actionPerformed (ActionEvent a_evt) {
            setAllTracksMuted (muteToggle.isSelected());
         }
      });

      muteToggle.setSize (GuiStorage.ICON_SIZE);
      muteToggle.setPreferredSize (GuiStorage.ICON_SIZE);
      muteToggle.setMinimumSize (GuiStorage.ICON_SIZE);
      muteToggle.setMaximumSize (GuiStorage.ICON_SIZE);
      
      expandToggle.setSelectedIcon (GuiStorage.CONTRACT_TRACK_ICON);
      expandToggle.addActionListener (new ActionListener() {
         public void actionPerformed (ActionEvent a_evt) {
            expandTracks (expandToggle.isSelected(), myInTracks);
            expandTracks (expandToggle.isSelected(), myOutTracks);
         }
      });

      expandToggle.setSize (GuiStorage.ICON_SIZE);
      expandToggle.setPreferredSize (GuiStorage.ICON_SIZE);
      expandToggle.setMinimumSize (GuiStorage.ICON_SIZE);
      expandToggle.setMaximumSize (GuiStorage.ICON_SIZE);
      
      masterPane.add (Box.createRigidArea (new Dimension (5, 0)));
      masterPane.add (expandToggle);
      masterPane.add (Box.createRigidArea (new Dimension (3, 0)));
      masterPane.add (muteToggle);
      
      masterPane.addMouseListener (wayTrackListener);
      wayTrack.addMouseListener (wayTrackListener);

      workspacePane.setLayout (new BoxLayout (workspacePane, BoxLayout.Y_AXIS));
      workspacePane.setAlignmentX (Component.LEFT_ALIGNMENT);
      workspacePane.setBackground (Color.WHITE);
      workspacePane.add (Box.createRigidArea (new Dimension (0, 31)));
      workspacePane.add (masterPane);
      workspacePane.add (Box.createRigidArea (new Dimension (0, 3)));
      workspacePane.add (probesPane);
      workspacePane.add (Box.createRigidArea (new Dimension (0, 100)));

      timelinePane.add (Box.createRigidArea (new Dimension (0, 1)));
      timelinePane.add (timescale);
      timelinePane.add (Box.createRigidArea (new Dimension (0, 3)));
      timelinePane.add (wayTrack);
      timelinePane.add (Box.createRigidArea (new Dimension (0, 1)));
      timelinePane.add (probeTrack);
      
      timelinePane.addMouseListener (new MouseAdapter () {
         public void mouseClicked (MouseEvent m_evt) {
            myMain.getSelectionManager().clearSelections();
         }      
      });
   }

   /**
    * Computes the length that each component on the timeline should represent
    * as a function of the time which the component spans over, and then updates
    * the size of these components accordingly.
    */
   public void updateComponentSizes() {
      int newTimescaleLength =
         timescale.computeExactLengthInPixel (timelineDuration, zoomLevel);
      int newWorkspacePaneWidth = mainSplitPane.getDividerLocation ();

      timelinePane.updateTimelinePaneSize (newTimescaleLength);
      timelineScrollPane.getHorizontalScrollBar().setMaximum (
         newTimescaleLength);
      
      // update the size of the wayTrack
      Dimension wayTrackSize = new Dimension (newTimescaleLength, wayTrack.getHeight());
      wayTrack.setSize (wayTrackSize);
      wayTrack.setMinimumSize (wayTrackSize);
      wayTrack.setMaximumSize (wayTrackSize);
      wayTrack.setPreferredSize (wayTrackSize);

      Dimension probeTrackSize = 
         new Dimension (newTimescaleLength, getProbeTrackHeight());

      probeTrack.setSize (probeTrackSize);
      probeTrack.setMinimumSize (probeTrackSize);
      probeTrack.setMaximumSize (probeTrackSize);
      probeTrack.setPreferredSize (probeTrackSize); 

      // update the size of each track associated with this model
      for (Track inTrack : myInTracks) {
         inTrack.updateTrackSize (newTimescaleLength, newWorkspacePaneWidth);
      }

      for (Track outTrack : myOutTracks) {
         outTrack.updateTrackSize (newTimescaleLength, newWorkspacePaneWidth);
      }
      
      Dimension size = new Dimension (
         newWorkspacePaneWidth, GuiStorage.TRACK_NORMAL_HEIGHT);

      masterPane.setSize (size);
      masterPane.setMinimumSize (size);
      masterPane.setMaximumSize (size);
      masterPane.setPreferredSize (size);

      requestUpdateDisplay();
   }
   
   public int getProbeTrackHeight () {
      // update the size of the probeTrack, accounting for hidden tracks
      int probeTrackHeight = 0;
      for (Track inTrack : myInTracks) {
         if (inTrack.isVisible()) {
            if (inTrack.isExpanded()) {
               probeTrackHeight += GuiStorage.TRACK_EXPAND_HEIGHT;
            }
            else {
               probeTrackHeight += GuiStorage.TRACK_NORMAL_HEIGHT;
            }
         }
      }
      
      for (Track outTrack : myOutTracks) {
         if (outTrack.isVisible()) {
            if (outTrack.isExpanded()) {
               probeTrackHeight += GuiStorage.TRACK_EXPAND_HEIGHT;
            }
            else {
               probeTrackHeight += GuiStorage.TRACK_NORMAL_HEIGHT;
            }
         }
      }
      
      return probeTrackHeight;
   }

   private void updateWidgets (RootModel rootModel, boolean refreshCursor) {
      double t = getCurrentTime();
      updateProbesAndWayPoints(rootModel);
      
      myToolBar.updateToolbarState(rootModel);
      setAllWayPointDisplay();
      updateAllProbeDisplays();
      updateTimeDisplay (t);
      if (refreshCursor || myScheduler.isPlaying()) {
         timescale.updateTimeCursor (t);
      }
      updateTimelineScroll();
      setAllMutable (!myScheduler.isPlaying());
   }

   private void updateDisplay() {
      timelinePane.revalidate();
      workspacePane.revalidate();
      repaintVisibleWindow();
   }
   
   class UpdateAction implements Runnable {
      private int myCode;

      UpdateAction (int code) {
         myCode = code;
      }
      public void run() {
         boolean refreshCursor = false;
         if ((myCode & REFRESH_CURSOR) == REFRESH_CURSOR) {
            refreshCursor = true;
         }
//         if ((myCode & REFRESH_COMPONENTS) == REFRESH_COMPONENTS) {
//            resetAll();
//         }
         // 
         RootModel rootModel = myMain.getRootModel();
         if (rootModel != null) {
            if ((myCode & REFRESH_WIDGETS) == REFRESH_WIDGETS) {
               updateWidgets (rootModel, refreshCursor);
            }
            if ((myCode & REFRESH_DISPLAY) == REFRESH_DISPLAY) {
               updateDisplay();
            }
         }
      }
   }

   class UpdateRequest extends TimerTask {
      public void run() {
         int code;
         synchronized (TimelineController.this) {
            code = myCurrentUpdateCode;
            myCurrentUpdateCode = 0;
         }
         SwingUtilities.invokeLater (new UpdateAction(code));
      }
   }

   Timer myRequestTimer = new Timer();
   //RepaintRequest myCurrentRepaintRequest = null;
   static int myCurrentUpdateCode = 0;
   static int REQUEST_DELAY = 20;

   public void requestUpdateDisplay () {
      requestUpdate (UPDATE_DISPLAY);
   }

   public void requestUpdateWidgets () {
      requestUpdate (UPDATE_WIDGETS);
   }

   public void requestResetAll () {
      if (SwingUtilities.isEventDispatchThread()) {
         resetAll();
      }
      else {
         try {
            SwingUtilities.invokeAndWait (new Runnable() {
               public void run () {
                  resetAll();
               }
            });
         }
         catch (Exception e) {
            e.printStackTrace();
            throw new InternalErrorException (
               "Exception inside timeline.resetAll()");
         }
      }
   }
   
   /** 
    * Schedules a update request for the GUI components. The purpose of this
    * routine is to limit the number of actual requests that are processed:
    * updates are set to occur ever 20 msec or so.
    */
   void requestUpdate(int code) {
      if (isVisible() && (myCurrentUpdateCode & code) != code) {
         UpdateRequest request = new UpdateRequest ();
         synchronized (TimelineController.this) {
            if (myCurrentUpdateCode != 0) { // check again to be sure
               request = null; // cancel the request, one already in progress
            }
            myCurrentUpdateCode |= code;
         }

         if (request != null) {
            myRequestTimer.schedule (request, REQUEST_DELAY);
         }
      }
   }

   /**
    * Get the current dimensions of the timeline window and repaint the timeline
    * accordingly, and make a call to repaint the workspace.
    */
   public void repaintVisibleWindow() {      
      int minHorizontalValue =
         timelineScrollPane.getHorizontalScrollBar().getValue();
      int maxHorizontalValue = minHorizontalValue + 
         timelineScrollPane.getHorizontalScrollBar().getVisibleAmount();

      int minVerticalValue = 0;
      int maxVerticalValue = timelinePane.getHeight();

      int width = maxHorizontalValue - minHorizontalValue;
      int height = maxVerticalValue - minVerticalValue;

      Rectangle visibleRect =
         new Rectangle (minHorizontalValue, minVerticalValue, width, height);

      timelinePane.repaint (visibleRect);
      workspacePane.repaint();
   }

   /**
    * Responsible for setting up the model after the mouse has been 
    * used to drag the time to Tn.
    */
   public void setRestartTime (int dragMode) {
      WayPoint prevWay, nextWay;

      double currentCursorTime = timescale.getTimescaleCursorTime();

      // Find previous waypoint that is valid before or at current cursor time
      WayPoint way = getWayPoints().getValidOnOrBefore (currentCursorTime);
      if (way.getTime() != myScheduler.getTime()) {
         myScheduler.setTime (way);
      }
   }
   
   public void setAllMutable (boolean muted) {
      for (Track inTrack : myInTracks) {
         inTrack.setMutable (muted);
      }

      for (Track outTrack : myOutTracks) {
         outTrack.setMutable (muted);
      }
   }

   public void setAllTracksMuted (boolean muted) {
      muteTracks (muted, myInTracks);
      muteTracks (muted, myOutTracks);
   }
   
   public void muteTracks (boolean muted, ArrayList<Track> list) {
      for (Track track : list) {
         track.muteTrack (muted);
      }
   }
   
   public void expandTracks (boolean expanded, ArrayList<Track> list) {
      for (Track track : list) {
         track.setExpanded (expanded);
      }
   }
   
   public void updateAllProbeDisplays() {
      for (Track inTrack : myInTracks) {
         for (ProbeInfo probeInfo : inTrack.getProbeInfos()) {
            probeInfo.setAppropriateColor ();               
            probeInfo.updateProbeDisplays();
         }
      }
      for (Track outTrack : myOutTracks) {
         for (ProbeInfo probeInfo : outTrack.getProbeInfos()) {
            probeInfo.setAppropriateColor ();
            probeInfo.updateProbeDisplays();
         }
      }
   }
   
   /**
    * Computes the location where each component on the timeline should be as a
    * function of the time with which the component spans over, and updates the
    * location and size of these components accordingly.
    */
   public void updateComponentLocations() {
      for (WayPointInfo wayInfo : wayInfos) {
         wayInfo.setWayMarkersLocation();
      }

      for (Track track : myInTracks) {
         track.updateProbeSizesAndLocations();
      }
      
      for (Track track : myOutTracks) {
         track.updateProbeSizesAndLocations();
      }
   }
   
   protected void setAllNumericDisplays() {
      for (Track inTrack : myInTracks) {
         if (inTrack.isExpanded()) {
            for (ProbeInfo probeInfo : inTrack.getProbeInfos()) {
               probeInfo.setNumericProbeDisplay();
            }
         }
      }

      for (Track outTrack : myOutTracks) {
         if (outTrack.isExpanded()) {
            for (ProbeInfo probeInfo : outTrack.getProbeInfos()) {
               probeInfo.setNumericProbeDisplay();
            }
         }
      }
   }
   
   protected void setAllWayPointDisplay() {
      for (WayPointInfo wayPoint : wayInfos) {
         wayPoint.setValidityDisplay (false);
      }
   }
   
//   /**
//    * Called internally when the step time is set by the GUI
//    */
//   void setStepTime (double newStep) {
//      myScheduler.setStepTime (newStep);
//   }

   /**
    * Resets the timeline.  Called when it is first created and when new
    * models are loaded.
    */
   public void resetAll() {
      // Assume that caller has ensured the scheduler is not running
      //myScheduler.stopRequest();
      //myScheduler.waitForPlayingToStop();
      RootModel rootModel = myMain.getRootModel();

      int count = myInTracks.size();
      for (int i = 0; i < count; i++) {
         deleteTrack (Track.TYPE_INPUT, 0, false);
      }
      count = myOutTracks.size();
      for (int j = 0; j < count; j++) {
         deleteTrack (Track.TYPE_OUTPUT, 0, false);
      }
      myProbeMap.clear();

      // Add the root model on timeline
      Track suitableTrack;

      // Process all the input probes into proper places
      for (Probe p : rootModel.getInputProbes()) {
         addProbe (p);
      }

      // Process all the output probes into proper places
      for (Probe p : rootModel.getOutputProbes()) {
         if (!(p instanceof WayPointProbe)) {
            addProbe (p);
         }
      }
      
      // Process the waypoints
      refreshWayPoints(rootModel);
//       for (WayPoint wayPoint : getWayPoints().getPoints ()) {
//          addWayPointFromRoot (wayPoint);
//       }

      myToolBar.updateToolbarState (rootModel);
      
      closeProbeEditors();
      myProbeEditors = new ArrayList<NumericProbeEditor>();
      selectedProbes = new ArrayList<ProbeInfo>();
      
      expandToggle.setSelected (false);
      muteToggle.setSelected (false);
      
      updateWidgets (rootModel, /*refreshCursor=*/true);
      updateDisplay();
      //requestUpdate (UPDATE_WIDGETS | REFRESH_CURSOR);
   }
   
   /**
    * Called when clearTimeline is requested on the timeline, or an action 
    * is performed requesting "Delete waypoint".
    */
   public void deleteWayPoint (int idx, boolean isTimelineReset) {
      // waypoint is to be deleted if this is a timeline reset, or if
      // the user had confirmed deletion of the selected waypoint.
      if ((isTimelineReset) || (confirmAction ("Delete this waypoint?"))) {
         if (!isTimelineReset) {
            // Remove the waypoint from the root
            wayInfos.get (idx).removeWayPointFromRoot();
         }

         wayInfos.remove (idx);

         // Re-index waypoints
         for (int i = 0; i < wayInfos.size(); i++) {
            wayInfos.get (i).setIndex (i);
         }

         rebuildWayPointTrack();
      }
      requestUpdateDisplay();
   }
   
   public void closeProbeEditors() {
      if (myProbeEditors != null) {
         for (NumericProbeEditor probeEditor : myProbeEditors) {
            probeEditor.dispose();
         }
      }
   }
   
   /**
    * Creates a JOptionPane which presents the user with yes/no options.
    * 
    * @return TRUE if user selected JOptionPane.YES_OPTION, false otherwise
    */

   private boolean confirmAction (String confirmMessage) {
      int confirmation = JOptionPane.showConfirmDialog (
         this, confirmMessage, "Confirm", JOptionPane.YES_NO_OPTION,
         JOptionPane.QUESTION_MESSAGE);

      return (confirmation == JOptionPane.YES_OPTION);
   }
   
   /**
    * Removes a wayPoint at the request of the system. This means that the
    * system does not need to be informed about its removal.
    */
   public void removeWayPointFromRoot (WayPoint way) {
      int idx = -1;
      for (int i = 0; i < wayInfos.size(); i++) {
         if (wayInfos.get (i).myWayPoint == way) {
            idx = i;
            break;
         }
      }
      
      if (idx != -1) {
         wayInfos.remove (idx);

         // Re-index waypoints
         for (int i = 0; i < wayInfos.size(); i++) {
            wayInfos.get (i).setIndex (i);
         }

         rebuildWayPointTrack();
      }
      requestUpdateDisplay();
   }
   
   public void setZoomLevel (int zoom) {
      if (zoom >= TimelineConstants.MAXIMUM_ZOOM) {
         zoomLevel = TimelineConstants.MAXIMUM_ZOOM;
      }
      else if (zoom <= TimelineConstants.MINIMUM_ZOOM) {
         zoomLevel = TimelineConstants.MINIMUM_ZOOM;
      }
      else {
         zoomLevel = zoom;
      }

      timescale.updateTimescaleSizeAndScale (timelineDuration, zoomLevel);
   }
   
   /**
    * Automatically decide which zoom level is better for the probes and set
    * this level based on the maximum probe time data.
    * 
    */

   public void automaticProbesZoom() {
      double largestTime = getLargestProbeTime();
      Integer currZoomLevel = getZoomLevel();

      int numZooms = 0;
      double maxVisibleZoom = GuiStorage.ZOOM_LEVELS[currZoomLevel];

      // If largestProbeTime = 0, try zooming on waypoints instead
      if (largestTime == 0) {
         if (getNumberOfWayPoints() <= 1) {
            return;
         }
         
         largestTime = getLargestWayPointTime();
      }

      // Try zooming in on the probes if they exist
      //Double maxVisibleZoomD = Double.parseDouble (maxVisibleZoom.toString());
      //Double largestTimeD = Double.parseDouble (largestTime.toString());

      double logValue = Math.log (maxVisibleZoom / largestTime);

      if (logValue >= 0) {
         numZooms = (int)Math.ceil (logValue);
         
         if (numZooms > (TimelineConstants.MAXIMUM_ZOOM - zoomLevel)) {
            numZooms = TimelineConstants.MAXIMUM_ZOOM - zoomLevel;
         }
         
         for (int i = 0; i < numZooms; i++) {
            zoom (TimelineConstants.ZOOM_IN);
            currZoomLevel++;
         }
      }
      else {
         numZooms = (int)Math.floor (logValue);
         numZooms = Math.abs (numZooms);
         
         for (int i = 0; i <= numZooms && currZoomLevel > 0; i++) {
            zoom (TimelineConstants.ZOOM_OUT);
            currZoomLevel--;
         }
      }

      // See if zoom out from the probe is needed
      if (GuiStorage.ZOOM_LEVELS[getZoomLevel()] < largestTime && 
          currZoomLevel > 0) {
         try {
            zoom (TimelineConstants.ZOOM_OUT);
            currZoomLevel--;
         }
         catch (Exception e) {}
      }

      setZoomLevel (currZoomLevel);
   }
   
   /**
    * Sets the current running time of the model
    * 
    * @param t time is seconds
    */
   public void updateTimeDisplay (double t) {
      myToolBar.updateTimeDisplay (t);
   }
   
   /** 
    * Checks to see if the currently loaded probes and waypoints are consistent
    * with the root model, and updates them if necessary. This method is not
    * thread-safe.
    * 
    * @return true if probes needed updating, false otherwise.
    */   
   public boolean updateProbesAndWayPoints (RootModel rootModel) {
      boolean updateWasNeeded = false;
      for (ProbeInfo info : myProbeMap.values()) {
         info.myMark = true;
      }
      for (Probe p : rootModel.getInputProbes()) {
         ProbeInfo info = myProbeMap.get(p);
         if (info != null) {
            info.myMark = false;
         }
         else {
            addProbe (p);
            updateWasNeeded = true;
         }
      }
      for (Probe p : rootModel.getOutputProbes()) {
         ProbeInfo info = myProbeMap.get(p);
         if (info != null) {
            info.myMark = false;
         }
         else {
            addProbe (p);
            updateWasNeeded = true;
         }
      }
      for (ProbeInfo info : myProbeMap.values()) {
         if (info.myMark == true) {
            removeProbe (info.getProbe());
            updateWasNeeded = true;
         }
      }
      if (refreshWayPoints(rootModel)) {
         updateWasNeeded = true;
      }
      return updateWasNeeded;
   }

   /**
    * Updates probes when probes are added or removed from the RootModel.
    */
   public void updateProbes(RootModel root) {
      
      LinkedList<Probe> inList = new LinkedList<Probe>();
      for (int i = 0; i < myInTracks.size(); i++) {
         myInTracks.get (i).appendProbes (inList);
      }      
      updateProbeChanges (root.getInputProbes(), inList);
      
      LinkedList<Probe> outList = new LinkedList<Probe>();
      for (int i = 0; i < myOutTracks.size(); i++) {
         myOutTracks.get (i).appendProbes (outList);
      }
      updateProbeChanges (root.getOutputProbes(), outList);
      
      for (int i = 0; i < myInTracks.size(); i++) {
         myInTracks.get (i).updateProbeData();
      }
      for (int i = 0; i < myOutTracks.size(); i++) {
         myOutTracks.get (i).updateProbeData();
      }
      refreshWayPoints(root);
      refreshProbeTrackDisplay();
      requestUpdateDisplay();
   }

   private void updateProbeChanges (
      Collection<Probe> newSet, Collection<Probe> oldSet) {

      for (Probe p : newSet) {
         if (!oldSet.contains (p)) {
            addProbe (p);
         }
      }
      for (Probe p : oldSet) {
         if (!newSet.contains (p)) {
            removeProbe (p);
         }
      }
   }
   
   /**
    * Adds a probe to the timeline.
    * 
    */
   public void addProbe (Probe probe) {
      Track suitableTrack = findTrackForProbe (probe);
      if (suitableTrack == null) {
         suitableTrack = createAndAddTrack (
            probe.isInput() ? Track.TYPE_INPUT : Track.TYPE_OUTPUT);
      }
      ProbeInfo pinfo = suitableTrack.addProbeFromRoot (probe);
      myProbeMap.put (probe, pinfo);
   }

   public void removeProbe (Probe probe) {
      Track track = getProbeTrack (probe);
      
      if (track != null) {
         track.deleteProbe (probe);
         myProbeMap.remove (probe);
      }
   }
   
   public void addProbeEditor (NumericProbeEditor editor) {
      myProbeEditors.add (editor);
   }
   
   public void setActiveWayPointExist (boolean exist) {
      activeWayPointExists = exist;
   }
   
   public void setActiveProbeExist (boolean exist) {
      activeProbeExists = exist;
   }
   
   public void updateWayPointListOrder (int modifiedWayPointIndex) {
      WayPointInfo modifiedWay = wayInfos.get (modifiedWayPointIndex);
      wayInfos.remove (modifiedWay);

      int insertionIndex = 0;
      for (WayPointInfo wayInfo : wayInfos) {
         if (modifiedWay.getTime() > wayInfo.getTime()) {
            insertionIndex++;
         }
      }System.out.println(modifiedWayPointIndex + "::"+insertionIndex);

      wayInfos.add (insertionIndex, modifiedWay);
      
      for (WayPointInfo wayInfo : wayInfos) {
         wayInfo.updateWayPointIndex();
      }
      
      WayPointProbe wayProbe = myMain.getRootModel().getWayPoints();
      wayProbe.remove (modifiedWay.myWayPoint);
      wayProbe.add (modifiedWay.myWayPoint);
   }
   
   public void setCurrentCursor (Cursor cur) {
      currentCursor = cur;
   }
   
   /**
    * Gets waypoint information from the current position of the timescale 
    * and adds the waypoint to the waypoint probe.
    */
   public void addWayPointFromUser (boolean isBreakPoint) {
      addWayPoint (timescale.getTimescaleCursorTime(), isBreakPoint);
      myToolBar.validateFastForward(myMain.getRootModel());
   }
   
   /**
    * Adds a waypoint to the timelime.
    * 
    */
   public void addWayPoint (double wayTime, boolean isBreakPoint) {
      WayPointInfo tempWay = null;
      WayPoint way = getWayPoint (wayTime);
      if (way == null) {
         // Search through the waypoint list for the appropriate index
         // the list is sorted in the order of ascending waypointTime
         int insertIndex = wayInfos.size();
         for (int i = 0; i < wayInfos.size(); i++) {
            tempWay = wayInfos.get (i);
            if (wayTime < tempWay.getTime()) {
               insertIndex = i;
               break;
            }
         }

         // Create the new waypoint object
         WayPointInfo newWay = new WayPointInfo (this, wayTime);
         if (isBreakPoint) {
            newWay.myWayPoint.setBreakPoint (true);
         }

         wayInfos.add (insertIndex, newWay);

         // Update the waypoint index after insertion
         for (WayPointInfo info : wayInfos) {
            info.updateWayPointIndex();
         }
         
         wayTrack.add (newWay);
         requestUpdateDisplay();
      }
      else {
         if (way.isBreakPoint() != isBreakPoint) {
            way.setBreakPoint (isBreakPoint);
            requestUpdateDisplay();
         }
      }
   }

   private boolean wayPointsNeedRefreshing(RootModel root) {
      if (wayInfos.size() != root.getWayPoints().size()) {
         return true;
      }
      int idx = 0;
      for (WayPoint wayPoint : root.getWayPoints()) {
         if (wayInfos.get(idx++).myWayPoint != wayPoint) {
            return true;
         }
      }
      return false;
   }

   // Refresh the set of waypoint from info the RootModel. Return true if
   // changes were made, and false otherwise.
   public boolean refreshWayPoints(RootModel root) {
      myToolBar.updateToolbarState(root);
      if (!wayPointsNeedRefreshing(root)) {
         return false;
      }
      // remove all way infos ...
      Iterator<WayPointInfo> it = wayInfos.iterator();
      while (it.hasNext()) {
         WayPointInfo info = it.next();
         it.remove();
         info.finalize();
      }
      
      // now add new ones from rootModel ...
      int idx = 0;
      for (WayPoint wayPoint : root.getWayPoints()) {
         WayPointInfo info = new WayPointInfo (this, wayPoint);
         wayInfos.add (info);
         info.setIndex (idx++);
      }
      rebuildWayPointTrack();
      return true;
   }

   public void deleteWayPoints() {
      if (GuiUtils.confirmAction (this, "Delete waypoints?")) {
         clearWayPoints();
      }
   }

   public void clearWayPoints() {
      int count = wayInfos.size();
      // Delete all but the 0 waypoint.
      for (int j = 1; j < count; j++) {
         WayPointInfo info = wayInfos.get (1);
         if (info.getTime() != 0) {
            info.removeWayPointFromRoot();
            deleteWayPoint (1, true);
            info.finalize();
         }
      }
   }

   public void saveWayPoints() {
      RootModel root = myMain.getRootModel();
      if (root != null) {
         ProbeEditor.saveWayPointData (root, this);
      }
   }

   public void saveWayPointsAs() {
      RootModel root = myMain.getRootModel();
      if (root != null) {    
         WayPointProbe wayPoints = root.getWayPoints();
         String oldFileName = wayPoints.getAttachedFileName();
         if (setWayPointsFileFromUser (this, "Save As")) {
            if (!ProbeEditor.saveWayPointData (root, this)) {
               wayPoints.setAttachedFileName (oldFileName);
            }
         }
      }
   }

   public void loadWayPoints() {
      loadWayPointsFromAttachedFile (this);
   }

   public void loadWayPointsFrom() {
      WayPointProbe wayPoints = myMain.getRootModel().getWayPoints();
      String oldFileName = wayPoints.getAttachedFileName();
      if (setWayPointsFileFromUser (this, "Load")) {
         if (!loadWayPointsFromAttachedFile (this)) {
            wayPoints.setAttachedFileName (oldFileName);
         }
      }
   }

   public boolean loadWayPointsFromAttachedFile (JFrame frame) {
      RootModel root = myMain.getRootModel();
      WayPointProbe wayPoints = root.getWayPoints();
      boolean status = true;
      try {
         wayPoints.load();
      }
      catch (IOException e) {
         GuiUtils.showError (
            frame,
            "Error loading waypoints from \"" + wayPoints.getAttachedFile() +
            "\":\n" + e.getMessage());
         status = false;
      }
      refreshWayPoints(root);
      return status;
   }

  public boolean setWayPointsFileFromUser (JFrame frame, String text) {
      WayPointProbe wayPoints = myMain.getRootModel().getWayPoints();
      String workspace = new String (ArtisynthPath.getWorkingDirPath());
      File current = wayPoints.getAttachedFile();

      if (current == null) {
         current = new File (workspace);
      }

      if (myWayPointFileChooser == null) {
         myWayPointFileChooser = new JFileChooser();
      }
      if (myMain.getWayPointsFile() != null) {         
         File wayFile = myMain.getWayPointsFile();
         myWayPointFileChooser.setSelectedFile (wayFile);
         if (!wayFile.isAbsolute()) {
            myWayPointFileChooser.setCurrentDirectory (current);
         }
      }
      else {
         myWayPointFileChooser.setCurrentDirectory (current);
      }
      myWayPointFileChooser.setFileSelectionMode (JFileChooser.FILES_ONLY);
      myWayPointFileChooser.setApproveButtonText (text);
      int returnVal = myWayPointFileChooser.showDialog (frame, text);

      if (returnVal == JFileChooser.APPROVE_OPTION) {
         File file = myWayPointFileChooser.getSelectedFile();
         if (text.startsWith ("Save") && file.exists() &&
             !GuiUtils.confirmOverwrite (frame, file)) {
            return false;
         }
         String relOrAbsPath = ArtisynthPath.getRelativeOrAbsolutePath (
            ArtisynthPath.getWorkingDir(), 
            myWayPointFileChooser.getSelectedFile());
         wayPoints.setAttachedFileName (relOrAbsPath);        
         return true;
      }
      else {
         return false;
      }
   }
   
   /**
    * Gets waypoint information from the user and adds the waypoint to 
    * the waypoint probe.
    */
   public void getWayPointFromUser () {
      DoubleField myTimeField = new DoubleField("Time");
      myTimeField.addValueCheckListener (new ValueCheckListener() {
         public Object validateValue (ValueChangeEvent e, StringHolder errMsg) {
            Object val = e.getValue();
            if (val instanceof Double && ((Double)val).doubleValue() <= 0) {
               if (errMsg != null) {
                  errMsg.value = "Time value must be positive";
               }
               return Property.IllegalValue;
            }
            else {
               if (errMsg != null) {
                  errMsg.value = null;
               }
               return val;
            }
         }   
      });                  
      myTimeField.setVoidValueEnabled (true);
      myTimeField.setValue (Property.VoidValue);
      
      IntegerField myRepeatField = new IntegerField("Repeat", 1);
      myRepeatField.setRange (1, Integer.MAX_VALUE);
      BooleanSelector myBreakPointSelector = 
         new BooleanSelector ("Breakpoint", false);
      
      PropertyPanel addPanel = new PropertyPanel();
      addPanel.addWidget (myTimeField);
      addPanel.addWidget (myRepeatField);
      addPanel.addWidget (myBreakPointSelector);
      
      PropertyDialog addDialog = 
         new PropertyDialog (this, "Add waypoints", addPanel, "OK Cancel");
      addDialog.setModal (true);
      GuiUtils.locateCenter (addDialog, this);
      addDialog.setVisible (true);
      if (addDialog.getReturnValue() == OptionPanel.OK_OPTION &&
         !myTimeField.valueIsVoid()) {
         
         double t = myTimeField.getDoubleValue();
         for (int i = 1; i <= myRepeatField.getIntValue(); i++) {
            addWayPoint (t*i, myBreakPointSelector.getBooleanValue());
         }
         
         myToolBar.validateFastForward(myMain.getRootModel());
      }         
   }
   
   public void disableAllBreakPoints() {
      for (WayPointInfo info : wayInfos) {
         if (info.myWayPoint.isBreakPoint()) {
            info.myWayPoint.setBreakPoint (false);
         }
      }
      
      requestUpdateDisplay();
   }
   
   public void setAllTracksExpanded (boolean expanded) {
      for (Track t : myInTracks) {
         t.setExpanded (expanded);
      }

      for (Track t : myOutTracks) {
         t.setExpanded (expanded);
      }
      
      requestUpdateDisplay();
   }
   
   
   ////////////////////////// Getters and Accessors //////////////////////////////////
   
   public Cursor getCurrentCursor() {
      return currentCursor;
   }
   
   public int getZoomLevel() {
      return zoomLevel;
   }

   public WayPoint getWayPoint (double time) {
      return getWayPoints().get (time);
   }

   private WayPointProbe getWayPoints() {
      return myMain.getRootModel().getWayPoints();
   }

   public double getCurrentTime() {
      return myScheduler.getTime();
   }

   public Timescale getTimescale() {
      return timescale;
   }

   /**
    * Returns the bounds for which the timeline is visible.
    */
   public int[] getVisibleBound() {
      int[] bounds = new int[2];
      bounds[0] = timelineScrollPane.getHorizontalScrollBar().getValue();
      bounds[1] = bounds[0] + 
         timelineScrollPane.getHorizontalScrollBar().getVisibleAmount();

      return bounds;
   }
   
   /**
    * Checks to see if there are any valid waypoints after the current time.
    * @param root TODO
    */
   public boolean isNextValidWayAvailable(RootModel root) {
      return root.getWayPoints().getValidAfter (getCurrentTime()) != null;
   }
   
   public boolean rootModelExists() {
      return (myMain.getWorkspace().getRootModel() == null ? false : true);
   }
   
   public boolean isActiveWayPointExist() {
      return activeWayPointExists;
   }
   
   public boolean isActiveProbeExist() {
      return activeProbeExists;
   }
   
   public void setTrackExpanded (
      boolean expanded, boolean isInput, int modelIdx, int trackIdx) {
      
      Track track = 
         isInput ? myInTracks.get (trackIdx) : myOutTracks.get (trackIdx);
      track.setExpanded (expanded);
   }

   public void setTrackMuted(boolean muted, boolean isInput, int modelIdx,
	 int trackIdx) {
      Track track = isInput ? myInTracks.get(trackIdx) : myOutTracks.get(trackIdx);
      track.muteTrack(muted);
   }
   
   public void updateTimelineScroll() {
      // Determine the current pixel location along the timescale
      int cursorLoc = getTimescale ().getCurrentPixel();
      
      // Retrieve the necessary information of the scroll bar
      int scrollLoc = 
         timelineScrollPane.getHorizontalScrollBar().getValue();
      int scrollExtent =
         timelineScrollPane.getHorizontalScrollBar().getVisibleAmount();
      int scrollMax = scrollLoc + scrollExtent;

      // If the current time approaches/exceeds the bounds of the active
      // window, advance/decrement the scroll accordingly
      int scrollValue = -1;
      
      // if the current time approaches/exceeds the bounds of the active
      // window, advance/decrement the scroll accordingly.
      // we handle things differently depending on whether the scroll 
      // bar is being manually dragged
      
      int cursorMax = scrollMax-SCROLL_OFFSET;
      int cursorMin = scrollLoc+Math.min(SCROLL_OFFSET,scrollLoc);
      int loOffset = 0;
      if (cursorLoc > cursorMax) {
         if (timescale.isTimeManuallyDragged()) {
            scrollValue = scrollLoc + (cursorLoc - cursorMax);
         }
         else {
            scrollValue = cursorLoc - SCROLL_OFFSET;
         }
      }
      else if (cursorLoc < cursorMin) {
         if (timescale.isTimeManuallyDragged()) {
            scrollValue = scrollLoc + (cursorLoc - cursorMin);
         }
         else {
            scrollValue = cursorLoc-scrollExtent+this.SCROLL_OFFSET;
         }
         if (scrollValue < 0) {
            scrollValue = 0;
         }
      }

      // Original settings by Michelle: create a continuously moving
      // timeline with the cursor in one place

//       if (cursorLoc >= (scrollMax - SCROLL_OFFSET)) {
//          scrollValue = scrollLoc + SCROLL_OFFSET - 
//             (scrollMax - cursorLoc);
//       }
//       else if (cursorLoc <= (scrollLoc + SCROLL_OFFSET)) {
//          scrollValue = cursorLoc - SCROLL_OFFSET;
//       }
      
      if (scrollValue != -1) {
         timelineScrollPane.getHorizontalScrollBar ().setValue (scrollValue);
      }
   }

//    public void setSelectionListener (SelectionListener listener) {
//       mySelectionListener = listener;
//    }
   
//    public SelectionListener getSelectionListener() {
//       return mySelectionListener;
//    }
   
   /**
    * add a track to the model on the left of the split pane in timeline
    * controller
    * 
    * @param trackType
    */

   private Track createAndAddTrack (int trackType) {
      Track newTrack = null;
      
      // if the type of track is input
      if (trackType == Track.TYPE_INPUT) {
         int trackIndex = myInTracks.size();
         newTrack = new Track (trackType, Integer.toString (trackIndex), this);
         myInTracks.add (newTrack);
         updateTrackIndices (myInTracks);
      }
      // if the track is output
      else if (trackType == Track.TYPE_OUTPUT) {
         int trackIndex = myOutTracks.size();
         newTrack = new Track (trackType, Integer.toString (trackIndex), this);
         myOutTracks.add (newTrack);
         updateTrackIndices (myOutTracks);
      }

      probesPane.add (newTrack);

      rebuildTimeline();
      return newTrack;
   }
   
   private void updateTrackIndices (ArrayList<Track> trackList) {
      for (int i = 0; i < trackList.size(); i++) {
         trackList.get (i).setIndex (i);
      }
   }
   
   /**
    * find the track to which to add the probe
    * 
    * @version 2.0 need to add an ability to use probe's track information
    * loaded form a file
    * @version 3.0 Andrei: added the ability of the track number to be loaded
    * from a file. Now empty tracks are not created, and probes can be loaded
    * from a file.
    * @param probe
    * @return track
    */

   private Track findTrackForProbe (Probe probe) {
      int preferredTrack = probe.getTrack();

      for (Track track : (probe.isInput ()) ? myInTracks : myOutTracks) {
         if (track.hasSpaceForProbe (probe) &&
             (preferredTrack == -1 ||
              preferredTrack == track.getTrackNumber())) {
            return track;
         }
      }
      
      return null;
   }
   
   void deleteTrack (int trackType, int trackIndex, boolean requireConfirm) {
      
      if (!requireConfirm || confirmAction ("Delete this track?")) {
         // delete everything associated with the track to be deleted,
         // also, remove it from this model's track list
         Track track = null;
         if (trackType == Track.TYPE_INPUT) {
            track = myInTracks.remove (trackIndex);
            updateTrackIndices (myInTracks);
         }
         else if (trackType == Track.TYPE_OUTPUT) {
            track = myOutTracks.remove (trackIndex);
            updateTrackIndices (myOutTracks);
         }
         else {
            throw new InternalErrorException (
               "Unimplemented track type: " + trackType);
         }
         
         track.dispose();
         rebuildTimeline();
      }
   }

   public void rebuildWayPointTrack () {
      // remove all indicators from the track
      wayTrack.removeAll();

      // add the remaining wayMarkers
      for (WayPointInfo wayInfo : wayInfos) {
         wayTrack.add (wayInfo);
      }
   }
   
   public Track getProbeTrack (Probe probe) {
      for (Track track : (probe.isInput() ? myInTracks : myOutTracks)) {
         if (track.getProbeIndex (probe) != -1) {
            return track;
         }
      }

      return null;
   }
   
   public void refreshProbeTrackDisplay() {
      probeTrack.removeAll();

      for (Track inTrack : myInTracks) {
         if (inTrack.isVisible()) {
            for (ProbeInfo probeInfo : inTrack.getProbeInfos()) {
               probeInfo.setAppropSizeAndLocation (true);
               probeTrack.add (probeInfo.getDisplayArea());
            }
         }
      }

      for (Track outTrack : myOutTracks) {
         if (outTrack.isVisible()) {
            for (ProbeInfo probeInfo : outTrack.getProbeInfos()) {
               probeInfo.setAppropSizeAndLocation (true);
               probeTrack.add (probeInfo.getDisplayArea());
            }
         }
      }
   }
   
   public Track getTrackAtYCoor (int yCoor, boolean isInputTrack) {
      if (yCoor < 0) {
         yCoor = 0;
      }

      int trackBounds[] = { 0, 0 };
      Track track = null;

      for (Track inTrack : myInTracks) {         
         if (inTrack.isVisible()) {
            track = inTrack;
            
            trackBounds[1] += (inTrack.isExpanded()) ? 
               GuiStorage.TRACK_EXPAND_HEIGHT : GuiStorage.TRACK_NORMAL_HEIGHT;
            
            if (yCoor >= trackBounds[0] && yCoor <= trackBounds[1]) {
               break;
            }

            trackBounds[0] = trackBounds[1];
         }
      }
      
      if (!isInputTrack) {
         for (Track outTrack : myOutTracks) {         
            if (outTrack.isVisible()) {
               track = outTrack;
               
               trackBounds[1] += (outTrack.isExpanded()) ? 
                  GuiStorage.TRACK_EXPAND_HEIGHT : GuiStorage.TRACK_NORMAL_HEIGHT;

               if (yCoor >= trackBounds[0] && yCoor <= trackBounds[1]) {
                  break;
               }

               trackBounds[0] = trackBounds[1];
            }
         }
      }

      return track;
   }
   
   /**
    * compute Y coordinate to track
    * 
    * @param curTrack current track
    * @return Y coordinate to track
    */

   public int getYCoorUpToTrack (Track curTrack) {
      int trackYCoor = 0;

      for (Track inTrack : myInTracks) {
         if (inTrack == curTrack) {
            break;
         }

         if (inTrack.isVisible()) {
            trackYCoor += (inTrack.isExpanded()) ? 
               GuiStorage.TRACK_EXPAND_HEIGHT : GuiStorage.TRACK_NORMAL_HEIGHT;
         }
      }

      if (!curTrack.isInputTrack()) {
         for (Track outTrack : myOutTracks) {
            if (outTrack == curTrack) {
               break;
            }

            if (outTrack.isVisible()) {
               trackYCoor += (outTrack.isExpanded()) ? 
                  GuiStorage.TRACK_EXPAND_HEIGHT : GuiStorage.TRACK_NORMAL_HEIGHT;
            }
         }
      }

      return trackYCoor;
   }
   
   public Track getTrack (int trackIdx, boolean isInput) {
      if (trackIdx < 0) {
         trackIdx = 0;
      }
      
      Track track = null;
      
      for (Track curTrack : isInput ? myInTracks : myOutTracks) {
         track = curTrack;
         
         if (curTrack.getTrackNumber () == trackIdx) {
            break;
         }
      }
      
      return track;
   }
   
   public ProbeTrack getProbeTrack() {
      return probeTrack;
   }
   
   public void addContiguousTrack (Track src) {
      // check if the src is part of a set of grouped tracks
      Track groupParent = src.getGroupParent();

      if (groupParent == null) {
         // src is not a part of a set of grouped tracks,
         // just select the src by itself
         addContiguousTrackHelper (src);
      }
      else {
         // if the src is part of a set of grouped tracks select all of the
         // grouped tracks
         ArrayList<Track> tracksToSelect = getTrackGroup (groupParent);

         // try adding the first track, if it works iterate forwards, if it
         // doesn't iterate backwards
         boolean trackAdded = addContiguousTrackHelper (tracksToSelect.get (0));

         if (trackAdded) {
            for (Track track : tracksToSelect) {
               addContiguousTrackHelper (track);
            }
         }
         else {
            for (int i = tracksToSelect.size() - 1; i >= 0; i--) {
               addContiguousTrackHelper (tracksToSelect.get (i));
            }
         }

      }
   }
   
   /**
    * Add a track to the list of contiguously selected tracks in the timeline.
    * 
    * @param src
    * The track to add to the list of selected tracks.
    */
   private boolean addContiguousTrackHelper (Track src) {
      // get the index of the track, make sure it is contiguous with the tracks
      // we already have
      int index;

      if (src.isInputTrack()) {
         index = myInTracks.indexOf (src);
      }
      else {
         index = myOutTracks.indexOf (src);
      }

      if (selectedTracks.size() == 0) {
         firstIndex = index;
         lastIndex = index;
         selectTrack (src, 0);
         return true;
      }
      else if (index == (firstIndex - 1)) {
         firstIndex--;
         selectTrack (src, 0);
         return true;
      }
      else if (index == (lastIndex + 1)) {
         lastIndex++;
         selectTrack (src, selectedTracks.size());
         return true;
      }

      return false;
   }
   
   public void removeContiguousTrack (Track src) {
      // check if the src is part of a set of grouped tracks
      Track groupParent = src.getGroupParent();

      if (groupParent == null) {
         // the src is not a part of a set of grouped tracks, just select the
         // src by itself
         removeContiguousTrackHelper (src);
      }
      else {
         // if the src is part of a set of grouped tracks select all of the
         // grouped tracks
         ArrayList<Track> tracksToSelect = getTrackGroup (groupParent);

         // try removing the first track, if it works iterate forwards, if it
         // doesn't iterate backwards
         boolean trackRemoved =
            removeContiguousTrackHelper (tracksToSelect.get (0));

         if (trackRemoved) {
            for (Track track : tracksToSelect) {
               removeContiguousTrackHelper (track);
            }
         }
         else {
            for (int i = tracksToSelect.size() - 1; i >= 0; i--) {
               removeContiguousTrackHelper (tracksToSelect.get (i));
            }
         }
      }
   }
   
   /**
    * Remove a track from the list of contiguously selected tracks in the
    * timeline.
    * 
    * @param src
    */
   private boolean removeContiguousTrackHelper (Track src) {
      int index;
      if (src.isInputTrack()) {
         index = myInTracks.indexOf (src);
      }
      else {
         index = myOutTracks.indexOf (src);
      }

      // the track is on either end of the selected tracks and can be removed
      // from the contiguous block
      if (selectedTracks.size() == 1) {
         // there is only one track left to remove
         firstIndex = 0;
         lastIndex = 0;
         deselectTrack (src);
         return true;
      }
      else if (index == firstIndex) {
         firstIndex++;
         deselectTrack (src);
         return true;
      }
      else if (index == lastIndex) {
         lastIndex--;
         deselectTrack (src);
         return true;
      }

      return false;
   }
   
   private void selectTrack (Track src, int index) {
      src.select();
      selectedTracks.add (index, src);
   }

   private void deselectTrack (Track src) {
      src.deselect();
      selectedTracks.remove (src);
   }
   
   /**
    * Determine if a track is part of the list of contiguous tracks or not.
    * 
    * @param src
    * The track to test if it is part of the contiguous list.
    * @return True if the track is in the list of contiguous tracks, false
    * otherwise.
    */
   public boolean isContiguousTrack (Track src) {
      return selectedTracks.contains (src);
   }
   
   public void removeAllContiguousTracks() {
      for (Track track : selectedTracks) {
         track.deselect();
      }

      selectedTracks.clear();

      firstIndex = 0;
      lastIndex = 0;
   }
   
   /**
    * Set the value of the track that was last entered by the mouse.
    * 
    * The src track is the track to be set as the currentTrack. If the src track
    * is part of a group of tracks and the currentTrack is also part of the
    * group then the currentTrack does not change. If the src track is in a new
    * group of tracks then the last track in that group is set as the
    * currentTrack. If the src track is a single track then it is set as the
    * currentTrack.
    * 
    * @param src
    * The track that was entered.
    * @param group
    * If true then treat groups of tracks as one single track, if false then
    * treate all tracks as single.
    */
   public Track setLastEntered (Track src, boolean group) {
      Track srcGroupParent = src.getGroupParent();

      if (!group || srcGroupParent == null) {
         currentTrack = src;
      }
      else if (srcGroupParent != null) {
         // we are entering a new set of grouped tracks
         // set the last track in the group as the last entered track
         ArrayList<Track> enteredTracks = getTrackGroup (src.getGroupParent());
         currentTrack = enteredTracks.get (enteredTracks.size() - 1);
      }

      if (!currentTrack.isVisible()) {
         // if the current track is not visible then set the current track to
         // the parent of the group
         currentTrack = currentTrack.getGroupParent();
      }

      return currentTrack;
   }
   
   /**
    * Get the group of tracks that has the specified track as the first one in
    * the group.
    * 
    * @param firstTrack
    * This must be the first track in the group of tracks that is being searched
    * for.
    * @return associated track group
    */
   public ArrayList<Track> getTrackGroup (Track firstTrack) {
      for (ArrayList<Track> tracksToFind : groupedTracks) {
         if (tracksToFind.get (0) == firstTrack) {
            return tracksToFind;
         }
      }

      return new ArrayList<Track>();
   }
   
   public Track getCurrentlyEnteredTrack() {
      return currentTrack;
   }
   
   public boolean groupTracks() {
      if (selectedTracks.size() <= 1) {
         return false;
      }

      Track firstTrack = selectedTracks.get (0);

      // iterate over the tracks setting their grouped value to true
      for (Track track : selectedTracks) {
         Track parentGroupTrack = track.getGroupParent();

         if (parentGroupTrack != null) {
            // this track is part of a group, remove that group
            ungroupTracks (parentGroupTrack);
         }

         // now we can safely group the track in
         track.setGrouped (firstTrack);
      }

      // stick the tracks in the grouped array
      // add the button for show/hide grouped tracks
      groupedTracks.add ((ArrayList<Track>) selectedTracks.clone());
      firstTrack.addShowHideButton (false);

      return true;
   }
   
   public void ungroupTracks (Track src) {
      // first make sure the tracks are showing
      showContiguousTracks (src);

      // find the tracks to ungroup and ungroup them
      ArrayList<Track> tracksToUngroup = getTrackGroup (src);

      for (Track track : tracksToUngroup) {
         track.setGrouped (null);
      }

      // enable the first track in the list
      tracksToUngroup.get (0).setEnabled (true);

      groupedTracks.remove (tracksToUngroup);
      tracksToUngroup.get (0).removeShowHideButton();
      
      rebuildTimeline();
   }
   
   public void showContiguousTracks (Track src) {
      ArrayList<Track> tracksToShow = getTrackGroup (src);

      // go through the set of grouped tracks and hide them all
      for (Track track : tracksToShow) {
         track.setVisible (true);
      }

      // add the button to the first track
      tracksToShow.get (0).addShowHideButton (false);      

      rebuildTimeline();
   }
   
   /**
    * Hide the contiguous tracks that are grouped with the src track.
    * 
    * @param src
    * The first track in the block of contiguous tracks to be hidden.
    */
   public void hideContiguousTracks (Track src) {
      ArrayList<Track> tracksToHide = getTrackGroup (src);

      // go through the set of grouped tracks and hide them all
      for (Track track : tracksToHide) {
         track.setVisible (false);
      }

      // reshow the first track
      tracksToHide.get (0).setVisible (true);

      rebuildTimeline();
   }
   
   /**
    * When the left mouse button is released on a track this function is called
    * to move the tracks around accordingly.
    * 
    * @return True if tracks were moved around, false if tracks could not be
    * moved becuase input and output tracks were being mixed.
    */
   public boolean mouseReleased() {
      ArrayList<Track> trackList = null;

      // get the correct track list for selection
      if (currentTrack.isInputTrack() && selectedTracks.size() > 0 && 
          selectedTracks.get (0).isInputTrack()) {
         trackList = myInTracks;
      }
      else if (!currentTrack.isInputTrack() && selectedTracks.size() > 0 && 
               !selectedTracks.get (0).isInputTrack()) {
         trackList = myOutTracks;
      }

      if (trackList != null) {
         // get the correct index to add the current track to
         // if the current track is not in a group it is the index of the track
         int currentIndex = trackList.indexOf (currentTrack);

         if (currentTrack.getGroupParent() != null) {
            // if the current track is in a group the index is the index of the
            // last track in that group
            Track groupParentTrack = currentTrack.getGroupParent();
            ArrayList<Track> trackGroup = getTrackGroup (groupParentTrack);

            currentIndex =
               trackList.indexOf (trackGroup.get (trackGroup.size() - 1));
         }

         if (currentIndex > lastIndex) {
            for (Track movingTrack : selectedTracks) {
               trackList.remove (movingTrack);
               trackList.add (currentIndex, movingTrack);
            }

         }
         else if (currentIndex < firstIndex) {
            for (int i = (selectedTracks.size() - 1); i >= 0; i--) {
               Track movingTrack = selectedTracks.get (i);
               trackList.remove (movingTrack);
               trackList.add (currentIndex + 1, movingTrack);
            }
         }
         
         rebuildTimeline();
         return true;
      }

      return false;
   }
   
   private void rebuildTimeline() {
      probesPane.removeAll ();
      
      for (Track inTracks : myInTracks) {
         if (inTracks.isVisible()) {
            probesPane.add (inTracks);
         }
      }

      for (Track outTracks : myOutTracks) {
         if (outTracks.isVisible()) {
            probesPane.add (outTracks);
         }
      }

      updateComponentSizes();

      // refresh the probe track display so that hidden tracks are not shown
      refreshProbeTrackDisplay();

      requestUpdateDisplay();
   }
   
   public void updateCurrentWayPointShadow (int x) {
      myWayShadow.setLine (x, 1, x, GuiStorage.WAY_MARKER_SIZE.height);
   }
   
//    public void deselectAllProbes() {
//       while (!selectedProbes.isEmpty ()) {
//          ProbeInfo pInfo = selectedProbes.get (0);
//          System.out.println ("pinfo=" + pInfo);
//          pInfo.getProbe().setSelected (false);
//          pInfo.setSelected (false);
//       }
//    }
   
   public boolean executeDrop (int yCoor, ProbeInfo pInfo, Track origTrack) {
      Track activeTrack = getTrackAtYCoor (yCoor, origTrack.isInputTrack());
      boolean isExecuted = activeTrack.isDragValid (pInfo, true);

      // if the drop does not conflict with the other probes
      // then execute the drop
      if (isExecuted) {
         if (origTrack != activeTrack) {
            origTrack.detachProbeInfo (pInfo);
            activeTrack.attachProbeInfo (pInfo);
            refreshProbeTrackDisplay();

            // set the title of the probe on the new track
            pInfo.updateLabelText();
            pInfo.getProbe().setTrack (activeTrack.getTrackNumber());
         }
         else {
            origTrack.markProbesUnsorted();
         }
      }

      return isExecuted;
   }

   public void schedulerActionPerformed (Scheduler.Action action) {
      switch (action) {
         case Reset: {
            requestUpdate (UPDATE_WIDGETS | REFRESH_CURSOR);
            break;
         }
         case Rewind: {
            requestUpdate (UPDATE_WIDGETS | REFRESH_CURSOR);
            break;
         }
         case Play: {
//             setAllMutable (!myScheduler.isPlaying());
//             myToolBar.updateToolbarState ();
//            updateTimelineScroll();
            requestUpdateWidgets();
            break;
         }
         case Pause: {
            requestUpdateWidgets();
            //setAllMutable (!myScheduler.isPlaying());
            //myToolBar.updateToolbarState ();
            break;
         }
         case Stopped: {
            //System.out.println ("stop " + getCurrentTime());
            requestUpdateWidgets();
            break;
         }
         case Step: {
            requestUpdateWidgets();
            //myToolBar.updateToolbarState ();
            //setAllMutable (!myScheduler.isPlaying());            
            break;
         }
         case Forward: {
            requestUpdate (UPDATE_WIDGETS | REFRESH_CURSOR);
            break;
         }
         case Advance: {
            // XXX fix
            timescale.updateTimeCursor (getCurrentTime());
            requestUpdateWidgets();
            break;
         }
//         case StepTimeSet: {
//            double newStep = myScheduler.getStepTime();
//            if (myToolBar.getStepTime() != newStep) {
//               requestUpdateWidgets();
//            }
//            break;
//         }
      }
   }

   public double getLargestProbeTime() {
      double max = 0;

      for (Track inTrack : myInTracks) {
         for (ProbeInfo probeInfo : inTrack.getProbeInfos()) {
            double t = probeInfo.getStopTime();
            if (t > max) {
               max = t;
            }
         }            
      }

      for (Track outTrack : myOutTracks) {
         for (ProbeInfo probeInfo : outTrack.getProbeInfos()) {
            double t = probeInfo.getStopTime();
            if (t > max) {
               max = t;
            }
         }            
      }

      return max;
   }

   public int getNumberOfWayPoints() {
      return myMain.getRootModel().getWayPoints().size();
   }

   public double getLargestWayPointTime() {
      return myMain.getRootModel().getWayPoints().maxEventTime();
   }

   public void extendTimeline() {
      timelineDuration *= 2;
      timescale.updateTimescaleSizeAndScale (timelineDuration, zoomLevel);
      updateComponentSizes();
      requestUpdateDisplay();
   }

   public void setAllInputProbes() {
      double cursorTime = timescale.getTimescaleCursorTime();

      for (Track inTrack : myInTracks) {
         for (ProbeInfo probeInfo : inTrack.getProbeInfos()) {
            probeInfo.setProbeWithTime (cursorTime);
         }
      }
   }

//   public void saveAllProbes() {
//      RootModel root = myMain.getRootModel();
//      if (root != null) {  // paranoid
//         ProbeEditor.saveAllOutputData (root, this);
//      }
//      // Save waypoint datafile
//      // Lloyd, Oct 2020: probably want to save waypoints *separately*
//      //saveWayPoints();
//   }

   protected void saveOutputProbeData() {
      RootModel root = myMain.getRootModel();
      if (root != null) {  // paranoid
         ProbeEditor.saveAllOutputData (root, this);
      }
   }

   public void zoom (int mode) {
      if (mode == TimelineConstants.ZOOM_IN) {
         zoomLevel++;
      }
      else if (mode == TimelineConstants.ZOOM_OUT) {
         zoomLevel--;
      }

      myToolBar.updateToolbarState (myMain.getRootModel());
      timescale.updateTimescaleSizeAndScale (timelineDuration, zoomLevel);
      updateComponentSizes();
      updateComponentLocations();
      setAllNumericDisplays();

      // Reset the position of the knob of the scroll bar         
      double scrollMax = timelineScrollPane.getHorizontalScrollBar().getMaximum();
      double scrollMin = timelineScrollPane.getHorizontalScrollBar().getMinimum();
      double currentScrollValue = 
         timelineScrollPane.getHorizontalScrollBar().getValue();

      timelineScrollPane.getHorizontalScrollBar().setValue (
         (int) (scrollMin + (scrollMax - scrollMin) * 
         (currentScrollValue - scrollMin) / (scrollMax - scrollMin)));
   }
   
   // =============================================
   // ProbeTrack panel class
   // =============================================

   protected class ProbeTrack extends JPanel {
      private static final long serialVersionUID = 1L;

      public ProbeTrack() {
         setAlignmentX (JComponent.LEFT_ALIGNMENT);
         setLayout (null);
         setBackground (GuiStorage.COLOR_PROBE_TRACK);

         Dimension defaultSize = new Dimension (
            GuiStorage.DEFAULT_TRACK_LENGTH, GuiStorage.TRACK_NORMAL_HEIGHT);

         setSize (defaultSize);
         setPreferredSize (defaultSize);
         setMinimumSize (defaultSize);
         setMaximumSize (defaultSize);
      }
      
      public void paint (Graphics g) {
         super.paint (g);

         int inProbeTrackHeight = 0;
         int outProbeTrackHeight = 0;
         int wayPixel = 0;

         int visibleRange[] = getVisibleBound();

         // draw the waypoint markers on the probe track
         for (WayPointInfo wayInfo : wayInfos) {
            wayPixel = timescale.getCorrespondingPixel (wayInfo.getTime());
            g.setColor (Color.GRAY);
            g.drawLine (wayPixel, 0, wayPixel, getHeight() - 1);
         }
         
         // draw the track lines
         g.setColor (Color.BLACK);
         for (Track inTrack : myInTracks) {
            if (inTrack.isVisible()) {
               g.drawLine (
                  visibleRange[0], inProbeTrackHeight, visibleRange[1],
                  inProbeTrackHeight);

               inProbeTrackHeight +=
                  (inTrack.isExpanded()) ? GuiStorage.TRACK_EXPAND_HEIGHT
                     : GuiStorage.TRACK_NORMAL_HEIGHT;

               g.drawLine (
                  visibleRange[0], inProbeTrackHeight - 1, visibleRange[1],
                  inProbeTrackHeight - 1);
            }
         }

         for (Track outTrack : myOutTracks) {
            if (outTrack.isVisible()) {
               g.drawLine (visibleRange[0], outProbeTrackHeight
               + inProbeTrackHeight, visibleRange[1], outProbeTrackHeight
               + inProbeTrackHeight);

               outProbeTrackHeight +=
                  (outTrack.isExpanded()) ? GuiStorage.TRACK_EXPAND_HEIGHT
                     : GuiStorage.TRACK_NORMAL_HEIGHT;

               g.drawLine (visibleRange[0], outProbeTrackHeight
               + inProbeTrackHeight - 1, visibleRange[1], outProbeTrackHeight
               + inProbeTrackHeight - 1);
            }
         }
         
         // Draw probe shadows when probes are selected and dragging
         if (isActiveProbeExist()) {
            Graphics2D g2d = (Graphics2D) g;
            
            for (ProbeInfo pInfo : selectedProbes) {
               g2d.setColor ((pInfo.getIsShadowValid ()) ? Color.GRAY : Color.RED);
               g2d.draw (pInfo.getProbeShadow ());
            }
         }
      }
   }
   
   // ======================================================
   // TimelinePane panel class
   // ======================================================
   
   private class TimelinePane extends JPanel implements MouseMotionListener {
      private static final long serialVersionUID = 1L;

      // Space b/w top boundary and top of timescale
      private final static int TOPBOUNDARYSPACER = 1;
      // Space b/w bottom of timescale and the new start point
      private final static int BOTBOUNDARYSPACER = 3;
      // Height of model track
      private final static int MODELTRACKHEIGHT = 20;
      // Height of space b/w adjacent model tracks
      private final static int MODELTRACKSPACER = 3;

      public TimelinePane (TimelineController controller) {
         setLayout (new BoxLayout (this, BoxLayout.Y_AXIS));
         setAlignmentX (JComponent.LEFT_ALIGNMENT);
         setBackground (Color.WHITE);

         // enable scrolling when the user drags the cursor outside window
         setAutoscrolls (true);
         addMouseMotionListener (this);
      }

      /**
       * Extends the functionality of the paint operation to also paint the Knob
       */
      public void paint (Graphics g) {
         super.paint (g);
         int currentKnobPosition = timescale.getCurrentPixel();
         g.drawLine (currentKnobPosition, 0,
                     currentKnobPosition, getHeight());
      }

      /**
       * Is responsible for setting the dimensions of the panes, based upon the
       * parameter the has been received for length, and the calculated height or
       * the TimelinePane.
       * 
       * @param newLength
       * contains the width of the pane as provided by the timescale
       */
      public void updateTimelinePaneSize (int newLength) {
         Dimension size = new Dimension (newLength, 
            TOPBOUNDARYSPACER + Timescale.HEIGHT + BOTBOUNDARYSPACER + 
            MODELTRACKHEIGHT + MODELTRACKSPACER + getProbeTrackHeight());
         
         setSize (size);
         setMinimumSize (size);
         setMaximumSize (size);
         setPreferredSize (size);
      }   
      
      public void mouseDragged (MouseEvent e) {
         Rectangle r = new Rectangle (e.getX(), e.getY(), 1, 1);
         scrollRectToVisible (r);
      }

      public void mouseMoved (MouseEvent e) {}
   }

   // ======================================================
   // WayPointTrack panel class
   // ======================================================
   
   private class WayPointTrack extends JPanel {
      private static final long serialVersionUID = 1L;

      public WayPointTrack() {
         setAlignmentX (JComponent.LEFT_ALIGNMENT);
         setLayout (null);
         setBackground (GuiStorage.COLOR_MODEL_TIMELINE);

         Dimension defaultSize = new Dimension (
            GuiStorage.DEFAULT_TRACK_LENGTH, GuiStorage.TRACK_NORMAL_HEIGHT);

         setSize (defaultSize);
         setPreferredSize (defaultSize);
         setMinimumSize (defaultSize);
         setMaximumSize (defaultSize);
      }

      public void paint (Graphics g) {
         super.paint (g);

         // draw the upper and lower borders
         g.setColor (Color.BLACK);
         g.drawLine (0, 0, getWidth(), 0);
         g.drawLine (0, getHeight() - 1, getWidth(), getHeight() - 1);

         // if dragging is going on, then show the waypoint shadow as well
         Graphics2D g2D = (Graphics2D)g;
         g2D.setColor (Color.BLACK);
         
         if (isActiveWayPointExist()) {
            g2D.draw (myWayShadow);
         }
      }
   }
   
   // ======================================================
   // WayPointTrackListener class
   // ======================================================
   
   private class WayPointTrackListener extends MouseAdapter implements ActionListener {
      public void actionPerformed (ActionEvent e) {
         String nameOfAction = e.getActionCommand();

         if (nameOfAction == "Add waypoint here") {
            addWayPointFromUser (/* breakpoint= */false);
         }
         else if (nameOfAction == "Add breakpoint here") {
            addWayPointFromUser (/* breakpoint= */true);
         }
         else if (nameOfAction == "Add waypoint(s) ...") {
            getWayPointFromUser ();
         }
         else if (nameOfAction == "Disable all breakpoints") {
            disableAllBreakPoints();
         }
         else if (nameOfAction == "Load probes ...") {
            ProbeEditor.loadProbes (myMain, TimelineController.this);
         }
         else if (nameOfAction == "Save probes") {
            ProbeEditor.saveProbes (myMain, TimelineController.this);
         }
         else if (nameOfAction == "Save probes as ...") {
            ProbeEditor.saveProbesAs (myMain, TimelineController.this);
         }
         else if (nameOfAction == "Save output probe data") {
            saveOutputProbeData();
         }
         else if (nameOfAction == "Delete waypoints") {
            deleteWayPoints();
         }
         else if (nameOfAction == "Save waypoints") {
            saveWayPoints();
         }
         else if (nameOfAction == "Save waypoints as ...") {
            saveWayPointsAs();
         }
         else if (nameOfAction == "Load waypoints ...") {
            loadWayPointsFrom();
         }
         else if (nameOfAction == "Reload waypoints") {
            loadWayPoints();
         }
         else if (nameOfAction == "Add input track") {
            createAndAddTrack (Track.TYPE_INPUT);
         }
         else if (nameOfAction == "Add output track") {
            createAndAddTrack (Track.TYPE_OUTPUT);
         }
         
         requestUpdateDisplay();
      }

      public void mouseReleased (MouseEvent e) {
         if (e.isPopupTrigger()) {
            JPopupMenu popup = createPopupMenu();
            popup.show (e.getComponent(), e.getX(), e.getY());
         }
      }

      public void mousePressed (MouseEvent e) {
         if (e.isPopupTrigger()) {
            JPopupMenu popup = createPopupMenu();
            popup.show (e.getComponent(), e.getX(), e.getY());
         }
      }
   }

   public void setVisible (boolean visible) {
      super.setVisible (visible);
      if (visible) {
         requestUpdateWidgets();
      }
   }

   public void selectionChanged (SelectionEvent e) {
      for (ModelComponent c : e.getAddedComponents()) {
         if (c instanceof Probe) {
            ProbeInfo info = myProbeMap.get((Probe)c);
            selectedProbes.add (info);
         }
      }
      for (ModelComponent c : e.getRemovedComponents()) {
         if (c instanceof Probe) {
            ProbeInfo info = myProbeMap.get((Probe)c);
            selectedProbes.remove (info);
         }
      }
   }

   public ArrayList<ProbeInfo> getSelectedProbes() {
      return selectedProbes;
   }

   protected int myMultipleSelectionMask = InputEvent.CTRL_DOWN_MASK;
   
   public int getMultipleSelectionMask() {
      return myMultipleSelectionMask;
   }

   public void setMultipleSelectionMask (int mask) {
      myMultipleSelectionMask = mask;
   }

   boolean isMultipleSelectionEnabled (MouseEvent e) {
      int mask = myMultipleSelectionMask;
      return (e.getModifiersEx() & mask) == mask;
   }

   public NumericProbeDisplayLarge setLargeDisplayVisible (
      Probe probe, boolean visible) {
      
      ProbeInfo info = myProbeMap.get(probe);
      if (info != null) {
         return info.setLargeDisplayVisible (visible);
      }
      else {
         return null;
      }
   }

   public void dispose() {
      if (myRequestTimer != null) {
         myRequestTimer.cancel();
         myRequestTimer = null;
      }
      super.dispose();
   }

}
