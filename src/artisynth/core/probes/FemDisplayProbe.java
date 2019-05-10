/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;

import maspack.geometry.BVFeatureQuery;
import maspack.geometry.BVIntersector;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TriangleIntersector;
import maspack.geometry.Vertex3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.IsRenderable;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Dragger3d.DraggerType;
import maspack.render.RenderProps;
import maspack.render.color.ColorMapBase;
import maspack.render.color.HueColorMap;
import maspack.util.DoubleInterval;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.femmodels.FemElement3dBase;
import artisynth.core.femmodels.FemIntersector;
import artisynth.core.femmodels.FemModel.Ranging;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.util.ScanToken;

/**
 * A probe that allows you to examine the stress/strain inside a {@link FemModel3d}.
 * @author Antonio
 *
 */
public class FemDisplayProbe extends CutPlaneProbe {

   // used for "skinning" probe to mesh
   private static class VtxInfo {
      FemNode3d[] nodes;
      double[] coords;
   }

   protected static SurfaceRender defaultSurfaceRender = SurfaceRender.Stress;
   protected static Ranging defaultStressPlotRanging = Ranging.Auto;
   protected static DoubleInterval defaultStressPlotRange = new DoubleInterval(
      0, 1);
   protected static ColorMapBase defaultColorMap  = new HueColorMap(2.0/3, 0);
   
   protected static double defaultBackgroundAlpha = 0.1;
   protected static boolean defaultDrawIntersections = false;
   protected static double defaultTolerance = 1e-10; // used for intersections
   
   // do we follow the mesh or use rest coordinates?
   protected static boolean defaultUseRestCoorindates = false; 
   
   
   protected FemModel3d myFem;
   protected SurfaceRender mySurfaceRendering = defaultSurfaceRender;
   protected Ranging myStressPlotRanging = defaultStressPlotRanging;
   protected DoubleInterval myStressPlotRange = defaultStressPlotRange;
   protected double myBackgroundAlpha = defaultBackgroundAlpha;
   
   protected boolean clipped = false;
   protected boolean useRestCoordinates = defaultUseRestCoorindates;
   
   protected boolean drawIntersections = defaultDrawIntersections;
   protected static double myIntersectionTolerance = defaultTolerance;
   protected ColorMapBase myColorMap = defaultColorMap.copy();

   protected HashMap<Vertex3d,Boolean> vtxIndicatorMap =
      new HashMap<Vertex3d,Boolean>(myPlaneSurface.numVertices());
   protected ArrayList<LinkedList<Point3d>> myIntersections = null;
   HashMap<Vertex3d,VtxInfo> clippedVtxMap = null;
   DraggerType lastDraggerType = null;

   public static PropertyList myProps =
      new PropertyList(FemDisplayProbe.class, CutPlaneProbe.class);

   static {
      myProps.add(
         "backgroundAlpha", "alpha value of background color",
         defaultBackgroundAlpha, "[0,1]");
      myProps.add(
         "surfaceRendering", "either stress or strain", defaultSurfaceRender);
      myProps.add("stressPlotRanging", "ranging mode for stress plots",
         defaultStressPlotRanging);
      myProps.add(
         "stressPlotRange", "stress value range for color stress plots",
         defaultStressPlotRange);
      myProps.add("colorMap", "color map for stress/strain", defaultColorMap, "CE");
      myProps.add(
         "drawIntersections", "draw intersection with mesh",
         defaultDrawIntersections);
      myProps.add("clipped isClipped clip", "clip to intersection mesh", false);
      myProps.add("useRestCoordinates isUsingRestCoordinates setUseRestCoordinates", 
         "clipped mesh follows motion", defaultUseRestCoorindates);
      // John Lloyd, May 12 2014: Model components should not be properties!
      //myProps.add("fem", "fem component", null);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemDisplayProbe () {
      super();
   }

   public FemDisplayProbe (FemModel3d fem) {
      this();
      setFem(fem);
   }

   /**
    * Sets the fem model to examine
    */
   public void setFem(FemModel3d model) {
      myFem = model;
      
      if (myFem != null) {
         if (mySurfaceRendering.usesStrain()) {
            myFem.setComputeNodalStrain(true);
         } 
         else if (mySurfaceRendering.usesStress()) {
            myFem.setComputeNodalStress(true);
         }
      }
      
      // unclip
      if (clipped) {
         clip(false);
      }
      updateVertexIndicators();
      updateVertexColoring();
   }

