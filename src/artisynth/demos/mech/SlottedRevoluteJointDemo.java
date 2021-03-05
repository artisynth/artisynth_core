package artisynth.demos.mech;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SlottedHingeJoint;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

public class SlottedRevoluteJointDemo extends RootModel {

   public void build (String[] args) throws IOException {

      // create mech model and set rigid body damping parameters
      MechModel mech = new MechModel ("mech");
      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);
      addModel (mech);

      double size = 1.0; // size parameter

      // create base - a rounded box
      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         size, size/2, size/4, 12);
      mesh.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));
      RigidBody base = RigidBody.createFromMesh (
         "base", mesh, /*density=*/1000.0, 1.0);
      base.setDynamic (false);
      mech.addRigidBody (base);

      // create slider - another rounded box
      mesh = MeshFactory.createRoundedBox (
         size, size/3, size/3, 2, 1, 1, 12);
      RigidBody slider = RigidBody.createFromMesh (
         "slider", mesh, /*density=*/1000.0, 1.0);
      slider.setPose (new RigidTransform3d (0, 0, size/2));
      mech.addRigidBody (slider);

      // Add a slotted revolute joint between the slider and base, with frames
      // C and D initially coincident. Frame D is set (in world coordinates)
      // with its origin at (0, 0, 0) and its z axis pointing along negative
      // world y.
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.mulRotX (Math.PI/2);
      SlottedHingeJoint joint = new SlottedHingeJoint (slider, base, TDW);
      mech.addBodyConnector (joint);
      // set the coordinate ranges
      joint.setMaxX (size/2);
      joint.setMinX (-size/2);
      joint.setMaxTheta (135.0);
      joint.setMinTheta (-135.0);
      // set theta to -45 degrees so the slider will fall under gravity
      joint.setTheta (-45);

      // set rendering properties
      joint.setAxisLength (0.6*size); // draw C frame
      joint.setDrawFrameC (Frame.AxisDrawStyle.ARROW);
      joint.setShaftLength (0.5*size); // draw shaft
      joint.setShaftRadius (0.05*size);
      joint.setSlotDepth (1.10*size/4); // draw slot
      joint.setSlotWidth (0.1*size);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (slider, new Color (0.5f, 1f, 1f));

      // create a control panel to interactively adjust properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (joint, "theta");
      panel.addWidget (joint, "thetaRange");
      panel.addWidget (joint, "x");
      panel.addWidget (joint, "xRange");
      panel.addWidget (joint, "drawFrameC");
      panel.addWidget (joint, "drawFrameD");
      panel.addWidget (joint, "axisLength");
      panel.addWidget (joint, "shaftLength");
      panel.addWidget (joint, "shaftRadius");
      panel.addWidget (joint, "slotWidth");
      panel.addWidget (joint, "slotDepth");      
      panel.addWidget (joint, "slotRange");
      panel.addWidget (joint, "linearCompliance");
      panel.addWidget (joint, "rotaryCompliance");
      panel.addWidget (joint, "compliance");
      panel.addWidget (joint, "damping");
      addControlPanel (panel);
   }
}
