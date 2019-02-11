package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;

public class TriangleIntersector {
   private double epsilon = 1e-24;

   public void setEpsilon (double e) {
      epsilon = e;
   }

   public double getEpsilon() {
      return epsilon;
   }

   // triangle-triangle intersection code

   /*
    * 
    * Triangle-Triangle Overlap Test Routines July, 2002 Updated December 2003
    * 
    * This file contains C implementation of algorithms for performing two and
    * three-dimensional triangle-triangle intersection test The algorithms and
    * underlying theory are described in
    * 
    * "Fast and Robust Triangle-Triangle Overlap Test Using Orientation
    * Predicates" P. Guigue - O. Devillers
    * 
    * Journal of Graphics Tools, 8(1), 2003
    * 
    * Several geometric predicates are defined. Their parameters are all points.
    * Each point is an array of two or three double precision floating point
    * numbers. The geometric predicates implemented in this file are:
    * 
    * int tri_tri_overlap_test_3d(p1,q1,r1,p2,q2,r2) int
    * tri_tri_overlap_test_2d(p1,q1,r1,p2,q2,r2)
    * 
    * int tri_tri_intersection_test_3d(p1,q1,r1,p2,q2,r2,
    * coplanar,source,target)
    * 
    * is a version that computes the segment of intersection when the triangles
    * overlap (and are not coplanar)
    * 
    * each function returns 1 if the triangles (including their boundary)
    * intersect, otherwise 0
    * 
    * 
    * Other information are available from the Web page 
    * http://jgt.akpeters.com/papers/GuigueDevillers03/
    * 
    */

   int CHECK_MIN_MAX (
      Vector3d p1, Vector3d q1, Vector3d r1, Vector3d p2, Vector3d q2,
      Vector3d r2) {
      v1.sub (p2, q1);
      v2.sub (p1, q1);
      N1.sub (v1, v2);
      v1.sub (q2, q1);
      if (v1.dot (N1) > 0.0)
         return 0;
      v1.sub (p2, p1);
      v2.sub (r1, p1);
      N1.sub (v1, v2);
      v1.sub (r2, p1);
      if (v1.dot (N1) > 0.0)
         return 0;
      else
         return 1;
   }

   /* Permutation in a canonical form of T2's vertices */

   Vector2d P1 = new Vector2d(), Q1 = new Vector2d(), R1 = new Vector2d();
   Vector2d P2 = new Vector2d(), Q2 = new Vector2d(), R2 = new Vector2d();

   int coplanar_tri_tri3d (
      Vector3d p1, Vector3d q1, Vector3d r1, Vector3d p2, Vector3d q2,
      Vector3d r2, Vector3d normal_1, Vector3d normal_2) {

      double n_x, n_y, n_z;

      n_x = ((normal_1.x < 0) ? -normal_1.x : normal_1.x);
      n_y = ((normal_1.y < 0) ? -normal_1.y : normal_1.y);
      n_z = ((normal_1.z < 0) ? -normal_1.z : normal_1.z);

      /*
       * Projection of the triangles in 3D onto 2D such that the area of the
       * projection is maximized.
       */

      if ((n_x > n_z) && (n_x >= n_y)) {
         // Project onto plane YZ

         P1.x = q1.z;
         P1.y = q1.y;
         Q1.x = p1.z;
         Q1.y = p1.y;
         R1.x = r1.z;
         R1.y = r1.y;

         P2.x = q2.z;
         P2.y = q2.y;
         Q2.x = p2.z;
         Q2.y = p2.y;
         R2.x = r2.z;
         R2.y = r2.y;

      }
      else if ((n_y > n_z) && (n_y >= n_x)) {
         // Project onto plane XZ

         P1.x = q1.x;
         P1.y = q1.z;
         Q1.x = p1.x;
         Q1.y = p1.z;
         R1.x = r1.x;
         R1.y = r1.z;

         P2.x = q2.x;
         P2.y = q2.z;
         Q2.x = p2.x;
         Q2.y = p2.z;
         R2.x = r2.x;
         R2.y = r2.z;

      }
      else {
         // Project onto plane XY

         P1.x = p1.x;
         P1.y = p1.y;
         Q1.x = q1.x;
         Q1.y = q1.y;
         R1.x = r1.x;
         R1.y = r1.y;

         P2.x = p2.x;
         P2.y = p2.y;
         Q2.x = q2.x;
         Q2.y = q2.y;
         R2.x = r2.x;
         R2.y = r2.y;
      }

      return tri_tri_overlap_test_2d (P1, Q1, R1, P2, Q2, R2);

   };

   /*
    * 
    * Three-dimensional Triangle-Triangle Intersection
    * 
    */

   /*
    * This macro is called when the triangles surely intersect It constructs the
    * segment of intersection of the two triangles if they are not coplanar.
    */

