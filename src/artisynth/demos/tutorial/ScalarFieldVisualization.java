package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.fields.ScalarNodalField;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import artisynth.core.materials.LinearMaterial;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.PointStyle;

/**
 * Illustrates visualization of a scalar nodal field.
 */
public class ScalarFieldVisualization extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create a hex FEM cylinder to use for the field
      FemModel3d fem = FemFactory.createHexCylinder (
         null, /*height=*/1.0, /*radius=*/0.5, /*nh=*/5, /*nt=*/10);
      fem.setMaterial (new LinearMaterial (10000, 0.45));
      fem.setName ("fem");
      mech.addModel (fem);

      // fix the top nodes of the FEM
      for (FemNode3d n : fem.getNodes()) {
         if (n.getPosition().z == 0.5) {
            n.setDynamic (false);
         }
      }

      // create a scalar field whose value is r^2, where r is the radial
      // distance from FEM axis
      ScalarNodalField field = new ScalarNodalField (fem);
      fem.addField (field);
      for (FemNode3d n : fem.getNodes()) {
         Point3d pnt = n.getPosition();
         double rsqr = pnt.x*pnt.x + pnt.y*pnt.y;
         field.setValue (n, rsqr);
      }
      
      // create a square FEM mesh to provide a visualization surface, and
      // rotate it into the z-x plane.
      PolygonalMesh mesh = MeshFactory.createPlane (1.0, 1.0, 20, 20);
      mesh.transform (new RigidTransform3d (0, 0, 0,  0, 0, Math.toRadians(90)));
      FemMeshComp mcomp = fem.addMesh (mesh);
      RenderProps.setVisible (mcomp, false);

      // make the mesh invisible and make it a field render mesh
      field.setVisualization (ScalarNodalField.Visualization.SURFACE);
      field.addRenderMeshComp (mcomp);

      // create a control panel to set properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (field, "visualization");
      panel.addWidget (field, "renderRange");
      panel.addWidget (field, "colorMap");
      addControlPanel (panel);

      // set render properties
      RenderProps.setPointStyle (field, PointStyle.SPHERE);
      RenderProps.setPointRadius (field, 0.01);
      RenderProps.setLineColor (fem, new Color (0.7f, 0.7f, 1f));
   }
}
