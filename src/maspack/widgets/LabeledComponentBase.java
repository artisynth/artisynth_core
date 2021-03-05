/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Component;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import maspack.properties.*;
import maspack.util.Disposable;

public abstract class LabeledComponentBase extends JPanel
   implements HasProperties, LabeledWidget, Disposable, ActionListener {
   private static final long serialVersionUID = 1L;
   protected boolean myScanningP = false;
   //protected int myPrelabelSpacing = 0;
   //protected int myLabelWidth = 0;

   public static PropertyList myProps =
      new PropertyList (LabeledComponentBase.class);

   static {
      myProps.add ("labelText * *", "label for this component", "", "1E");
      myProps.add (
         "labelFontColor", "font color for the component label", null);
      myProps.add (
         "backgroundColor", "background color for this component", null);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * {@inheritDoc}
    */
   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   /**
    * Queries if this component is in the process of being scanned.
    * 
    * @return true if component is being scanned
    */
   public boolean isScanning() {
      return myScanningP;
   }
   
   /**
    * Sets whether or not this component is being scanned.
    * 
    * @param scanning if {@code true}, component is being scanned
    */
   public void setScanning (boolean scanning) {
      myScanningP = scanning;
   }
   
   public Color getLabelFontColor() {
      Color color;
      JLabel label = getLabel();
      if (label != null) {
         color = (label.isForegroundSet() ?
                  label.getForeground() : null);
      }
      else {
         color = null;
      }
      return color;
   }

   public void setLabelFontColor (Color color) {
      JLabel label = getLabel();
      if (label != null) {
         label.setForeground (color);
      }
   }

   public void setLabelFontColor (float[] color) {
      setLabelFontColor (new Color (color[0], color[1], color[2]));
   }

   public Color getBackgroundColor() {
      Component comp = getMainComponent();
      if (mySelectedP) {
         return mySavedBackground;
      }
      else {
         return (isBackgroundSet() ? comp.getBackground() : null);
      }
   }

   public void setBackgroundColor (Color color) {
      Component comp = getMainComponent();
      if (mySelectedP) {
         mySavedBackground = color;
         if (color == null) {
            comp.setBackground (null);
            color = comp.getBackground();
         }
         comp.setBackground (color.darker());
      }
      else {
         comp.setBackground (color);
      }
   }

   protected abstract JLabel getLabel ();

   protected abstract Component getMainComponent ();

   private Color mySavedBackground;
   private boolean mySelectedP = false;

   public void setSelected (boolean selected) {
      Component comp = getMainComponent();
      if (mySelectedP != selected) {
         if (selected) { // set selected background
            mySavedBackground = getBackgroundColor();
            comp.setBackground (comp.getBackground().darker());
         }
         else { // set unselected background
            comp.setBackground (mySavedBackground);
         }
         mySelectedP = selected;
      }
   }

   public boolean isSelected() {
      return mySelectedP;
   }

   SizableLabel myLabel;
   ArrayList<Component> myComponents = new ArrayList<Component>();

   /**
    * Sets the label text associated with this component.
    * 
    * @param text
    * new label text
    * @see #getLabelText
    */
   public void setLabelText (String text) {
      JLabel label = getLabel();
      if (label != null) {
         label.setText (text);
         alignAllLabels(this);
      }
   }

   /**
    * Returns the label text associated with this component.
    * 
    * @return label text
    * @see #setLabelText
    */
   public String getLabelText() {
      JLabel label = getLabel();
      return label != null ? label.getText() : null;
   }

   public void dispose() {
   }

   protected void finalize() throws Throwable {
      dispose();
      super.finalize();
   }

   static void alignAllLabels (LabeledComponentBase comp) {
      // get the topmost LabeledComponentPanel
      LabeledComponentPanel topPanel = null;
      for (Component c=comp.getParent(); c!=null; c=c.getParent()) {
         if (c instanceof LabeledComponentPanel) {
            topPanel = (LabeledComponentPanel)c;
         }
      }
      if (topPanel != null) {
         topPanel.resetLabelAlignment();
      }
   }      

   public ArrayList<String> getActions() {
      return new ArrayList<String>();
   }

   public void actionPerformed (ActionEvent e) {
   }

   public static int getLeftInset (JComponent comp) {
      Insets insets = comp.getInsets();
      if (insets != null) {
         return insets.left;
      }
      else {
         return 0;
      }
   }
}
