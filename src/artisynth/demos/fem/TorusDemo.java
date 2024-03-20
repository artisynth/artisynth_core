package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.FemCutPlane;
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
import maspack.matrix.AffineTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.PointStyle;

/**
 * A 3D FEM torus, grounded near the top.
 */
public class TorusDemo extends RootModel {

   public void build (String[] args) {
      // create a MechModel to contain the FEM model
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create a simple hex grid FEM for the field
      FemModel3d fem = FemFactory.createHexTorus (
         null, 0.1, 0.0, 0.05, 10, 20, 3);
      fem.setMaterial (new LinearMaterial (10000, 0.49));
      fem.setName ("fem");
      mech.addModel (fem);

      // fix the top nodes of the FEM so it can deform under gravity
      for (FemNode3d n : fem.getNodes()) {
         Point3d pos = n.getPosition();
         if (pos.z > 0 && Math.abs(pos.x) < 1e-8) {
            n.setDynamic (false);
         }
      }

      fem.setElementWidgetSize (0.7);
      RenderProps.setFaceColor (fem, new Color (0.7f, 0.7f, 1f));
   }
}
