package artisynth.demos.fem;

import maspack.matrix.*;
import maspack.geometry.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.ElementFilter;
import artisynth.core.materials.*;
import artisynth.core.workspace.RootModel;

public class FemPlaneCollide extends RootModel {

   public static FemModel3d fem;
   RigidBody plane;

   double mu = 0.1;
   double density = 1000;

   // exclude elements with nodes for which x < 0
   private class RightFilter implements ElementFilter {
      public boolean elementIsValid (FemElement e) {
         for (FemNode n : e.getNodes()) {
            if (n.getPosition().x < 0) {
               return false;
            }
         }
         return true;
      }
   }

   public void build (String[] args) {

      MechModel mech = new MechModel();
      addModel (mech);

      plane = RigidBody.createBox ("plane", 5, 3, 0.5, density);
      mech.addRigidBody (plane);
      plane.transformGeometry (new RigidTransform3d (0, 0, -2));
      plane.setDynamic (false);

      fem = new FemModel3d("fem");
      fem.setDensity (density);
      FemFactory.createHexGrid (fem, 2, 2, 2, 4, 4, 4);
      fem.setMaterial (new LinearMaterial (100000, 0.4));
      mech.addModel (fem);

      // create a surface mesh containing only the right nodes
      FemMeshComp mesh1 = new FemMeshComp (fem);
      mesh1.setName ("halfSurface");
      mesh1.createSurface (new RightFilter());
      mesh1.setSurfaceRendering (FemModel.SurfaceRender.Stress);
      fem.addMeshComp (mesh1);

      mech.setDefaultCollisionBehavior (true, mu);
   }
}
 
