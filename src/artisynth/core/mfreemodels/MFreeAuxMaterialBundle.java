/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.LinkedList;
import java.util.List;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.IsRenderable;
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
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;

public class MFreeAuxMaterialBundle extends CompositeComponentBase 
   implements RenderableComponent, TransformableGeometry {

   double myExcitation = 0.0;
  
   protected FemMaterial myMat;
   protected RenderProps myRenderProps;

   protected MFreeAuxMaterialElementDescList myElementDescs;

   public static PropertyList myProps =
      new PropertyList (MFreeAuxMaterialBundle.class, CompositeComponentBase.class);

   
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
   
   public static FemMaterial createDefaultMaterial() {
      return new LinearMaterial();
   }
   
   public MFreeAuxMaterialBundle() {
      this (null);
   }

   public MFreeAuxMaterialBundle (String name) {
      super (name);

       myElementDescs = new MFreeAuxMaterialElementDescList ("elementDescs", "e");

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
   
   public void checkElementDesc (MFreeModel3d mod, MFreeAuxMaterialElementDesc desc) {
      if (!ModelComponentBase.recursivelyContains (
             mod, desc.getElement())) {
         throw new IllegalArgumentException (
            "desc element not contained within FEM model");
      }
   }

   public MFreeAuxMaterialElementDescList getElements() {
      return myElementDescs;
   }

//   /** 
//    * Returns true if this bundle references a specified FEM element.
//    */   
//   public boolean usesElement (MFreeElement3d e) {
//      for (ModelComponent c : e.getBackReferences()) {
//         if (c instanceof MFreeAuxMaterialElementDesc) {
//            MFreeAuxMaterialElementDesc desc = (MFreeAuxMaterialElementDesc)c;
//            if (desc.getGrandParent() == this) {
//               return true;
//            }
//         }
//      }
//      return false;
//   }
//
   public void addElement (MFreeAuxMaterialElementDesc desc) {
      // check to make sure particles are already in the FEM
      MFreeModel3d mod = getAncestorModel(this);
      if (mod != null) {
         checkElementDesc (mod, desc);
      }
      myElementDescs.add (desc);
   }

   public boolean removeElement (MFreeAuxMaterialElementDesc desc) {
      return myElementDescs.remove (desc);
   }

   public void clearElements() {
      myElementDescs.clear();
   }

   public static MFreeModel3d getAncestorModel (ModelComponent comp) {
      ModelComponent ancestor = comp.getParent();
      while (ancestor != null) {
         if (ancestor instanceof MFreeModel3d) {
            return (MFreeModel3d)ancestor;
         }
         ancestor = ancestor.getParent();
      }
      return null;
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createLineFaceProps (this);
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
   public void connectToHierarchy () {
      super.connectToHierarchy ();
      if (getAncestorModel (this) != null) {
         for (int i=0; i<myElementDescs.size(); i++) {
            myElementDescs.get(i).referenceElement();
         }
      }
   }

   @Override
   public void disconnectFromHierarchy() {
      if (getAncestorModel (this) != null) {
         for (int i=0; i<myElementDescs.size(); i++) {
            myElementDescs.get(i).dereferenceElement();
         }
      }
      super.disconnectFromHierarchy();
   }

   public void transformGeometry(AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      // nothing to at the top level
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      context.addAll (myElementDescs);
   } 
}
