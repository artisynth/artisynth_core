package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.FrameFrameAttachment;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.WrapComponent;
import artisynth.core.modelbase.RenderableComponentList;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SymmetricMatrix3d;

public class Body extends PhysicalFrame implements 
   ModelComponentGenerator<RigidBody> {

   private double mass;
   private Point3d mass_center;
   SymmetricMatrix3d inertia;
   Joint joint;
   
   //   private ArrayList<DisplayGeometry> boneMeshes = new ArrayList<DisplayGeometry>(); //Holds names of all the VTP files for the bone. 
   //   //Hand for example has multiple .VTP mesh files for different bones considered together
   //   private ArrayList<String> boneJointNames = new ArrayList<String>(); //Holds names of all joints associated with this bone
   //   private ArrayList<String> wrapObjects = new ArrayList<String>(); //Holds names of the wrap objects
   
   public Body() {
      // initialize
      mass = 0;
      mass_center = new Point3d(0,0,0);
      inertia = new SymmetricMatrix3d();
      joint = null;
   }
   
//   public void setParentBody(String pBody) {
//      parentBody = pBody;
//   }
//   public String getParentBody() {
//      return parentBody;
//   }
   
   public void setMass(double mass) {
      this.mass = mass;
   }
   
   public double getMass() {
      return mass;
   }
   
   public void setMassCenter(Point3d point) {
      mass_center = point;
   }
   
   public Point3d getMassCenter() {
      return mass_center;
   }
   
//   public void setLocation(Point3d location) {
//      loc = location;
//   }
//   
//   public Point3d getLocation() {
//      return loc;
//   }
   
//   public void setOrientation(Point3d orient) {
//      or = orient;
//   }
//   
//   public Point3d getOrientation() {
//      return or;
//   }
   
   public void setInertia(SymmetricMatrix3d sym) {
      inertia = sym;
   }
   
   public void setInertiaXX(double xx) {
      inertia.m00 = xx;
   }
   
   public void setInertiaYY(double yy) {
      inertia.m11 = yy;
   }
   
   public void setInertiaZZ(double zz) {
      inertia.m22 = zz;
   }
   
   public void setInertiaXY(double xy) {
      inertia.m01 = xy;
   }
   
   public void setInertiaXZ(double xz) {
      inertia.m02 = xz;
   }
   
   public void setInertiaYZ(double yz) {
      inertia.m12 = yz;
   }
   
   public void setInertia(double[] vals) {
      inertia.set (vals[0], vals[1], vals[2], vals[3], vals[4], vals[5]);
   }
   
   public SymmetricMatrix3d getInertia() {
      return inertia;
   }
   
   public Joint getJoint() {
      return joint;
   }
   
   public void setJoint(Joint joint) {
      this.joint = joint;
      this.joint.setParent (this);
   }
   
   //   public void addBoneMesh (DisplayGeometry mesh) {
   //      boneMeshes.add(mesh);
   //   }
   //   
   //   public ArrayList<DisplayGeometry> getBoneMeshes() {
   //      if (boneMeshes == null) {
   //         return null;
   //      } else {
   //         return new ArrayList<DisplayGeometry>(boneMeshes);
   //      }
   //   }
   //   
   //   public boolean hasMesh() {
   //      return (boneMeshes != null);
   //   }
   //   
   //   public void addJointName(String name) {
   //      boneJointNames.add(name);
   //   }
   //   
   //   public void addJointNames(ArrayList<String> names) {
   //      boneJointNames.addAll(names);
   //   }
   //   
   //   public ArrayList<String> getJointNames() {
   //      return boneJointNames;
   //   }
   //   
   //   public void addWrapObject(String name) {
   //      wrapObjects.add(name);
   //   }
   //   
   //   public ArrayList<String> getWrapObjects() {
   //      return wrapObjects;
   //   }
   
   @Override
   public Body clone () {

      Body body = (Body)super.clone ();
      
      body.setMass (mass);
      if (mass_center != null) {
         body.setMassCenter (mass_center.clone ());
      }
      if (inertia != null) {
         body.setInertia (inertia.clone ());
      }
      if (joint != null) {
         body.setJoint (joint.clone ());
      }
      
      return body;
   }

   @Override
   public RigidBodyOsim createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      
      RigidBodyOsim rb =
         (RigidBodyOsim)super.createComponent (geometryPath, componentMap);
      componentMap.put (this, rb);   // needs to be up-front so we can find it in the joint
      
      // add joint
      Joint jointHolder = getJoint ();
      if (jointHolder != null) {
         
         // set body pose and add joint
         
         // get parent and child bodies
         JointBase joint = jointHolder.getJoint ();
         if (joint != null) {
            Body parentBody = componentMap.findObjectByName (
               Body.class, joint.getParentBody ());
            RigidBody parentRB = (RigidBody)componentMap.get (parentBody);
            
            // create and add joint
            artisynth.core.mechmodels.JointBase jb = 
               joint.createComponent(geometryPath, componentMap);
            if (jb != null) {
               // connect joint within body
               rb.myJoint.add (jb);
               componentMap.put (joint, jb);
            } else {
               // can this happen?
               System.out.println ("null joint");
            }
            
            // compute body pose
            RigidTransform3d pose = new RigidTransform3d(parentRB.getPose ());
            RigidTransform3d jointToParent = joint.getJointTransformInParent ();
            RigidTransform3d jointToChild = joint.getJointTransformInChild ();
            // also need TCD in case some default coordinates are not 0
            RigidTransform3d TCD = new RigidTransform3d();
            if (jb != null) {
               jb.getStoredTCD (TCD);
            }
            else {
               // can this happen?
               TCD.setIdentity();
            }
            if (joint.getReverse ()) {
               // reverse transform
               pose.mul (jointToChild);
               pose.mulInverse (TCD);
               pose.mulInverse (jointToParent);
            } else {
               pose.mul (jointToParent);
               pose.mul (TCD);
               pose.mulInverse (jointToChild);
            }
            rb.setPose (pose);
            if (jb != null) {
               // update attachments because rb pose has changed
               jb.updateAttachments();
            }
         }
         else {
            // probably ground - make it so if named ground or if mass is 0
            if (getMass() == 0 || "ground".equalsIgnoreCase (getName())) {
               rb.setGrounded (true);
               rb.setDynamic (false);
            }
         }
      }
         
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
            artisynth.core.mechmodels.Frame wrap =
               (artisynth.core.mechmodels.Frame)componentMap.get (wo);
            wrap.setPose (trans);
            wrap.transformPose (rb.getPose ());
            
            FrameFrameAttachment ffa = new FrameFrameAttachment (wrap, rb, trans);
            rb.myWrapAttachments.add (ffa);
         }
         //rb.add(wrapAttachments);
      }
      
      // set mass and inertia last so not affected by geometries
      rb.setInertia (getMass (), getInertia (), getMassCenter ());
      
      return rb;
   }
   
}
