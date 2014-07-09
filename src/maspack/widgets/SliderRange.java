/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.util.*;

/*
 * A utility class to help calculate ranges for sliders.
 */
//
// Notes on setting a slider's range when the slider is attached to a property.
//
// In general, we may want to adjust the slider's range when:
//
// * the slider is created (by the PropertyWidget class)
// 
// * a new value is set (done inside updateInternalValue)
// 
// * when the property's range changes (done in PropertyWidget.updateValue)
//
// In general, for a property connected to a slider, we have:
//
// (a) a hard range interval, determined orginally by the property's range and 
//     which corresponds to the value returned by the slider's getRange method
//
// (b) a sliderRange which is a sub-interval of the hard range
// 
// The estimateBoundsIfNecessary() method can be used to determine an
// appropriate slider range given an initial range (typically corresponding to
// the hard range, which may not be bounded), and a reference value.
// This is usually called when the slider is created, or when the
// range is reset.
//
// The roundBoundsTo125() method is called to set the upper and lower
// bounds of the slider range to appropriately round numbers. This is
// usually called when the slider value is explicitly set by the user.
//
public class SliderRange {

   private static final double INF = Double.POSITIVE_INFINITY;

   /** 
    * Rounds a number x up to the nearest value defined by
    *
    * <pre>
    * sgn(x) k 10^n 
    * </pre>
    * where n is an integer and k is 1, 2, or 5.
    * @param x number to round up
    * @return rounded number
    */
   public static double roundUp125 (double x) {
      return Round.up125 (x);
   }

   /** 
    * Rounds a number x down to the nearest value defined by
    *
    * <pre>
    * sgn(x) k 10^n 
    * </pre>
    * where n is an integer and k is 1, 2, or 5.
    * @param x number to round down
    * @return rounded number
    */
   public static double roundDown125 (double x) {
      return Round.down125 (x);
   }

   /** 
    * Estimates the range for a slider, given an initial range
    * <code>range0</code> and an initial value <code>x</code> (assumed to be
    * within that range). Any finite bounds specified by <code>range0</code>
    * are preserved. In particular, if <code>range0</code> is completely
    * bounded, the method simply returns a copy the range.  Otherwise, if
    * either the upper or lower endpoints of the range are unbounded, the
    * method attempts to determine reasonable values for these, based on
    * <code>x</code>.
    *
    * @param range0 initial range value
    * @param x initial value to help determine bounds if necessary
    * @return bounded version of range0.
    */   
   public static DoubleInterval estimateBoundsIfNecessary (
      NumericInterval range0, double x) {

      double max = range0.getUpperBound();
      double min = range0.getLowerBound();

      x = range0.clipToRange (x); // make sure x is within range
      double lo, hi;
      if (max == INF && min == -INF) {
         // unbounded case
         if (x == 0) {
            return new DoubleInterval (0, 10);
         }
         else if (x > 0) {
            return new DoubleInterval (0, roundUp125 (4*x));
         }
         else { // x < 0 
            hi = roundUp125 (-4*x);
            return new DoubleInterval (-hi, hi);
         }
      }
      else if (max == INF) {
         // bounded from below
         lo = min;
         if (min == x) {
            hi = roundUp125 (min+10);
         }
         else if (min < 0 && x < -min/2) {
            hi = roundUp125 (-min);
         }
         else {
            hi = roundUp125 (min+4*(x-min));
         }
         DoubleInterval bounds = new DoubleInterval (lo, hi);
         bounds.setLowerBoundClosed (range0.isLowerBoundClosed());
         return bounds;
      }
      else if (min == -INF) {
         // bounded from above
         hi = max;
         if (max == x) {
            lo = roundDown125 (max-10);
         }
         else if (max > 0 && x > -max/2) {
            lo = roundDown125 (-max);
         }
         else {
            lo = roundDown125 (max-4*(max-x));
         }
         DoubleInterval bounds = new DoubleInterval (lo, hi);
         bounds.setUpperBoundClosed (range0.isUpperBoundClosed());
         return bounds;
      }
      else {
         return new DoubleInterval (range0);
      }
      
   }         

   /** 
    * Rounds the upper and lower bounds of a supplied range to conform to
    * <pre>
    * +/- k 10^n
    * </pre>
    * where n is an integer and k is 1, 2 or 5.
    *
    * @param range0 range to be rounded
    * @return rounded range
    */
   public static DoubleInterval roundBoundsTo125 (NumericInterval range0) {
      double max = range0.getUpperBound();
      double min = range0.getLowerBound();
      if (max >= 0) {
         double newmax = roundUp125 (max);
         double newdel = roundUp125 (newmax-min); 
         return new DoubleInterval (newmax-newdel, newmax);
      }
      else {
         double newmin = roundDown125 (min);
         double newdel = roundUp125 (max-newmin); 
         return new DoubleInterval (newmin, newmin+newdel);
      }
   }         

}
