package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.MooneyRivlinMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;

public class QuadraticLockingDemo extends RootModel {

   static double LENGTH = 1.0; // 1.0;
   static double WIDTH = 1.0; // .20;
   static double DENSITY = 1000;
   static double EPS = 1e-8;

   static int NX = 5;
   static int NY = 5;
   static int NZ = 5;

   FemModel3d myHexMod; 
   FemModel3d myTetMod; 
   FemModel3d myQuadtetMod;
   
   MechModel myMechMod;   

   ControlPanel myControlPanel;

   private void setModelProperties (FemModel3d mod) {
      mod.setDensity (DENSITY);
      setRenderProperties (mod, LENGTH);

      mod.setMaterial (
         new MooneyRivlinMaterial (2000, 0, 0, 0, 0, 5000000));
      for (FemNode3d n : mod.getNodes()) {
         if (Math.abs(n.getPosition().z-LENGTH/2) < EPS) {
            n.setDynamic(false);
         }
      }
   }

   public void build (String[] args) {

      myTetMod = new FemModel3d ("tet");
      FemFactory.createTetGrid (
         myTetMod, WIDTH, WIDTH, LENGTH, NX, NY, NZ);

      myTetMod.transformGeometry (new RigidTransform3d (-3*WIDTH/4, 0, 0));
      setModelProperties (myTetMod);

      myQuadtetMod = new FemModel3d("quadtet");
      FemFactory.createQuadtetGrid (
         myQuadtetMod, WIDTH, WIDTH, LENGTH, NX, NY, NZ);
      setModelProperties (myQuadtetMod);
      myQuadtetMod.transformGeometry (new RigidTransform3d (3*WIDTH/4, 0, 0));
      
      myHexMod = new FemModel3d ("hex");
      FemFactory.createHexGrid (
         myHexMod, WIDTH, WIDTH, LENGTH, NX, NY, NZ);
      setModelProperties (myHexMod);
      myHexMod.transformGeometry (new RigidTransform3d (9*WIDTH/4, 0, 0));

      myMechMod = new MechModel ("mech");
      myMechMod.addModel (myTetMod);
      myMechMod.addModel (myQuadtetMod);
      myMechMod.addModel (myHexMod);

      addModel (myMechMod);

      addControlPanel();
   }


   public void setRenderProperties (FemModel3d mod, double length) {
      
      mod.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setShading (mod, Renderer.Shading.FLAT);
      RenderProps.setFaceColor (mod, new Color (0.7f, 0.7f, 0.9f));
      RenderProps.setLineWidth (mod.getElements(), 3);
      RenderProps.setLineColor (mod.getElements(), Color.blue);
      RenderProps.setPointRadius (mod, 0.02*length);
      RenderProps.setPointStyle (mod, Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (mod.getNodes(), Color.GREEN);
   }

   public void addControlPanel () {

      myControlPanel = new ControlPanel ("options", "LiveUpdate");

      myControlPanel.addWidget (
         "tetMaterial", myTetMod, "material");
      myControlPanel.addWidget (
         "tetSoftIncomp", myTetMod, "softIncompMethod");
      myControlPanel.addWidget (
         "quadtetMaterial", myQuadtetMod, "material");
      myControlPanel.addWidget (
         "quadtetSoftIncomp", myQuadtetMod, "softIncompMethod");
      myControlPanel.addWidget (
         "hexMaterial", myHexMod, "material");
      myControlPanel.addWidget (
         "hexSoftIncomp", myHexMod, "softIncompMethod");

      addControlPanel (myControlPanel);
      Main.getMain().arrangeControlPanels(this);
   }

}
