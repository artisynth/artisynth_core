/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import maspack.util.StringHolder;
import maspack.properties.Property;

public class ColorSelector extends LabeledControl implements ActionListener {
   private static final long serialVersionUID = 1L;
   protected float[] myHolder;
   Color myVoidColor = new Color (0.9f, 0.9f, 0.9f);
   Object myValue = Property.VoidValue;
   JColorChooser myColorChooser = new JColorChooser();
   JButton mySetButton;
   JButton myClearButton;
   JLabel myColorSwatch;

   static Insets myMargin = new Insets (4, 4, 4, 4);
   static Dimension myButtonSize = new Dimension (50, 25);

   public static Dimension getButtonSize () {
      return new Dimension(myButtonSize);
   }

   static Icon voidColorIcon =
      GuiUtils.loadIcon (ColorSelector.class, "icons/VoidColor.gif");

   protected void invokeColorDialog() {
      myColorChooser.setColor (getColor());
      JDialog dialog =
         JColorChooser.createDialog (
            this, "color chooser", /* modal= */true, myColorChooser, this, this);
      GuiUtils.locateRight (dialog, this);
      dialog.setVisible (true);

   }

   /**
    * Creates a default ColorSelector with an empty label.
    */
   public ColorSelector() {
      this ("");
   }

   /**
    * Creates a ColorSelector with specified label text and initial color.
    * 
    * @param labelText
    * text for the control label
    * @param initialColor
    * initial color value
    */
   public ColorSelector (String labelText, Color initialColor) {
      super (labelText);
      initialize();
      if (initialColor == null) {
         enableNullColors();
      }
      setValue (initialColor);
   }

   /**
    * Creates a ColorSelector with a specified label text.
    * 
    * @param labelText
    * text for the control label
    */
   public ColorSelector (String labelText) {
      super (labelText);
      initialize();
      setValue (myVoidColor);
   }

   private void initialize() {
      myColorSwatch = new JLabel (" ");

      mySetButton = new JButton ("Set");
      mySetButton.setMargin (myMargin);
      GuiUtils.setFixedSize (mySetButton, myButtonSize);

      mySetButton.addActionListener (new ActionListener() {
         public void actionPerformed (ActionEvent e) {
            invokeColorDialog();
         }
      });

      GuiUtils.setFixedSize (myColorSwatch, myButtonSize);
      myColorSwatch.setOpaque (true);
      myColorSwatch.setBorder (BorderFactory.createLineBorder (Color.BLACK));
      myColorSwatch.setHorizontalAlignment (SwingConstants.CENTER);
      myColorSwatch.setBackground (myVoidColor);
      addMajorComponent (myColorSwatch);
      addMajorComponent (mySetButton);
   }

   /**
    * Configures this ColorSelector to accept null Color values.
    */
   public void enableNullColors() {
      if (!getNullValueEnabled()) {
         setNullValueEnabled (true);
         myClearButton = new JButton ("Clear");
         myClearButton.setMargin (myMargin);
         myClearButton.setPreferredSize (myButtonSize);
         myClearButton.setMinimumSize (myButtonSize);
         myClearButton.setMaximumSize (myButtonSize);
         myClearButton.setActionCommand ("clearColor");
         myClearButton.addActionListener (this);
         int addIdx = (getLabel() == null ? 1 : 2);
         addMajorComponent (myClearButton, addIdx);
         updateDisplay();
      }
   }

   public void actionPerformed (ActionEvent e) {
      String command = e.getActionCommand();
      if (command.equals ("clearColor")) {
         updateValue (null);
         updateDisplay();
      }
      else if (command.equals ("OK")) {
         updateValue (myColorChooser.getColor());
         updateDisplay();
      }
   }

   /**
    * Returns the current result holder for this control.
    * 
    * @return result holder
    * @see #setResultHolder
    */
   public float[] getResultHolder() {
      return myHolder;
   }

   /**
    * Sets the result holder for this control, into which updated values are
    * copied. No copying is performed if the result holder is set to null.
    * 
    * @param holder
    * new result holder for this control
    * @see #getResultHolder
    * @throws IllegalArgumentException
    * if holder has a length less than three.
    */
   public void setResultHolder (float[] holder) {
      if (holder.length < 3) {
         throw new IllegalArgumentException ("holder is too small");
      }
      myHolder = holder;
   }

   protected void updateDisplay() {
      if (myValue == Property.VoidValue) {
         myColorSwatch.setBackground (null);
         myColorSwatch.setIcon (voidColorIcon);
         myColorSwatch.setText ("void");
      }
      else if (myValue == null) {
         myColorSwatch.setBackground (myVoidColor);
         myColorSwatch.setIcon (null);
         myColorSwatch.setText ("null");
      }
      else {
         myColorSwatch.setBackground ((Color)myValue);
         myColorSwatch.setIcon (null);
         myColorSwatch.setText ("");
      }
      if (myClearButton != null) {
         myClearButton.setEnabled (myValue instanceof Color);
      }
   }

   public void setColor (float[] rgba) {
      if (rgba == null) {
         setColor ((Color)null);
      }
      else {
         setColor (new Color (rgba[0], rgba[1], rgba[2], rgba[3]));
      }
   }

   public void setColor (Color color) {
      setValue (color);
   }

   public Color getColor() {
      if (myValue instanceof Color) {
         return (Color)myValue;
      }
      else {
         return myVoidColor;
      }
   }

   /**
    * Returns the JLabel used to provide the color swatch for this control.
    * 
    * @return color swatch for this control
    */
   public JLabel getColorSwatch() {
      return myColorSwatch;
   }

   /**
    * Returns the JColorChooser associated with this control.
    * 
    * @return color chooser for this control
    */
   public JColorChooser getColorChooser() {
      return myColorChooser;
   }

   /**
    * Returns the JButton used to initiate color selectiion on this control.
    * 
    * @return color select button for this control
    */
   public JButton getSetButton() {
      return mySetButton;
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      return validateBasic (value, Color.class, errMsg);
   }

   /**
    * Updates the internal representation of the value, updates any result
    * holders, and returns true if the new value differs from the old value.
    */
   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (myValue, value)) {
         if (value != Property.VoidValue) {
            if (myHolder != null) {
               float[] comps;
               if (value == null) {
                  comps = new float[] { -1, -1, -1, -1 };
               }
               else {
                  comps = ((Color)value).getRGBComponents (null);
               }
               for (int i = 0; i < myHolder.length; i++) {
                  myHolder[i] = comps[i];
               }
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
