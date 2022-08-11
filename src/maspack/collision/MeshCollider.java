package maspack.collision;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import maspack.geometry.BVFeatureQuery;
import maspack.geometry.BVIntersector;
import maspack.geometry.ConvexPolygon3d;
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

   private static final double EPS = 1e-12;

   // The number of axes to take extrema along when throwing out contact points
   // 0 = disabled
   private static int numextremaaxes = 0;

   public static double maxErr = 0;

   public MeshCollider() {
   }

   static int iFirst = 0;
   public static int numIntNodes;

   public ContactInfo getContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {


      mesh0.updateFaceNormals();
      mesh1.updateFaceNormals();
      BVIntersector intersector = new BVIntersector();
      ContactInfo info = new ContactInfo (mesh0, mesh1);
      info.myIntersections = new ArrayList<TriTriIntersection>();
      boolean didInt = 
         intersector.intersectMeshMesh (info.myIntersections, mesh0, mesh1);
      if (!didInt) {
         return null;
      }
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

      for (TriTriIntersection isect : intersections) {
         ContactPlane region = new ContactPlane();
         if (accgrid.elementidxs.get (isect) != null) {
            traverseRegion (accgrid, region, isect, regionTol);
            regions.add (region);
         }
      }
   }

   /**
    * Finds the barycentric coordinates.
    */
   private static void getCoordinates (
      Vector3d coords, Point3d u0, Point3d u1, Point3d u2, Point3d v) {

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
      else {
         // reduce contact points using convex hull
         if (region.points.size() >= 3) {
            int[] idxs = ConvexPolygon3d.computeHullIndices (
               region.points, region.normal, /*angTol=*/1e-8);
            if (idxs.length < region.points.size()) {
               // hull is small, so replace region.points with hull
               ArrayList<Point3d> newpoints = new ArrayList<>(idxs.length);
               for (int i : idxs) {
                  newpoints.add (region.points.get(i));
               }
               region.points = newpoints;
            }
         }
      }
   }

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
