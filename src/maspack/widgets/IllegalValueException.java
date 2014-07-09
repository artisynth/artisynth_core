/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

public class IllegalValueException extends RuntimeException {
   public IllegalValueException (String msg) {
      super (msg);
   }

   public IllegalValueException() {
      super();
   }
}
