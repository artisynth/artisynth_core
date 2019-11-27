/*
 * Copyright (c) 2017, by the Authors: Bruce Haines (UBC), Antonio Sanchez
 * (UBC), John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * thze LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import maspack.matrix.IdentityVector3dTransform;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.VectorTransformer3d;
import maspack.render.PointLineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scannable;
import maspack.util.StringHolder;

/**
 * Base class for regular 3D grids that interpolate values using trilinear
 * interpolation.
 * 
 * <p>The grid is implemented as a regular 3D grid composed of
 * <code>nx</code> X <code>ny</code> X <code>nz</code> vertices along the x, y
 * and z directions, giving it a <i>resolution</i> of <code>rx</code>,
 * <code>ry</code>, <code>rz</code>, cells along each of these directions,
 * where <code>rx = nx-1</code>, <code>ry = ny-1</code>, and <code>rz =
 * nz-1</code>. The grid has widths <code>w = (wx, wy, wz)</code> along each of
 * these directions, and <i>cell widths</i> given by <code>wx/rx</code>,
 * <code>wy/ry</code>, and <code>wz/rz</code>.
 *
 * <p>Several coordinate frames are associated with the grid:
 *
 * <ul>
 *
 * <li>The <i>local</i> frame L in which <i>local</i> value queries are
 * made. By default, the grid is centered on, and axis aligned with, the local
 * frame. However, it is also possible to reposition the grid so that it has a
 * <i>center</i> and an <i>orientation</i> defined with respect to L.
 *
 * <li>The local frame L can itself by described with respect to a world frame
 * W, using a rigid transform TLW that maps from local to world coordinates.
 * Values queries can also be made with respect to world coordinates.
 *
 * <li>The <i>grid</i> frame G is defined with respect to the grid itself,
 * aligned with its orientation, and translated and scaled so that the
 * coordinates of each vertex corresponds to (i, j, k), where i, j, and k are
 * the vertex indices. The origin (0, 0, 0) hence corresponds to the minimum
 * grid vertex. Conversion from grid to local coordinates is done using a
 * combined scaling/rigid transform TGL.
 * </ul>
 */
public abstract class InterpolatingGridBase implements Renderable, Scannable {

   protected Vector3d myWidths;         // widths along x, y, z

   protected int myDebug = 0;

   private static int MAX_INT = Integer.MAX_VALUE;

   // colors, colorMap and color indices which can be used to assign colors to
   // the different vertices for rendering purposes
   protected LinkedHashMap<Color,Integer> myColorMap;
   protected ArrayList<Color> myColors;
   protected int[] myColorIndices;
   protected RenderProps myRenderProps = null;
   protected int[] myRenderRanges = null;

   protected RenderObject myRob;      // render object used for rendering
   protected boolean myRobValid = false;
   private static final int VECTOR_GROUP = 0; // rendering line group for vectors
   private static final int EDGE_GROUP = 1;   // rendering line group for edges

   RigidTransform3d myTLW = null; // temp - used only when scanning from file

   /**
    * Transform from grid to local coordinates.
    */
   protected VectorTransformer3d myGridToLocal;
   // transform from center to local:
   protected RigidTransform3d myTCL = new RigidTransform3d();  

   /**
    * Transform from local to world coordinates.
    */
   protected VectorTransformer3d myLocalToWorld;

   /**
    * Radius of this grid (half the distance across its diagonal)
    */
   protected double myRadius;

   protected int myNx = 0;  // number of vertices along X
   protected int myNy = 0;  // number of vertices along Y
   protected int myNz = 0;  // number of vertices along Z
   protected int myNxNy = 0; // nx*ny

   protected static final double INF = Double.POSITIVE_INFINITY;
   
   protected RigidTransform3d myTLocalToWorld;
   protected boolean myHasLocalToWorld = false;

   /**
    * For subclasses with parameterized typing, returns the type parameter.
    * Othewise, returns {@code null}.
    *
    * @return type parameter, or {@code null}
    */
   public abstract Class<?> getParameterType();

   protected InterpolatingGridBase () {
      myRenderProps = createRenderProps();
      myWidths = new Vector3d();
      myLocalToWorld = new IdentityVector3dTransform();
   }
   
   /**
    * Sets a transform that maps from local to world coordinates.
    * If the specified transform is the identity, then {@link hasLocalToWorld} 
    * will subsequently return {@code false}.
    * 
    * @param TLW transform from local to world coordinates
    */
   public void setLocalToWorld (RigidTransform3d TLW) {
      myTLocalToWorld = TLW.copy();
      if (!TLW.isIdentity()) {
         myLocalToWorld = myTLocalToWorld;
         myHasLocalToWorld = true;
      }
      else {
         myLocalToWorld = new IdentityVector3dTransform();
         myHasLocalToWorld = false;
      }
   }
   
