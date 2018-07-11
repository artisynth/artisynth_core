/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JToggleButton;

import maspack.properties.Property;
import maspack.util.StringHolder;

public class FontField extends LabeledControl {
   
   private static int FONT_DISPLAY_SIZE = 12;
   private static Font DEFAULT_FONT_BASE = new Font(Font.SERIF, Font.PLAIN, FONT_DISPLAY_SIZE);
   private static float DEFAULT_FONT_SIZE = 32;
   private static int DEFAULT_FONT_STYLE = Font.PLAIN;
   
   private static final long serialVersionUID = 1L;
   protected JComboBox<Font> myComboBox;
   protected JToggleButton myBold;
   protected JToggleButton myItalic;
   protected JComboBox<Integer> myFontSize;
   
   Font fontBase = DEFAULT_FONT_BASE;
   float fontSize = DEFAULT_FONT_SIZE;
   int fontStyle = DEFAULT_FONT_STYLE;
   
   Font internal = null;
   protected boolean myMasked;


   public FontField () {
      this("");
   }
   
   public FontField (String label) {
      super(label, new JComboBox<Font>());
      
      myBold = new JToggleButton ("B");
      myBold.setFont (new Font(Font.SERIF, Font.BOLD, myBold.getFont ().getSize ()));
      
      myItalic = new JToggleButton ("I");
      myItalic.setFont (new Font(Font.SERIF, Font.ITALIC, myItalic.getFont ().getSize ()));
      
      myFontSize = new JComboBox<Integer> ();
      int[] fontSizes = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 32, 36, 48, 64, 72};
      myFontSize.setSelectedItem (DEFAULT_FONT_SIZE);
      
      Dimension dim = myFontSize.getPreferredSize ();
      myFontSize.setPreferredSize (new Dimension (48, (int)(dim.getHeight ())));
      for (int i : fontSizes) {
         myFontSize.addItem (i);
      }
      myFontSize.setEditable (true);
      
      super.add (myFontSize);
      super.add (myBold);
      super.add (myItalic);
      
      // add default values
      Font[] systemFonts = GraphicsEnvironment.getLocalGraphicsEnvironment ().getAllFonts ();
      
      ArrayList<Font> fonts = new ArrayList<Font>(systemFonts.length + 3);
      fonts.add ( new Font(Font.SERIF, 0, FONT_DISPLAY_SIZE));
      fonts.add ( new Font(Font.SANS_SERIF, 0, FONT_DISPLAY_SIZE));
      fonts.add ( new Font(Font.MONOSPACED, 0, FONT_DISPLAY_SIZE));
      for (Font f : systemFonts) {
         fonts.add (f.deriveFont ((float)FONT_DISPLAY_SIZE));
      }
      
      @SuppressWarnings("unchecked")
      JComboBox<Font> fontBox = (JComboBox<Font>)getMajorComponent(label == null ? 0 : 1);
      myComboBox = fontBox;
      myComboBox.setRenderer(new DefaultListCellRenderer() {
         private static final long serialVersionUID = -755738510066156775L;
         @Override
         public Component getListCellRendererComponent(JList<?> list,
               Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Font font = null;
            if (value != null) {
               font = (Font) value;
               value = font.getName();
            }
            Component comp = super.getListCellRendererComponent(list, value, index,
               isSelected, cellHasFocus); 
            if (font != null) {
               comp.setFont (font);
            }
            return comp;
         }
      });
      
      for (Font f : fonts)  {
         myComboBox.addItem (f);
      }
      dim = myComboBox.getPreferredSize ();
      myComboBox.setPreferredSize (dim);
      myComboBox.repaint ();
      
      // update display based on current values
      fontBase = fonts.get (0);
      fontSize =  DEFAULT_FONT_SIZE;
      fontStyle = DEFAULT_FONT_STYLE;   
      updateDisplay ();
      
      ActionListener fontUpdate = new ActionListener() {
         
         @Override
         public void actionPerformed (ActionEvent e) {
            if (!myMasked) {
               Font base = (Font)myComboBox.getSelectedItem ();
               int size = (int)((Number)(myFontSize.getSelectedItem ())).intValue ();
               int style = 0;
               if (myBold.isSelected ()) {
                  style |= Font.BOLD;
               }
               if (myItalic.isSelected ()) {
                  style |= Font.ITALIC;
               }
               
               Font value = base.deriveFont (style, size);
               
               Object validValue = validateValue(value, null);
               if (validValue != value) {
                  updateDisplay();
               }
               if (updateValue(validValue)) {
                  updateDisplay();
               }
            }
         }
      };
      
      myComboBox.addActionListener(fontUpdate);
      myComboBox.setFocusable(false);
      
      myFontSize.addActionListener (fontUpdate);
      myFontSize.setFocusable (false);
     
      myBold.addActionListener (fontUpdate);
      myBold.setFocusable (false);
      
      myItalic.addActionListener (fontUpdate);
      myItalic.setFocusable (false);
   }

   /**
    * Returns the JComboBox associated with this control.
    * 
    * @return combo box for this control
    */
   public JComboBox<Font> getComboBox() {
      return myComboBox;
   }

   protected boolean updateInternalValue(Object value) {
      if (!valuesEqual(value, getInternalValue())) {
         internal = (Font)value;
         fontBase = internal.deriveFont (Font.PLAIN, FONT_DISPLAY_SIZE);
         fontSize = internal.getSize2D ();
         fontStyle = internal.getStyle ();
         return true;
      }
      else {
         return false;
      }
   }

   protected Object validateValue(Object value, StringHolder errMsg) {
      value = validateBasic(value, Object.class, errMsg);
      if (value == Property.IllegalValue) {
         return value;
      }
      return validValue(value, errMsg);
   }

   /**
    * Updates the control display to reflect the current internl value.
    */
   protected void updateDisplay() {
      myMasked = true;
      
      myComboBox.setSelectedItem (fontBase);
      myComboBox.setFont (fontBase);
      myFontSize.setSelectedItem (fontSize);
      
      boolean isBold = (fontStyle & Font.BOLD) != 0;
      myBold.setSelected (isBold);
      boolean isItalic = ((fontStyle & Font.ITALIC) != 0);
      myItalic.setSelected (isItalic);
      
      myMasked = false;
   }

   protected Object getInternalValue() {
      return internal;
   }

}
