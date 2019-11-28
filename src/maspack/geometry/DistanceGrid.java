/**
 * Copyright (c) 2017, by the Authors: Bruce Haines (UBC), Antonio Sanchez
 * (UBC), John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * thze LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.VectorTransformer3d;
import maspack.geometry.DistanceGridSurfCalc.*;
import maspack.geometry.BVFeatureQuery.*;
import maspack.util.InternalErrorException;
import maspack.util.*;

/**
 * Implements a distance field on a regular 3D grid. Distances, normals and
 * gradients can be interpolated between grid points using either linear or
 * quadratic interpolation. Methods are provided to compute the distance field
 * based on nearest distances to a set of geometric features, such as vertices,
 * edges, or faces. One common use of such a field is to detect point
 * penetration distances and normals for use in collision handling.
 *
 * <p>The field is implemented using a regular 3D grid composed of
 * <code>nx</code> X <code>ny</code> X <code>nz</code> vertices along the x, y
 * and z directions, giving the grid a <i>resolution</i> of <code>rx</code>,
 * <code>ry</code>, <code>rz</code>, cells along each of these directions,
 * where <code>rx = nx-1</code>, <code>ry = ny-1</code>, and <code>rz =
 * nz-1</code>. The grid has widths <code>w = (wx, wy, wz)</code> along each of
 * these directions, and <i>cell widths</i> given by <code>wx/rx</code>,
 * <code>wy/ry</code>, and <code>wz/rz</code>.
 *
 * <p>Several coordinate frames are associated with the grid: the <i>local</i>
 * frame L, the <i>world</i> frame W, and the <i>grid</i> frame G. Details on
 * these are given in the documentation for {@link InterpolatingGridBase}.
 *
 * <p>Support is provided to compute the distance field from sets of underlying
 * mesh features (such as faces or vertices). These computations are done in
 * the local frame.
 *
 * <p>Distances for any point within the grid can be obtained by interpolation
 * of the distances at surrounding vertices.  Interpolation can be either
 * linear or quadratic.  Linearly interpolated distances are obtained using
 * {@link #getLocalDistance} and are computed using trilinear interpolation
 * within the grid cell containing the point. If the query point is outside the
 * grid, then the distance is assigned the special value {@link
 * #OUTSIDE_GRID}. Normals can also be obtained, using the
 * <code>getLocalDistanceAndNormal</code> methods.  Normals are also
 * interpolated using trilinear interpolation, with the normals at each vertex
 * being computed on demand by mid-point differencing with adjacent
 * vertices. Note that this means that the normals do <i>not</i> correspond to
 * the gradient of the distance function; however, their interpolated values
 * are smoother than those of the gradient. Gradient values can be obtained
 * instead, using the <code>getLocalDistanceAndGradient</code> methods. The
 * returned gradient is the true gradient of the distance function within the
 * interpolating cell and a linear function within that cell. All inputs and
 * outputs for the above described distance methods are assumed to be in local
 * coordinates. The method {@link #getWorldDistance} and associated
 * <code>getWorldDistanceAndNormal</code> and
 * <code>getWorldDistanceAndGradient</code> methods perform the same operations
 * in world coordinates.
 *
 * <p>Quadratically interpolated distances are obtained using {@link
 * #getQuadDistance}. Quadratic interpolation is done within a composite
 * <i>quadratic</i> cell composed of 2 x 2 x 2 regular cells. To ensure that
 * all points within the grid can be assigned a unique quadratic cell, the grid
 * resolution is restricted so that <code>rx</code>, <code>ry</code> and
 * <code>rz</code> are always even. Moreover, within each quadratic cell,
 * interpolation is done by partitioning the cell into six tetrahedra, each
 * with 10 nodes, and then performing quadratic interpolation within the
 * appropriate tetrahedron. This is done to ensure that the interpolation
 * function is exactly quadratic instead of tri-quadratic. Because the
 * interpolation is exactly quadratic, certain operations, such as intersecting
 * level set surfaces, can be performed by solving a simple quadratic equation.
 * The gradient of the quadratically interpolated distance function can also be
 * obtained using {@link #getQuadDistanceAndGradient}; this will return
 * the gradient of the distance function within the quadratic cell containing
 * the query point. These gradient values are smoother than those associated
 * with linear interpolation and so (if normalized) can be used to provide
 * normal vectors. Consequently, quadratically interpolated normals, analagous
 * to those returned by {@link #getLocalDistanceAndNormal}, are not currently
 * supported. All inputs and outputs for the above described quadratic distance
 * methods are assumed to be in local coordinates. The methods
 * {@link #getWorldQuadDistance} and
 * {@link #getWorldQuadDistanceAndGradient}
 * perform the same operations
 * in world coordinates.
 *
 * <p>Distances at the grid vertices can either be assigned directly, using
 * {@link #setVertexDistances}, or can be computed based on nearest distance
 * calculations to a set of point-based geometric features such as vertices,
 * edges, or faces, using {@link #computeDistances}. The convenience methods
 * {@link #computeFromFeatures} and {@link #computeFromFeaturesOBB} will both
 * compute distances and also fit the grid to the features. When computed from
 * features, each grid vertex is associated with a <i>nearest</i>
 * feature.
 *
 * <p>Feature based distance computation assumes that the feature points are
 * represented in local coordinates. For grid vertices close to the mesh,
 * nearby features are examined to determine the distance from the mesh to the
 * vertex. A sweep method is then used to propagate distance values throughout
 * the volume and determine whether vertices are inside or outside. If the
 * features are faces, it is possible to request that the grid be
 * <i>signed</i>. In this case, ray casts are used to determine whether
 * vertices are inside or outside the feature set, with interior and exterior
 * vertices being given negative and positive distance values,
 * respectively. The algorithm is based on C++ code provided by Robert Bridson
 * (UBC) in the <code>makelevelset3</code> class of his <a
 * href="http://www.cs.ubc.ca/~rbridson/download/common_2008_nov_12.tar.gz">
 * common code set</a>.
 */
public class DistanceGrid extends ScalarGridBase {

   protected Vector3d[] myNormals;      // normal values at each vertex
   protected double[][] myQuadCoefs;    // quad tet interpolation coefficients 
   protected TetDesc[] myTets;          // quad tet interpolation coefficients 
   protected boolean[] myTetIsOutside;  // indicates "outside" tets

   /**
    * Describes what method to use when generating distance values from
    * polygonal meshes. (Distance values generated from feature sets
    * presently used BRIDSON.)
    */
   public enum DistanceMethod {
      /**
       * Use distance and inclusion queries based on bounding volume
       * hierarchies. This is much more accurate but takes about twice as long
       * as the BRIDSON method.
       */
      BVH,

      /**
       * Use the sweep method devised by Robert Bridson (described in the class
       * API header).
       */
      BRIDSON
   }

   protected static boolean storeQuadCoefs = true;

   /**
    * Transform from quadratic grid to local coordinates.
    */
   protected VectorTransformer3d myQuadGridToLocal;
   
   /**
    * Transform from quadratic grid to world coordinates.
    */
   protected VectorTransformer3d myQuadGridToWorld;
   
   /**
    * True if the distances are signed
    */
   protected boolean mySignedP = false;

   protected int myQx;  // number of quad cells along X
   protected int myQy;  // number of quad cells along Y
   protected int myQz;  // number of quad cells along Z
   protected int myQxQy;// qx*qy

   /**
    * An array giving the index of the nearest Feature to each vertex.
    */
   protected int[] myClosestFeatureIdxs;
   protected Feature[] myFeatures;

   public static DistanceMethod DEFAULT_DISTANCE_METHOD = 
      DistanceMethod.BRIDSON;
   protected DistanceMethod myDistanceMethod = DEFAULT_DISTANCE_METHOD;

   /**
    * Sets the method used to compute distance values from polygonal meshes.
    * Specifying {@code null} sets the method to the default value,
    * which is {@link DistanceMethod#BRIDSON}.
    *
    * @param method distance method for polygonal meshes
    */
   public void setDistanceMethod (DistanceMethod method) {
      if (method == null) {
         method = DEFAULT_DISTANCE_METHOD;
      }
      myDistanceMethod = method;
   }

   /**
    * Returns the method used to compute distance values from polygonal meshes.
    *
    * @return distance method for polygonal meshes
    */
   public DistanceMethod getDistanceMethod() {
      return myDistanceMethod;
   }

   /**
    * Default constructor. Should not be called by applications, unless
    * {@link #scan} is called immediately after.
    */   
   public DistanceGrid () {
      super();
   }
   
   /**
    * {@inheritDoc}
    */
   public void setLocalToWorld (RigidTransform3d TLW) {
      super.setLocalToWorld (TLW);
      if (myQuadGridToLocal != null) {
         // might be null if grid is still being constructed
         updateQuadGridToWorld (myQuadGridToLocal);
      }
   }

   private void fitAABB (
      Vector3d widths, Vector3d center, double marginFrac, 
      Collection<List<? extends Feature>> featureSets, VectorTransformer3d TCL) {
      
      Vector3d min = new Vector3d (INF, INF, INF);
      Vector3d max = new Vector3d (-INF, -INF, -INF);
      Point3d p = new Point3d();
      for (List<? extends Feature> features : featureSets) {
         for (Feature f : features) {     
            for (int i=0; i<f.numPoints(); i++) {
               if (TCL != null) {
                  TCL.inverseTransformPnt (p, f.getPoint(i));
               }
               else {
                  p.set (f.getPoint(i));
               }
               p.updateBounds (min, max);
            }
         }
      }
      if (TCL == null) {
         widths.sub (max, min);
         center.add (max, min);
         center.scale (0.5);
      }
      else {
         max.absolute();
         min.absolute();
         widths.x = 2*Math.max(max.x, min.x);
         widths.y = 2*Math.max(max.y, min.y);
         widths.z = 2*Math.max(max.z, min.z);
         TCL.transformPnt (center, Vector3d.ZERO);
      }
      adjustWidths (widths, marginFrac);
   }
   
   private void fitOBB (
      Vector3d widths, RigidTransform3d TCL, double marginFrac, 
      Collection<List<? extends Feature>> featureSets, OBB.Method method) {
      
      int nfeats = 0;
      for (List<? extends Feature> features : featureSets) {
         nfeats += features.size();
      }
      Feature[] allFeats = new Feature[nfeats];
      int k = 0;
      for (List<? extends Feature> features : featureSets) {
         for (Feature f : features) {
            allFeats[k++] = f;
         }
      }
      OBB obb = new OBB();
      obb.set (allFeats, nfeats, 0, method);
      
      Vector3d halfWidths = new Vector3d();
      obb.getHalfWidths (halfWidths);
      widths.scale (2, halfWidths);
      adjustWidths (widths, marginFrac);

      obb.getTransform (TCL);
   }
   
   private void adjustWidths (Vector3d widths, double marginFrac) {
      double maxw = widths.maxElement();
      if (maxw == 0) {
         maxw = 1.0;
      }
      double minw = 0.05*maxw;
      if (widths.x < minw) {
         widths.x = minw;
      }
      if (widths.y < minw) {
         widths.y = minw;
      }
      if (widths.z < minw) {
         widths.z = minw;
      }
      widths.scale (1+2*marginFrac);
   }
   
   /**
    * Creates a new distance grid for a specified list of features,
    * axis-aligned and centered on the features, with a uniform cell width
    * along all axes defined so that the resolution along the maximum width
    * axis is <code>maxRes</code>. The grid is
    * created by calling 
    * <pre>
    *  computeFromFeatures (features, marginFrac, null, maxRes, signed);
    * </pre>
    * 
    * @param features features used to compute the distance field
    * @param marginFrac specifies the fractional amount that the
    * grid should be grown in each direction to better contain the features
    * @param maxRes specfies the resolution along the longest
    * width, with resolutions along other widths set to ensure uniform
    * cell size. Must be {@code >} 0.
    * @param signed if <code>true</code>, indicates that the field should be
    * signed. At present, signed fields can only be computed if all features
    * are faces (i.e., {@link Face}).
    */
   public DistanceGrid (
      List<? extends Feature> features, double marginFrac, int maxRes,
      boolean signed) {
      this();

      if (maxRes <= 0) {
         throw new IllegalArgumentException (
            "maxRes=" + maxRes + "; must be > 0");
      }
      computeFromFeatures (
         features, marginFrac, /*TCL=*/null, maxRes, signed);
      clearColors();
   }
   
   /**
    * Creates a new distance grid for a specified list of features,
    * axis-aligned and centered on the features, with a the cell resolution
    * along each axis given by <code>resolution</code>. The grid is
    * created by calling 
    * <pre>
    *  setResolution (resolution);
    *  computeFromFeatures (features, marginFrac, null, 0, signed);
    * </pre>
    * 
    * @param features features used to compute the distance field
    * @param marginFrac specifies the fractional amount that the
    * grid should be grown in each direction to better contain the features
    * @param resolution specfies the resolution along each of the
    * x, y, and z axes.
    * @param signed if <code>true</code>, indicates that the field should be
    * signed. At present, signed fields can only be computed if all features
    * are faces (i.e., {@link Face}).
    */
   public DistanceGrid (
      List<? extends Feature> features, double marginFrac, Vector3i resolution,
      boolean signed) {

      this();
      setResolution (resolution);
      computeFromFeatures (
         features, marginFrac, /*TCL=*/null, /*maxRes=*/0, signed);
      clearColors();
   }

   /**
    * Creates a new distance grid with specified widths, resolution, and
    * position and orientation of the center given by <code>TCL</code>.
    * The grid distances are initialized to zero.
    * 
    * @param widths widths along the x, y, and z axes
    * @param resolution cell resolution along the x, y, and z axes
    * @param TCL transformation giving the position and orientation
    * of the grid center
    */
   public DistanceGrid (
      Vector3d widths, Vector3i resolution, RigidTransform3d TCL) {
      initGrid (widths, resolution, TCL);
   }

   /**
    * Creates a new distance grid, axis-aligned and centered on the origin,
    * with the specified resolution and x, y, z widths set to 1.
    * The grid distances are initialized to zero.
    *
    * @param resolution cell resolution along the x, y, and z axes
    */
   public DistanceGrid (Vector3i resolution) {
      initGrid (null, resolution, null);
   }

   /**
    * Creates a new distance grid that is a copy of an existing grid.
    *
    * @param grid distance grid to copy
    */
   public DistanceGrid (DistanceGrid grid) {
      set (grid);
   }

