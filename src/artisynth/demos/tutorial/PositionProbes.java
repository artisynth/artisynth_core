package artisynth.demos.tutorial;

import java.io.IOException;
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
import artisynth.core.probes.TracingProbe;
import artisynth.core.workspace.RootModel;
import maspack.interpolation.Interpolation;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationRep;
import maspack.geometry.PolygonalMesh;
import maspack.render.RenderProps;

/**
 * Simple demo that illustrates the use of position and velocity probes.
 */
public class PositionProbes extends RootModel {
   private static final double PI = Math.PI;  // simplify the code
   private double startTime = 0;           // probe start times
   private double stopTime = 2;            // probe stop times
   private boolean useTargetProps = true;  // bind input probes to target props

   public void build (String[] args) throws IOException {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // Create a rigid body and a point to control the positions of.
      PolygonalMesh mesh = new PolygonalMesh (
         getSourceRelativePath ("data/BlenderMonkey.obj"));
      RigidBody monkey = RigidBody.createFromMesh (
         "monkey", mesh, /*density*/1000.0, /*scale*/1);
      monkey.setDynamic (false);
      mech.addRigidBody (monkey);
      Point point = new Point ("point", new Point3d(0, 0, 3));
      mech.addPoint (point);

      // Make a list of these components for creating the probes.
      ArrayList<ModelComponent> comps = new ArrayList<>();
      comps.add (point);
      comps.add (monkey);

      // Create a PositionInputProbe to control the component positions. Since
      // the RotationRep will be ZYX, body poses are specified using 3 position
      // values and 3 ZYX angles in radians
      PositionInputProbe pip = new PositionInputProbe (
         "target positions", comps, RotationRep.ZYX, 
         useTargetProps, startTime, stopTime);
      
      pip.setData (new double[] {
      /*  time  point pos       monkey pos & rotation */
          0.0,    0, 0, 3.0,    0.0, 0.0, 0.0,  0, 0, 0,
          0.5, -1.5, 0, 1.5,    1.5, 0.0, 1.5,  0, PI/2, 0,
          1.0,    0, 0, 0,      0.0, 0.0, 3.0,  0, PI, 0,
          1.5,  1.5, 0, 1.5,   -1.5, 0.0, 1.5,  0, 3*PI/2, 0,
          2.0,    0, 0, 3.0,    0.0, 0.0, 0.0,  0, 0, 0,
         }, NumericProbeBase.EXPLICIT_TIME);         
      // since the knot points are sparse, use cubic interpolation to get a
      // smoother motion
      pip.setInterpolationOrder (Interpolation.Order.Cubic);
      addInputProbe (pip);

      // Create a VelocityInputProbe by differentiating the position probe.
      VelocityInputProbe vip = VelocityInputProbe.createInterpolated (
         "target velocities", pip, useTargetProps, 0.02);
      addInputProbe (vip);

      // Create a PositionOutputProbe to record the component positions
      PositionOutputProbe pop = new PositionOutputProbe (
         "tracked positions", comps, RotationRep.ZYX, startTime, stopTime);
      addOutputProbe (pop);

      // Create a VelocityOutputProbe to record the component velocities
      VelocityOutputProbe vop = new VelocityOutputProbe (
         "tracked velocities", comps, startTime, stopTime);
      addOutputProbe (vop);

      // add a tracing probe to view the path of the point in CYAN
      TracingProbe tprobe =
         addTracingProbe (point, "position", startTime, stopTime);
      tprobe.setName ("point tracing");
      RenderProps.setLineColor (tprobe, Color.CYAN);

      // render properties:
      // draw point as a large red sphere; set color for the monkey
      RenderProps.setSphericalPoints (mech, 0.2, Color.RED);
      // set color for monkey
      RenderProps.setFaceColor (mech, new Color(1f, 1f, 0.6f));
   }

}
