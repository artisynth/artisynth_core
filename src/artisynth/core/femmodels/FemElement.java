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
import java.util.List;
import java.util.Map;

import maspack.matrix.Point3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;

public abstract class FemElement extends RenderableComponentBase
   implements ScalableUnits, CopyableComponent, PropertyChangeListener {
   
   protected double myDensity = 1000;
   protected PropertyMode myDensityMode = PropertyMode.Inherited;
   protected double myMass = 0;
   protected boolean myWarpingStiffnessValidP = false;
   protected boolean myRestVolumeValidP = false;
   protected double myRestVolume = 0;
   protected double myVolume = 0;
   protected boolean myInvertedP = false;
   int myIndex;  // index number for associating with other info

   FemMaterial myMaterial = null;

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

   public void setMaterial (FemMaterial mat) {
      myMaterial = (FemMaterial)MaterialBase.updateMaterial (
         this, "material", myMaterial, mat);
      // issue DynamicActivityChange in case solve matrix symmetry has changed:
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
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

   public void updateNodeMasses (double elementMass) {
      double perNodeMass = elementMass / numNodes();
      FemNode[] nodes = getNodes();
      for (int i = 0; i < nodes.length; i++) {
         nodes[i].addMass (perNodeMass);
      }
   }
   
   protected void updateElementAndNodeMasses () {
      double newMass = myDensity * getRestVolume();
      updateNodeMasses (newMass - myMass);
      myMass = newMass;      
   }

   static FemElement firstElement = null;

   public void setMass (double m) {
      myMass = m;
   }

   public void setDensity (double p) {
      myDensity = p;
      updateElementAndNodeMasses ();
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
    * Returns true if there is a one-to-one mapping between integration points
    * and nodes. If so, this means that nodal values of quantities such as
    * stress and volume can be adequately approximated by averaging the same
    * quantities from all the associated integration points.
    */
   public boolean integrationPointsMapToNodes() {
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
    * Computes the volume and partial volumes associated with this element. 
    * The volume result is stored in the element's myVolume 
    * field, and the partial volumes are stored in the myVolumes field.
    * 
    * @return minimum Jacobian value resulting from volume computation
    */
   public abstract double computeVolumes();

   public double getMass() {
      return myMass;
   }

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
         myRestVolume = computeRestVolumes();
         myRestVolumeValidP = true;
      }
      return myRestVolume;
   }
   
   public void updateRestVolumeAndMass() {
      if (!myRestVolumeValidP) {
         double oldVol = myRestVolume;
         double newVol = computeRestVolumes();
         updateNodeMasses ((newVol*myDensity)-myMass);
         myMass = newVol*myDensity;
         myRestVolume = newVol;
      }
   }

   /** 
    * Computes the rest volume and partial rest volumes associated with this
    * element.
    * 
    * @return element rest volume
    */     
   protected abstract double computeRestVolumes();

   protected abstract void updateWarpingStiffness();

   public abstract void computeWarping();

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
      if ((cnt=scanAndStoreReferences (rtok, "nodes", tokens)) >= 0) {
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
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createLineFaceProps (this);
   }

   public void prerender (RenderList list) {
      // nothing to do
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      // nothing to do
   }

   protected abstract void renderEdges (Renderer renderer, RenderProps props);

   public void render(Renderer renderer, RenderProps rprops, int flags) {
      
      if (rprops.getLineWidth() > 0) {
         switch (rprops.getLineStyle()) {
            case LINE: {
               renderer.setLightingEnabled (false);
               renderer.setLineWidth (rprops.getLineWidth());
               renderer.setColor (
                  rprops.getLineColorArray(), isSelected());
               renderEdges (renderer, rprops);
               renderer.setLineWidth (1);
               renderer.setLightingEnabled (true);
               break;
            }
            case CYLINDER: {
               renderer.setMaterialAndShading (
                  rprops, myRenderProps.getLineMaterial(), isSelected());
               renderEdges (renderer,rprops);
               renderer.restoreShading (rprops);
               break;
            }
            default:
               break;
         }
      }
   }
   
   public void render (Renderer renderer, int flags) {
      render(renderer, myRenderProps, flags);
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public void scaleDistance (double s) {
      myDensity /= (s * s * s);
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
      VectorNd coords, Point3d pnt, boolean checkInside) {
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
      e.myWarpingStiffnessValidP = false;
      e.myRestVolumeValidP = false;
      e.myRestVolume = 0;
      e.myVolume = 0;
      
      e.myInvertedP = false;
      e.setMaterial (myMaterial);

      return e;
   }

   public void propertyChanged (PropertyChangeEvent e) {
      if (e.getHost() instanceof FemMaterial) {
         invalidateRestData();
         if (e.getPropertyName().equals ("viscoBehavior")) {
            // issue a structure change event in order to invalidate WayPoints
            notifyParentOfChange (new StructureChangeEvent (this));
         }
      }
   }
   

}
