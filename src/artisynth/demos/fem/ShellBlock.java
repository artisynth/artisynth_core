package artisynth.demos.fem;

import java.awt.Point;
import java.awt.Color;
import java.util.*;
import java.io.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.render.Renderer.*;
import maspack.util.*;
import maspack.widgets.DoubleFieldSlider;
import maspack.spatialmotion.*;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.FemNodeNeighbor;
import artisynth.core.femmodels.ShellNodeFrameAttachment;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.*;
import artisynth.core.probes.WayPoint;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import artisynth.core.driver.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;

import java.util.*;

public class ShellBlock extends RootModel {
   double EPS = 1e-9;

   static double myDensity = 1000;
   static double myWidthX = 0.6;
   static double myWidthY = 0.2;

   LinkedList<FemNode3d> getLeftNodes (FemModel3d femMod) {
      LinkedList<FemNode3d> nodes = new LinkedList<FemNode3d>();
      for (FemNode3d n : femMod.getNodes()) {
         if (n.getPosition().x < -myWidthX/2 + EPS) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   LinkedList<FemNode3d> getRightNodes (FemModel3d femMod) {
      LinkedList<FemNode3d> nodes = new LinkedList<FemNode3d>();
      for (FemNode3d n : femMod.getNodes()) {
         if (n.getPosition().x > myWidthX/2 - EPS) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   public void build (String[] args) {
      boolean membrane = false;
      double thickness = 0.01;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-membrane")) {
            membrane = true;
         }
         else {
            System.out.println ("Warning: unknown model argument '"+args[i]+"'");
         }
      }
      build ("shellTri", 9, 3, thickness, membrane, 0);
   }

   public void build (
      String type, int nx, int ny,
      double thickness, boolean membrane, int options) {

      MechModel mechMod = new MechModel ("mech");
      addModel (mechMod);

      FemModel3d femMod = new FemModel3d ("fem");
      femMod.setDensity (myDensity);
      if (type.equals ("shellTri")) {
         FemFactory.createShellTriGrid (
            femMod, myWidthX, myWidthY, nx, ny, thickness, membrane);
      }
      else if (type.equals ("shellQuad")) {
         FemFactory.createShellQuadGrid (
            femMod, myWidthX, myWidthY, nx, ny, thickness, membrane);
      }
      else {
         throw new UnsupportedOperationException (
            "Unsupported element type " + type);
      }
         
      femMod.setLinearMaterial (2000000, 0.4, true);
      femMod.setSurfaceRendering (SurfaceRender.Shaded);

      femMod.setStiffnessDamping (0.002);
      Renderable elements = femMod.getElements();
      RenderProps.setLineWidth (elements, 2);
      RenderProps.setLineColor (elements, Color.BLUE);
      Renderable nodes = femMod.getNodes();
      RenderProps.setPointStyle (nodes, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (mechMod, 0.005);
      RenderProps.setPointColor (nodes, new Color (153, 0, 204));
      RenderProps.setFaceColor (femMod, new Color (0, 255, 200));
      RenderProps.setFaceStyle (femMod, FaceStyle.FRONT_AND_BACK);

      // find the left and right nodes

      LinkedList<FemNode3d> leftNodes = getLeftNodes (femMod);
      LinkedList<FemNode3d> rightNodes = getRightNodes (femMod);


      double wx, wy, wz;
      double mass;
      RigidTransform3d X = new RigidTransform3d();
      PolygonalMesh mesh;

      // mechMod.setPrintState ("%10.5f");
      wx = 0.1;
      wy = 0.3;
      wz = 0.15;
      RigidBody leftBox =
         RigidBody.createBox ("leftBox", wx, wy, wz, myDensity);
      leftBox.setPose (new RigidTransform3d (-(myWidthX+wx)/2, 0, 0));
      leftBox.setDynamic (true);
      mechMod.addRigidBody (leftBox);

      RenderProps.setPointStyle (mechMod, Renderer.PointStyle.SPHERE);

      RigidTransform3d TCW = new RigidTransform3d();
      TCW.setXyzRpyDeg (-wx-myWidthX/2, 0, wz/2,  0, 0, 90.0);
      HingeJoint joint = new HingeJoint (leftBox, TCW);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setShaftLength (0.5);
      joint.setShaftRadius (0.01);
      mechMod.addBodyConnector (joint);

      // right box
      RigidBody rightBox =
         RigidBody.createBox ("rightBox", wx, wy, wz, myDensity);
      rightBox.setPose (new RigidTransform3d ((myWidthX+wx)/2, 0, 0));
      rightBox.setDynamic (true);
      mechMod.addRigidBody (rightBox);

      mechMod.addModel (femMod);

      for (FemNode3d n : leftNodes) {
         mechMod.addAttachment (new ShellNodeFrameAttachment (n, leftBox));
      }

      for (FemNode3d n : rightNodes) {
         mechMod.addAttachment (new ShellNodeFrameAttachment (n, rightBox));
      }

      Frame frame = new Frame();
      frame.setAxisLength (0.1);
      RenderProps.setLineWidth (frame, 3);
      mechMod.add (frame);
      mechMod.attachFrame (frame, femMod);

      // RigidBody block = RigidBody.createBox (
      //    "block", 0.04, 0.04, 0.2, 1000.0);
      // mechMod.add (block);
      // mechMod.attachFrame (block, femMod);

      Particle p = new Particle ("marker", 0,  myWidthX/4, 0, 0);
      RenderProps.setPointColor (p, Color.RED);
      mechMod.add (p);
      mechMod.attachPoint (p, femMod);
      
   }                        

}
