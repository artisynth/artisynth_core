package maspack.spatialmotion;

import maspack.util.*;
import maspack.matrix.*;

public class EllipsoidCoupling3dTest extends UnitTest {

   private double RTOD = 180/Math.PI;
   private double DTOR = Math.PI/180;

   void checkCoordinateJacobian (
      EllipsoidCoupling3d coupling, VectorNd coords0) {

      RigidTransform3d TCD0 = new RigidTransform3d();
      coupling.coordinatesToTCD (TCD0, coords0);
      int numc = coupling.numCoordinates();

      MatrixNd J = coupling.computeCoordinateJacobian (TCD0);

      double h = 1e-8;

      // check J by computing a numeric Jacobian Jnum in the C frame
      MatrixNd Jnum = new MatrixNd (6, numc);
      VectorNd coords1 = new VectorNd (numc);
      RigidTransform3d TCD1 = new RigidTransform3d();
      RigidTransform3d TCD10 = new RigidTransform3d();
      for (int j=0; j<numc; j++) {
         coords1.set (coords0);
         coords1.set (j, coords0.get(j) + h);

         coupling.coordinatesToTCD (TCD1, coords1);
         TCD10.mulInverseLeft (TCD0, TCD1);
         
         Twist velC = new Twist();
         velC.set (TCD10);
         velC.scale (1/h);
         Jnum.set (0, j, velC.v.x);
         Jnum.set (1, j, velC.v.y);
         Jnum.set (2, j, velC.v.z);
         Jnum.set (3, j, velC.w.x);
         Jnum.set (4, j, velC.w.y);
         Jnum.set (5, j, velC.w.z);
      }
      if (!J.epsilonEquals (Jnum, 1e-7)) {
         System.out.println ("J=\n" + J.toString ("%12.7f"));
         System.out.println ("Jnum=\n" + Jnum.toString ("%12.7f"));
         Jnum.sub (J);
         System.out.println ("Err=\n" + Jnum.toString ("%12.7f"));
         throw new TestException (
            "Bad constraint matrix for coords " + coords0.toString("%12.8f"));
      }
   }

   void testSpecial () {
      RigidTransform3d TCD = new RigidTransform3d();
      double size = 5.0;
      EllipsoidCoupling3d coupling = 
         new EllipsoidCoupling3d (size/2, size/3, size/4, true);
      
      VectorNd coords = new VectorNd (new double[] { 90, -30, 0 });
      coords.scale (DTOR);
      coupling.coordinatesToTCD (TCD, coords);

      MatrixNd J = coupling.computeCoordinateJacobian (TCD);
      System.out.println ("J=\n" + J.toString("%14.8f"));
   }

   void testKinematics (
      EllipsoidCoupling3d coupling, VectorNd coordsChk) {

      VectorNd coords = new VectorNd(coordsChk.size());
      RigidTransform3d TCD = new RigidTransform3d();
      coupling.coordinatesToTCD (TCD, coordsChk);
      coupling.setCoordinateValues (coordsChk, null);
      coupling.TCDToCoordinates (coords, TCD);

      checkEquals ("testCoordinateSolutions", coords, coordsChk, 1e-12);
      checkCoordinateJacobian (coupling, coordsChk);
   }

   void testKinematics (boolean useOpenSimApprox) {

      int ntests = 100;
      double size = 0.5;
      EllipsoidCoupling3d coupling = 
         new EllipsoidCoupling3d (size/2, size/3, size/4, useOpenSimApprox);

      VectorNd coordsChk = new VectorNd(3);
      testKinematics (coupling, coordsChk);
      // check at the singularity
      coordsChk.set (1, Math.PI/2);
      testKinematics (coupling, coordsChk);

      checkCoordinateJacobian (coupling, new VectorNd(3));
      for (int i=0; i<ntests; i++) {

         coordsChk.set (0, RandomGenerator.nextDouble(-Math.PI, Math.PI));
         coordsChk.set (1, RandomGenerator.nextDouble(-Math.PI/2, Math.PI/2));
         coordsChk.set (2, RandomGenerator.nextDouble(-Math.PI, Math.PI));
         testKinematics (coupling, coordsChk);
      }
   }

   public void test() {
      //testSpecial();
      testKinematics (true);
      testKinematics (false);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      EllipsoidCoupling3dTest tester = new EllipsoidCoupling3dTest();
      tester.runtest();
   }

}
