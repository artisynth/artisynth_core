/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.util.*;
import java.io.*;
import maspack.util.*;

/**
 * Container class for organizing property information into a tree.
 */
public class PropTreeCell {
   PropTreeData myData;

   PropTreeCell myParent;
   PropTreeCell myFirstChild;
   PropTreeCell myLastChild;
   PropTreeCell next;
   PropTreeCell prev;

   public PropTreeCell (PropertyInfo info, Object value) {
      this();
      myData.setInfo (info);
      myData.setValue (value);
   }

   public PropTreeCell (PropTreeData data) {
      myData = data;
   }

   public PropTreeCell() {
      myData = new PropTreeData();
   }

   public PropTreeCell (PropTreeCell cell) {
      doCopy (cell, null);
   }

   public PropertyMode getMode() {
      return myData.myMode;
   }

   public void setMode (PropertyMode mode) {
      myData.myMode = mode;
   }

   public Object getValue() {
      return myData.myValue;
   }

   public void setValue (Object value) {
      myData.myValue = value;
   }

   public PropertyInfo getInfo() {
      return myData.myInfo;
   }

   public PropTreeCell addChild (PropertyInfo info, Object value) {
      PropTreeCell child = new PropTreeCell (info, value);
      addChild (child);
      return child;
   }

   /**
    * Adds the children of root to this cell. All children will be removed from
    * root.
    */
   public void addChildren (PropTreeCell root) {
      PropTreeCell child, nextChild;
      for (child = root.myFirstChild; child != null; child = nextChild) {
         nextChild = child.next;
         addChild (child);
      }
      root.removeAllChildren();
   }

   public void setData (PropertyInfo info, Object value) {
      myData.set (info, value);
   }

   public void setData (PropTreeData data) {
      myData.set (data);
   }

   public void addChild (PropTreeCell child) {
      child.next = null;
      child.prev = myLastChild;
      if (myFirstChild == null) {
         myFirstChild = child;
      }
      else {
         myLastChild.next = child;
      }
      myLastChild = child;
      child.myParent = this;
   }

   public boolean hasChildren() {
      return myFirstChild != null;
   }

   public PropTreeCell getFirstChild() {
      return myFirstChild;
   }

   public PropTreeCell getNext() {
      return next;
   }

   public void removeChild (PropTreeCell child) {
      if (child.prev == null) {
         myFirstChild = child.next;
      }
      else {
         child.prev.next = child.next;
      }
      if (child.next == null) {
         myLastChild = child.prev;
      }
      else {
         child.next.prev = child.prev;
      }
      child.myParent = null;
   }

   LinkedList<String> getNamePath() {
      LinkedList<String> namePath = new LinkedList<String>();
      PropTreeCell cell = this;
      while (cell != null && cell.myData.myInfo != null) {
         namePath.addFirst (cell.myData.myInfo.getName());
         cell = cell.myParent;
      }
      return namePath;
   }

   public void removeAllChildren() {
      myFirstChild = myLastChild = null;
   }

   void doCopy (PropTreeCell cell, PropTreeCell end) {
      myData = cell.myData;

      removeAllChildren();

      for (PropTreeCell child = cell.myFirstChild; child != null &&
              child != end; child = child.next) {
         addChild (copyTree (child));
      }
   }

   public void setLeafValuesVoid() {
      if (myFirstChild != null) {
         for (PropTreeCell child = myFirstChild; child != null; child =
            child.next) {
            child.setLeafValuesVoid();
         }
      }
      else {
         setValue (Property.VoidValue);
      }
   }

   /**
    * Add to this node's children a copy of all the children (and their data
    * objects) of cell.
    */
   public void addCopyOfChildrenAndData (PropTreeCell cell) {
      for (PropTreeCell child = cell.myFirstChild; child != null; child =
         child.next) {
         addChild (new PropTreeCell (new PropTreeData (child.myData)));
      }
   }

   /**
    * Make a copy of this cell and it's first level of children that share's the
    * same data cells as this cell.
    */
   public PropTreeCell copyChildren() {
      PropTreeCell copy = new PropTreeCell();
      copy.myData = myData;
      for (PropTreeCell child = myFirstChild; child != null; child = child.next) {
         copy.addChild (new PropTreeCell (child.myData));
      }
      return copy;
   }

