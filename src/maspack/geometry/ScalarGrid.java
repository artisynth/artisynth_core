/*
 * Copyright (c) 2017, by the Authors: Bruce Haines (UBC), Antonio Sanchez
 * (UBC), John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * thze LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;

/**
 * Implements a regular 3D grid that interpolates scalar values using
 * trilinear interpolation.
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
 * <p>Scalar values at any point within the grid are be obtained by trilinear
 * interpolation of the values at surrounding vertices. Queries can be made for
 * points in either local or world coordinates, using {@link #getLocalValue} or
 * {@link #getWorldValue}. If the query point is outside the grid, then the
 * special value {@link #OUTSIDE_GRID} is returned.
 *
 * <p>Values at the grid vertices can be assigned collectively or individually,
 * using {@link #setVertexValues} or {@link #setVertexValue}. Values at grid
 * vertices can be queried using {@link #getVertexValue}.
 */
public class ScalarGrid extends ScalarGridBase {
   
   /**
    * Default constructor. Should not be called by applications, unless
    * {@link #scan} is called immediately after.
    */
   public ScalarGrid() {
      super();
   }
   
   /**
    * Creates a new grid with specified widths and resolution, centered on the
    * local coordinate system.  The grid values are initialized to zero.
    * 
    * @param widths widths along the x, y, and z axes
    * @param resolution cell resolution along the x, y, and z axes
    */
   public ScalarGrid (
      Vector3d widths, Vector3i resolution) {
      initGrid (widths, resolution, null);
   }


   /**
    * Creates a new grid with specified widths, resolution, and position and
    * orientation of the center given by <code>TCL</code>.  The grid values
    * are initialized to zero.
    * 
    * @param widths widths along the x, y, and z axes
    * @param resolution cell resolution along the x, y, and z axes
    * @param TCL if not {@code null}, describes the position and orientation 
    * of the grid center in local coordinates.
    */
   public ScalarGrid (
      Vector3d widths, Vector3i resolution, RigidTransform3d TCL) {
      initGrid (widths, resolution, TCL);
   }

   /**
    * Creates a new grid, axis-aligned and centered on the origin, with the
    * specified resolution and x, y, z widths set to 1.  The grid values are
    * initialized to zero.
    *
    * @param resolution cell resolution along the x, y, and z axes
    */
   public ScalarGrid (Vector3i resolution) {
      initGrid (null, resolution, null);
   }

   /**
    * Creates a new grid that is a copy of an existing grid.
    *
    * @param grid grid to copy
    */
   public ScalarGrid (ScalarGrid grid) {
      set (grid);
   }

   /**
    * Sets this grid to be a copy of an existing grid.
    *
    * @param grid grid to copy
    */
   public void set (ScalarGrid grid) {
      super.set (grid);
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
    * @param values value for each vertex. Must have a length
    * {@code >=} {@link #numVertices}.
    */
   public void setVertexValues (double[] values) {
      super.setVertexValues (values);
   }

   /**
    * Returns an array of the values at each vertex. See {@link
    * #setVertexValues} for a description of how vertices are indexed with
    * respect to this array.
    * 
    * @return array of vertex values.
    */
   public double[] getVertexValues() {
      return super.getVertexValues();
   }

   /**
    * Sets the value for the vertex indexed by {@code vi}. See {@link
    * #setVertexValues} for a description of how vertices are indexed.
    *
    * @param vi vertex index
    * @param value vertex value
    */
   public void setVertexValue (int vi, double value) {
      super.setVertexValue (vi, value);
   }

   /**
    * {@inheritDoc}
    */
   public void setVertexValue (int xi, int yi, int zi, double value) {
      super.setVertexValue (xi, yi, zi, value);
   }

   /**
    * Queries the value for the vertex indexed by {@code vi}. See {@link
    * #setVertexValues} for a description of how vertices are indexed.
    * 
    * @param vi vertex index
    * @return value at the vertex
    */
   public double getVertexValue (int vi) {
      return super.getVertexValue (vi);
   }

   /**
    * Queries the value at a specified vertex, as specified by x, y, z indices.
    * 
    * @param vxyz x, y, z vertex indices
    * @return value at the vertex
    */
   public double getVertexValue (Vector3i vxyz) {
      return super.getVertexValue (vxyz);
   }
   
   /**
    * Queries the value at a specified vertex, as specified by x, y, z indices.
    * 
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return value at the vertex
    */   
   public double getVertexValue (int xi, int yj, int zk) {
      return super.getVertexValue (xi, yj, zk);
   }

   /**
    * Zeros all the vertex values for this grid.
    */
   public void zeroVertexValues () {
      super.zeroVertexValues();
   }
   
   /** 
    * Calculates the value at an arbitrary point in world coordinates using
    * multilinear interpolation of the vertex values for the grid cell
    * containing the point.  If the point lies outside the grid volume, {@link
    * #OUTSIDE_GRID} is returned.
    *
    * @param point point at which to calculate the value
    * (world coordinates)
    * @return interpolated value, or <code>OUTSIDE_GRID</code>
    */
   public double getWorldValue (Point3d point) {
      return super.getWorldValue (point);
   }

   /** 
    * Calculates the value at an arbitrary point in world coordinates using
    * multilinear interpolation of the vertex values for the grid cell
    * containing the point.  If the point lies outside the grid volume, then
    * the method returns either the nearest grid value or {@link
    * #OUTSIDE_GRID}, depending on whether {@code clipToGrid} is {@code true}
    * or false.
    *
    * @param point point at which to calculate the value
    * (world coordinates)
    * @param clipToGrid if {@code true}, return the nearest grid value if
    * {@code point} is outside the grid volume
    * @return interpolated value, or <code>OUTSIDE_GRID</code>
    */
   public double getWorldValue (Point3d point, boolean clipToGrid) {
      return super.getWorldValue (point, clipToGrid);
   }

   /** 
    * Calculates the value at an arbitrary point in local coordinates using
    * multilinear interpolation of the vertex values for the grid cell
    * containing the point.  If the point lies outside the grid volume, {@link
    * #OUTSIDE_GRID} is returned.
    *
    * @param point point at which to calculate the normal and value
    * (local coordinates)
    * @return interpolated value, or <code>OUTSIDE_GRID</code>
    */
   public double getLocalValue (Point3d point) {
      return super.getLocalValue (point);
   }

   /** 
    * Calculates the value at an arbitrary point in local coordinates using
    * multilinear interpolation of the vertex values for the grid cell
    * containing the point.  If the point lies outside the grid volume, then
    * the method returns either the nearest grid value or {@link
    * #OUTSIDE_GRID}, depending on whether {@code clipToGrid} is {@code true}
    * or false.
    *
    * @param point point at which to calculate the normal and value
    * (local coordinates)
    * @param clipToGrid if {@code true}, return the nearest grid value if
    * {@code point} is outside the grid volume
    * @return interpolated value, or <code>OUTSIDE_GRID</code>
    */
   public double getLocalValue (Point3d point, boolean clipToGrid) {
      return super.getLocalValue (point, clipToGrid);
   }

   /**
    * Returns <code>true</code> if this grid equals another within a prescribed
    * tolerance. The grids are equal if the resolution, widths, center,
    * orientation and values are equal. Specifying {@code tol = 0} requires
    * exact equality.
    *
    * @param grid grid to compare against
    * @param tol floating point tolerance (absolute)
    * @return {@code true} if the grids are equal
    */
   public boolean epsilonEquals (ScalarGrid grid, double tol) {
      return super.epsilonEquals (grid, tol);
   }
}

