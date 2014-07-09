package maspack.spatialmotion.projections;

public class GnomonicProjector implements SphericalProjector {
   
   /*
    * Gnomonic Projection
    */
   static int REVERSE = 1;
   static int DEFAULT_FLAGS = 0; 
  
   int myFlags = DEFAULT_FLAGS;
   
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;
   
   public static void planeToSphere(double X, double Y, double[] out, int flags) {
      
      int c=1;
      if ((flags & REVERSE) != 0) {
         c=-1;
      }

      out[2] = c/Math.sqrt(X*X + Y*Y + 1);
      out[0] = X*out[2];
      out[1] = Y*out[2];
      
   }

   public static void sphereToPlane(double x, double y, double z, double[] out, int flags) {
      
      double a = Math.abs(z);
      
      if ( a < EPSILON) {
         // undefined
         out[0] = Math.signum(x)*Double.POSITIVE_INFINITY;
         out[1] = Math.signum(y)*Double.POSITIVE_INFINITY;

      } else {
            out[0] = x / a;
            out[1] = y / a;
      }
      
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
      return ProjectorType.GNOMONIC;
   }

   public void getJacobian(double X, double Y, double[] out) {
      
      getJacobian(X,Y,out,myFlags);
      
   }
   
   public static void getJacobian(double X, double Y, double[] out, int flags) {
      
      int c=1;
      if ((flags & REVERSE) != 0) {
         c=-1;
      }
      
      double d = Math.sqrt(X*X + Y*Y + 1);
      double d3 = d*d*d;
      
      out[0] = c*(1+Y*Y)/d3;
      out[1] = -c*X*Y/d3;
      
      out[2] = -c*X*Y/d3;
      out[3] = c*(1+X*X)/d3;;
      
      out[4] = -c*X/d3;
      out[5] = -c*Y/d3;
      
   }

   public static void getJacobian(double x, double y, double z, double [] out, int flags) {
      
      double a = Math.abs(z);
      
      if ( a < EPSILON) {
         // undefined
         out[0] = Double.POSITIVE_INFINITY;
         out[1] = 0;
         out[2] = -Math.signum(x)*Double.POSITIVE_INFINITY;
         
         out[0] = 0;
         out[1] = Double.POSITIVE_INFINITY;
         out[2] = -Math.signum(y)*Double.POSITIVE_INFINITY;
         
      } else {
         
         out[0] = 1/a;
         out[1] = 0;
         out[2] = -x/(a*a);
         
         out[0] = 0;
         out[1] = 1/a;
         out[2] = -y/(a*a);

      }
      
   }
   
   public void getJacobian(double x, double y, double z, double[] out) {
      
      getJacobian(x, y, z, out, myFlags);
      
   }

}
