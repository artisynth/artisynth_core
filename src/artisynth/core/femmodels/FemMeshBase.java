/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.color.ColorMapBase;
import maspack.render.color.HueColorMap;
import maspack.util.DoubleInterval;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.femmodels.FemModel.Ranging;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.SkinMeshBase;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.ScanToken;

/**
 * Describes a surface mesh that is "skinned" onto an FEM.
 **/
public abstract class FemMeshBase extends SkinMeshBase {

   protected static SurfaceRender defaultSurfaceRendering = SurfaceRender.Shaded;
   protected static Ranging defaultStressPlotRanging = Ranging.Auto;
   protected static DoubleInterval defaultStressPlotRange =
      new DoubleInterval(0,1);
   protected static ColorMapBase defaultColorMap =  new HueColorMap(2.0/3, 0);
   
   protected SurfaceRender mySurfaceRendering =  defaultSurfaceRendering;
   protected Ranging myStressPlotRanging = defaultStressPlotRanging;
   protected DoubleInterval myStressPlotRange =  new DoubleInterval(defaultStressPlotRange);

   protected PropertyMode myStressPlotRangeMode = PropertyMode.Inherited;
   protected PropertyMode myStressPlotRangingMode = PropertyMode.Inherited;
   protected PropertyMode mySurfaceRenderingMode = PropertyMode.Inherited;
   
   protected Color[] savedVertexColors;
   
   protected ArrayList<float[]> savedColors;
   protected int[] savedColorIndices;
   
   protected ColorMapBase myColorMap = defaultColorMap.copy();
   protected PropertyMode myColorMapMode = PropertyMode.Inherited;
   
   protected static double EPS = 1e-10;
   FemModel3d myFem;

   private float[] colorArray = new float[3];
   
   public static PropertyList myProps =
   new PropertyList (FemMeshBase.class, MeshComponent.class);

   static {
      myProps.addInheritable(
         "surfaceRendering:Inherited", 
         "either shaded, stress or strain", defaultSurfaceRendering, "NW");
      myProps.addInheritable (
         "stressPlotRanging:Inherited", "ranging mode for stress plots",
         defaultStressPlotRanging);         
      myProps.addInheritable (
         "stressPlotRange:Inherited", 
         "stress value range for color stress plots", defaultStressPlotRange);
      myProps.addInheritable(
         "colorMap:Inherited", "color map for stress/strain", 
         defaultColorMap, "CE");
   }
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemMeshBase () {
      super();
      savedVertexColors = null;
   }

   public FemMeshBase (FemModel3d fem) {
      this();
      myFem = fem;
   }

   public abstract int numAttachments ();

   public abstract PointAttachment getAttachment (int idx);

   /** 
    * Initialize data structures prior to adding vertices and faces to
    * this surface.
    */
   void initializeSurfaceBuild() {
      PolygonalMesh mesh = new PolygonalMesh();
      doSetMesh (mesh, null, null);
      mesh.setFixed (false);
      mesh.setUseDisplayList(true);
   }

   /** 
    * Finalize data structures after vertices and faces have been added.
    */
   void finalizeSurfaceBuild() {
   }

