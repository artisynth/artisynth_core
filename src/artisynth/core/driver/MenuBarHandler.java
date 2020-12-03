/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;

import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.editorManager.Command;
import artisynth.core.gui.editorManager.ProbeEditor;
import artisynth.core.gui.editorManager.RemoveComponentsCommand;
import artisynth.core.gui.probeEditor.InputNumericProbeEditor;
import artisynth.core.gui.probeEditor.NumericProbeEditor;
import artisynth.core.gui.probeEditor.OutputNumericProbeEditor;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.gui.timeline.GuiStorage;
import artisynth.core.gui.widgets.ImageFileChooser;
import artisynth.core.inverse.InverseManager;
import artisynth.core.inverse.TrackingController;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.HasMenuItems;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelmenu.ArtisynthModelMenu;
import artisynth.core.modelmenu.ModelActionEvent;
import artisynth.core.modelmenu.ModelActionListener;
import artisynth.core.probes.Probe;
import artisynth.core.probes.TracingProbe;
import artisynth.core.probes.WayPointProbe;
import artisynth.core.util.AliasTable;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ExtensionFileFilter;
import artisynth.core.util.JythonInit;
import artisynth.core.workspace.PullController;
import artisynth.core.workspace.RootModel;
import maspack.properties.HasProperties;
import maspack.properties.PropertyUtils;
import maspack.render.RenderListener;
import maspack.render.RenderableUtils;
import maspack.render.Renderer.HighlightStyle;
import maspack.render.RendererEvent;
import maspack.render.GL.GLGridPlane;
import maspack.render.GL.GLViewer;
import maspack.solvers.PardisoSolver;
import maspack.util.ClassFinder;
import maspack.util.FunctionTimer;
import maspack.util.GenericFileFilter;
import maspack.util.InternalErrorException;
import maspack.util.StringHolder;
import maspack.widgets.AutoCompleteStringField;
import maspack.widgets.ButtonCreator;
import maspack.widgets.DoubleField;
import maspack.widgets.GridDisplay;
import maspack.widgets.GuiUtils;
import maspack.widgets.MouseSettingsDialog;
import maspack.widgets.OptionPanel;
import maspack.widgets.PropertyDialog;
import maspack.widgets.PropertyPanel;
import maspack.widgets.RenderPropsDialog;
import maspack.widgets.StringField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.widgets.ValueCheckListener;
import maspack.widgets.ViewerToolBar;
import maspack.widgets.WidgetDialog;

/**
 * to create a class that handles the main menu interactions responds to the
 * events and calls appropriate functions to deal with the events
 * 
 */

