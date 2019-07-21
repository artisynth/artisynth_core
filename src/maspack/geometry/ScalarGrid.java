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
    * Creates a new grid with specified widths, resolution, and position and
    * orientation of the center given by <code>TCL</code>.  The grid values
    * are initialized to zero.
    * 
    * @param widths widths along the x, y, and z axes
    * @param resolution cell resolution along the x, y, and z axes
    * @param TCL transformation giving the position and orientation of the grid
    * center, or {@code null} if the grid is centered and aligned with the
    * local coordinate frame.
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
    * {@inheritDoc}
    */
   public void set (ScalarGrid grid) {
      super.set (grid);
   }

   /**
    * {@inheritDoc}
    */
   public void setVertexValues (double[] values) {
      super.setVertexValues (values);
   }

   /**
    * {@inheritDoc}
    */
   public double[] getVertexValues() {
      return super.getVertexValues();
   }

   /**
    * {@inheritDoc}
    */
   public void setVertexValue (int vi, double value) {
      super.setVertexValue (vi, value);
   }

   /**
    * {@inheritDoc}
    */
   public double getVertexValue (int vi) {
      return super.getVertexValue (vi);
   }

   /**
    * {@inheritDoc}
    */
   public double getVertexValue (Vector3i vxyz) {
      return super.getVertexValue (vxyz);
   }
   
   /**
    * {@inheritDoc}
    */
   public double getVertexValue (int xi, int yj, int zk) {
      return super.getVertexValue (xi, yj, zk);
   }

   /**
    * {@inheritDoc}
    */
   public void zeroVertexValues () {
      super.zeroVertexValues();
   }
   
   /**
    * {@inheritDoc}
    */
   public double getWorldValue (Point3d point) {
      return super.getWorldValue (point);
   }

   /**
    * {@inheritDoc}
    */
   public double getLocalValue (Point3d point) {
      return super.getLocalValue (point);
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (ScalarGrid grid, double tol) {
      return super.epsilonEquals (grid, tol);
   }
}

