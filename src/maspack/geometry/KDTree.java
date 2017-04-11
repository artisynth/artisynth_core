/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Generic KD-Tree utility, requires a KDComparator for computing
 * distances between elements.  Used for finding nearest M 
 * neighbours of a supplied point in log(N) time.
 * 
 * @author Antonio
 */
public class KDTree<T> {

   private int k;
   private KDNode<T> root;
   private KDComparator<T> comparator;

   /**
    * KD node associated with a node on a KD-Tree
    * @author Antonio
    */
   public static class KDNode<T> {
      
      KDNode<T> parent;
      KDNode<T> left;
      KDNode<T> right;
      
      int dim;
      int depth;
      int axis;
      
      T element;
      double distance;  // used for nearest neighbour searches
      
      public KDNode(int k, int depth, T element) {
         dim = k;
         this.depth = depth;
         this.element = element;
         axis = depth % dim;
         distance = -1;
      }
      
      public int compareTo(T val, KDComparator<T> comparator) {
         return comparator.compare(element, val, axis); 
      }
         
   }
   
   private static class KDimComparator<T> implements Comparator<T>{
      int myDim;
      KDComparator<T> myComparator;
      public KDimComparator(int dim, KDComparator<T> comp) {
         myDim = dim;
         myComparator = comp;
      }

      @Override
      public int compare(T o1, T o2) {
         return myComparator.compare(o1, o2, myDim);
      }
   }
   
   private static class KDistanceComparator<T> implements Comparator<KDNode<T>> {
      KDComparator<T> myComparator;
      T center;
      public KDistanceComparator (KDComparator<T> comp, T pnt) {
         myComparator = comp;
         center = pnt;
      }
      
      @Override
      public int compare(KDNode<T> o1, KDNode<T> o2) {
         
         double d1 = myComparator.distance(center, o1.element);
         double d2 = myComparator.distance(center, o2.element);
         
         if (d1 < d2) {
            return -1;
         } else if (d1 > d2) {
            return 1;
         } else {
            if (o1.hashCode() < o2.hashCode()) {
               return -1;
            } else if (o1.hashCode() > o2.hashCode()) {
               return 1;
            } 
         }
         
         return 0;
      }
   }
   
   private Comparator<T> kDimComparator[];

   /**
    * Default constructor.  Creates a KD-Tree structure for a given dimension, 
    * list of points, and comparator
    */
   public KDTree(int dim, List<T> list, KDComparator<T> comp) {
      k = dim;
      comparator = comp;
      buildKDimComparators();
      root = createNode(list, k, 0);
   }

   @SuppressWarnings("unchecked")
   private void buildKDimComparators() {
      kDimComparator = (Comparator<T>[])(new KDimComparator[k]);
      for (int i=0; i<k; i++) {
         kDimComparator[i] = new KDimComparator<T>(i, comparator);
      }
   }

   private KDNode<T> createNode(List<T> list, int k, int depth) {
      if (list==null || list.size()==0) {
         return null;
      }

      int axis = depth % k;
      Collections.sort(list, kDimComparator[axis]);

      // break at median
      int medianIdx = list.size()/2;

      KDNode<T> node = new KDNode<T>(k, depth, list.get(medianIdx));

      if (list.size()>0) {

         // left points
         if ( (medianIdx-1) >= 0) {

            List<T> left = list.subList(0, medianIdx);
            if (left.size() > 0) {
               node.left = createNode(left, k, depth+1);
               node.left.parent = node;
            }
         }

         // right points
         if ((medianIdx+1) <= (list.size()-1)) {
            List<T> right = list.subList(medianIdx+1, list.size());
            if (right.size() > 0) {
               node.right = createNode(right, k, depth+1);
               node.right.parent = node;
            }
         }
      }

      return node;
   }

   /**
    * Checks if the tree contains the supplied value
    */
   public boolean contains(T value) {
      if (value==null) return false;

      KDNode<T> node = getNode(value);
      return (node != null);
   }

   /**
    * Retrieves the KD node associated with a value
    */
   protected KDNode<T> getNode(T value) {

      if ( root==null || value==null) { 
         return null;
      }

      KDNode<T> node = root;

      while (node != null) {
         if (node.element.equals(value)) {
            return node;
         }

         int cmp = node.compareTo(value, comparator);
         if (cmp >= 0) {
            node = node.right;
         } else {
            node = node.left;
         } // end checking left/right
      } // end looping
      
      return node;
   }


