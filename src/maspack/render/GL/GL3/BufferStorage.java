package maspack.render.GL.GL3;

import maspack.render.GL.GLSupport;

public class BufferStorage {
   
   public enum StorageType {
      UNSIGNED_BYTE,
      UNSIGNED_SHORT,
      UNSIGNED_INT,
      SIGNED_BYTE,
      SIGNED_SHORT,
      SIGNED_INT,
      UNSIGNED_2_10_10_10_REV,
      SIGNED_2_10_10_10_REV,
      FLOAT
   }
   
   StorageType type;
   int size;
   boolean normalized;
   int bytes;
   
   static BufferStorage UBYTE = new BufferStorage(StorageType.UNSIGNED_BYTE, 1, 1, false);
   static BufferStorage UBYTE_2 = new BufferStorage(StorageType.UNSIGNED_BYTE, 2, 2, false);
   static BufferStorage UBYTE_3 = new BufferStorage(StorageType.UNSIGNED_BYTE, 3, 3, false);
   static BufferStorage UBYTE_4 = new BufferStorage(StorageType.UNSIGNED_BYTE, 4, 4, false);
   static BufferStorage USHORT = new BufferStorage(StorageType.UNSIGNED_SHORT, 1, GLSupport.SHORT_SIZE, false);
   static BufferStorage USHORT_2 = new BufferStorage(StorageType.UNSIGNED_SHORT, 2, 2*GLSupport.SHORT_SIZE, false);
   static BufferStorage USHORT_3 = new BufferStorage(StorageType.UNSIGNED_SHORT, 3, 3*GLSupport.SHORT_SIZE, false);
   static BufferStorage USHORT_4 = new BufferStorage(StorageType.UNSIGNED_SHORT, 4, 4*GLSupport.SHORT_SIZE, false);
   static BufferStorage UINT = new BufferStorage(StorageType.UNSIGNED_INT, 1, GLSupport.INTEGER_SIZE, false);
   static BufferStorage UINT_1 = new BufferStorage(StorageType.UNSIGNED_INT, 2, 2*GLSupport.INTEGER_SIZE, false);
   static BufferStorage UINT_3 = new BufferStorage(StorageType.UNSIGNED_INT, 3, 3*GLSupport.INTEGER_SIZE, false);
   static BufferStorage UINT_4 = new BufferStorage(StorageType.UNSIGNED_INT, 4, 4*GLSupport.INTEGER_SIZE, false);
   
   static BufferStorage BYTE = new BufferStorage(StorageType.SIGNED_BYTE, 1, 1, false);
   static BufferStorage BYTE_2 = new BufferStorage(StorageType.SIGNED_BYTE, 2, 2, false);
   static BufferStorage BYTE_3 = new BufferStorage(StorageType.SIGNED_BYTE, 3, 3, false);
   static BufferStorage BYTE_4 = new BufferStorage(StorageType.SIGNED_BYTE, 4, 4, false);
   static BufferStorage SHORT = new BufferStorage(StorageType.SIGNED_SHORT, 1, GLSupport.SHORT_SIZE, false);
   static BufferStorage SHORT_2 = new BufferStorage(StorageType.SIGNED_SHORT, 2, 2*GLSupport.SHORT_SIZE, false);
   static BufferStorage SHORT_3 = new BufferStorage(StorageType.SIGNED_SHORT, 3, 3*GLSupport.SHORT_SIZE, false);
   static BufferStorage SHORT_4 = new BufferStorage(StorageType.SIGNED_SHORT, 4, 4*GLSupport.SHORT_SIZE, false);
   static BufferStorage INT = new BufferStorage(StorageType.SIGNED_INT, 1, GLSupport.INTEGER_SIZE, false);
   static BufferStorage INT_2 = new BufferStorage(StorageType.SIGNED_INT, 2, 2*GLSupport.INTEGER_SIZE, false);
   static BufferStorage INT_3 = new BufferStorage(StorageType.SIGNED_INT, 3, 3*GLSupport.INTEGER_SIZE, false);
   static BufferStorage INT_4 = new BufferStorage(StorageType.SIGNED_INT, 4, 4*GLSupport.INTEGER_SIZE, false);
   
