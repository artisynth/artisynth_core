package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.*;
import artisynth.core.probes.IKProbe;
import artisynth.core.probes.IKSolver;
import artisynth.core.probes.*;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.PositionInputProbe;
import artisynth.core.probes.PositionOutputProbe;
import maspack.interpolation.Interpolation.Order;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;

/**
 * Creates the TRC data used to drive the IKInverseMuscleArm demo.
 */
public class IKInverseMuscleArmGen extends ToyMuscleArm {

   public final double startTime = 0;
   public final double stopTime = 4;

   String regTRCName = "data/regMarkers.trc";
   String offTRCName = "data/offMarkers.trc";

   List<String> regMkrNames = Arrays.asList (
      "mkr8", "mkr4", "mkr6", "mkr5", "mkr7", "mkr0", "mkr2");

   class WriteTRC extends MonitorBase {
      public void apply (double t0, double t1) {
         if (t0 == stopTime) {
            File file;
            PositionOutputProbe probe;

            try {
               file = new File(getSourceRelativePath(regTRCName));
               probe = (PositionOutputProbe)getOutputProbes().get ("reg markers");
               TRCWriter.write (file, probe, /*labels*/null);
               
               file = new File(getSourceRelativePath(offTRCName));
               probe = (PositionOutputProbe)getOutputProbes().get ("off markers");
               TRCWriter.write (file, probe, /*labels*/regMkrNames);
            }
            catch (Exception e) {
               e.printStackTrace(); 
            }
         }
      }      
   }

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

      ArrayList<FrameMarker> regMkrs = new ArrayList<>();
      for (String name : regMkrNames) {
         regMkrs.add (myMech.frameMarkers().get (name));
      }

      // add another set of frame markers, whose locations are offset by noise
      Point3d[] offLocs = new Point3d[] {
         new Point3d(0.00367129, 0.00450476, 0.375414),
         new Point3d(-0.049562, -0.0182569, 0.262195),
         new Point3d(0.0555504, -0.0220182, 0.2983),
         new Point3d(-0.070743, 0.0181577, 0.246405),
         new Point3d(0.111562, 0.0236301, 0.232052),
         new Point3d(-0.129425, 0.015168, 0.0500916),
         new Point3d(0.10764, -0.00609043, 0.0632107)
      };
      ArrayList<FrameMarker> offMkrs = new ArrayList<>();
      int i=0;
      for (FrameMarker mkr : regMkrs) {
         FrameMarker omkr = myMech.addFrameMarker (mkr.getFrame(), offLocs[i++]);
         RenderProps.setPointColor (omkr, Color.MAGENTA.darker());
         offMkrs.add (omkr);
      }

      // Create a probe to control the two joint angles for moving the arm and
      // creating an initial trajectory to track
      NumericInputProbe angProbe = new NumericInputProbe (
         myMech.bodyConnectors(),
         new String[] {"0:theta","1:theta"},
         startTime, stopTime);
      angProbe.setName ("angle inputs");
      angProbe.setData (
         new double[] {
            0, 0, 0,
            1.0, 0, -45,
            2, 0, 45,
            3, 45, -45,
            4, -45, 45,
         },
         NumericInputProbe.EXPLICIT_TIME);
      angProbe.setInterpolationOrder (Order.CubicStep);
      addInputProbe (angProbe);
      angProbe.setActive (true);

      // Create a probes to track the positions of the regular and offset
      // markers.
      PositionOutputProbe regOuts = new PositionOutputProbe (
         "reg markers", regMkrs,
         /*rotrep*/null, /*fileName*/null, startTime, stopTime, 0.02);
      addOutputProbe (regOuts);

      PositionOutputProbe offOuts = new PositionOutputProbe (
         "off markers", offMkrs, /*rotrep*/null, /*fileName*/null,
         startTime, stopTime, 0.02);
      addOutputProbe (offOuts);

      // make bodies non-dynamic for joint control:
      for (RigidBody body : myMech.rigidBodies()) {
         body.setDynamic (false);
      }

      addMonitor (new WriteTRC());
   }

   
}
     
