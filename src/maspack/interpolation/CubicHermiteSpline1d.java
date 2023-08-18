package maspack.interpolation;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import maspack.util.*;
import maspack.matrix.*;
import maspack.function.*;

/**
 * Implements one dimensional cubic Hermite spline interpolation in the {@code
 * x/y} plane.  The spline is described by a series of segments, each
 * specifying an interval of {@code x} within which {@code y} is interpolated
 * cubicly. The spline is defined by a sequence of knots delimiting the
 * boundaries of these segments, with each knot specifying {@code y} and its
 * derivative for a given value of {@code x}.
 */
public class CubicHermiteSpline1d extends Diff1Function1x1Base
   implements Iterable<CubicHermiteSpline1d.Knot> {

   private static final double EPS = 1e-14;

   // cached variable describing if this curve is invertible.
   // 1 yes, 0 no, -1 don't know
   private int myInvertible = -1;

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
         set (x0, y0, dy0);
      }

      public void set (double x0, double y0, double dy0) {
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

      public double evalDeriv2 (double x) {
         // interpolation is done locally, with respect to x - x0
         x -= x0;
         if (x < 0) {
            return 0;
         }
         else {
            return 6*a3*x + 2*a2;
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

      public double getY(double alpha) {
         return y0 + x0*alpha;
      }

      public double getDy() {
         return dy0;
      }

      public double getDy2() {
         return 2*a2;
      }

      public double getDy3() {
         return 6*a3;
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

      public double getA2() {
         return a2;
      }
      
      public double getA3() {
         return a3;
      }
      
      /**
       * Returns true if y'(x) contain a zero within this knot interval
       * (including at the interval end points).
       */
      public boolean hasZeroDy (Knot next) {
         double[] roots = new double[2];
         int nr = QuadraticSolver.getRoots (
            roots, 3*a3, 2*a2, dy0, 0, next.getX()-getX());
         if (nr != 0) {
            return true;
         }
         // extra cautious: check dy values at endpoints too
         if (dy0 == 0 || next.dy0 == 0) {
            return true;
         }
         return false;
      }

      /**
       * Returns true if y(x) is strictly monotone on the interval between this
       * knot and the next.
       */
      public boolean isStrictlyMonotone (Knot next) {
         double dely = next.y0-y0;
         double h = next.x0-x0;
         if (dely == 0 || h == 0) {
            return false;
         }
         if (dy0 == 0 && next.dy0 == 0) {
            // zero derivatives at the end points
            return true;
         }
         else if (dy0 == 0) {
            // have a root at 0; look for the other
            if (a3 != 0) {
               double root = -2*a2/3*a3;
               if (root < 0 || root > h) {
                  return true; // other root not in the interval
               }
            }
         }
         else if (next.dy0 == 0) {
            // have a root at h. Other root is at -dy0*h/(6*dely-3*dy0).
            double denom = 6*dely-3*dy0;
            if (denom != 0) {
               double root = -dy0*h/denom;
               if (root < 0 || root > h) {
                  return true; // other root not in the interval
               }
            }
         }
         else {
            double[] roots = new double[2];
            int nr = QuadraticSolver.getRoots (roots, 3*a3, 2*a2, dy0, 0, h);
            if (nr == 0) {
               return true; // no other root in the interval
            }
         }
         return false;
      }

      /**
       * Solves for x given a value of y associated with this interval.  It is
       * assumed that we have already determined that a unqiue solution exists.
       * If by some chance multiple roots are found, we return the first.
       */
      public double solveX (double y, Knot next) {
         if (next == null) {
            // just x0, y0 and dy0 values
            if (dy0 == 0) {
               return x0;
            }
            else {
               return x0 + (y-y0)/dy0;
            }
         }
         else if (dy0 == 0 && y == y0) {
            return x0;
         }
         else if (next.dy0 == 0 && y == next.y0) {
            return next.x0;
         }
         else {
            double[] roots = new double[3];
            CubicSolver.getRoots (roots, a3, a2, dy0, y0-y, 0.0, next.x0-x0);
            return roots[0] + x0;
         }
      }

      /**
       * Solves for x given a value of y associated with this interval.  {@code
       * alpha} defines an additional linear term {@code alpha x} that should
       * be added to the segment's value. It is assumed that we have already
       * determined that a unqiue solution exists.  If by some chance multiple
       * roots are found, we return the first.
       */
      public double solveX (double y, double alpha, Knot next) {
         double a0 = y0 + alpha*x0;
         double a1 = dy0 + alpha;
         if (next == null) {
            // just x0, a0 and a1 values. a1 won't be zero because
            // alpha != 0 and must have the same sign as dy0
            return x0 + (y-a0)/a1;
         }
         double dely = next.y0 - y0;
         if (dy0 == 0 && Math.abs((y-a0)/dely) <= EPS) {
            return x0;
         }
         else if (next.dy0 == 0 &&
                  Math.abs((y-(next.y0+next.x0*alpha)/dely)) <= EPS) {
            return next.x0;
         }
         else {
            double[] roots = new double[3];
            CubicSolver.getRoots (roots, a3, a2, a1, a0-y, 0.0, next.x0-x0);
            return roots[0] + x0;
         }
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
         return knot;
      }
      
      public int getIndex() {
         return myIndex;
      }

      public void setIndex(int idx) {
         myIndex = idx;
      }

      public String toString () {
         return "x0="+x0+" y0="+y0+" dy0="+dy0+" a2="+a2+" a3="+a3;
      }

      public boolean equals (Knot knot) {
         return (x0 == knot.x0 &&
                 y0 == knot.y0 &&
                 dy0 == knot.dy0 &&
                 a2 == knot.a2 &&
                 a3 == knot.a3);

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
      myKnots.clear();
      myInvertible = -1;
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
    * Sets this spline to a natural cubic spline for a set of knot points whose
    * x and y positions are given. This is a wrapper for {@link
    * #setNatural(double[],double[],double,double)} with {@code ddy0} and
    * {@code ddyL} both set to 0.
    *
    * @param x knot point x values
    * @param y knot point y values
    */
   public void setNatural (double[] x, double[] y) {
      setNatural (x, y, 0, 0);
   }

   /**
    * Sets this spline to a natural cubic spline for a set of knot points whose
    * x and y positions are given. The implementation is based on a some notes
    * published by James Keesling, in
    * <a href="https://people.clas.ufl.edu/kees/files/CubicSplines.pdf">
    * people.clas.ufl.edu/kees/files/CubicSplines.pdf</a>
    *
    * @param x knot point x values
    * @param y knot point y values
    * @param ddy0 desired second derivative at the first knot
    * @param ddyL desired second derivative at the last knot
    */
   public void setNatural (
      double[] x, double[] y, double ddy0, double ddyL) {

      myKnots.clear();
      myInvertible = -1;
      if (x.length != y.length) {
         throw new IllegalArgumentException (
            "x and y inputs have different lengths");
      }
      int numk = x.length;
      if (numk == 0) {
         return;
      }
      else if (numk == 1) {
         // constant value
         myKnots.add (new Knot (x[0], y[0], 0));
         myInvertible = 0;
      }
      else if (numk == 2) {
         // linear function
         if (x[1] == x[0]) {
            System.out.println ("x: " + new VectorNd(x));
            System.out.println ("y: " + new VectorNd(y));
            throw new IllegalArgumentException (
               "x inputs at 0 and 1 are the same");
         }
         double slope = (y[1]-y[0])/(x[1]-x[0]);
         myKnots.add (new Knot (x[0], y[0], slope));
         myKnots.add (new Knot (x[1], y[1], slope));
      }
      else {
         double[] dx = new double[numk-1];
         double[] dy = new double[numk-1];
         for (int i=0; i<numk-1; i++) {
            dx[i] = x[i+1] - x[i];
            dy[i] = y[i+1] - y[i];
            if (dx[i] == 0) {
            System.out.println ("x: " + new VectorNd(x));
            System.out.println ("y: " + new VectorNd(y));
               throw new IllegalArgumentException (
                  "x inputs at "+i+" and "+(i+1)+" are the same");
            }
         }
         MatrixNd M = new MatrixNd (numk, numk);
         VectorNd r = new VectorNd (numk); // right hand side
         M.set (0, 0, 1);
         r.set (0, ddy0/2);
         for (int k=1; k<numk-1; k++) {
            double hp = dx[k-1]; // hk previous
            double hk = dx[k];
            M.set (k, k-1, hp);
            M.set (k, k, 2*(hp+hk));
            M.set (k, k+1, hk);
            r.set (k, 3*(dy[k])/hk - 3*(dy[k-1])/hp);
         }
         M.set (numk-1, numk-1, 1);
         r.set (numk-1, ddyL/2);

         LUDecomposition LUD = new LUDecomposition (M);
         VectorNd z = new VectorNd (numk); // a2 coefficients to solve for
         if (!LUD.solve (z, r)) {
            throw new IllegalArgumentException (
               "unable to solve for spline - x/y inputs are ill-conditioned");
         }
         for (int k=0; k<numk; k++) {
            double a2 = z.get(k);
            double a3 = 0;
            double Dy = 0; // derivative of y at knot
            if (k < numk-1) {
               double hk = dx[k];
               double a2next = z.get(k+1);
               a3 = (a2next - a2)/(3*hk);
               Dy = dy[k]/hk - hk*(2*a2 + a2next)/3;
            }
            else {
               double hp = dx[k-1];
               Knot prev = myKnots.get(k-1);
               Dy = prev.dy0 + (3*prev.a3*hp + 2*prev.a2)*hp;
               a3 = 0;
            }
            Knot knot = new Knot (x[k], y[k], Dy);
            knot.a2 = a2;
            knot.a3 = a3;
            myKnots.add (knot);
         }
         updateCoefficients();
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
         doAddKnot (0, knot);
      }
      else {
         if (prev.x0 == x) {
            // just replace the existing knot
            myKnots.set (prev.getIndex(), knot);
            knot.setIndex (prev.getIndex());
         }
         else {
            doAddKnot (prev.getIndex()+1, knot);
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

   protected void doAddKnot (int idx, Knot knot) {
      myKnots.add (idx, knot);
      reindexFrom (idx);
      myInvertible = -1;
   }

   public void addKnot (int idx, Knot knot) {
      doAddKnot (idx, knot);
      updateCoefficients (knot);
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
         myInvertible = -1;
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
      myInvertible = -1;
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
    * Evaluates the y value for this spline at a specific x value. Values which
    * are outside the specified knot range are extrapolated based on y and dy
    * values at the end point knots. An empty spline evaluates to 0.
    *
    * @param x value at which y should be evaluated
    */
   public double evalY (double x) {
      if (numKnots() == 0) {
         return 0;
      }
      Knot prev = getPreviousKnot (x);
      if (prev == null) {
         prev = myKnots.get(0);
      }
      return prev.eval (x);
   }

   /**
    * Evaluates the y value of the function composed of this spline plus a linear
    * term {@code alpha x} at a specific x value. Values which are outside
    * the specified knot range are extrapolated based on y and dy values at the
    * end point knots. An empty spline evaluates to 0.
    *
    * @param x value at which y should be evaluated
    * @param alpha slope of the additional linear term
    */
   public double evalY (double x, double alpha) {
      if (numKnots() == 0) {
         return alpha*x;
      }
      Knot prev = getPreviousKnot (x);
      if (prev == null) {
         prev = myKnots.get(0);
      }
      return prev.eval(x) + alpha*x;
   }

   /**
    * Evaluates the y derivative value at a specific x value. Values
    * which are outside the specified knot range are extrapolated based on y
    * and dy values at the end point knots. An empty spline evaluates to 0.
    *
    * @param x value at which the y derivative should be evaluated
    */
   public double evalDy (double x) {
      if (numKnots() == 0) {
         return 0;
      }
      Knot prev = getPreviousKnot (x);
      if (prev == null) {
         prev = myKnots.get(0);
      }
      return prev.evalDeriv (x);
   }
   
   /**
    * Evaluates the y derivative of the function composed of this spline plus a
    * linear term {@code alpha x} at a specific x value. Values which are
    * outside the specified knot range are extrapolated based on y and dy
    * values at the end point knots. An empty spline evaluates to 0.
    *
    * @param x value at which the y derivative should be evaluated
    * @param alpha slope of the additional linear term
    */
   public double evalDy (double x, double alpha) {
      if (numKnots() == 0) {
         return alpha;
      }
      Knot prev = getPreviousKnot (x);
      if (prev == null) {
         prev = myKnots.get(0);
      }
      return prev.evalDeriv (x) + alpha;
   }
   
   /**
    * Evaluates the second y derivative value at a specific x value. Values
    * which are outside the specified knot range are extrapolated as 0.
    *
    * @param x value at which the second y derivative should be evaluated
    */
   public double evalDy2 (double x) {
      if (numKnots() == 0) {
         return 0;
      }
      Knot prev = getPreviousKnot (x);
      if (prev == null) {
         prev = myKnots.get(0);
      }
      else if (prev == myKnots.get(numKnots()-1)) {
         // need previous knot because last one doesn't define dy2
         prev = myKnots.get(numKnots()-2);
      }
      return prev.evalDeriv2 (x);
   }
   
   /**
    * Evaluates the third y derivative value spline at a specific x
    * value. Values which are outside the specified knot range are extrapolated
    * as 0.
    *
    * @param x value at which the second y derivative should be evaluated
    */
   public double evalDy3 (double x) {
      if (numKnots() == 0) {
         return 0;
      }
      Knot prev = getPreviousKnot (x);
      if (prev == null) {
         prev = myKnots.get(0);
      }
      else if (prev == myKnots.get(numKnots()-1)) {
         // need previous knot because last one doesn't define dy3
         prev = myKnots.get(numKnots()-2);
      }
      return prev.getDy3();
   }
   
   /* ---- Function interface ---- */

   public double eval (double x) {
      return evalY (x);
   }
   
   public double eval (DoubleHolder deriv, double x) {
      if (deriv != null) {
         deriv.value = evalDy (x);
      }
      return evalY (x);
   }

   public double evalDeriv (double x) {
      return evalDy (x);
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
      if (myKnots.size() == 1) {
         pw.println ("[ "+segToString(myKnots.get(0),fmt)+" ]");
      }
      else {
         pw.println ("[ ");
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
         doAddKnot (myKnots.size(), knot.copy());
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
    * Queries whether this spline is invertible. We use a strict definition:
    * the knot y values must be stictly monotonic, and y'(s) must not be zero
    * anywhere. The spline must also have at least one knots.
    *
    * @return {@code true} if this spline is invertible
    */
   public boolean isInvertible() {
      if (numKnots() < 1) {
         return false;
      }
      else {
         if (myInvertible == -1) {
            checkInvertible();
         }
         return myInvertible == 1;
      }
   }

   /**
    * Computes whether or not this curve is invertible
    */
   private void checkInvertible() {
      if (numKnots() < 1) {
         myInvertible = -1;
      }
      else if (numKnots() == 1) {
         myInvertible = (myKnots.get(0).getDy() != 0 ? 1 : 0);
      }
      else {
         double y0 = myKnots.get(0).getY();
         for (int i=0; i<numKnots()-1; i++) {
            Knot knot = myKnots.get(i);
            Knot next = myKnots.get(i+1);
            double dely = next.getY()-knot.getY();
            double dely0 = next.getY()-y0;
            // not invertible if segment is not strictly monotone or
            // direct has changed
            if (dely*dely0 < 0 || !knot.isStrictlyMonotone(next)) {
               myInvertible = 0;
               return;
            }
         }
         myInvertible = 1;
      }
   }

   /**
    * Solves for the x value given a specific y value.  In order to do this,
    * the spline must be invertible. Y values outside the nominal range are
    * extrapolated based on the x and 1/dy values at the end knots.
    *
    * @param y value for which x should be solved
    * @throws ImproperStateException if the spline is not invertible
    * @throws IllegalArgumentException if the spline is invertible
    * but y is out of range
    */
   public double solveX (double y) {
      if (!isInvertible()) {
         throw new ImproperStateException (
            "spline is not invertible");
      }
      if (numKnots() == 1) {
         // just use first knot
         return myKnots.get(0).solveX (y, null);
      }
      else {
         // y direction: 1 if y increases with knots, -1 if it decreases
         Knot first = myKnots.get(0);
         Knot last = myKnots.get(numKnots()-1);
         int ydir = (last.getY() > first.getY()) ? 1 : -1;

         if ((ydir == 1 && y <= first.getY()) || 
             (ydir == -1 && y >= first.getY())) {
            if (first.dy0 == 0) {
               if (y != first.getY()) {
                  throw new IllegalArgumentException (
                     "y value "+y+" out of range; first y value is "+first.y0);
               }
               return first.x0;
            }
            else {
               // extrapolate solution from first knot
               return first.solveX (y, null);
            }
         }
         else if ((ydir == 1 && y >= last.getY()) || 
                  (ydir == -1 && y <= last.getY())) {
            if (last.dy0 == 0) {
               if (y != last.getY()) {
                  throw new IllegalArgumentException (
                     "y value "+y+" out of range; last y value is "+last.y0);
               }
               return last.x0;
            }
            else {
               // extrapolate solution from last knot               
               return last.solveX (y, null);
            }
         }
         else {
            for (int i=0; i<numKnots()-1; i++) {
               Knot knot = myKnots.get(i);
               Knot next = myKnots.get(i+1);
               if ((ydir == 1 && y >= knot.getY() && y <= next.getY()) ||
                   (ydir == -1 && y <= knot.getY() && y >= next.getY())) {
                  return knot.solveX (y, next);
               }
            }
            throw new InternalErrorException (
               "Interval for y=" + y +
               " not found, even though spline is invertible");
         }
      }
      
   }

  /**
    * Solves for the x value given a specific y value of the function defined
    * by this spline plus a linear term {@code alpha x}. In order to do this,
    * the spline must be invertible, and the sign of {@code alpha} must be
    * consistent with its monotonicity. Y values outside the nominal range are
    * extrapolated based on the x and 1/dy values at the end knots.
    *
    * @param y value for which x should be solvde
    * @param alpha slope of the additional linear term
    * @throws ImproperStateException if the spline is not invertible
    * @throws IllegalArgumentException if the sign of {@code alpha}
    * is inconsistent with the spline's monotonicity
    */
   public double solveX (double y, double alpha) {
      if (!isInvertible()) {
         throw new ImproperStateException (
            "spline is not invertible");
      }
      if (alpha == 0) {
         return solveX (y);
      }
      if (numKnots() == 1) {
         // just use first knot
         return myKnots.get(0).solveX (y, alpha, null);
      }
      else {
         // y direction: 1 if y increases with knots, -1 if it decreases
         Knot first = myKnots.get(0);
         Knot last = myKnots.get(numKnots()-1);
         int ydir = (last.getY() > first.getY()) ? 1 : -1;
         if (alpha*ydir < 0) {
            throw new IllegalArgumentException (
               "sign of alpha incompatible with the monotonicity of the spline");
         }
         if ((ydir == 1 && y <= first.getY(alpha)) || 
             (ydir == -1 && y >= first.getY(alpha))) {
            // extrapolate solution from first knot
            return first.solveX (y, alpha, null);
         }
         else if ((ydir == 1 && y >= last.getY(alpha)) || 
                  (ydir == -1 && y <= last.getY(alpha))) {
            // extrapolate solution from last knot
            return last.solveX (y, alpha, null);
         }
         else {
            for (int i=0; i<numKnots()-1; i++) {
               Knot knot = myKnots.get(i);
               Knot next = myKnots.get(i+1);
               if ((ydir == 1 && y >= knot.getY(alpha) &&
                    y <= next.getY(alpha)) ||
                   (ydir == -1 && y <= knot.getY(alpha) &&
                    y >= next.getY(alpha))) {
                  return knot.solveX (y, alpha, next);
               }
            }
            throw new InternalErrorException (
               "Interval for y=" + y +
               " not found, even though spline is invertible");
         }
      }
      
   }

   /**
    * Creates a copy of this spline.
    */
   public CubicHermiteSpline1d copy () {
      CubicHermiteSpline1d spline = new CubicHermiteSpline1d();
      spline.set (this);
      return spline;
   }

   public boolean equals (CubicHermiteSpline1d curve) {
      if (numKnots() != curve.numKnots()) {
         return false;
      }
      for (int i=0; i<numKnots(); i++) {
         if (!getKnot(i).equals (curve.getKnot(i))) {
            return false;
         }
      }
      return true;
   }

   public CubicHermiteSpline1d clone() {
      CubicHermiteSpline1d spline = (CubicHermiteSpline1d)super.clone();
      spline.set (this);
      return spline;
   }
   

}
