package artisynth.models.tgcs2025;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import artisynth.core.inverse.InverseManager;
import artisynth.core.inverse.InverseManager.ProbeID;
import artisynth.core.inverse.TrackingController;
import artisynth.core.mechmodels.Marker;
import artisynth.core.mechmodels.MuscleComponent;
import artisynth.core.probes.PositionInputProbe;
import artisynth.core.probes.TRCReader;
import artisynth.core.probes.TracingProbe;
import maspack.render.RenderProps;

/**
 * Adds marker tracking to OpenSimImport.
 */
public class OpenSimTrack extends OpenSimImport {

   public void build (String[] args) throws IOException {
      super.build (args);

      // create a tracking controller and add regularization
      TrackingController tcon = new TrackingController(myMech);
      tcon.setL2Regularization (0.01);
      // add each muscle as a potential exciter
      for (MuscleComponent m : myParser.getMuscles()) {
         tcon.addExciter (m);
      }
      // list of the names of the markers we want to track
      String[] markerNames =
         new String[] { "r_radius_styloid", "r_humerus_epicondyle" };
      // create a list of the markers, and add to the controller as targets
      ArrayList<Marker> markers = new ArrayList<>();
      for (String name : markerNames) {
         Marker mkr = myParser.findMarker (name);
         markers.add (mkr);
         tcon.addPointTarget (mkr);
      }

      // tracking data is stored in a TRC file. Use this data to create an
      // input probe for the marker target positions
      String trcFilePath = getSourceRelativePath ("inputs/arm26_iktargets.trc");
      PositionInputProbe tprobe =
         TRCReader.createInputProbeUsingLabels (
            "marker targets", tcon.getTargetPoints(), Arrays.asList(markerNames),
            new File (trcFilePath), /*useTargetProps*/false);
      tprobe.scaleNumericList (0.001); // scale data from mm to m
      addInputProbe (tprobe);

      // add an output probe to view the generated muscle excitations
      addOutputProbe (
         InverseManager.createOutputProbe (
            tcon, ProbeID.COMPUTED_EXCITATIONS,
            null, 0, tprobe.getStopTime(), -1));
      addController (tcon);

      // add a control panel for the inverse controller
      InverseManager.addInversePanel (this, tcon);

      // add tracing probes to see actual and target marker trajectories
      TracingProbe markerTrace = addTracingProbe (
         markers.get(0), "position", 0.0, 1.0);
      markerTrace.setName ("marker trace");
      RenderProps.setLineColor (markerTrace, Color.ORANGE);

      TracingProbe targetTrace = addTracingProbe (
         tcon.getTargetPoints().get(0), "position", 0.0, 1.0);
      targetTrace.setName ("target trace");
      RenderProps.setLineColor (targetTrace, Color.CYAN);
   }
}
