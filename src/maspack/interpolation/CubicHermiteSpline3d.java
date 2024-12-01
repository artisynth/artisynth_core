package maspack.interpolation;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import maspack.util.*;
import maspack.matrix.*;
import maspack.solvers.*;

/**
 * Implements a three dimensional cubic Hermite spline interpolated by an
 * independent parameter s. The spline is defined by a sequence of knots
 * delimiting the boundaries between cubic interpolation segements.
 */
public class CubicHermiteSpline3d
   implements Scannable, Iterable<CubicHermiteSpline3d.Knot> {

   private static double DOUBLE_PREC = 1e-16;
   private double myClosingLength = 0;

   public static class Knot {
      double myS0;   // initial s value
      Vector3d myA0;   // initial x value
      Vector3d myA1;   // initial x derivative value
      Vector3d myA2;
      Vector3d myA3;
      int myIndex;

      public Knot() {
         myA0 = new Vector3d();
         myA1 = new Vector3d();
         myA2 = new Vector3d();
         myA3 = new Vector3d();
         myIndex = -1;
      }

      public Knot (double s0, Vector3d a0, Vector3d a1) {
         this();
         set (s0, a0, a1);
      }

      public double getS0() {
         return myS0;
      }
      
      public Vector3d getA0() {
         return myA0;
      }
      
      public Vector3d getA1() {
         return myA1;
      }

      public Vector3d getA2() {
         return myA2;
      }
      
      public Vector3d getA3() {
         return myA3;
      }

      public void set (double s0, Vector3d a0, Vector3d a1) {
         myS0 = s0;
         myA0.set (a0);
         myA1.set (a1);
         myA2.setZero();
         myA3.setZero();
      }

      public void eval (Vector3d x, double s) {
         s -= myS0;
         if (s < 0) {
            x.scaledAdd (s, myA1, myA0);
         }
         else {
            x.scaledAdd (s, myA3, myA2);
            x.scaledAdd (s, x, myA1);
            x.scaledAdd (s, x, myA0);
         }
      }

      public void evalDeriv (Vector3d dx, double s) {
         s -= myS0;
         if (s < 0) {
            dx.set (myA1);
         }
         else {
            dx.combine (2, myA2, s*3, myA3);
            dx.scaledAdd (s, dx, myA1);
         }
      }

      public void evalDeriv2 (Vector3d ddx, double s) {
         if (s < 0) {
            ddx.setZero();
         }
         else {
            ddx.combine (2, myA2, s*6, myA3);
         }
      }

      /**
       * Computes coefficents for interpolating within the interval
       * between this knot and the one following it.
       */
      public void computeCoefficients (Knot next, double h) {
         //
         // Let (x0, dx0, x1, dx1) give x and its derivative dxds at the end
         // points on the interval, and assume s is translated to 0 at the
         // start of the interval. Then the matrix mapping the coefficients
         // (a0, a1, a2, a3) onto (x0, dx0, x1, dx1) is
         //
         // [ 1, 0,   0,     0]
         // [ 0, 1,   0,     0]
         // [ 1, h, h^2,   h^3]
         // [ 0, 1, 2*h, 3*h^2]
         //
         // where h is the length of the interval. The matrix inverse is then
         //
         // [      1,     0,      0,     0]
         // [      0,     1,      0,     0] 
         // [ -3/h^2,  -2/h,  3/h^2,  -1/h]
         // [  2/h^3, 1/h^2, -2/h^3, 1/h^2]

         Vector3d x0s = new Vector3d();
         x0s.scale (1/h, myA0);
         Vector3d x1s = new Vector3d();
         x1s.scale (1/h, next.myA0);
         Vector3d dx0 = myA1;
         Vector3d dx1 = next.myA1;
         
         myA2.combine (-3, x0s, -2, dx0);
         myA2.scaledAdd (3, x1s);
         myA2.sub (dx1);
         myA2.scale (1/h);

         myA3.combine (2, x0s, -2, x1s);
         myA3.add (dx0);
         myA3.add (dx1);
         myA3.scale (1/(h*h));
      }

      public void clearCoefficients () {
         myA2.setZero();
         myA3.setZero();
      }            

      /**
       * Creates a copy of this knot. All coefficients are copied except the
       * index.
       */
      public Knot copy() {
         Knot knot = new Knot();
         knot.myS0 = myS0;
         knot.myA0.set (myA0);
         knot.myA1.set (myA1);
         knot.myA2.set (myA2);
         knot.myA3.set (myA3);
         return knot;
      }
      
      public int getIndex() {
         return myIndex;
      }

      public void setIndex(int idx) {
         myIndex = idx;
      }

      public boolean equals (Knot knot) {
         return (myS0 == knot.myS0 &&
                 myA0.equals(knot.myA0) &&
                 myA1.equals(knot.myA1) &&
                 myA2.equals(knot.myA2) &&
                 myA3.equals(knot.myA3));
      }

      public boolean epsilonEquals (
         Knot knot, double stol, double a0tol, double a1tol) {
         
         return (Math.abs(myS0-knot.myS0) <= stol &&
                 (myA0.distance(knot.myA0) <= a0tol) &&
                 (myA1.distance(knot.myA1) <= a1tol));
      }
   }

   ArrayList<Knot> myKnots;

   /**
    * Creates a empty spline
    */
   public CubicHermiteSpline3d() {
      myKnots = new ArrayList<>();
   }

   /**
    * Creates a spline from data containing the values of x and dxds at each
    * knot point, with the interval between each knot point assumed to have a
    * length of 1. Note that x and dxds correspond to the a0 and a1 coefficient
    * values for the knot.
    *
    * @param xVals x values for each knot point
    * @param dxdsVals dxds values for each knot point
    */
   public CubicHermiteSpline3d (
      ArrayList<? extends Vector3d> xVals, ArrayList<Vector3d> dxdsVals) {
      this();
      double[] sVals = new double[xVals.size()];
      for (int i=0; i<sVals.length; i++) {
         sVals[i] = i;
      }
      set (sVals, xVals, dxdsVals);
   }

   /**
    * Creates a spline from data containing the values of s, x and dxds at each
    * knot point. The values of s should be in strictly ascending order. Note
    * that x and dxds correspond to the a0 and a1 coefficient values for the
    * knot.
    *
    * @param sVals s values at each knot point
    * @param xVals x values for each knot point
    * @param dxdsVals dxds values for each knot point
    */
   public CubicHermiteSpline3d (
      double[] sVals,
      ArrayList<? extends Vector3d> xVals, ArrayList<Vector3d> dxdsVals) {
      this();
      set (sVals, xVals, dxdsVals);
   }

   /**
    * Creates a spline from a copy of an existing one.
    *
    * @param spline spline to copy
    */
   public CubicHermiteSpline3d (CubicHermiteSpline3d spline) {
      this();
      set (spline);
   }

   /**
    * Queries whether or not this spline is closed.
    *
    * @return {@code true} if the spline is closed
    */
   public boolean isClosed() {
      return myClosingLength > 0;
   }

   /**
    * For closed splines, queries the parameter length of the closing segment
    * between the last and first knots. If the spline is open, returns 0.
    *
    * @return closing segment length, or 0 if the spline is not closed.
    */
   public double getClosingLength() {
      return myClosingLength;
   }

   /**
    * Sets whether or not this spline is closed. If {@code h > 0}, there
    * spline will be closed with {@code h} giving the parameter length
    * between the last knot and the first knot. Otherwise, the spline will be
    * set to open.
    *
    * @param h if {@code > 0}, closes the spline and specifies the length of
    * the closing segment between the last and first knots.
    */
   public void setClosed (double h) {
      if (h < 0) {
         h = 0;
      }
      if (myClosingLength != h) {
         if (numKnots() > 1) {
            Knot prev = getLastKnot();
            if (h != 0) {
               // closing
               prev.computeCoefficients (getFirstKnot(), h);
            }
            else {
               // openning
               prev.clearCoefficients ();
            }
         }
         myClosingLength = h;
      }
   }

   /**
    * For closed splines, adds or subtracts {@link #getSLength()} to {@code s}
    * to ensure that it lies in the parameter range defined by {@link #getS0()}
    * and {@link #getSLast()}. For open splines, {@code s} is not modified.
    *
    * @param s curve parameter to be reduced
    * @return reduced value of {@code s}, or {@code s} for open splines
    */
   public double normalizeS (double s) {
      if (isClosed()) {
         double slen = getSLength();
         while (s < getS0()) {
            s += slen;
         }
         while (s > getSLast()) {
            s -= slen;
         }
      }
      return s;         
   }

   /**
    * Sets this spline from specified values of s, x and dxds at each knot
    * point. The values of s should be in strictly ascending order.
    *
    * @param sVals s values at each knot point
    * @param xVals x values for each knot point
    * @param dxdsVals dxds values for each knot point
    */
   public void set (
      double[] sVals, 
      ArrayList<? extends Vector3d> xVals, ArrayList<Vector3d> dxdsVals) {
      if (sVals.length != xVals.size()) {
         throw new IllegalArgumentException (
            "sVals and xVals have different lengths");
      }
      if (xVals.size() != dxdsVals.size()) {
         throw new IllegalArgumentException (
            "xVals and dxdsVals have different lengths");
      }
      for (int i=0; i<sVals.length-1; i++) {
         if (sVals[i] > sVals[i+1]) {
            throw new IllegalArgumentException (
               "sVals not in strictly ascending order");
         }
      }
      myKnots.clear();
      for (int i=0; i<xVals.size(); i++) {
         addKnot (sVals[i], xVals.get(i), dxdsVals.get(i));
      }
   }

   /**
    * Sets this spline to one consisting of a single segment with two knots,
    * with the coeffcients computed to best fit a set of x values uniformly
    * placed along the interval s in [s0, s1].
    */
   public void setSingleSegment (
      ArrayList<? extends Vector3d> xVals, double s0, double s1) {
      if (xVals.size() < 4) {
         throw new IllegalArgumentException (
            "xVals must have size >= 4");
      }
      int nump = xVals.size();
      MatrixNd A = new MatrixNd (nump, 4);
      double sl = s1-s0;
      for (int i=0; i<nump; i++) {
         double s = i*sl/(double)(nump-1);
         double a = 1;
         for (int j=0; j<4; j++) {
            A.set (i, j, a);
            a *= s;
         }
      }
      QRDecomposition qrd = new QRDecomposition (A);
      VectorNd b = new VectorNd (nump);
      VectorNd y = new VectorNd (4);
      Vector3d a0 = new Vector3d();
      Vector3d a1 = new Vector3d();
      Vector3d a2 = new Vector3d();
      Vector3d a3 = new Vector3d();
      for (int k=0; k<3; k++) {
         for (int i=0; i<nump; i++) {
            b.set (i, xVals.get(i).get(k));
         }
         qrd.solve (y, b);
         a0.set (k, y.get(0));
         a1.set (k, y.get(1));
         a2.set (k, y.get(2));
         a3.set (k, y.get(3));
      }
      // given coefs on unit interval, find x1 and dx1 at s = sl
      Vector3d x1 = new Vector3d();
      Vector3d dx1 = new Vector3d();
      x1.scaledAdd (sl, a3, a2);
      x1.scaledAdd (sl, x1, a1);
      x1.scaledAdd (sl, x1, a0);
      dx1.combine (3*sl, a3, 2, a2);
      dx1.scaledAdd (sl, dx1, a1);

      // build the spline from the two knots
      clearKnots();
      setClosed (0);
      addKnot (s0, a0, a1);
      addKnot (s1, x1, dx1);      
   }

   /**
    * Sets this spline to one consisting of n segments with n+1 knots, at s
    * values given by {@code svals}, with the coeffcients computed to best fit
    * a set of x values uniformly placed along the entire interval.  C(2)
    * continuity is enforced between intervals, with the second derivatives at
    * the end points equal to 0.
    */
   public void setMultiSegment (
      ArrayList<? extends Vector3d> xVals, double[] svals) {

      int nsegs = svals.length-1;
      if (nsegs <= 0) {
         throw new IllegalArgumentException (
            "svals must have at least two values");
      }
      int numc = 2*(nsegs-1); // number of constraints
      int dir = 0;
      for (int i=0; i<nsegs; i++) {
         boolean nonmonotone = false;
         if (i == 0) {
            if (svals[i] == svals[i+1]) {
               nonmonotone = true;
            }
            dir = (svals[i] < svals[i+1] ? 1 : -1);
         }
         else {
            if ((svals[i+1]-svals[i])*dir <= 0) {
               nonmonotone = true;
            }
         }
         if (nonmonotone) {
            throw new IllegalArgumentException (
               "svals must be monotonically increasing or decreasing");
         }
      }
      int nump = xVals.size();
      if (nump < (nsegs+1)*2) {
          throw new IllegalArgumentException (
             "not enough points: at least "+(nsegs+1)*2+
             " required for "+nsegs+" segments");
      }

      // build the A matrix for determining coefficents without constraints
      MatrixNd A = new MatrixNd (nump, 4*nsegs);
      double slen = svals[nsegs]-svals[0];  // length of whole interval
      int kseg = 0; // index of the segment
      int netrank = 0; // net rank of the matrix A
      int segrank = 0; // rank added to A for a particular segment
      for (int i=0; i<nump; i++) {
         double s = svals[0] + i*slen/(double)(nump-1);
         while (s >= svals[kseg+1] && kseg < nsegs-1) {
            if (segrank == 0) {
               throw new IllegalArgumentException (
                  "No points supplied for segment "+kseg);
            }
            netrank += segrank;
            segrank = 0;
            kseg++;
         }
         double ss = s - svals[kseg]; // local s value in segment
         double a = 1;
         for (int j=0; j<4; j++) {
            A.set (i, 4*kseg+j, a);
            a *= ss;
         }
         if (segrank < 4) {
            segrank++;
         }
      }
      if (kseg != nsegs-1) {
          throw new IllegalArgumentException (
             "No points supplied for segments beyond "+kseg);
      }
      netrank += segrank;
      if (netrank < 2*(nsegs+1)) {
         throw new IllegalArgumentException (
            "Points insufficiently distributed for full rank solve matrix. " +
            "Ranks is " + netrank + ", must be >= "+(2*(nsegs+1)));
      }
      
      // constraint matrix
      MatrixNd C = new MatrixNd (numc, 4*nsegs);
      for (int i=0; i<numc; i += 2) {
         kseg = i/2; // segment associated with this constraint
         double sl = svals[kseg+1]-svals[kseg];
         double c = 1;
         for (int j=0; j<4; j++) {
            C.set (i, 4*kseg+j, c);
            c *= sl;
         }
         C.set (i, 4*kseg+4, -1);
         C.set (i+1, 4*kseg+1, 1);
         C.set (i+1, 4*kseg+2, 2*sl);
         C.set (i+1, 4*kseg+3, 3*sl*sl);
         C.set (i+1, 4*kseg+5, -1);
      }
      // system matrix
      MatrixNd M = new MatrixNd (4*nsegs+numc, 4*nsegs+numc);
      MatrixNd ATA = new MatrixNd (4*nsegs, 4*nsegs); // Gram matrix
      ATA.mulTransposeLeft (A, A);
      M.setSubMatrix (0, 0, ATA);
      // add constraint matrix
      M.setSubMatrix (4*nsegs, 0, C);
      MatrixNd CT = new MatrixNd();
      CT.transpose(C);
      M.setSubMatrix (0, 4*nsegs, CT);
      LUDecomposition lud = new LUDecomposition (M);
      //double condEst = lud.conditionEstimate (M);

      // coefficient vectors, one each for x, y, z
      VectorNd a[] = new VectorNd[3];
      for (int k=0; k<3; k++) {
         VectorNd p = new VectorNd (nump);
         for (int i=0; i<nump; i++) {
            p.set (i, xVals.get(i).get(k));
         }
         VectorNd b = new VectorNd (4*nsegs);
         A.mulTranspose (b, p);
         b.setSize (4*nsegs+numc); // add extra 0s for constraints
         a[k] = new VectorNd(4*nsegs+numc);
         lud.solve (a[k], b);
      }

      clearKnots();
      setClosed (0);
      Vector3d a0 = new Vector3d();
      Vector3d a1 = new Vector3d();
      for (int i=0; i<nsegs; i++) {
         for (int k=0; k<3; k++) {
            a0.set (k, a[k].get(i*4));
            a1.set (k, a[k].get(i*4+1));
         }
         addKnot (svals[i], a0, a1);
      }
      // for last knot, need a2 and a3 so we can compute final x and dx 
      Vector3d a2 = new Vector3d();
      Vector3d a3 = new Vector3d();
      for (int k=0; k<3; k++) {
         a2.set (k, a[k].get((nsegs-1)*4+2));
         a3.set (k, a[k].get((nsegs-1)*4+3));
      }
      Vector3d xl = new Vector3d();
      Vector3d dxl = new Vector3d();
      double s = svals[nsegs]-svals[nsegs-1];
      xl.scaledAdd (s, a3, a2);
      xl.scaledAdd (s, xl, a1);
      xl.scaledAdd (s, xl, a0);
      dxl.combine (3*s, a3, 2, a2);
      dxl.scaledAdd (s, dxl, a1);
      addKnot (svals[nsegs], xl, dxl);      
   }

   /**
    * Sets this spline to a natural cubic spline for a set of knot points.
    *
    * @param xvals coordinate value of each knot point
    * @param svals s value of each knot point
    */
   public void setNatural (
      ArrayList<? extends Vector3d> xvals, double[] svals) {

      myKnots.clear();
      int numk = xvals.size();
      if (numk != svals.length) {
         throw new IllegalArgumentException (
            "kvals and svals have differing lengths of " + numk +
            " and " + svals.length);
      }
      double[] h;
      Vector3d[] r;
      Vector3d[] cvals = null;
      Vector3d xdiff = new Vector3d();
      Vector3d dxds = new Vector3d();
      Vector3d d = new Vector3d();
      Vector3d a2next = new Vector3d();
      if (isClosed()) {
         h = new double[numk];
         r = new Vector3d[numk];
      }
      else {
         h = new double[numk-1];
         r = new Vector3d[numk-2];
      }
      for (int i=0; i<r.length; i++) {
         r[i] = new Vector3d();
      }
      for (int i=0; i<numk-1; i++) {
         h[i] = svals[i+1] - svals[i];
      }
      if (isClosed()) {
         h[numk-1] = getClosingLength();
         if (numk > 2) {
            VectorNd c = new VectorNd(h);
            VectorNd b = new VectorNd(numk);           
            int iprev = numk-1;
            for (int i=0; i<numk; i++) {
               int inext = (i+1)%numk;
               xdiff.sub (xvals.get(inext), xvals.get(i));
               r[i].scale (3/h[i], xdiff);
               xdiff.sub (xvals.get(i), xvals.get(iprev));
               r[i].scaledAdd (-3/h[iprev], xdiff);
               b.set (i, 2*(h[iprev]+h[i]));
               iprev = i;
            }
            cvals = TriDiagonalSolver.solveCyclical (c, b, c, r);
            for (int i=0; i<numk; i++) {
               int inext = (i+1)%numk;
               xdiff.sub (xvals.get(inext), xvals.get(i));
               dxds.scaledAdd (2, cvals[i], cvals[inext]);
               dxds.scale (-h[i]/3);
               dxds.scaledAdd (1/h[i], xdiff);
               addKnot (svals[i], xvals.get(i), dxds);
            }
         }
         else {
            addKnot (svals[0], xvals.get(0), Vector3d.ZERO);
            addKnot (svals[1], xvals.get(1), Vector3d.ZERO);
         }
      }
      else {
//         for (int i=0; i<numk-2; i++) {
//            xdiff.sub (xvals.get(i+2), xvals.get(i+1));
//            r[i].scale (3/h[i+1], xdiff);
//            xdiff.sub (xvals.get(i+1), xvals.get(i));
//            r[i].scaledAdd (-3/h[i], xdiff);
//         }
         if (numk > 2) {
            VectorNd c = new VectorNd(numk-2);
            VectorNd b = new VectorNd(numk-2);           
            for (int i=0; i<numk-2; i++) {
               xdiff.sub (xvals.get(i+2), xvals.get(i+1));
               r[i].scale (3/h[i+1], xdiff);
               xdiff.sub (xvals.get(i+1), xvals.get(i));
               r[i].scaledAdd (-3/h[i], xdiff);
               c.set (i, h[i+1]);
               b.set (i, 2*(h[i]+h[i+1]));             
            }
            cvals = TriDiagonalSolver.solve (c, b, c, r);
            xdiff.sub (xvals.get(1), xvals.get(0));
            dxds.scale (-h[0]/3, cvals[0]);
            dxds.scaledAdd (1/h[0], xdiff);
            addKnot (svals[0], xvals.get(0), dxds);
            for (int i=0; i<numk-2; i++) {
               xdiff.sub (xvals.get(i+2), xvals.get(i+1));
               if (i==numk-3) {
                  a2next.setZero();
               }
               else {
                  a2next.set (cvals[i+1]);
               }
               dxds.scaledAdd (2, cvals[i], a2next);
               dxds.scale (-h[i+1]/3);
               dxds.scaledAdd (1/h[i+1], xdiff);
               addKnot (svals[i+1], xvals.get(i+1), dxds);
            }
            Knot prev = myKnots.get(numk-2);
            double hp = h[numk-2];
            Vector3d a2prev = (numk-3 >= 0 ? cvals[numk-3] : Vector3d.ZERO);
            d.scale (-1/(3*hp), a2prev);
            dxds.combine (3*hp, d, 2, a2prev);
            dxds.scale (hp);
            dxds.add (prev.myA1);
            addKnot (svals[numk-1], xvals.get(numk-1), dxds);
         }
         else {
            dxds.sub (xvals.get(1), xvals.get(0));
            dxds.scale (1/h[0]);
            addKnot (svals[0], xvals.get(0), dxds);
            addKnot (svals[1], xvals.get(1), dxds);
         }
      }
   }

   /**
    * Adds another knot point to this cubic hermite spline, consisting of an s
    * coordinate, along with x and dxds values.
    *
    * @param s parameter value at which the knot should be added
    * @param x value at the knot
    * @param x derivative value at the knot
    */
   public Knot addKnot (double s, Vector3d x, Vector3d dxds) {
      Knot knot = new Knot (s, x, dxds);
      Knot prev = getPreceedingKnot (s);
      if (prev == null) {
         doAddKnot (0, knot);
      }
      else {
         doAddKnot (prev.getIndex()+1, knot);
      }
      updateCoefficients (knot);
      return knot;
   }

   // reindex knots starting at idx
   protected void reindexFrom (int idx) {
      while (idx < myKnots.size()) {
         myKnots.get(idx).setIndex(idx);
         idx++;
      }
      for (int k=0; k<myKnots.size(); k++) {
         if (myKnots.get(k).getIndex() != k) {
            throw new InternalErrorException (
               "bad index at "+k+": "+ myKnots.get(k).getIndex());
         }
      }
   }

   protected void doAddKnot (int idx, Knot knot) {
      myKnots.add (idx, knot);
      reindexFrom (idx);
   }

   protected void addKnot (int idx, Knot knot) {
      doAddKnot (idx, knot);
      updateCoefficients (knot);
   }

   /**
    * Removes a knot from this spline.
    *
    * @param knot knot to remove
    */
   public boolean removeKnot (Knot knot) {
      int idx = myKnots.indexOf (knot);
      if (idx != -1) {
         if (idx == 0) {
            // knot is at the beginning
            if (isClosed()) {
               Knot last = getLastKnot();
               if (numKnots() > 2) {
                  last.computeCoefficients (getKnot(1), myClosingLength);
               }
               else {
                  last.clearCoefficients ();
               }
            }
         }
         else if (idx < myKnots.size()-1) {
            // knot is before the end
            Knot prev = myKnots.get(idx-1);
            Knot next = myKnots.get(idx+1);
            prev.computeCoefficients (next, next.myS0-prev.myS0);
         }
         else {
            // knot is at the end
            Knot prev = myKnots.get(idx-1);
            if (isClosed() && numKnots() > 2) {
               prev.computeCoefficients (getFirstKnot(), myClosingLength);
            }
            else {
               prev.clearCoefficients();
            }
         }
         myKnots.remove (idx);
         reindexFrom (idx);
         knot.setIndex (-1);
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Removes all knots from from this spline.
    */
   public void clearKnots() {
      myKnots.clear();
   }

   /**
    * Find the knot immediately preceeding s. Specifically, find the nearest
    * knot for which {@code knot.getS0() <= s}. If no such knot exists, {@code
    * null} is returned.
    *
    * @param s value for which preceeding knot is sought
    */
   public Knot getPreceedingKnot (double s) {
      return getPreceedingKnot (s, null);
   }

   /**
    * Find the knot immediately preceeding s. Specifically, find the nearest
    * knot for which {@code knot.x0 <= s}. If no such knot exists, {@code null}
    * is returned.
    *
    * <p>The optional parameter {@code lastIdx}, described below, can be used
    * to significantly reduce the search speed when queries are made with
    * increasing values of s.
    *
    * @param s value for which preceeding knot is sought
    * @param lastIdx if non-null, specifies a index value giving a hint on
    * where to start the knot search. On output, this value is set to the index
    * of the returned knot, or -1 if no knot is found.
    */
   public Knot getPreceedingKnot (double s, IntHolder lastIdx) {
      if (myKnots.size() == 0) {
         return null;
      }
      Knot prev = null;
      int k0 = 0;
      if (lastIdx != null) {
         int idx = lastIdx.value;
         if (idx > 0 && idx < myKnots.size() && myKnots.get(idx).myS0 <= s) {
            k0 = idx;
         }
      }
      for (int k=k0; k<myKnots.size(); k++) {
         if (myKnots.get(k).myS0 > s) {
            break;
         }
         prev = myKnots.get(k);
      }
      if (lastIdx != null) {
         lastIdx.value = (prev != null ? prev.myIndex : -1);
      }
      return prev;
   }

   /**
    * Find the knot immediately following s. Specifically, find the nearest
    * knot for which {@code knot.x0 > s}. If no such knot exists, {@code null}
    * is returned.
    *
    * @param s value for which next knot is sought
    */
   public Knot getNextKnot (double s) {
      if (myKnots.size() == 0) {
         return null;
      }
      Knot next = null;
      for (int k=myKnots.size()-1; k >= 0; k--) {
         if (s >= myKnots.get(k).myS0) {
            break;
         }
         next = myKnots.get(k);
      }
      return next;
   }

   /**
    * Evaluates the value of this spline for a specific parameter value. For
    * parameter values outside the knot range, the value is lineraly
    * extrapolated based on derivatives at the first or last knots. An empty
    * spline evaluates to 0.
    *
    * @param s parameter for which the value should be computed
    */
   public Vector3d eval (double s) {
      return eval (s, null);
   }

   /**
    * Evaluates the value of this spline for a specific parameter value. For
    * parameter values outside the knot range, the value is lineraly
    * extrapolated based on derivatives at the first or last knots. An empty
    * spline evaluates to 0.
    *
    * <p>The optional parameter {@code lastIdx}, described below, can be used
    * to significantly reduce the search speed when queries are made with
    * increasing values of s.
    *
    * @param s parameter for which the value should be computed
    * @param lastIdx if non-null, specifies a index value giving a hint on
    * where to search for the knot preceeding s. On output, this value is set
    * to the index of the preceeding knot, or -1 if no preceeding knot is
    * found.
    */
   public Vector3d eval (double s, IntHolder lastIdx) {
      Vector3d x = new Vector3d();
      if (isClosed()) {
         s = normalizeS (s);
      }
      if (numKnots() > 0) {
         Knot prev = getPreceedingKnot (s, lastIdx);
         if (prev == null) {
            prev = myKnots.get(0);
         }
         prev.eval (x, s);
      }
      return x;
   }

   /**
    * Evaluates the derivative (with respect to the parameter) of this spline
    * for a specific parameter value. For parameter values outside the knot
    * range, the value is set to the derivative at either the first or last
    * knot. An empty spline evaluates to 0.
    *
    * @param s parameter for which the derivative should be computed
    */
   public Vector3d evalDx (double s) {
      return evalDx (s, null);
   }

   /**
    * Evaluates the derivative (with respect to the parameter) of this spline
    * for a specific parameter value. For parameter values outside the knot
    * range, the value is set to the derivative at either the first or last
    * knot. An empty spline evaluates to 0.
    *
    * <p>The optional parameter {@code lastIdx}, described below, can be used
    * to significantly reduce the search speed when queries are made with
    * increasing values of s.
    *
    * @param s parameter for which the derivative should be computed
    * @param lastIdx if non-null, specifies a index value giving a hint on
    * where to search for the knot preceeding s. On output, this value is set
    * to the index of the preceeding knot, or -1 if no preceeding knot is
    * found.
    */
   public Vector3d evalDx (double s, IntHolder lastIdx) {
      Vector3d dx = new Vector3d();
      if (isClosed()) {
         s = normalizeS (s);
      }
      if (numKnots() > 0) {
         Knot prev = getPreceedingKnot (s, lastIdx);
         if (prev == null) {
            prev = myKnots.get(0);
         }
         prev.evalDeriv (dx, s);
      }
      return dx;
   }
   
   /**
    * Evaluates the second derivative (with respect to the parameter) of this
    * spline for a specific parameter value. For parameter values outside the
    * knot range, the value is set to 0.
    *
    * @param s parameter for which the derivative should be computed
    */
   public Vector3d evalDx2 (double s) {
      return evalDx2 (s, null);
   }
   
   /**
    * Evaluates the second derivative (with respect to the parameter) of this
    * spline for a specific parameter value. For parameter values outside the
    * knot range, the value is set to 0.
    *
    * <p>The optional parameter {@code lastIdx}, described below, can be used
    * to significantly reduce the search speed when queries are made with
    * increasing values of s.
    *
    * @param s parameter for which the derivative should be computed
    * @param lastIdx if non-null, specifies a index value giving a hint on
    * where to search for the knot preceeding s. On output, this value is set
    * to the index of the preceeding knot, or -1 if no preceeding knot is
    * found.
    */
   public Vector3d evalDx2 (double s, IntHolder lastIdx) {
      Vector3d ddx = new Vector3d();      
      if (isClosed()) {
         s = normalizeS (s);
      }
      if (numKnots() > 0) {
         Knot prev = getPreceedingKnot (s, lastIdx);
         if (prev == null) {
            prev = myKnots.get(0);
         }
         else if (!isClosed() && prev == myKnots.get(numKnots()-1)) {
            // need preceeding knot because last one doesn't define dy2
            prev = myKnots.get(numKnots()-2);
         }
         prev.evalDeriv2 (ddx, s);
      }
      return ddx;
   }

   /**
    * Scans this spline from a ReaderTokenizer. The expected format consists of
    * an opening square bracket, followed by {@code 7 n} numeric values giving
    * the s, x and dxdx values for each of the {@code n} knots, and terminated
    * with a closing square bracket. Whitespace characters are ignored. For
    * example,
    * <pre>
    *  [  0.0
    *     0.0 4.0 1.0
    *     2.0 5.0 -1.0
    *     1.0
    *     0.0 5.0 1.2
    *     -2.0 1.0 0.0
    *  ]
    * </pre>
    * describes a spline with two knots, with s, x and dxds equal to {@code 0},
    * {@code (0, 4, 1)} and {@code (2, 5, -1)} for the first knot and {@code
    * 1}, {@code (0, 5, 1.2)} and {@code (-2, 1, 0)} for the second knot.
    *
    * If the spline is closed, the coordinate list is terminated with the
    * keyword {@code CLOSED}, followed by the length of the closing segment:
    * <pre>
    *  [  0.0
    *     0.0 4.0 1.0
    *     2.0 5.0 -1.0
    *     1.0
    *     0.0 5.0 1.2
    *     -2.0 1.0 0.0
    *     2.0
    *     0.3 3.0 2.0
    *     -1.0 5.0 0.0
    *     CLOSED 1.0
    *  ]
    * </pre>
    * 
    * @param rtok
    * Tokenizer from which to scan the spline
    * @param ref
    * optional reference object (not used)
    * @throws IOException
    * if an I/O or formatting error occured
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      clearKnots();
      setClosed (0);
      rtok.scanToken ('[');
      Vector3d a0 = new Vector3d();
      Vector3d a1 = new Vector3d();
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsNumber()) {
            // check for closing
            if (rtok.tokenIsWord() && rtok.sval.equalsIgnoreCase("closed")) {
               double clen = rtok.scanNumber();
               setClosed (clen);
               rtok.scanToken (']');
               return;
            }
            else {
               throw new IOException ("knot s0 value expected, "+rtok);
            }
         }
         double s0 = rtok.nval;
         try {
            a0.scan (rtok);
         }
         catch (IOException e) {
            throw new IOException ("error scanning knot x value, "+rtok);
         }
         try {
            a1.scan (rtok);
         }
         catch (IOException e) {
            throw new IOException ("error scanning knot dxds value, "+rtok);
         }
         addKnot (s0, a0, a1);
      }
   }

   /**
    * Writes a text description of this spline to a PrintWriter. The format
    * is the same as that described for {@link #scan scan}.
    * 
    * @param pw
    * writer to which the spline should be written
    * @param fmt
    * describes how the numbers should be formatted
    * @param ref
    * optional reference object (not used)
    * @throws IOException
    * if an I/O error occured
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      if (myKnots.size() == 0) {
         pw.println ("[ ]");
      }
      else {
         pw.println ("[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (Knot knot : myKnots) {
            pw.println (fmt.format(knot.myS0));
            pw.println (knot.myA0.toString(fmt));
            pw.println (knot.myA1.toString(fmt));
         }
         if (myClosingLength > 0) {
            pw.println ("CLOSED " + fmt.format(myClosingLength));
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isWritable() {
      return true;
   }

   /**
    * Returns the number of knots in this spline.
    *
    * @return number of knots
    */
   public int numKnots() {
      return myKnots.size();
   }

   /**
    * Returns the {@code idx}-th knot in this spline.
    *
    * @param idx index of the requested knot 
    * @return {@code idx}-th knot
    */
   public Knot getKnot (int idx) {
      return myKnots.get (idx);
   }

   /**
    * Returns the first knot in this spline, or {@code null} if there
    * are no knots.
    *
    * @return first knot
    */
   public Knot getFirstKnot () {
      if (myKnots.size() > 0) {
         return myKnots.get (0);
      }
      else {
         return null;
      }
   }

   /**
    * Returns the last knot in this spline, or {@code null} if there
    * are no knots.
    *
    * @return last knot
    */
   public Knot getLastKnot () {
      if (myKnots.size() > 0) {
         return myKnots.get (numKnots()-1);
      }
      else {
         return null;
      }
   }

   /**
    * Returns the start value of the parameter controlling this spline.
    *
    * @return starting parameter value
    */
   public double getS0() { 
      if (myKnots.size() > 0) {
         return myKnots.get(0).getS0();
      }
      else {
         return 0;
      }
   }

   /**
    * Returns the last value of the parameter controlling this spline.  If the
    * spline is open, this is the {@code s} value of the last knot. If the
    * spline is closed, this is the {@code s} value of the last knot, plus the
    * closing length (as returned by {@link #getClosingLength}).
    *
    * @return last parameter value
    */
   public double getSLast() { 
      if (myKnots.size() > 0) {
         return myKnots.get(numKnots()-1).getS0() + getClosingLength();
      }
      else {
         return 0;
      }
   }

   /**
    * Returns the length of the parameter interval controlling this spline.
    * This is the difference between {@link #getSLast()} and {@link #getS0()}.
    *
    * @return parameter interval length
    */
   public double getSLength() { 
      return getSLast() - getS0();
   }

   /**
    * Returns an iterator for the knots in this spline.
    *
    * @return spline knot iterator
    */
   public Iterator<Knot> iterator() {
      return myKnots.iterator(); //new MyIterator();
   }

   private void updateCoefficients (Knot knot) {
      int idx = knot.getIndex();
      if (idx > 0) {
         Knot prev = myKnots.get(idx-1);
         prev.computeCoefficients (knot, knot.myS0-prev.myS0);
      }
      else if (isClosed()) {
         Knot prev = myKnots.get(numKnots()-1);
         prev.computeCoefficients (knot, myClosingLength);
      }
      if (idx < numKnots()-1) {
         Knot next = myKnots.get(idx+1);
         knot.computeCoefficients (next, next.myS0-knot.myS0);
      }
      else if (isClosed()) {
         Knot next = myKnots.get(0);
         knot.computeCoefficients (next, myClosingLength);
      }
      else {
         knot.clearCoefficients();
      }
   }

   /**
    * Sets this spline to be a copy of another spline.
    *
    * @param spline spline to copy
    */
   public void set (CubicHermiteSpline3d spline) {
      clearKnots();
      setClosed (0);
      for (Knot knot : spline) {
         doAddKnot (myKnots.size(), knot.copy());
      }
      setClosed (spline.getClosingLength());
   }

   /**
    * Creates and returns a list of values sampled uniformly along this spline.
    *
    * @param numv number of values to sample
    */
   public ArrayList<Vector3d> getSampledValues (int numv) {
      ArrayList<Vector3d> values = new ArrayList<>();
      double s0 = getS0();
      double sl = getSLast();
      double slen = sl - s0;
      if (sl-s0 > 0) {
         for (int i=0; i<=numv; i++) {
            double s = s0 + i*slen/numv;
            values.add (eval(s));
         }
      }
      return values;
   }

   /**
    * Creates a copy of this spline.
    */
   public CubicHermiteSpline3d copy () {
      CubicHermiteSpline3d spline = new CubicHermiteSpline3d();
      spline.set (this);
      return spline;
   }

   /**
    * Queries whether this spline is equal to another spline.
    *
    * @param spline spline to compare to this one
    * @return {@code true} if the splines are equal
    */
   public boolean equals (CubicHermiteSpline3d spline) {
      if (numKnots() != spline.numKnots()) {
         return false;
      }
      for (int i=0; i<numKnots(); i++) {
         if (!getKnot(i).equals (spline.getKnot(i))) {
            return false;
         }
      }
      if (myClosingLength != spline.myClosingLength) {
         return false;
      }
      return true;
   }


   /**
    * Queries whether this spline is equal to another spline within a specified
    * absolute tolerance. Only the s0, a0 and a1 values of each knot are
    * compared, since other values are derived from these.
    *
    * @param spline spline to compare to this one
    * @param tol absolute tolerance with which to compare the splines
    * @return {@code true} if the splines are equal within tolerance
    */
   public boolean epsilonEquals (CubicHermiteSpline3d spline, double tol) {
      if (numKnots() != spline.numKnots()) {
         return false;
      }
      for (int i=0; i<numKnots(); i++) {
         if (!getKnot(i).epsilonEquals (
                spline.getKnot(i), tol, tol, tol)) {
            return false;
         }
      }
      if (Math.abs(myClosingLength-spline.myClosingLength) > tol) {
         return false;
      }
      return true;
   }
}