   public static PropTreeCell copyTree (PropTreeCell cell) {
      return copyTreeUntil (cell, null);
   }

   /**
    * Returns a copy of the property tree whose root is this cell, while
    * preserving the original data objects.
    */
   public PropTreeCell copyTree() {
      PropTreeCell newRoot = new PropTreeCell (myData);
      for (PropTreeCell child = myFirstChild; child != null; child = child.next) {
         newRoot.addChild (child.copyTree());
      }
      return newRoot;
   }

   /**
    * Returns a complete copy of the property tree whose root is this cell,
    * including copies of all data objects.
    */
   public PropTreeCell copyTreeAndData() {
      PropTreeCell newRoot = new PropTreeCell (new PropTreeData (myData));
      for (PropTreeCell child = myFirstChild; child != null; child = child.next) {
         newRoot.addChild (child.copyTreeAndData());
      }
      return newRoot;
   }

   public static PropTreeCell copyTreeUntil (PropTreeCell cell, PropTreeCell end) {
      PropTreeCell copy = new PropTreeCell();
      copy.doCopy (cell, end);
      return copy;
   }

   public void detachFromParent() {
      if (myParent != null) {
         myParent.removeChild (this);
         myParent = null;
      }
   }

   public void printTree (PrintStream ps) {
      ps.print (treeString());
      ps.flush();
   }

   public PropTreeCell getChild (String name) {
      for (PropTreeCell child = myFirstChild; child != null; child = child.next) {
         if (child.getInfo() != null &&
             child.getInfo().getName().equals (name)) {
            return child;
         }
      }
      return null;
   }

   /**
    * Returns the index of this cell with respect to its parent, or -1 if it has
    * no parent.
    * 
    * @return index of this cell
    */
   public int getIndex() {
      if (myParent == null) {
         return -1;
      }
      int idx = 0;
      for (PropTreeCell child = myParent.myFirstChild;
           child != null; child = child.next) {
         if (child == this) {
            return idx;
         }
         idx++;
      }
      throw new InternalErrorException (
         "PropTreeCell @"+hashCode()+"('"+getInfo().getName()+
         "') not refered to by parent @"+myParent.hashCode()+
         "('"+myParent.getInfo().getName()+"')");
   }

   /**
    * Returns a path of indices that locate this cell starting from the root of
    * the hierarchy.
    */
   public int[] getIndexPath() {
      Stack<Integer> stack = new Stack<Integer>();
      PropTreeCell cell = this;
      int idx;
      while ((idx = cell.getIndex()) != -1) {
         stack.push (idx);
         cell = cell.getParent();
      }
      int[] idxList = new int[stack.size()];
      for (int k = 0; k < idxList.length; k++) {
         idxList[k] = stack.pop();
      }
      return idxList;
   }

   public PropTreeCell getDescendant (String path) {
      int idx = 0;
      PropTreeCell cell = this;

      while (idx < path.length()) {
         String name;
         int dotIdx = path.indexOf ('.', idx);
         if (dotIdx != -1) {
            name = path.substring (idx, dotIdx);
            idx = dotIdx + 1;
            if (idx == path.length()) {
               throw new IllegalArgumentException (
                  "path name should not end with a '.'");
            }
         }
         else {
            name = path.substring (idx);
            idx = path.length();
         }
         if ((cell = cell.getChild (name)) == null) {
            return null;
         }
      }
      return cell;
   }

   public PropTreeCell getParent() {
      return myParent;
   }

   public boolean removeDescendant (String path) {
      PropTreeCell cell = getDescendant (path);
      if (cell != null) {
         removeDescendant (cell);
         return true;
      }
      else {
         return false;
      }
   }

   public void removeDescendant (PropTreeCell cell) {
      // check the cell is a descendant
      PropTreeCell parent = cell.getParent();
      while (parent != null && parent != this) {
         parent = parent.getParent();
      }
      if (parent == null) {
         throw new IllegalArgumentException (
            "cell is not a descendant of this cell");
      }
      PropTreeCell child = cell;
      parent = child.getParent();
      do {
         parent.removeChild (child);
         child = parent;
         parent = parent.getParent();
      }
      while (child != this && !child.hasChildren());
   }

   public int numChildren() {
      int num = 0;
      for (PropTreeCell child = myFirstChild; child != null; child = child.next) {
         num++;
      }
      return num;
   }

