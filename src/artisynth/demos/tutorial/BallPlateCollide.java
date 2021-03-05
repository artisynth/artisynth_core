package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;

public class BallPlateCollide extends RootModel {

   public void build (String[] args) {

      // create MechModel and add to RootModel
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create and add the ball and plate
      RigidBody ball = RigidBody.createIcosahedralSphere ("ball", 0.8, 0.1, 1);
      ball.setPose (new RigidTransform3d (0, 0, 2, 0.4, 0.1, 0.1));
      mech.addRigidBody (ball);
      RigidBody plate = RigidBody.createBox ("plate", 5, 5, 0.4, 1);
      plate.setDynamic (false);
      mech.addRigidBody (plate);

      // turn on collisions
      mech.setDefaultCollisionBehavior (true, 0.20);

      // make ball transparent so that contacts can be seen more clearly
      RenderProps.setFaceStyle (ball, Renderer.FaceStyle.NONE);
      RenderProps.setShading (ball, Renderer.Shading.NONE);
      RenderProps.setDrawEdges (ball, true);
      RenderProps.setEdgeColor (ball, Color.WHITE);

      // enable rendering of contacts normals and contours
      CollisionManager cm = mech.getCollisionManager();
      RenderProps.setVisible (cm, true);
      RenderProps.setLineWidth (cm, 3);      
      RenderProps.setLineColor (cm, Color.RED);
      RenderProps.setEdgeWidth (cm, 3);
      RenderProps.setEdgeColor (cm, Color.BLUE);
      cm.setDrawContactNormals (true);
      cm.setDrawIntersectionContours (true);

      // create a control panel to allow contact regularization to be set
      ControlPanel panel = new ControlPanel();
      panel.addWidget (mech.getCollisionManager(), "compliance");
      panel.addWidget (mech.getCollisionManager(), "damping");
      addControlPanel (panel);
   }
}
