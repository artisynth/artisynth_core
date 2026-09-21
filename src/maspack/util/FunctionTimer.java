/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Class to measure the elapsed time between events, as marked by calls to the
 * {@link #start start} and {@link #stop stop} methods. Mainly intended for use
 * in timing the execution speed of functions.
 */
public class FunctionTimer {
   long startTime;
   long elapsedTime = 0;
   long totalTime = 0;
   int cycleCnt = 0;
   private double resolutionUsec = 0.001;
   private NumberFormat fmt;

   /**
    * Create a new FunctionTimer
    */
   public FunctionTimer() {
      elapsedTime = 0;
      cycleCnt = 0;
      totalTime = 0;
      startTime = -1;
   }

   /**
    * Marks the beginning of a new timing interval and sets the elapsed time to
    * 0.
    */
   public void start() {
      elapsedTime = 0;
      startTime = getCurrentTime();
   }

   /**
    * Marks the beginning of a new timing interval and leaves the elapsed time
    * unchanged.
    */
   public void restart() {
      startTime = getCurrentTime();
   }

   /**
    * Sets the elapsed time, total time and cycle count to 0 and re-initializes
    * the start indicator.
    */
   public void reset() {
      elapsedTime = 0;
      startTime = -1;
      totalTime = 0;
      cycleCnt = 0;
   }

   /**
    * Adds the time difference between the current time and the last time
    * {@link #start start} or {@link #restart restart} was called to the elapsed
    * time. If {@link #start start} was not previously called, no change is made
    * to the elapsed time.
    */
   public void stop() {
      if (startTime != -1) {
         double elapsed = (getCurrentTime() - startTime);
         elapsedTime += elapsed;
         totalTime += elapsed;
         startTime = -1;   // prevent duplicating time if stop called twice
         cycleCnt++;
      }
   }

   /**
    * Returns the elapsed time in microseconds.
    *
    * <p>If the timer is running (i.e., {@link #start start} or {@link #restart
    * restart} has been called but {@link #stop stop} has not yet been called,
    * the associated time difference will not be included in the result.
    * 
    * @return elapsed time
    */
   public double getTimeUsec() {
      return elapsedTime * resolutionUsec;
   }

   /**
    * Returns the sum of all the elapsed times measured (in microseconds) since
    * the timer was created or {@link #reset} was last called.
    *
    * <p>If the timer is running (i.e., {@link #start start} or {@link #restart
    * restart} has been called but {@link #stop stop} has not yet been called,
    * the associated time difference will not be included in the result.
    * 
    * @return total time
    */
   public double getTotalTimeUsec() {
      return totalTime * resolutionUsec;
   }

   /**
    * Returns the total number of record cycles ({@link #start} followed by
    * {@link #stop}, with possible {@link #restart} calls in between) since the
    * timer was created or {@link #reset} was last called.
    * 
    * @return cycle count
    */
   public int getCycleCount() {
      return cycleCnt;
   }


   /**
    * Returns the total time divided by the cycle count, in microseconds.  See
    * {@link #getTotalTimeUsec} and {@link #getCycleCount}.
    *
    * @return average time
    */
   public double getAverageTimeUsec() {
      if (cycleCnt > 0) {
         return getTotalTimeUsec()/cycleCnt;
      }
      else {
         return 0;
      }
   }


   /**
    * Returns the resolution of this timer, in microseconds. This depends on the
    * system calls available for measuring time.
    * 
    * @return timer resolution
    */
   public double getResolutionUsec() {
      return resolutionUsec;
   }

   protected String format (double val) {
      if (fmt == null) {
         fmt = new NumberFormat ("%.6g");
      }
      return fmt.format (val);
   }

   /**
    * Returns a string describing the current elapsed time, divided by the
    * supplied count parameter. The resulting divided time is displayed in
    * milliseconds, unless it is less than 1 millisecond, in which case it is
    * displayed in microseconds.
    * 
    * @param cnt
    * count to divide the elapsed time by
    * @return string describing the divdied time
    */
   public String result (int cnt) {
      double usec = getTimeUsec() / cnt;
      if (usec < 1000) {
         return "" + format(usec) + " usec";
      }
      else {
         return "" + format(usec/1000.0) + " msec";
      }
   }

   /**
    * Returns a string describing the average time, as returned by {@link
    * #getAverageTimeUsec}. The result is displayed in milliseconds, unless it
    * is less than 1 millisecond, in which case it is displayed in
    * microseconds.
    * 
    * @return string describing the average time
    */
   public String averageTime () {
      double usec = getTotalTimeUsec();
      if (usec < 1000) {
         return "" + format(usec) + " usec";
      }
      else {
         return "" + format(usec/1000.0) + " msec";
      }
   }

   /**
    * Returns a string describing the current elapsed time, divided by the
    * supplied count parameter. The resulting divided time is displayed in
    * microseconds.
    * 
    * @param cnt
    * count to divide the elapsed time by
    * @return string describing the divdied time
    */
   public String resultUsec (int cnt) {
      return "" + format(getTimeUsec()/cnt) + " usec";
   }

   /**
    * Returns a string describing the current elapsed time, divided by the
    * supplied count parameter. The resulting divided time is displayed in
    * milliseconds.
    * 
    * @param cnt
    * count to divide the elapsed time by
    * @return string describing the divdied time
    */
   public String resultMsec (int cnt) {
      return "" + format(getTimeUsec()/(cnt*1000.0)) + " msec";
   }

   private long getCurrentTime() {
      return System.nanoTime();
   }
}
