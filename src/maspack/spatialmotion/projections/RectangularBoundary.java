package maspack.spatialmotion.projections;

import maspack.matrix.ImproperSizeException;
import maspack.matrix.Point2d;
import maspack.matrix.VectorNd;

public class RectangularBoundary implements BoundaryCurve2D {

   VectorNd myParams;
   double EPSILON = 1e-14;

   public RectangularBoundary(VectorNd params) {
      if (params.size() != getNumParams()) {
         throw new ImproperSizeException("This rectangular boundary requires 4 parameters");
      }
      myParams = new VectorNd(params);
   }
   
   public RectangularBoundary () {
      myParams = new VectorNd(4);
   }

   public static boolean isWithin(double X, double Y, double a, double b) {

      if ((Math.abs(X) > a) | (Math.abs(Y) > b)) {
         return false;
      }
      return true;

   }

   public boolean isWithin(double X, double Y) {

      double[] buf = myParams.getBuffer();

      // check which quadrant
      if (X >= 0) {
         if (Y >= 0) {
            return isWithin(X, Y, buf[1], buf[3]);
         } else {
            return isWithin(X, Y, buf[1], buf[2]);
         }
      } else {
         if (Y <= 0) {
            return isWithin(X, Y, buf[0], buf[2]);
         } else {
            return isWithin(X, Y, buf[0], buf[3]);
         }
      }

   }

   public double getLength() {

      double[] buf = myParams.getBuffer();
      double len = 0;

      for (int i = 0; i < myParams.size(); i++) {
         len += 2 * buf[i];
      }

      return len;
   }

   // t from 0 to 1
   public Point2d getPoint(double t) {
      double [] out = new double[2];
      getPoint(t, out);
      return new Point2d(out);
   }

   private double modRange(double val, double min, double max) {

      double range = max - min;
      while (val > max) {
         val -= range;
      }
      while (val < min) {
         val += range;
      }
      return val;

   }

   public double getTVar(double X, double Y) {
      
      double len = getLength();
      double t = 0;
      double a = myParams.get(0);
      double b = myParams.get(1);
      double c = myParams.get(2);
      double d = myParams.get(3);
      
      // get quadrant
      if ( (X >= 0) ) {
         if (Y >= 0) {
            // quad 1
            
            if (Y > d-EPSILON ) {
               t = d + (b-X);
            } else {
               t = Y;
            }
            
            
         } else {
            // quad 4
            if (Y < -c+EPSILON) {
               t = len-c-b+X;
            } else {
               t = len+Y;
            }
         }
         
      } else {
         if (Y >= 0) {
            // quad 2
            
            if (Y>d-EPSILON) {
               t = d + b -X;
            } else {
               t = 2*d + b + a-Y; 
            }

         } else {
            // quad 3
            if (Y< -c+EPSILON) {
               t = 2*d + b + 2*a + c + X;
            } else {
               t= 2*d + b + a - Y;
            }

         }
      }
      
      return t/len;
   }
   
   public void getPoint(double t, double[] out) {

      double X = 0;
      double Y = 0;
      double buf[] = myParams.getBuffer();
      double len = getLength();

      double a = buf[0];
      double b = buf[1];
      double c = buf[2];
      double d = buf[3];

      t = modRange(t, 0, 1) * len;

      if (t <= d) {
         X = b;
         Y = t;
      } else if (t <= d + b + a) {
         X = b + d - t;
         Y = d;
      } else if (t <= 2 * d + b + a + c) {
         X = -a;
         Y = 2 * d + b + a - t;
      } else if (t <= len - c) {
         X = t - len + c + b;
         Y = -c;
      } else {
         X = b;
         Y = t-len;
      }

      out[0] = X;
      out[1] = Y;
   }
   