   /**
    * Gets the examined FEM
    */
   public FemModel3d getFem() {
      return myFem;
   }

   /**
    * Updates colors and recomputes intersection with plane
    */
   @Override
   protected void updateMeshDisplay() {

      if (myFem != null) {
         if (!clipped) {
            myPlaneSurface.setMeshToWorld(XGridToWorld);
            
            if (!useRestCoordinates) {
               updateVertexIndicators();
            }               
            updateVertexColoring();
            if (drawIntersections) {
               updateIntersections();
            }
         } else {
            computeClippedVertexColors();
         }
      }
   }
   
   /**
    * computes intersection with mesh and plane
    */
   protected void updateIntersections() {
      if (myFem != null) {
         getPlane(myPlane);
         BVIntersector intersector = new BVIntersector();
         myIntersections = intersector.intersectMeshPlane(
            myFem.getSurfaceMesh(), myPlane, myIntersectionTolerance);
      } 
   }
   
   /**
    * Updates display colors and intersections
    */
   public void updateDisplay() {
      updateMeshDisplay();
   }

   /**
    *  determines which vertices are inside the supplied mesh
    */
   protected void updateVertexIndicators() {

      vtxIndicatorMap =
         new HashMap<Vertex3d,Boolean>(myPlaneSurface.numVertices());
      
      TriangleIntersector ti = new TriangleIntersector();
      BVFeatureQuery query = new BVFeatureQuery();
      ti.setEpsilon(myIntersectionTolerance);
      
      if (myFem != null) {
         PolygonalMesh mesh = myFem.getSurfaceMesh();
         for (Vertex3d vtx : myPlaneSurface.getVertices()) {
            Point3d pnt = vtx.getWorldPoint();
            if (query.isInsideOrientedMesh (
                   mesh, pnt, myIntersectionTolerance)) {
               vtxIndicatorMap.put(vtx, true);
            } else {
               vtxIndicatorMap.put(vtx, false);
            }
         }
      }

   }

   protected void updateVertexColoring() {
      if (!useRestCoordinates) {
         updateVertexColoringDynamic();
      } else {
         updateVertexColoringRest();
      }
   }
   
   private void setColor (Vertex3d vtx, Color c, float a){
      float color[] = c.getRGBComponents (null);
      color[3] = a;
      myPlaneSurface.setColor (vtx.getIndex(), color);
   }
   
   private void setMapColor (Vertex3d vtx, double val, float a) {
      float[] carray = new float[3];
      myColorMap.getRGB(val/myStressPlotRange.getRange(), carray);
      setColor (vtx, carray[0], carray[1], carray[2], a);
   }

   private void setColor (Vertex3d vtx, float r, float g, float b, float a){
      myPlaneSurface.setColor (vtx.getIndex(), r, g, b, a);
   }
   
   /**
    * Updates vertex colors
    */
   protected void updateVertexColoringRest() {

      RenderProps rprops = getRenderProps();
      float alpha = (float)rprops.getAlpha();
      Color faceColor = rprops.getFaceColor();
      float backAlpha = (float)myBackgroundAlpha*alpha;
      Color femFaceColor = null;
      double stressVal = 0;

      if (mySurfaceRendering.usesStressOrStrain()) {
         if (myStressPlotRanging == Ranging.Auto) {
            myStressPlotRange
               .merge(myFem.getNodalPlotRange(mySurfaceRendering));
         }
      } else {
         femFaceColor = myFem.getRenderProps().getFaceColor();
      }
      
      float[] carray = new float[3];
      
      // use our stored map of values
      for (Vertex3d vtx : myPlaneSurface.getVertices()) {

         if (!vtxIndicatorMap.containsKey(vtx) || !vtxIndicatorMap.get(vtx)) {
            setColor (vtx, faceColor, backAlpha);
         } else {
            
            VtxInfo vtxInfo;
            
            switch (mySurfaceRendering) {
               case None:
                  setColor (vtx, faceColor, alpha);
                  break;
               case Stress:
                  vtxInfo = clippedVtxMap.get(vtx);
                  if (vtxInfo != null) {
                     setMapColor (vtx, getVMStress (vtxInfo), alpha);
                  } else {
                     setColor (vtx, femFaceColor, backAlpha);
                  }
                  break;
               case MAPStress:
                  vtxInfo = clippedVtxMap.get(vtx);
                  if (vtxInfo != null) {
                     setMapColor (vtx, getMAPStress (vtxInfo), alpha);
                  } else {
                     setColor (vtx, femFaceColor, backAlpha);
                  }
                  break;
               case Strain:
                  vtxInfo = clippedVtxMap.get(vtx);
                  if (vtxInfo != null) {
                     setMapColor (vtx, getVMStrain (vtxInfo), alpha);
                  } else {
                     setColor (vtx, femFaceColor, backAlpha);
                  }
                  break;
               case MAPStrain:
                  vtxInfo = clippedVtxMap.get(vtx);
                  if (vtxInfo != null) {
                     setMapColor (vtx, getMAPStrain (vtxInfo), alpha);
                  } else {
                     setColor (vtx, femFaceColor, backAlpha);
                  }
                  break;
               default:
                  setColor (vtx, femFaceColor, alpha);
   
            }
         }   
      }

   }

