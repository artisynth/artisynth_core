package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;

public class PlaneDerivTest extends UnitTest {

   private static double INF = Double.POSITIVE_INFINITY;

   public void testPlaneDeriv (Plane plane, RigidTransform3d TBW) {
      Plane planeB = new Plane(plane);
      planeB.inverseTransform (TBW);
      int ntests = 10;
      double h = 1e-8;
      for (int i=0; i<ntests; i++) {
         Twist vel = new Twist();
         vel.setRandom();

         RigidTransform3d TBWx = new RigidTransform3d(TBW);
         vel.extrapolateTransformWorld (TBWx, h);
         Plane planeX = new Plane (planeB);
         planeX.transform (TBWx);
         Vector3d dnrm = new Vector3d();
         double doff = (planeX.offset - plane.offset)/h;
         dnrm.sub (planeX.normal, plane.normal);
         dnrm.scale (1/h);
         
         Vector3d vnrm = new Vector3d();
         vnrm.cross (vel.w, plane.normal);
         double voff = plane.normal.dot (vel.v) + vnrm.dot (TBW.p);

         System.out.printf (
            "dnrm=%s  doff=%12.7f\n", dnrm.toString ("%12.7f"), doff);
         System.out.printf (
            "vnrm=%s  voff=%12.7f\n", vnrm.toString ("%12.7f"), voff);
         System.out.println ("");
      }
   }

   private double sqr (double x) {
      return x*x;
   }

   private double cube (double x) {
      return x*x*x;
   }

   public void testProjectedPlaneDeriv (Plane plane, RigidTransform3d TBW) {
      Plane planeB = new Plane(plane);
      planeB.inverseTransform (TBW);
      int ntests = 10;
      Vector3d nrmJ = new Vector3d();
      Vector3d nrm2 = new Vector3d();
      nrm2.set (plane.normal.x, plane.normal.y, 0);
      double n2mag = nrm2.norm();
      double offJ = plane.offset/n2mag;
      nrmJ.normalize(nrm2);

      double h = 1e-8;
      for (int i=0; i<ntests; i++) {
         Twist vel = new Twist();
         vel.setRandom();

         RigidTransform3d TBWx = new RigidTransform3d(TBW);
         vel.extrapolateTransformWorld (TBWx, h);
         Plane planeX = new Plane (planeB);
         planeX.transform (TBWx);
         
         Vector3d nrmJX = new Vector3d();
         nrmJX.set (planeX.normal.x, planeX.normal.y, 0);
         double offJX = planeX.offset/nrmJX.norm();
         nrmJX.normalize();

         Vector3d dnrmJ = new Vector3d();
         double doffJ = (offJX - offJ)/h;
         dnrmJ.sub (nrmJX, nrmJ);
         dnrmJ.scale (1/h);
         
         Vector3d vnrm = new Vector3d();
         vnrm.cross (vel.w, plane.normal);
         double voff = plane.normal.dot (vel.v) + vnrm.dot (TBW.p);

         Vector3d vnrm2 = new Vector3d(vnrm);
         vnrm2.z = 0;

         Vector3d vnrmJ = new Vector3d();
         vnrmJ.scale (n2mag, vnrm2);
         vnrmJ.scaledAdd (-nrm2.dot(vnrm2), nrmJ);
         vnrmJ.scale (1/sqr(n2mag));
         double voffJ =
            (n2mag*voff - plane.offset*nrmJ.dot(vnrm2))/sqr(n2mag);

         System.out.printf (
            "dnrmJ=%s  doffJ=%12.7f\n", dnrmJ.toString ("%12.7f"), doffJ);
         System.out.printf (
            "vnrmJ=%s  voffJ=%12.7f\n", vnrmJ.toString ("%12.7f"), voffJ);
         System.out.println ("");
      }
   }

   // public void testNearPointDeriv (
   //    Plane plane, RigidTransform3d TBW, Point3d pr) {
   //    Plane planeB = new Plane(plane);
   //    planeB.inverseTransform (TBW);
   //    int ntests = 10;
   //    Vector3d nrmJ = new Vector3d();
   //    Vector3d nrm2 = new Vector3d();
   //    nrm2.set (plane.normal.x, plane.normal.y, 0);
   //    double n2mag = nrm2.norm();
   //    double offJ = plane.offset/n2mag;
   //    nrmJ.normalize(nrm2);

