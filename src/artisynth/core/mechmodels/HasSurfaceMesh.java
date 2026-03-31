package artisynth.core.mechmodels;

import artisynth.core.modelbase.RenderableComponent;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Line;
import maspack.matrix.Point3d;

/**
 * Defines components that are associated with surface meshes.
 * 
 * @author lloyd
 */
public interface HasSurfaceMesh extends RenderableComponent {
   
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
    
    /**
     * Intersects this component with a ray, returning the nearest intersection
     * point on the surface meshes. If no intersection is found, then
     * <code>false</code> is returned and the value of <code>nearest</code>
     * is undefined.
     * 
     * @param nearest returns nearest intersection point on surface meshes
     * @param ray ray to intersect with
     * @return true if intersects
     */
    default public boolean computeRayIntersection (Point3d nearest, Line ray) {
       PolygonalMesh[] meshes = getSurfaceMeshes();
       if (meshes == null || meshes.length == 0) {
          return false;
       }
       
       double nearestDistance = Double.POSITIVE_INFINITY;
       for (PolygonalMesh mesh : meshes) {
          Point3d pos = BVFeatureQuery.nearestPointAlongRay (
             mesh, ray.getOrigin(), ray.getDirection());
          if (pos != null) {
             double d = pos.distance(ray.getOrigin());
             if (d < nearestDistance) {
                nearestDistance = d;
                nearest.set (pos);
             }
          }
       }
       return nearestDistance != Double.POSITIVE_INFINITY; 
    }

}
