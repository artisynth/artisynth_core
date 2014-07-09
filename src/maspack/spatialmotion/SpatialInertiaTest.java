/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import java.io.StringReader;
import java.util.Random;

import maspack.matrix.CholeskyDecomposition;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector;
import maspack.matrix.VectorNd;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;
import maspack.util.TestException;

class SpatialInertiaTest {
   Random randGen = new Random();

   private double EPS = 1e-9;

   SpatialInertiaTest() {
      randGen.setSeed (0x1234);
      RandomGenerator.setSeed (0x1234);
   }

   private void checkEqual (String msg, Matrix M, Matrix Mcheck, double eps) {
      if (!Mcheck.epsilonEquals (M, eps)) {
         throw new TestException (msg + "\nExpected\n"
         + Mcheck.toString() + "\nGot\n" + M.toString());
      }
   }

   private void checkEqual (String msg, Vector v, Vector vcheck, double eps) {
      if (!vcheck.epsilonEquals (v, eps)) {
         throw new TestException (msg + "\nExpected\n"
         + vcheck.toString() + "\nGot\n" + v.toString());
      }
   }

   private void checkComponents (
      String msg, SpatialInertia M, double massCheck, Point3d comCheck,
      SymmetricMatrix3d JCheck) {
      SymmetricMatrix3d J = new SymmetricMatrix3d();
      Point3d com = new Point3d();

      double mass = M.getMass();
      M.getCenterOfMass (com);
      M.getRotationalInertia (J);

      if (Math.abs (mass - massCheck) > EPS) {
         throw new TestException (msg + "\ngetMass(): expected " + massCheck
         + " but got " + mass);
      }
      if (!com.epsilonEquals (comCheck, EPS)) {
         throw new TestException (msg + "\ngetCenterOfMass(): expected "
         + comCheck + "\ngot " + com);
      }
      if (!J.epsilonEquals (JCheck, EPS)) {
         throw new TestException (msg + "\ngetRotationalInertia(): expected\n"
         + JCheck + "\nGot\n" + J);
      }
      MatrixNd MM = new MatrixNd (6, 6);
      MatrixNd MCheck = new MatrixNd (6, 6);

      MM.set (M);
      for (int i = 0; i < 3; i++) {
         MCheck.set (i, i, mass);
      }
      MCheck.set (0, 4, mass * com.z);
      MCheck.set (0, 5, -mass * com.y);
      MCheck.set (1, 5, mass * com.x);
      MCheck.set (1, 3, -mass * com.z);
      MCheck.set (2, 3, mass * com.y);
      MCheck.set (2, 4, -mass * com.x);

      MCheck.set (3, 3, J.m00 + mass * (com.z * com.z + com.y * com.y));
      MCheck.set (4, 4, J.m11 + mass * (com.z * com.z + com.x * com.x));
      MCheck.set (5, 5, J.m22 + mass * (com.y * com.y + com.x * com.x));
      MCheck.set (3, 4, J.m01 - mass * (com.x * com.y));
      MCheck.set (3, 5, J.m02 - mass * (com.x * com.z));
      MCheck.set (4, 5, J.m12 - mass * (com.z * com.y));

      for (int i = 1; i < 6; i++) {
         for (int j = 0; j < i; j++) {
            MCheck.set (i, j, MCheck.get (j, i));
         }
      }

      if (!MM.epsilonEquals (MCheck, EPS)) {
         throw new TestException (msg + "\nMatrix: expected\n" + MCheck
         + "\nGot\n" + MM);
      }
   }

