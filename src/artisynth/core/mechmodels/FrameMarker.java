/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*; //import artisynth.core.mechmodels.DynamicMechComponent.Activity;
import maspack.matrix.*;
import maspack.spatialmotion.Twist;
import maspack.properties.PropertyList;
import maspack.util.*;

import java.io.*;
import java.util.*;

import artisynth.core.util.*;

public class FrameMarker extends Marker {
   protected Frame myFrame;
   protected Point3d myLocation; // location relative to the frame
   protected Point3d myRefPos;
   private PointFrameAttachment myFrameAttachment = null;

   public static PropertyList myProps =
      new PropertyList (FrameMarker.class, Point.class);

   static {
      // XXX can be replaced with functional probes
      myProps.add (
         "displacement", "displacement from reference position",
         null, "%.8g NW");
      myProps.addReadOnly (
         "displacementNorm *", "norm of displacement from reference position",
         null);
      myProps.add (
         "refPos * *", "reference position used to calculate displacement",
         null, "%.8g");
      myProps.add ("location", "marker location relative to frame", null);
      // myProps.get ("renderProps").setDefaultValue (new
      // PointLineRenderProps());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FrameMarker() {
      super();
      myLocation = new Point3d();
      myRefPos = new Point3d();
   }

   public FrameMarker (String name) {
      this();
      setName (name);
   }

   public FrameMarker (Frame frame, Point3d pos) {
      this (pos);
      setFrame (frame);
   }

   public FrameMarker (Point3d pos) {
      this();
      myLocation.set (pos);
   }

   public FrameMarker (double x, double y, double z) {
      this();
      myLocation.set (x, y, z);
   }

   public FrameMarker (Frame frame, double x, double y, double z) {
      this();
      myLocation.set (x, y, z);
      setFrame (frame);
   }

   /** 
    * FrameMarkers don't have state that needs to be saved and restored,
    * since their state is derived from their attached frames.
    */
   public boolean hasState() {
      return false;
   }

   public void getLocation (Point3d loc) {
      loc.set (myLocation);
   }

   public Point3d getLocation() {
      return myLocation;
   }

   public void setLocation (Point3d loc) {
      myLocation.set (loc);
      if (myFrameAttachment != null) {
         myFrameAttachment.setLocation (myLocation);
      }
      updateState();
   }

   public void transformLocation (AffineTransform3dBase X) {
      myLocation.transform (X);
      if (myFrameAttachment != null) {
         myFrameAttachment.setLocation (myLocation);
      }
   }

   public void setWorldLocation (Point3d loc) {
      myLocation.set (loc);
      if (myFrame != null) {
         myLocation.inverseTransform (myFrame.myState.XFrameToWorld);
         myFrameAttachment.setLocation (myLocation);
      }
      updateState();
   }

   public Frame getFrame() {
      return myFrame;
   }

   public void setFrame (Frame frame) {
      setFrame (frame, /*updateRefPos=*/true);
   }

   protected void setFrame (Frame frame, boolean updateRefPos) {
      if (getParent() != null) {
         throw new IllegalStateException (
            "Cannot set frame when marker is connected to component hierarchy");
      }
      myFrame = frame;
      if (myFrame != null) {
         if (updateRefPos) {
            myRefPos.transform (myFrame.myState.XFrameToWorld, myLocation);
         }
         myFrameAttachment = new PointFrameAttachment (frame, this, myLocation);
         setAttached (myFrameAttachment);
      }
      else {
         myFrameAttachment = null;
         setAttached (null);
      }
      updateState();
   }
 
   /** 
    * {@inheritDoc}
    */
  public int addTargetJacobian (SparseBlockMatrix J, int bi) {
      if (!isControllable()) {
         throw new IllegalStateException (
            "Target marker is not controllable");
      }
      return myFrameAttachment.addTargetJacobian (J, bi);
   }

