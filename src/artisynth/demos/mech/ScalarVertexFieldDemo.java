package artisynth.demos.mech;

import artisynth.core.fields.ScalarMeshField;
import artisynth.core.fields.ScalarVertexField;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.PointStyle;

/**
 * Demonstrates creation and visualization of a simple ScalarVertexField.
 */
public class ScalarVertexFieldDemo extends RootModel {

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

      // create a ScalarVertexField and add it to the MechModel
      ScalarVertexField field = new ScalarVertexField (mbody);
      mech.addField (field);

      // set field values to the distance between each vertex and a reference
      // position at (0, -0.5, 0)
      Point3d ref = new Point3d(0, -0.5, 0);
      for (Vertex3d vtx : mesh.getVertices()) {
         field.setValue (vtx, vtx.getPosition().distance(ref));
      }

      // create a control panel to adjust various field properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (field, "visualization");
      panel.addWidget (field, "renderRange");
      panel.addWidget (field, "colorMap");
      addControlPanel (panel);

      // -- render properties --
      // initialize field visualization to POINT
      field.setVisualization (ScalarMeshField.Visualization.POINT);      
      // set field points to render as spheres with radii 0.01 (for POINT
      // visualization)
      RenderProps.setPointRadius (field, 0.01);
      RenderProps.setPointStyle (field, PointStyle.SPHERE);
      // render the mesh itself using only edges so it does not interfere with
      // field visualization
      RenderProps.setFaceStyle (mbody, FaceStyle.NONE);
      RenderProps.setDrawEdges (mbody, true);
   }
}
