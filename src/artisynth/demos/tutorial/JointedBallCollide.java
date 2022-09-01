package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.mechmodels.RigidMeshComp;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

/**
 * Extension of RigidBodyJoint that adds a ball to the tip of bodyA so that it
 * collides with bodyB.
 */
public class JointedBallCollide extends RigidBodyJoint {

   public void build (String[] args) {

      super.build (args); // build the RigidBodyJoint model

      // create a ball mesh
      PolygonalMesh ball = MeshFactory.createIcosahedralSphere (2.5, 1);
      // translate it to the tip of bodyA, add it to bodyA, and
      // make it blue gray
      ball.transform (new RigidTransform3d (5, 0, 0));
      RigidMeshComp mcomp = bodyA.addMesh (ball);
      RenderProps.setFaceColor (mcomp, new Color (.8f, .8f, 1f));

      // disable collisions for the main surface mesh of bodyA
      bodyA.getSurfaceMeshComp().setIsCollidable (false);
      // enable collisions between bodyA and bodyB
      mech.setCollisionBehavior (bodyA, bodyB, true);
   }

}