   public void updateState() {
      if (myFrame != null) {
         if (!RigidBody.useExternalAttachments) {
            updatePosState();
            updateVelState();
         }
         else {
            FrameState bodyState = myFrame.myState;
            Twist velBody = myFrame.getVelocity();
            myState.pos.transform (bodyState.XFrameToWorld, myLocation);

            Vector3d vtmp = myState.vel; // use for temp storage
            vtmp.sub (myState.pos, bodyState.pos);
            vtmp.cross (velBody.w, vtmp);
            myState.vel.add (vtmp, velBody.v);
         }

      }
      else {
         myState.pos.set (myLocation);
         myState.vel.setZero();
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "frame", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "frame")) {
         setFrame (postscanReference (tokens, Frame.class, ancestor),
                   /*updateRefPos=*/false);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      // need to write frame first so that refPos can overwrite
      pw.println (
         "frame=" + ComponentUtils.getWritePathName (ancestor, myFrame));
      super.writeItems (pw, fmt, ancestor);
   }

   public double getDisplacementNorm() {
      return getDisplacement().norm();
   }

   public void setDisplacement (Point3d disp) {
      disp.add (myRefPos);
      setWorldLocation (disp);
   }
   
   public Point3d getDisplacement() {
      Point3d disp = new Point3d();
      disp.sub (myState.pos, myRefPos);
      return disp;
   }

   public Point3d getRefPos() {
      return myRefPos;
   }

   public void setRefPos (Point3d referencePosition) {
      this.myRefPos.set (referencePosition);
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      super.transformGeometry (X, topObject, flags);
   }

   public void updateAttachment() {
      myLocation.inverseTransform (
         myFrame.myState.XFrameToWorld, getPosition());

      if (myFrame != null) {
         myRefPos.transform (myFrame.myState.XFrameToWorld, myLocation);
         myFrameAttachment.updateAttachment();
      }
   }

   protected void scale (double s) {
      super.scaleDistance (s);
      myLocation.scale (s);
      myRefPos.scale (s);
      // if (myFrameAttachment != null)
      // { myFrameAttachment.setLocation (myLocation);
      // }
   }

   public void scaleDistance (double s) {
      scale (s);

      if (isAttached()) {
         getAttachment().updateAttachment();
      }
   }

   public void applyForces() {
      myFrameAttachment.applyForces();
   }

   public void updatePosState() {
      myFrameAttachment.updatePosStates();
   }

   public void updateVelState() {
      myFrameAttachment.updateVelStates();
   }

   /**
    * Adds a diagonal block to the Jacobian for this marker if the frame to
    * which it is attached is active.
    */
   public int addAttachedSolveBlock (SparseNumberedBlockMatrix S) {
      if (myFrame.isActive()) {
         addSolveBlock (S);
         return 1;
      }
      else {
         setSolveIndex (-1);
         return 0;
      }
   }

   @Override
   public void connectToHierarchy () {
      if (myFrame == null) {
         throw new InternalErrorException ("frame is not set");
      }
      super.connectToHierarchy ();
      updateState();
      myFrame.addMasterAttachment (myFrameAttachment);
   }

   @Override
   public void disconnectFromHierarchy() {
      if (myFrame == null) {
         throw new InternalErrorException ("frame is not set");
      }
      super.disconnectFromHierarchy();
      myFrame.removeMasterAttachment (myFrameAttachment);
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      if (myFrame == null) {
         throw new InternalErrorException ("null frame");
      }
      refs.add (myFrame);
   }

   /**
    * {@inheritDoc}
    */
   public void getAttachments (List<DynamicAttachment> list) {
      list.add (getAttachment());
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
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FrameMarker mkr = (FrameMarker)super.copy (flags, copyMap);
      mkr.myLocation = new Point3d();
      mkr.myRefPos = new Point3d();
      mkr.setLocation (myLocation);
      mkr.setRefPos (myRefPos);
      mkr.setFrame (myFrame);
      return mkr;
   }
}
