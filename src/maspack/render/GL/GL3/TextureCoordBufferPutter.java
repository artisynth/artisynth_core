package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import maspack.render.GL.GLSupport;

public abstract class TextureCoordBufferPutter {

   public abstract void putTextureCoord(ByteBuffer buff, float x, float y);
   public abstract int bytesPerTextureCoord();
   public abstract GL3AttributeStorage storage();
   
   public void putTextureCoord(ByteBuffer buff, float[] tex) {
      putTextureCoord(buff, tex[0], tex[1]);
   }
   
   public void putTextureCoord(ByteBuffer buff, float[] tex, int offset) {
      putTextureCoord(buff, tex[offset], tex[offset+1]);
   }
   
   public void putTextureCoords(ByteBuffer buff, float[] texs) {
      for (int i=0; i<texs.length-2; i+=3) {
         putTextureCoord(buff, texs, i);
      }
   }
   
   public void putTextureCoords(ByteBuffer buff, float[] texs, int offset, int stride, int count) {
      if (stride <= 0) {
         stride = 3;
      }
      int idx = offset;
      for (int i=0; i<count; ++i) {
         putTextureCoord(buff, texs, idx);
         idx += stride;
      }  
   }
   
   public void putTextureCoords(ByteBuffer buff, Iterable<float[]> normals) {
      for (float[] tex : normals) {
         putTextureCoord(buff, tex);
      }
   }
   
   private void setLocation(ByteBuffer buff, int location) {
      if (location >= 0) {
         if (buff.position() != location) {
            buff.position(location);
         }
      }
   }
   
   public void putTextureCoord(ByteBuffer buff, int location, float[] pos) {
      setLocation(buff, location);
      putTextureCoord(buff, pos);
   }
   
   public void putTextureCoord(ByteBuffer buff, int location, float[] pos, int offset) {
      setLocation(buff, location);
      putTextureCoord(buff, pos, offset);
   }
   
   public void putTextureCoords(ByteBuffer buff, int location, float[] pos) {
      setLocation(buff, location);
      putTextureCoords(buff, pos);
   }
   
   public void putTextureCoords(ByteBuffer buff, int location, float[] pos, int offset, int stride, int count) {
      setLocation(buff, location);
      putTextureCoords(buff, pos, offset, stride, count);
   }
   
   public void putTextureCoords(ByteBuffer buff, int location, int bstride, float[] pos, int offset, int pstride, int count) {
      
      if (pstride <= 0) {
         pstride = 3;
      }
      if (bstride <= 0) {
         bstride = bytesPerTextureCoord();
      }
      for (int i=0; i<count; ++i) {
         setLocation(buff, location+i*bstride);   
         putTextureCoord(buff, pos, offset+i*pstride);   
      }
      
   }
   
   public void putTextureCoords(ByteBuffer buff, int location, Iterable<float[]> positions) {
      setLocation(buff, location);
      putTextureCoords(buff, positions);
   }
   
   public static class FloatTextureCoordBufferPutter extends TextureCoordBufferPutter {

      static FloatTextureCoordBufferPutter instance;
      public static FloatTextureCoordBufferPutter getInstance() {
         if (instance == null) {
            instance = new FloatTextureCoordBufferPutter ();
         }
         return instance;
      }
      
      protected FloatTextureCoordBufferPutter () {}
      
      @Override
      public void putTextureCoord(ByteBuffer buff, float x, float y) {
         buff.putFloat(x);
         buff.putFloat(y);
      }

      @Override
      public int bytesPerTextureCoord() {
         return 2*GLSupport.FLOAT_SIZE;
      }
      
      @Override
      public GL3AttributeStorage storage() {
         return GL3AttributeStorage.FLOAT_2;
      }
      
   }
   
   public static class ShortTextureCoordBufferPutter extends TextureCoordBufferPutter {

      static ShortTextureCoordBufferPutter instance;
      public static ShortTextureCoordBufferPutter getInstance() {
         if (instance == null) {
            instance = new ShortTextureCoordBufferPutter ();
         }
         return instance;
      }
      
      protected ShortTextureCoordBufferPutter () {}
      
      @Override
      public void putTextureCoord(ByteBuffer buff, float x, float y) {
         buff.putShort((short)(x*(Short.MAX_VALUE+0.5)-0.5));
         buff.putShort((short)(y*(Short.MAX_VALUE+0.5)-0.5));
      }

      @Override
      public int bytesPerTextureCoord() {
         return 2*GLSupport.SHORT_SIZE;
      }
      
      @Override
      public GL3AttributeStorage storage() {
         return GL3AttributeStorage.SHORT_N_2;
      }
      
   }
   
   public static TextureCoordBufferPutter getDefault() {
      return ShortTextureCoordBufferPutter.getInstance();
   }
   
}
