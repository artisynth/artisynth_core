/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Matrix3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.util.DataBuffer;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.geometry.Boundable;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.materials.MaterialChangeEvent;
import artisynth.core.materials.HasMaterialState;
import artisynth.core.materials.MaterialStateObject;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;

public abstract class FemElement extends RenderableComponentBase
   implements Boundable, ScalableUnits, CopyableComponent,
              PropertyChangeListener, HasNumericState {
   
   /**
    * Describes the different element types.
    */
   public enum ElementClass {
      VOLUMETRIC,
      SHELL,
      MEMBRANE
   }
   
   public abstract ElementClass getElementClass();
   
   /**
    * Returns true if the shape functions for this element are linear or 
    * multilinear. Override to return false for quadratic and higher 
    * order elements.
    */
   public boolean isLinear() {
      return true;
   }
   
   protected double myDensity = 1000;
   protected PropertyMode myDensityMode = PropertyMode.Inherited;
   protected double myMass = 0;
   protected boolean myMassValidP = false;
   protected boolean myMassExplicitP = false;
   protected boolean myWarpingStiffnessValidP = false;
   protected boolean myRestVolumeValidP = false;
   protected double myRestVolume = 0;
   protected double myVolume = 0;
   protected boolean myInvertedP = false;
   protected Matrix3d myFg = null; // plastic deformation gradient component
   int myIndex;  // index number for associating with other info
   int myIntegrationIndex; // base index of element's integration points

   FemMaterial myMaterial = null;
   // Augmenting materials, used to add to behavior
   protected ArrayList<FemMaterial> myAugMaterials = null;
   // Auxiliary Materials are mainly used for implementing muscle fibres
   protected ArrayList<AuxiliaryMaterial> myAuxMaterials = null;

   protected int myStateVersion = 0;
   protected int myNumMaterialsWithState = -1;

   public static PropertyList myProps =
      new PropertyList (FemElement.class, RenderableComponentBase.class);

   static {
      myProps.add ("renderProps * *", "render properties", null);
      myProps.addInheritable (
         "density:Inherited", "density", FemModel.DEFAULT_DENSITY);
      myProps.add (
         "material", "model material parameters", null, "XE");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemMaterial getMaterial() {
      return myMaterial;
   }

   public <T extends FemMaterial> void setMaterial (T mat) {
      FemMaterial oldMat = getEffectiveMaterial();
      T newMat = (T)MaterialBase.updateMaterial (
         this, "material", myMaterial, mat);
      myMaterial = newMat;
      // issue change event in case solve matrix symmetry or state has changed:
      MaterialChangeEvent mce = 
      MaterialBase.symmetryOrStateChanged ("material", newMat, oldMat);
      if (mce != null) {
         if (mce.stateChanged()) {
            notifyStateVersionChanged();
         }
         notifyParentOfChange (mce);
      }
      //return newMat;
   }

   public FemMaterial getEffectiveMaterial () {
      if (myMaterial != null) {
         return myMaterial;
      }
      ModelComponent grandParent = getGrandParent();
      if (grandParent instanceof FemModel) {
         return ((FemModel)grandParent).myMaterial;
      }
      else {
         return null;
      }
   }

   public FemModel3d getFemModel() {
      ModelComponent gparent = getGrandParent();
      if (gparent instanceof FemModel3d) {
         return (FemModel3d)gparent;
      }
      else {
         return null;
      }
   }
   
   /* --- Augmenting materials --- */
   
   public void addAugmentingMaterial (FemMaterial mat) {
      if (myAugMaterials == null) {
         myAugMaterials = new ArrayList<FemMaterial>(4);
      }
      myAugMaterials.add (mat);
   }

   public boolean removeAugmentingMaterial (FemMaterial mat) {
      if (myAugMaterials != null) {
         return myAugMaterials.remove (mat);
      }
      else {
         return false;
      }
   }

   public int numAugmentingMaterials() {
      return myAugMaterials == null ? 0 : myAugMaterials.size();
   }

   public ArrayList<FemMaterial> getAugmentingMaterials() {
      return myAugMaterials;
   }


   /**
    * Queries if the effective material for this element, and all auxiliary
    * materials, are defined for non-positive deformation gradients.
    *
    * @return <code>true</code> if the materials associated with this
    * element are invertible
    */
   public boolean materialsAreInvertible() {
      FemMaterial mat = getEffectiveMaterial();
      if (!mat.isInvertible()) {
         return false;
      }
      if (myAugMaterials != null) {
         for (FemMaterial amat : myAugMaterials) {
            if (!amat.isInvertible()) {
               return false;
            }
         }
      }
      if (myAuxMaterials != null) {
         for (AuxiliaryMaterial amat : myAuxMaterials) {
            if (!mat.isInvertible()) {
               return false;
            }
         }
      }
      return true;
   }

   public void setPlasticDeformation (Matrix3d F0) {
      if (F0 == null) {
         myFg = null;
      }
      else {
         if (myFg == null) {
            myFg = new Matrix3d();
         }
         myFg.set (F0);
      }
      invalidateRestData();
   }

   public Matrix3d getPlasticDeformation() {
      return myFg;
   }


   // public void updateNodeMasses (double elementMass) {
   //    // double perNodeMass = elementMass / numNodes();
   //    FemNode[] nodes = getNodes();
   //    for (int i = 0; i < nodes.length; i++) {
   //       if (!nodes[i].isMassExplicit()) {
   //          // nodes[i].addMass (perNodeMass);
   //          // signal invalid, since mass is now computed in FemNode.getMass()
   //          nodes[i].invalidateMassIfNecessary (); 
   //       }
   //    }
   // }

   void invalidateNodeMasses () {
      FemNode[] nodes = getNodes();
      for (int i = 0; i < nodes.length; i++) {
         FemNode n = nodes[i];
         // node[i] could be null if this method is called early
         // during element construction or scanning         
         if (n != null) {
            nodes[i].invalidateMassIfNecessary();
         }
      }
   }
   
   protected void invalidateElementAndNodeMasses () {
      invalidateMassIfNecessary();
      invalidateNodeMasses();
   }

   static FemElement firstElement = null;

   public double getMass() {
      if (!myMassExplicitP && !myMassValidP) {
         myMass = computeMassFromDensity();
         myMassValidP = true;
      }
      return myMass;
   }

   public void setMass (double m) {
      myMass = m;
      myMassValidP = true;
   }

   /**
    * Queries whether the mass for this element has been explicitly set.
    * 
    * @return true if the mass has been explicitly set
    */
   public boolean isMassExplicit() {
      return myMassExplicitP;
   }
   
   /**
    * Sets the mass for this element to an explicit value. This means that
    * the mass will no longer be determined from the rest volume and density.
    * 
    * @param mass explicit mass value
    */  
   public void setExplicitMass (double mass) {
      myMass = mass;
      myMassExplicitP = true;
      myMassValidP = true;
   }
   
   /**
    * Unsets an explicit mass for this element. This means that
    * the mass will be determined from the rest volume and density.
    */
   public void unsetExplicitMass () {
      myMassExplicitP = false;
      myMassValidP = false;
   }  
   
  public void invalidateMassIfNecessary() {
      if (!myMassExplicitP) {
         myMassValidP = false;
      }
   }

   protected double computeMassFromDensity() {
      return myDensity * getRestVolume();
   }

   public void setDensity (double p) {
      myDensity = p;
      if (!myMassExplicitP) {
         invalidateElementAndNodeMasses ();
      }
      myDensityMode =
         PropertyUtils.propagateValue (
            this, "density", myDensity, myDensityMode);
   }

   public double getDensity() {
      return myDensity;
   }

   public void setDensityMode (PropertyMode mode) {
      myDensityMode =
         PropertyUtils.setModeAndUpdate (this, "density", myDensityMode, mode);
   }

   public PropertyMode getDensityMode() {
      return myDensityMode;
   }

   public int numNodes() {
      return getNodes().length;
   }

   /** 
    * Queries whether there is a one-to-one mapping between integration points
    * and nodes. If so, this means that nodal values of quantities such as
    * stress and volume can be adequately approximated by averaging the same
    * quantities from all the associated integration points.
    *
    * @return <code>true</code> if there is a one-to-one mapping between
    * integration points and nodes
    */
   public boolean integrationPointsMapToNodes() {
      return false;
   }
   
   /** 
    * Queries whether there is an interpolation mapping between integration points
    * and nodes. If so, this means that nodal values of quantities such as
    * stress and volume can be adequately approximated by averaging the same
    * quantities from all the associated integration points.
    *
    * @return <code>true</code> if there is an interpolation mapping between
    * integration points and nodes
    */
   public boolean integrationPointsInterpolateToNodes() {
      return false;
   }

   public abstract FemNode[] getNodes();

   public int getLocalNodeIndex (FemNode p) {
      FemNode[] nodes = getNodes();
      for (int i = 0; i < nodes.length; i++) {
         if (nodes[i] == p) {
            return i;
         }
      }
      return -1;
   }

   public boolean containsNode (FemNode p) {
      FemNode[] nodes = getNodes();
      for (int i = 0; i < nodes.length; i++) {
         if (nodes[i] == p) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns the most recently computed volume for this element, as determined
    * by computeVolume().
    * 
    * @return element volume
    */
   public double getVolume() {
      return myVolume;
   }

   /** 
    * Computes the volume associated with this element and stores the result in
    * the {@code myVolume} field.
    *
    * <p>The method should return the minimum Jacobian determinant ratio
    * (det(J)/det(J0)) over all integration points. A negative value indicates
    * that the element is "inverted" at one or more integration points.
    * 
    * @return minimum Jacobian determinant ratio
    */
   public abstract double computeVolumes();

   /**
    * Invalidate data that relies on the element's rest position,
    * such as rest Jacobians and the base stiffness for co-rotated
    * stiffness.
    */
   public void invalidateRestData() {
      myWarpingStiffnessValidP = false;
      myRestVolumeValidP = false;
   }

   public double getRestVolume () {
      if (!myRestVolumeValidP) {
         computeRestVolumes();
         myRestVolumeValidP = true;
      }
      return myRestVolume;
   }
   
   public void updateRestVolumeAndMass() {
      if (!myRestVolumeValidP) {
         computeRestVolumes();
         invalidateNodeMasses();
         invalidateMassIfNecessary();
         myRestVolume = getRestVolume();
      }
   }

   /** 
    * Computes the rest volume associated with this element and stores the
    * result in the {@code myRestVolume} field.
    *
    * <p>The method should return the minimum Jacobian determinant (det(J0))
    * over all integration points. A negative value indicates that the element
    * is "inverted" in its rest position at one or more integration points.
    * 
    * @return minimum Jacobian determinant
    */
   public abstract double computeRestVolumes();

   @Deprecated
   /**
    * Warping now mostly handled separately by stiffness warper
    */
   protected abstract void updateWarpingStiffness(double weight);

   //public abstract void computeWarping();

   protected void printNodeReferences (
      PrintWriter pw, CompositeComponent ancestor) throws IOException {
      pw.println ("nodes=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      FemNode[] nodes = getNodes();
      for (int i = 0; i < nodes.length; i++) {
         pw.print (ComponentUtils.getWritePathName (ancestor, nodes[i])+" ");
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      int cnt;

      rtok.nextToken();     
      if (scanAttributeName (rtok, "mass")) {
         double mass = rtok.scanNumber();
         setExplicitMass (mass);
         return true;
      } 
      else if ((cnt=scanAndStoreReferences (rtok, "nodes", tokens)) >= 0) {
         if (cnt != numNodes()) {
            throw new IOException (
               "Expecting "+numNodes()+" node references, got "+cnt+
               ", line " + rtok.lineno());
         }
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }
   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "nodes")) {
         FemNode[] refs = ScanWriteUtils.postscanReferences (
            tokens, FemNode.class, ancestor);
         FemNode[] nodes = getNodes();
         for (int i=0; i<nodes.length; i++) {
            nodes[i] = refs[i];
         }
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      printNodeReferences (pw, ancestor);
      super.writeItems (pw, fmt, ancestor);
      if (myMassExplicitP) {
         pw.println ("mass=" + fmt.format(myMass));
      }
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createLineFaceProps (this);
   }

   public void prerender (RenderList list) {
      // nothing to do
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      FemNode[] nodes = getNodes();
      for (int i = 0; i < nodes.length; i++) {
         nodes[i].getPosition().updateBounds (pmin, pmax);
      }
   }  

   public abstract void render(
      Renderer renderer, RenderProps rprops, int flags);
   
   public void render (Renderer renderer, int flags) {
      render(renderer, myRenderProps, flags);
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   /* --- ScalableUnits --- */

   public void scaleDistance (double s) {
      myDensity /= (s * s * s);
      myVolume *= (s * s * s);
      myRestVolume *= (s * s * s);      
      //      myE /= s;
      invalidateRestData();
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
      if (myMaterial != null) {
         myMaterial.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      myDensity *= s;
      if (myMassExplicitP) {
         myMass *= s;
      }
      // myE *= s;
      invalidateRestData();
   }

   /**
    * Returns a string giving the node numbers associated with this element.
    * 
    * @return node index string
    */
   public String numberString() {
      StringBuilder buf = new StringBuilder (256);
      FemNode[] nodes = getNodes();
      for (int i = 0; i < nodes.length; i++) {
         if (i > 0) {
            buf.append (' ');
         }
         buf.append (nodes[i].getNumber());
      }
      return buf.toString();
   }

   /**
    * Used by the internal FEM code to mark whether or not this element is 
    * inverted. An element is inverted if the deformation gradient has a 
    * non-positive determinant at one or more integration points.
    * 
    * @param inverted if <code>true</code>, marks this element as inverted.
    */
   public void setInverted (boolean inverted) {
      myInvertedP = inverted;
   }

   /**
    * Returns true if this element has been marked as inverted by the FEM code.
    * 
    * @return true if this element has been marked as inverted.
    */
   public boolean isInverted() {
      return myInvertedP;
   }

   public boolean getMarkerCoordinates (
      VectorNd coords, Vector3d ncoords, Point3d pnt, boolean checkInside) {
      return false;
   }

   /** 
    * Returns true if at least one node in this element is active
    * 
    * @return true if one or more nodes are active
    */
   public boolean hasActiveNodes() {
      FemNode[] nodes = getNodes();
      for (int i=0; i<nodes.length; i++) {
         if (nodes[i].isActive()) {
            return true;
         }
      }
      return false;
   }

   /** 
    * Returns true if at least one node in this element is controllable
    * 
    * @return true if one or more nodes are controllable
    */
   public boolean hasControllableNodes() {
      FemNode[] nodes = getNodes();
      for (int i=0; i<nodes.length; i++) {
         if (nodes[i].isControllable()) {
            return true;
         }
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return false;
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      return false;
   }

   public int getIndex() {
      return myIndex;
   }
   
   public void setIndex(int idx) {
      myIndex = idx;
   }
   
   /**
    * Returns the index of this element's first integration point with respect
    * to it's FEM model.
    * @see #setIntegrationIndex
    * @return index of first integration point
    */
   public int getIntegrationIndex () {
      return myIntegrationIndex;
   }
   
   /**
    * Sets the index of this element's first integration point with respect to
    * it's FEM model. Used internally by FEM models to assign each integration
    * point an index, which is in turn used for caching Field values on a
    * per-integration point basis.
    * @see #getIntegrationIndex
    * @param idx assigned index of first integration point
    */
   public void setIntegrationIndex(int idx) {
      myIntegrationIndex = idx;
   }

   /**
    * {@inheritDoc}
    */
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      for (FemNode n : getNodes()) {
         refs.add (n);
      }
   }

   @Override
   public FemElement copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemElement e = (FemElement)super.copy (flags, copyMap);

      e.myDensityMode = PropertyMode.Inherited;
      e.myDensity = myDensity;

      e.myMass = myMass;
      if (myMassExplicitP) {
         e.myMassValidP = true;
         e.myMassExplicitP = true;
      }
      else {
         e.myMassValidP = false;
         e.myMassExplicitP = false;
      }
      e.myWarpingStiffnessValidP = false;
      e.myRestVolumeValidP = false;
      e.myRestVolume = 0;
      e.myVolume = 0;
      
      e.myInvertedP = false;
      e.setMaterial (myMaterial);
      e.myAugMaterials = null;
      if (myAugMaterials != null) {
         // ??? do we want to copy augmenting materials by reference?
         e.myAugMaterials = new ArrayList<FemMaterial>(myAugMaterials.size());
         e.myAugMaterials.addAll (myAugMaterials);
      }
      e.myAuxMaterials = null;
      if (myAuxMaterials != null) {
         for (AuxiliaryMaterial a : myAuxMaterials) {
            try {
               e.addAuxiliaryMaterial ((AuxiliaryMaterial)a.clone());
            }
            catch (Exception ex) {
               throw new InternalErrorException (
                  "Can't clone " + a.getClass());
            }
            
         }
      }

      return e;
   }

   public void propertyChanged (PropertyChangeEvent e) {
      if (e instanceof MaterialChangeEvent) {
         MaterialChangeEvent mce = (MaterialChangeEvent)e;
         invalidateRestData();
         if (mce.stateChanged() && e.getHost() == getMaterial()) {
            notifyStateVersionChanged(); // clear element material state 
         }
         if (mce.stateOrSymmetryChanged()) {
            notifyParentOfChange (new MaterialChangeEvent (this, mce));  
         }
      }
   }

   // Implementation of Boundable 

   public void computeCentroid (Vector3d centroid) {
      centroid.setZero();
      FemNode[] nodes = getNodes();
      for (int i = 0; i < nodes.length; i++) {
         centroid.add (nodes[i].getPosition());
      }
      centroid.scale (1.0 / nodes.length);
   }

   public double computeCovariance (Matrix3d C) {
      // Optional method - don't compute by default.
      return -1;
   }

   public int numPoints() {
      return numNodes();
   }

   public Point3d getPoint (int idx) {
      return getNodes()[idx].getPosition();
   }


   /* --- Auxiliary materials, used for implementing muscle fibres --- */
   
   public void addAuxiliaryMaterial (AuxiliaryMaterial mat) {
      if (myAuxMaterials == null) {
         myAuxMaterials = new ArrayList<AuxiliaryMaterial>(4);
      }
      myAuxMaterials.add (mat);
   }

   public boolean removeAuxiliaryMaterial (AuxiliaryMaterial mat) {
      if (myAuxMaterials != null) {
         return myAuxMaterials.remove (mat);
      }
      else {
         return false;
      }
   }

   public int numAuxiliaryMaterials() {
      return myAuxMaterials == null ? 0 : myAuxMaterials.size();
   }

   public AuxiliaryMaterial[] getAuxiliaryMaterials() {
      if (myAuxMaterials == null) {
         return new AuxiliaryMaterial[0];
      }
      return myAuxMaterials.toArray (new AuxiliaryMaterial[0]);
   }

   /* --- partial implementation of HasNumericState --- */
   
   public boolean hasState() {
      if (myNumMaterialsWithState == -1) {
         updateStateObjects();
      }
      return myNumMaterialsWithState != 0;
   }
  
   protected void collectMaterialsWithState (
      ArrayList<HasMaterialState> mats) {
      if (getEffectiveMaterial().hasState()) {
         mats.add (getEffectiveMaterial());
      }
      FemModel3d fem = getFemModel();
      if (fem != null) {
         ArrayList<FemMaterial> amats = fem.getAugmentingMaterials();
         if (amats != null) {
            for (FemMaterial mat : amats) {
               if (mat.hasState()) {
                  mats.add (mat);
               }
            }
         }
      }
      if (myAugMaterials != null) {
         for (FemMaterial mat : myAugMaterials) {
            if (mat.hasState()) {
               mats.add (mat);
            }
         }
      }
      if (myAuxMaterials != null) {
         for (AuxiliaryMaterial aux: myAuxMaterials) {
            if (aux.hasState()) {
               mats.add (aux);
            }
         }
      }
   }

   public abstract void notifyStateVersionChanged();

   protected abstract void updateStateObjects();

   public int getStateVersion() {
      return myStateVersion;
   }

}
