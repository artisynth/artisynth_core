package artisynth.demos.mech;

import java.awt.Color;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SegmentedPlanarConnector;
import artisynth.core.workspace.RootModel;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

public class SegmentedPlaneDemo extends RootModel {

   public void build (String[] args) {

      // create mech model and set rigid body damping parameters
      MechModel mech = new MechModel ("mech");
      mech.setFrameDamping (10.0);
      mech.setRotaryDamping (1.0);
      addModel (mech);
      mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler);

      // create box that will slide around on the plane
      RigidBody box = RigidBody.createBox (
         "box", 10.0, 5.0, 3.0, /*density=*/10.0);
      box.setPose (new RigidTransform3d (0, 0, 1.5, 0, 0, 0)); 
      mech.addRigidBody (box);

      // connect corner point (-5, 2.5, 1.5) on the box (local coordinates) to
      // a segmented plane, with 5 segments, whose coordinate system is
      // centered on (and aligned with) world coordinates
      RigidTransform3d TDW = new RigidTransform3d();
      SegmentedPlanarConnector connector =
         new SegmentedPlanarConnector (
            box, new Vector3d (-5, 2.5, 1.5), TDW,
            // coordinates of the segment corners in the x-y plane:
            new double[] { -6,4,  -3,1,  -1,0,  1,0,  3,1,  6,4 });
      connector.setUnilateral (true);  // make the connector unilateral
      connector.setPlaneSize (10);     // visible width of the segments in z
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
