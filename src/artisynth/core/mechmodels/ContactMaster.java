/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;
import maspack.geometry.*;

/**
 * Stores information about a master component associated with a contact
 * constraint, including the identity of the component itself, its contribution
 * weight, and the associated contact point.
 */
public class ContactMaster {
   
   CollidableDynamicComponent myComp;
   double myWeight;
   ContactPoint myCpnt;

   public ContactMaster (
      CollidableDynamicComponent comp, double wgt, ContactPoint cpnt) {

      myComp = comp;
      myWeight = wgt;
      myCpnt = cpnt;
   }
   
   public ContactMaster (
      CollidableDynamicComponent comp, double wgt) {

      myComp = comp;
      myWeight = wgt;
      myCpnt = null;
   }

   public int getSolveIndex () {
      return myComp.getSolveIndex();
   }

   MatrixBlock createBlock() {
      return MatrixBlockBase.alloc (myComp.getVelStateSize(), 1);
   }

   public MatrixBlock getBlock (Vector3d dir) {
      MatrixBlock blk = createBlock();
      double[] buf = new double[myComp.getVelStateSize()];
      myComp.setContactConstraint (buf, myWeight, dir, myCpnt);
      blk.setColumn (0, buf);
      return blk;
   }

   public MatrixBlock get1DFrictionBlock (Vector3d dir) {
      return getBlock (dir);
   }

   public MatrixBlock get2DFrictionBlock (Vector3d dir1, Vector3d dir2) {
      MatrixBlock blk = MatrixBlockBase.alloc (myComp.getVelStateSize(), 2);
      double[] buf = new double[myComp.getVelStateSize()];
      myComp.setContactConstraint (buf, myWeight, dir1, myCpnt);
      blk.setColumn (0, buf);
      myComp.setContactConstraint (buf, myWeight, dir2, myCpnt);
      blk.setColumn (1, buf);
      return blk;
   }

   public void addRelativeVelocity (Vector3d vel) {
      myComp.addToPointVelocity (vel, myWeight, myCpnt);
   }

}
