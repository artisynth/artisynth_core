/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import maspack.geometry.BSPTree;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.BVIntersector;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Sphere;
import maspack.geometry.TriTriIntersection;
import maspack.graph.DirectedEdge;
import maspack.graph.DirectedGraph;
import maspack.graph.Vertex;

public class IntersectionFactory {

   public static <A,B> DirectedGraph<A,B> buildConnectivityGraph(B[] nodeList, boolean[][] cChart) {
      return buildConnectivityGraph(Arrays.asList(nodeList), cChart);
   }
   
   public static <A,B> DirectedGraph<A,B> buildConnectivityGraph(List<B> nodeList, boolean[][] cChart) {
      
      DirectedGraph<A,B> graph = new DirectedGraph<A,B>();
      
      // initialize
      Vertex<A,B> root = 
         new Vertex<A,B>(null);
      ArrayList<Vertex<A,B>> endVtxs = 
         new ArrayList<Vertex<A,B>>(nodeList.size());
      ArrayList<int[]> endSets = 
         new ArrayList<int[]>(nodeList.size());
      
      for (int i=0; i<nodeList.size(); i++) {
         Vertex<A,B> vtx = new Vertex<A,B>(null);
         int [] nodeIdxs = new int[]{i};
         endSets.add(nodeIdxs);
         graph.connect(root, vtx, nodeList.get(i), 0);
         endVtxs.add(vtx);
      }
      recursivelyAppendConnectivity(graph, nodeList, cChart, endVtxs, endSets, 1);
      
      return graph;
   }
   
   public static DirectedGraph<int[],Integer> buildConnectivityGraph(boolean[][] cChart) {
      
      DirectedGraph<int[],Integer> graph = new DirectedGraph<int[],Integer>();
      int n = cChart.length;
      
      // initialize
      Vertex<int[],Integer> root = 
         new Vertex<int[],Integer>(new int[]{});
      
      ArrayList<Vertex<int[],Integer>> endVtxs = 
         new ArrayList<Vertex<int[],Integer>>(n);
      
      ArrayList<int[]> endSets = 
         new ArrayList<int[]>(n);
      
      // root -> {i}, all singletons
      for (int i=0; i<n; i++) {
         int [] nodeIdxs = new int[]{i};
         Vertex<int[],Integer> vtx = new Vertex<int[],Integer>(nodeIdxs);
         graph.connect(root, vtx, i, 0);
         
         endSets.add(nodeIdxs);
         endVtxs.add(vtx);
      }
      recursivelyAppendConnectivity(graph, cChart, endVtxs, endSets, 1);
      
      return graph;
   }
   
   private static void recursivelyAppendConnectivity(DirectedGraph<int[],Integer> graph, boolean[][] cChart,
      ArrayList<Vertex<int[],Integer>> endVtxs, ArrayList<int[]> endSets, int n) {
      
      ArrayList<Vertex<int[],Integer>> newEndVtxs = new ArrayList<Vertex<int[],Integer>>();
      ArrayList<int[]> newEndSets = new ArrayList<int[]>();
      int nNewSets = 0;
      
      int nEnds = endVtxs.size();
      int nNodes = cChart.length;
      boolean tried[][] = new boolean[nEnds][nNodes];
      
      for (int i=0; i<nEnds; i++) {         
         int[] set = endSets.get(i);
         for (int j=0; j<nNodes; j++) {
         
            if (!tried[i][j]) {
               
               boolean intersect = true;
               boolean duplicated = false;
               for (int k = 0; k<n; k++) {
                  if (set[k] == j) {
                     duplicated = true;
                     break;
                  } else if (!cChart[set[k]][j]) {
                     intersect = false;
                     break;
                  }
               }
               
               if (duplicated) {
                  tried[i][j] = true;
                  continue;
               }
               
               Vertex<int[],Integer> newVtx = null;
               int[] newSet = new int[n+1];
               for (int k=0; k<n; k++) {
                  newSet[k] = set[k];
               }
               newSet[n] = j;
               // Arrays.sort(newSet); turns out I don't need to
               
               if (intersect) {
                  nNewSets++;
                  newVtx = new Vertex<int[],Integer>(newSet);
                  newEndVtxs.add(newVtx);
                  newEndSets.add(newSet);
                  graph.connect(endVtxs.get(i), newVtx, j, 0);
               }
               
               // go through other possible (n+1)-sets and possibly connect to graph
               for (int k=i+1; k<nEnds; k++) {
                  int idx = findSingleMissingIndex(newSet, endSets.get(k));
                  if (idx >= 0) {
                     tried[k][idx] = true;
                     if (intersect) {
                        graph.connect(endVtxs.get(k), newVtx, idx, 0);
                     }
                  }
                  
               }
               tried[i][j] = true;
               
               
            }
         }
      }
      
      if (nNewSets > 0) {
         recursivelyAppendConnectivity(graph, cChart, newEndVtxs, newEndSets, n+1);
      }
      
      
   }
   