   //    Point3d pn = new Point3d();
   //    double dist = nrmJ.dot (pr) - offJ;
   //    pn.scaledAdd (-dist, nrmJ, pr);

   //    double h = 1e-8;
   //    for (int i=0; i<ntests; i++) {
   //       Twist vel = new Twist();
   //       vel.setRandom();

   //       RigidTransform3d TBWx = new RigidTransform3d(TBW);
   //       vel.extrapolateTransformWorld (TBWx, h);
   //       Plane planeX = new Plane (planeB);
   //       planeX.transform (TBWx);
         
   //       Vector3d nrmJX = new Vector3d();
   //       nrmJX.set (planeX.normal.x, planeX.normal.y, 0);
   //       double offJX = planeX.offset/nrmJX.norm();
   //       nrmJX.normalize();

   //       Point3d pnx = new Point3d();
   //       dist = nrmJX.dot (pr) - offJX;
   //       pnx.scaledAdd (-dist, nrmJX, pr);

   //       Vector3d dpn = new Vector3d();
   //       dpn.sub (pnx, pn);
   //       dpn.scale (1/h);
         
   //       Vector3d vnrm = new Vector3d();
   //       vnrm.cross (vel.w, plane.normal);
   //       double voff = plane.normal.dot (vel.v) + vnrm.dot (TBW.p);

   //       Vector3d vnrm2 = new Vector3d(vnrm);
   //       vnrm2.z = 0;

   //       Vector3d vnrmJ = new Vector3d();
   //       vnrmJ.scale (n2mag, vnrm2);
   //       vnrmJ.scaledAdd (-nrm2.dot(vnrm2), nrmJ);
   //       vnrmJ.scale (1/sqr(n2mag));
   //       double voffJ =
   //          (n2mag*voff - plane.offset*nrmJ.dot(vnrm2))/sqr(n2mag);

   //       Vector3d vpn = new Vector3d();
   //       vpn.scale (-(vnrmJ.dot(pr)-voffJ), nrmJ);
   //       vpn.scaledAdd (-(nrmJ.dot(pr)-offJ), vnrmJ);

   //       Matrix3d B = new Matrix3d();
   //       Matrix3d A = new Matrix3d();
   //       Vector3d mpn = new Vector3d();

   //       A.outerProduct (nrmJ, plane.normal);
   //       A.scale (1/n2mag);

   //       Matrix3d NN = new Matrix3d();
   //       NN.outerProduct (nrmJ, nrmJ);
   //       Matrix3d NP = new Matrix3d();
   //       NP.outerProduct (nrmJ, pr);
   //       double np = nrmJ.dot(pr);

   //       Matrix3d N2x = new Matrix3d();
   //       N2x.setSkewSymmetric (plane.normal);
   //       N2x.m20 = 0;
   //       N2x.m21 = 0;

   //       Matrix3d Tmp = new Matrix3d();

   //       B.set (NP);
   //       B.addDiagonal (np-offJ);

   //       Tmp.setIdentity();
   //       Tmp.sub (NN);
   //       B.mul (Tmp);
   //       B.scaledAdd (offJ, NN);
   //       B.mul (N2x);
         
   //       Tmp.outerProduct (nrmJ, TBW.p);
   //       Tmp.crossProduct (Tmp, plane.normal);
   //       B.sub (Tmp);

   //       B.scale (1/n2mag);         

   //       A.mul (mpn, vel.v);
   //       B.mulAdd (mpn, vel.w, mpn);

   //       if (!dpn.epsilonEquals (vpn, 1e-7) || 
   //           !vpn.epsilonEquals (mpn, 1e-14)) {
   //          System.out.printf (
   //             "dpn=%s\n", dpn.toString ("%12.7f"));
   //          System.out.printf (
   //             "vpn=%s\n", vpn.toString ("%12.7f"));
   //          System.out.printf (
   //             "mpn=%s\n", mpn.toString ("%12.7f"));
   //          System.out.println ("");
   //          throw new TestException ("inconsistent results");
   //       }
   //    }
   // }

