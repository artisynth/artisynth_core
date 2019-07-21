package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.MaterialBundle;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.NeoHookeanMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;

public class MaterialBundleDemo extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create a fem model consisting of a thin sheet of hexes
      FemModel3d fem = FemFactory.createHexGrid (null, 1.0, 1.0, 0.1, 10, 10, 1);
      fem.setDensity (1000);
      fem.setMaterial (new LinearMaterial (10000, 0.45));
      mech.add (fem);
      // fix the left-most nodes:
      double EPS = 1e-8;
      for (FemNode3d n : fem.getNodes()) {
         if (n.getPosition().x <= -0.5+EPS) {
            n.setDynamic (false);
         }
      }
      // create a "spine" of stiffer elements using a MaterialBundle with a
      // stiffer material
      MaterialBundle bun =
         new MaterialBundle ("spine", new NeoHookeanMaterial (5e6, 0.45), false);
      for (FemElement3d e : fem.getElements()) {
         // use element centroid to determine which elements are on the "spine"
         Point3d pos = new Point3d();
         e.computeCentroid (pos);
         if (Math.abs(pos.y) <= 0.1+EPS) {
            bun.addElement (e);
         }
      }
      fem.addMaterialBundle (bun);

      // add a control panel to control both the fem and bundle materials,
      // as well as the fem and bundle widget sizes      
      ControlPanel panel = new ControlPanel();
      panel.addWidget ("fem material", fem, "material");
      panel.addWidget ("fem widget size", fem, "elementWidgetSize");
      panel.addWidget ("bundle material", bun, "material");
      panel.addWidget ("bundle widget size", bun, "elementWidgetSize");
      addControlPanel (panel);
      
      // set rendering properties, using element widgets
      RenderProps.setFaceColor (fem, new Color (0.7f, 0.7f, 1.0f));
      RenderProps.setFaceColor (bun, new Color (0.7f, 1.0f, 0.7f));
      bun.setElementWidgetSize (0.9);
      fem.setElementWidgetSize (0.8);
   }

}
