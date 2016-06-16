package artisynth.demos.mech;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.*;
import artisynth.core.driver.*;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.render.*;

import java.awt.Color;
import java.io.*;

import javax.swing.*;

public class PlanarConnectorDemo extends RootModel {
   public static boolean debug = false;

   boolean readDemoFromFile = false;

   boolean writeDemoToFile = true;

   public void build (String[] args) {

      // set up the mechmodel

      MechModel mechMod = new MechModel ("mechMod");
      mechMod.setGravity (0, 0, -9.8);
      //mechMod.setFrameDamping (1.0);
      //mechMod.setRotaryDamping (4.0);
      mechMod.setPenetrationLimit (0);
      mechMod.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler);

      // set up the rigid body and the plane constraint

      double lenx = 6;
      double leny = 5;
      double lenz = 3;
      RigidBody box = new RigidBody ("box");
      RigidTransform3d XBoxToWorld = new RigidTransform3d();
      box.setInertia (SpatialInertia.createBoxInertia (
         6, lenx, leny, lenz));
      PolygonalMesh mesh = MeshFactory.createBox (lenx, leny, lenz);
      box.setMesh (mesh, /* fileName= */null);

      XBoxToWorld.p.set (0, 0, lenz / 2);
      box.setPose (XBoxToWorld);
      mechMod.addRigidBody (box);

      // FrameMarker mkr = new FrameMarker (box, -5, 2.5, 1.5);
      // mechMod.addFrameMarker (mkr);
      // RenderProps props = mkr.createRenderProps();
      // props.setPointColor (Color.blue);
      // props.setPointStyle (RenderProps.PointStyle.SPHERE);
      // props.setPointRadius (0.25);
      // mkr.setRenderProps (props);

      RigidTransform3d XPlanesToWorld = new RigidTransform3d();
      // XPlanesToWorld.R.setAxisAngle(0,1,0,Math.PI);
      RigidTransform3d XPW = new RigidTransform3d(0, 0, 3);

      // Connection on the corner
      //PlanarConnector connector =
      //   new PlanarConnector (box, new Vector3d (lenx/2, -2.5, 1.5), XPW);
      // Connection in the center
      PlanarConnector connector =
         new PlanarConnector (box, new Vector3d (0, 0, 1.5), XPW);

      connector.setUnilateral (false);
      connector.setPlaneSize (10);
      
      RenderProps props = connector.createRenderProps();
      props.setPointColor (Color.blue);
      props.setPointStyle (Renderer.PointStyle.SPHERE);
      props.setPointRadius (0.25);
      connector.setRenderProps (props);

      // mechMod.addRigidBody (box);
      mechMod.addBodyConnector (connector);

      addModel (mechMod);
      addControlPanel (mechMod);

      RigidTransform3d X = new RigidTransform3d (box.getPose());
      X.p.z += 2;
      box.setPose (X);
   }

   ControlPanel myControlPanel;

   public void addControlPanel (MechModel mech) {
      myControlPanel = new ControlPanel ("options", "");
      myControlPanel.addWidget (mech, "integrator");
      myControlPanel.addWidget (mech, "maxStepSize");
      myControlPanel.addWidget (mech, "bodyConnectors/0:unilateral");
      myControlPanel.addWidget (mech, "bodyConnectors/0:compliance");
      myControlPanel.addWidget (mech, "bodyConnectors/0:damping");
      addControlPanel (myControlPanel);
   }

   public void detach (DriverInterface driver) {
      super.detach (driver);
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "A demo to test segmented plane constraints";
   }
}
