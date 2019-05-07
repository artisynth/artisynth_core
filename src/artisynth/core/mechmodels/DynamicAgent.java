/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.HasNumericState;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformableGeometry;
import maspack.util.DataBuffer;
import maspack.matrix.Matrix;
import maspack.matrix.MatrixBlock;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.Vector3d;

import java.util.*;

public interface DynamicAgent extends HasNumericState {

   /**
    * Returns the slave attachment associated with this component, if any.
    * 
    * @return slave attachment, or null if the component is unattached
    */
   public DynamicAttachment getAttachment();

   /**
    * Returns a list of the attachments for which this component is a master, or
    * null if there are no such attachments. The returned list must not be
    * modified.
    * 
    * @return list of master attachments
    */
   public LinkedList<DynamicAttachment> getMasterAttachments();

   /**
    * Attach this component to another via a DynamicAttachment object.
    * This method is intended for internal use by attachment components.
    * 
    * @param attachment Specifies the attachment relationship between
    * this component and its master(s)
    */
   public void setAttached (DynamicAttachment attachment);

   /**
    * Add a DynamicAttachment to the list of master attachments associated
    * with this component. This method is intended for internal use by 
    * attachment components.
    * 
    * @param a master attachment to add
    */
   public void addMasterAttachment (DynamicAttachment a);

   /**
    * Removes a DynamicAttachment from the list of master attachments associated
    * with this component. This method is intended for internal use by 
    * attachment components.
    * 
    * @param a master attachment to remove
    */
   public void removeMasterAttachment (DynamicAttachment a);

   /**
    * Returns a list of Constrainers associated with this component. This list
    * is not necessarily complete; it is up to Constrainer objects themselves
    * to decide whether to add themselves to this list. Constrainers which are
    * in the list can be notified when aspects of this component change in a
    * way that requires the attention of the Constrainer. For example, calling
    * <code>transformGeometry()</code> on this component may require some of
    * its constrainers to be updated.
    * 
    * @return list of Constrainers associated with this component.
    */
   public List<Constrainer> getConstrainers();
   
   /**
    * Adds a Constrainer to the list returned by {@link #getConstrainers()}.
    * This method is intended for internal use by the Constrainer components
    * themselves.
    *
    * @param c Constrainer to add the constrainer list.
    */
   public void addConstrainer (Constrainer c);

   /**
    * Removes a Constrainer from the list returned by {@link
    * #getConstrainers()}.  This method is intended for internal use by the
    * Constrainer components themselves.
    *
    * @param c Constrainer to remove from the constrainer list.
    * @return <code>true</code> if the constrainer was present in the list.
    */
   public boolean removeConstrainer (Constrainer c);
   
   /**
    * Returns true if this component is dynamic. If a component is unattached,
    * then its state is determined by forces if it is dynamic, or
    * parametrically if it is non-dynamic.
    * 
    * @return true if this component is dynamic
    */
   public boolean isDynamic();

   /**
    * Returns true if this component is active. A component is active if
    * its position and velocity are determined by forces, which implies
    * that it is dynamic and unattached.
    * 
    * @return true if this component is active
    */
   public boolean isActive();

   /**
    * Returns true if this component is attached. The state of an attached
    * component is derived from the state of an attached <i>master</i>
    * component.
    * 
    * @return true if this component is attached
    */
   public boolean isAttached();

//   /**
//    * Returns true if this component is dynamic or attached.
//    * 
//    * @return true if this component is dynamic or attached.
//    */
//   public boolean isDynamicOrAttached();

   /**
    * Returns true if the state of this component is determined parametrically;
    * i.e., it is neither dynamic nor attached.
    *
    * @return true if this component's state is determined parametrically.
    */
   public boolean isParametric();

   /**
    * Returns true is this component is active, or it is attached to one
    * or more other components which ultimately are attached to at least
    * one active component.
    * 
    * @return true if this component is controllable.
    */
   public boolean isControllable();
   
   // public void setActivity(Activity a, DynamicMechComponent master);

   public int getSolveIndex();

   public void setSolveIndex (int idx);

//   public MatrixBlock getSolveBlock();

   /** 
    * Create a matrix block for representing the mass of this component,
    * initialized to the component's effective mass (instrinsic mass
    * plus the mass due to all attachmented components).
    */
   public MatrixBlock createMassBlock();

   public boolean isMassConstant();

   /** 
    * Returns the scalar mass of this component at time t.
    */
   public double getMass (double t);

