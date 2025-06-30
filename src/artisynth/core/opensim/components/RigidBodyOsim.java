package artisynth.core.opensim.components;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;

/**
 * Extension to ArtiSynth RigidBody component that contains wrapping
 * components, wrapping attachments and a "joint" list (for OpenSim 3)in order to implement OpenSim bodies.
 */
public class RigidBodyOsim extends RigidBody {
   
   protected RenderableComponentList<WrapComponent> myWrapComponents;
   protected RenderableComponentList<FrameAttachment> myWrapAttachments;
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
            FrameAttachment.class, "wrapobjectset_attachments");

      myJoint =
         new RenderableComponentList<>(
            artisynth.core.mechmodels.JointBase.class, "joint");

      add (myWrapComponents);
      add (myWrapAttachments);
      add (myJoint);
   }

   void addWrapComponent (WrapComponent wcomp) {
      myWrapComponents.add (wcomp);
   }

   public boolean detachWrapComponent (WrapComponent wcomp) {
      if (myWrapComponents.contains (wcomp)) {
         RigidBody wbody = (RigidBody)wcomp;
         myWrapAttachments.remove (wbody.getAttachment());
         myWrapComponents.remove (wcomp);
         return true;
      }
      else {
         return false;
      }
   }

   void addWrapAttachment (FrameAttachment fattach) {
      myWrapAttachments.add (fattach);
   }

   boolean detachWrapComponent (FrameAttachment fattach) {
      return myWrapComponents.remove (fattach);
   }
}


