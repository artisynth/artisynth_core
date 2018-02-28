package maspack.collision;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import maspack.geometry.BVFeatureQuery;
import maspack.geometry.BVIntersector;
import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TriTriIntersection;
import maspack.geometry.TriangleIntersector;
import maspack.geometry.Vertex3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;

public class MeshCollider implements AbstractCollider {

   //private TriangleIntersector intersector = new TriangleIntersector();
   //private BVIntersector myIntersector = new BVIntersector();
   //private BVFeatureQuery myQuery = new BVFeatureQuery();

   private static final double EPS = 1e-12;
   //private double epsilon = 0;

//   // The minimum distance between contact points
//   private double pointtolerance = 1e-4;
//
//   // The minimum distance between regions
//   private double regiontolerance = 1e-2;

   // The number of axes to take extrema along when throwing out contact points
   // 0 = disabled
   private static int numextremaaxes = 0;

   //   public long sumTime = 0;
   //   public long averageTime = 0;
   //   public long startTime = 0;
   //   public long finishTime = 0;
   //   public int runCount = 0;
   //   boolean warmup = false;

   // public static CollisionMetrics collisionMetrics = new
   // CollisionMetrics("elliot");

   public static double maxErr = 0;

   public MeshCollider() {
//      //setEpsilon (1e-12);
//      setPointTolerance (1e-4);
//      setRegionTolerance (1e-2);
   }

//   public double getEpsilon() {
//      return epsilon;
//   }
//
//   public void setEpsilon (double epsilon) {
//      this.epsilon = epsilon;
//      //intersector.setEpsilon (epsilon);
//   }

   static int iFirst = 0;
   public static int numIntNodes;

