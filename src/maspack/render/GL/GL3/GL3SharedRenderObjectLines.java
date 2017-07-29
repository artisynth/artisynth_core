package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL3;

import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectVersion;
import maspack.util.BufferUtilities;

/**
 * Allows easy switching between lines as GL_LINES and as cylinders/ellipsoids/etc...
 */
//=====================================================================================================================
// VBO layouts, ? if exists
// --lines expanded in each VBO for use both as GL_LINES, and as attributes for instanced drawing
// --primary positions/normals/colors/textures interleaved, separated into static/dynamic buffer
//
//  static           dynamic        
//  [pos[v[p00]] ]   [pos[v[p00]] ]
//  [nrm[v[p00]]?]   [nrm[v[p00]]?]
//  [clr[v[p00][?]   [clr[v[p00]]?]                     
//  [tex[v[p00]]?]   [tex[v[p00]]?]   
//  [pos[v[p01]] ]   [pos[v[p01]] ]   
//  [nrm[v[p01]]?]   [nrm[v[p01]]?]          
//  [clr[v[p01]]?]   [clr[v[p01]]?]          
//  [tex[v[p01]]?]   [tex[v[p01]]?]   
//    ...              ...                   
//  [pos[v[p0n]] ]   [pos[v[p0n]] ]          
//  [nrm[v[p0n]]?]   [nrm[v[p0n]]?]   
//  [clr[v[p0n]]?]   [clr[v[p0n]]?]                  
//  [tex[v[p0n]]?]   [tex[v[p0n]]?]   
//  [pos[v[p10]]?]   [pos[v[p10]]?]  
//    ... ...          ... ...          
//  [pos[v[pqr]]?]   [pos[v[pqr]]?]  
//=====================================================================================================================
public class GL3SharedRenderObjectLines extends GL3SharedRenderObjectBase {

   GL3VertexAttributeInfo bposAttr;
   GL3VertexAttributeInfo tposAttr;
   GL3VertexAttributeInfo bclrAttr;
   GL3VertexAttributeInfo tclrAttr;
   
   int[] lineGroupOffsets;       // separation between point groups (in # of vertices, for binding correct location)

   protected GL3SharedRenderObjectLines(RenderObject r,
      
      GL3VertexAttributeInfo posAttr, GL3VertexAttributeInfo nrmAttr, 
      GL3VertexAttributeInfo clrAttr, GL3VertexAttributeInfo texAttr,
      GL3VertexAttributeInfo bposAttr, GL3VertexAttributeInfo tposAttr,
      GL3VertexAttributeInfo bclrAttr, GL3VertexAttributeInfo tclrAttr,
      VertexBufferObject staticVBO, VertexBufferObject dynamicVBO) {
      super(r.getIdentifier (), posAttr, nrmAttr, clrAttr, texAttr, staticVBO, dynamicVBO);
      
      this.bposAttr = bposAttr;
      this.tposAttr = tposAttr;
      this.bclrAttr = bclrAttr;
      this.tclrAttr = tclrAttr;
   
      lineGroupOffsets = null;
   }

   public boolean maybeUpdate(GL3 gl, RenderObject robj) {
      boolean updated = false;
      robj.readLock (); {
         RenderObjectVersion rv = robj.getVersionInfo ();
         updated = maybeUpdateVertices (gl, robj, rv);
         if (updated) {
            lastVersionInfo = rv;
         }
      } robj.readUnlock ();
      return updated;
   }

   @Override
   protected boolean needsVertexRebuild(GL3 gl, RenderObjectVersion rv) {
      if (super.needsVertexRebuild (gl, rv)) {
         return true;
      }

      // if lines have changed, we need to rebuild
      if (rv.getLinesVersion () != lastVersionInfo.getLinesVersion ()) {
         streaming = true;
         return true;
      }
      return false;
   }
   