   /**
    * Updates vertex colors
    */
   protected void updateVertexColoringDynamic() {

      RenderProps rprops = getRenderProps();
      float alpha = (float)rprops.getAlpha();
      float backAlpha = (float)myBackgroundAlpha*alpha;
      Color faceColor = rprops.getFaceColor();
      Color femFaceColor = null;
      double val = 0;
      float[] carray = new float[3];

      if (myFem != null) {
         if (mySurfaceRendering.usesStressOrStrain()) {   
            if (myStressPlotRanging == Ranging.Auto) {
               myStressPlotRange
                  .merge(myFem.getNodalPlotRange(mySurfaceRendering));
            }
         } else {
            femFaceColor = myFem.getRenderProps().getFaceColor();
         }
      } else {
         femFaceColor = Color.BLACK;   // arbitrary, shouldn't be used
      }
      
      
      // XXX: Sometimes fails with null pointer exception
      // But I can't reproduce consistently. Suspect has to
      // do with some kind of synchronization
      try {
         for (Vertex3d vtx : myPlaneSurface.getVertices()) {
            if (!vtxIndicatorMap.containsKey(vtx) || !vtxIndicatorMap.get(vtx)) {
               setColor (vtx, faceColor, backAlpha);
            } else {

               switch (mySurfaceRendering) {
                  case None:
                     setColor (vtx, faceColor, alpha);
                     break;
                  case Stress:
                     val = getVMStress(vtx.getWorldPoint(), myFem);
                     setMapColor (vtx, val, alpha);
                     break;
                  case MAPStress:
                     val = getMAPStress(vtx.getWorldPoint(), myFem);
                     setMapColor (vtx, val, alpha);
                     break;
                  case Strain:
                     val = getVMStrain(vtx.getWorldPoint(), myFem);
                     setMapColor (vtx, val, alpha);
                     break;
                  case MAPStrain:
                     val = getMAPStrain(vtx.getWorldPoint(), myFem);
                     setMapColor (vtx, val, alpha);
                     break;
                  default:
                     setColor (vtx, femFaceColor, alpha);

               }
            }
         }
      } catch (Exception e) {
         System.err.println("Error in display probe: " + e.getMessage());
      }

   }

    private static double computeMaxAbsEigenvalue (SymmetricMatrix3d M) {    
      Vector3d eigs = new Vector3d();
      M.getEigenValues (eigs);
      return eigs.get(eigs.maxAbsIndex());
   }  

   // computes stress of a point inside the model based on FEM shape
   // function interpolation
   private static void getStressValue (
      SymmetricMatrix3d stress, Point3d pnt, FemModel3d model) {

      Point3d loc = new Point3d();
      FemElement3dBase elem = model.findNearestElement(loc, pnt);
      Vector3d coords = new Vector3d();
      stress.setZero();
      if (elem != null) {
         elem.getNaturalCoordinates(coords, pnt);
         FemNode3d[] nodes = elem.getNodes();
         for (int i = 0; i < elem.numNodes(); i++) {
            stress.scaledAdd (elem.getN(i, coords), nodes[i].getStress());
         }
      }
   }

   // computes von Mises stress value of a point inside the FemModel based on
   // shape function interpolation
   private static double getVMStress(Point3d pnt, FemModel3d model) {

      Point3d loc = new Point3d();
      FemElement3dBase elem = model.findContainingElement(pnt);
      if (elem == null) {
         elem = model.findNearestElement(loc, pnt);
      }
      Vector3d coords = new Vector3d();
      double stress = 0;
      if (elem != null) {
         elem.getNaturalCoordinates(coords, pnt);
         FemNode3d[] nodes = elem.getNodes();
         for (int i = 0; i < elem.numNodes(); i++) {
            stress += elem.getN(i, coords) * nodes[i].getVonMisesStress();
         }
      }

      return stress;
   }

