/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JComboBox;

import maspack.properties.Property;
import maspack.util.InternalErrorException;
import maspack.util.StringHolder;

public abstract class LabeledComboBox extends LabeledControl {
   protected static class NameValuePair {
      
      public static NameValuePair NULL = new NameValuePair("null", null);
      
      NameValuePair (String name, Object value) {
         this.name = name;
         this.value = value;
      }

      public String name;
      public Object value;
   }

   private static final long serialVersionUID = 1L;
   protected ComboBox myComboBox;
   protected int myIndex = -1;
   protected boolean myComboBoxMasked;
   protected boolean myListHasVoid = true;
   protected boolean myListHasNull  = true;
   protected ArrayList<NameValuePair> myNameValueList;

   /**
    * Returns true if the current set of list items equals the supplied names
    * and values.
    */
   protected boolean listItemsEqual(String[] names, Object[] values) {
      if (names.length != values.length) {
         throw new IllegalArgumentException(
            "Incompatible number of names and values");
      }
      if (names.length != myNameValueList.size()) {
         return false;
      }
      for (int i = 0; i < names.length; i++) {
         NameValuePair pair = myNameValueList.get(i);
         if (!pair.name.equals(names[i]) ||
            !pair.value.equals(values[i])) {
            return false;
         }
      }
      return true;
   }

   /**
    * Called by sub-classes to specify the name-value pairs associated with this
    * combo-box. Values should not include Property.VoidValue; that is handled
    * separately.
    */
   protected void setListItems(String[] names, Object[] values) {
      if (names.length != values.length) {
         throw new IllegalArgumentException(
            "Incompatible number of names and values");
      }
      Object oldValue = getInternalValue();
      myNameValueList.clear();
      if (myListHasVoid) {
         addVoidToList();
      }
      for (int i = 0; i < names.length; i++) {
         myNameValueList.add(new NameValuePair(names[i], values[i]));
      }
      if (myNullEnabled) {
         addNullToList();
      }
      setComboBoxItems();
      int idx = getIndexForValue(oldValue);
      if (idx == -1) {
         idx = 0;
      }
      myIndex = idx;
      updateComboBox();
   }

   private void addVoidToList() {
      myNameValueList.add(0, new NameValuePair(" ", Property.VoidValue));
      myIndex++;
   }

   private void removeVoidFromList() {
      myNameValueList.remove(0);
      myIndex--;
   }
   
   protected void addNullToList() {
      // check for null
      for (NameValuePair pair : myNameValueList) {
         if (pair.value == null) {
            return;
         }
      }
      myNameValueList.add(NameValuePair.NULL);
   }
  
   protected void removeNullFromList() {
      Iterator<NameValuePair> pit = myNameValueList.iterator();
      NameValuePair pair;
      while (pit.hasNext()) {
         pair = pit.next();
         if (pair.value == null) {
            pit.remove();
         }
      }
   }
   
   public String[] getComboLabels() {
      String[] out = new String[myNameValueList.size()];
      for (int i=0; i<myNameValueList.size(); i++) {
         out[i] = myNameValueList.get(i).name;
      }
      return out;
   }
   
   public Object[] getComboValues() {
      Object[] out = new Object[myNameValueList.size()];
      for (int i=0; i<myNameValueList.size(); i++) {
         out[i] = myNameValueList.get(i).value;
      }
      return out;
   }

   private void setListHasVoid(boolean hasVoid) {
      if (hasVoid != myListHasVoid) {
         if (hasVoid) {
            addVoidToList();
         }
         else {
            removeVoidFromList();
         }
         myListHasVoid = hasVoid;
         setComboBoxItems();
      }
   }

   public void setGUIVoidEnabled(boolean enable) {
      super.setGUIVoidEnabled(enable);
      boolean listShouldHaveVoid =
         (enable || getInternalValue() == Property.VoidValue);
      if (listShouldHaveVoid != myListHasVoid) {
         setListHasVoid(listShouldHaveVoid);
         if (listShouldHaveVoid) {
            myIndex++; // void was added
         }
         else {
            myIndex--; // void was removed
         }
      }
   }

