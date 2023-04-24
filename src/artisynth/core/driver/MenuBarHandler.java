/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Window;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingWorker;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.DefaultMenuLayout;

import artisynth.core.driver.ModelScriptHistory.ModelScriptHistoryInfo;
import artisynth.core.driver.ModelScriptInfo.InfoType;
import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.ExtClassPathEditor;
import artisynth.core.gui.editorManager.Command;
import artisynth.core.gui.editorManager.ProbeEditor;
import artisynth.core.gui.editorManager.RemoveComponentsCommand;
import artisynth.core.gui.probeEditor.InputNumericProbeEditor;
import artisynth.core.gui.probeEditor.NumericProbeEditor;
import artisynth.core.gui.probeEditor.OutputNumericProbeEditor;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.gui.timeline.GuiStorage;
import artisynth.core.gui.widgets.ImageFileChooser;
import artisynth.core.gui.widgets.ProgressFrame;
import artisynth.core.inverse.InverseManager;
import artisynth.core.inverse.TrackingController;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.HasMenuItems;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelmenu.*;
import artisynth.core.modelmenu.LoadModelDialog;
import artisynth.core.modelmenu.ModelScriptActionEvent;
import artisynth.core.modelmenu.ModelScriptActionForwarder;
import artisynth.core.modelmenu.ModelScriptActionListener;
import artisynth.core.modelmenu.ModelScriptMenuEditor;
import artisynth.core.modelmenu.PreferencesEditor;
import artisynth.core.modelmenu.ScriptDialogBase.ScriptDesc;
import artisynth.core.probes.Probe;
import artisynth.core.probes.TracingProbe;
import artisynth.core.probes.WayPointProbe;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.JythonInit;
import artisynth.core.util.AliasTable;
import artisynth.core.workspace.PullController;
import artisynth.core.workspace.RootModel;
import maspack.properties.*;
import maspack.properties.PropertyUtils;
import maspack.render.GridPlane;
import maspack.render.RenderListener;
import maspack.render.RenderableUtils;
import maspack.render.Renderer.HighlightStyle;
import maspack.render.RendererEvent;
import maspack.render.Viewer;
import maspack.render.GL.GLViewer;
import maspack.solvers.PardisoSolver;
import maspack.util.GenericFileFilter;
import maspack.util.InternalErrorException;
import maspack.util.StringHolder;
import maspack.widgets.ButtonCreator;
import maspack.widgets.DoubleField;
import maspack.widgets.GridDisplay;
import maspack.widgets.GuiUtils;
//import maspack.widgets.MouseSettingsDialog;
import maspack.widgets.OptionPanel;
import maspack.widgets.PropertyDialog;
import maspack.widgets.PropertyPanel;
import maspack.widgets.RenderPropsDialog;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.widgets.ValueCheckListener;
import maspack.widgets.ViewerToolBar;
import maspack.util.*;

/**
 * to create a class that handles the main menu interactions responds to the
 * events and calls appropriate functions to deal with the events
 * 
 */
