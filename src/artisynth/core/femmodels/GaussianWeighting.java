package artisynth.core.femmodels;

import artisynth.core.femmodels.SkinMeshBody.NearestPoint;
import maspack.matrix.Point3d;
import maspack.properties.*;

/**
 * Computes a non-normalized weight for a SkinConnection between a point and a
 * master body based using a Gaussian.
 */
public class GaussianWeighting extends SkinWeightingFunction {

   public static final double DEFAULT_SIGMA = 1.0;
   double mySigma = DEFAULT_SIGMA;

   public static PropertyList myProps =
      new PropertyList(GaussianWeighting.class, SkinWeightingFunction.class);

   static {
      myProps.add ("sigma", "standard deviation for the Gaussian", DEFAULT_SIGMA);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }  

   public GaussianWeighting () {
   }
   
   public GaussianWeighting (double sigma) {
      mySigma = sigma;
   }
   
   public double getSigma() {
      return mySigma;
   }

   public void setSigma (double sigma) {
      if (mySigma != sigma) {
         mySigma = sigma;
         notifyHostOfPropertyChange("sigma");
      }
   }

   public void scaleDistance (double s) {
      mySigma *= s;
   }

   /**
    * {@inheritDoc}
    */
   public void computeWeights (
      double[] weights, Point3d pos, NearestPoint[] nearestPnts) {

      double dmin = Double.POSITIVE_INFINITY;
      for (int i=0; i<nearestPnts.length; i++) {
         if (nearestPnts[i].distance < dmin) {
            dmin = nearestPnts[i].distance;
         }
      }
      double sumw = 0;
      for (int i=0; i<nearestPnts.length; i++) {
         double d = nearestPnts[i].distance;
         double w;
         if (d == dmin) {
            w = 1;
         }
         else {
            w = Math.exp(-(d-dmin)*(d-dmin)/(2*mySigma*mySigma));
         }
         weights[i] = w;
         sumw += w;
      }
      for (int i=0; i<nearestPnts.length; i++) {
         weights[i] /= sumw;
      }
   }
}

