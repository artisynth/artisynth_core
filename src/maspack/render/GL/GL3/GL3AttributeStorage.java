package maspack.render.GL.GL3;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.render.GL.GLSupport;

public class GL3AttributeStorage {
   
   public enum StorageType {
      UNSIGNED_BYTE (GL.GL_UNSIGNED_BYTE, GLSupport.BYTE_SIZE),
      UNSIGNED_SHORT (GL.GL_UNSIGNED_SHORT, GLSupport.SHORT_SIZE),
      UNSIGNED_INT (GL.GL_UNSIGNED_INT,GLSupport.INTEGER_SIZE),
      SIGNED_BYTE (GL.GL_BYTE, GLSupport.BYTE_SIZE),
      SIGNED_SHORT (GL.GL_SHORT, GLSupport.SHORT_SIZE),
      SIGNED_INT (GL3.GL_INT, GLSupport.INTEGER_SIZE),
      UNSIGNED_2_10_10_10_REV (GL3.GL_UNSIGNED_INT_2_10_10_10_REV, GLSupport.INTEGER_SIZE),
      SIGNED_2_10_10_10_REV (GL3.GL_INT_2_10_10_10_REV, GLSupport.INTEGER_SIZE),
      FLOAT (GL.GL_FLOAT, GLSupport.FLOAT_SIZE);
      
      int glType;
      int bytes;
      StorageType(int glType, int bytes) {
         this.glType = glType;
         this.bytes = bytes;
      }
      public int getGLType() {
         return this.glType;
      }
      public int bytes() {
         return bytes;
      }
      
   }
   
   StorageType type;
   int ncols;
   int nrows;
   boolean normalized;
   
