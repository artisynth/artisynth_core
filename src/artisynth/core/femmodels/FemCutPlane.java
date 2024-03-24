package artisynth.core.femmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;

import maspack.geometry.*;
import maspack.collision.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.color.*;
import maspack.render.Renderer.*;
import maspack.util.*;

import artisynth.core.femmodels.FemModel.Ranging;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;

/**
 * Class to allow visualization of stress or strain values within a planar
 * slice of an FEM.
 */
public class FemCutPlane extends Frame implements FemMesh {

   protected static final double INF = Double.POSITIVE_INFINITY;

   static final public ColorInterpolation 
      DEFAULT_COLOR_INTERPOLATION = ColorInterpolation.RGB;
   protected ColorInterpolation myColorInterp = DEFAULT_COLOR_INTERPOLATION;

   protected static SurfaceRender DEFAULT_SURFACE_RENDERING =
      SurfaceRender.Shaded;
   protected SurfaceRender mySurfaceRendering = DEFAULT_SURFACE_RENDERING;

   protected static Ranging DEFAULT_STRESS_PLOT_RANGING = Ranging.Auto;
   protected Ranging myStressPlotRanging = DEFAULT_STRESS_PLOT_RANGING;
   protected PropertyMode myStressPlotRangingMode = PropertyMode.Inherited;

   protected static DoubleInterval DEFAULT_STRESS_PLOT_RANGE =
      new DoubleInterval(0,1);
   protected DoubleInterval myStressPlotRange =
      new DoubleInterval(DEFAULT_STRESS_PLOT_RANGE);
   protected PropertyMode myStressPlotRangeMode = PropertyMode.Inherited;

   protected static ColorMapBase DEFAULT_COLOR_MAP = new HueColorMap(2.0/3, 0);
   protected ColorMapBase myColorMap = DEFAULT_COLOR_MAP.copy();
   protected PropertyMode myColorMapMode = PropertyMode.Inherited;

   protected static double DEFAULT_SQUARE_SIZE = -1;
   protected double mySquareSize = DEFAULT_SQUARE_SIZE;

   protected static double DEFAULT_RESOLUTION = -1;
   protected double myResolution = DEFAULT_RESOLUTION;
   protected double myDefaultResolution = -1;

   protected static boolean DEFAULT_HIGHLIGHT_COLORED_MESH = false;
   protected boolean myHighlightColoredMesh =
      DEFAULT_HIGHLIGHT_COLORED_MESH;

   protected PolygonalMesh myMesh;
   protected boolean myMeshValid;
   protected MeshBase myRenderMesh;

   private static final Point3d[] mySquareCoords = new Point3d[] {
      new Point3d(-1, -1, 0),
      new Point3d( 1, -1, 0),
      new Point3d( 1,  1, 0),
      new Point3d(-1,  1, 0)
   };

   // most recently computed radius for the FEM bounds
   protected double myCurrentRadius;

   public static PropertyList myProps =
      new PropertyList(FemCutPlane.class, Frame.class);

   static {
      myProps.remove ("renderProps");
      myProps.add (
         "renderProps", "render properties", defaultRenderProps(null));
      myProps.remove ("velocity");
      myProps.remove ("targetPosition");
      myProps.remove ("targetOrientation");
      myProps.remove ("targetVelocity");
      myProps.remove ("targetActivity");
      myProps.remove ("force");
      myProps.remove ("transForce");
      myProps.remove ("moment");
      myProps.remove ("externalForce");
      myProps.remove ("frameDamping");
      myProps.remove ("rotaryDamping");
      myProps.add(
         "squareSize", 
         "size of the rendered square", DEFAULT_SQUARE_SIZE);
      myProps.add("resolution", "display grid resolution", DEFAULT_RESOLUTION);

      myProps.add(
         "surfaceRendering", 
         "either shaded, stress or strain", DEFAULT_SURFACE_RENDERING);
      myProps.add (
         "colorInterpolation", "interpolation for vertex coloring", 
         DEFAULT_COLOR_INTERPOLATION);
      myProps.addInheritable (
         "stressPlotRanging:Inherited", "ranging mode for stress plots",
         DEFAULT_STRESS_PLOT_RANGING);         
     myProps.addInheritable (
         "stressPlotRange:Inherited", 
         "stress value range for color stress plots", DEFAULT_STRESS_PLOT_RANGE);
      myProps.addInheritable (
         "colorMap:Inherited", "color map for stress/strain", 
         DEFAULT_COLOR_MAP, "CE");
      myProps.add (
         "highlightColoredMesh",
         "apply selection highlighting to colored mesh",
         DEFAULT_HIGHLIGHT_COLORED_MESH);
   }   
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemCutPlane () {
      setColorInterpolation (ColorInterpolation.HSV);
   }

