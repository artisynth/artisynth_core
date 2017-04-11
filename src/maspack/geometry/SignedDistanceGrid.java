/**
 * Copyright (c) 2017, by the Authors: Bruce Haines (UBC), Antonio Sanchez
 * (UBC), John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.awt.Color;
import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.LineStyle;
import maspack.render.RenderObject;
import maspack.util.Logger;
import maspack.util.StringHolder;

/**
 * Implements a signed distance field for fixed triangular mesh. One common
 * use of such a field is to detect point penetration distances and normals for
 * use in collision handling.
 *
 * <p>The field is implemented using a regular 3D grid composed of
 * <code>numVX</code> X <code>numVY</code> X <code>numVZ</code> vertices along
 * the x, y and z directions, dividing the volume into <code>(numVX-1)</code> X
 * <code>(numVY-1)</code> X <code>(numVZ-1)</code> cells. For vertices close to
 * the mesh, nearby faces are examined to determine the distance from the mesh
 * to the vertex. A sweep method and ray-casting are then used to propogate
 * distance values throughout the volume and determine whether vertices are
 * inside or outside. The algorithm is based on C++ code provided by Robert
 * Bridson at UBC.
 *
 * <p>The grid is constructed in local mesh coordinates, and all queries are
 * assumed to be performed in local mesh coordinates. The mesh is assumed to be
 * closed; if it is not, the results are undefined. The distance field is
 * defined so as to be negative within the mesh and positive outside
 * it. Normals are computed at each vertex using numeric differentation of the
 * distances associated with the surrounding vertices. Trilinear interpolation
 * of vertex values across each cell is used to compute the distance and normal
 * for a general point within the grid volume.
 */
public class SignedDistanceGrid implements Renderable {

   private PolygonalMesh myMesh;      // mesh associated with the grid
   private Vector3d myCellWidths;     // cell widths along x, y, z
   private double[] myPhi;            // distance values at each vertex
   private Vector3d[] myNormals;      // normal values at each vertex
   public static double OUTSIDE = Double.MAX_VALUE;

   // colors, colorMap and color indices which can be used to assign colors to
   // the different vertices for rendering purposes
   protected LinkedHashMap<Color,Integer> myColorMap;
   protected ArrayList<Color> myColors;
   protected int[] myColorIndices;
   protected RenderProps myRenderProps = null;
   protected int[] myRenderRanges = null;
   //protected String myRenderProps = null;

   protected RenderObject myRob;      // render object used for rendering
   protected boolean myRobValid = false;

   /**
    * Maximum of the signed distance field, in mesh coordinates.
    */
   private Point3d myMaxCoord = new Point3d();
   /**
    * Minimum of the signed distance field, in mesh coordinates.
    */
   private Point3d myMinCoord = new Point3d();

   private int numVX = 0;  // number of vertices along X
   private int numVY = 0;  // number of vertices along Y
   private int numVZ = 0;  // number of vertices along Z
   private int numVXxVY = 0; // numVX*numVY

   /**
    * An array giving the index of the nearest face to each vertex.
    */
   private int[] myClosestFaceIdxs;

   SignedDistanceGrid () {
      myRenderProps = createRenderProps();
   }

   /**
    * Creates a new signed distance grid for a specified mesh. The mesh is
    * created as for {@link #SignedDistanceGrid(PolygonalMesh,double,Vector3i)},
    * with the number of cells along each axis given by 25.
    *
    * @param mesh mesh for which the grid should be created
    * @param marginFraction multiplied by the width in each direction to
    * determine the margin for that direction
    */
   public SignedDistanceGrid (PolygonalMesh mesh, double marginFraction) {
      this (mesh, marginFraction, new Vector3i (25, 25, 25));
   }

   private void setMeshAndBounds (
      Vector3d widths, Vector3d margin,
      PolygonalMesh mesh, double marginFraction) {
      
      if (!mesh.isTriangular()) {
         throw new IllegalArgumentException ("mesh is not triangular");
      }
      myMesh = mesh;
      myMesh.getLocalBounds (myMinCoord, myMaxCoord);      
      widths.sub (myMaxCoord, myMinCoord);
      margin.scale(marginFraction, widths);
      widths.scaledAdd(2, margin);     
   }

