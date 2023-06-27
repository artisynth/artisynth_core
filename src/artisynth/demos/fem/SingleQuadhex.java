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
import java.util.*;

import javax.swing.JFrame;

import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.matrix.*;

public class SingleQuadhex extends RootModel {
   FemModel3d mod;

   private double EPS = 1e-8;

   protected FemModel3d createSingleQuadhexFem() {

      FemModel3d fem = new FemModel3d();
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
         fem.addNode (nodes[i]);
      }
      FemNode3d[] edgeNodes = QuadhexElement.getQuadraticNodes (
         nodes[0], nodes[1], nodes[2], nodes[3],
         nodes[4], nodes[5], nodes[6], nodes[7]);
      for (int i=8; i<nodes.length; i++) {
         nodes[i] = edgeNodes[i-8];
         fem.addNode (nodes[i]);
      }
      
      QuadhexElement hex = new QuadhexElement (nodes);
      fem.addElement (hex);

      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setShading (fem, Renderer.Shading.FLAT);
      RenderProps.setFaceColor (fem, Color.PINK);
      RenderProps.setPointStyle (fem, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (fem, 0.05);

      LinearMaterial linMat = new LinearMaterial();
      linMat.setPoissonsRatio (0.0);
      //      fem.setMaterial (new StVenantKirchoffMaterial());
      // fem.setMaterial (new NeoHookeanMaterial());
      //fem.setMaterial (monMat);
      fem.setMaterial (linMat);
      fem.setDensity (10000);

      return fem;
   }

   public void build (String[] args) {

      FemModel3d fem = createSingleQuadhexFem();

      // FemNode3d dummy = new FemNode3d(0.0, 0.5, 0.01);
      // dummy.setDynamic(false);

      // mod.addNode(dummy);

      MechModel mech = new MechModel ("mech");
      //mech.setGravity (0, 0, 0);
      mech.addModel (fem);
      addModel (mech);

      IncompNeoHookeanMaterial inhMat = new IncompNeoHookeanMaterial();
      inhMat.setBulkModulus (30000);
      inhMat.setShearModulus (3000);
      MooneyRivlinMaterial monMat =
         new MooneyRivlinMaterial (30000.0, 0, 0, 0, 0, 5000000.0);
      //      fem.setMaterial (new StVenantKirchoffMaterial());
      // fem.setMaterial (new NeoHookeanMaterial());
      //fem.setMaterial (monMat);

      System.out.println ("mat=" + fem.getMaterial());
      //FemMarker mkr = new FemMarker (0, -1, 0);
      //fem.addMarker (mkr, fem.findContainingElement (mkr.getPosition()));

      // for (int i=0; i<nodes.length; i++) {
      //    if (nodes[i].getPosition().x <= -1+EPS) {
      //       nodes[i].setDynamic(false);
      //    }
      // }

      for (FemNode3d node : fem.getNodes()) {
         if (node.getPosition().z >= 1-EPS) {
            node.setDynamic(false);
         }
      }

      ArrayList<FemNode3d> mkrNodes = new ArrayList<>();
      mkrNodes.add (fem.getNode(0));
      mkrNodes.add (fem.getNode(8));
      mkrNodes.add (fem.getNode(11));
      mkrNodes.add (fem.getNode(16));

      mkrNodes.add (fem.getNode(9));
      mkrNodes.add (fem.getNode(10));
      mkrNodes.add (fem.getNode(19));
      mkrNodes.add (fem.getNode(15));
      mkrNodes.add (fem.getNode(12));
      mkrNodes.add (fem.getNode(17));

      mkrNodes.add (fem.getNode(1));
      mkrNodes.add (fem.getNode(2));
      mkrNodes.add (fem.getNode(3));
      mkrNodes.add (fem.getNode(7));
      mkrNodes.add (fem.getNode(4));
      mkrNodes.add (fem.getNode(5));

      FemMarker mkr = new FemMarker();
      double w0 = 5/9.0;
      double w1 = 2/9.0;
      double w2 = 1/9.0;
      double w3 = -1/18.0;
      mkr.setFromNodes (
         mkrNodes, new VectorNd (new double[] {
               w0, w1, w1, w1, w2, w2, w2, w2, w2, w2, w3, w3, w3, w3, w3, w3}));

      RenderProps.setPointColor (mkr, Color.RED);
      //fem.addMarker (mkr);

      double r = 0.444444444;
      //mkr = fem.addMarker (new Point3d (r, -r, -r));
      //RenderProps.setPointColor (mkr, Color.GREEN);

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

      createControlPanel (fem);

      fem.setSoftIncompMethod (IncompMethod.FULL);

      SolveMatrixTest tester = new SolveMatrixTest();
      System.out.println ("error=" + tester.testStiffness (fem, 1e-8));
      //System.out.println ("K=\n" + tester.getK().toString ("%10.1f"));
      //System.out.println ("N=\n" + tester.getKnumeric().toString ("%10.1f"));
      //System.out.println ("gravity weights=" + hex.computeGravityWeights().toString("%8.5f"));
      //System.out.println ("mass matrix=" + hex.computeConsistentMass().toString("%8.3f"));
   }

   private void createControlPanel(FemModel3d fem) {
      ControlPanel panel = new ControlPanel ("options");
      FemControlPanel.addFem3dControls (panel, fem, fem);
      panel.pack();
      addControlPanel (panel);      
      panel.setVisible (true);
      Main.getMain().arrangeControlPanels(this);

   }

}