   public FemCutPlane (RigidTransform3d TPW) {
      this();
      setPose (TPW);      
   }

   public FemCutPlane (double res) {
      this();
      setResolution(res);
   }

   public FemCutPlane (double res, RigidTransform3d TPW) {
      this();
      setResolution(res);
      setPose (TPW);      
   }

   public FemModel3d getFem() {
      ModelComponent gparent = getGrandParent();
      if (gparent instanceof FemModel3d) {
         return (FemModel3d)gparent;
      }
      else {
         return null;
      }
   }

   public boolean isMeshPolygonal() {
      return true;
   }

   public PolygonalMesh getMesh() {
      updateMesh();
      return myMesh;
   }

   // property accessors

   public ColorInterpolation getColorInterpolation() {
      return myColorInterp;
   }
   
   public void setColorInterpolation (ColorInterpolation interp) {
      if (interp != myColorInterp) {
         MeshBase mesh = getMesh();
         if (mesh != null) {
            mesh.setColorInterpolation (interp);
         }
         myColorInterp = interp;
      }
   }

   /**
    * Gets the size of the square.
    *
    * @return current square size
    * @see #setSquareSize
    */
   public double getSquareSize() {
      return mySquareSize;
   }

   /**
    * Sets the size of the square. The square is a square outline of the plane
    * rendered in the viewer to allow visualization and selection.  If {@code
    * size <= 0}, then the square is not displayed.  and selected in the
    * viewer.
    *
    * @param size new square size
    */
   public void setSquareSize(double size) {
      if (mySquareSize != size) {
         mySquareSize = size;
      }
   }

   /**
    * Gets the grid resolution of the display, in units of distance.
    *
    * @see #setResolution
    */
   public double getResolution() {
      return myResolution;
   }

   /**
    * Sets the grid resolution of the display, in units of distance.
    * Specifying a non-positive value will cause the resolution to be computed
    * automatically from a sampling of the FEM elements.
    */
   public void setResolution(double res) {
      if (myResolution != res) {
         myResolution = res;
      }
   }

   /**
    * Returns the currently computed grid resolution for the display.  If not
    * set explicitly set by {@link #setResolution}, this will be computed
    * automatically from a sampling of the FEM elements.
    */
   public double getCurrentResolution() {
      if (myResolution > 0) {
         return myResolution;
      }
      else {
         if (myDefaultResolution == -1 && getFem() != null) {
            myDefaultResolution = computeResolution(getFem());
         }
         return myDefaultResolution;
      }
   }

   private double computeResolution(FemModel3d fem) {
      int nsamps = 100;
      int[] idxs;
      if (fem.numElements() < nsamps) {
         idxs = new int[fem.numElements()];
         for (int i=0; i<idxs.length; i++) {
            idxs[i] = i;
         }
      }
      else {
         idxs = RandomGenerator.randomSequence (
            0, fem.numElements()-1, nsamps);
      }
      double avgr = 0;
      double minr = INF; // compute minr in case we want it later
      for (int i=0; i<idxs.length; i++) {
         FemElement3d elem = fem.getElement(idxs[i]);
         double r = RenderableUtils.getRadius(elem);
         if (r < minr) {
            minr = r;
         }
         avgr += r;
      }
      avgr /= nsamps;
      //System.out.printf ("minr=%g avgr=%g\n", minr, avgr);
      return avgr/2;
   }