   // /**
   // * Finds or creates the property host associated with the property path
   // * started by this cell.
   // *
   // * @param topHost top level property host.
   // */
   // HasProperties getOrCreateHost (HasProperties topHost)
   // {
   // // run down the path and find the value and info structures
   // // for the specific host
   // HasProperties curHost = topHost;
   // for (PropTreeCell cell=myFirstChild; cell!=null; cell=cell.myFirstChild)
   // { PropertyInfo info = PropertyUtils.getPropertyInfo (
   // curHost, cell.getInfo().getName());
   // curHost = getOrCreateSubHost (info, curHost);
   // }
   // return curHost;
   // }

   public void addLeafProperties (HasProperties host) {
      for (PropertyInfo info : host.getAllPropertyInfo()) {
         PropTreeCell child =
            addChild (info, PropertyUtils.getValue (info, host));
         if (info.isInheritable()) {
            child.setMode (PropertyUtils.getMode (info, host));
         }
         if (CompositeProperty.class.isAssignableFrom (info.getValueClass())) {
            HasProperties subhost = (HasProperties)child.myData.myValue;
            if (subhost != null) {
               child.addLeafProperties (subhost);
            }
         }
      }
   }

   // /**
   // * Wrapper method to create an inheritable property handle
   // * from a <code>PropertyInfo</code> object and throw
   // * an <code>InternalErrorException</code> if this fails.
   // *
   // * @param info property information
   // * @param host host for which handle is to be created
   // * @return inheritable property handle
   // */
   // InheritableProperty createInheritableHandle (
   // PropertyInfo info, HasProperties host)
   // {
   // try
   // { return (InheritableProperty)info.createHandle(host);
   // }
   // catch (ClassCastException e)
   // { throw new InternalErrorException (
   // "property '" + info.getName() +
   // "'described as inheritable when it is not");
   // }
   // }

   /**
    * Recursively populates this cell with all the explicitly set inheritable
    * properties accessible from a given host.
    * 
    * @param host
    * property host
    */
   public void addExplicitPropertyTree (HasProperties host) {
      for (PropertyInfo info : host.getAllPropertyInfo()) {
         if (info.isInheritable()) {
            if (PropertyUtils.getMode (info, host) == PropertyMode.Explicit) {
               addChild (info, PropertyUtils.getValue (info, host));
            }
         }
         else if (CompositeProperty.class.isAssignableFrom (
                     info.getValueClass())) {
            HasProperties chost =
               (HasProperties)PropertyUtils.getValue (info, host);
            if (chost != null) {
               PropTreeCell child = addChild (info, chost);
               child.addExplicitPropertyTree (chost);
            }
         }
      }
   }

   /**
    * Extends this property tree all the way back to the hierarchy node
    * associated with a particular host. This is done by climbing the property
    * tree back to the hierarchy node and prepending tree cells as required.
    * 
    * @param host
    * property host associated with the target hierarchy node
    * @return new root cell for the extended property tree
    */
   public PropTreeCell extendToHierarchyNode (HasProperties host) {
      PropTreeCell retCell = this;
      Object obj = host;
      while (!(obj instanceof HierarchyNode) &&
             (obj instanceof CompositeProperty)) {
         CompositeProperty cprop = (CompositeProperty)obj;
         retCell.myData = new PropTreeData();
         retCell.myData.setInfo (cprop.getPropertyInfo());
         PropTreeCell newCell = new PropTreeCell();
         newCell.addChild (retCell);
         retCell = newCell;
         obj = cprop.getPropertyHost();
      }
      return retCell;
   }

   /**
    * Creates a new path of property cells from this cell all the way back to
    * the root cell. This is done by repeatedly getting a cell's parent until
    * null is found. This routine is different from {@link
    * #extendToHierarchyNode extendToHierarchyNode} in that this cell is assumed
    * to belong to an existing tree and therefore should not be modified.
    */
   public PropTreeCell createPathToRoot() {
      PropTreeCell cell = new PropTreeCell (this);
      PropTreeCell origCell = this;
      while (origCell.myParent != null) {
         PropTreeCell newCell = new PropTreeCell (origCell.myParent);
         newCell.addChild (cell);
         cell = newCell;
         origCell = origCell.myParent;
      }
      return cell;
   }

