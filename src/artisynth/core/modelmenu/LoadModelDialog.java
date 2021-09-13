package artisynth.core.modelmenu;

import java.io.*;

import java.awt.*;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.awt.event.*;
import java.awt.geom.Line2D;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.plaf.*;

import artisynth.core.gui.ControlPanel;
import artisynth.core.workspace.RootModel;
import artisynth.core.driver.RootModelManager;
import artisynth.core.driver.ModelScriptInfo;
import artisynth.core.modelmenu.PackageDialog.PackageValidator;
import maspack.widgets.*;
import maspack.properties.Property;
import maspack.util.*;
import maspack.widgets.*;

/**
 * Dialog that lets a user choose a model to load into ArtiSynth
 */
public class LoadModelDialog extends ModelDialogBase {

   StringField myArgsField;
   FileNameField myWaypointsField;

   public String getBuildArgs() {
      return myArgsField.getStringValue();
   }

   public void setBuildArgs (String argsStr) {
      myArgsField.setValue (argsStr);
   }

   public File getWaypointsFile() {
      if (myWaypointsField != null) {
         return myWaypointsField.getFile();
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

   public LoadModelDialog (
      Frame frame, ModelScriptInfo modelInfo, String title, RootModelManager rmm) {

      super (frame, title, "Load");
      setDefaultCloseOperation (HIDE_ON_CLOSE); // overrride DISPOSE_ON_CLOSE

      myRootModelManager = rmm;

      // package field
      addPackageField (
         "Package containing the model (auto-complete field)", rmm);
      String className = modelInfo.getClassNameOrFile();
      String packageName = "";
      if (className != null) {
         packageName = getPackageName(className);
         myPackageField.setValue (packageName);
      }

      // model select panel
      addModelSelectPanel();

      // build arguments field
      myArgsField = new StringField ("Build args:", 28);
      myArgsField.setToolTipText (
         "Arguments for the model build() method");
      myArgsField.setValue (modelInfo.getArgsString());
      myArgsField.setStretchable (true);
      addWidget (myArgsField);

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
               myWaypointsField.setValue ("");
               updateWaypointsDirFromPackage();
            }
         });

      // disable confirm button and set up listeners to enable it only when the
      // file and the title are both non-null.

      OptionPanel options = getOptionPanel();
      JButton confirmButton = options.getButton ("Load");
      confirmButton.setEnabled (!myClassList.isSelectionEmpty());

      myClassList.addListSelectionListener (
         new ListSelectionListener() {
            public void valueChanged (ListSelectionEvent e) {
               if (myClassList.isSelectionEmpty()) {
                  confirmButton.setEnabled (false);
               }
               else {
                  confirmButton.setEnabled (true);
                  myWaypointsField.setValue ("");
               }
            }
         });

      addWaypointsField (null);
      
      pack();
   }

   public void addWaypointsField (File file) {
      // waypoint file field
      if (file != null) {
         myWaypointsField = new FileNameField ("Waypoints file: ", file, 60);
      }
      else {
         myWaypointsField = new FileNameField ("Waypoints file: ", 60);
         updateWaypointsDirFromPackage();
      }
      myWaypointsField.setToolTipText (
         "Optional file containing waypoints to load with the model");
      
      addWidget (myWaypointsField);
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

   public void updateKnownPackages () {
      // update the packages used for auto-completion in the package field
      myPackageField.setDataList (myRootModelManager.getKnownPackageNames());
   }
}