   public ContactInfo getContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {

      //long t0 = System.nanoTime();
      // collisionMetrics.totalTime -= time;
      // collisionMetrics.cullTime -= time;
      // if (iFirst++ == 0) collisionMetrics.elapsedRealTime = -time;

      mesh0.updateFaceNormals();
      mesh1.updateFaceNormals();
      BVIntersector intersector = new BVIntersector();
      ContactInfo info = new ContactInfo (mesh0, mesh1);
      //      boolean didInt =
      //         mesh0.getObbtree().intersectFully (
      //            mesh1.getObbtree(), info.intersections, intersector);
      info.myIntersections = new ArrayList<TriTriIntersection>();
      boolean didInt = 
         intersector.intersectMeshMesh (info.myIntersections, mesh0, mesh1);

      //long t1 = System.nanoTime();
      //System.out.println ("time=" + (t1-t0)*1e-3);

      // ArrayList<TriangleTriangleIntersection> check = new
      //    ArrayList<TriangleTriangleIntersection>();
      // mesh0.getObbtree().intersectFully (
      //    mesh1.getObbtree(), check, intersector);
      // if (check.size() != info.intersections.size()) {
      //    System.out.println (
      //       "NumIsects differ: "+check.size()+" vs "+info.intersections.size());
      // }
      // for (int k=0; k<check.size(); k++) {
      //    TriangleTriangleIntersection isec1 = info.intersections.get(k);
      //    TriangleTriangleIntersection isec2 = check.get(k);
      //    if ((isec1.faces[0] != isec2.faces[0]) ||
      //        (isec1.faces[1] != isec2.faces[1])) {
      //       System.out.println (
      //          "face pair "+isec1.faces[0]+"-"+isec1.faces[1]+ " differs from "+
      //          isec2.faces[0]+"-"+isec2.faces[1]);
      //    }
      //    if (isec1.points.size() != isec2.points.size()) {
      //       System.out.println (
      //          "point sizes differ: " +
      //          isec1.points.size() + " " + isec2.points.size());
      //    }
      //    for (int i=0; i<isec1.points.size(); i++) {
      //       Vector3d diff = new Vector3d();
      //       diff.sub (isec2.points.get(i), isec1.points.get(i));
      //       if (diff.norm() > maxErr) {
      //          maxErr = diff.norm();
      //       }
      //    }
      // }
      // System.out.println ("maxErr= " + maxErr);

      //startTime = System.nanoTime ();

      // time = System.nanoTime();
      // collisionMetrics.cullTime += time;
      if (!didInt) {
         // System.out.println("no intersections");
         // collisionMetrics.report(info);
         return null;
      }

//      // calculate transformations
//      RigidTransform3d trans0 = new RigidTransform3d(), trans1 =
//         new RigidTransform3d();
//
//      mesh0.getMeshToWorld (trans0);
//      mesh1.getMeshToWorld (trans1);
//      mesh0.updateFaceNormals();
//      mesh1.updateFaceNormals();
//      Point3d wpnt = new Point3d();
//
//      boolean isX0WIdentity = trans0.isIdentity ();
//      boolean isX1WIdentity = trans1.isIdentity ();

//         // calculate contact regions
//         createRegions (info.regions, info.intersections, regiontolerance);
//         // calculate contact region info
//         for (ContactRegion region : info.regions) {
//            getRegionInfo (
//               region, mesh0, mesh1, pointtolerance);
//         }
//         // collisionMetrics.femTime -= System.nanoTime();
//         // get internal vertices
//         //Point3d p = new Point3d();
//         Point3d nearest = new Point3d();
//         Vector3d disp = new Vector3d();
//         Vector2d uv = new Vector2d();
//
//         //Vector3d diff = new Vector3d();
//
//         BVFeatureQuery query = new BVFeatureQuery();
//         for (Vertex3d v : mesh0.getVertices()) {
//            // John Lloyd, Jan 3, 2014: rewrote to use isInsideOrientedMesh()
//            // to determine if a vertex is inside another mesh. Previous code
//            // would not always work and broke when the BVTree code was
//            // refactored.
//            wpnt.set (v.pnt);
//            if (!isX0WIdentity) {
//               wpnt.transform (trans0);
//            }
//            if (query.isInsideOrientedMesh (mesh1, wpnt, -1)) {
//               Face f = query.getFaceForInsideOrientedTest (nearest, uv);
//               nearest.transform (trans1);
//               disp.sub (nearest, wpnt);
//               info.myPoints0.add (
//                  new ContactPenetratingPoint (
//                     v, f, uv, nearest, disp));
//            }
//         }
//
//         for (Vertex3d v : mesh1.getVertices()) {
//            // John Lloyd, Jan 3, 2014: rewrote to use isInsideOrientedMesh()
//            // to determine if a vertex is inside another mesh. Previous code
//            // would not always work and broke when the BVTree code was
//            // refactored.
//            wpnt.set (v.pnt);
//            if (!isX1WIdentity) {
//               wpnt.transform (trans1);
//            }
//            if (query.isInsideOrientedMesh (mesh0, wpnt, -1)) {
//               Face f = query.getFaceForInsideOrientedTest (nearest, uv);
//               nearest.transform (trans0);
//               disp.sub (nearest, wpnt);
//               info.myPoints1.add (
//                  new ContactPenetratingPoint (
//                     v, f, uv, nearest, disp));
//            }
//         }
         // collisionMetrics.femTime += System.nanoTime();
//      info.myPointTol = pointtolerance;
//      info.myRegionTol = regiontolerance;

      return info;
   }

   private static void traverseRegion (
      AccelerationGrid<TriTriIntersection> accgrid,
      ContactPlane region, TriTriIntersection isect, double regionTol) {
      if (accgrid.elementidxs.containsKey (isect)) {
         accgrid.remove_element (isect);
         region.intersections.add (isect);
         ArrayList<TriTriIntersection> isects =
            new ArrayList<TriTriIntersection>();
         Point3d min = new Point3d(), max = new Point3d();
         for (Point3d p : isect.points) {
            min.set (
               p.x - regionTol / 2,
               p.y - regionTol / 2,
               p.z - regionTol / 2);
            max.set (
               p.x + regionTol / 2,
               p.y + regionTol / 2,
               p.z + regionTol / 2);
            ArrayList<TriTriIntersection> candidates =
               accgrid.find_overlapping_elements (min, max);
            // System.out.println("candidates " + candidates.size());
            for (TriTriIntersection c : candidates) {
               for (Point3d cp : c.points)
                  if (cp.greaterEquals (min) && max.greaterEquals (max)) {
                     isects.add (c);
                  }
            }
         }

         for (TriTriIntersection c : isects) {
            traverseRegion (accgrid, region, c, regionTol);
         }
      }
   }

