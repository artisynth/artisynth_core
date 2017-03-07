/**
 * Copyright (c) 2017, by the Authors: Bruce Haines (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.awt.Color;
import java.util.*;

import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.util.Logger;
import maspack.geometry.SignedDistanceGridCell;
//TODO: Include reference to Bridson's code.
/**
 * SignedDistanceGrid is a class used to create, render, and use
 * a signed distance field for collision detection purposes.
 *
 */
public class SignedDistanceGrid implements IsRenderable {

   private PolygonalMesh mesh;
   private Vector3d gridCellSize;
   private double[] phi;
   private Vector3d[] normals;
   public static double OUTSIDE = Double.MAX_VALUE;
   /**
    * Maximum of the signed distance field, in mesh coordinates.
    */
   private Point3d max = new Point3d();
   /**
    * Minimum of the signed distance field, in mesh coordinates.
    */
   private Point3d min = new Point3d();
   /**
    * Represents the number of cells in the signed distance field
    * for each direction.
    */
   private int[] gridSize = new int[3];
   /**
    * An array of faces copied from the mesh.
    */
   private Face[] faces;
   /**
    * An array of indices referencing to {@link #faces faces}.
    * To get the closest face to a point represented in phi,
    * use: faces[closestFace[<index of phi>]
    */
   private int[] closestFace;

   private SignedDistanceGridCell gridCellArray[];

   public SignedDistanceGrid () {
   }

   public SignedDistanceGrid (PolygonalMesh m, double gridMarginFraction) {
      mesh = m;
      if (!mesh.isTriangular()) {
         mesh.triangulate();
      }
      ArrayList<Face> mfaces = mesh.getFaces();
      faces = mfaces.toArray(new Face[mfaces.size()]);
      
      // Calculate cell size based on the average triangle size.
      //      double avg = 0;
      //      for (int h = 0; h < faces.length; h++) {
      //         Vector3d v0 = faces[h].he0.head.pnt;
      //         Vector3d v1 = faces[h].he0.next.head.pnt;
      //         Vector3d v2 = faces[h].he0.next.next.head.pnt;
      //         avg += v0.triangleArea (v1, v2);
      //      }
      //      avg /= faces.length;      
      //      double length = Math.sqrt(avg) / 4.0;
      //      gridCellSize = new Vector3d (length, length, length);
      gridCellSize = new Vector3d ();
      mesh.getLocalBounds (min, max);
      // add margin to both min and max
      Vector3d margin = new Vector3d();
      margin.sub(max, min);
      margin.scale(gridMarginFraction);
      max.add(margin);
      min.sub(margin);
      
      gridCellSize.x = (max.x - min.x) / 25.0; // Arbitrary
      gridCellSize.y = (max.y - min.y) / 25.0;
      gridCellSize.z = (max.z - min.z) / 25.0;
      
      calculatePhi ();
   }

   public SignedDistanceGrid (PolygonalMesh m, double gridMarginFraction, 
      int maxCellDivisions) {
      mesh = m;
      if (!mesh.isTriangular()) {
         mesh.triangulate();
      }
      ArrayList<Face> mfaces = mesh.getFaces();
      faces = mfaces.toArray(new Face[mfaces.size()]);
      
      Vector3d widths = new Vector3d();
      mesh.getLocalBounds (min, max);
      widths.sub (max, min);
      Vector3d margin = new Vector3d();
      margin.scale(gridMarginFraction, widths);
      max.add(margin);
      min.sub(margin);
      widths.scaledAdd(2, margin);
      
      double size = widths.maxElement()/maxCellDivisions;
      gridCellSize = new Vector3d (size, size, size);
      
      calculatePhi ();      
   }
   
   private SignedDistanceGridCell[] buildGridCellArray() {
       gridCellArray = new SignedDistanceGridCell [phi.length];
       for (int p = 0; p<phi.length; ++p) {
          gridCellArray [p] = new SignedDistanceGridCell (p, this);
          gridCellArray [p].setDistance(phi[p]);
       }
      return gridCellArray;
   }
   
   // construct on demand
   public SignedDistanceGridCell[] getGridCells() {
      if (gridCellArray == null) {
         gridCellArray = buildGridCellArray();
      }
      return gridCellArray;
   }
   
   public SignedDistanceGrid (PolygonalMesh m, double gridMarginFraction, 
      Vector3d cellDivisions) {
      mesh = m;
      if (!mesh.isTriangular()) {
         mesh.triangulate();
      }
      ArrayList<Face> mfaces = mesh.getFaces();
      faces = mfaces.toArray(new Face[mfaces.size()]);

      gridCellSize = new Vector3d ();
      mesh.getLocalBounds (min, max);
      Vector3d margin = new Vector3d();
      margin.sub(max, min);
      margin.scale(gridMarginFraction);
      max.add(margin);
      min.sub(margin);
      
      gridCellSize.x = (max.x - min.x) / cellDivisions.x; // Arbitrary
      gridCellSize.y = (max.y - min.y) / cellDivisions.y;
      gridCellSize.z = (max.z - min.z) / cellDivisions.z;
      
      calculatePhi ();
   }