   Point3d[] CONSTRUCT_INTERSECTION (
      Vector3d p1, Vector3d q1, Vector3d r1, Vector3d p2, Vector3d q2,
      Vector3d r2) {

      // any normal to plane P1
      v1.sub (q1, p1);
      v2.sub (r1, p1);
      N1.cross (v1, v2);
      
      // any normal to plane P2
      v1.sub (p2, r2);
      v2.sub (q2, r2);
      N2.cross (v1, v2);
      
      v1.sub (q1, p1);
      v2.sub (r2, p1);
      N.cross (v1, v2);
      
      v.sub (p2, p1);
      if (v.dot (N) > 0.0) {
         v1.sub (r1, p1);
         N.cross (v1, v2);
         if (v.dot (N) <= 0.0) {
            Point3d[] pts = new Point3d[2];
            Point3d source = new Point3d();
            Point3d target = new Point3d();

            v2.sub (q2, p1);
            N.cross (v1, v2);
            if (v.dot (N) > 0.0) {
               v1.sub (p1, p2);
               v2.sub (p1, r1);
               alpha = v1.dot (N2) / v2.dot (N2);
               v1.scale (alpha, v2);
               source.sub (p1, v1);
               v1.sub (p2, p1);
               v2.sub (p2, r2);
               alpha = v1.dot (N1) / v2.dot (N1);
               v1.scale (alpha, v2);
               target.sub (p2, v1);
               // return 1;
            }
            else {
               v1.sub (p2, p1);
               v2.sub (p2, q2);
               alpha = v1.dot (N1) / v2.dot (N1);
               v1.scale (alpha, v2);
               source.sub (p2, v1);
               v1.sub (p2, p1);
               v2.sub (p2, r2);
               alpha = v1.dot (N1) / v2.dot (N1);
               v1.scale (alpha, v2);
               target.sub (p2, v1);
               // return 1;
            }

            pts[0] = source;
            pts[1] = target;
            return pts;
         }
         else {
            return null;
         }
      }
      else {
         v2.sub (q2, p1);
         N.cross (v1, v2);
         if (v.dot (N) < 0.0) {
            return null;
         }
         else {
            Point3d[] pts = new Point3d[2];
            Point3d source = new Point3d();
            Point3d target = new Point3d();

            v1.sub (r1, p1);
            N.cross (v1, v2);
            if (v.dot (N) >= 0.0) {
               v1.sub (p1, p2);
               v2.sub (p1, r1);
               alpha = v1.dot (N2) / v2.dot (N2);
               v1.scale (alpha, v2);
               source.sub (p1, v1);
               v1.sub (p1, p2);
               v2.sub (p1, q1);
               alpha = v1.dot (N2) / v2.dot (N2);
               v1.scale (alpha, v2);
               target.sub (p1, v1);
               // return 1;
            }
            else {
               v1.sub (p2, p1);
               v2.sub (p2, q2);
               alpha = v1.dot (N1) / v2.dot (N1);
               v1.scale (alpha, v2);
               source.sub (p2, v1);
               v1.sub (p1, p2);
               v2.sub (p1, q1);
               alpha = v1.dot (N2) / v2.dot (N2);
               v1.scale (alpha, v2);
               target.sub (p1, v1);
               // return 1;
            }

            pts[0] = source;
            pts[1] = target;
            return pts;
         }
      }
   }

   Point3d[] TRI_TRI_INTER_3D (
      Vector3d p1, Vector3d q1, Vector3d r1, Vector3d p2, Vector3d q2,
      Vector3d r2, double dp2, double dq2, double dr2) {
      if (dp2 > 0.0) {
         if (dq2 > 0.0)
            return CONSTRUCT_INTERSECTION (p1, r1, q1, r2, p2, q2);
         else if (dr2 > 0.0)
            return CONSTRUCT_INTERSECTION (p1, r1, q1, q2, r2, p2);
         else
            return CONSTRUCT_INTERSECTION (p1, q1, r1, p2, q2, r2);
      }
      else if (dp2 < 0.0) {
         if (dq2 < 0.0)
            return CONSTRUCT_INTERSECTION (p1, q1, r1, r2, p2, q2);
         else if (dr2 < 0.0)
            return CONSTRUCT_INTERSECTION (p1, q1, r1, q2, r2, p2);
         else
            return CONSTRUCT_INTERSECTION (p1, r1, q1, p2, q2, r2);
      }
      else {
         if (dq2 < 0.0) {
            if (dr2 >= 0.0)
               return CONSTRUCT_INTERSECTION (p1, r1, q1, q2, r2, p2);
            else
               return CONSTRUCT_INTERSECTION (p1, q1, r1, p2, q2, r2);
         }
         else if (dq2 > 0.0) {
            if (dr2 > 0.0)
               return CONSTRUCT_INTERSECTION (p1, r1, q1, p2, q2, r2);
            else
               return CONSTRUCT_INTERSECTION (p1, q1, r1, q2, r2, p2);
         }
         else {
            if (dr2 > 0.0)
               return CONSTRUCT_INTERSECTION (p1, q1, r1, r2, p2, q2);
            else if (dr2 < 0.0)
               return CONSTRUCT_INTERSECTION (p1, r1, q1, r2, p2, q2);
            else {
               // coplanar.value = 1;
               return null;// coplanar_tri_tri3d (p1, q1, r1, p2, q2, r2, N1,
                           // N2);
            }
         }
      }
   }

   /*
    * The following version computes the segment of intersection of the two
    * triangles if it exists. coplanar returns whether the triangles are
    * coplanar source and target are the endpoints of the line segment of
    * intersection
    */

   double dp1, dq1, dr1, dp2, dq2, dr2;
   Vector3d v1 = new Vector3d(), v2 = new Vector3d(), v = new Vector3d();
   Vector3d N1 = new Vector3d(), N2 = new Vector3d(), N = new Vector3d();
   double alpha;

   // Vector3d source, target;

