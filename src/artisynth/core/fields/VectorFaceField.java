package artisynth.core.fields; 

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;

import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.MeshFieldPoint;
import artisynth.core.util.ScanToken;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorObject;
import maspack.render.RenderObject;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A vector field defined over a triangular polygonal mesh, using values set at
 * the mesh's faces. Values at other points are obtained by finding the faces
 * nearest to those points. Values at faces for which no explicit value has
 * been set are given by the field's <i>default value</i>. Since values
 * are assumed to be constant over a given face, this field is not continuous.
 * Vectors are of type {@code T}, which must be an instance of {@link
 * VectorObject}.
 */
public class VectorFaceField<T extends VectorObject<T>>
   extends VectorMeshField<T> {

   protected ArrayList<T> myValues;

   protected void initValues () {
      myValues = new ArrayList<T>();
      updateValueLists();
   }

   protected void updateValueLists() {
      int maxFaces = ((PolygonalMesh)myMeshComp.getMesh()).numFaces();
      resizeArrayList (myValues, maxFaces);
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public VectorFaceField (Class<T> type) {
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
   public VectorFaceField (Class<T> type, MeshComponent mcomp) {
      super (type, mcomp, null);
      initValues ();
   }

   /**
    * Constructs a field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param type class type of the {@link VectorObject} describing the field's
    * values
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for faces which don't have
    * explicitly set values
    */
   public VectorFaceField (Class<T> type, MeshComponent mcomp, T defaultValue) {
      super (type, mcomp, defaultValue);
      initValues ();
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
   public VectorFaceField (String name, Class<T> type, MeshComponent mcomp) {
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
    * @param defaultValue default value for faces which don't have
    * explicitly set values
    */
   public VectorFaceField (
      String name, Class<T> type, MeshComponent mcomp, T defaultValue) {
      this (type, mcomp, defaultValue);
      setName (name);
   }

   /**
    * Returns the value at the face specified by a given index. The default
    * value is returned if a value has not been explicitly set for that face.
    * 
    * @param fidx face index
    * @return value at the face
    */
   public T getValue (int fidx) {
      if (fidx >= myMesh.numFaces()) {
         throw new IllegalArgumentException (
            "face index "+fidx+" exceeds number of faces "+
            myMesh.numFaces());
      }
      else if (fidx < myValues.size() && myValues.get(fidx) != null) {
         return myValues.get(fidx);
      }
      else {
         return myDefaultValue;
      }
   }

   /**
    * Returns the value at a face. The default value is returned if a value
    * has not been explicitly set for that face.
    *
    * @param face face for which the value is requested
    * @return value at the face
    */
   public T getValue (Face face) {
      checkFaceBelongsToMesh (face);
      return getValue (face.getIndex());
   }

   /**
    * {@inheritDoc}
    */
   public T getValue (Point3d pos) {
      return getValue (createFieldPoint (pos));
   }
   
   private double computeTriangularFaceArea (HalfEdge he) {
      Point3d p0 = he.getTail().getPosition();
      Point3d p1 = he.getHead().getPosition();
      Point3d p2 = he.getNext().getHead().getPosition();
      return Face.computeTriangleArea (p0, p1, p2);
   }

   /**
    * {@inheritDoc}
    */
   public T getValue (MeshFieldPoint fp) {
      Vertex3d[] vtxs = fp.getVertices();
      switch (vtxs.length) {
         case 1: {
            T val = createTypeInstance();
            double wsum = 0;
            Iterator<HalfEdge> hedges = vtxs[0].getIncidentHalfEdges();
            while (hedges.hasNext()) {
               HalfEdge he = hedges.next();
               double w = computeTriangularFaceArea (he);
               val.scaledAddObj (w, getValue(he.getFace()));
               wsum += w;
            }
            val.scaleObj (1/wsum);
            return val;
         }
         case 2: {
            // edge: average face values
            HalfEdge he = vtxs[1].findIncidentHalfEdge (vtxs[0]);
            if (he != null) {
               T val = createTypeInstance();
               val.scaledAddObj (0.5, getValue(he.getFace()));
               val.scaledAddObj (0.5, getValue(he.getOppositeFace()));
               return val;
            }
            break;
         }
         case 3: {
            // single face
            HalfEdge he = vtxs[1].findIncidentHalfEdge (vtxs[0]);
            if (he != null) {
               return getValue(he.getFace());
            }
            // try the other way around, although it shouldn't happen
            he = vtxs[0].findIncidentHalfEdge (vtxs[1]);
            if (he != null) {
               return getValue(he.getFace());
            }
            break;
         }
      }
      return myDefaultValue;
   }

   /**
    * Sets the value at a face.
    * 
    * @param face face for which the value is to be set
    * @param value new value for the face
    */
   public void setValue (Face face, T value) {
      checkFaceBelongsToMesh (face);
      String sizeErr = checkSize (value);
      if (sizeErr != null) {
         throw new IllegalArgumentException (
            "value for face "+face.getIndex()+": "+sizeErr);
      }
      int fidx = face.getIndex();
      if (fidx >= myValues.size()) {
         updateValueLists();
      }
      if (fidx < myValues.size()) {
         T storedValue = createTypeInstance();
         storedValue.set (value);
         myValues.set (fidx, storedValue);
      }
   }

   /**
    * Queries whether a value has been seen at a given face.
    * 
    * @param face face being queried
    * @return {@code true} if a value has been set at the face
    */
   public boolean isValueSet (Face face) {
      checkFaceBelongsToMesh (face);
      int fidx = face.getIndex();
      if (fidx < myValues.size()) {
         return myValues.get (fidx) != null;
      }
      else {
         return false;
      }
   }

   /**
    * Clears the value at a given face. After this call, the face will be
    * associated with the default value.
    * 
    * @param face face whose value is to be cleared
    */
   public void clearValue (Face face) {
      checkFaceBelongsToMesh (face);
      int fidx = face.getIndex();
      if (fidx < myValues.size()) {
         myValues.set (fidx, null);
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
      pw.print ("values=");
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

   // build render object for rendering Vector3d values

   protected RenderObject buildRenderObject() {
      if (myRenderScale != 0 && hasThreeVectorValue()) {
         RenderObject robj = new RenderObject();
         robj.createLineGroup();
         Point3d pos = new Point3d();
         Vector3d vec = new Vector3d();
         for (int idx=0; idx<myValues.size(); idx++) {
            if (getThreeVectorValue (vec, myValues.get(idx))) {
               Face face = ((PolygonalMesh)myMeshComp.getMesh()).getFace(idx);
               face.computeCentroid (pos);
               addLineSegment (robj, pos, vec);
            }
         }
         return robj;
      }
      else {
         return null;
      }
   }

}
