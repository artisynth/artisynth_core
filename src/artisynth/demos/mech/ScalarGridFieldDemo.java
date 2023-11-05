package artisynth.demos.mech;

import artisynth.core.fields.ScalarGridField;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.ScalarGrid;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.render.RenderProps;
import maspack.render.Renderer.PointStyle;

/**
 * Demonstrates creation and visualization of a simple ScalarGridField.
 */
public class ScalarGridFieldDemo extends RootModel {

   public void build (String[] args) {
      // create a MechModel to contain the field
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create a 1.0 x 1.0 x 1.0 ScalarGrid, with resolution 5 x 5 x 5, and
      // use this to instantiate a ScalarGridField which is added to the
      // MechModel
      ScalarGrid grid = new ScalarGrid (
         new Vector3d (1, 1, 1), new Vector3i (5, 5, 5));
      ScalarGridField field = new ScalarGridField (grid);
      mech.addField (field);

      // set the field values to be the distance between each vertex and a
      // reference position at the origin
      Point3d refpos = new Point3d (0, 0, 0);
      for (int vi=0; vi<field.numVertices(); vi++) {
         Vector3d vpos = field.getVertexPosition (vi);
         field.setVertexValue (vi, vpos.distance(refpos));
      }

      // for SURFACE visualization, create two 1.0 x 1.0 planar meshes, with
      // triangle resolutions 20 x 20, at right angles to each other
      int res = 20;
      PolygonalMesh mesh0 = MeshFactory.createPlane (1.0, 1.0, res, res);
      PolygonalMesh mesh1 = MeshFactory.createPlane (1.0, 1.0, res, res);
      // rotate mesh1 from the x-y plane into the z-x plane
      mesh1.transform (new RigidTransform3d (0, -0.05, 0,  0, 0, Math.PI/2));
      
      // Add each plane to the MechModel as a FixedMeshBody, and then make the
      // bodies invisible (so they does not interfere with SURFACE 
      // visualization) and add them to the field as a render mesh.
      FixedMeshBody mcomp0 = new FixedMeshBody (mesh0);
      mech.addMeshBody (mcomp0);
      RenderProps.setVisible (mcomp0, false);
      field.addRenderMeshComp (mcomp0);         
      FixedMeshBody mcomp1 = new FixedMeshBody (mesh1);
      mech.addMeshBody (mcomp1);
      RenderProps.setVisible (mcomp1, false);
      field.addRenderMeshComp (mcomp1);

      // create a control panel to adjust various field properties
      ControlPanel panel = new ControlPanel();      
      panel.addWidget (field, "visualization");
      panel.addWidget (field, "renderRange");
      panel.addWidget (field, "colorMap");
      panel.addWidget (field, "renderGrid");
      panel.addWidget (field, "renderVertices");
      addControlPanel (panel);

      // -- render properties --
      // make the field itself visible because it is invisible by default
      RenderProps.setVisible (field, true);
      // initialize field visualization to POINT
      field.setVisualization (ScalarGridField.Visualization.POINT);
      // set field points to render as spheres with radii 0.02 (for POINT
      // visualization)
      RenderProps.setPointStyle (field, PointStyle.SPHERE);
      RenderProps.setPointRadius (field, 0.02);
   }
}
