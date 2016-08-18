package artisynth.demos.fem;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.materials.*;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.driver.*;
import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.FemControlPanel;
//import artisynth.core.gui.widgets.MaterialPanel;



import java.awt.Color;
import java.awt.Point;

import javax.swing.JFrame;

import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.matrix.*;

public class SingleQuadpyramid extends RootModel {
   FemModel3d mod;

   private double EPS = 1e-8;

   public void build (String[] args) {
      mod = new FemModel3d();

      double[] coords = new double[]
         {
            -1, -1, -1, 
            -1,  1, -1,
            -1,  1,  1,
            -1, -1,  1, 
             1,  0,  0,
         };

      FemNode3d[] nodes = new FemNode3d[13];
      for (int i=0; i<5; i++) {
         nodes[i] = new FemNode3d (coords[i*3], coords[i*3+1], coords[i*3+2]);
         mod.addNode (nodes[i]);
      }
      FemNode3d[] edgeNodes = QuadpyramidElement.getQuadraticNodes (
         nodes[0], nodes[1], nodes[2], 
         nodes[3], nodes[4]);
      for (int i=5; i<nodes.length; i++) {
         nodes[i] = edgeNodes[i-5];
         mod.addNode (nodes[i]);
      }
      
      QuadpyramidElement pyramid =
         new QuadpyramidElement (nodes);


      mod.addElement (pyramid);

      // FemNode3d dummy = new FemNode3d(0.0, 0.5, 0.01);
      // dummy.setDynamic(false);

      // mod.addNode(dummy);

      mod.setSurfaceRendering (SurfaceRender.Shaded);

      RenderProps.setShading (mod, Renderer.Shading.FLAT);
      RenderProps.setFaceColor (mod, Color.PINK);
      RenderProps.setShininess (mod, mod.getRenderProps().getShininess() * 10);
      RenderProps.setVisible (mod, true);
      RenderProps.setFaceStyle (mod, Renderer.FaceStyle.FRONT);

      MechModel mechMod = new MechModel ("mech");
      mechMod.addModel (mod);
      addModel (mechMod);

      RenderProps.setPointStyle (mod, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (mod, 0.05);

      mod.setGravity (0, 0, -9.8);
      // mod.setGravity (0, 0, 0);

      LinearMaterial linMat = new LinearMaterial();
      IncompNeoHookeanMaterial inhMat = new IncompNeoHookeanMaterial();
      inhMat.setBulkModulus (30000);
      inhMat.setShearModulus (3000);
      MooneyRivlinMaterial monMat =
         new MooneyRivlinMaterial (30000.0, 0, 0, 0, 0, 5000000.0);
      //      mod.setMaterial (new StVenantKirchoffMaterial());
      // mod.setMaterial (new NeoHookeanMaterial());
      mod.setMaterial (monMat);
      // mod.setMaterial (linMat);
      mod.setDensity (1000);

      //FemMarker mkr = new FemMarker (0, -1, 0);
      //mod.addMarker (mkr, mod.findContainingElement (mkr.getPosition()));

      // for (int i=0; i<nodes.length; i++) {
      //    if (nodes[i].getPosition().x <= -1+EPS) {
      //       nodes[i].setDynamic(false);
      //    }
      // }

      mod.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, 1, 0, -Math.PI/2));

      for (int i=0; i<nodes.length; i++) {
         if (nodes[i].getPosition().z >= 1-EPS) {
            nodes[i].setDynamic(false);
         }
      }

      createControlPanel (mod);
      System.out.println ("gravity weights=" + pyramid.computeGravityWeights().toString("%8.3f"));
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
