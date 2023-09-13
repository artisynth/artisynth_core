package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearElasticContact;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.CollisionBehavior.ColorMapType;
import artisynth.core.mechmodels.CollisionBehavior.Method;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.util.PathFinder;

public class ElasticFoundationContact extends RootModel {

   public void build (String[] args) throws IOException {
      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -9.8);
      addModel (mech);

      // read in the mesh for the bowl
      PolygonalMesh mesh = new PolygonalMesh (
         PathFinder.getSourceRelativePath (
            ElasticFoundationContact.class, "data/bowl.obj"));

      // create the bowl from the mesh and make it non-dynamic
      RigidBody bowl =
         RigidBody.createFromMesh (
            "bowl", mesh, /*density=*/1000.0, /*scale=*/1.0);
      mech.addRigidBody (bowl);
      bowl.setDynamic (false);

      // create another spherical mesh to define the ball            
      mesh = MeshFactory.createIcosahedralSphere (0.7, 3);
      // create the ball from the mesh
      RigidBody ball =
         RigidBody.createFromMesh (
            "ball", mesh, /*density=*/1000.0, /*scale=*/1.0);
      // move the ball into an appropriate "drop" position
      ball.setPose (new RigidTransform3d (0.1, 0, 0));
      mech.addRigidBody (ball);            

      // Create a collision behavior that uses EFC. Set friction
      // to 0.1 so that the ball will actually roll.
      CollisionBehavior behav = new CollisionBehavior (true, 0.1);
      behav.setMethod (Method.VERTEX_PENETRATION); // needed for EFC
      // create the EFC and set it in the behavior
      LinearElasticContact efc =
         new LinearElasticContact (
            /*E=*/100000.0, /*nu=*/0.4, /*damping=*/0.1, /*thickness=*/0.1);
      behav.setForceBehavior (efc);

      // set the collision behavior between the ball and bowl
      mech.setCollisionBehavior (ball, bowl, behav);

      // contact rendering: render contact pressures
      CollisionManager cm = mech.getCollisionManager();
      cm.setDrawColorMap (ColorMapType.CONTACT_PRESSURE);
      RenderProps.setVisible (cm, true);
      // mesh rendering: render only edges of the bowl so we can see through it
      RenderProps.setFaceStyle (bowl, FaceStyle.NONE); 
      RenderProps.setLineColor (bowl, new Color (0.8f, 1f, 0.8f));
      RenderProps.setDrawEdges (mech, true); // draw edges for all meshes
      RenderProps.setFaceColor (ball, new Color (0.8f, 0.8f, 1f));

      // create a panel to allow control over some of the force behavior
      // parameters and rendering properties
      ControlPanel panel = new ControlPanel("options");
      panel.addWidget (behav, "forceBehavior");
      panel.addWidget (behav, "friction");
      panel.addWidget (cm, "colorMapRange");
      addControlPanel (panel);
   }

// may need to add this the build() method:
//    if (mech.getUseImplicitFriction()) {
//       mech.setCompliantContact();
//    }
}


