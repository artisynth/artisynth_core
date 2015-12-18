package maspack.render.GL.GL3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectIdentifier;
import maspack.render.RenderObject.RenderObjectState;
import maspack.render.RenderObject.RenderObjectVersion;
import maspack.render.RenderObject.VertexIndexSet;
import maspack.render.GL.GLSupport;

/**
 * Allows easy switching between points as GL_POINT and as spheres
 */
//=====================================================================================================================
// VBO layouts, ? if exists
// --points expanded in each VBO for use both as GL_POINTS, and as attributes for instanced drawing
// --primary positions/normals/colors/textures interleaved, separated into static/dynamic buffer
// --extra attributes as separate VBOs, one per attribute that contains all extra per-attribute sets
//
//  static?             dynamic?           extra positions?  extra normals?  extra colors?  extra textures?  point info
//  [pos[0][v[p00]] ]   [pos[0][v[p00]] ]  [pos[1][v[p00]]]     ...             ...            ...           [pointRadius]
//  [nrm[0][v[p00]]?]   [nrm[0][v[p00]]?]  [pos[1][v[p01]]] 
//  [clr[0][v[p00][?]   [clr[0][v[p00]]?]    ...                   
//  [tex[0][v[p00]]?]   [tex[0][v[p00]]?]  [pos[1][v[p0n]]] 
//  [pos[0][v[p01]] ]   [pos[0][v[p01]] ]  [pos[1][v[p10]]] 
//  [nrm[0][v[p01]]?]   [nrm[0][v[p01]]?]    ... ...        
//  [clr[0][v[p01]]?]   [clr[0][v[p01]]?]  [pos[1][v[pqr]]]        
//  [tex[0][v[p01]]?]   [tex[0][v[p01]]?]  [pos[2][v[p00]]] 
//    ...                 ...              [pos[2][v[p01]]]     
//  [pos[0][v[p0n]] ]   [pos[0][v[p0n]] ]    ... ...        
//  [nrm[0][v[p0n]]?]   [nrm[0][v[p0n]]?]  [pos[2][v[pqr]]] 
//  [clr[0][v[p0n]]?]   [clr[0][v[p0n]]?]   ... ...                
//  [tex[0][v[p0n]]?]   [tex[0][v[p0n]]?]  [pos[i][v[p00]]] 
//  [pos[0][v[p10]]?]   [pos[0][v[p10]]?]  [pos[i][v[p01]]]
//    ... ...             ... ...            ... ...
//  [pos[0][v[pqr]]?]   [pos[0][v[pqr]]?]  [pos[i][v[pqr]]]
//=====================================================================================================================
public class GL3RenderObjectPoints extends GL3ResourceBase implements GL3Drawable {

   private RenderObjectIdentifier roId;
   private RenderObjectState roState;
   private PositionBufferPutter positionPutter;
   private NormalBufferPutter normalPutter;
   private ColorBufferPutter colorPutter;
   private TextureCoordBufferPutter texturePutter;
   
   private static class AttributeInfo {
      int vboIndex;
      int offset;       // in bytes
      int stride;       // in bytes
      int count;        // number of items
   }
   
   @SuppressWarnings("unused")
   private static class GLOInfo {
      int positionSet;
      int normalSet;
      int colorSet;
      int textureSet;
      int pointGroup;
      GL3Object pointObject;
      float pointRadius;
      public GLOInfo(int pset, int nset, int cset, int tset, int pgroup, 
         GL3Object pointObject, float pointRadius) {
         this.positionSet = pset;
         this.normalSet = nset;
         this.colorSet = cset;
         this.textureSet = tset;
         this.pointGroup = pgroup;
         this.pointObject = pointObject;
         this.pointRadius = pointRadius;
      }
   }
   
   AttributeInfo[] positionInfo;  // position buffer(s)
   AttributeInfo[] normalInfo;    // normal buffer(s)
   AttributeInfo[] colorInfo;     // color buffer(s)
   AttributeInfo[] textureInfo;   // texture buffer(s)
   AttributeInfo   pointInfo;     // radius
   int[] pointGroupOffsets;       // separation between point groups (in # of vertices, for binding correct location)
   
   int staticVertexSize;
   int staticMask;
   int dynamicVertexSize;
   int dynamicMask;
   int nVertices;
   
   BufferObject[] vbos;
   GL3Object[] glos;
   GLOInfo[] gloInfo;
   
   private static final int POSITION_FLAG = 0x01;
   private static final int NORMAL_FLAG = 0x02;
   private static final int COLOR_FLAG = 0x04;
   private static final int TEXTURE_FLAG = 0x08;

   // track version numbers so we can detect what has changed since last use
   RenderObjectVersion lastVersionInfo;
   GL3Object lastPointObject;
   float lastPointRadius;

   public GL3RenderObjectPoints(RenderObject r) {
      
      this.roId = r.getIdentifier();
      this.roState = r.getStateInfo();
      
      vbos = null;
      glos = null;
      gloInfo = null;
      
      positionInfo = null;
      normalInfo = null;
      colorInfo = null;
      textureInfo = null;
      pointGroupOffsets = null;
      
      lastVersionInfo = null;
      lastPointObject = null;
      lastPointRadius = -1;
      
      staticVertexSize = 0;
      staticMask = 0; 
      dynamicVertexSize = 0;
      dynamicMask = 0;
      nVertices = 0;
   }

