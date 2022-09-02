package artisynth.demos.fem;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.mechmodels.MechSystemSolver.*;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.demos.mech.CollisionTestBase;

public class EmbeddedCollisionTest extends CollisionTestBase {

   public EmbeddedCollisionTest () {
      super();
   }
   
   public EmbeddedCollisionTest(String name) {
      super(name);
      
      FemModel3d sphere = FemFactory.createEllipsoid (null, 1, 1, 1, 16, 5, 3);
      sphere.setName ("sphere");
      mech.addModel (sphere);
      
      PolygonalMesh osphere = MeshFactory.createOctahedralSphere (0.5, 2);
      FemMeshComp embedded = sphere.addMesh ("collision", osphere);
      embedded.setCollidable (Collidability.EXTERNAL);

      RenderProps.setAlpha (embedded, 0.4);
      
      RigidTransform3d X = new RigidTransform3d (new Vector3d(0,0,1), AxisAngle.IDENTITY);
      sphere.transformGeometry (X);
      
      mech.setCollisionBehavior (embedded, table, true, 0.2);
      //mech.setIntegrator (Integrator.Trapezoidal);
      //mech.setGravity (0, 0, -9);
   }
   
}
