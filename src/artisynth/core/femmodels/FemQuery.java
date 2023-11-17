/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.*;

import artisynth.core.materials.TensorUtils;

import maspack.matrix.*;
import maspack.util.*;
import maspack.geometry.*;

/** 
 * Provides query methods for FEM models, at present primarily concerned with
 * finding various sets of nodes.
 */
public class FemQuery {

   private static ArrayList<FemNode3d> createNodeArray (
      Collection<FemNode3d> nodes) {
      ArrayList<FemNode3d> array = new ArrayList<>();
      array.addAll (nodes);
      return array;
   }

   private static HashSet<FemNode3d> getHashSet (
      Collection<FemNode3d> nodes) {
      if (nodes instanceof HashSet) {
         return (HashSet<FemNode3d>)nodes;
      }
      else {
         return new LinkedHashSet<>(nodes);
      }
   }

   private static void nodesFromVertices (
      FemModel3d fem, ArrayList<FemNode3d> nodes, Collection<Vertex3d> verts) {
      FemMeshComp surfc = fem.getSurfaceMeshComp();
      for (Vertex3d vtx : verts) {
         FemNode3d node = surfc.getNodeForVertex (vtx);
         if (node != null) {
            nodes.add (node);
         }
      }
   }

   /**
    * Find all the surface nodes of an FEM model that lie along an <i>edge
    * line</i>, starting at {@code node0}. Edges along the edge line must have
    * a <i>bend angle</i> that is {@code >= minBendAngle}, where the bend angle
    * is the absolute value of the angle between an edge's adjacent faces. In
    * addition, the <i>edge angle</i> between adjacent edges must be {@code <=
    * maxEdgeAngle}. Branching is allowed if {@code allowBranching} is {@code
    * true}; otherwise, edges are followed so as to minimize the edge angle.
    *
    * <p>The returned node list will always include {@code node0}.
    *
    * @param fem FEM model for which the query is being made
    * @param node0 starting node
    * @param minBendAngle minimum bend angle between adjacent faces (radians)
    * @param maxEdgeAngle maximum edge angle between adjacent edges (radians)
    * @param allowBranching if {@code true}, allow branching
    * @return a list of the nodes on the line
    */
   public static ArrayList<FemNode3d> findEdgeLineNodes (
      FemModel3d fem, FemNode3d node0, double minBendAngle, 
      double maxEdgeAngle, boolean allowBranching) {
      ArrayList<FemNode3d> nodes = new ArrayList<>();
      Vertex3d vtx0 = fem.getSurfaceVertex (node0);
      if (vtx0 != null) {
         HashSet<Vertex3d> verts = PolygonalMeshQuery.findEdgeLineVertices(
            vtx0, minBendAngle, maxEdgeAngle, allowBranching);
         nodesFromVertices (fem, nodes, verts);
      }
      return nodes;      
   }

   /**
    * Find all the surface nodes of an FEM model in a <i>patch</i> containing
    * {@code node0}.  The patch is the collection of all faces surrounding
    * {@code node0} for which the <i>bend angle</i> between them is {@code <=
    * maxBendAngle}, where the bend angle is the absolute value of the angle
    * between two faces about their common edge.
    *
    * <p>The returned node list will always include {@code node0}.
    * 
    * @param fem FEM model for which the query is being made
    * @param node0 starting node within the patch
    * @param maxBendAngle maximum bend angle between adjacent faces (radians)
    * @return a list of the nodes in the patch
    */
   public static ArrayList<FemNode3d> findPatchNodes (
      FemModel3d fem, FemNode3d node0, double maxBendAngle) {
      ArrayList<FemNode3d> nodes = new ArrayList<>();
      Vertex3d vtx0 = fem.getSurfaceVertex (node0);
      if (vtx0 != null) {
         HashSet<Vertex3d> verts =
            PolygonalMeshQuery.findPatchVertices(vtx0, maxBendAngle);
         nodesFromVertices (fem, nodes, verts);
      }
      return nodes;      
   }

