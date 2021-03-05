package artisynth.demos.mech;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.PlanarTranslationJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

public class PlanarTranslationJointDemo extends RootModel {

   public void build (String[] args) throws IOException {

      // create mech model and set rigid body damping parameters
      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -9.8);
      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);
      addModel (mech);

      double size = 1.0; // size parameter

      // create base plane - a flat disk
      RigidBody base = RigidBody.createCylinder (
         "base", size, size/8, /*density=*/1000.0, /*nsides=*/100);
      base.setDynamic (false);
      mech.addRigidBody (base);

      // create box to slide on the plane
      RigidBody box = RigidBody.createBox (
         "box", size/2, size/2, size/8, /*density=*/1000.0);
      box.setPose (new RigidTransform3d (0, 0, size/8));
      mech.addRigidBody (box);

      // Add a planer translation joint between the box and the base, with
      // frames C and D initially coincident. Frame D is set so that its z axis
      // is aligned with the world z.
      PlanarTranslationJoint joint = new PlanarTranslationJoint (
         box, base, new Point3d (0, 0, size/8), Vector3d.Z_UNIT);
      mech.addBodyConnector (joint);
      // set coordinate ranges
      joint.setMaxX (size/2);
      joint.setMinX (-size/2);
      joint.setMaxY (size/2);
      joint.setMinY (-size/2);

      // tilt the entire model by 45 degrees about the x axis, so the box will
      // slide under gravity
      mech.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, 0, Math.PI/4));

      // set rendering properties
      joint.setAxisLength (0.5*size); // draw frame C
      joint.setDrawFrameC (Frame.AxisDrawStyle.ARROW);
      RenderProps.setFaceColor (box, new Color (0.5f, 1f, 1f)); // set color

      // create control panel to interactively adjust properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (joint, "x");
      panel.addWidget (joint, "xRange");
      panel.addWidget (joint, "y");
      panel.addWidget (joint, "yRange");
      panel.addWidget (joint, "drawFrameC");
      panel.addWidget (joint, "drawFrameD");
      panel.addWidget (joint, "axisLength");
      panel.addWidget (joint, "linearCompliance");
      panel.addWidget (joint, "rotaryCompliance");
      panel.addWidget (joint, "compliance");
      panel.addWidget (joint, "damping");
      addControlPanel (panel);
   }
}
