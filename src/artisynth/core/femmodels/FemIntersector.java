/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import maspack.geometry.AABBTree;
import maspack.geometry.BVNode;
import maspack.geometry.Boundable;
import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TriangleIntersector;
import maspack.geometry.Vertex3d;
import maspack.graph.DirectedEdge;
import maspack.graph.DirectedGraph;
import maspack.graph.Vertex;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

/**
 * Intersects the volumetric elements of a finite element model with a plane
 */
public class FemIntersector {

   private static double defaultTolerance = 1e-10;
   private double epsilon = defaultTolerance;
   
   public FemIntersector() {
      this(defaultTolerance);
   }
   
   public FemIntersector (double tol) {
      setTolerance(tol);
   }
   
   public void setTolerance(double tol) {
      epsilon = tol;
   }
   
   public double getTolerance() {
      return epsilon;
   }
   
   /**
    * Intersects the volumetric elements of an FEM model with a plane,
    * returning a Polygonal mesh on the plane corresponding to the element
    * intersections.
    *
    * @param fem model to intersect with the plane
    * @param plane plane to intersect with
    * @return intersection mesh
    */
   public PolygonalMesh intersectPlane(FemModel3d fem, Plane plane) {
      
      AABBTree aabb = new AABBTree();
      FemElement3d[] elements = fem.getElements().toArray(
         new FemElement3d[fem.numElements()]);
      aabb.build (elements, fem.numElements());

      ArrayList<BVNode> nodes = new ArrayList<BVNode>();
      aabb.intersectPlane(nodes, plane);

      DirectedGraph<Point3d,Vector3d> nodeGraph =
         new DirectedGraph<Point3d,Vector3d>();

      TriangleIntersector ti = new TriangleIntersector();
      ti.setEpsilon(epsilon);

      for (BVNode node : nodes) {
         Boundable[] elems = node.getElements();
         for (int i=0; i<node.getNumElements(); i++){
            FemElement3d elem = (FemElement3d)elems[i];
            FaceNodes3d[] faceNodes = elem.getFaces();
            for (FaceNodes3d fn : faceNodes) {
               FemNode3d[][] faces = fn.triangulate();

               for (FemNode3d[] face : faces) {
                  addIfUnique(
                     ti.intersectTrianglePlane(
                        face[0].getPosition(), 
                        face[1].getPosition(), 
                        face[2].getPosition(), plane),
                        nodeGraph, epsilon);
               } // end loop through faces
            } // end loop through "face nodes"
         } // end looping through elements
      } // end looping through BVNodes
     
      //reduceGraph(nodeGraph, tol);
      fixOverlaps(nodeGraph, epsilon);
      
      PolygonalMesh mesh = buildMesh( nodeGraph, plane.normal);
      removeBackFaces(mesh, plane.normal);
      nonConvexTriangulate(mesh, plane.normal, epsilon);
      
      return mesh;
      
   }
   
   private void removeBackFaces(PolygonalMesh mesh, Vector3d normal) {
      ArrayList<Face> remove = new ArrayList<Face>();
      
      Vector3d v1 = new Vector3d();
      Vector3d v2 = new Vector3d();
      
      for (Face face : mesh.getFaces())  {
         // deem a face to be on the back if sum of convex angles outweights concave ones
         double a = 0;
         for (int i=0; i<face.numVertices(); i++) {
            int b = (i+face.numVertices()-1)%(face.numVertices());
            int f = (i+1)%face.numVertices();
            v1.sub(face.getVertex(i).getWorldPoint(), face.getVertex(b).getWorldPoint());
            v2.sub(face.getVertex(f).getWorldPoint(), face.getVertex(i).getWorldPoint());
            a += getAngle(1, v1, 1, v2, normal);
         }
         
         if (a > 0) {
            remove.add(face);
         }
      }
      
      for (Face face : remove) {
         mesh.removeFace(face);
      }
      
   }
   
   

   private void addIfUnique(List<Point3d> addThese, DirectedGraph<Point3d,Vector3d> graph, double tol) {

      // add nodes
      if (addThese.size() >= 2) {
         addEdge(addThese.get(0), addThese.get(1), graph, tol);
      }
      
      if (addThese.size() == 3) {
         addEdge(addThese.get(1), addThese.get(2), graph, tol);
         addEdge(addThese.get(0), addThese.get(2), graph, tol);
      }

   }
   
   private DirectedEdge<Point3d,Vector3d> connectIfUnique(DirectedGraph<Point3d,Vector3d> graph, Vertex<Point3d,Vector3d> vtx1, 
      Vertex<Point3d,Vector3d> vtx2, Vector3d disp, double cost) {
      
      DirectedEdge<Point3d,Vector3d> edge = graph.findEdge(vtx1, vtx2);
      if (edge == null) {
         edge = new DirectedEdge<Point3d,Vector3d>(vtx1, vtx2, disp, cost);
         graph.addEdge(edge);
      }
      return edge;
      
   }
   
