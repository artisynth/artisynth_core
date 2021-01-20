/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

/**
 * Parsing token that holds a double value.
 */
public class DoubleToken extends ScanToken {

   double myValue;

   public DoubleToken (double value, int lineno) {
      super (lineno);
      myValue = value;
   }

   public DoubleToken (double value) {
      super ();
      myValue = value;
   }

   public Double value() {
      return myValue;
   }

   public String toString() {
      return "DoubleToken['"+value()+"' line "+lineno()+"]";
   }


}
