package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import maspack.util.BufferUtilities;


public class GL3PrimitiveFactory {

   final GL3VertexAttributeInfo vertex_position;
   final GL3VertexAttributeInfo vertex_normal;
   final GL3VertexAttributeInfo vertex_color;
   final GL3VertexAttributeInfo vertex_texcoord;
   
   public GL3PrimitiveFactory(GL3VertexAttributeInfo position, GL3VertexAttributeInfo normal, 
      GL3VertexAttributeInfo color, GL3VertexAttributeInfo texcoord) {
      this.vertex_position = position;
      this.vertex_normal = normal;
      this.vertex_color = color;
      this.vertex_texcoord = texcoord;
   }
   
   
   /**
    * Container holding attribute information and
    * non-shared buffer objects
    */
   public static class GL3ObjectInfo {
      GL3VertexAttributeArrayInfo[] vinfo;
      int[] vboIdxs;        // indices into shared VBOs

      GL3ElementAttributeArray.IBOInfo einfo;
      int mode;
      
      VertexBufferObject[] vbos;
      IndexBufferObject ibo;

      public GL3SharedObject generate() {

         GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[vinfo.length];
         GL3ElementAttributeArray elements = null;

         // build attributes
         for (int i=0; i<vinfo.length; ++i) {
            attributes[i] = new GL3VertexAttributeArray(vbos[vboIdxs[i]], vinfo[i]);
         }

         // build elements
         if (einfo != null) {
            elements = new GL3ElementAttributeArray(ibo, einfo);
         }

         return new GL3SharedObject(attributes, elements, vbos, ibo, mode);
      }
      
      public void dispose(GL3 gl) {
         if (vbos != null) {
            for (BufferObject vbo : vbos) {
               vbo.dispose (gl);
            }
            vbos = null;
         }
         if (ibo != null) {
            ibo.dispose (gl);
            ibo = null;
         }
      }
      
      public boolean releaseDispose(GL3 gl) {
         boolean disposed = false;
         if (vbos != null) {
            for (BufferObject vbo : vbos) {
               disposed |= vbo.releaseDispose (gl);
            }
         } else {
            disposed = true;
         }
         
         if (ibo != null) {
            disposed |= ibo.releaseDispose (gl);
         }
         
         if (disposed) {
            vbos = null;
            ibo = null;
         }
         
         return disposed;
      }

      /**
       * Checks if all vbos are still valid
       * @return
       */
      public boolean isValid () {
         for (BufferObject bo : vbos) {
            if (!bo.isValid ()) {
               return false;
            }
         }
         
         if (ibo != null && !ibo.isValid ()) {
            return false;
         }
         
         return true;
      }
   }

