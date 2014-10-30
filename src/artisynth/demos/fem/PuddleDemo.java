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
import maspack.util.*;
import maspack.widgets.DoubleFieldSlider;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import artisynth.core.driver.*;

import java.awt.*;
import java.util.*;

public class PuddleDemo extends RootModel {
   public static boolean debug = false;

   FemModel3d myFemMod;

   MechModel myMechMod;

   LinkedList<FemNode3d> myBottomNodes = new LinkedList<FemNode3d>();

   static boolean myConnectedP = true;

   static boolean myConnectedInit = false;

   static double myDensity = 1000;

   public PuddleDemo() {
      super (null);
   }

   public PuddleDemo (String name) {
      this();
      setName (name);

      int nn = 2;
      myFemMod =
         FemModel3d.createGrid (
            "fem", 0.6, 0.2, 0.2, nn * 3, nn * 1, nn * 1, myDensity);
      myFemMod.setBounds (new Point3d (-0.6, 0, 0), new Point3d (0.6, 0, 0));
      myFemMod.setStiffnessDamping (0.002);
      myFemMod.setLinearMaterial (5000, 0.33, true);
      Renderable elems = myFemMod.getElements();
      RenderProps.setLineWidth (elems, 2);
      RenderProps.setLineColor (elems, Color.BLUE);
      Renderable nodes = myFemMod.getNodes();
      RenderProps.setPointStyle (nodes, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (nodes, 0.005);
      RenderProps.setPointColor (nodes, Color.GREEN);
      // fix the leftmost nodes
      double EPS = 1e-9;

      for (FemNode3d n : myFemMod.getNodes()) {
         if (n.getPosition().z <= -0.1 + EPS) {
            myBottomNodes.add (n);
         }
      }

      double wx, wy, wz;
      double mass;
      RigidTransform3d X = new RigidTransform3d();
      PolygonalMesh mesh;

      myMechMod = new MechModel();
      wx = 0.8;
      wy = 0.3;
      wz = 0.1;
      RigidBody bottomBox = new RigidBody();
      mass = wx * wy * wz * myDensity;
      bottomBox.setInertia (SpatialInertia.createBoxInertia (
         mass, wx, wy, wz));
      mesh = MeshFactory.createBox (wx, wy, wz);
      // mesh.setRenderMaterial (Material.createSpecial (Material.GRAY));
      bottomBox.setMesh (mesh, /* fileName= */null);
      X.R.setIdentity();
      X.p.set (0, 0, -wz / 2 - 0.1);
      bottomBox.setPose (X);
      bottomBox.setDynamic (false);
      myMechMod.addRigidBody (bottomBox);

      for (FemNode3d n : myBottomNodes) {
         n.setDynamic (false);
      }

      // myFemMod.scaleDistance (5);
      // myFemMod.scaleMass (5);
      if (debug) {
         try {
            // ReaderTokenizer rtok =
            // new ReaderTokenizer (
            // new BufferedReader (
            // new FileReader (
            // ArtisynthPath.getHomeRelativeFile (
            // "src/artisynth/core/mechmodels/fem3dDemoInit.txt",
            // "."))));
            // rtok.wordChars ("./");
            // myFemMod.scan (rtok, this);

            IndentingPrintWriter pw =
               new IndentingPrintWriter (new FileWriter ("fem3dDemo.txt"));
            myFemMod.write (pw, new NumberFormat ("%10.4f"), this);
            pw.close();
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }

      addModel (myFemMod);
      addModel (myMechMod);
      addControlPanel (myMechMod, myFemMod);
   }

   ControlPanel myControlPanel;

   public void addControlPanel (MechModel mechMod, FemModel3d femMod) {

      myControlPanel = new ControlPanel ("options", "");
      FemControlPanel.addFem3dControls (myControlPanel, femMod, mechMod);
      addControlPanel (myControlPanel);
   }

   public void detach (DriverInterface driver) {
      super.detach (driver);
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "simple demo of a 3d fem";
   }
}
