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

   public void checkDfdldot (
      AxialMaterial mat, double l, double ldot,
      double l0, double ex, double maxval, double tol) {

      double h = 1e-8;

      double F = mat.computeF (l, ldot, l0, ex);
      double dFdldot = mat.computeDFdldot (l, ldot, l0, ex);
      double Fh = mat.computeF (l, ldot+h*ldot, l0, ex);
      double dFdldotChk = (Fh - F)/(h*ldot);

      double err = Math.abs(dFdldot-dFdldotChk);
      if (maxval > 0) {
         err /= maxval;
      }
      if (err > tol || err != err) {
         System.out.println (
            "error between computed and numeric dFdldot is "+ err);
         System.out.println (
            "l=" + l + " ldot=" + ldot + " ldoth=" + (ldot+h*ldot));
         System.out.println ("F=" + F + " Fh=" + Fh);
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

   public void checkDfdl (
      AxialMaterial mat, double l, double ldot,
      double l0, double ex, double maxval, double tol) {

      double h = 1e-8;

      double F = mat.computeF (l, ldot, l0, ex);
      double dFdl = mat.computeDFdl (l, ldot, l0, ex);
      double dFdlChk = (mat.computeF (l+h*l, ldot, l0, ex) - F)/(h*l);

      double err = Math.abs(dFdl-dFdlChk);
      if (maxval > 0) {
         err /= maxval;
      }
      if (err > tol || err != err) {
         System.out.println (
            "error between computed and numeric dFdl is "+ err);
         System.out.println ("F=" + F);
         System.out.println ("Fh=" +mat.computeF (l+h*l, ldot, l0, ex));
         System.out.println ("dFdl computed=" + dFdl);
         System.out.println ("dFdl numeric=" + dFdlChk);
         System.out.println ("l=" + l + " v=" + ldot);
         System.out.println ("l+h*l=" + (l+h*l));
         if (err != err) {
            throw new TestException (
               "Error between numeric and computed dFdl is NaN");
         }
         else {
            throw new TestException (
               "Error between numeric and computed dFdl > " + tol);
         }
      }
   }

   public void testDfdlOverRange (
      AxialMaterial mat,
      
      double lmin, double lmax, double vmin, double vmax,
      double l0, double ex, double tol) {
      
      int nl = 1; // number of l values to test
      int nv = 1; // number of v values to test
      double linc = 0; // l value increment
      double vinc = 0; // v value increment
      if (lmin != lmax) {
         nl = 40;
         linc = (lmax-lmin)/(nl-1);
      }
      if (vmin != vmax) {
         nv = 20;
         vinc = (vmax-vmin)/(nv-1);
      }
      // first find the magnitude over the range
      double maxDfdl = 0;
      for (int i=0; i<nl;  i++) {
         double l = lmin + i*linc;
         for (int j=0; j<nv; j++) {
            double v = vmin + j*vinc;
            double dfdl = mat.computeDFdl (l, v, l0, ex);
            if (Math.abs(dfdl) > maxDfdl) {
               maxDfdl = Math.abs(dfdl);
            }
         }
      }
      // then do the numeric check
      for (int i=0; i<nl; i++) {
         double l = lmin + i*linc;
         for (int j=0; j<nv; j++) {
            double v = vmin + j*vinc;
            checkDfdl (mat, l, v, l0, ex, maxDfdl, tol);
         }
      }
   }

   public void testDfdldotOverRange (
      AxialMaterial mat, 
      double lmin, double lmax, double vmin, double vmax,
      double l0, double ex, double tol) {

      int nl = 1; // number of l values to test
      int nv = 1; // number of v values to test
      double linc = 0; // l value increment
      double vinc = 0; // v value increment
      if (lmin != lmax) {
         nl = 40;
         linc = (lmax-lmin)/(nl-1);
      }
      if (vmin != vmax) {
         nv = 20;
         
         vinc = (vmax-vmin)/(nv-1);
      }
      // first find the magnitude over the range
      double maxDfdldot = 0;
      for (int i=0; i<nl;  i++) {
         double l = lmin + i*linc;
         for (int j=0; j<nv; j++) {
            double v = vmin + j*vinc;
            double dfdldot = mat.computeDFdldot (l, v, l0, ex);
            if (Math.abs(dfdldot) > maxDfdldot) {
               maxDfdldot = Math.abs(dfdldot);
            }
         }
      }
      // then do the numeric check
      for (int i=0; i<nl;  i++) {
         double l = lmin + i*linc;
         for (int j=0; j<nv; j++) {
            double v = vmin + j*vinc;
            checkDfdldot (mat, l, v, l0, ex, maxDfdldot, tol);
         }
      }
   }

   public void testMaterial (
      AxialMaterial mat, double l0, double ex, double tol) {
      testMaterial (
         mat, mat, 2*l0, 0.2*l0, l0, ex, tol);
   }

   /**
    * Tests a material by comparing the computed derivatives with the numeric
    * derivative of the same. Different materials are specified for testing
    * dFdl and dFdldot in case different parameter settings are needed to
    * numerically compute the different derivates.
    *
    * @param mat material to test for dFdl
    * @param matdot material to test for dFdldot
    * @param tol tolerance by which the computed and numeric derivatives should
    * match. Typically, this should be around 1e-8.
    */
   public void testMaterial (
      AxialMaterial mat, AxialMaterial matdot,
      double l, double ldot, double l0, double ex, double tol) {
      
      testDfdlOverRange (mat, l, l, ldot, ldot, l0, ex, tol);
      testDfdldotOverRange (matdot, l, l, ldot, ldot, l0, ex, tol);
   }

   public void testLinearAxialMaterial (double tol) {
      LinearAxialMaterial mat = new LinearAxialMaterial (1000.0, 200.0);

      double l0 = 0.5;
      double maxL = 2.0;
      double minL = 0.1;
      double maxV = 2.0;
      double minV = -2.0;

      testDfdlOverRange (mat, maxL, minL, maxV, minV, l0, 0, tol);

      mat.setStiffness (0);

      testDfdldotOverRange (mat, maxL, minL, maxV, minV, l0, 0, tol);
   }

   public void testSimpleAxialMuscle (double tol) {
      SimpleAxialMuscle mat =
         new SimpleAxialMuscle (1000.0, 200.0, 100);

      double ex = 0.5;
      double l0 = 0.5;
      double maxL = 2.0;
      double minL = 0.1;
      double maxV = 2.0;
      double minV = -2.0;

      testDfdlOverRange (mat, maxL, minL, maxV, minV, l0, ex, tol);

      mat.setUnilateral (true);
      mat.setBlendInterval (0.2);

      testDfdlOverRange (mat, maxL, minL, maxV, minV, l0, ex, tol);

      mat.setStiffness (0);
      testDfdldotOverRange (mat, maxL, minL, maxV, minV, l0, ex, tol);
   }

   public void testMillard2012AxialTendon (double tol) {

      Millard2012AxialTendon mmat =
         new Millard2012AxialTendon (2.0, 3.0);

      double maxL = 3.3;
      double minL = 2.5;

      testDfdlOverRange (mmat, minL, maxL, 0, 0, 0, 0, tol);
   }

   public void testThelen2003AxialMuscle (double tol) {
      double ex = 0.5;
      Thelen2003AxialMuscle mmat = new Thelen2003AxialMuscle (
         2.0, 2.0, 0.6, Math.toRadians(30));
      mmat.setRigidTendon (true);

      double maxL = 2.0;
      double minL = -0.2; 
      double maxV = 2.5;
      double minV = -2.5;

      mmat.setIgnoreForceVelocity (true);
      testDfdlOverRange (mmat, minL, maxL, minV, maxV, 0, ex, tol);
      testDfdldotOverRange (mmat, minL, maxL, minV, maxV, 0, ex, tol);
      mmat.setIgnoreForceVelocity (false);
      testDfdlOverRange (mmat, minL, maxL, minV, maxV, 0, ex, tol);
      testDfdldotOverRange (mmat, minL, maxL, minV, maxV, 0, ex, tol);
   }

   public void testMillard2012AxialMuscle (double tol) {
      double ex = 0.5;
      Millard2012AxialMuscle mmat = new Millard2012AxialMuscle (
         2.0, 2.0, 0.6, Math.toRadians(30));
      mmat.setRigidTendon (true);

      double maxL = 2.0;
      double minL = -0.2; 
      double maxV = 2.5;
      double minV = -2.5;

      mmat.setFibreDamping (1.0);
      mmat.setIgnoreForceVelocity (true);
      testDfdlOverRange (mmat, minL, maxL, minV, maxV, 0, ex, tol);
      testDfdldotOverRange (mmat, minL, maxL, minV, maxV, 0, ex, tol);
      mmat.setIgnoreForceVelocity (false);
      testDfdlOverRange (mmat, minL, maxL, minV, maxV, 0, ex, tol);
      testDfdldotOverRange (mmat, minL, maxL, minV, maxV, 0, ex, tol);
   }

   public void testBlankevoort1991Ligament (double tol) {
      double ex = 0.5;

      Blankevoort1991AxialLigament lmat = new Blankevoort1991AxialLigament();
      lmat.setLinearStiffness (500.0);
      double sl = 0.08226920;
      lmat.setSlackLength (sl);
      Blankevoort1991AxialLigament lmatdot = lmat.clone();
      lmatdot.setLinearStiffness (0);
      double lt = sl*(lmat.getTransitionStrain()+1);

      double maxL = lt+(lt-sl)/4;
      double minL = sl-(lt-sl)/4;
      double maxV = 0.1*(minL+maxL)/2;
      double minV = maxV;

      testDfdlOverRange (lmat, minL, maxL, minV, maxV, 0, ex, 10*tol);
      testDfdldotOverRange (lmatdot, minL, maxL, minV, maxV, 0, ex, 2*tol);
   }

   public void testAxialLigament (double tol) {

      AxialLigament lmat = new AxialLigament (3.0, 2.0, 0.1);

      double minL = 0.1;
      double maxL = 4.0;

      testDfdlOverRange (lmat, minL, maxL, 0, 0, 0, 0, tol);

      double maxV = 0.1*(minL+maxL)/2;
      double minV = maxV;

      testDfdldotOverRange (lmat, minL, maxL, minV, maxV, 0, 0, 2*tol);
   }

   public void testHill3ElemMuscleRigidTendon (double tol) {
      double ex = 0.5;

      double T = 0.1; // tendon slack length
      double L = 0.5;  // optimalFibreLen
      double penAng = 20.0; // pennation angle (degrees)
      double h = L*Math.sin(Math.toRadians(penAng));

      Hill3ElemMuscleRigidTendon mat = new Hill3ElemMuscleRigidTendon (
         1000.0, L, T, penAng);
      mat.setDamping (3.0);
      mat.setForceScaling (5.0);
      double maxL = T+Math.sqrt(4*L*L-h*h);
      double minL = T+Math.sqrt(4*L*L-h*h)/2.0;
      double maxV = 0.1*(minL+maxL)/2;
      double minV = maxV;
      Hill3ElemMuscleRigidTendon matdot = mat.clone();
      matdot.setMaxForce (0);

      //testOverRange (mat, minL, maxL, 1.0, 1e-5);
      //testMaterial (mat, 1.0, 1e-5);
      testDfdlOverRange (mat, minL, maxL, minV, maxV, 1.0, ex, tol);
      testDfdldotOverRange (matdot, minL, maxL, minV, maxV, 1.0, ex, tol);
   }

   /**
    * Test method executed by runtest().
    */
   public void test () {
         
      // numeric and analytical tangents should match within this tolerance
      double tol = 1e-6;
      double ex = 0.5;

      testMaterial (
         new LigamentAxialMaterial (1234.00, 222.0, 333.0), 2.0, ex, tol);
      testMaterial (new ConstantAxialMuscle (20.0), 2.0, ex, tol);
      testMaterial (new ConstantAxialMaterial (222.0), 2.0, ex, tol);
      testMaterial (new LinearAxialMuscle (20, 2.0), 2.0, ex, tol);
      testMaterial (
         new PeckAxialMuscle (20.0, 2.0, 3.0, 0.2, 0.5, 0.1), 2.0, ex, 1e-5);
      testMaterial (
         new PaiAxialMuscle (20.0, 2.0, 3.0, 0.2, 0.5, 0.1), 2.0, ex, 1e-5);
      testMaterial (
         new BlemkerAxialMuscle (1.4, 1.0, 3e5, 0.05, 6.6), 2.0, ex, tol);
      //testMaterial (new MasoudMillardLAM (2.0, 1.8, 0.5), 2.0, ex, tol);

      testLinearAxialMaterial (tol);
      testSimpleAxialMuscle (tol);

      testHill3ElemMuscleRigidTendon(5*tol);
      testBlankevoort1991Ligament(tol);
      testMillard2012AxialMuscle(2e-6);
      testMillard2012AxialTendon(5e-7);
      testThelen2003AxialMuscle(2e-6);
      testAxialLigament(2e-6);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);

      AxialMaterialTest tester = new AxialMaterialTest();
      tester.runtest();
   }

}
