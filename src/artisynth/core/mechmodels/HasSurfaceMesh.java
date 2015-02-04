package artisynth.core.mechmodels;

import maspack.geometry.PolygonalMesh;

/**
 * Defines components that are associated with surface meshes.
 * 
 * @author lloyd
 */
public interface HasSurfaceMesh {
   
   /**
    * Returns the surface mesh associated with this component, or
    * <code>null</code> if no mesh exists. If multiple surface
    * meshes exist, this should be first one in the array returned
    * by {@link #getSurfaceMeshes()}.
    * 
    * @return component's (primary) surface mesh
    */
   public PolygonalMesh getSurfaceMesh();
   
   /**
    * Returns the number of surfaces meshes associated with this
    * component, or 0 if no meshes exist. If {@link #getSurfaceMesh()}
    * returns <code>null</code>, then this method should return 0.
    * 
    * @return number of surface meshes associated with this component
    */
   public int numSurfaceMeshes();
   
   /**
    * Returns an array listing all the (non-null) surface meshes associated
    * with this component, or a zero-length array if there are none.
    * If {@link #getSurfaceMesh} returns <code>null</code>, then a 
    * zero-length array should be returned.
    * 
    * @return surface meshes associated with this component
    */
   
   public PolygonalMesh[] getSurfaceMeshes();

}
