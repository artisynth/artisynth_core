package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

public class GL3Utilities {

   public static boolean debug = false;
   
   public static GL3 wrap(GL3 gl) {
      if (debug) {
         return new GL3Debug(gl);
      }
      return gl;
   }
   
   /**
    * For buffer objects, attributes need to be aligned to multiples of 4-bytes.  This returns the 
    * required alignment width provided a true byte width (i.e. rounds up to nearest 4 bytes)
    * @param byteWidth width in bytes of attribute
    * @return properly aligned width
    */
   public static int getAlignedWidth(int byteWidth) {
      return (byteWidth+3) & 0xFFFFFFFC;
   }
   
   /**
    * Enables a vertex attribute pointer based on a given storage, correctly handling matrix types
    * (since matrices occupy multiple attribute locations)
    * @param gl active context
    * @param loc attribute location
    * @param storage attribute storage
    * @param stride attribute stride in VBO
    * @param offset attribute offset in VBO
    */
   public static void activateVertexAttribute(GL3 gl, int loc, GL3AttributeStorage storage, 
      int stride, int offset, int divisor) {
      
      if (storage.isMatrix()) {
         for (int i=0; i<storage.cols(); ++i) {
            int cloc = loc+i;
            gl.glEnableVertexAttribArray(cloc);
            gl.glVertexAttribPointer(cloc, storage.cols(), storage.getGLType(), 
               storage.isNormalized(), stride, offset+i*storage.colWidth());
            if (divisor > 0) {
               gl.glVertexAttribDivisor(cloc, divisor);
            }
         }
      } else {
         gl.glEnableVertexAttribArray(loc);
         gl.glVertexAttribPointer(loc, storage.size(), storage.getGLType(), storage.isNormalized(), stride, offset);
         if (divisor > 0) {
            gl.glVertexAttribDivisor(loc, divisor);
         }
      }
   }
   
   /**
    * Enables a vertex attribute pointer based on a given storage, correctly handling matrix types
    * (since matrices occupy multiple attribute locations)
    * @param gl active context
    * @param loc attribute location
    * @param storage attribute storage
    * @param stride attribute stride in VBO
    * @param offset attribute offset in VBO
    */
   public static void activateVertexAttribute(GL3 gl, int loc, GL3AttributeStorage storage, 
      int stride, int offset) {
      
      if (storage.isMatrix()) {
         for (int i=0; i<storage.rows(); ++i) {
            int rloc = loc+i;
            gl.glEnableVertexAttribArray(rloc);
            gl.glVertexAttribPointer(rloc, storage.cols(), storage.getGLType(), 
               storage.isNormalized(), stride, offset+i*storage.colWidth());
         }
      } else {
         gl.glEnableVertexAttribArray(loc);
         gl.glVertexAttribPointer(loc, storage.size(), storage.getGLType(), storage.isNormalized(), stride, offset);
      }
   }
   
   /**
    * Enables a vertex attribute pointer based on a given storage, correctly
    * handling matrix types (since matrices occupy multiple attribute
    * locations)
    * @param gl active context
    * @param loc attribute location
    */
   public static void deactivateVertexAttribute(GL3 gl, int loc) {
      gl.glDisableVertexAttribArray(loc);
   }
   
   public static int getGLType(GL3AttributeStorage.StorageType type) {
      switch(type) {
         case FLOAT:
            return GL3.GL_FLOAT;
         case SIGNED_2_10_10_10_REV:
            return GL3.GL_INT_2_10_10_10_REV;
         case SIGNED_BYTE:
            return GL3.GL_BYTE;
         case SIGNED_INT:
            return GL3.GL_INT;
         case SIGNED_SHORT:
            return GL3.GL_SHORT;
         case UNSIGNED_2_10_10_10_REV:
            return GL3.GL_UNSIGNED_INT_2_10_10_10_REV;
         case UNSIGNED_BYTE:
            return GL3.GL_UNSIGNED_BYTE;
         case UNSIGNED_INT:
            return GL3.GL_UNSIGNED_INT;
         case UNSIGNED_SHORT:
            return GL3.GL_UNSIGNED_SHORT;
         default:
            return -1;
         
      }
   }
   
}
