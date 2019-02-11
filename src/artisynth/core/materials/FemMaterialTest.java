package artisynth.core.materials;

import maspack.matrix.*;
import maspack.util.*;

/**
 * Tests FemMaterials by checking that the computed tangent matrix is
 * consistent with the value determined via numeric differentiation.
 *
 * Background material is provided in Bonet and Wood, "Nonlinear Continuum
 * Mechanics for Finite Element Analysis".
 */
public class FemMaterialTest extends UnitTest {

   double myPressure;
   double myExcitation;
   Matrix3d myF0;
   Matrix3d myQ;

   /**
    * Computes stress and tangent for either a FemMaterial or a MuscleMaterial,
    * using the supplied deformation gradient F and the default pressure,
    * excitation and direction (Q) values.
    */
   private void computeStressAndTangent (
      SymmetricMatrix3d sig, Matrix6d D, Matrix3d F, MaterialBase mat) {

      DeformedPointBase defp = new DeformedPointBase();
      defp.setF (F);
      defp.setAveragePressure (myPressure);
      
      if (mat instanceof FemMaterial) {
         ((FemMaterial)mat).computeStressAndTangent (
            sig, D, defp, myQ, 1.0);
      }
      else if (mat instanceof MuscleMaterial) {
         Vector3d dir = new Vector3d();
         myQ.getColumn (2, dir);
         ((MuscleMaterial)mat).computeStressAndTangent (
            sig, D, defp, dir, myExcitation);
      }
      else {
         throw new InternalErrorException (
            "Unsupported material type " + mat);
      }
   }

   FemMaterialTest() {
      // construct a default deformation gradient with det(F) > 0
      myF0 = new Matrix3d();
      myF0.set (new double[] {
            -0.06333731511461183, 0.4587337414699486, 0.22892041955795006,
            0.30779454389657457, 0.052508964600745056, 0.28357628272502233,
            0.48120609016230553, -0.13832821084284175, -0.2891920031945512 
         });
      // set up default pressure, excitaion and direction values
      myPressure = 1000;
      myExcitation = 0.33;
      myQ = new Matrix3d();
      RotationMatrix3d R = new RotationMatrix3d();
      R.setRandom();
      myQ.set (R);
   }

   // Compute a deformation increment appropriate to column j of the tangent
   // matrix. Note that the quantities for columns 3-5 are scaled by 0.5,
   // as required by Voigt notation.
   private void computeD (SymmetricMatrix3d d, int j, double h) {
      d.setZero();
      switch (j) {
         case 0: d.set (h, 0, 0, 0,   0,   0); break;
         case 1: d.set (0, h, 0, 0,   0,   0); break;
         case 2: d.set (0, 0, h, 0,   0,   0); break;
         case 3: d.set (0, 0, 0, h/2, 0,   0); break;
         case 4: d.set (0, 0, 0, 0,   0,   h/2); break;
         case 5: d.set (0, 0, 0, 0,   h/2, 0); break;
      }
   }

   // Sets column j of the numeric tangent matrix, using the entries of
   // Truesdell stress increment sigo.
   private void setColumn (Matrix6d D, Matrix3d sigo, int j) {
      VectorNd col = new VectorNd(6);
      col.set (0, sigo.m00);
      col.set (1, sigo.m11);
      col.set (2, sigo.m22);
      col.set (3, sigo.m01);
      col.set (4, sigo.m12);
      col.set (5, sigo.m02);
      D.setColumn (j, col);
   }

