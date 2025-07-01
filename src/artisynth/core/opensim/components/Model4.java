package artisynth.core.opensim.components;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;

import artisynth.core.mechmodels.Marker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.ConstrainerBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.opensim.components.JointBase.BodyAndTransform;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderableUtils;
import maspack.render.RenderProps;

public class Model4 extends ModelBase {
   
   private Ground ground;
   private JointSet jointSet;
   private ComponentSet componentSet;
   
   public Model4() {
      ground = null;
      jointSet = null;
      componentSet = null;
   }
   
   public Ground getGround() {
      return ground;
   }
   
   public void setGround(Ground ground) {
      this.ground = ground;
      this.ground.setParent (this);
   }
   
   public JointSet getJointSet() {
      return jointSet;
   }
   
   public void setJointSet(JointSet joints) {
      jointSet = joints;
      jointSet.setParent (this);
   }
   
   public ComponentSet getComponentSet() {
      return componentSet;
   }
   
   public void setComponentSet(ComponentSet cset) {
      componentSet = cset;
      componentSet.setParent (this);
   }
   
   @Override
   public Model4 clone () {
      Model4 model = (Model4)super.clone ();
      
      if (ground != null) {
         model.setGround (ground.clone ());
      }
      
      if (jointSet != null) {
         model.setJointSet (jointSet.clone ());
      }
      
      if (componentSet != null) {
         model.setComponentSet (componentSet.clone ());
      }
      
      return model;
   }

   /**
    * TODO: build model
    */
   public MechModel createModel (
      MechModel mech, File geometryPath, ModelComponentMap componentMap) {
      if (mech == null) {
         mech = new MechModel(getName ());
      }
      componentMap.put (this, mech);
      
      // ground
      Ground ground = this.getGround();
      if (ground != null) {
         RigidBody groundBody =
            ground.createComponent (geometryPath, componentMap);
         mech.add (groundBody);
      }
      
      // bodies
      BodySet bodySet = this.getBodySet ();
      RenderableComponentList<RigidBody> bodies =
         bodySet.createComponent(geometryPath, componentMap);
      mech.add (bodies);
      
      // joints
      JointSet jointSet = this.getJointSet ();
      RenderableComponentList<artisynth.core.mechmodels.JointBase> joints =
         jointSet.createComponent(geometryPath, componentMap);
      mech.add (joints);
      
      // Move all child bodies to satisfy joint pose relative to parent

      // collect parents
      HashMap<OpenSimObject,BodyAndTransform> parentMap = new HashMap<>();
      for (JointBase joint : jointSet) {
         artisynth.core.mechmodels.JointBase jb =
            (artisynth.core.mechmodels.JointBase)componentMap.get(joint);
         BodyAndTransform parent = joint.findParentBodyAndTransform(componentMap);
         BodyAndTransform child = joint.findChildBodyAndTransform(componentMap);
         RigidTransform3d TCP = new RigidTransform3d(); // child-to-parent trans
         // also need TCD in case some default coordinates are not 0
         RigidTransform3d TCD = new RigidTransform3d();
         if (jb != null) {
            jb.getStoredTCD (TCD);
         }
         else {
            TCD.setIdentity();
         }
         if (joint.getReverse()) {
            TCP.mulInverseRight (child.transform, TCD);
            TCP.mulInverseRight (TCP, parent.transform);
         }
         else {
            TCP.mul (parent.transform, TCD);
            TCP.mulInverse (child.transform);  
         }
         parentMap.put(child.body, new BodyAndTransform(parent.body, TCP));
      }
      
      // compute poses
      for (Body body : bodySet) {
      
         RigidTransform3d pose = new RigidTransform3d(); // body's pose
         
         BodyAndTransform parent = parentMap.get (body);
         while (parent != null) {
            pose.mul (parent.transform, pose);
            parent = parentMap.get(parent.body);
         }
         RigidBody rb = (RigidBody)componentMap.get (body);
         rb.setPose (pose);
      }

      // update joint attachments, since poses have changed
      for (ModelComponent mc : joints) {
         if (mc instanceof BodyConnector) {
            ((BodyConnector)mc).updateAttachments();
         }
      }
      
      // force effectors
      ForceSet forceSet = this.getForceSet ();
      RenderableComponentList<ModelComponent> forces =
         forceSet.createComponent(geometryPath, componentMap);
      mech.add (forces);
      
      // constrainers
      ConstraintSet constraintSet = this.getConstraintSet ();
      ComponentList<ConstrainerBase> constraints =
         constraintSet.createComponent(geometryPath, componentMap);
      mech.add (constraints);
      
      // markers
      MarkerSet markerSet = this.getMarkerSet ();
      PointList<Marker> markers =
         markerSet.createComponent(geometryPath, componentMap);
      mech.add (markers);
      
      // set gravity
      Vector3d gravity = this.getGravity ();
      if (gravity != null) {
         mech.setGravity (gravity);
      }

      // set markers to render as spheres
      double modelRadius = RenderableUtils.getRadius (mech);
      RenderProps.setSphericalPoints (markers, 0.008*modelRadius, Color.CYAN);
      
      return mech;
   }
   
}
