package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import maspack.render.GL.GLSupport;

public abstract class IndexBufferPutter {

   public abstract void putIndex(ByteBuffer buff, int idx);
   public abstract int bytesPerIndex();
   public abstract GL3AttributeStorage storage();
   
   public void putIndices(ByteBuffer buff, int... idxs) {
      for (int i=0; i<idxs.length; ++i) {
         putIndex(buff, idxs[i]);
      }
   }
   
   public void putIndices(ByteBuffer buff, int[] idxs, int offset, int stride, int count) {
      if (stride <= 0) {
         stride = 3;
      }
      int idx = offset;
      for (int i=0; i<count; ++i) {
         putIndex(buff, idxs[idx]);
         idx += stride;
      }  
   }
   
   public void putIndices(ByteBuffer buff, Iterable<int[]> idxs) {
      for (int[] ii : idxs) {
         for (int i=0; i<ii.length; ++i) {
            putIndex(buff, ii[i]);
         }
      }
   }
   
   private void setLocation(ByteBuffer buff, int location) {
      if (location >= 0 && buff.position() != location) {
         buff.position(location);
      }
   }
   
   public void putIndex(ByteBuffer buff, int location, int idx) {
      setLocation(buff, location);
      putIndex(buff, idx);
   }
   
   public void putIndices(ByteBuffer buff, int location, int[] idxs) {
      setLocation(buff, location);
      putIndices(buff, idxs);
   }
   
   public void putIndices(ByteBuffer buff, int location, int[] idxs, int offset, int stride, int count) {
      setLocation(buff, location);
      putIndices(buff, idxs, offset, stride, count);
   }
   
   public void putIndices(ByteBuffer buff, int location, Iterable<int[]> idxs) {
      setLocation(buff, location);
      putIndices(buff, idxs);
   }
   
   public static class ByteIndexBufferPutter extends IndexBufferPutter {

      @Override
      public void putIndex(ByteBuffer buff, int idx) {
         buff.put((byte)idx);
      }

      @Override
      public int bytesPerIndex() {
         return GLSupport.BYTE_SIZE;
      }

      @Override
      public GL3AttributeStorage storage() {
         return GL3AttributeStorage.UBYTE;
      }
      
   }
   
   public static class ShortIndexBufferPutter extends IndexBufferPutter {

      @Override
      public void putIndex(ByteBuffer buff, int idx) {
         buff.putShort((short)idx);
      }

      @Override
      public int bytesPerIndex() {
         return GLSupport.SHORT_SIZE;
      }
      
      @Override
      public GL3AttributeStorage storage() {
         return GL3AttributeStorage.USHORT;
      }
      
   }
   
   public static class IntegerIndexBufferPutter extends IndexBufferPutter {

      @Override
      public void putIndex(ByteBuffer buff, int idx) {
         buff.putInt(idx);
      }
      
      @Override
      public int bytesPerIndex() {
         return GLSupport.INTEGER_SIZE;
      }
      
      @Override
      public GL3AttributeStorage storage() {
         return GL3AttributeStorage.UINT;
      }
   }
   
   /**
    * Creates the most compact putter given a maximum index
    * @param maxIndex
    */
   public static IndexBufferPutter createDefault(int maxIndex) {
      if (maxIndex <= ((long)Byte.MAX_VALUE-(long)Byte.MIN_VALUE)) {
         return new ByteIndexBufferPutter();
      } else if (maxIndex <= ((long)Short.MAX_VALUE-(long)Short.MIN_VALUE)) {
         return new ShortIndexBufferPutter();
      } else {
         return new IntegerIndexBufferPutter();
      }
   }
   
}
