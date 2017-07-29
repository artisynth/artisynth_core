package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

public class GL3ElementAttributeArray {
   
   public static class IBOInfo {
      int type;
      int offset;
      int stride;
      int count;
      
      public IBOInfo(int type, int offset, int stride, int count) {
         this.type = type;
         this.offset = offset;
         this.stride = stride;
         this.count = count;
      }
      
      public int getType() {
         return type;
      }
      
      public int getOffset() {
         return offset;
      }
      
      public int getStride() {
         return stride;
      }
      
      public int getCount() {
         return count;
      }
   }
   
   IndexBufferObject ibo;  // pointer to buffer object
   IBOInfo info;
   
   public GL3ElementAttributeArray(IndexBufferObject ibo, IBOInfo info) {
      this.ibo = ibo;
      this.info = info;
   }
   
   public GL3ElementAttributeArray(IndexBufferObject ibo, int type, int offset, int stride, int count) {
      this(ibo, new IBOInfo(type, offset, stride, count));
   }
   
   public IndexBufferObject getIBO() {
      return ibo;
   }
   
   public int getType() {
      return info.type;
   }
   
   public int getOffset() {
      return info.offset;
   }
   
   public int getStride() {
      return info.stride;
   }
   
   public int getCount() {
      return info.count;
   }

   public void bind(GL3 gl) {
      ibo.bind(gl);
   }
   
   public boolean isValid() {
      return ibo.isValid ();
   }
}
