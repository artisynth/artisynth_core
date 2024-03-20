package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import artisynth.core.femmodels.FemCutPlane;
import artisynth.core.femmodels.FemMesh;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.fields.ScalarFieldUtils.ScalarVertexFunction;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.MeshFieldPoint;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.ScalarFieldComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.ScalarRange;
import artisynth.core.util.ScanToken;
import maspack.geometry.PolygonalMesh;
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
import maspack.util.DynamicBooleanArray;
import maspack.util.DynamicDoubleArray;
import maspack.util.EnumRange;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.Range;
import maspack.util.ReaderTokenizer;

/**
 * Base class for scalar field defined over an FEM model.
 */
public abstract class ScalarFemField
   extends FemFieldComp implements RenderableComponent, ScalarFieldComponent {

   double myDefaultValue = 0;   
   DoubleInterval myValueRange;
 
   RenderObject myRenderObj;

   public enum Visualization {
      POINT,
      SURFACE,
      ELEMENT,
      OFF
   };

   protected ArrayList<FemMesh> myRenderMeshComps = new ArrayList<>();

   static final public ColorInterpolation 
      DEFAULT_COLOR_INTERPOLATION = ColorInterpolation.HSV;
   protected ColorInterpolation myColorInterp = DEFAULT_COLOR_INTERPOLATION;

   protected static ColorMapBase defaultColorMap =  new HueColorMap(2.0/3, 0);
   protected ColorMapBase myColorMap = defaultColorMap.copy();

   static ScalarRange defaultRenderRange = new ScalarRange();
   ScalarRange myRenderRange = defaultRenderRange.clone();

   public static Visualization DEFAULT_VISUALIZATION = Visualization.OFF;
   protected Visualization myVisualization = DEFAULT_VISUALIZATION;

   public static PropertyList myProps =
      new PropertyList (ScalarFemField.class, FemFieldComp.class);

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

   // property accessors

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

   protected boolean isTriangular (FemMesh mcomp) {
      return ((mcomp.getMesh() instanceof PolygonalMesh) &&
              ((PolygonalMesh)mcomp.getMesh()).isTriangular());
   }

   public void addRenderMeshComp (FemMesh mcomp) {
      if (!(mcomp instanceof FemMeshComp) &&
          !(mcomp instanceof FemCutPlane)) {
         throw new IllegalArgumentException (
            "Render mesh must be FemMeshComp or a FemCutPlane");
      }
      if (mcomp.getFem() != myFem) {
         throw new IllegalArgumentException (
            "Render mesh component not associated with field's FEM");
      }
      if (!isTriangular(mcomp)) {
         throw new IllegalArgumentException ("Render mesh is not triangular");
      }
      myRenderMeshComps.add (mcomp);
   }

   public boolean removeRenderMeshComp (FemMesh mcomp) {
      return myRenderMeshComps.remove (mcomp);
   }
   
   public FemMesh getRenderMeshComp (int idx) {
      return myRenderMeshComps.get(idx);
   }
   
   public int numRenderMeshComps() {
      return myRenderMeshComps.size();
   }
   
   public void clearRenderMeshComps () {
      myRenderMeshComps.clear();
   }
   
   public ScalarFemField () {
      setRenderProps (createRenderProps());
   }

   public ScalarFemField (FemModel3d fem) {
      setRenderProps (createRenderProps());
      myDefaultValue = 0;
      setFem (fem);
   }

   public ScalarFemField (FemModel3d fem, double defaultValue) {
      setRenderProps (createRenderProps());
      myDefaultValue = defaultValue;
      setFem (fem);
   }

   /**
    * {@inheritDoc}
    */
   public double getValue (MeshFieldPoint fp) {
      return getValue(fp.getPosition());
   }
   
   /* ---- Begin I/O methods ---- */

   protected void writeValues (
      PrintWriter pw, NumberFormat fmt, DynamicDoubleArray values, 
      DynamicBooleanArray valuesSet, WritableTest writableTest)
      throws IOException {

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int num=0; num<values.size(); num++) {
         if (!valuesSet.get(num) || !writableTest.isWritable(num)) {
            pw.println ("null");
         }
         else {
            pw.println (fmt.format (values.get(num)));
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }
 
   protected void scanValues (
      ReaderTokenizer rtok,
      DynamicDoubleArray values, DynamicBooleanArray valuesSet)
      throws IOException {

      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            values.add (0);
            valuesSet.add (false);
         }
         else if (rtok.tokenIsNumber()) {
            values.add (rtok.nval);
            valuesSet.add (true);
         }
         else {
            throw new IOException ("Expecting number or 'null', got "+rtok);
         }
      }
      notifyValuesChanged();
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
      else if (ScanWriteUtils.scanAndStoreReferences (
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
      pw.println ("defaultValue=" + fmt.format(myDefaultValue));
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
            tokens, myRenderMeshComps, FemMesh.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   /* ---- value methods ---- */

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
    * Clear all values defined for the features (e.g., nodes, elements)
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
    * features (e.g., nodes, elements) for which values have not been
    * explicitly specified.
    * 
    * @param value new default value for this field
    */
   public void setDefaultValue (double value) {
      myDefaultValue = value;
      notifyValuesChanged();
   }
   
   /**
    * Checks if two values/valset pairings are equal. Each pairing is described
    * by a double dynamic array giving the value, and a boolean dynamic array
    * indicated if the value is actually set. When comparing the pairs, if a
    * value is not set for some index {@code i}, then the actual values at
    * {@code i} are ignored.
    */
   protected boolean valueSetArraysEqual (
      DynamicDoubleArray values0, DynamicBooleanArray valset0, 
      DynamicDoubleArray values1, DynamicBooleanArray valset1) {

      if (values0.size() != valset0.size()) {
         throw new IllegalArgumentException (
            "values0 and valset0 have different sizes");
      }
      if (values1.size() != valset1.size()) {
         throw new IllegalArgumentException (
            "values1 and valset1 have different sizes");
      }
      if (valset0.size() != valset1.size()) {
         return false;
      }
      for (int i=0; i<values0.size(); i++) {
         if (valset0.get(i) != valset1.get(i) ||
             (valset0.get(i) && values0.get(i) != values1.get(i))) {
            return false;
         }
      }
      return true;      
   }

   /**
    * Returns {@code true} if this field is functionally equal to another field.
    * Intended mainly for testing and debugging.
    */
   public boolean equals (ScalarFemField field) {
      return (
         super.equals (field) &&
         myDefaultValue == field.getDefaultValue());
   }

   /* --- Begin partial implementation of Renderable --- */

   protected int getColorIndex (double value, DoubleInterval range) {
      return ScalarFieldUtils.getColorIndex (value, range);
   }

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
      // by default, don't render anything
      return null;
   }

   protected RenderObject buildElementRenderObject(DoubleInterval range) {
      // by default, don't render anything
      return null;
   }

   protected ScalarVertexFunction getVertexFunction () {
      return null;
   }

   public void prerender (RenderList list) {
      switch (myVisualization) {
         case SURFACE: {
            RenderObject robj = null;
            ScalarVertexFunction vfxn = getVertexFunction();
            if (vfxn != null) {
               DoubleInterval range = updateRenderRange();
               ArrayList<FemMesh> rmeshComps = null;
               if (myRenderMeshComps.size() > 0) {
                  rmeshComps = myRenderMeshComps;
               }
               else {
                  rmeshComps = new ArrayList<>(1);
                  rmeshComps.add (myFem.getSurfaceMeshComp());
               }
               if (rmeshComps != null) {
                  robj = ScalarFieldUtils.buildMeshRenderObject (
                     rmeshComps, myColorMap, range, vfxn);
               }
            }
            myRenderObj = robj;
            break;
         }
         case POINT: {
            DoubleInterval range = updateRenderRange();
            myRenderObj = buildPointRenderObject(range);
            break;
         }
         case ELEMENT: {
            DoubleInterval range = updateRenderRange();
            myRenderObj = buildElementRenderObject(range);
            break;
         }
         case OFF: {
            myRenderObj = null;
            break;
         }
         default:{
            throw new UnsupportedOperationException (
               "Visualiztion "+myVisualization+" not implemented");
         }
      }
   }

   public void render (Renderer renderer, int flags) {
      RenderObject robj = myRenderObj;
      RenderProps props = myRenderProps;
      
      if (robj != null) {
         switch (myVisualization) {
            case SURFACE: {
               boolean selecting = renderer.isSelecting();

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
            case ELEMENT: {
               if (robj.numTriangles(0) > 0) {
                  renderer.setShading (props.getShading());
                  renderer.drawTriangles (robj, /*group=*/0);
               }
               if (robj.numTriangles(1) > 0) {
                  renderer.setShading (props.getShading());
                  renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
                  renderer.drawTriangles (robj, /*group=*/1);
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
   }

   @Override
   public int numSelectionQueriesNeeded() {
      if (getVisualization() == Visualization.SURFACE) {
         return myRenderMeshComps.size();
      }
      else {
         return -1;
      }
   }

   @Override
   public void getSelection(LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < myRenderMeshComps.size()) {
         list.addLast(myRenderMeshComps.get(qid));
      }
   }

   // render method will be implemented as needed by subclasses

   /* --- Edit methods --- */

   /**
    * {@inheritDoc}
    */
   public void getSoftReferences (List<ModelComponent> refs) {
      for (FemMesh mcomp : myRenderMeshComps) {
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
