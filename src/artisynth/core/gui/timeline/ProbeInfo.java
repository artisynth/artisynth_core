package artisynth.core.gui.timeline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.*;

import maspack.util.Clonable;
import maspack.util.InternalErrorException;
import maspack.widgets.GuiUtils;
import artisynth.core.driver.Main;
import artisynth.core.driver.Scheduler;
import artisynth.core.gui.Displayable;
import artisynth.core.gui.NumericProbeDisplayLarge;
import artisynth.core.gui.NumericProbePanel;
import artisynth.core.gui.editorManager.AddComponentsCommand;
import artisynth.core.gui.editorManager.ProbeEditor;
import artisynth.core.gui.probeEditor.InputNumericProbeEditor;
import artisynth.core.gui.probeEditor.NumericProbeEditor;
import artisynth.core.gui.probeEditor.OutputNumericProbeEditor;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.gui.widgets.ProbeExportChooser;
import artisynth.core.modelbase.MutableCompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.probes.Probe;
import artisynth.core.probes.WayPoint;
import artisynth.core.probes.WayPointProbe;
import artisynth.core.util.MatlabInterface;
import artisynth.core.util.TimeBase;
import artisynth.core.util.ArtisynthPath;

/**
 * @author Paul
 * @version 2.0 Revised March 10th by John Lloyd and Chad Decker
 */
public class ProbeInfo implements Clonable, ActionListener {
   private static boolean stretchFromStartP = false;
   private static boolean stretchFromStopP = false;
   private static boolean moveP = false;
   private static boolean cropFromStartP = false;
   private static boolean cropFromStopP = false;
   
   //private boolean selected = false;
   boolean myMark = false; // used to check for changes in the probe set
   
   private Probe myProbe;
   private TimelineController myController;

   private Track myTrack;
   private boolean draggingP;
   private int myIndex;
   private int myStopPixel;
   private int myStartPixel;
   private double oldStartTime, oldStopTime;
   private JLabel myLabel;
   private ProbeDisplayArea myDisplayArea;
   private ProbeMouseHandler mouseHandler;

   protected static final int MOVED = 0;
   protected static final int STRETCHED_START = 1;
   protected static final int STRETCHED_STOP = 2;
   protected static final int CROPPED_START = 3;
   protected static final int CROPPED_STOP = 4;
   protected static final int DISABLED = 5;
   protected static final int NO_CHANGE = 6;
   protected static final int DEFAULT = 7;
   protected static final int CHANGING = 8;
   protected static final int TYPE_INPUT = 20;
   protected static final int TYPE_OUTPUT = 21;

   // margin allowance for stretch/crop action
   private final int STRETCH_MARGIN = 3;

   // the variable holders for the small and large display
   // probes, needed in manipulation of data points on them
   private NumericProbeDisplayLarge largeDisplay = null;
   private NumericProbePanel smallDisplay = null; 
   
   private boolean isHighlighted;
   private Rectangle myProbeShadow;
   private boolean isShadowValid = true;

   private Scheduler getScheduler() {
      return myController.myScheduler;
   }

   private Main getMain() {
      return myController.myMain;
   }

   public ProbeInfo (TimelineController setController, 
      Track setTrack, Probe setProbe) {
      
      myController = setController;
      if (setTrack == null) {
         throw new InternalErrorException ("track is null");
      }
      myTrack = setTrack;
      myProbe = setProbe;
      getProbe().setTrack (setTrack.getTrackNumber());

      displayAreaInitialization();
      fieldsInitialization();
      displayInitialization();
   }
  
   /**
    * Returns true if the time interval specified by [t0, t1] overlaps
    * with the start and stop times of this probe, within the precision
    * of TimeBase.
    * @return true if this probe overlaps with the time interval
    */
   public boolean isOverlapping (double t0, double t1) {
      return (TimeBase.compare (t1, myProbe.getStartTime()) > 0 &&
              TimeBase.compare (t0, myProbe.getStopTime()) < 0);
   }

   /**
    * Returns true if the time interval of a specified probe overlaps
    * with the start and stop times of this probe, within the precision
    * of TimeBase.
    * @return true if this probe overlaps with a specified provbe
    */
   public boolean isOverlapping (Probe probe) {
      return isOverlapping (probe.getStartTime(), probe.getStopTime());
   }

   private void displayAreaInitialization() {
      myLabel = new JLabel();
      myDisplayArea = new ProbeDisplayArea();
   }
   
   // initialize the fields, including the label to append
   // the track number of the track this probe is associated with
   private void fieldsInitialization() {
      if (getName() == null) {
         setName ("");
      }
      
      myProbeShadow = new Rectangle();
      mouseHandler = new ProbeMouseHandler();
   }   

   private void displayInitialization() {
      myLabel.setAlignmentX (JComponent.CENTER_ALIGNMENT);
      myLabel.setAlignmentY (JComponent.CENTER_ALIGNMENT);
      myLabel.setBorder (BorderFactory.createMatteBorder (0, 1, 0, 1, Color.BLACK));
      // center the text
      myLabel.setHorizontalAlignment (SwingConstants.CENTER);
      myLabel.setOpaque (true);
      updateLabelText();

      myDisplayArea.setBackground (Color.WHITE);
      myDisplayArea.addMouseListener (mouseHandler);
      myDisplayArea.addMouseMotionListener (mouseHandler);

      setAppropriateColor();
      setAppropSizeAndLocation (true);
   }

   public int getIndex () {
      return myIndex;
   }
   
   public boolean getIsShadowValid() {
      return isShadowValid;
   }
   
   public Rectangle getProbeShadow() {
      return myProbeShadow;
   }
   
   public Track getTrack () {
      return myTrack;
   }

   public Probe getProbe () {
      return myProbe;
   }

   public ProbeDisplayArea getDisplayArea() {
      return myDisplayArea;
   }

   public double getStartTime() {
      return getProbe().getStartTime();
   }

   public double getStopTime() {
      return getProbe().getStopTime();
   }

   public String getName() {
      return getProbe().getName();
   }

   public boolean isActive() {
      return getProbe().isActive();
   }

   public Track getParentTrack() {
      return myTrack;
   }

   public void setStartTime (double t) {
      getProbe().setStartTime (t);
   }

   public Integer getTrackNumber() {
      return myTrack.getTrackNumber();
   }

   public void setStopTime (double t) {
      if (t == getProbe().getStartTime()){
         (new Throwable()).printStackTrace();
      }
      getProbe().setStopTime (t);
   }

   public void setName (String newName) {
      if (newName != getProbe().getName()) {
         getProbe().setName (newName);
         updateLabelText();
      }
   }

   public void setScale (double newScale) {
      getProbe().setScale (newScale);
   }

