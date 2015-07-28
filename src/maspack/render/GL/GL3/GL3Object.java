package maspack.render.GL.GL3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

public class GL3Object extends GL3ResourceBase implements GL3Drawable {
   
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
   
   int[] vao;
   BufferObject[] vbos;
   
   GL3VertexAttributeArray[] attributes;        // list of attributes
   GL3ElementAttributeArray elements;           // element indices
   
   // draw-specific info
   int start;
   int count;
   int mode;
   DrawType type;
   int numInstances;
   
   public GL3Object(GL3VertexAttributeArray[] attributes, GL3ElementAttributeArray elements) {
      this(attributes, elements, GL.GL_TRIANGLES); // default guess triangle mode
   }
   
   public GL3Object(GL3VertexAttributeArray[] attributes, GL3ElementAttributeArray elements, int glMode) {
      
      this.attributes = Arrays.copyOf(attributes, attributes.length);
      this.elements = elements;
      
      HashSet<BufferObject> vboSet = new HashSet<>();
      for (int i=0; i<attributes.length; ++i) {
         GL3VertexAttribute ai = attributes[i];
         if (ai instanceof GL3VertexAttributeArray) {
            vboSet.add(((GL3VertexAttributeArray)ai).getVBO());
         }
      }
      if (elements != null) {
         vboSet.add(elements.getIBO());
      }
      
      this.vbos = vboSet.toArray(new BufferObject[vboSet.size()]);
      for (BufferObject vbo : this.vbos) {
         vbo.acquire();  // hold a reference until disposed
      }
      
      detectDefaultDrawType();
      this.mode = glMode;
      
   }
   
   public GL3Object(GL3 gl, GL3VertexAttributeArray[] attributes, GL3ElementAttributeArray elements) {
      this(gl, attributes, elements, GL.GL_TRIANGLES);
   }
   
   public GL3Object(GL3 gl, GL3VertexAttributeArray[] attributes, GL3ElementAttributeArray elements, int glMode) {
      this(attributes, elements, glMode);
      init(gl);
   }