   // computes maximum principal stress value of a point inside the FemModel
   // based on shape function interpolation
   private static double getMAPStress (
      Point3d pnt, FemModel3d model) {
      SymmetricMatrix3d stress = new SymmetricMatrix3d();
      getStressValue (stress, pnt, model);
      return computeMaxAbsEigenvalue (stress);
   }

   // computes strain of a point inside the model based on FEM shape
   // function interpolation
   private static void getStrainValue (
      SymmetricMatrix3d strain, Point3d pnt, FemModel3d model) {

      Point3d loc = new Point3d();
      FemElement3dBase elem = model.findNearestElement(loc, pnt);
      Vector3d coords = new Vector3d();
      strain.setZero();
      if (elem != null) {
         elem.getNaturalCoordinates(coords, pnt);
         FemNode3d[] nodes = elem.getNodes();
         for (int i = 0; i < elem.numNodes(); i++) {
            strain.scaledAdd (elem.getN(i, coords), nodes[i].getStrain());
         }
      }
   }

   // computes von Mises strain of a point inside the model based on FEM shape
   // function interpolation
   private static double getVMStrain(Point3d pnt, FemModel3d model) {

      Point3d loc = new Point3d();
      FemElement3dBase elem = model.findNearestElement(loc, pnt);
      Vector3d coords = new Vector3d();
      double strain = 0;
      if (elem != null) {
         elem.getNaturalCoordinates(coords, pnt);
         FemNode3d[] nodes = elem.getNodes();
         for (int i = 0; i < elem.numNodes(); i++) {
            strain += elem.getN(i, coords) * nodes[i].getVonMisesStrain();
         }
      }
      return strain;
   }

   // computes maximum principal strain value of a point inside the FemModel
   // based on shape function interpolation
   private static double getMAPStrain (
      Point3d pnt, FemModel3d model) {
      SymmetricMatrix3d strain = new SymmetricMatrix3d();
      getStrainValue (strain, pnt, model);
      return computeMaxAbsEigenvalue (strain);
   }

   // if we are clipped to the FEM, then the shape function values are fixed   
   private void computeClippedVertexColors() {

      RenderProps rprops = getRenderProps();
      float alpha = (float)rprops.getAlpha();
      Color faceColor = rprops.getFaceColor();
      Color femFaceColor = null;

      if (mySurfaceRendering.usesStressOrStrain()) {
         if (myStressPlotRanging == Ranging.Auto) {
            myStressPlotRange
               .merge(myFem.getNodalPlotRange(mySurfaceRendering));
         }
      } else {
         femFaceColor = myFem.getRenderProps().getFaceColor();
      }

      // use our stored map of values
      for (Vertex3d vtx : myPlaneSurface.getVertices()) {

         switch (mySurfaceRendering) {
            case None:
               setColor (vtx, faceColor, alpha);
               break;
            case Stress:
               setMapColor (vtx, getVMStress(clippedVtxMap.get(vtx)), alpha);
               break;
            case MAPStress:
               setMapColor (vtx, getMAPStress(clippedVtxMap.get(vtx)), alpha);
               break;
            case Strain:
               setMapColor (vtx, getVMStrain(clippedVtxMap.get(vtx)), alpha);
               break;
            case MAPStrain:
               setMapColor (vtx, getMAPStrain(clippedVtxMap.get(vtx)), alpha);
               break;
            default:
               setColor (vtx, femFaceColor, alpha);

         }

      }
   }

   private static double getVMStrain(VtxInfo info) {
      double strain = 0;
      for (int i = 0; i < info.nodes.length; i++) {
         strain += info.nodes[i].getVonMisesStrain() * info.coords[i];
      }
      return strain;
   }

   private static void getStrainValue (SymmetricMatrix3d strain, VtxInfo info) {
      strain.setZero();
      for (int i = 0; i < info.nodes.length; i++) {
         strain.scaledAdd (info.coords[i], info.nodes[i].getStrain());
      }
   }

   private static double getMAPStrain (VtxInfo info) {
      SymmetricMatrix3d strain = new SymmetricMatrix3d();
      getStrainValue (strain, info);
      return computeMaxAbsEigenvalue (strain);
   }

   private static double getVMStress(VtxInfo info) {
      double stress = 0;
      for (int i = 0; i < info.nodes.length; i++) {
         stress += info.nodes[i].getVonMisesStress() * info.coords[i];
      }
      return stress;
   }