   /**
    * Returns the transform that maps from local to world coordinates.
    * If {@link hasLocalToWorld} returns {@code false}, this will be
    * the identity.
    * 
    * @return transform from local to world coordinates. Should
    * not be modified.
    */
   public RigidTransform3d getLocalToWorld() {
      return myTLocalToWorld;
   }
   
   /**
    * Queries whether this grid has a local-to-world transform that is
    * not the identity.
    * 
    *  @return {@code true} if the local-to-world transform is not the
    *  identity.
    */
   public boolean hasLocalToWorld() {
      return myHasLocalToWorld;
   }
   
   /**
    * Returns the transformer that maps from local to world coordinates.  If
    * the local-to-world transform is the identity, then this will be an 
    * identity transformer.
    * 
    * @return transformer that maps from local to world coordinates
    */
   public VectorTransformer3d getLocalToWorldTransformer() {
      return myLocalToWorld;
   }

   /**
    * Returns the transformer that maps from grid to local coordinates.
    *         
    * @return transformer from grid to local coordinates
    */
   public VectorTransformer3d getGridToLocalTransformer() {
      return myGridToLocal;
   }
   
   protected void initGrid (
      Vector3d widths, Vector3i resolution, RigidTransform3d TCL) {
      if (widths == null) {
         widths = new Vector3d (1, 1, 1);
      }
      setResolution (resolution);
      setWidths (widths);
      if (TCL == null) {
         TCL = RigidTransform3d.IDENTITY;
      }
      setCenterAndOrientation (TCL);
      clearColors();      
   }

   /**
    * Creates a new grid base that is a copy of an existing one.
    *
    * @param grid grid base to copy
    */
   public InterpolatingGridBase (InterpolatingGridBase grid) {
      this();
      set (grid);
   }

   /**
    * Sets the resolution for this grid along the x, y, and z axes.
    *
    * @param resolution cell resolution along the x, y and z axes.  Must be at
    * least 1 along each axis.
    */
   public void setResolution (Vector3i resolution) {
      if (resolution.x < 1 || resolution.y < 1 || resolution.z < 1) {
         throw new IllegalArgumentException (
            "Resolution is "+resolution+"; must be at least 1 along each axis");
      }
      int nvx = resolution.x+1;
      int nvy = resolution.y+1;
      int nvz = resolution.z+1;

      if (nvx != myNx || nvy != myNy || nvz != myNz) {

         // update vertex counts
         myNx = nvx;
         myNy = nvy;
         myNz = nvz;
         myNxNy = myNx*myNy;  

         // clear or recompute distances
         int numV = myNx*myNy*myNz;
         initVertexValues (numV);
         myColorIndices = new int[numV];
         // adjust render ranges
         if (myRenderRanges == null) {
            myRenderRanges = new int[] {0, MAX_INT, 0, MAX_INT, 0, MAX_INT};
         }
         else {
            adjustRenderRanges (myRenderRanges);
         }
         clearColors();
      }
   }

   protected abstract void initVertexValues(int numv);

   private boolean adjustRenderRanges (int[] ranges) {
      boolean changed = false;
      if (ranges[0] >= myNx) {
         ranges[0] = 0;
         ranges[1] = MAX_INT;
         changed = true;
      }
      if (ranges[2] >= myNy) {
         ranges[2] = 0;
         ranges[3] = MAX_INT;
         changed = true;
      }
      if (ranges[4] >= myNz) {
         ranges[4] = 0;
         ranges[5] = MAX_INT;
         changed = true;
      }      
      return changed;
   }
   
   /**
    * Sets the widths of this grid along the x, y, and z axes. The grid
    * resolution, center, orientation, features and distances remain unchanged.
    *
    * @param widths for this grid
    */
   void setWidths (Vector3d widths) {
      if (!widths.equals (myWidths)) {
         myWidths.set (widths);
         myRadius = widths.norm()/2;
         updateGridToLocal();
      }
   }

    private LinkedHashMap<Color,Integer> copyColorMap (
      HashMap<Color,Integer> map) {
      LinkedHashMap<Color,Integer> copy = new LinkedHashMap<>();
      for (Map.Entry<Color,Integer> e : map.entrySet()) {
         copy.put (e.getKey(), e.getValue());
      }
      return copy;
   }