   // cylinder (along zaxis)
   public GL3ObjectInfo createCylinder(GL3 gl, int nSlices, boolean capped) {

      GL3ObjectInfo cylinder = new GL3ObjectInfo();
      cylinder.mode = GL.GL_TRIANGLES;

      int nverts, nelems;

      if (capped) {
         // need to duplicate vertices for caps (different normal)
         nverts = 4*nSlices;
         nelems = 12*nSlices-12;
      } else {
         nverts = 2*nSlices;
         nelems = 6*nSlices;
      }

      PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      NormalBufferPutter nrmPutter = NormalBufferPutter.createDefault();
      IndexBufferPutter idxPutter = IndexBufferPutter.createDefault(nverts-1);

      GL3AttributeStorage posStorage = posPutter.storage();
      GL3AttributeStorage nrmStorage = nrmPutter.storage();
      GL3AttributeStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes() + nrmStorage.bytes();

      cylinder.vinfo = new GL3VertexAttributeArrayInfo[2];
      
      cylinder.vinfo[0] = new GL3VertexAttributeArrayInfo(
         vertex_position, posStorage, 0, vstride, nverts);
      cylinder.vinfo[1] = new GL3VertexAttributeArrayInfo(
         vertex_normal, nrmStorage, posStorage.bytes(), vstride, nverts);
      cylinder.vboIdxs = new int[] {0, 0};

      ByteBuffer pnbuff = BufferUtilities.newNativeByteBuffer(nverts*vstride);
      int pnbuffUsage = GL.GL_STATIC_DRAW;

      cylinder.einfo = new GL3ElementAttributeArray.IBOInfo(GL3Utilities.getGLType(idxStorage.type()), 0, idxStorage.bytes(), nelems);
      ByteBuffer ebuff = BufferUtilities.newNativeByteBuffer(nelems*idxStorage.bytes());
      int ebuffUsage = GL.GL_STATIC_DRAW;

      // bottom
      for (int i=0; i<nSlices; ++i) {
         double angle = 2*Math.PI/nSlices*i;
         float x = (float)Math.cos(angle);
         float y = (float)Math.sin(angle);
         posPutter.putPosition(pnbuff, x, y, 0);
         nrmPutter.putNormal(pnbuff, x, y, 0);
      }

      // top
      for (int i=0; i<nSlices; ++i) {
         double angle = 2*Math.PI/nSlices*i;
         float x = (float)Math.cos(angle);
         float y = (float)Math.sin(angle);
         posPutter.putPosition(pnbuff, x, y, 1);
         nrmPutter.putNormal(pnbuff, x, y, 0);
      }

      // sides
      for (int i=0; i<nSlices; ++i) {
         int j = (i+1) % nSlices;
         idxPutter.putIndices(ebuff, i, j, i+nSlices,
            j, j+nSlices, i+nSlices);
      }

      if (capped) {
         // bottom ring
         for (int i=0; i<nSlices; ++i) {
            double angle = 2*Math.PI/nSlices*i;
            float x = (float)Math.cos(angle);
            float y = (float)Math.sin(angle);
            posPutter.putPosition(pnbuff, x, y, 0);
            nrmPutter.putNormal(pnbuff, 0, 0, -1);
         }

         int vidx = 2*nSlices;
         for (int i=2; i<nSlices; ++i) {
            idxPutter.putIndices(ebuff, vidx+i, vidx+i-1, vidx);
         }

         // top ring
         for (int i=0; i<nSlices; ++i) {
            double angle = 2*Math.PI/nSlices*i;
            float x = (float)Math.cos(angle);
            float y = (float)Math.sin(angle);
            posPutter.putPosition(pnbuff, x, y, 1);
            nrmPutter.putNormal(pnbuff, 0, 0, 1);
         }
         vidx = 3*nSlices;
         for (int i=2; i<nSlices; ++i) {
            idxPutter.putIndices(ebuff, vidx+i-1, vidx+i, vidx);
         }
      }

      ebuff.flip();
      pnbuff.flip();

      cylinder.vbos = new VertexBufferObject[1];
      cylinder.vbos[0] = VertexBufferObject.generate(gl);
      cylinder.vbos[0].fill(gl, pnbuff, pnbuffUsage);
      BufferUtilities.freeDirectBuffer (pnbuff);
      
      cylinder.ibo = IndexBufferObject.generate (gl);
      cylinder.ibo.fill(gl, ebuff, ebuffUsage);
      BufferUtilities.freeDirectBuffer (ebuff);
      
      return cylinder;
   }

