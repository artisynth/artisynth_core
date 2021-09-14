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
 * Base class for dialogs that let users choose Jython scripts
 */
public class ScriptDialogBase extends WidgetDialog {

   protected static final Color myListBackgroundColor =
      UIManager.getColor ("List.background");
   protected static final Color myListSelectionBackgroundColor =
      UIManager.getColor ("List.selectionBackground");
   protected static final Color myListSelectionBorderColor =
      UIManager.getColor ("List.selectionBorderColor");
   protected static final Color myListForegroundColor =
      UIManager.getColor ("List.foreground");

   protected FileNameField myScriptFolderField;
   protected JList<ScriptDesc> myScriptList;
   protected DefaultListModel<ScriptDesc> myListModel;
   
   public static class ScriptDesc implements Comparable<ScriptDesc> {
      File myFile;
      String myExplicitName;
      
      ScriptDesc (File file, String name){
         myFile = file;
         myExplicitName = name;
      }
      
      public int compareTo (ScriptDesc si) {
         return myFile.getName().compareTo (si.myFile.getName());
      }

      public File getFile() {
         return myFile;
      }

      public String getName() {
         if (myExplicitName != null) {
            return myExplicitName;
         }
         else {
            return ScriptEntry.getBaseFileName (myFile);
         }
      }

      public boolean equals (Object obj) {
         if (obj instanceof ScriptDesc) {
            return myFile.equals (((ScriptDesc)obj).myFile);
         }
         else {
            return false;
         }
      }

      public int hashCode() {
         return myFile.hashCode();
      }
      
   }

   public ScriptDesc getScript() {
      return myScriptList.getSelectedValue();
   }

   public List<ScriptDesc> getScripts() {
      return myScriptList.getSelectedValuesList();
   }

   protected ScriptDialogBase (Frame frame, String title, String confirmCmd) {
      super (frame, title, confirmCmd);
   }

   protected class CheckboxListRenderer extends JCheckBox
      implements ListCellRenderer<ScriptDesc> {
      
      @Override
         public Component getListCellRendererComponent (
            JList<? extends ScriptDesc> list, ScriptDesc value, 
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
         String text = value.myFile.getName();
         if (value.myExplicitName != null) {
            text += " (" + value.myExplicitName + ")";
         }
         setText(text);
         return this;
      }
   }
                                                   
   public File getScriptFolder() {
      return myScriptFolderField.getFile();
   }

   protected void updateScriptEntries (File scriptFolder) {
      // check for null in case listener calls updateScriptEntries() before
      // list created
      if (myListModel != null) { 
         myListModel.removeAllElements();
         if (scriptFolder != null && scriptFolder.isDirectory()) { // paranoid
            ArrayList<ScriptDesc> scripts = new ArrayList<>();
            ArrayList<File> dirList = new ArrayList<>();
            dirList.add (scriptFolder);         
            File[] files = ArtisynthPath.findFilesMatching (
               dirList, ".*\\.(py|jy)");
            Pattern pattern = ScriptFolderEntry.getInternalScriptNamePattern();
            for (int i=0; i<files.length; i++) {
               String name =
                  ScriptFolderEntry.getInternallyDefinedScriptName (
                     files[i], pattern);
               if (name != null) {
                  if (name.equals(ScriptEntry.getBaseFileName (files[i]))) {
                     // don't need a separate name 
                     name = null;
                  }
               }
               scripts.add (new ScriptDesc (files[i], name));
            }
            Collections.sort (scripts);
            for (ScriptDesc script : scripts) {
               myListModel.addElement (script);
            }
         }
      }      
   }

   protected String getDefaultTitle () {
      ScriptDesc si = myScriptList.getSelectedValue();
      if (si != null) {
         return si.getName();
      }
      else {
         return "";
      }
   }

   protected void addScriptFolderField (String toolTip) {
      
      myScriptFolderField = new FileNameField ("Script folder:", 100);
      JFileChooser chooser = myScriptFolderField.getFileChooser();
      chooser.setFileSelectionMode (JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setAcceptAllFileFilterUsed (false);
      FolderFileFilter filter = new FolderFileFilter("Script folders");
      chooser.addChoosableFileFilter (filter);
      chooser.setFileFilter (filter);
      myScriptFolderField.setToolTipText (toolTip);
      myScriptFolderField.setFileMustBeReadable(true);
      myScriptFolderField.setEnterValueOnFocusLost (false);
      addWidget (myScriptFolderField);

      myScriptFolderField.addValueChangeListener (
         new ValueChangeListener () {
            public void valueChange (ValueChangeEvent e) {
               String fileName = (String)e.getValue();
               updateScriptEntries (new File(fileName));
            }
         });
   }

   protected void addScriptList () {
      LabeledComponent scriptLabel = new LabeledComponent ("Script file:");
      scriptLabel.setToolTipText ("Select the script from the list below");
      addWidget (scriptLabel);

      myListModel = new DefaultListModel<>();
      myScriptList = new JList<>(myListModel);
      myScriptList.setCellRenderer (new CheckboxListRenderer());

      JScrollPane listPane = new JScrollPane (myScriptList);
      listPane.setPreferredSize (new Dimension (320, 320));
      listPane.setViewportBorder (BorderFactory.createEmptyBorder (3, 3, 3, 3));
      addWidget (listPane);
   }

}
