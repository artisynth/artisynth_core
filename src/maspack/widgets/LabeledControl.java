/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.*;
import javax.swing.JTextField;
import java.util.*;

import maspack.util.StringHolder;
import maspack.matrix.Matrix;
import maspack.matrix.Vector;
import maspack.properties.Property;

/**
 * A LabeledComponent which is specifically intended to modify values of some
 * primitive, such as numeric or string values, colors, file selections, etc. In
 * support of this purpose, a LabeledControl can accept {@link
 * maspack.widgets.ValueChangeListener ValueChangeListeners}, which
 * are fired every time the value associated with this control changes. Values
 * can be set externally (outside of the GUI) by calling #setValue.
 * 
 * <p>
 * Values for a LabeledControl can be set either through the GUI or by calling
 * {@link #setValue setValue}. All controls should be able to support a special
 * ``void'' value identified by
 * {@link maspack.properties.Property#VoidValue Property.VoidValue}, which can
 * be used to indicate undefined or unspecified values. Void values are
 * typically not enabled by default but can be enabled by calling
 * {@link #setVoidValueEnabled setVoidValueEnabled}. If enabled, a void value
 * can be set by calling
 * 
 * <pre>
 * setValue (Property.VoidValue);
 * </pre>
 * 
 * Some individual controls may also allow a void value to be set through the
 * GUI. In this case, {@link #getGUIVoidEnabled getGUIVoidEnabled} will return
 * <code>true</code>.
 * 
 * <p>
 * Some controls may accept <code>null</code> as a valid value. In this case,
 * {@link #getNullValueEnabled getNullValueEnabled} will return
 * <code>true</code>. <code>null</code> is not used to indicate void values
 * because in some situations it may be a proper value in its own right.
 * 
 * <p>
 * The current value of a control can be read back using {@link #getValue
 * getValue}. If void values are enabled, the user should check that the value
 * is not {@link maspack.properties.Property#VoidValue Property.VoidValue}
 * before casting it to a type specific to the control.
 * 
 * An application can add value change listeners that are called whenever the
 * value of the control changes. Firing occurs only when there is a change a
 * value, not just when a value is set. An application can also add value check
 * listeners that are called to check any prospective new value. These listeners
 * can declare a value to be invalid, or correct it to an adjusted value.
 * 
 * <p>
 * In general, setting a new value entails:
 * <ul>
 * <li> checking the new value's validity
 * <li> set the new value, and fire the value change listeners if the new value
 * differs from the old
 * <li> update the display if the new value differs from the old
 * </ul>
 * Generally speaking, these items are handled by
 * {@link #validateValue validateValue}, {@link #updateValue updateValue}, and
 * {@link #updateDisplay updateDisplay}.
 */
public abstract class LabeledControl extends LabeledComponent {
   private static final long serialVersionUID = 1L;
   protected LinkedList<ValueChangeListener> myChangeListeners =
      new LinkedList<ValueChangeListener>();
   protected LinkedList<ValueCheckListener> myCheckListeners =
      new LinkedList<ValueCheckListener>();
   protected boolean myChangeListenersMasked = false;
   protected boolean myChecksMasked = false;
   protected boolean myGUIVoidEnabled = false;
   protected boolean myVoidEnabled = false;
   protected boolean myNullEnabled = false;
   protected boolean myAllEnabledP = true;

   /**
    * Enables or disables the value change listeners. If disabled, then
    * {@link #fireValueChangeListeners fireValueChangeListeners} will do
    * nothing.
    * 
    * @param masked
    * if true, masks the value change listeners.
    */
   public void maskValueChangeListeners (boolean masked) {
      myChangeListenersMasked = masked;
   }

   /**
    * Returns true if the value change listeners are masked.
    * 
    * @return true if change listeners are masked.
    */
   public boolean valueChangeListenersMasked() {
      return myChangeListenersMasked;
   }

