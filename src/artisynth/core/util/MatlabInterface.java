package artisynth.core.util;

import java.io.Serializable;

// for information matlabcontrol, see https://code.google.com/p/matlabcontrol
import matlabcontrol.*;
import matlabcontrol.MatlabProxy.MatlabThreadCallable;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;
import matlabcontrol.extensions.*;

import maspack.matrix.*;

public class MatlabInterface {

   private MatlabProxy myProxy;
   private MatlabTypeConverter myConverter;
   private Exception myException;

   private static int[] indexArray (double[] array) {
      int[] iarray = new int[array.length];
      for (int i=0; i<array.length; i++) {
         iarray[i] = (int)array[i]-1;
      }
      return iarray;
   }

   /**
    * Class to get information about a matlab array, either full or sparse.
    * Code based on GetArrayCallable in MatlabTypeConverter, by Joshua Kaplan,
    * distributed under a 3-clause BSD licence.
    */
   private static class GetArrayCallable
       implements MatlabThreadCallable<ArrayInfo>, Serializable {

      private static final long serialVersionUID = 1L;
      private final String myArrayName;
       
       GetArrayCallable(String arrayName) {
          myArrayName = arrayName;
       }

       private int[] getLengths(MatlabThreadProxy proxy)
          throws MatlabInvocationException {
          double[] size = (double[]) proxy.returningEval(
             "size(" + myArrayName + ");", 1)[0];
          int[] lengths = new int[size.length];
          for(int i = 0; i < size.length; i++) {
             lengths[i] = (int) size[i];
          }
          return lengths;
       }

       @Override
       public ArrayInfo call(MatlabThreadProxy proxy)
          throws MatlabInvocationException {

          //Retrieve real values
          boolean exists = 
             ((double[]) proxy.returningEval(
                "exist('"+myArrayName+"','var');", 1)[0])[0] == 1;
          if (!exists) {
             return null;
          }

          boolean isSparse =
             ((boolean[]) proxy.returningEval(
                "issparse(" + myArrayName + ");", 1)[0])[0];

          if (isSparse) {
             Object[] arrays = proxy.returningEval(
                "find(" + myArrayName + ");", 3);

             int[] lengths = getLengths(proxy);
             return new SparseArrayInfo (
                indexArray((double[])arrays[0]),
                indexArray((double[])arrays[1]),
                (double[])arrays[2], lengths);
          }
          else {
             Object realObject =
                proxy.returningEval(
                   "real(" + myArrayName + ");", 1)[0];
             double[] realValues = (double[]) realObject;
             int[] lengths = getLengths(proxy);
             return new FullArrayInfo (realValues, lengths);
          }
       }
    }
   
   private static class ArrayInfo implements Serializable {
      private static final long serialVersionUID = 1L;

      protected final int[] myLengths;

      ArrayInfo (int[] lengths) {
         myLengths = lengths;
      }

      boolean is2DArray() {
         return (myLengths.length == 2 &&
                 myLengths[0] != 0 &&
                 myLengths[1] != 0);
      }

      boolean isVector() {
         return (myLengths.length == 2 &&
                 (myLengths[0] == 1 ||
                  myLengths[1] == 1));
      }

      Matrix getMatrix() {
         return null;
      }

      VectorNd getVector() {
         return null;
      }
   }

   private static class FullArrayInfo extends ArrayInfo {
      private static final long serialVersionUID = 1L;
      // Note: values are returned from MATLAB in column-major order
      private final double[] myValues;
       
      FullArrayInfo (double[] values, int[] lengths) {
         super (lengths);
         myValues = values;
      }

      double[][] get2DArray() {
         if (!is2DArray()) {
            return null;
         }
         int nrows = myLengths[0];
         int ncols = myLengths[1];
         double[][] array = new double[nrows][ncols];
         for (int i=0; i<nrows; i++) {
            for (int j=0; j<ncols; j++) {
               array[i][j] = myValues[j*nrows+i];
            }
         }
         return array;
      }

      VectorNd getVector() {
         if (!isVector()) {
            return null;
         }
         int nrows = myLengths[0];
         int ncols = myLengths[1];
         VectorNd vec;
         if (nrows == 1) {
            vec = new VectorNd (ncols);
         }
         else {
            vec = new VectorNd (nrows);
         }
         for (int i=0; i<nrows*ncols; i++) {
            vec.set (i, myValues[i]);
         }
         return vec;
      }

      MatrixNd getMatrix() {
         if (!is2DArray()) {
            return null;
         }
         int nrows = myLengths[0];
         int ncols = myLengths[1];
         MatrixNd M = new MatrixNd (nrows, ncols);
         for (int i=0; i<nrows; i++) {
            for (int j=0; j<ncols; j++) {
               M.set (i, j, myValues[j*nrows+i]);
            }
         }
         return M;
      }
   }

    private static class SparseArrayInfo extends ArrayInfo {
       private static final long serialVersionUID = 1L;
       private final int[] myRowIdxs;
       private final int[] myColIdxs;
       private final double[] myValues;
       
