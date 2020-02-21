package artisynth.core.opensim.components;

import java.io.File;
import java.io.IOException;

import artisynth.core.mechmodels.RigidMeshComp;
import maspack.geometry.MeshBase;
import maspack.geometry.io.GenericMeshReader;
import maspack.matrix.Vector3d;

public class Mesh extends Geometry {
   
   String mesh_file;
   
   public Mesh() {
      mesh_file = null;
   }
   
   public String getMeshFile() {
      return mesh_file;
   }
   
   public void setMeshFile(String filename) {
      mesh_file = filename;
   }
   
   /**
    * Creates a mesh if it exists, transformed by any scale and transform
    * properties.
    * 
    * @param geometryPath path in which to search for geometry files
    * @return created mesh, or null if no geometry file specified
    */
   public MeshBase createMesh(File geometryPath) {
      MeshBase mesh = null;
      if (mesh_file != null) {
         try {
            mesh = GenericMeshReader.readMesh (new File(geometryPath, mesh_file));
            // scale and transform
            Vector3d scale = getScaleFactors ();
            if (scale != null) {
               mesh.scale (scale.x, scale.y, scale.z);
            }
            //            RigidTransform3d transform = getTransform ();
            //            if (transform != null) {
            //               mesh.transform (transform);
            //            }
         }
         catch (IOException e) {
            e.printStackTrace();
         }
         mesh.setRenderProps (createRenderProps ());
      }
      
      return mesh;
   }
   
   @Override
   public RigidMeshComp createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      
      MeshBase mesh = createMesh (geometryPath);
      RigidMeshComp rmc = new RigidMeshComp (getName());
      rmc.setMesh (mesh);
      rmc.setRenderProps (mesh.getRenderProps ());
      componentMap.put (this, rmc);
      
      // try to attach via socket frame
      if (attachToSocketFrame (rmc, componentMap)) {
         return null;
      }
      
      return rmc;
   }

   @Override
   public Mesh clone () {
      Mesh copy = (Mesh)super.clone ();
      copy.setMeshFile (mesh_file);
      return copy;
   }
   
}
