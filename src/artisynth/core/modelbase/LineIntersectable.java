package artisynth.core.modelbase;

import maspack.matrix.Line;

/**
 * Indicates an object that can be intersected by a line.
 */
public interface LineIntersectable {
   
   /**
    * Returns the point on this object which is nearest to the specified
    * line.  The point is described by a {@link HasPosition} object which is
    * attached to the object and moves with it.
    *  
    * @param line line to which nearest point is to be computed
    * @return nearest point to the line
    */
   public HasPosition nearestPointToLine (Line line);

}
