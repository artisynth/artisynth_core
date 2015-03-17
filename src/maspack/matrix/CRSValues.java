package maspack.matrix;

public class CRSValues {
   int numRows;
   int numCols;
   int numNonZeros;
   int[] rowOffs;
   int[] colIdxs;
   double[] values;

   public int rowSize() {
      return numRows;
   }

   public int colSize() {
      return numCols;
   }

   public int numNonZeroVals() {
      return numNonZeros;
   }

   public double[] getValues() {
      return values;
   }

   public int[] getRowOffs() {
      return rowOffs;
   }

   public int[] getColIdxs() {
      return colIdxs;
   }

   public int[] getRowIdxs() {
      int[] rowIdxs = new int[numNonZeros];
      for (int i=0; i<numRows; i++) {
         for (int j=rowOffs[i]-1; j<rowOffs[i+1]-1; j++) {
            rowIdxs[j] = i+1;
         }
      }
      return rowIdxs;
   }

   public CRSValues (Matrix M) {
      numRows = M.rowSize();
      numCols = M.colSize();
      rowOffs = new int[numRows+1];
      numNonZeros = M.numNonZeroVals();
      colIdxs = new int[numNonZeros];
      values = new double[numNonZeros];
      M.getCRSIndices (colIdxs, rowOffs, Matrix.Partition.Full);
      M.getCRSValues (values, Matrix.Partition.Full);
   }
}

