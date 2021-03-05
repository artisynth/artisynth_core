package artisynth.core.mechmodels;

import java.util.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;

public class DualQuaternionDerivTest {

   public DualQuaternionDerivTest () {
   }

   class TransDesc {
      double myW;
      RigidTransform3d myTW;
      RigidTransform3d myTW0;
      DualQuaternion myDqr;

      TransDesc (double w, RigidTransform3d TW, RigidTransform3d TW0) {
         myW = w;
         myTW = new RigidTransform3d (TW);
         myTW0 = new RigidTransform3d (TW0);
         RigidTransform3d TD = new RigidTransform3d();
         TD.mulInverseRight (TW, TW0);
         myDqr = new DualQuaternion (TD);
      }
   }

   public double combine (DualQuaternion dq, List<TransDesc> transList) {
      dq.setZero();
      RigidTransform3d TD = new RigidTransform3d();
      double wtotal = 0;
      for (TransDesc td : transList) {
         TD.mulInverseRight (td.myTW, td.myTW0);
         dq.scaledAdd (td.myW, new DualQuaternion (TD));
         wtotal += td.myW;
      }
      return wtotal;
   }

   private void computeNumericJv (
      MatrixNd Jv, int j, double h, Point3d r, Point3d r0,
      List<TransDesc> transList) {

      DualQuaternion dq = new DualQuaternion();
      double wtotal = combine (dq, transList);
      dq.normalize();
      Point3d pr = new Point3d();
      dq.transform (pr, r);
      pr.scale (wtotal);

      Vector3d vel = new Vector3d(pr);
      vel.sub (r0);
      vel.scale (1/h);
      Jv.setColumn (j, vel);
   }

   private void computeNumericJq (
      MatrixNd Jq, int j, double h, DualQuaternion dq0,
      List<TransDesc> transList) {

      DualQuaternion dq = new DualQuaternion();
      double wtotal = combine (dq, transList);
      dq.normalize();
      DualQuaternion dv = new DualQuaternion();
      dv.setZero();
      dv.sub (dq, dq0);
      dv.scale (1/h);
      double[] vals = new double[8];
      dv.get (vals);
      Jq.setColumn (j, new VectorNd(vals));
   }

   private void computeDQrDri (
      Matrix4x3 DQrDri, Quaternion qr, Quaternion qri, double m0) {

      double si = qri.s;
      Vector3d ui = qri.u;
      double s = qr.s;
      Vector3d u = qr.u;

      Vector3d uCrossUi = new Vector3d();
      uCrossUi.cross (u, ui);
      Vector3d tmp = new Vector3d();

      tmp.scale (s*s-1, ui);
      tmp.scaledAdd (-si*s, u);
      tmp.scaledAdd (s, uCrossUi);
      DQrDri.setSubMatrix00 (tmp);
      
      Matrix3d M = new Matrix3d();
      M.setSkewSymmetric (ui);
      M.negate();
      M.addScaledOuterProduct (s, u, ui);
      M.addScaledOuterProduct (-si, u, u);
      M.addOuterProduct (u, uCrossUi);
      M.addDiagonal (si);

      DQrDri.setSubMatrix10 (M);
      DQrDri.scale (1/(2*Math.sqrt(m0)));
   }

   boolean checkMatrices (
      String msg, Matrix M0, Matrix M1, double tol, boolean silent) {
      
      MatrixNd Err = new MatrixNd (M0);
      Err.sub (new MatrixNd (M1));
      double err = Err.frobeniusNorm()/M1.frobeniusNorm();
      if (err > tol) {
         if (!silent) {
            System.out.println (msg + ", error=" + err);
            System.out.println ("\n" + M0.toString ("%12.7f"));
            System.out.println ("expected\n" + M1.toString ("%12.7f"));
            System.out.println ("Err=\n" + Err.toString ("%12.7f"));
         }
         return false;
      }
      else {
         return true;
      }
   }

