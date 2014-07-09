/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.graph;

public class DirectedEdge<A, B> {

   protected B data;		// used for keeping data on this edge
   protected double myCost;	// associated cost for algorithms
   protected Vertex<A,B> myA;
   protected Vertex<A,B> myB;
   
   public DirectedEdge(Vertex<A,B> a, Vertex<A,B> b) {
      this(a,b,null);
   }
   
   public DirectedEdge(Vertex<A,B> a, Vertex<A,B> b, B dataValue) {
      this(a,b,dataValue,1);
   }
   
   public DirectedEdge(Vertex<A,B> a, Vertex<A,B> b, B dataValue, double cost) {
      myA = a;
      myB = b;
      data = dataValue;
      myCost = cost;
   }
   
   // travels along the edge to the vertex on the other end
   public Vertex<A,B> traverse(Vertex<A,B> source) {
      if (source == myA) {
         return myB;
      }
      return myA;
   }
   
   public Vertex<A,B> traverseForwards() {
      return myB;
   }
   
   public Vertex<A,B> traverseBackwards() {
      return myA;
   }
   
   public B getData() {
      return data;
   }
   public void setData(B value) {
      data = value;
   }
   
   public double getCost() {
      return myCost;
   }
   
   public void setCost(double value) {
      myCost = value;
   }
   
   public boolean doesConnect(Vertex<A,B> a, Vertex<A,B> b) {
      
      if ((myA == a) && (myB == b)) {
         return true;
      }
      return false;
   }
   
   public Vertex<A,B> getVertex(int idx) {
      if (idx == 0) {
         return myA;
      } else {
         return myB;
      }
   }
   
}
