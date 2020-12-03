package artisynth.core.gui.timeline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import artisynth.core.driver.GenericKeyHandler;
import artisynth.core.driver.Main;
import artisynth.core.gui.Timeline;
import artisynth.core.gui.editorManager.RemoveComponentsCommand;
import artisynth.core.probes.Probe;
import artisynth.core.probes.WayPoint;
import artisynth.core.probes.WayPointProbe;
import artisynth.core.util.TimeBase;

import maspack.widgets.GuiUtils;

public class Track extends JPanel {
   private static final long serialVersionUID = 1L;
   private final Color highlight = new Color (215, 215, 215);
   private final MatteBorder borderBottom =
      new MatteBorder (0, 0, 1, 0, Color.WHITE);
   private final MatteBorder borderBottomSelect =
      new MatteBorder (0, 0, 1, 0, Color.BLACK);
   private final  Border draggingBorder =
      BorderFactory.createCompoundBorder (borderBottom, borderBottomSelect);
   private final Border normalBorder =
      BorderFactory.createCompoundBorder (borderBottom, borderBottom);
   private final Border bevelBorder =
      BorderFactory.createBevelBorder (BevelBorder.LOWERED);

   private Color backgroundColor;
   
   private boolean isInputTrack;
   private int myIndex;
   protected boolean isExpanded;

   private TimelineController myController;
   private ArrayList<ProbeInfo> probeInfos;
   private boolean probeInfoSorted = true;
   private TrackListener myListener;

   private JToggleButton[] toggleButtons;
   private JLabel nameLabel;
//   protected TrackControl myTrackControl;

   protected final static int TRACK_EXPANDED = 0;
   protected final static int TRACK_CONTRACTED = 1;
   protected final static int TRACK_MUTED = 2;
   protected final static int TRACK_UNMUTED = 3;

   protected static final int DISABLED = 0;
   protected static final int NO_CHANGE = 1;
   protected static final int DEFAULT = 2;

   protected static final int TYPE_INPUT = 10;
   protected static final int TYPE_OUTPUT = 11;

   // Indicate if a track is being moved around in the timeline
   private static boolean dragging = false;
   // Indicate if a track is selected
   private boolean selected = false;
   // Indicate if the track is hidden from view
   private boolean visible = true;

   private Track myTrack = null;
   // if this track is in a group groupParent is the first track of the group
   private Track groupParent = null;

   // True if the track is editable, can be expanded and muted
   private boolean isEnabled = true;

   public Track (int type, String nameTrack, TimelineController controller) {
      super();
      
      isInputTrack = (type == Track.TYPE_INPUT);
      myController = controller;
      myTrack = this;
      overallInitialization (nameTrack);
   }

   /**
    * initialize the fields such as nameLabel and toggle buttons
    * 
    * @param nameTrack
    */

   private void fieldsInitialization (String nameTrack) {
      probeInfos = new ArrayList<ProbeInfo>();
      myListener = new TrackListener();
      myIndex = -1;
      
      nameLabel = new JLabel (nameTrack, SwingConstants.LEFT);
      toggleButtons = new JToggleButton[2];
      isExpanded = false;
   }
   
   void markProbesUnsorted() {
      probeInfoSorted = false;
   }

   /**
    * initialize the toggle buttons they are expanded or contracted ...
    */

   private void toggleButtonsInitialization() {
      JToggleButton expand = new JToggleButton (GuiStorage.EXPAND_TRACK_ICON);
      expand.setToolTipText ("Expand Track");
      JToggleButton mute = new JToggleButton (GuiStorage.MUTE_TRACK_ICON);
      mute.setToolTipText ("Mute Track");

      toggleButtons[0] = expand;
      toggleButtons[1] = mute;

      for (JToggleButton button : toggleButtons) {
         button.setSize (GuiStorage.TOGGLE_BUTTON_SIZE);
         button.setMinimumSize (GuiStorage.TOGGLE_BUTTON_SIZE);
         button.setMaximumSize (GuiStorage.TOGGLE_BUTTON_SIZE);
         button.setPreferredSize (GuiStorage.TOGGLE_BUTTON_SIZE);

         button.setActionCommand (button.getToolTipText());

         GenericKeyHandler keyHandler =
            new GenericKeyHandler(myController.myMain);

         button.addKeyListener (keyHandler);
         button.addActionListener (myListener);
      }
   }

