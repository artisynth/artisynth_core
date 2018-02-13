/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

/**
 * Converts between basic Artisynth time units (ticks) and seconds,
 * milliseconds, microseconds, and frequency. The basic time tick is a
 * sub-nanosecond quantity chosen to allow the time intervals associated with
 * standard audio frequencies (i.e., 48kHz, 44.1khz, 32khz, etc.), as well as
 * those which are multiples of 1 microsecond, to be represented as long
 * integers.
 */
public class TimeBase {

   /** 
    * Reciprocal of the smallest time unit used by {@link #round}.
    */
   public static final double PRECISION = 1e12;

   /** 
    * Rounds <code>t</code> to the nearst {@link #PRECISION} seconds.
    * 
    * @return rounded value of t.
    */
   public static double round (double t) {
      return Math.rint (t*PRECISION)/PRECISION;
   }

   /** 
    * Returns true if <code>t0</code> and <code>t1</code> are equal
    * within the tolerance specified by {@link #PRECISION}.
    * 
    * @return true if t0 and t1 are equal within tolerance
    */
   public static boolean equals (double t0, double t1) {
      return round(t0) == round(t1);
   }

   /** 
    * Returns <code>t0</code> modulo <code>t1</code>, 
    * within the tolerance specified by {@link #PRECISION}.
    * 
    * @return t0 % t1 within {@link #PRECISION}.
    */
   public static double modulo (double t0, double t1) {
      long l0 = (long)Math.round (t0*PRECISION);
      long l1 = (long)Math.round (t1*PRECISION);
      return (l0 % l1)/PRECISION;      
   }
   
   /** 
    * Compares <code>t0</code> and <code>t1</code> within the tolerance 
    * specified by the reciprocal of {@link #PRECISION}. Returns an
    * integer which is less than, equal, or greater than 0 if
    * <code>t0</code> is less than, equal, or greater than <code>t1</code>.
    * 
    * @return integer comparing t0 and t1
    */
   public static int compare (double t0, double t1) {
      double r0 = round (t0);
      double r1 = round (t1);
      if (r0 < r1) {
         return -1;
      }
      else if (r0 == r1) {
         return 0;
      }
      else {
         return 1;
      }
   }

   public static void main (String[] args) {
      // NumberFormat fmt = new NumberFormat ("%10d");
      // int[] freq =
      //    new int[] { 8000, 11025, 22050, 44100, 48000, 96000,192000, 1000000 };
      // System.out.println ("time ticks for frequencies:");
      // for (int i = 0; i < freq.length; i++) {
      //    long ticks = frequencyToTicks (freq[i]);
      //    System.out.println (fmt.format (freq[i]) + " " + fmt.format (ticks));
      //    assert (freq[i] == ticksToFrequency (ticks));
      // }

      // trying to see how accurately we can represent 0.5, 0.2 and 0.1 x 10^-n

      double[] times = new double[] {
         0.5, 0.2, 0.1, 
         0.05, 0.02, 0.01, 
         0.005, 0.002, 0.001, 
         0.0005, 0.0002, 0.0001, 
         0.00005, 0.00002, 0.00001, 
         0.000005, 0.000002, 0.000001, 
         0.0000005, 0.0000002, 0.0000001, 
         0.00000005, 0.00000002, 0.00000001, 
         0.000000005, 0.000000002, 0.000000001, 
         0.0000000005, 0.0000000002, 0.0000000001, 
         0.00000000005, 0.00000000002, 0.00000000001, 
         0.000000000005, 0.000000000002, 0.000000000001, 
         0.0000000000005, 0.0000000000002, 0.0000000000001, 
      };
      for (int i=0; i<times.length/3; i++) {
         System.out.println (
            round(times[i*3])+" "+round(times[i*3+1])+" "+round(times[i*3+2]));
      }
      double t = 1;
      for (int i=0; i<10; i++) {
         System.out.println (round(t*0.5)+" "+round(t*0.2)+" "+round(t*0.1));
         t *= 0.1;
      }

      for (int i=0; i<10; i++) {
         t = Math.pow (10, -i);
         System.out.println (t*0.5+" "+t*0.2+" "+t*0.1);
      }

      System.out.println ("values for 5.0e-12, 2.0e12, 1.0e-12:");
      System.out.println (5e-12 + " " + 2e-12 + " " + 1e-12);
      System.out.println (
         Double.doubleToLongBits(5e-12) + " " +
         Double.doubleToLongBits(2e-12) + " " +
         Double.doubleToLongBits(1e-12));
      System.out.println (
         Double.toHexString(5e-12) + " " +
         Double.toHexString(2e-12) + " " +
         Double.toHexString(1e-12));

      // double sec = 1.0;
      double nsec = 1.0;
      for (int i=0; i<9; i++) {
         nsec *= 0.1;
      }
      System.out.println ("nsec by different computations:");
      System.out.println (nsec + " " + 0.000000001);

      double t0 = 200000;
      double interval = 0.01;
      System.out.println (t0 + interval - TimeBase.modulo (t0, interval));
   }

}
