package artisynth.core.probes;

import java.util.*;

import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import maspack.util.*;
import maspack.interpolation.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;

public class IKSolverTest extends UnitTest {

   private static final double RTOD = 180/Math.PI;
   private static final double DTOR = Math.PI/180;

   int myNumIters = 0;
   int myNumSolves = 0;

   private double avgNumIters() {
      if (myNumSolves == 0) {
         return 0;
      }
      else {
         return myNumIters/(double)myNumSolves;
      }
   }

   private void clearSolveCounts() {
      myNumIters = 0;
      myNumSolves = 0;
   }

   private MechModel createOneLinkBase (boolean bodyCoordsAtCom) {
      MechModel mech = new MechModel();

      // add dummy body to make sure reindexing works inside IKSolver
      RigidBody dummy = RigidBody.createBox (
         "dummy", 0.5, 0.5, 0.5, /*density*/1000);
      mech.addRigidBody (dummy);      

      RigidBody link0 = RigidBody.createBox (
         "link0", 1, 0.25, 0.25, /*density*/1000);
      link0.setPose (new RigidTransform3d (0, 0, 0.5,  0, -90*DTOR, 0));
      if (!bodyCoordsAtCom) {
         link0.translateCoordinateFrame (new Vector3d(0.0, 0.0, -0.5));
      }
      mech.addRigidBody (link0);

      HingeJoint joint = new HingeJoint(
         link0, null, new Point3d(), new Vector3d (0, -1, 0));
      mech.addBodyConnector (joint);
      return mech;
   }

   private MechModel createOneBodyMech(int numMkrs) {
      MechModel mech = new MechModel();
      RigidBody box = RigidBody.createBox ("box", 1, 1, 1, /*density*/1000);
      mech.addRigidBody (box);

      double[] mlocs = new double[] {
         0, 0, 0,
         0.5, 0, 0,
         0, 0.5, 0
      };
      Point3d loc = new Point3d();
      for (int i=0; i<numMkrs; i++) {
         loc.set (mlocs[i*3+0], mlocs[i*3+1], mlocs[i*3+2]);
         mech.addFrameMarker (box, loc);
      }
      return mech;
   }

   private MechModel createOneLinkMech (boolean bodyCoordsAtCom) {
      MechModel mech = createOneLinkBase(bodyCoordsAtCom);

      HingeJoint joint = (HingeJoint)mech.bodyConnectors().get(0);
      // set the range for theta (in degrees)
      joint.setMaxTheta (10.0);
      joint.setMinTheta (-10.0);

      RigidBody link0 = mech.rigidBodies().get("link0");
      mech.addFrameMarker (link0, new Point3d(0.5, 0, 0));
      return mech;
   }

   private MechModel createTwoLinkMech (boolean bodyCoordsAtCom) {
      MechModel mech = createOneLinkBase(bodyCoordsAtCom);
      RigidBody link0 = mech.rigidBodies().get("link0");

      RigidBody link1 = RigidBody.createBox (
         "link1", 0.6, 0.15, 0.15, /*density*/1000);
      link1.setPose (new RigidTransform3d (0, 0, 1.3,  0, -90*DTOR, 0));
      if (!bodyCoordsAtCom) {
         link1.translateCoordinateFrame (new Vector3d(0, 0, -0.3));
      }
      mech.addRigidBody (link1);

      HingeJoint joint = new HingeJoint(
         link1, link0, new Point3d(0, 0, 1), new Vector3d (0, -1, 0));
      mech.addBodyConnector (joint);
      // set the range for theta (in degrees)
      joint.setMaxTheta (30.0);
      joint.setMinTheta (-30.0);

      double[] mlocs = new double[] {
         0.0, 0, 1.6,
         -0.075, 0, 1.3, 
         0.075, 0, 1.3, 
         0, -0.075, 1.15,
         0,  0.075, 1.15,
         0, -0.125, 1.0,
         0,  0.125, 1.0,
         0.125, 0, 0.5,
         -0.125, 0, 0.5
      };
      Point3d loc = new Point3d();
      for (int i=0; i<mlocs.length/3; i++) {
         loc.set (mlocs[i*3+0], mlocs[i*3+1], mlocs[i*3+2]);
         if (i < 5) {
            mech.addFrameMarkerWorld (link1, loc);
         }
         else {
            mech.addFrameMarkerWorld (link0, loc);
         }
      }
      return mech;
   }
   
