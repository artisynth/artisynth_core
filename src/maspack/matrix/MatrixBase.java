/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;

/**
 * Base implementation of {@link maspack.matrix.Matrix Matrix}.
 */
public abstract class MatrixBase implements LinearTransformNd, Matrix {
   protected static NumberFormat myDefaultFmt = new NumberFormat ("%g");

   // our own abs routine saves code space and might be faster than Math.abs
   final double abs (double x) {
      return (x < 0 ? -x : x);
   }

   /**
    * Sets the default format string used in {@link #toString() toString}. For
    * a description of the format string syntax, see {@link
    * maspack.util.NumberFormat NumberFormat}.
    * 
    * @param fmtStr
    * new format string
    * @throws IllegalArgumentException
    * if the format string is invalid
    * @see #getDefaultFormat
    */
   public static void setDefaultFormat (String fmtStr) {
      NumberFormat fmt = new NumberFormat (fmtStr);
      myDefaultFmt = fmt;
   }

   /**
    * Returns the default format string used in {@link #toString() toString}. If
    * unset, this string is "%g". For a description of the format string syntax,
    * see {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @return Default format string
    */
   public static String getDefaultFormat() {
      return myDefaultFmt.toString();
   }

   /**
    * {@inheritDoc}
    */
   public abstract int rowSize();

   /**
    * {@inheritDoc}
    */
   public abstract int colSize();

   /**
    * {@inheritDoc}
    */
   public abstract double get (int i, int j);

   /**
    * {@inheritDoc}
    */
   public void get (double[] values) {
      int ncols = colSize();
      for (int i = 0; i < rowSize(); i++) {
         for (int j = 0; j < ncols; j++) {
            values[i * ncols + j] = get (i, j);
         }
      }
   }

