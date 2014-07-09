/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.properties.Property;

public class PropChangeListener implements ValueChangeListener {
   protected Property myProp;
   protected Class myType;
   protected Object mySyncObj;

   public Object getSynchronizeObject() {
      return mySyncObj;
   }

   public void setSynchronizeObject (Object sobj) {
      mySyncObj = sobj;
   }

   public PropChangeListener (Property prop) {
      myProp = prop;
      myType = prop.getInfo().getValueClass();
   }

   public void valueChange (ValueChangeEvent e) {
      Object value = e.getValue();
      if (myType == float.class || myType == Float.class) {
         if (value instanceof Double) {
            value = new Float (((Double)value).floatValue());
         }
      }
      if (mySyncObj != null) {
         synchronized (mySyncObj) {
            myProp.set (value);
         }
      }
      else {
         myProp.set (value);
      }
   }

   public Property getProperty() {
      return myProp;
   }

   public void dispose() {
      myProp = null;
   }
}
