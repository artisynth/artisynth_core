package maspack.collision;

import maspack.matrix.*;
import maspack.util.*;

/**
 * Test method for the winding number calculator.
 */
public class WindingCalculatorTest extends UnitTest {

   WindingCalculator myCalc;

   WindingCalculatorTest() {
      myCalc = new WindingCalculator();
   }

   public void test (double x, double y, int check, double[] poly) {
      test (x, y, check, poly, new Vector3d (1, 0, 0), new RigidTransform3d());

      int randomRayCnt = 20;
      int randomTransCnt = 100;
      for (int i=0; i<randomTransCnt; i++) {
         RigidTransform3d T = new RigidTransform3d();
         T.setRandom();
         for (int j=0; j<randomRayCnt; j++) {
            Vector3d ray = new Vector3d();
            ray.setRandom();
            ray.z = 0;
            ray.normalize();
            test (x, y, check, poly, ray, T);
         }
      }
   }

   public void test (
      double x, double y, int check, double[] poly, Vector3d ray,
      RigidTransform3d T) {

      Point3d pnt = new Point3d (x, y, 0);
      pnt.transform (T);

      int nump = poly.length/2;
      Point3d[] polyPnts = new Point3d[nump];
      for (int i=0; i<nump; i++) {
         polyPnts[i] = new Point3d (poly[2*i], poly[2*i+1], 0);
         polyPnts[i].transform (T);
      }
      Vector3d rayt = new Vector3d(ray);
      rayt.transform (T);
      Vector3d nrm = new Vector3d();
      T.R.getColumn (2, nrm);
      myCalc.init (rayt, nrm);
      myCalc.start (pnt, polyPnts[nump-1]);
      for (int i=0; i<nump; i++) {
         myCalc.advance (polyPnts[i]);
      }
      if (myCalc.getNumber() != check) {
         throw new TestException (
            "Got wrapping number "+myCalc.getNumber()+", expected "+check);
      }
   }

   double[] myTriangle = new double[] {
      1.0, 1.0,  -1.0, 0.0,  1.0, -1.0 };

   double[] myM = new double[] {
      1.0, 1.0,    0.0, -0.5,  -0.9, 1.0,   -1.0, -1.0,  -0.3, -1.0, -0.5, -0.3, 
      0.0, -1.0,   0.5, -0.3,   0.3, -1.0,  1.0, -1.0 };

   double[] myClippedH = new double[] {
      -3, 0,  -3, 1,  3, 4,  3, 0,  1, 0,  1, 3,  -1, 2,  -1, 0 };

   public void test() {

      // testing triangle inside

      test ( 0.0,     0.0,     1, myTriangle);
      test (-0.9999,  0.0,     1, myTriangle);
      test ( 0.9999,  0.9999,  1, myTriangle);
      test ( 0.9999, -0.9999,  1, myTriangle);

      // testing triangle just outside each corner

      //test (-1.0001,  0.0,     0, myTriangle);
      test (-1.0000,  0.0001,  0, myTriangle);
      test (-1.0000, -0.0001,  0, myTriangle);
      test ( 1.0001,  1.0001,  0, myTriangle);
      test ( 0.9999,  1.0000,  0, myTriangle);
      test ( 1.0001, -1.0001,  0, myTriangle);
      test ( 0.9999, -1.0001,  0, myTriangle);

      // testing M just inside each corner

      test ( 0.9999,  0.9998,  1, myM);
      test ( 0.0,    -0.5001,  1, myM);
      test ( 0.0001, -0.5000,  1, myM);
      test (-0.0001, -0.5000,  1, myM);
      test (-0.8999,  0.9998,  1, myM);
      test (-0.9999, -0.9999,  1, myM);
      test (-0.3001, -0.9999,  1, myM);
      test (-0.5001, -0.2999,  1, myM);
      test (-0.5001, -0.3000,  1, myM);
      test (-0.5000, -0.2999,  1, myM);
      test (-0.4999, -0.3000,  1, myM);
      test (-0.5000, -0.3001,  1, myM);
      test ( 0.0,    -0.9999,  1, myM);
      test ( 0.5001, -0.2999,  1, myM);
      test ( 0.5001, -0.3000,  1, myM);
      test ( 0.5000, -0.2999,  1, myM);
      test ( 0.4999, -0.3000,  1, myM);
      test ( 0.5000, -0.3001,  1, myM);
      test ( 0.3001, -0.9999,  1, myM);
      test ( 0.9999, -0.9999,  1, myM);

      // testing M just outside each corner      

      test ( 1.0001,  1.0001,  0, myM);
      test ( 1.0001,  1.0000,  0, myM);
      test ( 1.0000,  1.0001,  0, myM);
      test ( 0.9999,  1.0000,  0, myM);
      test ( 0.0,    -0.4999,  0, myM);
      test (-0.9001,  1.0001,  0, myM);
      test (-0.9001,  1.0000,  0, myM);
      test (-0.9000,  1.0001,  0, myM);
      test (-0.8999,  1.0000,  0, myM);
      test (-1.0001, -0.9999,  0, myM);
      test (-1.0001, -1.0000,  0, myM);
      test (-1.0001, -1.0001,  0, myM);
      test (-1.0000, -1.0001,  0, myM);
      test (-0.9999, -1.0001,  0, myM);
      test (-0.2999, -0.9999,  0, myM);
      test (-0.2999, -1.0000,  0, myM);
      test (-0.2999, -1.0001,  0, myM);
      test (-0.3000, -1.0001,  0, myM);
      test (-0.3001, -1.0001,  0, myM);
      test (-0.4999, -0.3002,  0, myM);
      test ( 0.0001, -1.0000,  0, myM);
      test ( 0.0,    -1.0001,  0, myM);
      test (-0.0001, -1.0000,  0, myM);
      test ( 0.4999, -0.3002,  0, myM);
      test ( 0.2999, -0.9999,  0, myM);
      test ( 0.2999, -1.0000,  0, myM);
      test ( 0.2999, -1.0001,  0, myM);
      test ( 0.3000, -1.0001,  0, myM);
      test ( 0.3001, -1.0001,  0, myM);
      test ( 1.0001, -0.9999,  0, myM);
      test ( 1.0001, -1.0000,  0, myM);
      test ( 1.0001, -1.0001,  0, myM);
      test ( 1.0000, -1.0001,  0, myM);
      test ( 0.9999, -1.0001,  0, myM);

      // testing an H shape clipped to a triangle: outside points

      test ( 0.0,     0.0,     0, myClippedH);
      test (-3.0001,  0.5,     0, myClippedH);
      test (-0.9999,  2.0,     0, myClippedH);
      test ( 0.9999,  2.9999,  0, myClippedH);
      test ( 0.9999,  0.0001,  0, myClippedH);
      test ( 3.0001,  4.0,     0, myClippedH);

      // testing an H shape clipped to a triangle: inside points
      // because this polygon is clockwise, winding number is -1

      test (-1.0001,  0.0001, -1, myClippedH);
      test (-2.9999,  0.5,    -1, myClippedH);
      test (-1.0001,  1.9998, -1, myClippedH);
      test ( 1.0001,  3.0000, -1, myClippedH);
      test ( 1.0001,  0.0001, -1, myClippedH);
      test ( 2.9999,  3.9999, -1, myClippedH);
   }

   public static void main (String[] args) {
      WindingCalculatorTest tester = new WindingCalculatorTest();

      RandomGenerator.setSeed (0x1234);

      tester.runtest();
   }

}
