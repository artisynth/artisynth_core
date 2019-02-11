package maspack.geometry;

import java.util.*;
import maspack.matrix.*;

/**
 * Class to interpolate directions within a Polyline mesh.
 */
public class PolylineInterpolator {

   PolylineMesh myMesh;

   public PolylineInterpolator (PolylineMesh mesh) {
      myMesh = mesh;
   }

   /**
    * Computes the average direction in the vicinity of a point based on the
    * line segments contained in a PolylineMesh. Returns the number of
    * supporting line segments used for the calculation. If no segments were
    * found, the method returns 0 and the direction is undefined.
    * 
    * @param dir returns the normalized direction
    * @param pos position at which direction should be computed
    * @param rad radius of influence within which polyline mesh segments are
    * considerd
    */
   public int computeAverageDirection (
      Vector3d dir, Point3d pos, double rad) {

      BVTree bvh = myMesh.getBVTree();
      ArrayList<BVNode> nodes = new ArrayList<BVNode>();

      Matrix3d cov = new Matrix3d();
      SVDecomposition3d svd = new SVDecomposition3d();

      Vector3d tmp = new Vector3d();
      Matrix3d tmp2 = new Matrix3d();

      bvh.intersectSphere(nodes, pos, rad);

      dir.setZero();

      int nsegs = 0;

      Vector3d segmentSum = new Vector3d(); //for computing sign of direction vector

      // System.out.println("p=[");
      for (BVNode n : nodes) {
         Boundable[] elements = n.getElements();

         for (int i = 0; i < elements.length; i++) {

            LineSegment seg = (LineSegment)elements[i];
            seg = getSegmentInsideSphere(seg, pos, rad);

            if (seg != null) {
               tmp.sub(seg.myVtx1.pnt, seg.myVtx0.pnt);

               if (tmp.norm() >= 1e-8 * rad) {
                  //                  System.out.println(seg.myVtx0.getPosition() + " " +
                  //                     seg.myVtx1.getPosition());
                  nsegs++;
                  // prepare to average directions using SVD
                  computeCov(tmp2, tmp);
                  cov.add(tmp2);
                  segmentSum.add(tmp);
               }
            }
         }
      }
      //      System.out.println("];");

      if (nsegs > 0) {

         // we are technically including both +/- directions, so
         // we have twice the number of points
         cov.scale(2.0 / (2.0 * nsegs - 1));
         try {
            svd.factor(cov);
         } catch (Exception e) {
            //System.err.println(e.getMessage());
         }
         tmp2 = svd.getU(); // principal components
         tmp2.getColumn(0, dir);

         dir.normalize();

         // flip sign if not in same direction as
         // most line segments
         if (dir.dot(segmentSum)<0) {
            dir.scale(-1);
         }


         //          System.out.println("c=["+pos + " " + rad + "];");
         //          System.out.println("dir=[" + dir +"];\n");

         return nsegs;
      }
      else {
         return 0;
      }

   }

   private static void computeCov(Matrix3d mat, Vector3d vec) {
      mat.m00 = vec.x * vec.x;
      mat.m01 = vec.x * vec.y;
      mat.m02 = vec.x * vec.z;
      mat.m10 = mat.m01;
      mat.m11 = vec.y * vec.y;
      mat.m12 = vec.y * vec.z;
      mat.m20 = mat.m02;
      mat.m21 = mat.m12;
      mat.m22 = vec.z * vec.z;
   }

   // intersects segment with the given sphere, and returns
   // the portion inside or null if no intersection
   private static LineSegment getSegmentInsideSphere (
      LineSegment segment, Point3d pos,
      double rad) {

      Point3d p1 = segment.myVtx0.getPosition();
      Point3d p2 = segment.myVtx1.getPosition();

      // check if segment starts inside
      boolean isP1Inside = isInsideSphere(p1, pos, rad);
      Point3d p = new Point3d(); // p2-c
      Point3d dp = new Point3d(); // p1-p2

      double r2 = rad * rad;
      double a, b, c, d, lambda1, lambda2; // for use with quadratic equation

      p.sub(p2, pos);
      dp.sub(p1, p2);

      // find intersection with sphere
      a = dp.normSquared();
      b = 2 * dp.dot(p);
      c = p.normSquared() - r2;

      d = b * b - 4 * a * c;
      if (d >= 0) {
         d = Math.sqrt(d);
         lambda1 = (-b + d) / (2 * a);
         lambda2 = (-b - d) / (2 * a);
      } else {
         lambda1 = Double.NaN;
         lambda2 = Double.NaN;
      }

      // if p2 is inside, return
      if (p.normSquared() <= r2) {
         if (isP1Inside) {
            return segment;
         } else {

            // find intersection
            if (lambda1 >= 0 && lambda1 <= 1) {
               p1.scaledAdd(lambda1, dp, p2);
            } else {
               p1.scaledAdd(lambda2, dp, p2);
            }
            return new LineSegment(new Vertex3d(p1), new Vertex3d(p2));
         }

      } else {
         // if p1 inside, find single intersection
         if (isP1Inside) {
            if (lambda1 >= 0 && lambda1 <= 1) {
               p2.scaledAdd(lambda1, dp);
            } else {
               p2.scaledAdd(lambda2, dp);
            }
            return new LineSegment(new Vertex3d(p1), new Vertex3d(p2));

         } else {

            // check if passes entirely through sphere
            if (d >= 0) {
               if (lambda1 >= 0 && lambda1 <= 1 && lambda2 >= 0 && lambda2 <= 1) {
                  p1.scaledAdd(lambda1, dp, p2);
                  p2.scaledAdd(lambda2, dp);
                  return new LineSegment(new Vertex3d(p1), new Vertex3d(p2));
               }

            } // done checking if crossed sphere
         } // done checking if p1 outside
      } // done checking

      return null;

   }

   private static boolean isInsideSphere(Point3d pos, Point3d c, double rad) {

      Point3d p = new Point3d();
      p.sub(pos, c);
      if (p.normSquared() <= rad * rad) {
         return true;
      }
      return false;

   }


}
