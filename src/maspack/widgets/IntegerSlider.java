/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import javax.swing.*;
import javax.swing.event.*;

import maspack.util.IntegerInterval;
import maspack.util.NumericInterval;
import maspack.util.NumericIntervalRange;
import maspack.util.Range;
import maspack.util.StringHolder;
import maspack.properties.*;
import maspack.util.IntHolder;

public class IntegerSlider extends LabeledControl {
   private static final long serialVersionUID = 1L;
   JSlider mySlider;
   int myNumMajorTicks;
   int myNumMinorTicks;
   boolean mySliderMasked = false;
   IntHolder myHolder;
   Object myValue = Property.VoidValue;
   boolean myAutoClipP = true;

   protected IntegerInterval mySliderRange = new IntegerInterval();
   private static NumericInterval myDefaultSliderRange = new IntegerInterval();

   public static PropertyList myProps =
      new PropertyList (IntegerSlider.class, LabeledControl.class);

   static {
      myProps.add ("sliderRange * *", "slider range",
                   myDefaultSliderRange, "%d");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates an IntSlider with specified label text, initial value, and range.
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
   public IntegerSlider (String labelText, int initialValue, int min, int max) {
      super (labelText);
      initialize (min, max);
      setValue (initialValue);
      updateSlider();
   }

   /**
    * Creates an IntSlider with specified label text and range.
    * 
    * @param labelText
    * text for the control label
    * @param min
    * minimum value for this control
    * @param max
    * maximum value for this control
    */
   public IntegerSlider (String labelText, int min, int max) {
      super (labelText);
      initialize (min, max);
      setValue (min);
      updateSlider();
   }

   private void initialize (int min, int max) {
      mySlider = new Slider (0, 10000);
      mySlider.setMinimum (min);
      mySlider.setMaximum (max);
      addMajorComponent (mySlider);
      mySlider.addChangeListener (new ChangeListener() {
         public void stateChanged (ChangeEvent e) {
            if (!mySliderMasked) {
               updateValue (mySlider.getValue());
            }
         }
      });
   }

   /**
    * Returns the current result holder for this control.
    * 
    * @return result holder
    * @see #setResultHolder
    */
   public IntHolder getResultHolder() {
      return myHolder;
   }

   /**
    * Sets the result holder for this control, into which updated values are
    * copied. No copying is performed if the result holder is set to null.
    * 
    * @param holder
    * new result holder for this control
    * @see #getResultHolder
    */
   public void setResultHolder (IntHolder holder) {
      myHolder = holder;
   }

   private void updateSlider() {
      mySliderMasked = true;
      if (myValue == Property.VoidValue) {
         mySlider.setValue ((getMinimum() + getMaximum()) / 2);
      }
      else {
         mySlider.setValue (getIntValue());
      }
      mySliderMasked = false;
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

   private int getMinimum() {
      return (int)mySliderRange.getLowerBound();
   }

   private int getMaximum() {
      return (int)mySliderRange.getUpperBound();
   }

   /**
    * Sets the minimum and maximum values associated with this control.
    * 
    * @param min
    * minimum value
    * @param max
    * maximum value
    * @see #getMaximum
    * @see #getMinimum
    */
   public void setSliderRange (int min, int max) {
      if (min > max) { // Not sure this is the best way to handle this
         max = min = (max + min) / 2;
      }
      mySlider.setMaximum (max);
      mySlider.setMinimum (min);
      mySliderRange.set (min, max);
      if (myValue instanceof Number) {
         int ivalue = getIntValue();
         if (ivalue > max) {
            setValue (max);
            return;
         }
         else if (ivalue < min) {
            setValue (min);
            return;
         }
      }
      updateDisplay();
   }

   public void setSliderRange (NumericInterval range) {
      setSliderRange ((int)Math.round(range.getLowerBound()),
                      (int)Math.round(range.getUpperBound()));
   }

   public NumericInterval getSliderRange() {
      return new IntegerInterval (mySliderRange);
   }

   public Range getSliderRangeRange() {
      return new NumericIntervalRange();
   }

   /**
    * Updates the control display to reflect the current internl value.
    */
   protected void updateDisplay() {
      updateSlider();
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      value = validateBasic (value, Number.class, errMsg);
      if (value == Property.IllegalValue) {
         return value;
      }
      if (value instanceof Number) {
         value = mySliderRange.validate ((Number)value, myAutoClipP, errMsg);
         return value == Range.IllegalValue ? Property.IllegalValue : value;
      }
      else {
         return validValue (value, errMsg);
      }
   }

   /**
    * Updates the internal representation of the value, updates any result
    * holders, and returns true if the new value differs from the old value.
    */
   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (myValue, value)) {
         if (value instanceof Number) {
            int newValue = ((Number)value).intValue();
            if (myHolder != null) {
               myHolder.value = newValue;
            }
         }
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   protected Object getInternalValue() {
      return myValue;
   }

   private int clipValueToRange (int value) {
      if (value < mySlider.getMinimum()) {
         value = mySlider.getMinimum();
      }
      if (value > mySlider.getMaximum()) {
         value = mySlider.getMaximum();
      }
      return value;
   }

   public int getIntValue() {
      if (myValue instanceof Number) {
         return ((Number)myValue).intValue();
      }
      else {
         return 0;
      }
   }

   public double getDoubleValue() {
      if (myValue instanceof Number) {
         return ((Number)myValue).doubleValue();
      }
      else {
         return 0;
      }
   }

//    public static IntRange computeRange (int min, int max) {
//       if (max >= 0) {
//          int newmax = (int)Math.round(DoubleSlider.roundUp125 (max));
//          int newdel = (int)Math.round(DoubleSlider.roundUp125 (newmax-min));
//          return new IntRange (newmax-newdel, newmax);
//       }
//       else {
//          int newmin = (int)Math.round(DoubleSlider.roundDown125 (min));
//          int newdel = (int)Math.round(DoubleSlider.roundUp125 (max-newmin));
//          return new IntRange (newmin, newmin+newdel);
//       }
//    }

}