   private int getGLOidx() {

      int idx = roState.getPointGroupIdx();

      if (roState.hasTextureCoords()) {
         idx = idx*roState.numTextureCoordSets() + roState.getTextureCoordSetIdx();
      }

      if (roState.hasColors()) {
         idx = idx*roState.numColorSets() + roState.getColorSetIdx();
      }

      if (roState.hasNormals()) {
         idx = idx*roState.numNormalSets() + roState.getNormalSetIdx();
      }

      if (roState.hasPositions()) {
         idx = idx*roState.numPositionSets() + roState.getPositionSetIdx();
      }
      
      return idx;

   }

   public void drawPoints(GL3 gl) {
      if (!isValid() || roState.numPointGroups() == 0) {
         return;
      }
      int gloIndex = getGLOidx();
      glos[gloIndex].draw(gl); 
   }

   // maybe update VBOs
   /**
    * Potentially update the internal VAOs
    * @param gl
    * @param robj
    * @return true if updated, false if nothing changed
    */
   public boolean maybeUpdate(GL3 gl, RenderObject robj,
      GL3Object pointObject, float pointRadius) {
      
      boolean updated = false;
      
      synchronized(robj) {
         
         // update state info
         roState = robj.getStateInfo();
         
         // commit modified data if not already
         if (!robj.isCommitted()) {
            robj.commit();
         }

         // check version numbers and see if we need to update
         if (lastVersionInfo == null) {
            clearAndRebuild(gl, robj, pointObject, pointRadius);
            return true;
         } else if (robj.getVersion() != lastVersionInfo.getVersion()) {
            
            // find what has changed, see if we can update it
            boolean streaming = robj.isStreaming();
            if (!robj.isDynamic() || streaming) {
               // static or streaming
               clearAndRebuild(gl, robj, pointObject, pointRadius);
               return true;
            } else if (
               robj.getVerticesVersion() != lastVersionInfo.getVerticesVersion() ||
               robj.getPointsVersion() != lastVersionInfo.getPointsVersion()) {
               // vertices (all VBOs need update) or point groups (VBO size changes)
               clearAndRebuild(gl, robj, pointObject, pointRadius);
               return true;
            } else {
               
               // check if we should update all dynamic, update part dynamic, or rebuild everything
               int updateFlag = 0;
               
               if (robj.getPositionsVersion() != lastVersionInfo.getPositionsVersion()) {
                  boolean positionsDynamic = ((dynamicMask & POSITION_FLAG) != 0);
                  if  (!positionsDynamic || !robj.isPositionsDynamic()) {
                     // positions are static or static/dynamic property changed
                     clearAndRebuild(gl, robj, pointObject, pointRadius);
                     return true;
                  } else {
                     updateFlag |= POSITION_FLAG;
                  }
               }
               
               if (robj.getNormalsVersion() != lastVersionInfo.getNormalsVersion()) {
                  boolean normalsDynamic = ((dynamicMask & NORMAL_FLAG) != 0);
                  if (!normalsDynamic || !robj.isNormalsDynamic()) {
                     // normals are static or static/dynamic property changed
                     clearAndRebuild(gl, robj, pointObject, pointRadius);
                     return true;
                  } else {
                     updateFlag |= NORMAL_FLAG;
                  }
               }
                  
               if (robj.getColorsVersion() != lastVersionInfo.getColorsVersion()) {
                  boolean colorsDynamic = ((dynamicMask & COLOR_FLAG) != 0);
                  if (!colorsDynamic || !robj.isColorsDynamic()) {
                     // colors are static or static/dynamic property changed
                     clearAndRebuild(gl, robj, pointObject, pointRadius);
                     return true;
                  } else {
                     updateFlag |= COLOR_FLAG;
                  }
               }
               
               if (robj.getTextureCoordsVersion() != lastVersionInfo.getTextureCoordsVersion()) {
                  boolean texturesDynamic = ((dynamicMask & TEXTURE_FLAG) != 0);
                  if (!texturesDynamic || !robj.isTextureCoordsDynamic()) {
                     // textures are static or static/dynamic property changed
                     clearAndRebuild(gl, robj, pointObject, pointRadius);
                     return true;
                  } else {
                     updateFlag |= TEXTURE_FLAG;
                  }
               }
               
               if (updateFlag != 0) {
                  updateDynamicVBOs(gl, robj, updateFlag, updateFlag == dynamicMask);
                  // mark version numbers
                  lastVersionInfo = robj.getVersionInfo();
                  updated = true;
               }
               
            } // end static, streaming, or size change
         } // end different version
         
         // if point object has changed, rebuild GLO
         if (lastPointObject != pointObject) {
            createGLOs(gl, robj, pointObject, pointRadius);
            lastPointObject = pointObject;
            updated = true;
         }
         
      } // end synchronize
      
      // radius/lengths
      if (lastPointRadius != pointRadius) {
         updatePointVBO(gl, pointRadius);
         updated = true;
      }
      
      return updated;
   }
   
