/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*; //import artisynth.core.mechmodels.DynamicMechComponent.Activity;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.*;
import maspack.spatialmotion.Twist;
import maspack.properties.PropertyList;
import maspack.util.*;

import java.io.*;
import java.util.*;

import artisynth.core.util.*;

public class FrameMarker extends Marker {
   protected Point3d myRefPos;
   private PointFrameAttachment myFrameAttachment = null;

   public static PropertyList myProps =
      new PropertyList (FrameMarker.class, Marker.class);

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
      myProps.add ("location", "marker location relative to frame", null, "NW");
      // myProps.get ("renderProps").setDefaultValue (new
      // PointLineRenderProps());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FrameMarker() {
      super();
      myRefPos = new Point3d();
      myFrameAttachment = new PointFrameAttachment(this);
      setAttached (myFrameAttachment);      
   }

   public FrameMarker (String name) {
      this();
      setName (name);
   }

   public FrameMarker (Frame frame, Point3d loc) {
      this (loc);
      setFrame (frame);
   }

   public FrameMarker (Point3d loc) {
      this();
      myFrameAttachment.setLocation (loc);
   }

   public FrameMarker (double x, double y, double z) {
      this();
      myFrameAttachment.setLocation (new Point3d(x,y,z));
   }

   public FrameMarker (Frame frame, double x, double y, double z) {
      this();
      myFrameAttachment.setLocation (new Point3d(x,y,z));
      setFrame (frame);
   }

   @Override
   public void setAttached (DynamicAttachment ax) {
      if (ax != myFrameAttachment) {
         throw new IllegalArgumentException (
            "Changing the attachment for marker is not permitted");
      }
      super.setAttached (ax);
   }

// John Lloyd, Dec 2020: markers now have state, since state is
// now saved and restored for attached components (since Nov 26)
//   public boolean hasState() {
//      return false;
//   }

   public void getLocation (Point3d loc) {
      loc.set (myFrameAttachment.getLocation());
   }

   public Point3d getLocation() {
      return myFrameAttachment.getLocation();
   }

   public void setLocation (Point3d loc) {
      myFrameAttachment.setLocation (loc);
      updateState();
   }

   public void transformLocation (AffineTransform3dBase X) {
      Point3d loc = new Point3d (getLocation());
      loc.transform (X);
      setLocation (loc);
   }

   public void setWorldLocation (Point3d loc) {
      Point3d newLoc = new Point3d (loc);
      Frame frame = myFrameAttachment.getFrame();
      if (frame != null) {
         newLoc.inverseTransform (frame.getPose());
         myFrameAttachment.setLocation (newLoc);
      }
      updateState();
   }

   public Frame getFrame() {
      return myFrameAttachment.getFrame();
   }

   public void setFrame (Frame frame) {
      setFrame (frame, /*updateRefPos=*/true);
   }

   protected void setFrame (Frame frame, boolean updateRefPos) {
      if (frame != null) {
         if (updateRefPos) {
            myRefPos.transform (frame.myState.XFrameToWorld, getLocation());
         }
         myFrameAttachment.setFrame (frame, getLocation());
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);         
      }
      else {
         // not sure what to do here - is frame ever null?
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
      myFrameAttachment.updatePosStates();
      myFrameAttachment.updateVelStates();      
//      if (myFrame != null) {
//         if (!RigidBody.useExternalAttachments) {
//            updatePosState();
//            updateVelState();
//         }
//         else {
//            FrameState bodyState = myFrame.myState;
//            Twist velBody = myFrame.getVelocity();
//            myState.pos.transform (bodyState.XFrameToWorld, myLocation);
//
//            Vector3d vtmp = new Vector3d();
//            vtmp.sub (myState.pos, bodyState.pos);
//            vtmp.cross (velBody.w, vtmp);
//            myState.vel.add (vtmp, velBody.v);
//         }
//
//      }
//      else {
//         myState.pos.set (myLocation);
//         myState.vel.setZero();
//      }
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
      // need to write attachment first so that refPos can overwrite
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
      disp.sub (getPosition(), myRefPos);
      return disp;
   }

   public Point3d getRefPos() {
      return myRefPos;
   }

   public void setRefPos (Point3d referencePosition) {
      this.myRefPos.set (referencePosition);
   }

//   public void transformGeometry (
//      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
//      super.transformGeometry (gtr, context, flags);
//   }

   public void updateAttachment() {
      Frame frame = myFrameAttachment.getFrame();
      if (frame != null) {
         myRefPos.set (getPosition());
         myFrameAttachment.updateAttachment();
      }
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myRefPos.scale (s);
      myFrameAttachment.scale (s);
   }

   // public void applyForces() {
   //    myFrameAttachment.applyForces();
   // }

   public void updatePosState() {
      myFrameAttachment.updatePosStates();
   }

   public void updateVelState() {
      myFrameAttachment.updateVelStates();
   }

//   /**
//    * Adds a diagonal block to the Jacobian for this marker if the frame to
//    * which it is attached is active.
//    */
//   public int addAttachedSolveBlock (SparseNumberedBlockMatrix S) {
//      if (myFrame.isActive()) {
//         addSolveBlock (S);
//         return 1;
//      }
//      else {
//         setSolveIndex (-1);
//         return 0;
//      }
//   }

//   @Override
//   public void connectToHierarchy () {
//      if (myFrame == null) {
//         throw new InternalErrorException ("frame is not set");
//      }
//      super.connectToHierarchy ();
//      updateState();
//      myFrame.addMasterAttachment (myFrameAttachment);
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      if (myFrame == null) {
//         throw new InternalErrorException ("frame is not set");
//      }
//      super.disconnectFromHierarchy();
//      myFrame.removeMasterAttachment (myFrameAttachment);
//   }

//   @Override
//   public void getHardReferences (List<ModelComponent> refs) {
//      super.getHardReferences (refs);
//      if (myFrame == null) {
//         throw new InternalErrorException ("null frame");
//      }
//      refs.add (myFrame);
//   }

//   /**
//    * {@inheritDoc}
//    */
//   public void getAttachments (List<DynamicAttachment> list) {
//      list.add (getAttachment());
//   }

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
      FrameMarker m = (FrameMarker)super.copy (flags, copyMap);

      // bit of a hack: enter the new marker in the copyMap so that
      // PointFem3dAttachment.copy() will be able to find it
      if (copyMap != null) {
	 copyMap.put (this, m);
      }
      m.myFrameAttachment = myFrameAttachment.copy (flags, copyMap);
      m.setAttached (m.myFrameAttachment);      
      m.myRefPos = new Point3d(myRefPos);
      return m;
   }
}
