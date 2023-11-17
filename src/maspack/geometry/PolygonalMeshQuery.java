/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;

import maspack.matrix.Vector3d;

/**
 * Provides various query methods for a PolygonalMesh.
 */
public class PolygonalMeshQuery {

   /**
    * Find all the vertices that lie along an <i>edge line</i>, starting at
    * {@code vtx0}. Edges along the edge line must have a <i>bend angle</i>
    * that is {@code >= minBendAngle}, where the bend angle is the absolute
    * value of the angle between an edge's adjacent faces. In addition, the
    * <i>edge angle</i> between adjacent edges must be {@code <=
    * maxEdgeAngle}. Branching is allowed if {@code allowBranching} is {@code
    * true}; otherwise, edges are followed so as to minimize the edge angle.
    *
    * <p>The returned vertex set will always include {@code vtx0}.
    *
    * @param vtx0 starting vertex
    * @param minBendAngle minimum bend angle between adjacent faces (radians)
    * @param maxEdgeAngle maximum edge angle between adjacent edges (radians)
    * @param allowBranching if {@code true}, allow branching
    * @return a set of the vertices on the line
    */
   public static HashSet<Vertex3d> findEdgeLineVertices (
      Vertex3d vtx0, 
      double minBendAngle, double maxEdgeAngle, boolean allowBranching) {
      
      double maxBendCos = Math.cos (minBendAngle);
      double minEdgeCos = Math.cos (maxEdgeAngle);
      LinkedHashSet<Vertex3d> vertices = new LinkedHashSet<>();

      vertices.add (vtx0);
      if (allowBranching) {
         recursivelyFindAllEdgeLineVertices (
            vertices, vtx0, null, maxBendCos, minEdgeCos);
      }
      else {
         followEdgeLineVertices (
            vertices, vtx0, null, maxBendCos, minEdgeCos);
         followEdgeLineVertices (
            vertices, vtx0, null, maxBendCos, minEdgeCos);
      }
      return vertices;
   }

   static void recursivelyFindAllEdgeLineVertices (
      HashSet<Vertex3d> vertices, Vertex3d vtx, Vector3d prevDir, 
      double maxBendCos, double minEdgeCos) {
      
      Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
      while (it.hasNext()) {
         HalfEdge he = it.next();
         Face opface = he.getOppositeFace();
         Vertex3d tail = he.getTail();
         if (opface != null && !vertices.contains(tail)) {
            double cos = he.getFace().getNormal().dot(opface.getNormal());
            if (cos > maxBendCos) {
               continue;
            }
            Vector3d dir = new Vector3d();
            he.computeUnitVec (dir);
            dir.negate();
            if (prevDir != null && dir.dot(prevDir) < minEdgeCos) {
               continue;
            }
            vertices.add (tail);
            recursivelyFindAllEdgeLineVertices (
               vertices, tail, dir, maxBendCos, minEdgeCos);
         }
      }
   }      

   static void followEdgeLineVertices (
      HashSet<Vertex3d> vertices, Vertex3d vtx, Vector3d prevDir, 
      double maxBendCos, double minEdgeCos) {
      
      Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
      
      // information on best vertex to follow, if any
      Vertex3d bestNextVertex = null;
      Vector3d bestNextDir = null;
      double bestCos = 0;

      // search all adjacent edges to find best vertex to follow
      while (it.hasNext()) {
         HalfEdge he = it.next();
         Face opface = he.getOppositeFace();

         Vertex3d tail = he.getTail();
         if (opface != null && !vertices.contains(tail)) {
            double bendCos = he.getFace().getNormal().dot(opface.getNormal());
            if (bendCos > maxBendCos) {
               // don't follow - bend is not sharp enough
               continue;
            }
            Vector3d dir = new Vector3d();
            he.computeUnitVec (dir);
            dir.negate();
            double edgeCos = 0;
            if (prevDir != null && (edgeCos=dir.dot(prevDir)) < minEdgeCos) {
               // don't follow - angle with prev edge is too large
               continue;
            }
            // edge is ok to follow. Check if it is the best
            if (bestNextVertex == null ||
                (prevDir == null && bendCos < bestCos) ||
                (prevDir != null && edgeCos > bestCos)) {
               bestNextVertex = tail;
               bestNextDir = dir;
               bestCos = (prevDir == null ? bendCos : edgeCos);
            }
         }
      }
      if (bestNextVertex != null) {
         vertices.add (bestNextVertex);
         followEdgeLineVertices (
            vertices, bestNextVertex, bestNextDir, maxBendCos, minEdgeCos);
      }
   }      

   /**
    * Find all the vertices in a <i>patch</i> containing {@code vtx0}.  The
    * patch is the collection of all faces surrounding {@code vtx0} for which
    * the <i>bend angle</i> between them is {@code <= maxBendAngle}, where the
    * bend angle is the absolute value of the angle between two faces about
    * their common edge.
    *
    * <p>The returned vertex set will always include {@code vtx0}.
    * 
    * @param vtx0 starting vertex within the patch
    * @param maxBendAngle maximum bend angle between adjacent faces (radians)
    * @return a set of the vertices in the patch
    */
   public static HashSet<Vertex3d> findPatchVertices (
      Vertex3d vtx0, double maxBendAngle) {
      LinkedHashSet<Vertex3d> vertices = new LinkedHashSet<>();

      vertices.add (vtx0);
      HalfEdge he0 = vtx0.firstIncidentHalfEdge();
      if (he0 != null) {
         HashSet<Face> faces = findPatchFaces (he0.getFace(), maxBendAngle);
         for (Face face : faces) {
            he0 = face.firstHalfEdge();
            HalfEdge he = he0;
            do {
               vertices.add (he.getHead());
               he = he.getNext();
            }
            while (he != he0);
         }
      }
      return vertices;     
   }