   public Point3d[] intersectTriangleTriangle (
      Vector3d p1, Vector3d q1, Vector3d r1, Vector3d p2, Vector3d q2,
      Vector3d r2) {
      // MeshCollider.cm.primInts++;
      // Compute distance signs of p1, q1 and r1
      // to the plane of triangle(p2,q2,r2)

      v1.sub (p2, r2);
      v2.sub (q2, r2);
      N2.cross (v1, v2);

      v1.sub (p1, r2);
      dp1 = v1.dot (N2);
      v1.sub (q1, r2);
      dq1 = v1.dot (N2);
      v1.sub (r1, r2);
      dr1 = v1.dot (N2);

      // //epsilon test
      // if(dp1 < epsilon && -dp1 < epsilon)
      // dp1 = 0;
      // if(dq1 < epsilon && -dq1 < epsilon)
      // dq1 = 0;
      // if(dr1 < epsilon && -dr1 < epsilon)
      // dr1 = 0;

      if (((dp1 * dq1) > 0.0) && ((dp1 * dr1) > 0.0))
         return null;

      // Compute distance signs of p2, q2 and r2
      // to the plane of triangle(p1,q1,r1)

      v1.sub (q1, p1);
      v2.sub (r1, p1);
      N1.cross (v1, v2);

      v1.sub (p2, r1);
      dp2 = v1.dot (N1);
      v1.sub (q2, r1);
      dq2 = v1.dot (N1);
      v1.sub (r2, r1);
      dr2 = v1.dot (N1);

      // //epsilon test
      // if(dp2 < epsilon && -dp2 < epsilon)
      // dp2 = 0;
      // if(dq2 < epsilon && -dq2 < epsilon)
      // dq2 = 0;
      // if(dr2 < epsilon && -dr2 < epsilon)
      // dr2 = 0;

      if (((dp2 * dq2) > 0.0) && ((dp2 * dr2) > 0.0))
         return null;

      // System.out.println(dp1 + " " + dq1 + " " + dr1);
      // System.out.println(dp2 + " " + dq2 + " " + dr2);

      // Permutation in a canonical form of T1's vertices

      if (dp1 > 0.0) {
         if (dq1 > 0.0)
            return TRI_TRI_INTER_3D (r1, p1, q1, p2, r2, q2, dp2, dr2, dq2);
         else if (dr1 > 0.0)
            return TRI_TRI_INTER_3D (q1, r1, p1, p2, r2, q2, dp2, dr2, dq2);

         else
            return TRI_TRI_INTER_3D (p1, q1, r1, p2, q2, r2, dp2, dq2, dr2);
      }
      else if (dp1 < 0.0) {
         if (dq1 < 0.0)
            return TRI_TRI_INTER_3D (r1, p1, q1, p2, q2, r2, dp2, dq2, dr2);
         else if (dr1 < 0.0)
            return TRI_TRI_INTER_3D (q1, r1, p1, p2, q2, r2, dp2, dq2, dr2);
         else
            return TRI_TRI_INTER_3D (p1, q1, r1, p2, r2, q2, dp2, dr2, dq2);
      }
      else {
         if (dq1 < 0.0) {
            if (dr1 >= 0.0)
               return TRI_TRI_INTER_3D (q1, r1, p1, p2, r2, q2, dp2, dr2, dq2);
            else
               return TRI_TRI_INTER_3D (p1, q1, r1, p2, q2, r2, dp2, dq2, dr2);
         }
         else if (dq1 > 0.0) {
            if (dr1 > 0.0)
               return TRI_TRI_INTER_3D (p1, q1, r1, p2, r2, q2, dp2, dr2, dq2);
            else
               return TRI_TRI_INTER_3D (q1, r1, p1, p2, q2, r2, dp2, dq2, dr2);
         }
         else {
            if (dr1 > 0.0)
               return TRI_TRI_INTER_3D (r1, p1, q1, p2, q2, r2, dp2, dq2, dr2);
            else if (dr1 < 0.0)
               return TRI_TRI_INTER_3D (r1, p1, q1, p2, r2, q2, dp2, dr2, dq2);
            else {
               // triangles are co-planar

               // coplanar.value = 1;
               return null;// coplanar_tri_tri3d (p1, q1, r1, p2, q2, r2, N1,
                           // N2);
            }
         }
      }
   };

   /*
    * 
    * Two dimensional Triangle-Triangle Overlap Test
    * 
    */

   /* some 2D macros */

   double ORIENT_2D (Vector2d a, Vector2d b, Vector2d c) {
      return ((a.x - c.x) * (b.y - c.y) - (a.y - c.y) * (b.x - c.x));
   }

   int INTERSECTION_TEST_VERTEX (
      Vector2d P1, Vector2d Q1, Vector2d R1, Vector2d P2, Vector2d Q2,
      Vector2d R2) {
      if (ORIENT_2D (R2, P2, Q1) >= 0.0)
         if (ORIENT_2D (R2, Q2, Q1) <= 0.0)
            if (ORIENT_2D (P1, P2, Q1) > 0.0) {
               if (ORIENT_2D (P1, Q2, Q1) <= 0.0)
                  return 1;
               else
                  return 0;
            }
            else {
               if (ORIENT_2D (P1, P2, R1) >= 0.0)
                  if (ORIENT_2D (Q1, R1, P2) >= 0.0)
                     return 1;
                  else
                     return 0;
               else
                  return 0;
            }
         else if (ORIENT_2D (P1, Q2, Q1) <= 0.0)
            if (ORIENT_2D (R2, Q2, R1) <= 0.0)
               if (ORIENT_2D (Q1, R1, Q2) >= 0.0)
                  return 1;
               else
                  return 0;
            else
               return 0;
         else
            return 0;
      else if (ORIENT_2D (R2, P2, R1) >= 0.0)
         if (ORIENT_2D (Q1, R1, R2) >= 0.0)
            if (ORIENT_2D (P1, P2, R1) >= 0.0)
               return 1;
            else
               return 0;
         else if (ORIENT_2D (Q1, R1, Q2) >= 0.0) {
            if (ORIENT_2D (R2, R1, Q2) >= 0.0)
               return 1;
            else
               return 0;
         }
         else
            return 0;
      else
         return 0;
   };

   int INTERSECTION_TEST_EDGE (
      Vector2d P1, Vector2d Q1, Vector2d R1, Vector2d P2, Vector2d Q2,
      Vector2d R2) {
      if (ORIENT_2D (R2, P2, Q1) >= 0.0) {
         if (ORIENT_2D (P1, P2, Q1) >= 0.0) {
            if (ORIENT_2D (P1, Q1, R2) >= 0.0)
               return 1;
            else
               return 0;
         }
         else {
            if (ORIENT_2D (Q1, R1, P2) >= 0.0) {
               if (ORIENT_2D (R1, P1, P2) >= 0.0)
                  return 1;
               else
                  return 0;
            }
            else
               return 0;
         }
      }
      else {
         if (ORIENT_2D (R2, P2, R1) >= 0.0) {
            if (ORIENT_2D (P1, P2, R1) >= 0.0) {
               if (ORIENT_2D (P1, R1, R2) >= 0.0)
                  return 1;
               else {
                  if (ORIENT_2D (Q1, R1, R2) >= 0.0)
                     return 1;
                  else
                     return 0;
               }
            }
            else
               return 0;
         }
         else
            return 0;
      }
   }

