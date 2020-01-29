package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.RigidBody;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;

public abstract class JointBase extends OpenSimObject implements ModelComponentGenerator<artisynth.core.mechmodels.JointBase>{

   private String parent_body;
   private Point3d location_in_parent;
   private AxisAngle orientation_in_parent;
   private Point3d location;
   private AxisAngle orientation;
   private boolean reverse;  // whether the joint transform defines parent->child or child->parent
   private CoordinateSet coordinateSet;

   public JointBase() {
      parent_body = null;
      location_in_parent = null;
      orientation_in_parent  = null;
      location = null;
      orientation = null;
      reverse = false;
      setCoordinateSet(new CoordinateSet ());
   }
   
   public void setParentBody(String body) {
      parent_body = body;
   }
   
   public String getParentBody() {
      return parent_body;
   }
   
   public void setLocationInParent(Point3d loc) {
      location_in_parent = loc;
   }
   
   public Point3d getLocationInParent() {
      return location_in_parent;
   }
   
   public void setOrientationInParent(AxisAngle orient) {
      orientation_in_parent = orient;
   }
   
   public AxisAngle getOrientationInParent() {
      return orientation_in_parent;
   }
   
   /**
    * Joint pose relative to parent body
    * @return joint pose
    */
   public RigidTransform3d getJointTransformInParent() {
      return new RigidTransform3d(location_in_parent, orientation_in_parent);
   }
   
   public void setLocation(Point3d loc) {
      location = loc;
   }
   
   public Point3d getLocation() {
      return location;
   }
   
   /**
    * Joint pose relative to child body
    * @return joint pose
    */
   public RigidTransform3d getJointTransformInChild() {
      return new RigidTransform3d(location, orientation);
   }
   
   public void setOrientation(AxisAngle orient) {
      orientation = orient;
   }
   
   public AxisAngle getOrientation() {
      return orientation;
   }
   
   public void setReverse(boolean set) {
      reverse = set;
   }
   
   public boolean getReverse() {
      return reverse;
   }
   
   public CoordinateSet getCoordinateSet() {
      return coordinateSet;
   }
   
   public void setCoordinateSet(CoordinateSet cs) {
      coordinateSet = cs;
      coordinateSet.setParent (this);
   }
   
   @Override
   public JointBase clone () {
      
      JointBase jb = (JointBase)super.clone ();
      
      jb.setParentBody (parent_body);
      if (location_in_parent != null) {
         jb.setLocationInParent (location_in_parent.clone ());
      }
      if (orientation_in_parent != null) {
         jb.setOrientationInParent (orientation_in_parent.clone());
      }
      if (location != null) {
         jb.setLocation (location.clone ());
      }
      if (orientation != null) {
         jb.setOrientation (orientation.clone());
      }
      jb.setReverse (reverse);
      if (coordinateSet != null) {
         jb.setCoordinateSet (coordinateSet.clone ());
      }
      
      return jb;
   }
   
   protected abstract artisynth.core.mechmodels.JointBase createJoint(RigidBody parent, RigidBody child);
   
   @Override
   public artisynth.core.mechmodels.JointBase createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      // find parent and child
      Body parentBody = componentMap.findObjectByName (Body.class, getParentBody ());
      RigidBody parentRB = (RigidBody)componentMap.get (parentBody);
      
      RigidBody childRB = null;
      OpenSimObject parent = getParent ();
      if (parent instanceof Joint) {
         OpenSimObject grandParent = parent.getParent ();
         if (grandParent instanceof Body) {
            childRB = (RigidBody)componentMap.get (grandParent);     
         }
      }
      
      if (parentRB == null || childRB == null) {
         return null;
      }
      
      artisynth.core.mechmodels.JointBase joint = createJoint (parentRB, childRB);
      
      componentMap.put (this, joint);
      
      return joint;
   }

}
