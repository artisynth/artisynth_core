package maspack.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import maspack.matrix.LUDecomposition;
import maspack.matrix.Matrix4d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.util.IndexedBinaryHeap;

/**
 * Methods that modify a mesh, XXX many of these methods are broken
 */
public class MeshUtilities {
   
   /**
    * Applies the Laplacian smoothing operating to the mesh, with cotangent weights
    * 
    * @param mesh polygonal mesh to smooth
    * @param lambda scale factor for gradient addition
    * @param iters number of iterations
    */
   public static void laplacianSmooth(PolygonalMesh mesh, double lambda, int iters) {
      PolygonalMesh trimesh = mesh;
      if (!mesh.isTriangular ()) {
         trimesh = new PolygonalMesh();
         trimesh.triangulate ();
      }
      
      laplacianSmoothTriMesh (trimesh, lambda, iters);
      
      if (!mesh.isTriangular ()) {
         Iterator<Vertex3d> vout = mesh.getVertices ().iterator ();
         Iterator<Vertex3d> vin = trimesh.getVertices ().iterator ();
         
         while (vout.hasNext () && vin.hasNext ()) {
            vout.next ().setPosition (vin.next ().getPosition ());
         }
      }
   }
   
   /**
    * Applies the Taubin smoothing operating to the mesh, with cotangent
    * weights, suggested factors 0 {@code <} lambda {@code <} -mu {@code <=} 1.
    * Meshlab uses lambda=0.5, mu=-0.53 by default.
    *  
    * 
    * @param mesh polygonal mesh to smooth
    * @param lambda scale factor for gradient addition (smoothing)
    * @param mu scale factor for gradient addition
    * @param iters number of iterations
    */
   public static void taubinSmooth(PolygonalMesh mesh, double lambda, double mu, int iters) {
      PolygonalMesh trimesh = mesh;
      if (!mesh.isTriangular ()) {
         trimesh = new PolygonalMesh();
         trimesh.triangulate ();
      }
      
      if (mu != 0) {
         for (int i=0; i<iters; ++i) {
            laplacianSmoothTriMesh (trimesh, lambda, 1);
            laplacianSmoothTriMesh (trimesh, mu, 1);
         }
      } else {
         laplacianSmoothTriMesh (trimesh, lambda, iters);
      }
      
      if (!mesh.isTriangular ()) {
         Iterator<Vertex3d> vout = mesh.getVertices ().iterator ();
         Iterator<Vertex3d> vin = trimesh.getVertices ().iterator ();
         
         while (vout.hasNext () && vin.hasNext ()) {
            vout.next ().setPosition (vin.next ().getPosition ());
         }
      }
   }
   
   /**
    * Applies the Laplacian smoothing operating to the mesh, with cotangent weights
    * 
    * @param mesh polygonal mesh to smooth
    * @param lambda scale factor for gradient addition
    * @param iters, number of iterations
    */
   private static void laplacianSmoothTriMesh(PolygonalMesh trimesh, double lambda, int iters) {
      
      double[] gradient = new double[3*trimesh.numVertices ()];
      double[] cotansum = new double[trimesh.numVertices ()];
      
      Vector3d diff = new Vector3d();
      
      for (int k=0; k<iters; ++k) {
         for (Face f : trimesh.getFaces ()) {
            HalfEdge he = f.he0;
            do {
               int i = he.head.idx;
               int j = he.tail.idx;
               double wij = cotangent(he);
               wij = Math.abs (wij);       // absolute value
               
               if (Double.isNaN (wij) || Double.isInfinite (wij)) {
                  wij = 0;
               }
               
               // double weight on open edges (assumes reflection)
               if (he.opposite != null) {
                  wij = wij/2;
               }
               cotansum[i] += wij;
               cotansum[j] += wij;
               diff.sub (he.head.getPosition (), he.tail.getPosition ());
               diff.scale (wij);
               
               i = 3*i;
               gradient[i++] -= diff.x;
               gradient[i++] -= diff.y;
               gradient[i] -= diff.z;
               
               j = 3*j;
               gradient[j++] += diff.x;
               gradient[j++] += diff.y;
               gradient[j++] += diff.z;
               
               he = he.next;
            } while (he != f.he0);
         }
         
         for (Vertex3d vtx : trimesh.getVertices ()) {
            int i = vtx.getIndex ();
            int j = 3*i;
            double s = lambda/cotansum[i];
            if (cotansum[i] == 0) {
               s = 0;
            }
            vtx.getPosition ().add (s*gradient[j], s*gradient[j+1], s*gradient[j+2]);
         }
      }
      
      trimesh.notifyVertexPositionsModified ();
   }
   
   /**
    * Cotangent weight between vertices sharing half-edge
    * @param he half-edge
    * @return cotangent weight (might be negative)
    */
   private static double cotangent(HalfEdge he) {
      
      Vector3d v0 = new Vector3d(he.head.getPosition ());
      Vector3d v1 = new Vector3d(he.next.head.getPosition ());
      v0.sub (v1);
      v1.sub (he.tail.getPosition ());
      
      double cost = -v0.dot (v1);
      v1.cross (v0);
      double sint = v1.norm ();
      
      // essentially inf
      double eps = 1e-10;
      if (sint < eps*cost) {
         return 1.0/eps;
      }
      return cost/sint;
   }
   
   private static class VertexQuadric {
      Vertex3d vtx;
      Matrix4d Q;
      
      public VertexQuadric(Vertex3d vtx) {
         this.vtx = vtx;
         this.Q = new Matrix4d();
         

         // update each face
         Iterator<HalfEdge> hit = vtx.getIncidentHalfEdges ();
         while (hit.hasNext ()) {
            Face f = hit.next ().face;
            if (f != null) {
               Vector3d nrm = f.getNormal ();
               double a = nrm.x;
               double b = nrm.y;
               double c = nrm.z;
               double d = -nrm.dot (vtx.getPosition ());
               Q.m00 += a*a;
               Q.m01 += a*b;
               Q.m02 += a*c;
               Q.m03 += a*d;
               Q.m11 += b*b;
               Q.m12 += b*c;
               Q.m13 += b*d;
               Q.m22 += c*c;
               Q.m23 += c*d;
               Q.m33 += d*d;
            }
         }
         
         Q.m10 = Q.m01;
         Q.m20 = Q.m02;
         Q.m30 = Q.m03;
         Q.m21 = Q.m12;
         Q.m31 = Q.m13;
         Q.m32 = Q.m23;
         
      }
   }
   
