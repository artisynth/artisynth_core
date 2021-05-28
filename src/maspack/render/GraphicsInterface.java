package maspack.render;

/**
 * Describes the graphics interface used to supprt the rendering.
 */
public enum GraphicsInterface {

   /**
    * OpenGL, version 2.1
    */
   GL2(2,1),

   /**
    * OpenGL, version 3.3
    */
   GL3(3,3);

   int major;
   int minor;
      
   GraphicsInterface (int major, int minor) {
      this.major = major;
      this.minor = minor;
   }
   
   public int getMajorVersion() {
      return major;
   }
   
   public int getMinorVersion() {
      return minor;
   }
}
