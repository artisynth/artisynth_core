package artisynth.core.modelmenu;

import java.awt.Font;
import java.util.*;
import java.io.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import artisynth.core.workspace.RootModel;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.driver.RootModelManager;
import artisynth.core.modelmenu.ModelScriptMenuParser.MenuType;

import maspack.util.*;
import maspack.properties.*;

public class ScriptFolderEntry extends MenuEntry {

   protected boolean myChildNodesValid;

   private File myFile;

   ScriptFolderEntry() {
      super();
      setExpandable (true);
   }

   public ScriptFolderEntry (String title) {
      super (title);
      setExpandable (true);
   }

   public File getFile() {
      return myFile;
   }

   public void setFile (File dir) {
      if (dir != myFile) {
         myFile = dir;
         invalidateChildNodes();
      }
   }

   @Override
   public void setFont (Font font) {
      if (myFont != font) {
         updateFontSpec (font);
         recursivelySetFont (this, font);
      }
   }

  @Override
   public boolean equals (MenuNode node) {

      if (! (node instanceof ScriptFolderEntry)) {
         return false;
      }
      if (!super.equals (node)) {
         return false;
      }
      ScriptFolderEntry other = (ScriptFolderEntry)node;
      if ((myFile == null) != (other.myFile == null)) {
         return false;
      }
      else if (myFile != null && !myFile.equals (other.myFile)) {
         return false;
      }
      return true;
   }

   static Pattern getInternalScriptNamePattern() {
      Pattern pattern = null;
      try {
         pattern = Pattern.compile (".*ArtisynthScript:\\s*\"([^\"]+)\".*");
      }
      catch (Exception e) {
         throw new InternalErrorException (
            "Script matching pattern won't compile: " + e);
      }
      return pattern;
   }

   static String getInternallyDefinedScriptName (File file, Pattern pattern) {
      if (!file.canRead()) {
         return null;
      }
      
      BufferedReader reader = null;
      try {
         reader = new BufferedReader (new FileReader (file));
         String firstLine = reader.readLine();
         Matcher matcher = pattern.matcher (firstLine);
         if (matcher.matches()) {
            return matcher.group(1);
         }
         else {
            return null;
         }
      }
      catch (Exception e) {
         return null;
      } finally {
         try {
            if (reader != null) {
               reader.close();
            }
         }
         catch (Exception e) {
            // ignore
         }
      }
   }
   
   public void updateChildNodes () {
      if (!myChildNodesValid) {
         createChildNodes ();
      }
   }

   /**
    * Expands the nodes for this scriptFolder entry given all of its attributes.
    */
   public void createChildNodes () {

      clearChildren();
      myMenuValid = false;

      if (myFile != null && myFile.isDirectory()) { // paranoid
         ArrayList<File> dirList = new ArrayList<>();
         dirList.add (myFile);         
         File[] files = ArtisynthPath.findFilesMatching (dirList, ".*\\.(py|jy)");
         Pattern pattern = getInternalScriptNamePattern();
         for (int i=0; i<files.length; i++) {
            String name = getInternallyDefinedScriptName (files[i], pattern);
            if (name != null) {
               ScriptEntry script =
                  new ScriptEntry (files[i].getAbsolutePath(), name, null);
               addChild (script);
            }
         }
         // sort the menu?
         //sortMenu (this, new MenuCompareByNameButDemosLast());
      }

      // set fonts for all entries
      Font font = getFont();
      if (font != null) {
         for (MenuNode child : getChildren()) {
            child.setFont (font);
         }
      }

      myChildNodesValid = true;
   }

   public void set (ScriptFolderEntry dentry) {
      super.set (dentry);
      setFile (dentry.getFile());
   }

   public void invalidateChildNodes () {
      myChildNodesValid = false;
      myMenuValid = false;
   }

   public boolean childNodesAreValid () {
      return myChildNodesValid;
   }


}
