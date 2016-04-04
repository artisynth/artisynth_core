package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import javax.media.opengl.GL3;

import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectVersion;
import maspack.util.BufferUtilities;

//=====================================================================================================================
// VBO layouts, ? if exists
// --primary positions/normals/colors/textures interleaved, separated into static/dynamic buffer
// --separate IBO for all primitive indices, points/lines/triangles
//
//  static         dynamic       primitives
//  [pos[v0] ]     [pos[v0] ]    [pnt[0][0]? ]
//  [nrm[v0]?]     [nrm[v0]?]      ...
//  [clr[v0]?]     [clr[v0]?]    [pnt[0][r]? ]
//  [tex[v0]?]     [tex[v0]?]      ... ...
//  [pos[v1] ]     [pos[v1] ]    [pnt[s][t]? ]
//  [nrm[v1]?]     [nrm[v1]?]    [line[0][0]?]
//  [clr[v1]?]     [clr[v1]?]      ...
//  [tex[v1]?]     [tex[v1]?]    [line[0][u]?]
//    ...            ...           ... ...
//  [pos[vn] ]     [pos[vn] ]    [line[v][w]?]
//  [nrm[vn]?]     [nrm[vn]?]    [tri[0][0]? ]
//  [clr[vn]?]     [clr[vn]?]      ... ...
//  [tex[vn]?]     [tex[vn]?]    [tri[x][y]? ]
//
//=====================================================================================================================
public class GL3SharedRenderObjectIndexed extends GL3SharedRenderObjectBase {

   AttributeInfo[] pointsInfo;
   AttributeInfo[] linesInfo;
   AttributeInfo[] trianglesInfo;

   IndexBufferObject ibo;

   protected GL3SharedRenderObjectIndexed(RenderObject r,
      VertexBufferObject staticVBO, VertexBufferObject dynamicVBO, IndexBufferObject ibo,
      GL3VertexAttributeInfo posAttribute, GL3VertexAttributeInfo nrmAttribute, 
      GL3VertexAttributeInfo clrAttribute, GL3VertexAttributeInfo texAttribute) {
      super(r.getIdentifier (), posAttribute, nrmAttribute,
         clrAttribute, texAttribute, staticVBO, dynamicVBO);

      pointsInfo = null;
      linesInfo = null;
      trianglesInfo = null;
      
      this.ibo = ibo.acquire ();
   }
   
   public boolean maybeUpdate(GL3 gl, RenderObject robj) {
      boolean updated = false;
      
      robj.readLock (); {
         RenderObjectVersion rv = robj.getVersionInfo ();
         updated = maybeUpdateVertices (gl, robj, rv);
         updated |= maybeUpdatePrimitives (gl, robj);
         if (updated) {
            lastVersionInfo = rv;
         }
      }
      robj.readUnlock ();
      return updated;
   }

   // maybe update VBOs
   protected boolean maybeUpdatePrimitives(GL3 gl, RenderObject robj) {

      RenderObjectVersion rv = robj.getVersionInfo ();
      boolean updatePrimitives = (lastVersionInfo == null 
         || lastVersionInfo.getPointsVersion() != rv.getPointsVersion()
         || lastVersionInfo.getLinesVersion() != rv.getLinesVersion()
         || lastVersionInfo.getTrianglesVersion() != rv.getTrianglesVersion());

      if (updatePrimitives) {
         fillPrimitiveVBO (gl, robj);
      }

      return updatePrimitives;
   }

