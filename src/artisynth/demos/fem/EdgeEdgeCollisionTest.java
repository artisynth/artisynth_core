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
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.CollisionManager.ColliderType;
import artisynth.core.workspace.RootModel;
import maspack.collision.SurfaceMeshCollider;

public class EdgeEdgeCollisionTest extends RootModel {

   @Override
   public void build (String[] args) throws IOException {
      super.build (args);
      
      MechModel mech = new MechModel ("mech");
      addModel(mech);
      
      FemModel3d fem = FemFactory.createHexGrid (null, 1, 1, 1, 1, 1, 1);
      fem.setName ("fem");
      fem.transformGeometry (
         new RigidTransform3d(0, 0, 0, 0, 0, Math.PI/4));

      mech.addModel (fem);
      
      RigidBody box = RigidBody.createBox ("table", 2, 2, 0.5, /*density=*/1);
      box.transformGeometry (
         new RigidTransform3d(0.3, 0, -2, 0, Math.PI/4, 0));
      mech.addRigidBody (box);
      box.setDynamic (false);

      //SurfaceMeshCollider.doEdgeEdgeContacts = true;

      CollisionManager cm = mech.getCollisionManager();
      cm.setReduceConstraints (true);
      cm.setColliderType (ColliderType.AJL_CONTOUR);

      CollisionBehavior behav = new CollisionBehavior (true, 0);
      behav.setMethod (CollisionBehavior.Method.VERTEX_EDGE_PENETRATION);
      mech.setCollisionBehavior (fem, box, behav);
   }
   
}
