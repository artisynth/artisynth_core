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

   /**
    * Like valueOf(), except if the string is not valid then the method simply
    * returns {@code null} instead of throwing an exception.
    */
   static public GraphicsInterface fromString (String str) {
      GraphicsInterface gi = null;
      try {
         gi = valueOf (str);
      }
      catch (Exception e) {
         // ignore
      }
      return gi;
   }
}