   private MatrixNd computeJq (List<TransDesc> transList) {
      MatrixNd Jq = new MatrixNd(8, 6*transList.size());

      DualQuaternion dq = new DualQuaternion();
      double wtotal = combine (dq, transList);
      DualScalar m = dq.normSquared();
      double m0 = m.getReal();
      double me = m.getDual();
      double sqrtM0 = Math.sqrt(m0);
      dq.normalize();

      Quaternion qr = new Quaternion();
      Quaternion qt = new Quaternion();
      dq.getReal (qr);
      dq.getDual (qt);

      Matrix4d Mrr = new Matrix4d();
      Mrr.setIdentity();
      Mrr.addScaledOuterProduct (-1, qr, qr);
      Mrr.scale (1/sqrtM0);

      Matrix4d Mtr = new Matrix4d();
      Mtr.setIdentity();
      Mtr.addScaledOuterProduct (-1, qr, qr);
      Mtr.scale (-me/2);
      Mtr.addScaledOuterProduct (-m0, qr, qt);
      Mtr.addScaledOuterProduct (-m0, qt, qr);
      Mtr.scale (1/(sqrtM0*m0));

      int k = 0;
      for (TransDesc td : transList) {
         Matrix4x3 DQriDri = new Matrix4x3();
         Matrix4x3 DQtiDri = new Matrix4x3();
         DualQuaternionDeriv.computeDerivs (
            DQriDri, DQtiDri, td.myDqr, td.myTW0);

         // RigidTransform3d TD = new RigidTransform3d();
         // TD.mulInverseRight (td.myTW, td.myTW0);
         // //TD.set (td.myTW);
         // DualQuaternion dqi = new DualQuaternion(td.myTW);
         // Quaternion qri = new Quaternion();
         // Quaternion qti = new Quaternion();
         // dqi.getReal (qri);
         // dqi.getDual (qti);
         
         // Matrix4x3 DQriDri = computeDQrDr (qri);
         // Matrix4x3 DQtiDri = computeDQtDr (qti, qri);

         // // begin adjust for base transform
         // DualQuaternion dq0 = new DualQuaternion();
         // dq0.set (td.myTW0);
         // dq0.invert();
         // DualQuaternion dqx = new DualQuaternion();
         // dqx.mul (dqi, dq0);
         // if (dqx.dot (td.myDqr) < 0) {
         //    //dq0.scale (-1);
         // }
         // Quaternion qr0 = new Quaternion();
         // Quaternion qt0 = new Quaternion();
         // dq0.getReal (qr0);
         // dq0.getDual (qt0);

         // Matrix4x3 Dtmp0 = new Matrix4x3();
         // Matrix4x3 Dtmp1 = new Matrix4x3();
         // DualQuaternionDeriv.postmulDeriv (Dtmp1, DQtiDri, qr0);
         // DualQuaternionDeriv.postmulDeriv (Dtmp0, DQriDri, qt0);
         // DQtiDri.add (Dtmp0, Dtmp1);
         // DualQuaternionDeriv.postmulDeriv (Dtmp0, DQriDri, qr0);
         // DQriDri.set (Dtmp0);
         // // end adjust

         // Matrix4x3 D;
         // D = DualQuaternionDeriv.computeNumericDQtDr (td.myTW, td.myTW0);
         // if (checkMatrices ("DQtiDri", DQtiDri, D, 1e-7)) {
         //    System.out.println ("DQtiDri OK");
         // }

         // D = DualQuaternionDeriv.computeNumericDQrDr (td.myTW, td.myTW0);
         // if (checkMatrices ("DQriDri", DQriDri, D, 1e-7)) {
         //    System.out.println ("DQriDri OK");
         // }

         Matrix4x3 DQrDri = new Matrix4x3();
         Matrix4x3 DQtDri = new Matrix4x3();

         //computeDQrDri (DQrDri, qr, qri, m0);
         DQrDri.mulAdd (Mrr, DQriDri);
         DQrDri.scale (td.myW);
            
         DQtDri.mulAdd (Mtr, DQriDri);
         DQtDri.mulAdd (Mrr, DQtiDri);
         DQtDri.scale (td.myW);

         Jq.setSubMatrix (0, 6*k+3, DQrDri);
         Jq.setSubMatrix (4, 6*k, DQrDri);
         Jq.setSubMatrix (4, 6*k+3, DQtDri);
         k++;
      }
      return Jq;
   }

