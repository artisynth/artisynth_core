package maspack.matrix;

/**
 * Structure signature for a sparse matrix, and a sparse block matrix in
 * particular.
 */
public class SparseSignature {
   
   int myRowSize;
   int myColSize;
   boolean myVertical;
   int[] myData;

   public SparseSignature (
      int rowSize, int colSize, boolean vertical, int[] data) {
      myRowSize = rowSize;
      myColSize = colSize;
      myVertical = vertical;
      if (data == null) {
         throw new IllegalArgumentException (
            "data argument can't be null");
      }
      myData = data;
   }

   public int rowSize() {
      return myRowSize;
   }

   public int colSize() {
      return myColSize;
   }

   public int[] getData() {
      return myData;
   }

   public boolean isVertical() {
      return myVertical;
   }

   public boolean equals (SparseSignature sig) {
      if (myRowSize != sig.myRowSize || 
          myColSize != sig.myColSize || 
          myVertical != sig.myVertical || 
          myData.length != sig.myData.length) {
         return false;
      }
      for (int i=0; i<myData.length; i++) {
         if (myData[i] != sig.myData[i]) {
            return false;
         }
      }
      return true;
   }
}
