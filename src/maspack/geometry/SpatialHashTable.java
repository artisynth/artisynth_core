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
 * Spatial Hash Table for speeding up 3D spatial nearest neighbour searches.<br><br>
 * 
 * 3D space is partitioned into an orthogonal, equally spaced grid. Each grid 
 * point is the centre of a "cell", which is essentially a List&lt;T&gt;. To search
 * for elements T near a point P, the cell containing P, AND the cells 
 * that neighbour the cell containing P must be searched. These cells can be 
 * easily found by calling getCellsNear or getCellsIntersecting, which returns
 * an Iterator&lt;List&lt;T&gt;&gt;, where each List&lt;T&gt; represents the elements in a single 
 * cell. <br><br>
 * 
 * Grid spacing is defined upon construction, and construction is performed 
 * by calling setup().<br><br>
 
 * Example usage:<br>
 * <pre>
 * List&lt;T&gt; myElements;
 * ...
 * SpatialHashTable&lt;T&gt; table = new SpatialHashTable&lt;&gt;(spacing);
 * 
 * List&lt;Point3d&gt; positions = new ArrayList&lt;Point3d&gt;();
 * for (T el : myElements) {
 *    positions.add(el.getPos());
 * }
 * 
 * table.setup(positions,elements); // construct map 
 * 
 * Point3d mySearchPoint = new Point3d(0., 1., 2.);
 * 
 * Iterator&lt;List&lt;T&gt;&gt; it = table.getCellsNear (searchPoint);
 * while (it.hasNext()) {
 *    List&lt;T&gt; cell = it.next();
 *    for (T el : cell) {
 *       el.doSomething();
 *    }
 * }
 * </pre>
 * @author andrew ho
 */
public class SpatialHashTable<T> {
   private final double myGridSpacing;
   private HashMap<Index, List<T>> myGrid = new HashMap<>();
   private HashMap<Index, List<T>> myIndexList = new HashMap<>();

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
      myIndexList.clear ();

      if (positions.size() != elements.size()) {
         throw new IllegalArgumentException ("Non-matching position and element sizes!");
      }

      for (int cnt=0; cnt<elements.size (); cnt++) {
         T p = elements.get(cnt);
         
         // This is ok since negative indices are allowed now
         Point3d pos = positions.get(cnt);
         int ix = (int) Math.round((pos.x)/myGridSpacing);
         int iy = (int) Math.round((pos.y)/myGridSpacing);
         int iz = (int) Math.round((pos.z)/myGridSpacing);
         Index idx = new Index(ix,iy,iz);

         List<T> parList = myGrid.get(idx);

         if (parList == null) {
            parList = new ArrayList<T>();
            myGrid.put(idx, parList);
         }
         
         parList.add(p);

         // Put into indexlist
         for (int i=-1; i<=1; i++) {
            for (int j=-1; j<=1; j++) {
               for (int k=-1; k<=1; k++) {
                  idx = new Index(ix+i,iy+j,iz+k);

                  parList = myIndexList.get(idx);
                  if (parList == null) {
                     parList = new ArrayList<T>();
                     myIndexList.put(idx, parList);
                  }
                  parList.add (p);
               }
            }
         }
      }
   }
   
   /**
    * Returns a List of cells (List&lt;T&gt;'s) that might intersect a
    * bv tree.
    * 
    * @param bvtree
    */
   //public Map<List<T>,ArrayList<BVNode>> getCellsIntersecting (BVTree bvtree) {
   //   Map<List<T>,ArrayList<BVNode>> potentials = new HashMap<List<T>,ArrayList<BVNode>>();
   //   
   //   // Do this naively and just intersect sphere of each cell with the bvtree.
   //   Point3d cellCentre = new Point3d();
   //   // 2D pythagorean
   //   double searchRad = myGridSpacing * Math.sqrt(0.5);
   //   // 3D
   //   searchRad = Math.sqrt (searchRad*searchRad + myGridSpacing*myGridSpacing/4);
   //   searchRad += myGridSpacing;
   //   double sphereRadius = Math.sqrt(2*searchRad*searchRad);
   //   for (Index idx : myGrid.keySet ()) {
   //      ArrayList<BVNode> nodes = new ArrayList<BVNode> ();
   //      // We need to add all cells which do intersect, and their neighbouring cells.
   //      cellCentre.x = idx.x * myGridSpacing;
   //      cellCentre.y = idx.y * myGridSpacing;
   //      cellCentre.z = idx.z * myGridSpacing;
   //      
   //      bvtree.intersectSphere (nodes, cellCentre, sphereRadius);
   //      if (nodes.size() > 0) {
   //         potentials.put (myGrid.get (idx), nodes);
   //      }
   //   }
   //   
   //   return potentials;
   //}

   /** 
    * Return an iterator which iterates through 27 cells.
    *
    * Iterates through the 27 cells which include and are adjacent to
    * the cell containing pos.
    *
    * @param pos A position in the centre bin.
    * @return Iterator for 27 bins.
    */
   public List<T> getElsNear (Point3d pos) {
      int xIdx = (int) Math.round (pos.x/myGridSpacing);
      int yIdx = (int) Math.round (pos.y/myGridSpacing);
      int zIdx = (int) Math.round (pos.z/myGridSpacing);
      //int[] idx = new int[]{xIdx, yIdx, zIdx};
      Index idx = new Index(xIdx,yIdx,zIdx);
      return myIndexList.get(idx);
   }

   /**
    * Immutable inner key class for hash function
    * @author andrew
    *
    */
   private static class Index {
      // give direct access to values for convenience 
      public final int x,y,z;
      private Index(int x, int y, int z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
      
      @Override
      public int hashCode () {
         final int p1 = 73856093;
         final int p2 = 19349663;
         final int p3 = 83492791;
         
         //int M = SpatialHashTable.this.myGrid.size();
         //M = M==0?1:M;
       
         return (x*p1 ^ y*p2 ^ z*p3);//% M;
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
   
}
