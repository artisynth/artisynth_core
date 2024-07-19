package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.inverse.FrameExciter;
import artisynth.core.inverse.FrameExciter.WrenchDof;
import artisynth.core.inverse.InverseManager;
import artisynth.core.inverse.InverseManager.ProbeID;
import artisynth.core.inverse.TargetPoint;
import artisynth.core.inverse.TrackingController;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.probes.TracingProbe;
import maspack.interpolation.Interpolation;
import maspack.render.RenderProps;

/**
 * Uses a tracking controller to make the tip marker in ToyMuscleArm follow a
 * prescribed trajectory, using translational forces applied to each link.
 */
public class InverseFrameExciterArm extends ToyMuscleArm {

   /**
    * Creates a frame exciter for a rigid body, controlling the force DOF
    * described by 'dof' with a maximum activation for of 'maxf', and adds it
    * to both the mech model and the tracking controller 'tcon'.
    */
   void addFrameExciter (
      TrackingController tcon, RigidBody body, WrenchDof dof, double maxf) {
      FrameExciter fex = new FrameExciter (null, body, dof, maxf);
      myMech.addForceEffector (fex);
      tcon.addExciter (fex);
   }

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
      // configuration
      for (AxialSpring spr : myMech.axialSprings()) {
         spr.setRestLength (spr.getLength());
      }
      for (MultiPointSpring spr : myMech.multiPointSprings()) {
         spr.setRestLength (spr.getLength());
      }
      
      // For each link, add two frame exciters to give the controller access to
      // translational forces in the x-z plane
      addFrameExciter (tcon, myLink0, WrenchDof.FX, 200);
      addFrameExciter (tcon, myLink0, WrenchDof.FZ, 200);
      addFrameExciter (tcon, myLink1, WrenchDof.FX, 200);
      addFrameExciter (tcon, myLink1, WrenchDof.FZ, 200);

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
      // 4 second, giving a closed loop shape:
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
      tprobe.setName ("target position");
      RenderProps.setLineColor (tprobe, Color.CYAN);
      tprobe = addTracingProbe (myTipMkr, "position", startTime, stopTime);
      tprobe.setName ("source position");
      RenderProps.setLineColor (tprobe, Color.RED);
   }
}
