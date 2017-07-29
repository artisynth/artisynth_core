package maspack.render.GL.GL3;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectIdentifier;
import maspack.render.RenderObject.RenderObjectVersion;

public abstract class GL3SharedRenderObjectBase extends GL3ResourceBase {
   
   private RenderObjectIdentifier roId;
   int bindVersion;
   
   protected static PositionBufferPutter positionPutter = PositionBufferPutter.getDefault ();
   protected static NormalBufferPutter normalPutter = NormalBufferPutter.getDefault ();
   protected static ColorBufferPutter colorPutter = ColorBufferPutter.getDefault ();
   protected static TextureCoordBufferPutter texturePutter = TextureCoordBufferPutter.getDefault ();
   
   protected static class AttributeInfo {
      int vboIndex;
      int offset;       // in bytes
      int stride;       // in bytes
      int count;        // number of items
      int type;         // (gl) type of data
   }
   
   protected int staticVertexSize;
   protected int dynamicVertexSize;
   protected int dynamicMask;
   protected boolean streaming; 
   
   protected static final int STATIC_VBO_IDX = 0;
   protected static final int DYNAMIC_VBO_IDX = 1;
   
   protected static final int POSITION_FLAG = 0x01;
   protected static final int NORMAL_FLAG = 0x02;
   protected static final int COLOR_FLAG = 0x04;
   protected static final int TEXCOORDS_FLAG = 0x08;
   
   protected AttributeInfo positionInfo;
   protected AttributeInfo normalInfo;
   protected AttributeInfo colorInfo;
   protected AttributeInfo textureInfo;
   
   protected GL3VertexAttributeInfo posAttribute;
   protected GL3VertexAttributeInfo nrmAttribute;
   protected GL3VertexAttributeInfo clrAttribute;
   protected GL3VertexAttributeInfo texAttribute;
   
   // track version numbers so we can detect what has changed since last use
   RenderObjectVersion lastVersionInfo;
   
   protected VertexBufferObject[] vbos;
   
   protected GL3SharedRenderObjectBase(RenderObjectIdentifier roId, 
      GL3VertexAttributeInfo posAttribute, GL3VertexAttributeInfo nrmAttribute, 
      GL3VertexAttributeInfo clrAttribute, GL3VertexAttributeInfo texAttribute,
      VertexBufferObject staticVBO, VertexBufferObject dynamicVBO) {
      
      this.roId = roId;
      this.posAttribute = posAttribute;
      this.nrmAttribute = nrmAttribute;
      this.clrAttribute = clrAttribute;
      this.texAttribute = texAttribute;

      positionInfo = null;
      normalInfo = null;
      colorInfo = null;
      textureInfo = null;

      lastVersionInfo = null;

      // start with everything static
      staticVertexSize = 0;
      dynamicVertexSize = 0;
      dynamicMask = 0;
      streaming = false;

      staticVBO.acquire ();
      dynamicVBO.acquire ();
      vbos = new VertexBufferObject[] {staticVBO, dynamicVBO};
      
      bindVersion = 0;
   }
   
   public RenderObjectIdentifier getRenderObjectIdentifier() {
      return roId;
   }
   
   @Override
   public boolean isValid() {
      if (vbos == null) {
         return false;
      }
      for (BufferObject vbo : vbos) {
         if (vbo != null) {
            if (!vbo.isValid ()) {
               return false;
            }
         }
      }
      return true;
   }
   
   protected boolean needsVertexRebuild(GL3 gl, RenderObjectVersion rv) {
      if (lastVersionInfo == null) {
         return true;
      }
      
      // always rebuild if streaming vertices
      if (streaming) {
         return true;
      }
      
      boolean rebuild = false;
      
      // vertices have changed
      if (rv.getVerticesVersion () != lastVersionInfo.getVerticesVersion()) {
         streaming = true;
         dynamicMask = 0;
         return rebuild = true;
      }
      
      // trying to update static component
      if (rv.getPositionsVersion() != lastVersionInfo.getPositionsVersion()) {
         boolean positionsDynamic = ((dynamicMask & POSITION_FLAG) != 0);
         // positions are static
         if  (!positionsDynamic) {
            rebuild = true;
            dynamicMask |= POSITION_FLAG;
         }
      }
      if (rv.getNormalsVersion() != lastVersionInfo.getNormalsVersion()) {
         boolean normalsDynamic = ((dynamicMask & NORMAL_FLAG) != 0);
         if (!normalsDynamic) {
            rebuild = true;
            dynamicMask |= NORMAL_FLAG;
         }
      }
      if (rv.getColorsVersion() != lastVersionInfo.getColorsVersion()) {
         boolean colorsDynamic = ((dynamicMask & COLOR_FLAG) != 0);
         if (!colorsDynamic) {
            rebuild = true;
            dynamicMask |= COLOR_FLAG;
         }
      }
      if (rv.getTextureCoordsVersion() != lastVersionInfo.getTextureCoordsVersion()) {
         boolean texturesDynamic = ((dynamicMask & TEXCOORDS_FLAG) != 0);
         if (!texturesDynamic) {
            rebuild = true;
            dynamicMask |= TEXCOORDS_FLAG;
         }
      }
      
      
      return rebuild;
   }
   
