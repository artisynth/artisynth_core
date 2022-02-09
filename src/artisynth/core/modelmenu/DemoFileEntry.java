package artisynth.core.modelmenu;

import java.awt.Font;
import java.io.*;
import java.util.*;
import java.lang.reflect.Modifier;

import artisynth.core.workspace.RootModel;

import artisynth.core.util.AliasTable;
import maspack.util.ClassFinder;

public class DemoFileEntry extends MenuEntry {

   private File myFile;

   DemoFileEntry() {
      super();
      setExpandable (true);
   }

   public DemoFileEntry (String title) {
      super (title);
      setExpandable (true);
   }

   public void setFile (File file) {
      if (myFile != file) {
         myFile = file;
      }
   }

   public File getFile() {
      return myFile;
   }

   // public void setArgs (String[] args) {
   //    myArgs = args;
   // }

   // public String[] getArgs() {
   //    return myArgs;
   // }

   @Override
   public void setFont (Font font) {
      if (myFont != font) {
         updateFontSpec (font);
         recursivelySetFont (this, font);
      }
   }

   @Override
   public boolean equals (MenuNode node) {

      if (! (node instanceof DemoFileEntry)) {
         return false;
      }
      if (!super.equals (node)) {
         return false;
      }
      DemoFileEntry other = (DemoFileEntry)node;
      if ((myFile == null) != (other.myFile == null)) {
         return false;
      }
      else if (myFile != null && !myFile.equals (other.myFile)) {
         return false;
      }
      return true;
   }

   AliasTable readAliasTable (File file) {
      AliasTable table = null;
      try {
         table = new AliasTable (file);
      }
      catch (Exception e) {
         System.out.println (
            "WARNING: error reading demoFile: '"+getFile()+"'");
         System.out.println (e.getMessage());
      }
      return table;
   }      

   /**
    * Create the node for this demo file entry.
    */
   public void createChildNodes (AliasTable table) {

      clearChildren();
      myMenuValid = false;

      // remove entries that are not valid models
      Iterator<Map.Entry<String,String>> li = table.entrySet().iterator();
      while (li.hasNext()) {
         Map.Entry<String,String> entry = li.next();
         try {
            Class<?> clazz = ClassFinder.forName (entry.getValue(), false);
            if (!RootModel.class.isAssignableFrom (clazz) ||
                Modifier.isAbstract (clazz.getModifiers()) ||
                ModelScriptMenuParser.omitFromMenu (clazz)) {
               li.remove();
            }
         }
         catch (Exception e) {
            // if there's a problem finding the class, just remove the entry
            li.remove();
         }
      }

      String[] aliases = table.getAliases();
      ArrayList<ModelEntry> models = new ArrayList<ModelEntry>();
      for (int i = 0; i < aliases.length; i++) {
         models.add (
            new ModelEntry (table.getName (aliases[i]), aliases[i], null));
      }
      
      // set fonts for all entries
      Font font = getFont();
      if (font != null) {
         for (MenuNode model : models) {
            model.setFont (font);
         }
      }

      for (ModelEntry model : models) {
         addChild (model);
      }
   }

   public void set (DemoFileEntry dentry) {
      super.set (dentry);
      setFile (dentry.getFile());
   }

}
