package maspack.spatialmotion.projections;

public interface SphericalProjector {

   public static enum ProjectorType {
      POLAR, ORTHOGRAPHIC, STEREOGRAPHIC, LAMBERT, GNOMONIC
   }
   
   public void planeToSphere(double X, double Y, double[] out);
   public void sphereToPlane(double x, double y, double z, double [] out);
   public void getJacobian(double X, double Y, double [] out);  // plane-to-sphere Jacobian
   public void getJacobian(double x, double y, double z, double [] out); // sphere-to-plane Jacobian
   
   public ProjectorType getType();
   
}
