/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Color;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyUtils;
import maspack.util.DoubleInterval;
import maspack.util.IntegerInterval;
import maspack.util.NumericInterval;
import maspack.util.NumericIntervalRange;
import maspack.util.Range;
import maspack.util.StringHolder;

public class IntegerFieldSlider extends IntegerField
   implements NumericFieldSlider {
   private static final long serialVersionUID = 1L;
   JSlider mySlider;
   boolean mySliderMasked = false;
   boolean myAutoRangingP = true;
   private static final double INF = Double.POSITIVE_INFINITY;
   private static final int SLIDER_INTERNAL_RANGE = 10000;
   
   IntegerInterval mySliderRange = new IntegerInterval();
   private static NumericInterval myDefaultSliderRange =
      new IntegerInterval (0, 100);

   public static PropertyList myProps =
      new PropertyList (IntegerFieldSlider.class, IntegerField.class);

   static {
      myProps.add ("sliderRange", "slider range", myDefaultSliderRange, "%.6g");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setAutoRangingEnabled (boolean enable) {
      myAutoRangingP = enable;
   }

   public boolean getAutoRangingEnabled () {
      return myAutoRangingP;
   } 

   /**
    * Creates a default IntFieldSlider with an empty label and a range of 0 to
    * 100.
    */
   public IntegerFieldSlider() {
      this ("", 0, 100);
   }

   /**
    * Creates an IntFieldSlider with specified label text, initial value, and
    * range.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial integer value
    * @param min
    * minimum value for this control
    * @param max
    * maximum value for this control
    */
   public IntegerFieldSlider (String labelText, int initialValue, int min, int max) {
      this (labelText, initialValue, min, max, "%d");
   }

   /**
    * Creates an IntFieldSlider with specified label text, initial value, range,
    * and format for converting numeric values to text.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial integer value
    * @param min
    * minimum value for this control
    * @param max
    * maximum value for this control
    * @param fmtStr
    * format string (printf style; see {@link #setFormat setFormat})
    */
   public IntegerFieldSlider (String labelText, int initialValue, int min, int max,
   String fmtStr) {
      super (labelText, initialValue, fmtStr);
      addSlider (min, max);
   }

   /**
    * Creates an IntFieldSlider with specified label text and range.
    * 
    * @param labelText
    * text for the control label
    * @param min
    * minimum value for this control
    * @param max
    * maximum value for this control
    */
   public IntegerFieldSlider (String labelText, int min, int max) {
      super (labelText);
      addSlider (min, max);
   }

   private void addSlider (int min, int max) {
      mySlider = new Slider();
      setSliderRange (min, max);
      addMajorComponent (mySlider);
      mySlider.addChangeListener (new ChangeListener() {
         public void stateChanged (ChangeEvent e) {
            if (!mySliderMasked) {
               
               myAutoRangingP = false;
               if (updateValue (mySlider.getValue())) {
                  IntegerFieldSlider.super.updateDisplay();
               }
               myAutoRangingP = true;
            }
         }
      });
      updateSlider();
   }

   private void updateSlider() {
      if (mySlider != null) {
         mySliderMasked = true;
         mySlider.setMaximum ((int)mySliderRange.getUpperBound());
         mySlider.setMinimum ((int)mySliderRange.getLowerBound());
         if (myValue == Property.VoidValue) {
            mySlider.setValue ((getMinimum() + getMaximum()) / 2);
         }
         else {
            mySlider.setValue (getIntValue());
         }
         mySliderMasked = false;
      }
   }

   protected void updateDisplay() {
      super.updateDisplay();
      updateSlider();
   }

   public boolean getPaintTicks() {
      return mySlider.getPaintTicks();
   }

   public void setPaintTicks (boolean enable) {
      mySlider.setPaintTicks (enable);
   }

   public void setMajorTickSpacing (int inc) {
      mySlider.setMajorTickSpacing (inc);
   }

   public int getMajorTickSpacing() {
      return mySlider.getMajorTickSpacing();
   }

   public void setMinorTickSpacing (int inc) {
      mySlider.setMinorTickSpacing (inc);
   }

   public int getMinorTickSpacing() {
      return mySlider.getMinorTickSpacing();
   }

   public void setLabels (int increment) {
      if (increment > 0) {
         mySlider.setLabelTable (mySlider.createStandardLabels (increment));
         mySlider.setPaintLabels (true);
      }
      else {
         mySlider.setPaintLabels (false);
      }
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
    * @see #getMaximum
    * @see #getMinimum
    */
   public void setSliderRange (int min, int max) {
      if (min > max) {
         throw new IllegalArgumentException (
            "Upper range is less than lower range");
      }
      if (max > (int)myRange.getUpperBound()) {
         max = (int)myRange.getUpperBound();
      }
      if (min < (int)myRange.getLowerBound()) {
         min = (int)myRange.getLowerBound();
      }
      mySliderRange.set (min, max);
      clipValueToRange (mySliderRange);
      updateSlider();
   }

   public void setSliderRange (NumericInterval range) {
      setSliderRange ((int)Math.round(range.getLowerBound()),
                      (int)Math.round(range.getUpperBound()));
   }

   public NumericInterval getSliderRange() {
      return new IntegerInterval (mySliderRange);
   }

   public Range getSliderRangeRange () {
      return new NumericIntervalRange (myRange);
   }

   /**
    * Validates slider range settings. This method will usually be called (by
    * the property code) through the reflection mechanism, so it may not have
    * any explicit references.
    */
   public Object validateSliderRange (
      NumericInterval range, StringHolder errMsg) {
      if (range.getLowerBound() > range.getUpperBound()) {
         return PropertyUtils.illegalValue (
            "minimum must not be greater than maximum", errMsg);
      }
      if (range.getUpperBound() > myRange.getUpperBound() ||
          range.getLowerBound() < myRange.getLowerBound()) {
         return PropertyUtils.illegalValue (
            "slider range must fit within hard range", errMsg);
      }
      return PropertyUtils.validValue (range, errMsg);
   }

   /**
    * {@inheritDoc}
    */
   public void setRange (NumericInterval range) {
      if (!range.equals (myRange)) {
         super.setRange (range);
         if (myRange.getUpperBound() != INF) {
            mySliderRange.setUpperBound ((int)myRange.getUpperBound());
         }
         if (myRange.getLowerBound() != -INF) {
            mySliderRange.setLowerBound ((int)myRange.getLowerBound());
         }
         if (mySlider != null) {
            mySlider.setMaximum((int)myRange.getUpperBound());
            mySlider.setMinimum((int)myRange.getLowerBound());
         }
         clipValueToRange (mySliderRange);
         updateSlider();
      }
   }

   // Return the slider value that corresponds approximately to half a pixel
   protected int halfPixelValue () {
      // assume 1% to left
      return (int)(0.01*(mySliderRange.getUpperBound() - mySliderRange.getLowerBound()) 
         + mySliderRange.getLowerBound());
   }
   
   protected boolean updateInternalValue (Object value) {
      if (super.updateInternalValue (value)) {
         if (value instanceof Number && mySliderRange != null) {
            int newValue = ((Number)value).intValue();
            int max = (int)mySliderRange.getUpperBound();
            int min = (int)mySliderRange.getLowerBound();
            if (myAutoRangingP) {
               IntegerInterval newRange = null;
               if (newValue > max) {
                  newRange = new IntegerInterval (min, newValue);
               }
               else if (newValue < min) {
                  newRange = new IntegerInterval (newValue, max);
               }
               if (newRange != null) {
                  DoubleInterval dNewRange = SliderRange.roundBoundsTo125 (newRange);
                  dNewRange.intersect (getRange()); // make sure hard range OK
                  setSliderRange (dNewRange);
               }
               // also, reset range if slider position is at the lowest
               // value but we are not actually at the lowest range value,
               // and if this widget does not have hard bounds
               if (!myRange.isBounded() && mySlider != null) {
                  int halfPixel = halfPixelValue();
                  if (newValue < halfPixel && newValue >= min) {
                     int newMax = 
                        (int)SliderRange.roundUp125 (min+4*(newValue-min));
                     setSliderRange (new IntegerInterval (min, newMax));
                  }
               }
               
            }
            if (newValue < min || newValue > max) {
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
     