   private static <A,B> void recursivelyAppendConnectivity(DirectedGraph<A,B> graph, List<B> nodes, boolean[][] cChart,
      ArrayList<Vertex<A,B>> endVtxs, ArrayList<int[]> endSets, int n) {
      
      ArrayList<Vertex<A,B>> newEndVtxs = new ArrayList<Vertex<A,B>>();
      ArrayList<int[]> newEndSets = new ArrayList<int[]>();
      int nNewSets = 0;
      
      int nEnds = endVtxs.size();
      int nNodes = nodes.size();
      boolean tried[][] = new boolean[nEnds][nNodes];
      
      for (int i=0; i<nEnds; i++) {
         
         int[] set = endSets.get(i);
         for (int j=0; j<nNodes; j++) {
         
            if (!tried[i][j]) {
               
               boolean intersect = true;
               boolean duplicated = false;
               for (int k = 0; k<n; k++) {
                  if (set[k] == j) {
                     duplicated = true;
                     break;
                  } else if (!cChart[set[k]][j]) {
                     intersect = false;
                     break;
                  }
               }
               
               if (duplicated) {
                  tried[i][j] = true;
                  continue;
               }
               
               Vertex<A,B> newVtx = null;
               int[] newSet = new int[n+1];
               for (int k=0; k<n; k++) {
                  newSet[k] = set[k];
               }
               newSet[n] = j;
               // Arrays.sort(newSet); turns out I don't need to
               
               if (intersect) {
                  nNewSets++;
                  newVtx = new Vertex<A,B>(null);
                  newEndVtxs.add(newVtx);
                  newEndSets.add(newSet);
                  graph.connect(endVtxs.get(i), newVtx, nodes.get(j), 0);
               }
               
               // go through other possible (n+1)-sets and possibly connect to graph
               for (int k=i+1; k<nEnds; k++) {
                  int idx = findSingleMissingIndex(newSet, endSets.get(k));
                  if (idx >= 0) {
                     tried[k][idx] = true;
                     if (intersect) {
                        graph.connect(endVtxs.get(k), newVtx, nodes.get(idx), 0);
                     }
                  }
                  
               }
               tried[i][j] = true;
               
               
            }
         }
      }
      
      if (nNewSets > 0) {
         recursivelyAppendConnectivity(graph, nodes, cChart, newEndVtxs, newEndSets, n+1);
      }
      
   }
   
   private static int findSingleMissingIndex(int[] superset, int[] subset) {
      int out = -1;
      int nMiss = 0;
      for (int i=0; i<superset.length; i++) {
         if (superset[i] != subset[i-nMiss]) {
            out = superset[i];
            nMiss++;
            if (nMiss > 1){
               return -1;
            }
         }
      }
      return out;
   }
   
   public static boolean[][] buildIntersectionChart(Sphere[] sphereList) {
      int n = sphereList.length;
      boolean out[][] = new boolean[n][n];
      
      for (int i=0; i<n; i++) {
         out[i][i] = true;
         Sphere sphere1 = sphereList[i];
         for (int j=i+1; j<n; j++) {
            boolean intersects = sphere1.intersects(sphereList[j]);
            out[i][j] = intersects;
            out[j][i] = intersects;
         }
      }
      return out;
   }
   
   public static boolean[][] buildIntersectionChart(BSPTree[] bspList) {
      
      int n = bspList.length;
      boolean out[][] = new boolean[n][n];
      
      for (int i=0; i<n; i++) {
         BSPTree mesh1 = bspList[i];
         
         out[i][i] = true;
         for (int j=i+1; j<n; j++) {
            BSPTree mesh2 = bspList[j];
            
            boolean intersects = false;
            if (mesh1.intersect(mesh2).numPolygons() > 0) {
               intersects = true;
            }
            out[i][j] = intersects;
            out[j][i] = intersects;
         }
      }
      
      return out;
      
   }
   
