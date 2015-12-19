package maspack.render.GL.GL3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;


public class GL3ObjectFactory {

   private static class GL3PreObject {

      GL3VertexAttribute.AttributeInfo[] ainfo;
      GL3VertexAttributeArray.VBOInfo[] vinfo;
      int[] vbuffIdxs;
      ByteBuffer[] vbuffs;
      int[] vbuffUsage;

      GL3ElementAttributeArray.ElementInfo einfo;
      ByteBuffer ebuff;
      int ebuffUsage;

      int mode;

      public GL3Object generate(GL3 gl) {

         BufferObject[] vbos = new BufferObject[vbuffs.length];
         GL3VertexAttributeArray[] attributes = new GL3VertexAttributeArray[vinfo.length];
         GL3ElementAttributeArray elements = null;

         // generate VBOs
         for (int i=0; i<vbuffs.length; ++i) {
            vbos[i] = new BufferObject(gl);
            vbos[i].fill(gl, vbuffs[i], GL.GL_ARRAY_BUFFER, vbuffUsage[i]);
         }

         for (int i=0; i<vinfo.length; ++i) {
            attributes[i] = new GL3VertexAttributeArray(vbos[vbuffIdxs[i]], ainfo[i], vinfo[i]);
         }

         // generate IBO
         if (einfo != null) {
            BufferObject ibo = new BufferObject(gl);
            ibo.fill(gl, ebuff, GL.GL_ELEMENT_ARRAY_BUFFER, ebuffUsage);

            elements = new GL3ElementAttributeArray(ibo, einfo);
         }

         return new GL3Object(attributes, elements, mode);
      }
   }

