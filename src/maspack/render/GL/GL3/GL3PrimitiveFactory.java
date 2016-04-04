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
   
   public GL3SharedObject createCylinder(GL3 gl, int nSlices, boolean capped) {
      int nverts, nelems;
      if (capped) {
         // need to duplicate vertices for caps (different normal)
         nverts = 4*nSlices;
         nelems = 12*nSlices-6;
      } else {
         nverts = 2*nSlices;
         nelems = 6*nSlices;
      }
      
      PositionBufferPutter posPutter = PositionBufferPutter.getDefault();
      NormalBufferPutter nrmPutter = NormalBufferPutter.getDefault();
      IndexBufferPutter idxPutter = IndexBufferPutter.getDefault(nverts-1);

      GL3AttributeStorage posStorage = posPutter.storage();
      GL3AttributeStorage nrmStorage = nrmPutter.storage();
      GL3AttributeStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes() + nrmStorage.bytes();

      VertexBufferObject vbo = VertexBufferObject.generate (gl);
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[2];
      attributes[0] = new GL3VertexAttributeArray (vbo, new GL3VertexAttributeArrayInfo(
         vertex_position, posStorage, 0, vstride, nverts));
      attributes[1] = new GL3VertexAttributeArray (vbo, new GL3VertexAttributeArrayInfo(
         vertex_normal, nrmStorage, posStorage.bytes(), vstride, nverts));
      ByteBuffer pnbuff = BufferUtilities.newNativeByteBuffer(nverts*vstride);
      
      IndexBufferObject ibo = IndexBufferObject.generate (gl);
      GL3ElementAttributeArray elements = new GL3ElementAttributeArray(ibo,
         idxStorage.getGLType (), 0, idxStorage.bytes(), nelems);
      ByteBuffer ebuff = BufferUtilities.newNativeByteBuffer(nelems*idxStorage.bytes());
       
      if (capped) { 
         for (int i=0; i<nSlices; ++i) {
            double angle = 2*Math.PI/nSlices*i;
            float x = (float)Math.cos(angle);
            float y = (float)Math.sin(angle);
            
            // bottom side, top side
            posPutter.putPosition (pnbuff, x, y, 0); 
            nrmPutter.putNormal (pnbuff, x, y, 0);
            posPutter.putPosition (pnbuff, x, y, 1);
            nrmPutter.putNormal (pnbuff, x, y, 0);
            
            // bottom, top
            posPutter.putPosition (pnbuff, x, y, 0);
            nrmPutter.putNormal (pnbuff, 0, 0, -1);
            posPutter.putPosition (pnbuff, x, y, 1);
            nrmPutter.putNormal (pnbuff, 0, 0, 1);
         }
         
         int r = 0;
         int m, l;
         for (int i=1; i<nSlices; ++i) {
            m = r+4;
            l = m+4;
            idxPutter.putIndices (ebuff, 
               r, m, r+1, m, m+1, r+1,      // sides
                   2, l+2, m+2, 3, m+3, l+3);   // bottom/top
            r += 4;
         }
         // close the gap
         idxPutter.putIndices (ebuff, r, 0, r+1, 0, 1, r+1);  // sides
         
      } else {
         for (int i=0; i<nSlices; ++i) {
            double angle = 2*Math.PI/nSlices*i;
            float x = (float)Math.cos(angle);
            float y = (float)Math.sin(angle);
            
            // bottom side, top side
            posPutter.putPosition (pnbuff, x, y, 0); 
            nrmPutter.putNormal (pnbuff, x, y, 0);
            posPutter.putPosition (pnbuff, x, y, 1);
            nrmPutter.putNormal (pnbuff, x, y, 0);
         }
         
         int r = 0;
         int l;
         for (int i=1; i<nSlices; ++i) {
            l = r+2;
            idxPutter.putIndices (ebuff, 
               r, l, r+1, l, l+1, r+1);   // sides
            r += 2;
         }
         // close the gap
         idxPutter.putIndices (ebuff, r, 0, r+1, 0, 1, r+1);  // sides
      }     
      
      // ensure no vertex array is bound
      gl.glBindVertexArray (0);
      
      pnbuff.flip();
      vbo.fill(gl, pnbuff, GL.GL_STATIC_DRAW);
      BufferUtilities.freeDirectBuffer (pnbuff);
      
      ebuff.flip();
      ibo.fill(gl, ebuff, GL.GL_STATIC_DRAW);
      BufferUtilities.freeDirectBuffer (ebuff);
      
      return new GL3SharedObject(attributes, elements, new VertexBufferObject[]{vbo}, ibo, GL.GL_TRIANGLES);

   }

   // Cone (along zaxis)
   public GL3SharedObject createCone(GL3 gl, int nSlices, boolean capped) {

      int nverts, nelems;

      if (capped) {
         // need to duplicate vertices for caps (different normal)
         nverts = 3*nSlices;
         nelems = 6*nSlices-3;
      } else {
         nverts = 2*nSlices;
         nelems = 3*nSlices;
      }

      PositionBufferPutter posPutter = PositionBufferPutter.getDefault();
      NormalBufferPutter nrmPutter = NormalBufferPutter.getDefault();
      IndexBufferPutter idxPutter = IndexBufferPutter.getDefault(nverts-1);

      GL3AttributeStorage posStorage = posPutter.storage();
      GL3AttributeStorage nrmStorage = nrmPutter.storage();
      GL3AttributeStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes() + nrmStorage.bytes();

      VertexBufferObject vbo = VertexBufferObject.generate (gl);
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[2];
      attributes[0] = new GL3VertexAttributeArray (vbo, new GL3VertexAttributeArrayInfo(
         vertex_position, posStorage, 0, vstride, nverts));
      attributes[1] = new GL3VertexAttributeArray (vbo, new GL3VertexAttributeArrayInfo(
         vertex_normal, nrmStorage, posStorage.bytes(), vstride, nverts));
      ByteBuffer pnbuff = BufferUtilities.newNativeByteBuffer(nverts*vstride);
      
      IndexBufferObject ibo = IndexBufferObject.generate (gl);
      GL3ElementAttributeArray elements = new GL3ElementAttributeArray(ibo,
         idxStorage.getGLType (), 0, idxStorage.bytes(), nelems);
      ByteBuffer ebuff = BufferUtilities.newNativeByteBuffer(nelems*idxStorage.bytes());
       
      float r2 = (float)(1.0/Math.sqrt(2));
      
      if (capped) { 
         for (int i=0; i<nSlices; ++i) {
            double angle = 2*Math.PI/nSlices*i;
            float x = (float)Math.cos(angle);
            float y = (float)Math.sin(angle);
            
            // bottom side, top side
            posPutter.putPosition (pnbuff, x, y, 0); 
            nrmPutter.putNormal (pnbuff, x*r2, y*r2, r2);
            posPutter.putPosition (pnbuff, 0, 0, 1);
            nrmPutter.putNormal (pnbuff, x*r2, y*r2, r2);
            
            // bottom
            posPutter.putPosition (pnbuff, x, y, 0);
            nrmPutter.putNormal (pnbuff, 0, 0, -1);
         }
         
         int r = 0;
         int m, l;
         for (int i=1; i<nSlices; ++i) {
            m = r+3;
            l = m+3;
            idxPutter.putIndices (ebuff, 
               r, m, r+1,                   // sides
               2, l+2, m+2);                // bottom
            r += 3;
         }
         // close the gap
         idxPutter.putIndices (ebuff, r, 0, r+1);  // sides
         
      } else {
         for (int i=0; i<nSlices; ++i) {
            double angle = 2*Math.PI/nSlices*i;
            float x = (float)Math.cos(angle);
            float y = (float)Math.sin(angle);
            
            // bottom side, top side
            posPutter.putPosition (pnbuff, x, y, 0); 
            nrmPutter.putNormal (pnbuff, x*r2, y*r2, r2);
            posPutter.putPosition (pnbuff, 0, 0, 1);
            nrmPutter.putNormal (pnbuff, x*r2, y*r2, r2);
         }
         
         int r = 0;
         int l;
         for (int i=1; i<nSlices; ++i) {
            l = r+2;
            idxPutter.putIndices (ebuff, r, l, r+1);   // sides
            r += 2;
         }
         // close the gap
         idxPutter.putIndices (ebuff, r, 0, r+1);  // sides
      }     
      
      // ensure no vertex array is bound
      gl.glBindVertexArray (0);
      
      pnbuff.flip();
      vbo.fill(gl, pnbuff, GL.GL_STATIC_DRAW);
      BufferUtilities.freeDirectBuffer (pnbuff);
      
      ebuff.flip();
      ibo.fill(gl, ebuff, GL.GL_STATIC_DRAW);
      BufferUtilities.freeDirectBuffer (ebuff);
      
      return new GL3SharedObject(attributes, elements, new VertexBufferObject[]{vbo}, ibo, GL.GL_TRIANGLES);
      
   }

   // spindle (along zaxis)
   public GL3SharedObject createSpindle(GL3 gl, int nSlices, int nLevels) {

      if (nLevels < 2) {
         nLevels = 2;
      }

      int nverts = 2+nSlices*(nLevels-1);
      int nelems = 6*nSlices*(nLevels-1);
      
      PositionBufferPutter posPutter = PositionBufferPutter.getDefault();
      NormalBufferPutter nrmPutter = NormalBufferPutter.getDefault();
      IndexBufferPutter idxPutter = IndexBufferPutter.getDefault(nverts-1);

      GL3AttributeStorage posStorage = posPutter.storage();
      GL3AttributeStorage nrmStorage = nrmPutter.storage();
      GL3AttributeStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes() + nrmStorage.bytes();

      VertexBufferObject vbo = VertexBufferObject.generate (gl);
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[2];
      attributes[0] = new GL3VertexAttributeArray (vbo, new GL3VertexAttributeArrayInfo(
         vertex_position, posStorage, 0, vstride, nverts));
      attributes[1] = new GL3VertexAttributeArray (vbo, new GL3VertexAttributeArrayInfo(
         vertex_normal, nrmStorage, posStorage.bytes(), vstride, nverts));
      ByteBuffer pnbuff = BufferUtilities.newNativeByteBuffer(nverts*vstride);
      
      IndexBufferObject ibo = IndexBufferObject.generate (gl);
      GL3ElementAttributeArray elements = new GL3ElementAttributeArray(ibo,
         idxStorage.getGLType (), 0, idxStorage.bytes(), nelems);
      ByteBuffer ebuff = BufferUtilities.newNativeByteBuffer(nelems*idxStorage.bytes());

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
      
      // ensure no vertex array is bound
      gl.glBindVertexArray (0);
      
      pnbuff.flip();
      vbo.fill(gl, pnbuff, GL.GL_STATIC_DRAW);
      BufferUtilities.freeDirectBuffer (pnbuff);
      
      ebuff.flip();
      ibo.fill(gl, ebuff, GL.GL_STATIC_DRAW);
      BufferUtilities.freeDirectBuffer (ebuff);
      
      return new GL3SharedObject(attributes, elements, new VertexBufferObject[]{vbo}, ibo, GL.GL_TRIANGLES);
   }

   // sphere
   public GL3SharedObject createSphere(GL3 gl, int nSlices, int nLevels) {

      if (nLevels < 2) {
         nLevels = 2;
      }
      
      int nverts = 2+nSlices*(nLevels-1);
      int nelems = 6*nSlices*(nLevels-1);
      
      PositionBufferPutter posPutter = PositionBufferPutter.getDefault();
      IndexBufferPutter idxPutter = IndexBufferPutter.getDefault(nverts-1);

      GL3AttributeStorage posStorage = posPutter.storage();
      GL3AttributeStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes();

      VertexBufferObject vbo = VertexBufferObject.generate (gl);
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[2];
      attributes[0] = new GL3VertexAttributeArray (vbo, new GL3VertexAttributeArrayInfo(
         vertex_position, posStorage, 0, vstride, nverts));
      attributes[1] = new GL3VertexAttributeArray (vbo, new GL3VertexAttributeArrayInfo(
         vertex_normal, posStorage, 0, vstride, nverts));  // aliased
      ByteBuffer pnbuff = BufferUtilities.newNativeByteBuffer(nverts*vstride);
      
      IndexBufferObject ibo = IndexBufferObject.generate (gl);
      GL3ElementAttributeArray elements = new GL3ElementAttributeArray(ibo,
         idxStorage.getGLType (), 0, idxStorage.bytes(), nelems);
      ByteBuffer ebuff = BufferUtilities.newNativeByteBuffer(nelems*idxStorage.bytes());
      
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
      
      // ensure no vertex array is bound
      gl.glBindVertexArray (0);
      
      pnbuff.flip();
      vbo.fill(gl, pnbuff, GL.GL_STATIC_DRAW);
      BufferUtilities.freeDirectBuffer (pnbuff);
      
      ebuff.flip();
      ibo.fill(gl, ebuff, GL.GL_STATIC_DRAW);
      BufferUtilities.freeDirectBuffer (ebuff);
      
      return new GL3SharedObject(attributes, elements, new VertexBufferObject[]{vbo}, ibo, GL.GL_TRIANGLES);
   
   }
   
   // 3D axes
   public GL3SharedObject createAxes(GL3 gl, boolean x, boolean y, boolean z) {
     
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
      
      PositionBufferPutter posPutter = PositionBufferPutter.getDefault();
      ColorBufferPutter clrPutter = ColorBufferPutter.getDefault();

      GL3AttributeStorage posStorage = posPutter.storage();
      GL3AttributeStorage clrStorage = clrPutter.storage();

      int vstride = posStorage.bytes()+clrStorage.bytes();

      VertexBufferObject vbo = VertexBufferObject.generate (gl);
      GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[2];
      attributes[0] = new GL3VertexAttributeArray (vbo, new GL3VertexAttributeArrayInfo(
         vertex_position, posStorage, 0, vstride, nverts));
      attributes[1] = new GL3VertexAttributeArray (vbo, new GL3VertexAttributeArrayInfo(
         vertex_color, clrStorage, 0, vstride, nverts));  // aliased
      ByteBuffer pcbuff = BufferUtilities.newNativeByteBuffer(nverts*vstride);
      
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
      
      // ensure no vertex array is bound
      gl.glBindVertexArray (0);
      
      pcbuff.flip();
      vbo.fill(gl, pcbuff, GL.GL_STATIC_DRAW);
      BufferUtilities.freeDirectBuffer (pcbuff);
      
      return new GL3SharedObject(attributes, null, new VertexBufferObject[]{vbo}, null, GL.GL_LINES);
   }
   
}
