package artisynth.demos.wrapping;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Point;
import maspack.matrix.RigidTransform3d;

/**
 * A wrapping object for which a wrap path can be solved using exact
 * methods. Given any two points (not on or in the mesh), this object
 * should be able to find a minimum length continuously differentiable path
 * between them such that segments of the path not on the mesh are straight
 * lines.
 * @author Omar
 */
public abstract class ExactWrappableGeometry {
   protected Frame parent;              // Parent frame for this object.
   protected RigidTransform3d toParent; // Transform to the parent.
   
   /**
    * Get the parent of this wrap object.
    * @return           parent frame for this object
    */
   public Frame getParent() {
      return parent;
   }
   
   /**
    * Get transform of geometry in parent's frame.
    * @return           transform to parent's frame
    */
   public RigidTransform3d getTransformToParent() {
      return toParent;
   }
   
   /**
    * Get transform of geometry in world frame. Returns a new object (not a
    * reference).
    * @return           transform to world frame
    */
   public RigidTransform3d getTransformToWorld() {
      RigidTransform3d toW = new RigidTransform3d();
      toW.mul(parent.getPose(), toParent);
      return toW;
   }
   
   /**
    * Get transform from world frame to the geometry's frame. Returns a new
    * object (not a reference).
    * @return           transform from world frame
    */
   public RigidTransform3d getTransformFromWorld() {
      RigidTransform3d fromW = getTransformToWorld();
      fromW.invert();
      return fromW;
   }
   
   /**
    * Find a path between the two points that wraps around this object.
    * @param p1         first point
    * @param p2         second point
    * @return           the path
    */
   public abstract ExactWrapPath wrap(Point p1, Point p2);
   
   /**
    * Find a path between the two points that wraps around this object. In case
    * of a non-unique solution, use the solution closest to the previous path.
    * @param p1         first point
    * @param p2         second point
    * @param prev       previous path
    * @return           the path
    */
   public abstract ExactWrapPath wrap(Point p1, Point p2, ExactWrapPath prev);
}