   /**
    * Enables or disables value checking from within {@link #setValue setValue}.
    * 
    * @param masked
    * if true, masks value checking.
    */
   public void maskValueChecks (boolean masked) {
      myChecksMasked = masked;
   }

   /**
    * Returns true if value checking is enabled within {@link #setValue
    * setValue}.
    * 
    * @return true if value checking is enabled.
    */
   public boolean valueChecksMasked() {
      return myChecksMasked;
   }

   /**
    * Adds a ValueChangeListener to this control.
    * 
    * @param l
    * listener to add
    */
   public void addValueChangeListener (ValueChangeListener l) {
      myChangeListeners.add (l);
   }

   /**
    * Removes a ValueChangeListener from this control.
    * 
    * @param l
    * listener to remove
    */
   public void removeValueChangeListener (ValueChangeListener l) {
      myChangeListeners.remove (l);
   }

   /**
    * Returns a list of all the ValueChangeListeners currently held by this
    * control.
    * 
    * @return list of ValueChangeListeners
    */
   public ValueChangeListener[] getValueChangeListeners() {
      return myChangeListeners.toArray (new ValueChangeListener[0]);
   }

   /**
    * Adds a ValueCheckListener to this control.
    * 
    * @param l
    * listener to add
    */
   public void addValueCheckListener (ValueCheckListener l) {
      myCheckListeners.add (l);
   }

   /**
    * Removes a ValueCheckListener from this control.
    * 
    * @param l
    * listener to remove
    */
   protected void removeValueCheckListener (ValueCheckListener l) {
      myCheckListeners.remove (l);
   }

   /**
    * Returns a list of all the ValueCheckListeners currently held by this
    * control.
    * 
    * @return list of ValueCheckListeners
    */
   public ValueCheckListener[] getValueCheckListeners() {
      return myCheckListeners.toArray (new ValueCheckListener[0]);
   }

   /**
    * Fire all the ValueChangeListeners associated with this control. This
    * method should be called by a subclass whenever the value it is managing
    * changes.
    * 
    * @param value
    * object representation of the new value
    */
   protected void fireValueChangeListeners (Object value) {
      if (!myChangeListenersMasked) {
         for (ValueChangeListener l : myChangeListeners) {
            l.valueChange (new ValueChangeEvent (this, value));
         }
      }
   }

   private Object runAllChecks (ValueChangeEvent e, StringHolder errMsg) {
      for (ValueCheckListener l : myCheckListeners) {
         Object res = l.validateValue (e, errMsg);
         if (res != e.getValue()) {
            return res;
         }
      }
      return e.getValue();
   }

   /**
    * Fire all the ValueCheckListeners associated with this control. This method
    * should be called by a subclass whenever the value it is managing changes.
    * 
    * @param value
    * object representation of the new value
    */
   protected Object fireValueCheckListeners (Object value, StringHolder errMsg) {
      ValueChangeEvent e = new ValueChangeEvent (this, value);
      Object validValue = runAllChecks (e, errMsg);
      if (validValue == value || validValue == Property.IllegalValue) {
         return validValue;
      }

      // run checks again in case adjusted value conflicts with
      // one of the other range checks
      e = new ValueChangeEvent (this, validValue);
      if (runAllChecks (e, null) != validValue) {
         if (errMsg != null) {
            errMsg.value = "Illegal adjusted value: " + errMsg.value;
         }
         return null;
      }
      return validValue;
   }

   /**
    * Creates a control with specified label text.
    * 
    * @param labelText
    * text value for the control label
    */
   public LabeledControl (String labelText) {
      super (labelText);
   }

   /**
    * Creates a control with specified label text and a major component.
    * 
    * @param labelText
    * text value for the control label
    * @param comp
    * major component to add to this control
    */
   public LabeledControl (String labelText, Component comp) {
      super (labelText, comp);
   }

   /**
    * Releases all the resources held by this control.
    */
   public void dispose() {
      super.dispose();
      myChangeListeners.clear();
   }

   protected void finalize() throws Throwable {
      myChangeListeners.clear();
      super.finalize();
   }

