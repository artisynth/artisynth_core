package artisynth.core.mechmodels;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;

public class EBBeamBodyTest extends UnitTest {

   protected EBBeamBody createBody (
      double lenx, double stiffness, double density) {
      
      double leny = lenx/20;
      int nslices = 16;
      PolygonalMesh mesh =
         MeshFactory.createRoundedBox (
            lenx, lenx/20, lenx/20, 20, 4, 4, nslices / 2);
      RigidTransform3d XMB = new RigidTransform3d (
         lenx/2, 0, 0,  0, Math.PI/2, 0);
      EBBeamBody beam = new EBBeamBody (mesh, lenx, density, stiffness); 
      return beam;
   }

   private void computeEffectiveFrameMass (
      MatrixNd Mchk, EBBeamBody beam, SpatialInertia SI, RigidTransform3d TFL0) {

      RigidTransform3d TFL = new RigidTransform3d();
      beam.computeDeformedFrame (TFL, TFL0);

      MatrixNd S = new MatrixNd (3, 4);
      double xi = 2*TFL0.p.x/beam.getLength()-1;
      double s0 = beam.shape0 (xi);
      double s1 = beam.shape1 (xi);
      S.set (new double[] { 0, 0, 0, 0,  s0, s1, 0, 0,  0, 0, s0, s1 });

      MatrixNd STS = new MatrixNd (4, 4);
      STS.mulTransposeLeft (S, S);
      
      Point3d com = new Point3d (SI.getCenterOfMass());
      com.transform (TFL);

      MatrixNd LX = new MatrixNd (3, 3);
      LX.set (new double[] {
            0, com.z, -com.y,  -com.z, 0, com.x,  com.y, -com.x, 0 });

      MatrixNd LXX = new MatrixNd(3, 3);
      LXX.mul (LX, LX);
      LXX.negate();

      MatrixNd LXS = new MatrixNd (3, 4);
      LXS.mul (LX, S);

      Mchk.setZero();

      Mchk.set (0, 0, 1);
      Mchk.set (1, 1, 1);
      Mchk.set (2, 2, 1);

      Mchk.setSubMatrix (0, 3, LX);
      Mchk.setSubMatrix (0, 6, S);
      Mchk.setSubMatrix (3, 3, LXX);
      Mchk.setSubMatrix (3, 6, LXS);
      Mchk.setSubMatrix (6, 6, STS);
      
      Mchk.scale (SI.getMass());

      // set lower to upper
      for (int i=0; i<10; i++) {
         for (int j=i+1; j<10; j++) {
            Mchk.set (j, i, Mchk.get (i, j));
         }
      }

      MatrixNd RM = new MatrixNd (10, 10);
      RM.setSubMatrix (0, 0, beam.getPose().R);
      RM.setSubMatrix (3, 3, beam.getPose().R);
      RM.set (6, 6, 1);
      RM.set (7, 7, 1);
      RM.set (8, 8, 1);
      RM.set (9, 9, 1);

      Mchk.mulTransposeRight (Mchk, RM);
      Mchk.mul (RM, Mchk);
   }

   public void testAddEffectiveFrameMass() {

      int cnt = 10;
      for (int i=0; i<cnt; i++) {
         EBBeamBody beam = createBody (2.0, 0.1, 1.0);

         beam.setRandomPosState();

         SpatialInertia SI = new SpatialInertia();
         SI.setRandom();
         RigidTransform3d TFL0 = new RigidTransform3d();
         TFL0.setRandom();

         beam.addEffectiveFrameMass (SI, TFL0);
         MatrixNd Mres = beam.myAttachedFrameMassMatrix;
         MatrixNd Mchk = new MatrixNd(10,10);
         computeEffectiveFrameMass (Mchk, beam, SI, TFL0);
         MatrixNd Merr = new MatrixNd(10,10);
         Merr.sub (Mres, Mchk);
         double tol = 1e-14*Mchk.frobeniusNorm();
         if (Merr.frobeniusNorm() > tol) {
            System.out.println ("Mres=\n" + Mres.toString("%8.4f"));
            System.out.println ("Mchk=\n" + Mchk.toString ("%8.4f"));
            System.out.println ("Merr=\n" + Merr.toString ("%8.4f"));
            throw new TestException ("Incorrect attached frame mass");
         }
      }
   }

