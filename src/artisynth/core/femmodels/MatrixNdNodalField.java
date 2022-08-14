package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.util.ScanToken;
import maspack.matrix.MatrixNd;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A vector field, for vectors of type {@link MatrixNd}, defined over an FEM
 * model, using values set at the nodes. Values at other points are obtained by
 * nodal interpolation on the elements nearest to those points. Values at nodes
 * for which no explicit value has been set are given by the field's <i>default
 * value</i>. The {@code MatrixNd} values must be of a fixed size as
 * specified in the field's constructor.
 */
public class MatrixNdNodalField extends VectorNodalField<MatrixNd> {

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
   public MatrixNdNodalField () {
      super (MatrixNd.class);
   }
   
   /**
    * Constructs a field for a given FEM model, with a default value of 0.
    *
    * @param rowSize row size of the field's {@code MatrixNd} values
    * @param colSize column size of the field's {@code MatrixNd} values
    * @param fem FEM model over which the field is defined
    */
   public MatrixNdNodalField (int rowSize, int colSize, FemModel3d fem) {
      super (MatrixNd.class); 
      initSize (rowSize, colSize);
      initFemAndDefaultValue (fem, null);
      initValues();
   }

   /**
    * Constructs a field for a given FEM model and default value.
    * 
    * @param rowSize row size of the field's {@code MatrixNd} values
    * @param colSize column size of the field's {@code MatrixNd} values
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for nodes which don't have
    * explicitly set values
    */
   public MatrixNdNodalField (
      int rowSize, int colSize, FemModel3d fem, MatrixNd defaultValue) {
      super (MatrixNd.class);
      initSize (rowSize, colSize);
      initFemAndDefaultValue (fem, defaultValue);
      initValues();
   }

   /**
    * Constructs a named field for a given FEM model, with a default value of 0.
    * 
    * @param name name of the field
    * @param rowSize row size of the field's {@code MatrixNd} values
    * @param colSize column size of the field's {@code MatrixNd} values
    * @param fem FEM model over which the field is defined
    */
   public MatrixNdNodalField (
      String name, int rowSize, int colSize, FemModel3d fem) {
      this (rowSize, colSize, fem);
      setName (name);
   }

   /**
    * Constructs a named field for a given FEM model and default value.
    *
    * @param name name of the field
    * @param rowSize row size of the field's {@code MatrixNd} values
    * @param colSize column size of the field's {@code MatrixNd} values
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for nodes which don't have
    * explicitly set values
    */
   public MatrixNdNodalField (
      String name, int rowSize, int colSize, FemModel3d fem,
      MatrixNd defaultValue) {
      this (rowSize, colSize, fem, defaultValue);
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