   /**
    * Recursively populates this cell with all the non-explicitly set
    * inheritable properties accessible from a given host.
    * 
    * @param host
    * property host
    */
   public void addNonexplicitPropertyTree (HasProperties host) {
      for (PropertyInfo info : host.getAllPropertyInfo()) {
         if (info.isInheritable()) {
            if (PropertyUtils.getMode (info, host) != PropertyMode.Explicit) {
               addChild (info, PropertyUtils.getValue (info, host));
            }
         }
         else if (CompositeProperty.class.isAssignableFrom (
                     info.getValueClass())) {
            HasProperties chost =
               (HasProperties)PropertyUtils.getValue (info, host);
            if (chost != null) {
               PropTreeCell child = addChild (info, chost);
               child.addNonexplicitPropertyTree (chost);
            }
         }
      }
   }

   public PropTreeCell copyTreeChanges (PropTreeCell oldTree) {
      PropTreeCell newRoot = new PropTreeCell (myData);
      for (PropTreeCell child = myFirstChild; child != null; child = child.next) {
         PropertyInfo info = child.getInfo();
         PropTreeCell oldChild = oldTree.findMatchingChild (info);
         if (oldChild != null) {
            Object newValue = child.getValue();
            Object oldValue = oldChild.getValue();

            if (CompositeProperty.class.isAssignableFrom (
                   info.getValueClass())) {
               if (newValue == null && oldValue != null) {
                  newRoot.addChild (new PropTreeCell (child.myData));
               }
               else if (newValue != null && oldValue == null) {
                  newRoot.addChild (child.copyTree());
               }
               else if (newValue != null && oldValue != null) {
                  PropTreeCell childChanges = child.copyTreeChanges (oldChild);
                  if (child.hasChildren()) {
                     newRoot.addChild (childChanges);
                  }
               }
            }
            else { // leaf nodes
               if (!PropertyUtils.equalValues (newValue, oldValue)) {
                  newRoot.addChild (new PropTreeCell (child.myData));
               }
               else if (child.getMode() != oldChild.getMode()) {
                  newRoot.addChild (new PropTreeCell (child.myData));
               }
            }
         }
      }
      return newRoot;
   }

   // /**
   // * Recursively adds to the children of this cell a copy of each branch of
   // * newTree which is present in oldTree but whose values differ. The copy
   // * preserves the original data objects.
   // */
   // public void addChangedPropertyTree (
   // PropTreeCell oldTree, PropTreeCell newTree)
   // {
   // for (PropTreeCell oldChild=tree1.myFirstChild; oldChild!=null;
   // oldChild=oldChild.next)
   // { PropetyInfo oldInfo = oldChild.getInfo();
   // Object oldValue = oldChild.getValue();
   // for (PropTreeCell newChild=tree2.myFirstChild; newChild!=null;
   // newChild=newChild.next)
   // { PropertyInfo newInfo = newChild.getInfo();
   // Object newValue = newChild.getValue();
   // if (PropertyUtils.propertiesMatch (oldInfo, newInfo))
   // { if (CompositeProperty.class.isAssignableFrom (
   // oldInfo.getValueClass()))
   // { if (newValue == null && oldValue != null)
   // { addChild (new PropTreeCell (newChild.getData()));
   // }
   // else if (newValue != null && oldValue == null)
   // { addChild (new PropTreeCell (newChild.getData()));

   // }

   // }
   // }
   // }
   // }
   // }

   /**
    * Finds the child of this cell whose property matches the specified property
    * information.
    */
   public PropTreeCell findMatchingChild (PropertyInfo info) {
      for (PropTreeCell child = myFirstChild; child != null; child = child.next) {
         if (PropertyUtils.propertiesMatch (child.getInfo(), info)) {
            return child;
         }
      }
      return null;
   }

   // public PropTreeCell findMatchingChild (PropTreeCell cell)
   // {
   // for (PropTreeCell child=myFirstChild; child!=null; child=child.next)
   // { // XXX should we use equality here?
   // if (cell.myData.myInfo == child.myData.myInfo)
   // { return child;
   // }
   // }
   // return null;
   // }