   public void setMeshFromInfo () {
      // Overloaded from super class. Is called by super.setMesh() and by scan
      // (whenever a mesh is scanned) to set mesh properties and auxiliary
      // data structures specific to the class.
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setFixed (false);
         mesh.setUseDisplayList (true);
      }
      if (myRenderProps != null) {
         myRenderProps.clearMeshDisplayList();
      }
   }

   public void setMesh (MeshBase mesh) {
      super.setMesh (mesh);
   }    
   
   protected void saveVertexColors() {
      MeshBase mesh = getMesh();
      savedColors = mesh.getColors();
      savedColorIndices = mesh.getColorIndices();
   }
   
   protected void restoreVertexColors() {
      MeshBase mesh = getMesh();
      if (savedColors == null) {
         mesh.clearColors();
      }
      else {
         mesh.setColors (savedColors, savedColorIndices);
      }
   }

   public void setSurfaceRendering (SurfaceRender mode) {
      if (mySurfaceRendering != mode) {
         if (myStressPlotRanging == Ranging.Auto) {
            myStressPlotRange.set (0, 0);
         }
         SurfaceRender oldMode = mySurfaceRendering;
         mySurfaceRendering = mode;
         
         // save/restore original vertex colors
         MeshBase mesh = getMesh();
         if ( mesh != null && mesh.getColors() != null) {
            if ( (oldMode == SurfaceRender.Strain || oldMode == SurfaceRender.Stress)
               && (mode != SurfaceRender.Strain && mode != SurfaceRender.Stress) ) {
               restoreVertexColors();
            } else if ( (oldMode != SurfaceRender.Strain && oldMode != SurfaceRender.Stress)
               &&  (mode == SurfaceRender.Strain || mode == SurfaceRender.Stress)) {
               saveVertexColors();
            }
         }
         
         if (myFem != null) {
            switch (mode) {
               case Strain:
                  myFem.setComputeNodalStrain(true);
                  myFem.updateStressAndStiffness();
                  mesh.setVertexColoringEnabled();
                  updateVertexColors();
                  break;
               case Stress:
                  myFem.setComputeNodalStress(true);
                  myFem.updateStressAndStiffness();
                  mesh.setVertexColoringEnabled();
                  updateVertexColors();
                  break;
               default: {
               }
            }
         }
      }
      // propagate to make mode explicit
      mySurfaceRenderingMode =
         PropertyUtils.propagateValue (
            this, "surfaceRendering", mode, mySurfaceRenderingMode);
   }

   public SurfaceRender getSurfaceRendering() {
      return mySurfaceRendering;
   }
   
   public PropertyMode getSurfaceRenderingMode() {
      return mySurfaceRenderingMode;
   }
   
   public void setSurfaceRenderingMode(PropertyMode mode) {
      if (mode != mySurfaceRenderingMode) {
         mySurfaceRenderingMode = PropertyUtils.setModeAndUpdate (
            this, "surfaceRendering", mySurfaceRenderingMode, mode);
      }
   }

   public Ranging getStressPlotRanging (){
      return myStressPlotRanging;
   }

   public void setStressPlotRanging (Ranging ranging) {
      if (myStressPlotRanging != ranging) {
         if (ranging == Ranging.Auto) {
            resetStressPlotRange();
         }
         myStressPlotRanging = ranging;
      }
      myStressPlotRangingMode =
         PropertyUtils.propagateValue (
            this, "stressPlotRanging", ranging, myStressPlotRangingMode);
   }
   
   public PropertyMode getStressPlotRangingMode() {
      return myStressPlotRangingMode;
   }
   
   public void setStressPlotRangingMode(PropertyMode mode) {
      if (mode != myStressPlotRangingMode) {
         myStressPlotRangingMode = PropertyUtils.setModeAndUpdate (
            this, "stressPlotRanging", myStressPlotRangingMode, mode);
      }
   }
   
   public PropertyMode getStressPlotRangeMode() {
      return myStressPlotRangeMode;
   }
   
   public void setStressPlotRangeMode(PropertyMode mode) {
      if (mode != myStressPlotRangeMode) {
         myStressPlotRangeMode = PropertyUtils.setModeAndUpdate (
            this, "stressPlotRange", myStressPlotRangeMode, mode);
      }
   }

   public DoubleInterval getStressPlotRange (){
      return new DoubleInterval (myStressPlotRange);
   }

   public void resetStressPlotRange () {
      myStressPlotRange.set (0, 0);
   }

   public void setStressPlotRange (DoubleInterval range) {
      myStressPlotRange = new DoubleInterval (range);
      myStressPlotRangeMode =
         PropertyUtils.propagateValue (
            this, "stressPlotRange", range, myStressPlotRangeMode);
   }

   protected abstract void updateVertexColors();
   
   @Override
   public void prerender(RenderList list) {
      super.prerender(list);
      
      if (mySurfaceRendering == SurfaceRender.Strain || 
         mySurfaceRendering == SurfaceRender.Stress) {
         updateVertexColors();
      }
   }
   
   @Override
   public void render(
      GLRenderer renderer, RenderProps props, int flags) {

      // highlight if either fem or mesh is selected
      if (isSelected() || (myFem != null && myFem.isSelected() )) {
         flags |= GLRenderer.SELECTED;
      }
      
      if (mySurfaceRendering == SurfaceRender.Stress || 
         mySurfaceRendering == SurfaceRender.Strain) {
         
         if ( (flags & GLRenderer.REFRESH) != 0) {
            updateVertexColors();
         }
         
         // only enable vertex colors if not selecting
         // During selection, requires VERTEX_COLORING in order to
         //    skip using display list
         if (renderer.isSelecting() || !((flags & GLRenderer.SELECTED) != 0)) {
            flags |= (GLRenderer.VERTEX_COLORING |
                      GLRenderer.HSV_COLOR_INTERPOLATION);
         }
         
      } else if (mySurfaceRendering == SurfaceRender.None) {
         return;
      }
      
      if (renderer.isSelecting()) {
         renderer.beginSelectionQuery (0);
      }
      super.render (renderer, props, flags);
      if (renderer.isSelecting()) {
         renderer.endSelectionQuery ();
      }
   }
   
   public void setColorMap(ColorMapBase map) {
      myColorMap = map;
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.clearDisplayList(myRenderProps);
      }
      myColorMapMode =
         PropertyUtils.propagateValue (
            this, "colorMap", map, myColorMapMode);
   }
   
   public ColorMapBase getColorMap() {
      return myColorMap;
   }
   
   public PropertyMode getColorMapMode() {
      return myColorMapMode;
   }
   
   public void setColorMapMode(PropertyMode mode) {
      if (mode != myColorMapMode) {
         myColorMapMode = PropertyUtils.setModeAndUpdate (
            this, "colorMap", myColorMapMode, mode);
      }
   }

   // public void scaleDistance (double s) {
   //    super.scaleDistance (s);
   //    // shouldn't need to change anything since everything is weight-based
   //    updatePosState();
   // }  

   // public void transformGeometry (
   //    AffineTransform3dBase X, TransformableGeometry topObject, int flags) {

   //    if ((flags & TransformableGeometry.SIMULATING) != 0) {
   //       return;
   //    }
   // }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "fem", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "savedVertexColors")) {
         rtok.scanToken ('[');
         int nv = getMesh().numVertices();
         savedVertexColors = new Color[nv];
         for (int i=0; i<nv; i++) {
            int r = rtok.scanInteger();
            int g = rtok.scanInteger();
            int b = rtok.scanInteger();
            int a = rtok.scanInteger();
            savedVertexColors[i] = new Color(r,g,b,a);
         }
         rtok.scanToken(']');
         return true;
      }
      else if (ScanWriteUtils.scanAndStorePropertyValue (
                  rtok, this, "surfaceRendering", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "fem")) {
         myFem = postscanReference (tokens, FemModel3d.class, ancestor);
         return true;
      }
      else if (ScanWriteUtils.postscanPropertyValue (
                  tokens, this, "surfaceRendering")) {
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   // @Override
   // public void postscan (
   // Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
   //    super.postscan (tokens, ancestor);
   //    if (myScannedSurfaceRendering != null) {
   //       setSurfaceRendering (myScannedSurfaceRendering);
   //       myScannedSurfaceRendering = null;
   //    }
   // }
 
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      
      if (savedVertexColors != null) {
         pw.println("savedVertexColors=[");
         IndentingPrintWriter.addIndentation(pw, 1);
         for (int i=0; i<savedVertexColors.length; i++) {
            Color c = savedVertexColors[i];
            pw.println(c.getRed() + " " + c.getGreen() + " " + c.getBlue() + " " + c.getAlpha());
         }
         IndentingPrintWriter.addIndentation(pw, -1);
         pw.println("]");
      }
      if (myFem != null) {
         pw.print ("fem=");
         pw.println (ComponentUtils.getWritePathName (ancestor, myFem));
      }
      // surfaceRendering has to be written near the end because it has to be
      // scanned after the model and mesh structures.
      myProps.get("surfaceRendering").writeIfNonDefault (this, pw, fmt);      
   }

   @Override
   public void connectToHierarchy () {
      // XXX not sure what to do here. Probably don't want to add back
      // references to all master components, but then we need a way to remove
      // masters from the attachments when masters disappear
      super.connectToHierarchy ();
   }

   @Override
   public void disconnectFromHierarchy() {
      // XXX not sure what to do here ... see comment in connectToParent()
      super.disconnectFromHierarchy();
   }
   
   public void setFem(FemModel3d fem) {
      myFem = fem;
   }
   
   public FemModel3d getFem() {
      return myFem;
   }
   
   @Override
   public FemMeshBase copy(int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemMeshBase fmb = (FemMeshBase)super.copy(flags, copyMap);
      
      if (mySurfaceRenderingMode == PropertyMode.Explicit) {
         fmb.setSurfaceRendering (mySurfaceRendering);
      }      
      if (myStressPlotRangingMode == PropertyMode.Explicit) {
         fmb.setStressPlotRanging(myStressPlotRanging);
      }
      if (myStressPlotRangeMode == PropertyMode.Explicit) {
         fmb.setStressPlotRange(myStressPlotRange);
      }

      if (myColorMapMode == PropertyMode.Explicit) {
         fmb.setColorMap(myColorMap);
      }
      
      if (savedVertexColors == null) {
         fmb.savedVertexColors = null;
      } else {
         fmb.savedVertexColors = new Color[savedVertexColors.length];
         for (int i=0; i<savedVertexColors.length; i++) {
            fmb.savedVertexColors[i] = savedVertexColors[i];   // shallow copy is fine for immutable
         }
      }

      FemModel3d newFem = (FemModel3d)copyMap.get(myFem);
      if (newFem != null) {
         fmb.setFem(newFem);
      } else {
         fmb.setFem(myFem);
      }
      
      return fmb;
   }
   
   @Override
   public int numSelectionQueriesNeeded() {
      // trigger queries so I can add FEM if needed
      return 1;
   }
   
   @Override
   public void getSelection(LinkedList<Object> list, int qid) {
      list.addLast(this);
   }
   
}
