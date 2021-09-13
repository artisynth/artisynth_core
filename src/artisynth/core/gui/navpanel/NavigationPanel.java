/**
 * Copyright (c) 2014, by the Authors: Chad Decker (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
/**
 * The following implementation is for Artisynth's Navigation Bar,
 * which allows a user to be able to Navigate through a model and
 * edit the pokeable components of the model itself, and to adjust
 * constraints.
 * 
 * Version 1.3 includes support for the probe editor package which
 * enables the add probe option in the popup menu options when a
 * pokeable component has been selected.
 * 
 * @author	Decker Chad
 * @version 1.3 - Feb. 23, 2006
 * @version 1.4 - April 20th,2006 NavPanel is modified to be a singleton object.
 * @author andreio Aug 20/2006 John and Andrei rewritten then class.
 */

package artisynth.core.gui.navpanel;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Component;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.JMenuItem;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeSelectionListener;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.plaf.IconUIResource;

import maspack.widgets.ButtonMasks;
import maspack.widgets.GuiUtils;
import maspack.util.PathFinder;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.modelbase.*;
import artisynth.core.modelbase.CompositeComponent.NavpanelDisplay;
import artisynth.core.workspace.RootModel;

public class NavigationPanel extends JScrollPane {
   private static final long serialVersionUID = 1L;

   boolean debug = false;

   static ImageIcon myTreeCollapsedIcon;
   static ImageIcon myTreeExpandedIcon;

   // the JTree structure within which the model components
   // and constraints are organized
   private JTree tree;

   private TreeSelectionModel myTreeSelectionModel;

   private NavPanelSelectionListener treeListener;

   private boolean expanded = false;

   private int myLeftBorderWidth = -1; // -1 means uninitialized
   
   private HashMap<String,Boolean> map = new HashMap<String,Boolean>();

   private ArrayList<TreeSelectionListener> mySelectionListeners;

   private SelectionManager mySelectionManager;

   private NavPanelTreeModel myTreeModel;

   protected boolean myHideEmptyComponentsP = true;

   static int myUnnamedVisibleLimit = 100;

   private boolean myLinesEnabled = false;

   public boolean getLinesEnabled() {
      return myLinesEnabled;
   }

   public void setLinesEnabled (boolean enabled) {
      if (myLinesEnabled != enabled) {
         myLinesEnabled = enabled;
         if (tree != null) {
            tree.putClientProperty (
               "JTree.lineStyle", enabled ? "Angled" : "None");
            tree.repaint();
         }
      }
   }

   public void setHideEmptyComponents (boolean enable) {
      if (enable != myTreeModel.getHideEmptyComponents()) {
         myTreeModel.setHideEmptyComponents (enable);
         updateStructure (myTreeModel.getRoot().myComponent);
      }
   }

   public boolean getHideEmptyComponents () {
      return myTreeModel.getHideEmptyComponents();
   }

   public static void setUnnamedVisibleLimit(int limit) {
      myUnnamedVisibleLimit = limit;
   }

   public static int getUnnamedVisibleLimit() {
      return myUnnamedVisibleLimit;
   }

   public void setSelectionManager(SelectionManager slectionManager) {
      mySelectionManager = slectionManager;
   }

   protected void initializeIcons() {
      if (myTreeCollapsedIcon == null) {
         String iconDir = PathFinder.getSourceRelativePath (
            getClass(), "../icon/");
         myTreeCollapsedIcon = GuiUtils.loadIcon(iconDir+"treeCollapsed.png");
         myTreeExpandedIcon = GuiUtils.loadIcon(iconDir+"treeExpanded.png");

         UIManager.put (
            "Tree.collapsedIcon", new IconUIResource(myTreeCollapsedIcon));
         UIManager.put (
            "Tree.expandedIcon", new IconUIResource (myTreeExpandedIcon));
      }
   }

   public NavigationPanel () {
      //this.parent = parent;
      initializeIcons();
      mySelectionListeners = new ArrayList<TreeSelectionListener>(8);
      setAlignmentX(0);
   }

   public void addSelectionListener(TreeSelectionListener l) {
      mySelectionListeners.add(l);
      if (tree != null) {
         tree.addTreeSelectionListener(l);
      }
   }

   public void removeSelectionListener(TreeSelectionListener l) {
      mySelectionListeners.remove(l);
      if (tree != null) {
         tree.removeTreeSelectionListener(l);
      }
   }

   public void expandAll(JTree tree) {
      tree.expandRow(0);
      tree.expandRow(1);
   }

   public void setExpanded(boolean expand) {
      expanded = expand;
   }

   public boolean isExpanded() {
      return expanded;
   }

   public JTree getTree() {
      return tree;
   }

   /**
    * Don't use this at the moment. It's just here in case we want
    * an "empty" navpanel.
    */
   public void unloadModel() {
      if (tree != null) {
         tree = null;
         myTreeSelectionModel = null;
         setEmptyView();
      }
   }
   
   private void setEmptyView() {
      JPanel panel = new JPanel();
      panel.setBackground (Color.WHITE);
      panel.setPreferredSize (new Dimension(100, 100));
      setViewportView (panel);
   }

