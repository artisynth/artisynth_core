/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;

import maspack.geometry.io.WavefrontReader;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.Vector4d;
import maspack.render.PointEdgeRenderProps;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Base class for 2 and 3 dimensional NURBS curves
 */
public abstract class NURBSCurveBase extends NURBSObject {

   protected int myDegree;
   protected double[] myKnots = new double[10];
   protected int myNumKnots;
   // double[] basisVals;
   protected boolean myClosedP = false;
   protected double myUstart;
   protected double myUend;

   protected int myResolution = 5;

   /**
    * Specifies an open curve.
    */
   static public final int OPEN = 0;

   /**
    * Specifies a closed curve.
    */
   static public final int CLOSED = 1;

   /**
    * Returns true if this curve is closed.
    * 
    * @return true if the curve is closed
    */
   public boolean isClosed() {
      return myClosedP;
   }

   public int getType() {
      if (myClosedP) {
         return CLOSED;
      }
      else {
         return OPEN;
      }
   }

   public NURBSCurveBase() {
      super();
   }

   /**
    * Sets the resolution used for rendering this curve. This is the
    * approximate number of pixels in each of the lines segments used to
    * approximate the curve.
    * 
    * @param res
    * rendering resolution
    * @see #getResolution
    */
   public void setResolution (int res) {
      myResolution = res;
   }

   /**
    * Evaluates the point on this curve for parameter u. If necessary, u is
    * clipped to the range [ustart, uend].
    * 
    * @param pnt
    * returns the curve point value
    * @param u
    * curve parameter value
    */
   public abstract void eval (Point3d pnt, double u);

   /**
    * Returns the resolution used for rendering this curve.
    * 
    * @return rendering resolution
    * @see #setResolution
    */
   public int getResolution() {
      return myResolution;
   }

   /**
    * Sets the range of the curve parameter u. If necessary, ustart and uend are
    * clipped so that they lie in the range knots[d-1] to knots[numk-d], where
    * d, knots, and numk describe the curve's degree, knots, and number of
    * knots.
    * 
    * @param ustart
    * minimum curve parameter
    * @param uend
    * maximum curve parameter
    * @see #getRange
    */
   public void setRange (double ustart, double uend) {
      if (myKnots != null) {
         this.myUstart = Math.max (ustart, myKnots[myDegree - 1]);
         this.myUend = Math.min (uend, myKnots[myNumKnots - myDegree]);
      }
      else {
         this.myUstart = ustart;
         this.myUend = uend;
      }
   }

   /**
    * Gets the range of the curve parameter u.
    * 
    * @param uStartEnd
    * returns the ustart and uend values, in elements 0 and 1, respectively.
    * @see #setRange
    */
   public void getRange (double[] uStartEnd) {
      uStartEnd[0] = myUstart;
      uStartEnd[1] = myUend;
   }

   void checkKnotConsistency (String msgPrefix, int degree, double[] knots) {
      if (degree < 1) {
         throw new IllegalArgumentException (
            msgPrefix + "degree is less than one");
      }
      for (int i = 0; i < knots.length - 1; i++) {
         if (knots[i] > knots[i + 1]) {
            throw new IllegalArgumentException (
               msgPrefix + "knots are not monotonically increasing");
         }
      }
      if (knots.length < 2 * (degree)) {
         throw new IllegalArgumentException (
            msgPrefix + "insufficient knots");
      }
   }

   /**
    * Creates an array of evenly spaced knots. For a curve of degree d, the
    * number of knots numk will be numi+2*d-1, where numi is the number of
    * active knot intervals.  Knot values will increase in unit increments from
    * 0 to numi over the active knot interval from knots[d-1] to
    * knots[d-1+numi]. The values of the remaining knots depend on whether the
    * curve is open or closed. For open curves, the remaining knot values are
    * extrapolated so that knots[i+1] - knots[i] = 1. Otherwise, for closed
    * curves, the d knots at the beginning and end are clamped to 0 and numi,
    * respectively.
    * 
    * @param numi
    * number of active intervals
    * @param d
    * degree of the curve
    * @param open
    * true if the curve is open and false if it is closed
    * @return an array of knot values
    * @throws IllegalArgumentException
    * if constraints on the arguments are violated
    */
   public static double[] createUniformKnots (int numi, int d, boolean open) {
      if (d < 1) {
         throw new IllegalArgumentException ("degree is less than one");
      }
      if (numi < 1) {
         throw new IllegalArgumentException ("numi is less than one");
      }
      double[] knots = new double[numi+2*d-1];
      for (int i=0; i<knots.length; i++) {
         if (open && i < d-1) {
            knots[i] = 0;
         }
         else if (open && i > d-1+numi) {
            knots[i] = numi;
         }
         else {
            knots[i] = (i-d+1);
         }
      }
      return knots;
   }

