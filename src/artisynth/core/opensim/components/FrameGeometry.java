package artisynth.core.opensim.components;

import java.awt.Color;
import java.io.File;

import artisynth.core.mechmodels.RigidMeshComp;
import maspack.geometry.PolylineMesh;
import maspack.render.RenderProps;

public class FrameGeometry extends Geometry {
   
   double display_radius;
   
   public FrameGeometry() {
      // initialize
      display_radius = -1;
   }
   
   public double getDisplayRadius() {
      return display_radius;
   }
   
   public void setDisplayRadius(double r) {
      this.display_radius = r;
   }
   
   @Override
   public FrameGeometry clone () {
      FrameGeometry frame = (FrameGeometry)super.clone ();    
      return frame;
   }

   /**
    * Creates a frame mesh component.
    */
   @Override
   public RigidMeshComp createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      
      // create basic frame
      PolylineMesh mesh = new PolylineMesh();
      mesh.addVertex (0, 0, 0);
      mesh.addVertex (1, 0, 0);
      mesh.addVertex (0, 1, 0);
      mesh.addVertex (0, 0, 1);
      mesh.addLine (new int[] {0, 1}); // x-axis
      mesh.addLine (new int[] {0, 2}); // y-axis
      mesh.addLine (new int[] {0, 3}); // z-axis
      mesh.setFeatureColoringEnabled ();
      mesh.setColor (0, Color.RED);
      mesh.setColor (1, Color.GREEN);
      mesh.setColor (2, Color.BLUE);
            
      RigidMeshComp axes = new RigidMeshComp(getName ());
      axes.setMesh (mesh);
      componentMap.put(this, axes);
      
      RenderProps rprops = new RenderProps();
      updateRenderProps (rprops);
      axes.setRenderProps (rprops);
      
      if (scale_factors != null) {
         mesh.scale (scale_factors.x, scale_factors.y, scale_factors.z);
      }
      
      if (display_radius >= 0) {
         mesh.scale(display_radius);
      }
      
      // try to attach component to socket frame
      // if successful, return null to prevent adding twice
      if (attachToSocketFrame(axes, componentMap)) {
         return null;
      }
            
      return axes;
   }
   
}