   private MatrixNd computeJv (List<TransDesc> transList, Point3d r) {
      MatrixNd Jv = new MatrixNd(3, 6*transList.size());

      DualQuaternion dq = new DualQuaternion();
      double wtotal = combine (dq, transList);
      DualScalar m = dq.normSquared();
      double m0 = m.getReal();
      double me = m.getDual();
      double sqrtM0 = Math.sqrt(m0);
      dq.normalize();

      Matrix3x4Block Jr = new Matrix3x4Block();
      Matrix3x4Block Jtt = new Matrix3x4Block();

      DualQuaternionDeriv.computeJrJtt (Jr, Jtt, dq, m0, me, r);

      int k = 0;
      for (TransDesc td : transList) {
         Matrix4x3 DQriDri = new Matrix4x3();
         Matrix4x3 DQtiDri = new Matrix4x3();
         DualQuaternionDeriv.computeDerivs (
            DQriDri, DQtiDri, td.myDqr, td.myTW0);

         // System.out.println ("DQriDri=\n" + DQriDri.toString ("%10.6f"));
         // System.out.println ("DQtiDri=\n" + DQtiDri.toString ("%10.6f"));
         //    System.out.println ("Jtt=\n" + Jtt.toString ("%10.6f"));
         //    System.out.println ("sqrtM0=" + sqrtM0);

         Matrix3d DrDri = new Matrix3d();
         DrDri.mulAdd (Jr, DQriDri);
         DrDri.mulAdd (Jtt, DQtiDri);
         DrDri.scale (wtotal*td.myW/sqrtM0);
         Jv.setSubMatrix (0, 6*k+3, DrDri);

         Matrix3d DrDti = new Matrix3d();
         DrDti.mulAdd (Jtt, DQriDri);
         DrDti.scale (wtotal*td.myW/sqrtM0);
         Jv.setSubMatrix (0, 6*k, DrDti);
         k++;
      }
      return Jv;
   }

   /**
    * Sets M to
    * <pre>
    *  u  -s I - [u]
    * </pre>
    */
   private void set (Matrix3x4 M, double s, Vector3d u) {
      M.m00 = u.x;
      M.m10 = u.y;
      M.m20 = u.z;

      M.m01 = -s;
      M.m02 = u.z;
      M.m03 = -u.y;

      M.m11 = -u.z;
      M.m12 = -s;
      M.m13 = u.x;

      M.m21 = u.y;
      M.m22 = -u.x;
      M.m23 = -s;
   }

   private Matrix4x3 computeDQrDr (Quaternion qr) {
      Matrix4x3 D = new Matrix4x3();
      DualQuaternionDeriv.computeDQrDr (D, qr);
      return D;
   }

   private Matrix4x3 computeDQtDr (Quaternion qt, Quaternion qr) {
      Matrix4x3 D = new Matrix4x3();
      DualQuaternionDeriv.computeDQtDr (D, qt, qr);
      return D;
   }

   public boolean test (
      Point3d r, List<TransDesc> transList, boolean silent) {

      double h = 1e-8;

      DualQuaternion dq0 = new DualQuaternion();
      double wtotal = combine (dq0, transList);
      DualScalar m0 = dq0.normSquared();
      dq0.normalize();

      Point3d r0 = new Point3d();
      dq0.transform (r0, r);
      r0.scale (wtotal);

      Quaternion qr = new Quaternion();
      Quaternion qt = new Quaternion();
      dq0.getReal (qr);
      dq0.getDual (qt);

      Vector3d u = qr.u;
      double s = qr.s;
      Vector3d z = qt.u;
      double t = qt.s;

      MatrixNd JvNum = new MatrixNd (3, 6*transList.size());
      MatrixNd JqNum = new MatrixNd (8, 6*transList.size());

      RotationMatrix3d Rinc = new RotationMatrix3d();
      RigidTransform3d TWsave = new RigidTransform3d();
      int j=0;
      for (TransDesc td : transList) {
         TWsave.set (td.myTW);
         for (int i=0; i<3; i++) {
            Vector3d pinc = new Vector3d();
            Vector3d w = new Vector3d();
            
            pinc.set (i, h);
            w.set (i, 1);
            Rinc.setAxisAngle (w, h);

            td.myTW.p.add (pinc);
            computeNumericJv (JvNum, j+i, h, r, r0, transList);
            computeNumericJq (JqNum, j+i, h, dq0, transList);
            td.myTW.set (TWsave);

            td.myTW.R.mul (Rinc,td.myTW.R);
            computeNumericJv (JvNum, j+i+3, h, r, r0, transList);
            computeNumericJq (JqNum, j+i+3, h, dq0, transList);
            td.myTW.set (TWsave);
         }
         j += 6;
      }

      MatrixNd Jq = computeJq (transList);
      boolean passed = true;
      if (!checkMatrices ("Jq", Jq, JqNum, 1e-6, silent)) {
         passed = false;
      }
      MatrixNd Jv = computeJv (transList, r);
      if (!checkMatrices ("Jv", Jv, JvNum, 1e-6, silent)) {
         passed = false;
      }
      return passed;
   }

