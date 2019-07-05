/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Support class for sorting arrays of integers and doubles. The code for
 * quicksort was based on information provided in the wikipedia entry for
 * quicksort.
 */
public class ArraySort {

   /** 
    * Sorts an array of integers into ascending order. The method
    * chooses between quicksort and bubble sort depending on the size of the
    * input.
    * 
    * @param keys integers to sort
    */   
   public static void sort (int[] keys) {
      sort (keys, 0, keys.length-1);
   }

   /** 
    * Sorts a subregion of an array of integers into ascending order.
    * The subregion is defined by left &lt;= i &lt;= right. The method chooses
    * between quicksort and bubble sort depending on the size of the input.
    * 
    * @param keys integers containing the subregion to sort
    * @param left lower inclusive bound of the subregion
    * @param right upper inclusive bound of the subregion
    */   
   public static void sort (
      int[] keys, int left, int right) {
      if (right-left < 8) {
         bubbleSort (keys, left, right);
      }
      else {
         quickSort (keys, left, right);
      }
   }

   /** 
    * Sorts an array of integers into ascending order, and
    * correspondingly rearranges an accompanying array of values. The method
    * chooses between quicksort and bubble sort depending on the size of the
    * input.
    * 
    * @param keys integers to sort
    * @param vals accompanying values to be sorted into the same order as keys.
    * Must have a length at least as long as keys.
    */   
   public static void sort (int[] keys, double[] vals) {
      sort (keys, vals, 0, keys.length-1);
   }

   /** 
    * Sorts a subregion of an array of integers into ascending order,
    * and correspondingly rearranges the same subregion of an accompanying
    * array of values. The subregion is defined by left &lt;= i &lt;= right. The
    * method chooses between quicksort and bubble sort depending on the size of
    * the input.
    * 
    * @param keys integers containing the subregion to sort
    * @param vals accompanying values, whose equivalent subregion is sorted
    * into the same order as the subregion as keys. Must be long enough
    * to encompass the subregion.
    * @param left lower inclusive bound of the subregion
    * @param right upper inclusive bound of the subregion
    */   
   public static void sort (
      int[] keys, double[] vals, int left, int right) {
      if (right-left < 8) {
         bubbleSort (keys, vals, left, right);
      }
      else {
         quickSort (keys, vals, left, right);
      }
   }

   /** 
    * Performs a bubble sort on an array of integers into ascending order.
    * 
    * @param keys integers to sort
    */   
   public static void bubbleSort (int[] keys) {
      bubbleSort (keys, 0, keys.length-1);
   }

   /** 
    * Performs a bubble sort on a subregion of an array of integers
    * into ascending order. The subregion is defined by left &lt;= i &lt;= right.
    * 
    * @param keys integers containing the subregion to sort
    * @param left lower inclusive bound of the subregion
    * @param right upper inclusive bound of the subregion
    */   
   public static void bubbleSort (
      int[] keys, int left, int right) {
      if (left >= right) {
         return;
      }
      for (int i=left+1; i<=right; i++) {
         int key = keys[i-1];
         for (int j=i; j<=right; j++) {
            if (keys[j] < key) {
               int tmpk = key;
               key = keys[j];
               keys[j] = tmpk;
            }
         }
         keys[i-1] = key;
      }
   }

   /** 
    * Performs a bubble sort on an array of integers into ascending order, and
    * correspondingly rearranges an accompanying array of values.
    * 
    * @param keys integers to sort
    * @param vals accompanying values to be sorted into the same order as keys.
    * Must have a length at least as long as keys.
    */   
   public static void bubbleSort (int[] keys, double[] vals) {
      bubbleSort (keys, vals, 0, keys.length-1);
   }