   /**
    * Creates a new signed distance grid for a specified mesh. The grid is
    * aligned with the x, y, z axes, is centered on the mesh, and has cells of
    * uniform size. Its widths are first determined using the mesh bounds and
    * <code>marginFraction</code> as described for {@link
    * #SignedDistanceGrid(PolygonalMesh,double,Vector3i)}. The axis with maximum
    * width is then divided into <code>maxResolution</code> cells, while the
    * other axes are divided into a number of cells {@code <= maxResolution}
    * with the same cell width, with the overall axis widths grown as necessary
    * to accommodate this.
    * 
    * @param mesh mesh for which the grid should be created
    * @param marginFraction multiplied by the width in each direction
    * to determine the (initial) margin for that direction
    * @param maxResolution number of grid cells along the axis of maximum
    * width
    */
   public SignedDistanceGrid (
      PolygonalMesh mesh, double marginFraction, int maxResolution) {
      this();

      Vector3d widths = new Vector3d();
      Vector3d margin = new Vector3d();
      setMeshAndBounds (widths, margin, mesh, marginFraction);
      
      double cwidth = widths.maxElement()/maxResolution;
      numVX = (int)(Math.ceil(widths.x/cwidth))+1; 
      numVY = (int)(Math.ceil(widths.y/cwidth))+1;
      numVZ = (int)(Math.ceil(widths.z/cwidth))+1;      
      numVXxVY = numVX*numVY;      
      myCellWidths = new Vector3d (cwidth, cwidth, cwidth);
      // margin increases to accommodate uniform cell width
      margin.x += ((numVX-1)*cwidth - widths.x)/2;
      margin.y += ((numVY-1)*cwidth - widths.y)/2;
      margin.z += ((numVZ-1)*cwidth - widths.z)/2;
      // adjust max and min coords
      myMaxCoord.add(margin);
      myMinCoord.sub(margin);
      
      calculatePhi (margin.norm());      
      clearColors();
      myRenderRanges = new int[] {0, numVX, 0, numVY, 0, numVZ};
   }
   
   /**
    * Creates a new signed distance grid for a specified mesh. The grid is
    * aligned with the x, y, z axes and centered on the mesh. Its width along
    * each axis is computed from the mesh's maximum and minimum bounds, and
    * then enlarged by a margin computed by multiplying the width by
    * <code>marginFraction</code>. The width along each axis is therefore
    * <pre>
    *   width = (1 + 2*marginFraction)*boundsWidth
    * </pre>
    * where <code>boundsWidth</code> is the width determined from the mesh
    * bounds.
    * 
    * @param mesh mesh for which the grid should be created
    * @param marginFraction multiplied by the width in each direction
    * to determine the margin for that direction
    * @param resolution number of grid cells that should be used along
    * each axis
    */
   public SignedDistanceGrid (
      PolygonalMesh mesh, double marginFraction, Vector3i resolution) {

      this();

      Vector3d widths = new Vector3d();
      Vector3d margin = new Vector3d();
      setMeshAndBounds (widths, margin, mesh, marginFraction);
      
      myCellWidths = new Vector3d ();
      myCellWidths.x = widths.x / resolution.x;
      myCellWidths.y = widths.y / resolution.y;
      myCellWidths.z = widths.z / resolution.z;

      numVX = resolution.x+1;
      numVY = resolution.y+1;
      numVZ = resolution.z+1;
      numVXxVY = numVX*numVY;      

      // adjust max and min coords
      myMaxCoord.add(margin);
      myMinCoord.sub(margin);
      
      calculatePhi (margin.norm());
      clearColors();
      myRenderRanges = new int[] {0, numVX, 0, numVY, 0, numVZ};
   }

   /**
    * Returns the mesh associated with this grid.
    * 
    * @return mesh associated with this grid
    */
   public PolygonalMesh getMesh() {
      return myMesh;
   }

   /**
    * Clears the colors used to render the vertices.
    */
   public void clearColors() {
      myColorMap = null;
      myRobValid = false;
   }

   private void initColorMap() {
      myColorMap = new LinkedHashMap<Color,Integer>();
      myColors = new ArrayList<Color>();
      myColors.add (Color.GREEN);
      for (int i=0; i<myColorIndices.length; i++) {
         myColorIndices[i] = 0;
      }
   }

   /**
    * Sets the color used to render a specified vertex.
    * 
    * @param idx vertex index
    * @param color rendering color
    */
   public void setVertexColor (int idx, Color color) {
      if (myColorMap == null) {
         initColorMap();
      }
      Integer cidx = myColorMap.get(color);
      if (cidx == null) {
         cidx = myColors.size();
         myColorMap.put (color, cidx);
         myColors.add (color);
      }
      myColorIndices[idx] = cidx;
      myRobValid = false;
   }
   
   /**
    * Returns the color used for rendering a specified vertex.
    * 
    * @param idx vertex index
    * @return rendering color
    */
   public Color getVertexColor (int idx) {
      if (myColorMap == null) {
         return null;
      }
      else {
         return myColors.get(myColorIndices[idx]);
      }
   }

   public void setDefaultVertexColor (Color color) {
      if (myColorMap == null) {
         initColorMap();
      }
      myColors.set (0, color);
      myRobValid = false;
   }

   public Color getDefaultVertexColor() {
      if (myColorMap == null) {
         return null;
      }
      else {
         return myColors.get(0);
      }
   }
   
