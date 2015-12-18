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
 * Allows easy switching between lines as GL_LINES and as cylinders/ellipsoids/etc...
 */
//=====================================================================================================================
// VBO layouts, ? if exists
// --lines expanded in each VBO for use both as GL_LINES, and as attributes for instanced drawing
// --primary positions/normals/colors/textures interleaved, separated into static/dynamic buffer
// --extra attributes as separate VBOs, one per attribute that contains all extra per-attribute sets
//
//  static?             dynamic?           extra positions?  extra normals?  extra colors?  extra textures?  line info
//  [pos[0][v[p00]] ]   [pos[0][v[p00]] ]  [pos[1][v[p00]]]     ...             ...            ...           [radius]
//  [nrm[0][v[p00]]?]   [nrm[0][v[p00]]?]  [pos[1][v[p01]]]                                                  [length_offset]
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
public class GL3RenderObjectLines extends GL3ResourceBase implements GL3Drawable {

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
      int lineGroup;
      GL3Object lineObject;
      float lineRadius;
      GL3Object headObject;
      float headRadius;
      float headLength;
      public GLOInfo(int pset, int nset, int cset, int tset, int lgroup, 
         GL3Object lineObject, float lineRadius, 
         GL3Object headObject, float headRadius, float headLength) {
         this.positionSet = pset;
         this.normalSet = nset;
         this.colorSet = cset;
         this.textureSet = tset;
         this.lineGroup = lgroup;
         this.lineObject = lineObject;
         this.lineRadius = lineRadius;
         this.headObject = headObject;
         this.headRadius = headRadius;
         this.headLength = headLength;
      }
   }
   
   AttributeInfo[] positionInfo;
   AttributeInfo[] normalInfo;
   AttributeInfo[] colorInfo;
   AttributeInfo[] textureInfo;
   AttributeInfo[] lineInfo;
   AttributeInfo[] headInfo;
   int[] lineGroupOffsets;       // separation between point groups (in # of vertices, for binding correct location)

   int staticVertexSize;
   int staticMask;
   int dynamicVertexSize;
   int dynamicMask;
   int nVertices;
   
   BufferObject[] vbos;
   GL3Object[] lglos;  // line GLOs
   GL3Object[] hglos;  // head glos
   GLOInfo[] gloInfo;
   
   private static final int POSITION_FLAG = 0x01;
   private static final int NORMAL_FLAG = 0x02;
   private static final int COLOR_FLAG = 0x04;
   private static final int TEXTURE_FLAG = 0x08;

   // track version numbers so we can detect what has changed since last use
   RenderObjectVersion lastVersionInfo;
   GL3Object lastLineObject;
   GL3Object lastHeadObject;
   float lastLineRadius;
   float lastHeadRadius;
   float lastHeadLength;

   public GL3RenderObjectLines(RenderObject r) {
      
      this.roId = r.getIdentifier();
      this.roState = r.getStateInfo();
      
      vbos = null;
      lglos = null;
      hglos = null;
      gloInfo = null;
      
      positionInfo = null;
      normalInfo = null;
      colorInfo = null;
      textureInfo = null;
      lineGroupOffsets = null;
      
      lastVersionInfo = null;
      lastLineObject = null;
      lastLineRadius = -1;
      lastHeadObject = null;
      lastHeadRadius = -1;
      lastHeadLength = -1;
      
      staticVertexSize = 0;
      staticMask = 0; 
      dynamicVertexSize = 0;
      dynamicMask = 0;
      nVertices = 0;
   }

   private int getGLOidx() {

      int idx = roState.getLineGroupIdx();

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

   public void drawLines(GL3 gl) {
      if (!isValid() || roState.numLineGroups() == 0) {
         return;
      }
      int gloIndex = getGLOidx();
      lglos[gloIndex].draw(gl);  // line
      if (hglos != null) {
         hglos[gloIndex].draw(gl); // head
      }
   }

   // maybe update VBOs
   /**
    * Potentially update the internal VAOs
    * @param gl
    * @param robj
    * @return true if updated, false if nothing changed
    */
   public boolean maybeUpdate(GL3 gl, RenderObject robj, 
      GL3Object lineObject, float lineRadius,
      GL3Object headObject, float headRadius, float headLength) {
      
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
            clearAndRebuild(gl, robj, lineObject, lineRadius, 
               headObject, headRadius, headLength);
            return true;
         } else if (robj.getVersion() != lastVersionInfo.getVersion()) {
            
            // find what has changed, see if we can update it
            boolean streaming = robj.isStreaming();
            if (!robj.isDynamic() || streaming) {
               // static or streaming
               clearAndRebuild(gl, robj, lineObject, lineRadius, 
                  headObject, headRadius, headLength);
               return true;
            } else if (
               robj.getVerticesVersion() != lastVersionInfo.getVerticesVersion() ||
               robj.getPointsVersion() != lastVersionInfo.getPointsVersion()) {
               // vertices (all VBOs need update) or point groups (VBO size changes)
               clearAndRebuild(gl, robj, lineObject, lineRadius, 
                  headObject, headRadius, headLength);
               return true;
            } else {
               
               // check if we should update all dynamic, update part dynamic, or rebuild everything
               int updateFlag = 0;
               
               if (robj.getPositionsVersion() != lastVersionInfo.getPositionsVersion()) {
                  boolean positionsDynamic = ((dynamicMask & POSITION_FLAG) != 0);
                  if  (!positionsDynamic || !robj.isPositionsDynamic()) {
                     // positions are static or static/dynamic property changed
                     clearAndRebuild(gl, robj, lineObject, lineRadius, 
                        headObject, headRadius, headLength);
                     return true;
                  } else {
                     updateFlag |= POSITION_FLAG;
                  }
               }
               
               if (robj.getNormalsVersion() != lastVersionInfo.getNormalsVersion()) {
                  boolean normalsDynamic = ((dynamicMask & NORMAL_FLAG) != 0);
                  if (!normalsDynamic || !robj.isNormalsDynamic()) {
                     // normals are static or static/dynamic property changed
                     clearAndRebuild(gl, robj, lineObject, lineRadius, 
                        headObject, headRadius, headLength);
                     return true;
                  } else {
                     updateFlag |= NORMAL_FLAG;
                  }
               }
                  
               if (robj.getColorsVersion() != lastVersionInfo.getColorsVersion()) {
                  boolean colorsDynamic = ((dynamicMask & COLOR_FLAG) != 0);
                  if (!colorsDynamic || !robj.isColorsDynamic()) {
                     // colors are static or static/dynamic property changed
                     clearAndRebuild(gl, robj, lineObject, lineRadius, 
                        headObject, headRadius, headLength);
                     return true;
                  } else {
                     updateFlag |= COLOR_FLAG;
                  }
               }
               
               if (robj.getTextureCoordsVersion() != lastVersionInfo.getTextureCoordsVersion()) {
                  boolean texturesDynamic = ((dynamicMask & TEXTURE_FLAG) != 0);
                  if (!texturesDynamic || !robj.isTextureCoordsDynamic()) {
                     // textures are static or static/dynamic property changed
                     clearAndRebuild(gl, robj, lineObject, lineRadius, 
                        headObject, headRadius, headLength);
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
         if (lastLineObject != lineObject || lastHeadObject != headObject) {
            
            createGLOs(gl, robj, lineObject, lineRadius, 
               headObject, headRadius, headLength);
            
            lastLineObject = lineObject;
            lastHeadObject = headObject;
            updated = true;
         } 
         
      } // end synchronize
      
      // radius/lengths
      if (lastLineRadius != lineRadius || lastHeadRadius != headRadius 
         ||lastHeadLength != headLength) {
         updateLineVBO(gl, lineRadius, headRadius, headLength);
         lastLineRadius = lineRadius;
         lastHeadRadius = headRadius;
         lastHeadLength = headLength;
         updated = true;
      }
      
      return updated;
   }
   
   private void updateLineVBO(GL3 gl, float lineRadius, float headRadius, float headLength) {
      ByteBuffer buff = vbos[lineInfo[0].vboIndex].mapNewBuffer(gl);
      buff.putFloat(lineRadius);
      buff.putFloat(headLength);
      buff.putFloat(1.0f);   // subtract from length
      buff.putFloat(0.0f);
      buff.putFloat(0.0f);
      buff.putFloat(headRadius);
      buff.putFloat(headLength);
      buff.putFloat(0.0f);   // do not from length
      buff.putFloat(headLength);
      buff.putFloat(1.0f);   // subtract from length
      vbos[lineInfo[0].vboIndex].unmapBuffer(gl);
   }
   
   private void updateDynamicVBOs(GL3 gl, RenderObject robj, int updateMask, boolean replace) {
      ByteBuffer[] buffs = new ByteBuffer[vbos.length];
      
      int vidx = 0;
      
      for (int lg=0; lg<robj.numLineGroups(); ++lg) {
         List<int[]> lines = robj.getLines(lg);
         
         for (int[] line : lines) {
            for (int j=0; j<2; ++j) {
               VertexIndexSet v = robj.getVertex(line[j]);
               
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
      }
    
      // unmap
      for (int i=0; i<buffs.length; ++i) {
         if (buffs[i] != null) {
            vbos[i].unmapBuffer(gl);
         }
      }
      
   }

   public void draw(GL3 gl) {
      drawLines(gl);
   }

   public boolean isValid() {
      return lglos != null;
   }

   @Override
   public void init(GL3 gl) {
      // nothing, must be initialized with RenderObject
   }
   
   public void init(GL3 gl, RenderObject robj, GL3Object lineObject, float lineRadius,
      GL3Object headObject, float headRadius, float headLength) {
      
      synchronized(robj) {
         // commit info
         if (!robj.isCommitted()) {
            robj.commit();
         }
         // stores VBO indices/offsets/strides for each attribute 
         build(gl, robj, lineObject, lineRadius, headObject, headRadius, headLength);
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
      lineInfo = null;
      headInfo = null;
      lineGroupOffsets = null;
      
      // clear object storage
      if (lglos != null) {
         for (GL3Object glo : lglos) {
            glo.release(gl);
         }
      }
      lglos = null;
      if (hglos != null) {
         for (GL3Object glo : hglos) {
            glo.release(gl);
         }
      }
      hglos = null;
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
      lastLineObject = null;
      lastLineRadius = -1;
      lastHeadObject = null;
      lastHeadRadius = -1;
      lastHeadLength = -1;
   }
   
   private void clearAndRebuild(GL3 gl, RenderObject robj, 
      GL3Object lineObject, float lineRadius,
      GL3Object headObject, float headRadius, float headLength ) {
      clearAll(gl);
      build(gl, robj, lineObject, lineRadius, headObject, headRadius, headLength);
   }
   
   private void build(GL3 gl, RenderObject robj, 
      GL3Object lineObject, float lineRadius,
      GL3Object headObject, float headRadius, float headLength) {
      
      // create and fill VBOs
      createVBOs(gl, robj, lineObject, lineRadius,
         headObject, headRadius, headLength);
      createGLOs(gl, robj, lineObject, lineRadius,
         headObject, headRadius, headLength);

      // mark version numbers
      lastVersionInfo = robj.getVersionInfo();
      lastLineObject = lineObject;
      lastLineRadius = lineRadius;
      lastHeadObject = headObject;
      lastHeadRadius = headRadius;
      lastHeadLength = headLength;
   }
      
   private AttributeInfo[] createAttributeInfoArrays(int n) {
      AttributeInfo[] ai = new AttributeInfo[n];
      for (int i=0; i<ai.length; ++i) {
         ai[i] = new AttributeInfo();
      }
      return ai;
   }
   
   private void createVBOs(GL3 gl, RenderObject robj,
      GL3Object lineObject, float lineRadius,
      GL3Object headObject, float headRadius, float headLength) {
      
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
      lineGroupOffsets = new int[robj.numLineGroups ()+1];
      lineInfo = createAttributeInfoArrays(2);
      headInfo = createAttributeInfoArrays(2);

      staticVertexSize = 0;
      staticMask = 0;
      dynamicVertexSize = 0;
      dynamicMask = 0;
      boolean streaming = robj.isStreaming();
      nVertices = robj.numVertices();

      int numLinesTotal = 0;
      for (int lg = 0; lg < robj.numLineGroups(); ++lg) {
    	  lineGroupOffsets[lg] = numLinesTotal;
         numLinesTotal += robj.numLines(lg);
      }
      lineGroupOffsets[robj.numLineGroups ()] = numLinesTotal;
      
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
         positionInfo[0].count = 2*numLinesTotal;

         for (int i=1; i<positionInfo.length; ++i) {
            positionInfo[i].offset = (i-1)*POSITION_BYTES*2*numLinesTotal;
            positionInfo[i].stride = POSITION_BYTES;
            positionInfo[i].count = 2*numLinesTotal;
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
         normalInfo[0].count = 2*numLinesTotal;
         
         for (int i=1; i<normalInfo.length; ++i) {
            normalInfo[i].offset = (i-1)*NORMAL_BYTES*2*numLinesTotal;
            normalInfo[i].stride = NORMAL_BYTES;
            normalInfo[i].count = 2*numLinesTotal;
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
         colorInfo[0].count = 2*numLinesTotal;

         for (int i=1; i<colorInfo.length; ++i) {
            colorInfo[i].offset = (i-1)*COLOR_BYTES*2*numLinesTotal;
            colorInfo[i].stride = COLOR_BYTES;
            colorInfo[i].count = 2*numLinesTotal;
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
         textureInfo[0].count = 2*numLinesTotal;

         for (int i=1; i<textureInfo.length; ++i) {
            textureInfo[i].offset = (i-1)*TEXTURE_BYTES*2*numLinesTotal;
            textureInfo[i].stride = TEXTURE_BYTES;
            textureInfo[i].count = 2*numLinesTotal;
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
      // line-specific information
      // radius
      lineInfo[0].offset = 0;
      lineInfo[0].stride = 10*GLSupport.FLOAT_SIZE;
      lineInfo[0].count = 1;
      lineInfo[0].vboIndex = vboIdx;
      // length/offsets
      lineInfo[1].offset = 1*GLSupport.FLOAT_SIZE;
      lineInfo[1].stride = 10*GLSupport.FLOAT_SIZE;
      lineInfo[1].count = 1;
      lineInfo[1].vboIndex = vboIdx;
      
      // head radius
      headInfo[0].offset = 5*GLSupport.FLOAT_SIZE;
      headInfo[0].stride = 10*GLSupport.FLOAT_SIZE;
      headInfo[0].count = 1;
      headInfo[0].vboIndex = vboIdx;
      // length/offsets
      headInfo[1].offset = 6*GLSupport.FLOAT_SIZE;
      headInfo[1].stride = 10*GLSupport.FLOAT_SIZE;
      headInfo[1].count = 1;
      headInfo[1].vboIndex = vboIdx;   
      vboIdx++;
      
      int nVBOs = vboIdx;
      
      // create buffers for VBOs
      ByteBuffer[] buffs = new ByteBuffer[nVBOs];

      vboIdx = 0;
      if (staticVertexSize > 0) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(2*numLinesTotal*staticVertexSize);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      // dynamic
      if (!streaming && robj.isDynamic()) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(2*numLinesTotal*dynamicVertexSize);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      
      // extra attribute sets
      if (robj.numPositionSets() > 1 ) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(2*numLinesTotal*(robj.numPositionSets()-1)*POSITION_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      if (robj.numNormalSets() > 1) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(2*numLinesTotal*(robj.numNormalSets()-1)*NORMAL_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      if (robj.numColorSets() > 1) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(2*numLinesTotal*(robj.numColorSets()-1)*COLOR_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      if (robj.numTextureCoordSets() > 1) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(2*numLinesTotal*(robj.numTextureCoordSets()-1)*TEXTURE_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      
      // fill vertex buffers
      int vidx = 0;
      for (int lg=0; lg<robj.numLineGroups(); ++lg) {
         List<int[]> lines = robj.getLines(lg);
         
         for (int[] line : lines) {
            for (int j=0; j<2; ++j) {
               VertexIndexSet v = robj.getVertex(line[j]);
               
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
      }
      
      // radius/length_offset
      buffs[vboIdx] = ByteBuffer.allocateDirect(10*GLSupport.FLOAT_SIZE);
      buffs[vboIdx].order(ByteOrder.nativeOrder());
      // length info
      buffs[vboIdx].putFloat(lineRadius);
      buffs[vboIdx].putFloat(headLength);
      buffs[vboIdx].putFloat(1.0f);   // subtract from length
      buffs[vboIdx].putFloat(0.0f);
      buffs[vboIdx].putFloat(0.0f);
      buffs[vboIdx].putFloat(headRadius);
      buffs[vboIdx].putFloat(headLength);
      buffs[vboIdx].putFloat(0.0f);   // do not from length
      buffs[vboIdx].putFloat(headLength);
      buffs[vboIdx].putFloat(1.0f);   // subtract from length
      
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
      
      // line information
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
      GL3Object lineObject, float lineRadius,
      GL3Object headObject, float headRadius, float headLength) {
      
      // number of GL objects:
      int nGLOs = robj.numLineGroups();
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
      
      lglos = new GL3Object[nGLOs];
      if (headObject != null) {
         hglos = new GL3Object[nGLOs];
      } else {
         hglos = null;
      }
      gloInfo = new GLOInfo[nGLOs];
      gloForPrimitives(gl, robj, lineObject, lineRadius,
         headObject, headRadius, headLength);
      
   }
   
   private void gloForPrimitives(GL3 gl, RenderObject robj, 
      GL3Object lineObject, float lineRadius,
      GL3Object headObject, float headRadius, float headLength) {
      
      // just vertices
      int gidx = 0;
      int nLineGroups = robj.numLineGroups();
      for (int i=0; i<nLineGroups; ++i) {
         gidx = gloForTextures(gl, robj, i, gidx, lineObject, lineRadius,
            headObject, headRadius, headLength);   
      }
      
   }
   
   private int gloForTextures(GL3 gl, RenderObject robj, int oidx, int gidx, 
      GL3Object lineObject, float lineRadius,
      GL3Object headObject, float headRadius, float headLength) {
      if (robj.hasTextureCoords()) {
         for (int i=0; i<textureInfo.length; ++i) {
            gidx = gloForColors(gl, robj, i, oidx, gidx, lineObject, lineRadius,
               headObject, headRadius, headLength);
         }
      } else {
         gidx = gloForColors(gl, robj, -1, oidx, gidx, lineObject, lineRadius,
            headObject, headRadius, headLength);
      }
      return gidx;
   }
   
   private int gloForColors(GL3 gl, RenderObject robj, int tidx, int oidx, int gidx, 
      GL3Object lineObject, float lineRadius,
      GL3Object headObject, float headRadius, float headLength) {
      if (robj.hasColors()) {
         for (int i=0; i<colorInfo.length; ++i) {
            gidx = gloForNormals(gl, robj, i, tidx, oidx, gidx, lineObject, lineRadius,
               headObject, headRadius, headLength);
         }
      } else {
         gidx = gloForNormals(gl, robj, -1, tidx, oidx, gidx, lineObject, lineRadius,
            headObject, headRadius, headLength);
      }
      return gidx;
   }
   
   private int gloForNormals(GL3 gl, RenderObject robj, int cidx, int tidx, int oidx, int gidx, 
      GL3Object lineObject, float lineRadius,
      GL3Object headObject, float headRadius, float headLength) {
      if (robj.hasNormals()) {
         for (int i=0; i<normalInfo.length; ++i) {
            gidx = gloForPositions(gl, robj, i, cidx, tidx, oidx, gidx, lineObject, lineRadius,
               headObject, headRadius, headLength);
         }
      } else {
         gidx = gloForPositions(gl, robj, -1, cidx, tidx, oidx, gidx, lineObject, lineRadius,
            headObject, headRadius, headLength);
      }
      return gidx;
   }
   
   private int gloForPositions(GL3 gl, RenderObject robj, int nidx, int cidx, int tidx, int oidx, int gidx,
      GL3Object lineObject, float lineRadius,
      GL3Object headObject, float headRadius, float headLength) {
      
      boolean hasLengthOffset = ((hglos != null) && (headObject != null));
      if (robj.hasPositions()) {
         for (int i=0; i<positionInfo.length; ++i) {
            lglos[gidx] = buildLineGLO(gl, robj, i, nidx, cidx, tidx, oidx, lineObject, 
               hasLengthOffset);
            if (hglos != null && headObject != null) {
               hglos[gidx] = buildObjectGLO(gl, robj, i, nidx, cidx, tidx, oidx,
                  headInfo, headObject, hasLengthOffset);
            }
            gloInfo[gidx] = new GLOInfo(i, nidx, cidx, tidx, oidx, lineObject, lineRadius,
               headObject, headRadius, headLength);
            gidx++;
         }
      } else {
         lglos[gidx] = buildLineGLO(gl, robj, -1, nidx, cidx, tidx, oidx, lineObject, hasLengthOffset);
         if (hasLengthOffset) {
            hglos[gidx] = buildObjectGLO(gl, robj, -1, nidx, cidx, tidx, oidx,
               headInfo, headObject, true);
         }
         gloInfo[gidx] = new GLOInfo(-1, nidx, cidx, tidx, oidx, lineObject, lineRadius,
            headObject, headRadius, headLength);
         gidx++;
      }
      return gidx;
   }
   
   private GL3Object buildLineGLO(GL3 gl, RenderObject robj,
      int pidx, int nidx, int cidx, int tidx, int oidx, 
      GL3Object lineObject, boolean hasLengthOffset) {
      
      if (lineObject == null) {
         return buildLineGLO(gl, pidx, nidx, cidx, tidx, oidx);
      } else {
         return buildObjectGLO(gl, robj,
            pidx, nidx, cidx, tidx, oidx, lineInfo, lineObject, hasLengthOffset);
      }
   }
   
   private GL3Object buildLineGLO(GL3 gl, int pidx, int nidx, int cidx, int tidx, int oidx) {
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
      
      int lineCount2 = 2*(lineGroupOffsets[oidx+1] - lineGroupOffsets[oidx]);
      
      // position
      int aidx = 0;
      if (pidx >= 0) {
         AttributeInfo pinfo = positionInfo[pidx];
         BufferObject vbo = vbos[pinfo.vboIndex];
         BufferStorage bs = positionPutter.storage();
         
         // adjust offset based on line group index
         int offset = pinfo.offset + 2*lineGroupOffsets[oidx]*pinfo.stride;
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_POSITION, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, pinfo.stride, lineCount2);
         aidx++;
      }
      
      // normal
      if (nidx >= 0) {
         AttributeInfo ninfo = normalInfo[nidx];
         BufferObject vbo = vbos[ninfo.vboIndex];
         BufferStorage bs = normalPutter.storage();
         
         // adjust offset based on line group index
         int offset = ninfo.offset + 2*lineGroupOffsets[oidx]*ninfo.stride;
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_NORMAL, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, ninfo.stride, lineCount2);
         aidx++;
      }
      
      // color
      if (cidx >= 0) {
         // bind and enable color
         AttributeInfo cinfo = colorInfo[cidx];
         BufferObject vbo = vbos[cinfo.vboIndex];
         BufferStorage bs = colorPutter.storage();
         
         // adjust offset based on line group index
         int offset = cinfo.offset + 2*lineGroupOffsets[oidx]*cinfo.stride;
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_COLOR, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, cinfo.stride, lineCount2);
         aidx++;
      }
      
      // texture
      if (tidx >= 0) {
         // bind and enable texture
         AttributeInfo tinfo = textureInfo[tidx];
         BufferObject vbo = vbos[tinfo.vboIndex];
         BufferStorage bs = texturePutter.storage();
         
         // adjust offset based on line group index
         int offset = tinfo.offset + 2*lineGroupOffsets[oidx]*tinfo.stride;
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_TEXTURE, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, tinfo.stride, lineCount2);
         aidx++;
      }
     
      GL3Object glo = new GL3Object(gl, attribs, null, GL.GL_LINES);
      
      return glo;
   }
   
   private GL3Object buildObjectGLO(GL3 gl, RenderObject robj,
      int pidx, int nidx, int cidx, int tidx, int oidx, 
      AttributeInfo[] radiusLengthInfo,
      GL3Object object, boolean hasLengthOffset) {
      
      // collect attributes
      GL3VertexAttributeArray[] pattribs = object.getGL3VertexAttributes();
      int nattribs = pattribs.length;
      nattribs++;  // radius
      if (hasLengthOffset) {
         nattribs++;
      }
      if (pidx >= 0) {
         nattribs+=2; // line positions
      }
      //      if (nidx >= 0) {
      //         nattribs++;
      //      }
      if (cidx >= 0) {
         nattribs+=2; // line color
      }
      if (tidx >= 0) {
         tidx+=2;     // line texture
      }
      
      // number of lines in this group
      int lineCount = lineGroupOffsets[oidx+1] - lineGroupOffsets[oidx];
     
      GL3VertexAttributeArray[] attribs = new GL3VertexAttributeArray[nattribs];
      for (int i=0; i<pattribs.length; ++i) {
         attribs[i] = pattribs[i];
      }
     
      int aidx = pattribs.length;
      
      // radius
      attribs[aidx++] = new GL3VertexAttributeArray(vbos[radiusLengthInfo[0].vboIndex], 
         GL3VertexAttribute.LINE_RADIUS, GL.GL_FLOAT, 1, false, radiusLengthInfo[0].offset,
            radiusLengthInfo[0].stride, radiusLengthInfo[0].count, lineCount);
      
      // head length
      if (hasLengthOffset) {
         attribs[aidx++] = new GL3VertexAttributeArray(vbos[radiusLengthInfo[1].vboIndex], 
            GL3VertexAttribute.LINE_LENGTH_OFFSET, GL.GL_FLOAT, 4, false, radiusLengthInfo[1].offset,
               radiusLengthInfo[1].stride, radiusLengthInfo[1].count, lineCount);
      }
      
      // position
      if (pidx >= 0) {
         AttributeInfo pinfo = positionInfo[pidx];
         BufferObject vbo = vbos[pinfo.vboIndex];
         BufferStorage bs = positionPutter.storage();
         
         // adjust offset based on line group index
         int offset = pinfo.offset + 2*lineGroupOffsets[oidx]*pinfo.stride;
         attribs[aidx++] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.LINE_BOTTOM_POSITION, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, 2*pinfo.stride, lineCount, 1);
         attribs[aidx++] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.LINE_TOP_POSITION, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset+pinfo.stride, 2*pinfo.stride, lineCount, 1);
      }
      
      // color
      if (cidx >= 0) {
         // bind and enable color
         AttributeInfo cinfo = colorInfo[cidx];
         BufferObject vbo = vbos[cinfo.vboIndex];
         BufferStorage bs = colorPutter.storage();
         
         // adjust offset based on line group index
         int offset = cinfo.offset + 2*lineGroupOffsets[oidx]*cinfo.stride;
         attribs[aidx++] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.LINE_BOTTOM_COLOR, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, 2*cinfo.stride, lineCount, 1);
         attribs[aidx++] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.LINE_TOP_COLOR, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset+cinfo.stride, 2*cinfo.stride, lineCount, 1);
      }
      
      // texture
      if (tidx >= 0) {
         // bind and enable texture
         AttributeInfo tinfo = textureInfo[tidx];
         BufferObject vbo = vbos[tinfo.vboIndex];
         BufferStorage bs = texturePutter.storage();
         
         // adjust offset based on line group index
         int offset = tinfo.offset + 2*lineGroupOffsets[oidx]*tinfo.stride;
         attribs[aidx++] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.LINE_BOTTOM_TEXTURE, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset, 2*tinfo.stride, lineCount, 1);
         attribs[aidx++] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.LINE_TOP_TEXTURE, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), offset+tinfo.stride, 2*tinfo.stride, lineCount, 1);
      }
     
      GL3Object glo = new GL3Object(gl, attribs, object.getGL3ElementAttribute());
      glo.setDrawInfo(object.getStart(), object.getCount(), object.getMode(), 
         lineCount);
      
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
