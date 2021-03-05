package artisynth.demos.mech;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.GimbalJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

public class GimbalJointDemo extends RootModel {

   public void build (String[] args) throws IOException {

      // create mech model and set rigid body damping parameters
      MechModel mech = new MechModel ("mech");
      mech.setFrameDamping (1.0);
      mech.setRotaryDamping (0.1);
      addModel (mech);

      double size = 0.5; // size parameter

      // create base - a cylinder rounded at the top
      PolygonalMesh mesh = MeshFactory.createRoundedCylinder (
         size/5, size, /*nslices=*/30, /*negs=*/1, /*flatBottom=*/true);
      RigidBody base = RigidBody.createFromMesh (
         "base", mesh, /*density=*/1000.0, /*scale=*/1.0);
      base.setDynamic (false);
      mech.addRigidBody (base);

      // create tip - a flat ellipsoid
      RigidBody tip = RigidBody.createEllipsoid (
         "tip", size/10, size/3, size/2, /*density=*/1000.0, /*nslices=*/30);
      tip.setPose (new RigidTransform3d (0, 0, 1.25*size));
      mech.addRigidBody (tip);

      // Add a gimbal joint between the tip and the base, with frames C and D
      // initially coincident. Frame D is set (in world coordinates) with its
      // origin at (0, 0, 0.75*size) and its z axis lying along world y. This
      // helps accomodate the singularity at pitch = +/-PI/2.
      RigidTransform3d TDW =
         new RigidTransform3d (0, 0, 0.75*size, 0, 0, -Math.PI/2);
      GimbalJoint joint = new GimbalJoint (tip, base, TDW);
      // set coordinate ranges (in degrees)
      joint.setRollRange (-90, 90);
      joint.setPitchRange (-45, 45);
      joint.setYawRange (-90, 90);
      // set joint coordinates so the tip will fall under gravity
      joint.setRoll (30);
      joint.setYaw (5);
      mech.addBodyConnector (joint);

      // set rendering properties
      joint.setAxisLength (0.75*size); // draw frame C
      joint.setDrawFrameC (Frame.AxisDrawStyle.ARROW);
      joint.setJointRadius (0.10*size); // draw ball around the joint
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (tip, new Color (0.5f, 1f, 1f));

      // create control panel to interactively adjust properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (joint, "roll");
      panel.addWidget (joint, "pitch");
      panel.addWidget (joint, "yaw");
      panel.addWidget (joint, "rollRange");
      panel.addWidget (joint, "pitchRange");
      panel.addWidget (joint, "yawRange");
      panel.addWidget (joint, "drawFrameC");
      panel.addWidget (joint, "drawFrameD");
      panel.addWidget (joint, "axisLength");
      panel.addWidget (joint, "jointRadius");
      panel.addWidget (joint, "linearCompliance");
      panel.addWidget (joint, "rotaryCompliance");
      panel.addWidget (joint, "compliance");
      panel.addWidget (joint, "damping");
      addControlPanel (panel);
   }
}
