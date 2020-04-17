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
import javax.swing.tree.*;
import java.util.*;

public class NavPanelNode {
   ModelComponent myComponent;

   NavPanelNode myParent;
   Object[] myChildList;

   boolean myUnnamedVisibleP;
   int myNumNamed;
   // Number of children should equal the length of the childList. Need to keep
   // the number cached because JTree will sometimes ask for the number of
   // children even after the node is collapsed and myChildLiist has been
   // cleared.
   int myNumChildren = -1;

   NavPanelTreeModel myModel;

   public NavPanelNode(ModelComponent c, NavPanelNode parent,
	 NavPanelTreeModel model) {
      myComponent = c;
      myParent = parent;
      myChildList = null; // initialize on demand
      myNumChildren = -1;
      myUnnamedVisibleP = false;
      myNumNamed = 0;
      myModel = model;
   }

   // private void debug (String msg) {
   //    if (myComponent != null && myComponent.getName() != null &&
   //        myComponent.getName().equals ("particles")) {
   //       System.out.println (msg);
   //    }
   // }

   private void buildChildListIfNeeded() {
      if (myChildList == null) {
	 buildChildList();
      }
   }

   public Object[] clearChildList() {
      Object[] list = myChildList;
      myChildList = null;
      myNumChildren = -1;
      return list;
   }
   
   /**
    * Remove the children when a node is collapsed, but keep the
    * myNumChildren cached because JTree may still want to know
    * the number of "real" children after the node has been collapsed.
    */
   public void deallocateChildList() {
      myChildList = null;
   }

   /**
    * Remove the specified child from the list of this nodes children. Set that
    * nodes parent to null.
    * 
    * @param child
    *           The node to remove.
    */
   public void removeChild(Object child) {
      for (int i = 0; i < myChildList.length; i++) {
	 if (myChildList[i] == child) {
	    if (!myModel.isLeaf(child)) ((NavPanelNode) child).myParent = null;

	    myChildList[i] = null;
	 }
      }

      // get rid of the null item in the child list
      Object[] newChildList = new Object[myChildList.length - 1];
      int j = 0;

      for (int i = 0; i < myChildList.length; i++) {
	 if (myChildList[i] != null) {
	    newChildList[j] = myChildList[i];
	    j++;
	 }
      }

      myChildList = newChildList;
      //myNumChildren--;
   }

   /**
    * Insert the specified child into this nodes list of children if the child
    * list has been initialized. If it is a NavPanelNode set this node as its
    * parent.
    * 
    * @param child
    *           The node to insert.
    */
   public boolean insertChild(Object child) {
      if (myChildList != null) {
	 int numChildren = myChildList.length + 1;

	 Object[] newChildList = new Object[numChildren];

	 // copy the list of children
	 for (int i = 0; i < myChildList.length; i++)
	    newChildList[i] = myChildList[i];

	 // add the new node to the end
	 newChildList[newChildList.length - 1] = child;

	 myChildList = newChildList;

	 // set the parent of the new child if it is a nav panel node
	 if (!myModel.isLeaf(child)) ((NavPanelNode) child).myParent = this;

	 return true;
      }

      return false;
   }

   /**
    * Returns the index of the specified child in this nodes child array. If the
    * specified node is not a child of this node, returns -1.
    * 
    * @param child
    *           The node to get the index of.
    */
   public int getIndex(Object child) {
      for (int i = 0; i < myChildList.length; i++) {
	 if (myChildList[i] == child) return i;
      }

      return -1;
   }

   private boolean isDisplayable (ModelComponent c) {
      return !myModel.nodeShouldBeHidden (c);
   }

   private Object getDisplayable (ModelComponent c) {
      if (myModel.nodeShouldBeHidden (c)) {
         return null;
      }
      else {
         CompositeComponent cc = getComposite(c);
         if (cc != null) {
            return new NavPanelNode(c, this, myModel);
         }
         else {
            return c;
         }
      }
   }

   public boolean isUnnamedVisible() {
      return myUnnamedVisibleP;
   }

   private int[] createIndexList(int num) {
      int[] list = new int[num];
      for (int i = 0; i < num; i++) {
	 list[i] = i;
      }
      return list;
   }

   public void setUnnamedVisible(boolean visible) {
      buildChildListIfNeeded();
      if (visible != myUnnamedVisibleP && 
          myNumNamed < myChildList.length) {
	 myUnnamedVisibleP = visible;
	 //clearChildList();
	 myModel.nodeStructureChanged(this);
      }
   }

   private CompositeComponent getComposite (ModelComponent c) {
      CompositeComponent ccomp = null;
      if (c instanceof CompositeComponent) {
         return (CompositeComponent)c;
      }
      //
      // uncomment to allow NavPanel to recursively descend through
      // references to composites:

      // else if (c instanceof ReferenceComponent) {
      //    ReferenceComponent rcomp = (ReferenceComponent)c;
      //    if (rcomp.getReference() instanceof CompositeComponent) {
      //       return (CompositeComponent)rcomp.getReference();
      //    }
      // }
      return null;
   }      

