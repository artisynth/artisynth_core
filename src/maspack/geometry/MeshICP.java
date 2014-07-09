/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;

/**
 * Performs basic ICP alignment of meshes
 * 
 * @author "Antonio Sanchez" Creation date: 18 Nov 2012
 * 
 */
public class MeshICP {

   public enum AlignmentType {
      RIGID, RIGID_WITH_SCALING, ORTHOGONAL, AFFINE
   }
   public static double DEFAULT_EPSILON = 1e-12;
   public static int DEFAULT_MAX_ITERS = 1000;
   public static AlignmentType DEFAULT_ALIGNMENT_TYPE = AlignmentType.RIGID_WITH_SCALING;

   public static AffineTransform3d align(PolygonalMesh mesh1,
      PolygonalMesh mesh2, AlignmentType alignType, double eps, int maxIters, ArrayList<Point3d> out) {

      // align mesh1 to mesh2, then return inverse transform
      //OBBTree obbt = mesh2.getObbtree(); // for projecting points onto mesh1
      //TriangleIntersector ti = new TriangleIntersector(); // stores information in nearest face
                                          // algorithm
      Vector2d coords = new Vector2d(); // barycentric coordinates of closest
                                        // point

      double err = Double.POSITIVE_INFINITY; // mean distance error
      double prevErr = Double.POSITIVE_INFINITY; // previous mean error
      int iters = 0; // number of iterations

      // get points
      ArrayList<Point3d> pnts = new ArrayList<Point3d>();
      ArrayList<Point3d> projected = new ArrayList<Point3d>();
      
      
      AffineTransform3d transInc = new AffineTransform3d(); // incremental transformation
      AffineTransform3d transOut = new AffineTransform3d(); // total transform

      // clear point sets
      pnts.clear();
      
      for (Vertex3d v : mesh1.getVertices()) {
         pnts.add(new Point3d(v.getWorldPoint()));
      }

      do {
         projected.clear();
         for (int i = 0; i < pnts.size(); i++) {
            Point3d p = pnts.get(i);
            Point3d q = new Point3d();
            
            BVFeatureQuery.getNearestFaceToPoint (q, coords, mesh2, p);
            projected.add(q); // q is the closest point on mesh2 to p
            
         }
         
         switch(alignType) {
            case AFFINE:
               transInc.fit(projected, pnts); // affine
               break;
            case ORTHOGONAL:
               transInc.fitOrthogonal(projected, pnts); // allow orthogonal scaling
               break;
            case RIGID:
               transInc.fitRigid(projected, pnts, false); // rigid no scaling
               break;
            case RIGID_WITH_SCALING:
               transInc.fitRigid(projected, pnts, true); // rigid with scaling
               break;
         }
         
         transOut.mul(transInc, transOut);   // concatenate transforms through pre-multiplication

         // compute error
         prevErr = err;
         err = 0;
         for (int i = 0; i < pnts.size(); i++) {
            pnts.get(i).transform(transInc);
            err += pnts.get(i).distance(projected.get(i));
         }
         err = err / pnts.size(); // mean error
         iters++;

         // stop when we mean error has converged or maxIters is reached.
      } while ( Math.abs(err-prevErr) > eps && iters < maxIters);

      // copy projected points to out (so in same reference coordinate as mesh2)
      if (out != null) {
         out.clear();
         for (int i = 0; i < projected.size(); i++) {
            Point3d p = projected.get(i);
//            p.transform(transOut);
            out.add(p);
         }
      }
      
      //  transOut now holds mesh1 -> mesh2 transform, we want opposite
      transOut.invert();
      
      return transOut;

   }

   public static AffineTransform3d align(PolygonalMesh mesh1,
      PolygonalMesh mesh2, AlignmentType alignType, double eps, int maxIters, PolygonalMesh out) {

      // align meshes
      ArrayList<Point3d> outPnts = new ArrayList<Point3d>();
      AffineTransform3d trans = align(mesh1, mesh2, alignType, eps, maxIters, outPnts);

      if (out != null) {
         Point3d[] nodes = outPnts.toArray(new Point3d[outPnts.size()]);
         ArrayList<Face> faces = mesh1.getFaces();
         int[][] faceIndices = new int[faces.size()][];
         for (int i = 0; i < faceIndices.length; i++) {
            faceIndices[i] = faces.get(i).getVertexIndices();
         }

         // construct new mesh from outPnts and connectivity from mesh1
         out.set(nodes, faceIndices);
      }
      return trans;

   }

   public static AffineTransform3d align(PolygonalMesh mesh1,
      PolygonalMesh mesh2, AlignmentType alignType, double eps, int maxIters) {
      return align(mesh1, mesh2, alignType, eps, maxIters, new ArrayList<Point3d>());
   }

   public static AffineTransform3d align(PolygonalMesh mesh1,
      PolygonalMesh mesh2, AlignmentType alignType, ArrayList<Point3d> out) {
      return align(mesh1, mesh2, alignType, DEFAULT_EPSILON, DEFAULT_MAX_ITERS, out);
   }

   public static AffineTransform3d align(PolygonalMesh mesh1,
      PolygonalMesh mesh2, AlignmentType alignType, PolygonalMesh out) {
      return align(mesh1, mesh2, alignType, DEFAULT_EPSILON, DEFAULT_MAX_ITERS, out);
   }

   public static AffineTransform3d align(PolygonalMesh mesh1,
      PolygonalMesh mesh2, AlignmentType alignType) {
      return align(
         mesh1, mesh2, alignType, DEFAULT_EPSILON, DEFAULT_MAX_ITERS,
         new ArrayList<Point3d>());
   }

}
