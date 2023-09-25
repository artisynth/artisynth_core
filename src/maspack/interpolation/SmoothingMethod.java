package maspack.interpolation;

/**
 * Describes a smoothing method to be used on a sequence of data.
 */
public enum SmoothingMethod {

   /**
    * Average values across a moving window.
    */
   MOVING_AVERAGE,

   /**
    * Fit values to a polynomial within a moving window.
    */
   SAVITZKY_GOLAY
}