   public void setActive (boolean active) {
      getProbe().setActive (active);
   }

   public void setParentTrack (Track newParentTrack) {
      if (newParentTrack == null) {
         throw new InternalErrorException ("track is null");
      }
      myTrack = newParentTrack;
   }

   public Object clone() {
      ProbeInfo cloned = null;
      
      try {
         if (getProbe().isCloneable()) {
            cloned = (ProbeInfo) super.clone();
            cloned.myProbe = (Probe) getProbe().clone();

            cloned.largeDisplay = null;
            cloned.smallDisplay = null;

            cloned.displayAreaInitialization();
            cloned.displayInitialization();
         }
      }
      catch (CloneNotSupportedException e) {
         e.printStackTrace();
      }
      
      return cloned;
   }

   /**
    * duplicate the probe
    * 
    */
   private void duplicateProbe() {
      // don't need to supply a copyMap to copy because it won't be used
      Probe oldProbe= getProbe();
      if (!(oldProbe.getParent() instanceof MutableCompositeComponent)) {
         throw new InternalErrorException ("Probe's parent is not editable");
      }
      
      Probe newProbe = null;
      try {
         newProbe = (Probe)getProbe().clone();
      }
      catch (Exception e) {
         e.printStackTrace();
         throw new InternalErrorException (
            "Cannot clone probe of type " + getProbe().getClass());
      }

      // set start time so that probe follows right after this one ...
      double startTime = getProbe().getStartTime();
      double stopTime = getProbe().getStopTime();
      newProbe.setStartTime (stopTime);
      newProbe.setStopTime (stopTime + (stopTime - startTime));

      AddComponentsCommand cmd = new AddComponentsCommand (
         "duplicate probe", newProbe, (MutableCompositeComponent) oldProbe.getParent());
      getMain().getUndoManager().saveStateAndExecute (cmd);
      SelectionManager selman = getMain().getSelectionManager();
      selman.removeSelected (oldProbe);
      selman.addSelected (newProbe);
      //myController.deselectAllProbes(); XXX need to fix?
   }

   public void setProbeWithTime (double time) {
      if (getProbe().isInput()) {
         setNumericProbeDisplay();
      }
   }

   // // save a probe
   // public void saveProbe() {
   //    try {
   //       getProbe().save();
   //       myController.requestUpdateDisplay();
   //    }
   //    catch (IOException e) {
   //       System.out.println ("Couldn't save probe ");
   //       e.printStackTrace();
   //    }
   // }

   // // load a probe
   // public void loadProbe() {
   //    try {
   //       getProbe().load();
   //       updateProbeDisplays();
   //       myController.requestUpdateDisplay();
   //    }
   //    catch (IOException e) {
   //       System.out.println ("Couldn't load probe ");
   //       e.printStackTrace();
   //    }
   // }

   // load a probe from a MATLAB variable
   public void loadFromMatlab (Probe probe) {
      // connection should exist or this method shouldn't be called
      MatlabInterface mi = getMain().getMatlabConnection();
      NumericProbeBase np = (NumericProbeBase)probe;
      ProbeEditor.loadFromMatlab (np, mi, myController);
   }

   private void clearProbeData() {
      if (NumericProbeBase.class.isAssignableFrom (getProbe().getClass())) {
         NumericProbeBase numericProbe = (NumericProbeBase)getProbe();
         numericProbe.getNumericList().clear();

         // add empty data, using set if settable
         if (getProbe().isSettable()) {
            getProbe().setData (getProbe().getStartTime());
            getProbe().setData (getProbe().getStopTime());
         }
         else if (NumericInputProbe.class.isAssignableFrom (
                     getProbe().getClass())) {
            ((NumericInputProbe)getProbe()).loadEmpty();
         }

      }
      else
         System.out.println ("probe is not numeric, cannot clear");
   }