   /**
    * Sets this NURBS curve to an open curve with a specified degree, knot
    * points, and control points. For information on how these arguments are
    * constrained, see the more general version of
    * {@link #set(int,int,Vector4d[],double[]) set}.
    * 
    * @param d
    * degree of the curve
    * @param ctrlPnts
    * control points
    * @param knots
    * knot values    * @throws IllegalArgumentException
    * if constraints on the arguments are violated
    * @see #set(int,int,Vector4d[],double[])
    */
   public void set (int d, Vector4d[] ctrlPnts, double[] knots) {
      set (d, OPEN, ctrlPnts, knots);
   }

   /**
    * Sets this NURBS curve to a curve of third degree with uniform
    * unity knot spacing and a specified number of control points.
    *
    * @param type should be {@link #OPEN} for open curves and
    * {@link #CLOSED} for closed curves.
    * @param ctrlPnts control points for this curve
    */
   public void setUniformCubic (int type, Vector4d[] ctrlPnts) {
      set (3, type, ctrlPnts, null);
   }

   /**
    * Returns true if this curve represents a B-spline (i.e.,
    * if all control points have unity weights).
    */
   public boolean isBSpline() {
      for (int i=0; i<numControlPoints(); i++) {
         if (getControlPoint(i).w != 1) {
            return false;
         }
      }
      return true;
   }

   /**
    * Returns true if this curve represents a piecewise bezier
    * curve.
    */
   public boolean isBezier() {
      int numc = numControlPoints();
      int d = myDegree;
      int kend;
      // make sure number of points consistent with Bezier curve
      if (myClosedP) {
         if ((numc % d) != 0) {
            return false;
         }
         kend = myNumKnots-d+1;
      }
      else {
         if (((numc-d-1) % d) != 0) {
            return false;
         }
         kend = myNumKnots;
      }
      for (int k=0; k<kend; ) {
         double uval = myKnots[k++];
         for (int i=0; i<d-1; i++) {
            if (myKnots[k++] != uval) {
               return false;
            }
         }
      }
      return true;
   }

   public void convertToBezier () {
      if (!isBSpline()) {
         throw new UnsupportedOperationException (
            "Bezier conversion currentlyonly implemented for B-splines");
      }
      int d = myDegree;
      int k; // current knot
      int endskip; // number of knots to skip at the end
      if (myClosedP) {
         k = 0;
         endskip = d;
      }
      else {
         k = d;
         endskip = d-1;
      }
      int multiplicity = 1;
      while (k < myNumKnots-endskip-1) {
         if (myKnots[k] == myKnots[k+1]) {
            multiplicity++;
         }
         else {
            while (multiplicity < d) {
               insertKnot (k, myKnots[k]);
               multiplicity++;
               k++;
            }
            multiplicity = 1;
         }
         k++;
      }
   }

   private void printKnots (String msg, double[] knots) {
      System.out.print (msg);
      for (int i=0; i<myNumKnots; i++) {
         System.out.printf (" %4.1f", myKnots[i]);
      }
      System.out.println ("");

   }

