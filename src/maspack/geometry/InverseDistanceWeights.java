package maspack.geometry;

import java.util.Collection;
import maspack.matrix.*;

/**
 * Computes normalized weights for point-based interpolation based on
 * inverse-distance weighting, given a reference point <code>p</code> and a set
 * of support points <code>p_i</code>.
 *
 * <p>Each weight <code>w_i</code> is generated according to
 * <pre>
 *  w_i = f(p,p_i)/ sum_i f(p,p_i)
 * </pre>
 * where
 * <pre>
 *  f(p,p_i) = 1/ (d(p,p_i)^exp + lam)
 * </pre>
 * and <code>d(p,p_i)</code> is the distance from <code>p</code> to
 * <code>p_i</code>. If <code>normalize</code> is <code>true</code>,
 * <code>d(p,p_i)</code> is normalized by dividing by the maximum
 * distance.
 */
public class InverseDistanceWeights implements PointInterpolationWeights {

   private double myExp; // exponent
   private double myLam; // regularizer
   private boolean myNormalize;  // use normalized distances

   /**
    * Creates an InverseDistanceWeightGenerator with specified parameters.
    *
    * @param exp exponent for the inverse distance function
    * @param lam regularization term for the inverse distance function
    * @param normalize if <code>true</code>, use normalized distances
    * (distance divided by the maximum distance)
    */
   public InverseDistanceWeights (
      double exp, double lam, boolean normalize) {

      myExp = exp;
      myLam = lam;
      myNormalize = normalize;
   }

   private double pow (double d) {
      if (myExp == 0) {
         return 1;
      }
      else if (myExp == 1) {
         return d;
      }
      else if (myExp == 2) {
         return d*d;
      }
      else {
         return Math.pow (d, myExp);
      }
   }

   /**
    * Takes a set of desired weights, based on distance calculations, and
    * adjusts them so that the weighted sum of the support points equals p, and
    * the sum of the weights equals 1. We do this by minimizing the distance
    * between the desired weights wd, and the computed weights w, subject
    * to the above stated equality constraints. In other words,
    * <pre>
    *     min || w - wd ||^2
    * 
    * subject to
    *
    *     G w = y
    * </pre>
    * where the columns of G and y are given by
    * <pre>
    *            [ p_i ]         [ p ]
    *     G(j) = [     ]     y = [   ]
    *            [  1  ]         [ 1 ]
    * </pre>
    * This problem can be solved by computing
    * <pre>
    *     w = G^T inv(G G^T) (y + G wd) - wd
    * </pre>
    * Since G may be rank deficient, we compute the inverse using
    * a singular value decomposition of G^T, noting that if
    * <pre>
    *     G^T = U S V^T
    * </pre>
    * then
    * <pre>
    *     G^T inv(G G^T) = U inv(S) V^T
    * </pre>
    * Rank deficiency can be determined by examining the singular values.
    */
   public static boolean adjustWeightsForPosition (
      VectorNd w, Vector3d p, Collection<Vector3d> support) {

      MatrixNd GT = new MatrixNd (support.size(), 4);
      int i = 0;
      double[] vals = new double[] {0, 0, 0, 1};         
      for (Vector3d pi : support) {
         pi.get (vals);
         GT.setRow (i++, vals);
      }
      VectorNd wd = new VectorNd (w); // wd are the orginal desired weights
      p.get (vals);
      VectorNd x = new VectorNd (vals);
      GT.mulTransposeAdd (x, wd);
      SVDecomposition svd = new SVDecomposition();
      svd.factor (GT);
      MatrixNd U = svd.getU();
      MatrixNd V = svd.getV();
      VectorNd sig = svd.getS();
      V.mulTranspose (w, x);
      boolean rankDeficient = false;
      double sigmax = sig.get(0);
      for (i=0; i<4; i++) {
         double s = sig.get(i);
         if (s/sigmax < 1e-14) {
            rankDeficient = true;
            break;
         }
         w.set (i, w.get(i)/s);
      }
      U.mul (w, w);
      w.sub (wd);
      normalizeWeights (w);
      return !rankDeficient;
   }

   private static void normalizeWeights (VectorNd w) {
      double[] wbuf = w.getBuffer();
      double wsum = 0;
      for (int i=0; i<w.size(); i++) {
         wsum += wbuf[i];
      }
      for (int i=0; i<w.size(); i++) {
         wbuf[i] /= wsum;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean compute (VectorNd w, Vector3d p, Collection<Vector3d> support) {

      w.setSize (support.size());
      double[] dists = new double[support.size()];

      double maxd = 0;
      int i = 0;
      for (Vector3d pi : support) {
         double d = pi.distance (p);
         if (d > maxd) {
            maxd = d;
         }
         dists[i++] = d;
      }

      for (i=0; i<w.size(); i++) {
         double d = dists[i];
         if (myNormalize) {
            d /= maxd;
         }
         w.set (i, 1/((pow(d)+myLam)));
      }
      normalizeWeights (w);
      return adjustWeightsForPosition (w, p, support);
   }
}
