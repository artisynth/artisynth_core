/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.graph;

import java.util.ArrayList;

// vertex on a graph
public class Vertex<A,B> {
   
   A data;
   ArrayList<DirectedEdge<A,B>> forwardEdges;
   ArrayList<DirectedEdge<A,B>> backwardEdges;
   
   private double myCost;	                        // useful in graph search algorithm
   private DirectedEdge<A,B> lastUsedEdge = null;	// useful in graph search algorithm
   
   public Vertex(A value) {
      data = value;
      forwardEdges = new ArrayList<DirectedEdge<A,B>>();
      backwardEdges = new ArrayList<DirectedEdge<A,B>>();      
   }
   
   public A getData() {
      return data;
   }
   
   public void setData(A value ) {
      data = value;
   }
   
   public void addEdge(DirectedEdge<A,B> edge) {
      if (edge.getVertex(0) == this) {
         forwardEdges.add(edge);
      } else {
         backwardEdges.add(edge);
      }
   }
   
   public void removeEdge(DirectedEdge<A,B> edge) {
      if (edge.getVertex(0) == this) {
         forwardEdges.remove(edge);
      } else {
         backwardEdges.remove(edge);
      }
   }
   
   public ArrayList<DirectedEdge<A,B>> getForwardEdges() {
      return forwardEdges;
   }
   
   public ArrayList<DirectedEdge<A,B>> getBackwardEdges() {
      return backwardEdges;
   }

   public double getCost() {
      return myCost;
   }
   
   public void setCost(double cost) {
      myCost = cost;
   }
   
   public void addToCost(double cost) {
      myCost += cost;
   }
   
   public void setCostIfSmaller(double cost) {
      if (cost < myCost) {
         setCost(cost);
      }
   }
   
   public Vertex<A,B> travel(DirectedEdge<A,B> edge) {
      if (!forwardEdges.contains(edge)) {
         System.out.println("Edge does not exist!");
         return null;
      }
      lastUsedEdge = edge;
      return edge.traverse(this);
   }
   
   public Vertex<A,B> travelBackward(DirectedEdge<A,B> edge) {
      if (!backwardEdges.contains(edge)) {
         System.out.println("Edge does not exist!");
         return null;
      }
      lastUsedEdge = edge;
      return edge.traverseBackwards();
   }
   
   private DirectedEdge<A,B> findPath(Vertex<A,B> vtx, ArrayList<DirectedEdge<A,B>> edges) {
      
      for (DirectedEdge<A,B> edge : edges ) {
         if (edge.doesConnect(this, vtx)) {
            return edge;
         } else if (edge.doesConnect(vtx, this)) {
            return edge;
         }
      }
      return null;
   }
   
   public Vertex<A,B> travel(Vertex<A,B> vtx) {
      DirectedEdge<A,B> edge = findPath(vtx, forwardEdges);
      
      if (edge == null) {
         System.out.println("Cannot get to that vertex");
         return null;
      }
      return travel(edge);
   }
   
   public Vertex<A,B> travelForward(Vertex<A,B> vtx) {
      DirectedEdge<A,B> edge = findPath(vtx, forwardEdges);
      
      if (edge == null) {
         System.out.println("Cannot get to that vertex");
         return null;
      }
      return edge.traverse(this);
   }
   
   
   public Vertex<A,B> travelBackward(Vertex<A,B> vtx) {
      DirectedEdge<A,B> edge = findPath(vtx, backwardEdges);
      
      if (edge == null) {
         System.out.println("Cannot get to that vertex");
         return null;
      }
      return edge.traverse(this);
   }
   
   public DirectedEdge<A,B> getLastTravelled() {
      return lastUsedEdge;
   }
   
   public void clear() {
      forwardEdges.clear();
      backwardEdges.clear();
   }
   
   public int numForwardEdges() {
      return forwardEdges.size();
   }
   
   public int numBackwardEdges() {
      return backwardEdges.size();
   }
   
}