   /**
    * Returns the number of child components for the Component
    * associated with this node.
    */
   protected int numChildComponents() {
      CompositeComponent ccomp = getComposite(myComponent);
      if (ccomp != null) {
         return ccomp.numComponents();
      }
      else {
         return 0;
      }
   }
   
   /**
    * Returns the idx-th child component of the Component
    * associated with this node.
    */
   protected ModelComponent getChildComponent (int idx) {
      CompositeComponent ccomp = getComposite(myComponent);
      if (ccomp != null) {
         return ccomp.get(idx);
      }
      else {
         return null;
      }
   }
   
   /**
    * Returns the display mode associated with this node.
    */
   protected NavpanelDisplay getDisplayMode () {
      CompositeComponent ccomp = getComposite(myComponent);
      if (ccomp != null) {
         return ccomp.getNavpanelDisplay();
      }
      else {
         return NavpanelDisplay.NORMAL;
      }
   }

   public String getName (ModelComponent comp) {
      String name = comp.getName();
      if (name == null) {
         if (comp instanceof ReferenceComp) {
            ModelComponent ref = ((ReferenceComp)comp).getReference();
            if (ref != null) {
               name = NavPanelRenderer.getReferenceName (ref);
            }
         }
      }
      return name;
   }

   private class ComponentNamePair {
      String name;
      ModelComponent comp;
      
      ComponentNamePair (ModelComponent comp, String name) {
         this.name = name;
         this.comp = comp;
      }
   }

   private class ComponentNameComparator
      implements Comparator<ComponentNamePair> {
      public int compare (ComponentNamePair c1, ComponentNamePair c2) {
         return c1.name.compareTo (c2.name);
      }
   }
   
   protected Object[] buildChildList() {

      int numChildComps = numChildComponents();
      if (numChildComps > 0) {
         ArrayList<ComponentNamePair> namedChildren =
            new ArrayList<ComponentNamePair>();

	 int numChildren = 0;

	 int numSelected = 0;
	 int numDisplayable = 0;
	 for (int i = 0; i < numChildComps; i++) {
	    ModelComponent c = getChildComponent (i);
	    if (isDisplayable(c)) {
	       numDisplayable++;
               String name = getName (c);
               if (name != null) {
                  namedChildren.add (new ComponentNamePair (c, name));
	       }
	       if (c.isSelected()) {
		  numSelected++;
	       }
	    }
	 }
         myNumNamed = namedChildren.size();
	 // number of named and unnamed components that we always show
	 int numAlwaysVisible = myNumNamed
	       + Math.min(NavigationPanel.myUnnamedVisibleLimit, numDisplayable
		     - myNumNamed);

	 if (numAlwaysVisible == numDisplayable) {
	    numChildren = numDisplayable;
	 }
         else if (myUnnamedVisibleP) {
	    numChildren = numDisplayable + 1;
	 }
         else {
	    numChildren = numAlwaysVisible + 1;
	 }
	 myChildList = new Object[numChildren];
	 TreePath newSelectPaths[] = new TreePath[numSelected];
	 int is = 0; // index for select paths
	 int in = 0; // index for named components
	 int iu = myNumNamed; // index for unnamed components
	 if (iu == numAlwaysVisible) {
	    iu++;
	 }

         if (getDisplayMode() == NavpanelDisplay.ALPHABETIC) {
            // sort the list of named children
            Collections.sort (namedChildren, new ComponentNameComparator());
         }
         for (int i=0; i<myNumNamed; i++) {
            ModelComponent c = namedChildren.get(i).comp;
            Object obj = getDisplayable(c);
            myChildList[i] = obj;
            if (c.isSelected()) {
               newSelectPaths[is++] = getTreePath(this, obj);
            }
         }
	 for (int i = 0; i < numChildComps; i++) {
	    ModelComponent c = getChildComponent (i);
	    Object obj;
	    if (c.getName() != null) {
	       // if ((obj = getDisplayable(c)) != null) {
	       //    myChildList[in++] = obj;
	       //    if (c.isSelected()) {
	       //       newSelectPaths[is++] = getTreePath(this, obj);
	       //    }
	       // }
	    }
            else if (myUnnamedVisibleP || iu < numChildren) {
	       if ((obj = getDisplayable(c)) != null) {
		  myChildList[iu++] = obj;
		  if (iu == numAlwaysVisible) {
		     iu++;
		  }
		  if (c.isSelected()) {
		     newSelectPaths[is++] = getTreePath(this, obj);
		  }
	       }
	    }
	 }
	 if (numAlwaysVisible != numDisplayable) {
	    myChildList[numAlwaysVisible] = new UnnamedPlaceHolder(this);
	 }
	 if (numSelected > 0) {
	    myModel.addSelectionPaths(newSelectPaths);
	 }
      }
      else {
         myChildList = new Object[0];
      }
      myNumChildren = myChildList.length;
      return myChildList;
   }

