package artisynth.demos.mech;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.*;
import artisynth.core.probes.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.driver.*;
import maspack.render.*;

import java.awt.Color;
import java.io.*;

import javax.swing.JFrame;

public class RigidBodyDemo extends RootModel {
   public static boolean debug = false;

   /**
    * Controller that adjust the size of a rigid body mesh as simulation
    * proceeds. This is done by changing vertex positions directly.
    */
   private class MeshModifier extends ControllerBase {
      
      RigidBody myBody; // rigid body containing the mesh
      Point3d[] myInitialPos; // initial mesh vertex positions

      public MeshModifier (RigidBody body) {
         myBody = body;
         PolygonalMesh mesh = body.getMesh();
         // record the initial mesh vertex positions
         myInitialPos = new Point3d[mesh.getNumVertices()];
         for (int i=0; i<mesh.getNumVertices(); i++) {
            Vertex3d vtx = mesh.getVertices().get(i);
            myInitialPos[i] = new Point3d(vtx.pnt);
         }
         // set the mesh to be *not* fixed, so that the renderer will
         // know that the vertex positions might be changing
         mesh.setFixed (false);
      }

      public void apply (double t0, double t1) {
         PolygonalMesh mesh = myBody.getMesh();
         // adjust the mesh vertex positions by applying a scale factor
         // to the initial positions
         for (int i=0; i<mesh.getNumVertices(); i++) {
            Vertex3d vtx = mesh.getVertices().get(i);
            Point3d pos = new Point3d (myInitialPos[i]);
            pos.scale (1 + 0.5*Math.sin (t0));
            vtx.pnt.set (pos);
         }
         // tell the mesh that the vertex positions have been changed
         mesh.notifyVertexPositionsModified();
         // tell the rigid body to recompute the inertia 
         myBody.setInertiaFromDensity (myBody.getDensity());
      }
   }

   public RigidBodyDemo() {
      super (null);
   }