   /**
    * Enables or disables this control by enabling or disabling all its
    * components.
    * 
    * @param enable
    * enables or disables all components
    */
   public void setEnabledAll (boolean enable) {
      myAllEnabledP = enable;
      super.setEnabled (enable);
      for (Component comp : myComponents) {
         comp.setEnabled (enable);
      }
   }

   /**
    * Sets the background of this control and all its sub-components, except for
    * JTextFields, where setting the background would obscure the text field.
    * 
    * @param color
    * new background color
    */
   public void setBackgroundAll (Color color) {
      setBackground (color);
      for (Component comp : myComponents) {
         if (!(comp instanceof JTextField)) {
            comp.setBackground (color);
         }
      }
   }

   /**
    * Returns true if this control, and all its components, are enabled.
    * 
    * @return true if this control is enabled
    */
   public boolean isEnabledAll() {
      return myAllEnabledP;
   }

   /*
    * Enables void values to be set from the GUI. Not all controls will
    * implement this ability. This method can be publically exposed by those
    * that do. The default value for this setting is <code>false</code>.
    */
   /**
    * @param enable
    * or disables setting void values from the GUI
    */
   protected void setGUIVoidEnabled (boolean enable) {
      myGUIVoidEnabled = enable;
   }

   /**
    * Returns true if void values can be set from the GUI.
    * 
    * @return true if the GUI can set void values
    */
   public boolean getGUIVoidEnabled() {
      return myGUIVoidEnabled;
   }

   /**
    * Enables void values to be set for this control. The default setting is
    * <code>false</code>.
    * 
    * @param enable
    * enables or disables void values for this control
    */
   public void setVoidValueEnabled (boolean enable) {
      myVoidEnabled = enable;
   }

   /**
    * Returns true if void values are enabled for this control.
    * 
    * @return true if void values are enabled
    */
   public boolean getVoidValueEnabled() {
      return myVoidEnabled;
   }

   /**
    * Enables null values to be set for this control. Not all controls will
    * implement this ability. This method can be publically exposed by those
    * that do and wish to make it externally controlable. The default value for
    * this setting is <code>false</code>.
    * 
    * @param enable enables or disables null values for this control
    */
   protected void setNullValueEnabled (boolean enable) {
      myNullEnabled = enable;
   }

   /**
    * Returns true if null values are enabled for this control.
    * 
    * @return true if null values are enabled
    */
   public boolean getNullValueEnabled() {
      return myNullEnabled;
   }

   /**
    * Updates this control's GUI display to reflect its current value. This
    * method will be called whenever the value changes.
    */
   protected abstract void updateDisplay();

   /**
    * Validates a specified value for this control. If the value is valid, then
    * it should be returned unchanged. If the value is invalid but can be
    * replaced with a suitable corrected value, then that corrected value should
    * be returned in an object different from the original value. If the value
    * is invalid and no alternative value exists, then
    * {@link maspack.properties.Property#IllegalValue Property.IllegalValue}
    * should be returned. The optional variable errMsg is used to return an
    * error message describing the problem with any invalid value. The errMsg
    * setting is undefined for valid values.
    * 
    * <p>
    * Note in particularly that contract of this method provides that if the
    * returned value is the same object as the supplied value, then the original
    * value is valid. Implementations of this method may use support methods
    * such as {@link #validateType validateType} and {@link #validateBasic
    * validateBasic}
    * 
    * @param value
    * value to be tested
    * @param errMsg
    * optional argument for holding an error message
    * @return the original value reference if it is valid, or a different object
    * containing a corrected value, or
    * {@link maspack.properties.Property#IllegalValue Property.IllegalValue} if
    * no correction is possible.
    */
   protected abstract Object validateValue (Object value, StringHolder errMsg);

   protected Object validValue (Object value, StringHolder errMsg) {
      if (errMsg != null) {
         errMsg.value = null;
      }
      return value;
   }

