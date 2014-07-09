/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import maspack.util.BinaryHeap;

// graph with vertices of type A, and Edges of type B
public class DirectedGraph<A, B> {

   ArrayList<Vertex<A,B>> vertices;
   ArrayList<DirectedEdge<A,B>> edges;
   
   private class MinCostComparator implements Comparator<Vertex<A, B>>{
      @Override
      public int compare(Vertex<A,B> o1, Vertex<A,B> o2) {
         double c1 = o1.getCost();
         double c2 = o2.getCost();
         if (c1 < c2) {
            return -1;
         } else if (c1 > c2) {
            return 1;
         }
         return 0;
      }
      
   }

   public DirectedGraph () {
      vertices = new ArrayList<Vertex<A,B>>();
      edges = new ArrayList<DirectedEdge<A,B>>();
   }

   public int disconnect(Vertex<A,B> vtx1, Vertex<A,B> vtx2) {
      DirectedEdge<A,B> edge = findEdge(vtx1, vtx2);
      int nEdges = 0;
      while (edge != null) {
         if (removeEdge(edge)) {
            nEdges ++;
         }
         edge = findEdge(vtx1, vtx2);
      }
      
      edge = findEdge(vtx2, vtx1);
      while (edge != null) {
         if (removeEdge(edge)) {
            nEdges ++;
         }
         edge = findEdge(vtx2, vtx1);
      }
      return nEdges;
   }
   
   public boolean removeVertex(Vertex<A,B> vtx) {
      if (vertices.contains(vtx)) {
         ArrayList<DirectedEdge<A,B>> edgeList = 
            new ArrayList<DirectedEdge<A,B>>(
               vtx.numBackwardEdges() + vtx.numForwardEdges());
         edgeList.addAll(vtx.getBackwardEdges());
         edgeList.addAll(vtx.getForwardEdges());
         
         for (DirectedEdge<A,B> edge : edgeList) {
            removeEdge(edge);
         }
         vertices.remove(vtx);
         return true;
      }
      return false;
   }
   
   public boolean removeEdge(DirectedEdge<A,B> edge) {
      if (edges.contains(edge)) {
         Vertex<A,B> vtx1 = edge.getVertex(0);
         Vertex<A,B> vtx2 = edge.getVertex(1);
         vtx1.removeEdge(edge);
         vtx2.removeEdge(edge);
         edges.remove(edge);
         return true;
      }
      return false;
   }
   
   public boolean addEdge(DirectedEdge<A,B> edge) {
      if (!edges.contains(edge)) {
         Vertex<A,B> vtx1 = edge.myA;
         Vertex<A,B> vtx2 = edge.myB;

         if (!vertices.contains(vtx1)) {
            vertices.add(vtx1);
         }
         if (!vertices.contains(vtx2)) {
            vertices.add(vtx2);
         }
         
         vtx1.addEdge(edge);
         vtx2.addEdge(edge);
         edges.add(edge);
         return true;
      }
      return false;
   }
   
   public DirectedEdge<A,B> connect(Vertex<A,B> vtx1, Vertex<A,B> vtx2, B objEdge, double cost) {
      
      DirectedEdge<A,B> edge;
      // get edge if exists
      edge = findEdge(vtx1, vtx2);
      if (edge != null) {
         if (objEdge == edge.getData()) {
            System.out.println("Warning: edge already exists!!");
         }
      }

      if (!vertices.contains(vtx1)) {
         vertices.add(vtx1);
      }
      if (!vertices.contains(vtx2)) {
         vertices.add(vtx2);
      }
      
      // create edge
      edge = new DirectedEdge<A,B>(vtx1, vtx2, objEdge, cost);
      vtx1.addEdge(edge);
      vtx2.addEdge(edge);

      edges.add(edge);
      return edge;
   }
   
