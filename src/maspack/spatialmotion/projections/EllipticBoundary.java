package maspack.spatialmotion.projections;

import maspack.matrix.ImproperSizeException;
import maspack.matrix.Point2d;
import maspack.matrix.VectorNd;

public class EllipticBoundary implements BoundaryCurve2D {

   VectorNd myParams;

   final static double EPSILON = 1e-15;
   final static int MAX_ITERS = 1000;


   public EllipticBoundary(VectorNd params) {

      if (params.size() != getNumParams()) {
         throw new ImproperSizeException("This elliptic boundary requires 4 parameters");
      }

      myParams = new VectorNd(params);
   }

   public EllipticBoundary () {
      myParams = new VectorNd(4);
   }

   public static boolean isWithin(double X, double Y, double a, double b) {

      double v = X * X / (a * a) + Y * Y / (b * b);
      if (v > 1) {
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

   private double ramanujanLength(double a, double b) {

      double h = Math.abs(a - b) / (a + b);
      return Math.PI * (a + b) * (1 + 3 * h / (10 + Math.sqrt(4 - 3 * h)));

   }

   public double getLength() {

      // Add the four quadrant arclengths
      // Ramanujan estimate
      double len = 0;
      double[] buf = myParams.getBuffer();

      len += ramanujanLength(buf[1], buf[3]);
      len += ramanujanLength(buf[1], buf[2]);
      len += ramanujanLength(buf[0], buf[2]);
      len += ramanujanLength(buf[0], buf[3]);
      len = len / 4;

      return len;
   }

   // t from 0 to 1
   public Point2d getPoint(double t) {
      double [] out = new double[2];
      getPoint(t, out);
      return new Point2d(out);
   }

   public double getTVar(double X, double Y) {

      double buf[] = myParams.getBuffer();
      double theta = 0;

      // get quadrant
      if ( (X >= 0) ) {
         if (Y >= 0) {
            // quad 1
            theta = Math.acos(X/buf[1]);
         } else {
            // quad 4
            theta = 2*Math.PI-Math.acos(X/buf[1]);
         }

      } else {
         if (Y >= 0) {
            // quad 2
            theta = Math.acos(X/buf[0]);
         } else {
            // quad 3
            theta = 2*Math.PI-Math.acos(X/buf[0]);
         }
      }

      return theta/2.0/Math.PI;

   }

   public static void getPoint(double t, double a, double b, double[] out) {
      out[0] = a*Math.cos(t);
      out[1] = b*Math.sin(t);
   }

   public void getPoint(double t, double [] out) {

      double X = 0;
      double Y = 0;
      double buf[] = myParams.getBuffer();

      t = 2 * t * Math.PI;

      if (t < Math.PI / 2) {
         X = buf[1] * Math.cos(t);
         Y = buf[3] * Math.sin(t);
      } else if (t < Math.PI) {
         X = buf[0] * Math.cos(t);
         Y = buf[3] * Math.sin(t);
      } else if (t < 3.0 / 2 * Math.PI) {
         X = buf[0] * Math.cos(t);
         Y = buf[2] * Math.sin(t);
      } else {
         X = buf[1] * Math.cos(t);
         Y = buf[2] * Math.sin(t);
      }

      out[0] = X;
      out[1] = Y;
   }

   public void getTangent(double t, double [] out) {

      double X = 0;
      double Y = 0;
      double buf[] = myParams.getBuffer();

      t = 2 * t * Math.PI;

      if (t < Math.PI / 2) {
         X = -buf[1] * Math.sin(t);
         Y = buf[3] * Math.cos(t);
      } else if (t < Math.PI) {
         X = -buf[0] * Math.sin(t);
         Y = buf[3] * Math.cos(t);
      } else if (t < 3.0 / 2 * Math.PI) {
         X = -buf[0] * Math.sin(t);
         Y = buf[2] * Math.cos(t);
      } else {
         X = -buf[1] * Math.sin(t);
         Y = buf[2] * Math.cos(t);
      }

      out[0] = 2*Math.PI*X;
      out[1] = 2*Math.PI*Y;
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
      return CurveType.ELLIPTIC;
   }

   /**
    * If in[] is outside curve, projects to boundary 
    */
   public boolean projectWithin(double[] in, double[] out) {

      if (isWithin(in[0], in[1])) {
         out[0] = in[0];
         out[1] = in[1];
         return true;
      }
      
      projectToBoundaryFromWithout(in, out);
      
      return false;

   }

//   private static double distObjective(double t, double a, double b, double [] x) {
//
//      double st = Math.sin(t);
//      double ct = Math.cos(t);
//      return a*x[0]*st-a*a*st*ct-x[1]*b*ct+b*b*st*ct;
//
//   }


   private static void fillSolution(double[] in, double a, double b, double t, double[] out, int offset) {
      out[offset]=a*a*in[0]/(t+a*a);
      out[offset+1] = b*b*in[1]/(t+b*b);
   }
   
   
   private static double F(double t, double a, double b, double u, double v) {
      double a2 = a*a;
      double b2 = b*b;
      
      return a2*u*u/((t+a2)*(t+a2)) + b2*v*v/((t+b2)*(t+b2))-1;
      
   }
   
   private static double dF(double t, double a, double b, double u, double v) {
      double a2 = a*a;
      double b2 = b*b;
      double da = (t+a2);
      double db = (t+b2);
      
      return -2*a2*u*u/(da*da*da)-2*b2*v*v/(db*db*db);
   }
   
   private static double newton(double t0, double [] bounds, double a, double b, double u, double v) {
      
      double t = t0;
      double f = F(t, a, b, u, v);
      double df = dF(t,a,b,u,v);
      double dt = f/df;
      int iters = 0;
      
      while (Math.abs(f)>EPSILON && Math.abs(dt)>EPSILON && iters < MAX_ITERS) {
         
         t = t-dt;
         
         if (t<bounds[0]) {
            t = bounds[0];
         } else if (t>bounds[1]) {
            t = bounds[1];
         }
         
         f= F(t,a,b,u,v);
         df = dF(t,a,b,u,v);
         dt = f/df;
         iters++;
                  
      }
      
      return t;
   }
   
   // bisection iterates to find a good starting point for newton's method
   private static double findGoodNewtonStart(double [] bounds, boolean moveRight, double a, double b, double u, double v) {
      
      double tMid = (bounds[0]+bounds[1])/2;
      double fMid = F(tMid,a,b,u,v);
      int iters = 0;
      
      while (fMid < 0 && iters < MAX_ITERS) {
         if (moveRight) {
            bounds[1] = tMid;
         } else {
            bounds[0] =  tMid;
         }
         tMid = (bounds[0]+bounds[1])/2;
         fMid = F(tMid,a,b,u,v);
         
         iters++;
         
      }
      
      if (moveRight) {
         bounds[0] = tMid;
      } else {
         bounds[1] =  tMid;
      }
      
      return tMid;
      
   }
   
   
   // special case when in[] lies on an axis
   private static int projectPerpSpecial(double [] in, double a, double b, int quadrant, double [] out) {

      double u = in[0];
      double v = in[1];
      double a2 = a*a;
      double b2 = b*b;
      
      int nSol = 0;
      
      int signx = 1;
      int signy = 1;
      
      switch(quadrant) {
         case 1:
            signx=-1;
            break;
         case 2:
            signx=-1;
            signy=-1;
            break;
         case 3:
            signy=-1;
            break;
         default:
      }
      
      if (Math.abs(u) < EPSILON) {
         // (0, +-b)
         out[0] = 0;
         out[1] = signy*b;
         nSol = 1;
         if (Math.abs(v) < EPSILON) {
            // (+-a, 0)
            out[2] = signx*a;
            out[3] = 0;
            nSol = 2;
         } else if (signy*Math.signum(v)>0) {
            // ( _ , b^2*v/(b^2-a^2) )
            out[3] = b2*v/(b2-a2);
            out[2] = signx*a*Math.sqrt(1-out[3]*out[3]/b2);
            nSol = 2;
         }
      } else {
         // (+-a, 0)
         out[0] = signx*a;
         out[1] = 0;
         nSol = 1;         
         if (signx*Math.signum(v) > 0) {
            // ( a^2*u/(a^2-b^2), _ )
            out[2] = a2*u/(a2-b2);
            out[3] = signy*b*Math.sqrt(1-out[2]*out[2]/a2);
            nSol = 2;
         }
         
      }
      
      return nSol;
   }
   
   // Newton's method to find all perpendiculars, then project to one in range
   public static int projectPerp(double [] in, double a, double b, int quadrant, double [] out) {
      
      double u = in[0];
      double v = in[1];
      
      if (Math.abs(u)<EPSILON || Math.abs(v) < EPSILON) {
         return projectPerpSpecial(in, a, b, quadrant, out);
      }
      
      double a2 = a*a;
      double b2 = b*b;
      double au23 = Math.pow(u*a,2.0/3.0);
      double bv23 = Math.pow(v*b,2.0/3.0);
      double c = -(a*a*(bv23)+b*b*(au23))/(au23+bv23);
      double fc = F(c,a,b,u,v);
      double t = 0; // used in newton iterates
      
      int type = 0;
      final int CENTER_AB=1;
      final int CENTER_BA=2;
      final int LEFT_AB=3;
      final int LEFT_BA=4;
      final int RIGHT_AB=5;
      final int RIGHT_BA=6;
      
      int signx = 1;
      int signy = 1;
      
      double [] bounds = {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};  
      
      switch (quadrant) {
         case 1:
            signx=-1;
            break;
         case 2:
            signx=-1;
            signy=-1;
            break;
         case 3:
            signy=-1;
            break;
         default:
      }
      
      if (signy*v >= 0 && signx*u >= 0) {
         if (a2>b2) {
            type = RIGHT_AB;
         } else {
            type = RIGHT_BA;
         }

      } else if (signy*v >= 0 && signx*u < 0) {
         if (a2 > b2) {
            return 0;
         }
         type = CENTER_BA;
      } else if (signy*v<0 && signx*u >=0) {
         if (a2 < b2) {
            return 0;
         }
         type = CENTER_AB;
      } else {
         if (a2 > b2) {
            type = LEFT_AB;
         } else {
            type = LEFT_BA;
         }
      }
      
      
      if (type == CENTER_AB || type == CENTER_BA) {
         
         if ( Math.abs(fc) < EPSILON ) {
            // single solution at c
            fillSolution(in, a, b, c, out, 0);
            return 1;
         } else if (fc > 0) {
            // no solution
            return 0;
            
         } else {
            
            // split at c, find two solutions
            if (type == CENTER_AB) {
               bounds[0] = -a2+EPSILON;
            } else {
               bounds[0] = -b2+EPSILON;
            }
            bounds[1] = c;
            
            t  = findGoodNewtonStart(bounds,true, a, b, u, v);
            t = newton(t, bounds, a, b, u, v);
            fillSolution(in, a, b, t, out, 0);
            
            bounds[0] = c;
            if (type == CENTER_AB) {
               bounds[1] = -b2-EPSILON;
            } else {
               bounds[1] = -a2-EPSILON;
            }
                        
            t  = findGoodNewtonStart(bounds, false, a, b, u, v);
            t = newton(t, bounds, a, b, u, v);
            fillSolution(in, a, b, t, out, 2);
            return 2;
         }
      }
      
      // find single solution
      if (type == LEFT_AB) {
         bounds[1] = -a2-a*Math.abs(u);
         t = bounds[1];
      } else if (type == LEFT_BA) {
         bounds[1] = -b2-b*Math.abs(v);
         t = bounds[1];
      } else if (type==RIGHT_AB) {
         bounds[0] = -b2+b*Math.abs(v);
         t = bounds[0];
      } else {
         bounds[0] = -a2+a*Math.abs(u);
         t = bounds[0];
      }
       
      t = newton(t, bounds, a, b, u, v);
      fillSolution(in, a, b, t, out, 0);
      
      return 1;

   }

   static final int [][] P_IDX = {{1,3},{0,3},{0,2},{1,2}};

   private double dist2(double [] p1, double [] p2, int offset1, int offset2) {
      double a = p2[0+offset2]-p1[0+offset1];
      double b = p2[1+offset2]-p1[1+offset1];
      return a*a+b*b;
   }

   public void projectToBoundary(double[] in, double[] out) {

      if (isWithin(in[0], in[1])) {

         // check all 8 sections, keep closest
         double [] best = new double[2];
         double bestDist2 = Double.POSITIVE_INFINITY;
         double dist2 = 0;
         double[] buf = myParams.getBuffer();
         double [] tmp2 = new double[8];

         for (int i=0; i<4; i++) {
            
            int nsol = projectPerp(in, buf[P_IDX[i][0]], buf[P_IDX[i][1]], i, tmp2); 
            
            for (int j=0; j<nsol; j++) {
               dist2 = dist2(in,tmp2, 0,j*2);
               if (dist2<bestDist2) {
                  bestDist2 = dist2;
                  best[0] = tmp2[0+2*j];
                  best[1] = tmp2[1+2*j];
               }
            }
         }
         
         out[0] = best[0];
         out[1] = best[1];

      } else {
         projectToBoundaryFromWithout(in, out);
      }

   }

   private void projectToBoundaryFromWithout(double [] in, double [] out) {

      double[] buf = myParams.getBuffer();
      int quad;

      // check which quadrant
      if (in[0] >= 0) {
         if (in[1] >= 0) {
            quad = 0;
         } else {
            quad = 3;
         }
      } else {
         if (in[1] <= 0) {
            quad = 2;
         } else {
            quad = 1;
         }
      }

      double dist2 = 0;
      double bestDist2 = Double.POSITIVE_INFINITY;
      double tmp [] = new double[4];
      double best [] = new double[2];

      
      int nsol = projectPerp(in, buf[P_IDX[quad][0]], buf[P_IDX[quad][1]], quad, tmp); 
      
      for (int j=0; j<nsol; j++) {
         dist2 = dist2(in,tmp, 0,j*2);
         if (dist2<bestDist2) {
            bestDist2 = dist2;
            best[0] = tmp[0+2*j];
            best[1] = tmp[1+2*j];
         }
      }
      out[0] = best[0];
      out[1] = best[1];
   }

}
