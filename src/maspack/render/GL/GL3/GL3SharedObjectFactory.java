package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL3;

import maspack.util.BufferUtilities;

/**
 * Convenience routines for generating objects
 *
 */
public class GL3SharedObjectFactory {
   
   final GL3VertexAttributeInfo vertex_position;
   final GL3VertexAttributeInfo vertex_normal;
   final GL3VertexAttributeInfo vertex_color;
   final GL3VertexAttributeInfo vertex_texcoord;
   
   PositionBufferPutter posPutter;
   
   public GL3SharedObjectFactory(GL3VertexAttributeInfo position, GL3VertexAttributeInfo normal,
      GL3VertexAttributeInfo color, GL3VertexAttributeInfo texcoord) {
      this.vertex_position = position;
      this.vertex_normal = normal;
      this.vertex_color = color;
      this.vertex_texcoord = texcoord;
      
      posPutter = PositionBufferPutter.getDefault();
      
   }

   /**
    * vertex
    */
   public GL3SharedObject createVE(GL3 gl, int mode, float[] v, 
      int vUsage, int[] elems, int eUsage) {

      int nverts = v.length/3;
      
      // buffer data
      int posWidth = posPutter.bytesPerPosition();
      ByteBuffer vbuff = BufferUtilities.newNativeByteBuffer(nverts*posWidth);
      posPutter.putPositions(vbuff, v);
      
      IndexBufferPutter idxPutter = IndexBufferPutter.getDefault(nverts-1);
      int idxWidth = idxPutter.bytesPerIndex();
      ByteBuffer ebuff = BufferUtilities.newNativeByteBuffer(elems.length*idxWidth);
      idxPutter.putIndices(ebuff, elems);
      
      // rewind buffers
      vbuff.flip();
      ebuff.flip();
      
      GL3SharedObject out = createVE(gl, mode, 
         vbuff, nverts, posPutter.storage(), vUsage, 
         ebuff, elems.length, idxPutter.storage(), eUsage);
      
      BufferUtilities.freeDirectBuffer (vbuff);
      BufferUtilities.freeDirectBuffer (ebuff);
      
      return out;
   }
   