   private VectorNd packTargetVector (Collection<Point3d> pnts) {
      VectorNd mtargs = new VectorNd (3*pnts.size());
      int j=0;
      for (Point3d pnt : pnts) {
         mtargs.setSubVector (3*j, pnt);
         j++;
      }
      return mtargs;
   }

   public void testOneBody (MechModel mech) {
      IKSolver solver = new IKSolver (mech, mech.frameMarkers());

      ArrayList<Point3d> mtargs = new ArrayList<>();
      for (FrameMarker mkr : mech.frameMarkers()) {
         mtargs.add (new Point3d(mkr.getPosition()));
      }
      RigidTransform3d T = new RigidTransform3d();
      T.p.setRandom();
      T.R.setRpy (Math.toRadians(20), 0, 0);
      for (Point3d pnt : mtargs) {
         pnt.transform (T);
      }
      
      int niters = solver.solve (packTargetVector(mtargs));
      System.out.println ("niters=" + niters);
      for (int i=0; i<mtargs.size(); i++) {
         FrameMarker mkr = mech.frameMarkers().get(i);
         checkEquals ("Marker "+i, mkr.getPosition(), mtargs.get(i), 1e-8);
      }
   }      

   public void testOneLink () {
      MechModel mech = createOneLinkMech(/*bodyCoordsAtCom*/true);
      IKSolver solver = new IKSolver (mech, mech.frameMarkers());

      ArrayList<Point3d> mtargs = new ArrayList<>();
      mtargs.add (new Point3d(mech.frameMarkers().get(0).getPosition()));
      solver.solve (packTargetVector(mtargs));
      mtargs.get(0).set (new Point3d (0.2, 0.0, 1.0));
      Point3d chk = new Point3d (0.173746657573826, 0.0, 0.9847903832704314);
      int niters = solver.solve (packTargetVector(mtargs));
      System.out.println ("niters=" + niters);
      checkEquals (
         "Marker 0", mech.frameMarkers().get(0).getPosition(), chk, 1e-10);
   }      

   public void testTwoLink (int numMkrs, boolean bodyCoordsAtCom) {
      testTwoLink (
         numMkrs, bodyCoordsAtCom,
         /*zeroLink0Inertia*/false, /*shuffleMarkers*/false);
   }         

   public void testTwoLink (
      int numMkrs, boolean bodyCoordsAtCom, boolean zeroLink0Inertia,
      boolean reorderMarkers) {
      
      MechModel mech = createTwoLinkMech(bodyCoordsAtCom);

      if (zeroLink0Inertia) {
         mech.rigidBodies().get("link0").setInertia (new SpatialInertia());
      }

      RigidBody dummy = mech.rigidBodies().get("dummy");
      ArrayList<FrameMarker> mkrs = new ArrayList<>();
      VectorNd wgts = new VectorNd(numMkrs);
      for (int i=0; i<numMkrs; i++) {
         mkrs.add (mech.frameMarkers().get(i));
         wgts.set (i, (i%2)==0 ? 1.0 : 2.0);
      }
      if (reorderMarkers) {
         Collections.shuffle (mkrs);
      }
      IKSolver solver = new IKSolver (mech, mkrs, wgts);
      double[] testAngs = new double[] {
         0, 20,
         10, 20,
         30, 10,
         30, -10,
         30, -30,
         10, -30,
         -10, -20,
         -10, 0
      };
      HingeJoint joint0 = (HingeJoint)mech.bodyConnectors().get(0);
      HingeJoint joint1 = (HingeJoint)mech.bodyConnectors().get(1);
      for (int k=0; k<testAngs.length/2; k++) {
         if ((k%2) == 1) {
            // change dummy dynamic setting to cause solve indices to change
            dummy.setDynamic (!dummy.isDynamic());
         }
         double theta0 = joint0.getTheta();
         double theta1 = joint1.getTheta();
         joint0.setTheta (testAngs[2*k+0]);
         joint1.setTheta (testAngs[2*k+1]);
         VectorNd mtargs = collectMarkerPositions (mkrs, /*noise*/0);
         joint0.setTheta (theta0);
         joint1.setTheta (theta1);
         int niter = solver.solve (mtargs);
         //System.out.println ("niter=" + niter);
         VectorNd msolve = collectMarkerPositions (mkrs, /*noise*/0);
         checkEquals ("solved markers", msolve, mtargs, 1e-8);
      }
      myNumIters += solver.numIterations();
      myNumSolves += solver.numSolves();
   }