   protected boolean maybeUpdateVertices(GL3 gl, RenderObject robj, RenderObjectVersion rv) {
      
      if (lastVersionInfo != null && rv.getVersion () == lastVersionInfo.getVersion ()) {
         return false;
      }
      
      if (robj.isTransient ()) {
         streaming = true;
      }
      
      if (needsVertexRebuild(gl, rv)) {
         clearVertices(gl);
         buildVertices(gl, robj);
         incrementBindVersion ();
         return true;
      } 
      
      // check if we should update all dynamic, or update part dynamic
      int updateFlag = 0;
      if (positionInfo != null && rv.getPositionsVersion() != lastVersionInfo.getPositionsVersion()) {
         updateFlag |= POSITION_FLAG;
      }

      if (normalInfo != null && rv.getNormalsVersion() != lastVersionInfo.getNormalsVersion()) {
         updateFlag |= NORMAL_FLAG;
      }

      if (colorInfo != null && rv.getColorsVersion() != lastVersionInfo.getColorsVersion()) {
         updateFlag |= COLOR_FLAG;
      }

      if (textureInfo != null && robj.getTextureCoordsVersion() != lastVersionInfo.getTextureCoordsVersion()) {
         updateFlag |= TEXCOORDS_FLAG;
      }
      
      // if at least one dynamic component needs to be updated, do so here
      boolean update = (updateFlag != 0);
      if (update) {
         updateDynamicVertices(gl, robj, updateFlag, updateFlag == dynamicMask);
      }
     
      return update;
   }
   
   protected void buildVertexInfo(GL3 gl, RenderObject robj, int nVertices) {
      // buffer manipulators
      final int POSITION_BYTES = positionPutter.bytesPerPosition();
      final int NORMAL_BYTES = normalPutter.bytesPerNormal();
      final int COLOR_BYTES = colorPutter.bytesPerColor();
      final int TEXTURE_BYTES = texturePutter.bytesPerTextureCoord();

      // build up position info
      positionInfo = null;
      normalInfo = null;
      colorInfo = null;
      textureInfo = null;

      staticVertexSize = 0;
      dynamicVertexSize = 0;

      boolean positionsDynamic = !streaming && ((dynamicMask & POSITION_FLAG) != 0);
      boolean normalsDynamic = !streaming && ((dynamicMask & NORMAL_FLAG) != 0);
      boolean colorsDynamic = !streaming && ((dynamicMask & COLOR_FLAG) != 0);
      boolean texcoordDynamic = !streaming && ((dynamicMask & TEXCOORDS_FLAG) != 0);
      
      // determine necessary sizes
      if (robj.hasPositions()) {
         positionInfo = new AttributeInfo ();
         if (positionsDynamic) {
            positionInfo.vboIndex = DYNAMIC_VBO_IDX;
            positionInfo.offset = dynamicVertexSize;
            dynamicVertexSize += POSITION_BYTES;  // 3 floats per position
         } else {
            positionInfo.vboIndex = STATIC_VBO_IDX;
            positionInfo.offset = staticVertexSize;
            staticVertexSize += POSITION_BYTES;   // 3 floats per position
         }
         positionInfo.count = nVertices;
         positionInfo.type = positionPutter.storage ().getGLType ();
      }

      if (robj.hasNormals()) {
         normalInfo = new AttributeInfo ();
         if (normalsDynamic) {
            normalInfo.vboIndex = DYNAMIC_VBO_IDX;
            normalInfo.offset = dynamicVertexSize;
            dynamicVertexSize += NORMAL_BYTES;  // 3 shorts per normal, +1 for 4-byte alignment
         } else {
            normalInfo.vboIndex = STATIC_VBO_IDX;
            normalInfo.offset = staticVertexSize;
            staticVertexSize += NORMAL_BYTES; 
         }
         normalInfo.count = nVertices;
         normalInfo.type = normalPutter.storage ().getGLType ();
      }

      if (robj.hasColors()) {
         colorInfo = new AttributeInfo();
         if (colorsDynamic) {
            colorInfo.vboIndex = DYNAMIC_VBO_IDX;
            colorInfo.offset = dynamicVertexSize;
            dynamicVertexSize += COLOR_BYTES;   // 4 bytes per color
         } else {
            colorInfo.vboIndex = STATIC_VBO_IDX;
            colorInfo.offset = staticVertexSize;
            staticVertexSize += COLOR_BYTES;
         }
         colorInfo.count = nVertices;
         colorInfo.type = colorPutter.storage ().getGLType ();
      }

      if (robj.hasTextureCoords()) {
         textureInfo = new AttributeInfo();
         if (texcoordDynamic) {
            textureInfo.vboIndex = DYNAMIC_VBO_IDX;
            textureInfo.offset = dynamicVertexSize;
            dynamicVertexSize += TEXTURE_BYTES;  // 2 shorts per texture
         } else {
            textureInfo.vboIndex = STATIC_VBO_IDX;
            textureInfo.offset = staticVertexSize;
            staticVertexSize += TEXTURE_BYTES; 
         }
         textureInfo.count = nVertices;
         textureInfo.type = texturePutter.storage ().getGLType ();
      }

      // set strides
      if (robj.hasPositions()) {
         if (positionsDynamic) {
            positionInfo.stride = dynamicVertexSize;
         } else {
            positionInfo.stride = staticVertexSize;
         }
      }
      if (robj.hasNormals()) {
         if (normalsDynamic) {
            normalInfo.stride = dynamicVertexSize;
         } else {
            normalInfo.stride = staticVertexSize;
         }
      }
      if (robj.hasColors()) {
         if (colorsDynamic) {
            colorInfo.stride = dynamicVertexSize;
         } else {
            colorInfo.stride = staticVertexSize;
         }
      }
      if (robj.hasTextureCoords()) {
         if (texcoordDynamic) {
            textureInfo.stride = dynamicVertexSize;
         } else {
            textureInfo.stride = staticVertexSize;
         }
      }
      
   }
   
