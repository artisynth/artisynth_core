package artisynth.core.driver;

import java.awt.Color;
import javax.swing.*;

import maspack.util.*;
import maspack.util.Logger.LogLevel;
import maspack.matrix.*;
import maspack.widgets.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.GL.GLViewer.*;
import maspack.render.GL.GLViewer;

import java.io.IOException;
import java.io.PrintWriter;

import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.workspace.*;
import artisynth.core.mechmodels.MechSystemSolver.*;
import artisynth.core.mechmodels.CollisionManager.*;
import artisynth.core.util.*;
import artisynth.core.util.*;
import artisynth.core.gui.*;
import artisynth.core.gui.timeline.*;
import artisynth.core.gui.jythonconsole.*;
import maspack.solvers.*;
import maspack.properties.*;

/**
 * Preferences related to maintenance settings
 */
public class InteractionSettings extends SettingsBase {

   private Main myMain;

   public static final double DEFAULT_FRAME_RATE =
      Main.DEFAULT_FRAME_RATE;

   public static final boolean DEFAULT_ARTICULATED_TRANSFORMS =
      Main.DEFAULT_ARTICULATED_TRANSFORMS;

   public static final boolean DEFAULT_INIT_DRAGGERS_IN_WORLD =
      Main.DEFAULT_INIT_DRAGGERS_IN_WORLD;

   public static final double DEFAULT_REAL_TIME_SCALING = 1.0;

   public static final boolean DEFAULT_NAVIGATION_PANEL_LINES = false;

   public static PropertyList myProps =
      new PropertyList (InteractionSettings.class);

   static {
      myProps.add (
         "articulatedTransforms",
         "enforce joint constraints when transforming rigid bodies",
         DEFAULT_ARTICULATED_TRANSFORMS);
      myProps.add (
         "frameRate", 
         "rate (in Hz) at which the simulation is updated in the viewer",
         DEFAULT_FRAME_RATE, "NS");
      myProps.add (
         "realTimeScaling", 
         "scale factor for the ideal viewer simulation speed",
         DEFAULT_REAL_TIME_SCALING, "NS");
      myProps.add (
         "initDraggersInWorld", 
         "align initial dragger orientation with world (vs. local) coordinates",
         DEFAULT_INIT_DRAGGERS_IN_WORLD, "NS");
      myProps.add (
         "navigationPanelLines", 
         "draw lines between nodes in the navigation panel",
         DEFAULT_NAVIGATION_PANEL_LINES);
   }

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   /**
    * {@inheritDoc}
    */
   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public InteractionSettings (Main main) {
      myMain = main;
   }

   public boolean getArticulatedTransforms () {
      return myMain.getArticulatedTransformsEnabled();
   }

   public void setArticulatedTransforms (boolean enable) {
      myMain.setArticulatedTransformsEnabled (enable);
   }

   public double getFrameRate () {
      return myMain.getFrameRate();
   }

   public void setFrameRate (double rate) {
      myMain.setFrameRate (rate);
   }

   public boolean getInitDraggersInWorld() {
      return myMain.getInitDraggersInWorldCoords();
   }

   public void setInitDraggersInWorld(boolean enable) {
      myMain.setInitDraggersInWorldCoords(enable);
   }

   public double getRealTimeScaling() {
      return myMain.getRealTimeScaling();
   }

   public void setRealTimeScaling (double s) {
      myMain.setRealTimeScaling (s);
   }

   public boolean getNavigationPanelLines() {
      if (myMain.getMainFrame() != null) {
         return myMain.getMainFrame().getNavPanel().getLinesEnabled();
      }
      else {
         return false;
      }
   }

   public void setNavigationPanelLines (boolean enabled) {
      if (myMain.getMainFrame() != null) {
         myMain.getMainFrame().getNavPanel().setLinesEnabled (enabled);
      }
   }
}