       SparseArrayInfo (
          int[] rowIdxs, int[] colIdxs, double[] values, int[] lengths) {
          super (lengths);
          myRowIdxs = rowIdxs;
          myColIdxs = colIdxs;
          myValues = values;
       }

       SparseMatrixNd getMatrix() {
         if (!is2DArray()) {
            return null;
         }
         int nrows = myLengths[0];
         int ncols = myLengths[1];
         SparseMatrixNd S = new SparseMatrixNd (nrows, ncols);
         S.set (myRowIdxs, myColIdxs, myValues, myRowIdxs.length);
         return S;
       }
    }

   /**
    * Class to manage setting a sparse array in Matlab from a SparseArray
    * object. Code based on SetArrayCallable in MatlabTypeConverter, by Joshua
    * Kaplan, distributed under a 3-clause BSD licence.
    */
   private static class SetSparseArrayCallable
      implements MatlabThreadCallable<Object>, Serializable {
      
      private static final long serialVersionUID = 1L;
      private final String myArrayName;
      private final int[] myRowIdxs;
      private final int[] myColIdxs;
      private final double[] myValues;
      private final int myNumRows;
      private final int myNumCols;
        
      private SetSparseArrayCallable (String arrayName, SparseMatrix M) {
         CRSValues crs = new CRSValues(M);
         myArrayName = arrayName;
         myRowIdxs = crs.getRowIdxs();
         myColIdxs = crs.getColIdxs();
         myValues = crs.getValues();
         myNumRows = M.rowSize();
         myNumCols = M.colSize();
      }
        
      private String getVarName (MatlabThreadProxy proxy, String suffix) 
         throws MatlabInvocationException {
         return (String) proxy.returningEval(
            "genvarname('" + myArrayName + suffix + "', who);", 1)[0];
      }

      @Override
         public Object call(MatlabThreadProxy proxy)
         throws MatlabInvocationException
      {
         //Store rowIdxs, colIdxs, and values in the MATLAB environment
         String rowIdxsVar = getVarName (proxy, "myRowIdxs");
         proxy.setVariable (rowIdxsVar, myRowIdxs);
         String colIdxsVar = getVarName (proxy, "myColIdxs");
         proxy.setVariable (colIdxsVar, myColIdxs);
         String valuesVar = getVarName (proxy, "myValues");
         proxy.setVariable (valuesVar, myValues);

         //Build a statement to eval
         // - If imaginary array exists, combine the real and imaginary arrays
         // - Set the proper dimension length metadata
         // - Store as arrayName
         String evalStr =
            myArrayName + " = sparse(double(" + rowIdxsVar + "), double(" +
            colIdxsVar + "), " + valuesVar + ", "+myNumRows+", "+myNumCols+");";
         proxy.eval(evalStr);
            
         //Clear variables holding separate real and imaginary arrays
         proxy.eval("clear " + rowIdxsVar + ";");
         proxy.eval("clear " + colIdxsVar + ";");
         proxy.eval("clear " + valuesVar + ";");
            
         return null;
      }
   }

   // make constructor private so that we have to use the create method.
   private MatlabInterface (MatlabProxy proxy) {
      myProxy = proxy;
      myConverter = new MatlabTypeConverter(proxy);
   }

//   public void arrayToMatlab (String name, double[][] rvals)
//      throws MatlabInterfaceException {
//
//   }
   
   protected void arrayToMatlab (double[][] rvals, String matlabName) 
      throws MatlabInterfaceException { 
      
      if (rvals.length == 0 || rvals[0].length == 0) {
         return;
      }
      MatlabNumericArray array = new MatlabNumericArray (rvals, null);
      myException = null;
      try {
         myConverter.setNumericArray (matlabName, array);      
      }
      catch (MatlabInvocationException e) {
         myException = e;
         throw new MatlabInterfaceException (e);
      }
   }

   /**
    * Sets the MATLAB array named <code>matlabName</code> to the values
    * associated with the ArtiSynth object <code>obj</code>. The ArtiSynth
    * object must be either a {@link maspack.matrix.Matrix Matrix}, {@link
    * maspack.matrix.Vector Vector}, or <code>double[][]</code>.  The MATLAB
    * array is dense, unless the ArtiSynth object is an instance of {@link
    * maspack.matrix.SparseMatrix SparseMatrix}, in which the assigned array is
    * sparse.
    * 
    * @param obj ArtiSynth objecy
    * @param matlabName name of the MATLAB array
    * @throws MatlabInterfaceException if communication with MATLAB fails
    */
   public void objectToMatlab (Object obj, String matlabName)
      throws MatlabInterfaceException {

      if (obj instanceof Matrix) {
         Matrix M = (Matrix)obj;
         if (M.rowSize() == 0 || M.colSize() == 0) {
            return;
         }
         if (M instanceof SparseMatrix) {
            myException = null;
            try {
               myProxy.invokeAndWait(
                  new SetSparseArrayCallable(matlabName, (SparseMatrix)M));
            }
            catch (MatlabInvocationException e) {
               myException = e;
               throw new MatlabInterfaceException (e);
            }          
         }
         else {
            int nrows = M.rowSize();
            int ncols = M.colSize();
            double[][] rvals = new double[nrows][ncols];
            for (int i=0; i<nrows; i++) {
               for (int j=0; j<ncols; j++) {
                  rvals[i][j] = M.get(i, j);
               }
            }
            arrayToMatlab (rvals, matlabName);
         }
      }
      else if (obj instanceof Vector) {
         Vector v = (Vector)obj;
         int nrows = v.size();
         int ncols = 1;
         double[][] rvals = new double[nrows][ncols];
         for (int i=0; i<nrows; i++) {
            rvals[i][0] = v.get(i);
         }
         arrayToMatlab (rvals, matlabName);
      }
      else if (obj instanceof double[][]) {
         arrayToMatlab ((double[][])obj, matlabName);
      }
      else {
         throw new IllegalArgumentException (
            "obj must be either a Matrix, Vector, or double[][]");
      }
   }

