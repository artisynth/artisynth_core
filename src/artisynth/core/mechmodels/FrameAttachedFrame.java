/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.util.*;

import java.io.*;
import java.util.*;

/**
 * A Frame that is rigidly attached to another Frame, analogous to
 * FrameMarker, which is a Point attached to a Frame.
 */
public class FrameAttachedFrame extends AttachedFrame {

   protected FrameFrameAttachment myFrameAttachment = null;

   public static PropertyList myProps =
      new PropertyList (FrameAttachedFrame.class, AttachedFrame.class);

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a new FrameAttachedFrame with default (identity) pose and no
    * master frame assigned.
    */
   public FrameAttachedFrame() {
      super();
      myFrameAttachment = new FrameFrameAttachment (this);
      setAttached (myFrameAttachment);
   }

   /**
    * Creates a new FrameAttachedFrame with the specified name, default
    * (identity) pose, and no master frame assigned.
    *
    * @param name name of the frame
    */
   public FrameAttachedFrame (String name) {
      this();
      setName (name);
   }

   /**
    * Creates a new FrameAttachedFrame with the specified world pose and no
    * master frame assigned.
    *
    * @param TFW transform from this frame to world coordinates
    */
   public FrameAttachedFrame (RigidTransform3d TFW) {
      this();
      setPose (TFW);
   }

   /**
    * Creates a new FrameAttachedFrame with the specified world pose and
    * attaches it to the given master frame. The relative transform between
    * this frame and the master is computed from their current world poses.
    *
    * @param frame master frame to attach to
    * @param TFW transform from this frame to world coordinates
    */
   public FrameAttachedFrame (Frame frame, RigidTransform3d TFW) {
      this (TFW);
      setFrame (frame);
   }

   @Override
   public void setAttached (DynamicAttachment ax) {
      if (ax != myFrameAttachment) {
         throw new IllegalArgumentException (
            "Changing the attachment for FrameAttachedFrame is not permitted");
      }
      super.setAttached (ax);
   }

   /**
    * Returns the Frame to which this frame is attached, or {@code null} if
    * no master frame has been set.
    *
    * @return master frame, or {@code null}
    */
   public Frame getFrame() {
      return myFrameAttachment.getMaster();
   }

   /**
    * Attaches this frame to the specified master frame. The relative
    * transform between this frame and the master is computed from their
    * current world poses.
    *
    * @param frame master frame to attach to
    */
   public void setFrame (Frame frame) {
      if (frame != null) {
         myFrameAttachment.set (frame, getPose());
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
      updateState();
   }

   /**
    * Returns the transform from this frame to its master frame. The returned
    * value is a copy and may be modified without affecting the attachment.
    *
    * @return transform from this frame to the master frame
    */
   public RigidTransform3d getTFM() {
      return new RigidTransform3d (myFrameAttachment.getTFM());
   }

   /**
    * Sets the transform from this frame to its master frame and updates the
    * world pose accordingly.
    *
    * @param TFM transform from this frame to the master frame
    */
   public void setTFM (RigidTransform3d TFM) {
      myFrameAttachment.setTFM (TFM);
      updateState();
   }

   /**
    * Sets the world pose of this frame by specifying the transform from this
    * frame to world coordinates. The relative transform to the master frame is
    * updated accordingly.
    *
    * @param TFW transform from this frame to world coordinates
    */
   public void setTFW (RigidTransform3d TFW) {
      myFrameAttachment.setCurrentTFW (TFW);
   }

   public void updateState() {
      myFrameAttachment.updatePosStates();
      myFrameAttachment.updateVelStates();
   }

   public void updateAttachment() {
      Frame frame = myFrameAttachment.getMaster();
      if (frame != null) {
         myFrameAttachment.updateAttachment();
      }
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myFrameAttachment.scaleDistance (s);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "attachment")) {
         tokens.offer (new StringToken ("attachment", rtok.lineno()));
         myFrameAttachment.scan (rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "attachment")) {
         myFrameAttachment.postscan (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      if (myFrameAttachment != null) {
         pw.print ("attachment=");
         myFrameAttachment.write (pw, fmt, ancestor);
      }
      super.writeItems (pw, fmt, ancestor);
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
   public FrameAttachedFrame copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FrameAttachedFrame f = (FrameAttachedFrame)super.copy (flags, copyMap);

      if (copyMap != null) {
         copyMap.put (this, f);
      }
      f.myFrameAttachment = myFrameAttachment.copy (flags, copyMap);
      f.setAttached (f.myFrameAttachment);
      return f;
   }
}
