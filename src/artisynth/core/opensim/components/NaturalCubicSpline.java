package artisynth.core.opensim.components;

import java.util.List;

import maspack.matrix.MatrixNd;
import maspack.matrix.Point2d;
import maspack.matrix.QRDecomposition;
import maspack.matrix.VectorNd;

/**
 * Simple 2D Natural Cubic Spline for interpolation in OpenSim Models
 */
public class NaturalCubicSpline extends FunctionBase {

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

   private static class SplineSegment {

      double a, b, c, d;
      double[] xrange;
      double[] yrange;
      Point2d[] ipnts;

      public SplineSegment(double x1, double y1, double x2, double y2, double z1,
         double z2) {
         double h = x2 - x1;
         a = (z2 - z1) / (6 * h);
         b = (x2 * z1 - x1 * z2) / (2 * h);
         c =
            (3 * (x1 * x1 * z2 - x2 * x2 * z1) + h * h * (z1 - z2) + 6 * (y2 - y1))
            / (6 * h);
         d =
            ((x2 * x2 * x2 * z1 - x1 * x1 * x1 * z2) + h * h
            * (x1 * z2 - x2 * z1) + 6 * (x2 * y1 - x1 * y2))
            / (6 * h);
         xrange = new double[] { x1, x2 };
         updateRangeAndPoints();
      }
      
      private void updateRangeAndPoints() {
         yrange =
            new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };

         // endpoints
         double y0 = eval(xrange[0]);
         if (y0 < yrange[0]) {
            yrange[0] = y0;
         }
         if (y0 > yrange[1]) {
            yrange[1] = y0;
         }

         double y1 = eval(xrange[1]);
         if (y1 < yrange[0]) {
            yrange[0] = y1;
         }
         if (y1 > yrange[1]) {
            yrange[1] = y1;
         }

         // maxima
         if (a == 0) {
            if (b != 0) {
               double x = -c / (2 * b);
               if (x > xrange[0] && x < xrange[1]) {
                  double y = eval(x);
                  if (y < yrange[0]) {
                     yrange[0] = y;
                  }
                  if (y > yrange[1]) {
                     yrange[1] = y;
                  }

                  ipnts = new Point2d[3];
                  ipnts[0] = new Point2d(xrange[0], y0);
                  ipnts[1] = new Point2d(x, y);
                  ipnts[2] = new Point2d(xrange[1], y1);
               }
            }
         } else {
            double r = b * b - 3 * a * c;
            if (r > 0) {
               // potentially 2 solutions
               r = Math.sqrt(r);
               double x1 = (-b - r) / (3 * a);
               double x2 = (-b + r) / (3 * a);

               if (x1 > xrange[0] && x1 < xrange[1]) {
                  if (x2 < xrange[1]) {
                     // both valid
                     double dy1 = eval(x1);
                     if (dy1 < yrange[0]) {
                        yrange[0] = dy1;
                     }
                     if (dy1 > yrange[1]) {
                        yrange[1] = dy1;
                     }

                     double dy2 = eval(x2);
                     if (dy2 < yrange[0]) {
                        yrange[0] = dy2;
                     }
                     if (dy2 > yrange[1]) {
                        yrange[1] = dy2;
                     }
                     ipnts = new Point2d[4];
                     ipnts[0] = new Point2d(xrange[0], y0);
                     ipnts[1] = new Point2d(x1, dy1);
                     ipnts[2] = new Point2d(x2, dy2);
                     ipnts[3] = new Point2d(xrange[1], y1);

                  } else {
                     // x1 valid
                     double dy1 = eval(x1);
                     if (dy1 < yrange[0]) {
                        yrange[0] = dy1;
                     }
                     if (dy1 > yrange[1]) {
                        yrange[1] = dy1;
                     }
                     ipnts = new Point2d[3];
                     ipnts[0] = new Point2d(xrange[0], y0);
                     ipnts[1] = new Point2d(x1, dy1);
                     ipnts[2] = new Point2d(xrange[1], y1);
                  }
               } else if (x2 > xrange[0] && x2 < xrange[1]) {
                  // x2 valid
                  double dy2 = eval(x2);
                  if (dy2 < yrange[0]) {
                     yrange[0] = dy2;
                  }
                  if (dy2 > yrange[1]) {
                     yrange[1] = dy2;
                  }
                  ipnts = new Point2d[3];
                  ipnts[0] = new Point2d(xrange[0], y0);
                  ipnts[1] = new Point2d(x2, dy2);
                  ipnts[2] = new Point2d(xrange[1], y1);
               }

            } else if (r == 0) {
               double x = -b / (3 * a);

               if (x > xrange[0] && x < xrange[1]) {
                  double y = eval(x);
                  if (y < yrange[0]) {
                     yrange[0] = y;
                  }
                  if (y > yrange[1]) {
                     yrange[1] = y;
                  }

                  ipnts = new Point2d[3];
                  ipnts[0] = new Point2d(xrange[0], y0);
                  ipnts[1] = new Point2d(x, y);
                  ipnts[2] = new Point2d(xrange[1], y1);
               }
            }
         }

