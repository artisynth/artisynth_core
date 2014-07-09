/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.graph;

public class Graph<A,B> extends DirectedGraph<A,B> {

   
   public DirectedEdge<A,B> connect(Vertex<A,B> vtx1, Vertex<A,B> vtx2, B objEdge, double cost) {
      DirectedEdge<A,B> edge = super.connect(vtx1, vtx2, objEdge, cost);
      super.connect(vtx2, vtx1, objEdge, cost);
      return edge;   
   }
   
   public int numEdges() {
      return edges.size()/2;
   }
   
}
