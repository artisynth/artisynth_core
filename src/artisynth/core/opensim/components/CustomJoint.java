package artisynth.core.opensim.components;

import java.util.HashMap;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.opensim.customjoint.OpenSimCustomJoint;
import maspack.matrix.RigidTransform3d;

public class CustomJoint extends JointBase {

   // indices of transform axes
   static final int ROTATION1 = 0;
   static final int ROTATION2 = 1;
   static final int ROTATION3 = 2;
   static final int[] ROTATION_IDXS = {ROTATION1, ROTATION2, ROTATION3};
   static final int TRANSLATION1 = 3;
   static final int TRANSLATION2 = 4;
   static final int TRANSLATION3 = 5;
   static final int[] TRANSLATION_IDXS = {TRANSLATION1, TRANSLATION2, TRANSLATION3};
   
   SpatialTransform spatialTransform;
   
   public CustomJoint() {
      spatialTransform = null;
   }
   
   public SpatialTransform getSpatialTransform() {
      return spatialTransform;
   }
   
   public void setSpatialTransform(SpatialTransform st) {
      spatialTransform = st;
      st.setParent (this);
   }
   
   @Override
   public CustomJoint clone () {
      CustomJoint joint = (CustomJoint)super.clone ();
      if (spatialTransform != null) {
         joint.setSpatialTransform (spatialTransform);
      }
      return joint;
   }

   @Override
   public OpenSimCustomJoint createJoint (
      RigidBody parent, RigidBody child) {
      
      // try to use Spherical, SphericalRpy, RollPitch, Revolute
      
      RigidBody childRB = child;
      RigidBody parentRB = parent;
      RigidTransform3d childTrans = getJointTransformInChild ();
      RigidTransform3d parentTrans = getJointTransformInParent ();
      if (getReverse ()) {
         childRB = parent;
         parentRB = child;
         childTrans = parentTrans;
         parentTrans = getJointTransformInChild ();
      }
      
      CoordinateSet cs = getCoordinateSet ();
      HashMap<String,Coordinate> coordMap = new HashMap<> (cs.size ());
      if (cs != null) {
         for (Coordinate coord : cs) {
            coordMap.put(coord.getName (), coord);
         }
      }
     
      OpenSimCustomJoint joint = new OpenSimCustomJoint(this);
      joint.setBodies (parentRB, parentTrans, childRB, childTrans);
      joint.setName (getName());
      
      return joint;
   }
   
}
