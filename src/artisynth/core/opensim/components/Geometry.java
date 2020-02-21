package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidMeshComp;
import artisynth.core.modelbase.ModelComponent;
import maspack.matrix.Vector3d;

public class Geometry extends HasAppearance implements ModelComponentGenerator<RigidMeshComp> {
   
   String socket_frame;
   Vector3d scale_factors;
   
   public Geometry() {
      // initialize
      socket_frame = null;
      scale_factors = new Vector3d(1,1,1);
   }
   
   public String getSocketFrame() {
      return socket_frame;
   }
   
   public void setSocketFrame(String path) {
      socket_frame = path;
   }
   
   public Vector3d getScaleFactors() {
      return scale_factors;
   }
   
   public void setScaleFactors(Vector3d scale) {
      scale_factors = scale;
   }
   
   @Override
   public Geometry clone () {

      Geometry geom = (Geometry)super.clone ();
      
      if (socket_frame != null) {
         geom.setSocketFrame (socket_frame);
      }
      if (scale_factors != null) {
         geom.setScaleFactors (scale_factors.clone ());
      }
      
      return geom;
   }
   
   // try to attach supplied component to this geometry's socket frame, true if successful
   protected boolean attachToSocketFrame(RigidMeshComp mesh, ModelComponentMap componentMap) {
      if (socket_frame != null) {
         // find frame to attach to
         OpenSimObject socket = componentMap.findObjectByPath (this, socket_frame);
         if (socket != null) {
            ModelComponent comp = componentMap.get (socket);
            if (comp instanceof RigidBody) {
               RigidBody rb = (RigidBody)comp;
               rb.addMeshComp (mesh);
               return true;
            }
         }
      }
      return false;
   }

   /** 
    * Will try to add mesh to the appropriate body via the socket_frame first, 
    * and if successful, will return null.  Otherwise, will return the created
    * mesh component.
    */
   @Override
   public RigidMeshComp createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      return null;
   }

}