   public void insertKnot (int k, double uval) {
      int d = myDegree;
      if (k < d-2 && !myClosedP) {
         throw new IllegalArgumentException (
            "k is "+k+", must be >= degree-1 for open curves");
      }
      int numc = numControlPoints();
      Vector4d[] q = new Vector4d[d];
      for (int i=0; i<d; i++) {
         q[i] = new Vector4d();
         Vector4d c0 = getControlPoint (getCtrlIndex(k,i,numc));
         Vector4d c1 = getControlPoint (getCtrlIndex(k,i+1,numc));
         double u0, u1;
         u1 = myKnots[k+1+i];
         if (k-d+1+i >= 0) {
            u0 = myKnots[k-d+1+i];
         }
         else {
            // look at the end of the knot sequence to get appropriate value
            u0 = myKnots[0] - (myKnots[numc]-myKnots[numc+k-d+1+i]);
         }
         double alpha = (uval-u0)/(u1-u0);
         q[i].combine (1-alpha, c0, alpha, c1);
      }
      for (int i=0; i<d; i++) {
         if (i < d-1) {
            // just overwrite existing control point
            getControlPoint (getCtrlIndex(k,i+1,numc)).set (q[i]);
         }
         else {
            // insert new control point.
            addControlPoint (getCtrlIndex(k,d,numc), q[i]);
            numc++;
         }
      }
      ensureKnotCapacity (myNumKnots+1);
      myNumKnots++;
      for (int i=myNumKnots-1; i>k+1; i--) {
         myKnots[i] = myKnots[i-1];
      }
      myKnots[k+1] = uval;
      if (myClosedP) {
         // For periodic splines, only nc+1 knots are indepentant. Ensure that
         // the intervals of the end knots match those at the beginning.
         for (int i=0; i<2*d-2; i++) {
            myKnots[numc+1+i] = myKnots[numc+i] + (myKnots[i+1]-myKnots[i]);
         }
         myUstart = myKnots[d-1];
         myUend = myKnots[myNumKnots-d];
      }
   }

   void setKnots (int d, int type, double[] knots) {
      checkKnotConsistency ("", d, knots);

      myClosedP = (type == CLOSED);
      myDegree = d;
      ensureKnotCapacity (knots.length);
      for (int i = 0; i < knots.length; i++) {
         myKnots[i] = knots[i];
      }
      myNumKnots = knots.length;
      // basisVals = new double[d+1];
      myUstart = this.myKnots[myDegree - 1];
      myUend = this.myKnots[myNumKnots - myDegree];

      //setControlPoints (null, 0);
   }

   protected void ensureKnotCapacity (int maxk) {
      if (myKnots.length < maxk) {
         // increase the capacity of myKnots
         double[] newknots = new double[(int)Math.max(1.5*maxk, 10)];
         System.arraycopy (myKnots, 0, newknots, 0, myNumKnots);
         myKnots = newknots;
      }
   }      

   /**
    * Add an additional control point and knot to the end of this curve.  The
    * knot spacing can be given by <code>h</code>. If this is specified as -1, 
    * then the default uniform knot spacing (typically 1) is used.
    */
   public void addControlPoint (Vector4d pnt, double h) {
      addControlPoint (pnt);
      if (h < 0) {
         h = 1;
      }
      if (myKnots.length == myNumKnots) {
         // increase the capacity of myKnots
         double[] newknots = new double[Math.max(2*myKnots.length, 10)];
         System.arraycopy (myKnots, 0, newknots, 0, myNumKnots);
         myKnots = newknots;
      }
      myNumKnots++;
      myUend += h;
      if (myClosedP) {
         myKnots[myNumKnots-1] = myKnots[myNumKnots-2]+h;
      }
      else {
         // clamp end knots to new end value
         for (int i=0; i<myDegree; i++) {
            myKnots[myNumKnots-1-i] = myUend;
         }
      }
   }

   /**
    * Remove the control point at the end of this curve. The number of
    * remaining control points must be sufficient for the degree of the curve.
    */
   public void removeControlPoint() {
      int numc = numControlPoints();
      if (numc-1 < myDegree+1) {
         throw new IllegalStateException (
            ""+(numc-1)+" remaining controls points insufficient "+
            "to support curve of degree "+myDegree);
      }
      removeControlPoint (numc-1);
      myNumKnots--;
      myUend = myKnots[myNumKnots-myDegree];
      if (!myClosedP) {
         // clamp end knots to new end value
         for (int i=0; i<myDegree-1; i++) {
            myKnots[myNumKnots-1-i] = myUend;
         }
      }     
   }

