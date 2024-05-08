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
   protected static DoubleInterval DEFAULT_STRESS_PLOT_RANGE =
      new DoubleInterval(0,0);
   protected static ColorMapBase defaultColorMap =  new HueColorMap(2.0/3, 0);
   
   protected SurfaceRender mySurfaceRendering = defaultSurfaceRendering;
   protected Ranging myStressPlotRanging = defaultStressPlotRanging;
   protected DoubleInterval myStressPlotRange = new DoubleInterval(DEFAULT_STRESS_PLOT_RANGE);

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
         "stress value range for color stress plots",
         DEFAULT_STRESS_PLOT_RANGE, "NW");
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

   /**
    * Configure (or disconfigure) a mesh to be rendered using a color map.
    */
   protected void setMeshVertexColoring (MeshBase mesh, boolean enable) {
      if (enable) {
         saveShading();
         saveMeshColoring (mesh);
         mesh.setVertexColoringEnabled();
         mesh.setVertexColorMixing (ColorMixing.REPLACE);
         myRenderProps.setShading (Shading.NONE);
      }
      else {
         // disable stress/strain rendering *before* restoring colors
         restoreMeshColoring (mesh);
         restoreShading();
      }
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
         setMeshVertexColoring (mesh, true);
         mesh.setVertexColoringEnabled();
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
   
   // surface rendering

   protected abstract boolean maybeUpdateFixedRangeFromFEM();

   public abstract FemModel getFem();

   public void setSurfaceRendering (SurfaceRender rendering) {
      SurfaceRender prev = mySurfaceRendering;
      FemModel fem = getFem();
      // propagated value means this method is being called because
      // surfaceRendering was changed in an ancestor:
      boolean propagatedValue = (mySurfaceRenderingMode==PropertyMode.Inactive);
      if (propagatedValue && fem != null && "surface".equals (getName())) {
         // XXX Need to do this to keep surfaceRendering for the FEM and the
         // surface mesh the same. Otherwise, they could differ when
         // FEM.surfaceRenderMode is inherited, since FemModel and FemMeshBase
         // have different default values for surfaceRender.
         rendering = fem.getSurfaceRendering();
      }
      if (rendering != prev) {
         mySurfaceRendering = rendering;
         if ((rendering.usesStressOrStrain() != prev.usesStressOrStrain())) {
            MeshBase mesh = getMesh();
            if (mesh != null) {
               setMeshVertexColoring (mesh, rendering.usesStressOrStrain());
            }
         }
         if (!isScanning()) {
            if (fem != null) { // paranoid: fem should always be non-null here
               if (fem.updateStressStrainRenderFlags() != 0) {
                  fem.updateStressAndStiffness();
               }
            }
         }
         if (myStressPlotRanging == Ranging.Auto) {
            resetAutoStressPlotRange();
         }
         else {
            maybeUpdateFixedRangeFromFEM();
         }       
      }
      // propagate to make mode explicit
      mySurfaceRenderingMode =
         PropertyUtils.propagateValue (
            this, "surfaceRendering", rendering, mySurfaceRenderingMode);
   }

   public SurfaceRender getSurfaceRendering() {
      return mySurfaceRendering;
   }
   
   public void setSurfaceRenderingMode(PropertyMode mode) {
      if (mode != mySurfaceRenderingMode) {
         mySurfaceRenderingMode = PropertyUtils.setModeAndUpdate (
            this, "surfaceRendering", mySurfaceRenderingMode, mode);
      }
   }

   public PropertyMode getSurfaceRenderingMode() {
      return mySurfaceRenderingMode;
   }

   // stress plot ranging

   public void setStressPlotRanging (Ranging ranging) {
      if (myStressPlotRanging != ranging) {
         myStressPlotRanging = ranging;
         if (ranging == Ranging.Auto) {
            resetAutoStressPlotRange();
         }
         else {
            maybeUpdateFixedRangeFromFEM();
         }
      }
      myStressPlotRangingMode =
         PropertyUtils.propagateValue (
            this, "stressPlotRanging", ranging, myStressPlotRangingMode);
   }
   
   public Ranging getStressPlotRanging (){
      return myStressPlotRanging;
   }

   public void setStressPlotRangingMode(PropertyMode mode) {
      Ranging prevRanging = myStressPlotRanging;
      if (mode != myStressPlotRangingMode) {
         myStressPlotRangingMode = PropertyUtils.setModeAndUpdate (
            this, "stressPlotRanging", myStressPlotRangingMode, mode);
      }
      if (myStressPlotRanging != prevRanging) {
         if (myStressPlotRanging == Ranging.Auto) {
            resetAutoStressPlotRange();
         }
         else {
            maybeUpdateFixedRangeFromFEM();
         }
      }
   }
   
   public PropertyMode getStressPlotRangingMode() {
      return myStressPlotRangingMode;
   }

   // stress plot range

   protected void doSetStressPlotRange (DoubleInterval range) {
      if (!myStressPlotRange.equals(range)) {
         myStressPlotRange.set (range);
         updateVertexColors();
      }
   }      

   public void resetAutoStressPlotRange () {
      if (mySurfaceRendering.usesStressOrStrain() && !isScanning()) {
         myStressPlotRange.set (0, 0);
         updatePlotRangeIfAuto();
      }
   }
   
   public void setStressPlotRange (DoubleInterval range) {
      doSetStressPlotRange (range);
      myStressPlotRangeMode =
         PropertyUtils.propagateValue (
            this, "stressPlotRange", range, myStressPlotRangeMode);
   }
 
   public DoubleInterval getStressPlotRange (){
      return new DoubleInterval (myStressPlotRange);
   }

   public void setStressPlotRangeMode(PropertyMode mode) {
       if (mode != myStressPlotRangeMode) {
          PropertyMode prevMode = myStressPlotRangeMode;
          myStressPlotRangeMode = mode;          
          if (mode == PropertyMode.Explicit) {
             // ignore propogation since there are no subcomponents
          }
          else if (prevMode == PropertyMode.Explicit) {
             maybeUpdateFixedRangeFromFEM();
             // ignore propogation since there are no subcomponents
          }
       }
   }

   public PropertyMode getStressPlotRangeMode() {
      return myStressPlotRangeMode;
   }

   // end stress plot range

   protected abstract void updatePlotRangeIfAuto();
   protected abstract void updateVertexColors();
   
   @Override
   public void prerender(RenderList list) {
      if (mySurfaceRendering.usesStressOrStrain()) {
         updatePlotRangeIfAuto();
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
      else if (scanAttributeName (rtok, "stressPlotRange")) {
         myStressPlotRange.scan (rtok, null);
         return true;
      }
      else if (scanAttributeName (rtok, "stressPlotRangeMode")) {
         myStressPlotRangeMode = rtok.scanEnum(PropertyMode.class);
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
      // need to write stressPlotRange explicitly because Auto ranging will
      // change its value regardless of stressPlotRangeMode.
      if (!myStressPlotRange.equals(DEFAULT_STRESS_PLOT_RANGE)) {
         pw.print ("stressPlotRange=");
         myStressPlotRange.write (pw, fmt, null);
      }
      if (myStressPlotRangeMode != PropertyMode.Inherited) {
         pw.println ("stressPlotRangeMode=" + myStressPlotRangeMode);
      }
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
      fmb.myStressPlotRange = new DoubleInterval(myStressPlotRange);
      fmb.myStressPlotRangeMode = myStressPlotRangeMode;

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