         if (ipnts == null) {
            ipnts = new Point2d[2];
            ipnts[0] = new Point2d(xrange[0], y0);
            ipnts[1] = new Point2d(xrange[1], y1);
         }

      }

      private double binarySolveSegment(
         double xlow, double xhigh, double y, double eps) {
         double xmid = (xlow + xhigh) / 2;
         double ymid = eval(xmid);

         if (Math.abs(y - ymid) < eps) {
            return xmid;
         } else if (y > ymid) {
            xlow = xmid;
         } else {
            xhigh = xmid;
         }
         return binarySolveSegment(xlow, xhigh, ymid, eps);
      }

      public int solveForX(double y, List<Double> x, double eps) {
         int nx = 0;

         if (y >= yrange[0] && y <= yrange[1]) {
            // check each monotone segment
            for (int i = 0; i < ipnts.length - 1; i++) {
               if (ipnts[i].y <= y && y <= ipnts[i + 1].y) {
                  double xl =
                     binarySolveSegment(ipnts[i].x, ipnts[i + 1].x, y, eps);
                  if (Math.abs(xl - x.get(x.size() - 1)) > eps) {
                     x.add(xl);
                     nx++;
                  }
               } else if (ipnts[i+1].y <= y && y <= ipnts[i].y) {
                  double xl =
                     binarySolveSegment(ipnts[i+1].x, ipnts[i].x, y, eps);
                  if (Math.abs(xl - x.get(x.size() - 1)) > eps) {
                     x.add(xl);
                     nx++;
                  }
               }
            }
         }

         return nx;
      }

      public double solveForNearestX(double y, double nearestx, double eps) {

         double nearest = Double.NEGATIVE_INFINITY;
         double nearestDist = Double.POSITIVE_INFINITY;

         if (y >= yrange[0] && y <= yrange[1]) {
            // check each monotone segment
            for (int i = 0; i < ipnts.length - 1; i++) {
               if (ipnts[i].y <= y && y <= ipnts[i + 1].y) {
                  double xl =
                     binarySolveSegment(ipnts[i].x, ipnts[i + 1].x, y, eps);
                  double d = Math.abs(xl - nearestx);
                  if (d < nearestDist) {
                     nearestDist = d;
                     nearest = xl;
                  }
               } else if (ipnts[i + 1].y <= y && y <= ipnts[i].y) {
                  double xl = binarySolveSegment(ipnts[i + 1].x, ipnts[i].x, y, eps);
                  double d = Math.abs(xl - nearestx);
                  if (d < nearestDist) {
                     nearestDist = d;
                     nearest = xl;
                  }
               }
            }
         }

         return nearest;
      }

      public double eval(double x) {
         return a * x * x * x + b * x * x + c * x + d;
      }

      public double evalDerivative(double x) {
         return 3 * a * x * x + 2 * b * x + c;
      }

      public double evalDerivative(double x, int d) {
         if (d == 0) {
            return eval(x);
         } else if (d == 1) {
            return evalDerivative(x);
         } else if (d == 2) {
            return 6 * a * x + 2 * b;
         } else if (d == 3) {
            return 6 * a;
         }
         return 0;
      }
      
