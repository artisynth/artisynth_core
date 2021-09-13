package artisynth.core.modelmenu;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileFilter;

import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.widgets.*;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.RootModel;
import maspack.widgets.*;
import maspack.properties.Property;
import maspack.util.*;
import maspack.widgets.*;

/**
 * Dialog that lets a user add or edit a scriptFolder entry for the model menu.
 */
public class ScriptFolderDialog extends WidgetDialog {

   StringField myTitleField;
   FileNameField myFileField;
   boolean myTitleExplicitlySet = false;

   /**
    * To add or edit, file field must not be empty
    */
   private boolean fileSelected() {
      return !myFileField.valueIsEmpty();
   }   

   public File getSelectedFile() {
      return myFileField.getFile();
   }

   private boolean textSelected() {
      String text = myTitleField.getStringValue();
      return (text != null && text.length() > 0);
   }

   private void myabeSetTitleFromFolder () {
      if (!myFileField.valueIsEmpty()) {
         if (!myTitleExplicitlySet) {
            myTitleField.setValue (getDefaultTitle());
            // need to reset myTitleExplicitlySet since text handler will set it
            // to true
            myTitleExplicitlySet = false; 
         }
      }
   } 

   private String getDefaultTitle() {
      File file = myFileField.getFile();
      if (file != null) {
         return file.getName();
      }
      else {
         return "";
      }
   }

   public ScriptFolderDialog (
      ModelScriptMenuEditor editor, ScriptFolderEntry dentry, File prevFile,
      String title, String confirmCmd) {

      super (editor, title, confirmCmd);
      
      myFileField = new FileNameField ("Script folder:", 100);
      myFileField.setSearchPath (editor.myFileSearchPath);
      JFileChooser chooser = myFileField.getFileChooser();
      chooser.setFileSelectionMode (JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setAcceptAllFileFilterUsed (false);
      FileFilter filter = new FolderFileFilter("Script folders");
      chooser.addChoosableFileFilter (filter);
      chooser.setFileFilter (filter);
      myFileField.setFileMustBeReadable(true);
      File currentFile = dentry.getFile();
      if (currentFile == null) {
         currentFile = prevFile;
      }
      if (currentFile != null) {
         if (currentFile.getName().equals (".")) {
            myFileField.setValue (".");
         }
         else {
            myFileField.setValue (currentFile.getAbsolutePath());
         }
      }
      else {
         File userHomeDir = ArtisynthPath.getUserHomeFolder();
         if (userHomeDir != null) {
            myFileField.setChooserDirectory (userHomeDir);            
         }            
      }      
      addWidget (myFileField);
      addWidget (new JSeparator());

      myTitleField = (StringField)addWidget ("Menu title: ", dentry, "title");
      if (confirmCmd.equals ("Done")) {
         // edit mode
         ModelScriptMenuEditor.addMenuWidgets (this, dentry);
         myTitleExplicitlySet = !getDefaultTitle().equals(dentry.getTitle());
      }
      else {
         myabeSetTitleFromFolder();
      }

      // disable confirm button and set up listeners to enable it only when the
      // file and the title are both non-null.

      OptionPanel options = getOptionPanel();
      JButton confirmButton = options.getButton (confirmCmd);
      confirmButton.setEnabled (fileSelected());

      myTitleField.addValueChangeListener (
         new ValueChangeListener () {
            public void valueChange (ValueChangeEvent e) {
               String text = (String)e.getValue();
               myTitleExplicitlySet = (text != null && text.length() > 0);
               confirmButton.setEnabled (fileSelected());
            }
         });
      myFileField.addValueChangeListener (
         new ValueChangeListener () {
            public void valueChange (ValueChangeEvent e) {
               myabeSetTitleFromFolder();
               confirmButton.setEnabled (fileSelected());
            }
         });

      pack();
   }
}