   /** 
    * Calculates the signed distance field.
    *
    * @param gridMargin Ratio to scale the boundary of the grid beyond the
    * mesh boundary.
    */
   private void calculatePhi () {
      Logger logger = Logger.getSystemLogger();
      logger.info ("Calculating Signed Distance Field...");

      // gridSize represents the maximum point in grid coordinates, rounded
      // up to the nearest full cell.
      // +1 for rounding up after casting to an integer.
      // +1 because it is a node count, not cell count
      gridSize[0] = (int)(Math.ceil( (max.x - min.x) / gridCellSize.x))+1; 
      gridSize[1] = (int)(Math.ceil( (max.y - min.y) / gridCellSize.y))+1;
      gridSize[2] = (int)(Math.ceil((max.z - min.z) / gridCellSize.z))+1;
      double maxDist = Math.sqrt ((double)(gridSize[0] - 1 ) * gridCellSize.x *
         (double)(gridSize[0] - 1 ) * gridCellSize.x +
         (double)(gridSize[1] - 1 ) * gridCellSize.y *
         (double)(gridSize[1] - 1 ) * gridCellSize.y +
         (double)(gridSize[2] - 1 ) * gridCellSize.z *
         (double)(gridSize[2] - 1 ) * gridCellSize.z);

      phi = new double [gridSize[0] * gridSize[1] * gridSize[2]];
      normals = new Vector3d [gridSize[0] * gridSize[1] * gridSize[2]];

      for (int p = 0; p < phi.length; p++) {
         phi [p] = maxDist;
      }
      // The index of closestFace matches with phi.
      // Each entry in closestFace represents the index of faces[] that 
      // corresponds to the closest face to the grid vertex.
      closestFace = new int[phi.length];
      int zIntersectionCount[] = new int[phi.length];
      
      for (int i = 0; i < phi.length; i++) {
         closestFace[i] = -1;
         zIntersectionCount[i] = 0;
      }
      Point3d faceMin          = new Point3d();
      Point3d faceMax          = new Point3d();
      Point3d closestPoint     = new Point3d();
      // Point3d currentPoint     = new Point3d();
      Point3d currentPointMesh = new Point3d();
      Point3d bary    = new Point3d();
      Point3d pc = new Point3d(); // A temp variable passed in for performance.
      Point3d p1 = new Point3d(); // A temp variable passed in for performance.
      Point3d bottom = new Point3d();
      Point3d top = new Point3d();
      
      boolean sdPredsWarning = false;

      // For every triangle...
      for (int t = 0; t < faces.length; t++) {
         Face face = faces[t];
         faceMin.set (Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY);
         faceMax.set (Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY, 
            Double.NEGATIVE_INFINITY);
         face.updateBounds (faceMin, faceMax);
         // Converting mesh min/max to grid coordinates.
         int faceMinX = (int)((faceMin.x - min.x) / gridCellSize.x);
         int faceMinY = (int)((faceMin.y - min.y) / gridCellSize.y);
         int faceMinZ = (int)((faceMin.z - min.z) / gridCellSize.z);
         if (faceMinX < 0) {
            faceMinX = 0;
         }
         if (faceMinY < 0) {
            faceMinY = 0;
         }
         if (faceMinZ < 0) {
            faceMinZ = 0;
         }
         int faceMaxX = (int)((faceMax.x - min.x) / gridCellSize.x) + 1;
         int faceMaxY = (int)((faceMax.y - min.y) / gridCellSize.y) + 1;
         int faceMaxZ = (int)((faceMax.z - min.z) / gridCellSize.z) + 1;
         if (faceMaxX > gridSize[0] - 1) {
            faceMaxX = gridSize[0] - 1;
         }
         if (faceMaxY > gridSize[1] - 1) {
            faceMaxY = gridSize[1] - 1;
         }
         if (faceMaxZ > gridSize[2] - 1) {
            faceMaxZ = gridSize[2] - 1;
         }

         // Now go through the entire parallelpiped. Calculate distance and
         // closestFace.
         for (int z = faceMinZ; z <= faceMaxZ; z++) {
            for (int y = faceMinY; y <= faceMaxY; y++) {
               for (int x = faceMinX; x <= faceMaxX; x++) {
                  // Get mesh coordinates
                  getMeshCoordinatesFromGrid (currentPointMesh,
                     (double)x, (double)y, (double)z);
                  // Get the distance from this point to the face.
                  face.nearestPoint (closestPoint, currentPointMesh);
                  double distance = currentPointMesh.distance (closestPoint);
                  int index = x +
                  y * gridSize[0] +
                  z * gridSize[0] * gridSize[1];
                  if (distance < phi[index]) {
                     phi [index] = distance;
                     closestFace [index] = t;
                  }
               }
            }
         }
         
         // Ray-casts from bottom x-y plane, upwards, counting intersections.
         // We're building intersectionCount[] to use in ray casting below.
         for (int y = faceMinY; y <= faceMaxY; y++) {
            currentPointMesh.y = y * gridCellSize.y + min.y;
            for (int x = faceMinX; x <= faceMaxX; x++) {
               currentPointMesh.x = x * gridCellSize.x + min.x;
               
               // Need to test if the point defined by x,y is inside the
               // 2d triangle formed by 'face' projected onto the x,y plane.
               
               
               Vertex3d v0 = face.he0.head;
               Point3d v0p = v0.getWorldPoint();
               Vertex3d v1 = face.he0.next.head;
               Point3d v1p = v1.getWorldPoint();
               Vertex3d v2 = face.he0.next.next.head;
               Point3d v2p = v2.getWorldPoint();
               
               int inTri = isPointInTriangle2D(currentPointMesh.x, currentPointMesh.y, 
                  v0.getIndex(), v0p.x, v0p.y, 
                  v1.getIndex(), v1p.x, v1p.y,
                  v2.getIndex(), v2p.x, v2p.y,
                  bary);
               
               if (inTri == 0) {
                  // XXX Robust predicates if required
                  if (!sdPredsWarning) {
                     logger.debug("SD using RobustPreds");
                     sdPredsWarning = true;
                  }
                  // could be on edge or vertex, use RobustPreds
                  bottom.x = currentPointMesh.x;
                  bottom.y = currentPointMesh.y;
                  bottom.z = min.z-1;
                  top.x = currentPointMesh.x;
                  top.y = currentPointMesh.y;
                  top.z = max.z+1;
                  if (RobustPreds.intersectSegmentTriangle(bottom, top, face, bary)) {
                     inTri = 1;
                  }
                  currentPointMesh.z = bary.z;
                  
               } else if (inTri == 1) {
                  // Point in triangle written as a weighted sum of the
                  // vertices.
                  currentPointMesh.z = bary.x * face.he0.head.pnt.z +
                     bary.y * face.he0.next.head.pnt.z +
                     bary.z * face.he0.next.next.head.pnt.z;
               }
               
               if (inTri > 0) {
                  // We should now use the z value in grid coordinates.
                  // Extract it from currentPointMesh
                  double currentPointZ = (currentPointMesh.z - min.z) / gridCellSize.z;
                  int zInterval = (int)currentPointZ + 1;
                  // intersection counted in next grid square
                  if (zInterval < 0) {
                     ++zIntersectionCount [x + gridSize[0]*y + gridSize[0]*gridSize[1]*0];
                  } else if (zInterval < gridSize[2]) {
                     ++zIntersectionCount[
                                         x + gridSize[0]*y + gridSize[0]*gridSize[1]*zInterval];
                  }
               } // point in triangle
            } // x
         } // y
      }

      // Done all triangles.
      // Sweep, propagating values throughout the grid volume.
      for (int pass = 0; pass < 2; pass++) {
         sweep(+1, +1, +1, pc, p1);
         sweep(-1, -1, -1, pc, p1);
         sweep(+1, +1, -1, pc, p1);
         sweep(-1, -1, +1, pc, p1);
         sweep(+1, -1, +1, pc, p1);
         sweep(-1, +1, -1, pc, p1);
         sweep(+1, -1, -1, pc, p1);
         sweep(-1, +1, +1, pc, p1);
      }
      
      // This is a ray-casting implementation to find the sign of each vertex in
      // the grid.
      for (int x = 0; x < gridSize[0]; x++) {
         for (int y = 0; y < gridSize[1]; y++) {
            int total_count = 0;
            //Count the intersections of the x axis
            for (int z = 0; z < gridSize[2]; z++) {
               int index =  x + gridSize[0]*y + gridSize[0]*gridSize[1]*z;
               total_count += zIntersectionCount [index];

               // If parity of intersections so far is odd, we are inside the 
               // mesh.
               if (total_count % 2 == 1) {
                  phi [index] =- phi [index];
               }
            }
         }
      }
      logger.println ("done.");
   }

