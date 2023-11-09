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

/**
 * Preferences related to maintenance settings
 */
public class InteractionPrefs extends Preferences {

   private InteractionSettings mySettings; // current application settings

   private double myFrameRate =
      InteractionSettings.DEFAULT_FRAME_RATE;

   private boolean myArticulatedTransforms =
      InteractionSettings.DEFAULT_ARTICULATED_TRANSFORMS;

   private boolean myInitDraggersInWorld =
      InteractionSettings.DEFAULT_INIT_DRAGGERS_IN_WORLD;

   private boolean myAlignDraggersToPoints =
      InteractionSettings.DEFAULT_ALIGN_DRAGGERS_TO_POINTS;

   private double myRealTimeScaling =
      InteractionSettings.DEFAULT_REAL_TIME_SCALING;

   private boolean myNavigationPanelLines =
      InteractionSettings.DEFAULT_NAVIGATION_PANEL_LINES;

   static PropertyList myProps =
      new PropertyList (InteractionPrefs.class, InteractionSettings.class);

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public InteractionPrefs (InteractionSettings settings) {
      mySettings = settings;
   }

   public boolean getArticulatedTransforms () {
      return myArticulatedTransforms;
   }

   public void setArticulatedTransforms (boolean enable) {
      myArticulatedTransforms = enable;
   }

   public double getFrameRate () {
      return myFrameRate;
   }

   public void setFrameRate (double rate) {
      myFrameRate = rate;
   }

   public boolean getInitDraggersInWorld() {
      return myInitDraggersInWorld;
   }

   public void setInitDraggersInWorld (boolean enable) {
      myInitDraggersInWorld = enable;
   }

   public boolean getAlignDraggersToPoints() {
      return myAlignDraggersToPoints;
   }

   public void setAlignDraggersToPoints(boolean enable) {
      myAlignDraggersToPoints = enable; 
   }

   public double getRealTimeScaling() {
      return myRealTimeScaling;
   }

   public void setRealTimeScaling (double scaling) {
      myRealTimeScaling = scaling;
   }

   public boolean getNavigationPanelLines() {
      return myNavigationPanelLines;
   }

   public void setNavigationPanelLines (boolean enabled) {
      myNavigationPanelLines = enabled;
   }

   public void setFromCurrent() {
      setFrameRate (mySettings.getFrameRate());
      setArticulatedTransforms (mySettings.getArticulatedTransforms());
      setInitDraggersInWorld (mySettings.getInitDraggersInWorld());
      setAlignDraggersToPoints (mySettings.getAlignDraggersToPoints());
      setRealTimeScaling (mySettings.getRealTimeScaling());
      setNavigationPanelLines (mySettings.getNavigationPanelLines());
   }

   public void applyToCurrent() {
      mySettings.setFrameRate (getFrameRate());
      mySettings.setArticulatedTransforms (getArticulatedTransforms());
      mySettings.setInitDraggersInWorld (getInitDraggersInWorld());
      mySettings.setAlignDraggersToPoints (getAlignDraggersToPoints());
      mySettings.setRealTimeScaling (getRealTimeScaling());
      mySettings.setNavigationPanelLines (getNavigationPanelLines());

      if (mySettings.getDialog() != null) {
         mySettings.getDialog().updateWidgetValues();
      }
   }

   protected PropertyPanel createEditingPanel() {
      PropertyPanel panel = createDefaultEditingPanel();
      addLoadApplyButtons (panel);            
      return panel;
   }
}
