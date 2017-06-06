package maspack.render;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;

/**
 * Experimental object to allow dragger motions to be constrained in arbitrary
 * ways as specified by the application.
 */
public interface Dragger3dConstrainer {

   /**
    * Takes a new value for the dragger rotation matrix and updates it to one
    * that satifies any constraints. The previous dragger transform is provided
    * for reference. If the constraint is such that the rotation matrix is
    * simply reset to its original value, the method should return false.
    *
    * @param RDW new value for the dragger rotation matrix, which should be
    * modified if necessary
    * @param TDW previous dragger-to-world transform.
    * @return <code>false</code> if there is no motion and <code>RDWnew</code>
    * is reset to its original value <code>TDW.R</code>.
    */
   public boolean updateRotation (
      RotationMatrix3d RDW, RigidTransform3d TDW);

   /**
    * Takes a new value for the dragger translation and updates it to one that
    * satifies any constraints. The previous dragger transform is provided for
    * reference. If the constraint is such that the translation is simply reset
    * to its original value, the method should return false.
    *
    * @param pnew new value for the dragger translation, which should be
    * modified if necessary
    * @param TDW previous dragger-to-world transform.
    * @return <code>false</code> if there is no motion and <code>pnew</code> is
    * reset to its original value <code>TDW.p</code>.
    */
   public boolean updatePosition (
      Vector3d pnew, RigidTransform3d TDW);

}
