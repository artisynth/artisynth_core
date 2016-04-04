package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import maspack.render.GL.GLSupport;

/**
 * Generic GL Buffer Object
 */
public class BufferObject extends GL3ResourceBase {
   
   int boId;
   int target;
   int size;
   int usage;
      
   public BufferObject(int target, int vboId) {
      this.target = target;
      this.boId = vboId;
      setInfo(0, 0);
   }
   
   protected void setInfo(int size, int usage) {
      this.size = size;
      this.usage = usage;
   }
   
   /**
    * Dispose of resource
    */
   @Override
   public void dispose(GL3 gl) {
      if (boId > 0) {
         int[] vbo = new int[]{boId};
         gl.glDeleteBuffers(1, vbo, 0);
         boId = -1;
      }
   }
   
   public boolean isValid() {
      return (boId > 0);
   }
   
   public void allocate(GL3 gl, int size, int usage) {
      setInfo(size, usage);
      bind(gl);
      gl.glBufferData(target, size, null, usage);
   }
   
   
   public void fill(GL3 gl, ByteBuffer buff, int usage) {
      fill(gl, buff, buff.limit (), usage);
   }
   
   public void fill(GL3 gl, ByteBuffer buff, int size, int usage) {
      setInfo(size*GLSupport.BYTE_SIZE, usage);
      bind(gl);
      gl.glBufferData(target, size*GLSupport.BYTE_SIZE, buff, usage);
   }
   
   public void update(GL3 gl, byte[] buff) {
      update(gl, ByteBuffer.wrap(buff), 0);
   }
   
   public void update(GL3 gl, byte[] buff, int start) {
      update(gl, ByteBuffer.wrap(buff), start);
   }
   
   public void update(GL3 gl, ByteBuffer buff) {
      update(gl, buff, 0);
   }
   
   public void update(GL3 gl, ByteBuffer buff, int start) {
      update(gl, buff, start, buff.limit ());
   }
   
   public void update(GL3 gl, ByteBuffer buff, int start, int size) {
      bind(gl);
      if (start == 0 && size >= this.size) {
         gl.glBufferData(target, size, null, usage); // orphan
      }
      gl.glBufferSubData(target, start*GLSupport.BYTE_SIZE, size*GLSupport.BYTE_SIZE, buff);
   }
   
   /**
    * Retrieve a mapped buffer to the underlying VBO data
    * @param gl
    * @param access either GL3.GL_WRITE_ONLY, GL3.GL_READ_ONLY, or GL3.GL_READ_WRITE;
    * @return the mapped buffer
    */
   public ByteBuffer mapBuffer(GL3 gl, int access) {
      bind(gl);
      return gl.glMapBuffer(target, access);
   }
   
   /**
    * Maps only a portion of the underlying buffer
    */
   public ByteBuffer mapBufferRange(GL3 gl, int offset, int length, int access) {
      bind(gl);
      return gl.glMapBufferRange(target, offset, length, access);
   }
   
   public void flushBufferRange(GL3 gl, int offset, int length) {
      gl.glFlushMappedBufferRange(target, offset, length);
   }
   
   /**
    * First orphans the original buffer, potentially causing a re-allocation,
    * then returns a mapped buffer ready for writing.
    */
   public ByteBuffer mapNewBuffer(GL3 gl) {
      bind(gl);
      // orphan
      gl.glBufferData(target, size, null, usage);
      return gl.glMapBuffer(target, GL.GL_WRITE_ONLY);
   }
   
   public void unmapBuffer(GL3 gl) {
      gl.glUnmapBuffer(target);
   }
   
   public void bind(GL3 gl) {
      System.out.println ("VBO bound: " + boId + " (target: " + target + ")");
      gl.glBindBuffer(target, boId);
   }
   
   public int getId() {
      return boId;
   }
   
   public int getUsage() {
      return usage;
   }
   
   public int getSize() {
      return size;
   }
   
   @Override
   public BufferObject acquire () {
      return (BufferObject)super.acquire ();
   }
   
   public static BufferObject generate(GL3 gl, int target) {
      int[] vbo = new int[1];
      gl.glGenBuffers (1, vbo, 0);
      return new BufferObject (GL.GL_ARRAY_BUFFER, vbo[0]);
   }
   
}
