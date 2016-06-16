package artisynth.demos.mech;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.render.Renderer.PointStyle;
import maspack.util.*;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SegmentedPlanarConnector;
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

public class SegmentedPlaneDemo extends RootModel {
   public static boolean debug = false;

   boolean readDemoFromFile = false;

   boolean writeDemoToFile = true;

   public void build (String[] args) {

      // set up the mechmodel

      MechModel mechMod = new MechModel ("mechMod");
      mechMod.setGravity (0, 0, -9.8);
      mechMod.setFrameDamping (1.0);
      mechMod.setRotaryDamping (4.0);
      mechMod.setIntegrator (MechSystemSolver.Integrator.BackwardEuler);

      // set up the rigid body and the plane constraint

      double lenx = 10;
      double leny = 5;
      double lenz = 3;
      RigidBody box = new RigidBody ("box");
      RigidTransform3d XBoxToWorld = new RigidTransform3d();
      box.setInertia (SpatialInertia.createBoxInertia (
         10, lenx, leny, lenz));
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
      SegmentedPlanarConnector segPlanes =
         new SegmentedPlanarConnector (
            box, new Vector3d (-5, 2.5, 1.5), XPlanesToWorld,
            // new double[]
            // { -6, -4,
            // -3, -1,
            // -1, 0,
            // 1, 0,
            // 3, -1,
            // 6, -4,
            // });

            new double[] { -6, 4, -3, 1, -1, 0, 1, 0, 3, 1, 6, 4, });
      segPlanes.setUnilateral (true);
      segPlanes.setPlaneSize (10);
      
      RenderProps props = segPlanes.createRenderProps();
      props.setPointColor (Color.blue);
      props.setPointStyle (PointStyle.SPHERE);
      props.setPointRadius (0.25);
      segPlanes.setRenderProps (props);

      // mechMod.addRigidBody (box);
      mechMod.addBodyConnector (segPlanes);

      addModel (mechMod);
      addControlPanel (mechMod);
      
      // AffineTransform3d X = new AffineTransform3d ();
      // X.applyScaling (1, 1, 2);
      // mechMod.transformGeometry (X);

      // RigidTransform3d X = new RigidTransform3d (0, 0, 1.5);
      // box.transformGeometry (X);
   }

   ControlPanel myControlPanel;

   public void addControlPanel (MechModel mech) {
      myControlPanel = new ControlPanel ("options", "");
      myControlPanel.addWidget (mech, "integrator");
      myControlPanel.addWidget (mech, "maxStepSize");
      addControlPanel (myControlPanel);
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "A demo to test segmented plane constraints";
   }
}