   /**
    * Convenience method for returning {@link
    * maspack.properties.Property#IllegalValue Property.IllegalValue} a value
    * and setting an error message if errMsg is not null. It is intended to be
    * used in validation methods in the following fashion:
    * 
    * <pre>
    * return illegalValue (&quot;Value out of range&quot;, errMsg);
    * </pre>
    * 
    * @param err
    * error message to be returned
    * @param errMsg
    * optional variable in which to return the message
    * @return Property.IllegalValue
    */
   protected Object illegalValue (String err, StringHolder errMsg) {
      if (errMsg != null) {
         errMsg.value = err;
      }
      return Property.IllegalValue;
   }

   /**
    * Convenience method for returning a value and setting an error message if
    * errMsg is not null. It is intended to be used in validation methods in the
    * following fashion:
    * 
    * <pre>
    * return correctedValue (value, &quot;Value was range corrected&quot;, errMsg);
    * </pre>
    * 
    * @param value
    * to be returned
    * @param err
    * error message to be returned
    * @param errMsg
    * optional variable in which to return the message
    * @return a reference to value
    */
   protected Object correctedValue (
      Object value, String err, StringHolder errMsg) {
      if (errMsg != null) {
         errMsg.value = err;
      }
      return value;
   }

   /**
    * Convenience method to set a void value, if possible, from the widget.
    */
   protected Object setVoidIfPossible (StringHolder errMsg) {
      if (myGUIVoidEnabled) {
         return Property.VoidValue;
      }
      else {
         return illegalValue ("Void not settable from this widget", errMsg);
      }
   }

   /**
    * Updates the internal representation of the value and returns true if the
    * new value differs from the old value.
    */
   protected abstract boolean updateInternalValue (Object value);

   /**
    * Returns the current value of a control.
    */
   protected abstract Object getInternalValue();

   // protected abstract void setInternalValue (Object value);

   /**
    * Returns the current value for this control. This may include void or null
    * values if these are enabled
    */
   /*
    * NOTE: the below comment was part of the documentation material above, but
    * was removed because it was causing problems. This is because
    * setVoidValuesEnabled seems to be removed.
    */
   /**
    * @return current value for this control
    */
   public Object getValue() {
      return getInternalValue();
   }

   /**
    * Returns true if the current value for this control is void (i.e.,
    * {@link maspack.properties.Property#VoidValue Property.VoidValue}).
    * 
    * @return true if the current value is void
    */
   public boolean valueIsVoid() {
      return (getInternalValue() == Property.VoidValue);
   }

   /**
    * Returns true if the current value for this control is null.
    * 
    * @return true if the current value is null
    */
   public boolean valueIsNull() {
      return (getInternalValue() == null);
   }

   /**
    * Returns true if two control values are equal, allowing for values to be
    * null and void, as well as various vector, matrix, and object values. This
    * method is used by updateInternalValue method of the specific control. Care
    * should be taken when using this method to verify that its equality checks
    * are appropriate.
    */
   protected boolean valuesEqual (Object obj1, Object obj2) {
      if (obj1 == null && obj2 == null) {
         return true;
      }
      else if (obj1 instanceof Vector && obj2 instanceof Vector) {
         return ((Vector)obj1).equals ((Vector)obj2);
      }
      else if (obj1 instanceof Matrix && obj2 instanceof Matrix) {
         return ((Matrix)obj1).equals ((Matrix)obj2);
      }
      else if (obj1 instanceof int[] && obj2 instanceof int[]) {
         return Arrays.equals ((int[])obj1, (int[])obj2);
      }
      else if (obj1 instanceof double[] && obj2 instanceof double[]) {
         return Arrays.equals ((double[])obj1, (double[])obj2);
      }
      else if (obj1 != null && obj2 != null) {
         return (obj1.equals (obj2));
      }
      else {
         return false;
      }
   }

