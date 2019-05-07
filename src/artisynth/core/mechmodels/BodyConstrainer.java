/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.spatialmotion.RigidBodyConstraint;
import maspack.matrix.MatrixNdBlock;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.util.DoubleHolder;
import java.util.ArrayList;
import java.util.HashMap;

import artisynth.core.modelbase.HasNumericState;

public interface BodyConstrainer extends HasNumericState {
   /**
    * Updates internal information needed for computing constraints.
    * 
    * @param t
    * current time (seconds)
    * @param setEngaged if <code>true</code>, then the method should determine
    * which unilateral constraints are engaged
    */
   public void updateBodyStates (double t, boolean setEngaged);

   /**
    * Returns the first body associated with this constrainer.
    * 
    * @return first body associated with this constrainer
    */
   public ConnectableBody getBodyA();

   /**
    * Returns the second body associated with this constrainer, or null if there
    * is no such body.
    * 
    * @return second body associated with this constrainer
    */
   public ConnectableBody getBodyB();

//   public MatrixNdBlock getBilateralBlockA (int numb);
//
//   public MatrixNdBlock getBilateralBlockB (int numb);
//
//   public MatrixNdBlock getUnilateralBlockA (int numu);
//
//   public MatrixNdBlock getUnilateralBlockB (int numu);

//   /**
//    * Updates the indices of the bodies associated with this constrainer.
//    * 
//    * @param indexMap
//    * maps rigid bodies to their associate indices
//    */
//   public void updateBodyIndices (HashMap<RigidBody,Integer> indexMap);

   /**
    * Returns the number of bilateral constraints associated with this
    * constrainer.
    * 
    * @return number of bilateral constraints
    */
   public int numBilateralConstraints();
   
   /**
    * Returns the number of unilateral constraints which are currently
    * engaged. This should be the same as the number of constraints
    * that would be returned by 
    * {@link #getUnilateralConstraints getUnilateralConstraints} with
    * <code>setEngaged</code> set to <code>false</code>.
    * 
    * @return number of currently engaged unilateral constraints
    */
   public int numUnilateralConstraints();

   /**
    * Gets the bilateral constraints associated with this constrainer. They are
    * supplied by the constrainer and appended to an array list.
    * 
    * @param bilaterals
    * array list into which the constrainer places the constraints
    */
   public int getBilateralConstraints (ArrayList<RigidBodyConstraint> bilaterals);

   public int setBilateralForces (VectorNd lam, double s, int idx);

   public int getBilateralForces (VectorNd lam, int idx);

   // added to implement Constrainer

   public void getBilateralSizes (VectorNi sizes);

   public void getUnilateralSizes (VectorNi sizes);

   public double updateConstraints (double t, int flags);

   /**
    * Gets the unilateral constraints associated with this constrainer. They are
    * supplied by the constrainer and appended to an array list.
    * 
    * @param unilaterals
    * array list into which the constrainer places the constraints
    * @param setEngaged if true, recompute the set of unilateral constraints.
    * If false, the set of unilateral constraints will stay the same.
    * @return maximum penetration of the unilateral constraints
    */
   public double getUnilateralConstraints (
      ArrayList<RigidBodyConstraint> unilaterals, boolean setEngaged);

   public int setUnilateralForces (VectorNd the, double s, int idx);

   public int getUnilateralForces (VectorNd the, int idx);
   
   public void zeroForces();

   /**
    * Returns true if this constrainer has unilateral constraints.
    * 
    * @return true if there are unilateral constraints
    */
   public boolean hasUnilateralConstraints();

   /**
    * Updates the unilateral constraints associated with this constrainer. The
    * constraints are the same as those returned by the most recent call to
    * {@link
    * artisynth.core.mechmodels.BodyConstrainer#getUnilateralConstraints
    * getUnilateralConstraints}, located at a specific offset within an array
    * list.
    * 
    * @param unilaterals
    * contains constraints which should be updated
    * @param offset
    * starting location of constraints within <code>unilaterals</code>
    * @param num
    * number of constraints to update
    */
   public void updateUnilateralConstraints (
      ArrayList<RigidBodyConstraint> unilaterals, int offset, int num);

   public void setPenetrationTol (double tol);

   public double getPenetrationTol();

}