   int ccw_tri_tri_intersection_2d (
      Vector2d p1, Vector2d q1, Vector2d r1, Vector2d p2, Vector2d q2,
      Vector2d r2) {
      if (ORIENT_2D (p2, q2, p1) >= 0.0) {
         if (ORIENT_2D (q2, r2, p1) >= 0.0) {
            if (ORIENT_2D (r2, p2, p1) >= 0.0)
               return 1;
            else
               return INTERSECTION_TEST_EDGE (p1, q1, r1, p2, q2, r2);
         }
         else {
            if (ORIENT_2D (r2, p2, p1) >= 0.0)
               return INTERSECTION_TEST_EDGE (p1, q1, r1, r2, p2, q2);
            else
               return INTERSECTION_TEST_VERTEX (p1, q1, r1, p2, q2, r2);
         }
      }
      else {
         if (ORIENT_2D (q2, r2, p1) >= 0.0) {
            if (ORIENT_2D (r2, p2, p1) >= 0.0)
               return INTERSECTION_TEST_EDGE (p1, q1, r1, q2, r2, p2);
            else
               return INTERSECTION_TEST_VERTEX (p1, q1, r1, q2, r2, p2);
         }
         else
            return INTERSECTION_TEST_VERTEX (p1, q1, r1, r2, p2, q2);
      }
   };

   int tri_tri_overlap_test_2d (
      Vector2d p1, Vector2d q1, Vector2d r1, Vector2d p2, Vector2d q2,
      Vector2d r2) {
      if (ORIENT_2D (p1, q1, r1) < 0.0)
         if (ORIENT_2D (p2, q2, r2) < 0.0)
            return ccw_tri_tri_intersection_2d (p1, r1, q1, p2, r2, q2);
         else
            return ccw_tri_tri_intersection_2d (p1, r1, q1, p2, q2, r2);
      else if (ORIENT_2D (p2, q2, r2) < 0.0)
         return ccw_tri_tri_intersection_2d (p1, q1, r1, p2, r2, q2);
      else
         return ccw_tri_tri_intersection_2d (p1, q1, r1, p2, q2, r2);
   };

   // triangle-ray intersection code
   // http://jgt.akpeters.com/papers/MollerTrumbore97/code.html

   Vector3d edge0 = new Vector3d(), edge1 = new Vector3d(),
   tvec = new Vector3d(), pvec = new Vector3d(), qvec = new Vector3d();
   double det, inv_det;

   /**
    * Determines the bary centric coordinates of a ray hitting a triangle.
    * 
    * @param v0
    * The first vertex.
    * @param v1
    * The second vertex.
    * @param v2
    * The third vertex.
    * @param pos
    * The ray's origin.
    * @param dir
    * The ray's direction.
    * @param duv
    * The resulting coordinates of the projection in the space of the ray and
    * vector with t being the distance along the vector and u/v being
    * barycentric coordinates.
    */
   public int intersect (
      Point3d v0, Point3d v1, Point3d v2, Point3d pos, Vector3d dir,
      Vector3d duv) {
      edge0.sub (v1, v0);
      edge1.sub (v2, v0);

      /* begin calculating determinant - also used to calculate U parameter */
      pvec.cross (dir, edge1);

      /* if determinant is near zero, ray lies in plane of triangle */
      det = edge0.dot (pvec);

      if (det > -epsilon && det < epsilon)
         return 0;

      inv_det = 1.0 / det;

      /* calculate distance from vert0 to ray origin */
      tvec.sub (pos, v0);

      /* calculate U parameter and test bounds */
      duv.y = tvec.dot (pvec) * inv_det;
      if (duv.y < 0.0 || duv.y > 1.0)
         return 0;

      /* prepare to test V parameter */
      qvec.cross (tvec, edge0);

      /* calculate V parameter and test bounds */
      duv.z = dir.dot (qvec) * inv_det;
      if (duv.z < 0.0 || duv.y + duv.z > 1.0)
         return 0;

      /* calculate t, ray intersects triangle */
      duv.x = edge1.dot (qvec) * inv_det;

      return 1;
   }

   // aabb triangle overlap code

   /* **************************************************** */
   /* AABB-triangle overlap test code */
   /* by Tomas Akenine-Moeller */
   /* Function: int triBoxOverlap(float boxcenter[3], */
   /* float boxhalfsize[3],float triverts[3][3]); */
   /* History: */
   /* 2001-03-05: released the code in its first version */
   /* 2001-06-18: changed the order of the tests, faster */
   /*                                                      */
   /* Acknowledgement: Many thanks to Pierre Terdiman for */
   /* suggestions and discussions on how to optimize code. */
   /* Thanks to David Hunt for finding a ">="-bug! */
   /* **************************************************** */

   private Vector3d vmin = new Vector3d(), vmax = new Vector3d();

   private int planeBoxOverlap (Vector3d normal, Vector3d vert, Vector3d maxbox) {
      double v;
      // for (int q = 0; q < 3; q++)
      // {
      // v = vert.get(q);
      // if (normal.get(q) > 0.0f)
      // {
      // vmin.set(q, -maxbox.get(q) - v);
      // vmax.set(q, maxbox.get(q) - v);
      // }
      // else
      // {
      // vmin.set(q, maxbox.get(q) - v);
      // vmax.set(q, -maxbox.get(q) - v);
      // }
      // }
      // if (normal.dot(vmin) > 0.0)
      // return 0;
      // if (normal.dot(vmax) >= 0.0)
      // return 1;

      v = vert.x;
      if (normal.x > 0.0f) {
         vmin.x = -maxbox.x - v;
         vmax.x = maxbox.x - v;
      }
      else {
         vmin.x = maxbox.x - v;
         vmax.x = -maxbox.x - v;
      }

      v = vert.y;
      if (normal.y > 0.0f) {
         vmin.y = -maxbox.y - v;
         vmax.y = maxbox.y - v;
      }
      else {
         vmin.y = maxbox.y - v;
         vmax.y = -maxbox.y - v;
      }

      v = vert.z;
      if (normal.z > 0.0f) {
         vmin.z = -maxbox.z - v;
         vmax.z = maxbox.z - v;
      }
      else {
         vmin.z = maxbox.z - v;
         vmax.z = -maxbox.z - v;
      }

      if ((normal.x * vmin.x + normal.y * vmin.y + normal.z * vmin.z) > 0.0)
         return 0;
      if ((normal.x * vmax.x + normal.y * vmax.y + normal.z * vmax.z) >= 0.0)
         return 1;

      return 0;
   }

