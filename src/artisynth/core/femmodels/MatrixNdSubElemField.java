package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.util.*;
import artisynth.core.modelbase.FieldUtils.VectorFieldFunction;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.PropertyDesc;

public class MatrixNdSubElemField extends VectorSubElemField<MatrixNd> {

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

   @Override
   protected MatrixNd createInstance () {
      return new MatrixNd (myRowSize, myColSize);
   }
   
   public boolean hasParameterizedType() {
      return false;
   }   

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public MatrixNdSubElemField () {
      super (MatrixNd.class);
   }

   public MatrixNdSubElemField (int rowSize, int colSize, FemModel3d fem)  {
      super (MatrixNd.class);
      initSize (rowSize, colSize);
      initFemAndDefaultValue (fem, null);
      initValues();
   }

   public MatrixNdSubElemField (
      int rowSize, int colSize, FemModel3d fem, MatrixNd defaultValue) {
      super (MatrixNd.class);
      initSize (rowSize, colSize);
      initFemAndDefaultValue (fem, defaultValue);
      initValues();
   }

   public MatrixNdSubElemField (
      String name, int rowSize, int colSize, FemModel3d fem) {
      this (rowSize, colSize, fem);
      setName (name);
   }

   public MatrixNdSubElemField (
      String name, int rowSize, int colSize, FemModel3d fem,
      MatrixNd defaultValue) {
      this (rowSize, colSize, fem, defaultValue);
      setName (name);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("size=[ "+myRowSize+" "+myColSize+" ]");
      super.writeItems (pw, fmt, ancestor);
   }

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