   public void setSurfaceRendering (SurfaceRender mode) {
      SurfaceRender oldMode = getSurfaceRendering();
      FemModel3d fem = getFem();

      if (oldMode != mode) {
         if (myStressPlotRanging == Ranging.Auto) {
            myStressPlotRange.set (0, 0);
         }
         mySurfaceRendering = mode; // set now if not already set
         if (fem != null) { // paranoid: myFem should always be non-null here
            if (!isScanning()) {
               fem.updateInternalNodalStressSettings();
               fem.updateInternalNodalStrainSettings();
               if (mode.usesStressOrStrain()) {
                  fem.updateStressAndStiffness();
               }
            }
         }
      }
   }

   public SurfaceRender getSurfaceRendering() {
      return mySurfaceRendering;
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

   public boolean getHighlightColoredMesh() {
      return myHighlightColoredMesh;
   }

   public void setHighlightColoredMesh (boolean enable) {
      myHighlightColoredMesh = enable;
   }

   // end accessors
   
   public void resetStressPlotRange () {
      myStressPlotRange.set (0, 0);
      invalidateMesh();
   }

   public void setStressPlotRange (DoubleInterval range) {
      myStressPlotRange = new DoubleInterval (range);
      myStressPlotRangeMode =
         PropertyUtils.propagateValue (
            this, "stressPlotRange", range, myStressPlotRangeMode);
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
  
   @Override
   protected void updatePosState() {
      invalidateMesh();
   }  

   /* --- begin render properties --- */

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      Vector3d center = getPose().p;
      // Update the bounds so that we get a reasonable dragger size. That means
      // making the bounds a smaller than we otherwise might.
      double size = -1;
      if (getAxisLength() > 0) {
         size = getAxisLength()/2;
      }
      else {
         size = mySquareSize/3;
      }
      if (size > 0) {
         RenderableUtils.updateSphereBounds (pmin, pmax, center, size);
      }
      else {
         // Set bounds to the center. Radius will then be zero and bounds will
         // be set from the viewer.
         center.updateBounds (pmin, pmax);
      }
   }

   protected static RenderProps defaultRenderProps(HasProperties host) {
      RenderProps props = RenderProps.createMeshProps(host);
      props.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      return props;
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps(this);
   }

   public void prerender (RenderList list) {
      super.prerender(list);
      if (myRenderProps == null) {
         throw new InternalErrorException (
            "FemCutPlane has null RenderProps");
      }
      updateMesh();
      // if (mySurfaceRendering.usesStressOrStrain()) {
      //    updateVertexColors(myMesh);
      // }  
      
      MeshBase renderMesh = myMesh;
      if (renderMesh != null) {
         renderMesh.prerender(getRenderProps());
      }
      myRenderMesh = renderMesh;
      myRenderFrame.set (getPose());
   }

   public void invalidateMesh() {
      myMeshValid = false;
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);

      if (mySquareSize > 0) {
         // render square grid
         renderer.pushModelMatrix();
         renderer.mulModelMatrix(myRenderFrame);
         renderer.setColor (myRenderProps.getLineColor());
         float savedWidth = renderer.getLineWidth();
         renderer.setLineWidth (myRenderProps.getLineWidth());
         if (isSelected()) {
            renderer.setHighlighting (true);
         }
         Point3d pnt0 = new Point3d();
         Point3d pnt1 = new Point3d();
         pnt0.scale (mySquareSize, mySquareCoords[3]);
         for (int i=0; i<4; i++) {
            pnt1.scale (mySquareSize, mySquareCoords[i]);
            renderer.drawLine (pnt0, pnt1);
            pnt0.set (pnt1);
         }
         if (isSelected()) {
            renderer.setHighlighting (false);
         }
         renderer.setLineWidth(savedWidth);
         renderer.popModelMatrix();
      }
      if (isSelected() &&
          (getHighlightColoredMesh() ||
           !mySurfaceRendering.usesStressOrStrain())) {
         flags |= Renderer.HIGHLIGHT;
      }
      if (mySurfaceRendering == SurfaceRender.None) {
         return;
      }
      MeshBase renderMesh = myRenderMesh;
      if (renderMesh != null) {
         renderMesh.render (renderer, getRenderProps(), flags);
      }
   }

