package artisynth.core.mechmodels;

import java.awt.Color;
import java.io.*;
import java.util.*;
import maspack.util.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import artisynth.core.mechmodels.CoordinateSetter.SetStatus;
import artisynth.core.modelbase.*;

/**
 * Test class for CoordinateSetter.
 */
public class CoordinateSetterTest extends UnitTest {

   // Code to create the test model:

   static boolean verbose = false;
   static boolean printIterations = false;

   static double jointCompliance = 1e-7;
   static double coordCompliance = 0;

   static double MAX_THETA = 270;
   static double MIN_THETA = -270;
   static double MAX_ROLL = 270;

   static double MAX_UJ_ROLL = 150;

   private static final double RTOD = 180/Math.PI;
   private static final double DTOR = Math.PI/180;

   private static double INF = Double.POSITIVE_INFINITY;

   private final double abs (double x) {
      return x >= 0 ? x : -x;
   }

   static double length = 0.5;
   static double width = length/4;
   static double radius = width/2;
   static int nslices = 32;
   static double density = 1000;
   static double shaftLen = 0.2;
   static double shaftRad = 0.015;
   
   int solveCnt;   

   private static UniversalJoint createUniversalJoint (
      MechModel mech, double x, double z, double deg, double roll,
      RigidBody bodyA, RigidBody bodyB, boolean compliant,
      double minRoll, double maxRoll) {

      UniversalJoint joint = new UniversalJoint ();
      joint.setRollRange (minRoll, maxRoll);
      joint.setPitchRange (-80, 80);
      joint.setShaftLength (shaftLen);
      joint.setShaftRadius (shaftRad);

      connectJointToBodies (joint, x, z, deg, roll, bodyA, bodyB, compliant);

      joint.setName ("joint"+mech.bodyConnectors().size());
      mech.addBodyConnector (joint);
      return joint;
   }

   private static HingeJoint createHingeJoint (
      MechModel mech, double x, double z, double deg, double theta,
      RigidBody bodyA, RigidBody bodyB, boolean compliant,
      double minTheta, double maxTheta) {

      HingeJoint joint = new HingeJoint ();
      joint.setThetaRange (minTheta, maxTheta);
      joint.setShaftLength (shaftLen);
      joint.setShaftRadius (shaftRad);

      connectJointToBodies (joint, x, z, deg, theta, bodyA, bodyB, compliant);
      joint.setName ("joint"+mech.bodyConnectors().size());
      mech.addBodyConnector (joint);
      return joint;
   }

   private static SlottedHingeJoint createSlottedJoint (
      MechModel mech, double x, double z, double deg, double theta,
      RigidBody bodyA, RigidBody bodyB, boolean compliant,
      double minTheta, double maxTheta) {

      SlottedHingeJoint joint = new SlottedHingeJoint ();
      joint.setThetaRange (minTheta, maxTheta);
      joint.setXRange (-0.9*length, 0);
      joint.setShaftLength (shaftLen);
      joint.setShaftRadius (shaftRad);
      joint.setSlotWidth (shaftRad);
      joint.setSlotDepth (1.1*width);

      connectJointToBodies (joint, x, z, deg, theta, bodyA, bodyB, compliant);
      joint.setName ("joint"+mech.bodyConnectors().size());
      mech.addBodyConnector (joint);
      return joint;
   }

   private static void connectJointToBodies (
      JointBase joint, double x, double z, double deg, double theta,
      RigidBody bodyA, RigidBody bodyB, boolean compliant) {

      RigidTransform3d TDW = new RigidTransform3d (length*x, 0, length*z);
      TDW.R.mulRotX (Math.PI/2);
      TDW.R.mulRotZ (Math.toRadians(deg));
         RigidTransform3d TCW = new RigidTransform3d (TDW);
      TCW.R.mulRotZ (Math.toRadians(theta));

      joint.setBodies (bodyA, bodyB, TCW, TDW);

      RenderProps.setFaceColor (joint, Color.BLUE);
      if (compliant) {
         int nconstraints = 6 - joint.numCoordinates();
         VectorNd compliance = new VectorNd(6);
         VectorNd damping = new VectorNd(6);
         for (int i=0; i<nconstraints; i++) {
            compliance.set (i, jointCompliance);
            damping.set (i, 0);
         }
         for (int i=nconstraints; i<6; i++) {
            compliance.set (i, coordCompliance);
            damping.set (i, 0);
         }
         joint.setCompliance (compliance);
         joint.setDamping (damping);
      }
   }

   private static RigidBody createTubeLink (
      double x, double z, double deg) {

      PolygonalMesh mesh = MeshFactory.createRoundedCylinder (
         radius, length, nslices/2, /*nsegs*/1, /*flatBottom*/false);
      mesh.transform (new RigidTransform3d (0, 0, 0,  0, Math.PI/2, 0));
      return createLink (mesh, x, z, deg);
   }

   private static RigidBody createTubeLink (
      MechModel mech, double x, double z, double deg) {

      PolygonalMesh mesh = MeshFactory.createRoundedCylinder (
         radius, length, nslices/2, /*nsegs*/1, /*flatBottom*/false);
      mesh.transform (new RigidTransform3d (0, 0, 0,  0, Math.PI/2, 0));
      return createLink (mech, mesh, x, z, deg);
   }

   private static RigidBody createBoxLink (
      double x, double z, double deg) {

      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         length, width, width, nslices/2);
      mesh.transform (new RigidTransform3d (0, 0, 0,  0, Math.PI/2, 0));