   /**
    * Tests a material by comparing the computed tangent matrix with the
    * numeric derivative of the same.
    *
    * @param mat material to test
    * @param tol tolerance by which the computed and numeric tangents
    * should match. Typically, this should be around 1e-8.
    */
   public void testMaterial (MaterialBase mat, double tol) {

      double h = 1e-8; // increment for computing numeric derivative

      SymmetricMatrix3d stress0 = new SymmetricMatrix3d();
      Matrix6d D = new Matrix6d();
      Matrix6d Dnumeric = new Matrix6d();
      Matrix3d F = new Matrix3d();

      // Start by computing the initial stress (stored as stress0) and tangent
      // for the base deformation gradient myF0.
      computeStressAndTangent (stress0, D, myF0, mat);

      // Compute the tangent numerically by applying deformation increments to
      // the existing gradient F and examining how the stress varies from the
      // initial stress0.
      //
      // The tangent matrix is the spatial elasticity tensor, which relates the
      // rate of deformation d to the Truesdell stress rate sig0 (Bonet and
      // Wood, 2nd edition, Section 6.3.2). Thus to compute each column of the
      // tangent matrix, we determine a small deformation increment d
      // appropriate to the column, use this to adjust F, compute the resulting
      // stress, and use this to determine a Truesdell stress increment. Note
      // that the Truesdell stress increment is not simply
      // stress-stress0. Instead, it is given by
      //
      // sig0 = (stress-stress0) - d*stress0 - stress0*d + (trace(d))*stress0.
      //
      // See Bonet and Woodm, 2nd edition, equation 5.56. That equation uses
      // the velocity gradient tensor l instead of d, but in our case these are
      // identical by construction.
      for (int j=0; j<6; j++) {
         SymmetricMatrix3d d = new SymmetricMatrix3d();
         SymmetricMatrix3d stress = new SymmetricMatrix3d();
         Matrix3d dF = new Matrix3d();
         Matrix3d tmp = new Matrix3d();
         Matrix3d dsig = new Matrix3d();
         Matrix3d sigd = new Matrix3d();
         Matrix3d sigo = new Matrix3d();

         computeD (d, j, h); // deformation increment appropriate to the column
         dF.mul (d, myF0); // compute resulting change in F 
         F.add (myF0, dF);

         // compute resulting stress from upated F
         computeStressAndTangent (stress, null, F, mat);

         // compute Truesdell stress increment sig0 from stress and stress0
         dsig.mul (d, stress0);
         sigd.mul (stress0, d);
         sigo.sub (stress, stress0);
         sigo.sub (dsig);
         sigo.sub (sigd);
         sigo.scaledAdd (d.trace(), stress0);

         // complete numeric differentiation by dividing by h, and the
         // the corresponding column of Dnumeric.
         sigo.scale (1/h);
         setColumn (Dnumeric, sigo, j);
      }

      // Find the differene between the computed and numeric tangent.
      // If the relative error exceeds the specified t
      Matrix6d Ddiff = new Matrix6d();
      Ddiff.sub (Dnumeric, D);
      if (Dnumeric.frobeniusNorm() == 0) {
         throw new TestException (
            "Numeric tangent evaluates to zero");
      }
      double err = Ddiff.frobeniusNorm()/Dnumeric.frobeniusNorm();

      if (err > tol || err != err) {
         System.out.println (
            "error between computed and numeric D is "+ err);
         System.out.println ("stress=\n" + stress0.toString("%14.3f"));
         System.out.println ("D computed=\n" + D.toString("%14.3f"));
         System.out.println ("D numeric=\n" + Dnumeric.toString("%14.3f"));
         System.out.println ("numeric-computed=\n" + Ddiff.toString("%14.3f"));
         if (err != err) {
            throw new TestException (
               "Error between numeric and computed tangent is NaN");
         }
         else {
            throw new TestException (
               "Error between numeric and computed tangent > " + tol);
         }
      }
   }

   /**
    * Test method executed by runtest().
    */
   public void test () {

      // numeric and analytical tangents should match within this tolerance
      double tol = 1e-6;

      testMaterial (new NeoHookeanMaterial(10000.0, 0.49), tol);   
      testMaterial (
         new MooneyRivlinMaterial (1.2, 3.4, 0, 0, 0, 0), tol);
      testMaterial (new StVenantKirchoffMaterial (1234, 0.3), tol);
      testMaterial (new IncompNeoHookeanMaterial (123.0, 666.0), tol);

      testMaterial (
          new OgdenMaterial (
           new double[] {3e5, 1e4, 2e3, 3e3, 4e3, 1.5e2},
           new double[] {1, 2, 3, 4, 5, 6}, 1e5), tol);

      //testMaterial (new FungMaterial(), tol);

      testMaterial (new CubicHyperelastic(1000.0, 2000.0, 3000.0, 10000.0), tol);

      testMaterial (new GenericMuscle (1.4, 3e5, 0.05, 6.6), tol);
      testMaterial (new FullBlemkerMuscle (1.4, 1.0, 3e5, 0.05, 6.6, 0, 0), tol);
      testMaterial (new BlemkerMuscle (1.4, 1.0, 3e5, 0.05, 6.6), tol);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);

      FemMaterialTest tester = new FemMaterialTest();

      tester.runtest();
   }

}