public class MenuBarHandler implements 
ActionListener, ValueChangeListener, SchedulerListener, RenderListener,
ModelScriptActionListener {
   private Main myMain;
   private MainFrame myFrame;

   protected ModelScriptMenu myModelMenu;  // model menu
   protected File myModelMenuFile; // file from which model menu was read
   protected ModelScriptMenuEditor myModelMenuEditor;  // edits the model menu

   protected ModelScriptMenu myScriptMenu;  // script menu
   protected File myScriptMenuFile; // file from which script menu was read
   protected ModelScriptMenuEditor myScriptMenuEditor;  // edits the script menu

   protected PreferencesEditor myPreferencesEditor;  // edits preferences
   protected ExtClassPathEditor myExtClassPathEditor; // edits external class path
   //protected ScriptPathEditor myScriptPathEditor; // edits script path
   protected StartupModelEditor myStartupModelEditor; // edits startup model

   public static final int MAX_MENU_ROWS = 20; // change to grid layout if larger

   protected JButton navBarButton;
   protected JButton rerenderButton;
   protected ImageIcon realtimeEnabledIcon;
   protected ImageIcon realtimeDisabledIcon;
   protected JButton realtimeButton;
   protected JButton resetStateButton;
   protected JButton resetButton;
   protected JButton backButton;
   protected JButton playButton;
   protected JButton singleStepButton;
   protected JButton forwardButton;
   protected JButton stopButton;
   // initialized in 
   protected Color defaultButtonBackground;
   protected Border defaultButtonBorder;

   // protected JMenu myScriptMenu;

   protected JToolBar modeSelectionToolbar = null;
   protected JPanel toolbarPanel;
   protected GridDisplay myGridDisplay;
   protected ViewerToolBar myViewerToolBar;
   protected DoubleField myTimeDisplay;
   protected DoubleField myStepDisplay;
   protected int myGridDisplayIndex;
   protected JMenuBar myMenuBar;
   protected JPanel myToolBar;
   protected File myViewerImageFile = null;
   protected final JColorChooser colorChooser = new JColorChooser();
   protected JMenu myApplicationMenu;
   protected boolean myApplicationMenuAddedP = false;

   private boolean isToolbarVisible = true;

   private RenderPropsDialog myPullControllerRenderPropsDialog;
   private PropertyDialog myPullControllerPropertyDialog;
   private MouseSettingsDialog myMouseSettingsDialog;
   private SettingsDialog myViewerSettingsDialog;
   private SettingsDialog mySimulationSettingsDialog;
   private SettingsDialog myInteractionSettingsDialog;
   private LoadModelDialog myLoadModelDialog;
   private RunScriptDialog myRunScriptDialog;

   private JFrame myArtisynthInfoFrame;
   private JFrame myModelInfoFrame;
   private JFrame myKeyBindingInfoFrame;

   private ModelScriptInfo myLastSelectedModel = null;

   private ModelScriptInfo myLastSelectedScript = null;

   public MenuBarHandler (Main parent, MainFrame theFrame) {
      myMain = parent;
      myFrame = theFrame;
   }

   public void initToolbar() {
      attachToolbar();
   }

   /**
    * Strips redundant "." parent folders from a file.
    */
   static File simplifyFile (File file) {
      if (file != null) {
         File parent = file.getParentFile();
         if (parent != null && parent.getName().equals(".")) {
            file = new File (parent.getParent(), file.getName());
         }
      }
      return file;
   }

//   class ScriptAction implements ActionListener {
//      String myScript;
//
//      ScriptAction (String script) {
//         myScript = script;
//      }
//
//     @Override
//     public void actionPerformed (ActionEvent e) {
//        runScript (myScript);
//     }
//   }

   protected JRadioButtonMenuItem makeRadioMenuItem(String name, String cmd) {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(name);
      item.addActionListener(this);
      item.setActionCommand(cmd);
      return item;
   }

   protected JMenuItem makeMenuItem (String name, String cmd) {
      JMenuItem item = new JMenuItem(name);
      item.addActionListener(this);
      item.setActionCommand(cmd);
      return item;
   }

   protected JMenuItem makeMenuItem(String name, String cmd, String tip) {
      JMenuItem item = new JMenuItem(name);
      item.addActionListener(this);
      if (cmd == null) {
         cmd = name;
      }
      item.setActionCommand(cmd);
      item.setToolTipText(tip);
      return item;
   }

   boolean isApplicationMenuEnabled() {
      return myApplicationMenuAddedP;
   }         

   void setApplicationMenuEnabled (boolean enable) {
      if (enable != myApplicationMenuAddedP) {
         if (enable) {
            myMenuBar.add (myApplicationMenu, myMenuBar.getMenuCount()-2);
         }
         else {
            myMenuBar.remove (myApplicationMenu);
         }
         myMenuBar.revalidate();
         myApplicationMenuAddedP = enable;
      }
   }         

   private void setButtonPressed (JButton button, boolean pressed) {
      if (pressed) {
         button.setBorder (new BevelBorder (BevelBorder.LOWERED));
         button.setBackground (Color.LIGHT_GRAY);
      }
      else {
         button.setBorder (defaultButtonBorder);
         button.setBackground (defaultButtonBackground);
      }
   }

   void updateModelMenu (JMenu menu) {
      int endItemCnt = 3; // num special menu items after the separator
      boolean hasEditItem = !myModelMenu.isSimple();
      if (hasEditItem) {
         endItemCnt++;
      }
      
      // disable/enable "Reload model" depending on whether a model is loaded
      JMenuItem item = menu.getItem (menu.getItemCount()-endItemCnt);
      item.setEnabled(myMain.modelIsLoaded());
      // Update "Load recent ..." submenu
      JMenu recentMenu = (JMenu)menu.getItem (menu.getItemCount()-endItemCnt+1);
      updateRecentMenu (
         recentMenu, "loadModel", InfoType.CLASS, InfoType.FILE);
      if (hasEditItem) {
         // disable/enable "Edit menu ..." depending on whether editor is open
         item = menu.getItem (menu.getItemCount()-1);
         item.setEnabled (!isWindowOpen (myModelMenuEditor));
      }
   }

   void updateScriptMenu (JMenu menu) {
      int endItemCnt = 2; // num special menu items after the separator
      boolean hasEditItem = !myScriptMenu.isSimple();
      if (hasEditItem) {
         endItemCnt++;
      }
      
      // Update "Run recent ..." submenu
      JMenu recentMenu = (JMenu)menu.getItem (menu.getItemCount()-endItemCnt);
      updateRecentMenu (
         recentMenu, "runScript", InfoType.SCRIPT);
      if (hasEditItem) {
         // disable/enable "Edit menu ..." depending on whether editor is open
         JMenuItem item = menu.getItem (menu.getItemCount()-1);
         item.setEnabled (!isWindowOpen (myScriptMenuEditor));
      }
   }

   /**
    * creates menu items
    */
   public void createMenus() {
      JPopupMenu.setDefaultLightWeightPopupEnabled(false);
      myMenuBar = new JMenuBar();
      myToolBar = new JPanel();
      myToolBar.setLayout(new BoxLayout(myToolBar, BoxLayout.LINE_AXIS));
      myToolBar.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

      // File menu
      JMenu menu = new JMenu("File");
      menu.addMenuListener(new MenuListener() { 
         public void menuCanceled(MenuEvent m_evt) {
         }

         public void menuDeselected(MenuEvent m_evt) {
            JMenu menu = (JMenu)m_evt.getSource();
            menu.removeAll();
         }

         public void menuSelected(MenuEvent m_evt) {
            createFileMenu((JMenu)m_evt.getSource());
         }
     });

      myMenuBar.add(menu);

      // Create the model menu
      menu = new JMenu("Models");
      menu.addMenuListener(new MenuListener() {
         public void menuCanceled(MenuEvent m_evt) {
         }

         public void menuDeselected(MenuEvent m_evt) {
         }

         public void menuSelected(MenuEvent m_evt) {
            updateModelMenu((JMenu)m_evt.getSource());
         }
      });
      myMenuBar.add(menu);
      createModelMenu(menu);

      // Create the scripts menu
      menu = new JMenu("Scripts");
      menu.addMenuListener(new MenuListener() {
         public void menuCanceled(MenuEvent m_evt) {
         }

         public void menuDeselected(MenuEvent m_evt) {
         }

         public void menuSelected(MenuEvent m_evt) {
            updateScriptMenu((JMenu)m_evt.getSource());
         }
      });
      myMenuBar.add(menu);
      createScriptMenu(menu);

      // "Edit" menu:
      menu = new JMenu("Edit");
      menu.addMenuListener(new MenuListener() {
         public void menuCanceled(MenuEvent m_evt) {
         }

         public void menuDeselected(MenuEvent m_evt) {
            JMenu menu = (JMenu)m_evt.getSource();
            menu.removeAll();
         }

         public void menuSelected(MenuEvent m_evt) {
            createEditMenu((JMenu)m_evt.getSource());
         }
      });

      myMenuBar.add(menu);

      // "Settings" menu:
      menu = new JMenu("Settings");
      menu.addMenuListener(new MenuListener() {
         public void menuCanceled(MenuEvent m_evt) {
         }

         public void menuDeselected(MenuEvent m_evt) {
            JMenu menu = (JMenu)m_evt.getSource();
            menu.removeAll();
         }

         public void menuSelected(MenuEvent m_evt) {
            createSettingsMenu((JMenu)m_evt.getSource());
         }
      });

      myMenuBar.add(menu);

      // Create the view menu
      menu = new JMenu("View");
      menu.addMenuListener(new MenuListener() {
         public void menuSelected(MenuEvent m_evt) {
            createViewMenu((JMenu)m_evt.getSource());
         }

         public void menuDeselected(MenuEvent m_evt) {
            JMenu menu = (JMenu)m_evt.getSource();
            menu.removeAll();
         }

         public void menuCanceled(MenuEvent m_evt) {
         }
      });

      myMenuBar.add(menu);

      // Help menu section
      menu = new JMenu("Help");
      menu.addMenuListener(new MenuListener() {
         public void menuCanceled(MenuEvent m_evt) {
         }

         public void menuDeselected(MenuEvent m_evt) {
            JMenu menu = (JMenu)m_evt.getSource();
            menu.removeAll();
         }

         public void menuSelected(MenuEvent m_evt) {
            createHelpMenu((JMenu)m_evt.getSource());
         }
      });

      myMenuBar.add(menu);

      // Application menu section
      menu = new JMenu("Application");
      menu.addMenuListener(new MenuListener() {
         public void menuCanceled(MenuEvent m_evt) {
         }

         public void menuDeselected(MenuEvent m_evt) {
            JMenu menu = (JMenu)m_evt.getSource();
            menu.removeAll();
         }

         public void menuSelected(MenuEvent m_evt) {
            createApplicationMenu((JMenu)m_evt.getSource());
         }
      });
      myApplicationMenu = menu;

      // Adding iconic buttons
      // Create a space separator
      // height makes space for GridDisplay box
      myMenuBar.add(Box.createRigidArea(new Dimension(20, 28)));
      // Create navigation bar button
      if (myFrame.getNavPanel().isExpanded()) {
         navBarButton = ButtonCreator.createIconicButton(
            GuiStorage.getNavBarIcon(),
            "Hide NavPanel", "Hide navigation panel", 
            ButtonCreator.BUTTON_ENABLED, false, this);
      }
      else {
         navBarButton = ButtonCreator.createIconicButton(
            GuiStorage.getNavBarIcon(),
            "Show NavPanel", "Show navigation panel", 
            ButtonCreator.BUTTON_ENABLED, false, this);
      }

      defaultButtonBackground = navBarButton.getBackground();
      defaultButtonBorder = navBarButton.getBorder();

      myToolBar.add(Box.createRigidArea(new Dimension(2, 0)));
      myToolBar.add(navBarButton);

      myToolBar.add(Box.createRigidArea(new Dimension(2, 0)));
      resetStateButton = ButtonCreator.createIconicButton(
         GuiUtils.loadIcon(ControlPanel.class, "icon/resetState.png"),
         "Reset initial state", "Reset the initial state", 
         ButtonCreator.BUTTON_ENABLED, false, this);
      myToolBar.add(resetStateButton);

      myToolBar.add(Box.createRigidArea(new Dimension(2, 0)));
      rerenderButton = ButtonCreator.createIconicButton(
         GuiUtils.loadIcon(ControlPanel.class, "icon/refresh.png"),
         "Rerender", "Rerender all displays",
         ButtonCreator.BUTTON_ENABLED, false, this);

      myToolBar.add(rerenderButton);

      myToolBar.add(Box.createRigidArea(new Dimension(2, 0)));
      realtimeButton = ButtonCreator.createIconicButton(
         GuiUtils.loadIcon(ControlPanel.class, "icon/clock.png"),
         "Disable real-time", "Enables real-time simulation when pressed", 
         ButtonCreator.BUTTON_ENABLED, false, this);
      myToolBar.add(realtimeButton);
      setButtonPressed (realtimeButton, true);

      // Charles modification
      // creates a message Box. Currently only used for Units
      // grid not visible reset textField
      myToolBar.add(Box.createRigidArea(new Dimension(10, 0)));
      myToolBar.add(Box.createHorizontalGlue());

      myGridDisplayIndex = myToolBar.getComponentCount();
      myToolBar.add(GridDisplay.createPlaceHolder());

      myStepDisplay = createStepDisplay();
      myToolBar.add(myStepDisplay);

      myTimeDisplay = createTimeDisplay();
      myToolBar.add(myTimeDisplay);
      myToolBar.add(Box.createRigidArea(new Dimension(4, 0)));

      // Create reset button
      resetButton = ButtonCreator.createIconicButton(
         GuiStorage.getResetIcon(),
         "Reset", "Reset time to 0",
         ButtonCreator.BUTTON_ENABLED, false, this);
      myToolBar.add(resetButton);
      myToolBar.add(Box.createRigidArea(new Dimension(2, 0)));

      // Create back button
      backButton = ButtonCreator.createIconicButton(
         GuiStorage.getRewindIcon(),
         "Skip back", "Skip back to previous valid waypoint",
         ButtonCreator.BUTTON_ENABLED, false, this);
      myToolBar.add(backButton);
      myToolBar.add(Box.createRigidArea(new Dimension(2, 0)));

      // Create play button
      playButton = ButtonCreator.createIconicButton(
         GuiStorage.getPlayIcon(),
         "Play", "Start simulation",
         ButtonCreator.BUTTON_DISABLED, false, this);
      myToolBar.add(playButton);
      myToolBar.add(Box.createRigidArea(new Dimension(2, 0)));

      // Create step button
      singleStepButton = ButtonCreator.createIconicButton(
         GuiStorage.getStepForwardIcon(),
         "Single step", "Advance simulation by a single time step",
         ButtonCreator.BUTTON_DISABLED, false, this);

      myToolBar.add(singleStepButton);
      myToolBar.add(Box.createRigidArea(new Dimension(2, 0)));

      // Create back button
      forwardButton = ButtonCreator.createIconicButton(
         GuiStorage.getFastForwardIcon(),
         "Skip forward", "Skip forward to next valid waypoint",
         ButtonCreator.BUTTON_DISABLED, false, this);
      myToolBar.add(forwardButton);
      myToolBar.add(Box.createRigidArea(new Dimension(2, 0)));
      forwardButton.setEnabled (false);

      // Create stop button
      stopButton = ButtonCreator.createIconicButton(
         GuiStorage.getStopAll(),
         "Stop all", "Stop simulation and all Jython commands and scripts",
         ButtonCreator.BUTTON_ENABLED, false, this);

      myToolBar.add(stopButton);
      myToolBar.add(Box.createRigidArea(new Dimension(2, 0)));
      stopButton.setEnabled (true);

      // Set the menu bar
      myFrame.setJMenuBar(myMenuBar);
      myFrame.add(myToolBar, BorderLayout.NORTH);
   }

   /**
    * For debugging only
    */
   public JMenuBar getMenuBar() {
      return myMenuBar;
   }

   private File readModelOrScriptMenu (
      ModelScriptMenu menu, String altFileName, String altOption) {

      File menuFile = null;
      if (altFileName != null) {
         menuFile = new File (altFileName);
         if (!menuFile.canRead()) {
            System.out.println (
               "WARNING: can't find or read file specified by "+
               "'"+altOption+"': " + menuFile);
            menuFile = null;
         }
      }
      if (menuFile == null) {
         // use the default
         menuFile = menu.getOrCreateDefaultFile();
      }
      if (menuFile != null) {
         try {
            menu.read (menuFile);
         }
         catch (Exception e) {
            System.out.println (
               "WARNING: error reading menu file "+menuFile+": "+e);
         }
      }
      return menuFile;
   }

   public void createScriptMenu (JMenu menu) {
      myScriptMenu = 
         new ModelScriptMenu (
            ModelScriptMenu.Type.SCRIPT, 
            menu, this, null);

      myScriptMenuFile = readModelOrScriptMenu (
         myScriptMenu, myMain.getScriptMenuFilename(), "-scriptMenu");
   }
   
   void updateDemosMenu() {
      myModelMenu.updatePackageEntries();
   }

   private void createModelMenu(JMenu menu) {
      myModelMenu = 
         new ModelScriptMenu (
            ModelScriptMenu.Type.MODEL, 
            menu, this, myMain.myRootModelManager);
      
      if (myMain.getDemoFilename() != null) {
         // explicitly specified simple menu
         File file = new File (myMain.getDemoFilename());
         try {
            myModelMenu.readSimpleMenu (file);
         }
         catch (Exception e) {
            System.out.println (
               "WARNING: can't find or read file specified by "+
               "'-demoFile': " + file+ ": "+e);
         }        
      }
      else {
         myModelMenuFile = readModelOrScriptMenu (
            myModelMenu, myMain.getModelMenuFilename(), "-modelMenu");
      }
   }

   private DoubleField createTimeDisplay() {
      DoubleField display = new DoubleField("", 0, "%9.5f");
      display.setToolTipText("current time");
      display.getTextField().setToolTipText("current time");
      display.setEnabledAll(false);
      display.setColumns(7);
      return display;
   }

   private DoubleField createStepDisplay() {
      DoubleField display = new DoubleField("step:", 0, "%7.5f");
      display.setToolTipText("maximum step size");
      display.getTextField().setToolTipText("maximum step size");
      display.setColumns(5);
      display.addValueChangeListener(this);
      display.addValueCheckListener(
         new ValueCheckListener() {
            public Object
            validateValue(ValueChangeEvent e, StringHolder errMsg) {
               double dval = ((Double)e.getValue()).doubleValue();
               if (dval <= 0) {
                  return PropertyUtils.illegalValue(
                     "value must be positive", errMsg);
               }
               else {
                  return PropertyUtils.validValue(e.getValue(), errMsg);
               }
            }
         });
      return display;
   }

   void updateTimeDisplay() {
      myTimeDisplay.setValue(myMain.getTime());
   }

   void updateStepDisplay() {
      myStepDisplay.setValue(myMain.getMaxStep());
   }

   private void doBlankMechmodel() {
      doClearModel();
      myMain.getRootModel().addModel(new MechModel());
   }

   private void doClearModel() {
      myMain.clearRootModel();
      myMain.setRootModel(new RootModel(), null);
   }

   private void doSaveModel() {
      File modelFile = myMain.getModelFile();
      if (modelFile != null) {
         try {
            myMain.saveModelFile(modelFile);
         }
         catch (Exception e) {
            e.printStackTrace();
            GuiUtils.showError (myFrame, "Error writing "+modelFile.getPath());
         }
      }
   }

   private void doSaveModelAs() {
      ModelFileChooser chooser =
         new ModelFileChooser (
            myMain.getModelFile(),
            myMain.getSaveCoreOnly(),
            myMain.getSaveWayPointData());
      
      if (chooser.showDialog(myFrame, "Save As") ==
          JFileChooser.APPROVE_OPTION) {
         File file = simplifyFile(chooser.getSelectedFileWithExtension());
         if (file.exists() && !GuiUtils.confirmOverwrite (myFrame, file)) {
            return;
         }
         int status = 0;
         try {
            status = myMain.saveModelFile (
               file, null,
               chooser.getSaveWayPointData(),
               chooser.getCoreCompsOnly());
         }
         catch (Exception e) {
            e.printStackTrace();
            GuiUtils.showError (
               myFrame, "Error writing " + file.getPath());
         }
         if (status > 0) {
            GuiUtils.showNotice (
               myFrame, "Removed "+status+" non-core components");
         }
      }
   }

   private void doReloadModel() {
      try {
         myMain.reloadModel();
      } catch (Exception e) {
         e.printStackTrace();
         GuiUtils.showError (myFrame, "Error reloading model:\n" + e);
      }
   }

   private void doLoadModel() {
      File file = selectFile("Load", myMain.getModelFile());
      if (file != null) {
         ModelScriptInfo modelInfo = new ModelScriptInfo (
            InfoType.FILE, file.getAbsolutePath(), file.getName(), null);
         if (!myMain.loadModel(modelInfo)) {
            GuiUtils.showError (myFrame, myMain.getErrorMessage());
         }
         else {
            myLastSelectedModel = modelInfo;
         }
      }
   }

   private void doLoadFromClass() {
      // look for 'last model' context to initialize the class selection menu
      ModelScriptInfo lastModel = myLastSelectedModel;
      if (lastModel == null && myMain.myModelScriptHistory != null) {
         lastModel =
            myMain.myModelScriptHistory.getMostRecent(InfoType.CLASS);
      }
      if (lastModel == null) {
         // no context - just use a blank
         lastModel = new ModelScriptInfo (ModelScriptInfo.InfoType.CLASS, "", "", null);
      }
      ModelScriptInfo modelInfo = selectClass (lastModel);
      if (modelInfo != null) {
         if (!myMain.loadModel(modelInfo)) {
            GuiUtils.showError (myFrame, myMain.getErrorMessage());
         }
         else {
            myLastSelectedModel = modelInfo;
            // load waypoints if specified
            File waypntsFile = myLoadModelDialog.getWaypointsFile();
            if (waypntsFile != null) {
               try {
                  myMain.loadWayPoints (waypntsFile);
               }
               catch (IOException e) {
                  String errMsg =
                     "Error loading waypoints "+waypntsFile+"\n"+e.getMessage();
                  GuiUtils.showError (myFrame, errMsg);
               }
            }
         }
      }
   }

   private void doRunScript() {
      // look for 'last script' context to initialize the script selection menu
      ModelScriptInfo lastScript = myLastSelectedScript;
      if (lastScript == null && myMain.myModelScriptHistory != null) {
         lastScript =
            myMain.myModelScriptHistory.getMostRecent(InfoType.SCRIPT);
      }
      if (lastScript == null) {
         // no context - just use a blank
         lastScript = new ModelScriptInfo (ModelScriptInfo.InfoType.SCRIPT, "", "", null);
      }
      ModelScriptInfo scriptInfo = selectScript (lastScript);
      if (scriptInfo != null) {
         myMain.runScript (scriptInfo);
         myLastSelectedScript = scriptInfo;
      }
   }

   /**
    * add an input probe into model
    */
   private void doAddInputProbe() {
      addProbe(true);
   }

   /**
    * add an output probe into model
    */
   private void doAddOutputProbe() {
      addProbe(false);
   }

   /**
    * add a control panel to the model
    */
   private void doAddControlPanel() {
      RootModel root = myMain.getRootModel();
      int number = root.getControlPanels().nextComponentNumber();
      ControlPanel panel =
      new ControlPanel("panel " + number, "LiveUpdate Close");

      GuiUtils.locateVertically(
         panel.getFrame(), myMain.getFrame(), GuiUtils.CENTER);
      GuiUtils.locateHorizontally(
         panel.getFrame(), myMain.getFrame(), GuiUtils.RIGHT);
      root.addControlPanel(panel);
   }

   /**
    * Load a control panel into the model
    */
   private void doLoadControlPanel() {
      RootModel root = myMain.getRootModel();
      JFileChooser chooser = new JFileChooser();
      chooser.setCurrentDirectory(myMain.getModelDirectory());
      int retVal = chooser.showDialog(myFrame, "Load");
      if (retVal == JFileChooser.APPROVE_OPTION) {
         File file = chooser.getSelectedFile();
         ControlPanel panel = null;
         try {
            panel =
            (ControlPanel)ComponentUtils.loadComponent(
               file, root, ControlPanel.class);
         } catch (Exception e) {
            GuiUtils.showError(
               myFrame,"Error reading file: " + e.getMessage());
         }
         if (panel != null) {
            root.addControlPanel(panel);
            myMain.setModelDirectory(chooser.getCurrentDirectory());
         }
      }
   }

   private void doUndoCommand() {
      myMain.getUndoManager().undoLastCommand();
   }

   private void doSaveOutputProbeData() {
      RootModel root = myMain.getRootModel();
      if (root != null) {
         ProbeEditor.saveAllOutputData (root, myFrame);
      }
   }

   private void doSaveAllProbeData() {
      RootModel root = myMain.getRootModel();
      if (root != null) {
         ProbeEditor.saveAllInputData (root, myFrame);
         ProbeEditor.saveAllOutputData (root, myFrame);
         ProbeEditor.saveWayPointData (root, myFrame);
      }
   }

   private File selectProbeDir(String approveMsg, File existingFile) {
      JFileChooser chooser =
      new JFileChooser(myMain.getProbeDirectory());
      chooser.setApproveButtonText(approveMsg);
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      int retval;
      if (approveMsg.startsWith ("Save")) {
         retval = chooser.showDialog (myFrame, approveMsg);
      }
      else {
         retval = chooser.showOpenDialog(myFrame);
      }
      File dir = null;
      if (retval == JFileChooser.APPROVE_OPTION) {
         dir = chooser.getSelectedFile();
      }
      if (dir != null && approveMsg == "Load") {
         File probesFile = new File(dir, "probeInfo.art");
         if (!probesFile.isFile() || !probesFile.canRead()) {
            GuiUtils.showError (
               myFrame, "File 'probeInfo.art' does not exist or " +
               "cannot be read in chosen directory");
            dir = null;
         }
      }
      return dir;
   }

   private boolean saveProbesFile(File dir) {
      File probesFile = new File(dir, "probeInfo.art");
      try {
         if (!myMain.saveProbesFile(probesFile)) {
            return false;
         }
      } catch (IOException e) {
         e.printStackTrace();
         GuiUtils.showError( myFrame, "Error writing " + probesFile.getPath());
         return false;
      }
      return true;
   }

   private boolean loadProbesFile(File dir) {
      File probesFile = new File(dir, "probeInfo.art");
      try {
         if (!myMain.loadProbesFile(probesFile)) {
            return false;
         }
      } catch (IOException e) {
         e.printStackTrace();
         GuiUtils.showError (myFrame, "Error loading " + probesFile.getPath());
         return false;
      }
      return true;
   }

   /**
    * save the probes in a new directory
    */
   private void newSaveProbesIn() {
      File dir = selectProbeDir("Save As", myMain.getProbeDirectory());
      if (dir != null) {
         if (saveProbesFile(dir)) {
            myMain.setProbeDirectory(dir);
            doSaveAllProbeData();
         }
      }
   }

   /**
    * save the probes
    */
   private void newSaveProbes() {
      if (saveProbesFile(myMain.getProbeDirectory())) {
         doSaveAllProbeData();
      }
   }

   private void newLoadProbesFrom() {
      File dir = selectProbeDir("Load", myMain.getProbeDirectory());
      if (dir != null) {
         File oldDir = myMain.getProbeDirectory();
         myMain.setProbeDirectory(dir);
         if (!loadProbesFile(dir)) {
            myMain.setProbeDirectory(oldDir);
         }
      }
   }

   /**
    * save the waypoints
    */
   private void doSaveWayPoints() {
      RootModel root = myMain.getRootModel();
      if (root != null) {
         ProbeEditor.saveWayPointData (root, myFrame);
      }
   }

   private void doSaveWayPointsAs() {
      RootModel root = myMain.getRootModel();
      if (root == null) {
         return;
      }
      WayPointProbe wayPoints = root.getWayPoints();
      String oldFileName = wayPoints.getAttachedFileName();
      if (myMain.getTimeline().setWayPointsFileFromUser (myFrame, "Save As")) {
         if (!ProbeEditor.saveWayPointData (root, myFrame)) {
            wayPoints.setAttachedFileName(oldFileName);
         }
      }
   }

   /**
    * load the wayPoints into the model
    */
   private void doLoadWayPoints() {
      RootModel root = myMain.getRootModel();
      if (root == null) {
         return;
      }
      WayPointProbe wayPoints = root.getWayPoints();
      String oldFileName = wayPoints.getAttachedFileName();
      if (myMain.getTimeline().setWayPointsFileFromUser (myFrame, "Load")) {
         if (!myMain.getTimeline().loadWayPointsFromAttachedFile (myFrame)) {
            wayPoints.setAttachedFileName(oldFileName);
         }
      }
      // not sure why we need to rerender here but *not* if called from timeline
      myMain.rerender();
   }

   /**
    * reload the wayPoints into the model
    */
   private void doReloadWayPoints() {
      RootModel root = myMain.getRootModel();
      if (root == null) {
         return;
      }
      WayPointProbe wayPoints = root.getWayPoints();
      String fileName = wayPoints.getAttachedFileName();
      if (fileName != null) {
         if (!myMain.getTimeline().loadWayPointsFromAttachedFile (myFrame)) {
            wayPoints.setAttachedFileName(null);
         }
      }
      // not sure why we need to rerender here but *not* if called from timeline
      myMain.rerender();
   }

   /**
    * delete the wayPoints from the model
    */
   private void doDeleteWayPoints() {
      RootModel root = myMain.getRootModel();
      if (root == null) {
         return;
      }
      if (GuiUtils.confirmAction (myFrame, "Delete waypoints?")) {
         myMain.getTimeline().clearWayPoints();
         // not sure why rerender needed here but *not* if called from timeline
         myMain.rerender();
      }
   }

   private void saveViewerImage() {
      ImageFileChooser chooser = new ImageFileChooser (myViewerImageFile);
      int returnVal = chooser.showValidatedDialog(myFrame, "Save");
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         File file = chooser.getSelectedFileWithExtension();
         boolean confirm = true;
         if (file.exists()) {
            confirm = GuiUtils.confirmOverwrite (myFrame, file);
         }
         if (confirm) {
            GLViewer viewer = myMain.getViewer();
            viewer.setupScreenShot(
               viewer.getScreenWidth(), viewer.getScreenHeight(),
               file, chooser.getSelectedFileExtension());
            viewer.repaint();
         }
         myViewerImageFile = file;
      }
   }

   private void doSetWorkingDirectory() {
      JFileChooser chooser = new JFileChooser();

      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setAcceptAllFileFilterUsed(false);
      FolderFileFilter filter = new FolderFileFilter("Folders");
      chooser.addChoosableFileFilter (filter);
      chooser.setFileFilter (filter);
      // hack to show current working folder
      File workingDir = new File(ArtisynthPath.getWorkingDir().getAbsolutePath());
      if (workingDir.getName().equals (".")) {
         workingDir = workingDir.getParentFile();
      }
      chooser.setSelectedFile(workingDir);
      int returnVal = chooser.showDialog(myFrame, "Set working folder");
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         ArtisynthPath.setWorkingDir(chooser.getSelectedFile());
         myFrame.updateWorkingDirDisplay();
      }
   }

   private void doOpenMatlab() {
      if (myMain.openMatlabConnection() == null) {
         GuiUtils.showError (myFrame, "Unable to open connection");
      }
   }

   private void doCloseMatlab() {
      myMain.closeMatlabConnection();
   }

   private void doVerifyLibraries() {
      UpdateLibrariesAgent agent = new UpdateLibrariesAgent();
      agent.createAndShowPanel (myFrame);
   }

   /**
    * this function opens up a dialog that allows the adding of probes
    */
   private void addProbe(boolean isInput) {
      NumericProbeEditor dialog;
      if (isInput) {
         dialog = new InputNumericProbeEditor();
      }
      else {
         dialog = new OutputNumericProbeEditor();
      }
      dialog.setVisible(true);
      java.awt.Point loc = myMain.getFrame().getLocation();
      dialog.setLocation(loc.x + myMain.getFrame().getWidth() / 2,
         myMain.getFrame().getHeight() / 2);
      myMain.getTimeline().addProbeEditor(dialog);
   }

   private File selectFile (String approveMsg, File existingFile) {
      File dir = ArtisynthPath.getWorkingDir(); // is always non-null

      JFileChooser chooser = new JFileChooser(dir);
      if (existingFile != null) {
         chooser.setSelectedFile (existingFile);
      }
      else {
         chooser.setCurrentDirectory(myMain.getModelDirectory());
      }
      GenericFileFilter filter = new GenericFileFilter (
         "art", "ArtiSynth model files (*.art)");
      chooser.addChoosableFileFilter (filter);
      if (existingFile == null || filter.fileExtensionMatches (existingFile)) {
         chooser.setFileFilter (filter);
      }
      int retval = chooser.showDialog(myFrame, approveMsg);
      return ((retval == JFileChooser.APPROVE_OPTION) ?
         simplifyFile(chooser.getSelectedFile()) : null);
   }

   private class WaitForRootModelManagerUpdate extends SwingWorker<Void,Void> {
      
      Runnable myRunnable;
      ProgressFrame myProgressFrame;
      Thread myUpdateThread;

      WaitForRootModelManagerUpdate (Runnable r) {
         myRunnable = r;
         myUpdateThread = myMain.getRootModelUpdateThread();
         if (myUpdateThread != null && myUpdateThread.isAlive()) {
            myProgressFrame =
               new ProgressFrame ("Waiting for RootModel manager to update ...");
            myProgressFrame.setVisible(true);
            GuiUtils.locateCenter (myProgressFrame, myFrame);
            execute();
         }
         else {
            myRunnable.run();
         }
      }
      
      @Override
      public Void doInBackground() {
         while (myUpdateThread.isAlive()) {
            try {
               myUpdateThread.join();
            }
            catch (Exception e) {
            }
         }
         return null;
      }

      @Override
      protected void done() {
         myProgressFrame.dispose();
         myRunnable.run();
      }
   };

   private ModelScriptInfo selectClass (ModelScriptInfo lastSelectedModel) {

      RootModelManager rmm = myMain.myRootModelManager;

      if (myLoadModelDialog == null) {
         myLoadModelDialog =
            new LoadModelDialog (
               myFrame, lastSelectedModel, "Select root model", rmm);
      }
      LoadModelDialog dialog = myLoadModelDialog;
      GuiUtils.locateCenter (dialog, myFrame);
      dialog.setVisible (true);
      if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
         String className = dialog.getClassName();
         String argsStr = dialog.getBuildArgs();
         String[] args = null;
         if (argsStr != null && !argsStr.equals("")) {
            args = ModelScriptInfo.splitArgs (argsStr);
         }
         rmm.saveCacheIfModified();
         return new ModelScriptInfo (
            ModelScriptInfo.InfoType.CLASS,
            className, RootModelManager.getLeafName(className), args);
      }
      return null;
   }

   private ModelScriptInfo selectScript (ModelScriptInfo lastSelectedScript) {

      if (myRunScriptDialog == null) {
         myRunScriptDialog =
            new RunScriptDialog (
               myFrame, lastSelectedScript, "Select script");
      }
      RunScriptDialog dialog = myRunScriptDialog;
      GuiUtils.locateCenter (dialog, myFrame);
      dialog.setVisible (true);
      if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
         ScriptDesc si = dialog.getScript();
         String argsStr = dialog.getArgs();
         String[] args = null;
         if (argsStr != null && !argsStr.equals("")) {
            args = ModelScriptInfo.splitArgs (argsStr);
         }
         return new ModelScriptInfo (
            ModelScriptInfo.InfoType.SCRIPT,
            si.getFile().getAbsolutePath(), si.getName(), args);
      }
      return null;
   }

   public void valueChange(ValueChangeEvent e) {
      if (e.getSource() == myStepDisplay) {
         myMain.setMaxStep(myStepDisplay.getDoubleValue());
      }
      else if (e.getSource() instanceof MouseSettingsDialog) {
         MouseSettingsDialog dialog = (MouseSettingsDialog)e.getSource();
         myMain.setMouseBindings (dialog.getBindings());
         myMain.setMouseWheelZoomScale (dialog.getWheelZoom());
      }
   }

   void updateRealTimeWidgets() {
      boolean realTimeAdvance = myMain.getRealTimeAdvance();
      setButtonPressed (realtimeButton, realTimeAdvance);
      if (realTimeAdvance) {
         realtimeButton.setActionCommand ("Disable real-time");
      }
      else {
         realtimeButton.setActionCommand ("Enable real-time");
      } 
      if (myInteractionSettingsDialog != null) {
         myInteractionSettingsDialog.updateWidgetValues();
      }
   }
   
   private void setRealTimeAdvance (boolean enable) {
      if (myMain.getScheduler().getRealTimeAdvance() != enable) {
         myMain.getScheduler().setRealTimeAdvance (enable);
         updateRealTimeWidgets();
      }
   }

   private SettingsDialog openSettingsDialog (
      String name, SettingsBase settings) {

      SettingsDialog dialog = settings.getDialog();
      if (dialog == null) {
         dialog = settings.createDialog (name, myMain.myPreferencesManager);
         GuiUtils.locateCenter (dialog, myFrame);
      }
      else {
         dialog.reloadSettings();
      }
      dialog.setVisible(true);
      return dialog;
   }

   private void openMouseSettingsDialog() {
      if (myMouseSettingsDialog == null) {
         MouseSettingsDialog dialog =
            new MouseSettingsDialog ("Mouse settings", myMain);
         GuiUtils.locateCenter(dialog, myFrame);
         myMouseSettingsDialog = dialog;
      }
      myMouseSettingsDialog.setVisible(true);
   }

   void maybeUpdateMouseSettingsDialog() {
      if (myMouseSettingsDialog != null) {
         myMouseSettingsDialog.updateWidgetValues();
      }
   }

   private void showPullControllerPropertyDialog() {
      if (myPullControllerPropertyDialog == null) {
         PullController pc = myMain.getPullController();
         PropertyPanel panel = new PropertyPanel();
         PropertyDialog dialog =
         new PropertyDialog("PullController properties", panel, "OK");
         dialog.addWidget(pc, "stiffness");
         dialog.locateRight(myMain.getFrame());
         //dialog.setSynchronizeObject(myMain.getRootModel());
         dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
               myPullControllerPropertyDialog = null;
            }
         });
         myMain.registerWindow(dialog);
         myPullControllerPropertyDialog = dialog;
         dialog.pack();
         dialog.setVisible(true);
      }
   }

   private void showPullControllerRenderPropsDialog() {
      if (myPullControllerRenderPropsDialog == null) {
         PullController pc = myMain.getPullController();
         LinkedList<HasProperties> list = new LinkedList<HasProperties>();
         list.add(pc);
         RenderPropsDialog dialog =
         new RenderPropsDialog("Edit render properties", list);
         dialog.locateRight(myMain.getFrame());
         //dialog.setSynchronizeObject(myMain.getRootModel());
         dialog.setTitle("RenderProps for PullController");
         dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
               myPullControllerRenderPropsDialog = null;
            }
         });
         myMain.registerWindow(dialog);
         myPullControllerRenderPropsDialog = dialog;
         dialog.pack();
         dialog.setVisible(true);
      }
   }

   private void doLoadModelSafely(ModelScriptInfo mi) {
//      // Collect as much possible space before loading another model
//      if (myMain.getScheduler().isPlaying()) {
//         myMain.getScheduler().stopRequest();
//         myMain.getScheduler().waitForPlayingToStop();
//      }

//      System.gc();
//      System.runFinalization();

      // load the model with name cmd
      if (!myMain.loadModel(mi)) {
         GuiUtils.showError (myFrame, myMain.getErrorMessage());
      }
   }


   /**
    * action performed to process all the menu and button actions in this class
    */
   public void actionPerformed (ModelScriptActionEvent event) {
      String cmd = event.getCommand();
      ModelScriptInfo mi = event.getModelInfo();

      if ("loadModel".equals(cmd)) {
         doLoadModelSafely(mi);
      }
      else if ("runScript".equals(cmd)) {
         myMain.runScript (mi);
      }
      else if ("Run script ...".equals(cmd)) {
         doRunScript();
      }
      else if ("Reload model".equals(cmd)) {
         doReloadModel();
      }
      else if ("Load from class ...".equals(cmd)) {
         new WaitForRootModelManagerUpdate (
            new Runnable() {
               public void run() {
                  doLoadFromClass();
               }
            });
      }
      else if ("Edit MODEL menu".equals(cmd)) {
         new WaitForRootModelManagerUpdate (
            new Runnable() {
               public void run() {
                  showModelMenuEditor();
               }
            });
      }
      else if ("Edit SCRIPT menu".equals(cmd)) {
         showScriptMenuEditor();
      }
   }

   private void showModelMenuEditor() {
      if (myModelMenuEditor == null) {
         myModelMenuEditor =
            new ModelScriptMenuEditor (
               myModelMenu, myMain.getUndoManager(), myModelMenuFile);
      }
      myModelMenuEditor.setLocationRelativeTo (myFrame);
      myModelMenuEditor.setVisible (true);
   }

   private void showScriptMenuEditor() {
      if (myScriptMenuEditor == null) {
         myScriptMenuEditor =
            new ModelScriptMenuEditor (
               myScriptMenu, myMain.getUndoManager(), myScriptMenuFile);
      }
      myScriptMenuEditor.setLocationRelativeTo (myFrame);
      myScriptMenuEditor.setVisible (true);
   }

   private void showStartupModelEditor() {
      if (myStartupModelEditor == null) {
         myStartupModelEditor =
            new StartupModelEditor (myMain.myStartupModel, myMain);
      }
      else {
         myStartupModelEditor.reloadValues();
      }
      myStartupModelEditor.setLocationRelativeTo (myFrame);
      myStartupModelEditor.setVisible (true);
   }

   private class NullOutputStream extends OutputStream {
      public void write(int b) {
         //DO NOTHING
      }
   }

   private void openBrowserDocumentation (String filePath) {
      String uriName = "https://www.artisynth.org/doc/" + filePath;
      PrintStream savedOut = System.out;
      PrintStream savedErr = System.err;
      try {
         // redirect output from the browser
         System.setOut (new PrintStream(new NullOutputStream()));
         System.setErr (new PrintStream(new NullOutputStream()));
         Desktop.getDesktop().browse (new URI(uriName));
      }
      catch (Exception e) {
         GuiUtils.showError (myFrame, "Error opening "+uriName+": "+e);
      }
      finally {
         System.setOut (savedOut);
         System.setErr (savedErr);
      }
   }

   /**
    * action performed to process all the menu and button actions in this class
    */
   public void actionPerformed(ActionEvent event) {
      String cmd = event.getActionCommand();
      RootModel root = myMain.getRootModel();

//      //
//      // Scripts menu
//      //
//      if (cmd.equals("load script from file")) {
//         JFileChooser fileChooser = new JFileChooser();
//         fileChooser.setCurrentDirectory(ArtisynthPath.getWorkingDir());
//
//         FileFilter jythonFilter = new GenericFileFilter (
//            new String[]{"py", "jy"}, "Jython files") ;
//         fileChooser.addChoosableFileFilter(jythonFilter);
//         fileChooser.setFileFilter(jythonFilter);
//         int result = fileChooser.showOpenDialog(myFrame);
//         if (result == JFileChooser.APPROVE_OPTION) {
//            File selectedFile = getSelectedFile(fileChooser);
//            runScript(selectedFile.getAbsolutePath());
//         }
//      }
      //
      // File Menu
      //
      if (cmd.equals("Clear model")) {
         doClearModel();
      }
      else if (cmd.equals("New blank MechModel")) {
         doBlankMechmodel();
      }
      else if (cmd.equals("Save model")) {
         doSaveModel();
      }
      else if (cmd.equals("Save model as ...")) {
         doSaveModelAs();
      }
      // else if (cmd.equals("Reload model")) {
      //    doReloadModel();
      // }
      else if (cmd.equals("Load model ...")) {
         doLoadModel();
      }
      // else if (cmd.equals("Load from class ...")) {
      //    new WaitForRootModelManagerUpdate (
      //       new Runnable() {
      //          public void run() {
      //             doLoadFromClass();
      //          }
      //       });
      // }
      // else if (cmd.equals("Run script ...")) {
      //    doRunScript();
      // }
      else if (cmd.equals("Set working folder ...")) {
         doSetWorkingDirectory();
      }
      else if (cmd.equals("Open MATLAB connection")) {
         doOpenMatlab();
      }
      else if (cmd.equals("Close MATLAB connection")) {
         doCloseMatlab();
      }
      else if (cmd.equals("Update libraries")) {
         doVerifyLibraries ();
      }
      else if (cmd.equals("Load probes ...")) {
         ProbeEditor.loadProbes (myMain, myFrame);
      }
      else if (cmd.equals("Save probes")) {
         ProbeEditor.saveProbes (myMain, myFrame);
      }
      else if (cmd.equals("Save probes as ...")) {
         ProbeEditor.saveProbesAs (myMain, myFrame);
      }
      else if (cmd.equals("Save output probe data")) {
         doSaveOutputProbeData();
      }
      else if (cmd.equals("Load probes from ...")) {
         newLoadProbesFrom();
      }
      else if (cmd.equals("Save probes new")) {
         newSaveProbes();
      }
      else if (cmd.equals("Save probes in ...")) {
         newSaveProbesIn();
      }
      else if (cmd.equals("Save waypoints")) {
         doSaveWayPoints();
      }
      else if (cmd.equals("Save waypoints as ...")) {
         doSaveWayPointsAs();
      } 
      else if (cmd.equals("Load waypoints ...")) {
         doLoadWayPoints();
      }
      else if (cmd.equals("Reload waypoints")) {
         doReloadWayPoints();
      }
      else if (cmd.equals("Delete waypoints")) {
         doDeleteWayPoints();
      }
      else if (cmd.equals("Save viewer image ...")) {
         saveViewerImage();
      }
      else if (cmd.equals("Quit")) {
         myMain.quit();
      }
      //
      // Edit menu
      //
      else if (cmd.equals("Add input probe")) {
         doAddInputProbe();
      }
      else if (cmd.equals("Add output probe")) {
         doAddOutputProbe();
      }
      else if (cmd.equals("Add control panel")) {
         doAddControlPanel();
      }
      else if (cmd.equals("Load control panel")) {
         doLoadControlPanel();
      }
      else if (cmd.equals("Print selection")) {
         SelectionManager sm = myMain.getSelectionManager();
         for (ModelComponent c : sm.getCurrentSelection()) {
            System.out.println(ComponentUtils.getPathName(c));
         }
      }
      else if (cmd.equals("Undo")) {
         doUndoCommand();
      }      
      else if (cmd.equals("External classpath ...")) {
         if (myExtClassPathEditor == null) {
            File file = ArtisynthPath.getConfigFile ("EXTCLASSPATH");
            try {
               myExtClassPathEditor =
                  new ExtClassPathEditor (myMain.getUndoManager(), file);
            }
            catch (Exception e) {
               GuiUtils.showError (myFrame, "Could not open "+file);
            }
         }
         if (myExtClassPathEditor != null) {
            myExtClassPathEditor.setLocationRelativeTo (myFrame);
            myExtClassPathEditor.open();
         }
      }
      // else if (cmd.equals("Script path ...")) {
      //    if (myScriptPathEditor == null) {
      //       File file = ArtisynthPath.getConfigFile ("SCRIPTPATH");
      //       try {
      //          myScriptPathEditor =
      //             new ScriptPathEditor (myMain.getUndoManager(), this, file);
      //       }
      //       catch (Exception e) {
      //          GuiUtils.showError (myFrame, "Could not open "+file);
      //       }
      //    }
      //    if (myScriptPathEditor != null) {
      //       myScriptPathEditor.setLocationRelativeTo (myFrame);
      //       myScriptPathEditor.open();
      //    }
      // }
      else if (cmd.equals("Startup model ...")) {
         new WaitForRootModelManagerUpdate (
            new Runnable() {
               public void run() {
                  showStartupModelEditor();
               }
            });
      }
      else if (cmd.equals ("Preferences ...")) {
         if (myPreferencesEditor == null) {
            myPreferencesEditor = 
               new PreferencesEditor (myMain.getPreferencesManager());
            myPreferencesEditor.build();
         }
         else {
            myPreferencesEditor.reloadValues();
         }
         myPreferencesEditor.setLocationRelativeTo (myFrame);
         myPreferencesEditor.setVisible (true);
      }
      
      //
      // Settings menu
      //
      else if (cmd.equals("Viewers ...")) {
         myViewerSettingsDialog =
            openSettingsDialog (
               "Viewer settings", myMain.myViewerManager);
      }
      else if (cmd.equals("Simulation ...")) {
         mySimulationSettingsDialog =
            openSettingsDialog (
               "Simulation settings", myMain.mySimulationSettings);
      }
      else if (cmd.equals("Interaction ...")) {
         myInteractionSettingsDialog =
            openSettingsDialog (
               "Interaction settings", myMain.myInteractionSettings);
      }
      else if (cmd.equals("Enable selection highlighting")) {
         setSelectionColorEnabled(true);
      }
      else if (cmd.equals("Disable selection highlighting")) {
         setSelectionColorEnabled(false);
      }
      else if (cmd.equals("Mouse ...")) {
         openMouseSettingsDialog();
      }
      else if (cmd.equals("PullController properties ...")) {
         showPullControllerPropertyDialog();
      }
      else if (cmd.equals("PullController render props ...")) {
         showPullControllerRenderPropsDialog();
      }
      //
      // View menu
      //
      else if (cmd.equals("Center view on selection")) {
         myMain.centerViewOnSelection();
      }
      else if (cmd.equals("Hide timeline")) {
         myMain.setTimelineVisible(false);
      }
      else if (cmd.equals("Show timeline")) {
         myMain.setTimelineVisible(true);
      }
      else if (cmd.equals("Reset view")) {
         Viewer v = myMain.getViewer();
         v.setAxialView(v.getAxialView());
         v.autoFit();
      }
      else if (cmd.equals("Perspective view")) {
         myMain.getViewer().setOrthographicView(false);
      }
      else if (cmd.equals("Orthographic view")) {
         myMain.getViewer().setOrthographicView(true);
      }
      else if (cmd.equals("Hide Jython console")) {
         myMain.setJythonFrameVisible(false);
      }
      else if (cmd.equals("Show Jython console")) {
         myMain.setJythonFrameVisible(true);
      }
      else if (cmd.equals("Show Inverse panel")) {
         ControlPanel panel = InverseManager.findInversePanel(root);
         TrackingController controller = 
            InverseManager.findInverseController(root);
         if (panel == null && controller != null) {
            InverseManager.addInversePanel (root, controller);
         }
      }
      else if (cmd.equals("Hide Inverse panel")) {
         ControlPanel panel = InverseManager.findInversePanel(root);
         if (panel != null) {
            root.removeControlPanel(panel);
         }
         //myMain.getInverseManager().hideInversePanel();
      }
      else if (cmd.equals("Show movie panel")) {
         myFrame.getMain().getViewer().getCanvas().display();
         myMain.getMovieMaker().showDialog(myFrame);
      }
      else if (cmd.equals("Hide movie panel")) {
         // TODO: this isn't implemented yet because we need to set
         // this up as an action
         myMain.getMovieMaker().closeDialog();
      }
      else if (cmd.equals ("Show empty components in navpanel")) {
         myFrame.getNavPanel().setHideEmptyComponents (false);
      }
      else if (cmd.equals ("Hide empty components in navpanel")) {
         myFrame.getNavPanel().setHideEmptyComponents (true);
      }
      else if (cmd.equals("New viewer")) {
         myMain.createViewerFrame();
      }
      else if (cmd.equals("Clear traces")) {
         root.clearTraces();
      }
      else if (cmd.equals("Disable all tracing")) {
         root.disableAllTracing();
      }
      else if (cmd.equals("Remove traces")) {
         RemoveComponentsCommand rmCmd =
         new RemoveComponentsCommand(
            "remove traces", root.getTracingProbes());
         myMain.getUndoManager().saveStateAndExecute(rmCmd);
         myMain.rerender();
      }
      else if (cmd.equals("Set traces visible")) {
         root.setTracingProbesVisible(true);
         myMain.rerender();
      }
      else if (cmd.equals("Set traces invisible")) {
         root.setTracingProbesVisible(false);
         myMain.rerender();
      }
      else if (cmd.equals("Hide viewer toolbar")) {
         isToolbarVisible = false;
         detachViewerToolbar(toolbarPanel);
      }
      else if (cmd.equals("Show viewer toolbar")) {
         isToolbarVisible = true;
         attachViewerToolbar(toolbarPanel);
      }
      else if (cmd.equals("Merge control panels")) {
         root.mergeAllControlPanels(true);
      }
      else if (cmd.equals("Separate control panels")) {
         root.mergeAllControlPanels(false);
      }
      //
      // Help menu
      //
      else if (cmd.equals("About ArtiSynth")) {
         if (myArtisynthInfoFrame == null) {
            myArtisynthInfoFrame = myFrame.createArtisynthInfo();
            GuiUtils.locateCenter (myArtisynthInfoFrame, myFrame);
         }
         myArtisynthInfoFrame.setVisible (true);
      }
      else if (cmd.equals ("User interface guide ...")) {
         openBrowserDocumentation ("pdf/uiguide.pdf");
      }
      else if (cmd.equals ("Modeling guide ...")) {
         openBrowserDocumentation ("pdf/modelguide.pdf");
      }
      else if (cmd.equals ("ArtiSynth Java API ...")) {
         openBrowserDocumentation ("javadocs/index.html");
      }
      else if (cmd.equals ("MATLAB interface ...")) {
         openBrowserDocumentation ("pdf/matlab.pdf");
      }
      else if (cmd.equals("About the current model")) {
         if (myModelInfoFrame == null) {
            myModelInfoFrame = myFrame.createModelInfo (myMain.getRootModel());
            GuiUtils.locateCenter (myModelInfoFrame, myFrame);
         }
         myModelInfoFrame.setVisible (true);
      }
      else if (cmd.equals("Viewer key bindings")) {
         if (myKeyBindingInfoFrame == null) {
            myKeyBindingInfoFrame = myFrame.createKeyBindingInfo();
            GuiUtils.locateCenter (myKeyBindingInfoFrame, myFrame);
         }
         myKeyBindingInfoFrame.setVisible (true);
      }
      //
      // Tool bar buttons
      //
      else if (cmd.equals("Hide NavPanel")) {
         myFrame.getNavPanel().setExpanded(!myFrame.getNavPanel().isExpanded());
         myFrame.refreshSplitPane();
         navBarButton.setToolTipText("Show navigation panel");
         navBarButton.setActionCommand("Show NavPanel");
      }
      else if (cmd.equals("Show NavPanel")) {
         myFrame.getNavPanel().setExpanded(!myFrame.getNavPanel().isExpanded());
         myFrame.refreshSplitPane();
         navBarButton.setToolTipText("Hide navigation panel");
         navBarButton.setActionCommand("Hide NavPanel");
      }
      else if (cmd.equals("Rerender")) {
         myMain.rerender();
      }
      else if (cmd.equals("Reset")) {
         myMain.getScheduler().reset();
      }
      else if (cmd.equals("Skip back")) {
         myMain.getScheduler().rewind();
      }
      else if (cmd.equals("Play")) {
         myMain.getScheduler().play();
      }
      else if (cmd.equals("Pause")) {
         myMain.getScheduler().pause();
      }
      else if (cmd.equals("Single step")) {
         myMain.getScheduler().step();
      }
      else if (cmd.equals("Skip forward")) {
         myMain.getScheduler().fastForward();
      }
      else if (cmd.equals("Stop all")) {
         myMain.stopAll();
      }
      else if (cmd.equals("Reset initial state")) {
         if (root != null) {
            root.resetInitialState();
         }
      }
      else if (cmd.equals("Disable real-time")) {
         setRealTimeAdvance (false);
      }
      else if (cmd.equals("Enable real-time")) {
         setRealTimeAdvance (true);
      }
      else if (cmd.equals("cancel")) {
         return;
      }
      else {
         throw new InternalErrorException("Unimplemented command: " + cmd);
      }
   }

   void clearModelInfoFrame() {
      if (myModelInfoFrame != null) {
         myModelInfoFrame.setVisible (false);
         myModelInfoFrame.dispose();
      }
   }

   private static String protectWindowsSlashes(String filename) {
      StringBuilder out = new StringBuilder();
      char[] chars = filename.toCharArray();

      for (int idx=0; idx <chars.length; idx++) {
         if (chars[idx] == '\\' ) {
            out.append("\\\\");  // double it up
            if (idx+1 < chars.length && chars[idx+1] == '\\') {
               // skip next char if was already doubled
               idx++;
            }
         } else  {
            out.append(chars[idx]);
         }
      }
      return out.toString();
   }

   public void enableShowPlay() {
      playButton.setIcon(GuiStorage.getPlayIcon());
      playButton.setToolTipText("Start simulation");
      playButton.setActionCommand("Play");
      playButton.setEnabled(true);
      singleStepButton.setEnabled(true);
   }

   public void disableShowPlay() {
      playButton.setIcon(GuiStorage.getPauseIcon());
      playButton.setToolTipText("Pause simulation");
      playButton.setActionCommand("Pause");
      singleStepButton.setEnabled(false);
   }
   
   protected void updateForwardButton () {
      boolean enabled = false;
      RootModel root = myMain.getRootModel();
      if (root != null) {
         enabled = 
            (root.getWayPoints().getValidAfter (myMain.getTime()) != null);
      }
      forwardButton.setEnabled (enabled);     
   }

   public void detachToolbar() {
      toolbarPanel.setVisible(false);

      if (!myFrame.getNavPanel().isExpanded())
         myFrame.getNavPanel().setVisible(false);

      myFrame.refreshSplitPane();
   }

   public void attachToolbar() {
      if (toolbarPanel == null) {
         toolbarPanel = new JPanel(new BorderLayout());
      }
      attachModeSelectionToolbar(toolbarPanel);
      attachViewerToolbar(toolbarPanel);

      myFrame.add(toolbarPanel, BorderLayout.WEST);
      toolbarPanel.setVisible(true);
   }

   private void createModeSelectionToolbar() {
      modeSelectionToolbar = new SelectionToolbar(myMain);
   }

   public void attachModeSelectionToolbar(JPanel panel) {
      if (modeSelectionToolbar == null) {
         createModeSelectionToolbar();
      }
      modeSelectionToolbar.setOrientation(JToolBar.VERTICAL);
      panel.add(modeSelectionToolbar, BorderLayout.NORTH);
   }

   /**
    * author: andreio create a camera toolbar to control the view of the camera
    * on the GlViewer
    */

   public void attachViewerToolbar(JPanel panel) {
      if (myViewerToolBar == null) {
         myViewerToolBar = 
         new ViewerToolBar (getMainViewer(), /*allowGridDisplay=*/false);
      }
      myViewerToolBar.setOrientation(JToolBar.VERTICAL);
      panel.add(myViewerToolBar, BorderLayout.SOUTH);
      panel.revalidate();
      panel.repaint();
      myViewerToolBar.setVisible(true);
   }

   public void clearClipPlaneControls() {
      myViewerToolBar.clearClipPlanes ();
   }

   public void detachViewerToolbar(JPanel panel) {
      if (myViewerToolBar != null) {
         panel.remove(myViewerToolBar);
         panel.revalidate();
         panel.repaint();
         myViewerToolBar.setVisible(false);
      }
   }

   public Viewer getMainViewer() {
      return myMain.getViewer();
   }

   public void setSelectionColorEnabled (boolean enable) {
      ViewerManager vm = myMain.getViewerManager();
      if (enable) {
         vm.setSelectionHighlightStyle (HighlightStyle.COLOR);
      }
      else {
         vm.setSelectionHighlightStyle (HighlightStyle.NONE);
      }
   }

   public void updateWidgets() {
      // return if frame is not visible, since updating widgets while
      // frame is being set visible can cause some problems
      if (!myFrame.isVisible()) {
         return;
      }
      Viewer v = getMainViewer();
      boolean gridOn = v.getGridVisible();
      GridPlane grid = v.getGrid();
      if ((myGridDisplay != null) != gridOn) {
         if (gridOn) {
            myGridDisplay =
            GridDisplay.createAndAdd (grid, myToolBar, myGridDisplayIndex);
         }
         else {
            GridDisplay.removeAndDispose (
               myGridDisplay, myToolBar, myGridDisplayIndex);
            myGridDisplay = null;
         }
      }
      if (myGridDisplay != null) {
         myGridDisplay.updateWidgets();
      }
      updateForwardButton();
   }

   private JMenuItem addMenuItem(JMenu menu, String label, String cmd) {
      JMenuItem item = makeMenuItem(label, cmd);
      menu.add(item);
      return item;
   }

   private JMenuItem addMenuItem (
      JMenu menu, String label, String cmd, String tip) {
      JMenuItem item = makeMenuItem(label, cmd, tip);
      menu.add(item);
      return item;
   }

   private JMenuItem addMenuItem(JMenu menu, String labelAndCmd) {
      return addMenuItem(menu, labelAndCmd, labelAndCmd);
   }

   private void updateRecentMenu (
      JMenu menu, String cmd, InfoType... types) {

      ModelScriptHistory hist = myMain.getModelScriptHistory();
      List<ModelScriptInfo> mhi = hist.getRecent (10, types);

      menu.removeAll();                  
      if (mhi.size() > 0) {
         menu.setEnabled (true);
         for (int i=0; i<mhi.size(); i++) {
            ModelScriptInfo mi = mhi.get(i);
            JMenuItem item = new JMenuItem (mi.getShortName());
            item.addActionListener (
               new ModelScriptActionForwarder (this, cmd, mi));
            item.setToolTipText (mi.getClassNameOrFile());
            menu.add (item);
         }
      }
      else {
         menu.setEnabled (false);
      }
   }

   private void createLoadRecentMenu (JMenu menu) {

      ModelScriptHistory hist = myMain.getModelScriptHistory();
      List<ModelScriptInfo> mhi = hist.getRecent (
         10, InfoType.CLASS, InfoType.FILE);
                  
      if (mhi.size() > 0) {
         JMenu recent = new JMenu ("Load recent");
         menu.add (recent);
         for (int i=0; i<mhi.size(); i++) {
            ModelScriptInfo mi = mhi.get(i);
      
            JMenuItem item = new JMenuItem (mi.getShortName());
            item.addActionListener (
               new ModelScriptActionForwarder (this, "loadModel", mi));
            item.setToolTipText (mi.getClassNameOrFile());
            recent.add (item);
         }
      }
   }

   private void createFileMenu(JMenu menu) {
      boolean hasRootModel = (myMain.getRootModel() != null);

      JMenuItem item;

      addMenuItem(menu, "New blank MechModel");
      menu.add(new JSeparator());

      item = addMenuItem(menu, "Save model");
      item.setEnabled(myMain.getModelFile() != null);

      item = addMenuItem(menu, "Save model as ...");
      item.setEnabled(hasRootModel);
      //menu.add(new JSeparator());

      addMenuItem(menu, "Load model ...");
      // addMenuItem(menu, "Load from class ...");

      // item = addMenuItem(menu, "Reload model");
      // item.setEnabled(myMain.modelIsLoaded());

      //createLoadRecentMenu (menu);

      JMenuItem loadProbesItem, saveProbesItem, saveProbesAsItem;

      // menu.add(new JSeparator());
      // addMenuItem(menu, "Run script ...");
      menu.add(new JSeparator());


      if (Probe.useOldSaveMethod) {
         loadProbesItem = addMenuItem(menu, "Load probes ...");
         saveProbesItem = addMenuItem(menu, "Save probes");
         saveProbesAsItem = addMenuItem(menu, "Save probes as ...");
      }
      else {
         loadProbesItem = addMenuItem(menu, "Load probes from ...");
         saveProbesItem = addMenuItem(menu, "Save probes", "Save probes new");
         saveProbesAsItem = addMenuItem(menu, "Save probes in ...");
      }
      loadProbesItem.setEnabled(hasRootModel);
      saveProbesItem.setEnabled(
         hasRootModel && myMain.getProbesFile() != null);
      saveProbesAsItem.setEnabled(hasRootModel);

      JMenuItem saveProbeDataItem =
         addMenuItem(menu, "Save output probe data");
      saveProbeDataItem.setEnabled(hasRootModel);


      menu.add(new JSeparator());

      JMenuItem saveWayPointsItem = addMenuItem(menu, "Save waypoints");
      JMenuItem saveWayPointsAsItem = addMenuItem(menu, "Save waypoints as ...");
      JMenuItem loadWayPointsItem = addMenuItem(menu, "Load waypoints ...");
      JMenuItem reloadWayPointsItem = addMenuItem(menu, "Reload waypoints");
      JMenuItem deleteWayPointsItem = addMenuItem(menu, "Delete waypoints");

      boolean hasWayPointFile = (myMain.getWayPointsFile() != null);

      saveWayPointsItem.setEnabled(hasRootModel && hasWayPointFile);
      saveWayPointsAsItem.setEnabled(hasRootModel);      
      loadWayPointsItem.setEnabled(hasRootModel);
      reloadWayPointsItem.setEnabled(hasRootModel && hasWayPointFile);
      deleteWayPointsItem.setEnabled(hasRootModel);         

      menu.add(new JSeparator());
      addMenuItem(menu, "Save viewer image ...");
      addMenuItem(menu, "Set working folder ...");

      if (myMain.hasMatlabConnection()) {
         addMenuItem(menu, "Close MATLAB connection");
      }
      else {
         addMenuItem(menu, "Open MATLAB connection");
      }
      menu.add(new JSeparator());
      addMenuItem (menu, "Update libraries");
      menu.add(new JSeparator());

      addMenuItem(menu, "Quit");
   }

   private void createEditMenu(JMenu menu) {
      boolean hasRootModel = (myMain.getRootModel() != null);

      JMenuItem item;

      item = addMenuItem(menu, "Add input probe");
      item.setEnabled(hasRootModel);

      item = addMenuItem(menu, "Add output probe");
      item.setEnabled(hasRootModel);

      item = addMenuItem(menu, "Add control panel");
      item.setEnabled(hasRootModel);

      item = addMenuItem(menu, "Load control panel");
      item.setEnabled(hasRootModel);

      addMenuItem(menu, "Print selection");
      
      menu.add (new JSeparator());
      JMenuItem undoItem = makeMenuItem("Undo", "Undo");
      Command cmd = myMain.getUndoManager().getLastCommand();
      if (cmd != null && hasRootModel) {
         undoItem.setEnabled(true);
         String text = "Undo";
         if (cmd.getName() != null) {
            text += " " + cmd.getName();
         }
         undoItem.setText(text);
      }
      else {
         undoItem.setEnabled(false);
         undoItem.setText("Undo");
      }
      menu.add(undoItem);
   }

   private boolean isWindowOpen (Window win) {
      return win != null && win.isVisible();
   }
   private void createSettingsMenu(JMenu menu) {

      JMenuItem item;

      item = addMenuItem(menu, "Viewers ...");
      item.setEnabled (!isWindowOpen (myViewerSettingsDialog));

      item = addMenuItem(menu, "Simulation ...");
      item.setEnabled (!isWindowOpen (mySimulationSettingsDialog));

      item = addMenuItem(menu, "Interaction ...");
      item.setEnabled (!isWindowOpen (myInteractionSettingsDialog));

      addMenuItem(menu, "Mouse ...");

      item = addMenuItem (
         menu, "External classpath ...", null,
         "Edit the classpath which allows access to external projects");
      item.setEnabled (!isWindowOpen (myExtClassPathEditor));
         
      // item = addMenuItem (
      //    menu, "Script path ...", null,
      //    "Edit the folder path used for finding scripts");
      // item.setEnabled (!isWindowOpen (myScriptPathEditor));
         
      item = addMenuItem (
         menu, "Startup model ...", null,
         "Edit the model (if any) which is loaded at startup");
      item.setEnabled (!isWindowOpen (myStartupModelEditor));

      JMenu submenu = new JMenu("PullController");
      menu.add(submenu);

      item = addMenuItem(
         submenu, "properties ...", "PullController properties ...");
      item.setEnabled(myPullControllerPropertyDialog == null);

      item = addMenuItem(
         submenu, "render props ...", "PullController render props ...");
      item.setEnabled(myPullControllerRenderPropsDialog == null);

      ViewerManager vm = myMain.getViewerManager();
      if (vm.getSelectionHighlightStyle() == HighlightStyle.COLOR) {
         addMenuItem(menu, "Disable selection highlighting");
      }
      else {
         addMenuItem(menu, "Enable selection highlighting");
      }

      menu.add (new JSeparator());
         
      item = addMenuItem (
         menu, "Preferences ...", null,
         "Edit default application settings");
      item.setEnabled (!isWindowOpen (myPreferencesEditor));

   }

   private void createViewMenu(JMenu menu) {
      JMenuItem item;

      RootModel root = myMain.getRootModel();
      boolean hasTraceables = root.getNumTraceables() > 0;

      boolean hasTraces = false;
      boolean hasVisibleTrace = false;
      boolean hasInvisibleTrace = false;
      for (Probe p : root.getOutputProbes()) {
         if (p instanceof TracingProbe) {
            hasTraces = true;

            if (RenderableUtils.isVisible((TracingProbe)p)) {
               hasVisibleTrace = true;
            }
            else {
               hasInvisibleTrace = true;
            }
         }
      }

      if (myMain.isTimelineVisible()) {
         addMenuItem(menu, "Hide timeline");
      }
      else {
         addMenuItem(menu, "Show timeline");
      }

      if (JythonInit.jythonIsAvailable()) {
         if (isWindowOpen (myMain.myJythonFrame)) {
            addMenuItem(menu, "Hide Jython console");
         }
         else {
            addMenuItem(menu, "Show Jython console");
         }
      }

      if (InverseManager.findInverseController(root) != null) {
         if (InverseManager.findInversePanel(root) != null) {
            addMenuItem(menu, "Hide Inverse panel");
         }
         else {
            addMenuItem(menu, "Show Inverse panel");
         }
      }

      if (isToolbarVisible) {
         addMenuItem(menu, "Hide viewer toolbar");
      }
      else {
         addMenuItem(menu, "Show viewer toolbar");
      }

      addMenuItem(menu, "Show movie panel");

      menu.add(new JSeparator());      

      addMenuItem(menu, "New viewer");

      SelectionManager sm = myMain.getSelectionManager();
      if (sm.getNumSelected() > 0) {
         addMenuItem(menu, "Center view on selection");
      }

      if (getMainViewer().isOrthogonal()) {
         addMenuItem(menu, "Perspective view");
      }
      else {
         addMenuItem(menu, "Orthographic view");
      }

      menu.add(new JSeparator());      

      addMenuItem(menu, "Merge control panels");
      addMenuItem(menu, "Separate control panels");

      if (myFrame.getNavPanel().getHideEmptyComponents()) {
         addMenuItem(menu, "Show empty components in navpanel");         
      }
      else {
         addMenuItem(menu, "Hide empty components in navpanel");         
      }

      menu.add(new JSeparator());      

      item = addMenuItem(menu, "Clear traces");
      item.setEnabled(hasTraceables);

      item = addMenuItem(menu, "Disable all tracing");
      item.setEnabled(hasTraceables);

      item = addMenuItem(menu, "Remove traces");
      item.setEnabled(hasTraces);

      item = addMenuItem(menu, "Set traces visible");
      item.setEnabled(hasInvisibleTrace);

      item = addMenuItem(menu, "Set traces invisible");
      item.setEnabled(hasVisibleTrace);

   }

   private void createHelpMenu(JMenu menu) {
      RootModel rootModel = myMain.getRootModel();

      if (Desktop.isDesktopSupported() &&
          Desktop.getDesktop().isSupported (Desktop.Action.BROWSE)) {
         addMenuItem(menu, "User interface guide ...");
         addMenuItem(menu, "Modeling guide ...");
         addMenuItem(menu, "ArtiSynth Java API ...");
         addMenuItem(menu, "MATLAB interface ...");
      }
      addMenuItem(menu, "Viewer key bindings");

      menu.add (new JSeparator());
      if (rootModel.getAbout() != null && rootModel.getAbout().length() > 0) {
         addMenuItem(menu, "About the current model");
      }
      JMenuItem item = addMenuItem(menu, "About ArtiSynth");
      item.setEnabled (!isWindowOpen (myArtisynthInfoFrame));

   }

   private boolean addMenuItems (ArrayList<Object> items, HasMenuItems comp) {
      int size0 = items.size();
      if (comp.getMenuItems (items)) {
         if (size0 < items.size()) {
            items.add (new JSeparator());
         }
         return true;
      }
      else {
         return false;
      }
   }

   ArrayList<Object> getApplicationMenuItems (RootModel root) {
      ArrayList<Object> items = new ArrayList<Object>();
      boolean hasItems = false;
      hasItems |= addMenuItems (items, root);
      for (int i=0; i<root.numComponents(); i++) {
         ModelComponent comp0 = root.get(i);
         if (comp0 instanceof HasMenuItems) {
            hasItems |= addMenuItems (items, (HasMenuItems)comp0);
         }
         if (comp0 instanceof CompositeComponent) {
            CompositeComponent ccomp = (CompositeComponent)comp0;
            for (int j=0; j<ccomp.numComponents(); j++) {
               ModelComponent comp1 = ccomp.get(j);
               if (comp1 instanceof HasMenuItems) {
                  hasItems |= addMenuItems (items, (HasMenuItems)comp1);
               }
            }
         }
      }
      return hasItems ? items : null;
   }

   private void createApplicationMenu(JMenu menu) {
      RootModel rootModel = myMain.getRootModel();

      ArrayList<Object> items = getApplicationMenuItems (rootModel);

      if (items != null) {
         for (int i=0; i<items.size(); i++) {
            Object item = items.get(i);
            if (item instanceof JMenuItem) {
               menu.add ((JMenuItem)item);
            }
            else if (item instanceof JSeparator && i == items.size()-1) {
               // ignore
            }
            else if (item instanceof Component) {
               menu.add ((Component)item);
            }
            else if (item instanceof String) {
               menu.add ((String)item);
            }
            else {
               // ignore
            }
         }
      }
   }

   public void schedulerActionPerformed(Scheduler.Action action) {
      if (myMain.getScheduler().isPlaying()) {
         disableShowPlay();
      }
      else {
         enableShowPlay();
      }
   }

   public void renderOccurred (RendererEvent e) {
      updateWidgets();
   }

}
