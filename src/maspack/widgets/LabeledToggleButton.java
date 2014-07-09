/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import maspack.util.BooleanHolder;
import maspack.util.StringHolder;

public class LabeledToggleButton extends LabeledControl {
   private static final long serialVersionUID = 1L;
   
   private JToggleButton toggleBtn;
   private Boolean myValue = false;
   BooleanHolder myHolder;

   public LabeledToggleButton (String labelText) {
      super (labelText, new JToggleButton());
      initialize (labelText);
      setValue (false);
   }
   
   public LabeledToggleButton (String labelText, boolean initialValue) {
      super (labelText, new JToggleButton());
      initialize (labelText);
      setValue (initialValue);
   }
   
   public LabeledToggleButton (String labelText, boolean initialValue, 
                               Icon defaultIcon, Icon selectedIcon) {
      super (labelText, new JToggleButton());
      initialize (labelText);
      setValue (initialValue);
      
      setIcon (defaultIcon);
      setSelectedIcon (selectedIcon);
   }
   
   private void initialize (String labelText) {
      toggleBtn = (JToggleButton) getMajorComponent (labelText == null ? 0 : 1);
      toggleBtn.setHorizontalAlignment (SwingConstants.LEFT);
      updateDisplay();
      toggleBtn.setActionCommand ("set");
      toggleBtn.addActionListener (this);
      toggleBtn.setMargin (new Insets (0, 0, 0, 0));
      toggleBtn.setBorder (BorderFactory.createLineBorder (Color.darkGray));
   }
   
   public JToggleButton getToggleButton() {
      return toggleBtn;
   }
   
   public void setIcon (Icon icon) {
      toggleBtn.setIcon (icon);
   }

   public void setSelectedIcon (Icon icon) {
      toggleBtn.setSelectedIcon (icon);
   }
   
   public Boolean getBooleanValue() {
      return myValue;
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
   
   protected void updateDisplay() {
      toggleBtn.setSelected (myValue);
   }

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
         myValue = (Boolean) value;
         return true;
      }
      else {
         return false;
      }
   }

   protected Object getInternalValue () {
      return myValue;
   }
   
   public void actionPerformed (ActionEvent e) {
      String command = e.getActionCommand();
      if (command.equals ("set")) {
         if (updateValue (toggleBtn.isSelected())) {
            updateDisplay ();
         }
      }
   }
}
