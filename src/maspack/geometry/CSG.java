/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC), John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import maspack.collision.SurfaceMeshIntersector;
import maspack.geometry.OBB.Method;
import maspack.matrix.Matrix3d;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.util.Logger;

/**
 * Constructive Solid Geometry tools
 */
public class CSG {

   SurfaceMeshIntersector myIntersector;
   
   public CSG() {
      myIntersector = new SurfaceMeshIntersector();
   }
      
   public PolygonalMesh getIntersection(PolygonalMesh mesh1, PolygonalMesh mesh2) {
      return myIntersector.findIntersection(mesh1, mesh2);
   }

 
   public PolygonalMesh getSubtraction(PolygonalMesh mesh1, PolygonalMesh mesh2) {
      return myIntersector.findDifference01(mesh1, mesh2);
   }
      
   public PolygonalMesh getUnion(PolygonalMesh mesh1, PolygonalMesh mesh2) {
      return myIntersector.findUnion(mesh1, mesh2);
   }

   /**
    * Computes the intersection volume contained by two meshes
    */
   public double computeIntersectionVolume(PolygonalMesh mesh1, PolygonalMesh mesh2) {
      PolygonalMesh isect = getIntersection(mesh1, mesh2);
      return isect.computeVolume();
   }

   /**
    * Computes Dice coefficient between two meshes.
    */
   public double computeDice(PolygonalMesh mesh1, PolygonalMesh mesh2) {

      double v1 = mesh1.computeVolume();
      double v2 = mesh2.computeVolume();

      double vi = computeIntersectionVolume(mesh1, mesh2);
      return (2*vi/(v1+v2));

   }

   private void doSphereTest(double r1, double r2, double d) {
      // test with spheres

      double svol1 = 4*Math.PI*(r1*r1*r1)/3;
      double svol2 = 4*Math.PI*(r2*r2*r2)/3;
      double svoli;
      if (d > r1 + r2) {
         svoli = 0;
      } else if (d == 0) {
         double minr = Math.min(r1, r2);
         svoli = 4*Math.PI*minr*minr*minr/3;
      } else {
         svoli = Math.PI/12/d*(r1+r2-d)*(r1+r2-d)*(d*d+2*d*r1-3*r1*r1+2*d*r2-3*r2*r2+6*r1*r2);
      }
      double sdice = 2*svoli/(svol1+svol2);

      PolygonalMesh s1 = MeshFactory.createOctahedralSphere(r1, 7);
      PolygonalMesh s2 = MeshFactory.createOctahedralSphere(r2, 7);
      s2.translate(new Vector3d(d,0,0));

      double mvol1 = s1.computeVolume();
      double mvol2 = s2.computeVolume();
      double mvoli = computeIntersectionVolume(s1, s2);
      double mdice = 2*mvoli/(mvol1+mvol2);

      System.out.println("Sphere test:");
      System.out.println("   Ideal dice: " + sdice);
      System.out.println("   Mesh dice: " + mdice);
   }

   private void doCubeTest(Point3d dist) {

      PolygonalMesh m1 = MeshFactory.createQuadBox(1, 1, 1);
      PolygonalMesh m2 = MeshFactory.createQuadBox(1, 1, 1);
      m2.translate(dist);

      m1.triangulate();
      m2.triangulate();

      double mvol1 = m1.computeVolume();
      double mvol2 = m2.computeVolume();
      double mvoli = computeIntersectionVolume(m1, m2);
      double mdice = 2*mvoli/(mvol1+mvol2);
      System.out.println("Cube test:");
      System.out.println("   Mesh dice: " + mdice);
   }

   private void doDiceTest(String fn1, String fn2) {

      try {
         PolygonalMesh m1 = new PolygonalMesh(fn1);
         PolygonalMesh m2 = new PolygonalMesh(fn2);

         OBBTree tree1 = new OBBTree(m1, Method.Covariance, 1, 1e-10);
         OBBTree tree2 = new OBBTree(m2, Method.Covariance, 1, 1e-10);

         //         AABBTree tree1 = new AABBTree(m1, 1, 1e-10);
         //         AABBTree tree2 = new AABBTree(m2, 1, 1e-10);

         ArrayList<BVNode> nodes1 = tree1.getLeafNodes();
         ArrayList<BVNode> nodes2 = tree2.getLeafNodes();

         System.out.println("# Tree 1 has " + nodes1.size() + " leaves");
         System.out.println("# Tree 2 has " + nodes2.size() + " leaves");

         nodes1.clear();
         nodes2.clear();
         tree1.intersectTree(nodes1, nodes2, tree2);

         for (int i=0; i<nodes1.size(); i++) {
            BVNode node1 = nodes1.get(i);
            BVNode node2 = nodes2.get(i);

            System.out.printf("Intersection %d: \n", i);

            System.out.printf("   Node 1: ");
            for (int j=0; j<node1.getNumElements(); j++) {
               Boundable elem = node1.myElements[j];
               Face face = (Face)elem;

               System.out.printf(" (");
               for (Vertex3d v : face.getVertices()) {
                  System.out.printf(" %d", v.idx);
               }
               System.out.printf(" ) ");
            }
            System.out.printf("\n");

            System.out.printf("   Node 2: ");
            for (int j=0; j<node2.getNumElements(); j++) {
               Boundable elem = node2.myElements[j];
               Face face = (Face)elem;

               System.out.printf(" (");
               for (Vertex3d v : face.getVertices()) {
                  System.out.printf(" %d", v.idx);
               }
               System.out.printf(" ) ");
            }
            System.out.printf("\n");
         }

         double d = computeDice(m1, m2);
         System.out.println("# Dice: " + d);

      } catch (Exception e) {
         e.printStackTrace();
      }


   }

   private static void doObbTest() {

      OBB obb1 = new OBB();
      RigidTransform3d trans = new RigidTransform3d(new Point3d(0.5, -0.25, -0.25),
         new RotationMatrix3d(0.81649658092772603, 1.1102230246251565e-016, 0.57735026918962573, 0.40824829046386307, -0.70710678118654757, -0.57735026918962573, 0.40824829046386302, 0.70710678118654746, -0.57735026918962573));
      obb1.setTransform(trans);
      obb1.myHalfWidths.set(0.61237243579579459, 0.70710678128654769, 1.0000004163336343e-010);

      OBB obb2 = new OBB();
      trans = new RigidTransform3d(new Point3d( -0.099999999999999978, -0.25000000000000011, -0.24999999999999994),
         new RotationMatrix3d(0.81649658092772603, -1.1102230246251565e-016, -0.57735026918962573, -0.40824829046386307, -0.70710678118654757, -0.57735026918962573, -0.40824829046386302, 0.70710678118654746, -0.57735026918962573));
      obb2.setTransform(trans);
      obb2.myHalfWidths.set(0.61237243579579459, 0.70710678128654758, 1.0000005551115123e-010);

      boolean disjoint = BVBoxNodeTester.isDisjoint(obb1, obb2);
      System.out.printf("%s\n", disjoint ? "no" : "yes");
   }

   public static void main(String[] args) {

      CSG csg = new CSG();
      
      csg.doSphereTest(1, 1, 1);
      csg.doCubeTest(new Point3d(0.75, 0.75, 0.75));

      doObbTest();

      if (args.length == 2) {
         csg.doDiceTest(args[0], args[1]);
      }

   }


}
