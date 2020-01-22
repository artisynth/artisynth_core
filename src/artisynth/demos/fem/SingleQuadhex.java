package artisynth.demos.fem;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel.IncompMethod;
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

public class SingleQuadhex extends RootModel {
   FemModel3d mod;

   private double EPS = 1e-8;

   public void build (String[] args) {
      mod = new FemModel3d();

      double[] coords = new double[]
         {
            -1, -1, -1, 
            -1, -1,  1, 
            -1,  1,  1,
            -1,  1, -1,

             1, -1, -1, 
             1, -1,  1, 
             1,  1,  1,
             1,  1, -1,
         };

      FemNode3d[] nodes = new FemNode3d[20];
      for (int i=0; i<8; i++) {
         nodes[i] = new FemNode3d (coords[i*3], coords[i*3+1], coords[i*3+2]);
         mod.addNode (nodes[i]);
      }
      FemNode3d[] edgeNodes = QuadhexElement.getQuadraticNodes (
         nodes[0], nodes[1], nodes[2], nodes[3],
         nodes[4], nodes[5], nodes[6], nodes[7]);
      for (int i=8; i<nodes.length; i++) {
         nodes[i] = edgeNodes[i-8];
         mod.addNode (nodes[i]);
      }
      
      QuadhexElement hex =
         new QuadhexElement (nodes);


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

      MechModel mechMod = new MechModel ("mech");
      mechMod.addModel (mod);
      addModel (mechMod);

      RenderProps.setPointStyle (mod, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (mod, 0.05);

      mod.setGravity (0, 0, -9.8);
      //mod.setGravity (0, 0, 0);

      LinearMaterial linMat = new LinearMaterial();
      linMat.setPoissonsRatio (0.0);
      IncompNeoHookeanMaterial inhMat = new IncompNeoHookeanMaterial();
      inhMat.setBulkModulus (30000);
      inhMat.setShearModulus (3000);
      MooneyRivlinMaterial monMat =
         new MooneyRivlinMaterial (30000.0, 0, 0, 0, 0, 5000000.0);
      //      mod.setMaterial (new StVenantKirchoffMaterial());
      // mod.setMaterial (new NeoHookeanMaterial());
      //mod.setMaterial (monMat);
      mod.setMaterial (linMat);
      mod.setDensity (10000);

      System.out.println ("mat=" + mod.getMaterial());
      //FemMarker mkr = new FemMarker (0, -1, 0);
      //mod.addMarker (mkr, mod.findContainingElement (mkr.getPosition()));

      // for (int i=0; i<nodes.length; i++) {
      //    if (nodes[i].getPosition().x <= -1+EPS) {
      //       nodes[i].setDynamic(false);
      //    }
      // }

      for (int i=0; i<nodes.length; i++) {
         if (nodes[i].getPosition().z >= 1-EPS) {
            nodes[i].setDynamic(false);
         }
      }

      // for (int i=0; i<nodes.length; i++) {
      //    Vector3d gforce = new Vector3d (0, 0, -9.8);
      //    FemNode3d n = nodes[i];
      //    if (hex.getLocalNodeIndex(n) < 8) {
      //       gforce.scale (-1.0*hex.getDensity());
      //    }
      //    else {
      //       gforce.scale (1.333333*hex.getDensity());
      //    }
      //    n.setExternalForce (gforce);
      // }

      // FemNode3d n4 = nodes[4];
      // FemNode3d n16 = nodes[16];
      // FemNode3d n15 = nodes[15];
      // FemNode3d n12 = nodes[12];
      // double base = -10000;
      // double sc = 0.25;
      // double se = 1.0;
      // n4.setExternalForce (new Vector3d (0, 0, sc*base));
      // n16.setExternalForce (new Vector3d (0, 0, se*base));
      // n15.setExternalForce (new Vector3d (0, 0, se*base));
      // n12.setExternalForce (new Vector3d (0, 0, se*base));

      createControlPanel (mod);

      mod.setSoftIncompMethod (IncompMethod.FULL);

      SolveMatrixTest tester = new SolveMatrixTest();
      System.out.println ("error=" + tester.testStiffness (mod, 1e-8));
      //System.out.println ("K=\n" + tester.getK().toString ("%10.1f"));
      //System.out.println ("N=\n" + tester.getKnumeric().toString ("%10.1f"));
      System.out.println ("gravity weights=" + hex.computeGravityWeights().toString("%8.5f"));
      //System.out.println ("mass matrix=" + hex.computeConsistentMass().toString("%8.3f"));
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
