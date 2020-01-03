package artisynth.core.femmodels.integration;

import maspack.matrix.Vector2d;
import maspack.matrix.VectorNd;
import maspack.util.Logger;

/**
 * Integrates generic functions using Monte-Carlo sampling
 * 
 * @author Antonio
 *
 */
public class MonteCarloIntegrator {

   static int MIN_SAMPLES = 100;
   static int MAX_SAMPLES = 100000;
   static double MAX_VARIANCE = 1e-12;

   private int minSamples;
   private int maxSamples;
   private double maxVariance;

   public MonteCarloIntegrator() {
      setLimits(MIN_SAMPLES, MAX_SAMPLES, MAX_VARIANCE);
   }

   /**
    * Samples a function, producing the value and probability (or 'importance')
    * of selecting the sampled point
    */
   public static interface FunctionSampler {
      /**
       * Produces a function sample and importance probability weight
       * 
       * @param vp
       * output value and probability
       */
      public void sample(Vector2d vp);
   }

   /**
    * Samples a function, producing the value and probability (or 'importance')
    * of selecting the sampled point
    */
   public static interface FunctionNdSampler {

      /**
       * Size of the output
       * 
       * @return number of output dimensions
       */
      public int getSize();

      /**
       * Produces an Nd-function sample, with associated importance probability
       * weights.
       * 
       * @param v
       * N-D output value
       * @param p
       * N-D probabilities
       */
      public void sample(VectorNd v, VectorNd p);
   }

   /**
    * Integrator will continue integrating until either the variance estimate
    * falls below maxVariance, or maxIterations is reached
    * 
    * @param minSamples
    * minimum number of samples
    * @param maxSamples maximum number of samples
    * @param maxVariance variance threshold
    */
   public void setLimits(int minSamples, int maxSamples, double maxVariance) {
      this.minSamples = minSamples;
      this.maxSamples = maxSamples;
      this.maxVariance = maxVariance;
   }

   public void setMaxSamples(int max) {
      this.maxSamples = max;
   }

   public void setMinSamples(int min) {
      this.minSamples = min;
   }

   public void setMaxVariance(int max) {
      this.maxVariance = max;
   }

   /**
    * Integrates the provided function using importance-sampling
    * 
    * @param func function to integrate
    * @param minSamples minimum number of samples
    * @param maxSamples maximum number of samples
    * @param maxVariance variance threshold to stop integration
    * @return integral estimate
    */
   public static double integrate(
      FunctionSampler func, int minSamples, int maxSamples,
      double maxVariance) {

      if (minSamples < 2) {
         minSamples = 2;
      }

      Vector2d vp = new Vector2d();

      // error approximation: V*sigma_N/sqrt(N)
      // sigma_N = 1/(N-1)*sum(f(xi)-f)^2;

      double swfi2 = 0; // sum of (w_i*f_i)^2, w_i = 1/p_i
      double swfi = 0; // sum of (w_i*f_i)

      // do at least some samples
      int count = 0;
      for (int i = 0; i < minSamples; ++i) {
         func.sample(vp);
         double fi = vp.x;
         double p = vp.y;
         double wfi = fi / p; // weighted by inverse probability
         swfi2 += wfi * wfi;
         swfi += wfi;
         ++count;
      }

      double I = swfi / count; // estimated integral
      double e2 =
         (swfi2 - 2 * swfi * I + count * I * I) / ((count - 1) * count);

      // keep sampling until error drops
      while (e2 > maxVariance && count < maxSamples) {
         func.sample(vp);
         double fi = vp.x;
         double p = vp.y;
         double wfi = fi / p; // weighted by inverse probability
         swfi2 += wfi * wfi;
         swfi += wfi;
         ++count;
         I = swfi / count;
         e2 = (swfi2 - 2 * swfi * I + count * I * I) / ((count - 1) * count);
      }

      Logger.getSystemLogger().debug(
         "Computed integral in " + count + " iterations, sigma^2=" + e2);

      return I;
   }