   /** 
    * Calculates the distance at a point on the grid in mesh coordinates.
    *
    * @param point The point on the grid, in the same units as the corresponding
    * mesh.
    * @return The normal to the mesh surface at the given point.
    */
   public double interpolatePhi (Vector3d point) {

      double pointOnGridX = ((point.x - min.x) / gridCellSize.x);
      double pointOnGridY = ((point.y - min.y) / gridCellSize.y);
      double pointOnGridZ = ((point.z - min.z) / gridCellSize.z);

      int minx = (int)pointOnGridX;
      int miny = (int)pointOnGridY;
      int minz = (int)pointOnGridZ;

      double dx = pointOnGridX - minx;
      double dy = pointOnGridY - miny;
      double dz = pointOnGridZ - minz;

      double i1 = 
      getPhiAtPoint (minx    , miny    , minz)     * (1 - dz) +
      getPhiAtPoint (minx    , miny    , minz + 1) * dz;
      double i2 = 
      getPhiAtPoint (minx    , miny + 1, minz)     * (1 - dz) + 
      getPhiAtPoint (minx    , miny + 1, minz + 1) * dz;
      double j1 = 
      getPhiAtPoint (minx + 1, miny    , minz)     * (1 - dz) + 
      getPhiAtPoint (minx + 1, miny    , minz + 1) * dz;
      double j2 = 
      getPhiAtPoint (minx + 1, miny + 1, minz)     * (1 - dz) + 
      getPhiAtPoint (minx + 1, miny + 1, minz + 1) * dz;

      double w1 = i1 * (1 - dy) + i2 * dy;
      double w2 = j1 * (1 - dy) + j2 * dy;

      double interpolatedPhi = (w1 * (1 - dx) + w2 * dx);

      return interpolatedPhi;
   }


