/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

public class NativeLibraryException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   public NativeLibraryException (String msg) {
      super (msg);
   }
}