   /**
    * Add a button to show or hide the group of contiguous tracks.
    * 
    * @param show
    * If true then display the 'show' button, if false display the 'hide' button
    */
   public void addShowHideButton (boolean show) {
      // make sure the previous show/hide button has been cleared away
      removeShowHideButton();

      // JToggleButton showHideButton;
      ShowHideButton showHideBtn = new ShowHideButton (show);

      showHideBtn.addActionListener (myListener);
      showHideBtn.setLocation (3, 4);

      add (showHideBtn);
      repaint();

      JToggleButton[] tempList = new JToggleButton[3];

      for (int i = 0; i < toggleButtons.length; i++) {
         tempList[i] = toggleButtons[i];
      }

      tempList[2] = showHideBtn;

      toggleButtons = tempList;
   }

   public void removeShowHideButton() {
      if (toggleButtons.length == 3) {
         remove (toggleButtons[2]);
         repaint();

         JToggleButton[] tempList = new JToggleButton[2];

         for (int i = 0; i < tempList.length; i++) {
            tempList[i] = toggleButtons[i];
         }

         toggleButtons = tempList;
      }
   }

   /**
    * Set whether a track is enabled or not.
    * <p>
    * Enabling a track allows it to be expanded, muted and edited. Disabling a
    * track means these actions can no longer be performed on the track. This is
    * done to the track that is the parent of a group when the group is shown
    * and hidden.
    * 
    * @param enabled if <code>true</code>, enables this track
    */
   public void setEnabled (boolean enabled) {
      isEnabled = enabled;

      for (int i = 0; i < toggleButtons.length; i++) {
         toggleButtons[i].setVisible (enabled);
      }
   }

   public int getProbeIndex (Probe probe) {
      ArrayList<ProbeInfo> pinfos = getProbeInfos();
      for (int i = 0; i < pinfos.size(); i++) {
         if (pinfos.get (i).getProbe() == probe) {
            return i;
         }
      }
      return -1;
   }

   /**
    * Check if a track is enabled or not.
    * 
    * @return isEnabled
    */
   public boolean isEnabled() {
      return isEnabled;
   }

   /**
    * initialize the track controls, by adding toggle buttons and setting the
    * dimentions of the workspace
    * 
    */

   private void trackControlInitialization() {
      setBorder (normalBorder);

      backgroundColor = isInputTrack ? 
         GuiStorage.COLOR_INPUTTRACK_WORKSPACE : 
         GuiStorage.COLOR_OUTPUTTRACK_WORKSPACE;

      dragging = false;
      addMouseListener (myListener);
      
      setLayout (null);
      setAlignmentX (JComponent.LEFT_ALIGNMENT);

      int maxWidth = GuiStorage.TIMELINE_PANEL_WIDTH;
      int maxHeight = GuiStorage.TRACK_NORMAL_HEIGHT;

      Dimension workspaceSize = new Dimension (maxWidth, maxHeight);
      setSize (workspaceSize);
      setMinimumSize (workspaceSize);
      setMaximumSize (workspaceSize);
      setPreferredSize (workspaceSize);

      nameLabel.setIconTextGap (1);
      nameLabel.setSize (50, 18);

      toggleButtons[0].setLocation (15, 1);
      toggleButtons[1].setLocation (35, 1);
      nameLabel.setLocation (60, 1);

      add (toggleButtons[0]);
      add (toggleButtons[1]);
      add (nameLabel);

      if (isInputTrack) {
         setBackground (GuiStorage.COLOR_INPUTTRACK_WORKSPACE);
      }
      else {
         setBackground (GuiStorage.COLOR_OUTPUTTRACK_WORKSPACE);
      }
   }

   /**
    * get the track number of the current track
    * 
    * @return track number
    */