   /**
    * Find all the surface nodes of an FEM model that border a <i>patch</i>
    * containing {@code node0}.  The patch is the collection of all faces
    * surrounding {@code node0} for which the <i>bend angle</i> between them is
    * {@code <= maxBendAngle}, where the bend angle is the absolute value of
    * the angle between two faces about their common edge.
    *
    * @param fem FEM model for which the query is being made
    * @param node0 starting node within the patch
    * @param maxBendAngle maximum bend angle between adjacent faces (radians)
    * @return a list of the nodes on the patch border
    */
   public static ArrayList<FemNode3d> findPatchBoundaryNodes (
      FemModel3d fem, FemNode3d node0, double maxBendAngle) {
      ArrayList<FemNode3d> nodes = new ArrayList<>();
      Vertex3d vtx0 = fem.getSurfaceVertex (node0);
      if (vtx0 != null) {
         HashSet<Vertex3d> verts =
            PolygonalMeshQuery.findPatchVertices (vtx0, maxBendAngle);
         HashSet<Vertex3d> border = 
            PolygonalMeshQuery.findBoundaryVertices (verts);
         nodesFromVertices (fem, nodes, border);
      }
      return nodes;      
   }

   /**
    * Returns all the nodes in an FEM model that are <i>adjacent</i> to a given
    * node. If the mesh component {@code mcomp} is non-{@code null}, the search
    * is restricted to nodes which lie on the mesh surface and are coincident
    * with mesh vertices.
    *
    * <p>Adjacent nodes are found by examing all the elements which contain the
    * node and collecting the adjacent nodes in each element.
    *
    * <p>For adjacency not constrained to a mesh surface, adjacent nodes within
    * each element are determined as follows:
    *
    * <ul>
    *   <li>If the element is <i>not</i> quadratic, then all of its other nodes
    *   are considered adjacent to {@code node}.
    *
    *   <li>If the element is quadratic and {@code node} is a
    *   corner node, then adjacency is restricted to nodes which are immediately
    *   adjacent along a face edge.
    *
    *   <li>If the element is quadratic and {@code node} is a edge node, then
    *   adjacency is restricted to nodes which are either immediately adjacent
    *   or one away along a face edge.
    * </ul>
    *
    * <p>If adjacency is constrained to a mesh surface, then adjacent nodes
    * within an element are chosen from those nodes which lie in element faces
    * that both contain {@code node} and are also contained in the mesh
    * surface. For each of these eligible element faces:
    *
    * <ul>
    *   <li>If the element is <i>not</i> quadratic, then the face will be
    *   either a triangle or a quad, and all of its nodes are considered
    *   adjacent to {@code node}, <i>unless</i> {@code forFill} is {@code true},
    *   in which case adjacency for quad faces is restricted to the two
    *   nodes nearest to {@code node} (this latter condition prevents
    *   region filling on surfaces from passing through diagonal boundaries).
    *
    *   <li>If the element is quadratic and {@code node} is a corner node, then
    *   adjacency is restricted to the two immediately adjacent edge nodes.
    *
    *   <li>If the element is quadratic and {@code node} is an edge node, then
    *   adjacency is restricted to the two immediately adjacent corner nodes,
    *   plus the two edge nodes which are adjacent to them.
    *
    * </ul>
    *
    * @param node node for which adjacent nodes are desired
    * @param mcomp if non-{@code null}, restricts the search to nodes
    * coincident with mesh vertices
    * @param forFill if {@code true}, adjacency is being requested for
    * region filling purposes
    * @return list of adjacent nodes
    */
   public static ArrayList<FemNode3d> getAdjacentNodes (
      FemNode3d node, FemMeshComp mcomp, boolean forFill) {

      if (mcomp != null && mcomp.getVertexForNode(node) == null) {
         
         return new ArrayList<>();
      }
      LinkedHashSet<FemNode3d> nodes = new LinkedHashSet<>();         
      for (FemElement3dBase elem : node.getAdjacentElements()) {
         if (mcomp != null) {
            collectAdjacentSurfaceNodes (nodes, elem, node, forFill, mcomp);
         }
         else {
            collectAdjacentNodes (nodes, elem, node, forFill);
         }
      }
      return new ArrayList<>(nodes);
   }