   private static double computeUmbrellaArea(Vertex3d vtx) {
      double a = 0;
      Iterator<HalfEdge> hit = vtx.getIncidentHalfEdges ();
      while (hit.hasNext ()) {
         a += hit.next ().face.computeArea ();
      }
      return a;
   }
   
   private static class HalfEdgeQuadric implements Comparable<HalfEdgeQuadric> {
      HalfEdge edge;
      double d2;
      double w;
      Vector4d p;
      
      public HalfEdgeQuadric(HalfEdge he) {
         edge = he;
         d2 = -1;
         p = new Vector4d();
      }
      
      public void updateWeight() {
         
         if (!canCollapseEdge (edge)) {
            w = Double.POSITIVE_INFINITY;
            return;
         }
         
         // check if new position would flip a normal attached to the edge
         Vector3d dir0 = new Vector3d();
         Vector3d nrm = new Vector3d();
         HalfEdgeNode hen = edge.tail.incidentHedges;
         while (hen != null) {
            if (hen.he != edge.opposite) {
               dir0.set (hen.he.tail.getPosition ());
               dir0.add (-p.x, -p.y, -p.z);
               nrm.set (hen.he.next.getHead ().getPosition ());
               nrm.add (-p.x, -p.y, -p.z);
               nrm.cross (dir0);
               nrm.normalize ();
               double dotp = nrm.dot (hen.he.face.getNormal ());
               if (dotp <= 0) {
                  w = Double.POSITIVE_INFINITY;
                  return;
               }
            }
            hen = hen.next;
         }
         
         hen = edge.head.incidentHedges;
         while (hen != null) {
            if (hen.he != edge) {
               dir0.set (hen.he.tail.getPosition ());
               dir0.add (-p.x, -p.y, -p.z);
               nrm.set (hen.he.next.getHead ().getPosition ());
               nrm.add (-p.x, -p.y, -p.z);
               nrm.cross (dir0);
               nrm.normalize ();
               double dotp = nrm.dot (hen.he.face.getNormal ());
               if (dotp <= 0) {
                  w = Double.POSITIVE_INFINITY;
                  return;
               }
            }
            hen = hen.next;
         }
         
         // root of umbrella area (so that is units of distance, like plane distances)
         w = computeUmbrellaArea (edge.head) + computeUmbrellaArea (edge.tail);
         if (edge.opposite == null) {
            w += edge.face.computeArea ();
         }
         w = Math.sqrt (w);
       
      }
      
      public void compute(Matrix4d Q0, Matrix4d Q1) {
         
         Matrix4d Q = new Matrix4d();
         Q.add (Q0);
         Q.add (Q1);
         
         if (edge.opposite == null) {
            // normal perpendicular to edge
            Vector3d n = new Vector3d(edge.head.getPosition ());
            n.sub (edge.tail.getPosition ());
            n.normalize ();
            double d1 = -n.dot (edge.head.getPosition ());
            double d2 = -n.dot (edge.tail.getPosition ());
            double a = n.x;
            double b = n.y;
            double c = n.z;
            double w = 1000;
            
            Q.m00 += 2*w*a*a;
            Q.m01 += 2*w*a*b;
            Q.m02 += 2*w*a*c;
            Q.m03 += w*a*(d1 + d2);
            Q.m10 = Q.m01;
            Q.m11 += 2*w*b*b;
            Q.m12 += 2*w*b*c;
            Q.m13 += w*b*(d1 + d2);
            Q.m20 = Q.m02;
            Q.m21 = Q.m12;
            Q.m22 += 2*w*c*c;
            Q.m23 += w*c*(d1 + d2);
            Q.m30 = Q.m03;
            Q.m31 = Q.m13;
            Q.m32 = Q.m23;
            Q.m33 += w*(d1*d1 + d2*d2);
         }
         
         double Q33 = Q.m33;
         Q.m30 = 0;
         Q.m31 = 0;
         Q.m32 = 0;
         Q.m33 = 1;
         
         LUDecomposition lud = new LUDecomposition (Q);
         Vector4d b = new Vector4d(0,0,0,1);
         
         // tikhonov regularization
         double eps = 1e-10*Math.sqrt (Q33);
         Q.m00 += eps;
         Q.m11 += eps;
         Q.m22 += eps;
         Point3d hpos = edge.head.getPosition ();
         Point3d tpos = edge.tail.getPosition ();
         b.x += 0.5*eps*(hpos.x + tpos.x);
         b.y += 0.5*eps*(hpos.y + tpos.y);
         b.z += 0.5*eps*(hpos.z + tpos.z);
         lud.factor (Q);
         lud.solve (p, b);
         
         Q.m30 = Q.m03;
         Q.m31 = Q.m13;
         Q.m32 = Q.m23;
         Q.m33 = Q33;
         
         b.set (p);
         Q.mul (b);
         d2 = p.dot (b);
       
         updateWeight ();
      }
      
      public double getSquaredDistance() {
         return d2;
      }
      
      public void getOptimalPosition(Point3d pnt) {
         pnt.x = p.x;
         pnt.y = p.y;
         pnt.z = p.z;
      }

      @Override
      public int compareTo (HalfEdgeQuadric other) {
         return Double.compare (d2, other.d2);
      }
   }
   
