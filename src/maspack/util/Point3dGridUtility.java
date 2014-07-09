/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.ArrayList;

import maspack.matrix.Point3d;

public class Point3dGridUtility implements FastMarcherUtility {

   public static boolean defaultUseDiagonals = false;
   private static final int NEIGHBOURS [][] = {
      {-1, 0, 0}, {0, -1, 0}, {0, 0, -1}, 
      {1, 0, 0}, {0, 1, 0}, {0, 0, 1},
      
      // all diagonals
      {-1, -1, 0}, {-1, 0, -1}, {0, -1, -1},
      {1, -1, 0}, {1, 0, -1}, {0, 1, -1},
      {-1, 1, 0}, {-1, 0, 1}, {0, -1, 1},
      {1, 1, 0}, {1, 0, 1}, {0, 1, 1},
      {-1, -1, -1},  
      {1, -1, -1}, {-1, 1, -1}, {-1, -1, 1},
      {-1, 1, 1}, {1, -1, 1}, {1, 1, -1},
      {1, 1, 1}
   };
   
   int maxNeighbours = 0;
   boolean useDiagonals = defaultUseDiagonals;
   
   Point3d[][][] myPoints = null;
   int nx;
   int ny;
   int nz;
   ArrayList<int[]> indices = null;
   
   public Point3dGridUtility(Point3d[][][] pnts) {
      myPoints = pnts;
      nx = pnts.length;
      ny = pnts[0].length;
      nz = pnts[0][0].length;
      buildIndices();
      useDiagonals(defaultUseDiagonals);
   }
   
   public void useDiagonals(boolean set) {
      useDiagonals = set;
      if (!useDiagonals) {
         maxNeighbours = 6;
      } else {
         maxNeighbours = 26;
      }
   }
   
   public boolean isUsingDiagonals() {
      return useDiagonals;
   }
   
   public ArrayList<int[]> getIndexArray() {
      return indices;
   }
   
   private void buildIndices() {
      indices=  new ArrayList<int[]>(nx*ny*nz);
      for (int i=0; i<nx; i++) {
         for (int j=0; j<ny; j++) {
            for (int k=0; k<nz; k++) {
               indices.add(new int[]{i,j,k});
            }
         }
      }
   }
   
   private int getIndex(int i, int j, int k) {
      return k + j*nz + i*ny*nz;
   }

   @Override
   public double distance(int p1Idx, int p2Idx) {
      int[] p1 = indices.get(p1Idx);
      int[] p2 = indices.get(p2Idx);
      Point3d pnt1 = myPoints[p1[0]][p1[1]][p1[2]];
      Point3d pnt2 = myPoints[p2[0]][p2[1]][p2[2]];
      return pnt1.distance(pnt2);
   }

   @Override
   public int getNeighbour(int pIdx, int neighbourIdx) {
      int nNeighbours = 0;
      int x,y,z;
      
      int[] p = indices.get(pIdx);
      
      for (int i=0; i<maxNeighbours; i++) {
         int[] nbr = NEIGHBOURS[i];
         x = p[0]+nbr[0];
         y = p[1]+nbr[1];
         z = p[2]+nbr[2];
         
         if (x >= 0 && x < nx 
            && y >= 0 && y < ny
            && z >= 0 && z < nz
            && myPoints[x][y][z] != null) {
            
            if (neighbourIdx == nNeighbours) {
               return getIndex(x, y, z);
            }
            nNeighbours++;
         }
      }
      throw new IndexOutOfBoundsException("No neighbour " + neighbourIdx);
   }

   @Override
   public int getNumNeighbours(int pIdx) {
      int nNeighbours = 0;
      int x,y,z;
      
      int[] p = indices.get(pIdx);
      
      for (int i=0; i<maxNeighbours; i++) {
         int[] nbr = NEIGHBOURS[i];
         x = p[0]+nbr[0];
         y = p[1]+nbr[1];
         z = p[2]+nbr[2];
         
         if (x >= 0 && x < nx 
            && y >= 0 && y < ny
            && z >= 0 && z < nz
            && myPoints[x][y][z] != null) {
            nNeighbours++;
         }
      }
      return nNeighbours;
   }

}