   /** 
    * Performs a bubble sort on a subregion of an array of integers into
    * ascending order, and correspondingly rearranges the same subregion of an
    * accompanying array of values. The subregion is defined by left &lt;= i &lt;=
    * right.
    * 
    * @param keys integers containing the subregion to sort
    * @param vals accompanying values, whose equivalent subregion is sorted
    * into the same order as the subregion as keys. Must be long enough
    * to encompass the subregion.
    * @param left lower inclusive bound of the subregion
    * @param right upper inclusive bound of the subregion
    */   
   public static void bubbleSort (
      int[] keys, double[] vals, int left, int right) {
      if (left >= right) {
         return;
      }
      for (int i=left+1; i<=right; i++) {
         int key = keys[i-1];
         double val = vals[i-1];
         for (int j=i; j<=right; j++) {
            if (keys[j] < key) {
               int tmpk = key;
               key = keys[j];
               keys[j] = tmpk;
               double tmpv = val;
               val = vals[j];
               vals[j] = tmpv;
            }
         }
         keys[i-1] = key;
         vals[i-1] = val;
      }
   }

   private static int partition (
      int[] keys, double[] vals, int left, int right, int pivot) {

      int tmpk;
      double tmpv;

      int pivotKey = keys[pivot];
      // swap pivot and right to move pivot to the end
      tmpk = keys[pivot];
      keys[pivot] = keys[right];
      keys[right] = tmpk;
      tmpv = vals[pivot];
      vals[pivot] = vals[right];
      vals[right] = tmpv;    

      int store = left;
      for (int i=left; i<right; i++) {
         if (keys[i] <= pivotKey) {
            // swap i and store

            tmpk = keys[store];
            keys[store] = keys[i];
            keys[i] = tmpk;
            tmpv = vals[store];
            vals[store] = vals[i];
            vals[i] = tmpv;    

            store++;
         }
      }
      // swap store and right to move pivot to its final place
      tmpk = keys[store];
      keys[store] = keys[right];
      keys[right] = tmpk;
      tmpv = vals[store];
      vals[store] = vals[right];
      vals[right] = tmpv;    

      return store;      
   }

   private static int partition (
      int[] keys, int left, int right, int pivot) {

      int tmpk;

      int pivotKey = keys[pivot];
      // swap pivot and right to move pivot to the end
      tmpk = keys[pivot];
      keys[pivot] = keys[right];
      keys[right] = tmpk;

      int store = left;
      for (int i=left; i<right; i++) {
         if (keys[i] <= pivotKey) {
            // swap i and store

            tmpk = keys[store];
            keys[store] = keys[i];
            keys[i] = tmpk;

            store++;
         }
      }
      // swap store and right to move pivot to its final place
      tmpk = keys[store];
      keys[store] = keys[right];
      keys[right] = tmpk;

      return store;      
   }

   /** 
    * Performs a quicksort on a subregion of an array of integers into
    * ascending order. The subregion is defined by left &lt;= i &lt;= right.
    * 
    * @param keys integers containing the subregion to sort
    * @param left lower inclusive bound of the subregion
    * @param right upper inclusive bound of the subregion
    */   
   public static void quickSort (int[] keys, int left, int right) {
      if (right > left) {
         int pivot = left+(right-left)/2;
         int pivotNew = partition (keys, left, right, pivot);
         quickSort (keys, left, pivotNew - 1);
         quickSort (keys, pivotNew + 1, right);
      }
   }

   /** 
    * Performs a quicksort on an array of integers into ascending order.
    * 
    * @param keys integers to sort
    */   
   public static void quickSort (int[] keys) {
      quickSort (keys, 0, keys.length-1);
   }

   /** 
    * Performs a quicksort on a subregion of an array of integers into
    * ascending order, and correspondingly rearranges the same subregion of an
    * accompanying array of values. The subregion is defined by left &lt;= i &lt;=
    * right.
    * 
    * @param keys integers containing the subregion to sort
    * @param vals accompanying values, whose equivalent subregion is sorted
    * into the same order as the subregion as keys. Must be long enough
    * to encompass the subregion.
    * @param left lower inclusive bound of the subregion
    * @param right upper inclusive bound of the subregion
    */   
   public static void quickSort (int[] keys, double[] vals, int left, int right) {
      if (right > left) {
         int pivot = left+(right-left)/2;
         int pivotNew = partition (keys, vals, left, right, pivot);
         quickSort (keys, vals, left, pivotNew - 1);
         quickSort (keys, vals, pivotNew + 1, right);
      }
   }

