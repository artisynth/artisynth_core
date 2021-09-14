package artisynth.core.modelmenu;

import java.awt.*;
import java.io.*;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import artisynth.core.gui.ControlPanel;
import artisynth.core.workspace.RootModel;
import artisynth.core.driver.RootModelManager;
import artisynth.core.modelmenu.PackageDialog.PackageValidator;
import artisynth.core.util.*;
import maspack.widgets.*;
import maspack.properties.Property;
import maspack.util.*;
import maspack.widgets.*;

/**
 * Dialog that lets a user choose scripts to add to the script menu.
 */
public class ScriptDialog extends ScriptDialogBase {

   StringField myTitleField;
   boolean myTitleExplicitlySet = false;

   /**
    * Ready to add if multiple selection, OR single selection and text field is
    * not empty
    */
   private boolean readyToAdd() {
      if (myScriptList.isSelectionEmpty()) {
         return false;
      }
      else if (myScriptList.getMinSelectionIndex() ==
               myScriptList.getMaxSelectionIndex()) {
         String text = myTitleField.getStringValue();
         // single selection
         return (!myTitleField.valueIsEmpty());
      }
      else {
         // multiple selection
         return true;
      }
   }

   public ScriptDialog (
      ModelScriptMenuEditor editor, ScriptEntry scriptEntry, File scriptFolder,
      String title, String confirmCmd) {

      super (editor, title, confirmCmd);

      // script folder field
      addScriptFolderField ("Folder containing the script(s)");
      myScriptFolderField.setSearchPath (editor.myFileSearchPath);
      if (confirmCmd.equals ("Add")) {
         if (scriptFolder != null) {
            myScriptFolderField.setFile (scriptFolder);
         }
         else {
            File userHomeDir = ArtisynthPath.getUserHomeFolder();
            if (userHomeDir != null) {
               myScriptFolderField.setChooserDirectory (userHomeDir);            
            }            
         }
      }
      else {
         File scriptFile = new File(scriptEntry.getFileName());
         myScriptFolderField.setFile (scriptFile.getParentFile());
      }

      // script list 
      addScriptList();
      
      // title field
      myTitleField =
         (StringField)addWidget ("Script title: ", scriptEntry, "title");

      // arguments fields
      addWidget ("Args: ", scriptEntry, "args");

      if (confirmCmd.equals ("Add")) {
         updateScriptEntries (scriptFolder);
      }
      else {
         // edit mode
         myScriptList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
         if (!myScriptFolderField.valueIsEmpty()) {
            updateScriptEntries (myScriptFolderField.getFile());
         }
         myScriptList.setSelectedValue (
            new ScriptDesc (scriptEntry.getFile(), null), /*shouldScroll=*/true);
         ModelScriptMenuEditor.addFontWidgets (this, scriptEntry);
         myTitleExplicitlySet = !getDefaultTitle().equals(scriptEntry.getTitle());
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
      myScriptList.addListSelectionListener (
         new ListSelectionListener() {
            public void valueChanged (ListSelectionEvent e) {
               if (myScriptList.isSelectionEmpty()) {
                  // no selections
                  myTitleField.setEnabledAll (true);
                  if (!myTitleExplicitlySet) {
                     myTitleField.setValue ("");
                  }
               }
               else if (myScriptList.getMinSelectionIndex() ==
                        myScriptList.getMaxSelectionIndex()) {
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
