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

   /**
    * Create an EditingProperty that edits a single property across a set of
    * hosts, each specified by a property path with respect to a common
    * ancestor host.
    *
    * @param propPaths paths of the property with respect to the ancestor host
    * @param ancestor ancestor property host
    * @param isLive if {@code true}, this widget should support live updating
    * of the property value within the hosts
    * @return created EditingProperty, or {@code null} if the specified
    * property is not the same across all hosts.
    */
   public static EditingProperty createProperty (
      String[] propPaths, HasProperties ancestor, boolean isLive) {
      Property prop = null;
      HostList hostList = new HostList (propPaths.length);
      String propName = null;
      // see if a property with the indicated name exists under all hosts,
      // saving the property's immediate host in the hostList.
      for (String propPath : propPaths) {
         prop = ancestor.getProperty (propPath);
         if (prop == null) {
            return null;
         }
         if (propName == null) {
            propName = prop.getName();
         }
         else if (!propName.equals (prop.getName())) {
            return null;
         }
         hostList.addHost (prop.getHost());
      }
      // Create a property tree from the immediate hosts, verifying that
      // property does in fact match (with respect to value, name, etc.)
      // across all hosts.
      PropTreeCell tree = hostList.commonProperty (
         prop.getName(), /*allowReadonly=*/true);
      if (tree == null) {
         return null;
      }
      // finish hostList initialization by calling saveBackupValues().
      hostList.saveBackupValues (tree);
      hostList.getCommonValues (tree, isLive);
      return new EditingProperty (tree.getFirstChild(), hostList, isLive);
   }

   /**
    * Create an EditingProperty that edits a property specified by propPath
    * with respect to a set of property hosts.
    *
    * @param propPath path of the property with respect to each host
    * @param hosts property host object
    * @param isLive if {@code true}, this widget should support live updating
    * of the property value in the hosts
    * @return created EditingProperty, of {@code null} if the specified
    * property is not common to all hosts.
    */
   public static EditingProperty createProperty (
      String propPath, HasProperties[] hosts, boolean isLive) {
      Property prop = null;
      HostList hostList = new HostList (hosts.length);
      // see if a property with the indicated name exists under all hosts,
      // saving the property's immediate host in the hostList.
      for (HasProperties host : hosts) {
         prop = host.getProperty (propPath);
         if (prop == null) {
            return null;
         }
         hostList.addHost (prop.getHost());
      }
      // Create a property tree from the immediate hosts, verifying that
      // property does in fact match (with respect to value, name, etc.)
      // across all hosts.
      PropTreeCell tree = hostList.commonProperty (
         prop.getName(), /*allowReadonly=*/true);
      if (tree == null) {
         return null;
      }
      // finish hostList initialization
      hostList.saveBackupValues (tree);
      hostList.getCommonValues (tree, isLive);
      return new EditingProperty (tree.getFirstChild(), hostList, isLive);
   }

   /**
    * Resets the host list within this EditingProperty. The specified hosts
    * must all be immediate hosts of the current property.
    *
    * @param hostList new host list
    */
   public void setHostList (HostList hostList, boolean isLive) {
      // check that all hosts contain the specified property
      String propName = (myCell != null ? myCell.getPropName() : null);
      if (propName == null) {
         throw new IllegalStateException (
            "EditingProperty is does not have a property set");
      }
      // Create a property tree from the new hostList, verifying that property
      // does in fact match (with respect to value, name, etc.)  across all
      // hosts.
      PropTreeCell tree = hostList.commonProperty (
         propName, /*allowReadonly=*/true);
      if (tree == null) {
         throw new IllegalArgumentException (
            "Existing property is not common to all hosts in the new hostList");
      }
      // finish hostList initialization.  Have to create a root tree node for
      // this.
      hostList.saveBackupValues (tree);
      hostList.getCommonValues (tree, isLive);
      myHostList = hostList;
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