      @Override
      protected SplineSegment clone () {
         SplineSegment spline = null;
         try {
            spline = (SplineSegment)super.clone ();
            spline.xrange = new double[] {xrange[0], xrange[1]};
            spline.updateRangeAndPoints ();
         }
         catch (CloneNotSupportedException e) {
         }
         
         return spline;
      }
   }
  
   private Knot[] knots;
   private SplineSegment[] splines;

   public NaturalCubicSpline() {
      knots = null;
      splines = null;
   }
   
   public NaturalCubicSpline(double[] x, double[] y) {
      set(x, y);
   }

   private void setKnots(double[] x, double[] y) {
      int len = x.length;
      if (len == 1) {
         x = new double[] {x[0], x[0], x[0]};
         y = new double[] {y[0], y[0], y[0]};
         len = 3;
         len = 3;
      } else if (len == 2) {
         x = new double[] {x[0], (x[0]+x[1])/2, x[1]};
         y = new double[] {y[0], (y[0]+y[1])/2, y[1]};
         len = 3;
      }
      knots = new Knot[len];
      for (int i = 0; i < len; i++) {
         knots[i] = new Knot(x[i], y[i]);
      }
   }

   private int findSplineIdx(double x) {
      if (x <= knots[1].x) {
         return 0;
      } else if (x >= knots[knots.length - 2].x) {
         return splines.length - 1;
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
    * Verify that endspoints and endpoint derivatives match
    * @return true if match, false otherwise
    */
   public boolean validate() {

      double eps = 1e-6;
      for (int i = 0; i < splines.length - 1; i++) {
         SplineSegment spline = splines[i];

         double yh = spline.eval(knots[i].x);
         if (Math.abs(yh - knots[i].y) > eps) {
            return false;
         }

         yh = spline.eval(knots[i + 1].x);
         if (Math.abs(yh - knots[i + 1].y) > eps) {
            return false;
         }

      }

      for (int i = 0; i < splines.length - 2; i++) {
         // check that derivatives match
         for (int j = 0; j < 3; j++) {
            double ds1 = splines[i].evalDerivative(knots[i + 1].x, j);
            double ds2 = splines[i + 1].evalDerivative(knots[i + 1].x, j);
            if (Math.abs(ds1 - ds2) > eps) {
               return false;
            }
         }
      }

      return true;
   }
   
   /**
    * Sets the spline knot locations
    * @param x x location
    * @param y location
    */
   public void set(double[] x, double[] y) {
      setKnots(x, y);
      
      int len = knots.length;

      MatrixNd M = new MatrixNd(len - 2, len - 2);
      VectorNd u = new VectorNd(len - 2);
      VectorNd w = new VectorNd(len - 2);

      double[] h = new double[len - 1];
      double[] b = new double[len - 1];

      for (int i = 0; i < len - 1; i++) {
         h[i] = knots[i + 1].x - knots[i].x;
         b[i] = (knots[i + 1].y - knots[i].y) / h[i];
      }

      for (int i = 1; i < len - 2; i++) {
         M.set(i - 1, i - 1, 2 * (h[i] + h[i - 1]));
         M.set(i - 1, i, h[i]);
         M.set(i, i - 1, h[i]);
         u.set(i - 1, 6 * (b[i] - b[i - 1]));
      }
      M.set(len - 3, len - 3, 2 * (h[len - 2] + h[len - 3]));
      u.set(len - 3, 6 * (b[len - 2] - b[len - 3]));

      QRDecomposition qrd = new QRDecomposition(M);
      qrd.solve(w, u);

      double[] z = new double[len];
      z[0] = 0;
      z[len - 1] = 0;
      w.get(z, 1);

      splines = new SplineSegment[len - 1];
      for (int i = 0; i < len - 1; i++) {
         splines[i] =
            new SplineSegment(
               knots[i].x, knots[i].y, knots[i + 1].x, knots[i + 1].y, z[i],
               z[i + 1]);
      }
   }

   public double evaluate(VectorNd x) {
      int idx = findSplineIdx(x.get(0));
      return splines[idx].eval(x.get(0));
   }
   
   @Override
   public void evaluateDerivative (VectorNd x, VectorNd df) {
      int idx = findSplineIdx(x.get(0));
      df.set(0, splines[idx].evalDerivative(x.get(0)));
   }

   public int solveForX(double y, List<Double> x, double eps) {
      int nx = 0;
      for (SplineSegment s : splines) {
         nx += s.solveForX(y, x, eps);
      }
      return nx;
   }

   public double solveForNearestX(double fx, double x, double eps) {
      double nearest = Double.NEGATIVE_INFINITY;
      double nearestDist = Double.POSITIVE_INFINITY;
      for (SplineSegment s : splines) {
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
   public NaturalCubicSpline clone () {
      NaturalCubicSpline spline = (NaturalCubicSpline)super.clone ();
      
      // clone knots
      if (knots != null) {
         spline.knots = new Knot[knots.length];
         for (int i=0; i<knots.length; ++i) {
            spline.knots[i] = knots[i].clone();
         }
      }
      
      if (splines != null) {
         spline.splines = new SplineSegment[splines.length];
         for (int i=0; i<splines.length; ++i) {
            spline.splines[i] = splines[i].clone();
         }
      }
      
      return spline;
   }
   
   public static void main(String[] args) {

      double[] x = { 0.9, 1.3, 1.9, 2.1 };
      double[] y = { 1.3, 1.5, 1.85, 2.1 };

      NaturalCubicSpline spline = new NaturalCubicSpline(x, y);
      boolean valid = spline.validate();
      if (valid) {
         System.out.println("Splines seem valid");
      } else {
         System.out.println("ERROR!! Splines invalid!");
      }

   }

}
