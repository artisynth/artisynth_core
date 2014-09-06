/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.ModelComponent;

import maspack.util.DataBuffer;
import maspack.matrix.Matrix;
import maspack.matrix.MatrixBlock;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.Vector3d;
import java.util.*;

public interface DynamicComponent extends ModelComponent, ForceEffector {

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

   /** M
    * Attach this component to another via a DynamicAttachment object.
    * 
    * @param attachment Specifies the attachment relationship between
    * this component and its master(s)
    */
   public void setAttached (DynamicAttachment attachment);


   public void addMasterAttachment (DynamicAttachment a);

   public void removeMasterAttachment (DynamicAttachment a);


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
    * Gets the mass of this component at a particular time.
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
   public int getMassForces (VectorNd f, double t, int idx);

   /** 
    * Inverts a mass for this component.
    * 
    * @param Minv matrix to return the inverse mass in
    * @param M matrix containing the mass to be inverted
    */
   public void getInverseMass (Matrix Minv, Matrix M);

   // public void getEffectiveMass (Matrix M);

   //public void invertMass (MatrixBlock Minv, MatrixBlock M);

   public void addSolveBlock (SparseNumberedBlockMatrix S);

   public MatrixBlock createSolveBlock ();

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
   
   /**
    * Sets the state of this DynamicMechComponent from that of
    * another.
    * 
    * @param c component from which the state is to be copied.
    */
   public void setState (DynamicComponent c);

   public void addPosImpulse (
      double[] xbuf, int xidx, double h, double[] vbuf, int vidx);

   public int getPosDerivative (double[] buf, int idx);

   public int getPosState (double[] buf, int idx);

   public int setPosState (double[] buf, int idx);

   public int getVelState (double[] buf, int idx);

   public int setVelState (double[] buf, int idx);

   public int setForce (double[] buf, int idx);

   public int getForce (double[] buf, int idx);

   public int getPosStateSize();

   public int getVelStateSize();

   public void zeroForces();

   public void zeroExternalForces();

   public void setForcesToExternal();
   
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
   
   
   // Flag stuff
   /**
    * Set flag
    */
   public void setFlag(int mask);
   
   /**
    * Check if a flag is set
    */
   public boolean checkFlag(int mask);
   
   /**
    * Clear a flag
    */
   public void clearFlag(int mask);
}