   /**
    * Finds the property contained by a host that matches a specified property.
    * 
    * @param host
    * property host in which to search
    * @param info
    * information for specified property
    * @return information for matching property, if found
    */
   public static PropertyInfo findMatchingProperty (
      HasProperties host, PropertyInfo info) {
      PropertyInfo matchingInfo =
         host.getAllPropertyInfo().get (info.getName());
      if (matchingInfo != null) {
         if (!PropertyUtils.propertiesMatch (matchingInfo, info)) {
            return null;
         }
      }
      // else if (matchingInfo.isInheritable())
      // { if (PropertyUtils.getMode (matchingInfo, host) ==
      // PropertyMode.Inactive)
      // { return null;
      // }
      // }
      return matchingInfo;
   }

   private HasProperties getSubHost (PropertyInfo info, HasProperties host) {
      // Property prop = info.createHandle (host);
      // Object value = prop.get();
      Object value = PropertyUtils.getValue (info, host);
      try {
         return (HasProperties)value;
      }
      catch (ClassCastException e) {
         throw new InternalErrorException ("intermediate property '"
         + info.getName() + "' not an instance of HasProperties");
      }
   }

   /**
    * Uses the values contained with this property tree to update any matching
    * non-explicit properties within a specific host.
    * 
    * <p>
    * This method will also return a property tree which is suitable for
    * propagation to descendant nodes within the hierarchy. The returned tree
    * may be this tree or a modified copy of this tree. In particular, if
    * <code>reduce</code> is true and the host contains matching non-explicit
    * properties, then a copy of this tree is made with the associated property
    * cells removed A returned value of <code>null</code> indicates an empty
    * tree and implies that propagation should stop.
    * 
    * @param host
    * property host to update
    * @param reduce
    * if true, creates a copy of this tree with matching properties removed
    * @return property tree for continuing the propagation further down the
    * hierarchy
    */
   public PropTreeCell updateTreeValuesInHost (
      HasProperties host, boolean reduce) {
      PropTreeCell retcell = this;
      for (PropTreeCell child = myFirstChild; child != null; child = child.next) {
         PropertyInfo info = findMatchingProperty (host, child.getInfo());
         if (info != null) {
            PropTreeCell childx = child;
            if (child.hasChildren()) {
               HasProperties subHost = getSubHost (info, host);
               if (subHost != null) {
                  childx = child.updateTreeValuesInHost (subHost, reduce);
               }
            }
            else if (info.isInheritable()) {
               if (PropertyUtils.getMode (info, host) != PropertyMode.Explicit) {
                  PropertyUtils.setInheritedValue (
                     info, host, child.getValue());
               }
               else {
                  childx = null;
               }
            }
            if (reduce) {
               if (childx != child) {
                  if (retcell == this) {
                     retcell = copyTreeUntil (this, child);
                  }
                  if (childx != null) {
                     retcell.addChild (childx);
                  }
               }
               else if (retcell != this) {
                  retcell.addChild (copyTree (child));
               }
            }
         }
      }
      if (retcell.hasChildren()) {
         return retcell;
      }
      else {
         return null;
      }
   }

   /**
    * Uses the values contained with this property tree to set any matching
    * properties within a specific destinate host.
    * 
    * <p>
    * If the tree contains any unexpanded composite properties, the source host
    * is used to provide these instead.
    * 
    * @param dstHost
    * property host to update
    * @param srcHost
    * source for undefined property values
    */
   public void setTreeValuesInHost (HasProperties dstHost, HasProperties srcHost) {
      for (PropTreeCell child = myFirstChild; child != null; child = child.next) {
         PropertyInfo info = findMatchingProperty (dstHost, child.getInfo());
         if (info != null) {
            if (child.hasChildren()) {
               HasProperties subDstHost = getSubHost (info, dstHost);
               HasProperties subSrcHost = getSubHost (info, srcHost);
               if (subDstHost == null) {
                  subDstHost = PropertyUtils.createInstance (info, dstHost);
                  PropertyUtils.setValue (info, dstHost, subDstHost);
                  // read this back in case of defensive copy
                  subDstHost =
                     (HasProperties)PropertyUtils.getValue (info, dstHost);
               }
               if (subDstHost != null) {
                  if (subSrcHost == null) {
                     throw new InternalErrorException (
                        "sub-destination host is " + subDstHost
                        + " but sub-source host is null");
                  }
                  child.setTreeValuesInHost (subDstHost, subSrcHost);
               }
            }
            else {
               Object value = child.getValue();
               if (value != Property.VoidValue) {
                  if (value == CompositeProperty.class) {
                     value = PropertyUtils.getValue (info, srcHost);
                  }
                  // System.out.println ("setting "+info.getName()+" " + value);
                  PropertyUtils.setValue (info, dstHost, value);
                  if (info.isInheritable()) {
                     PropertyUtils.setMode (info, dstHost, child.getMode());
                  }
               }
            }
         }
      }
   }