   /** 
    * Gets the mass of this component at a particular time. Not currently
    * used; getEffectiveMass() is used instead.
    * 
    * @param M matrix to return the mass in
    * @param t current time
    */
   public void getMass (Matrix M, double t);

   /** 
    * Gets the mass forces for this component at a particular time. The
    * forces should be stored in <code>f</code>, starting at the location
    * specified by <code>idx</code>. Upon return, this method should
    * return the value of <code>idx</code> incremented by the dimension
    * of the mass forces.
    * 
    * @param f vector to return the forces in
    * @param t current time
    * @param idx starting location within <code>f</code>
    * where forces should be stored
    * @return updated value for <code>idx</code>
    */
   public int getEffectiveMassForces (VectorNd f, double t, int idx);

   /** 
    * Inverts a mass for this component.
    * 
    * @param Minv matrix to return the inverse mass in
    * @param M matrix containing the mass to be inverted
    */
   public void getInverseMass (Matrix Minv, Matrix M);

   /**
    * Resets the effective mass of this component to the nominal mass.
    */
   public void resetEffectiveMass();

   /** 
    * Gets the effective mass of this component at a particular time.  The
    * effective mass is the nominal mass plus any additional mass incurred from
    * attached components.
    * 
    * @param M matrix to return the mass in
    * @param t current time
    */
   public void getEffectiveMass (Matrix M, double t);
   
   /**
    * Gets the effective scalar mass of this component.  The
    * effective mass is the nominal mass plus any additional mass incurred from
    * attached components.
    * 
    * @return effective scalar mass
    */
   public double getEffectiveMass();

   public int mulInverseEffectiveMass (Matrix M, double[] a, double[] f, int idx);

   //public void invertMass (MatrixBlock Minv, MatrixBlock M);

   public void addSolveBlock (SparseNumberedBlockMatrix S);

//   public MatrixBlock createSolveBlock ();

//    /**
//     * Update the velocity states of attached components using an impulse-based
//     * method.
//     */
//    public void updateAttachmentVelStates();

//    /**
//     * Update the position states of attached components using an impulse-based
//     * method.
//     */
//    public void updateAttachmentPosStates();
   
//   /**
//    * Sets the state of this DynamicMechComponent from that of
//    * another.
//    * 
//    * @param c component from which the state is to be copied.
//    */
//   public void setState (DynamicAgent c);

   public void addPosImpulse (
      double[] xbuf, int xidx, double h, double[] vbuf, int vidx);

   public int getPosDerivative (double[] buf, int idx);

   public int getPosState (double[] buf, int idx);

   public int setPosState (double[] buf, int idx);

   public int getVelState (double[] buf, int idx);

   public int setVelState (double[] buf, int idx);

   public int setForce (double[] buf, int idx);
   
   public int addForce (double[] buf, int idx);

   public int getForce (double[] buf, int idx);

   public int getPosStateSize();

   public int getVelStateSize();

   public void zeroForces();

   public void zeroExternalForces();

   //public void setForcesToExternal();
   
   public void applyExternalForces();
   
   /**
    * Checks if the current component velocity exceeds specified limits. Used
    * to check solution stability.
    *
    * @param tlimit translational velocity limit
    * @param rlimit rotational velocity limit
    * @return true if velocity exceeds specified limits
    */
   public boolean velocityLimitExceeded (double tlimit, double rlimit);

   /**
    * Applies a gravity force to this component, given a prescribed
    * gravity acceleration vector.
    */
   public void applyGravity (Vector3d gacc);

   /**
    * Sets the position state of this component to a random value.  Used for
    * testing.
    */
   public void setRandomPosState();

   /**
    * Sets the velocity state of this component to a random value.  Used for
    * testing.
    */
   public void setRandomVelState();

   /**
    * Sets the force of this component to a random value.  Used for testing.
    */
   public void setRandomForce();
   
   /** 
    * Queries whether or not this component actually exerts its own
    * state-dependent forces (typically associated with damping). If
    * it does not, then the component makes no contribution to its
    * stiffness and damping matrices, a fact that can be exploited to
    * improve the efficiency of the physics solve.
    * 
    * <p>Any action that alters the return value of this method should
    * propagate a {@link StructureChangeEvent}. This can be
    * a <i>state-not-changed</i> event if component's state structure
    * is not altered (which it typically won't be).
    * 
    * @return <code>true</code> if this component exerts its own forces.
    */
   public boolean hasForce();

   public void getState (DataBuffer data);

   public void setState (DataBuffer data);

}
