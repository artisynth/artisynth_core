/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.List;

import maspack.matrix.Point3d;

/**
 * KD-Tree based on Point3d
 * @author Antonio
 *
 */
public class KDTree3d extends KDTree<Point3d>{

   private static class Point3dKdComparator implements KDComparator<Point3d> {

      @Override
      public int compare(Point3d a, Point3d b, int dim) {
         double x = a.get(dim);
         double y = b.get(dim);
         
         if ( x < y) {
            return -1;
         } else if ( x > y) {
            return 1;
         }
         return 0;
      }

      @Override
      public double distance(Point3d a, Point3d b) {
         return a.distanceSquared(b);
      }

      @Override
      public double distance(Point3d a, Point3d b, int dim) {
         double d = a.get(dim)-b.get(dim);
         return d*d;
      }
      
   }
   
   /**
    * Default constructor starting with a supplied list of points
    */
   public KDTree3d (List<Point3d> pnts) {
      super(3, pnts, new Point3dKdComparator());
   }

}
