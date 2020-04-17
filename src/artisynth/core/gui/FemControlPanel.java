/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import java.awt.Color;
import java.awt.Point;

import maspack.render.RenderProps;
import maspack.widgets.DoubleFieldSlider;
import maspack.widgets.LabeledComponentBase;
import artisynth.core.femmodels.FemModel;
import artisynth.core.femmodels.FemMuscleModel;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.mechmodels.MuscleExciter;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.probes.PlotTraceInfo;
import artisynth.core.workspace.RootModel;

/**
 * Utility class for creating control panels associated with FemModels
 */
public class FemControlPanel {

   public static void addFemControls (
      ControlPanel controlPanel, FemModel femModel, ModelComponent topModel) {
      controlPanel.addWidget (femModel, "density", 100, 5000);
      controlPanel.addWidget (femModel, "particleDamping", 0, 20);
      controlPanel.addWidget (femModel, "stiffnessDamping", 0, 1);
      controlPanel.addWidget (topModel, "integrator");
      controlPanel.addWidget (topModel, "matrixSolver");
      //controlPanel.addWidget (topModel, "maxStepSize");
      controlPanel.addWidget (topModel, "gravity");
      controlPanel.addWidget (femModel, "volume");
      controlPanel.addWidget (femModel, "numInverted");
      controlPanel.addWidget (femModel, "material");
      controlPanel.addWidget (femModel, "incompressible");
   }

   public static void addFem3dControls(
      ControlPanel controlPanel, FemModel femModel, ModelComponent topModel) {
      addFemControls(controlPanel, femModel, topModel);
      // controlPanel.addWidget (femModel, "freeVolume");
      controlPanel.addWidget(femModel, "softIncompMethod");
      // controlPanel.addWidget (femModel, "hardIncompMethod");
      controlPanel.addWidget(femModel, "surfaceRendering");
      controlPanel.addWidget(femModel, "elementWidgetSize", 0, 1.0);
   }

   public static void addMuscleControls(
      ControlPanel controlPanel, FemModel femModel, ModelComponent topModel) {
      addFem3dControls(controlPanel, femModel, topModel);
      controlPanel.addWidget(femModel, "muscleMaterial");
   }

   public static void addBundleControls(
      ControlPanel panel, FemMuscleModel muscle) {
      for (MuscleBundle b : muscle.getMuscleBundles()) {
         LabeledComponentBase widget =
            panel.addWidget(b.getName(), b, "excitation");
         if (b.getRenderProps() != null) {
            widget.setLabelFontColor(b.getRenderProps().getLineColor());
         }
      }
   }

   public static void
      addExcitersToPanel(ControlPanel panel, FemMuscleModel fem) {
      for (MuscleExciter mex : fem.getMuscleExciters()) {
         DoubleFieldSlider slider = (DoubleFieldSlider)panel.addWidget(mex
            .getName(), fem, "exciters/" + mex.getNumber() + ":excitation",
            0, 1);
         slider.setRoundingTolerance(0.00001);
         slider.getLabel().setForeground(
            FemControlPanel.getMuscleExciterColor(mex.getNumber()));
      }
   }

   public static void addBundlesToPanel(ControlPanel panel, FemMuscleModel fem,
      boolean reColor) {
      int ncolors = PlotTraceInfo.numPalatteColors();
      ComponentList<MuscleBundle> bundles =
         fem.getMuscleBundles();
      for (int i = 0; i < bundles.size(); ++i) {
         MuscleBundle bundle = bundles.get(i);
         DoubleFieldSlider slider =
            (DoubleFieldSlider)panel.addWidget(
               bundle.getName(), fem, "bundles/"
                  + bundle.getNumber() + ":excitation", 0, 1);
         if (slider == null)
            continue;
         slider.setRoundingTolerance(0.00001);
         if (reColor) {
            slider.getLabel().setForeground(
               PlotTraceInfo.getPaletteColors()[i % ncolors]);
            RenderProps.setLineColor(
               bundles.get(i), PlotTraceInfo.getPaletteColors()[i % ncolors]);
            RenderProps.setFaceColor(
               bundles.get(i), PlotTraceInfo.getPaletteColors()[i % ncolors]);
         }
         else {
            if (bundles.get(i).getRenderProps() != null) {
               slider.getLabel().setForeground(
                  bundles.get(i).getRenderProps().getLineColor());
            }
         }
      }
   }

   public static ControlPanel createMusclePanel(RootModel root,
      FemMuscleModel fem, boolean addExciters) {
      String panelname;
      if (addExciters) {
         panelname = fem.getName() + " Muscle exciters";
      }
      else {
         panelname = fem.getName() + " Muscle bundles";
      }
      ControlPanel controlPanel = new ControlPanel(panelname, "LiveUpdate");
      controlPanel.setScrollable(true);
      if (addExciters) {
         addExcitersToPanel(controlPanel, fem);
      } else {
         addBundlesToPanel(controlPanel, fem, /* reset colors= */false);
      }
   
      // controlPanel.setVisible(true); -- set be set visible by addControlPanel
      root.addControlPanel(controlPanel);
      return controlPanel;
   }

   public static ControlPanel createMuscleBundlesPanel(
      RootModel root, FemMuscleModel fem) {
      return createMusclePanel(root, fem, /* exciters= */false);
   }

   public static ControlPanel createMuscleExcitersPanel(
      RootModel root, FemMuscleModel fem) {
      return createMusclePanel(root, fem, /* exciters= */true);
   }

   public static ControlPanel createControlPanel(RootModel root,
      FemMuscleModel fem, ModelComponent topModel) {
      ControlPanel controlPanel =
         new ControlPanel(fem.getName() + " Controls", "LiveUpdate");
      controlPanel.setScrollable(true);
      addMuscleControls(controlPanel, fem, topModel);
      controlPanel.addWidget(
         "elements visible", fem, "elements:renderProps.visible");
      controlPanel.addWidget(
         "muscles visisble", fem, "bundles:renderProps.visible");
      controlPanel.addWidget("nodes visible", fem, "nodes:renderProps.visible");
      controlPanel.addWidget("surface rendering", fem, "surfaceRendering");
      // controlPanel.setVisible(true); -- will be set visible by
      // addControlPanel
      root.addControlPanel(controlPanel);
      return controlPanel;
   }

   public static Color getMuscleExciterColor(int idx) {
      /* assume exciters are bilateral muscle groups */
      return FemControlPanel.getMuscleColor(idx * 2);
   }

   public static Color getMuscleColor(int idx) {
      int valididx = idx % myMuscleColors.length;
      // 21 muscle bundles, therefore use bright/dark colors for right/left pair
      if (valididx % 2 == 1) { // left side muscle, return color
         return myMuscleColors[(valididx - 1) / 2];
      } else { // right-side, return darker color
         return myMuscleColors[valididx / 2].darker();
      }
   }

   public static final Color[] myMuscleColors = new Color[] {
   new Color(255, 0, 0), new Color(0f, 0.5f, 0f), new Color(0, 0, 255),
   new Color(0, 255, 255), new Color(255, 0, 255),
   new Color(255, 140, 0), new Color(255, 150, 150),
   new Color(138, 43, 226), new Color(255, 222, 173),
   new Color(85, 107, 47), new Color(205, 92, 92),
   new Color(255, 218, 185), };



}
