/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;

import artisynth.core.modelbase.StepAdjustment;
import maspack.matrix.*;
import maspack.util.IntHolder;

/**
 * Interface to a second order mechanical system that can be integrated by 
 * a variety of integrators.
 */
public interface MechSystem {

   /** 
    * Flag passed to {@link #updateConstraints updateConstraints()}
    * indicating that contact information should be computed.
    */
   public static final int COMPUTE_CONTACTS = 0x01;

   /** 
    * Flag passed to {@link #updateConstraints updateConstraints()} indicating
    * that contact information should be updated. This means that there has
    * only been an adjustment in position and existing contacts between bodies
    * should be maintained, though possibly with a modified set of constraints.
    */
   public static final int UPDATE_CONTACTS = 0x02;

   /**
    * Contains information for a single constraint direction
    */
   public class ConstraintInfo {

      // Note: distance to the constraint surface should be signed in such a
      // way that solving the constraint equation G dx = -dist will produce
      // an impulse that moves the system back towards the constraint surface.
      public double dist;      // distance to the constraint surface.
      public double compliance;// inverse stiffness; 0 implies rigid constraint
      public double damping;   // damping; only used if compliance > 0
      public double force;     // used for computing non-linear compliance
   };

   /**
    * Contains information for a single friction constraint set.
    * This is associated with one bilateral or unilateral constraint,
    * and corresponds to a single column block entry in the transposed friction
    * constraint matrix <code>DT</code> produced by
    * {@link #getFrictionConstraints getFrictionConstraints()}.
    * The constraint set contains either one or two friction directions
    * (corresponding to a column block size of either 1 or 2).
    */
   public class FrictionInfo {

      // Flag indicating that the associated contact constraint is BILATERAL
      public static final int BILATERAL = 0x01;

      public double mu;        // friction coefficient
      public int contactIdx0;  // corresponding contact constraint index
      public int contactIdx1;  // second contact constraint index (if needed)
      public int flags;        // information flags
      
      /**
       * Returns the maximum friction value based on the most recent
       * contact force and the coefficient of friction. Contacts forces
       * are stored in the supplied vector lam, at the location(s) indexed
       * by contactIdx0 and (possibly) contactIdx1. 
       */
      public double getMaxFriction (VectorNd lam) {
         if (contactIdx1 == -1) {
            return mu*lam.get(contactIdx0);
         }
         else {
            return mu*Math.hypot (lam.get(contactIdx0), lam.get(contactIdx1));
         }
      }
   };

   /**
    * Returns the current structure version of this system. The structure
    * version should be incremented whenever the number and arrangement
    * of active and parametric components changes, implying that
    * any mass and solve matrices should be be rebuilt.
    * 
    * @return current structure version number for this system
    */
   public int getStructureVersion();

   /**
    * Returns the size of the position state for all active components.
    * This should remain unchanged as long as the current structure
    * version (returned by {@link #getStructureVersion getStructureVersion()} 
    * remains unchanged.
    * 
    * @return size of the active position state
    */
   public int getActivePosStateSize();

   /**
    * Gets the current position state for all active components. This is stored
    * in the vector <code>q</code> (which will be sized appropriately by the
    * system).
    * 
    * @param q 
    * vector in which the state is stored
    */
   public void getActivePosState (VectorNd q);

   /**
    * Sets the current position state for all active components. This is
    * supplied in the vector <code>q</code>, whose size should be greater than
    * or equal to the value returned by {@link #getActivePosStateSize
    * getActivePosStateSize()}.
    * 
    * @param q
    * vector supplying the state information
    */
   public void setActivePosState (VectorNd q);

   /**
    * Returns the size of the velocity state for all active components.
    * This should remain unchanged as long as the current structure
    * version (returned by {@link #getStructureVersion getStructureVersion()} 
    * remains unchanged.
    * 
    * @return size of the velocity state
    */
   public int getActiveVelStateSize();

   /**
    * Gets the current velocity state for all active components. This is stored
    * in the vector <code>u</code> (which will be sized appropriately by the
    * system).
    * 
    * @param u
    * vector in which the state is stored
    */
   public void getActiveVelState (VectorNd u);