   private static boolean disconnectFaceIfDegenerate (Face face) {

      HalfEdge he = face.firstHalfEdge();
      HalfEdge hn = he.next;
      if (hn.next == he) {
         // face has only two edges, so disconnect

         // this is an unrolled call to face.disconnect(), where we also attach
         // adjacent faces, and set he.opposite and hn.opposite to null, so
         // that when face.disconnect() is called later by removeFaces(), it
         // won't damage adjacent structures
         he.head.removeIncidentHalfEdge (he);
         hn.head.removeIncidentHalfEdge (hn);

         HalfEdge heOpp = he.opposite;
         HalfEdge hnOpp = hn.opposite;

         if (heOpp != null) {
            heOpp.opposite = hnOpp;
            he.opposite = null;
         }
         if (hnOpp != null) {
            hnOpp.opposite = heOpp;
            hn.opposite = null;
         }
         // adjust hardness. New edge can only be hard if it is
         // not open and one of the previous edges was hard
         if (heOpp != null && hnOpp != null) {
            if (heOpp.isHard() || hnOpp.isHard()) {
               heOpp.setHard (true);
               hnOpp.setHard (true);
            }
            // make sure heOpp and hnOpp are primary opposites
            if (heOpp.isPrimary() == hnOpp.isPrimary()) {
               hnOpp.setPrimary (!hnOpp.isPrimary());
            }
         }
         else {
            if (heOpp != null) {
               heOpp.setHard (false);
               heOpp.setPrimary (true);
            }
            if (hnOpp != null) {
               hnOpp.setHard (false);
               hnOpp.setPrimary (true);
            }
         }
         return true;
      }
      else {
         return false;
      }
   }
   
   private static boolean rotateEdge (HalfEdge he) {

      HalfEdge te = he.opposite; // incident edge for tail, if any
      if (te == null) {
         return false;
      }
      
      HalfEdge hnext = he.next;
      HalfEdge hprev = he.face.getPreviousEdge (he);
      HalfEdge tnext = te.next;
      HalfEdge tprev = te.face.getPreviousEdge (te);
      
      he.head.removeIncidentHalfEdge (he);
      te.head.removeIncidentHalfEdge (te);
      
      he.next = hnext.next;
      hprev.next = tnext;
      te.next = tnext.next;
      tprev.next = hnext;
      
      he.head = hnext.head;
      he.tail = tnext.head;
      te.head = tnext.head;
      te.tail = hnext.head;
      
      tnext.next = he;
      hnext.next = te;
      
      he.head.addIncidentHalfEdge (he);
      te.head.addIncidentHalfEdge (te);
      
      // update face for moved edges
      hnext.face = te.face;
      tnext.face = he.face;
      
      // update first half-edges if now bad
      if (he.face.he0 == hnext) {
         he.face.he0 = tnext;
      }
      if (te.face.he0 == tnext) {
         te.face.he0 = hnext;
      }
      
      return true;
   }
   
   private static Vertex3d midpointSplit(Face f) {
      PolygonalMesh mesh = f.getMesh ();
      
      // compute and add new vertex
      Point3d pnt = new Point3d();
      f.computeCentroid (pnt);
      Vertex3d mvtx = new Vertex3d(pnt);
      mesh.addVertex (mvtx);
      
      // remove face and re-triangulate
      if (f.isTriangle ()) {
         HalfEdge he = f.he0;
         HalfEdge hnext = he.next;
         HalfEdge hnextOpp = hnext.opposite;
         HalfEdge hprev = hnext.next;
         HalfEdge hprevOpp = hprev.opposite;
         Vertex3d v0 = he.head;
         Vertex3d v1 = hnext.head;
         Vertex3d v2 = hprev.head;
         
         // detach next
         hnext.head.removeIncidentHalfEdge (hnext);
         if (hnextOpp != null) {
            hnextOpp.opposite = null;
            hnextOpp.setPrimary (true);
            hnext.opposite = null;
         }
         hnext.setHard (false);
         hnext.setPrimary (true);
         
         // detach prev
         if (hprevOpp != null) {
            hprevOpp.opposite = null;
            hprevOpp.setPrimary (true);
            hprev.opposite = null;
         }
         hprev.setHard (false);
         hprev.setPrimary (true);
         
         /// adjust current face to use midpoint
         hnext.head = mvtx;
         mvtx.addIncidentHalfEdge (hnext);
         hprev.tail = mvtx;
         
         // add two new faces
         Face f1 = mesh.addFace (v1, mvtx, v0);
         if (f1.he0.opposite != null && f1.he0.opposite.isHard()) {
            f1.he0.setHard (true);
         }
         
         Face f2 = mesh.addFace (v2, mvtx, v1);
         if (f2.he0.opposite != null && f2.he0.opposite.isHard()) {
            f2.he0.setHard (true);
         }
         
      } else {
         // remove face and add new triangles
         HalfEdge he = f.he0;
         mesh.removeFaceFast (f);
         
         do {
            Face fm = mesh.addFace (he.head, mvtx, he.tail);
            if (fm.he0.opposite != null && fm.he0.opposite.isHard ()) {
               fm.he0.setHard (true);
            }
            he = he.next;
         } while (he != f.he0);
      }
      
      return mvtx;
   }
   
