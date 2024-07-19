package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import artisynth.core.inverse.InverseManager;
import artisynth.core.inverse.InverseManager.ProbeID;
import artisynth.core.inverse.TargetPoint;
import artisynth.core.inverse.TrackingController;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.probes.TracingProbe;
import maspack.interpolation.Interpolation;
import maspack.render.RenderProps;
import maspack.util.PathFinder;

/**
 * Uses a tracking controller to make the tip marker in ToyMuscleArm follow a
 * prescribed trajectory, using the model's muscles as exciters.
 */
public class InverseMuscleArm extends ToyMuscleArm {

   public void build (String[] args) throws IOException {
      super.build(args); // create ToyMuscleArm

      // move the model into a non-singular position so it can track a target
      // trajectory more easily
      myHinge0.setTheta (-20);
      myHinge1.setTheta (38.4);
      myMech.updateWrapSegments(); // update muscle wrapping for new config
      
      // Create a tracking controller
      TrackingController tcon = new TrackingController (myMech, "tcon");
      addController (tcon);
      // For each muscle, reinitialize its rest length for the new
      // configuration and add it to the controller as an exciter
      for (AxialSpring spr : myMech.axialSprings()) {
         spr.setRestLength (spr.getLength());
         tcon.addExciter ((Muscle)spr);
      }
      for (MultiPointSpring spr : myMech.multiPointSprings()) {
         spr.setRestLength (spr.getLength());
         tcon.addExciter ((MultiPointMuscle)spr);
      }

      // Add the tip marker to the controller as a motion target
      TargetPoint target = tcon.addPointTarget (myTipMkr);
      // add an L-2 regularization term to handle exciter redundancy
      tcon.setL2Regularization(/*weight=*/0.1);

      double startTime = 0; // probe start times
      double stopTime = 5;  // probe stop times
      // Specify a target trajectory for the tip marker using an input probe.
      NumericInputProbe targetprobe = new NumericInputProbe (
         target, "position", startTime, stopTime);
      targetprobe.setName ("target positions");
      double x0 = 0;      // initial x coordinate of the marker
      double z0 = 1.1806; // initial z coordinate of the marker
      double xmax = 0.6;  // max x coordinate of the trajectory
      // Trajectory data: five cubically interpolated knot points, running for
      // 5 seconds, giving a closed loop shape:
      targetprobe.addData (new double[] {
         x0,0,z0,  xmax,0,z0-0.2,  x0,0,z0-0.4,  -xmax,0,z0-0.2,  x0,0,z0},
         /*timestep=*/stopTime/4);
      targetprobe.setInterpolationOrder (Interpolation.Order.Cubic);
      addInputProbe (targetprobe);

      // add an output probe to record the excitations:
      NumericOutputProbe exprobe = InverseManager.createOutputProbe (
         tcon, ProbeID.COMPUTED_EXCITATIONS, /*fileName=*/null,
         startTime, stopTime, /*interval=*/-1);
      addOutputProbe (exprobe);

      // add tracing probes to view both the tracking target (in cyan) and the
      // actual tracked position (in red).
      TracingProbe tprobe;
      tprobe = addTracingProbe (target, "position", startTime, stopTime);
      tprobe.setName ("target tracing");
      RenderProps.setLineColor (tprobe, Color.CYAN);
      tprobe = addTracingProbe (myTipMkr, "position", startTime, stopTime);
      tprobe.setName ("source tracing");
      RenderProps.setLineColor (tprobe, Color.RED);

      // add inverse control panel
      InverseManager.addInversePanel (this, tcon);
      // settings to allow probe management by InvereManager:
      tcon.setProbeDuration (stopTime); // default probe duration
      // set working folder for probe files
      ArtisynthPath.setWorkingFolder (
         new File (PathFinder.getSourceRelativePath (this, "inverseMuscleArm")));
   }
}
