package maspack.render.GL.GL3;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.jogamp.opengl.GL3;

import maspack.render.GL.GLSupport;

/**
 * Debugging wrapper for a GL3 object. Every OpenGL call ({@code gl*}
 * method) is printed together with its arguments, then passed on to the
 * wrapped object, after which (if error checking is enabled) any OpenGL
 * error is printed. Other methods are passed on and error checked without
 * being printed.
 *
 * <p>The wrapper is a dynamic proxy, created by {@link #wrap}, rather than a
 * class implementing GL3 explicitly. GL3 has well over a thousand methods
 * and changes between JOGL versions (JOGL 2.6 added over 400 and removed
 * some), so an explicit implementation compiles against only one JOGL
 * version, whereas the proxy works with any.
 */
public class GL3Debug implements InvocationHandler {

   GL3 gl;
   boolean checkForErrors;

   public GL3Debug (GL3 gl) {
      this.gl = gl;
      checkForErrors = true;
   }

   /**
    * Creates a debugging wrapper for a GL3 object.
    *
    * @param gl object to wrap
    * @return GL3 object which prints and error checks calls to {@code gl}
    */
   public static GL3 wrap (GL3 gl) {
      return (GL3)Proxy.newProxyInstance (
         GL3.class.getClassLoader(), new Class<?>[] { GL3.class },
         new GL3Debug (gl));
   }

   /**
    * Returns the debugging handler of a GL3 object created by {@link
    * #wrap}, or {@code null} if it was not created that way.
    */
   public static GL3Debug getHandler (GL3 gl) {
      if (Proxy.isProxyClass (gl.getClass())) {
         InvocationHandler handler = Proxy.getInvocationHandler (gl);
         if (handler instanceof GL3Debug) {
            return (GL3Debug)handler;
         }
      }
      return null;
   }

   public void setCheckErrors (boolean set) {
      checkForErrors = set;
   }

   public boolean getCheckErrors() {
      return checkForErrors;
   }

   private static String argsToString (Object[] args) {
      StringBuilder sb = new StringBuilder();
      if (args != null) {
         for (int i=0; i<args.length; i++) {
            if (i > 0) {
               sb.append (',');
            }
            sb.append (args[i]);
         }
      }
      return sb.toString();
   }

   @Override
   public Object invoke (Object proxy, Method method, Object[] args)
      throws Throwable {

      // Object methods refer to the proxy itself
      if (method.getDeclaringClass() == Object.class) {
         switch (method.getName()) {
            case "equals":
               return proxy == args[0];
            case "hashCode":
               return System.identityHashCode (proxy);
            case "toString":
               return "GL3Debug[" + gl + "]";
            default:
               break;
         }
      }
      String name = method.getName();
      if (name.startsWith ("gl")) {
         System.out.println (name + "(" + argsToString (args) + ")");
      }
      Object out;
      try {
         out = method.invoke (gl, args);
      }
      catch (InvocationTargetException e) {
         throw e.getCause();
      }
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError (gl);
      }
      return out;
   }
}
