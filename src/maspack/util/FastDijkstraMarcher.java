/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;


/**
 * Performs an algorithm similar to "Fast Marching", except uses graph 
 * distances like Dijkstra's algorithm. This is faster but less accurate
 * than the real F-M.  This implementation does support varying propagation
 * speeds, defined at points.  The speed along an edge is assumed to be
 * the average between two points.
 * 
 * @author Antonio
 * 
 */
public class FastDijkstraMarcher extends FastMarcherBase {

   protected double[] speed;

   /**
    * Creates a FastRadialMarcher object for a given data size, and uses the
    * supplied utility object to connect to data for finding neighbours and
    * computing distances
    */
   public FastDijkstraMarcher (int dataSize, FastMarcherUtility utility) {
      super(dataSize, utility);
   }
   
   /**
    * Assigns a vector of 'speeds' associated with each node.  The speed can be
    * thought of as a scaling factor.  The "distance" between adjacent nodes
    * is given by:<br>
    * <code>
    *     distance[node2] = distance[node1] + 
    *    dist(node1,node2)*(speed[node1]+speed[node2])/2
    *  </code><br>
    *  If {@code speed=null}, the speed array is cleared, and all speeds are
    *  assumed to be 1. 
    */
   public void setSpeeds(double[] speed) {
      if (speed == null) {
         this.speed = null;
         return;
      }
      if (speed.length != myDataSize) {
         throw new IllegalArgumentException("Size of array doesn't match the"
            + "number of nodes.  Should have length " + myDataSize + ".");
      }
      this.speed = speed;
   }

   protected boolean updateDistances(int posIdx, int[] voronoi, 
      double[] distance, IndexedBinaryHeap heap) {

      boolean update = false;

      int nBours = myUtility.getNumNeighbours(posIdx);
      for (int i = 0; i < nBours; i++) {
         int nidx = myUtility.getNeighbour(posIdx, i);

         if (statii[nidx] != FROZEN) {
            double prevD = distance[nidx];
            double sp = 1;
            double dx = myUtility.distance(posIdx, nidx);
            if (speed != null) {
               sp = (speed[posIdx] + speed[nidx])/2;
            }
            double newD = distance[posIdx] + sp*dx;
               
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
               update = true;
            }
         }
      }

      return update;
   }

}
