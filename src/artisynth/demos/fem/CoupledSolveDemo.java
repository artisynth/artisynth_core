package artisynth.demos.fem;

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputAdapter;

import maspack.interpolation.Interpolation;
import maspack.matrix.*;
import maspack.render.RenderProps;
import maspack.render.GL.GLViewer;
import maspack.util.*;
import maspack.widgets.DoubleFieldSlider;
import maspack.widgets.LabeledControl;
import maspack.widgets.PropertyWidget;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshFactory;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.*;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.Model;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.probes.WayPoint;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.TimeBase;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;

public class CoupledSolveDemo extends RootModel {
   public FemModel3d femMod;

   public RigidBody collider;

   protected ControlPanel myControlPanel;

   protected GLViewer myViewer;

   static boolean debug = false;

   double myScale = 1.0;

   public void addFemModel() {
      femMod = new FemModel3d ("FemMod");
      femMod.setDensity (1000.0);
      femMod.setParticleDamping (1.22);
      femMod.setStiffnessDamping (0.05);
      //femMod.setPoissonsRatio (0.0);
      //femMod.setYoungsModulus (12000.0);
      femMod.setLinearMaterial (12000.0, 0, true);
//       femMod.setMaterial (
//          new MooneyRivlinMaterial (6000.0, 0, 3000, 0, 0, 60000));

      FemFactory.createHexGrid (femMod, 0.025, 0.025, 0.05, 1, 1, 2);
      //FemFactory.createHexGrid (femMod, 0.025, 0.025, 0.05, 3, 3, 6);
      femMod.getSurfaceMesh();
      for (FemNode3d n : femMod.getNodes()) {
         if (femMod.isSurfaceNode (n) && n.getPosition().z < -0.024) {
            n.setDynamic (false);
            System.out.println ("fixing " + n.getNumber());
         }
      }

      MechModel mechMod = (MechModel)models().get (0);
      RigidBody collider = mechMod.rigidBodies().get ("Collider");
      mechMod.addModel (femMod);
      if (collider != null) {
         mechMod.setCollisionBehavior (femMod, collider, true);
         System.out.println ("enable collisions");
      }
      femMod.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setVisible (femMod.getElements(), true);
      RenderProps.setPointStyle (femMod, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (femMod, 0.0005);
//       for (FemNode3d n : femMod.getNodes()) {
//          if (femMod.isSurfaceNode (n)) {
//             RenderProps.setPointColor (n, new Color(0f,0.8f,0.8f));
//          }
//          else {
//             RenderProps.setVisible (n, false);
//          }
//       }
      
      RenderProps.setFaceColor (femMod, new Color(0.7f, 0.7f, 1f));
      RenderProps.setLineColor (femMod, new Color (0f,0f,0.4f));
      RenderProps.setLineWidth (femMod, 1);
      //RenderProps.setDrawEdges (femMod, true);

      removeAllControlPanels();
      ControlPanel panel = new ControlPanel ("FemMod options", "LiveUpdate");

      FemControlPanel.addFem3dControls (panel, femMod, mechMod);

      ControlPanel oldPanel = getControlPanels().get ("FemMod options");
      if (oldPanel != null) {
         removeControlPanel (oldPanel);
         oldPanel.dispose();
      }
      addControlPanel (panel);
   }

   public void addCollider() {
      MechModel mod = (MechModel)models().get (0);

      collider = new RigidBody ("Collider");
      PolygonalMesh colliderMesh = null;
      String meshFileName = null;

      colliderMesh = MeshFactory.createBox (0.05, 0.05, 0.03);
      collider.setPose (new RigidTransform3d (0, 0, 0.05));
      collider.setMesh (colliderMesh, meshFileName);
      collider.setInertiaFromDensity (10000);
      collider.setDynamic (false);

      RenderProps.setShading (collider, RenderProps.Shading.GOURAUD);
      RenderProps.setVisible (collider, true);
      RenderProps.setFaceStyle (collider, RenderProps.Faces.FRONT_AND_BACK);
      RenderProps.setFaceColor (collider, new Color(0.7f,0f,0f));

      RenderProps.setAlpha (collider, 1);
      RenderProps.setShading (collider, RenderProps.Shading.FLAT);

      mod.addRigidBody (collider);
      FemModel3d femMod = (FemModel3d)mod.models().get ("FemMod");
      mod.setCollisionBehavior (femMod, collider, true);
   }

   public void build (String[] args) {

      MechModel mechMod = new MechModel ("mechModel");
      mechMod.getCollisionManager().setPenetrationTol (1e-5);
      mechMod.setMaxStepSize (0.01);
      mechMod.setIntegrator (Integrator.ConstrainedBackwardEuler);
      addModel (mechMod);

      // add the FemMod
      addFemModel();
      addCollider();

      NumericInputProbe probe =
         new NumericInputProbe (collider, "position", 0, 5.0);
      probe.addData (new double[]
         { 0.0, 0, 0, 0.05,
           1.0, 0, 0, 0.01,
           1.5, 0, 0, 0.05,
           2.5, 0, 0, 0.01,
           3.0, 0, 0, 0.01,
           3.5, 0, 0, 0.05,
           4.5, 0, 0, 0.01,
           5.0, 0, 0, 0.05, },
           NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (probe);

      probe =
         new NumericInputProbe (collider, "velocity", 0, 5.0);
      probe.addData (new double[]
         { 0.0, 0, 0, -0.04, 0, 0, 0,
           1.0, 0, 0,  0.08, 0, 0, 0,
           1.5, 0, 0, -0.04, 0, 0, 0,
           2.5, 0, 0,  0.00, 0, 0, 0,
           3.0, 0, 0,  0.08, 0, 0, 0,
           3.5, 0, 0, -0.04, 0, 0, 0, 
           4.5, 0, 0,  0.08, 0, 0, 0,
           5.0, 0, 0,  0.00, 0, 0, 0,
         },
           NumericInputProbe.EXPLICIT_TIME);
      probe.setInterpolationOrder (Interpolation.Order.Step);
      addInputProbe (probe);
      addBreakPoint (2.5);
      probe.setActive (true);
   }
}
