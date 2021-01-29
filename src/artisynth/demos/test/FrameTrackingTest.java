package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class FrameTrackingTest extends RootModel {

   public static boolean omitFromMenu = true;

   public void build (String[] args) throws IOException {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      RigidBody box = RigidBody.createBox ("frame", 0.05, 0.05, 0.05, 1.0);
      box.setAxisLength (0.2);
      box.setPose (new RigidTransform3d (
                      /*xyz=*/ -0.0199977, 0.926652, -0.00174013,
                      /*rpy=*/-0.551998, 0.030926, -0.040558));
      box.setDynamic (false);
      mech.addRigidBody (box);

      // set bounds so that the whole range of motion is in view
      mech.setBounds (
         new Point3d (0.019, 0.56, -0.00),
         new Point3d (1.0, 0.92, 0.09));
      

      mech.addFrameMarker (
         box, new Point3d (0.0339921, -0.000938364, 0.133713));
      mech.addFrameMarker (
         box, new Point3d (-0.207687, 0.0207198, 0.053254));
      mech.addFrameMarker (
         box, new Point3d (0.0359722, 0.00915471, -0.121965));
      mech.addFrameMarker (
         box, new Point3d (-0.202932, 0.0323304, -0.0519688));

      String trackFile = PathFinder.getSourceRelativePath (
         this, "data/target_marker_positions.txt");

      // add tracking probe
      FrameTrackingProbe probe = new FrameTrackingProbe (
         box, mech.frameMarkers());
      probe.setData (trackFile);
      addInputProbe (probe);

      NumericOutputProbe oprobe =
         new NumericOutputProbe (
            box, "position", 0, 1.31, -1);
      addOutputProbe (oprobe);

      RenderProps.setSphericalPoints (mech, 0.007, Color.WHITE);
   }

}
