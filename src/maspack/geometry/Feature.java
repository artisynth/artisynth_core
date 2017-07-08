/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;

/**
 * Super class for vertices, half-edges, and faces.
 */
public abstract class Feature implements Boundable {
   protected int myFlags;

   public static int VISITED = 0x01000000;
   
   public static final int UNKNOWN = 0;
   public static final int VERTEX_3D = 1;
   public static final int HALF_EDGE = 2;
   public static final int FACE = 3;
   public static final int LINE_SEGMENT = 4;
   public static final int EDGE = 5;
   public static final int FACET = 6;
   public static final int CELL = 7;
   public static final int TYPE_MASK = 0xff;

   public Feature (int type) {
      this.myFlags = type & TYPE_MASK;
   }
   
   public void setVisited() {
      myFlags |= VISITED;
   }
   
   public boolean isVisited() {
      return ((myFlags & VISITED) != 0);
   }
   
   public void clearVisited() {
      myFlags = myFlags & ~VISITED;
   }

   public int getType() {
      return myFlags & TYPE_MASK;
   }

   public String getTypeName() {
      switch (getType()) {
         case VERTEX_3D: {
            return "VERTEX_3D";
         }
         case LINE_SEGMENT: {
            return "LINE_SEGMENT";
         }
         case HALF_EDGE: {
            return "HALF_EDGE";
         }
         case FACE: {
            return "FACE";
         }
         case FACET: {
            return "FACET";
         }
         case EDGE: {
            return "EDGE";
         }
         case CELL: {
            return "CELL";
         }
         default: {
            return "???";
         }
      }
   }
   
   public boolean checkFlag(int mask) {
      if ( (myFlags & mask) > 0) {
         return true;
      }
      return false;
   }
   
   public void setFlag(int mask) {
      myFlags |= mask;
   }
   
   public void clearFlag(int mask) {
      myFlags = myFlags & ~mask;
   }

   /**
    * Determine nearest point to this feature
    * @param nearest populated nearest point
    * @param pnt point to check distance to
    */
   public abstract void nearestPoint(Point3d nearest, Point3d pnt);

   //   public boolean voronoiCheck (Point3d pnt) {
   //      return true;
   //   }
}
