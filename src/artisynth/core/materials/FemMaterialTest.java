package artisynth.core.materials;

import java.util.*;
import maspack.matrix.*;
import maspack.util.*;

import artisynth.core.materials.IncompressibleMaterialBase.BulkPotential;

/**
 * Tests FemMaterials by checking that the computed tangent matrix is
 * consistent with the value determined via numeric differentiation.
 *
 * Background material is provided in Bonet and Wood, "Nonlinear Continuum
 * Mechanics for Finite Element Analysis".
 */
public class FemMaterialTest extends UnitTest {

   double myExcitation;
   Matrix3d myF0;
   Matrix3d myQ;
   ArrayList<Matrix3d> myFVals;
   ArrayList<Matrix3d> myIncompFVals;
   ArrayList<Matrix3d> myMuscleFVals;

   /**
    * Computes stress and tangent for either a FemMaterial or a MuscleMaterial,
    * using the supplied deformation gradient F and the default pressure,
    * excitation and direction (Q) values.
    */
   private void computeStressAndTangent (
      SymmetricMatrix3d sig, Matrix6d D, Matrix3d F,
      MaterialBase mat, double p) {

      DeformedPointBase defp = new DeformedPointBase();
      defp.setF (F);
      defp.setAveragePressure (p);

      // assume quadratic potention
      //defp.setAverageDetF (getJFromPressure (
      
      if (mat instanceof MuscleMaterial) {
         Vector3d dir = new Vector3d();
         myQ.getColumn (2, dir);
         ((MuscleMaterial)mat).computeStressAndTangent (
            sig, D, defp, dir, myExcitation, null);
      }
      else if (mat instanceof FemMaterial) {
         ((FemMaterial)mat).computeStressAndTangent (
            sig, D, defp, myQ, 1.0, null);
      }
      else {
         throw new InternalErrorException (
            "Unsupported material type " + mat);
      }
   }