   /** 
    * Calculates the signed distance field.
    */
   private void calculatePhi (double marginDist) {
      Logger logger = Logger.getSystemLogger();
      //logger.info ("Calculating Signed Distance Field...");

      double maxDist = myMaxCoord.distance (myMinCoord);
      int numV = numVX*numVY*numVZ;

      myPhi = new double [numV];
      myNormals = new Vector3d [numV];
      myColorIndices = new int [numV];

      for (int p = 0; p < myPhi.length; p++) {
         myPhi[p] = maxDist;
      }
      // The index of closestFace matches with phi.
      // Each entry in closestFace is the index of the closest 
      // face to the grid vertex.
      myClosestFaceIdxs = new int[myPhi.length];
      int zIntersectionCount[] = new int[myPhi.length];
      
      for (int i = 0; i < myPhi.length; i++) {
         myClosestFaceIdxs[i] = -1;
         zIntersectionCount[i] = 0;
      }
      Point3d faceMin          = new Point3d();
      Point3d faceMax          = new Point3d();
      Point3d closestPoint     = new Point3d();
      // Point3d currentPoint     = new Point3d();
      Point3d currentPointMesh = new Point3d();
      Point3d pc = new Point3d(); // A temp variable passed in for performance.
      Point3d p1 = new Point3d(); // A temp variable passed in for performance.
      Point3d bot = new Point3d();
      Point3d top = new Point3d();
      
      // For every triangle...
      for (int t = 0; t < myMesh.numFaces(); t++) {
         Face face = myMesh.getFace(t);
         faceMin.set (Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY);
         faceMax.set (Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY, 
            Double.NEGATIVE_INFINITY);
         face.updateBounds (faceMin, faceMax);
         // Converting mesh min/max to grid coordinates.
         int faceMinX = (int)((faceMin.x - myMinCoord.x) / myCellWidths.x);
         int faceMinY = (int)((faceMin.y - myMinCoord.y) / myCellWidths.y);
         int faceMinZ = (int)((faceMin.z - myMinCoord.z) / myCellWidths.z);
         if (faceMinX < 0) {
            faceMinX = 0;
         }
         if (faceMinY < 0) {
            faceMinY = 0;
         }
         if (faceMinZ < 0) {
            faceMinZ = 0;
         }
         int faceMaxX = (int)((faceMax.x - myMinCoord.x) / myCellWidths.x) + 1;
         int faceMaxY = (int)((faceMax.y - myMinCoord.y) / myCellWidths.y) + 1;
         int faceMaxZ = (int)((faceMax.z - myMinCoord.z) / myCellWidths.z) + 1;
         if (faceMaxX > numVX - 1) {
            faceMaxX = numVX - 1;
         }
         if (faceMaxY > numVY - 1) {
            faceMaxY = numVY - 1;
         }
         if (faceMaxZ > numVZ - 1) {
            faceMaxZ = numVZ - 1;
         }

         // Now go through the entire parallelpiped. Calculate distance and
         // closestFace.
         for (int z = faceMinZ; z <= faceMaxZ; z++) {
            for (int y = faceMinY; y <= faceMaxY; y++) {
               for (int x = faceMinX; x <= faceMaxX; x++) {
                  // Get mesh coordinates
                  getVertexCoords (currentPointMesh, x, y, z);
                  // Get the distance from this point to the face.
                  face.nearestPoint (closestPoint, currentPointMesh);
                  double distance = currentPointMesh.distance (closestPoint);
                  int index = xyzIndicesToVertex (x, y, z);
                  if (distance < myPhi[index]) {
                     myPhi[index] = distance;
                     myClosestFaceIdxs [index] = t;
                  }
               }
            }
         }
         
         // Ray-casts from bottom x-y plane, upwards, counting intersections.
         // We're building intersectionCount[] to use in ray casting below.
         for (int y = faceMinY; y <= faceMaxY; y++) {
            currentPointMesh.y = y * myCellWidths.y + myMinCoord.y;
            for (int x = faceMinX; x <= faceMaxX; x++) {
               currentPointMesh.x = x * myCellWidths.x + myMinCoord.x;

               bot.x = currentPointMesh.x;
               bot.y = currentPointMesh.y;
               bot.z = myMinCoord.z-1;
               top.x = currentPointMesh.x;
               top.y = currentPointMesh.y;
               top.z = myMaxCoord.z+1;

               Point3d ipnt = new Point3d();
               int res = RobustPreds.intersectSegmentTriangle (
                  ipnt, bot, top, face, maxDist, /*worldCoords=*/false);

               if (res > 0) {
                  currentPointMesh.z = ipnt.z;
                  // We should now use the z value in grid coordinates.
                  // Extract it from currentPointMesh
                  double currentPointZ =
                     (currentPointMesh.z - myMinCoord.z) / myCellWidths.z;
                  int zInterval = (int)currentPointZ + 1;
                  // intersection counted in next grid square
                  if (zInterval < 0) {
                     ++zIntersectionCount [xyzIndicesToVertex (x, y, 0)];
                  } else if (zInterval < numVZ) {
                     ++zIntersectionCount[xyzIndicesToVertex(x, y, zInterval)];
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
      for (int x = 0; x < numVX; x++) {
         for (int y = 0; y < numVY; y++) {
            int total_count = 0;
            //Count the intersections of the x axis
            for (int z = 0; z < numVZ; z++) {
               int index = xyzIndicesToVertex (x, y, z);
               total_count += zIntersectionCount [index];

               // If parity of intersections so far is odd, we are inside the 
               // mesh.
               if (total_count % 2 == 1) {
                  myPhi[index] =- myPhi[index];
               }
            }
         }
      }
      //logger.println ("done.");
   }


   /** 
    * Calculates the signed distance field, using bounding volume
    * hierarchy queries at each vertex. This method can be considerably
    * slower than the default approach and so is not currently used.
    */
   private void calculatePhiBVH (double marginDist) {
      Logger logger = Logger.getSystemLogger();
      logger.info ("Calculating Signed Distance Field...");

      // gridSize represents the maximum point in grid coordinates, rounded
      // up to the nearest full cell.
      // +1 for rounding up after casting to an integer.
      // +1 because it is a node count, not cell count
      // numVX = (int)(Math.ceil((myMaxCoord.x-myMinCoord.x) / myCellWidths.x))+1; 
      // numVY = (int)(Math.ceil((myMaxCoord.y-myMinCoord.y) / myCellWidths.y))+1;
      // numVZ = (int)(Math.ceil((myMaxCoord.z-myMinCoord.z) / myCellWidths.z))+1;
      // numVXxVY = numVX*numVY;
      
      int numV = numVX*numVY*numVZ;

      myPhi = new double [numV];
      myNormals = new Vector3d [numV];
      myColorIndices = new int [numV];

      // The index of closestFace matches with phi.
      // Each entry in closestFace is the index of the closest 
      // face to the grid vertex.
      myClosestFaceIdxs = new int[myPhi.length];

      BVFeatureQuery query = new BVFeatureQuery();
      // create our own bvtree for the mesh, since we can then dispose of it
      // after we are down building the SD field
      AABBTree bvtree = new AABBTree (myMesh, 2, marginDist);      
      System.out.println ("tree done");

      // Now go through the entire parallelpiped. Calculate distance and
      // closestFace.
      Vector3i vxyz = new Vector3i();
      Point3d near = new Point3d();
      Point3d coords = new Point3d();
      double tol = 1e-12*myMaxCoord.distance(myMinCoord);

      for (int idx=0; idx<myPhi.length; idx++) {
         vertexToXyzIndices (vxyz, idx);
         getVertexCoords (coords, vxyz);
         // Get the distance from this point to the face.
         boolean inside = query.isInsideOrientedMesh (bvtree, coords, tol);
         Face face = query.getFaceForInsideOrientedTest (near, null);
         
         Vector3d diff = new Vector3d();
         Vector3d normal = new Vector3d();
         diff.sub (coords, near);
         double dist = diff.norm();
         if (dist < tol) {
            face.computeNormal (normal);
            normal.negate();
            dist = normal.dot (diff);
         }
         else {
            normal.normalize(diff);
            if (inside) {
               dist -= dist;
            }
            else {
               normal.negate();
            }
         }
         myPhi[idx] = dist;
         myNormals[idx] = normal;
         myClosestFaceIdxs[idx] = face.getIndex();
      }
      logger.println ("done.");
   }

   /** 
    * Calculates the normal at a vertex on the grid, using numeric
    * differentiation.
    *
    * @param x x coordinate on grid.
    * @param y y coordinate on grid.
    * @param z z coordinate on grid.
    * @return normal to the mesh surface at this point.
    */
   private Vector3d calcNormal (int x, int y, int z) {

      Vector3d nrm = new Vector3d();
      //********************************************************************
      if (x == numVX - 1) {
         nrm.x = getVertexDistance (x, y, z) - getVertexDistance (x-1, y, z);
         nrm.x /= myCellWidths.x;
      }
      else if (x == 0) {
         nrm.x = getVertexDistance (x+1, y, z) - getVertexDistance (x, y, z);
         nrm.x /= myCellWidths.x;
      }
      else {
         nrm.x = getVertexDistance (x+1, y, z) - getVertexDistance (x-1, y, z);
         nrm.x /= (myCellWidths.x * 2);
      }
      //********************************************************************
      if (y == numVY - 1) {
         nrm.y = getVertexDistance (x, y, z) - getVertexDistance (x, y-1, z) ;
         nrm.y /= myCellWidths.y;
      }
      else if (y == 0) {
         nrm.y = getVertexDistance (x, y+1, z) - getVertexDistance (x, y, z);
         nrm.y /= myCellWidths.y;
      }
      else {
         nrm.y = getVertexDistance (x, y+1, z) - getVertexDistance (x, y-1, z);
         nrm.y /= (myCellWidths.y * 2);
      }
      //********************************************************************
      if (z == numVZ - 1) {
         nrm.z = getVertexDistance (x, y, z) - getVertexDistance (x, y, z-1);
         nrm.z /= myCellWidths.z;
      }
      else if (z == 0) {
         nrm.z = getVertexDistance (x, y, z+1) - getVertexDistance (x, y, z);
         nrm.z /= myCellWidths.z;
      }
      else {
         nrm.z = getVertexDistance (x, y, z+1) - getVertexDistance (x, y, z-1);
         nrm.z /= (myCellWidths.z * 2);
      }
      //********************************************************************
      nrm.normalize ();
      myNormals[xyzIndicesToVertex (x, y, z)] = nrm;
      return nrm;
   }

   /** 
    * Calculates the distance and normal at an arbitrary point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE} is returned.
    *
    * @param norm returns the normal (in mesh coordinates)
    * @param x x coordinate of point (in mesh coordinates).
    * @param y y coordinate of point (in mesh coordinates).
    * @param z z coordinate of point (in mesh coordinates).
    * @return distance to the mesh surface
    */
   public double getDistanceAndNormal (
      Vector3d norm, double x, double y, double z) {
      Vector3d point = new Vector3d (x, y, z);
      return getDistanceAndNormal (norm, point);
   }  

   /** 
    * Calculates the distance and normal at an arbitrary point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE} is returned.
    *
    * @param norm returns the normal (in mesh coordinates)
    * @param point point at which to calculate the normal and distance
    * (in mesh coordinates).
    * @return distance to the mesh surface
    */
   public double getDistanceAndNormal (Vector3d norm, Vector3d point) {
      // Change to grid coordinates
      double tempPointX = (point.x - myMinCoord.x) / myCellWidths.x;
      double tempPointY = (point.y - myMinCoord.y) / myCellWidths.y;
      double tempPointZ = (point.z - myMinCoord.z) / myCellWidths.z;
      int minx = (int)tempPointX;
      int miny = (int)tempPointY;
      int minz = (int)tempPointZ;

      if (tempPointX < 0 || tempPointX > numVX - 1 ||
      tempPointY < 0 || tempPointY > numVY - 1 ||
      tempPointZ < 0 || tempPointZ > numVZ - 1) {
         return OUTSIDE;
      }
      double dx = tempPointX - minx;
      double dy = tempPointY - miny;
      double dz = tempPointZ - minz;
      if (tempPointX == numVX - 1) {
         minx -= 1;
         dx = 1;
      }
      if (tempPointY == numVY - 1) {
         miny -= 1;
         dy = 1;
      }
      if (tempPointZ == numVZ - 1) {
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

         Vector3d nrm000 = getVertexNormal (minx  , miny  , minz  );
         Vector3d nrm001 = getVertexNormal (minx  , miny  , minz+1);
         Vector3d nrm010 = getVertexNormal (minx  , miny+1, minz  );
         Vector3d nrm011 = getVertexNormal (minx  , miny+1, minz+1);
         Vector3d nrm100 = getVertexNormal (minx+1, miny  , minz  );
         Vector3d nrm101 = getVertexNormal (minx+1, miny  , minz+1);
         Vector3d nrm110 = getVertexNormal (minx+1, miny+1, minz  );
         Vector3d nrm111 = getVertexNormal (minx+1, miny+1, minz+1);

         norm.setZero();
         norm.scaledAdd (w000, nrm000);
         norm.scaledAdd (w001, nrm001);
         norm.scaledAdd (w010, nrm010);
         norm.scaledAdd (w011, nrm011);
         norm.scaledAdd (w100, nrm100);
         norm.scaledAdd (w101, nrm101);
         norm.scaledAdd (w110, nrm110);
         norm.scaledAdd (w111, nrm111);
         //norm.scale(-1.0);
      }

      return w000*getVertexDistance (minx  , miny  , minz  ) +
      w001*getVertexDistance (minx  , miny  , minz+1) +
      w010*getVertexDistance (minx  , miny+1, minz  ) +
      w011*getVertexDistance (minx  , miny+1, minz+1) +
      w100*getVertexDistance (minx+1, miny  , minz  ) +
      w101*getVertexDistance (minx+1, miny  , minz+1) +
      w110*getVertexDistance (minx+1, miny+1, minz  ) +
      w111*getVertexDistance (minx+1, miny+1, minz+1);
   }

   /**
    * Given a vertex index <code>vi</code>, compute the corresponding
    * x, y, z indices.
    * 
    * @param vxyz returns the x, y, z indices. 
    * @param vi global index
    * @return reference to <code>vxyz</code> 
    */
   public Vector3i vertexToXyzIndices (Vector3i vxyz, int vi) {
      vxyz.z = vi / (numVXxVY);
      vxyz.y = (vi - vxyz.z * numVXxVY) / numVX;
      vxyz.x = vi % numVX;
      return vxyz;
   }
   
   /**
    * Given the x, y, z indices for a vertex, compute the corresponding
    * vertex index.
    * 
    * @param vxyz x, y, z indices
    * @return vertex index
    */
   int xyzIndicesToVertex (Vector3i vxyz) {
      return vxyz.x + vxyz.y*numVX + vxyz.z*numVXxVY;
   }
   
   /**
    * Given the x, y, z indices for a vertex, compute the corresponding
    * vertex index.
    * 
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return vertex index
    */
   int xyzIndicesToVertex (int xi, int yj, int zk) {
      return xi + yj*numVX + zk*numVXxVY;
   }
   
   /**
    * Returns the resolution of this grid along each axis. This
    * equals the number of cells along each axis.
    * 
    * @return number of cells along each axis
    */
   public Vector3i getResolution() {
      return new Vector3i (numVX-1, numVY-1, numVZ-1);
   }
   
   /**
    * Returns the grid cell widths along each axis. For each axis 
    * <code>i</code>, this equals {@code w_i/res_i}, where {@code w_i}
    * and {@code res_i} are the overall width and resolution associated
    * with that axis.
    * 
    * @return grid cell widths in each direction
    */
   public Vector3d getCellWidths() {
      return new Vector3d(myCellWidths);
   }
   
   /**
    * Returns the coordinates of the vertex with minimum x,y,z values
    * 
    * @return minimum vertex coordinates
    */
   public Point3d getMinCoords() {
      return new Point3d(myMinCoord);
   }
   
   /**
    * Returns the coordinates of the vertex with maximum x,y,z values
    * 
    * @return maximum vertex coordinates
    */
   public Point3d getMaxCoords() {
      return new Point3d(myMaxCoord);
   }

   /**
    * Returns the normal to the mesh at a vertex, as specified by
    * its x, y, z indices.
    * 
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return mesh normal at the vertex (must not be modified)
    */
   public Vector3d getVertexNormal (int xi, int yj, int zk) {
      Vector3d nrm = myNormals[xyzIndicesToVertex(xi, yj, zk)];
      if (nrm == null) {
         nrm = calcNormal (xi, yj, zk);
      }
      return nrm;
   }
   
   /**
    * Returns the normal to the mesh at a vertex, as specified by
    * its x, y, z indices.
    * 
    * @param vxyz x, y, z vertex indices
    * @return mesh distance at the vertex (must not be modified)
    */
   public Vector3d getVertexNormal (Vector3i vxyz) {
      Vector3d nrm = myNormals[xyzIndicesToVertex(vxyz)];
      if (nrm == null) {
         nrm = calcNormal (vxyz.x, vxyz.y, vxyz.z);
      }
      return nrm;
   }

   /**
    * Returns the full array of distances to the mesh at each vertex.
    * The array is indexed such that for vertex indices xi, yj, zk,
    * the corresponding index into this array is
    * <pre>
    * idx = xi + numVX*yj + (numVX*numVY)*zk
    * </pre>
    * where <code>numVX</code> and <code>numVY</code> are the number
    * of vertices along x and y axes.
    * 
    * @return array of distances
    */
   public double[] getDistances() {
      return myPhi;
   }
   
   /**
    * Returns the distance to the mesh at a specified vertex, as specified
    * by x, y, z indices.
    * 
    * @param vxyz x, y, z vertex indices
    * @return mesh distance at the vertex
    */
   public double getVertexDistance (Vector3i vxyz) {
      return myPhi[xyzIndicesToVertex(vxyz)];
   }
   
   /**
    * Returns the distance to the mesh at a specified vertex, as specified
    * by x, y, z indices.
    * 
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return mesh distance at the vertex
    */   
   public double getVertexDistance (int xi, int yj, int zk) {
      return myPhi[xyzIndicesToVertex(xi, yj, zk)];
   }

   /**
    * Utility method for the {@link #sweep sweep} method. Compares a given 
    * vertex with one of its neighbours.
    * 
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @param dx x value of the neighbouring vertex
    * @param dy y value of the neighbouring vertex
    * @param dz z value of the neighbouring vertex
    */
   private void checkNeighbouringVertex (
      int xi, int yj, int zk, int dx, int dy, int dz, 
      Point3d closestPointOnFace, Point3d p1) {
      
      // dx, dy, dz represent a point +- 1 grid cell away from out current
      // point. There are 26 neighbours.
      int neighbourIndex = xyzIndicesToVertex (dx, dy, dz);
      if (myClosestFaceIdxs[neighbourIndex] >= 0 ) {
         // Everything is in mesh coordinates.
         getVertexCoords (p1, xi, yj, zk);
         Face neighbourFace = 
            myMesh.getFace (myClosestFaceIdxs[neighbourIndex]);
         neighbourFace.nearestPoint (closestPointOnFace, p1);
         double distanceToNeighbourFace = p1.distance (closestPointOnFace);
         int index = xyzIndicesToVertex (xi, yj, zk);
         if (distanceToNeighbourFace < myPhi[index]) {
            myPhi[index] = distanceToNeighbourFace;
            // gridCellArray [index].setDistance (distanceToNeighbourFace);
            myClosestFaceIdxs [index] = 
               myClosestFaceIdxs [neighbourIndex];
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
         x1 = numVX;
      }
      else {
         x0 = numVX-2;  // sweeps backwards
         x1 = -1;
      }
      int y0, y1;
      if (dy > 0) { 
         y0 = 1;
         y1 = numVY;
      }
      else { 
         y0 = numVY-2;
         y1 = -1;
      }
      int z0, z1;
      if (dz > 0) {
         z0 = 1;
         z1 = numVZ;
      }
      else {
         z0 = numVZ-2;
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

   /**
    * Creates a new render object for rendering the points and normals
    * of this grid.
    * 
    * @return new render object
    */
   RenderObject buildRenderObject() {
      RenderObject rob = new RenderObject();

      Vector3d widths = getCellWidths();
      double len = 0.66*widths.minElement();

      if (myColorMap != null) {
         for (Color color : myColors) {
            rob.addColor (color);
         }
      }
      int vidx=0;
      Vector3d coords = new Vector3d();
      for (int xi=myRenderRanges[0]; xi<myRenderRanges[1]; xi++) {
         for (int yj=myRenderRanges[2]; yj<myRenderRanges[3]; yj++) {
            for (int zk=myRenderRanges[4]; zk<myRenderRanges[5]; zk++) {

               getVertexCoords (coords, xi, yj, zk);
               int vi = xyzIndicesToVertex (xi, yj, zk);
               int cidx = myColorMap != null ? myColorIndices[vi] : -1;
               rob.addPosition (coords);
               rob.addVertex (vidx, -1, cidx, -1);
               coords.scaledAdd (len, getVertexNormal (xi, yj, zk));
               rob.addPosition (coords);
               rob.addVertex (vidx+1, -1, cidx, -1);

               rob.addPoint (vidx);
               rob.addLine (vidx, vidx+1);
               vidx += 2;
            }
         }
      }
      return rob;
   }

   // renderable implementation ...

   /**
    * {@inheritDoc}
    */
   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   /**
    * {@inheritDoc}
    */
   public RenderProps createRenderProps() {
      return new PointLineRenderProps();
   }  

   /**
    * {@inheritDoc}
    */
   public void setRenderProps (RenderProps props) {
      if (props == null) {
         throw new IllegalArgumentException ("Render props cannot be null");
      }
      myRenderProps = createRenderProps();
      myRenderProps.set (props);
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public boolean isSelectable() {
      return false;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   /**
    * {@inheritDoc}
    */
   public void prerender (RenderList list) {
      if (myRob == null || !myRobValid) {
         myRob = buildRenderObject();
         myRobValid = true;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void render (Renderer renderer, int flags) {
      render (renderer, myRenderProps, flags);
   }
   
   public void render (Renderer renderer, RenderProps props, int flags) {

      if (myMesh != null) {
         renderer.pushModelMatrix();
         renderer.mulModelMatrix (myMesh.XMeshToWorld);
      }

      if (false) {
         Shading savedShading = renderer.getShading();
         renderer.setShading (Shading.NONE);
         renderer.setColor (0, 0, 1);
         
         Vector3d maxGrid = new Vector3d();
         maxGrid.x = myMinCoord.x + (numVX-1) * myCellWidths.x;
         maxGrid.y = myMinCoord.y + (numVY-1) * myCellWidths.y; 
         maxGrid.z = myMinCoord.z + (numVZ-1) * myCellWidths.z;

         renderer.beginDraw (DrawMode.LINES); // Draw 4 vertical lines.
         renderer.addVertex (maxGrid.x, maxGrid.y, maxGrid.z);
         renderer.addVertex (maxGrid.x, maxGrid.y, myMinCoord.z);
         renderer.addVertex (myMinCoord.x, maxGrid.y, maxGrid.z);
         renderer.addVertex (myMinCoord.x, maxGrid.y, myMinCoord.z);
         renderer.addVertex (myMinCoord.x, myMinCoord.y, maxGrid.z);
         renderer.addVertex (myMinCoord.x, myMinCoord.y, myMinCoord.z);
         renderer.addVertex (maxGrid.x, myMinCoord.y, maxGrid.z);
         renderer.addVertex (maxGrid.x, myMinCoord.y, myMinCoord.z);
         // Draw a diagonal line from max to min.
         renderer.addVertex (myMinCoord.x, myMinCoord.y, myMinCoord.z);
         renderer.addVertex (maxGrid.x, maxGrid.y, maxGrid.z);
         renderer.endDraw();
         // getGridCells();
         
         renderer.setShading (savedShading);
      }

      Vector3d widths = getCellWidths();
      double r = 0.05*widths.minElement();

      RenderObject rob = myRob;

      if (rob != null) {
         if (props.getPointStyle() == PointStyle.POINT) {
            if (props.getPointSize() != 0) {
               renderer.setColor (props.getPointColor());
               renderer.drawPoints (rob, PointStyle.POINT, props.getPointSize());
            }
         }
         else {
            if (props.getPointRadius() > 0) {
               renderer.setColor (props.getPointColor());
               renderer.drawPoints (
                  rob, props.getPointStyle(), props.getPointRadius());
            }
         }
         if (props.getLineStyle() == LineStyle.LINE) {
            if (props.getLineWidth() != 0) {
               renderer.setColor (props.getLineColor());
               renderer.drawLines (rob, LineStyle.LINE, props.getLineWidth());
            }
         }
         else {
            if (props.getLineRadius() > 0) {
               renderer.setColor (props.getLineColor());
               renderer.drawLines (
                  rob, props.getLineStyle(), props.getLineRadius());
            }
         }
      }

      if (myMesh != null) {
         renderer.popModelMatrix();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   /**
    * {@inheritDoc}
    */
   public int getRenderHints() {
      return 0;
   }

   /** 
    * Returns the closest face to the vertex indexed by <code>idx</code>.
    *
    * @param idx vertex index
    * @return nearest face to the vertex
    */
   public Face getClosestFace(int idx) {
      return myMesh.getFace(myClosestFaceIdxs[idx]);
   }

   /**
    * Find the coordinates at a vertex, as specified by
    * its x, y, z indices.
    * 
    * @param coords returns the coordinates
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return coords
    */
   public Vector3d getVertexCoords (
      Vector3d coords, int xi, int yj, int zk) {
      coords.x = xi * myCellWidths.x + myMinCoord.x;
      coords.y = yj * myCellWidths.y + myMinCoord.y;
      coords.z = zk * myCellWidths.z + myMinCoord.z;
      return coords;
   }

   /**
    * Find the coordinates at a vertex, as specified by
    * its x, y, z indices.
    * 
    * @param coords returns the coordinates
    * @param vxyz x, y, z vertex indices
    * @return coords
    */
   public Vector3d getVertexCoords (Vector3d coords, Vector3i vxyz) {
      coords.x = vxyz.x * myCellWidths.x + myMinCoord.x;
      coords.y = vxyz.y * myCellWidths.y + myMinCoord.y;
      coords.z = vxyz.z * myCellWidths.z + myMinCoord.z;
      return coords;
   }

   static int parsePositiveInt (String str) {
      int value = -1;
      try {
         value = Integer.parseInt (str);
      }
      catch (NumberFormatException e) {
         // ignore
      }
      return value;
   }

   public String getRenderRanges() {
      StringBuilder sbuild = new StringBuilder();
      int[] numv = new int[] { numVX, numVY, numVZ };
      for (int i=0; i<3; i++) {
         int lo = myRenderRanges[2*i];
         int hi = myRenderRanges[2*i+1]-1;
         if (lo == 0 && hi == numv[i]-1) {
            sbuild.append ("*");
         }
         else if (lo == hi) {
            sbuild.append (lo);
         }
         else {
            sbuild.append (lo + ":" + hi);
         }
         if (i < 2) {
            sbuild.append (" ");
         }
      }
      return sbuild.toString();
   }

   public void setRenderRanges (String str) {
      StringHolder errMsg = new StringHolder();
      int[] ranges = parseRenderRanges (str, errMsg);
      if (ranges != null) {
         myRenderRanges = ranges;
         myRobValid = false;
      }
      else {
         throw new IllegalArgumentException (
            "Error parsing string: " + errMsg.value);
      }
   }

   public int[] parseRenderRanges (String str, StringHolder errorMsg) {

      String[] strs = str.split ("\\s+");
      if (strs.length > 0 && strs[0].equals ("")) {
         strs = Arrays.copyOfRange (strs, 1, strs.length);
      }
      int numv[] = new int[] { numVX, numVY, numVZ };

      int[] ranges = new int[6];
      String error = null;
      if (strs.length == 1 && strs[0].equals ("*")) {
         ranges[0] = 0;
         ranges[1] = numv[0];
         ranges[2] = 0;
         ranges[3] = numv[1];
         ranges[4] = 0;
         ranges[5] = numv[2];
      }
      else if (strs.length > 3) {
         error = "More than three subranges specified";
      }
      else {
         int i = 0;
         for (String s : strs) {
            int lo = -1;
            int hi = -1;
            if (s.equals ("*")) {
               lo = 0;
               hi = numv[i]-1;
            }
            else if (s.indexOf(':') != -1) {
               String[] substrs = s.split (":");
               if (substrs.length > 2) {
                  error = "More than one ':' in subrange";
                  break;
               }
               if (substrs.length < 2 || substrs[0].equals ("")) {
                  error = "Missing low or high value in subrange";
                  break;
               }
               lo = parsePositiveInt (substrs[0]);
               hi = parsePositiveInt (substrs[1]);
               if (lo < 0 || hi < 0) {
                  error = "Malformed or negative integer";
                  break;
               }
               if (lo > hi) {
                  error = "Low > high in subrange";
                  break;
               }
               if (lo > numv[i]-1 || hi > numv[i]-1) {
                  error = "Range error in subrange";
                  break;
               }
            }
            else {
               lo = parsePositiveInt (s);
               if (lo < 0) {
                  error = "Malformed or negative integer";
                  break;
               }
               if (lo > numv[i]-1) {
                  error = "Range error in subrange";
                  break;
               }
               hi = lo;
            }
            ranges[2*i] = lo;
            ranges[2*i+1] = hi+1;
            i++;
         }
         if (i < 3 && error == null) {
            error = "Less than three subranges specified";
         }
      }
      if (error != null) {
         ranges = null;
      }
      if (errorMsg != null) {
         errorMsg.value = error;
      }
      return ranges;
   }

   static String readLine (BufferedReader reader) {
      try {
         return reader.readLine();
      }
      catch (Exception e) {
         return null;
      }
   }

   public static void main (String[] args) {
      BufferedReader reader =
         new BufferedReader(new InputStreamReader(System.in));
      String line;
      SignedDistanceGrid grid = new SignedDistanceGrid();
      grid.numVX = 10;
      grid.numVY = 5;
      grid.numVZ = 5;
      while ((line = readLine(reader)) != null) {
         StringHolder errorMsg = new StringHolder();
         if (errorMsg.value == null) {
            grid.setRenderRanges (line);
            System.out.println (grid.getRenderRanges());
         }
         else {
            System.out.println (errorMsg.value);
         }
      }
   }

}