   @Override
   protected void updateDynamicVertices(GL3 gl, RenderObject robj, int updateMask, boolean replace) {

      robj.readLock (); {
         int vertStride = robj.getVertexStride ();
         int[] verts = robj.getVertexBuffer ();

         ByteBuffer buff = null;
         if (replace) {
            buff = vbos[DYNAMIC_VBO_IDX].mapNewBuffer (gl);
         } else {
            buff = vbos[DYNAMIC_VBO_IDX].mapBuffer (gl, GL3.GL_WRITE_ONLY);
         }

         if ( (updateMask & POSITION_FLAG) != 0) {
            int bidx = positionInfo.offset;
            int pidx = robj.getVertexPositionOffset();

            for (int i=0; i<positionInfo.count; ++i) {
               float[] pos = robj.getPosition (verts[pidx]);
               buff.position (bidx);
               positionPutter.putPosition(buff, pos);

               pidx += vertStride;
               bidx += positionInfo.stride;
            }
         }

         // normal
         if ( (updateMask & NORMAL_FLAG) != 0) {
            int bidx = normalInfo.offset;
            int pidx = robj.getVertexNormalOffset();

            for (int i=0; i<normalInfo.count; ++i) {
               float[] pos = robj.getNormal (verts[pidx]);
               buff.position (bidx);
               normalPutter.putNormal(buff, pos);

               pidx += vertStride;
               bidx += normalInfo.stride;
            }  
         }

         // color
         if ( (updateMask & COLOR_FLAG) != 0) {
            int bidx = colorInfo.offset;
            int pidx = robj.getVertexColorOffset();

            for (int i=0; i<colorInfo.count; ++i) {
               byte[] pos = robj.getColor (verts[pidx]);
               buff.position (bidx);
               colorPutter.putColor(buff, pos);

               pidx += vertStride;
               bidx += colorInfo.stride;
            }
         }

         // texture
         if ( (updateMask & TEXTURE_FLAG) != 0) {
            int bidx = textureInfo.offset;
            int pidx = robj.getVertexTextureCoordOffset ();

            for (int i=0; i<textureInfo.count; ++i) {
               float[] pos = robj.getTextureCoord (verts[pidx]);
               buff.position (bidx);
               texturePutter.putTextureCoord(buff, pos);
               pidx += vertStride;
               bidx += textureInfo.stride;
            }
         }

         // unmap
         vbos[DYNAMIC_VBO_IDX].unmapBuffer (gl);
      } robj.readUnlock ();
   }

   public boolean isValid() {
      if (!super.isValid ()) {
         return false;
      }
      if (ibo == null) {
         return false;
      }

      return true;
   }

   protected void clearAll(GL3 gl) {
      super.clearAll(gl);

      pointsInfo = null;
      linesInfo = null;
      trianglesInfo = null;
   }
   
   @Override
   public void dispose (GL3 gl) {
      super.dispose (gl);
      
      if (ibo != null) {
         ibo.releaseDispose (gl);
         ibo = null;
      }
   }

   @Override
   protected void buildVertices(GL3 gl, RenderObject robj) {
      buildVertexInfo(gl, robj, robj.numVertices ());
      fillVertexVBO(gl, robj);
   }

