package artisynth.demos.tutorial;

import java.io.IOException;

import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.inverse.InverseManager;
import artisynth.core.inverse.InverseManager.ProbeID;
import artisynth.core.inverse.TargetFrame;
import artisynth.core.inverse.TrackingController;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import maspack.util.PathFinder;

/**
 * Uses a tracking controller to make the frame in ToyMuscleFem follow a
 * prescribed trajectory, using the model's muscles as exciters.
 */
public class InverseMuscleFem extends ToyMuscleFem {

   protected String dataDir = PathFinder.getSourceRelativePath (this, "data/");   

   public void build (String[] args) throws IOException {
      super.build (args);  // build the underlying ToyMuscleFem model

      // create a tracking controller
      TrackingController tcon = new TrackingController (myMech, "tcon");
      addController (tcon);
      // add each FEM muscle bundle to it as an exciter
      for (MuscleBundle b : myFem.getMuscleBundles()) {
         tcon.addExciter (b);
      }
      // add the frame attached to the FEM as a motion target
      TargetFrame target = tcon.addFrameTarget (myFrame);

      // add an L-2 regularization term to handle exciter redundancy
      tcon.setL2Regularization(/*weight=*/0.1);
      // set the controller motion term to use PD control
      tcon.getMotionTargetTerm().setUsePDControl (true);
      tcon.getMotionTargetTerm().setKp (1000);
      tcon.getMotionTargetTerm().setKd (100);

      // add input probes specifying the desired position and orientation
      // trajectory of the target:
      double startTime = 0;
      double stopTime = 5;
      NumericInputProbe tprobePos =
         new NumericInputProbe (
            target, "position", dataDir+"inverseFemFramePos.txt");
      tprobePos.setName ("target frame position");
      addInputProbe (tprobePos);
      NumericInputProbe tprobeRot =
         new NumericInputProbe (
            target, "orientation", dataDir+"inverseFemFrameRot.txt");
      tprobeRot.setName ("target frame orientation");
      addInputProbe (tprobeRot);

      // add output probes showing the tracked position and orientation of the
      // target frame source:
      NumericOutputProbe sprobePos =
         new NumericOutputProbe (
            myFrame,"position", startTime, stopTime, /*interval*/-1);
      sprobePos.setName ("source frame position");
      addOutputProbe (sprobePos);
      NumericOutputProbe sprobeRot =
         new NumericOutputProbe (
            myFrame,"orientation", startTime, stopTime, /*interval*/-1);
      sprobeRot.setName ("source frame orientation");
      addOutputProbe (sprobeRot);

      // add an output probe to record the excitations:
      NumericOutputProbe exprobe = InverseManager.createOutputProbe (
         tcon, ProbeID.COMPUTED_EXCITATIONS, /*fileName=*/null,
         startTime, stopTime, /*interval=*/-1);
      addOutputProbe (exprobe);
      // create a control panel for the controller
      InverseManager.addInversePanel (this, tcon);
   }
}
