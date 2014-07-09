/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

/**
 * Used for maintaining component numbers within CompositeComponents
 */
class NumberNode {
   int num;
   NumberNode next;

   NumberNode (int n) {
      num = n;
      next = null;
   }
}