   public Integer getTrackNumber() {
      return myIndex;
   }

   /**
    * update the display status of the tracks based on their internal states,
    * expanded or contracted
    * 
    * @param mode
    */

   private void updateAllProbesToggleStatus (int mode) {
      for (ProbeInfo probeInfo : probeInfos) {
         if (mode == Track.TRACK_EXPANDED) {
            probeInfo.expandProbe();
         }
         else if (mode == Track.TRACK_CONTRACTED) {
            probeInfo.contractProbe();
         }
         else if (mode == Track.TRACK_MUTED) {
            probeInfo.muteProbe();
         }
         else if (mode == Track.TRACK_UNMUTED) {
            probeInfo.unmuteProbe();
         }
      }
   }

   private void updateProbeIndices() {
      for (int i = 0; i < probeInfos.size(); i++) {
         probeInfos.get(i).setIndex (i);
      }
   }
   
   public void updateProbeSizesAndLocations() {
      for (int i = 0; i < probeInfos.size(); i++) {
         probeInfos.get(i).setAppropSizeAndLocation (true);
      }
   }

   public ProbeInfo addProbeFromRoot (Probe probe) {
      // create the new ProbeInfo object
      
      ProbeInfo newProbe = new ProbeInfo (myController, this, probe);
      
      double[] range = new double[2];
      int insertionPoint = calcInsertionPoint (range, newProbe);

      myController.getProbeTrack().add (newProbe.getDisplayArea());
      // Note: probeInfos will be updated because of call to calcInsertionPoint
      probeInfos.add (insertionPoint, newProbe);
      updateProbeIndices();

      refreshTrackChanges();
      return newProbe;
   }

   /**
    * 
    * [0] - start time [1] - stop time [2] - insertion index
    */
   public int calcInsertionPoint (double[] range, ProbeInfo pinfo) {
      int insertionPoint = 0;
      
      range[0] = pinfo.getStartTime();
      range[1] = pinfo.getStopTime();
      double duration = range[1]-range[0];

      ArrayList<ProbeInfo> pinfos = getProbeInfos();
      for (ProbeInfo probeInfo : pinfos) {
         // if there is overlap with existing probes, slide to the next slot
         if (probeInfo.isOverlapping (range[0], range[1])) {
            // slide the new probe to the next slot
            range[0] = probeInfo.getStopTime();
            range[1] = range[0] + duration;
            insertionPoint++;
         }
         else { // if no overlap, searh for the correct insertion point
            if (TimeBase.compare (range[0], probeInfo.getStopTime()) >= 0) {
               insertionPoint++;
            }
            else {
               break; // since insertion point already found
            }
         }

      } 
       return insertionPoint;
   }

   /**
    * find is there is enough space for the probe on this track
    * 
    * @param newProbe
    * The probe to be inserted if there is space in the track
    * @return If there is space for this probe (boolean)
    */

   public boolean hasSpaceForProbe (Probe newProbe) {
      ArrayList<ProbeInfo> pinfos = getProbeInfos();
      for (ProbeInfo pinfo : pinfos) {
         // return false if there is any overlap with probe.
         // assume that startTime <= stopTime.
         if (pinfo.isOverlapping (newProbe)) {
            return false;
         }
      }
      return true;
   } 
   
   private class ProbeInfoOrdering implements Comparator<ProbeInfo> {
      public boolean equals (Object obj) {
         return obj instanceof ProbeInfoOrdering;
      }
      
      public int compare (ProbeInfo pinfo0, ProbeInfo pinfo1) {
         double st0 = pinfo0.getStartTime();
         double st1 = pinfo1.getStartTime();
         if (st0 < st1) {
            return -1;
         }
         else if (st0 == st1) {
            return 0;
         }
         else {
            return 1;
         }
      }
   }
   
   public ArrayList<ProbeInfo> getProbeInfos() {
      if (!probeInfoSorted) {
         Collections.sort (probeInfos, new ProbeInfoOrdering());
         updateProbeIndices();
         probeInfoSorted = true;
      }
      return probeInfos;
   } 
   
