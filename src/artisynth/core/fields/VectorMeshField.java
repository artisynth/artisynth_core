package artisynth.core.fields;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;

import java.util.ArrayList;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.util.ParameterizedClass;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.Renderer.*;

/**
 * Base class for vector fields of type {@code T} defined over a mesh, where
 * {@code T} is an instance of {@link VectorObject}.
 */
public abstract class VectorMeshField<T extends VectorObject<T>>
   extends MeshFieldComp
   implements VectorFieldComponent<T>, ParameterizedClass, RenderableComponent {
   
   protected T myDefaultValue = null;
   protected Class<T> myTypeParameter = null;
   protected TypeCode myValueType = TypeCode.OTHER;

   protected static double DEFAULT_RENDER_SCALE = 0;
   protected double myRenderScale = DEFAULT_RENDER_SCALE;
   protected RenderObject myRenderObj = null;

   public static PropertyList myProps =
      new PropertyList (VectorMeshField.class, MeshFieldComp.class);

   static {
      myProps.add (
         "renderProps", "renderer properties", createDefaultRenderProps());
      myProps.add (
         "renderScale", "scale factor for rendered values", DEFAULT_RENDER_SCALE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }   

   /**
    * Queries the {@code renderScale} property for this vector mesh field.
    * See {@link #setRenderScale}.
    *
    * @return render scale property value
    */
   public double getRenderScale() {
      return myRenderScale;
   }

   /**
    * Set the {@code renderScale} property for this vector mesh field.  This is
    * used to scale the rendered size of the vector components of this field in
    * subclasses that support rendering of those values. The default value is
    * 0, meaning that the components will not be rendered.
    *
    * @param scale new scale property value
    */
   public void setRenderScale (double scale) {
      myRenderScale = scale;
   }

   protected void initType (Class<T> type) {
      myTypeParameter = type;
      myValueType = PropertyDesc.getTypeCode(type);
   }
   
   /**
    * {@inheritDoc}
    */
   public Class<T> getParameterType() {
      return myTypeParameter;
   }   
   
   /**
    * {@inheritDoc}
    */
   public boolean hasParameterizedType() {
      return true;
   }   

   /**
    * Checks that the size of the supplied vector value matches the size
    * required by this field. If it does not, returns an error message
    * describing the size error. If it does, returns {@code null}. This method
    * only needs to be overridden by subclasses implementing vector values
    * whose size is variable.
    *
    * @param value vector value whose size is to be checked
    */
   protected String checkSize (T value) {
      // size check only needed when T is VectorNd or MatrixNd
      return null;
   }
   
   /**
    * Create an instance of the VectorObject type associated with this
    * field.
    * 
    * @return new vector type
    */
   public T createTypeInstance () {
      try {
         return myTypeParameter.newInstance();
      }
      catch (Exception e) {
         throw new InternalErrorException (
            "Cannot create instance of "+myTypeParameter);
      }
   }

   protected void initMeshAndDefaultValue (MeshComponent mcomp, T defaultValue) {
      if (defaultValue == null) {
         myDefaultValue = createTypeInstance();
      }
      else {
         String sizeErr = checkSize (defaultValue);
         if (sizeErr != null) {
            throw new IllegalArgumentException (
               "default value: "+sizeErr);
         }
         myDefaultValue = defaultValue;
      }
      setMeshComp (mcomp);
   }     

   public VectorMeshField (Class<T> type) {
      initType (type);
      setRenderProps (createRenderProps());
   }

   public VectorMeshField (Class<T> type, MeshComponent mcomp) {
      initType (type);
      setRenderProps (createRenderProps());
      initMeshAndDefaultValue (mcomp, null);
   }

   public VectorMeshField (Class<T> type, MeshComponent mcomp, T defaultValue) {
      initType (type);
      setRenderProps (createRenderProps());
      initMeshAndDefaultValue (mcomp, defaultValue);
   }

   /**
    * {@inheritDoc}
    */
   public abstract T getValue (Point3d pos);
   
   /**
    * {@inheritDoc}
    */
   public T getValue (FemFieldPoint fp) {
      return getValue(fp.getSpatialPos());
   }

   protected T scanValue (ReaderTokenizer rtok) throws IOException {
      rtok.nextToken();
      if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
         return null;
      }
      else {
         rtok.pushBack();
         T value = (T)PropertyDesc.scanValue (
            rtok, myValueType, myTypeParameter);
         String sizeErr = checkSize (value);
         if (sizeErr != null) {
            throw new IOException (
               "scanned value: "+sizeErr+", line "+rtok.lineno());
         }
         return value;
      }
   }

   protected <S> void writeValues (
      PrintWriter pw, NumberFormat fmt, ArrayList<S> values) 
      throws IOException {

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int num=0; num<values.size(); num++) {
         S val = values.get(num);
         if (val == null) {
            pw.println ("null");
         }
         else {
            PropertyDesc.writeValue (
               val, pw, myValueType, myTypeParameter, fmt, null, null);
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }
 
   protected <S> void scanValues (
      ReaderTokenizer rtok, ArrayList<S> values) throws IOException {
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         S value;
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            value = null;
         }
         else {
            rtok.pushBack();
            value = (S)PropertyDesc.scanValue (
               rtok, myValueType, myTypeParameter);
         }
         values.add (value);
      }
   }

   protected <S> void scanValueArrays (
      ReaderTokenizer rtok, ArrayList<S[]> valueArrays) throws IOException {
      ArrayList<S> values = new ArrayList<>();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            valueArrays.add (null);
         }
         else {
            if (rtok.ttype != '[') {
               throw new IOException ("Expecting token '[', got "+rtok);
            }
            values.clear();
            while (rtok.nextToken() != ']') {
               S value;
               if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
                  value = null;
               }
               else {
                  rtok.pushBack();
                  value = (S)PropertyDesc.scanValue (
                     rtok, myValueType, myTypeParameter);
               }
               values.add (value);              
            }
            valueArrays.add ((S[])values.toArray());
         }
      }
   }

   protected <S> void writeValue (
      PrintWriter pw, NumberFormat fmt, S val) throws IOException {
      if (val == null) {
         pw.println ("null");
      }
      else {
         PropertyDesc.writeValue (
            val, pw, myValueType, myTypeParameter, fmt, null, /*ancestor=*/null);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "type")) {
         Class<T> clazz = (Class<T>)Scan.scanClass (rtok);
         initType (clazz);
         return true;
      }
      else if (scanAttributeName (rtok, "defaultValue")) {
         myDefaultValue = scanValue (rtok);
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
      pw.println ("type=" + myTypeParameter.getName());
      pw.print ("defaultValue=");
      writeValue (pw, fmt, myDefaultValue);
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
   public T getDefaultValue() {
      return myDefaultValue;
   }

   /**
    * Sets the default value for this field. Default values are used at
    * features (e.g., vertices, faces) for which values have not been
    * explicitly specified.
    * 
    * @param value new default value for this field
    */
   public void setDefaultValue (T value) {
      try {
         myDefaultValue = (T)value.clone();
      }
      catch (Exception e) {
         throw new InternalErrorException (
            "Can't clone instance of "+value.getClass());
      }
   }

   /* --- Begin partial implementation of Renderable --- */

   // This default implementation of renderable provides for the rendering of
   // 3D vectors as lines

   public RenderProps createRenderProps() {
      RenderProps props = RenderProps.createPointLineProps (this);
      return props;
   }

   public static RenderProps createDefaultRenderProps() {
      RenderProps props = RenderProps.createPointLineProps (null);
      return props;
   }

   public void prerender (RenderList list) {
      myRenderObj = buildRenderObject();
   }

   protected boolean hasThreeVectorValue() {
      return Vector.class.isAssignableFrom (myTypeParameter);
   }

   void addLineSegment (RenderObject robj, Point3d pos, Vector3d vec) {
      Point3d vpos = new Point3d(pos);
      robj.vertex (vpos);
      vpos.scaledAdd (myRenderScale, vec);
      int vidx = robj.vertex (vpos);      
      robj.addLine (vidx-1, vidx);
   }

   protected RenderObject buildRenderObject() {
      // by default, don't render anything
      return null;
   }

   public void render (Renderer renderer, int flags) {
      RenderObject robj = myRenderObj;
      
      if (robj != null) {
         double size;
         // draw the directions
         LineStyle lineStyle = myRenderProps.getLineStyle();
         if (lineStyle == LineStyle.LINE) {
            size = myRenderProps.getLineWidth();
         }
         else {
            size = myRenderProps.getLineRadius();
         }
         renderer.setLineColoring (myRenderProps, isSelected());
         renderer.drawLines (robj, lineStyle, size);
      }
   }

   /* --- End partial implementation of Renderable --- */   
}