   private static void collectAdjacentSurfaceNodes (
      LinkedHashSet<FemNode3d> nodes, FemElement3dBase elem,
      FemNode3d node, boolean forFill, FemMeshComp mcomp) {
               
      FemNode3d[] enodes = elem.getNodes();
      // find node index within the element
      int nodeIdx = elem.getNodeIndex(node);
      if (nodeIdx == -1) {
         throw new InternalErrorException (
            "Node number " + node.getNumber() +
            " not found in adjacent element");
      }
      // Examine all element faces. If mcomp != null, eliminate those for
      // which all the nodes do not correspond to mesh vertices. Then choose
      // only faces which contain the node.

      // faceIdxs gives local node indices for all faces in a single array,
      // which also contains the sizes for each face
      int[] faceIdxs = elem.getFaceIndices();
      int fsize = 0; // number of nodes in the face
      int fbase = 0; // start index of the face's local node indices
      for (int i=0; i<faceIdxs.length; i+=(fsize+1)) {
         fsize = faceIdxs[i];
         fbase = i+1;
         // eliminate face if all nodes do not correspond to vertices of
         // mcomp
         boolean allNodesOnSurface = true;               
         for (int j=fbase; j<fbase+fsize; j++) {
            if (mcomp.getVertexForNode(enodes[faceIdxs[j]]) == null) {
               allNodesOnSurface = false;
               break;
            }
         }
         if (!allNodesOnSurface) {
            continue;
         }
         int jn = -1;
         for (int j=fbase; j<fbase+fsize; j++) {
            if (faceIdxs[j] == nodeIdx) {
               jn = j;
               break;
            }
         }
         if (jn != -1) {
            // face contains the node.
            int kn = jn-fbase; // index of the node within the face
            if (fsize <= 4) {
               if (fsize == 3 || forFill) {
                  // triangle face, or quad face when checking for fill: test
                  // adjacent vertices
                  FemNode3d n;
                  n = enodes[faceIdxs[(kn-1)%fsize+fbase]];
                  if (mcomp.isSurfaceEdge (node, n)) {
                      nodes.add (n);
                  }
                  n = enodes[faceIdxs[(kn+1)%fsize+fbase]];
                  if (mcomp.isSurfaceEdge (node, n)) {
                      nodes.add (n);
                  }
               }
               else {
                  // quad face, not checking for fill: add all adjacent faces;
                  // no need to check edges
                  for (int j=fbase; j<fbase+fsize; j++) {
                     if (j != jn) {
                        nodes.add (enodes[faceIdxs[j]]);
                     }
                  }
               }
            }
            else {
               // for quad element faces, add only some nodes
               if (kn % 2 == 0) {
                  // corner node. Add immediately adjacent nodes.
                  kn += fsize; // boost kn so that kn%fsize works properly
                  FemNode3d n;
                  n = enodes[faceIdxs[(kn-1)%fsize+fbase]];
                  if (mcomp.isSurfaceEdge (node, n)) {
                      nodes.add (n);
                  }
                  n = enodes[faceIdxs[(kn+1)%fsize+fbase]];
                  if (mcomp.isSurfaceEdge (node, n)) {
                      nodes.add (n);
                  }
               }
               else {
                  // edge node. Add next *two* adjacent nodes.
                  kn += fsize; // boost kn so that kn%fsize works properly
                  for (int k = kn-2; k <= kn+2; k++) {
                     if (k != kn) {
                        FemNode3d n = enodes[faceIdxs[k%fsize+fbase]];
                        if (mcomp.isSurfaceEdge (node, n)) {
                           nodes.add (n);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void collectAdjacentNodes (
      LinkedHashSet<FemNode3d> nodes, FemElement3dBase elem, 
      FemNode3d node, boolean forFill) {
               
      FemNode3d[] enodes = elem.getNodes();
      if (elem.numNodes() < 10) {
         // non-quadratic element
         for (FemNode3d n : enodes) {
            if (n != node) {
               nodes.add (n);
            }
         }
      }
      else {
         // find node index within the element
         int nodeIdx = elem.getNodeIndex(node);
         if (nodeIdx == -1) {
            throw new InternalErrorException (
               "Node number " + node.getNumber() +
               " not found in adjacent element");
         }
         // Examine all element faces and choose only faces which contain the
         // node.

         // faceIdxs gives local node indices for all faces in a single array,
         // which also contains the sizes for each face
         int[] faceIdxs = elem.getFaceIndices();
         int fsize = 0; // number of nodes in the face
         int fbase = 0; // start index of the face's local node indices
         for (int i=0; i<faceIdxs.length; i+=(fsize+1)) {
            fsize = faceIdxs[i];
            fbase = i+1;
            int jn = -1;
            for (int j=fbase; j<fbase+fsize; j++) {
               if (faceIdxs[j] == nodeIdx) {
                  jn = j;
                  break;
               }
            }
            if (jn != -1) {
               // face contains the node. Face must also belong
               // to a quad element, and so we add only closer nodes.
               int kn = jn-fbase; // index of the node within the face
               if (kn % 2 == 0) {
                  // corner node. Add immediately adjacent nodes.
                  kn += fsize; // boost kn so that kn%fsize works properly
                  nodes.add (enodes[faceIdxs[(kn-1)%fsize+fbase]]);
                  nodes.add (enodes[faceIdxs[(kn+1)%fsize+fbase]]);
               }
               else {
                  // edge node. Add next *two* adjacent nodes.
                  kn += fsize; // boost kn so that kn%fsize works properly
                  for (int k = kn-2; k <= kn+2; k++) {
                     if (k != kn) {
                        nodes.add (enodes[faceIdxs[k%fsize+fbase]]);
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * Finds all the nodes of an FEM model that lie on the boundary of an
    * existing node set {@code nodes}, where the boundary is defined in the
    * documentation for {@link #findBoundaryNodes(Collection,FemMeshComp)}.  If
    * {@code surfaceOnly} is {@code true}, the search is restricted to nodes
    * which lie on the FEM surface mesh and are coincident with mesh vertices.
    *
    * @param fem FEM model for which the query is being made
    * @param nodes initial node set. The method will run faster if this is an
    * actual set.
    * @param surfaceOnly if {@code true}, restricts the search to nodes
    * on the FEM surface
    * @return nodes on the boundary of the set defined by {@code nodes}
    */
   public static ArrayList<FemNode3d> findBoundaryNodes (
      FemModel3d fem, Collection<FemNode3d> nodes, boolean surfaceOnly) {
      FemMeshComp surfc = surfaceOnly ? fem.getSurfaceMeshComp() : null;
      return createNodeArray (findBoundaryNodes (nodes, surfc));
   }

   /**
    * Finds all the nodes of an FEM model that lie on the boundary of an
    * existing node set {@code nodes}. If the mesh component {@code mcomp} is
    * non-{@code null}, the search is restricted to nodes which lie on the mesh
    * surface and are coincident with mesh vertices.
    *
    * <p>A node is on the boundary if it is <i>adjacent</i> to at least one
    * other node that is not in {@code nodes}, where adjacency is defined in
    * the documentation for {@link #getAdjacentNodes} with {@code forFill}
    * set to {@code false}.
    *
    * @param nodes initial node set. The method will run faster if this is an
    * actual set.
    * @param mcomp if non-{@code null}, restricts the search to nodes
    * coincident with mesh vertices
    * @return nodes on the boundary of the set defined by {@code nodes}
    */
   public static HashSet<FemNode3d> findBoundaryNodes (
      Collection<FemNode3d> nodes, FemMeshComp mcomp) {
      LinkedHashSet<FemNode3d> bounding = new LinkedHashSet<>();
      HashSet<FemNode3d> nodeSet = getHashSet(nodes);
      for (FemNode3d node : nodeSet) {
         ArrayList<FemNode3d> adjacent =
            getAdjacentNodes(node,mcomp, /*forFill=*/false);
         if (adjacent.size() > 0) {
            for (FemNode3d n : adjacent) {
               if (!nodeSet.contains(n)) {
                  bounding.add (node);
                  break;
               }
            }
         }
         else {
            bounding.add (node);
         }
      }
      return bounding;
   }

   /**
    * Finds all the nodes of an FEM model that lie inside the boundary of an
    * existing node set {@code nodes}, where the boundary is defined in the
    * documentation for {@link #findEnclosedNodes(Collection,FemMeshComp)}. If
    * {@code surfaceOnly} is {@code true}, the search is restricted to nodes
    * which lie on the FEM surface mesh and are coincident with mesh vertices.
    *
    * @param fem FEM model for which the query is being made
    * @param nodes initial node set. The method will run faster if this is an
    * actual set.
    * @param surfaceOnly if {@code true}, restricts the search to nodes
    * on the FEM surface
    * @return nodes inside the boundary of the set defined by {@code nodes}
    */
   public static ArrayList<FemNode3d> findEnclosedNodes (
      FemModel3d fem, Collection<FemNode3d> nodes, boolean surfaceOnly) {
      FemMeshComp surfc = surfaceOnly ? fem.getSurfaceMeshComp() : null;
      return createNodeArray (findEnclosedNodes (nodes, surfc));
   }

   /**
    * Finds all the nodes of an FEM model that lie inside the boundary of an
    * existing node set {@code nodes}. If the mesh component {@code mcomp} is
    * non-{@code null}, the search is restricted to nodes which lie on the mesh
    * surface and are coincident with mesh vertices.
    *
    * <p>A node is inside the boundary if every <i>adjacent</i> node is
    * contained in {@code nodes}, where adjacency is defined in the
    * documentation for {@link #getAdjacentNodes} with {@code forFill}
    * set to {@code false}.
    *
    * @param nodes initial node set. The method will run faster if this is an
    * actual set.
    * @param mcomp if non-{@code null}, restricts the search to nodes
    * coincident with mesh vertices
    * @return nodes inside the boundary of the set defined by {@code nodes}
    */
   public static HashSet<FemNode3d> findEnclosedNodes (
      Collection<FemNode3d> nodes, FemMeshComp mcomp) {
      HashSet<FemNode3d> enclosed = new LinkedHashSet<>();
      HashSet<FemNode3d> nodeSet = getHashSet (nodes);
      for (FemNode3d node : nodeSet) {
         ArrayList<FemNode3d> adjacent =
            getAdjacentNodes(node,mcomp, /*forFill=*/false);
         if (adjacent.size() > 0) {
            boolean onBoundary = false;               
            for (FemNode3d n : adjacent) {
               if (!nodeSet.contains(n)) {
                  onBoundary = true;
                  break;
               }
            }
            if (!onBoundary) {
               enclosed.add (node);
            }
         }
      }
      return enclosed;
   }

   /**
    * Finds all the nodes of an FEM model that are adjacent to an existing node
    * set {@code nodes}, where adjacency is defined in the documentation for
    * {@link #getAdjacentNodes} with {@code forFill} set to {@code false}. If
    * {@code surfaceOnly} is {@code true}, the search is restricted to nodes
    * which lie on the FEM surface mesh and are coincident with mesh vertices.
    *
    * @param fem FEM model for which the query is being made
    * @param nodes initial node set. The method will run faster if this is an
    * actual set.
    * @param surfaceOnly if {@code true}, restricts the search to nodes
    * on the FEM surface
    * @return all nodes adjacent to (but not contained in) {@code nodes}
    */
   public static ArrayList<FemNode3d> findAdjacentNodes (
      FemModel3d fem, Collection<FemNode3d> nodes, boolean surfaceOnly) {
      FemMeshComp surfc = surfaceOnly ? fem.getSurfaceMeshComp() : null;
      return createNodeArray (findAdjacentNodes (nodes, surfc));
   }

   /**
    * Finds all the nodes of an FEM model that are adjacent to an existing node
    * set {@code nodes}, where adjacency is defined in the documentation for
    * {@link #getAdjacentNodes} with {@code forFill} set to {@code false}. If
    * the mesh component {@code mcomp} is non-{@code null}, the search is
    * restricted to nodes which lie on the mesh surface and are coincident with
    * mesh vertices.
    *
    * @param nodes initial node set. The method will run faster if this is an
    * actual set.
    * @param mcomp if non-{@code null}, restricts the search to nodes
    * coincident with mesh vertices
    * @return all nodes adjacent to (but not contained in) {@code nodes}
    */
   public static HashSet<FemNode3d> findAdjacentNodes (
      Collection<FemNode3d> nodes, FemMeshComp mcomp) {
      LinkedHashSet<FemNode3d> adjacent = new LinkedHashSet<>();
      HashSet<FemNode3d> nodeSet = getHashSet (nodes);
      for (FemNode3d node : nodeSet) {
         for (FemNode3d n : getAdjacentNodes(node,mcomp, /*forFill=*/false)) {
            if (!nodeSet.contains(n)) {
               adjacent.add (n);
            }
         }
      }
      return adjacent;
   }

   /**
    * Keeps node information for doing a Dijkstra search of the shortest
    * surface path between two nodes.
    */
   private static class PathNode {
      FemNode3d myNode;
      double myDist;      
      PathNode myPrev;

      PathNode (FemNode3d node, double dist, PathNode prev) {
         myNode = node;
         myDist = dist;
         myPrev = prev;
      }
   }

   private static class PathNodeCompare implements Comparator<PathNode> {
      @Override
      public int compare(PathNode n0, PathNode n1) {
         if (n0.myDist < n1.myDist) {
            return -1;
         }
         else if (n0.myDist > n1.myDist) {
            return 1;
         }
         else {
            return 0;
         }
      }
   }

   /**
    * Finds all the nodes of an FEM model that are required to fill a region
    * about a starting node {@code node0}. The fill proceeds as described
    * in the documentation for {@link
    * #fillNodeRegion(Collection,FemNode3d,FemMeshComp)}. If
    * {@code surfaceOnly} is {@code true}, the fill is restricted to nodes
    * which lie on the FEM surface mesh and are coincident with mesh vertices.
    *
    * @param fem FEM model for which the query is being made    
    * @param region initial node region. The method will run faster if this is
    * an actual set.
    * @param node0 starting node inside the region
    * @param surfaceOnly if {@code true}, restricts the search to nodes
    * on the FEM surface
    * @return nodes not already contained in {@code region} that are needed to
    * perform the fill
    */
   public static ArrayList<FemNode3d> fillNodeRegion (
      FemModel3d fem, Collection<FemNode3d> region,
      FemNode3d node0, boolean surfaceOnly) {
      FemMeshComp surfc = surfaceOnly ? fem.getSurfaceMeshComp() : null;
      return createNodeArray (fillNodeRegion (region, node0, surfc));
   }


   /**
    * Finds all the nodes of an FEM model that are required to fill a region
    * about a starting node {@code node0}. The fill proceeds by adding adjacent
    * nodes that are not currently contained in {@code region}, and terminates
    * when at the region's boundary. If the region does not have a proper
    * boundary, the fill will expand to include all nodes in the FEM model.  If
    * the mesh component {@code mcomp} is non-{@code null}, the search is
    * restricted to nodes which lie on the mesh surface and are coincident with
    * mesh vertices.
    *
    * <p>For fill purposes, adjacency is defined in the documentation for
    * {@link #getAdjacentNodes} with {@code forFill} set to {@code true}.
    *
    * @param region initial node region. The method will run faster if this is
    * an actual set.
    * @param node0 starting node inside the region
    * @param mcomp if non-{@code null}, restricts the search to nodes
    * coincident with mesh vertices
    * @return nodes not already contained in {@code region} that are needed to
    * perform the fill
    */
   public static HashSet<FemNode3d> fillNodeRegion (
      Collection<FemNode3d> region, FemNode3d node0, FemMeshComp mcomp) {
      HashSet<FemNode3d> regionSet = getHashSet (region);
      LinkedHashSet<FemNode3d> fillNodes = new LinkedHashSet<>();
      LinkedList<FemNode3d> queue = new LinkedList<>();
      queue.add (node0);
      while (queue.size() > 0) {
         FemNode3d node = queue.poll();
         for (FemNode3d n : getAdjacentNodes(node, mcomp, /*forFill=*/true)) {
            if (!regionSet.contains(n) && !fillNodes.contains(n)) {
               fillNodes.add (n);
               queue.offer (n);
            }
         }
      }
      return fillNodes;
   }

   /**
    * Finds a minimum distance path of nodes within an FEM model between nodes
    * {@code nodeA} and {@code nodeB}, as described in the documentation for
    * {@link #findNodePath(FemNode3d,FemNode3d,FemMeshComp)}. If {@code
    * surfaceOnly} is {@code true}, the search is restricted to nodes which lie
    * on the FEM surface mesh and are coincident with mesh vertices.
    *
    * @param fem FEM model for which the query is being made
    * @param nodeA path start node
    * @param nodeB path end node
    * @param surfaceOnly if {@code true}, restricts the search to nodes
    * on the FEM surface
    * @return nodes between {@code node0} and {@code node1} that comprise the
    * path
    */
   public static ArrayList<FemNode3d> findNodePath (
      FemModel3d fem, FemNode3d nodeA, FemNode3d nodeB, boolean surfaceOnly) {
      FemMeshComp surfc = surfaceOnly ? fem.getSurfaceMeshComp() : null;
      return findNodePath (nodeA, nodeB, surfc);
   }


   /**
    * Finds a minimum distance path of nodes within an FEM model between nodes
    * {@code nodeA} and {@code nodeB}. The path is computed using a graph
    * search based on the adjacency relationships between nodes, as defined in
    * the documentation for {@link #getAdjacentNodes} with {@code forFill} set
    * to {@code false}, and so may not necessarily yield a path that is
    * strictly minimal with respect to Euclidean distance. If the mesh
    * component {@code mcomp} is non-{@code null}, the search is restricted to
    * nodes which lie on the mesh surface and are coincident with mesh
    * vertices.
    *
    * @param nodeA path start node
    * @param nodeB path end node
    * @param mcomp if non-{@code null}, restricts the search to nodes
    * coincident with mesh vertices
    * @return nodes between {@code node0} and {@code node1} that comprise the
    * path
    */
   public static ArrayList<FemNode3d> findNodePath (
      FemNode3d nodeA, FemNode3d nodeB, FemMeshComp mcomp) {

      // Implement using Dijkstra's algorithm.

      ArrayList<FemNode3d> path = new ArrayList<>();
      if (nodeA == nodeB) {
         // trivial case
         return path;
      }
      if (mcomp != null && 
          (mcomp.getVertexForNode(nodeA) == null ||
           mcomp.getVertexForNode(nodeA) == null)) {
         // infeasible case
         return path;
      }

      HashMap<FemNode3d,Double> distances = new HashMap<>();
      HashSet<FemNode3d> visited = new HashSet<>();
      distances.put (nodeA, 0.0);

      PriorityQueue<PathNode> queue =
         new PriorityQueue<PathNode>(new PathNodeCompare());
      queue.add (new PathNode(nodeA, 0, null));
      PathNode pnode0 = null;
      while (queue.size() > 0) {
         pnode0 = queue.poll();
         FemNode3d node0 = pnode0.myNode;
         if (node0 == nodeB) {
            break;
         }
         visited.add (node0);
         ArrayList<FemNode3d> adjacent = 
            getAdjacentNodes(node0, mcomp, /*forFill=*/false);
         for (FemNode3d node1 : adjacent) {
            if (!visited.contains(node1)) {
               double newd = pnode0.myDist + node0.distance (node1);
               Double dist = distances.get(node1);
               if (dist == null || dist > newd) {
                  distances.put (node1, newd);
                  queue.add (new PathNode (node1, newd, pnode0));
               }
            }
         }
      }
      // trace path back from node
      PathNode prev = pnode0.myPrev;
      while (prev.myNode != nodeA) {
         path.add (prev.myNode);
         prev = prev.myPrev;
      }
      Collections.reverse(path);
      return path;
   }
}
