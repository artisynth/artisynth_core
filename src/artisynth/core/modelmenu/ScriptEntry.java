/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import java.awt.Component;
import java.io.*;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import artisynth.core.driver.ModelScriptInfo;
import artisynth.core.driver.ModelScriptInfo.InfoType;
import artisynth.core.modelmenu.ModelScriptMenuParser.MenuType;
import artisynth.core.workspace.RootModel;

import maspack.util.*;
import maspack.properties.*;

public class ScriptEntry extends MenuNode {

   static String getBaseFileName (File file) {
      String baseName = null;
      if (file != null) {
         baseName = file.getName();
         int lastDot = baseName.lastIndexOf ('.');
         if (lastDot != -1) {
            baseName = baseName.substring (0, lastDot);
         }
      }
      return baseName;
   }         

   static String getDefaultScriptTitle (File file) {
      // first see if the title is defined internally:
      Pattern pattern = ScriptFolderEntry.getInternalScriptNamePattern();
      String title =
         ScriptFolderEntry.getInternallyDefinedScriptName (file, pattern);
      if (title == null) {
         // use the file base name instead
         title = file.getName();
         int lastDot = title.lastIndexOf ('.');
         if (lastDot != -1) {
            title = title.substring (0, lastDot);
         }
      }
      return title;
   }         


   // /**
   //  * Checks that a file name describes a valid script
   //  */
   // static class FileNameRange extends RangeBase {

   //    public boolean isValid (Object obj, StringHolder errMsg) {
   //       if (obj == null) {
   //          return true;
   //       }
   //       else if (obj instanceof String) {
   //          String name = (String)obj;
   //          if (name.length() == 0) {
   //             return true;
   //          }
   //          // Class<?> clazz;
   //          // try {
   //          //    clazz = Class.forName (name);
   //          // }
   //          // catch (Exception e) {
   //          //    if (errMsg != null) {
   //          //       errMsg.value = "'"+name+"' is not a known class";
   //          //    }
   //          //    return false;
   //          // }
   //          // if (!RootModel.class.isAssignableFrom (clazz)) {
   //          //    if (errMsg != null) {
   //          //       errMsg.value = "'"+name+"' is not a RootModel";
   //          //    }
   //          //    return false;
   //          // }
   //          return true;
   //       }
   //       return false;
   //    }
   // }

   private ModelScriptInfo myScriptInfo =
      new ModelScriptInfo (InfoType.SCRIPT, "", "", new String[0]);
   private ModelScriptActionForwarder myActionForwarder = null;
 
   public static final String DEFAULT_FILE_NAME = "";
   public static final String DEFAULT_ARGS = "";
   protected String myArgs = new String(DEFAULT_ARGS);

   private JMenuItem myMenuItem;

   public static PropertyList myProps =
      new PropertyList (ScriptEntry.class, MenuNode.class);

   static {
      myProps.add ("fileName", "File name for the script", DEFAULT_FILE_NAME);
      myProps.add ("args", "Arguments for the script", DEFAULT_ARGS);
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

  public ScriptEntry() {
      super();
   }

   public ScriptEntry (String fileName, String title, String[] args) {
      super (fileName);
      setScriptInfo (new ModelScriptInfo (InfoType.SCRIPT, fileName, title, args));
   }

   public ModelScriptInfo getScriptInfo () {
      return myScriptInfo;
   }

   public void setScriptInfo (ModelScriptInfo info) {
      myScriptInfo = info;
   }

   public String getTitle() {
      return myScriptInfo.getShortName();
   }

   public void setTitle (String title) {
      if (!title.equals (getTitle())) {
         String[] args = ModelScriptInfo.splitArgs (myArgs);
         myScriptInfo =
            new ModelScriptInfo (InfoType.SCRIPT, getFileName(), title, args);
      }
   }

   public boolean fileNameIsEmpty() {
      String name = myScriptInfo.getClassNameOrFile();
      return name == null || name.length() == 0;
   }

   public String getFileName() {
      return myScriptInfo.getClassNameOrFile();
   }

   public File getFile() {
      return new File(myScriptInfo.getClassNameOrFile());
   }

   public void setFile (File file) {
      setFileName (file.getAbsolutePath());
   }

   public void setFileName (String fileName) {
      if (!fileName.equals (getFileName())) {
         String[] args = ModelScriptInfo.splitArgs (myArgs);
         myScriptInfo =
            new ModelScriptInfo (InfoType.SCRIPT, fileName, getTitle(), args);
      }
   }

   // public Range getFileNameRange() {
   //    return new FileNameRange ();
   // }

   public String getArgs() {
      return myArgs;
   }

   public void setArgs (String args) {
      if (!args.equals (myArgs)) {
         String[] splitArgs = ModelScriptInfo.splitArgs (args);
         myScriptInfo =
            new ModelScriptInfo (InfoType.SCRIPT, getFileName(), getTitle(), splitArgs);
         myArgs = args;
      }
   }

   @Override
   public MenuType getType() {
      return MenuType.SCRIPT;
   }

   @Override
   public boolean equals (MenuNode node) {

      if (! (node instanceof ScriptEntry)) {
         return false;
      }
      if (!super.equals (node)) {
         return false;
      }
      ScriptEntry other = (ScriptEntry)node;
      if (! (myScriptInfo.equals (other.myScriptInfo))) {
         return false;
      }
      return true;
   }   
   
   @Override
   public String toString() {
      return myScriptInfo.toString();
   }

   public void set (ScriptEntry modelEntry) {
      super.set (modelEntry);
      setFileName (modelEntry.getFileName());
      setArgs (modelEntry.getArgs());
   }

   public JMenuItem getComponent() {
      return myMenuItem;
   }

   public Component updateComponent (ModelScriptMenu modelMenu) {

      if (myMenuItem == null) {
         myMenuItem = new JMenuItem (getTitle());
         myActionForwarder = 
            modelMenu.setScriptMenuAction (myMenuItem, myScriptInfo);
         setLabelAttributes (myMenuItem);
      }
      else {
         if (myActionForwarder.mi != myScriptInfo) {
            // model has changed
            myMenuItem.removeActionListener (myActionForwarder);
            myActionForwarder =
               modelMenu.setScriptMenuAction (myMenuItem, myScriptInfo);
         }
         updateLabelAttributes (myMenuItem);
      }
      return myMenuItem;   
   }

}
