package artisynth.core.gui.timeline;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

import artisynth.core.driver.Main;
import artisynth.core.probes.WayPoint;

public class WayPointInfo extends JPanel {
   private static final long serialVersionUID = 1L;
   protected WayPoint myWayPoint;
   private TimelineController myController;

   private int myIndex;

   private WayPointListener myListener;

   private int wayCoor;
   private int newCoor;
   private int prevCoor;

   protected final int WAYPOINT_INDICATOR_HEIGHT = 18;
   protected final int WAYPOINT_INDICATOR_WIDTH = 5;   

   boolean myHighlighted = false;

   // when the waypoint is created by the user
   public WayPointInfo (TimelineController controller, double time) {
      this (controller, new WayPoint (time));
      myController.myMain.getRootModel().addWayPoint (myWayPoint);
   }

   // when the waypoint is extracted from the root model
   public WayPointInfo (TimelineController controller, WayPoint way) {
      myController = controller;
      myWayPoint = way;

      myListener = new WayPointListener();
      displayInitialization();
   }

   public boolean removeWayPointFromRoot() {
      return myController.myMain.getRootModel().removeWayPoint (myWayPoint);
   }

   public void setWayMarkersLocation() {
      wayCoor = myController.timescale.getCorrespondingPixel (getTime());

      setBounds (wayCoor - 2, 1,
         GuiStorage.WAY_MARKER_SIZE.width, GuiStorage.WAY_MARKER_SIZE.height);
   }

   public void updateTimes() {
      setTime (myController.timescale.getCorrespondingTime (wayCoor));
   }

   public void updateWayPointIndex() {
      myIndex = myController.wayInfos.indexOf (this);
   }

   public void setValidityDisplay (boolean isHighlighted) {
      myHighlighted = isHighlighted;
      
      if (myWayPoint.isValid()) {
         if (isHighlighted) {
            setBackground (GuiStorage.COLOR_WAYPOINT_VALID_HIGHLIGHT);
         }
         else {
            setBackground (GuiStorage.COLOR_WAYPOINT_VALID_NORMAL);
         }
      }
      else {
         if (isHighlighted) {
            setBackground (GuiStorage.COLOR_WAYPOINT_INVALID_HIGHLIGHT);
         }
         else {
            setBackground (GuiStorage.COLOR_WAYPOINT_INVALID_NORMAL);
         }
      }
      //myController.requestUpdateDisplay();
   }

   void setIndex (int idx) {
      myIndex = idx;
   }

   public double getTime() {
      return myWayPoint.getTime();
   }

   public void setTime (double newTime) {
      myWayPoint.setTime (newTime);
   }

   private JPopupMenu getPopupMenu() {
      JPopupMenu menu = new JPopupMenu();

      JMenuItem delete = new JMenuItem ("Delete waypoint");
      delete.addActionListener (myListener);
      delete.setActionCommand ("Delete waypoint");

      JMenuItem viewProperty = new JMenuItem ("View waypoint property");
      viewProperty.addActionListener (myListener);
      viewProperty.setActionCommand ("View waypoint property");

      JMenuItem setBreakPoint;
      if (myWayPoint.isBreakPoint()) {
         setBreakPoint = new JMenuItem ("Disable breakpoint");
         setBreakPoint.setActionCommand ("Disable breakpoint");
      }
      else {
         setBreakPoint = new JMenuItem ("Enable breakpoint");
         setBreakPoint.setActionCommand ("Enable breakpoint");
      }
      setBreakPoint.addActionListener (myListener);

      if (getTime() > 0) {
         menu.add (delete);
      }
      
      menu.add (setBreakPoint);
      menu.addSeparator();
      menu.add (viewProperty);
      viewProperty.setEnabled (false);
      return menu;
   }

   private void displayInitialization() {     
      Dimension waypointDimension = new Dimension (
         GuiStorage.WAY_MARKER_SIZE.width, GuiStorage.WAY_MARKER_SIZE.height);
      setSize (waypointDimension);
      setMinimumSize (waypointDimension);
      setMaximumSize (waypointDimension);
      setPreferredSize (waypointDimension);
      
      setBorder (null);
      
      if (getTime() != 0) {
         addMouseListener (myListener);
         addMouseMotionListener (myListener);
      }

      setWayMarkersLocation();
      setValidityDisplay (false);
   }
   
