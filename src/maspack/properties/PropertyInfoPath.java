/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.util.*;

class PropertyInfoPath {
   private LinkedList<PropertyInfo> myInfoList;

   public PropertyInfoPath() {
      myInfoList = new LinkedList<PropertyInfo>();
   }

   public PropertyInfoPath (PropertyInfo info) {
      this();
      append (info);
   }

   public PropertyInfoPath (PropertyInfoPath path) {
      this();
      set (path);
   }

   public PropertyInfoPath (PropertyInfoPath path, PropertyInfo info) {
      this (path);
      myInfoList.add (info);
   }

   public void set (PropertyInfoPath path) {
      clear();
      if (path != null) {
         for (PropertyInfo info : path.myInfoList) {
            myInfoList.add (info);
         }
      }
   }

   public void append (PropertyInfo info) {
      myInfoList.add (info);
   }

   public void prepend (PropertyInfo info) {
      if (info == null) {
         throw new IllegalArgumentException ("info is null");
      }
      myInfoList.addFirst (info);
   }

   public void removeFront (int n) {
      for (int i = 0; i < n; i++) {
         myInfoList.removeFirst();
      }
   }

   public void prepend (PropertyInfoPath path) {
      myInfoList.addAll (0, path.myInfoList);
   }

   public void clear() {
      myInfoList.clear();
   }

   public PropertyInfo getLast() {
      return myInfoList.getLast();
   }

   Iterator<PropertyInfo> iterator() {
      return myInfoList.iterator();
   }

   public String toString() {
      StringBuffer buf = new StringBuffer (256);
      for (PropertyInfo info : myInfoList) {
         buf.append (info.getName());
         if (info != getLast()) {
            buf.append ('.');
         }
      }
      return buf.toString();
   }

   public boolean equals (Object obj) {
      if (obj instanceof PropertyInfoPath) {
         PropertyInfoPath path = (PropertyInfoPath)obj;
         return myInfoList.equals (path.myInfoList);
      }
      else {
         return false;
      }
   }

   public int length() {
      return myInfoList.size();
   }

}
