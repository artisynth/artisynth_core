package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.femmodels.FemCutPlane;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.fields.ScalarNodalField;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

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
      
      // create a FemCutPlane to provide the visualization surface, rotated
      // into the z-x plane.
      FemCutPlane cutplane = new FemCutPlane (
         new RigidTransform3d (0,0,0, 0,0,Math.toRadians(90)));
      fem.addCutPlane (cutplane);

      // set the field's visualization and the cut plane to it as a render mesh
      field.setVisualization (ScalarNodalField.Visualization.SURFACE);
      field.addRenderMeshComp (cutplane);

      // create a control panel to set properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (field, "visualization");
      panel.addWidget (field, "renderRange");
      panel.addWidget (field, "colorMap");
      addControlPanel (panel);

      // set render properties
      // set FEM line color to render edges blue grey:
      RenderProps.setLineColor (fem, new Color (0.7f, 0.7f, 1f));
      // make cut plane visible via its coordinate axes; make surface invisible
      // to avoid conflicting with field rendering:
      cutplane.setSurfaceRendering (SurfaceRender.None);
      cutplane.setAxisLength (0.4);
      RenderProps.setLineWidth (cutplane, 2);
      // for point visualization: render points as spheres with radius 0.01
      RenderProps.setSphericalPoints (field, 0.02, Color.GRAY); // color ignored
   }
}