   @Override
   protected void updateDynamicVertices(GL3 gl, RenderObject robj, int updateMask, boolean replace) {

      ByteBuffer buff = null;
      if (replace) {
         // buff = vbos[DYNAMIC_VBO_IDX].mapNewBuffer (gl);
         //buff = vbos[DYNAMIC_VBO_IDX].mapBuffer (gl, GL3.GL_WRITE_ONLY);
         buff = BufferUtilities.newNativeByteBuffer (vbos[DYNAMIC_VBO_IDX].getSize ());
      } else {
         buff = vbos[DYNAMIC_VBO_IDX].mapBuffer (gl, GL3.GL_WRITE_ONLY);
      }

      // position
      if ( positionInfo != null && (updateMask & POSITION_FLAG) != 0) {
         int bidx = positionInfo.offset;
         int lineStride = robj.getLineStride ();

         for (int lg=0; lg<robj.numLineGroups(); ++lg) {
            int[] lines = robj.getLines (lg);
            int lineCount = robj.numLines (lg);
            int lidx = 0;
            for (int l = 0; l<lineCount; ++l) {
               for (int j=0; j<2; ++j) {
                  int vidx = lines[lidx+j];
                  float[] pos = robj.getVertexPosition (vidx);
                  buff.position (bidx);
                  positionPutter.putPosition(buff, pos);
                  bidx += positionInfo.stride;
               }
               lidx += lineStride;
            }
         }
      }

      // normal
      if ( normalInfo != null && (updateMask & NORMAL_FLAG) != 0) {
         int bidx = normalInfo.offset;
         int lineStride = robj.getLineStride ();

         for (int lg=0; lg<robj.numLineGroups(); ++lg) {
            int[] lines = robj.getLines (lg);
            int lineCount = robj.numLines (lg);
            int lidx = 0;
            for (int l = 0; l<lineCount; ++l) {
               for (int j=0; j<2; ++j) {
                  int vidx = lines[lidx+j];
                  float[] pos = robj.getVertexNormal (vidx);
                  buff.position (bidx);
                  normalPutter.putNormal(buff, pos);
                  bidx += normalInfo.stride;
               }
               lidx += lineStride;
            }
         }
      }

      // color
      if ( colorInfo != null && (updateMask & COLOR_FLAG) != 0) {
         int bidx = colorInfo.offset;
         int lineStride = robj.getLineStride ();

         for (int lg=0; lg<robj.numLineGroups(); ++lg) {
            int[] lines = robj.getLines (lg);
            int lineCount = robj.numLines (lg);
            int lidx = 0;
            for (int l = 0; l<lineCount; ++l) {
               for (int j=0; j<2; ++j) {
                  int vidx = lines[lidx+j];
                  byte[] pos = robj.getVertexColor (vidx);
                  buff.position (bidx);
                  colorPutter.putColor(buff, pos);
                  bidx += colorInfo.stride;
               }
               lidx += lineStride;
            }
         }
      }

      // texture
      if ( textureInfo != null && (updateMask & TEXCOORDS_FLAG) != 0) {
         int bidx = textureInfo.offset;
         int lineStride = robj.getLineStride ();

         for (int lg=0; lg<robj.numLineGroups(); ++lg) {
            int[] lines = robj.getLines (lg);
            int lineCount = robj.numLines (lg);
            int lidx = 0;
            for (int l = 0; l<lineCount; ++l) {
               for (int j=0; j<2; ++j) {
                  int vidx = lines[lidx+j];
                  float[] pos = robj.getVertexTextureCoord (vidx);
                  buff.position (bidx);
                  texturePutter.putTextureCoord(buff, pos);
                  bidx += textureInfo.stride;
               }
               lidx += lineStride;
            }
         }
      }

      if (replace) {
         buff.flip ();
         vbos[DYNAMIC_VBO_IDX].update (gl, buff);
         buff = BufferUtilities.freeDirectBuffer (buff);
      } else {
         vbos[DYNAMIC_VBO_IDX].unmapBuffer(gl);
      }
   }

   public boolean isValid () {
      if (!super.isValid()) {
         return false;
      }
      return true;
   }

   @Override
   protected void clearAll(GL3 gl) {
      super.clearAll (gl);
      lineGroupOffsets = null;
   }

   @Override
   protected void buildVertices(GL3 gl, RenderObject robj) {

      int numLinesTotal = 0;
      int numLineGroups = robj.numLineGroups ();
      lineGroupOffsets = new int[numLineGroups+1];
      for (int lg = 0; lg < numLineGroups; ++lg) {
         lineGroupOffsets[lg] = numLinesTotal;
         numLinesTotal += robj.numLines(lg);
      }
      lineGroupOffsets[numLineGroups] = numLinesTotal;
      int nLineVertices = 2*numLinesTotal;

      buildVertexInfo (gl, robj, nLineVertices);
      // create and fill VBOs
      createVertexVBOs(gl, robj, nLineVertices);

   }