   /**
    * Sets this NURBS curve to either an open or closed curve with a specified
    * degree, knots, and control points.
    * 
    * <p>
    * Let d, numk, and numc be the degree of the curve, the number of knots, and
    * the number of control points. The degree must be 1 or greater. For open
    * curves, <blockquote> numc = numk - d + 1 </blockquote> and for closed
    * curves, <blockquote> numc = numk - 2*d + 1 </blockquote>
    * 
    * <p>
    * The knots argument may be set to null, in which case an appropriate number
    * of uniformly spaced knots will be created, in the range [0, 1]. Otherwise,
    * if knots is specified, the ctrlPnts argument must contain at least numc
    * elements.
    * 
    * <p>
    * This method automatically sets ustart and uend (see
    * {@link #setRange setRange}) to knots[d-1] and knots[numk-d].
    * 
    * <p>
    * The control points are specified as 4-vectors, where their spatial
    * location is given by x, y, and z and their weight is given by w. The
    * points should not be in homogeneous form; i.e., x, y, and z should not be
    * premultipled by w.
    * 
    * @param d
    * degree of the curve
    * @param type
    * curve type, which must be either {@link #OPEN OPEN} or
    * {@link #CLOSED CLOSED}.
    * @param knots
    * knot values
    * @param ctrlPnts
    * control points
    * @throws IllegalArgumentException
    * if constraints on the arguments are violated
    */
   public void set (int d, int type, Vector4d[] ctrlPnts, double[] knots) {
      if (knots == null) {
         if (d < 1) {
            throw new IllegalArgumentException ("degree is less than one");
         }
         int numi = (type == CLOSED ? ctrlPnts.length : ctrlPnts.length - d);
         if (numi < 1) {
            throw new IllegalArgumentException ("not enough control points");
         }
         knots = createUniformKnots (numi, d, (type == OPEN));
      }

      setKnots (d, type, knots);

      int numCtrlPnts;
      if (myClosedP) {
         numCtrlPnts = knots.length - 2 * d + 1;
      }
      else {
         numCtrlPnts = knots.length - d + 1;
      }
      if (ctrlPnts.length < numCtrlPnts) {
         throw new IllegalArgumentException ("insufficient control points");
      }
      setControlPoints (ctrlPnts, numCtrlPnts);
   }

   /**
    * Resets the degrees, type, and knots of this curve. The requested
    * settings must be consistent with the current set of control points.
    * If <code>knots</code> is null, then appropriate uniform knots are
    * computed for the curve type.
    * 
    * @param d degree of the curve
    * @param type curve type, which must be either {@link #OPEN OPEN} or
    * {@link #CLOSED CLOSED}.
    * @param knots knot values (optional)
    */
   public void reset (int d, int type, double[] knots) {
      int numc = numControlPoints();
      if (d < 1) {
         throw new IllegalArgumentException ("degree must be at least one");
      }
      if (numc < d+1) {
         throw new IllegalArgumentException (
            "Curve degree "+d+" is too high for "+numc+" control points");
      }
      if (knots != null) {
         int mink = (type==CLOSED ? numc+2*d-1 : numc+d-1);
         if (knots.length < mink) {
            throw new IllegalArgumentException (
               "Insufficient number of knots for curve");
         }
         checkKnotConsistency ("", d, knots);
      }
      else {
         int numi = (type==CLOSED ? numc : numc-d);
         knots = createUniformKnots (numi, d, (type == OPEN));
      }
      setKnots (d, type, knots);
   }
   
   /**
    * Sets this curve to a piecewise Bezier curve with a prescribed degree and
    * set of control points. The required knots are computed automatically. If
    * numc is the number of control points and d is the degree, then the number
    * of pieces <i>nump</i> for such a curve is (numc - 1)/d. numc and d must
    * be such that this number is a positive integer. Knots will be created and
    * assigned such that the range of u is 0 to <i>nump</i>.
    * 
    * @param d
    * degree for the curve
    * @param ctrlPnts
    * control points
    * @throws IllegalArgumentException
    * if constraints on the arguments are violated
    */
   public void setBezier (int d, Vector4d[] ctrlPnts) {
      if (d < 1) {
         throw new IllegalArgumentException ("degree is less than one");
      }
      int npieces = (ctrlPnts.length - 1) / d;
      if (npieces < 1 || npieces * d != ctrlPnts.length - 1) {
         throw new IllegalArgumentException (
            "improper number of control points");
      }
      int nknots = ctrlPnts.length - 1 + d;
      double[] knots = new double[nknots];
      for (int i = 0; i < nknots; i++) {
         knots[i] = i / d;
      }
      set (d, OPEN, ctrlPnts, knots);
   }

