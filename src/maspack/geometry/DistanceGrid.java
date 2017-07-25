/**
 * Copyright (c) 2017, by the Authors: Bruce Haines (UBC), Antonio Sanchez
 * (UBC), John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.Matrix3d;
import maspack.render.PointLineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.Shading;
import maspack.util.StringHolder;

/**
 * Implements a distance field for fixed triangular mesh. One common
 * use of such a field is to detect point penetration distances and normals for
 * use in collision handling.
 *
 * <p>The field is implemented using a regular 3D grid composed of
 * <code>numVX</code> X <code>numVY</code> X <code>numVZ</code> vertices along
 * the x, y and z directions, dividing the volume into <code>(numVX-1)</code> X
 * <code>(numVY-1)</code> X <code>(numVZ-1)</code> cells. For vertices close to
 * the mesh, nearby Features are examined to determine the distance from the mesh
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
public class DistanceGrid implements Renderable {

   protected Vector3d myCellWidths;     // cell widths along x, y, z
   protected double[] myPhi;            // distance values at each vertex
   protected Vector3d[] myNormals;      // normal values at each vertex
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
    * Maximum of the distance field, in mesh coordinates.
    */
   protected Point3d myMaxCoord = new Point3d();
   /**
    * Minimum of the distance field, in mesh coordinates.
    */
   protected Point3d myMinCoord = new Point3d();

   /**
    * Diamter of the grid
    */
   private double myDiameter;

   protected int numVX = 0;  // number of vertices along X
   protected int numVY = 0;  // number of vertices along Y
   protected int numVZ = 0;  // number of vertices along Z
   protected int numVXxVY = 0; // numVX*numVY

   /**
    * An array giving the index of the nearest Feature to each vertex.
    */
   protected int[] myClosestFeatureIdxs;
   
   protected Feature[] myFeatures;
   protected AffineTransform3dBase myWorldTransform;

   DistanceGrid () {
      myRenderProps = createRenderProps();
   }
   
   /**
    * Sets a transform to be used to map between local and world coordinates
    * @param trans local-to-world transform
    */
   public void setWorldTransform(AffineTransform3dBase trans) {
      myWorldTransform = trans;
   }
   
   /**
    * Converts point to local coordinates, if the distance grid has a world transform
    * @param local populated local coordinates
    * @param world world coordinates
    */
   private void getLocalCoordinates(Point3d local, Point3d world) {
      if (local != world) {
         local.set(world);
      }
      if (myWorldTransform != null) {
         local.inverseTransform(myWorldTransform);
      }
   }
   
   /**
    * Converts point to world coordinates, if the distance grid has a world transform
    * @param world populated world coordinates
    * @param local local coordinates
    */
   private void getWorldCoordinates(Point3d world, Point3d local) {
      if (local != world) {
         world.set(local);
      }
      if (myWorldTransform != null) {
         world.transform(myWorldTransform);
      }
   }
   
   /**
    * Converts a normal to world coordinates
    * @param world populated world normal
    * @param local local normal
    */
   private void getWorldNormal(Vector3d world, Vector3d local) {
      if (local != world) {
         world.set(local);
      }
      if (myWorldTransform != null) {
         myWorldTransform.getMatrix().mulInverseTranspose(world);
      }
   }

   /**
    * Creates a new distance grid for a specified set of features. The feature is
    * created as for {@link #SignedDistanceFeatureGrid(Features,double,Vector3i)},
    * with the number of cells along each axis given by 25.
    *
    * @param features features for which the grid should be created
    * @param marginFraction multiplied by the width in each direction to
    * determine the margin for that direction
    */
   public DistanceGrid (Feature[] features, double marginFraction) {
      this (features, marginFraction, new Vector3i (25, 25, 25));
   }

   private void setFeaturesAndBounds (
      Vector3d widths, Vector3d margin,
      Feature[] features, double marginFraction) {
      
      myMinCoord.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      myMaxCoord.set(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
      for (Feature f : features) {
         f.updateBounds(myMinCoord, myMaxCoord);
      }
            
      widths.sub (myMaxCoord, myMinCoord);
      margin.scale(marginFraction, widths);
      widths.scaledAdd(2, margin);     
   }

   /**
    * Creates a new distance grid for a specified list of features. 
    * The grid is aligned with the x, y, z axes, is centered on the features, and has cells of
    * uniform size. Its widths are first determined using the feature bounds and
    * <code>marginFraction</code>. The axis with maximum
    * width is then divided into <code>maxResolution</code> cells, while the
    * other axes are divided into a number of cells {@code <= maxResolution}
    * with the same cell width, with the overall axis widths grown as necessary
    * to accommodate this.
    * 
    * @param features list of features for which the grid should be created
    * @param marginFraction multiplied by the width in each direction
    * to determine the (initial) margin for that direction
    * @param maxResolution number of grid cells along the axis of maximum
    * width
    */
   public DistanceGrid (
      Feature[] features, double marginFraction, int maxResolution) {
      this();

      Vector3d widths = new Vector3d();
      Vector3d margin = new Vector3d();
      setFeaturesAndBounds (widths, margin, features, marginFraction);
      
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
      myDiameter = myMaxCoord.distance(myMinCoord);
      
      calculatePhi (features, margin.norm());      
      clearColors();
      myRenderRanges = new int[] {0, numVX, 0, numVY, 0, numVZ};
   }
   
   /**
    * Creates a new distance grid for a specified features. The grid is
    * aligned with the x, y, z axes and centered on the features. Its width along
    * each axis is computed from the features' maximum and minimum bounds, and
    * then enlarged by a margin computed by multiplying the width by
    * <code>marginFraction</code>. The width along each axis is therefore
    * <pre>
    *   width = (1 + 2*marginFraction)*boundsWidth
    * </pre>
    * where <code>boundsWidth</code> is the width determined from the feature
    * bounds.
    * 
    * @param features features for which the grid should be created
    * @param marginFraction multiplied by the width in each direction
    * to determine the margin for that direction
    * @param resolution number of grid cells that should be used along
    * each axis
    */
   public DistanceGrid (
      Feature[] features, double marginFraction, Vector3i resolution) {

      this();

      Vector3d widths = new Vector3d();
      Vector3d margin = new Vector3d();
      setFeaturesAndBounds (widths, margin, features, marginFraction);
      
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
      
      calculatePhi (features, margin.norm());
      clearColors();
      myRenderRanges = new int[] {0, numVX, 0, numVY, 0, numVZ};
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
    * Calculates the distance field.
    */
   protected void calculatePhi (Feature[] features, double marginDist) {
      // Logger logger = Logger.getSystemLogger();
      //logger.info ("Calculating Distance Field...");

      myFeatures = features;
      
      double maxDist = myMaxCoord.distance (myMinCoord);
      int numV = numVX*numVY*numVZ;

      myPhi = new double [numV];
      myNormals = new Vector3d [numV];
      myColorIndices = new int [numV];

      for (int p = 0; p < myPhi.length; p++) {
         myPhi[p] = maxDist;
      }
      // The index of closestFeature matches with phi.
      // Each entry in closestFeature is the index of the closest 
      // Feature to the grid vertex.
      myClosestFeatureIdxs = new int[myPhi.length];
      int zIntersectionCount[] = new int[myPhi.length];
      
      for (int i = 0; i < myPhi.length; i++) {
         myClosestFeatureIdxs[i] = -1;
         zIntersectionCount[i] = 0;
      }
      Point3d featureMin          = new Point3d();
      Point3d featureMax          = new Point3d();
      Point3d closestPoint     = new Point3d();
      Point3d currentPointFeature = new Point3d();
      Point3d pc = new Point3d(); // A temp variable passed in for performance.
      Point3d p1 = new Point3d(); // A temp variable passed in for performance.

      // For every feature
      for (int t=0; t<features.length; ++t) {
         Feature feature = features[t];
         featureMin.set (Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY);
         featureMax.set (Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY, 
            Double.NEGATIVE_INFINITY);
         feature.updateBounds (featureMin, featureMax);
         // Converting features min/max to grid coordinates.
         int featureMinX = (int)((featureMin.x - myMinCoord.x) / myCellWidths.x);
         int featureMinY = (int)((featureMin.y - myMinCoord.y) / myCellWidths.y);
         int featureMinZ = (int)((featureMin.z - myMinCoord.z) / myCellWidths.z);
         if (featureMinX < 0) {
            featureMinX = 0;
         }
         if (featureMinY < 0) {
            featureMinY = 0;
         }
         if (featureMinZ < 0) {
            featureMinZ = 0;
         }
         int featureMaxX = (int)((featureMax.x - myMinCoord.x) / myCellWidths.x) + 1;
         int featureMaxY = (int)((featureMax.y - myMinCoord.y) / myCellWidths.y) + 1;
         int featureMaxZ = (int)((featureMax.z - myMinCoord.z) / myCellWidths.z) + 1;
         if (featureMaxX > numVX - 1) {
            featureMaxX = numVX - 1;
         }
         if (featureMaxY > numVY - 1) {
            featureMaxY = numVY - 1;
         }
         if (featureMaxZ > numVZ - 1) {
            featureMaxZ = numVZ - 1;
         }

         // Now go through the entire parallelpiped. Calculate distance and
         // closestFeature.
         for (int z = featureMinZ; z <= featureMaxZ; z++) {
            for (int y = featureMinY; y <= featureMaxY; y++) {
               for (int x = featureMinX; x <= featureMaxX; x++) {
                  // Get features coordinates
                  getLocalVertexCoords (currentPointFeature, x, y, z);
                  // Get the distance from this point to the Feature.
                  feature.nearestPoint (closestPoint, currentPointFeature);
                  double distance = currentPointFeature.distance (closestPoint);
                  int index = xyzIndicesToVertex (x, y, z);
                  if (distance < myPhi[index]) {
                     myPhi[index] = distance;
                     myClosestFeatureIdxs [index] = t;
                  }
               }
            }
         }
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
      
   }

   /** 
    * Calculates the normal at a vertex on the grid, using numeric
    * differentiation.
    *
    * @param x x coordinate on grid.
    * @param y y coordinate on grid.
    * @param z z coordinate on grid.
    * @return normal to the features at this point.
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
    * @param norm returns the normal (in local coordinates)
    * @param x x coordinate of point (in local coordinates).
    * @param y y coordinate of point (in local coordinates).
    * @param z z coordinate of point (in local coordinates).
    * @return distance to the nearest feature
    */
   public double getLocalDistanceAndNormal (
      Vector3d norm, double x, double y, double z) {
      Point3d point = new Point3d (x, y, z);
      return getLocalDistanceAndNormal (norm, null, point);
   }
   
   /** 
    * Calculates the distance and normal at an arbitrary world point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE} is returned.
    *
    * @param norm returns the normal (in world coordinates)
    * @param x x coordinate of point (in world coordinates).
    * @param y y coordinate of point (in world coordinates).
    * @param z z coordinate of point (in world coordinates).
    * @return distance to the nearest feature
    */
   public double getWorldDistanceAndNormal(Vector3d norm, double x, double y, double z) {
      Point3d point = new Point3d(x,y,z);
      getLocalCoordinates(point, point);
      double d = getLocalDistanceAndNormal(norm, null, point);
      // transform normal by inverse transform
      if (norm != null) {
         getWorldNormal(norm, norm);
      }
      return d;
   }
   
   /** 
    * Calculates the distance and gradient at an arbitrary point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE} is returned.
    *
    * @param grad returns the gradient (in local coordinates)
    * @param x x coordinate of point (in local coordinates).
    * @param y y coordinate of point (in local coordinates).
    * @param z z coordinate of point (in local coordinates).
    * @return distance to the nearest feature
    */
   public double getLocalDistanceAndGradient(
      Vector3d grad, double x, double y, double z) {
      Point3d point = new Point3d(x, y, z);
      return getLocalDistanceAndGradient(grad, point);
   }
   
   /** 
    * Calculates the distance and gradient at an arbitrary world point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE} is returned.
    *
    * @param norm returns the grad (in world coordinates)
    * @param x x coordinate of point (in world coordinates).
    * @param y y coordinate of point (in world coordinates).
    * @param z z coordinate of point (in world coordinates).
    * @return distance to the nearest feature
    */
   public double getWorldDistanceAndGradient(Vector3d grad, double x, double y, double z) {
      Point3d point = new Point3d(x,y,z);
      return getWorldDistanceAndGradient(grad, point);
   }
   
   /**
    * Determines nearest feature to a point
    * @param nearest populates with the nearest point on the feature
    * @param point point for which to find nearest feature (in local coordinates)
    * @return nearest feature, or null if outside of domain
    */
   public Feature getNearestLocalFeature(Point3d nearest, Point3d point) {
      
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
         return null;
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
      
      Point3d tmp = new Point3d();
      double dmin = Double.POSITIVE_INFINITY;
      Feature nf = null;
      
      int[] coords = {minx, miny, minz, 
                    minx+1, miny, minz,
                    minx, miny+1, minz,
                    minx, miny, minz+1,
                    minx+1, miny+1, minz,
                    minx+1, miny, minz+1,
                    minx, miny+1, minz+1,
                    minx+1, miny+1, minz+1};
      
      // check 8 nearest features from corners
      for (int i=0; i<coords.length; i+=3) {
         int idx = xyzIndicesToVertex(coords[i], coords[i+1], coords[i+2]);
         Feature f = myFeatures[idx];
         f.nearestPoint(tmp, point);
         double d = tmp.distance(point);
         if (d < dmin) {
            dmin = d;
            if (nearest != null) {
               nearest.set(tmp);
            }
            nf = f;
         }   
      }
      
      return nf; 
      
   }
   
   /**
    * Determines nearest feature to a point
    * @param nearest populates with the nearest point on the feature
    * @param point point for which to find nearest feature (in world coordinates)
    * @return nearest feature, or null if outside of domain
    */
   public Feature getNearestWorldFeature(Point3d nearest, Point3d point) {
      Point3d lpnt = new Point3d();
      getLocalCoordinates(lpnt, point);
      Feature out = getNearestLocalFeature(nearest, lpnt);
      getWorldCoordinates(nearest, nearest);
      return out;
   }

   /** 
    * Calculates the distance at an arbitrary point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE} is returned.
    *
    * @param point point at which to calculate the normal and distance
    * (in local coordinates).
    * @return distance to the nearest feature
    */
   public double getLocalDistance(Point3d point) {
      return getLocalDistanceAndNormal(null, null, point);
   }
   
   public double getWorldDistance(Point3d point) {
      Point3d lpnt = new Point3d();
      getLocalCoordinates(lpnt, point);
      return getLocalDistance(lpnt);
   }
   
   /** 
    * Calculates the distance and normal at an arbitrary point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE} is returned.
    *
    * @param norm returns the normal (in local coordinates)
    * @param point point at which to calculate the normal and distance
    * (in local coordinates).
    * @return distance to the nearest feature
    */
   public double getLocalDistanceAndNormal (
      Vector3d norm, Point3d point) {
      return getLocalDistanceAndNormal(norm, null, point);
   }
   
   /** 
    * Calculates the distance and normal at an arbitrary point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE} is returned.
    *
    * @param norm returns the normal (in local coordinates)
    * @param Dnrm returns the normal derivative (in local coordinates)
    * @param point point at which to calculate the normal and distance
    * (in local coordinates).
    * @return distance to the nearest feature
    */
   public double getLocalDistanceAndNormal (
      Vector3d norm, Matrix3d Dnrm, Point3d point) {
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
      // Compute weights w000, w001, w010, etc. for trilinear interpolation.
      
      // w001z, w011z, etc. are the derivatives of the weights
      // with respect to dz. Can use to compute weights, and also
      // to compute Dnrm if needed
      double w001z = (1-dx)*(1-dy);
      double w011z = (1-dx)*dy;
      double w101z = dx*(1-dy);
      double w111z = dx*dy;

      double w000  = w001z*(1-dz);
      double w001  = w001z*dz;
      double w010  = w011z*(1-dz);
      double w011  = w011z*dz;
      double w100  = w101z*(1-dz);
      double w101  = w101z*dz;
      double w110  = w111z*(1-dz);
      double w111  = w111z*dz;

      double d000  = getVertexDistance (minx  , miny  , minz  );
      double d001  = getVertexDistance (minx  , miny  , minz+1);
      double d010  = getVertexDistance (minx  , miny+1, minz  );
      double d011  = getVertexDistance (minx  , miny+1, minz+1);
      double d100  = getVertexDistance (minx+1, miny  , minz  );
      double d101  = getVertexDistance (minx+1, miny  , minz+1);
      double d110  = getVertexDistance (minx+1, miny+1, minz  );
      double d111  = getVertexDistance (minx+1, miny+1, minz+1);

      double d =
         w000*d000 + w001*d001 + w010*d010 + w011*d011 +
         w100*d100 + w101*d101 + w110*d110 + w111*d111;
      
      if (norm != null || Dnrm != null) {
         Vector3d n000 = getLocalVertexNormal (minx  , miny  , minz  );
         Vector3d n001 = getLocalVertexNormal (minx  , miny  , minz+1);
         Vector3d n010 = getLocalVertexNormal (minx  , miny+1, minz  );
         Vector3d n011 = getLocalVertexNormal (minx  , miny+1, minz+1);
         Vector3d n100 = getLocalVertexNormal (minx+1, miny  , minz  );
         Vector3d n101 = getLocalVertexNormal (minx+1, miny  , minz+1);
         Vector3d n110 = getLocalVertexNormal (minx+1, miny+1, minz  );
         Vector3d n111 = getLocalVertexNormal (minx+1, miny+1, minz+1);

         if (norm == null && Dnrm != null) {
            norm = new Vector3d();
         }
         
         if (norm != null) {
            norm.setZero();
            norm.scaledAdd (w000, n000);
            norm.scaledAdd (w001, n001);
            norm.scaledAdd (w010, n010);
            norm.scaledAdd (w011, n011);
            norm.scaledAdd (w100, n100);
            norm.scaledAdd (w101, n101);
            norm.scaledAdd (w110, n110);
            norm.scaledAdd (w111, n111);
            //norm.normalize();
         }
         
         if (Dnrm != null) {
            // compute weight derivatives with respect to dx and dy
            double w100x = (1-dy)*(1-dz);
            double w101x = (1-dy)*dz;
            double w110x = dy*(1-dz);
            double w111x = dy*dz;

            double w010y = (1-dx)*(1-dz);
            double w011y = (1-dx)*dz;
            double w110y = dx*(1-dz);
            double w111y = dx*dz;           

            if (true) {
               
            Dnrm.m00 =
               (-w100x*n000.x - w101x*n001.x - w110x*n010.x - w111x*n011.x
                +w100x*n100.x + w101x*n101.x + w110x*n110.x + w111x*n111.x);
            Dnrm.m10 =
               (-w100x*n000.y - w101x*n001.y - w110x*n010.y - w111x*n011.y
                +w100x*n100.y + w101x*n101.y + w110x*n110.y + w111x*n111.y);
            Dnrm.m20 =
               (-w100x*n000.z - w101x*n001.z - w110x*n010.z - w111x*n011.z
                +w100x*n100.z + w101x*n101.z + w110x*n110.z + w111x*n111.z);

            double s = d/myCellWidths.x;
            Dnrm.m00 *= s;
            Dnrm.m10 *= s;
            Dnrm.m20 *= s;
            
            Dnrm.m01 =
               (-w010y*n000.x - w011y*n001.x + w010y*n010.x + w011y*n011.x
                -w110y*n100.x - w111y*n101.x + w110y*n110.x + w111y*n111.x);
            Dnrm.m11 =
               (-w010y*n000.y - w011y*n001.y + w010y*n010.y + w011y*n011.y
                -w110y*n100.y - w111y*n101.y + w110y*n110.y + w111y*n111.y);
            Dnrm.m21 =
               (-w010y*n000.z - w011y*n001.z + w010y*n010.z + w011y*n011.z
                -w110y*n100.z - w111y*n101.z + w110y*n110.z + w111y*n111.z);

            s = d/myCellWidths.y;
            Dnrm.m01 *= s;
            Dnrm.m11 *= s;
            Dnrm.m21 *= s;

            Dnrm.m02 =
               (-w001z*n000.x + w001z*n001.x - w011z*n010.x + w011z*n011.x
                -w101z*n100.x + w101z*n101.x - w111z*n110.x + w111z*n111.x);
            Dnrm.m12 =
               (-w001z*n000.y + w001z*n001.y - w011z*n010.y + w011z*n011.y
                -w101z*n100.y + w101z*n101.y - w111z*n110.y + w111z*n111.y);
            Dnrm.m22 =
               (-w001z*n000.z + w001z*n001.z - w011z*n010.z + w011z*n011.z
                -w101z*n100.z + w101z*n101.z - w111z*n110.z + w111z*n111.z);
            
            s = d/myCellWidths.z;
            Dnrm.m02 *= s;
            Dnrm.m12 *= s;
            Dnrm.m22 *= s;
            }
            else {
               Dnrm.setZero();
            }
               
            Vector3d grad = new Vector3d();

            grad.x = (-w100x*d000 - w101x*d001 - w110x*d010 - w111x*d011
                      +w100x*d100 + w101x*d101 + w110x*d110 + w111x*d111);
            grad.y = (-w010y*d000 - w011y*d001 + w010y*d010 + w011y*d011
                      -w110y*d100 - w111y*d101 + w110y*d110 + w111y*d111);
            grad.z = (-w001z*d000 + w001z*d001 - w011z*d010 + w011z*d011
                      -w101z*d100 + w101z*d101 - w111z*d110 + w111z*d111);
            
            grad.x /= myCellWidths.x;
            grad.y /= myCellWidths.y;
            grad.z /= myCellWidths.z;
            Dnrm.addOuterProduct (norm, grad);
         }
      }
      return d;
   }
   
   /** 
    * Calculates the distance and normal at an arbitrary point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE} is returned.
    *
    * @param norm returns the normal (in world coordinates)
    * @param point point at which to calculate the normal and distance
    * (in world coordinates).
    * @return distance to the nearest feature
    */
   public double getWorldDistanceAndNormal (Vector3d norm, Point3d point) {
      Point3d lpnt = new Point3d();
      getLocalCoordinates(lpnt, point);
      double d = getLocalDistanceAndNormal(norm, null, lpnt);
      if (norm != null) {
         getWorldNormal(norm, norm);
      }
      return d;
   }

   /** 
    * Calculates the distance and gradient at an arbitrary point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE} is returned.
    *
    * @param grad returns the gradient direction (in local coordinates)
    * @param point point at which to calculate the gradient and distance
    * (in local coordinates).
    * @return distance to the nearest feature
    */
   public double getLocalDistanceAndGradient (Vector3d grad, Point3d point) {
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

      double d000 = getVertexDistance (minx  , miny  , minz  );
      double d001 = getVertexDistance (minx  , miny  , minz+1);
      double d010 = getVertexDistance (minx  , miny+1, minz  );
      double d011 = getVertexDistance (minx  , miny+1, minz+1);
      double d100 = getVertexDistance (minx+1, miny  , minz  );
      double d101 = getVertexDistance (minx+1, miny  , minz+1);
      double d110 = getVertexDistance (minx+1, miny+1, minz  );
      double d111 = getVertexDistance (minx+1, miny+1, minz+1);

      if (grad != null) {
         grad.x = -(1-dy)*(1-dz)*d000 -(1-dy)*dz*d001
            -dy*(1-dz)*d010 - dy*dz*d011
            + (1-dy)*(1-dz)*d100 + (1-dy)*dz*d101
            + dy*(1-dz)*d110 + dy*dz*d111;
         grad.y = -(1-dx)*(1-dz)*d000 -(1-dx)*dz*d001 
            +(1-dx)*(1-dz)*d010 + (1-dx)*dz*d011
            - dx*(1-dz)*d100 - dx*dz*d101
            + dx*(1-dz)*d110 + dx*dz*d111;
         grad.z = -(1-dx)*(1-dy)*d000+(1-dx)*(1-dy)*d001
            -(1-dx)*dy*d010+(1-dx)*dy*d011
            -dx*(1-dy)*d100+dx*(1-dy)*d101
            -dx*dy*d110+dx*dy*d111;
      }      
      
      return w000*d000 + w001*d001 + w010*d010 + w011*d011 + w100*d100 +
         w101*d101 + w110*d110 + w111*d111;
   }
   
   /** 
    * Calculates the distance and gradient at an arbitrary point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE} is returned.
    *
    * @param grad returns the gradient (in world coordinates)
    * @param point point at which to calculate the gradient and distance
    * (in world coordinates).
    * @return distance to the nearest feature
    */
   public double getWorldDistanceAndGradient (Vector3d grad, Point3d point) {
      Point3d lpnt = new Point3d();
      getLocalCoordinates(lpnt, point);
      double d = getLocalDistanceAndGradient(grad, lpnt);
      if (grad != null) {
         // gradients transform like normals
         getWorldNormal(grad, grad);
      }
      return d;
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
   private int xyzIndicesToVertex (Vector3i vxyz) {
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
   protected int xyzIndicesToVertex (int xi, int yj, int zk) {
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
    * Returns the diameter of this grid, defined as the distance
    * between the maximum and minimum coordinates.
    *
    * @return diameter of this grid
    */
   public double getDiameter() {
      return myDiameter;
   }

   /**
    * Returns the normal to the nearest feature at a vertex, as specified by
    * its x, y, z indices.
    * 
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return nearest feature normal at the vertex (must not be modified)
    */
   protected Vector3d getLocalVertexNormal (int xi, int yj, int zk) {
      Vector3d nrm = myNormals[xyzIndicesToVertex(xi, yj, zk)];
      if (nrm == null) {
         nrm = calcNormal (xi, yj, zk);
      }
      return nrm;
   }
   
   /**
    * Returns the normal to the nearest feature at a vertex, as specified by
    * its x, y, z indices.
    * 
    * @param vxyz x, y, z vertex indices
    * @return nearest feature distance at the vertex (must not be modified)
    */
   private Vector3d getLocalVertexNormal (Vector3i vxyz) {
      Vector3d nrm = myNormals[xyzIndicesToVertex(vxyz)];
      if (nrm == null) {
         nrm = calcNormal (vxyz.x, vxyz.y, vxyz.z);
      }
      return nrm;
   }

   /**
    * Returns the full array of distances to the features at each vertex.
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
    * Returns the distance to the features at a specified vertex, as specified
    * by x, y, z indices.
    * 
    * @param vxyz x, y, z vertex indices
    * @return mesh distance at the vertex
    */
   public double getVertexDistance (Vector3i vxyz) {
      return myPhi[xyzIndicesToVertex(vxyz)];
   }
   
   /**
    * Returns the distance to the features at a specified vertex, as specified
    * by x, y, z indices.
    * 
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return nearest feature distance at the vertex
    */   
   protected double getVertexDistance (int xi, int yj, int zk) {
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
      Point3d closestPointOnFeature, Point3d p1) {
      
      // dx, dy, dz represent a point +- 1 grid cell away from out current
      // point. There are 26 neighbours.
      int neighbourIndex = xyzIndicesToVertex (dx, dy, dz);
      if (myClosestFeatureIdxs[neighbourIndex] >= 0 ) {
         // Everything is in local coordinates.
         getLocalVertexCoords (p1, xi, yj, zk);
         Feature neighbourFeature = myFeatures[myClosestFeatureIdxs[neighbourIndex]];
         neighbourFeature.nearestPoint (closestPointOnFeature, p1);
         double distanceToNeighbourFeature = p1.distance (closestPointOnFeature);
         int index = xyzIndicesToVertex (xi, yj, zk);
         if (distanceToNeighbourFeature < myPhi[index]) {
            myPhi[index] = distanceToNeighbourFeature;
            // gridCellArray [index].setDistance (distanceToNeighbourFeature);
            myClosestFeatureIdxs [index] = 
               myClosestFeatureIdxs [neighbourIndex];
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
   protected void sweep (int dx, int dy, int dz, Point3d pc, Point3d p1) {
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

               getLocalVertexCoords (coords, xi, yj, zk);
               int vi = xyzIndicesToVertex (xi, yj, zk);
               int cidx = myColorMap != null ? myColorIndices[vi] : -1;
               rob.addPosition (coords);
               rob.addVertex (vidx, -1, cidx, -1);
               coords.scaledAdd (len, getLocalVertexNormal (xi, yj, zk));
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

      if (myWorldTransform != null) {
         renderer.pushModelMatrix();
         renderer.mulModelMatrix (myWorldTransform);
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

      if (myWorldTransform != null) {
         renderer.popModelMatrix();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      Point3d w = new Point3d();
      getWorldCoordinates(w, myMinCoord);
      w.updateBounds(pmin, pmax);
      getWorldCoordinates(w, myMaxCoord);
      w.updateBounds(pmin, pmax);
   }

   /**
    * {@inheritDoc}
    */
   public int getRenderHints() {
      return 0;
   }

   /** 
    * Returns the closest Feature to the vertex indexed by <code>idx</code>.
    *
    * @param idx vertex index
    * @return nearest Feature to the vertex
    */
   public Feature getClosestFeature(int idx) {
      return myFeatures[myClosestFeatureIdxs[idx]];
   }

   /** 
    * Returns the index of the closest vertex to a point.
    *
    * @param point point for which to calculate closest vertex
    * @return index of closest vertex to <code>point</code>
    */
   public int getClosestVertex (Point3d point) {

      // Change to grid coordinates
      int xi = (int)Math.rint((point.x - myMinCoord.x) / myCellWidths.x);
      int yi = (int)Math.rint((point.y - myMinCoord.y) / myCellWidths.y);
      int zi = (int)Math.rint((point.z - myMinCoord.z) / myCellWidths.z);
      if (xi < 0) {
         xi = 0;
      }
      else if (xi > numVX-1) {
         xi = numVX-1;
      }
      if (yi < 0) {
         yi = 0;
      }
      else if (yi > numVY-1) {
         yi = numVY-1;
      }
      if (zi < 0) {
         zi = 0;
      }
      else if (zi > numVZ-1) {
         zi = numVZ-1;
      }
      return xyzIndicesToVertex (xi, yi, zi);
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
   protected Vector3d getLocalVertexCoords (
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
   public Vector3d getLocalVertexCoords (Vector3d coords, Vector3i vxyz) {
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
      DistanceGrid grid = new DistanceGrid();
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
   
   /**
    * Apply the smoothing operation n times
    * @param n
    */
   public void smooth(int n) {
      for (int i=0; i<n; ++i) {
         smooth();
      }
   }
   
   /**
    * For each point, computes the inverse-distance-weighted difference with neighbours
    *   (an approximation of the gradient) and adds a fraction of this weighted difference to phi(xi)
    * 
    * @param alpha scale factor for added gradient, positive for contraction, negative for expansion
    */
   private void smoothIter(double alpha) {
      double[] sphi = new double[myPhi.length];
      for (int i=0; i<numVX; ++i) {
         for (int j=0; j<numVY; ++j) {
            for (int k=0; k<numVZ; ++k) {
               
               // loop through 3x3x3 neighbourhood
               double wtotal = 0; 
               double p = 0;
               
               for (int ii=Math.max(i-1, 0); ii<Math.min(i+2, numVX); ++ii) {
                  for (int jj=Math.max(j-1, 0); jj<Math.min(j+2, numVY); ++jj) {
                     for (int kk=Math.max(k-1, 0); kk<Math.min(k+2, numVZ); ++kk) {
                        if (ii != i || jj != j || kk != k) {
                           int idx = xyzIndicesToVertex(ii,jj,kk);
                           // squared distance
                           double d2 = (ii-i)*(ii-i)*myCellWidths.x*myCellWidths.x + 
                              (jj-j)*(jj-j)*myCellWidths.y*myCellWidths.y +
                              (kk-k)*(kk-k)*myCellWidths.z*myCellWidths.z;
                           double w = 1.0/Math.sqrt(d2);
                           p += w*myPhi[idx];  // weighted by inverse distance
                           wtotal += w;
                        }
                     }
                  }
               }
               
               int idx = xyzIndicesToVertex(i, j, k);
               sphi[idx] = myPhi[idx] + alpha*p/wtotal;
            }
         }
      }
      
      myPhi = sphi;
   }
   
   /**
    * Taubin Smoothing
    * 
    * @param lambda > 0, fraction of gradient to shrink
    * @param mu < 0, fraction of gradient to expand
    */
   public void smooth(double lambda, double mu) {
      smoothIter(lambda);
      smoothIter(mu);
   }
   
   /**
    * Applies Taubin smoothing
    * @param lambda > 0, fraction of gradient to shrink
    * @param mu < 0, fraction of gradient to expand
    * @param iters number of applications
    */
   public void smooth(double lambda, double mu, int iters) {
      for (int i=0; i<iters; ++i) {
         smoothIter(lambda);
         smoothIter(mu);
      }
   }
   
   /**
    * Applies a simple Laplacian smoothing operation to the distance grid
    */
   public void smooth() {
      
      double[] sphi = new double[myPhi.length];
      for (int i=0; i<numVX; ++i) {
         for (int j=0; j<numVY; ++j) {
            for (int k=0; k<numVZ; ++k) {
               
               // loop through 3x3x3 neighbourhood
               int N = 0; 
               double p = 0;
               for (int ii=Math.max(i-1, 0); ii<Math.min(i+2, numVX); ++ii) {
                  for (int jj=Math.max(j-1, 0); jj<Math.min(j+2, numVY); ++jj) {
                     for (int kk=Math.max(k-1, 0); kk<Math.min(k+2, numVZ); ++kk) {
                        int idx = xyzIndicesToVertex(ii,jj,kk);
                        p += myPhi[idx];
                        ++N;
                     }
                  }
               }
               
               int idx = xyzIndicesToVertex(i, j, k);
               sphi[idx] = p/N;
            }
         }
      }
      
      myPhi = sphi;
      
   }

   public PolygonalMesh computeDistanceSurface() {
      MarchingTetrahedra marcher = new MarchingTetrahedra();

      return marcher.createMesh (
         myPhi, myMinCoord, myCellWidths, getResolution(), /*iso=*/0);
   }
   
   public PolygonalMesh computeDistanceSurface(double val) {
      MarchingTetrahedra marcher = new MarchingTetrahedra();

      return marcher.createMesh (
         myPhi, myMinCoord, myCellWidths, getResolution(), val);
   }

   public AffineTransform3dBase getWorldTransform() {
      return myWorldTransform;
   }
   
   public double getRadius() {
      return myMaxCoord.distance(myMinCoord)/2;
   }

}
