package artisynth.core.mechmodels;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

/**
 * Interface for objects that can be acted on by a point force. This
 * is used to implement the ArtiSynth PullController.
 */
public interface Pullable {

   boolean isPullable();

   /**
    * Constructs force origin storage data formed by interesting the object
    * with a ray in space. If null, assumes that there is no origin, so no
    * force can be applied
    *
    * @param pnt origin of the ray
    * @param dir direction of the ray
    * @return force origin data
    */
   public Object getOriginData (Point3d pnt, Vector3d dir);

   /**
    * Determines the world-coordinate point to which force will
    * be applied (used for determining magnitude of force)
    */
   public Point3d getOriginPoint(Object data);

   public double getPointRenderRadius();

   /**
    * Given the supplied force origin info and a force vector,
    * apply the force (typically sets an external force)
    */
   public void applyForce(Object origin, Vector3d force);
}
