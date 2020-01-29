package artisynth.core.opensim.components;

import java.io.File;
import java.util.ArrayList;

import maspack.geometry.MeshBase;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

public class VisibleObject extends VisibleBase {
   
   GeometrySet geometrySet;
   Vector3d scale_factors;
   boolean show_axes;
   RigidTransform3d transform;
   DisplayPreference display_preference;
   
   public VisibleObject() {
      geometrySet = new GeometrySet ();
      scale_factors = new Vector3d(1,1,1);
      show_axes = false;
      transform = new RigidTransform3d();
      display_preference = null;
   }
   
   public GeometrySet getGeometrySet() {
      return geometrySet;
   }
   
   public void setGeometrySet(GeometrySet geom) {
      geometrySet = geom;
   }
   
   public Vector3d getScaleFactors() {
      return scale_factors;
   }
   
   public void setScaleFactors(Vector3d scale) {
      scale_factors = scale;
   }
   
   public boolean getShowAxes() {
      return show_axes;
   }
   
   public void setShowAxes(boolean show) {
      show_axes = show;
   }
   
   public void setTransform(RigidTransform3d trans) {
      transform = trans;
   }
   
   public RigidTransform3d getTransform() {
      return transform;
   }
   
   public void setDisplayPreference(DisplayPreference display) {
      display_preference = display;
   }
   
   public DisplayPreference getDisplayPreference() {
      return display_preference;
   }
   
   @Override
   public VisibleObject clone () {
      VisibleObject vo = (VisibleObject) super.clone ();
      if (geometrySet != null) {
         vo.setGeometrySet (geometrySet.clone ());
      }
      if (scale_factors != null) {
         vo.setScaleFactors (scale_factors.clone ());
      }
      vo.setShowAxes (show_axes);
      if (transform != null) {
         vo.setTransform (transform.copy ());
      }
      vo.setDisplayPreference (display_preference);
      return vo;
   }

   /**
    * Creates a set of polygonal mesh objects from the internal geometry set
    * @param geometryPath path in which to search for geometry files
    * @return list of meshes, complete with corresponding render properties
    */
   public ArrayList<MeshBase> createMeshes (File geometryPath) {

      ArrayList<MeshBase> meshes = new ArrayList<>();
      
      if (geometrySet != null) {
         for (DisplayGeometry dg : geometrySet) {
             MeshBase pm = dg.createMesh (geometryPath);
             if (pm != null) {
                RenderProps mprops = pm.getRenderProps ();
                if (mprops == null) {
                   mprops = new RenderProps ();
                   pm.setRenderProps (mprops);
                }
                dg.updateRenderProps (mprops);
                meshes.add (pm);
             }
         }
         
         // potentially transform all meshes
         Vector3d scale = getScaleFactors ();
         if (scale != null) {
            for (MeshBase mesh : meshes) {
               mesh.scale (scale.x, scale.y, scale.z);
            }
         }
         
         // orientation
         RigidTransform3d trans = getTransform ();
         if (trans != null) {
            for (MeshBase mesh : meshes ) {
               mesh.transform (trans);
            }
         }     
      }
      
      return meshes;
      
   }

}
