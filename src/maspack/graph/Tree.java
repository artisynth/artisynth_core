/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Tree of Objects of generic type T. The Tree is represented as a
 * single rootElement which points to a {@code List<Node<T>>} of
 * children. There is no restriction on the number of children that a
 * particular node may have.  This Tree provides a method to serialize the Tree
 * into a List by doing a pre-order traversal. It has several methods to allow
 * easy updation of Nodes in the Tree.
 * 
 * Modified from: http://sujitpal.blogspot.ca/2006/05/java-data-structure-generic-tree.html
 * 
 */
public class Tree<T> {
 
    private Node<T> rootElement;
     
    /**
     * Default ctor.
     */
    public Tree() {
        super();
    }
 
    /**
     * Creates a tree from the supplied root
     * @param root the root element.
     */
    public Tree(Node<T> root) {
       super();
       setRootElement (root);
    }
    
    /**
     * Check if the value of this tree equals another
     * 
     * @param tree tree to compare with
     * @return <code>true</code> if <code>tree</code> equals this tree
     */
    public boolean equalsTree(Tree<T> tree) {
       if (tree == null) {
          return false;
       }
       if (this == tree) {
          return true;
       }
       
       return rootElement.equalsNode(tree.getRootElement());
    }
    
    /**
     * Creates a tree from the supplied root
     * @param data the data for the root element.
     */
    public Tree(T data) {
       super();
       setRootElement (new Node<T>(data));
    }
    
    /**
     * Return the root Node of the tree.
     * @return the root element.
     */
    public Node<T> getRootElement() {
        return this.rootElement;
    }
 
    /**
     * Set the root Element for the tree.
     * @param rootElement the root element to set.
     */
    public void setRootElement(Node<T> rootElement) {
        this.rootElement = rootElement;
    }
     
    /**
     * Returns the {@code Tree<T>} as a List of {@code Node<T>} objects. The
     * elements of the List are generated from a pre-order traversal of the
     * tree.
     * @return a {@code List<Node<T>>}.
     */
    public List<Node<T>> toList() {
        List<Node<T>> list = new ArrayList<Node<T>>();
        walk(rootElement, list);
        return list;
    }
     
    /**
     * Returns a String representation of the Tree. The elements are generated
     * from a pre-order traversal of the Tree.
     * @return the String representation of the Tree.
     */
    public String toString() {
        return toList().toString();
    }
     
    /**
     * Walks the Tree in pre-order style. This is a recursive method, and is
     * called from the toList() method with the root element as the first
     * argument. It appends to the second argument, which is passed by reference     * as it recurses down the tree.
     * @param element the starting element.
     * @param list the output of the walk.
     */
    private void walk(Node<T> element, List<Node<T>> list) {
        list.add(element);
        for (Node<T> data : element.getChildren()) {
            walk(data, list);
        }
    }
    
    public void clear() {
       rootElement.clear ();
    }
    
    /**
     * Merges branches if the nodes have equal content. For example,
     * <pre>
     * {@code
     * family -> sister -> Anne     
     *        -> sister -> Josephine
     * }
     * </pre>
     * becomes   
     * <pre>
     * {@code
     *        family -> sister -> Anne     
     *                         -> Josephine
     * }
     * </pre>
     */
    public static void consolidate(Tree<?> tree) {
       Node<?> root = tree.getRootElement();
       root.consolidate();
    }
    
   /**
    * Merges branches if the nodes have equal content. For example,
    * <pre>
    * {@code
    * family -> sister -> Anne
    *        -> sister -> Josephine
    * }
    * </pre>
    * becomes
    * <pre>
    * {@code
    *        family -> sister -> Anne
    *                         -> Josephine
    * }
    * </pre>
    */
    public void consolidate() {
       Node<T> root = getRootElement();
       root.consolidate();
    }
}
