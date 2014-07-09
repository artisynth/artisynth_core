package maspack.spatialmotion.projections;

public class OrthographicProjector implements SphericalProjector {

   static final int DEFAULT_FLAGS = 0;
   int myFlags = DEFAULT_FLAGS;
   static final int YMAJOR = 0x0001;   // for sphereToPlane, favours larger Y
   
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;
   
   // if |X| or |Y| > 1, continue to bottom half
   public static void planeToSphere(double X, double Y, double[] out, int flags) {
      
      int c = 1;
      
      // trim invalid entries and wrap around sphere
      if ( (flags & YMAJOR)==0) {
         if (Math.abs(Y) > 1) {
            Y = Math.signum(Y);
         }
         
         double Xc = Math.sqrt(1-Y*Y);
         if (Math.abs(Xc)<EPSILON) {
            X = 0;  
          }
         while (Math.abs(X)>Xc) {
            X=2*Math.signum(X)*Xc-X;
            c = -c;
         }
         
      } else { 
         if (Math.abs(X) > 1) {
            X = Math.signum(X);
         }
         
         double Yc = Math.sqrt(1-X*X);
         if (Math.abs(Yc)<EPSILON) {
           Y = 0;  
         }
         
         while (Math.abs(Y)>Yc) {
            Y=2*Math.signum(Y)*Yc-Y;
            c = -c;            
         }         
      }
      
      out[0] = X;
      out[1] = Y;
      out[2] = c*Math.sqrt(1-X*X-Y*Y);

   }

   public static void sphereToPlane(double x, double y, double z, double[] out, int flags) {
      
      if (z < 0) {
         if ( (flags & YMAJOR) == 0 ) {
            // extend X
            double Xc = Math.sqrt(1-y*y);
            x = 2*Math.signum(x)*Xc-x;
         } else {
            // extend Y
            double Yc = Math.sqrt(1-x*x);
            y = 2*Math.signum(y)*Yc-y;
         }
      }
      
      out[0] = x;
      out[1] = y;
      
   }

   public void planeToSphere(double X, double Y, double[] out) {
      planeToSphere(X, Y, out, myFlags);
   }

   public void sphereToPlane(double x, double y, double z, double[] out) {
      sphereToPlane(x, y, z, out, myFlags);
   }

   public void setFlags(int flags) {
      this.myFlags = flags;
   }

   public int getFlags() {
      return myFlags;
   }

   public ProjectorType getType() {
      return ProjectorType.ORTHOGRAPHIC;
   }

   public void getJacobian(double X, double Y, double[] out) {
      getJacobian(X, Y, out, myFlags);
   }
   
   public static void getJacobian(double X, double Y, double[] out, int flags) {
      
      int a = 1;
      int b = 1;
      
      // trim invalid entries and wrap around sphere
      if ( (flags & YMAJOR)==0) {
         if (Math.abs(Y) > 1) {
            Y = Math.signum(Y);
         }
         
         double Xc = Math.sqrt(1-Y*Y);
         if (Math.abs(Xc)<EPSILON) {
            X = 0;  
          }
         while (Math.abs(X)>Xc) {
            X=2*Math.signum(X)*Xc-X;
            a = -a;
         }
         
      } else { 
         if (Math.abs(X) > 1) {
            X = Math.signum(X);
         }
         
         double Yc = Math.sqrt(1-X*X);
         if (Math.abs(Yc)<EPSILON) {
           Y = 0;  
         }
         
         while (Math.abs(Y)>Yc) {
            Y=2*Math.signum(Y)*Yc-Y;
            b = -b;
         }         
      }
      
      out[0] = a;
      out[1] = 0;
      
      out[2] = 0;
      out[3] = b;
      
      // at singularity, set to zero for symmetry
      if (Math.abs(1-X*X-Y*Y)<EPSILON) {
         out[4] = 0;
         out[5] = 0;
      } else {
         double d = Math.sqrt(1-X*X-Y*Y);
         out[4] = -b*X/d;
         out[5] = -a*Y/d;
      }
      
   }

   public void getJacobian(double x, double y, double z, double[] out) {
      getJacobian(x,y,z, out, myFlags);
   }
      
   public static void getJacobian(double x, double y, double z, double[] out, int flags) {
      
      double a = 1;
      double b = 1;
      
      if (z < 0) {
         if ( (flags & YMAJOR) == 0 ) {
            // extend X
            double Xc = Math.sqrt(1-y*y);
            x = 2*Math.signum(x)*Xc-x;
            a=-1;
         } else {
            // extend Y
            double Yc = Math.sqrt(1-x*x);
            y = 2*Math.signum(y)*Yc-y;
            b=-1;
         }
      }
      
      out[0] = a;
      out[1] = 0;      
      out[2] = 0;
      
      out[3] = 0;
      out[4] = b;      
      out[5] = 0;
      
   }
   
   

}
