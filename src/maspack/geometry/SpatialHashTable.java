package maspack.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.NoSuchElementException;

import maspack.matrix.Point3d;


/** 
 * Spatial Hash Table for speeding up 3D spatial nearest neighbour searches.
 * 
 * 3D space is partitioned into an orthogonal, equally spaced grid. Each grid 
 * point is the centre of a "cell", which is essentially a List<T>. To search
 * for elements T near a point P, the cell containing P, AND the cells 
 * that neighbour the cell containing P must be searched. These cells can be 
 * easily found by calling getCellsNear or getCellsIntersecting, which returns
 * an Iterator<List<T>>, where each List<T> represents the elements in a single 
 * cell. 
 * 
 * Grid spacing is defined upon construction, and construction is performed 
 * by calling setup().
 
 * Example usage:
 * List<T> myElements;
 * ...
 * SpatialHashTable<T> table = new SpatialHashTable<>(spacing);
 * 
 * List<Point3d> positions = new ArrayList<Point3d>();
 * for (T el : myElements) {
 *    positions.add(el.getPos());
 * }
 * 
 * table.setup(positions,elements); // construct map 
 * 
 * Point3d mySearchPoint = new Point3d(0., 1., 2.);
 * 
 * Iterator<List<T>> it = table.getCellsNear (searchPoint);
 * while (it.hasNext()) {
 *    List<T> cell = it.next();
 *    for (T el : cell) {
 *       el.doSomething();
 *    }
 * }
 * 
 * @author andrew ho
 */
public class SpatialHashTable<T> {
   private final double myGridSpacing;
   private HashMap<Index, List<T>> myGrid = new HashMap<>();
   private HashMap<Index, List<List<T>>> myIndexList = new HashMap<>();

   public SpatialHashTable (double gridSpacing) {
      myGridSpacing = gridSpacing;
   }

   /** 
    * Replace the current map with a 
    * new one generated from positions and elements.
    *
    * @param positions List of positions that determine the 
    *   position of each element in elements
    * @param elements List of elements which will populate this
    *   SpatialHashTable
    */
   public synchronized void setup (List<? extends Point3d> positions, List<? extends T> elements) {
      myGrid.clear();

      if (positions.size() != elements.size()) {
         throw new IllegalArgumentException ("Non-matching position and element sizes!");
      }

      for (int i=0; i<elements.size (); i++) {
         T p = elements.get(i);
         
         int idxX, idxY, idxZ;
         
         // This is ok since negative indices are allowed now
         Point3d pos = positions.get(i);
         idxX = (int) Math.round((pos.x)/myGridSpacing);
         idxY = (int) Math.round((pos.y)/myGridSpacing);
         idxZ = (int) Math.round((pos.z)/myGridSpacing);

         Index idx = new Index(idxX,idxY,idxZ);
         List<T> parList = myGrid.get(idx);

         if (parList == null) {
            parList = new ArrayList<T>();
            myGrid.put(idx, parList);
         }
         
         parList.add(p);
      }
      setupListsToNeighbours();
   }
   
   protected synchronized void setupListsToNeighbours () {
      myIndexList.clear ();
      for (Index index : myGrid.keySet ()) {
         List<List<T>> list = new LinkedList<>();
         NearCellIter it = new NearCellIter(index);
         while (it.hasNext ()) {
            List<T> cells = it.next();
            if (cells == null) {
               continue;
            }
            list.add (cells);
         }

         myIndexList.put(index,list);
      }
   }

   /**
    * Returns a List of cells (List<T>'s) that might intersect a
    * bv tree.
    * 
    * @param bvtree
    */
   public Map<List<T>,ArrayList<BVNode>> getCellsIntersecting (BVTree bvtree) {
      Map<List<T>,ArrayList<BVNode>> potentials = new HashMap<List<T>,ArrayList<BVNode>>();
      
      // Do this naively and just intersect sphere of each cell with the bvtree.
      Point3d cellCentre = new Point3d();
      // 2D pythagorean
      double searchRad = myGridSpacing * Math.sqrt(0.5);
      // 3D
      searchRad = Math.sqrt (searchRad*searchRad + myGridSpacing*myGridSpacing/4);
      searchRad += myGridSpacing;
      double sphereRadius = Math.sqrt(2*searchRad*searchRad);
      for (Index index : myGrid.keySet ()) {
         ArrayList<BVNode> nodes = new ArrayList<BVNode> ();
         // We need to add all cells which do intersect, and their neighbouring cells.
         cellCentre.x = index.x * myGridSpacing;
         cellCentre.y = index.y * myGridSpacing;
         cellCentre.z = index.z * myGridSpacing;
         
         bvtree.intersectSphere (nodes, cellCentre, sphereRadius);
         if (nodes.size() > 0) {
            potentials.put (myGrid.get (index), nodes);
         }
      }
      
      return potentials;
   }

