package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.fields.VectorNodalField;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

/**
 * Demonstrates creation and visualization of a simple VectorNodalField.
 */
public class VectorNodalFieldDemo extends RootModel {

   public void build (String[] args) {
      // create a MechModel to contain the FEM model
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create a simple hex grid FEM for the field
      FemModel3d fem = FemFactory.createHexGrid (
         null, 1.0, 1.0, 1.0, 5, 5, 5);
      fem.setMaterial (new LinearMaterial (10000, 0.49));
      fem.setName ("fem");
      mech.addModel (fem);
      // fix the top nodes of the FEM so it can deform under gravity
      for (FemNode3d n : fem.getNodes()) {
         if (n.getPosition().z == 0.5) {
            n.setDynamic (false);
         }
      }

      // create a VectorNodalField and add it to the FEM
      VectorNodalField<Vector3d> field = 
         new VectorNodalField<> (Vector3d.class, fem);
      fem.addField (field);

      // set field values to the vector between each node's initial position
      // and a reference position at the world origin
      Point3d refpos = new Point3d (0,0,0);
      Vector3d vec = new Vector3d();
      for (FemNode3d n : fem.getNodes()) {
         vec.sub (n.getPosition(), refpos);
         field.setValue (n, vec);
      }

      // create a control panel to adjust the render scale
      ControlPanel panel = new ControlPanel();
      panel.addWidget (field, "renderScale");
      addControlPanel (panel);

      // -- render properties --
      // render field values as blue arrows, with radius 0.015, scaled by 0.5
      // from their true value
      RenderProps.setSolidArrowLines (field, 0.015, new Color(0.2f, 0.6f, 1f));
      field.setRenderScale (0.5);
      // make FEM lines blue gray (for rendering element edges)
      RenderProps.setLineColor (fem, new Color (0.7f, 0.7f, 1f));
   }
}
