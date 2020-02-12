package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.RigidBody;

public class PhysicalFrame extends Frame {

   WrapObjectSet wrapObjectSet;
   
   public PhysicalFrame() {
      // initialize
      wrapObjectSet = null;
   }
   public WrapObjectSet getWrapObjectSet() {
      return wrapObjectSet;
   }
   
   public void setWrapObjectSet(WrapObjectSet wrap) {
      wrapObjectSet = wrap;
      wrapObjectSet.setParent (this);
   }
   
   @Override
   public PhysicalFrame clone () {

      PhysicalFrame body = (PhysicalFrame)super.clone ();
   
      if (wrapObjectSet != null) {
         body.setWrapObjectSet (wrapObjectSet.clone ());
      }
      
      return body;
   }

   @Override
   public RigidBody createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      
     RigidBody rb  = super.createComponent (geometryPath, componentMap);
     
     return rb;
   }
   
}
 