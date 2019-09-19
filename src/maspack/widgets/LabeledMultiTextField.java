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
import java.util.Arrays;

import maspack.util.*;
import maspack.properties.Property;

public abstract class LabeledMultiTextField extends LabeledControl {
   private static final long serialVersionUID = 1L;
   private boolean myFocusListenersMasked = false;
   TextFieldInfo[] myFieldInfo;
   protected NumberFormat myFmt;
   protected int myNumFields;
   protected int myNumColumns;

   protected abstract Object textToValue (
      String[] text, boolean[] corrected, StringHolder errMsg);

   protected abstract String[] valueToText (Object value);

   private class TextFieldInfo implements ActionListener, FocusListener,
   CaretListener {
      String myLastText = "";
      JTextField myTextField;
      boolean myBackgroundReversedP = false;
      boolean myCaretListenerMasked = false;
      int myIdx;

      TextFieldInfo (JTextField field, int idx) {
         myTextField = field;
         myIdx = idx;
      }

      public void actionPerformed (ActionEvent e) {
         String[] currentText = getAllCurrentText();
         for (int i = 0; i < currentText.length; i++) {
            if (currentText[i].equals ("") && i != myIdx) {
               return; // some other field contains a void, so no value yet
            }
         }
         setValueFromDisplay (currentText);
      }

      public void focusGained (FocusEvent e) {
      }

      public void focusLost (FocusEvent e) {
         if (!myFocusListenersMasked) {
            String[] currentText = getAllCurrentText();
            for (int i = 0; i < currentText.length; i++) {
               if (currentText[i].equals ("") && i != myIdx) {
                  return; // some other field contains void, so no value yet
               }
            }
            setValueFromDisplay (currentText);
         }
      }

      public void caretUpdate (CaretEvent evt) {
         if (!myCaretListenerMasked
         && !myLastText.equals (myTextField.getText())) {
            setReverseTextBackground (true);
         }
      }

      public void setText (String text) {
         myLastText = text;
         myCaretListenerMasked = true;
         myTextField.setText (text);
         myCaretListenerMasked = false;
      }

      protected void setReverseTextBackground (boolean reverse) {
         if (reverse != myBackgroundReversedP) {
            Color bgColor = myTextField.getBackground();
            Color fgColor = myTextField.getForeground();
            myTextField.setForeground (bgColor);
            myTextField.setCaretColor (bgColor);
            myTextField.setBackground (fgColor);
            myBackgroundReversedP = reverse;
         }
      }
   }

   /**
    * Returns the number of text fields associated with this control.
    * 
    * @return number of text fields
    */
   public int numTextFields() {
      return myNumFields;
   }

   /**
    * Returns a specific JTextField associated with this control.
    * 
    * @param idx
    * index of the text field
    * @return requested text field for this control
    */
   public JTextField getTextField (int idx) {
      if (idx < 0 || idx >= myNumFields) {
         throw new IllegalArgumentException (
            "Text field index must be between 0 and " + (myNumFields - 1));
      }
      return myFieldInfo[idx].myTextField;
   }

   /**
    * Returns the current text stored in a specific JTextField.
    * 
    * @param idx
    * index of the text field
    * @return current text value
    */
   public String getText (int idx) {
      if (idx < 0 || idx >= myNumFields) {
         throw new IllegalArgumentException (
            "Text field index must be between 0 and " + (myNumFields - 1));
      }
      return myFieldInfo[idx].myTextField.getText();
   }

   private void clearReverseTextBackground() {
      for (int i = 0; i < myNumFields; i++) {
         myFieldInfo[i].setReverseTextBackground (false);
      }
   }

   private void setValueFromDisplay (String[] currentText) {
      if (!Arrays.equals (currentText, getAllLastText())) {
         StringHolder errMsg = new StringHolder();
         // we explicitly call fireValueCheckListeners instead of
         // checkValue since textToValue may itself throw an error
         // and we want to handle that in the same way
         boolean[] corrected = new boolean[myNumFields];
         Object value = textToValue (currentText, corrected, errMsg);
         if (value != Property.IllegalValue) {
            value = validateValue (value, errMsg);
         }
         if (value == Property.IllegalValue) {
            myFocusListenersMasked = true;
            GuiUtils.showError (LabeledMultiTextField.this, errMsg.value);
            myFocusListenersMasked = false;
            restoreAllText();
            updateDisplay();
            return;
         }
         updateValue (value);
         updateDisplay();
      }
      else {
         clearReverseTextBackground();
      }
   }