   // cylinder (along zaxis)
   private static GL3PreObject createCylinder(int nSlices, boolean capped) {

      GL3PreObject cylinder = new GL3PreObject();
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

      BufferStorage posStorage = posPutter.storage();
      BufferStorage nrmStorage = nrmPutter.storage();
      BufferStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes() + nrmStorage.bytes();

      cylinder.ainfo = new GL3VertexAttribute.AttributeInfo[2];
      cylinder.vinfo = new GL3VertexAttributeArray.VBOInfo[2];
      
      cylinder.ainfo[0] = GL3VertexAttribute.VERTEX_POSITION;
      cylinder.vinfo[0] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(posStorage.type()), 
         posStorage.size(), posStorage.isNormalized(), 0, vstride, nverts);
      cylinder.ainfo[1] = GL3VertexAttribute.VERTEX_NORMAL;
      cylinder.vinfo[1] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(nrmStorage.type()), 
         nrmStorage.size(), nrmStorage.isNormalized(), posStorage.bytes(), vstride, nverts);
      cylinder.vbuffIdxs = new int[] {0, 0};

      cylinder.vbuffs = new ByteBuffer[1];
      cylinder.vbuffs[0] = ByteBuffer.allocateDirect(nverts*vstride);
      cylinder.vbuffs[0].order(ByteOrder.nativeOrder());
      cylinder.vbuffUsage = new int[] {GL.GL_STATIC_DRAW};

      cylinder.einfo = new GL3ElementAttributeArray.ElementInfo(GL3Util.getGLType(idxStorage.type()), 0, idxStorage.bytes(), nelems);
      cylinder.ebuff = ByteBuffer.allocateDirect(nelems*idxStorage.bytes());
      cylinder.ebuff.order(ByteOrder.nativeOrder());
      cylinder.ebuffUsage = GL.GL_STATIC_DRAW;


      ByteBuffer pnbuff = cylinder.vbuffs[0];

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
         idxPutter.putIndices(cylinder.ebuff, i, j, i+nSlices,
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
            idxPutter.putIndices(cylinder.ebuff, vidx+i, vidx+i-1, vidx);
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
            idxPutter.putIndices(cylinder.ebuff, vidx+i-1, vidx+i, vidx);
         }
      }

      cylinder.ebuff.rewind();
      pnbuff.rewind();

      return cylinder;
   }

   // cylinder (along zaxis)
   public static GL3Object createCylinder(GL3 gl, int nSlices, boolean capped) {
      GL3PreObject pglo = createCylinder(nSlices, capped);
      return pglo.generate(gl);
   }

   // cylinder (along zaxis)
   private static GL3PreObject createCylinder(float topRad, int nSlices, boolean capped) {

      GL3PreObject cylinder = new GL3PreObject();
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

      BufferStorage posStorage = posPutter.storage();
      BufferStorage nrmStorage = nrmPutter.storage();
      BufferStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes() + nrmStorage.bytes();

      cylinder.ainfo = new GL3VertexAttribute.AttributeInfo[2];
      cylinder.vinfo = new GL3VertexAttributeArray.VBOInfo[2];
      
      cylinder.ainfo[0] = GL3VertexAttribute.VERTEX_POSITION;
      cylinder.vinfo[0] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(posStorage.type()), 
         posStorage.size(), posStorage.isNormalized(), 0, vstride, nverts);
      cylinder.ainfo[1] = GL3VertexAttribute.VERTEX_NORMAL;
      cylinder.vinfo[1] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(nrmStorage.type()), 
         nrmStorage.size(), nrmStorage.isNormalized(), posStorage.bytes(), vstride, nverts);
      cylinder.vbuffIdxs = new int[] {0, 0};

      cylinder.vbuffs = new ByteBuffer[1];
      cylinder.vbuffs[0] = ByteBuffer.allocateDirect(nverts*vstride);
      cylinder.vbuffs[0].order(ByteOrder.nativeOrder());
      cylinder.vbuffUsage = new int[] {GL.GL_STATIC_DRAW};

      cylinder.einfo = new GL3ElementAttributeArray.ElementInfo(GL3Util.getGLType(idxStorage.type()), 0, idxStorage.bytes(), nelems);
      cylinder.ebuff = ByteBuffer.allocateDirect(nelems*idxStorage.bytes());
      cylinder.ebuff.order(ByteOrder.nativeOrder());
      cylinder.ebuffUsage = GL.GL_STATIC_DRAW;

      ByteBuffer pnbuff = cylinder.vbuffs[0];

      float dr = (float)(1.0/Math.sqrt(2+topRad*topRad-2*topRad));

      // bottom
      for (int i=0; i<nSlices; ++i) {
         double angle = 2*Math.PI/nSlices*i;
         float x = (float)Math.cos(angle);
         float y = (float)Math.sin(angle);
         posPutter.putPosition(pnbuff, x, y, 0);
         nrmPutter.putNormal(pnbuff, x*dr, y*dr, (1-topRad)*dr);
      }

      // top
      for (int i=0; i<nSlices; ++i) {
         double angle = 2*Math.PI/nSlices*i;
         float x = (float)Math.cos(angle);
         float y = (float)Math.sin(angle);
         posPutter.putPosition(pnbuff, topRad*x, topRad*y, 1);
         nrmPutter.putNormal(pnbuff, x*dr, y*dr, (1-topRad)*dr);
      }

      // sides
      for (int i=0; i<nSlices; ++i) {
         int j = (i+1) % nSlices;
         idxPutter.putIndices(cylinder.ebuff, i, j, i+nSlices, 
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
            idxPutter.putIndices(cylinder.ebuff, vidx+i, vidx+i-1, vidx);
         }


         // top ring
         for (int i=0; i<nSlices; ++i) {
            double angle = 2*Math.PI/nSlices*i;
            float x = (float)Math.cos(angle);
            float y = (float)Math.sin(angle);
            posPutter.putPosition(pnbuff, topRad*x, topRad*y, 1);
            nrmPutter.putNormal(pnbuff, 0, 0, 1);
         }
         vidx = 3*nSlices;
         for (int i=2; i<nSlices; ++i) {
            idxPutter.putIndices(cylinder.ebuff, vidx+i-1, vidx+i, vidx);
         }
      }

      cylinder.ebuff.rewind();
      pnbuff.rewind();

      return cylinder;

   }

   // cylinder (along zaxis)
   public static GL3Object createCylinder(GL3 gl, float topRad, int nSlices, boolean capped) {
      GL3PreObject pglo = createCylinder(topRad, nSlices, capped);
      return pglo.generate(gl);
   }

   // Cone (along zaxis)
   private static GL3PreObject createCone(int nSlices, boolean capped) {

      GL3PreObject cone = new GL3PreObject();
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

      BufferStorage posStorage = posPutter.storage();
      BufferStorage nrmStorage = nrmPutter.storage();
      BufferStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes() + nrmStorage.bytes();

      cone.ainfo = new GL3VertexAttribute.AttributeInfo[2];
      cone.vinfo = new GL3VertexAttributeArray.VBOInfo[2];
      
      cone.ainfo[0] = GL3VertexAttribute.VERTEX_POSITION;
      cone.vinfo[0] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(posStorage.type()), 
         posStorage.size(), posStorage.isNormalized(), 0, vstride, nverts);
      cone.ainfo[1] = GL3VertexAttribute.VERTEX_NORMAL;
      cone.vinfo[1] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(nrmStorage.type()), 
         nrmStorage.size(), nrmStorage.isNormalized(), posStorage.bytes(), vstride, nverts);
      cone.vbuffIdxs = new int[] {0, 0};

      cone.vbuffs = new ByteBuffer[1];
      cone.vbuffs[0] = ByteBuffer.allocateDirect(nverts*vstride);
      cone.vbuffs[0].order(ByteOrder.nativeOrder());
      cone.vbuffUsage = new int[] {GL.GL_STATIC_DRAW};

      cone.einfo = new GL3ElementAttributeArray.ElementInfo(GL3Util.getGLType(idxStorage.type()), 0, idxStorage.bytes(), nelems);
      cone.ebuff = ByteBuffer.allocateDirect(nelems*idxStorage.bytes());
      cone.ebuff.order(ByteOrder.nativeOrder());
      cone.ebuffUsage = GL.GL_STATIC_DRAW;

      ByteBuffer pnbuff = cone.vbuffs[0];

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
         idxPutter.putIndices(cone.ebuff, i, j, i+nSlices, 
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
            idxPutter.putIndices(cone.ebuff, vidx+i, vidx+i-1, vidx);
         }

      }
      
      pnbuff.rewind();
      cone.ebuff.rewind();

      return cone;
   }

   // cone (along zaxis)
   public static GL3Object createCone(GL3 gl, int nSlices, boolean capped) {
      GL3PreObject pglo = createCone(nSlices, capped);
      return pglo.generate(gl);
   }

   // tapered ellipsoid (along zaxis)
   private static GL3PreObject createTaperedEllipsoid(int nSlices, int nLevels) {

      if (nLevels < 2) {
         nLevels = 2;
      }

      GL3PreObject ellipsoid = new GL3PreObject();
      ellipsoid.mode = GL.GL_TRIANGLES;

      int nverts = 2+nSlices*(nLevels-1);
      int nelems = 6*nSlices*(nLevels-1);
      
      PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      NormalBufferPutter nrmPutter = NormalBufferPutter.createDefault();
      IndexBufferPutter idxPutter = IndexBufferPutter.createDefault(nverts-1);

      BufferStorage posStorage = posPutter.storage();
      BufferStorage nrmStorage = nrmPutter.storage();
      BufferStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes() + nrmStorage.bytes();

      ellipsoid.ainfo = new GL3VertexAttribute.AttributeInfo[2];
      ellipsoid.vinfo = new GL3VertexAttributeArray.VBOInfo[2];
      
      ellipsoid.ainfo[0] = GL3VertexAttribute.VERTEX_POSITION;
      ellipsoid.vinfo[0] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(posStorage.type()), 
         posStorage.size(), posStorage.isNormalized(), 0, vstride, nverts);
      ellipsoid.ainfo[1] = GL3VertexAttribute.VERTEX_NORMAL;
      ellipsoid.vinfo[1] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(nrmStorage.type()), 
         nrmStorage.size(), nrmStorage.isNormalized(), posStorage.bytes(), vstride, nverts);
      ellipsoid.vbuffIdxs = new int[] {0, 0};

      ellipsoid.vbuffs = new ByteBuffer[1];
      ellipsoid.vbuffs[0] = ByteBuffer.allocateDirect(nverts*vstride);
      ellipsoid.vbuffs[0].order(ByteOrder.nativeOrder());
      ellipsoid.vbuffUsage = new int[] {GL.GL_STATIC_DRAW};

      ellipsoid.einfo = new GL3ElementAttributeArray.ElementInfo(GL3Util.getGLType(idxStorage.type()), 0, idxStorage.bytes(), nelems);
      ellipsoid.ebuff = ByteBuffer.allocateDirect(nelems*idxStorage.bytes());
      ellipsoid.ebuff.order(ByteOrder.nativeOrder());
      ellipsoid.ebuffUsage = GL.GL_STATIC_DRAW;

      ByteBuffer pnbuff = ellipsoid.vbuffs[0];

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
         idxPutter.putIndices(ellipsoid.ebuff, 0, j+1, i+1);
      }

      // middle
      for (int l=0; l<nLevels-2; ++l) {
         int boff = 1+l*nSlices;
         for (int i=0; i<nSlices; ++i) {
            int j = (i + 1) % nSlices;
            idxPutter.putIndices(ellipsoid.ebuff,
               boff+j+nSlices, boff+i+nSlices, boff+i,
               boff+j, boff+j+nSlices, boff+i);
         }
      }

      // top
      int boff = 1+nSlices*(nLevels-2);
      int toff = boff+nSlices;
      for (int i=0; i<nSlices; ++i) {
         int j = (i + 1) % nSlices;
         idxPutter.putIndices(ellipsoid.ebuff, boff+j, toff, boff+i);
      }
      
      pnbuff.rewind();
      ellipsoid.ebuff.rewind();

      return ellipsoid;
   }

   // tapered ellipsoid (along zaxis)
   public static GL3Object createTaperedEllipsoid(GL3 gl, int nSlices, int nLevels) {
      GL3PreObject pglo = createTaperedEllipsoid(nSlices, nLevels);
      return pglo.generate(gl);
   }

   // sphere
   private static GL3PreObject createSphere(int nSlices, int nLevels) {

      if (nLevels < 2) {
         nLevels = 2;
      }
      
      GL3PreObject sphere = new GL3PreObject();
      sphere.mode = GL.GL_TRIANGLES;
      int nverts = 2+nSlices*(nLevels-1);
      int nelems = 6*nSlices*(nLevels-1);
      
      PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      IndexBufferPutter idxPutter = IndexBufferPutter.createDefault(nverts-1);

      BufferStorage posStorage = posPutter.storage();
      BufferStorage idxStorage = idxPutter.storage();

      int vstride = posStorage.bytes();

      sphere.ainfo = new GL3VertexAttribute.AttributeInfo[2];
      sphere.vinfo = new GL3VertexAttributeArray.VBOInfo[2];
      
      sphere.ainfo[0] = GL3VertexAttribute.VERTEX_POSITION;
      sphere.vinfo[0] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(posStorage.type()), 
         posStorage.size(), posStorage.isNormalized(), 0, vstride, nverts);
      sphere.ainfo[1] = GL3VertexAttribute.VERTEX_NORMAL;
      sphere.vinfo[1] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(posStorage.type()), 
         posStorage.size(), posStorage.isNormalized(), 0, vstride, nverts);
      sphere.vbuffIdxs = new int[] {0, 0};

      sphere.vbuffs = new ByteBuffer[1];
      sphere.vbuffs[0] = ByteBuffer.allocateDirect(nverts*vstride);
      sphere.vbuffs[0].order(ByteOrder.nativeOrder());
      sphere.vbuffUsage = new int[] {GL.GL_STATIC_DRAW};

      sphere.einfo = new GL3ElementAttributeArray.ElementInfo(GL3Util.getGLType(idxStorage.type()), 0, idxStorage.bytes(), nelems);
      sphere.ebuff = ByteBuffer.allocateDirect(nelems*idxStorage.bytes());
      sphere.ebuff.order(ByteOrder.nativeOrder());
      sphere.ebuffUsage = GL.GL_STATIC_DRAW;

      ByteBuffer pnbuff = sphere.vbuffs[0];

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
         idxPutter.putIndices(sphere.ebuff, 0, j+1, i+1);
      }

      // middle
      for (int l=0; l<nLevels-2; ++l) {
         int boff = 1+l*nSlices;
         for (int i=0; i<nSlices; ++i) {
            int j = (i + 1) % nSlices;
            idxPutter.putIndices(sphere.ebuff,
               boff+j+nSlices, boff+i+nSlices, boff+i,
               boff+j, boff+j+nSlices, boff+i);
         }
      }

      // top
      int boff = 1+nSlices*(nLevels-2);
      int toff = boff+nSlices;
      for (int i=0; i<nSlices; ++i) {
         int j = (i + 1) % nSlices;
         idxPutter.putIndices(sphere.ebuff, boff+j, toff, boff+i);
      }
      
      pnbuff.rewind();
      sphere.ebuff.rewind();

      return sphere;
   
   }

   // sphere
   public static GL3Object createSphere(GL3 gl, int nSlices, int nLevels) {

      GL3PreObject pglo = createSphere(nSlices, nLevels);
      return pglo.generate(gl);

   }
   
   private static GL3PreObject createAxes(boolean x, boolean y, boolean z) {
     
      GL3PreObject axes = new GL3PreObject();
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

      BufferStorage posStorage = posPutter.storage();
      BufferStorage clrStorage = clrPutter.storage();

      int vstride = posStorage.bytes()+clrStorage.bytes();

      axes.ainfo = new GL3VertexAttribute.AttributeInfo[2];
      axes.vinfo = new GL3VertexAttributeArray.VBOInfo[2];
      
      axes.ainfo[0] = GL3VertexAttribute.VERTEX_POSITION;
      axes.vinfo[0] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(posStorage.type()), 
         posStorage.size(), posStorage.isNormalized(), 0, vstride, nverts);
      axes.ainfo[1] = GL3VertexAttribute.VERTEX_COLOR;
      axes.vinfo[1] = new GL3VertexAttributeArray.VBOInfo(
         GL3Util.getGLType(clrStorage.type()), 
         clrStorage.size(), clrStorage.isNormalized(), posStorage.bytes(), vstride, nverts);
      axes.vbuffIdxs = new int[] {0, 0};

      axes.vbuffs = new ByteBuffer[1];
      axes.vbuffs[0] = ByteBuffer.allocateDirect(nverts*vstride);
      axes.vbuffs[0].order(ByteOrder.nativeOrder());
      axes.vbuffUsage = new int[] {GL.GL_STATIC_DRAW};

      axes.einfo = null;

      ByteBuffer pcbuff = axes.vbuffs[0];
      
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
      
      pcbuff.rewind();
      
      return axes;
   }
   
   public static GL3Object createAxes(GL3 gl, boolean x, boolean y, boolean z) {
      GL3PreObject pglo = createAxes(x, y, z);
      return pglo.generate(gl);
   }

}
