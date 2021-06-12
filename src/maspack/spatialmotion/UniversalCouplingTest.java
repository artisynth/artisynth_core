package maspack.spatialmotion;

import maspack.util.*;
import maspack.matrix.*;
import maspack.spatialmotion.UniversalCoupling.AxisSet;

public class UniversalCouplingTest extends UnitTest {

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

      for (AxisSet axes : AxisSet.values()) {
         
         UniversalCoupling coupling = new UniversalCoupling (skewAng, axes);
         int ntests = 100;
         VectorNd angs = new VectorNd(2);
         for (int i=0; i<ntests; i++) {

            // test set/getRollPitch
            double roll = toRange(RandomGenerator.nextDouble());
            double pitch = toRange(RandomGenerator.nextDouble());
            RotationMatrix3d R = new RotationMatrix3d();
            RigidTransform3d TGD = new RigidTransform3d();
            angs.set (0, roll);
            angs.set (1, pitch);
            coupling.setCoordinateValues (angs, TGD);
            coupling.getCoordinates (angs, TGD);
            if (Math.abs(toRange(angs.get(0))-roll) > 1e-14) {
               System.out.println ("roll in:  " + roll);
               System.out.println ("roll out: " + toRange(angs.get(0)));
               throw new TestException (
                  "Roll angle not recovered, skew=" + skewAng);
            }
            if (Math.abs(toRange(angs.get(1))-pitch) > 1e-14) {
               System.out.println ("pitch in:  " + pitch);
               System.out.println ("pitch out: " + toRange(angs.get(1)));
               throw new TestException (
                  "Pitch angle not recovered, skew=" + skewAng);
            }
            // test projection
            RigidTransform3d TCD = new RigidTransform3d();
            TCD.R.setRandom();
            //TGD.set (TCD);
            coupling.projectToConstraints (TGD, TCD, null);
            coupling.TCDToCoordinates (angs, TGD);
            // see if we can reconstruct projected transform with the angs
            RigidTransform3d TCHK = new RigidTransform3d();
            coupling.setCoordinateValues (angs, TCHK);
            if (!TGD.R.epsilonEquals (TCHK.R, 1e-14)) {
               throw new TestException (
                  "Projection failed, skewAngle=" + skewAng);
            }
         } 
      }
      
   }

   public void test() {
      //test (0);
      test (0.10);
      test (Math.toRadians(-23.0));
      test (Math.toRadians(45.0));
      test (Math.toRadians(-80.0));
      test (Math.toRadians(11.0));
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      UniversalCouplingTest tester = new UniversalCouplingTest();
      tester.runtest();
   }

}
