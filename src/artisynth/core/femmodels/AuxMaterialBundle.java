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
import maspack.matrix.Point3d;
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
import artisynth.core.modelbase.CompositeComponentBase;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.util.TransformableGeometry;

public class AuxMaterialBundle extends CompositeComponentBase 
   implements RenderableComponent, TransformableGeometry {

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

   public void setMaterial (FemMaterial mat) {
      myMat = (FemMaterial)MaterialBase.updateMaterial (
         this, "material", myMat, mat);
      // issue DynamicActivityChange in case solve matrix symmetry has changed:
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);      
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

//   /** 
//    * Returns true if this bundle references a specified FEM element.
//    */   
//   public boolean usesElement (FemElement3d e) {
//      for (ModelComponent c : e.getBackReferences()) {
//         if (c instanceof AuxMaterialElementDesc) {
//            AuxMaterialElementDesc desc = (AuxMaterialElementDesc)c;
//            if (desc.getGrandParent() == this) {
//               return true;
//            }
//         }
//      }
//      return false;
//   }
//
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

   public void updateBounds (Point3d pmin, Point3d pmax) {
   }

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.getAlpha() != 1) {
         code |= TRANSLUCENT;
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
   public void connectToHierarchy () {
      super.connectToHierarchy ();
      if (getAncestorFem (this) != null) {
         for (int i=0; i<myElementDescs.size(); i++) {
            myElementDescs.get(i).referenceElement();
         }
      }
   }

   @Override
   public void disconnectFromHierarchy() {
      if (getAncestorFem (this) != null) {
         for (int i=0; i<myElementDescs.size(); i++) {
            myElementDescs.get(i).dereferenceElement();
         }
      }
      super.disconnectFromHierarchy();
   }

   public void transformGeometry(AffineTransform3dBase X) {
      transformGeometry(X, this, 0);
   }

   public void transformGeometry(AffineTransform3dBase X,
      TransformableGeometry topObject, int flags) {
      for (AuxMaterialElementDesc elemDesc : myElementDescs) {
         elemDesc.transformGeometry(X, topObject, flags);
      }
      
   }

}