   /* ======================== X-tests ======================== */
   private int AXISTEST_X01 (
      double a, double b, double fa, double fb, Vector3d v0, Vector3d v1,
      Vector3d v2, Vector3d boxhalfsize) {
      p0 = a * v0.y - b * v0.z;
      p2 = a * v2.y - b * v2.z;
      if (p0 < p2) {
         min = p0;
         max = p2;
      }
      else {
         min = p2;
         max = p0;
      }
      rad = fa * boxhalfsize.y + fb * boxhalfsize.z;
      if (min > rad || max < -rad)
         return 0;
      return 1;
   }

   private int AXISTEST_X2 (
      double a, double b, double fa, double fb, Vector3d v0, Vector3d v1,
      Vector3d v2, Vector3d boxhalfsize) {
      p0 = a * v0.y - b * v0.z;
      p1 = a * v1.y - b * v1.z;
      if (p0 < p1) {
         min = p0;
         max = p1;
      }
      else {
         min = p1;
         max = p0;
      }
      rad = fa * boxhalfsize.y + fb * boxhalfsize.z;
      if (min > rad || max < -rad)
         return 0;
      return 1;
   }

   /* ======================== Y-tests ======================== */
   private int AXISTEST_Y02 (
      double a, double b, double fa, double fb, Vector3d v0, Vector3d v1,
      Vector3d v2, Vector3d boxhalfsize) {
      p0 = -a * v0.x + b * v0.z;
      p2 = -a * v2.x + b * v2.z;
      if (p0 < p2) {
         min = p0;
         max = p2;
      }
      else {
         min = p2;
         max = p0;
      }
      rad = fa * boxhalfsize.x + fb * boxhalfsize.z;
      if (min > rad || max < -rad)
         return 0;
      return 1;
   }

   private int AXISTEST_Y1 (
      double a, double b, double fa, double fb, Vector3d v0, Vector3d v1,
      Vector3d v2, Vector3d boxhalfsize) {
      p0 = -a * v0.x + b * v0.z;
      p1 = -a * v1.x + b * v1.z;
      if (p0 < p1) {
         min = p0;
         max = p1;
      }
      else {
         min = p1;
         max = p0;
      }
      rad = fa * boxhalfsize.x + fb * boxhalfsize.z;
      if (min > rad || max < -rad)
         return 0;
      return 1;
   }

   /* ======================== Z-tests ======================== */
   private int AXISTEST_Z12 (
      double a, double b, double fa, double fb, Vector3d v0, Vector3d v1,
      Vector3d v2, Vector3d boxhalfsize) {
      p1 = a * v1.x - b * v1.y;
      p2 = a * v2.x - b * v2.y;
      if (p2 < p1) {
         min = p2;
         max = p1;
      }
      else {
         min = p1;
         max = p2;
      }
      rad = fa * boxhalfsize.x + fb * boxhalfsize.y;
      if (min > rad || max < -rad)
         return 0;
      return 1;
   }

   private int AXISTEST_Z0 (
      double a, double b, double fa, double fb, Vector3d v0, Vector3d v1,
      Vector3d v2, Vector3d boxhalfsize) {
      p0 = a * v0.x - b * v0.y;
      p1 = a * v1.x - b * v1.y;
      if (p0 < p1) {
         min = p0;
         max = p1;
      }
      else {
         min = p1;
         max = p0;
      }
      rad = fa * boxhalfsize.x + fb * boxhalfsize.y;
      if (min > rad || max < -rad)
         return 0;
      return 1;
   }

   private double min, max, p0, p1, p2, rad, fex, fey, fez;
   Vector3d cv0 = new Vector3d(), cv1 = new Vector3d(),
   cv2 = new Vector3d(), e0 = new Vector3d(), e1 = new Vector3d(),
   e2 = new Vector3d();
   Vector3d normal = new Vector3d();

