package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SolidJoint;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;

public class PhysicalOffsetFrame extends PhysicalFrame {
   
   Vector3d translation;
   AxisAngle orientation;
   String socket_parent;
   
   public PhysicalOffsetFrame() {
      // initialize
      translation = null;
      orientation = null;
      socket_parent = null;
   }
   
   public Vector3d getTranslation() {
      return translation;
   }
   
   public void setTranslation(Vector3d trans) {
      translation = trans;
   }
   
   public AxisAngle getOrientation() {
      return orientation;
   }
   
   public void setOrientation(AxisAngle orient) {
      orientation = orient;
   }
   
   public String getSocketParent() {
      return socket_parent;
   }
   
   public void setSocketParent(String path) {
      socket_parent = path;
   }
   
   @Override
   public PhysicalOffsetFrame clone () {
      PhysicalOffsetFrame frame = (PhysicalOffsetFrame)super.clone ();
      
      if (translation != null) {
         frame.setTranslation(translation.clone ());
      }
      
      if (orientation != null) {
         frame.setOrientation (orientation.clone ());
      }
      
      return frame;
   }
   
   public RigidTransform3d getOffsetTransform() {
      Vector3d t = translation;
      if (t == null) {
         t = Vector3d.ZERO;
      }
      
      AxisAngle orient = orientation;
      if (orient == null) {
         orient = AxisAngle.IDENTITY;
      }
      
      return new RigidTransform3d(t, orient);
   }

   @Override
   public RigidBody createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      
      RigidBody pof = super.createComponent (geometryPath, componentMap);
      
      // offset from parent
      if (socket_parent != null) {
         OpenSimObject parentFrame = componentMap.findObjectByPath (this, socket_parent);
         RigidBody parentRB = (RigidBody)componentMap.get (parentFrame);
         
         if (parentRB != null) {
            RigidTransform3d TFM = getOffsetTransform();
            
            
            SolidJoint sj = new SolidJoint();
            sj.setBodies (parentRB, TFM, pof, RigidTransform3d.IDENTITY);
            pof.add(sj);
            
            //            FrameFrameAttachment ffa = new FrameFrameAttachment (pof);
            //            ffa.setWithTFM (parentRB, TFM);
            //            ffa.setName ("attachment");
            //            pof.add (ffa);
         }
      }
      
      return pof;
   }
   
}