   /** 
    * Calculates the normal at a vertex on the grid.
    *
    * @param x x coordinate on grid.
    * @param y y coordinate on grid.
    * @param z z coordinate on grid.
    * @return normal to the mesh surface at this point.
    */
   private Vector3d calcNormal (int x, int y, int z) {

      Vector3d nrm = new Vector3d();
      //********************************************************************
      if (x == gridSize[0] - 1) {
         nrm.x = getPhiAtPoint (x-1, y, z) -
         getPhiAtPoint (x, y, z);
         nrm.x /= gridCellSize.x;
      }
      else if (x == 0) {
         nrm.x = getPhiAtPoint (x, y, z) -
         getPhiAtPoint (x+1, y, z);
         nrm.x /= gridCellSize.x;
      }
      else {
         nrm.x = getPhiAtPoint (x-1, y, z) -
         getPhiAtPoint (x+1, y, z);
         nrm.x /= (gridCellSize.x * 2);
      }
      //********************************************************************
      if (y == gridSize[1] - 1) {
         nrm.y = getPhiAtPoint (x, y-1, z) -
         getPhiAtPoint (x, y, z) ;
         nrm.y /= gridCellSize.y;
      }
      else if (y == 0) {
         nrm.y = getPhiAtPoint (x, y, z) -
         getPhiAtPoint (x, y+1, z);
         nrm.y /= gridCellSize.y;
      }
      else {
         nrm.y = getPhiAtPoint (x, y-1, z) -
         getPhiAtPoint (x, y+1, z);
         nrm.y /= (gridCellSize.y * 2);
      }
      //********************************************************************
      if (z == gridSize[2] - 1) {
         nrm.z = getPhiAtPoint (x, y, z-1) -
         getPhiAtPoint (x, y, z);
         nrm.z /= gridCellSize.z;
      }
      else if (z == 0) {
         nrm.z = getPhiAtPoint (x, y, z) -
         getPhiAtPoint (x, y, z+1);
         nrm.z /= gridCellSize.z;
      }
      else {
         nrm.z = getPhiAtPoint (x, y, z-1) -
         getPhiAtPoint (x, y, z+1);
         nrm.z /= (gridCellSize.z * 2);
      }
      //********************************************************************
      nrm.normalize ();
      normals[x + y * gridSize[0] + z * gridSize[0] * gridSize[1]] = nrm;
      return nrm;
   }

   /** 
    * Calculates the normal at a point in the grid in mesh coordinates.
    *
    * @param x x coordinate of point in mesh coordinates.
    * @param y y coordinate of point in mesh coordinates.
    * @param z z coordinate of point in mesh coordinates.
    * @return Interpolated normal to the mesh surface, in world coordinates,
    * as well as the distance to the mesh surface.
    */
   public double getDistanceAndNormal (
      Vector3d norm, double x, double y, double z) {
      Vector3d point = new Vector3d (x, y, z);
      return getDistanceAndNormal (norm, point);
   }  

   /** 
    * Calculates the normal at a point in the grid in mesh coordinates.
    *
    * @param norm Used to return the normal.
    * @param point The point at which to calculate the normal and distance
    * in mesh coordinates.
    * @return Interpolated normal to the mesh surface, in world coordinates,
    * as well as the distance to the mesh surface.
    */
   public double getDistanceAndNormal (Vector3d norm, Vector3d point) {
      // Change to grid coordinates
      double tempPointX = (point.x - min.x) / gridCellSize.x;
      double tempPointY = (point.y - min.y) / gridCellSize.y;
      double tempPointZ = (point.z - min.z) / gridCellSize.z;
      int minx = (int)tempPointX;
      int miny = (int)tempPointY;
      int minz = (int)tempPointZ;

      if (tempPointX < 0 || tempPointX > gridSize[0] - 1 ||
      tempPointY < 0 || tempPointY > gridSize[1] - 1 ||
      tempPointZ < 0 || tempPointZ > gridSize[2] - 1) {
         return OUTSIDE;
      }
      double dx = tempPointX - minx;
      double dy = tempPointY - miny;
      double dz = tempPointZ - minz;
      if (tempPointX == gridSize[0] - 1) {
         minx -= 1;
         dx = 1;
      }
      if (tempPointY == gridSize[1] - 1) {
         miny -= 1;
         dy = 1;
      }
      if (tempPointZ == gridSize[2] - 1) {
         minz -= 1;
         dz = 1;
      }

      // Now use trilinear interpolation to get the normal at 'point'.
      double w000 = (1-dx)*(1-dy)*(1-dz);
      double w001 = (1-dx)*(1-dy)*dz;
      double w010 = (1-dx)*dy*(1-dz);
      double w011 = (1-dx)*dy*dz;
      double w100 = dx*(1-dy)*(1-dz);
      double w101 = dx*(1-dy)*dz;
      double w110 = dx*dy*(1-dz);
      double w111 = dx*dy*dz;

      if (norm != null) {

         Vector3d nrm000 = getNormal (minx  , miny  , minz  );
         Vector3d nrm001 = getNormal (minx  , miny  , minz+1);
         Vector3d nrm010 = getNormal (minx  , miny+1, minz  );
         Vector3d nrm011 = getNormal (minx  , miny+1, minz+1);
         Vector3d nrm100 = getNormal (minx+1, miny  , minz  );
         Vector3d nrm101 = getNormal (minx+1, miny  , minz+1);
         Vector3d nrm110 = getNormal (minx+1, miny+1, minz  );
         Vector3d nrm111 = getNormal (minx+1, miny+1, minz+1);

         norm.setZero();
         norm.scaledAdd (w000, nrm000);
         norm.scaledAdd (w001, nrm001);
         norm.scaledAdd (w010, nrm010);
         norm.scaledAdd (w011, nrm011);
         norm.scaledAdd (w100, nrm100);
         norm.scaledAdd (w101, nrm101);
         norm.scaledAdd (w110, nrm110);
         norm.scaledAdd (w111, nrm111);
         norm.scale(-1.0);
      }

      return w000*getPhiAtPoint (minx  , miny  , minz  ) +
      w001*getPhiAtPoint (minx  , miny  , minz+1) +
      w010*getPhiAtPoint (minx  , miny+1, minz  ) +
      w011*getPhiAtPoint (minx  , miny+1, minz+1) +
      w100*getPhiAtPoint (minx+1, miny  , minz  ) +
      w101*getPhiAtPoint (minx+1, miny  , minz+1) +
      w110*getPhiAtPoint (minx+1, miny+1, minz  ) +
      w111*getPhiAtPoint (minx+1, miny+1, minz+1);
   }