   public void testTwoLinkWithZeroLink0Inertia () {
      testTwoLink (
         2, /*bodyCoordsAtCom*/true, /*zeroLink0Inertia*/true,
         /*shuffleMarkers*/false);
   }

   public void testTwoLinkWithShuffledMarkers () {
      testTwoLink (
         2, /*bodyCoordsAtCom*/true, /*zeroLink0Inertia*/false,
         /*shuffleMarkers*/true);
   }

   VectorNd collectMarkerPositions (
      ArrayList<FrameMarker> mkrs, double noise) {
      VectorNd mpos = new VectorNd (3*mkrs.size());
      int k=0;
      Point3d pos = new Point3d();
      for (FrameMarker mkr : mkrs) {
         pos.set (mkr.getPosition());
         if (noise != 0) {
            Vector3d perturb = new Vector3d();
            perturb.setRandom ();
            pos.scaledAdd (noise, perturb);
         }
         mpos.setSubVector (3*k, pos);
         k++;
      }
      return mpos;
   }

   public void testTwoLinkWithNoise (int numMkrs, boolean bodyCoordsAtCom) {
      MechModel mech = createTwoLinkMech(bodyCoordsAtCom);

      ArrayList<FrameMarker> mkrs = new ArrayList<>();
      for (int i=0; i<numMkrs; i++) {
         mkrs.add (mech.frameMarkers().get(i));
      }
      HingeJoint joint0 = (HingeJoint)mech.bodyConnectors().get(0);
      HingeJoint joint1 = (HingeJoint)mech.bodyConnectors().get(1);
      joint1.setThetaRange (-45, 45);

      // trajectory specified with angles
      NumericList testAngs = new NumericList(2);
      testAngs.add (0,   0, 0);
      testAngs.add (0.5, 0, -45);
      testAngs.add (1,   0, 45);
      testAngs.add (2,   45, -45);
      testAngs.add (3,   -45, 45);

      IKSolver solver = new IKSolver (mech, mkrs);
      
      double tend = testAngs.getLast().t;
      int nintervals = 10;
      VectorNd angs = new VectorNd(2);
      for (int k=0; k<=nintervals; k++) {
         double t = k*tend/nintervals;
         testAngs.interpolate (angs, t);

         double theta0 = joint0.getTheta();
         double theta1 = joint1.getTheta();
         joint0.setTheta (angs.get(0));
         joint1.setTheta (angs.get(1));
         VectorNd mtargs = collectMarkerPositions (mkrs, /*noise*/0.05);
         joint0.setTheta (theta0);
         joint1.setTheta (theta1);
         int niter = solver.solve (mtargs);
         //System.out.println ("niter=" + niter);
         if (niter == -1) {
            System.out.println ("niter=" + niter);
            throw new TestException (
               "Solver did not converge with noise added, t=" + t);
         }
         VectorNd msolve = collectMarkerPositions (mkrs, /*noise*/0);
         //checkEquals ("solved markers", msolve, mtargs, 1e-8);
      }
      myNumIters += solver.numIterations();
      myNumSolves += solver.numSolves();
   }

   private Vector3d randomVector3d() {
      Vector3d vec = new Vector3d();
      vec.setRandom();
      return vec;
   }

   MatrixNd createJMatrix (ArrayList<Vector3d> pnts) {

      MatrixNd J = new MatrixNd (3*pnts.size(), 6);
      int pidx = 0;
      Matrix3d X = new Matrix3d();
      for (Vector3d p : pnts) {
         J.setSubMatrix (3*pidx, 0, Matrix3d.IDENTITY);
         X.setSkewSymmetric (p);
         X.scale (-1);
         J.setSubMatrix (3*pidx, 3, X);
         pidx++;
      }
      return J;
   }

