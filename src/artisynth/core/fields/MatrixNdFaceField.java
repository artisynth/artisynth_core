package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.mechmodels.*;
import artisynth.core.util.ScanToken;
import maspack.matrix.Vector3d;
import maspack.matrix.MatrixNd;
import maspack.matrix.VectorObject;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.geometry.PolygonalMesh;

/**
 * A vector field, for vectors of type {@link MatrixNd}, defined over a
 * triangular polygonal mesh, using values set at the mesh's vertices. Values
 * at other points are obtained by barycentric interpolation on the faces
 * nearest to those points. Values at vertices for which no explicit value has
 * been set are given by the field's <i>default value</i>. The {@code
 * MatrixNd} values must be of a fixed size as specified in the field's
 * constructor.
 */
public class MatrixNdFaceField extends VectorFaceField<MatrixNd> {

   protected int myRowSize;
   protected int myColSize;

   private void initSize (int rowSize, int colSize) {
      if (rowSize <= 0 || colSize <= 0) {
         throw new IllegalArgumentException (
            "specified matrix size is "+rowSize+"x"+colSize+
            ", both must be > 0");
      }
      myRowSize = rowSize;
      myColSize = colSize;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String checkSize (MatrixNd value) {
      if (value.rowSize() != myRowSize || value.colSize() != myColSize) {
         return ("size "+value.getSize()+
                 " incompatible with field size of "+myRowSize+"x"+myColSize);
      }
      else {
         return null;
      }
   }  

   /**
    * {@inheritDoc}
    */
   @Override
   public MatrixNd createTypeInstance () {
      return new MatrixNd (myRowSize, myColSize);
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean hasParameterizedType() {
      return false;
   }   

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public MatrixNdFaceField () {
      super (MatrixNd.class);
   }
   
  /**
    * Constructs a field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param rowSize row size of the field's {@code MatrixNd} values
    * @param colSize column size of the field's {@code MatrixNd} values
    * @param mcomp component containing the mesh associated with the field
    */   
   public MatrixNdFaceField (int rowSize, int colSize, MeshComponent mcomp) {
      super (MatrixNd.class, mcomp);
      initSize (rowSize, colSize);
      initValues();
   }

   /**
    * Constructs a field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param rowSize row size of the field's {@code MatrixNd} values
    * @param colSize column size of the field's {@code MatrixNd} values
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for faces which don't have
    * explicitly set values
    */
   public MatrixNdFaceField (
      int rowSize, int colSize, MeshComponent mcomp, MatrixNd defaultValue) {
      super (MatrixNd.class, mcomp, defaultValue);
      initSize (rowSize, colSize);
      initValues();
   }

   /**
    * Constructs a named field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param name name of the field
    * @param rowSize row size of the field's {@code MatrixNd} values
    * @param colSize column size of the field's {@code MatrixNd} values
    * @param mcomp component containing the mesh associated with the field
    */
   public MatrixNdFaceField (
      String name, int rowSize, int colSize, MeshComponent mcomp) {
      this (rowSize, colSize, mcomp);
      setName (name);
   }

   /**
    * Constructs a named field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param name name of the field
    * @param rowSize row size of the field's {@code MatrixNd} values
    * @param colSize column size of the field's {@code MatrixNd} values
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for faces which don't have
    * explicitly set values
    */
   public MatrixNdFaceField (
      String name, int rowSize, int colSize,
      MeshComponent mcomp, MatrixNd defaultValue) {
      this (rowSize, colSize, mcomp, defaultValue);
      setName (name);
   }

   /**
    * {@inheritDoc}
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("size=[ "+myRowSize+" "+myColSize+" ]");
      super.writeItems (pw, fmt, ancestor);
   }

   /**
    * {@inheritDoc}
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "size")) {
         rtok.scanToken ('[');
         int rowSize = rtok.scanInteger();
         int colSize = rtok.scanInteger();
         rtok.scanToken (']');
         initSize (rowSize, colSize);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }
}
