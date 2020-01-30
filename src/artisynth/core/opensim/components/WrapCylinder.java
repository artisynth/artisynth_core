package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.RigidCylinder;

public class WrapCylinder extends WrapObject {

   private double radius;
   private double length;
   
   public WrapCylinder() {
      radius = 0;
      length = 0;
   }
   
   public double getRadius () {
      return radius;
   }

   public void setRadius (double radius) {
      this.radius = radius;
   }
   
   public double getLength () {
      return length;
   }

   public void setLength (double length) {
      this.length = length;
   }
   
   @Override
   public WrapCylinder clone () {
      return (WrapCylinder)super.clone ();
   }
   
   @Override
   public RigidCylinder createComponent (File geometryPath, ModelComponentMap componentMap) {
      RigidCylinder rb = new RigidCylinder (getName(), getRadius (), getLength(), 0);
      rb.setPose (getTransform());
      rb.setRenderProps (createRenderProps ());
      componentMap.put(this, rb);
      return rb;
   }
   
}