   private void updatePointVBO(GL3 gl, float pointRadius) {
      ByteBuffer buff = vbos[pointInfo.vboIndex].mapNewBuffer(gl);
      buff.putFloat(pointRadius);
      vbos[pointInfo.vboIndex].unmapBuffer(gl);
   }
   
   private void updateDynamicVBOs(GL3 gl, RenderObject robj, int updateMask, boolean replace) {
      ByteBuffer[] buffs = new ByteBuffer[vbos.length];
      
      int vidx = 0;
      
      for (int pg=0; pg<robj.numPointGroups(); ++pg) {
         List<int[]> pnts = robj.getPoints(pg);
         
         for (int[] pnt : pnts) {
            VertexIndexSet v = robj.getVertex(pnt[0]);
            
            // position
            if ( (updateMask & POSITION_FLAG) != 0) {
               for (int i=0; i<positionInfo.length; ++i) {
                  AttributeInfo ai = positionInfo[i];
                  if (buffs[ai.vboIndex] == null) {
                     if (replace) {
                        buffs[ai.vboIndex] = vbos[ai.vboIndex].mapNewBuffer(gl);
                     } else {
                        buffs[ai.vboIndex] = vbos[ai.vboIndex].mapBuffer(gl, GL3.GL_WRITE_ONLY);
                     }
                  }
                  
                  float[] pos = robj.getPosition(i, v.getPositionIndex());
                  positionPutter.putPosition(buffs[ai.vboIndex], ai.offset+vidx*ai.stride, pos);
               }
            }
            
            // normal
            if ( (updateMask & NORMAL_FLAG) != 0) {
               for (int i=0; i<normalInfo.length; ++i) {
                  AttributeInfo ai = normalInfo[i];
                  if (buffs[ai.vboIndex] == null) {
                     if (replace) {
                        buffs[ai.vboIndex] = vbos[ai.vboIndex].mapNewBuffer(gl);
                     } else {
                        buffs[ai.vboIndex] = vbos[ai.vboIndex].mapBuffer(gl, GL3.GL_WRITE_ONLY);
                     }
                  }
                  
                  float[] nrm = robj.getNormal(i, v.getNormalIndex());
                  normalPutter.putNormal(buffs[ai.vboIndex], ai.offset+vidx*ai.stride, nrm);
               }
            }
            
            // color
            if ( (updateMask & COLOR_FLAG) != 0) {
               for (int i=0; i<colorInfo.length; ++i) {
                  AttributeInfo ai = colorInfo[i];
                  if (buffs[ai.vboIndex] == null) {
                     if (replace) {
                        buffs[ai.vboIndex] = vbos[ai.vboIndex].mapNewBuffer(gl);
                     } else {
                        buffs[ai.vboIndex] = vbos[ai.vboIndex].mapBuffer(gl, GL3.GL_WRITE_ONLY);
                     }
                  }
                  
                  byte[] color = robj.getColor(i, v.getColorIndex());
                  colorPutter.putColor(buffs[ai.vboIndex], ai.offset+vidx*ai.stride, color);
               }
            }
            
            // texture
            if ( (updateMask & TEXTURE_FLAG) != 0) {
               for (int i=0; i<textureInfo.length; ++i) {
                  AttributeInfo ai = textureInfo[i];
                  if (buffs[ai.vboIndex] == null) {
                     if (replace) {
                        buffs[ai.vboIndex] = vbos[ai.vboIndex].mapNewBuffer(gl);
                     } else {
                        buffs[ai.vboIndex] = vbos[ai.vboIndex].mapBuffer(gl, GL3.GL_WRITE_ONLY);
                     }
                  }
                  
                  float[] pos = robj.getTextureCoord(i, v.getTextureCoordIndex());
                  texturePutter.putTextureCoord(buffs[ai.vboIndex], ai.offset+vidx*ai.stride, pos);
               }
            }
            vidx++;
            
         }
      }
    
      // unmap
      for (int i=0; i<buffs.length; ++i) {
         if (buffs[i] != null) {
            vbos[i].unmapBuffer(gl);
         }
      }
      
   }

   public void draw(GL3 gl) {
      drawPoints(gl);
   }

   public boolean isValid() {
      return glos != null;
   }

   @Override
   public void init(GL3 gl) {
      // nothing, must be initialized with RenderObject
   }
   
   public void init(GL3 gl, RenderObject robj, 
      GL3Object pointObject, float pointRadius) {
      
      synchronized(robj) {
         // commit info
         if (!robj.isCommitted()) {
            robj.commit();
         }
         // stores VBO indices/offsets/strides for each attribute 
         build(gl, robj, pointObject, pointRadius);
      }      
   }
   
