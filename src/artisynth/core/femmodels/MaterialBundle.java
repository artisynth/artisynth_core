/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.LinkedList;
import java.util.*;
import java.io.*;

import maspack.matrix.AffineTransform3dBase;
import maspack.util.*;
import maspack.matrix.Vector3d;
import maspack.geometry.GeometryTransformer;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer;
import maspack.render.RenderObject;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.materials.MaterialChangeEvent;
import artisynth.core.util.*;
import artisynth.core.modelbase.*;
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

public class MaterialBundle extends RenderableComponentBase 
   implements TransformableGeometry, PropertyChangeListener {

   protected static FemMaterial DEFAULT_MATERIAL = null;
   
   protected FemMaterial myMat;
   private RenderObject myWidgetRob = null;  
   private boolean myWidgetRobValid = false;

   protected ArrayList<FemElement3dBase> myElements;
   protected boolean myUseAllElements = false;

   public static PropertyList myProps =
      new PropertyList (MaterialBundle.class, RenderableComponentBase.class);

   public boolean useAllElements() {
      return myUseAllElements;
   }

   public void setUseAllElements (boolean enable) {
      if (enable != myUseAllElements) {
         if (!myUseAllElements) {
            clearElements();
         }
         myUseAllElements = enable;
      }
   }
   
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
         "material", "aux material parameters", DEFAULT_MATERIAL, "CE");
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
   
   public MaterialBundle() {
      this (null);
   }

   public MaterialBundle (String name) {
      super ();
      setName (name);
      myElements = new ArrayList<FemElement3dBase>();
      //add (myElementDescs);
      setDefaultValues();
   }

   public MaterialBundle (String name, boolean useAllElements) {
      this (name);
      setUseAllElements (useAllElements);
   }

   
   public MaterialBundle (String name, FemMaterial mat, boolean useAllElements) {
      this(name);
      setMaterial(mat);
      setUseAllElements (useAllElements);
   }

   public FemMaterial getMaterial() {
      return myMat;
   }

   protected void notifyElementsOfMaterialStateChange () {
      // clear states for any elements that use the model material
      FemModel3d fem = getAncestorFem(this);
      if (fem != null) {
         if (useAllElements()) {
            fem.notifyElementsOfMaterialStateChange();
         }
         else {
            for (FemElement3dBase e : myElements) {
               e.notifyStateVersionChanged();
            }
         }
      }
   }

   protected void handlePossibleStateOrSymmetryChange (
      FemMaterial mat, FemElement3dBase elem) {
      if (mat.hasState() || !mat.hasSymmetricTangent()) {
         MaterialChangeEvent mce = new MaterialChangeEvent (
            this, "materialBundle", mat.hasState(), !mat.hasSymmetricTangent());
         notifyParentOfChange (mce);
         if (mce.stateChanged()) {
            if (elem != null) {
               elem.notifyStateVersionChanged();
            }
            else {
               // all elements are implicated
               notifyElementsOfMaterialStateChange();
            }
         }
      }
   }
   
   public <T extends FemMaterial> T setMaterial (T mat) {
      FemMaterial oldMat = myMat;
      T newMat = (T)MaterialBase.updateMaterial (
         this, "material", myMat, mat);
      myMat = newMat;
      if (!isScanning() && !useAllElements()) {
         // don't do this if we are scanning; elements aren't properly set yet
         // also not needed if using all elements
         if (oldMat != null) {
            removeMaterialFromElements (oldMat, myElements);
         }
         if (newMat != null){
            addMaterialToElements (newMat, myElements);
         }
      }
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
   
   // public void checkElementDesc (FemModel femMod, MaterialElementDesc desc) {
   //    if (!ModelComponentBase.recursivelyContains (
   //           femMod, desc.getElement())) {
   //       throw new IllegalArgumentException (
   //          "desc element not contained within FEM model");
   //    }
   // }

   public ArrayList<FemElement3dBase> getElements() {
      return myElements;
   }

   public void addElement (FemElement3dBase elem) {
      if (useAllElements()) {
         throw new IllegalStateException (
            "Bundle is set to use all elements; specific ones cannot be added");
      }
      FemModel femMod = getAncestorFem(this);
      if (femMod != null) {
         // we are connected
         if (!ModelComponentBase.recursivelyContains (femMod, elem)) {
            throw new IllegalArgumentException (
               "element not contained within FEM model");
         }
         if (myMat != null) {
            addMaterialToElement (elem, myMat);
            handlePossibleStateOrSymmetryChange (myMat, elem);
         }
      }
      myElements.add (elem);
   }

   public boolean removeElement (FemElement3dBase elem) {
      if (myElements.remove (elem)) {
         if (useAllElements()) {
            throw new InternalErrorException (
               "Bundle is set to use all elements, but still has elements");
         }
         FemModel femMod = getAncestorFem(this);
         if (femMod != null) {
            // we are connected
            if (myMat != null) {
               removeMaterialFromElement (elem, myMat);
               handlePossibleStateOrSymmetryChange (myMat, elem);
            }           
         }
         return true;
      }
      else {
         return false;
      }
   }

   public void clearElements() {
      FemModel femMod = getAncestorFem(this);
      if (femMod != null) {
         // we are connected
         if (myMat != null) {
            if (myElements.size() > 0) {
               if (useAllElements()) {
                  throw new InternalErrorException (
                     "Bundle is set to use all elements, but still has elements");
               }
               removeMaterialFromElements (myMat, myElements);
            }
            handlePossibleStateOrSymmetryChange (myMat, null);
         }
      }
      myElements.clear();
   }

   public int numElements () {
      return myElements.size();
   }
   
   public FemElement3dBase getElement (int idx) {
      return myElements.get(idx);
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

   protected void buildRenderObjectIfNecessary() {
      double wsize = getElementWidgetSize();
      if (!myWidgetRobValid || (wsize > 0) != (myWidgetRob != null)) {
         if (wsize > 0) {
            RenderObject r = new RenderObject();
            r.createTriangleGroup();
            for (FemElement3dBase e : myElements) {
               FemElementRenderer.addWidgetFaces (r, e);
            }
            myWidgetRob = r;
         }
         else {
            myWidgetRob = null;
         }
         myWidgetRobValid = true;
      }
   }

   public void prerender (RenderList list) {
      buildRenderObjectIfNecessary();
      if (myWidgetRob != null) {
         RenderObject r = myWidgetRob;
         double wsize = getElementWidgetSize();
         int pidx = 0;
         for (FemElement3dBase e : myElements) {
            pidx = FemElementRenderer.updateWidgetPositions (r, e, wsize, pidx);
         }
         FemElementRenderer.updateWidgetNormals (r, /*triangle group=*/0);
         r.notifyPositionsModified();      
      }  
   }

   public void render (Renderer renderer, int flags) {
      RenderProps props = myRenderProps;
      RenderObject r = myWidgetRob;
      if (r != null) {
         renderer.setFaceColoring (props, isSelected());
         renderer.drawTriangles (r);
      }
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      for (FemElement3dBase e : myElements) {
         e.updateBounds (pmin, pmax);
      }
   }

   /* --- end Renderable --- */

   public void scaleDistance (double s) {
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
      if (myMat != null) {
         myMat.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      if (myMat != null) {
         myMat.scaleMass (s);
      }     
   }

   private void addMaterialToElements (
      FemMaterial mat, ArrayList<FemElement3dBase> elems) {
      for (FemElement3dBase elem : elems) {
         addMaterialToElement (elem, mat);
      }
   }
   
   private void removeMaterialFromElements (
      FemMaterial mat, ArrayList<FemElement3dBase> elems) {
      for (FemElement3dBase elem : elems) {
         removeMaterialFromElement (elem, mat);
      }
   }

   private void addMaterialToElement (
      FemElement3dBase elem, FemMaterial mat) {
      elem.addAugmentingMaterial (mat);
   }
   
   private void removeMaterialFromElement (
      FemElement3dBase elem, FemMaterial mat) {
      elem.removeAugmentingMaterial (mat);
   }
   
   @Override
   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      if (hcomp == getParent() && getAncestorFem (this) != null) {
         if (myMat != null) {
            if (!useAllElements()) {
               addMaterialToElements (myMat, myElements);
            }
            handlePossibleStateOrSymmetryChange (myMat, null);
         }
      }
   }

   @Override
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      if (hcomp == getParent() && getAncestorFem (this) != null) {
         if (myMat != null) {
            if (!useAllElements()) {
               removeMaterialFromElements (myMat, myElements);
            }
            handlePossibleStateOrSymmetryChange (myMat, null);
         }
      }
      super.disconnectFromHierarchy(hcomp);
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }   

   public void transformGeometry(AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gt, TransformGeometryContext context, int flags) {
      // nothing to at the top level
   }

   // public void addTransformableDependencies (
   //    TransformGeometryContext context, int flags) {
   //    context.addAll (myElementDescs);
   // }  

   public void propertyChanged (PropertyChangeEvent e) {
      if (e instanceof MaterialChangeEvent) {
         FemModel3d fem = getAncestorFem(this);
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

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "useAllElements")) {
         setUseAllElements (rtok.scanBoolean());
         return true;
      }
      else if (scanAndStoreReferences (rtok, "elements", tokens) >= 0) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }      

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "elements")) {
         myElements = new ArrayList<FemElement3dBase>();
         ScanWriteUtils.postscanReferences (
            tokens, myElements, FemElement3dBase.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      if (useAllElements()) {
         pw.println ("useAllElements=true");
      }
      super.writeItems (pw, fmt, ancestor);
      pw.print ("elements=");
      ScanWriteUtils.writeBracketedReferences (pw, myElements, ancestor);
   }

   public void getSoftReferences (List<ModelComponent> refs) {
      refs.addAll (myElements);
   }

   // private boolean elementIsReferenced (int num) {
   //    return myFem.getElements().getByNumber(num) != null;
   // }

   // private boolean shellElementIsReferenced (int num) {
   //    return myFem.getShellElements().getByNumber(num) != null;
   // }

   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      // assume we connected, since otherwise updateReferences won't be called
      if (undo) {
         Object obj = undoInfo.peekFirst();
         if (obj != ModelComponentBase.NULL_OBJ) {
            ListRemove<FemElement3dBase> lremove =
               (ListRemove<FemElement3dBase>)obj;

            if (myMat != null) {
               addMaterialToElements (myMat, lremove.getRemoved());
               handlePossibleStateOrSymmetryChange (myMat, null);
            }
            myWidgetRobValid = false;
         }
         ComponentUtils.updateReferences (this, myElements, undo, undoInfo);
      }
      else {
         if (ComponentUtils.updateReferences (
                this, myElements, undo, undoInfo)) {
            ListRemove<FemElement3dBase> lremove =
               (ListRemove<FemElement3dBase>)undoInfo.peekLast();
            if (myMat != null) {
               removeMaterialFromElements (myMat, lremove.getRemoved());
               handlePossibleStateOrSymmetryChange (myMat, null);
            }
            myWidgetRobValid = false;
         }
      }      
   }

   // changes:
   /*

     change material
        if (connected) {
           dreference all elements from old material
           reference all elements to new material
        }

     change material property
        if (connected) {
           MaterialChangeEvent mce = checkTangentState (old, new);
           if (mce != null) {
              if (mce.stateChanged()) {
                 notifyElementsOfStateChange();
              }
              notfyParent();
           }
        }

     connect to hierarchy
        reference all elements
        MaterialChangeEvent mce = checkFromMaterial (mat);
        if (mce != null) {
           if (mce.stateChanged()) {
              notifyElementsOfStateChange();
           }
           notfyParent();
        }      

     remove from hierarchy
        dreference all elements
        MaterialChangeEvent mce = checkFromMaterial (mat);
        if (mce != null) {
           if (mce.stateChanged()) {
              notifyElementsOfStateChange();
           }
           notfyParent();
        }      

     // will only happen if not using all elements:

     addElement
        if (connected) {
           reference element
           MaterialChangeEvent mce = checkFromMaterial (mat);
           if (mce != null) {
              if (mce.stateChanged()) {
                 e.notifyStateVersionChanged();
              }
             notfyParent();
           }
        }

     removeElement
        if (connected) {
           dereference element
           MaterialChangeEvent mce = checkFromMaterial (mat);
           if (mce != null) {
              if (mce.stateChanged()) {
                 e.notifyStateVersionChanged();
              }
             notfyParent();
           }
        }

    */


   // TODO: scan/write

   // element check when adding/removing

   // rendering

   // soft references

   // @Override
   // public void componentChanged (ComponentChangeEvent e) {
   //    if (e instanceof StructureChangeEvent) {
   //       System.out.println ("structure changed");
   //    }
   // }
}
