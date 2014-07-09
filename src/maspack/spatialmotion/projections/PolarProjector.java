package maspack.spatialmotion.projections;

public class PolarProjector implements SphericalProjector {
   

   // identifies which pole center of plane corresponds to
   public static enum Pole {
      X_MINUS, X_PLUS, Y_MINUS, Y_PLUS, Z_MINUS, Z_PLUS;
   }
   
   public static int CENTER = 0x0001;
   static int DEFAULT_FLAGS = 0;
   static Pole DEFAULT_POLE = Pole.X_MINUS; 
   
   int myFlags = DEFAULT_FLAGS;
   Pole myPole = DEFAULT_POLE;
   
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;

   private static void rotateCoordinates(double in[], double out[], Pole pole, boolean invert) {
      
      double tmp;
      int c = 1;
      if (invert) {
         c = -1;
      }
      
      switch(pole) {
         case X_MINUS:
            return;
         case X_PLUS:
            out[0]=-in[0];
            out[1]=-in[1];
            out[2]= in[2];
            return;
         case Y_MINUS:
            tmp=in[0];
            out[0] = -c*in[1];
            out[1] = c*tmp;
            out[2] = in[2];
            return;
         case Y_PLUS:
            tmp = in[0];
            out[0] = c*in[1];
            out[1] = -c*tmp;
            out[2] = in[2];
            return;
         case Z_MINUS:
            tmp=in[0];
            out[0] = -c*in[2];
            out[1] = -in[1];
            out[2] = -c*tmp;
            return;
         default:
      }
      
      // Z_PLUS
      tmp=in[0];
      out[0] = c*in[2];
      out[1] = in[1];
      out[2] = -c*tmp;
   }
   
   public Pole getPole() {
      return myPole;
   }
   
   public void setPole(Pole pole) {
      myPole = pole;
   }
   
   public static void planeToSphere(double X, double Y, double[] out, Pole pole, int flags) {

      if ((flags & CENTER) != 0) {
         X = X+Math.PI/2;
      }
      
      out[0] = Math.sin(X)*Math.cos(Y);
      out[1] = Math.sin(X)*Math.sin(Y);
      out[2] = Math.cos(X);
      
      rotateCoordinates(out, out, pole, false);

   }

   
   public static void sphereToPlane(double x, double y, double z, double[] out, Pole pole, int flags) {

      double[] in = {x,y,z};
      rotateCoordinates(in, in, pole, true);
      
      out[0] = Math.acos(clip(in[2],-1,1));
      
      if (Math.abs(in[1])<EPSILON) {
         // either cos(Y)=0 -> Y = Pi/2
         //   or sin(Y)=0 -> Y is anything  
         out[1] = Math.PI/2;  
      } else {
         out[1] = Math.atan2(in[1], in[0]);
      }
      
      
      if ((flags & CENTER) != 0) {
         out[0] = out[0]-Math.PI/2;
      }
      
   }
   
   private static double clip(double val, double min, double max) {
      if (val < min) {
         return min;
      } else if (val > max) {
         return max;
      }
      return val;
   }

   public void planeToSphere(double X, double Y, double[] out) {
      planeToSphere(X, Y, out, myPole, myFlags);
   }

   public void sphereToPlane(double x, double y, double z, double[] out) {
      sphereToPlane(x, y, z, out, myPole, myFlags);
   }

   public void setFlags(int flags) {
      this.myFlags = flags;
   }

   public int getFlags() {
      return myFlags;
   }

   public ProjectorType getType() {
      return ProjectorType.POLAR;
   }

   public void getJacobian(double X, double Y, double[] out) {
      // TODO Auto-generated method stub
      
   }

   public void getJacobian(double x, double y, double z, double[] out) {
      // TODO Auto-generated method stub
      
   }
   

}