   /**
    * To be called whenever some attributes may need to be re-bound
    */
   protected void incrementBindVersion() {
      ++bindVersion;
   }
      
   /**
    * @return version incremented when attributes need to be re-bound
    */
   public int getBindVersion() {
      return bindVersion;
   }
   
   protected abstract void buildVertices(GL3 gl, RenderObject robj);   
   protected abstract void updateDynamicVertices(GL3 gl, RenderObject robj, int updateFlag, boolean replace);
   
   protected void clearVertices(GL3 gl) {
      dynamicVertexSize = 0;
      staticVertexSize = 0;

      positionInfo = null;
      normalInfo = null;
      colorInfo = null;
      textureInfo = null;
      
      lastVersionInfo = null;
   }
   
   protected static AttributeInfo[] createAttributeInfoArrays(int n) {
      AttributeInfo[] ai = new AttributeInfo[n];
      for (int i=0; i<ai.length; ++i) {
         ai[i] = new AttributeInfo();
      }
      return ai;
   }
   
   protected static int getBufferUsage(boolean dynamic, boolean streaming) {
      if (streaming) {
         return GL3.GL_STREAM_DRAW;
      }
      if (dynamic) {
         return GL3.GL_DYNAMIC_DRAW;
      }
      return GL3.GL_STATIC_DRAW;
   }
   
   protected void clearAll(GL3 gl) {
      clearVertices(gl);
   }
   
   public void dispose(GL3 gl) {
      clearAll(gl);
      
      // clear shared VBOs
      if (vbos != null) {
         for (BufferObject vbo : vbos) {
            if (vbo != null) {
               vbo.releaseDispose(gl);
            }
         }
         vbos = null;
      }
   }
   
   @Override
   public boolean isDisposed () {
      return vbos == null;
   }
   
   @Override
   public GL3SharedRenderObjectBase acquire () {
      return (GL3SharedRenderObjectBase)super.acquire ();
   }

   public void bindVertices(GL3 gl) {
      if (positionInfo != null) {
         int loc = posAttribute.getLocation ();
         GL3AttributeStorage storage = positionPutter.storage ();
         vbos[positionInfo.vboIndex].bind (gl);
         GL3Utilities.activateVertexAttribute(gl, loc, storage,
            positionInfo.stride, positionInfo.offset);
      }

      if (normalInfo != null) {
         int loc = nrmAttribute.getLocation ();
         GL3AttributeStorage storage = normalPutter.storage ();
         vbos[normalInfo.vboIndex].bind (gl);
         GL3Utilities.activateVertexAttribute(gl, loc, storage,
            normalInfo.stride, normalInfo.offset);
      }

      if (colorInfo != null) {
         int loc = clrAttribute.getLocation ();
         GL3AttributeStorage storage = colorPutter.storage ();
         vbos[colorInfo.vboIndex].bind (gl);
         GL3Utilities.activateVertexAttribute(gl, loc, storage, 
            colorInfo.stride, colorInfo.offset);
      }

      if (textureInfo != null) {
         int loc = texAttribute.getLocation ();
         GL3AttributeStorage storage = texturePutter.storage ();
         vbos[textureInfo.vboIndex].bind (gl);
         GL3Utilities.activateVertexAttribute(gl, loc, storage,
            textureInfo.stride, textureInfo.offset);
      }
   }
   
   public void unbindVertices(GL3 gl) {
      if (positionInfo != null) {
         int loc = posAttribute.getLocation ();
         gl.glDisableVertexAttribArray (loc);
      }

      if (normalInfo != null) {
         int loc = nrmAttribute.getLocation ();
         gl.glDisableVertexAttribArray (loc);
      }

      if (colorInfo != null) {
         int loc = clrAttribute.getLocation ();
         gl.glDisableVertexAttribArray (loc);
      }

      if (textureInfo != null) {
         int loc = texAttribute.getLocation ();
         gl.glDisableVertexAttribArray (loc);
      }
      
      gl.glBindBuffer (GL.GL_ARRAY_BUFFER, 0);
   }
   
}