   // Cone (along zaxis)
   public GL3ObjectInfo createCone(GL3 gl, int nSlices, boolean capped) {

      GL3ObjectInfo cone = new GL3ObjectInfo();
      cone.mode = GL.GL_TRIANGLES;

      int nverts, nelems;

      if (capped) {
         // need to duplicate vertices for caps (different normal)
         nverts = 3*nSlices;
         nelems = 9*nSlices-6;
      } else {
         nverts = 2*nSlices;
         nelems = 6*nSlices;
      }

      PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      NormalBufferPutter nrmPutter = NormalBufferPutter.createDefault();
      IndexBufferPutter idxPutter = IndexBufferPutter.createDefault(nverts-1);

      GL3AttributeStorage posStorage = posPutter.storage();
      GL3AttributeStorage nrmStorage = nrmPutter.storage();
      GL3AttributeStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes() + nrmStorage.bytes();

      cone.vinfo = new GL3VertexAttributeArrayInfo[2];
      
      cone.vinfo[0] = new GL3VertexAttributeArrayInfo(
         vertex_position, posStorage, 0, vstride, nverts);
      cone.vinfo[1] = new GL3VertexAttributeArrayInfo(
         vertex_normal, nrmStorage, posStorage.bytes(), vstride, nverts);
      cone.vboIdxs = new int[] {0, 0};

      ByteBuffer pnbuff = BufferUtilities.newNativeByteBuffer(nverts*vstride);
      int pnbuffUsage = GL.GL_STATIC_DRAW;

      cone.einfo = new GL3ElementAttributeArray.IBOInfo(GL3Utilities.getGLType(idxStorage.type()), 0, idxStorage.bytes(), nelems);
      ByteBuffer ebuff = BufferUtilities.newNativeByteBuffer(nelems*idxStorage.bytes());
      int ebuffUsage = GL.GL_STATIC_DRAW;

      float r2 = (float)(1.0/Math.sqrt(2));

      // bottom
      for (int i=0; i<nSlices; ++i) {
         double angle = 2*Math.PI/nSlices*i;
         float x = (float)Math.cos(angle);
         float y = (float)Math.sin(angle);
         posPutter.putPosition(pnbuff, x, y, 0);
         nrmPutter.putNormal(pnbuff, x*r2, y*r2, r2);
      }

      // top
      for (int i=0; i<nSlices; ++i) {
         double angle = 2*Math.PI/nSlices*i;
         float x = (float)Math.cos(angle);
         float y = (float)Math.sin(angle);
         posPutter.putPosition(pnbuff, 0, 0, 1);
         nrmPutter.putNormal(pnbuff, x*r2, y*r2, r2);
      }

      // sides
      for (int i=0; i<nSlices; ++i) {
         int j = (i+1) % nSlices;
         idxPutter.putIndices(ebuff, i, j, i+nSlices, 
            j, j+nSlices, i+nSlices);
      }

      if (capped) {

         // bottom ring
         for (int i=0; i<nSlices; ++i) {
            double angle = 2*Math.PI/nSlices*i;
            float x = (float)Math.cos(angle);
            float y = (float)Math.sin(angle);
            posPutter.putPosition(pnbuff, x, y, 0);
            nrmPutter.putNormal(pnbuff, 0, 0, -1);
         }

         int vidx = 2*nSlices;
         for (int i=2; i<nSlices; ++i) {
            idxPutter.putIndices(ebuff, vidx+i, vidx+i-1, vidx);
         }

      }
      
      pnbuff.flip();
      ebuff.flip();
      
      cone.vbos = new VertexBufferObject[1];
      cone.vbos[0] = VertexBufferObject.generate(gl);
      cone.vbos[0].fill(gl, pnbuff, pnbuffUsage);
      BufferUtilities.freeDirectBuffer (pnbuff);
      
      cone.ibo = IndexBufferObject.generate(gl);
      cone.ibo.fill(gl, ebuff, ebuffUsage);
      BufferUtilities.freeDirectBuffer (ebuff);

      return cone;
   }