   private static void getStressValue (SymmetricMatrix3d stress, VtxInfo info) {
      stress.setZero();
      for (int i = 0; i < info.nodes.length; i++) {
         stress.scaledAdd (info.coords[i], info.nodes[i].getStress());
      }
   }

   private static double getMAPStress (VtxInfo info) {
      SymmetricMatrix3d stress = new SymmetricMatrix3d();
      getStressValue (stress, info);
      return computeMaxAbsEigenvalue (stress);
   }

   /**
    * Move vertices of clipped mesh along with FEM, like an embedded mesh
    */
   protected synchronized void updatePosState() {
      
      if (clipped && !useRestCoordinates) {
         
         for (Vertex3d vtx : myPlaneSurface.getVertices()) {
            VtxInfo vi = clippedVtxMap.get(vtx);
            vtx.pnt.setZero();
            for (int i=0; i<vi.nodes.length; i++) {
               vtx.pnt.scaledAdd(vi.coords[i], vi.nodes[i].getPosition());
            }
         }
         
      }
   }
   
   @Override 
   public void setDragger(DraggerType type) {
      if (clipped) {
         clip(false);
      }
      super.setDragger(type);
   }
   
   @Override
   public void setOrientation(AxisAngle axisAng) {
      if (clipped) {
         clip(false);  // unclip
      }
      super.setOrientation(axisAng);
   }
   
   @Override
   public void setPosition(Point3d pos) {
      if (clipped) {
         clip(false);  // unclip
      }
      super.setPosition(pos);
   }
   
   @Override
   public void setResolution(Vector2d res) {
      if (clipped) {
         clip(false);  // unclip
      }
      super.setResolution(res);
   }
   
   @Override
   public void setSize(Vector2d size) {
      if (clipped) {
         clip(false);  // unclip
      }
      super.setSize(size);
   }
   
   @Override
   public int getRenderHints() {
      int code = super.getRenderHints();
      if (myBackgroundAlpha < 1) {
         code |= IsRenderable.TRANSPARENT;
      }
      return code;
   }


   @Override
   public synchronized void render (Renderer renderer, int flags) {

      RenderProps rprops = getRenderProps();

      if (myPlaneSurface != null) {

//       if ((flags & Renderer.UPDATE_RENDER_CACHE) != 0) {
//          updateVertexColoring();
//       }
       
       if (mySurfaceRendering != SurfaceRender.None) {
          myPlaneSurface.render (renderer, rprops, 0);
          //renderer.drawMesh(rprops, myPlaneSurface,
          //   flags | GLRenderer.VERTEX_COLORING | GLRenderer.HSV_COLOR_INTERPOLATION);
       }

         if (drawIntersections) {
            for (LinkedList<Point3d> contour : myIntersections) {
               drawContour(contour, renderer, false);
            }
         }
      }

      if (isSelected()) {
         drawBoundary(renderer, true);
      }

   }
   
   @Override
   protected void rebuildMesh() {
      super.rebuildMesh();
      clipped = false;
   }
   
   /**
    * Clip the probe viewer to fall within the FEM.  This also causes the probe
    * to stick to the FEM, acting like an embedded surface
    */
   public void clip(boolean set) {
    
      if (clipped == set) {
         return;  // prevent excessive setting
      }
      
      if (clipped == false) {
         clipToMesh();
      } else {
         rebuildMesh();
         if (lastDraggerType != null) {
            setDragger(lastDraggerType);
         }
      }
      clipped = set;
      updateMeshDisplay();
   }
   
   public boolean isClipped() {
      return clipped;
   }

   // does the actual clipping
   protected void clipToMesh() {

      rebuildMesh();
      
      FemIntersector fi = new FemIntersector();
      myPlaneSurface = fi.intersectPlane(myFem, getPlane());
      createVtxMap();
      lastDraggerType = getDragger();
      myPlaneSurface.setFixed(false);
      myPlaneSurface.setColorsFixed(false);
      myPlaneSurface.setColorInterpolation (ColorInterpolation.HSV);
      myPlaneSurface.setVertexColoringEnabled();
      super.setDragger(DraggerType.None);

      mySurfaceBoundaries = extractBoundaries(myPlaneSurface);
      
      if (mySurfaceBoundaries.size()>1) {
         System.out.println("More than one boundary (" + mySurfaceBoundaries.size() + ")");
      }
   }
   