   private void addEdge(Point3d p1, Point3d p2, DirectedGraph<Point3d,Vector3d> graph, double tol) {
      
      Vertex<Point3d,Vector3d> v1 = getVertex(p1, graph, tol);
      Vertex<Point3d,Vector3d> v2 = getVertex(p2, graph, tol);
      DirectedEdge<Point3d,Vector3d> edge = graph.findEdge(v1, v2);
 
      if (edge == null) {
         Vector3d disp = new Vector3d(v2.getData());
         disp.sub(v1.getData());
         disp.normalize();
         connectIfUnique(graph, v1, v2, disp, 1);
         connectIfUnique(graph, v2, v1, disp, -1);
      }
      
   }
   
   private Vertex<Point3d,Vector3d> getVertex(Point3d p, DirectedGraph<Point3d,Vector3d> graph, double tol) {
      
      for ( Vertex<Point3d,Vector3d> v : graph.getVertices()) {
         if (v.getData().distance(p)< tol) {
            return v;
         }
      }
      
      return new Vertex<Point3d,Vector3d>(p);
   }
   
   private PolygonalMesh buildMesh(DirectedGraph<Point3d,Vector3d> graph, Vector3d normal) {
      
      PolygonalMesh mesh = new PolygonalMesh();
      LinkedList<DirectedEdge<Point3d,Vector3d>> remainingEdges = 
         new LinkedList<DirectedEdge<Point3d,Vector3d>>(graph.getEdges());
      
      HashMap<Point3d,Vertex3d> vtxMap = new HashMap<Point3d,Vertex3d>(graph.numVertices());
      for (Vertex<Point3d,Vector3d> v : graph.getVertices()) {
         Vertex3d vtx = mesh.addVertex(v.getData());
         vtxMap.put(v.getData(), vtx);
      }
      
      while (remainingEdges.size() > 0) {
         
         DirectedEdge<Point3d,Vector3d> e = remainingEdges.get(0);
         
         ArrayList<DirectedEdge<Point3d,Vector3d>> face = findFace(e, graph, normal);
         
         if (face == null) {
            remainingEdges.remove(0);
         } else {
            Vertex3d[] vtxs = new Vertex3d[face.size()];
            int idx = 0;
            for (DirectedEdge<Point3d,Vector3d> edge : face) {
               vtxs[idx++] = vtxMap.get(edge.getVertex(0).getData());
               remainingEdges.remove(edge);
            }
            mesh.addFace(vtxs);
            
         }
         
      }
      
      return mesh;
   }
   
   // fix graph so that there are no overlapping edges
   private void fixOverlaps(DirectedGraph<Point3d,Vector3d> graph, double tol) {
      
      ArrayList<DirectedEdge<Point3d,Vector3d>> edges = 
         new ArrayList<DirectedEdge<Point3d,Vector3d>>(graph.getEdges());
      
      int idx = 0;
      while (idx < edges.size()) {
         DirectedEdge<Point3d,Vector3d> edge = edges.get(idx); 
         
         Vertex<Point3d,Vector3d> vtx1 = edge.getVertex(0);
         Vertex<Point3d,Vector3d> vtx2 = edge.getVertex(1);
         Point3d a = vtx1.getData();
         Point3d b = vtx2.getData();
         for (Vertex<Point3d,Vector3d> vtx : graph.getVertices()) {
            
            if (vtx != vtx1 && vtx != vtx2) {
               double d = lineSegmentDistance(a, b, vtx.getData());
               
               if (d < tol) {
                  graph.removeEdge(edge);
                  
                  DirectedEdge<Point3d,Vector3d> newEdge = connectIfUnique(graph,
                     vtx1, vtx, 
                     edge.getData(), edge.getCost());
                  if (!edges.contains(newEdge)) {
                     edges.add(newEdge);   
                  }
                  
                  newEdge = connectIfUnique(graph,
                        vtx, vtx2, 
                        edge.getData(), edge.getCost());
                  if (!edges.contains(newEdge)) {
                     edges.add(newEdge);   
                  }
                  break;
               }
            }
         }
         
         idx++;
      }
      
   }
   
   private double lineSegmentDistance(Point3d p1, Point3d p2, Point3d p) {
      Vector3d v = new Vector3d(p2.x-p1.x,p2.y-p1.y,p2.z-p1.z);
      Vector3d w = new Vector3d(p.x-p1.x,p.y-p1.y,p.z-p1.z);
      
      double c1 = v.dot(w);
      double c2 = v.dot(v);
      
      if (c1 <= 0) {
         return p.distance(p1);
      }
      if (c2 <= c1) {
         return p.distance(p2);
      }
      
      double b = c1 / c2;
      w.scaledAdd(b, v, p1);
      return p.distance(w);
   }
   
