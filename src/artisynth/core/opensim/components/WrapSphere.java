package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.RigidSphere;

public class WrapSphere extends WrapObject {

   private double radius;
   
   public WrapSphere() {
      radius = 0;
   }
   
   public double getRadius () {
      return radius;
   }

   public void setRadius (double radius) {
      this.radius = radius;
   }
   
   @Override
   public WrapSphere clone () {
      return (WrapSphere)super.clone ();
   }
   
   @Override
   public RigidSphere createComponent (File geometryPath, ModelComponentMap componentMap) {
      RigidSphere rb = new RigidSphere (getName(), getRadius (), 0);
      rb.setPose (getTransform());
      rb.setRenderProps (createRenderProps ());
      componentMap.put(this, rb);
      return rb;
   }
   
}
