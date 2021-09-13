package artisynth.core.modelmenu;

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
import artisynth.core.modelmenu.PackageDialog.PackageValidator;
import maspack.widgets.*;
import maspack.properties.Property;
import maspack.util.*;
import maspack.widgets.*;

/**
 * Base class for dialogs that let users choose model classes
 */
public class ModelDialogBase extends WidgetDialog {

   protected static final Color myListBackgroundColor =
      UIManager.getColor ("List.background");
   protected static final Color myListSelectionBackgroundColor =
      UIManager.getColor ("List.selectionBackground");
   protected static final Color myListSelectionBorderColor =
      UIManager.getColor ("List.selectionBorderColor");
   protected static final Color myListForegroundColor =
      UIManager.getColor ("List.foreground");

   protected AutoCompleteStringField myPackageField;
   protected JList<String> myClassList;
   protected DefaultListModel<String> myListModel;
   protected RootModelManager myRootModelManager;

   protected String getPackageName (String className) {
      int lastDot = className.lastIndexOf ('.');
      if (lastDot != -1) {
         return className.substring (0, lastDot);
      }
      else {
         return className;
      }
   }

   protected String getSimpleName (String className) {
      int lastDot = className.lastIndexOf ('.');
      if (lastDot != -1) {
         return className.substring (lastDot+1);
      }
      else {
         return className;
      }
   }

   public String getClassName() {
      return myClassList.getSelectedValue();
   }

   public List<String> getClassNames() {
      return myClassList.getSelectedValuesList();
   }

   protected class CheckboxListRenderer extends JCheckBox
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

   protected void updateClassEntries (String packageName) {
      // check for null in case listener calls updateScriptEntries() before
      // list created
      if (myListModel != null) {
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
   }

   protected String getDefaultTitle() {
      String name = myClassList.getSelectedValue();
      if (name != null) {
         return getSimpleName(name);
      }
      else {
         return "";
      }
   }

   protected ModelDialogBase (Frame frame, String title, String confirmCmd) {
      super (frame, title, confirmCmd);
   }

   protected void addPackageField (String toolTip, RootModelManager rmm) {
      myPackageField =
         new AutoCompleteStringField (
            "Model package: ", "", 28, rmm.getKnownPackageNames());
      myPackageField.setStretchable (true);
      myPackageField.setToolTipText (toolTip);
      myPackageField.setEnterValueOnFocusLost (false);
      myPackageField.addValueCheckListener (new PackageValidator());
      addWidget (myPackageField);   

      myPackageField.addValueChangeListener (
         new ValueChangeListener () {
            public void valueChange (ValueChangeEvent e) {
               String packname = (String)e.getValue();
               updateClassEntries (packname);
            }
         });
   }

   protected void addModelSelectPanel() {
      LabeledComponent modelLabel = new LabeledComponent ("Model class:");
      modelLabel.setToolTipText (
         "Select the model from the list below");
      addWidget (modelLabel);

      myListModel = new DefaultListModel<>();
      myClassList = new JList<>(myListModel);
      myClassList.setCellRenderer (new CheckboxListRenderer());

      JScrollPane listPane = new JScrollPane (myClassList);
      listPane.setPreferredSize (new Dimension (320, 320));
      listPane.setMinimumSize (new Dimension (0, 20));
      listPane.setViewportBorder (BorderFactory.createEmptyBorder (3, 3, 3, 3));
      addWidget (listPane);
   }
}
