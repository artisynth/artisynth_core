package maspack.spatialmotion;

import maspack.util.*;
import maspack.matrix.*;

public class RollPitchCouplingTest extends UnitTest {

   private double toRange (double ang) {
      while (ang >= Math.PI) {
         ang -= 2*Math.PI;
      }
      while (ang < -Math.PI) {
         ang += 2*Math.PI;
      }
      return ang;
   }

   private void test (double skewAng) {
      RollPitchCoupling rpc = new RollPitchCoupling (skewAng);

      int ntests = 10;
      double[] angs = new double[3];
      for (int i=0; i<ntests; i++) {

         // test set/getRollPitch
         double roll = toRange(RandomGenerator.nextDouble());
         double pitch = toRange(RandomGenerator.nextDouble());
         RotationMatrix3d R = new RotationMatrix3d();
         rpc.setRollPitch (R, roll, pitch);
         rpc.getRollPitch (angs, R);
         if (Math.abs(toRange(angs[0])-roll) > 1e-14) {
            System.out.println ("roll in:  " + roll);
            System.out.println ("roll out: " + toRange(angs[0]));
            throw new TestException (
               "Roll angle not recovered, skew=" + skewAng);
         }
         if (Math.abs(toRange(angs[1])-pitch) > 1e-14) {
            System.out.println ("pitch in:  " + pitch);
            System.out.println ("pitch out: " + toRange(angs[1]));
            throw new TestException (
               "Pitch angle not recovered, skew=" + skewAng);
         }

         // test projection
         // RigidTransform3d TCD = new RigidTransform3d();
         // RigidTransform3d TGD = new RigidTransform3d();
         // TCD.setRandom();
         // rpc.projectToConstraint (TGD, TCD);
         // RotationMatrix3d RDG = new RotationMatrix3d();
         // RotationMatrix3d RDC = new RotationMatrix3d();
         // RDG.transpose (TGD.R);
         // rpc.getRollPitch (angs, RDG);
         // rpc.setRollPitch (RDC, angs[0], angs[1]);
         // if (!RDG.epsilonEquals (RDC, 1e-14)) {
         //    System.out.println ("i=" + i);
         //    throw new TestException ("Projection failed, skewAngle=" + skewAng);
         // }

         RigidTransform3d TCD = new RigidTransform3d();
         RigidTransform3d TGD = new RigidTransform3d();
         TCD.R.transpose (R);
         rpc.projectToConstraint (TGD, TCD);
         if (!TGD.R.epsilonEquals (TCD.R, 1e-14)) {
            throw new TestException ("Projection failed, skewAngle=" + skewAng);
         }
      }      
   }

   public void test() {
      test (0);
      test (0.10);
      test (Math.toRadians(-23.0));
      test (Math.toRadians(45.0));
      test (Math.toRadians(-80.0));
      test (Math.toRadians(11.0));
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      RollPitchCouplingTest tester = new RollPitchCouplingTest();
      tester.runtest();
   }

}
