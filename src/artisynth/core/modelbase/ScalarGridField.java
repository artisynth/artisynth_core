package artisynth.core.modelbase;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import artisynth.core.modelbase.*;
import artisynth.core.modelbase.FieldUtils.ScalarFieldFunction;
import artisynth.core.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.properties.*;

/**
 * Component that encapsulates a ScalarGrid.
 */
public class ScalarGridField 
   extends GridCompBase implements ScalarField, FieldComponent {

   ScalarGrid myGrid = null;
   PolygonalMesh mySurface = null;
   PolygonalMesh myRenderSurface = null;
   boolean mySurfaceValidP = false;

   protected static double DEFAULT_SURFACE_DISTANCE = 0;
   private double mySurfaceDistance = DEFAULT_SURFACE_DISTANCE;

   protected static boolean DEFAULT_RENDER_SURFACE = false;
   private boolean myRenderSurfaceP = DEFAULT_RENDER_SURFACE;

   public static PropertyList myProps =
      new PropertyList (ScalarGridField.class, GridCompBase.class);

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

   public class RestFieldFunction extends ScalarFieldFunction {

      public RestFieldFunction () {
      }

      public ScalarGridField getField() {
         return ScalarGridField.this;
      }

      public double eval (FieldPoint def) {
         return getValue (def.getRestPos());
      }
   }

   public class SpatialFieldFunction extends ScalarFieldFunction {

      public SpatialFieldFunction () {
      }

      public ScalarGridField getField() {
         return ScalarGridField.this;
      }

      public double eval (FieldPoint def) {
         return getValue (def.getRestPos());
      }

      public boolean useRestPos() {
         return false;
      }
   }

   public ScalarFieldFunction createFieldFunction (boolean useRestPos) {
      if (useRestPos) {
         return new RestFieldFunction();
      }
      else {
         return new SpatialFieldFunction();
      }
   }

   public ScalarGridField() {
      super();
   }

   public ScalarGridField (String name) {
      super(name);
   }

   public ScalarGridField (String name, ScalarGrid grid) {
      super (name);
      setGrid (grid);
   }

   public ScalarGridField ( ScalarGrid grid) {
      super();
      setGrid (grid);
   }

   /**
    * Queries the field value associated with this grid at a specifed position.
    * The position is assumed to be in either local or world coordinates
    * depeneding on whether {@link #getLocalValuesForField} returns {@code true}.
    * 
    * @param pos query position
    */
   public double getValue (Point3d pos) {
      if (myLocalValuesForFieldP) {
         return myGrid.getLocalValue (pos);
      }
      else {
         return myGrid.getWorldValue (pos);
      }
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

   public void setLocalToWorld (RigidTransform3d TGW) {
      super.setLocalToWorld (TGW);
      if (mySurface != null) {
         mySurface.setMeshToWorld (TGW);
      }
   }

   public ScalarGrid getGrid() {
      return myGrid;
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
