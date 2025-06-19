package artisynth.core.opensim.components;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;

/**
 * Extension to ArtiSynth RigidBody component that contains wrapping
 * components, wrapping attachments and a "joint" list (for OpenSim 3)in order to implement OpenSim bodies.
 */
public class RigidBodyOsim extends RigidBody {
   
   protected RenderableComponentList<WrapComponent> myWrapComponents;
   protected RenderableComponentList<FrameFrameAttachment> myWrapAttachments;
   protected RenderableComponentList<artisynth.core.mechmodels.JointBase> myJoint;

   public RigidBodyOsim() {
      super();
   }

   public RigidBodyOsim (String name) {
      super(name);
   }

   protected void initializeChildComponents() {
      super.initializeChildComponents();

      myWrapComponents = 
         new RenderableComponentList<> (
            WrapComponent.class, "wrapobjectset");

      myWrapAttachments =
         new RenderableComponentList<> (
            FrameFrameAttachment.class, "wrapobjectset_attachments");

      myJoint =
         new RenderableComponentList<>(
            artisynth.core.mechmodels.JointBase.class, "joint");

      add (myWrapComponents);
      add (myWrapAttachments);
      add (myJoint);
   }


}
