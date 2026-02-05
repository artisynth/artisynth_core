package artisynth.demos.opensim;

import java.io.File;
import java.io.IOException;
import java.util.*;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.*;
import artisynth.core.opensim.OpenSimParser;
import artisynth.core.workspace.RootModel;
import artisynth.core.inverse.*;
import artisynth.core.inverse.InverseManager.*;
import artisynth.core.probes.*;
import maspack.matrix.AxisAlignedRotation;
import maspack.render.*;

/**
 * Adds marker tracking to OpenSimArm26.
 */
public class InverseArm26 extends OpenSimArm26 {

   public void build (String[] args) throws IOException {
      super.build (args);

      TrackingController tcon = new TrackingController(myMech);
      tcon.setL2Regularization (0.01);
      for (MuscleComponent m : myParser.getMuscles()) {
         tcon.addExciter (m);
      }
      String[] markerNames =
         new String[] { "r_radius_styloid", "r_humerus_epicondyle" };
      ArrayList<Marker> markers = new ArrayList<>();
      for (String name : markerNames) {
         Marker mkr = myParser.findMarker (name);
         markers.add (mkr);
         tcon.addPointTarget (mkr);
      }

      String trcFilePath = getSourceRelativePath ("inputs/arm26_iktargets.trc");
      PositionInputProbe tprobe =
         TRCReader.createInputProbeUsingLabels (
            "marker targets", tcon.getTargetPoints(), Arrays.asList(markerNames),
            new File (trcFilePath), /*useTargetProps*/false);
      tprobe.scaleNumericList (0.001);
      addInputProbe (tprobe);
      addOutputProbe (
         InverseManager.createOutputProbe (
            tcon, ProbeID.COMPUTED_EXCITATIONS,
            null, 0, tprobe.getStopTime(), -1));
                         
      RenderProps.setPointRadius (this, 0.01);
      addController (tcon);
      InverseManager.addInversePanel (this, tcon);
   }
}
