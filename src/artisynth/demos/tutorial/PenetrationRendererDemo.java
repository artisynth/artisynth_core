package artisynth.demos.tutorial;

import java.awt.Color;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.Renderer;
import maspack.render.RenderProps;
import artisynth.core.driver.Main;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;

public class PenetrationRendererDemo extends RootModel {

   public void build (String[] args) {

      // create MechModel and add to RootModel
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create and add the ball and plate
      RigidBody ball = RigidBody.createIcosahedralSphere ("ball", 0.8, 0.1, 1);
//      ball.setPose (new RigidTransform3d (0, 0, 2, 0.4, 0.1, 0.1));
      ball.setPose (new RigidTransform3d (0, 0, 0, 0.4, 0.1, 0.1));
      ball.setDynamic (false);
      mech.addRigidBody (ball);
      RigidBody plate = RigidBody.createBox ("plate", 5, 5, 5, 1);
      plate.setPose (new RigidTransform3d (0, 0, 2.94, 1, 0, 0, 0));
      plate.setDynamic (false);
      mech.addRigidBody (plate);

      // turn on collisions
      mech.setDefaultCollisionBehavior (true, 0.20);

      // make ball transparent so that contacts can be seen more clearly
      RenderProps.setFaceStyle (ball, RenderProps.Faces.NONE);
      RenderProps.setDrawEdges (ball, true);
      RenderProps.setEdgeColor (ball, Color.WHITE);
      
      RenderProps.setVisible (plate, false);
      RenderProps.setAlpha(plate, 0.5);

      // enable rendering of contacts normals and contours
      CollisionManager cm = mech.getCollisionManager();
      RenderProps.setVisible (cm, true);
      RenderProps.setLineWidth (cm, 3);      
      RenderProps.setLineColor (cm, Color.RED);
      RenderProps.setEdgeWidth (cm, 3);
      RenderProps.setEdgeColor (cm, Color.BLUE);
      cm.setContactNormalLen (0.5);
      cm.setDrawIntersectionContours (true);
      cm.setDrawIntersectionFaces (true);
      cm.setDrawIntersectionPoints (true);
      
      addMonitor (new PenetrationRenderer (cm.collisionHandlers ().get (0)));
   }
   
   @Override
   public void attach (DriverInterface driver) {
      super.attach (driver);     
      
      // set the default camera view to show the intersection
      setViewerEye (new Point3d(-1.64363, -3.53706, 3.008));
      setViewerCenter (new Point3d());
   }
   
}
