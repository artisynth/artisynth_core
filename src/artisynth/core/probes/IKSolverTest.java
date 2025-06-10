package artisynth.core.probes;

import java.util.*;

import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.*;
import maspack.util.*;
import maspack.geometry.*;
import maspack.function.*;
import maspack.interpolation.*;
import maspack.interpolation.Interpolation.Order;
import maspack.spatialmotion.*;
import maspack.matrix.*;

public class IKSolverTest extends UnitTest {

   private static final double RTOD = 180/Math.PI;
   private static final double DTOR = Math.PI/180;

   // dimensions for the four-bar test case
   double BAR_LEN = 1.0;
   double BAR_WIDTH = 0.25;

   boolean useJointLimits = true;

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
      if (useJointLimits) {
         // set the range for theta (in degrees)
         joint.setMaxTheta (10.0);
         joint.setMinTheta (-10.0);
      }
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
      if (useJointLimits) {
         // set the range for theta (in degrees)
         joint.setMaxTheta (30.0);
         joint.setMinTheta (-30.0);
      }
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

   private RigidBody createFourBarLink (
      MechModel mech, String name, 
      double depth, double x, double z, double deg) {
      int nslices = 20; // num slices on the rounded mesh ends
      PolygonalMesh mesh =
         MeshFactory.createRoundedBox (BAR_LEN, BAR_WIDTH, depth, nslices);
      RigidBody body = RigidBody.createFromMesh (
         name, mesh, /*density=*/1000.0, /*scale=*/1.0);
      body.setPose (new RigidTransform3d (x, 0, z, 0, Math.toRadians(deg), 0));
      mech.addRigidBody (body);
      return body;
   }

   private MechModel createFourBarMech (int numMkrs) {
      MechModel mech = new MechModel ("mech");
      
      ArrayList<RigidBody> bars = new ArrayList<>();
      bars.add (createFourBarLink (mech, "link0", 0.2, -0.5,  0.0, 0));
      bars.add (createFourBarLink (mech, "link1", 0.3,  0.0,  0.5, 90));
      bars.add (createFourBarLink (mech, "link2", 0.2,  0.5,  0.0, 180));
      bars.add (createFourBarLink (mech, "link3", 0.3,  0.0, -0.5, 270));
      // ground the left bar
      bars.get(0).setGrounded (true);

      // connect the bars using four hinge joints
      HingeJoint[] joints = new HingeJoint[4];
      for (int j=0;j<4; j++) {
         // easier to locate the link using TCA and TDB since we know where frames
         // C and D are with respect the link0 and link1
         RigidTransform3d TCA = new RigidTransform3d (0, 0, 0.5, 0, 0, Math.PI/2);
         RigidTransform3d TDB = new RigidTransform3d (0, 0,-0.5, 0, 0, Math.PI/2);
         HingeJoint joint =
            new HingeJoint (bars.get(j), TCA, bars.get((j+1)%4), TDB);
         joint.setName ("joint"+j);
         mech.addBodyConnector (joint);
         joints[j] = joint;
      }
      // Set uniform compliance and damping for all bilateral constraints,
      // which are the first 5 constraints of each joint
      VectorNd compliance = new VectorNd(5);
      VectorNd damping = new VectorNd(5);
      for (int i=0; i<5; i++) {
         compliance.set (i, 0.0000001);
         damping.set (i, 0);
      }
      for (int i=0; i<joints.length; i++) {
         joints[i].setCompliance (compliance);
         joints[i].setDamping (damping);
      }

      // Add markers, starting at bar 1 and wrapping around to bar 0
      ArrayList<FrameMarker> mkrs = new ArrayList<>();
      for (int i=0; i<numMkrs; i++) {
         RigidBody bar = bars.get((i+1)%4);
         mkrs.add (mech.addFrameMarker (bar, new Point3d(-BAR_WIDTH/2, 0, 0)));
      }
      return mech;
   }

