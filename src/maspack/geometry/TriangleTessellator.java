/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.fileutil.NativeLibraryManager;
import maspack.matrix.Point2d;

/**
 * Uses the native Triangle code developed by Jonathon Shewchuck
 * to create triangulated 2D meshes.
 *   https://www.cs.cmu.edu/~quake/triangle.html
 *
 * Requires a JNI binary library called TriangleJNI.1.6.  Since Triangle is
 * free only for non-commercial applications, ArtiSynth does not supply it by
 * default. We can supply this library if you meet the license requirements of
 * Triangle.
 */
public class TriangleTessellator {

   private native int doBuildFromPoints (
      long handle, double[] pntCoords, int numPnts, double minAngle);

   private native int doBuildFromSegmentsAndPoints (
      long handle, double[] pntCoords, int numPnts, 
      int[] segmentIndices, int numSegments,
      double minAngle);

   private native int doBuildFromSegments (
      long handle, double[] pntCoords, int numPnts, 
      int[] segmentIndices, int numSegments, 
      double minAngle);

   private native int doRefineMesh(
      long handle, double[] nodeCoords, int numNodes,
      int[] tetIndices, int numTets, double quality);

   private native int doGetNumTriangles (long handle);
   private native void doGetTriangles (long handle, int[] triangles);
   private native int doGetNumPoints (long handle);
   private native void doGetPoints (long handle, double[] pntCoords);
   private native long doCreateTessellator();
   private native void doDeleteTessellator(long handle);

   private long myHandle = 0;
   private static boolean myNativeSupportLoaded = false;

   /**
    * Create a new Triangular Tesselator.  The tesselator does hold state,
    * so should only be used to tesselate one mesh at a time.
    */
   public TriangleTessellator() {
      if (!myNativeSupportLoaded) {
         try {
            NativeLibraryManager.load ("TriangleJNI.1.6");
            myNativeSupportLoaded = true;
         }
         catch (UnsatisfiedLinkError e) {
            System.out.println (e.getMessage());
            throw new UnsupportedOperationException (
               "Can't load native library \"TriangleJNI\"");
         }
      }
   }
   
   /**
    * Build a mesh from a collection of 2D points.  The resulting
    * mesh will consist of the convex hull of the set of points.
    * 
    * @param pnts points to include in mesh
    */
   public void buildFromPoints(Point2d[] pnts) {
      buildFromPoints(pnts, 0);
   }

   /**
    * Construct a quality 2D mesh with a minimum angle within any
    * given triangle.  Additional points may be added to the mesh
    * to allow for the angle constraint. 
    *  
    * @param pnts points to include in mesh
    * @param minAngle minimum angle
    */
   public void buildFromPoints (Point2d[] pnts, double minAngle) {
      int nump = pnts.length;
      double[] coords = new double[2*nump];

      for (int i=0; i<nump; i++) {
         coords[2*i] = pnts[i].x;
         coords[2*i+1] = pnts[i].y;
      }
      buildFromPoints (coords, minAngle);
   }
   
   /**
    * Construct a 2D mesh from a set of point coordinates.  The coordinates are
    * assumed interleaved, {x0,y0,x1,y1,x2,y2,...}.
    * @param coords set of 2D point coordinates
    */
   public void buildFromPoints (double[] coords) {
      buildFromPoints(coords, 0);
   }

   /**
    * Construct a quality 2D mesh with a minimum angle within any
    * given triangle.  Additional points may be added to the mesh
    * to allow for the angle constraint. The coordinates are
    * assumed interleaved, {x0,y0,x1,y1,x2,y2,...}.
    *  
    * @param coords coordinates of points to include in mesh
    * @param minAngle minimum angle
    */
   public void buildFromPoints (double[] coords, double minAngle) {
      if (myHandle == 0) {
         myHandle = doCreateTessellator();
      }
      int nump = coords.length/2;
      if (doBuildFromPoints (myHandle, coords, nump, minAngle) < 0) {
         throw new IllegalArgumentException ("Error creating tesselation");
      }
   }

   /**
    * Builds a constrained mesh that includes the given 2D line segments.
    * 
    * The segments array includes pairs of indices into the points array.
    * For example, <code>segments={0,1,2,3}</code> would include the two
    * line segments, {@code pnts[0] -> pnts[1]}, and 
    * {@code pnts[2] -> pnts[3]}.
    * 
    * @param pnts points to include in the tesselation
    * @param segments line-segments to include as constraints
    */
   public void buildFromSegmentsAndPoints(Point2d[] pnts, int[] segments) {
      buildFromSegmentsAndPoints(pnts, segments, 0);
   }

