package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.FrameFrameAttachment;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.WrapComponent;
import artisynth.core.modelbase.RenderableComponentList;
import maspack.matrix.RigidTransform3d;

public class Ground extends PhysicalFrame {
   
   public Ground() {
      // initialize
   }
   
   @Override
   public Ground clone () {
      Ground body = (Ground)super.clone ();
      return body;
   }

   @Override
   public RigidBodyOsim createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      
      RigidBodyOsim rb =
         (RigidBodyOsim)super.createComponent (geometryPath, componentMap);

      // add wrapping surfaces
      WrapObjectSet wrapBodies = getWrapObjectSet ();
      if (wrapBodies != null) {
         // RenderableComponentList<WrapComponent> wrapComponents = 
         //    wrapBodies.createComponent(geometryPath, componentMap);
         // rb.add(wrapComponents);
         wrapBodies.addComponents (
            rb.myWrapComponents, geometryPath, componentMap);
        
         // attach wrappables to this frame
         //RenderableComponentList<FrameFrameAttachment> wrapAttachments = new RenderableComponentList<> (FrameFrameAttachment.class, "wrapobjectset_attachments");
         for (WrapObject wo : wrapBodies) {
            RigidTransform3d trans = wo.getTransform ();
            // set initial pose
            artisynth.core.mechmodels.Frame wrap = (artisynth.core.mechmodels.Frame)componentMap.get (wo);
            wrap.setPose (trans);
            wrap.transformPose (rb.getPose ());
            
            FrameFrameAttachment ffa = new FrameFrameAttachment (wrap, rb, trans);
            rb.myWrapAttachments.add (ffa);
         }
         //rb.add(wrapAttachments);
      }
     
      rb.setDynamic (false);  // fixed in space
      rb.setGrounded (true);  // set grounded property too (used by IKSolvers)

      return rb;
   }
   
}
