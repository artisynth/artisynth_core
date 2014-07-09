package maspack.spatialmotion.projections;

public class StereographicProjector implements SphericalProjector {
   
   
   static int REVERSE = 0x0001;
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
         
      out[0] = 2*X/(1+a);
      out[1] = 2*Y/(1+a);
      out[2] = c*(1-a)/(a+1);

   }

   public static void sphereToPlane(double x, double y, double z, double[] out, int flags) {

      int c=1;
      if ((flags & REVERSE) != 0) {
         c=-c;
      }
      
      if ( c*z > c*(1-EPSILON) ) {
         out[0] = Math.signum(x)*Double.POSITIVE_INFINITY;
         out[1] = Math.signum(y)*Double.POSITIVE_INFINITY;
      } else {
         out[0] = x/(1+c*z);
         out[1] = y/(1+c*z);
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
      return ProjectorType.STEREOGRAPHIC;
   }

   public void getJacobian(double X, double Y, double[] out) {
      getJacobian(X, Y, out, myFlags);
   }

   public void getJacobian(double x, double y, double z, double[] out) {
      getJacobian(x, y, z, out, myFlags);
   }
   
   public static void getJacobian(double X, double Y, double[] out, int flags) {

      double a = X*X+Y*Y;
      int c=1;
      if ((flags & REVERSE) != 0) {
         c=-c;
      }
      
      double d = (1+a)*(1+a);
      
      out[0] = -2*(X*X-Y*Y-1)/d;
      out[1] = -4*X*Y/d;
      
      out[2] = -4*X*Y/d;
      out[3] = -2*(Y*Y-X*X-1)/d;
      
      out[4] = -4*c*X/d;
      out[5] = -4*c*Y/d;

   }

   public static void getJacobian(double x, double y, double z, double[] out, int flags) {

      int c=1;
      if ((flags & REVERSE) != 0) {
         c=-c;
      }
      
      if ( c*z > c*(1-EPSILON) ) {
         out[0] = Double.POSITIVE_INFINITY;
         out[1] = 0;
         out[2] = -c*Math.signum(x)*Double.POSITIVE_INFINITY;
            
         out[0] = 0;
         out[1] = Double.POSITIVE_INFINITY;
         out[2] = -c*Math.signum(y)*Double.POSITIVE_INFINITY;
      } else {
         
         double a = 1+c*z;
         double a2 = a*a;
         
         out[0] = 1/a;
         out[1] = 0;
         out[2] = -c*x/a2;
         
         out[0] = 0;
         out[1] = 1/a;
         out[2] = -c*y/a2;
         
      }
      
   }  

}