   public void testDQtDr() {

      RigidTransform3d TW = new RigidTransform3d();

      TW.setRandom();
      DualQuaternion dq = new DualQuaternion();
      
      dq.set (TW);
      Quaternion qr = new Quaternion(); // rotational part
      Quaternion qt = new Quaternion(); // translation part
      dq.getReal (qr);
      dq.getDual (qt);
         
      Matrix4x3 DQtDr = new Matrix4x3();

      DualQuaternionDeriv.computeDQtDr (DQtDr, qt, qr);

      System.out.println ("DQtQr=\n" + DQtDr.toString("%12.7f"));

      Matrix4x3 Dchk = new Matrix4x3();
      double h = 1e-8;
      RigidTransform3d Tinc = new RigidTransform3d();
      RigidTransform3d TX = new RigidTransform3d();
      Quaternion qt0 = new Quaternion(qt);

      for (int j=0; j<3; j++) {
         Twist vel = new Twist();
         vel.set (j+3, h);
         vel.setTransform (Tinc);
         TX.p.set (TW.p);
         TX.R.mul (Tinc.R, TW.R);
         dq.set (TX);
         dq.getDual (qt);
         qt.sub (qt0);
         qt.scale (1/h);
         Dchk.setColumn (j, qt);
      }
      System.out.println ("DQtQrChk=\n" + Dchk.toString("%12.7f"));

      Matrix4x3 Err = new Matrix4x3();
      Err.sub (DQtDr, Dchk);
      double err = Err.frobeniusNorm()/Dchk.frobeniusNorm();
      System.out.println ("Err=" + err + "\n" + Err.toString("%12.7f"));
   }

   public void testBlendDeriv() {

      RigidTransform3d TW0 = new RigidTransform3d();
      RigidTransform3d TW = new RigidTransform3d();
      RigidTransform3d TD = new RigidTransform3d();

      TW0.setRandom();
      TW.setRandom();
      TD.mulInverseRight (TW, TW0);

      DualQuaternion dq = new DualQuaternion();
      
      MatrixNd D = new MatrixNd(8,6);
      dq.set (TW);
      Quaternion qr = new Quaternion(); // rotational part
      Quaternion qt = new Quaternion(); // translation part
      dq.getReal (qr);
      dq.getDual (qt);
         
      Matrix4x3 DQrDr = new Matrix4x3();
      Matrix4x3 DQtDr = new Matrix4x3();

      DualQuaternionDeriv.computeDQrDr (DQrDr, qr);
      DualQuaternionDeriv.computeDQtDr (DQtDr, qt, qr);

      // postmultiply DQrDr and DQtDr by the dual quaternion representing
      // inv(myBasePos) so they reflect derivatives of the blendQuaternion
      dq.set (TW0);
      dq.invert();
      dq.getReal (qr);
      dq.getDual (qt);

      Matrix4x3 Dtmp0 = new Matrix4x3();
      Matrix4x3 Dtmp1 = new Matrix4x3();
      DualQuaternionDeriv.postmulDeriv (Dtmp0, DQrDr, qr);

      D.setSubMatrix (0, 3, Dtmp0);
      D.setSubMatrix (4, 0, Dtmp0);
      DualQuaternionDeriv.postmulDeriv (Dtmp1, DQtDr, qr);
      DualQuaternionDeriv.postmulDeriv (Dtmp0, DQrDr, qt);
      Dtmp1.add (Dtmp0);
      D.setSubMatrix (4, 3, Dtmp1);
      System.out.println ("D=\n" + D.toString("%12.7f"));

      double h = 1e-8;
      RigidTransform3d Tinc = new RigidTransform3d();
      RigidTransform3d TX = new RigidTransform3d();
      DualQuaternion dq0 = new DualQuaternion();
      dq0.set (TD);

      MatrixNd Dchk = new MatrixNd (8,6);
      double[] vals = new double[8];
      for (int j=0; j<6; j++) {
         Twist vel = new Twist();
         vel.set (j, h);
         vel.setTransform (Tinc);
         if (j < 3) {
            TX.mul (Tinc, TW);
         }
         else {
            TX.p.set (TW.p);
            TX.R.mul (Tinc.R, TW.R);
         }
         TD.mulInverseRight (TX, TW0);
         dq.set (TD);
         dq.sub (dq0);
         dq.scale (1/h);
         dq.get (vals);
         Dchk.setColumn (j, vals);
      }
      System.out.println ("Dchk=\n" + Dchk.toString("%12.7f"));

      MatrixNd Err = new MatrixNd();
      Err.sub (D, Dchk);
      double err = Err.frobeniusNorm()/Dchk.frobeniusNorm();
      System.out.println ("Err=" + err + "\n" + Err.toString("%12.7f"));
   }

