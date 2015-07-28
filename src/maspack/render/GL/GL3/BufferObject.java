package maspack.render.GL.GL3;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

/**
 * Generic GL Buffer Object
 */

public class BufferObject extends GL3ResourceBase {
   
   int boId;
   int target;
   int size;
   int usage;
   
   public BufferObject(GL3 gl) {
      boId = -1;
      init(gl);
      setInfo(0, 0, 0);
   }
   
   public BufferObject(int vboId) {
      this.boId = vboId;
      setInfo(0, 0, 0);
   }
   
   protected void setInfo(int target, int size, int usage) {
      this.target = target;
      this.size = size;
      this.usage = usage;
   }
  
   @Override
   public void init(GL3 gl) {
      if (boId <= 0) {
         int[] vbo = new int[1];
         gl.glGenBuffers(1, vbo, 0);
         boId = vbo[0];
      }
   }
   
   /**
    * Dispose of resource
    */
   public void dispose(GL3 gl) {
      if (boId > 0) {
         int[] vbo = new int[]{boId};
         gl.glDeleteBuffers(1, vbo, 0);
         boId = -1;
      }
   }
   
   @Override
   public boolean isValid() {
      return (boId > 0);
   }
   
   public void allocate(GL3 gl, int target, int size, int usage) {
      setInfo(target, size, usage);
      gl.glBindBuffer(target, boId);
      gl.glBufferData(target, size, null, usage);
   }
   
   public void fill(GL3 gl, float[] buff, int target, int usage) {
      fill(gl, FloatBuffer.wrap(buff), target, usage);
   }
   
   public void fill(GL3 gl, FloatBuffer buff, int target, int usage) {
      setInfo(target, buff.limit()*Float.BYTES, usage);
      gl.glBindBuffer(target, boId);
      gl.glBufferData(target, buff.limit()*Float.BYTES, buff, usage);
   }
   
   public void update(GL3 gl, float[] buff) {
      update(gl, FloatBuffer.wrap(buff), 0);
   }
   
   public void update(GL3 gl, float[] buff, int start) {
      update(gl, FloatBuffer.wrap(buff), start);
   }
   
   public void update(GL3 gl, FloatBuffer buff) {
      gl.glBufferData(target, size, null, usage);
      update(gl, buff, 0);
   }
   
   public void update(GL3 gl, FloatBuffer buff, int start) {
      gl.glBindBuffer(target, boId);
      gl.glBufferSubData(target, start*Float.BYTES, buff.limit()*Float.BYTES, buff);
   }
   
   public void fill(GL3 gl, int[] buff, int target, int usage) {
      fill(gl, IntBuffer.wrap(buff), target, usage);
   }
   
   public void fill(GL3 gl, IntBuffer buff, int target, int usage) {
      setInfo(target, buff.limit()*Integer.BYTES, usage);
      gl.glBindBuffer(target, boId);
      gl.glBufferData(target, buff.limit()*Integer.BYTES, buff, usage);
   }
   
   public void update(GL3 gl, int[] buff) {
      update(gl, IntBuffer.wrap(buff), 0);
   }
   
   public void update(GL3 gl, int[] buff, int start) {
      update(gl, IntBuffer.wrap(buff), start);
   }
   
   public void update(GL3 gl, IntBuffer buff) {
      update(gl, buff, 0);
   }
   
   public void update(GL3 gl, IntBuffer buff, int start) {
      gl.glBindBuffer(target, boId);
      gl.glBufferSubData(target, start*Integer.BYTES, buff.limit()*Integer.BYTES, buff);
   }
   
   public void fill(GL3 gl, short[] buff, int target, int usage) {
      fill(gl, ShortBuffer.wrap(buff), target, usage);
   }
   
   public void fill(GL3 gl, ShortBuffer buff, int target, int usage) {
      setInfo(target, buff.limit()*Short.BYTES, usage);
      gl.glBindBuffer(target, boId);
      gl.glBufferData(target, buff.limit()*Short.BYTES, buff, usage);
   }
   
   public void update(GL3 gl, short[] buff) {
      update(gl, ShortBuffer.wrap(buff), 0);
   }
   
   public void update(GL3 gl, short[] buff, int start) {
      update(gl, ShortBuffer.wrap(buff), start);
   }
   
   public void update(GL3 gl, ShortBuffer buff) {
      update(gl, buff, 0);
   }
   
   public void update(GL3 gl, ShortBuffer buff, int start) {
      gl.glBindBuffer(target, boId);
      gl.glBufferSubData(target, start*Short.BYTES, buff.limit()*Short.BYTES, buff);
   }
   
   public void fill(GL3 gl, byte[] buff, int target, int usage) {
      fill(gl, ByteBuffer.wrap(buff), target, usage);
   }
   
   public void fill(GL3 gl, ByteBuffer buff, int target, int usage) {
      setInfo(target, buff.limit()*Byte.BYTES, usage);
      gl.glBindBuffer(target, boId);
      gl.glBufferData(target, buff.limit()*Byte.BYTES, buff, usage);
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
      gl.glBindBuffer(target, boId);
      if (start == 0 && buff.limit() == size) {
         gl.glBufferData(target, size, null, usage); // orphan
      }
      gl.glBufferSubData(target, start*Byte.BYTES, buff.limit()*Byte.BYTES, buff);
   }
   
   /**
    * Retrieve a mapped buffer to the underlying VBO data
    * @param gl
    * @param access either GL3.GL_WRITE_ONLY, GL3.GL_READ_ONLY, or GL3.GL_READ_WRITE;
    * @return the mapped buffer
    */
   public ByteBuffer mapBuffer(GL3 gl, int access) {
      gl.glBindBuffer(target, boId);
      return gl.glMapBuffer(target, access);
   }
   
   /**
    * Maps only a portion of the underlying buffer
    */
   public ByteBuffer mapBufferRange(GL3 gl, int offset, int length, int access) {
      gl.glBindBuffer(target, boId);
      return gl.glMapBufferRange(target, offset, length, access);
   }
   
   public void flushBufferRange(GL3 gl, int offset, int length) {
      gl.glFlushMappedBufferRange(target, offset, length);
   }
   
   /**
    * First orphans the original buffer, potentially causing a re-allocation,
    * then returns a mapped buffer ready for writing.
    * @return
    */
   public ByteBuffer mapNewBuffer(GL3 gl) {
      gl.glBindBuffer(target, boId);
      // orphan
      gl.glBufferData(target, size, null, usage);
      return gl.glMapBuffer(target, GL.GL_WRITE_ONLY);
   }
   
   public void unmapBuffer(GL3 gl) {
      gl.glUnmapBuffer(target);
   }
   
   public void bind(GL3 gl) {
      gl.glBindBuffer(target, boId);
   }
   
   public int getId() {
      return boId;
   }
   
   public int getUsage() {
      return usage;
   }
   
}
