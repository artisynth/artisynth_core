package artisynth.core.opensim.components;

import java.io.File;
import java.util.ArrayList;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.FrameFrameAttachment;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.RenderableComponentList;
import maspack.geometry.MeshBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.render.RenderProps;

public class Body extends HasVisibleObject implements ModelComponentGenerator<RigidBody> {

   private double mass;
   private Point3d mass_center;
   SymmetricMatrix3d inertia;
   Joint joint;
   
   WrapObjectSet wrapObjectSet;
   
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
      visibleObject = null;
      wrapObjectSet = null;
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
      inertia.set (vals);
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
   
   public WrapObjectSet getWrapObjectSet() {
      return wrapObjectSet;
   }
   
   public void setWrapObjectSet(WrapObjectSet wrap) {
      wrapObjectSet = wrap;
      wrapObjectSet.setParent (this);
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
      if (wrapObjectSet != null) {
         body.setWrapObjectSet (wrapObjectSet.clone ());
      }
      
      return body;
   }

   @Override
   public RigidBody createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      
      RigidBody rb = new RigidBody(getName ());
      VisibleObject vo = getVisibleObject ();
      
      // show axes
      if (vo != null) {
         
         // extract geometries
         ArrayList<MeshBase> meshList = vo.createMeshes(geometryPath);
         for (MeshBase mesh : meshList) {
            rb.addMesh (mesh);
         }
         
         if (vo.getShowAxes ()) {
            // estimate from bounds
            Point3d pmin = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
            Point3d pmax = new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
            rb.updateBounds (pmin, pmax);
            
            double len = pmin.distance (pmax)/4;
            rb.setAxisLength (len);
         }
      }
      
      componentMap.put (this, rb);   // needs to be up-front so we can find it in the joint
      
      // add joint
      Joint jointHolder = getJoint ();
      if (jointHolder != null) {
         
         // set body pose and add joint
         
         // get parent and child bodies
         JointBase joint = jointHolder.getJoint ();
         if (joint != null) {
            Body parentBody = componentMap.findObjectByName (Body.class, joint.getParentBody ());
            RigidBody parentRB = (RigidBody)componentMap.get (parentBody);
            
            // compute body pose
            RigidTransform3d pose = new RigidTransform3d(parentRB.getPose ());
            RigidTransform3d jointToParent = joint.getJointTransformInParent ();
            RigidTransform3d jointToChild = joint.getJointTransformInChild ();
            if (joint.getReverse ()) {
               // reverse transform
               pose.mul (jointToChild);
               pose.mulInverse (jointToParent);
            } else {
               pose.mul (jointToParent);
               pose.mulInverse (jointToChild);
            }
            
            // update pose
            rb.setPose (pose);
            
            // create and add joint
            artisynth.core.mechmodels.JointBase jb = joint.createComponent(geometryPath, componentMap);
            if (jb != null) {
               // connect joint within body
               RenderableComponentList<artisynth.core.mechmodels.JointBase> joints = new RenderableComponentList<>(artisynth.core.mechmodels.JointBase.class, "joint");
               rb.add (joints);
               joints.add (jb);
            } else {
               System.out.println ("null joint");
            }
         }
      }
         
      // add wrapping surfaces
      WrapObjectSet wrapBodies = getWrapObjectSet ();
      RenderableComponentList<RigidBody> wrapComponents = wrapBodies.createComponent(geometryPath, componentMap);
      rb.add(wrapComponents);
      
      // attach wrappables to this frame
      RenderableComponentList<FrameFrameAttachment> wrapAttachments = new RenderableComponentList<> (FrameFrameAttachment.class, "wrapobjectset_attachments");
      for (WrapObject wo : wrapBodies) {
         RigidTransform3d trans = wo.getTransform ();
         // set initial pose
         Frame wrap = (Frame)componentMap.get (wo);
         wrap.setPose (trans);
         wrap.transformPose (rb.getPose ());
         
         FrameFrameAttachment ffa = new FrameFrameAttachment (wrap, rb, trans);
         wrapAttachments.add (ffa);
      }
      rb.add(wrapAttachments);
      
      // set mass and inertia last so not affected by geometries
      rb.setInertia (getMass (), getInertia (), getMassCenter ());
      
      // rb's render properties
      RenderProps rprops = vo.createRenderProps ();
      rb.setRenderProps (rprops);
      
      return rb;
   }
   
}