   /**
    * Sets this curve to an eight-point circle formed using rational cubics.
    * 
    * @param x
    * center x coordinate
    * @param y
    * center y coordinate
    * @param radius
    * circle radius
    * @throws IllegalArgumentException
    * if radius is non-positive
    */
   public void setCircle (double x, double y, double radius) {
      if (radius <= 0) {
         throw new IllegalArgumentException ("radius must be positive");
      }
      Vector4d[] cpnts = new Vector4d[8];

      double w = Math.sqrt (2) / 4;

      cpnts[0] = new Vector4d (1, 0, 0, 1);
      cpnts[1] = new Vector4d (1, 1, 0, w);
      cpnts[2] = new Vector4d (0, 1, 0, 1);
      cpnts[3] = new Vector4d (-1, 1, 0, w);
      cpnts[4] = new Vector4d (-1, 0, 0, 1);
      cpnts[5] = new Vector4d (-1, -1, 0, w);
      cpnts[6] = new Vector4d (0, -1, 0, 1);
      cpnts[7] = new Vector4d (1, -1, 0, w);

      for (int i = 0; i < cpnts.length; i++) {
         cpnts[i].x = radius * cpnts[i].x + x;
         cpnts[i].y = radius * cpnts[i].y + y;
      }
      set (3, CLOSED, cpnts, null);
   }

   protected double getAlpha (int i, int n, double s) {
      double denom = myKnots[i+n] - myKnots[i];
      if (denom == 0) {
         return 0;
      }
      else {
         return (myKnots[i+n] - s) / denom;
      }
   }

   protected void basisValues (double[] vals, int k, double s) {
      // build up the basis values starting with B_i,0
      vals[myDegree] = 1;

      for (int n = 1; n <= myDegree; n++) {
         double alpha = getAlpha (k-n+1, n, s);
         vals[myDegree - n] = alpha*vals[myDegree-n+1];
         for (int i = 1; i < n; i++) {
            double alphaPrev = alpha;
            alpha = getAlpha (k-n+1+i, n, s);
            vals[myDegree-n+i] =
               (1-alphaPrev)*vals[myDegree-n+i] + alpha*vals[myDegree-n+i+1];
         }
         vals[myDegree] = (1-alpha)*vals[myDegree];
      }
   }

   /**
    * Gets the lower knot point index for the knot interval which contains the
    * parameter value u. If necessary, u is clipped so that it lies in the range
    * of active intervals defined by knots[d-1] and knots[numKnots-d], where d
    * is the degree of the curve and numKnots is the number of knots. This if u
    * is less than knots[d-1], d-1 is returned, while if u is greater than
    * knots[numKnots-d], numKnots-d-1 is returned.
    * 
    * @param u
    * curve parameter value
    * @return lower knot point index for interval containing u
    */
   public int getKnotIndex (double u) {
      int i = myDegree - 1;
      if (u < myKnots[i]) {
         return i;
      }
      while (i < myNumKnots - myDegree - 1) {
         if (myKnots[i] <= u && u <= myKnots[i+1] && myKnots[i] < myKnots[i+1]) {
            return i;
         }
         i++;
      }
      return myNumKnots - myDegree - 1;
   }

   /**
    * Returns the index of the <code>i</code>-th control point associated with
    * knot <code>k</code>. <code>i</code> should lie in the range [0, d], where
    * d is the degree of the curve.
    */
   protected int getCtrlIndex (int k, int i, int numCtrlPnts) {
      int idx;

      if (myClosedP) {
         // first idx for first knot (degree-1) should be -degree/2.
         idx = k - myDegree + 1 + i - myDegree / 2;
         idx = (numCtrlPnts + idx) % numCtrlPnts;
      }
      else {
         idx = k - myDegree + 1 + i;
      }
      return idx;
   }

   protected final double U_SEARCH_TOL = 1e-8;