      return createLink (mesh, x, z, deg);
   }

   private static RigidBody createBoxLink (
      MechModel mech, double x, double z, double deg) {

      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         length, width, width, nslices/2);
      mesh.transform (new RigidTransform3d (0, 0, 0,  0, Math.PI/2, 0));

      return createLink (mech, mesh, x, z, deg);
   }

   private static RigidBody createLink (
      PolygonalMesh mesh, double x, double z, double deg) {

      double ang = Math.toRadians (deg);
      double s = Math.sin(ang);
      double c = Math.cos(ang);

      RigidBody link = RigidBody.createFromMesh (
         null, mesh, density, /*scale*/1.0);
      link.setPose (
         new RigidTransform3d (length*(x+c/2), 0, length*(z+s/2),  0, -ang, 0));
      return link;
   }

   private static RigidBody createLink (
      MechModel mech, PolygonalMesh mesh, double x, double z, double deg) {

      double ang = Math.toRadians (deg);
      double s = Math.sin(ang);
      double c = Math.cos(ang);

      RigidBody link = RigidBody.createFromMesh (
         null, mesh, density, /*scale*/1.0);
      link.setPose (
         new RigidTransform3d (length*(x+c/2), 0, length*(z+s/2),  0, -ang, 0));

      link.setName ("link"+mech.rigidBodies().size());
      mech.addRigidBody (link);
      return link;
   }

   public static MechModel createFourBarModel (double maxTheta) {
      MechModel mech = new MechModel ("mech");
      mech.setInertialDamping (2.0);
      RigidBody[] links = addFourBarModel (mech, 0.0, 0.0, maxTheta);
      links[0].setDynamic (false);
      RenderProps.setFaceColor (mech, new Color (0.8f, 0.8f, 1f));
      return mech;
   }

   public static RigidBody[] addFourBarModel (
      MechModel mech, double x0, double y0, double maxTheta) {

      RigidBody[] links = new RigidBody[4];
      JointBase[] joints = new JointBase[4];

      double s45 = Math.sin (Math.PI/4);
      double c45 = Math.sin (Math.PI/4);
     
      int k = 0;
      // first four bar
      links[0] = createBoxLink (mech, x0+0.0, y0+0.0, 0.0);
      links[1] = createBoxLink (mech, x0+1.0, y0+0.0, 45);
      links[2] = createBoxLink (mech, x0+0.0, y0+0.0, 45);
      links[3] = createBoxLink (mech, x0+c45, y0+s45, 0.0);

      boolean compliant = true;

      joints[0] = createHingeJoint (
         mech, x0+1.0, y0+0.0, 0.0, 45.0,
         links[1], links[0], compliant, -maxTheta, maxTheta);
      joints[1] = createHingeJoint (
         mech, x0+1.0+c45, y0+s45, 45.0, 135.0,
         links[3], links[1], compliant, -maxTheta, maxTheta);
      joints[2] = createHingeJoint (
         mech, x0+c45, y0+s45, 180.0, 45.0, links[2],
         links[3], compliant, -maxTheta, maxTheta);
      joints[3] = createHingeJoint (
         mech, x0+0.0, y0+0.0, -135.0, 135.0,
         links[0], links[2], compliant, -maxTheta, maxTheta);

      return links;
   }

   public static MechModel createTwinFourBarModel (double maxTheta) {
      MechModel mech = new MechModel ("mech");
      mech.setInertialDamping (2.0);
      RigidBody[] links = addTwinFourBarModel (mech, 0.0, 0.0, maxTheta);
      links[0].setDynamic (false);
      RenderProps.setFaceColor (mech, new Color (0.8f, 0.8f, 1f));
      return mech;
   }

   public static RigidBody[] addTwinFourBarModel (
      MechModel mech, double x0, double y0, double maxTheta) {

      RigidBody[] links = new RigidBody[7];
      JointBase[] joints = new JointBase[8];

      boolean compliant = true;
     
      int k = 0;
      // first four bar
      links[0] = createBoxLink (mech, x0+0.0, y0+0.0, 0.0);
      links[1] = createBoxLink (mech, x0+1.0, y0+0.0, 90);
      links[2] = createBoxLink (mech, x0+0.0, y0+0.0, 90);
      links[3] = createBoxLink (mech, x0+1.0, y0+1.0, 180);

      joints[0] = createHingeJoint (
         mech, x0+1.0, y0+0.0, 0.0, 90.0,
         links[1], links[0], compliant, -maxTheta, maxTheta);
      joints[1] = createHingeJoint (
         mech, x0+1.0, y0+1.0, 90.0, 90.0,
         links[3], links[1], compliant, -maxTheta, maxTheta);
      joints[2] = createHingeJoint (
         mech, x0+0.0, y0+1.0, 180.0, 90.0, links[2],
         links[3], compliant, -maxTheta, maxTheta);
      joints[3] = createHingeJoint (
         mech, x0+0.0, y0+0.0, -90.0, 90.0,
         links[0], links[2], compliant, -maxTheta, maxTheta);

      // second four bar
      links[4] = createBoxLink (mech, x0+1.0, y0+0.0, 0.0);
      links[5] = createBoxLink (mech, x0+2.0, y0+0.0, 90);
      links[6] = createBoxLink (mech, x0+2.0, y0+1.0, 180);

      joints[4] = createHingeJoint (
         mech, x0+2.0, y0+0.0, 0.0, 90.0,
         links[5], links[4], compliant, -maxTheta, maxTheta);
      joints[5] = createHingeJoint (
         mech, x0+2.0, y0+1.0, 90.0, 90.0,
         links[6], links[5], compliant, -maxTheta, maxTheta);
      joints[6] = createHingeJoint (
         mech, x0+1.0, y0+1.0, 180.0, 90.0,
         links[1], links[6], compliant, -maxTheta, maxTheta);
      joints[7] = createHingeJoint (
         mech, x0+1.0, y0+0.0, -90.0, 90.0,
         links[4], links[1], compliant, -maxTheta, maxTheta);

      return links;
   }

   public static MechModel createThreeBarModel (
      double minTheta, double maxTheta) {
      MechModel mech = new MechModel ("mech");
      mech.setInertialDamping (2.0);
      RigidBody[] links = addThreeBarModel (mech, 0.0, 0.0, minTheta, maxTheta);
      //links[0].setDynamic (false);
      RenderProps.setFaceColor (mech, new Color (0.8f, 0.8f, 1f));
      return mech;
   }

   public static RigidBody[] addThreeBarModel (
      MechModel mech, double x0, double y0, double minTheta, double maxTheta) {

      RigidBody[] links = new RigidBody[3];
      JointBase[] joints = new JointBase[3];

      double s30 = Math.sin (Math.PI/6);
      double c30 = Math.cos (Math.PI/6);

      links[0] = createBoxLink (mech, x0+0, y0+0, 90.0);
      links[1] = createBoxLink (mech, x0+0, y0+0, 30.0);
      links[2] = createBoxLink (mech, x0+c30, y0+s30, 150.0);

      joints[0] = createSlottedJoint (
         mech, x0+0, y0+0, -90.0, 120.0,
         links[1], links[0], true, minTheta, maxTheta);
      joints[1] = createSlottedJoint (
         mech, x0+c30, y0+s30, 30.0, 120.0,
         links[2], links[1], true, minTheta, maxTheta);
      joints[2] = createSlottedJoint (
         mech, x0+0, y0+1.0, 150.0, 120.0,
         links[0], links[2], true, minTheta, maxTheta);
      return links;
   }

   public static RigidBody[] addUJFourBarModel (
      MechModel mech, double x0, double y0, double maxRoll) {

      RigidBody[] links = new RigidBody[4];
      JointBase[] joints = new JointBase[4];

      double s45 = Math.sin (Math.PI/4);
      double c45 = Math.sin (Math.PI/4);
     
      // handle plus fours bar
      links[0] = createTubeLink (mech, x0+1.0, y0-0.5, 90.0);
      links[1] = createTubeLink (mech, x0+1.0, y0-0.5, 0.0);
      links[2] = createTubeLink (mech, x0+2.0, y0-0.5, 90.0);
      links[3] = createTubeLink (mech, x0+2.0, y0+0.5, 180.0);

      boolean compliant = true;

      joints[0] = createUniversalJoint (
         mech, x0+1.0, y0-0.5, -90.0, 90.0, 
         links[1], links[0], compliant, -maxRoll, maxRoll);
      joints[1] = createUniversalJoint (
         mech, x0+2.0, y0-0.5, 0.0, 90.0,
         links[2], links[1], compliant, -maxRoll, maxRoll);
      joints[2] = createUniversalJoint (
         mech, x0+2.0, y0+0.5, 90.0, 90.0,
         links[3], links[2], compliant, -maxRoll, maxRoll);
      joints[3] = createUniversalJoint (
         mech, x0+1.0, y0+0.5, 180.0, 90.0,
         links[0], links[3], compliant, -maxRoll, maxRoll);
      return links;
   }

   public static MechModel createUJFourBarModel () {
      return createUJFourBarModel (MAX_UJ_ROLL);
   }

   public static MechModel createUJFourBarModel (double maxRoll) {
      MechModel mech = new MechModel ("mech");
      mech.setInertialDamping (2.0);
      RigidBody[] links = addUJFourBarModel (mech, 0.0, 0.0, maxRoll);
      RenderProps.setFaceColor (mech, new Color (0.8f, 0.8f, 1f));
      return mech;
   }

   public static MechModel createMultiLoopModel() {
      MechModel mech = new MechModel ("mech");
      
      ArrayList<RigidBody> links = new ArrayList<>();
      ArrayList<JointBase> joints = new ArrayList<>();

      mech.setInertialDamping (2.0);

      double s45 = Math.sin (Math.PI/4);
      double c45 = Math.cos (Math.PI/4);

      double s30 = Math.sin (Math.PI/6);
      double c30 = Math.cos (Math.PI/6);

      boolean compliant = true;

      // handle
      RigidBody link0 = createTubeLink (mech, 0.0, 0.0, 0.0);
      link0.setDynamic (false);
      createUniversalJoint (
         mech, 0.0, 0.0, 0.0, 0.0, link0, null, !compliant, -INF, INF);

      RigidBody[] fblinks = addUJFourBarModel (mech, 0.0, 0.0, MAX_UJ_ROLL);

      createUniversalJoint (
         mech, 1.0, 0.0, 0.0, -90.0, fblinks[0], link0, compliant, -INF, INF);

      // distal links
      RigidBody link5 = createTubeLink (mech, 1.5, -0.5, -45);
      double l6x = 1.5+c45;
      double l6y = -(0.5+s45);
      RigidBody link6 = createTubeLink (mech, l6x, l6y, 0.0);
      RigidBody link7 = createTubeLink (mech, 1.5, 0.5, 45);

      createUniversalJoint (
         mech, 1.5, -0.5, 0.0, -45, link5, fblinks[1], false, -180.0, 180.0);
      createUniversalJoint (
         mech, l6x, l6y, 0.0, -45, link6, link5, false, -180.0, 180.0);
      createUniversalJoint (
         mech, 1.5, 0.5, 0.0, 45, link7, fblinks[3], false, -180.0, 180.0);

      // // last loop 

      RigidBody[] tblinks = addThreeBarModel (mech, l6x, s45, 15, 165);
      double l11x = l6x+c30/2;
      double l11y = s45+s30/2;
      
      RigidBody link11 = createTubeLink (mech, l11x, l11y, -90.0);

      createUniversalJoint (
        mech, l6x, -l6y, 30.0, -120.0, tblinks[0], link7, true, -INF, INF);
      createUniversalJoint (
         mech, l11x, l11y, 30.0, -120.0, link11, tblinks[1], true, -INF, INF);

      RenderProps.setFaceColor (mech, new Color (0.8f, 0.8f, 1f));
      return mech;
   }

   private VectorNd getCoordValuesDeg (MechModel mech) {
      VectorNd coordVals = new VectorNd();
      for (BodyConnector bcon : mech.bodyConnectors()) {
         JointBase joint = (JointBase)bcon;
         VectorNd cvals = new VectorNd();
         joint.getCoordinatesDeg (cvals);
         for (int i=0; i<joint.numCoordinates(); i++) {
            coordVals.append (cvals.get(i));
         }
      }
      return coordVals;
   }

   private VectorNd getCoordValues (MechModel mech) {
      VectorNd coordVals = new VectorNd();
      for (BodyConnector bcon : mech.bodyConnectors()) {
         JointBase joint = (JointBase)bcon;
         VectorNd cvals = new VectorNd();
         joint.getCoordinates (cvals);
         for (int i=0; i<joint.numCoordinates(); i++) {
            coordVals.append (cvals.get(i));
         }
      }
      return coordVals;
   }

   private ArrayList<JointCoordinateHandle> getCoordHandles (MechModel mech) {
      ArrayList<JointCoordinateHandle> handles = new ArrayList<>();
      for (BodyConnector bcon : mech.bodyConnectors()) {
         JointBase joint = (JointBase)bcon;
         for (int i=0; i<joint.numCoordinates(); i++) {
            handles.add (new JointCoordinateHandle(joint, i));
         }
      }
      return handles;
   }

   private void checkLinkage (
      MechModel mech, VectorNd coordChks, boolean[] dependent) {

      int ci = 0;
      for (BodyConnector bcon : mech.bodyConnectors()) {
         JointBase joint = (JointBase)bcon;
         VectorNd cvals = new VectorNd();
         joint.getCoordinatesDeg (cvals);
         for (int i=0; i<joint.numCoordinates(); i++) {
            if ((dependent == null || !dependent[ci]) &&
                Math.abs (cvals.get(i)-coordChks.get(ci)) > 1e-3) {
               throw new TestException (
                  "Joint "+joint.getName()+", coord "+i+
                  " is " + cvals.get(i) + ", expected " + coordChks.get(ci));
            }
            ci++;
         }
         Twist TCerr = joint.getCurrentTCError();
         if (TCerr.norm() > 1e-4) {
            throw new TestException (
               "Joint " + joint.getName()+": TCError is " + TCerr);
         }
      }
   }

   public double getRandomCoordVal (JointCoordinateHandle handle) {
      DoubleInterval range = handle.getValueRangeDeg();
      if (range.getRange() != INF) {
         return RandomGenerator.nextDouble (
            range.getLowerBound(), range.getUpperBound());
      }
      else if (handle.getMotionType() == MotionType.ROTARY) {
         return RandomGenerator.nextDouble (-180, 180);
      }
      else {
         return RandomGenerator.nextDouble (-1, 1);
      }
   }

   public boolean[] getDefaultMultiLoopDependents (int numc) {
      boolean[] dependent = new boolean[numc];
      for (int i=2; i<10; i++) {
         dependent[i] = true;
      }
      for (int i=18; i<24; i++) {
         dependent[i] = true;
      }
      return dependent;
   }

   SetStatus setCoordDeg (
      CoordinateSetter csetter, JointCoordinateHandle handle, double val) {
      SetStatus status = csetter.setCoordinateDeg (handle, val);
      solveCnt++;
      System.out.printf (
         "Setting "+getFullCoordName(handle)+" to %15.10f, got %15.10f, %s\n",
         val, handle.getValueDeg(), status);
      return status;
   }

   public void testMultiLoop() {
      RandomGenerator.setSeed (0x1234);
      MechModel mech = createMultiLoopModel();
      CoordinateSetter csetter = new CoordinateSetter (mech);
      
      VectorNd coordsDeg = getCoordValuesDeg(mech);
      int numc = coordsDeg.size();
      // check linkage with respect to initail values      
      checkLinkage (mech, coordsDeg, /*dependent*/null);

      SetStatus status = null;

      ArrayList<JointCoordinateHandle> handles = getCoordHandles (mech);
      ArrayList<JointCoordinateHandle> ujFourBarHandles = new ArrayList<>();
      ArrayList<JointCoordinateHandle> threeBarHandles = new ArrayList<>();
      for (int cidx=2; cidx<10; cidx++) {
         ujFourBarHandles.add (handles.get(cidx));
      }
      for (int cidx=18; cidx<24; cidx++) {
         threeBarHandles.add (handles.get(cidx));
      }
      // set restore position for three bar so that the x coordinates are not
      // at joint limits
      VectorNd coords0Deg = getCoordsDeg (handles);
      coords0Deg.set (18, -0.2);
      coords0Deg.set (20, -0.2);
      coords0Deg.set (22, -0.2);
      NumericState state0 = new NumericState();
      mech.getState (state0);

      int nstests = 100;
      for (int i=0; i<nstests; i++) {
         int cidx = 0;
         for (JointCoordinateHandle handle : handles) {
            //csetter.debug = (i==91 && cidx==2);
            status = testSingleCoord (i, csetter, handles, cidx);
            checkUJFourBar (status, ujFourBarHandles);
            checkThreeBar (status, threeBarHandles);
            cidx++;
         }
      }
      int nmtests = 100;
      for (int i=0; i<nmtests; i++) {
         status = testMultiCoord (i, csetter, handles, null);
         checkUJFourBar (status, ujFourBarHandles);
         checkThreeBar (status, threeBarHandles);
      }
      int[] indepCidxs = new int[] {
         0, 1, // open chain links
         2, 3, // uj four bar
         10, 11, 12, 13, 14, 15, 16, 17, // open chain links
         18, 19, 20, // three bar
         24, 25, 26, 27 };

      mech.setState (state0);
      for (int i=0; i<nmtests; i++) {
         status = testMultiCoord (i, csetter, handles, indepCidxs);
         checkUJFourBar (status, ujFourBarHandles);
         checkThreeBar (status, threeBarHandles);
         //csetter.debug = (i == 0);
         status = testMultiRestore (
            i, csetter, handles, coords0Deg, indepCidxs);
         checkUJFourBar (status, ujFourBarHandles);
         checkThreeBar (status, threeBarHandles);
         mech.setState (state0);
      }
   }

   double reduceRangeDeg (double deg) {
      while (deg > 180) {
         deg -= 360;
      }
      while (deg < -180) {
         deg += 360;
      }
      return deg;
   }

   double reduceRange (double ang) {
      while (ang > Math.PI) {
         ang -= 2*Math.PI;
      }
      while (ang < -Math.PI) {
         ang += 2*Math.PI;
      }
      return ang;
   }

   private enum FourBarConfig {
      OPEN,
      FOLDED,
      SINGULAR
   };


   private String getCoordName (JointCoordinateHandle handle) {
      return handle.getName() + handle.getJoint().getNumber();
   }

   private String getFullCoordName (JointCoordinateHandle handle) {
      return handle.getJoint().getName() + " " + handle.getName();
   }

   void checkThreeBar (
      SetStatus status, List<JointCoordinateHandle> handles) {
      
      if (status.converged()) {
         
         double degTol = 1e-4; // answer tolerance (degrees)
         // check that all angles add up to 0
         double degSum = 0;
         for (JointCoordinateHandle handle : handles) {
            if (handle.getMotionType() == MotionType.ROTARY) {
               degSum += handle.getValueDeg();
            }
         }
         if (Math.abs(reduceRangeDeg(degSum)) > degTol) {
            showError (
               "angle sum is "+degSum+" (degrees); should be near 0");
         }
      }
   }

   void checkGenericModel (
      SetStatus status, List<JointCoordinateHandle> handles,
      VectorNd checksDeg) {

      double degTol = 1e-4;
      VectorNd coordsDeg = getCoordsDeg (handles);     
      int cidx = 0;
      double maxErr = 0;
      for (JointCoordinateHandle handle : handles) {
         if (status.isFree (handle)) {
            double chk = checksDeg.get(cidx);
            double val = coordsDeg.get(cidx);
            double err = Math.abs(val-chk);
            if (status.converged() && err > degTol) {
               showError (
                  getCoordName(handle) + " is "+val+"; expecting "+chk);
            }
            if (err > maxErr) {
               maxErr = err;
            }
         }
         cidx++;
      }
      if (verbose && !status.converged()) {
         maxErr = Math.abs(reduceRangeDeg (maxErr));
         System.out.printf ("MAX angle error=%10.5f (degrees)\n", maxErr);
      }
      checkJointErrors (handles);
   }

   void checkFourBar (
      SetStatus status, List<JointCoordinateHandle> handles) {
      
      if (status.converged()) {
         VectorNd coords = getCoords (handles);
         VectorNd coordsDeg = getCoordsDeg (handles);

         double ang0 = coords.get(0);
         double ang1 = coords.get(1);
         double ang2 = coords.get(2);
         double ang3 = coords.get(3);
         
         double sin0 = Math.sin(ang0);
         double sin1 = Math.sin(ang1);
         double sin2 = Math.sin(ang2);
         double sin3 = Math.sin(ang3);

         double tol = DTOR*1e-2; // answer tolerance (radians)
         double degTol = 1e-4; // answer tolerance (degrees)
         boolean nearSingular = true;
         for (int j=0; j<coords.size(); j++) {
            double ang = coords.get(j);
            if (Math.abs (Math.sin(ang)) > DTOR*2) {
               nearSingular = false;
            }
         }
         if (nearSingular) {
            degTol = 1e-2;
         }
         // check that all angles add up to 0
         if (Math.abs(reduceRangeDeg(coordsDeg.sum())) > degTol) {
            showError (
               "angle sum is "+coordsDeg.sum()+" (degrees); should be near 0");
         }
               
         // check possible solution configurations
         FourBarConfig config = null;            
         if (reduceRange (ang0-ang2) < tol && reduceRange (ang1-ang3) < tol) {
            config = FourBarConfig.OPEN;
         }
         else if (abs(sin0) < tol && abs(sin1) < tol && 
                  abs(sin2) < tol && abs(sin3) < tol) {
            config = FourBarConfig.SINGULAR;
         }
         else if ((reduceRange (ang0+ang2) < tol &&
                   abs(sin1) < tol && abs(sin3) < tol) ||
                  (reduceRange (ang1+ang3) < tol &&
                   abs(sin0) < tol && abs(sin2) < tol)) {
            config = FourBarConfig.FOLDED;
         }
         if (config == null) {
            showError (
               "Incorrect solution configuration; coords=" +
               coordsDeg.toString ("%9.4f")+ ", tol=" + RTOD*tol +
               " (degrees)");
         }
      }
   }

   void checkUJFourBar (
      SetStatus status, List<JointCoordinateHandle> handles) {

      VectorNd coords = getCoords (handles);
      VectorNd coordsDeg = getCoordsDeg (handles);

      double roll0 = coordsDeg.get(0);
      double roll1 = coordsDeg.get(2);
      double roll2 = coordsDeg.get(4);
      double roll3 = coordsDeg.get(6);
         
      double pitch0 = coordsDeg.get(1);
      double pitch1 = coordsDeg.get(3);
      double pitch2 = coordsDeg.get(5);
      double pitch3 = coordsDeg.get(7);

      double tol = DTOR*1e-3; // answer tolerance (radians)
      double degTol = 1e-3; // answer tolerance (degrees)

      double roll02SumErr = Math.abs(reduceRangeDeg(roll0+roll2));
      double roll02SubErr = Math.abs(reduceRangeDeg(roll0-roll2));

      RotationMatrix3d R = new RotationMatrix3d();
      R.mulRotZ (coords.get(0));
      R.mulRotY (coords.get(1));
      R.mulRotZ (coords.get(2));
      R.mulRotY (coords.get(3));
      R.mulRotZ (coords.get(4));
      R.mulRotY (coords.get(5));
      R.mulRotZ (coords.get(6));
      R.mulRotY (coords.get(7));
      AxisAngle axisAng = new AxisAngle();
      R.getAxisAngle (axisAng);
      double degErr = Math.abs(RTOD*axisAng.angle);
      if (degErr > degTol) {
         showError (
            String.format (
               "net rotation differs from identity by %9.5f (degrees)", degErr));
      }

      // if (roll02SumErr <= degTol ||
      //     (Math.abs(Math.abs(roll0-roll2)-360) <= degTol)) {
      //    // if roll0+roll2 = 0, then we should have
      //    //    roll1+roll3 = 0, pitch0+pitch2 = 0, pitch1+pitch3 = 0
      //    if (Math.abs(reduceRangeDeg(roll1+roll3)) > degTol) {
      //       showError ("roll1+roll3 != 0, are "+roll1+" and "+roll3);
      //    }
      //    if (Math.abs(reduceRangeDeg(pitch0+pitch2)) > degTol) {
      //       showError ("pitch0+pitch2 != 0, are "+pitch0+" and "+pitch2);
      //    }
      //    if (Math.abs(reduceRangeDeg(pitch1+pitch3)) > degTol) {
      //       showError ("pitch1+pitch3 != 0, are "+pitch1+" and "+pitch3);
      //    }
      // }
      // else if (roll02SubErr <= degTol) {
      //    // if roll0-roll2 = 0, then we should have
      //    //    roll1-roll3 = 0, pitch0-pitch2 = 0, pitch1-pitch3 = 0
      //    if (Math.abs(reduceRangeDeg(roll1-roll3)) > degTol) {
      //       showError ("roll1-roll3 != 0, are "+roll1+" and "+roll3);
      //    }
      //    if (Math.abs(reduceRangeDeg(pitch0-pitch2)) > degTol) {
      //       showError ("pitch0-pitch2 != 0, are "+pitch0+" and "+pitch2);
      //    }
      //    if (Math.abs(reduceRangeDeg(pitch1-pitch3)) > degTol) {
      //       showError ("pitch1-pitch3 != 0, are "+pitch1+" and "+pitch3);
      //    }
      // }
      // else {
      //    coords.scale (RTOD);
      //    NumberFormat fmt = new NumberFormat ("bad config, err=%7.4f");
      //    String errMsg = fmt.format(Math.min(roll02SumErr, roll02SubErr));
      //    if (status.converged()) {
      //       errMsg += ", CONVRGD: ";
      //    }
      //    else {
      //       errMsg += ", NO CONV: ";
      //    }
      //    showError (errMsg + coords.toString("%8.4f"));
      // }
   }

   private VectorNd getCoordsDeg (List<JointCoordinateHandle> handles) {
      VectorNd coordsDeg = new VectorNd(handles.size());
      int cidx = 0;
      for (JointCoordinateHandle handle : handles) {
         coordsDeg.set (cidx++, handle.getValueDeg());
      }
      return coordsDeg;
   }

   private VectorNd getCoords (List<JointCoordinateHandle> handles) {
      VectorNd coords = new VectorNd(handles.size());
      int cidx = 0;
      for (JointCoordinateHandle handle : handles) {
         coords.set (cidx++, handle.getValue());
      }
      return coords;
   }

   private SetStatus testSingleCoord (
      int testNum, CoordinateSetter csetter,
      ArrayList<JointCoordinateHandle> handles, int cidx) {
      JointCoordinateHandle handle = handles.get (cidx);

      String cname = getCoordName(handle);
      int numc = handles.size();

      VectorNd checksDeg = new VectorNd (numc);
      double chk = getRandomCoordVal (handle);
      
      if (verbose) {
         System.out.printf ("test %d, setting %s to %s\n", testNum, cname, chk);
      }
      checksDeg.set (cidx, chk);
      SetStatus status = csetter.setCoordinateDeg (handle, chk);
      solveCnt++;
      if (verbose || !status.converged()) {
         System.out.printf (
            "%3dS DONE %s niters=%d\n",
            testNum, status, status.numIterations());
      }
      if (verbose) {
         printCoordNames("joints=    ", handles);
         printCoords("COORDS=    ", handles);
      }
      checkGenericModel (status, handles, checksDeg);
      return status;
   }
   
   int myTestNum;

   private SetStatus testMultiCoord (
      int testNum, CoordinateSetter csetter,
      ArrayList<JointCoordinateHandle> handles,
      int[] cidxs) {

      int numc = handles.size();
      if (cidxs == null) {
         do {
            cidxs = RandomGenerator.randomSubsequence (numc);
         }
         while (cidxs.length < 2);
      }

      VectorNd checksDeg = new VectorNd (numc);

      String cnames = null;
      VectorNd targets = new VectorNd();
      for (int cidx : cidxs) {
         JointCoordinateHandle handle = handles.get (cidx);         
         double chk = getRandomCoordVal (handle);
         targets.append (chk);
         checksDeg.set (cidx, chk);
         csetter.requestDeg (handle, chk);
         String cname = getCoordName(handle);
         cnames = (cnames == null ? cname : cnames + "," + cname);
      }
      if (verbose) {
         System.out.printf (
            "test %d, setting %s to %s\n",
            testNum, cnames, targets.toString("%10.5f"));
      }

      SetStatus status = csetter.setCoordinates();
      solveCnt++;
      if (verbose || !status.converged()) {
         System.out.printf (
            "%3dM DONE %s niters=%d\n",
            testNum, status, status.numIterations());
      }
      if (verbose) {
         printCoordNames("joints=    ", handles);
         printCoords("COORDS=    ", handles);
      }
      myTestNum = testNum;
      checkGenericModel (status, handles, checksDeg);
      return status;
   }

   private void printCoords (
      String label, List<JointCoordinateHandle> handles) {
      System.out.print (label);
      for (JointCoordinateHandle handle : handles) {
         System.out.printf (" %10.5f", handle.getValueDeg());
      }
      System.out.println ("");
   }

   private void printCoordNames (
      String label, List<JointCoordinateHandle> handles) {
      System.out.print (label);
      for (JointCoordinateHandle handle : handles) {
         System.out.printf (" %10s", getCoordName (handle));
      }
      System.out.println ("");
   }

   private SetStatus testMultiRestore (
      int testNum, CoordinateSetter csetter,
      ArrayList<JointCoordinateHandle> handles,
      VectorNd targetsDeg,
      int[] cidxs) {

      int numc = handles.size();

      if (cidxs != null) {
         for (int k=0; k<cidxs.length; k++) {
            JointCoordinateHandle handle = handles.get (cidxs[k]);         
            csetter.requestDeg (handle, targetsDeg.get(cidxs[k]));
         }
      }
      else {
         for (int cidx=0; cidx<numc; cidx++) {
            JointCoordinateHandle handle = handles.get (cidx);         
            csetter.requestDeg (handle, targetsDeg.get(cidx));
         }
      }

      if (verbose) {
         System.out.printf ("test %d, restoring\n", testNum);
      }
      SetStatus status = csetter.setCoordinates();
      solveCnt++;
      if (verbose || !status.converged()) {
         System.out.printf (
            "%3dR DONE %s niters=%d\n",
            testNum, status, status.numIterations());
      }
      VectorNd coordsDeg = getCoordsDeg (handles);
      if (!coordsDeg.epsilonEquals (targetsDeg, 1e-4)) {
         showError ("incorrect restored value: "+coordsDeg.toString ("%10.5f"));
      }
      if (verbose) {
         System.out.println ("RESTORE=   " + coordsDeg.toString ("%10.5f"));
      }
      myTestNum = testNum;
      checkGenericModel (status, handles, targetsDeg);
      return status;
   }

   public void testFourBar() {
      MechModel mech = createFourBarModel(MAX_THETA);
      CoordinateSetter csetter = new CoordinateSetter (mech);

      ArrayList<JointCoordinateHandle> handles = getCoordHandles (mech);
      int numc = handles.size();
      VectorNd coordsDeg = getCoordsDeg (handles);
      checkLinkage (mech, coordsDeg, new boolean[numc]);

      int ntests = 100;
      solveCnt = 0;

      for (int i=0; i<ntests; i++) {
         int cidx = 0;
         for (JointCoordinateHandle handle : handles) {
            SetStatus status = testSingleCoord (i, csetter, handles, cidx);
            checkFourBar (status, handles);
            cidx++;
         }
      }
      for (int i=0; i<ntests; i++) {
         //csetter.debug = (i == 12);
         SetStatus status = testMultiCoord (i, csetter, handles, null);
         checkFourBar (status, handles);
      }
      if (printIterations) {
         System.out.println (
            "avg iterations: " + csetter.numIterations()/(double)solveCnt);
      }
   }
      
   void showError (String msg) {
      throw new TestException (msg);
      //System.out.println ("ERROR: " + msg);
   }        

   private void writeState (MechModel mech, String fileName) {
      try {
         mech.writeState (fileName);
      }
      catch (IOException e) {
         e.printStackTrace(); 
      }
   }       

   public void testUJFourBar() {
      MechModel mech = createUJFourBarModel(MAX_UJ_ROLL);
      CoordinateSetter csetter = new CoordinateSetter (mech);

      ArrayList<JointCoordinateHandle> handles = getCoordHandles (mech);
      int numc = handles.size();
      VectorNd coordsDeg = getCoordsDeg (handles);
      checkLinkage (mech, coordsDeg, new boolean[numc]);

      int ntests = 100;
      solveCnt = 0;

      NumericState state0 = new NumericState();
      mech.getState (state0);
      for (int i=0; i<ntests; i++) {
         int cidx = 0;
         for (JointCoordinateHandle handle : handles) {
            //csetter.debug = (i == 31 && cidx==7);
            // if (csetter.debug) {
            //    writeState (mech, "mech.txt");
            // }
            SetStatus status = testSingleCoord (i, csetter, handles, cidx);
            checkUJFourBar (status, handles);
            cidx++;
         }
      }
      for (int i=0; i<4*ntests; i++) {
         //csetter.applyPolish = (i == 4);
         SetStatus status = testMultiCoord (i, csetter, handles, null);
         checkUJFourBar (status, handles);
         mech.setState (state0);
      }
      if (printIterations) {
         System.out.println (
            "avg iterations: " + csetter.numIterations()/(double)solveCnt);
      }
   }

   public void testUJFourBarRestore() {
      MechModel mech = createUJFourBarModel(MAX_UJ_ROLL);
      CoordinateSetter csetter = new CoordinateSetter (mech);
      ArrayList<JointCoordinateHandle> handles = getCoordHandles (mech);
      int numc = handles.size();

      int ntests = 100;
      solveCnt = 0;

      VectorNd coords0Deg = getCoordsDeg (handles);
      NumericState state0 = new NumericState();
      mech.getState (state0);
      for (int i=0; i<ntests; i++) {
         SetStatus status = testMultiCoord (
            i, csetter, handles, new int[] { 0, 1});
         checkUJFourBar (status, handles);
         status = testMultiRestore (
            i, csetter, handles, coords0Deg, new int[] {0, 1});
         checkUJFourBar (status, handles);
         mech.setState (state0);
      }
      if (printIterations) {
         System.out.println (
            "avg iterations: " + csetter.numIterations()/(double)solveCnt);
      }
   }

   private void checkJointErrors (List<JointCoordinateHandle> handles) {
      LinkedHashSet<JointBase> joints = new LinkedHashSet<>();
      for (JointCoordinateHandle handle : handles) {
         joints.add (handle.getJoint());
      }
      double maxErr = 0;
      for (JointBase joint : joints) {
         Twist TCerr = joint.getCurrentTCError();
         if (TCerr.norm() > maxErr) {
            maxErr = TCerr.norm();
         }
      }
      if (maxErr > 1e-4) {
         showError ("max TC error is " + maxErr);
      }
   }

   public void testThreeBar() {
      MechModel mech = createThreeBarModel(MIN_THETA, MAX_THETA);
      CoordinateSetter csetter = new CoordinateSetter (mech);

      ArrayList<JointCoordinateHandle> handles = getCoordHandles (mech);
      int numc = handles.size();
      VectorNd coordsDeg = getCoordsDeg (handles);
      checkLinkage (mech, coordsDeg, new boolean[numc]);

      int ntests = 100;
      solveCnt = 0;

      NumericState state0 = new NumericState();
      mech.getState (state0);
      for (int i=0; i<ntests; i++) {
         int cidx = 0;
         for (JointCoordinateHandle handle : handles) {
            //csetter.debug = (i==0 && getCoordName(handle).equals("theta1"));
            SetStatus status = testSingleCoord (i, csetter, handles, cidx);
            checkThreeBar (status, handles);
            mech.setState (state0);
         }
      }
      for (int i=0; i<ntests; i++) {
         //csetter.debug = (i==8);
         SetStatus status = testMultiCoord (i, csetter, handles, null);
         checkThreeBar (status, handles);
         mech.setState (state0);
      }
      if (printIterations) {
         System.out.println (
            "avg iterations: " + csetter.numIterations()/(double)solveCnt);
      }
   }

   public void testThreeBarRestore() {

      MechModel mech = createThreeBarModel (10, 170);
      CoordinateSetter csetter = new CoordinateSetter (mech);
      ArrayList<JointCoordinateHandle> handles = getCoordHandles (mech);      
      int numc = handles.size();

      VectorNd coords0Deg = getCoordsDeg (handles);
      coords0Deg.set (0, -0.2);
      coords0Deg.set (2, -0.2);
      coords0Deg.set (4, -0.2);

      int ntests = 200;
      solveCnt = 0;

      for (int i=0; i<ntests; i++) {
         SetStatus status = testMultiCoord (i, csetter, handles, null);
         checkThreeBar (status, handles);
         status = testMultiRestore (i, csetter, handles, coords0Deg, null);
         checkThreeBar (status, handles);
      }
      if (printIterations) {
         System.out.println (
            "avg iterations: " + csetter.numIterations()/(double)solveCnt);
      }
   }
   
   public void test() {
      testFourBar();
      testThreeBar();
      testThreeBarRestore();
      testUJFourBar();
      testUJFourBarRestore();
      testMultiLoop();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      CoordinateSetterTest tester = new CoordinateSetterTest();
      tester.runtest();
   }

}
