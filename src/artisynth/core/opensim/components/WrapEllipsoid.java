package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.RigidEllipsoid;
import maspack.matrix.Vector3d;

public class WrapEllipsoid extends WrapObject {
   
   Vector3d dimensions;
   
   public WrapEllipsoid() {
      dimensions = null;
   }
   
   public Vector3d getDimensions () {
      return dimensions;
   }

   public void setDimensions (Vector3d dimensions) {
      this.dimensions = dimensions;
   }

   @Override
   public WrapEllipsoid clone () {
      
      WrapEllipsoid we = (WrapEllipsoid)super.clone ();
      if (dimensions != null) {
         we.setDimensions (dimensions.clone ());
      }
         
      return we;
   }
   
   @Override
   public RigidEllipsoid createComponent (File geometryPath, ModelComponentMap componentMap) {
      RigidEllipsoid rb = new RigidEllipsoid (getName(), dimensions.x, dimensions.y, dimensions.z, 0, 32);
      rb.setPose (getTransform());
      rb.setRenderProps (createRenderProps ());
      componentMap.put (this, rb);
      return rb;
   }
   
}
