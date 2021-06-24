package maspack.render;

import maspack.render.GL.GLSupport.GLVersionInfo;
import maspack.render.GL.GLSupport;

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

   /**
    * Check if a specific graphics interface is supported on the hardware and
    * if not try to substitute on that is.
    */
   public static GraphicsInterface checkAvailability (GraphicsInterface gi) {
      if (gi == GraphicsInterface.GL3) {
         GLVersionInfo vinfo = GLSupport.getMaxGLVersionSupported();
         if ( (vinfo.getMajorVersion() < gi.getMajorVersion()) ||
              ((vinfo.getMajorVersion() == gi.getMajorVersion()) && 
               (vinfo.getMinorVersion() < gi.getMinorVersion()))) {
            System.err.println(
               "WARNING: " + gi + " graphics not supported on this system.");
            System.err.println(
               "     Required: OpenGL "+gi.getMajorVersion()+
               "."+gi.getMinorVersion());
            System.err.println(
               "     Using GL2 instead");
            return GraphicsInterface.GL2;
         }
      }
      // default - assume OK
      return gi;
   }

}
