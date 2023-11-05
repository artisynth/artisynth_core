package artisynth.core.modelmenu;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.BoxLayout;
import javax.swing.plaf.basic.DefaultMenuLayout;

import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;

import artisynth.core.driver.VerticalGridLayout;
import artisynth.core.modelmenu.ModelScriptMenuParser.MenuType;
import maspack.properties.*;
import maspack.util.*;

public class MenuEntry extends MenuNode {

   protected static final boolean DEFAULT_SCROLLING = false;
   protected boolean myScrolling = DEFAULT_SCROLLING;

   protected static final int DEFAULT_MAX_ROWS = 30;
   protected int myMaxRows = DEFAULT_MAX_ROWS;

   private boolean expandable;

   protected JMenu myMenu;
   protected boolean myMenuIsExternal;
   protected JMenuScroller myMenuScroller;
   protected boolean myMenuValid;

   protected ArrayList<MenuNode> myChildren;
  
   public static PropertyList myProps =
      new PropertyList (MenuEntry.class, MenuNode.class);

   static {
      myProps.add (
         "scrolling isScrolling",
         "Create a scrolling menu if it contains more than 'maxRows' entries",
         DEFAULT_SCROLLING);
      myProps.add (
         "maxRows",
         "Number of entries to engage scrolling if 'scrolling' is true",
         DEFAULT_MAX_ROWS);
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MenuEntry() {
      super();
      myScrolling = false;
      myMaxRows = DEFAULT_MAX_ROWS;
      myChildren = new ArrayList<>();
   }
   
   public MenuEntry (String title) {
      super (title);
      myScrolling = false;
      myMaxRows = DEFAULT_MAX_ROWS;
      myChildren = new ArrayList<>();
   }
   
   public void setMaxRows (int max) {
      if (myMaxRows != max) {
         myMaxRows = max;
         myMenuValid = false;
      }
   }
   
   protected void recursivelySetMaxRows (MenuEntry entry, int maxRows) {
      entry.myMaxRows = maxRows;
      entry.myMenuValid = false;
      for (MenuNode child : entry.myChildren) {
         if (child instanceof MenuEntry) {
            recursivelySetMaxRows ((MenuEntry)child, maxRows);
         }
      }
   }
   
   public int getMaxRows() {
      return myMaxRows;
   }
   
   public void setScrolling (boolean set) {
      if (myScrolling != set) {
         myScrolling = set;
         myMenuValid = false;
      }
   }

   protected void recursivelySetScrolling (MenuEntry entry, boolean scrolling) {
      entry.myScrolling = scrolling;
      entry.myMenuValid = false;
      for (MenuNode child : entry.myChildren) {
         if (child instanceof MenuEntry) {
            recursivelySetScrolling ((MenuEntry)child, scrolling);
         }
      }
   }
   
   public boolean isScrolling() {
      return myScrolling;
   }
   
   public void setExpandable (boolean set) {
      this.expandable = set;
   }
   
   public boolean isExpandable() {
      return expandable;
   }

   protected void recursivelySetFont (MenuNode node, Font font) {
      node.myFont = font;
      if (node instanceof MenuEntry) {
         MenuEntry menuEntry = (MenuEntry)node;
         menuEntry.invalidateMenu();
         for (MenuNode child : menuEntry.myChildren) {
            recursivelySetFont (child, font);
         }
      }
   }
   
   @Override
   public boolean equals (MenuNode node) {

      if (! (node instanceof MenuEntry)) {
         return false;
      }
      if (!super.equals (node)) {
         return false;
      }
      MenuEntry other = (MenuEntry)node;
      if (myScrolling != other.myScrolling) {
         return false;
      }
      if (myMaxRows != other.myMaxRows) {
         return false;
      }
      if (expandable != other.expandable) {
         return false;
      }
      return true;
   }
   
   @Override
   public int compareTo (MenuNode o) {
      int cmp =  super.compareTo (o);
      
      if (cmp != 0) {
         return cmp;
      }
      
      MenuEntry other = (MenuEntry)o;
      
      cmp = Integer.compare (myMaxRows, other.myMaxRows);
      if (cmp != 0) {
         return cmp;
      }
      
      cmp = Boolean.compare (myScrolling, other.myScrolling);
      return cmp;
   }

   public void set (MenuEntry menuEntry) {
      super.set (menuEntry);
      setScrolling (menuEntry.isScrolling());
      setMaxRows (menuEntry.getMaxRows());
   }

   public JMenu getComponent() {
      return myMenu;
   }

   public void invalidateMenu () {
      myMenuValid = false;
   }

   /**
    * Invalidate the menu for this MenuEntry, along with the menus of all its
    * ancestors.
    */
   public void invalidateMenusToRoot () { 
      invalidateMenu();
      MenuEntry ancestor = this;
      while (ancestor.getParent() != null) {
         ancestor = ancestor.getParent();
         ancestor.invalidateMenu();
      }
   }


   public boolean isMenuValid() {
      return myMenuValid;
   }

   public Component updateComponent (ModelScriptMenu modelMenu) {
      if (myMenu == null) {
         myMenu = new JMenu();
         setLabelAttributes (myMenu);
      }
      else if (!myMenuIsExternal) {
         updateLabelAttributes (myMenu);
      }
      if (!myMenuValid) {
         buildJMenu (modelMenu);
      }
      return myMenu;
   }

   boolean expandEntry () {
      return (isExpandable() && titleIsEmpty() && getIcon() == null);
   }

   boolean isMenuEmpty (MenuNode node) {

      switch (node.getType()) {
         case DIVIDER:
            return false;
         case LABEL:
            return false;
         case MODEL:
            return false;
         case SCRIPT:
            return false;
         case MENU: {
            MenuEntry menuEntry = (MenuEntry)node;
            for (MenuNode child : menuEntry.getChildren()) {
               if (!isMenuEmpty (child)) {
                  return false;
               }
            }
            return true;
         }
         default:
            throw new InternalErrorException (
               "Unimplemented node type " + node.getType());
      }
   }

   // Recursively find if there are any model entries under a node.
   // Faster than actually counting entries.
   private boolean hasModelEntries (MenuNode node) {

      switch (node.getType()) {
         case MENU: {
            MenuEntry menuEntry = (MenuEntry)node;
            for (MenuNode child : menuEntry.getChildren()) {
               boolean hasModels = hasModelEntries (child);
               if (hasModels) {
                  return true;
               }
            }
            break;
         }
         case MODEL: {
            if (node instanceof ModelEntry) {
               return true;
            }
            break;
         }
         default:
            break;
      }
      return false;
   }

   public void setJMenu (JMenu menu) {
      myMenu = menu;
      myMenuIsExternal = true;
   }

   private void addSpecialItem (
      ModelScriptMenu modelMenu, String title, String cmd, String toolTip) {

      if (cmd == null) {
         cmd = title;
      }
      JMenuItem item = new JMenuItem (title);
      item.setToolTipText (toolTip);
      if (modelMenu.myActionListener != null) {
         // set model action listener to invoke the model editor
         item.addActionListener (
            new ModelScriptActionForwarder (
               modelMenu.myActionListener, cmd, null));
      }
      myMenu.add (item);
   }

   public void buildJMenu (ModelScriptMenu modelMenu) {
      // clear menu items and restore layout
      myMenu.removeAll();
      if (myMenu.getPopupMenu().getLayout() instanceof VerticalGridLayout) {
         myMenu.getPopupMenu().setLayout (
            new DefaultMenuLayout(myMenu.getPopupMenu(), BoxLayout.PAGE_AXIS));
      }
      if (myMenuScroller != null) {
         myMenuScroller.dispose();
         myMenuScroller = null;
      }
      ArrayList<MenuNode> children = getExpandedChildren();
      if (children.size() > getMaxRows()) {
         if (isScrolling()) {
            myMenuScroller =
               JMenuScroller.setScrollerFor (myMenu, getMaxRows());   
         }
         else {
            VerticalGridLayout menuGrid =
               new VerticalGridLayout (getMaxRows(), 0);
            myMenu.getPopupMenu().setLayout (menuGrid);
         }
      }
      // loop through all the children
      for (MenuNode child : children) {
         if (!isMenuEmpty (child)) {
            // recursively builds myMenu
            child.updateComponent (modelMenu);
            myMenu.add (child.getComponent());
         }
      }
      // special case for top menu - add a command to invoke the editor
      if (getParent() == null) {
         myMenu.add (new JSeparator());

         if (modelMenu.getType() == ModelScriptMenu.Type.MODEL) {
            addSpecialItem (
               modelMenu, "Reload model", null,
               "Reload the most recently loaded model");
            myMenu.add (new JMenu("Load recent"));
            addSpecialItem (
               modelMenu, "Load from class ...", null,
               "Load a model by specifying its RootModel class");
            if (!modelMenu.isSimple()) {
               addSpecialItem (
                  modelMenu, "Edit menu ...",
                  "Edit MODEL menu", "Customize this model menu");
            }
         }
         else if (modelMenu.getType() == ModelScriptMenu.Type.SCRIPT) {
            myMenu.add (new JMenu("Run recent"));
            addSpecialItem (
               modelMenu, "Run script ...", null,
               "Run a script by specifying the script file");
            if (!modelMenu.isSimple()) {
               addSpecialItem (
                  modelMenu, "Edit menu ...",
                  "Edit SCRIPT menu", "Customize this model menu");
            }
         }
         else {
            throw new InternalErrorException (
               "Unknown menu type " + modelMenu.getType());
         }
      }
      myMenuValid = true;
   }

   public int numChildren() {
      return myChildren.size();
   }
   
   public MenuNode getChild (int idx) {
      return myChildren.get (idx);
   }
   
   public ArrayList<MenuNode> getChildren() {
      return myChildren;
   }
   
   public void addChild (MenuNode node) {
      addChild (myChildren.size(), node);
   }
   
   public void addChild (int idx, MenuNode node) {
      myChildren.add (idx, node);
      node.myIndex = idx;
      node.parent = this;
      // reindex trailing children
      for (int i=idx+1; i<myChildren.size(); i++) {
         myChildren.get(i).myIndex = i;
      }     
   }
   
//   public int indexOfChild (MenuNode node) {
//      return myChildren.indexOf (node);
//   }
//   
   public boolean removeChild (MenuNode node) {
      int idx = node.myIndex;
      if (myChildren.get(idx) != node) {
         if (myChildren.contains (node)) {
            throw new InternalErrorException (
               "Node "+node+" is not at index "+idx);
         }
         else {
            return false;
         }
      }
      myChildren.remove (idx);
      // reindex remaining children
      for (int i=idx; i<myChildren.size(); i++) {
         myChildren.get(i).myIndex = i;
      }
      node.parent = null;
      return true;
   }
   
   public void shiftChildren (int minIdx, int maxIdx, int shift) {
      if (shift < -1 || shift > 1) {
         throw new IllegalArgumentException ("shift must be -1, 0, or 1");
      }
      if (shift == 1 && maxIdx+1 < myChildren.size()) {
         MenuNode last = myChildren.get(maxIdx+1);
         for (int i=minIdx; i<=maxIdx+1; i++) {
            MenuNode node = myChildren.get(i);
            myChildren.set (i, last);
            last.myIndex = i;
            last = node;
         }
      }
      else if (shift == -1 && minIdx-1 >= 0) {
         MenuNode last = myChildren.get(minIdx-1);
         for (int i=maxIdx; i>=minIdx-1; i--) {
            MenuNode node = myChildren.get(i);
            myChildren.set (i, last);
            last.myIndex = i;
            last = node;
         }
      }
   }
   
   public void moveChild (MenuNode node, int shift) {
      int oldIdx = node.myIndex;
      if (myChildren.get(oldIdx) != node) {
         if (node.parent != this) {
            throw new InternalErrorException (
               "Node "+node+" is not at index "+oldIdx);
         }
      }
      int newIdx = oldIdx+shift;
      if (newIdx >= 0 && newIdx < myChildren.size()) {
         if (shift > 0) {
            // shift child nodes in the range (oldIx+1, ... newIdx) down
            for (int idx=oldIdx; idx<newIdx; idx++) {
               MenuNode next = myChildren.get(idx+1);
               myChildren.set (idx, next);
               next.myIndex = idx;
            }
         }
         else if (shift < 0) {
            // shift child nodes in the range (newIdx, ... oldIx-1) up
            for (int idx=oldIdx; idx>newIdx; idx--) {
               MenuNode prev = myChildren.get(idx-1);
               myChildren.set (idx, prev);
               prev.myIndex = idx;
            }
         }
         // move node to newIdx
         myChildren.set (newIdx, node);
         node.myIndex = newIdx;
      }
   }
   
   public void clearChildren () {
      myChildren.clear();
   }
   
   /**
    * Return the children for this menu entry, expanding any menu entries
    * which are themselves expandable (and hence not visible to the
    * menu itself).
    */
   ArrayList<MenuNode> getExpandedChildren() {
      ArrayList<MenuNode> children = new ArrayList<>();
      for (MenuNode child : getChildren()) {
         if (child instanceof MenuEntry &&
             ((MenuEntry)child).expandEntry()) {
            children.addAll (((MenuEntry)child).getChildren());
         }
         else {
            children.add (child);
         }
      }   
      return children;
   }

   /**
    * Check that this node and its children are equal to another node and its
    * children.
    */
   public boolean treeEquals (MenuNode menuNode) {
      if (!equals (menuNode)) {
         return false;
      }
      MenuEntry menuEntry = (MenuEntry)menuNode;

      ArrayList<MenuNode> children = getExpandedChildren();
      ArrayList<MenuNode> otherChildren = menuEntry.getExpandedChildren();

      if (children.size() != otherChildren.size()) {
         return false;
      }

      for (int i=0; i<children.size(); i++) {
         MenuNode child = children.get(i);
         if (child instanceof MenuEntry) {
            MenuEntry childMenu = (MenuEntry)child;
            if (!childMenu.treeEquals (otherChildren.get(i))) {
               return false;
            }
         }
         else {
            if (!child.equals (otherChildren.get(i))) {
               return false;
            }
         }
      }
      return true;
   }

   // divides models from packages with a divider
   protected void insertDividersInMenu (MenuEntry root) {

      List<MenuNode> children = root.getChildren();
      MenuType typeA, typeB;

      int i = 1;
      while (i < children.size()) {

         // if different data type, insert a divider
         typeA = children.get (i).getType();
         typeB = children.get (i - 1).getType();
         if ( (typeA != typeB) && (typeA != MenuType.DIVIDER)
            && (typeB != MenuType.DIVIDER)) {
            root.addChild (i, new SeparatorEntry());
            i = i++; // skip divider
         }
         i++;
      }

      // recursively add dividers
      for (MenuNode child : children) {
         if (child.numChildren() > 0) {
            insertDividersInMenu ((MenuEntry)child);
         }
      }
   }

   /**
    * Given a list of strings, finds the greatest common prefix
    * 
    * @param array
    * input array of strings
    * @return the greatest common prefix
    */
   public String getPrefix (ArrayList<String> array) {
      String pre = "";
      int preLength = 0;

      if (array.size() == 0) {
         return "";
      }

      int maxLength = array.get (0).length();

      char c;

      // loop through each character to see if it matches in all supplied words
      for (int i = 0; i < maxLength; i++) {
         boolean diff = false;
         c = array.get (0).charAt (i);
         for (int j = 1; j < array.size(); j++) {
            if (array.get (j).charAt (i) != c) {
               diff = true;
               break;
            }
         }
         if (diff) {
            break;
         }
         preLength++;
      }
      pre = array.get (0).substring (0, preLength);
      return pre;
   }

   // sorts a menu, respecting dividers
   public void sortMenu (
      MenuEntry root, Comparator<MenuNode> comparer) {

      // sort children, respecting dividers
      int start = 0;
      int end = 0;

      // create array of children
      List<MenuNode> children = root.getChildren();

      while (start < children.size()) {

         end = start;

         // find divider or end of array
         while (children.get (end).getType() != MenuType.DIVIDER) {
            end++;
            if (end == root.numChildren()) {
               break;
            }
         }
         // sort the sublist
         Collections.sort (children.subList (start, end), comparer);
         // skip divider or/ end of list
         start = end + 1; // move to next item
      }

      if (root.numChildren() > 0) {
         for (MenuNode child : root.getChildren()) {
            if (child instanceof MenuEntry) {
               sortMenu ((MenuEntry)child, comparer);
            }
         }
      }
   }


}