   private int triBoxOverlap (
      Point3d boxcenter, Vector3d boxhalfsize, Point3d v0, Point3d v1,
      Point3d v2) {

      /* use separating axis theorem to test overlap between triangle and box */
      /* need to test for overlap in these directions: */
      /*
       * 1) the {x,y,z}-directions (actually, since we use the AABB of the
       * triangle
       */
      /* we do not even need to test these) */
      /* 2) normal of the triangle */
      /* 3) crossproduct(edge from tri, {x,y,z}-directin) */
      /* this gives 3x3=9 more tests */

      // double v0[3],v1[3],v2[3];
      // Vector3d normal, e0, e1, e2;
      /* This is the fastest branch on Sun */
      /* move everything so that the boxcenter is in (0,0,0) */
      cv0.sub (v0, boxcenter);
      cv1.sub (v1, boxcenter);
      cv2.sub (v2, boxcenter);

      /* compute triangle edges */
      e0.sub (cv1, cv0);
      e1.sub (cv2, cv1);
      e2.sub (cv0, cv2);

      /* Bullet 3: */
      /* test the 9 tests first (this was faster) */
      // fex = Math.abs(e0.x);
      // fey = Math.abs(e0.y);
      // fez = Math.abs(e0.z);
      fex = e0.x >= 0 ? e0.x : -e0.x;
      fey = e0.y >= 0 ? e0.y : -e0.y;
      fez = e0.z >= 0 ? e0.z : -e0.z;
      if (AXISTEST_X01 (e0.z, e0.y, fez, fey, cv0, cv1, cv2, boxhalfsize) == 0)
         return 0;
      if (AXISTEST_Y02 (e0.z, e0.x, fez, fex, cv0, cv1, cv2, boxhalfsize) == 0)
         return 0;
      if (AXISTEST_Z12 (e0.y, e0.x, fey, fex, cv0, cv1, cv2, boxhalfsize) == 0)
         return 0;

      // fex = Math.abs(e1.x);
      // fey = Math.abs(e1.y);
      // fez = Math.abs(e1.z);
      fex = e1.x >= 0 ? e1.x : -e1.x;
      fey = e1.y >= 0 ? e1.y : -e1.y;
      fez = e1.z >= 0 ? e1.z : -e1.z;
      if (AXISTEST_X01 (e1.z, e1.y, fez, fey, cv0, cv1, cv2, boxhalfsize) == 0)
         return 0;
      if (AXISTEST_Y02 (e1.z, e1.x, fez, fex, cv0, cv1, cv2, boxhalfsize) == 0)
         return 0;
      if (AXISTEST_Z0 (e1.y, e1.x, fey, fex, cv0, cv1, cv2, boxhalfsize) == 0)
         return 0;

      // fex = Math.abs(e2.x);
      // fey = Math.abs(e2.y);
      // fez = Math.abs(e2.z);
      fex = e2.x >= 0 ? e2.x : -e2.x;
      fey = e2.y >= 0 ? e2.y : -e2.y;
      fez = e2.z >= 0 ? e2.z : -e2.z;
      if (AXISTEST_X2 (e2.z, e2.y, fez, fey, cv0, cv1, cv2, boxhalfsize) == 0)
         return 0;
      if (AXISTEST_Y1 (e2.z, e2.x, fez, fex, cv0, cv1, cv2, boxhalfsize) == 0)
         return 0;
      if (AXISTEST_Z12 (e2.y, e2.x, fey, fex, cv0, cv1, cv2, boxhalfsize) == 0)
         return 0;

      /* Bullet 1: */
      /* first test overlap in the {x,y,z}-directions */
      /* find min, max of the triangle each direction, and test for overlap in */
      /* that direction -- this is equivalent to testing a minimal AABB around */
      /* the triangle against the AABB */

      // /* test in X-direction */
      // min = Math.min(v0.x, Math.min(v1.x, v2.x));
      // max = Math.max(v0.x, Math.max(v1.x, v2.x));
      // if (min > boxhalfsize.x || max < -boxhalfsize.x)
      // return 0;
      //
      // /* test in Y-direction */
      // min = Math.min(v0.y, Math.min(v1.y, v2.y));
      // max = Math.max(v0.y, Math.max(v1.y, v2.y));
      // if (min > boxhalfsize.y || max < -boxhalfsize.y)
      // return 0;
      //
      // /* test in Z-direction */
      // min = Math.min(v0.z, Math.min(v1.z, v2.z));
      // max = Math.max(v0.z, Math.max(v1.z, v2.z));
      // if (min > boxhalfsize.z || max < -boxhalfsize.z)
      // return 0;
      /* test in X-direction */
      if ((v0.x > boxhalfsize.x &&
           v1.x > boxhalfsize.x &&
           v2.x > boxhalfsize.x) ||
          (v0.x < -boxhalfsize.x &&
           v1.x < -boxhalfsize.x &&
           v2.x < -boxhalfsize.x))
         return 0;

      /* test in Y-direction */
      if ((v0.y > boxhalfsize.y &&
           v1.y > boxhalfsize.y &&
           v2.y > boxhalfsize.y) ||
         (v0.y < -boxhalfsize.y &&
          v1.y < -boxhalfsize.y &&
          v2.y < -boxhalfsize.y))
         return 0;

      /* test in Z-direction */
      if ((v0.z > boxhalfsize.z &&
           v1.z > boxhalfsize.z &&
           v2.z > boxhalfsize.z) ||
         (v0.z < -boxhalfsize.z &&
          v1.z < -boxhalfsize.z &&
          v2.z < -boxhalfsize.z))
         return 0;

      /* Bullet 2: */
      /* test if the box intersects the plane of the triangle */
      /* compute plane equation of triangle: normal*x+d=0 */
      normal.cross (e0, e1);

      if (planeBoxOverlap (normal, cv0, boxhalfsize) == 0)
         return 0;

      return 1; /* box and triangle overlaps */
   }

   Point3d lv0 = new Point3d(), lv1 = new Point3d(), lv2 = new Point3d();

//   /**
//    * Checks to see whether a triangle intersects an obb.
//    * 
//    * @param obb
//    * The obb.
//    * @param v0
//    * The first vertice.
//    * @param v1
//    * The second vertice.
//    * @param v2
//    * The third vertice.
//    * @return True if the triangle and obb intersect.
//    */
//   public boolean intersect (OBB obb, Point3d v0, Point3d v1, Point3d v2) {
//      lv0.inverseTransform (obb.X, v0);
//      lv1.inverseTransform (obb.X, v1);
//      lv2.inverseTransform (obb.X, v2);
//
//      return triBoxOverlap (
//         new Point3d (0, 0, 0), obb.halfWidths, lv0, lv1, lv2) == 1;
//   }

   // taken from
   // http://www.geometrictools.com/Foundation/Distance/Wm3DistVector3Triangle3.cpp
   //
   // Geometric Tools, Inc.
   // http://www.geometrictools.com
   // Copyright (c) 1998-2006. All Rights Reserved
   //
   // The Wild Magic Library (WM3) source code is supplied under the terms of
   // the license agreement
   // http://www.geometrictools.com/License/WildMagic3License.pdf
   // and may not be copied or disclosed except in accordance with the terms
   // of that agreement.

   Vector3d kDiff = new Vector3d(), kEdge0 = new Vector3d(),
   kEdge1 = new Vector3d();

