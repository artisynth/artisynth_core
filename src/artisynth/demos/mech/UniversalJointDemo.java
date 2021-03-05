package artisynth.demos.mech;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SkewedUniversalJoint;
import artisynth.core.mechmodels.UniversalJoint;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

public class UniversalJointDemo extends RootModel {

   public void build (String[] args) throws IOException {

      // parse model arguments to see if a skew angle is set
      double skewAngle = 0;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-skew")) {
            if (++i == args.length) {
               System.out.println (
                  "WARNING: option -skew needs another argument");
            }
            else {
               double ang = Double.valueOf (args[i]);
               if (ang <= -90.0 || ang >= 90.0) {
                  System.out.println (
                     "WARNING: skew angle must be between -90 and 90");
               }
               else {
                  skewAngle = Math.toRadians(ang);
               }
            }
         }
         else {
            System.out.println ("WARNING: unknown option "+args[i]);
         }
      }

      // create mech model and set rigid body damping parameters
      MechModel mech = new MechModel ("mech");
      mech.setFrameDamping (0.10);
      mech.setRotaryDamping (0.001);
      addModel (mech);

      double size = 0.5; // size parameter

      // create base - a cylinder rounded at the top
      PolygonalMesh mesh = MeshFactory.createRoundedCylinder (
         size/4, size, /*nslices=*/30, /*negs=*/1, /*flatBottom=*/true);
      RigidBody base = RigidBody.createFromMesh (
         "base", mesh, /*density=*/1000.0, /*scale=*/1.0);
      base.setDynamic (false);
      mech.addRigidBody (base);

      // create tip - a cylinder rounded at both ends
      mesh = MeshFactory.createRoundedCylinder (
         size/6, size, /*nslices=*/30, /*negs=*/1, /*flatBottom=*/false);
      RigidBody tip = RigidBody.createFromMesh (
         "tip", mesh, /*density=*/1000.0, /*scale=*/1.0);
      tip.setPose (new RigidTransform3d (0, 0, 1.45*size));
      mech.addRigidBody (tip);

      // Add a gimbal joint between the tip and the base, with frames C and D
      // initially coincident. A SkewedUniversalJoint is used if the skew angle
      // != 0. Frame D is set (in world coordinates) with its origin at (0, 0,
      // 0.75*size) and its y axis lying along negative world y.
      RigidTransform3d TDW =
         new RigidTransform3d (0, 0, 0.75*size, 0, 0, Math.PI);
      UniversalJoint joint;
      if (skewAngle > 0) {
         joint = new SkewedUniversalJoint (tip, base, TDW, skewAngle);
      }
      else {
         joint = new UniversalJoint (tip, base, TDW);
      }
      // set the coordinate ranges (in degrees)
      joint.setRollRange (-90, 90);
      joint.setPitchRange (-100, 100);
      // set pitch angle so the tip will fall under gravity
      joint.setPitch (-45);
      mech.addBodyConnector (joint);

      // set rendering properties
      joint.setAxisLength (0.75*size); // draw C frame
      joint.setDrawFrameC (Frame.AxisDrawStyle.ARROW);
      joint.setShaftLength (0.9*size); // draw rotational axis
      joint.setShaftRadius (0.05*size);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (tip, new Color (0.5f, 1f, 1f));

      // create control panel to interactively adjust properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (joint, "roll");
      panel.addWidget (joint, "pitch");
      panel.addWidget (joint, "rollRange");
      panel.addWidget (joint, "pitchRange");
      panel.addWidget (joint, "drawFrameC");
      panel.addWidget (joint, "drawFrameD");
      panel.addWidget (joint, "axisLength");
      panel.addWidget (joint, "shaftLength");
      panel.addWidget (joint, "shaftRadius");
      panel.addWidget (joint, "jointRadius");
      panel.addWidget (joint, "linearCompliance");
      panel.addWidget (joint, "rotaryCompliance");
      panel.addWidget (joint, "compliance");
      panel.addWidget (joint, "damping");
      addControlPanel (panel);
   }
}
