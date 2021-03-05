package artisynth.demos.mech;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SphericalJoint;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

public class SphericalJointDemo extends RootModel {

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

      // Add a spherical joint between the tip and the base, with frames C and
      // D initially coincident. Frame D is set (in world coordinates) with its
      // origin at (0, 0, 0.75*size) and its axes aligned with world.
      RigidTransform3d TDW =
         new RigidTransform3d (0, 0, 0.75*size, 0, 0, 0);
      SphericalJoint joint = new SphericalJoint (tip, base, TDW);
      // set rotation ranges (in degrees)
      joint.setMaxRotation (100);
      joint.setMaxTilt (120); // will enable tilt limit over rotation limit
      mech.addBodyConnector (joint);

      // rotate tip pose so that it will fall under gravity
      RigidTransform3d TTW = new RigidTransform3d();
      tip.getPose (TTW);
      TTW.mulXyz (0, 0, -size/2);
      TTW.mulRpy (0, Math.PI/4, 0);
      TTW.mulXyz (0, 0, size/2);
      tip.setPose (TTW);

      // set rendering properties
      joint.setAxisLength (0.75*size); // draw frame C
      joint.setDrawFrameC (Frame.AxisDrawStyle.ARROW);
      joint.setJointRadius (0.10*size); // draw ball around the joint
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (tip, new Color (0.5f, 1f, 1f));

      // create control panel to interactively adjust properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (joint, "tilt");
      panel.addWidget (joint, "maxTilt");
      panel.addWidget (joint, "tiltLimited"); 
      panel.addWidget (joint, "maxRotation");
      panel.addWidget (joint, "rotationLimited");
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