   protected boolean bracket (
      double[] u, double[] sv, Point3d pnt, double smin, double smax) {
      Point3d pntu = new Point3d();
      boolean bracketted = false;
      double s0 = smin;
      double s1 = (smin + smax) / 2.0;
      double s2 = smax;

      eval (pntu, smin);
      double uinit = pntu.distance (pnt);

      eval (pntu, smax);
      double ufinal = pntu.distance (pnt);

      double u1 = uinit;

      if (ufinal >= uinit) {
         while (s2 - s0 > U_SEARCH_TOL) {
            s1 = (s2 + s0) / 2;
            eval (pntu, s1);
            u1 = pntu.distance (pnt);
            if (u1 < uinit) {
               bracketted = true;
               break;
            }
            else {
               s2 = s1;
            }
         }
      }
      else {
         while (s2 - s0 > U_SEARCH_TOL) {
            s1 = (s2 + s0) / 2;
            eval (pntu, s1);
            u1 = pntu.distance (pnt);
            if (u1 < ufinal) {
               bracketted = true;
               break;
            }
            else if (u1 == ufinal) {
               s2 = s1;
            }
            else {
               s0 = s1;
            }
         }
      }
      u[0] = u1;
      sv[0] = s0;
      sv[1] = s1;
      sv[2] = s2;
      return (bracketted);
   }

   protected double golden (double[] u, double[] sv, Point3d pnt, double fb) {
      Point3d pntu = new Point3d();
      double s0, s1, s2, s3;
      double f1, f2;
      double r = (Math.sqrt (5) - 1) / 2.0;
      double c = 1 - r;

      s0 = sv[0];
      s3 = sv[2];
      if (sv[2] - sv[1] > sv[1] - sv[0]) {
         s1 = sv[1];
         f1 = fb;
         s2 = sv[1] + c * (sv[2] - sv[1]);
         eval (pntu, s2);
         f2 = pntu.distance (pnt);
         // f2 = calcEnergy (minfo, s2);
      }
      else {
         s2 = sv[1];
         f2 = fb;
         s1 = sv[1] - c * (sv[1] - sv[0]);
         eval (pntu, s1);
         f1 = pntu.distance (pnt);
         // f1 = calcEnergy (minfo, s1);
      }
      while (s3 - s0 > U_SEARCH_TOL) {
         if (f2 < f1) {
            s0 = s1;
            s1 = s2;
            s2 = r * s1 + c * s3;
            f1 = f2;
            // f2 = calcEnergy (minfo, s2);
            eval (pntu, s2);
            f2 = pntu.distance (pnt);
         }
         else {
            s3 = s2;
            s2 = s1;
            s1 = r * s2 + c * s0;
            f2 = f1;
            eval (pntu, s1);
            f1 = pntu.distance (pnt);
            // f1 = calcEnergy (minfo, s1);
         }
      }
      if (f2 < f1) {
         u[0] = f2;
         return (s2);
      }
      else {
         u[0] = f1;
         return (s1);
      }
   }

   public double computeControlPolygonLength () {
      double len = 0;
      int numc = numControlPoints();
      if (numc > 0) {
         Point3d diff = new Point3d();
         Vector4d c0 = getControlPoint(0);
         for (int i=1; i<numControlPoints(); i++) {
            Vector4d c1 = getControlPoint(i);
            diff.x = c1.x-c0.x;
            diff.y = c1.y-c0.y;
            diff.z = c1.z-c0.z;
            len += diff.norm();
            c0 = c1;
         }
      }
      return len;
   }

   public static boolean init = false;

