package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import maspack.render.GL.GLSupport;
import maspack.render.GL.GL3.NormalBufferPutter.ShortNormalBufferPutter;

public abstract class ColorBufferPutter {

   protected ColorBufferPutter() {}
   public abstract void putColor(ByteBuffer buff, byte r, byte g, byte b, byte a);
   public abstract int bytesPerColor();
   public abstract GL3AttributeStorage storage();
   
   public void putColor(ByteBuffer buff, byte[] color) {
      putColor(buff, color[0], color[1], color[2], color[3]);
   }
   
   public void putColor(ByteBuffer buff, byte[] color, int offset) {
      putColor(buff, color[offset], color[offset+1], color[offset+2], color[offset+3]);
   }
   
   public void putColor(ByteBuffer buff, float[] color, int offset) {
      byte r = (byte)(color[offset++]*255);
      byte g = (byte)(color[offset++]*255);
      byte b = (byte)(color[offset++]*255);
      byte a = (byte)(color[offset]*255);
      putColor(buff, r,g,b,a);
   }
   
   public void putColors(ByteBuffer buff, byte[] colors) {
      for (int i=0; i<colors.length-2; i+=3) {
         putColor(buff, colors, i);
      }
   }
   
   public void putColors(ByteBuffer buff, byte[] colors, int offset, int stride, int count) {
      if (stride <= 0) {
         stride = 3;
      }
      int idx = offset;
      for (int i=0; i<count; ++i) {
         putColor(buff, colors, idx);
         idx += stride;
      }  
   }
   
   public void putColors(ByteBuffer buff, Iterable<byte[]> colors) {
      for (byte[] color : colors) {
         putColor(buff, color);
      }
   }
  
   private void setLocation(ByteBuffer buff, int location) {
      if (location >= 0) {
         if (buff.position() != location) {
            buff.position(location);
         }
      }
   }
   
   public void putColor(ByteBuffer buff, int location, byte[] color) {
      setLocation(buff, location);
      putColor(buff, color);
   }
   
   public void putColor(ByteBuffer buff, int location, byte[] color, int offset) {
      setLocation(buff, location);
      putColor(buff, color, offset);
   }
   
   public void putColors(ByteBuffer buff, int location, byte[] colors) {
      setLocation(buff, location);
      putColors(buff, colors);
   }
   
   public void putColors(ByteBuffer buff, int location, byte[] colors, int offset, int stride, int count) {
      setLocation(buff, location);
      putColors(buff, colors, offset, stride, count);
   }
   
   public void putColors(ByteBuffer buff, int location, int bstride, byte[] colors, int offset, int pstride, int count) {
      
      if (pstride <= 0) {
         pstride = 3;
      }
      if (bstride <= 0) {
         bstride = bytesPerColor();
      }
      for (int i=0; i<count; ++i) {
         setLocation(buff, location+i*bstride);   
         putColor(buff, colors, offset+i*pstride);   
      }
      
   }
   
   public void putColors(ByteBuffer buff, int location, Iterable<byte[]> colors) {
      setLocation(buff, location);
      putColors(buff, colors);
   }
   
   public static class ByteColorBufferPutter extends ColorBufferPutter {

      static ByteColorBufferPutter instance;
      public static ByteColorBufferPutter getInstance() {
         if (instance == null) {
            instance = new ByteColorBufferPutter ();
         }
         return instance;
      }
      
      protected ByteColorBufferPutter () {}
      
      @Override
      public void putColor(ByteBuffer buff, byte r, byte g, byte b, byte a) {
         buff.put(r);
         buff.put(g);
         buff.put(b);
         buff.put(a);
      }

      @Override
      public int bytesPerColor() {
         return 4*GLSupport.BYTE_SIZE;
      }

      @Override
      public GL3AttributeStorage storage() {
         return GL3AttributeStorage.UBYTE_N_4;
      }
      
   }
   
   public static ColorBufferPutter getDefault() {
      return ByteColorBufferPutter.getInstance ();
   }
   
}
