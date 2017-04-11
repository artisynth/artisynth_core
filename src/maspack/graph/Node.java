/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Represents a node of the {@code Tree<T>} class. The {@code Node<T>} is also
 * a container, and can be thought of as instrumentation to determine the
 * location of the type T in the {@code Tree<T>}.
 * 
 * Ideas from:
 * http://sujitpal.blogspot.ca/2006/05/java-data-structure-generic-tree.html
 * 
 */
public class Node<T> {

   protected T data;
   protected List<Node<T>> children;
   protected List<Node<T>> parents;

   /**
    * Default constructor.
    */
   public Node () {
      super();
      children = new ArrayList<Node<T>>();
      parents = new ArrayList<Node<T>>();
   }

   /**
    * Convenience ctor to create a {@code Node<T>} with an instance of T.
    * 
    * @param data
    * an instance of T.
    */
   public Node (T data) {
      this();
      setData(data);
   }

   /**
    * Return the children of {@code Node<T>}. The {@code Tree<T>} is represented by a
    * single root {@code Node<T>} whose children are represented by a {@code
    * List<Node<T>>}. Each of these {@code Node<T>} elements in the List can
    * have children. The getChildren() method will return the children of a
    * {@code Node<T>}.
    * 
    * @return the children of {@code Node<T>}
    */
   public List<Node<T>> getChildren() {
      if (this.children == null) {
         return new ArrayList<Node<T>>();
      }
      return this.children;
   }

   /**
    * Gets a child at a particular index
    * 
    * @param idx
    * the index of the child to get
    * @return the child
    */
   public Node<T> getChild(int idx) throws IndexOutOfBoundsException {
      return children.get(idx);
   }

   /**
    * Sets the children of a {@code Node<T>} object. See docs for getChildren() for more
    * information.
    * 
    * @param children
    * the {@code List<Node<T>>} to set.
    */
   public void setChildren(List<Node<T>> children) {
      this.children = children;
      for (Node<T> child : children) {
         child.addParent(this);
      }
   }

   /**
    * Returns the number of immediate children of this {@code Node<T>}.
    * 
    * @return the number of immediate children.
    */
   public int getNumberOfChildren() {
      if (children == null) {
         return 0;
      }
      return children.size();
   }

   /**
    * Returns the number of immediate parents of this {@code Node<T>}.
    * 
    * @return the number of immediate parents.
    */
   public int getNumberOfParents() {
      if (parents == null) {
         return 0;
      }
      return parents.size();
   }

   /**
    * Adds a child to the list of children for this {@code Node<T>}. The addition of the
    * first child will create a new {@code List<Node<T>>}.
    * 
    * @param child
    * a {@code Node<T>} object to set.
    */
   public void addChild(Node<T> child) {
      if (children == null) {
         children = new ArrayList<Node<T>>();
      }
      if (!children.contains(child)) {
         children.add(child);
         child.addParent(this);
      }
   }

   /**
    * Adds a child to the list of children for this {@code Node<T>} at a particular
    * index
    * 
    * @param index
    * the index at which to insert the child
    * @param child
    * a {@code Node<T>} object to set.
    */
   public void insertChildAt(int index, Node<T> child) {
      if (children == null) {
         children = new ArrayList<Node<T>>();
      }
      if (!children.contains(child)) {
         children.add(index, child);
         child.addParent(this);
      }
   }

   private void addParent(Node<T> parent) {
      if (!parents.contains(parent)) {
         parents.add(parent);
      }
   }

   public List<Node<T>> getParents() {
      return parents;
   }

   public Node<T> getParent(int idx) {
      return parents.get(idx);
   }

   /**
    * Adds a list of children to this {@code Node<T>}.
    * 
    * @param newChildren
    * list of {@code Node<T>} objects to add
    */
   public void addChildren(List<Node<T>> newChildren) {
      if (children == null) {
         children = new ArrayList<Node<T>>();
      }
      for (Node<T> child : newChildren) {
         addChild(child);
      }
   }

   /**
    * Remove the {@code Node<T>} element at index index of the {@code List<Node<T>>}.
    * 
    * @param index
    * the index of the element to delete.
    * @throws IndexOutOfBoundsException
    * if thrown.
    */
   public void removeChild(int index) throws IndexOutOfBoundsException {
      Node<T> child = children.get(index);
      removeChild(child);
   }

   /**
    * Remove the {@code Node<T>} element
    * 
    * @param child
    * the node to remove
    */
   public void removeChild(Node<T> child) {

      if (children.contains(child)) {
         child.parents.remove(this); // remove this from child's list of parents
         children.remove(child); // remove child
      }
   }

