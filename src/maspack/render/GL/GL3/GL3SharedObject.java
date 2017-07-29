package maspack.render.GL.GL3;

import java.util.Arrays;
import java.util.HashSet;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

/**
 * Holds pointers to VBOs, attribute info, to be used in creating VAOs.
 * These can be shared between contexts, but each context needs to maintain
 * its own VAOs.
 * 
 * @author Antonio
 *
 */
public class GL3SharedObject extends GL3ResourceBase 
   implements GL3SharedDrawable {
   
   public enum DrawType {
      ARRAY,
      ELEMENT,
      INSTANCED_ARRAY,
      INSTANCED_ELEMENT
   }
   
   // general purpose indicators
   static final int VERTEX_FLAG_POSITION = 0x01;
   static final int VERTEX_FLAG_NORMAL = 0x02;
   static final int VERTEX_FLAG_COLOR = 0x04;
   static final int VERTEX_FLAG_TEXTURE = 0x08;
   
   VertexBufferObject[] vbos;
   IndexBufferObject ibo;
   
   GL3VertexAttributeArray[] attributes;        // list of attributes
   GL3ElementAttributeArray elements;           // element indices
   
   // draw-specific info
   int start;      // starting vertex
   int count;      // # vertices
   int mode;       // e.g. triangles
   DrawType type;
   int numInstances;
   
   public GL3SharedObject(GL3VertexAttributeArray[] attributes, GL3ElementAttributeArray elements) {
      this(attributes, elements, GL.GL_TRIANGLES); // default guess triangle mode
   }
   
   public GL3SharedObject(GL3VertexAttributeArray[] attributes, 
      GL3ElementAttributeArray elements, int glMode) {
      
      HashSet<VertexBufferObject> vboSet = new HashSet<>();
      for (int i=0; i<attributes.length; ++i) {
         GL3VertexAttributeArray ai = attributes[i];
         if (ai instanceof GL3VertexAttributeArray) {
            vboSet.add(((GL3VertexAttributeArray)ai).getVBO());
         }
      }
      
      IndexBufferObject ibo = null;
      if (elements != null) {
         ibo = elements.getIBO ();
      }
      
      VertexBufferObject[] vbos = vboSet.toArray(new VertexBufferObject[vboSet.size()]);
      set(attributes, elements, vbos, ibo, glMode);
   }
   
   public GL3SharedObject(GL3VertexAttributeArray[] attributes, 
      GL3ElementAttributeArray elements, VertexBufferObject[] vbos, 
      IndexBufferObject ibo, int glMode) {
      set (attributes, elements, vbos, ibo, glMode);
   }
      
   private void set(GL3VertexAttributeArray[] attributes, 
      GL3ElementAttributeArray elements, VertexBufferObject[] vbos, 
      IndexBufferObject ibo, int glMode) {
      this.attributes = Arrays.copyOf(attributes, attributes.length);
      this.elements = elements;
      
      for (VertexBufferObject vbo : vbos) {
         vbo.acquire();  // hold a reference until disposed
      }
      this.vbos = vbos;
      if (ibo != null) {
         this.ibo = ibo.acquire ();
      } else {
         this.ibo = null;
      }
      
      detectDefaultDrawType();
      this.mode = glMode;
   }
   
   /**
    * Bind program attributes to the given program (uses the program's attribute 
    * locations)
    */
   public void bindAttributes(GL3 gl) {
      // bind attributes
      for (GL3VertexAttributeArray ai : attributes) {
         ai.bind(gl);
      }

      // maybe bind indices
      if (elements != null) {
         elements.bind(gl);
      }
   }

   /**
    * Creates and binds a vertex array object attached to a given program.
    * Attributes must have common program locations if the provided VAO
    * is to be used for multiple programs.
    * @return vertex array object index
    */
   public int createVAO(GL3 gl) {
      int[]  vao = new int[1];
      gl.glGenVertexArrays(1, vao, 0);
      gl.glBindVertexArray(vao[0]);
      bindAttributes(gl);      
      gl.glBindVertexArray(0);
      return vao[0];
   }
   
   /**
    * Detects draw mode, count, instances, based on attributes
    */
   private void detectDefaultDrawType() {
      
      boolean instanced = false;
      int nVertices = 0;
      int nElements = 0;
      int nInstanced = Integer.MAX_VALUE;
      
      // guess draw mode
      for (GL3VertexAttributeArray ai : attributes) {
         if (ai instanceof GL3VertexAttributeArray) {
            GL3VertexAttributeArray aiVBO = (GL3VertexAttributeArray)ai;
            if (aiVBO.getDivisor() > 0) {
               instanced = true;
               nInstanced = Math.min(aiVBO.getDivisor()*aiVBO.getCount(), nInstanced);
            } else {
               nVertices = aiVBO.getCount();
            }
         }
      }
      
      // element indices
      boolean hasElements = false;
      if (elements != null) {
         hasElements = true;
         nElements = elements.getCount();
      }
      
      // choose draw type
      if (hasElements) {
         if (instanced) {
            type = DrawType.INSTANCED_ELEMENT;
            numInstances = nInstanced;
         } else {
            type = DrawType.ELEMENT;
            numInstances = 0;
         }
         start = elements.getOffset();
         count = nElements;
      } else {
         if (instanced) {
            type = DrawType.INSTANCED_ARRAY;
            numInstances = nInstanced;
         } else {
            type = DrawType.ARRAY;
            numInstances = 0;
         }
         start = 0;
         count = nVertices;
      }
      
      // default to triangles?
      mode = GL3.GL_TRIANGLES;
   }
   
   public GL3VertexAttributeArray[] getGL3VertexAttributes() {
      return attributes;
   }
   
   public GL3ElementAttributeArray getGL3ElementAttribute() {
      return elements;
   }
   
   public int getMode() {
      return mode;
   }
   
   public int getStart() {
      return start;
   }
   
   public int getCount() {
      return count;
   }
   
   public int getNumInstances() {
      return numInstances;
   }
   
   public DrawType getDrawType() {
      return type;
   }
   
   public void setDrawInfo(int start, int count, int glMode) {
      setDrawInfo(start, count, glMode, 0);
   }
   
   public void setDrawInfo(int start, int count, int glMode, int numInstances) {
      this.start = start;
      this.count = count;
      this.mode = glMode;
      this.numInstances = numInstances;
      if (numInstances > 0) {
         if (elements != null) {
            this.type = DrawType.INSTANCED_ELEMENT;
         } else {
            this.type = DrawType.INSTANCED_ARRAY;
         }
      } else {
         if (elements != null) {
            this.type = DrawType.ELEMENT;
         } else {
            this.type = DrawType.ARRAY;
         }
      }
   }
   
   /**
    * Checks whether all internal GL resources are still valid
    * @return true if internal resources are all still valid
    */
   public boolean isValid () {
      
      // check that all VBOs are valid
      for (GL3VertexAttributeArray ai : attributes) {
         if (!ai.isValid ()) {
            return false;
         }
      }
      
      // maybe bind indices
      if (elements != null) {
         if (!elements.isValid ()) {
            return false;
         }
      }   
      return true;
   }
   
   /**
    * Release hold on any VBOs/IBOs
    */
   public void dispose() {
      if (vbos != null) {
         // release vbos
         for (BufferObject bo : vbos) {
            bo.release ();
         }
         vbos = null;
      }
      if (ibo != null) {
         ibo.release ();
         ibo = null;
      }
   }
   
   /**
    * Release hold on any VBOs/IBOs
    */
   public void dispose(GL3 gl) {
      if (vbos != null) {
         for (BufferObject bo : vbos) {
            bo.releaseDispose (gl);
         }
         vbos = null;
      }
      if (ibo != null) {
         ibo.releaseDispose (gl);
         ibo = null;
      }
   }
   
   @Override
   public boolean isDisposed () {
      return (vbos == null);
   }
   
   @Override
   public GL3SharedObject acquire () {
      return (GL3SharedObject)super.acquire ();
   }
   
   /**
    * Inefficient, generates VAO and destroys it
    */
   public void drawWithVAO(GL3 gl) {
      int vao = createVAO (gl);
      draw(gl,  mode, start, count);
      gl.glDeleteVertexArrays (1, new int[]{vao}, 0);
   }   

   public void draw(GL3 gl) {
      draw(gl, mode, start, count);
   }   
   
   public void drawArrays(GL3 gl, int mode) {
      drawArrays(gl,  mode, start, count);
   }
   
   public void drawArrays(GL3 gl, int mode, int start, int count) {
      gl.glDrawArrays(mode, start, count);
   }
   
   public void drawElements(GL3 gl, int mode, int start, int count, int indexType) {
      gl.glDrawElements(mode, count, indexType, start);
   }
   
   public void drawInstancedArray(GL3 gl, int mode, int start, int count, int instances) {
      gl.glDrawArraysInstanced(mode, start, count, instances);
   }
   
   public void drawInstancedElements(GL3 gl, int mode, int start, int count, int instances) {
      gl.glDrawElementsInstanced (mode, count, elements.getType (), start, instances);
   }
   
   public void drawInstanced(GL3 gl, int mode, int instanceCount) {
      if (elements != null) {
         gl.glDrawElementsInstanced(mode, elements.getCount(), 
            elements.getType(), 0, instanceCount);
      } else {
         gl.glDrawArraysInstanced (mode, 0, count, instanceCount);
      }
   }
   

   public void drawInstanced(GL3 gl, int instances) {
      drawInstanced(gl, mode, instances);
   }
   
   public void draw(GL3 gl, int start, int count) {
      draw(gl,  mode, start, count);
   }
   
   public void draw(GL3 gl, int mode, int start, int count) {
      switch (type) {
         case ARRAY:
            drawArrays(gl, mode, start, count);
            break;
         case ELEMENT:
            drawElements(gl, mode, start, count, elements.getType());
            break;
         case INSTANCED_ARRAY:
            drawInstancedArray(gl, mode, start, count, numInstances);
            break;
         case INSTANCED_ELEMENT:
            drawInstancedElements(gl, mode, start, count, numInstances);
            break;
         default:
            break;
      }
   }

   @Override
   public int getBindVersion() {
      return 1;
   }

   @Override
   public void bind(GL3 gl) {
      bindAttributes(gl);
   }

   @Override
   public boolean equals(GL3SharedDrawable other) {
      return other == this;
   }

  
}
