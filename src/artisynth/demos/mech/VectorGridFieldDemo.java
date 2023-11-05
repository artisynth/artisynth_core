package artisynth.demos.mech;

import java.awt.Color;

import artisynth.core.fields.VectorGridField;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.geometry.VectorGrid;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.render.RenderProps;

/**
 * Demonstrates creation and visualization of a simple VectorGridField.
 */
public class VectorGridFieldDemo extends RootModel {

   public void build (String[] args) {
      // create a MechModel to contain the field
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create a 1.0 x 1.0 x 1.0 VectorGrid, with resolution 5 x 5 x 5, and
      // use this to instantiate a VectorGridField which is added to the
      // MechModel
      Vector3i res = new Vector3i (5, 5, 5);
      VectorGrid<Vector3d> grid = new VectorGrid<Vector3d> (
         Vector3d.class, new Vector3d (1, 1, 1), res, null);
      VectorGridField<Vector3d> field = new VectorGridField<Vector3d> (grid);
      mech.add (field);

      // set field values to the vector between each vertex position
      // and a reference position at the world origin
      Point3d refpos = new Point3d (0,0,0);
      Vector3d vec = new Vector3d();
      for (int vi=0; vi<grid.numVertices(); vi++) {
         vec.sub (field.getVertexPosition(vi), refpos);
         field.setVertexValue (vi, vec);
      }

      // create a control panel to adjust various field properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (field, "renderScale");
      panel.addWidget (field, "renderGrid");
      panel.addWidget (field, "renderVertices");
      addControlPanel (panel);

      // -- render properties --
      // make the field itself visible because it is invisible by default
      RenderProps.setVisible (field, true);
      // render field values as blue arrows, with radius 0.015, scaled by 0.5
      // from their true value
      RenderProps.setSolidArrowLines (field, 0.01, new Color(0.2f, 0.6f, 1f));
      field.setRenderScale (0.5);
      // set the edge color to render the grid itself in a different color
      RenderProps.setEdgeColor (field, Color.BLUE);
   }
}