   // spindle (along zaxis)
   public GL3ObjectInfo createSpindle(GL3 gl, int nSlices, int nLevels) {

      if (nLevels < 2) {
         nLevels = 2;
      }

      GL3ObjectInfo spindle = new GL3ObjectInfo();
      spindle.mode = GL.GL_TRIANGLES;

      int nverts = 2+nSlices*(nLevels-1);
      int nelems = 6*nSlices*(nLevels-1);
      
      PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      NormalBufferPutter nrmPutter = NormalBufferPutter.createDefault();
      IndexBufferPutter idxPutter = IndexBufferPutter.createDefault(nverts-1);

      GL3AttributeStorage posStorage = posPutter.storage();
      GL3AttributeStorage nrmStorage = nrmPutter.storage();
      GL3AttributeStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes() + nrmStorage.bytes();

      spindle.vinfo = new GL3VertexAttributeArrayInfo[2];
      
      spindle.vinfo[0] = new GL3VertexAttributeArrayInfo(
         vertex_position, posStorage, 0, vstride, nverts);
      spindle.vinfo[1] = new GL3VertexAttributeArrayInfo(
         vertex_normal, nrmStorage, posStorage.bytes(), vstride, nverts);
      spindle.vboIdxs = new int[] {0, 0};

      ByteBuffer pnbuff = BufferUtilities.newNativeByteBuffer(nverts*vstride);
      int pnbuffUsage = GL.GL_STATIC_DRAW;

      spindle.einfo = new GL3ElementAttributeArray.IBOInfo(GL3Utilities.getGLType(idxStorage.type()), 0, idxStorage.bytes(), nelems);
      ByteBuffer ebuff = BufferUtilities.newNativeByteBuffer(nelems*idxStorage.bytes());
      int ebuffUsage = GL.GL_STATIC_DRAW;

      // bottom
      posPutter.putPosition(pnbuff, 0,0,0);
      nrmPutter.putNormal(pnbuff, 0,0,-1);
      
      for (int j=1; j < nLevels; ++j) {
         float h = j * 1.0f / nLevels;
         
         for (int i = 0; i < nSlices; ++i) {
            double ang = i * 2 * Math.PI / nSlices;
            float c0 = (float)Math.cos (ang);
            float s0 = (float)Math.sin (ang);

            float r = (float)Math.sin (h * Math.PI);
            float drdh = (float)(Math.PI * Math.cos (h * Math.PI));

            posPutter.putPosition(pnbuff, c0*r, s0*r, h);
            nrmPutter.putNormal(pnbuff, c0, s0, -drdh);
         }
      }

      // top
      posPutter.putPosition(pnbuff, 0,0,1);
      nrmPutter.putNormal(pnbuff, 0,0,1);

      // triangles
      // bottom
      for (int i=0; i<nSlices; ++i) {
         int j = (i + 1) % nSlices;
         idxPutter.putIndices(ebuff, 0, j+1, i+1);
      }

      // middle
      for (int l=0; l<nLevels-2; ++l) {
         int boff = 1+l*nSlices;
         for (int i=0; i<nSlices; ++i) {
            int j = (i + 1) % nSlices;
            idxPutter.putIndices(ebuff,
               boff+j+nSlices, boff+i+nSlices, boff+i,
               boff+j, boff+j+nSlices, boff+i);
         }
      }

      // top
      int boff = 1+nSlices*(nLevels-2);
      int toff = boff+nSlices;
      for (int i=0; i<nSlices; ++i) {
         int j = (i + 1) % nSlices;
         idxPutter.putIndices(ebuff, boff+j, toff, boff+i);
      }
      
      pnbuff.flip();
      ebuff.flip();

      spindle.vbos = new VertexBufferObject[1];
      spindle.vbos[0] = VertexBufferObject.generate(gl);
      spindle.vbos[0].fill(gl, pnbuff, pnbuffUsage);
      BufferUtilities.freeDirectBuffer (pnbuff);
      
      spindle.ibo = IndexBufferObject.generate(gl);
      spindle.ibo.fill(gl, ebuff, ebuffUsage);
      BufferUtilities.freeDirectBuffer (ebuff);
      
      return spindle;
   }

   // sphere
   public GL3ObjectInfo createSphere(GL3 gl, int nSlices, int nLevels) {

      if (nLevels < 2) {
         nLevels = 2;
      }
      
      GL3ObjectInfo sphere = new GL3ObjectInfo();
      sphere.mode = GL.GL_TRIANGLES;
      int nverts = 2+nSlices*(nLevels-1);
      int nelems = 6*nSlices*(nLevels-1);
      
      PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      IndexBufferPutter idxPutter = IndexBufferPutter.createDefault(nverts-1);

      GL3AttributeStorage posStorage = posPutter.storage();
      GL3AttributeStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes();

      sphere.vinfo = new GL3VertexAttributeArrayInfo[2];
      
      sphere.vinfo[0] = new GL3VertexAttributeArrayInfo(
         vertex_position, posStorage, 0, vstride, nverts);
      sphere.vinfo[1] = new GL3VertexAttributeArrayInfo(
         vertex_normal, posStorage, 0, vstride, nverts);
      sphere.vboIdxs = new int[] {0, 0};

      ByteBuffer pnbuff = BufferUtilities.newNativeByteBuffer(nverts*vstride);
      int pnbuffUsage = GL.GL_STATIC_DRAW;

      sphere.einfo = new GL3ElementAttributeArray.IBOInfo(GL3Utilities.getGLType(idxStorage.type()), 0, idxStorage.bytes(), nelems);
      ByteBuffer ebuff = BufferUtilities.newNativeByteBuffer(nelems*idxStorage.bytes());
      int ebuffUsage = GL.GL_STATIC_DRAW;

      // bottom
      posPutter.putPosition(pnbuff, 0, 0,-1);
      
      for (int j=1; j < nLevels; ++j) {
         double hang = j*Math.PI/nLevels;
         float h = -(float)Math.cos(hang);
         float r = (float)Math.sin(hang);
         
         for (int i = 0; i < nSlices; ++i) {
            double ang = i * 2 * Math.PI / nSlices;
            float c0 = (float)Math.cos (ang);
            float s0 = (float)Math.sin (ang);
            posPutter.putPosition(pnbuff, c0*r, s0*r, h);
         }
      }

      // top
      posPutter.putPosition(pnbuff, 0,0,1);
      
      // triangles
      // bottom
      for (int i=0; i<nSlices; ++i) {
         int j = (i + 1) % nSlices;
         idxPutter.putIndices(ebuff, 0, j+1, i+1);
      }

      // middle
      for (int l=0; l<nLevels-2; ++l) {
         int boff = 1+l*nSlices;
         for (int i=0; i<nSlices; ++i) {
            int j = (i + 1) % nSlices;
            idxPutter.putIndices(ebuff,
               boff+j+nSlices, boff+i+nSlices, boff+i,
               boff+j, boff+j+nSlices, boff+i);
         }
      }

      // top
      int boff = 1+nSlices*(nLevels-2);
      int toff = boff+nSlices;
      for (int i=0; i<nSlices; ++i) {
         int j = (i + 1) % nSlices;
         idxPutter.putIndices(ebuff, boff+j, toff, boff+i);
      }
      
      pnbuff.flip();
      ebuff.flip();

      sphere.vbos = new VertexBufferObject[1];
      sphere.vbos[0] = VertexBufferObject.generate(gl);
      sphere.vbos[0].fill(gl, pnbuff, pnbuffUsage);
      BufferUtilities.freeDirectBuffer (pnbuff);
      
      sphere.ibo = IndexBufferObject.generate(gl);
      sphere.ibo.fill(gl, ebuff, ebuffUsage);
      BufferUtilities.freeDirectBuffer (ebuff);
      
      return sphere;
   
   }
   