   /**
    * Sets the resolution for this grid along the x, y, and z axes.
    * Resolutions are rounded up to be an even number, to allow quadratic
    * interpolation (and the {@code resolution} argument is adjusted
    * accordingly). If features are present for this grid, as described for
    * {@link #getFeatures}, then these are used to recompute the
    * distances. Otherwise, the distances are set to zero. If a color map is
    * present, it is cleared.
    *
    * @param resolution cell resolution along the x, y and z axes.  Must be at
    * least 1 along each axis.
    */
   public void setResolution (Vector3i resolution) {
      // round resolutions up to the nearest even number
      if ((resolution.x%2) == 1) {
         resolution.x++;
      }
      if ((resolution.y%2) == 1) {
         resolution.y++;
      }
      if ((resolution.z%2) == 1) {
         resolution.z++;
      }      
      int nvx = resolution.x+1;
      int nvy = resolution.y+1;
      int nvz = resolution.z+1;
      if (nvx != myNx || nvy != myNy || nvz != myNz) {

         super.setResolution (resolution);
         // update quad cell counts
         myQx = (nvx-1)/2;
         myQy = (nvy-1)/2;
         myQz = (nvz-1)/2;
         myQxQy = myQx*myQy;  

         myQuadCoefs = null;
         myTetIsOutside = null;
         if (myFeatures != null) {
            int[] closestFeatureIdxs = new int[numVertices()];
            calculatePhi (myValues, closestFeatureIdxs, myFeatures, mySignedP);
            myClosestFeatureIdxs = closestFeatureIdxs;
         }
      }
   }

   protected void initVertexValues (int numv) {
      super.initVertexValues (numv);
      myNormals = new Vector3d[numv];
   }

   protected void updateGridToLocal () {
      super.updateGridToLocal();
      Vector3d cwidths = getCellWidths();
      cwidths.scale (2.0);
      Vector3d origin = new Vector3d();
      if (myGridToLocal instanceof ScaledTranslation3d) {
         ((ScaledTranslation3d)myGridToLocal).getOrigin(origin);
         myQuadGridToLocal = new ScaledTranslation3d (cwidths, origin);
      }
      else {
         ((ScaledRigidTransformer3d)myGridToLocal).getOrigin(origin);
         myQuadGridToLocal =
            new ScaledRigidTransformer3d (cwidths, myTCL.R, origin);
      }
      updateQuadGridToWorld (myQuadGridToLocal);
   }

   /**
    * Sets this distance grid to be a copy of an existing grid.
    *
    * @param grid distance grid to copy
    */
   public void set (DistanceGrid grid) {

      super.set (grid);
      if (grid.myNormals != null) {
         myNormals = new Vector3d[grid.myNormals.length];
         for (int i=0; i<myNormals.length; i++) {
            if (grid.myNormals[i] != null) {
               myNormals[i] = new Vector3d (grid.myNormals[i]);
            }
         }
      }
      else {
         myNormals = null;
      }
      
      myQuadCoefs = null;    // will be recomputed on demand
      myTetIsOutside = null; // will be recomputed on demand
      myTets = null;         // will be recomputed on demand
      
      if (grid.myQuadGridToLocal != null) {
         myQuadGridToLocal = grid.myQuadGridToLocal.copy();
      }
      else {
         myQuadGridToLocal = null;
      }

      if (grid.myQuadGridToWorld != null) {
         myQuadGridToWorld = grid.myQuadGridToWorld.copy();
      }
      else {
         myQuadGridToWorld = null;
      }

      mySignedP = grid.mySignedP;

      myQx = grid.myQx;
      myQy = grid.myQy;
      myQz = grid.myQz;
      myQxQy = grid.myQxQy;

      if (grid.myClosestFeatureIdxs != null) {
         myClosestFeatureIdxs = Arrays.copyOf (
            grid.myClosestFeatureIdxs, grid.myClosestFeatureIdxs.length);
      }
      else {
         myClosestFeatureIdxs = null;
      }
      if (grid.myFeatures != null) {
         myFeatures = Arrays.copyOf (
            grid.myFeatures, grid.myFeatures.length);
      }
      else {
         myFeatures = null;
      }
   }

   private void clearNormals() {
      for (int i=0; i<myNormals.length; i++) {
         myNormals[i] = null;
      }
   }

   private final double sqr (double x) {
      return x*x;
   }

   /**
    * Computes the distance field for this grid, based on the nearest distances
    * to a supplied set of features. These features are stored internally and
    * can be later retrieved using {@link #getFeatures}. The grid resolution,
    * widths, center and orientation remain unchanged.
    *
    * @param features features used to compute the distance field
    * @param signed if <code>true</code>, indicates that the field should be
    * signed. At present, signed fields can only be computed if all features
    * are faces (i.e., {@link Face}).
    */
   public void computeDistances (
      List<? extends Feature> features, boolean signed) {
      
      calculatePhi (features, signed);
   }
   
   public void computeDistances (
      PolygonalMesh mesh, boolean signed) {
      
      calculatePhi (mesh, signed);
   }
   
   /**
    * Explicitly sets the distance field for this grid, by setting the distance
    * values at each vertex. The internal feature cache and nearest feature
    * setting for each vertex are cleared.
    *
    * <p>The input array is indexed such that for vertex indices xi, yj, zk,
    * the corresponding index into this array is
    * <pre>
    * idx = xi + nx*yj + (nx*ny)*zk
    * </pre>
    * where <code>nx</code> and <code>ny</code> are the number
    * of vertices along x and y axes.
    *
    * @param distances distance for each vertex. Must have a length
    * {@code >=} {@link #numVertices}.
    * @param signed if <code>true</code>, indicates that the field should be
    * considered signed.
    */
   public void setVertexDistances (
      double[] distances, boolean signed) {
      super.setVertexValues (distances);
      myQuadCoefs = null;
      myTetIsOutside = null;
      clearNormals();
      clearFeatures();
      mySignedP = signed;
   }

   /**
    * Returns the full array of distances at each vertex. See {@link
    * #setVertexDistances} for a description of how vertices are indexed with
    * respect to this array.
    * 
    * @return array of distances, or <code>null</code> if distances have
    * not yet been set.
    */
   public double[] getVertexDistances() {
      return super.getVertexValues();
   }
   
   /**
    * Queries the distance value at the vertex indexed by {@code vi}. See
    * {@link #setVertexDistances} for a description of how vertices are
    * indexed.
    * 
    * @param vi vertex index
    * @return distance value at the vertex
    */
   protected double getVertexDistance (int vi) {
      return super.getVertexValue (vi);
   }

   /**
    * Queries the distance value at a specified vertex, as specified by x, y, z
    * indices.
    * 
    * @param vxyz x, y, z vertex indices
    * @return distance value at the vertex
    */
   public double getVertexDistance (Vector3i vxyz) {
      return super.getVertexValue (vxyz);
   }
   
   /**
    * Queries the distance value at a specified vertex, as specified by x, y, z
    * indices.
    * 
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return distance value at the vertex
    */   
   protected double getVertexDistance (int xi, int yj, int zk) {
      return super.getVertexValue (xi, yj, zk);
   }

   /**
    * Explicitly zeros the distance field for this grid. The internal feature
    * cache and nearest feature setting for each vertex are cleared.  The
    * <i>signed</i> property is set to <code>false</code>.
    */
   public void zeroVertexDistances () {
      super.zeroVertexValues();
      myQuadCoefs = null;
      myTetIsOutside = null;
      clearNormals();
      clearFeatures();
      mySignedP = false;
   }
   
   /**
    * Explicitly sets the distance field and features for this grid.
    *
    * @param distances distance for each vertex. Must have a length
    * {@code >=} {@link #numVertices}.
    * @param features list of features to be associated with this
    * grid. 
    * @param closestFeatures index (with respect to <code>features</code>(
    * of the nearest feature to each vertex. Must have a length
    * {@code >=} {@link #numVertices}.
    * @param signed if <code>true</code>, indicates that the field should be
    * considered signed.
    */
   public void setDistancesAndFeatures (
      double[] distances,
      List<? extends Feature> features, int[] closestFeatures, boolean signed) {

      int numv = numVertices();

      if (distances.length < numv) {
         throw new IllegalArgumentException (
            "distances.length=" + distances.length +
            "; must be >= num vertices ("+numv+")");
      }
      if (closestFeatures.length < numv) {
         throw new IllegalArgumentException (
            "closestFeatures.length=" + closestFeatures.length +
            "; must be >= num vertices ("+numv+")");
      }
      myFeatures = features.toArray(new Feature[0]);
      myClosestFeatureIdxs = new int[numv];
      for (int i=0; i<numv; i++) {
         myValues[i] = distances[i];
         int idx = closestFeatures[i];
         if (idx < 0 || idx >= myFeatures.length) {
            throw new IllegalArgumentException (
               "closest feature index "+idx+" at vertex "+i+" is out of range: "+
               "must be within [0, "+(myFeatures.length-1)+"]");
         }
         myClosestFeatureIdxs[i] = idx;
      }
      myQuadCoefs = null;
      myTetIsOutside = null;
      clearNormals();
      mySignedP = signed;
      myRobValid = false;
   }
   
   private void updateQuadGridToWorld (VectorTransformer3d quadGridToLocal) {
      if (myTLocalToWorld == null) {
         myQuadGridToWorld = quadGridToLocal;
      }
      else {
         Vector3d scaling = new Vector3d();
         RigidTransform3d TGW = new RigidTransform3d();
         // start by loading TGW with TquadGridToLocal
         if (quadGridToLocal instanceof ScaledTranslation3d) {
            ScaledTranslation3d XGL = (ScaledTranslation3d)quadGridToLocal;
            XGL.getOrigin (TGW.p);
            XGL.getScaling (scaling);
         }
         else {
            // quadGridToLocal is instance of ScaledRigidTransformer3d 
            ScaledRigidTransformer3d XGL =
               (ScaledRigidTransformer3d)quadGridToLocal;
            XGL.getRigidTransform (TGW);
            XGL.getScaling (scaling);
         }
         TGW.mul (myTLocalToWorld, TGW);
         myQuadGridToWorld = new ScaledRigidTransformer3d (scaling, TGW);
      }
   }      
   
   /**
    * Fits this grid to a set of features and computes the corresponding
    * distance field. These features are stored internally and can be later
    * retrieved using {@link #getFeatures}. The existing grid resolution is
    * unchanged, <i>unless</i> <code>maxRes</code> exceeds 0, in which
    * case the resolution along the longest width is set to <code>maxRes</code>
    * and the resolution along all other axes are set so as to ensure
    * a uniform cell size. All resolutions are rounded up to an even number.
    * 
    * <p>The points of the features are assumed to be given in local
    * coordinates. The application has the option of specifying the
    * position and orientation of the grid center using the optional
    * argument <code>TCL</code>. If this is non-null, then the widths
    * are set large enough about the specified center to accomodate
    * the features. Otherwise, if <code>TCL</code> is null, then
    * the grid is fit in an axis-aligned manner to the local coordinate
    * frame (i.e., {@link #getOrientation} returns the identity), with
    * a center and widths computed to best fit the features.
    *
    * <p>To ensure that the grid is not degenerate, all widths are adjusted
    * if necessary to ensure that they are at least 0.05 of the maximum
    * width. Widths are then further adjusted by multiplying by
    * <pre>
    * (1 + 2*marginFrac)
    * </pre>
    * to provide a margin around the features. Finally,
    * if {@code maxRes > 0},
    * widths may be grown to ensure uniform
    * cell size.
    * 
    * @param features features used to compute the distance field
    * @param marginFrac specifies the fractional amount that the
    * grid should be grown in each direction to better contain the features
    * @param TCL optional - if non-null, specifies the pose of the grid center
    * @param maxRes if {@code > 0},
    * specifies the resolution along the longest
    * width, with resolutions along other widths set to ensure uniform
    * cell size
    * @param signed if <code>true</code>, indicates that the field should be
    * signed. At present, signed fields can only be computed if all features
    * are faces (i.e., {@link Face}).
    */
   public void computeFromFeatures (
      List<? extends Feature> features, 
      double marginFrac, RigidTransform3d TCL, int maxRes, boolean signed) {
      
      fitToFeatures (features, marginFrac, TCL, maxRes);
      calculatePhi (features, signed);      
   }
   
   public void computeFromMesh (
      PolygonalMesh mesh, 
      double marginFrac, RigidTransform3d TCL, int maxRes, boolean signed) {
      
      fitToFeatures (mesh.getFaces(), marginFrac, TCL, maxRes);
      calculatePhi (mesh, signed);      
   }    

   /**
    * Fits the widths and center of this grid to a set of features. The way in
    * which this is done is the same as for {@link #computeFromFeatures};
    * however, no distance field is computed and the features are not stored.
    *
    * @param features features used to fit the grid
    * @param marginFrac specifies the fractional amount that the
    * grid should be grown in each direction to better contain the features
    * @param TCL optional - if non-null, specifies the pose of the grid center
    * @param maxRes if {@code > 0},
    * specifies the resolution along the longest
    * width, with resolutions along other widths set to ensure uniform
    * cell size
    */
   public void fitToFeatures (
      List<? extends Feature> features, double marginFrac,
      RigidTransform3d TCL, int maxRes) {

      ArrayList<List<? extends Feature>> featureList =
         new ArrayList<List<? extends Feature>>();
      featureList.add (features);
      fitToFeatures (featureList, marginFrac, TCL, maxRes);
   }

   /**
    * Fits the widths and center of this grid to a several sets of
    * features. The way in which this is done is the same as for {@link
    * #computeFromFeatures}; however, no distance field is computed and the
    * features are not stored.
    *
    * @param featureSets lists of features used to fit the grid
    * @param marginFrac specifies the fractional amount that the
    * grid should be grown in each direction to better contain the features
    * @param TCL optional - if non-null, specifies the pose of the grid center
    * @param maxRes if {@code > 0},
    * specifies the resolution along the longest
    * width, with resolutions along other widths set to ensure uniform
    * cell size
    */
   public void fitToFeatures (
      Collection<List<? extends Feature>> featureSets, double marginFrac,
      RigidTransform3d TCL, int maxRes) {

      Vector3d widths = new Vector3d();
      Vector3d center = new Vector3d();
      fitAABB (widths, center, marginFrac, featureSets, TCL);
      if (maxRes > 0) {
        double cwidth = widths.maxElement()/maxRes;
        Vector3i resolution = new Vector3i (
           (int)(Math.ceil(widths.x/cwidth)),
           (int)(Math.ceil(widths.y/cwidth)),
           (int)(Math.ceil(widths.z/cwidth)));
        setResolution (resolution);       
        // update widths and origin to accommodate uniform cell width
        widths.set (resolution);
        widths.scale (cwidth);       
      }
      setWidths (widths);
      if (TCL == null) {
         TCL = new RigidTransform3d (center.x, center.y, center.z);
      }
      setCenterAndOrientation (TCL);
   }

