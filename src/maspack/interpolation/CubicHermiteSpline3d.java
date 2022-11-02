package maspack.interpolation;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import maspack.util.*;
import maspack.matrix.*;

/**
 * Implements a three dimensional cubic Hermite spline interpolated by an
 * independent parameter s. The spline is defined by a sequence of knots
 * delimiting the boundaries between cubic interpolation segements.
 */
public class CubicHermiteSpline3d
   implements Scannable, Iterable<CubicHermiteSpline3d.Knot> {

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
      public void computeCoefficients (Knot next) {
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

         double h = next.myS0 - myS0;  // interval size
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
      addKnot (s0, a0, a1);
      addKnot (s1, x1, dx1);      
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
            // knot is at the beginning; no need to update coefficients
         }
         else if (idx < myKnots.size()-1) {
            // knot is before the end
            myKnots.get(idx-1).computeCoefficients (myKnots.get(idx+1));
         }
         else {
            // knot is at the end; no need to update coefficients
            myKnots.get(idx-1).clearCoefficients();
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

   // /**
   //  * Find the knot immediately preceeding s. Specifically, find the nearest
   //  * knot for which {@code knot.x0 <= s}. If no such knot exists, {@code null}
   //  * is returned.
   //  *
   //  * @param s value for which preceeding knot is sought
   //  */
   // public Knot getPreceedingKnot (double s) {
   //    if (myKnots.size() == 0) {
   //       return null;
   //    }
   //    Knot prev = null;
   //    for (int k=0; k<myKnots.size(); k++) {
   //       if (s < myKnots.get(k).myS0) {
   //          break;
   //       }
   //       prev = myKnots.get(k);
   //    }
   //    return prev;
   // }

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
      if (numKnots() > 0) {
         Knot prev = getPreceedingKnot (s, lastIdx);
         if (prev == null) {
            prev = myKnots.get(0);
         }
         else if (prev == myKnots.get(numKnots()-1)) {
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
    * @param rtok
    * Tokenizer from which to scan the spline
    * @param ref
    * optional reference object (not used)
    * @throws IOException
    * if an I/O or formatting error occured
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      clearKnots();
      rtok.scanToken ('[');
      Vector3d a0 = new Vector3d();
      Vector3d a1 = new Vector3d();
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsNumber()) {
            throw new IOException ("knot s0 value expected, "+rtok);
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
    * Returns the last value of the parameter controlling this spline.
    *
    * @return last parameter value
    */
   public double getSLast() { 
      if (myKnots.size() > 0) {
         return myKnots.get(numKnots()-1).getS0();
      }
      else {
         return 0;
      }
   }

   /**
    * Returns the length of the parameter interval controlling this spline.
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
         Knot k = myKnots.get(idx-1);
         myKnots.get (idx-1).computeCoefficients (knot);
      }
      if (idx >= 0 && idx < numKnots()-1) {
         knot.computeCoefficients (myKnots.get (idx+1));
      }
   }

   /**
    * Sets this spline to be a copy of another spline.
    *
    * @param spline spline to copy
    */
   public void set (CubicHermiteSpline3d spline) {
      clearKnots();
      for (Knot knot : spline) {
         doAddKnot (myKnots.size(), knot.copy());
      }
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
      return true;
   }
   

}
