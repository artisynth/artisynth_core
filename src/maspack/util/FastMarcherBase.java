/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for fast marching types of algorithms
 * 
 * @author Antonio
 * 
 */
public abstract class FastMarcherBase {

   protected static final byte CLOSE = 1;
   protected static final byte FROZEN = 2;
   protected static final byte FAR = 0;

   protected FastMarcherUtility myUtility;

   protected double[] distance;
   protected int[] voronoi;
   protected byte[] statii;
   protected int myDataSize;
   protected IndexedBinaryHeap distanceHeap = null;
   protected IndexedBinaryHeap closeHeap = null;

   protected static class DistanceComparator implements Comparator<Integer> {

      public double myDistances[];

      public DistanceComparator (double[] distances) {
         myDistances = distances;
      }

      @Override
      public int compare(Integer idx1, Integer idx2) {
         double d1 = myDistances[idx1];
         double d2 = myDistances[idx2];
         if (d1 < d2) {
            return -1;
         } else if (d1 > d2) {
            return 1;
         }
         return 0;
      }
   }

   /**
    * Creates a FastMarcher object for a given data size, and uses the
    * supplied utility object to connect to data for finding neighbours and
    * computing distances
    */
   public FastMarcherBase (int dataSize, FastMarcherUtility utility) {
      myUtility = utility;
      myDataSize = dataSize;
   }

   protected <T> T[] allocate(List<T> a, int size) {
      @SuppressWarnings("unchecked")
      T[] r = (T[])java.lang.reflect.Array.newInstance(
         a.get(0).getClass(), size);

      return r;
   }

   /**
    * Creates a binary heap that is useful for finding the farthest point
    * in the supplied array of distances.
    */
   public static IndexedBinaryHeap createDistanceHeap(double[] distance) {
      Comparator<Integer> indexedComparator = new DistanceComparator(distance);
      IndexedBinaryHeap dheap =
         new IndexedBinaryHeap(distance.length, indexedComparator, false);

      // initialize
      dheap.setAll();

      return dheap;
   }

   /**
    * Useful for farthest point sampling, sets a heap to be updated every time a
    * distance value is assigned.
    * 
    * @param distHeap distance heap
    */
   public void setDistanceHeap(IndexedBinaryHeap distHeap) {
      distanceHeap = distHeap;
   }

   /**
    * Retrieves an associated distance binary heap. If one does not exist,
    * creates one. This is useful for keeping track of the farthest point
    */
   public IndexedBinaryHeap getDistanceHeap() {
      if (distanceHeap == null && distance != null) {
         // create
         distanceHeap = createDistanceHeap(distance);
      }
      return distanceHeap;
   }
   
   /**
    * Gets the index of the farthest point, -1 if distances are not
    * initialized
    */
   public int getFarthest() {
      if (distanceHeap == null) {
         distanceHeap = getDistanceHeap();
         if (distanceHeap == null) {
            return -1;
         }
      }
      return distanceHeap.peek();
   }

   /**
    * Retrieves a vector of the current distances
    */
   public double[] getDistance() {
      return distance;
   }

   /**
    * Sets the distance vector to use
    */
   public void setDistance(double[] d) {
      if (d.length != myDataSize) {
         throw new IllegalArgumentException(
            "Supplied distance array has the wrong size");
      }
      distance = d;
   }

   /**
    * Gets the voronoi region assignments. Each value corresponds to the index
    * of the data element that was used as a starting point
    */
   public int[] getVoronoi() {
      return voronoi;
   }

   /**
    * Sets the vector to be used to hold voronoi indicies
    * 
    * @param v voronoi index vector
    */
   public void setVoronoi(int[] v) {
      if (v.length != myDataSize) {
         throw new IllegalArgumentException(
            "Supplied voronoi array has the wrong size");
      }
      voronoi = v;
   }

   /**
    * Creates distance and voronoi arrays if required, and fills them with their
    * default values (distance=infinity, voronoi=-1).
    */
   public void initializeArrays() {
      if (distance == null) {
         distance = new double[myDataSize];
      }
      Arrays.fill(distance, Double.POSITIVE_INFINITY);
      if (voronoi == null) {
         voronoi = new int[myDataSize];
      }
      Arrays.fill(voronoi, -1);
   }