   /**
    * vertex only
    */
   public GL3SharedObject createVE(GL3 gl, int mode,
      ByteBuffer vbuff, int nverts, GL3AttributeStorage vstorage, int vUsage,
      ByteBuffer ebuff, int nelems, GL3AttributeStorage estorage, int eUsage) {
      
      // generate VBOs
      VertexBufferObject vbo = VertexBufferObject.generate(gl);
      gl.glBindVertexArray (0); // unbind any existing VAOs
      vbo.fill(gl, vbuff, vUsage);
      IndexBufferObject ibo = IndexBufferObject.generate(gl);
      ibo.fill(gl, ebuff, eUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[1];
      attributes[0] = new GL3VertexAttributeArray(vbo,
         new GL3VertexAttributeArrayInfo (vertex_position,
            vstorage, 0, vstorage.width(), nverts, 0 /*divisor*/));
      
      GL3ElementAttributeArray indices = new GL3ElementAttributeArray(ibo, estorage.getGLType (), 
         0, estorage.width(), nelems);
      
      GL3SharedObject glo = new GL3SharedObject(attributes, indices, mode);
      
      return glo;
      
   }
   
   /**
    * Vertex positions only
    */
   public GL3SharedObject createV(GL3 gl, int mode, float[] v, int vUsage) {
      // buffer data
      int nverts = v.length/3;
      int posWidth = posPutter.bytesPerPosition();
      // buffer data
      ByteBuffer vbuff = BufferUtilities.newNativeByteBuffer(nverts*posWidth);
      posPutter.putPositions(vbuff, v);
      
      // rewind buffers
      vbuff.flip();
      
      GL3SharedObject out = createV(gl, mode, vbuff, nverts, posPutter.storage(), posWidth, vUsage);
      BufferUtilities.freeDirectBuffer (vbuff);
      return out;
   }
   
   /**
    * Vertex positions only
    */
   public GL3SharedObject createV(GL3 gl, int mode, 
      ByteBuffer vbuff, int nverts, GL3AttributeStorage vstorage, int vstride, int vUsage) {
      
     return createP(gl, mode, vbuff, nverts, vstorage, vstride, vUsage);
   }
   
   /**
    * Vertex positions only
    */
   public GL3SharedObject createP(GL3 gl, int mode, 
      ByteBuffer vbuff, int nverts, GL3AttributeStorage pstorage, int vstride, int vUsage) {
      
      // generate VBOs
      VertexBufferObject vbo = VertexBufferObject.generate(gl);
      gl.glBindVertexArray (0); // unbind any existing VAOs
      vbo.fill(gl, vbuff, vUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[1];
      attributes[0] = new GL3VertexAttributeArray(vbo, 
         new GL3VertexAttributeArrayInfo (vertex_position, pstorage, 0, vstride, nverts));
      
      GL3SharedObject glo = new GL3SharedObject(attributes, null, mode);
      
      return glo;
      
   }
   
   /**
    * Vertex and normal into an interleaved buffer
    */
   public GL3SharedObject createVN(GL3 gl, int mode, float[] vn, int vnUsage) {

      int verts = vn.length/6;
      int voffset = 0;
      int noffset = 3;
      int stride = 6;
      return createVN(gl, mode, vn, voffset, stride, vn, noffset, stride, verts, vnUsage); 
   }
   
   /**
    * Vertex and normal into an interleaved buffer
    */
   public GL3SharedObject createVN(GL3 gl, int mode, float[] v, int voffset, int vstride,
      float[] n, int noffset, int nstride, int nverts, int vnUsage) {

      // buffer data
      PositionBufferPutter posPutter = PositionBufferPutter.getDefault();
      NormalBufferPutter nrmPutter = NormalBufferPutter.getDefault();
      
      // align to 4-byte multiples
      int posWidth = posPutter.bytesPerPosition();
      int nrmWidth = nrmPutter.bytesPerNormal();
      
      int stride = posWidth + nrmWidth;
      ByteBuffer vnbuff = BufferUtilities.newNativeByteBuffer(nverts*stride);
      posPutter.putPositions(vnbuff, 0, stride, v, voffset, vstride, nverts);
      nrmPutter.putNormals(vnbuff, posWidth, stride, n, noffset, nstride, nverts);
      
      // rewind buffers
      vnbuff.flip();
      
      GL3SharedObject out = createVN(gl, mode, vnbuff, nverts,
         posPutter.storage(), 0, stride, 
         nrmPutter.storage(), posWidth, stride, vnUsage);
      
      BufferUtilities.freeDirectBuffer (vnbuff);
      return out;
   }
   
   /**
    * Interleaved vertex and normal
    */
   public GL3SharedObject createVNE(GL3 gl, int mode, float[] vn, int vnUsage, int[] eidxs, int eUsage) {

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
   public GL3SharedObject createVNE(GL3 gl, int mode, float[] v, int voffset, int vstride,
      float[] n, int noffset, int nstride, int nverts, int vnUsage,
      int[] eidxs, int eoffset, int estride, int nelems, int eUsage) {

      // buffer data
      PositionBufferPutter posPutter = PositionBufferPutter.getDefault();
      NormalBufferPutter nrmPutter = NormalBufferPutter.getDefault();
      
      int posWidth = posPutter.bytesPerPosition();
      int nrmWidth = nrmPutter.bytesPerNormal();
      
      int stride = posWidth+nrmWidth;
      ByteBuffer vnbuff = BufferUtilities.newNativeByteBuffer(nverts*stride);
      
      posPutter.putPositions(vnbuff, 0, stride, v, voffset, vstride, nverts);
      nrmPutter.putNormals(vnbuff, posWidth, stride, n, noffset, nstride, nverts);
      
      IndexBufferPutter idxPutter = IndexBufferPutter.getDefault(nverts-1);
      int idxWidth = idxPutter.bytesPerIndex();
      ByteBuffer ebuff = BufferUtilities.newNativeByteBuffer(nelems*idxWidth);
      idxPutter.putIndices(ebuff, eidxs, eoffset, estride, nelems);
      
      // rewind buffers
      vnbuff.flip();
      ebuff.flip();
      
      GL3SharedObject out = createVNE(gl, mode, vnbuff, nverts,
         posPutter.storage(), 0, stride, 
         nrmPutter.storage(), posWidth, stride, vnUsage,
         ebuff, nelems, idxPutter.storage(), 0, idxWidth, eUsage);
      
      BufferUtilities.freeDirectBuffer (vnbuff);
      BufferUtilities.freeDirectBuffer (ebuff);
      
      return out;
   }
   
   /**
    * Interleaved vertex and normal
    */
   public GL3SharedObject createVN(GL3 gl, int mode, 
      ByteBuffer vnbuff, int nverts, 
      GL3AttributeStorage vstorage, int voffset, int vstride,
      GL3AttributeStorage nstorage, int noffset, int nstride, int vnUsage) {
      
      // generate VBOs
      VertexBufferObject vbo = VertexBufferObject.generate(gl);
      gl.glBindVertexArray (0); // unbind any existing VAOs
      vbo.fill(gl, vnbuff, vnUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[2];
      attributes[0] = new GL3VertexAttributeArray(vbo, 
         new GL3VertexAttributeArrayInfo (vertex_position, vstorage, voffset, 
         vstride, nverts));
      
      attributes[1] = new GL3VertexAttributeArray(vbo,
         new GL3VertexAttributeArrayInfo(vertex_normal, nstorage, noffset, 
         nstride, nverts));
      
      GL3SharedObject glo = new GL3SharedObject(attributes, null, mode);
 
      return glo;
   }
   
   /**
    * Separate vertex and normal
    */
   public GL3SharedObject createVN(GL3 gl, int mode, int nverts,
      ByteBuffer vbuff, GL3AttributeStorage vstorage, int voffset, int vstride,
      ByteBuffer nbuff, GL3AttributeStorage nstorage, int noffset, int nstride, int vnUsage) {
      
      // generate VBOs
      VertexBufferObject vbov = VertexBufferObject.generate(gl);
      gl.glBindVertexArray (0); // unbind any existing VAOs
      vbov.fill(gl, vbuff, vnUsage);
      
      VertexBufferObject vbon = VertexBufferObject.generate(gl);
      vbon.fill (gl,  nbuff, vnUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[2];
      attributes[0] = new GL3VertexAttributeArray(vbov, 
         new GL3VertexAttributeArrayInfo (vertex_position, vstorage, voffset, 
         vstride, nverts));
      attributes[1] = new GL3VertexAttributeArray(vbon,
         new GL3VertexAttributeArrayInfo(vertex_normal, nstorage, noffset, 
         nstride, nverts));
      
      GL3SharedObject glo = new GL3SharedObject(attributes, null, mode);
 
      return glo;
   }
   
   /**
    * Separate vertex, normal, color
    */
   public GL3SharedObject createVNC(GL3 gl, int mode, int nverts,
      ByteBuffer vbuff, GL3AttributeStorage vstorage, int voffset, int vstride,
      ByteBuffer nbuff, GL3AttributeStorage nstorage, int noffset, int nstride, 
      ByteBuffer cbuff, GL3AttributeStorage cstorage, int coffset, int cstride, int vncUsage) {
      
      // generate VBOs
      VertexBufferObject vbov = VertexBufferObject.generate(gl);
      gl.glBindVertexArray (0); // unbind any existing VAOs
      vbov.fill(gl, vbuff, vncUsage);
      
      VertexBufferObject vbon = VertexBufferObject.generate(gl);
      vbon.fill(gl, nbuff, vncUsage);
      
      VertexBufferObject vboc = VertexBufferObject.generate(gl);
      vboc.fill(gl, cbuff, vncUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[3];
      attributes[0] = new GL3VertexAttributeArray(vbov, 
         new GL3VertexAttributeArrayInfo (vertex_position, vstorage, voffset, 
         vstride, nverts));
      attributes[1] = new GL3VertexAttributeArray(vbon,
         new GL3VertexAttributeArrayInfo(vertex_normal, nstorage, noffset, 
         nstride, nverts));
      attributes[2] = new GL3VertexAttributeArray(vboc, 
         new GL3VertexAttributeArrayInfo (vertex_color, cstorage, coffset, 
         cstride, nverts));
      GL3SharedObject glo = new GL3SharedObject(attributes, null, mode);
 
      return glo;
   }
   
   /**
    * Interleaved vertex, color
    */
   public GL3SharedObject createVC(GL3 gl, int mode, int nverts,
      ByteBuffer vcbuff, 
      GL3AttributeStorage vstorage, int voffset, int vstride, 
      GL3AttributeStorage cstorage, int coffset, int cstride, int vcUsage) {
      
      // generate VBOs
      VertexBufferObject vbo = VertexBufferObject.generate(gl);
      gl.glBindVertexArray (0); // unbind any existing VAOs
      vbo.fill(gl, vcbuff, vcUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[2];
      attributes[0] = new GL3VertexAttributeArray(vbo, 
         new GL3VertexAttributeArrayInfo (vertex_position, vstorage, voffset, 
         vstride, nverts));

      attributes[1] = new GL3VertexAttributeArray(vbo, 
         new GL3VertexAttributeArrayInfo (vertex_color, cstorage, coffset, 
         cstride, nverts));
      
      GL3SharedObject glo = new GL3SharedObject(attributes, null, mode);
 
      return glo;
   }
   
   /**
    * Interleaved vertex, normal, color
    */
   public GL3SharedObject createVNC(GL3 gl, int mode, int nverts,
      ByteBuffer vncbuff, 
      GL3AttributeStorage vstorage, int voffset, int vstride,
      GL3AttributeStorage nstorage, int noffset, int nstride, 
      GL3AttributeStorage cstorage, int coffset, int cstride, int vncUsage) {
      
      // generate VBOs
      VertexBufferObject vbo = VertexBufferObject.generate(gl);
      gl.glBindVertexArray (0); // unbind any existing VAOs
      vbo.fill(gl, vncbuff, vncUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[3];
      attributes[0] = new GL3VertexAttributeArray(vbo, 
         new GL3VertexAttributeArrayInfo (vertex_position, vstorage, voffset, 
         vstride, nverts));
      attributes[1] = new GL3VertexAttributeArray(vbo,
         new GL3VertexAttributeArrayInfo(vertex_normal, nstorage, noffset, 
         nstride, nverts));
      attributes[2] = new GL3VertexAttributeArray(vbo, 
         new GL3VertexAttributeArrayInfo (vertex_color, cstorage, coffset, 
         cstride, nverts));
      
      GL3SharedObject glo = new GL3SharedObject(attributes, null, mode);
 
      return glo;
   }
   
   /**
    * Interleaved vertex and normal
    */
   public GL3SharedObject createVNE(GL3 gl, int mode, 
      ByteBuffer vnbuff, int nverts, 
      GL3AttributeStorage vstorage, int voffset, int vstride,
      GL3AttributeStorage nstorage, int noffset, int nstride, int vnUsage,
      ByteBuffer ebuff, int nelems, GL3AttributeStorage estorage, 
      int eoffset, int estride, int eusage) {
      
      // generate VBOs
      VertexBufferObject vbo = VertexBufferObject.generate(gl);
      gl.glBindVertexArray (0); // unbind any existing VAOs
      vbo.fill(gl, vnbuff, vnUsage);
      
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[2];
      attributes[0] = new GL3VertexAttributeArray(vbo, 
         new GL3VertexAttributeArrayInfo (vertex_position, vstorage, voffset, 
         vstride, nverts));
      attributes[1] = new GL3VertexAttributeArray(vbo,
         new GL3VertexAttributeArrayInfo(vertex_normal, nstorage, noffset, 
         nstride, nverts));
      
      IndexBufferObject ibo = IndexBufferObject.generate(gl);
      ibo.fill(gl,  ebuff, eusage);
      GL3ElementAttributeArray indices = new GL3ElementAttributeArray(ibo, estorage.getGLType (), 
         eoffset, estride, nelems);
      
      GL3SharedObject glo = new GL3SharedObject(attributes, indices, mode);
 
      return glo;
      
   }
   
}
