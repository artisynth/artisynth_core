package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.render.VertexIndexArray;
import maspack.util.BufferUtilities;

public class GL3SharedVertexIndexArray extends GL3ResourceBase {

   public int lastVersion = -1;
   ElementArray ibo;
   
   public GL3SharedVertexIndexArray(ElementArray ibo) {
      this.ibo = ibo.acquire ();
      lastVersion = -1;
   }
   
   public boolean maybeUpdate(GL3 gl, VertexIndexArray elements) {
      boolean modified = false;
      int version = elements.getVersion ();
      
      if (lastVersion != version) {
         lastVersion = version;
         
         IndexBufferPutter putter = IndexBufferPutter.getDefault ();
         
         int esize = elements.size ();
         ByteBuffer buff = BufferUtilities.newNativeByteBuffer (esize*putter.bytesPerIndex ());
         putter.putIndices (buff, elements.getArray (), 0, 1, esize);
         buff.flip ();
         
         ibo.fill (gl, buff, putter.storage ().getGLType (), 
            putter.storage ().width (),
            esize, buff.limit (), GL.GL_DYNAMIC_DRAW);
         
         buff = BufferUtilities.freeDirectBuffer (buff);
         modified = true;
      }
      
      return modified;
   }
   
   public void bind(GL3 gl) {
      ibo.bind (gl);
   }
   
   public void unbind(GL3 gl) {
      ibo.unbind (gl);
   }
   
   public int count() {
      return ibo.count();
   }
   
   public int type() {
      return ibo.type ();
   }
   
   public int stride() {
      return ibo.stride();
   }

   @Override
   public boolean isValid () {
      if (ibo == null) {
         return false;
      }
      return ibo.isValid ();
   }
   
   @Override
   public void dispose (GL3 gl) {
      if (ibo != null) {
         ibo.releaseDispose (gl);
         ibo = null;
         lastVersion = -1;
      }
   }

   @Override
   public boolean isDisposed () {
      return (ibo == null);
   }
   
   @Override
   public GL3SharedVertexIndexArray acquire () {
      return (GL3SharedVertexIndexArray)super.acquire ();
   }
   
   public static GL3SharedVertexIndexArray generate(GL3 gl) {
      ElementArray ibo = ElementArray.generate (gl);
      GL3SharedVertexIndexArray out = new GL3SharedVertexIndexArray (ibo);
      return out;
   }
   
}