   public DirectedEdge<A,B> connect(A object1, A object2, B objEdge, double cost) {

      Vertex<A,B> vtx1;
      Vertex<A,B> vtx2;

      // get first object
      vtx1 = findVertex(object1);
      if (vtx1 == null) {
         vtx1 = new Vertex<A,B>(object1);
      }

      // get second object
      vtx2 = findVertex(object2);
      if (vtx2 == null) {
         vtx2 = new Vertex<A,B>(object2);
      }

      return connect(vtx1,vtx2, objEdge,cost);
   }

   public Vertex<A,B> findVertex(A obj) {

      for (Vertex<A,B> vtx : vertices) {
         if (vtx.getData() == obj) {
            return vtx;
         }
      }
      return null;
   }

   public DirectedEdge<A,B> findEdge(Vertex<A,B> vtx1, Vertex<A,B> vtx2) {

      for (DirectedEdge<A,B> edge : edges) {
         if (edge.doesConnect(vtx1, vtx2)) {
            return edge;
         }
      }
      return null;
   }

   public Vertex<A,B> traverseEdgesForward(Vertex<A,B> start, B[] edgeData) {
      
      Vertex<A,B> vtx = start;
      
      for (B dat : edgeData) {
         boolean found = false;
         for (DirectedEdge<A,B> edge : vtx.getForwardEdges()) {
            if (edge.getData() == dat) {
               vtx = edge.traverseForwards();
               found = true;
               break;
            }
         }
         if (!found) {
            return null;
         }
      }
      
      return vtx;
   }
   
   public Vertex<A,B> traverseEdgesBackward(Vertex<A,B> start, B[] edgeData) {
      
      Vertex<A,B> vtx = start;
      for (B dat : edgeData) {
         boolean found = false;
         for (DirectedEdge<A,B> edge : vtx.getBackwardEdges()) {
            if (edge.getData() == dat) {
               vtx = edge.traverseBackwards();
               found = true;
               break;
            }
         }
         if (!found) {
            return null;
         }
      }
      
      return vtx;
   }
   
   public Vertex<A,B> traverseVerticesForward(Vertex<A,B> start, A[] vtxData) {
      
      Vertex<A,B> vtx = start;
      for (A dat : vtxData) {
         boolean found = false;
         for (DirectedEdge<A,B> edge : vtx.getForwardEdges()) {
            if (edge.getVertex(1).getData() == dat) {
               vtx = edge.traverseForwards();
               found = true;
               break;
            }
         }
         if (!found) {
            return null;
         }
      }   
      return vtx;
   }
   
   public Vertex<A,B> traverseVerticesBackward(Vertex<A,B> start, A[] vtxData) {
      
      Vertex<A,B> vtx = start;
      for (A dat : vtxData) {
         boolean found = false;
         for (DirectedEdge<A,B> edge : vtx.getBackwardEdges()) {
            if (edge.getVertex(0).getData() == dat) {
               vtx = edge.traverseForwards();
               found = true;
               break;
            }
         }
         if (!found) {
            return null;
         }
      }
      return vtx;
   }
   
   public DirectedEdge<A,B> findEdge(A obj1, A obj2) {
      Vertex<A,B> vtx1 = findVertex(obj1);
      if (vtx1 == null) {
         return null;
      }

      Vertex<A,B> vtx2 = findVertex(obj2);
      if (vtx2 == null) {
         return null;
      }

      return findEdge(vtx1, vtx2);
   }

