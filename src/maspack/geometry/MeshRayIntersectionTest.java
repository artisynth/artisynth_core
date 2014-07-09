/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;

public class MeshRayIntersectionTest {
   public static void main (String[] args) {
      testRayIntersection();
      System.out.println ("\nPassed\n");
   }

   /*
    * Make a sphere and fire rays at the centroid of each face, from outside the
    * sphere. Ensure the correct face is hit. Test both a fixed rigid body mesh
    * and a non-fixed fem mesh.
    */

   static void testRayIntersection() {
      double sphereRadius = 3.0;
      RigidTransform3d T0 = new RigidTransform3d (1.0, 8.0, -7.0);
      //Point3d sphere0Pos = new Point3d (1.0, 8.0, -7.0);
      Point3d sphereMeshOrigin = new Point3d(); // sphere origin in mesh coords
      PolygonalMesh mesh0;

      mesh0 = MeshFactory.createSphere (sphereRadius, 32);
      mesh0.setMeshToWorld (T0);
      //Test.positionRigidMesh (mesh0, sphere0Pos);
      sphereMeshOrigin.set (0, 0, 0); // MeshFactory centers the sphere at
                                       // 0,0,0 in mesh coordinates.
      fireRays (mesh0, sphereRadius, sphereMeshOrigin);

      mesh0 = MeshFactory.createSphere (sphereRadius, 44);
      mesh0.setFixed (false);
      mesh0.transform (T0);
      //Test.positionFemMesh (mesh0, sphere0Pos);
      sphereMeshOrigin.set (T0.p);
      fireRays (mesh0, sphereRadius, sphereMeshOrigin);
   }

   static void fireRays (
      PolygonalMesh mesh, double sphereRadius, Point3d sphereMeshOrigin) {
      Vector3d duv = new Vector3d();
//      TriangleIntersector intersector = new TriangleIntersector();
      BVFeatureQuery query = new BVFeatureQuery();
      Point3d rayOrigin = new Point3d(), worldOrigin = new Point3d();
      Vector3d rayDir = new Vector3d(), worldDir = new Vector3d();
      Vector3d centroid = new Vector3d();
      int nPassed = 0;
      for (Face f : mesh.getFaces()) {
         // Construct a ray, in mesh coordinates, that points from the face
         // centroid to the origin.
         f.computeCentroid (centroid);
         rayDir.sub (sphereMeshOrigin, centroid);
         // Start the ray outside the sphere
         rayOrigin.scaledAdd (-3 * sphereRadius, rayDir, sphereMeshOrigin);
         // Transform the ray to world coordinates.
         worldDir.transform (mesh.getMeshToWorld(), rayDir);
         worldOrigin.transform (mesh.getMeshToWorld(), rayOrigin);
         Face faceHit = 
            query.nearestFaceAlongRay (
               null, duv, mesh, worldOrigin, worldDir);
//         Face faceHit =
//            mesh.getBvHierarchy().nearestFaceIntersectedByRay (
//               worldOrigin, worldDir, duv, intersector);
         if (faceHit != f)
            throw new RuntimeException ("asdf");
         nPassed++;
      }

   }
}
