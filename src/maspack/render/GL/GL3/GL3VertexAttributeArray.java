package maspack.render.GL.GL3;

import javax.media.opengl.GL3;


public class GL3VertexAttributeArray extends GL3VertexAttribute {

   public static class VBOInfo {
      private int type;              // (gl) type of elements
      private int size;              // (gl) number of elements per attribute instance
      private boolean normalized;    // normalized
      private int offset;            // offset in buffer
      private int stride;            // stride in buffer
      private int count;             // number of attribute instances
      private int divisor;           // instanced rendering divisor
      
      public VBOInfo(int type, int size, boolean normalized, int offset, int stride, int count, int divisor) {
         this.offset = offset;
         this.stride = stride;
         this.type = type;
         this.size = size;
         this.count = count;
         this.divisor = divisor;
         this.normalized = normalized;
      }
      
      public VBOInfo( int type, int size, boolean normalized, int offset, int stride, int count) {
         this(type, size, normalized, offset, stride, count, /*divisor*/ 0);
      }
      
      public int getType() {
         return type;
      }
      
      public int getSize() {
         return size;
      }
      
      public boolean isNormalized() {
         return normalized;
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
      
      public int getDivisor() {
         return divisor;
      }
   }
   
   private BufferObject vbo;            // pointer to VBO
   private VBOInfo info;       // info
   
   public GL3VertexAttributeArray(BufferObject vbo, AttributeInfo attr, VBOInfo info) {
      super(attr);
      this.vbo = vbo;
      this.info = info;
   }
   
   public GL3VertexAttributeArray(BufferObject vbo, AttributeInfo attr, int type, int size, boolean normalized, int offset, int stride, int count, int divisor) {
      this(vbo, attr, new VBOInfo(type, size, normalized, offset, stride, count, divisor));
   }
   
   public GL3VertexAttributeArray(BufferObject vbo, AttributeInfo attr, int type, int size, boolean normalized, int offset, int stride, int count) {
      this(vbo, attr, type, size, normalized, offset, stride, count, /*divisor*/ 0);
   }
   public BufferObject getVBO() {
      return vbo;
   }
   
   public VBOInfo getVBOInfo() {
      return info;
   }
   
   public int getType() {
      return info.type;
   }
   
   public int getSize() {
      return info.size;
   }
   
   public boolean isNormalized() {
      return info.normalized;
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
   
   public int getDivisor() {
      return info.divisor;
   }

   @Override
   public void bind(GL3 gl) {
      vbo.bind(gl);
      gl.glEnableVertexAttribArray(getAttributeIndex());  // enable attribute
      gl.glVertexAttribPointer(getAttributeIndex(), info.size, info.type,
         info.normalized, info.stride, info.offset);
      if (info.divisor > 0) {
         gl.glVertexAttribDivisor(getAttributeIndex(), info.divisor);
      }
   }
   
}