   void testJTJ (ArrayList<Vector3d> pnts, double regc) {

      MatrixNd J = createJMatrix (pnts);
      SVDecomposition svd = new SVDecomposition (J);
      MatrixNd VJ = new MatrixNd(svd.getV());
      VectorNd sig = svd.getS();
      MatrixNd DJ = new MatrixNd(sig.size(), sig.size());
      DJ.setDiagonal (sig);

      // confirm that unmodified S = J^T J
      SpatialInertia Sraw = new SpatialInertia();
      for (Vector3d p : pnts) {
         Sraw.addPointMass (1, p);
      }
      MatrixNd Schk = new MatrixNd (6, 6);
      Schk.mulTransposeLeft (J, J);
      if (!Schk.epsilonEquals (Sraw, 1e-14)) {
         throw new TestException ("Unmodified S != J^T*J");
      }

      VectorNd wgts = new VectorNd(pnts.size());
      wgts.setAll (1.0);

      IntHolder rank = new IntHolder();
      SpatialInertia S = IKSolver.buildJTJMatrix (pnts, wgts, regc, rank);

      Matrix6d InvS = new Matrix6d();
      SpatialInertia.invert (InvS, S);
      
      MatrixNd P = new MatrixNd(InvS);
      P.mul (P, VJ);
      P.mul (P, DJ);
      P.mulTransposeLeft (VJ, P);
      P.mulTransposeLeft (DJ, P);
      for (int i=0; i<rank.value; i++) {
         P.set (i, i, P.get(i, i)-1);
      }
      if (P.frobeniusNorm() > 1e-10) {
         throw new TestException ("P-I=\n" + P.toString ("%16.12f"));
      }
   }

   public void testBuildJTJMatrix() {
      ArrayList<Vector3d> pnts = new ArrayList<>();

      int ntests = 5;

      // test zero  point:
      pnts.add (new Vector3d (0, 0, 0));
      testJTJ (pnts, 0.001);

      // test one point:
      pnts.add (new Vector3d (0.4, 1, -.1));
      testJTJ (pnts, 0.001);

      // test two identical points:
      pnts.clear();
      pnts.add (new Vector3d (0.4, 1, -.1));
      pnts.add (new Vector3d (0.4, 1, -.1));
      testJTJ (pnts, 0.001);

      // test two points:
      for (int i=0; i<ntests; i++) {
         pnts.clear();
         pnts.add (randomVector3d());
         pnts.add (randomVector3d());
         testJTJ (pnts, 0.001);
      }

      // test three colinear points:
      for (int i=0; i<ntests; i++) {
         Vector3d vec0 = randomVector3d();
         Vector3d vec1 = new Vector3d(vec0);
         vec1.scale (0.5);
         Vector3d vec2 = new Vector3d(vec0);
         vec1.scale (-1.2);
         pnts.clear();
         pnts.add (vec0);
         pnts.add (vec1);
         pnts.add (vec2);
         testJTJ (pnts, 0.001);
      }

      // test three general points:
      for (int i=0; i<ntests; i++) {
         pnts.clear();
         pnts.add (randomVector3d());
         pnts.add (randomVector3d());
         pnts.add (randomVector3d());
         testJTJ (pnts, 0.001);
      }
   }

   public void test() {
      testOneBody (createOneBodyMech(3));
      testOneBody (createOneBodyMech(2));
      testOneBody (createOneBodyMech(1));

      testOneLink();

      boolean bodyCoordsAtCom = true;
      clearSolveCounts();
      testTwoLink (2, bodyCoordsAtCom);
      testTwoLink (5, bodyCoordsAtCom);
      testTwoLink (9, bodyCoordsAtCom);
      System.out.printf ("iters: %g\n", avgNumIters());

      bodyCoordsAtCom = false;

      clearSolveCounts();
      testTwoLink (2, bodyCoordsAtCom);
      testTwoLink (5, bodyCoordsAtCom);
      testTwoLink (9, bodyCoordsAtCom);
      System.out.printf ("iters with offset body coords: %g\n", avgNumIters());

      testTwoLinkWithShuffledMarkers();

      clearSolveCounts();
      testTwoLinkWithZeroLink0Inertia();
      System.out.printf ("iters with link0 inertia 0: %g\n", avgNumIters());

      clearSolveCounts();
      testTwoLinkWithNoise (2, bodyCoordsAtCom);
      testTwoLinkWithNoise (5, bodyCoordsAtCom);
      testTwoLinkWithNoise (9, bodyCoordsAtCom);
      System.out.printf (
         "iters with offset body coords, noise: %g\n", avgNumIters());

      testBuildJTJMatrix();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      IKSolverTest tester = new IKSolverTest();
      tester.runtest();
   }

}
