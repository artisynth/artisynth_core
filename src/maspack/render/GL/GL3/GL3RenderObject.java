package maspack.render.GL.GL3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectIdentifier;
import maspack.render.RenderObject.RenderObjectState;
import maspack.render.RenderObject.RenderObjectVersion;
import maspack.render.RenderObject.VertexIndexSet;
import maspack.render.Renderer.DrawMode;
import maspack.render.GL.GL3.GL3Object.DrawType;

//=====================================================================================================================
// VBO layouts, ? if exists
// --primary positions/normals/colors/textures interleaved, separated into static/dynamic buffer
// --extra attributes as separate VBOs, one per attribute that contains all extra per-attribute sets
// --separate IBO for all primitive indices, points/lines/triangles
//
//  static?           dynamic?       extra positions?  extra normals?  extra colors?  extra textures?  primitives? 
//  [pos[0][v0] ]     [pos[0][v0] ]  [pos[1][v0]]      [nrm[1][v0]]    [clr[1][v0]]   [tex[1][v0]]     [pnt[0][0]? ]
//  [nrm[0][v0]?]     [nrm[0][v0]?]  [pos[1][v1]]      [nrm[1][v1]]    [clr[1][v1]]   [tex[1][v1]]       ...
//  [clr[0][v0]?]     [clr[0][v0]?]    ...               ...             ...            ...            [pnt[0][r]? ]
//  [tex[0][v0]?]     [tex[0][v0]?]  [pos[1][vn]]      [nrm[1][vn]]    [clr[1][vn]]   [tex[1][vn]]       ... ...
//  [pos[0][v1] ]     [pos[0][v1] ]  [pos[2][v0]]      [nrm[2][v0]]    [clr[2][v0]]   [tex[2][v0]]     [pnt[s][t]? ]
//  [nrm[0][v1]?]     [nrm[0][v1]?]  [pos[2][v1]]      [nrm[2][v1]]    [clr[2][v1]]   [tex[2][v1]]     [line[0][0]?]
//  [clr[0][v1]?]     [clr[0][v1]?]    ...               ...             ...            ...              ...
//  [tex[0][v1]?]     [tex[0][v1]?]  [pos[2][vn]]      [nrm[2][vn]]    [clr[2][vn]]   [tex[2][vn]]     [line[0][u]?]
//    ...               ...            ... ...           ... ...         ... ...        ... ...          ... ...
//  [pos[0][vn] ]     [pos[0][vn] ]  [pos[i][v0]]      [nrm[j][v0]]    [clr[k][v0]]   [tex[l][v0]]     [line[v][w]?]
//  [nrm[0][vn]?]     [nrm[0][vn]?]  [pos[i][v1]]      [nrm[j][v1]]    [clr[k][v1]]   [tex[l][v1]]     [tri[0][0]? ]
//  [clr[0][vn]?]     [clr[0][vn]?]    ...               ...             ...            ...              ... ...
//  [tex[0][vn]?]     [tex[0][vn]?]  [pos[i][vn]]      [nrm[j][vn]]    [clr[k][vn]]   [tex[l][vn]]     [tri[x][y]? ]
//
//=====================================================================================================================
public class GL3RenderObject extends GL3ResourceBase implements GL3Drawable {