   private static HalfEdge edgeSplit3(HalfEdge edge) {
      Face f = edge.face;
      f.he0 = edge;
      
      Vertex3d head = edge.head;
      Vertex3d tail = edge.tail;
      Vertex3d vtp = edge.next.head;
      
      HalfEdge edgeOpp = edge.opposite;      
      boolean hard = edge.isHard ();
      
      Point3d tpos = tail.getPosition ();
      Vector3d dir = new Vector3d(head.getPosition ());
      dir.sub (tpos);
      dir.scale (1.0/3);
      Vertex3d vt0 = new Vertex3d(tpos.x + dir.x, tpos.y + dir.y, tpos.z + dir.z);
      Vertex3d vt1 = new Vertex3d(tpos.x + 2*dir.x, tpos.y + 2*dir.y, tpos.z + 2*dir.z);
      
      // add new vertices
      PolygonalMesh mesh = f.getMesh ();
      mesh.addVertex (vt0);
      mesh.addVertex (vt1);
      
      // detach opposite edge temporarily
      if (edgeOpp != null) {
         edge.opposite = null;
         edgeOpp.opposite = null;
         edge.setPrimary (true);
         edgeOpp.setPrimary (true);
         
         Face of = edgeOpp.getFace ();
         Vertex3d ovtp = edgeOpp.next.head;
         
         if (of.isTriangle ()) {
            // adjust face
            HalfEdge hprev = edgeOpp.next.next;
            // detach next's opposite
            if (hprev.opposite != null) {
               hprev.opposite.setPrimary (true);
               hprev.opposite.opposite = null;
               hprev.opposite = null;
               hprev.setPrimary (true);
            }
            hprev.setHard (false);
            
            // move opposite edge's tail to vt0
            head.removeIncidentHalfEdge (hprev);
            hprev.head = vt0;
            edgeOpp.tail = vt0;
            vt0.addIncidentHalfEdge (hprev);
            
         } else {
            // remove and add re-add first triangle
            mesh.removeFaceFast (of);
            Face f0 = mesh.addFace (tail, ovtp, vt0);
            edgeOpp = f0.he0;
            edgeOpp.setHard (hard);
         }
        
         // add other two faces
         Face f1 = mesh.addFace (vt1, ovtp, head);
         f1.he0.setHard (hard);
         Face f2 = mesh.addFace (vt0, ovtp, vt1);
         f2.he0.setHard (hard);
      }
      
      // adjust face
      if (f.isTriangle ()) {
         HalfEdge hnext = edge.next;
         // detach next's opposite
         if (hnext.opposite != null) {
            hnext.opposite.setPrimary (true);
            hnext.opposite.opposite = null;
            hnext.opposite = null;
            hnext.setPrimary (true);
         }
         hnext.setHard (false);
         
         // move edge's head to vt0
         head.removeIncidentHalfEdge (edge);
         edge.head = vt0;
         hnext.tail = vt0;
         vt0.addIncidentHalfEdge (edge);
         
      } else {
         // add first triangle
         mesh.removeFaceFast (f);
         Face f0 = mesh.addFace (vt0, vtp, tail);
         edge = f0.he0;
         edge.setHard (hard);
      }
      
      // add next two faces
      Face f1 = mesh.addFace (vt1, vtp, vt0);
      f1.he0.setHard (hard);      
      Face f2 = mesh.addFace (head, vtp, vt1);
      f2.he0.setHard (hard);
      
      // reconnect edge
      edge.opposite = edgeOpp;
      edge.setPrimary (true);
      if (edgeOpp != null) {
         edgeOpp.opposite = edge;
         edgeOpp.setPrimary (false);
      }
      
      return edge;
   }
   
   private static boolean canCollapseEdge(HalfEdge he) {
      Vertex3d head = he.head;
      Vertex3d tail = he.tail;
      
      HalfEdge hprev = he.face.getPreviousEdge (he);
      if (hprev.tail == head) {
         return false;
      }
      
      // opposite edge
      HalfEdge hopp = he.opposite;
      if (hopp != null) {
         HalfEdge hnext = hopp.next;
         if (hnext.head == hopp.tail) {
            return false;
         }
      }
      
      // check if collapsing would change topology 
      HalfEdgeNode node = tail.incidentHedges;
      while (node != null) {
         HalfEdge ee = node.he;
         if (ee != he.opposite) {
            if (ee.tail == head) {
               return false;
            }
            if (ee.next != he && ee.next.head == head) {
               return false;
            }
         }
         node = node.next;
      }
      return true;
   }
   
   private static void collapseEdge (HalfEdge he) {

      HalfEdge te = he.opposite; // incident edge for tail, if any
      
      Vertex3d head = he.head;
      Vertex3d tail = he.tail;

      HalfEdge hprev = he.face.getPreviousEdge (he);
      HalfEdge hnext = he.next;

      head.removeIncidentHalfEdge (he);
      tail.removeIncidentHalfEdge (hprev);

      head.addIncidentHalfEdge (hprev);
      hprev.next = hnext;
      hprev.head = head;
      if (he.face.he0 == he) {
         he.face.he0 = hprev;
      }

      if (te != null) {
         HalfEdge tprev = te.face.getPreviousEdge(te);
         HalfEdge tnext = te.next;

         tail.removeIncidentHalfEdge (te);
         tprev.next = tnext;

         tnext.tail = head;
         if (te.face.he0 == te) {
            te.face.he0 = tprev;
         }
      }

      // move other faces connected to tail 
      HalfEdgeNode node = tail.incidentHedges;
      while (node != null) {
         HalfEdge ee = node.he;
         head.addIncidentHalfEdge (ee);
         ee.head = head;
         ee.next.tail = head;
         node = node.next;
      }
      tail.incidentHedges = null;
      
      PolygonalMesh mesh = he.face.getMesh ();
      mesh.removeVertexFast(tail);

      if (disconnectFaceIfDegenerate (he.face)) {
        mesh.removeFaceFast (he.face);
      }

      if (te != null && disconnectFaceIfDegenerate (te.face)) {
         mesh.removeFaceFast (te.face);
      }
      
      mesh.invalidateTriQuadCounts ();

   }
   
