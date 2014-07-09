/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JTextField;

import maspack.properties.PropertyList;
import maspack.util.StringHolder;

/**
 * A LabeledControl that uses a JTextField to input its data.
 */
public class MouseClickField extends LabeledControl {
   private static final long serialVersionUID = 1L;
   
   public static int MAX_COLUMNS = 28;
   
   JTextField myTextField;
   JButton myButton;
   
   int newModifiers;
   int modifiers;
   int lastModifiers;
   boolean myTextBackgroundReversedP = false;
   

   public static PropertyList myProps =
      new PropertyList (MouseClickField.class, LabeledControl.class);

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private class TextMouseClickedListener implements MouseListener {
      boolean pressed = true;
      
      @Override
      public void mouseClicked(MouseEvent e) {}

      @Override
      public void mousePressed(MouseEvent e) {
         myButton.getModel().setArmed(true);
         myButton.getModel().setPressed(true);
         pressed = true;
         setReverseTextBackground(true);
         
         newModifiers = e.getModifiersEx();
         myTextField.setText(MouseEvent.getModifiersExText(newModifiers));
        
      }

      @Override
      public void mouseReleased(MouseEvent e) {
         myButton.getModel().setArmed(false);
         myButton.getModel().setPressed(false);
         if (pressed) {
             setValue(newModifiers);
         }
         setReverseTextBackground(false);
         updateDisplay();
         pressed = false;
      }

      @Override
      public void mouseEntered(MouseEvent e) {}

      @Override
      public void mouseExited(MouseEvent e) {
         pressed = false;
      }
      
   }
   
   /**
    * Returns the JTextField associated with this control.
    * 
    * @return text field for this control
    */
   public JTextField getTextField() {
      return myTextField;
   }

   /**
    * Returns the current text stored in this control's JTextField.
    * 
    * @return current text value
    */
   public String getText() {
      return myTextField.getText();
   }
   
   public JButton getButton() {
      return myButton;
   }

   private boolean focusListenerMasked = false;

   private void setValue(int newModifiers) {
      
      lastModifiers = modifiers;
      modifiers = newModifiers;
      
      if (modifiers != lastModifiers) {
         fireValueChangeListeners(modifiers);
      }
      
   }
   
   public MouseClickField (String label, int ncols) {
      super (label, new JTextField (Math.min(ncols, MAX_COLUMNS)));
      
      myButton = new JButton("click");
      add(myButton);
      
      myTextField = (JTextField)getMajorComponent (label == null ? 0 : 1);
      myTextField.setEditable(false);
      myTextField.setBackground(Color.WHITE);

      myButton.addMouseListener(new TextMouseClickedListener());

   }

   protected void setReverseTextBackground (boolean reverse) {
      if (reverse != myTextBackgroundReversedP) {
         Color bgColor = myTextField.getBackground();
         Color fgColor = myTextField.getForeground();
         myTextField.setForeground (bgColor);
         myTextField.setCaretColor (bgColor);
         myTextField.setBackground (fgColor);
         myTextBackgroundReversedP = reverse;
      }
   }

   /**
    * Gets the number of visible columns in this component's text field.
    * 
    * @return number of columns
    * @see #setColumns
    */
   public int getColumns() {
      return myTextField.getColumns();
   }

   /**
    * Sets the number of visible columns in this component's text field.
    * 
    * @param numc
    * number of columns
    * @see #getColumns
    */
   public void setColumns (int numc) {
      myTextField.setColumns (Math.min(numc, MAX_COLUMNS));
   }
   

   /**
    * Updates the control display to reflect the current internal value.
    */
   protected void updateDisplay() {
      myTextField.setText(MouseEvent.getModifiersExText(modifiers));
   }

   @Override
   protected Object validateValue(Object value, StringHolder errMsg) {
      
      if (value instanceof Integer) {
         return value;
      } else {
         errMsg.value = "Expecting integer";
      }
      return null;
   }

   @Override
   protected boolean updateInternalValue(Object value) {
      if (value instanceof Integer) {
         newModifiers = ((Integer)value).intValue();
         if (newModifiers != modifiers) {
            modifiers = newModifiers;
            return true;
         }
      }
      
      return false;
   }

   @Override
   protected Object getInternalValue() {
      return modifiers;
   }

}
