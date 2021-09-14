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
public class MaintenancePrefs extends Preferences {

   private Main myMain;

   static PropertyList myProps =
      new PropertyList (MaintenancePrefs.class);

   private boolean myTestSaveRestoreState = false;

   private LogLevel myLogLevel = Main.DEFAULT_LOG_LEVEL;

   static {
      myProps.add (
         "logLevel",
         "logger level for system messages", Main.DEFAULT_LOG_LEVEL);
      myProps.add (
         "testSaveRestoreState",
         "test save/restore state while models are running. Slows performance.",
         false);
   }

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public MaintenancePrefs (Main main) {
      myMain = main;
   }

   public LogLevel getLogLevel () {
      return myLogLevel;
   }

   public void setLogLevel (LogLevel level) {
      myLogLevel = level;
   }

   public boolean getTestSaveRestoreState () {
      return myTestSaveRestoreState;
   }

   public void setTestSaveRestoreState (boolean enable) {
      myTestSaveRestoreState = enable;
   }

   public void setFromCurrent() {
      setLogLevel (myMain.getLogLevel());
      setTestSaveRestoreState (RootModel.getTestSaveRestoreState());
   }

   public void applyToCurrent() {
      myMain.setLogLevel (getLogLevel());
      RootModel.setTestSaveRestoreState (getTestSaveRestoreState());
   }

   protected PropertyPanel createEditingPanel() {
      PropertyPanel panel = createDefaultEditingPanel();
      addLoadApplyButtons (panel);            
      return panel;
   }
}
