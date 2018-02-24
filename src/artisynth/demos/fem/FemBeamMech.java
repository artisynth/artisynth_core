package artisynth.demos.fem;

import java.awt.Color;
import java.util.LinkedList;

import artisynth.core.femmodels.FemElement;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemMarker;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.FemControlPanel;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.probes.WayPoint;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;

public class FemBeamMech extends RootModel {
   MechModel myMechMod;

   FemModel3d myFemMod;

   LinkedList<FemNode3d> myLeftNodes = new LinkedList<FemNode3d>();

   static double myDensity = 1000;

   public static PropertyList myProps =
      new PropertyList (FemBeamMech.class, RootModel.class);

   static {
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private String femPath;

   private String modPath;

   public static String fempath =
      ArtisynthPath.getHomeRelativePath (
         "src/artisynth/core/femmodels/meshes/", ".");

   public void build (String[] args) {

      femPath = "models/mech/models/fem/";
      modPath = "models/mech/";

      int nn = 2;

      myFemMod =
         FemFactory.createTetGrid (
            null, 0.6, 0.2, 0.2, nn * 3, nn * 1, nn * 1);
      myFemMod.setName("fem");
      myFemMod.setDensity(myDensity);

      myFemMod.setBounds (new Point3d (-0.6, 0, 0), new Point3d (0.6, 0, 0));
      myFemMod.setLinearMaterial (60000, 0.33, true);

      myFemMod.setStiffnessDamping (0.002);
      myFemMod.setImplicitIterations (100);
      myFemMod.setImplicitPrecision (0.001);
      myFemMod.setSurfaceRendering (SurfaceRender.Shaded);

      Renderable elems = myFemMod.getElements();
      RenderProps.setLineWidth (elems, 2);
      RenderProps.setLineColor (elems, Color.BLUE);
      Renderable nodes = myFemMod.getNodes();
      RenderProps.setPointStyle (nodes, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (nodes, 0.005);
      RenderProps.setPointColor (nodes, Color.GREEN);
      // fix the leftmost nodes
      double EPS = 1e-9;

      for (FemNode3d n : myFemMod.getNodes()) {
         if (n.getPosition().x < -0.3 + EPS) {
            myLeftNodes.add (n);
         }
      }

      System.out.println ("fixed nodes:");
      for (FemNode3d n : myLeftNodes) {
         n.setDynamic (false);
      }

      RenderProps.setFaceColor (myFemMod, new Color (0.4f, 0.4f, 1.0f));
      myFemMod.setProfiling (true);

      RigidBody anchorBox = new RigidBody ("anchorBox");
      PolygonalMesh mesh = MeshFactory.createBox (0.1, 0.3, 0.3);
      anchorBox.setMesh (mesh, /* fileName= */null);
      RigidTransform3d X = new RigidTransform3d();
      X.p.set (-0.35, 0, 0);
      anchorBox.setPose (X);
      anchorBox.setDynamic (false);

      myMechMod = new MechModel ("mech");
      myMechMod.addModel (myFemMod);
      myMechMod.addRigidBody (anchorBox);
      System.out.println ("models: " + myMechMod.findComponent ("models"));
      System.out.println ("models/fem: "
      + myMechMod.findComponent ("models/fem"));
      myMechMod.setIntegrator (Integrator.BackwardEuler);
      addModel (myMechMod);
      myMechMod.setProfiling (true);

      // add marker to lower right corner element
      Point3d corner = new Point3d (0.3, -0.1, -0.1);
      FemElement cornerElem = null;
      for (FemElement e : myFemMod.getElements()) {
         FemNode[] nodeList = e.getNodes();
         for (int i = 0; i < nodeList.length; i++) {
            if (nodeList[i].getPosition().epsilonEquals (corner, 1e-8)) {
               cornerElem = e;
               break;
            }
         }
      }
      if (cornerElem != null) {
         FemMarker mkr = new FemMarker (0.3, -0.07, -0.03);
         myFemMod.addMarker (mkr, cornerElem);
         RenderProps.setPointStyle (mkr, Renderer.PointStyle.SPHERE);
         RenderProps.setPointRadius (mkr, 0.01);
         RenderProps.setPointColor (mkr, Color.WHITE);

         Particle part = new Particle (1, 0.5, -0.07, -0.03);
         RenderProps.setPointStyle (part, Renderer.PointStyle.SPHERE);
         RenderProps.setPointRadius (part, 0.01);
         part.setDynamic (false);
         myMechMod.addParticle (part);

         AxialSpring spr = new AxialSpring (1000, 0, 0);
         myMechMod.attachAxialSpring (part, mkr, spr);
         RenderProps.setLineStyle (spr, Renderer.LineStyle.SPINDLE);
         RenderProps.setLineRadius (spr, 0.01);
         RenderProps.setLineColor (spr, Color.GREEN);
         
      }

      int numWays = 0;
      double res = 0.2;
      for (int i = 0; i < numWays; i++) {
         addWayPoint (new WayPoint ((i + 1)*res, true));
      }
      addControlPanel (myMechMod, myFemMod);
   }

   ControlPanel myControlPanel;

   public void addControlPanel (MechModel mechMod, FemModel3d femMod) {

      myControlPanel = new ControlPanel ("options", "");
      FemControlPanel.addFemControls (myControlPanel, femMod, mechMod);
      addControlPanel (myControlPanel);
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "simple demo of a 3d fem";
   }
}
