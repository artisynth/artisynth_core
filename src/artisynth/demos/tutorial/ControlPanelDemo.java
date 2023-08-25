package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;
import artisynth.core.gui.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class ControlPanelDemo extends RootModel {

   double myStiffness = 10.0;
   double myMaxForce = 100.0;
   double DTOR = Math.PI/180;

   Muscle attachMuscle (String name, MechModel mech, Point p0, Point p1) {
      Muscle mus = new Muscle (name);
      mus.setMaterial (
         new SimpleAxialMuscle (myStiffness, /*damping=*/0, myMaxForce));
      mech.attachAxialSpring (p0, p1, mus);
      return mus;
   }

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      mech.setGravity (0, 0, 0);
      double density = 100.0;
      double inertialDamping = 1.0;
      double particleMass = 50.0;
      double restLength = 1.0;
      double bodyRotY = Math.toRadians(60);

      mech.setInertialDamping (0.1);

      RigidBody body0 = RigidBody.createIcosahedralSphere (
         "body0", 0.5, density, /*ndivs=*/2);
      FrameMarker mkr0 = mech.addFrameMarker (body0, new Point3d(0, 0, 0.5));
      body0.setPose (new RigidTransform3d (1, 0, -0.5,  0, -DTOR*60, 0));
      mech.addRigidBody (body0);

      RigidBody body1 = RigidBody.createIcosahedralSphere (
         "body1", 0.5, 1.5*density, /*ndivs=*/2);
      FrameMarker mkr1 = mech.addFrameMarker (body1, new Point3d(0, 0, 0.5));
      body1.setPose (new RigidTransform3d (-1, 0, -0.5,  0, DTOR*60, 0));
      mech.addRigidBody (body1);

      RigidBody body2 = RigidBody.createIcosahedralSphere (
         "body2", 0.5, 1.5*density, /*ndivs=*/2);
      FrameMarker mkr2 = mech.addFrameMarker (body2, new Point3d(0, 0, 0.5));
      body2.setPose (new RigidTransform3d (0, 0, 1.1,  0, -DTOR*180, 0));
      mech.addRigidBody (body2);

      Particle p0 = new Particle ("p0", particleMass, 0, 0, 0);
      mech.addParticle (p0);

      Muscle mus0 = attachMuscle ("mus0", mech, p0, mkr0);
      Muscle mus1 = attachMuscle ("mus1", mech, p0, mkr1);
      Muscle mus2 = attachMuscle ("mus2", mech, p0, mkr2);

      RenderProps.setFaceColor (body0, new Color(0f, 0.4f, 0.8f));
      RenderProps.setFaceColor (body1, new Color(0f, 0.8f, 0.4f));
      RenderProps.setFaceColor (body2, new Color(0f, 0.8f, 0.8f));
      RenderProps.setSpindleLines (mech, 0.05, Color.RED);
      RenderProps.setSphericalPoints (mech, 0.1, Color.WHITE);
      RenderProps.setSphericalPoints (mkr0, 0.1, Color.WHITE);
      RenderProps.setSphericalPoints (mkr1, 0.1, Color.WHITE);
      RenderProps.setSphericalPoints (mkr2, 0.1, Color.WHITE);

      mech.setDefaultCollisionBehavior (true, 0);

      ControlPanel panel = new ControlPanel();
      panel.addWidget ("stiffness", "material.stiffness", mus0, mus1, mus2);
      panel.addWidget ("stiffness 0", "material.stiffness", mus0);
      panel.addWidget ("stiffness 1", "material.stiffness", mus1);
      panel.addWidget ("stiffness 2", "material.stiffness", mus2);
      panel.addWidget ("excitation", "excitation", mus0, mus1, mus2);
      panel.addWidget ("inertialDamping", body0, body1, body2);
      panel.addWidget ("body 0 color", "renderProps.faceColor", body0);
      panel.addWidget ("body 1 color", "renderProps.faceColor", body1);
      panel.addWidget ("body 2 color", "renderProps.faceColor", body2);
      panel.addWidget (
         "bodies visible", "renderProps.visible", body0, body1, body2);
      panel.addWidget (
         "bodies color", "renderProps.faceColor", body0, body1, body2);
      panel.addWidget (
         "marker color", "renderProps.pointColor", mkr0, mkr1, mkr2);
      addControlPanel (panel);
   }

}