  /**
    * Sets this grid base to be a copy of an existing one.
    *
    * @param grid grid base to copy
    */
   public void set (InterpolatingGridBase grid) {

      myWidths.set (grid.myWidths);
      
      if (grid.myColorMap != null) {
         myColorMap = copyColorMap (grid.myColorMap);
      }
      else {
         myColorMap = null;
      }
      
      if (grid.myColors != null) {
         myColors = new ArrayList<Color>();
         myColors.addAll (grid.myColors);
      }
      else {
         myColors = null;
      }
      if (grid.myColorIndices != null) {
         myColorIndices = Arrays.copyOf (
            grid.myColorIndices, grid.myColorIndices.length);
      }
      else {
         myColorIndices = null;
      }
      myRenderProps = new RenderProps (grid.myRenderProps);
      if (grid.myRenderRanges != null) {
         myRenderRanges = Arrays.copyOf (
            grid.myRenderRanges, grid.myRenderRanges.length);
      }
      else {
         myRenderRanges = null;
      }

      //myDrawEdges = grid.myDrawEdges;
      myRobValid = false;
      
      if (grid.myGridToLocal != null) {
         myGridToLocal = grid.myGridToLocal.copy();
      }
      else {
         myGridToLocal = null;
      }
      myTCL.set (grid.myTCL);

      myRadius = grid.myRadius;

      myNx = grid.myNx;
      myNy = grid.myNy;
      myNz = grid.myNz;
      myNxNy = grid.myNxNy;

      setLocalToWorld (grid.myTLocalToWorld);
   }

   /**
    * Clears the color map used to specify vertex colors. When no color map is
    * present, vertices are rendered using the point color of the render
    * properties passed to the render method.
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
    * Sets the color used to render a specific vertex. If no color map
    * is currently present, one is created.
    * 
    * @param idx vertex index
    * @param color rendering color for the vertex
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
    * Returns the color used for rendering a specific vertex when using a color
    * map. If no color map is currently present, <code>null</code> is returned.
    * 
    * @param idx vertex index
    * @return rendering color for the vertex
    */
   public Color getVertexColor (int idx) {
      if (myColorMap == null) {
         return null;
      }
      else {
         return myColors.get(myColorIndices[idx]);
      }
   }

   /**
    * Sets the default color used for rendering vertices. If no color map
    * is currently present, one is initialized.
    *
    * @param color default vertex color when using a color map.
    */
   public void setDefaultVertexColor (Color color) {
      if (myColorMap == null) {
         initColorMap();
      }
      myColors.set (0, color);
      myRobValid = false;
   }

  /**
    * Returns the default color used for rendering vertices when a color map is
    * specified. If no color map is currently present, then <code>null</code>
    * is returned.
    *
    * @return default vertex color when using a color map.
    */
   public Color getDefaultVertexColor() {
      if (myColorMap == null) {
         return null;
      }
      else {
         return myColors.get(0);
      }
   }
   
   /**
    * Sets the center and orientation of this grid with respect to local
    * coordinates, by means of the transform <code>TCL</code> whose rotation
    * matrix <code>R</code> gives the orientation and offset vector
    * <code>p</code> gives the center. All other aspects of the grid remain
    * unchanged.
    *
    * @param TCL transform giving the center and orientation of the
    * grid with respect to local coordinates.
    */
   public void setCenterAndOrientation (RigidTransform3d TCL) {
      myTCL.set (TCL);
      updateGridToLocal();
   }

   /**
    * Returns the transform TCL that describes the center and orientation of
    * this grid with respect to local coordinates.
    *
    * @return transform giving the center and orientation with
    * respect to local coordinates. Should not be modified.
    */
   public RigidTransform3d getCenterAndOrientation() {
      return myTCL;
   }

   /**
    * Returns the center of this grid with respect to its local coordinates.
    * 
    * @param center returns the grid center
    */  
   public void getCenter (Vector3d center) {
      center.set (myTCL.p);
   }
   
   /**
    * Sets the center of this grid with respect to its local coordinates.
    * 
    * @param center new grid center
    */  
   public void setCenter (Vector3d center) {
      myTCL.p.set (center);
      updateGridToLocal();
   }
   
   /**
    * Returns the orientation of this grid with respect to its local
    * coordinates.
    * 
    * @param R grid orientation in local coordinates
    */
   public void getOrientation (RotationMatrix3d R) {
      R.set (myTCL.R);
   }

   /**
    * Sets the orientation of this grid with respect to its local coordinates.
    * 
    * @param R new grid orientation in local coordinates
    */
   public void setOrientation (RotationMatrix3d R) {
      myTCL.R.set (R);
      updateGridToLocal();
   }

   /**
    * Returns the orientation of this grid with respect to world coordinates.
    * 
    * @param R grid orientation in world coordinates
    */
   public void getWorldOrientation (RotationMatrix3d R) {
      getOrientation (R);
      R.mul (myTLocalToWorld.R, R);
   }
   