   boolean hasUnnamedChildren() {
      return myNumNamed < numChildComponents();
   }

   int numChildren() {
      if (myNumChildren == -1) {
         buildChildListIfNeeded();
         myNumChildren = myChildList.length;
      }
      return myNumChildren;
   }

   public int getIndexOfChild(Object child) {
      buildChildListIfNeeded();
      for (int i = 0; i < myChildList.length; i++) {
	 if (child == myChildList[i]) { return i; }
      }
      return -1;
   }

   public boolean isChildListExpanded() {
      return myChildList != null;
   }

   public static ModelComponent getNodeComponent(Object obj) {
      if (obj instanceof NavPanelNode) {
	 return ((NavPanelNode) obj).myComponent;
      } else if (obj instanceof ModelComponent) {
	 return (ModelComponent) obj;
      } else {
	 return null;
      }
   }

   public Object getChild(ModelComponent comp) {
      // XXX maybe make this more efficient by using a
      buildChildListIfNeeded();
      for (int i = 0; i < myChildList.length; i++) {
	 Object obj = myChildList[i];
	 if (obj instanceof ModelComponent) {
	    if ((ModelComponent) obj == comp) { return obj; }
	 } else if (obj instanceof NavPanelNode) {
	    if (((NavPanelNode) obj).myComponent == comp) { return obj; }
	 }
      }
      return null;
   }

   void printChildren() {
      buildChildListIfNeeded();
      for (int i = 0; i < myChildList.length; i++) {
	 Object obj = myChildList[i];
	 if (obj instanceof ModelComponent) {
	    System.out.println(((ModelComponent) obj).getName());
	 } else if (obj instanceof NavPanelNode) {
	    System.out.println(((NavPanelNode) obj).myComponent.getName());
	 }
      }
   }

   public Object getChild(int idx) {
      buildChildListIfNeeded();
      if (idx >= 0 && idx < myChildList.length) {
	 Object obj = myChildList[idx];
	 return obj;
      } else {
	 System.out.println("getChild " + myComponent.getName() + " null");
	 return null;
      }
   }

   TreePath getTreePath() {
      LinkedList<Object> objs = new LinkedList<Object>();
      for (NavPanelNode node = this; node != null; node = node.myParent) {
	 objs.addFirst(node);
      }
      return new TreePath(objs.toArray(new Object[0]));
   }

   TreePath getTreePath(Object obj) {
      LinkedList<Object> objs = new LinkedList<Object>();
      objs.addFirst (obj);
      for (NavPanelNode node = this; node != null; node = node.myParent) {
	 objs.addFirst(node);
      }
      return new TreePath(objs.toArray(new Object[0]));
   }

   static TreePath getTreePath(NavPanelNode parent, Object obj) {
      LinkedList<Object> objs = new LinkedList<Object>();
      objs.addFirst(obj);
      for (NavPanelNode node = parent; node != null; node = node.myParent) {
	 objs.addFirst(node);
      }
      return new TreePath(objs.toArray(new Object[0]));
   }

   /**
    * Returns true if a TreePath corresponds to an actual path in the
    * model component hierarchy. 
    */
   public static boolean isCanonicalPath (TreePath path) {
      int k = path.getPathCount()-1;
      ModelComponent comp = getNodeComponent (path.getPathComponent(k));
      if (comp == null) {
         return false;
      }
      while (--k > 0) {
         ModelComponent parent = getNodeComponent (path.getPathComponent(k));
         if (parent == null || comp.getParent() != parent) {
            return false;
         }
         comp = parent;
      }
      return true;
   }

   /**
    * Recursively print the expanded nodes which are descendants of this node.
    */
   public void printExpandedNodes () {
      ModelComponent c = myComponent;
      if (c != null) {
         System.out.println (ComponentUtils.getPathName (c));
      }
      if (isChildListExpanded()) {
         for (int i=0; i<numChildren(); i++) {
            Object child = getChild(i);
            if (child instanceof NavPanelNode) {
               ((NavPanelNode)child).printExpandedNodes();
            }
            else if (child instanceof ModelComponent) {
               System.out.println (
                  ComponentUtils.getPathName ((ModelComponent)child));
            }
         }
      }
   }     

//   static void printPath(TreePath path) {
//      for (int i = 0; i < path.getPathCount(); i++) {
//	 Object obj = path.getPathComponent(i);
//	 if (obj instanceof NavPanelNode) {
//	    System.out.println(" NPN "
//		  + ((NavPanelNode) obj).myComponent.getName());
//	 } else if (obj instanceof ModelComponent) {
//	    System.out.println(" MC " + ((ModelComponent) obj).getName());
//	 } else {
//	    System.out.println(" " + obj.getClass().getName());
//	 }
//      }
//   }
}
