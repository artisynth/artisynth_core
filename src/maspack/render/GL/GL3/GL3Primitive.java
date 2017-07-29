package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

import maspack.render.GL.GL3.GL3SharedPrimitive.PrimitiveKey;

/**
 * Standard VAO-based primitive, CANNOT be shared between multiple contexts
 */
public class GL3Primitive extends GL3Object {
   
   /**
    * VAO should either already have attributes bound, or be sure
    * to call {@link #bind(GL3)} before drawing
    * @param vao vertex array object
    * @param glo potentially shared object
    */
   private GL3Primitive(VertexArrayObject vao, GL3SharedPrimitive glo) {
      super(vao, glo);
   }
   
   public PrimitiveKey getKey() {
      return ((GL3SharedPrimitive)getShared ()).getKey();
   }
   
   public boolean matches(PrimitiveKey key) {
      return ((GL3SharedPrimitive)getShared()).matches (key);
   }
 
   @Override
   public GL3Primitive acquire () {
      return (GL3Primitive)super.acquire ();
   }
   
   public static GL3Primitive generate(GL3 gl, GL3SharedPrimitive glo) {
      VertexArrayObject vao = VertexArrayObject.generate (gl);
      GL3Primitive out = new GL3Primitive (vao, glo);
      out.bind (gl);
      return out;
   }
   
}
