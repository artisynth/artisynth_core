package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL3;

import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectVersion;
import maspack.util.BufferUtilities;

/**
 * Allows easy switching between points as GL_POINTS and as spheres
 */
//=====================================================================================================================
// VBO layouts, ? if exists
// --points expanded in each VBO for use both as GL_POINTS, and as attributes for instanced drawing
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
public class GL3SharedRenderObjectPoints extends GL3SharedRenderObjectBase {

   GL3VertexAttributeInfo pposAttr;
   GL3VertexAttributeInfo pclrAttr;

   int[] pointGroupOffsets;       // separation between point groups (in # of vertices, for binding correct location)

   public GL3SharedRenderObjectPoints(RenderObject r,
      GL3VertexAttributeInfo posAttribute, GL3VertexAttributeInfo nrmAttribute, 
      GL3VertexAttributeInfo clrAttribute, GL3VertexAttributeInfo texAttribute,
      GL3VertexAttributeInfo pposAttr, GL3VertexAttributeInfo pclrAttr,
      VertexBufferObject staticVBO, VertexBufferObject dynamicVBO) {

      super(r.getIdentifier (), posAttribute, nrmAttribute,
         clrAttribute, texAttribute, staticVBO, dynamicVBO);
      this.pposAttr = pposAttr;
      this.pclrAttr = pclrAttr;

      pointGroupOffsets = null;
   }

