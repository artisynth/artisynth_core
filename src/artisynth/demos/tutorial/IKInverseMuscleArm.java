package artisynth.demos.tutorial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import artisynth.core.inverse.InverseManager;
import artisynth.core.inverse.InverseManager.ProbeID;
import artisynth.core.inverse.MotionTargetTerm;
import artisynth.core.inverse.TrackingController;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Point;
import artisynth.core.probes.IKSolver;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.probes.PositionInputProbe;
import artisynth.core.probes.PositionOutputProbe;
import artisynth.core.probes.TRCReader;
import artisynth.core.probes.VelocityInputProbe;
import maspack.render.RenderProps;

/**
 * Uses a tracking controller to follow several markers in ToyMuscleArm, using
 * data read from a TRC file, an IKSolver to transform the data into a feasible
 * trajectory, and target velocity information combined with PD tracking
 * control to reduce lag.
 */
public class IKInverseMuscleArm extends ToyMuscleArm {

   double startTime = 0; // probe start times
   double stopTime = 4;  // probe stop times

   public void build (String[] args) throws IOException {
      super.build(args); // create ToyMuscleArm

      // create a tracking controller
      TrackingController tcon = new TrackingController (myMech, "tcon");
      addController (tcon);
      // for each muscle, reinitialize its rest length for the new
      // configuration and add it to the controller as an exciter
      for (AxialSpring spr : myMech.axialSprings()) {
         spr.setRestLength (spr.getLength());
         tcon.addExciter ((Muscle)spr);
      }
      for (MultiPointSpring spr : myMech.multiPointSprings()) {
         spr.setRestLength (spr.getLength());
         tcon.addExciter ((MultiPointMuscle)spr);
      }
      // set the base to be ground - useful when using the IK solver
      myMech.rigidBodies().get("base").setGrounded (true);

      // apply inverse tracking to markers with the following numbers:
      int[] mkrNums = new int[] { 8, 0, 2 };
      // use the numbers to build a list of the markers, and also a list of
      // their labels in the TRC file
      ArrayList<FrameMarker> markers = new ArrayList<>();
      ArrayList<String> mkrLabels = new ArrayList<>();
      for (int num : mkrNums) {
         markers.add (myMech.frameMarkers().getByNumber (num));
         mkrLabels.add ("mkr"+num);
      }

      // add markers to the controller as motion targets
      for (FrameMarker mkr : markers) {
         tcon.addPointTarget (mkr);
      }
      // add an L-2 regularization term to handle exciter redundancy
      tcon.setL2Regularization(/*weight=*/0.1);

      // create a probe from TRC data to drive the points
      File trcFile = new File(getSourceRelativePath("data/offMarkers.trc"));
      PositionInputProbe targetPos =
          TRCReader.createInputProbeFromLabels (
             "target positions", tcon.getTargetPoints(),
             mkrLabels, trcFile, /*targetProps*/false);
      addInputProbe (targetPos);

      // create an IKSolver and use this to create a feasible target probe
      IKSolver iksolver = new IKSolver (myMech, markers);
      PositionInputProbe feasible = iksolver.createMarkerPositionProbe (
         /*name*/null, targetPos, /*useTargetProps*/false, 0.01);
      // replace data in target probe with feasible data
      targetPos.setValues (feasible, /*useAbsoluteTime*/false);

      // create a velocity input probe to help with tracking
      VelocityInputProbe targetVel = 
         VelocityInputProbe.createNumeric (
            "target velocities", targetPos, /*useTargetProps*/false, 0.01);
      addInputProbe (targetVel);         

      // set tracking controler for PD control so it can use velocity data
      MotionTargetTerm mterm = tcon.getMotionTargetTerm();
      mterm.setUsePDControl (true);
      mterm.setKd (10);
      mterm.setKp (1000);

      // add an output probe to record the excitations
      NumericOutputProbe exprobe = InverseManager.createOutputProbe (
         tcon, ProbeID.COMPUTED_EXCITATIONS, /*fileName=*/null,
         startTime, stopTime, /*interval=*/-1);
      addOutputProbe (exprobe);

      // to see lag when velocity probe is inactive, add a probe to record the
      // target and actual position of the distal marker
      List<Point> pnts = Arrays.asList (
         tcon.getTargetPoints().get(0), markers.get(0));
      PositionOutputProbe tracking = new PositionOutputProbe (
         "distal tracking", pnts, /*rotRep*/null, startTime, stopTime);
      addOutputProbe (tracking);

      // add inverse control panel
      InverseManager.addInversePanel (this, tcon);

      // set point render radius for controller so target points not too large
      RenderProps.setPointRadius (tcon, 0.02);
   }
}