   /** 
    * Performs a quicksort on an array of integers, into ascending order, and
    * correspondingly rearranges an accompanying array of values.
    * 
    * @param keys integers to sort
    * @param vals accompanying values to be sorted into the same order as keys.
    * Must have a length at least as long as keys.
    */   
   public static void quickSort (int[] keys, double[] vals) {
      quickSort (keys, vals, 0, keys.length-1);
   }


   private static int partition (
      double[] vals, int[] keys, int left, int right, int pivot) {

      double tmpv;
      int tmpk;

      double pivotVal = vals[pivot];
      // swap pivot and right to move pivot to the end
      tmpv = vals[pivot];
      vals[pivot] = vals[right];
      vals[right] = tmpv;
      tmpk = keys[pivot];
      keys[pivot] = keys[right];
      keys[right] = tmpk;    

      int store = left;
      for (int i=left; i<right; i++) {
         if (vals[i] <= pivotVal) {
            // swap i and store

            tmpv = vals[store];
            vals[store] = vals[i];
            vals[i] = tmpv;
            tmpk = keys[store];
            keys[store] = keys[i];
            keys[i] = tmpk;    

            store++;
         }
      }
      // swap store and right to move pivot to its final place
      tmpv = vals[store];
      vals[store] = vals[right];
      vals[right] = tmpv;
      tmpk = keys[store];
      keys[store] = keys[right];
      keys[right] = tmpk;    

      return store;      
   }

   private static int partition (
      double[] vals, int left, int right, int pivot) {

      double tmpv;

      double pivotVal = vals[pivot];
      // swap pivot and right to move pivot to the end
      tmpv = vals[pivot];
      vals[pivot] = vals[right];
      vals[right] = tmpv;

      int store = left;
      for (int i=left; i<right; i++) {
         if (vals[i] <= pivotVal) {
            // swap i and store

            tmpv = vals[store];
            vals[store] = vals[i];
            vals[i] = tmpv;

            store++;
         }
      }
      // swap store and right to move pivot to its final place
      tmpv = vals[store];
      vals[store] = vals[right];
      vals[right] = tmpv;

      return store;      
   }

   /** 
    * Performs a quicksort on a subregion of an array of doubles into ascending
    * order.  The subregion is defined by left &lt;= i &lt;= right.
    * 
    * @param vals doubles containing the subregion to sort
    * @param left lower inclusive bound of the subregion
    * @param right upper inclusive bound of the subregion
    */   
   public static void quickSort (double[] vals, int left, int right) {
      if (right > left) {
         int pivot = left+(right-left)/2;
         int pivotNew = partition (vals, left, right, pivot);
         quickSort (vals, left, pivotNew - 1);
         quickSort (vals, pivotNew + 1, right);
      }
   }

   /** 
    * Performs a quicksort on an array of doubles into ascending order.
    * 
    * @param vals values to sort
    */   
   public static void quickSort (double[] vals) {
      quickSort (vals, 0, vals.length-1);
   }

   /** 
    * Performs a quicksort on a subregion of an array of doubles into ascending
    * order, and correspondingly rearranges the same subregion of an
    * accompanying array of keys. The subregion is defined by left &lt;= i &lt;=
    * right.
    * 
    * @param vals array of values containing the subregion to sort
    * @param keys accompanying keys, whose equivalent subregion is sorted
    * into the same order as the subregion of vals. Must be long enough
    * to encompass the subregion.
    * @param left lower inclusive bound of the subregion
    * @param right upper inclusive bound of the subregion
    */   
   public static void quickSort (double[] vals, int[] keys, int left, int right) {
      if (right > left) {
         int pivot = left+(right-left)/2;
         int pivotNew = partition (vals, keys, left, right, pivot);
         quickSort (vals, keys, left, pivotNew - 1);
         quickSort (vals, keys, pivotNew + 1, right);
      }
   }

   /** 
    * Performs a quicksort on an array of doubles into ascending order, and
    * correspondingly rearranges an accompanying array of integer keys.
    * 
    * @param vals values to sort
    * @param keys accompanying keys to be sorted into the same order as vals.
    * Must have a length at least as long as vals.
    */   
   public static void quickSort (double[] vals, int[] keys) {
      quickSort (vals, keys, 0, vals.length-1);
   }

}
