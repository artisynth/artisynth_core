package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.ScalarNodalField;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

public class VariableStiffness extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create regular hex grid FEM model
      FemModel3d fem = FemFactory.createHexGrid (
         null, 1.0, 0.25, 0.05, 20, 5, 1);
      fem.transformGeometry (new RigidTransform3d (0.5, 0, 0)); // shift right
      fem.setDensity (1000.0);
      mech.addModel (fem);
      
      // fix the left-most nodes
      double EPS = 1e-8;
      for (FemNode3d n : fem.getNodes()) {
         if (n.getPosition().x < EPS) {
            n.setDynamic (false);
         }
      }
      // create a scalar nodel field to make the stiffness vary
      // nonlinearly along the rest position x axis
      ScalarNodalField stiffnessField = new ScalarNodalField(fem, 0);
      for (FemNode3d n : fem.getNodes()) {
         double s = 10*(n.getRestPosition().x);
         double E = 100000000*(1/(1+s*s*s));
         stiffnessField.setValue (n, E);
      }
      fem.addField (stiffnessField);
      // create a linear material, bind its Youngs modulus property to the
      // field, and set the material in the FEM model
      LinearMaterial linearMat = new LinearMaterial (100000, 0.49);
      linearMat.setYoungsModulusField (stiffnessField, /*useRestPos=*/true);
      fem.setMaterial (linearMat);

      // set some render properties for the FEM model
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem, Color.CYAN);
   }
}
