package maspack.render.GL;

import java.util.HashMap;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;

public class GLShaderProgram extends GLResourceBase {

   int id;
   HashMap<String,Integer> attributeMap;
   HashMap<String,Integer> uniformMap;
   HashMap<String,Integer> uniformBlockMap;
   HashMap<String,Integer> samplerMap;
   
   public GLShaderProgram(int id) {
      this.id = id;
      attributeMap = new HashMap<> ();
      uniformMap = new HashMap<> ();
      uniformBlockMap = new HashMap<> ();
      samplerMap = new HashMap<> ();
   }
   
   public int getId() {
      return id;
   }
   
   public int getAttributeLocation(GL2GL3 gl, String name) {
      Integer l = attributeMap.get (name);
      if (l == null) {
         l = gl.glGetAttribLocation (id, name);
         if (l != null) {
            attributeMap.put (name, l);
         }
      }
      return l;
   }
   
   public int getUniformLocation(GL2GL3 gl, String name) {
      Integer l = uniformMap.get (name);
      if (l == null) {
         l = gl.glGetUniformLocation (id, name);
         if (l != null) {
            uniformMap.put (name, l);
         }
      }
      return l;
   }
   
   public int getUniformBlockIndex(GL2GL3 gl, String name) {
      Integer l = uniformBlockMap.get (name);
      if (l == null) {
         l = gl.glGetUniformBlockIndex (id, name);
         if (l != null) {
            uniformBlockMap.put (name, l);
         }
      }
      return l;
   }
   
   public int getSamplerIndex(GL2GL3 gl, String name) {
      Integer idx = samplerMap.get (name);
      if (idx == null) {
         int l = getUniformLocation (gl, name);
         int[] buff = new int[1];
         gl.glGetUniformiv (id, l, buff, 0);
         idx = buff[0];
         samplerMap.put (name, idx);
      }
      return idx;
   }
   
   public void use(GL2GL3 gl) {
      gl.glUseProgram (id);
   }
   
   public void unuse(GL2GL3 gl) {
      gl.glUseProgram (0);
   }
   
   @Override
   public GLResourceBase acquire () {
      return (GLShaderProgram)super.acquire ();
   }

   @Override
   public void dispose (GL gl) {
      GL2GL3 gl23 = (GL2GL3)gl;
      gl23.glDeleteProgram (id);
      id = 0;
   }
   
   @Override
   public boolean isDisposed () {
      return  (id != 0);
   }
   
}
