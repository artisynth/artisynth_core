package artisynth.demos.wrapping;

import maspack.matrix.Point3d;

/**
 * Represents a mathematical object that is parametrized.
 * @author Omar
 */
public interface ParametricEntity {
   /**
    * Get location of the point described by the given parameters, in some body
    * coordinate frame.
    * @param params     parameters
    * @return           location of the point with the given parameters
    */
   public Point3d getLocation(double params[]);
   
   /**
    * Get position of the point described by the given parameters, in world
    * coordinates.
    * @param params     parameters
    * @return           position of the point with the given parameters
    */
   public Point3d getPosition(double params[]);
}