   @Override
   /**
    * Sets up the vertex array object
    */
   public void init(GL3 gl) {

      if (vao == null) {
         // generate VAO
         vao = new int[1];
         gl.glGenVertexArrays(1, vao, 0);
      }
      gl.glBindVertexArray(vao[0]);
     
      // bind attributes
      for (GL3VertexAttribute ai : attributes) {
         ai.bind(gl);
      }
      
      // maybe bind indices
      if (elements != null) {
         elements.bind(gl);
      }      
      
      gl.glBindVertexArray(0);
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
      for (GL3VertexAttribute ai : attributes) {
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
   
   public void dispose(GL3 gl) {
      // destroy vaos
      gl.glDeleteVertexArrays(vao.length, vao, 0);
      vao = null;
      
      // release vbos
      if (vbos != null) {
         for (BufferObject v : vbos) {
            v.release(gl);
         }
      }
      vbos = null;
   }
   
   @Override
   public boolean isValid() {
      return (vao != null);
   }
   
   @Override
   public void draw(GL3 gl) {
      draw(gl, mode, start, count);
   }
   
   public void drawArrays(GL3 gl, int mode) {
      drawArrays(gl, mode, start, count);
   }
   
   public void drawArrays(GL3 gl, int mode, int start, int count) {
      gl.glBindVertexArray(vao[0]);
      gl.glDrawArrays(mode, start, count);
      gl.glBindVertexArray(0);
   }
   
   public void drawElements(GL3 gl, int mode, int start, int count, int indexType) {
      gl.glBindVertexArray(vao[0]);
      gl.glDrawElements(mode, count, indexType, start);
      gl.glBindVertexArray(0);
   }
   
   public void drawInstancedArray(GL3 gl, int mode, int start, int count, int instances) {
      gl.glBindVertexArray(vao[0]);
      gl.glDrawArraysInstanced(mode, start, count, instances);
      gl.glBindVertexArray(0);
   }
   
   public void drawInstancedElements(GL3 gl, int mode, int start, int count, int instances) {
      gl.glBindVertexArray(vao[0]);
      gl.glDrawElementsInstanced(mode, count, elements.getType(), elements.getOffset(), instances);
      gl.glBindVertexArray(0);
   }
   
   public void draw(GL3 gl, int start, int count) {
      draw(gl, mode, start, count);
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

   
   public void draw(GL3 gl, int progId) {
      gl.glUseProgram(progId);
      draw(gl);
      gl.glUseProgram(0); // detach program
   }
   
   /**
    * vertex
    */
   public static GL3Object createVE(GL3 gl, int mode, float[] v, 
      int vUsage, int[] elems, int eUsage) {

      int nverts = v.length/3;
      
      // buffer data
      PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      ByteBuffer vbuff = ByteBuffer.allocateDirect(nverts*posPutter.bytesPerPosition());
      vbuff.order(ByteOrder.nativeOrder());
      posPutter.putPositions(vbuff, v);
      
      IndexBufferPutter idxPutter = IndexBufferPutter.createDefault(nverts-1);
      ByteBuffer ebuff = ByteBuffer.allocateDirect(elems.length*idxPutter.bytesPerIndex());
      ebuff.order(ByteOrder.nativeOrder());
      idxPutter.putIndices(ebuff, elems);
      
      // rewind buffers
      vbuff.rewind();
      ebuff.rewind();
      
      return createVE(gl, mode, 
         vbuff, nverts, posPutter.storage(), vUsage, 
         ebuff, elems.length, idxPutter.storage(), eUsage);      
   }
   
   /**
    * vertex only
    */
   public static GL3Object createVE(GL3 gl, int mode, 
      ByteBuffer vbuff, int nverts, BufferStorage vstorage, int vUsage,
      ByteBuffer ebuff, int nelems, BufferStorage estorage, int eUsage) {
      
      // generate VBOs
      BufferObject vbo = new BufferObject(gl);
      vbo.fill(gl, vbuff, GL.GL_ARRAY_BUFFER, vUsage);
      BufferObject ibo = new BufferObject(gl);
      ibo.fill(gl, ebuff, GL.GL_ELEMENT_ARRAY_BUFFER, eUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[1];
      attributes[0] = new GL3VertexAttributeArray(vbo, GL3VertexAttribute.VERTEX_POSITION, 
         GL3Util.getGLType(vstorage.type()), 
         vstorage.size(), vstorage.isNormalized(), 0 /*offset*/, 
         vstorage.bytes(), nverts, 0 /*divisor*/);
      
      GL3ElementAttributeArray indices = new GL3ElementAttributeArray(ibo, GL3Util.getGLType(estorage.type()), 
         0, estorage.bytes(), nelems);
      
      GL3Object glo = new GL3Object(attributes, indices, mode);
      glo.init(gl);
      
      return glo;
      
   }
   
   /**
    * Vertex positions only
    */
   public static GL3Object createV(GL3 gl, int mode, float[] v, int vUsage) {
      // buffer data
      int nverts = v.length/3;
      
      // buffer data
      PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      ByteBuffer vbuff = ByteBuffer.allocateDirect(nverts*posPutter.bytesPerPosition());
      vbuff.order(ByteOrder.nativeOrder());
      posPutter.putPositions(vbuff, v);
      
      // rewind buffers
      vbuff.rewind();
      
      return createV(gl, mode, vbuff, nverts, posPutter.storage(), posPutter.bytesPerPosition(), vUsage);      
   }
   
   /**
    * Vertex positions only
    */
   public static GL3Object createV(GL3 gl, int mode, 
      ByteBuffer vbuff, int nverts, BufferStorage vstorage, int vstride, int vUsage) {
      
     return createV(gl, mode, vbuff, nverts, 
        GL3Util.getGLType(vstorage.type()), vstorage.size(), vstride, vUsage);
      
   }
   
   /**
    * Vertex positions only
    */
   public static GL3Object createV(GL3 gl, int mode, 
      ByteBuffer vbuff, int nverts, int type, int size, int vstride, int vUsage) {
      
      // generate VBOs
      BufferObject vbo = new BufferObject(gl);
      vbo.fill(gl, vbuff, GL.GL_ARRAY_BUFFER, vUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[1];
      attributes[0] = new GL3VertexAttributeArray(vbo, 
         GL3VertexAttribute.VERTEX_POSITION, type, 
         size, false, 0 /*offset*/, 
         vstride, nverts, 0 /*divisor*/);
      
      GL3Object glo = new GL3Object(attributes, null, mode);
      glo.init(gl);
      
      return glo;
      
   }
   
   /**
    * Vertex and normal into an interleaved buffer
    */
   public static GL3Object createVN(GL3 gl, int mode, float[] vn, int vnUsage) {

      int verts = vn.length/6;
      int voffset = 0;
      int noffset = 3;
      int stride = 6;
      return createVN(gl, mode, vn, voffset, stride, vn, noffset, stride, verts, vnUsage); 
   }
   
   /**
    * Vertex and normal into an interleaved buffer
    */
   public static GL3Object createVN(GL3 gl, int mode, float[] v, int voffset, int vstride,
      float[] n, int noffset, int nstride, int nverts, int vnUsage) {

      // buffer data
      PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      NormalBufferPutter nrmPutter = NormalBufferPutter.createDefault();
      
      int stride = posPutter.bytesPerPosition()+nrmPutter.bytesPerNormal();
      ByteBuffer vnbuff = ByteBuffer.allocateDirect(nverts*stride);
      vnbuff.order(ByteOrder.nativeOrder());
      posPutter.putPositions(vnbuff, 0, stride, v, voffset, vstride, nverts);
      nrmPutter.putNormals(vnbuff, posPutter.bytesPerPosition(), stride, n, noffset, nstride, nverts);
      
      // rewind buffers
      vnbuff.rewind();
      
      return createVN(gl, mode, vnbuff, nverts,
         posPutter.storage(), 0, stride, 
         nrmPutter.storage(), posPutter.bytesPerPosition(), stride, vnUsage);      
   }
   
   /**
    * Interleaved vertex and normal
    */
   public static GL3Object createVNE(GL3 gl, int mode, float[] vn, int vnUsage, int[] eidxs, int eUsage) {

      int verts = vn.length/6;
      int voffset = 0;
      int noffset = 3;
      int stride = 6;
      return createVNE(gl, mode, vn, voffset, stride, vn, noffset, stride, 
         verts, vnUsage, eidxs, 0, 1, eidxs.length, eUsage); 
     
   }
   
   /**
    * Vertex and normal into an interleaved buffer
    */
   public static GL3Object createVNE(GL3 gl, int mode, float[] v, int voffset, int vstride,
      float[] n, int noffset, int nstride, int nverts, int vnUsage,
      int[] eidxs, int eoffset, int estride, int nelems, int eUsage) {

      // buffer data
      PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      NormalBufferPutter nrmPutter = NormalBufferPutter.createDefault();
      int stride = posPutter.bytesPerPosition()+nrmPutter.bytesPerNormal();
      ByteBuffer vnbuff = ByteBuffer.allocateDirect(nverts*stride);
      vnbuff.order(ByteOrder.nativeOrder());
      
      posPutter.putPositions(vnbuff, 0, stride, v, voffset, vstride, nverts);
      nrmPutter.putNormals(vnbuff, posPutter.bytesPerPosition(), stride, n, noffset, nstride, nverts);
      
      IndexBufferPutter idxPutter = IndexBufferPutter.createDefault(nverts-1);
      int istride = idxPutter.bytesPerIndex();
      ByteBuffer ebuff = ByteBuffer.allocateDirect(nelems*istride);
      ebuff.order(ByteOrder.nativeOrder());
      idxPutter.putIndices(ebuff, eidxs, eoffset, estride, nelems);
      
      // rewind buffers
      vnbuff.rewind();
      ebuff.rewind();
      
      return createVNE(gl, mode, vnbuff, nverts,
         posPutter.storage(), 0, stride, 
         nrmPutter.storage(), posPutter.bytesPerPosition(), stride, vnUsage,
         ebuff, nelems, idxPutter.storage(), 0, idxPutter.bytesPerIndex(), eUsage);      
   }
   
   /**
    * Interleaved vertex and normal
    */
   public static GL3Object createVN(GL3 gl, int mode, 
      ByteBuffer vnbuff, int nverts, 
      BufferStorage vstorage, int voffset, int vstride,
      BufferStorage nstorage, int noffset, int nstride, int vnUsage) {
      
      // generate VBOs
      BufferObject vbo = new BufferObject(gl);
      vbo.fill(gl, vnbuff, GL.GL_ARRAY_BUFFER, vnUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[2];
      attributes[0] = new GL3VertexAttributeArray(vbo, 
         GL3VertexAttribute.VERTEX_POSITION, GL3Util.getGLType(vstorage.type()), 
         vstorage.size(), vstorage.isNormalized(), voffset, 
         vstride, nverts, 0 /*divisor*/);
      
      attributes[1] = new GL3VertexAttributeArray(vbo, 
         GL3VertexAttribute.VERTEX_NORMAL, GL3Util.getGLType(nstorage.type()), 
         nstorage.size(), nstorage.isNormalized(), noffset, 
         nstride, nverts, 0 /*divisor*/);
      
      GL3Object glo = new GL3Object(attributes, null, mode);
      glo.init(gl);
 
      return glo;
      
   }
   
   /**
    * Interleaved vertex and normal
    */
   public static GL3Object createVNE(GL3 gl, int mode, 
      ByteBuffer vnbuff, int nverts, 
      BufferStorage vstorage, int voffset, int vstride,
      BufferStorage nstorage, int noffset, int nstride, int vnUsage,
      ByteBuffer ebuff, int nelems, BufferStorage estorage, 
      int eoffset, int estride, int eusage) {
      
      // generate VBOs
      BufferObject vbo = new BufferObject(gl);
      vbo.fill(gl, vnbuff, GL.GL_ARRAY_BUFFER, vnUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[2];
      attributes[0] = new GL3VertexAttributeArray(vbo, 
         GL3VertexAttribute.VERTEX_POSITION, GL3Util.getGLType(vstorage.type()), 
         vstorage.size(), vstorage.isNormalized(), voffset, 
         vstride, nverts, 0 /*divisor*/);
      
      attributes[1] = new GL3VertexAttributeArray(vbo, 
         GL3VertexAttribute.VERTEX_NORMAL, GL3Util.getGLType(nstorage.type()), 
         nstorage.size(), nstorage.isNormalized(), noffset, 
         nstride, nverts, 0 /*divisor*/);
      
      BufferObject ibo = new BufferObject(gl);
      ibo.fill(gl,  ebuff, GL.GL_ELEMENT_ARRAY_BUFFER, eusage);
      GL3ElementAttributeArray indices = new GL3ElementAttributeArray(ibo, GL3Util.getGLType(estorage.type()), 
         eoffset, estride, nelems);
      
      GL3Object glo = new GL3Object(attributes, indices, mode);
      glo.init(gl);
 
      return glo;
      
   }
  
}