   public LabeledMultiTextField (String label, int nfields, int ncols) {
      this (label, new String[nfields], ncols);
   }

   public LabeledMultiTextField (String label, String[] fieldLabels, int ncols) {
      super (label);
      int nfields = fieldLabels.length;
      myFieldInfo = new TextFieldInfo[nfields];
      myNumFields = nfields;
      myNumColumns = ncols;
      for (int i = 0; i < nfields; i++) {
         JTextField field = new JTextField (ncols);
         TextFieldInfo info = new TextFieldInfo (field, i);
         field.setDisabledTextColor (Color.BLACK);

         field.addActionListener (info);
         field.addFocusListener (info);
         field.addCaretListener (info);

         if (fieldLabels[i] != null) {
            addMajorComponent (new JLabel (fieldLabels[i]));
         }
         addMajorComponent (field);
         myFieldInfo[i] = info;
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
   protected void setFormat (String fmtStr) {
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
   }

   /**
    * Returns the formatter used to convert numeric values into text. See
    * {@link #setFormat setFormat} for more details.
    * 
    * @return numeric formatter
    * @see #setFormat
    */
   protected NumberFormat getFormat() {
      return myFmt;
   }

   /**
    * Gets the number of visible columns in each text field.
    * 
    * @return number of columns in each field
    * @see #setColumns
    */
   public int getColumns() {
      return myNumColumns;
   }

   /**
    * Sets the number of visible columns in each text field.
    * 
    * @param numc
    * number of columns in each field
    * @see #getColumns
    */
   public void setColumns (int numc) {
      myNumColumns = numc;
      for (int i = 0; i < myNumFields; i++) {
         myFieldInfo[i].myTextField.setColumns (numc);
      }
   }

   public String[] getAllCurrentText() {
      String[] allText = new String[myNumFields];
      for (int i = 0; i < myNumFields; i++) {
         allText[i] = myFieldInfo[i].myTextField.getText();
      }
      return allText;
   }

   private String[] getAllLastText() {
      String[] allText = new String[myNumFields];
      for (int i = 0; i < myNumFields; i++) {
         allText[i] = myFieldInfo[i].myLastText;
      }
      return allText;
   }

   private String[] getEmptyText() {
      String[] allText = new String[myNumFields];
      for (int i = 0; i < myNumFields; i++) {
         allText[i] = "";
      }
      return allText;
   }

   private void restoreAllText() {
      for (int i = 0; i < myNumFields; i++) {
         TextFieldInfo info = myFieldInfo[i];
         info.myTextField.setText (info.myLastText);
      }
   }

   private boolean containsCorrection (boolean[] corrected) {
      for (int i = 0; i < corrected.length; i++) {
         if (corrected[i]) {
            return true;
         }
      }
      return false;
   }

   /**
    * Updates the control display to reflect the current internl value.
    */
   protected void updateDisplay() {
      String newText[] = new String[myNumFields];
      Object value = getInternalValue();
      if (value == Property.VoidValue) {
         newText = getEmptyText();
      }
      else {
         boolean[] corrected = new boolean[myNumFields];
         Object textValue = textToValue (getAllCurrentText(), corrected, null);
         if (textValue == Property.IllegalValue
         || !valuesEqual (textValue, value)) {
            newText = valueToText (value);
         }
         else if (containsCorrection (corrected)) {
            newText = valueToText (value);
            for (int i = 0; i < corrected.length; i++) {
               if (!corrected[i]) {
                  newText[i] = null;
               }
            }
         }
      }
      for (int i = 0; i < myNumFields; i++) {
         if (newText[i] != null) {
            myFieldInfo[i].setText (newText[i]);
         }
         else {
            TextFieldInfo info = myFieldInfo[i];
            info.myLastText = info.myTextField.getText();
         }
      }
      clearReverseTextBackground();
   }

   /**
    * Sets the horizontal alignment for all the text fields in this control.
    * 
    * @param align
    * new alignment
    */
   public void setHorizontalAlignment (int align) {
      for (int i = 0; i < myNumFields; i++) {
         myFieldInfo[i].myTextField.setHorizontalAlignment (align);
      }
   }

   /**
    * Gets the horizontal alignment for the text fields in this control.
    * 
    * @return horizontal alignment
    */
   public int getHorizontalAlignment() {
      return myFieldInfo[0].myTextField.getHorizontalAlignment();
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
