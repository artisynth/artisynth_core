package artisynth.demos.mech;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver;
import artisynth.core.mechmodels.PlanarConnector;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.SphericalJoint;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.*;
import artisynth.core.probes.WayPoint;
import artisynth.core.driver.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.gui.widgets.*;
import maspack.render.*;
import maspack.properties.*;

import java.awt.Color;
import java.io.*;

import javax.swing.*;

public class CompliantConstraintDemo extends RootModel {
   public static boolean debug = false;

   private boolean useSphericalJoint = false;

   public void build (String[] args) {

      MechModel mechMod = new MechModel ("mechMod");
      // mechMod.setProfiling (true);
      mechMod.setGravity (0, 0, -50);
      // mechMod.setRigidBodyDamper (new FrameDamper (1.0, 4.0));
      mechMod.setFrameDamping (1.0);
      mechMod.setRotaryDamping (4.0);
      mechMod.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler);

      RigidTransform3d XMB = new RigidTransform3d();
      RigidTransform3d XLW = new RigidTransform3d();
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d TCB = new RigidTransform3d();
      RigidTransform3d TCW = new RigidTransform3d();
      RigidTransform3d XAB = new RigidTransform3d();
      PolygonalMesh mesh;
      int nslices = 16; // number of slices for approximating a circle

      // // set view so tha points upwards
      // X.R.setAxisAngle (1, 0, 0, -Math.PI/2);
      // viewer.setTransform (X);

      double lenx0 = 15;
      double leny0 = 15;
      double lenz0 = 1.5;

      RenderProps props;

      double ks = 10;
      double ds = 10;

      // mechMod.addRigidBody (base);

      // first link
      double lenx1 = 10;
      double leny1 = 2;
      double lenz1 = 3;
      RigidBody link1 = new RigidBody ("link1");
      link1.setInertia (SpatialInertia.createBoxInertia (
         10, lenx1, leny1, lenz1));
      mesh =
         MeshFactory.createRoundedBox (
            lenx1, leny1, lenz1, nslices / 2);
      XMB.setIdentity();
      XMB.R.setAxisAngle (1, 1, 1, 2 * Math.PI / 3);
      mesh.transform (XMB);
      // mesh.setRenderMaterial (Material.createSpecial (Material.GRAY));
      link1.setMesh (mesh, /* fileName= */null);
      XLW.R.setAxisAngle (1, 0, 0, Math.PI / 2);
      // XLW.R.mulAxisAngle (0, 1, 0, Math.PI/4);
      XLW.p.set (0, 0, 1.5 * lenx1);
      link1.setPose (XLW);
      mechMod.addRigidBody (link1);

      // joint 1
      BodyConnector joint1 = null;
      HingeJoint rjoint = null;

      TCA.setIdentity();
      TCA.p.set (-lenx1/2, 0, 0);
      // TCA.R.mulAxisAngle (0, 1, 0, Math.PI/4);
      TCW.mul (link1.getPose(), TCA);
      rjoint = new HingeJoint (link1, TCW);
      rjoint.setName ("joint1");
      rjoint.setShaftLength (4);
      RenderProps.setFaceColor(rjoint, Color.BLUE);
      joint1 = rjoint;

      // second link
      double lenx2 = 10;
      double leny2 = 2;
      double lenz2 = 2;
      RigidBody link2 = new RigidBody ("link2");

      mesh =
         MeshFactory.createRoundedCylinder (
            leny2 / 2, lenx2, nslices, /*nsegs=*/1, /*flatBottom=*/false);
      mesh.transform (XMB);
      link2.setInertia (SpatialInertia.createBoxInertia (
         10, lenx2, leny2, lenz2));
      XLW.R.setAxisAngle (1, 0, 0, Math.PI / 2);
      XLW.p.set (lenx1 / 2 + lenx2 / 2, 0, 1.5 * lenx1);
      if (useSphericalJoint) {
         double ang = 0; // Math.PI/4;
         XLW.R.mulAxisAngle (0, 1, 0, ang);
         XLW.p.y += Math.sin (ang) * lenx2 / 2;
         XLW.p.x -= (1 - Math.cos (ang)) * lenx2 / 2;
      }
      link2.setPose (XLW);
      link2.setMesh (mesh, /* fileName= */null);
      mechMod.addRigidBody (link2);

      BodyConnector joint2 = null;

      // joint 2
      if (useSphericalJoint) {
         TCA.setIdentity();
         TCA.p.set (-lenx2 / 2, 0, 0);
         TCW.mul (link2.getPose(), TCA);
         SphericalJoint sjoint = new SphericalJoint (link2, link1, TCW);
         sjoint.setName ("joint2");
         sjoint.setJointRadius (1.0);
         RenderProps.setFaceColor (sjoint, Color.BLUE);
         joint2 = sjoint;
      }
      else {
         TCA.setIdentity();
         TCA.p.set (-lenx2 / 2, 0, 0);
         TCW.mul (link2.getPose(), TCA);
         rjoint = new HingeJoint (link2, link1, TCW);

         rjoint.setName ("joint2");
         rjoint.setShaftLength (4);
         RenderProps.setFaceColor (rjoint, Color.BLUE);
         joint2 = rjoint;
      }

      mechMod.addBodyConnector (joint1);
      if (joint2 != null) {
         mechMod.addBodyConnector (joint2);
      }

      mechMod.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, Math.toRadians(75), 0));

      addModel (mechMod);

      addControlPanel(mechMod);
   }

   void addControlPanel (MechModel mod) {

      myControlPanel = new ControlPanel ("options", "");
      myControlPanel.addWidget (mod, "integrator");
      myControlPanel.addWidget (this, "maxStepSize");
      myControlPanel.addLabel (" Joint 1 --");
      myControlPanel.addWidget (
         mod, "bodyConnectors/0:linearCompliance");
      myControlPanel.addWidget (
         mod, "bodyConnectors/0:rotaryCompliance");
      myControlPanel.addWidget (
         mod, "bodyConnectors/0:compliance");
      myControlPanel.addWidget (
         mod, "bodyConnectors/0:damping");
      myControlPanel.addLabel (" Joint 2 --");
      myControlPanel.addWidget ( 
         mod, "bodyConnectors/1:linearCompliance");
      myControlPanel.addWidget (
         mod, "bodyConnectors/1:rotaryCompliance");
      myControlPanel.addWidget (
         mod, "bodyConnectors/1:compliance");
      myControlPanel.addWidget (
         mod, "bodyConnectors/1:damping");
      addControlPanel (myControlPanel);
      Main.getMain().arrangeControlPanels (this);
   }

   ControlPanel myControlPanel;

}