   /**
    * Returns the center of this grid with respect to world coordinates.
    * 
    * @param center returns the grid center
    */
   public void getWorldCenter (Point3d center) {
      getCenter (center);
      myLocalToWorld.transformPnt (center, center);
   }

   protected RigidTransform3d getGridToLocal() {
      RigidTransform3d TGL = new RigidTransform3d();
      getOrientation (TGL.R);
      return TGL;
   }

   protected void updateGridToLocal () {
      Vector3d origin = new Vector3d();
      Vector3d cwidths = getCellWidths();
      origin.scale (-0.5, myWidths);
      if (myTCL.R.isIdentity()) {
         origin.add (myTCL.p);
         myGridToLocal = new ScaledTranslation3d (cwidths, origin);
      }
      else {
         origin.transform (myTCL.R);
         origin.add (myTCL.p);
         myGridToLocal =
            new ScaledRigidTransformer3d (cwidths, myTCL.R, origin);
      }      
   }
   
   protected boolean transformToGrid (Vector3d pgrid, Point3d ploc) {
      boolean inside = true;

      myGridToLocal.inverseTransformPnt (pgrid, ploc);
      if (pgrid.x < 0) {
         pgrid.x = 0;
         inside = false;
      }
      else if (pgrid.x > myNx-1) {
         pgrid.x = myNx-1;
         inside = false;
      }
      if (pgrid.y < 0) {
         pgrid.y = 0;
         inside = false;
      }
      else if (pgrid.y > myNy-1) {
         pgrid.y = myNy-1;
         inside = false;
      }
      if (pgrid.z < 0) {
         pgrid.z = 0;
         inside = false;
      }
      else if (pgrid.z > myNz-1) {
         pgrid.z = myNz-1;
         inside = false;
      }
      return inside;
   }

   protected boolean getCellCoords (
      Vector3i xyzi, Vector3d coords, Point3d point) {

      Vector3d pgrid = new Vector3d();
      boolean inside = transformToGrid (pgrid, point);

      xyzi.set (pgrid);

      if (xyzi.x == myNx - 1) {
         xyzi.x -= 1;
      }
      if (xyzi.y == myNy - 1) {
         xyzi.y -= 1;
      }
      if (xyzi.z == myNz - 1) {
         xyzi.z -= 1;
      }
      if (coords != null) {
         coords.x = pgrid.x - xyzi.x;
         coords.y = pgrid.y - xyzi.y;
         coords.z = pgrid.z - xyzi.z;
      }
      return inside;
   }

   /**
    * Returns the x, y, z indices of the minimum vertex of the cell containing
    * a given point in local coordinates. If <code>point</code> is outside the
    * grid, <code>null</code> is returned.
    *
    * @param xyzi returns the x, y, z indices. If specified as
    * <code>null</code>, then the containing vector is allocated internally.
    * @param point point for which the cell vertex is desired (local coordinates)
    * @return vector containing the cell vertex indices, or
    * <code>null</code> if <code>point</code> is outside the grid.
    */
   public Vector3i getCellVertex (Vector3i xyzi, Point3d point) {
      if (xyzi == null) {
         xyzi = new Vector3i();
      }
      if (getCellCoords (xyzi, null, point)) {
         return xyzi;
      }
      else {
         return null;
      }
   }      
   
   /**
    * Given a vertex index <code>vi</code>, compute the corresponding
    * x, y, z indices.
    * 
    * @param vxyz returns the x, y, z indices. 
    * @param vi vertex index
    * @return reference to <code>vxyz</code> 
    */
   public Vector3i vertexToXyzIndices (Vector3i vxyz, int vi) {
      vxyz.z = vi / (myNxNy);
      vxyz.y = (vi - vxyz.z * myNxNy) / myNx;
      vxyz.x = vi % myNx;
      return vxyz;
   }
   
   /**
    * Given the x, y, z indices for a vertex, compute the corresponding
    * vertex index.
    * 
    * @param vxyz x, y, z indices
    * @return vertex index
    */
   public int xyzIndicesToVertex (Vector3i vxyz) {
      return vxyz.x + vxyz.y*myNx + vxyz.z*myNxNy;
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
      return xi + yj*myNx + zk*myNxNy;
   }
   
   /**
    * Returns the resolution of this grid along each axis. This
    * equals the number of cells along each axis.
    * 
    * @return number of cells along each axis
    */
   public Vector3i getResolution() {
      return new Vector3i (myNx-1, myNy-1, myNz-1);
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
      Vector3d cwidths = new Vector3d(myWidths);
      cwidths.x /= (myNx-1);
      cwidths.y /= (myNy-1);
      cwidths.z /= (myNz-1);
      return cwidths;
   }

   /**
    * Returns the widths of this grid along the x, y, and z axes.
    * 
    * @return grid widths (passed by value, can be modified)
    */
   public Vector3d getWidths() {
      return new Vector3d (myWidths);
   }
   