   /**
    * From a complete set of intersections, get all of the regions so that each
    * region is defined by a set of continuous intersections.
    * 
    * @param regions (output) the complete set of intersection regions.
    * @param intersections
    * The complete set of intersections.
    * @param regionTol tolerance
    * 
    */
   static void createContactPlanes (
      ArrayList<ContactPlane> regions,
      ArrayList<TriTriIntersection> intersections, double regionTol) {
      // System.out.println("determining regions");
      // long t0 = System.currentTimeMillis();

      if (intersections.size() == 0) {
         // no intersections, so no planes to compute
         return;
      }

      AccelerationGrid<TriTriIntersection> accgrid =
         new AccelerationGrid<TriTriIntersection>();
      Point3d min = new Point3d (intersections.get (0).points[0]), max =
         new Point3d (intersections.get (0).points[0]);
      for (TriTriIntersection isect : intersections)
         for (Point3d p : isect.points) {
            min.min (p);
            max.max (p);
         }

      
      double dist = Math.max (min.distance (max), EPS);

      min.x -= dist * 0.1;
      min.y -= dist * 0.1;
      min.z -= dist * 0.1;

      max.x += dist * 0.1;
      max.y += dist * 0.1;
      max.z += dist * 0.1;

      accgrid.set (min, max, intersections.size() * 2);

      for (TriTriIntersection isect : intersections)
         for (Point3d p : isect.points)
            accgrid.add_element (isect, p, p);

      // System.out.println("cells");
      // for(LinkedHashSet<TriangleTriangleIntersection> s:accgrid.cells)
      // {
      // if(s != null)
      // {
      // System.out.println("set " + s.size());
      // }
      // }

      for (TriTriIntersection isect : intersections) {
         ContactPlane region = new ContactPlane();
         if (accgrid.elementidxs.get (isect) != null) {
            traverseRegion (accgrid, region, isect, regionTol);
            regions.add (region);
         }
      }

      // System.out.println("region time " + (System.currentTimeMillis() - t0));
   }

   /**
    * Finds the barycentric coordinates.
    */
   private static void getCoordinates (
      Vector3d coords, Point3d u0, Point3d u1, Point3d u2, Point3d v) {
      // double inv = 1.0/(u0.z*u1.y*u2.x - u0.y*u1.z*u2.x - u0.z*u1.x*u2.y +
      // u0.x*u1.z*u2.y + u0.y*u1.x*u2.z - u0.x*u1.y*u2.z);
      //    
      // System.out.println("inv det " + inv);
      // if(Double.isInfinite(inv))
      // {
      // System.out.println(u0);
      // System.out.println(u1);
      // System.out.println(u2);
      // System.out.println(v);
      // }
      //    
      // coords.x = (u1.z*u2.y*v.x - u1.y*u2.z*v.x - u1.z*u2.x*v.y +
      // u1.x*u2.z*v.y + u1.y*u2.x*v.z - u1.x*u2.y*v.z)*inv;
      // coords.y = (u0.z*u2.y*v.x - u0.y*u2.z*v.x - u0.z*u2.x*v.y +
      // u0.x*u2.z*v.y + u0.y*u2.x*v.z - u0.x*u2.y*v.z)*inv;
      // coords.z = (u0.z*u1.y*v.x - u0.y*u1.z*v.x - u0.z*u1.x*v.y +
      // u0.x*u1.z*v.y + u0.y*u1.x*v.z - u0.x*u1.y*v.z)*inv;

      Vector3d edge0 = new Vector3d(), edge1 = new Vector3d();
      edge0.sub (u1, u0);
      edge1.sub (u2, u0);
      edge0.cross (edge1);
      double tarea = edge0.norm();

      edge0.sub (u1, v);
      edge1.sub (u2, v);
      edge0.cross (edge1);
      coords.x = edge0.norm() / tarea;

      edge0.sub (u2, v);
      edge1.sub (u0, v);
      edge0.cross (edge1);
      coords.y = edge0.norm() / tarea;

      edge0.sub (u0, v);
      edge1.sub (u1, v);
      edge0.cross (edge1);
      coords.z = edge0.norm() / tarea;

      // System.out.println((coords.x + coords.y + coords.z));
   }

