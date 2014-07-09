/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.util.Hashtable;

import java.io.*;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import maspack.util.*;
import maspack.properties.*;

public class DoubleSlider extends LabeledControl {
   private static final long serialVersionUID = 1L;
   JSlider mySlider;
   boolean mySliderMasked = false;
   double myMajorTickSpacing = 0;
   double myMinorTickSpacing = 0;
   double myRoundingTol = 0;
   DoubleHolder myHolder;
   Object myValue = Property.VoidValue;
   private static double inf = Double.POSITIVE_INFINITY;

   protected DoubleInterval mySliderRange = new DoubleInterval (-inf, inf);
   private static NumericInterval mySliderDefaultRange = 
      new DoubleInterval (-inf, inf);

   boolean myAutoClipP = true;

   public static PropertyList myProps =
      new PropertyList (DoubleSlider.class, LabeledControl.class);

   static {
      myProps.add ("sliderRange * *", "slider range",
                   mySliderDefaultRange, "%.6g");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a DoubleSlider with specified label, initial value, and range.
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
   public DoubleSlider (String labelText, double initialValue, double min,
                        double max) {
      super (labelText);
      initialize (min, max);
      setValue (initialValue);
      updateSlider();
   }

   /**
    * Creates a DoubleSlider with specified label text * and range.
    * 
    * @param labelText
    * text for the control label
    * @param min
    * minimum value for this control
    * @param max
    * maximum value for this control
    */
   public DoubleSlider (String labelText, double min, double max) {
      super (labelText);
      initialize (min, max);
      setValue (min);
      updateSlider();
   }

   private void initialize (double min, double max) {
      mySliderRange.set (min, max);

      mySlider = new Slider (0, 10000);
      addMajorComponent (mySlider);
      mySlider.addChangeListener (new ChangeListener() {
         public void stateChanged (ChangeEvent e) {
            if (!mySliderMasked) {
               double value = toVirtualValue (mySlider.getValue());
               if (myRoundingTol != 0) {
                  value = (myRoundingTol * Math.round (value / myRoundingTol));
               }
               updateValue (value);
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
   public DoubleHolder getResultHolder() {
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
   public void setResultHolder (DoubleHolder holder) {
      myHolder = holder;
   }

   private double sliderValueRatio() {
      return ((mySliderRange.getUpperBound() - mySliderRange.getLowerBound()) /
              (double)(mySlider.getMaximum() - mySlider.getMinimum()));
   }

   private int toSliderValue (double value) {
      return (mySlider.getMinimum() +
              (int)Math.round ((value - mySliderRange.getLowerBound()) /
                               sliderValueRatio()));
   }

   private double toVirtualValue (int sval) {
      return mySliderRange.getLowerBound() + sliderValueRatio()
      * (sval - mySlider.getMinimum());
   }

   private void updateSlider() {
      mySliderMasked = true;
      mySlider.setValue (toSliderValue (getDoubleValue()));
      mySliderMasked = false;
   }

   public double getDoubleValue() {
      if (myValue instanceof Number) {
         return ((Number)myValue).doubleValue();
      }
      else {
         return 0;
      }
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
    * Sets the minimum and maximum values associated with this control.
    * 
    * @param min
    * minimum value
    * @param max
    * maximum value
    * @see #getSliderRange
    */
   public void setSliderRange (double min, double max) {
      if (min > max) { // Not sure this is the best way to handle this
         max = min = (max + min) / 2;
      }
      mySliderRange.set (min, max);
      if (myValue instanceof Number) {
         double dvalue = getDoubleValue();
         if (dvalue > max) {
            setValue (max);
         }
         else if (dvalue < min) {
            setValue (min);
         }
      }
      else {
         updateDisplay();
      }
   }

   public void setSliderRange (NumericInterval range) {
      setSliderRange (range.getLowerBound(), range.getUpperBound());
   }

   public NumericInterval getSliderRange() {
      return new DoubleInterval (mySliderRange);
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
            double newValue = ((Number)value).doubleValue();
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


}
