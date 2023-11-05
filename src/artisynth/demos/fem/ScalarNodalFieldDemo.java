package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.fields.ScalarFemField;
import artisynth.core.fields.ScalarNodalField;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.PointStyle;

/**
 * Demonstrates creation and visualization of a simple ScalarNodalField.
 */
public class ScalarNodalFieldDemo extends RootModel {

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

      // create a ScalarNodalField and add it to the FEM
      ScalarNodalField field = new ScalarNodalField (fem);
      fem.addField (field);

      // set field values to the distance between each node's initial position
      // and a reference position at the world origin
      Point3d refpos = new Point3d (0,0,0);
      for (FemNode3d n : fem.getNodes()) {
         field.setValue (n, n.getPosition().distance(refpos));
      }

      // for SURFACE visualization, create two 1.0 x 1.0 planar meshes, with
      // triangle resolutions 40 x 40, at right angles to each other
      int res = 40;
      PolygonalMesh mesh0 = MeshFactory.createPlane (1.0, 1.0, res, res);
      PolygonalMesh mesh1 = MeshFactory.createPlane (1.0, 1.0, res, res);
      // rotate mesh1 from the x-y plane into the z-x plane
      mesh1.transform (new RigidTransform3d (0, -0.05, 0,  0, 0, Math.PI/2));

      // Add each planar mesh to the FEM, and then make its corresponding
      // FemMeshComp invisible (so it does not interfere with SURFACE
      // visualization) and add it to the field as a render mesh.
      FemMeshComp mcomp0 = fem.addMesh (mesh0);
      RenderProps.setVisible (mcomp0, false);
      field.addRenderMeshComp (mcomp0);      
      FemMeshComp mcomp1 = fem.addMesh (mesh1);
      RenderProps.setVisible (mcomp1, false);
      field.addRenderMeshComp (mcomp1);

      // create a control panel to adjust various field properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (field, "visualization");
      panel.addWidget (field, "renderRange");
      panel.addWidget (field, "colorMap");
      addControlPanel (panel);

      // -- render properties --
      // initialize field visualization to SURFACE
      field.setVisualization (ScalarFemField.Visualization.SURFACE);
      // set field points to render as spheres with radii 0.01 (for POINT
      // visualization)
      RenderProps.setPointRadius (field, 0.025);
      RenderProps.setPointStyle (field, PointStyle.SPHERE);
      // make FEM lines blue gray (for rendering element edges)
      RenderProps.setLineColor (fem, new Color (0.7f, 0.7f, 1f));
   }
}
