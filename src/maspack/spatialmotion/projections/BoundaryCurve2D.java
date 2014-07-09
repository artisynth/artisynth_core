package maspack.spatialmotion.projections;

import maspack.matrix.Point2d;
import maspack.matrix.VectorNd;

public interface BoundaryCurve2D {

   public enum CurveType {
      ELLIPTIC, RECTANGULAR
   }
   
   public boolean isWithin(double X, double Y);
   
   public boolean projectWithin(double in[], double out[]);
   public void projectToBoundary(double in[], double out[]);
   
   public double getLength();
   
   public Point2d getPoint(double t);
   public double getTVar(double X, double Y);
   public void getPoint(double t, double[] out);
   public void getTangent(double t, double [] out);
   public void setParameters(VectorNd params);
   public VectorNd getParameters();
   public int getNumParams();
   
   public CurveType getType();
   
   
}
