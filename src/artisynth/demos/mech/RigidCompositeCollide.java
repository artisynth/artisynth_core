package artisynth.demos.mech;

import java.awt.Color;
import maspack.matrix.*;
import maspack.render.*;
import maspack.geometry.*;
import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;

public class RigidCompositeCollide extends RootModel {

   public void build (String[] args) {

      // create MechModel and add to RootModel
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      mech.setGravity (0, 0, -98.0);

      // create and add the composite body and plate
      PolygonalMesh ball1 = MeshFactory.createIcosahedralSphere (0.8, 1);
      ball1.transform (new RigidTransform3d (1.5, 0, 0));
      PolygonalMesh ball2 = MeshFactory.createIcosahedralSphere (0.8, 1);
      ball2.transform (new RigidTransform3d (-1.5, 0, 0));
      PolygonalMesh axis = MeshFactory.createCylinder (0.2, 2.0, 12);
      axis.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));

      RigidBody body = new RigidBody ("body");
      body.setDensity (10);
      body.addMesh (ball1);
      body.addMesh (ball2);
      body.addMesh (axis);
      body.setPose (new RigidTransform3d (0, 0, 6, 0.4, 0.1, 0.1));
      mech.addRigidBody (body);

      RigidBody plate = RigidBody.createBox ("plate", 5, 5, 0.4, 1);
      plate.setDynamic (false);
      mech.addRigidBody (plate);

      // turn on collisions
      mech.setDefaultCollisionBehavior (true, 0.20);
   }
}