   // 3D axes
   public GL3ObjectInfo createAxes(GL3 gl, boolean x, boolean y, boolean z) {
     
      GL3ObjectInfo axes = new GL3ObjectInfo();
      axes.mode = GL.GL_LINES;
      int nverts = 0;
      if (x) {
         nverts += 2;
      }
      if (y) {
         nverts += 2;
      }
      if (z) {
         nverts += 2;
      }
      
      PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      ColorBufferPutter clrPutter = ColorBufferPutter.createDefault();

      GL3AttributeStorage posStorage = posPutter.storage();
      GL3AttributeStorage clrStorage = clrPutter.storage();

      int vstride = posStorage.bytes()+clrStorage.bytes();

      axes.vinfo = new GL3VertexAttributeArrayInfo[2];
      
      axes.vinfo[0] = new GL3VertexAttributeArrayInfo(
         vertex_position, posStorage, 0, vstride, nverts);
      axes.vinfo[1] = new GL3VertexAttributeArrayInfo(
         vertex_color, clrStorage, posStorage.bytes(), vstride, nverts);
      axes.vboIdxs = new int[] {0, 0};

      ByteBuffer pcbuff = BufferUtilities.newNativeByteBuffer(nverts*vstride);
      int pcbuffUsage = GL.GL_STATIC_DRAW;
      axes.einfo = null;
      
      if (x) {
         byte[] red = new byte[]{(byte)255, 0, 0, (byte)255};
         posPutter.putPosition(pcbuff, 0, 0, 0);
         clrPutter.putColor(pcbuff, red);
         posPutter.putPosition(pcbuff, 1, 0, 0);
         clrPutter.putColor(pcbuff, red);
      }
      
      if (y) {
         byte[] green = new byte[]{0, (byte)255, 0, (byte)255};
         posPutter.putPosition(pcbuff, 0, 0, 0);
         clrPutter.putColor(pcbuff, green);
         posPutter.putPosition(pcbuff, 0, 1, 0);
         clrPutter.putColor(pcbuff, green);
      }
      
      if (z) {
         byte[] blue = new byte[]{0, 0, (byte)255, (byte)255};
         posPutter.putPosition(pcbuff, 0, 0, 0);
         clrPutter.putColor(pcbuff, blue);
         posPutter.putPosition(pcbuff, 0, 0, 1);
         clrPutter.putColor(pcbuff, blue);
      }
      
      pcbuff.flip();
      
      axes.vbos = new VertexBufferObject[1];
      axes.vbos[0] = VertexBufferObject.generate(gl);
      axes.vbos[0].fill(gl, pcbuff, pcbuffUsage);
      BufferUtilities.freeDirectBuffer (pcbuff);
      
      axes.ibo = null;

      return axes;
   }
   
}
