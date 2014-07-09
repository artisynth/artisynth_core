package maspack.spatialmotion.projections;

public class LambertProjector implements SphericalProjector {
   
   /*
    * Lambert Azimuthal Equal Area Projection
    */
   
   static int REVERSE = 1;
   static int DEFAULT_FLAGS = 0;
   
   int myFlags = DEFAULT_FLAGS;
   
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;
   
   public static void planeToSphere(double X, double Y, double[] out, int flags) {

      double a = X*X+Y*Y;
         
      int c=1;
      if ((flags & REVERSE) != 0) {
         c=-c;
      }
         
      double b = Math.sqrt(1-a/4);
      out[0] = X*b;
      out[1] = Y*b;
      out[2] = c*(1-a/2);

   }

   public static void sphereToPlane(double x, double y, double z, double[] out, int flags) {

      int c = 1;
      if ((flags & REVERSE) != 0) {
         c=-1;
      }
      
      if ( c*z < c*(EPSILON-1) ) {
         
         // should lie somewhere on the circle of radius Pi
         double r = x*x+y*y;
         if (r < EPSILON) {
            out[0] = Math.PI;
            out[1] = 0;
         } else {
            out[0] = x/r*Math.PI;
            out[1] = y/r*Math.PI;
         }
      } else {
         out[0] = x * Math.sqrt( 2.0 /(1+c*z));
         out[1] = y * Math.sqrt( 2.0 /(1+c*z));
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
      return ProjectorType.LAMBERT;
   }

   public void getJacobian(double X, double Y, double[] out) {
      getJacobian(X, Y, out, myFlags);
   }
   
   public static void getJacobian(double X, double Y, double [] out, int flags) {
      
      int c=1;
      if ((flags & REVERSE) != 0) {
         c=-c;
      }
         
      double a = X*X+Y*Y;
      double b = Math.sqrt(1-a/4);
      
      out[0] = (4-2*X*X-Y*Y)/b;
      out[1] = -X*Y/b;
      
      out[2] = -X*Y/b;
      out[3] = (4-2*Y*Y-X*X)/b;
      
      out[4] = -c*X;
      out[5] = -c*Y;
      
   }

   public void getJacobian(double x, double y, double z, double[] out) {
      getJacobian(x, y, z, out,myFlags);
   }

   public static void getJacobian(double x, double y, double z, double[] out, int flags) {
      
      int c = 1;
      if ((flags & REVERSE) != 0) {
         c=-1;
      }
      
      if ( c*z > c*(1-EPSILON) ) {
         
         out[0] = Double.POSITIVE_INFINITY;
         out[1] = 0;
         out[2] = -c*Math.signum(x)*Double.POSITIVE_INFINITY;
         
         out[0] = 0;
         out[1] = Double.POSITIVE_INFINITY;
         out[2] = -c*Math.signum(y)*Double.POSITIVE_INFINITY;
         
         
      } else {
      
         double d = Math.sqrt(2/(1+c*z));
         double d3 = d*d*d;
         
         out[0] = d;
         out[1] = 0;
         out[2] = -c*x*d3/2;
         
         out[3] = 0;
         out[4] = d;
         out[5] = -c*y*d3/2;
         
      }
      
   }
   
}
