package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.FemFieldPoint;
import artisynth.core.modelbase.MeshFieldPoint;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScalarFieldComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.ScalarRange;
import artisynth.core.util.ScanToken;
import maspack.geometry.InterpolatingGridBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.ScalarGrid;
import maspack.geometry.ScalarGridBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3i;
import maspack.properties.PropertyList;
import maspack.properties.PropertyUtils;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.FaceStyle;
import maspack.render.color.ColorMapBase;
import maspack.render.color.HueColorMap;
import maspack.util.DoubleInterval;
import maspack.util.EnumRange;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.Range;
import maspack.util.ReaderTokenizer;

/**
 * A scalar field defined over a regular 3D grid, using values set at the
 * grid's vertices, with values at other points obtained using trilinear
 * interpolation. The field is implemented using an embedded {@link ScalarGrid}
 * component.
 */
public class ScalarGridField 
   extends GridFieldBase implements ScalarFieldComponent {

   ScalarGrid myGrid = null;
   PolygonalMesh mySurface = null;
   PolygonalMesh myRenderSurface = null;
   boolean mySurfaceValidP = false;

   RenderObject myRenderObj;
   DoubleInterval myValueRange;

   public enum Visualization {
      POINT,
      SURFACE,
      OFF
   };

   protected ArrayList<FixedMeshBody> myRenderMeshComps = new ArrayList<>();

   static final public ColorInterpolation 
      DEFAULT_COLOR_INTERPOLATION = ColorInterpolation.HSV;
   protected ColorInterpolation myColorInterp = DEFAULT_COLOR_INTERPOLATION;

   protected static ColorMapBase defaultColorMap =  new HueColorMap(2.0/3, 0);
   protected ColorMapBase myColorMap = defaultColorMap.copy();

   static ScalarRange defaultRenderRange = new ScalarRange();
   ScalarRange myRenderRange = defaultRenderRange.clone();

   public static Visualization DEFAULT_VISUALIZATION = Visualization.OFF;
   protected Visualization myVisualization = DEFAULT_VISUALIZATION;

   /**
    * Special value indicating that a query point is outside the grid.
    */
   public static double OUTSIDE_GRID = ScalarGridBase.OUTSIDE_GRID;

   protected static double DEFAULT_SURFACE_DISTANCE = 0;
   private double mySurfaceDistance = DEFAULT_SURFACE_DISTANCE;

   protected static boolean DEFAULT_RENDER_SURFACE = false;
   private boolean myRenderSurfaceP = DEFAULT_RENDER_SURFACE;

   public static PropertyList myProps =
      new PropertyList (ScalarGridField.class, GridFieldBase.class);

   static {
      myProps.add (
         "visualization", "how to visualize this field",
         DEFAULT_VISUALIZATION);
      myProps.add (
         "renderRange", "range for drawing color maps", 
         defaultRenderRange);
      myProps.add (
         "colorInterpolation", "interpolation for vertex coloring", 
         DEFAULT_COLOR_INTERPOLATION);
      myProps.add (
         "colorMap", "color map for visualization", 
         defaultColorMap, "CE");
      myProps.add (
         "surfaceDistance", "distance value associated with the iso-surface",
         DEFAULT_SURFACE_DISTANCE);
      myProps.add (
         "renderSurface", 
         "render the iso-surface of the grid in the viewer",
         DEFAULT_RENDER_SURFACE);
   }
  
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public ScalarGridField() {
      super();
   }

   /**
    * Constructs a field using a given grid.
    *
    * @param grid scalar grid used to implement the field.
    */
   public ScalarGridField (ScalarGrid grid) {
      super();
      setGrid (grid);
   }

   /**
    * Constructs a named field using a given grid.
    *
    * @param grid name name of the field
    * @param grid scalar grid used to implement the field
    */
   public ScalarGridField (String name, ScalarGrid grid) {
      super (name);
      setGrid (grid);
   }

   /**
    * Returns the number of vertices in the grid.
    *
    * @return number of grid vertices
    */
   public int numVertices() {
      return myGrid.numVertices();
   }
   
   /* --- Begin property accessors --- */

   public Visualization getVisualization() {
      return myVisualization;
   }

   public void setVisualization (Visualization vis) {
      myVisualization = vis;
   }

   public Range getVisualizationRange() {
      return new EnumRange<Visualization>(
         Visualization.class, Visualization.values());
   }

   public ColorInterpolation getColorInterpolation() {
      return myColorInterp;
   }
   
   public void setColorInterpolation (ColorInterpolation interp) {
      if (interp != myColorInterp) {
         myColorInterp = interp;
      }
   }

   public ColorMapBase getColorMap() {
      return myColorMap;
   }
   
   public void setColorMap(ColorMapBase map) {
      myColorMap = map;
   }

    public void setRenderRange (ScalarRange range) {
      if (range != null) {
         ScalarRange newRange = range.clone();
         PropertyUtils.updateCompositeProperty (
            this, "renderRange", myRenderRange, newRange);
         myRenderRange = newRange;        
      }
      else if (myRenderRange != null) {
         PropertyUtils.updateCompositeProperty (
            this, "renderRange", myRenderRange, null);
         myRenderRange = null;
      }
   }
   
   public ScalarRange getRenderRange() {
      return myRenderRange;
   }

   /**
    * Returns the distance value used for creating this grid's iso-surface.
    *
    * @return iso-surface distance value
    */
   public double getSurfaceDistance() {
      return mySurfaceDistance;
   }

   /**
    * Sets the distance value used for creating this grid's iso-surface.
    *
    * @param dist iso-surface distance value
    */
   public void setSurfaceDistance (double dist) {
      if (mySurfaceDistance != dist) {
         mySurfaceDistance = dist;
         mySurfaceValidP = false;
      }
   }

   /**
    * Queries whether or not iso-surface rendering is enabled for this grid.
    *
    * @return {@code true} if iso-surface rendering is enabled 
    */
   public boolean getRenderSurface() {
      return myRenderSurfaceP;
   }

   /**
    * Sets whether or not iso-surface rendering is enabled for this grid.  If
    * enabled, the iso-surface associated with the grid is rendered in the
    * viewer.
    *
    * @param enable if {@code true}, enables iso-surface rendering
    */
   public void setRenderSurface (boolean enable) {
      myRenderSurfaceP = enable;
   }

   /* --- support for rendering meshes --- */

   protected boolean isTriangular (FixedMeshBody mcomp) {
      return ((mcomp.getMesh() instanceof PolygonalMesh) &&
              ((PolygonalMesh)mcomp.getMesh()).isTriangular());
   }

   public void addRenderMeshComp (FixedMeshBody mcomp) {
      if (!isTriangular(mcomp)) {
         throw new IllegalArgumentException ("Render mesh is not triangular");
      }
      myRenderMeshComps.add (mcomp);
   }

   public boolean removeRenderMeshComp (FixedMeshBody mcomp) {
      return myRenderMeshComps.remove (mcomp);
   }
   
   public FixedMeshBody getRenderMeshComp (int idx) {
      return myRenderMeshComps.get(idx);
   }
   
   public int numRenderMeshComps() {
      return myRenderMeshComps.size();
   }
   
   public void clearRenderMeshComps () {
      myRenderMeshComps.clear();
   }
   
   /* --- end support for rendering meshes --- */

   public void setLocalToWorld (RigidTransform3d TGW) {
      super.setLocalToWorld (TGW);
      if (mySurface != null) {
         mySurface.setMeshToWorld (TGW);
      }
   }

   /**
    * Queries the field value associated with this grid at a specified
    * position.  The position is assumed to be in either local or world
    * coordinates depending on whether {@link
    * getUseLocalValuesForField} returns {@code true}.
    *
    * <p>If the query point is outside the grid, then the value for the nearest
    * grid point is returned if the property {@code cliptoGrid} is {@code
    * true}; otherwise the special value {@link #OUTSIDE_GRID} is returned.
    * 
    * @param pos query position
    * @return value at the query position
    */
   public double getValue (Point3d pos) {
      if (myUseLocalValuesForFieldP) {
         return myGrid.getLocalValue (pos, myClipToGrid);
      }
      else {
         return myGrid.getWorldValue (pos, myClipToGrid);
      }
   }
   
   /**
    * Gets the position of the vertex indexed by its indices along the x, y,
    * and z axes. The position is in either local or world coordinates
    * depending on whether #getUseLocalValuesForField} returns
    * {@code true}.
    *
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return position of the vertex
    */
   public Point3d getVertexPosition (int xi, int yj, int zk) {
      Point3d coords = new Point3d();
      if (myUseLocalValuesForFieldP) {
         return (Point3d)myGrid.getLocalVertexCoords (
            coords, new Vector3i(xi, yj, zk));
      }
      else {
         return (Point3d)myGrid.getWorldVertexCoords (
            coords, new Vector3i(xi, yj, zk));
      }
   }
        
   /**
    * Gets the position of the vertex indexed by {@code vi}.  See {@link
    * #setVertexValue(int,double)} for a description of how {@code vi} is
    * computed. The position is in either local or world coordinates depending
    * on whether {@link #getUseLocalValuesForField} returns {@code true}.
    *
    * @param vi vertex index
    * @return position of the vertex
    */
   public Point3d getVertexPosition (int vi) {
      Point3d coords = new Point3d();
      if (myUseLocalValuesForFieldP) {
         return (Point3d)myGrid.getLocalVertexCoords (coords, vi);
      }
      else {
         return (Point3d)myGrid.getWorldVertexCoords (coords, vi);
      }
   }
        
   public double getValue (FemFieldPoint fp) {
      Point3d pos;
      if (myUseFemRestPositions) {
         pos = fp.getRestPos();
      }
      else {
         pos = fp.getSpatialPos();
      }
      if (myUseLocalValuesForFieldP) {
         return myGrid.getLocalValue (pos, myClipToGrid);
      }
      else {
         return myGrid.getWorldValue (pos, myClipToGrid);
      }
   }

   public double getValue (MeshFieldPoint fp) {
      if (myUseLocalValuesForFieldP) {
         return myGrid.getLocalValue (fp.getPosition(), myClipToGrid);
      }
      else {
         return myGrid.getWorldValue (fp.getPosition(), myClipToGrid);
      }
   }

   public ScalarGrid getGrid() {
      return myGrid;
   }

   /**
    * Returns the value at the grid vertex indexed by {@code vi}.  See {@link
    * #setVertexValue(int,double)} for a description of how {@code vi} is
    * computed.
    *
    * @param vi index of the vertex
    * @return value at the vertex
    */
   public double getVertexValue (int vi) {
      return myGrid.getVertexValue (vi);
   }

   /**
    * Sets the value at the grid vertex indexed by its indices along the x,
    * y, and z axes.
    *
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @param value new vertex value
    */
   public void setVertexValue (int xi, int yj, int zk, double value) {
      myGrid.setVertexValue (xi, yj, zk, value);
   }

   /**
    * Sets the value at the grid vertex indexed by {@code vi}, which is
    * computed from the axial vertex indices {@code xi}, {@code yj}, {@code zk}
    * according to
    * <pre>
    * vi = xi + nx*yj + (nx*ny)*zk
    * </pre>
    * where <code>nx</code> and <code>ny</code> are the number
    * of vertices along x and y axes.
    *
    * @param vi index of the vertex
    * @param value new vertex value
    */
   public void setVertexValue (int vi, double value) {
      myGrid.setVertexValue (vi, value);
   }

   /**
    * Returns the value at the grid vertex indexed by its indices along the x,
    * y, and z axes.
    *
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return value for the vertex
    */
   public double getVertexValue (int xi, int yj, int zk) {
      return myGrid.getVertexValue (xi, yj, zk);
   }

   /**
    * Sets the grid for this ScalarGridField. The grid is set by reference
    * (i.e., it is not copied). The grid's {@code renderRanges} and {@code
    * localToWorld} transform are updated from the current {@code renderRanges}
    * and {@code gridtoWorld} transform values for this component.
    *
    * @param grid grid to set
    */
   public void setGrid (ScalarGrid grid) {
      if (grid == null) {
         throw new IllegalArgumentException ("Grid cannot be null");
      }
      super.setGrid (grid); // set myBaseGrid in the super class
      myGrid = grid;
      mySurfaceValidP = false;

      grid.setLocalToWorld (myLocalToWorld);
      grid.setRenderRanges (myRenderRanges);
   }

   public PolygonalMesh getSurface() {
      if (!mySurfaceValidP) {
         ScalarGrid grid = getGrid();
         if (grid != null) {
            PolygonalMesh surf;
            surf = grid.createDistanceSurface(mySurfaceDistance, 4);
            surf.setMeshToWorld (myLocalToWorld);
            // do we need to call prerender()?
            mySurface = surf;
         }
         mySurfaceValidP = true;
      }
      return mySurface;
   }

   /* --- Renderable --- */

   public RenderProps createRenderProps() {
      RenderProps props = super.createRenderProps();
      props.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      return props;
   }

   /**
    * Returns the range of all values in this field, including
    * the default value.
    */
   public DoubleInterval getValueRange() {
      if (myValueRange == null) {
         DoubleInterval range = new DoubleInterval (0, 0);
         for (int vi=0; vi<numVertices(); vi++) {
            double val = getVertexValue(vi);
            if (vi == 0) {
               range.set (val, val);
            }
            else {
               range.updateBounds (val);
            }
         }
         myValueRange = range;
      }
      return myValueRange;
   }

   DoubleInterval updateRenderRange() {
      if (myRenderRange.getUpdating() != ScalarRange.Updating.FIXED) {
         myRenderRange.updateInterval (getValueRange());
      }
      return myRenderRange.getInterval();
   }

   protected RenderObject buildPointRenderObject(DoubleInterval range) {
      RenderObject rob = new RenderObject();

      rob.createPointGroup();
      ScalarFieldUtils.addColors (rob, myColorMap);
      int vidx = 0;
      for (int vi=0; vi<numVertices(); vi++) {
         rob.addPosition (getVertexPosition (vi));
         int cidx = ScalarFieldUtils.getColorIndex (getVertexValue(vi), range);
         rob.addVertex (vidx, -1, cidx, -1);
         rob.addPoint (vidx);
         vidx++;
      }     
      return rob;
   }

   public void prerender (RenderList list) {
      super.prerender (list);
      if (myRenderSurfaceP) {
         PolygonalMesh surf = getSurface();
         if (surf != null) {
            surf.prerender (myRenderProps); 
         }
         myRenderSurface = surf;
      }
      else {
         myRenderSurface = null;
      }
      switch (myVisualization) {
         case SURFACE: {
            DoubleInterval range = updateRenderRange();
            ArrayList<FixedMeshBody> rmeshComps = null;
            if (myRenderMeshComps.size() > 0) {
               rmeshComps = myRenderMeshComps;
            }
            if (rmeshComps != null) {
               myRenderObj = ScalarFieldUtils.buildMeshRenderObject (
                  rmeshComps, myColorMap, range,
                  (mcomp,vtx) -> myGrid.getWorldValue (
                     vtx.getWorldPoint(), /*clip=*/true));
            }
            else {
               myRenderObj = null;
            }
            break;
         }
         case POINT: {
            DoubleInterval range = updateRenderRange();
            myRenderObj = buildPointRenderObject(range);
            break;
         }
         default:{
            myRenderObj = null;
            break;
         }
      }
   }

   public void render (Renderer renderer, int flags) {
      boolean selecting =
         (getVisualization() == Visualization.SURFACE && renderer.isSelecting());

      // render visualization first so POINT rendering will appear instead of
      // the grid vertices
      RenderObject robj = myRenderObj;
      RenderProps props = myRenderProps;
      
      if (robj != null) {
         switch (myVisualization) {
            case SURFACE: {
               ColorInterpolation savedColorInterp = null;
               if (getColorInterpolation() == ColorInterpolation.HSV) {
                  savedColorInterp =
                     renderer.setColorInterpolation (ColorInterpolation.HSV);
               }
               for (int mid=0; mid<robj.numTriangleGroups(); mid++) {
                  if (selecting) {
                     renderer.beginSelectionQuery (mid);
                  }
                  RenderableUtils.drawTriangles (
                     renderer, robj, mid, props, /*selected=*/false);
                  if (selecting) {
                     renderer.endSelectionQuery ();
                  }
               }
               if (savedColorInterp != null) {
                  renderer.setColorInterpolation (savedColorInterp);
               }
               break;
            }
            case POINT: {
               RenderableUtils.drawPoints (
                  renderer, robj, 0, props, isSelected());
               break;
            }
            default: {
               break;
            }
         }
      }

      if (selecting) {
         renderer.beginSelectionQuery (robj.numTriangleGroups());
      }
      if (getVisualization() == Visualization.POINT) {
         // mask rendering of vertices 
         InterpolatingGridBase gridBase = myRenderGrid;         
         //gridBase.setVertexRenderingEnabled (false);
         super.render (renderer, flags);
         //gridBase.setVertexRenderingEnabled (true);
      }
      else {
         super.render (renderer, flags);
      }
      PolygonalMesh surf = myRenderSurface;
      if (surf != null) {
         surf.render (renderer, myRenderProps, flags);
      }
      if (selecting) {
         renderer.endSelectionQuery();
      }
   }

   @Override
   public int numSelectionQueriesNeeded() {
      if (getVisualization() == Visualization.SURFACE) {
         return myRenderMeshComps.size()+1;
      }
      else {
         return -1;
      }
   }

   @Override
   public void getSelection(LinkedList<Object> list, int qid) {
      if (qid == myRenderMeshComps.size()) {
         list.addLast(this);
      }
      else if (qid >= 0 && qid < myRenderMeshComps.size()) {
         list.addLast(myRenderMeshComps.get(qid));
      }
   }

   /* --- I/O --- */

   protected void writeGrid (PrintWriter pw, NumberFormat fmt)
      throws IOException {
      if (myGrid != null) {
         pw.print ("grid=");
         IndentingPrintWriter.addIndentation (pw, 2);
         getGrid().write (pw, fmt, null);
         IndentingPrintWriter.addIndentation (pw, -2);
      }
   }

   protected InterpolatingGridBase scanGrid (
      ReaderTokenizer rtok) throws IOException {
      myGrid = new ScalarGrid();
      myGrid.scan (rtok, null);
      return myGrid;
   }

   /**
    * {@inheritDoc}
    */  
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (ScanWriteUtils.scanAndStoreReferences (
             rtok, "renderMeshes", tokens) != -1) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   /**
    * {@inheritDoc}
    */  
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      if (myRenderMeshComps.size() > 0) {
         pw.print ("renderMeshes=");
         ScanWriteUtils.writeBracketedReferences (
            pw, myRenderMeshComps, ancestor);
      }
   }

   /**
    * {@inheritDoc}
    */  
   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "renderMeshes")) {
         myRenderMeshComps.clear();
         ScanWriteUtils.postscanReferences (
            tokens, myRenderMeshComps, FixedMeshBody.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   /* --- Edit methods --- */

   /**
    * {@inheritDoc}
    */
   public void getSoftReferences (List<ModelComponent> refs) {
      for (FixedMeshBody mcomp : myRenderMeshComps) {
         refs.add (mcomp);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      ComponentUtils.updateReferences (this, myRenderMeshComps, undo, undoInfo);
   }

}
