package artisynth.demos.fem;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.femmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.materials.IncompNeoHookeanMaterial;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.MooneyRivlinMaterial;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.driver.*;
import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.FemControlPanel;
//import artisynth.core.gui.widgets.MaterialPanel;

import java.awt.Color;

import javax.swing.JFrame;

import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.matrix.*;

public class SingleHex extends RootModel {
   FemModel3d mod;
   MechModel mechMod;

   public void build (String[] args) {
      mod = new FemModel3d();

      double[] coords = new double[]
         {
            -2, -2,  -2, 
             2, -2,  -2,
            -2,  2,  -2,
             2,  2,  -2,
            -2, -2,   2, 
             2, -2,   2,
            -2,  2,   2,
             2,  2,   2,
         };

      FemNode3d[] nodes = new FemNode3d[8];
      for (int i=0; i<8; i++) {
         nodes[i] = new FemNode3d (coords[i*3], coords[i*3+1], coords[i*3+2]);
         mod.addNode (nodes[i]);
      }
      HexElement hex =
         new HexElement (nodes[4], nodes[5], nodes[7], nodes[6], 
                         nodes[0], nodes[1], nodes[3], nodes[2]);

      mod.addElement (hex);

      // FemNode3d dummy = new FemNode3d(0.0, 0.5, 0.01);
      // dummy.setDynamic(false);

      // mod.addNode(dummy);

      mod.setSurfaceRendering (SurfaceRender.Shaded);

      RenderProps.setShading (mod, Renderer.Shading.FLAT);
      RenderProps.setFaceColor (mod, Color.PINK);
      RenderProps.setShininess (mod, mod.getRenderProps().getShininess() * 10);
      RenderProps.setVisible (mod, true);
      RenderProps.setFaceStyle (mod, Renderer.FaceStyle.FRONT);

      mechMod = new MechModel ("mech");
      mechMod.addModel (mod);
      mechMod.setIntegrator (
         MechSystemSolver.Integrator.ConstrainedBackwardEuler);

      addModel (mechMod);

      //addModel (mod);

      RenderProps.setPointStyle (mechMod, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (mechMod, 0.05);

      mod.setGravity (0, 0, 0);
      mod.setIncompressible (FemModel.IncompMethod.OFF);
      //mod.setGravity (0, 0, -1);

      LinearMaterial linMat = new LinearMaterial();
      linMat.setYoungsModulus (1000);
      linMat.setPoissonsRatio (0);
      IncompNeoHookeanMaterial inhMat = new IncompNeoHookeanMaterial();
      inhMat.setBulkModulus (30000);
      inhMat.setShearModulus (3000);
      MooneyRivlinMaterial monMat = new MooneyRivlinMaterial();
      monMat.setBulkModulus (15000000);
      monMat.setC10 (150000);
      monMat.setJLimit (0.2);
      // mod.setMaterial (new StVenantKirchoffMaterial());
      // mod.setMaterial (new NeoHookeanMaterial());
      mod.setMaterial (monMat);
      //mod.setMaterial (linMat);
      mod.setDensity (10000);

//       nodes[0].setDynamic(false);
//       nodes[3].setDynamic(false);

      FemMarker mkr = new FemMarker (0, -1, 0);
      mod.addMarker (mkr, mod.findContainingElement (mkr.getPosition()));

      Point pnt = new Point (new Point3d(0, -1, 0));
      mechMod.add (pnt);
      mechMod.attachPoint (pnt, mod);

//       nodes[1].setPosition (-1, -1, -0.5);
//       nodes[5].setPosition ( 1, -1, -0.5);
//       nodes[7].setPosition ( 1,  1, -0.5);
//       nodes[3].setPosition (-1,  1, -0.5);


      // nodes[1].setPosition (1.1, -1, 1);
      // nodes[5].setPosition (1.1, 1, 1);
      // nodes[7].setPosition (1.1, -1, -1);
      // nodes[3].setPosition (1.1, 1, -1);


      if (true) {
         // fix the top nodes
         for (int i=4; i<8; i++) {
            nodes[i].setDynamic (false);
         }
      }

      if (false) {
         // fix the side nodes
         nodes[0].setDynamic(false);
         nodes[2].setDynamic(false);
         nodes[4].setDynamic(false);
         nodes[6].setDynamic(false);
      }
      

      // nodes[1].setPosition (-0.40, -2.13, -5.86);
      // nodes[5].setPosition (3.73, -2.02, -3.56);
      // nodes[7].setPosition (3.73,  2.02, -3.56);
      // nodes[3].setPosition (-0.40,  2.13, -5.86);

      // set to invert elements

      // nodes[1].setPosition (-3.001, -2, -2);
      // nodes[5].setPosition (-3.001, -2,  2);
      // nodes[7].setPosition (-3.001,  2,  2);
      // nodes[3].setPosition (-3.001,  2, -2);

      createControlPanel (mechMod, mod);
      mod.setSoftIncompMethod (IncompMethod.AUTO);

      SolveMatrixTest tester = new SolveMatrixTest();
      //System.out.println ("error=" + tester.testStiffness (mod, 1e-8));
      //System.out.println ("K=\n" + tester.getK().toString ("%10.1f"));
      //System.out.println ("N=\n" + tester.getKnumeric().toString ("%10.1f"));

      System.out.println ("gravity weights=" + hex.computeGravityWeights().toString("%8.3f"));
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
      // MechModel mech = getMechMod();
      // if (mech != null) {
      //    SolveMatrixTest tester = new SolveMatrixTest();
      //    System.out.println ("error=" + tester.testStiffness (mech, 1e-8));
      // }
      return super.advance (t0, t1, flags);
   }

   private void createControlPanel (MechModel mechMod, FemModel3d mod) {
      ControlPanel panel = new ControlPanel ("options");
      FemControlPanel.addFem3dControls (panel, mod, mechMod);
      panel.pack();
      addControlPanel (panel);      
      panel.setVisible (true);
      Main.getMain().arrangeControlPanels(this);

   }

}