public class MenuBarHandler implements 
ActionListener, ValueChangeListener, SchedulerListener, RenderListener,
ModelActionListener {
   private Main myMain;
   private MainFrame myFrame;

   protected ArtisynthModelMenu myModelsMenuGenerator;  // generates models menu

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
   // initialized in 
   protected Color defaultButtonBackground;
   protected Border defaultButtonBorder;

   protected JMenuItem[] scriptMenuItems;

   protected JToolBar modeSelectionToolbar = null;
   protected JPanel toolbarPanel;
   protected VerticalGridLayout menuGrid;
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

   private boolean isTimelineVisible = false;
   private boolean isToolbarVisible = true;

   private RenderPropsDialog myPullControllerRenderPropsDialog;
   private PropertyDialog myPullControllerPropertyDialog;

   public MenuBarHandler (Main parent, MainFrame theFrame) {
      myMain = parent;
      myFrame = theFrame;
   }

   public void initToolbar() {
      attachToolbar();
   }

   protected JRadioButtonMenuItem makeRadioMenuItem(String name, String cmd) {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(name);
      item.addActionListener(this);
      item.setActionCommand(cmd);
      return item;
   }

   protected JMenuItem makeMenuItem(String name, String cmd) {
      JMenuItem item = new JMenuItem(name);
      item.addActionListener(this);
      item.setActionCommand(cmd);
      return item;
   }

   boolean isScriptMenuItem(Object comp) {
      for (int i = 0; i < scriptMenuItems.length; i++) {
         if (scriptMenuItems[i] == comp) {
            return true;
         }
      }
      return false;
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

      // Create the menu with models (taken from .demoModels)
      menu = new JMenu("Models");
      myMenuBar.add(menu);
      createDemosMenu(menu);

      menu = createScriptsMenu("Scripts");
      if (menu != null) {
         myMenuBar.add(menu);
      }

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

      //myFrame.setJMenuBar(myMenuBar);

      // Adding iconic buttons
      // Create a space separator
      // height makes space for GridDisplay box
      myMenuBar.add(Box.createRigidArea(new Dimension(20, 28)));
      // Create navigation bar button
      if (myFrame.getNavPanel().getStatus()) {
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
//      realtimeEnabledIcon =
//         GuiUtils.loadIcon(ControlPanel.class, "icon/checkedClock.png");
//      realtimeDisabledIcon =
//         GuiUtils.loadIcon(ControlPanel.class, "icon/uncheckedClock.png");
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
      // myToolBar.add (Box.createRigidArea (new Dimension (4, 0)));

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

      // Set the menu bar
      myFrame.setJMenuBar(myMenuBar);
      myFrame.add(myToolBar, BorderLayout.NORTH);
   }

   private JMenu createScriptsMenu(String title) {

      String[] scriptNames = myMain.getScriptNames();
      scriptMenuItems = new JMenuItem[scriptNames.length];
      JMenu menu = null;
      if (scriptNames.length != 0) {
         menu = new JMenu(title);
         VerticalGridLayout scriptGrid = new VerticalGridLayout(20, 0);
         menu.getPopupMenu().setLayout(scriptGrid);

         for (int i = 0; i < scriptNames.length; i++) {
            JMenuItem item = makeMenuItem(scriptNames[i], scriptNames[i]);
            item.setToolTipText(myMain.getScriptName(scriptNames[i]));
            menu.add(item);
            scriptMenuItems[i] = item;
         }

         // separator before load
         menu.add(new JSeparator());

      } else {
         menu = new JMenu(title); // empty
      }

      // load from file dialog
      JMenuItem loadItem = makeMenuItem("Load script...", "load script from file");
      menu.add(loadItem);

      return menu;
   }

   /**
    * Reads the demo menu stuff
    */
   public ArtisynthModelMenu readDemoMenu(String filename) {

      // if no file specified, exit
      if (filename==null || filename.equals("")) {
         return null;
      } 
      ArtisynthModelMenu modelsMenu = null;
      File file = ArtisynthPath.findFile(filename);
      if (file == null) {
         System.out.println ("Warning: demosMenuFile '" + filename + "' not found");
      }
      else {
         try {
            modelsMenu = new ArtisynthModelMenu(file);
            return modelsMenu;
         } catch (Exception e) {
            System.out.println ("Warning: error reading demosMenuFile: "
            + filename);
            System.out.println (e.getMessage());
            modelsMenu = null;
         }
      }
      return modelsMenu;
   }

   private void populateModelMenu(ArtisynthModelMenu generator, JMenu menu) {
      // clear
      menu.removeAll();
      // build from tree
      generator.buildMenu(menu, this, myMain.getModelHistory());
      // add demo entries from menu
      AliasTable demoTable = myMain.getDemoTable();
      AliasTable generatedTable = generator.getDemoTable();
      for (Entry<String,String> entry : generatedTable.getEntries()) {
         demoTable.addEntry(entry.getKey(), entry.getValue());
      }
   }

   private class BackgroundModelMenuThread extends Thread {

      JMenu menu;
      File menuFile;

      public BackgroundModelMenuThread(File menuFile, JMenu menu) {
         super("ModelMenu Loader");
         this.menuFile = menuFile;
         this.menu = menu;
      }

      @Override
      public void run() {
         if (menuFile != null && menuFile.exists()) {
            FunctionTimer timer = new FunctionTimer();
            timer.start();
            ArtisynthModelMenu generator = readDemoMenu(menuFile.getAbsolutePath());
            timer.stop();
            //System.out.println ("menu parse time=" + timer.result(1));

            // XXX only replace menu if it differs from current menu
            if (myModelsMenuGenerator == null || !generator.getMenuTree().equalsTree(myModelsMenuGenerator.getMenuTree())) {
               populateModelMenu(generator, menu);
               // save as cache
               File cachedMenu = getMenuCacheFile(menuFile.getName());
               generator.write(cachedMenu);
               myModelsMenuGenerator = generator;
            }
         }
      }

   }

   private File getMenuCacheFile(String menuFilename) {
      File cachedMenu = new File(ArtisynthPath.getCacheDir(), "/menu/" + menuFilename);
      return cachedMenu;
   }

   private void createDemosMenu(JMenu menu) {

      myModelsMenuGenerator = null;
      String menuFilename = myMain.getDemosMenuFilename();

      File menuFile = ArtisynthPath.findFile(menuFilename);

      // look for cached menu
      if (menuFile != null) {
         File cachedMenu = getMenuCacheFile(menuFile.getName());
         if (cachedMenu.exists()) {
            // read and display cached version, real menu to be created in background
            // file exists, so should be read correctly
            myModelsMenuGenerator = readDemoMenu(cachedMenu.getAbsolutePath());

            if (myModelsMenuGenerator != null) {
               populateModelMenu(myModelsMenuGenerator, menu);

               // background thread to update menu later
               if (menuFile != null && menuFile.exists()) {
                  BackgroundModelMenuThread thread = new BackgroundModelMenuThread(menuFile, menu);
                  thread.start();
               }
            }

         } else {
            // read and create menu now
            if (menuFile != null && menuFile.exists()) {
               myModelsMenuGenerator = readDemoMenu(menuFile.getAbsolutePath());
            }

            if (myModelsMenuGenerator != null) {
               populateModelMenu(myModelsMenuGenerator, menu);
               // save as cache
               myModelsMenuGenerator.write(cachedMenu);
            }
         }
      }
   }

   public void updateHistoryMenu() {
      ModelHistory hist = myMain.getModelHistory();
      myModelsMenuGenerator.updateHistoryNodes(hist, this);
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
      // display.setEnabledAll (false);
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
      myMain.setRootModel(new RootModel(), null, null);
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
         File file = chooser.getSelectedFile();
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
      File modelFile = selectFile("Load", myMain.getModelFile());
      if (modelFile != null) {
         try {
            myMain.loadModelFile(modelFile);
         } catch (Exception e) {
            e.printStackTrace();
            GuiUtils.showError(
               myFrame, "Error reading " + modelFile.getPath() + ":\n" + e);
         }
      }
   }

   private void doLoadFromClass() {
      Class<?> rootModelClass = selectClass("");
      if (rootModelClass != null) {
         if (!RootModel.class.isAssignableFrom(rootModelClass)) {
            GuiUtils.showError (
               myFrame, "Class is not an instanceof RootModel");
            return;
         }
         String className = rootModelClass.getName();
         ModelInfo mi = new ModelInfo(className, rootModelClass.getSimpleName(), null);
         if (!myMain.loadModel(mi)) {
            GuiUtils.showError (myFrame, myMain.getErrorMessage());
         }
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
            // panel.pack();
            // panel.setVisible (true);
            root.addControlPanel(panel);
            myMain.setModelDirectory(chooser.getCurrentDirectory());
         }
      }
   }

   private void doUndoCommand() {
      myMain.getUndoManager().undoLastCommand();
   }

   // /**
   //  * save the probes
   //  * 
   //  */
   // private void doSaveProbes() {
   //    File probeFile = myMain.getProbesFile();
   //    if (probeFile != null) {
   //       try {
   //          myMain.saveProbesFile(probeFile);
   //       }
   //       catch (IOException e) {
   //          GuiUtils.showError (myFrame, "Error writing "+probeFile.getPath(), e);
   //          myMain.setProbesFile (null);
   //       }
   //    }
   // }

   // private void doSaveProbesAs() {
   //    File probeFile = selectFile("Save As", myMain.getProbesFile());
   //    if (probeFile != null) {
   //       if (probeFile.exists()) {
   //          if (!GuiUtils.confirmOverwrite (myFrame, probeFile)) {
   //             return;
   //          }
   //       }
   //       try {
   //          myMain.saveProbesFile (probeFile);
   //          myMain.setProbesFile (probeFile);
   //       }
   //       catch (IOException e) {
   //          GuiUtils.showError (myFrame, "Error writing "+probeFile.getPath(), e);
   //       }
   //    }
   // }

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
      chooser.setCurrentDirectory(ArtisynthPath.getWorkingDir());
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.setAcceptAllFileFilterUsed(false);
      int returnVal = chooser.showDialog(myFrame, "Set working directory");
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         System.out.println(chooser.getSelectedFile());
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

   private File selectFile(String approveMsg, File existingFile) {
      File dir = ArtisynthPath.getWorkingDir(); // is always non-null

      JFileChooser chooser = new JFileChooser(dir);
      if (existingFile != null) {
         chooser.setSelectedFile (existingFile);
      }
      else {
         chooser.setCurrentDirectory(myMain.getModelDirectory());
      }
      int retval = chooser.showDialog(myFrame, approveMsg);
      return ((retval == JFileChooser.APPROVE_OPTION) ?
         chooser.getSelectedFile() : null);
   }

   private Class<?> getClassFromName(String name) {
      try {
         return Class.forName(name);
      } catch (Exception e) {
         return null;
      }
   }

   private String lastSelectedClassName = "";

   private Class<?> selectClass(String existingClassName) {

      // ClassDialog dialog =
      // ClassDialog.createDialog (
      // myFrame, "Choose model class", "Load", "class", existingClassName);
      WidgetDialog dialog =
      WidgetDialog.createDialog(
         myFrame, "Choose model class", "Load");

      // find all instances of 'RootModel' and create an AutoComplete test field
      ArrayList<String> demoClassNames =
      ClassFinder.findClassNames("artisynth.models", RootModel.class);
      AutoCompleteStringField widget =
      new AutoCompleteStringField(
         "class:", lastSelectedClassName, 30, demoClassNames);

      // widget.addValueCheckListener (
      // new ValueCheckListener() {
      // public Object validateValue(ValueChangeEvent e, StringHolder errMsg) {
      // String className = (String)e.getValue();
      // if (className != null || !className.equals("")) {
      // Class type = getClassFromName (className);
      // if (type == null) {
      // return PropertyUtils.illegalValue (
      // "class not found", errMsg);
      // }
      // if (!RootModel.class.isAssignableFrom (type)) {
      // return PropertyUtils.illegalValue (
      // "class not an instance of RootModel", errMsg);
      // }
      // }
      // return PropertyUtils.validValue (className, errMsg);
      // }
      // });
      dialog.setValidator(
         new WidgetDialog.Validator() {
            public String validateSettings(PropertyPanel panel) {
               StringField widget = (StringField)panel.getWidgets()[0];
               String className = widget.getStringValue();
               if (className != null) {
                  Class<?> type = getClassFromName(className);
                  if (type == null) {
                     return "class not found";
                  }
                  if (!RootModel.class.isAssignableFrom(type)) {
                     return "class not an instance of RootModel";
                  }
               }
               return null;
            }
         });
      dialog.addWidget(widget);
      GuiUtils.locateCenter(dialog, myFrame);
      dialog.setVisible(true);
      if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
         String className = widget.getStringValue();
         if (className != null && !className.equals("")) {
            lastSelectedClassName = className;
            return getClassFromName(className);
         }
      }
      return null;
   }

   void setJythonConsoleVisible(boolean visible) {
      if (visible) {
         boolean created = false;
         if (myMain.myJythonFrame == null) {
            myMain.createJythonConsole(/*guiBased=*/true);
            created = true;
         }
         myMain.myJythonFrame.setVisible(true);
         if (created) {
            GuiUtils.locateBelow(myMain.myJythonFrame, myFrame);
         }
      }
      else {
         if (myMain.myJythonFrame != null) {
            myMain.myJythonFrame.setVisible(false);
         }
      }
   }

   public void valueChange(ValueChangeEvent e) {
      if (e.getSource() == myStepDisplay) {
         myMain.setMaxStep(myStepDisplay.getDoubleValue());
      }
      else if (e.getSource() instanceof MouseSettingsDialog) {
         MouseSettingsDialog dialog = (MouseSettingsDialog)e.getSource();
         myMain.setMouseBindings (dialog.getBindings());
         myMain.setMouseWheelZoomScale(dialog.getWheelZoom());
      }
   }

   private void setRealTimeScaling() {

      Scheduler scheduler = myMain.getScheduler(); 

      double scaling = scheduler.getRealTimeScaling ();
      if (!scheduler.getRealTimeAdvance ()) {
         scaling = -1;
      }

      String inputValue = JOptionPane.showInputDialog(
         myFrame, "Scale factor for real-time display",
         scaling);

      System.out.println("Got value for scaling: " + inputValue);

      if (inputValue == null) {
         System.out.println("Clicked cancel on the framerate dialog");
         return;
      }

      String errorMessage = null;
      try {
         scaling = Double.parseDouble(inputValue);
      } catch (NumberFormatException e) {
         errorMessage = "Improperly formed number";
      }

      if (errorMessage != null) {
         GuiUtils.showError (myFrame, errorMessage);
      }
      else {
         if (scaling <= 0) {
            scheduler.setRealTimeAdvance (false);
         } else {
            scheduler.setRealTimeAdvance (true);
            scheduler.setRealTimeScaling (scaling);
         }
         myMain.rerender();
      }
   }

   private void setVisualDisplayRate() {
      String inputValue = JOptionPane.showInputDialog(
         myFrame, "Visual display rate in frames per second",
         myMain.getFrameRate());
      System.out.println("Got value for framerate: " + inputValue);

      if (inputValue == null) {
         System.out.println("Clicked cancel on the framerate dialog");
         return;
      }

      String errorMessage = null;
      double rate = 0;
      try {
         rate = Double.parseDouble(inputValue);
      } catch (NumberFormatException e) {
         errorMessage = "Improperly formed number";
      }

      if (rate < 0) {
         errorMessage = "Rate must be non-negative";
      }

      if (errorMessage != null) {
         GuiUtils.showError (myFrame, errorMessage);
      }
      else {
         myMain.setFrameRate(rate);
      }
   }

   private void openMouseSettingsDialog() {
      MouseSettingsDialog dialog =
      new MouseSettingsDialog (
         "Mouse settings", 
         myMain.getMouseBindings(),
         myMain.getAllMouseBindings(),
         myMain.getMouseWheelZoomScale());
      GuiUtils.locateRight(dialog, myFrame);
      myMain.registerWindow (dialog);
      dialog.addValueChangeListener (this);
      dialog.setVisible(true);
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

   void spawnProgressBar () {
      @SuppressWarnings("unused")
      PardisoSolver s = new PardisoSolver();
   }

   private void doLoadModelSafely(String className, String title, String[] args) {
      doLoadModelSafely(new ModelInfo(className, title, args));
   }

   private void doLoadModelSafely(ModelInfo mi) {
      // Collect as much possible space before loading another model
      if (myMain.getScheduler().isPlaying()) {
         myMain.getScheduler().stopRequest();
         myMain.getScheduler().waitForPlayingToStop();
      }

      System.gc();
      System.runFinalization();

      // load the model with name cmd
      if (!myMain.loadModel(mi)) {
         GuiUtils.showError (myFrame, myMain.getErrorMessage());
      } else {
         myFrame.setBaseTitle("Artisynth " + mi.getShortName());
      }
   }


   /**
    * action performed to process all the menu and button actions in this class
    */
   public void actionPerformed(ModelActionEvent event) {
      String cmd = event.getCommand();
      ModelInfo mi = event.getModelInfo();

      if ("load".equals(cmd)) {
         doLoadModelSafely(mi);
      }
   }

   /**
    * action performed to process all the menu and button actions in this class
    */
   public void actionPerformed(ActionEvent event) {
      String cmd = event.getActionCommand();
      RootModel root = myMain.getRootModel();
      //
      // Scripts menu
      if (isScriptMenuItem(event.getSource())) {
         String scriptName = myMain.getScriptName(cmd);
         runScript(scriptName);
      } else if (cmd.equals("load script from file")) {
         JFileChooser fileChooser = new JFileChooser();
         fileChooser.setCurrentDirectory(ArtisynthPath.getWorkingDir());

         FileFilter jythonFilter = new GenericFileFilter(new String[]{"py", "jy"}, "Jython files") ;
         fileChooser.addChoosableFileFilter(jythonFilter);
         fileChooser.setFileFilter(jythonFilter);
         int result = fileChooser.showOpenDialog(myFrame);
         if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            runScript(selectedFile.getAbsolutePath());
         }
      }
      //
      // Models menu
      //
      else if (myMain.isDemoClassName(cmd)) {
         ModelInfo mi = new ModelInfo(myMain.getDemoClassName(cmd), cmd, null);
         doLoadModelSafely(mi);
      }
      //
      // File Menu
      //
      else if (cmd.equals("Clear model")) {
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
      else if (cmd.equals("Reload model")) {
         doReloadModel();
      }
      else if (cmd.equals("Load model ...")) {
         doLoadModel();
      }
      else if (cmd.equals("Load from class ...")) {
         doLoadFromClass();
      }
      else if (cmd.equals("Set working directory ...")) {
         doSetWorkingDirectory();
      }
      else if (cmd.equals("Open MATLAB connection")) {
         doOpenMatlab();
      }
      else if (cmd.equals("Close MATLAB connection")) {
         doCloseMatlab();
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
      //
      // Settings menu
      //
      else if (cmd.equals("Background color")) {
         setBackgroundColor();
      }
      else if (cmd.equals("Selection color")) {
         setSelectionColor();
      }
      else if (cmd.equals("Enable selection highlighting")) {
         setSelectionColorEnabled(true);
      }
      else if (cmd.equals("Disable selection highlighting")) {
         setSelectionColorEnabled(false);
      }
      else if (cmd.equals("Visual display rate")) {
         setVisualDisplayRate();
      }
      else if (cmd.equals ("Real-time scaling")) {
         setRealTimeScaling ();
      }
      else if (cmd.equals("Mouse Preferences ...")) {
         openMouseSettingsDialog();
      }
      else if (cmd.equals("Init draggers in world coords")) {
         myMain.setInitDraggersInWorldCoords (true);
      }
      else if (cmd.equals("Init draggers in local coords")) {
         myMain.setInitDraggersInWorldCoords (false);
      }
      else if (cmd.equals("Enable articulated transforms")) {
         myMain.setArticulatedTransformsEnabled(true);
      }
      else if (cmd.equals("Disable articulated transforms")) {
         myMain.setArticulatedTransformsEnabled(false);
      }
      //      else if (cmd.equals("Enable GL_SELECT selection")) {
      //         GL2Viewer.enableGLSelectSelection (true);
      //      }
      //      else if (cmd.equals("Disable GL_SELECT selection")) {
      //         GL2Viewer.enableGLSelectSelection (false);
      //      }
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
         isTimelineVisible = false;
      }
      else if (cmd.equals("Show timeline")) {
         myMain.setTimelineVisible(true);
         isTimelineVisible = true;
      }
      else if (cmd.equals("Reset view")) {
         GLViewer v = myMain.getViewer();
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
         setJythonConsoleVisible(false);
      }
      else if (cmd.equals("Show Jython console")) {
         setJythonConsoleVisible(true);
      }
      else if (cmd.equals("Show Inverse panel")) {
         ControlPanel panel = InverseManager.findInversePanel(root);
         TrackingController controller = 
            InverseManager.findInverseController(root);
         if (panel == null && controller != null) {
            InverseManager.addInversePanel (root, controller);
//            myMain.getInverseManager().showInversePanel(
//               myMain.getRootModel(), controller);
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
      else if (cmd.equals("Show progress")) {
         spawnProgressBar ();
      }
      //
      // Help menu
      //
      else if (cmd.equals("About ArtiSynth")) {
         myFrame.displayAboutArtisynth();
      }
      else if (cmd.equals("About the current model")) {
         myFrame.displayAboutModel(root);
      }
      else if (cmd.equals("Keybindings")) {
         myFrame.displayKeybindings();
      }
      //
      // Tool bar buttons
      //
      else if (cmd.equals("Hide NavPanel")) {
         myFrame.getNavPanel().setStatus(!myFrame.getNavPanel().getStatus());
         myFrame.refreshSplitPane();
         navBarButton.setToolTipText("Show navigation panel");
         navBarButton.setActionCommand("Show NavPanel");
      }
      else if (cmd.equals("Show NavPanel")) {
         myFrame.getNavPanel().setStatus(!myFrame.getNavPanel().getStatus());
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
      else if (cmd.equals("Reset initial state")) {
         if (root != null) {
            root.resetInitialState();
         }
      }
      else if (cmd.equals("Disable real-time")) {
         myMain.getScheduler().setRealTimeAdvance(false);
         realtimeButton.setActionCommand ("Enable real-time");
         setButtonPressed (realtimeButton, false);
      }
      else if (cmd.equals("Enable real-time")) {
         myMain.getScheduler().setRealTimeAdvance(true);
         realtimeButton.setActionCommand ("Disable real-time");
         setButtonPressed (realtimeButton, true);
      }

      else if (cmd.equals("cancel")) {
         return;
      }
      else {
         throw new InternalErrorException("Unimplemented command: " + cmd);
      }

   }

   // public void setResetButton (boolean resetEnable) {
   // resetButton.setEnabled (resetEnable);
   // }

   // public void setPlayButton (boolean playEnable) {
   // playButton.setEnabled (playEnable);
   // }

   // public void setStepButton (boolean stepEnable) {
   // singleStepButton.setEnabled (stepEnable);
   // }

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

   public void runScript(String scriptName) {
      runScript(scriptName, null);
   }

   public void runScript(String scriptName, String[] args) {
      setJythonConsoleVisible(true);
      File[] files = ArtisynthPath.findFiles(scriptName);
      if (files != null && files.length > 0) {
         String pathName = protectWindowsSlashes (files[0].getPath());
         try {
            myMain.myJythonConsole.executeScript (pathName, args);
         }
         catch (Exception e) {
            System.out.println ("Error executing script '"+pathName+"':");
            System.out.println (e);
         }
      }
      else {
         GuiUtils.showError (myFrame, 
            "Script " + scriptName + " not found in ARTISYNTH_PATH");
      }
   }

   public void enableShowPlay() {
      playButton.setIcon(GuiStorage.getPlayIcon());
      playButton.setToolTipText("Start simulation");
      playButton.setActionCommand("Play");
      playButton.setEnabled(true);
      // setResetButton (true);
      singleStepButton.setEnabled(true);
   }

   public void disableShowPlay() {
      playButton.setIcon(GuiStorage.getPauseIcon());
      playButton.setToolTipText("Pause simulation");
      playButton.setActionCommand("Pause");
      // setResetButton (true);
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

      if (!myFrame.getNavPanel().getStatus())
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
      // myFrame.add (modeSelectionToolbar, BorderLayout.NORTH);
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

   //   public ViewerController getMainViewerController() {
   //      return myMain.getViewerManager().getController(0);
   //   }

   public GLViewer getMainViewer() {
      return myMain.getViewer();
   }

   public void setBackgroundColor() {

      final ViewerManager vm = myMain.getViewerManager();
      colorChooser.setColor(vm.getBackgroundColor());

      ActionListener setBColor = new ActionListener() {   
         @Override
         public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("OK")) {
               vm.setBackgroundColor(colorChooser.getColor());
            }
            else if (cmd.equals("Cancel")) {
               // do nothing
            }
         }
      };
      JDialog dialog =
      JColorChooser.createDialog(
         myFrame, "color chooser", /* modal= */true, colorChooser,
         setBColor, setBColor);
      GuiUtils.locateRight(dialog, myFrame);
      dialog.setVisible(true);
   }

   public void setSelectionColor() {

      final ViewerManager vm = myMain.getViewerManager();
      colorChooser.setColor(vm.getSelectionColor());

      ActionListener setSColor = new ActionListener() {   
         @Override
         public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("OK")) {
               vm.setSelectionColor(colorChooser.getColor());
            }
            else if (cmd.equals("Cancel")) {
               // do nothing
            }
         }
      };
      JDialog dialog =
      JColorChooser.createDialog(
         myFrame, "color chooser", /* modal= */true, colorChooser,
         setSColor, setSColor);
      GuiUtils.locateRight(dialog, myFrame);
      dialog.setVisible(true);
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

   // public void setFramePlayEnabled (boolean enable) {
   // if (enable) {
   // //resetButton.setEnabled (true);
   // playButton.setEnabled (true);
   // singleStepButton.setEnabled (true);
   // }
   // else {
   // //resetButton.setEnabled (false);
   // playButton.setEnabled (false);
   // singleStepButton.setEnabled (false);
   // }
   // }

   public void updateWidgets() {
      // return if frame is not visible, since updating widgets while
      // frame is being set visible can cause some problems
      if (!myFrame.isVisible()) {
         return;
      }
      GLViewer v = getMainViewer();
      boolean gridOn = v.getGridVisible();
      GLGridPlane grid = v.getGrid();
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

   private JRadioButtonMenuItem addRadioMenuItem (
      JMenu menu, String label, String cmd, ButtonGroup group) {
      JRadioButtonMenuItem item = makeRadioMenuItem(label, cmd);
      group.add(item);
      menu.add(item);
      return item;
   }

   private JMenuItem addMenuItem(JMenu menu, String labelAndCmd) {
      return addMenuItem(menu, labelAndCmd, labelAndCmd);
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
      menu.add(new JSeparator());

      item = addMenuItem(menu, "Reload model");
      item.setEnabled(myMain.modelIsLoaded());

      addMenuItem(menu, "Load model ...");
      addMenuItem(menu, "Load from class ...");

      JMenuItem loadProbesItem, saveProbesItem, saveProbesAsItem;

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
      addMenuItem(menu, "Set working directory ...");

      if (myMain.hasMatlabConnection()) {
         addMenuItem(menu, "Close MATLAB connection");
      }
      else {
         addMenuItem(menu, "Open MATLAB connection");
      }
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

   private void createSettingsMenu(JMenu menu) {

      JMenuItem item;

      addMenuItem(menu, "Background color");
      addMenuItem(menu, "Selection color");

      ViewerManager vm = myMain.getViewerManager();
      if (vm.getSelectionHighlightStyle() == HighlightStyle.COLOR) {
         addMenuItem(menu, "Disable selection highlighting");
      }
      else {
         addMenuItem(menu, "Enable selection highlighting");
      }

      addMenuItem(menu, "Visual display rate");
      addMenuItem(menu, "Real-time scaling");

      if (myMain.getInitDraggersInWorldCoords()) {
         addMenuItem(menu, "Init draggers in local coords");
      }
      else {
         addMenuItem(menu, "Init draggers in world coords");
      }

      if (myMain.getArticulatedTransformsEnabled()) {
         addMenuItem(menu, "Disable articulated transforms");
      }
      else {
         addMenuItem(menu, "Enable articulated transforms");
      }

      //      if (GL2Viewer.isGLSelectSelectionEnabled()) {
      //         addMenuItem(menu, "Disable GL_SELECT selection");
      //      }
      //      else {
      //         addMenuItem(menu, "Enable GL_SELECT selection");
      //      }

      addMenuItem(menu, "Mouse Preferences ...");

      JMenu submenu = new JMenu("PullController");
      menu.add(submenu);

      item = addMenuItem(
         submenu, "properties ...", "PullController properties ...");
      item.setEnabled(myPullControllerPropertyDialog == null);

      item = addMenuItem(
         submenu, "render props ...", "PullController render props ...");
      item.setEnabled(myPullControllerRenderPropsDialog == null);
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

      if (isTimelineVisible) {
         addMenuItem(menu, "Hide timeline");
      }
      else {
         addMenuItem(menu, "Show timeline");
      }

      if (JythonInit.jythonIsAvailable()) {
         if (myMain.myJythonFrame != null && myMain.myJythonFrame.isVisible()) {
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
      addMenuItem(menu, "Show progress");

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

   public void setTimelineVisible(boolean visible) {
      isTimelineVisible = visible;
   }

   private void createHelpMenu(JMenu menu) {
      RootModel rootModel = myMain.getRootModel();

      addMenuItem(menu, "About ArtiSynth");

      addMenuItem(menu, "Keybindings");

      if (rootModel.getAbout() != null && rootModel.getAbout().length() > 0) {
         addMenuItem(menu, "About the current model");
      }
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
