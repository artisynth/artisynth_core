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
 * Component that implements attachment between dynamic components.
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
public abstract class DynamicAttachmentBase extends ModelComponentBase 
  implements DynamicAttachmentComp {

   public static boolean useNewConnect = true;

   private boolean mySlaveAffectsStiffnessP = true;

   public boolean slaveAffectsStiffness() {
      return mySlaveAffectsStiffnessP;
   }
   
   public void setSlaveAffectsStiffness (boolean affects) {
      mySlaveAffectsStiffnessP = affects;
   }
   
   public abstract DynamicComponent[] getMasters();

   public int numMasters() {
      return getMasters().length;
   }
   
   public abstract void invalidateMasters();

   public boolean oneMasterActive() {
      DynamicComponent[] masters = getMasters();
      for (int i = 0; i < masters.length; i++) {
         if (masters[i].isActive()) {
            return true;
         }
      }
      return false;
   }

   public static void addBackRefs (DynamicAttachment at) {
      DynamicComponent[] masters = at.getMasters();
      if (masters != null) {
         for (int i = 0; i < masters.length; i++) {
            masters[i].addMasterAttachment (at);
         }     
      }
   }
   
   public static void addBackRefsIfConnected (
      ModelComponent comp, DynamicAttachment at) {
      if (comp.getParent() != null) {
         // check comp.getParent(0 first because at.getMasters() may not work
         // if comp==at and is still being initialized
         DynamicComponent[] masters = at.getMasters();
         if (masters != null) {
            for (int i=0; i<masters.length; i++) {
               DynamicComponent m = masters[i];
               if (m != null && ComponentUtils.haveCommonAncestor (comp, m)) {
                  m.addMasterAttachment (at);
               }
            }     
         }      
      }
   }
   
   public static void addNewlyConnectedBackRefs (
      ModelComponent comp, DynamicAttachment at, CompositeComponent connector) {
      DynamicComponent[] masters = at.getMasters();
      if (masters != null) {
         for (int i=0; i<masters.length; i++) {
            DynamicComponent m = masters[i];
            if (ComponentUtils.areConnectedVia (comp, m, connector)) {
               m.addMasterAttachment (at);
            }
         }     
      }      
   }
   
   public static void removeBackRefs (DynamicAttachment at) {
      DynamicComponent[] masters = at.getMasters();
      if (masters != null) {
         for (int i = 0; i < masters.length; i++) {
            masters[i].removeMasterAttachment (at);
         }     
      }
   }
   
   public static void removeBackRefsIfConnected (
      ModelComponent comp, DynamicAttachment at) {
      if (comp.getParent() != null) {
         // check comp.getParent(0 first because at.getMasters() may not work
         // if comp==at and is still being initialized
         DynamicComponent[] masters = at.getMasters();
         if (masters != null) {
            for (int i=0; i<masters.length; i++) {
               DynamicComponent m = masters[i];
               if (m != null && ComponentUtils.haveCommonAncestor (comp, m)) {
                  m.removeMasterAttachment (at);
               }
            }     
         }
      }
   }
   
   public static void removeNewlyDisconnectedBackRefs (
      ModelComponent comp, DynamicAttachment at, CompositeComponent connector) {
      DynamicComponent[] masters = at.getMasters();
      if (masters != null) {
         for (int i=0; i<masters.length; i++) {
            DynamicComponent m = masters[i];
            if (ComponentUtils.areConnectedVia (comp, m, connector)) {
               m.removeMasterAttachment (at);
            }
         }
      }
   }
   
   /**
    * Every master component should contain a back reference to each
    * attachment that references it. This method adds the back reference
    * for this attachment to each of the masters.
    */
   public void addBackRefs() {
      addBackRefs (this);
   }
   
   /**
    * Removes the back reference to this attachment's slave component
    * from each of the master component.
    */
   public void removeBackRefs() {
      removeBackRefs (this);
   }
 
   /**
    * Calls {@link #addBackRefs} if this attachment is currently
    * connected to the hierarchy.
    */
   protected void addBackRefsIfConnected() {
      if (getSlave() instanceof Marker) {
         addBackRefsIfConnected (getSlave(), this);
      }
      else {
         addBackRefsIfConnected (this, this);
      }
      // if (isConnectedToHierarchy()) {
      //    addBackRefs();
      // }
   }

   /**
    * Calls {@link #removeBackRefs} if this attachment is currently
    * connected to the hierarchy.
    */
   protected void removeBackRefsIfConnected() {
      if (getSlave() instanceof Marker) {
         removeBackRefsIfConnected (getSlave(), this);
      }
      else {
         removeBackRefsIfConnected (this, this);
      }
      // if (isConnectedToHierarchy()) {
      //    removeBackRefs();
      // }
   }

  /**
    * Returns the slave DynamicMechComponent associated with this attachment.
    * In some cases, the attachment may connect some other entity (such
    * as a mesh vertex) to the master components, in which case this method
    * should return <code>null</code>.  
    * 
    * @return slave DynamicMechComponent, if any
    */
   public abstract DynamicComponent getSlave();
   
   public abstract void updatePosStates();

   public abstract void updateVelStates();

   /**
    * Update attachment to reflect changes in the slave state.
    */
   public abstract void updateAttachment();

   public abstract void applyForces();

   public abstract void addMassToMasters();

   public abstract boolean getDerivative (double[] buf, int idx);

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
   public abstract void mulSubGTM (
      MatrixBlock D, MatrixBlock M, int idx);

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
   public abstract void mulSubMG (
      MatrixBlock D, MatrixBlock M, int idx);

   /**
    * Returns the transpose of the constraint matrix G associated
    * with the idx-th master component.
    * 
    * @param idx index of the master component
    * @return transpose of G associated with idx
    */
   public abstract MatrixBlock getGT (int idx);

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
   public abstract void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx);

   public void getHardReferences (List<ModelComponent> refs) {
      DynamicComponent s = getSlave();
      if (s != null) {
         refs.add (s);
      }
      for (DynamicComponent m : getMasters()) {
         refs.add (m);
      }
   }

   /**
    * Update the attachment position state whenever we connect to the parent
    * (i.e., plug in to the hierarchy).
    */
   public void connectToHierarchy (CompositeComponent hcomp) {
      if (useNewConnect) {
         updatePosStates(); // do we need this?
         super.connectToHierarchy (hcomp);
         DynamicComponent slave = getSlave();
         if (slave != null &&
             ComponentUtils.areConnectedVia (this, slave, hcomp)) {
            slave.setAttached (this);
         }
         addNewlyConnectedBackRefs (this, this, hcomp);
      }
      else {
         if (hcomp == getParent()) {
            updatePosStates();
         }
         super.connectToHierarchy (hcomp);
         if (hcomp == getParent()) {
            DynamicComponent slave = getSlave();
            if (slave != null) {
               slave.setAttached (this);
            }
            addBackRefs();
         }
      }
   }
   
   /**
    * Update the attachment position state whenever we connect to the parent
    * (i.e., plug in to the hierarchy).
    */
   public void disconnectFromHierarchy (CompositeComponent hcomp) {
      super.disconnectFromHierarchy (hcomp);
      if (useNewConnect) {
         DynamicComponent slave = getSlave();
         if (slave != null &&
             ComponentUtils.areConnectedVia (this, slave, hcomp)) {
            slave.setAttached (null);
         }
         removeNewlyDisconnectedBackRefs (this, this, hcomp);
      }
      else {
         if (hcomp == getParent()) {
            DynamicComponent slave = getSlave();
            if (slave != null) {
               slave.setAttached (null);
            }
            removeBackRefs();
         }
      }
   }

   public Object clone() throws CloneNotSupportedException {
      DynamicAttachmentBase a = (DynamicAttachmentBase)super.clone();
      a.mySlaveAffectsStiffnessP = true;
      return a;
   }

   public DynamicAttachmentBase copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      DynamicAttachmentBase a =
         (DynamicAttachmentBase)super.copy (flags, copyMap);
      a.mySlaveAffectsStiffnessP = true;
      return a;
   }   
  
}
