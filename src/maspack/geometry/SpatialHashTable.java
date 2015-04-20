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
   private double myGridSpacing;
   private HashMap<Index, List<T>> myGrid = new HashMap<Index, List<T>>();
//   private ArrayList<Index> myUsedIndexList;
   private boolean myIndexListInitialized = false;
   private HashMap<Index, List<List<T>>> myIndexList = new HashMap<Index, List<List<T>>>();

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
      myIndexListInitialized = false;
//      myUsedIndexList.clear();
      if (positions.size() != elements.size()) {
         throw new IllegalArgumentException ("Non-matching position and element sizes!");
      }
      Index idx = new Index();
      for (int i=0; i<elements.size (); i++) {
         T p = elements.get(i);
         
         int idxX, idxY, idxZ;
         
         // This is ok since negative indices are allowed now
//         idxX = (int) Math.round((p.getPosReadOnly().x)/myGridSpacing);
//         idxY = (int) Math.round((p.getPosReadOnly().y)/myGridSpacing);
//         idxZ = (int) Math.round((p.getPosReadOnly().z)/myGridSpacing);
         Point3d pos = positions.get(i);
         idxX = (int) Math.round((pos.x)/myGridSpacing);
         idxY = (int) Math.round((pos.y)/myGridSpacing);
         idxZ = (int) Math.round((pos.z)/myGridSpacing);

         idx.vals[0] = idxX;
         idx.vals[1] = idxY;
         idx.vals[2] = idxZ;

         List<T> parList = myGrid.get(idx);

         if (parList == null) {
            Index newIndex = new Index();
            newIndex.vals[0] = idxX;
            newIndex.vals[1] = idxY;
            newIndex.vals[2] = idxZ;
            
            parList = new ArrayList<T>();
            myGrid.put(newIndex, parList );
//            myUsedIndexList.add( newIndex );
         }
         
         parList.add(p);
      }
      setupListsToNeighbours();
   }
   
//   /**
//    * Add a single element into the spatial hash table
//    * @param pos position of the element
//    * @param el element to be added.
//    */
//   public synchronized void addElement (Point3d pos, T el) {
//      Index idx = new Index();
//      // This is ok since negative indices are allowed now
//      idx.vals[0] = (int) Math.round((pos.x)/myGridSpacing);
//      idx.vals[1] = (int) Math.round((pos.y)/myGridSpacing);
//      idx.vals[2] = (int) Math.round((pos.z)/myGridSpacing);
//      
//      List<T> parList = myGrid.get(idx);
//
//      if (parList == null) {
//         Index newIndex = new Index();
//         newIndex.vals[0] = idx.vals[0];
//         newIndex.vals[1] = idx.vals[1];
//         newIndex.vals[2] = idx.vals[2];
//         
//         parList = new ArrayList<T>();
//         myGrid.put(newIndex, parList);
//      }
//      
//      parList.add(el);
//      myIndexListInitialized = false;
//   }
   
   protected synchronized void setupListsToNeighbours () {
      if (myIndexListInitialized) {
         return;
      }
      myIndexList.clear ();
      for (Index index : myGrid.keySet ()) {
         List<List<T>> list = new LinkedList<List<T>>();
         NearCellIter it = new NearCellIter(index.vals[0], index.vals[1], index.vals[2]);
         while (it.hasNext ()) {
            List<T> cells = it.next();
            if (cells == null) {
               continue;
            }
            list.add (cells);
         }

         myIndexList.put(index,list);
      }
      myIndexListInitialized = true;
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
         cellCentre.x = index.vals[0] * myGridSpacing;
         cellCentre.y = index.vals[1] * myGridSpacing;
         cellCentre.z = index.vals[2] * myGridSpacing;
         
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
      if (!myIndexListInitialized) 
         setupListsToNeighbours();

      int xIdx = (int) Math.round (pos.x/myGridSpacing);
      int yIdx = (int) Math.round (pos.y/myGridSpacing);
      int zIdx = (int) Math.round (pos.z/myGridSpacing);

      Iterator<List<T>> it = myIndexList.get (new Index(xIdx, yIdx, zIdx)).listIterator ();
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

   private class Index {
      // give direct access to values for convenience 
      protected int[] vals;
      private Index() {
         vals = new int[] {0,0,0};
      }
      private Index(int x, int y, int z) {
         vals = new int[] {x,y,z};
      }
      @Override
      public int hashCode () {
         final int prime = 31;
         int result = 1;
         result = prime * result + Arrays.hashCode (vals);
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
         if (!Arrays.equals (vals, other.vals))
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
         idx = new Index();
         idx.vals[0] = xc - 2;
         idx.vals[1] = yc - 1;
         idx.vals[2] = zc - 1;
      }
      /**
       * Construct a NearCellIter with the index of the central cell you wish
       * to iterate around. 
       * 
       * @param index
       */
      private NearCellIter (Index index) {
         super();
         xc = index.vals[0];
         yc = index.vals[1];
         zc = index.vals[2];
         
         idx = new Index();
         idx.vals[0] = xc - 2;
         idx.vals[1] = yc - 1;
         idx.vals[2] = zc - 1;
      }
      
      @Override
      public boolean hasNext() {
         if (idx.vals[0]-xc == 1 && idx.vals[1]-yc == 1 && idx.vals[2]-zc == 1) {
            return false;
         }
         return true;
      }

      @Override
      public List<T> next() {
         if (idx.vals[0]-xc != 1) {
            idx.vals[0]++;
         }
         else if (idx.vals[1]-yc != 1) {
            idx.vals[1]++;
            idx.vals[0] = xc-1;
         }
         else if (idx.vals[2]-zc != 1) {
            idx.vals[2]++;
            idx.vals[0] = xc-1;
            idx.vals[1] = yc-1;
         }
         else {
            throw new NoSuchElementException ("No more adjacent cells!");
         }

         return myGrid.get(idx);
      }

      @Override
      public String toString() {
         return ("( " + idx.vals[0] + ", " + idx.vals[1]+ ", " + idx.vals[2] + " )");
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException ("This iterator doesn't support remove()!!");
      }
   }
}
