package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import maspack.render.GL.GLSupport;

public abstract class PositionBufferPutter {

   public abstract void putPosition(ByteBuffer buff, float x, float y, float z);
   public abstract int bytesPerPosition();
   public abstract GL3AttributeStorage storage();
   
   public void putPosition(ByteBuffer buff, float[] pos) {
      putPosition(buff, pos[0], pos[1], pos[2]);
   }
   
   public void putPosition(ByteBuffer buff, float[] pos, int offset) {
      putPosition(buff, pos[offset], pos[offset+1], pos[offset+2]);
   }
   
   public void putPositions(ByteBuffer buff, float[] pos) {
      for (int i=0; i<pos.length-2; i+=3) {
         putPosition(buff, pos, i);
      }
   }
   
   public void putPositions(ByteBuffer buff, float[] pos, int offset, int stride, int count) {
      if (stride <= 0) {
         stride = 3;
      }
      int idx = offset;
      for (int i=0; i<count; ++i) {
         putPosition(buff, pos, idx);
         idx += stride;
      }  
   }
   
   public void putPositions(ByteBuffer buff, Iterable<float[]> positions) {
      for (float[] pos : positions) {
         putPosition(buff, pos);
      }
   }
  
   private void setLocation(ByteBuffer buff, int location) {
      if (location >= 0) {
         if (buff.position() != location) {
            buff.position(location);
         }
      }
   }
   
   public void putPosition(ByteBuffer buff, int location, float[] pos) {
      setLocation(buff, location);
      putPosition(buff, pos);
   }
   
   public void putPosition(ByteBuffer buff, int location, float[] pos, int offset) {
      setLocation(buff, location);
      putPosition(buff, pos, offset);
   }
   
   public void putPositions(ByteBuffer buff, int location, float[] pos) {
      setLocation(buff, location);
      putPositions(buff, pos);
   }
   
   public void putPositions(ByteBuffer buff, int location, float[] pos, int offset, int stride, int count) {
      setLocation(buff, location);
      putPositions(buff, pos, offset, stride, count);
   }
   
   public void putPositions(ByteBuffer buff, int location, int bstride, float[] pos, int offset, int pstride, int count) {
      
      if (pstride <= 0) {
         pstride = 3;
      }
      if (bstride <= 0) {
         bstride = bytesPerPosition();
      }
      for (int i=0; i<count; ++i) {
         setLocation(buff, location+i*bstride);   
         putPosition(buff, pos, offset+i*pstride);   
      }
      
   }
   
   public void putPositions(ByteBuffer buff, int location, Iterable<float[]> positions) {
      setLocation(buff, location);
      putPositions(buff, positions);
   }
   
   public static class FloatPositionBufferPutter extends PositionBufferPutter {
      
      static FloatPositionBufferPutter instance;
      
      protected FloatPositionBufferPutter() {}
      
      public static FloatPositionBufferPutter getInstance() {
         if (instance == null) {
            instance = new FloatPositionBufferPutter ();
         }
         return instance;
      }
      
      @Override
      public void putPosition(ByteBuffer buff, float x, float y, float z) {
         buff.putFloat(x);
         buff.putFloat(y);
         buff.putFloat(z);  
      }

      @Override
      public int bytesPerPosition() {
         return 3*GLSupport.FLOAT_SIZE;
      }
      
      @Override
      public GL3AttributeStorage storage() {
         return GL3AttributeStorage.FLOAT_3;
      }
   }
   
   public static PositionBufferPutter getDefault() {
      return new FloatPositionBufferPutter();
   }
   
}