   public RigidBodyDemo (String name) {
      this();
      setName (name);

      MechModel msmod = new MechModel ("msmod");
      msmod.setGravity (0, 0, -4.9);
      // msmod.setRigidBodyDamper (new FrameDamper (1.0, 4.0));
      // msmod.setParticleDamper (new PointDamper (1.0));
      msmod.setFrameDamping (1.0);
      msmod.setRotaryDamping (4.0);
      msmod.setPointDamping (1.0);
      // msmod.setPrintState ("%10.6f");

      RigidTransform3d X = new RigidTransform3d();
      PolygonalMesh mesh;
      double wx, wy, wz;

      // // set view so tha points upwards
      // X.R.setAxisAngle (1, 0, 0, -Math.PI/2);
      // viewer.setTransform (X);

      // top box
      wx = 10;
      wy = 5;
      wz = 2;
      RigidBody topBox = new RigidBody();
      topBox.setInertia (
         SpatialInertia.createBoxInertia (10, wx, wy, wz));
      mesh = MeshFactory.createBox (wx, wy, wz);
      // mesh.getVertices().get(0).setColor (Color.RED);
      // mesh.getVertices().get(1).setColor (Color.BLUE);
      // mesh.getVertices().get(2).setColor (Color.GREEN);
      // mesh.setRenderMaterial (Material.createSpecial (Material.GRAY));
      topBox.setMesh (mesh, /* fileName= */null);
      X.R.setIdentity();
      X.p.set (0, 0, 9);
      topBox.setPose (X);
      topBox.setDynamic (false);

      // lower box
      wx = 5;
      wy = 4;
      wz = 2;
      RigidBody lowerBox = new RigidBody();
      lowerBox.setInertia (SpatialInertia.createBoxInertia (
         10, wx, wy, wz));
      mesh = MeshFactory.createBox (wx, wy, wz);
      // mesh.setRenderMaterial (Material.createSpecial (Material.GRAY));
      lowerBox.setMesh (mesh, /* fileName= */null);
      X.p.set (5, 0, 5);
      X.R.setAxisAngle (0, 1, 0, -Math.PI / 4);
      lowerBox.setPose (X);

      // // ball
      // RigidBody ball = new RigidBody();
      // double radius = 1.5;
      // ball.setSpatialInertia(
      // SpatialInertia.createSphereInertia(10, radius));
      // mesh = MeshFactory.createSphere (radius, 16);
      // mesh.setRenderMaterial (Material.createSpecial (Material.RED));
      // ball.setMesh (mesh, /*fileName=*/null);
      // X.p.set (5, 0, 0);
      // ball.setPose (X);

      FrameMarker at0 = new FrameMarker (1, 0, -1);
      FrameMarker at1 = new FrameMarker (-1, 0, -1);

      msmod.addRigidBody (topBox);
      msmod.addRigidBody (lowerBox);

      msmod.addFrameMarker (at0, topBox, null);
      msmod.addFrameMarker (at1, topBox, null);

      RenderProps.setPointRadius (msmod, 0.5);
      RenderProps.setPointStyle (msmod, RenderProps.PointStyle.SPHERE);

      FrameMarker at2 = new FrameMarker (2.5, -2, 1);
      FrameMarker at3 = new FrameMarker (2.5, 2, 1);
      msmod.addFrameMarker (at2, lowerBox, null);
      msmod.addFrameMarker (at3, lowerBox, null);

      Particle part1 = new Particle (1, 2.5, 0, 7.5);
      part1.setName ("red");
      RenderProps props = part1.createRenderProps();
      props.setPointColor (Color.RED);
      part1.setRenderProps (props);

      Particle part2 = (Particle)part1.copy (0, null);

      RenderProps.setLineRadius (msmod, 0.1);
      RenderProps.setLineColor (msmod, new Color (0.93f, 0.8f, 0.063f));
      RenderProps.setLineStyle (msmod, RenderProps.LineStyle.CYLINDER);

      AxialSpring spring1 = new AxialSpring (6, 5, 0);
      AxialSpring spring2 = new AxialSpring (4, 2, 0);
      AxialSpring spring3 = new AxialSpring (4, 2, 0);

      props = spring1.createRenderProps();
      props.setLineColor (Color.GREEN);
      spring1.setRenderProps (props);

      // msmod.addRigidBody (topBox);
      // msmod.addRigidBody (lowerBox);

      FrameMarker at4 = new FrameMarker (-2.5, -2, -1);
      FrameMarker at5 = new FrameMarker (-2.5, 2, -1);

      msmod.addFrameMarker (at4, lowerBox, null);
      msmod.addFrameMarker (at5, lowerBox, null);

      msmod.addParticle (part2);
      part2.setMass (10);
      msmod.attachPoint (part2, lowerBox, new Point3d (-2.5, 0, -1));

      msmod.addParticle (part1);
      msmod.attachAxialSpring (at0, at2, spring1);
      msmod.attachAxialSpring (at1, part1, spring2);
      msmod.attachAxialSpring (part1, at3, spring3);

      //part1.setDynamic(false);


      // Particle part3 = new Particle(1, -4, 0, 8);
      // Particle part4 = new Particle(1, -4, 0, 4);
      // Particle part5 = new Particle(1, -4, 0, 4);
      // part5.setMass (2);
      // AxialSpring spring4 = new AxialSpring (2, 5, 0);

      // part3.setDynamic(false);
      // msmod.addParticle (part3);
      // msmod.addParticle (part5);
      // msmod.addParticle (part4);
      // RenderProps.setPointColor (part5, Color.RED);
      // msmod.attachAxialSpring (part3, part4, spring4);
      // msmod.attachPoint (part5, part4);

      msmod.setBounds (new Point3d (0, 0, -10), new Point3d (0, 0, 10));

      msmod.scaleDistance (5);
      // msmod.scaleMass (5);

      // AffineTransform3d XT = new AffineTransform3d();
      // XT.setIdentity();
      // XT.applyScaling (1, 1, 1);
      // XT.getOffset().set (0, 0, -5);

      addModel (msmod);

      // AffineTransform3d XA = new AffineTransform3d();
      // XA.applyScaling (1, 1, 1.2);
      // lowerBox.transformGeometry (XA);
      // lowerBox.transformGeometry (XA);

      int numWays = 0;
      double res = 1;
      for (int i = 0; i < numWays; i++) {
         addWayPoint (new WayPoint ((i + 1)*res, true));
      }

      // msmod.initialize(0L);

      // lowerBox.setDynamic (false);
      // try {
      //    NumericInputProbe inprobe =
      //       new NumericInputProbe (
      //          msmod, "rigidBodies/1:pose", 0, 10);
      //    // inprobe.setDefaultDisplayRange (-10, 10);
      //    addInputProbe (inprobe);
      // }
      // catch (Exception e) {
      //    e.printStackTrace();
      // }

      ControlPanel panel = new ControlPanel ("options");
      panel.addWidget (this, "models/msmod:integrator");
      panel.addWidget (this, "models/msmod:maxStepSize");
      //panel.pack();
      //panel.setVisible (true);
      // java.awt.Point loc = Main.getFrame().getLocation();
      // panel.setLocation (
      //    loc.x + driver.getFrame().getWidth(), loc.y);
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);

      // addController (new MeshModifier (topBox));
   }

   // ControlPanel myControlPanel;

   // public void attach (DriverInterface driver) {
   //    // System.out.println();
   //    super.attach (driver);

   //    myControlPanel = new ControlPanel ("options");
   //    myControlPanel.addWidget (this, "models/msmod:integrator");
   //    myControlPanel.addWidget (this, "models/msmod:maxStepSize");
   //    myControlPanel.pack();
   //    //myControlPanel.setVisible (true);
   //    java.awt.Point loc = driver.getFrame().getLocation();
   //    myControlPanel.setLocation (
   //       loc.x + driver.getFrame().getWidth(), loc.y);
   //    addControlPanel (myControlPanel);
   // }

   // public void detach (DriverInterface driver) {
   //    super.detach (driver);
   //    // if (myControlPanel != null)
   //    // {
   //    // myControlPanel.setVisible(false);
   //    // myControlPanel = null;
   //    // }

   //    System.out.println ("Cleaning up Rigid Body Demo");
   // }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return artisynth.core.util.TextFromFile.getTextOrError (
         ArtisynthPath.getSrcRelativeFile (this, "RigidBodyDemo.txt"));
   }
}
