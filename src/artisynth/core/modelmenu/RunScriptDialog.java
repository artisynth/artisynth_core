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
import artisynth.core.driver.ModelScriptInfo;
import artisynth.core.driver.RootModelManager;
import artisynth.core.modelmenu.PackageDialog.PackageValidator;
import artisynth.core.util.*;
import maspack.widgets.*;
import maspack.properties.Property;
import maspack.util.*;
import maspack.widgets.*;

/**
 * Dialog that lets a user choose a script to run
 */
public class RunScriptDialog extends ScriptDialogBase {

   StringField myArgsField;

   public String getArgs() {
      return myArgsField.getStringValue();
   }

   public void setArgs (String argsStr) {
      myArgsField.setValue (argsStr);
   }

   public RunScriptDialog (
      Frame frame, ModelScriptInfo scriptInfo, String title) {

      super (frame, title, "Run");
      setDefaultCloseOperation (HIDE_ON_CLOSE); // overrride DISPOSE_ON_CLOSE

      // script folder field
      addScriptFolderField ("Folder containing the script");
      myScriptFolderField.setSearchPath (ArtisynthPath.createDefaultSearchPath());
      String scriptName = scriptInfo.getClassNameOrFile();
      if (scriptName != null) {
         myScriptFolderField.setFile (new File (scriptName).getParentFile());
      }

      // script list 
      addScriptList();
      myScriptList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);

      // arguments field
      myArgsField = new StringField ("Args:", 28);
      myArgsField.setToolTipText (
         "Arguments for the script");
      myArgsField.setValue (scriptInfo.getArgsString());
      myArgsField.setStretchable (true);
      addWidget (myArgsField);

      if (!myScriptFolderField.valueIsEmpty()) {
         updateScriptEntries (myScriptFolderField.getFile());
      }
      if (scriptName != null) {
         myScriptList.setSelectedValue (
            new ScriptDesc (new File(scriptName), null),
            /*shouldScroll=*/true);
      }

      // disable confirm button and set up listeners to enable it only when the
      // file and the title are both non-null.

      OptionPanel options = getOptionPanel();
      JButton confirmButton = options.getButton ("Run");
      confirmButton.setEnabled (!myScriptList.isSelectionEmpty());

      myScriptList.addListSelectionListener (
         new ListSelectionListener() {
            public void valueChanged (ListSelectionEvent e) {
               confirmButton.setEnabled (!myScriptList.isSelectionEmpty());
            }
         });

      pack();
   }


}