   // uses Dijkstra's algorithm to find the shortest path between vertices vtx1
   // and vtx2
   public Path<A,B> shortestPath(Vertex<A,B> vtxStart, Vertex<A,B> vtxEnd) {

      if ((!vertices.contains(vtxStart)) || (!vertices.contains(vtxEnd))) {
         System.out.println("Source or destination not found on graph");
         return null;
      }

      // array of costs
      for (int i = 0; i < vertices.size(); i++) {
         vertices.get(i).setCost(Double.MAX_VALUE);
      }
      
      MinCostComparator costComparator = new MinCostComparator();

      BinaryHeap<Vertex<A,B>> unvisited = new BinaryHeap<Vertex<A,B>>(costComparator);
      Vertex<A,B> currLoc = vtxStart;
      vtxStart.setCost(0);
      unvisited.set(vertices);

      while (unvisited.size() > 0) {

         // find vertex with smallest cost
         currLoc = unvisited.poll(); 
         if (currLoc == vtxEnd) {
            break;
         }

         // check each edge connected to this vertex
         boolean costsChanged = false;
         for (DirectedEdge<A,B> edge : currLoc.getForwardEdges()) {
            Vertex<A,B> look = edge.traverse(currLoc); // vertex we're looking
                                                       // at
            double altCost = currLoc.getCost() + edge.getCost(); // possible
                                                              // alternate cost
            if (altCost < look.getCost()) {
               look.setCost(altCost);     // set alternate cost
               look.travelBackward(edge); // trigger last travelled indicator
               costsChanged = true;
            }
         }
         // re-order heap if required
         if (costsChanged) {
            unvisited.update();
         }

      }

      // we haven't found the end
      if (currLoc != vtxEnd) {
         return null;
      }

      // now we should have found the destination, travel backwards until we hit
      // the start
      Path<A,B> path = new Path<A,B>(vtxEnd);
      Vertex<A,B> nextLoc = null;
      DirectedEdge<A,B> nextEdge = null;
      while (currLoc != vtxStart) {
         nextEdge = currLoc.getLastTravelled();
         nextLoc = currLoc.travelBackward(nextEdge);
         path.prependToRoute(nextEdge, nextLoc);
         currLoc = nextLoc;
      }
      return path;
   }

   // shortest path to one of many vertices
   public Path<A,B> shortestPath(Vertex<A,B> vtxStart, ArrayList<Vertex<A,B>> vtxEnd) {

      if (!vertices.contains(vtxStart)) {
         System.out.println("Source not found on graph");
         return null;
      }

      // array of costs
      for (int i = 0; i < vertices.size(); i++) {
         vertices.get(i).setCost(Double.MAX_VALUE);
      }

      MinCostComparator costComparator = new MinCostComparator();
      BinaryHeap<Vertex<A,B>> unvisited = new BinaryHeap<Vertex<A,B>>(costComparator);
      Vertex<A,B> currLoc = vtxStart;
      vtxStart.setCost(0);
      unvisited.set(vertices);

      while (unvisited.size() > 0) {

         // find vertex with smallest cost
         currLoc = unvisited.poll();
         if (vtxEnd.contains(currLoc)) {
            break;
         }

         // check each edge connected to this vertex
         boolean costsChanged = false;
         for (DirectedEdge<A,B> edge : currLoc.getForwardEdges()) {
            Vertex<A,B> look = edge.traverse(currLoc); // vertex we're looking
                                                       // at
            double altCost = currLoc.getCost() + edge.getCost(); // possible
                                                              // alternate cost
            if (altCost < look.getCost()) {
               look.setCost(altCost); // set alternate cost
               look.travelBackward(edge); // trigger last travelled indicator
               costsChanged = true;
            }
         }
         // update queue
         if (costsChanged) {
            unvisited.update();
         }

      }

      // we haven't found the end
      if (!vtxEnd.contains(currLoc)) {
         return null;
      }

      // now we should have found the destination, travel backwards until we hit
      // the start
      Path<A,B> path = new Path<A,B>(currLoc);
      Vertex<A,B> nextLoc = null;
      DirectedEdge<A,B> nextEdge = null;
      while (currLoc != vtxStart) {
         nextEdge = currLoc.getLastTravelled();
         nextLoc = currLoc.travelBackward(nextEdge);
         path.prependToRoute(nextEdge, nextLoc);
         currLoc = nextLoc;
      }
      return path;
   }
   
