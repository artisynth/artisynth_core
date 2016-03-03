package maspack.render.GL.GL3;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

public class UniformBufferObject extends GL3ResourceBase {
   
   int ubo;
   int usage;
   String blockName;
   int blockSize;
   
   String[] attributes;
   int[] offsets;
   
   public UniformBufferObject(GL3 gl, int progId, String blockName, String[] attributes, int usage) {
      
      this.blockName = blockName;
      this.attributes = attributes;
      this.offsets = new int[attributes.length];
      this.usage = usage;
      
      init(gl, progId);
     
   }
   
   public int getOffset(int attribId) {
      return offsets[attribId];
   }
   
   public int getSize() {
      return blockSize;
   }
   
   public void bindLocation(GL3 gl, int program, int location) {
      gl.glUseProgram(program);
      int ubidx = gl.glGetUniformBlockIndex(program,  blockName);
      // only bind if present (may have been optimized out if not used)
      if (ubidx != GL3.GL_INVALID_INDEX) {
         gl.glUniformBlockBinding(program, ubidx, location);
         gl.glBindBufferBase(GL3.GL_UNIFORM_BUFFER, location, ubo);  
      }
      gl.glUseProgram(0);
   }
   
   public void bind(GL3 gl) {
      gl.glBindBuffer(GL3.GL_UNIFORM_BUFFER, ubo);
   }
   
   public void unbind(GL3 gl) {
      gl.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
   }
   
   public void update(GL3 gl, Buffer data) {
      bind(gl);
      // orphan
      gl.glBufferData(GL3.GL_UNIFORM_BUFFER, blockSize, null, usage);
      gl.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, blockSize, data);
      unbind(gl);
   }
   
   public void update(GL3 gl, ByteBuffer data, int offset) {
      update(gl, data, offset, data.limit());
   }
   
   public void update(GL3 gl, ByteBuffer data, int offset, int size) {
      bind(gl);
      gl.glBufferSubData(GL3.GL_UNIFORM_BUFFER, offset, data.limit(), data);
      unbind(gl);
   }
   
   /**
    * Retrieve a mapped buffer to the underlying VBO data
    * @param gl
    * @param access either GL3.GL_WRITE_ONLY, GL3.GL_READ_ONLY, or GL3.GL_READ_WRITE;
    * @return the mapped buffer
    */
   public ByteBuffer mapBuffer(GL3 gl, int access) {
      gl.glBindBuffer(GL3.GL_UNIFORM_BUFFER, ubo);
      return gl.glMapBuffer(GL3.GL_UNIFORM_BUFFER, access);
   }
   
   /**
    * Maps only a portion of the underlying buffer
    */
   public ByteBuffer mapBufferRange(GL3 gl, int offset, int length, int access) {
      gl.glBindBuffer(GL3.GL_UNIFORM_BUFFER, ubo);
      return gl.glMapBufferRange(GL3.GL_UNIFORM_BUFFER, offset, length, access);
   }
   
   public void flushBufferRange(GL3 gl, int offset, int length) {
      gl.glFlushMappedBufferRange(GL3.GL_UNIFORM_BUFFER, offset, length);
   }
   
   /**
    * First orphans the original buffer, potentially causing a re-allocation,
    * then returns a mapped buffer ready for writing.
    */
   public ByteBuffer mapNewBuffer(GL3 gl) {
      gl.glBindBuffer(GL3.GL_UNIFORM_BUFFER, ubo);
      // orphan
      gl.glBufferData(GL3.GL_UNIFORM_BUFFER, blockSize, null, usage);
      return gl.glMapBuffer(GL3.GL_UNIFORM_BUFFER, GL.GL_WRITE_ONLY);
   }
   
   public void unmapBuffer(GL3 gl) {
      gl.glUnmapBuffer(GL3.GL_UNIFORM_BUFFER);
   }
   
   public void create(GL3 gl, Buffer data, int usage) {
      this.usage = usage;
      bind(gl);
      gl.glBufferData(GL3.GL_UNIFORM_BUFFER, blockSize, data, usage);
      unbind(gl);
   }
   
   @Override
   public void init(GL3 gl) {
      init(gl, -1);  // check in first program?
   }
   
   public void init(GL3 gl, int progId) {
   
      int[] indices = new int[attributes.length];
      int buff[] = new int[1]; 
      
      if (progId > 0) {
         // retrieve block size and offsets
         int blockIndex = gl.glGetUniformBlockIndex(progId,  blockName);
         if (blockIndex != GL3.GL_INVALID_INDEX) {
            gl.glGetActiveUniformBlockiv(progId, blockIndex, GL3.GL_UNIFORM_BLOCK_DATA_SIZE, buff, 0);
            this.blockSize = buff[0];
            gl.glGetUniformIndices(progId, attributes.length, attributes, indices, 0);
            gl.glGetActiveUniformsiv(progId, attributes.length, indices, 0, GL3.GL_UNIFORM_OFFSET, offsets, 0);
         } else {
            System.err.println("Could not initialize UBO, does not seem to be present in the given program.");
         }
      }
      
      // generate handle
      gl.glGenBuffers(1, buff, 0);
      ubo = buff[0];
      
      // initialize storage
      create(gl, null, usage);
   }
   
   /**
    * Should be called to safely free memory before discarding
    */
   public void dispose(GL3 gl) {
      // invalidate data then delete buffer
      // gl.glInvalidateBufferData(ubo);
      int buff[] = new int[1];
      buff[0] = ubo;
      gl.glDeleteBuffers(1, buff, 0);
      ubo = -1;
   }
   
   public boolean isValid() {
      return (ubo >= 0);
   }
   
   
}
