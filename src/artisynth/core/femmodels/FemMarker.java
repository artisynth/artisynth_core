/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import artisynth.core.mechmodels.Marker;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.util.*;

import java.io.*;

import artisynth.core.util.*;

import java.util.*;

public class FemMarker extends Marker {
   protected PointFem3dAttachment myNodeAttachment;

   public static PropertyList myProps =
      new PropertyList (FemMarker.class, Point.class);

   static {
      // myProps.add (
      // "refPos * *", "reference position", null);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemMarker() {
      super();
      myNodeAttachment = new PointFem3dAttachment(this);
      setAttached (myNodeAttachment);
   }

   public FemMarker (Point3d pos) {
      this();
      setPosition (pos);
   }

   public FemMarker (double x, double y, double z) {
      this();
      setPosition (x, y, z);
   }

   public FemMarker (FemElement elem, Point3d pos) {
      this (pos);
      setFromElement (elem);
   }

   public FemMarker (FemElement elem, double x, double y, double z) {
      this (new Point3d (x, y, z));
      setFromElement (elem);
   }

   @Override
   public void setAttached (DynamicAttachment ax) {
      if (ax != myNodeAttachment) {
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

   public VectorNd getCoordinates() {
      return myNodeAttachment.getCoordinates();
   }

   public FemElement getElement() {
      return myNodeAttachment.getElement();
   }

   /** 
    * {@inheritDoc}
    */
   public int addTargetJacobian (SparseBlockMatrix J, int bi) {
      if (!isControllable()) {
         throw new IllegalStateException (
            "Target marker is not controllable");
      }
      return myNodeAttachment.addTargetJacobian (J, bi);
   }

   public void setFromElement (FemElement elem) {
      if (elem != null) {
         myNodeAttachment.setFromElement (getPosition(), elem);
         myNodeAttachment.updateAttachment();
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
      else {
         // not sure what to do here ... is elem ever null?
      }
   }
   
   private void finishSet() {
      myNodeAttachment.updateAttachment();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);      
   }

   public void setFromFem (FemModel3d fem) {
      myNodeAttachment.setFromFem (getPosition(), fem);
      finishSet();
   }

   public void setFromNodes (
      Collection<? extends FemNode> nodes, VectorNd weights) {
      myNodeAttachment.setFromNodes (nodes, weights);
      finishSet();
   }

   public void setFromNodes (
      FemNode[] nodes, double[] weights) {
      myNodeAttachment.setFromNodes (nodes, weights);
      finishSet();
   }

   public boolean setFromNodes (
      Collection<? extends FemNode> nodes) {
      boolean status = myNodeAttachment.setFromNodes (getPosition(), nodes);
      finishSet();
      return status;
   }

   public boolean setFromNodes (FemNode[] nodes) {
      boolean status = myNodeAttachment.setFromNodes (getPosition(), nodes);
      finishSet();
      return status;
   }

   public void updateState() {
      myNodeAttachment.updatePosStates();
      myNodeAttachment.updateVelStates();
   }

   public void updatePosState() {
      myNodeAttachment.updatePosStates();
   }

   public void updateVelState() {
      myNodeAttachment.updateVelStates();
   }

   public void getRestPosition (Point3d pos) {
      myNodeAttachment.getRestPosition (pos);
   }

   public FemModel3d getFemModel() {
      return myNodeAttachment.getFemModel();
   }

   public void updateAttachment() {
      myNodeAttachment.updateAttachment();
   }
   
   /** 
    * Called when the marker is moved (or when we remesh underneath it)
    * and we need to redetermine which element this marker is embedded in.
    */
   public boolean resetElement (FemModel3d model) {
      return resetElement(model, true);
   }

   /** 
    * Called when the marker is moved (or when we remesh underneath it)
    * and we need to redetermine which element this marker is embedded in.
    */
   public boolean resetElement (FemModel3d model, boolean project) {
      Point3d pos = getPosition();
      Point3d res = new Point3d();
      FemElement updatedElement = model.findNearestElement (res, pos);
      if (project) {
         setPosition (res);
      } 
      if (updatedElement != getElement()) {
         myNodeAttachment.setFromElement(getPosition(), updatedElement);
         myNodeAttachment.updateAttachment();
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
         return true;
      }
      else {
         myNodeAttachment.updateAttachment();
         return false;
      }
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

   public void scaleDistance (double s) {
      // no need to scale since attachments are weight-based
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
   public FemMarker copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemMarker m = (FemMarker)super.copy (flags, copyMap);
      
      // bit of a hack: enter the new marker in the copyMap so that
      // PointFem3dAttachment.copy() will be able to find it
      if (copyMap != null) {
	 copyMap.put (this, m);       // EDIT: if copyMap is null, this throws an error!  Sanchez, Nov 30,2011
      }
      m.myNodeAttachment = myNodeAttachment.copy (flags, copyMap);
         // (PointFem3dAttachment)ComponentUtils.maybeGetCopy (
         //    flags, copyMap, myNodeAttachment);
      m.setAttached (m.myNodeAttachment);        

      return m;
   }

}
