/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;

import maspack.geometry.io.WavefrontReader;
import maspack.matrix.Point3d;
import maspack.matrix.Vector4d;
import maspack.render.PointLineRenderProps;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.util.NumberFormat;

/**
 * Implements a NURBS surface
 */
public class NURBSSurface extends NURBSObject {
   NURBSCurve3d ucurve;
   NURBSCurve3d vcurve;
   int numCtrlPntsU;
   int numCtrlPntsV;

   private Point3d[] renderVertices;

   private int defaultResolution = 30;

   private int urenderSize = defaultResolution + 1;
   private int vrenderSize = defaultResolution + 1;

   /**
    * Creates an empty NURBS surface.
    */
   public NURBSSurface() {
      super();
      vcurve = new NURBSCurve3d();
      ucurve = new NURBSCurve3d();
   }

   /**
    * Creates a NURBS surface using degree and knot values in the u and v
    * directions, along with control points. The surface may be open or closed
    * in either the u or v directions. For information on how these arguments
    * are constrained, see the general version of
    * {@link #set(int,int,double[],int,int,double[],Vector4d[]) set}.
    * 
    * @param du
    * degree in the u direction
    * @param typeu
    * u direction type, which must be either {@link NURBSCurve3d#OPEN OPEN} or
    * {@link NURBSCurve3d#CLOSED CLOSED}.
    * @param knotsu
    * knot values for the u direction
    * @param dv
    * degree in the v direction
    * @param typev
    * v direction type, which must be either {@link NURBSCurve3d#OPEN OPEN} or
    * {@link NURBSCurve3d#CLOSED CLOSED}.
    * @param knotsv
    * knot values for the v direction
    * @param ctrlPnts
    * control points
    * @throws IllegalArgumentException
    * if constraints on the arguments are violated
    * @see #set(int,int,double[],int,int,double[],Vector4d[])
    */
   public NURBSSurface (int du, int typeu, double[] knotsu, int dv, int typev,
   double[] knotsv, Vector4d[] ctrlPnts) {
      super();
      set (du, typeu, knotsu, dv, typev, knotsv, ctrlPnts);
   }

   /**
    * Sets the resolution used for rendering this surface along the u parameter.
    * This is the number of linear segments used to plot the surface between
    * it's minimum and maximum u values.
    * 
    * @param res
    * rendering resolution for u
    * @see #getResolutionU
    */
   public void setResolutionU (int res) {
      urenderSize = res + 1;
   }

   /**
    * Returns the resolution used for rendering this surface along the u
    * parameter.
    * 
    * @return rendering resolution for u
    * @see #setResolutionU
    */
   public int getResolutionU() {
      return urenderSize - 1;
   }

   /**
    * Sets the resolution used for rendering this surface along the v parameter.
    * This is the number of linear segments used to plot the surface between
    * it's minimum and maximum v values.
    * 
    * @param res
    * rendering resolution for v
    * @see #getResolutionV
    */
   public void setResolutionV (int res) {
      vrenderSize = res + 1;
   }

   /**
    * Returns the resolution used for rendering this surface along the v
    * parameter.
    * 
    * @return rendering resolution for v
    * @see #setResolutionV
    */
   public int getResolutionV() {
      return vrenderSize - 1;
   }

   /**
    * Sets the range of the u parameter. If necessary, ustart and uend are
    * clipped so that they lie in the range knots[d-1] to knots[numk-d], where
    * d, knots, and numk describe the degree, knots, and number of knots
    * associated with u.
    * 
    * @param ustart
    * minimum u parameter
    * @param uend
    * maximum u parameter
    * @see #getRangeU
    */
   public void setRangeU (double ustart, double uend) {
      ucurve.setRange (ustart, uend);
   }

   /**
    * Gets the range of the u parameter.
    * 
    * @param uStartEnd
    * returns the ustart and uend values, in elements 0 and 1, respectively.
    * @see #setRangeU
    */
   public void getRangeU (double[] uStartEnd) {
      ucurve.getRange (uStartEnd);
   }