   /**
    * Sets the current velocity state for all active components. This is
    * supplied in the vector <code>u</code>, whose size should be greater than
    * or equal to the value returned by {@link #getActiveVelStateSize
    * getActiveVelStateSize()}.
    * 
    * @param u
    * vector supplying the state information
    */
   public void setActiveVelState (VectorNd u);

   /**
    * Returns the size of the position state for all parametric components.
    * This should remain unchanged as long as the current structure
    * version (returned by {@link #getStructureVersion getStructureVersion()} 
    * remains unchanged.
    * 
    * @return size of the parametric position state
    */
   public int getParametricPosStateSize();

   /**
    * Obtains the desired target position for all parametric components at a
    * particular time.  This is stored in the vector <code>q</code> (which will
    * be sized appropriately by the system).
    *
    * The system interpolates between the current parametric position values
    * and those desired for the end of the time step, using a parameter
    * <code>s</code> defined on the interval [0,1].  In particular, specifying
    * s = 0 yields the current parametric positions, and s = 1 yields the
    * positions desired for the end of the step. The actual time step size
    * <code>h</code> is also provided, as this may be needed by some
    * interpolation methods.
    *
    * <p>The interpolation uses the current parametric positions, and possibly
    * the current parametric velocities as well.  Hence this method should be
    * called before either of these are changed using {@link
    * #setParametricPosState setParametricPosState()} or {@link
    * #setParametricVelState setParametricVelState()}.
    * 
    * @param q
    * vector returning the state information
    * @param s
    * specifies time relative to the current time step
    * @param h
    * time step size
    */
   public void getParametricPosTarget (VectorNd q, double s, double h);

   /**
    * Gets the current position state for all parametric components. This is
    * stored in the vector <code>q</code> (which will be sized appropriately by
    * the system).
    * 
    * @param q
    * vector in which the state is stored
    */
   public void getParametricPosState (VectorNd q);

   /**
    * Sets the current position state for all parametric components. This is
    * supplied in the vector <code>q</code>, whose size should be greater than
    * or equal to the value returned by {@link #getParametricPosStateSize
    * getParametricPosStateSize()}.
    * 
    * @param q
    * vector supplying the state information
    */
   public void setParametricPosState (VectorNd q);

   /**
    * Returns the size of the velocity state for all parametric components.
    * This should remain unchanged as long as the current structure
    * version (returned by {@link #getStructureVersion getStructureVersion()} 
    * remains unchanged.
    * 
    * @return size of the parametric velocity state
    */
   public int getParametricVelStateSize();

   /**
    * Obtains the desired target velocity for all parametric components at a
    * particular time.  This is stored in the vector <code>u</code> (which will
    * be sized appropriately by the system).
    *
    * The system interpolates between the current parametric velocity values
    * and those desired for the end of the time step, using a parameter
    * <code>s</code> defined on the interval [0,1].  In particular, specifying
    * s = 0 yields the current parametric velocities, and s = 1 yields the
    * velocities desired for the end of the step. The actual time step size
    * <code>h</code> is also provided, as this may be needed by some
    * interpolation methods.
    *
    * <p>The interpolation uses the current parametric velocities, and possibly
    * the current parametric positions as well.  Hence this method should be
    * called before either of these are changed using {@link
    * #setParametricPosState setParametricPosState()} or {@link
    * #setParametricVelState setParametricVelState()}.
    * 
    * @param u
    * vector returning the parametric target velocites
    * @param s
    * specifies time relative to the current time step
    * @param h
    * time step size
    */
   public void getParametricVelTarget (VectorNd u, double s, double h);

   /**
    * Gets the current velocity state for all parametric components. This is
    * stored in the vector <code>u</code> (which will be sized appropriately by
    * the system).
    * 
    * @param u
    * vector in which state is stored
    */
   public void getParametricVelState (VectorNd u);

   /**
    * Sets the current velocity state for all parametric components. This is
    * supplied in the vector <code>u</code>, whose size should be greater than
    * or equal to the value returned by {@link #getParametricVelStateSize
    * getParametricVelStateSize()}.
    * 
    * @param u
    * vector supplying the state information
    */
   public void setParametricVelState (VectorNd u);