   public void finalize() {
      if (getTime() != 0) {
         removeMouseListener (myListener);
         removeMouseMotionListener (myListener);
      }
      myWayPoint = null;
   }

   public void paint (Graphics g) {
      Color oldColor = g.getColor();
      Color drawColor;
      
      if (myHighlighted) {
         if (myWayPoint.isBreakPoint()) {
            drawColor = GuiStorage.COLOR_WAYPOINT_INVALID_HIGHLIGHT;
         }
         else {
            drawColor = GuiStorage.COLOR_WAYPOINT_VALID_HIGHLIGHT;
         }
      }
      else {
         if (myWayPoint.isBreakPoint()) {
            drawColor = GuiStorage.COLOR_WAYPOINT_INVALID_NORMAL;
         }
         else {
            drawColor = GuiStorage.COLOR_WAYPOINT_VALID_NORMAL;
         }
      }
      
      g.setColor (drawColor);
      
      if (myWayPoint.isValid()) {
         g.fillRect (0, 0, getWidth(), getHeight());
      }
      else {
         g.drawRect (0, 0, getWidth() - 1, getHeight() - 1);
      }
      
      g.setColor (oldColor);
   }

   private class WayPointListener extends MouseInputAdapter
      implements ActionListener {
      public void mousePressed (MouseEvent e) {
         if (!myController.myScheduler.isPlaying()) {
            if (e.isPopupTrigger())
               getPopupMenu().show (e.getComponent(), e.getX(), e.getY());

            // perform moving only on press of primary mouse button
            if (e.getButton() == MouseEvent.BUTTON1) {
               prevCoor = e.getX();

               // "this" myWayPoint becomes active
               myController.setActiveWayPointExist (true);
            }
            
            setValidityDisplay (true);
         }
      }

      public void mouseDragged (MouseEvent e) {
         if (!myController.myScheduler.isPlaying()) {
            int tempCoor = e.getX();

            // store the original properties prior to dragging
            int origWayCoor = wayCoor;
            double origWayTime = getTime();

            wayCoor = wayCoor + tempCoor - prevCoor;

            // update the time value given the new wayCoor
            updateTimes();
            
            myController.updateCurrentWayPointShadow (wayCoor);

            // recover back the original coordinates and times before dragging
            wayCoor = origWayCoor;
            setTime (origWayTime);
            myController.requestUpdateDisplay();
         }
      }

      public void mouseReleased (MouseEvent e) {
         if (!myController.myScheduler.isPlaying()) {
            if (e.isPopupTrigger()) {
               getPopupMenu().show (e.getComponent(), e.getX(), e.getY());
            }

            // perform moving only on release of primary mouse button
            if (e.getButton() == MouseEvent.BUTTON1) {
               newCoor = e.getX();

               // only update the times when the new mouse location
               // is different from the old mouse location
               if (newCoor != prevCoor) {
                  wayCoor = wayCoor + newCoor - prevCoor;

                  // update the time value given the new wayCoor
                  updateTimes();

                  // reorder the waypoint list based on the waypointTime
                  myController.updateWayPointListOrder (myIndex);

                  // reset the bounds and update the waypoints
                  setWayMarkersLocation();
               }
            }

            // this waypoint is not longer active
            myController.setActiveWayPointExist (false);
            setValidityDisplay (true);
            myController.requestUpdateDisplay();
         }
      }

      public void mouseEntered (MouseEvent e) {
         // if there is no other active components, activate highlighting
         if (!myController.isActiveWayPointExist()
             && !myController.isActiveProbeExist()) {
            setValidityDisplay (true);
         }
      }

      public void mouseExited (MouseEvent e) {
         if (!myController.isActiveWayPointExist()) {
            setValidityDisplay (false);
         }
      }

      public void actionPerformed (ActionEvent e) {
         String nameOfAction = e.getActionCommand();

         if (nameOfAction == "Delete waypoint") {
            myController.deleteWayPoint (myIndex, false);
         }
         else if (nameOfAction == "Disable breakpoint") {
            myWayPoint.setBreakPoint (false);
         }
         else if (nameOfAction == "Enable breakpoint") {
            myWayPoint.setBreakPoint (true);
         }
         else if (nameOfAction == "View waypoint Property") {
         }

         myController.requestUpdateDisplay();
      }
   }
}
