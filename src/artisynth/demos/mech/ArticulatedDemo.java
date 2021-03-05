package artisynth.demos.mech;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.PlanarConnector;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SphericalJoint;
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

public class ArticulatedDemo extends RootModel {
   public static boolean debug = false;

   boolean usePlanarContacts = false;

   double zPlane = -0.5;

   private static RigidBody ground = new RigidBody ("ground");

   void create9LinkLoop (MechModel mech) {
      RigidBody box = addBox (mech, /* mass= */10, 20, 10, 2, 0, 0, 10);
      box.setDynamic (false);
      Point3d base0 = new Point3d (-5, 0, 9);
      Point3d tip0 = new Point3d (-8, 0, 0);
      Point3d base1 = new Point3d (5, 0, 9);
      Point3d tip1 = new Point3d (2, -2, 0);
      double radius = 1.0;
      RigidBody link0 =
         addSphericalLinkage (mech, radius, ground, base0, null, tip0);
      RigidBody link1 =
         addSphericalLinkage (mech, radius, ground, base1, null, tip1);

      RigidBody link2 =
         addSphericalLinkage (mech, radius, link0, tip0, link1, tip1);

      Point3d tip3 = new Point3d (0, 0, -5);
      Point3d tip4 = new Point3d (5, 3, -5);
      RigidBody link3 =
         addSphericalLinkage (mech, radius, link0, tip0, null, tip3);
      RigidBody link4 =
         addSphericalLinkage (mech, radius, link1, tip1, null, tip4);

      RigidBody link5 =
         addSphericalLinkage (mech, radius, link3, tip3, link4, tip4);

      Point3d tip6 = new Point3d (-2, 0, -10);
      Point3d tip7 = new Point3d (-3, 0, -15);
      RigidBody link6 =
         addSphericalLinkage (mech, radius, link3, tip3, null, tip6);
      RigidBody link7 =
         addSphericalLinkage (mech, radius, link6, tip6, null, tip7);

      Point3d tip8 = new Point3d (6, 1, -10);
      Point3d tip9 = new Point3d (8, 2, -15);
      RigidBody link8 =
         addSphericalLinkage (mech, radius, link4, tip4, null, tip8);
      RigidBody link9 =
         addSphericalLinkage (mech, radius, link8, tip8, null, tip9);
   }

   void create3LinkLoop (MechModel mech) {
      RigidBody box = addBox (mech, /* mass= */10, 20, 10, 2, 0, 0, 10);
      box.setDynamic (false);
      Point3d base0 = new Point3d (-5, 0, 9);
      Point3d tip0 = new Point3d (-8, 0, 0);
      Point3d base1 = new Point3d (5, 0, 9);
      Point3d tip1 = new Point3d (2, -2, 0);
      double radius = 1.0;
      RigidBody link0 =
         addSphericalLinkage (mech, radius, ground, base0, null, tip0);
      RigidBody link1 =
         addSphericalLinkage (mech, radius, ground, base1, null, tip1);

      RigidBody link2 =
         addSphericalLinkage (mech, radius, link0, tip0, link1, tip1);
   }

   void create3LinkPlaneCollider (MechModel mech) {
      usePlanarContacts = true;

      Point3d base0 = new Point3d (-5, 0, 9);
      Point3d tip0 = new Point3d (0, 0, 5);
      Point3d tip1 = new Point3d (5, 0, 3);
      Point3d tip2 = new Point3d (10, 0, 3);
      double radius = 1.0;
      RigidBody link0 =
         addSphericalLinkage (mech, radius, ground, base0, null, tip0);
      RigidBody link1 =
         addSphericalLinkage (mech, radius, link0, tip0, null, tip1);
      RigidBody link2 =
         addSphericalLinkage (mech, radius, link1, tip1, null, tip2);
   }

   void createLongLinkage (MechModel mech, int nlinks) {
      Point3d base0 = new Point3d (0, 0, 9);
      RigidBody link = ground;
      Point3d lastTip = base0;
      double radius = 1.0;
      for (int i = 0; i < nlinks; i++) {
         Point3d tip = new Point3d ((i + 1) * 5, 0, 9 - (i + 1) * 5);
         link = addSphericalLinkage (mech, radius, link, lastTip, null, tip);
         lastTip = tip;
      }
   }