   /**
    * Quadric edge-collapse decimation, removing vertices through edge-collapse based on
    * least quadric plane error
    * 
    * @param mesh mesh to update
    * @param n number of edges to collapse
    */
   public static void quadricEdgeCollapse(PolygonalMesh mesh, int n) {
      
      if (n <= 0) {
         return;
      }
      
      final ArrayList<HalfEdgeQuadric> edgeQuadrics = new ArrayList<HalfEdgeQuadric>();
      HashMap<HalfEdge,Integer> edgeQuadricMap = new HashMap<>();
      
      HashMap<Vertex3d,VertexQuadric> vertexQuadrics = new HashMap<>();
      
      for (Vertex3d vtx : mesh.getVertices ()) {
         vertexQuadrics.put (vtx, new VertexQuadric(vtx));
      }
      
      int nedges = 0;
      for (Face f : mesh.getFaces ()) {
         HalfEdge he0 = f.firstHalfEdge ();
         HalfEdge he = he0;
         do {
            he.clearVisited ();
            if (he.isPrimary ()) {
               edgeQuadricMap.put (he, nedges);
               HalfEdgeQuadric heq = new HalfEdgeQuadric (he);
               heq.compute (vertexQuadrics.get (he.head).Q, vertexQuadrics.get (he.tail).Q);
               edgeQuadrics.add (heq);
               ++nedges;
            }
            he = he.next;
         } while (he != he0);
      }
      
      // compare by distance squared
      Comparator<Integer> quadricComparator = new Comparator<Integer>() {
         @Override
         public int compare (Integer i0, Integer i1) {
            HalfEdgeQuadric he0 = edgeQuadrics.get (i0);
            HalfEdgeQuadric he1 = edgeQuadrics.get (i1);

            int c = Double.compare (he0.getSquaredDistance ()*he0.w, he1.getSquaredDistance ()*he1.w);
            return c;
         }
      };
      IndexedBinaryHeap minHeap = new IndexedBinaryHeap (nedges, quadricComparator, true);
      for (int i=0; i<edgeQuadrics.size (); ++i) {
         minHeap.add (i);
      }
      
      // remove half-edges
      for (int i=0; i<n && !minHeap.isEmpty (); ++i) {
         
         // remove quadric
         int removeIdx = minHeap.poll ();
         HalfEdgeQuadric heq = edgeQuadrics.get (removeIdx);
         HalfEdge he = heq.edge;
         
         if (!canCollapseEdge (he)) {
            // collapsing edge would change topology
            --i;
         } else {
            edgeQuadrics.set (removeIdx, null);
            edgeQuadricMap.remove (heq.edge);
            
            // update vertex position
            Vertex3d vtx = he.head;
            Vertex3d ovtx = he.tail;
            heq.getOptimalPosition (vtx.getPosition ());
            
            // update vertex quadric
            VertexQuadric ovtxq = vertexQuadrics.remove (ovtx);
            VertexQuadric nvtxq = vertexQuadrics.get (vtx);
            nvtxq.Q.add (ovtxq.Q);
            
            // remove quadrics for attached triangular face
            if (he.face.isTriangle ()) {
               HalfEdge next = he.next;
               HalfEdge prev = next.next;
               
               // remove one or both quadrics, both only if both opposites are null
               if (next.opposite == null && prev.opposite == null) {
                  int idx = edgeQuadricMap.remove (prev);
                  minHeap.remove (idx);
                  edgeQuadrics.set (idx, null);
                  idx = edgeQuadricMap.remove (next);
                  minHeap.remove (idx);
                  edgeQuadrics.set (idx, null);
               } else if (next.opposite == null){
                  int idx = edgeQuadricMap.remove (next);
                  minHeap.remove (idx);
                  edgeQuadrics.set (idx, null);
                  
                  // move primary to prev's opposite
                  if (prev.isPrimary ()) {
                     idx = edgeQuadricMap.remove (prev);
                     edgeQuadrics.get (idx).edge = prev.opposite;
                     edgeQuadricMap.put (prev.opposite, idx);
                     prev.setPrimary (false);
                     prev.opposite.setPrimary (true);
                  }
               } else {
                  // remove prev's quadric, ensure next is not primary
                  int idx = edgeQuadricMap.remove (prev.getPrimary ());
                  minHeap.remove (idx);
                  edgeQuadrics.set (idx, null);
                  
                  // ensure prev is primary, since will merge with next's opposite
                  if (!prev.isPrimary ()) {
                     prev.setPrimary (true);
                     prev.opposite.setPrimary (false);
                  }
                  
                  if (next.isPrimary ()) {
                     idx = edgeQuadricMap.remove (next);
                     edgeQuadrics.get (idx).edge = next.opposite;
                     edgeQuadricMap.put (next.opposite, idx);
                     next.setPrimary (false);
                     next.opposite.setPrimary (true);
                  }
               }
            }
            if (he.opposite != null && he.opposite.face.isTriangle ()) {
               HalfEdge next = he.opposite.next;
               HalfEdge prev = next.next;
               
               // remove one or both quadrics, both only if both opposites are null
               if (next.opposite == null && prev.opposite == null) {
                  int idx = edgeQuadricMap.remove (prev);
                  minHeap.remove (idx);
                  edgeQuadrics.set (idx, null);
                  idx = edgeQuadricMap.remove (next);
                  minHeap.remove (idx);
                  edgeQuadrics.set (idx, null);
               } else if (next.opposite == null){
                  int idx = edgeQuadricMap.remove (next);
                  minHeap.remove (idx);
                  edgeQuadrics.set (idx, null);
                  
                  if (prev.isPrimary ()) {
                     idx = edgeQuadricMap.remove (prev);
                     edgeQuadrics.get (idx).edge = prev.opposite;
                     edgeQuadricMap.put (prev.opposite, idx); 
                     prev.setPrimary (false);
                     prev.opposite.setPrimary (true);
                  }
               } else {
                  // remove prev's quadric, ensure next is not primary
                  int idx = edgeQuadricMap.remove (prev.getPrimary ());
                  minHeap.remove (idx);
                  edgeQuadrics.set (idx, null);
                  
                  // ensure prev is primary, since will merge with next's opposite
                  if (!prev.isPrimary ()) {
                     prev.setPrimary (true);
                     prev.opposite.setPrimary (false);
                  }
                  
                  if (next.isPrimary ()) {
                     idx = edgeQuadricMap.remove (next);
                     edgeQuadrics.get (idx).edge = next.opposite;
                     edgeQuadricMap.put (next.opposite, idx);
                     next.setPrimary (false);
                     next.opposite.setPrimary (true);
                  }
               }
            }
            
            collapseEdge (he);
            
            // update adjacent edges
            Iterator<HalfEdge> hit = vtx.getIncidentHalfEdges ();
            
            //          Vector3d nrm = new Vector3d();
            while (hit.hasNext ()) {
               HalfEdge hh = hit.next ();
               //               hh.face.computeNormal (nrm);
               //               double d = nrm.dot (hh.face.getNormal ());
               //               if (d < 0) {
               //                  System.out.println (he.tail.getIndex () + ", " + he.head.getIndex () + ", " + hh.tail.getIndex () + ", " + hh.next.head.getIndex());
               //                  System.out.println ("flipped");
               //                  hh.face.computeNormal (nrm);
               //               }
               hh.face.clearNormal ();
               HalfEdge hprimary = hh.getPrimary ();
               Integer hidx = edgeQuadricMap.get (hprimary);
               HalfEdgeQuadric hq = edgeQuadrics.get (hidx);
               hq.compute (vertexQuadrics.get (hh.head).Q, vertexQuadrics.get (hh.tail).Q);
               minHeap.update (hidx);
               hprimary.setVisited ();
            }
            
            // update all edges affected by faces
            // for each incident half edge, update for all edges extending from tail
            hit = vtx.getIncidentHalfEdges ();
            while (hit.hasNext ()) {
               HalfEdge hh0 = hit.next ();
               Iterator<HalfEdge> hhit = hh0.tail.getIncidentHalfEdges ();
               while (hhit.hasNext ()) {
                  HalfEdge hhh = hhit.next ().getPrimary ();
                  if (!hhh.isVisited ()) {
                     Integer hidx = edgeQuadricMap.get (hhh);
                     HalfEdgeQuadric hq = edgeQuadrics.get (hidx);
                     hq.updateWeight ();   
                     minHeap.update (hidx);
                     hhh.setVisited ();
                  }
               }
            }
            
            // clear visited flag
            hit = vtx.getIncidentHalfEdges ();
            while (hit.hasNext ()) {
               HalfEdge hh0 = hit.next ();
               hh0.getPrimary ().clearVisited ();
               Iterator<HalfEdge> hhit = hh0.tail.getIncidentHalfEdges ();
               while (hhit.hasNext ()) {
                  HalfEdge hhh = hhit.next ().getPrimary ();
                  hhh.clearVisited ();
               }
            }
         }
      }
      
      mesh.notifyVertexPositionsModified ();
      mesh.updateTriQuadCounts ();
      mesh.notifyStructureChanged ();
   }
   