   /**
    * Computes the strain energy density and stress for either a FemMaterial or
    * a MuscleMaterial, using the supplied deformation gradient F and the
    * default pressure, excitation=0, and direction (Q) values.
    */
   private double computeEnergyDensityAndStress (
      SymmetricMatrix3d stress, Matrix3d F, MaterialBase mat,
      double J, double p) {

      DeformedPointBase defp = new DeformedPointBase();
      if (mat instanceof IncompressibleMaterialBase) {
         IncompressibleMaterialBase imat = (IncompressibleMaterialBase)mat;
         J = F.determinant();
         p = imat.getEffectivePressure(imat.getBulkModulus(), J);
      }

      defp.setF (F);
      defp.setAveragePressure (p);
      defp.setAverageDetF (J);
      
      if (mat instanceof MuscleMaterial) {
         MuscleMaterial mmat = (MuscleMaterial)mat;
         Vector3d dir = new Vector3d();
         myQ.getColumn (2, dir);
         if (stress != null) {
            mmat.computeStressAndTangent (stress, null, defp, dir, 0.0, null);
         }
         return mmat.computeStrainEnergyDensity (defp, dir, 0.0, null);
      }
      else if (mat instanceof FemMaterial) {
         FemMaterial fmat = (FemMaterial)mat;
         if (stress != null) {
            fmat.computeStressAndTangent (stress, null, defp, myQ, 0.0, null);
         }
         return fmat.computeStrainEnergyDensity (defp, myQ, 0.0, null);
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
      // set up default excitation and direction values
      myExcitation = 0.33;
      myQ = new Matrix3d();
      RotationMatrix3d R = new RotationMatrix3d();
      R.setRandom();
      myQ.set (R);
      myFVals = new ArrayList<Matrix3d>();

      myFVals.add (randomF (1.0, 0.5, 0.4));
      //myFVals.add (randomF (1.0, 1.0, 1.0));
      myFVals.add (randomF (2.4, 1.0, 1.0));
      myFVals.add (randomF (2.4, 5.8, 0.9));
      myFVals.add (randomF (2.4, 2.4, 0.3));
      myFVals.add (randomF (0.8, 0.9, 0.8));
      myFVals.add (randomF (1.0, 0.5, 0.3));
      myFVals.add (myF0);

      myIncompFVals = new ArrayList<Matrix3d>();
      for (Matrix3d F : myFVals) {
         // compute new F such that the volume difference is not that high
         Matrix3d newF = new Matrix3d(F);
         double J = F.determinant();
         newF.scale (1/Math.pow (J, 1.0/3)); 
         newF.scale (RandomGenerator.nextDouble(0.85, 1.15));
         myIncompFVals.add (newF);
      }

      myMuscleFVals = new ArrayList<Matrix3d>();
      myMuscleFVals.add (rotatedF (1.0, 1.0, 1.1, R));
      myMuscleFVals.add (rotatedF (1.0, 1.0, 1.2, R));
      myMuscleFVals.add (rotatedF (1.0, 1.0, 1.5, R));
      myMuscleFVals.add (rotatedF (1.0, 1.0, 2.0, R));
      myMuscleFVals.add (rotatedF (1.0, 1.0, 0.9, R));
      myMuscleFVals.add (rotatedF (0.8, 1.4, 1.5, R));
      myMuscleFVals.add (rotatedF (1.2, 0.9, 1.4, R));
   }

   protected Matrix3d randomF (double eig0, double eig1, double eig2) {
      RotationMatrix3d R0 = new RotationMatrix3d();
      R0.setRandom();
      RotationMatrix3d R1 = new RotationMatrix3d();
      R1.setRandom();
      Matrix3d F = new Matrix3d();
      F.setDiagonal (eig0, eig1, eig2);
      F.mul (R1);
      F.mulTransposeLeft (R0, F);
      return F;
   }

   protected Matrix3d rotatedF (
      double eig0, double eig1, double eig2, RotationMatrix3d R) {
      Matrix3d F = new Matrix3d();
      F.setDiagonal (eig0, eig1, eig2);
      F.mulTransposeRight (F, R);
      F.mul (R, F);
      return F;
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

   public ArrayList<Matrix3d> getFTests (MaterialBase mat) {
      if (mat instanceof IncompressibleMaterialBase) {
         return myIncompFVals;
      }
      else if (mat instanceof MuscleMaterial) {
         return myMuscleFVals;
      }
      else {
         return myFVals;
      }
   }

   /**
    * Performs tests on a linear material for a given deformation.
    *
    * @param mat material to test
    * @param tol tolerance for computing tests.
    */
   public void testLinear (LinearMaterialBase mat, double tol) {
      for (Matrix3d F0 : getFTests (mat)) {
         testLinear (mat, F0, tol);
      }
   }   

   /**
    * Tests a material by comparing the computed tangent matrix with the
    * numeric derivative of the same.
    *
    * @param mat material to test
    * @param tol tolerance by which the computed and numeric tangents
    * should match. Typically, this should be around 1e-8.
    */
   public void testTangent (MaterialBase mat, double tol) {
      for (Matrix3d F0 : getFTests (mat)) {
         testTangent (mat, F0, tol);
      }
   }   

   /**
    * Tests a material by comparing the computed stress matrix with the
    * numeric derivative of the same.
    *
    * @param mat material to test
    * @param tol tolerance by which the computed and numeric stresses
    * should match. Typically, this should be around 1e-8.
    */
   public void testStress (MaterialBase mat, double tol) {
      for (Matrix3d F0 : getFTests (mat)) {
         testStress (mat, F0, tol);
      }
   }

   public void testLinear (LinearMaterialBase mat, Matrix3d F0, double tol) {

      // compute the strain associate with F0
      SymmetricMatrix3d eps = new SymmetricMatrix3d();
      RotationMatrix3d R = null;
      if (mat.isCorotated()) {
         PolarDecomposition3d polard = new PolarDecomposition3d(F0);
         R = polard.getR();         
         // remove rotation from F0
         eps.mulTransposeLeftSymmetric(R, F0);
      }
      else {
         eps.setSymmetric (F0);
      }
      eps.m00 -= 1;
      eps.m11 -= 1;
      eps.m22 -= 1;

      SymmetricMatrix3d stress = new SymmetricMatrix3d();
      Matrix6d D = new Matrix6d();

      computeStressAndTangent (stress, D, F0, mat, /*p=*/0);
      double W = computeEnergyDensityAndStress (
         null, F0, mat, /*p=*/0, /*J=*/0);

      if (R != null) {
         // either rotate eps into world frame, or rotate stress back into
         // corotated frame
         eps.mulLeftAndTransposeRight(R);
         //stress.mulTransposeLeftAndRight (R);
      }
      // comfirm strain energy density:
      double Wchk = TensorUtils.dot(stress, eps)/2;
      double err = Math.abs(W-Wchk)/Math.abs(W);
      if (err > tol) {
         throw new TestException (
            "Linear strain energy density: computed "+W+", expected "+Wchk);
      }
   }

   public void testTangent (MaterialBase mat, Matrix3d F0, double tol) {

      double h = 1e-8; // increment for computing numeric derivative

      SymmetricMatrix3d stress0 = new SymmetricMatrix3d();
      Matrix6d D = new Matrix6d();
      Matrix6d Dnumeric = new Matrix6d();
      Matrix3d F = new Matrix3d();

      double p = 0;
      if (mat instanceof IncompressibleMaterialBase) {
         IncompressibleMaterialBase imat = (IncompressibleMaterialBase)mat;
         double J = F0.determinant();
         p = imat.getEffectivePressure(imat.getBulkModulus(), J);
      }


      // Start by computing the initial stress (stored as stress0) and tangent
      // for the base deformation gradient F0.
      computeStressAndTangent (stress0, D, F0, mat, p);

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
         dF.mul (d, F0); // compute resulting change in F 
         F.add (F0, dF);

         // compute resulting stress from upated F
         computeStressAndTangent (stress, null, F, mat, p);

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

   public void testStress (MaterialBase mat, Matrix3d F0, double tol) {

      double h = 1e-8; // increment for computing numeric derivative

      double W0;
      SymmetricMatrix3d stress = new SymmetricMatrix3d();
      Matrix3d stressNumeric = new Matrix3d();
      Matrix3d P = new Matrix3d();
      Matrix3d F = new Matrix3d();

      double J = 0;
      double p = 0;
      if (mat instanceof IncompressibleMaterialBase) {
         IncompressibleMaterialBase imat = (IncompressibleMaterialBase)mat;
         J = F0.determinant();
         p = imat.getEffectivePressure(imat.getBulkModulus(), J);
      }

      // Start by computing the initial stress (stored as stress0) and tangent
      // for the base deformation gradient F0.
      W0 = computeEnergyDensityAndStress (stress, F0, mat, J, p);

      for (int i=0; i<3; i++) {
         for (int j=0; j<3; j++) {
            F.set (F0);
            F.set (i, j, h+F.get(i,j));
            double W = computeEnergyDensityAndStress (null, F, mat, J, p);
            P.set (i, j, W-W0);
         }
      }
      P.scale (1/h);
      stressNumeric.mulTransposeRight (P, F0);
      stressNumeric.scale (1/F0.determinant());

      // Find the differene between the computed and numeric tangent.
      // If the relative error exceeds the specified t
      SymmetricMatrix3d stressDiff = new SymmetricMatrix3d(stressNumeric);
      stressDiff.sub (stress);
      double err = 0;
      if (stressNumeric.frobeniusNorm() != 0) {
         err = stressDiff.frobeniusNorm()/stressNumeric.frobeniusNorm();
      }
      else {
         err = stressDiff.frobeniusNorm();
      }

      if (err > tol || err != err) {
         System.out.println (
            "error between computed and numeric stress is "+ err);
         System.out.println ("W=" + W0);
         System.out.println (
            "stress computed=\n" + stress.toString("%14.3f"));
         System.out.println (
            "stress numeric=\n" + stressNumeric.toString("%14.3f"));
         System.out.println (
            "numeric-computed=\n" + stressDiff.toString("%14.3f"));
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

   public void testIncompressible (IncompressibleMaterialBase mat, double tol) {
      testTangent (mat, tol);
      testStress (mat, tol);
      mat.setBulkPotential (BulkPotential.LOGARITHMIC);
      testTangent (mat, tol);
      testStress (mat, tol);
   }         

   /**
    * Test method executed by runtest().
    */
   public void test () {

      // numeric and analytical tangents should match within this tolerance
      double tol = 1e-6;


      NeoHookeanMaterial neohook =
         new NeoHookeanMaterial(10000.0, 0.49);
      MooneyRivlinMaterial mooney =
         new MooneyRivlinMaterial (1.2, 3.4, 0, 0, 0, 1000.0);
      StVenantKirchoffMaterial stvk =
         new StVenantKirchoffMaterial (1234, 0.3);
      IncompNeoHookeanMaterial incompNeohook =
         new IncompNeoHookeanMaterial (123.0, 666.0);
      OgdenMaterial ogden = new OgdenMaterial (
         new double[] {3e5, 1e4, 2e3, 3e3, 4e3, 1.5e2},
         new double[] {1, 2, 3, 4, 5, 6}, 1e5);
      FungOrthotropicMaterial fung =
         new FungOrthotropicMaterial(
            1000, 2000, 3000, 200, 300, 400, 500, 600, 700, 1000, 10000);
      CubicHyperelastic cubicHyper =
         new CubicHyperelastic(1000.0, 2000.0, 3000.0, 1000.0);
      YeohMaterial yeoh =
         new YeohMaterial (1000.0, 500.0, 250.0, 125.0, 62.5, 1000.0);
      IncompressibleMaterial rawIncomp = new IncompressibleMaterial (1234.0);
      VerondaWestmannMaterial vwm =
         new VerondaWestmannMaterial (1000, 10, 100000);      
      ArrudaBoyceMaterial abm =
         new ArrudaBoyceMaterial (1000, 2, 100000);      

      SimpleMuscle simpMuscle = new SimpleMuscle (123.0);
      Vector3d restDir = new Vector3d();
      restDir.setRandom();
      restDir.normalize();
      simpMuscle.setRestDir (restDir);
      simpMuscle.setExcitation (myExcitation);

      LinearMaterial linMat = new LinearMaterial (10000.0, 0.49);
      linMat.setCorotated (false);
      LinearMaterial linMatCorotated = new LinearMaterial (40000.0, 0.33);

      TransverseLinearMaterial linTransverse =
         new TransverseLinearMaterial (
            /*E=*/ new Vector2d (10000.0, 20000.0), /*G=*/3500,
            /*nu=*/ new Vector2d (0.33, 0.45), /*corotated=*/false);
      Vector3d dir = new Vector3d();
      dir.setRandom();
      linTransverse.setDirection (dir);

      GenericMuscle genericMuscle =
         new GenericMuscle (1.4, 3e5, 0.05, 6.6);
      FullBlemkerMuscle fullBlemkerMuscle =
         new FullBlemkerMuscle (1.4, 1.0, 3e5, 0.05, 6.6, 1e6, 1e5);
      BlemkerMuscle blemkerMuscle =
         new BlemkerMuscle (1.4, 1.0, 3e5, 0.05, 6.6);
      SimpleForceMuscle simpleMuscle =
         new SimpleForceMuscle (123.0);

      testLinear (linMat, tol);
      testLinear (linMatCorotated, tol);
      testLinear (linTransverse, tol);
      linTransverse.setCorotated (true);
      testLinear (linTransverse, tol);

      testIncompressible (cubicHyper, tol);
      testIncompressible (yeoh, tol);
      testIncompressible (fung, 2e-6);
      testIncompressible (incompNeohook, tol);
      testIncompressible (rawIncomp, tol);
      testIncompressible (mooney, tol);
      testIncompressible (ogden, 2e-6);
      testIncompressible (vwm, 2e-6);
      testIncompressible (abm, tol);

      testTangent (neohook, tol);
      testStress (neohook, tol);
      testTangent (stvk, tol);
      testStress (stvk, tol);

      testTangent (blemkerMuscle, tol);
      testStress (blemkerMuscle, tol);
      testTangent (fullBlemkerMuscle, tol);
      testStress (fullBlemkerMuscle, 1e-5);
      testTangent (simpleMuscle, tol);
      testStress (simpleMuscle, tol);
      testTangent (simpMuscle, tol);
      testStress (simpMuscle, tol);
      testTangent (genericMuscle, tol);
      testStress (genericMuscle, 2e-4);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);

      FemMaterialTest tester = new FemMaterialTest();

      tester.runtest();
   }

}