   /**
    * Fits this grid in an oriented manner to a set of features and computes
    * the corresponding distance field. These features are stored internally
    * and can be later retrieved using {@link #getFeatures}. The existing grid
    * resolution is unchanged, <i>unless</i> <code>maxRes</code> exceeds 0, in
    * which case the resolution along the longest width is set to
    * <code>maxRes</code> and the resolution along all other axes are set so as
    * to ensure a uniform cell size. All resolutions are rounded up to an even
    * number.
    * 
    * <p>The points of the features are assumed to be given in local
    * coordinates. The grid is fit to an oriented bounding box (OBB) with
    * respect to this frame; {@link #getCenter} and {@link #getOrientation} will
    * return the center and orientation of this box. To ensure that the grid is
    * not degenerate, all widths are adjusted if necessary to ensure that they
    * are at least 0.05 of the maximum width. Widths are then further adjusted
    * by multiplying by <pre> (1 + 2*marginFrac) </pre> to provide a margin
    * around the features.
    * 
    * @param features features used to compute the distance field
    * @param marginFrac specifies the fractional amount that the
    * grid should be grown in each direction to better contain the features
    * @param maxRes if {@code >} 0, specfies the resolution along the longest
    * width, with resolutions along other widths set to ensure uniform
    * cell size
    * @param signed if <code>true</code>, indicates that the field should be
    * signed. At present, signed fields can only be computed if all features
    * are faces (i.e., {@link Face}).
    */
   public void computeFromFeaturesOBB (
      List<? extends Feature> features, 
      double marginFrac, int maxRes, boolean signed) {

      fitToFeaturesOBB (features, marginFrac, maxRes);      
      calculatePhi (features, signed);      
   }

   /**
    * Fits the widths, center and local orientation of this grid to a set of
    * features. The way in which this is done is the same as for {@link
    * #computeFromFeaturesOBB}; however, no distance field is computed and the
    * features are not stored.
    *
    * @param features features used to fit the grid
    * @param marginFrac specifies the fractional amount that the
    * grid should be grown in each direction to better contain the features
    * @param maxRes if {@code >} 0, specfies the resolution along the longest
    * width, with resolutions along other widths set to ensure uniform
    * cell size
    */
   public void fitToFeaturesOBB (
      List<? extends Feature> features, double marginFrac, int maxRes) {

      ArrayList<List<? extends Feature>> featureList =
         new ArrayList<List<? extends Feature>>();
      featureList.add (features);
      fitToFeaturesOBB (featureList, marginFrac, maxRes);
   }

   /**
    * Fits the widths, center and local orientation of this grid to several
    * sets of features. The way in which this is done is the same as for {@link
    * #computeFromFeaturesOBB}; however, no distance field is computed and the
    * features are not stored.
    *
    * @param featureSets lists of features used to fit the grid
    * @param marginFrac specifies the fractional amount that the
    * grid should be grown in each direction to better contain the features
    * @param maxRes if {@code >} 0, specfies the resolution along the longest
    * width, with resolutions along other widths set to ensure uniform
    * cell size
    */
   public void fitToFeaturesOBB (
      Collection<List<? extends Feature>> featureSets,
      double marginFrac, int maxRes) {
      
      Vector3d widths = new Vector3d();
      RigidTransform3d TCL = new RigidTransform3d();
      fitOBB (widths, TCL, marginFrac, featureSets, OBB.Method.Covariance);
      if (maxRes > 0) {
        double cwidth = widths.maxElement()/maxRes;
        Vector3i resolution = new Vector3i (
           (int)(Math.ceil(widths.x/cwidth)),
           (int)(Math.ceil(widths.y/cwidth)),
           (int)(Math.ceil(widths.z/cwidth)));
        setResolution (resolution);       
        // update widths and origin to accommodate uniform cell width
        widths.set (resolution);
        widths.scale (cwidth);       
      }      
      setWidths (widths);
      setCenterAndOrientation (TCL);
   }      

   private int clip (int x, int min, int max) {
      if (x < min) {
         return min;
      }
      else if (x > max) {
         return max;
      }
      else {
         return x;
      }
   }

   void calculatePhi (
      List<? extends Feature> features, boolean signed) {
      
      Feature[] featArray = features.toArray(new Feature[0]);
      int numv = numVertices();
      int[] closestFeatureIdxs = new int[numv];
      calculatePhi (myValues, closestFeatureIdxs, featArray, signed);
      myQuadCoefs = null;
      myTetIsOutside = null;
      clearNormals();
      myClosestFeatureIdxs = closestFeatureIdxs;
      myFeatures = featArray;
      mySignedP = signed;
   }

   void calculatePhi (
      PolygonalMesh mesh, boolean signed) {
      
      Feature[] featArray = mesh.getFaces().toArray(new Feature[0]);
      int numv = numVertices();
      int[] closestFeatureIdxs = new int[numv];
      if (myDistanceMethod == DistanceMethod.BRIDSON) {
         calculatePhi (myValues, closestFeatureIdxs, featArray, signed);
      }
      else {
         calculatePhi (myValues, closestFeatureIdxs, mesh, signed);
      }
      myQuadCoefs = null;
      myTetIsOutside = null;
      clearNormals();
      myClosestFeatureIdxs = closestFeatureIdxs;
      myFeatures = featArray;
      mySignedP = signed;
   }

   protected void calculateDistances (
      double[] values, PolygonalMesh mesh, boolean signed) {
      if (myDistanceMethod == DistanceMethod.BRIDSON) {
         Feature[] featArray = mesh.getFaces().toArray(new Feature[0]);
         calculatePhi (values, null, featArray, signed);
      }
      else {
         calculatePhi (values, null, mesh, signed);
      }
   }

   public void computeUnion (PolygonalMesh mesh) {
      int numv = numVertices();
      double[] phiNew = new double[numv];
      calculateDistances (phiNew, mesh, /*signed=*/true);
      for (int i=0; i<numv; i++) {
         myValues[i] = Math.min (myValues[i], phiNew[i]);
      }
      myQuadCoefs = null;
      myTetIsOutside = null;
      clearNormals();
      myFeatures = null;
      mySignedP = true;
   }
   
   public void computeIntersection (PolygonalMesh mesh) {
      int numv = numVertices();
      double[] phiNew = new double[numv];
      calculateDistances (phiNew, mesh, /*signed=*/true);
      for (int i=0; i<numv; i++) {
         myValues[i] = Math.max (myValues[i], phiNew[i]);
      }
      myQuadCoefs = null;
      myTetIsOutside = null;
      clearNormals();
      myFeatures = null;
      mySignedP = true;
   }
   
   public void computeDifference01 (PolygonalMesh mesh) {
      int numv = numVertices();
      double[] phiNew = new double[numv];
      calculateDistances (phiNew, mesh, /*signed=*/true);
      for (int i=0; i<numv; i++) {
         myValues[i] = Math.max (myValues[i], -phiNew[i]);
      }
      myQuadCoefs = null;
      myTetIsOutside = null;
      clearNormals();
      myFeatures = null;
      mySignedP = true;
   }
   
   public void computeDifference10 (PolygonalMesh mesh) {
      int numv = numVertices();
      double[] phiNew = new double[numv];
      calculateDistances (phiNew, mesh, /*signed=*/true);
      for (int i=0; i<numv; i++) {
         myValues[i] = Math.max (-myValues[i], phiNew[i]);
      }
      myQuadCoefs = null;
      myTetIsOutside = null;
      clearNormals();
      myFeatures = null;
      mySignedP = true;
   }
   
   /** 
    * Calculates the distance field.
    */
   void calculatePhi (
      double[] phi, int[] closestFeatureIdxs,
      PolygonalMesh mesh, boolean signed) {
      
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      double maxDist = 2*getRadius();
      int numv = myNx*myNy*myNz;

      if (phi.length != numv ||
          (closestFeatureIdxs != null && closestFeatureIdxs.length != numv)) {
         throw new InternalErrorException (
            "length of phi and/or closestFeatureIdxs != numVertices()");
      }
      
      for (int k = 0; k < numv; k++) {
         phi[k] = maxDist;
      }

      // The index of closestFeature matches with phi.
      // Each entry in closestFeature is the index of the closest 
      // Feature to the grid vertex.
      if (closestFeatureIdxs == null) {
         closestFeatureIdxs = new int[numv];
      }
      for (int i = 0; i < numv; i++) {
         closestFeatureIdxs[i] = -1;
      }

      BVFeatureQuery query = new BVFeatureQuery();
      Point3d pnt = new Point3d();
      Point3d near = new Point3d();
      for (int xi = 0; xi < myNx; xi++) {
         for (int yj = 0; yj < myNy; yj++) {
            for (int zk = 0; zk < myNz; zk++) {
               int vi = xyzIndicesToVertex (xi, yj, zk);
               myGridToLocal.transformPnt (pnt, new Vector3d (xi, yj, zk));
               myLocalToWorld.transformPnt (pnt, pnt);

               boolean inside = false;
               Face face = null;
               if (signed) {
                  // inside = query.isInsideOrientedMesh (mesh, pnt, -1);
                  // face = query.getFaceForInsideOrientedTest (near, null);
                  // if (face == null) {
                  //    // face wasn't computed because isInsideOrientedMesh() was
                  //    // able to determine the point was outside without it
                  //    face = query.nearestFaceToPoint (near, null, mesh, pnt);
                  // }
                  InsideQuery res = query.isInsideMesh (mesh, pnt);
                  if (res == InsideQuery.UNSURE) {
                     System.out.println ("UNSURE");
                  }
                  inside = (res == InsideQuery.INSIDE);
                  face = query.nearestFaceToPoint (near, null, mesh, pnt);
               }
               else {
                  face = query.nearestFaceToPoint (near, null, mesh, pnt);
               }
               if (face != null) {
                  closestFeatureIdxs[vi] = face.getIndex();
                  double d = near.distance (pnt);
                  phi[vi] = (inside ? -d : d);

               }
            }
         }
      }   
      timer.stop();
      System.out.println ("BVH: " + timer.result(1));
   }

   /** 
    * Calculates the distance field.
    */
   void calculatePhi (
      double[] phi, int[] closestFeatureIdxs,
      Feature[] features, boolean signed) {
      
      //FunctionTimer timer = new FunctionTimer();
      //timer.start();
      
      double maxDist = 2*getRadius();
      int numv = myNx*myNy*myNz;

      if (phi.length != numv ||
          (closestFeatureIdxs != null && closestFeatureIdxs.length != numv)) {
         throw new InternalErrorException (
            "length of phi and/or closestFeatureIdxs != numVertices()");
      }
      
      for (int k = 0; k < numv; k++) {
         phi[k] = maxDist;
      }

      // The index of closestFeature matches with phi.
      // Each entry in closestFeature is the index of the closest 
      // Feature to the grid vertex.
      if (closestFeatureIdxs == null) {
         closestFeatureIdxs = new int[numv];
      }
      for (int i = 0; i < numv; i++) {
         closestFeatureIdxs[i] = -1;
      }
      
      int zIntersectCount[] = null;
      if (signed) {
         zIntersectCount = new int[numv];
         for (int i = 0; i < numv; i++) {
            zIntersectCount[i] = 0;
         }
      }
      Point3d featPnt    = new Point3d();
      Point3d gridPnt    = new Point3d();
      Vector3i gridMin   = new Vector3i();
      Vector3i gridMax   = new Vector3i();
      Point3d nearPntLoc = new Point3d();
      Point3d featPntLoc = new Point3d();
      Vector3i hi = new Vector3i();
      Vector3i lo = new Vector3i();

      // For every feature ...
      for (int t=0; t<features.length; ++t) {
         // Find the vertex-aligned parallelpiped containing the feature's
         // bounding box.
         Feature feature = features[t];

         gridMin.set (myNx+1, myNy+1, myNz+1);
         gridMax.set (-1, -1, -1);
         double maxz = -INF; // max, minz of the feature in grid coords
         double minz = INF;
         for (int i=0; i<feature.numPoints(); i++) {
            featPnt = feature.getPoint(i);
            
            myGridToLocal.inverseTransformPnt (gridPnt, featPnt);

            lo.x = clip((int)gridPnt.x, 0, myNx-1);
            lo.y = clip((int)gridPnt.y, 0, myNy-1);
            lo.z = clip((int)gridPnt.z, 0, myNz-1);
            hi.x = clip((int)(gridPnt.x+1), 0, myNx-1);
            hi.y = clip((int)(gridPnt.y+1), 0, myNy-1);
            hi.z = clip((int)(gridPnt.z+1), 0, myNz-1);
            if (gridPnt.z < minz) {
               minz = gridPnt.z;
            }
            if (gridPnt.z > maxz) {
               maxz = gridPnt.z;
            }
            lo.updateBounds (gridMin, gridMax);
            hi.updateBounds (gridMin, gridMax);
         }

         // Go through the entire parallelpiped. Calculate distance and
         // closestFeature.
         for (int zk = gridMin.z; zk <= gridMax.z; zk++) {
            for (int yj = gridMin.y; yj <= gridMax.y; yj++) {
               for (int xi = gridMin.x; xi <= gridMax.x; xi++) {
                  // Get features coordinates
                  featPntLoc.set (xi, yj, zk);
                  myGridToLocal.transformPnt (featPntLoc, featPntLoc);
                  // Get the distance from this point to the Feature.
                  feature.nearestPoint (nearPntLoc, featPntLoc);
                  double distance = featPntLoc.distance (nearPntLoc);
                  int index = xyzIndicesToVertex (xi, yj, zk);
                  if (index >= numv) {
                     System.out.println ("coords = " +xi+" "+yj+" "+zk);
                  }
                  if (distance < phi[index]) {
                     phi[index] = distance;
                     if (closestFeatureIdxs != null) {
                        closestFeatureIdxs [index] = t;
                     }
                  }
               }
            }
         }

         if (signed) {
            if (!(feature instanceof Face)) {
               throw new IllegalArgumentException (
                  "Signed grid can only be created if all features are Faces");
            }
            Face face = (Face)feature;
            Point3d bot = new Point3d();
            Point3d top = new Point3d();
            // Ray-casts from bottom x-y plane, upwards, counting intersections.
            // We're building intersectionCount[] to use in ray casting below.
            for (int yj = gridMin.y; yj <= gridMax.y; yj++) {
               for (int xi = gridMin.x; xi <= gridMax.x; xi++) {

                  Point3d ipnt = new Point3d();
                  int res = 0;
                  if (maxz >= 0) {
                     myGridToLocal.transformPnt (
                        bot, new Point3d (xi, yj, minz-1));
                     myGridToLocal.transformPnt (
                        top, new Point3d (xi, yj, maxz+1));
                     
                     res = RobustPreds.intersectSegmentTriangle (
                        ipnt, bot, top, face, maxDist, /*worldCoords=*/false);
                  }
                  
                  if (res > 0) {
                     myGridToLocal.inverseTransformPnt (gridPnt, ipnt);
                     int zInterval = clip((int)Math.ceil(gridPnt.z), 0, myNz-1);
                     ++zIntersectCount [xyzIndicesToVertex (xi, yj, zInterval)];
                  } // point in triangle
               } // x
            } // y 
         }
      }

      // Done all triangles.
      // Sweep, propagating values throughout the grid volume.
      for (int pass = 0; pass < 2; pass++) {
         sweep(phi, +1, +1, +1, closestFeatureIdxs, features);
         sweep(phi, -1, -1, -1, closestFeatureIdxs, features);
         sweep(phi, +1, +1, -1, closestFeatureIdxs, features);
         sweep(phi, -1, -1, +1, closestFeatureIdxs, features);
         sweep(phi, +1, -1, +1, closestFeatureIdxs, features);
         sweep(phi, -1, +1, -1, closestFeatureIdxs, features);
         sweep(phi, +1, -1, -1, closestFeatureIdxs, features);
         sweep(phi, -1, +1, +1, closestFeatureIdxs, features);
      }

      if (signed) {
         // This is a ray-casting implementation to find the sign of each
         // vertex in the grid.
         for (int xi = 0; xi < myNx; xi++) {
            for (int yj = 0; yj < myNy; yj++) {
               int total_count = 0;
               //Count the intersections of the x axis
               for (int zk = 0; zk < myNz; zk++) {
                  int index = xyzIndicesToVertex (xi, yj, zk);
                  total_count += zIntersectCount [index];
                  
                  // If parity of intersections so far is odd, we are inside the 
                  // mesh.
                  if (total_count % 2 == 1) {
                     phi[index] =- phi[index];
                  }
               }
            }
         }         
      }
      //timer.stop();
      //System.out.println ("BRIDSON: " + timer.result(1));
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
      if (x == myNx - 1) {
         nrm.x = getVertexValue (x, y, z) - getVertexValue (x-1, y, z);
      }
      else if (x == 0) {
         nrm.x = getVertexValue (x+1, y, z) - getVertexValue (x, y, z);
      }
      else {
         nrm.x = getVertexValue (x+1, y, z) - getVertexValue (x-1, y, z);
         nrm.x *= 0.5;
      }
      //********************************************************************
      if (y == myNy - 1) {
         nrm.y = getVertexValue (x, y, z) - getVertexValue (x, y-1, z) ;
      }
      else if (y == 0) {
         nrm.y = getVertexValue (x, y+1, z) - getVertexValue (x, y, z);
      }
      else {
         nrm.y = getVertexValue (x, y+1, z) - getVertexValue (x, y-1, z);
         nrm.y *= 0.5;            
      }
      //********************************************************************
      if (z == myNz - 1) {
         nrm.z = getVertexValue (x, y, z) - getVertexValue (x, y, z-1);
      }
      else if (z == 0) {
         nrm.z = getVertexValue (x, y, z+1) - getVertexValue (x, y, z);
      }
      else {
         nrm.z = getVertexValue (x, y, z+1) - getVertexValue (x, y, z-1);
         nrm.z *= 0.5;
      }
      //********************************************************************
      myGridToLocal.transformCovec (nrm, nrm);
      nrm.normalize ();
      myNormals[xyzIndicesToVertex (x, y, z)] = nrm;
      return nrm;
   }

