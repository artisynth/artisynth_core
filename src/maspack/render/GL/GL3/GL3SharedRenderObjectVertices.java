package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL3;

import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectIdentifier;
import maspack.render.RenderObject.RenderObjectVersion;
import maspack.util.BufferUtilities;

public class GL3SharedRenderObjectVertices extends GL3SharedRenderObjectBase {

   protected GL3SharedRenderObjectVertices(RenderObjectIdentifier rId,
      VertexBufferObject staticVBO, VertexBufferObject dynamicVBO,
      GL3VertexAttributeInfo posAttribute, GL3VertexAttributeInfo nrmAttribute, 
      GL3VertexAttributeInfo clrAttribute, GL3VertexAttributeInfo texAttribute) {
      super(rId, posAttribute, nrmAttribute,
         clrAttribute, texAttribute, staticVBO, dynamicVBO);
   }
   
   public boolean maybeUpdate(GL3 gl, RenderObject robj) {
      boolean updated = false;
      
      robj.readLock (); {
         RenderObjectVersion rv = robj.getVersionInfo ();
         updated = maybeUpdateVertices (gl, robj, rv);
         if (updated) {
            lastVersionInfo = rv;
         }
      }
      robj.readUnlock ();
      return updated;
   }

   @Override
   protected void updateDynamicVertices(GL3 gl, RenderObject robj, int updateMask, boolean replace) {

      robj.readLock (); {
         int vertStride = robj.getVertexStride ();
         int[] verts = robj.getVertexBuffer ();

         ByteBuffer buff = null;
         if (replace) {
            // buff = vbos[DYNAMIC_VBO_IDX].mapNewBuffer (gl);
            buff = BufferUtilities.newNativeByteBuffer (vbos[DYNAMIC_VBO_IDX].getSize ());
         } else {
            buff = vbos[DYNAMIC_VBO_IDX].mapBuffer (gl, GL3.GL_WRITE_ONLY);
         }

         if ( positionInfo != null && (updateMask & POSITION_FLAG) != 0) {
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
         if ( normalInfo != null && (updateMask & NORMAL_FLAG) != 0) {
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
         if ( colorInfo != null && (updateMask & COLOR_FLAG) != 0) {
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
         if ( textureInfo != null && (updateMask & TEXCOORDS_FLAG) != 0) {
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
         if (replace) {
            buff.flip ();
            vbos[DYNAMIC_VBO_IDX].update (gl, buff);
            buff = BufferUtilities.freeDirectBuffer (buff);
         } else {
            vbos[DYNAMIC_VBO_IDX].unmapBuffer (gl);
         }
      } robj.readUnlock ();
   }

   public boolean isValid() {
      if (!super.isValid ()) {
         return false;
      }

      return true;
   }

   protected void clearAll(GL3 gl) {
      super.clearAll(gl);
   }
   
   @Override
   public void dispose (GL3 gl) {
      super.dispose (gl);
   }

   @Override
   protected void buildVertices(GL3 gl, RenderObject robj) {
      buildVertexInfo(gl, robj, robj.numVertices ());
      fillVertexVBO(gl, robj);
   }

   private void fillVertexVBO(GL3 gl, RenderObject robj) {
      
      int nVertices = robj.numVertices ();
      
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
 
   @Override
   public GL3SharedRenderObjectPrimitives acquire () {
      return (GL3SharedRenderObjectPrimitives)super.acquire ();
   }
 
   public void drawVertices(GL3 gl, int mode) {
      gl.glDrawArrays (mode, 0, positionInfo.count);
   }
   
   public void drawElements(GL3 gl, int mode, int count, int type, int offset) {
      gl.glDrawElements (mode, count, type, offset);
   }

   public void drawInstancedElements(GL3 gl, int mode, int count, int type, 
      int offset, int instanceCount) {
      gl.glDrawElementsInstanced(mode, count, type, offset, instanceCount);
   }

   public static GL3SharedRenderObjectVertices generate (
      GL3 gl, RenderObject robj, GL3VertexAttributeInfo position,
      GL3VertexAttributeInfo normal, GL3VertexAttributeInfo color,
      GL3VertexAttributeInfo texcoord) {
      
      VertexBufferObject staticVBO = VertexBufferObject.generate (gl);
      VertexBufferObject dynamicVBO = VertexBufferObject.generate (gl);
      GL3SharedRenderObjectVertices out = 
         new GL3SharedRenderObjectVertices(robj.getIdentifier (), 
            staticVBO, dynamicVBO, 
            position, normal, color, texcoord);
      out.maybeUpdate (gl, robj);  // trigger a build
      return out;
   }
   
}
