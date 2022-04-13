package maspack.matrix;

import maspack.util.*;

import java.io.*;

public class CRSValues {
   int myNumRows;
   int myNumCols;
   int myNumNonZeros;
   int[] myRowOffs;
   int[] myColIdxs;
   double[] myValues;
   String myMatrixType;

   public int rowSize() {
      return myNumRows;
   }

   public int colSize() {
      return myNumCols;
   }

   public int numNonZeros() {
      return myNumNonZeros;
   }

   public double[] getValues() {
      return myValues;
   }

   public int[] getRowOffs() {
      return myRowOffs;
   }

   public int[] getColIdxs() {
      return myColIdxs;
   }

   public int[] getRowIdxs() {
      int[] rowIdxs = new int[myNumNonZeros];
      for (int i=0; i<myNumRows; i++) {
         for (int j=myRowOffs[i]-1; j<myRowOffs[i+1]-1; j++) {
            rowIdxs[j] = i+1;
         }
      }
      return rowIdxs;
   }

   public CRSValues () {
   }

   public CRSValues (Matrix M) {
      myNumRows = M.rowSize();
      myNumCols = M.colSize();
      myRowOffs = new int[myNumRows+1];
      myNumNonZeros = M.numNonZeroVals();
      myColIdxs = new int[myNumNonZeros];
      myValues = new double[myNumNonZeros];
      M.getCRSIndices (myColIdxs, myRowOffs, Matrix.Partition.Full);
      M.getCRSValues (myValues, Matrix.Partition.Full);
   }
   
   public CRSValues (
      int[] rowOffs, int[] colIdxs, double[] values, int ncols) {
      myNumRows = rowOffs.length-1;
      myNumCols = ncols;
      myRowOffs = rowOffs;
      myNumNonZeros = values.length;
      myColIdxs = colIdxs;
      myValues = values;
   }
   
   public void incrementIndices() {
      if (myRowOffs != null) {
         for (int i=0; i<myRowOffs.length; i++) {
            myRowOffs[i]++;
         }
      }
      if (myColIdxs != null) {
         for (int k=0; k<myColIdxs.length; k++) {
            myColIdxs[k]++;
         }
      }
   }

   public void decrementIndices() {
      if (myRowOffs != null) {
         for (int i=0; i<myRowOffs.length; i++) {
            myRowOffs[i]--;
         }
      }
      if (myColIdxs != null) {
         for (int k=0; k<myColIdxs.length; k++) {
            myColIdxs[k]--;
         }
      }
   }

   public boolean write (String fileName) {
      try {
         write (new File(fileName), "%g");
         return true;
      }
      catch (IOException e) {
         System.out.println ("Error writing file "+fileName+": " + e);
         return false;
      }
   }

   public boolean write (String fileName, String fmtStr) {
      try {
         write (new File(fileName), fmtStr);
         return true;
      }
      catch (IOException e) {
         System.out.println ("Error writing file "+fileName+": " + e);
         return false;
      }
   }

   public void write (File file, String fmtStr) throws IOException {
      PrintWriter pw = null;
      try {
         pw = new PrintWriter (new BufferedWriter (new FileWriter(file)));      
         write (pw, new NumberFormat (fmtStr));
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (pw != null) {
            try {
               pw.close();
            }
            catch (Exception e) {
               // ignore
            }
         }
      }
   }

   public void write (PrintWriter pw, NumberFormat fmt) throws IOException {
      if (myRowOffs == null || myColIdxs == null) {
         throw new IllegalStateException (
            "roff offsets and/or column indices not present");
      }
      pw.println (myNumRows);
      for (int i=0; i<myNumRows+1; i++) {
         pw.print (myRowOffs[i]+" ");
      }
      pw.println ("");
      for (int i=0; i<myNumNonZeros; i++) {
         pw.print (myColIdxs[i]+" ");
      }
      pw.println ("");
      for (int i=0; i<myNumNonZeros; i++) {
         pw.print (fmt.format(myValues[i])+" ");
      }
      pw.println ("");
   }

   public void scan (ReaderTokenizer rtok) throws IOException {
      rtok.nextToken();
      if (rtok.tokenIsWord()) {
         myMatrixType = rtok.sval;
      }
      else {
         rtok.pushBack();
      }
      int size = rtok.scanInteger();
      myRowOffs = new int[size+1];
      for (int i=0; i<size+1; i++) {
         myRowOffs[i] = rtok.scanInteger();
      }
      int nvals = myRowOffs[size]-1;
      myColIdxs = new int[nvals];
      myValues = new double[nvals];
      for (int i=0; i<nvals; i++) {
         myColIdxs[i] = rtok.scanInteger();
      }
      for (int i=0; i<nvals; i++) {
         myValues[i] = rtok.scanNumber();
      }      
      myNumRows = size;
      myNumCols = size;
      myNumNonZeros = nvals;
   }

   public void scan (String filename) {
      ReaderTokenizer rtok = null;
      try {
         rtok = new ReaderTokenizer (
            new BufferedReader (new FileReader (filename)));
         scan (rtok);
      }
      catch (IOException e) {
         System.out.println ("Error reading '"+filename+"': "+e);
      }
      finally {
         if (rtok != null) {
            rtok.close();
         }
      }
   }

}

