package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemMuscleModel;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.MaterialBundle;
import artisynth.core.femmodels.Vector3dElementField;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.NeoHookeanMaterial;
import artisynth.core.materials.SimpleForceMuscle;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

public class RadialMuscle extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create a thin cylindrical FEM model with two layers along z
      double radius = 0.8;
      FemMuscleModel fem = new FemMuscleModel ("radialMuscle");
      mech.addModel (fem);
      fem.setDensity (1000);
      FemFactory.createCylinder (fem, radius/8, radius, 20, 2, 8);
      fem.setMaterial (new NeoHookeanMaterial (200000.0, 0.33));
      // fix the nodes close to the center
      for (FemNode3d node : fem.getNodes()) {
         Point3d pos = node.getPosition();
         double radialDist = Math.sqrt (pos.x*pos.x + pos.y*pos.y);
         if (radialDist < radius/2) {
            node.setDynamic (false);
         }
      }
      // compute a direction field, with the directions arranged radially
      Vector3d dir = new Vector3d();
      Vector3dElementField dirField = new Vector3dElementField (fem);
      for (FemElement3d elem : fem.getElements()) {
         elem.computeCentroid (dir);
         // set directions only for the upper layer elements
         if (dir.z > 0) {
            dir.z = 0; // remove z component from direction
            dir.normalize();
            dirField.setValue (elem, dir);
         }
      }
      fem.addField (dirField);
      // add a muscle material, and use it to hold a simple force
      // muscle whose 'restDir' property is attached to the field
      MaterialBundle bun = new MaterialBundle ("bundle",/*all elements=*/true);
      fem.addMaterialBundle (bun);
      SimpleForceMuscle muscleMat = new SimpleForceMuscle (500000);
      muscleMat.setRestDirField (dirField, true);
      bun.setMaterial (muscleMat);

      // add a control panel to control the excitation
      ControlPanel panel = new ControlPanel();
      panel.addWidget (bun, "material.excitation", 0, 1);
      addControlPanel (panel);

      // set some rendering properties
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem, new Color (0.6f, 0.6f, 1f));
   }
}