   private static Vector3d edgeEdgeNormal (
      RigidTransform3d trans0, RigidTransform3d trans1, Face face0, Face face1,
      int e0, int e1) {
      Vector3d edge0 = new Vector3d();
      Vector3d edge1 = new Vector3d();
      edge0.sub (face0.getVertex ((e0 + 1) % 3).pnt, face0.getVertex (e0).pnt);
      edge1.sub (face1.getVertex ((e1 + 1) % 3).pnt, face1.getVertex (e1).pnt);

      edge0.transform (trans0);
      edge1.transform (trans1);

      Vector3d normal = new Vector3d();
      normal.cross (edge0, edge1);
      normal.normalize();

      edge0.sub (face0.getVertex ((e0 + 2) % 3).pnt,
         face0.getVertex ((e0 + 1) % 3).pnt);
      edge0.transform (trans0);

      if (edge0.dot (normal) < 0)
         normal.negate();

      return normal;
   }

   private static Vector3d edgeFaceNormal (
      RigidTransform3d trans0, RigidTransform3d trans1, Face face0, Face face1,
      int e) {
      Vector3d normal = new Vector3d (face1.getNormal());
      normal.transform (trans1);

      return normal;
   }

   private static Vector3d vertexEdgeNormal (
      RigidTransform3d trans0, RigidTransform3d trans1, Face face0, Face face1,
      int v, int e) {
      // use the vertex normal orthogonalized to the edge
      // Vector3d edge = new Vector3d();
      // edge.sub(face1.getVertex((e+1)%3).pnt, face1.getVertex(e).pnt);
      // edge.transform(trans1);
      //    
      // Vector3d normal = new Vector3d();
      // face0.getVertex(v).computeNormal(normal);
      // normal.transform(trans0);
      // normal.negate();
      //    
      // normal.scaledAdd(-normal.dot(edge), edge);
      // normal.normalize();

      // use the normal associated with the edge
      Vector3d normal = new Vector3d();
      normal.transform (trans1, face1.getNormal());

      return normal;
   }

   private static Vector3d vertexFaceNormal (
      RigidTransform3d trans0, RigidTransform3d trans1, Face face0, Face face1,
      int v) {
      Vector3d normal = new Vector3d (face1.getNormal());
      normal.transform (trans1);

      return normal;
   }

   private static Vector3d vertexVertexNormal (
      RigidTransform3d trans0, RigidTransform3d trans1, Face face0, Face face1,
      int v0, int v1) {
      Vector3d normal = new Vector3d(), tmp = new Vector3d();

      face0.getVertex (v0).computeNormal (tmp);
      tmp.transform (trans0);

      face1.getVertex (v1).computeNormal (normal);
      normal.transform (trans1);

      normal.sub (tmp);
      normal.normalize();

      return normal;
   }

   private static Vector3d faceFaceNormal (
      RigidTransform3d trans0, RigidTransform3d trans1, Face face0, Face face1) {
      Vector3d normal = new Vector3d();
      normal.transform (trans1, face1.getNormal());

      Vector3d tmp = new Vector3d();
      tmp.transform (trans0, face0.getNormal());

      tmp.sub (normal, tmp);
      tmp.normalize();

      if (tmp.containsNaN())
         return normal;

      return tmp;
   }

