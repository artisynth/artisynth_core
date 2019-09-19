/*
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * thze LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import maspack.matrix.Matrix;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorObject;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.ParameterizedClass;


/**
 * Implements a regular 3D grid that interpolates {@link VectorObject} values
 * using trilinear interpolation. The VectorObjects used are assumed to be of a
 * fixed size. For the variable sized objects {@link VectorNd} and {@link
 * MatrixNd}, one should use {@link VectorNdGrid} and {@link MatrixNdGrid}
 * instead.
 * 
 * <p>The grid is implemented using a regular 3D grid composed of
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
 * <p>Values at any point within the grid are be obtained by trilinear
 * interpolation of the values at surrounding vertices. Queries can be made for
 * points in either local or world coordinates, using {@link #getLocalValue} or
 * {@link #getWorldValue}. If the query point is outside the grid, then {@code
 * null} is returned.
 *
 * <p>Values at the grid vertices can be assigned collectively or individually,
 * using {@link #setVertexValues} or {@link #setVertexValue}. Values at grid
 * vertices can be queried using {@link #getVertexValue}.
 */
public class VectorGrid<T extends VectorObject<T>> 
   extends InterpolatingGridBase implements ParameterizedClass {
   
   protected Class<T> myTypeParameter;
   protected ArrayList<T> myValues;  // values at each vertex

   protected static final double INF = Double.POSITIVE_INFINITY;
   
   protected void initType (Class<T> type) {
      if (type == null) {
         throw new IllegalArgumentException ("type cannot be null");
      }
      myTypeParameter = type;
      // make sure the type has a fixed size
      if (Vector.class.isAssignableFrom (type)) {
         T value = createInstance();
         if (!((Vector)value).isFixedSize()) {
            throw new IllegalArgumentException (
               "type "+type+" does not have a fixed size; " +
               "use VectorNdGridField instead");
         }
      }
      else if (Matrix.class.isAssignableFrom (type)) {
         T value = createInstance();
         if (!((Matrix)value).isFixedSize()) {
            throw new IllegalArgumentException (
               "type "+type+" does not have a fixed size; " +
               "use MatrixNdGridField instead");
         }
      }
   }     

   protected VectorGrid() {
      super();
   }
   
   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public VectorGrid (Class<T> type) {
      super();
      initType (type);
   }
   
  /**
    * Creates a new grid with specified widths, resolution, and position and
    * orientation of the center given by <code>TCL</code>.  The grid values
    * are initialized to zero.
    *
    * @param type class type for the {@link VectorObject} associated with this
    * grid.
    * @param widths widths along the x, y, and z axes
    * @param resolution cell resolution along the x, y, and z axes
    * @param TCL transformation giving the position and orientation
    * of the grid center. 
    */
   public VectorGrid (
      Class<T> type,
      Vector3d widths, Vector3i resolution, RigidTransform3d TCL) {

      initType (type);
      initGrid (widths, resolution, TCL);
   }

   /**
    * Creates a new grid, axis-aligned and centered on the origin, with the
    * specified resolution and x, y, z widths set to 1.  The grid values are
    * initialized to zero.
    *
    * @param type class type for the {@link VectorObject} associated with this
    * grid.
    * @param resolution cell resolution along the x, y, and z axes
    */
   public VectorGrid (Class<T> type, Vector3i resolution) {

      initType (type);
      initGrid (null, resolution, null);
   }

   /**
    * Creates a new grid that is a copy of an existing grid.
    *
    * @param grid grid to copy
    */
   public VectorGrid (VectorGrid<T> grid) {
      set (grid);
   }

   protected T createInstance() {
      try {
         return myTypeParameter.newInstance();
      }
      catch (Exception e) {
         throw new InternalErrorException (
            "Cannot create instance of "+myTypeParameter);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Class<T> getParameterType() {
      return myTypeParameter;
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean hasParameterizedType() {
      return true;
   }

   protected void initVertexValues (int numv) {
      myValues = new ArrayList<T>(numv);
      for (int i=0; i<numv; i++) {
         T value = createInstance();
         value.setZero();
         myValues.add (value);
      }      
   }

   /**
    * Sets this grid to be a copy of an existing grid.
    *
    * @param grid grid to copy
    */
   public void set (VectorGrid<T> grid) {

      super.set (grid);
      myTypeParameter = grid.myTypeParameter;
      if (grid.myValues != null) {
         myValues = new ArrayList<T>(grid.myValues.size());
         for (int i=0; i<grid.myValues.size(); i++) {
            T value = createInstance();
            value.set (grid.myValues.get(i));
            myValues.add (value);
         }
      }
      else {
         myValues = null;
      }
   }

   /**
    * Sets all the vertex values for this grid. The input array is indexed such
    * that for vertex indices xi, yj, zk, the corresponding index into this
    * array is
    * <pre>
    * idx = xi + nx*yj + (nx*ny)*zk
    * </pre>
    * where <code>nx</code> and <code>ny</code> are the number
    * of vertices along x and y axes.
    *
    * <p>The input values are copied internally and are not referenced by the
    * grid.
    *
    * @param values value for each vertex. Must have a length
    * {@code >=} {@link #numVertices}.
    */
   public void setVertexValues (T[] values) {
      int numv = numVertices();
      if (values.length < numv) {
         throw new IllegalArgumentException (
            "values.length=" + values.length +
            "; must be >= num vertices ("+numv+")");
      }
      for (int i=0; i<numv; i++) {
         setVertexValue (i, values[i]);
      }
      myRobValid = false;
   }

   /**
    * Returns the internal array of the values at each vertex. See {@link
    * #setVertexValues} for a description of how vertices are indexed with
    * respect to this array.
    *
    * <p>The returned values are those referenced directly by the grid.
    * 
    * @return array of vertex values.
    */
   public ArrayList<T> getVertexValues() {
      return myValues;
   }

   /**
    * Sets the value for the vertex indexed by {@code vi}. See {@link
    * #setVertexValues} for a description of how vertices are indexed.
    *
    * <p>The input value is copied internally and is not referenced by the
    * grid.
    *
    * @param vi vertex index
    * @param value vertex value
    */
   public void setVertexValue (int vi, T value) {
      int numv = numVertices();
      if (vi < 0 || vi >= numv) {
         throw new IndexOutOfBoundsException (
            "index is "+vi+", number of vertices is "+numv);
      }
      String sizeErr = checkSize (value);
      if (sizeErr != null) {
         throw new IllegalArgumentException (
            "value for vertex "+vi+": "+sizeErr);
      }
      T copy = createInstance();
      copy.set(value);
      myValues.set (vi, copy);
      myRobValid = false;
   }

   /**
    * Queries the value for the vertex indexed by {@code vi}. See {@link
    * #setVertexValues} for a description of how vertices are indexed.
    *
    * <p>The returned value is the internal value referenced by the grid.
    * 
    * @param vi vertex index
    * @return value at vertex
    */
   public T getVertexValue (int vi) {
      int numv = numVertices();
      if (vi < 0 || vi >= numv) {
         throw new IndexOutOfBoundsException (
            "index is "+vi+", number of vertices is "+numv);
      }
      return myValues.get (vi);
   }

   /**
    * Queries the value at a specified vertex, as specified by x, y, z indices.
    *
    * <p>The returned value is the internal value referenced by the grid.
    * 
    * @param vxyz x, y, z vertex indices
    * @return value at the vertex
    */
   public T getVertexValue (Vector3i vxyz) {
      return myValues.get(xyzIndicesToVertex(vxyz));
   }
   
   /**
    * Queries the value at a specified vertex, as specified by x, y, z indices.
    *
    * <p>The returned value is the internal value referenced by the grid.
    * 
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return value at the vertex
    */
   protected T getVertexValue (int xi, int yj, int zk) {
      return myValues.get(xyzIndicesToVertex(xi, yj, zk));
   }


   protected String checkSize (T value) {
      // only needed for types with non-fixed size
      return null;
   }
   
   /**
    * Zeros all the vertex values for this grid.
    */
   public void zeroVertexValues () {
      for (T value : myValues) {
         if (value != null) {
            value.setZero();
         }
      }
      myRobValid = false;
   }
   
   /** 
    * Calculates the value at an arbitrary point in world coordinates using
    * multilinear interpolation of the vertex values for the grid cell
    * containing the point.  If the point lies outside the grid volume, {@code
    * null} is returned.
    *
    * @param point point at which to calculate the value
    * (world coordinates).
    * @return interpolated value, or <code>null</code>.
    */
   public T getWorldValue (Point3d point) {
      Point3d lpnt = new Point3d();
      myLocalToWorld.inverseTransformPnt (lpnt, point);
      return getLocalValue(lpnt);
   }

   /** 
    * Calculates the value at an arbitrary point in local coordinates using
    * multilinear interpolation of the vertex values for the grid cell
    * containing the point.  If the point lies outside the grid volume, {@code
    * null} is returned.
    *
    * @param point point at which to calculate the normal and value
    * (local coordinates).
    * @return interpolated value, or <code>null</code>.
    */
   public T getLocalValue (Point3d point) {

      Vector3d coords = new Vector3d();
      Vector3i vidx = new Vector3i();
      if (!getCellCoords (vidx, coords, point)) {
         return null;
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

      T result = createInstance();
      result.setZero();
      result.scaledAddObj (w000, getVertexValue (vidx.x  , vidx.y  , vidx.z  ));
      result.scaledAddObj (w001, getVertexValue (vidx.x  , vidx.y  , vidx.z+1));
      result.scaledAddObj (w010, getVertexValue (vidx.x  , vidx.y+1, vidx.z  ));
      result.scaledAddObj (w011, getVertexValue (vidx.x  , vidx.y+1, vidx.z+1));
      result.scaledAddObj (w100, getVertexValue (vidx.x+1, vidx.y  , vidx.z  ));
      result.scaledAddObj (w101, getVertexValue (vidx.x+1, vidx.y  , vidx.z+1));
      result.scaledAddObj (w110, getVertexValue (vidx.x+1, vidx.y+1, vidx.z  ));
      result.scaledAddObj (w111, getVertexValue (vidx.x+1, vidx.y+1, vidx.z+1));
      
      return result;
   }

   public Vector3d getRenderVector (int xi, int yj, int zk) {
      if (myTypeParameter == Vector3d.class) {
         return (Vector3d)getVertexValue (xi, yj, zk);
      }
      else {
         return null;
      }
   }

   public double getRenderVectorScale() {
      return 1.0;
   }
   
   protected boolean scanItem (
      ReaderTokenizer rtok, Object ref) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         int numv = numVertices();
         myValues = new ArrayList<T>(numv);
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            T value = createInstance();
            value.scan (rtok, null);
            String sizeErr = checkSize (value);
            if (sizeErr != null) {
               throw new IOException ("scanned value: "+sizeErr);
            }
            myValues.add (value);
         }
         if (myValues.size() != numv) {
            throw new IOException (
               "input has "+myValues.size()+" values, expected "+numv);
         }
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, ref);      
   }
   
   protected void writeItems (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {

      super.writeItems (pw, fmt, ref);
      pw.println ("values=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      int numv = numVertices();
      if (Vector.class.isAssignableFrom (myTypeParameter)) {
         // place square brackets around the output. The scan method should be
         // able to handle this
         for (int i=0; i<numv; i++) {
            pw.print ("[ ");
            myValues.get(i).write (pw, fmt, null);
            pw.println (" ]");
         }    
      }
      else if (Matrix.class.isAssignableFrom (myTypeParameter)) {
         // place square brackets around the output, and adjust
         // indentation. The scan method should be able to handle this
         for (int i=0; i<numv; i++) {
            pw.print ("[ ");
            IndentingPrintWriter.addIndentation (pw, 2);
            myValues.get(i).write (pw, fmt, null);
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("]");
         }
      }
      else {
         for (int i=0; i<numv; i++) {
            myValues.get(i).write (pw, fmt, null);
            pw.println ("");
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   /**
    * Returns <code>true</code> if this grid equals another within a prescribed
    * tolerance. The grids are equal if the resolution, widths, center,
    * orientation and values are equal. Specifying {@code tol = 0} requires
    * exact equality.
    *
    * @param grid grid to compare against
    * @param tol floating point tolerance (absolute)
    */
   public boolean epsilonEquals (VectorGrid<T> grid, double tol) {
      if (!super.epsilonEquals (grid, tol)) {
         return false;
      }
      int numv = numVertices();
      for (int i=0; i<numv; i++) {
         if (!myValues.get(i).epsilonEquals(grid.myValues.get(i), tol)) {
            return false;
         }
      }
      return true;
   }
}

