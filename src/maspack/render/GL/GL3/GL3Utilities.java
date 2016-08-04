package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

public class GL3Utilities {

   public static boolean debug = false;
   
   public static GL3 wrap(GL3 gl) {
      if (debug) {
         return new GL3Debug(gl);
      }
      return gl;
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