   void createLongLinkage (
      MechModel mech, Point3d start, Point3d end, double curveRadius,
      double linkRadius, int nlinks) {
      RigidBody link = ground;
      Point3d lastTip = start;
      double chordLength = start.distance (end);
      double theta = 2 * Math.asin (chordLength / (2 * curveRadius));
      double chordHeight = curveRadius * Math.cos (theta / 2);

      Vector3d xdir = new Vector3d();
      Vector3d ydir = new Vector3d();

      xdir.sub (end, start);
      xdir.scale (1 / chordLength);
      xdir.normalize();
      ydir.scaledAdd (xdir.z, xdir, new Vector3d (0, 0, -1));
      ydir.normalize();

      for (int i = 0; i < nlinks - 1; i++) {
         double ang = theta / 2 - (i + 1) * theta / nlinks;
         double x = chordLength / 2 - curveRadius * Math.sin (ang);
         double y = curveRadius * Math.cos (ang) - chordHeight;
         Point3d tip = new Point3d();
         tip.set (start);
         tip.scaledAdd (x, xdir, tip);
         tip.scaledAdd (y, ydir, tip);
         link =
            addSphericalLinkage (mech, linkRadius, link, lastTip, null, tip);
         lastTip = tip;
      }
      addSphericalLinkage (mech, linkRadius, link, lastTip, null, end);
   }

   void createLadderPlaneCollider (MechModel mech) {
      usePlanarContacts = true;

      Point3d base0 = new Point3d (-5, 2, 9);
      Point3d tip0 = new Point3d (0, 2, 5);
      Point3d tip1 = new Point3d (5, 2, 3);
      Point3d tip2 = new Point3d (10, 2, 3);
      double radius = 1.0;
      RigidBody link0 =
         addSphericalLinkage (mech, radius, ground, base0, null, tip0);
      RigidBody link1 =
         addSphericalLinkage (mech, radius, link0, tip0, null, tip1);
      RigidBody link2 =
         addSphericalLinkage (mech, radius, link1, tip1, null, tip2);

      Point3d base1 = new Point3d (-5, -2, 9);
      Point3d tip3 = new Point3d (0, -2, 5);
      Point3d tip4 = new Point3d (5, -2, 3);
      Point3d tip5 = new Point3d (10, -2, 3);
      RigidBody link3 =
         addSphericalLinkage (mech, radius, ground, base1, null, tip3);
      RigidBody link4 =
         addSphericalLinkage (mech, radius, link3, tip3, null, tip4);
      RigidBody link5 =
         addSphericalLinkage (mech, radius, link4, tip4, null, tip5);

      addSphericalLinkage (mech, radius, link5, tip5, link2, tip2);
      addSphericalLinkage (mech, radius, link4, tip4, link1, tip1);
   }

   void createCollidingLinkage (MechModel mech) {
      Point3d base0 = new Point3d (-1, 2, 9);
      Point3d base1 = new Point3d (-1, -2, 9);
      Point3d tip0 = new Point3d (-3, 0, 5);
      Point3d tip1 = new Point3d (-7, 0, 3);
      double radius = 1.0;
      RigidBody link0 =
         addSphericalLinkage (mech, radius, ground, base0, null, tip0);
      RigidBody link1 =
         addSphericalLinkage (mech, radius, link0, tip0, null, tip1);
      RigidBody link2 =
         addSphericalLinkage (mech, radius, ground, base1, link0, tip0);

      Point3d base2 = new Point3d (1, 2, 9);
      Point3d base3 = new Point3d (1, -2, 9);
      Point3d tip2 = new Point3d (3, 0, 5);
      Point3d tip3 = new Point3d (7, 0, 3);
      RigidBody link3 =
         addSphericalLinkage (mech, radius, ground, base2, null, tip2);
      RigidBody link4 =
         addSphericalLinkage (mech, radius, link3, tip2, null, tip3);
      RigidBody link5 =
         addSphericalLinkage (mech, radius, ground, base3, link3, tip2);
   }