   private void clearAll(GL3 gl) {
      
      dynamicMask = 0;
      dynamicVertexSize = 0;
      
      staticMask = 0;
      staticVertexSize = 0;
      
      nVertices = 0;
      
      positionInfo = null;
      normalInfo = null;
      colorInfo = null;
      textureInfo = null;
      pointGroupOffsets = null;
      
      // clear object storage
      if (glos != null) {
         for (GL3Object glo : glos) {
            glo.release(gl);
         }
      }
      glos = null;
      gloInfo = null;
      
      // clear shared VBOs
      if (vbos != null) {
         for (BufferObject vbo : vbos) {
            vbo.release(gl);
         }
      }
      vbos = null;
      
      positionPutter = null;
      normalPutter = null;
      colorPutter = null;
      texturePutter = null;
      
      lastVersionInfo = null;
      lastPointObject = null;
      lastPointRadius = -1;
   }
   
   private void clearAndRebuild(GL3 gl, RenderObject robj, 
      GL3Object pointObject, float pointRadius) {
      clearAll(gl);
      build(gl, robj, pointObject, pointRadius);
   }
   
   private void build(GL3 gl, RenderObject robj, 
      GL3Object pointObject, float pointRadius) {
      
      // create and fill VBOs
      createVBOs(gl, robj, pointRadius);
      createGLOs(gl, robj, pointObject, pointRadius);

      // mark version numbers
      lastVersionInfo = robj.getVersionInfo();
      lastPointObject = pointObject;
      lastPointRadius = pointRadius;
   }
      
   private AttributeInfo[] createAttributeInfoArrays(int n) {
      AttributeInfo[] ai = new AttributeInfo[n];
      for (int i=0; i<ai.length; ++i) {
         ai[i] = new AttributeInfo();
      }
      return ai;
   }
   
