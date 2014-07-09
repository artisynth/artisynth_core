/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;
import maspack.geometry.Vertex3d;
import java.util.*;

public class MeshFactoryTest {

   public void checkMesh (String methodName, PolygonalMesh mesh) {

      mesh.checkConsistency();
      if (!mesh.isManifold()) {
         throw new TestException (
            methodName + " returns non-manifold mesh");
      }
      if (!mesh.isClosed()) {
         throw new TestException (
            methodName + " returns non-closed mesh");
      }
      mesh.triangulate();
      mesh.checkConsistency();
      if (!mesh.isManifold()) {
         throw new TestException (
            "Triangulation of "+methodName+" is non-manifold");
      }
      if (!mesh.isClosed()) {
         throw new TestException (
            "Triangulation of "+methodName+" is non-close");
      }
   }

   public void test() {
      PolygonalMesh mesh;

      checkMesh ("createQuadBox",
                 MeshFactory.createQuadBox (1, 2, 3));
      checkMesh ("createCylinder",
                 MeshFactory.createCylinder (/* radius= */1, /* height= */2, 8));
      checkMesh ("createPointedCylinder",
                 MeshFactory.createPointedCylinder (
                    /* radius= */1, /* height= */2, /* tiph= */1, 8));
      checkMesh ("createQuadSphere",
                 MeshFactory.createQuadSphere (1.0, /* nslices= */6, 0, 0, 0));
   }
   
   public void testMaxMin() {
      PolygonalMesh mesh = new PolygonalMesh();
      //mesh = MeshFactory.createSphere (10, 10);
      mesh = MeshFactory.createSphere (1.0, 4);
      Point3d meshMax = new Point3d();
      Point3d meshMin = new Point3d();
      mesh.getLocalBounds (meshMin, meshMax);
      
      
      for (Vertex3d v : mesh.myVertices) {
         if (v.pnt.x < meshMin.x) {
            throw new TestException ("Mesh's minimum is incorrect");
         }
         if (v.pnt.x > meshMax.x) {
            throw new TestException ("Mesh's maximum is incorrect");
         }
         if (v.pnt.y < meshMin.y) {
            throw new TestException ("Mesh's minimum is incorrect");
         }
         if (v.pnt.y > meshMax.y) {
            throw new TestException ("Mesh's maximum is incorrect");
         }
         if (v.pnt.z < meshMin.z) {
            throw new TestException ("Mesh's minimum is incorrect");
         }
         if (v.pnt.z > meshMax.z) {
            throw new TestException ("Mesh's maximum is incorrect");
         }
      }
   }
   
   
   public static void main (String[] args) {
      MeshFactoryTest tester = new MeshFactoryTest();
      try {
         tester.test();
         tester.testMaxMin();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
