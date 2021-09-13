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

import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.widgets.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.util.ArtisynthPath;
import maspack.widgets.*;
import maspack.properties.Property;
import maspack.util.*;
import maspack.widgets.*;

/**
 * Dialog that lets a user add or edit a DemoFile entry for the model menu.
 */
public class DemoFileDialog extends WidgetDialog {

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

   private void maybeSetTitleFromFile () {
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

  public DemoFileDialog (
      ModelScriptMenuEditor editor, DemoFileEntry dentry, File prevFile,
      String title, String confirmCmd) {

      super (editor, title, confirmCmd);
      
      myFileField = new FileNameField ("File name:", 100);
      myFileField.setFileMustBeReadable(true);
      if (dentry.getFile() != null) {
         myFileField.setValue (dentry.getFile().getAbsolutePath());
      }
      else if (prevFile != null) {
         if (prevFile.isDirectory()) {
            myFileField.getFileChooser().setCurrentDirectory(prevFile);
         }
         else {
            myFileField.setValue (prevFile.getAbsolutePath());
         }
      }
      else {
         File userHomeDir = ArtisynthPath.getUserHomeFolder();
         if (userHomeDir != null) {
            myFileField.setChooserDirectory (userHomeDir);            
         }
      }
      myFileField.setSearchPath (editor.myFileSearchPath);
      addWidget (myFileField);
      addWidget (new JSeparator());

      myTitleField = (StringField)addWidget ("Menu title: ", dentry, "title");
      if (confirmCmd.equals ("Done")) {
         // edit mode
         ModelScriptMenuEditor.addMenuWidgets (this, dentry);
         myTitleExplicitlySet = !getDefaultTitle().equals(dentry.getTitle());
      }
      else {
         maybeSetTitleFromFile();
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
               maybeSetTitleFromFile();
               confirmButton.setEnabled (fileSelected());
            }
         });

      pack();
   }
}
