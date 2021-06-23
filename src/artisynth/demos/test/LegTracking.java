package artisynth.demos.test;

import java.awt.Color;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.RotAxisFrameMaterial;
import artisynth.core.util.*;
import artisynth.core.gui.*;
import artisynth.core.driver.Main;
import artisynth.core.workspace.*;
import artisynth.demos.mech.LaymanDemo;
import maspack.geometry.*;
import maspack.properties.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer.LineStyle;
import maspack.util.*;
import maspack.numerics.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class LegTracking extends RootModel {
   
   private File mySrcDir = new File(PathFinder.getSourceRelativePath(this, ""));

   private static double DTOR = Math.PI / 180.0;

   // parameters used for constructing the leg model
   private static double LEG_SEP = 0.2;
   private static double INF = Double.POSITIVE_INFINITY;
   private static double UPPER_LEG_Z = 1-.85;
   private static double FOOT_Y = -0.05;
   private static double K_ROT = 0.1;

   // leg model bodies
   RigidBody myLUppLeg;
   RigidBody myLLowLeg;
   RigidBody myLFoot;

   // marker sets
   ArrayList<MarkerSet> myMarkerSets = new ArrayList<MarkerSet>();

   /**
    * Marker set, consisting of a rigid body, a set of frame markers, and a
    * reference to the underlying MechModel.
    */
   private class MarkerSet {
      MechModel myMech; // the underlying mechmodel
      RigidBody myBody; // rigid body 
      ArrayList<FrameMarker> myMarkers; // body markers
      
      ArrayList<Point> myTargets; // marker targets (when attached)
      ArrayList<AxialSpring> mySprings; // marker-target springs (when attached)

      /**
       * Initialize a marker set for a specific rigid body.
       */
      MarkerSet (MechModel mech, RigidBody body) {
         myMech = mech;
         myBody = body;
         myMarkers = new ArrayList<>();
         myTargets = new ArrayList<>();
         mySprings = new ArrayList<>();
      }

      /**
       * Initialize a marker set for a rigid body with a given name.
       */
      MarkerSet (MechModel mech, String bodyName) {
         this (mech, mech.rigidBodies().get(bodyName));
         myMarkers = new ArrayList<>();
         myTargets = new ArrayList<>();
         mySprings = new ArrayList<>();
      }

      /**
       * Add a marker to this marker set at the specified location
       * {@code lx, ly, lz} (in body coordinates).
       */
      FrameMarker addMarker (double lx, double ly, double lz) {
         Point3d loc = new Point3d(lx, ly, lz);
         FrameMarker mkr = myMech.addFrameMarker (myBody, loc);
         myMarkers.add (mkr);
         return mkr;
      }

      /**
       * Attach target points and springs to this marker set.  The springs have
       * a stiffness given by {@code k}, and the targets and springs are added
       * to the mechmodel. Damping for the rigid body is also set to 0.
       */
      void attachTargets (double stiffness) {
         for (int i=0; i<myMarkers.size(); i++) {
            FrameMarker mkr = myMarkers.get(i);
            Point targ = new Point (mkr.getPosition());
            AxialSpring spr = new AxialSpring (stiffness, 0, 0);
            myMech.attachAxialSpring (targ, mkr, spr);
            myMech.addPoint (targ);
            myTargets.add (targ);
            mySprings.add (spr);
         }
         myBody.setFrameDamping (0);
         myBody.setRotaryDamping (0);
      }

      /**
       * Removed target points and springs from this marker set and the
       * underlying mechmodel. Damping for the rigid body is also restored to
       * whatever value is inherited from the mechmodel.
       */
      void detachTargets() {
         for (int i=0; i<myMarkers.size(); i++) {
            myMech.removeAxialSpring (mySprings.get(i));
            myMech.removePoint (myTargets.get(i));
         }
         myTargets.clear();
         mySprings.clear();
         myBody.setFrameDampingMode (PropertyMode.Inherited);
         myBody.setRotaryDampingMode (PropertyMode.Inherited);
      }
   }         

   /**
    * Tracker that causes a set of rigid bodies and markers, as defined by a
    * collection of MarkerSets, to follow track a set of marker target
    * positions read from a file.
    */
   class Tracker extends ControllerBase {

      ArrayList<Point> myAllTargets; // all targets across all bodies
      ArrayList<Point3d> myData; // all target data read from the file
      ArrayList<Vector3d> myNoise; // optional noise vector to add to the data

      double myStop; // tracker stop time
      int myDataIdx = 0; // index into the data, updated each time step

      /**
       * Read tracking data from a specified file
       */
      public void readData (File file) throws IOException {
         myData = new ArrayList<>();
         ReaderTokenizer rtok = ArtisynthIO.newReaderTokenizer (file);
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            rtok.pushBack();
            Point3d p = new Point3d();
            p.x = rtok.scanNumber();
            p.y = rtok.scanNumber();
            p.z = rtok.scanNumber();
            myData.add (p);
         }
         
      }

      /**
       * Attach target points and springs to all the marker sets
       * in preparation for tracking.
       *
       * @param stiffness linear spring stiffness
       */
      public void attachTargets (double stiffness) {
         for (MarkerSet set : myMarkerSets) {
            set.attachTargets (stiffness);
         }
      }

      /**
       * Remove target points and springs from all the marker sets.
       */
      public void detachTargets () {
         for (MarkerSet set : myMarkerSets) {
            set.detachTargets();
         }
      }

      /**
       * Set rigid body positions to the best pose for the current
       * target positions.
       */
      public void setBodyPosesFromTargets () {
         for (MarkerSet set : myMarkerSets) {
            ArrayList<Point3d> tpnts = new ArrayList<Point3d>();
            ArrayList<Point3d> locs = new ArrayList<Point3d>();
            for (int i=0; i<set.myMarkers.size(); i++) {
               tpnts.add (new Point3d(set.myTargets.get(i).getPosition()));
               locs.add (new Point3d(set.myMarkers.get(i).getLocation()));
            }
            RigidPoseEstimator estimator = new RigidPoseEstimator (locs);
            RigidTransform3d TBW = new RigidTransform3d();
            estimator.estimatePose (TBW, tpnts);
            set.myBody.setPose (TBW);
         }
      }

      /**
       * Create a tracker for the specified collection of marker sets.
       * If {@code noise} is not 0, creates noise to add to the tracking
       * data.
       */
      public Tracker (Collection<MarkerSet> markerSets, double noise) {
         myAllTargets = new ArrayList<>();
         myNoise = new ArrayList<>();
         for (MarkerSet set : markerSets) {
            myAllTargets.addAll (set.myTargets);
         }
         // initialize noise vector
         if (noise != 0) {
            for (int i=0; i<myAllTargets.size(); i++) {
               Vector3d noiseVec = new Vector3d();
               noiseVec.setRandom (-noise, noise);
               myNoise.add (noiseVec);
            }
         }
      }

      /**
       * Tracking function. At the beginning of the time step between {@code
       * t0} and {@code t1}, sets the target positions to the data pointed to
       * be {@code myDataIdx}, and then increment {@code myDataIdx}.
       */
      public void apply (double t0, double t1) {
         if (t0 == 0) {
            myDataIdx = 0;
         }
         Point3d pos = new Point3d();
         if (myDataIdx <= myData.size()-myAllTargets.size()) {
            for (int k=0; k<myAllTargets.size(); k++) {
               pos.add (myData.get(myDataIdx+k), myNoise.get(k));
               myAllTargets.get(k).setPosition (pos);
            }
            myDataIdx += myAllTargets.size();
         }
         if (t0 == 0) {
            setBodyPosesFromTargets();
         }
      }
   }

   /**
    * Monitor to record the marker positions defined by a collection of
    * MarkerSets.
    */
   class MarkerMonitor extends MonitorBase {

      ArrayList<FrameMarker> myMarkers;
      double myStop;
      PrintWriter myWriter;

      /**
       * Create a MarkerMonitor for a collection of marker sets, to record
       * marker data to the specified file from time 0 to {@code stop}.
       */
      MarkerMonitor (
         ArrayList<MarkerSet> markerSets, double stop, File file)
         throws IOException {
         myMarkers = new ArrayList<>();
         for (MarkerSet set : markerSets) {
            myMarkers.addAll (set.myMarkers);
         }
         myStop = stop;
         myWriter = ArtisynthIO.newIndentingPrintWriter (file);
      }

      /**
       * Writes out the marker positions for the time step between {@code t0}
       * and {@code t1}.
       */
      public void apply (double t0, double t1) {
         if (t1 <= myStop) {
            for (FrameMarker pnt : myMarkers) {
               myWriter.println (pnt.getPosition().toString ("%10.6f"));
            }
            myWriter.println ("");
         }
         if (t1 == myStop) {
            myWriter.close();
         }
      }
   }

   // --- begin methods for building the leg model ---

   private double getHeight (RigidBody body) {
      Point3d min = new Point3d (INF, INF, INF);
      Point3d max = new Point3d (-INF, -INF, -INF);

      body.updateBounds (min, max);
      return max.z - min.z;
   }

   private Point3d getCenter (RigidBody body) {
      Point3d min = new Point3d (INF, INF, INF);
      Point3d max = new Point3d (-INF, -INF, -INF);

      body.updateBounds (min, max);
      Point3d center = new Point3d();
      center.add (min, max);
      center.scale (0.5);
      return center;
   }

   private void addFrameSpring (
      MechModel mech, RigidBody bodyA, RigidBody bodyB,
      double x, double y, double z, double kRot) {
      RigidTransform3d TDW = new RigidTransform3d();
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      TDW.p.set (x, y, z);

      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      XDB.mulInverseLeft (bodyB.getPose(), TDW);

      FrameSpring spring = new FrameSpring (null);
      spring.setMaterial (new RotAxisFrameMaterial (0, kRot, 0, 0));
      spring.setAttachFrameA (TCA);
      spring.setAttachFrameB (XDB);
      mech.attachFrameSpring (bodyA, bodyB, spring);
   }

   private RigidBody addBody (MechModel mech, String bodyName, String meshName) {
      RigidBody body = RigidBody.createFromMesh (
         bodyName, LaymanDemo.class, "geometry/"+meshName+".obj", 1000, 0.1);
      mech.addRigidBody (body);
      return body;
   }

   private GimbalJoint addGimbalJoint (
      MechModel mech, RigidBody bodyA, RigidBody bodyB,
      double x, double y, double z, double maxAng) {
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (x, y, z);

      return addGimbalJoint (mech, bodyA, bodyB, TDW, maxAng);
   }

   private GimbalJoint addGimbalJoint (
      MechModel mech, RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW,
      double maxAng) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();

      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      GimbalJoint joint = new GimbalJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setJointRadius (0.035);
      mech.addBodyConnector (joint);
      return joint;
   }

   private HingeJoint addHingeJoint (
      MechModel mech, RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();

      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      HingeJoint joint = new HingeJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setShaftRadius (0.025);
      joint.setShaftLength (0.05);
      mech.addBodyConnector (joint);
      return joint;
   }

   private HingeJoint addHingeJoint (
      MechModel mech, RigidBody bodyA, RigidBody bodyB,
      double x, double y, double z) {
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (x, y, z);
      TDW.R.setAxisAngle (Vector3d.Y_UNIT, Math.toRadians (90));
      return addHingeJoint (mech, bodyA, bodyB, TDW);
   }

   // --- end methods for building the leg model ---

   public void build (String[] args) throws IOException {
      
      // set random number generator seed to ensure repeatable results
      RandomGenerator.setSeed (0x1234);

      // build the leg model

      MechModel mech = new MechModel();
      addModel (mech);
      mech.setFrameDamping (0.50);
      mech.setRotaryDamping (0.01);

      myLUppLeg = addBody (mech, "leftUppLeg", "upperLeg");
      myLLowLeg = addBody (mech, "leftLowLeg", "lowerLeg");
      myLFoot = addBody (mech, "leftFoot", "foot");

      double kneeZ =
         UPPER_LEG_Z + getCenter (myLUppLeg).z - getHeight (myLUppLeg) / 2;
      double ankleZ = kneeZ - getHeight (myLLowLeg);
      double hipZ = kneeZ + getHeight (myLUppLeg);

      double lowerLegZ =
         kneeZ - getCenter (myLLowLeg).z - getHeight (myLLowLeg) / 2;
      double footZ = ankleZ - getCenter (myLFoot).z - getHeight (myLFoot) / 2;

      myLUppLeg.setPose (LEG_SEP / 2, 0, UPPER_LEG_Z, 0, 0, 0);
      myLLowLeg.setPose (LEG_SEP / 2, 0, lowerLegZ, 0, 0, 0);
      myLFoot.setPose (LEG_SEP / 2, FOOT_Y, footZ, 0, 0, 0);

      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (LEG_SEP / 2, 0, hipZ);
      GimbalJoint hipjoint = new GimbalJoint (myLUppLeg, TDW);
      RenderProps.setFaceColor (hipjoint, Color.BLUE);
      hipjoint.setJointRadius (0.035);
      mech.addBodyConnector (hipjoint);

      HingeJoint rjoint = addHingeJoint (
         mech, myLUppLeg, myLLowLeg, LEG_SEP / 2, 0, kneeZ);
      rjoint.setMinTheta (-135);
      rjoint.setMaxTheta (5);

      GimbalJoint ankle = addGimbalJoint (
         mech, myLFoot, myLLowLeg, LEG_SEP / 2, 0, ankleZ, 45);
      addFrameSpring (mech, myLLowLeg, myLFoot, LEG_SEP / 2, 0, ankleZ, K_ROT);

      myLFoot.setRotaryDamping (0.1);

      RenderProps r = new RenderProps();
      r.setFaceColor (new Color (0.8f, 0.8f, 1f));
      r.setPointStyle (Renderer.PointStyle.SPHERE);
      r.setPointColor (new Color (88f / 255f, 106f / 255f, 155f / 255f));
      r.setPointRadius (0.015);
      r.setLineColor (new Color (0, 0.7f, 0));
      r.setLineStyle (LineStyle.CYLINDER);
      r.setLineRadius (0.004);
      mech.setRenderProps (r);    

      setDefaultViewOrientation (AxisAlignedRotation.Y_Z);

      // create the marker sets and then either generate the
      // tracking data, or track the tracking data

      createMarkerSets (mech);
      boolean createTrackingData = true;
      if (createTrackingData) {
         // set the joint angles so that the leg motion will be produced by
         // falling under gravity:
         hipjoint.setYaw (-60);
         rjoint.setTheta (-60);
         // create a marker monitor to record 2 seconds of marker positions to
         // the file "markers.txt":
         addMonitor (
            new MarkerMonitor (
               myMarkerSets, 2.0, new File (mySrcDir, "markers.txt")));
      }
      else {
         // apply tracking data

         mech.setGravity (0, 0, 0); // remove gravity

         // create a tracker, attach target points and springs, and read data from
         // the file "markers.txt"
         Tracker tracker = new Tracker (myMarkerSets, 0.05);
         tracker.attachTargets (1000);
         tracker.readData (new File (mySrcDir, "markers.txt"));
         addController (tracker);
      }

      // set render properties so target points are rendered in red         
      RenderProps.setSphericalPoints (mech.points(), 0.015, Color.RED);
   }

   /**
    * Create marker sets
    */
   void createMarkerSets (MechModel mech) {

      MarkerSet set;
      // marker set for "leftUppLeg"
      set = new MarkerSet(mech, "leftUppLeg");
      set.addMarker (-0.01080,  0.06094, -0.11598);
      set.addMarker (-0.00564,  0.08129,  0.126956);
      set.addMarker ( 0.00678, -0.08230,  0.109111);
      set.addMarker ( 0.01242, -0.06531, -0.080199);
      set.addMarker ( 0.05544,  0.00310,  0.017325);
      set.addMarker (-0.05527, -0.00066,  0.011561);
      myMarkerSets.add (set);

      // marker set for "leftLowLeg"
      set = new MarkerSet(mech, "leftLowLeg");
      set.addMarker ( 0.00654, -0.07013,  0.075088);
      set.addMarker ( 0.01027, -0.05586, -0.066328);
      set.addMarker (-0.00420, 0.06964, 0.046335);
      set.addMarker (-0.00197, 0.05261, -0.102397);
      set.addMarker ( 0.04601, 0.00725, 0.004400);
      set.addMarker (-0.04738, 0.00500, 0.021729);
      myMarkerSets.add (set);

      // marker set for "leftFoot"
      set = new MarkerSet(mech, "leftFoot");
      set.addMarker ( 0.05219, 0.03603, -0.012672);
      set.addMarker (-0.05430, 0.01270, -0.019982);
      set.addMarker (-0.00810, 0.13490, -0.013873);
      set.addMarker (-0.00087, -0.13714, -0.028192);
      myMarkerSets.add (set);
   }

}
