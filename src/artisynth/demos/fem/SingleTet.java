package artisynth.demos.fem;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.femmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.materials.*;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.driver.*;

import java.awt.Color;
import java.awt.Point;

import javax.swing.JFrame;

import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.matrix.*;

public class SingleTet extends RootModel {
   FemModel3d mod;
   MechModel mechMod;

   FemNode3d myN1;
   FemNode3d myN2;
   FemNode3d myN3;
   FemNode3d myN4;

   public void build (String[] args) {
      mod = new FemModel3d();

      myN1 = new FemNode3d (-1, -1, 1);
      myN2 = new FemNode3d (-1, 0, -1);
      myN3 = new FemNode3d (-1, 1, 1);
      myN4 = new FemNode3d (1, 0, 0);

      TetElement tet = new TetElement (myN1, myN2, myN3, myN4);

      mod.addNode (myN1);
      mod.addNode (myN2);
      mod.addNode (myN3);
      mod.addNode (myN4);

      mod.addElement (tet);

      //mod = FemFactory.subdivideFem (null, mod);

      myN1.setDynamic (false);
      myN2.setDynamic (false);
      myN3.setDynamic (false);

      // FemNode3d dummy = new FemNode3d(0.0, 0.5, 0.01);
      // dummy.setDynamic(false);

      // mod.addNode(dummy);

      mod.setSurfaceRendering (SurfaceRender.Shaded);

      //RenderProps.setShading (mod, RenderProps.Shading.GOURARD);
      RenderProps.setFaceColor (mod, Color.PINK);
      RenderProps.setShininess (mod, mod.getRenderProps().getShininess() * 10);
      RenderProps.setVisible (mod, true);
      RenderProps.setFaceStyle (mod, Renderer.FaceStyle.FRONT);

      MooneyRivlinMaterial monMat = new MooneyRivlinMaterial();
      monMat.setBulkModulus (15000000);
      monMat.setC10 (150000);
      monMat.setJLimit (0.2);
      QLVBehavior qlv = new QLVBehavior();
      qlv.setTau (0.1, 0.0, 0, 0, 0, 0);
      qlv.setGamma (4.0, 0, 0, 0, 0, 0);

      LinearMaterial linMat = new LinearMaterial (500000, 0.4);

      mod.setMaterial (new ViscoelasticMaterial (monMat, qlv));
      //mod.setMaterial (linMat);

      mechMod = new MechModel ("mech");
      mechMod.addModel (mod);

      addModel (mechMod);

      RenderProps.setPointStyle (mod.getNodes(), Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (mod.getNodes(), 0.05);

      mod.setGravity (0, 0, -9.8);
      mod.setDensity (10000);
      mod.setParticleDamping (0);

      createControlPanel (mod);

      //mechMod.setProfiling (true);
      //myN4.setPosition (-2, 0, 0);

      //testInvertedForces (tet);
   }


   MechModel getMechMod() {
      if (models().size() > 0 && models().get(0) instanceof MechModel) {
         return (MechModel)models().get(0);
      }
      else {
         return null;
      }
   }      

   public StepAdjustment advance (double t0, double t1, int flags) {
      MechModel mech = getMechMod();
      if (mech != null) {
         SolveMatrixTest tester = new SolveMatrixTest();
         System.out.println ("error=" + tester.testStiffness (mech, 1e-8));
      }
      return super.advance (t0, t1, flags);
   }

   private void createControlPanel(FemModel3d mod) {
      ControlPanel panel = new ControlPanel ("options");
      FemControlPanel.addFem3dControls (panel, mod, mod);
      panel.pack();
      addControlPanel (panel);      
      panel.setVisible (true);
      Main.getMain().arrangeControlPanels(this);

   }

}
