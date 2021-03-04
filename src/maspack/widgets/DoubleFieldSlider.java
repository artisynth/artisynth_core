/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Color;

import javax.swing.*;
import javax.swing.event.*;

import maspack.util.DoubleInterval;
import maspack.util.NumberFormat;
import maspack.util.NumericInterval;
import maspack.util.NumericIntervalRange;
import maspack.util.Range;
import maspack.properties.*;

import java.util.Hashtable;

//
// Notes on setting a slider's range when the slider is attached to a property:
// In general, we may want to adjust the slider's range when:
//
// * the slider is created (done by the PropertyWidget class)
// * a new value is entered (done inside updateInternalValue)
// * when the property's range changes (done in PropertyWidget.updateValue)
//
// In general, we for a property connected to a slider:
//
// (a) a range interval, determined by property's range, and which
//     equals the getRange() values for the slider
// (b) a sliderRange which is a sub-interval of the range
// 
// When determining a sliderRange automatically, we can use the
// following rules:
//
// (a) If the range is bounded, then we use the range.
// (b) If the range is bounded from one end, then we set the
//     the other end so that the current value is 20% of the
//     way there - unless the current value equals the lower
//     bound, in which case we assume the interval has a length of 1
// (c) If the range is open on both ends, we set the lower bound
//     to 0 if the value > 0 and proceed as in (b), otherwise
//     if the value = 0 we assume [0,1], otherwise if value < 0
//     we use [-hi,hi] where hi = 5*abs(value).
//
// autoComputeRange - computes range from existing value and an optional range
//     -> "autofit slider range"
//     -> updateIntervalValue when range is unbounded and value is close
//        to lower bound but within half a slider increment
//     -> PropertyWidget.getNumericRange - called when we create a widget
// autoFitRange - rounds the range to round numbers
//     -> updateIntervalValue to extend a range
//
public class DoubleFieldSlider extends DoubleField
   implements NumericFieldSlider {
   private static final long serialVersionUID = 1L;

   private static NumericInterval myDefaultSliderRange =
      new DoubleInterval (0, 100);

   JSlider mySlider;
   boolean mySliderMasked = false;
   boolean myAutoRangingP = true;
   double myMajorTickSpacing = 0;
   double myMinorTickSpacing = 0;
   double myRoundingTol = 0;
   DoubleInterval mySliderRange = new DoubleInterval(myDefaultSliderRange);
   private static final double INF = Double.POSITIVE_INFINITY;

   private static final int SLIDER_INTERNAL_RANGE = 10000;
   
   public static PropertyList myProps =
      new PropertyList (DoubleFieldSlider.class, DoubleField.class);

   static {
      myProps.add ("sliderRange", "slider range", myDefaultSliderRange, "%.6g");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   

   private static String automaticFormat (double min, double max) {
      // assume 6 places of precision
      if (min == 0 && max == 0) {
         return "%.6g";
      }
      String fmt = null;
      int precision = 6;
      int leadingDigitPos;
      double log10Mag = Math.log10 (Math.max (Math.abs (min), Math.abs (max)));
      if (log10Mag > 0) {
         leadingDigitPos = (int)log10Mag + 1;
      }
      else {
         leadingDigitPos = (int)log10Mag;
      }
      if (leadingDigitPos >= precision) {
         fmt = "%" + (leadingDigitPos + 3) + ".1f";
      }
      else if (leadingDigitPos >= 0) {
         fmt = "%8." + (precision - leadingDigitPos) + "f";
      }
      else {
         fmt = ("%" + (precision - leadingDigitPos) + "."
         + (precision - leadingDigitPos - 2) + "f");
      }
      return fmt;
   }

   /**
    * Creates a DoubleFieldSlider with an empty label and a range of 0 to 1.
    */
   public DoubleFieldSlider() {
      this ("", 0, 100);
   }

   /**
    * Creates a DoubleFieldSlider with specified label text, initial value, and
    * range.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial double value
    * @param min
    * minimum value for this control
    * @param max
    * maximum value for this control
    */
   public DoubleFieldSlider (
      String labelText, double initialValue, double min, double max) {
      this (labelText, initialValue, min, max, automaticFormat (min, max));
   }

   /**
    * Creates a DoubleFieldSlider with specified label text, initial value,
    * range, and format for converting numeric values to text.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial double value
    * @param min
    * minimum value for this control
    * @param max
    * maximum value for this control
    * @param fmtStr
    * format string (printf style; see {@link #setFormat setFormat})
    */
   public DoubleFieldSlider (
      String labelText, double initialValue, double min, 
      double max, String fmtStr) {
      super (labelText, initialValue, fmtStr);
      addSlider (min, max);
   }

   /**
    * Creates a DoubleFieldSlider with specified label text and range.
    * 
    * @param labelText
    * text for the control label
    * @param min
    * minimum value for this control
    * @param max
    * maximum value for this control
    */
   public DoubleFieldSlider (String labelText, double min, double max) {
      super (labelText);
      setFormat (automaticFormat (min, max));
      addSlider (min, max);
   }

   // Return the slider value that corresponds approximately to half a pixel
   protected int halfPixelValue () {
      int width = mySlider.getSize().width;
      if (width == 0) {
         width = 200; // assume 200 until the slider actually gets sized
      }
      return SLIDER_INTERNAL_RANGE/(2*width);
   }

   protected void addSlider (double min, double max) {
      mySlider = new Slider (0, SLIDER_INTERNAL_RANGE);
      setSliderRange (min, max);
      //      mySlider.setBorder (BorderFactory.createLineBorder (Color.RED));
      addMajorComponent (mySlider);
      mySlider.addChangeListener (new ChangeListener() {
         public void stateChanged (ChangeEvent e) {
            if (!mySliderMasked) {
               double value = toVirtualValue (mySlider.getValue());
               if (myRoundingTol != 0) {
                  value = myRoundingTol * Math.round (value / myRoundingTol);
               }
               myAutoRangingP = false;
               if (updateValue (value)) {
                  DoubleFieldSlider.super.updateDisplay();
               }
               myAutoRangingP = true;
            }
         }
      });
      updateSlider();
   }

   public void setAutoRangingEnabled (boolean enable) {
      myAutoRangingP = enable;
   }

   public boolean getAutoRangingEnabled () {
      return myAutoRangingP;
   }

   private double sliderValueRatio() {
      return ((mySliderRange.getUpperBound() - mySliderRange.getLowerBound()) /
              (double)(mySlider.getMaximum() - mySlider.getMinimum()));
   }

   private int toSliderValue (double value) {
      return (mySlider.getMinimum() +
              (int)Math.round ((value - mySliderRange.getLowerBound())
                               / sliderValueRatio()));
   }

   private double toVirtualValue (int sval) {
      return mySliderRange.getLowerBound() + sliderValueRatio()
      * (sval - mySlider.getMinimum());
   }

   private void updateSlider() {
      if (mySlider != null) {
         mySliderMasked = true;
         mySlider.setValue (toSliderValue (getDoubleValue()));
         mySliderMasked = false;
      }
   }

   protected void updateDisplay() {
      super.updateDisplay();
      updateSlider();
   }

   public double getRoundingTolerance() {
      return myRoundingTol;
   }

   public void setRoundingTolerance (double tol) {
      myRoundingTol = tol;
   }

   public boolean getPaintTicks() {
      return mySlider.getPaintTicks();
   }

   public void setPaintTicks (boolean enable) {
      mySlider.setPaintTicks (enable);
   }

   public void setMajorTickSpacing (double inc) {
      mySlider.setMajorTickSpacing ((int)Math.round (inc / sliderValueRatio()));
      myMajorTickSpacing = inc;
   }

   public double getMajorTickSpacing() {
      return myMajorTickSpacing;
   }

   public void setMinorTickSpacing (double inc) {
      mySlider.setMinorTickSpacing ((int)Math.round (inc / sliderValueRatio()));
      myMinorTickSpacing = inc;
   }

   public double getMinorTickSpacing() {
      return myMinorTickSpacing;
   }

   public void setLabels (String fmtStr, double inc) {
      Hashtable<Integer,JComponent> labels =
         new Hashtable<Integer,JComponent>();
      NumberFormat fmt = new NumberFormat (fmtStr);

      double x = mySliderRange.getLowerBound();
      int sliderVal;
      while ((sliderVal = toSliderValue (x)) <= mySlider.getMaximum()) {
         JLabel label = new JLabel (fmt.format (x).trim());
         label.setHorizontalTextPosition (SwingConstants.CENTER);
         labels.put (new Integer (sliderVal), label);
         x += inc;
      }
      mySlider.setLabelTable (labels);
      mySlider.setPaintLabels (true);
   }

   /**
    * Returns the JSlider associated with this control.
    * 
    * @return slider for this control
    */
   public JSlider getSlider() {
      return mySlider;
   }

   /**
    * Set the slider range for this control.
    * 
    * @param min
    * minimum value
    * @param max
    * maximum value
    * @see #getSliderRange
    */
   public void setSliderRange (double min, double max) {
      if (min > max) {
         throw new IllegalArgumentException (
            "Upper range is less than lower range");
      }
      if (max > myRange.getUpperBound()) {
         max = myRange.getUpperBound();
      }
      if (min < myRange.getLowerBound()) {
         min = myRange.getLowerBound();
      }
      mySliderRange.set (min, max);
      if (!isScanning()) {
         // scanning will set the format by itself
         setFormat (automaticFormat (min, max));
      }
      clipValueToRange (mySliderRange);
      updateSlider();
   }

   public void setSliderRange (NumericInterval range) {
      setSliderRange (range.getLowerBound(), range.getUpperBound());
   }

   public NumericInterval getSliderRange() {
      return new DoubleInterval (mySliderRange);
   }

   public Range getSliderRangeRange () {
      return new NumericIntervalRange (myRange);
   }

//   /**
//    * Validates slider range settings. This method will usually be called (by
//    * the property code) through the reflection mechanism, so it may not have
//    * any explicit references.
//    */
//   public Object validateSliderRange (
//      NumericInterval range, StringHolder errMsg) {
//      if (range.getLowerBound() > range.getUpperBound()) {
//         return PropertyUtils.illegalValue (
//            "minimum must not be greater than maximum", errMsg);
//      }
//      if (range.getUpperBound() > myRange.getUpperBound() ||
//          range.getLowerBound() < myRange.getLowerBound()) {
//         return PropertyUtils.illegalValue (
//            "slider range must fit within hard range", errMsg);
//      }
//      return PropertyUtils.validValue (range, errMsg);
//   }

   public void setRange (NumericInterval range) {
      if (!range.equals (myRange)) {
         super.setRange (range);
         double upper = myRange.getUpperBound();
         if (upper != INF) {
            if (mySliderRange.getUpperBound() > upper) {
               mySliderRange.setUpperBound (upper);
            }
         }
         double lower = myRange.getLowerBound();
         if (lower != -INF) {
            if (mySliderRange.getLowerBound() < lower) {
               mySliderRange.setLowerBound (lower);
            }
         }
         clipValueToRange (mySliderRange);
         updateSlider();
      }
   }

   // assume 25 corresponds to half a slider increment (since
   // slider range is 10000 and typical length in pixels is 200
   private int halfPixelValue = 25;

   protected boolean updateInternalValue (Object value) {
      if (super.updateInternalValue (value)) {
         if ( value instanceof Number && mySliderRange != null) {

            double newValue = ((Number)value).doubleValue();
            int sliderValue = toSliderValue(newValue);
            int halfPixel = halfPixelValue();
            if (myAutoRangingP) {
               double max = mySliderRange.getUpperBound();
               double min = mySliderRange.getLowerBound();
               // adjust the upper or lower slider bounds, if necessary
               DoubleInterval newRange = null;

               if (newValue > max) {
                  newRange = new DoubleInterval (min, newValue);
               }
               else if (newValue < min) {
                  newRange = new DoubleInterval (newValue, max);
               }
               if (newRange != null) {
                  newRange = SliderRange.roundBoundsTo125 (newRange);
                  newRange.intersect (getRange()); // make sure hard range OK
                  setSliderRange (newRange);
               }
               // also, reset range if slider position is at the lowest
               // value but we are not actually at the lowest range value,
               // and if this widget does not have hard bounds
               if (!myRange.isBounded() && mySlider != null) {
                  if (sliderValue < halfPixel && newValue > min) {
                     double newMax = 
                        SliderRange.roundUp125 (min+4*(newValue-min));
                     setSliderRange (new DoubleInterval (min, newMax));
                  }
               }
            }
            if (sliderValue < -halfPixel ||
                sliderValue > SLIDER_INTERNAL_RANGE+halfPixel) {
               mySlider.setBackground (Color.GRAY);
            }
            else {
               mySlider.setBackground (null);
            }
         }
         return true;
      }
      else {
         return false;
      }
   }      

}
