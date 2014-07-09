/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.properties.Property;
import maspack.util.Range;
import maspack.util.StringHolder;

public class PropCheckListener implements ValueCheckListener {
   protected Property myProp;
   protected Class myType;

   public PropCheckListener (Property prop) {
      myProp = prop;
      myType = prop.getInfo().getValueClass();
   }

   public Object validateValue (ValueChangeEvent e, StringHolder errMsg) {
      //return myProp.validate (e.getValue(), errMsg);
      Range range = myProp.getRange();
      Object value = e.getValue();
      if (range != null && !range.isValid (value, errMsg)) {
         Object corrected = range.makeValid (value);
         if (corrected == Range.IllegalValue) {
            return Property.IllegalValue;
         }
         else {
            return corrected;
         }
      }
      else {
         if (errMsg != null) { // probably don't need to do this ...make
            errMsg.value = null;
         }
         return value;
      }
   }

   public Property getProperty() {
      return myProp;
   }

   public void dispose() {
      myProp = null;
   }
}