   private static int[] classifyPoint (Vector3d coords) {
      int[] type = new int[2];

      if (coords.x < EPS) {
         if (coords.y < EPS) {
            // coords.x=0,coords.y=0
            type[0] = 2;
            type[1] = 2;
         }
         else {
            if (coords.z < EPS) {
               // coords.x=0,coords.z=0
               type[0] = 2;
               type[1] = 1;
            }
            else {
               // coords.x=0
               type[0] = 1;
               type[1] = 1;
            }
         }
      }
      else {
         if (coords.y < EPS) {
            if (coords.z < EPS) {
               // coords.y=0,coords.z=0
               type[0] = 2;
               type[1] = 0;
            }
            else {
               // coords.y=0
               type[0] = 1;
               type[1] = 2;
            }
         }
         else {
            if (coords.z < EPS) {
               // coords.z=0
               type[0] = 1;
               type[1] = 0;
            }
            else {
               type[0] = 0;
            }
         }
      }

      return type;
   }

   /**
    * Get information about a specific region of intersections.
    */
    static void getContactPlaneInfo (
      ContactPlane region, PolygonalMesh mesh0, PolygonalMesh mesh1, 
      double pointTol) {
      Vector3d sectnormal = new Vector3d(), tmpnormal = new Vector3d();
      
      BVFeatureQuery query = new BVFeatureQuery();
      RigidTransform3d trans0 = mesh0.getMeshToWorld();
      RigidTransform3d trans1 = mesh1.getMeshToWorld();

      // calculate a weighted average of the face normals
      for (TriTriIntersection isect : region.intersections) {
         region.points = new ArrayList<Point3d>();
         region.points.add(isect.points[0]);
         region.points.add(isect.points[1]);
         double length = isect.points[0].distance (isect.points[1]);

         tmpnormal.transform (trans0, isect.face0.getNormal());
         tmpnormal.negate();
         sectnormal.scaledAdd (length, tmpnormal, sectnormal);

         tmpnormal.transform (trans1, isect.face1.getNormal());
         sectnormal.scaledAdd (length, tmpnormal, sectnormal);
      }

      // calculate the weighted intersection center
      Point3d center = new Point3d();
      double weight = 0;
      for (TriTriIntersection isect : region.intersections) {
         double length = isect.points[0].distance (isect.points[1]);

         center.scaledAdd (length, isect.points[0], center);
         center.scaledAdd (length, isect.points[1], center);

         weight += 2 * length;
      }
      center.scale (1.0 / weight);
      region.centroid = center;

      // calculate the weighted normal
      Vector3d cp0 = new Vector3d(), cp1 = new Vector3d();
      region.normal.setZero();
      for (TriTriIntersection isect : region.intersections) {
         cp0.sub (isect.points[0], center);
         cp1.sub (isect.points[1], center);

         tmpnormal.cross (cp0, cp1);
         if (tmpnormal.dot (sectnormal) < 0)
            tmpnormal.negate();

         region.normal.add (tmpnormal);
      }

      if (region.normal.dot (sectnormal) < 0)
         region.normal.negate();

      // handle degenerate cases
      if (region.normal.containsNaN() || region.normal.norm() < EPS) {
         region.normal.setZero();

         Point3d p0 = new Point3d();
         Point3d p1 = new Point3d();
         Vector3d c0 = new Vector3d();
         Vector3d c1 = new Vector3d();

         for (TriTriIntersection isect : region.intersections) {
            for (Point3d p : isect.points) {
               p0.inverseTransform (trans0, p);
               p1.inverseTransform (trans1, p);
               Vertex3d u0 = isect.face0.getVertex (0);
               Vertex3d u1 = isect.face0.getVertex (1);
               Vertex3d u2 = isect.face0.getVertex (2);
               Vertex3d v0 = isect.face1.getVertex (0);
               Vertex3d v1 = isect.face1.getVertex (1);
               Vertex3d v2 = isect.face1.getVertex (2);
               getCoordinates (c0, u0.pnt, u1.pnt, u2.pnt, p0);
               getCoordinates (c1, v0.pnt, v1.pnt, v2.pnt, p1);

               int[] type0 = classifyPoint (c0);
               int[] type1 = classifyPoint (c1);

               // [0] = 0 = face, 1 = edge, 2 = vertex
               // [1] = idx

               if (type0[0] == 2) {
                  if (type1[0] == 2) {
                     // vertex,vertex
                     region.normal.add (vertexVertexNormal (
                        trans0, trans1, isect.face0, isect.face1,
                        type0[1], type1[1]));
                  }
                  else if (type1[0] == 1) {
                     // vertex,edge
                     region.normal.add (vertexEdgeNormal (
                        trans0, trans1, isect.face0, isect.face1,
                        type0[1], type1[1]));
                  }
                  else {
                     // vertex,face
                     region.normal.add (vertexFaceNormal (
                        trans0, trans1, isect.face0, isect.face1,
                        type0[1]));
                  }
               }
               else if (type0[0] == 1) {
                  if (type1[0] == 2) {
                     // edge,vertex
                     region.normal.sub (vertexEdgeNormal (
                        trans1, trans0, isect.face1, isect.face0,
                        type1[1], type0[1]));
                  }
                  else if (type1[0] == 1) {
                     // edge,edge
                     region.normal.add (edgeEdgeNormal (
                        trans0, trans1, isect.face0, isect.face1,
                        type0[1], type1[1]));
                  }
                  else {
                     // edge,face
                     region.normal.add (edgeFaceNormal (
                        trans0, trans1, isect.face0, isect.face1,
                        type0[1]));
                  }
               }
               else {
                  if (type1[0] == 2) {
                     // face,vertex
                     region.normal.sub (vertexFaceNormal (
                        trans1, trans0, isect.face1, isect.face0,
                        type1[1]));
                  }
                  else if (type1[0] == 1) {
                     // face,edge
                     region.normal.sub (edgeFaceNormal (
                        trans1, trans0, isect.face1, isect.face0,
                        type1[1]));
                  }
                  else {
                     // face,face
                     region.normal.add (faceFaceNormal (
                        trans0, trans1, isect.face0, isect.face1));
                  }
               }
            }
         }
      }

      region.normal.normalize();

      // calculate the contact depth for the region
      boolean foundPenetratingVertice = false;
      Point3d p = new Point3d();
      Point3d nearest = new Point3d();
      Vector3d diff = new Vector3d();
      Vector2d coords = new Vector2d();
      Vertex3d v;
      Face nf;
      Point3d plocal = new Point3d();

      LinkedHashSet<Vertex3d> regionvertices0 = new LinkedHashSet<Vertex3d>();
      LinkedHashSet<Vertex3d> regionvertices1 = new LinkedHashSet<Vertex3d>();

      region.depth = 0;
      for (TriTriIntersection isect : region.intersections) {
         for (int i = 0; i < 3; i++) {
            // face0 vertex depths
            v = isect.face0.getVertex (i);

            p.transform (trans0, v.pnt);
            plocal.inverseTransform (trans1, p);
            plocal.sub (isect.face1.getVertex (0).pnt);
            if (plocal.dot (isect.face1.getNormal()) <= 0) {
               regionvertices0.add (v);
            }

            // face1 vertex depths
            v = isect.face1.getVertex (i);

            p.transform (trans1, v.pnt);
            plocal.inverseTransform (trans0, p);
            plocal.sub (isect.face0.getVertex (0).pnt);
            if (plocal.dot (isect.face0.getNormal()) <= 0) {
               regionvertices1.add (v);
            }
         }
      }

      for (Vertex3d v0 : regionvertices0) {
         p.transform (trans0, v0.pnt);
         // XXX Sanchez, Jun 22, 2014
         // Changed to isInside.  Sometimes a vertex is outside
         // the mesh but determined to be "penetrating" due to
         // normal (e.g. when nearest to an edge)
         // nf = myQuery.nearestFaceToPoint (nearest, coords, mesh1, p);
         boolean inside = query.isInsideOrientedMesh(mesh1, p, 0);

         if (inside) {
            query.getFaceForInsideOrientedTest(nearest, coords);
            nearest.transform(trans1);
            diff.sub (p, nearest);
            diff.inverseTransform (trans1);
            foundPenetratingVertice = true;

            double dist = diff.norm();//-diff.dot (nf.getNormal());
            if (dist > region.depth)
               region.depth = dist;
         }
      }

      for (Vertex3d v1 : regionvertices1) {
         p.transform (trans1, v1.pnt);
         //nf = myQuery.nearestFaceToPoint (nearest, coords, mesh0, p);
         boolean inside = query.isInsideOrientedMesh(mesh0, p, 0);

         if (inside) {
            query.getFaceForInsideOrientedTest(nearest, coords);
            nearest.transform(trans0);
            diff.sub (p, nearest);
            diff.inverseTransform (trans0);

            foundPenetratingVertice = true;
            double dist = diff.norm();//-diff.dot (nf.getNormal());
            if (dist > region.depth)
               region.depth = dist;
         }
      }

      if (!foundPenetratingVertice) {
         double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;

         for (int i = 0; i < region.points.size(); i++) {
            double d = region.points.get (i).dot (region.normal);

            if (d < min)
               min = d;
            if (d > max)
               max = d;
         }

         region.depth = max - min;
      }

      // eliminate redundant points
      // use point tolerance
      region.points.clear();
      for (TriTriIntersection isect : region.intersections) {
         for (Point3d pcandidate : isect.points) {
            boolean add = true;
            for (Point3d other : region.points)
               if (pcandidate.epsilonEquals (other, pointTol)) {
                  add = false;
                  break;
               }
            if (add) {
               region.points.add (pcandidate);
            }
         }
      }

      // take extrema along n axes
      if (numextremaaxes > 0) {
         // final ArrayList<Vector3d> axes = new ArrayList<Vector3d>();

         Vector3d crosszup = new Vector3d (0, 0, 1);
         crosszup.cross (region.normal, crosszup);
         double crosszupnorm = crosszup.norm();

         RigidTransform3d normtoworld;
         if (crosszup.norm() > EPS) {
            normtoworld =
            new RigidTransform3d (new Vector3d(), new AxisAngle (
               crosszup, Math.asin (crosszupnorm)));
         }
         else {
            normtoworld = new RigidTransform3d();
         }

         boolean[] keep = new boolean[region.points.size()];
         for (int j = 0; j < region.points.size(); j++)
            keep[j] = false;

         Vector3d offset = new Vector3d();
         Vector3d axis = new Vector3d();

         for (int i = 0; i < numextremaaxes; i++) {
            double min = Double.POSITIVE_INFINITY, max =
               Double.NEGATIVE_INFINITY;
            int mini = 0, maxi = 0;
            double angle = Math.PI * i / numextremaaxes;
            axis.set (Math.cos (angle), Math.sin (angle), 0);
            axis.transform (normtoworld);

            // axes.add(new Vector3d(axis));

            for (int j = 0; j < region.points.size(); j++) {
               offset.sub (region.points.get (j), center);
               double dot = offset.dot (axis);
               if (dot < min) {
                  min = dot;
                  mini = j;
               }
               if (dot > max) {
                  max = dot;
                  maxi = j;
               }
            }

            keep[mini] = true;
            keep[maxi] = true;
         }

         for (int j = (region.points.size() - 1); j >= 0; j--) {
            if (!keep[j])
               region.points.remove (j);
         }
      }
   }

//   public double getPointTolerance() {
//      return pointtolerance;
//   }
//
//   public void setPointTolerance (double tolerance) {
//      this.pointtolerance = tolerance;
//   }
//
//   public double getRegionTolerance() {
//      return regiontolerance;
//   }
//
//   public void setRegionTolerance (double regiontolerance) {
//      this.regiontolerance = regiontolerance;
//   }

   /**
    * See setter.
    */
   public static int getNumextremaaxes() {
      return numextremaaxes;
   }

   /**
    * Set the number of axes along which to take the extrema points when
    * throwing out redundant contact points. 0 = disabled.
    * 
    * @param numaxes number of axes
    */
   public static void setNumextremaaxes (int numaxes) {
      numextremaaxes = numaxes;
   }
}