   /**
    * Sets the forces associated with parametric components. This is supplied
    * in the vector <code>f</code>, whose size should be greater or equal to
    * the value returned by {@link #getParametricVelStateSize 
    * getParametricVelStateSize()}.
    * 
    * @param f
    * vector supplying the force information
    */
   public void setParametricForces (VectorNd f);

   /**
    * Gets the forces associated with parametric components. This is returned
    * in the vector <code>f</code> (which will be sized appropriately by the
    * system).
    * 
    * @param f
    * vector in which to return the force information
    */
   public void getParametricForces (VectorNd f);

   /**
    * Gets the current value of the position derivative for all active
    * components. This is stored in the vector <code>dxdt</code> (which will be
    * sized appropriately by the system).
    * 
    * @param dxdt
    * vector in which the derivative is stored
    * @param t
    * current time value
    */
   public void getActivePosDerivative (VectorNd dxdt, double t);

   /**
    * Returns the generalized forces acting on all the active components in this
    * system. This is stored in the vector <code>f</code> (which will be
    * sized appropriately by the system).
    * 
    * @param f
    * vector in which the forces are stored
    */
   public void getActiveForces (VectorNd f);

   /**
    * Sets the generalized forces acting on all the active components in this
    * system. The values are specifies in the vector <code>f</code>, whose size 
    * should be greater or equal to the value returned by 
    * {@link #getActiveVelStateSize getActiveVelStateSize()}.
    * 
    * @param f
    * vector specifying the forces to be set
    */
   public void setActiveForces (VectorNd f);

   /** 
    * Builds a mass matrix for this system. This is done by adding blocks of an
    * appropriate type to the sparse block matrix <code>M</code>. On input,
    * <code>M</code> should be empty with zero size; it will be sized
    * appropriately by the system.
    *
    * <p>This method returns <code>true</code> if the mass matrix is constant;
    * i.e., does not vary with time. It does not place actual values in the
    * matrix; that must be done by calling {@link #getMassMatrix 
    * getMassMatrix()}.
    *
    * <p>A new mass matrix should be built whenever the system's structure
    * version (as returned by {@link #getStructureVersion 
    * getStructureVersion()}) changes.
    * 
    * @param M matrix in which the mass matrix will be built
    * @return true if the mass matrix is constant.
    */
   public boolean buildMassMatrix (SparseNumberedBlockMatrix M);

   /** 
    * Sets <code>M</code> to the current value of the mass matrix for this
    * system, evaluated at time <code>t</code>. <code>M</code> should
    * have been previously created with a call to
    * {@link #buildMassMatrix buildMassMatrix()}. The current mass forces
    * are returned in the vector <code>f</code>, which should have a
    * size greater or equal to the size of <code>M</code>.
    * The mass forces (also known as the fictitious forces)
    * are given by
    * <pre>
    * - dM/dt u
    * </pre>
    * where <code>u</code> is the current system velocity.
    * <code>f</code> will be sized appropriately by the system.
    *
    * @param M returns the mass matrix
    * @param f returns the mass forces
    * @param t current time
    */
   public void getMassMatrix (
      SparseNumberedBlockMatrix M, VectorNd f, double t);

   /** 
    * Sets <code>Minv</code> to the inverse of the mass matrix <code>M</code>.
    * <code>Minv</code> should have been previously created with a call to
    * {@link #buildMassMatrix buildMassMatrix()}.
    *
    * <p> This method assumes that <code>M</code> is block diagonal and hence
    * <code>Minv</code> has the same block structure. Although it is possible
    * to compute <code>Minv</code> by simply inverting each block of
    * <code>M</code>, the special structure of each mass block may permit
    * the system to do this in a highly optimized way.
    * 
    * @param Minv returns the inverted mass matrix
    * @param M mass matrix to invert
    */   
   public void getInverseMassMatrix (SparseBlockMatrix Minv, SparseBlockMatrix M);

   public void mulInverseMass (SparseBlockMatrix M, VectorNd a, VectorNd f);