   /**
    * Find all the vertices that are on the <i>boundary</i> of a given set of
    * vertices. A vertex is on the boundary if it is connected by an edge to at
    * least one other vertex that is not in the set.
    *
    * @param vertices original set of vertices
    * @return set of vertices describing the boundary of {@code vertices}
    */
   public static HashSet<Vertex3d> findBoundaryVertices (
      HashSet<Vertex3d> vertices) {
      LinkedHashSet<Vertex3d> outer = new LinkedHashSet<>();
      for (Vertex3d vtx : vertices) {
         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         while (it.hasNext()) {
            HalfEdge he = it.next();
            if (!vertices.contains(he.getTail())) {
               outer.add (vtx);
            }
         }
      }
      return outer;
   }

   /**
    * Find all the vertices that are in the <i>interior</i> of a given set of
    * vertices. A vertex is in the interior if every vertex to which it is
    * connected by an edge is in the set.
    *
    * @param vertices original set of vertices
    * @return set of vertices describing the interior of {@code vertices}
    */
   public static HashSet<Vertex3d> findInteriorVertices (
      HashSet<Vertex3d> vertices) {
      HashSet<Vertex3d> inner = new LinkedHashSet<>();
      for (Vertex3d vtx : vertices) {
         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         boolean outer = false;
         while (it.hasNext()) {
            HalfEdge he = it.next();
            if (!vertices.contains(he.getTail())) {
               outer = true;
               break;
            }
         }
         if (!outer) {
            inner.add (vtx);
         }
      }
      return inner;
   }

   /**
    * Find all the faces in a <i>patch</i> containing {@code face0}.  The patch
    * is the collection of all faces surrounding {@code face0} for which the
    * <i>bend angle</i> between them is {@code <= maxBendAngle}, where the bend
    * angle is the absolute value of the angle between two faces about their
    * common edge.
    *
    * <p>The returned face set will always include {@code face0}.
    * 
    * @param face0 starting face within the patch
    * @param maxBendAngle maximum bend angle between adjacent faces (radians)
    * @return a set of the faces in the patch
    */
   public static HashSet<Face> findPatchFaces (
      Face face0, double maxBendAngle) {
      double minBendCos = Math.cos (maxBendAngle);
      LinkedHashSet<Face> faces = new LinkedHashSet<>();
      faces.add (face0);
      recursivelyFindPatchFaces (faces, face0, minBendCos);
      return faces;
   }

   static void recursivelyFindPatchFaces (
      HashSet<Face> faces, Face face, double minBendCos) {

      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         Face opface = he.getOppositeFace();
         if (opface != null && !faces.contains(opface)) {
            if (he.getFace().getNormal().dot(opface.getNormal()) >= minBendCos) {
               faces.add (opface);
               recursivelyFindPatchFaces (faces, opface, minBendCos);
            }
         }
         he = he.getNext();
      }
      while (he != he0);
   }      

   static class PathCostCompare implements Comparator<Vertex3d> {
      double[] myDistances;
      
      PathCostCompare (double[] distances) {
         myDistances = distances;
      }

      @Override
      public int compare(Vertex3d vtx0, Vertex3d vtx1) {
         double d0 = myDistances[vtx0.getIndex()];
         double d1 = myDistances[vtx1.getIndex()];

         return (d0 < d1 ? -1 : (d0 > d1 ? 1 : 0));
      }
   }

   public static ArrayList<Vertex3d> findShortestPath (
      Vertex3d vtxA, Vertex3d vtxB, int numv) {

      ArrayList<Vertex3d> path = new ArrayList<>();
      if (vtxA == vtxB) {
         // trivial case
         return path;
      }

      double[] distances = new double[numv];
      boolean[] visited = new boolean[numv];
      Vertex3d[] previous = new Vertex3d[numv];
      for (int i=0; i<numv; i++) {
         distances[i] = Double.MAX_VALUE;
      }
      distances[vtxA.getIndex()] = 0;

      PathCostCompare comparator = new PathCostCompare(distances);
      PriorityQueue<Vertex3d> queue =
         new PriorityQueue<Vertex3d>(numv, comparator);
      queue.add (vtxA);
      while (queue.size() > 0) {
         Vertex3d vtx0 = queue.poll();
         if (vtx0 == vtxB) {
            break;
         }
         visited[vtx0.getIndex()] = true;
         Iterator<HalfEdge> it = vtx0.getIncidentHalfEdges();
         while (it.hasNext()) {
            HalfEdge he = it.next();
            Vertex3d vtx1 = he.getTail();
            if (!visited[vtx1.getIndex()]) {
               double newd =
                  distances[vtx0.getIndex()] + vtx0.distance (vtx1);
               if (distances[vtx1.getIndex()] > newd) {
                  distances[vtx1.getIndex()] = newd;
                  queue.add (vtx1);
                  previous[vtx1.getIndex()] = vtx0;
               }
            }
         }
      }
      // trace path back from vtx
      Vertex3d prev = previous[vtxB.getIndex()];
      while (prev != vtxA) {
         path.add (prev);
         prev = previous[prev.getIndex()];
      }
      Collections.reverse(path);
      return path;
   }
}