   /**
    * Performs the fast marching algorithm starting at points with indices given
    * in {@code start}.
    */
   public void march(int[] start) {
      if (distance == null) {
         distance = new double[myDataSize];
         Arrays.fill(distance, Double.POSITIVE_INFINITY);
      }
      if (voronoi == null) {
         voronoi = new int[myDataSize];
         Arrays.fill(voronoi, -1);
      }
      march(start, voronoi, distance);
   }

   /**
    * Performs the fast marching algorithm starting at data point with index
    * {@code start}.
    */
   public void march(int start) {
      int[] startList = new int[] { start };
      march(startList);
   }

   /**
    * Performs the fast marching algorithm starting at data point with index
    * {@code start}, using the supplied voronoi and distance arrays instead of
    * this object's internal versions.
    */
   public void march(int start, int[] voronoi, double[] distance) {
      int[] startList = new int[] { start };
      march(startList, voronoi, distance);
   }

   /**
    * Performs the fast marching algorithm starting at data points with indices
    * given by {@code start}, using the supplied voronoi and distance arrays
    * instead of this object's internal version.
    */
   public void march(int[] start, int[] voronoi, double[] distance) {

      this.voronoi = voronoi;
      this.distance = distance;
      if (statii == null) {
         statii = new byte[myDataSize];
      }
      Arrays.fill(statii, FAR);

      IndexedBinaryHeap closeHeap = new IndexedBinaryHeap(myDataSize,
         new DistanceComparator(distance));

      // mark starting points
      for (int i = 0; i < start.length; i++) {
         int idx = start[i];
         distance[idx] = 0;
         statii[idx] = FROZEN;
         voronoi[idx] = idx;
         if (distanceHeap != null) {
            distanceHeap.update(idx);
         }
      }

      // mark "close" region
      for (int i = 0; i < start.length; i++) {
         int idx = start[i];
         updateDistances(idx, voronoi, distance, closeHeap);
      }

      // main loop
      Integer idx = -1;
      while (closeHeap.size() > 0) {
         idx = closeHeap.poll();
         statii[idx] = FROZEN;
         if (distanceHeap != null) {
            distanceHeap.update(idx);
         }
         updateDistances(idx, voronoi, distance, closeHeap);
      }

   }
   
   public void startmarch(int start) {
      startmarch(new int[] {start});
   }
   
   /**
    * Performs the fast marching algorithm starting at data points with indices
    * given by {@code start}, using the supplied voronoi and distance arrays
    * instead of this object's internal version.
    */
   public void startmarch(int[] start) {

      if (statii == null) {
         statii = new byte[myDataSize];
      }
      Arrays.fill(statii, FAR);

      IndexedBinaryHeap closeHeap = new IndexedBinaryHeap(myDataSize,
         new DistanceComparator(distance));

      // mark starting points
      for (int i = 0; i < start.length; i++) {
         int idx = start[i];
         distance[idx] = 0;
         statii[idx] = FROZEN;
         voronoi[idx] = idx;
         if (distanceHeap != null) {
            distanceHeap.update(idx);
         }
      }

      // mark "close" region
      for (int i = 0; i < start.length; i++) {
         int idx = start[i];
         updateDistances(idx, voronoi, distance, closeHeap);
      }
      
      this.closeHeap = closeHeap;

   }
   
   public int step() {
      
      if (closeHeap == null || closeHeap.size() == 0) {
         return 0;
      }
      
      int idx = closeHeap.poll();
      statii[idx] = FROZEN;
      if (distanceHeap != null) {
         distanceHeap.update(idx);
      }
      updateDistances(idx, voronoi, distance, closeHeap);
      return closeHeap.size();
   }
   
   /**
    * Updates the distances of all neighbour points to the one located at 
    * {@code posIdx}
    * @param posIdx index of point to update neighbours
    * @param voronoi array of voronoi indicators
    * @param distance array of distances
    * @param heap binary heap storing list of "close" points
    * @return true if any of the distances have changed in the heap, false
    * otherwise
    */
   protected abstract boolean updateDistances(int posIdx, int[] voronoi, 
      double[] distance, IndexedBinaryHeap heap);
   
   

   /**
    * Removes internal distance and voronoi arrays so they will be re-created on
    * the next call to @{link {@link #march(int)}
    */
   public void clear() {
      distance = null;
      voronoi = null;
   }

   /**
    * Creates and/or re-initializes internal set of distance and voronoi arrays
    */
   public void reset() {
      initializeArrays();
      closeHeap = null;
   }

}