   private void fillVertexVBO(GL3 gl, RenderObject robj) {
      
      int nVertices = robj.numVertices ();
      boolean streaming = robj.isTransient ();
      
      // create buffers for vertex VBOs
      ByteBuffer[] buffs = new ByteBuffer[2];
      if (staticVertexSize > 0) {
         buffs[STATIC_VBO_IDX] = BufferUtilities.newNativeByteBuffer(nVertices*staticVertexSize);
      }
      // dynamic
      if (dynamicVertexSize > 0) {
         buffs[DYNAMIC_VBO_IDX] = BufferUtilities.newNativeByteBuffer(nVertices*dynamicVertexSize);
      }

      int vertStride = robj.getVertexStride ();
      int[] verts = robj.getVertexBuffer ();

      // fill vertex buffers      
      if ( positionInfo != null ) {
         int bidx = positionInfo.offset;
         int pidx = robj.getVertexPositionOffset();
         ByteBuffer buff = buffs[positionInfo.vboIndex];

         for (int i=0; i<positionInfo.count; ++i) {
            float[] pos = robj.getPosition (verts[pidx]);
            buff.position (bidx);
            positionPutter.putPosition(buff, pos);

            pidx += vertStride;
            bidx += positionInfo.stride;
         }
      }

      // normal
      if ( normalInfo != null ) {
         int bidx = normalInfo.offset;
         int pidx = robj.getVertexNormalOffset();
         ByteBuffer buff = buffs[normalInfo.vboIndex];

         for (int i=0; i<normalInfo.count; ++i) {
            float[] pos = robj.getNormal (verts[pidx]);
            buff.position (bidx);
            normalPutter.putNormal(buff, pos);

            pidx += vertStride;
            bidx += normalInfo.stride;
         }  
      }

      // color
      if ( colorInfo != null ) {
         int bidx = colorInfo.offset;
         int pidx = robj.getVertexColorOffset();
         ByteBuffer buff = buffs[colorInfo.vboIndex];

         for (int i=0; i<colorInfo.count; ++i) {
            byte[] pos = robj.getColor (verts[pidx]);
            buff.position (bidx);
            colorPutter.putColor(buff, pos);

            pidx += vertStride;
            bidx += colorInfo.stride;
         }
      }

      // texture
      if ( textureInfo != null ) {
         int bidx = textureInfo.offset;
         int pidx = robj.getVertexTextureCoordOffset ();
         ByteBuffer buff = buffs[textureInfo.vboIndex];

         for (int i=0; i<textureInfo.count; ++i) {
            float[] pos = robj.getTextureCoord (verts[pidx]);
            buff.position (bidx);
            texturePutter.putTextureCoord(buff, pos);
            pidx += vertStride;
            bidx += textureInfo.stride;
         }
      }

      // vertex buffer object
      gl.glBindVertexArray (0); // unbind any existing VAOs
      if (staticVertexSize > 0) {
         buffs[STATIC_VBO_IDX].flip();
         vbos[STATIC_VBO_IDX].fill(gl, buffs[STATIC_VBO_IDX],
            getBufferUsage(false, streaming));
         BufferUtilities.freeDirectBuffer (buffs[STATIC_VBO_IDX]);
      }
      // dynamic
      if (dynamicVertexSize > 0) {
         buffs[DYNAMIC_VBO_IDX].flip();
         vbos[DYNAMIC_VBO_IDX].fill(gl, buffs[DYNAMIC_VBO_IDX],
            getBufferUsage(true, streaming));
         BufferUtilities.freeDirectBuffer (buffs[DYNAMIC_VBO_IDX]);
      }
   }