   public Path<A,B> shortestPath(A start, List<A> endList) {
      Vertex<A,B> vtxStart = findVertex(start);
      ArrayList<Vertex<A,B>> vtxEndList = new ArrayList<Vertex<A,B>>(endList.size());
      for (A val : endList) {
         Vertex<A,B> vtx = findVertex(val);
         if (vtx != null) {
            vtxEndList.add(vtx);
         }
      }
      return shortestPath(vtxStart, vtxEndList);
      
   }
   
   public Path<A,B> shortestPath(A start, A end) {
      Vertex<A,B> vtxStart = findVertex(start);
      Vertex<A,B> vtxEnd = findVertex(end);

      return shortestPath(vtxStart, vtxEnd);
   }

   public ArrayList<DirectedEdge<A,B>> getEdges() {
      return edges;
   }
   
   public ArrayList<Vertex<A,B>> getVertices() {
      return vertices;
   }
   
   public Vertex<A,B> getVertex(int idx) {
      return vertices.get(idx);
   }
   
   public DirectedEdge<A,B> getDirectedEdge(int idx) {
      return edges.get(idx);
   }
   
   
   //   private Vertex<A,B> findCheapest(BinaryHeap<Vertex<A,B>> vtxs) {
   //      
   //      Vertex<A,B> cheapest = vtxs.peek();
   //      
   //      //      double minCost = Double.MAX_VALUE;
   //      //
   //      //      for (Vertex<A,B> vtx : vtxs) {
   //      //         if (vtx.getCost() < minCost) {
   //      //            cheapest = vtx;
   //      //            minCost = vtx.getCost();
   //      //         }
   //      //      }
   //      //
   //      //      if (minCost > 0.99*Double.MAX_VALUE) {
   //      //         return null;
   //      //      }
   //
   //      return cheapest;
   //   }

   public void clear() {
      edges.clear();
      vertices.clear();
   }
   
   public ArrayList<Vertex<A,B>> findStartNodes() {
      
      ArrayList<Vertex<A,B>> starts  = new ArrayList<Vertex<A,B>>();
      for (Vertex<A,B> vtx : vertices) {
         if (vtx.getBackwardEdges().size() == 0) {
            starts.add(vtx);
         }
      }
      
      return starts;
   }
   
   public ArrayList<Vertex<A,B>> findEndNodes() {
      ArrayList<Vertex<A,B>> ends  = new ArrayList<Vertex<A,B>>();
      for (Vertex<A,B> vtx : vertices) {
         if (vtx.getForwardEdges().size() == 0) {
            ends.add(vtx);
         }
      }
      return ends;
   }
   
   public int numVertices() {
      return vertices.size();
   }
   
   public int numDirectedEdges() {
      return edges.size();
   }

   public int getVertexIndex(Vertex<A,B> vtx) {
      return vertices.indexOf(vtx);
   }
   
   public int getDirectedEdgeIndex(DirectedEdge<A,B> edge) {
      return edges.indexOf(edge);
   }
   
   public <C,D> DirectedGraph<C,D> exchangeData(HashMap<A,C> vtxMap, HashMap<B,D> edgeMap) {
      return exchangeData(this, vtxMap, edgeMap);
   }
   
   public <C> DirectedGraph<C,B> exchangeVertexData(HashMap<A,C> vtxMap) {
      return exchangeVertexData(this, vtxMap);
   }
   
   public <D> DirectedGraph<A,D> exchangeEdgeData(HashMap<A,D> edgeMap) {
      return exchangeEdgeData(this, edgeMap);
   }
   