   /**
    * Copies the elements of this matrix into a 2-dimensional array of doubles.
    * 
    * @param values
    * array into which values are copied
    * @throws IllegalArgumentException
    * <code>values</code> has inconsistent row sizes
    * @throws ImproperSizeException
    * dimensions of <code>values</code> do not match the size of this matrix
    */
   public void get (double[][] values) {
      int nrows = values.length;
      int ncols = 0;
      if (nrows > 0) {
         ncols = values[0].length;
         for (int i = 1; i < nrows; i++) {
            if (values[i].length != ncols) {
               throw new IllegalArgumentException (
                  "inconsistent rows sizes in input");
            }
         }
      }
      if (nrows != rowSize() || ncols != colSize()) {
         throw new ImproperSizeException();
      }
      for (int i = 0; i < nrows; i++) {
         getRow (i, values[i]);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getColumn (int j, double[] values) {
      for (int i = 0; i < rowSize(); i++) {
         values[i] = get (i, j);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getColumn (int j, double[] values, int off) {
      for (int i = 0; i < rowSize(); i++) {
         values[i + off] = get (i, j);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getRow (int i, double[] values) {
      for (int j = 0; j < colSize(); j++) {
         values[j] = get (i, j);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getRow (int i, double[] values, int off) {
      for (int j = 0; j < colSize(); j++) {
         values[j + off] = get (i, j);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getColumn (int j, Vector v) {
      if (v.size() != rowSize()) {
         if (v.isFixedSize()) {
            throw new ImproperSizeException();
         }
         else {
            v.setSize (rowSize());
         }
      }
      for (int i = 0; i < rowSize(); i++) {
         v.set (i, get (i, j));
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getRow (int i, Vector v) {
      if (v.size() != colSize()) {
         if (v.isFixedSize()) {
            throw new ImproperSizeException();
         }
         else {
            v.setSize (colSize());
         }
      }
      for (int j = 0; j < colSize(); j++) {
         v.set (j, get (i, j));
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isFixedSize() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void setSize (int numRows, int numCols) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   public double determinant() throws ImproperSizeException {
      if (rowSize() != colSize()) {
         throw new ImproperSizeException ("matrix must be square");
      }
      LUDecomposition lu = new LUDecomposition (this);
      double det = 0;
      try {
         det = lu.determinant();
      }
      catch (ImproperStateException e) { // can't happen
      }
      return det;
   }
   
   /**
    * {@inheritDoc}
    */
   public double trace() throws ImproperSizeException {
      if (rowSize() != colSize()) {
         throw new ImproperSizeException("matrix must be square");
      }
      
      double t = 0;
      for (int i=0; i<rowSize(); i++) {
         t += get(i,i);
      }
      return t;
      
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix M1, double epsilon) {
      if (M1.rowSize() != rowSize() || M1.colSize() != colSize()) {
         return false;
      }
      for (int i = 0; i < rowSize(); i++) {
         for (int j = 0; j < colSize(); j++) {
            if (!(Math.abs (get (i, j) - M1.get (i, j)) <= epsilon)) {
               return false;
            }
         }
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals (Matrix M1) {
      if (M1.rowSize() != rowSize() || M1.colSize() != colSize()) {
         return false;
      }
      for (int i = 0; i < rowSize(); i++) {
         for (int j = 0; j < colSize(); j++) {
            if (get (i, j) != M1.get (i, j)) {
               return false;
            }
         }
      }
      return true;
   }

   // Dec 9, 2008. John Lloyd: removed hashCode/equals override, since it was
   // causing confusion. For now equals (Object obj) should return true only if
   // the objects are identical. If equals based on contents are required, then
   // one should create a subclass.
   // /**
   // * Returns true if this matrix and a specified object
   // * have the same class type and if all the elements are
   // * exactly equal.
   // *
   // * @param obj object to compare with
   // * @return false if the objects are not equal
   // */
   // public boolean equals (Object obj)
   // {
   // if (!getClass().isInstance(obj) ||
   // !obj.getClass().isInstance(this))
   // { return false;
   // }
   // Matrix M = (Matrix)obj;
   // if (M.rowSize() != rowSize() ||
   // M.colSize() != colSize())
   // { return false;
   // }
   // boolean isEqual = false;
   // try
   // { isEqual = equals(M);
   // }
   // catch (Exception e)
   // { // can't happen
   // }
   // return isEqual;
   // }

   // /**
   // * Computes hash code based on all elements of matrix.
   // *
   // * @return hash code based on all elements of matrix
   // */
   // public int hashCode()
   // {
   // final int PRIME = 31;
   // int result = 1;
   // long temp;
   // for (int row = 0; row < rowSize(); row++)
   // {
   // for (int col = 0; col < colSize(); col++)
   // {
   // temp = Double.doubleToLongBits(get(row, col));
   // result = PRIME * result + (int) (temp ^ (temp >>> 32));
   // }
   // }

   // return result;
   // }

   static double computeOneNorm (Matrix M) {
      double max = 0;
      for (int j = 0; j < M.colSize(); j++) {
         double sum = 0;
         for (int i = 0; i < M.rowSize(); i++) {
            sum += Math.abs (M.get (i, j));
         }
         if (sum > max) {
            max = sum;
         }
      }
      return max;
   }

   /**
    * {@inheritDoc}
    */
   public double oneNorm() {
      // largest vector one-norm over all columns
      return computeOneNorm (this);
   }

   static double computeInfinityNorm (Matrix M) {
      double max = 0;
      for (int i = 0; i < M.rowSize(); i++) {
         double sum = 0;
         for (int j = 0; j < M.colSize(); j++) {
            sum += Math.abs (M.get (i, j));
         }
         if (sum > max) {
            max = sum;
         }
      }
      return max;
   }

   /**
    * {@inheritDoc}
    */
   public double infinityNorm() {
      // largest vector one-norm over all rows
      return computeInfinityNorm (this);
   }

   static double computeFrobeniusNormSquared (Matrix M) {
      double trace = 0;
      for (int j = 0; j < M.colSize(); j++) {
         double diag_jj = 0;
         for (int i = 0; i < M.rowSize(); i++) {
            double M_ij = M.get (i, j);
            diag_jj += M_ij * M_ij;
         }
         trace += diag_jj;
      }
      return trace;
   }
   
   static double computeFrobeniusNorm (Matrix M) {
      return Math.sqrt(computeFrobeniusNormSquared (M));
   }

   /**
    * {@inheritDoc}
    */
   public double frobeniusNorm() {
      // returns sqrt(sum (diag (M'*M))
      return Math.sqrt(frobeniusNormSquared());
   }

   public double frobeniusNormSquared() {
      // returns sum (diag (M'*M)
      return computeFrobeniusNormSquared (this);
   }
   
   static double computeMaxNorm(Matrix M) {
      double max = 0;
      for (int i=0; i<M.rowSize(); ++i) {
         for (int j=0; j<M.colSize(); ++j) {
            double e = Math.abs(M.get(i, j));
            if (e > max) {
               max = e;
            }
         }
      }
      return max;
   }
   
   @Override
   public double maxNorm() {
      return computeMaxNorm(this);
   }
   
   /**
    * Sets the elements of this matrix to uniformly distributed random values in
    * the range -0.5 (inclusive) to 0.5 (exclusive).
    */
   protected void setRandom() {
      setRandom (-0.5, 0.5, RandomGenerator.get());
   }

   /**
    * Sets the elements of this matrix to uniformly distributed random values in
    * a specified range.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    */
   protected void setRandom (double lower, double upper) {
      setRandom (lower, upper, RandomGenerator.get());
   }

   /**
    * Sets the elements of this matrix to uniformly distributed random values in
    * a specified range, using a supplied random number generator.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    * @param generator
    * random number generator
    */
   protected abstract void setRandom (
      double lower, double upper, Random generator);

   /**
    * Returns a String representation of this matrix, using the default format
    * returned by {@link #getDefaultFormat getDefaultFormat}.
    * 
    * @return String representation of this matrix
    * @see #toString(String)
    */
   public String toString() {
      return toString (new NumberFormat(myDefaultFmt));
   }

   public String idString() {
      return super.toString();
   }

   /**
    * Returns a String representation of this matrix, in which each element is
    * formatted using a C <code>printf</code> style format string. For a
    * description of the format string syntax, see {@link
    * maspack.util.NumberFormat NumberFormat}. Note that when called numerous
    * times, {@link #toString(NumberFormat) toString(NumberFormat)} will be more
    * efficient because the {@link maspack.util.NumberFormat NumberFormat} will
    * not need to be recreated each time from a specification string.
    * 
    * @param fmtStr
    * numeric format specification
    * @return String representation of this vector
    */
   public String toString (String fmtStr) {
      return toString (new NumberFormat (fmtStr));
   }

   /**
    * {@inheritDoc}
    */
   public String toString (NumberFormat fmt) {
      StringBuffer buf = new StringBuffer (20 * rowSize() * colSize());
      for (int i = 0; i < rowSize(); i++) {
         for (int j = 0; j < colSize(); j++) {
            buf.append (fmt.format (get (i, j)));
            buf.append (' ');
         }
         buf.append ('\n');
      }
      return buf.toString();
   }

   public void writeToFile (String fileName, String fmtStr) {
      NumberFormat fmt = new NumberFormat (fmtStr);
      try {
         PrintWriter pw =
            new PrintWriter (new BufferedWriter (new FileWriter (fileName)));
         write (pw, fmt);
         pw.close();
      }
      catch (Exception e) {
         System.out.println ("Error writing matrix to file "+ fileName + ":");
         System.out.println (e);
      }
   }

   /**
    * Writes this matrix to a specified file, and returns the PrintWriter that
    * was created to do the writing.
    * 
    * @param fileName
    * Path name for the file to written
    * @param msg
    * Optional message - if not <code>null</code>, is printed on
    * a separate line preceeding the matrix information.
    * @param wfmt
    * specifies the matrix output format
    * @return PrintWriter used to do the writing
    */
   public PrintWriter write (String fileName, String msg, WriteFormat wfmt)
      throws IOException {
      return write (fileName, msg, wfmt, rowSize(), colSize());
   }

   /**
    * Writes a principal submatrix of this matrix to a specified file, and
    * returns the PrintWriter that was created to do the writing.
    * 
    * @param fileName
    * Path name for the file to written
    * @param msg
    * Optional message - if not <code>null</code>, is printed on
    * a separate line preceeding the matrix information.
    * @param wfmt
    * specifies the matrix output format
    * @param nrows number of rows in the principle submatrix
    * @param ncols number of columns in the principle submatrix
    * @return PrintWriter used to do the writing
    */
   public PrintWriter write (
      String fileName, String msg, WriteFormat wfmt, int nrows, int ncols)
      throws IOException {
      NumberFormat fmt = new NumberFormat("%g");
      PrintWriter pw =
         new PrintWriter (new BufferedWriter (new FileWriter (fileName)));
      if (msg != null) {
         pw.println (msg);
      }
      write (pw, fmt, wfmt, nrows, ncols);
      return pw;
   }

   /**
    * Writes the contents of this matrix to a PrintWriter.
    * 
    * @param pw 
    * PrintWriter to write the matrix to
    * @param msg
    * Optional message - if not <code>null</code>, is printed on
    * a separate line preceeding the matrix information.
    * @param wfmt
    * specifies the matrix output format
    */
   public void write (PrintWriter pw, String msg, WriteFormat wfmt)
      throws IOException {
      NumberFormat fmt = new NumberFormat("%g");
      if (msg != null) {
         pw.println (msg);
      }
      write (pw, fmt, wfmt, rowSize(), colSize());
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt) throws IOException {
      write (pw, fmt, WriteFormat.Dense);
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, WriteFormat wfmt)
      throws IOException {
      write (pw, fmt, wfmt, rowSize(), colSize());
   }

   /**
    * {@inheritDoc}
    */
   public void write (
      PrintWriter pw, NumberFormat fmt, WriteFormat wfmt, int numRows,
      int numCols) throws IOException {
      if (numRows > rowSize() || numCols > colSize()) {
         throw new IllegalArgumentException (
            "specified block matrix is out of bounds");
      }
      if (wfmt == WriteFormat.Dense) {
         for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
               pw.print (fmt.format (get (i, j)));
               if (j < numCols - 1) {
                  pw.print (" ");
               }
               else {
                  pw.println ("");
               }
            }
         }
         pw.flush();
         return;
      }
      Partition part = Partition.Full;
      if (wfmt == WriteFormat.SYMMETRIC_CRS) {
         part = Partition.UpperTriangular;
      }
      int nvals = numNonZeroVals (part, numRows, numCols);
      int[] rowOffs = new int[numRows+1];
      int[] colIdxs = new int[nvals];
      double[] vals = new double[nvals];
      
      getCRSIndices (colIdxs, rowOffs, part, numRows, numCols);
      getCRSValues (vals, part, numRows, numCols);
      if (wfmt == WriteFormat.MatrixMarket) {
         pw.println ("%%MatrixMarket matrix coordinate real general");
         pw.println (numRows + " " + numCols + " " + nvals);
         int k = 0;
         for (int i = 0; i < numRows; i++) {
            int endK = (i < numRows - 1 ? rowOffs[i+1]-1 : nvals);
            while (k < endK) {
               pw.println ((i+1)+" "+(colIdxs[k])+" "+fmt.format (vals[k]));
               k++;
            }
         }
      }
      else if (wfmt == WriteFormat.CRS || wfmt == WriteFormat.SYMMETRIC_CRS) {
         pw.println (numRows);
         for (int i = 0; i < numRows; i++) {
            pw.print ((rowOffs[i])+" ");
         }
         pw.println (nvals+1);
         for (int i = 0; i < nvals; i++) {
            pw.print ((colIdxs[i])+" ");
         }
         pw.println ("");
         for (int i = 0; i < nvals; i++) {
            pw.print (fmt.format (vals[i]) + " ");
         }
         pw.println ("");
      }
      else // (wfmt == WriteFormat.Sparse)
      {
         int k = 0;
         for (int i = 0; i < numRows; i++) {
            int endK = (i < numRows - 1 ? rowOffs[i+1]-1 : nvals);
            while (k < endK) {
               pw.println ("("+i+" "+(colIdxs[k]-1)+
                           " "+fmt.format (vals[k]) + ")");
               k++;
            }
         }
      }
      pw.flush();
   }

   private boolean scanMatrixMarket (ReaderTokenizer rtok) throws IOException {

      if (rtok.nextToken() != '%') {
         rtok.pushBack();
         return false;
      }  
      if (rtok.nextToken() != '%' ||
          rtok.nextToken() != ReaderTokenizer.TT_WORD ||
          !rtok.sval.equals ("MatrixMarket")) {
         throw new IOException (
            "MatrixMarket file: expecting '%%MatrixMarket'");
      }
      boolean savedEOLsignificant = rtok.getEolIsSignificant();
      rtok.eolIsSignificant (true);
      boolean symmetric = false;
      if (!rtok.scanWord().equals ("matrix")) {
         throw new IOException (
            "Expecting 'matrix' keyword after %%MatrixMarket");
      }
      if (!rtok.scanWord().equals ("coordinate")) {
         throw new IOException (
            "MatrixMarket file: only 'coordinate' formats are supported");
      }
      if (!rtok.scanWord().equals ("real")) {
         throw new IOException (
            "MatrixMarket file: Only 'real' numbers are supported");
      }
      String word = rtok.scanWord();
      if (word.equals ("symmetric")) {
         symmetric = true;
      }
      else if (!word.equals ("general")) {
         throw new IOException (
            "MatrixMarket file: only 'general' and 'symmetric' supported");
      }
      rtok.scanToken (ReaderTokenizer.TT_EOL);
      while (rtok.nextToken() == '%') {
         while (rtok.nextToken() != ReaderTokenizer.TT_EOL) {
         }
      }
      rtok.pushBack();
      int nrows = rtok.scanInteger();
      int ncols = rtok.scanInteger();
      int nvals = rtok.scanInteger();
      rtok.eolIsSignificant (false);

      System.out.println ("nrows="+nrows+" ncols="+ncols);

      if (isFixedSize()) {
         if (nrows != rowSize() || ncols != colSize()) {
            throw new IOException (
               "MatrixMarket file: matrix cannot be sized to "+nrows+"x"+ncols);
         }
      }
      else {
         setSize (nrows, ncols);
      }
      int[] indices;
      double[] values;
      if (symmetric) {
         // allow extra room for transposed entries
         indices = new int[4*nvals];
         values = new double[2*nvals];
      }
      else {
         indices = new int[2*nvals];
         values = new double[nvals];
      }

      int cnt = 0;
      for (int k=0; k<nvals; k++) {
         int i = rtok.scanInteger()-1;
         if (i < 0 || i >= nrows) {
            throw new IOException (
"MatrixMarket file: row index "+i+" out of range, line "+rtok.lineno());
         }
         int j = rtok.scanInteger()-1;
         if (j < 0 || j >= ncols) {
            throw new IOException (
"MatrixMarket file: column index "+j+" out of range, line "+rtok.lineno());
         }
         double value = rtok.scanNumber(); 
         indices[2*cnt  ] = i;
         indices[2*cnt+1] = j;
         values[cnt++] = value;
         if (symmetric && i != j) {
            indices[2*cnt  ] = j;
            indices[2*cnt+1] = i;
            values[cnt++] = value;
         }
      }
      set (values, indices, cnt);
      rtok.eolIsSignificant (savedEOLsignificant);
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok) throws IOException {
      boolean parseNumbersSave = rtok.getParseNumbers();
      String ochars = ";()[]";
      int[] typesSave = rtok.getCharSettings (ochars);
      rtok.ordinaryChars (ochars);

      if (rtok.nextToken() == '[') {
         if (rtok.nextToken() == '(') {
            rtok.pushBack();
            scanSparseInput (rtok);
         }
         else {
            rtok.pushBack();
            scanDenseInput (rtok);
         }
      }
      else if (rtok.ttype == '%') {
         rtok.pushBack();
         scanMatrixMarket (rtok); 
      }
      else if (rtok.ttype == ReaderTokenizer.TT_NUMBER) {
         rtok.pushBack();
         int nvals = rowSize()*colSize();
         int[] indices = new int[2*nvals];
         double[] values = new double[nvals];
         int k = 0;
         for (int i = 0; i < rowSize(); i++) {
            for (int j = 0; j < colSize(); j++) {
               values[k] = rtok.scanNumber();
               indices[k*2  ] = i;
               indices[k*2+1] = j;
               k++;
            }
         }
         set (values, indices, nvals);
      }
      else {
         throw new IOException ("Unrecognized token at file start: "+rtok);
      }
      
      rtok.parseNumbers (parseNumbersSave);
      rtok.setCharSettings (ochars, typesSave);
   }

   private void scanDenseInput (ReaderTokenizer rtok) throws IOException {
      LinkedList<Double> valueList = new LinkedList<Double>();
      int numRows = 0;
      int currentLine = -1;
      int numCols = -1;
      int colCnt = 0;

      while (true) {
         rtok.nextToken();
         if (currentLine != -1) {
            if (rtok.ttype == ';' || rtok.ttype == ']' ||
                rtok.lineno() != currentLine) { // finish current row
               if (numCols == -1) {
                  numCols = colCnt;
               }
               else if (colCnt != numCols) {
                  throw new ImproperSizeException (
                     "Inconsistent row size, line " + currentLine);
               }
               numRows++;
               colCnt = 0;
               currentLine = -1;
            }
         }
         if (rtok.ttype == ']') {
            break;
         }
         else if (rtok.ttype != ';') {
            rtok.pushBack();
            valueList.add (new Double (rtok.scanNumber()));
            if (currentLine == -1) {
               currentLine = rtok.lineno();
            }
            colCnt++;
         }
      }
      if (numRows != rowSize() || numCols != colSize()) {
         if (isFixedSize()) {
            throw new ImproperSizeException (
               "Matrix size incompatible with input, line " + rtok.lineno());
         }
         else {
            setSize (numRows, numCols);
         }
      }
      Iterator<Double> it = valueList.iterator();
      int nvals = rowSize()*colSize();
      int[] indices = new int[2*nvals];
      double[] values = new double[nvals];
      int k = 0;
      for (int i = 0; i < rowSize(); i++) {
         for (int j = 0; j < colSize(); j++) {
            values[k] = it.next().doubleValue();
            indices[k*2  ] = i;
            indices[k*2+1] = j;
            k++;
         }
      }
      set (values, indices, nvals);
   }

   private void scanSparseInput (ReaderTokenizer rtok) throws IOException {
      LinkedList<SparseMatrixCell> valueList = new LinkedList<SparseMatrixCell>();
      int numRows = rowSize();
      int numCols = colSize();

      while (rtok.nextToken() != ']') {
         SparseMatrixCell cell = new SparseMatrixCell();
         if (rtok.ttype != '(') {
            throw new IOException (
               "Token '(' expected for sparse matrix input, line "
               + rtok.lineno());
         }
         if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER ||
             !rtok.tokenIsInteger() || rtok.nval < 0) {
            throw new IOException (
               "Expected non-negative integer for row index, got " + rtok);
         }
         cell.i = (int)rtok.nval;
         if (cell.i >= numRows) {
            if (isFixedSize()) {
               throw new ImproperSizeException (
                  "Matrix size incompatible with row index " + cell.i
                  + ", line " + rtok.lineno());
            }
            numRows = cell.i + 1;
         }
         // rtok.scanToken (',');
         if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER ||
             !rtok.tokenIsInteger() || rtok.nval < 0) {
            throw new IOException (
               "Expected non-negative integer for column index, got " + rtok);
         }
         cell.j = (int)rtok.nval;
         if (cell.j >= numCols) {
            if (isFixedSize()) {
               throw new ImproperSizeException (
                  "Matrix size incompatible with column index " + cell.j
                  + ", line " + rtok.lineno());
            }
            numCols = cell.j + 1;
         }
         // rtok.scanToken (',');
         if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
            throw new IOException ("Numeric element value expected, line "
            + rtok.lineno());
         }
         cell.value = rtok.nval;
         if (rtok.nextToken() != ')') {
            throw new IOException (
               "Token ')' expected for sparse matrix input, line "
               + rtok.lineno());
         }
         valueList.add (cell);
      }

      if (numRows >= rowSize() || numCols >= colSize()) {
         setSize (numRows, numCols);
      }
      int nvals = valueList.size();
      int[] indices = new int[2*nvals];
      double[] values = new double[nvals];
      int k = 0;
      for (SparseMatrixCell cell : valueList) {
         values[k] = cell.value;
         indices[k*2  ] = cell.i;
         indices[k*2+1] = cell.j;
         k++;
      }
      set (values, indices, nvals);
   }

   public boolean isWritable() {
      return true;
   }

   public void scan (ReaderTokenizer rtok, Object obj)
      throws IOException {
      scan (rtok);
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object obj)
      throws IOException {
      write (pw, fmt);
   }

   /**
    * Returns true if any element of this matrix is not a number.
    * 
    * @return true if any element is NaN
    */
   public boolean containsNaN() {
      for (int i = 0; i < rowSize(); i++) {
         for (int j = 0; j < colSize(); j++) {
            if (get (i, j) != get (i, j)) {
               return true;
            }
         }
      }
      return false;
   }

   /** 
    * Multiplies a submatrix of this matrix by the data in <code>vec</code> and
    * places the result in <code>res</code>. The submatrix is specified by the
    * <code>nr</code> rows and <code>nc</code> columns of this matrix,
    * beginning at <code>r0</code> and <code>c0</code>, respectively.
    *
    * <p>
    * It is assumed that <code>res</code> and <code>vec</code>
    * are different, and that all submatrix dimensions are compatible
    * with the dimensions of this matrix.
    */
   protected void mulVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      //System.out.printf ("r0=%d nr=%d c0=%d nc=%d\n", r0, nr, c0, nc);

      int rowf = r0+nr;
      int colf = c0+nc;
      for (int i=r0; i<rowf; i++) {
         double sum = 0;
         for (int j=c0; j<colf; j++) {
            sum += get(i,j)*vec[j-c0];
         }
         res[i-r0] = sum;
      }
   }

   /** 
    * Multiplies a submatrix of this matrix by the data in <code>vec</code> and
    * adds the result to <code>res</code>. The submatrix is specified by the
    * <code>nr</code> rows and <code>nc</code> columns of tis matrix, beginning at
    * <code>r0</code> and <code>c0</code>, respectively.
    *
    * <p>
    * It is assumed that <code>res</code> and <code>vec</code>
    * are different, and that all submatrix dimensions are compatible
    * with the dimensions of this matrix.
    */
   protected void mulAddVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      int rowf = r0+nr;
      int colf = c0+nc;
      for (int i=r0; i<rowf; i++) {
         double sum = 0;
         for (int j=c0; j<colf; j++) {
            sum += get(i,j)*vec[j-c0];
         }
         res[i-r0] += sum;
      }
   }

   /** 
    * Multiplies a submatrix of the transpose of this matrix by the data in
    * <code>vec</code> and places the result in <code>res</code>. The submatrix
    * is specified by the <code>nr</code> rows and <code>nc</code> columns of
    * the transpose of this matrix, beginning at <code>r0</code> and
    * <code>c0</code>, respectively.
    *
    * <p>
    * It is assumed that <code>res</code> and <code>vec</code>
    * are different, and that all submatrix dimensions are compatible
    * with the dimensions of this matrix.
    */
   protected void mulTransposeVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      // rowf and colf are with respect to the *transposed* matrix
      int rowf = r0+nr;
      int colf = c0+nc;
      for (int j=r0; j<rowf; j++) {
         double sum = 0;
         for (int i=c0; i<colf; i++) {
            sum += get(i,j)*vec[i-c0];
         }
         res[j-r0] = sum;
      }
   }

   /** 
    * Multiplies a submatrix of the transpose of this matrix by the data in
    * <code>vec</code> and adds the result to <code>res</code>. The submatrix
    * is specified by the <code>nr</code> rows and <code>nc</code> columns of
    * the transpose of this matrix, beginning at <code>r0</code> and
    * <code>c0</code>, respectively.
    *
    * <p>
    * It is assumed that <code>res</code> and <code>vec</code>
    * are different, and that all submatrix dimensions are compatible
    * with the dimensions of this matrix.
    */
   protected void mulTransposeAddVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      // rowf and colf are with respect to the *transposed* matrix
      int rowf = r0+nr;
      int colf = c0+nc;
      for (int j=r0; j<rowf; j++) {
         double sum = 0;
         for (int i=c0; i<colf; i++) {
            sum += get(i,j)*vec[i-c0];
         }
         res[j-r0] += sum;
      }
   }

   protected String getSubMatrixStr (int r0, int nr, int c0, int nc) {
      return "("+r0+":"+(r0+nr-1)+","+c0+":"+(c0+nc-1)+")";
   }

   protected void mulCheckArgs (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {
      
      if (r0 < 0 || c0 < 0) {
         throw new ImproperSizeException ("r0 and c0 must not be negative");
      }
      if (nr < 0 || nc < 0) {
         throw new ImproperSizeException ("nr and nc must not be negative");
      }
      if (v1.size < nc) {
         throw new ImproperSizeException (
            "v1 size "+v1.size()+" < column size "+nc);
      }
      if (vr != null && vr.size < nr) {
         throw new ImproperSizeException (
            "vr size "+vr.size()+" < row size "+nr);
      }
      if (r0+nr > rowSize() || c0+nc > colSize()) {
         throw new ImproperSizeException (
            "Specified submatrix "+getSubMatrixStr(r0,nr,c0,nc)+
            " incompatible with "+getSize()+" matrix");
      }
   }      

   /**
    * {@inheritDoc}
    */
   public void mul (VectorNd vr, VectorNd v1) {
      if (v1.size() < colSize()) {
         throw new ImproperSizeException (
            "v1 size "+v1.size()+" < column size "+colSize());
      }
      mul (vr, v1, 0, rowSize(), 0, colSize());
   }

   /**
    * {@inheritDoc}
    */
   public void mul (VectorNd vr, VectorNd v1, int nr, int nc) {
      mul (vr, v1, 0, nr, 0, nc);
   }

   /**
    * {@inheritDoc}
    */
   public void mul (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {

      mulCheckArgs (null, v1, r0, nr, c0, nc);
      if (vr.size < nr && vr != v1) {
         vr.resetSize (nr);
      }
      double[] res = ((vr == v1) ? new double[nr] : vr.buf);
      mulVec (res, v1.buf, r0, nr, c0, nc);
      if (vr.size < nr) {
         vr.resetSize (nr); // reset size if needed when vr == v1
      }
      if (v1 == vr) {
         double[] buf = vr.buf;
         for (int i=0; i<nr; i++) {
            buf[i] = res[i];
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void mulAdd (VectorNd vr, VectorNd v1) {
      if (v1.size() < colSize()) {
         throw new ImproperSizeException (
            "v1 size "+v1.size()+" < column size "+colSize());
      }
      if (vr.size() < rowSize()) {
         throw new ImproperSizeException (
            "vr size "+vr.size()+" < row size "+rowSize());
      }
      mulAdd (vr, v1, 0, rowSize(), 0, colSize());
   }

   /**
    * {@inheritDoc}
    */
   public void mulAdd (VectorNd vr, VectorNd v1, int nr, int nc) {
      mulAdd (vr, v1, 0, nr, 0, nc);
   }

   /**
    * {@inheritDoc}
    */
   public void mulAdd (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {
      mulCheckArgs (vr, v1, r0, nr, c0, nc);
      // if (vr.size != nr) {
      //    vr.resetSize (nr); // reset size if needed when vr == v1
      // }
      if (v1 == vr) {
         double[] res = new double[nr];
         mulVec (res, v1.buf, r0, nr, c0, nc);
         double[] buf = vr.buf;
         for (int i=0; i<nr; i++) {
            buf[i] += res[i];
         }
      }
      else {
         mulAddVec (vr.buf, v1.buf, r0, nr, c0, nc);
      }
   }

   protected void mulTransposeCheckArgs (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {

      if (r0 < 0 || c0 < 0) {
         throw new ImproperSizeException ("r0 and c0 must not be negative");
      }
      if (nr < 0 || nc < 0) {
         throw new ImproperSizeException ("nr and nc must not be negative");
      }
      if (v1.size < nc) {
         throw new ImproperSizeException (
            "v1 size "+v1.size()+" < column size "+nc);
      }
      if (vr != null && vr.size < nr) {
         throw new ImproperSizeException (
            "vr size "+vr.size()+" < row size "+nr);
      }
      if (r0+nr > colSize() || c0+nc > rowSize()) {
         throw new ImproperSizeException (
            "Specified submatrix "+getSubMatrixStr(r0,nr,c0,nc)+
            " incompatible with transpose of "+getSize()+" matrix");
      }
   }      

   /**
    * {@inheritDoc}
    */
   public void mulTranspose (VectorNd vr, VectorNd v1) {
      if (v1.size < rowSize()) {
         throw new ImproperSizeException (
            "v1 size "+v1.size()+" < column size "+rowSize());
      }
      mulTranspose (vr, v1, 0, colSize(), 0, rowSize());
   }
      
   /**
    * {@inheritDoc}
    */
   public void mulTranspose (VectorNd vr, VectorNd v1, int nr, int nc) {
      mulTranspose (vr, v1, 0, nr, 0, nc);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTranspose (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {
      
      mulTransposeCheckArgs (null, v1, r0, nr, c0, nc);
      if (vr.size < nr && vr != v1) {
         vr.resetSize (nr);
      }
      double[] res = ((vr == v1) ? new double[nr] : vr.buf);
      mulTransposeVec (res, v1.buf, r0, nr, c0, nc);
      if (vr.size < nr) {
         vr.resetSize (nr); // reset size if needed when vr == v1
      }
      if (v1 == vr) {
         double[] buf = vr.buf;
         for (int i=0; i<nr; i++) {
            buf[i] = res[i];
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeAdd (VectorNd vr, VectorNd v1) {
      mulTransposeAdd (vr, v1, 0, colSize(), 0, rowSize());
   }
      
   /**
    * {@inheritDoc}
    */
   public void mulTransposeAdd (VectorNd vr, VectorNd v1, int nr, int nc) {
      mulTransposeAdd (vr, v1, 0, nr, 0, nc);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeAdd (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {
      mulTransposeCheckArgs (vr, v1, r0, nr, c0, nc);
      if (v1 == vr) {
         double[] res = new double[nr];
         mulTransposeVec (res, v1.buf, r0, nr, c0, nc);
         double[] buf = vr.buf;
         for (int i=0; i<nr; i++) {
            buf[i] += res[i];
         }
      }
      else {
         mulTransposeAddVec (vr.buf, v1.buf, r0, nr, c0, nc);
      }
   }

   /**
    * Returns true if one or more elements of this matrix is NaN.
    * 
    * @return true if one or more elements is NaN
    */
   public boolean hasNaN() {
      for (int i = 0; i < rowSize(); i++) {
         for (int j = 0; j < colSize(); j++) {
            if (Double.isNaN (get (i, j))) {
               return true;
            }
         }
      }
      return false;
   }

   static boolean checkUpperTriangular (Partition part) {
      if (part == Partition.UpperTriangular) {
         return true;
      }
      else if (part == Partition.Full) {
         return false;
      }
      else {
         throw new IllegalArgumentException ("Invalid partition " + part);
      }
   }

   static boolean checkLowerTriangular (Partition part) {
      if (part == Partition.LowerTriangular) {
         return true;
      }
      else if (part == Partition.Full) {
         return false;
      }
      else {
         throw new IllegalArgumentException ("Invalid partition " + part);
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getCRSIndices (int[] colIdxs, int[] rowOffs) {
      return getCRSIndices (
         colIdxs, rowOffs, Partition.Full, rowSize(), colSize());
   }

   /**
    * {@inheritDoc}
    */
   public int getCRSIndices (int[] colIdxs, int[] rowOffs, Partition part) {
      return getCRSIndices (colIdxs, rowOffs, part, rowSize(), colSize());
   }

   /**
    * {@inheritDoc}
    */
   public int getCRSIndices (
      int[] colIdxs, int[] rowOffs, Partition part, int numRows, int numCols) {
      if (numRows > rowSize() || numCols > colSize()) {
         throw new IllegalArgumentException (
            "specified sub-matrix is out of bounds");
      }
      checkUpperTriangular (part);
      int off = 0;
      int k = 0;
      for (int i = 0; i < numRows; i++) {
         int j0 = (part == Partition.Full ? 0 : i);
         for (int j = j0; j < numCols; j++) {
            colIdxs[k] = j+1;
            k++;
         }
         rowOffs[i] = off+1;
         if (part == Partition.Full) {
            off += numCols;
         }
         else {
            off += Math.max (numCols-i, 0);
         }
      }
      rowOffs[numRows] = off+1;
      return off;
   }

   /**
    * {@inheritDoc}
    */
   public int getCRSValues (double[] vals) {
      return getCRSValues (vals, Partition.Full, rowSize(), colSize());
   }

   /**
    * {@inheritDoc}
    */
   public int getCRSValues (double[] vals, Partition part) {
      return getCRSValues (vals, part, rowSize(), colSize());
   }

   /**
    * {@inheritDoc}
    */
   public int getCRSValues (
      double[] vals, Partition part, int numRows, int numCols) {
      if (numRows > rowSize() || numCols > colSize()) {
         throw new IllegalArgumentException (
            "specified sub-matrix is out of bounds");
      }
      checkUpperTriangular (part);
      int k = 0;
      for (int i = 0; i < numRows; i++) {
         int j0 = (part == Partition.Full ? 0 : i);
         for (int j = j0; j < numCols; j++) {
            vals[k] = get (i, j);
            k++;
         }
      }
      return k;
   }

   protected void checkSetCRSValuesArgs (
      double[] vals, int[] colIdxs, int[] rowOffs, int nvals, int nrows,
      Partition part) {
      checkUpperTriangular (part);
      if (nrows > rowSize()) {
         throw new IllegalArgumentException (
            "nrows exceeds number of matrix rows");
      }
      if (rowOffs.length < nrows+1) {
         throw new IllegalArgumentException (
            "Length of rowOffs is less than nrows+1");
      }
      if (vals.length < nvals) {
         throw new IllegalArgumentException (
            "Length of vals is less than nvals");
      }
      if (colIdxs.length < nvals) {
         throw new IllegalArgumentException (
            "Length of colIdxs is less than nvals");
      }
   }

   /**
    * Sets the contents of this matrix given a set of values in compressed row
    * storage (CRS). 
    * 
    * @param vals
    * non-zero element values
    * @param colIdxs
    * column indices for each non-zero element
    * @param rowOffs
    * location within <code>vals</code> ( and <code>colIdxs</code>)
    * corresponding to the first non-zero element in each row
    */
   public void setCRSValues (double[] vals, int[] colIdxs, int[] rowOffs) {
      setCRSValues (
         vals, colIdxs, rowOffs, vals.length, rowSize(), Partition.Full);
   }

   /**
    * {@inheritDoc}
    */
   public int getCCSIndices (int[] rowIdxs, int[] colOffs) {
      return getCCSIndices (
         rowIdxs, colOffs, Partition.Full, rowSize(), colSize());
   }

   /**
    * {@inheritDoc}
    */
   public int getCCSIndices (int[] rowIdxs, int[] colOffs, Partition part) {
      return getCCSIndices (rowIdxs, colOffs, part, rowSize(), colSize());
   }

   /**
    * {@inheritDoc}
    */
   public int getCCSIndices (
      int[] rowIdxs, int[] colOffs, Partition part, int numRows, int numCols) {
      if (numRows > rowSize() || numCols > colSize()) {
         throw new IllegalArgumentException (
            "specified sub-matrix is out of bounds");
      }
      checkLowerTriangular (part);
      int off = 0;
      int k = 0;
      for (int j = 0; j < numCols; j++) {
         int i0 = (part == Partition.Full ? 0 : j);
         for (int i = i0; i < numRows; i++) {
            rowIdxs[k] = i+1;
            k++;
         }
         colOffs[j] = off+1;
         if (part == Partition.Full) {
            off += numRows;
         }
         else {
            off += Math.max (numRows-j, 0);
         }
      }
      colOffs[numCols] = off+1;
      return off;
   }

   /**
    * {@inheritDoc}
    */
   public int getCCSValues (double[] vals) {
      return getCCSValues (vals, Partition.Full, rowSize(), colSize());
   }

   /**
    * {@inheritDoc}
    */
   public int getCCSValues (double[] vals, Partition part) {
      return getCCSValues (vals, part, rowSize(), colSize());
   }

   /**
    * {@inheritDoc}
    */
   public int getCCSValues (
      double[] vals, Partition part, int numRows, int numCols) {
      if (numRows > rowSize() || numCols > colSize()) {
         throw new IllegalArgumentException (
            "specified sub-matrix is out of bounds");
      }
      checkLowerTriangular (part);
      int k = 0;
      for (int j = 0; j < numCols; j++) {
         int i0 = (part == Partition.Full ? 0 : j);
         for (int i = i0; i < numRows; i++) {
            vals[k] = get (i, j);
            k++;
         }
      }
      return k;
   }

   protected void checkSetCCSValuesArgs (
      double[] vals, int[] rowIdxs, int[] colOffs, int nvals, int ncols,
      Partition part) {
      checkLowerTriangular (part);
      if (ncols > colSize()) {
         throw new IllegalArgumentException (
            "ncols exceeds number of matrix columns");
      }
//       if (part == Partition.LowerTriangular && ncols > rowSize()) {
//          throw new IllegalArgumentException (
//             "specified values exceed matrix bounds");
//       }
      if (colOffs.length < ncols+1) {
         throw new IllegalArgumentException (
            "Length of colOffs is less than ncols+1");
      }
      if (vals.length < nvals) {
         throw new IllegalArgumentException (
            "Length of vals is less than nvals");
      }
      if (rowIdxs.length < nvals) {
         throw new IllegalArgumentException (
            "Length of rowIdxs is less than nvals");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getSubMatrix (int baseRow, int baseCol, DenseMatrix Mdest)
      throws ImproperSizeException {
      if (baseRow < 0 || baseCol < 0) {
         throw new ImproperSizeException ("Negative row or column index");
      }
      int numDestRows = Mdest.rowSize();
      int numDestCols = Mdest.colSize();
      if (baseRow + numDestRows > rowSize() ||
          baseCol + numDestCols > colSize()) {
         throw new ImproperSizeException ("Dimensions out of bounds");
      }
      if (Mdest == this) { // nothing to do, since in this case baseRow and
                           // baseCol must
         // both equal 0 (otherwise a bounds exception would have been
         // tripped)
         return;
      }
      for (int i = 0; i < numDestRows; i++) {
         for (int j = 0; j < numDestCols; j++) {
            Mdest.set (i, j, get (i + baseRow, j + baseCol));
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSymmetric (double tol) {
      int nrows = rowSize();
      int ncols = colSize();
      if (nrows != ncols) {
         return false;
      }
      for (int i = 0; i < nrows; i++) {
         for (int j = i + 1; j < ncols; j++) {
            double valUpper = get (i, j);
            double valLower = get (j, i);
            if (Math.abs (valUpper - valLower) > tol) {
               return false;
            }
         }
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public int numNonZeroVals() {
      return numNonZeroVals (Partition.Full, rowSize(), colSize());
   }

   /**
    * {@inheritDoc}
    */
   public int numNonZeroVals (Partition part, int numRows, int numCols) {
      if (numRows > rowSize() || numCols > colSize()) {
         throw new IllegalArgumentException (
            "specified sub-matrix is out of bounds");
      }
      switch (part) {
         case Full: {
            return numRows * numCols;
         }
         case UpperTriangular: {
            if (numRows >= numCols) {
               return numCols * (numCols + 1) / 2;
            }
            else {
               return numRows * numCols - numRows * (numRows - 1) / 2;
            }
         }
         case LowerTriangular: {
            if (numCols >= numRows) {
               return numRows * (numRows + 1) / 2;
            }
            else {
               return numRows * numCols - numCols * (numCols - 1) / 2;
            }
         }
         case None: {
            return 0;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented partition: " + part);
         }
      }
   }

   /** 
    * {@inheritDoc}
    */   
   public String getSize() {
      return rowSize()+"x"+colSize();
   }

}
