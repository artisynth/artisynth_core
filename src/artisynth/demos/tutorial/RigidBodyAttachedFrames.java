package artisynth.demos.tutorial;

import artisynth.core.mechmodels.FrameAttachedFrame;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Point3d;
import maspack.render.Renderer.AxisDrawStyle;

/**
 * Demo extending RigidBodyJoint to illustrate FrameAttachedFrames. Two
 * FrameAttachedFrames are added to bodyA: one specified via a frame-relative
 * transform (TFM) and one via a world-coordinate pose (TFW).
 */
public class RigidBodyAttachedFrames extends RigidBodyJoint {

   public void build (String[] args) {

      // build the base scene (two rigid bodies + hinge joint)
      super.build (args);

      // Add a FrameAttachedFrame via TFM: positioned at the tip of bodyA
      // along its local x-axis (half of lenx2 from the body origin).
      RigidTransform3d TFM1 = new RigidTransform3d (lenx2/2, 0, 0);
      FrameAttachedFrame frm1 = bodyA.addAttachedFrame (TFM1);
      frm1.setName ("frameAtTip");
      frm1.setAxisLength (4.0);
      frm1.setAxisDrawStyle (AxisDrawStyle.ARROW);

      // Add a FrameAttachedFrame via TFW: placed in world coordinates at the
      // centre of bodyA's current world pose.
      RigidTransform3d TFW = new RigidTransform3d (bodyA.getPose());
      FrameAttachedFrame frm2 = bodyA.addAttachedFrameWorld (TFW);
      frm2.setName ("frameAtCenter");
      frm2.setAxisLength (4.0);
      frm2.setAxisDrawStyle (AxisDrawStyle.ARROW);
      frm2.addMarker (new Point3d());
   }
}
