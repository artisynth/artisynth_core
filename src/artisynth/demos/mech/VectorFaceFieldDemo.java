package artisynth.demos.mech;

import java.awt.Color;

import artisynth.core.fields.VectorFaceField;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.geometry.Face;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;

/**
 * Demonstrates creation and visualization of a simple VectorFaceField.
 */
public class VectorFaceFieldDemo extends RootModel {

   public void build (String[] args) {
      // create a MechModel to contain the field and its associated mesh
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // for the mesh, create an ellipsoid from an icosahedral sphere, and add
      // it to the MechModel as a FixedMeshBody
      PolygonalMesh mesh =
        MeshFactory.createIcosahedralSphere (0.5, /*divs=*/2);
      mesh.scale (1.0, 0.5, 2.0);
      FixedMeshBody mbody = new FixedMeshBody (mesh);
      mech.addMeshBody (mbody);

      // create a VectorFaceField and add it to the MechModel      
      VectorFaceField<Vector3d> field = 
         new VectorFaceField<> (Vector3d.class, mbody);
      mech.addField (field);

      // set field values to the normal at each face
      for (Face face : mesh.getFaces()) {
         field.setValue (face, face.getNormal());
      }

      // create a control panel to adjust the render scale
      ControlPanel panel = new ControlPanel();
      panel.addWidget (field, "renderScale");
      addControlPanel (panel);

      // -- render properties --
      // render field values as blue arrows, with radius 0.005, scaled by 0.5
      // from their true value
      RenderProps.setSolidArrowLines (field, 0.005, new Color (0.2f, 0.6f, 1f));
      field.setRenderScale (0.5);
      // render the mesh itself using only edges so it does not interfere with
      // field visualization
      RenderProps.setFaceStyle (mbody, FaceStyle.NONE);
      RenderProps.setDrawEdges (mbody, true);
   }
}
