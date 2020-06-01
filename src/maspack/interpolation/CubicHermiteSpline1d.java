package maspack.interpolation;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import maspack.util.*;
import maspack.matrix.*;

/**
 * Implements one dimensional cubic Hermite spline interpolation in the {@code
 * x/y} plane.  The spline is described by a series of segments, each
 * specifying an interval of {@code x} within which {@code y} is interpolated
 * cubicly. The spline is defined by a sequence of knots delimiting the
 * boundaries of these segments, with each knot specifying {@code y} and its
 * derivative for a given value of {@code x}.
 */
public class CubicHermiteSpline1d
   implements Scannable, Iterable<CubicHermiteSpline1d.Knot> {

   /**
    * Describes a single knot for a specific value of x. This contains values
    * of y and its derivative. If this is not the final knot in the spline,
    * then it also stores additional coefficients for cubic interpolation
    * within the interval defined by this knot and the one following it.
    */
   public static class Knot {
      double x0;    // x value
      double y0;    // y value
      double dy0;   // y derivative
      double a2;    // additional coefficients for cubic interpolation
      double a3;

      int myIndex = -1;  // index of this knot within the spline

      public Knot () {
      }

      public Knot (double x0, double y0, double dy0) {
         this.x0 = x0;
         this.y0 = y0;
         this.dy0 = dy0;
      }

      public double eval (double x) {
         // interpolation is done locally, with respect to x - x0
         x -= x0;
         if (x < 0) {
            return y0 + dy0*x;
         }
         else {
            return ((a3*x + a2)*x + dy0)*x + y0;
         }
      }

      public double evalDeriv (double x) {
         // interpolation is done locally, with respect to x - x0
         x -= x0;
         if (x < 0) {
            return dy0;
         }
         else {
            return (3*a3*x + 2*a2)*x + dy0;
         }
      }

      /**
       * Computes coefficents for interpolating within the interval
       * between this knot and the one following it.
       */
      public void computeCoefficients (Knot next) {
         //
         // Let (y0, dy0, y1, dy1) give y and dy at the end points on the
         // interval, and assume x is translated to 0 at the start of the
         // interval. Then the matrix mapping the coefficients (y0, y0d, a2,
         // a3) onto (y0, y0d, y1, dy1) is
         //
         // [ 1, 0,   0,     0]
         // [ 0, 1,   0,     0]
         // [ 1, h, h^2,   h^3]
         // [ 0, 1, 2*h, 3*h^2]
         //
         // where h is the length of the interval. The matrix inverse is
         //
         // [      1,     0,      0,     0]
         // [      0,     1,      0,     0] 
         // [ -3/h^2,  -2/h,  3/h^2,  -1/h]
         // [  2/h^3, 1/h^2, -2/h^3, 1/h^2]

         double h = next.x0 - x0;  // interval size
         double y0s = y0/h;
         double y1s = next.y0/h;
         double dy1 = next.dy0;

         a2 = (-3*y0s - 2*dy0 + 3*y1s - dy1)/h;
         a3 = (2*y0s + dy0 - 2*y1s + dy1)/(h*h);
      }

      public void clearCoefficients () {
         a2 = 0;
         a3 = 0;
      }            

      public double getX() {
         return x0;
      }

      public double getY() {
         return y0;
      }

      public double getDy() {
         return dy0;
      }

      public void setX (double x) {
         x0 = x;
      }

      public void setY (double y) {
         y0 = y;
      }

      public void setDy (double dy) {
         dy0 = dy;
      }

      /**
       * Creates a copy of this knot. All coefficients are copied except the
       * index.
       */
      public Knot copy() {
         Knot knot = new Knot();
         knot.x0 = x0;
         knot.y0 = y0;
         knot.dy0 = dy0;
         knot.a2 = a2;
         knot.a3 = a3;
         knot.myIndex = myIndex;
         return knot;
      }
      
      public int getIndex() {
         return myIndex;
      }

      public void setIndex(int idx) {
         myIndex = idx;
      }
   }

   private class MyIterator implements Iterator<Knot> {
      int myIdx = 0;

      public boolean hasNext() {
         return myIdx < numKnots();
      }

      public Knot next() {
         try {
            return myKnots.get (myIdx++);
         }
         catch (Exception e) {
            throw new NoSuchElementException();
         }
      }
   }

   ArrayList<Knot> myKnots;

   /**
    * Creates a empty spline
    */
   public CubicHermiteSpline1d() {
      myKnots = new ArrayList<>();
   }

   /**
    * Creates a spline from data containing the x, y and y derivative values
    * for each knot point.
    *
    * @param x x values. These should be in ascending order. If they are not,
    * they will be sorted into ascending order (internally) with the y and
    * derivative values reordered appropriately. All x values should also be
    * unique.
    * @param y y values
    * @param dy y derivative values
    */
   public CubicHermiteSpline1d (double[] x, double[] y, double[] dy) {
      this();
      if (y.length != y.length) {
         throw new IllegalArgumentException (
            "y.length "+y.length+" not equal to x.length " + x.length);
      }
      if (dy.length != x.length) {
         throw new IllegalArgumentException (
            "dy.length "+dy.length+" not equals to x.length " + x.length);
      }
      set (x, y, dy);
   }

   /**
    * Creates a spline from data containing the x, y and y derivative values
    * for each knot point. This is specified using a single array with {@code 3
    * n} entries, where {@code n} is the number of knots. The entries
    * should gave the form
    * <pre>
    *  x0 y0 dy0 x1 y1 dy1 x2 y2 dy2 ...
    * </pre>
    * where {@code xi}, {@code yi}, and {@code dyi} are the x, y
    * and derivative values for the i-th knot.
    *
    * <p>The x values should be in ascending order. If they are not, they will
    * be sorted into ascending order (internally) with the y and derivative
    * values reordered appropriately. All x values should also be unique.
    * 
    * @param values x, y, and derivative values for the knots.
    */
   public CubicHermiteSpline1d (double[] values) {
      this();
      if (values.length < 3) {
         throw new IllegalArgumentException (
            "values must contain 3*n entries, where n is the number of knots");
      }
      int n = values.length/3;
      double[] x = new double[n];
      double[] y = new double[n];
      double[] dy = new double[n];
      int k = 0;
      for (int i=0; i<n; i++) {
         x[i] = values[k++];
         y[i] = values[k++];
         dy[i] = values[k++];
      }
      set (x, y, dy);
   }

   private void set (double[] x, double[] y, double[] dy) {
      for (int i=0; i<x.length-1; i++) {
         if (x[i] > x[i+1]) {
            // need to reorder
            x = Arrays.copyOf (x, x.length);
            double[] yNew = Arrays.copyOf (y, y.length);
            double[] dyNew = Arrays.copyOf (dy, dy.length);
            int[] indices = new int[x.length];
            for (int k=0; k<indices.length; k++) {
               indices[k] = k;
            }
            ArraySort.quickSort (x, indices);
            for (int k=0; k<x.length-1; k++) {
               if (x[i] == x[i+1]) {
                  throw new IllegalArgumentException (
                     "x contains non-unique value "+x[i]);
               }
            }
            for (int k=0; k<indices.length; k++) {
               yNew[k] = y[indices[k]];
               dyNew[k] = dy[indices[k]];
            }
            y = yNew;
            dy = dyNew;
            break;
         }
      }
      for (int i=0; i<x.length-1; i++) {
         addKnot (x[i], y[i], dy[i]);
      }
   }

   /**
    * Creates a spline from a copy of an existing one.
    *
    * @param spline spline to copy
    */
   public CubicHermiteSpline1d (CubicHermiteSpline1d spline) {
      this();
      set (spline);
   }

   /**
    * Adds another knot point to this cubic hermite spline, consisting of an x
    * coordinate, along with y and y derivative values. The value of x must be
    * greater than the x value of any previous knot point.
    */
   public Knot addKnot (double x, double y, double dy) {
      Knot knot = new Knot (x, y, dy);
      Knot prev = getPreviousKnot (x);
      if (prev == null) {
         addKnot (0, knot);
      }
      else {
         if (prev.x0 == x) {
            // just replace the existing knot
            myKnots.set (prev.getIndex(), knot);
            knot.setIndex (prev.getIndex());
         }
         else {
            addKnot (prev.getIndex()+1, knot);
         }
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

   protected void addKnot (int idx, Knot knot) {
      myKnots.add (idx, knot);
      reindexFrom (idx);
   }

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

   public void clearKnots() {
      for (int k=0; k<myKnots.size(); k++) {
         myKnots.get(k).setIndex (-1);
      }
      myKnots.clear();
   }

   /**
    * Find the knot immediately preceeding x. Specifically, find the nearest
    * knot for which {@code knot.x0 <= x}. If no such knot exists, {@code null}
    * is returned.
    *
    * @param x value for which previous knot is sought
    */
   public Knot getPreviousKnot (double x) {
      if (myKnots.size() == 0) {
         return null;
      }
      Knot prev = null;
      for (int k=0; k<myKnots.size(); k++) {
         if (x < myKnots.get(k).x0) {
            break;
         }
         prev = myKnots.get(k);
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
         if (x >= myKnots.get(k).x0) {
            break;
         }
         next = myKnots.get(k);
      }
      return next;
   }

   /**
    * Evaluates the y derivative value spline at a specific x value. Values
    * which are outside the specified knot range are extrapolated based on y
    * and dy values at the end point knots. An empty spline evaluates to 0.
    *
    * @param x value at which the y derivative should be evaluated
    */
   public double evalDy (double x) {
      Knot prev = getPreviousKnot (x);
      if (prev == null) {
         return myKnots.get(0).evalDeriv (x);         
      }
      else {
         return prev.evalDeriv (x);
      }
   }
   
   /**
    * Evaluates the y value for this spline at a specific x value. Values which
    * are outside the specified knot range are extrapolated based on y and dy
    * values at the end point knots. An empty spline evaluates to 0.
    *
    * @param x value at which y should be evaluated
    */
   public double evalY (double x) {
      Knot prev = getPreviousKnot (x);
      if (prev == null) {
         return myKnots.get(0).eval (x);         
      }
      else {
         return prev.eval (x);
      }
   }
   
    /**
    * Scans this spline from a ReaderTokenizer. The expected format consists of
    * an openning square bracket, followed by {@code 3 n} numeric values giving
    * the x, y amd y derivative values for each of the {@code n} knots, and
    * terminated with a closing square bracket. Whitespace characters are
    * ignored. For example,
    * 
    * <pre>
    *  [  0.0 4.0 1.0  2.0 5.0 -1.0 ]
    * </pre>
    * 
    * describes a spline with two knots, with the first at {@code x = 0.0} and
    * having y and y derivative values of {@code 4.0} and {@code 1.0}, and the
    * second at {@code x = 2.0} with y and y derivative values of {@code
    * 5.0} and {@code -1.0}.
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
         double x, y, dy;
         if (!rtok.tokenIsNumber()) {
            throw new IOException ("knot x value expected, "+rtok);
         }
         x = rtok.nval;
         if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
            throw new IOException ("knot y value expected, "+rtok);
         }
         y = rtok.nval;
         if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
            throw new IOException ("knot y derivative value expected, "+rtok);
         }
         dy = rtok.nval;
         addKnot (x, y, dy);
      }
   }

   private String segToString (Knot knot, NumberFormat fmt) {
      StringBuilder sb = new StringBuilder();
      sb.append (fmt.format(knot.x0));
      sb.append (' ');
      sb.append (fmt.format(knot.y0));
      sb.append (' ');
      sb.append (fmt.format(knot.dy0));
      return sb.toString();
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
         pw.println ("[ "+segToString(myKnots.get(0),fmt)+" ]");
      }
      else {
         pw.print ("[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (int i=0; i<myKnots.size(); i++) {
            pw.println (segToString(myKnots.get(i),fmt));
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

   public int numKnots() {
      return myKnots.size();
   }

   public Knot getKnot (int idx) {
      return myKnots.get (idx);
   }

   public Knot getFirstKnot () {
      if (myKnots.size() > 0) {
         return myKnots.get (0);
      }
      else {
         return null;
      }
   }

   public Knot getLastKnot () {
      if (myKnots.size() > 0) {
         return myKnots.get (numKnots()-1);
      }
      else {
         return null;
      }
   }

   public void updateCoefficients (Knot knot) {
      int idx = knot.getIndex();
      if (idx > 0) {
         Knot k = myKnots.get(idx-1);
         myKnots.get (idx-1).computeCoefficients (knot);
      }
      if (idx >= 0 && idx < numKnots()-1) {
         knot.computeCoefficients (myKnots.get (idx+1));
      }
   }

   public void updateCoefficients () {
      for (int i=0; i<numKnots()-1; i++) {
         Knot k = myKnots.get(i);
         myKnots.get(i).computeCoefficients (myKnots.get(i+1));
      }
   }

   public Iterator<Knot> iterator() {
      return new MyIterator();
   }

   /**
    * Sets this spline to be a copy of another spline.
    *
    * @param spline spline to copy
    */
   public void set (CubicHermiteSpline1d spline) {
      clearKnots();
      for (Knot knot : spline) {
         addKnot (myKnots.size(), knot.copy());
      }
   }

   /**
    * Scales the x values of this spline.
    *
    * @param s scale factor
    */
   public void scaleX (double s) {
      for (Knot knot : this) {
         knot.x0 *= s;
         knot.dy0 /= s;
      }
      updateCoefficients();
   }

   /**
    * Scales the y values of this spline.
    *
    * @param s scale factor
    */
   public void scaleY (double s) {
      for (Knot knot : this) {
         knot.y0 *= s;
         knot.dy0 *= s;
      }
      updateCoefficients();
   }

   /**
    * Creates a copy of this spline.
    */
   public CubicHermiteSpline1d copy () {
      CubicHermiteSpline1d spline = new CubicHermiteSpline1d();
      spline.set (this);
      return spline;
   }
}
