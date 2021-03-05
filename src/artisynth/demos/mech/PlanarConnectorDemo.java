package artisynth.demos.mech;

import java.awt.Color;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.PlanarConnector;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

public class PlanarConnectorDemo extends RootModel {
   public static boolean debug = false;

   public void build (String[] args) {

      // create mech model and set rigid body damping parameters
      MechModel mech = new MechModel ("mech");
      mech.setFrameDamping (10.0);
      mech.setRotaryDamping (1.0);
      addModel (mech);

      // create box that will slide around on the plane
      RigidBody box = RigidBody.createBox (
         "box", 6.0, 5.0, 3.0, /*density=*/10.0);
      box.setPose (new RigidTransform3d (0, 0, -1.5, 0, 0, 0)); 
      mech.addRigidBody (box);

      // connect corner point (-3, 2.5, 1.5) on the box (local coordinates) to
      // a plane centered on (and aligned with) world coordinates
      RigidTransform3d TDW = new RigidTransform3d();
      PlanarConnector connector =
         new PlanarConnector (box, new Vector3d (-3, 2.5, 1.5), TDW);
      connector.setPlaneSize (10); // set visible plane size to 10
      mech.addBodyConnector (connector);

      // create a control panel to interactively adjust properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (connector, "unilateral");
      panel.addWidget (connector, "drawFrameC");
      panel.addWidget (connector, "drawFrameD");
      panel.addWidget (connector, "axisLength");
      panel.addWidget (connector, "linearCompliance");
      panel.addWidget (connector, "compliance");
      panel.addWidget (connector, "damping");
      addControlPanel (panel);

      // set rendering properties      
      RenderProps.setSphericalPoints (mech, 0.25, Color.BLUE);
      RenderProps.setFaceColor (box, new Color (0.5f, 1f, 1f));
      RenderProps.setFaceColor (connector, Color.LIGHT_GRAY);
   }
}
