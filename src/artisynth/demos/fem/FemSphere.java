package artisynth.demos.fem;

import java.awt.Point;
import java.util.*;
import java.io.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.util.*;
import maspack.widgets.DoubleFieldSlider;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.TetGenReader;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.*;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.probes.WayPoint;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import artisynth.core.driver.*;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;

import java.awt.*;
import java.util.*;

public class FemSphere extends RootModel {
   MechModel myMechMod;

   FemModel3d myFemMod;

   static double myDensity = 1000;

   public static PropertyList myProps =
      new PropertyList (FemSphere.class, RootModel.class);

   static {
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private String femPath;

   private String modPath;

   public FemSphere() {
      super (null);
      femPath = "models/mech/models/fem/";
      modPath = "models/mech/";
   }

   public static String fempath =
      ArtisynthPath.getHomeRelativePath (
         "src/artisynth/core/femmodels/meshes/", ".");

   public FemSphere (String name) {
      this();
      setName (name);

      int nn = 3;

      try {
         myFemMod =
            TetGenReader.read (
               "fem", myDensity, fempath + "sphere2.1.node", fempath
               + "sphere2.1.ele", null);

         System.out.println ("femmod " + myFemMod);
      }
      catch (Exception e) {
         System.out.println (e.getMessage());
      }

      myFemMod.setBounds (new Point3d (-0.6, 0, 0), new Point3d (0.6, 0, 0));
      myFemMod.setLinearMaterial (200000, 0.4, true);
      myFemMod.setSurfaceRendering (SurfaceRender.Shaded);

      myFemMod.setStiffnessDamping (0.002);
      myFemMod.setImplicitIterations (100);
      myFemMod.setImplicitPrecision (0.001);

      Renderable elems = myFemMod.getElements();
      RenderProps.setLineWidth (elems, 2);
      RenderProps.setLineColor (elems, Color.BLUE);
      Renderable nodes = myFemMod.getNodes();
      RenderProps.setPointStyle (nodes, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (nodes, 0.005);
      RenderProps.setPointColor (nodes, Color.GREEN);
      // fix the leftmost nodes
      double EPS = 1e-9;

      myFemMod.setProfile (true);

      myMechMod = new MechModel ("mech");
      myMechMod.addModel (myFemMod);
      System.out.println ("models: " + myMechMod.findComponent ("models"));
      System.out.println ("models/fem: "
      + myMechMod.findComponent ("models/fem"));
      myMechMod.setIntegrator (Integrator.BackwardEuler);
      addModel (myMechMod);
      myMechMod.setProfiling (true);

      int numWays = 0;
      double res = 0.2;
      for (int i = 0; i < numWays; i++) {
         addWayPoint (new WayPoint ((i + 1)*res, true));
      }

   }

   ControlPanel myControlPanel;

   @Override
   public void attach (DriverInterface driver) {
      super.attach (driver);
      JFrame frame = driver.getFrame();

      myFemMod = (FemModel3d)findComponent ("models/mech/models/fem");
      myMechMod = (MechModel)findComponent ("models/mech");

//       if (getControlPanels().size() == 0) {
// //         myControlPanel = new ControlPanel ("options", "");
// //         DoubleFieldSlider ymSlider =
// //            (DoubleFieldSlider)myControlPanel.addWidget (
// //               myFemMod, "YoungsModulus", 100000, 1600000);
// //         ymSlider.setRoundingTolerance (10000);
// //         FemModel.addControls (myControlPanel, myFemMod, myMechMod);

//          myControlPanel.pack();
//          myControlPanel.setVisible (true);
//          Point loc = frame.getLocation();
//          myControlPanel.setLocation (loc.x + frame.getWidth(), loc.y);
//          addControlPanel (myControlPanel);
//       }
      // MatrixNd G = new MatrixNd();
      // myFemMod.divergenceConstraintMatrix (G);
      // System.out.println ("G=\n" + G.toString("%9.6f"));

      try {
         NumericOutputProbe collector =
            new NumericOutputProbe (myFemMod, "volume", null, 0.01);
         collector.setDefaultDisplayRange (0, 5);
         collector.setStopTime (60);

         addOutputProbe (collector);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   @Override
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
