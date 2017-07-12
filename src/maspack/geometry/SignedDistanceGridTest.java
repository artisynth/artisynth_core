package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;

public class SignedDistanceGridTest extends UnitTest {

   private Matrix3d computeNumericNormalDerivative (
      SignedDistanceGrid grid, Point3d q) {

      Matrix3d Dchk = new Matrix3d();
      Point3d qh = new Point3d(q);

      Vector3d nrm = new Vector3d();
      
      double d0 = grid.getLocalDistanceAndNormal (nrm, null, q);
      Vector3d f0 = new Vector3d();
      f0.scale (d0, nrm);
      double h = 1e-8;

      for (int i=0; i<3; i++) {
         qh.set (q);
         qh.set (i, q.get(i)+h);
         double d = grid.getLocalDistanceAndNormal (nrm, null, qh);
         Vector3d df = new Vector3d();
         df.scale (d, nrm);
         df.sub (f0);
         df.scale (1/h);
         Dchk.setColumn (i, df);
      }
      return Dchk;
   }

   private Vector3d computeNumericGradient (
      SignedDistanceGrid grid, Point3d q) {

      Vector3d gchk = new Vector3d();
      Point3d qh = new Point3d(q);
      
      double d0 = grid.getLocalDistanceAndNormal (null, null, q);
      double h = 1e-8;

      for (int i=0; i<3; i++) {
         qh.set (q);
         qh.set (i, q.get(i)+h);
         double d = grid.getLocalDistanceAndNormal (null, null, qh);
         gchk.set (i, (d-d0)/h);
      }
      return gchk;
   }


   public void test() {
      PolygonalMesh torus = MeshFactory.createTorus (1.0, 0.5, 24, 24);

      SignedDistanceGrid grid = new SignedDistanceGrid (torus, 0.1);

      Vector3d minc = grid.getMinCoords();
      Vector3d maxc = grid.getMaxCoords();
      Vector3d widths = new Vector3d();
      Vector3d center = new Vector3d();
      widths.sub (maxc, minc);
      center.add (maxc, minc);
      center.scale (0.5);

      int numtests = 1000;
      for (int i=0; i<numtests; i++) {
         Point3d q = new Point3d (
            RandomGenerator.nextDouble (0, widths.x),
            RandomGenerator.nextDouble (0, widths.y),
            RandomGenerator.nextDouble (0, widths.z));
         q.add (center);
         q.scaledAdd (-0.5, widths);
         
         Vector3d nrm = new Vector3d();
         Matrix3d Dnrm = new Matrix3d();

         double d = grid.getLocalDistanceAndNormal (nrm, Dnrm, q);
         if (d != SignedDistanceGrid.OUTSIDE) {
            // check Dnrm
            Matrix3d Dchk = computeNumericNormalDerivative (grid, q);
            Matrix3d Err = new Matrix3d();
            Err.sub (Dchk, Dnrm);
            double err = Err.frobeniusNorm()/Dnrm.frobeniusNorm();
            if (err > 1e-7) {
               System.out.println ("Dchk=\n" + Dchk.toString("%12.8f"));
               System.out.println ("Dnrm=\n" + Dnrm.toString("%12.8f"));
               System.out.println ("Err=\n" + Err.toString("%12.8f"));
               throw new TestException (
                  "Dnrm gradient error computation: err=" + err);
            }
         }

         Vector3d grad = new Vector3d();

         d = grid.getLocalDistanceAndGradient (grad, q);
         if (d != SignedDistanceGrid.OUTSIDE) {
            // check Dnrm
            Vector3d gchk = computeNumericGradient (grid, q);
            Vector3d gerr = new Vector3d();
            gerr.sub (gchk, grad);
            double err = gerr.norm()/gchk.norm();
            if (err > 1e-7) {
               System.out.println ("gchk=\n" + gchk.toString("%12.8f"));
               System.out.println ("grad=\n" + grad.toString("%12.8f"));
               throw new TestException (
                  "Distance gradient error computation: err=" + err);
            }
         }
      }
   }

   public static void main (String[] args) {
      SignedDistanceGridTest tester = new SignedDistanceGridTest();

      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }
}