   /**
    * Sets the range of the v parameter. If necessary, vstart and vend are
    * clipped so that they lie in the range knots[d-1] to knots[numk-d], where
    * d, knots, and numk describe the degree, knots, and number of knots
    * associated with v.
    * 
    * @param vstart
    * minimum v parameter
    * @param vend
    * maximum v parameter
    * @see #getRangeV
    */
   public void setRangeV (double vstart, double vend) {
      vcurve.setRange (vstart, vend);
   }

   /**
    * Gets the range of the v parameter.
    * 
    * @param vStartEnd
    * returns the vstart and vend values, in elements 0 and 1, respectively.
    * @see #setRangeV
    */
   public void getRangeV (double[] vStartEnd) {
      vcurve.getRange (vStartEnd);
   }

   private void allocateRenderVertices() {
      renderVertices = new Point3d[urenderSize * vrenderSize];
      for (int i = 0; i < renderVertices.length; i++) {
         renderVertices[i] = new Point3d();
      }
   }

   /**
    * Sets this NURBS surface using degree and knot values in the u and v
    * directions, along with control points. For information on how these
    * arguments are constrained, see the more general version of
    * {@link #set(int,int,double[],int,int,double[],Vector4d[]) set}.
    * 
    * @param du
    * degree in the u direction
    * @param knotsu
    * knot values for the u direction
    * @param dv
    * degree in the v direction
    * @param knotsv
    * knot values for the v direction
    * @param ctrlPnts
    * control points
    * @throws IllegalArgumentException
    * if constraints on the arguments are violated
    * @see #set(int,int,double[],int,int,double[],Vector4d[])
    */
   public void set (
      int du, double[] knotsu, int dv, double[] knotsv, Vector4d[] ctrlPnts) {
      set (du, NURBSCurve3d.OPEN, knotsu, dv, NURBSCurve3d.OPEN, knotsv, ctrlPnts);
   }

   /**
    * Sets this NURBS surface using degree and knot values in the u and v
    * directions, along with control points. The surface may be open or closed
    * in either the u or v directions.
    * 
    * <p>
    * Let du, numku, and numcu be the degree, number of knots, and number of
    * control points associated with the u direction. The degree must be 1 or
    * greater. If the u direction is open, then <blockquote> numcu = numku - du +
    * 1 </blockquote> and if it is closed, <blockquote> numcu = numku - 2*du + 1
    * </blockquote> Analogous results hold for the v direction. The total number
    * of control points is numcu*numcv, and so the ctrlPnts argument must be at
    * least this length. Control points should be arranged so that the first set
    * of v control points comes first, followed by the second set, etc.
    * 
    * <p>
    * This method automatically sets ustart, uend, vstart, and vend (see
    * {@link #setRangeU setRangeU} and {@link #setRangeV setRangeV}) to to
    * knotsu[du-1], knotsu[numku-du], knotsv[dv-1], and knotsv[numkv-dv].
    * 
    * <p>
    * The control points are specified as 4-vectors, where their spatial
    * location is given by x, y, and z and their weight is given by w. The
    * points should not be in homogeneous form; i.e., x, y, and z should not be
    * premultipled by w.
    * 
    * @param du
    * degree in the u direction
    * @param typeu
    * u direction type, which must be either {@link NURBSCurve3d#OPEN OPEN} or
    * {@link NURBSCurve3d#CLOSED CLOSED}.
    * @param knotsu
    * knot values for the u direction
    * @param dv
    * degree in the v direction
    * @param typev
    * v direction type, which must be either {@link NURBSCurve3d#OPEN OPEN} or
    * {@link NURBSCurve3d#CLOSED CLOSED}.
    * @param knotsv
    * knot values for the v direction
    * @param ctrlPnts
    * control points
    * @throws IllegalArgumentException
    * if constraints on the arguments are violated
    */
   public void set (
      int du, int typeu, double[] knotsu, int dv, int typev, double[] knotsv,
      Vector4d[] ctrlPnts) {
      // check consistency

      ucurve.checkKnotConsistency ("u: ", du, knotsu);
      vcurve.checkKnotConsistency ("v: ", dv, knotsv);

      if (typeu == NURBSCurve3d.OPEN) {
         numCtrlPntsU = (knotsu.length - du + 1);
      }
      else {
         numCtrlPntsU = (knotsu.length - 2 * du + 1);
      }
      if (typev == NURBSCurve3d.OPEN) {
         numCtrlPntsV = (knotsv.length - dv + 1);
      }
      else {
         numCtrlPntsV = (knotsv.length - 2 * dv + 1);
      }
      int numCtrlPnts = numCtrlPntsU * numCtrlPntsV;
      if (ctrlPnts.length < numCtrlPnts) {
         throw new IllegalArgumentException ("insufficient control points");
      }

      ucurve.setKnots (du, typeu, knotsu);
      vcurve.setKnots (dv, typev, knotsv);

      allocateRenderVertices();

      // this.ctrlPnts = ctrlPnts;
      setControlPoints (ctrlPnts, numCtrlPnts);
   }