   /**
    * Odd iterations result in border split, even iterations border is not split
    * @param mesh mesh to guess if split is odd or even
    * @return 
    */
   private static boolean guessIfEven(PolygonalMesh mesh) {
      // check if even or odd split,
      //  if any face has two borders, then is even
      //     if border is not the first half-edge, then is even
      //     otherwise, if most borders are the longest edge, assume odd
      int numLongBorders = 0;
      int numBorders = 0;
      for (Face f : mesh.getFaces ()) {
         HalfEdge he = f.he0;
         int numFaceBorders = 0;
         do {
            if (isBorderEdge(he)) {
               ++numFaceBorders;
               double l0 = he.head.distance (he.tail);
               double l1 = he.head.distance (he.next.head);
               if (l0 > l1) {
                  double l2 = he.next.head.distance (he.tail);
                  if (l0 > l2) {
                    ++numLongBorders;
                  }
               }
            }
            he = he.next;
         } while (he != f.he0);
         numBorders += numFaceBorders;
         if (numFaceBorders > 1) {
            return true;
         } else if (numFaceBorders == 1 && !isBorderEdge(f.he0)) {
            return true;
         }
      }
      
      if (2*numLongBorders > numBorders) {
         return false;
      }
      return true;
   }
   
   private static boolean isBorderEdge(HalfEdge he) {
      if (he.opposite == null || he.isHard ()) {
         return true;
      }
      return false;
   }
   
   private static HalfEdge findBorderEdge(Face face) {
      HalfEdge he = face.he0;
      do {
         if (isBorderEdge(he)) {
            return he;
         }
         he = he.next;
      } while (he != face.he0);
      
      return null;
   }
   
   /**
    * Sqrt-3 subdivision
    * 
    * @param mesh mesh to subdivide
    * @param niters number of iterations
    */
   public static void sqrt3Subdivide(PolygonalMesh mesh, int niters) {
      
      if (!mesh.isTriangular ()) {
         mesh.triangulate ();
      }
      
      boolean even = guessIfEven (mesh);
      sqrt3Subdivide (mesh, even, niters);
   }
   
   private static interface VertexData {
      public boolean isBorder();
      public void relax();
   }
   
   private static class CentralVertexData implements VertexData {
      double gamma;
      Point3d pinf;
      Vertex3d vtx;
      
      public CentralVertexData(Vertex3d vtx) {
         this.vtx = vtx;
         
         pinf = new Point3d();
         int n = 0;
         HalfEdgeNode hen = vtx.incidentHedges;
         while (hen != null) {
            HalfEdge he = hen.he;
            ++n;
            pinf.add (he.tail.getPosition ());
            hen = hen.next;
         }
         
         double alpha = (4 - 2*Math.cos (2*Math.PI/n))/9;
         double beta = 3*alpha/(1+3*alpha);
         pinf.scale (beta/n);
         pinf.scaledAdd (1-beta, vtx.getPosition ());
         gamma = 2.0/3 - alpha;
      }
      
      public void relax() {
         vtx.getPosition().interpolate (pinf, gamma, vtx.getPosition ());
      }
      
      @Override
      public boolean isBorder () {
         return false;
      }
   }
   
   private static class BorderVertexData implements VertexData {
      Vertex3d vtx;
      Vertex3d vtxp;
      Vertex3d vtxn;
      
      // "next" info
      Point3d npos;
      Vertex3d nvtxp;
      Vertex3d nvtxn;
      
      public BorderVertexData(Vertex3d vtxp, Vertex3d vtx, Vertex3d vtxn) {
         this.vtx = vtx;
         this.vtxp = vtxp;
         this.vtxn = vtxn;
         this.nvtxp = null;
         this.nvtxn = null;
         npos = new Point3d();
         updateNewPos ();
      }
      
      /**
       * Prepare for a neighbour swap on relax
       * @param orig vertex to replace
       * @param nvtx new vertex
       */
      public void queueNeighbour(Vertex3d orig, Vertex3d nvtx) {
         if (vtxp == orig) {
            nvtxp = nvtx;
         } else {
            nvtxn = vtx;
         }
      }
      
      public Vertex3d getOtherNeighbour(Vertex3d nbr) {
         if (nbr == vtxn) {
            return vtxp;
         }
         return vtxn;
      }
      