   protected void getNodesAndWeights (
      ArrayList<FemNode3d> nodes, VectorNd weights,
      FemModel3d fem, Point3d wpos) {

      double reduceTol = 1e-8;
      nodes.clear();

      FemElement3dBase elem = fem.findContainingElement (wpos);
      Point3d newLoc = new Point3d(wpos);
      if (elem == null) {
         // won't use newLoc since we're not projecting vertex onto FEM
         elem = fem.findNearestSurfaceElement (newLoc, wpos);
         if (newLoc.distance (wpos) > reduceTol*myCurrentRadius) {
            weights.setSize(0);
            return;
         }
      }
      VectorNd coords = new VectorNd (elem.numNodes());

      // first see if there's a node within reduceTol of the point, and if
      // so just use that.
      double maxd = Double.NEGATIVE_INFINITY;
      double mind = Double.POSITIVE_INFINITY;
      FemNode3d nearestNode = null;
      FemNode3d[] elemNodes = elem.getNodes();
      for (int k=0; k<elemNodes.length; k++) {
         double d = wpos.distance(elemNodes[k].getPosition());
         if (d > maxd) {
            maxd = d;
         }
         if (d < mind) {
            mind = d;
            nearestNode = elemNodes[k];
         }
      }
      if (mind/maxd <= reduceTol) {
         // weight everything to the nearest node
         nodes.add(nearestNode);
         weights.setSize(1);
         weights.set (0, 1.0);
      }
      else {
         Vector3d c3 = new Vector3d();
         boolean converged = 
            elem.getNaturalCoordinates (c3, wpos, 1000) >= 0;
         if (!converged) {
            System.err.println(
               "Warning: getNaturalCoordinates() did not converge, "+
               "element=" + ComponentUtils.getPathName(elem) +
               ", point=" + wpos);
            c3.setZero ();
         }
         for (int j=0; j<elem.numNodes(); j++) {
            coords.set (j, elem.getN (j, c3));
         }
         weights.setSize(0);
         for (int k=0; k<coords.size(); k++) {
            if (Math.abs(coords.get(k)) >= reduceTol) {
               nodes.add (elem.getNodes()[k]);
               weights.append(coords.get(k));                            
            }
         }
      }
   }

   protected void computeStressOrStrain (
      SymmetricMatrix3d S, ArrayList<FemNode3d> nodes, VectorNd weights,
      SurfaceRender rendering) {

      S.setZero();
      if (rendering.usesStress()) {
         for (int j=0; j<nodes.size(); j++) {
            S.scaledAdd (weights.get(j), nodes.get(j).getStress());
         }
      }
      else if (rendering.usesStrain()) {
         for (int j=0; j<nodes.size(); j++) {
            S.scaledAdd (weights.get(j), nodes.get(j).getStrain());
         }
      }
   }


   protected void updateMesh() {
      if (!myMeshValid || myMesh == null) {
         myMesh = createMesh();
         if (mySurfaceRendering.usesStressOrStrain()) {
            updateVertexColors (myMesh);

         }
         myMeshValid = true;
      }
   }

   protected PolygonalMesh createMesh() {

      FemModel3d fem = getFem();
      double res = getCurrentResolution();
      if (fem == null || fem.getSurfaceMesh() == null || res <= 0) {
         return null;
      }

      Point3d pmin = new Point3d (INF, INF, INF);
      Point3d pmax = new Point3d (-INF, -INF, -INF);
      Point3d cent = new Point3d();
      fem.updateBounds (pmin, pmax);
      cent.combine (0.5, pmin, 0.5, pmax);
      myCurrentRadius = cent.distance (pmin);

      int ncells = (int)Math.ceil(3*myCurrentRadius/res);
      double size = res*ncells;
      PolygonalMesh cutmesh = MeshFactory.createRectangle (
         size, size, ncells, ncells, /*addTextureCoords=*/false);

      RigidTransform3d TCW = new RigidTransform3d(getPose());
      // project the bounding volume center onto the plane
      Vector3d nrm = new Vector3d(); // plane normal
      Vector3d vpc = new Vector3d(); // vector from plane origin to center
      TCW.R.getColumn (2, nrm);
      vpc.sub (cent, TCW.p);
      TCW.p.scaledAdd (-nrm.dot(vpc), nrm, cent);
      
      cutmesh.setMeshToWorld (TCW);
      SurfaceMeshIntersector smi = new SurfaceMeshIntersector();
      PolygonalMesh mesh = smi.findMesh1Inside (
         fem.getSurfaceMesh(), cutmesh);

      if (mySurfaceRendering.usesStressOrStrain()) {
         mesh.setVertexColoringEnabled();
         mesh.setVertexColorMixing (ColorMixing.REPLACE);
      }
      mesh.setColorInterpolation (getColorInterpolation());
      return mesh;
   }

