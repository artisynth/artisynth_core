package artisynth.demos.tutorial;

import java.awt.Color;
import java.util.List;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.CollisionResponse;
import artisynth.core.mechmodels.ContactData;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.MonitorBase;
import artisynth.core.workspace.RootModel;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;

public class ContactForceMonitorSavable extends RootModel {

   public static class ContactMonitor extends MonitorBase {
      public ContactMonitor() {
      }

      public void apply (double t0, double t1) {
         CollisionResponse resp =
            (CollisionResponse)RootModel.getRoot(this).findComponent (
               "models/mech/collisionManager/responses/0");
         // get the contacts from the collision response and print their
         // positions and forces.
         List<ContactData> cdata = resp.getContactData();
         if (cdata.size() > 0) {
            System.out.println (
               "num contacts: "+ cdata.size() + ", time=" + t0);
            for (ContactData cd : cdata) {
               System.out.print (
                  " pos:   " + cd.getPosition0().toString("%8.3f"));
               System.out.println (
                  ", force: " + cd.getContactForce().toString("%8.1f"));
            }
         }
      }
   }

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create an FEM ball model
      FemModel3d ball =
         FemFactory.createIcosahedralSphere (
            null, /*radius=*/0.7, /*ndivisions=*/1, /*quality=*/2);
      ball.setName ("ball");
      ball.setSurfaceRendering (SurfaceRender.Shaded);
      mech.addModel (ball);
      // reposition the ball to an appropriate "drop" position
      ball.transformGeometry (new RigidTransform3d (-0.5, 0, 2.5, 0, 0.1, 0.1));

      // create an inclined plane for the ball to collide with
      RigidBody plane = RigidBody.createBox (
         "plane", 4.0, 2.5, 0.25, /*density=*/1000);
      plane.setPose (new RigidTransform3d (0, 0, 0, 0, Math.PI/5, 0));
      plane.setDynamic (false);
      mech.addRigidBody (plane);

      // Enable collisions between the ball and the plane. Specifying the ball
      // first means contact forces will be rendered in the direction acting on
      // the ball.
      mech.setCollisionBehavior (ball, plane, true, 0.3);
      mech.setCollisionResponse (ball, plane);

      // add a monitor to print out contact positions and forces
      addMonitor (new ContactMonitor());

      // Render properties: set the collision manager to render contact and
      // friction forces, with a scale factor of 0.0001
      CollisionManager cm = mech.getCollisionManager();
      cm.setDrawContactForces (true);
      cm.setDrawFrictionForces (true);
      cm.setContactForceLenScale (0.0001);
      RenderProps.setVisible (cm, true);
      RenderProps.setSolidArrowLines (cm, 0.02, Color.BLUE);
      // Render properties: for the ball, make the elements invisible, and
      // render its surface as a wire frame to make it easy to see through
      RenderProps.setVisible (ball.getElements(), false);
      RenderProps.setLineColor (ball, new Color (.8f, .8f, 1f));
      RenderProps.setFaceStyle (ball, FaceStyle.NONE);
      RenderProps.setDrawEdges (ball, true);
      RenderProps.setEdgeWidth (ball, 2); // wire frame edge width
      RenderProps.setFaceColor (plane, new Color (.7f, 1f, .7f));
   }
}