   private void createVBOs(GL3 gl, RenderObject robj, float pointRadius) {
      
      // create buffer manipulators
      positionPutter = PositionBufferPutter.createDefault();
      normalPutter = NormalBufferPutter.createDefault();
      colorPutter = ColorBufferPutter.createDefault();
      texturePutter = TextureCoordBufferPutter.createDefault();
      
      final int POSITION_BYTES = positionPutter.bytesPerPosition();
      final int NORMAL_BYTES = normalPutter.bytesPerNormal();
      final int COLOR_BYTES = colorPutter.bytesPerColor();
      final int TEXTURE_BYTES = texturePutter.bytesPerTextureCoord();
      
      // build up position info
      positionInfo = createAttributeInfoArrays(robj.numPositionSets());
      normalInfo = createAttributeInfoArrays(robj.numNormalSets());
      colorInfo = createAttributeInfoArrays(robj.numColorSets());
      textureInfo = createAttributeInfoArrays(robj.numTextureCoordSets());
      pointGroupOffsets = new int[robj.numPointGroups()];
      pointInfo = new AttributeInfo();

      staticVertexSize = 0;
      staticMask = 0;
      dynamicVertexSize = 0;
      dynamicMask = 0;
      boolean streaming = robj.isStreaming();
      nVertices = robj.numVertices();

      int numPointsTotal = 0;
      for (int pg = 0; pg < robj.numPointGroups(); ++pg) {
         pointGroupOffsets[pg] = numPointsTotal;
         numPointsTotal += robj.numPoints(pg);
      }
      
      // determine necessary sizes
      if (robj.hasPositions()) {
         if (!streaming && robj.isPositionsDynamic()) {
            positionInfo[0].offset = dynamicVertexSize;
            dynamicVertexSize += POSITION_BYTES;  // 3 floats per position
            dynamicMask |= POSITION_FLAG;
         } else {
            positionInfo[0].offset = staticVertexSize;
            staticVertexSize += POSITION_BYTES;   // 3 floats per position
            staticMask |= POSITION_FLAG;
         }
         positionInfo[0].count = numPointsTotal;

         for (int i=1; i<positionInfo.length; ++i) {
            positionInfo[i].offset = (i-1)*POSITION_BYTES*numPointsTotal;
            positionInfo[i].stride = POSITION_BYTES;
            positionInfo[i].count = numPointsTotal;
         }
      }

      if (robj.hasNormals()) {
         if (!streaming && robj.isNormalsDynamic()) {
            normalInfo[0].offset = dynamicVertexSize;
            dynamicVertexSize += NORMAL_BYTES;  // 3 shorts per normal, +1 for 4-byte alignment
            dynamicMask |= NORMAL_FLAG;
         } else {
            normalInfo[0].offset = staticVertexSize;
            staticVertexSize += NORMAL_BYTES; 
            staticMask |= NORMAL_FLAG;
         }
         normalInfo[0].count = numPointsTotal;
         
         for (int i=1; i<normalInfo.length; ++i) {
            normalInfo[i].offset = (i-1)*NORMAL_BYTES*numPointsTotal;
            normalInfo[i].stride = NORMAL_BYTES;
            normalInfo[i].count = numPointsTotal;
         }
      }

      if (robj.hasColors()) {
         if (!streaming && robj.isColorsDynamic()) {
            colorInfo[0].offset = dynamicVertexSize;
            dynamicVertexSize += COLOR_BYTES;   // 4 bytes per color
            dynamicMask |= COLOR_FLAG;
         } else {
            colorInfo[0].offset = staticVertexSize;
            staticVertexSize += COLOR_BYTES;
            staticMask |= COLOR_FLAG;
         }
         colorInfo[0].count = numPointsTotal;

         for (int i=1; i<colorInfo.length; ++i) {
            colorInfo[i].offset = (i-1)*COLOR_BYTES*numPointsTotal;
            colorInfo[i].stride = COLOR_BYTES;
            colorInfo[i].count = numPointsTotal;
         }
      }

      if (robj.hasTextureCoords()) {
         if (!streaming && robj.isTextureCoordsDynamic()) {
            textureInfo[0].offset = dynamicVertexSize;
            dynamicVertexSize += TEXTURE_BYTES;  // 2 shorts per texture
            dynamicMask |= TEXTURE_FLAG;
         } else {
            textureInfo[0].offset = staticVertexSize;
            staticVertexSize += TEXTURE_BYTES; 
            staticMask |= TEXTURE_FLAG;
         }
         textureInfo[0].count = numPointsTotal;

         for (int i=1; i<textureInfo.length; ++i) {
            textureInfo[i].offset = (i-1)*TEXTURE_BYTES*numPointsTotal;
            textureInfo[i].stride = TEXTURE_BYTES;
            textureInfo[i].count = numPointsTotal;
         }
      }

      // strides and vbo index
      int staticVBOIdx = 0;
      int dynamicVBOIdx = 0;
      if (staticVertexSize > 0 && dynamicVertexSize > 0) {
         dynamicVBOIdx = 1;
      }
      if (robj.hasPositions()) {
         if (!streaming && robj.isPositionsDynamic()) {
            positionInfo[0].stride = dynamicVertexSize;
            positionInfo[0].vboIndex = dynamicVBOIdx;
         } else {
            positionInfo[0].stride = staticVertexSize;
            positionInfo[0].vboIndex = staticVBOIdx;
         }
      }
      if (robj.hasNormals()) {
         if (!streaming && robj.isNormalsDynamic()) {
            normalInfo[0].stride = dynamicVertexSize;
            normalInfo[0].vboIndex = dynamicVBOIdx;
         } else {
            normalInfo[0].stride = staticVertexSize;
            normalInfo[0].vboIndex = staticVBOIdx;
         }
      }
      if (robj.hasColors()) {
         if (!streaming && robj.isColorsDynamic()) {
            colorInfo[0].stride = dynamicVertexSize;
            colorInfo[0].vboIndex = dynamicVBOIdx;
         } else {
            colorInfo[0].stride = staticVertexSize;
            colorInfo[0].vboIndex = staticVBOIdx;
         }
      }
      if (robj.hasTextureCoords()) {
         if (!streaming && robj.isTextureCoordsDynamic()) {
            textureInfo[0].stride = dynamicVertexSize;
            textureInfo[0].vboIndex = dynamicVBOIdx;
         } else {
            textureInfo[0].stride = staticVertexSize;
            textureInfo[0].vboIndex = staticVBOIdx;
         }
      }

      // next index is whatever is after dynamic (which==static if no dynamic info)
      int vboIdx = dynamicVBOIdx+1;
      
      // extra attributes
      if (robj.numPositionSets() > 1) {
         for (int i=1; i<positionInfo.length; ++i) {
            positionInfo[i].vboIndex = vboIdx;
         }
         vboIdx++;
      }
      if (robj.numNormalSets() > 1) {
         for (int i=1; i<normalInfo.length; ++i) {
            normalInfo[i].vboIndex = vboIdx;
         }
         vboIdx++;
      }
      if (robj.numColorSets() > 1) {
         for (int i=1; i<colorInfo.length; ++i) {
            colorInfo[i].vboIndex = vboIdx;
         }
         vboIdx++;
      }
      if (robj.numTextureCoordSets() > 1) {
         for (int i=1; i<textureInfo.length; ++i) {
            textureInfo[i].vboIndex = vboIdx;
         }
         vboIdx++;
      }
      
      // point-specific information
      // radius
      pointInfo.offset = 0;
      pointInfo.stride = 1*GLSupport.FLOAT_SIZE;
      pointInfo.count = 1;
      pointInfo.vboIndex = vboIdx;
      vboIdx++;
      
      int nVBOs = vboIdx;
      
      // create buffers for VBOs
      ByteBuffer[] buffs = new ByteBuffer[nVBOs];

      vboIdx = 0;
      if (staticVertexSize > 0) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(numPointsTotal*staticVertexSize);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      // dynamic
      if (!streaming && robj.isDynamic()) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(numPointsTotal*dynamicVertexSize);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      
      // extra attribute sets
      if (robj.numPositionSets() > 1 ) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(numPointsTotal*(robj.numPositionSets()-1)*POSITION_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      if (robj.numNormalSets() > 1) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(numPointsTotal*(robj.numNormalSets()-1)*NORMAL_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      if (robj.numColorSets() > 1) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(numPointsTotal*(robj.numColorSets()-1)*COLOR_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      if (robj.numTextureCoordSets() > 1) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(numPointsTotal*(robj.numTextureCoordSets()-1)*TEXTURE_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      
      // fill vertex buffers
      int vidx = 0;
      for (int pg=0; pg<robj.numPointGroups(); ++pg) {
         List<int[]> pnts = robj.getPoints(pg);
         for (int[] pnt : pnts) {
            VertexIndexSet v = robj.getVertex(pnt[0]);
            
            // position
            for (int i=0; i<positionInfo.length; ++i) {
               AttributeInfo ai = positionInfo[i];
               float[] pos = robj.getPosition(i, v.getPositionIndex());
               positionPutter.putPosition(buffs[ai.vboIndex], ai.offset+vidx*ai.stride, pos);
            }
            
            // normal
            for (int i=0; i<normalInfo.length; ++i) {
               AttributeInfo ai = normalInfo[i];
               float[] nrm = robj.getNormal(i, v.getNormalIndex());
               normalPutter.putNormal(buffs[ai.vboIndex], ai.offset+vidx*ai.stride, nrm);
            }
            
            // color
            for (int i=0; i<colorInfo.length; ++i) {
               AttributeInfo ai = colorInfo[i];
               byte[] color = robj.getColor(i, v.getColorIndex());
               colorPutter.putColor(buffs[ai.vboIndex], ai.offset+vidx*ai.stride, color);
            }
            
            // texture
            for (int i=0; i<textureInfo.length; ++i) {
               AttributeInfo ai = textureInfo[i];
               float[] pos = robj.getTextureCoord(i, v.getTextureCoordIndex());
               texturePutter.putTextureCoord(buffs[ai.vboIndex], ai.offset+vidx*ai.stride, pos);
            }
            vidx++;
         }
      }
      
      // radius
      buffs[vboIdx] = ByteBuffer.allocateDirect(1*GLSupport.FLOAT_SIZE);
      buffs[vboIdx].order(ByteOrder.nativeOrder());
      buffs[vboIdx].putFloat(pointRadius); // radius
      
      // create VBOs
      vbos = new BufferObject[nVBOs];
      
      vboIdx = 0;
      if (staticVertexSize > 0) {
         vbos[vboIdx] = new BufferObject(gl);
         buffs[vboIdx].rewind();
         vbos[vboIdx].fill(gl, buffs[vboIdx], GL.GL_ARRAY_BUFFER, 
            getBufferUsage(false, streaming));
         vboIdx++;
      }
      // dynamic
      if (!streaming && robj.isDynamic()) {
         vbos[vboIdx] = new BufferObject(gl);
         buffs[vboIdx].rewind();
         vbos[vboIdx].fill(gl, buffs[vboIdx], GL.GL_ARRAY_BUFFER, 
            getBufferUsage(true, streaming));
         vboIdx++;
      }
      
      // extra attribute sets
      if (robj.numPositionSets() > 1 ) {
         vbos[vboIdx] = new BufferObject(gl);
         buffs[vboIdx].rewind();
         vbos[vboIdx].fill(gl, buffs[vboIdx], GL.GL_ARRAY_BUFFER, 
            getBufferUsage(robj.isPositionsDynamic(), streaming));
         vboIdx++;
      }
      if (robj.numNormalSets() > 1) {
         vbos[vboIdx] = new BufferObject(gl);
         buffs[vboIdx].rewind();
         vbos[vboIdx].fill(gl, buffs[vboIdx], GL.GL_ARRAY_BUFFER, 
            getBufferUsage(robj.isNormalsDynamic(), streaming));
         vboIdx++;
      }
      if (robj.numColorSets() > 1) {
         vbos[vboIdx] = new BufferObject(gl);
         buffs[vboIdx].rewind();
         vbos[vboIdx].fill(gl, buffs[vboIdx], GL.GL_ARRAY_BUFFER, 
            getBufferUsage(robj.isColorsDynamic(), streaming));
         vboIdx++;
      }
      if (robj.numTextureCoordSets() > 1) {
         vbos[vboIdx] = new BufferObject(gl);
         buffs[vboIdx].rewind();
         vbos[vboIdx].fill(gl, buffs[vboIdx], GL.GL_ARRAY_BUFFER, 
            getBufferUsage(robj.isTextureCoordsDynamic(), streaming));
         vboIdx++;
      }
      
      // point information
      vbos[vboIdx] = new BufferObject(gl);
      buffs[vboIdx].rewind();
      vbos[vboIdx].fill(gl, buffs[vboIdx], GL.GL_ARRAY_BUFFER, GL.GL_DYNAMIC_DRAW);
      vboIdx++;
   }
   
   private int getBufferUsage(boolean dynamic, boolean streaming) {
      if (streaming) {
         return GL3.GL_STREAM_DRAW;
      }
      if (dynamic) {
         return GL3.GL_DYNAMIC_DRAW;
      }
      return GL3.GL_STATIC_DRAW;
   }

   private void createGLOs(GL3 gl, RenderObject robj, 
      GL3Object pointObject, float pointRadius) {
      
      // number of GL objects:
      int nGLOs = robj.numPointGroups();
      if (robj.hasTextureCoords()) {
         nGLOs *= robj.numTextureCoordSets();
      }
      if (robj.hasColors()) {
         nGLOs *= robj.numColorSets();
      }
      if (robj.hasNormals()) {
         nGLOs *= robj.numNormalSets();
      }
      if (robj.hasPositions()) {
         nGLOs *= robj.numPositionSets();
      }
      
      glos = new GL3Object[nGLOs];
      gloInfo = new GLOInfo[nGLOs];
      gloForPrimitives(gl, robj, pointObject, pointRadius);
      
   }
   
   private void gloForPrimitives(GL3 gl, RenderObject robj, 
      GL3Object pointObject, float pointRadius) {
      
      // just vertices
      int gidx = 0;
      int nPointGroups = roState.numPointGroups();
      for (int i=0; i<nPointGroups; ++i) {
         gidx = gloForTextures(gl, robj, i, gidx, pointObject, pointRadius);   
      }
      
   }
   
   private int gloForTextures(GL3 gl, RenderObject robj, int oidx, int gidx, 
      GL3Object pointObject, float pointRadius) {
      if (robj.hasTextureCoords()) {
         for (int i=0; i<textureInfo.length; ++i) {
            gidx = gloForColors(gl, robj, i, oidx, gidx, pointObject, pointRadius);
         }
      } else {
         gidx = gloForColors(gl, robj, -1, oidx, gidx, pointObject, pointRadius);
      }
      return gidx;
   }
   
   private int gloForColors(GL3 gl, RenderObject robj, int tidx, int oidx, int gidx, 
      GL3Object pointObject, float pointRadius) {
      if (robj.hasColors()) {
         for (int i=0; i<colorInfo.length; ++i) {
            gidx = gloForNormals(gl, robj, i, tidx, oidx, gidx, pointObject, pointRadius);
         }
      } else {
         gidx = gloForNormals(gl, robj, -1, tidx, oidx, gidx, pointObject, pointRadius);
      }
      return gidx;
   }
   
   private int gloForNormals(GL3 gl, RenderObject robj, int cidx, int tidx, int oidx, int gidx, 
      GL3Object pointObject, float pointRadius) {
      if (robj.hasNormals()) {
         for (int i=0; i<normalInfo.length; ++i) {
            gidx = gloForPositions(gl, robj, i, cidx, tidx, oidx, gidx, pointObject, pointRadius);
         }
      } else {
         gidx = gloForPositions(gl, robj, -1, cidx, tidx, oidx, gidx, pointObject, pointRadius);
      }
      return gidx;
   }
   
   private int gloForPositions(GL3 gl, RenderObject robj, int nidx, int cidx, int tidx, int oidx, int gidx,
      GL3Object pointObject, float pointRadius) {
      if (robj.hasPositions()) {
         for (int i=0; i<positionInfo.length; ++i) {
            glos[gidx] = buildGLO(gl, robj, i, nidx, cidx, tidx, oidx, pointObject, pointRadius);
            gloInfo[gidx] = new GLOInfo(i, nidx, cidx, tidx, oidx, pointObject, pointRadius);
            gidx++;
         }
      } else {
         glos[gidx] = buildGLO(gl, robj, -1, nidx, cidx, tidx, oidx, pointObject, pointRadius);
         gloInfo[gidx] = new GLOInfo(-1, nidx, cidx, tidx, oidx, pointObject, pointRadius);
         gidx++;
      }
      return gidx;
   }
   
   private GL3Object buildGLO(GL3 gl, RenderObject robj, 
      int pidx, int nidx, int cidx, int tidx, int oidx, 
      GL3Object pointObject, float pointRadius) {
      
      if (pointObject == null) {
         return buildPointGLO(gl, pidx, nidx, cidx, tidx, oidx);
      } else {
         return buildObjectGLO(gl, robj, pidx, nidx, cidx, tidx, oidx, pointObject, pointRadius);
      }
   }
   
   private GL3Object buildPointGLO(GL3 gl, int pidx, int nidx, int cidx, int tidx, int oidx) {
      // collect attributes
      int nattribs = 0;
      if (pidx >= 0) {
         nattribs++;
      }
      if (nidx >= 0) {
         nattribs++;
      }
      if (cidx >= 0) {
         nattribs++;
      }
      if (tidx >= 0) {
         tidx++;
      }
     
      GL3VertexAttributeArray[] attribs = new GL3VertexAttributeArray[nattribs];
      
      
      // position
      int aidx = 0;
      if (pidx >= 0) {
         AttributeInfo pinfo = positionInfo[pidx];
         BufferObject vbo = vbos[pinfo.vboIndex];
         BufferStorage bs = positionPutter.storage();

         // adjust offset based on point group index
         int offset = pinfo.offset + pointGroupOffsets[oidx]*pinfo.stride;
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_POSITION, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, pinfo.stride, pinfo.count);
         aidx++;
      }
      
      // normal
      if (nidx >= 0) {
         AttributeInfo ninfo = normalInfo[nidx];
         BufferObject vbo = vbos[ninfo.vboIndex];
         BufferStorage bs = normalPutter.storage();
         
         // adjust offset based on point group index
         int offset = ninfo.offset + pointGroupOffsets[oidx]*ninfo.stride;
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_NORMAL, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, ninfo.stride, ninfo.count);
         aidx++;
      }
      
      // color
      if (cidx >= 0) {
         // bind and enable color
         AttributeInfo cinfo = colorInfo[cidx];
         BufferObject vbo = vbos[cinfo.vboIndex];
         BufferStorage bs = colorPutter.storage();
         // adjust offset based on point group index
         int offset = cinfo.offset + pointGroupOffsets[oidx]*cinfo.stride;
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_COLOR, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, cinfo.stride, cinfo.count);
         aidx++;
      }
      
      // texture
      if (tidx >= 0) {
         // bind and enable texture
         AttributeInfo tinfo = textureInfo[tidx];
         BufferObject vbo = vbos[tinfo.vboIndex];
         BufferStorage bs = texturePutter.storage();
         // adjust offset based on point group index
         int offset = tinfo.offset + pointGroupOffsets[oidx]*tinfo.stride;
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_TEXTURE, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, tinfo.stride, tinfo.count);
         aidx++;
      }
     
      GL3Object glo = new GL3Object(gl, attribs, null, GL.GL_POINTS);
      
      return glo;
   }
   
