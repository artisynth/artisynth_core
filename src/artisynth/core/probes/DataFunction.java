package artisynth.core.probes;

import maspack.matrix.VectorNd;

/**
 * Defines a function for applying the numeric data in NumericControlProbes, or
 * generating data in NumericMonitorProbes.
 */
public interface DataFunction {

      /**
       * When used in a {@link NumericControlProbe}, applies the numeric data
       * of this probe, supplied by <code>vec</code>, for either the specified
       * absolute time <code>t</code> or probe relative time <code>trel</code>.
       * This will be called within the probe's default implementation of its
       * {@link NumericControlProbe#applyData} function, with the arguments
       * assuming the same roles.
       *
       * When used in a {@link NumericMonitorProbe}, evaluates a
       * vectored-valued function of time and stores the result in
       * <code>vec</code>. This will be called within the probe's default
       * implementation of its {@link NumericMonitorProbe#generateData}
       * function, with the arguments assuming the same roles.
       *
       * @param vec supplies (or returns) the numeric data
       * @param t absolute time (seconds)
       * @param trel probe relative time
       */
      public void eval (VectorNd vec, double t, double trel);

}