   /** 
    * Builds a solve matrix for this system. This is done by adding blocks of
    * an appropriate type to the sparse block matrix <code>S</code>. On input,
    * <code>S</code> should be empty with zero size; it will be sized
    * appropriately by the system.  The resulting matrix should have all the
    * blocks required to store any combination of the mass matrix, the
    * force-position Jacobian, and the force-velocity Jacobian.
    * 
    * This method does not place actual values in the matrix; that must be done
    * by adding a mass matrix to it, or by calling {@link #addVelJacobian 
    * addVelJacobian()} or
    * {@link #addPosJacobian addPosJacobian()}.
    *
    * <p>A new solve matrix should be built whenever the system's structure
    * version (as returned by {@link #getStructureVersion 
    * getStructureVersion()}) changes.
    * 
    * @param S matrix in which the solve matrix will be built
    */
   public void buildSolveMatrix (SparseNumberedBlockMatrix S);

   /**
    * Returns information about the solve matrix for this system. This consists
    * of an or-ed set of flags, including {@link 
    * maspack.matrix.Matrix#SYMMETRIC SYMMETRIC} or {@link 
    * maspack.matrix.Matrix#SYMMETRIC POSITIVE_DEFINITE},
    * which aid in determining the best way to solve the matrix.
    * 
    * @return type information for the solve matrix
    */
   public int getSolveMatrixType();

   /**
    * Returns the current number of active components in this system.
    * This should remain unchanged as long as the current structure
    * version (returned by {@link #getStructureVersion getStructureVersion()} 
    * remains unchanged.
    * 
    * @return number of active components
    */
   public int numActiveComponents();

   /**
    * Returns the current number of parametric components in this system.
    * This should remain unchanged as long as the current structure
    * version (returned by {@link #getStructureVersion getStructureVersion()} 
    * remains unchanged.
    * 
    * @return number of parametric components
    */
   public int numParametricComponents();

   /**
    * Adds the current force-velocity Jacobian, scaled by <code>h</code>, to
    * the matrix <code>S</code>, which should have been previously created with
    * a call to {@link #buildSolveMatrix buildSolveMatrix()}. 
    * Addition fictitious forces associated
    * with the Jacobian can be optionally returned in the vector
    * <code>f</code>, which will be sized appropriately by the system.
    * 
    * @param S
    * matrix to which scaled Jacobian is to be added
    * @param f
    * if non-null, returns fictitious forces associated with the Jacobian
    * @param h
    * scale factor for the Jacobian
    */
   public void addVelJacobian (SparseNumberedBlockMatrix S, VectorNd f, double h);

   /**
    * Adds the current force-position Jacobian, scaled by <code>h</code>, to
    * the matrix <code>S</code>, which should have been previously created with
    * a call to {@link #buildSolveMatrix buildSolveMatrix()}.  Addition
    * fictitious forces associated with the Jacobian can be optionally returned
    * in the vector <code>f</code>, which will be sized appropriately by the
    * system.
    * 
    * @param S
    * matrix to which scaled Jacobian is to be added
    * @param f
    * if non-null, returns fictitious forces associated with the Jacobian
    * @param h
    * scale factor for the Jacobian
    */
   public void addPosJacobian (SparseNumberedBlockMatrix S, VectorNd f, double h);

   /**
    * Queries whether or not the matrix structure of the bilateral constraints
    * returned by this system is constant for a given structure version.
    * 
    * @return {@code true} if bilateral constraints have a constant structure
    */
   public boolean isBilateralStructureConstant();   
   
   /** 
    * Obtains the transpose of the current bilateral constraint matrix G for
    * this system. This is built and stored in <code>GT</code>. On input,
    * <code>GT</code> should be empty with zero size; it will be sized
    * appropriately by the system. The derivative term is returned
    * in <code>dg</code>; this is given by
    * <pre>
    * dG/dt u
    * </pre>
    * where <code>u</code> is the current system velocity.
    * <code>dg</code> will also be sized appropriately by the system.
    *
    * @param GT returns the transpose of G
    * @param dg returns the derivative term for G
    */
   public void getBilateralConstraints (SparseBlockMatrix GT, VectorNd dg);

