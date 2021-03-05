package artisynth.demos.mech;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.SliderJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

public class SliderJointDemo extends RootModel {

   public void build (String[] args) throws IOException {

      // create mech model and set rigid body damping parameters
      MechModel mech = new MechModel ("mech");
      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);
      addModel (mech);

      double size = 1.0; // size parameter
      double tilt = Math.toRadians(10); // tilt angle for the slider

      // create base - a vertical cylinder
      RigidBody base = RigidBody.createCylinder (
         "base", 0.125*size, size, /*density=*/1000.0, /*nsides=*/25);
      base.setDynamic (false);
      // create sleave - a horizontal tube
      PolygonalMesh sleaveMesh = MeshFactory.createTube (
         0.1*size, 0.2*size, 0.25*size, 25, 1, 1);
      // transform tube to so it lies on top of the cylinder at the angle
      // specified by 'tilt'
      RigidTransform3d TSW =
         new RigidTransform3d (0, 0, 0.65*size, 0, Math.PI/2-tilt, 0);
      sleaveMesh.transform (TSW);
      base.addMesh (sleaveMesh);
      mech.addRigidBody (base);

      // create slider - a cylinder
      RigidBody slider = RigidBody.createCylinder (
         "slider", 0.1*size, size, /*density=*/1000.0, /*nsides=*/25);
      slider.setPose (TSW);
      mech.addRigidBody (slider);

      // Add a slider joint between the slider and the base, with frames C
      // and D coincident. D is set to the same as the slider.
      SliderJoint joint = new SliderJoint (slider, base, TSW);
      mech.addBodyConnector (joint);
      // set the range for z
      joint.setMaxZ (0.4*size);
      joint.setMinZ (-0.4*size);

      // set rendering properties
      joint.setAxisLength (0.75*size); // draw C frame
      joint.setDrawFrameC (Frame.AxisDrawStyle.ARROW);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (slider, new Color (0.5f, 1f, 1f));

      // create a control panel to interactively adjust properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (joint, "z");
      panel.addWidget (joint, "zRange");
      panel.addWidget (joint, "drawFrameC");
      panel.addWidget (joint, "drawFrameD");
      panel.addWidget (joint, "axisLength");
      panel.addWidget (joint, "shaftLength");
      panel.addWidget (joint, "shaftRadius");
      panel.addWidget (joint, "linearCompliance");
      panel.addWidget (joint, "rotaryCompliance");
      panel.addWidget (joint, "compliance");
      panel.addWidget (joint, "damping");
      addControlPanel (panel);
   }
}
