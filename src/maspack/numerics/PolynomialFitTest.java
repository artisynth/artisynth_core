package maspack.numerics;

import maspack.util.*;
import maspack.matrix.*;

public class PolynomialFitTest extends UnitTest {


   public void testForDegree (int degree, int numy) {
      double xlo = RandomGenerator.nextDouble (-1.0, 1.0);
      double xhi = RandomGenerator.nextDouble (2.0, 4.0);

      VectorNd coefs = new VectorNd();
      VectorNd check = new VectorNd(degree+1);
      for (int i=0; i<degree+1; i++) {
         check.set (i, RandomGenerator.nextDouble (-2.0, 2.0));
      }

      VectorNd yv = new VectorNd (numy);
      PolynomialFit fitter = new PolynomialFit();
      for (int i=0; i<numy; i++) {
         double x = xlo + i*(xhi-xlo)/(numy-1.0);
         yv.set (i, fitter.evalPoly (check, x));
      }

      fitter.fit (coefs, yv, xlo, xhi, degree);
      double maxdev = fitter.maximumDeviation (coefs, yv, xlo, xhi);
      for (int j=0; j<degree+1; j++) {
         double err = Math.abs(coefs.get(j)-check.get(j));
         if (err > 1e-12) {
            throw new TestException (
               "Badly computed coefs. Expected\n" +
               check.toString ("%12.8f") + "\nGot\n" +
               coefs.toString ("%12.8f"));
         }
      }
      if (maxdev > 1e-12) {
         throw new TestException (
            "Maximum deviation is "+maxdev+", coefs=\n" +
            coefs.toString ("%12.8f"));
      }      
   }

   public void  testSavitzkyGolay() {
      PolynomialFit fit;
      VectorNd wgts;
      VectorNd wchk;

      double EPS = 1e-12;

      wchk = new VectorNd(new double[] {-3, 12, 17, 12, -3});
      wchk.scale (1/35.0);
      
      fit = new PolynomialFit (3, 5, 0.0, 1.0);
      wgts = fit.getSavitzkyGolayWeights (0.5);
      checkEquals ("SavitzkyGolay win=5, deg=3", wgts, wchk, EPS);
      fit = new PolynomialFit (2, 5, 0.0, 1.0);
      wgts = fit.getSavitzkyGolayWeights (0.5);
      checkEquals ("SavitzkyGolay win=5, deg=2", wgts, wchk, EPS);

      wchk = new VectorNd(new double[] {5, -30, 75, 131, 75, -30, 5});
      wchk.scale (1/231.0);
      
      fit = new PolynomialFit (4, 7, 0.0, 1.0);
      wgts = fit.getSavitzkyGolayWeights (0.5);
      checkEquals ("SavitzkyGolay win=7, deg=4", wgts, wchk, EPS);
      fit = new PolynomialFit (5, 7, 0.0, 1.0);
      wgts = fit.getSavitzkyGolayWeights (0.5);
      checkEquals ("SavitzkyGolay win=7, deg=5", wgts, wchk, EPS);

   }

   public void test() {

      for (int deg=1; deg<5; deg++) {
         testForDegree (deg, 6);
         testForDegree (deg, 10);
         testForDegree (deg, 20);
         testForDegree (deg, 50);
      }
      testSavitzkyGolay();
   }

   public static void main (String[] args) {
      PolynomialFitTest tester = new PolynomialFitTest();

      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }
   
   

}