   private File chooseFile (
      File StartFrom, int SelectionMode, String ApproveButtonText) {
      JFileChooser myFileChooser = new JFileChooser();
      myFileChooser.setCurrentDirectory (StartFrom);
      myFileChooser.setFileSelectionMode (SelectionMode);

      if (ApproveButtonText != null)
         myFileChooser.setApproveButtonText (ApproveButtonText);

      int returnVal;
      if (ApproveButtonText == "Save As") {
         returnVal = myFileChooser.showSaveDialog (myController);
      }
      else {
         returnVal = myFileChooser.showOpenDialog (myController);
      }
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         return myFileChooser.getSelectedFile();
      }
      else {
         return null;
      }
   }

   // /**
   //  * author: Chad Currently only works with loading files from within the
   //  * working directory.
   //  */

   // private void loadfromProbe() {
   //    File current = null;
   //    String workspace = new String (ArtisynthPath.getWorkingDirPath());
   //    // workspace = workspace.substring(0, (workspace.length() - 2));

   //    current = getProbe().getAttachedFile();

   //    if (current == null)
   //       current = new File (workspace);

   //    File file = chooseFile (current, JFileChooser.FILES_ONLY, "Load from");
   //    if (file != null) {
   //       String probePath = Probe.getPathFromFile (file);
   //       System.out.println ("Workspace: " + workspace);
   //       System.out.println ("Selected file address: " + probePath);
   //       String oldFileName = getProbe().getAttachedFileName();

   //       try {
   //          getProbe().setAttachedFileName (probePath);
   //          getProbe().load();
   //          updateProbeDisplays();
   //          myController.requestUpdateDisplay();
   //       }
   //       catch (IOException e) {
   //          // Back out
   //          System.err.println ("Couldn't load probe ");

   //          getProbe().setAttachedFileName (oldFileName);
   //          e.printStackTrace();
   //       }
   //    }
   // }

   // private void saveasProbe() {
   //    File current = null;
   //    String workspace = new String (ArtisynthPath.getWorkingDirPath());

   //    current = getProbe().getAttachedFile();

   //    if (current == null || !current.exists())
   //       current = new File (workspace);

   //    File file = chooseFile (current, JFileChooser.FILES_ONLY, "Save As");
   //    if (file != null) {
   //       if (file.exists() && !GuiUtils.confirmOverwrite (myController, file)) {
   //          return;
   //       }
   //       String probePath = Probe.getPathFromFile (file);
   //       System.out.println ("Selected file path: " + probePath);
   //       String oldFileName = getProbe().getAttachedFileName();
   //       try {
   //          getProbe().setAttachedFileName (probePath);
   //          getProbe().save();
   //          myController.requestUpdateDisplay();
   //       }
   //       catch (IOException e) {
   //          System.out.println ("Couldn't save probe ");
   //          // Back out
   //          getProbe().setAttachedFileName (oldFileName);
   //          e.printStackTrace();
   //       }
   //    }
   // }

   // private void exportProbe() {
   //    File file = myProbe.getExportFile();
   //    if (file != null) {
   //       String ext = ArtisynthPath.getFileExtension(file).toLowerCase();
   //       Probe.ExportProps props = myProbe.getExportProps (ext);
   //       if (props == null) {
   //          // shouldn't happen
   //          System.out.println (
   //             "Warning: unsupported file extension for " + file);
   //          myProbe.setExportFileName (null);
   //       }
   //       else {
   //          try {
   //             myProbe.export (file, props);
   //          }
   //          catch (Exception e) {
   //             e.printStackTrace(); 
   //             myProbe.setExportFileName (null);
   //          }
   //       }
   //    }
   // }

   private String getMatlabName (Probe probe) {
      if (probe.getName() != null) {
         return probe.getName();
      }
      else if (probe.isInput()) {
         return "iprobe" + probe.getNumber();
      }
      else {
         return "oprobe" + probe.getNumber();
      }
   }

   private void saveToMatlab (Probe probe) {
      // connection should exist or this method shouldn't be called
      MatlabInterface mi = getMain().getMatlabConnection();
      NumericProbeBase np = (NumericProbeBase)probe;
      ProbeEditor.saveToMatlab (np, mi, myController);
   }

   private void printProbe() {
      getProbe().print (
         myController.getTimescale().getTimescaleCursorTime());
   }

   private void setProbe() {
      myProbe.setData (
         myController.getTimescale().getTimescaleCursorTime());
   }

   /**
    * edited by johnty to prevent null exceptions.
    * 
    */
   private void toggleProbeActivation() {
      WayPointProbe wayProbe = getMain().getRootModel().getWayPoints();
      double earliestTime = getStartTime();
      wayProbe.invalidateAfterTime (getStartTime());

      if (myController.getCurrentTime() > earliestTime) {
         if (getMain().getWorkspace().rootModelHasState()) {
            WayPoint way = wayProbe.getValidOnOrBefore (earliestTime);
            if (way != null) {
               getScheduler().setTime (way);
            }
            else {
               getScheduler().setTime (0);               
            }
         }
         else {
            getScheduler().setTime (earliestTime);
         }
      }

      setActive (!isActive());
      setAppropriateColor ();
      myController.requestUpdateWidgets();
   }

   public void expandProbe() {
      int width = myDisplayArea.getWidth();
      int height = GuiStorage.PROBE_DETAIL_HEIGHT;

      if (getProbe() instanceof Displayable) {
         Displayable dispProbe = (Displayable)getProbe();
         JPanel display = dispProbe.getDisplay (width, height, false);
         if (display instanceof NumericProbePanel &&
             getProbe() instanceof NumericProbeBase) {
            smallDisplay = (NumericProbePanel)display;
         }
         myDisplayArea.add (display);
      }
      myLabel.setBorder (
         BorderFactory.createMatteBorder (0, 1, 1, 1, Color.BLACK));
      myController.updateComponentSizes();
      updateProbeDisplays();
   }

   public void contractProbe() {
      myDisplayArea.removeAll();
      myDisplayArea.add (myLabel);
      if (smallDisplay != null) {
         ((Displayable)getProbe()).removeDisplay (smallDisplay);
         smallDisplay = null;
      }
      myLabel.setBorder (
         BorderFactory.createMatteBorder (0, 1, 0, 1, Color.BLACK));
      myController.updateComponentSizes();
   }

   public void muteProbe() {
      setActive (false);
      setAppropriateColor ();
   }

   public void unmuteProbe() {
      setActive (true);
      setAppropriateColor ();
   }

   public void setIndex (int idx) {
      myIndex = idx;
   }

   public void updateCoordinates() {
      myStartPixel =
         myController.timescale.getCorrespondingPixel (getStartTime());
      myStopPixel =
         myController.timescale.getCorrespondingPixel (getStopTime());
   }

   // change the name of the probe
   private void changeProbeName() {
      String name =
         JOptionPane.showInputDialog (
            myController, "Please input the new probe name", "Probe Name",
            JOptionPane.PLAIN_MESSAGE);

      if (name != null) {
         setName (name);
      }
   }
   
   // edit the probe
   private void editProbe() {
      NumericProbeEditor dialog;
      if (getProbe().isInput()) {
         dialog = new InputNumericProbeEditor ((NumericInputProbe) getProbe());
      }
      else {
         dialog = new OutputNumericProbeEditor ((NumericOutputProbe) getProbe());
      }
      
      dialog.setVisible (true);
      
      Point loc = getMain().getFrame().getLocation();
      dialog.setLocation (loc.x + getMain().getFrame().getWidth(),
                          getMain().getFrame().getHeight());
      
      myController.addProbeEditor (dialog);
   }

   // update the probe changes
   public void updateProbeDisplays() {
      if (getProbe() instanceof Displayable) {
         if (myTrack.isExpanded && myDisplayArea.getComponentCount() == 1) {
            expandProbe();
         }
         ((Displayable)getProbe()).updateDisplays();
      }
   }

   public void setNumericProbeDisplay() {
      if (myTrack.isExpanded()) {
         contractProbe();
         expandProbe();
      }
   }

   public void setAppropSizeAndLocation (boolean isDurationModified) {
      int trackYCoor;
      int newDisplayAreaHeight;
      int newDisplayAreaWidth;
      Dimension newDisplayAreaSize;
      Dimension newLabelSize;

      // convert the times into appropriate pixel values
      updateCoordinates();

      // if time duration of the probe has been modified,
      // update the width of display frame
      if (isDurationModified) {
         newDisplayAreaWidth = myStopPixel - myStartPixel;
      }

      // otherwise, the width remains the same as the original
      else {
         newDisplayAreaWidth = myDisplayArea.getWidth();
      }

      // depending on whether the probe display is in
      // expanded state, change the timeline height accordingly
      if (myTrack.isExpanded()) {
         newDisplayAreaHeight = GuiStorage.PROBE_EXPAND_HEIGHT;
      }
      else {
         newDisplayAreaHeight = GuiStorage.PROBE_NORMAL_HEIGHT;
      }

      // update the size of the probe
      newLabelSize =
         new Dimension (newDisplayAreaWidth, GuiStorage.PROBE_LABEL_HEIGHT);
      myLabel.setSize (newLabelSize);
      myLabel.setMinimumSize (newLabelSize);
      myLabel.setMaximumSize (newLabelSize);
      myLabel.setPreferredSize (newLabelSize);
      newDisplayAreaSize =
         new Dimension (newDisplayAreaWidth, newDisplayAreaHeight);
      myDisplayArea.setSize (newDisplayAreaSize);
      myDisplayArea.setMinimumSize (newDisplayAreaSize);
      myDisplayArea.setMaximumSize (newDisplayAreaSize);
      myDisplayArea.setPreferredSize (newDisplayAreaSize);

      // update the location of the probe
      trackYCoor = myTrack.computeTrackYCoor();
      myDisplayArea.setBounds (myStartPixel, trackYCoor + 1,
         myDisplayArea.getWidth(), myDisplayArea.getHeight());

      updateProbeDisplays();
   }

   public void setAppropriateColor () {
      Color c;
      double currentTime = myController.getCurrentTime();

      // if the probe is muted

      if (isProbeSelected()) {
         if (isHighlighted) {
            c = GuiStorage.COLOR_PROBE_SELECTED_HIGHLIGHT;
         }
         else {
            c = GuiStorage.COLOR_PROBE_SELECTED_NORMAL;
         }
      }
      else if (!getProbe().isActive()) {
         if (isHighlighted) {
            c = GuiStorage.COLOR_PROBE_MUTED_HIGHLIGHT;
         }
         else {
            c = GuiStorage.COLOR_PROBE_MUTED_NORMAL;
         }
      }
      // else if the probe finishes the integration
      else if (getProbe().getStopTime() < currentTime) {
         if (isHighlighted) {
            if (getProbe().isInput()) {
               c = GuiStorage.COLOR_INPUTPROBE_AFTER_HIGHLIGHT;
            }
            else {
               c = GuiStorage.COLOR_OUTPUTPROBE_AFTER_HIGHLIGHT;
            }
         }
         else {
            if (getProbe().isInput()) {
               c = GuiStorage.COLOR_INPUTPROBE_AFTER_NORMAL;
            }
            else {
               c = GuiStorage.COLOR_OUTPUTPROBE_AFTER_NORMAL;
            }
         }
      }
      // if entire probe is not yet integrated or is being integrated
      else {
         if (isHighlighted) {
            if (getProbe().isInput()) {
               c = GuiStorage.COLOR_INPUTPROBE_BEFORE_HIGHLIGHT;
            }
            else {
               c = GuiStorage.COLOR_OUTPUTPROBE_BEFORE_HIGHLIGHT;
            }
         }
         else {
            if (getProbe().isInput()) {
               c = GuiStorage.COLOR_INPUTPROBE_BEFORE_NORMAL;
            }
            else {
               c = GuiStorage.COLOR_OUTPUTPROBE_BEFORE_NORMAL;
            }
         }
      }
      
      if (myLabel.getBackground() != c) {
         myLabel.setBackground (c);
      }
   }

   // code style changes by Andrei
   private void setAppropriateCursor (int mode) {
      if (mode == ProbeInfo.NO_CHANGE) {
         myDisplayArea.setCursor (myController.getCurrentCursor());
      }
      else if (mode == ProbeInfo.DEFAULT) {
         myDisplayArea.setCursor (Cursor.getDefaultCursor());
         myController.setCurrentCursor (Cursor.getDefaultCursor());
      }
      else if (mode == ProbeInfo.DISABLED) {
         myDisplayArea.setCursor (
            Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
         myController.setCurrentCursor (
            Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
      }
      else if (mode == ProbeInfo.MOVED) {
         myDisplayArea.setCursor (
            Cursor.getPredefinedCursor (Cursor.HAND_CURSOR));
         myController.setCurrentCursor (
            Cursor.getPredefinedCursor (Cursor.HAND_CURSOR));
      }
      else if (mode == ProbeInfo.STRETCHED_START) {
         myDisplayArea.setCursor (
            Cursor.getPredefinedCursor (Cursor.W_RESIZE_CURSOR));
         myController.setCurrentCursor (
            Cursor.getPredefinedCursor (Cursor.W_RESIZE_CURSOR));
      }
      else if (mode == ProbeInfo.STRETCHED_STOP) {
         myDisplayArea.setCursor (
            Cursor.getPredefinedCursor (Cursor.E_RESIZE_CURSOR));
         myController.setCurrentCursor (
            Cursor.getPredefinedCursor (Cursor.E_RESIZE_CURSOR));
      }
      else if (mode == ProbeInfo.CROPPED_START) {
         myDisplayArea.setCursor (
            Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
         myController.setCurrentCursor (
            Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
      }
      else if (mode == ProbeInfo.CROPPED_STOP) {
         myDisplayArea.setCursor (
            Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
         myController.setCurrentCursor (
            Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
      }
   }

   public void updateLabelText() {
      String text = getProbe().getName();
      if (text == null) {
         text = Integer.toString (getProbe().getNumber());
      }
      
      if (!text.equals (myLabel.getText())) {
         myLabel.setText (text);
      }
   }

   // ==============================================
   // Functions for moving temporarily grouped sets of probes around in the
   // timeline, not finished yet.

   public void dispose() {
      if (myProbe != null) {
         myProbe.dispose();
         myProbe = null;
      }
      
      myDisplayArea.removeMouseListener (mouseHandler);
      myDisplayArea.removeMouseMotionListener (mouseHandler);
      myDisplayArea = null;
      myTrack = null;

      if (largeDisplay != null) {
         largeDisplay.dispose();
         largeDisplay = null;
      }
   }

   private void addMenuItem (JPopupMenu menu, String cmd) {
      JMenuItem item = new JMenuItem (cmd);
      item.addActionListener (this);
      item.setActionCommand (cmd);
      menu.add (item);      
   }

   private void showPopupMenu (Component component, int x, int y) {
      final JPopupMenu popupMenu = new JPopupMenu ();
      JMenuItem myChangeNameItem = new JMenuItem ("Rename");
      myChangeNameItem.addActionListener (this);
      myChangeNameItem.setActionCommand ("Rename");

      JMenuItem myDeleteItem = new JMenuItem ("Delete");
      myDeleteItem.addActionListener (this);
      myDeleteItem.setActionCommand ("Delete");

      JMenuItem myViewPropertyItem = new JMenuItem ("Properties");
      myViewPropertyItem.addActionListener (this);
      myViewPropertyItem.setActionCommand ("Properties");

      JMenuItem myLargeProbeDisplayItem = new JMenuItem ("Large Display");
      myLargeProbeDisplayItem.addActionListener (this);
      myLargeProbeDisplayItem.setActionCommand ("Large Display");

      JMenuItem myEditProbeItem = null;
      if (getProbe() instanceof NumericOutputProbe ||
          getProbe() instanceof NumericInputProbe) {
         myEditProbeItem = new JMenuItem ("Edit");
         myEditProbeItem.addActionListener (this);
         myEditProbeItem.setActionCommand ("Edit");
      }

      JMenuItem myDuplicateItem = null;
      if (getProbe().isCloneable()) {
         myDuplicateItem = new JMenuItem ("Duplicate");
         myDuplicateItem.addActionListener (this);
         myDuplicateItem.setActionCommand ("Duplicate");
      }

      JMenuItem mySaveItem = new JMenuItem ("Save data");
      mySaveItem.addActionListener (this);
      mySaveItem.setActionCommand ("Save data");

      JMenuItem mySaveasItem = new JMenuItem ("Save data as ...");
      mySaveasItem.addActionListener (this);
      mySaveasItem.setActionCommand ("Save data as ...");

      JMenuItem mySetItem = new JMenuItem ("Set");
      mySetItem.addActionListener (this);
      mySetItem.setActionCommand ("Set");

      JMenuItem myPrintItem = new JMenuItem ("Print");
      myPrintItem.addActionListener (this);
      myPrintItem.setActionCommand ("Print");

      JMenuItem myLoadfromItem = new JMenuItem ("Load data from ...");
      myLoadfromItem.addActionListener (this);
      myLoadfromItem.setActionCommand ("Load data from ...");

      JMenuItem myLoadItem = new JMenuItem ("Load data");
      myLoadItem.addActionListener (this);
      myLoadItem.setActionCommand ("Load data");

      JMenuItem myExportItem = new JMenuItem ("Export data");
      myExportItem.addActionListener (this);
      myExportItem.setActionCommand ("Export data");

      JMenuItem myExportAsItem = new JMenuItem ("Export data as ...");
      myExportAsItem.addActionListener (this);
      myExportAsItem.setActionCommand ("Export data as");

      // JMenuItem myImportItem = new JMenuItem ("Import");
      // myImportItem.addActionListener (this);
      // myImportItem.setActionCommand ("Import");

      // JMenuItem myImportAsItem = new JMenuItem ("Import as ...");
      // myImportAsItem.addActionListener (this);
      // myImportAsItem.setActionCommand ("Import as");

      JMenuItem myClearItem = new JMenuItem ("Clear");
      myClearItem.addActionListener (this);
      myClearItem.setActionCommand ("Clear");

      JMenuItem activityItem;
      if (getProbe().isActive()) {
         activityItem = new JMenuItem ("Deactivate");
      }
      else {
         activityItem = new JMenuItem ("Activate");
      }
      activityItem.addActionListener (this);
      activityItem.setActionCommand ("ToggleActivation");

      popupMenu.add (myLargeProbeDisplayItem);
      popupMenu.addSeparator();

      popupMenu.add (activityItem);
      if (myDuplicateItem != null) {
         popupMenu.add (myDuplicateItem);
      }
      if (NumericProbeBase.class.isAssignableFrom (getProbe().getClass())) {
         popupMenu.add (myClearItem);
      }
      popupMenu.add (myDeleteItem);

      popupMenu.addSeparator();

      popupMenu.add (mySaveItem);
      popupMenu.add (mySaveasItem);
      if (getProbe() instanceof NumericProbeBase &&
          getMain().hasMatlabConnection()) {
         addMenuItem (popupMenu, "Save to MATLAB");
      }
      popupMenu.add (myLoadItem);
      popupMenu.add (myLoadfromItem);
      if (getProbe() instanceof NumericProbeBase &&
          getMain().hasMatlabConnection()) {
         addMenuItem (popupMenu, "Load from MATLAB");
      }

      popupMenu.addSeparator();

      if (getProbe().getExportFileInfo().length > 0) {
         popupMenu.add (myExportItem);
         popupMenu.add (myExportAsItem);
         popupMenu.addSeparator();
      }

      popupMenu.add (myChangeNameItem);
      if (myEditProbeItem != null) {
         popupMenu.add (myEditProbeItem);
      }
      popupMenu.add (mySetItem);
      popupMenu.add (myPrintItem);
      
      mySaveItem.setEnabled (getProbe().getAttachedFile() != null);
      myExportItem.setEnabled (getProbe().getExportFile() != null);
      myLoadItem.setEnabled (getProbe().getAttachedFile() != null);
      mySetItem.setEnabled (getProbe().isSettable());
      myPrintItem.setEnabled (getProbe().isPrintable());
      
      popupMenu.show (component, x, y);
   }

   public void finalize() {
      dispose();
   }

   public boolean isProbeSelected (){
      return myProbe.isSelected();
   }

   /**
    * Updates timeline information about the probe to reflect any changes in the
    * probe's properties.
    */
   public void updateProbeData() {
      updateLabelText();
      setAppropriateColor();
      
//       if (getProbe().isSelected ()) {
//          setSelected (true);
//       }
//       else {
//          setSelected (false);
//       }
      
      if (getProbe() instanceof NumericProbeBase) {
         ((NumericProbeBase)getProbe()).updateDisplaysForPropertyChanges();
      }
   }
   
   // public void closeLargeDisplay() {
   //    largeDisplay = null;
   // }
   
//    private void setSelected (boolean value) {
//       if (selected != value) {
//          selected = value;
//          updateLabelText();
         
//          LinkedList<ModelComponent> list = new LinkedList<ModelComponent>();
//          list.add (getProbe());
   
//          if (selected) {
//             myController.getSelectionListener ().selectionChanged (
//                new SelectionEvent (list, null));
            
//             if (!myController.selectedProbes.contains (this)) {
//                myController.selectedProbes.add (this);
//             }
//          }
//          else {
//             myController.getSelectionListener ().selectionChanged (
//                new SelectionEvent (null, list));
//             myController.selectedProbes.remove (this);
//          }
         
//          setAppropriateColor();
//       }
//    }   

   // save the original time variables
   private void saveOldTimes() {
      oldStartTime = getStartTime();
      oldStopTime = getStopTime();
   }
   
   public void restoreOldTimes() {
      setStartTime (oldStartTime);
      setStopTime (oldStopTime);
   }

   private void setActiveCursor() {
      if (moveP) {
         setAppropriateCursor (ProbeInfo.MOVED);
      }
      else if (stretchFromStartP) {
         setAppropriateCursor (ProbeInfo.STRETCHED_START);
      }
      else if (stretchFromStopP) {
         setAppropriateCursor (ProbeInfo.STRETCHED_STOP);
      }
      else if (cropFromStartP) {
         setAppropriateCursor (ProbeInfo.CROPPED_START);
      }
      else if (cropFromStopP) {
         setAppropriateCursor (ProbeInfo.CROPPED_STOP);
      }
      else {
         setAppropriateCursor (ProbeInfo.NO_CHANGE);
      }
   }

   private void updateScaleOnStretch() {
      if (getProbe().isScalable()) {
         double newDuration = getStopTime() - getStartTime();
         double newScale =
            (newDuration) / (oldStopTime - oldStartTime);
         setScale (newScale * getProbe().getScale());
      }
   }

   private void invalidateWayPoints() {
      // determine which waypoints have to be invalidated
      WayPointProbe wayProbe = getMain().getRootModel().getWayPoints();

      double newStartTime = getStartTime();
      wayProbe.invalidateAfterTime (
         (oldStartTime < newStartTime) ? oldStartTime : newStartTime);
   }
   
   private void updateTimes() {
      double newStartTime, newStopTime;

      if (moveP) {
         newStartTime =
            myController.timescale.getCorrespondingTime (myStartPixel);
         newStopTime = newStartTime + (getStopTime() - getStartTime());
         setStartTime (newStartTime);
         setStopTime (newStopTime);
      }
      else if (stretchFromStartP || cropFromStartP) {
         newStartTime =
            myController.timescale.getCorrespondingTime (myStartPixel);
         setStartTime (newStartTime);
      }
      else if (stretchFromStopP || cropFromStopP) {
         newStopTime =
            myController.timescale.getCorrespondingTime (myStopPixel);
         setStopTime (newStopTime);
      }
   }
   
   public void setAppropriateShadow (int yCoor, int pixelWidth) {      
      Track activeTrack = myController.getTrackAtYCoor (yCoor, myTrack.isInputTrack ());
      myProbeShadow = new Rectangle();
      myProbeShadow.x = myStartPixel;
      myProbeShadow.width = pixelWidth - 1;
      
      myProbeShadow.height = (activeTrack.isExpanded() ? 
         GuiStorage.PROBE_EXPAND_HEIGHT : GuiStorage.PROBE_NORMAL_HEIGHT) - 1;

      myProbeShadow.y = myController.getYCoorUpToTrack (activeTrack) + 1;
      isShadowValid = activeTrack.isDragValid (this, moveP);
   }

   private class WindowHandler extends WindowAdapter {
      public void windowClosed (WindowEvent e) {
         if (e.getSource() == largeDisplay) {
            System.out.println ("clearing large display");
            largeDisplay = null;
         }
      }
   }

   //=============================================================
   // ActionListener method implementation
   //=============================================================
   
   public void actionPerformed (ActionEvent e) {
      String nameOfAction = e.getActionCommand();

      if (nameOfAction == "Duplicate") {
         duplicateProbe();
      }
      else if (nameOfAction == "Save data") {
         ProbeEditor.saveData (myProbe, myController);
      }
      else if (nameOfAction == "Save data as ...") {
         ProbeEditor.saveDataAs (myProbe, myController);
      }
      else if (nameOfAction == "Save to MATLAB") {
         saveToMatlab(myProbe);
      }
      else if (nameOfAction == "Export data") {
         ProbeEditor.export (myProbe, myController);
      }
      else if (nameOfAction == "Export data as") {
         ProbeEditor.exportAs (myProbe, myController);
      }
      else if (nameOfAction == "Set") {
         setProbe();
      }
      else if (nameOfAction == "Print") {
         printProbe();
      }
      else if (nameOfAction == "Load data from ...") {
         ProbeEditor.loadDataFrom (myProbe, myController);
      }
      else if (nameOfAction == "Load data") {
         ProbeEditor.loadData (myProbe, myController);
      }
      else if (nameOfAction == "Load from MATLAB") {
         loadFromMatlab(myProbe);
      }
      else if (nameOfAction == "Clear") {
         clearProbeData();
      }
      else if (nameOfAction == "ToggleActivation") {
         toggleProbeActivation();
      }
      else if (nameOfAction == "Delete") {
         Track track = myTrack; // myTrack will be nulled by delete operation
         track.deleteProbe (this, /*confirm=*/true);
         track.refreshTrackChanges();
         return;
      }
      else if (nameOfAction == "Rename") {
         changeProbeName();
      }
      else if (nameOfAction == "Properties") {
      }
      else if (nameOfAction == "Edit") {
         editProbe();
      }
      else if (nameOfAction == "Large Display") {
         if (setLargeDisplayVisible (true) == null) {
            System.out.println ("Error: probe is not numeric");
         }
      }

      // refresh the track changes after the popup command has been executed
      myTrack.refreshTrackChanges();
   }

   public NumericProbeDisplayLarge setLargeDisplayVisible (boolean visible) {
      
      Probe probe = getProbe();
      if (!(probe instanceof NumericProbeBase)) {
         return null;
      }
      // display large probe display
      if (visible) {
         if (largeDisplay == null) {
            largeDisplay = new NumericProbeDisplayLarge (
               probe, Integer.toString (getTrackNumber ()));;
            largeDisplay.addWindowListener (new WindowHandler());
            
            // position the large probe window to the left of the timeline window
            Point timelineLocation = myController.getLocation();
            Dimension timelineSize = myController.getSize();
            
            Point newLargeProbePosition = new Point (
               timelineLocation.x + timelineSize.width, timelineLocation.y);
            largeDisplay.setLocation (newLargeProbePosition);
         }
         largeDisplay.setVisible (true);
      }
      else {
         if (largeDisplay == null) {
            largeDisplay.setVisible (false);
         }
      }
      return largeDisplay;
   }
   
   //=============================================================
   // Probe panel mouse listener
   //=============================================================

   private class ProbeMouseHandler implements MouseInputListener {
      private Point prevPoint;
      
      public void mousePressed (MouseEvent e) {      
         if (e.getButton () == MouseEvent.BUTTON1) {
            // select the probe that was clicked on
            boolean multSelection = myController.isMultipleSelectionEnabled(e);
            SelectionManager selectionManager = getMain().getSelectionManager();

            boolean selected = isProbeSelected();
            if (!selected) {
               if (!multSelection) {
                  selectionManager.clearSelections();
               }
               selectionManager.addSelected (myProbe);
               //setSelected (true);
            }
            else if (selected && multSelection) {
               selectionManager.removeSelected (myProbe);
               //setSelected (false);
            }
         }
         
         if (!getScheduler().isPlaying()) {
            if (e.isPopupTrigger() && myTrack.isEnabled()) {
               showPopupMenu (e.getComponent(), e.getX(), e.getY());
            }
   
            // perform drag-and-drop only on press of primary mouse button
            if (e.getButton() == MouseEvent.BUTTON1) {
               prevPoint = e.getPoint();
   
               // since "this" probe is active, reset the
               // "activeProbeExist" flag of the myController to true
               myController.setActiveProbeExist (true);
               determineAppropAction (e.isShiftDown());

               for (ProbeInfo pInfo : myController.selectedProbes) {
                  pInfo.saveOldTimes();
                  pInfo.setAppropriateShadow (
                     myController.getYCoorUpToTrack(pInfo.myTrack)+prevPoint.y, 
                     pInfo.myStopPixel-pInfo.myStartPixel);
               }
            }
         }
      }
   
      public void mouseDragged (MouseEvent e) {
         if (!getScheduler ().isPlaying () && myTrack.isEnabled()) {
            if (isProbeSelected()) { 
               draggingP = true;
               
               setActiveCursor();            
               setTimesOnDrag (e.getPoint(), prevPoint);
            }
         }
      }
      
      public void mouseReleased (MouseEvent e) {
         if (!getScheduler().isPlaying()) {
            if (e.isPopupTrigger() && myTrack.isEnabled()) {
               showPopupMenu (e.getComponent(), e.getX(), e.getY());
            }
   
            // perform drag-and-drop, or stretching, only on release of
            // primary mouse button
            if (e.getButton() == MouseEvent.BUTTON1 && 
                myTrack.isEnabled() && draggingP == true) {
               
               draggingP = false;
               Point newPoint = e.getPoint();
   
               int yBounds[] = { 0, 0 };
               if (myTrack.isExpanded()) {
                  yBounds[1] = GuiStorage.TRACK_EXPAND_HEIGHT - 1;
               }
               else {
                  yBounds[1] = GuiStorage.TRACK_NORMAL_HEIGHT - 1;
               }
         
               if (newPoint.x != prevPoint.x || newPoint.y < yBounds[0] || 
                   newPoint.y > yBounds[1]) {
                  setTimesOnRelease (newPoint, prevPoint);
               }
            }
   
            // disable the flags upon the release of mouse
            moveP = false;
            stretchFromStartP = false;
            stretchFromStopP = false;
            cropFromStartP = false;
            cropFromStopP = false;
   
            myController.setActiveProbeExist (false);
   
            // reset cursor back to default
            setAppropriateCursor (ProbeInfo.DEFAULT);
   
            myTrack.refreshTrackChanges();
         }
      }
      

   
      public void mouseEntered (MouseEvent e) {
         // if there is no other active components, then activate the
         // highlighting of the probe when the mouse enters the probe region
         if (!myController.isActiveProbeExist() && 
             !myController.isActiveWayPointExist() && 
             myTrack.isEnabled()) {
   
            isHighlighted = true;
            setAppropriateColor ();
            setAppropriateCursor (ProbeInfo.DEFAULT);
            
            myController.requestUpdateDisplay ();
         }
         else {
            setAppropriateCursor (ProbeInfo.NO_CHANGE);
         }
      }
   
      public void mouseExited (MouseEvent e) {
         // when this probe is inactive, disable highlighting
         if (!(moveP || stretchFromStartP || stretchFromStopP || 
               cropFromStartP || cropFromStopP)) {
   
            isHighlighted = false;
            setAppropriateColor ();
            
            myController.requestUpdateDisplay ();
         }
      }
   
      /**
       * author: andrei here we handle the double clicking on the probe display
       * if we double click on the probe display it expands / second time it
       * contracts
       */
      public void mouseClicked (MouseEvent e) {
         if (e.getClickCount() == 2 && myTrack.isEnabled()) {
            myTrack.setExpanded (!myTrack.isExpanded());
         }
      }
   
      public void mouseMoved (MouseEvent e) {
         // if no active component exists, then enable cursor update
         if (!myController.isActiveProbeExist() && 
             !myController.isActiveWayPointExist()) {
            // set appropriate cursor
            if (e.getX() <= STRETCH_MARGIN) {
               setAppropriateCursor (e.isShiftDown() ? 
                  ProbeInfo.CROPPED_START : ProbeInfo.STRETCHED_START);
            }
            else if (e.getX() >= myDisplayArea.getWidth() - STRETCH_MARGIN) {
               setAppropriateCursor (e.isShiftDown() ? 
                  ProbeInfo.CROPPED_STOP : ProbeInfo.STRETCHED_STOP);
            }
            else {
               setAppropriateCursor (ProbeInfo.DEFAULT);
            }
         }
      }
      
      private void setTimesOnDrag (Point tempPoint, Point prevPoint) {         
         int trackOffset = 0;
         
         if (moveP || stretchFromStartP || cropFromStartP) {
            myStartPixel += tempPoint.x - prevPoint.x;
         }
         else if (stretchFromStopP || cropFromStopP) {
            myStopPixel += tempPoint.x - prevPoint.x;
         }
         
         if (moveP) {
            Track newTrack = myController.getTrackAtYCoor (
               myController.getYCoorUpToTrack (myTrack) + tempPoint.y, 
               myProbe.isInput ());
            
            trackOffset = newTrack.getTrackNumber () - getTrackNumber ();
         }
   
         updateTimes();
         updateCoordinates();
      
         for (ProbeInfo pInfo : myController.selectedProbes) {
            if (pInfo != ProbeInfo.this) {
               if (moveP || stretchFromStartP || cropFromStartP) {
                  pInfo.myStartPixel += tempPoint.x - prevPoint.x;
               }
               else if (stretchFromStopP || cropFromStopP) {
                  pInfo.myStopPixel += tempPoint.x - prevPoint.x;
               }
               
               if (moveP) {
                  Track newTrack = myController.getTrack (
                     pInfo.getTrackNumber () + trackOffset, pInfo.myProbe.isInput ());
                  
                  int offset = newTrack.getTrackNumber () - pInfo.getTrackNumber ();
                  
                  trackOffset = (trackOffset < 0) ? 
                     Math.max (trackOffset, offset) : Math.min (trackOffset, offset);
               }
         
               pInfo.updateTimes();
               pInfo.updateCoordinates();
            }
         }
         
         for (ProbeInfo pInfo : myController.selectedProbes) {
            int yCoor;
            if (moveP) {
               yCoor = myController.getYCoorUpToTrack (
                  myController.getTrack (pInfo.getTrackNumber () + 
                     trackOffset, pInfo.myProbe.isInput ())) + 1;
            }
            else {
               yCoor = myController.getYCoorUpToTrack (pInfo.getTrack ()) + 1;
            }
             
            pInfo.setAppropriateShadow (yCoor, pInfo.myStopPixel - pInfo.myStartPixel);
         }
         
         for (ProbeInfo pInfo : myController.selectedProbes) {         
            pInfo.restoreOldTimes();
            pInfo.updateCoordinates();
         }      
      
         myController.requestUpdateDisplay();
      }
      
      private void setTimesOnRelease (Point newPoint, Point prevPoint) {
         int trackOffset = 0;
         
         if (moveP || stretchFromStartP || cropFromStartP) {
            myStartPixel += newPoint.x - prevPoint.x;
            
            if (!moveP && myStartPixel >= myStopPixel) {
               myStartPixel = myStopPixel - 1;
            }
         }
         else if (stretchFromStopP || cropFromStopP) {
            myStopPixel += newPoint.x - prevPoint.x;
            
            if (myStopPixel <= myStartPixel) {
               myStopPixel = myStartPixel + 1;
            }
         }
         
         if (moveP) {
            Track newTrack = myController.getTrackAtYCoor (
               myController.getYCoorUpToTrack (myTrack) + newPoint.y, 
               myProbe.isInput ());
            
            trackOffset = newTrack.getTrackNumber () - getTrackNumber ();
            
            //Don't think we need this at all ...
            //myTrack.detachProbeInfo (ProbeInfo.this);
         }
   
         updateTimes();
         updateCoordinates();
         
         for (ProbeInfo pInfo : myController.selectedProbes) {
            if (pInfo != ProbeInfo.this) {
               if (moveP || stretchFromStartP || cropFromStartP) {
                  pInfo.myStartPixel += newPoint.x - prevPoint.x;
   
                  // when the beg. of the probe is dragged beyond the probe's
                  // stopTime, shrink the probe to the width of 1 pixel
                  if (!moveP && pInfo.myStartPixel >= pInfo.myStopPixel) {
                     pInfo.myStartPixel = pInfo.myStopPixel - 1;
                  }
               }
               else {
                  pInfo.myStopPixel += newPoint.x - prevPoint.x;
   
                  // when the end of the probe is dragged beyond the probe's
                  // startTime, shrink the probe to the width of 1 pixel
                  if (pInfo.myStopPixel <= pInfo.myStartPixel) {
                     pInfo.myStopPixel = pInfo.myStartPixel + 1;
                  }
               }
   
               pInfo.updateTimes();
   
               if (moveP) {
                  Track newTrack = myController.getTrack (
                     pInfo.getTrackNumber () + trackOffset, pInfo.myProbe.isInput ());
                  
                  int offset = newTrack.getTrackNumber () - pInfo.getTrackNumber ();
   
                  trackOffset = (trackOffset < 0) ? 
                     Math.max (trackOffset, offset) : Math.min (trackOffset, offset);
   
                  //Don't think we need this at all ...
                  //pInfo.myTrack.detachProbeInfo (pInfo);
               }
            }
         }

         if (moveP) {
            boolean validDrag = true;

            for (ProbeInfo pInfo : myController.selectedProbes) {
               // compute yCoor relative to ProbeTrack's coordinates
               int yCoor = myController.getYCoorUpToTrack (
                  myController.getTrack (pInfo.getTrackNumber () + 
                     trackOffset, pInfo.myProbe.isInput ())) + 1;

               Track activeTrack = myController.getTrackAtYCoor (
                  yCoor, pInfo.myTrack.isInputTrack ());
               validDrag = activeTrack.isDragValid (pInfo, moveP);
               //Don't think we need this at all ...
               //pInfo.myTrack.detachProbeInfo (pInfo);

               if (!validDrag) {
                  break;
               }
            }

            for (ProbeInfo pInfo : myController.selectedProbes) {
               if (!validDrag) {
                  pInfo.restoreOldTimes ();
                  //Don't think we need this at all ...
                  //pInfo.myTrack.attachProbeInfo (pInfo);
               }
               else {
                  int yCoor = myController.getYCoorUpToTrack (
                     myController.getTrack (pInfo.getTrackNumber () + 
                        trackOffset, pInfo.myProbe.isInput ())) + 1;

                  myController.executeDrop (yCoor, pInfo, pInfo.myTrack);
               }
            }
         }
         else {
            for (ProbeInfo pInfo : myController.selectedProbes) {
               if (stretchFromStartP || cropFromStartP) {
                  // if this is not the first probe, then check if overlap occurs
                  if (pInfo.myIndex != 0) {
                     ArrayList<ProbeInfo> pinfos = myTrack.getProbeInfos();
                     ProbeInfo prevProbe = pinfos.get (pInfo.myIndex - 1);

                     if (pInfo.getStartTime() < prevProbe.getStopTime()) {
                        pInfo.setStartTime(prevProbe.getStopTime());
                     }
                  }
               }
               else if (stretchFromStopP || cropFromStopP) {                  
                  // if this is not the last probe, then check if overlap occurs
                  
                  if (pInfo.myIndex != myTrack.numProbeInfos()-1) {
                     ArrayList<ProbeInfo> pinfos = myTrack.getProbeInfos();
                     ProbeInfo nextProbe = pinfos.get (pInfo.myIndex + 1);

                     if (pInfo.getStopTime() > nextProbe.getStartTime()) {
                        pInfo.setStopTime (nextProbe.getStartTime());
                     }
                  }
               }

               if (stretchFromStartP || stretchFromStopP) {
                  pInfo.updateScaleOnStretch();
               }
            }
         }

         for (ProbeInfo pInfo : myController.selectedProbes) {
            pInfo.updateCoordinates();
            pInfo.invalidateWayPoints();

            myController.setRestartTime (
               TimelineConstants.RESTART_BY_PROBE_DRAG);

            // refresh the display of the probes
            pInfo.setAppropSizeAndLocation (true);
            pInfo.setNumericProbeDisplay();

            // reset the display times of the probe if they exist
            if (pInfo.smallDisplay != null) {
               pInfo.smallDisplay.setDefaultXRange();
            }

            if (pInfo.largeDisplay != null) {
               pInfo.largeDisplay.getPanel().setDefaultXRange();
            }
         }
         myController.requestUpdateWidgets();
      }
      
      private void determineAppropAction (boolean isShiftDown) {
         // depending on the coordinate of the mouse press,
         // the appropriate action is activated
         if (prevPoint.x <= STRETCH_MARGIN) {
            if (isShiftDown) {
               cropFromStartP = true;
            }
            else {
               stretchFromStartP = true;
            }
         }
         else if (prevPoint.x >= myDisplayArea.getWidth() - STRETCH_MARGIN) {
            if (isShiftDown) {
               cropFromStopP = true;
            }
            else {
               stretchFromStopP = true;
            }
         }
         else {
            moveP = true;
            setAppropriateCursor (ProbeInfo.MOVED);
         }
      }
   }

   // ==============================================
   // Main probe display area
   // ==============================================
   
   private class ProbeDisplayArea extends JPanel {
      private static final long serialVersionUID = 1L;

      public ProbeDisplayArea() {
         setLayout (new BoxLayout (this, BoxLayout.Y_AXIS));
         add (myLabel);
      }

      // repaint required to draw timeline cursor
      public void repaint() {
         myController.repaint();
      }
   }
}
