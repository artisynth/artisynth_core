package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL3;

import maspack.util.BufferUtilities;

public class UniformBufferObject extends BufferObject {
   
   private String blockName;
   private int blockSize;
   
   private String[] attributes;
   private int[] offsets;
   
   private ByteBuffer buff; // used for assigning stuff, must be obtained through getBuffer()
   
   protected UniformBufferObject(GL3 gl, int progId, String blockName, String[] attributes, int usage) {
      super(GL3.GL_UNIFORM_BUFFER, 0);
      
      this.offsets = new int[attributes.length];
      this.attributes = attributes;
      this.blockName = blockName;
      
      int[] indices = new int[attributes.length];
      int val[] = new int[1]; 
      
      if (progId > 0) {
         // retrieve block size and offsets
         int blockIndex = gl.glGetUniformBlockIndex(progId,  blockName);
         if (blockIndex != GL3.GL_INVALID_INDEX) {
            gl.glGetActiveUniformBlockiv(progId, blockIndex, GL3.GL_UNIFORM_BLOCK_DATA_SIZE, val, 0);
            blockSize = val[0];
            gl.glGetUniformIndices(progId, attributes.length, attributes, indices, 0);
            gl.glGetActiveUniformsiv(progId, attributes.length, indices, 0, GL3.GL_UNIFORM_OFFSET, offsets, 0);
         } else {
            System.err.println("Could not initialize UBO, does not seem to be present in the given program.");
         }
      }
      
      // generate handle
      gl.glGenBuffers(1, val, 0);
      int uboId = val[0];
      this.boId = uboId;   // set id
      
      // initialize storage
      fill (gl, null, blockSize, usage);
      
      buff = BufferUtilities.newNativeByteBuffer (blockSize);
   }
   
   protected UniformBufferObject(int ubo, String blockName, int blockSize, String[] attributes, 
      int[] offsets, int usage) {
      super(GL3.GL_UNIFORM_BUFFER, ubo);
      
      this.blockName = blockName;
      this.blockSize = blockSize;
      this.attributes = attributes;
      this.offsets = offsets;
      this.usage = usage;
      
      buff = BufferUtilities.newNativeByteBuffer (blockSize);
      
   }
   
   protected void setInfo(int size, int usage) {
      this.size = blockSize;  // don't change size
      this.usage = usage;
   }
   
   public String getBlockName() {
      return blockName;
   }
   
   public int getByteOffset(int attribId) {
      return offsets[attribId];
   }
   
   public String getAttribute(int attribId) {
      return attributes[attribId];
   }
   
   public int numAttributes() {
      return attributes.length;
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
         gl.glBindBufferBase(GL3.GL_UNIFORM_BUFFER, location, getId ());  
      }
      gl.glUseProgram(0);
   }
   
   public void set(GL3 gl, ByteBuffer data) {
      fill(gl, data);
   }
   
   public void update(GL3 gl, ByteBuffer data) {
      set(gl, data);
   }
   
   public void update(GL3 gl, ByteBuffer data, int offset, int size) {
      bind(gl);
      gl.glBufferSubData(GL3.GL_UNIFORM_BUFFER, offset, size, data);
      unbind(gl);
   }
   
   public ByteBuffer getBuffer() {
      buff.clear();
      return buff;
   }
  
   /**
    * Should be called to safely free memory before discarding
    */
   @Override
   public void dispose(GL3 gl) {
      // invalidate data then delete buffer
      // gl.glInvalidateBufferData(ubo);
      if (!isDisposed()) {
         super.dispose (gl);
         BufferUtilities.freeDirectBuffer (this.buff);
         this.buff = null;
      }
   }
   
   @Override
   public UniformBufferObject acquire () {
      return (UniformBufferObject)super.acquire ();
   }
   
   public static UniformBufferObject generate(GL3 gl, int progId, String blockName, String[] attributes, int usage) {
      UniformBufferObject ubo = new UniformBufferObject (gl, progId, blockName, attributes, usage);
      return ubo;
   }
   
   
}