   private void createTree(RootModel rootModel) {

      TreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
      myTreeModel = new NavPanelTreeModel(rootModel, selectionModel);
      tree = new JTree(myTreeModel);
      if (!myLinesEnabled) {
         tree.putClientProperty ("JTree.lineStyle", "None");
      }

      // add a mouse listener to the tree
      tree.addMouseListener(new TreeMouseListener());

      tree.setSelectionModel(selectionModel);
      for (TreeSelectionListener l : mySelectionListeners) {
         tree.addTreeSelectionListener(l);
      }

      treeListener = new NavPanelSelectionListener(myTreeModel);

      myTreeSelectionModel = tree.getSelectionModel();
      myTreeSelectionModel
         .setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

      // adding listeners
      tree.addTreeSelectionListener(treeListener);
      tree.addTreeExpansionListener(new NavPanelExpansionListener());
      tree.addTreeWillExpandListener(new NavPanelWillExpandListener());

      final JPopupMenu menu = new JPopupMenu();

      // Create and add a menu item
      JMenuItem item = new JMenuItem("Item Label");
      // item.addActionListener(actionListener);
      menu.add(item);

      tree.setLargeModel(true);
      tree.setExpandsSelectedPaths(false);
      tree.setEditable (false);
      tree.setCellRenderer(new NavPanelRenderer());
      tree.setRootVisible(true);
      tree.setBorder (new EmptyBorder (2, 5, 0, 0));
      setViewportView(tree);
      expandAll(tree);
   }

   public void loadModel(RootModel rootModel) {
      map.clear();
      if (rootModel != null) {
         createTree(rootModel);
      }
      revalidate();
   }

   public void rebuildTree (RootModel rootModel) {
      if (rootModel != null) {
         createTree(rootModel);
         setViewportView(tree);
      }
      revalidate();
   }

   private boolean onlyExpandedPaths = true;

   void printNodes (NavPanelNode node) {
      ModelComponent c = node.myComponent;
      if (c != null) {
         System.out.println (ComponentUtils.getPathName (c));
      }
      if (node.isChildListExpanded()) {
         for (int i=0; i<node.numChildren(); i++) {
            Object child = node.getChild(i);
            if (child instanceof NavPanelNode) {
               printNodes ((NavPanelNode)child);
            }
            else if (child instanceof ModelComponent) {
               System.out.println (
                  ComponentUtils.getPathName ((ModelComponent)child));
            }
         }
      }
   }

   public void selectPath(ModelComponent c) {
      if (myTreeModel != null) {
         TreePath path = myTreeModel.componentTreePath(c, onlyExpandedPaths);
         if (path != null && tree != null) {
            tree.addSelectionPath(path);
         }
      }
   }

   public void unselectPath(TreePath path) {
      if (myTreeModel != null) {
         tree.removeSelectionPath(path);
      }
   }

   public void unselectPath(ModelComponent c) {
      if (myTreeModel != null) {
         TreePath path = myTreeModel.componentTreePath(c, onlyExpandedPaths);
         if (path != null && tree != null) {
            tree.removeSelectionPath(path);
         }
      }
   }

   public void unselectAllPaths() {
      if (tree != null && tree != null) {
	 tree.clearSelection();
      }
   }

   public boolean pathContainsReference (TreePath path) {
      for (int i=0; i<path.getPathCount(); i++) {
         Object obj = path.getPathComponent(i);
         if (obj instanceof ReferenceComp) {
            return true;
         }
         else if (obj instanceof NavPanelNode) {
            NavPanelNode node = (NavPanelNode)obj;
            if (node.myComponent instanceof ReferenceComp) {
               return true;
            }
         }
      }
      return false;
   }

   public void updateStructure (ModelComponent c) {
      if (myTreeModel != null) {
         TreePath path = myTreeModel.componentTreePath(c, onlyExpandedPaths);
         if (path == null || myTreeModel.nodeShouldBeHidden (c)) {
            if (c.getParent() != null) {
               // try getting the parent path instead, so we can send a
               // structure change to the parent. Might be that c was not
               // expanded because component was previously hidden
               path = myTreeModel.componentTreePath (
                  c.getParent(), onlyExpandedPaths);
            }
         }
         if (path != null) {
            NavPanelNode node = (NavPanelNode)path.getLastPathComponent();
            myTreeModel.nodeStructureChanged(node);
         }
      }
   }

   public boolean hasExpandedPath(ModelComponent c) {
      return myTreeModel.componentTreePath(c, true) != null;
   }

   /**
    * A class to listen for mouse events in the navigation panel on the JTree.
    */
   private class TreeMouseListener extends MouseInputAdapter {
      public void mousePressed(MouseEvent e) {
         displayPopup(e);
      }

      public void mouseReleased(MouseEvent e) {
         displayPopup(e);
      }

      /**
       * Make a component selection if a new component has been clicked on, then
       * display the popup for the currently selected component.
       * 
       * @param e
       * The mouse event.
       */
      private void displayPopup(MouseEvent e) {
         if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
            // if there is a new tree path for the location then select that
            // tree path first unless it is inside a pre-selected group
            TreePath selectedPath = tree.getPathForLocation(e.getX(), e.getY());

            // select the single node clicked on unless it is inside a group of
            // nodes that are already selected
            if (selectedPath != null && !tree.isPathSelected(selectedPath))
               tree.setSelectionPath(selectedPath);

            // display the popup for selected items
            mySelectionManager.displayPopup(e);
         }
      }
   }
   
   public int getDefaultDividerLoc() {
      if (expanded) {
         return (int)getPreferredSize().getWidth() + 10;
      }
      else {
         return getLeftBorderWidth();
      }
   }
   
   public int getLeftBorderWidth() {
      if (myLeftBorderWidth == -1) {
         if (getBorder() != null) {
            Insets insets = getBorder().getBorderInsets(this);
            myLeftBorderWidth = insets.left;
         }
         else {
            myLeftBorderWidth = 0;
         }
      }
      return myLeftBorderWidth;
   }
   
   public Dimension getPreferredSize() {
      if (expanded) {
         return super.getPreferredSize();
      }
      else {
         return new Dimension(0,0);
      }
   }

   public Dimension getMinimumSize() {
      if (expanded) {
         return new Dimension(0,0);//super.getMinimumSize();
      }
      else {
         return new Dimension(0,0);
      }
   }

}
