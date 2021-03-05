/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.*;
import maspack.spatialmotion.Twist;
import maspack.properties.PropertyList;
import maspack.util.*;

import java.io.*;
import java.util.*;

import artisynth.core.util.*;

public class SkinMarker extends Marker {
   private PointSkinAttachment mySkinAttachment = null;

   public static PropertyList myProps =
      new PropertyList (SkinMarker.class, Marker.class);

   static {
      //myProps.add ("location", "marker location relative to frame", null, "NW");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public SkinMarker() {
      super();
      mySkinAttachment = new PointSkinAttachment(this);
      setAttached (mySkinAttachment);      
   }

   public SkinMarker (String name) {
      this();
      setName (name);
   }

   public SkinMarker (Point3d pos) {
      setPosition (pos);
      mySkinAttachment = new PointSkinAttachment(this);
      setAttached (mySkinAttachment); 
   }

   public SkinMarker (double x, double y, double z) {
      this();
      // XXX finish
   }

   @Override
   public void setAttached (DynamicAttachment ax) {
      if (ax != mySkinAttachment) {
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
 
   /** 
    * {@inheritDoc}
    */
  public int addTargetJacobian (SparseBlockMatrix J, int bi) {
      if (!isControllable()) {
         throw new IllegalStateException (
            "Target marker is not controllable");
      }
      return mySkinAttachment.addTargetJacobian (J, bi);
   }

   public void updateState() {
      mySkinAttachment.updatePosStates();
      mySkinAttachment.updateVelStates();      
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "attachment")) {
         tokens.offer (new StringToken ("attachment", rtok.lineno()));
         mySkinAttachment.scan (rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "attachment")) {
         mySkinAttachment.postscan (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      if (mySkinAttachment != null) {
         pw.print ("attachment=");
         mySkinAttachment.write (pw, fmt, ancestor);
      }
      // need to write attachment first so that refPos can overwrite
      super.writeItems (pw, fmt, ancestor);
   }

   public void updateAttachment() {
      // XXX finish
   }
   
   public PointSkinAttachment getAttachment() {
      return mySkinAttachment;
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      mySkinAttachment.scaleDistance (s);
   }

   public void updatePosState() {
      mySkinAttachment.updatePosStates();
   }

   public void updateVelState() {
      mySkinAttachment.updateVelStates();
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
   public void getHardReferences (List<ModelComponent> refs) {
      // nothing to do - mySkinAttachment would return this component as a hard
      // reference, but that's it
   }

   @Override
   public void getSoftReferences (List<ModelComponent> refs) {
      mySkinAttachment.getSoftReferences (refs);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      mySkinAttachment.updateReferences (undo, undoInfo);
   }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SkinMarker m = (SkinMarker)super.copy (flags, copyMap);

      // bit of a hack: enter the new marker in the copyMap so that
      // PointFem3dAttachment.copy() will be able to find it
      if (copyMap != null) {
	 copyMap.put (this, m);
      }
      m.mySkinAttachment = mySkinAttachment.copy (flags, copyMap);
      m.setAttached (m.mySkinAttachment);      
      return m;
   }
}
