package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

public class GL3VertexAttributeArrayInfo {

   GL3VertexAttributeInfo attribute;     // attribute
   private GL3AttributeStorage storage;  // type and info
   private int offset;            // offset in buffer
   private int stride;            // stride in buffer
   private int count;             // number of attribute instances
   private int divisor;           // instanced rendering divisor

   public GL3VertexAttributeArrayInfo( String name, int location,
      GL3AttributeStorage storage, int offset, int stride, int count, int divisor) {
      this(new GL3VertexAttributeInfo (name, location), storage, offset, stride, count, divisor);
      
   }

   public GL3VertexAttributeArrayInfo( GL3VertexAttributeInfo attribute,
      GL3AttributeStorage storage, int offset, int stride, int count, int divisor) {
      
      this.attribute = attribute;
      this.storage = storage;
      this.offset = offset;
      this.stride = stride;
      this.count = count;
      this.divisor = divisor;
      
   }
   
   public GL3VertexAttributeArrayInfo( GL3VertexAttributeInfo info, 
      GL3AttributeStorage storage, int offset, int stride, int count) {
      this(info.name, info.location, storage, offset, stride, count, /*divisor*/ 0);
   }
   
   public void setStorage(GL3AttributeStorage storage) {
      this.storage = storage;
   }

   public int getType() {
      return storage.getGLType ();
   }

   public int getSize() {
      return storage.size ();
   }

   public boolean isNormalized() {
      return storage.isNormalized ();
   }

   public int getOffset() {
      return offset;
   }
   
   public void setOffset(int o) {
      offset = o;
   }

   public int getStride() {
      return stride;
   }
   
   public void setStride(int s) {
      stride = s;
   }

   public int getCount() {
      return count;
   }
   
   public void setCount(int c) {
      count = c;
   }

   public int getDivisor() {
      return divisor;
   }
   
   public void setDivisor(int d) {
      divisor = d;
   }
   
   public GL3VertexAttributeInfo getAttributeInfo() {
      return attribute;
   }

   public void bind(GL3 gl, int loc) {
      if (loc >= 0) {
         gl.glEnableVertexAttribArray(loc);  // enable attribute
         gl.glVertexAttribPointer(loc, storage.size (), storage.getGLType(),
            storage.isNormalized (), stride, offset);
         if (divisor > 0) {
            gl.glVertexAttribDivisor(loc, divisor);
         }
      } else {
         gl.glDisableVertexAttribArray (loc);
      }
   }
   
   public void bind(GL3 gl) {
      bind(gl, attribute.getLocation ());
   }
   
   public void unbind(GL3 gl, int loc) {
      gl.glDisableVertexAttribArray (loc);
   }
}