   public static boolean[][] buildIntersectionChart(PolygonalMesh[] meshList, double tol) {
      
      int n = meshList.length;
      boolean out[][] = new boolean[n][n];
      ArrayList<TriTriIntersection> intersections = new ArrayList<TriTriIntersection>();
//      TriangleIntersector ti = new TriangleIntersector();
      BVFeatureQuery query = new BVFeatureQuery();
      BVIntersector intersector = new BVIntersector();
      
      for (int i=0; i<n; i++) {
         PolygonalMesh mesh1 = meshList[i];
         
         out[i][i] = true;
         for (int j=i+1; j<n; j++) {
            PolygonalMesh mesh2 = meshList[j];
            
            boolean intersects = false;
//            if (obbt1.intersectFully(obbt2, intersections, ti)) {
//               intersects = true;
//               intersections.clear();
//            }
            if (intersector.intersectMeshMesh (intersections, mesh1, mesh2)) {
               intersects = true;
               intersections.clear();
            }
            else if (query.isInsideOrientedMesh (
                         mesh1, mesh2.getVertex(0).getPosition(), tol)  ||
                      query.isInsideOrientedMesh (
                         mesh2, mesh1.getVertex(0).getPosition(), tol)) {
               intersects = true;
            }
            out[i][j] = intersects;
            out[j][i] = intersects;
         }
      }
      
      return out;
   }
   
   public static void buildSpatialPartition(DirectedGraph<BSPTree,BSPTree> graph, BSPTree base) {
      
      HashMap<Vertex<BSPTree,BSPTree>,Boolean> computed = 
         new HashMap<Vertex<BSPTree,BSPTree>, Boolean>(graph.numVertices());
      
      for (Vertex<BSPTree,BSPTree> vtx : graph.getVertices()) {
         if (!computed.containsKey(vtx) ) {
            recursivelyIntersect(graph, vtx, computed, base);
         }
      }
      
      // subtract non-intersecting regions
      for (Vertex<BSPTree,BSPTree> vtx : graph.getVertices()) {
         BSPTree vtxData = vtx.getData();
         if (vtxData != null) {
            for (DirectedEdge<BSPTree,BSPTree> edge : vtx.getForwardEdges()) {
               vtxData = vtxData.subtract(edge.getData());
               if (vtxData.numPolygons() < 1) {
                  break;
               }
            }
            vtx.setData(vtxData);
         }
      }
      
   }

   private static void recursivelyIntersect(DirectedGraph<BSPTree,BSPTree> graph, Vertex<BSPTree,BSPTree> vtx, 
      HashMap<Vertex<BSPTree,BSPTree>,Boolean> computed, BSPTree base) {
      
      if (vtx.numBackwardEdges() == 0) {
         vtx.setData(base);
         computed.put(vtx, true);
         return;
      } 
      
      // build tree backwards
      for (DirectedEdge<BSPTree,BSPTree> edge : vtx.getBackwardEdges()) {
         Vertex<BSPTree, BSPTree> backVtx = edge.traverseBackwards();
         if (!computed.containsKey(backVtx)) {
            recursivelyIntersect(graph, backVtx, computed, base);
         }         
      }
      
      // build current entry from one backward node
      DirectedEdge<BSPTree, BSPTree> backEdge = vtx.getBackwardEdges().get(0);
      Vertex<BSPTree,BSPTree> backVtx = backEdge.traverseBackwards();
      BSPTree backData = backVtx.getData();
      BSPTree edgeData =  backEdge.getData();
      
      // intersect previous with edge
      BSPTree isect = null;
      if (backData == null) {
         isect = edgeData;
      } else {
         isect = backData.intersect(edgeData);
      }
      if (isect.numPolygons() < 1) {
         recursivelyTrimForward(graph, vtx);
         return;
      }
      
      vtx.setData(isect);
      // set as computed
      computed.put(vtx, true);
   }  
   
   private static <A,B> void recursivelyTrimForward(DirectedGraph<A,B> graph, Vertex<A,B> vtx) {
      if (vtx.numForwardEdges() < 1) {
         graph.removeVertex(vtx);
         return;
      }
      
      for (DirectedEdge<A,B> edge : vtx.getForwardEdges()) {
         recursivelyTrimForward(graph, edge.traverseForwards());
      }
      graph.removeVertex(vtx);
      
   }
   
}


