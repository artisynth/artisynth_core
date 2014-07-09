/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

public class TestException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   public TestException() {
      super();
   }

   public TestException (String msg) {
      super (msg);
   }
}
