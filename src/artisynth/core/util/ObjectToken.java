/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

/**
 * Parsing token that holds a string value.
 */
public class ObjectToken extends ScanToken {

   Object myValue;

   public ObjectToken (Object obj, int lineno) {
      super (lineno);
      myValue = obj;
   }

   public ObjectToken (Object obj) {
      super ();
      myValue = obj;
   }

   public Object value() {
      return myValue;
   }

   public String toString() {
      return "ObjectToken['"+value()+"' line "+lineno()+"]";
   }


}