   /**
    * Sets cell values within this property tree from any explicitly-set
    * matching properties in a specific host. The corresponding cells are then
    * deleted.
    * 
    * @param host
    * property host
    */
   public void inheritTreeValuesFromHost (HasProperties host) {
      PropTreeCell nextChild;
      for (PropTreeCell child = myFirstChild; child != null; child = nextChild) {
         nextChild = child.next;
         PropertyInfo info = findMatchingProperty (host, child.getInfo());
         if (info != null) {
            if (child.hasChildren()) {
               HasProperties subHost = getSubHost (info, host);
               if (subHost != null) {
                  child.inheritTreeValuesFromHost (subHost);
                  if (!child.hasChildren()) {
                     removeChild (child);
                  }
               }
            }
            else if (info.isInheritable()) {
               if (PropertyUtils.getMode (info, host) == PropertyMode.Explicit) {
                  child.myData.myValue = PropertyUtils.getValue (info, host);
                  removeChild (child);
               }
            }
         }
      }
   }

   // static HasProperties getOrCreateAndAddSubHost (
   // PropertyInfo info, HasProperties host)
   // {
   // Object value = PropertyUtils.getValue (info, host);
   // if (value == null)
   // { value = PropertyUtils.createInstance (info, host);
   // PropertyUtils.setValue (info, host, value);
   // }
   // if (!(value instanceof CompositeProperty))
   // { throw new InternalErrorException (
   // "Property '"+info.getName()+
   // "' not an instance of CompositeProperty");
   // }
   // return (HasProperties)value;
   // }

   // static HasProperties getOrCreateSubHost (
   // PropertyInfo info, HasProperties host)
   // {
   // Object value = PropertyUtils.getValue (info, host);
   // if (value == null)
   // { value = PropertyUtils.createInstance (info, host);
   // }
   // if (!(value instanceof CompositeProperty))
   // { throw new InternalErrorException (
   // "Property '"+info.getName()+
   // "' not an instance of CompositeProperty");
   // }
   // return (HasProperties)value;
   // }

   /**
    * Gets the explicit value, if possible, within a specfic host for the
    * property path defined by this cell.
    * 
    * @param host
    * property host
    * @return explicit property value, or Property.VoidValue if the property is
    * not found.
    */
   public Object getExplicitValue (HasProperties host) {
      HasProperties curHost = host;

      for (PropTreeCell cell = myFirstChild; cell != null; cell =
         cell.myFirstChild) {
         PropertyInfo info = findMatchingProperty (curHost, cell.getInfo());
         if (info == null) {
            return Property.VoidValue; // matching property not found, quit
         }
         if (cell.myFirstChild == null) { // end of the path; see if the value
                                          // is explict
            if (info.isInheritable()) {
               if (PropertyUtils.getMode (info, curHost) == PropertyMode.Explicit) {
                  return PropertyUtils.getValue (info, curHost);
               }
            }
         }
         else {
            curHost = getSubHost (info, curHost);
            if (curHost == null) {
               return Property.VoidValue; // shouldn't happen but just in case
                                          // ...
            }
         }
      }
      return Property.VoidValue;
   }

   /**
    * Climbs a hierarchy to determine all inherited values for the property tree
    * defined by this cell.
    * 
    * @param node
    * base node whose ancestors are to supply the inheritance
    */
   public void inheritTreeValuesFromHierachy (HierarchyNode node) {
      PropTreeCell remaining = PropTreeCell.copyTree (this);
      HierarchyNode parent = node.getParent();
      while (parent != null && remaining.hasChildren()) {
         if (parent instanceof HasProperties) {
            remaining.inheritTreeValuesFromHost ((HasProperties)parent);
         }
         parent = parent.getParent();
      }
   }

   /**
    * Climbs a hierarchy to find the inherited value for the property path
    * defined by this cell.
    * 
    * @param node
    * base node whose ancestors are to supply the inheritance
    */
   public Object findInheritedValue (HierarchyNode node) {
      HierarchyNode parent = node.getParent();
      while (parent != null) {
         if (parent instanceof HasProperties) {
            Object value = getExplicitValue ((HasProperties)parent);
            if (value != Property.VoidValue) {
               return value;
            }
         }
         parent = parent.getParent();
      }
      return Property.VoidValue;
   }

