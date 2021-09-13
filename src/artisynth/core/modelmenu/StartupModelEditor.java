package artisynth.core.modelmenu;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.IconUIResource;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;

import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.editorManager.Command;
import artisynth.core.gui.editorManager.UndoManager;
import artisynth.core.modelmenu.PackageDialog.PackageValidator;
import artisynth.core.util.AliasTable;
import artisynth.core.driver.*;
import artisynth.core.driver.ModelScriptInfo.InfoType;
import artisynth.core.workspace.*;
import artisynth.core.util.ArtisynthPath;
import maspack.util.PathFinder;
import maspack.util.*;
import maspack.widgets.*;
import maspack.properties.Property;
import maspack.widgets.OptionPanel;
import maspack.widgets.StringField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.widgets.WidgetDialog;

public class StartupModelEditor extends JFrame
   implements ActionListener {

   StartupModel myStartupModel;
   Main myMain;
   File myConfigFile;

   private static final Color myListBackgroundColor =
      UIManager.getColor ("List.background");
   private static final Color myListSelectionBackgroundColor =
      UIManager.getColor ("List.selectionBackground");
   private static final Color myListSelectionBorderColor =
      UIManager.getColor ("List.selectionBorderColor");
   private static final Color myListForegroundColor =
      UIManager.getColor ("List.foreground");

   AutoCompleteStringField myPackageField;
   JList<String> myClassList;
   DefaultListModel<String> myListModel;
   StringField myArgsField;
   FileNameField myModelFileField;
   FileNameField myWaypointsField;
   RootModelManager myRootModelManager;

   JPanel myPanel;
   PropertyPanel myTopPanel;

   JButton mySaveButton;
   JButton myCancelButton;
   JButton myClearButton;
   JButton myLoadModelButton;

   class ClassSelectionListener implements ListSelectionListener {

      public void valueChanged (ListSelectionEvent e) {
         ListSelectionModel lsm = myClassList.getSelectionModel();
      }
   }

   class ClassListItem {

      private String myClassName;
      private boolean myIsSelected = false;

      public ClassListItem (String className) {
         myClassName = className;
      }

      public String getTailName() {
         return ModelScriptMenuEditor.getTailName(myClassName);
      }
   }  

   private String getPackageName (String className) {
      int lastDot = className.lastIndexOf ('.');
      if (lastDot != -1) {
         return className.substring (0, lastDot);
      }
      else {
         return className;
      }
   }

   private String getSimpleName (String className) {
      int lastDot = className.lastIndexOf ('.');
      if (lastDot != -1) {
         return className.substring (lastDot+1);
      }
      else {
         return className;
      }
   }

   public String getClassName() {
      if (myClassList.isSelectionEmpty()) {
         return null;
      }
      else {
         return myClassList.getSelectedValue();
      }
   }

   public File getModelFile() {
      String fileName = myModelFileField.getStringValue();
      if (fileName != null && fileName.length() > 0) {
         return new File(myModelFileField.getStringValue());
      }
      else {
         return null;
      }
   }

   ModelScriptInfo getModelInfo() {
      if (getClassName() != null) {
         String className = getClassName();
         return new ModelScriptInfo (
            InfoType.CLASS, className, getSimpleName(className),
            ModelScriptInfo.splitArgs(myArgsField.getStringValue()));
      }
      else if (getModelFile() != null) {
         File file = getModelFile();
         return new ModelScriptInfo (
            InfoType.FILE, file.getAbsolutePath(), file.getName(), null);
      }
      else {
         return null;
      }
   }

   public List<String> getClassNames() {
      return myClassList.getSelectedValuesList();
   }

   public String getBuildArgs() {
      return myArgsField.getStringValue();
   }

   public void setBuildArgs (String argsStr) {
      myArgsField.setValue (argsStr);
   }

   public File getWaypointsFile() {
      if (!myWaypointsField.valueIsEmpty()) {
         return new File(myWaypointsField.getStringValue());
      }
      else {
         return null;
      }
   }

   public void setWaypointsFile (File file) {
      if (myWaypointsField != null) {
         myWaypointsField.setValue (file.toString());
      }
   }

   class CheckboxListRenderer extends JCheckBox
      implements ListCellRenderer<String> {
      
      @Override
         public Component getListCellRendererComponent (
            JList<? extends String> list, String value, 
            int index, boolean isSelected, boolean cellHasFocus) {
         setEnabled (list.isEnabled());
         setSelected (isSelected);
         if (isSelected) {
            setBackground(myListSelectionBackgroundColor);
         }
         else {
            setBackground(myListBackgroundColor);
         }
         setForeground(list.getForeground());
         setText(getSimpleName(value));
         return this;
      }
   }
                                                   

   public String getPackageName() {
      return myPackageField.getStringValue();
   }

   private void updateClassEntries (String packageName) {
      myListModel.removeAllElements();
      if (packageName.length() > 0) {
         ArrayList<String> clsList;
         if (myRootModelManager != null) {
            clsList = myRootModelManager.findModels (
               packageName, /*flags=*/RootModelManager.INCLUDE_HIDDEN);
         }
         else {
            clsList = ClassFinder.findClassNames (
               packageName, RootModel.class, /*recursive=*/false);
         }
         Collections.sort (clsList);
         for (String name : clsList) {
            myListModel.addElement (name);
         }
      }
   }

   private JButton addBottomButton (JPanel panel, String cmd, String toolTip) {
      JButton button = new JButton (cmd);
      button.setActionCommand (cmd);
      button.addActionListener (this);
      button.setAlignmentX (Component.CENTER_ALIGNMENT);
      if (toolTip != null) {
         button.setToolTipText (toolTip);
      }
      panel.add (button);
      return button;
   }

   public StartupModelEditor (StartupModel config, Main main) {

      super ("Startup model editor");

      myStartupModel = config;
      myMain = main;
      ModelScriptInfo modelInfo = config.getModelInfo();
      File waypointsFile = config.getWaypointsFile();
      File userHomeDir = ArtisynthPath.getUserHomeFolder();

      myPanel = new JPanel();
      myPanel.setLayout (new BorderLayout());

      PropertyPanel topPanel = new PropertyPanel();

      // package field
      myPackageField =
         new AutoCompleteStringField (
            "Model Package: ", "", 28,
            main.getRootModelManager().getKnownPackageNames());
      myPackageField.setStretchable (true);
      myPackageField.setToolTipText (
         "Package containing the model (auto-complete field)");
      String className = modelInfo != null ? modelInfo.getClassName() : "";
      String packageName = "";
      if (className.length() > 0) {
         packageName = getPackageName(className);
         myPackageField.setValue (packageName);
      }
      myPackageField.setEnterValueOnFocusLost (false);
      myPackageField.addValueCheckListener (new PackageValidator());
      topPanel.addWidget (myPackageField);

      // model select panel
      LabeledComponent modelLabel = new LabeledComponent ("Model class:");
      modelLabel.setToolTipText (
         "Select the model from the list below");
      topPanel.addWidget (modelLabel);

      myListModel = new DefaultListModel<>();
      updateClassEntries (packageName);

      myClassList = new JList<>(myListModel);
      myClassList.setCellRenderer (new CheckboxListRenderer());
      myClassList.addListSelectionListener (new ClassSelectionListener());

      JScrollPane listPane = new JScrollPane (myClassList);
      listPane.setPreferredSize (new Dimension (320, 320));
      listPane.setViewportBorder (BorderFactory.createEmptyBorder (3, 3, 3, 3));
      topPanel.addWidget (listPane);

      // build arguments field
      myArgsField = new StringField ("Build args:", 28);
      myArgsField.setToolTipText (
         "Arguments for the model build() method");
      if (modelInfo != null && modelInfo.getArgs() != null) {
         myArgsField.setValue (modelInfo.getArgsString());
      }
      myArgsField.setStretchable (true);
      topPanel.addWidget (myArgsField);

      // model file field
      topPanel.addWidget (new JSeparator());
      File modelFile = modelInfo != null ? modelInfo.getFile() : null;
      if (modelFile != null) {
         myModelFileField =
            new FileNameField ("Model file: ", modelFile, 60);
      }
      else {
         myModelFileField = new FileNameField ("Model file: ", 60);
         if (userHomeDir != null) {
            myModelFileField.setChooserDirectory (userHomeDir);
         }
      }
      myModelFileField.setToolTipText (
         "File to load the model from instead of a class");
      JFileChooser chooser =myModelFileField.getFileChooser();
      GenericFileFilter filter = new GenericFileFilter (
         "art", "ArtiSynth model files (*.art)");
      chooser.addChoosableFileFilter (filter);
      if (modelFile == null || filter.fileExtensionMatches (modelFile)) {
         chooser.setFileFilter (filter);
      }
      topPanel.addWidget (myModelFileField);

      // waypoint file field
      if (waypointsFile != null) {
          myWaypointsField =
            new FileNameField ("Waypoints file: ", waypointsFile, 60);
      }
      else {
         myWaypointsField =
            new FileNameField ("Waypoints file: ", 60);
         if (modelFile != null) {
            updateWaypointsDirFromModelFile (modelFile);
         }
         else if (className.length() > 0) {
            updateWaypointsDirFromPackage();
         }
         else {
            if (userHomeDir != null) {
               myWaypointsField.setChooserDirectory (userHomeDir);
            }
         }
      }
      myWaypointsField.setToolTipText (
         "Optional file containing waypoints to load with the model");

      topPanel.addWidget (new JSeparator());      
      topPanel.addWidget (myWaypointsField);
      topPanel.addWidget (new JSeparator());      

      JPanel bottomPanel = new JPanel();
      bottomPanel.setBorder (BorderFactory.createEmptyBorder (5, 5, 5, 5));
      bottomPanel.setLayout (new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
      bottomPanel.add (Box.createRigidArea (new Dimension (0, 10)));
      myLoadModelButton = addBottomButton (
         bottomPanel, "Load Model", "Load model into application");
      bottomPanel.add (Box.createRigidArea (new Dimension (10, 10)));
      bottomPanel.add (Box.createHorizontalGlue());
      myClearButton = addBottomButton (
         bottomPanel, "Clear", "Clear startup model");
      bottomPanel.add (Box.createRigidArea (new Dimension (10, 10)));
      myCancelButton = addBottomButton (
         bottomPanel, "Cancel", "Close dialog without making changes");
      bottomPanel.add (Box.createRigidArea (new Dimension (10, 10)));
      mySaveButton = addBottomButton (
         bottomPanel, "Save", "Save startup model and close dialog");
      bottomPanel.add (Box.createRigidArea (new Dimension (0, 10)));

      myPanel.add (topPanel, BorderLayout.CENTER);
      myPanel.add (bottomPanel, BorderLayout.PAGE_END);

      getContentPane().add (myPanel);

      //if (confirmCmd.equals ("Done")) {
         // edit mode
      myClassList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
      if (!myPackageField.valueIsEmpty()) {
         updateClassEntries (myPackageField.getStringValue());
      }
      if (className != null) {
         myClassList.setSelectedValue (
            className, /*shouldScroll=*/true);
      }
      myPackageField.addValueChangeListener (
         new ValueChangeListener () {
            public void valueChange (ValueChangeEvent e) {
               String packname = (String)e.getValue();
               updateClassEntries (packname);
               myWaypointsField.setValue ("");
               updateWaypointsDirFromPackage();
            }
         });

      // if we select a model class, clear the model file field
      myClassList.addListSelectionListener (
         new ListSelectionListener() {
            public void valueChanged (ListSelectionEvent e) {
               if (!myClassList.isSelectionEmpty()) {
                  if (!myModelFileField.valueIsEmpty()) {
                     myModelFileField.maskValueChangeListeners (true);
                     myModelFileField.setValue ("");
                     myModelFileField.maskValueChangeListeners (false);
                  }
                  myWaypointsField.setValue ("");
               }
               updateLoadModelButton();
            }
         });

      // Only allow selection of model files that exist1
      myModelFileField.addValueCheckListener (
         new ValueCheckListener() {
            public Object validateValue (
               ValueChangeEvent e, StringHolder errMsg) {
               String fileName = (String)e.getValue();
               if (fileName != null && fileName.length() > 0) {
                  File file = new File(fileName);
                  if (!file.canRead()) {
                     errMsg.value = "File does not exist or is unreadable";
                     return Property.IllegalValue;
                  }
               }
               return fileName;
            }
         });

      myModelFileField.addValueChangeListener (
         new ValueChangeListener() {
            public void valueChange (ValueChangeEvent e) {
               if (!myModelFileField.valueIsEmpty()) {
                  if (!myClassList.isSelectionEmpty()) {
                     myClassList.clearSelection();
                  }
                  myWaypointsField.setValue ("");
                  updateWaypointsDirFromModelFile(myModelFileField.getFile());
               }
               updateLoadModelButton();
            }
         });

      updateLoadModelButton();
      pack();
   }

   private void updateWaypointsDirFromPackage() {
      String packname = myPackageField.getStringValue();
      if (!"".equals(packname)) {
         String dirpath = PathFinder.findPackageSourceDir (packname);
         if (dirpath != null) {
            myWaypointsField.setChooserDirectory(new File(dirpath));
         }
      }
   }

   private void updateWaypointsDirFromModelFile (File modelFile) {
      if (modelFile != null && modelFile.getParentFile() != null) {
         myWaypointsField.setChooserDirectory(modelFile.getParentFile());
      }
   }

   public void updateLoadModelButton() {
      myLoadModelButton.setEnabled (
         getClassName() != null || getModelFile() != null);
   }

   public void reloadValues() {
      ModelScriptInfo modelInfo = myStartupModel.getModelInfo();
      File waypointsFile = myStartupModel.getWaypointsFile();

      if (modelInfo == null) {
         myArgsField.setValue ("");
         myModelFileField.setValue ("");
         myPackageField.setValue ("");
      }
      else if (modelInfo.getType() == InfoType.CLASS) {
         String className = modelInfo.getClassName();
         myPackageField.setValue (getPackageName(className));
         if (className != null) {
            myClassList.setSelectedValue (
               className, /*shouldScroll=*/true);
         }
         myArgsField.setValue (modelInfo.getArgsString());
         myModelFileField.setValue ("");
      }
      else if (modelInfo.getType() == InfoType.FILE) {
         myPackageField.setValue ("");
         myArgsField.setValue ("");
         myModelFileField.setValue (modelInfo.getFile().getAbsolutePath());
      }
   }

   public void updateKnownPackages () {
      // update the packages used for auto-completion in the package field
      myPackageField.setDataList (myRootModelManager.getKnownPackageNames());
   }

   public void actionPerformed (ActionEvent ev) {
      String cmdName = ev.getActionCommand();

      if (cmdName.equals ("Save")) {
         myStartupModel.setModelInfo (getModelInfo());
         myStartupModel.setWaypointsFile (getWaypointsFile());
         myStartupModel.save();
         setVisible (false);
      }
      else if (cmdName.equals ("Cancel")) {
         setVisible (false);
      }
      else if (cmdName.equals ("Clear")) {
         myPackageField.setValue ("");
         myArgsField.setValue ("");
         myModelFileField.setValue ("");
         myWaypointsField.setValue ("");
      }
      else if (cmdName.equals ("Load Model")) {
         ModelScriptInfo modelInfo = getModelInfo();
         File waypointsFile = getWaypointsFile();
         if (modelInfo != null) {
            if (!myMain.loadModel(modelInfo)) {
               String errMsg = null;
               if (myMain.getErrorMessage() != null) {
                  errMsg = myMain.getErrorMessage();
               }
               else {
                  errMsg = "Error loading model";
               }
               GuiUtils.showError (this, errMsg);
               modelInfo = null;
            }
         }
         else {
            myMain.setRootModel (new RootModel(), null);
         }
         if (modelInfo != null && waypointsFile != null) {
            try {
               myMain.loadWayPoints (waypointsFile);
            }
            catch (IOException e) {
               e.printStackTrace(); 
               GuiUtils.showError (
                  this, "Error loading waypoints: "+e);
            }           
         }
      }
   }
}

/*
// TODO:

 make sure waypoint file is absolute and is readable

 implement reload values for cancel

*/

