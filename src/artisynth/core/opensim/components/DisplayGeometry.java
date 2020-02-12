package artisynth.core.opensim.components;

import java.io.File;
import java.io.IOException;

import maspack.geometry.MeshBase;
import maspack.geometry.io.GenericMeshReader;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.ColorMapProps;
import maspack.render.MeshRenderProps;
import maspack.render.RenderProps;

public class DisplayGeometry extends VisibleBase {

   private String geometry_file;
   private String texture_file;
   private RigidTransform3d transform;
   private Vector3d scale_factors;
   
   public DisplayGeometry() {
      geometry_file = null;
      texture_file = null;
      transform = null;
      scale_factors = null;
   }
   
   public DisplayGeometry(String filename) {
      this();
      this.geometry_file = filename;
   }
   
   public void setGeometryFile(String file) {
      geometry_file = file;
   }
   
   public String getGeometryFile () {
      return geometry_file;
   }

  
   public void setTextureFile(String file) {
      texture_file = file;
   }
   
   public String getTextureFile () {
      return texture_file;
   }

   public void setTransform(RigidTransform3d trans) {
      transform = trans;
   }
   
   public RigidTransform3d getTransform () {
      return transform;
   }
   
   public void setScaleFactors(Vector3d scale) {
      scale_factors = scale;
   }

   public Vector3d getScaleFactors () {
      return scale_factors;
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
      if (geometry_file != null) {
         try {
            mesh = GenericMeshReader.readMesh (new File(geometryPath, geometry_file));
            // scale and transform
            Vector3d scale = getScaleFactors ();
            if (scale != null) {
               mesh.scale (scale.x, scale.y, scale.z);
            }
            
            RigidTransform3d transform = getTransform ();
            if (transform != null) {
               mesh.transform (transform);
            }
         }
         catch (IOException e) {
            e.printStackTrace();
         }
      }
      return mesh;
   }
   
   /**
    * Updates the render properties with any explicit values set in this object
    * @param rprops
    */
   protected void updateRenderProps(RenderProps rprops) {
     
      if (texture_file != null) {
         ColorMapProps texture = new ColorMapProps ();
         texture.setFileName (texture_file);
         rprops.setColorMap (texture);
      }
      
      super.updateRenderProps (rprops);
   }
   
   /**
    * Gets render properties from this object, ignoring the mesh details.  If no
    * render props are explicity set, will return null
    * 
    * @return new render props
    */
   public RenderProps createRenderProps() {
      RenderProps rprops = new MeshRenderProps ();
      return rprops;
   }
   
   public DisplayGeometry clone() {
      DisplayGeometry dg = (DisplayGeometry)super.clone ();
      if (transform != null) {
         dg.setTransform (transform.copy ());
      }
      if (scale_factors != null) {
         dg.setScaleFactors (scale_factors.clone ());
      }
      return dg;
   }
   
   
   
}
