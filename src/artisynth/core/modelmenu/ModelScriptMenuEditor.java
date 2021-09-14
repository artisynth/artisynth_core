package artisynth.core.modelmenu;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.ArrayList;
import java.util.*;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.IconUIResource;
//import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.filechooser.*;

import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.editorManager.Command;
import artisynth.core.gui.editorManager.UndoManager;
import artisynth.core.gui.editorManager.UndoManager.CommandFilter;
import artisynth.core.util.AliasTable;
import artisynth.core.driver.RootModelManager;
import artisynth.core.modelmenu.ScriptDialogBase.ScriptDesc;
import artisynth.core.util.ArtisynthPath;
import maspack.util.PathFinder;
import maspack.util.FileSearchPath;
import maspack.util.GenericFileFilter;
import maspack.util.InternalErrorException;
import maspack.widgets.GuiUtils;
import maspack.widgets.OptionPanel;
import maspack.widgets.StringField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.widgets.WidgetDialog;

public class ModelScriptMenuEditor extends JFrame
   implements ActionListener, ValueChangeListener {
   
   static ImageIcon myPackageIcon;
   static ImageIcon myJavaFileIcon;
   static ImageIcon myScriptFileIcon;
   static ImageIcon myScriptFolderIcon;
   static ImageIcon myTextFileIcon;
   static Icon mySeparatorIcon;
   static Icon mySeparatorSelectedIcon;
   static Icon mySeparatorSelectedFocusedIcon;
   static ImageIcon myLabelIcon;
   static ImageIcon myTreeCollapsedIcon;
   static ImageIcon myTreeExpandedIcon;
   static ImageIcon myMenuIcon;
   static ImageIcon myUpIcon;
   static ImageIcon myDownIcon;
   static ImageIcon myDeleteIcon;

   JPanel myPanel;
   JScrollPane myTreeView;
   JTree myTree;
   MenuTreeModel myTreeModel;
   MenuEntry myRootMenu;
   LinkedHashSet<MenuNode> mySelectedNodes;
   ModelScriptMenu myModelMenu;
   UndoManager myUndoManager;
   File myDefaultFile; // file used by "Save" and "Reload"
   File myAlternateFile;   // file used by "Save to" and "Load from"

   JButton myAddPackageButton;
   JButton myAddModelsButton;
   JButton myAddMenuButton;
   JButton myAddLabelButton;
   JButton myAddSeparatorButton;
   JButton myAddDemoFileButton;
   JButton myAddScriptButton;
   JButton myAddScriptFolderButton;

   JButton myDeleteButton;
   JButton myEditButton;

   JButton myUpButton;
   JButton myDownButton;

   JButton mySaveButton;
   JButton myReloadButton;
   JButton mySaveToButton;
   JButton myLoadFromButton;
   
   JButton myResetButton;
   JButton myDoneButton;

   String myCurrentPackageName = "";
   File myLastDemoFile;
   File myLastScriptFolder;
   File myLastScriptFile;

   JFileChooser myMenuFileChooser;
   FileSearchPath myFileSearchPath;

   // keep track of modifications:

   boolean myContentsEdited = false; // editing changes (not undoable)
   int myStructureModCount = 0; // undoable changes: add/remove/shift

   private void updateSaveButton () {
      mySaveButton.setEnabled (
         myDefaultFile != null && myContentsEdited || myStructureModCount > 0);
   }

   private void addStructureModCount (int inc) {
      myStructureModCount += inc;
      updateSaveButton();
   }

   private void markContentsEdited() {
      myContentsEdited = true;
      updateSaveButton();
   }

   private void clearContentsModified() {
      myContentsEdited = false;
      myStructureModCount = 0;
      updateSaveButton();
   }

   private void clearUndoCommands() {
      myUndoManager.clearCommands (
         new CommandFilter() {
            public boolean accept (Command cmd) {
               return (cmd instanceof ModelMenuCommand);
            }
         });
   }

   private static final Color myTreeBackgroundColor =
      UIManager.getColor ("Tree.background");
   private static final Color myTreeSelectionBackgroundColor =
      UIManager.getColor ("Tree.selectionBackground");
   private static final Color myTreeSelectionBorderColor =
      UIManager.getColor ("Tree.selectionBorderColor");
   private static final Color myTreeForegroundColor =
      UIManager.getColor ("Tree.foreground");

   RootModelManager getRootModelManager() {
      return myModelMenu.getRootModelManager();
   }

   void maybeSaveRootModelCache() {
      myModelMenu.getRootModelManager().saveCacheIfModified();
   }

   class SeparatorIcon implements Icon {

      private static final int WIDTH = 48;
      private static final int HEIGHT = 16;

      Color myBackground;
      boolean myDrawBorder = false;

      SeparatorIcon (boolean selected, boolean hasFocus) {
         if (selected) {
            myBackground = myTreeSelectionBackgroundColor;
         }
         else {
            myBackground = myTreeBackgroundColor;
         }
         myDrawBorder = hasFocus;
      }

      public void paintIcon (Component c, Graphics g, int x, int y) {
         g.setColor (myBackground);
         g.fillRect (x, y, WIDTH, HEIGHT);

         if (myDrawBorder) {
            g.setColor (myTreeSelectionBorderColor);
            g.drawRect (x, y, WIDTH, HEIGHT-1);
         }

         g.setColor (myTreeForegroundColor);
         Graphics2D g2 = (Graphics2D)g;
         g2.setStroke (new BasicStroke(2));
         g2.draw (new Line2D.Float(x, y+HEIGHT/2, x+WIDTH-1, y+HEIGHT/2));
      }

      public int getIconWidth() {
         return WIDTH;
      }

      public int getIconHeight() {
         return HEIGHT;
      }
   }

   static boolean isStringEmpty (String str) {
      return str == null || str.length() == 0;
   }

   static String getSimpleName (String className) {
      int lastDot = className.lastIndexOf ('.');
      if (lastDot != -1) {
         return className.substring (lastDot+1);
      }
      else {
         return className;
      }
   }

   protected void initializeIcons() {
      if (myPackageIcon == null) {
         String iconDir = PathFinder.getSourceRelativePath (
            ControlPanel.class, "icon/");

         myPackageIcon = GuiUtils.loadIcon(iconDir+"package.png");
         myJavaFileIcon = GuiUtils.loadIcon(iconDir+"javaFile.png");
         myTextFileIcon = GuiUtils.loadIcon(iconDir+"textFile.png");
         myMenuIcon = GuiUtils.loadIcon(iconDir+"menu.png");
         myScriptFolderIcon = GuiUtils.loadIcon(iconDir+"folder.png");
         myScriptFileIcon = GuiUtils.loadIcon(iconDir+"scriptFile.png");

         mySeparatorIcon =
            new SeparatorIcon(/*selected=*/false, /*focused=*/false);
         mySeparatorSelectedIcon =
            new SeparatorIcon(/*selected=*/true, /*focused=*/false);
         mySeparatorSelectedFocusedIcon =
            new SeparatorIcon(/*selected=*/true, /*focused=*/true);
         myLabelIcon = GuiUtils.loadIcon(iconDir+"squareLabel.png");

         myTreeCollapsedIcon = GuiUtils.loadIcon(iconDir+"treeCollapsed.png");
         myTreeExpandedIcon = GuiUtils.loadIcon(iconDir+"treeExpanded.png");

         myUpIcon = GuiUtils.loadIcon (iconDir + "moveUpArrow.png");
         myDownIcon = GuiUtils.loadIcon (iconDir + "moveDownArrow.png");
         myDeleteIcon = GuiUtils.loadIcon (iconDir + "xcross.png");
      }
   }

   protected void addMenuItem (JPopupMenu menu, String cmd) {
      JMenuItem item = new JMenuItem (cmd);
      item.addActionListener (this);
      menu.add (item);      
   }

   protected void displayContextMenu (MouseEvent e) {
      TreePath[] paths = myTree.getSelectionPaths();
      if (paths.length == 1) {
         MenuNode mnode = (MenuNode)paths[0].getLastPathComponent();
         JPopupMenu popup = new JPopupMenu();
         if (mnode instanceof MenuEntry) {
            addMenuItem (popup, "Add package menu ..."); 
         }
         else {
            addMenuItem (popup, "Leaf node"); 
         }
         popup.setLightWeightPopupEnabled (false);
         popup.show (e.getComponent(), e.getX(), e.getY());
      }
   }

   protected class MenuMouseListener extends MouseInputAdapter {

      public void mouseClicked(MouseEvent e) {
         int row = myTree.getRowForLocation (e.getX(), e.getY());
         if (row == -1) {
            myTree.clearSelection();
         }
      }
   }

   protected class MenuSelectionListener implements TreeSelectionListener {

      public void valueChanged (TreeSelectionEvent e) {
         updateButtons();
      }
   }

   protected TreePath pathToNode (MenuNode node) {
      LinkedList<MenuNode> path = new LinkedList<>();
      while (node != null) {
         path.push (node);
         node = node.getParent();
      }
      return new TreePath (path.toArray(new MenuNode[0]));
   }

   private MenuEntry getRootMenuEntry() {
      return myModelMenu.getMenuTree();
   }

   protected class MenuTreeModel implements TreeModel {
      ArrayList<TreeModelListener> myListeners;
      
      MenuTreeModel() {
         myListeners = new ArrayList<>();
      }

      public void addTreeModelListener(TreeModelListener l) {
         myListeners.add(l);
      }

      public void removeTreeModelListener(TreeModelListener l) {
         myListeners.remove(l);
      }

      public Object getChild (Object parent, int index) {
         if (parent instanceof MenuEntry) {
            MenuEntry pnode = (MenuEntry)parent;
            if (index >= 0 && index < pnode.numChildren()) {
               return pnode.getChild(index);
            }
         }
         return null;
      }

      public int getChildCount (Object parent) {
         if (parent instanceof MenuEntry) {
            MenuEntry pnode = (MenuEntry)parent;
            return pnode.numChildren();
         }
         return 0;
      }

      public int getIndexOfChild (Object parent, Object child) {
         if (parent instanceof MenuEntry) {
            MenuEntry pnode = (MenuEntry)parent;
            int idx = 0;
            for (MenuNode c : pnode.getChildren()) {
               if (c == child) {
                  return idx;
               }
               idx++;
            }
         }
         return -1;
      }

      public Object getRoot() {
         return getRootMenuEntry();
      }

      public boolean isLeaf (Object node) {
         if (node instanceof MenuEntry) {
            MenuEntry menu = (MenuEntry)node;
            return menu.isExpandable();
         }
         return true;
      }

      protected void fireTreeNodesChanged (
         TreePath path, int[] childIndices, Object[] children) {
         TreeModelEvent e = null;
         for (int i = myListeners.size() - 1; i >= 0; i--) {
            if (e == null) {
               e = new TreeModelEvent (this, path, childIndices, children);
            }
            myListeners.get(i).treeNodesChanged(e);
         }
      }

      protected void fireTreeNodeChanged (TreePath path) {
         TreeModelEvent e = null;
         for (int i = myListeners.size() - 1; i >= 0; i--) {
            if (e == null) {
               e = new TreeModelEvent (this, path);
            }
            myListeners.get(i).treeNodesChanged(e);
         }
      }

      protected void fireTreeNodesRemoved(
         TreePath path, int[] childIndices, Object[] children) {
         TreeModelEvent e = null;
         for (int i = myListeners.size() - 1; i >= 0; i--) {
            if (e == null) {
               e = new TreeModelEvent (this, path, childIndices, children);
            }
            myListeners.get(i).treeNodesRemoved(e);
         }
      }

      protected void fireTreeNodesInserted(
         TreePath path, int[] childIndices, Object[] children) {
         TreeModelEvent e = null;
         for (int i = myListeners.size() - 1; i >= 0; i--) {
            if (e == null) {
               e = new TreeModelEvent (this, path, childIndices, children);
            }
            myListeners.get(i).treeNodesInserted(e);
         }
      }

      protected void fireTreeStructureChanged (TreePath path) {
         TreeModelEvent e = null;
         for (int i = myListeners.size() - 1; i >= 0; i--) {
            if (e == null) {
               e = new TreeModelEvent (this, path);
            }
            myListeners.get(i).treeStructureChanged(e);
         }
      }

      public void valueForPathChanged(TreePath path, Object newValue) {
      }
   }

   private void configureNodeRenderComponent (
      DefaultTreeCellRenderer comp, MenuNode mnode,
      boolean selected, boolean hasFocus) {
      comp.setFont (mnode.getFont());
      if (mnode instanceof PackageEntry) {
         PackageEntry pentry = (PackageEntry)mnode;
         comp.setIcon (myPackageIcon);
         String text = "["+pentry.getPackageName()+"]";
         if (pentry.getTitle() != null) {
            text = pentry.getTitle() + " " + text;
         }
         comp.setText (text);
      }
      else if (mnode instanceof ModelEntry) {
         comp.setIcon (myJavaFileIcon);
         comp.setText (mnode.getTitle());
      }
      else if (mnode instanceof DemoFileEntry) {
         DemoFileEntry modelEntry = (DemoFileEntry)mnode;
         comp.setIcon (myTextFileIcon);
         String text = "["+modelEntry.getFile().getName()+"]";
         if (modelEntry.getTitle() != null) {
            text = modelEntry.getTitle() + " " + text;
         }
         comp.setText (text);
      }
      else if (mnode instanceof ScriptEntry) {
         comp.setIcon (myScriptFileIcon);
         comp.setText (mnode.getTitle());
      }
      else if (mnode instanceof ScriptFolderEntry) {
         ScriptFolderEntry entry = (ScriptFolderEntry)mnode;
         comp.setIcon (myScriptFolderIcon);
         String text = "["+entry.getFile().getName()+"]";
         if (entry.getTitle() != null) {
            text = entry.getTitle() + " " + text;
         }
         comp.setText (text);
      }
      else if (mnode instanceof SeparatorEntry) {
         if (selected) {
            if (hasFocus) {
               comp.setIcon (mySeparatorSelectedFocusedIcon);
            }
            else {
               comp.setIcon (mySeparatorSelectedIcon);
            }
         }
         else {
            comp.setIcon (mySeparatorIcon);
         }
         comp.setText ("");
      }
      else if (mnode instanceof LabelEntry) {
         comp.setIcon (myLabelIcon);
         comp.setText (mnode.getTitle());
      }
      else if (mnode instanceof MenuEntry) {
         comp.setIcon (myMenuIcon);
         comp.setText (mnode.getTitle());
         if (((MenuEntry)mnode).numChildren() == 0) {
            comp.setClosedIcon (null);
         }
      }
      else {
         throw new InternalErrorException (
            "Unknown menu node: " + mnode);
      }
   }

   protected class MenuTreeRenderer extends DefaultTreeCellRenderer {

      public Component getTreeCellRendererComponent (
         JTree tree, Object value, boolean selected, boolean expanded,
         boolean leaf, int row, boolean hasFocus) {

         super.getTreeCellRendererComponent (
            tree, value, selected, expanded, leaf, row, hasFocus);

         if (value instanceof MenuNode) {
            configureNodeRenderComponent (
               this, (MenuNode)value, selected, hasFocus);
         }
         return this;
      }
   }

   public void processDemoFileEdits (
      File demoFile, DemoFileEntry demEntry, DemoFileEntry tmpEntry) {

      //boolean oldExpanded = demEntry.expandEntry();
      if (!demoFile.canRead()) {
         GuiUtils.showError (
            this, "File '" + demoFile +
            "' does not exist or is not readable");
         return;
      }
      AliasTable table = null;
      if (tmpEntry.getFile() == null ||
          !demoFile.equals(tmpEntry.getFile())) {
         table = tmpEntry.readAliasTable (demoFile);
         if (table == null) {
            GuiUtils.showError (
               this, "Can't read demos from file '"+demoFile+"'");
            return;
         }
         tmpEntry.setFile (demoFile);
      }
      demEntry.set (tmpEntry);
      if (table != null) {
         demEntry.createChildNodes (table);
      }
      myLastDemoFile = demoFile;
      myTreeModel.fireTreeNodeChanged (pathToNode (demEntry));

      updateMenusToRoot (demEntry);

      // // find the menu entry we need to invalidate
      // MenuEntry mentry = demEntry;      
      // boolean newExpanded = demEntry.expandEntry();
      // if (oldExpanded && !newExpanded) {
      //    mentry.invalidateMenu();
      // }
      // if (newExpanded || newExpanded != oldExpanded) {
      //    // go up a level
      //    mentry = mentry.getParent();
      // }
      // mentry.invalidateMenu();
      // mentry.updateComponent (myModelMenu);
   }

   public class AddNodesCommand extends ModelMenuCommand {

      ArrayList<MenuNode> myNodes;
      MenuEntry myParent;
      int myInsertIdx;
      String myCmdName;
      int[] myInsertIndices;
      Object[] myInsertNodes;

      public AddNodesCommand (
         MenuEntry parent, MenuNode node, int idx, String cmdName) {
         myParent = parent;
         myNodes = new ArrayList<>();
         myNodes.add (node);
         myInsertIdx = idx;
         myInsertIndices = new int[] { idx }; 
         myInsertNodes = new Object[] { node }; 
         myCmdName = cmdName;
      }

      public AddNodesCommand (
         MenuEntry parent, Collection<MenuNode> nodes,
         int idx, String cmdName) {
         myParent = parent;
         myNodes = new ArrayList<>();
         myNodes.addAll (nodes);
         myInsertIdx = idx;
         myInsertIndices = new int[nodes.size()];
         myInsertNodes = new Object[nodes.size()];
         for (int i=0; i<myInsertIndices.length; i++) {
            myInsertIndices[i] = myInsertIdx+i;
            myInsertNodes[i] = myNodes.get(i);
         }
         myCmdName = cmdName;
      }

      public void execute() {
         for (int i=0; i<myNodes.size(); i++) {
            MenuNode node = myNodes.get(i);
            myParent.addChild (myInsertIdx+i, node);
         }
         myTreeModel.fireTreeNodesInserted (
            pathToNode(myParent), myInsertIndices, myInsertNodes);
         //notifyTreeStructureChanged (myParent);
         updateMenusToRoot (myParent);
         addStructureModCount (1);
         myTree.expandPath (pathToNode (myParent));
         updateButtons();
      }
      
      public void undo() {
         for (int i=myNodes.size()-1; i >= 0; i--) {
            MenuNode node = myNodes.get(i);
            myParent.removeChild (node);
         }
         myTreeModel.fireTreeNodesRemoved (
            pathToNode(myParent), myInsertIndices, myInsertNodes);
         updateMenusToRoot (myParent);
         addStructureModCount (-1);
         myTree.expandPath (pathToNode (myParent));
         updateButtons();
      }
      
      public String getName() {
         return myCmdName;
      }
   }

   public abstract class ModelMenuCommand implements Command {
   }                       

   /**
    * Information need to remove at set of nodes from a particular parent.
    */
   private class RemoveRecord { 
      MenuEntry myParent;
      ArrayList<MenuNode> myNodeList;
      int[] myIndices;
      MenuNode[] myNodes;
      boolean[] myWasExpanded;

      RemoveRecord (MenuEntry parent) {
         myParent = parent;
         myNodeList = new ArrayList<>();
      }

      void execute() {
         // sort nodes by ascending indices
         Collections.sort (
            myNodeList, 
            new Comparator<MenuNode>() {
               public int compare (MenuNode n0, MenuNode n1) {
                  if (n0.getIndex() < n1.getIndex()) {
                     return -1;
                  }
                  else if (n0.getIndex() == n1.getIndex()) {
                     return 0;
                  }
                  else {
                     return 1;
                  }
               }
            });
         myNodes = myNodeList.toArray (new MenuNode[0]);
         myIndices = new int[myNodes.length];
         myWasExpanded = new boolean[myNodes.length];
         for (int i=0; i<myNodes.length; i++) {
            MenuNode node = myNodes[i];
            if (node instanceof MenuNode) {
               myWasExpanded[i] = myTree.isExpanded (pathToNode(node));
            }
            myIndices[i] = node.getIndex();
         }
         for (int i=myNodes.length-1; i >= 0; i--) {
            myParent.removeChild (myNodes[i]);
         }
         myTreeModel.fireTreeNodesRemoved (
            pathToNode(myParent), myIndices, myNodes);
         myParent.invalidateMenusToRoot ();
      }

      void undo() {
         for (int i=0; i<myNodes.length; i++) {
            myParent.addChild (myIndices[i], myNodes[i]);
         }
         myTreeModel.fireTreeNodesInserted (
            pathToNode(myParent), myIndices, myNodes);
         for (int i=0; i<myNodes.length; i++) {
            if (myWasExpanded[i]) {
               myTree.expandPath (pathToNode(myNodes[i]));
            }
         }
         myParent.invalidateMenusToRoot ();
      }
   }

   public class RemoveNodesCommand extends ModelMenuCommand {

      HashMap<MenuEntry,RemoveRecord> myRecords;
      String myCmdName;

      // private void updateTreeAndMenus() {
      //    for (RemoveRecord rec : myRecords.values()) {
      //       myTreeModel.fireTreeNodesRemoved (
      //          rec.myParent, rec.myIndices, rec.myNodes);
      //       rec.myParent.invalidateMenusToRoot ();
      //    }
      //    getRootMenuEntry().updateComponent (myModelMenu);
      // }

      public RemoveNodesCommand (
         Collection<MenuNode> nodes, String cmdName) {
         myRecords = new HashMap<>();
         for (MenuNode n : nodes) {
            RemoveRecord rec = myRecords.get(n.getParent());
            if (rec == null) {
               rec = new RemoveRecord(n.getParent());
               myRecords.put (n.getParent(), rec);
            }
            rec.myNodeList.add (n);
         }
         myCmdName = cmdName;
      }

      public void execute() {
         for (RemoveRecord rec : myRecords.values()) {
            rec.execute();
         }
         getRootMenuEntry().updateComponent (myModelMenu);
         //updateTreeAndMenus();
         addStructureModCount (1);
         for (RemoveRecord rec : myRecords.values()) {
            myTree.expandPath (pathToNode (rec.myParent));
         }
         updateButtons();
      }
      
      public void undo() {
         for (RemoveRecord rec : myRecords.values()) {
            rec.undo();
         }
         getRootMenuEntry().updateComponent (myModelMenu);
         //updateTreeAndMenus();
         addStructureModCount (-1);
         for (RemoveRecord rec : myRecords.values()) {
            myTree.expandPath (pathToNode (rec.myParent));
         }
         updateButtons();
      }
      
      public String getName() {
         return myCmdName;
      }
   }

   public class ShiftNodesCommand extends ModelMenuCommand {

      MenuEntry myParent;
      int myMinIdx;
      int myMaxIdx;
      String myCmdName;
      int myShift;

      public ShiftNodesCommand (
         MenuEntry parent, int minIdx, int maxIdx,
         int shift, String cmdName) {

         myParent = parent;
         myMinIdx = minIdx;
         myMaxIdx = maxIdx;
         myShift = shift;
         myCmdName = cmdName;
      }

      public void execute() {
         myParent.shiftChildren (myMinIdx, myMaxIdx, myShift);
         notifyTreeStructureChanged (myParent);
         updateMenu (myParent);
         addStructureModCount (1);
      }
      
      public void undo() {
         if (myShift == 1) {
            myParent.shiftChildren (myMinIdx+1, myMaxIdx+1, -1);
         }
         else if (myShift == -1) {
            myParent.shiftChildren (myMinIdx-1, myMaxIdx-1, 1);
         }
         notifyTreeStructureChanged (myParent);
         updateMenu (myParent);
         addStructureModCount (-1);
      }
      
      public String getName() {
         return myCmdName;
      }
   }


   /**
    * Update the menu for {@code mentry}, along with the menus of all its
    * ancestors. This may be necessary because whether or not a menu needs
    * updating depends non-trivially on whether 'isEmpty()' and 'isExpanded()'
    * has changed for {@code mentry}, with the effect propogating upward
    * through the ancestors.
    */
   private void updateMenusToRoot (MenuEntry mentry) { 
      mentry.invalidateMenusToRoot();
      getRootMenuEntry().updateComponent(myModelMenu);
   }
   
   /**
    * Notify the editor menu tree of a structure change involving {@code
    * mentry}.
    */
   private void notifyTreeStructureChanged (MenuEntry mentry) {
      myTreeModel.fireTreeStructureChanged (pathToNode (mentry));
   }

   /**
    * Uodate the menu associated with {@code mentry}.
    */
   private void updateMenu (MenuEntry mentry) {
      mentry.invalidateMenu();
      mentry.updateComponent (myModelMenu);
   }

   public void actionPerformed (ActionEvent e) {
      String cmdName = e.getActionCommand();

      if (cmdName.equals ("Add package")) {
         PackageEntry packageEntry = new PackageEntry();
         PackageDialog dialog =
            new PackageDialog (
               this, packageEntry, myCurrentPackageName,
               "Add package", "Add", getRootModelManager());
         GuiUtils.locateCenter (dialog, this);
         dialog.setVisible (true);
         if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
            String packname = dialog.getPackageName();
            packageEntry.setPackageName (packname);
            packageEntry.createChildNodes (
               getRootModelManager(), /*useCache=*/false);
            // save package name for later
            myCurrentPackageName = packname;
            addNodeToMenu (packageEntry, cmdName);
            maybeSaveRootModelCache();
         }
      }
      else if (cmdName.equals ("Add label")) {
         LabelEntry label = new LabelEntry();
         label.setFontStyle (MenuNode.FontStyle.ITALIC);
         WidgetDialog dialog = createLabelDialog (label, /*edit=*/false);
         GuiUtils.locateCenter (dialog, this);
         dialog.setVisible (true);
         if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
            addNodeToMenu (label, cmdName);            
         }
      }
      else if (cmdName.equals ("Add demo file")) {
         DemoFileEntry modelEntry = new DemoFileEntry();

         DemoFileDialog dialog =
            new DemoFileDialog (
               this, modelEntry, myLastDemoFile, "Add demo file", "Add");
         GuiUtils.locateCenter (dialog, this);
         dialog.setVisible (true);
         if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
            File demoFile = dialog.getSelectedFile();
            if (!demoFile.canRead()) {
               GuiUtils.showError (
                  this, "File '" + demoFile +
                  "' does not exist or is not readable");
            }
            else {
               AliasTable table = null;
               table = modelEntry.readAliasTable (demoFile);
               if (table == null) {
                  GuiUtils.showError (
                     this, "Can't read demos from file '"+demoFile+"'");
               }
               if (table != null) {
                  modelEntry.setFile (demoFile);
                  modelEntry.createChildNodes (table);
                  myLastDemoFile = demoFile;
                  addNodeToMenu (modelEntry, cmdName);
               }
            }
         }        
      }
      else if (cmdName.equals ("Add separator")) {
         addNodeToMenu (new SeparatorEntry(), cmdName);
      }
      else if (cmdName.equals ("Add models")) {
         ModelEntry modelEntry = new ModelEntry();
         ModelDialog dialog =
            new ModelDialog (
               this, modelEntry, myCurrentPackageName,
               "Add models", "Add", getRootModelManager());
         GuiUtils.locateCenter (dialog, this);
         dialog.setVisible (true);
         if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
            List<String> classNames = dialog.getClassNames();
            if (classNames.size() > 0) { // size should be > 0; just paranoid
               ArrayList<MenuNode> nodes = new ArrayList<>();
               for (String className : classNames) {
                  ModelEntry newEntry = new ModelEntry();
                  newEntry.set (modelEntry);
                  newEntry.setClassName (className);
                  if (isStringEmpty (modelEntry.getTitle())) {
                     newEntry.setTitle (getSimpleName (className));
                  }
                  nodes.add (newEntry);
               }
               addNodesToMenu (nodes, cmdName);
            }
            myCurrentPackageName = dialog.getPackageName();
            maybeSaveRootModelCache();            
         }
      }
      else if (cmdName.equals ("Add submenu")) {
         MenuEntry menuEntry = new MenuEntry();
         WidgetDialog dialog = createMenuDialog (menuEntry, /*edit=*/false);
         GuiUtils.locateCenter (dialog, this);
         dialog.setVisible (true);
         if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
            addNodeToMenu (menuEntry, cmdName);            
         }
      }
      else if (cmdName.equals ("Add script folder")) {
         ScriptFolderEntry entry = new ScriptFolderEntry();
         ScriptFolderDialog dialog =
            new ScriptFolderDialog (
               this, entry, myLastScriptFolder,
               "Add script folder", "Add");
         GuiUtils.locateCenter (dialog, this);
         dialog.setVisible (true);
         if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
            File dir = dialog.getSelectedFile();
            entry.setFile (dir);
            entry.createChildNodes ();
            // save folder for later
            myLastScriptFolder = dir;
            addNodeToMenu (entry, cmdName); 
         }
      }
      else if (cmdName.equals ("Add script")) {
         ScriptEntry entry = new ScriptEntry();
         ScriptDialog dialog =
            new ScriptDialog (
               this, entry, myLastScriptFolder, "Add scripts", "Add");
         GuiUtils.locateCenter (dialog, this);
         dialog.setVisible (true);
         if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
            List<ScriptDesc> scripts = dialog.getScripts();
            if (scripts.size() > 0) { // size should be > 0; just paranoid
               ArrayList<MenuNode> nodes = new ArrayList<>();
               for (ScriptDesc script : scripts) {
                  ScriptEntry newEntry = new ScriptEntry();
                  newEntry.set (entry);
                  newEntry.setFile (script.getFile());
                  if (isStringEmpty (entry.getTitle())) {
                     newEntry.setTitle (script.getName());
                  }
                  nodes.add (newEntry);
               }
               addNodesToMenu (nodes, cmdName);
            }
         }
         myLastScriptFolder = dialog.getScriptFolder();

         // ScriptDialog dialog =
         //    new ScriptDialog (
         //       this, entry, myLastScriptFile, "Add script", "Add");
         // GuiUtils.locateCenter (dialog, this);
         // dialog.setVisible (true);
         // if (dialog.getReturnValue() == OptionPanel.OK_OPTION &&
         //     // selected file should be non-null; just paranoid
         //     dialog.getSelectedFile() != null) {
         //    File script = dialog.getSelectedFile();
         //    ScriptEntry newEntry = new ScriptEntry();
         //    newEntry.set (entry);
         //    newEntry.setFileName (script.getAbsolutePath());
         //    if (isStringEmpty (entry.getTitle())) {
         //       newEntry.setTitle (
         //          ScriptEntry.getDefaultScriptTitle (script));
         //    }
         //    addNodeToMenu (newEntry, cmdName);
         //    myLastScriptFile = script;
         // }
         // else {
         //    myLastScriptFile = dialog.getCurrentDirectory();
         // }
      }
      else if (cmdName.equals ("Edit")) {
         TreePath path = myTree.getSelectionPath();
         MenuNode node = (MenuNode)path.getLastPathComponent();
         if (node instanceof PackageEntry) {
            PackageEntry tmpEntry = new PackageEntry();
            PackageEntry pkgEntry = (PackageEntry)node;
            tmpEntry.set (pkgEntry);

            PackageDialog dialog =
               new PackageDialog (
                  this, tmpEntry, null,
                  "Edit package", "Done",
                  getRootModelManager());
            GuiUtils.locateCenter (dialog, this);
            dialog.setVisible (true);
            //boolean oldExpanded = pkgEntry.expandEntry();
            //boolean oldEmpty = pkgEntry.isMenuEmpty(pkgEntry);
            if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
               String packname = dialog.getPackageName();
               // set in tmpEntry so it will then get set if needed in node
               tmpEntry.setPackageName (packname);
               pkgEntry.set (tmpEntry);
               pkgEntry.updateChildNodes (getRootModelManager());
               //boolean newEmpty = pkgEntry.isMenuEmpty(pkgEntry);
               // save package name for later
               myCurrentPackageName = packname;
               myTreeModel.fireTreeNodeChanged (pathToNode (node));

               updateMenusToRoot (pkgEntry);
               // // find the menu entry we need to invalidate
               // MenuEntry mentry = pkgEntry;
               // boolean newExpanded = pkgEntry.expandEntry();
               // if (oldExpanded && !newExpanded) {
               //    mentry.invalidateMenu();
               // }
               // if (newExpanded || newExpanded != oldExpanded) {
               //    // go up a level
               //    mentry = mentry.getParent();
               // }
               // if (oldEmpty != newEmpty) {
               //    // go up a level 
               //    mentry = mentry.getParent();
               // }
               // mentry.invalidateMenu();
               // mentry.updateComponent (myModelMenu);
               maybeSaveRootModelCache();
               markContentsEdited();
            }
         }
         else if (node instanceof DemoFileEntry) {
            DemoFileEntry tmpEntry = new DemoFileEntry();
            DemoFileEntry demEntry = (DemoFileEntry)node;
            tmpEntry.set (demEntry);
            DemoFileDialog dialog =
               new DemoFileDialog (
                  this, tmpEntry, myLastDemoFile, "Edit demo file", "Done");
            GuiUtils.locateCenter (dialog, this);
            dialog.setVisible (true);
            if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
               processDemoFileEdits (
                  dialog.getSelectedFile(), demEntry, tmpEntry);
               markContentsEdited();
            }
         }
         else if (node instanceof ModelEntry) {
            ModelEntry modelEntry = new ModelEntry();
            modelEntry.set ((ModelEntry)node);
            ModelDialog dialog =
               new ModelDialog (
                  this, modelEntry, /*packageName=*/null,
                  "Edit model", "Done", getRootModelManager());
            GuiUtils.locateCenter (dialog, this);
            dialog.setVisible (true);
            if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
               modelEntry.setClassName (dialog.getClassName());
               ((ModelEntry)node).set (modelEntry);
               // save package name for later
               myCurrentPackageName = dialog.getPackageName();
               myTreeModel.fireTreeNodeChanged (pathToNode (node));
               node.updateComponent (myModelMenu); 
               maybeSaveRootModelCache();
               markContentsEdited();
            }
         }
         else if (node instanceof ScriptEntry) {
            ScriptEntry entry = new ScriptEntry();
            entry.set ((ScriptEntry)node);
            ScriptDialog dialog =
               new ScriptDialog (
                  this, entry, /*scriptFolde=*/null, "Edit script", "Done");
            GuiUtils.locateCenter (dialog, this);
            dialog.setVisible (true);
            if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
               entry.setFile (dialog.getScript().getFile());
               ((ScriptEntry)node).set (entry);
               myTreeModel.fireTreeNodeChanged (pathToNode (node));
               node.updateComponent (myModelMenu); 
               markContentsEdited();
            }
            myLastScriptFolder = dialog.getScriptFolder();
         }
         else if (node instanceof ScriptFolderEntry) {
            ScriptFolderEntry tmpEntry = new ScriptFolderEntry();
            ScriptFolderEntry dirEntry = (ScriptFolderEntry)node;
            tmpEntry.set (dirEntry);
            ScriptFolderDialog dialog =
               new ScriptFolderDialog (
                  this, tmpEntry, myLastScriptFolder, 
                  "Edit script folder", "Done");
            GuiUtils.locateCenter (dialog, this);
            dialog.setVisible (true);
            if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
               File dir = dialog.getSelectedFile();
               tmpEntry.setFile (dir);
               dirEntry.set (tmpEntry);

               dirEntry.updateChildNodes ();
               // save folder for later
               myLastScriptFolder = dir;
               myTreeModel.fireTreeNodeChanged (pathToNode (node));
               updateMenusToRoot (dirEntry);               
               markContentsEdited();
            }
         }
         else if (node instanceof MenuEntry) {
            MenuEntry menuEntry = new MenuEntry();
            menuEntry.set ((MenuEntry)node);
            WidgetDialog dialog = createMenuDialog (menuEntry, /*edit=*/true);
            GuiUtils.locateCenter (dialog, this);
            dialog.setVisible (true);
            if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
               ((MenuEntry)node).set (menuEntry);
               myTreeModel.fireTreeNodeChanged (pathToNode (node));
               node.updateComponent (myModelMenu);               
               markContentsEdited();
            }
         }
         else if (node instanceof LabelEntry) {
            LabelEntry label = new LabelEntry();
            label.set ((LabelEntry)node);
            WidgetDialog dialog = createLabelDialog (label, /*edit=*/true);
            GuiUtils.locateCenter (dialog, this);
            dialog.setVisible (true);
            if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
               ((LabelEntry)node).set (label);
               myTreeModel.fireTreeNodeChanged (pathToNode (node));
               node.updateComponent (myModelMenu);
               markContentsEdited();
            }
         }
      }
      else if (cmdName.equals ("Delete")) {
         deleteSelectedNodes ();
      }
      else if (cmdName.equals ("Up")) {
         shiftSelectedNodes (-1);
      }
      else if (cmdName.equals ("Save")) {
         saveMenu();
      }
      else if (cmdName.equals ("Reload")) {
         reloadMenu();
      }
      else if (cmdName.equals ("Save to ...")) {
         saveMenuTo();
      }
      else if (cmdName.equals ("Load from ...")) {
         loadMenuFrom();
      }
      else if (cmdName.equals ("Done")) {
         setVisible (false);
         clearUndoCommands();
      }
      else if (cmdName.equals ("Reset to default")) {
         restoreDefaultMenu();
      }
      else if (cmdName.equals ("Down")) {
         shiftSelectedNodes (1);
      }
   }

   private void updateButtons() {
      // Selection requirements for different operations:
      //
      // Edit: a single node that is not the root node or a separator
      //
      // Add: no selection, or a single node that is a MenuNode
      //
      // Delete: any nodes that are not the root node
      //
      // Up: a set of nodes with a common parent with lowest index > 0
      //
      // Down: a set of nodes with a common parent with highest index <
      // numChildren-1
      
      boolean hasEditSelection = false;
      boolean hasDeleteSelection = false;
      boolean hasUpSelection = false;
      boolean hasDownSelection = false;
      boolean hasAddSelection = true;
    
      TreePath[] paths = myTree.getSelectionPaths();
      if (paths != null && paths.length > 0) {
         int numSelected = paths.length;
         boolean separatorSelected = false;
         boolean rootMenuSelected = false;
         boolean menuSelected = false;
         boolean hasCommonParent = false;
         MenuEntry parent = null;

         hasAddSelection = (numSelected == 1);

         for (int i=0; i<numSelected; i++) {
            MenuNode node = (MenuNode)paths[i].getLastPathComponent();
            if (i == 0) {
               parent = node.getParent();
               hasCommonParent = (parent != null);
            }
            else if (hasCommonParent && parent != node.getParent()) {
               hasCommonParent = false;
            }
            if (node instanceof SeparatorEntry) {
               separatorSelected = true;
            }
            else if (node == getRootMenuEntry()) {
               rootMenuSelected = true;
            }
            if (node instanceof MenuEntry && !((MenuEntry)node).isExpandable()) {
               menuSelected = true;
            }
         }
         hasEditSelection =
            (numSelected == 1 && !separatorSelected && !rootMenuSelected);
         hasDeleteSelection = !rootMenuSelected;

         if (hasCommonParent) {
            boolean[] selected = new boolean[parent.numChildren()];
            // process again for min/max indices
            int minIdx = Integer.MAX_VALUE;
            int maxIdx = -1;
            for (int i=0; i<numSelected; i++) {
               MenuNode node = (MenuNode)paths[i].getLastPathComponent();
               int idx = node.getIndex();
               selected[idx] = true;
               if (idx > maxIdx) {
                  maxIdx = idx;
               }
               if (idx < minIdx) {
                  minIdx = idx;
               }
            }
            boolean contiguous = true;
            for (int i=minIdx; i<=maxIdx; i++) {
               if (!selected[i]) {
                  contiguous = false;
                  break;
               }
            }
            hasUpSelection = contiguous && minIdx > 0;
            hasDownSelection = contiguous && maxIdx < parent.numChildren()-1;
         }
      }

      if (myAddPackageButton != null) {
         myAddPackageButton.setEnabled (hasAddSelection);
         myAddModelsButton.setEnabled (hasAddSelection);
         myAddDemoFileButton.setEnabled (hasAddSelection);
      }
      else if (myAddScriptButton != null) {
         myAddScriptButton.setEnabled (hasAddSelection);
         myAddScriptFolderButton.setEnabled (hasAddSelection);
      }
      myAddMenuButton.setEnabled (hasAddSelection);
      myAddLabelButton.setEnabled (hasAddSelection);
      myAddSeparatorButton.setEnabled (hasAddSelection);


      myUpButton.setEnabled (hasUpSelection);
      myDownButton.setEnabled (hasDownSelection);

      myEditButton.setEnabled (hasEditSelection);
      myDeleteButton.setEnabled (hasDeleteSelection);
   }

   public void valueChange (ValueChangeEvent e) {
      // if (e.getSource() == myPackageField) {
      //    updateButtons();
      // }
   }

   private JButton createIconButton (
      JPanel panel, ImageIcon icon, String cmd, String toolTip) {
      JButton button = new JButton (icon);
      button.setActionCommand (cmd);
      if (toolTip != null) {
         button.setToolTipText (toolTip);
      }
      button.addActionListener (this);
      button.setEnabled (false);  
      button.setHorizontalAlignment (SwingConstants.LEFT);
      button.setMargin (new Insets (5, 10, 5, 10));
      panel.add (button);
      panel.add (Box.createRigidArea (new Dimension(2, 0)));
      return button;
   }

   private JButton createVerticalButton (JPanel panel, String name) {
      return createVerticalButton (panel, name, null);
   }

   private JButton createVerticalButton (
      JPanel panel, String name, String toolTip) {
      JButton button = new JButton (name);
      button.setActionCommand (name);
      if (toolTip != null) {
         button.setToolTipText (toolTip);
      }
      button.addActionListener (this);
      button.setAlignmentX (Component.LEFT_ALIGNMENT);
      Dimension size = button.getPreferredSize();
      button.setMaximumSize (new Dimension (Short.MAX_VALUE, size.height));
      button.setHorizontalAlignment (SwingConstants.LEFT);
      button.setMargin (new Insets (5, 10, 5, 10));
      panel.add (button);
      panel.add (Box.createRigidArea (new Dimension(0, 2)));
      return button;
   }

   private JPanel createUpDownButtons (JPanel panel) { 
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout (new BoxLayout (buttonPanel, BoxLayout.LINE_AXIS));

      myUpButton = createIconButton (
         buttonPanel, myUpIcon, "Up", "Shift the selected menu items up");
      myDownButton = createIconButton (
         buttonPanel, myDownIcon, "Down", "Shift the selected menu items down");
      myDeleteButton = createIconButton (
         buttonPanel, myDeleteIcon, "Delete", "Delete the selected menu items");

      buttonPanel.setAlignmentX (Component.LEFT_ALIGNMENT);
      Dimension size = buttonPanel.getPreferredSize();
      buttonPanel.setMaximumSize (new Dimension (Short.MAX_VALUE, size.height));

      panel.add (buttonPanel);      
      return buttonPanel;
   }

   private JPanel createButtonPanel (ModelScriptMenu.Type type) {
      JPanel panel = new JPanel();
      panel.setLayout (new BoxLayout (panel, BoxLayout.PAGE_AXIS));

      if (type == ModelScriptMenu.Type.MODEL) {
         myAddModelsButton = createVerticalButton (
            panel, "Add models",
            "Add one or more models to the menu");
         myAddPackageButton = createVerticalButton (
            panel, "Add package",
            "Add a model package submenu to the menu");
      }
      else if (type == ModelScriptMenu.Type.SCRIPT) {
         myAddScriptButton = createVerticalButton (
            panel, "Add script",
            "Add a Jython script to the menu");
         myAddScriptFolderButton = createVerticalButton (
            panel, "Add script folder",
            "Add a script folder to the menu");
      }
      myAddMenuButton = createVerticalButton (
         panel, "Add submenu",
         "Create and add a submenu to the menu");
      if (type == ModelScriptMenu.Type.MODEL) {
         myAddDemoFileButton = createVerticalButton (
            panel, "Add demo file",
            "Add a demo-file based submenu to the menu");
      }
      myAddLabelButton = createVerticalButton (
         panel, "Add label",
         "Add a label to the menu");
      myAddSeparatorButton = createVerticalButton (
         panel, "Add separator",
         "Add a separator to the menu");

      // JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
      // sep.setMaximumSize (new Dimension(Short.MAX_VALUE, 2));
      // sep.setAlignmentX (Component.LEFT_ALIGNMENT);
      // panel.add (sep);
      panel.add (Box.createRigidArea (new Dimension(0, 10)));

      myEditButton = createVerticalButton (
         panel, "Edit", "Edit the selected menu item");
      myEditButton.setHorizontalAlignment (SwingConstants.CENTER);
      myEditButton.setEnabled (false);
      //myDeleteButton = createVerticalButton (panel, "Delete");
      createUpDownButtons (panel);

      panel.add (Box.createVerticalGlue());

      mySaveToButton = createVerticalButton (
         panel, "Save to ...", "Save menu to a specified file");
      myLoadFromButton = createVerticalButton (
         panel, "Load from ...", "Load menu from a specified file");
      panel.add (Box.createRigidArea (new Dimension(0, 10)));
      mySaveButton = createVerticalButton (panel, "Save", "");
      // tool tip will be set later ...
      mySaveButton.setEnabled (false);
      myReloadButton = createVerticalButton (panel, "Reload", "");
      // tool tip will be set later ...

      panel.setBorder (
         BorderFactory.createCompoundBorder (
            BorderFactory.createEtchedBorder (),
            BorderFactory.createEmptyBorder (2, 2, 2, 2)));

      return panel;
   }

   public ModelScriptMenuEditor (
      ModelScriptMenu modelMenu, UndoManager undoManager, File file) {

      super (modelMenu.getType().mixedCaseName() + " menu editor");

      myFileSearchPath = ArtisynthPath.createDefaultSearchPath();
      if (modelMenu.getType() == ModelScriptMenu.Type.MODEL) {
         // allow the file directory itself to be searched for demo files
         myFileSearchPath.addDirectory (0, FileSearchPath.getParentFile(file));
      }     

      addWindowListener (new WindowAdapter() {
         public void windowClosing (WindowEvent e) {
            clearUndoCommands();
         }
      });
      myModelMenu = modelMenu;
      myUndoManager = undoManager;

      initializeIcons();

      UIManager.put (
         "Tree.collapsedIcon", new IconUIResource(myTreeCollapsedIcon));
      UIManager.put (
         "Tree.expandedIcon", new IconUIResource (myTreeExpandedIcon));

      myTree = new JTree();
      myTreeView = new JScrollPane (myTree);      
      myTreeModel = new MenuTreeModel();
      myTree.setModel (myTreeModel);

      myTree.setEditable (false);
      myTree.setRootVisible (true);
      myTree.putClientProperty ("JTree.lineStyle", "None");
      myTree.setCellRenderer (new MenuTreeRenderer());
      myTree.getSelectionModel().setSelectionMode(
         TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
      myTree.addTreeSelectionListener (new MenuSelectionListener());
      myTree.addMouseListener (new MenuMouseListener());
      myTree.expandRow (0);

      myTreeView.setPreferredSize(new Dimension(320, 480));

      myPanel = new JPanel();
      myPanel.setLayout (new BorderLayout());
      myPanel.add (myTreeView, BorderLayout.CENTER);

      JPanel buttonPanel = createButtonPanel (modelMenu.getType());
      myPanel.add (buttonPanel, BorderLayout.LINE_END);
      
      JPanel bottomPanel = new JPanel();
      bottomPanel.setBorder (BorderFactory.createEmptyBorder (5, 5, 5, 5));
      bottomPanel.setLayout (new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
      
      if (file.equals (modelMenu.getDefaultMenuFile())) {
         myResetButton = GuiUtils.addHorizontalButton (
            bottomPanel, "Reset to default", this, 
            "Reset the menu and its config files to the default configuration");
         mySaveButton.setToolTipText (
            "Save menu to the default file "+file);
         myReloadButton.setToolTipText (
            "Reload menu from the default file "+file);
      }
      else {
         mySaveButton.setToolTipText (
            "Save menu to the file "+file);
         myReloadButton.setToolTipText (
            "Reload menu from the file "+file);
      }

      bottomPanel.add (Box.createHorizontalGlue());
      myDoneButton = GuiUtils.addHorizontalButton (
         bottomPanel, "Done", this, "Close the model editing menu");

      myPanel.add (bottomPanel, BorderLayout.PAGE_END);

      getContentPane().add (myPanel);
      updateButtons();

      myDefaultFile = file;
      updateSaveButton();
      
      pack();
   }

   protected void selectNodes (Collection<MenuNode> nodes) {
      TreePath[] paths = new TreePath[nodes.size()];
      int k = 0;
      for (MenuNode node : nodes) {
         paths[k++] = pathToNode (node);
      }
      myTree.setSelectionPaths (paths);
   }

   protected void deleteSelectedNodes () {
      TreePath[] paths = myTree.getSelectionPaths();      
      if (paths == null) {
         return;
      }
      LinkedHashSet<MenuNode> removeSet = new LinkedHashSet<>();
      for (TreePath path : paths) {
         if (path.getPathCount() > 1) {
            MenuNode node = (MenuNode)path.getLastPathComponent();
            // XXX check for empty component?
            removeSet.add (node);
         }
      }
      ArrayList<MenuNode> redundent = new ArrayList<>();
      // remove nodes whose ancestors are being removed
      for (MenuNode n : removeSet) {
         for (MenuNode p=n.getParent(); p!=null; p=p.getParent()) {
            if (removeSet.contains(p)) {
               redundent.add (n);
               break;
            }
         }
      }
      removeSet.removeAll (redundent);
      myUndoManager.execute (
         new RemoveNodesCommand (removeSet, "Delete nodes"));
   }

   protected void shiftSelectedNodes (int shift) {
      TreePath[] paths = myTree.getSelectionPaths();      
      // verify again that we have a selection
      if (paths == null || paths.length == 0) {
         return;
      }
      // verify again that all nodes have the same parent; if not, return (do
      // nothing)
      MenuEntry parent = null;
      int minIdx = Integer.MAX_VALUE;
      int maxIdx = -1;
      for (int i=0; i<paths.length; i++) {
         if (paths[i].getPathCount() == 0 ||
             !(paths[i].getLastPathComponent() instanceof MenuNode)) {
            return;
         }
         MenuNode node = (MenuNode)paths[i].getLastPathComponent();
         if (i == 0) {
            parent = node.getParent();
         }
         else if (node.getParent() != parent) {
            return;
         }
         int idx = node.getIndex();
         if (idx < minIdx) {
            minIdx = idx;
         }
         if (idx > maxIdx) {
            maxIdx = idx;
         }
      }
      myUndoManager.execute (
         new ShiftNodesCommand (parent, minIdx, maxIdx, shift, "Move nodes"));
      // keep shifted nodes selected
      TreePath[] selectionPaths = new TreePath[maxIdx-minIdx+1];
      int k = 0;
      for (int idx=minIdx+shift; idx<=maxIdx+shift; idx++) {
         selectionPaths[k++] = pathToNode (parent.getChild(idx));
      }
      myTree.setSelectionPaths (selectionPaths);
   }

   protected void addNodeToMenu (MenuNode node, String cmdName) {
      ArrayList<MenuNode> nodes = new ArrayList<>(1);
      nodes.add (node);
      addNodesToMenu (nodes, cmdName);
   }

   protected void addNodesToMenu (
      ArrayList<MenuNode> nodes, String cmdName) {
      TreePath[] paths = myTree.getSelectionPaths();
      MenuEntry parent;
      int insertIdx;

      MenuNode selectedNode = null;
      if (paths != null && paths.length > 0) {
         // find selected path, if any. Use last selected component if there
         // are multiple selections (although currently that is not allowed)
         selectedNode = (MenuNode)paths[paths.length-1].getLastPathComponent();
      }

      // first, figure out where to add the nodes
      if (selectedNode == null) {
         // nothing selected; add to end of root menu
         parent = getRootMenuEntry();
         insertIdx = parent.numChildren();
      }
      else {
         if (selectedNode instanceof MenuEntry &&
             !((MenuEntry)selectedNode).isExpandable()) {
            parent = (MenuEntry)selectedNode;
            insertIdx = parent.numChildren();
         }
         else {
            parent = selectedNode.getParent();
            insertIdx = selectedNode.getIndex();
         }
      }
      myUndoManager.execute (
         new AddNodesCommand (parent, nodes, insertIdx, cmdName));
      // reselect the selected component, if any
      if (selectedNode != null) {
         myTree.setSelectionPath (pathToNode (selectedNode));
      }
   }

   WidgetDialog createNodeDialog (
      MenuNode menuNode, String type, boolean edit) {

      String dialogTitle = (edit ? "Edit " : "Add ");
      String confirmCmd = (edit ? "Done" : "Add");

      return new WidgetDialog (this, dialogTitle+type, confirmCmd);
   }
      
   static void addEnableOnNonNullText (
      WidgetDialog dialog, StringField textField, String confirmCmd) {

      OptionPanel options = dialog.getOptionPanel();
      JButton confirmButton = options.getButton (confirmCmd);
      confirmButton.setEnabled (!textField.valueIsEmpty());

      textField.addValueChangeListener (
         new ValueChangeListener () {
            public void valueChange (ValueChangeEvent e) {
               confirmButton.setEnabled (!textField.valueIsEmpty());
            }
         });
   }
   
   static void addMenuWidgets (WidgetDialog dialog, MenuEntry menuEntry) {
      dialog.addWidget (new JSeparator());
      dialog.addWidget ("Scrolling: ", menuEntry, "scrolling");      
      dialog.addWidget ("Max rows: ", menuEntry, "maxRows");      
      addFontWidgets (dialog, menuEntry);
   }

   static void addFontWidgets (WidgetDialog dialog, MenuNode menuNode) {
      dialog.addWidget (new JSeparator());
      dialog.addWidget ("Font name: ", menuNode, "fontName");
      dialog.addWidget ("Font style: ", menuNode, "fontStyle");
      dialog.addWidget ("Font size: ", menuNode, "fontSize");
   }

   WidgetDialog createLabelDialog (LabelEntry label, boolean edit) {

      WidgetDialog dialog = createNodeDialog (label, "label", edit);
      StringField textField =
         (StringField)dialog.addWidget ("Label title: ", label, "title");
      if (edit) {
         addFontWidgets (dialog, label);
      }
      addEnableOnNonNullText (dialog, textField, edit ? "Done" : "Add");
      dialog.pack();
      return dialog;
   }

   WidgetDialog createMenuDialog (MenuEntry menuEntry, boolean edit) {

      WidgetDialog dialog = createNodeDialog (menuEntry, "submenu", edit);
      StringField textField =
         (StringField)dialog.addWidget ("Menu title: ", menuEntry, "title");
      if (edit) {
         addMenuWidgets (dialog, menuEntry);
      }
      addEnableOnNonNullText (dialog, textField, edit ? "Done" : "Add");
      dialog.pack();
      return dialog;
   }

   static String getTailName (String str) {
      int lastDot = str.lastIndexOf ('.');
      if (lastDot != -1) {
         return str.substring (lastDot+1);
      }
      else {
         return str;
      }
   }
   
   JFileChooser getOrCreateMenuFileChooser () {
      if (myMenuFileChooser == null) {
         JFileChooser chooser = new JFileChooser();
         FileFilter filter = new GenericFileFilter ("xml", "XML files (*.xml)");
         chooser.addChoosableFileFilter (filter);
         chooser.setFileFilter (filter);
         if (myAlternateFile != null) {
            chooser.setSelectedFile (myAlternateFile);
         }
         else if (myDefaultFile != null) {
            chooser.setCurrentDirectory (myDefaultFile.getParentFile());
         }
         myMenuFileChooser = chooser;
      }
      return myMenuFileChooser;
   }

   private void saveMenu() {
      if (myDefaultFile != null) {
         try {
            myModelMenu.write (myDefaultFile);
            clearContentsModified();
         }
         catch (Exception e) {
            e.printStackTrace();
            GuiUtils.showError (this, "Error writing "+myDefaultFile); 
         }
      }
   }

   private void reloadMenu() {
      if (myDefaultFile != null) {
         try {
            myModelMenu.read (myDefaultFile);
            clearContentsModified();
            clearUndoCommands();
            notifyTreeStructureChanged (getRootMenuEntry());
         }
         catch (Exception e) {
            e.printStackTrace();
            GuiUtils.showError (this, "Error reading "+myDefaultFile); 
         }
      }
   }

   private void saveMenuTo() {
      JFileChooser chooser = getOrCreateMenuFileChooser();
      if (chooser.showDialog(this, "Save") ==
          JFileChooser.APPROVE_OPTION) {
         File file = chooser.getSelectedFile();
         if (file.getName().indexOf ('.') == -1) {
            // no extension, so add .xml
            file =  new File(file.getPath() + ".xml");
         }
         if (file.exists() && !GuiUtils.confirmOverwrite (this, file)) {
            return;
         }     
         try {
            myModelMenu.write (file);
            myAlternateFile = file;
         }
         catch (Exception e) {
            System.out.println (e);
            GuiUtils.showError (this, "Error writing "+file); 
         }
      }
   }

   private void loadMenuFrom() {
      JFileChooser chooser = getOrCreateMenuFileChooser();
      if (chooser.showDialog(this, "Load") ==
          JFileChooser.APPROVE_OPTION) {
         File file = chooser.getSelectedFile();
         if (file.getName().indexOf ('.') == -1) {
            // no extension, so add .xml
            file =  new File(file.getPath() + ".xml");
         }
         try {
            myModelMenu.read (file);
            markContentsEdited();
            clearUndoCommands();
            myAlternateFile = file;
            notifyTreeStructureChanged (getRootMenuEntry());
         }
         catch (Exception e) {
            System.out.println (e);
            GuiUtils.showError (this, "Error reading "+file); 
         }
      }
   }

   private void restoreDefaultMenu() {
      myModelMenu.createDefaultFiles();
      reloadMenu();      
   }

   public void dispose() {
      super.dispose();
      clearUndoCommands();
   }

}
/**


   1) adding or removing items from a single menuEntry

   fireTreeStructureChanged (menuEntry);
   invalidateMenusToRoot (menuEntry);
   root.updateComponent();

   2) adding or removing items from a collection of menuEntrys

   for (each menuEntry) {
      fireTreeStructureChanged (menuEntry);
      invalidateMenusToRoot (menuEntry);
   }
   root.updateComponent();

   3) Shifting nodes within a menuEntry

   fireTreeStructureChanged (menuEntry);
   menuEntry.invalidateMenu();
   menuEntry.updateComponent();

   4) Reloading a model

   fireTreeStructureChanged (root);
   //root.invalidateMenu();
   //root.updateComponent();

   

 */
