/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Centralizes loading of native libraries, in case we want to try (at some
 * point) loading them some other way (such as through System.load()), which
 * would allow us to specify the library directory.
 */
public class LibraryLoader {

   public static void load (String name) {
      System.loadLibrary (name);
   }

}