   public int[] getGridSize() {
      return gridSize;
   }
   public Vector3d getGridCellSize() {
      return gridCellSize;
   }
   public Vector3d getMin() {
      return min;
   }
   public Vector3d getMax() {
      return max;
   }

   public SignedDistanceGridCell getGridCell (int idx) {
      if (gridCellArray == null) {
         gridCellArray = buildGridCellArray();
      }
      return gridCellArray [idx];
   }

   public Vector3d getNormal (int x, int y, int z) {
      if (normals[ (x + 
      y * gridSize[0] + 
      z * gridSize[0] * gridSize[1])] == null) {
         return calcNormal (x, y, z);
      }
      else {
         return normals[(
         x + 
         y * gridSize[0] + 
         z * gridSize[0] * gridSize[1])];
      }
   }

   public double[] getPhi() {
      return phi;
   }

   public double getPhiAtPoint (int x, int y, int z){
      return phi[(
      x + 
      y * gridSize[0] + 
      z * gridSize[0] * gridSize[1])];
   }

   public double getPhiAtPoint (int[] point) {
      // The point we're given is in grid coordinates
      return phi[(
      point[0] + 
      point[1] * gridSize[0] + 
      point[2] * gridSize[0] * gridSize[1])];
   }

   //   /**
   //    * Matches a point in mesh coordinates to a point on the grid
   //    * 
   //    * @param meshPoint The Vector3d object in mesh coordinates to be converted to
   //    * grid coordinates. 
   //    * @param roundUp Indicates that the values should be increased after
   //    * casting them to integers.
   //    * @return An integer array
   //    */
   //   private int[] matchToGrid (Vector3d meshPoint, boolean roundUp) {
   //      // Given a Vector3d(), change it to units of gridCellSize, and
   //      // base it off of min.
   //      int matchedPoint[] = new int[3];
   //      Vector3d gridPoint = new Vector3d(meshPoint);
   //      gridPoint.x = (meshPoint.x - min.x) / gridCellSize.x;
   //      gridPoint.y = (meshPoint.y - min.y) / gridCellSize.y;
   //      gridPoint.z = (meshPoint.z - min.z) / gridCellSize.z;
   //      
   //      if (roundUp) { // 
   //         matchedPoint[0] = (int)gridPoint.x + 1;
   //         matchedPoint[1] = (int)gridPoint.y + 1;
   //         matchedPoint[2] = (int)gridPoint.z + 1;
   //         // Bounds checking
   //         if (matchedPoint[0] > (gridSize[0] - 1)){
   //            matchedPoint[0] = gridSize[0] - 1;
   //         }
   //         if (matchedPoint[1] > (gridSize[1] - 1)){
   //            matchedPoint[1] = gridSize[1] - 1;
   //         }
   //         if (matchedPoint[2] > (gridSize[2] - 1)){
   //            matchedPoint[2] = gridSize[2] - 1;
   //         }
   //      }
   //      else {
   //         matchedPoint[0] = (int)gridPoint.x;
   //         matchedPoint[1] = (int)gridPoint.y;
   //         matchedPoint[2] = (int)gridPoint.z;
   //         // Bounds checking
   //         for (int i = 0; i < 3; i++) {
   //            if (matchedPoint[i] < 0){
   //               matchedPoint[i] = 0;
   //            }
   //         }
   //      }
   //      return matchedPoint;
   //   }   

