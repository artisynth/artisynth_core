/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.util.*;

import maspack.util.Range;

/**
 * Stub property that interfaces to a common property of one or more underlying
 * property hosts.
 */
public class EditingProperty implements InheritableProperty {
   private PropTreeCell myCell;
   private HostList myHostList;
   private boolean myLiveP;

   public EditingProperty (PropTreeCell cell, HostList hostList, boolean isLive) {
      myCell = cell;
      myHostList = hostList;
      myLiveP = isLive;
   }

   public void updateValue() {
      Object value = myHostList.getCommonValue (myCell);
      myCell.setValue (value);
      if (myCell.getMode() != PropertyMode.Inactive) {
         PropertyMode mode = myHostList.getCommonMode (myCell);
         myCell.setMode (mode);
      }
   }

   public Object get() {
      return myCell.getValue();
   }

   public void set (Object value) {
      myCell.setValue (value);
      if (myCell.getMode() != PropertyMode.Inactive) {
         myCell.setMode (PropertyMode.Explicit);
      }
      if (myLiveP) {
         myHostList.setValue (myCell, value);
      }
   }
   
   public Range getRange () {
      return myHostList.getCommonRange (myCell);
   }

//   public Object validate (Object value, StringHolder errMsg) {
//      if (myLiveP) {
//         return myHostList.validateValue (myCell, value, errMsg);
//      }
//      else {
//         if (errMsg != null) {
//            errMsg.value = null;
//         }
//         return value;
//      }
//   }

   public PropertyMode getMode() {
      return myCell.getMode();
   }

   public void setMode (PropertyMode mode) {
      myCell.setMode (mode);
      if (myLiveP) {
         myHostList.setMode (myCell, mode);
         if (mode == PropertyMode.Inherited) {
            Object commonValue = myHostList.getCommonValue (myCell);
            myCell.setValue (commonValue);
         }
      }
      else {
         if (mode == PropertyMode.Inherited) {
            myCell.setValue (myHostList.getCommonValue (myCell));
         }
      }
   }

   public String getName() {
      return myCell.getInfo().getName();
   }

   /**
    * Assuming this is not needed ...
    */
   public HasProperties getHost() {
      return null;
   }

   public PropertyInfo getInfo() {
      return myCell.getInfo();
   }

   public HostList getHostList() {
      return myHostList;
   }

   public boolean isLive() {
      return myLiveP;
   }

   public PropTreeCell getCell() {
      return myCell;
   }

   public static LinkedList<Property> createProperties (
      PropTreeCell tree, HostList hostList, boolean isLive) {
      LinkedList<Property> props = new LinkedList<Property>();

      PropTreeCell child = tree.myFirstChild;
      PropTreeCell saveChild = null;

      while (child != null) {
         props.add (new EditingProperty (child, hostList, isLive));
         child = child.next;
         if (saveChild != null) {
            saveChild = saveChild.next;
         }
      }
      return props;
   }

}