   // maybe update VBOs
   /**
    * Potentially update the internal VBOs
    * @return true if updated, false if nothing changed
    */
   public boolean maybeUpdate(GL3 gl, RenderObject robj) {

      boolean updated = false;
      robj.readLock (); {
         RenderObjectVersion rv = robj.getVersionInfo ();
         updated = maybeUpdateVertices(gl, robj, rv);
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

      // if points have changed, we need to rebuild
      if (rv.getPointsVersion () != lastVersionInfo.getPointsVersion ()) {
         streaming = true;  // set to streaming mode since points are changing around
         return true;
      }
      return false;
   }


   @Override
   protected void updateDynamicVertices(GL3 gl, RenderObject robj, int updateMask, boolean replace) {

      ByteBuffer buff = null;
      if (replace) {
         buff = BufferUtilities.newNativeByteBuffer (vbos[DYNAMIC_VBO_IDX].getSize ());
         // buff = vbos[DYNAMIC_VBO_IDX].mapNewBuffer (gl);
      } else {
         buff = vbos[DYNAMIC_VBO_IDX].mapBuffer (gl, GL3.GL_WRITE_ONLY);
      }

      // position
      if ( positionInfo != null && (updateMask & POSITION_FLAG) != 0) {
         int bidx = positionInfo.offset;
         int pointStride = robj.getPointStride ();

         for (int lg=0; lg<robj.numPointGroups(); ++lg) {
            int[] points = robj.getPoints (lg);
            int pointCount = robj.numPoints (lg);
            int lidx = 0;
            for (int l = 0; l<pointCount; ++l) {
               int vidx = points[lidx];
               float[] pos = robj.getVertexPosition (vidx);
               buff.position (bidx);
               positionPutter.putPosition(buff, pos);
               bidx += positionInfo.stride;
               lidx += pointStride;
            }
         }
      }

      // normal
      if ( normalInfo != null && (updateMask & NORMAL_FLAG) != 0) {
         int bidx = normalInfo.offset;
         int pointStride = robj.getPointStride ();

         for (int lg=0; lg<robj.numPointGroups(); ++lg) {
            int[] points = robj.getPoints (lg);
            int pointCount = robj.numPoints (lg);
            int lidx = 0;
            for (int l = 0; l<pointCount; ++l) {
               int vidx = points[lidx];
               float[] pos = robj.getVertexNormal (vidx);
               buff.position (bidx);
               normalPutter.putNormal(buff, pos);
               bidx += normalInfo.stride;
               lidx += pointStride;
            }
         }
      }

      // color
      if ( colorInfo != null && (updateMask & COLOR_FLAG) != 0) {
         int bidx = colorInfo.offset;
         int pointStride = robj.getPointStride ();

         for (int lg=0; lg<robj.numPointGroups(); ++lg) {
            int[] points = robj.getPoints (lg);
            int pointCount = robj.numPoints (lg);
            int lidx = 0;
            for (int l = 0; l<pointCount; ++l) {
               int vidx = points[lidx];
               byte[] pos = robj.getVertexColor (vidx);
               buff.position (bidx);
               colorPutter.putColor(buff, pos);
               bidx += colorInfo.stride;
               lidx += pointStride;
            }
         }
      }

      // texture
      if ( textureInfo != null && (updateMask & TEXCOORDS_FLAG) != 0) {
         int bidx = textureInfo.offset;
         int pointStride = robj.getPointStride ();

         for (int lg=0; lg<robj.numPointGroups(); ++lg) {
            int[] points = robj.getPoints (lg);
            int pointCount = robj.numPoints (lg);
            int lidx = 0;
            for (int l = 0; l<pointCount; ++l) {
               int vidx = points[lidx];
               float[] pos = robj.getVertexTextureCoord (vidx);
               buff.position (bidx);
               texturePutter.putTextureCoord(buff, pos);
               bidx += textureInfo.stride;
               lidx += pointStride;
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
      pointGroupOffsets = null;
   }


   @Override
   protected void buildVertices(GL3 gl, RenderObject robj) {

      int numPointsTotal = 0;
      int numPointGroups = robj.numPointGroups ();
      pointGroupOffsets = new int[numPointGroups+1];
      for (int lg = 0; lg < numPointGroups; ++lg) {
         pointGroupOffsets[lg] = numPointsTotal;
         numPointsTotal += robj.numPoints(lg);
      }
      pointGroupOffsets[numPointGroups] = numPointsTotal;

      buildVertexInfo(gl, robj, numPointsTotal);
      // create and fill VBOs
      createVertexVBOs(gl, robj, numPointsTotal);

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
      int pointStride = robj.getPointStride ();
      int pidx = 0;
      for (int lg=0; lg<robj.numPointGroups(); ++lg) {
         int[] points = robj.getPoints(lg);
         int pointCount = robj.numPoints (lg);
         int lidx = 0;
         for (int i=0; i<pointCount; ++i) {
            int vidx = points[lidx];
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
            lidx += pointStride;
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
   
   public int numPointGroups() {
      return pointGroupOffsets.length-1;
   }

   public int numPoints(int gidx) {
      return pointGroupOffsets[gidx+1]-pointGroupOffsets[gidx];
   }
   
   
   public void bindInstancedVertices(GL3 gl, int gidx) {
      bindInstancedVertices (gl, gidx, 0)
      ;
   }
   
   public void bindInstancedVertices(GL3 gl, int gidx, int offset) {

      int vstart = pointGroupOffsets[gidx]+offset;
      
      int loc = pposAttr.getLocation ();
      if (positionInfo != null ) {
         GL3AttributeStorage storage = positionPutter.storage ();
         vbos[positionInfo.vboIndex].bind (gl);
         GL3Utilities.activateVertexAttribute(gl, loc, storage, 
            positionInfo.stride, positionInfo.offset+vstart*positionInfo.stride, 1);
      }

      loc = pclrAttr.getLocation ();
      if (colorInfo != null) {
         GL3AttributeStorage storage = colorPutter.storage ();
         vbos[colorInfo.vboIndex].bind (gl);

         GL3Utilities.activateVertexAttribute(gl, loc, storage, 
            colorInfo.stride, colorInfo.offset+vstart*colorInfo.stride, 1);
      }
   }
   
   @Override
   public GL3SharedRenderObjectPoints acquire () {
      return (GL3SharedRenderObjectPoints)super.acquire ();
   }

   public void drawPoints(GL3 gl, int mode, int gidx){
      int vstart = pointGroupOffsets[gidx];
      int vcount = pointGroupOffsets[gidx+1]-vstart;
      gl.glDrawArrays (mode, vstart, vcount);  // 1 vertex per point
   }
   
   public void drawPoints(GL3 gl, int mode, int gidx, int offset, int count){
      int vstart = pointGroupOffsets[gidx] + offset;
      gl.glDrawArrays (mode, vstart, count);  // 1 vertex per point
   }

   public void drawInstancedPoints(GL3 gl, GL3SharedObject point, int gidx) {
      int vstart = pointGroupOffsets[gidx];
      int vcount = pointGroupOffsets[gidx+1]-vstart;
      point.drawInstanced (gl, vcount);
   }
   
   public void drawInstancedPoints(GL3 gl, GL3SharedObject point, int gidx, 
      int count) {
      point.drawInstanced (gl, count);
   }

   public static GL3SharedRenderObjectPoints generate (
      GL3 gl, RenderObject robj, GL3VertexAttributeInfo position,
      GL3VertexAttributeInfo normal, GL3VertexAttributeInfo color,
      GL3VertexAttributeInfo texcoord,
      GL3VertexAttributeInfo pointpos,
      GL3VertexAttributeInfo pointclr) {
      
      VertexBufferObject staticVBO = VertexBufferObject.generate (gl);
      VertexBufferObject dynamicVBO = VertexBufferObject.generate (gl);
      GL3SharedRenderObjectPoints out = new GL3SharedRenderObjectPoints (robj, position, normal, color, texcoord, 
         pointpos, pointclr, staticVBO, dynamicVBO);
      out.maybeUpdate (gl, robj);  // trigger a build
      return out;
   }

}