   public void test() throws Exception {
      SpatialInertia M1 = new SpatialInertia();
      SpatialInertia M2 = new SpatialInertia();
      SpatialInertia M3 = new SpatialInertia();

      double mass;
      Point3d com = new Point3d();
      SymmetricMatrix3d J = new SymmetricMatrix3d();
      Matrix3d F = new Matrix3d();

      for (int i = 0; i < 100; i++) {
         M1.setRandom (-0.5, 0.5, randGen);
         String s = M1.toString();
         M2.scan (new ReaderTokenizer (new StringReader (s)));

         checkEqual ("toString/scan MASS_INERTIA check", M2, M1, EPS);

         s = M1.toString ("%g", SpatialInertia.MATRIX_STRING);
         M2.scan (new ReaderTokenizer (new StringReader (s)));

         checkEqual ("toString/scan MATRIX_STRING check", M2, M1, EPS);

         MatrixNd MA = new MatrixNd (0, 0);
         MatrixNd MB = new MatrixNd (0, 0);

         MA.set (M1);
         M2.set (MA);

         mass = randGen.nextDouble();
         com.setRandom (-0.5, 0.5, randGen);
         F.setRandom (-0.5, 0.5, randGen);
         J.mulTransposeLeft (F);

         M1.setMass (mass);
         M1.setCenterOfMass (com);
         M1.setRotationalInertia (J);

         checkComponents ("set component check", M1, mass, com, J);
         M2.set (M1);
         checkComponents ("set check", M2, mass, com, J);

         M1.set (mass, J, com);
         checkComponents ("set check", M1, mass, com, J);

         M1.set (mass, J);
         com.setZero();
         checkComponents ("set check", M1, mass, com, J);

         M1.setRandom();
         MA.set (M1);
         MB.set (M2);

         // check addition

         M2.add (M1);
         MB.add (MA);
         M3.set (MB);
         checkEqual ("add check", M2, M3, EPS);

         // check subtraction

         MB.sub (MA);
         M2.sub (M1);
         M3.set (MB);
         checkEqual ("sub check", M2, M3, EPS);

         // check scaling

         MB.scale (3);
         M2.scale (3);
         M3.set (MB);
         checkEqual ("scale check", M2, M3, EPS);

         MatrixNd invM2 = new MatrixNd (0, 0);
         MatrixNd invMB = new MatrixNd (0, 0);

         // check inversion

         MB.set (M2);
         M2.getInverse (invM2);
         invMB.invert (MB);

         double eps = invM2.frobeniusNorm() * EPS;
         checkEqual ("inverse check", invM2, invMB, eps);

         Matrix6d invM6 = new Matrix6d();
         SpatialInertia.invert (invM6, M2);

         checkEqual ("inverse check", invM6, invMB, eps);

         CholeskyDecomposition chol = new CholeskyDecomposition();
         chol.factor (MB);
         MatrixNd U = new MatrixNd (6, 6);
         MatrixNd L = new MatrixNd (6, 6);
         chol.get (L);
         U.transpose (L);

         MatrixNd invU = new MatrixNd (6, 6);
         MatrixNd invL = new MatrixNd (6, 6);
         invU.invert (U);
         invL.transpose (invU);

         VectorNd vec = new VectorNd (6);
         VectorNd res = new VectorNd (6);
         Twist tw1 = new Twist();
         Twist twr = new Twist();
         Twist twrCheck = new Twist();
         Wrench wr1 = new Wrench();
         Wrench wrr = new Wrench();
         Wrench wrrCheck = new Wrench();

         // check multiply by vector (twist)

         tw1.setRandom();
         M2.mul (wrr, tw1);

         vec.set (tw1);
         MB.mul (res, vec);
         wrrCheck.set (res);

         checkEqual ("mul check", wrr, wrrCheck, EPS);

         // check inverse multiply by vector (wrench)

         wr1.setRandom();
         M2.mulInverse (twr, wr1);

         vec.set (wr1);
         invMB.mul (res, vec);
         twrCheck.set (res);

         checkEqual ("inverse mul check", twr, twrCheck, eps);

         // check inverse multiply by vector (twist, twist)

         wr1.setRandom();
         twr.set (wr1);
         M2.mulInverse (twr, twr);

         vec.set (wr1);
         invMB.mul (res, vec);
         twrCheck.set (res);

         checkEqual ("inverse mul check", twr, twrCheck, eps);

         // check right factor multiply by vector

         tw1.setRandom();
         M2.mulRightFactor (twr, tw1);

         vec.set (tw1);
         U.mul (res, vec);
         twrCheck.set (res);

         checkEqual ("mul right factor", twr, twrCheck, EPS);

         M2.mulRightFactor (tw1, tw1);

         checkEqual ("mul right factor (twr == tw1)", tw1, twrCheck, EPS);

         // check right factor inverse multiply by vector

         tw1.setRandom();
         M2.mulRightFactorInverse (twr, tw1);

         vec.set (tw1);
         invU.mul (res, vec);
         twrCheck.set (res);

         checkEqual ("mul right factor inverse", twr, twrCheck, eps);

         M2.mulRightFactorInverse (tw1, tw1);

         checkEqual ("mul right factor inverse (twr == tw1)", tw1, twrCheck, eps);

         // check left factor multiply by vector

         wr1.setRandom();
         M2.mulLeftFactor (wrr, wr1);

         vec.set (wr1);
         L.mul (res, vec);
         wrrCheck.set (res);

         checkEqual ("mul left factor", wrr, wrrCheck, EPS);

         M2.mulLeftFactor (wr1, wr1);

         checkEqual ("mul left factor (wrr == wr1)", wr1, wrrCheck, EPS);

         // check left factor inverse multiply by vector

         wr1.setRandom();
         M2.mulLeftFactorInverse (wrr, wr1);

         vec.set (wr1);
         invL.mul (res, vec);
         wrrCheck.set (res);

         checkEqual ("mul left factor inverse", wrr, wrrCheck, eps);

         M2.mulLeftFactorInverse (wr1, wr1);

         checkEqual ("mul left factor inverse (wrr == wr1)", wr1, wrrCheck, eps);

         // check addition of point mass

         Matrix6d M6 = new Matrix6d();

         SpatialInertia MCheck = new SpatialInertia();
         MCheck.set (M1);
         M6.set (M1);

         Point3d com1 = new Point3d();
         double mass1 = randGen.nextDouble();
         com1.setRandom (-0.5, 0.5, randGen);

         Point3d com2 = new Point3d();
         double mass2 = randGen.nextDouble();
         com2.setRandom (-0.5, 0.5, randGen);

         M3.setPointMass (mass1, com1);
         MCheck.add (M3);
         M3.setPointMass (mass2, com2);
         MCheck.add (M3);

         M1.addPointMass (mass1, com1);
         M1.addPointMass (mass2, com2);

         checkEqual ("point mass addition", M1, MCheck, EPS);

         SpatialInertia.addPointMass (M6, mass1, com1);
         SpatialInertia.addPointMass (M6, mass2, com2);

         Matrix6d MM = new Matrix6d();
         MM.sub (MCheck, M6);

         checkEqual ("point mass addition", M6, MCheck, EPS);

         // check rotation transforms

         RotationMatrix3d R = new RotationMatrix3d();
         SpatialInertia MSave = new SpatialInertia (M1);
         R.setRandom();

         M1.getRotated (M6, R);
         MCheck.set (M1);
         MCheck.transform (R);
         checkEqual ("getRotated", M6, MCheck, EPS);

         MCheck.inverseTransform (R);
         checkEqual ("inverseTransform", MCheck, MSave, EPS);

         R.transpose();
         M1.set (M6);
         M1.getRotated (M6, R);
         // should be back to original
         checkEqual ("getRotated (back)", M6, MSave, EPS);
         
      }

//      // test creation of inertia from a mesh
//      double density = 20;
//      double wx = 3.0;
//      double wy = 1.0;
//      double wz = 2.0;
//      SpatialInertia boxInertia =
//         SpatialInertia.createBoxInertia (20 * wx * wy * wz, wx, wy, wz);
//      PolygonalMesh boxMesh = MeshFactory.createQuadBox (wx, wy, wz);
//      SpatialInertia meshInertia =
//         SpatialInertia.createClosedVolumeInertia (boxMesh, density);
//
//      checkMeshInertia (boxMesh, density, boxInertia);
//
//      SpatialInertia innerBoxInertia =
//         SpatialInertia.createBoxInertia (1.0 * density, 1, 1, 1);
//      boxInertia.setBox (20 * wx * wy * wz, wx, wy, wz);
//      boxInertia.sub (innerBoxInertia);
//      boxMesh = buildMeshWithHole (wx, wy, wz);
//
//      checkMeshInertia (boxMesh, density, boxInertia);
//
//      boxMesh.triangulate();
//      checkMeshInertia (boxMesh, density, boxInertia);
   }

//   void checkMeshInertia (
//      PolygonalMesh mesh, double density, SpatialInertia inertia) {
//      PolygonalMesh testMesh = new PolygonalMesh (mesh);
//      SpatialInertia testInertia = new SpatialInertia (inertia);
//
//      RigidTransform3d X = new RigidTransform3d();
//
//      for (int i = 0; i < 10; i++) {
//         X.setRandom();
//         testMesh.transform (X);
//         testInertia.transform (X);
//
//         SpatialInertia meshInertia =
//            SpatialInertia.createClosedVolumeInertia (testMesh, density);
//
//         if (!testInertia.epsilonEquals (meshInertia, 1e-8)) {
//            System.out.println ("inertia computed from mesh:\n"
//            + meshInertia.toString ("%8.3f"));
//            System.out.println ("reference inertia:\n"
//            + testInertia.toString ("%8.3f"));
//            throw new TestException ("incorrect inertia created from mesh");
//         }
//      }
//   }

//   private PolygonalMesh buildMeshWithHole (double wx, double wy, double wz) {
//      PolygonalMesh mesh = new PolygonalMesh();
//      mesh.addVertex (wx / 2, wy / 2, wz / 2);
//      mesh.addVertex (wx / 2, wy / 2, -wz / 2);
//      mesh.addVertex (-wx / 2, wy / 2, -wz / 2);
//      mesh.addVertex (-wx / 2, wy / 2, wz / 2);
//
//      mesh.addVertex (wx / 2, -wy / 2, wz / 2);
//      mesh.addVertex (wx / 2, -wy / 2, -wz / 2);
//      mesh.addVertex (-wx / 2, -wy / 2, -wz / 2);
//      mesh.addVertex (-wx / 2, -wy / 2, wz / 2);
//
//      mesh.addVertex (1 / 2., 1 / 2., 1 / 2.);
//      mesh.addVertex (1 / 2., 1 / 2., -1 / 2.);
//      mesh.addVertex (-1 / 2., 1 / 2., -1 / 2.);
//      mesh.addVertex (-1 / 2., 1 / 2., 1 / 2.);
//
//      mesh.addVertex (1 / 2., -1 / 2., 1 / 2.);
//      mesh.addVertex (1 / 2., -1 / 2., -1 / 2.);
//      mesh.addVertex (-1 / 2., -1 / 2., -1 / 2.);
//      mesh.addVertex (-1 / 2., -1 / 2., 1 / 2.);
//
//      // top
//      mesh.addFace (new int[] { 0, 1, 9, 8 });
//      mesh.addFace (new int[] { 1, 2, 10, 9 });
//      mesh.addFace (new int[] { 2, 3, 11, 10 });
//      mesh.addFace (new int[] { 3, 0, 8, 11 });
//
//      // bottom
//      mesh.addFace (new int[] { 12, 13, 5, 4 });
//      mesh.addFace (new int[] { 13, 14, 6, 5 });
//      mesh.addFace (new int[] { 14, 15, 7, 6 });
//      mesh.addFace (new int[] { 15, 12, 4, 7 });
//
//      // inner sides
//      mesh.addFace (new int[] { 12, 8, 9, 13 });
//      mesh.addFace (new int[] { 13, 9, 10, 14 });
//      mesh.addFace (new int[] { 14, 10, 11, 15 });
//      mesh.addFace (new int[] { 15, 11, 8, 12 });
//
//      // outer sides
//      mesh.addFace (new int[] { 0, 4, 5, 1 });
//      mesh.addFace (new int[] { 1, 5, 6, 2 });
//      mesh.addFace (new int[] { 2, 6, 7, 3 });
//      mesh.addFace (new int[] { 3, 7, 4, 0 });
//      return mesh;
//   }

   public static void main (String[] args) {
      SpatialInertiaTest tester = new SpatialInertiaTest();

      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
