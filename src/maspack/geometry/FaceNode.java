/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

public class FaceNode {
   FaceNode next;
   Face face;

   FaceNode (Face face) {
      this.face = face;
      this.next = null;
   }
}
