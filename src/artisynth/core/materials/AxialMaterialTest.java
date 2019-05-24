package artisynth.core.materials;

import maspack.matrix.*;
import maspack.util.*;

/**
 * Tests AxialMaterials by checking that their computed derivatives are
 * consistent with the values determined via numeric differentiation.
 */
public class AxialMaterialTest extends UnitTest {

   AxialMaterialTest() {
   }

   /**
    * Tests a material by comparing the computed derivatives with the numeric
    * derivative of the same.
    *
    * @param mat material to test
    * @param tol tolerance by which the computed and numeric derivatives should
    * match. Typically, this should be around 1e-8.
    */
   public void testMaterial (AxialMaterial mat, double l0, double tol) {

      double l = 2*l0;
      double ldot = 0.1*l;
      double ex = 0.5;

      double h = 1e-8;

      double F = mat.computeF (l, ldot, l0, ex);
      double dFdl = mat.computeDFdl (l, ldot, l0, ex);
      double dFdldot = mat.computeDFdldot (l, ldot, l0, ex);

      double dFdlChk = (mat.computeF (l+h*l, ldot, l0, ex) - F)/(h*l);
      double dFdldotChk = (mat.computeF (l, ldot+h*ldot, l0, ex) - F)/(h*ldot);

      double mag = Math.abs(dFdl);
      double err = Math.abs(dFdl-dFdlChk);
      if (mag > 0) {
         err /= mag;
      }
      if (err > tol || err != err) {
         System.out.println (
            "error between computed and numeric dFdl is "+ err);
         System.out.println ("F=" + F);
         System.out.println ("dFdl computed=" + dFdl);
         System.out.println ("dFdl numeric=" + dFdlChk);
         if (err != err) {
            throw new TestException (
               "Error between numeric and computed dFdl is NaN");
         }
         else {
            throw new TestException (
               "Error between numeric and computed dFdl > " + tol);
         }
      }

      mag = Math.abs(dFdldot);
      err = Math.abs(dFdldot-dFdldotChk);
      if (mag > 0) {
         err /= mag;
      }
      if (err > tol || err != err) {
         System.out.println (
            "error between computed and numeric dFdldot is "+ err);
         System.out.println ("F=" + F);
         System.out.println ("dFdldot computed=" + dFdldot);
         System.out.println ("dFdldot numeric=" + dFdldotChk);
         if (err != err) {
            throw new TestException (
               "Error between numeric and computed dFdldot is NaN");
         }
         else {
            throw new TestException (
               "Error between numeric and computed dFdldot > " + tol);
         }
      }
   }

   /**
    * Test method executed by runtest().
    */
   public void test () {

      // numeric and analytical tangents should match within this tolerance
      double tol = 1e-6;

      testMaterial (new LinearAxialMaterial(1000.0, 200.0), 1.0, tol);   
      testMaterial (new SimpleAxialMuscle (1000.0, 200.0, 300), 1.0, tol);
      testMaterial (new LigamentAxialMaterial (1234.00, 222.0, 333.0), 2.0, tol);
      testMaterial (new ConstantAxialMuscle (20.0), 2.0, tol);
      testMaterial (new ConstantAxialMaterial (222.0), 2.0, tol);
      testMaterial (new LinearAxialMuscle (20, 2.0), 2.0, tol);
      testMaterial (new PeckAxialMuscle (20.0, 2.0, 3.0, 0.2, 0.5, 0.1), 2.0, 1e-5);
      testMaterial (new PaiAxialMuscle (20.0, 2.0, 3.0, 0.2, 0.5, 0.1), 2.0, 1e-5);
      testMaterial (new BlemkerAxialMuscle (1.4, 1.0, 3e5, 0.05, 6.6), 2.0, tol);
      testMaterial (new MasoudMillardLAM (2.0, 1.8, 0.5), 2.0, tol);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);

      AxialMaterialTest tester = new AxialMaterialTest();

      tester.runtest();
   }

}
