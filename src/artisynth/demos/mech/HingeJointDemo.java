package artisynth.demos.mech;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

public class HingeJointDemo extends RootModel {

   public void build (String[] args) throws IOException {

      // create mech model and set rigid body damping parameters
      MechModel mech = new MechModel ("mech");
      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);
      addModel (mech);

      double size = 1.0; // size parameter

      // create base - a rounded box flat at the bottom
      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         size, size/2, size/4, 2, 1, 1, 12, /*flatBottom=*/true);
      RigidBody base = RigidBody.createFromMesh (
         "base", mesh, /*density=*/1000.0, 1.0);
      base.setDynamic (false);
      mech.addRigidBody (base);

      // create tip - a box rounded at both ends
      mesh = MeshFactory.createRoundedBox (
         size, size/3, size/3, 2, 1, 1, 12);
      RigidBody tip = RigidBody.createFromMesh (
         "tip", mesh, /*density=*/1000.0, 1.0);
      tip.setPose (new RigidTransform3d (0, 0, size));
      mech.addRigidBody (tip);

      // Add a hinge joint between the tip and base, with frames C and D
      // initially coincident. Frame D is set (in world coordinates) with its
      // origin at (0, 0, size/2) and its z axis pointing along negative
      // (world) y.
      HingeJoint joint = new HingeJoint (
         tip, base, new Point3d (0, 0, size/2), new Vector3d (0, -1, 0));
      mech.addBodyConnector (joint);
      // set the range for theta (in degrees)
      joint.setMaxTheta (135.0);
      joint.setMinTheta (-135.0);
      // set theta to -45 degrees so the tip will fall under gravity
      joint.setTheta (-45);

      // set rendering properties
      joint.setAxisLength (0.6*size); // draw C frame
      joint.setDrawFrameC (Frame.AxisDrawStyle.ARROW);
      joint.setShaftLength (0.5*size); // draw shaft
      joint.setShaftRadius (0.05*size);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (tip, new Color (0.5f, 1f, 1f));

      // create a control panel to interactively adjust properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (joint, "theta");
      panel.addWidget (joint, "thetaRange");
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