   /**
    * Sets this surface to a 40-point sphere formed by sweeping a 5-point
    * rational quadratic semi-circle about the z axis.
    * 
    * @param x
    * center x coordinate
    * @param y
    * center y coordinate
    * @param z
    * center z coordinate
    * @param radius
    * circle radius
    * @throws IllegalArgumentException
    * if radius is non-positive
    */
   public void setSphere (double x, double y, double z, double radius) {
      if (radius <= 0) {
         throw new IllegalArgumentException ("radius must be positive");
      }
      Vector4d[] cpnts = new Vector4d[8 * 5];

      double[] xbase = new double[] { 1, 1, 0, -1, -1, -1, 0, 1 };
      double[] ybase = new double[] { 0, 1, 1, 1, 0, -1, -1, -1 };

      for (int i = 0; i < 8; i++) {
         double cx = xbase[i] * radius + x;
         double cy = ybase[i] * radius + y;

         cpnts[i * 5 + 0] = new Vector4d (x, y, z + radius, 1);
         cpnts[i * 5 + 1] = new Vector4d (cx, cy, z + radius, 1);
         cpnts[i * 5 + 2] = new Vector4d (cx, cy, z, 1);
         cpnts[i * 5 + 3] = new Vector4d (cx, cy, z - radius, 1);
         cpnts[i * 5 + 4] = new Vector4d (x, y, z - radius, 1);

         cpnts[i * 5 + 1].w = (1 / Math.sqrt (2));
         cpnts[i * 5 + 3].w = (1 / Math.sqrt (2));

         if ((i % 2) != 0) {
            for (int j = 0; j < 5; j++) {
               cpnts[i * 5 + j].w *= Math.sqrt (2) / 4;
            }
         }
      }

      set (
         3, NURBSCurve3d.CLOSED, new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                                             11, 12 }, 2, NURBSCurve3d.OPEN,
         new double[] { 0, 0, 1, 1, 2, 2 }, cpnts);
   }

   /**
    * Evaluates the point on this surface corresponding to u and v. These values
    * are clipped, if necessary, to the range [ustart, uend] and [vstart, vend].
    * 
    * @param pnt
    * returns the surface point value
    * @param u
    * u parameter value
    * @param v
    * v parameter value
    */
   public void eval (Point3d pnt, double u, double v) {
      double[] uBasisVals = new double[ucurve.myDegree + 1];
      double[] vBasisVals = new double[vcurve.myDegree + 1];

      int k = ucurve.getKnotIndex (u);
      ucurve.basisValues (uBasisVals, k, u);

      int l = vcurve.getKnotIndex (v);
      vcurve.basisValues (vBasisVals, l, v);

      int dv = vcurve.getDegree();
      int du = ucurve.getDegree();
      // 
      // the i, j control point is located at ctrlPnts[i*numCtrlPntsV+j]
      // 
      pnt.setZero();
      double w = 0;
      for (int i = 0; i <= du; i++) {
         int d_i = ucurve.getCtrlIndex (k, i, numCtrlPntsU);
         double B_i = uBasisVals[i];
         for (int j = 0; j <= dv; j++) {
            int d_j = vcurve.getCtrlIndex (l, j, numCtrlPntsV);
            double B_j = vBasisVals[j];
            Vector4d cpnt = myCtrlPnts.get(d_i * numCtrlPntsV + d_j);
            double wbb = cpnt.w * B_i * B_j;
            pnt.x += wbb * cpnt.x;
            pnt.y += wbb * cpnt.y;
            pnt.z += wbb * cpnt.z;
            w += wbb;
         }
      }
      pnt.scale (1 / w);
   }

   private void evalRenderVertices() {
      double[] minmax = new double[2];

      ucurve.getRange (minmax);
      double ubegin = minmax[0];
      double uend = minmax[1];

      vcurve.getRange (minmax);
      double vbegin = minmax[0];
      double vend = minmax[1];

      for (int i = 0; i < urenderSize; i++) {
         double u = ubegin + (uend - ubegin) * i / (urenderSize - 1.0);
         for (int j = 0; j < vrenderSize; j++) {
            double v = vbegin + (vend - vbegin) * j / (vrenderSize - 1.0);
            eval (renderVertices[i * vrenderSize + j], u, v);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void render (Renderer renderer, RenderProps props, int flags) {
      
      boolean selecting = renderer.isSelecting();
      
      if (numControlPoints() == 0) {
         return;
      }

      evalRenderVertices();

      renderer.setShading (Shading.NONE);

      // draw the control points
      if (myDrawControlShapeP) {
         drawControlPoints (renderer, props, flags);
      }         

      // draw the surface as quads
      if (!selecting) {
         renderer.setColor (contourColor);
      }
      renderer.beginDraw (DrawMode.LINES);
      for (int i = 0; i < urenderSize - 1; i++) {
         for (int j = 0; j < vrenderSize - 1; j++) {
            Point3d p0 = renderVertices[i * vrenderSize + j];
            Point3d p1 = renderVertices[(i + 1) * vrenderSize + j];
            Point3d p2 = renderVertices[(i + 1) * vrenderSize + (j + 1)];
            Point3d p3 = renderVertices[i * vrenderSize + (j + 1)];
            renderer.addVertex (p0);
            renderer.addVertex (p1);
            renderer.addVertex (p1);
            renderer.addVertex (p2);
            renderer.addVertex (p2);
            renderer.addVertex (p3);
            renderer.addVertex (p3);
            renderer.addVertex (p0);
         }
      }
      renderer.endDraw();

      // draw the control polygon
      if (myDrawControlShapeP) {
         if (!selecting) {
            renderer.setColor (edgeColor);
         }
         for (int i = 0; i < numCtrlPntsU; i++) {
            if (vcurve.isClosed()) {
               renderer.beginDraw (DrawMode.LINE_LOOP);
            }
            else {
               renderer.beginDraw (DrawMode.LINE_STRIP);
            }
            for (int j = 0; j < numCtrlPntsV; j++) { // pnt.setFromHomogeneous
               // (ctrlPnts[i*numCtrlPntsV+j]);
               Vector4d cpnt = myCtrlPnts.get(i * numCtrlPntsV + j);
               renderer.addVertex (cpnt.x, cpnt.y, cpnt.z);
            }
            renderer.endDraw();
         }
         for (int j = 0; j < numCtrlPntsV; j++) {
            if (ucurve.isClosed()) {
               renderer.beginDraw (DrawMode.LINE_LOOP);
            }
            else {
               renderer.beginDraw (DrawMode.LINE_STRIP);
            }
            for (int i = 0; i < numCtrlPntsU; i++) {
               Vector4d cpnt = myCtrlPnts.get(i * numCtrlPntsV + j);
               renderer.addVertex (cpnt.x, cpnt.y, cpnt.z);
            }
            renderer.endDraw();
         }
      }

      renderer.setShading (Shading.FLAT);
   }

   /**
    * Returns the NURBS curve controlling this surface in the u direction.
    * 
    * @return u direction curve
    */
   NURBSCurve3d getUCurve() {
      return ucurve;
   }

   /**
    * Returns the NURBS curve controlling this surface in the v direction.
    * 
    * @return v direction curve
    */
   public NURBSCurve3d getVCurve() {
      return vcurve;
   }

   /**
    * Returns the degree of this surface in the u direction.
    * 
    * @return degree in the u direction
    */
   public int getDegreeU() {
      return ucurve.myDegree;
   }

   /**
    * Returns the number of knots in the u direction.
    * 
    * @return number of knots in the u direction
    */
   public int numKnotsU() {
      return ucurve.myNumKnots;
   }

   /**
    * Returns the knots in the u direction.
    * 
    * @return knot values in the u direction
    */
   public double[] getKnotsU() {
      return ucurve.myKnots;
   }

   /**
    * Returns the degree of this surface in the v direction.
    * 
    * @return degree in the v direction
    */
   public int getDegreeV() {
      return vcurve.myDegree;
   }

   /**
    * Returns the number of knots in the v direction.
    * 
    * @return number of knots in the v direction
    */
   public int numKnotsV() {
      return vcurve.myNumKnots;
   }

   /**
    * Returns the knots in the v direction.
    * 
    * @return knot values in the v direction
    */
   public double[] getKnotsV() {
      return vcurve.myKnots;
   }

   /**
    * Reads this surface from a text description supplied by a reader. The
    * allowed format is a subset of the Alias Wavefront OBJ format for surfaces,
    * and consists of a set of statements, one per line (lines can be continued,
    * if necessary, using the line continuation character <code>\</code>).
    * 
    * <p>
    * The allowed statements are:
    * <ul>
    * <li>Vertex definition of the form <blockquote> v <i>x</i> <i>y</i> <i>z</i> [<i>w</i>]
    * </blockquote> each giving the x, y, and z values for a control point (with
    * an optional weighting value w which defaults to 1 if omitted).
    * <li>Degree statement of the form <blockquote> deg <i>du</i> <i>dv</i>
    * </blockquote> where du and dv are degree of the surface in the u and v
    * directions. A degree statement must precede a surface statement (described
    * below).
    * 
    * <li>Surface statement of the form <blockquote> surf <i>ustart</i>
    * <i>uend</i> <i>vstart</i> <i>vend</i> <i>i0</i> <i>i1</i> ...
    * </blockquote> where ustart, uend, vstart, and vend give the minimum and
    * maximum values for the u and v surface parameters, and i0, i1, etc. give
    * the indices of the control points defined by the vertex statements.
    * Control points are indexed, starting at 1, by their order of occurance. If
    * the index value is negative, then it gives the relative location of a
    * control point with respect to the surface statement, where -1 is the
    * closest preceding, -2 is the next closest preceding, etc. There should be
    * numcu*numcv control points in all, where numcu and numcv are described in
    * {@link #set(int,int,double[],int,int,double[],Vector4d[]) set}, and the
    * points should be ordered so that the first set of v points comes first,
    * followed by the second set, etc.
    * 
    * <li>A u parameter statement of the form <blockquote> parm u [closed]
    * <i>k0</i> <i>k1</i> ... </blockquote> where <code>closed</code> is an
    * optional keyword indicating that the surface is closed in u direction, and
    * k0, k1, etc. are the u direction knot values. The u parameter statement
    * must follow the surface statement and precede the end statement (described
    * below).
    * 
    * <li>A v parameter statement of the form <blockquote> parm v [closed]
    * <i>k0</i> <i>k1</i> ... </blockquote> which is analogous to the u
    * parameter statement.
    * 
    * <li>An end statement of the form <blockquote> end </blockquote> This must
    * be placed after the curve and parameter statements.
    * </ul>
    * The total number of control points and knots must match as described in
    * the documentation for
    * {@link #set(int,int,double[],int,int,double[],Vector4d[]) set}. Explicit
    * knot values may be omitted for either u or v, in which case an appropriate
    * number of uniformly-spaced knots is created with start and end values
    * corresponding to 0 and 1.
    * 
    * <p>
    * As an example, here are the statements that define a simple bezier surface
    * patch: 
    * <pre>
    * v -10.0 0.0 10.0 1.0
    * v -5.0 5.0 10.0 1.0
    * v 5.0 5.0 10.0 1.0
    * v 10.0 0.0 10.0 1.0
    * v -10.0 0.0 5.0 1.0
    * v -5.0 5.0 5.0 1.0
    * v 5.0 5.0 5.0 1.0
    * v 10.0 0.0 5.0 1.0
    * v -10.0 0.0 -5.0 1.0
    * v -5.0 5.0 -5.0 1.0
    * v 5.0 5.0 -5.0 1.0
    * v 10.0 0.0 -5.0 1.0
    * v -10.0 0.0 -10.0 1.0
    * v -5.0 5.0 -10.0 1.0
    * v 5.0 5.0 -10.0 1.0
    * v 10.0 0.0 -10.0 1.0
    * deg 3 3
    * surf 0.0 1.0 0.0 1.0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16
    * parm u 0.0 0.0 0.0 1.0 1.0 1.0
    * parm v 0.0 0.0 0.0 1.0 1.0 1.0
    * end
    * </pre>
    * 
    * @param reader
    * providing the text input
    * @throws IOException
    * if an I/O or format error occurs
    */
   public void read (Reader reader) throws IOException {
      WavefrontReader wfr = new WavefrontReader(reader);
      wfr.parse ();

      ArrayList<WavefrontReader.Surface> surfList = wfr.getSurfaceList();
      WavefrontReader.Surface surface = surfList.get (surfList.size() - 1);

      if (surface == null) {
         throw new IOException ("no surface specified in input");
      }
      try {
         set (surface, wfr.getHomogeneousPoints());
      }
      catch (IllegalArgumentException e) {
         throw new IOException (e.getMessage());
      }
   }

   private double[] createUniformKnots (
      int dv, int typev, double[] knotsv, int du, int typeu, int numCtrlPnts) {
      int numCtrlPntsV;
      if (typev == NURBSCurve3d.OPEN) {
         numCtrlPntsV = (knotsv.length - dv + 1);
      }
      else {
         numCtrlPntsV = (knotsv.length - 2 * dv + 1);
      }
      int numCtrlPntsU = numCtrlPnts / numCtrlPntsV;
      if (numCtrlPntsU < 1) {
         throw new IllegalArgumentException ("insufficient control points");
      }
      int numIntervals =
         (typeu == NURBSCurve3d.CLOSED ? numCtrlPntsU : numCtrlPntsU - du);
      if (numIntervals < 1) {
         throw new IllegalArgumentException ("not enough control points");
      }
      return NURBSCurve3d.createUniformKnots (
         numIntervals, du, typeu != NURBSCurve3d.CLOSED);
   }

   public void set (WavefrontReader.Surface surface, Vector4d[] allPnts) {
      // build up the list of control points
      Vector4d[] ctrlPnts = new Vector4d[allPnts.length];
      for (int i = 0; i < ctrlPnts.length; i++) {
         int idx = surface.indices[i];
         if (idx >= allPnts.length) {
            throw new IllegalArgumentException (
               "index " + (idx + 1) + " out of range, line " + surface.lineNum);
         }
         ctrlPnts[i] = allPnts[idx];
      }
      int du = surface.udegree;
      if (du < 1) {
         throw new IllegalArgumentException ("u: degree is less than one");
      }
      int typeu = surface.uIsClosed ? NURBSCurve3d.CLOSED : NURBSCurve3d.OPEN;
      double[] knotsu = surface.uknots;
      if (knotsu.length == 0) {
         knotsu = null;
      }

      int dv = surface.vdegree;
      if (dv < 1) {
         throw new IllegalArgumentException ("v: degree is less than one");
      }
      int typev = surface.vIsClosed ? NURBSCurve3d.CLOSED : NURBSCurve3d.OPEN;
      double[] knotsv = surface.vknots;
      if (knotsv.length == 0) {
         knotsv = null;
      }

      if (knotsu == null && knotsv == null) {
         throw new IllegalArgumentException (
            "knots must be explicitly specified for at least one of u or v");
      }
      if (knotsu == null) {
         vcurve.checkKnotConsistency ("v: ", dv, knotsv);
         knotsu =
            createUniformKnots (dv, typev, knotsv, du, typeu, ctrlPnts.length);
      }
      else if (knotsv == null) {
         ucurve.checkKnotConsistency ("u: ", du, knotsu);
         knotsv =
            createUniformKnots (du, typeu, knotsu, dv, typev, ctrlPnts.length);
      }
      set (du, typeu, knotsu, dv, typev, knotsv, ctrlPnts);
      setRangeU (surface.u0, surface.u1);
      setRangeV (surface.v0, surface.v1);
   }

   int lineLen;

   private void resetLine (PrintWriter pw) {
      if (lineLen > 0) {
         pw.println ("");
         lineLen = 0;
      }
   }

   private void addToLine (PrintWriter pw, String s) {
      int l = s.length();
      if (lineLen + l >= 78) {
         pw.println (" \\");
         lineLen = 0;
      }
      pw.print (s);
      lineLen += l;
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, String fmtStr, boolean relative)
      throws IOException {
      double[] minmax = new double[2];
      int numc = numControlPoints();
      NumberFormat fmt = new NumberFormat (fmtStr);
      for (int i = 0; i < myCtrlPnts.size(); i++) {
         pw.println ("v " + myCtrlPnts.get(i).toString (fmt));
      }
      pw.println ("deg " + ucurve.getDegree() + " " + vcurve.getDegree());
      resetLine (pw);
      addToLine (pw, "surf");
      ucurve.getRange (minmax);
      addToLine (pw, " " + fmt.format (minmax[0]) + " " +
                 fmt.format (minmax[1]));
      vcurve.getRange (minmax);
      addToLine (pw, " " + fmt.format (minmax[0]) + " " +
                 fmt.format (minmax[1]));
      for (int i = 0; i < numc; i++) {
         addToLine (pw, relative ? " " + (-numc + i) : " " + (i + 1));
      }
      resetLine (pw);
      addToLine (pw, "parm u");
      if (ucurve.isClosed()) {
         addToLine (pw, " closed");
      }
      double[] knotsu = ucurve.getKnots();
      for (int i = 0; i < knotsu.length; i++) {
         addToLine (pw, " " + fmt.format (knotsu[i]));
      }
      resetLine (pw);
      addToLine (pw, "parm v");
      if (vcurve.isClosed()) {
         addToLine (pw, " closed");
      }
      double[] knotsv = vcurve.getKnots();
      for (int i = 0; i < knotsv.length; i++) {
         addToLine (pw, " " + fmt.format (knotsv[i]));
      }
      resetLine (pw);
      pw.println ("end");
      pw.flush();
   }

   /**
    * {@inheritDoc}
    */
   public RenderProps createRenderProps() {
      return new PointLineRenderProps();
   }

}
