/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.List;
import java.util.Map;

import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import maspack.matrix.MatrixBlock;

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
 *
 */
public abstract class DynamicAttachmentBase extends ModelComponentBase 
  implements DynamicAttachmentComp, AttachingComponent {

   /*
      Dynamic attachments maintain back references in both the master and slave
      components that they are associated with. For master components, these
      are contained in the masterAttachments list. For slave components, it
      consists of the 'attachment' attribute.

      The component behavior for an attachment is designed so that back
      references are added or removed depending on whether the master or slave
      components are connected to the attachment (or its containing marker)
      within the component hierarchy. That in turn means that the
      connectToHierarchy() and disconnectFromHierarchy() methods for both
      attachments (or their containing markers) and dynamic components have to
      examine this connection status and update the references accordingly.

      As a simple illustration of how this is done, let A be an attachment and
      B a master or slave component. When A is first connected to the
      hierarchy, we check to see if B is reachable. If it is, then the back
      references are added. If it is not, then we post an attachment request
      with B so that B knows to check the connectivity to A when it connects to
      the hierarchy. Likewise, when A or B is disconnected, we check
      connectivity and remove references if necessary.

      In more detail, let C be the connection component in the
      connectToHierarchy() and disconnectFromHierarchy() calls. Assume also
      that B is a master component, so that the back references are maintained
      via addMasterAttachment() and removeMasterAttachment() methods. Then a
      general template for back reference maintenance is:
      
      A.connectToHierarchy (C) {
         if (ComponentUtils.areConnectedVia (A, B, C)) {
            // B is about to become connected to A
            B.addBackRefs()
         }
         else if (C == getParent()) {
            // A is first being connected to the hierarchy
            B.addAttachmentRequest (A);
         }
      }

      A.disconnectFromHierarchy (C) {
         if (ComponentUtils.areConnectedVia (A, B, C)) {
            // B is about to become unconnected from A
            B.removeMasterAttachment (A);
         }
         else if (B is NOT connected and C == getParent()) {
            B.removeAttachmentRequest (A);
         }
      }

      B.connectToHierarchy (C) {
         for (each connection request A) {
            if (ComponentUtils.areConnectedVia (B, A, C)) {
               // A is about to become connected to B
               A.connectAttachment (B);
               B.removeConnectionRequest (A);
            }
         }
      }

      B.disconnectFromHierarchy (C) {
         for (each master attachment A) {
            if (ComponentUtils.areConnectedVia (B, A, C)) {
               // A is about to become unconnected from B
               B.removeMasterAttachment (A);
               // add connection request for when B reconnects
               B.addConnectionRequest (A);
            }
         }
      }

      The connectAttachment(B) method supported by A performs the appropriate
      back referencing operation depending on whether B is a master or slave.
      Adjustments to the above template are required if B is a slave, or if
      A is contained within a marker but is not a component itself.
   */ 

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
   
   public boolean oneMasterNotAttached() {
      DynamicComponent[] masters = getMasters();
      for (int i = 0; i < masters.length; i++) {
         if (!masters[i].isAttached()) {
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
   
   public static void removeBackRefs (DynamicAttachment at) {
      DynamicComponent[] masters = at.getMasters();
      if (masters != null) {
         for (int i = 0; i < masters.length; i++) {
            masters[i].removeMasterAttachment (at);
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
      ModelComponent comp;
      if (getSlave() instanceof Marker) {
         comp = getSlave();
      }
      else {
         comp = this;
      }
      if (comp.getParent() != null) {
         // check comp.getParent(0 first because at.getMasters() may not work
         // if comp==at and is still being initialized
         DynamicComponent[] masters = getMasters();
         if (masters != null) {
            for (int i=0; i<masters.length; i++) {
               DynamicComponent m = masters[i];
               if (m != null && ComponentUtils.haveCommonAncestor (comp, m)) {
                  m.addMasterAttachment (this);
               }
            }     
         }      
      }      
   }

   /**
    * Calls {@link #removeBackRefs} if this attachment is currently
    * connected to the hierarchy.
    */
   protected void removeBackRefsIfConnected() {
      ModelComponent comp;
      if (getSlave() instanceof Marker) {
         comp = getSlave();
      }
      else {
         comp = this;
      }
      if (comp.getParent() != null) {
         // check comp.getParent(0 first because at.getMasters() may not work
         // if comp==at and is still being initialized
         DynamicComponent[] masters = getMasters();
         if (masters != null) {
            for (int i=0; i<masters.length; i++) {
               DynamicComponent m = masters[i];
               if (m != null && ComponentUtils.haveCommonAncestor (comp, m)) {
                  m.removeMasterAttachment (this);
               }
            }     
         }
      }
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

   public static void addConnectedMasterRefs (
      AttachingComponent ac, CompositeComponent hcomp) {
      DynamicAttachment at = ac.getAttachment();
      DynamicComponent[] masters = at.getMasters();
      if (masters != null) {
         for (int i=0; i<masters.length; i++) {
            DynamicComponent m = masters[i];
            if (ComponentUtils.areConnectedVia (ac, m, hcomp)) {
               m.addMasterAttachment (at);
            }
            else if (hcomp == ac.getParent()) {
               m.addAttachmentRequest (ac);
            }
         }     
      }  
   }
   
   /**
    * Update the attachment position state whenever we connect to the parent
    * (i.e., plug in to the hierarchy).
    */
   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      DynamicComponent slave = getSlave();
      if (slave != null) {
         if (ComponentUtils.areConnectedVia (this, slave, hcomp)) {
            updatePosStates(); // do we need this?
            slave.setAttached (this);
         }
         else if (hcomp == getParent()) {
            slave.addAttachmentRequest (this);
         }
      } 
      addConnectedMasterRefs (this, hcomp);
   }
   
   public static void removeConnectedMasterRefs (
      AttachingComponent ac, CompositeComponent hcomp) {
      DynamicAttachment at = ac.getAttachment();
      DynamicComponent[] masters = at.getMasters();
      if (masters != null) {
         for (int i=0; i<masters.length; i++) {
            DynamicComponent m = masters[i];
            int con = ComponentUtils.checkConnectivity (ac, m, hcomp);
            if (con == 1) {
               m.removeMasterAttachment (at);
            }
            else if (con == 0 && hcomp == ac.getParent()) {
               m.removeAttachmentRequest (ac);
            }
         }     
      }  
   }
   
   /**
    * Update the attachment position state whenever we connect to the parent
    * (i.e., plug in to the hierarchy).
    */
   public void disconnectFromHierarchy (CompositeComponent hcomp) {
      super.disconnectFromHierarchy (hcomp);
      DynamicComponent slave = getSlave();
      if (slave != null) {
         int con = ComponentUtils.checkConnectivity (this, slave, hcomp);
         if (con == 1) {
            slave.setAttached (null);
         }
         else if (con == 0 && hcomp == getParent()) {
            slave.removeAttachmentRequest (this);
         }
      } 
      removeConnectedMasterRefs (this, hcomp);
   }

   public void connectAttachment (DynamicComponent dcomp) {
      if (getSlave() == dcomp) {
         updatePosStates(); // do we need this?
         dcomp.setAttached (this);
      }
      else {
         // should we check to see if dcomp is a known master attachment?
         dcomp.addMasterAttachment (this);
      }
   }
   
   public DynamicAttachment getAttachment() {
      return this;
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