   /**
    * Builds a constrained mesh that includes the given 2D line segments.
    * 
    * The segments array includes pairs of indices into the points array.
    * For example, <code>segments={0,1,2,3}</code> would include the two
    * line segments,
    * <ul>
    *   <li>{@code (pntCoords[0],pntCoords[1]) -> (pntCoords[2],pntsCoords[3])}, and</li>  
    *   <li>{@code (pntCoords[4],pntCoords[5]) -> (pntCoords[6],pntsCoords[7])}.  </li>
    * </ul>
    * @param pntCoords 2D coordinates of points, interleaved, to include in the tesselation
    * @param segments line-segments to include as constraints
    */
   public void buildFromSegmentsAndPoints(double[] pntCoords, int[] segments) {
      buildFromSegmentsAndPoints(pntCoords, segments, 0);
   }
   
   /**
    * Builds a constrained quality mesh that includes the given 2D line segments, 
    * attempting to guarantee a minimum angle within the triangle.
    * 
    * The segments array includes pairs of indices into the points array.
    * For example, <code>segments={0,1,2,3}</code> would include the two
    * line segments, {@code pnts[0] -> pnts[1]}, and 
    * {@code pnts[2] -> pnts[3]}.
    * 
    * @param pnts points to include in the tesselation
    * @param segments line-segments to include as constraints
    * @param minAngle minimum angle within any given triangle
    */
   public void buildFromSegmentsAndPoints(Point2d[] pnts, int[] segments, double minAngle) {

      int numPnts = pnts.length;
      double[] pntCoords = new double[2*numPnts];
      int idx = 0;
      for (int i=0; i<numPnts; i++) {
         pntCoords[idx++ ] = pnts[i].x;
         pntCoords[idx++ ] = pnts[i].y;
      }

      buildFromSegmentsAndPoints(pntCoords, segments, minAngle);
   }
   
   /**
    * Builds a constrained quality mesh that includes the given 2D line segments, 
    * attempting to guarantee a minimum angle within the triangle.
    * 
    * The segments array includes pairs of indices into the points array.
    * For example, <code>segments={0,1,2,3}</code> would include the two
    * line segments,
    * <ul>
    *   <li>{@code (pntCoords[0],pntCoords[1]) -> (pntCoords[2],pntsCoords[3])}, and</li>  
    *   <li>{@code (pntCoords[4],pntCoords[5]) -> (pntCoords[6],pntsCoords[7])}.  </li>
    * </ul>
    * @param pntCoords 2D coordinates of points, interleaved, to include in the tesselation
    * @param segments line-segments to include as constraints
    * @param minAngle minimum angle within any given triangle
    */
   public void buildFromSegmentsAndPoints(double[] pntCoords, int[] segments, double minAngle) {
      if (myHandle == 0) {
         myHandle = doCreateTessellator();
      }

      int numPnts = pntCoords.length/2;
      if (doBuildFromSegmentsAndPoints (myHandle, pntCoords, numPnts,
         segments, segments.length/2, minAngle) < 0) {
         throw new IllegalArgumentException ("Error creating tesselation");
      }

   }

   /** 
    * Returns the vertex indices of all the triangles in this tessellation.
    * If <code>idxs</code> is the array returned by this method, then the
    * length of <code>idxs</code> is three times the number of triangles, with
    * the vertex indices of the first triangle given by
    * <code>idxs[0]</code>, <code>idxs[1]</code>, <code>idxs[2]</code>
    *
    * <p>
    * The vertices are ordered such that if v0 is the first vertex,
    * v0, v1, and v2 are arranged counter-clockwise
    * @return vertices for the triangles in this tessellation.
    */
   public int[] getTriangles() {
      int numTris = doGetNumTriangles (myHandle);
      int[] tris = new int[3*numTris];
      if (numTris > 0) {
         doGetTriangles (myHandle, tris);
      }
      return tris;
   }

   /**
    * The number of triangles in the resulting tessellation
    * @return number of triangles
    */
   public int getNumTriangles() {
      return doGetNumTriangles(myHandle);
   }
   
   /**
    * Number of points in the resulting tessellation
    * @return number of points
    */
   public int getNumPoints() {
      return doGetNumPoints (myHandle);
   }
   
   /**
    * The 2D coordinates of points in the tesselation, interleaved.
    * @return coordinates of 2D points
    */
   public double[] getPointCoords() {
      double[] coords;

      int numPnts = doGetNumPoints (myHandle);
      coords = new double[2*numPnts];
      if (numPnts > 0) {
         doGetPoints (myHandle, coords);
      }

      return coords;
   }

   /**
    * The final 2D points in the tessellation
    * @return 2D points
    */
   public Point2d[] getPoints() {
      double[] coords = getPointCoords();
      int nump = coords.length/2;
      Point2d[] pnts = new Point2d[nump];
      for (int i=0; i<nump; i++) {
         pnts[i] = new Point2d (coords[2*i], coords[2*i+1]);
      }
      return pnts;
   }

   /**
    * Destroy the tessellator
    */
   public void dispose () {
      if (myHandle != 0) {
         doDeleteTessellator (myHandle);
         myHandle = 0;
      }
   }

   public void finalize () {
      dispose();
   }

   public static void main (String[] args) {
      TriangleTessellator tesser = new TriangleTessellator();
   }

}
