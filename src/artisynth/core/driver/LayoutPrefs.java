package artisynth.core.driver;

import java.awt.Color;
import javax.swing.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.widgets.*;
import maspack.widgets.GuiUtils.RelativeLocation;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.GL.GLViewer.*;
import maspack.render.GL.GLViewer;

import java.io.IOException;
import java.io.PrintWriter;

import artisynth.core.util.*;
import artisynth.core.util.*;
import artisynth.core.gui.*;
import artisynth.core.gui.timeline.*;
import artisynth.core.gui.jythonconsole.*;

/**
 * Preferences related to the GUI layout
 */
public class LayoutPrefs extends Preferences {

   private Main myMain;

   static PropertyList myProps = new PropertyList (LayoutPrefs.class);

   private int myViewerWidth = Main.DEFAULT_VIEWER_WIDTH;
   private int myViewerHeight = Main.DEFAULT_VIEWER_HEIGHT;
   private boolean myTimelineVisible = Main.DEFAULT_TIMELINE_VISIBLE;
   private int myTimelineWidth = Main.DEFAULT_TIMELINE_WIDTH;
   private int myTimelineHeight = Main.DEFAULT_TIMELINE_HEIGHT;
   private double myTimelineRange = Main.DEFAULT_TIMELINE_RANGE;
   private RelativeLocation myTimelineLocation = Main.DEFAULT_TIMELINE_LOCATION;
   private boolean myJythonFrameVisible = Main.DEFAULT_JYTHON_FRAME_VISIBLE;
   private RelativeLocation myJythonLocation = Main.DEFAULT_JYTHON_LOCATION;

   static {
      myProps.add (
         "viewerWidth",
         "main viewer width in pixels", Main.DEFAULT_VIEWER_WIDTH);
      myProps.add (
         "viewerHeight",
         "main viewer width in pixels", Main.DEFAULT_VIEWER_HEIGHT);
      myProps.add (
         "timelineVisible isTimelineVisible",
         "whether or not the timeline is visible", Main.DEFAULT_TIMELINE_VISIBLE);
      myProps.add (
         "timelineWidth",
         "main timeline width in pixels", Main.DEFAULT_TIMELINE_WIDTH);
      myProps.add (
         "timelineHeight",
         "main timeline width in pixels", Main.DEFAULT_TIMELINE_HEIGHT);
      myProps.add (
         "timelineRange", 
         "default temporal extent of the timeline",
         Main.DEFAULT_TIMELINE_RANGE, "[-1,inf] NS");
      myProps.add (
         "timelineLocation", 
         "default location of the timeline relative to the main frame",
         Main.DEFAULT_TIMELINE_LOCATION);
      myProps.add (
         "jythonFrameVisible isJythonFrameVisible",
         "whether or not the jython console frame is visible",
         Main.DEFAULT_JYTHON_FRAME_VISIBLE);
      myProps.add (
         "jythonLocation", 
         "default location of the Jython console relative to the main frame",
         Main.DEFAULT_JYTHON_LOCATION);
   }

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public LayoutPrefs (Main main) {
      myMain = main;
   }

   public int getViewerWidth () {
      return myViewerWidth;
   }

   public void setViewerWidth (int viewerWidth) {
      myViewerWidth = viewerWidth;
   }

   public int getViewerHeight () {
      return myViewerHeight;
   }

   public void setViewerHeight (int viewerHeight) {
      myViewerHeight = viewerHeight;
   }

   public boolean isTimelineVisible () {
      return myTimelineVisible;
   }

   public void setTimelineVisible (boolean timelineVisible) {
      myTimelineVisible = timelineVisible;
   }

   public int getTimelineWidth () {
      return myTimelineWidth;
   }

   public void setTimelineWidth (int timelineWidth) {
      myTimelineWidth = timelineWidth;
   }

   public int getTimelineHeight () {
      return myTimelineHeight;
   }

   public void setTimelineHeight (int timelineHeight) {
      myTimelineHeight = timelineHeight;
   }

   public double getTimelineRange () {
      return myTimelineRange;
   }

   public void setTimelineRange (double timelineRange) {
      myTimelineRange = timelineRange;
   }

   public RelativeLocation getTimelineLocation() {
      return myTimelineLocation;
   }

   public void setTimelineLocation (RelativeLocation loc) {
      myTimelineLocation = loc;
   }

   public boolean isJythonFrameVisible () {
      return myJythonFrameVisible;
   }

   public void setJythonFrameVisible (boolean visible) {
      myJythonFrameVisible = visible;
   }

   public RelativeLocation getJythonLocation () {
      return myJythonLocation;
   }

   public void setJythonLocation (RelativeLocation loc) {
      myJythonLocation = loc;
   }

   public void setFromCurrent() {
      Viewer viewer = myMain.getViewer();
      if (viewer != null) {
         setViewerWidth (viewer.getScreenWidth());
         setViewerHeight (viewer.getScreenHeight());
      }
      Timeline timeline = myMain.getTimeline();
      if (timeline != null) {
         setTimelineWidth (timeline.getWidth());
         setTimelineHeight (timeline.getHeight());
         setTimelineVisible (timeline.isVisible());
         setTimelineRange (myMain.getDefaultTimelineRange());
         setTimelineLocation (myMain.getTimelineLocation());
      }
      JFrame jyframe = myMain.getJythonFrame();
      setJythonFrameVisible (jyframe != null && jyframe.isVisible());
      setJythonLocation (myMain.getJythonLocation());
   }

   public void applyToCurrent() {
      Viewer viewer = myMain.getViewer();
      if (viewer != null) {
         myMain.setViewerSize (getViewerWidth(), getViewerHeight());
      }
      Timeline timeline = myMain.getTimeline();
      if (timeline != null) {
         timeline.setSize (getTimelineWidth(), getTimelineHeight());
         timeline.setVisible (isTimelineVisible());
         myMain.setDefaultTimelineRange (getTimelineRange());
         myMain.setTimelineLocation (getTimelineLocation());
      }
      if (myMain.getMainFrame() != null) {
         myMain.setJythonFrameVisible (isJythonFrameVisible());
      }
      myMain.setJythonLocation (getJythonLocation());
   }

   protected PropertyPanel createEditingPanel() {
      PropertyPanel panel = createDefaultEditingPanel();
      addLoadApplyButtons (panel);
      return panel;
   }

}

