package artisynth.core.modelbase;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.util.ScanToken;
import maspack.geometry.InterpolatingGridBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.ScalarGrid;
import maspack.geometry.ScalarGridBase;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3i;
import maspack.matrix.Vector3d;
import maspack.matrix.RigidTransform3d;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.Renderer;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
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

   /* --- end property accessors --- */

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
        
   public double getValue (FieldPoint fp) {
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
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      PolygonalMesh surf = myRenderSurface;
      if (surf != null) {
         surf.render (renderer, myRenderProps, flags);
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

}      
