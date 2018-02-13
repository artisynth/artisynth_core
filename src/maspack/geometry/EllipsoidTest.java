package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;

/**
 * This class is intended to test the computation of prinicpal curvatures and
 * radii for ellipsoids, based on the first and second fundamental forms.
 *
 * <p> For computing the fundamental forms of ellipsoid, see
 *  http://mathworld.wolfram.com/Ellipsoid.html.
 *
 * For details on how to use these to compute the principal directions and
 * radii, see Basics of the Differential Geometry of Surfaces, by Gallier,
 * http://www.cis.upenn.edu/~cis610/gma-v2-chap20.pdf, and Curvature of
 * ellipsoids and other surfaces, by W.F. Harris (2006).
 */
public class EllipsoidTest {

   double myA;
   double myB;
   double myC;

   public EllipsoidTest (double a, double b, double c) {
      myA = a;
      myB = b;
      myC = c;
   }

   private final double sqr (double x) {
      return x*x;
   }         

   void calcNormal (Vector3d nrm, double u, double v) {
      double a = myA;
      double b = myB;
      double c = myC;

      double cu = Math.cos(u);
      double su = Math.sin(u);
      double cv = Math.cos(v);
      double sv = Math.sin(v);

      double x = a*cu*sv;
      double y = b*su*sv;
      double z = c*cv;

      nrm.set (x/sqr(a), y/sqr(b), z/sqr(c));
      nrm.normalize();
   }