   public void test() {
      ArrayList<TransDesc> transList = new ArrayList<TransDesc>();
      RigidTransform3d T0W = new RigidTransform3d (1, 2, 3, 4, 5, 6);
      RigidTransform3d T1W = new RigidTransform3d (-2, 1, -3, 6, 5, 4);
      RigidTransform3d T2W = new RigidTransform3d (-1, -3, 4, -4, 6, 7);

      RigidTransform3d T0W0 = new RigidTransform3d ();
      RigidTransform3d T1W0 = new RigidTransform3d ();
      RigidTransform3d T2W0 = new RigidTransform3d ();
      Point3d r = new Point3d();

      int testCnt = 1000;
      int numTrans = 3;
      boolean silent = false;

      int failCnt = 0;

      // T0W.R.set (
      //    0.7316888688738209, 0.0, 0.6816387600233341,
      //    0.0, 1.0, 0.0,
      //    -0.6816387600233341, 0.0, 0.7316888688738209);
      // T0W.p.set (-0.6816387600233341, 0, 0.2683111311261791);
      // T0W0.set (T0W);

      // T1W.R.set (
      //    0.7316888688738209, -0.0, -0.6816387600233341,
      //    0.0, 1.0, -0.0,
      //    0.6816387600233341, 0.0, 0.7316888688738209);
      // T1W.p.set (0.6816387600233341, 0.0, 0.2683111311261791);
      // T1W0.set (T1W);

      for (int k=0; k<testCnt; k++) {

         transList.clear();
         T0W.setRandom();
         T1W.setRandom();
         T2W.setRandom();

         T0W0.setRandom();
         T1W0.setRandom();
         T2W0.setRandom();

         switch (numTrans) {
            case 1: {
               transList.add (new TransDesc (1.0, T0W, T0W0));
               break;
            }
            case 2: {
               // transList.add (new TransDesc (0.7575593, T0W, T0W0));
               // transList.add (new TransDesc (0.040721294, T1W, T1W0));
               transList.add (new TransDesc (0.1, T0W, T0W0));
               transList.add (new TransDesc (0.7, T1W, T1W0));
               break;
            }
            case 3: {
               transList.add (new TransDesc (0.1, T0W, T0W0));
               transList.add (new TransDesc (0.2, T1W, T1W0));
               transList.add (new TransDesc (0.6, T2W, T2W0));
               break;
            }
         }
         r.set (-0.5369, 0.0, 0.7064);
         if (!test (r, transList, silent)) {
            failCnt++;
         }
      }
      System.out.println ("failCnt=" + failCnt);
   }

   public static void main (String[] args) {
      DualQuaternionDerivTest tester = new DualQuaternionDerivTest();
      RandomGenerator.setSeed (0x1234);
      tester.test();
   }
}
