package artisynth.demos.mech;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.CollisionManager.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

/**
 * Demonstrates rigid body collision using vertex penetration constraints
 */
public class RigidVertexCollide extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (0.5, 2);
      RigidBody ball = RigidBody.createFromMesh ("ball", mesh, 1000.0, 1.0);
      ball.setPose (new RigidTransform3d (0, 0, 1.0, 0.01, 0.2, 0.0));


      RigidBody plate = RigidBody.createBox (
         "plate", 2.0, 2.0, 0.2, 10, 10, 1, 1000.0, false);
      plate.setDynamic (false);
      mech.addRigidBody (ball);
      mech.addRigidBody (plate);

      CollisionBehavior behav = new CollisionBehavior (true, 0.2);
      behav.setMethod (CollisionBehavior.Method.VERTEX_PENETRATION);
      mech.setCollisionBehavior (ball, plate, behav);

      CollisionManager cm = mech.getCollisionManager();
      cm.setReduceConstraints (true);
      cm.setColliderType (ColliderType.AJL_CONTOUR);
      // cm.setCompliance (0.00001);
      // cm.setDamping (10000.0);
      cm.setDrawContactNormals (true);
      RenderProps.setVisible (cm, true);
      RenderProps.setLineColor (cm, Color.RED);
      RenderProps.setLineWidth (cm, 3);

      // RenderProps.setDrawEdges (ball, true);
      // RenderProps.setDrawEdges (plate, true);
      // RenderProps.setFaceStyle (ball, FaceStyle.NONE);

      RenderProps.setFaceColor (ball, Color.CYAN);
   }

}