   void test (double u, double v) {

      System.out.printf (
         "u=%g v=%g\n", Math.toDegrees(u), Math.toDegrees(v));

      double a = myA;
      double b = myB;
      double c = myC;

      double cu = Math.cos(u);
      double su = Math.sin(u);
      double cv = Math.cos(v);
      double sv = Math.sin(v);

      double E = (sqr(b*cu) + sqr(a*su))*sv*sv;
      double F = (b*b-a*a)*cu*su*cv*sv;
      double G = (sqr(a*cu)+sqr(b*su))*cv*cv + sqr(c*sv);

      double denom = Math.sqrt(sqr(a*b*cv) + sqr(c*sv)*(sqr(b*cu) + sqr(a*su)));
      double N = a*b*c/denom;
      double L = sv*sv*N;

      double x = a*cu*sv;
      double y = b*su*sv;
      double z = c*cv;

      Vector3d nrm = new Vector3d();
      calcNormal (nrm, u, v);

      Vector3d ru = new Vector3d(-a*su*sv, b*cu*sv, 0);
      Vector3d rv = new Vector3d(a*cu*cv, b*su*cv, -c*sv);

      Vector3d nrmr = new Vector3d();
      nrmr.cross (rv, ru);
      nrmr.normalize();
      if (!nrm.epsilonEquals (nrmr, 1e-14)) {
         System.out.println ("nrm= " + nrm);
         System.out.println ("nrmr= "+ nrmr);
      }

      Vector3d dnrmuChk = new Vector3d();
      Vector3d dnrmvChk = new Vector3d();

      double h = 1e-8;
      calcNormal (dnrmuChk, u+h, v);
      dnrmuChk.sub(nrm);
      dnrmuChk.scale(1/h);
      calcNormal (dnrmvChk, u, v+h);
      dnrmvChk.sub(nrm);
      dnrmvChk.scale(1/h);

      double D = E*G-F*F;

      Vector3d dnrmu = new Vector3d();
      Vector3d dnrmv = new Vector3d();

      dnrmu.scale (G*L/D, ru);
      dnrmu.scaledAdd (-F*L/D, rv);
      dnrmv.scale (-F*N/D, ru);
      dnrmv.scaledAdd (E*N/D, rv);

      if (!dnrmu.epsilonEquals (dnrmuChk, 1e-6*dnrmu.norm())) {
         System.out.println ("dnrmu=   " + dnrmu.toString());
         System.out.println ("dnrmuChk=" + dnrmuChk.toString());
      }
      if (!dnrmv.epsilonEquals (dnrmvChk, 1e-6*dnrmv.norm())) {
         System.out.println ("dnrmv=   " + dnrmv.toString());
         System.out.println ("dnrmvChk=" + dnrmvChk.toString());
      }

      double[] roots = new double[2];
      int nr = QuadraticSolver.getRoots (roots, F*N, E*N-G*L, -F*L);
      if (nr != 2) {
         System.out.println ("only "+nr+" roots");
      }
      Vector3d u0 = new Vector3d();
      Vector3d u1 = new Vector3d();

      double a0 = Math.atan(roots[0]);
      double s0 = Math.sin(a0);
      double c0 = Math.cos(a0);
      double r0 = (E*c0*c0+2*F*s0*c0+G*s0*s0)/(L*c0*c0+N*s0*s0);

      u0.scale (c0, dnrmu);
      u0.scaledAdd (s0, dnrmv);

      double a1 = Math.atan(roots[1]);
      double s1 = Math.sin(a1);
      double c1 = Math.cos(a1);
      double r1 = (E*c1*c1+2*F*s1*c1+G*s1*s1)/(L*c1*c1+N*s1*s1);

      u1.scale (c1, dnrmu);
      u1.scaledAdd (s1, dnrmv);

      // System.out.printf (" r0=  %g\n", r0);
      // u0.normalize();
      // System.out.println (" u0=" + u0.toString ("%8.3f"));
      // System.out.printf (" r1=  %g\n", r1);
      // u1.normalize();
      // System.out.println (" u1=" + u1.toString ("%8.3f"));

      Vector3d e0 = new Vector3d();
      e0.scale (1/Math.sqrt(E), ru);
      Vector3d e1 = new Vector3d();
      e1.scale (E, rv);
      e1.scaledAdd (-F, ru);
      e1.scale (1/Math.sqrt(E*(E*G-F*F)));

      double H = (G*L+E*N)/(2*(E*G-F*F));
      double A = (L*(E*G-2*F*F) - E*E*N)/(2*E*(E*G-F*F));
      double B = -F*L/(E*Math.sqrt(E*G-F*F));

      double C = Math.sqrt(A*A+B*B);
      nr = QuadraticSolver.getRoots (roots, 4*C*C, -4*C*A, -B*B);
      if (nr != 2) {
         System.out.println ("only "+nr+" roots");
      }      
      double cos = Math.sqrt(roots[1]);
      double sin = B/(C*2*cos);

      u0.scale (cos, e0);
      u0.scaledAdd (sin, e1);
      System.out.printf (" r0=  %g\n", 1/(H+C));
      System.out.println (" u0=" + u0.toString ("%8.3f"));
      u1.cross (nrm, u0);
      System.out.printf (" r1=  %g\n", 1/(H-C));
      System.out.println (" u1=" + u1.toString ("%8.3f"));

      if (z <= c*Math.sqrt(2)/2) {
         System.out.println ("REG");
         sv = Math.sqrt(c*c-z*z)/c;
         ru.set (-a*y/b, b*x/a, 0);
         rv.set (x*z/(sv*c), y*z/(sv*c), -c*sv);
         E = ru.x*ru.x + ru.y*ru.y;
         F = ru.x*rv.x + ru.y*rv.y;
      }
      else {
         System.out.println ("FLIP");
         sv = Math.sqrt(b*b-y*y)/b;
         ru.set (a*z/c, 0, -c*x/a);
         rv.set (x*y/(sv*b), -b*sv, z*y/(sv*b));
         E = ru.x*ru.x + ru.z*ru.z;
         F = ru.x*rv.x + ru.z*rv.z;
      }

      G = rv.x*rv.x + rv.y*rv.y + rv.z*rv.z;

      denom = Math.sqrt (sqr(a*b*z/c) + sqr(c*b*x/a) + sqr(c*a*y/b));
      N = a*b*c/denom;
      L = N*sv*sv;

      //System.out.printf ("E=%g F=%g G=%g N=%g L=%g\n", E, F, G, L, N);

      e0.scale (1/Math.sqrt(E), ru);
      e1.scale (E, rv);
      e1.scaledAdd (-F, ru);
      e1.scale (1/Math.sqrt(E*(E*G-F*F)));
      Vector3d xprod = new Vector3d();
      xprod.cross (e0, e1);
      System.out.println ("DOT " + (xprod.dot(nrm) < 0 ? " NEG " : " POS"));

      H = (G*L+E*N)/(2*(E*G-F*F));
      A = (L*(E*G-2*F*F) - E*E*N)/(2*E*(E*G-F*F));
      B = -F*L/(E*Math.sqrt(E*G-F*F));

      C = Math.sqrt(A*A+B*B);
      nr = QuadraticSolver.getRoots (roots, 4*C*C, -4*C*A, -B*B);
      if (nr != 2) {
         System.out.println ("only "+nr+" roots");
      }      
      cos = Math.sqrt(roots[1]);
      sin = B/(C*2*cos);

      u0.scale (cos, e0);
      u0.scaledAdd (sin, e1);
      System.out.printf (" r0=  %g\n", 1/(H+C));
      System.out.println (" u0=" + u0.toString ("%8.3f"));
      u1.cross (nrm, u0);
      System.out.printf (" r1=  %g\n", 1/(H-C));
      System.out.println (" u1=" + u1.toString ("%8.3f"));
   }

   void test () {
      int cnt = 20;
      for (int i=0; i<cnt; i++) {
         double u = Math.toRadians(RandomGenerator.nextDouble (-180, 180));
         double v = Math.toRadians(RandomGenerator.nextDouble (0, 180));
         test (u, v);
      }
      test (Math.toRadians(1.0), Math.toRadians(89.0));
      test (Math.toRadians(89.0), Math.toRadians(89.0));
   }


   public static void main (String[] args) {
      EllipsoidTest tester = new EllipsoidTest (3.0, 2.0, 1.0);
      RandomGenerator.setSeed (0x1234);
      tester.test();
   }

}