   private void fillPrimitiveVBO(GL3 gl, RenderObject robj) {

      int nVertices = robj.numVertices ();
      boolean streaming = robj.isTransient ();
      
      IndexBufferPutter indexPutter = IndexBufferPutter.getDefault(nVertices-1);
      int gltype = indexPutter.storage ().getGLType ();
      final int INDEX_BYTES = indexPutter.bytesPerIndex();

      pointsInfo = createAttributeInfoArrays(robj.numPointGroups());
      linesInfo = createAttributeInfoArrays(robj.numLineGroups());
      trianglesInfo = createAttributeInfoArrays(robj.numTriangleGroups());

      // primitives
      int primitiveOffset = 0;
      for (int i=0; i<pointsInfo.length; ++i) {
         pointsInfo[i].offset = primitiveOffset;
         pointsInfo[i].stride = INDEX_BYTES;
         pointsInfo[i].vboIndex = 0;
         pointsInfo[i].count = robj.numPoints(i);
         pointsInfo[i].type = gltype;
         primitiveOffset += robj.numPoints(i)*INDEX_BYTES;
      }
      for (int i=0; i<linesInfo.length; ++i) {
         linesInfo[i].offset = primitiveOffset;
         linesInfo[i].stride = INDEX_BYTES;
         linesInfo[i].vboIndex = 0;
         linesInfo[i].count = 2*robj.numLines(i);
         linesInfo[i].type = gltype;
         primitiveOffset += 2*robj.numLines(i)*INDEX_BYTES;
      }
      for (int i=0; i<trianglesInfo.length; ++i) {
         trianglesInfo[i].offset = primitiveOffset;
         trianglesInfo[i].stride = INDEX_BYTES;
         trianglesInfo[i].vboIndex = 0;
         trianglesInfo[i].count = 3*robj.numTriangles(i);
         trianglesInfo[i].type = gltype;
         primitiveOffset += 3*robj.numTriangles(i)*INDEX_BYTES;
      }

      // primitives
      if (robj.hasPoints() || robj.hasLines() || robj.hasTriangles()) {

         int size = 0;
         for (int i=0; i<robj.numPointGroups(); ++i) {
            size += robj.numPoints(i)*INDEX_BYTES;
         }
         for (int i=0; i<robj.numLineGroups(); ++i) {
            size += 2*robj.numLines(i)*INDEX_BYTES;
         }
         for (int i=0; i<robj.numTriangleGroups(); ++i) {
            size += 3*robj.numTriangles(i)*INDEX_BYTES;
         }

         ByteBuffer buff = BufferUtilities.newNativeByteBuffer(size);

         for (int i=0; i<pointsInfo.length; ++i) {
            AttributeInfo ai = pointsInfo[i];
            indexPutter.putIndices(buff, ai.offset, robj.getPoints(i));
         }
         for (int i=0; i<linesInfo.length; ++i) {
            AttributeInfo ai = linesInfo[i];
            indexPutter.putIndices(buff, ai.offset, robj.getLines(i));
         }
         for (int i=0; i<trianglesInfo.length; ++i) {
            AttributeInfo ai = trianglesInfo[i];
            indexPutter.putIndices(buff, ai.offset, robj.getTriangles(i));
         }

         buff.flip();
         gl.glBindVertexArray (0); // unbind any existing VAOs
         ibo.fill(gl, buff, getBufferUsage(false, streaming));
         BufferUtilities.freeDirectBuffer (buff);
         
         incrementBindVersion ();
      }
   }

   public void bindIndices(GL3 gl) {
      // bind index buffer
      if (ibo != null) {
         ibo.bind (gl);
      }
   }
   
   @Override
   public GL3SharedRenderObjectIndexed acquire () {
      return (GL3SharedRenderObjectIndexed)super.acquire ();
   }

   public void drawPointGroup(GL3 gl, int mode, int gidx) {
      gl.glDrawElements (mode, pointsInfo[gidx].count, pointsInfo[gidx].type, pointsInfo[gidx].offset);
   }

   public void drawLineGroup(GL3 gl, int mode, int gidx) {
      gl.glDrawElements (mode, linesInfo[gidx].count, linesInfo[gidx].type, linesInfo[gidx].offset);
   }

   public void drawTriangleGroup(GL3 gl, int mode, int gidx) {
      gl.glDrawElements (mode, trianglesInfo[gidx].count, trianglesInfo[gidx].type, trianglesInfo[gidx].offset);
   }
   
   public void drawVertices(GL3 gl, int mode) {
      gl.glDrawArrays (mode, 0, positionInfo.count);
   }

   public static GL3SharedRenderObjectIndexed generate (
      GL3 gl, RenderObject robj, GL3VertexAttributeInfo position,
      GL3VertexAttributeInfo normal, GL3VertexAttributeInfo color,
      GL3VertexAttributeInfo texcoord) {
      
      VertexBufferObject staticVBO = VertexBufferObject.generate (gl);
      VertexBufferObject dynamicVBO = VertexBufferObject.generate (gl);
      IndexBufferObject ibo = IndexBufferObject.generate (gl);
      GL3SharedRenderObjectIndexed out = 
         new GL3SharedRenderObjectIndexed (robj, staticVBO, dynamicVBO, 
            ibo, position, normal, color, texcoord);
      out.maybeUpdate (gl, robj);  // trigger a build
      return out;
   }

}