   static GL3AttributeStorage UBYTE = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 1, false);
   static GL3AttributeStorage UBYTE_2 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 2, false);
   static GL3AttributeStorage UBYTE_3 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 3, false);
   static GL3AttributeStorage UBYTE_4 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 4, false);
   static GL3AttributeStorage USHORT = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 1, false);
   static GL3AttributeStorage USHORT_2 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 2, false);
   static GL3AttributeStorage USHORT_3 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 3, false);
   static GL3AttributeStorage USHORT_4 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 4, false);
   static GL3AttributeStorage UINT = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 1, false);
   static GL3AttributeStorage UINT_1 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 2, false);
   static GL3AttributeStorage UINT_3 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 3, false);
   static GL3AttributeStorage UINT_4 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 4, false);
   
   static GL3AttributeStorage BYTE = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 1, false);
   static GL3AttributeStorage BYTE_2 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 2, false);
   static GL3AttributeStorage BYTE_3 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 3, false);
   static GL3AttributeStorage BYTE_4 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 4, false);
   static GL3AttributeStorage SHORT = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 1, false);
   static GL3AttributeStorage SHORT_2 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 2, false);
   static GL3AttributeStorage SHORT_3 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 3, false);
   static GL3AttributeStorage SHORT_4 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 4, false);
   static GL3AttributeStorage INT = new GL3AttributeStorage(StorageType.SIGNED_INT, 1, false);
   static GL3AttributeStorage INT_2 = new GL3AttributeStorage(StorageType.SIGNED_INT, 2, false);
   static GL3AttributeStorage INT_3 = new GL3AttributeStorage(StorageType.SIGNED_INT, 3, false);
   static GL3AttributeStorage INT_4 = new GL3AttributeStorage(StorageType.SIGNED_INT, 4, false);
   
   static GL3AttributeStorage UBYTE_N = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 1, true);
   static GL3AttributeStorage UBYTE_N_2 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 2, true);
   static GL3AttributeStorage UBYTE_N_3 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 3, true);
   static GL3AttributeStorage UBYTE_N_4 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 4, true);
   static GL3AttributeStorage USHORT_N = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 1, true);
   static GL3AttributeStorage USHORT_N_2 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 2, true);
   static GL3AttributeStorage USHORT_N_3 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 3, true);
   static GL3AttributeStorage USHORT_N_4 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 4, true);
   static GL3AttributeStorage UINT_N = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 1, true);
   static GL3AttributeStorage UINT_N_1 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 2, true);
   static GL3AttributeStorage UINT_N_3 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 3, true);
   static GL3AttributeStorage UINT_N_4 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 4, true);
   
   static GL3AttributeStorage BYTE_N = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 1, true);
   static GL3AttributeStorage BYTE_N_2 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 2, true);
   static GL3AttributeStorage BYTE_N_3 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 3, true);
   static GL3AttributeStorage BYTE_N_4 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 4, true);
   static GL3AttributeStorage SHORT_N = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 1, true);
   static GL3AttributeStorage SHORT_N_2 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 2, true);
   static GL3AttributeStorage SHORT_N_3 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 3, true);
   static GL3AttributeStorage SHORT_N_4 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 4, true);
   static GL3AttributeStorage INT_N = new GL3AttributeStorage(StorageType.SIGNED_INT, 1, true);
   static GL3AttributeStorage INT_N_2 = new GL3AttributeStorage(StorageType.SIGNED_INT, 2, true);
   static GL3AttributeStorage INT_N_3 = new GL3AttributeStorage(StorageType.SIGNED_INT, 3, true);
   static GL3AttributeStorage INT_N_4 = new GL3AttributeStorage(StorageType.SIGNED_INT, 4, true);
   
   static GL3AttributeStorage FLOAT = new GL3AttributeStorage(StorageType.FLOAT, 1, false);
   static GL3AttributeStorage FLOAT_2 = new GL3AttributeStorage(StorageType.FLOAT, 2, false);
   static GL3AttributeStorage FLOAT_3 = new GL3AttributeStorage(StorageType.FLOAT, 3, false);
   static GL3AttributeStorage FLOAT_N_3 = new GL3AttributeStorage(StorageType.FLOAT, 3, true);
   static GL3AttributeStorage FLOAT_4 = new GL3AttributeStorage(StorageType.FLOAT, 4, false);
   
   // matrices
   static GL3AttributeStorage FLOAT_4x4 = new GL3AttributeStorage(StorageType.FLOAT, 4, 4, false);
   
   // packed formats
   static GL3AttributeStorage UINT_2_10_10_10_REV = new GL3AttributeStorage(StorageType.UNSIGNED_2_10_10_10_REV, 4, false);
   static GL3AttributeStorage UINT_2_10_10_10_REV_N = new GL3AttributeStorage(StorageType.UNSIGNED_2_10_10_10_REV, 4, true);
   static GL3AttributeStorage INT_2_10_10_10_REV = new GL3AttributeStorage(StorageType.SIGNED_2_10_10_10_REV, 4, false);
   static GL3AttributeStorage INT_2_10_10_10_REV_N = new GL3AttributeStorage(StorageType.SIGNED_2_10_10_10_REV, 4, true);
   
   public GL3AttributeStorage(StorageType type, int size, boolean normalized) {
      this.type = type;
      this.nrows = size;
      this.ncols = 1;
      this.normalized = normalized;
   }
   
   public GL3AttributeStorage(StorageType type, int rows, int cols, boolean normalized) {
      this.type = type;
      this.nrows = rows;
      this.ncols = cols;
      this.normalized = normalized;
   }
   
   public StorageType type() {
      return type;
   }
   
   public int size() {
      return nrows*ncols;
   }
   
   public int rows() {
      return nrows;
   }
   
   public int cols() {
      return ncols;
   }
   
   public boolean isMatrix() {
      return (ncols > 1);
   }
   
   /**
    * Aligned width in bytes
    */
   public int width() {
      return colWidth()*cols();
   }
   
   public int colWidth() {
      return GL3Utilities.getAlignedWidth(rows()*type.bytes());
   }
   
   public boolean isNormalized() {
      return normalized;
   }

   public int getGLType () {
      return type.getGLType ();
   }

}
