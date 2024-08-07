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
import maspack.render.Renderer;
import maspack.util.*;
import maspack.widgets.DoubleFieldSlider;
import maspack.interpolation.Interpolation;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.femmodels.*;
import artisynth.core.materials.AxialMuscleMaterial;
import artisynth.core.materials.MooneyRivlinMaterial;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.*;
import artisynth.core.probes.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import artisynth.core.driver.*;

import java.awt.*;
import java.util.*;

public class TetCube extends RootModel {

   static double LENGTH = 1.0; // 1.0;
   static double WIDTH = 1.0; // .20;
   static double DENSITY = 1000;
   static double EPS = 1e-8;

   static int NX = 4;
   static int NY = 4;
   static int NZ = 4;

   FemModel3d myTetMod;   
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

      boolean printUsage = false;
      boolean quad = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-res")) {
            if (i == args.length-1) {
               System.out.println (
                  "WARNING: option '-res' requires another argument");
            }
            else {
               i++;
               int n = Integer.valueOf (args[i]);
               NX = NY = NZ = n;
            }
         }
         else if (args[i].equals ("-quad")) {
            quad = true;
         }
         else {
            System.out.println (
               "WARNING: unknown argument '"+args[i]+"'; ignoring");
            printUsage = true;
         }
      }
      if (printUsage) {
         System.out.println (
            "Options: [-res <n>] [-quad]");
      }

      myTetMod = new FemModel3d ("tet");
      if (quad) {
         FemFactory.createQuadtetGrid (
            myTetMod, WIDTH, WIDTH, LENGTH, NX, NY, NZ);
      }
      else {
         FemFactory.createTetGrid (
            myTetMod, WIDTH, WIDTH, LENGTH, NX, NY, NZ);
      }

      setModelProperties (myTetMod);

      myMechMod = new MechModel ("mech");
      myMechMod.addModel (myTetMod);

      addModel (myMechMod);

      addControlPanel();
      // myMechMod.setProfiling (true);
      // myTetMod.setIncompressible (IncompMethod.NODAL);
      // addBreakPoint (2.0);
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
         "volume", myTetMod, "volume");
      myControlPanel.addWidget (
         "incompressible", myTetMod, "incompressible");
      myControlPanel.addWidget (
         "incompCompliance", myTetMod, "incompCompliance");
      myControlPanel.addWidget (
         "softIncomp", myTetMod, "softIncompMethod");
      myControlPanel.addWidget (
         "material", myTetMod, "material");

      addControlPanel (myControlPanel);
      Main.getMain().arrangeControlPanels(this);
   }

}
