package artisynth.demos.tutorial;

import java.io.*;
import java.util.ArrayList;

import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.probes.IKProbe;
import artisynth.core.probes.IKSolver;
import artisynth.core.probes.*;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.PositionInputProbe;
import artisynth.core.probes.PositionOutputProbe;
import maspack.interpolation.Interpolation.Order;
import maspack.matrix.RotationRep;
import maspack.matrix.VectorNd;

/**
 * Uses inverse kinematics to control ToyMuscleArm, using one or several
 * markers.
 */
public class IKToyMuscleArm extends ToyMuscleArm {

   public void build (String[] args) throws IOException {
      super.build(args); // create ToyMuscleArm

      // Set the base to be ground - useful when setting the link positions
      // using joint angles.
      myMech.rigidBodies().get("base").setGrounded (true);

      // Give the markers names based on their component number. We do this so
      // we can identify them in the TRC file.
      for (FrameMarker mkr : myMech.frameMarkers()) {
         mkr.setName ("mkr" + mkr.getNumber());
      }

      // For inverse kinematics, use either 3 markers on the top link (link1)
      // or 7 markers on both links.

      // topMkrs is a the 3 markers on link1:
      int[] topMkrNums = new int[] { 8, 4, 6 };
      ArrayList<FrameMarker> topMkrs = new ArrayList<>();
      for (int num : topMkrNums) {
         topMkrs.add (myMech.frameMarkers().get (num));
      }
      // allMkrs is 7 markers on both links:
      int[] allMkrNums = new int[] { 8, 4, 6, 5, 7, 0, 2 };
      ArrayList<FrameMarker> allMkrs = new ArrayList<>();
      for (int num : allMkrNums) {
         allMkrs.add (myMech.frameMarkers().get (num));
      }

      // Create a probe to control the two joint angles for moving the arm and
      // creating an initial trajectory to track
      NumericInputProbe angProbe = new NumericInputProbe (
         myMech.bodyConnectors(),
         new String[] {"0:theta","1:theta"},
         0.0, 3.0);
      angProbe.setName ("angle inputs");
      angProbe.setData (
         new double[] {
            0, 0, 0,
            0.5, 0, -45,
            1, 0, 45,
            2, 45, -45,
            3, -45, 45,
         },
         NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (angProbe);
      angProbe.setActive (true);

      // Create a probe to track the positions of all the markers. These
      // positions can then be copied into the inverse kinematics probes.
      PositionOutputProbe mkrOuts = new PositionOutputProbe (
         "marker outputs", allMkrs, /*rotrep*/null, /*fileName*/null, 0, 3, 0.02);
      addOutputProbe (mkrOuts);


      // Create two inverse kinematic probes: one controlled by 3 markers and
      // the other controlled by 7 markers. Both probes are initially disabled.

      // The 3 marker probe is not initialized with data, so its marker
      // positions are simply set to their initial postions. Data can be copied
      // from other marker probes after the program is run
      IKProbe iktop;
      iktop = new IKProbe (
         "invk top markers", myMech, topMkrs, /*wgts*/null, 0.0, 3.0);
      addInputProbe (iktop);
      iktop.setBodiesNonDynamicIfActive (true);
      iktop.setActive (false);

      // The 7 marker probe is initialized with data from "data/ikall.txt".
      // Use the marker weights specified by 'wgts'.
      IKProbe ikall;
      String fileName = getSourceRelativePath ("data/ikall.txt");
      VectorNd wgts = new VectorNd ( 2, 2, 1, 1, 1, 0.5, 1 );
      ikall = new IKProbe (
         "invk all markers", myMech, allMkrs, wgts, fileName);
      addInputProbe (ikall);
      ikall.setBodiesNonDynamicIfActive (true);
      ikall.setActive (false);

      // Create an IKSolver for the 7 marker set, and use it to create a
      // PositionInputProbe controlling the body positions. This is done by
      // doing a stand-alone IK solve using frame marker data contained in the
      // probe data file "data/ikall.txt".
      IKSolver iksolver = new IKSolver (myMech, allMkrs, /*wgts*/null);
      PositionInputProbe posprobe = iksolver.createBodyPoseProbe (
         "ik position inputs", fileName, RotationRep.QUATERNION, 
         /*targetProps*/false, /*interval=*/0.05);
      posprobe.setInterpolationOrder (Order.Cubic);
      posprobe.setActive (false);
      addInputProbe (posprobe);         
      
      // make bodies non-dynamic for joint control:
      for (RigidBody body : myMech.rigidBodies()) {
         body.setDynamic (false);
      }
    }
}
     
