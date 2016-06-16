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
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import artisynth.core.driver.*;

import java.awt.*;
import java.util.*;

public class AttachDemo extends RootModel {

   private static String ellipsoidMeshPath =
      "src/artisynth/core/femmodels/meshes/sphere2";

   public static boolean debug = false;

   // MechFemConnector myConnector;
   LinkedList<FemNode3d> myLeftNodes = new LinkedList<FemNode3d>();

   LinkedList<FemNode3d> myRightNodes = new LinkedList<FemNode3d>();

   static double myDensity = 1000;

   private double myYoungsModulus = 100000;

   private double myPoissonsRatio = 0.33;

   private double myParticleDamping = 2.0;

   private double myStiffnessDamping = 0.002;

   private IncompMethod myIncompressible = IncompMethod.OFF;

   private FemModel3d createEllipsoid (
      String name, double sx, double sy, double sz) {
      FemModel3d fem = createFem (name);
      String meshPath =
         ArtisynthPath.getHomeRelativePath (ellipsoidMeshPath, ".");
      fem.setIncompressible (myIncompressible);
      try {
         TetGenReader.read (
            fem, fem.getDensity(), meshPath + ".1.node", meshPath + ".1.ele",
            new Vector3d (sx, sy, sz));
      }
      catch (Exception e) {
         throw new InternalErrorException ("Can't create TetGen FEM from "
         + meshPath);
      }
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      return fem;
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
      RenderProps.setPointStyle (fem, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (fem, 0.02);
      RenderProps.setLineColor (fem, Color.BLUE);
      RenderProps.setPointColor (fem, new Color (153, 0, 204));
      RenderProps.setFaceColor (fem, new Color (255, 153, 153));
      return fem;
   }

   public void build (String[] args) {

      int nn = 2;
      double len0 = 0.8;
      double len1 = 0.6;

      FemModel3d femMod0 = createEllipsoid ("fem0", 0.8, 0.4, 0.4);
      FemModel3d femMod1 = createEllipsoid ("fem1", 0.6, 0.5, 0.4);
      // FemModel3d.createGrid (
      // "fem0", len0, 0.2, 0.2, nn * 2, nn * 1, nn * 1, myDensity);
      // FemModel3d femMod1 =
      // FemModel3d.createGrid (

      // "fem1", len1, 0.2, 0.2, 3, nn * 1, nn * 1, myDensity);

      RigidBody block = new RigidBody ("block");
      block.setInertiaFromDensity (2000);
      block.setMesh (MeshFactory.createBox (0.2, 0.8, 0.8), null);
      block.transformGeometry (new RigidTransform3d (2.15, 0, 0));

      femMod1.transformGeometry (new RigidTransform3d (1.4, 0, 0));
      // new RigidTransform3d (0.55, 0, -0.1));
      // femMod0.setBounds (new Point3d(-1, 0, 0), new Point3d(1, 0, 0));

      // fix the leftmost nodes
      double EPS = 1e-9;

      for (FemNode3d n : femMod0.getNodes()) {
         if (n.getPosition().x < -len0 / 2 + EPS) {
            myLeftNodes.add (n);
         }
         else if (n.getPosition().x > len0 / 2 - EPS) {
            myRightNodes.add (n);
         }
      }

      for (FemNode3d n : myLeftNodes) {
         n.setDynamic (false);
      }

      MechModel mechMod = new MechModel ("mech");
      mechMod.setIntegrator (MechSystemSolver.Integrator.BackwardEuler);
      mechMod.setMaxStepSize (0.01);
      mechMod.addModel (femMod0);
      mechMod.addModel (femMod1);
      mechMod.addRigidBody (block);
      addModel (mechMod);
      addControlPanel (mechMod);
   }

   ControlPanel myControlPanel;

   public void addControlPanel (MechModel mech) {
      myControlPanel = new ControlPanel ("options", "LiveUpdate");

      FemModel3d femMod0 =
         (FemModel3d)mech.findComponent ("models/fem0");     
      FemControlPanel.addFem3dControls (myControlPanel, femMod0, mech);
      addControlPanel (myControlPanel);
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "simple demo of a 3d fem";
   }
}
