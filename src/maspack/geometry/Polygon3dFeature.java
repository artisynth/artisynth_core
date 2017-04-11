package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;

public class Polygon3dFeature {

   double myVtxDist = -1;
   double myEdgeDist = -1;

   Vertex3dNode myNode;

   Point3d myPA;    // first feature vertex
   Point3d myPB;    // second feature vertex
   Point3d myPC;    // third feature vertex

   int myOutsideIfClockwise = -1;

   /**
    * Returns the number of polygon vertices associated with this feature:
    *
    * <dl>
    * <dt> 0: no feature is currently defined
    * <dt> 1: feature is a single polygon vertex; no edges have been encountered
    * <dt> 2: feature is an edge
    * <dt> 3: feature is the vertex indexed by 1, with 0 and 2 indexing
    * the previous and following vertices
    * </dl>
    * 
    * @return number of feature vertices
    */
   public int numVertices() {
      if (myPA == null) {
         return 0;
      }
      else if (myPB == null) {
         return 1;
      }
      else if (myPC == null) {
         return 2;
      }
      else {
         return 3;
      }
   }

   /**
    * Returns the <code>idx</code>-th polygon vertex associated with this
    * feature.
    * @param idx vertex index. Should be less than the value returned by
    * {@link #numVertices}.
    * @return <code>idx</code>-th polygon vertex. Value is a reference
    * and should not be modified.
    */
   public Point3d getVertex (int idx) {
      switch (idx) {
         case 0: return myPA;
         case 1: return myPB;
         case 2: return myPC;
         default: {
            throw new IndexOutOfBoundsException (
               "Index "+idx+" not in the range [0,2]");
         }
      }
   }

   public Vertex3dNode getVertexNode () {
      return myNode;
   }

   /**
    * Returns the distance to the nearest feature, or -1 if no feature has been
    * determined yet.
    * 
    * @return distance to the nearest feature.
    */
   public double getDistance() {
      if (myPA == null) {
         return -1;
      }
      else if (myPB == null) {
         return myVtxDist;
      }
      else {
         return myEdgeDist;
      }
   }
   

   /**
    * Returns the nearest point on the nearest fearure, or <code>null</code>
    * if no feature has been determined yet.
    *
    * @return nearest point on the nearest feature
    */
   public Point3d getNearestPoint (Point3d px) {
      switch (numVertices()) {
         case 0: {
            return null;
         }
         case 1: {
            return new Point3d(myPA);
         }
         case 2: {
            double s = LineSegment.projectionParameter (myPA, myPB, px);
            Point3d pnt = new Point3d();
            pnt.combine (1-s, myPA, s, myPB);
            return pnt;
         }
         case 3: {
            return new Point3d(myPC);
         }
         default: {
            throw new InternalErrorException (
               "Unexpected number of vertices: " + numVertices());
         }
      }
   }

   /**
    * Prints out a description of this feature.
    * 
    * @param msg optional message line
    */
   public void print (String msg) {
      if (msg != null) {
         System.out.println (msg);
      }
      String desc;
      switch (numVertices()) {
         case 0: desc = "UNDEFINED"; break;
         case 1: desc = "VERTEX"; break;
         case 2: desc = "EDGE"; break;
         case 3: desc = "VERTEX"; break;
         default: {
            throw new InternalErrorException (
               "numVertices=" + numVertices() + ", should be between 0 and 3");
         }
      }
      System.out.println (desc + ", nverts=" + numVertices());
      if (numVertices() > 0) {
         System.out.println ("  pa=" + getVertex(0).toString ("%12.8f"));
      }
      if (numVertices() > 1) {
         System.out.println ("  pb=" + getVertex(1).toString ("%12.8f"));
      }
      if (numVertices() > 2) {
         System.out.println ("  pc=" + getVertex(2).toString ("%12.8f"));
      }
   }
   
   public void setNearestVertex (
      Point3d pa, Point3d pb, Point3d pc, boolean outsideIfClockwise) {
      myPA = pa;
      myPB = pb;
      myPC = pc;
      myOutsideIfClockwise = outsideIfClockwise ? 1 : 0;
   }

   public void setNearestEdge (
      Point3d pa, Point3d pb, boolean outsideIfClockwise) {
      myPA = pa;
      myPB = pb;
      myPC = null;
      myOutsideIfClockwise = outsideIfClockwise ? 1 : 0;
   }

   public int isOutside (boolean clockwise) {
      if (myOutsideIfClockwise == -1) {
         return -1;
      }
      else {
         if (clockwise) {
            return myOutsideIfClockwise;
         }
         else {
            return myOutsideIfClockwise == 0 ? 1 : 0;         
         }
      }       
   }

}

