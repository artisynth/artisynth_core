package maspack.render.GL.GL3;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import maspack.render.GL.GLSupport;

public class GL3AttributeStorage {
   
   public enum StorageType {
      UNSIGNED_BYTE (GL.GL_UNSIGNED_BYTE),
      UNSIGNED_SHORT (GL.GL_UNSIGNED_SHORT),
      UNSIGNED_INT (GL.GL_UNSIGNED_INT),
      SIGNED_BYTE (GL.GL_BYTE),
      SIGNED_SHORT (GL.GL_SHORT),
      SIGNED_INT (GL3.GL_INT),
      UNSIGNED_2_10_10_10_REV (GL3.GL_UNSIGNED_INT_2_10_10_10_REV),
      SIGNED_2_10_10_10_REV (GL3.GL_INT_2_10_10_10_REV),
      FLOAT (GL.GL_FLOAT);
      
      int glType;
      StorageType(int glType) {
         this.glType = glType;
      }
      public int getGLType() {
         return this.glType;
      }
      
   }
   
   StorageType type;
   int size;
   boolean normalized;
   int bytes;
   
   static GL3AttributeStorage UBYTE = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 1, 1, false);
   static GL3AttributeStorage UBYTE_2 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 2, 2, false);
   static GL3AttributeStorage UBYTE_3 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 3, 3, false);
   static GL3AttributeStorage UBYTE_4 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 4, 4, false);
   static GL3AttributeStorage USHORT = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 1, GLSupport.SHORT_SIZE, false);
   static GL3AttributeStorage USHORT_2 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 2, 2*GLSupport.SHORT_SIZE, false);
   static GL3AttributeStorage USHORT_3 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 3, 3*GLSupport.SHORT_SIZE, false);
   static GL3AttributeStorage USHORT_4 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 4, 4*GLSupport.SHORT_SIZE, false);
   static GL3AttributeStorage UINT = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 1, GLSupport.INTEGER_SIZE, false);
   static GL3AttributeStorage UINT_1 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 2, 2*GLSupport.INTEGER_SIZE, false);
   static GL3AttributeStorage UINT_3 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 3, 3*GLSupport.INTEGER_SIZE, false);
   static GL3AttributeStorage UINT_4 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 4, 4*GLSupport.INTEGER_SIZE, false);
   
   static GL3AttributeStorage BYTE = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 1, 1, false);
   static GL3AttributeStorage BYTE_2 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 2, 2, false);
   static GL3AttributeStorage BYTE_3 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 3, 3, false);
   static GL3AttributeStorage BYTE_4 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 4, 4, false);
   static GL3AttributeStorage SHORT = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 1, GLSupport.SHORT_SIZE, false);
   static GL3AttributeStorage SHORT_2 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 2, 2*GLSupport.SHORT_SIZE, false);
   static GL3AttributeStorage SHORT_3 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 3, 3*GLSupport.SHORT_SIZE, false);
   static GL3AttributeStorage SHORT_4 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 4, 4*GLSupport.SHORT_SIZE, false);
   static GL3AttributeStorage INT = new GL3AttributeStorage(StorageType.SIGNED_INT, 1, GLSupport.INTEGER_SIZE, false);
   static GL3AttributeStorage INT_2 = new GL3AttributeStorage(StorageType.SIGNED_INT, 2, 2*GLSupport.INTEGER_SIZE, false);
   static GL3AttributeStorage INT_3 = new GL3AttributeStorage(StorageType.SIGNED_INT, 3, 3*GLSupport.INTEGER_SIZE, false);
   static GL3AttributeStorage INT_4 = new GL3AttributeStorage(StorageType.SIGNED_INT, 4, 4*GLSupport.INTEGER_SIZE, false);
   
   static GL3AttributeStorage UBYTE_N = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 1, 1, true);
   static GL3AttributeStorage UBYTE_N_2 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 2, 2, true);
   static GL3AttributeStorage UBYTE_N_3 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 3, 3, true);
   static GL3AttributeStorage UBYTE_N_4 = new GL3AttributeStorage(StorageType.UNSIGNED_BYTE, 4, 4, true);
   static GL3AttributeStorage USHORT_N = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 1, 1*GLSupport.SHORT_SIZE, true);
   static GL3AttributeStorage USHORT_N_2 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 2, 2*GLSupport.SHORT_SIZE, true);
   static GL3AttributeStorage USHORT_N_3 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 3, 3*GLSupport.SHORT_SIZE, true);
   static GL3AttributeStorage USHORT_N_4 = new GL3AttributeStorage(StorageType.UNSIGNED_SHORT, 4, 4*GLSupport.SHORT_SIZE, true);
   static GL3AttributeStorage UINT_N = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 1, 1*GLSupport.INTEGER_SIZE, true);
   static GL3AttributeStorage UINT_N_1 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 2, 2*GLSupport.INTEGER_SIZE, true);
   static GL3AttributeStorage UINT_N_3 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 3, 3*GLSupport.INTEGER_SIZE, true);
   static GL3AttributeStorage UINT_N_4 = new GL3AttributeStorage(StorageType.UNSIGNED_INT, 4, 4*GLSupport.INTEGER_SIZE, true);
   
   static GL3AttributeStorage BYTE_N = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 1, 1, true);
   static GL3AttributeStorage BYTE_N_2 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 2, 2, true);
   static GL3AttributeStorage BYTE_N_3 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 3, 3, true);
   static GL3AttributeStorage BYTE_N_4 = new GL3AttributeStorage(StorageType.SIGNED_BYTE, 4, 4, true);
   static GL3AttributeStorage SHORT_N = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 1, 1*GLSupport.SHORT_SIZE, true);
   static GL3AttributeStorage SHORT_N_2 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 2, 2*GLSupport.SHORT_SIZE, true);
   static GL3AttributeStorage SHORT_N_3 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 3, 3*GLSupport.SHORT_SIZE, true);
   static GL3AttributeStorage SHORT_N_4 = new GL3AttributeStorage(StorageType.SIGNED_SHORT, 4, 4*GLSupport.SHORT_SIZE, true);
   static GL3AttributeStorage INT_N = new GL3AttributeStorage(StorageType.SIGNED_INT, 1, 1*GLSupport.INTEGER_SIZE, true);
   static GL3AttributeStorage INT_N_2 = new GL3AttributeStorage(StorageType.SIGNED_INT, 2, 2*GLSupport.INTEGER_SIZE, true);
   static GL3AttributeStorage INT_N_3 = new GL3AttributeStorage(StorageType.SIGNED_INT, 3, 3*GLSupport.INTEGER_SIZE, true);
   static GL3AttributeStorage INT_N_4 = new GL3AttributeStorage(StorageType.SIGNED_INT, 4, 4*GLSupport.INTEGER_SIZE, true);
   
   static GL3AttributeStorage FLOAT = new GL3AttributeStorage(StorageType.FLOAT, 1, 1*GLSupport.FLOAT_SIZE, false);
   static GL3AttributeStorage FLOAT_2 = new GL3AttributeStorage(StorageType.FLOAT, 2, 2*GLSupport.FLOAT_SIZE, false);
   static GL3AttributeStorage FLOAT_3 = new GL3AttributeStorage(StorageType.FLOAT, 3, 3*GLSupport.FLOAT_SIZE, false);
   static GL3AttributeStorage FLOAT_3_N = new GL3AttributeStorage(StorageType.FLOAT, 3, 3*GLSupport.FLOAT_SIZE, true);
   static GL3AttributeStorage FLOAT_4 = new GL3AttributeStorage(StorageType.FLOAT, 4, 4*GLSupport.FLOAT_SIZE, false);
   
   // packed formats
   static GL3AttributeStorage UINT_2_10_10_10_REV = new GL3AttributeStorage(StorageType.UNSIGNED_2_10_10_10_REV, 4, 4, false);
   static GL3AttributeStorage UINT_2_10_10_10_REV_N = new GL3AttributeStorage(StorageType.UNSIGNED_2_10_10_10_REV, 4, 4, true);
   static GL3AttributeStorage INT_2_10_10_10_REV = new GL3AttributeStorage(StorageType.SIGNED_2_10_10_10_REV, 4, 4, false);
   static GL3AttributeStorage INT_2_10_10_10_REV_N = new GL3AttributeStorage(StorageType.SIGNED_2_10_10_10_REV, 4, 4, true);
   
   public GL3AttributeStorage(StorageType type, int size, int bytes, boolean normalized) {
      this.type = type;
      this.size = size;
      this.bytes = bytes;
      this.normalized = normalized;
   }
   
   public StorageType type() {
      return type;
   }
   
   public int size() {
      return size;
   }
   
   public int bytes() {
      return bytes;
   }
   
   public boolean isNormalized() {
      return normalized;
   }

   public int getGLType () {
      return type.getGLType ();
   }

}