      private void updateNewPos() {
         if (vtxp == null || vtxn == null) {
            // position stays the same
            npos.set (vtx.getPosition ());
         } else {
            
            Point3d cpos = vtx.getPosition ();
            Point3d lpos = vtxp.getPosition ();
            Point3d rpos = vtxn.getPosition ();
            // cubic spline interpolation
            npos.x = (4.0*lpos.x + 19.0*cpos.x + 4.0*rpos.x)/27.0;
            npos.y = (4.0*lpos.y + 19.0*cpos.y + 4.0*rpos.y)/27.0;
            npos.z = (4.0*lpos.z + 19.0*cpos.z + 4.0*rpos.z)/27.0;
         }
      }
      
      public void relax() {
         vtx.setPosition (npos);
         
         vtxp = nvtxp;
         vtxn = nvtxn;
         nvtxp = null;
         nvtxn = null;
         updateNewPos ();
      }
      
      @Override
      public boolean isBorder () {
         return true;
      }
   }
   
   
   private static HalfEdge findPrevHalfEdge(HalfEdge he) {
      HalfEdge hp = he;
      while (hp.next != he) {
         hp = hp.next;
      }
      return hp;
   }
   
   private static HalfEdge findPrevBorder(HalfEdge he) {
      HalfEdge hp = findPrevHalfEdge (he);
      // ensure never gets stuck with tortoise/hare type of chase
      int count = 10;
      int iters = 0;
      while (!isBorderEdge (hp)) {
         hp = findPrevHalfEdge(hp.opposite);
         if (hp == he) {
            return null;
         }
         // reset he to catch loop
         ++iters;
         if (iters == count) {
            ++count;
            iters = 0;
            he = hp;
         }
      }
      return hp;
   }
   
   private static HalfEdge findNextBorder(HalfEdge he) {
      HalfEdge hn = he.next;
      // ensure never gets stuck with tortoise/hare type of chase
      int count = 10;
      int iters = 0;
      while (!isBorderEdge (hn)) {
         hn = hn.opposite.next;
         if (hn == he) {
            return null;
         }
         // reset he to catch loop
         ++iters;
         if (iters == count) {
            ++count;
            iters = 0;
            he = hn;
         }
      }
      return hn;
   }
   