   //   private int orientationXY (
   //      double p1x, double p1y, double p1z, double p2x, double p2y, double p2z,
   //      double twiceSignedArea[], int xyz) {
   //      
   //      twiceSignedArea[xyz] = p1y * p2x - p1x * p2y;
   //      if (twiceSignedArea[xyz] > 0) {
   //         return 1;
   //      }
   //      else if (twiceSignedArea[xyz] < 0) {
   //         return -1;   
   //      }
   //      // (twice signed area == 0).
   //      else if (p2y > p1y) return  1;
   //      else if (p2y < p1y) return -1;
   //      else if (p1x > p2x) return  1;
   //      else if (p1x < p2x) return -1;
   //      else return 0; // only true when x1==x2 and y1==y2
   //   } 

//   /**
//    * This method has two purposes: First, to detect if a point lies
//    * inside a 2-dimensional triangle. Second, to return the barycentric
//    * coordinates of the triangle in question.
//    * 
//    * @param point Point in consideration, only x and y values considered.
//    * @param face Face in consideration, only x and y values considered.
//    * @param signedArea Used first to determine if the point is inside the 
//    * triangle, and then to return the barycentric coordinates of the triangle.
//    * @return 1 if inside, -1 if outside, 0 if near-enough to the boundary
//    *         that we are not quite sure
//    */
//   private int isPointInXYTriangle (
//      Point3d point, Face face, Point3d signedArea ) {
//      // Shift the triangle to the point.
//      double shifted0x = face.he0.head.pnt.x - point.x;
//      double shifted0y = face.he0.head.pnt.y - point.y;
//      double shifted1x = face.he0.next.head.pnt.x - point.x;
//      double shifted1y = face.he0.next.head.pnt.y - point.y;
//      double shifted2x = face.he0.next.next.head.pnt.x - point.x;
//      double shifted2y = face.he0.next.next.head.pnt.y - point.y;
//      // Calculate the twice signed area.
//      signedArea.x = shifted1y * shifted2x - shifted1x * shifted2y;
//      int signa;
//      if (signedArea.x > 0) {
//         signa = 1;
//      }
//      else if (signedArea.x < 0) {
//         signa = -1;   
//      }
//      else if (shifted2y > shifted1y) signa =  1;
//      else if (shifted2y < shifted1y) signa = -1;
//      else if (shifted1x > shifted2x) signa =  1;
//      else if (shifted1x < shifted2x) signa = -1;
//      else return -1; // signa == 0
//      
//      //************************************************************************
//      int signb;
//      signedArea.y = shifted2y * shifted0x - shifted2x * shifted0y;
//      if (signedArea.y > 0) {
//         signb = 1;
//      }
//      else if (signedArea.y < 0) {
//         signb = -1;   
//      }
//      else if (shifted0y > shifted2y) signb =  1;
//      else if (shifted0y < shifted2y) signb = -1;
//      else if (shifted2x > shifted0x) signb =  1;
//      else if (shifted2x < shifted0x) signb = -1;
//      else signb = 0;
//      if (signb != signa) {
//         return -1;
//      }
//      //************************************************************************
//      int signc;
//      signedArea.z = shifted0y * shifted1x - shifted0x * shifted1y;
//      if (signedArea.z > 0) {
//         signc = 1;
//      }
//      else if (signedArea.z < 0) {
//         signc = -1;   
//      }
//      else if (shifted1y > shifted0y) signc =  1;
//      else if (shifted1y < shifted0y) signc = -1;
//      else if (shifted0x > shifted1x) signc =  1;
//      else if (shifted0x < shifted1x) signc = -1;
//      else signc = 0;
//      if (signc != signa) {
//         return -1;
//      }
//      //************************************************************************
//      double sum = signedArea.x + signedArea.y + signedArea.z;
//      // need to assert that sum != 0.
//      signedArea.x /= sum;
//      signedArea.y /= sum;
//      signedArea.z /= sum;
//      
//      return 1;
//   }
   
   /**
    * Integer signum
    * @param a
    * @param eps
    * @return
    */
   private static int signum(double a) {
      if (a < -0) {
         return -1;
      }
      if (a > 0) {
         return 1;
      }
      return 0;
   }
   