   /**
    * Returns the widths of this grid along the x, y, and z axes.
    * 
    * @param widths returns the grid widths
    */
   public void getWidths(Vector3d widths) {
      widths.set (myWidths);
   }
   
   /**
    * Returns the total number of vertices in this grid.
    *
    * @return number of grid vertices
    */
   public int numVertices() {
      return myNx*myNy*myNz;
   }

   /**
    * For grid subclasses wishing to render 3D vector information, this method
    * should be overridden to optionally returns a vector quantity associated
    * with the vertex at indices {@code xi}, {@code yj}, and {@code zk}. Should
    * returns {@code null} if no vector information should be rendered at that
    * vertex.
    *
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    */
   public Vector3d getRenderVector (int xi, int yj, int zk) {
      return null;
   }

   /**
    * For grid subclasses wishing to render 3D vector information, this method
    * should be overridden to return a scaling factor to be applied to vectors
    * returned by {@link #getRenderVector} to determine the length of the
    * actual rendered vector. Returning 0 disables vector rendering.
    *
    * @return scaling factor for rendered vectors
    */
   public double getRenderVectorScale() {
      return 0;
   }

   /**
    * Creates a new render object for rendering the points and vectors of this
    * grid.
    * 
    * @return new render object
    */
   RenderObject buildRenderObject (RenderProps props) {
      RenderObject rob = new RenderObject();

      if (props.getDrawEdges()) {
         // create line groups for vectors and edges
         rob.createLineGroup();
         rob.createLineGroup();
      }
      else {
         // just one line group for vectors
         rob.createLineGroup();
      }

      double veclen = getRenderVectorScale();

      if (myColorMap != null) {
         for (Color color : myColors) {
            rob.addColor (color);
         }
      }
      int vidx=0;
      Vector3d coords = new Vector3d();
      int xlo = myRenderRanges[0];
      int xhi = Math.min(myNx,myRenderRanges[1]);
      int ylo = myRenderRanges[2];
      int yhi = Math.min(myNy,myRenderRanges[3]);
      int zlo = myRenderRanges[4];
      int zhi = Math.min(myNz,myRenderRanges[5]);

      rob.lineGroup (VECTOR_GROUP);
      for (int zk=zlo; zk<zhi; zk++) {
         for (int yj=ylo; yj<yhi; yj++) {
            for (int xi=xlo; xi<xhi; xi++) {
               coords.set (xi, yj, zk);
               myGridToLocal.transformPnt (coords, coords);
               int cidx = myColorMap != null ? myColorIndices[vidx] : -1;
               rob.addPosition (coords);
               rob.addVertex (vidx, -1, cidx, -1);
               rob.addPoint (vidx++);
            }
         }
      }
      if (veclen != 0) {
         int pidx = 0;
         for (int zk=zlo; zk<zhi; zk++) {
            for (int yj=ylo; yj<yhi; yj++) {
               for (int xi=xlo; xi<xhi; xi++) {
                  Vector3d vec = getRenderVector (xi, yj, zk);
                  if (vec != null) {
                     float[] pos = rob.getPosition(pidx);
                     coords.set (pos[0], pos[1], pos[2]);
                     coords.scaledAdd (veclen, vec);
                     int cidx = myColorMap != null ? myColorIndices[pidx] : -1;
                     rob.addPosition (coords);
                     rob.addVertex (vidx, -1, cidx, -1);
                     rob.addLine (pidx, vidx);
                     vidx++;
                  }
                  pidx++;
               }
            }
         }
      }
      if (props.getDrawEdges()) {
         rob.lineGroup (EDGE_GROUP);
         int nvx = xhi-xlo;
         int nvy = yhi-ylo;
         int nvz = zhi-zlo;
         int nvxy = nvx*nvy;
         // lines parallel to x
         if (nvx > 1) {
            for (int j=0; j<nvy; j++) {
               for (int k=0; k<nvz; k++) {
                  int vidx0 = (j*nvx + k*nvxy);
                  int vidx1 = vidx0 + (nvx-1);
                  rob.addLine (vidx0, vidx1);
               }
            }
         }
         // lines parallel to y
         if (nvy > 1) {
            for (int i=0; i<nvx; i++) {
               for (int k=0; k<nvz; k++) {
                  int vidx0 = (i + k*nvxy);
                  int vidx1 = vidx0 + (nvy-1)*nvx;
                  rob.addLine (vidx0, vidx1);
               }
            }
         }
         // lines parallel to z
         if (nvz > 1) {
            for (int i=0; i<nvx; i++) {
               for (int j=0; j<nvy; j++) {
                  int vidx0 = (i + j*nvx);
                  int vidx1 = vidx0 + (nvz-1)*nvxy;
                  rob.addLine (vidx0, vidx1);
               }
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

   /**
    * {@inheritDoc}
    */
   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public int numSelectionQueriesNeeded() {
      return -1;
   }

   /**
    * {@inheritDoc}
    */
   public void prerender (RenderList list) {
      prerender (myRenderProps);
   }

   public void prerender (RenderProps props) {
      if (myRob != null &&
          props.getDrawEdges() != (myRob.numLineGroups()==2)) {
         myRobValid = false;
      }
      if (myRob == null || !myRobValid) {
         myRob = buildRenderObject(props);
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

      boolean highlight = ((flags & Renderer.HIGHLIGHT) != 0);

      if (myTLocalToWorld != null) {
         renderer.pushModelMatrix();
         renderer.mulModelMatrix (myTLocalToWorld);
      }
      RenderObject rob = myRob;
      if (rob != null) {
         if (props.getPointStyle() == PointStyle.POINT) {
            if (props.getPointSize() != 0) {
               renderer.setPointColoring (props, highlight);
               renderer.drawPoints (rob, PointStyle.POINT, props.getPointSize());
            }
         }
         else {
            if (props.getPointRadius() > 0) {
               renderer.setPointColoring (props, highlight);
               renderer.drawPoints (
                  rob, props.getPointStyle(), props.getPointRadius());
            }
         }
         if (props.getLineStyle() == LineStyle.LINE) {
            if (props.getLineWidth() != 0) {
               rob.lineGroup (VECTOR_GROUP);
               renderer.setLineColoring (props, highlight);
               renderer.drawLines (rob, LineStyle.LINE, props.getLineWidth());
            }
         }
         else {
            if (props.getLineRadius() > 0) {
               rob.lineGroup (VECTOR_GROUP);
               renderer.setLineColoring (props, highlight);
               renderer.drawLines (
                  rob, props.getLineStyle(), props.getLineRadius());
            }
         }

         if (props.getDrawEdges() && rob.numLineGroups() == 2) {
            if (props.getEdgeWidth() > 0) {
               rob.lineGroup (EDGE_GROUP);
               renderer.setEdgeColoring (props, highlight);
               renderer.drawLines (rob, LineStyle.LINE, props.getEdgeWidth());
            }
         }
      }
      if (myTLocalToWorld != null) {
         renderer.popModelMatrix();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      Point3d w = new Point3d();
      for (int i=0; i<8; i++) {
         // update bounds using the eight corners of the grid, defined
         // in grid coords and then transformed into world coordinates
         double gx = (i < 4 ? 0 : myNx-1);
         double gy = (((i/2)%2) == 0 ? 0 : myNy-1);
         double gz = ((i%2) == 0 ? 0 : myNz-1);
         w.set (gx, gy, gz);
         myGridToLocal.transformPnt (w, w);
         myLocalToWorld.transformPnt (w, w);
         w.updateBounds(pmin, pmax);
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getRenderHints() {
      return 0;
   }

   /* --- end of Renderable implementation --- */

   /** 
    * Returns the vertex index of the nearest vertex to a point in local
    * coordinates.
    *
    * @param point point for which to calculate nearest vertex (local
    * coordinates)
    * @return index of nearest vertex to <code>point</code>
    */
   public int getClosestVertex (Point3d point) {

      // Change to grid coordinates
      Point3d pgrid = new Point3d();
      myGridToLocal.inverseTransformPnt (pgrid, point);
      int xi = (int)Math.rint(pgrid.x);
      int yi = (int)Math.rint(pgrid.x);
      int zi = (int)Math.rint(pgrid.x);
      if (xi < 0) {
         xi = 0;
      }
      else if (xi > myNx-1) {
         xi = myNx-1;
      }
      if (yi < 0) {
         yi = 0;
      }
      else if (yi > myNy-1) {
         yi = myNy-1;
      }
      if (zi < 0) {
         zi = 0;
      }
      else if (zi > myNz-1) {
         zi = myNz-1;
      }
      return xyzIndicesToVertex (xi, yi, zi);
   }

   /**
    * Find the local coordinates of a vertex, as specified by its vertex index.
    * 
    * @param coords returns the coordinates
    * @param vi vertex index
    * @return coords
    */
   public Vector3d getLocalVertexCoords (Vector3d coords, int vi) {
      if (coords == null) {
         coords = new Point3d();
      }
      Vector3i vxyz = new Vector3i();
      coords.set (vertexToXyzIndices (vxyz, vi));
      myGridToLocal.transformPnt (coords, coords);
      return coords;
   }
   
   /**
    * Find the local coordinates of a vertex, as specified by its x, y, z
    * indices.
    * 
    * @param coords returns the coordinates
    * @param vxyz x, y, z vertex indices
    * @return coords
    */
   public Vector3d getLocalVertexCoords (Vector3d coords, Vector3i vxyz) {
      if (coords == null) {
         coords = new Point3d();
      }
      coords.set (vxyz);
      myGridToLocal.transformPnt (coords, coords);
      return coords;
   }

   /**
    * Find the world coordinates of a vertex, as specified by its x, y, z
    * indices.
    * 
    * @param coords returns the coordinates
    * @param vxyz x, y, z vertex indices
    * @return coords
    */
   public Vector3d getWorldVertexCoords (Vector3d coords, Vector3i vxyz) {
      Point3d pnt = new Point3d(vxyz.x, vxyz.y, vxyz.z);
      myGridToLocal.transformPnt (pnt, pnt);
      myLocalToWorld.transformPnt (pnt, pnt);
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

   /**
    * Returns a string describing the render ranges for this grid, as described
    * for {@link #setRenderRanges}.
    *
    * @return render range string 
    */
   public String getRenderRanges() {
      StringBuilder sbuild = new StringBuilder();
      for (int i=0; i<3; i++) {
         int lo = myRenderRanges[2*i];
         int hi = myRenderRanges[2*i+1];
         if (lo == 0 && hi == MAX_INT) {
            sbuild.append ("*");
         }
         else if (lo == (hi-1)) {
            sbuild.append (lo);
         }
         else {
            sbuild.append (lo + ":" + (hi-1));
         }
         if (i < 2) {
            sbuild.append (" ");
         }
      }
      return sbuild.toString();
   }

   /**
    * Sets the render ranges for this grid. The render ranges control the range
    * of vertices that are rendering along each axis when the grid is being
    * rendered.
    *
    * <p>The format
    * consists of three separate range descriptors, one for each axis.
    * A range descriptor may be '*' (all vertices), 'n:m' (vertices
    * in the index range n to m, inclusive), or 'n' (vertices only at index n).
    * For example:
    * <pre>
    *   "* * *"     - all vertices      
    *   "* 7 *"     - all vertices along x and z, and those at index 7 along y
    *   "0 2 3"     - a single vertex at indices (0, 2, 3)
    *   "0:3 4:5 *" - all vertices between indices 0-3 along x, and 4-5 along y
    * </pre>
    * In addition, the single charater "*" also indicates all vertices.
    * @param str render range specification
    * @throws IllegalArgumentException if the range specification is not valid.
    */
   public void setRenderRanges (String str) {
      StringHolder errMsg = new StringHolder();
      int[] ranges = parseRenderRanges (str, errMsg);
      if (ranges != null) {
         adjustRenderRanges (ranges);
         myRenderRanges = ranges;
         myRobValid = false;
      }
      else {
         throw new IllegalArgumentException (
            "Error parsing string: " + errMsg.value);
      }
   }

   /**
    * Parses a render range specification for this grid. The specification must
    * have the format described for {@link #setRenderRanges}. This method does
    * not set anything; it simply parses the ranges and converts then into an
    * integer array of length 6, giving the upper and lower bounds along each
    * of the x, y, and z axes. For the ``all'' specifier ({@code '*'}), the
    * values {@code 0} and {@code Integer.MAX_VALUE} are used. Applications can
    * use this method to test if a range specification is valid prior to
    * calling {@link #setRenderRanges}.  If the specification is invalid, then
    * an error message is placed in <code>errorMsg</code> and the method
    * returns <code>null</code>.
    * 
    * @param str range specification
    * @param errorMsg returns an error message in case of an error
    * @return array giving the ranges, or <code>null</code> if there
    * is an error
    */
   public static int[] parseRenderRanges (String str, StringHolder errorMsg) {

      String[] strs = str.split ("\\s+");
      if (strs.length > 0 && strs[0].equals ("")) {
         strs = Arrays.copyOfRange (strs, 1, strs.length);
      }
      int[] ranges = new int[6];
      String error = null;
      if (strs.length == 1 && strs[0].equals ("*")) {
         ranges[0] = 0;
         ranges[1] = MAX_INT;
         ranges[2] = 0;
         ranges[3] = MAX_INT;
         ranges[4] = 0;
         ranges[5] = MAX_INT;
      }
      else if (strs.length > 3) {
         error = "More than three subranges specified";
      }
      else {
         int i = 0;
         for (String s : strs) {
            int lo = -1;
            int hi = 0;
            if (s.equals ("*")) {
               lo = 0;
               hi = MAX_INT;
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
               hi = parsePositiveInt (substrs[1])+1;
               if (lo >= hi) {
                  error = "Low > high in subrange";
                  break;
               }
               if (lo < 0 || hi <= 0) {
                  error = "Malformed or negative integer";
                  break;
               }
            }
            else {
               lo = parsePositiveInt (s);
               if (lo < 0) {
                  error = "Malformed or negative integer";
                  break;
               }
               hi = lo+1;
            }
            ranges[2*i] = lo;
            ranges[2*i+1] = hi;
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
    * Returns the radius of this grid, defined as half the distance across its
    * maximal diagonal.
    * 
    * @return radius of this grid
    */
   public double getRadius() {
      return myRadius;
   }

   protected static boolean scanAttributeName (ReaderTokenizer rtok, String name)
      throws IOException {
      if (rtok.ttype == ReaderTokenizer.TT_WORD && rtok.sval.equals (name)) {
         rtok.scanToken ('=');
         return true;
      }
      return false;
   }

   protected boolean scanItem (
      ReaderTokenizer rtok, Object ref) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "resolution")) {
         Vector3i resolution = new Vector3i();
         resolution.scan (rtok);
         setResolution (resolution);
         return true;
      }
      else if (scanAttributeName (rtok, "widths")) {
         Vector3d widths = new Vector3d();
         widths.scan (rtok);
         setWidths (widths);
         return true;
      }
      else if (scanAttributeName (rtok, "center")) {
         myTCL.p.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "orientation")) {
         myTCL.R.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "LocalToWorld")) {
         myTLW = new RigidTransform3d();
         myTLW.scan (rtok);
         return true;
      }         
      rtok.pushBack();
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      myTCL = new RigidTransform3d();
      myTLW = null;
      myTLocalToWorld = null;
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!scanItem (rtok, ref)) {
            throw new IOException ("Unexpected token: " + rtok);
         }         
      }
      updateGridToLocal();
      if (myTLW != null) {
         setLocalToWorld (myTLW);
      }
   }

   protected void writeItems (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      Vector3d widths = getWidths();
      Vector3d center = new Vector3d();
      getCenter (center);
      RotationMatrix3d R = new RotationMatrix3d();
      getOrientation (R);
      pw.println ("resolution=[" + getResolution() +"]");
      pw.println ("widths=[" + widths.toString(fmt) +"]");
      pw.println ("center=[" + center.toString(fmt) +"]");
      if (!R.isIdentity()) {
         pw.println (
            "orientation=" + R.toString(fmt, RotationMatrix3d.AXIS_ANGLE_STRING));
      }
      if (myTLocalToWorld != null) {
         pw.println ("LocalToWorld=\n" + myTLocalToWorld);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {

      pw.println ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      writeItems (pw, fmt, ref);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public boolean isWritable () {
      return true;
   }

   /**
    * Returns <code>true</code> if this grid base equals another within a
    * prescribed tolerance. The grid bases are equal if the resolution, widths,
    * center, and orientation are equal. Specifying {@code tol = 0} requires
    * exact equality.
    *
    * @param grid grid base to compare against
    * @param tol floating point tolerance (absolute)
    */
   public boolean epsilonEquals (InterpolatingGridBase grid, double tol) {
      Vector3i resolution = getResolution();
      if (!resolution.equals (grid.getResolution())) {
         return false;
      }
      if (!myWidths.epsilonEquals (grid.myWidths, tol)) {
         return false;
      }
      RigidTransform3d TCL = new RigidTransform3d();
      RigidTransform3d gridTCL = new RigidTransform3d();
      getOrientation (TCL.R);
      getCenter (TCL.p);
      getOrientation (gridTCL.R);
      getCenter (gridTCL.p);
      if (!TCL.epsilonEquals (gridTCL, tol)) {
         return false;
      }
      return true;
   }

   public int getDebug () {
      return myDebug;
   }

   public void setDebug (int level) {
      myDebug = level;
   }

   protected void scaleTransformer (VectorTransformer3d transformer, double s) {
      if (transformer instanceof ScaledTranslation3d) {
         ScaledTranslation3d xform = (ScaledTranslation3d)transformer;
         xform.scaleDistance (s);
      }
      else if (transformer instanceof ScaledRigidTransformer3d) {
         ScaledRigidTransformer3d xform = (ScaledRigidTransformer3d)transformer;
         xform.scaleDistance (s);
      }
   }

   /**
    * Scales the distance units of the grid, which entails
    * scaling the widths and coordinate transform information.
    *
    * @param s scaling factor
    */
   public void scaleDistance (double s) {
      myWidths.scale (s);
      myRadius *= s;
      if (myTLocalToWorld != null) {
         myTLocalToWorld.p.scale (s);
      }
      if (myGridToLocal != null) {
         scaleTransformer (myGridToLocal, s);
      }
      myRobValid = false;
   }

   
 
}