   private ArrayList<DirectedEdge<Point3d,Vector3d>>  findFace(DirectedEdge<Point3d,Vector3d> start,
      DirectedGraph<Point3d,Vector3d> graph, Vector3d normal) {
      
      ArrayList<DirectedEdge<Point3d,Vector3d>> edges = new ArrayList<DirectedEdge<Point3d,Vector3d>>();
      edges.add(start);

      Vertex<Point3d,Vector3d> finalPos = start.getVertex(0);
      Vertex<Point3d,Vector3d> prevPos = finalPos;
      Vertex<Point3d,Vector3d> pos = start.getVertex(1);
      
      DirectedEdge<Point3d,Vector3d> prevEdge = start;
      ArrayList<Vertex<Point3d,Vector3d>> usedPos = 
         new ArrayList<Vertex<Point3d,Vector3d>>();
      usedPos.add(start.getVertex(0));
      usedPos.add(start.getVertex(1));
      
      while(pos != finalPos) {
         
         double minAngle = Double.MAX_VALUE;
         DirectedEdge<Point3d,Vector3d> minEdge = null;
         
         for (DirectedEdge<Point3d,Vector3d> e : pos.getForwardEdges()) {
            if (e.getVertex(1) != prevPos) { // don't use opposite edge
               double ang = getAngle(prevEdge.getCost(), prevEdge.getData(), 
                  e.getCost(), e.getData(), normal);
               if (ang < minAngle) {
                  minEdge = e;
                  minAngle = ang;
               }
            }
         }
         
         if (minEdge == null) {
            return null;
         }
         
         edges.add(minEdge);
         prevEdge = minEdge;
         prevPos = pos;
         pos = minEdge.getVertex(1);
         if (pos != start.getVertex(0) && usedPos.contains(pos)) {
            System.out.println("Point already on curve?!?!");
            //return findFace(start, graph, normal, 10);
            return null;
         } {
            usedPos.add(pos);
         }
      }
      
      if (pos == finalPos) {
         return edges;
      }
      
      return null;
      
   }
   
   // dot product s*v1.dot(v2)+k, with 
   private static double getAngle(double s1, Vector3d v1, double s2, Vector3d v2, Vector3d n) {
      Vector3d c = new Vector3d(v1);
      c.cross(v2);
      
      double cost = s1*s2*v1.dot(v2);
      double t = Math.atan2(c.norm(),cost);
      if (s1*s2*n.dot(c)<0) {
         t = -t;  // I want negative angles
      }
      return t;
   }
   
   // ear splitting technique
   protected static void nonConvexTriangulate(PolygonalMesh mesh, Vector3d normal, double tol) {
      
      ArrayList<Face> faces = new ArrayList<Face>(mesh.getFaces());
      
      for (Face face : faces) {
         if (!face.isTriangle()) {
            makeConvexFace(mesh,  face, normal, tol);
         }
      }
      mesh.triangulate();
      
   }
   
   // ear splitting technique
   protected static void makeConvexFace(PolygonalMesh mesh, Face face, Vector3d normal, double tol) {
      
      if (face.isTriangle()) {
         return;
      }
      
      Vector3d v1 = new Vector3d();
      Vector3d v2 = new Vector3d();
      
      Vertex3d[] vtxs = face.getVertices();
      for (int i=0; i<vtxs.length; i++) {
         Vertex3d vm1 = vtxs[(i+vtxs.length-1)%vtxs.length];
         Vertex3d v0 = vtxs[i];
         Vertex3d vp1 = vtxs[(i+1)%vtxs.length];
         
         v1.sub(v0.getWorldPoint(), vm1.getWorldPoint());
         v2.sub(vp1.getWorldPoint(), v0.getWorldPoint());
         double ang = getAngle(1, v1, 1, v2, normal);
         if (ang > -tol) {
            // concave section
            
            for (int j=1; j<vtxs.length-1; j++) {
               vp1 = vtxs[(i+j+1)%vtxs.length];
               v2.sub(vp1.getWorldPoint(), v0.getWorldPoint());
               ang = getAngle(1, v1, 1, v2, normal);
               if (ang < -tol) {
                  // XXX check that doesn't intersect any edges?
                  
                  Vertex3d[] f1 = new Vertex3d[j+2];
                  Vertex3d[] f2 = new Vertex3d[vtxs.length-j];
                  
                  for (int k=0; k<f1.length; k++) {
                     f1[k] = vtxs[ (i+k) % vtxs.length];
                  }
                  
                  for (int k=0; k<f2.length-2; k++) {
                     f2[k] = vtxs[ (i+k + f1.length) % vtxs.length];
                  }
                  f2[f2.length-2] = v0;
                  f2[f2.length-1] = f1[f1.length-1];
                  
                  mesh.removeFace(face);
                  Face face1 = mesh.addFace(f1);
                  Face face2 = mesh.addFace(f2);
                  
                  makeConvexFace(mesh, face1, normal, tol);
                  makeConvexFace(mesh, face2, normal, tol);
                  return;
                  
               }
            }
            
         }
         
      }
      
       
   }
   
}
