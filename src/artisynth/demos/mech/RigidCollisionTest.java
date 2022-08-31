package artisynth.demos.mech;

import java.awt.Color;
import java.util.ArrayList;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.RigidTransform3d;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidMeshComp;
import artisynth.core.renderables.TextLabeller3d;

public class RigidCollisionTest extends CollisionTestBase {

   RigidBody box0, box1, box2, box3, box4;

   ArrayList<RigidBody> boxes;

   
   public RigidCollisionTest() {
      super();
   }

   public RigidCollisionTest (String name) throws Exception {
      super (name);

      RigidTransform3d X = new RigidTransform3d();
      boxes = new ArrayList<RigidBody>();
     
      box0 = RigidBody.createBox ("box0", 1, 1, 1, 1000);
      X.p.set (0, 0, 0.5);
      box0.setPose (X);
      addBox (box0, Color.GREEN);
      boxes.add (box0);
      
      box1 = createCBox ("box1", 1, 1, 1, 1000);
      X.p.set (0, 0, 0.5);
      box1.setPose (X);
      addBox (box1, Color.BLUE);
      boxes.add (box1);
      
      TextLabeller3d labeller1 = new TextLabeller3d();
      for (Vertex3d vtx : table.getSurfaceMesh().getVertices()) {
         labeller1.addItem("" + vtx.getIndex(), vtx.getPosition(), table.getPose(), true);
      }
      labeller1.setTextSize(0.1);
      TextLabeller3d labeller2 = new TextLabeller3d();
      for (Vertex3d vtx : box0.getSurfaceMesh().getVertices()) {
         labeller2.addItem("" + vtx.getIndex(), vtx.getPosition(), box0.getPose(), true);
         
      }
      labeller2.setTextSize(0.1);
      addRenderable(labeller1);
      addRenderable(labeller2);
      
      mech.setCollisionBehavior (box0, table, true, 0.20);
      mech.setCollisionBehavior (box1, table, true, 0.20);


   }
   
   RigidBody createCBox (
      String bodyName, double wx, double wy, double wz, double density) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh = MeshFactory.createBox (wx, wy, wz);
      body.setInertiaFromDensity (density);
      RigidMeshComp rmc = body.addMesh(mesh, true, true);
      rmc.setName ("surface");
      return body;
   }

   
}
