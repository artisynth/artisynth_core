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
 * Dialog that lets a user choose models to add to the model menu.
 */
public class ModelDialog extends ModelDialogBase {

   StringField myTitleField;
   boolean myTitleExplicitlySet = false;

   /**
    * Readu to add if multiple selection, OR single selection and text field is
    * not empty
    */
   private boolean readyToAdd() {
      if (myClassList.isSelectionEmpty()) {
         return false;
      }
      else if (myClassList.getMinSelectionIndex() ==
               myClassList.getMaxSelectionIndex()) {
         String text = myTitleField.getStringValue();
         // single selection
         return (!myTitleField.valueIsEmpty());
      }
      else {
         // multiple selection
         return true;
      }
   }

   public ModelDialog (
      Frame frame, ModelEntry modelEntry, String packageName,
      String title, String confirmCmd, RootModelManager rmm) {

      super (frame, title, confirmCmd);

      myRootModelManager = rmm;

      // package field
      addPackageField (
         "Package containing the model(s) (auto-complete field)", rmm);
      if (confirmCmd.equals ("Add")) {
         myPackageField.setValue (packageName);
      }
      else {
         myPackageField.setValue (getPackageName(modelEntry.getClassName()));
      }

      // model select panel
      addModelSelectPanel();

      // title field
      myTitleField = 
         (StringField)addWidget ("Menu title: ", modelEntry, "title");
      // build arguments field
      addWidget ("Build args: ", modelEntry, "buildArgs");

      if (confirmCmd.equals ("Add")) {
         updateClassEntries (packageName);
      }
      else {
         // edit mode
         myClassList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
         if (!myPackageField.valueIsEmpty()) {
            updateClassEntries (myPackageField.getStringValue());
         }
         myClassList.setSelectedValue (
            modelEntry.getClassName(), /*shouldScroll=*/true);
         ModelScriptMenuEditor.addFontWidgets (this, modelEntry);
         myTitleExplicitlySet = !getDefaultTitle().equals(modelEntry.getTitle());
      }
      
      // disable confirm button and set up listeners to enable it only when the
      // file and the title are both non-null.

      OptionPanel options = getOptionPanel();
      JButton confirmButton = options.getButton (confirmCmd);
      confirmButton.setEnabled (readyToAdd());

      myTitleField.addValueChangeListener (
         new ValueChangeListener () {
            public void valueChange (ValueChangeEvent e) {
               String text = (String)e.getValue();
               myTitleExplicitlySet = (text != null && text.length() > 0);
               confirmButton.setEnabled (readyToAdd());
            }
         });
      myClassList.addListSelectionListener (
         new ListSelectionListener() {
            public void valueChanged (ListSelectionEvent e) {
               if (myClassList.isSelectionEmpty()) {
                  // no selections
                  myTitleField.setEnabledAll (true);
                  if (!myTitleExplicitlySet) {
                     myTitleField.setValue ("");
                  }
               }
               else if (myClassList.getMinSelectionIndex() ==
                        myClassList.getMaxSelectionIndex()) {
                  // single selection
                  myTitleField.setEnabledAll (true);
                  if (myTitleField.valueIsEmpty() || !myTitleExplicitlySet) {
                     myTitleField.setValue (getDefaultTitle());
                     myTitleExplicitlySet = false;
                  }
               }
               else {
                  // multiple selection
                  myTitleField.setEnabledAll (false);
                  if (!myTitleExplicitlySet) {
                     myTitleField.setValue ("");
                  }
               }
               confirmButton.setEnabled (readyToAdd());
            }
         });

      pack();
   }


}
