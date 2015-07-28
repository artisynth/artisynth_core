package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

public class GL3ElementAttributeArray {
   
   public static class ElementInfo {
      int type;
      int offset;
      int stride;
      int count;
      
      public ElementInfo(int type, int offset, int stride, int count) {
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
   
   BufferObject ibo;  // pointer to buffer object
   ElementInfo info;
   
   public GL3ElementAttributeArray(BufferObject ibo, ElementInfo info) {
      this.ibo = ibo;
      this.info = info;
   }
   
   public GL3ElementAttributeArray(BufferObject ibo, int type, int offset, int stride, int count) {
      this(ibo, new ElementInfo(type, offset, stride, count));
   }
   
   public BufferObject getIBO() {
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
}
