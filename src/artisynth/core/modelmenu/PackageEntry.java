package artisynth.core.modelmenu;

import java.awt.Font;
import java.util.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;

import artisynth.core.workspace.RootModel;
import artisynth.core.driver.RootModelManager;
import artisynth.core.modelmenu.ModelScriptMenuParser.MenuType;

import maspack.util.*;
import maspack.properties.*;

public class PackageEntry extends MenuEntry {

   /**
    * Checks that a package name describes a real package
    */
   static class PackageNameRange extends RangeBase {

      public boolean isValid (Object obj, StringHolder errMsg) {
         if (obj == null) {
            return true;
         }
         else if (obj instanceof String) {
            String name = (String)obj;
            if (name.length() == 0) {
               return true;
            }
            Package pack = null;
            try {
               pack = Package.getPackage (name);
            }
            catch (Exception e) {
            }
            if (pack == null) {
               if (errMsg != null) {
                  errMsg.value = "'"+name+"' is not a known package";
               }
               return false;
            }
            return true;
         }
         return false;
      }
   }

   protected boolean myChildNodesValid;

   public static final String DEFAULT_PACKAGE_NAME = "";
   private String myPackageName = new String(DEFAULT_PACKAGE_NAME);

   public static final int DEFAULT_COMPACT = 0;
   private int myCompact = DEFAULT_COMPACT;

   public static final boolean DEFAULT_FLAT_VIEW = false;
   private boolean myFlatView = DEFAULT_FLAT_VIEW;

   public static PropertyList myProps =
      new PropertyList (PackageEntry.class, MenuEntry.class);