   /**
    * Finds the nearest distance between a point and a triangle.
    * 
    * @param v0
    * The first vertice.
    * @param v1
    * The second vertice.
    * @param v2
    * The third vertice.
    * @param p
    * The point to measure from.
    * @param closest (optional) 
    * Returns the closest point to p on the triangle.
    * @param uv (optional)
    * Returns the barycentric coordinates of the nearest
    * point where u and v are the weights for vertices 1 and 2 respectively.
    * @return The distance from point p to the nearest point on triangle v0, v1
    * and v2.
    */
   public double nearestpoint (
      Point3d v0, Point3d v1, Point3d v2, Point3d p, Point3d closest,
      Vector2d uv) {
      kDiff.sub (v0, p);
      kEdge0.sub (v1, v0);
      kEdge1.sub (v2, v0);
      double fA00 = kEdge0.normSquared();
      double fA01 = kEdge0.dot (kEdge1);
      double fA11 = kEdge1.normSquared();
      double fB0 = kDiff.dot (kEdge0);
      double fB1 = kDiff.dot (kEdge1);
      double fC = kDiff.normSquared();
      double fDet = Math.abs (fA00 * fA11 - fA01 * fA01);
      double fS = fA01 * fB1 - fA11 * fB0;
      double fT = fA01 * fB0 - fA00 * fB1;
      double fSqrDistance;

      if (fS + fT <= fDet) {
         if (fS < (double)0.0) {
            if (fT < (double)0.0) // region 4
            {
               if (fB0 < (double)0.0) {
                  fT = (double)0.0;
                  if (-fB0 >= fA00) {
                     fS = (double)1.0;
                     fSqrDistance = fA00 + ((double)2.0) * fB0 + fC;
                  }
                  else {
                     fS = -fB0 / fA00;
                     fSqrDistance = fB0 * fS + fC;
                  }
               }
               else {
                  fS = (double)0.0;
                  if (fB1 >= (double)0.0) {
                     fT = (double)0.0;
                     fSqrDistance = fC;
                  }
                  else if (-fB1 >= fA11) {
                     fT = (double)1.0;
                     fSqrDistance = fA11 + ((double)2.0) * fB1 + fC;
                  }
                  else {
                     fT = -fB1 / fA11;
                     fSqrDistance = fB1 * fT + fC;
                  }
               }
            }
            else
            // region 3
            {
               fS = (double)0.0;
               if (fB1 >= (double)0.0) {
                  fT = (double)0.0;
                  fSqrDistance = fC;
               }
               else if (-fB1 >= fA11) {
                  fT = (double)1.0;
                  fSqrDistance = fA11 + ((double)2.0) * fB1 + fC;
               }
               else {
                  fT = -fB1 / fA11;
                  fSqrDistance = fB1 * fT + fC;
               }
            }
         }
         else if (fT < (double)0.0) // region 5
         {
            fT = (double)0.0;
            if (fB0 >= (double)0.0) {
               fS = (double)0.0;
               fSqrDistance = fC;
            }
            else if (-fB0 >= fA00) {
               fS = (double)1.0;
               fSqrDistance = fA00 + ((double)2.0) * fB0 + fC;
            }
            else {
               fS = -fB0 / fA00;
               fSqrDistance = fB0 * fS + fC;
            }
         }
         else
         // region 0
         {
            // minimum at interior point
            double fInvDet = ((double)1.0) / fDet;
            fS *= fInvDet;
            fT *= fInvDet;
            fSqrDistance =
               fS * (fA00 * fS + fA01 * fT + ((double)2.0) * fB0) + fT
               * (fA01 * fS + fA11 * fT + ((double)2.0) * fB1) + fC;
         }
      }
      else {
         double fTmp0, fTmp1, fNumer, fDenom;

         if (fS < (double)0.0) // region 2
         {
            fTmp0 = fA01 + fB0;
            fTmp1 = fA11 + fB1;
            if (fTmp1 > fTmp0) {
               fNumer = fTmp1 - fTmp0;
               fDenom = fA00 - 2.0f * fA01 + fA11;
               if (fNumer >= fDenom) {
                  fS = (double)1.0;
                  fT = (double)0.0;
                  fSqrDistance = fA00 + ((double)2.0) * fB0 + fC;
               }
               else {
                  fS = fNumer / fDenom;
                  fT = (double)1.0 - fS;
                  fSqrDistance =
                     fS * (fA00 * fS + fA01 * fT + 2.0f * fB0) + fT
                     * (fA01 * fS + fA11 * fT + ((double)2.0) * fB1) + fC;
               }
            }
            else {
               fS = (double)0.0;
               if (fTmp1 <= (double)0.0) {
                  fT = (double)1.0;
                  fSqrDistance = fA11 + ((double)2.0) * fB1 + fC;
               }
               else if (fB1 >= (double)0.0) {
                  fT = (double)0.0;
                  fSqrDistance = fC;
               }
               else {
                  fT = -fB1 / fA11;
                  fSqrDistance = fB1 * fT + fC;
               }
            }
         }
         else if (fT < (double)0.0) // region 6
         {
            fTmp0 = fA01 + fB1;
            fTmp1 = fA00 + fB0;
            if (fTmp1 > fTmp0) {
               fNumer = fTmp1 - fTmp0;
               fDenom = fA00 - ((double)2.0) * fA01 + fA11;
               if (fNumer >= fDenom) {
                  fT = (double)1.0;
                  fS = (double)0.0;
                  fSqrDistance = fA11 + ((double)2.0) * fB1 + fC;
               }
               else {
                  fT = fNumer / fDenom;
                  fS = (double)1.0 - fT;
                  fSqrDistance =
                     fS * (fA00 * fS + fA01 * fT + ((double)2.0) * fB0) + fT
                     * (fA01 * fS + fA11 * fT + ((double)2.0) * fB1) + fC;
               }
            }
            else {
               fT = (double)0.0;
               if (fTmp1 <= (double)0.0) {
                  fS = (double)1.0;
                  fSqrDistance = fA00 + ((double)2.0) * fB0 + fC;
               }
               else if (fB0 >= (double)0.0) {
                  fS = (double)0.0;
                  fSqrDistance = fC;
               }
               else {
                  fS = -fB0 / fA00;
                  fSqrDistance = fB0 * fS + fC;
               }
            }
         }
         else
         // region 1
         {
            fNumer = fA11 + fB1 - fA01 - fB0;
            if (fNumer <= (double)0.0) {
               fS = (double)0.0;
               fT = (double)1.0;
               fSqrDistance = fA11 + ((double)2.0) * fB1 + fC;
            }
            else {
               fDenom = fA00 - 2.0f * fA01 + fA11;
               if (fNumer >= fDenom) {
                  fS = (double)1.0;
                  fT = (double)0.0;
                  fSqrDistance = fA00 + ((double)2.0) * fB0 + fC;
               }
               else {
                  fS = fNumer / fDenom;
                  fT = (double)1.0 - fS;
                  fSqrDistance =
                     fS * (fA00 * fS + fA01 * fT + ((double)2.0) * fB0) + fT
                     * (fA01 * fS + fA11 * fT + ((double)2.0) * fB1) + fC;
               }
            }
         }
      }

      // // account for numerical round-off error
      // if (fSqrDistance < (double) 0.0)
      // {
      // fSqrDistance = (double) 0.0;
      // }

      // m_kClosestPoint0 = m_rkVector;
      if (closest != null) {
         closest.scaledAdd (fS, kEdge0, v0);
         closest.scaledAdd (fT, kEdge1, closest);
         // m_kClosestPoint1 = v0 + fS*kEdge0 + fT*kEdge1;
      }

      if (uv != null) {
         uv.x = fS;
         uv.y = fT;
      }
      // m_afTriangleBary[1] = fS;
      // m_afTriangleBary[2] = fT;
      // m_afTriangleBary[0] = (double)1.0 - fS - fT;

      // System.out.println(fSqrDistance);
      if (fSqrDistance < 0)
         return 0;
      else
         return Math.sqrt (fSqrDistance);
   }