   /**
    * K Nearest Neighbour search
    *
    * @param pnt point to find neighbors of.
    * @param K Number of neighbors to retrieve. Can return more than K, 
    * if last nodes are equal distances.
    * @param tol tolerance for located neighbors
    * @return collection of T neighbors.
    */
   public ArrayList<T> nearestNeighbourSearch(T pnt, int K, double tol) {
      
      if (pnt == null) {
         return null;
      }

      //Map used for results
      TreeSet<KDNode<T>> results = new TreeSet<KDNode<T>>(
         new KDistanceComparator<T>(comparator, pnt));

      //Find the closest leaf node
      KDNode<T> prev = null;
      KDNode<T> node = root;
      while ( node != null) {
         if (node.compareTo(pnt, comparator) <= 0) {
            // right
            prev = node;
            node = node.right;
         } else {
            // left
            prev = node;
            node = node.left;
         }
      }
      KDNode<T> leaf = prev; // last non-null
      prev = null;

      if (leaf != null) {
         
         //prevent re-examining nodes
         HashSet<KDNode<T>> examined = new HashSet<KDNode<T>>();

         //Travel up the tree, looking for better solutions
         node = leaf;
         while (node != null) {
            
            //Search node
            searchNode(pnt, node, prev, K, results, examined, tol);
            prev = node;
            node = node.parent;
         }
      }

      //Load up the collection of the results
      ArrayList<T> collection = new ArrayList<T>(K);
      for (KDNode<T> kdNode : results) {
         collection.add(kdNode.element);
      }
      return collection;
   }
   
   /**
    * Nearest Neighbour search
    *
    * @param pnt point to find neighbors of.
    * @return nearest point
    */
   public T nearestNeighbourSearch(T pnt, double tol) {
      
      if (pnt == null) {
         return null;
      }

      //Map used for results
      TreeSet<KDNode<T>> results = new TreeSet<KDNode<T>>(
         new KDistanceComparator<T>(comparator, pnt));

      //Find the closest leaf node
      KDNode<T> prev = null;
      KDNode<T> node = root;
      while ( node != null) {
         if (node.compareTo(pnt, comparator) <= 0) {
            // right
            prev = node;
            node = node.right;
         } else {
            // left
            prev = node;
            node = node.left;
         }
      }
      KDNode<T> leaf = prev; // last non-null
      prev = null;

      if (leaf != null) {
         
         //prevent re-examining nodes
         HashSet<KDNode<T>> examined = new HashSet<KDNode<T>>();

         //Travel up the tree, looking for better solutions
         node = leaf;
         while (node != null) {
            
            //Search node
            searchNode(pnt, node, prev, 1, results, examined, tol);
            prev = node;
            node = node.parent;
         }
      }

      return results.pollFirst().element;
   }

   private void searchNode(T value, KDNode<T> node, KDNode<T> prev, int K, 
      TreeSet<KDNode<T>> results, HashSet<KDNode<T>> examined, double tol) {
      
      examined.add(node);

      //Search node
      KDNode<T> lastNode = null;
      double lastDistance = Double.MAX_VALUE;
      if (results.size() > 0) {
         lastNode = results.last();
         lastDistance = lastNode.distance;
      }
      double nodeDistance = comparator.distance(node.element, value);
      node.distance = nodeDistance; // store distance instead of recomputing
      
      if (lastDistance - nodeDistance > tol) {
         
         results.add(node);
         // we may need to remove a whole bunch of stuff
         if (results.size() > K && lastNode!=null) {
            Iterator<KDNode<T>> it = results.iterator();
            
            int idx = 0;
            KDNode<T> next;
            while (it.hasNext()) {
               next = it.next();
               if (idx == (K-1)) {
                 lastDistance = next.distance;
               } else if (idx >= K && 
                  next.distance - lastDistance > tol) {
                  it.remove();
               }
               idx++;
            }
            
         } 
         
      } else if ( nodeDistance - lastDistance <= tol) {
         results.add(node);
      } else if (results.size() < K) {
         results.add(node);
      }
      lastNode = results.last(); // because adding may change order
      lastDistance = lastNode.distance;

      //Search child branches if hypersphere crosses hyperplane
      double d = comparator.distance(node.element, value, node.axis);
      boolean intersect = false;
      if ( d - lastDistance <= tol || results.size() < K) {
         intersect = true;
      }
     
      KDNode<T> left = node.left;
      KDNode<T> right = node.right;
      d = kDimComparator[node.axis].compare(value, node.element);
      boolean isLeft = false;
      boolean isRight = false;
      if (d <= 0) {
         isLeft = true;
      }
      if (d >= 0) {
         isRight = true;
      }
      
      if ( left !=null && !examined.contains(left) && ( intersect || isLeft )) {
         searchNode(value, left, null, K, results, examined, tol);
      }
      
      if ( right !=null && !examined.contains(right) && ( intersect || isRight )) {
         searchNode(value, right, null, K, results, examined, tol);
      }
   }

}