   static {
      myProps.add (
         "packageName", "Name of the package containing the model classes",
         DEFAULT_PACKAGE_NAME);
      myProps.add (
         "compact",
         "Now much to compact the menu (0-2), with 0 being the least ",
         DEFAULT_COMPACT);
      myProps.add (
         "flatView", "Arrange all classes in a single flat view", 
         DEFAULT_FLAT_VIEW);
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   PackageEntry() {
      super();
      setExpandable (true);
   }

   public PackageEntry (String title) {
      super (title);
      setExpandable (true);
   }

   public String getPackageName() {
      return myPackageName;
   }

   public void setPackageName (String name) {
      if (!stringEquals (myPackageName, name)) {
         myPackageName = name;
         invalidateChildNodes();
      }
   }

   public Range getPackageNameRange() {
      return new PackageNameRange();
   }

   public void setCompact (int compact) {
      myCompact = compact;
   }

   public int getCompact() {
      return myCompact;
   }

   public void setFlatView (boolean flat) {
      if (myFlatView != flat) {
         myFlatView = flat;
         invalidateChildNodes();
      }
   }

   public boolean getFlatView() {
      return myFlatView;
   }

   @Override
   public void setMaxRows (int max) {
      if (myMaxRows != max) {
         recursivelySetMaxRows (this, max);
      }
   }

   @Override
   public void setScrolling (boolean scrolling) {
      if (myScrolling != scrolling) {
         recursivelySetScrolling (this, scrolling);
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

      if (! (node instanceof PackageEntry)) {
         return false;
      }
      if (!super.equals (node)) {
         return false;
      }
      PackageEntry other = (PackageEntry)node;

      if (!myPackageName.equals (other.myPackageName)) {
         return false;
      }
      if (myFlatView != other.myFlatView) {
         return false;
      }
      if (myCompact != other.myCompact) {
         return false;
      }
      return true;
   }
   
   public void updateChildNodes (RootModelManager rmm) {
      if (!myChildNodesValid) {
         createChildNodes(rmm, /*useCache=*/false);
      }
   }

   /**
    * Expands the nodes for this package entry given all of its attributes.
    * @param useCache TODO
    */
   public void createChildNodes (RootModelManager rmm, boolean useCache) {

      clearChildren();
      myMenuValid = false;

      String pkgName = getPackageName();

      // System.out.println ("Searching for classes of type '" + base.getName()
      // + "' in package '" + pkg + "'");

      ArrayList<String> clsList;      if (rmm != null) {
         int flags = RootModelManager.RECURSIVE;
         if (useCache) {
            flags |= RootModelManager.USE_CACHE;
         }
         clsList = rmm.findModels (pkgName, flags);
      }
      else {
         clsList = ClassFinder.findClassNames (
            pkgName, RootModel.class);
      }

      String pkg = pkgName;
      if (!pkg.equals ("") && !pkg.endsWith (".")) {
         pkg = pkg + "."; // add a dot to the prefix
      }

      // if the view is flat, simply list packages
      if (getFlatView()) {

         // finds greatest common prefix of class list (used for compacting)
         String prefix = getPrefix (new ArrayList<String> (clsList));

         for (String cls : clsList) {
            String title = cls;
            if (title.startsWith (pkg)) {
               title = cls.substring (pkg.length()); // remove supplied package
               // from title
            }
            if (getCompact() > 1) {
               String[] titleArray = title.split ("\\.");
               title = titleArray[titleArray.length - 1];
            } 
            else if (getCompact() == 1) {
               // remove prefix
               title = cls.substring (prefix.length());
            }
            ModelEntry model = new ModelEntry (cls, title, null);
            addChild (model);
         }
         sortMenu (this, new MenuCompareByNameButDemosLast());

         // default to hierarchical
      }
      else {
         getPackageMenuTree (this, clsList, pkg);
         sortMenu (this, new MenuCompareByNameButDemosLast()); // sort first
         insertDividersInMenu (this); // then insert dividers
      }

      Font font = getFont();
      if (font != null) {
         // go through and set font for all items in the menu
         for (MenuNode child : getChildren()) {
            recursivelySetFont (child, font);
         }
      }

      myChildNodesValid = true;
   }

   private void getPackageMenuTree (
      PackageEntry rootMenu, ArrayList<String> clsList, String pkg) {

      int compact = rootMenu.getCompact();
      boolean scrolling = rootMenu.isScrolling();
      int maxRows = rootMenu.getMaxRows();

      MenuEntry menu = rootMenu;
      String[] cursecs = new String[1];

      for (int i = 0; i < clsList.size(); i++) {
         String cls = clsList.get (i);
         String title = cls;
         if (title.startsWith (pkg)) {
            title = title.substring (pkg.length());
         }
         String[] newsecs = title.split ("\\."); // split at periods
         int clsIdx = newsecs.length-1; // index of the class section
         // compute the index at which the *package* components of cursecs
         // and newsecs differ
         int idx = 0;
         while (idx<Math.min (cursecs.length-1, newsecs.length-1)) {
            if (!cursecs[idx].equals (newsecs[idx])) {
               break;
            }
            idx++;
         }
         if (cursecs.length != newsecs.length || idx != clsIdx) {
            // Need to adjust the menu. First go up the hierarchy to reach the
            // idx point ...
            for (int j=0; j<cursecs.length-1-idx; j++) {
               menu = menu.getParent();
            }
            // and then chain on extra nodes as needed:
            for (int j=idx; j<clsIdx; j++) {
               MenuEntry newMenu = new MenuEntry (newsecs[j]);
               newMenu.setScrolling (scrolling);
               newMenu.setMaxRows (maxRows);
               menu.addChild (newMenu);
               menu = newMenu;
            }
         }
         menu.addChild (new ModelEntry (cls, newsecs[clsIdx], null));
         cursecs = newsecs;
      }
   }

   public void set (PackageEntry pentry) {
      super.set (pentry);
      setPackageName (pentry.getPackageName());
      setCompact (pentry.getCompact());
      setFlatView (pentry.getFlatView());
   }

   public void invalidateChildNodes () {
      myChildNodesValid = false;
      myMenuValid = false;
   }

   public boolean childNodesAreValid () {
      return myChildNodesValid;
   }


}