   /**
    * {@inheritDoc}
    */
   public void render (Renderer renderer, RenderProps props, int flags) {
      boolean selecting = renderer.isSelecting();
      
      int numc = numControlPoints();
      if (numc == 0) {
         return;
      }

      renderer.pushModelMatrix();
      if (myXObjToWorld != RigidTransform3d.IDENTITY) {
         RigidTransform3d XOW = new RigidTransform3d(myXObjToWorld);
         renderer.mulModelMatrix (XOW);
      }
      renderer.setShading (Shading.NONE);

      if (myDrawControlShapeP) {
         // draw the control polygon
         if (props.getDrawEdges()) {
            renderer.setLineWidth (props.getEdgeWidth());
            if (!selecting) {
               renderer.setColor (props.getEdgeOrLineColorF());
            }
            if (myClosedP) {
               renderer.beginDraw (DrawMode.LINE_LOOP);
            }
            else {
               renderer.beginDraw (DrawMode.LINE_STRIP);
            }
            for (int i = 0; i < numc; i++) {
               Vector4d cpnt = myCtrlPnts.get(i);
               renderer.addVertex (cpnt.x, cpnt.y, cpnt.z);
            }
            renderer.endDraw();
         }

         // draw the control points
         drawControlPoints (renderer, props, flags);
      }

      //draw the curve itself
      if (!selecting) {
         renderer.setColor (props.getLineColorF());
      }

      double len = computeControlPolygonLength();
      double res = myResolution*renderer.distancePerPixel (myXObjToWorld.p);
      int nsegs = (int)Math.max(10, len/res);      

      Point3d pnt = new Point3d();
      renderer.setLineWidth (props.getLineWidth());
      renderer.beginDraw (DrawMode.LINE_LOOP);
      double[] urange = new double[2];
      getRange (urange);
      for (int i = 0; i < nsegs + 1; i++) {
         eval (pnt, urange[0] + (urange[1]-urange[0])*i/nsegs);
         renderer.addVertex (pnt);
      }
      renderer.endDraw();

      renderer.setLineWidth (1);

      renderer.setShading (Shading.FLAT);

      renderer.popModelMatrix();
   }

   /**
    * Returns the degree of this curve.
    * 
    * @return degree of the curve
    */
   public int getDegree() {
      return myDegree;
   }

   /**
    * Returns the number of knots used by this curve.
    * 
    * @return number of knots
    */
   public int numKnots() {
      return myNumKnots;
   }

   /**
    * Returns the i-th knot used by this curve.
    * 
    * @return i-th knot value
    */
   public double getKnot (int i) {
      return myKnots[i];
   }

   /**
    * Returns the knots used by this curve.
    * 
    * @return knot values
    */
   public double[] getKnots() {
      return myKnots;
   }

   /**
    * Reads this curve from a text description supplied by a reader. The allowed
    * format is a subset of the Alias Wavefront OBJ format for curves, and
    * consists of a set of statements, one per line (lines can be continued, if
    * necessary, using the line continuation character <code>\</code>).
    * 
    * <p>
    * The allowed statements are:
    * <ul>
    * <li>Vertex definition of the form <blockquote> v <i>x</i> <i>y</i>
    * <i>z</i> [<i>w</i>]
    *
    * </blockquote> each giving the x, y, and z values for a control point (with
    * an optional weighting value w which defaults to 1 if omitted).
    * <li>Degree statement of the form <blockquote> deg <i>d</i> </blockquote>
    * where d is the degree of the curve. A degree statement must precede a
    * curve statement (described below).
    * 
    * <li>Curve statement of the form <blockquote> curv <i>ustart</i> <i>uend</i>
    * <i>i0</i> <i>i1</i> ... </blockquote> where ustart and uend give the
    * minimum and maximum values for the curve parameter u, and i0, i1, etc.
    * give the indices of the control points defined by the vertex statements.
    * Control points are indexed, starting at 1, by their order of occurance. If
    * the index value is negative, then it gives the relative location of a
    * control point relative to the curve statement, where -1 is the closest
    * preceding, -2 is the next closest preceding, etc.
    * 
    * <li>A parameter statement of the form <blockquote> parm u [closed] <i>k0</i>
    * <i>k1</i> ... </blockquote> where <code>closed</code> is an optional
    * keyword indicating that the curve is closed, and k0, k1, etc. are the knot
    * values for the curve. The parameter statement must follow the curve
    * statement and precede the end statement (described below). If a curve has
    * degree d and numc control points, then it number of knots numk must be
    * given by <blockquote> numk = numc + 2*d - 1 </blockquote> if the curve is
    * closed and by <blockquote> numk = numc + d - 1 </blockquote> if the curve
    * is open. If no knots are specified, then an appropriate number of
    * uniformly-spaced knots is created with ustart and uend corresponding to 0
    * and 1.
    * <li>An end statement of the form <blockquote> end </blockquote> This must
    * be placed after the curve and parameter statements.
    * </ul>
    * 
    * <p>
    * As an example, here are the statements that define an eight point NURBS
    * circle:
    * 
    * <pre>
    *    v  1  0  0  1
    *    v  1  1  0  0.35355339059327
    *    v  0  1  0  1
    *    v -1  1  0  0.35355339059327
    *    v -1  0  0  1
    *    v -1 -1  0  0.35355339059327
    *    v  0 -1  0  1
    *    v  1 -1  0  0.35355339059327
    *    deg 3
    *    curv 0 1  1 2 3 4 5 6 7 8 
    *    parm u closed
    *    end
    * </pre>
    * 
    * @param reader
    * providing the text input
    * @throws IOException
    * if an I/O or format error occurs
    */
   public void read (Reader reader) throws IOException {
      readFromWavefront (new WavefrontReader(reader));
   }