   /** 
    * Calculates the distance at an arbitrary point in local coordinates using
    * multilinear interpolation of the vertex values for the grid cell
    * containing the point.  If the point lies outside the grid volume, {@link
    * #OUTSIDE_GRID} is returned.
    *
    * @param point point at which to calculate the normal and distance
    * (local coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getLocalDistance (Point3d point) {
      return super.getLocalValue (point);
   }

   /** 
    * Calculates the distance at an arbitrary point in world coordinates using
    * multilinear interpolation of the vertex values for the grid cell
    * containing the point.  If the point lies outside the grid volume, {@link
    * #OUTSIDE_GRID} is returned.
    *
    * @param point point at which to calculate the normal and distance
    * (world coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getWorldDistance (Point3d point) {
      return super.getWorldValue (point);
   }

   /** 
    * Calculates the distance and normal at an arbitrary point in local
    * coordinates using multilinear interpolation, as described
    * for {@link #getLocalDistanceAndNormal(Vector3d,Point3d)}.
    * If the point lies outside
    * the grid volume, {@link #OUTSIDE_GRID} is returned.
    *
    * @param norm returns the normal (local coordinates)
    * @param x x coordinate of point (local coordinates).
    * @param y y coordinate of point (local coordinates).
    * @param z z coordinate of point (local coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getLocalDistanceAndNormal (
      Vector3d norm, double x, double y, double z) {
      Point3d point = new Point3d (x, y, z);
      return getLocalDistanceAndNormal (norm, null, point);
   }
   
   /** 
    * Calculates the distance and normal at an arbitrary point in world
    * coordinates using multilinear interpolation, as described
    * for {@link #getLocalDistanceAndNormal(Vector3d,Point3d)}.
    * If the point lies outside
    * the grid volume, {@link #OUTSIDE_GRID} is returned.
    *
    * @param norm returns the normal (world coordinates)
    * @param x x coordinate of point (world coordinates).
    * @param y y coordinate of point (world coordinates).
    * @param z z coordinate of point (world coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getWorldDistanceAndNormal (
      Vector3d norm, double x, double y, double z) {
      Point3d ploc = new Point3d(x,y,z);
      myLocalToWorld.inverseTransformPnt (ploc, ploc);
      double d = getLocalDistanceAndNormal(norm, null, ploc);
      // transform normal by inverse transform
      if (norm != null) {
         myLocalToWorld.transformCovec(norm, norm);
      }
      return d;
   }
   
   /** 
    * Calculates the distance and gradient at an arbitrary point in
    * local coordinates using multilinear interpolation, as described
    * for {@link #getLocalDistanceAndGradient(Vector3d,Point3d)}.
    * If the point lies outside
    * the grid volume, {@link #OUTSIDE_GRID} is returned.
    *
    * @param grad returns the gradient (local coordinates)
    * @param x x coordinate of point (local coordinates).
    * @param y y coordinate of point (local coordinates).
    * @param z z coordinate of point (local coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getLocalDistanceAndGradient(
      Vector3d grad, double x, double y, double z) {
      Point3d point = new Point3d(x, y, z);
      return getLocalDistanceAndGradient(grad, point);
   }
   
   /** 
    * Calculates the distance and gradient at an arbitrary point in
    * world coordinates using multilinear interpolation, as described
    * for {@link #getLocalDistanceAndGradient(Vector3d,Point3d)}.
    * If the point lies outside
    * the grid volume, {@link #OUTSIDE_GRID} is returned.
    *
    * @param grad returns the gradient (world coordinates)
    * @param x x coordinate of point (world coordinates).
    * @param y y coordinate of point (world coordinates).
    * @param z z coordinate of point (world coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getWorldDistanceAndGradient (
      Vector3d grad, double x, double y, double z) {
      Point3d point = new Point3d(x,y,z);
      return getWorldDistanceAndGradient(grad, point);
   }
   
   /** 
    * Calculates the distance and normal at an arbitrary point in local
    * coordinates using multilinear interpolation of the vertex values for the
    * grid cell containing the point. Both the distance and normal values are
    * determined this way, with normal values at the vertices being computed on
    * demand by mid-point differencing of distances at adjacent vertices. The
    * returned normal values do <i>not</i> therefore correspond to the gradient
    * of the distance field, but instead vary more smoothly across the grid.
    * If the point lies outside the grid volume, {@link #OUTSIDE_GRID} is
    * returned.
    *
    * @param norm returns the normal (local coordinates)
    * @param point point at which to calculate the normal and distance
    * (local coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getLocalDistanceAndNormal (
      Vector3d norm, Point3d point) {
      return getLocalDistanceAndNormal(norm, null, point);
   }
   
   /** 
    * Calculates the distance and normal at an arbitrary point in world
    * coordinates using multilinear interpolation, as described for {@link
    * #getLocalDistanceAndNormal(Vector3d,Point3d)}.
    * If the point lies outside
    * the grid volume, {@link #OUTSIDE_GRID} is returned.
    *
    * @param norm returns the normal (world coordinates)
    * @param point point at which to calculate the normal and distance
    * (world coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getWorldDistanceAndNormal (Vector3d norm, Point3d point) {
      Point3d lpnt = new Point3d();
      myLocalToWorld.inverseTransformPnt (lpnt, point);
      double d = getLocalDistanceAndNormal(norm, null, lpnt);
      if (norm != null && d != OUTSIDE_GRID) {
         myLocalToWorld.transformCovec(norm, norm);
      }
      return d;
   }

   /** 
    * Calculates the distance and normal at an arbitrary point in local
    * coordinates using multilinear interpolation, as described for {@link
    * #getLocalDistanceAndNormal(Vector3d,Point3d)}. If <code>Dnrm</code> is
    * non-<code>null</code>, the method also calculated the normal derivative.
    * If the point lies outside the grid volume, {@link #OUTSIDE_GRID} is
    * returned.
    *
    * @param norm returns the normal (local coordinates)
    * @param Dnrm if non-null, returns the normal derivative (local coordinates)
    * @param point point at which to calculate the normal and distance
    * (local coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getLocalDistanceAndNormal (
      Vector3d norm, Matrix3d Dnrm, Point3d point) {

      Vector3d coords = new Vector3d();
      Vector3i vidx = new Vector3i();
      if (!getCellCoords (vidx, coords, point)) {
         return OUTSIDE_GRID;
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

      double d000  = getVertexValue (vidx.x  , vidx.y  , vidx.z  );
      double d001  = getVertexValue (vidx.x  , vidx.y  , vidx.z+1);
      double d010  = getVertexValue (vidx.x  , vidx.y+1, vidx.z  );
      double d011  = getVertexValue (vidx.x  , vidx.y+1, vidx.z+1);
      double d100  = getVertexValue (vidx.x+1, vidx.y  , vidx.z  );
      double d101  = getVertexValue (vidx.x+1, vidx.y  , vidx.z+1);
      double d110  = getVertexValue (vidx.x+1, vidx.y+1, vidx.z  );
      double d111  = getVertexValue (vidx.x+1, vidx.y+1, vidx.z+1);

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

            Vector3d row = new Vector3d();

            // top row of Dnrm 
            row.x = 
               (-w100x*n000.x - w101x*n001.x - w110x*n010.x - w111x*n011.x
                +w100x*n100.x + w101x*n101.x + w110x*n110.x + w111x*n111.x);
            row.y = 
               (-w010y*n000.x - w011y*n001.x + w010y*n010.x + w011y*n011.x
                -w110y*n100.x - w111y*n101.x + w110y*n110.x + w111y*n111.x);
            row.z =
               (-w001z*n000.x + w001z*n001.x - w011z*n010.x + w011z*n011.x
                -w101z*n100.x + w101z*n101.x - w111z*n110.x + w111z*n111.x);

            myGridToLocal.transformCovec (row, row);
            row.scale (d);

            Dnrm.m00 = row.x;
            Dnrm.m01 = row.y;
            Dnrm.m02 = row.z;

            // middle row of Dnrm

            row.x =
               (-w100x*n000.y - w101x*n001.y - w110x*n010.y - w111x*n011.y
                +w100x*n100.y + w101x*n101.y + w110x*n110.y + w111x*n111.y);
            row.y = 
               (-w010y*n000.y - w011y*n001.y + w010y*n010.y + w011y*n011.y
                -w110y*n100.y - w111y*n101.y + w110y*n110.y + w111y*n111.y);
            row.z = 
               (-w001z*n000.y + w001z*n001.y - w011z*n010.y + w011z*n011.y
                -w101z*n100.y + w101z*n101.y - w111z*n110.y + w111z*n111.y);

            myGridToLocal.transformCovec (row, row);
            row.scale (d);

            Dnrm.m10 = row.x;
            Dnrm.m11 = row.y;
            Dnrm.m12 = row.z;

            // bottom row of Dnrm

            row.x = 
               (-w100x*n000.z - w101x*n001.z - w110x*n010.z - w111x*n011.z
                +w100x*n100.z + w101x*n101.z + w110x*n110.z + w111x*n111.z);

            row.y = 
               (-w010y*n000.z - w011y*n001.z + w010y*n010.z + w011y*n011.z
                -w110y*n100.z - w111y*n101.z + w110y*n110.z + w111y*n111.z);

            row.z = 
               (-w001z*n000.z + w001z*n001.z - w011z*n010.z + w011z*n011.z
                -w101z*n100.z + w101z*n101.z - w111z*n110.z + w111z*n111.z);

            myGridToLocal.transformCovec (row, row);
            row.scale (d);

            Dnrm.m20 = row.x;
            Dnrm.m21 = row.y;
            Dnrm.m22 = row.z;
               
            Vector3d grad = new Vector3d();

            grad.x = (-w100x*d000 - w101x*d001 - w110x*d010 - w111x*d011
                      +w100x*d100 + w101x*d101 + w110x*d110 + w111x*d111);
            grad.y = (-w010y*d000 - w011y*d001 + w010y*d010 + w011y*d011
                      -w110y*d100 - w111y*d101 + w110y*d110 + w111y*d111);
            grad.z = (-w001z*d000 + w001z*d001 - w011z*d010 + w011z*d011
                      -w101z*d100 + w101z*d101 - w111z*d110 + w111z*d111);
            
            myGridToLocal.transformCovec (grad, grad);
            Dnrm.addOuterProduct (norm, grad);
         }
      }
      return d;
   }
   
   /** 
    * Calculates the distance and gradient at an arbitrary point in local
    * coordinates using multilinear interpolation of the vertex values for the
    * grid cell containing the point. The gradient is the true derivative
    * of the interpolated distance function within the cell, and is
    * in fact linear within the cell.
    * If the point lies outside the grid volume, {@link #OUTSIDE_GRID} is
    * returned.
    *
    * @param grad returns the gradient direction (local coordinates)
    * @param point point at which to calculate the gradient and distance
    * (local coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getLocalDistanceAndGradient (Vector3d grad, Point3d point) {

      Vector3d coords = new Vector3d();
      Vector3i vidx = new Vector3i();
      if (!getCellCoords (vidx, coords, point)) {
         return OUTSIDE_GRID;
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
      
      double d000  = getVertexValue (vidx.x  , vidx.y  , vidx.z  );
      double d001  = getVertexValue (vidx.x  , vidx.y  , vidx.z+1);
      double d010  = getVertexValue (vidx.x  , vidx.y+1, vidx.z  );
      double d011  = getVertexValue (vidx.x  , vidx.y+1, vidx.z+1);
      double d100  = getVertexValue (vidx.x+1, vidx.y  , vidx.z  );
      double d101  = getVertexValue (vidx.x+1, vidx.y  , vidx.z+1);
      double d110  = getVertexValue (vidx.x+1, vidx.y+1, vidx.z  );
      double d111  = getVertexValue (vidx.x+1, vidx.y+1, vidx.z+1);

      if (grad != null) {
         grad.x = (-w100x*d000 - w101x*d001 - w110x*d010 - w111x*d011
                   +w100x*d100 + w101x*d101 + w110x*d110 + w111x*d111);
         grad.y = (-w010y*d000 - w011y*d001 + w010y*d010 + w011y*d011
                   -w110y*d100 - w111y*d101 + w110y*d110 + w111y*d111);
         grad.z = (-w001z*d000 + w001z*d001 - w011z*d010 + w011z*d011
                   -w101z*d100 + w101z*d101 - w111z*d110 + w111z*d111);
         
         myGridToLocal.transformCovec (grad, grad);
      }

      return (w000*d000 + w001*d001 + w010*d010 + w011*d011 +
              w100*d100 + w101*d101 + w110*d110 + w111*d111);
   }

   /** 
    * Calculates the distance and gradient at an arbitrary point. These values
    * are determined by multilinear interpolation of the vertex values
    * for the grid cell containing the point. If the point lies outside
    * the grid volume, {@link #OUTSIDE_GRID} is returned.
    *
    * @param grad returns the gradient (world coordinates)
    * @param point point at which to calculate the gradient and distance
    * (world coordinates).
    * @return distance to the nearest feature
    */
   public double getWorldDistanceAndGradient (Vector3d grad, Point3d point) {
      Point3d lpnt = new Point3d();
      myLocalToWorld.inverseTransformPnt (lpnt, point);
      double d = getLocalDistanceAndGradient(grad, lpnt);
      if (grad != null && d != OUTSIDE_GRID) {
         // gradients transform like normals
         myLocalToWorld.transformCovec(grad, grad);
      }
      return d;
   }
   