   private void setComboBoxItems() {
      myComboBoxMasked = true;
      myComboBox.removeAllItems();
      for (NameValuePair pair : myNameValueList) {
         myComboBox.addItem(pair.name);
      }
      myComboBoxMasked = false;
   }

   protected void updateComboBox() {
      myComboBoxMasked = true;
      myComboBox.setSelectedIndex(myIndex);
      myComboBoxMasked = false;
   }

   public LabeledComboBox (String label) {
      super(label, new ComboBox());
      myNameValueList = new ArrayList<NameValuePair>();
      // add a void value to the initial list since the
      // initial value is void
      addVoidToList();
      myIndex = 0;

      myComboBox = (ComboBox)getMajorComponent(label == null ? 0 : 1);
      myComboBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if (!myComboBoxMasked) {
               int idx = myComboBox.getSelectedIndex();
               Object value = myNameValueList.get(idx).value;
               Object validValue = validateValue(value, null);
               if (validValue != value) {
                  idx = getIndexForValue(validValue);
                  updateDisplay();
                  if (idx == -1) {
                     return;
                  }
               }
               if (updateValue(validValue)) {
                  updateDisplay();
               }
            }
         }
      });
      myComboBox.setFocusable(false);
   }

   /**
    * Returns the JComboBox associated with this control.
    * 
    * @return combo box for this control
    */
   public JComboBox getComboBox() {
      return myComboBox;
   }

   protected void printNameValueList() {
      for (NameValuePair pair : myNameValueList) {
         System.out.println(pair.name + "  " + pair.value);
      }
   }

   protected int getIndexForValue(Object value) {
      for (int i = 0; i < myNameValueList.size(); i++) {
         NameValuePair pair = myNameValueList.get(i);
         if (value == null && pair.value == null) {
            return i;
         } else if (pair.value != null && pair.value.equals(value)) {
            return i;
         }
      }
      return -1;
   }

   protected abstract void updateResultHolder(Object value);

   protected abstract Object convertToListObject(Object value);

   protected boolean updateInternalValue(Object value) {
      if (!valuesEqual(value, getInternalValue())) {
         Object obj = value;
         int idx;
         if (value != Property.VoidValue) {
            obj = convertToListObject(value);
            updateResultHolder(obj);
            if (!myGUIVoidEnabled && myListHasVoid) {
               setListHasVoid(false);
            }
         }
         else {
            if (!myListHasVoid) {
               setListHasVoid(true);
            }
         }
         idx = getIndexForValue(obj);
         if (idx == -1) {
            throw new InternalErrorException("value " + value
               + " not contained in list");
         }
         myIndex = idx;
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
      if (value != Property.VoidValue) {
         Object obj;
         try {
            obj = convertToListObject(value);
         } catch (Exception e) {
            return illegalValue(e.getMessage(), errMsg);
         }
         if (getIndexForValue(obj) == -1) {
            return illegalValue(
               "value " + value + " is not present in list", errMsg);
         }
      }
      return validValue(value, errMsg);
   }

   /**
    * Updates the control display to reflect the current internl value.
    */
   protected void updateDisplay() {
      updateComboBox();
   }

   protected Object getInternalValue() {
      return myNameValueList.get(myIndex).value;
   }

   // protected void setInternalValue (Object value)
   // {
   // myIndex = getIndexForValue (value);
   // if (myIndex == -1)
   // { throw new InternalErrorException (
   // "Index not found for object " + value);
   // }
   // }

   protected Object getListValue(int idx) {
      return myNameValueList.get(idx).value;
   }

}

class ComboBox extends JComboBox<String> {
   private static final long serialVersionUID = 1223987918789912L;

   ComboBox () {
      super();
   }

   public Dimension getMaximumSize() {
      return getPreferredSize();
   }

   public Dimension getMinimumSize() {
      return getPreferredSize();
   }
}