   public void build (String[] args) {

      MechModel mechMod = new MechModel ("mechMod");
      mechMod.setGravity (0, 0, -9.8);
      mechMod.setFrameDamping (1.0);
      mechMod.setRotaryDamping (4.0);

      // mechMod.setIntegrator (MechSystemSolver.Integrator.BackwardEuler);

      // create9LinkLoop (mechMod);
      // create3LinkPlaneCollider (mechMod);
      // createLadderPlaneCollider (mechMod);
      // createCollidingLinkage (mechMod);
      // createLongLinkage (mechMod, 6);
      usePlanarContacts = true;

      createLongLinkage (mechMod, new Point3d (-5, 2, 10), new Point3d (
         15, 2, 5), 20, 0.5, 10);
      createLongLinkage (mechMod, new Point3d (5, -2, 10), new Point3d (
         -15, -2, 5), 20, 0.5, 10);

      mechMod.setBounds (new Point3d (0, 0, -10), new Point3d (0, 0, 10));
      //mechMod.setProfiling (true);
      addModel (mechMod);
      addControlPanel (mechMod);
   }

   private RigidBody addBox (
      MechModel mechMod, double mass, double wx, double wy, double wz,
      double px, double py, double pz) {
      RigidBody box = new RigidBody (null);
      box.setInertia (SpatialInertia.createBoxInertia (mass, wx, wy, wz));
      box.setMesh (MeshFactory.createBox (wx, wy, wz), null);
      RigidTransform3d XBoxToWorld = new RigidTransform3d();
      XBoxToWorld.p.set (px, py, pz);
      box.setPose (XBoxToWorld);
      mechMod.addRigidBody (box);
      return box;
   }

   private RigidBody addSphericalLinkage (
      MechModel mechMod, double radius, RigidBody body0, Point3d pnt0,
      RigidBody body1, Point3d pnt1) {
      // set up the position of the link
      Vector3d u = new Vector3d();
      u.sub (pnt1, pnt0);
      double len = u.norm();
      u.scale (1 / len);
      RigidTransform3d XLinkToWorld = new RigidTransform3d();
      XLinkToWorld.p.scaledAdd (0.5 * len, u, pnt0);
      XLinkToWorld.R.setZDirection (u);

      // create the link
      PolygonalMesh mesh =
         MeshFactory.createRoundedCylinder (
            radius, len, /*nslices=*/12, /*nsegs=*/1, /*flatBottom=*/false);
      SpatialInertia inertia = SpatialInertia.createBoxInertia (
      /* mass= */10, radius, radius, len);
      RigidBody link = new RigidBody (null);
      link.setInertia (inertia);
      link.setMesh (mesh, null);
      link.setPose (XLinkToWorld);

      mechMod.addRigidBody (link);

      // create the spherical joint(s)

      RigidTransform3d TCW = new RigidTransform3d();
      RigidTransform3d TCA = new RigidTransform3d();
      if (body0 != null) {
         TCA.setIdentity();
         TCA.p.set (0, 0, -len / 2);
         TCW.mul (XLinkToWorld, TCA);
         SphericalJoint joint0;
         if (body0 == ground) {
            joint0 = new SphericalJoint (link, TCW);
         }
         else {
            joint0 = new SphericalJoint (link, body0, TCW);
         }
         mechMod.addBodyConnector (joint0);
      }
      if (body1 != null) {
         TCA.setIdentity();
         TCA.p.set (0, 0, len / 2);
         TCW.mul (XLinkToWorld, TCA);
         SphericalJoint joint1;
         if (body1 == ground) {
            joint1 = new SphericalJoint (link, TCW);
         }
         else {
            joint1 = new SphericalJoint (link, body1, TCW);
         }
         mechMod.addBodyConnector (joint1);
      }
      if (usePlanarContacts) { // set up a unilateral constraint at the tip
         TCW.setIdentity();
         TCW.p.set (0, 0, zPlane);
         Point3d pCA = new Point3d (0, 0, len / 2);
         PlanarConnector contact = new PlanarConnector (link, pCA, TCW);
         contact.setUnilateral (true);
         contact.setPlaneSize (20);
         mechMod.addBodyConnector (contact);
      }

      return link;
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
      return "A model showing lots of constrained rigid bodies";
   }

}
