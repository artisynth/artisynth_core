/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import maspack.properties.*;
import maspack.util.InternalErrorException;

public class PropertyModeButton extends JToggleButton implements ActionListener {
   static Icon inheritedIcon =
      GuiUtils.loadIcon (PropertyModeButton.class, "icons/Inherited.gif");

   static Icon explicitIcon =
      GuiUtils.loadIcon (PropertyModeButton.class, "icons/Explicit.gif");

   static Icon voidModeIcon =
      GuiUtils.loadIcon (PropertyModeButton.class, "icons/VoidMode.gif");

   InheritableProperty myProp;
   LabeledControl myWidget;
   LinkedList<ValueChangeListener> myChangeListeners =
      new LinkedList<ValueChangeListener>();

   public PropertyModeButton() {
      super (voidModeIcon);

      addActionListener (this);
      Dimension size =
         new Dimension (inheritedIcon.getIconWidth(),
                        inheritedIcon.getIconHeight());
      GuiUtils.setFixedSize (this, getButtonSize());
      setBorderPainted (false);
      setActionCommand ("set");
      setSelectedIcon (explicitIcon);
   }

   /**
    * Adds a ValueChangeListener to this button.
    * 
    * @param l
    * listener to add
    */
   public void addValueChangeListener (ValueChangeListener l) {
      myChangeListeners.add (l);
   }

   /**
    * Removes a ValueChangeListener from this button.
    * 
    * @param l
    * listener to remove
    */
   public void removeValueChangeListener (ValueChangeListener l) {
      myChangeListeners.remove (l);
   }

   /**
    * Fire all the ValueChangeListeners associated with this button.
    * 
    * @param value
    * object representation of the new value
    */
   protected void fireValueChangeListeners (Object value) {
      for (ValueChangeListener l : myChangeListeners) {
         l.valueChange (new ValueChangeEvent (this, value));
      }
   }

   public static Dimension getButtonSize() {
      return new Dimension (inheritedIcon.getIconWidth(),
                            inheritedIcon.getIconHeight());
   }

   public PropertyModeButton (LabeledControl widget, InheritableProperty prop) {
      this();
      setWidget (widget);
      setProperty (prop);
   }

   public void setMode (PropertyMode mode) {
      switch (mode) {
         case Explicit: {
            setSelected (true);
            break;
         }
         case Inherited:
         case Void: {
            setSelected (false);
            break;
         }
         default: {
            throw new InternalErrorException ("unimplemented mode " + mode);
         }
      }
      updateDisplay (mode);
   }

   private void updateDisplay (PropertyMode mode) {
      switch (mode) {
         case Inherited: {
            setIcon (inheritedIcon);
            break;
         }
         case Explicit: {
            break;
         }
         case Void: {
            setIcon (voidModeIcon);
            break;
         }
         default: {
            throw new InternalErrorException ("unimplemented mode " + mode);
         }
      }
      if (myWidget != null) {
         PropertyWidget.setModeAppearance (
            myWidget, myWidget.getParent(), mode);
      }
   }

   public InheritableProperty getProperty() {
      return myProp;
   }

   public void setProperty (InheritableProperty prop) {
      myProp = prop;
      if (prop != null) {
         setMode (prop.getMode());
      }
   }

   public LabeledControl getWidget() {
      return myWidget;
   }

   public void setWidget (LabeledControl control) {
      myWidget = control;
      if (myProp != null) {
         updateDisplay (myProp.getMode());
      }
   }

   public void actionPerformed (ActionEvent e) {
      if (e.getActionCommand().equals ("set")) {
         if (myProp != null) {
            if (isSelected()) {
               if (myProp.getMode() != PropertyMode.Explicit) {
                  myProp.setMode (PropertyMode.Explicit);
                  updateDisplay (PropertyMode.Explicit);
               }
            }
            else {
               if (myProp.getMode() != PropertyMode.Inherited) {
                  myProp.setMode (PropertyMode.Inherited);
                  if (myWidget != null) {
                     myWidget.maskValueChangeListeners (true);
                     Object value = myProp.get();
                     myWidget.setValue (value);
                     // fire only local value change listeners;
                     // not those associated with the control
                     fireValueChangeListeners (value);
                     myWidget.maskValueChangeListeners (false);
                  }
                  updateDisplay (PropertyMode.Inherited);
               }
            }
         }
      }
   }
}
