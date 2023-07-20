package maspack.spatialmotion;

import maspack.util.*;
import maspack.matrix.*;

public class EllipsoidCouplingTest extends UnitTest {

   private double RTOD = 180/Math.PI;
   private double DTOR = Math.PI/180;

   private double DOUBLE_PREC = 1e-16;
   private double EPS = 1000*DOUBLE_PREC;

   void checkCoordinateJacobian (
      EllipsoidCoupling coupling, VectorNd coords0) {

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
      if (!J.epsilonEquals (Jnum, 5e-7)) {
         System.out.println ("J=\n" + J.toString ("%12.7f"));
         System.out.println ("Jnum=\n" + Jnum.toString ("%12.7f"));
         Jnum.sub (J);
         System.out.println ("Err=\n" + Jnum.toString ("%12.7f"));
         throw new TestException (
            "Bad constraint matrix for coords " + coords0.toString("%12.8f"));
      }
   }

   void testKinematics (
      EllipsoidCoupling coupling, VectorNd coordsChk) {

      VectorNd coords = new VectorNd(coordsChk.size());
      RigidTransform3d TCD = new RigidTransform3d();
      coupling.coordinatesToTCD (TCD, coordsChk);
      coupling.setCoordinateValues (coordsChk, null);
      coupling.TCDToCoordinates (coords, TCD);

      checkEquals ("testCoordinateSolutions", coords, coordsChk, 1e-12);
      checkCoordinateJacobian (coupling, coordsChk);
   }

   private void setRandomCoords (VectorNd coords) {
      coords.set (0, RandomGenerator.nextDouble(-Math.PI, Math.PI));
      coords.set (1, RandomGenerator.nextDouble(-Math.PI/2, Math.PI/2));
      coords.set (2, RandomGenerator.nextDouble(-Math.PI, Math.PI));
      coords.set (3, 0); //RandomGenerator.nextDouble(-Math.PI, Math.PI));
   }

   void testProjection (EllipsoidCoupling coupling) {
      RigidTransform3d TCD = new RigidTransform3d();
      RigidTransform3d TGD = new RigidTransform3d();
      RigidTransform3d ERR = new RigidTransform3d();
      VectorNd coords = new VectorNd (coupling.numCoordinates());

      setRandomCoords (coords);
      coupling.coordinatesToTCD (TCD, coords);
      coupling.projectToConstraints (TGD, TCD, coords);
      ERR.mulInverseLeft (TGD, TCD);
      checkEquals (
         "TGD projection error, random coords", ERR,
         RigidTransform3d.IDENTITY, 1000*DOUBLE_PREC);

      TCD.setRandom();
      coupling.projectToConstraints (TGD, TCD, coords);
      coupling.coordinatesToTCD (ERR, coords);
      ERR.mulInverseLeft (TGD, ERR);
      checkEquals (
         "TGD projection error, random TCD", ERR,
         RigidTransform3d.IDENTITY, 1000*DOUBLE_PREC);
   }

   void testCoupling (double alpha, boolean useOpenSimApprox) {

      int ntests = 100;
      double size = 0.5;
      EllipsoidCoupling coupling = 
         new EllipsoidCoupling (size/2, size/3, size/4, alpha, useOpenSimApprox);

      VectorNd coordsChk = new VectorNd(4);
      testKinematics (coupling, coordsChk);
      // check at the singularity
      coordsChk.set (1, Math.PI/2);
      testKinematics (coupling, coordsChk);

      for (int i=0; i<ntests; i++) {
         setRandomCoords (coordsChk);
         testKinematics (coupling, coordsChk);
      }
      for (int i=0; i<ntests; i++) {
         testProjection (coupling);
      }
   }

   void testSpecial() {
      double size = 0.5;
      EllipsoidCoupling coupling = 
         new EllipsoidCoupling (
            size/2, size/3, size/4,
            Math.toRadians(30), false);
      testProjection (coupling);
      
   }

   public void test() {
      double alpha = 0;
      testCoupling (alpha, true);
      testCoupling (alpha, false);
      int nalphaTests = 10;
      for (int i=0; i<nalphaTests; i++) {
         alpha = RandomGenerator.nextDouble(-Math.PI, Math.PI);
         testCoupling (alpha, true);
         testCoupling (alpha, false);
      }
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      EllipsoidCouplingTest tester = new EllipsoidCouplingTest();
      tester.runtest();
   }

}