   public void getTangent(double t, double[] out) {

      double X = 0;
      double Y = 0;
      double buf[] = myParams.getBuffer();
      double len = getLength();

      double a = buf[0];
      double b = buf[1];
      double c = buf[2];
      double d = buf[3];

      t = modRange(t, 0, 1) * len;

      if (t <= d) {
         X = 0;
         Y = 1;
      } else if (t <= d + b + a) {
         X = -1;
         Y = 0;
      } else if (t <= 2 * d + b + a + c) {
         X = 0;
         Y = -1;
      } else if (t <= len - c) {
         X = 1;
         Y = 0;
      } else {
         X = 0;
         Y = 1;
      }

      out[0] = X*len;
      out[1] = Y*len;
   }

   public void setParameters(VectorNd params) {
      if (params.size() != getNumParams()) {
         throw new ImproperSizeException("Wrong number of parameters");
      }
      myParams.set(params);
   }

   public VectorNd getParameters() {
      return myParams;
   }
   
   public int getNumParams() {
      return 4;
   }

   public CurveType getType() {
      return CurveType.RECTANGULAR;
   }
   
   public void projectToBoundary(double [] in, double [] out) {

      // project to within
      projectToBoundaryFromWithout(in, out);
      
      double X = out[0];
      double Y = out[1];

      double[] buf = myParams.getBuffer();
      double a = buf[0];
      double b = buf[1];
      double c = buf[2];
      double d = buf[3];
      
      // project "out" to closest boundary
      // candidates are (X,-c), (X,d), (-a,Y), (b,Y)
      double d2 = dist2(X, Y, X,-c);
      double bestDist2 = d2;
      out[1] = -c;
      
      d2 = dist2(X,Y,X,d);
      if (d2 < bestDist2) {
         bestDist2 = d2;
         out[1] = d;
      }
      
      d2 = dist2(X,Y,-a,Y);
      if (d2 < bestDist2) {
         bestDist2 = d2;
         out[0] = -a;
         out[1] = Y;
      }
      
      d2 = dist2(X,Y,b,Y);
      if (d2 < bestDist2) {
         bestDist2 = d2;
         out[0] = b;
         out[1] = Y;
      }
      
//      // project radially
//      if (X >= 0 && Y >= 0) {
//         
//         if (b*Y > d*X){
//            out[0] = d*X/Y;
//            out[1] = d;
//         } else if ( X > EPSILON) {
//            out[0] = b;
//            out[1] = b*Y/X;
//         } else {
//            out[0] = b;
//            out[1] = 0;
//         }
//         
//      } else if ( X < 0 && Y >=0) {
//         
//         if ( a*Y > -d*X ) {
//            out[0] = d*X/Y;
//            out[1] = d;
//         } else {
//            out[0] = -a;
//            out[1] = -a*Y/X;
//         }
//         
//      } else if ( X < 0 && Y<=0 ) {
//         
//         if ( a*Y < c*X) {
//            out[0] = -c*X/Y;
//            out[1] = -c;
//         } else {
//            out[0] = -a;
//            out[1] = -a*Y/X;
//         }
//         
//      } else {
//         
//         if ( -b*Y > c*X ) {
//            out[0] = -c*X/Y;
//            out[1] = -c;
//         } else {
//            out[0] = b;
//            out[1] = b*Y/X;
//         }
//         
//      }      
      
   }
   
   private double dist2(double p1x, double p1y, double p2x, double p2y) {
      return (p1x-p2x)*(p1x-p2x)+(p1y-p2y)*(p1y-p2y);
   }
   
   private void projectToBoundaryFromWithout(double[] in, double[] out) {
      
      double X = in[0];
      double Y = in[1];
      
      double[] buf = myParams.getBuffer();
      double a = buf[0];
      double b = buf[1];
      double c = buf[2];
      double d = buf[3];
      
      // project to within
      if (in[0] > b) {
         out[0] = b;
      } else if ( X < -a) {
         out[0] = -a;
      } else {
         out[0] = X;
      }
      
      if (Y > d) {
         out[1] = d;
      } else if ( Y < -c) {
         out[1] = -c;
      } else {
         out[1] = Y; 
      }
      
   }

   public boolean projectWithin(double[] in, double[] out) {
       
      if (isWithin(in[0], in[1])) {
         out[0] = in[0];
         out[1] = in[1];
         return true;
      }
      
      projectToBoundary(in, out);
      return false;
   }

}
