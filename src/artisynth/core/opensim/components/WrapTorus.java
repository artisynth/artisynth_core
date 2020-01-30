package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.RigidTorus;

public class WrapTorus extends WrapObject {

   private double inner_radius;
   private double outer_radius;
   
   public WrapTorus() {
      inner_radius = 0;
      outer_radius = 0;
   }
   
   public double getInnerRadius () {
      return inner_radius;
   }

   public void setInnerRadius (double inner_radius) {
      this.inner_radius = inner_radius;
   }

   public double getOuterRadius () {
      return outer_radius;
   }

   public void setOuterRadius (double outer_radius) {
      this.outer_radius = outer_radius;
   }
   
   @Override
   public WrapTorus clone () {
      return (WrapTorus)super.clone ();
   }
   
   @Override
   public RigidTorus createComponent (File geometryPath, ModelComponentMap componentMap) {
      RigidTorus rb = new RigidTorus (getName(), outer_radius, inner_radius, 0.0);
      rb.setPose (getTransform());
      rb.setRenderProps (createRenderProps ());
      componentMap.put (this, rb);
      return rb;
   }
   
}
