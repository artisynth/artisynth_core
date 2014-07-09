/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

public class HalfEdgeNode {
   HalfEdgeNode next;
   HalfEdge he;

   HalfEdgeNode (HalfEdge he) {
      this.he = he;
      this.next = null;
   }
}
