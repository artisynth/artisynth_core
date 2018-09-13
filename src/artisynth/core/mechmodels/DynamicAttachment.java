/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.MatrixBlock;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import artisynth.core.modelbase.*;

/**
 * Object that implements attachment between dynamic components.
 * A component 'a' can be attached to one or more "master" components
 * 'b' if it's position q_a can be made an
 * explicit differentiable function of the positions q_m of the masters:
 * <pre>
 * q_a = f (q_m)
 * </pre>
 * Differentiating means that the attached component velocity u_a
 * is a linear function of the master velocities u_m:
 * <pre>
 * u_a = -G (u_m)
 * </pre>
 * where G = -(d f)/(d q_m) is the "constraint matrix".
 */
public interface DynamicAttachment {

   public DynamicComponent[] getMasters();

   public int numMasters();
   
   // should be able to remove
   public void invalidateMasters();

  /**
    * Returns the slave DynamicMechComponent associated with this attachment.
    * In some cases, the attachment may connect some other entity (such
    * as a mesh vertex) to the master components, in which case this method
    * should return <code>null</code>.  
    * 
    * @return slave DynamicMechComponent, if any
    */
   public DynamicComponent getSlave();

   public boolean slaveAffectsStiffness();

   public void setSlaveAffectsStiffness (boolean affects);


   /**
    * Every master component should contain a back reference to each
    * attachment that references it. This method adds the back reference
    * for this attachment to each of the masters.
    */
   public void addBackRefs();
   
   /**
    * Removes the back reference to this attachment's slave component
    * from each of the master component.
    */
   public void removeBackRefs();

   public void updatePosStates();

   public void updateVelStates();

   /**
    * Update attachment to reflect changes in the slave state.
    */
   public void updateAttachment();

   public void applyForces();

   public void addMassToMasters();

   public boolean getDerivative (double[] buf, int idx);

   /** 
    * Computes
    * <pre>
    *       T
    * D -= G  M
    * </pre>
    * where D and M are matrices associated with master and slave components,
    * respectively, and G is the constraint matrix for the attachment.
    * @param D dependent matrix associated with a master component
    * @param M matrix associated with a slave component
    */
   public void mulSubGTM (MatrixBlock D, MatrixBlock M, int idx);

   /** 
    * Computes
    * <pre>
    * D -= M G
    * </pre>
    * where D and M are matrices associated with master and slave components,
    * respectively, and G is the constraint matrix for the attachment.
    * @param D dependent matrix associated with a master component
    * @param M matrix associated with a slave component
    */
   public void mulSubMG (MatrixBlock D, MatrixBlock M, int idx);
   
   /**
    * Returns the transpose of the constraint matrix G associated
    * with the idx-th master component.
    * 
    * @param idx index of the master component
    * @return transpose of G associated with idx
    */
   public MatrixBlock getGT (int idx);

   /** 
    * Computes
    * <pre>
    *       T
    * y -= G  x
    * </pre>
    * where y and x are vectors associated with master and slave components,
    * respectively, and G is the constraint matrix for the attachment.
    * @param ybuf buffer into which to store result
    * @param yoff offset into ybuf
    * @param xbuf buffer containing right hand side vector
    * @param xoff offset into xbuf
    * @param idx master component index
    */
   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx);

}
