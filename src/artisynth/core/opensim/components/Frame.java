package artisynth.core.opensim.components;

import java.io.File;
import java.util.ArrayList;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidMeshComp;
import maspack.geometry.MeshBase;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;

public class Frame extends HasVisibleObjectOrAppearance implements ModelComponentGenerator<RigidBody> {

   FrameGeometry frameGeometry;
   GeometryList attached_geometry;
   
   public Frame() {
      // initialize
      frameGeometry = null;
      attached_geometry = null;
   }
   
   public GeometryList getAttachedGeometry() {
      return attached_geometry;
   }
   
   public void setAttachedGeometry(GeometryList list) {
      attached_geometry = list;
      list.setParent (this);
   }
   
   public void setFrameGeometry(FrameGeometry fg) {
      frameGeometry = fg;
      frameGeometry.setParent (this);
   }
  
   @Override
   public Frame clone () {

      Frame body = (Frame)super.clone ();
   
      if (frameGeometry != null) {
         body.setFrameGeometry (frameGeometry.clone ());
      }
      
      if (attached_geometry != null) {
         body.setAttachedGeometry (attached_geometry.clone ());
      }
      
      return body;
   }

   @Override
   public RigidBody createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      
      RigidBody rb = new RigidBody(getName ());
      componentMap.put (this, rb);   // needs to be up-front so we can find it in the geometry/joint
      
      // OpenSim 3 VisibleObject
      VisibleObject vo = getVisibleObject ();
      if (vo != null) {
         
         // extract geometries
         ArrayList<MeshBase> meshList = vo.createMeshes(geometryPath);
         for (MeshBase mesh : meshList) {
            rb.addMesh (mesh);
         }
         
         if (vo.getShowAxes ()) {
            // estimate from bounds
            Point3d pmin = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
            Point3d pmax = new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
            rb.updateBounds (pmin, pmax);
            
            double len = pmin.distance (pmax)/4;
            rb.setAxisLength (len);
         }
      }
      
      // OpenSim 4 FrameGeometry
      if (frameGeometry != null) {
         RigidMeshComp fg = frameGeometry.createComponent (geometryPath, componentMap);
         if (fg != null) {
            rb.addMeshComp (fg);
         }
      }
      
      // OpenSim 4 attached_geometry
      if (attached_geometry != null) {
         for (Geometry ag : attached_geometry) {
            RigidMeshComp mesh = ag.createComponent (geometryPath, componentMap);
            if (mesh != null) {
               rb.addMeshComp (mesh);
            }
         }
      }
      
      // rb's render properties
      if (vo != null) {
         RenderProps rprops = vo.createRenderProps ();
         rb.setRenderProps (rprops);
      }
      
      return rb;
   }
   
}
 