package artisynth.core.modelmenu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import maspack.util.InternalErrorException;
import artisynth.core.driver.ModelScriptInfo;
import artisynth.core.driver.RootModelManager;
import artisynth.core.util.ArtisynthPath;

/**
 * Class that generates a model menu from an underlying model tree structure,
 * that can be either read from a file or adjusted within an editor. In
 * general, it is assumed that one of these will be created per ArtiSynth
 * application instance.
  */
public class ModelScriptMenu {

   /**
    * Describes whether this menu is for models or scripts.
    */
   public enum Type {
      MODEL,
      SCRIPT;

      public String mixedCaseName() {
         String name = toString();
         return name.charAt(0) + name.substring(1).toLowerCase();
      }
      
      public String lowerCaseName() {
         return toString().toLowerCase();
      }
   };

   Type myType = Type.MODEL;
   JMenu myJMenu;
   MenuEntry myMenuTree;
   ModelScriptActionListener myActionListener;
   RootModelManager myRootModelManager;
   boolean myIsSimple;

   public ModelScriptMenu (
      Type type, JMenu menu, 
      ModelScriptActionListener actionListener, RootModelManager rootModelManager) {
      myType = type;
      myJMenu = menu;
      myActionListener = actionListener;
      myRootModelManager = rootModelManager;
      setMenuTree (new MenuEntry()); // create initial empty menu
   }

   public Type getType() {
      return myType;
   }
   
   public MenuEntry getMenuTree() {
      return myMenuTree;
   }
   
   RootModelManager getRootModelManager() {
      return myRootModelManager;
   }
   
   public boolean isSimple() {
      return myIsSimple;
   }
   
   public void setMenuTree (MenuEntry tree) {
      myMenuTree = tree;
      myMenuTree.setJMenu (myJMenu);
      buildMenu ();      
   }
   
