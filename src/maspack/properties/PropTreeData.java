/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

public class PropTreeData {
   Object myValue = Property.VoidValue;
   PropertyMode myMode = PropertyMode.Inactive;
   PropertyInfo myInfo;
   HasProperties myHost; // used by HostLists to store composite property values
   PropTreeData[] mySubData; // used to HostLists to maintain property tree

   public PropTreeData() {
   }

   public PropTreeData (PropertyInfo info, Object value, PropertyMode mode) {
      set (info, value, mode);
   }

   public PropTreeData (PropTreeData data) {
      set (data.myInfo, data.myValue, data.myMode);
   }

   public void set (PropertyInfo info, Object value) {
      myInfo = info;
      myValue = value;
   }

   public void set (PropertyInfo info, Object value, PropertyMode mode) {
      myInfo = info;
      myValue = value;
      myMode = mode;
   }

   public void set (PropTreeData data) {
      set (data.myInfo, data.myValue);
   }

   public void setMode (PropertyMode mode) {
      myMode = mode;
   }

   public Object getValue () {
      return myValue;
   }

   public void setValue (Object value) {
      myValue = value;
   }

   public HasProperties getHost() {
      return myHost;
   }

   public void setHost (HasProperties host) {
      myHost = host;
   }

   public PropertyInfo getInfo () {
      return myInfo;
   }

   public void setInfo (PropertyInfo info) {
      myInfo = info;
   }

   public void setSubData (PropTreeData[] data) {
      mySubData = data;
   }

   public PropTreeData[] getSubData() {
      return mySubData;
   }

   public String getName() {
      return myInfo == null ? null : myInfo.getName();
   }
}