   private void createVertexVBOs(GL3 gl, RenderObject robj, int nVertices) {

      // create buffers for VBOs
      ByteBuffer[] buffs = new ByteBuffer[2];

      if (staticVertexSize > 0) {
         buffs[STATIC_VBO_IDX] = BufferUtilities.newNativeByteBuffer(nVertices*staticVertexSize);
      }
      // dynamic
      if (dynamicVertexSize > 0) {
         buffs[DYNAMIC_VBO_IDX] = BufferUtilities.newNativeByteBuffer(nVertices*dynamicVertexSize);
      }

      // fill vertex buffers
      int lineStride = robj.getLineStride ();
      int pidx = 0;
      for (int lg=0; lg<robj.numLineGroups(); ++lg) {
         int[] lines = robj.getLines(lg);
         int lineCount = robj.numLines (lg);
         int lidx = 0;
         for (int i=0; i<lineCount; ++i) {
            for (int j=0; j<2; ++j) {
               int vidx = lines[lidx+j];
               // position
               if (positionInfo != null) {
                  float[] pos = robj.getVertexPosition(vidx);
                  positionPutter.putPosition(buffs[positionInfo.vboIndex], 
                     positionInfo.offset+pidx*positionInfo.stride, pos);
               }

               // normal
               if (normalInfo != null) {
                  float[] nrm = robj.getVertexNormal(vidx);
                  normalPutter.putNormal(buffs[normalInfo.vboIndex], 
                     normalInfo.offset+pidx*normalInfo.stride, nrm);
               }

               // color
               if (colorInfo != null) {
                  byte[] color = robj.getVertexColor(vidx);
                  colorPutter.putColor(buffs[colorInfo.vboIndex], 
                     colorInfo.offset+pidx*colorInfo.stride, color);
               }

               // texture
               if (textureInfo != null) {
                  float[] pos = robj.getVertexTextureCoord(vidx);
                  texturePutter.putTextureCoord(buffs[textureInfo.vboIndex], 
                     textureInfo.offset+pidx*textureInfo.stride, pos);
               }
               pidx++;
            }
            lidx += lineStride;
         }
      }

      // create VBOs
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

   public int numLineGroups() {
      return lineGroupOffsets.length-1;
   }
   
   /**
    * Bind with a line-offset (e.g. offset=k will bind starting at kth line) 
    */
   public void bindInstancedVertices(GL3 gl, int gidx, int offset) {

      int vstart = 2*(lineGroupOffsets[gidx]+offset);
      if (positionInfo != null ) {
         GL3AttributeStorage storage = positionPutter.storage ();
         vbos[positionInfo.vboIndex].bind (gl);
         int loc = bposAttr.getLocation ();
         if (loc >= 0) {
            GL3Utilities.activateVertexAttribute(gl, loc, storage, 
               2*positionInfo.stride, positionInfo.offset+vstart*positionInfo.stride, 1);
         }
         loc = tposAttr.getLocation ();
         if (loc >= 0) {
            GL3Utilities.activateVertexAttribute(gl, loc, storage, 
               2*positionInfo.stride, positionInfo.offset+(vstart+1)*positionInfo.stride, 1);
         }
      }

      if (colorInfo != null) {
         GL3AttributeStorage storage = colorPutter.storage ();
         vbos[colorInfo.vboIndex].bind (gl);
         int loc = bclrAttr.getLocation ();
         if (loc >= 0) {
            GL3Utilities.activateVertexAttribute(gl, loc, storage, 
               colorInfo.stride, colorInfo.offset+vstart*colorInfo.stride, 1);
         }
         loc = tclrAttr.getLocation ();
         if (loc >= 0) {
            GL3Utilities.activateVertexAttribute(gl, loc, storage, 
               2*colorInfo.stride, colorInfo.offset+(vstart+1)*colorInfo.stride, 1);
         }
      }
   }

   @Override
   public GL3SharedRenderObjectLines acquire () {
      return (GL3SharedRenderObjectLines)super.acquire ();
   }
   
   public void drawLines(GL3 gl, int mode, int gidx){
      int lstart = lineGroupOffsets[gidx];
      int lcount = lineGroupOffsets[gidx+1]-lstart;
      gl.glDrawArrays (mode, 2*lstart, 2*lcount);  // 2 vertices per line
   }
   
   public void drawLines(GL3 gl, int mode, int gidx, int offset, int count){
      int lstart = lineGroupOffsets[gidx]+offset;
      gl.glDrawArrays (mode, 2*lstart, 2*count);  // 2 vertices per line
   }

   public void drawInstancedLines(GL3 gl, GL3SharedObject line, int gidx) {
      int lcount = lineGroupOffsets[gidx+1]-lineGroupOffsets[gidx];
      line.drawInstanced (gl, lcount);
   }
   
   public void drawInstancedLines(GL3 gl, GL3SharedObject line, int gidx, int count) {
      line.drawInstanced (gl, count);   
   }

   public static GL3SharedRenderObjectLines generate (
      GL3 gl, RenderObject robj, GL3VertexAttributeInfo position,
      GL3VertexAttributeInfo normal, GL3VertexAttributeInfo color,
      GL3VertexAttributeInfo texcoord,
      GL3VertexAttributeInfo bpos,
      GL3VertexAttributeInfo tpos,
      GL3VertexAttributeInfo bclr,
      GL3VertexAttributeInfo tclr) {

      VertexBufferObject staticVBO = VertexBufferObject.generate (gl);
      VertexBufferObject dynamicVBO = VertexBufferObject.generate (gl);
      
      GL3SharedRenderObjectLines out = new GL3SharedRenderObjectLines (robj, position, normal, color, texcoord, 
         bpos, tpos, bclr, tclr, staticVBO, dynamicVBO);
      out.maybeUpdate (gl, robj);  // trigger a build
      return out;
   }

   public int numLines (int gidx) {
      return lineGroupOffsets[gidx+1]-lineGroupOffsets[gidx];
   }
}
