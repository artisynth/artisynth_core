package artisynth.demos.tutorial;

import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.probes.PositionInputProbe;
import artisynth.core.probes.PositionOutputProbe;
import artisynth.core.probes.VelocityInputProbe;
import artisynth.core.probes.VelocityOutputProbe;
import artisynth.core.workspace.RootModel;
import maspack.interpolation.Interpolation;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationRep;
import maspack.render.RenderProps;

/**
 * Simple demo that illustrates the use of position and velocity probes.
 */
public class PositionProbes extends RootModel {

   public static boolean omitFromMenu = true;

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // Create three non-dynamic componentw to control the positions of: two
      // boxes and a point.
      RigidBody box1 =
         RigidBody.createBox ("box1", 2, 1, 0.5, /*density=*/1000);
      box1.setDynamic (false);
      box1.setPose (new RigidTransform3d (1.5, 0, 0));
      mech.addRigidBody (box1);

      RigidBody box2 =
         RigidBody.createBox ("box2", 0.75, 0.75, 0.75, /*density=*/1000);
      box2.setDynamic (false);
      box2.setPose (new RigidTransform3d (-.75, 0, 0));
      mech.addRigidBody (box2);

      Point pnt = new Point ("pnt", new Point3d(0, 0, 1));
      mech.addPoint (pnt);

      // Make a list of these components for creating the probes.
      ArrayList<ModelComponent> comps = new ArrayList<>();
      comps.add (pnt);
      comps.add (box1);
      comps.add (box2);

      // Create a PositionInputProbe to control the component positions.  Body
      // poses are specified using 3 position values and 3 ZYX angles in
      // degrees (since the RotationRep is ZYX_DEG),
      PositionInputProbe pip = new PositionInputProbe (
         "target positions", comps, RotationRep.ZYX_DEG, 0, 2);
      pip.setData (new double[] {
      /*  time  pnt pos     box1 pos/orientation       box2 pos/orientation */
          0.0,  0, 0, 1,    1.5,  0, 0,  0, 0, 0,      -0.75, 0, 0,  0, 0, 0,
          0.5,  1, 0, 1.5,  2.0,  0, .5, 90, 45, 0,    -1.0,  0, -1, 10, 20, 30,
          1.0,  0, 0, 1.5,  1.5,  0, 0,  0,  10, 20,   -0.5,  0, 0,  -40, 0, 45,
          1.5, -1, 0, 2,    0.5,-.5, 0, -90, 45, -30,  -0.75, -.5, 1, 20, -20, 30,
          2.0,  0, 0, 1,    1.5,  0, 0,  0, 0, 0,      -0.75, 0, 0,  0, 0, 0
         }, NumericProbeBase.EXPLICIT_TIME);         
      // use cubic interpolation since the knot points are sparse
      pip.setInterpolationOrder (Interpolation.Order.Cubic);
      addInputProbe (pip);

      // Create a VelocityInputProbe by differentiating the position probe.
      VelocityInputProbe vip = VelocityInputProbe.createInterpolated (
         "target velocities", pip, 0.02);
      addInputProbe (vip);

      // Create a PositionOutputProbe to record the component positions
      PositionOutputProbe pop = new PositionOutputProbe (
         "tracked positions", comps, RotationRep.ZYX_DEG, 0, 2);
      addOutputProbe (pop);

      // Create a VelocityOutputProbe to record the component velocities
      VelocityOutputProbe vop = new VelocityOutputProbe (
         "tracked velocities", comps, 0, 2);
      addOutputProbe (vop);

      RenderProps.setSphericalPoints (mech, 0.05, Color.RED);
   }

}