   private RenderObjectIdentifier roId;
   private RenderObjectState roState;
   private PositionBufferPutter positionPutter;
   private NormalBufferPutter normalPutter;
   private ColorBufferPutter colorPutter;
   private TextureCoordBufferPutter texturePutter;
   private IndexBufferPutter indexPutter;

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
      int primitiveGroup;
      int primitiveType;
      public GLOInfo(int pset, int nset, int cset, int tset, int pgroup, int ptype) {
         this.positionSet = pset;
         this.normalSet = nset;
         this.colorSet = cset;
         this.textureSet = tset;
         this.primitiveGroup = pgroup;
         this.primitiveType = ptype;
      }
   }

   AttributeInfo[] positionInfo;
   AttributeInfo[] normalInfo;
   AttributeInfo[] colorInfo;
   AttributeInfo[] textureInfo;
   AttributeInfo[] pointsInfo;
   AttributeInfo[] linesInfo;
   AttributeInfo[] trianglesInfo;

   int staticVertexSize;
   int staticMask;
   int dynamicVertexSize;
   int dynamicMask;
   int nVertices;

   BufferObject[] vbos;
   GL3Object[] glos;
   GLOInfo[] gloInfo;

   private static final int VERTICES = 0;
   private static final int POINTS = 1;
   private static final int LINES = 2;
   private static final int TRIANGLES = 3;

   private static final int POSITION_FLAG = 0x01;
   private static final int NORMAL_FLAG = 0x02;
   private static final int COLOR_FLAG = 0x04;
   private static final int TEXTURE_FLAG = 0x08;

   // track version numbers so we can detect what has changed since last use
   RenderObjectVersion lastVersionInfo;

   public GL3RenderObject(RenderObject r) {

      this.roId = r.getIdentifier();
      this.roState = r.getStateInfo();

      vbos = null;
      glos = null;
      gloInfo = null;

      positionInfo = null;
      normalInfo = null;
      colorInfo = null;
      textureInfo = null;
      pointsInfo = null;
      linesInfo = null;
      trianglesInfo = null;

      lastVersionInfo = null;

      staticVertexSize = 0;
      staticMask = 0; 
      dynamicVertexSize = 0;
      dynamicMask = 0;
      nVertices = 0;
   }

   private int getGLOidx(int primitive) {

      int idx = 0;

      // vertex/point/line/triangle
      switch (primitive) {
         case VERTICES:
            idx = 0;
            break;
         case POINTS:
            idx = 1 + roState.getPointGroupIdx();
            break;
         case LINES:
            idx = 1 + roState.numPointGroups() + roState.getLineGroupIdx();
            break;
         case TRIANGLES:
            idx = 1 + roState.numPointGroups() + roState.numLineGroups() + roState.getTriangleGroupIdx();
            break;
      }

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
      if (!isValid() || pointsInfo == null || pointsInfo.length == 0) {
         return;
      }
      int gloIndex = getGLOidx(POINTS);
      glos[gloIndex].draw(gl); 
   }

   public void drawLines(GL3 gl) {
      if (!isValid() || linesInfo == null || linesInfo.length == 0) {
         return;
      }
      int gloIndex = getGLOidx(LINES);
      glos[gloIndex].draw(gl); 
   }

   public void drawTriangles(GL3 gl) {
      if (!isValid() || trianglesInfo == null || trianglesInfo.length == 0) {
         return;
      }
      int gloIndex = getGLOidx(TRIANGLES);
      glos[gloIndex].draw(gl); 
   }

   private int getGLMode(DrawMode mode) {
      switch (mode) {
         case LINES:
            return GL.GL_LINES;
         case LINE_LOOP:
            return GL.GL_LINE_LOOP;
         case LINE_STRIP:
            return GL.GL_LINE_STRIP;
         case POINTS:
            return GL.GL_POINTS;
         case TRIANGLES:
            return GL.GL_TRIANGLES;
         case TRIANGLE_FAN:
            return GL.GL_TRIANGLE_FAN;
         case TRIANGLE_STRIP:
            return GL.GL_TRIANGLE_STRIP;
      }

      throw new RuntimeException("Unknown mode: " + mode);
   }

   public void drawVertices(GL3 gl, DrawMode mode) {
      if (!isValid() || nVertices == 0) {
         return;
      }
      int gloIndex = getGLOidx(VERTICES);
      int glMode = getGLMode(mode);
      glos[gloIndex].drawArrays(gl, glMode);
   }

   // maybe update VBOs
   public boolean maybeUpdate(GL3 gl, RenderObject robj) {

      boolean updated = false;

      synchronized(robj) {

         roState = robj.getStateInfo();

         // commit modified data if not already
         if (!robj.isCommitted()) {
            robj.commit();
         }

         // check version numbers and see if we need to update
         if (lastVersionInfo == null) {
            clearAndRebuild(gl, robj);
            return true;
         } else if (robj.getVersion() != lastVersionInfo.getVersion()) {

            // find what has changed, see if we can update it
            boolean streaming = robj.isStreaming();
            if (!robj.isDynamic() || streaming) {
               clearAndRebuild(gl, robj);
               return true;
            } else if (robj.getVerticesVersion() != lastVersionInfo.getVerticesVersion()) {
               // vertices have changed, so all VBOs need to be updated
               clearAndRebuild(gl, robj);
               return true;
            } else {

               // check if we should update all dynamic, update part dynamic, or rebuild everything
               int updateFlag = 0;
               if (robj.getPositionsVersion() != lastVersionInfo.getPositionsVersion()) {
                  boolean positionsDynamic = ((dynamicMask & POSITION_FLAG) != 0);
                  if  (!positionsDynamic || !robj.isPositionsDynamic()) {
                     // positions are static or static/dynamic property changed
                     clearAndRebuild(gl, robj);
                     return true;
                  } else {
                     updateFlag |= POSITION_FLAG;
                  }
               }

               if (robj.getNormalsVersion() != lastVersionInfo.getNormalsVersion()) {
                  boolean normalsDynamic = ((dynamicMask & NORMAL_FLAG) != 0);
                  if (!normalsDynamic || !robj.isNormalsDynamic()) {
                     // normals are static or static/dynamic property changed
                     clearAndRebuild(gl, robj);
                     return true;
                  } else {
                     updateFlag |= NORMAL_FLAG;
                  }
               }

               if (robj.getColorsVersion() != lastVersionInfo.getColorsVersion()) {
                  boolean colorsDynamic = ((dynamicMask & COLOR_FLAG) != 0);
                  if (!colorsDynamic || !robj.isColorsDynamic()) {
                     // colors are static or static/dynamic property changed
                     clearAndRebuild(gl, robj);
                     return true;
                  } else {
                     updateFlag |= COLOR_FLAG;
                  }
               }

               if (robj.getTextureCoordsVersion() != lastVersionInfo.getTextureCoordsVersion()) {
                  boolean texturesDynamic = ((dynamicMask & TEXTURE_FLAG) != 0);
                  if (!texturesDynamic || !robj.isTextureCoordsDynamic()) {
                     // textures are static or static/dynamic property changed
                     clearAndRebuild(gl, robj);
                     return true;
                  } else {
                     updateFlag |= TEXTURE_FLAG;
                  }
               }

               if (updateFlag != 0) {
                  updateDynamicVBOs(gl, robj, updateFlag, updateFlag == dynamicMask);
                  updated = true;
               }


               // maybe rebuild primitives
               if (lastVersionInfo.getPointsVersion() != robj.getPointsVersion()
                  || lastVersionInfo.getLinesVersion() != robj.getLinesVersion()
                  || lastVersionInfo.getTrianglesVersion() != robj.getTrianglesVersion()) {

                  // check if any primitive before
                  int pcount = pointsInfo.length + linesInfo.length + trianglesInfo.length;
                  // any primitives now
                  int npcount = robj.numPointGroups() + robj.numLineGroups() + robj.numTriangleGroups();

                  // adjust accordingly
                  if (pcount != npcount) {
                     if (pcount == 0) {
                        // create a VBO to hold new primitives
                        vbos = Arrays.copyOf(vbos, vbos.length+1); // add primitive VBO
                     } else if (npcount == 0) {
                        // discard VBO since no primitives
                        BufferObject pvbo = vbos[vbos.length-1];
                        pvbo.release(gl); // release and maybe dispose
                        vbos = Arrays.copyOf(vbos, vbos.length-1); // remove primitive VBO
                     }
                  }

                  // if primitive set counts changed, destroy GL objects
                  if (pointsInfo.length != robj.numPointGroups() ||
                  linesInfo.length != robj.numLineGroups() ||
                  trianglesInfo.length != robj.numTriangleGroups()) {
                     // release GLOs
                     for (GL3Object glo : glos) {
                        glo.release(gl);
                     }
                     glos = null;   
                  }

                  // recreate primitive VBO
                  createPrimitiveVBO(gl, robj, streaming, vbos.length-1);

                  if (glos == null) {
                     createGLOs(gl, robj);
                  } else {
                     // update primitive counts
                     for (int i=0; i<glos.length; ++i) {
                        GLOInfo gi = gloInfo[i];
                        switch (gi.primitiveType) {
                           case POINTS:
                              glos[i].start = pointsInfo[gi.primitiveGroup].offset;
                              glos[i].count = pointsInfo[gi.primitiveGroup].count;
                              glos[i].mode = GL.GL_POINTS;
                              glos[i].type = DrawType.ELEMENT;
                              break;
                           case LINES:
                              glos[i].start = linesInfo[gi.primitiveGroup].offset;
                              glos[i].count = linesInfo[gi.primitiveGroup].count;
                              glos[i].mode = GL.GL_LINES;
                              glos[i].type = DrawType.ELEMENT;
                              break;
                           case TRIANGLES:
                              glos[i].start = trianglesInfo[gi.primitiveGroup].offset;
                              glos[i].count = trianglesInfo[gi.primitiveGroup].count;
                              glos[i].mode = GL.GL_TRIANGLES;
                              glos[i].type = DrawType.ELEMENT;
                              break;
                        }
                     }
                     updated = true;
                  }

                  // mark version numbers
                  lastVersionInfo = robj.getVersionInfo();

               } // end rebuild or update
            } // end static, streaming, or size change
         } // end different version
      } // end synchronize
      
      return updated;
   }

   private void updateDynamicVBOs(GL3 gl, RenderObject robj, int updateMask, boolean replace) {
      ByteBuffer[] buffs = new ByteBuffer[vbos.length];

      int vidx = 0;
      List<VertexIndexSet> verts = robj.getVertices();
      for (VertexIndexSet v : verts) {
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

      // unmap
      for (int i=0; i<buffs.length; ++i) {
         if (buffs[i] != null) {
            vbos[i].unmapBuffer(gl);
         }
      }

   }

   public void draw(GL3 gl) {
      drawPoints(gl);
      drawLines(gl);
      drawTriangles(gl);
   }

   public boolean isValid() {
      return glos != null;
   }

   @Override
   public void init(GL3 gl) {
      // nothing, must be initialized with RenderObject
   }

   public void init(GL3 gl, RenderObject robj) {

      synchronized(robj) {
         // commit info
         if (!robj.isCommitted()) {
            robj.commit();
         }
         // stores VBO indices/offsets/strides for each attribute 
         build(gl, robj);

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
      pointsInfo = null;
      linesInfo = null;
      trianglesInfo = null;

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
      indexPutter = null;

      lastVersionInfo = null;
   }

   private void clearAndRebuild(GL3 gl, RenderObject robj) {
      clearAll(gl);
      build(gl, robj);
   }
   
   private void build(GL3 gl, RenderObject robj) {

      // create and fill VBOs
      createVBOs(gl, robj);
      createGLOs(gl, robj);

      // mark version numbers
      lastVersionInfo = robj.getVersionInfo();
   }

   private AttributeInfo[] createAttributeInfoArrays(int n) {
      AttributeInfo[] ai = new AttributeInfo[n];
      for (int i=0; i<ai.length; ++i) {
         ai[i] = new AttributeInfo();
      }
      return ai;
   }

   private void createVBOs(GL3 gl, RenderObject robj) {

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

      staticVertexSize = 0;
      staticMask = 0;
      dynamicVertexSize = 0;
      dynamicMask = 0;
      boolean streaming = robj.isStreaming();
      nVertices = robj.numVertices();

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
         positionInfo[0].count = nVertices;

         for (int i=1; i<positionInfo.length; ++i) {
            positionInfo[i].offset = (i-1)*POSITION_BYTES*nVertices;
            positionInfo[i].stride = POSITION_BYTES;
            positionInfo[i].count = nVertices;
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
         normalInfo[0].count = nVertices;

         for (int i=1; i<normalInfo.length; ++i) {
            normalInfo[i].offset = (i-1)*NORMAL_BYTES*nVertices;
            normalInfo[i].stride = NORMAL_BYTES;
            normalInfo[i].count = nVertices;
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
         colorInfo[0].count = nVertices;

         for (int i=1; i<colorInfo.length; ++i) {
            colorInfo[i].offset = (i-1)*COLOR_BYTES*nVertices;
            colorInfo[i].stride = COLOR_BYTES;
            colorInfo[i].count = nVertices;
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
         textureInfo[0].count = nVertices;

         for (int i=1; i<textureInfo.length; ++i) {
            textureInfo[i].offset = (i-1)*TEXTURE_BYTES*nVertices;
            textureInfo[i].stride = TEXTURE_BYTES;
            textureInfo[i].count = nVertices;
         }
      }

      // strides and vbo index
      int vboIdx = 0;
      if (staticVertexSize > 0) {
         vboIdx++;
      }
      if (robj.hasPositions()) {
         if (!streaming && robj.isPositionsDynamic()) {
            positionInfo[0].stride = dynamicVertexSize;
            positionInfo[0].vboIndex = vboIdx;
         } else {
            positionInfo[0].stride = staticVertexSize;
            positionInfo[0].vboIndex = 0;
         }
      }
      if (robj.hasNormals()) {
         if (!streaming && robj.isNormalsDynamic()) {
            normalInfo[0].stride = dynamicVertexSize;
            normalInfo[0].vboIndex = vboIdx;
         } else {
            normalInfo[0].stride = staticVertexSize;
            normalInfo[0].vboIndex = 0;
         }
      }
      if (robj.hasColors()) {
         if (!streaming && robj.isColorsDynamic()) {
            colorInfo[0].stride = dynamicVertexSize;
            colorInfo[0].vboIndex = vboIdx;
         } else {
            colorInfo[0].stride = staticVertexSize;
            colorInfo[0].vboIndex = 0;
         }
      }
      if (robj.hasTextureCoords()) {
         if (!streaming && robj.isTextureCoordsDynamic()) {
            textureInfo[0].stride = dynamicVertexSize;
            textureInfo[0].vboIndex = vboIdx;
         } else {
            textureInfo[0].stride = staticVertexSize;
            textureInfo[0].vboIndex = 0;
         }
      }

      // dynamic
      if (!streaming && robj.isDynamic()) {
         vboIdx++;
      }
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

      // create buffers for VBOs
      ByteBuffer[] buffs = new ByteBuffer[vboIdx];

      vboIdx = 0;
      if (staticVertexSize > 0) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(nVertices*staticVertexSize);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      // dynamic
      if (!streaming && robj.isDynamic()) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(nVertices*dynamicVertexSize);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }

      // extra attribute sets
      if (robj.numPositionSets() > 1 ) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(nVertices*(robj.numPositionSets()-1)*POSITION_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      if (robj.numNormalSets() > 1) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(nVertices*(robj.numNormalSets()-1)*NORMAL_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      if (robj.numColorSets() > 1) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(nVertices*(robj.numColorSets()-1)*COLOR_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }
      if (robj.numTextureCoordSets() > 1) {
         buffs[vboIdx] = ByteBuffer.allocateDirect(nVertices*(robj.numTextureCoordSets()-1)*TEXTURE_BYTES);
         buffs[vboIdx].order(ByteOrder.nativeOrder());
         vboIdx++;
      }

      // fill vertex buffers
      List<VertexIndexSet> verts = robj.getVertices();
      int vidx = 0;
      for (VertexIndexSet v : verts) {
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

      // create VBOs
      int nVBOs = vboIdx;
      // extra VBO for primitives
      if (robj.hasPoints() || robj.hasLines() || robj.hasTriangles()) {
         nVBOs++;
      }
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

      createPrimitiveVBO(gl, robj, streaming, vboIdx);
   }

   private void createPrimitiveVBO(GL3 gl, RenderObject robj, boolean streaming, int vboIdx) {

      indexPutter = IndexBufferPutter.createDefault(nVertices-1);

      final int INDEX_BYTES = indexPutter.bytesPerIndex();

      pointsInfo = createAttributeInfoArrays(robj.numPointGroups());
      linesInfo = createAttributeInfoArrays(robj.numLineGroups());
      trianglesInfo = createAttributeInfoArrays(robj.numTriangleGroups());

      // primitives
      int primitiveOffset = 0;
      for (int i=0; i<pointsInfo.length; ++i) {
         pointsInfo[i].offset = primitiveOffset;
         pointsInfo[i].stride = INDEX_BYTES;
         pointsInfo[i].vboIndex = vboIdx;
         pointsInfo[i].count = robj.numPoints(i);
         primitiveOffset += robj.numPoints(i)*INDEX_BYTES;
      }
      for (int i=0; i<linesInfo.length; ++i) {
         linesInfo[i].offset = primitiveOffset;
         linesInfo[i].stride = INDEX_BYTES;
         linesInfo[i].vboIndex = vboIdx;
         linesInfo[i].count = 2*robj.numLines(i);
         primitiveOffset += 2*robj.numLines(i)*INDEX_BYTES;
      }
      for (int i=0; i<trianglesInfo.length; ++i) {
         trianglesInfo[i].offset = primitiveOffset;
         trianglesInfo[i].stride = INDEX_BYTES;
         trianglesInfo[i].vboIndex = vboIdx;
         trianglesInfo[i].count = 3*robj.numTriangles(i);
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

         ByteBuffer buff = ByteBuffer.allocateDirect(size);
         buff.order(ByteOrder.nativeOrder());

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

         vbos[vboIdx] = new BufferObject(gl);
         buff.rewind();
         vbos[vboIdx].fill(gl, buff, GL.GL_ELEMENT_ARRAY_BUFFER,
            getBufferUsage(false, streaming));
      }
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

   private void createGLOs(GL3 gl, RenderObject robj) {

      // number of GL objects:
      int nGLOs = (1+robj.numPointGroups()+robj.numLineGroups()+robj.numTriangleGroups());
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
      gloForPrimitives(gl, robj);

   }

   private void gloForPrimitives(GL3 gl, RenderObject robj) {

      // just vertices
      int gidx = 0;
      gidx = gloForTextures(gl, robj, -1, VERTICES, gidx);

      for (int i=0; i<pointsInfo.length; ++i) {
         gidx = gloForTextures(gl, robj, i, POINTS, gidx);   
      }
      for (int i=0; i<linesInfo.length; ++i) {
         gidx = gloForTextures(gl, robj, i, LINES, gidx);   
      }
      for (int i=0; i<trianglesInfo.length; ++i) {
         gidx = gloForTextures(gl, robj, i, TRIANGLES, gidx);   
      }

   }

   private int gloForTextures(GL3 gl, RenderObject robj, int oidx, int type, int gidx) {
      if (robj.hasTextureCoords()) {
         for (int i=0; i<textureInfo.length; ++i) {
            gidx = gloForColors(gl, robj, i, oidx, type, gidx);
         }
      } else {
         gidx = gloForColors(gl, robj, -1, oidx, type, gidx);
      }
      return gidx;
   }

   private int gloForColors(GL3 gl, RenderObject robj, int tidx, int oidx, int type, int gidx) {
      if (robj.hasColors()) {
         for (int i=0; i<colorInfo.length; ++i) {
            gidx = gloForNormals(gl, robj, i, tidx, oidx, type, gidx);
         }
      } else {
         gidx = gloForNormals(gl, robj, -1, tidx, oidx, type, gidx);
      }
      return gidx;
   }

   private int gloForNormals(GL3 gl, RenderObject robj, int cidx, int tidx, int oidx, int type, int gidx) {
      if (robj.hasNormals()) {
         for (int i=0; i<normalInfo.length; ++i) {
            gidx = gloForPositions(gl, robj, i, cidx, tidx, oidx, type, gidx);
         }
      } else {
         gidx = gloForPositions(gl, robj, -1, cidx, tidx, oidx, type, gidx);
      }
      return gidx;
   }

   private int gloForPositions(GL3 gl, RenderObject robj, int nidx, int cidx, int tidx, int oidx, int type, int gidx) {
      if (robj.hasPositions()) {
         for (int i=0; i<positionInfo.length; ++i) {
            glos[gidx] = buildGLO(gl, robj, i, nidx, cidx, tidx, oidx, type);
            gloInfo[gidx] = new GLOInfo(i, nidx, cidx, tidx, oidx, type);
            gidx++;
         }
      } else {
         glos[gidx] = buildGLO(gl, robj, -1, nidx, cidx, tidx, oidx, type);
         gloInfo[gidx] = new GLOInfo(-1, nidx, cidx, tidx, oidx, type);
         gidx++;
      }
      return gidx;
   }

   private GL3Object buildGLO(GL3 gl, RenderObject robj, int pidx, int nidx, int cidx, int tidx, int oidx, int type) {

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
         nattribs++;
      }

      GL3VertexAttributeArray[] attribs = new GL3VertexAttributeArray[nattribs];

      // position
      int aidx = 0;
      if (pidx >= 0) {
         AttributeInfo pinfo = positionInfo[pidx];
         BufferObject vbo = vbos[pinfo.vboIndex];
         BufferStorage bs = positionPutter.storage();

         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_POSITION, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), pinfo.offset, pinfo.stride, pinfo.count);
         aidx++;
      }

      // normal
      if (nidx >= 0) {
         AttributeInfo ninfo = normalInfo[nidx];
         BufferObject vbo = vbos[ninfo.vboIndex];
         BufferStorage bs = normalPutter.storage();
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_NORMAL, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), ninfo.offset, ninfo.stride, ninfo.count);
         aidx++;
      }

      // color
      if (cidx >= 0) {
         // bind and enable color
         AttributeInfo cinfo = colorInfo[cidx];
         BufferObject vbo = vbos[cinfo.vboIndex];
         BufferStorage bs = colorPutter.storage();
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_COLOR, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), cinfo.offset, cinfo.stride, cinfo.count);
         aidx++;
      }

      // texture
      if (tidx >= 0) {
         // bind and enable texture
         AttributeInfo tinfo = textureInfo[tidx];
         BufferObject vbo = vbos[tinfo.vboIndex];
         BufferStorage bs = texturePutter.storage();
         attribs[aidx] = new GL3VertexAttributeArray(
            vbo, GL3VertexAttribute.VERTEX_TEXTURE, GL3Util.getGLType(bs.type()), bs.size(), 
            bs.isNormalized(), tinfo.offset, tinfo.stride, tinfo.count);
         aidx++;
      }

      int mode;
      GL3ElementAttributeArray elems = null;

      // primitives, bind appropriate index buffer
      switch (type) {
         case POINTS: {
            AttributeInfo oinfo = pointsInfo[oidx];
            BufferStorage bs = indexPutter.storage();
            BufferObject ibo = vbos[oinfo.vboIndex];

            mode = GL.GL_POINTS;
            elems = new GL3ElementAttributeArray(ibo, GL3Util.getGLType(bs.type()), oinfo.offset, oinfo.stride, oinfo.count);
            break;
         }
         case LINES: {
            AttributeInfo oinfo = linesInfo[oidx];
            BufferStorage bs = indexPutter.storage();
            BufferObject ibo = vbos[oinfo.vboIndex];

            mode = GL.GL_LINES;
            elems = new GL3ElementAttributeArray(ibo, GL3Util.getGLType(bs.type()), oinfo.offset, oinfo.stride, oinfo.count);
            break;
         }
         case TRIANGLES: {
            AttributeInfo oinfo = trianglesInfo[oidx];
            BufferStorage bs = indexPutter.storage();
            BufferObject ibo = vbos[oinfo.vboIndex];

            mode = GL.GL_TRIANGLES;
            elems = new GL3ElementAttributeArray(ibo, GL3Util.getGLType(bs.type()), oinfo.offset, oinfo.stride, oinfo.count);
            break;
         }
         case VERTICES:
         default:
            // not indexed
            mode = GL.GL_POINTS;
      }

      GL3Object glo = new GL3Object(attribs, elems, mode);

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
