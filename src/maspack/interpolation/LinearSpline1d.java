package maspack.interpolation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import maspack.util.IndentingPrintWriter;
import maspack.util.*;
import maspack.util.IntHolder;
import maspack.util.ReaderTokenizer;
import maspack.function.Diff1Function1x1Base;

/**
 * Implements 1D piecewise-linear spline interpolation.  The spline is defined
 * by a sequence of knots delimiting the boundaries between linear
 * interpolation segements.
 */
public class LinearSpline1d extends Diff1Function1x1Base
   implements Iterable<LinearSpline1d.Knot> {

   public static class Knot {
      double myX0;   // s value at knot
      double myY0; // x value at knot
      int myIndex;

      public Knot() {
          myIndex = -1;
      }

      public Knot (double x0, double y0) {
         this();
         set (x0, y0);
      }

      public double getX0() {
         return myX0;
      }
      
      public double getY0() {
         return myY0;
      }

      public void set (double x0, double y0) {
         myX0 = x0;
         myY0 = y0;
      }

      public double eval (double x, Knot next) {
         double delx = next.myX0 - myX0;
         if (delx == 0) {
            // shouldn't happen but just in case
            return myY0;
         }
         else {
            double t = (x-myX0)/delx; // normalize x to the interval
            return (1-t)*myY0 + t*next.myY0;
         }
      }

      public double evalDy (double x, Knot next) {
         double delx = next.myX0 - myX0;
         if (delx == 0) {
            // shouldn't happen but just in case
            return 0;
         }
         else {
            return (next.myY0-myY0)/delx;
         }
      }

      /**
       * Creates a copy of this knot. All coefficients are copied except the
       * index.
       */
      public Knot copy() {
         Knot knot = new Knot();
         knot.myX0 = myX0;
         knot.myY0 = myY0;
         return knot;
      }
      
      public int getIndex() {
         return myIndex;
      }

      public void setIndex(int idx) {
         myIndex = idx;
      }

      public boolean equals (Knot knot) {
         return (myX0 == knot.myX0 && myY0 == knot.myY0);
      }
   }

   ArrayList<Knot> myKnots;

   /**
    * Creates a empty spline
    */
   public LinearSpline1d() {
      myKnots = new ArrayList<>();
   }

   /**
    * Creates a spline from specified values of x and y at each knot point. The
    * values of x should be in strictly ascending order.
    *
    * @param x x values at each knot point
    * @param y y values at each knot point
    */
   public LinearSpline1d (double[] x, double[] y) {
      this();
      set (x, y);
   }

   /**
    * Creates a spline from a copy of an existing one.
    *
    * @param spline spline to copy
    */
   public LinearSpline1d (LinearSpline1d spline) {
      this();
      set (spline);
   }

   /**
    * Sets this spline from specified values of x and y at each knot point. The
    * values of x should be in strictly ascending order.
    *
    * @param x x values at each knot point
    * @param y y values at each knot point
    */
   public void set (double[] x, double[] y) {
      if (x.length != y.length) {
         throw new IllegalArgumentException (
            "x and y have different lengths");
      }
      for (int i=0; i<x.length-1; i++) {
         if (x[i] > x[i+1]) {
            throw new IllegalArgumentException (
               "x values not in strictly ascending order");
         }
      }
      clearKnots();
      for (int i=0; i<x.length; i++) {
         addKnot (x[i], y[i]);
      }
   }

   /**
    * Returns an array giving the values of x at each knot.
    *
    * @return array of x values
    */
   public double[] getXValues() {
      double[] xvals = new double[numKnots()];
      int idx = 0;
      for (Knot knot : myKnots) {
         xvals[idx++] = knot.myX0;
      }
      return xvals;
   }

   /**
    * Adds another knot point to this spline at a specified x coordinate.
    *
    * @param x x value at the knot
    * @param y y value at the knot
    */
   public Knot addKnot (double x, double y) {
      Knot knot = new Knot (x, y);
      Knot prev;
      Knot last = getLastKnot();
      if (last != null && last.myX0 <= x) {
         prev = last;
      }
      else {
         prev = getPreceedingKnot (x);
      }
      if (prev == null) {
         doAddKnot (0, knot);
      }
      else {
         doAddKnot (prev.getIndex()+1, knot);
      }
      return knot;
   }

   // reindex knots starting at idx
   private void reindexFrom (int idx) {
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

   private void doAddKnot (int idx, Knot knot) {
      myKnots.add (idx, knot);
      reindexFrom (idx);
   }

   protected void addKnot (int idx, Knot knot) {
      doAddKnot (idx, knot);
   }

   /**
    * Removes a knot from this spline.
    *
    * @param knot knot to remove
    */
   public boolean removeKnot (Knot knot) {
      int idx = myKnots.indexOf (knot);
      if (idx != -1) {
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
    * Find the knot immediately preceeding x. Specifically, find the nearest
    * knot for which {@code knot.getX0() <= x}. If no such knot exists, {@code
    * null} is returned.
    *
    * @param x value for which preceeding knot is sought
    */
   public Knot getPreceedingKnot (double x) {
      return getPreceedingKnot (x, null);
   }

   /**
    * Find the knot immediately preceeding x. Specifically, find the nearest
    * knot for which {@code knot.getX0() <= x}. If no such knot exists, {@code
    * null} is returned.
    *
    * <p>The optional parameter {@code lastIdx}, described below, can be used
    * to significantly reduce the search speed when queries are made with
    * increasing values of x.
    *
    * @param x value for which preceeding knot is sought
    * @param lastIdx if non-null, specifies a index value giving a hint on
    * where to start the knot search. On output, this value is set to the index
    * of the returned knot, or -1 if no knot is found.
    */
   public Knot getPreceedingKnot (double x, IntHolder lastIdx) {
      if (myKnots.size() == 0) {
         return null;
      }
      Knot prev = null;
      int k0 = 0;
      if (lastIdx != null) {
         int idx = lastIdx.value;
         if (idx > 0 && idx < myKnots.size() && myKnots.get(idx).myX0 <= x) {
            k0 = idx;
         }
      }
      for (int k=k0; k<myKnots.size(); k++) {
         if (myKnots.get(k).myX0 > x) {
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
    * Find the knot immediately following x. Specifically, find the nearest
    * knot for which {@code knot.x0 > x}. If no such knot exists, {@code null}
    * is returned.
    *
    * @param x value for which next knot is sought
    */
   public Knot getNextKnot (double x) {
      if (myKnots.size() == 0) {
         return null;
      }
      Knot next = null;
      for (int k=myKnots.size()-1; k >= 0; k--) {
         if (x >= myKnots.get(k).myX0) {
            break;
         }
         next = myKnots.get(k);
      }
      return next;
   }

   private Knot getNextKnot (Knot prev) {
      if (prev.myIndex < numKnots()-1) {
         return myKnots.get(prev.myIndex+1);
      }
      else {
         return null;
      }
   }

   private Knot getPrevKnot (Knot prev) {
      if (prev.myIndex > 0) {
         return myKnots.get(prev.myIndex-1);
      }
      else {
         return null;
      }
   }

   /**
    * Evaluates the y value of this spline for a specific x value. For
    * parameter values outside the knot range, the value is lineraly
    * extrapolated based on derivatives at the first or last knots. An empty
    * spline evaluates to 0.
    *
    * @param x value at which the y value should be computed
    */
   public double eval (double x) {
      return eval (x, null);
   }

   /**
    * Evaluates the y value of this spline for a specific x value. For
    * parameter values outside the knot range, the value is lineraly
    * extrapolated based on derivatives at the first or last knots. An empty
    * spline evaluates to 0.
    *
    * <p>The optional parameter {@code lastIdx}, described below, can be used
    * to significantly reduce the search speed when queries are made with
    * increasing values of x.
    *
    * @param x value at which the y value should be computed
    * @param lastIdx if non-null, specifies a index value giving a hint on
    * where to search for the knot preceeding x. On output, this value is set
    * to the index of the preceeding knot, or -1 if no preceeding knot is
    * found.
    */
   public double eval (double x, IntHolder lastIdx) {
      if (numKnots() > 0) {
         Knot prev = getPreceedingKnot (x, lastIdx);
         if (prev == null) {
            prev = myKnots.get(0);
         }
         Knot next = getNextKnot (prev);
         if (next == null) {
            Knot pprev = getPrevKnot (prev);
            if (pprev == null) {
               return prev.myY0;
            }
            else {
               next = prev;
               prev = pprev;
            }
         }
         return prev.eval (x, next);
      }
      else {
         return 0;
      }
   }

   /**
    * Evaluates the derivative (with respect to x) of this spline for a
    * specific x value. For parameter values outside the knot range, the value
    * is set to the derivative at either the first or last knot. An empty
    * spline evaluates to 0.
    *
    * @param x value at which the derivative should be computed
    */
   public double evalDy (double x) {
      return evalDy (x, null);
   }
   
   public double eval (DoubleHolder deriv, double x) {
      if (deriv != null) {
         deriv.value = evalDy (x);
      }
      return eval (x);
   }

   public double evalDeriv (double x) {
      return evalDy (x);
   }

   /**
    * Evaluates the derivative (with respect to the parameter) of this spline
    * for a specific x value. For parameter values outside the knot range, the
    * value is set to the derivative at either the first or last knot. An empty
    * spline evaluates to 0.
    *
    * <p>The optional parameter {@code lastIdx}, described below, can be used
    * to significantly reduce the search speed when queries are made with
    * increasing values of x.
    *
    * @param x value for which the derivative should be computed
    * @param lastIdx if non-null, specifies a index value giving a hint on
    * where to search for the knot preceeding x. On output, this value is set
    * to the index of the preceeding knot, or -1 if no preceeding knot is
    * found.
    */
   public double evalDy (double x, IntHolder lastIdx) {
      if (numKnots() > 0) {
         Knot prev = getPreceedingKnot (x, lastIdx);
         if (prev == null) {
            prev = myKnots.get(0);
         }
         Knot next = getNextKnot (prev);
         if (next == null) {
            Knot pprev = getPrevKnot (prev);
            if (pprev == null) {
               return 0;
            }
            else {
               next = prev;
               prev = pprev;
            }
         }
         return prev.evalDy (x, next);
      }
      else {
         return 0;
      }
   }
   
   /**
    * Scans this spline from a ReaderTokenizer. The expected format consists of
    * an opening square bracket, followed by {@code 2 n} numeric values giving
    * the x and y values for each of the {@code n} knots, and terminated
    * with a closing square bracket. Whitespace characters are ignored. For
    * example,
    * <pre>
    *  [  0.0 1.0
    *     2.0 4.3
    *     3.0 5.0
    *  ]
    * </pre>
    * describes a spline with three knots, with x and y equal to
    * (0, 1), (2, 4.4), and (3, 5), respectively.
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
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsNumber()) {
            throw new IOException ("knot x value expected, "+rtok);
         }
         double x0 = rtok.nval;
         rtok.nextToken();
         if (!rtok.tokenIsNumber()) {
            throw new IOException ("knot y value expected, "+rtok);
         }
         double y0 = rtok.nval;
         addKnot (x0, y0);
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
            pw.println (
               fmt.format(knot.myX0) + " " + fmt.format(knot.myY0));
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
    * Returns the firxt x value of this spline.
    *
    * @return starting x value
    */
   public double getX0() { 
      if (myKnots.size() > 0) {
         return myKnots.get(0).getX0();
      }
      else {
         return 0;
      }
   }

   /**
    * Returns the last x value of this spline.
    *
    * @return last x value
    */
   public double getXLast() { 
      if (myKnots.size() > 0) {
         return myKnots.get(numKnots()-1).getX0();
      }
      else {
         return 0;
      }
   }

   /**
    * Returns an iterator for the knots in this spline.
    *
    * @return spline knot iterator
    */
   public Iterator<Knot> iterator() {
      return myKnots.iterator(); //new MyIterator();
   }

   /**
    * Sets this spline to be a copy of another spline.
    *
    * @param spline spline to copy
    */
   public void set (LinearSpline1d spline) {
      clearKnots();
      for (Knot knot : spline) {
         doAddKnot (myKnots.size(), knot.copy());
      }
   }

   /**
    * Creates a copy of this spline.
    */
   public LinearSpline1d copy () {
      LinearSpline1d spline = new LinearSpline1d();
      spline.set (this);
      return spline;
   }

   /**
    * Queries whether this spline is equal to another spline.
    *
    * @param spline spline to compare to this one
    * @return {@code true} if the splines are equal
    */
   public boolean equals (LinearSpline1d spline) {
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

   public LinearSpline1d clone() {
      LinearSpline1d spline = (LinearSpline1d)super.clone();
      spline.set (this);
      return spline;
   }

}