   /** 
    * Obtains information for all the constraint directions returned
    * by the most recent call to {@link #getBilateralConstraints 
    * getBilateralConstraints()}.
    * The information is returned in the array <code>ginfo</code>,
    * which should contain preallocated
    * {@link artisynth.core.mechmodels.MechSystem.ConstraintInfo ConstraintInfo}
    * structures and should have a length greater or equal to
    * the column size of <code>GT</code> returned by
    * {@link #getBilateralConstraints getBilateralConstraints()}.
    *
    * @param ginfo
    * Array of
    * {@link artisynth.core.mechmodels.MechSystem.ConstraintInfo ConstraintInfo}
    * objects used to return the constraint information.
    */
   public void getBilateralInfo (ConstraintInfo[] ginfo);

   /** 
    * Supplies to the system the most recently computed bilateral constraint
    * forces. These are supplied in the form {@code lam*s}, where {@code lam}
    * is a vector of impulses and {@code s} is the inverse of the step size
    * used in the computation. <code>lam</code>, which should have a size
    * greater or equal to the column size of <code>GT</code> returned by {@link
    * #getBilateralConstraints getBilateralConstraints()}.
    * 
    * @param lam
    * When scaled by {@code s}, gives the bilateral constraint forces 
    * being supplied to the system.
    * @param s 
    * Scaling factor to be applied to the {@code lam}.
    */
   public void setBilateralForces (VectorNd lam, double s);

   /** 
    * Returns from the system the most recently computed bilateral constraint
    * forces.  These are stored in the vector <code>lam</code>, which should
    * have a size greater or equal to the column size of <code>GT</code>
    * returned by {@link #getBilateralConstraints getBilateralConstraints()}.  
    * For constraints which
    * where present in the previous solve step, the force values should equal
    * those which were set by the prevous call to {@link
    * #setBilateralForces setBilateralForces()}.  
    * Otherwise, values can be estimated from
    * previous force values (where appropriate), or supplied as 0.
    * 
    * @param lam
    * Bilateral constraint forces being returned from the system.
    */
   public void getBilateralForces (VectorNd lam);

   /** 
    * Obtains the transpose of the current unilateral constraint matrix N for this
    * system. This is built and stored in <code>NT</code>. On input,
    * <code>NT</code> should be empty with zero size; it will be sized
    * appropriately by the system. The derivative term is returned
    * in <code>dn</code>; this is given by
    * <pre>
    * dn/dt u
    * </pre>
    * where <code>u</code> is the current system velocity.
    * <code>dn</code> will also be sized appropriately by the system.
    *
    * @param NT returns the transpose of N
    * @param dn returns the derivative term for N
    */
   public void getUnilateralConstraints (SparseBlockMatrix NT, VectorNd dn);

   /** 
    * Obtains information for all the constraint directions returned
    * by the most recent call to {@link #getUnilateralConstraints 
    * getUnilateralConstraints()}.
    * The information is returned in the array <code>ninfo</code>,
    * which should contain preallocated
    * {@link artisynth.core.mechmodels.MechSystem.ConstraintInfo ConstraintInfo}
    * structures and should have a length greater or equal to
    * the column size of <code>NT</code> returned by
    * {@link #getUnilateralConstraints getUnilateralConstraints()}.
    *
    * @param ninfo
    * Array of
    * {@link artisynth.core.mechmodels.MechSystem.ConstraintInfo ConstraintInfo}
    * objects used to return the constraint information.
    */
   public void getUnilateralInfo (ConstraintInfo[] ninfo);

   /** 
    * Supplies to the system the most recently computed unilateral constraint
    * forces. These are supplied in the form {@code the*s}, where {@code the}
    * is a vector of impulses and {@code s} is the inverse of the step size
    * used in the computation. <code>the</code> should have a size greater or
    * equal to the column size of <code>NT</code> returned by {@link
    * #getUnilateralConstraints getUnilateralConstraints()}.
    * 
    * @param the
    * When scaled by {@code s}, gives the unilateral constraint forces 
    * being supplied to the system.
    * @param s 
    * Scaling factor to be applied to the {@code the}.
    */
   public void setUnilateralForces (VectorNd the, double s);

   /** 
    * Returns from the system the most recently computed unilateral constraint
    * forces.  These are stored in the vector <code>the</code>, which should
    * have a size greater or equal to the column size of <code>NT</code>
    * returned by {@link #getUnilateralConstraints getUnilateralConstraints()}.
    * For constraints which where present in the previous solve step, the
    * force values should equal those which were set by the previous call to
    * {@link #setUnilateralForces setUnilateralForces()}.  Otherwise,
    * values can be estimated from previous force values (where appropriate),
    * or supplied as 0.
    * 
    * @param the
    * Unilateral constraint forces being returned from the system.
    */
   public void getUnilateralForces (VectorNd the);

