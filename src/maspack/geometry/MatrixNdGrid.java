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
 * Implements a regular 3D grid that interpolates {@link MatrixNd} values using
 * trilinear interpolation. The class is implemented using {@code
 * VectorGrid<MatrixNd>} as a base class, with augmentation to specify the size
 * of the {@link MatrixNd} objects to be interpolated. This size must be
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
public class MatrixNdGrid extends VectorGrid<MatrixNd> {

   protected int myRowSize;
   protected int myColSize;

   /**
    * Returns the row size of the matrices interpolated by this grid.
    *
    * @return matrix row size
    */
   public int getMatrixRowSize() {
      return myRowSize;
   }

   /**
    * Returns the column size of the matrics interpolated by this grid.
    *
    * @return matrix column size
    */
   public int getMatrixColSize() {
      return myColSize;
   }

   protected void initType (Class<MatrixNd> type) {
      myTypeParameter = type;
   }

   private void initSize (int rowSize, int colSize) {
      if (rowSize <= 0 || colSize <= 0) {
         throw new IllegalArgumentException (
            "specified matrix size is "+rowSize+"x"+colSize+
            ", both must be > 0");
      }
      myRowSize = rowSize;
      myColSize = colSize;
   }

   /**
    * Default constructor. Should not be called by applications, unless
    * {@link #scan} is called immediately after.
    */   
   public MatrixNdGrid () {
      super (MatrixNd.class);
   }

   protected MatrixNdGrid (int rowSize, int colSize) {
      super (MatrixNd.class);
      initSize (rowSize, colSize);
   }

   /**
    * Creates a new grid with specified matrix size, widths, resolution, and
    * position and orientation of the center given by <code>TCL</code>.  The
    * grid values are initialized to zero.
    *
    * @param rowSize row size of the matrices
    * @param colSize column size of the matrices
    * @param widths widths along the x, y, and z axes
    * @param resolution cell resolution along the x, y, and z axes
    * @param TCL transformation giving the position and orientation
    * of the grid center
    */
   public MatrixNdGrid (
      int rowSize, int colSize,
      Vector3d widths, Vector3i resolution, RigidTransform3d TCL) {

      super (MatrixNd.class, widths, resolution, TCL);     
      initSize (rowSize, colSize);
   }

   /**
    * Creates a new grid, axis-aligned and centered on the origin, with the
    * specified resolution and x, y, z widths set to 1.  The grid values are
    * initialized to zero.
    *
    * @param rowSize row size of the matrices
    * @param colSize column size of the matrices
    * @param resolution cell resolution along the x, y, and z axes
    */
   public MatrixNdGrid (int rowSize, int colSize, Vector3i resolution) {
      super (MatrixNd.class, resolution);
      initSize (rowSize, colSize);
   }

   /**
    * Creates a new grid that is a copy of an existing grid.
    *
    * @param grid grid to copy
    */
   public MatrixNdGrid (MatrixNdGrid grid) {
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
   public void set (MatrixNdGrid grid) {
      myRowSize = grid.myRowSize;
      myColSize = grid.myColSize;
      super.set (grid);
   }

   protected MatrixNd createInstance() {
      return new MatrixNd (myRowSize, myColSize);
   }

   @Override
   protected String checkSize (MatrixNd value) {
      if (value.rowSize() != myRowSize || value.colSize() != myColSize) {
         return ("size "+value.getSize()+
                 " incompatible with grid size of "+myRowSize+"x"+myColSize);
      }
      else {
         return null;
      }
   }  

   protected void writeItems (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      
      pw.println ("size=[ "+myRowSize+" "+myColSize+" ]");
      super.writeItems (pw, fmt, ref);
   } 

   protected boolean scanItem (
      ReaderTokenizer rtok, Object ref) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "size")) {
         rtok.scanToken ('[');
         int rowSize = rtok.scanInteger();
         int colSize = rtok.scanInteger();
         rtok.scanToken (']');
         initSize (rowSize, colSize);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, ref);      
   }
    
   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (MatrixNdGrid grid, double tol) {
      if (myRowSize != grid.getMatrixRowSize()) {
         return false;
      }
      if (myColSize != grid.getMatrixColSize()) {
         return false;
      }
      return super.epsilonEquals (grid, tol);
   }

}
