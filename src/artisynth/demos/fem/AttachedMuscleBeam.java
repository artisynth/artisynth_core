package artisynth.demos.fem;

import java.awt.Point;
import java.util.*;
import java.io.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.util.*;
import maspack.widgets.DoubleFieldSlider;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.femmodels.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import artisynth.core.driver.*;

import java.awt.*;
import java.util.*;

public class AttachedMuscleBeam extends RootModel {

   static double myDensity = 1000;
   private double myYoungsModulus = 500000;
   private double myPoissonsRatio = 0.33;
   private double myParticleDamping = 2.0;
   private double myStiffnessDamping = 0.002;
   private boolean myIncompressible = false;

   Color PURPLE = new Color (153, 0, 204);

   private double EPS = 1e-9;

   private void setPointRenderProps (Renderable r, Color color) {
      RenderProps.setPointStyle (r, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (r, 0.02);
      RenderProps.setPointColor (r, color);
   }

   private AxialSpring createSpring (String name) {
      AxialSpring spring = new AxialSpring (name, 1000.0, 200, 0);
      RenderProps.setLineStyle (spring, Renderer.LineStyle.SPINDLE);
      RenderProps.setLineRadius (spring, 0.04);
      RenderProps.setLineColor (spring, Color.RED);
      return spring;
   }

   private AxialSpring createMuscle (String name) {
      AxialSpring spring = new Muscle (name);
      spring.setMaterial (new LinearAxialMuscle (1000.0, 200.0));
      RenderProps.setLineStyle (spring, Renderer.LineStyle.SPINDLE);
      RenderProps.setLineRadius (spring, 0.04);
      RenderProps.setLineColor (spring, Color.RED);
      return spring;
   }

   private FemModel3d createFem (String name) {
      FemModel3d fem = new FemModel3d (name);
      fem.setDensity (myDensity);
      fem.setParticleDamping (myParticleDamping);
      fem.setStiffnessDamping (myStiffnessDamping);
      fem.setLinearMaterial (myYoungsModulus, myPoissonsRatio, true);
      //fem.setPoissonsRatio (myPoissonsRatio);
      //fem.setYoungsModulus (myYoungsModulus);
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      setPointRenderProps (fem, PURPLE);
      RenderProps.setLineColor (fem, Color.BLUE);
      RenderProps.setFaceColor (fem, new Color (255, 153, 153));
      return fem;
   }

   private LinkedList<FemNode3d> getLeftNodes (FemModel3d fem) {
      double minx = Double.MAX_VALUE;
      for (FemNode3d n : fem.getNodes()) {
         if (n.getPosition().x < minx) {
            minx = n.getPosition().x;
         }
      }
      LinkedList<FemNode3d> nodes = new LinkedList<FemNode3d>();
      for (FemNode3d n : fem.getNodes()) {
         if (Math.abs(n.getPosition().x-minx) < EPS) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   private LinkedList<FemNode3d> getRightNodes (FemModel3d fem) {
      double maxx = Double.MIN_VALUE;
      for (FemNode3d n : fem.getNodes()) {
         if (n.getPosition().x > maxx) {
            maxx = n.getPosition().x;
         }
      }
      LinkedList<FemNode3d> nodes = new LinkedList<FemNode3d>();
      for (FemNode3d n : fem.getNodes()) {
         if (Math.abs(n.getPosition().x-maxx) < EPS) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   public void build (String[] args) {

      FemModel3d beam0 = FemFactory.createHexGrid (
         createFem ("beam0"), 1, 0.5, 0.5, 4, 4, 4);
      FemModel3d beam1 = FemFactory.createHexGrid (
         createFem ("beam1"), 1, 0.5, 0.5, 2, 2, 2);
      beam0.setIncompressible (FemModel.IncompMethod.AUTO);
      beam1.setIncompressible (FemModel.IncompMethod.AUTO);

      RigidBody block = RigidBody.createBox ("block", 0.2, 0.8, 0.8, 1000);
      block.transformGeometry (new RigidTransform3d (1.6, 0, 0));
      beam1.transformGeometry (new RigidTransform3d (1, 0, 0));

      for (FemNode3d n : getLeftNodes(beam0)) {
         n.setDynamic (false);
      }

      MechModel mechMod = new MechModel ("mech");
      //mechMod.setIntegrator (MechSystemSolver.Integrator.BackwardEuler);
      mechMod.setMaxStepSize (0.01);
      mechMod.addModel (beam0);
      mechMod.addModel (beam1);
      mechMod.addRigidBody (block);

      for (FemNode3d n : getRightNodes(beam0)) {
         mechMod.addAttachment (beam1.createPointAttachment (n, EPS));
      }

      for (FemNode3d n : getRightNodes(beam1)) {
         mechMod.attachPoint (n, block);
      }

      // create anchor points for springs
      Particle anchor1 = new Particle (0, 0.5,  0.5, 1);
      Particle anchor2 = new Particle (0, 0.5, -0.5, 1);
      anchor1.setDynamic (false);
      anchor2.setDynamic (false);
      setPointRenderProps (anchor1, Color.WHITE);      
      setPointRenderProps (anchor2, Color.WHITE);
      mechMod.addParticle (anchor1);
      mechMod.addParticle (anchor2);

      // attach these to the end nodes attached to the block
      FemMarker mkr1 = new FemMarker (1.25,  0.25, 0.20);
      FemMarker mkr2 = new FemMarker (1.25, -0.25, 0.20);
      beam1.addMarker (mkr1);
      beam1.addMarker (mkr2);
      setPointRenderProps (mkr1, Color.WHITE);      
      setPointRenderProps (mkr2, Color.WHITE);
      mechMod.add (mkr1);
      mechMod.add (mkr2);
      AxialSpring spring1 = createMuscle ("muscle1");
      AxialSpring spring2 = createMuscle ("muscle2");
      mechMod.attachAxialSpring (anchor1, mkr1, spring1);
      mechMod.attachAxialSpring (anchor2, mkr2, spring2);

      FemNode3d n1 = beam1.getNodes().getByNumber(26);
      FemNode3d n2 = beam1.getNodes().getByNumber(20);
      n1.setName ("n1");
      n2.setName ("n2");

      // attach these to the beam0 nodes attached to the beam1
      Particle p3 = new Particle ("p3", 20, 0.5,  0.25, 0.25);
      Particle p4 = new Particle ("p4", 20, 0.5, -0.25, 0.25);
      setPointRenderProps (p3, Color.WHITE);      
      setPointRenderProps (p4, Color.WHITE);
      mechMod.addParticle (p3);
      mechMod.addParticle (p4);
      AxialSpring spring3 = createSpring ("spring3");
      AxialSpring spring4 = createSpring ("spring4");
      mechMod.attachAxialSpring (anchor1, p3, spring3);
      mechMod.attachAxialSpring (anchor2, p4, spring4);

      FemNode3d n3 = beam1.getNodes().getByNumber(24);
      FemNode3d n4 = beam1.getNodes().getByNumber(18);
      n3.setName ("n3");
      n4.setName ("n4");
      mechMod.attachPoint (n3, p3);
      mechMod.attachPoint (n4, p4);

      // MooneyRivlinMaterial monMat = new MooneyRivlinMaterial();
      // monMat.setBulkModulus (15000000);
      // monMat.setC10 (150000);
      // beam1.setMaterial (monMat);

      // beam1.setSoftIncompMethod (IncompMethod.NODAL);

      addModel (mechMod);
   }

   // ControlPanel myControlPanel;

   // @Override
   // public void attach (DriverInterface driver) {
   //    super.attach (driver);
   //    JFrame frame = driver.getFrame();

   //    // FemModel3d beam = (FemModel3d)findComponent ("models/fem");
   //    FemModel3d beam0 =
   //       (FemModel3d)findComponent ("models/mech/models/fem0");
   //    FemModel3d beam1 =
   //       (FemModel3d)findComponent ("models/mech/models/fem1");
   //    Model mainMod = (Model)findComponent ("models/0");

   //    if (getControlPanels().size() == 0) {
   //       myControlPanel = new ControlPanel ("options", "LiveUpdate");
   //       FemModel3d.addControls (myControlPanel, beam0, mainMod);

   //       myControlPanel.pack();
   //       myControlPanel.setVisible (true);
   //       Point loc = frame.getLocation();
   //       myControlPanel.setLocation (loc.x + frame.getWidth(), loc.y);
   //       addControlPanel (myControlPanel);
   //    }
   // }

}
