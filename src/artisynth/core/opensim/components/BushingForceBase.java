package artisynth.core.opensim.components;

import java.io.File;
import java.util.ArrayList;

import artisynth.core.mechmodels.FrameSpring;
import artisynth.core.mechmodels.Frame;
import artisynth.core.materials.FrameMaterial;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.opensim.components.JointBase.BodyAndTransform;
import maspack.matrix.*;

public abstract class BushingForceBase extends ForceBase
   implements ModelComponentGenerator<ModelComponent>{

   protected Vector3d translational_damping;
   protected Vector3d rotational_damping;

   // OpenSim 3
   private String body_1;
   private Point3d location_body_1;
   private AxisAngle orientation_body_1;
   private String body_2;
   private Point3d location_body_2;
   private AxisAngle orientation_body_2;

   // OpenSim 4.0
   private FrameList frames;
   private String socket_frame1;
   private String socket_frame2;

   public BushingForceBase() {
      translational_damping = null;
      rotational_damping = null;
      body_1 = null;
      location_body_1 = null;
      orientation_body_1  = null;
      body_2 = null;
      location_body_2 = null;
      orientation_body_2  = null;
      frames = null;
      socket_frame1 = null;
      socket_frame2 = null;
   }
   
   public void setTranslationalDamping (Vector3d dvec) {
      translational_damping = dvec;
   }

   public Vector3d getTranslationalDamping() {
      return translational_damping;
   }

   public void setRotationalDamping (Vector3d dvec) {
      rotational_damping = dvec;
   }

   public Vector3d getRotationalDamping() {
      return rotational_damping;
   }

   public void setBody1 (String body) {
      body_1 = body;
   }
   
   public String getBody1() {
      return body_1;
   }
   
   public void setLocationBody1 (Point3d loc) {
      location_body_1 = loc;
   }
   
   public Point3d getLocationBody1() {
      return location_body_1;
   }
   
   public void setOrientationBody1 (AxisAngle axisAng) {
      orientation_body_1 = axisAng;
   }
   
   public AxisAngle getOrientationBody1() {
      return orientation_body_1;
   }
   
   public void setBody2 (String body) {
      body_2 = body;
   }
   
   public String getBody2() {
      return body_2;
   }
   
   public void setLocationBody2 (Point3d loc) {
      location_body_2 = loc;
   }
   
   public Point3d getLocationBody2() {
      return location_body_2;
   }
   
   public void setOrientationBody2 (AxisAngle axisAng) {
      orientation_body_2 = axisAng;
   }
   
   public AxisAngle getOrientationBody2() {
      return orientation_body_2;
   }

   public FrameList getFrames() {
      return frames;
   }
   
   public void setFrames (FrameList frames) {
      this.frames = frames;
      this.frames.setParent (this);
   }
   
   public String getSocketFrame1() {
      return socket_frame1;
   }
   
   public void setSocketFrame1 (String path) {
      socket_frame1 = path;
   }
   
   public String getSocketFrame2() {
      return socket_frame2;
   }
   
   public void setSocketFrame2 (String path) {
      socket_frame2 = path;
   }
   
   /**
    * Transform from body to spring (OpenSim 3)
    * @return joint pose
    */
   public RigidTransform3d getBodyTransform (
      Point3d loc, AxisAngle orient) {
      if (loc == null) {
         loc = Point3d.ZERO;
      }
      if (orient == null) {
         orient = AxisAngle.IDENTITY;
      }
      return new RigidTransform3d(loc, orient);
   }
   
   @Override
   public BushingForceBase clone () {
      
      BushingForceBase bushing = (BushingForceBase)super.clone ();
      
      if (translational_damping != null) {
         bushing.setTranslationalDamping (translational_damping.clone());
      }
      if (rotational_damping != null) {
         bushing.setRotationalDamping (rotational_damping.clone());
      }
      bushing.setBody1 (body_1);
      if (location_body_1 != null) {
         bushing.setLocationBody1 (location_body_1.clone ());
      }
      if (orientation_body_1 != null) {
         bushing.setOrientationBody1 (orientation_body_1.clone());
      }
      bushing.setBody1 (body_2);
      if (location_body_2 != null) {
         bushing.setLocationBody1 (location_body_2.clone ());
      }
      if (orientation_body_2 != null) {
         bushing.setOrientationBody2 (orientation_body_2.clone());
      }
      bushing.setSocketFrame1 (socket_frame1);
      bushing.setSocketFrame2 (socket_frame2);
      if (frames != null) {
         bushing.setFrames (frames.clone ());
      }
      return bushing;
   }
   
   protected abstract FrameMaterial createMaterial();

   protected FrameSpring createFrameSpring (
      Frame bodyA, RigidTransform3d TCA, Frame bodyB, RigidTransform3d TDB) {

      FrameSpring fspring = new FrameSpring();
      fspring.setMaterial (createMaterial());
      fspring.setFrames (bodyA, TCA, bodyB, TDB);
      return fspring;
   }
   
   @Override
   public ModelComponent createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      
      // create frames first in case referred to within joint
      FrameList frames = getFrames ();
      if (frames != null) {
         // TODO: figure out why this doesn't work
         //         RenderableComponentList<RigidBody> jframes = new RenderableComponentList<>(RigidBody.class, "frames");
         //         jointRoot.add (jframes);
         //         for (Frame frame : frames) {
         //            RigidBody f = frame.createComponent (geometryPath, componentMap);
         //            jframes.add (f);
         //         }
         
         // Alternate hack: add null frames so we can find offsets later
         for (artisynth.core.opensim.components.Frame frame : frames) {
            componentMap.put(frame, null);
         }
      }

      Frame frame2 = null;
      Frame frame1 = null;
      RigidTransform3d TD2 = null;  // pose of joint in parent
      RigidTransform3d TC1 = null;  // pose of joint in child
      
      String socket_frame1 = getSocketFrame1();
      if (socket_frame1 != null) {

         // OpenSim 4.0

         String socket_frame2 = getSocketFrame2();
         
         // TODO: fix this, doesn't seem to work to attach directly to PhysicalOffsetFrame
         //         OpenSimObject parentFrame = componentMap.findObjectByPath (this, socket_frame1);
         //         frame2 = (Frame)componentMap.get (parentFrame);
         //         TD2 = RigidTransform3d.IDENTITY;
         //         
         //         OpenSimObject childFrame = componentMap.findObjectByPath (this, socket_child_frame);
         //         frame1 = (Frame)componentMap.get (childFrame);
         //         TC1 = RigidTransform3d.IDENTITY;
         
         // HACK: find first connected body and relative transform
         BodyAndTransform body1 = 
            JointBase.findBodyAndTransform (this, socket_frame1, componentMap);
         BodyAndTransform body2 = 
            JointBase.findBodyAndTransform (this, socket_frame2, componentMap);

         frame1 = (Frame)componentMap.get (body1.body);
         TC1 = body1.transform;
         frame2 = (Frame)componentMap.get (body2.body);
         TD2 = body2.transform;
         
      } else {
         
         // OpenSim 3.0
   
         // find parent and child
         Body body1 = componentMap.findObjectByName (Body.class, getBody1());
         frame1 = (Frame)componentMap.get (body1);
         Body body2 = componentMap.findObjectByName (Body.class, getBody2());
         frame2 = (Frame)componentMap.get (body2);

         TC1 = getBodyTransform (location_body_1, orientation_body_1);        
         TD2 = getBodyTransform (location_body_2, orientation_body_2);
      }
      
      if (frame1 == null) {
         return null;
      }
      
      // create FrameSpring to implement the bushing
      FrameSpring fspring = createFrameSpring (frame1, TC1, frame2, TD2);  
      fspring.setName (getName());
      return fspring;
   }

}