   /**
    * Recursively descends this tree and, for any property whose editing
    * property is Single, sets its value to the default value. Returns true if
    * any default values were set.
    */
   public boolean setSingleEditDefaultValues() {
      boolean changed = false;
      if (hasChildren()) {
         for (PropTreeCell child = myFirstChild; child != null; child =
            child.next) {
            if (child.setSingleEditDefaultValues()) {
               changed = true;
            }
         }
      }
      else {
         PropertyInfo info = getInfo();
         if (info != null && info.getEditing() == PropertyInfo.Edit.Single) {
            setValue (info.getDefaultValue());
            if (info.isInheritable()) {
               setMode (info.getDefaultMode());
            }
            changed = true;
         }
      }
      return changed;
   }

   /**
    * Uses the values contained with this property tree to set any matching
    * properties within a specific destinate host.
    * 
    * <p>
    * If the tree contains any unexpanded composite properties, the source host
    * is used to provide these instead.
    * 
    * @param dstHost
    * property host to update
    * @param srcHost
    * source for undefined property values
    */
   public void setTreeSingleEditDefaultValuesInHost (
      HasProperties dstHost, HasProperties srcHost) {
      for (PropTreeCell child = myFirstChild; child != null; child = child.next) {
         PropertyInfo info = findMatchingProperty (dstHost, child.getInfo());
         if (info != null) {
            if (child.hasChildren()) {
               HasProperties subDstHost = getSubHost (info, dstHost);
               HasProperties subSrcHost = getSubHost (info, srcHost);
               if (subDstHost != null) {
                  if (subSrcHost == null) {
                     throw new InternalErrorException (
                        "sub-destination host is " + subDstHost
                        + " but sub-source host is null");
                  }
                  child.setTreeValuesInHost (subDstHost, subSrcHost);
               }
            }
            else {
               Object value = child.getValue();
               if (value == CompositeProperty.class) {
                  value = PropertyUtils.getValue (info, srcHost);
               }
               PropertyUtils.setValue (info, dstHost, value);
               if (info.isInheritable()) {
                  PropertyUtils.setMode (info, dstHost, child.getMode());
               }
            }
         }
      }
   }

   public int numLeafCells() {
      if (hasChildren()) {
         int num = 0;
         for (PropTreeCell child = myFirstChild; child != null; child =
            child.next) {
            num += child.numLeafCells();
         }
         return num;
      }
      else {
         return 1;
      }
   }

   public String treeString() {
      StringBuffer buf = new StringBuffer();
      treeString (buf);
      return buf.toString();
   }

   public String pathString() {
      StringBuilder builder = new StringBuilder (256);
      PropTreeCell cell = this;
      while (cell.getParent() != null) {
         if (cell != this) {
            builder.insert (0, '.');
         }
         builder.insert (0, cell.getPropName());
         cell = cell.getParent();
      }
      return builder.toString();
   }

   private void treeString (StringBuffer buf) {
      if (hasChildren()) {
         for (PropTreeCell child = myFirstChild; child != null; child =
            child.next) {
            child.treeString (buf);
         }
      }
      else if (myData != null) // leaf cell
      {
         Iterator<String> it = getNamePath().iterator();
         while (it.hasNext()) {
            buf.append (it.next());
            if (it.hasNext()) {
               buf.append (".");
            }
         }
         buf.append (" " + myData.myValue);
         if (myData.myMode != PropertyMode.Inactive) {
            buf.append (" " + myData.myMode);
         }
         buf.append ("\n");
      }
   }

   String getPropName() {
      PropertyInfo info = myData.myInfo;
      return info == null ? null : info.getName();
   }

   public void validate () {
      PropTreeCell child;
      for (child=myFirstChild; child!=null; child=child.next) {
         if (child.myParent != this) {
            throw new IllegalStateException (
               "Child child does not point to parent");
         }
         if (child.next == null && child != myLastChild) {
            throw new IllegalStateException (
               "Children not terminated by last child");
         }
         child.validate();
      }
      if (myLastChild != null && myLastChild.next != null) {
         throw new IllegalStateException (
               "Last child is not followed by a null");
      }
   }
}
