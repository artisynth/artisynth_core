package artisynth.core.opensim.components;

import java.util.List;

import maspack.matrix.VectorNd;

/**
 * Simple 2D piecewise linear function for interpolation in OpenSim Models
 */
public class PiecewiseLinearFunction extends FunctionBase {

   private static class Knot {
      double x;
      double y;

      public Knot(double x, double y) {
         this.x = x;
         this.y = y;
      }
      
      public Knot clone() {
         try {
            return (Knot)super.clone ();
         }
         catch (CloneNotSupportedException e) {}
         
         return null;
      }
   }

   private static class LinearSegment {

      double[] xrange;
      double[] yrange;
      double m;
      double b;
      
      public LinearSegment(double x1, double y1, double x2, double y2) {
         xrange = new double[] { x1, x2 };
         yrange = new double[] { y1, y2 };
         m = (y2-y1)/(x2-x1);
         b = 0.5*((y1 - m*x1) + (y2 - m*x2));
      }
      
      public int solveForX(double y, List<Double> x, double eps) {
         int nx = 0;

         if (y >= yrange[0] && y <= yrange[1]) {
            // solve within linear segment
            double xx = (y -b ) / m;
            x.add (xx);
            ++nx;
         }

         return nx;
      }

      public double solveForNearestX(double y, double nearestx, double eps) {

         double nearest = Double.NEGATIVE_INFINITY;

         if (y >= yrange[0] && y <= yrange[1]) {
            nearest = (y -b ) / m;
         }

         return nearest;
      }

      public double eval(double x) {
         return m*x + b;
      }

      public double evalDerivative(double x) {
         return m;
      }
      
      @Override
      protected LinearSegment clone () {
         LinearSegment segment = null;
         try {
            segment = (LinearSegment)super.clone ();
            segment.xrange = new double[] {xrange[0], xrange[1]};
            segment.yrange = new double[] {yrange[0], yrange[1]};
         }
         catch (CloneNotSupportedException e) {
         }
         
         return segment;
      }
   }
  
   private Knot[] knots;
   private LinearSegment[] segments;

   public PiecewiseLinearFunction() {
      knots = null;
      segments = null;
   }
   
   public PiecewiseLinearFunction(double[] x, double[] y) {
      set(x, y);
   }

   private void setKnots(double[] x, double[] y) {
      int len = x.length;
      if (len == 1) {
         x = new double[] {x[0], x[0]};
         y = new double[] {y[0], y[0]};
         len = 2;
         len = 2;
      }
      knots = new Knot[len];
      for (int i = 0; i < len; i++) {
         knots[i] = new Knot(x[i], y[i]);
      }
   }

   private int findSegmentIdx(double x) {
      if (x <= knots[1].x) {
         return 0;
      } else if (x >= knots[knots.length - 2].x) {
         return segments.length - 1;
      }

      // binary search
      int ilower = 1;
      int iupper = knots.length - 3;

      int i = (ilower + iupper) / 2;

      while (ilower < iupper) {
         if (x < knots[i].x) {
            iupper = i - 1;
         } else if (x > knots[i + 1].x) {
            ilower = i + 1;
         } else {
            return i;
         }
         i = (ilower + iupper) / 2; // next mid-point
      }
      return i;

   }
   
   /**
    * Sets the knot locations
    * @param x x location
    * @param y location
    */
   public void set(double[] x, double[] y) {
      setKnots(x, y);
      
      int len = knots.length;
      segments = new LinearSegment[len - 1];
      for (int i = 0; i < len - 1; i++) {
         segments[i] =
            new LinearSegment(
               knots[i].x, knots[i].y, knots[i + 1].x, knots[i + 1].y);
      }
   }

   public double evaluate(VectorNd x) {
      int idx = findSegmentIdx(x.get(0));
      return segments[idx].eval(x.get(0));
   }
   
   @Override
   public void evaluateDerivative (VectorNd x, VectorNd df) {
      int idx = findSegmentIdx(x.get(0));
      df.set(0, segments[idx].evalDerivative(x.get(0)));
   }

   public int solveForX(double y, List<Double> x, double eps) {
      int nx = 0;
      for (LinearSegment s : segments) {
         nx += s.solveForX(y, x, eps);
      }
      return nx;
   }

   public double solveForNearestX(double fx, double x, double eps) {
      double nearest = Double.NEGATIVE_INFINITY;
      double nearestDist = Double.POSITIVE_INFINITY;
      for (LinearSegment s : segments) {
         double xn = s.solveForNearestX(fx, x, eps);
         double d = Math.abs(xn-x);
         if (d < nearestDist) {
            nearest = xn;
            nearestDist = d;
         }
      }
      return nearest;
   }

   @Override
   public PiecewiseLinearFunction clone () {
      PiecewiseLinearFunction func = (PiecewiseLinearFunction)super.clone ();
      
      // clone knots
      if (knots != null) {
         func.knots = new Knot[knots.length];
         for (int i=0; i<knots.length; ++i) {
            func.knots[i] = knots[i].clone();
         }
      }
      
      if (segments != null) {
         func.segments = new LinearSegment[segments.length];
         for (int i=0; i<segments.length; ++i) {
            func.segments[i] = segments[i].clone();
         }
      }
      
      return func;
   }

}
