/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;


/**
 * Performs an algorithm similar to "Fast Marching", except uses basic euclidean
 * distances. This is faster and more accurate than F-M for a uniform speed and
 * point-source start points. Drawbacks: it does not support varying speeds
 * (non-uniform sampling), or non-point-source starting points (i.e. cannot be
 * used for creating skeletons).
 * 
 * @author Antonio
 * 
 */
public class FastRadialMarcher extends FastMarcherBase {
   
   /**
    * Creates a FastRadialMarcher object for a given data size, and uses the
    * supplied utility object to connect to data for finding neighbours and
    * computing distances
    */
   public FastRadialMarcher (int dataSize, FastMarcherUtility utility) {
      super(dataSize, utility);
   }
   
   /**
    * {@inheritDoc}
    */
   protected boolean updateDistances(int posIdx, int[] voronoi, 
      double[] distance, IndexedBinaryHeap heap) {

      boolean updated = false;

      int vidx = voronoi[posIdx];

      int nBours = myUtility.getNumNeighbours(posIdx);
      for (int i = 0; i < nBours; i++) {
         int nidx = myUtility.getNeighbour(posIdx, i);

         if (statii[nidx] != FROZEN ) {
            double prevD = distance[nidx];
            double newD = myUtility.distance(vidx, nidx);

            if (newD < prevD) {
               distance[nidx] = newD;
               voronoi[nidx] = voronoi[posIdx];
               if (statii[nidx] == FROZEN) {
                  if (distanceHeap != null) {
                     distanceHeap.update(nidx);
                  }
               } else if (statii[nidx] == CLOSE) {
                  heap.update(nidx);
               } else if (statii[nidx] == FAR) {
                  heap.add(nidx);
                  statii[nidx] = CLOSE;
               }
               updated = true;
            }
         }
      }

      return updated;
   }
}
