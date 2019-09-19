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
import java.util.Arrays;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.util.ArraySupport;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Base class for a regular 3D grid that interpolates scalar values using
 * trilinear interpolation. Both DistanceGrid and ScalarGrid derive from this
 * class.
 */
public class ScalarGridBase extends InterpolatingGridBase {
   
   protected double[] myValues;  // values at each vertex

   protected static final double INF = Double.POSITIVE_INFINITY;
   
   /**
    * Special value indicating that a query point is outside the grid.
    */
   public static double OUTSIDE_GRID = Double.MAX_VALUE;

   protected void initVertexValues (int numv) {
      myValues = new double[numv];
   }

   /**
    * {@inheritDoc}
    */
   public Class<?> getParameterType() {
      return null;
   }

   /**
    * Sets this grid to be a copy of an existing grid.
    *
    * @param grid grid to copy
    */
   protected void set (ScalarGridBase grid) {
      super.set (grid);
      if (grid.myValues != null) {
         myValues = Arrays.copyOf (grid.myValues, grid.myValues.length);
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
    * @param values value for each vertex. Must have a length
    * {@code >=} {@link #numVertices}.
    */
   protected void setVertexValues (double[] values) {
      int numv = numVertices();
      if (values.length < numv) {
         throw new IllegalArgumentException (
            "values.length=" + values.length +
            "; must be >= num vertices ("+numv+")");
      }
      myValues = Arrays.copyOf (values, numv);
      myRobValid = false;
   }

   /**
    * Returns an array of the values at each vertex. See {@link
    * #setVertexValues} for a description of how vertices are indexed with
    * respect to this array.
    * 
    * @return array of vertex values.
    */
   protected double[] getVertexValues() {
      return myValues;
   }

   /**
    * Sets the value for the vertex indexed by {@code vi}. See {@link
    * #setVertexValues} for a description of how vertices are indexed.
    *
    * @param vi vertex index
    * @param value vertex value
    */
   protected void setVertexValue (int vi, double value) {
      int numv = numVertices();
      if (vi < 0 || vi >= numv) {
         throw new IndexOutOfBoundsException (
            "index is "+vi+", number of vertices is "+numv);
      }
      myValues[vi] = value;
      myRobValid = false;
   }

   /**
    * Queries the value for the vertex indexed by {@code vi}. See {@link
    * #setVertexValues} for a description of how vertices are indexed.
    * 
    * @param vi vertex index
    * @return value at the vertex
    */
   protected double getVertexValue (int vi) {
      int numv = numVertices();
      if (vi < 0 || vi >= numv) {
         throw new IndexOutOfBoundsException (
            "index is "+vi+", number of vertices is "+numv);
      }
      return myValues[vi];
   }

   /**
    * Queries the value at a specified vertex, as specified by x, y, z indices.
    * 
    * @param vxyz x, y, z vertex indices
    * @return value at the vertex
    */
   protected double getVertexValue (Vector3i vxyz) {
      return myValues[xyzIndicesToVertex(vxyz)];
   }
   
   /**
    * Queries the value at a specified vertex, as specified by x, y, z indices.
    * 
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return value at the vertex
    */   
   protected double getVertexValue (int xi, int yj, int zk) {
      return myValues[xyzIndicesToVertex(xi, yj, zk)];
   }

   /**
    * Zeros all the vertex values for this grid.
    */
   protected void zeroVertexValues () {
      if (myValues != null) {
         for (int i=0; i<myValues.length; i++) {
            myValues[i] = 0;
         }
         myRobValid = false;
      }
   }
   
   /** 
    * Calculates the value at an arbitrary point in world coordinates using
    * multilinear interpolation of the vertex values for the grid cell
    * containing the point.  If the point lies outside the grid volume, {@link
    * #OUTSIDE_GRID} is returned.
    *
    * @param point point at which to calculate the value
    * (world coordinates).
    * @return interpolated value, or <code>OUTSIDE_GRID</code>.
    */
   protected double getWorldValue (Point3d point) {
      Point3d lpnt = new Point3d();
      myLocalToWorld.inverseTransformPnt (lpnt, point);
      return getLocalValue(lpnt);
   }

   /** 
    * Calculates the value at an arbitrary point in local coordinates using
    * multilinear interpolation of the vertex values for the grid cell
    * containing the point.  If the point lies outside the grid volume, {@link
    * #OUTSIDE_GRID} is returned.
    *
    * @param point point at which to calculate the normal and value
    * (local coordinates).
    * @return interpolated value, or <code>OUTSIDE_GRID</code>.
    */
   protected double getLocalValue (Point3d point) {

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

      double result = 
         (w000*getVertexValue (vidx.x  , vidx.y  , vidx.z  ) +
          w001*getVertexValue (vidx.x  , vidx.y  , vidx.z+1) +
          w010*getVertexValue (vidx.x  , vidx.y+1, vidx.z  ) +
          w011*getVertexValue (vidx.x  , vidx.y+1, vidx.z+1) +
          w100*getVertexValue (vidx.x+1, vidx.y  , vidx.z  ) +
          w101*getVertexValue (vidx.x+1, vidx.y  , vidx.z+1) +
          w110*getVertexValue (vidx.x+1, vidx.y+1, vidx.z  ) +
          w111*getVertexValue (vidx.x+1, vidx.y+1, vidx.z+1));
      return result;
   }
   

   /**
    * Creates a triangular mesh approximating the surface on which the linearly
    * interpolated distance function equals 0.
    *
    * @return iso surface for linear interpolation
    */
   public PolygonalMesh createDistanceSurface() {
      return createDistanceSurface(/*iso=*/0);
   }
    
   /**
    * Creates a triangular mesh approximating the surface on which the linearly
    * interpolated distance function equals <code>val</code>.
    *
    * @param val iso surface value
    * @return iso surface for linear interpolation
    */
   public PolygonalMesh createDistanceSurface(double val) {
      MarchingTetrahedra marcher = new MarchingTetrahedra();

      PolygonalMesh mesh = marcher.createMesh (
         myValues, Vector3d.ZERO, new Vector3d(1,1,1), getResolution(), val);
      mesh.transform (myGridToLocal);
      return mesh;
   }

   /**
    * Creates a triangular mesh approximating the surface on which the linearly
    * interpolated distance function equals <code>val</code>.
    *
    * @param val iso surface value
    * @return iso surface for linear interpolation
    */
   public PolygonalMesh createDistanceSurface (double val, int res) {
      MarchingTetrahedra marcher = new MarchingTetrahedra();

      Vector3i cellRes = new Vector3i(getResolution());
      cellRes.scale (res);

      int nvx = cellRes.x+1;
      int nvy = cellRes.y+1;
      int nvz = cellRes.z+1;
      double[] dists = new double[nvx*nvy*nvz];
      double invRes = 1.0/res;

      for (int i=0; i<myNx-1; i++) {
         for (int j=0; j<myNy-1; j++) {
            for (int k=0; k<myNz-1; k++) {
               double d000  = getVertexValue (i  , j  , k  );
               double d001  = getVertexValue (i  , j  , k+1);
               double d010  = getVertexValue (i  , j+1, k  );
               double d011  = getVertexValue (i  , j+1, k+1);
               double d100  = getVertexValue (i+1, j  , k  );
               double d101  = getVertexValue (i+1, j  , k+1);
               double d110  = getVertexValue (i+1, j+1, k  );
               double d111  = getVertexValue (i+1, j+1, k+1);

               int maxci = (i < myNx-2 ? res-1 : res);
               int maxcj = (j < myNy-2 ? res-1 : res);
               int maxck = (k < myNz-2 ? res-1 : res);
               double cx, cy, cz;
               for (int ci=0; ci<=maxci; ci++) {
                  cx = ci*invRes;
                  for (int cj=0; cj<=maxcj; cj++) {
                     cy = cj*invRes;
                     for (int ck=0; ck<=maxck; ck++) {
                        cz = ck*invRes;
                        double w001z = (1-cx)*(1-cy);
                        double w011z = (1-cx)*cy;
                        double w101z = cx*(1-cy);
                        double w111z = cx*cy;
                        
                        double w000  = w001z*(1-cz);
                        double w001  = w001z*cz;
                        double w010  = w011z*(1-cz);
                        double w011  = w011z*cz;
                        double w100  = w101z*(1-cz);
                        double w101  = w101z*cz;
                        double w110  = w111z*(1-cz);
                        double w111  = w111z*cz;                         
                        
                        dists[(i*res+ci)+(j*res+cj)*nvx+(k*res+ck)*nvx*nvy] =
                           w000*d000 + w001*d001 + w010*d010 + w011*d011 +
                           w100*d100 + w101*d101 + w110*d110 + w111*d111;
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

   protected boolean scanItem (
      ReaderTokenizer rtok, Object ref) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         int numv = numVertices();
         ArrayList<Double> values = new ArrayList<Double>();
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            values.add (rtok.scanNumber());
         }
         if (values.size() != numv) {
            throw new IOException (
               "input has "+values.size()+" values, expected "+numv);
         }
         myValues = ArraySupport.toDoubleArray(values);
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
      for (int i=0; i<numv; i++) {
         pw.println (fmt.format(myValues[i]));
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
   public boolean epsilonEquals (ScalarGridBase grid, double tol) {
      if (!super.epsilonEquals (grid, tol)) {
         return false;
      }
      int numv = numVertices();
      for (int i=0; i<numv; i++) {
         if (Math.abs(myValues[i]-grid.myValues[i]) > tol) {
            return false;
         }
      }
      return true;
   }
}

