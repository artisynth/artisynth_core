package artisynth.core.modelbase;

import maspack.matrix.Point3d;

/**
 * An object that has a 3D position in space.
 */
public interface HasPosition {

   /**
    * Returns the position of this object in world coordinates.
    * 
    * @return point position
    */
   public Point3d getPosition();

}
