package artisynth.demos.fem;

import java.io.IOException;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.workspace.RootModel;

public class RedundantCollisionTest extends RootModel {


   @Override
   public void build (String[] args) throws IOException {
      super.build (args);
      
      MechModel mech = new MechModel ("mech");
      addModel(mech);
      
      FemModel3d fem = FemFactory.createHexGrid (null, 1, 1, 1, 2, 2, 2);
      fem.setName ("fem");
      PolygonalMesh surface = fem.getSurfaceMesh ();
      surface = MeshFactory.subdivide (surface, 2);
      FemMeshComp fem_hires = fem.addMesh ("hires", surface);
      fem_hires.setCollidable (Collidability.EXTERNAL);
      mech.addModel (fem);
      
      PolygonalMesh box = MeshFactory.createBox (2, 2, 0.1);
      RigidBody table = new RigidBody("table");
      table.setMesh (box);
      table.transformGeometry (
         new RigidTransform3d(new Vector3d(0,0,-1), AxisAngle.IDENTITY));
      mech.addRigidBody (table);
      table.setDynamic (false);

      CollisionManager cm = mech.getCollisionManager();
      cm.setReduceConstraints (true);
      
      mech.setCollisionBehavior (fem_hires, table, true);
   }
   
}