   static BufferStorage UBYTE_N = new BufferStorage(StorageType.UNSIGNED_BYTE, 1, 1, true);
   static BufferStorage UBYTE_N_2 = new BufferStorage(StorageType.UNSIGNED_BYTE, 2, 2, true);
   static BufferStorage UBYTE_N_3 = new BufferStorage(StorageType.UNSIGNED_BYTE, 3, 3, true);
   static BufferStorage UBYTE_N_4 = new BufferStorage(StorageType.UNSIGNED_BYTE, 4, 4, true);
   static BufferStorage USHORT_N = new BufferStorage(StorageType.UNSIGNED_SHORT, 1, 1*GLSupport.SHORT_SIZE, true);
   static BufferStorage USHORT_N_2 = new BufferStorage(StorageType.UNSIGNED_SHORT, 2, 2*GLSupport.SHORT_SIZE, true);
   static BufferStorage USHORT_N_3 = new BufferStorage(StorageType.UNSIGNED_SHORT, 3, 3*GLSupport.SHORT_SIZE, true);
   static BufferStorage USHORT_N_4 = new BufferStorage(StorageType.UNSIGNED_SHORT, 4, 4*GLSupport.SHORT_SIZE, true);
   static BufferStorage UINT_N = new BufferStorage(StorageType.UNSIGNED_INT, 1, 1*GLSupport.INTEGER_SIZE, true);
   static BufferStorage UINT_N_1 = new BufferStorage(StorageType.UNSIGNED_INT, 2, 2*GLSupport.INTEGER_SIZE, true);
   static BufferStorage UINT_N_3 = new BufferStorage(StorageType.UNSIGNED_INT, 3, 3*GLSupport.INTEGER_SIZE, true);
   static BufferStorage UINT_N_4 = new BufferStorage(StorageType.UNSIGNED_INT, 4, 4*GLSupport.INTEGER_SIZE, true);
   
   static BufferStorage BYTE_N = new BufferStorage(StorageType.SIGNED_BYTE, 1, 1, true);
   static BufferStorage BYTE_N_2 = new BufferStorage(StorageType.SIGNED_BYTE, 2, 2, true);
   static BufferStorage BYTE_N_3 = new BufferStorage(StorageType.SIGNED_BYTE, 3, 3, true);
   static BufferStorage BYTE_N_4 = new BufferStorage(StorageType.SIGNED_BYTE, 4, 4, true);
   static BufferStorage SHORT_N = new BufferStorage(StorageType.SIGNED_SHORT, 1, 1*GLSupport.SHORT_SIZE, true);
   static BufferStorage SHORT_N_2 = new BufferStorage(StorageType.SIGNED_SHORT, 2, 2*GLSupport.SHORT_SIZE, true);
   static BufferStorage SHORT_N_3 = new BufferStorage(StorageType.SIGNED_SHORT, 3, 3*GLSupport.SHORT_SIZE, true);
   static BufferStorage SHORT_N_4 = new BufferStorage(StorageType.SIGNED_SHORT, 4, 4*GLSupport.SHORT_SIZE, true);
   static BufferStorage INT_N = new BufferStorage(StorageType.SIGNED_INT, 1, 1*GLSupport.INTEGER_SIZE, true);
   static BufferStorage INT_N_2 = new BufferStorage(StorageType.SIGNED_INT, 2, 2*GLSupport.INTEGER_SIZE, true);
   static BufferStorage INT_N_3 = new BufferStorage(StorageType.SIGNED_INT, 3, 3*GLSupport.INTEGER_SIZE, true);
   static BufferStorage INT_N_4 = new BufferStorage(StorageType.SIGNED_INT, 4, 4*GLSupport.INTEGER_SIZE, true);
   
   static BufferStorage FLOAT = new BufferStorage(StorageType.FLOAT, 1, 1*GLSupport.FLOAT_SIZE, false);
   static BufferStorage FLOAT_2 = new BufferStorage(StorageType.FLOAT, 2, 2*GLSupport.FLOAT_SIZE, false);
   static BufferStorage FLOAT_3 = new BufferStorage(StorageType.FLOAT, 3, 3*GLSupport.FLOAT_SIZE, false);
   static BufferStorage FLOAT_4 = new BufferStorage(StorageType.FLOAT, 4, 4*GLSupport.FLOAT_SIZE, false);
   
   // packed formats
   static BufferStorage UINT_2_10_10_10_REV = new BufferStorage(StorageType.UNSIGNED_2_10_10_10_REV, 4, 4, false);
   static BufferStorage UINT_2_10_10_10_REV_N = new BufferStorage(StorageType.UNSIGNED_2_10_10_10_REV, 4, 4, true);
   static BufferStorage INT_2_10_10_10_REV = new BufferStorage(StorageType.SIGNED_2_10_10_10_REV, 4, 4, false);
   static BufferStorage INT_2_10_10_10_REV_N = new BufferStorage(StorageType.SIGNED_2_10_10_10_REV, 4, 4, true);
   
   public BufferStorage(StorageType type, int size, int bytes, boolean normalized) {
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

}
