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
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.util.StringHolder;
import maspack.util.InternalErrorException;
import maspack.util.ReaderTokenizer;
import maspack.util.Scannable;
import maspack.util.NumberFormat;
import maspack.util.IndentingPrintWriter;

/**
 * Implements a regular 3D grid that interpolates {@link VectorNd} values using
 * trilinear interpolation. The class is implemented using {@code
 * VectorGrid<VectorNd>} as a base class, with augmentation to specify the size
 * of the {@link VectorNd} objects to be interpolated. This size must be
 * specified in the constructor, cannot be modified, and must be uniform
 * throughout the grid.
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
 *
 * <p>For more details, see the documentation for {@link VectorGrid}.
 */
public class VectorNdGrid extends VectorGrid<VectorNd> {

   protected int myVecSize;

   protected void initType (Class<VectorNd> type) {
      myTypeParameter = type;
   }

   /**
    * Returns the size of the vectors interpolated by this grid.
    *
    * @return vector size
    */
   public int getVectorSize() {
      return myVecSize;
   }

   private void initSize (int vsize) {
      if (vsize <= 0) {
         throw new IllegalArgumentException (
            "specified vector size is " + vsize + ", must be > 0");
      }
      myVecSize = vsize;
   }

   /**
    * Default constructor. Should not be called by applications, unless
    * {@link #scan} is called immediately after.
    */
   public VectorNdGrid () {
      super (VectorNd.class);
   }
   
   protected VectorNdGrid (int vsize) {
      super (VectorNd.class);
      initSize (vsize);
   }

   /**
    * Creates a new grid with specified vector size, widths, resolution, and
    * position and orientation of the center given by <code>TCL</code>.  The
    * grid values are initialized to zero.
    *
    * @param vsize size of the vectors to be interpolated
    * @param widths widths along the x, y, and z axes
    * @param resolution cell resolution along the x, y, and z axes
    * @param TCL transformation giving the position and orientation
    * of the grid center
    */
   public VectorNdGrid (
      int vsize, Vector3d widths, Vector3i resolution, RigidTransform3d TCL) {
      super (VectorNd.class, widths, resolution, TCL);
      initSize (vsize);
   }

   /**
    * Creates a new grid, axis-aligned and centered on the origin,
    * with the specified resolution and x, y, z widths set to 1.
    * The grid values are initialized to zero.
    *
    * @param vsize size of the vectors to be interpolated
    * @param resolution cell resolution along the x, y, and z axes
    */
   public VectorNdGrid (int vsize, Vector3i resolution) {
      super (VectorNd.class, resolution);
      initSize (vsize);
   }

   /**
    * Creates a new grid that is a copy of an existing grid.
    *
    * @param grid grid to copy
    */
   public VectorNdGrid (VectorNdGrid grid) {
      set (grid);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasParameterizedType() {
      return false;
   }
   
   /**
    * {@inheritDoc}
    */
   public void set (VectorNdGrid grid) {
      myVecSize = grid.myVecSize;
      super.set (grid);
   }

   protected VectorNd createInstance() {
      return new VectorNd (myVecSize);
   }

   @Override
   protected String checkSize (VectorNd value) {
      if (value.size() != myVecSize) {
         return ("size "+value.size()+
                 " incompatible with grid size of "+myVecSize);
      }
      else {
         return null;
      }
   } 

   protected void writeItems (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      
      pw.println ("size="+myVecSize);
      super.writeItems (pw, fmt, ref);
   } 

   protected boolean scanItem (
      ReaderTokenizer rtok, Object ref) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "size")) {
         initSize (rtok.scanInteger());
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, ref);      
   }      

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (VectorNdGrid grid, double tol) {
      if (myVecSize != grid.getVectorSize()) {
         return false;
      }
      return super.epsilonEquals (grid, tol);
   }
}
