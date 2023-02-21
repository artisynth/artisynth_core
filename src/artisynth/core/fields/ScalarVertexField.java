package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.MeshFieldPoint;
import artisynth.core.util.ScanToken;
import maspack.geometry.Vertex3d;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.util.DynamicBooleanArray;
import maspack.util.DynamicDoubleArray;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A scalar field defined over a triangular polygonal mesh, using values set at
 * the mesh's vertices. Values at other points are obtained by barycentric
 * interpolation on the faces nearest to those points. Values at vertices for
 * which no explicit value has been set are given by the field's <i>default
 * value</i>.
 */
public class ScalarVertexField extends ScalarMeshField {

   DynamicDoubleArray myValues;
   DynamicBooleanArray myValuesSet;

   protected void initValues() {
      myValues = new DynamicDoubleArray();
      myValuesSet = new DynamicBooleanArray();
      updateValueLists();
   }

   protected void updateValueLists() {
      int size = myMeshComp.numVertices();
      myValues.resize (size);
      myValuesSet.resize (size);
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public ScalarVertexField () {
   }
   
   /**
    * Constructs a field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param mcomp component containing the mesh associated with the field
    */
   public ScalarVertexField (MeshComponent mcomp) {
      super (mcomp);
      initValues();
   }

   /**
    * Constructs a field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for vertices which don't have
    * explicitly set values
    */
   public ScalarVertexField (MeshComponent mcomp, double defaultValue) {
      super (mcomp, defaultValue);
      initValues();
   }

   /**
    * Constructs a named field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param name name of the field
    * @param mcomp component containing the mesh associated with the field
    */
   public ScalarVertexField (String name, MeshComponent mcomp) {
      this (mcomp);
      setName (name);
   }

   /**
    * Constructs a named field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param name name of the field
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for vertices which don't have
    * explicitly set values
    */
   public ScalarVertexField (
      String name, MeshComponent mcomp, double defaultValue) {
      this (mcomp, defaultValue);
      setName (name);
   }

   /**
    * Returns the value at the vertex specified by a given index. The default
    * value is returned if a value has not been explicitly set for that vertex.
    * 
    * @param vidx vertex index
    * @return value at the vertex
    */
   public double getValue (int vidx) {
      if (vidx >= myMesh.numVertices()) {
         throw new IllegalArgumentException (
            "vertex index "+vidx+" exceeds number of vertices "+
            myMesh.numVertices());
      }
      else if (vidx < myValuesSet.size() && myValuesSet.get(vidx)) {
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
   public double getValue (Vertex3d vtx) {
      checkVertexBelongsToMesh (vtx);
      return getValue (vtx.getIndex());
   }

   /**
    * {@inheritDoc}
    */
   public double getValue (Point3d pos) {
      return getValue (createFieldPoint(pos));
   }
   
   /**
    * {@inheritDoc}
    */
   public double getValue (MeshFieldPoint fp) {
      Vertex3d[] vtxs = fp.getVertices();
      double[] weights = fp.getWeights();
      int numv = fp.numVertices();
      double value = 0;
      for (int k=0; k<numv; k++) {
         value += weights[k]*getValue (vtxs[k]);
      }
      return value;
   }

   /**
    * Sets the value at a vertex.
    * 
    * @param vtx vertex for which the value is to be set
    * @param value new value for the vertex
    */
   public void setValue (Vertex3d vtx, double value) {
      checkVertexBelongsToMesh (vtx);
      int vidx = vtx.getIndex();
      if (vidx >= myValues.size()) {
         updateValueLists();
      }
      if (vidx < myValues.size()) {
         myValues.set (vidx, value);
         myValuesSet.set (vidx, true);
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
      if (vidx < myValuesSet.size()) {
         return myValuesSet.get (vidx);
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
      if (vidx < myValuesSet.size()) {
         myValuesSet.set (vidx, false);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void clearAllValues() {
      for (int i=0; i<myValuesSet.size(); i++) {
         myValuesSet.set (i, false);
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
      writeValues (pw, fmt, myValues, myValuesSet);
   }

   /**
    * {@inheritDoc}
    */  
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValues = new DynamicDoubleArray();
         myValuesSet = new DynamicBooleanArray();
         scanValues (rtok, myValues, myValuesSet);
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
}
