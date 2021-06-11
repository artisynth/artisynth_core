package maspack.spatialmotion;

import maspack.util.*;
import maspack.matrix.*;
import maspack.spatialmotion.GimbalCoupling.AxisSet;

public class GimbalCouplingTest extends UnitTest {

   void testRpySolutions() {

      int ntests = 100;

      RotationMatrix3d R = new RotationMatrix3d();
      RotationMatrix3d RX = new RotationMatrix3d();
      for (int i=0; i<ntests; i++) {
         double[] rpy = new double[3];

         for (AxisSet axes : AxisSet.values()) {

            rpy[0] = RandomGenerator.nextDouble(-Math.PI, Math.PI);
            rpy[1] = RandomGenerator.nextDouble(-Math.PI/2, Math.PI/2);
            rpy[2] = RandomGenerator.nextDouble(-Math.PI, Math.PI);
            
            GimbalCoupling.setRpy (R, rpy[0], rpy[1], rpy[2], axes);
            
            rpy[0] += Math.PI;
            rpy[1] = Math.PI - rpy[1];
            rpy[2] += Math.PI;
            GimbalCoupling.setRpy (RX, rpy[0], rpy[1], rpy[2], axes);
            
            checkEquals ("testRpySolutions", R, RX, 1e-13);
            
            GimbalCoupling.getRpy (rpy, R, axes);
            GimbalCoupling.setRpy (RX, rpy[0], rpy[1], rpy[2], axes);
            
            checkEquals ("get/setRpy", R, RX, 1e-13);
         }
      }
   }


   public void test() {
      testRpySolutions();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      GimbalCouplingTest tester = new GimbalCouplingTest();
      tester.runtest();
   }

}
