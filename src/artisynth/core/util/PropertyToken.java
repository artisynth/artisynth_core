/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import maspack.properties.*;

/**
 * Parsing token that holds a property and its value for properties whose
 * values require postscanning.
 */
public class PropertyToken extends ScanToken {

   Property myProp;
   Object myValue;

   public PropertyToken (Property prop, Object value, int lineno) {
      super (lineno);
      myProp = prop;
      myValue = value;
   }

   public PropertyToken (Property prop, Object value) {
      super ();
      myProp = prop;
      myValue = value;
   }

   public Object value() {
      return myValue;
   }

   public Property getProperty() {
      return myProp;
   }

   public String toString() {
      return "PropertyToken['"+myProp.getName()+"' line "+lineno()+"]";
   }

}
