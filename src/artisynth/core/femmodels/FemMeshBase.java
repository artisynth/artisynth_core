/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

import artisynth.core.femmodels.FemModel.Ranging;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.SkinMeshBase;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.ScanToken;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AffineTransform3dBase;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;
import maspack.render.color.ColorMapBase;
import maspack.render.color.HueColorMap;
import maspack.util.DoubleInterval;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

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
   
   // When stress or strain rendering is requested for this component, the
   // shading and coloring properties for the mesh are modified and their
   // previous values are stored in these variables so that they can be
   // restored when stress/strain rendering is turned off
   protected ArrayList<float[]> mySavedColors;
   protected int[] mySavedColorIndices;
   protected boolean mySavedVertexColoring;
   protected boolean mySavedFeatureColoring;
   protected ColorMixing mySavedColorMixing;
   protected Shading mySavedShading;
   protected PropertyMode mySavedShadingMode;
   
   protected ColorMapBase myColorMap = defaultColorMap.copy();
   protected PropertyMode myColorMapMode = PropertyMode.Inherited;
   
   protected static double EPS = 1e-10;
   
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
      myProps.addInheritable (
         "colorMap:Inherited", "color map for stress/strain", 
         defaultColorMap, "CE");
   }
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemMeshBase () {
      super();
      setColorInterpolation (ColorInterpolation.HSV);
   }

   public abstract int numVertexAttachments ();

   public abstract PointAttachment getVertexAttachment (int idx);

   /** 
    * Initialize data structures prior to adding vertices and faces to
    * this surface.
    */
   protected void initializeSurfaceBuild() {
      PolygonalMesh mesh = new PolygonalMesh();
      doSetMesh (mesh, null, null);
      mesh.setFixed (false);
      mesh.setColorsFixed (false);
   }

   /** 
    * Finalize data structures after vertices and faces have been added.
    */
   protected void finalizeSurfaceBuild() {
   }

   public void setMeshFromInfo () {
      // Overridden from super class. Is called by super.setMesh() and by scan
      // (whenever a mesh is scanned) to set mesh properties and auxiliary
      // data structures specific to the class.
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setFixed (false);
         mesh.setColorsFixed (false);
         mesh.setColorInterpolation (getColorInterpolation());
      }
   }
   
   protected void doSetMesh (
      MeshBase mesh, String fileName, AffineTransform3dBase X) {
      MeshBase oldMesh = getMesh();
      if (oldMesh != null && mySurfaceRendering.usesStressOrStrain()) {
         restoreMeshColoring (oldMesh);
      }      
      super.doSetMesh (mesh, fileName, X);
      if (mySurfaceRendering.usesStressOrStrain()) {
         saveMeshColoring (mesh);
         mesh.setVertexColoringEnabled();
         updateVertexColors(); // not sure we need this here
      }
   }
   
   protected void restoreMeshColoring (MeshBase mesh) {
      if (mySavedColors == null) {
         mesh.clearColors();
      }
      else {
         mesh.setColors (mySavedColors, mySavedColorIndices);
         if (mySavedVertexColoring) {
            mesh.setVertexColoringEnabled();
         }
         else if (mySavedFeatureColoring) {
            mesh.setFeatureColoringEnabled();
         }
      }
      mesh.setVertexColorMixing (mySavedColorMixing);
   }
   
   protected void restoreShading() {
      if (mySavedShading != null) {
         myRenderProps.setShading (mySavedShading);
         myRenderProps.setShadingMode (mySavedShadingMode);
      }
   }

   protected void saveMeshColoring (MeshBase mesh) {
      mySavedColors = mesh.getColors();
      mySavedColorIndices = mesh.getColorIndices();
      mySavedVertexColoring = mesh.getVertexColoringEnabled();
      mySavedFeatureColoring = mesh.getFeatureColoringEnabled();
      mySavedColorMixing = mesh.getVertexColorMixing();
   }

   protected void saveShading() {
      mySavedShadingMode = myRenderProps.getShadingMode();
      mySavedShading = myRenderProps.getShading();
   }
   
   public void setSurfaceRendering (SurfaceRender mode) {
      if (mySurfaceRendering != mode) {
         if (myStressPlotRanging == Ranging.Auto) {
            myStressPlotRange.set (0, 0);
         }
         mySurfaceRendering = mode; // set now if not already set
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
      if (mySurfaceRendering.usesStressOrStrain()) {
         updateVertexColors();
      }
      super.prerender(list);
   }
   
   @Override
   public void render(
      Renderer renderer, RenderProps props, int flags) {
      // highlight if either fem or mesh is selected
      if (isSelected()) {
         flags |= Renderer.HIGHLIGHT;
      }

      if (mySurfaceRendering == SurfaceRender.None) {
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

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "fem", tokens)) {
         return true;
      }
      else if (ScanWriteUtils.scanAndStorePropertyValue (
                  rtok, this, "surfaceRendering", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);

      // surfaceRendering has to be written near the end because it has to be
      // scanned after the model and mesh structures.
      myProps.get("surfaceRendering").writeIfNonDefault (
         this, pw, fmt, ancestor);      
   }

   @Override
   public void connectToHierarchy (CompositeComponent hcomp) {
      // XXX not sure what to do here. Probably don't want to add back
      // references to all master components, but then we need a way to remove
      // masters from the attachments when masters disappear
      super.connectToHierarchy (hcomp);
   }

   @Override
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      // XXX not sure what to do here ... see comment in connectToParent()
      super.disconnectFromHierarchy(hcomp);
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

      if (mySavedColors != null) {
         fmb.mySavedColors = new ArrayList<float[]>(mySavedColors.size());
         for (float[] c : mySavedColors) {
            fmb.mySavedColors.add (Arrays.copyOf (c, c.length));
         }
      }
      else {
         fmb.mySavedColors = null;
      }
      if (mySavedColorIndices != null) {
         fmb.mySavedColorIndices =
            Arrays.copyOf (mySavedColorIndices, mySavedColorIndices.length);
      }
      else {
         fmb.mySavedColorIndices = null;
      }
      fmb.mySavedVertexColoring = mySavedVertexColoring;
      fmb.mySavedFeatureColoring = mySavedFeatureColoring;
      fmb.mySavedColorMixing = mySavedColorMixing;
      fmb.mySavedShading = mySavedShading;
      fmb.mySavedShadingMode = mySavedShadingMode;
      
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
