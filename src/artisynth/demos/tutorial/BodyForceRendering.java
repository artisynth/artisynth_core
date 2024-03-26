package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.RenderableComponentBase;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;

public class BodyForceRendering extends RigidBodySpring {

   // Custom rendering component to draw forces acting on a rigid body
   class ForceRenderer extends RenderableComponentBase {

      RigidBody myBody;     // body whose forces are to be rendered
      double myForceScale;  // scale factor for force vector
      double myMomentScale; // scale factor for moment vector

      ForceRenderer (RigidBody body, double forceScale, double momentScale) {
         myBody = body;
         myForceScale = forceScale;
         myMomentScale = momentScale;
      }

      public void render (Renderer renderer, int flags) {
         if (myForceScale > 0) {
            // render force vector as a cyan colored arrow
            Vector3d pnt = myBody.getPosition();
            Vector3d dir = myBody.getForce().f;
            renderer.setColor (Color.CYAN);
            double radius = myRenderProps.getLineRadius();
            renderer.drawArrow (pnt, dir, myForceScale, radius,/*capped=*/false);
         }
         if (myMomentScale > 0) {
            // render moment vector as a green arrow
            Vector3d pnt = myBody.getPosition();
            Vector3d dir = myBody.getForce().m;
            renderer.setColor (Color.GREEN);
            double radius = myRenderProps.getLineRadius();
            renderer.drawArrow (pnt, dir, myMomentScale, radius,/*capped=*/false);
         }
      }
   }

   public void build (String[] args) {
      super.build (args);
      
      // get the MechModel from the superclass
      MechModel mech = (MechModel)findComponent ("models/mech");
      RigidBody box = mech.rigidBodies().get("box");

      // create and add the force renderer
      ForceRenderer frender = new ForceRenderer (box, 0.1, 0.5);
      mech.addRenderable (frender);

      // set line radius property to control radius of the force arrows
      RenderProps.setLineRadius (frender, 0.01);
   }
}
