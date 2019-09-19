package artisynth.core.modelbase;

import java.awt.Color;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.LineStyle;
import maspack.properties.*;

/**
 * Base class for components that contain interpolation grids.
 */
public abstract class GridCompBase extends RenderableComponentBase
   implements ScalableUnits, TransformableGeometry {

   protected InterpolatingGridBase myGridBase;
   protected InterpolatingGridBase myRenderGrid;
   protected RigidTransform3d myLocalToWorld = new RigidTransform3d();

   protected static boolean DEFAULT_LOCAL_VALUES_FOR_FIELD = true;
   protected boolean myLocalValuesForFieldP = DEFAULT_LOCAL_VALUES_FOR_FIELD;

   protected static boolean DEFAULT_RENDER_GRID = true;
   protected boolean myRenderGridP = DEFAULT_RENDER_GRID;

   protected static String DEFAULT_RENDER_RANGES = "* * *";
   protected String myRenderRanges = DEFAULT_RENDER_RANGES;

   public static PropertyList myProps =
      new PropertyList (GridCompBase.class, RenderableComponentBase.class);

   static {
      myProps.add ("renderProps * *", "render properties", null);
      myProps.add (
         "localValuesForField", "use local values for field values", 
         DEFAULT_LOCAL_VALUES_FOR_FIELD);
      myProps.add (
         "renderGrid", 
         "render the grid in the viewer",
         DEFAULT_RENDER_GRID);
      myProps.add (
         "renderRanges",
         "controls which part of the grid to render", 
         DEFAULT_RENDER_RANGES);      
   }
  
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public GridCompBase() {
      setRenderProps (createRenderProps());
   }

   public GridCompBase (String name) {
      this();
      setName (name);
   }

   /**
    * Queries whether or local values should be used for field queries.
    *
    * @return {@code true} if local values should be used for field queries
    */
   public boolean getLocalValuesForField() {
      return myLocalValuesForFieldP;
   }

   /**
    * Sets whether or not local values should be used for field queries.
    *
    * @param enable if {@code true}, enables using local values for field queries
    */
   public void setLocalValuesForField (boolean enable) {
      myLocalValuesForFieldP = enable;
   }

   /**
    * Queries whether or not grid rendering is enabled.
    *
    * @return {@code true} if grid rendering is enabled 
    */
   public boolean getRenderGrid() {
      return myRenderGridP;
   }

   /**
    * Sets whether or not rendering is enabled for this grid.  If enabled, the
    * grid is rendered in the viewer.
    *
    * @param enable if {@code true}, enables grid rendering
    */
   public void setRenderGrid (boolean enable) {
      myRenderGridP = enable;
   }

   /**
    * Returns a string describing the x, y, z vertex ranges used when rendering
    * this grid. See {@link #setRenderRanges} for a more detailed description.
    * 
    * @return string describing the render ranges
    * @see #setRenderRanges
    */  
   public String getRenderRanges() {
      return myRenderRanges;
   }

   /**
    * Specifies the x, y, z vertex ranges used when rendering this grid.
    * The signed distance grid will have
    * <code>numVX</code> X <code>numVY</code> X <code>numVZ</code> vertices in
    * the x, y, z directions, where <code>numVX</code>, <code>numVY</code>, and
    * <code>numVZ</code> are each one greater than the x, y, z cell resolution
    * values returned by the {@link VectorGrid#getResolution}
    * method of the distance grid itself. In general, the range string should
    * contain three range specifications, one for each axis, where each
    * specification is either <code>*</code> (all vertices), <code>n:m</code>
    * (vertices in the index range <code>n</code> to <code>m</code>, inclusive),
    * or <code>n</code> (vertices only at index <code>n</code>). A
    * range specification of <code>"* * *"</code> (or <code>"*"</code>)
    * means draw all vertices, which is the default behavior. Other
    * examples include:
    * <pre>
    *  "* 7 *"      - all vertices along x and z, and those at index 7 along y
    *  "0 2 3"      - a single vertex at indices (0, 2, 3)
    *  "0:3 4:5 *"  - all vertices between indices 0 and 3 along x, and 4 and 5
    *                 along y
    * </pre>
    * 
    * @param ranges describing the render ranges
    * @see #getRenderRanges
    * @throws IllegalArgumentException if the range syntax is invalid
    * or out of range
    */
   public void setRenderRanges (String ranges) {
      if (ranges.matches ("\\s*\\*\\s*") ||
          ranges.matches ("\\s*\\*\\s+\\*\\s+\\*\\s*")) {
         myRenderRanges = "* * *";
      }
      else {
         StringHolder errMsg = new StringHolder();
         if (VectorGrid.parseRenderRanges (ranges, errMsg) == null) {
            throw new IllegalArgumentException (
               "Illegal range spec: " + errMsg.value);
         }
         myRenderRanges = ranges;
      }
      if (myGridBase != null) {
         myGridBase.setRenderRanges (ranges);
         myRenderRanges = myGridBase.getRenderRanges();
      }
   }
   
   public Range getRenderRangesRange() {
      return new RenderRangesRange();
   }  
   
   protected class RenderRangesRange extends RangeBase {

      @Override
      public boolean isValid (Object obj, StringHolder errMsg) {
         if (!(obj instanceof String)) {
            errMsg.value = "Object is not a string";
            return false;
         }
         else {
            String ranges = (String)obj;
            VectorGrid.parseRenderRanges (ranges, errMsg);
            return errMsg.value == null;
         }
      }
   }
   
   public void setLocalToWorld (RigidTransform3d TGW) {
      myLocalToWorld.set (TGW);
      if (myGridBase != null) {
         myGridBase.setLocalToWorld (TGW);
      }
   }

   public void getLocalToWorld (RigidTransform3d TGW) {
      TGW.set (myLocalToWorld);
   }

   public RigidTransform3d getLocalToWorld() {
      return myLocalToWorld;
   }

   public void scaleDistance (double s) {
      if (myGridBase != null) {
         myGridBase.scaleDistance (s);
      }
      myLocalToWorld.p.scale (s);
   }

   public void scaleMass (double s) {
      // nothing to do
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies to add
   }

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // transform the pose
      RigidTransform3d Xpose = new RigidTransform3d();
      Xpose.set (myLocalToWorld);
      gtr.transform (Xpose);
      myLocalToWorld.set (Xpose);     
      
      if (myGridBase != null) {
         myGridBase.setLocalToWorld (myLocalToWorld);
         // extract scaling factor from transform
         if (gtr.isRestoring()) {
            double scale = gtr.restoreObject (new Double(1));
            myGridBase.scaleDistance (1/scale);
         }
         else {
            AffineTransform3d XL = gtr.computeLocalAffineTransform (
               Xpose, new GeometryTransformer.UniformScalingConstrainer());
            double scale = Math.abs(XL.A.m00);
            if (gtr.isSaving()) {
               gtr.saveObject (scale);
            }
            myGridBase.scaleDistance (scale);
         }
      }
   }

   public abstract InterpolatingGridBase getGrid();

   public void setGrid (InterpolatingGridBase gridBase) {
      myGridBase = gridBase;
   }

   /* --- Renderable --- */

   public RenderProps createRenderProps() {
      RenderProps props = RenderProps.createRenderProps (this);
      props.setVisible (false);
      props.setDrawEdges (true);
      props.setPointRadius (0.0);
      props.setPointSize (0);
      props.setLineStyle (LineStyle.LINE);
      props.setLineColor (Color.BLUE);
      return props;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      if (myGridBase != null) {
         myGridBase.updateBounds (pmin, pmax);
      }
   }

   public void prerender (RenderList list) {
      if (myRenderGridP) {
         // call getGrid() in case grid is being lazily allocated
         InterpolatingGridBase gridBase = getGrid();
         if (gridBase !=null) {
            gridBase.prerender (myRenderProps);
         }
         myRenderGrid = gridBase;
      }
      else {
         myRenderGrid = null;
      }
   }

   public void render (Renderer renderer, int flags) {
      if (isSelected()) {
         flags |= Renderer.HIGHLIGHT;
      }
      InterpolatingGridBase gridBase = myRenderGrid;
      if (gridBase != null) {
         gridBase.render (renderer, myRenderProps, flags);
      }
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      writeGrid (pw, fmt);
   }  

   protected abstract void writeGrid (PrintWriter pw, NumberFormat fmt)
      throws IOException;

   protected abstract InterpolatingGridBase scanGrid (
      ReaderTokenizer rtok) throws IOException;

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "grid")) {
         myGridBase = scanGrid (rtok);
         setRenderRanges (myRenderRanges);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void clearCacheIfNecessary() {
   }
}      
