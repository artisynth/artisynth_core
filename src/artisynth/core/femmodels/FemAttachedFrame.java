/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import artisynth.core.mechmodels.AttachedFrame;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.Frame;
import artisynth.core.modelbase.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.util.*;

import java.io.*;
import java.util.*;

import artisynth.core.util.*;

/**
 * A Frame attached to a FemModel3d, analogous to how FemMarker is a Point
 * attached to a FemModel3d.
 */
public class FemAttachedFrame extends AttachedFrame {
   protected FrameFem3dAttachment myNodeAttachment;

   public static PropertyList myProps =
      new PropertyList (FemAttachedFrame.class, Frame.class);

   static {
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a new FemAttachedFrame with default (identity) pose and no
    * FEM attachment assigned.
    */
   public FemAttachedFrame() {
      super();
      myNodeAttachment = new FrameFem3dAttachment (this);
      setAttached (myNodeAttachment);
   }

   /**
    * Creates a new FemAttachedFrame with the specified world pose and no
    * FEM attachment assigned.
    *
    * @param pose transform from this frame to world coordinates
    */
   public FemAttachedFrame (RigidTransform3d pose) {
      this();
      setPose (pose);
   }

   /**
    * Creates a new FemAttachedFrame with the specified world pose and attaches
    * it to the given FEM element. The attachment weights are computed from the
    * frame's world pose using natural coordinates within the element.
    *
    * @param elem FEM element to attach to
    * @param pose transform from this frame to world coordinates
    */
   public FemAttachedFrame (FemElement3dBase elem, RigidTransform3d pose) {
      this (pose);
      setFromElement (elem);
   }

   @Override
   public void setAttached (DynamicAttachment ax) {
      if (ax != myNodeAttachment) {
         throw new IllegalArgumentException (
            "Changing the attachment for FemAttachedFrame is not permitted");
      }
      super.setAttached (ax);
   }
   
   /**
    * {@inheritDoc}
    */
   public FrameFem3dAttachment getAttachment() {
      return myNodeAttachment;
   }

   /**
    * Returns the FEM element to which this frame is currently attached,
    * or {@code null} if the attachment is node-based rather than
    * element-based.
    *
    * @return element this frame is attached to, or {@code null}
    */
   public FemElement3dBase getElement() {
      return myNodeAttachment.getElement();
   }

   /**
    * Attaches this frame to the specified FEM element. The attachment
    * weights are computed from the frame's current pose using natural
    * coordinates within the element.
    *
    * @param elem element to attach the frame to
    */
   public void setFromElement (FemElement3dBase elem) {
      if (elem != null) {
         myNodeAttachment.setFromElement (getPose(), elem);
         myNodeAttachment.updateAttachment();
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
   }

   private void finishSet() {
      myNodeAttachment.updateAttachment();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   /**
    * Attaches this frame to the nearest element in the specified FEM model,
    * projecting the frame's origin onto the model surface if it lies outside.
    *
    * @param fem FEM model to attach to
    */
   public void setFromFem (FemModel3d fem) {
      myNodeAttachment.setFromFem (getPose(), fem);
      finishSet();
   }

   /**
    * Attaches this frame to the nearest element in the specified FEM model.
    * If the frame's origin lies outside the model and {@code project} is
    * {@code true}, it is projected onto the model surface; otherwise,
    * returns {@code false} without attaching.
    *
    * @param fem FEM model to attach to
    * @param project if {@code true}, project the origin onto the surface
    * when it lies outside the model
    * @return {@code false} if the origin is outside the model and
    * {@code project} is {@code false}
    */
   public boolean setFromFem (FemModel3d fem, boolean project) {
      boolean status = myNodeAttachment.setFromFem (getPose(), fem, project);
      finishSet();
      return status;
   }

   /**
    * Attaches this frame to a weighted combination of the specified nodes.
    *
    * @param nodes nodes to attach the frame to
    * @param weights weight for each node
    * @return {@code false} if the weighting computation did not fully converge
    */
   public boolean setFromNodes (
      Collection<FemNode3d> nodes, VectorNd weights) {
      boolean status = myNodeAttachment.setFromNodes (getPose(), nodes, weights);
      finishSet();
      return status;
   }

   /**
    * Attaches this frame to a weighted combination of the specified nodes.
    *
    * @param nodes nodes to attach the frame to
    * @param weights weight for each node
    * @return {@code false} if the weighting computation did not fully converge
    */
   public boolean setFromNodes (FemNode3d[] nodes, double[] weights) {
      boolean status = myNodeAttachment.setFromNodes (getPose(), nodes, weights);
      finishSet();
      return status;
   }

   /**
    * Attaches this frame to the specified nodes using inverse-distance
    * weighting based on the frame's current pose.
    *
    * @param nodes nodes to attach the frame to
    * @return {@code false} if the weighting computation did not fully converge
    */
   public boolean setFromNodes (Collection<FemNode3d> nodes) {
      boolean status = myNodeAttachment.setFromNodes (getPose(), nodes);
      finishSet();
      return status;
   }

   /**
    * Attaches this frame to the specified nodes using inverse-distance
    * weighting based on the frame's current pose.
    *
    * @param nodes nodes to attach the frame to
    * @return {@code false} if the weighting computation did not fully converge
    */
   public boolean setFromNodes (FemNode3d[] nodes) {
      boolean status = myNodeAttachment.setFromNodes (getPose(), nodes);
      finishSet();
      return status;
   }

   /**
    * Updates the pose and velocity of this frame from the current state
    * of its attached nodes.
    */
   public void updateState() {
      myNodeAttachment.updatePosStates();
      myNodeAttachment.updateVelStates();
   }

//   public void updatePosState() {
//      myNodeAttachment.updatePosStates();
//   }
//
//   public void updateVelState() {
//      myNodeAttachment.updateVelStates();
//   }

   public void updateAttachment() {
      myNodeAttachment.updateAttachment();
   }

   /**
    * Called when the frame is moved (or when we remesh underneath it)
    * and we need to redetermine which element this frame is embedded in.
    */
   public boolean resetElement (FemModel3d model) {
      return resetElement (model, /*project=*/true);
   }

   /**
    * Called when the frame is moved (or when we remesh underneath it)
    * and we need to redetermine which element this frame is embedded in.
    */
   public boolean resetElement (FemModel3d model, boolean project) {
      RigidTransform3d pose = getPose();
      Point3d res = new Point3d();
      FemElement3dBase updatedElement =
         model.findNearestElement (res, new Point3d(pose.p));
      if (project) {
         RigidTransform3d newPose = new RigidTransform3d (pose);
         newPose.p.set (res);
         setPose (newPose);
      }
      if (updatedElement != getElement()) {
         myNodeAttachment.setFromElement (getPose(), updatedElement);
         myNodeAttachment.updateAttachment();
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
         return true;
      }
      else {
         myNodeAttachment.updateAttachment();
         return false;
      }
   }

   public void scaleDistance (double s) {
      // no need to scale since attachments are weight-based
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "attachment")) {
         tokens.offer (new StringToken ("attachment", rtok.lineno()));
         myNodeAttachment.scan (rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "attachment")) {
         myNodeAttachment.postscan (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      if (myNodeAttachment != null) {
         pw.print ("attachment=");
         myNodeAttachment.write (pw, fmt, ancestor);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      return true;
   }

   @Override
   public FemAttachedFrame copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemAttachedFrame f = (FemAttachedFrame)super.copy (flags, copyMap);

      // bit of a hack: enter the new frame in the copyMap so that
      // FrameFem3dAttachment.copy() will be able to find it
      if (copyMap != null) {
         copyMap.put (this, f);
      }
      f.myNodeAttachment = myNodeAttachment.copy (flags, copyMap);
      f.setAttached (f.myNodeAttachment);
      return f;
   }
}