   ArrayInfo getArrayInfo (String name)
      throws MatlabInvocationException {

      return (ArrayInfo)myProxy.invokeAndWait (
         new GetArrayCallable(name));
   }

   /**
    * Takes the MATLAB array named by <code>matlabName</code> and returns the
    * corresponding <code>double[][]</code> object, with values assigned in
    * row-major order. If the named MATLAB array does not exist, or is not
    * dense and 2-dimensional, then <code>null</code> is returned.
    * 
    * @param matlabName name of the MATLAB array
    * @return double array corresponding to the MATLAB array
    * @throws MatlabInterfaceException if communication with MATLAB fails
    */
   public double[][] arrayFromMatlab (String matlabName)
      throws MatlabInterfaceException {
      
      ArrayInfo info;
      try {
         myException = null;
         info = getArrayInfo (matlabName);
         if (info instanceof FullArrayInfo) {
            FullArrayInfo ainfo = (FullArrayInfo)info;
            if (!ainfo.is2DArray()) {
               return null;
            }
            else {
               return ainfo.get2DArray();
            }
         }
         else {
            return null;
         }
      }
      catch (MatlabInvocationException e) {
         myException = e;
         throw new MatlabInterfaceException (e);
      }      
   }

   /**
    * Takes the MATLAB array named by <code>matlabName</code> and returns a
    * corresponding {@link maspack.matrix.Matrix Matrix} object.  If the named
    * MATLAB array does not exist, or is not 2-dimensional, then
    * <code>null</code> is returned. Otherwise, either a {@link
    * maspack.matrix.MatrixNd MatrixNd} or {@link maspack.matrix.SparseMatrixNd
    * SparseMatrixNd} is returned, depending on whether the array is dense or
    * sparse.
    *
    * @param matlabName name of the MATLAB array
    * @return matrix corresponding to the MATLAB array
    * @throws MatlabInterfaceException if communication with MATLAB fails
    */
   public Matrix matrixFromMatlab (String matlabName)
      throws MatlabInterfaceException {

      ArrayInfo info;
      try {
         myException = null;
         info = getArrayInfo (matlabName);
         if (info == null || !info.is2DArray()) {
            return null;
         }
         return info.getMatrix();
      }
      catch (MatlabInvocationException e) {
         myException = e;
         throw new MatlabInterfaceException (e);
      }      
   }

   /**
    * Takes the MATLAB array named by <code>matlabName</code> and returns a
    * corresponding {@link maspack.matrix.VectorNd VectorNd} object.  If the
    * named MATLAB array does not exist, or is not 2-dimensional with a size of
    * 1 in at least one dimension, then <code>null</code> Is returned.
    * 
    * @param matlabName name of the MATLAB array
    * @return vector corresponding to the MATLAB array
    * @throws MatlabInterfaceException if communication with MATLAB fails
    */
   public VectorNd vectorFromMatlab (String matlabName) 
      throws MatlabInterfaceException {

      ArrayInfo info;
      try {
         myException = null;
         info = getArrayInfo (matlabName);
         if (info == null) {
            return null;
         }
         return info.getVector();
      }
      catch (MatlabInvocationException e) {
         myException = e;
         throw new MatlabInterfaceException (e);
      }      
   }

   public void dispose() {
      if (myProxy != null) {
         myProxy.disconnect();
         myProxy = null;
      }
   }      

   public boolean errorOccurred() {
      return myException != null;
   }

   public Exception getError() {
      return myException;
   }

   public boolean isConnected() {
      return myProxy.isConnected();                
   }

   public static MatlabInterface create () throws MatlabInterfaceException {

      MatlabProxyFactoryOptions.Builder builder =
         new MatlabProxyFactoryOptions.Builder();
      builder.setUsePreviouslyControlledSession (true);
      MatlabProxyFactoryOptions opts = builder.build();

      MatlabProxyFactory factory = new MatlabProxyFactory(opts);
      MatlabProxy proxy = null;
      try {
         proxy = factory.getProxy();
      }
      catch (MatlabConnectionException e) {
         throw new MatlabInterfaceException (e);
      }
      if (proxy != null) {
         return new MatlabInterface (proxy);
      }
      else {
         return null;
      }
   }
}
