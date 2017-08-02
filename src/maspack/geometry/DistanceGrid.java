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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.*;
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
import maspack.util.DoubleHolder;
import maspack.util.ArraySupport;
import maspack.util.QuadraticSolver;
import maspack.util.InternalErrorException;

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
   public void getLocalCoordinates(Point3d local, Point3d world) {
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
   public void getWorldCoordinates(Point3d world, Point3d local) {
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
    * Converts a normal to local coordinates
    * @param local populated local normal
    * @param world world normal
    */
   public void getLocalNormal(Vector3d local, Vector3d world) {
      if (local != world) {
         local.set(world);
      }
      if (myWorldTransform != null) {
         myWorldTransform.getMatrix().mulTranspose(local);
      }
   }

   /**
    * Creates a new distance grid for a specified set of features. The feature is
    * created as for {@link
    * #DistanceGrid(Feature[],double,Vector3i)},
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

   private void enforceEvenResolution (Vector3i res) {
      if ((res.x%2) == 1) {
         res.x++;
      }
      if ((res.y%2) == 1) {
         res.y++;
      }
      if ((res.z%2) == 1) {
         res.z++;
      }
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
      Vector3i resolution = new Vector3i (
         (int)(Math.ceil(widths.x/cwidth)),
         (int)(Math.ceil(widths.y/cwidth)),
         (int)(Math.ceil(widths.y/cwidth)));
      enforceEvenResolution (resolution);
      numVX = resolution.x+1;
      numVY = resolution.y+1;
      numVZ = resolution.z+1;
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

      enforceEvenResolution (resolution);      
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

   public DistanceGrid (
      Vector3d widths, Vector3i resolution) {

      this();

      enforceEvenResolution (resolution);

      myCellWidths = new Vector3d ();
      myCellWidths.x = widths.x / resolution.x;
      myCellWidths.y = widths.y / resolution.y;
      myCellWidths.z = widths.z / resolution.z;

      numVX = resolution.x+1;
      numVY = resolution.y+1;
      numVZ = resolution.z+1;
      numVXxVY = numVX*numVY;      

      // adjust max and min coords
      myMaxCoord.scale ( 0.5, widths);
      myMinCoord.scale (-0.5, widths);

      myPhi = new double[numVX*numVY*numVZ];
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
    * @param grad returns the gradient (in world coordinates)
    * @param x x coordinate of point (in world coordinates).
    * @param y y coordinate of point (in world coordinates).
    * @param z z coordinate of point (in world coordinates).
    * @return distance to the nearest feature
    */
   public double getWorldDistanceAndGradient (
      Vector3d grad, double x, double y, double z) {
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
      
      Vector3d cpos = new Vector3d();
      Vector3i vidx = new Vector3i();
      if (getCellCoords (vidx, cpos, point) == null) {
         return null;
      }
      Point3d tmp = new Point3d();
      double dmin = Double.POSITIVE_INFINITY;
      Feature nf = null;
      
      int[] coords = {vidx.x, vidx.y, vidx.z, 
                    vidx.x+1, vidx.y, vidx.z,
                    vidx.x, vidx.y+1, vidx.z,
                    vidx.x, vidx.y, vidx.z+1,
                    vidx.x+1, vidx.y+1, vidx.z,
                    vidx.x+1, vidx.y, vidx.z+1,
                    vidx.x, vidx.y+1, vidx.z+1,
                    vidx.x+1, vidx.y+1, vidx.z+1};
      
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
   
   private void clipToGrid (Vector3d p) {
      if (p.x < myMinCoord.x) {
         p.x = 0;
      }
      else if (p.x > myMaxCoord.x) {
         p.x = myMaxCoord.x;
      }
      if (p.y < myMinCoord.y) {
         p.y = 0;
      }
      else if (p.y > myMaxCoord.y) {
         p.y = myMaxCoord.y;
      }
      if (p.z < myMinCoord.z) {
         p.z = 0;
      }
      else if (p.z > myMaxCoord.z) {
         p.z = myMaxCoord.z;
      }
   }      

   private boolean isOutside (Vector3d p) {
      return (p.x < myMinCoord.x || p.x > myMaxCoord.x ||
              p.y < myMinCoord.y || p.y > myMaxCoord.y ||
              p.z < myMinCoord.z || p.z > myMaxCoord.z);
   }      

   protected Vector3i getCellCoords (
      Vector3i xyzi, Vector3d coords, Point3d point) {
      if (isOutside (point)) {
         return null;
      }
      double tempPointX = (point.x - myMinCoord.x) / myCellWidths.x;
      double tempPointY = (point.y - myMinCoord.y) / myCellWidths.y;
      double tempPointZ = (point.z - myMinCoord.z) / myCellWidths.z;

      if (xyzi == null) {
         xyzi = new Vector3i();
      }
      xyzi.x = (int)tempPointX;
      xyzi.y = (int)tempPointY;
      xyzi.z = (int)tempPointZ;

      if (tempPointX == numVX - 1) {
         xyzi.x -= 1;
      }
      if (tempPointY == numVY - 1) {
         xyzi.y -= 1;
      }
      if (tempPointZ == numVZ - 1) {
         xyzi.z -= 1;
      }
      if (coords != null) {
         coords.x = tempPointX - xyzi.x;
         coords.y = tempPointY - xyzi.y;
         coords.z = tempPointZ - xyzi.z;
      }
      return xyzi;
   }

   /**
    * Returns the x, y, z indices of the minimum vertex of the cell containing
    * <code>point</code>. If <code>point</code> is outside the grid,
    * <code>null</code> is returned.
    *
    * @param xyzi returns the x, y, z indices. If specified as
    * <code>null</code>, then the containing vector is allocated internally.
    * @param point point for which the cell vertex is desired
    * @return vector containing the cell vertex indices, or
    * <code>null</code> if <code>point</code> is outside the grid.
    */
   public Vector3i getCellVertex (Vector3i xyzi, Point3d point) {
      return getCellCoords (xyzi, null, point);
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

      Vector3d coords = new Vector3d();
      Vector3i vidx = new Vector3i();
      if (getCellCoords (vidx, coords, point) == null) {
         return OUTSIDE;
      }
      double dx = coords.x;
      double dy = coords.y;
      double dz = coords.z;

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

      double d000  = getVertexDistance (vidx.x  , vidx.y  , vidx.z  );
      double d001  = getVertexDistance (vidx.x  , vidx.y  , vidx.z+1);
      double d010  = getVertexDistance (vidx.x  , vidx.y+1, vidx.z  );
      double d011  = getVertexDistance (vidx.x  , vidx.y+1, vidx.z+1);
      double d100  = getVertexDistance (vidx.x+1, vidx.y  , vidx.z  );
      double d101  = getVertexDistance (vidx.x+1, vidx.y  , vidx.z+1);
      double d110  = getVertexDistance (vidx.x+1, vidx.y+1, vidx.z  );
      double d111  = getVertexDistance (vidx.x+1, vidx.y+1, vidx.z+1);

      double d =
         w000*d000 + w001*d001 + w010*d010 + w011*d011 +
         w100*d100 + w101*d101 + w110*d110 + w111*d111;
      
      if (norm != null || Dnrm != null) {
         Vector3d n000 = getLocalVertexNormal (vidx.x  , vidx.y  , vidx.z  );
         Vector3d n001 = getLocalVertexNormal (vidx.x  , vidx.y  , vidx.z+1);
         Vector3d n010 = getLocalVertexNormal (vidx.x  , vidx.y+1, vidx.z  );
         Vector3d n011 = getLocalVertexNormal (vidx.x  , vidx.y+1, vidx.z+1);
         Vector3d n100 = getLocalVertexNormal (vidx.x+1, vidx.y  , vidx.z  );
         Vector3d n101 = getLocalVertexNormal (vidx.x+1, vidx.y  , vidx.z+1);
         Vector3d n110 = getLocalVertexNormal (vidx.x+1, vidx.y+1, vidx.z  );
         Vector3d n111 = getLocalVertexNormal (vidx.x+1, vidx.y+1, vidx.z+1);

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

      Vector3d coords = new Vector3d();
      Vector3i vidx = new Vector3i();
      if (getCellCoords (vidx, coords, point) == null) {
         return OUTSIDE;
      }
      double dx = coords.x;
      double dy = coords.y;
      double dz = coords.z;

      double w100x = (1-dy)*(1-dz);
      double w101x = (1-dy)*dz;
      double w110x = dy*(1-dz);
      double w111x = dy*dz;

      double w010y = (1-dx)*(1-dz);
      double w011y = (1-dx)*dz;
      double w110y = dx*(1-dz);
      double w111y = dx*dz;

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
      
      double d000  = getVertexDistance (vidx.x  , vidx.y  , vidx.z  );
      double d001  = getVertexDistance (vidx.x  , vidx.y  , vidx.z+1);
      double d010  = getVertexDistance (vidx.x  , vidx.y+1, vidx.z  );
      double d011  = getVertexDistance (vidx.x  , vidx.y+1, vidx.z+1);
      double d100  = getVertexDistance (vidx.x+1, vidx.y  , vidx.z  );
      double d101  = getVertexDistance (vidx.x+1, vidx.y  , vidx.z+1);
      double d110  = getVertexDistance (vidx.x+1, vidx.y+1, vidx.z  );
      double d111  = getVertexDistance (vidx.x+1, vidx.y+1, vidx.z+1);

      if (grad != null) {
         grad.x = (-w100x*d000 - w101x*d001 - w110x*d010 - w111x*d011
                   +w100x*d100 + w101x*d101 + w110x*d110 + w111x*d111);
         grad.y = (-w010y*d000 - w011y*d001 + w010y*d010 + w011y*d011
                   -w110y*d100 - w111y*d101 + w110y*d110 + w111y*d111);
         grad.z = (-w001z*d000 + w001z*d001 - w011z*d010 + w011z*d011
                   -w101z*d100 + w101z*d101 - w111z*d110 + w111z*d111);
         
         grad.x /= myCellWidths.x;
         grad.y /= myCellWidths.y;
         grad.z /= myCellWidths.z;
      }

      return (w000*d000 + w001*d001 + w010*d010 + w011*d011 +
              w100*d100 + w101*d101 + w110*d110 + w111*d111);
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

   /**
    * Find the world coordinates at a vertex, as specified by
    * its x, y, z indices.
    * 
    * @param coords returns the coordinates
    * @param vxyz x, y, z vertex indices
    * @return coords
    */
   public Vector3d getWorldVertexCoords (Vector3d coords, Vector3i vxyz) {
      Point3d pnt = new Point3d();
      getLocalVertexCoords (pnt, vxyz);
      getWorldCoordinates (pnt, pnt);
      coords.set(pnt);
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

   public PolygonalMesh computeQuadraticDistanceSurface (double val, int res) {
      MarchingTetrahedra marcher = new MarchingTetrahedra();

      // sample at a 3 X higher resolution so we can get
      // a better sense of the smooth surface
      Vector3i cellRes = new Vector3i(getResolution());
      cellRes.scale (res);

      int numVX = cellRes.x+1;
      int numVY = cellRes.y+1;
      int numVZ = cellRes.z+1;
      double[] dists = new double[numVX*numVY*numVZ];
      Vector3d cellWidths = new Vector3d(myCellWidths);
      cellWidths.scale (1.0/res);

      Point3d q = new Point3d();
      for (int i=0; i<numVX; i++) {
         for (int j=0; j<numVY; j++) {
            for (int k=0; k<numVZ; k++) {
               q.set (i*cellWidths.x, j*cellWidths.y, k*cellWidths.z);
               q.add (myMinCoord);
               clipToGrid (q);
               dists[i + j*numVX + k*numVX*numVY] =
                  getQuadraticDistanceAndGradient (null, q);
            }
         }
      }
      return marcher.createMesh (
         dists, myMinCoord, cellWidths, cellRes, val);
   }

   public AffineTransform3dBase getWorldTransform() {
      return myWorldTransform;
   }
   
   public double getRadius() {
      return myMaxCoord.distance(myMinCoord)/2;
   }


   // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   // 
   // All code below this point is for getQuadraticDistanceAndGradient() and
   // findQuadraticSurfaceTangent(), which is used in the implementation of
   // strand wrapping. Still very messy and under development. Keep out!
   //
   // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX


   public enum PlaneType {
      YZ,
      ZX,
      XY
   };

   protected HashMap<TetFace,TetDesc> myFaceTets;
   protected HashMap<TetEdge,TetDesc[]> myEdgeTets;
   protected ArrayList<TetDesc[]> myNodeTets;

   protected int[] myTetOffsets0156;
   protected int[] myTetOffsets0456;
   protected int[] myTetOffsets0476;
   protected int[] myTetOffsets0126;
   protected int[] myTetOffsets0326;
   protected int[] myTetOffsets0376;
   protected int[] myQuadHexOffsets;
   protected int[] myTetFaceOffsets;

   /**
    * Identifies a sub tet within a hex cell. The numbers identify the
    * corresponding hex nodes.
    */
   public enum TetID {
      TET0516,
      TET0456,
      TET0746,
      TET0126,
      TET0236,
      TET0376;

      public int intValue() {
         switch (this) {
            case TET0516: {
               return 0;
            }
            case TET0456: {
               return 1;
            }
            case TET0746: {
               return 2;
            }
            case TET0126: {
               return 3;
            }
            case TET0236: {
               return 4;
            }
            case TET0376: {
               return 5;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented tet " + this);
            }
         }
      }

      public static TetID fromInt (int num) {
         switch (num) {
            case 0: return TET0516;
            case 1: return TET0456;
            case 2: return TET0746;
            case 3: return TET0126;
            case 4: return TET0236;
            case 5: return TET0376;
            default: {
               throw new InternalErrorException (
                  "num=" + num + ", must be in the range [0-5]");
            }
         }
      }

      public int[] getNodes() {
         switch (this) {
            case TET0516: return new int[] { 0, 5, 1, 6 }; 
            case TET0126: return new int[] { 0, 1, 2, 6 }; 
            case TET0236: return new int[] { 0, 2, 3, 6 }; 
            case TET0376: return new int[] { 0, 3, 7, 6 }; 
            case TET0746: return new int[] { 0, 7, 4, 6 }; 
            case TET0456: return new int[] { 0, 4, 5, 6 }; 
            default: {
               throw new InternalErrorException (
                  "Unimplemented tet " + this);
            }
         }
      }

      public int[] getMiddleNodeOffsets() {
         switch (this) {
            case TET0516: return new int[] { 2, 2, 0, 2, 0, 0 }; 
            case TET0126: return new int[] { 2, 0, 0, 2, 0, 2 };
            case TET0236: return new int[] { 2, 0, 2, 0, 0, 2 };
            case TET0376: return new int[] { 0, 0, 2, 0, 2, 2 };
            case TET0746: return new int[] { 0, 2, 2, 0, 2, 0 };
            case TET0456: return new int[] { 0, 2, 0, 2, 2, 0 };
            default: {
               throw new InternalErrorException (
                  "Unimplemented tet " + this);
            }
         }
      }


      boolean isInside (double x, double y, double z) {
         if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1) {
            return false;
         }
         else {
            return findSubTet (x, y, z) == this;
         }
      }

      boolean isInside (Point3d p) {
         if (p.x < 0 || p.x > 1 || p.y < 0 || p.y > 1 || p.z < 0 || p.z > 1) {
            return false;
         }
         else {
            return findSubTet (p.x, p.y, p.z) == this;
         }
      }

      /**
       * Finds the sub-tet within a hex cell, based on the x, y, z coordinates of
       * a point within the cell. These coordinates are assumed to be normalized
       * to [0,1] to correspond to the cell dimensions.
       */
      static TetID findSubTet (double x, double y, double z) {
         if (y >= z) {
            if (x >= z) {
               if (x >= y) {
                  return TetID.TET0516;
               }
               else {
                  return TetID.TET0456;
               }
            }
            else {
               return TetID.TET0746;
            }
         }
         else {
            if (x >= y) {
               if (x >= z) {
                  return TetID.TET0126;
               }
               else {
                  return TetID.TET0236;
               }
            }
            else {
               return TetID.TET0376;
            }
         }
      }  
      
   };

   public int[] getTetVertices (TetID tetId, int xi, int yj, int zk) {
      createTetOffsetsIfNecessary();

      int[] vertices = new int[4];
      int vbase = xyzIndicesToVertex (xi, yj, zk);
      int[] nodes = tetId.getNodes();
      for (int i=0; i<nodes.length; i++) {
         vertices[i] = vbase + myQuadHexOffsets[nodes[i]];
      }
      return vertices;
   }

   protected boolean isInsideTet (TetID tetId, double x, double y, double z) {
      if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1) {
         return false;
      }
      else {
         return findSubTet (x, y, z) == tetId;
      }
   }

   /**
    * Finds the sub-tet within a hex cell, based on the x, y, z coordinates of
    * a point within the cell. These coordinates are assumed to be normalized
    * to [0,1] to correspond to the cell dimensions.
    */
   protected TetID findSubTet (double x, double y, double z) {
      if (y >= z) {
         if (x >= z) {
            if (x >= y) {
               return TetID.TET0516;
            }
            else {
               return TetID.TET0456;
            }
         }
         else {
            return TetID.TET0746;
         }
      }
      else {
         if (x >= y) {
            if (x >= z) {
               return TetID.TET0126;
            }
            else {
               return TetID.TET0236;
            }
         }
         else {
            return TetID.TET0376;
         }
      }
   }  

   public static class TetFeature {
   }

   public static class TetNode extends TetFeature {
      int myNode0;

      public TetNode (int node0) {
         myNode0 = node0;
      }

      public int getNode() {
         return myNode0;
      }  

      public String toString() {
         return ""+myNode0;
      }
   }

   public static class TetEdge extends TetFeature {
      int myNode0;
      int myNode1;

      TetEdge (int node0, int node1) {
         if (node0 > node1) {
            myNode0 = node1;
            myNode1 = node0;
         }
         else {
            myNode0 = node0;
            myNode1 = node1;
         }
      }

      public boolean equals (Object obj) {
         if (obj instanceof TetEdge) {
            TetEdge edge = (TetEdge)obj;
            return edge.myNode0 == myNode0 && edge.myNode1 == myNode1;
         }
         else {
            return false;
         }
      }

      public int hashCode() {
         return myNode0 + 11*myNode1;
      }

      public String toString() {
         return "" + myNode0 + "-" + myNode1;
      }
   }

   private static class TetFace extends TetFeature {
      int myNode0;
      int myNode1;
      int myNode2;

      TetFace (int node0, int node1, int node2) {
         Vector3i nodes = new Vector3i (node0, node1, node2);
         int minIdx = nodes.minAbsIndex();
         myNode0 = nodes.get(minIdx);
         myNode1 = nodes.get((minIdx+1)%3);
         myNode2 = nodes.get((minIdx+2)%3);
      }

      public boolean equals (Object obj) {
         if (obj instanceof TetFace) {
            TetFace edge = (TetFace)obj;
            return (edge.myNode0 == myNode0 &&
                    edge.myNode1 == myNode1 &&
                    edge.myNode2 == myNode2);
         }
         else {
            return false;
         }
      }

      public int hashCode() {
         return myNode0 + 11*myNode1 + 71*myNode2;
      }

      public String toString() {
         return "" + myNode0 + "-" + myNode1 + "-" + myNode2;
      }
   }

   int findRefVertex (int vidx) {
      for (int i=0; i<myQuadHexOffsets.length; i++) {
         if (vidx == myQuadHexOffsets[i]) {
            return i;
         }
      }
      return -1;
   }

   protected Vector3i getQuadCellCoords (
      Vector3i xyzi, Vector3d coords, Point3d point) {

      if ((numVX-1)%2 != 0 || (numVY-1)%2 != 0 || (numVZ-1)%2 != 0) {
         throw new IllegalStateException (
            "Grid must have an even number of cells along all dimensions");
      }
      xyzi = getCellCoords (xyzi, coords, point);
      if (xyzi == null) {
         return null;
      }
      // round down to even vertices
      if ((xyzi.x%2) != 0) {
         xyzi.x--;
         if (coords != null) coords.x += 1;
      }
      if ((xyzi.y%2) != 0) {
         xyzi.y--;
         if (coords != null) coords.y += 1;
      }
      if ((xyzi.z%2) != 0) {
         xyzi.z--;
         if (coords != null) coords.z += 1;
      }      
      // and scale coords by half 
      if (coords != null) {
         coords.scale (0.5);
      }
      return xyzi;
   }

   protected Vector3i createEdgeNode (Vector3i v0, Vector3i v1) {
      Vector3i en = new Vector3i();
      en.x = (v0.x+v1.x)/2;
      en.y = (v0.y+v1.y)/2;
      en.z = (v0.z+v1.z)/2;
      return en;
   }

   protected int[] createTetOffsets (
      Vector3i v0, Vector3i v1, Vector3i v2, Vector3i v3) {

      Vector3i e01 = createEdgeNode (v0, v1);
      Vector3i e12 = createEdgeNode (v1, v2);
      Vector3i e23 = createEdgeNode (v2, v3);
      Vector3i e02 = createEdgeNode (v0, v2);
      Vector3i e13 = createEdgeNode (v1, v3);
      Vector3i e03 = createEdgeNode (v0, v3);

      Vector3i[] nodes = new Vector3i[] {
         v0, v1, v2, v3, e01, e12, e23, e02, e13, e03 };
      int[] offsets = new int[nodes.length];
      for (int i=0; i<nodes.length; i++) {
         offsets[i] = xyzIndicesToVertex (nodes[i]);
      }
      return offsets;
   }

   protected void createTetOffsetsIfNecessary() {
      if (myTetOffsets0156 == null) {
         Vector3i v0 = new Vector3i (0, 0, 0);
         Vector3i v1 = new Vector3i (2, 0, 0);
         Vector3i v2 = new Vector3i (2, 0, 2);
         Vector3i v3 = new Vector3i (0, 0, 2);
         
         Vector3i v4 = new Vector3i (0, 2, 0);
         Vector3i v5 = new Vector3i (2, 2, 0);
         Vector3i v6 = new Vector3i (2, 2, 2);
         Vector3i v7 = new Vector3i (0, 2, 2);
         
         myTetOffsets0156 = createTetOffsets (v0, v1, v5, v6);
         myTetOffsets0126 = createTetOffsets (v0, v1, v2, v6);
         myTetOffsets0326 = createTetOffsets (v0, v3, v2, v6);
         myTetOffsets0376 = createTetOffsets (v0, v3, v7, v6);
         myTetOffsets0476 = createTetOffsets (v0, v4, v7, v6);
         myTetOffsets0456 = createTetOffsets (v0, v4, v5, v6);

         Vector3i[] nodes = new Vector3i[] {v0, v1, v2, v3, v4, v5, v6, v7};
         myQuadHexOffsets = new int[8];
         for (int i=0; i<nodes.length; i++) {
            myQuadHexOffsets[i] = xyzIndicesToVertex (nodes[i]);
         }
         
         //createTetFaceOffsets();
         createConnectivity();
      }
   }

   void maybeAddFace (
      HashMap<TetFace,TetDesc> faceTets,
      int vi0, int vi1, int vi2, TetDesc tdesc) {
      
      int ref0, ref1, ref2;
      if ((ref0=findRefVertex (vi0)) != -1 &&
          (ref1=findRefVertex (vi1)) != -1 &&
          (ref2=findRefVertex (vi2)) != -1) {
         TetFace tface = new TetFace (ref0, ref2, ref1);
         if (faceTets.get (tdesc) != null) {
            System.out.println (
               "Error: face "+tface+" already assigned tet " +
               faceTets.get (tface));
         }
         faceTets.put (tface, tdesc);
      }
   }

   void maybeAddEdge (
      HashMap<TetEdge,HashSet<TetDesc>> edgeTets,
      int vi0, int vi1, TetDesc tdesc) {
      
      int ref0, ref1;
      if ((ref0=findRefVertex (vi0)) != -1 &&
          (ref1=findRefVertex (vi1)) != -1) {
         TetEdge tedge = new TetEdge (ref0, ref1);
         HashSet<TetDesc> tets = edgeTets.get(tedge);
         if (tets == null) {
            tets = new HashSet<TetDesc>();
            edgeTets.put (tedge, tets);
         }
         if (tets.contains(tdesc)) {
            System.out.println (
               "Error: edge "+tedge+" already assigned tet " + tdesc);
         }
         tets.add (tdesc);
      }
   }

   private void createConnectivity () {
      createTetOffsetsIfNecessary();

      myFaceTets = new HashMap<TetFace,TetDesc>();

      ArrayList<HashSet<TetDesc>> nodeTets = new ArrayList<HashSet<TetDesc>>();
      for (int i=0; i<8; i++) {
         nodeTets.add (new HashSet<TetDesc>());
      }
      HashMap<TetEdge,HashSet<TetDesc>> edgeTets =
         new HashMap<TetEdge,HashSet<TetDesc>>();

      for (int i=-1; i<=1; i++) {
         for (int j=-1; j<=1; j++) {
            for (int k=-1; k<=1; k++) {
               for (TetID tetId : TetID.values()) {
                  TetDesc tdesc =
                     new TetDesc (new Vector3i (2*i, 2*j, 2*k), tetId);
                  int[] verts = tdesc.getVertices ();
                  for (int vi=0; vi<verts.length; vi++) {
                     int refVtx = findRefVertex (verts[vi]);
                     if (refVtx != -1) {
                        nodeTets.get(refVtx).add (tdesc);
                     }
                  }
                  maybeAddFace (myFaceTets, verts[0], verts[1], verts[2], tdesc);
                  maybeAddFace (myFaceTets, verts[0], verts[2], verts[3], tdesc);
                  maybeAddFace (myFaceTets, verts[2], verts[1], verts[3], tdesc);
                  maybeAddFace (myFaceTets, verts[1], verts[0], verts[3], tdesc);

                  maybeAddEdge (edgeTets, verts[0], verts[1], tdesc);
                  maybeAddEdge (edgeTets, verts[1], verts[2], tdesc);
                  maybeAddEdge (edgeTets, verts[2], verts[0], tdesc);
                  maybeAddEdge (edgeTets, verts[0], verts[3], tdesc);
                  maybeAddEdge (edgeTets, verts[1], verts[3], tdesc);
                  maybeAddEdge (edgeTets, verts[2], verts[3], tdesc);
               }
            }
         }
      }

      myNodeTets = new ArrayList<TetDesc[]>();
      for (int i=0; i<nodeTets.size(); i++) {
         myNodeTets.add (nodeTets.get(i).toArray(new TetDesc[0]));
      }

      myEdgeTets = new HashMap<TetEdge,TetDesc[]>();
      for (TetEdge edge : edgeTets.keySet()) {
         myEdgeTets.put (edge, edgeTets.get(edge).toArray(new TetDesc[0]));
      }
   }

   void printConnectivity() {
      
      System.out.println ("Nodes:");
      for (int i=0; i<8; i++) {
         TetDesc[] tdescs = myNodeTets.get(i);
         System.out.print (" "+i+"("+tdescs.length+") : ");
         for (TetDesc tdesc : tdescs) {
            System.out.print (tdesc + " ");
         }
         System.out.println ("");
      }

      System.out.println ("Edges:");
      for (TetEdge edge : myEdgeTets.keySet()) {
         System.out.print (" "+edge+": ");
         for (TetDesc tdesc : myEdgeTets.get(edge)) {
            System.out.print (tdesc + " ");
         }
         System.out.println ("");
      }
      
      System.out.println ("Faces:");
      for (TetFace face : myFaceTets.keySet()) {
         System.out.println (
            " "+face+": " + myFaceTets.get(face));
      }
   }         

   /** 
    */
   public double getQuadraticDistanceAndGradient (Vector3d grad, Point3d point) {
      // Change to grid coordinates
      if ((numVX-1)%2 != 0 || (numVY-1)%2 != 0 || (numVZ-1)%2 != 0) {
         throw new IllegalStateException (
            "Grid must have an even number of cells along all dimensions");
      }
      Vector3d coords = new Vector3d();
      Vector3i vidx = new Vector3i();
      if (getQuadCellCoords (vidx, coords, point) == null) {
         return OUTSIDE;
      }
      double dx = coords.x;
      double dy = coords.y;
      double dz = coords.z;

      double[] c = new double[10];
      TetDesc tdesc = new TetDesc (vidx, TetID.findSubTet (dx, dy, dz));
      tdesc.computeQuadCoefs (c);
      if (grad != null) {
         tdesc.computeGradient (grad, c, dx, dy, dz);
      }
      return tdesc.computeDistance (c, dx, dy, dz);
   }
   
   public boolean findQuadraticSurfaceTangent (
      Point3d pt, Point3d p0, Point3d pa, Vector3d nrm) {

      // transform p0, pa and nrm into grid local coordinates
      Point3d p0Loc = new Point3d();
      getLocalCoordinates (p0Loc, p0);
      Point3d paLoc = new Point3d();
      getLocalCoordinates (paLoc, pa);
      Vector3d nrmLoc = new Vector3d(nrm);
      getLocalNormal (nrmLoc, nrm);
      nrmLoc.normalize();

      // System.out.println (
      //    "findSurfaceTangent nrmLoc=" + nrmLoc.toString("%8.3f"));

      // generate a plane from the normal and p0
      Plane planeLoc = new Plane (nrmLoc, p0Loc);

      // Dynamic array to store tet/plane intersections
      ArrayList<TetPlaneIntersection> isects =
         new ArrayList<TetPlaneIntersection>();
      isects.add (new TetPlaneIntersection()); // will need at least one

      // temporary storage for computed tet vertex coordinates
      Point3d[] vpnts = new Point3d[] {
         new Point3d(), new Point3d(), new Point3d(), new Point3d() };      

      int icnt = 0;
      int maxIters = 2;
      int code = NONE;
      TetDesc tdesc = null;
      TanSearchInfo sinfo = new TanSearchInfo();
      int sgnDotGrad = 0;
      Point3d ps = new Point3d(p0Loc);

      Vector3d grad = new Vector3d();
      double d = getQuadraticDistanceAndGradient (grad, p0Loc);
      if (d == OUTSIDE) {
         // shouldn't happen - p0 should be inside by definition
         return false;
      }
      grad.scaledAdd (-grad.dot(nrmLoc), nrmLoc); // project grad into plane
      grad.normalize();
      
      Vector3i vxyz = new Vector3i();
      Vector3d xyz = new Vector3d();
      if (getQuadCellCoords (vxyz, xyz, p0Loc) == null) {
         // shouldn't happen - grid should have enough margin to prevent this
         return false;
      }
      tdesc =
         new TetDesc (vxyz, TetID.findSubTet (xyz.x, xyz.y, xyz.z));      
      tdesc.getVertexCoords (vpnts);
      TetPlaneIntersection isect = isects.get(0);
      if (!intersectTetAndPlane (isect, tdesc, vpnts, planeLoc)) {
         // shouldn't happen - p0 should be on the plane
         return false;
      }

      PlaneType planeType = null;
      Vector3d nrmCell = new Vector3d();
      transformNormalToCell (nrmCell, planeLoc.normal);
      switch (nrmCell.maxAbsIndex()) {
         case 0: planeType = PlaneType.YZ; break;
         case 1: planeType = PlaneType.ZX; break;
         case 2: planeType = PlaneType.XY; break;
      }
      int planeSgn = (nrmCell.get(nrmCell.maxAbsIndex()) > 0 ? 1 : -1);

      double[] srng = new double[] { -Double.MAX_VALUE, Double.MAX_VALUE };
      DoubleHolder edgeS = new DoubleHolder();
      int edgeIdx = isect.intersectRay (
         srng, edgeS, p0Loc, grad, planeType, planeSgn);
      if (edgeIdx == -1) {
         // shouldn't happen - p0 should be in the tet
         return false;
      }
      TetFeature lastFeat = isect.getFeature (edgeS.value, edgeIdx);

      HashSet<TetDesc> visitedTets = new HashSet<TetDesc>();
      visitedTets.add (tdesc);

      while (findSurfaceIntersectionInTet (
                ps, isect, srng, p0Loc, grad, planeLoc) == CONTINUE) {
          int ntets = getFeatureAdjacentTets (
             isects, tdesc, lastFeat, planeLoc, visitedTets);
          TetPlaneIntersection ibest = null;
          double bestLen = 0;
          int bestEdgeIdx = -1;
          double bestS = -1;          
          for (int k=0; k<ntets; k++) {
             isect = isects.get(k);
             double[] irng =
                new double[] { -Double.MAX_VALUE, Double.MAX_VALUE };
             edgeIdx = isect.intersectRay (
                irng, edgeS, p0Loc, grad, planeType, planeSgn);
             if (edgeIdx != -1) {
                double ilen = irng[1]-irng[0];
                if (ilen > bestLen) {
                   bestLen = ilen;
                   ibest = isect;
                   bestEdgeIdx = edgeIdx;
                   bestS = edgeS.value;
                   srng[0] = irng[0];
                   srng[1] = irng[1];
                }
             }
          }
          if (bestEdgeIdx == -1) {
             return false;
          }
          else {
             lastFeat = ibest.getFeature (bestS, bestEdgeIdx);
             isect = ibest;
             tdesc = isect.myTetDesc;
             visitedTets.add (tdesc);
          }
      }

      // System.out.println ("  intersection ps=" + ps.toString("%8.5f"));
      // System.out.println ("               p0=" + p0Loc.toString("%8.5f"));

      d = getQuadraticDistanceAndGradient (grad, ps);
      if (d == OUTSIDE) {
         // shouldn't happen - ps should be inside by definition
         return false;
      }
      grad.scaledAdd (-grad.dot(nrmLoc), nrmLoc); // project grad into plane
      Vector3d dela0 = new Vector3d();
      dela0.sub (ps, paLoc);
      sgnDotGrad = (dela0.dot(grad) > 0 ? 1 : -1);       
      
      //System.out.println ("findSurfaceTangent(A) for " + tdesc);
      code = findSurfaceTangent (
         pt, sinfo, isect, null, sgnDotGrad, paLoc, planeLoc);
      if (code == DONE) {
         // tangent found
         getWorldCoordinates (pt, pt);
         //System.out.println ("  found pt=" + pt.toString ("%8.3f"));
         return true;
      }

      visitedTets.clear();
      visitedTets.add (tdesc);

      while (code != NONE) {
         int ntets = getFeatureAdjacentTets (
            isects, tdesc, sinfo.lastFeat, planeLoc, visitedTets);
         TetPlaneIntersection ibest = null;
         //System.out.println ("ntets= " + ntets);
         if (ntets > 1) {
            // find the tet that best intersects the current curve tangent
            double maxLen = -1;
            int maxTet = -1;
            for (int k=0; k<ntets; k++) {
               double len = isects.get(k).tanIntersectOverlap (
                  pt, sinfo.lastGrad, planeType);
               if (len > maxLen) {
                  maxLen = len;
                  maxTet = k;
               }
            }
            ibest = isects.get(maxTet);
         }
         else if (ntets > 0) {
            ibest = isects.get(0);
         }
         else {
            System.out.println ("  Couldn't find tangent");
            code = NONE;
            continue;
         }
         tdesc = ibest.myTetDesc;
         visitedTets.add (tdesc);
         //System.out.println ("findSurfaceTangent(B) for " + tdesc);
         code = findSurfaceTangent (
            pt, sinfo, ibest, pt, sgnDotGrad, paLoc, planeLoc);
         int tidx = 0;
         while (code == NONE && tidx < ntets) {
            isect = isects.get(tidx++);
            tdesc = isect.myTetDesc;
            visitedTets.add (tdesc);         
            code = findSurfaceTangent (
               pt, sinfo, isect, pt, sgnDotGrad, paLoc, planeLoc);
         }
         if (code == DONE) {
            getWorldCoordinates (pt, pt);
            //System.out.println ("  found pt=" + pt.toString ("%8.3f"));
            return true;
         }
      }
      getWorldCoordinates (pt, ps);
      //System.out.println ("  fallback pt=" + pt.toString ("%8.3f"));
      return false;
   }

   protected int findTangentPoints (
      Vector2d[] pnts, double b[], double pax, double pay) {

      double ax = (b[3] + 2*b[0]*pax + b[2]*pay);
      double ay = (b[4] + 2*b[1]*pay + b[2]*pax);
      double ab = (2*b[5] + b[3]*pax + b[4]*pay);

      double[] roots = new double[2];
      int nr;
      if (Math.abs(ax) >= Math.abs(ay)) {
         // x = r0 y + r1,  r0 = -ay/ax, r1 = -ab/ax
         double r0 = -ay/ax;
         double r1 = -ab/ax;
         double aa = b[0]*r0*r0 + b[2]*r0 + b[1];
         double bb = 2*b[0]*r0*r1 + b[2]*r1 + b[3]*r0 + b[4];
         double cc = b[0]*r1*r1 + b[3]*r1 + b[5];
         nr = QuadraticSolver.getRoots (roots, aa, bb, cc);
         for (int i=0; i<nr; i++) {
            double y = roots[i];
            pnts[i].y = y;
            pnts[i].x = r0*y + r1;
         }
      }
      else {
         // y = r0 x + r1,  r0 = -ax/ay, r1 = -ab/ay
         double r0 = -ax/ay;
         double r1 = -ab/ay;
         double aa = b[1]*r0*r0 + b[2]*r0 + b[0];
         double bb = 2*b[1]*r0*r1 + b[2]*r1 + b[4]*r0 + b[3];
         double cc = b[1]*r1*r1 + b[4]*r1 + b[5];
         nr = QuadraticSolver.getRoots (roots, aa, bb, cc);
         for (int i=0; i<nr; i++) {
            double x = roots[i];
            pnts[i].x = x;
            pnts[i].y = r0*x + r1;
         }
      }
      return nr;
   }

   private double computeDist (double[] c, double x, double y, double z) {
      return (c[0]*x*x + c[1]*y*y + c[2]*z*z +
              c[3]*y*z + c[4]*x*z + c[5]*x*y +
              c[6]*x + c[7]*y + c[8]*z + c[9]);
   }

   private double computePerp (
      double[] c, double x, double y, double z, Vector3d p) {

      Vector3d grad = new Vector3d();

      grad.x = 2*c[0]*x + c[4]*z + c[5]*y + c[6];
      grad.y = 2*c[1]*y + c[3]*z + c[5]*x + c[7];
      grad.z = 2*c[2]*z + c[3]*y + c[4]*x + c[8];
      Vector3d diff = new Vector3d(x, y, z);
      diff.sub (p);
      return diff.dot(grad)/(grad.norm()*diff.norm());
   }

   private double computePlanePerp (
      double[] b, double x, double y, double px, double py) {

      Vector2d grad = new Vector2d();

      grad.x = 2*b[0]*x + b[2]*y + b[3];
      grad.y = 2*b[1]*y + b[2]*x + b[4];
      Vector2d diff = new Vector2d(x, y);
      diff.x -= px;
      diff.y -= py;
      return diff.dot(grad)/(grad.norm()*diff.norm());
   }

   private double computePlaneDist (
      double[] b, double x, double y) {

      return b[0]*x*x + b[1]*y*y + b[2]*x*y + b[3]*x + b[4]*y + b[5];
   }

   public class TetDesc {

      int myXi;
      int myYj;
      int myZk;
      int myVoff;
      TetID myTetId;
      
      public TetDesc (Vector3i vxyz, TetID tetId) {
         this (vxyz.x, vxyz.y, vxyz.z, tetId);
      }

      public TetDesc (TetDesc tdesc) {
         myXi = tdesc.myXi;
         myYj = tdesc.myYj;
         myZk = tdesc.myZk;
         myVoff = tdesc.myVoff;
         myTetId = tdesc.myTetId;
      }

      public TetDesc (int xi, int yj, int zk, TetID tetId) {
         myXi = xi;
         myYj = yj;
         myZk = zk;
         myTetId = tetId;
         myVoff = xyzIndicesToVertex (xi, yj, zk);
      }

      public TetDesc (int tnum) {
         myVoff = (tnum/6);
         int num = tnum%6;
         if (num < 0) {
            myVoff -= 1;
            num += 6;
         }
         myTetId = TetID.fromInt (num);
         Vector3i vxyz = new Vector3i();
         vertexToXyzIndices (vxyz, myVoff);
         myXi = vxyz.x;
         myYj = vxyz.y;
         myZk = vxyz.z;
      }

      public void addOffset (TetDesc tdesc) {
         myXi += tdesc.myXi;
         myYj += tdesc.myYj;
         myZk += tdesc.myZk;
         myVoff += tdesc.myVoff;
      }

      public boolean inRange () {
         return (myXi >= 0 && myXi < numVX-2 &&
                 myYj >= 0 && myYj < numVY-2 &&
                 myZk >= 0 && myZk < numVZ-2);
      }

      public int getNumber() {
         return 6*myVoff + myTetId.intValue();
      }

      public boolean equals (Object obj) {
         if (obj instanceof TetDesc) {
            TetDesc tdesc = (TetDesc)obj;
            return (myVoff == tdesc.myVoff && myTetId == tdesc.myTetId);
         }
         else {
            return false;
         }
      }

      public void transformToQuadCell (Point3d pc, Point3d pl) {
         Point3d pbase = new Point3d();
         getLocalVertexCoords (pbase, myXi, myYj, myZk);

         pc.sub (pl, pbase);
         pc.x /= (2*myCellWidths.x);
         pc.y /= (2*myCellWidths.y);
         pc.z /= (2*myCellWidths.z);
      }

      public void transformToQuadCell (Vector3d vc, Vector3d v1) {
         vc.x = v1.x / (2*myCellWidths.x);
         vc.y = v1.y / (2*myCellWidths.y);
         vc.z = v1.z / (2*myCellWidths.z);
      }

      public void transformFromQuadCell (
         Point3d pl, double x, double y, double z) {

         Point3d pbase = new Point3d();
         getLocalVertexCoords (pbase, myXi, myYj, myZk);
         pl.x = x * (2*myCellWidths.x);
         pl.y = y * (2*myCellWidths.y);
         pl.z = z * (2*myCellWidths.z);
         pl.add (pbase);
      }

      public void transformFromQuadCell (
         Point3d pl, Point3d pc) {

         Point3d pbase = new Point3d();
         getLocalVertexCoords (pbase, myXi, myYj, myZk);
         pl.x = pc.x * (2*myCellWidths.x);
         pl.y = pc.y * (2*myCellWidths.y);
         pl.z = pc.z * (2*myCellWidths.z);
         pl.add (pbase);
      }

      public void transformPlaneToCell (Plane pc, Plane pl) {
         if (pc != pl) {
            pc.set (pl);
         }
         Point3d pbase = new Point3d();
         getLocalVertexCoords (pbase, myXi, myYj, myZk);
         pc.offset -= pc.normal.dot (pbase);
         pc.normal.x *= (2*myCellWidths.x);
         pc.normal.y *= (2*myCellWidths.y);
         pc.normal.z *= (2*myCellWidths.z);
         // normalize
         double mag = pc.normal.norm();
         pc.normal.scale (1/mag);
         pc.offset /= mag;         
      }

      public int hashCode() {
         return 6*myVoff + myTetId.intValue();
      }

      public void getVertexCoords (Point3d[] v) {
         v[0].x = myXi * myCellWidths.x + myMinCoord.x;
         v[0].y = myYj * myCellWidths.y + myMinCoord.y;
         v[0].z = myZk * myCellWidths.z + myMinCoord.z;
         
         int[] offs = myTetId.getMiddleNodeOffsets();

         v[1].x = (myXi+offs[0]) * myCellWidths.x + myMinCoord.x;
         v[1].y = (myYj+offs[1]) * myCellWidths.y + myMinCoord.y;
         v[1].z = (myZk+offs[2]) * myCellWidths.z + myMinCoord.z;

         v[2].x = (myXi+offs[3]) * myCellWidths.x + myMinCoord.x;
         v[2].y = (myYj+offs[4]) * myCellWidths.y + myMinCoord.y;
         v[2].z = (myZk+offs[5]) * myCellWidths.z + myMinCoord.z;

         v[3].x = (myXi+2) * myCellWidths.x + myMinCoord.x;
         v[3].y = (myYj+2) * myCellWidths.y + myMinCoord.y;
         v[3].z = (myZk+2) * myCellWidths.z + myMinCoord.z;
      }

      private void computeQuadCoefs (
         double[] c, int[] nodeOffs, int xi, int yi, int zi) {
      
         double v0  = myPhi[myVoff+nodeOffs[0]];
         double v1  = myPhi[myVoff+nodeOffs[1]];
         double v2  = myPhi[myVoff+nodeOffs[2]];
         double v3  = myPhi[myVoff+nodeOffs[3]];
         double v4  = myPhi[myVoff+nodeOffs[4]];
         double v5  = myPhi[myVoff+nodeOffs[5]];
         double v6  = myPhi[myVoff+nodeOffs[6]];
         double v7  = myPhi[myVoff+nodeOffs[7]];
         double v8  = myPhi[myVoff+nodeOffs[8]];
         double v9  = myPhi[myVoff+nodeOffs[9]];

         c[xi] =  2*v0 + 2*v1 - 4*v4;
         c[yi] =  2*v1 + 2*v2 - 4*v5;
         c[zi] =  2*v2 + 2*v3 - 4*v6;

         c[3+xi] = -4*v2 + 4*v5 + 4*v6 - 4*v8;
         c[3+yi] = -4*v5 + 4*v7 + 4*v8 - 4*v9;
         c[3+zi] = -4*v1 + 4*v4 + 4*v5 - 4*v7;

         c[6+xi] = -3*v0 -   v1 + 4*v4;
         c[6+yi] =    v1 -   v2 - 4*v4 + 4*v7;
         c[6+zi] =    v2 -   v3 - 4*v7 + 4*v9;

         c[9] = v0;
      }

      double computeDistance (
         double[] c, double dx, double dy, double dz) {

         double d =
            c[0]*dx*dx + c[1]*dy*dy + c[2]*dz*dz +
            c[3]*dy*dz + c[4]*dx*dz + c[5]*dx*dy +
            c[6]*dx + c[7]*dy + c[8]*dz + c[9];
         return d;
      }

      void computeGradient (
         Vector3d grad, double[] c, double dx, double dy, double dz) {

         grad.x = 2*c[0]*dx + c[5]*dy + c[4]*dz + c[6];
         grad.y = 2*c[1]*dy + c[5]*dx + c[3]*dz + c[7];
         grad.z = 2*c[2]*dz + c[4]*dx + c[3]*dy + c[8];
            
         grad.x /= (2*myCellWidths.x);
         grad.y /= (2*myCellWidths.y);
         grad.z /= (2*myCellWidths.z);
      }

      public PlaneType computeBCoefs (
         double[] b, Vector3d r, double[] c, Plane planeCell) {

         double rx, ry, rz;
         Vector3d nrmCell = planeCell.normal;
         double off = planeCell.offset;

         PlaneType planeType;
         switch (nrmCell.maxAbsIndex()) {
            case 0: {
               rx = nrmCell.y/nrmCell.x;
               ry = nrmCell.z/nrmCell.x;
               rz = off/nrmCell.x;

               b[0] = c[1] + c[0]*rx*rx - c[5]*rx;
               b[1] = c[2] + c[0]*ry*ry - c[4]*ry;
               b[2] = c[3] + 2*c[0]*rx*ry - c[5]*ry - c[4]*rx;
               b[3] = c[7] - 2*c[0]*rz*rx + c[5]*rz - c[6]*rx;
               b[4] = c[8] - 2*c[0]*rz*ry + c[4]*rz - c[6]*ry;
               b[5] = c[9] + c[0]*rz*rz + c[6]*rz;

               planeType = PlaneType.YZ;
               break;
            }
            case 1: {
               rx = nrmCell.z/nrmCell.y;
               ry = nrmCell.x/nrmCell.y;
               rz = off/nrmCell.y;


               b[0] = c[2] + c[1]*rx*rx - c[3]*rx;
               b[1] = c[0] + c[1]*ry*ry - c[5]*ry;
               b[2] = c[4] + 2*c[1]*rx*ry - c[3]*ry - c[5]*rx;
               b[3] = c[8] - 2*c[1]*rz*rx + c[3]*rz - c[7]*rx;
               b[4] = c[6] - 2*c[1]*rz*ry + c[5]*rz - c[7]*ry;
               b[5] = c[9] + c[1]*rz*rz + c[7]*rz;

               planeType = PlaneType.ZX;
               break;
            }
            case 2: {
               rx = nrmCell.x/nrmCell.z;
               ry = nrmCell.y/nrmCell.z;
               rz = off/nrmCell.z;

               b[0] = c[0] + c[2]*rx*rx - c[4]*rx;
               b[1] = c[1] + c[2]*ry*ry - c[3]*ry;
               b[2] = c[5] + 2*c[2]*rx*ry - c[4]*ry - c[3]*rx;
               b[3] = c[6] - 2*c[2]*rz*rx + c[4]*rz - c[8]*rx;
               b[4] = c[7] - 2*c[2]*rz*ry + c[3]*rz - c[8]*ry;
               b[5] = c[9] + c[2]*rz*rz + c[8]*rz;

               planeType = PlaneType.XY;
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Vector3d.maxAbsIndex() returned value "+nrmCell.maxAbsIndex());
            }
         }
         if (r != null) {
            r.x = rx;
            r.y = ry;
            r.z = rz;
         }
         return planeType;
      }


      public void computeQuadCoefs (double[] c) {
         
         createTetOffsetsIfNecessary();
         switch (myTetId) {
            case TET0516: {
               computeQuadCoefs (c, myTetOffsets0156, 0, 1, 2);
               break;
            }
            case TET0456: {
               computeQuadCoefs (c, myTetOffsets0456, 1, 0, 2);
               break;
            }
            case TET0746: {
               computeQuadCoefs (c, myTetOffsets0476, 1, 2, 0);
               break;
            }
            case TET0126: {
               computeQuadCoefs (c, myTetOffsets0126, 0, 2, 1);
               break;
            }
            case TET0236: {
               computeQuadCoefs (c, myTetOffsets0326, 2, 0, 1);
               break;
            }
            case TET0376: {
               computeQuadCoefs (c, myTetOffsets0376, 2, 1, 0);
               break;
            }
            default: {
               throw new InternalErrorException ("Unknown tet type " + myTetId);
            }
         }
      }

      public int[] getVertices () {
         int[] vertices = new int[4];
         int[] nodes = myTetId.getNodes();
         for (int i=0; i<nodes.length; i++) {
            vertices[i] = myVoff + myQuadHexOffsets[nodes[i]];
         }
         return vertices;
      }
      
      public String toString() {
         return myTetId + "("+myXi+","+myYj+","+myZk+")";
      }
   }

   protected void addAdjacentTets (
      ArrayList<Integer> tets, int vbase, int hexNode) {
      switch (hexNode) {
         case 0:
         case 6: {
            for (int i=0; i<6; i++) {
               tets.add (vbase + i);
            }
            break;
         }
         case 1: {
            tets.add (vbase + TetID.TET0516.intValue());
            tets.add (vbase + TetID.TET0126.intValue());
            break;
         }
         case 2: {
            tets.add (vbase + TetID.TET0126.intValue());
            tets.add (vbase + TetID.TET0236.intValue());
            break;
         }
         case 3: {
            tets.add (vbase + TetID.TET0236.intValue());
            tets.add (vbase + TetID.TET0376.intValue());
            break;
         }
         case 4: {
            tets.add (vbase + TetID.TET0456.intValue());
            tets.add (vbase + TetID.TET0746.intValue());
            break;
         }
         case 5: {
            tets.add (vbase + TetID.TET0516.intValue());
            tets.add (vbase + TetID.TET0456.intValue());
            break;
         }
         case 7: {
            tets.add (vbase + TetID.TET0376.intValue());
            tets.add (vbase + TetID.TET0746.intValue());
            break;
         }
      }
   }      

   public static class TetPlaneIntersection {
      private static final double EPS = 1e-12;

      TetDesc myTetDesc;
      public int myNumSides;
      int myN0;
      int myN1;
      int myN2;
      int myN3;

      double myS0;
      double myS1;
      double myS2;
      double myS3;

      public Point3d myP0;
      public Point3d myP1;
      public Point3d myP2;
      public Point3d myP3;

      int myCCW; // 1 or -1 if the intersection is CCW or CW WRT the plane

      public TetPlaneIntersection () {
         myP0 = new Point3d();
         myP1 = new Point3d();
         myP2 = new Point3d();
         myP3 = new Point3d();
      }

      double triS (int i) {
         switch (i%3) {
            case 0: return myS1;
            case 1: return myS2;
            case 2: return myS3;
            default: {
               // can't get here
               return 0;
            }
         }         
      }

      int triN (int i) {
         switch (i%3) {
            case 0: return myN1;
            case 1: return myN2;
            case 2: return myN3;
            default: {
               // can't get here
               return 0;
            }
         }         
      }

     TetFeature getTriangleFeature (double s, int ei) {
         if (s <= EPS) {
            double s1 = triS(ei);
            // must lie on edge myN0, N(ei)
            if (s1 <= EPS) {
               return new TetNode (myN0);
            }
            else if (s1 >= 1-EPS) {
               return new TetNode (triN(ei));
            }
            else {
               return new TetEdge (myN0, triN(ei));
            }
         }
         else if (s >= 1-EPS) {
            double s2 = triS(ei+1);
            // must lie on edge myN0, N(ei+1)
            if (s2 <= EPS) {
               return new TetNode (myN0);
            }
            else if (s2 >= 1-EPS) {
               return new TetNode (triN(ei+1));
            }
            else {
               return new TetEdge (myN0, triN(ei+1));
            }
         }
         else {
            double s1 = triS(ei);
            double s2 = triS(ei+1);
            if (s1 >= 1-EPS && s2 >= 1-EPS) {
               // lies on edge N(ei), N(ei+1)
               return new TetEdge (triN(ei), triN(ei+1));
            }
            else {
               // lies on face myN0,  N(ei+1), N(ei)
               return new TetFace (myN0, triN(ei+1), triN(ei));
            }
         }
      }

      double quadS (int i) {
         switch (i%4) {
            case 0: return myS0;
            case 1: return myS1;
            case 2: return myS2;
            case 3: return myS3;
            default: {
               // can't get here
               return 0;
            }
         }         
      }

      int quadN (int i) {
         switch (i%4) {
            case 0: return myN0;
            case 1: return myN1;
            case 2: return myN2;
            case 3: return myN3;
            default: {
               // can't get here
               return 0;
            }
         }         
      }

      TetFeature getQuadFeature (double s, int ei) {
         if (s <= EPS) {
            // must lie on edge N(ei), N(ei+1)
            double s1 = quadS(ei+1);
            if (s1 <= EPS) {
               return new TetNode (quadN(ei));
            }
            else if (s1 >= 1-EPS) {
               return new TetNode (quadN(ei+1));
            }
            else {
               return new TetEdge (quadN(ei), quadN(ei+1));
            }
         }
         else if (s >= 1-EPS) {
            // must lie on edge N(ei+1), N(ei+2)
            double s2 = quadS(ei+2);
            if (s2 <= EPS) {
               return new TetNode (quadN(ei+1));
            }
            else if (s2 >= 1-EPS) {
               return new TetNode (quadN(ei+2));
            }
            else {
               return new TetEdge (quadN(ei+1), quadN(ei+2));
            }
         }
         else {
            double s1 = quadS(ei+1);
            if (s1 >= 1-EPS) {
               // then s2 must be close to 0, with intersection at N(ei+1)
               return new TetNode (quadN(ei+1));
            }
            else {
               int n0 = quadN(ei);
               int n1 = quadN(ei+1);
               int n2 = quadN(ei+2);
               if (n0 == myN0 || n0 == myN2) {
                  return new TetFace (n0, n1, n2);
               }
               else {
                  // need to flip
                  return new TetFace (n0, n2, n1);
               }
            }
         }
      }

      TetFeature getFeature (double s, int ei) {
         if (myNumSides == 3) {
            return getTriangleFeature (s, ei);
         }
         else if (myNumSides == 4) {
            return getQuadFeature (s, ei);
         }
         else {
            throw new InternalErrorException (
               "Uninitialized myNumSides field of TetPlaneIntersection");
         }
      }

      int getNode (int nidx) {
         switch (nidx) {
            case 0: return myN0;
            case 1: return myN1;
            case 2: return myN2;
            case 3: return myN3;
            default: {
               throw new InternalErrorException (
                  "nidx=" + nidx + ", must be in the range [0,3]");
            }
         }
      }

      boolean clipRange (
         double[] rng, DoubleHolder edgeS, int ccw, 
         double ax, double ay, double bx, double by, double cx, double cy) {

         boolean clippedUpper = false;
         double aXb = ax*by - ay*bx;
         if (aXb != 0) {
            // double s = (cx*by - cy*bx)/aXb;
            // if (s >= 0.0 && s <= 1.0) {
               double t = (cx*ay - cy*ax)/aXb;
               if (ccw*aXb > 0) {
                  // solution is lower bound
                  if (t > rng[0]) {
                     rng[0] = t;
                  }
               }
               else {
                  // solution is upper bound
                  if (t < rng[1]) {
                     rng[1] = t;
                     edgeS.value = (cx*by - cy*bx)/aXb;
                     clippedUpper = true;
                  }
               }
               //}
         }
         return clippedUpper;
      }

      int intersectRay (
         double[] rng,
         DoubleHolder edgeS, Point3d q, Vector3d dir,
         PlaneType planeType, int planeSgn) {
         
         Point3d[] pi = new Point3d[] {
            myP0, myP1, myP2, myP3};

         int[] ni;
         if (myNumSides == 4) {
            ni = new int[] { myN0, myN1, myN2, myN3};
         }
         else {
            ni = new int[] { myN1, myN2, myN3};
         }

         Point3d p0 = pi[0];
         int upperIdx = -1;
         int ccw = planeSgn*myCCW;
         for (int i=0; i<myNumSides; i++) {
            Point3d p1 = i<myNumSides-1 ? pi[i+1] : pi[0];
            switch (planeType) {
               case YZ: {
                  if (clipRange (
                         rng, edgeS, ccw, p1.y-p0.y, p1.z-p0.z,
                         dir.y, dir.z, q.y-p0.y, q.z-p0.z)) {
                     upperIdx = i;
                  }
                  break;
               }
               case ZX: {
                  if (clipRange (
                         rng, edgeS, ccw, p1.z-p0.z, p1.x-p0.x,
                         dir.z, dir.x, q.z-p0.z, q.x-p0.x)) {
                     upperIdx = i;
                  }
                  break;
               }
               case XY: {
                  if (clipRange (
                         rng, edgeS, ccw, p1.x-p0.x, p1.y-p0.y,
                         dir.x, dir.y, q.x-p0.x, q.y-p0.y)) {
                     upperIdx = i;
                  }
                  break;
               }
            }
            p0 = p1;
         }
         return upperIdx;
      }

      void updateRange (
         double[] rng, double ax, double ay, double bx, double by) {

         double aXb = ax*by - ay*bx;
         if (aXb < rng[0]) {
            rng[0] = aXb;
         }
         else if (aXb > rng[1]) {
            rng[1] = aXb;
         }
      }

      double tanIntersectOverlap (
         Point3d q, Vector3d grad, PlaneType planeType) {

         double[] rng = new double[2];

         switch (planeType) {
            case YZ: {
               updateRange (rng, -grad.z, grad.y, myP0.y-q.y, myP0.z-q.z);
               updateRange (rng, -grad.z, grad.y, myP1.y-q.y, myP1.z-q.z);
               updateRange (rng, -grad.z, grad.y, myP2.y-q.y, myP2.z-q.z);
               if (myNumSides == 4) {
                  updateRange (rng, -grad.z, grad.y, myP3.y-q.y, myP3.z-q.z);
               }
               break;
            }
            case ZX: {
               updateRange (rng, -grad.x, grad.z, myP0.z-q.z, myP0.x-q.x);
               updateRange (rng, -grad.x, grad.z, myP1.z-q.z, myP1.x-q.x);
               updateRange (rng, -grad.x, grad.z, myP2.z-q.z, myP2.x-q.x);
               if (myNumSides == 4) {
                  updateRange (rng, -grad.x, grad.z, myP3.z-q.z, myP3.x-q.x);
               }
               break;
            }
            case XY: {
               updateRange (rng, -grad.y, grad.x, myP0.x-q.x, myP0.y-q.y);
               updateRange (rng, -grad.y, grad.x, myP1.x-q.x, myP1.y-q.y);
               updateRange (rng, -grad.y, grad.x, myP2.x-q.x, myP2.y-q.y);
               if (myNumSides == 4) {
                  updateRange (rng, -grad.y, grad.x, myP3.x-q.x, myP3.y-q.y);
               }
               break;
            }
         }
         if (rng[0]*rng[1] < 0) {
            return Math.min(Math.abs(rng[0]), Math.abs(rng[1]));
         }
         else {
            return -Math.min(Math.abs(rng[0]), Math.abs(rng[1]));
         }
      }
   }

   void intersectTriangle (
      TetPlaneIntersection isect,
      int n0, double d0, Point3d p0, int n1, double d1, Point3d p1, 
      int n2, double d2, Point3d p2, int n3, double d3, Point3d p3) {
      
      double EPS = 1e-8;

      double s1 = d0/(d0-d1);
      double s2 = d0/(d0-d2);
      double s3 = d0/(d0-d3);

      int numsmall = 0;
      if (s1 <= EPS) {
         numsmall++;
      }
      if (s2 <= EPS) {
         numsmall++;
      }
      if (s3 <= EPS) {
         numsmall++;
      }
      if (numsmall > 1) {
         isect.myNumSides = 0;
      }
      isect.myN0 = n0;
      isect.myP0.combine (1-s1, p0, s1, p1);
      isect.myN1 = n1;
      isect.myS1 = s1;
      isect.myP1.combine (1-s2, p0, s2, p2);
      isect.myN2 = n2;
      isect.myS2 = s2;
      isect.myP2.combine (1-s3, p0, s3, p3);
      isect.myN3 = n3;
      isect.myS3 = s3;
      isect.myNumSides = 3;
      isect.myCCW = d0 < 0 ? 1 : -1;
   }

   void intersectQuad (
      TetPlaneIntersection isect,
      int n0, double d0, Point3d p0, int n1, double d1, Point3d p1, 
      int n2, double d2, Point3d p2, int n3, double d3, Point3d p3) {
      
      double EPS = 1e-8;

      double s1 = d0/(d0-d1);
      double s2 = d1/(d1-d2);
      double s3 = d2/(d2-d3);
      double s0 = d3/(d3-d0);

      if (s1 <= EPS) {
         if (s3 <= EPS) {
            // quad meets at points p0 and p2, so is degenerate
            isect.myNumSides = 0;
         }
      }
      else if (s2 <= EPS) {
         if (s0 < EPS) {
            // quad meets at points p1 and p3, so is degenerate
            isect.myNumSides = 0;
         }
      }
      isect.myN0 = n0;
      isect.myS0 = s0;
      isect.myP0.combine (1-s1, p0, s1, p1);
      isect.myN1 = n1;
      isect.myS1 = s1;
      isect.myP1.combine (1-s2, p1, s2, p2);
      isect.myN2 = n2;
      isect.myS2 = s2;
      isect.myP2.combine (1-s3, p2, s3, p3);
      isect.myN3 = n3;
      isect.myS3 = s3;
      isect.myP3.combine (1-s0, p3, s0, p0);
      isect.myNumSides = 4;
      isect.myCCW = d0 > 0 ? 1 : -1;
   }

   public boolean intersectTetAndPlane (
      TetPlaneIntersection isect, TetDesc tdesc, Point3d[] vpnts, Plane plane) {

      Point3d p0 = vpnts[0];
      Point3d p1 = vpnts[1];
      Point3d p2 = vpnts[2];
      Point3d p3 = vpnts[3];

      double d0 = plane.distance(vpnts[0]);
      double d1 = plane.distance(vpnts[1]);
      double d2 = plane.distance(vpnts[2]);
      double d3 = plane.distance(vpnts[3]);

      int[] nodes = tdesc.myTetId.getNodes();
      int n0 = nodes[0];
      int n1 = nodes[1];
      int n2 = nodes[2];
      int n3 = nodes[3];
      
      int sgn0 = (d0 >= 0 ? 1 : -1);
      int sgn1 = (d1 >= 0 ? 1 : -1);
      int sgn2 = (d2 >= 0 ? 1 : -1);
      int sgn3 = (d3 >= 0 ? 1 : -1);

      if (sgn0*sgn1 == 1) {
         if (sgn0*sgn2 == 1) {
            if (sgn0*sgn3 == 1) {
               // + + +   nothing
               isect.myNumSides = 0;
            }
            else {
               // + + -   3-0 3-1 3-2
               intersectTriangle (
                  isect, n3, d3, p3, n0, d0, p0, n1, d1, p1, n2, d2, p2);
            }
         }
         else {
            if (sgn0*sgn3 == 1) {
               // + - +   2-0 2-3 2-1
               intersectTriangle (
                  isect, n2, d2, p2, n0, d0, p0, n3, d3, p3, n1, d1, p1);
            }
            else {
               // + - -   0-3 3-1 1-2 2-0
               intersectQuad (
                  isect, n0, d0, p0, n3, d3, p3, n1, d1, p1, n2, d2, p2);
            }
         }
      }
      else {
         if (sgn0*sgn2 == 1) {
            if (sgn0*sgn3 == 1) {
               // - + +   1-0 1-2 1-3
               intersectTriangle (
                  isect, n1, d1, p1, n0, d0, p0, n2, d2, p2, n3, d3, p3);
            }
            else {
               // - + -   0-1 1-2 2-3 3-0
               intersectQuad (
                  isect, n0, d0, p0, n1, d1, p1, n2, d2, p2, n3, d3, p3);
            }
         }
         else {
            if (sgn0*sgn3 == 1) {
               // - - +   0-2 2-3 3-1 1-0
               intersectQuad (
                  isect, n0, d0, p0, n2, d2, p2, n3, d3, p3, n1, d1, p1);
            }
            else {
               // - - -   0-1 0-3 0-2
               intersectTriangle (
                  isect, n0, d0, p0, n1, d1, p1, n3, d3, p3, n2, d2, p2);
            }
         } 
      }
      if (isect.myNumSides != 0) {
         isect.myTetDesc = tdesc;
         return true;
      }
      else {
         return false;
      }
   }

   private int[] oppNodes = new int[] { 6, 7, 4, 5, 2, 3, 0, 1 };

   /**
    * Return all tets adjacent to a given node
    */
   protected int[] getTetNodeNeighbours (int vbase, int nidx) {
      createTetOffsetsIfNecessary();      

      ArrayList<Integer> tets = new ArrayList<Integer>();
      int nbase = vbase + myQuadHexOffsets[nidx] - myQuadHexOffsets[6];
      for (int i=0; i<8; i++) {
         int base = nbase + myQuadHexOffsets[i];
         if (base != vbase && base >= 0) {
            addAdjacentTets (tets, base, oppNodes[i]);
         }
      }
      return ArraySupport.toIntArray (tets);
   }

   /**
    * Return all tets adjacent to a given edge
    */
   protected int[] getTetEdgeNeighbours (int vbase, int nidx0, int nidx1) {
      createTetOffsetsIfNecessary();      
      return new int[0];
   }

//   /**
//    * Returns the single tet adjacent to a given face
//    */
//   protected int getTetFaceNeighbour (int vbase, FaceID faceId) {
//
//      createTetOffsetsIfNecessary();      
//      return 0;
//   }

   protected int intersectCurveWithTetBoundary (
      Vector2d[] pnts, double[] b, Vector3d r, int minCoord, int maxCoord) {

      return 0;
   }

   protected boolean findSurfaceTangentInTet (
      Point3d ptLoc, TetDesc tdesc, 
      Point3d paLoc, Plane planeLoc) {
      // XXX convert normal, off, and pa to tet coordinates

      Plane planeCell = new Plane();
      Point3d paCell = new Point3d();
      tdesc.transformPlaneToCell (planeCell, planeLoc);
      tdesc.transformToQuadCell (paCell, paLoc);

      double[] c = new double[10];
      double[] b = new double[6];
      tdesc.computeQuadCoefs (c);
      PlaneType planeType;

      Vector2d[] pnts = new Vector2d[] { new Vector2d(), new Vector2d() };

      Vector3d r = new Vector3d();
      planeType = tdesc.computeBCoefs (b, r, c, planeCell);
      double x = 0, y = 0, z = 0;
      boolean found = false;

      switch (planeType) {
         case YZ: {
            int nr = findTangentPoints (pnts, b, paCell.y, paCell.z);
            for (int i=0; i<nr; i++) {
               y = pnts[i].x;
               z = pnts[i].y;
               x = r.z - r.x*y - r.y*z;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  found = true;
                  break;
               }
            }
            break;
         }
         case ZX: {

            int nr = findTangentPoints (pnts, b, paCell.z, paCell.x);
            for (int i=0; i<nr; i++) {
               z = pnts[i].x;
               x = pnts[i].y;
               y = r.z - r.x*z - r.y*x;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  found = true;
                  break;
               }
            }
            break;
         }
         case XY: {
            int nr = findTangentPoints (pnts, b, paCell.x, paCell.y);
            for (int i=0; i<nr; i++) {
               x = pnts[i].x;
               y = pnts[i].y;
               z = r.z - r.x*x - r.y*y;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  found = true;
                  break;
               }
            }
            break;
         }
      }
      if (found) {
         //System.out.println ("dist=" + computeDist (c, x, y, z));
         //System.out.println ("perp=" + computePerp (c, x, y, z, paCell));
         tdesc.transformFromQuadCell (ptLoc, x, y, z);
         return true;
      }
      else {
         return false;
      }
   }

   protected boolean findSurfaceTangentCell (
      Point3d ptCell, TetDesc tdesc,
      double[] b, Vector3d r, PlaneType planeType,
      Point3d paCell) {
      // XXX convert normal, off, and pa to tet coordinates

      Vector2d[] pnts = new Vector2d[] { new Vector2d(), new Vector2d() };

      double x = 0, y = 0, z = 0;

      switch (planeType) {
         case YZ: {
            int nr = findTangentPoints (pnts, b, paCell.y, paCell.z);
            for (int i=0; i<nr; i++) {
               y = pnts[i].x;
               z = pnts[i].y;
               x = r.z - r.x*y - r.y*z;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  ptCell.set (x, y, z);
                  return true;
               }
            }
            break;
         }
         case ZX: {
            int nr = findTangentPoints (pnts, b, paCell.z, paCell.x);
            for (int i=0; i<nr; i++) {
               z = pnts[i].x;
               x = pnts[i].y;
               y = r.z - r.x*z - r.y*x;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  ptCell.set (x, y, z);
                  return true;
               }
            }
            break;
         }
         case XY: {
            int nr = findTangentPoints (pnts, b, paCell.x, paCell.y);
            for (int i=0; i<nr; i++) {
               x = pnts[i].x;
               y = pnts[i].y;
               z = r.z - r.x*x - r.y*y;
               if (tdesc.myTetId.isInside (x, y, z)) {
                  ptCell.set (x, y, z);
                  return true;
               }
            }
            break;
         }
      }
      return false;
   }

   protected double findSurfaceIntersectionCell (
      TetDesc tdesc,
      double[] b, PlaneType planeType,
      double[] srng, Point3d pc, Vector3d dc) {
      // XXX convert normal, off, and pa to tet coordinates

      double[] svals = new double[2];
      double smax = Double.MAX_VALUE;
      double x = 0, y = 0, z = 0;

      int nr = 0;
      switch (planeType) {
         case YZ: {
            nr = intersectSurfaceAndRay (
               svals, b, pc.y, dc.y, pc.z, dc.z, srng[0], srng[1]);
            break;
         }
         case ZX: {
            nr = intersectSurfaceAndRay (
               svals, b, pc.z, dc.z, pc.x, dc.x, srng[0], srng[1]);
            break;
         }
         case XY: {
            nr = intersectSurfaceAndRay (
               svals, b, pc.x, dc.x, pc.y, dc.y, srng[0], srng[1]);
            break;
         }
      }
      if (nr > 0) {
         // choose one nearest the ray origin
         return svals[nr-1];
      }
      else {
         return -1;
      }
   }

   public static class TanSearchInfo {
      TetFeature lastFeat;
      Vector3d lastGrad;

      TanSearchInfo() {
         lastGrad = new Vector3d();
      }            
   }

   public boolean findQuadraticSurfaceIntersection (
      Point3d pi, Point3d p0, Point3d pa, Vector3d nrm) {

      // transform p0, pa and nrm into grid local coordinates
      Point3d p0Loc = new Point3d();
      getLocalCoordinates (p0Loc, p0);
      Point3d paLoc = new Point3d();
      getLocalCoordinates (paLoc, pa);
      Vector3d nrmLoc = new Vector3d(nrm);
      getLocalNormal (nrmLoc, nrm);
      nrmLoc.normalize();

      // generate a plane from the normal and p0
      Plane planeLoc = new Plane (nrmLoc, p0Loc);

      // Dynamic array to store tet/plane intersections
      ArrayList<TetPlaneIntersection> isects =
         new ArrayList<TetPlaneIntersection>();
      isects.add (new TetPlaneIntersection()); // will need at least one

      // temporary storage for computed tet vertex coordinates
      Point3d[] vpnts = new Point3d[] {
         new Point3d(), new Point3d(), new Point3d(), new Point3d() };      

      int icnt = 0;
      int maxIters = 2;
      int code = NONE;
      TetDesc tdesc = null;
      TanSearchInfo sinfo = new TanSearchInfo();
      int sgnDotGrad = 0;
      Point3d ps = new Point3d(p0Loc);

      Vector3d dirLoc = new Vector3d();
      dirLoc.sub (paLoc, p0Loc);
      if (dirLoc.normSquared() == 0) {
         return false;
      }
      dirLoc.normalize();
      
      Vector3i vxyz = new Vector3i();
      Vector3d xyz = new Vector3d();
      if (getQuadCellCoords (vxyz, xyz, p0Loc) == null) {
         // shouldn't happen - grid should have enough margin to prevent this
         return false;
      }
      tdesc =
         new TetDesc (vxyz, TetID.findSubTet (xyz.x, xyz.y, xyz.z));      
      tdesc.getVertexCoords (vpnts);
      TetPlaneIntersection isect = isects.get(0);
      if (!intersectTetAndPlane (isect, tdesc, vpnts, planeLoc)) {
         // shouldn't happen - p0 should be on the plane
         return false;
      }

      PlaneType planeType = null;
      Vector3d nrmCell = new Vector3d();
      transformNormalToCell (nrmCell, planeLoc.normal);
      switch (nrmCell.maxAbsIndex()) {
         case 0: planeType = PlaneType.YZ; break;
         case 1: planeType = PlaneType.ZX; break;
         case 2: planeType = PlaneType.XY; break;
      }
      int planeSgn = (nrmCell.get(nrmCell.maxAbsIndex()) > 0 ? 1 : -1);

      double[] srng = new double[] { -Double.MAX_VALUE, Double.MAX_VALUE };
      DoubleHolder edgeS = new DoubleHolder();
      int edgeIdx = isect.intersectRay (
         srng, edgeS, p0Loc, dirLoc, planeType, planeSgn);
      if (edgeIdx == -1) {
         // shouldn't happen - p0 should be in the tet
         return false;
      }
      TetFeature lastFeat = isect.getFeature (edgeS.value, edgeIdx);

      HashSet<TetDesc> visitedTets = new HashSet<TetDesc>();
      visitedTets.add (tdesc);
      
      while (findSurfaceIntersectionInTet (
                pi, isect, srng, p0Loc, dirLoc, planeLoc) == CONTINUE) {
          int ntets = getFeatureAdjacentTets (
             isects, tdesc, lastFeat, planeLoc, visitedTets);
          TetPlaneIntersection ibest = null;
          double bestLen = 0;
          int bestEdgeIdx = -1;
          double bestS = -1;          
          for (int k=0; k<ntets; k++) {
             isect = isects.get(k);
             double[] irng =
                new double[] { -Double.MAX_VALUE, Double.MAX_VALUE };
             edgeIdx = isect.intersectRay (
                irng, edgeS, p0Loc, dirLoc, planeType, planeSgn);
             if (edgeIdx != -1) {
                double ilen = irng[1]-irng[0];
                if (ilen > bestLen) {
                   bestLen = ilen;
                   ibest = isect;
                   bestEdgeIdx = edgeIdx;
                   bestS = edgeS.value;
                   srng[0] = irng[0];
                   srng[1] = irng[1];
                }
             }
          }
          if (bestEdgeIdx == -1) {
             return false;
          }
          else {
             lastFeat = ibest.getFeature (bestS, bestEdgeIdx);
             isect = ibest;
             tdesc = isect.myTetDesc;
             visitedTets.add (tdesc);
          }
      }
      getWorldCoordinates (pi, pi);
      return true;
   }
         
   private int intersectSurfaceAndRay (
      double[] roots, double[] b,
      double px, double dx, double py, double dy, double smin, double smax) {

      double aa =
         b[0]*dx*dx + b[1]*dy*dy + b[2]*dx*dy;
      double bb =
         2*b[0]*px*dx + 2*b[1]*py*dy + b[2]*(px*dy + dx*py) + b[3]*dx + b[4]*dy;
      double cc =
         b[0]*px*px + b[1]*py*py + b[2]*px*py + b[3]*px + b[4]*py + b[5];

      return QuadraticSolver.getRoots (roots, aa, bb, cc, smin, smax);
   }

   private final int NONE = 0;
   private final int CONTINUE = 1;
   private final int DONE = 2;

   int findSurfaceIntersectionInTet (
      Point3d pi, TetPlaneIntersection isect,
      double[] srng, Point3d p0, Vector3d dir, Plane plane) {

      if (srng[0] >= srng[1]) {
         return CONTINUE;
      }

      double[] c = new double[10];
      TetDesc tdesc = isect.myTetDesc;
      tdesc.computeQuadCoefs (c);         

      Point3d p0Cell = new Point3d();
      tdesc.transformToQuadCell (p0Cell, p0);
      Vector3d dirCell = new Vector3d();
      tdesc.transformToQuadCell (dirCell, dir);
      Plane planeCell = new Plane();
      tdesc.transformPlaneToCell (planeCell, plane);

      double[] b = new double[6];
      Vector3d r = new Vector3d();
      PlaneType planeType = tdesc.computeBCoefs (b, r, c, planeCell);
      

      double s = findSurfaceIntersectionCell (
         tdesc, b, planeType, srng, p0Cell, dirCell);
      if (s != -1) {
         pi.scaledAdd (s, dir, p0);
         return DONE;
      }
      else {
         return CONTINUE;
      }
   }

   int findSurfaceTangent (
      Point3d pt, TanSearchInfo sinfo, TetPlaneIntersection isect,
      Point3d pold, int sgnDotGrad, Point3d pa, Plane plane) {

      double[] c = new double[10];
      TetDesc tdesc = isect.myTetDesc;
      tdesc.computeQuadCoefs (c);         

      //System.out.println ("  checking " + tdesc); 

      Point3d paCell = new Point3d();
      tdesc.transformToQuadCell (paCell, pa);
      Plane planeCell = new Plane();
      tdesc.transformPlaneToCell (planeCell, plane);
      double[] b = new double[6];
      Vector3d r = new Vector3d();
      PlaneType planeType = tdesc.computeBCoefs (b, r, c, planeCell);

      if (pold != null) {
         Point3d pc = new Point3d();
         Vector3d grad = new Vector3d();
         Vector3d dela0 = new Vector3d();
         tdesc.transformToQuadCell (pc, pold);
         // compute gradient and project it into the plane
         tdesc.computeGradient (grad, c, pc.x, pc.y, pc.z);
         grad.scaledAdd (-grad.dot(plane.normal), plane.normal);
         dela0.sub (pold, pa);
         int newSgnDotGrad = (dela0.dot(grad) > 0 ? 1 : -1);
         if (newSgnDotGrad*sgnDotGrad < 0) {
            // sign change, so accept pold
            pt.set (pold);
            return DONE;
         }
      }
      
      if (findSurfaceTangentCell (
             pt, tdesc, b, r, planeType, paCell)) {
         tdesc.transformFromQuadCell (pt, pt);
         return DONE;
      }
      else {
         // find the intersection points, if any, of the curve with each
         // of the boundary edges
         Point3d[] ip = new Point3d[] {
            isect.myP0, isect.myP1, isect.myP2, isect.myP3 };         

         Point3d pl0 = ip[0];
         Point3d pl1 = null;
         Point3d pc0 = new Point3d();
         Point3d pc1 = new Point3d();
         tdesc.transformToQuadCell (pc0, pl0);

         double EPS = 1e-12;
         double[] svals = new double[2];
         int bestEdgeIdx = -1; // index of edge containing the best next point
         double bestDistToA = -1; 
         double bestS = -1; 
         for (int i=0; i<isect.myNumSides; i++) {
            pl1 = i<isect.myNumSides-1 ? ip[i+1] : ip[0];
            tdesc.transformToQuadCell (pc1, pl1);

            int nr = 0;
            switch (planeType) {
               case YZ: {
                  nr = intersectSurfaceAndRay (
                     svals, b, pc0.y, pc1.y-pc0.y, pc0.z, pc1.z-pc0.z, 0.0, 1.0);
                  break;
               }
               case ZX: {
                  nr = intersectSurfaceAndRay (
                     svals, b, pc0.z, pc1.z-pc0.z, pc0.x, pc1.x-pc0.x, 0.0, 1.0);
                  break;
               }
               case XY: {
                  nr = intersectSurfaceAndRay (
                     svals, b, pc0.x, pc1.x-pc0.x, pc0.y, pc1.y-pc0.y, 0.0, 1.0);
                  break;
               }
            }
            if (nr > 0) {
               Point3d pi = new Point3d();
               Vector3d grad = new Vector3d();
               Vector3d dela0 = new Vector3d();
               for (int j=0; j<nr; j++) {
                  // compute gradient at the intersection point, and
                  // project it into the plane
                  pi.combine (1-svals[j], pc0, svals[j], pc1);
                  tdesc.computeGradient (grad, c, pi.x, pi.y, pi.z);
                  grad.scaledAdd (-grad.dot(plane.normal), plane.normal);
                  pi.combine (1-svals[j], pl0, svals[j], pl1);
                  dela0.sub (pi, pa);
                  if (pold != null && pi.distance (pold) <= myDiameter*EPS) {
                     // same as initial point, ignore
                     continue;
                  }
                  if (dela0.dot(grad)*sgnDotGrad < 0) {
                     // tangent direction switched sides; can't
                     // be part of the same curve section, ignore
                     continue;
                  }
                  double distToA = pa.distance (pi);
                  // if sgnDotGrad > 0, want to move towards pa.
                  // otherwise, want to move away
                  if (bestEdgeIdx == -1 ||
                      (sgnDotGrad > 0 && distToA < bestDistToA) ||
                      (sgnDotGrad < 0 && distToA > bestDistToA)) {
                     bestEdgeIdx = i;
                     bestDistToA = distToA;
                     bestS = svals[j];
                     sinfo.lastGrad.set (grad);
                     pt.set (pi);
                  }
               }
            }
            pl0 = pl1;
            pc0.set (pc1);
         }
         if (bestEdgeIdx != -1) {
            // need to find the feature associated with this point
            sinfo.lastFeat = isect.getFeature (bestS, bestEdgeIdx);
            //System.out.println ("setting feat=" + sinfo.lastFeat);
            return CONTINUE;
         }
         else {
            return NONE;
         }
      }
   }

   public TetDesc getTetDesc (int xi, int yj, int zk, TetID tetId) {
      return new TetDesc (xi, yj, zk, tetId);
   }

   public void transformNormalToCell (Vector3d nc, Vector3d nl) {
      if (nc != nl) {
         nc.set (nl);
      }
      nc.x *= myCellWidths.x;
      nc.y *= myCellWidths.y;
      nc.z *= myCellWidths.z;
      nc.normalize();
   }

   public int getFeatureAdjacentTets (
      ArrayList<TetPlaneIntersection> isects, 
      TetDesc tdesc, TetFeature feat, Plane plane,
      HashSet<TetDesc> visited) {
      
      createTetOffsetsIfNecessary();

      Point3d[] vpnts = new Point3d[] {
         new Point3d(), new Point3d(), new Point3d(), new Point3d() };

      if (feat instanceof TetFace) {
         TetDesc adjDesc = myFaceTets.get((TetFace)feat);
         TetDesc adesc = new TetDesc(adjDesc);
         adesc.addOffset (tdesc);
         if (adesc.inRange() && !visited.contains(adesc)) {
            adesc.getVertexCoords (vpnts);
            if (isects.size() == 0) {
               isects.add (new TetPlaneIntersection());
            }
            TetPlaneIntersection isect = isects.get(0);
            if (intersectTetAndPlane (isect, adesc, vpnts, plane)) {
               return 1;
            }
         }
         return 0;
      }
      else {
         TetDesc[] adjDescs = null;
         if (feat instanceof TetNode) {
            adjDescs = myNodeTets.get(((TetNode)feat).getNode());
         }
         else { // feat instanceof TetEdge
            adjDescs = myEdgeTets.get((TetEdge)feat);
         }
         int numi = 0;
         for (int i=0; i<adjDescs.length; i++) {
            TetDesc adesc = new TetDesc(adjDescs[i]);
            adesc.addOffset (tdesc);
            if (adesc.inRange() && !visited.contains(adesc)) {
               // for node-adjacent tets, only allow those whose quad hex is
               // different from that of tdesc
               if ((feat instanceof TetNode && adesc.myVoff != tdesc.myVoff) ||
                   (feat instanceof TetEdge && !adesc.equals(tdesc))) {
                  adesc.getVertexCoords (vpnts);
                  if (isects.size() == numi) {
                     isects.add (new TetPlaneIntersection());
                  }
                  TetPlaneIntersection isect = isects.get(numi);
                  if (intersectTetAndPlane (isect, adesc, vpnts, plane)) {
                     numi++;
                  }
               }
            }
         }
         return numi;
      }
   }

   public static void main (String[] args) {
      DistanceGrid grid =
         new DistanceGrid (new Vector3d (2, 1, 1), new Vector3i (6, 4, 4));

      grid.createConnectivity();
      grid.printConnectivity();
   }
   
 
}