   public void updatePackageEntries() {
      // run this in the GUI thread since we may be changing menu items
      try {
         if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeAndWait (new Runnable() {
               @Override
               public void run() {
                  recursivelyUpdatePackageEntries (myMenuTree); 
                  recursivelyUpdateComponents (myMenuTree);
               }
            });
         }
         else {
            recursivelyUpdatePackageEntries (myMenuTree);
            recursivelyUpdateComponents (myMenuTree);
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   private void recursivelyUpdatePackageEntries (MenuEntry entry) {
      for (MenuNode child : entry.getChildren()) {
         if (child instanceof PackageEntry) {
            PackageEntry pentry = (PackageEntry)child;
            if (myRootModelManager.cacheDiffersFromMain (
                   pentry.getPackageName())) {
               pentry.invalidateChildNodes();
               pentry.createChildNodes (myRootModelManager, /*useCache=*/false);
               pentry.invalidateMenusToRoot();
            }
         }
         if (child instanceof MenuEntry) {
            recursivelyUpdatePackageEntries ((MenuEntry)child);
         }
      }
   }

   private void recursivelyUpdateComponents (MenuEntry entry) {
      if (!entry.isMenuValid()) {
         entry.updateComponent (this);
      }
      else {
         for (MenuNode child : entry.getExpandedChildren()) {
            if (child instanceof MenuEntry) {
               recursivelyUpdateComponents ((MenuEntry)child);
            }
         }
      }
   }

   public void write (File file) {
      ModelScriptMenuParser parser = new ModelScriptMenuParser();
      parser.writeXML (file, getType(), myMenuTree);
   }
   
//   public MenuEntry readMenuTree (File file) throws 
//      IOException, ParserConfigurationException, SAXException {
//      return ModelMenuParser.parseXML (file, getRootModelManager());
//   }
//   
   public void readSimpleMenu (File file) throws 
      IOException, ParserConfigurationException, SAXException {
      ModelScriptMenuParser parser = new ModelScriptMenuParser();
      MenuEntry menuTree = parser.parseSimpleFile (file);
      myIsSimple = true;
      setMenuTree (menuTree);
   }
 
   public void read (File file) throws 
      IOException, ParserConfigurationException, SAXException {
      ModelScriptMenuParser parser = new ModelScriptMenuParser();
      MenuEntry menuTree = parser.parseXML (
         file, getType(), getRootModelManager());
      myIsSimple = false;
      setMenuTree (menuTree);
   }

   protected void buildMenu () {
      try {
         if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeAndWait (new Runnable() {
               @Override
               public void run() {
                  myMenuTree.buildJMenu (ModelScriptMenu.this);
               }
            });
         }
         else {
            myMenuTree.buildJMenu (this);
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   private String getFileName (String filePath) {
      int sidx = filePath.lastIndexOf (File.separatorChar);
      if (sidx != -1) {
         filePath = filePath.substring (sidx+1);
      }
      return filePath;
   }

   protected ModelScriptActionForwarder setModelMenuAction (
      JMenuItem item, ModelScriptInfo mi) {
      ModelScriptActionForwarder actionForwarder = null;
      if (myActionListener != null) {
         actionForwarder =
            new ModelScriptActionForwarder (myActionListener, "loadModel", mi);
         item.addActionListener (actionForwarder);
      }
      item.setToolTipText (mi.getClassNameOrFile());
      return actionForwarder;
   }

   protected ModelScriptActionForwarder setScriptMenuAction (
      JMenuItem item, ModelScriptInfo mi) {
      ModelScriptActionForwarder actionForwarder = null;
      if (myActionListener != null) {
         actionForwarder =
            new ModelScriptActionForwarder (myActionListener, "runScript", mi);
         item.addActionListener (actionForwarder);
      }
      item.setToolTipText (getFileName(mi.getClassNameOrFile()));
      return actionForwarder;
   }

   public File getDefaultMenuFile() {
      switch (myType) {
         case MODEL: {
            return ArtisynthPath.getConfigFile ("settings/modelMenu.xml");
         }
         case SCRIPT: {
            return ArtisynthPath.getConfigFile ("settings/scriptMenu.xml");
         }
         default: {
            throw new InternalErrorException ("Unknown menu type: " + myType);
         }
      }
   }

   public File getOrCreateDefaultFile() {
      File menuFile = getDefaultMenuFile();
      if (menuFile.exists()) {
         if (!menuFile.canRead()) {
            System.out.println (
               "WARNING: model menu file "+menuFile+" not readable");
            menuFile = null;
         }
      }
      else {
         createDefaultFiles();
         if (!menuFile.exists()) {
            // something went wrong - couldn't create the files
            menuFile = null;
         }
      }            
      return menuFile;
   }

   public void createDefaultFiles () {
      File dir = ArtisynthPath.getConfigFile ("settings");
      if (dir != null) {
         String[] fileNames;
         switch (myType) {
            case MODEL: {
               fileNames = new String[] {
                  "modelMenu.xml", "demoModels.txt", "mainModels.txt" };
               break;
            }
            case SCRIPT: {
               fileNames = new String[] { "scriptMenu.xml" };
               break;
            }
            default: {
               throw new InternalErrorException ("Unknown menu type: " + myType);
            } 
         }
         for (String fileName : fileNames) {
            File source = ArtisynthPath.getHomeRelativeFile (fileName, ".");
            if (source.canRead()) {
               try {
                  File target = new File (dir, fileName);            
                  Files.copy (
                     source.toPath(), target.toPath(),
                     StandardCopyOption.REPLACE_EXISTING);
                  System.out.println (
                     "Copied menu file "+fileName+" to "+dir);
               }
               catch (IOException e) {
                  System.out.println (
                     "WARNING: unable to copy "+source+" to "+dir+": "+e);
               }
            }
            else {
               System.out.println (
                  "WARNING: unable to read menu file "+source);
            }
         }
      }
   }
}