   public static <A,B,C,D> DirectedGraph<C,D> exchangeData(DirectedGraph<A,B> graph, HashMap<A,C> vtxMap, HashMap<B,D> edgeMap) {
      DirectedGraph<C,D> newGraph = new DirectedGraph<C,D>();
      
      HashMap<Vertex<A,B>, Vertex<C,D>> transferMap = 
         new HashMap<Vertex<A,B>, Vertex<C,D>>(graph.numVertices());
      
      for (Vertex<A,B> vtx : graph.vertices) {
         A data = vtx.getData();
         C newData = null;
         if (data != null) {
            newData = vtxMap.get(data);
         }
         Vertex<C,D> nVtx = new Vertex<C,D>(newData);
         nVtx.setCost(vtx.getCost());
         transferMap.put(vtx, nVtx);
      }
      
      for (DirectedEdge<A,B> edge : graph.edges) {
         B data = edge.getData();
         D newData = null;
         if (data != null) {
            newData = edgeMap.get(data);
         }
         newGraph.connect(
            transferMap.get(edge.getVertex(0)), 
            transferMap.get(edge.getVertex(1)), 
            newData, edge.getCost());
      }
      
      return newGraph;
   }
   
   public static <A,B,C> DirectedGraph<C,B> exchangeVertexData(DirectedGraph<A,B> graph, HashMap<A,C> vtxMap) {
      DirectedGraph<C,B> newGraph = new DirectedGraph<C,B>();
      
      HashMap<Vertex<A,B>, Vertex<C,B>> transferMap = 
         new HashMap<Vertex<A,B>, Vertex<C,B>>(graph.numVertices());
      
      for (Vertex<A,B> vtx : graph.vertices) {
         A data = vtx.getData();
         C newData = null;
         if (data != null) {
            newData = vtxMap.get(data);
         }
         Vertex<C,B> nVtx = new Vertex<C,B>(newData);
         nVtx.setCost(vtx.getCost());
         transferMap.put(vtx, nVtx);
      }
      
      for (DirectedEdge<A,B> edge : graph.edges) {
         newGraph.connect(
            transferMap.get(edge.getVertex(0)), 
            transferMap.get(edge.getVertex(1)), 
            edge.getData(), edge.getCost());
      }
      
      return newGraph;
      
   }
   
   public static <A,B,D> DirectedGraph<A,D> exchangeEdgeData(DirectedGraph<A,B> graph, HashMap<A,D> edgeMap) {
      DirectedGraph<A,D> newGraph = new DirectedGraph<A,D>();
      
      HashMap<Vertex<A,B>, Vertex<A,D>> transferMap = 
         new HashMap<Vertex<A,B>, Vertex<A,D>>(graph.numVertices());
      
      for (Vertex<A,B> vtx : graph.vertices) {
         Vertex<A,D> nVtx = new Vertex<A,D>(vtx.getData());
         nVtx.setCost(vtx.getCost());
         transferMap.put(vtx, nVtx);
      }
      
      for (DirectedEdge<A,B> edge : graph.edges) {
         B data = edge.getData();
         D newData = null;
         if (data != null) {
            newData = edgeMap.get(data);
         }
         newGraph.connect(
            transferMap.get(edge.getVertex(0)), 
            transferMap.get(edge.getVertex(1)), 
            newData, edge.getCost());
      }
      
      return newGraph;
      
   }
   
   public static <A,B,C,D> DirectedGraph<C,D> cloneStructure(DirectedGraph<A,B> orig) {
      
      DirectedGraph<C,D> out = new DirectedGraph<C,D>();
      HashMap<Vertex<A,B>,Vertex<C, D>> vtxMap = 
         new HashMap<Vertex<A,B>,Vertex<C, D>>(orig.numVertices());
      
      for (Vertex<A, B> vtx : orig.getVertices()) {
         Vertex<C,D> newVtx = new Vertex<C,D>(null);
         vtxMap.put(vtx, newVtx);
      }
      
      for (DirectedEdge<A,B> edge : orig.getEdges()) {
         out.connect(vtxMap.get(edge.getVertex(0)), vtxMap.get(edge.getVertex(1)), null, 0);
      }
      
      return out;
   }
   
   public <C,D> DirectedGraph<C,D> cloneStructure() {
      return cloneStructure(this);
   }
   
   
}