   public void testNearPointDeriv (
      Plane plane, RigidTransform3d TBW, RigidTransform3d TJW, Point3d pr) {

      Plane planeB = new Plane(plane);
      planeB.inverseTransform (TBW);

      // transform pr, pb, and plane into junction plane coordinates
      Point3d prJ = new Point3d(pr);
      prJ.inverseTransform (TJW);
      Point3d pbJ = new Point3d (TBW.p);
      pbJ.inverseTransform (TJW);
      Plane planeJ = new Plane(plane);
      planeJ.inverseTransform (TJW);

      int ntests = 10;
      Vector3d nrmJ = new Vector3d();
      Vector3d nrm2 = new Vector3d();
      nrm2.set (planeJ.normal.x, planeJ.normal.y, 0);
      double n2mag = nrm2.norm();
      double offJ = planeJ.offset/n2mag;
      nrmJ.normalize(nrm2);

      Point3d pnJ = new Point3d();
      double dist = nrmJ.dot (prJ) - offJ;
      pnJ.scaledAdd (-dist, nrmJ, prJ);

      Matrix3d Jr = new Matrix3d();
      Matrix3d Jt = new Matrix3d();

      // compute Jacobian matrices

      // System.out.println ("planeJ=" + planeJ.toString ("%14.9f"));
      // System.out.println ("prJ=" + prJ.toString ("%14.9f"));
      // System.out.println ("pbJ=" + pbJ.toString ("%14.9f"));

      Jt.outerProduct (nrmJ, planeJ.normal);
      Jt.scale (1/n2mag);
      Jt.transform (TJW.R);

      Matrix3d NN = new Matrix3d();
      NN.outerProduct (nrmJ, nrmJ);
      Matrix3d NP = new Matrix3d();
      NP.outerProduct (nrmJ, prJ);
      double np = nrmJ.dot(prJ);

      Matrix3d N2x = new Matrix3d();
      N2x.setSkewSymmetric (planeJ.normal);
      N2x.m20 = 0;
      N2x.m21 = 0;

      Matrix3d Tmp = new Matrix3d();

      Jr.set (NP);
      Jr.addDiagonal (np-offJ);

      Tmp.setIdentity();
      Tmp.sub (NN);
      Jr.mul (Tmp);
      Jr.scaledAdd (offJ, NN);
      Jr.mul (N2x);
         
      Tmp.outerProduct (nrmJ, pbJ);
      Tmp.crossProduct (Tmp, planeJ.normal);
      Jr.sub (Tmp);

      Jr.scale (1/n2mag);   
      Jr.transform (TJW.R);

      //System.out.println ("Jt=\n" + Jt.toString ("%12.7f"));
      //System.out.println ("Jr=\n" + Jr.toString ("%12.7f"));

      double h = 1e-8;
      for (int i=0; i<ntests; i++) {
         Twist vel = new Twist();
         vel.setRandom();
         Twist velJ = new Twist(vel);
         velJ.inverseTransform (TJW.R);
         //System.out.println ("i=" + i);

         // numeric result

         RigidTransform3d TBWx = new RigidTransform3d(TBW);
         vel.extrapolateTransformWorld (TBWx, h);
         Plane planeX = new Plane (planeB);
         planeX.transform (TBWx);
         planeX.inverseTransform (TJW);
         
         Vector3d nrmJX = new Vector3d();
         nrmJX.set (planeX.normal.x, planeX.normal.y, 0);
         double offJX = planeX.offset/nrmJX.norm();
         nrmJX.normalize();

         Point3d pnx = new Point3d();
         dist = nrmJX.dot (prJ) - offJX;
         pnx.scaledAdd (-dist, nrmJX, prJ);

         Vector3d dpn = new Vector3d();
         dpn.sub (pnx, pnJ);
         dpn.scale (1/h);
         dpn.transform (TJW.R);

         // velocity result

         Vector3d vnrm = new Vector3d();
         vnrm.cross (velJ.w, planeJ.normal);
         double voff = planeJ.normal.dot (velJ.v) + vnrm.dot (pbJ);

         Vector3d vnrm2 = new Vector3d(vnrm);
         vnrm2.z = 0;

         Vector3d vnrmJ = new Vector3d();
         vnrmJ.scale (n2mag, vnrm2);
         vnrmJ.scaledAdd (-nrm2.dot(vnrm2), nrmJ);
         vnrmJ.scale (1/sqr(n2mag));
         double voffJ =
            voff/n2mag - planeJ.offset*nrmJ.dot(vnrm2)/sqr(n2mag);

         Vector3d vpn = new Vector3d();
         vpn.scale (-(vnrmJ.dot(prJ)), nrmJ);
         vpn.scaledAdd (voffJ, nrmJ);
         vpn.scaledAdd (-(nrmJ.dot(prJ)-offJ), vnrmJ);
         vpn.transform (TJW.R);

         // matrix result


         Vector3d mpn = new Vector3d();
         Jt.mul (mpn, vel.v);
         Jr.mulAdd (mpn, vel.w, mpn);
         
         double toln = Math.max(1, dpn.norm())*1e-4;
         double tolm = Math.max(1, dpn.norm())*1e-11;

         if (!dpn.epsilonEquals (mpn, toln) ||
             !vpn.epsilonEquals (mpn, tolm)) {
            System.out.printf (
               "dpn=%s\n", dpn.toString ("%12.7f"));
            System.out.printf (
               "vpn=%s\n", vpn.toString ("%12.7f"));
            System.out.printf (
               "mpn=%s\n", mpn.toString ("%12.7f"));
            System.out.println ("");
            throw new TestException ("inconsistent results");
         }
      }
   }