   public int numProbeInfos() {
      return probeInfos.size();
   }

   /**
    * delete a probe from the track, need to give the index of the probe
    * 
    * @param pinfo probe information object
    * @param confirm confirm the deletion in the GUI
    */
   public void deleteProbe (ProbeInfo pinfo, boolean confirm) {
      if (confirmDelete ("Delete this probe?", confirm)) {
         RemoveComponentsCommand rmCmd = new RemoveComponentsCommand (
            "delete probe", pinfo.getProbe());
         myController.selectedProbes.remove (pinfo);
         myController.myMain.getUndoManager().saveStateAndExecute (rmCmd);
         myController.myMain.rerender();
      }
   }

   public boolean deleteProbe (Probe probe) {
      int idx = getProbeIndex (probe);
      if (idx != -1) {
         ArrayList<ProbeInfo> pinfos = getProbeInfos();
         ProbeInfo info = pinfos.remove (idx);
         info.dispose();

         myController.refreshProbeTrackDisplay();

         for (int i = 0; i < pinfos.size(); i++) {
            pinfos.get (i).setIndex (i);
         }
         refreshTrackChanges();
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Attach probe info to this track
    * 
    * @param pInfo probe info
    */
   public void attachProbeInfo (ProbeInfo pInfo) {
      double[] range = new double[2];
      int insertionPoint = calcInsertionPoint (range, pInfo);
      pInfo.setStartTime (range[0]);
      pInfo.setStopTime (range[1]);     
      
      pInfo.setParentTrack (this);
      // Note: probeInfos will be updated because of call to calcInsertionPoint
      probeInfos.add (insertionPoint, pInfo);
      updateProbeIndices();
      updateProbeSizesAndLocations();

      myController.refreshProbeTrackDisplay();
      refreshTrackChanges();
   }

   /** 
    * Detach probe info from this track
    * 
    * @param pInfo probe info
    */
   public void detachProbeInfo (ProbeInfo pInfo) {
      probeInfos.remove (pInfo);
      updateProbeIndices();

      myController.refreshProbeTrackDisplay();
      refreshTrackChanges();
   }

   /**
    * expand the track view
    * 
    */
   private void expandTrack() {
      isExpanded = true;
      updateToggleStatus (Track.TRACK_EXPANDED);
      updateAllProbesToggleStatus (Track.TRACK_EXPANDED);
      myController.updateComponentSizes();
      refreshTrackChanges();
   }

   /**
    * contract the track view
    * 
    */

   private void contractTrack() {
      isExpanded = false;
      updateToggleStatus (Track.TRACK_CONTRACTED);
      updateAllProbesToggleStatus (Track.TRACK_CONTRACTED);
      myController.updateComponentSizes();

      refreshTrackChanges();
   }

   /**
    * this is the beginning of the initialization section
    */

   private void overallInitialization (String nameTrack) {
      fieldsInitialization (nameTrack);
      toggleButtonsInitialization();
      trackControlInitialization();
   }

   public void muteTrack (boolean mute) {
      WayPointProbe wayProbe = myController.myMain.getRootModel().getWayPoints();
      double earliestTime;

      ArrayList<ProbeInfo> pinfos = getProbeInfos();
      if (pinfos.size() > 0) {
         earliestTime = pinfos.get(0).getStartTime();

         wayProbe.invalidateAfterTime (earliestTime);

         // if (myController.getCurrentTime() > earliestTime) {
         //    if (myController.myMain.getWorkspace().rootModelHasState()) {
         //       WayPoint wayPoint = 
         //          wayProbe.getNearestValidBefore (earliestTime);
            
         //       if (wayPoint != null) {
         //          myController.myScheduler.setTime (wayPoint);
         //       }
         //       else {
         //          myController.myScheduler.setTime (0);
         //       }
         //    }
         //    else {
         //       myController.myScheduler.setTime (earliestTime);
         //    }
         // }
      }

      if (mute) {
         updateAllProbesToggleStatus (Track.TRACK_MUTED);
         updateToggleStatus (Track.TRACK_MUTED);
      }
      else {
         updateAllProbesToggleStatus (Track.TRACK_UNMUTED);
         updateToggleStatus (Track.TRACK_UNMUTED);
      }
      myController.requestUpdateWidgets();
   }

   /**
    * update the toggle status
    * 
    * @param mode described which buttons to update
    */
   public void updateToggleStatus (int mode) {
      if (mode == Track.TRACK_EXPANDED) {
         toggleButtons[0].setToolTipText ("Contract Track");
         toggleButtons[0].setActionCommand ("Contract Track");
         toggleButtons[0].setIcon (GuiStorage.CONTRACT_TRACK_ICON);
      }
      else if (mode == Track.TRACK_CONTRACTED) {
         toggleButtons[0].setToolTipText ("Expand Track");
         toggleButtons[0].setActionCommand ("Expand Track");
         toggleButtons[0].setIcon (GuiStorage.EXPAND_TRACK_ICON);
      }
      else if (mode == Track.TRACK_MUTED) {
         toggleButtons[1].setToolTipText ("Un-mute Track");
         toggleButtons[1].setActionCommand ("Un-mute Track");
         toggleButtons[1].setIcon (GuiStorage.UNMUTE_TRACK_ICON);
      }
      else if (mode == Track.TRACK_UNMUTED) {
         toggleButtons[1].setToolTipText ("Mute Track");
         toggleButtons[1].setActionCommand ("Mute Track");
         toggleButtons[1].setIcon (GuiStorage.MUTE_TRACK_ICON);
      }
   }

   /**
    * confirmation dialog of probe deletion
    * 
    * @param confirmMessage
    * @param confirm confirm this deletion in the GUI
    * @return confirmation on delete Andrei performance improvements
    */

   private boolean confirmDelete (
      String confirmMessage, boolean confirm) {
      int confirmation = JOptionPane.YES_OPTION;

      // if individaul probe is deleted
      if (confirm) {
         confirmation =
            JOptionPane.showConfirmDialog (
               myController, confirmMessage, "Confirm",
               JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      }
      return (confirmation == JOptionPane.YES_OPTION);
   }

   protected void setIndex (int idx) {
      myIndex = idx;
   }

   /**
    * delete the track
    * 
    * @param isParentModelDeleted
    * Andrei's code simplification
    */

   public void deleteThisTrack (boolean isParentModelDeleted) {
      if (getProbeInfos().isEmpty()) {
         myController.deleteTrack ((isInputTrack) ? Track.TYPE_INPUT
            : Track.TYPE_OUTPUT, myIndex, !isParentModelDeleted);
      }
      else {
         GuiUtils.showError (
            myController,
            "All probes must be removed from track before deleting.");
      }
   }

   /**
    * change the track name
    * 
    */

   private void changeTrackName() {
      int confirmation = JOptionPane.showConfirmDialog (
         myController, "Change the name of the track?", "Confirm",
         JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

      if (confirmation == JOptionPane.YES_OPTION) {
         String newTrackName = JOptionPane.showInputDialog (
            myController, "Please input the new track name", "Track Name",
            JOptionPane.PLAIN_MESSAGE);
         
         if (newTrackName != null) {
            nameLabel.setText (" " + newTrackName);
         }
      }
   }

   private void viewTrackProperty() {}

   /**
    * update the track size
    */
   public void updateTrackSize (int trackLength, int workspaceWidth) {
      // update the size of the myTrackControl
      Dimension size = new Dimension (workspaceWidth, 
         (isExpanded) ? GuiStorage.TRACK_EXPAND_HEIGHT : GuiStorage.TRACK_NORMAL_HEIGHT);

      setSize (size);
      setMinimumSize (size);
      setMaximumSize (size);
      setPreferredSize (size);
      
      nameLabel.setSize (workspaceWidth - 75, nameLabel.getHeight ());

      // update the sizes of the probes
      updateProbeSizesAndLocations();
   }

   /**
    * is this a valid drag? this function determines if we have performed a
    * valid drag
    * 
    * @param pInfo probe info
    * @return is the drag valid or not
    */
   public boolean isDragValid (ProbeInfo pInfo, boolean isMove) {
      // check for overlap with every probe from this track
      // Note: don't call getProbeInfos() because probes don't need to
      // to be sorted and this methods may be called from the mouse
      // methods which cause the probe order to be changed
      for (ProbeInfo probeInfo : probeInfos) {
         if (!isMove || !myController.selectedProbes.contains (probeInfo)) {
            
            // overlap condition
            if (probeInfo.isOverlapping (pInfo.getProbe())) {
               return false;
            }
         }
      }
      return true;
   }

   /**
    * compute the track coordinates
    * 
    * @return Y coordinate to track
    */

   public int computeTrackYCoor() {
      return myController.getYCoorUpToTrack (this);
   }

   public void setExpanded (boolean expanded) {
      if (expanded) {
         expandTrack();
      }
      else {
         contractTrack();
      }
      isExpanded = expanded;
   }

   public boolean isExpanded() {
      return isExpanded;
   }

   public boolean isInputTrack() {
      return isInputTrack;
   }

   public void refreshTrackChanges() {
      validateTrackStatus();
      revalidate();
      repaint();
      myController.requestUpdateDisplay();
   }

   public void setMutable (boolean enable) {
      toggleButtons[1].setEnabled (enable);
   }

   /**
    * author: Chad Written to update that state of a track depending on the
    * status of its probes. Track status = [or] of probea.status, probeb.status
    * ...
    */

   private void validateTrackStatus() {
      if (probeInfos.size() > 0) {
         boolean trackStatus = false;

         for (ProbeInfo probeInfo : probeInfos) {
            if (probeInfo.isActive()) {
               trackStatus = true;
               break;
            }
         }
         
         updateToggleStatus (
            trackStatus ? Track.TRACK_UNMUTED : Track.TRACK_MUTED);
         toggleButtons[1].setSelected (!trackStatus);
      }
   }

   /**
    * Set if this track is visible or not.
    * 
    * @param value
    * The visible value.
    */
   public void setVisible (boolean value) {
      visible = value;
   }

   public boolean isVisible() {
      return visible;
   }

   /**
    * Set whether or not a track is part of a group.
    * 
    * @param value
    * The first track in the group if the track is being grouped, null if the
    * track is removed from the group.
    */
   public void setGrouped (Track value) {
      groupParent = value;
   }

   public Track getGroupParent() {
      return groupParent;
   }

   /**
    * Select a track for moving it around or grouping it with others.
    * 
    */
   public void select() {
      selected = true;
      setDecorated (true);
   }

   /**
    * Deselect a track.
    * 
    */
   public void deselect() {
      selected = false;
      setDecorated (false);
   }

   public boolean isSelected() {
      return selected;
   }

   public void appendProbes (LinkedList<Probe> list) {
      ArrayList<ProbeInfo> pinfos = getProbeInfos();
      for (int i = 0; i < pinfos.size(); i++) {
         list.add (pinfos.get(i).getProbe());
      }
   }

   public void updateProbeData() {
      ArrayList<ProbeInfo> pinfos = getProbeInfos();
      for (int i = 0; i < pinfos.size(); i++) {
         pinfos.get(i).updateProbeData();
         pinfos.get(i).updateLabelText();
      }      
   }

   public void dispose() {
      ArrayList<ProbeInfo> pinfos = getProbeInfos();
      for (ProbeInfo pInfo : pinfos) {
         pInfo.dispose();
      }
      pinfos.clear(); 
   }   

   private void showPopupMenu (Component component, int x, int y) {
      JPopupMenu popupMenu = new JPopupMenu();
      
      boolean showGroupItem = selected || (groupParent != null);
      boolean showOtherItems = !showGroupItem && isEnabled;
      
      if (showOtherItems) {   
         JMenuItem changeTrackNameItem = new JMenuItem ("Change track name");
         changeTrackNameItem.addActionListener (myListener);
         changeTrackNameItem.setActionCommand ("Change track name");
         popupMenu.add (changeTrackNameItem);
   
         JMenuItem deleteTrackItem = new JMenuItem ("Delete track");
         deleteTrackItem.addActionListener (myListener);
         deleteTrackItem.setActionCommand ("Delete track");
         popupMenu.add (deleteTrackItem);

         // TODO not implemented
         //popupMenu.addSeparator ();
         //JMenuItem viewPropertyItem = new JMenuItem ("View track property");
         //viewPropertyItem.addActionListener (myListener);
         //viewPropertyItem.setActionCommand ("View track property");
         // popupMenu.add (viewPropertyItem); TODO not implemented
      }
      
      if (showGroupItem) {
         if (showOtherItems) {
            popupMenu.addSeparator ();
         }
         
         JMenuItem groupTracks = new JMenuItem ("Group tracks");
         groupTracks.addActionListener (myListener);
         groupTracks.setActionCommand ("Group tracks");
         popupMenu.add (groupTracks);
   
         JMenuItem ungroupTracks = new JMenuItem ("Ungroup tracks");
         ungroupTracks.addActionListener (myListener);
         ungroupTracks.setActionCommand ("Ungroup tracks");         
         popupMenu.add (ungroupTracks);
         
         JMenuItem muteAllTracks = new JMenuItem ("Mute selected tracks");
         muteAllTracks.addActionListener (myListener);
         muteAllTracks.setActionCommand ("Mute selected tracks");
         popupMenu.add (muteAllTracks);
         
         JMenuItem unmuteAllTracks = new JMenuItem ("Unmute selected tracks");
         unmuteAllTracks.addActionListener (myListener);
         unmuteAllTracks.setActionCommand ("Unmute selected tracks");
         popupMenu.add (unmuteAllTracks);
      }
      
      popupMenu.show (component, x, y);
   }
   
   public void paint (Graphics g) {
      super.paint (g);
      g.setColor (Color.GRAY);

      if (groupParent != null && groupParent != myTrack) {
         g.drawLine (8, 0, 8, getHeight());
      }
      else if (groupParent != null && isEnabled) {
         // this is the group parent so draw a shorter line
         g.drawLine (8, 14, 8, getHeight());
      }
   }

   public void setDecorated (boolean decorated) {
      if (decorated) {
         setBackground (highlight);
         setBorder (bevelBorder);
      }
      else {
         setBackground (backgroundColor);
         setBorder (normalBorder);
      }
   }
   
   // =============================================================
   // Track ActionListener class
   // =============================================================

   /**
    * Listens for commands and mouse clicks associated with the track. Handles
    * expanding, deleting, muting track and other features such as
    * changing its name
    */
   private class TrackListener implements ActionListener, MouseListener {
      public void actionPerformed (ActionEvent e) {
         String nameOfAction = e.getActionCommand();

         if (nameOfAction == "Expand Track" && isEnabled) {
            setExpanded (true);
         }
         else if (nameOfAction == "Contract Track") {
            setExpanded (false);
         }
         else if (nameOfAction == "Mute Track") {
            muteTrack (true);
         }
         else if (nameOfAction == "Un-mute Track") {
            muteTrack (false);
         }
         else if (nameOfAction == "Change track name") {
            changeTrackName();
         }
         else if (nameOfAction == "Delete track") {
            deleteThisTrack (false);
         }
         else if (nameOfAction == "View track property") {
            viewTrackProperty();
         }
         else if (nameOfAction == "Hide Tracks") {
            myController.hideContiguousTracks (myTrack);
            setEnabled (false);
            addShowHideButton (true);
         }
         else if (nameOfAction == "Show Tracks") {
            myController.showContiguousTracks (myTrack);
            setEnabled (true);
            addShowHideButton (false);
         }
         else if (nameOfAction == "Group tracks") {
            myController.groupTracks();
         }
         else if (nameOfAction == "Ungroup tracks" && groupParent != null) {
            myController.ungroupTracks (groupParent);
         }
         else if (nameOfAction == "Mute selected tracks") {
            if (groupParent == null) {
               myController.muteTracks (true, myController.selectedTracks);
            }
            else {
               myController.muteTracks (
                  true, myController.getTrackGroup (groupParent));
            }
         }
         else if (nameOfAction == "Unmute selected tracks") {
            if (groupParent == null) {
               myController.muteTracks (false, myController.selectedTracks);
            }
            else {
               myController.muteTracks (
                  false, myController.getTrackGroup (groupParent));
            }
         }

         refreshTrackChanges();
      }

      public void mouseClicked (MouseEvent e) {
         
         if (e.getButton() == MouseEvent.BUTTON1 && 
             myController.isMultipleSelectionEnabled(e)) {
            if (!selected) {
               myController.addContiguousTrack (myTrack);
            }
            else {
               myController.removeContiguousTrack (myTrack);
            }
         }
         else if (e.isPopupTrigger()) {
            showPopupMenu (e.getComponent (), e.getX (), e.getY ());
         }
      }

      public void mousePressed (MouseEvent e) {
         if (e.getButton() == MouseEvent.BUTTON1) {
            dragging = true;

            if (!myController.isMultipleSelectionEnabled(e)) {
               if (!myController.isContiguousTrack (myTrack)) {
                  // if the track that was clicked on was not one of the
                  // contiguous tracks, remove the contiguous tracks
                  myTrack.select();
                  myController.removeAllContiguousTracks();
               }

               myController.addContiguousTrack (myTrack);
            }
         }
         else if (e.isPopupTrigger()) {
            showPopupMenu (e.getComponent (), e.getX (), e.getY ());
         }
      }

      public void mouseReleased (MouseEvent e) {
         if (e.getButton() == MouseEvent.BUTTON1) {
            dragging = false;

            if (!myController.isMultipleSelectionEnabled(e)) {
               boolean tracksMoved = myController.mouseReleased();
               myTrack.deselect();
               myController.removeAllContiguousTracks();

               if (!tracksMoved) {
                  GuiUtils.showError (
                     myController, "Input and output tracks can not be mixed.");
               }
            }
         }
         else if (e.isPopupTrigger()) {
            showPopupMenu (e.getComponent (), e.getX (), e.getY ());
         }
      }

      public void mouseEntered (MouseEvent e) {
         if (dragging == false) {
            myController.setLastEntered (
               myTrack, false).setBackground (highlight);
         }
         else {
            Track lastEnteredTrack =
               myController.setLastEntered (myTrack, true);

            if (!lastEnteredTrack.isSelected()) {
               lastEnteredTrack.setBorder (draggingBorder);
            }
         }
      }

      public void mouseExited (MouseEvent e) {
         Track currentTrack = myController.getCurrentlyEnteredTrack();

         if (!dragging && !selected && currentTrack != null) {
            currentTrack.setBackground (backgroundColor);
         }

         if (currentTrack != null) {
            currentTrack.setBorder (selected ? bevelBorder : normalBorder);
         }
      }
   }
   
   // ========================================================
   // ShowHideButton class to show/hide tracks
   // ========================================================

   private class ShowHideButton extends JToggleButton {
      private static final long serialVersionUID = 1L;
      private Shape shape;

      public ShowHideButton (boolean show) {
         super();

         if (show) {
            setIcon (GuiStorage.SHOW_TRACKS_ICON);
            setActionCommand ("Show Tracks");
         }
         else {
            setIcon (GuiStorage.HIDE_TRACKS_ICON);
            setActionCommand ("Hide Tracks");
         }

         Dimension btnSize = new Dimension (10, 10);
         setSize (btnSize);
         setMinimumSize (btnSize);
         setMaximumSize (btnSize);
         setPreferredSize (btnSize);
         setContentAreaFilled (false);

         shape = new Ellipse2D.Float (0, 0, btnSize.width, btnSize.height);
      }

      protected void paintBorder (Graphics g) {}

      /**
       * Detects if the button was clicked on or not
       */
      public boolean contains (int x, int y) {
         return shape.contains (x, y);
      }
   }
}
