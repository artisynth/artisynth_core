package artisynth.demos.mech;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.materials.*;
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

public class BeamBodyCollide extends RootModel {
   public static boolean debug = false;

   Color myLinkColor = new Color (228/255f, 115/255f, 33/255f);
   Color myEdgeColor = new Color (144/255f, 52/255f, 0);

   private double DTOR = Math.PI/180;

   double lenx0 = 15;
   double leny0 = 15;
   double lenz0 = 1.5;
   double stiffness = 10;
   double density = 1;

   RigidBody myBase;
   MechModel myMech;

   DeformableBody createBody (
      double x, double y, double z, double roll, double pitch, double yaw) {

      double lenx1 = 2;
      double leny1 = 0.4;
      double lenz1 = 0.6;
      int nslices = 16; // number of slices for approximating a circle

      RigidTransform3d XMB = new RigidTransform3d();
      XMB.R.setAxisAngle (0, 1, 0, Math.PI/2);
      PolygonalMesh mesh =
         MeshFactory.createRoundedBox (
            lenx1, leny1, lenz1, 20, 4, 4, nslices / 2);
      mesh.transform (XMB);
      DeformableBody body = new BeamBody (mesh, density, lenx1, stiffness); 
      body.setMassDamping (1);
      body.setMaterial (new LinearMaterial (5*stiffness, 0.3));
      body.setPose (
         new RigidTransform3d (x, y, z, DTOR*roll, DTOR*pitch, DTOR*yaw));

      RenderProps.setFaceColor (body, myLinkColor);
      RenderProps.setEdgeColor (body, myEdgeColor);

      return body;
   }

   public void build (String[] args) {

      myMech = new MechModel ("myMech");
      addModel (myMech);

      // myMech.setProfiling (true);
      myMech.setGravity (0, 0, -9.8);
      // myMech.setRigidBodyDamper (new FrameDamper (1.0, 4.0));
      myMech.setFrameDamping (0.1);
      myMech.setRotaryDamping (0.3);
      myMech.setIntegrator (
         MechSystemSolver.Integrator.ConstrainedBackwardEuler);

      RigidTransform3d XMB = new RigidTransform3d();
      RigidTransform3d XLW = new RigidTransform3d();
      PolygonalMesh mesh;
      int nslices = 16; // number of slices for approximating a circle
      int nsegs = 16; // number of cylinder segments

      // // set view so tha points upwards
      // X.R.setAxisAngle (1, 0, 0, -Math.PI/2);
      // viewer.setTransform (X);

      RenderProps props;

      FrameMarker mk0 = new FrameMarker();
      props = mk0.createRenderProps();
      // props.setColor (Color.GREEN);
      props.setPointRadius (0.1);
      props.setPointStyle (Renderer.PointStyle.SPHERE);
      mk0.setRenderProps (props);

      double ks = 10;
      double ds = 10;

      // first link
      //DeformableBody link1 = createBody (0, 0, 1, 5, 10, 7);
      DeformableBody link1 = createBody (0, 0, 1, 5, 10, 7);
      myMech.addRigidBody (link1);
      //DeformableBody link2 = createBody (0, 0, 2, 90, 10, 5);
      DeformableBody link2 = createBody (0, 0, 2, 90, 10, 5);
      myMech.addRigidBody (link2);
      DeformableBody link3 = createBody (0, -0.2, 6, 60, 7, 15);
      myMech.addRigidBody (link3);

      RigidBody base = RigidBody.createBox ("base", 4, 2, 0.2, density);
      base.setDynamic (false);
      myMech.addRigidBody (base);

      RenderProps.setPointColor (myMech, Color.BLUE);
      RenderProps.setPointStyle (myMech, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (myMech, 0.05);

      addControlPanel (myMech);

      myMech.setDefaultCollisionBehavior (true, 0.2);
   }

   ControlPanel myControlPanel;

   public void addControlPanel (MechModel mech) {
      myControlPanel = new ControlPanel ("options", "");
      myControlPanel.addWidget (mech, "integrator");
      myControlPanel.addWidget (mech, "maxStepSize");
      addControlPanel (myControlPanel);
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      
      // int sec = (int)t1;
      // if (sec > 4 && sec%4 == 0) {
      //    DeformableBody bod = createBody (0, 0, 10, 90, 10, 5);
      //    myMech.addRigidBody (bod);
      // }
      return super.advance (t0, t1, flags);
   }

}
