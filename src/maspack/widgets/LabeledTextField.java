/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

import maspack.util.*;
import maspack.properties.*;

/**
 * A LabeledControl that uses a JTextField to input its data.
 */
public abstract class LabeledTextField extends LabeledControl {
   private static final long serialVersionUID = 1L;
   
   public static int MAX_COLUMNS = 28;
   
   protected JTextField myTextField;
   protected String myLastText = "";
   boolean myTextBackgroundReversedP = false;
   boolean myCaretListenerMasked = false;
   protected NumberFormat myFmt = new NumberFormat (myDefaultFmt);
   protected static String myDefaultFmt = "%.6g";
   protected boolean myAlwaysParseText = false;

   public static PropertyList myProps =
      new PropertyList (LabeledTextField.class, LabeledControl.class);

   static {
      myProps.add ("format", "numeric format string", myDefaultFmt);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private static FormatRange myFormatRange = new FormatRange();

   protected static class TextFieldKeyListener implements KeyListener {

      private static final int CTRL_A = 0x01;
      private static final int CTRL_B = 0x02;
      private static final int CTRL_C = 0x03;
      private static final int CTRL_D = 0x04;
      private static final int CTRL_E = 0x05;
      private static final int CTRL_F = 0x06;
      private static final int CTRL_G = 0x07;
      private static final int CTRL_H = 0x08;
      private static final int CTRL_I = 0x09;
      private static final int CTRL_J = 0x0A;
      private static final int CTRL_K = 0x0B;
      private static final int CTRL_L = 0x0C;
      private static final int CTRL_M = 0x0D;
      private static final int CTRL_N = 0x0E;
      private static final int CTRL_O = 0x0F;
      private static final int CTRL_P = 0x10;
      private static final int CTRL_Q = 0x11;
      private static final int CTRL_R = 0x12;
      private static final int CTRL_S = 0x13;
      private static final int CTRL_T = 0x14;
      private static final int CTRL_U = 0x15;
      private static final int CTRL_V = 0x16;
      private static final int CTRL_W = 0x17;
      private static final int CTRL_X = 0x18;
      private static final int CTRL_Y = 0x19;
      private static final int CTRL_Z = 0x1A;
      private static final int ESC = 0x1B;
      private static final int DEL = 0x7F;

      JTextField myField; 

      public TextFieldKeyListener (JTextField field) {
         myField = field;
      }

      public void keyPressed (KeyEvent e) {
      }

      public void keyReleased (KeyEvent e) {
      }

      public void keyTyped (KeyEvent e) {
         JTextField field = (JTextField)e.getSource();
         switch (e.getKeyChar()) {
            case CTRL_A: {
               field.setCaretPosition (0);
               break;
            }
            case CTRL_F: {
               int idx = field.getCaretPosition ();
               String text = field.getText();
               if (idx < text.length()) {
                  field.setCaretPosition (idx+1);
               }
               break;
            }
            case CTRL_B: {
               int idx = field.getCaretPosition ();
               if (idx > 0) {
                  field.setCaretPosition (idx-1);
               }
               break;
            }
            case CTRL_K: {
               int idx = field.getCaretPosition ();
               String text = field.getText();
               if (idx < text.length()) {
                  field.setText (text.substring (0, idx));
               }
               break;
            }
            case CTRL_U: {
               field.setText ("");
               break;
            }
            default: {
               // do nothing
            }
         }
      }

   }

   /**
    * Parses text that has been received by the text box and returns the
    * associated value. If the text is malformed, then an IllegalValueException
    * should be thrown with an appropriate error message; this will be caught by
    * the widget and used to produce an error message dialog. If the text
    * generates a value that is then corrected to a legal value (such as
    * clipping an integer to lie within its maximum range), then the boolean
    * value in <code>corrected</code> should be set to <code>true</code>
    * 
    * @param text
    * text to generate value for
    * @param corrected
    * set to <code>true</code> if the resulting value has been corrected
    * @return value corresponding to text
    */
   protected abstract Object textToValue (
      String text, BooleanHolder corrected, StringHolder errMsg);

   /**
    * Produces the text for a given value.
    * 
    * @param value
    * value to produce text for
    * @param text
    * for the supplied value
    */
   protected abstract String valueToText (Object value);

   protected void setText (String text) {
      myLastText = text;
      myCaretListenerMasked = true;
      myTextField.setText (text);
      myCaretListenerMasked = false;
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

   protected boolean focusListenerMasked = false;

   protected boolean myLastEntryAccepted = false;

   protected void setValueFromDisplay() {
      if (myAlwaysParseText || !myLastText.equals (myTextField.getText())) {
         StringHolder errMsg = new StringHolder();
         // we explicitly call fireValueCheckListeners instead of
         // checkValue since textToValue may itself throw an error
         // and we want to handle that in the same way
         BooleanHolder corrected = new BooleanHolder();
         Object value = textToValue (myTextField.getText(), corrected, errMsg);
         if (value != Property.IllegalValue) {
            value = validateValue (value, errMsg);
         }
         if (value == Property.IllegalValue) {
            focusListenerMasked = true;
            GuiUtils.showError (LabeledTextField.this, errMsg.value);
            focusListenerMasked = false;
            myTextField.setText (myLastText);
            updateDisplay();
            myLastEntryAccepted = false;
            return;
         }
         updateValue (value);
         updateDisplay();
      }
      else {
         setReverseTextBackground (false);
      }
      myLastEntryAccepted = true;
   }

   /**
    * Returns true if the most recent text entry to this field was accepted,
    * either directly, or by substituting a corrected value.
    */
   public boolean lastEntryWasAccepted() {
      return myLastEntryAccepted;
   }

   public LabeledTextField (String label, int ncols) {
      super (label, new JTextField (Math.min(ncols, MAX_COLUMNS)));
      
      myTextField = (JTextField)getMajorComponent (label == null ? 0 : 1);
      myTextField.setDisabledTextColor (Color.BLACK);

      myTextField.addKeyListener (new TextFieldKeyListener(myTextField));

      myTextField.addActionListener (new ActionListener() {
         public void actionPerformed (ActionEvent e) {
            setValueFromDisplay();
         }
      });

      myTextField.addFocusListener (new FocusListener() {
         public void focusGained (FocusEvent e) {
         }

         public void focusLost (FocusEvent e) {
            if (!focusListenerMasked) {
               setValueFromDisplay();
            }
         }
      });

      myTextField.addCaretListener (new CaretListener() {
         public void caretUpdate (CaretEvent evt) {
            if (!myCaretListenerMasked
            && !myLastText.equals (myTextField.getText())) {
               setReverseTextBackground (true);
            }
         }
      });
      // setTextSizeFixed (false);
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
    * Sets the formatter used to convert numeric values into text. The formatter
    * is specified using a C <code>printf</code> style format string. A space
    * is inserted between values. For a description of the format string syntax,
    * see {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @param fmtStr
    * format specification string
    * @see #getFormat
    * @throws IllegalArgumentException
    * if the format string syntax is invalid
    */
   public void setFormat (String fmtStr) {
      setFormat (new NumberFormat (fmtStr));
   }

   /**
    * Directly sets the formatter used used to convert numeric values into text.
    * 
    * @param fmt
    * numeric formatter
    * @see #getFormat
    * @see #setFormat(String)
    */
   public void setFormat (NumberFormat fmt) {
      myFmt = fmt;
      updateDisplay(/*forceUpdate=*/true);
   }

   public Range getFormatRange () {
      return myFormatRange;
   }

   /**
    * Returns the format string used to convert numeric values into text. See
    * {@link #setFormat setFormat} for more details.
    * 
    * @return numeric formatter
    * @see #setFormat
    */
   public String getFormat() {
      return myFmt.toString();
   }
   
   /**
    * Returns the default format string used to convert numeric values into 
    * text. This the value that will be returned by 
    * {@link #getFormat getFormat()} if {@link #setFormat setFormat()} 
    * has not been called.
    * 
    * @return default numeric formatter
    * @see #setFormat
    */
   public String getDefaultFormat() {
      return myDefaultFmt;
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

   protected void updateDisplay (boolean forceUpdate) {
      String newText = null;
      Object value = getInternalValue();
      BooleanHolder corrected = new BooleanHolder();
      if (value == Property.VoidValue) {
         newText = "";
      }
      else {
         Object textValue =
            textToValue (myTextField.getText(), corrected, null);
         if (forceUpdate ||
             textValue == Property.IllegalValue ||
             corrected.value ||
             !valuesEqual (textValue, value)) {
            newText = valueToText (value);
         }
      }
      if (newText != null) {
         myLastText = newText;
         myTextField.setText (newText);
      }
      else {
         myLastText = myTextField.getText();
      }
      setReverseTextBackground (false);
   }

   /**
    * Updates the control display to reflect the current internal value.
    */
   protected void updateDisplay() {
      updateDisplay (/*forceUpdate=*/false);
   }

   static protected boolean isBlank (String str) {
      for (int i = 0; i < str.length(); i++) {
         if (str.charAt (i) != ' ') {
            return false;
         }
      }
      return true;
   }
   // protected Object validateValue (Object value, StringHolder errMsg)
   // {
   // throw new UnsupportedOperationException (
   // "validateValue not implemented for this control");
   // }

   // /**
   // * Updates the internal representation of the value, updates any result
   // * holders, and returns true if the new value differs from the old value.
   // */
   // protected boolean updateInternalValue (Object value)
   // {
   // return false;
   // }

   // protected Object getInternalValue()
   // {
   // return null;
   // }
}
