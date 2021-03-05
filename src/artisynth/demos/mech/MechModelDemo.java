package artisynth.demos.mech;

import java.awt.event.ActionEvent;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer;
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
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.core.workspace.PanController;
import artisynth.core.gui.*;
import maspack.render.*;

import java.awt.Color;
import java.io.*;
import java.util.List;

import javax.swing.*;

public class MechModelDemo extends RootModel {

   public static boolean debug = false;

   boolean readDemoFromFile = false;

   boolean writeDemoToFile = true;

   boolean usePlanarJoint = true;

   boolean useSphericalJoint = false;

   boolean usePlanarContacts = true;

   public MechModel myMech = null;

   public void build (String[] args) {

      myMech = new MechModel ("mechMod");
      // mechMod.setProfiling (true);
      myMech.setGravity (0, 0, -50);
      // myMech.setRigidBodyDamper (new FrameDamper (1.0, 4.0));
      myMech.setFrameDamping (1.0);
      myMech.setRotaryDamping (4.0);
      myMech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler);

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

      double lenx0 = 15;
      double leny0 = 15;
      double lenz0 = 1.5;
      RigidBody base = new RigidBody ("base");
      base.setInertia (SpatialInertia.createBoxInertia (
         10, lenx0, leny0, lenz0));
      mesh = MeshFactory.createBox (lenx0, leny0, lenz0);
      // XMB.setIdentity();
      // XMB.R.setAxisAngle (1, 1, 1, 2*Math.PI/3);
      // mesh.transform (XMB);
      // mesh.setRenderMaterial (Material.createSpecial (Material.GRAY));
      base.setMesh (mesh, /* fileName= */null);
      XLW.setIdentity();
      XLW.p.set (0, 0, 22);
      base.setPose (XLW);
      base.setDynamic (false);
      myMech.addRigidBody (base);

      RenderProps props;

      FrameMarker mk0 = new FrameMarker();
      props = mk0.createRenderProps();
      // props.setColor (Color.GREEN);
      props.setPointRadius (0.5);
      props.setPointStyle (Renderer.PointStyle.SPHERE);
      mk0.setRenderProps (props);
      myMech.addFrameMarker (mk0, base, new Point3d (lenx0 / 2, leny0 / 2, 0));

      FrameMarker mk1 = new FrameMarker();
      mk1.setRenderProps (props);
      myMech.addFrameMarker (
         mk1, base, new Point3d (-lenx0 / 2, -leny0 / 2, 0));

      FrameMarker mk2 = new FrameMarker();
      mk2.setRenderProps (props);

      FrameMarker mk3 = new FrameMarker();
      mk3.setRenderProps (props);

      double ks = 10;
      double ds = 10;

      AxialSpring spr0 = new AxialSpring (50, 10, 0);
      AxialSpring spr1 = new AxialSpring (50, 10, 0);

      props = spr0.createRenderProps();
      props.setLineStyle (Renderer.LineStyle.CYLINDER);
      props.setLineRadius (0.2);
      props.setLineColor (Color.RED);
      spr0.setRenderProps (props);
      spr1.setRenderProps (props);

      // myMech.addRigidBody (base);

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
      myMech.addRigidBody (link1);

      myMech.addFrameMarker (mk2, link1, new Point3d (
         -lenx1 / 2, 0, -lenz1 / 2));
      myMech.addFrameMarker (
         mk3, link1, new Point3d (-lenx1 / 2, 0, lenz1 / 2));

      // second link
      double lenx2 = 10;
      double leny2 = 2;
      double lenz2 = 2;
      RigidBody link2 = new RigidBody ("link2");

      if (false) // useSphericalJoint)
      {
         mesh =
            MeshFactory.createRoundedCylinder (
               leny2 / 2, lenx2, nslices, /*nsegs=*/1, /*flatBottom=*/false);
         link2.setInertia (SpatialInertia.createBoxInertia (
            10, leny2, leny2, lenx2));
         XLW.R.setAxisAngle (1, 0, 0, Math.PI / 2);
         XLW.p.set (lenx1 / 2, lenx2 / 2, lenx1);
         link2.setPose (XLW);
      }
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
      myMech.addRigidBody (link2);

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
         joint2 = sjoint;
      }
      else {
         TCA.setIdentity();
         TCA.p.set (-lenx2 / 2, 0, 0);
         XAB.mulInverseLeft (link1.getPose(), link2.getPose());
         TCB.mul (XAB, TCA);
         HingeJoint rjoint = new HingeJoint (link2, TCA, link1, TCB);
         rjoint.setName ("joint2");
         rjoint.setShaftLength (4);
         RenderProps.setFaceColor (rjoint, Color.BLUE);
         joint2 = rjoint;
      }

      if (joint2 != null) {
         myMech.addBodyConnector (joint2);
      }

      myMech.attachAxialSpring (mk0, mk2, spr0);
      myMech.attachAxialSpring (mk1, mk3, spr1);

      if (usePlanarContacts) {
         TCA.setIdentity();
         TCA.p.set (lenx2 / 2 + leny2 / 2, 0, 0);
         TCB.setIdentity();

         TCB.R.setIdentity();
         TCB.R.setAxisAngle (0, 0, 1, Math.PI / 2);
         TCB.R.mulAxisAngle (1, 0, 0, Math.toRadians (20));

         PlanarConnector contact1 = new PlanarConnector (link2, TCA.p, TCB);
         contact1.setUnilateral (true);
         contact1.setName ("contact1");
         contact1.setPlaneSize (20);
         RenderProps.setFaceColor (contact1, new Color (0.5f, 0.5f, 1f));
         RenderProps.setAlpha (contact1, 0.5);
         myMech.addBodyConnector (contact1);

         TCB.R.setIdentity();
         TCB.R.setAxisAngle (0, 0, 1, Math.PI / 2);
         TCB.R.mulAxisAngle (1, 0, 0, -Math.toRadians (20));

         PlanarConnector contact2 = new PlanarConnector (link2, TCA.p, TCB);
         contact2.setUnilateral (true);
         contact2.setName ("contact2");
         contact2.setPlaneSize (20);
         RenderProps.setFaceColor (contact2, new Color (0.5f, 0.5f, 1f));
         RenderProps.setAlpha (contact2, 0.5);

         myMech.addBodyConnector (contact2);
      }

      myMech.setBounds (new Point3d (0, 0, -10), new Point3d (0, 0, 10));

      addModel (myMech);
      addControlPanel (myMech);

      // RigidTransform3d X = new RigidTransform3d (link1.getPose());
      // X.R.mulRpy (Math.toRadians(-10), 0, 0);
      // link1.setPose (X);
      // myMech.projectRigidBodyPositionConstraints();

      //myMech.setProfiling (true);
      //myMech.setIntegrator (Integrator.ForwardEuler);
      //addBreakPoint (0.57);

      //addController (new PanController (this, 5.0, 1.0, 11.0));

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
      return artisynth.core.util.TextFromFile.getTextOrError (
         ArtisynthPath.getSrcRelativeFile (this, "MechModelDemo.txt"));
   }

}
