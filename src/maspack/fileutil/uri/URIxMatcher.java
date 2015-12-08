/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.uri;

/**
 * Used to see if a URI matches a specified pattern
 * @author "Antonio Sanchez"
 * Creation date: 29 Oct 2012
 *
 */
public interface URIxMatcher {

   public boolean matches(URIx uri);
   
}
