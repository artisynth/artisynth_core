/**
 * Copyright (c) 2014, by the Authors: Chad Decker (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.navpanel;

import artisynth.core.modelbase.*;
import artisynth.core.modelbase.CompositeComponent.NavpanelDisplay;
import artisynth.core.modelbase.ModelComponent.NavpanelVisibility;
import artisynth.core.workspace.RootModel;

import javax.swing.tree.*;
import javax.swing.event.*;

import java.util.*;

public class NavPanelTreeModel implements TreeModel {
   NavPanelNode myRoot;

   TreeSelectionModel mySelectionModel;
   protected boolean myHideEmptyComponentsP = true;

   ArrayList<TreeModelListener> myListeners;

   public NavPanelTreeModel(RootModel root, TreeSelectionModel selectionModel) {
      this();
      mySelectionModel = selectionModel;
      myRoot = new NavPanelNode(root, null, this);
   }

   public NavPanelTreeModel() {
      myListeners = new ArrayList<TreeModelListener>(10);
   }

   public void setHideEmptyComponents (boolean enable) {
      myHideEmptyComponentsP = enable;
   }

   public boolean getHideEmptyComponents () {
      return myHideEmptyComponentsP;
   }

   public boolean nodeShouldBeHidden (ModelComponent c) {
      switch (c.getNavpanelVisibility()) {
         case HIDDEN: {
            return true;
         }
         case ALWAYS: {
            return false;
         }
         default: {
            if (c instanceof CompositeComponent) {
               CompositeComponent cc = (CompositeComponent)c;
               return myHideEmptyComponentsP && cc.numComponents() == 0;
            }
            else {
               return false;
            }
         }
      }
      // return (cc.getNavpanelDisplay() == NavpanelDisplay.HIDDEN || 
      //          (myHideEmptyComponentsP && 
      //           cc.numComponents() == 0 &&
      //           !(cc instanceof ReferenceList)));
   }

   public void addTreeModelListener(TreeModelListener l) {
      myListeners.add(l);
   }

   public Object getChild(Object parent, int index) {
      if (parent instanceof NavPanelNode) {
	 return ((NavPanelNode) parent).getChild(index);
      } else {
	 return null;
      }
   }

   public int getChildCount(Object parent) {
      int numc;
      if (parent instanceof NavPanelNode) {
         NavPanelNode node = (NavPanelNode)parent;
         numc = node.numChildren();
      } else if (parent instanceof CompositeComponent) {
	 throw new IllegalStateException("getChildCount: "
	       + parent.getClass().getSimpleName()
	       + " should be a NavPanelNode");
      } else {
	 numc = 0;
      }
      return numc;
   }

   public int getIndexOfChild(Object parent, Object child) {
      if (parent instanceof NavPanelNode) {
	 return ((NavPanelNode) parent).getIndexOfChild(child);
      } else {
	 return -1;
      }
   }

   public NavPanelNode getRoot() {
      return myRoot;
   }

   public TreePath getRootPath() {
      return new TreePath(myRoot);
   }

   public boolean isLeaf(Object node) {
      return !(node instanceof NavPanelNode);
   }

   public void removeTreeModelListener(TreeModelListener l) {
      myListeners.remove(l);
   }

   public void valueForPathChanged(TreePath path, Object newValue) {
      for (TreeModelListener l : myListeners) {}
   }

   /**
    * Remove the node at the end of the specified path.
    * 
    * @param path path from which node should be removed
    */
   public void removeChildAtPathEnd(TreePath path) {
      if (path != null) {
	 // get the child and the parent node
	 Object child = path.getLastPathComponent();
	 System.out.println("last path elem: " + child);
	 NavPanelNode parentNode = (NavPanelNode) path.getParentPath()
	       .getLastPathComponent();

	 // get the index of the child
	 int[] childIndices = new int[1];
	 childIndices[0] = parentNode.getIndex(child);

	 Object[] children = new Object[1];
	 children[0] = child;

	 // remove the child from the parent
	 parentNode.removeChild(child);

	 // notify all listeners that the node was removed from the parent
	 nodesWereRemoved(parentNode, childIndices, children);
      }
   }

   /**
    * Insert a child object to the node at the end of the specified path, if the
    * path does not end with a leaf node
    * 
    * @param child
    *           The child object to insert
    * @param parentPath
    *           The path to the parent
    * @return True if the parent is valid and the child is inserted, false
    *         otherwise.
    */
   public boolean insertChildAtPathEnd(Object child, TreePath parentPath) {
      Object parent = parentPath.getLastPathComponent();

      if (parent instanceof NavPanelNode) {
	 NavPanelNode parentNode = (NavPanelNode) parent;
	 boolean childInserted = parentNode.insertChild(child);

	 if (childInserted) {
	    // get the index of the child
	    int[] childIndices = new int[1];
	    childIndices[0] = parentNode.getIndex(child);

	    Object[] children = new Object[1];
	    children[0] = child;

	    nodesWereInserted(parentNode, childIndices, children);

	    return true;
	 }
      }

      return false;
   }

   public void nodesWereRemoved(NavPanelNode node, int[] childIndices,
	 Object[] children) {
      fireTreeNodesRemoved(this, node.getTreePath(), childIndices, children);
   }

   public void nodesWereInserted(NavPanelNode node, int[] childIndices,
	 Object[] children) {
      fireTreeNodesInserted(this, node.getTreePath(), childIndices, children);
   }

   public void nodeStructureChanged(NavPanelNode node) {
      if (node != null) {
         node.clearChildList();
	 fireTreeStructureChanged(this, node.getTreePath());
      }
   }

   protected void fireTreeNodesRemoved(Object source, TreePath path,
	 int[] childIndices, Object[] children) {
      TreeModelEvent e = null;
      for (int i = myListeners.size() - 1; i >= 0; i--) {
	 if (e == null) {
	    e = new TreeModelEvent(source, path, childIndices, children);
	 }
	 myListeners.get(i).treeNodesRemoved(e);
      }
   }

   protected void fireTreeNodesInserted(Object source, TreePath path,
	 int[] childIndices, Object[] children) {
      TreeModelEvent e = null;
      for (int i = myListeners.size() - 1; i >= 0; i--) {
	 if (e == null) {
	    e = new TreeModelEvent(source, path, childIndices, children);
	 }
	 myListeners.get(i).treeNodesInserted(e);
      }
   }

   protected void fireTreeStructureChanged(Object source, TreePath path) {
      TreeModelEvent e = null;
      for (int i = myListeners.size() - 1; i >= 0; i--) {
	 if (e == null) {
	    e = new TreeModelEvent(source, path);
	 }
	 myListeners.get(i).treeStructureChanged(e);
      }
   }

   protected TreePath componentTreePath(ModelComponent comp,
	 boolean onlyIfExpanded) {
      if (myRoot == null) {
         return null; 
      }
      // build path of model components
      LinkedList<ModelComponent> cpath = new LinkedList<ModelComponent>();
      for (ModelComponent c = comp; c != null; c = c.getParent()) {
	 cpath.addFirst(c);
      }
      Object[] pathObjs = new Object[cpath.size()];
      int i = 0;
      NavPanelNode parent = null;
      for (ModelComponent c : cpath) {
	 Object obj;
	 if (i == 0) {
	    if (myRoot.myComponent != c) {
               return null;
            }
	    parent = myRoot;
	    obj = myRoot;
	 } else {
	    if (onlyIfExpanded && !parent.isChildListExpanded()) {
               return null;
            }
	    obj = parent.getChild(c);
	    if (obj == null) {
               return null;
            }
	    if (obj instanceof NavPanelNode) {
	       parent = (NavPanelNode) obj;
	    }
	 }
	 pathObjs[i++] = obj;
      }
      return new TreePath(pathObjs);
   }

   void addSelectionPaths(TreePath[] paths) {
      mySelectionModel.addSelectionPaths(paths);
   }

   void removeSelectionPaths(TreePath[] paths) {
      mySelectionModel.removeSelectionPaths(paths);
   }

   public TreePath getComponentTreePath(ModelComponent comp) {
      return componentTreePath(comp, false);
   }

}
