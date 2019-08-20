/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.LinkedList;
import java.util.List;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Vector3d;
import maspack.geometry.GeometryTransformer;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.materials.MaterialChangeEvent;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CompositeComponentBase;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;

public class AuxMaterialBundle extends CompositeComponentBase 
   implements RenderableComponent, TransformableGeometry, 
   PropertyChangeListener {

   public enum FractionRenderType {
      ELEMENT, ELEMENT_SCALED, INTEGRATION_POINT, INTEGRATION_POINT_SCALED
   }
   static FractionRenderType DEFAULT_FRACTION_RENDER_TYPE = FractionRenderType.INTEGRATION_POINT_SCALED;
   static double DEFAULT_FRACTION_RENDER_RADIUS = 0;
   
   double fractionRenderRadius = DEFAULT_FRACTION_RENDER_RADIUS;
   PropertyMode fractionRenderRadiusMode = PropertyMode.Inherited;
   FractionRenderType fractionRenderType = DEFAULT_FRACTION_RENDER_TYPE;
   PropertyMode fractionRenderTypeMode = PropertyMode.Inherited;
   
   double myExcitation = 0.0;
  
   protected FemMaterial myMat;
   protected RenderProps myRenderProps;

   protected AuxMaterialElementDescList myElementDescs;

   public static PropertyList myProps =
      new PropertyList (AuxMaterialBundle.class, CompositeComponentBase.class);

   
   private double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   PropertyMode myElementWidgetSizeMode = PropertyMode.Inherited;

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createFaceProps (host);
      return props;
   }
  
   private static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.0;

   static {
      myProps.add ("renderProps", "render properties", defaultRenderProps(null));
      myProps.add (
         "material", "aux material parameters", createDefaultMaterial(), "CE");
      myProps.addInheritable (
         "elementWidgetSize:Inherited",
         "size of rendered widget in each element's center",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
      myProps.addInheritable (
         "fractionRenderRadius:Inherited",
         "length of directions rendered in each element",
         DEFAULT_FRACTION_RENDER_RADIUS);
      myProps.addInheritable(
         "fractionRenderType:Inherited",
         "method for rendering fiber directions (per element, or per inode)",
         DEFAULT_FRACTION_RENDER_TYPE);
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
      myElementWidgetSizeMode = PropertyMode.Inherited;
      setRenderProps (defaultRenderProps (null));
      setMaterial(createDefaultMaterial());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setElementWidgetSize (double size) {
      myElementWidgetSize = size;
      myElementWidgetSizeMode = 
         PropertyUtils.propagateValue (
            this, "elementWidgetSize",
            myElementWidgetSize, myElementWidgetSizeMode);
   }

   public double getElementWidgetSize () {
      return myElementWidgetSize;
   }

   public void setElementWidgetSizeMode (PropertyMode mode) {
      myElementWidgetSizeMode =
         PropertyUtils.setModeAndUpdate (
            this, "elementWidgetSize", myElementWidgetSizeMode, mode);
   }

   public PropertyMode getElementWidgetSizeMode() {
      return myElementWidgetSizeMode;
   }
   
   public void setFractionRenderRadius (double size) {
      fractionRenderRadius = size;
      fractionRenderRadiusMode = 
         PropertyUtils.propagateValue (
            this, "fractionRenderRadius",
            fractionRenderRadius, fractionRenderRadiusMode);
   }

   public double getFractionRenderRadius () {
      return fractionRenderRadius;
   }

   public void setFractionRenderRadiusMode (PropertyMode mode) {
      fractionRenderRadiusMode =
         PropertyUtils.setModeAndUpdate (
            this, "fractionRenderRadius", fractionRenderRadiusMode, mode);
   }

   public PropertyMode getFractionRenderRadiusMode() {
      return fractionRenderRadiusMode;
   }

   public void setFractionRenderType(FractionRenderType type) {
      fractionRenderType = type;
      fractionRenderTypeMode =
         PropertyUtils.propagateValue(
            this, "fractionRenderType",
            fractionRenderType, fractionRenderTypeMode);
   }

   public FractionRenderType getFractionRenderType() {
      return fractionRenderType;
   }

   public void setFractionRenderTypeMode(PropertyMode mode) {
      fractionRenderTypeMode =
         PropertyUtils.setModeAndUpdate(
            this, "fractionRenderType", fractionRenderTypeMode, mode);
   }

   public PropertyMode getFractionRenderTypeMode() {
      return fractionRenderTypeMode;
   }
   
   public static FemMaterial createDefaultMaterial() {
      return new LinearMaterial();
   }
   
   public AuxMaterialBundle() {
      this (null);
   }

   public AuxMaterialBundle (String name) {
      super (name);

       myElementDescs = new AuxMaterialElementDescList ("elementDescs", "e");

      add (myElementDescs);
      setDefaultValues();
   }

   
   public AuxMaterialBundle(String name, FemMaterial mat) {
      this(name);
      setMaterial(mat);
   }

   public FemMaterial getMaterial() {
      return myMat;
   }

   FemMaterial getEffectiveMaterial () {
      if (myMat != null) {
         return myMat;
      }
      myMat = createDefaultMaterial();
      return myMat;      
   }

   protected void notifyElementsOfMaterialStateChange () {
      // clear states for any elements that use the model material
      for (AuxMaterialElementDesc desc : myElementDescs) {
         desc.myElement.notifyStateVersionChanged();
      }
   }
   
   public <T extends FemMaterial> T setMaterial (T mat) {
      if (mat == null) {
         throw new IllegalArgumentException ("material 'mat' cannot be null");
      }
      FemMaterial oldMat = myMat;
      T newMat = (T)MaterialBase.updateMaterial (
         this, "material", myMat, mat);
      myMat = newMat;
      FemModel3d fem = getAncestorFem(this);
      if (fem != null) {
         // issue change event in case solve matrix symmetry or state has changed:
         MaterialChangeEvent mce = 
            MaterialBase.symmetryOrStateChanged ("material", newMat, oldMat);
         if (mce != null) {
            if (mce.stateChanged()) {
               notifyElementsOfMaterialStateChange();
            }
            notifyParentOfChange (mce);
         }      
         fem.invalidateStressAndStiffness();
         fem.invalidateRestData();
      }
      return newMat;
   }
   
   public void checkElementDesc (FemModel femMod, AuxMaterialElementDesc desc) {
      if (!ModelComponentBase.recursivelyContains (
             femMod, desc.getElement())) {
         throw new IllegalArgumentException (
            "desc element not contained within FEM model");
      }
   }

   public AuxMaterialElementDescList getElements() {
      return myElementDescs;
   }

   public void addElement (AuxMaterialElementDesc desc) {
      // check to make sure particles are already in the FEM
      FemModel femMod = getAncestorFem(this);
      if (femMod != null) {
         checkElementDesc (femMod, desc);
      }
      myElementDescs.add (desc);
   }
   
   public void addElement(FemElement3d elem, double frac) {
      AuxMaterialElementDesc desc = new AuxMaterialElementDesc(elem);
      desc.setFraction(frac);
      addElement(desc);
   }
   
   public void addElement(FemElement3d elem, double[] fracs) {
      AuxMaterialElementDesc desc = new AuxMaterialElementDesc(elem);
      desc.setFractions(fracs);
      addElement(desc);
   }

   public boolean removeElement (AuxMaterialElementDesc desc) {
      return myElementDescs.remove (desc);
   }

   public void clearElements() {
      myElementDescs.clear();
   }

   public static FemModel3d getAncestorFem (ModelComponent comp) {
      ModelComponent ancestor = comp.getParent();
      while (ancestor != null) {
         if (ancestor instanceof FemModel) {
            return (FemModel3d)ancestor;
         }
         ancestor = ancestor.getParent();
      }
      return null;
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps (this);
   }

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps = 
         RenderableComponentBase.updateRenderProps (this, myRenderProps, props);
   }

   public void prerender (RenderList list) {
      list.addIfVisible (myElementDescs);
   }

   public void render (Renderer renderer, int flags) {
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return false;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= TRANSPARENT;
      }
      return code;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public void scaleDistance (double s) {
      for (int i=0; i<myElementDescs.size(); i++) {
         myElementDescs.get(i).scaleDistance (s);
      }
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
      if (myMat != null) {
         myMat.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      for (int i=0; i<myElementDescs.size(); i++) {
         myElementDescs.get(i).scaleMass (s);
      }
   }

   @Override
   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      // XXX reference elements already connected recursively
      //      if (hcomp == getParent() && getAncestorFem (this) != null) {
      //         for (int i=0; i<myElementDescs.size(); i++) {
      //            myElementDescs.get(i).referenceElement();
      //         }
      //      }
   }

   @Override
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      if (hcomp == getParent() && getAncestorFem (this) != null) {
         for (int i=0; i<myElementDescs.size(); i++) {
            myElementDescs.get(i).dereferenceElement();
         }
      }
      super.disconnectFromHierarchy(hcomp);
   }

   public void transformGeometry(AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gt, TransformGeometryContext context, int flags) {
      // nothing to at the top level
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      context.addAll (myElementDescs);
   }   

   public void propertyChanged (PropertyChangeEvent e) {
      if (e instanceof MaterialChangeEvent) {
         FemModel fem = getAncestorFem(this);
         if (fem != null) {
            fem.invalidateStressAndStiffness();
            if (e.getHost() instanceof FemMaterial && 
                ((FemMaterial)e.getHost()).isLinear()) {
               // invalidate rest data for linear materials, to rebuild
               // the initial warping stiffness matrices
               fem.invalidateRestData();
            }
            MaterialChangeEvent mce = (MaterialChangeEvent)e;
            if (mce.stateChanged() && e.getHost() == getMaterial()) {
               notifyElementsOfMaterialStateChange();
            }
            if (mce.stateOrSymmetryChanged()) {
               notifyParentOfChange (new MaterialChangeEvent (this, mce));  
            }
         }
      }
   }

}