   public void testNearRayPointDeriv (
      Plane plane, RigidTransform3d TBW, RigidTransform3d TJW,
      Point3d pr, Vector3d y) {

      Plane planeB = new Plane(plane);
      planeB.inverseTransform (TBW);

      int ntests = 10;
      Vector3d nrm = plane.normal;
      double off = plane.offset;
      Point3d pb = new Point3d (TBW.p);

      Point3d pn = new Point3d();

      pn.scaledAdd ((off - pr.dot(nrm))/y.dot(nrm), y, pr);

      double yDotN = y.dot(nrm);

      Matrix3d Jt = new Matrix3d();
      Jt.outerProduct (y, nrm);
      Jt.scale (1/yDotN);

      Vector3d v = new Vector3d();

      Matrix3d Jr = new Matrix3d();
      v.sub (pr, pb);
      Jr.outerProduct (y, v);
      Matrix3d YY = new Matrix3d();
      YY.outerProduct (y, y);
      Jr.scaledAdd ((off-pr.dot(nrm))/yDotN, YY);
      Jr.crossProduct (Jr, nrm);
      Jr.scale (1/yDotN);

      //System.out.println ("Jt=\n" + Jt.toString ("%12.7f"));
      //System.out.println ("Jr=\n" + Jr.toString ("%12.7f"));

      double h = 1e-8;
      for (int i=0; i<ntests; i++) {
         Twist vel = new Twist();
         vel.setRandom();

         // numeric result

         RigidTransform3d TBWx = new RigidTransform3d(TBW);
         vel.extrapolateTransformWorld (TBWx, h);
         Plane planeX = new Plane (planeB);
         planeX.transform (TBWx);
         Vector3d nrmX = planeX.normal;
         double offX = planeX.offset;         

         Point3d pnX = new Point3d();
         pnX.scaledAdd ((offX - pr.dot(nrmX))/y.dot(nrmX), y, pr);
         Vector3d dpn = new Vector3d();
         dpn.sub (pnX, pn);
         dpn.scale (1/h);

         // matrix result

         Vector3d mpn = new Vector3d();
         Jt.mul (mpn, vel.v);
         Jr.mulAdd (mpn, vel.w, mpn);
         
         double toln = Math.max(1, dpn.norm())*1e-5;

         if (!dpn.epsilonEquals (mpn, toln)) {
            System.out.printf (
               "dpn=%s\n", dpn.toString ("%12.7f"));
            System.out.printf (
               "mpn=%s\n", mpn.toString ("%12.7f"));
            System.out.println ("");
            throw new TestException ("inconsistent results");
         }
      }
   }

   public void testPlaneDeriv () {
      for (int i=0; i<10; i++) {
         RigidTransform3d TBW = new RigidTransform3d();
         Plane plane = new Plane();
         TBW.setRandom();
         plane.setRandom();
         testPlaneDeriv (plane, TBW);
      }
   }
   
   public void testProjectedPlaneDeriv () {
      for (int i=0; i<10; i++) {
         RigidTransform3d TBW = new RigidTransform3d();
         Plane plane = new Plane();
         TBW.setRandom();
         plane.setRandom();
         testProjectedPlaneDeriv (plane, TBW);
      }
   }
   