   /**
    * For the four-bar example, compute up to four marker positions as a
    * function of the angle of bar 1 with respect to the horizontal. Markers
    * 0-3 are attached to bars 1,2,3,0.
    */
   VectorNd computeFourBarMarkerPositions (double theta, int numMarkers) {
      VectorNd mpos = new VectorNd(3*numMarkers);
      double s = Math.sin (theta);
      double c = Math.cos (theta);
      
      Point3d pos = new Point3d();
      // marker 0 is attached to bar 1
      double x = c*BAR_LEN/2-s*BAR_WIDTH/2 - BAR_LEN/2;
      double z = s*BAR_LEN/2+c*BAR_WIDTH/2 + BAR_LEN/2;
      pos.set (x, 0, z);
      if (numMarkers > 0) {
         mpos.setSubVector (0, pos);
      }
      // marker 2 is attached to bar 3 and offset in z by -BAR_LEN
      x = c*BAR_LEN/2+s*BAR_WIDTH/2 - BAR_LEN/2;
      z = s*BAR_LEN/2-c*BAR_WIDTH/2 - BAR_LEN/2;
      pos.set (x, 0, z);
      if (numMarkers > 2) {
         mpos.setSubVector (6, pos);
      }
      // marker 1 is attached to bar 2
      x = c*BAR_LEN+BAR_WIDTH/2 - BAR_LEN/2;
      z = s*BAR_LEN;
      pos.set (x, 0, z);
      if (numMarkers > 1) {
         mpos.setSubVector (3, pos);
      }
      // marker 3 is fixed since bar 0 is fixed:
      pos.set (-(BAR_LEN+BAR_WIDTH)/2, 0, 0);
      if (numMarkers > 3) {
         mpos.setSubVector (9, pos);
      }
      return mpos;
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
         checkEquals ("Marker "+i, mkr.getPosition(), mtargs.get(i), 1e-7);
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
      if (useJointLimits) {
         checkEquals (
            "Marker 0", mech.frameMarkers().get(0).getPosition(), chk, 1e-10);
      }
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
         checkEquals ("solved markers", msolve, mtargs, 1e-7);
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
      if (useJointLimits) {
         joint1.setThetaRange (-45, 45);
      }

      // trajectory specified with angles
      NumericList testAngs = new NumericList(2);
      System.out.println (
         "interpolation=" + testAngs.getInterpolation().getOrder());
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
         //solver.debug = (t == 1.5);
         int niter = solver.solve (mtargs);
         //System.out.println ("niter=" + niter);
         if (niter == -1) {
            System.out.println ("niter=" + niter);
            throw new TestException (
               "Solver did not converge with noise added, t=" + t);
         }
         VectorNd msolve = collectMarkerPositions (mkrs, /*noise*/0);
         //checkEquals ("solved markers", msolve, mtargs, 1e-7);
      }
      myNumIters += solver.numIterations();
      myNumSolves += solver.numSolves();
   }

   public void testTwoLinkWithJointCoupling (
      int numMkrs, boolean bodyCoordsAtCom) {
      MechModel mech = createTwoLinkMech(bodyCoordsAtCom);

      ArrayList<FrameMarker> mkrs = new ArrayList<>();
      for (int i=0; i<numMkrs; i++) {
         mkrs.add (mech.frameMarkers().get(i));
      }
      HingeJoint joint0 = (HingeJoint)mech.bodyConnectors().get(0);
      HingeJoint joint1 = (HingeJoint)mech.bodyConnectors().get(1);
      if (useJointLimits) {
         joint0.setThetaRange (-60, 60);
         joint1.setThetaRange (-90, 90);
      }

      // add a coordinate coupling between the two joints to enforce
      // theta1 = scale*theta0;
      double scale = 1.5;
      ArrayList<JointCoordinateHandle> coords = new ArrayList<>();
      coords.add (new JointCoordinateHandle (joint1, 0));
      coords.add (new JointCoordinateHandle (joint0, 0));
      JointCoordinateCoupling coupling =
         new JointCoordinateCoupling (coords, new LinearFunction1x1 (scale, 0));
      mech.addConstrainer (coupling);

      // trajectory specified with angles
      NumericList testAngs = new NumericList(1);
      testAngs.setInterpolationOrder (Order.Linear);
      System.out.println (
         "interpolation=" + testAngs.getInterpolation().getOrder());
      testAngs.add (0,   0);
      testAngs.add (0.5, -59);
      testAngs.add (1,   59);
      testAngs.add (2,   0);

      IKSolver solver = new IKSolver (mech, mkrs);
      
      double tend = testAngs.getLast().t;
      int nintervals = 10;
      VectorNd angs = new VectorNd(2);
      // solve inverse kinematics for each of the joint angles
      for (int k=0; k<=nintervals; k++) {
         double t = k*tend/nintervals;
         testAngs.interpolate (angs, t);

         double savedTheta0 = joint0.getTheta();
         double savedTheta1 = joint1.getTheta();
         double theta0 = angs.get(0);
         joint0.setTheta (theta0);
         joint1.setTheta (scale*theta0);
         VectorNd mtargs = collectMarkerPositions (mkrs, /*noise*/0);
         joint0.setTheta (savedTheta0);
         joint1.setTheta (savedTheta1);
         int niter = solver.solve (mtargs);
         System.out.printf (
            "niter=%d t=%g\n", niter, t);
         if (niter == -1) {
            System.out.println ("niter=" + niter);
            throw new TestException (
               "Solver did not converge, t=" + t);
         }
         VectorNd msolve = collectMarkerPositions (mkrs, /*noise*/0);
         checkEquals ("solved markers", msolve, mtargs, 1e-7);
      }
      // solve each of the joint angles again, only using targets that are
      // infeasbile
      for (int k=0; k<=nintervals; k++) {
         double t = k*tend/nintervals;
         testAngs.interpolate (angs, t);

         double savedTheta0 = joint0.getTheta();
         double savedTheta1 = joint1.getTheta();
         double theta0 = angs.get(0);
         joint0.setTheta (theta0);
         // change scale for theta1 to make target infeasible
         joint1.setTheta (1.1*scale*theta0); 
         VectorNd mtargs = collectMarkerPositions (mkrs, /*noise*/0);
         joint0.setTheta (savedTheta0);
         joint1.setTheta (savedTheta1);
         int niter = solver.solve (mtargs);
         System.out.printf (
            "infeasible niter=%d t=%g\n", niter, t);
         if (niter == -1) {
            System.out.println ("niter=" + niter);
            throw new TestException (
               "Solver did not converge, t=" + t);
         }
         theta0 = joint0.getTheta();
         // check that joint angles are scaled correctly
         checkEquals ("theta1", joint1.getTheta(), theta0*scale, 1e-6);
      }

      myNumIters += solver.numIterations();
      myNumSolves += solver.numSolves();
   }

   public void testFourBar (int numMkrs) {
      
      MechModel mech = createFourBarMech (numMkrs);

      ArrayList<FrameMarker> mkrs = new ArrayList<>();
      for (int i=0; i<numMkrs; i++) {
         mkrs.add (mech.frameMarkers().get(i));
      }
      IKSolver solver = new IKSolver (mech, mkrs); 
      // four-bar is a one-dof system. We define its configuration by the angle
      // that the top-most link 1 makes with with horizontal.
      double[] testAngs = new double[] {
         20, 45, 15, 0, -15, -45 // test angles in degrees
      };
      System.out.println ("test four bar: " + numMkrs);
      RigidBody link0 = mech.rigidBodies().get(0);
      for (int k=0; k<testAngs.length; k++) {
         VectorNd mtargs = computeFourBarMarkerPositions (
            DTOR*testAngs[k], numMkrs);
         int niter = solver.solve (mtargs);
         //System.out.println ("niter=" + niter);
         VectorNd msolve = collectMarkerPositions (mkrs, /*noise*/0);
         checkEquals ("solved markers", msolve, mtargs, 1e-7);
         System.out.println (" " + link0.getPosition());
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
      SpatialInertia S = IKSolver.buildJTWJMatrix (pnts, wgts, regc, rank);

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

      System.out.println ("test one link");
      testOneLink();
      System.out.println ("done");

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

      bodyCoordsAtCom = true;
      clearSolveCounts();
      testTwoLinkWithJointCoupling (2, bodyCoordsAtCom);
      testTwoLinkWithJointCoupling (5, bodyCoordsAtCom);
      testTwoLinkWithJointCoupling (9, bodyCoordsAtCom);
      System.out.printf (
         "iters with joint coupling: %g\n", avgNumIters());

      testFourBar (1);
      testFourBar (2);
      testFourBar (3);
      testFourBar (4);

      testBuildJTJMatrix();

      System.out.println ("avg dqRatio=" + IKSolver.getAvgDqRatio());
      System.out.println ("total iters=" + IKSolver.myTotalNumIterations);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      IKSolverTest tester = new IKSolverTest();
      tester.runtest();
   }

}