   Point3d lp = new Point3d();
   Vector3d off = new Vector3d();

//   /**
//    * Determines the distance of nearest and farthest points on the obb from a
//    * given point.
//    * 
//    * @param obb
//    * The obb.
//    * @param p
//    * The point.
//    * @param d
//    * The distances of the points on the obb where x is the nearest distance to
//    * p and y is the farthest distance from p.
//    * @return Whether or not the point is in the box.
//    */
//   public boolean proximity (OBB obb, Point3d p, Vector2d d) {
//      lp.inverseTransform (obb.X, p);
//      lp.absolute();
//      off.sub (lp, obb.halfWidths);
//
//      boolean inside;
//
//      if (off.x <= 0 && off.y <= 0 && off.z <= 0) {
//         d.x = -off.maxElement();
//
//         inside = true;
//      }
//      else {
//         d.x = 0;
//
//         if (off.x > 0)
//            d.x += off.x * off.x;
//
//         if (off.y > 0)
//            d.x += off.y * off.y;
//
//         if (off.z > 0)
//            d.x += off.z * off.z;
//
//         d.x = Math.sqrt (d.x);
//
//         inside = false;
//      }
//
//      lp.add (obb.halfWidths);
//      d.y = lp.norm();
//
//      return inside;
//   }

   // public boolean proximity (AjlBvNode bvNode, Point3d p, Vector2d d) {
   //    bvNode.transformMeshToBox (lp, p);
   //    lp.absolute();
   //    off.sub (lp, bvNode.halfWidths);

   //    boolean inside;

   //    if (off.x <= 0 && off.y <= 0 && off.z <= 0) {
   //       d.x = -off.maxElement();

   //       inside = true;
   //    }
   //    else {
   //       d.x = 0;

   //       if (off.x > 0)
   //          d.x += off.x * off.x;

   //       if (off.y > 0)
   //          d.x += off.y * off.y;

   //       if (off.z > 0)
   //          d.x += off.z * off.z;

   //       d.x = Math.sqrt (d.x);

   //       inside = false;
   //    }

   //    lp.add (bvNode.halfWidths);
   //    d.y = lp.norm();

   //    return inside;
   // }

   public ArrayList<Point3d> intersectTrianglePlane(Point3d p0, Point3d p1,
      Point3d p2, Plane plane) {
      
      ArrayList<Point3d> points = new ArrayList<Point3d>();
      double d0 = plane.distance(p0);
      double d1 = plane.distance(p1);
      double d2 = plane.distance(p2);
      double ad0 = Math.abs(d0);
      double ad1 = Math.abs(d1);
      double ad2 = Math.abs(d2);
      Point3d p;
      
      // from p0 to p1
      double t = d0/(d0-d1);
      if (t > 0 && t < 1 && ad0 > epsilon && ad1 > epsilon) {
         p = new Point3d();
         p.interpolate(p0, t, p1);
         points.add(p);
      }
      
      // from p1 to p2
      t = d1/(d1-d2);
      if (t > 0 && t < 1 && ad1 > epsilon && ad2 > epsilon) {
         p = new Point3d();
         p.interpolate(p1, t, p2);
         points.add(p);
      }
      
      // from p2 to p0
      t = d2/(d2-d0);
      if (t > 0 && t < 1 && ad2 > epsilon && ad0 > epsilon) {
         p = new Point3d();
         p.interpolate(p2, t, p0);
         points.add(p);
      }
      
      // end points
      if (ad0 <= epsilon) {
         points.add(new Point3d(p0));
      }
      if (ad1 <= epsilon) {
         points.add(new Point3d(p1));
      }
      if (ad2 <= epsilon) {
         points.add(new Point3d(p2));
      }
      
      return points;
   }

//   public ArrayList<Point3d> intersectPlaneLineSegment(Vector3d n, double d, Point3d ls0, Point3d ls1) {
//      
//      ArrayList<Point3d> points = new ArrayList<Point3d>();
//      double d0 = n.dot(ls0)-d;
//      double d1 = n.dot(ls1)-d;
//      double ad0 = Math.abs(d0);
//      double ad1 = Math.abs(d1);
//      
//      if (ad0 < epsilon) {
//         points.add(new Point3d(ls0));
//      }
//      if (ad1 < epsilon) {
//         points.add(new Point3d(ls1));
//      }
//      
//      // from p0 to p1
//      double t = d0/(d0-d1);
//      if (t > 0 && t < 1 && ad0 > epsilon && ad1 > epsilon) {
//         Point3d p = new Point3d();
//         p.interpolate(ls0, t, ls1);
//         points.add(p);
//      }
//      
//      return points;
//   }
   
}