   public void testComputeFrameVel() {
      EBBeamBody beam = createBody (2.0, 0.1, 1.0);
      RigidTransform3d TFB = new RigidTransform3d (1.5, 0, 0);
      TFB.R.setRandom();
      int cnt = 10;
      for (int i=0; i<cnt; i++) {
         Twist vel = new Twist();
         RigidTransform3d TFW = new RigidTransform3d();
         MatrixNdBlock J = new MatrixNdBlock (beam.getVelStateSize(), 6);
         beam.setRandomPosState();
         // RigidTransform3d TBW = new RigidTransform3d();
         // TBW.p.setRandom();
         // beam.setPose (RigidTransform3d.IDENTITY);
         //beam.setElasticPos (new VectorNd (new double[] {1, 0, 0, 0}));

         beam.setRandomVelState();
         beam.computeFramePosVel (TFW, vel, J, null, TFB);

         Twist chk = new Twist();
         VectorNd v = new VectorNd(6);

         // now test vel using numeric differentiation

         MatrixNd Jchk = new MatrixNd (beam.getVelStateSize(), 6);

         double h = 1e-8;
         VectorNd pos0 = new VectorNd (beam.getPosStateSize());
         VectorNd pos = new VectorNd (beam.getPosStateSize());
         VectorNd dpos = new VectorNd (beam.getPosStateSize());
         RigidTransform3d TFW0 = new RigidTransform3d (TFW);

         beam.getPosState (pos0.getBuffer(), 0);
         for (int k=0; k<beam.getVelStateSize(); k++) {
            pos.set (pos0);
            dpos.setZero();
            dpos.set (k, 1.0);
            beam.addPosImpulse (pos.getBuffer(), 0, h, dpos.getBuffer(), 0);
            beam.setPosState (pos.getBuffer(), 0);
            beam.computeFramePosition (TFW, TFB);
            RigidTransform3d TD = new RigidTransform3d();
            TD.mulInverseLeft (TFW0, TFW);
            
            Twist dv = new Twist();
            dv.set (TD);
            dv.transform (TFW0.R);
            v.set (dv);
            v.scale (1/h);
            Jchk.setRow (k, v);
         }
         beam.setPosState (pos0.getBuffer(), 0);
         MatrixNd Err = new MatrixNd (J);
         Err.sub (Jchk);
         if (Err.frobeniusNorm()/J.frobeniusNorm() > 1e-6) {
            J.transpose();
            Jchk.transpose();
            Err.transpose();
            System.out.println ("J=\n" + J.toString ("%12.6f"));
            System.out.println ("Jchk=\n" + Jchk.toString ("%12.6f"));
            System.out.println ("Err=\n" + Err.toString ("%12.6f"));         
            throw new TestException (
               "Computed frame velocity Jacobian does not match numeric value");
         }

         // first test vel against multiplying by J

         VectorNd bvel = new VectorNd(beam.getVelStateSize());
         beam.getVelState (bvel.getBuffer(), 0);
         J.mulTranspose (v, bvel);
         chk.set (v);
         Twist err = new Twist();
         err.sub (vel, chk);
         if (err.norm() > 1e-10) {
            throw new TestException (
               "Frame velocity does not match that computed using Jacobian");
         }

      }
   }

   @Override
   public void test() {
      testComputeFrameVel();
      testAddEffectiveFrameMass();
   }

   public static void main (String[] args) {
      EBBeamBodyTest tester = new EBBeamBodyTest();
      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }
}
