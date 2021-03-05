package artisynth.demos.mech;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.util.*;
import artisynth.core.mechmodels.CollisionManager;
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
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.render.*;

import java.awt.Color;
import java.io.*;

import javax.swing.*;

public class MechModelCollide extends RootModel {

   boolean useSphericalJoint = false;
   boolean transparentLinks = true;

   public void build (String[] args) {

      MechModel mechMod = new MechModel ("mechMod");
      // mechMod.setProfiling (true);
      mechMod.setGravity (0, 0, -98);
      // mechMod.setRigidBodyDamper (new FrameDamper (1.0, 4.0));
      mechMod.setFrameDamping (1.0);
      mechMod.setRotaryDamping (4.0);
      RigidTransform3d XMB = new RigidTransform3d();
      RigidTransform3d XLW = new RigidTransform3d();
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d TCB = new RigidTransform3d();
      RigidTransform3d XAB = new RigidTransform3d();
      PolygonalMesh mesh;
      int nslices = 16; // number of slices for approximating a circle

      // // set view so tha points upwards
      // X.R.setAxisAngle (1, 0, 0, -Math.PI/2);
      // viewer.setTransform (X);

      double lenx0 = 30;
      double leny0 = 30;
      double lenz0 = 2;

      RigidBody base =
         RigidBody.createBox ("base", lenx0, leny0, lenz0, 0.2);
      base.setDynamic (false);

      mechMod.setDefaultCollisionBehavior (true, 0.20);

      RenderProps props;

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
      
      if (transparentLinks) {
         RenderProps.setFaceStyle (link1, Renderer.FaceStyle.NONE);
         RenderProps.setDrawEdges (link1, true);
         RenderProps.setFaceStyle (link2, Renderer.FaceStyle.NONE);
         RenderProps.setDrawEdges (link2, true);
      }

      BodyConnector joint2 = null;

      // joint 2
      if (useSphericalJoint) {
         TCA.setIdentity();
         TCA.p.set (-lenx2 / 2, 0, 0);
         XAB.mulInverseLeft (link1.getPose(), link2.getPose());
         TCB.mul (XAB, TCA);
         SphericalJoint sjoint = new SphericalJoint (link2, TCA, link1, TCB);
         sjoint.setName ("joint2");
         sjoint.setJointRadius (1.0);
         RenderProps.setFaceColor (sjoint, Color.BLUE);
         mechMod.addBodyConnector (sjoint);
         joint2 = sjoint;
      }
      else {
         TCA.setIdentity();
         TCA.p.set (-lenx2 / 2, 0, 0);
         // TCA.R.mulAxisAngle (1, 0, 0, -Math.toRadians(90));
         XAB.mulInverseLeft (link1.getPose(), link2.getPose());
         TCB.mul (XAB, TCA);
         HingeJoint rjoint = new HingeJoint (link2, TCA, link1, TCB);

         rjoint.setName ("joint2");
         rjoint.setAxisLength (4);
         RenderProps.setFaceColor (rjoint, Color.BLUE);
         mechMod.addBodyConnector (rjoint);
         rjoint.setTheta (-35);
         joint2 = rjoint;
      }

      mechMod.transformGeometry (
         new RigidTransform3d (-5, 0, 10, 1, 1, 0, Math.toRadians (30)));
      mechMod.addRigidBody (base);
      mechMod.setCollisionBehavior (link1, link2, false);

      CollisionManager cm = mechMod.getCollisionManager();
      RenderProps.setVisible (cm, true);
      RenderProps.setLineWidth (cm, 3);      
      RenderProps.setLineColor (cm, Color.RED);
      cm.setDrawContactNormals (true);
      mechMod.setPenetrationTol (1e-3);

      // try {
      //    MechSystemSolver.setLogWriter (
      //       ArtisynthIO.newIndentingPrintWriter ("solve.txt"));
      // }
      // catch (Exception e)  {
      // }
      
      addModel (mechMod);

      // mechMod.setIntegrator (Integrator.ForwardEuler);
      //addBreakPoint (0.51);
      //mechMod.setProfiling (true);
      addControlPanel (mechMod);
      
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
      return null;
   }

}
