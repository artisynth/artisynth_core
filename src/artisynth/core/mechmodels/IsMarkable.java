package artisynth.core.mechmodels;

import artisynth.core.mechmodels.Marker;
import artisynth.core.modelbase.ComponentList;
import maspack.matrix.Line;
import maspack.matrix.Point3d;

/**
 * Can add a marker to this component
 */
public interface IsMarkable {
   /**
    * Creates a marker along a ray of intersection
    * @param ray ray to intersect with
    * @return created marker, or null if no intersection or not supported
    */
   public Marker createMarker(Line ray);
   
   /**
    * Creates a marker at a specific location
    * @param pnt world location for marker
    * @return create marker, or null if no intersection or not supported
    */
   public Marker createMarker(Point3d pnt);

   /**
    * Adds the marker to the component (or potentially somewhere within the component hierarchy)
    * @param mkr marker to add
    * @return true if add is successful, false otherwise
    */
   public boolean addMarker(Marker mkr);
   
   /**
    * Checks whether or not the marker can be added to the component (or potentially somewhere within the component hierarchy), 
    * without actually adding it.
    * 
    * @param mkr marker to add
    * @return true if add would be successful, false otherwise
    * @see #addMarker(Marker)
    */
   public boolean canAddMarker(Marker mkr);
   
}