   /**
    * Integrates the provided N-dimensional function using importance-sampling
    * 
    * @param func function to integrate
    * @param minSamples minimum number of samples
    * @param maxSamples maximum number of samples
    * @param maxVariance variance threshold for integration
    * @param out output of integration
    */
   public static void integrate(
      FunctionNdSampler func, int minSamples, int maxSamples,
      double maxVariance, VectorNd out) {

      if (minSamples < 2) {
         minSamples = 2;
      }

      int dim = func.getSize();
      VectorNd v = new VectorNd(dim);
      VectorNd p = new VectorNd(dim);

      // error approximation: V*sigma_N/sqrt(N)
      // sigma_N = 1/(N-1)*sum(f(xi)-f)^2;

      VectorNd swfi2 = new VectorNd(dim); // sum of (w_i*f_i)^2, w_i = 1/p_i
      VectorNd swfi = new VectorNd(dim); // sum of (w_i*f_i)

      // do at least some samples
      int count = 0;
      for (int i = 0; i < minSamples; ++i) {
         func.sample(v, p);
         for (int j = 0; j < dim; ++j) {
            double wfi = v.get(j) / p.get(j); // weighted by inverse probability
            swfi.add(j, wfi);
            swfi2.add(j, wfi * wfi);
         }
         ++count;
      }

      out.scale(1.0 / count, swfi); // estimated integral
      VectorNd e2 = new VectorNd(dim);
      double e2max = 0;
      for (int i = 0; i < dim; ++i) {
         double I = out.get(i);
         double err =
            (swfi2.get(i) - 2 * swfi.get(i) * I + count * I * I)
               / ((count - 1) * count);
         e2.set(i, err);
         if (err > e2max) {
            e2max = err;
         }
      }

      // keep sampling until maximum error drops
      while (e2max > maxVariance && count < maxSamples) {
         func.sample(v, p);
         for (int j = 0; j < dim; ++j) {
            double wfi = v.get(j) / p.get(j); // weighted by inverse probability
            swfi.add(j, wfi);
            swfi2.add(j, wfi * wfi);
         }
         ++count;

         e2max = 0;
         for (int i = 0; i < dim; ++i) {
            double I = out.get(i);
            double err =
               (swfi2.get(i) - 2 * swfi.get(i) * I + count * I * I)
                  / ((count - 1) * count);
            e2.set(i, err);
            if (err > e2max) {
               e2max = err;
            }
         }
      }

      Logger.getSystemLogger().debug(
         "Computed integral in " + count + " iterations, sigma^2=" + e2);

   }

   /**
    * Integrates the supplied random sampler function
    * 
    * @param func function to integrate
    * @return estimated integral
    */
   public double integrate(FunctionSampler func) {
      return integrate(func, minSamples, maxSamples, maxVariance);
   }

   /**
    * Integrates the supplied random sampler function, normalizing the error
    * estimate threshold by the provided volume
    * 
    * @param func function to integrate
    * @param volume volume of integration region
    * @return estimated integral
    */
   public double integrate(FunctionSampler func, double volume) {
      return integrate(
         func, minSamples, maxSamples, maxVariance * (volume * volume));
   }

   /**
    * Integrates the supplied random sampler function
    * 
    * @param func function to integrate
    * @param out estimated integral
    */
   public void integrate(FunctionNdSampler func, VectorNd out) {
      integrate(func, minSamples, maxSamples, maxVariance, out);
   }

   /**
    * Integrates the supplied random sampler function, normalizing the error
    * estimate threshold by the provided volume
    * 
    * @param func function to integrate
    * @param volume volume of integration region
    * @param out estimated integral
    */
   public void integrate(FunctionNdSampler func, double volume, VectorNd out) {
      integrate(
         func, minSamples, maxSamples, maxVariance * (volume * volume), out);
   }

}