   protected void updateVertexColors (PolygonalMesh mesh) {

      FemModel3d fem = getFem();

      if (fem == null || !mySurfaceRendering.usesStressOrStrain()) {
         return;
      }

      if (myStressPlotRanging == Ranging.Auto) {
         myStressPlotRange.merge (fem.getNodalPlotRange(mySurfaceRendering));
      } 

      RenderProps rprops = getRenderProps();
      float alpha = (float)rprops.getAlpha();

      ArrayList<FemNode3d> nodes = new ArrayList<>();
      VectorNd weights = new VectorNd();

      float[] colorArray = new float[3];
      SymmetricMatrix3d S = new SymmetricMatrix3d();
      for (int i=0; i<mesh.numVertices(); i++) {
         Point3d wpos = mesh.getVertex(i).getWorldPoint();
         getNodesAndWeights (nodes, weights, fem, wpos);
         computeStressOrStrain (S, nodes, weights, mySurfaceRendering);
         double sval;
         switch (mySurfaceRendering) {
            case Stress: {
               sval = FemUtilities.computeVonMisesStress (S);
               break;
            }
            case MAPStress: {
               sval = S.computeMaxAbsEigenvalue();
               break;
            }
            case MaxShearStress: {
               sval = S.computeMaxShear();
               break;
            }
            case Strain: {
               sval = FemUtilities.computeVonMisesStrain (S);
               break;
            }
            case MAPStrain: {
               sval = S.computeMaxAbsEigenvalue();
               break;
            }
            case MaxShearStrain: {
               sval = S.computeMaxShear();
               break;
            }
            default: {
               sval = 0;
               break;
            }
         }
         double smin = myStressPlotRange.getLowerBound();
         double srng = myStressPlotRange.getRange();
         double c = (sval-smin)/srng;
         c = Math.max (0, Math.min (c, 1.0));
         myColorMap.getRGB(c, colorArray);
         mesh.setColor (
            i, colorArray[0], colorArray[1], colorArray[2], alpha);
      }
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      super.transformGeometry (gtr, context, flags);
      invalidateMesh();
   }   

   /* ---- begin I/O methods ---- */

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
      pw.println ("position=[ " + myState.getPosition().toString (fmt) + "]");
      pw.println ("rotation=[ " + myState.getRotation().toString (fmt) + "]");
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "position")) {
         Point3d pos = new Point3d();
         pos.scan (rtok);
         setPosition (pos);
         return true;
      }
      else if (scanAttributeName (rtok, "rotation")) {
         Quaternion q = new Quaternion();
         q.scan (rtok);
         setRotation (q);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   /* ---- end I/O methods ---- */
   
   public FemCutPlane copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemCutPlane copy = (FemCutPlane)super.copy (flags, copyMap);

      copy.setResolution (getResolution());
      copy.myDefaultResolution = myDefaultResolution;
      copy.myRenderMesh = null;

      if (myStressPlotRangingMode == PropertyMode.Explicit) {
         copy.setStressPlotRanging(myStressPlotRanging);
      }
      if (myStressPlotRangeMode == PropertyMode.Explicit) {
         copy.setStressPlotRange(myStressPlotRange);
      }

      if (myColorMapMode == PropertyMode.Explicit) {
         copy.setColorMap(myColorMap);
      }
      copy.myHighlightColoredMesh = myHighlightColoredMesh;

      return copy;
   }

}