   /**
    * Reads this curve in from a ReaderTokenizer, using the same format as
    * {@link #read(Reader) read(Reader)}.
    * 
    * @param rtok
    * tokenizer which provides the input
    * @throws IOException
    * if an I/O or format error occurs
    */
   public void read (ReaderTokenizer rtok) throws IOException {
      readFromWavefront (new WavefrontReader(rtok));
   }

   protected void readFromWavefront (WavefrontReader wfr) throws IOException {
      wfr.parse ();

      ArrayList<WavefrontReader.Curve> curveList = wfr.getCurveList();
      WavefrontReader.Curve curve = curveList.get (curveList.size() - 1);
      if (curve == null) {
         throw new IOException ("no curve specified in input");
      }
      try {
         set (curve, wfr.getHomogeneousPoints());
      }
      catch (IllegalArgumentException e) {
         throw new IOException (e.getMessage());
      }
   }

   public void set (WavefrontReader.Curve curve, Vector4d[] allPnts) {

      // build up the list of control points
      Vector4d[] ctrlPnts = new Vector4d[curve.indices.length];
      for (int i = 0; i < ctrlPnts.length; i++) {
         int idx = curve.indices[i];
         if (idx >= allPnts.length) {
            throw new IllegalArgumentException (
               "index " + (idx + 1) + " out of range, line " + curve.lineNum);
         }
         ctrlPnts[i] = allPnts[idx];
      }
      set (curve.degree,
           curve.isClosed ? CLOSED : OPEN,
           ctrlPnts,
           curve.knots.length != 0 ? curve.knots : null);
      setRange (curve.u0, curve.u1);
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

   public void write (PrintWriter pw, NumberFormat fmt, boolean relative)
      throws IOException {
      // StringBuffer buf = new StringBuffer(80);
      int numc = numControlPoints();
      for (int i = 0; i < myCtrlPnts.size(); i++) {
         pw.println ("v " + myCtrlPnts.get(i).toString (fmt));
      }
      pw.println ("deg " + myDegree);
      resetLine (pw);
      addToLine (pw, "curv " + fmt.format (myUstart) + " " + fmt.format (myUend));
      for (int i = 0; i < numc; i++) {
         addToLine (pw, relative ? " " + (-numc + i) : " " + (i + 1));
      }
      resetLine (pw);
      addToLine (pw, "parm u");
      if (myClosedP) {
         addToLine (pw, " closed");
      }
      for (int i = 0; i < myNumKnots; i++) {
         addToLine (pw, " " + fmt.format (myKnots[i]));
      }
      resetLine (pw);
      pw.println ("end");
      pw.flush();
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, String fmtStr, boolean relative)
      throws IOException {
      write (pw, new NumberFormat(fmtStr), relative);
   }

   /**
    * {@inheritDoc}
    */
   public RenderProps createRenderProps() {
      RenderProps props = new PointEdgeRenderProps();
      props.setDrawEdges (true);
      props.setPointSize (3);
      props.setLineWidth (2);
      props.setLineColor (Color.WHITE);
      props.setEdgeColor (Color.BLUE);
      props.setPointColor (Color.GREEN);
      return props;
   }

   protected void set (NURBSCurveBase curve) {
      super.set (curve);

      myDegree = curve.myDegree;
      myClosedP = curve.myClosedP;
      myUstart = curve.myUstart;
      myUend = curve.myUend;
      myResolution = curve.myResolution;
      myNumKnots = curve.myNumKnots;
      myKnots = new double[myNumKnots];
      for (int i=0; i<myNumKnots; i++) {
         myKnots[i] = curve.myKnots[i];
      }
   }


}
