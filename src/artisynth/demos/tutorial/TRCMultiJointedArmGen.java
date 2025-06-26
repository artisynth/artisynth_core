package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidCylinder;
import artisynth.core.mechmodels.Wrappable;
import artisynth.core.workspace.RootModel;
import artisynth.core.probes.*;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.util.PathFinder;

/**
 * Generate marker data for the TRCMultiJointedArm demo.
 */
public class TRCMultiJointedArmGen extends MultiJointedArm {
   protected String geodir = PathFinder.getSourceRelativePath (this, "data/");

   public double startTime = 0;
   public double stopTime = 3;


   class WriteTRC extends MonitorBase {
      public void apply (double t0, double t1) {
         if (t0 == stopTime) {
            File file;
            PositionOutputProbe probe;

            try {
               file = new File(getSourceRelativePath("data/multiJointMkrs.trc"));
               probe = (PositionOutputProbe)getOutputProbes().get ("markers");
               TRCWriter.write (file, probe, /*labels*/null);
            }
            catch (Exception e) {
               e.printStackTrace(); 
            }
         }
      }      
   }


   public void build (String[] args) throws IOException {
      super.build (args);

      // Create a probe to control the two joint angles for moving the arm and
      // creating an initial trajectory to track
      System.out.println ("myMech=" + myMech);
      NumericInputProbe angProbe = new NumericInputProbe (
         myMech.bodyConnectors(),
         new String[] {"0:roll", "0:pitch", "1:theta"},
         startTime, stopTime);
      angProbe.setName ("angle inputs");
      angProbe.addData (
         new double[] {
            0.75,  0,  -45, -45,
            2.25, 180, -45, -45,
            3.00, 180,  45,  90,
         },
         NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (angProbe);
      angProbe.setActive (true);

      for (RigidBody body : myMech.rigidBodies()) {
         body.setDynamic (false);
      }
      
      // Create a probe to track the positions of the markers.
      PositionOutputProbe mkrOuts = new PositionOutputProbe (
         "markers", (Collection)myMech.frameMarkers(),
         /*rotrep*/null, /*fileName*/null, startTime, stopTime, 0.02);
      addOutputProbe (mkrOuts);

      addMonitor (new WriteTRC());
   }
}