   /** 
    * Returns that maximum number of friction constraint set that may be added by
    * the method {@link #getFrictionConstraints getFrictionConstraints()}.
    * This is used to size the <code>finfo</code> array supplied to that
    * method.
    *
    * @return maximum friction constraint sets
    */
   public int maxFrictionConstraintSets();

   /** 
    * Obtains the transpose of the current friction constraint matrix D for
    * this system. This is built and stored in <code>DT</code>. On input,
    * <code>DT</code> should be empty with zero size; it will be sized
    * appropriately by the system.
    *
    * <p>Each column block in <code>DT</code> describes a friction constraint
    * set associated with one unilateral or bilateral constraint. Information
    * about each friction constraint set is returned in the array
    * <code>finfo</code>, which should contain preallocated 
    * {@link artisynth.core.mechmodels.MechSystem.FrictionInfo FrictionInfo}
    * structures and should have a length greater or 
    * equal to the value returned by
    * {@link #maxFrictionConstraintSets FrictionConstraintSets()}.
    *
    * @param DT returns the transpose of D
    * @param finfo returns information for each friction constraint set
    */
   public void getFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo);

   /**
    * Computes an adjustment to the active positions of the system (stored
    * in the vector <code>q</code>) by applying
    * a velocity <code>u</code> for time
    * <code>h</code>.  Where velocity equals position derivative,
    * this corresponds to computing
    * <pre>
    * q += h u.
    * </pre>
    * In other situations, such as where position is orientation expressed as a
    * quaternion and velocity is angular velocity, the system should perform
    * the analagous computation.
    *
    * <code>q</code> should have a size greater or equal to the value
    * returned by {@link #getActivePosStateSize getActivePosStateSize()}, and
    * <code>u</code> should have a size greater or equal to the value
    * returned by {@link #getActiveVelStateSize getActiveVelStateSize()}.
    * 
    * @param q positions to be adjusted
    * @param h length of time to apply velocity
    * @param u velocity to be applied
    */
   public void addActivePosImpulse (VectorNd q, double h, VectorNd u);

   /** 
    * Updates the constraints associated with this system to be consistent with
    * the current position and indicated time. This method should be called
    * between any change to position values and any call that obtains
    * constraint information (such as {@link #getBilateralConstraints
    * getBilateralConstraints()}.
    *
    * <p>Because contact computations are expensive, the constraints associated
    * with contact should only be computed if the flags {@link
    * #COMPUTE_CONTACTS} or {@link #UPDATE_CONTACTS} are specified. The former
    * calls for contact information to be computed from scratch, while the
    * latter calls for contact information to be modified to reflect changes in
    * body positions, while preserving the general contact state between
    * bodies.
    *
    * <p>In the process of updating the constraints, the system may determine
    * that a smaller step size is needed.  This is particulary true when
    * contact calculations show an unacceptable level of interpenetration.
    * A smaller step size can be recommended If so, it can indicate this through
    * the optional argument <code>stepAdjust</code>, if present.
    * 
    * @param t current time
    * @param stepAdjust
    * (optional) can be used to indicate whether the current advance
    * should be redone with a smaller step size.
    * @param flags information flags
    * @return true if the system contains constraints
    */
   public boolean updateConstraints (
      double t, StepAdjustment stepAdjust, int flags);
   
   /**
    * Updates all internal forces associated with this system to be
    * consistent with the current position and velocity and the indicated time
    * <code>t</code>. This method should be called between any change to
    * position or velocity values (such as through
    * {@link #setActivePosState setActivePosState()} or
    * {@link #setActiveVelState setActiveVelState()},
    * and any call to {@link #getActiveForces getActiveForces()}.
    *
    * <p>In the process of updating the forces, the system may determine that a
    * smaller step size is needed.  If so, it can indicate this through the
    * argument optional <code>stepAdjust</code>, if present.
    *
    * @param t
    * current time
    */
   public void updateForces (double t);
   
}