   /**
    * Clears all children, ensuring to remove the parent entry
    */
   public void removeAllChildren() {

      int nChildren = getNumberOfChildren();
      for (int i = 0; i < nChildren; i++) {
         removeChild(nChildren - i - 1); // going backwards for efficiency
      }

   }

   /**
    * Removes node from all parents
    */
   public void detachFromParents() {
      for (Node<T> parent : parents) {
         detachFromParent(parent);
      }
   }

   /**
    * Removes child from a particular parent
    * 
    * @param parent parent from which this node is to be removed
    */
   public void detachFromParent(Node<T> parent) {
      if (parents.contains(parent)) {
         parent.removeChild(this);
      }
   }

   public T getData() {
      return this.data;
   }

   public void setData(T data) {
      this.data = data;
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();
      
      String dataStr = null;
      T data = getData();
      if (data != null) {
         dataStr = data.toString();
      } else {
         dataStr = "null";
      }

      if (data != null) {
         sb.append("{").append(dataStr).append(",[");
      }
      int i = 0;
      for (Node<T> e : getChildren()) {
         if (i > 0) {
            sb.append(",");
         }
         sb.append(e.toString());
         i++;
      }
      sb.append("]").append("}");
      return sb.toString();
   }

   /**
    * Removes the branch below this item, clearing children if this is the only
    * parent
    */
   public void clear() {
      // if child has only this as a parent, then clear it as well
      for (Node<T> child : children) {
         if (child.getNumberOfParents() == 1) {
            child.clear();
         }
      }
      removeAllChildren();
   }
   
   private static class NodeEqualityWrapper<T> {
      Node<T> node;
      public NodeEqualityWrapper(Node<T> node) {
         this.node = node;
      }
      public Node<T> node() {
         return node;
      }
      
      @Override
      public int hashCode() {
         int hc = 0;
         if (node.data != null) {
            hc = node.data.hashCode();
         }
         hc = hc*31+node.children.size();
         return hc;
      }
      
      @Override
      public boolean equals(Object obj) {
        if (obj == this) {
           return true;
        }
        if (obj == null) {
           return false;
        }
        if (!(obj instanceof NodeEqualityWrapper)) {
           return false;
        }
        
        @SuppressWarnings("unchecked")
        NodeEqualityWrapper<T> other = (NodeEqualityWrapper<T>)obj;
        return node.equalsNode(other.node);
        
      }
   }
   

   /**
    * Value-equality of nodes and all children (i.e. entire tree)
    * 
    * @param node node to compare with
    * @return true if equals, false otherwise
    */
   public boolean equalsNode(Node<T> node) {
      if (node == null) {
         return false;
      }
      if (this == node) {
         return true;
      }
      
      if (data != null) {
         if (!data.equals(node.data)) {
            return false;
         }
      } else if (node.data != null) {
         return false;
      }
      
      // children, assume consistent parents
      if (children.size() != node.children.size()) {
         return false;
      }
      
      // check that each child exists in other (set equality)
      HashSet<NodeEqualityWrapper<T>> childSet = new HashSet<NodeEqualityWrapper<T>>();
      for (Node<T> child : children) {
         childSet.add(new NodeEqualityWrapper<>(child));
      }
      
      HashSet<NodeEqualityWrapper<T>> otherChildSet = new HashSet<NodeEqualityWrapper<T>>();
      for (Node<T> child : node.children) {
         otherChildSet.add(new NodeEqualityWrapper<>(child));
      }
      
      boolean setsEqual = childSet.equals(otherChildSet);
      
      return setsEqual;
   }
   
   /**
    * merges children with equal content, reducing branches
    */
   public void consolidate() {

      List<Node<T>> newChildren = new ArrayList<Node<T>>();
      List<Node<T>> children = getChildren();

      if (getNumberOfChildren() == 0) {
         return;
      }

      newChildren.add(children.get(0));
      for (int i = 1; i < children.size(); i++) {

         boolean found = false;
         for (int j = 0; j < newChildren.size(); j++) {
            T childData = children.get(i).getData();
            T newChildData = newChildren.get(j).getData();

            if (childData.equals(newChildData)) {
               found = true;

               // copy content to newChild
               newChildren.get(j).addChildren(children.get(i).getChildren());
               break;
            }
         } // end looping through new children to see if child exists

         if (!found) {
            newChildren.add(children.get(i));
         }

      } // end looping through old list of children

      // set new list of children
      children.clear(); // clear list (without actually clearing children
                        // contents)
      addChildren(newChildren);

      // now repeat process for all children
      for (Node<T> child : getChildren()) {
         child.consolidate();
      }

   }
}