   /**
    * This method has two purposes: First, to detect if a point lies
    * inside a 2-dimensional triangle. Second, to return the barycentric
    * coordinates of the triangle in question.
    * 
    * @param point Point in consideration, only x and y values considered.
    * @param face Face in consideration, only x and y values considered.
    * @return 1 if inside, -1 if outside, 0 if near-enough to the boundary
    *         that we are not quite sure
    */
   private static int isPointInTriangle2D (
      double px, double py, 
      int t0i, double t0x, double t0y, 
      int t1i, double t1x, double t1y,
      int t2i, double t2x, double t2y,
      Vector3d bary) {
     
      
      // sort triangle points
      double dx0, dx1, dx2, dy0, dy1, dy2; // ordered points centered at (px, py)
      // flip to apply to predicates
      boolean flip01 = false;
      boolean flip12 = false;
      boolean flip02 = false; 
      if (t0i <= t1i) {
         if (t1i <= t2i) {
            // t0, t1, t2
            dx0 = t0x-px;
            dy0 = t0y-py;
            dx1 = t1x-px;
            dy1 = t1y-py;
            dx2 = t2x-px;
            dy2 = t2y-py;
            flip02 = true;
         } else if (t0i <= t2i) {
            // t0, t2, t1
            dx0 = t0x-px;
            dy0 = t0y-py;
            dx1 = t2x-px;
            dy1 = t2y-py;
            dx2 = t1x-px;
            dy2 = t1y-py;
            flip01 = true;
            flip12 = true;
         } else {
            // t2, t0, t1
            dx0 = t2x-px;
            dy0 = t2y-py;
            dx1 = t0x-px;
            dy1 = t0y-py;
            dx2 = t1x-px;
            dy2 = t1y-py;
            flip02 = true;
         }
      } else if (t2i < t1i) {
         // t2, t1, t0
         dx0 = t2x-px;
         dy0 = t2y-py;
         dx1 = t1x-px;
         dy1 = t1y-py;
         dx2 = t0x-px;
         dy2 = t0y-py;
         flip01 = true;
         flip12 = true;
      } else if (t2i < t0i) {
         // t1, t2, t0
         dx0 = t1x-px;
         dy0 = t1y-py;
         dx1 = t2x-px;
         dy1 = t2y-py;
         dx2 = t0x-px;
         dy2 = t0y-py;
         flip02 = true;
      } else {
         // t1, t0, t2
         dx0 = t1x-px;
         dy0 = t1y-py;
         dx1 = t0x-px;
         dy1 = t0y-py;
         dx2 = t2x-px;
         dy2 = t2y-py;
         flip01 = true;
         flip12 = true;
      }

      // squared machine precision
      final double EPS2 = 2e-31; // approx (2^{-51})^2
      
      // always compare using lower index first for consistency
      double dx0y1 = dx0*dy1;
      double dy0x1 = dy0*dx1; 
      double da01 = (dx0y1-dy0x1);
      if (flip01) {
         da01 = -da01;
      }
      // error analysis ~ sqrt(2*(dx0y1*dx0y1+ dy0x1*dy0x1))
      double eps01squared = 4*((dx0y1*dx0y1 + dy0x1*dy0x1)*EPS2); // double just-in-case
      int s01 = 0;
      if (da01*da01 > eps01squared) {
         s01 = signum(da01);
      }
      
      double dx1y2 = dx1*dy2;
      double dy1x2 = dy1*dx2; 
      double da12 = (dx1y2-dy1x2);
      if (flip12) {
         da12 = -da12;
      }
      // error analysis ~ sqrt(2*(dx1y2*dx1y2+ dy1x2*dy1x2))
      double eps12squared = 4*((dx1y2*dx1y2 + dy1x2*dy1x2)*EPS2); // double just-in-case
      int s12 = 0;
      if (da12*da12 > eps12squared) {
         s12 = signum(da12);
      }
      
      // non-zero and opposite sides, outside triangle
      if (s01 != 0 && s12 != 0 && s01 != s12) {
         return -1;
      }
      
      double dx0y2 = dx0*dy2;
      double dy0x2 = dy0*dx2; 
      double da02 = (dx0y2-dy0x2);
      if (flip02) {
         da02 = -da02;
      }
      // error analysis ~ sqrt(2*(dx0y2*dx0y2+ dy0x2*dy0x2))
      double eps02squared = 4*((dx0y2*dx0y2 + dy0x2*dy0x2)*EPS2); // double just-in-case
      int s02 = 0;
      if (da02*da02 > eps02squared) {
         s02 = signum(da02);
      }
      
      // if any two non-zero signs differ, outside triangle
      if (s02 != 0) {
         if (s01 != 0 && s01 != s02) {
            return -1;
         } else if (s12 != 0 && s12 != s02) {
            return -1;
         } 
      }
      
      // barycentric coordinates
      double sum = da01+da12+da02;
      bary.x = da12/sum;
      bary.y = da02/sum;
      bary.z = da01/sum;
      
      // if all three same sign and not zero, true
      if (s01 == s02 && s01 == s12 && s01 != 0) {
         return 1;
      }
      
      // unsure, seems to be exactly on an edge or vertex
      return 0;
   }
   
   /**
    * Utility method for the {@link #sweep sweep} method. Compares a given 
    * vertex with one of its neighbours.
    * 
    * @param x x value of point in consideration
    * @param y y value of point in consideration
    * @param z z value of point in consideration
    * @param dx x value of the neighbouring vertex
    * @param dy y value of the neighbouring vertex
    * @param dz z value of the neighbouring vertex
    */
   private void checkNeighbouringVertex (
      int x, int y, int z, int dx, int dy, int dz, 
      Point3d closestPointOnFace, Point3d p1) {
      
      // dx, dy, dz represent a point +- 1 grid cell away from out current
      // point. There are 26 neighbours.
      int neighbourIndex = 
      dx + 
      dy * gridSize[0] + 
      dz * gridSize[0] * gridSize[1];
      if (closestFace[neighbourIndex] >= 0 ) {
         // Everything is in mesh coordinates.
         getMeshCoordinatesFromGrid (p1, (double)x, (double)y, (double)z);
         Face neighbourFace = faces [closestFace [neighbourIndex]];
         neighbourFace.nearestPoint (closestPointOnFace, p1);
         double distanceToNeighbourFace = p1.distance (closestPointOnFace);
         int testPointIndex = x + 
         y * gridSize[0] + 
         z * gridSize[0] * gridSize[1];
         if (distanceToNeighbourFace < phi[testPointIndex]) {
            phi [testPointIndex] = distanceToNeighbourFace;
            // gridCellArray [testPointIndex].setDistance (distanceToNeighbourFace);
            closestFace [testPointIndex] = closestFace [neighbourIndex];
         }
      }
   }