   // map of vertices to coordinates inside FEM
   private void createVtxMap() {
      clippedVtxMap = new HashMap<Vertex3d,VtxInfo>(myPlaneSurface.numVertices());
      
      Point3d loc = new Point3d();
      Vector3d ncoords = new Vector3d();
      for (Vertex3d vtx : myPlaneSurface.getVertices()) {
         
         FemElement3dBase elem = myFem.findNearestElement(loc, vtx.getWorldPoint());
         elem.getNaturalCoordinates(ncoords, vtx.getWorldPoint());
         VtxInfo info = new VtxInfo();
         info.nodes = elem.getNodes();
         info.coords = new double[info.nodes.length];
         for (int i=0; i<info.coords.length; i++) {
            info.coords[i] = elem.getN(i, ncoords);
         }
         clippedVtxMap.put(vtx, info);
         
      }
      
   }

   /**
    * Sets the surface rendering mode, allowing Stress, Strain,
    * Shaded or None
    * 
    * @param mode surface rendering mode
    */
   public void setSurfaceRendering(SurfaceRender mode) {
      if (mySurfaceRendering != mode) {
         if (myStressPlotRanging == Ranging.Auto) {
            myStressPlotRange.set(0, 0);
         }
         mySurfaceRendering = mode;

         if (myFem != null) {
            if (mode.usesStress()) {
               myFem.setComputeNodalStress(true);               
            }
            else if (mode.usesStrain()) {
               myFem.setComputeNodalStrain(true);               
            }
         }
      }

   }

   /**
    * Gets the surface rendering mode
    */
   public SurfaceRender getSurfaceRendering() {
      return mySurfaceRendering;
   }

   /**
    * Gets the stress/strain plot range type (auto or fixed)
    */
   public Ranging getStressPlotRanging() {
      return myStressPlotRanging;
   }

   /**
    * Sets the stress/strain plot range type (auto or fixed)
    */
   public void setStressPlotRanging(Ranging ranging) {
      if (myStressPlotRanging != ranging) {
         if (ranging == Ranging.Auto) {
            resetStressPlotRange();
         }
         myStressPlotRanging = ranging;
      }
   }

   /**
    * Gets the stress plot range
    */
   public DoubleInterval getStressPlotRange() {
      return new DoubleInterval(myStressPlotRange);
   }

   public void resetStressPlotRange() {
      myStressPlotRange.set(0, 0);
   }

   /**
    * Sets the stress/strain plot range
    */
   public void setStressPlotRange(DoubleInterval range) {
      myStressPlotRange = new DoubleInterval(range);
   }
   
   /**
    * Sets the color mapping for stress/strain plots
    */
   public void setColorMap(ColorMapBase map) {
      myColorMap = map;
   }
   
   /**
    * Gets the map used for interpolating colors in stress/strain plots
    */
   public ColorMapBase getColorMap() {
      return myColorMap;
   }

   /**
    * Transparency of the background for this probe
    */
   public double getBackgroundAlpha() {
      return myBackgroundAlpha;
   }

   /**
    * Sets the transparency for the background of this probe
    */
   public void setBackgroundAlpha(double val) {
      myBackgroundAlpha = val;
   }
   
   /**
    * Sets whether to draw an outline of the intersection of this
    * mesh with the plane
    */
   public void setDrawIntersections(boolean enable) {
      if (drawIntersections != enable) {
         drawIntersections = enable;
         if (drawIntersections) {
            updateIntersections();
         }
      }
   }
   
   /**
    * Gets whether we are computing/drawing intersection of mesh with plane
    */
   public boolean getDrawIntersections() {
      return drawIntersections;
   }

   @Override
   public void initialize(double t) {
      updatePosState();
   }

   @Override
   public synchronized void apply(double t) {
      updatePosState();
      super.apply(t);
   }
   
   public boolean isUsingRestCoordinates() {
      return useRestCoordinates;
   }
   
   public void setUseRestCoordinates(boolean set) {
      if (useRestCoordinates != set) {
         useRestCoordinates = set;
         if (set) {
            createVtxMap();
         }
      }
      
   }

   /**
    * {@inheritDoc}
    */
   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      if (myFem != null) {
         pw.print ("fem=");
         pw.println (ComponentUtils.getWritePathName (ancestor, myFem));
      }
   } 

   /**
    * {@inheritDoc}
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "fem", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "fem")) {
         setFem (postscanReference (
            tokens, FemModel3d.class, ancestor));         
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }


}