   /**
    * Does basic validation of new control value, checking its type and firing
    * all the value check listeners. This method will typically be used in the
    * implementation of {@link #validateValue validateValue}.
    */
   protected Object validateBasic (Object value, Class type, StringHolder errMsg) {
      value = validateType (value, type, errMsg);
      if (value == Property.IllegalValue) {
         return value;
      }
      return fireValueCheckListeners (value, errMsg);
   }

   /**
    * Does basic validation of new control value, checking its type and firing
    * all the value check listeners. This method will typically be used in the
    * implementation of {@link #validateValue validateValue}.
    */
   protected Object validateBasic (
      Object value, Class[] types, StringHolder errMsg) {
      value = validateType (value, types, errMsg);
      if (value == Property.IllegalValue) {
         return value;
      }
      return fireValueCheckListeners (value, errMsg);
   }

   /**
    * Validates the type of a new control value. This method may be used in the
    * implementation of {@link #validateValue validateValue}.
    * 
    * @param value
    * value to be checked
    * @param type
    * class which the value should be an instance of if it neither null nor void
    * @param errMsg
    * optional argument for returning error messages
    * @return a reference to value if it is valid, or Property.IllegalValue if
    * it is not
    */
   protected Object validateType (Object value, Class type, StringHolder errMsg) {
      if (value == null) {
         if (!myNullEnabled) {
            return illegalValue ("Value may not be null", errMsg);
         }
         else {
            return validValue (value, errMsg);
         }
      }
      else if (value == Property.VoidValue) {
         if (!myVoidEnabled) {
            return illegalValue ("Value may not be void", errMsg);
         }
         else {
            return validValue (value, errMsg);
         }
      }
      else {
         if (!(type.isAssignableFrom (value.getClass()))) {
            return illegalValue (
               "Value must be of type " + type.getName(), errMsg);
         }
         else {
            return validValue (value, errMsg);
         }
      }
   }

   /**
    * Validates the type of a new control value. This method may be used in the
    * implementation of {@link #validateValue validateValue}.
    * 
    * @param value
    * value to be checked
    * @param types
    * classes which the value should be an instance of if it neither null nor
    * void
    * @param errMsg
    * optional argument for returning error messages
    * @return a reference to value if it is valid, or Property.IllegalValue if
    * it is not
    */
   protected Object validateType (
      Object value, Class[] types, StringHolder errMsg) {
      if (value == null) {
         if (!myNullEnabled) {
            return illegalValue ("Value may not be null", errMsg);
         }
         else {
            return validValue (value, errMsg);
         }
      }
      else if (value == Property.VoidValue) {
         if (!myVoidEnabled) {
            return illegalValue ("Value may not be void", errMsg);
         }
         else {
            return validValue (value, errMsg);
         }
      }
      else {
         for (int i = 0; i < types.length; i++) {
            if (types[i].isAssignableFrom (value.getClass())) {
               return validValue (value, errMsg);
            }
         }
         String message = "Value must have one of the following types:";
         for (int i = 0; i < types.length; i++) {
            message += " " + types[i].getName();
         }
         return illegalValue (message, errMsg);
      }
   }

   /**
    * Updates the value for this control. If the new value differs from the old
    * value, then the value change listeners are fired and the method returns
    * true. Otherwise, the method returns false. The method does not perform
    * value checking or display updating. This method is used by #setValue and
    * may also be used internally by subclasses to handle value changes induced
    * by the widgets.
    * 
    * @param value
    * new value for this control
    */
   protected boolean updateValue (Object value) {
      if (updateInternalValue (value)) {
         fireValueChangeListeners (getInternalValue());
         return true;
      }
      return false;
   }

   /**
    * Sets a new value for this control. This may include null or void if those
    * values are enabled.
    * 
    * @param value
    * new value for this control
    */
   public void setValue (Object value) {
      StringHolder errMsg = new StringHolder();
      if (!myChecksMasked) {
         Object checkedValue = validateValue (value, errMsg);
         if (checkedValue == Property.IllegalValue) {
            throw new IllegalArgumentException (errMsg.value);
         }
      }
      if (updateValue (value)) {
         updateDisplay();
      }
   }
}
