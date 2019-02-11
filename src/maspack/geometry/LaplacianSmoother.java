/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.Iterator;
import maspack.matrix.*;

/**
 * Applies Laplacian or Taubin smoothing to a polygonal mesh. Taubin smoothing
 * is a modification to Laplacian smoothing that can prevent shrinkage (see
 * Taubin, ``Curve and surface smoothing without shrinkage'', Fifth
 * International Conference on Computer Vision, 1995).
 */
public class LaplacianSmoother {

   private static double estimateRadius (PolygonalMesh mesh) {
      if (mesh.isClosed()) {
         return Math.pow (mesh.computeVolume(), 1/3.0);
      }
      else {
         double sumDsqr = 0;
         Point3d cent = new Point3d();
         mesh.computeCentroid(cent);
         // estimate distance from centroid
         for (Vertex3d vtx : mesh.getVertices()) {
            sumDsqr += vtx.pnt.distanceSquared (cent);
         }
         return Math.sqrt (sumDsqr/mesh.numVertices());
      }
   }

   private static void addScaledLaplacian (
      PolygonalMesh mesh, double s, Vector3d[] L) {

      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vtx = mesh.getVertices().get(i);
         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         Vector3d lap = L[i];
         lap.setZero();
         int n = 0;
         while (it.hasNext()) {
            HalfEdge he = it.next();
            lap.add (he.getTail().pnt);
            n++;
         }
         if (n != 0) {
            lap.scale (s/(double)n);
            lap.scaledAdd (-s, vtx.pnt);
         }
         else {
            // no adjacent vertices, so don't do anything
            lap.setZero();
         }
      }
      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vtx = mesh.getVertices().get(i);
         vtx.pnt.add (L[i]);
      }
   }

   /**
    * Implements a specified number of iterations of Taubin smoothing.  lam
    * should be positive, and mu should be negative, with an absolute value
    * greater than lam. Typical values are {@code lam = 0.33}, {@code mu =
    * -0.34}. Setting {@code lam = 1} and {@code mu = 0} results in traditional
    * Laplacian smoothing.
    *
    * @param mesh mesh to be smoothed
    * @param numi number of iterations
    * @param lam first Taubin parameter
    * @param mu second Taubin parameter
    */
   public static void smooth (
      PolygonalMesh mesh, int numi, double lam, double mu) {
      double r0, r1;

      r0 = estimateRadius (mesh);
      // L is used to store the Laplacian
      Vector3d[] L = new Vector3d[mesh.numVertices()];
      for (int i=0; i<mesh.numVertices(); i++) {
         L[i] = new Vector3d();
      }
      for (int k=0; k<numi; k++) {
         addScaledLaplacian (mesh, lam, L);
         if (mu != 0) {
            addScaledLaplacian (mesh, mu, L);
         }
      }
      r1 = estimateRadius (mesh);

      Point3d cent = new Point3d();
      Vector3d diff = new Vector3d();
      double s = r0/r1;
      mesh.computeCentroid(cent);
      // for (Vertex3d vtx : mesh.getVertices()) {
      //    diff.sub (vtx.pnt, cent);
      //    diff.scale (s);
      //    vtx.pnt.add (cent, diff);
      // }
   }
}