   /**
    * Sqrt-3 subdivision
    * 
    * @param mesh mesh to subdivide
    * @param even set first iteration to an "even" split, where borders are not divided
    * @param niters number of iterations
    */
   public static void sqrt3Subdivide(PolygonalMesh mesh, boolean even, int niters) {
      
      if (!mesh.isTriangular ()) {
         mesh.triangulate ();
      }
      
      boolean splitBorders = !even;
      
      ArrayList<VertexData> vdata = new ArrayList<VertexData>(mesh.numVertices ());
      
      // collect border neighbours
      // HashMap<Vertex3d,HashSet<Vertex3d>> borderNeighbors = new HashMap<> ();
      
      // determine if a vertex is a border, end-point or center
      //  end-points have one or 3+ border edges, will remain fixed
      for (Vertex3d vtx : mesh.getVertices ()) {
         Vertex3d[] nbrs = new Vertex3d[2];
         int nborders = 0;
         HashSet<Vertex3d> opposites = new HashSet<>();
         
         // collect neighboring borders
         HalfEdgeNode hen = vtx.incidentHedges;
         while (hen != null) {
            HalfEdge he = hen.he;
            if (opposites.contains (he.tail)) {
               nborders = -1;  // non-manifold
               break;
            } else {
               opposites.add (he.tail);
            }
            if (isBorderEdge (he)) {
               if (nborders == 0) {
                  nbrs[0] = he.tail;
                  ++nborders;
               } else if (nborders == 1) {
                  if (he.tail != nbrs[0]) {
                     nbrs[1] = he.tail;
                     ++nborders;
                  }
               } else {
                  ++nborders;
                  break;
               }
            }
            // non-incident border edge
            if (he.next.opposite == null) {
               if (opposites.contains (he.next.head)) {
                  nborders = -1;  // non-manifold
                  break;
               } else {
                  opposites.add(he.next.head);
               }
               if (nborders == 0) {
                  nbrs[0] = he.next.head;
                  ++nborders;
               } else if (nborders == 1) {
                  if (he.next.head != nbrs[0]) {
                     nbrs[1] = he.next.head;
                     ++nborders;
                  }
               } else {
                  ++nborders;
                  break;
               }
            }
            hen = hen.next;
         }
         
         if (nborders == 0) {
            vdata.add (new CentralVertexData(vtx));
         } else if (nborders == 1) {
            vdata.add (new BorderVertexData(null, vtx, nbrs[0]));
         } else if (nborders == 2) {
            vdata.add (new BorderVertexData (nbrs[0], vtx, nbrs[1]));
         } else {
            vdata.add (new BorderVertexData(null, vtx, null));
         }
      }      

      // clear visited on all edges
      for (Face face : mesh.getFaces ()) {
         HalfEdge he0 = face.he0;
         HalfEdge he = he0;
         do {
            he.clearVisited ();
            he = he.next;
         } while (he != he0);
      }
      
      for (int k=0; k<niters; ++k) {

         int oldNumVertices = mesh.numVertices ();
         
         // split all faces at midpoint
         int nFaces = mesh.numFaces ();
         for (int i=0; i<nFaces; ++i) {
            Face face = mesh.getFace (i);
            
            if (splitBorders && isBorderEdge(face.he0)) {
               HalfEdge he = face.he0;
               if (he.opposite == null 
                  || he.opposite.face.getIndex () < face.getIndex()) {
                  
                  // find next and previous border vertices for interpolation
                  Vertex3d head = he.head;
                  Vertex3d tail = he.tail;
                  Vertex3d left = null;
                  Vertex3d right = null;
                  VertexData vdat = vdata.get (head.getIndex ());
                  BorderVertexData headData = null;
                  if (vdat.isBorder ()) {
                     headData = (BorderVertexData)vdat;
                     right = headData.getOtherNeighbour (tail);
                  } else {
                     System.out.println ("Error on vertex " + head.getIndex ());
                  }
                  vdat = vdata.get (tail.getIndex ());
                  BorderVertexData tailData = null;
                  if (vdat.isBorder ()) {
                     tailData = (BorderVertexData)vdat;
                     left = tailData.getOtherNeighbour (head);
                  } else {
                     System.out.println ("Error on vertex " + tail.getIndex ());
                  }
                  
                  // split from tail to head
                  HalfEdge be0 = edgeSplit3 (face.he0);
                  HalfEdge be1 = be0.next.opposite.next;
                  HalfEdge be2 = be1.next.opposite.next;
                  
                  // interpolate positions with cubic splines
                  // if left/right don't exist, interpolation is linear,
                  //   corresponding to cubic if end-point is fixed
                  if (left != null) {
                     Point3d b0 = be0.head.getPosition ();
                     Point3d lpos = left.getPosition ();
                     Point3d cpos = tail.getPosition ();
                     Point3d rpos = head.getPosition ();
                     b0.x = (1.0*lpos.x + 16.0*cpos.x + 10.0*rpos.x)/27.0;
                     b0.y = (1.0*lpos.y + 16.0*cpos.y + 10.0*rpos.y)/27.0;
                     b0.z = (1.0*lpos.z + 16.0*cpos.z + 10.0*rpos.z)/27.0;
                  }
                  if (right != null) {
                     Point3d b1 = be1.head.getPosition ();
                     Point3d lpos = tail.getPosition ();
                     Point3d cpos = head.getPosition ();
                     Point3d rpos = right.getPosition ();
                     b1.x = (1.0*rpos.x + 16.0*cpos.x + 10.0*lpos.x)/27.0;
                     b1.y = (1.0*rpos.y + 16.0*cpos.y + 10.0*lpos.y)/27.0;
                     b1.z = (1.0*rpos.z + 16.0*cpos.z + 10.0*lpos.z)/27.0;
                  }
                  
                  // mark next neighbours
                  if (headData != null) {
                     headData.queueNeighbour (tail, be2.tail);
                  }
                  if (tailData != null) {
                     tailData.queueNeighbour (head, be0.head);
                  }
                  
                  // add new vdata for border vertices
                  vdata.add(new BorderVertexData (be0.tail, be0.head, be1.tail));
                  vdata.add(new BorderVertexData (be1.tail, be1.head, be2.tail));
                  
               }
            } else {
               Vertex3d vtxm = midpointSplit (face);
               vdata.add(new CentralVertexData (vtxm));
            }
         }
         
         // mark edges to flip
         for (Face f : mesh.getFaces ()) {
            HalfEdge he0 = f.he0;
            if (!isBorderEdge (he0)) {
               he0.setVisited ();
            }
         }
         
         // flip edges
         nFaces = mesh.numFaces ();
         for (int i=0; i<nFaces; ++i) {
            Face face = mesh.getFace (i);
            HalfEdge he = face.he0;
            
            // flip edge if it is not a border
            //   and opposite face has lower index, or opposite face is a border face
            if (he.isVisited () && ( he.opposite.face.getIndex () < face.getIndex ()
                     || isBorderEdge(he.opposite.face.he0))) {
               rotateEdge (he);
               
               // mark as rotated
               he.clearVisited ();
               he.opposite.clearVisited ();
               
               // correct first half-edges for consistency
               // border edges must be first, otherwise rotated is first
               if (he.face.he0 != he) {
                  he.face.he0 = he;
               }
               HalfEdge be = he.next;
               do {
                  if (isBorderEdge (be)) {
                     he.face.he0 = be;
                     break;
                  }
                  be = be.next;
               } while (be != he);
               
               HalfEdge te = he.opposite;
               if (te.face.he0 != te) {
                  te.face.he0 = te;
               }
               be = te.next;
               do {
                  if (isBorderEdge (be)) {
                     te.face.he0 = be;
                     break;
                  }
                  be = be.next;
               } while (be != te);
            }
         }
         
         // set relaxed positions
         for (int i=0; i<oldNumVertices; ++i) {
            VertexData dat = vdata.get (i);
            
            if (splitBorders || !dat.isBorder ()) {
               dat.relax ();
            }
         }
         
         splitBorders = !splitBorders;
      }
      
      mesh.notifyStructureChanged ();
      mesh.notifyVertexPositionsModified ();
   }
   
   
   private static HalfEdge findNextNullBorder(HalfEdge he) {
      HalfEdge hn = he.next;
      // ensure never gets stuck with tortoise/hare type of chase
      int count = 10;
      int iters = 0;
      while (hn.opposite != null) {
         hn = hn.opposite.next;
         if (hn == he) {
            return null;
         }
         // reset he to catch loop
         ++iters;
         if (iters == count) {
            ++count;
            iters = 0;
            he = hn;
         }
      }
      return hn;
   }
   
   /**
    * Attempts to close holes smaller or equal to the given size
    * @param mesh mesh to close
    * @param size max number of edges in hole (if {@code <=} 0, closes holes of
    * any size)
    */
   public static void closeHoles(PolygonalMesh mesh, int size) {
      
      for (Face f : mesh.getFaces ()) {
         HalfEdge he = f.he0;
         do {
            he.clearVisited ();
            he = he.next;
         } while (he != f.he0);
      }
      
      // find holes and close then
      ArrayList<Vertex3d[]> tris = new ArrayList<Vertex3d[]>();
      for (Face f : mesh.getFaces ()) {
         HalfEdge hs = f.he0;
         do {
            if (hs.opposite == null && !hs.isVisited ()) {
               ArrayList<Vertex3d> verts = new ArrayList<>();
               verts.add (hs.head);
               hs.setVisited ();
               int n = 1;
               
               // find end of face
               HalfEdge he = hs;
               HalfEdge hnext = findNextNullBorder (he);
               while (hnext != null && hnext != hs) {
                  ++n;
                  he = hnext;
                  verts.add(he.head);
                  he.setVisited ();
                  hnext = findNextNullBorder (he);
               }
               
               if (n >= 3 && (size <= 0 || n <= size)) {
                  Collections.reverse (verts);
                  Face face = new Face(-1);
                  Vertex3d[] vtxs = verts.toArray (new Vertex3d[verts.size()]);
                  face.set (vtxs, vtxs.length, false);
                  face.triangulate (tris);
               }
            }
            hs = hs.next;
         } while (hs != f.he0);
      }
      
      // add triangles
      for (Vertex3d[] tri : tris) {
         mesh.addFace (tri);
      }
         
   }
   

}