   public void testNearRayPointDeriv () {
      for (int i=0; i<100; i++) {
         //System.out.println ("i=" + i);
         RigidTransform3d TBW = new RigidTransform3d();
         RigidTransform3d TJW = new RigidTransform3d();
         Plane plane = new Plane();
         Point3d pr = new Point3d();
         Vector3d y = new Vector3d();

         TBW.setRandom(); TBW.p.scale (100);
         TJW.setRandom(); TJW.p.scale (100);
         plane.setRandom(); plane.offset *= 100;

         pr.setRandom();
         pr.scale (100);
         pr.z = 0;
         pr.transform (TJW);

         y.setRandom();
         y.z = 0;
         y.normalize();
         y.transform (TJW);

         testNearRayPointDeriv (plane, TBW, TJW, pr, y);
      }
   }

   public void testNearPointDeriv () {
      double near = INF;
      int inear = 0;
      RotationMatrix3d Rnear = new RotationMatrix3d();
      for (int i=0; i<1000000; i++) {
         //System.out.println ("i=" + i);
         RigidTransform3d TBW = new RigidTransform3d();
         RigidTransform3d TJW = new RigidTransform3d();
         Point3d pr = new Point3d();
         Plane plane = new Plane();
         TBW.setRandom(); TBW.p.scale (100);

         RotationMatrix3d RD = new RotationMatrix3d();
         RD.mulInverseLeft (myTBW.R, TBW.R);
         AxisAngle axisAng = RD.getAxisAngle();
         if (axisAng.angle < near) {
            near = axisAng.angle;
            inear = i;
            Rnear.set (TBW.R);
         }

         if (i == -132057) {
            TBW.R.set (myTBW.R);
         }

         //System.out.println ("TBW=\n" + TBW.toString ("%12.7f"));

         TJW.setRandom();
         plane.setRandom();
         plane.offset *= 100;

         TJW.p.scale (100);
         pr.setRandom();
         pr.scale (100);
         pr.z = 0;
         pr.transform (TJW);
         testNearPointDeriv (plane, TBW, TJW, pr);
      }

      System.out.printf ("inear=%d near=%g\n", inear, near);
      System.out.println ("Rnear=\n" + Rnear.toString ("%12.7f"));
      System.out.println ("Rbad=\n" + myTBW.R.toString ("%12.7f"));
   }

   static RigidTransform3d myTBW = new RigidTransform3d();
   static {
      myTBW.fromString (
         " [ 0.242755016   -0.896457948    0.370719773  -39.082976251\n" +
         " -0.401707554   -0.440740380   -0.802732184    4.799007046\n" +
         " 0.883006820    0.045946331   -0.467105867   -9.966307118 ]");
   }

   public void testSpecial() {
      RigidTransform3d TBW = new RigidTransform3d();
      RigidTransform3d TJW = new RigidTransform3d();
      Point3d pr = new Point3d();
      Plane plane = new Plane();

      plane.set ( -0.897657488, -0.438995446, 0.038652714, 37.905146455);
      pr.set (-50.563938484, 16.714058467, -1.845962881);

      TJW.fromString (
         " [ 0.750615277    0.575237842    0.325081729  -50.460391373\n"+
         "   0.575237842   -0.326859846   -0.749842694   16.681287062\n"+
         "  -0.325081729    0.749842694   -0.576244569   -1.744903928 ]");
      TBW.fromString (
         " [ 0.242755016   -0.896457948    0.370719773  -39.082976251\n" +
         " -0.401707554   -0.440740380   -0.802732184    4.799007046\n" +
         " 0.883006820    0.045946331   -0.467105867   -9.966307118 ]");
      pr.inverseTransform (TJW);
      pr.z = 0;
      pr.transform (TJW);

      TBW.setRandom(); TBW.p.scale (100);
      //TJW.setRandom(); TJW.p.scale (100);
      //plane.setRandom(); plane.offset *= 100;
      // pr.setRandom(); pr.scale (100);
      // pr.z = 0;
      // pr.transform (TJW);

      System.out.println ("TJW=\n" + TJW.toString("%14.9f"));
      System.out.println ("TBW=\n" + TBW.toString("%14.9f"));
      System.out.println ("plane=" + plane.toString ("%14.9f"));
      System.out.println ("pr=" + pr.toString ("%14.9f"));

      testNearPointDeriv (plane, TBW, TJW, pr);
   }

   public void test() {
      //tester.testPlaneDeriv();
      //tester.testProjectedPlaneDeriv();
      //testNearPointDeriv();
      //testSpecial();
      testNearRayPointDeriv();
   }
   
   public static void main (String[] args) {
      PlaneDerivTest tester = new PlaneDerivTest();
      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }
}
