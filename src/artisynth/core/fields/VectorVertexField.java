package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;

import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.MeshFieldPoint;
import artisynth.core.util.ScanToken;
import maspack.geometry.Vertex3d;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorObject;
import maspack.render.RenderObject;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A vector field defined over a triangular polygonal mesh, using values set at
 * the mesh's vertices. Values at other points are obtained by barycentric
 * interpolation on the faces nearest to those points. Values at vertices for
 * which no explicit value has been set are given by the field's
 * <i>default value</i>. Vectors are of type {@code T}, which must be an
 * instance of {@link VectorObject}.
 */
public class VectorVertexField<T extends VectorObject<T>>
   extends VectorMeshField<T> {

   ArrayList<T> myValues;

   protected void initValues() {
      myValues = new ArrayList<>();
      updateValueLists();
      setRenderProps (createRenderProps());
   }

   protected void updateValueLists() {
      int size = myMeshComp.numVertices();
      resizeArrayList (myValues, size);
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public VectorVertexField (Class<T> type) {
      super (type);
   }

   /**
    * Constructs a field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param type class type of the {@link VectorObject} describing the field's
    * values
    * @param mcomp component containing the mesh associated with the field
    */
   public VectorVertexField (Class<T> type, MeshComponent mcomp) {
      super (type, mcomp);
      initValues();
   }

   /**
    * Constructs a field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param type class type of the {@link VectorObject} describing the field's
    * values
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for vertices which don't have
    * explicitly set values
    */
   public VectorVertexField (Class<T> type, MeshComponent mcomp, T defaultValue) {
      super (type, mcomp, defaultValue);
      initValues();
   }

   /**
    * Constructs a named field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param name name of the field
    * @param type class type of the {@link VectorObject} describing the field's
    * values
    * @param mcomp component containing the mesh associated with the field
    */
   public VectorVertexField (String name, Class<T> type, MeshComponent mcomp) {
      this (type, mcomp);
      setName (name);
   }

   /**
    * Constructs a named field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param name name of the field
    * @param type class type of the {@link VectorObject} describing the field's
    * values
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for vertices which don't have
    * explicitly set values
    */
   public VectorVertexField (
      String name, Class<T> type, MeshComponent mcomp, T defaultValue) {
      this (type, mcomp, defaultValue);
      setName (name);
   }

   /**
    * Returns the value at the vertex specified by a given index. The default
    * value is returned if a value has not been explicitly set for that vertex.
    * 
    * @param vidx vertex index
    * @return value at the vertex
    */
   public T getValue (int vidx) {
      if (vidx >= myMesh.numVertices()) {
         throw new IllegalArgumentException (
            "vertex index "+vidx+" exceeds number of vertices "+
            myMesh.numVertices());
      }
      else if (vidx < myValues.size() && myValues.get(vidx) != null) {
         return myValues.get (vidx);
      }
      else {
         return myDefaultValue;
      }
   }

   /**
    * Returns the value at a vertex. The default value is returned if a value
    * has not been explicitly set for that vertex.
    *
    * @param vtx vertex for which the value is requested
    * @return value at the vertex
    */
   public T getValue (Vertex3d vtx) {
      checkVertexBelongsToMesh (vtx);
      return getValue (vtx.getIndex());
   }

   /**
    * {@inheritDoc}
    */
   public T getValue (Point3d pos) {
      return getValue (createFieldPoint(pos));
   }
   
   /**
    * {@inheritDoc}
    */
   public T getValue (MeshFieldPoint fp) {
      T vec = createTypeInstance();
      Vertex3d[] vtxs = fp.getVertices();
      double[] weights = fp.getWeights();
      int numv = fp.numVertices();
      for (int k=0; k<numv; k++) {
         vec.scaledAddObj (weights[k], getValue (vtxs[k]));
      }
      return vec;
   }

   /**
    * Sets the value at a vertex.
    * 
    * @param vtx vertex for which the value is to be set
    * @param value new value for the vertex
    */
   public void setValue (Vertex3d vtx, T value) {
      checkVertexBelongsToMesh (vtx);
      String sizeErr = checkSize (value);
      if (sizeErr != null) {
         throw new IllegalArgumentException (
            "value for vertex "+vtx.getIndex()+": "+sizeErr);
      }
      int vidx = vtx.getIndex();
      if (vidx >= myValues.size()) {
         updateValueLists();
      }
      if (vidx < myValues.size()) {
         T storedValue = createTypeInstance();
         storedValue.set (value);
         myValues.set (vidx, storedValue);
      }
   }

   /**
    * Queries whether a value has been seen at a given vertex.
    * 
    * @param vtx vertex being queried
    * @return {@code true} if a value has been set at the vertex
    */
   public boolean isValueSet (Vertex3d vtx) {
      checkVertexBelongsToMesh (vtx);
      int vidx = vtx.getIndex();
      if (vidx < myValues.size()) {
         return myValues.get (vidx) != null;
      }
      else {
         return false;
      }
   }

   /**
    * Clears the value at a given vertex. After this call, the vertex will be
    * associated with the default value.
    * 
    * @param vtx vertex whose value is to be cleared
    */
   public void clearValue (Vertex3d vtx) {
      checkVertexBelongsToMesh (vtx);
      int vidx = vtx.getIndex();
      if (vidx < myValues.size()) {
         myValues.set (vidx, null);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void clearAllValues() {
      for (int i=0; i<myValues.size(); i++) {
         myValues.set (i, null);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("values=");
      writeValues (pw, fmt, myValues);
   }

   /**
    * {@inheritDoc}
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValues = new ArrayList<T>();
         scanValues (rtok, myValues);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   /**
    * {@inheritDoc}
    */
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      updateValueLists();
   }

   protected RenderObject buildRenderObject() {
      if (myRenderScale != 0 && hasThreeVectorValue()) {
         RenderObject robj = new RenderObject();
         robj.createLineGroup();
         Point3d pos = new Point3d();
         Vector3d vec = new Vector3d();
         for (int idx=0; idx<myValues.size(); idx++) {
            if (getThreeVectorValue (vec, myValues.get(idx))) {
               Vertex3d vtx = myMeshComp.getMesh().getVertex (idx);
               addLineSegment (robj, vtx.getPosition(), vec);
            }
         }
         return robj;
      }
      else {
         return null;
      }
   }
   
}
