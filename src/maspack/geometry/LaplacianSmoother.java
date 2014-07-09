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
 * Applies a simple Laplacian smoothing algorithm to a polygonal mesh, along
 * with volume compenstation.
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
         return Math.sqrt (sumDsqr/mesh.getNumVertices());
      }
   }

   private static void addScaledLaplacian (
      PolygonalMesh mesh, double s, Vector3d[] L) {

      for (int i=0; i<mesh.getNumVertices(); i++) {
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
      for (int i=0; i<mesh.getNumVertices(); i++) {
         Vertex3d vtx = mesh.getVertices().get(i);
         vtx.pnt.add (L[i]);
      }
   }

   public static void smooth (
      PolygonalMesh mesh, int iterations, double lam, double mu) {
      double r0, r1;

      r0 = estimateRadius (mesh);
      // L is used to store the Laplacian
      Vector3d[] L = new Vector3d[mesh.getNumVertices()];
      for (int i=0; i<mesh.getNumVertices(); i++) {
         L[i] = new Vector3d();
      }
      for (int k=0; k<iterations; k++) {
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