   private GL3Object buildObjectGLO(GL3 gl, RenderObject robj, 
      int pidx, int nidx, int cidx, int tidx, int oidx, 
      GL3Object pointObject, float pointRadius) {
      
      // collect attributes
      GL3VertexAttributeArray[] pattribs = pointObject.getGL3VertexAttributes();
      int nattribs = pattribs.length;
      nattribs++;  // radius
      if (pidx >= 0) {
         nattribs++; // instance position
      }
      //      if (nidx >= 0) {
      //         nattribs++;
      //      }
      if (cidx >= 0) {
         nattribs++; // instance color
      }
      if (tidx >= 0) {
         tidx++;     // instance texture
      }
     
      GL3VertexAttributeArray[] attribs = new GL3VertexAttributeArray[nattribs];
      for (int i=0; i<pattribs.length; ++i) {
         attribs[i] = pattribs[i];
      }
     
      int aidx = pattribs.length;
      
      // radius
      attribs[aidx++] = new GL3VertexAttributeArray(vbos[pointInfo.vboIndex], 
         GL3VertexAttribute.INSTANCE_SCALE, GL.GL_FLOAT, 1, false, pointInfo.offset,
         pointInfo.stride, pointInfo.count, robj.numPoints(oidx));
      
      
      // position
      if (pidx >= 0) {
         AttributeInfo pinfo = positionInfo[pidx];
         BufferObject vbo = vbos[pinfo.vboIndex];
         BufferStorage bs = positionPutter.storage();
         // adjust offset based on point group index
         int offset = pinfo.offset + pointGroupOffsets[oidx]*pinfo.stride;
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.INSTANCE_POSITION, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, pinfo.stride, pinfo.count, 1);
         aidx++;
      }
      