   /**
    * Determines nearest feature to an arbitray point in local coordinates.  If
    * the grid is not associated with features (i.e., {@link #getFeatures}
    * returns <code>null</code>), or if the point lies outside the grid,
    * <code>null</code> is returned.
    *
    * @param nearest returns the nearest point on the feature (local coordinates)
    * @param point point for which to find nearest feature (local coordinates)
    * @return nearest feature, or null if outside of domain or if
    * no features are set.
    */
   public Feature getNearestLocalFeature (Point3d nearest, Point3d point) {
      
      Vector3d cpos = new Vector3d();
      Vector3i vidx = new Vector3i();
      if (!getCellCoords (vidx, cpos, point)) {
         return null;
      }
      Point3d tmp = new Point3d();
      double dmin = INF;
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
    * Determines nearest feature to an arbitray point in world coordinates.  If
    * the grid is not associated with features (i.e., {@link
    * #getFeatures} returns <code>null</code>), or if the point lies outside
    * the grid, <code>null</code> is returned.
    *
    * @param nearest returns the nearest point on the feature (world coordinates)
    * @param point point for which to find nearest feature (world coordinates)
    * @return nearest feature, or null if outside of domain or if
    * no features are set.
    */
   public Feature getNearestWorldFeature(Point3d nearest, Point3d point) {
      Point3d lpnt = new Point3d();
      myLocalToWorld.inverseTransformPnt (lpnt, point);
      Feature out = getNearestLocalFeature(nearest, lpnt);
      myLocalToWorld.transformPnt(nearest, nearest);
      return out;
   }

   protected int getQuadCellCoords (
      Vector3d coords, Vector3i vidx, Point3d ploc, 
      VectorTransformer3d quadGridToX) {

      Vector3d pgrid = new Vector3d();
      quadGridToX.inverseTransformPnt (pgrid, ploc);

      int xi, yj, zk;

      if (pgrid.x < 0 || pgrid.x > myQx) {
         return -1;
      }
      else if (pgrid.x == myQx) {
         xi = (int)pgrid.x - 1;
      }
      else {
         xi = (int)pgrid.x;
      }
      if (pgrid.y < 0 || pgrid.y > myQy) {
         return -1;
      }
      else if (pgrid.y == myQy) {
         yj = (int)pgrid.y - 1;
      }
      else {
         yj = (int)pgrid.y;
      }
      if (pgrid.z < 0 || pgrid.z > myQz) {
         return -1;
      }
      else if (pgrid.z == myQz) {
         zk = (int)pgrid.z - 1;
      }
      else {
         zk = (int)pgrid.z;
      }
      coords.x = pgrid.x - xi;
      coords.y = pgrid.y - yj;
      coords.z = pgrid.z - zk;
      if (vidx != null) {
         vidx.x = 2*xi;
         vidx.y = 2*yj;
         vidx.z = 2*zk;
      }
      return xi + myQx*yj + myQxQy*zk;
   }

   /**
    * Queries whether or not this grid is signed.
    *
    * @return <code>true</code> if this grid is signed.
    */
   public boolean isSigned() {
      return mySignedP;
   }

   /**
    * Returns the normal at a vertex, as specified by its x, y, z indices. The
    * normal is in local coordinates and is calculated on demand if necessary.
    * Normal calculation is done by mid-point differencing of the distance
    * values at adjacent vertices.
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
    * Utility method for the {@link #sweep sweep} method. Compares a given 
    * vertex with one of its neighbours.
    * @param phi contains the distance field
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @param dx x value of the neighbouring vertex
    * @param dy y value of the neighbouring vertex
    * @param dz z value of the neighbouring vertex
    */
   private void checkNeighbouringVertex (
      double[] phi, int xi, int yj, int zk, int dx, int dy, 
      int dz, Point3d closestPointOnFeature,
      Point3d p1, Feature[] features, int[] closestFeatureIdxs) {
      
      // dx, dy, dz represent a point +- 1 grid cell away from out current
      // point. There are 26 neighbours.
      int neighbourIndex = xyzIndicesToVertex (dx, dy, dz);
      if (closestFeatureIdxs[neighbourIndex] >= 0 ) {
         // Everything is in local coordinates.
         p1.set (xi, yj, zk);
         myGridToLocal.transformPnt (p1, p1);
         //getLocalVertexCoords (p1, xi, yj, zk);
         Feature neighbourFeature =
            features[closestFeatureIdxs[neighbourIndex]];
         neighbourFeature.nearestPoint (closestPointOnFeature, p1);
         double distanceToNeighbourFeature = p1.distance (closestPointOnFeature);
         int index = xyzIndicesToVertex (xi, yj, zk);
         
         if (distanceToNeighbourFeature < phi[index]) {
            phi[index] = distanceToNeighbourFeature;
            // gridCellArray [index].setDistance (distanceToNeighbourFeature);
            closestFeatureIdxs [index] = 
               closestFeatureIdxs [neighbourIndex];
         }
      }
   }

   /**
    * Sweeps across the entire grid, propagating distance values.
    * @param phi containts the distance field
    * @param dx x direction of sweep
    * @param dy y direction of sweep
    * @param dz z direction of sweep
    * @param featIdxs index of nearest feature at each vertex
    * @param features features being used to compute the grid
    */
   protected void sweep (
      double[] phi, int dx, int dy, int dz, 
      int[] featIdxs, Feature[] features) {

      Point3d pc = new Point3d();
      Point3d p1 = new Point3d();

      int x0, x1;
      if (dx > 0) {
         x0 = 1;
         x1 = myNx;
      }
      else {
         x0 = myNx-2;  // sweeps backwards
         x1 = -1;
      }
      int y0, y1;
      if (dy > 0) { 
         y0 = 1;
         y1 = myNy;
      }
      else { 
         y0 = myNy-2;
         y1 = -1;
      }
      int z0, z1;
      if (dz > 0) {
         z0 = 1;
         z1 = myNz;
      }
      else {
         z0 = myNz-2;
         z1 = -1;
      }
      for (int z = z0; z != z1; z += dz) {
         for (int y = y0; y != y1; y += dy) {
            for (int x = x0; x != x1; x += dx) {
               // What are the neighbours? Depending on dx,dy,dz...
               checkNeighbouringVertex (
                  phi, x, y, z, x-dx,    y   , z, pc, p1, features, featIdxs);
               checkNeighbouringVertex (
                  phi, x, y, z,    x, y-dy   , z, pc, p1, features, featIdxs);
               checkNeighbouringVertex (
                  phi, x, y, z, x-dx, y-dy   , z, pc, p1, features, featIdxs);
               checkNeighbouringVertex (
                  phi, x, y, z,    x,    y, z-dz, pc, p1, features, featIdxs);
               checkNeighbouringVertex (
                  phi, x, y, z, x-dx,    y, z-dz, pc, p1, features, featIdxs);
               checkNeighbouringVertex (
                  phi, x, y, z,    x, y-dy, z-dz, pc, p1, features, featIdxs);
               checkNeighbouringVertex (
                  phi, x, y, z, x-dx, y-dy, z-dz, pc, p1, features, featIdxs);
            }
         }
      }
   }

   /**
    * Returns the features, if any, associated with this distance
    * grid Features will be associated with the field if they were used to
    * compute it, via {@link #computeFromFeatures}, {@link
    * #computeFromFeaturesOBB}, or one of the associated constructors. They
    * will also be associated with the field if they were explicitly specified
    * via a call to {@link #setDistancesAndFeatures}.
    * If no features are associated with this grid,
    * <code>null</code> is returned.
    *
    * @return features associated with this grid, or <code>null</code> if
    * there are none.
    */
   public Feature[] getFeatures() {
      return myFeatures;
   }

   /**
    * Clears the features, if any, associated with this distance
    * grid. 
    */
   public void clearFeatures() {
      myFeatures = null;
      myClosestFeatureIdxs = null;
   }

   /** 
    * Returns the closest Feature to the vertex indexed by <code>idx</code>.
    * This assumes that the distance field is associated with features, as
    * described for {@link #getFeatures}. If this is not the case,
    * <code>null</code> is returned.
    *
    * @param idx vertex index
    * @return nearest Feature to the vertex, or <code>null</code>
    * if there are no features
    */
   public Feature getClosestFeature(int idx) {
      if (myFeatures == null) {
         return null;
      }
      else {
         return myFeatures[myClosestFeatureIdxs[idx]];
      }
   }

   /**
    * Apply the smoothing operation n times
    * @param n number of times to apply the smoothing operation
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

      double[] sphi = new double[myValues.length];
      Vector3d cellWidths = getCellWidths();
      for (int i=0; i<myNx; ++i) {
         for (int j=0; j<myNy; ++j) {
            for (int k=0; k<myNz; ++k) {
               
               // loop through 3x3x3 neighbourhood
               double wtotal = 0; 
               double p = 0;
               
               for (int ii=Math.max(i-1, 0); ii<Math.min(i+2, myNx); ++ii) {
                  for (int jj=Math.max(j-1, 0); jj<Math.min(j+2, myNy); ++jj) {
                     for (int kk=Math.max(k-1, 0); kk<Math.min(k+2, myNz); ++kk) {
                        if (ii != i || jj != j || kk != k) {
                           int idx = xyzIndicesToVertex(ii,jj,kk);
                           // squared distance
                           double d2 = 
                              sqr((ii-i)*cellWidths.x) +
                              sqr((jj-j)*cellWidths.y) +
                              sqr((kk-k)*cellWidths.z);
                           double w = 1.0/Math.sqrt(d2);
                           p += w*myValues[idx];  // weighted by inverse distance
                           wtotal += w;
                        }
                     }
                  }
               }
               
               int idx = xyzIndicesToVertex(i, j, k);
               sphi[idx] = myValues[idx] + alpha*p/wtotal;
            }
         }
      }
      
      myValues = sphi;
      myQuadCoefs = null;
      myTetIsOutside = null;
      clearNormals();

   }
   
   /**
    * Taubin Smoothing
    * 
    * @param lambda {@code >} 0, fraction of gradient to shrink
    * @param mu {@code <} 0, fraction of gradient to expand
    */
   public void smooth(double lambda, double mu) {
      smoothIter(lambda);
      smoothIter(mu);
   }
   
   /**
    * Applies Taubin smoothing
    * @param lambda {@code >} 0, fraction of gradient to shrink
    * @param mu {@code <} 0, fraction of gradient to expand
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
      
      double[] sphi = new double[myValues.length];
      for (int i=0; i<myNx; ++i) {
         for (int j=0; j<myNy; ++j) {
            for (int k=0; k<myNz; ++k) {
               
               // loop through 3x3x3 neighbourhood
               int N = 0; 
               double p = 0;
               for (int ii=Math.max(i-1, 0); ii<Math.min(i+2, myNx); ++ii) {
                  for (int jj=Math.max(j-1, 0); jj<Math.min(j+2, myNy); ++jj) {
                     for (int kk=Math.max(k-1, 0); kk<Math.min(k+2, myNz); ++kk) {
                        int idx = xyzIndicesToVertex(ii,jj,kk);
                        p += myValues[idx];
                        ++N;
                     }
                  }
               }
               
               int idx = xyzIndicesToVertex(i, j, k);
               sphi[idx] = p/N;
            }
         }
      }
      
      myValues = sphi;
      myQuadCoefs = null;      
      myTetIsOutside = null;
      clearNormals();
   }

  /**
    * Creates a triangular mesh approximating the surface on which the
    * quadratically interpolated distance function equals <code>val</code>.
    *
    * @param val iso surface value
    * @param res multiplies the resolution of this grid to obtain the
    * resolution of the grid used to create the mesh.
    * @return iso surface for quadratic interpolation
    */
   public PolygonalMesh createQuadDistanceSurface (double val, int res) {
      MarchingTetrahedra marcher = new MarchingTetrahedra();

      // sample at a res X higher resolution so we can get
      // a better sense of the smooth surface
      Vector3i cellRes = new Vector3i(getResolution());
      cellRes.scale (res);

      int nvx = cellRes.x+1;
      int nvy = cellRes.y+1;
      int nvz = cellRes.z+1;
      double[] dists = new double[nvx*nvy*nvz];
      double invRes = 1.0/res;

      // build a high res distance map by iterating through all the quad grid
      // cells:
      double[] a = new double[10];
      for (int i=0; i<myQx; i++) {
         for (int j=0; j<myQy; j++) {
            for (int k=0; k<myQz; k++) {
               Vector3i vidx = new Vector3i(2*i, 2*j, 2*k);
               double cx, cy, cz;

               int maxci = ((i < myQx-1) ? 2*res-1 : 2*res);
               int maxcj = ((j < myQy-1) ? 2*res-1 : 2*res);
               int maxck = ((k < myQz-1) ? 2*res-1 : 2*res);
               
               for (int ci=0; ci<=maxci; ci++) {
                  cx = ci*invRes/2;
                  for (int cj=0; cj<=maxcj; cj++) {
                     cy = cj*invRes/2;
                     for (int ck=0; ck<=maxck; ck++) {
                        cz = ck*invRes/2;
                        TetDesc tdesc =
                           new TetDesc (vidx, TetID.findSubTet (cx, cy, cz));
                        computeQuadCoefs (a, tdesc);
                        dists[(2*i*res+ci) + 
                              (2*j*res+cj)*nvx + 
                              (2*k*res+ck)*nvx*nvy] =
                           computeQuadDistance (a, cx, cy, cz);
                     }
                  }
               }
            }
         }
      }
      PolygonalMesh mesh = marcher.createMesh (
         dists, Vector3d.ZERO, new Vector3d(invRes,invRes,invRes), cellRes, val);
      mesh.transform (myGridToLocal);
      return mesh;      
   }

   /* --- rendering --- */

   public Vector3d getRenderVector (int xi, int yj, int zk) {
      return getLocalVertexNormal (xi, yj, zk);
   }

   public double getRenderVectorScale() {
      return 0.66*getCellWidths().minElement();
   }

   // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   // 
   // All code below this point is for getQuadDistanceAndGradient() and
   // findQuadSurfaceTangent(), which is used in the implementation of
   // strand wrapping. Still very messy and under development. Keep out!
   //
   // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

   public boolean useNormalsForQuadGrad = false;
   
   protected int[] myTetOffsets0156;
   protected int[] myTetOffsets0456;
   protected int[] myTetOffsets0476;
   protected int[] myTetOffsets0126;
   protected int[] myTetOffsets0326;
   protected int[] myTetOffsets0376;

   protected Vector3i createEdgeNode (Vector3i v0, Vector3i v1) {
      Vector3i en = new Vector3i();
      en.x = (v0.x+v1.x)/2;
      en.y = (v0.y+v1.y)/2;
      en.z = (v0.z+v1.z)/2;
      return en;
   }

   /*
    * Index offset for the eight corner nodes of a quad cell.
    */
   protected static Vector3i[] myBaseQuadCellXyzi = new Vector3i[] {
      new Vector3i (0, 0, 0),
      new Vector3i (2, 0, 0),
      new Vector3i (2, 0, 2),
      new Vector3i (0, 0, 2),
      new Vector3i (0, 2, 0),
      new Vector3i (2, 2, 0),
      new Vector3i (2, 2, 2),
      new Vector3i (0, 2, 2)
   };      

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

      boolean isInside (double x, double y, double z, double eps) {
         if (x < -eps || x > 1+eps ||
             y < -eps || y > 1+eps ||
             z < -eps || z > 1+eps) {
            return false;
         }
         switch (this) {
            case TET0516: {
               return y >= z-eps && x >= z-eps && x >= y-eps;
            }
            case TET0456: {
               return y >= z-eps && x >= z-eps && x < y+eps;
            }
            case TET0746: {
               return y >= z-eps && x < z+eps;
            }
            case TET0126: {
               return y < z+eps && x >= y-eps && x >= z-eps;
            }
            case TET0236: {
               return y < z+eps && x >= y-eps && x < z+eps;
            }
            case TET0376: {
               return y < z+eps && x < y+eps;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented tet " + this);
            }
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

      /**
       * Finds the index of the sub-tet within a hex cell, based on the x, y, z
       * coordinates of a point within the cell. These coordinates are assumed
       * to be normalized to [0,1] to correspond to the cell dimensions.
       */
      static int findSubTetIdx (double x, double y, double z) {
         if (y >= z) {
            if (x >= z) {
               if (x >= y) {
                  return 0;
               }
               else {
                  return 1;
               }
            }
            else {
               return 2;
            }
         }
         else {
            if (x >= y) {
               if (x >= z) {
                  return 3;
               }
               else {
                  return 4;
               }
            }
            else {
               return 5;
            }
         }
      } 
   };
   
   public static class TetDesc {
      // Indices of the base vertex of quad hex cell containing this tet.
      int myCXi;
      int myCYj;
      int myCZk;
      
      TetID myTetId;
      
      public TetDesc (Vector3i vxyz, TetID tetId) {
         this (vxyz.x, vxyz.y, vxyz.z, tetId);
      }

      public TetDesc (TetDesc tdesc) {
         myCXi = tdesc.myCXi;
         myCYj = tdesc.myCYj;
         myCZk = tdesc.myCZk;
         myTetId = tdesc.myTetId;
      }

      public TetDesc (int xi, int yj, int zk, TetID tetId) {
         myCXi = xi;
         myCYj = yj;
         myCZk = zk;
         myTetId = tetId;
      }

      public void addOffset (TetDesc tdesc) {
         myCXi += tdesc.myCXi;
         myCYj += tdesc.myCYj;
         myCZk += tdesc.myCZk;
      }

      public void addVertexOffset (Vector3i vidx) {
         myCXi += vidx.x;
         myCYj += vidx.y;
         myCZk += vidx.z;
      }

      public boolean equals (Object obj) {
         if (obj instanceof TetDesc) {
            TetDesc tdesc = (TetDesc)obj;
            return (myCXi == tdesc.myCXi &&
                    myCYj == tdesc.myCYj &&
                    myCZk == tdesc.myCZk &&          
                    myTetId == tdesc.myTetId);
         }
         else {
            return false;
         }
      }
      
      /**
       * Returns the indices of the base vertex for this tet.
       * 
       * @param vidx0 if not null, used to return the base vertex indices
       * @return base vertex indices
       */
      public Vector3i getBaseVertex (Vector3i vidx0) {
         if (vidx0 == null) {
            vidx0 = new Vector3i();
         }
         vidx0.x = myCXi;
         vidx0.y = myCYj;
         vidx0.z = myCZk;
         return vidx0;
      }
      
      public boolean baseVertexEquals (Vector3i vidx0) {
         return (myCXi == vidx0.x && myCYj == vidx0.y && myCZk == vidx0.z); 
      }

      public int hashCode() {
         // assume grid not likely bigger than 300 x 300 x 300, so
         // pick prime numbers close to 300 and 300^2
         return 6*(myCXi + myCYj*307 + myCZk*90017) + myTetId.intValue();
      }

      /**
       * Get the coordinates of the base vertex in grid coordinates.
       */
      public void getBaseCoords (Vector3d coords) {
         coords.set (myCXi, myCYj, myCZk);
      }

      public void getNodalVertexIndices (Vector3i vidx, int nodeIdx) {
         vidx.set (myCXi, myCYj, myCZk);
         vidx.add(myBaseQuadCellXyzi[myTetId.getNodes()[nodeIdx]]);
      }

      public Vector3i[] getVertices () {
         Vector3i[] vertices = new Vector3i[4];
         int[] nodes = myTetId.getNodes();
         for (int i=0; i<nodes.length; i++) {
            Vector3i vtx = new Vector3i (myCXi, myCYj, myCZk);
            vtx.add(myBaseQuadCellXyzi[nodes[i]]);
            vertices[i] = vtx;
         }
         return vertices;
      }

      public String toString() {
         return myTetId + "("+(myCXi)+","+(myCYj)+","+(myCZk)+")";
      }

      /**
       * Clip srng[] to accomodate the inequality a s > b.
       */
      private void clipRange (double[] srng, double a, double b) {
         if (a > 0) {
            // s > b/a
            double min = b/a;
            if (srng[0] < min) {
               srng[0] = min;
            }
         }
         else if (a < 0) {
            // s < b/a
            double max = b/a;
            if (srng[1] > max) {
               srng[1] = max;
            }
         }
      }

      /**
       * Clips a line segment defined by p = p0 + del*s, with s in the range
       * (srng[0], srng[1]) to fit within this tet. p0 and del are assumed
       * to be given in quadratic grid coordinates.
       */
      public void clipLineSegment (
         double[] srng, Point3d p0, Vector3d del) {

         // convert to cell coordinates
         double px = p0.x - myCXi/2;
         double py = p0.y - myCYj/2;
         double pz = p0.z - myCZk/2;

         switch (myTetId) {
            case TET0516: {
               clipRange (srng, -del.x, px-1);  // x < 1
               clipRange (srng,  del.z, -pz);   // z > 0
               clipRange (srng, del.y-del.z, pz-py); // y >= z
               clipRange (srng, del.x-del.y, py-px); // x >= y
               break;
            }
            case TET0456: {
               clipRange (srng, -del.y, py-1);  // y < 1
               clipRange (srng,  del.z, -pz);   // z > 0
               clipRange (srng, del.x-del.z, pz-px); // x >= z
               clipRange (srng, del.y-del.x, px-py); // y > x
               break;
            }
            case TET0746: {
               clipRange (srng, -del.y, py-1);  // y < 1
               clipRange (srng,  del.x, -px);   // x > 0
               clipRange (srng, del.y-del.z, pz-py); // y >= z
               clipRange (srng, del.z-del.x, px-pz); // z > x
               break;
            }
            case TET0126: {
               clipRange (srng, -del.x, px-1);  // x < 1
               clipRange (srng,  del.y, -py);   // y > 0
               clipRange (srng, del.x-del.z, pz-px); // x >= z
               clipRange (srng, del.z-del.y, py-pz); // z > y
               break;
            }
            case TET0236: {
               clipRange (srng, -del.z, pz-1);  // z < 1
               clipRange (srng,  del.y, -py);   // y > 0
               clipRange (srng, del.x-del.y, py-px); // x >= y
               clipRange (srng, del.z-del.x, px-pz); // z > x
               break;
            }
            case TET0376: {
               clipRange (srng, -del.z, pz-1);  // z < 1
               clipRange (srng,  del.x, -px);   // x > 0
               clipRange (srng, del.y-del.x, px-py); // y > x
               clipRange (srng, del.z-del.y, py-pz); // z > y
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented tet " + myTetId);
            }
         }
      }
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
      }
   }

   protected Vector3i getQuadCellCoords (
      Vector3i xyzi, Vector3d coords, Point3d point) {

      if (xyzi == null) {
         xyzi = new Vector3i();
      }
      if (!getCellCoords (xyzi, coords, point)) {
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

   protected void updateQuadCoefsIfNecessary() {
      if (myQuadCoefs == null) {
         // calculate number of quad cells in x, y, and z
         int ncx = (myNx-1)/2;
         int ncy = (myNy-1)/2;
         int ncz = (myNz-1)/2;
         myQuadCoefs = new double[6*ncx*ncy*ncz][];
         myTets = new TetDesc[6*ncx*ncy*ncz];
         for (int xi=0; xi<ncx; xi++) {
            for (int yj=0; yj<ncy; yj++) {
               for (int zk=0; zk<ncz; zk++) {
                  for (TetID tetId : TetID.values()) {
                     TetDesc tdesc = new TetDesc (2*xi, 2*yj, 2*zk, tetId);
                     double[] a = new double[10];
                     computeQuadCoefs (a, tdesc);
                     int cidx = 6*(xi + ncx*yj + ncx*ncy*zk) + tetId.intValue();
                     myQuadCoefs[cidx] = a;
                     myTets[cidx] = tdesc;
                  }
               }
            }
         }
      }
   }
   
   /**
    * Calculates the distance at an arbitrary point in local coordinates using
    * quadratic interpolation, as described for
    * {@link #getQuadDistanceAndGradient}.
    * If the point lies outside the grid volume, {@link
    * #OUTSIDE_GRID} is returned.
    *
    * @param point point at which to calculate the normal and distance
    * (local coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getQuadDistance (Point3d point) {
      return doGetQuadDistance (point, myQuadGridToLocal);      
   }
   
   public static int totalQueries = 0;
   public static int culledQueries = 0;

   protected double doGetQuadDistance (
      Point3d point, VectorTransformer3d quadGridToX) {

      Vector3d coords = new Vector3d();
      double[] a;
      double dx, dy, dz;
      if (storeQuadCoefs) {
         updateQuadCoefsIfNecessary();
         int voff = getQuadCellCoords (coords, null, point, quadGridToX);
         if (voff == -1) {
            return OUTSIDE_GRID;
         }
         dx = coords.x;
         dy = coords.y;
         dz = coords.z;
         totalQueries++;
         int tidx = 6*voff + TetID.findSubTetIdx (dx, dy, dz);
         if (myTetIsOutside != null && myTetIsOutside[tidx]) {
            culledQueries++;
            return OUTSIDE_GRID;
         }
         a = myQuadCoefs[tidx];
      }
      else {
         // Change to grid coordinates
         Vector3i vidx = new Vector3i();
         if (getQuadCellCoords (coords, vidx, point, quadGridToX) == -1) {
            return OUTSIDE_GRID;
         }
         dx = coords.x;
         dy = coords.y;
         dz = coords.z;
         a = new double[10];         
         TetDesc tdesc = new TetDesc (vidx, TetID.findSubTet (dx, dy, dz));
         computeQuadCoefs (a, tdesc);
      }
      return computeQuadDistance (a, dx, dy, dz);
   }

   public String getQuadCellInfo (Point3d point) {
      Vector3d coords = new Vector3d();
      Vector3i vidx = new Vector3i();
      if (getQuadCellCoords (coords, vidx, point, myQuadGridToLocal) == -1) {
         return "outside";
      }
      return vidx + " " + TetID.findSubTet (coords.x, coords.y, coords.z);
   }

   /**
    * Calculates the distance at an arbitrary point in world coordinates using
    * quadratic interpolation, as described for
    * {@link #getQuadDistanceAndGradient}.
    * If the point lies outside the grid volume, {@link
    * #OUTSIDE_GRID} is returned.
    *
    * @param point point at which to calculate the normal and distance
    * (world coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getWorldQuadDistance (Point3d point) {
      return doGetQuadDistance (point, myQuadGridToWorld);
   }

   /**
    * Used for debugging
    */
   public TetDesc getQuadTet (Point3d point) {
      Vector3d coords = new Vector3d();
      Vector3i vidx = new Vector3i();
      if (getQuadCellCoords (coords, vidx, point, myQuadGridToLocal) == -1) {
         return null;
      }
      double dx = coords.x;
      double dy = coords.y;
      double dz = coords.z;
      return new TetDesc (vidx, TetID.findSubTet (dx, dy, dz));
   }
   
   /** 
    * Calculates the distance and gradient at an arbitrary point in local
    * coordinates using quadratic interpolation. This interpolation is done
    * within the composite <i>quadratic</i> cell (composed of 2 x 2 x 2 regular
    * cells) containing the point. This cell is in turn decomposed into 6
    * tetrahedra, each with ten vertices, and the interpolation is done within
    * the tet containing the point. Performing the interpolation on a tet
    * ensures that the interpolation function is purely quadratic, and that its
    * gradient is purely linear. The returned gradient is the exact gradient of
    * the interpolation function within the tet.  If the point lies outside the
    * grid volume, {@link #OUTSIDE_GRID} is returned.
    *
    * @param grad returns the gradient direction (local coordinates)
    * @param point point at which to calculate the gradient and distance
    * (local coordinates).
    * @param dgrad if non-null, returns the derivative of the gradient (local
    * coordinates)
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getQuadDistanceAndGradient (
      Vector3d grad, Matrix3d dgrad, Point3d point) {
      
      return doGetQuadDistanceAndGradient (
         grad, dgrad, point, myQuadGridToLocal);
      // Point3d lpnt = new Point3d();
      // myLocalToWorld.inverseTransformPnt (lpnt, point);
      // double d = doGetQuadDistanceAndGradient (
      //    grad, dgrad, lpnt, myQuadGridToLocal);
      // if (d != OUTSIDE_GRID) {
      //    myLocalToWorld.transformCovec (grad, grad);
      //    if (dgrad != null) {
      //       dgrad.transform (myTLocalToWorld.R);
      //    }
      // }
      //return d;

   }
      
   protected double doGetQuadDistanceAndGradient (
         Vector3d grad, Matrix3d dgrad, Point3d point, 
         VectorTransformer3d quadGridToX) {

      Vector3d coords = new Vector3d();
      double[] a;
      double dx, dy, dz;
      Vector3i vidx = null;
      if (storeQuadCoefs) {
         updateQuadCoefsIfNecessary();
         if (useNormalsForQuadGrad) {
            vidx = new Vector3i();
         }
         int voff = getQuadCellCoords (coords, vidx, point, quadGridToX);
         if (voff == -1) {
            return OUTSIDE_GRID;
         }
         dx = coords.x;
         dy = coords.y;
         dz = coords.z;
         int tidx = 6*voff + TetID.findSubTetIdx (dx, dy, dz);
         totalQueries++;
         if (myTetIsOutside != null && myTetIsOutside[tidx]) {
            culledQueries++;
            return OUTSIDE_GRID;
         }
         a = myQuadCoefs[tidx];
      }
      else {
         // Change to grid coordinates
         vidx = new Vector3i();
         if (getQuadCellCoords (coords, vidx, point, quadGridToX) == -1) {
            return OUTSIDE_GRID;
         }
         dx = coords.x;
         dy = coords.y;
         dz = coords.z;
         a = new double[10];
         TetDesc tdesc = new TetDesc (vidx, TetID.findSubTet (dx, dy, dz));
         computeQuadCoefs (a, tdesc);
      }
      if (useNormalsForQuadGrad && grad != null) {
         TetDesc tdesc = new TetDesc (vidx, TetID.findSubTet (dx, dy, dz));
         computeGradientFromNormals (
            grad, dgrad, tdesc, dx, dy, dz, quadGridToX);
      }
      else {
         if (grad != null) {
            computeQuadGradient (grad, a, dx, dy, dz, quadGridToX);
         }
         if (dgrad != null) {
            computeQuadHessian (dgrad, a, quadGridToX);
         }
      }
      return computeQuadDistance (a, dx, dy, dz);
   }

   /** 
    * Calculates the distance and gradient at an arbitrary point in
    * world coordinates using quadratic interpolation, as described
    * for {@link #getLocalDistanceAndGradient(Vector3d,Point3d)}.
    * If the point lies outside
    * the grid volume, {@link #OUTSIDE_GRID} is returned.
    * @param grad returns the gradient (world coordinates)
    * @param dgrad if non-null, returns the derivative of the gradient (world 
    * coordinates)
    * @param point point at which to calculate the gradient and distance
    * (world coordinates).
    * @return interpolated distance, or <code>OUTSIDE_GRID</code>.
    */
   public double getWorldQuadDistanceAndGradient (
      Vector3d grad, Matrix3d dgrad, Point3d point) {
      return doGetQuadDistanceAndGradient (
         grad, dgrad, point, myQuadGridToWorld);
      // Point3d lpnt = new Point3d();
      // myLocalToWorld.inverseTransformPnt (lpnt, point);
      // double d = getQuadDistanceAndGradient (grad, dgrad, lpnt);
      // if (d != OUTSIDE_GRID) {
      //    myLocalToWorld.transformCovec (grad, grad);
      //    if (dgrad != null) {
      //       dgrad.transform (myTLocalToWorld.R);
      //    }
      // }
      // return d;
   }
   
   private void computeQuadCoefs (
      double[] a, int voff, int[] nodeOffs, int xi, int yi, int zi) {
      
      double v0  = myValues[voff+nodeOffs[0]];
      double v1  = myValues[voff+nodeOffs[1]];
      double v2  = myValues[voff+nodeOffs[2]];
      double v3  = myValues[voff+nodeOffs[3]];
      double v4  = myValues[voff+nodeOffs[4]];
      double v5  = myValues[voff+nodeOffs[5]];
      double v6  = myValues[voff+nodeOffs[6]];
      double v7  = myValues[voff+nodeOffs[7]];
      double v8  = myValues[voff+nodeOffs[8]];
      double v9  = myValues[voff+nodeOffs[9]];

      a[xi] =  2*v0 + 2*v1 - 4*v4;
      a[yi] =  2*v1 + 2*v2 - 4*v5;
      a[zi] =  2*v2 + 2*v3 - 4*v6;

      a[3+xi] = -4*v2 + 4*v5 + 4*v6 - 4*v8;
      a[3+yi] = -4*v5 + 4*v7 + 4*v8 - 4*v9;
      a[3+zi] = -4*v1 + 4*v4 + 4*v5 - 4*v7;

      a[6+xi] = -3*v0 -   v1 + 4*v4;
      a[6+yi] =    v1 -   v2 - 4*v4 + 4*v7;
      a[6+zi] =    v2 -   v3 - 4*v7 + 4*v9;

      a[9] = v0;
   }

   public void computeQuadCoefsStub (double[] a, TetDesc tdesc) {
      for (int i=0; i<10; i++) a[i] = 0;
   }

   public void computeQuadCoefs (double[] a, TetDesc tdesc) {

      createTetOffsetsIfNecessary();
      int voff = xyzIndicesToVertex (tdesc.myCXi, tdesc.myCYj, tdesc.myCZk);
      for (int i=0; i<10; i++) a[i] = 0;
      switch (tdesc.myTetId) {
         case TET0516: {
            computeQuadCoefs (a, voff, myTetOffsets0156, 0, 1, 2);
            break;
         }
         case TET0456: {
            computeQuadCoefs (a, voff, myTetOffsets0456, 1, 0, 2);
            break;
         }
         case TET0746: {
            computeQuadCoefs (a, voff, myTetOffsets0476, 1, 2, 0);
            break;
         }
         case TET0126: {
            computeQuadCoefs (a, voff, myTetOffsets0126, 0, 2, 1);
            break;
         }
         case TET0236: {
            computeQuadCoefs (a, voff, myTetOffsets0326, 2, 0, 1);
            break;
         }
         case TET0376: {
            computeQuadCoefs (a, voff, myTetOffsets0376, 2, 1, 0);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unknown tet type " + tdesc.myTetId);
         }
      }
   }

   double computeQuadDistance (
      double[] a, double dx, double dy, double dz) {

      double d =
         a[0]*dx*dx + a[1]*dy*dy + a[2]*dz*dz +
         a[3]*dy*dz + a[4]*dx*dz + a[5]*dx*dy +
         a[6]*dx + a[7]*dy + a[8]*dz + a[9];
      return d;
   }

   void computeQuadGradient (
     Vector3d grad, double[] a, double dx, double dy, double dz,
      VectorTransformer3d quadGridToX) {
 
      grad.x = 2*a[0]*dx + a[5]*dy + a[4]*dz + a[6];
      grad.y = 2*a[1]*dy + a[5]*dx + a[3]*dz + a[7];
      grad.z = 2*a[2]*dz + a[4]*dx + a[3]*dy + a[8];
            
      quadGridToX.transformCovec (grad, grad); 
      //grad.scale (0.5); // convert to regular grid coords
      //myGridToLocal.transformCovec (grad, grad); // then to local coords
   }

   void computeGradientFromNormals (
      Vector3d grad, Matrix3d dgrad, TetDesc tdesc, 
      double dx, double dy, double dz, VectorTransformer3d quadGridToX) {  
      
      Vector3i vidx = new Vector3i();
      tdesc.getNodalVertexIndices (vidx, 0);
      Vector3d n0 = getLocalVertexNormal (vidx.x, vidx.y, vidx.z);
      tdesc.getNodalVertexIndices (vidx, 1);
      Vector3d n1 = getLocalVertexNormal (vidx.x, vidx.y, vidx.z);
      tdesc.getNodalVertexIndices (vidx, 2);
      Vector3d n2 = getLocalVertexNormal (vidx.x, vidx.y, vidx.z);
      tdesc.getNodalVertexIndices (vidx, 3);
      Vector3d n3 = getLocalVertexNormal (vidx.x, vidx.y, vidx.z);

      Vector3d n10 = new Vector3d();
      Vector3d n21 = new Vector3d();
      Vector3d n32 = new Vector3d();
      
      n10.sub (n1, n0);
      n21.sub (n2, n1);
      n32.sub (n3, n2);

      Vector3d nx, ny, nz;

      switch (tdesc.myTetId) {
         case TET0516: nx = n10; ny = n21; nz = n32; break;
         case TET0126: nx = n10; ny = n32; nz = n21; break;
         case TET0456: nx = n21; ny = n10; nz = n32; break;
         case TET0746: nx = n32; ny = n10; nz = n21; break;
         case TET0236: nx = n21; ny = n32; nz = n10; break;
         case TET0376: nx = n32; ny = n21; nz = n10; break;
         default: {
            throw new InternalErrorException (
               "Unknown tet type " + tdesc.myTetId);
         }      
      }
      grad.scaledAdd (dx, nx, n0);
      grad.scaledAdd (dy, ny);
      grad.scaledAdd (dz, nz);
      quadGridToX.transformCovec (grad, grad);

      if (dgrad != null) {

         // compute from right to left:

         // derivative of grad x
         Vector3d tmp = new Vector3d();
         quadGridToX.transformCovec (tmp, nx);
         dgrad.m00 = tmp.x;
         dgrad.m01 = tmp.y;
         dgrad.m02 = tmp.z;

         // derivative of grad y
         quadGridToX.transformCovec (tmp, ny);
         dgrad.m10 = tmp.x;
         dgrad.m11 = tmp.y;
         dgrad.m12 = tmp.z;

         // derivative of grad z
         quadGridToX.transformCovec (tmp, nz);
         dgrad.m20 = tmp.x;
         dgrad.m21 = tmp.y;
         dgrad.m22 = tmp.z;

         tmp.set (dgrad.m00, dgrad.m10, dgrad.m20);
         quadGridToX.transformCovec (tmp, tmp);
         dgrad.m00 = tmp.x;
         dgrad.m10 = tmp.y;
         dgrad.m20 = tmp.z;

         tmp.set (dgrad.m01, dgrad.m11, dgrad.m21);
         quadGridToX.transformCovec (tmp, tmp);
         dgrad.m01 = tmp.x;
         dgrad.m11 = tmp.y;
         dgrad.m21 = tmp.z;

         tmp.set (dgrad.m02, dgrad.m12, dgrad.m22);
         quadGridToX.transformCovec (tmp, tmp);
         dgrad.m02 = tmp.x;
         dgrad.m12 = tmp.y;
         dgrad.m22 = tmp.z;
      }
   }
   
   void computeQuadHessian (
      Matrix3d dgrad, double[] a, VectorTransformer3d quadGridToX) {

      // Hessian is computed in cell coordinates and converted to
      // local coordinates using
      //
      // d^2 f(x)      -1 T  d^2 f(c)   -1
      // -------- =   A      --------  A
      //   d x^2              d c^2
      //
      // where x = A c + b
      //
      // With respect to cell coordinates,
      //
      //            [ 2 a0   a5    a4  ]
      // d^2 f(c)   [                  ]
      // -------- = [  a5   2 a1   a3  ]
      //   d c^2    [                  ]
      //            [  a4    a3   2 a2 ]

      Vector3d tmp = new Vector3d();

      // compute from right to left:

      // derivative of grad x
      tmp.set (2*a[0], a[5], a[4]);
      quadGridToX.transformCovec (tmp, tmp);
      dgrad.m00 = tmp.x;
      dgrad.m01 = tmp.y;
      dgrad.m02 = tmp.z;

      // derivative of grad y
      tmp.set (a[5], 2*a[1], a[3]);
      quadGridToX.transformCovec (tmp, tmp);
      dgrad.m10 = tmp.x;
      dgrad.m11 = tmp.y;
      dgrad.m12 = tmp.z;

      // derivative of grad z
      tmp.set (a[4], a[3], 2*a[2]);
      quadGridToX.transformCovec (tmp, tmp);
      dgrad.m20 = tmp.x;
      dgrad.m21 = tmp.y;
      dgrad.m22 = tmp.z;

      tmp.set (dgrad.m00, dgrad.m10, dgrad.m20);
      quadGridToX.transformCovec (tmp, tmp);
      dgrad.m00 = tmp.x;
      dgrad.m10 = tmp.y;
      dgrad.m20 = tmp.z;

      tmp.set (dgrad.m01, dgrad.m11, dgrad.m21);
      quadGridToX.transformCovec (tmp, tmp);
      dgrad.m01 = tmp.x;
      dgrad.m11 = tmp.y;
      dgrad.m21 = tmp.z;

      tmp.set (dgrad.m02, dgrad.m12, dgrad.m22);
      quadGridToX.transformCovec (tmp, tmp);
      dgrad.m02 = tmp.x;
      dgrad.m12 = tmp.y;
      dgrad.m22 = tmp.z;
            
      //dgrad.scale (0.25); // scaling to convert from quad to regular grid coords
   }

//   boolean inRange (TetDesc tdesc) {
//      return (tdesc.myXi >= 0 && tdesc.myXi < myNx-2 &&
//              tdesc.myYj >= 0 && tdesc.myYj < myNy-2 &&
//              tdesc.myZk >= 0 && tdesc.myZk < myNz-2);
//   }
   
   boolean inRange (TetDesc tdesc) {
      return (tdesc.myCXi >= 0 && tdesc.myCXi < 2*myQx &&
              tdesc.myCYj >= 0 && tdesc.myCYj < 2*myQy &&
              tdesc.myCZk >= 0 && tdesc.myCZk < 2*myQz);
   }
      
   /**
    * Find a point <code>pt</code> such that the line segment
    * <code>pa-pt</code> is tangent to the quadratic zero distance surface and
    * also lies in the plane defined by point <code>p0</code> and normal
    * <code>nrm</code>. The search starts by finding a point on the surface
    * that is close to <code>p0</code> and proceeds from there.
    * 
    * @param pt returns the tangent point
    * @param p0 point on the plane
    * @param pa origin for the tangent segment
    * @param nrm normal defining the plane
    * @return false if the intersection could not be found
    */
   public boolean findQuadSurfaceTangent (
      Point3d pt, Point3d p0, Point3d pa, Vector3d nrm) {

      DistanceGridSurfCalc calc = new DistanceGridSurfCalc(this);
      calc.setDebug (myDebug);
      return calc.findQuadSurfaceTangent (pt, p0, pa, nrm);
   }

   /**
    * Experimental method. Find the intersection point <code>pi</code> between
    * the quadratic zero surface and a ray <code>(p0, pa)</code> lying in the
    * plane defined by point <code>p0</code> and normal <code>nrm</code>.
    * <code>pa</code> is projected onto the plane internally so the caller does
    * not need to ensure this.
    * 
    * @param pi returns the intersection point
    * @param p0 point on the plane and ray origin
    * @param pa projected onto the plane to form the outer point defining the ray
    * @param nrm normal defining the plane
    * @return false if the intersection could not be found
    */
   public boolean findQuadSurfaceIntersection (
      Point3d pi, Point3d p0, Point3d pa, Vector3d nrm) {

      DistanceGridSurfCalc calc = new DistanceGridSurfCalc(this);
      calc.setDebug (myDebug);
      return calc.findQuadSurfaceIntersection (pi, p0, pa, nrm);
   }

   protected boolean scanItem (
      ReaderTokenizer rtok, Object ref) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "signed")) {
         mySignedP = rtok.scanBoolean();
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, ref);      
   }
   
   protected void writeItems (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {

      super.writeItems (pw, fmt, ref);
      pw.println ("signed=" + mySignedP);
   }

   /**
    * Returns <code>true</code> if this grid equals another within a prescribed
    * tolerance. The grids are equal if the resolution, widths, center,
    * orientation, distances and signed property are equal. Specifying {@code
    * tol = 0} requires exact equality.
    *
    * @param grid grid to compare against
    * @param tol floating point tolerance (absolute)
    */
   public boolean epsilonEquals (DistanceGrid grid, double tol) {
      if (!super.epsilonEquals (grid, tol)) {
         return false;
      }
      if (mySignedP != grid.mySignedP) {
         return false;
      }
      return true;
   }

   /**
    * Scales the distance units of the grid, which entails scaling the widths,
    * distances and coordinate transform information.
    *
    * @param s scaling factor
    */
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      for (int i=0; i<myValues.length; i++) {
         myValues[i] *= s;
      }
      if (myGridToLocal != null) {
         scaleTransformer (myQuadGridToLocal, s);
         updateQuadGridToWorld (myQuadGridToLocal);
      }
   }

   private boolean tetIsOutside (TetDesc tdesc, double mindist) {
      Vector3i[] vertices = tdesc.getVertices();
      for (int k=0; k<vertices.length; k++) {
         int vi = xyzIndicesToVertex (vertices[k]);
         if (myValues[vi] <= mindist) {
            return false;
         }
      }
      return true;
   }

   /**
    * Marks all tets whose nodes all have distance values {@code > mindist} as
    * being "outside".
    */
   public void markOutsideQuadtets (double mindist) {
      int ncx = (myNx-1)/2;
      int ncy = (myNy-1)/2;
      int ncz = (myNz-1)/2;
      myTetIsOutside = new boolean[6*ncx*ncy*ncz];
      int nout = 0;
      for (int xi=0; xi<ncx; xi++) {
         for (int yj=0; yj<ncy; yj++) {
            for (int zk=0; zk<ncz; zk++) {
               for (TetID tetId : TetID.values()) {
                  TetDesc tdesc = new TetDesc (2*xi, 2*yj, 2*zk, tetId);
                  int cidx = 6*(xi + ncx*yj + ncx*ncy*zk) + tetId.intValue();
                  boolean outside = tetIsOutside (tdesc, mindist);
                  myTetIsOutside[cidx] = outside;
                  if (outside) {
                     nout++;
                  }
               }
            }
         }
      }
      System.out.println ("number outside: "+nout+"/"+(6*ncx*ncy*ncz));
   }

   // --- extra experimental stuff ----

   private boolean quadCellContainsBoundary (int xi, int yj, int zk) {
      boolean hasPositive = false;
      boolean hasNegative = false;
      for (int i=0; i<3; i++) {
         for (int j=0; j<3; j++) {
            for (int k=0; k<3; k++) {
               int vi = xyzIndicesToVertex (xi+i, yj+j, zk+k);
               if (myValues[vi] >= 0) {
                  hasPositive = true;
               }
               else {
                  hasNegative = true;
               }
            }
         }
      }
      return hasPositive && hasNegative;
   }

   public void checkGradient() {
      for (int xi=0; xi<myQx; xi++) {
         for (int yj=0; yj<myQy; yj++) {
            for (int zk=0; zk<myQz; zk++) {
               if (quadCellContainsBoundary (2*xi, 2*yj, 2*zk)) {
                  boolean show = (zk == 4 && xi == 0 && yj == 0);
                  TetDesc[] adjDescs = DistanceGridSurfCalc.getNodeTets().get(0);
                  for (int i=0; i<adjDescs.length; i++) {
                     TetDesc adesc = new TetDesc(adjDescs[i]);
                     adesc.addVertexOffset (new Vector3i(2*xi, 2*yj, 2*zk));
                     if (inRange (adesc)) {
                        double dx = adesc.myCXi/2 < xi ? 1.0 : 0;
                        double dy = adesc.myCYj/2 < yj ? 1.0 : 0;
                        double dz = adesc.myCZk/2 < zk ? 1.0 : 0;
                        if (show) {
                           System.out.println (
                              "  tet " + adesc +"  dx="+dx+" dy="+dy + " dz="+dz);
                        }
                        double a[] = new double[10];
                        computeQuadCoefs (a, adesc);
                        Vector3d grad = new Vector3d();
                        computeQuadGradient (
                           grad, a, dx, dy, dz, myQuadGridToLocal);
                        if (show) {
                           System.out.println (
                              "    grad=" + grad.toString ("%10.6f"));
                           System.out.println (
                              "    dist=" + computeQuadDistance (a, dx, dy, dz));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public class GridEdge {

      int myVi0;
      int myVi1;

      GridEdge (int vi0, int vi1) {
         Vector3i vxyz0 = new Vector3i();
         Vector3i vxyz1 = new Vector3i();
         vertexToXyzIndices (vxyz0, vi0);
         vertexToXyzIndices (vxyz1, vi1);
         if (vxyz0.x > vxyz1.x || vxyz0.y > vxyz1.y || vxyz0.z > vxyz1.z) {
            myVi0 = vi1;
            myVi1 = vi0;
         }
         else {
            myVi0 = vi0;
            myVi1 = vi1;
         }
      }

      public boolean equals (Object obj) {
         if (obj instanceof GridEdge) {
            GridEdge edge = (GridEdge)obj;
            return edge.myVi0 == myVi0 && edge.myVi1 == myVi1;
         }
         else {
            return false;
         }
      }

      public int hashCode() {
         return myVi0 + 131071*myVi1;
      }

      public String toString() {
         return "" + myVi0 + "-" + myVi1;
      }

      public Vector3i getBaseVertex (Vector3i vidx0) {
         if (vidx0 == null) {
            vidx0 = new Vector3i();
         }
         vertexToXyzIndices (vidx0, myVi0);
         return vidx0;
      }

      public TetEdge getEdgeFeature() {
         Vector3i vxyz0 = new Vector3i();
         Vector3i vxyz1 = new Vector3i();
         vertexToXyzIndices (vxyz0, myVi0);
         vertexToXyzIndices (vxyz1, myVi1);
         Vector3i vidx0 = getBaseVertex (null);
         vxyz0.sub (vidx0);
         vxyz1.sub (vidx0);
         return new TetEdge (
            DistanceGridSurfCalc.findRefVertex (vxyz0),
            DistanceGridSurfCalc.findRefVertex (vxyz1));
      }

   }

   boolean first = true;

   void maybeAddEdge (HashSet<GridEdge> edges, int v0, int v1) {
      double d0 = myValues[v0];
      double d1 = myValues[v1];
      if (d0*d1 < 0) {
         GridEdge edge = new GridEdge (v0, v1);
         if (!edges.contains (edge)) {
            Vector3i xyzi0 = new Vector3i();
            Vector3i xyzi1 = new Vector3i();
            vertexToXyzIndices (xyzi0, v0);
            vertexToXyzIndices (xyzi1, v1);
            double lam = Math.abs(d0)/(Math.abs(d0)+Math.abs(d1));
            Vector3d isect = new Vector3d();
            isect.sub (new Vector3d(xyzi1), new Vector3d(xyzi0));
            isect.scale (lam);
            isect.add (new Vector3d(xyzi0));
            TetEdge featEdge = edge.getEdgeFeature();
            Vector3i vidx0 = edge.getBaseVertex(null);
            ArrayList<TetDesc> tets = new ArrayList<>();
            DistanceGridSurfCalc.getFeatureAdjacentTets (
               tets, vidx0, null, featEdge, this, null);
            edges.add (edge);
            if (first) {
               System.out.println ("edge at " + xyzi0 + "   " + xyzi1);
            }
            for (TetDesc adesc : tets) {
               vidx0 = adesc.getBaseVertex(null);
               double dx = (isect.x - vidx0.x)/2;
               double dy = (isect.y - vidx0.y)/2;
               double dz = (isect.z - vidx0.z)/2;
               double a[] = new double[10];
               computeQuadCoefs (a, adesc);
               Vector3d grad = new Vector3d();
               computeQuadGradient (
                  grad, a, dx, dy, dz, myQuadGridToLocal);
               if (first) {
                  System.out.println (
                     "    grad=" + grad.toString ("%10.6f"));
                  System.out.println (
                     "    dist=" + computeQuadDistance (a, dx, dy, dz));
               }               
            }
            first = false;
         }
      }
   }

   private void addSurfaceIntersectingEdges (
      HashSet<GridEdge> edges, Vector3i vidx0) {

      int v0 = xyzIndicesToVertex (vidx0.x,   vidx0.y,   vidx0.z);
      int v1 = xyzIndicesToVertex (vidx0.x+2, vidx0.y,   vidx0.z);
      int v2 = xyzIndicesToVertex (vidx0.x+2, vidx0.y,   vidx0.z+2);
      int v3 = xyzIndicesToVertex (vidx0.x,   vidx0.y,   vidx0.z+2);
      int v4 = xyzIndicesToVertex (vidx0.x,   vidx0.y+2, vidx0.z);
      int v5 = xyzIndicesToVertex (vidx0.x+2, vidx0.y+2, vidx0.z);
      int v6 = xyzIndicesToVertex (vidx0.x+2, vidx0.y+2, vidx0.z+2);
      int v7 = xyzIndicesToVertex (vidx0.x,   vidx0.y+2, vidx0.z+2);
      
      maybeAddEdge (edges, v0, v1);
      maybeAddEdge (edges, v1, v2);
      maybeAddEdge (edges, v2, v3);
      maybeAddEdge (edges, v3, v0);

      maybeAddEdge (edges, v4, v5);
      maybeAddEdge (edges, v5, v6);
      maybeAddEdge (edges, v6, v7);
      maybeAddEdge (edges, v7, v4);

      maybeAddEdge (edges, v0, v4);
      maybeAddEdge (edges, v1, v5);
      maybeAddEdge (edges, v2, v6);
      maybeAddEdge (edges, v3, v7);

      maybeAddEdge (edges, v0, v2);
      maybeAddEdge (edges, v4, v6);
      maybeAddEdge (edges, v0, v7);
      maybeAddEdge (edges, v1, v6);
      maybeAddEdge (edges, v0, v5);
      maybeAddEdge (edges, v3, v6);

      maybeAddEdge (edges, v0, v6);
   }

   public ArrayList<GridEdge> findSurfaceIntersectingEdges() {
      LinkedHashSet<GridEdge> edges = new LinkedHashSet<>();
      for (int xi=0; xi<myQx; xi++) {
         for (int yj=0; yj<myQy; yj++) {
            for (int zk=0; zk<myQz; zk++) {
               addSurfaceIntersectingEdges (
                  edges, new Vector3i (2*xi, 2*yj, 2*zk));
            }
         }
      }
      ArrayList<GridEdge> list = new ArrayList<GridEdge>();
      list.addAll (edges);
      return list;
   }

   public ArrayList<TetDesc> findSurfaceEdgeTets (ArrayList<GridEdge> edges) {
      ArrayList<TetDesc> tets = new ArrayList<>();
      HashSet<TetDesc> found = new HashSet<>();
      for (GridEdge edge : edges) {
         TetEdge featEdge = edge.getEdgeFeature();
         Vector3i vidx0 = edge.getBaseVertex(null);
         int k0 = tets.size();
         DistanceGridSurfCalc.getFeatureAdjacentTets (
            tets, vidx0, null, featEdge, this, found);
         int k1 = tets.size();
         for (int k=k0; k<k1; k++) {
            found.add (tets.get(k));
         }
      }
      return tets;
   }
 
}

