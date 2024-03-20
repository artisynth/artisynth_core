package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.FemFieldPoint;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.ScalarFieldComponent;
import artisynth.core.util.ScalarRange;
import artisynth.core.util.ScanToken;
import maspack.matrix.Point3d;
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
import maspack.util.NumberFormat;
import maspack.util.Range;
import maspack.util.ReaderTokenizer;

/**
 * Base class for scalar field defined over a mesh.
 */
public abstract class ScalarMeshField
   extends MeshFieldComp implements RenderableComponent, ScalarFieldComponent {

   double myDefaultValue = 0; 
   DoubleInterval myValueRange;

   RenderObject myRenderObj;

   public enum Visualization {
      POINT,
      FACE,
      SURFACE,
      OFF
   };

   public static Visualization DEFAULT_VISUALIZATION = Visualization.OFF;
   protected Visualization myVisualization = DEFAULT_VISUALIZATION;

   static final public ColorInterpolation 
      DEFAULT_COLOR_INTERPOLATION = ColorInterpolation.HSV;
   protected ColorInterpolation myColorInterp = DEFAULT_COLOR_INTERPOLATION;

   protected static ColorMapBase defaultColorMap =  new HueColorMap(2.0/3, 0);
   protected ColorMapBase myColorMap = defaultColorMap.copy();

   static ScalarRange defaultRenderRange = new ScalarRange();
   ScalarRange myRenderRange = defaultRenderRange.clone();

   public static PropertyList myProps =
      new PropertyList (ScalarMeshField.class, MeshFieldComp.class);

   static {
      myProps.add (
         "renderProps", "renderer properties", createDefaultRenderProps());
      myProps.add (
         "visualization", "how to visualize this field",
         DEFAULT_VISUALIZATION);
       myProps.add (
         "colorInterpolation", "interpolation for vertex coloring", 
         DEFAULT_COLOR_INTERPOLATION);
     myProps.add (
         "renderRange", "range for drawing color maps", 
         defaultRenderRange);
      myProps.add (
         "colorMap", "color map for visualization", 
         defaultColorMap, "CE");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }   

   public ScalarMeshField () {
      setRenderProps (createRenderProps());
   }

   public ScalarMeshField (MeshComponent mcomp) {
      setRenderProps (createRenderProps());
      myDefaultValue = 0;
      setMeshComp (mcomp);
   }

   public ScalarMeshField (MeshComponent mcomp, double defaultValue) {
      setRenderProps (createRenderProps());
      myDefaultValue = defaultValue;
      setMeshComp (mcomp);
   }

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
    * {@inheritDoc}
    */
   public double getValue (FemFieldPoint fp) {
      return getValue(fp.getSpatialPos());
   }

   /**
    * {@inheritDoc}
    */  
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "defaultValue")) {
         myDefaultValue = rtok.scanNumber();
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
      pw.println ("defaultValue=" + fmt.format(myDefaultValue));
   }

   /**
    * {@inheritDoc}
    */   
   public abstract double getValue (Point3d pos);
   
   /**
    * Returns the range of all values in this field, including
    * the default value.
    */
   public DoubleInterval getValueRange() {
      if (myValueRange == null) {
         DoubleInterval range =
            new DoubleInterval (myDefaultValue, myDefaultValue);
         updateValueRange (range);
         myValueRange = range;
      }
      return myValueRange;
   }

   /**
    * Updates range to include all values in this field
    */
   abstract void updateValueRange (DoubleInterval range);

   DoubleInterval updateRenderRange() {
      if (myRenderRange.getUpdating() != ScalarRange.Updating.FIXED) {
         myRenderRange.updateInterval (getValueRange());
      }
      return myRenderRange.getInterval();
   }

   /**
    * {@inheritDoc}
    */
   protected void notifyValuesChanged() {
      myValueRange = null;
   }

   /**
    * Clear all values defined for the features (e.g., vertices, faces)
    * associated with this field. After this call, the field will have a
    * uniform value defined by its {@code defaultValue}.
    */
   public abstract void clearAllValues();

   /**
    * Returns the default value for this field. See {@link #setDefaultValue}.
    *
    * @return default value for this field
    */
   public double getDefaultValue() {
      return myDefaultValue;
   }

   /**
    * Sets the default value for this field. Default values are used at
    * features (e.g., vertices, faces) for which values have not been
    * explicitly specified.
    * 
    * @param value new default value for this field
    */
   public void setDefaultValue (double value) {
      myDefaultValue = value;
   }

   /* --- Begin partial implementation of Renderable --- */

   // This default implementation of renderable provides for the rendering of
   // scalar values

   public RenderProps createRenderProps() {
      RenderProps props = RenderProps.createPointFaceProps (this);
      props.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      return props;
   }

   public static RenderProps createDefaultRenderProps() {
      RenderProps props = RenderProps.createPointFaceProps (null);
      props.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      return props;
   }

   protected RenderObject buildPointRenderObject(DoubleInterval range) {
      return null;
   }

   protected RenderObject buildMeshRenderObject(DoubleInterval range) {
      return null;
   }

   protected RenderObject buildFaceRenderObject(DoubleInterval range) {
      return null;
   }

   public void prerender (RenderList list) {
      switch (myVisualization) {
         case POINT: {
            DoubleInterval range = updateRenderRange();
            myRenderObj = buildPointRenderObject(range);
            break;
         }
         case FACE: {
            DoubleInterval range = updateRenderRange();
            myRenderObj = buildFaceRenderObject(range);
            break;
         }
         case SURFACE: {
            DoubleInterval range = updateRenderRange();
            myRenderObj = buildMeshRenderObject(range);
            break;
         }
         case OFF: {
            myRenderObj = null;
            break;
         }
         default:{
            myRenderObj = null;
            break;
         }
      }
   }
   
   public void render (Renderer renderer, int flags) {
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
               RenderableUtils.drawTriangles (
                  renderer, robj, 0, props, isSelected());
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
            case FACE: {
               if (robj.numTriangles() > 0) {
                  renderer.setShading (props.getShading());
                  renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
                  renderer.drawTriangles (robj, /*group=*/0);
               }
               break;
            }   
            default: {
               break;
            }
         }
      }
   }

   /* --- End partial implementation of Renderable --- */     
}
