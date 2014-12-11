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
   protected FemElement myElement;
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
      myElement = null;
      myNodeAttachment = null;
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
      setElement (elem);
   }

   public FemMarker (FemElement elem, double x, double y, double z) {
      this (new Point3d (x, y, z));
      setElement (elem);
   }

   /** 
    * FemMarkers don't have state that needs to be saved and restored,
    * since their state is derived from nodes to which they are attached.
    */
   public boolean hasState() {
      return false;
   }

   public VectorNd getCoordinates() {
      return myNodeAttachment.getCoordinates();
   }

   public FemElement getElement() {
      return myElement;
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

   public void setElement (FemElement elem) {
      if (getParent() != null) {
         throw new IllegalStateException (
            "Cannot set element when marker is connected to component hierarchy");
      }
      myElement = elem;
      if (myElement != null) {
         myNodeAttachment = new PointFem3dAttachment (this, elem);
         myNodeAttachment.updateAttachment();
         setAttached (myNodeAttachment);        
      }
      else {
         myNodeAttachment = null;
         setAttached (null);        
      }
      // updateState();
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

   public void applyForces() {
      myNodeAttachment.applyForces();
   }

   public void getRestPosition (Point3d pos) {
      myNodeAttachment.getRestPosition (pos);
   }

   public FemModel3d getFemModel() {
      ModelComponent ancestor;
      if (myElement == null) {
         return null;
      }
      if ((ancestor = myElement.getParent()) == null) {
         return null;
      }
      if ((ancestor = ancestor.getParent()) == null) {
         return null;
      }
      if (!(ancestor instanceof FemModel3d)) {
         return null;
      }
      return (FemModel3d)ancestor;
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
      FemElement updatedElement = model.findContainingElement (pos);
      if (updatedElement == null) {
         Point3d res = new Point3d();
         updatedElement = model.findNearestSurfaceElement (res,pos);
         if (project) {
            setPosition (res);
         } else {
            //setPosition(pos); //XXX no need to reposition
         }
      }
      if (updatedElement != myElement) {
         if (myElement != null) {
            // paranoid - expect myElement != null
            //myElement.removeBackReference (this);
         }
         myElement = updatedElement;
         myNodeAttachment.setFromElement(updatedElement);
         myNodeAttachment.updateAttachment();
         //myElement.addBackReference (this);
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
         return true;
      }
      else {
         myNodeAttachment.updateAttachment();
         return false;
      }
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      super.transformGeometry (X, topObject, flags);
      if (myElement != null) {
         FemModel3d femModel = getFemModel();
         if (femModel == null || !resetElement (femModel)) {
            myNodeAttachment.updateAttachment();
         }
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "elem", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "elem")) {
         setElement (postscanReference (tokens, FemElement3d.class, ancestor));
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.println ("elem="+ComponentUtils.getWritePathName (
                     ancestor, myElement));
   }

   public void scaleDistance (double s) {
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      if (myElement == null) {
         throw new InternalErrorException ("null element");
      }
      refs.add (myElement);
   }


   @Override
   public void connectToHierarchy () {
      if (myElement == null) {
         throw new InternalErrorException ("element is not set");
      }
      super.connectToHierarchy ();
      FemNode nodes[] = myNodeAttachment.getMasters();
      if (nodes != null) {
         for (FemNode node : nodes) {
            node.addMasterAttachment (myNodeAttachment);
         }
      }
   }

   @Override
   public void disconnectFromHierarchy() {
      if (myElement == null) {
         throw new InternalErrorException ("element is not set");
      }
      super.disconnectFromHierarchy();
      FemNode nodes[] = myNodeAttachment.getMasters();
      if (nodes != null) {
         for (FemNode node : nodes) {
            node.removeMasterAttachment (myNodeAttachment);
         }
      }
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
   public FemMarker copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemMarker m = (FemMarker)super.copy (flags, copyMap);

      m.myElement =
         (FemElement3d)ComponentUtils.maybeGetCopy (flags, copyMap, myElement);
      
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
