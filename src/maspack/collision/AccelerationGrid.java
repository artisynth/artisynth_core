package maspack.collision;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

public class AccelerationGrid<T> {
   public ArrayList<LinkedHashSet<T>> cells = null;
   public LinkedHashMap<T,ArrayList<int[]>> elementidxs = null;
   public Point3d xmin = null, xmax = null;
   public int nx, ny, nz;
   public Vector3d cellsize = null;

   public void add_element (T elem, Point3d min, Point3d max) {
      ArrayList<int[]> eidxs = elementidxs.get (elem);
      if (eidxs == null) {
         eidxs = new ArrayList<int[]>();
         elementidxs.put (elem, eidxs);
      }

      int[][] idxs = getIndexes (min, max);

      // System.out.println("idxs");
      // System.out.println(idxs[0][0] + " " + idxs[0][1] + " " + idxs[0][2]);
      // System.out.println(idxs[1][0] + " " + idxs[1][1] + " " + idxs[1][2]);

      for (int i = idxs[0][0]; i <= idxs[1][0]; i++)
         for (int j = idxs[0][1]; j <= idxs[1][1]; j++)
            for (int k = idxs[0][2]; k <= idxs[1][2]; k++) {
               LinkedHashSet<T> cell = get (i, j, k);
               if (cell == null) {
                  cell = new LinkedHashSet<T>();
                  set (i, j, k, cell);
               }
               if (!cell.contains (elem)) {
                  cell.add (elem);
                  eidxs.add (new int[] { i, j, k });
               }
            }
   }

   public void remove_element (T elem) {
      ArrayList<int[]> eidxs = elementidxs.get (elem);
      for (int[] cidx : eidxs) {
         LinkedHashSet<T> cell = get (cidx[0], cidx[1], cidx[2]);
         cell.remove (elem);
      }

      elementidxs.remove (elem);
   }

   public void set (Point3d _xmin, Point3d _xmax, int n) {
      Vector3d size = new Vector3d();
      size.sub (_xmax, _xmin);

      double elementspercell = 0.5;
      double volume = size.x * size.y * size.z;
      double volumepercell = volume / n * elementspercell;
      double idealcellsize = Math.pow (volumepercell, 1.0 / 3.0);

      int x = Math.min ((int)Math.ceil (size.x / idealcellsize), n);
      int y = Math.min ((int)Math.ceil (size.y / idealcellsize), n);
      int z = Math.min ((int)Math.ceil (size.z / idealcellsize), n);

      // System.out.println("set " + size + " " + x + " " + y + " " + z + " " +
      // n);

      set (_xmin, _xmax, x, y, z);
   }

   public void set (Point3d _xmin, Point3d _xmax, int _nx, int _ny, int _nz) {
      xmin = _xmin;
      xmax = _xmax;
      nx = _nx;
      ny = _ny;
      nz = _nz;

      cellsize =
         new Vector3d (
            (xmax.x - xmin.x) / nx, (xmax.y - xmin.y) / ny, (xmax.z - xmin.z)
            / nz);

      int n = nx * ny * nz;
      elementidxs = new LinkedHashMap<T,ArrayList<int[]>>();
      cells = new ArrayList<LinkedHashSet<T>>();
      for (int i = 0; i < n; i++)
         cells.add (null);
   }

   public LinkedHashSet<T> get (int x, int y, int z) {
      return cells.get (x * ny * nz + y * nz + z);
   }

   public void set (int x, int y, int z, LinkedHashSet<T> cell) {
      cells.set (x * ny * nz + y * nz + z, cell);
   }

   public int[] getIndex (Point3d p) {
      int[] idx =
         new int[] { (int)Math.floor ((p.x - xmin.x) / cellsize.x),
                    (int)Math.floor ((p.y - xmin.y) / cellsize.y),
                    (int)Math.floor ((p.z - xmin.z) / cellsize.z) };

      return idx;
   }

   public int[][] getIndexes (Point3d min, Point3d max) {
      int[][] idxs = new int[2][];

      idxs[0] = getIndex (min);
      idxs[1] = getIndex (max);

      // System.out.println("idxs " + min + " " + max);
      // System.out.println(idxs[0][0] + " " + idxs[0][1] + " " + idxs[0][2]);
      // System.out.println(idxs[1][0] + " " + idxs[1][1] + " " + idxs[1][2]);

      idxs[0][0] = Math.max (0, idxs[0][0]);
      idxs[0][1] = Math.max (0, idxs[0][1]);
      idxs[0][2] = Math.max (0, idxs[0][2]);

      idxs[1][0] = Math.min (nx - 1, idxs[1][0]);
      idxs[1][1] = Math.min (ny - 1, idxs[1][1]);
      idxs[1][2] = Math.min (nz - 1, idxs[1][2]);

      return idxs;
   }

   public ArrayList<T> find_overlapping_elements (Point3d min, Point3d max) {
      ArrayList<T> elems = new ArrayList<T>();

      int[][] idxs = getIndexes (min, max);

      for (int i = idxs[0][0]; i <= idxs[1][0]; i++)
         for (int j = idxs[0][1]; j <= idxs[1][1]; j++)
            for (int k = idxs[0][2]; k <= idxs[1][2]; k++) {
               LinkedHashSet<T> cell = get (i, j, k);
               if (cell != null) {
                  for (T elem : cell) {
                     if (!elems.contains (elem))
                        elems.add (elem);
                  }
               }
            }

      return elems;
   }
}
