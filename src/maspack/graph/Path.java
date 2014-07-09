/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.graph;

import java.util.ArrayList;

public class Path<A, B> {

   private ArrayList<Vertex<A,B>> vertices;
   private ArrayList<DirectedEdge<A,B>> edges;
   private double cost;

   public Path (Vertex<A,B> startingPoint) {
      vertices = new ArrayList<Vertex<A,B>>();
      edges = new ArrayList<DirectedEdge<A,B>>();
      vertices.add(startingPoint);
   }

   public Vertex<A,B> getStart() {
      return vertices.get(0);
   }

   public Vertex<A,B> getEnd() {
      return vertices.get(vertices.size() - 1);
   }

   public double getCost() {
      return cost;
   }
   
   public boolean prependToRoute(DirectedEdge<A,B> edge, Vertex<A,B> start) {
      Vertex<A,B> first = vertices.get(0);
      if (!edge.doesConnect(start, first)) {
         System.out.println("edge does not connect end of path!");
         return false;
      }
      
      // add to the route
      vertices.add(0, start);
      edges.add(0, edge);
      cost += edge.getCost();
      return true;
   }
   
   public boolean addToRoute(DirectedEdge<A,B> edge, Vertex<A,B> dest) {

      Vertex<A,B> last = vertices.get(vertices.size() - 1);
      if (!edge.doesConnect(dest, last)) {
         System.out.println("edge does not connect end of path!");
         return false;
      }

      // add to the route
      vertices.add(dest);
      edges.add(edge);
      cost += edge.getCost();
      return true;
   }

   // clear all but the starting point
   public void clearPath() {
      Vertex<A,B> start = getStart();
      vertices.clear();
      edges.clear();
      vertices.add(start);
   }

   public ArrayList<Vertex<A,B>> getVertices() {
      return vertices;
   }

   public ArrayList<DirectedEdge<A,B>> getEdges() {
      return edges;
   }

   public String toString() {
      String path = "";

      path += vertices.get(0).getData().toString();
      for (int i = 0; i < edges.size(); i++) {
         path +=
            "  --( " + edges.get(i).getData().toString() + " )--> "
               + vertices.get(i + 1).getData().toString() + "\n";
      }

      return path;
   }

}
