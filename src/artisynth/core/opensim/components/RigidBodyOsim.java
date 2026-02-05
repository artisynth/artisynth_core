package artisynth.core.opensim.components;

import java.io.*;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;

import maspack.geometry.*;
import maspack.matrix.*;

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

   public void addWrapComponent (WrapComponent wcomp) {
      myWrapComponents.add (wcomp);
   }

   public boolean removeWrapComponent (WrapComponent wcomp) {
      return myWrapComponents.remove (wcomp);
   }

   public FrameFrameAttachment attachWrapComponent (
      WrapComponent wcomp, RigidTransform3d TWB) {
      if (!(wcomp instanceof artisynth.core.mechmodels.Frame)) {
         throw new IllegalArgumentException (
            "wrap component 'wcomp' is not an instance of Frame");
      }
      FrameFrameAttachment ffa = new FrameFrameAttachment (
         (artisynth.core.mechmodels.Frame)wcomp, this, TWB);
      myWrapAttachments.add (ffa);
      myWrapComponents.add (wcomp);
      return ffa;
   }

   public FrameFrameAttachment attachWrapComponent (WrapComponent wcomp) {
      // TWB is the transform from wcomp to this body
      RigidTransform3d TWB = new RigidTransform3d(); 
      TWB.mulInverseLeft (getPose(), wcomp.getPose());      
      return attachWrapComponent (wcomp, TWB);
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

   public void addWrapAttachment (FrameAttachment fattach) {
      myWrapAttachments.add (fattach);
   }

   public boolean removeWrapAttachment (FrameAttachment fattach) {
      return myWrapAttachments.remove (fattach);
   }

   // boolean detachWrapComponent (FrameAttachment fattach) {
   //    return myWrapComponents.remove (fattach);
   // }

   public RenderableComponentList<WrapComponent> getWrapComponents() {
      return myWrapComponents;
   }

   public RenderableComponentList<FrameAttachment> getWrapAttachments() {
      return myWrapAttachments;
   }

   public artisynth.core.mechmodels.JointBase getJoint() {
      if (myJoint.size() > 0) {
         return myJoint.get(0);
      }
      else {
         return null;
      }
   }

   public static RigidBodyOsim createFromMesh (
      String bodyName, PolygonalMesh mesh, double density, double scale) {
      
      return createFromMesh (bodyName, mesh, null, density, scale);
   }

   public static RigidBodyOsim createFromMesh (
      String bodyName, PolygonalMesh mesh, String meshFilePath,
      double density, double scale) {

      RigidBodyOsim body = new RigidBodyOsim (bodyName);
      mesh.triangulate(); // just to be sure ...
      mesh.scale (scale);
      AffineTransform3d X = null;
      if (scale != 1) {
         X = AffineTransform3d.createScaling (scale);
      }
      body.setDensity (density);
      body.setMesh (mesh, meshFilePath, X);
      return body;
   }

   public static RigidBodyOsim createFromMesh (
      String bodyName, String meshPath, double density, double scale) {

      PolygonalMesh mesh = null;
      try {
         mesh = new PolygonalMesh (new File (meshPath));
      }
      catch (IOException e) {
         System.out.println ("Can't create mesh from "+meshPath);
         e.printStackTrace();
         return null;
      }
      return createFromMesh (bodyName, mesh, meshPath, density, scale);
   }


      
   
}