      // normal
      //      if (nidx >= 0) {
      //         // bind and enable normal
      //         AttributeInfo ninfo = normalInfo[nidx];
      //         BufferObject vbo = vbos[ninfo.vboIndex];
      //         BufferStorage bs = colorPutter.storage();
      //         // over-ride vertex normal?
      //         // adjust offset based on point group index
      //         int offset = ninfo.offset + pointGroupOffsets[oidx]*ninfo.stride;
      //         attribs[aidx] = new GL3VertexAttributeArray(
      //            vbo, GL3VertexAttribute.VERTEX_NORMAL, GL3Util.getGLType(bs.type()), bs.size(), 
      //            bs.isNormalized(), offset, ninfo.stride, ninfo.count, 1);
      //         aidx++;
      //      }
      
      // color
      if (cidx >= 0) {
         // bind and enable color
         AttributeInfo cinfo = colorInfo[cidx];
         BufferObject vbo = vbos[cinfo.vboIndex];
         BufferStorage bs = colorPutter.storage();
         // adjust offset based on point group index
         int offset = cinfo.offset + pointGroupOffsets[oidx]*cinfo.stride;
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.INSTANCE_COLOR, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, cinfo.stride, cinfo.count, 1);
         aidx++;
      }
      
      // texture
      if (tidx >= 0) {
         // bind and enable texture
         AttributeInfo tinfo = textureInfo[tidx];
         BufferObject vbo = vbos[tinfo.vboIndex];
         BufferStorage bs = texturePutter.storage();
         // adjust offset based on point group index
         int offset = tinfo.offset + pointGroupOffsets[oidx]*tinfo.stride;
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.INSTANCE_TEXTURE, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, tinfo.stride, tinfo.count, 1);
         aidx++;
      }
     
      GL3Object glo = new GL3Object(gl, attribs, pointObject.getGL3ElementAttribute());
      glo.setDrawInfo(pointObject.getStart(), pointObject.getCount(), pointObject.getMode(), 
         robj.numPoints(oidx));
      
      return glo;
   }

   @Override
   public void dispose(GL3 gl) {
      clearAll(gl);
   }

   public RenderObjectIdentifier getRenderObjectIdentifier() {
      return roId;
   }

   public RenderObjectState getRenderObjectState() {
      return roState;
   }

}
