package artisynth.core.femmodels;

import artisynth.core.femmodels.SkinMeshBody.NearestPoint;
import maspack.matrix.Point3d;

/**
 * Computes a non-normalized weight for a SkinConnection between a point and a
 * master body based on the inverse square of the distance.
 */
public class InverseSquareWeighting extends SkinWeightingFunction {

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
            w = dmin*dmin/(d*d);
         }
         weights[i] = w;
         sumw += w;
      }
      for (int i=0; i<nearestPnts.length; i++) {
         weights[i] /= sumw;
      }
   }

}