   /**
    * Sweeps across the entire grid, propagating distance values.
    * 
    * @param dx x direction of sweep
    * @param dy y direction of sweep
    * @param dz z direction of sweep
    * @param pc temporary variable
    */
   private void sweep (int dx, int dy, int dz, Point3d pc, Point3d p1) {
      int x0, x1;
      if (dx > 0) {
         x0 = 1;
         x1 = gridSize[0];
      }
      else {
         x0 = gridSize[0]-2;  // sweeps backwards
         x1 = -1;
      }
      int y0, y1;
      if (dy > 0) { 
         y0 = 1;
         y1 = gridSize[1];
      }
      else { 
         y0 = gridSize[1]-2;
         y1 = -1;
      }
      int z0, z1;
      if (dz > 0) {
         z0 = 1;
         z1 = gridSize[2];
      }
      else {
         z0 = gridSize[2]-2;
         z1 = -1;
      }
      for (int z = z0; z != z1; z += dz) {
         for(int y = y0; y != y1; y += dy) {
            for (int x = x0; x != x1; x += dx) {
               // What are the neighbours? Depending on dx,dy,dz...
               checkNeighbouringVertex (x, y, z, x-dx, y,    z   , pc, p1);
               checkNeighbouringVertex (x, y, z, x,    y-dy, z   , pc, p1);
               checkNeighbouringVertex (x, y, z, x-dx, y-dy, z   , pc, p1);
               checkNeighbouringVertex (x, y, z, x,    y,    z-dz, pc, p1);
               checkNeighbouringVertex (x, y, z, x-dx, y,    z-dz, pc, p1);
               checkNeighbouringVertex (x, y, z, x,    y-dy, z-dz, pc, p1);
               checkNeighbouringVertex (x, y, z, x-dx, y-dy, z-dz, pc, p1);
            }
         }
      }
   }

   public void prerender (RenderList list) {
   }

   public void render (Renderer renderer, int flags) {

      Shading savedShading = renderer.getShading();
      renderer.setShading (Shading.NONE);
      renderer.pushModelMatrix();
      if (mesh != null) {
         renderer.mulModelMatrix (mesh.XMeshToWorld);
      }
      renderer.popModelMatrix();
      renderer.setColor (0, 0, 1);

      Vector3d maxGrid = new Vector3d();
      maxGrid.x = min.x + (gridSize[0]-1) * gridCellSize.x;
      maxGrid.y = min.y + (gridSize[1]-1) * gridCellSize.y; 
      maxGrid.z = min.z + (gridSize[2]-1) * gridCellSize.z;

      renderer.beginDraw (DrawMode.LINES); // Draw 4 vertical lines.
      renderer.addVertex (maxGrid.x, maxGrid.y, maxGrid.z);
      renderer.addVertex (maxGrid.x, maxGrid.y, min.z);
      renderer.addVertex (min.x, maxGrid.y, maxGrid.z);
      renderer.addVertex (min.x, maxGrid.y, min.z);
      renderer.addVertex (min.x, min.y, maxGrid.z);
      renderer.addVertex (min.x, min.y, min.z);
      renderer.addVertex (maxGrid.x, min.y, maxGrid.z);
      renderer.addVertex (maxGrid.x, min.y, min.z);
      // Draw a diagonal line from max to min.
      renderer.addVertex (min.x, min.y, min.z);
      renderer.addVertex (maxGrid.x, maxGrid.y, maxGrid.z);
      renderer.endDraw();
      
      // getGridCells();

      // Draw the vertices on the grid.
      double myVertex[] = new double[3];
      for (int i = 0; i < phi.length; i++) {
         int z = (i / (gridSize[0] * gridSize[1]));
         int y = (i - z * (gridSize[0] * gridSize[1])) / (gridSize[0]);
         int x = (i % (gridSize[0]));
         getMeshCoordinatesFromGrid (x, y, z, myVertex);   
         // gridCellArray[i].render (renderer, flags);
         if (phi[i] <= 0) {
            renderer.setColor(Color.BLUE);
            renderer.drawPoint(myVertex[0], myVertex[1], myVertex[2]);
         } else {
            renderer.setColor(Color.RED);
            renderer.drawPoint(myVertex[0], myVertex[1], myVertex[2]);
         }
         
      }
      renderer.setShading (savedShading);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   public int getRenderHints() {
      return 0;
   }

   public Face[] getFaces() {
      return faces;
   }

   public int[] getClosestFace() {
      return closestFace;
   }

   /** 
    * Returns the index in {@link #faces faces} corresponding to the closest face
    * at this point in {@link #phi phi}.
    *
    * @param idx The index of phi that corresponds to the point in question.
    */
   public int getClosestFace(int idx) {
      return closestFace[idx];
   }

   public double[] getMeshCoordinatesFromGrid (int x, int y, int z, double[] meshCoords) {
      meshCoords[0] = x * gridCellSize.x + min.x;
      meshCoords[1] = y * gridCellSize.y + min.y;
      meshCoords[2] = z * gridCellSize.z + min.z;
      return meshCoords;
   }

   public void getMeshCoordinatesFromGrid (
      Point3d meshCoords, double x, double y, double z) {
      meshCoords.x = x * gridCellSize.x + min.x;
      meshCoords.y = y * gridCellSize.y + min.y;
      meshCoords.z = z * gridCellSize.z + min.z;
   }

   public Vector3d getMeshMin () {
      Vector3d meshMin = new Vector3d();
      mesh.getLocalBounds (meshMin, null);
      return meshMin;
   }


   public Vector3d getMeshMax () {
      Vector3d meshMax = new Vector3d();
      mesh.getLocalBounds (null, meshMax);
      return meshMax;
   }


}
