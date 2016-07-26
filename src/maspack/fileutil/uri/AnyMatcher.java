/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.uri;

/**
 * Used for checking if URIs match anything
 * @author "Antonio Sanchez"
 * Creation date: 24 Oct 2012
 *
 */
public class AnyMatcher implements URIxMatcher {

   @Override
   public boolean matches(URIx uri) {
      return true;
   }
   
}