   /** 
    * Return an iterator which iterates through 27 cells.
    *
    * Iterates through the 27 cells which include and are adjacent to
    * the cell containing pos.
    *
    * @param pos A position in the centre bin.
    * @return Iterator for 27 bins.
    */
   public Iterator<List<T>> getCellsNear (Point3d pos) {
      int xIdx = (int) Math.round (pos.x/myGridSpacing);
      int yIdx = (int) Math.round (pos.y/myGridSpacing);
      int zIdx = (int) Math.round (pos.z/myGridSpacing);

      //Iterator<List<T>> it = myIndexList.get (new Index(xIdx, yIdx, zIdx)).listIterator ();
      List<List<T>> list = myIndexList.get(new Index(xIdx, yIdx, zIdx));
      if (list == null) {
         return getCellsNearOld(pos);
      }
      Iterator<List<T>> it = list.listIterator ();
      return it;
   }

   /**
    * This is the old method that for sure works, but might be slower.
    * It requires lots of hashmap lookups. Iterator may iterate through
    * some null cells.
    * 
    * @param pos
    */
   public Iterator<List<T>> getCellsNearOld (Point3d pos) {
      int xIdx = (int) Math.round (pos.x/myGridSpacing);
      int yIdx = (int) Math.round (pos.y/myGridSpacing);
      int zIdx = (int) Math.round (pos.z/myGridSpacing);

      NearCellIter it = new NearCellIter (xIdx, yIdx, zIdx);
      return it;
   }

   public void testIter (int a, int b, int c) {
      NearCellIter it = new NearCellIter (a, b, c);
      System.out.println ("Iterator test:");
      boolean caught = false;
      for (int i=0; i<28; i++) {
         System.out.println (i + ": " + it);
         try {
            it.next();
         } catch (NoSuchElementException e) {
            caught = true;
         }
      }
      if (!caught) {
         throw new RuntimeException("Test failed, didn't catch exception!");
      }
   }

   private static class Index {
      // give direct access to values for convenience 
      private final int x,y,z;
      private Index(int x, int y, int z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
      @Override
      public int hashCode () {
         final int prime = 31;
         int result = 1;
         result = prime * result + x;
         result = prime * result + y;
         result = prime * result + z;
         return result;
      }
      @Override
      public boolean equals (Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass () != obj.getClass ())
            return false;
         Index other = (Index)obj;
         if (x != other.x)
            return false;
         if (y != other.y)
            return false;
         if (z != other.z)
            return false;
         return true;
      }
      
   }

   /** Iterates through all cells adjacent to cell at index (x,y,z)
    *
    * The private constructor ensures it can only be instantiated by a call to getCellsNear();
    */
   private class NearCellIter implements Iterator<List<T>> {
      private final int xc, yc, zc;
      private Index idx;
      
      /** Construct by passing the indices you want to iterate around.
       */
      private NearCellIter (int x, int y, int z) {
         super();
         xc = x; yc = y; zc = z;
         idx = new Index(xc-2,yc-1,zc-1);
      }
      /**
       * Construct a NearCellIter with the index of the central cell you wish
       * to iterate around. 
       * 
       * @param index
       */
      private NearCellIter (Index index) {
         super();
         xc = index.x;
         yc = index.y;
         zc = index.z;
         
         idx = new Index(xc-2,yc-1,zc-1);
      }
      
      @Override
      public boolean hasNext() {
         if (idx.x-xc == 1 && idx.y-yc == 1 && idx.z-zc == 1) {
            return false;
         }
         return true;
      }

      @Override
      public List<T> next() {
         if (idx.x-xc != 1) {
            idx = new Index(idx.x+1,idx.y,idx.z);
         }
         else if (idx.y-yc != 1) {
            idx = new Index(xc-1,idx.y+1,idx.z);
//            idx.vals[1]++;
//            idx.vals[0] = xc-1;
         }
         else if (idx.z-zc != 1) {
            idx = new Index(xc-1,yc-1,idx.z+1);
//            idx.vals[2]++;
//            idx.vals[0] = xc-1;
//            idx.vals[1] = yc-1;
         }
         else {
            throw new NoSuchElementException ("No more adjacent cells!");
         }

         return myGrid.get(idx);
      }

      @Override
      public String toString() {
         return ("( " + idx.x + ", " + idx.y+ ", " + idx.z + " )");
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException ("This iterator doesn't support remove()!!");
      }
   }
}
