/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import maspack.util.BooleanHolder;
import maspack.util.StringHolder;
import maspack.properties.Property;

public class BooleanSelector extends LabeledControl implements ActionListener {
   private static final long serialVersionUID = 1L;
   Object myValue = Property.VoidValue;
   BooleanHolder myHolder;
   JToggleButton myCheckBox;

   static Icon checkMarkIcon =
      GuiUtils.loadIcon (BooleanSelector.class, "icons/CheckMark.gif");

   static Icon questionMarkIcon =
      GuiUtils.loadIcon (BooleanSelector.class, "icons/QuestionMark.gif");

   /**
    * Creates a BooleanSelector with an empty label.
    */
   public BooleanSelector() {
      this ("");
   }

   /**
    * Creates a BooleanSelector with specified label text and initial value.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial boolean value
    */
   public BooleanSelector (String labelText, boolean initialValue) {
      super (labelText, new JToggleButton());
      initialize (labelText);
      setValue (initialValue);
   }

   /**
    * Creates a BooleanSelector with specified label text.
    * 
    * @param labelText
    * text for the control label
    */
   public BooleanSelector (String labelText) {
      super (labelText, new JToggleButton());
      initialize (labelText);
      setValue (false);
   }

   private void initialize (String labelText) {
      myCheckBox = (JToggleButton)getMajorComponent (labelText == null ? 0 : 1);
      myCheckBox.setHorizontalAlignment (SwingConstants.LEFT);
      updateCheckBox();
      myCheckBox.setActionCommand ("set");
      myCheckBox.addActionListener (this);
      myCheckBox.setMargin (new Insets (0, 0, 0, 0));
      myCheckBox.setBorder (BorderFactory.createLineBorder (Color.darkGray));
      GuiUtils.setFixedSize (myCheckBox, 13, 13);
   }

   /**
    * Returns the current result holder for this control.
    * 
    * @return result holder
    * @see #setResultHolder
    */
   public BooleanHolder getResultHolder() {
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
   public void setResultHolder (BooleanHolder holder) {
      myHolder = holder;
   }

   protected void updateCheckBox() {
      if (myValue == Property.VoidValue) {
         myCheckBox.setSelected (false);
         myCheckBox.setIcon (questionMarkIcon);
      }
      else {
         boolean value = getBooleanValue();
         myCheckBox.setSelected (value);
         myCheckBox.setIcon (value ? checkMarkIcon : null);
      }
   }

   public void setValue (boolean value) {
      setValue ((Boolean)value);
   }

   public boolean getBooleanValue() {
      if (myValue instanceof Boolean) {
         return ((Boolean)myValue).booleanValue();
      }
      else {
         return false;
      }
   }

   /**
    * Returns the JToggleButton associated with this control.
    * 
    * @return check box for this control
    */
   public JToggleButton getCheckBox() {
      return myCheckBox;
   }

   public void actionPerformed (ActionEvent e) {
      String command = e.getActionCommand();
      if (command.equals ("set")) {
         if (updateValue (myCheckBox.isSelected())) {
            updateDisplay();
         }
      }
   }

   protected Object getInternalValue() {
      return myValue;
   }

   // protected void setInternalValue (Object value)
   // {
   // myValue = value;
   // }

   protected Object validateValue (Object value, StringHolder errMsg) {
      return validateBasic (value, Boolean.class, errMsg);
   }

   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (myValue, value)) {
         if (value instanceof Boolean) {
            if (myHolder != null) {
               myHolder.value = getBooleanValue();
            }
         }
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   protected void updateDisplay() {
      updateCheckBox();
   }

}
