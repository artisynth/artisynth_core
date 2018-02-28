package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL3;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.Matrix3d;
import maspack.matrix.Quaternion;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderInstances;
import maspack.render.RenderInstances.InstanceTransformType;
import maspack.render.RenderInstances.RenderInstanceConsumer;
import maspack.render.RenderInstances.RenderInstancesVersion;
import maspack.util.BufferUtilities;

/**
 * Allows drawing of instances with point, frame, affine transforms
 */
//=====================================================================================================================
// VBO layouts, ? if exists
// -- global scale (if exists)
// -- transform, scale, color interleaved, separated into static/dynamic buffer, 
//    points followed by frames followed by colors
//
//  static           dynamic
//  // global
//  [scale?]         [scale?]
//  // points
//  [pos[i[p00]] ]   [pos[i[p00]] ] 
//  [sca[i[p00]]?]   [sca[i[p00]]?] 
//  [clr[i[p00][?]   [clr[i[p00]]?]                    
//      ...
//  // frames
//  [pos[i[f00]] ]   [pos[i[f00]] ]
//  [rot[i[f00]] ]   [rot[i[f00]] ]
//  [sca[i[f00]]?]   [sca[i[f00]]?] 
//  [clr[i[f00][?]   [clr[i[f00]]?]
//      ...
//  // affines
//  [aff[i[a00]] ]   [aff[i[a00]] ]
//  [nrm[i[a00]] ]   [nrm[i[a00]] ]
//  [sca[i[a00]]?]   [sca[i[a00]]?] 
//  [clr[i[a00][?]   [clr[i[a00]]?]

//=====================================================================================================================
public class GL3SharedRenderInstances extends GL3ResourceBase {

   GL3VertexAttributeInfo posAttr;
   GL3VertexAttributeInfo rotAttr;
   GL3VertexAttributeInfo affAttr;
   GL3VertexAttributeInfo nrmAttr;
   GL3VertexAttributeInfo scaAttr;
   GL3VertexAttributeInfo clrAttr;

   protected enum AttributeType {
      POSITION,
      ORIENTATION,
      AFFINE,
      NORMAL,
      SCALE,
      COLOR,
      GLOBAL_SCALE
   }

   protected static class AttributeInfo {
      AttributeType attrib;
      int vboIndex;
      int offset;       // in bytes
      int stride;       // in bytes
      int count;        // number of items
      GL3AttributeStorage storage;  // item storage per attribute
      GL3VertexAttributeInfo ainfo; // attribute's program info
   }

   protected static final int STATIC_VBO_IDX = 0;
   protected static final int DYNAMIC_VBO_IDX = 1;

   protected static final int POINTS_FLAG = 0x01;
   protected static final int SCALES_FLAG = 0x02;
   protected static final int COLORS_FLAG = 0x04;
   protected static final int FRAMES_FLAG = 0x08;
   protected static final int AFFINES_FLAG = 0x10;

   protected static int POINT_POSITION_IDX = 0;
   protected static int POINT_SCALE_IDX = 1;
   protected static int POINT_COLOR_IDX = 2;
   protected static int POINT_INFO_SIZE = 3;

   protected static int FRAME_POSITION_IDX = 0;
   protected static int FRAME_ORIENTATION_IDX = 1;
   protected static int FRAME_SCALE_IDX = 2;
   protected static int FRAME_COLOR_IDX = 3;
   protected static int FRAME_INFO_SIZE = 4;

   protected static int AFFINE_AFFINE_IDX = 0;
   protected static int AFFINE_NORMAL_IDX = 1;
   protected static int AFFINE_SCALE_IDX = 2;
   protected static int AFFINE_COLOR_IDX = 3;
   protected static int AFFINE_INFO_SIZE = 4;

   protected static int GLOBAL_SCALE_IDX = 0;
   protected static int GLOBAL_INFO_SIZE = 1;

   protected AttributeInfo[] pointInfo;
   protected AttributeInfo[] frameInfo;
   protected AttributeInfo[] affineInfo;
   protected AttributeInfo[] globalInfo;

   // track version numbers so we can detect what has changed since last use
   RenderInstancesVersion lastVersionInfo;
   
   protected int dynamicMask;
   protected boolean globalScale; // whether or not we are using a global scale
   
   protected boolean streaming; 
   protected int staticBufferSize;
   protected int dynamicBufferSize;
   protected int bindVersion;
   protected VertexBufferObject[] vbos;

   protected GL3SharedRenderInstances(RenderInstances r,
      GL3VertexAttributeInfo posAttribute, GL3VertexAttributeInfo rotAttribute,
      GL3VertexAttributeInfo affAttribute, GL3VertexAttributeInfo nrmAttribute,
      GL3VertexAttributeInfo scaAttribute, GL3VertexAttributeInfo clrAttribute,
      VertexBufferObject staticVBO, VertexBufferObject dynamicVBO) {

      this.posAttr = posAttribute;
      this.rotAttr = rotAttribute;
      this.affAttr = affAttribute;
      this.nrmAttr = nrmAttribute;
      this.scaAttr = scaAttribute;
      this.clrAttr = clrAttribute;

      vbos = new VertexBufferObject[]{staticVBO, dynamicVBO};

      dynamicMask = 0;
      streaming = false;

      lastVersionInfo = null;
      pointInfo = null;
      frameInfo = null;
      affineInfo = null;
      globalInfo = null;
   }

   // maybe update VBOs
   /**
    * Potentially update the internal VBOs
    * @param gl context handle
    * @param rinst instance storage
    * @return true if updated, false if nothing changed
    */
   public boolean maybeUpdate(GL3 gl, RenderInstances rinst) {

      boolean updated = false;
      rinst.readLock (); {
         RenderInstancesVersion rv = rinst.getVersionInfo ();
         updated = maybeUpdateInstances(gl, rinst, rv);
         if (updated) {
            lastVersionInfo = rv;
         }
      } rinst.readUnlock ();
      return updated;
   }

   protected boolean updateDynamicMasks(GL3 gl, RenderInstancesVersion rv) {

      if (lastVersionInfo == null) {
         return true;
      }
      
      // always rebuild if streaming vertices
      if (streaming) {
         return true;
      }
      
      // if instances have changed, we need to rebuild
      if (rv.getInstancesVersion() != lastVersionInfo.getInstancesVersion ()) {
         // Also changes triggers global scale property re-check
         streaming = true;  // set to streaming mode since instances are changing around
         dynamicMask = 0;
         return true;
      }
      
      // check if needs rebuild and set dynamic masks
      boolean rebuild = false;      
      
      // trying to update static component
      if (rv.getPointsVersion() != lastVersionInfo.getPointsVersion()) {
         boolean pointsDynamic = ((dynamicMask & POINTS_FLAG) != 0);
         // positions are static
         if  (!pointsDynamic) {
            rebuild = true;
            dynamicMask |= POINTS_FLAG;
         }
      }
      if (rv.getFramesVersion() != lastVersionInfo.getFramesVersion()) {
         boolean framesDynamic = ((dynamicMask & FRAMES_FLAG) != 0);
         // positions are static
         if  (!framesDynamic) {
            rebuild = true;
            dynamicMask |= FRAMES_FLAG;
         }
      }
      if (rv.getAffinesVersion() != lastVersionInfo.getAffinesVersion()) {
         boolean affinesDynamic = ((dynamicMask & AFFINES_FLAG) != 0);
         // positions are static
         if  (!affinesDynamic) {
            rebuild = true;
            dynamicMask |= AFFINES_FLAG;
         }
      }
      if (rv.getColorsVersion() != lastVersionInfo.getColorsVersion()) {
         boolean colorsDynamic = ((dynamicMask & COLORS_FLAG) != 0);
         if (!colorsDynamic) {
            rebuild = true;
            dynamicMask |= COLORS_FLAG;
         }
      }
      if (rv.getScalesVersion() != lastVersionInfo.getScalesVersion()) {
         
         // check if scale is global
         boolean scalesDynamic = ((dynamicMask & SCALES_FLAG) != 0);
         if (!scalesDynamic) {
            rebuild = true;
            dynamicMask |= SCALES_FLAG;
         }
         
      }
      
      return rebuild;
   }

   protected boolean maybeUpdateInstances(GL3 gl, RenderInstances rinst, RenderInstancesVersion rv) {

      if (lastVersionInfo != null && rv.getVersion () == lastVersionInfo.getVersion ()) {
         return false;
      }

      if (updateDynamicMasks(gl, rv)) {
         clearInstances(gl);
         buildInstances(gl, rinst);
         incrementBindVersion ();
         return true;
      } 

      // check what needs to be updated and do some update stuff
      
      // check if we should update all dynamic, or update part dynamic
      int updateFlag = 0;
      if (rv.getPointsVersion() != lastVersionInfo.getPointsVersion()) {
         updateFlag |= POINTS_FLAG;
      }
      
      if (rv.getFramesVersion() != lastVersionInfo.getFramesVersion()) {
         updateFlag |= FRAMES_FLAG;
      }
      
      if (rv.getAffinesVersion() != lastVersionInfo.getAffinesVersion()) {
         updateFlag |= AFFINES_FLAG;
      }
      
      if (rv.getColorsVersion() != lastVersionInfo.getColorsVersion()) {
         updateFlag |= COLORS_FLAG;
      }
      
      if (rv.getScalesVersion() != lastVersionInfo.getScalesVersion()) {
         updateFlag |= SCALES_FLAG;
      }
      
      // if at least one dynamic component needs to be updated, do so here
      boolean update = (updateFlag != 0);
      if (update) {
         updateDynamicInstances(gl, rinst, updateFlag, updateFlag == dynamicMask);
      }
      
      return update;
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

   private static class MyRenderInstanceConsumer implements RenderInstanceConsumer {

      ByteBuffer[] buffs;

      AttributeInfo pointPosInfo;
      AttributeInfo pointScaleInfo;
      AttributeInfo pointColorInfo;

      AttributeInfo framePosInfo;
      AttributeInfo frameOrientInfo;
      AttributeInfo frameScaleInfo;
      AttributeInfo frameColorInfo;

      AttributeInfo affineAffineInfo;
      AttributeInfo affineNormalInfo;
      AttributeInfo affineScaleInfo;
      AttributeInfo affineColorInfo;

      public MyRenderInstanceConsumer(ByteBuffer[] buffs, 
         int populateMask,
         AttributeInfo[] pointInfo, AttributeInfo[] frameInfo, 
         AttributeInfo[] affineInfo) {

         this.buffs = buffs;

         if ((populateMask & POINTS_FLAG) != 0) {
            pointPosInfo = pointInfo[POINT_POSITION_IDX];
         } else {
            pointPosInfo = null;
         }

         if ((populateMask & FRAMES_FLAG) != 0) {
            framePosInfo = frameInfo[FRAME_POSITION_IDX];
            frameOrientInfo = frameInfo[FRAME_ORIENTATION_IDX];
         } else {
            framePosInfo = null;
            frameOrientInfo = null;
         }

         if ((populateMask & AFFINES_FLAG) != 0) {
            affineAffineInfo = affineInfo[AFFINE_AFFINE_IDX];
            affineNormalInfo = affineInfo[AFFINE_NORMAL_IDX];
         } else {
            affineAffineInfo = null;
            affineNormalInfo = null;
         }
         
         if ((populateMask & SCALES_FLAG) != 0) {
            pointScaleInfo = pointInfo[POINT_SCALE_IDX];
            frameScaleInfo = frameInfo[FRAME_SCALE_IDX];
            affineScaleInfo = affineInfo[AFFINE_SCALE_IDX];
         } else {
            pointScaleInfo = null;
            frameScaleInfo = null;
            affineScaleInfo = null;
         }

         if ((populateMask & COLORS_FLAG) != 0) {
            pointColorInfo = pointInfo[POINT_COLOR_IDX];    
            frameColorInfo = frameInfo[FRAME_COLOR_IDX];
            affineColorInfo = affineInfo[AFFINE_COLOR_IDX];
         } else {
            pointColorInfo = null;
            frameColorInfo = null;
            affineColorInfo = null;
         }
      }

      @Override
      public void point(int pidx, float[] pos, Double scale, byte[] color) {

         if (pos != null && pointPosInfo != null) {
            ByteBuffer buff = buffs[pointPosInfo.vboIndex];
            int off = pointPosInfo.offset + pidx*pointPosInfo.stride;
            buff.position(off);
            buff.putFloat(pos[0]);
            buff.putFloat(pos[1]);
            buff.putFloat(pos[2]);
         }

         if (scale != null && pointScaleInfo != null) {
            ByteBuffer buff = buffs[pointScaleInfo.vboIndex];
            int off = pointScaleInfo.offset + pidx*pointScaleInfo.stride;
            buff.position(off);
            buff.putFloat(scale.floatValue());
         }

         if (color != null && pointColorInfo != null) {
            ByteBuffer buff = buffs[pointColorInfo.vboIndex];
            int off = pointColorInfo.offset + pidx*pointColorInfo.stride;
            buff.position(off);
            buff.put(color, 0, 4);
         }
      }

      @Override
      public void frame(int fidx, RigidTransform3d trans, Double scale, byte[] color) {
         if (trans != null) {
            if (framePosInfo != null) {
               ByteBuffer buff = buffs[framePosInfo.vboIndex];
               int off = framePosInfo.offset + fidx*framePosInfo.stride;
               buff.position(off);
               buff.putFloat((float)(trans.p.x));
               buff.putFloat((float)(trans.p.y));
               buff.putFloat((float)(trans.p.z));
            }

            if (frameOrientInfo != null) {
               ByteBuffer buff = buffs[frameOrientInfo.vboIndex];
               int off = frameOrientInfo.offset + fidx*frameOrientInfo.stride;
               buff.position(off);
               Quaternion q = new Quaternion(trans.R.getAxisAngle());
               buff.putFloat((float)(q.s));
               buff.putFloat((float)(q.u.x));
               buff.putFloat((float)(q.u.y));
               buff.putFloat((float)(q.u.z));
            }
         }

         if (scale != null && frameScaleInfo != null) {
            ByteBuffer buff = buffs[frameScaleInfo.vboIndex];
            int off = frameScaleInfo.offset + fidx*frameScaleInfo.stride;
            buff.position(off);
            buff.putFloat(scale.floatValue());
         }

         if (color != null && frameColorInfo != null) {
            ByteBuffer buff = buffs[frameColorInfo.vboIndex];
            int off = frameColorInfo.offset + fidx*frameColorInfo.stride;
            buff.position(off);
            buff.put(color, 0, 4);
         }

      }

      @Override
      public void affine(int aidx, AffineTransform3d trans, Double scale, byte[] color) {
         if (trans != null) {
            if (affineAffineInfo != null) {
               ByteBuffer buff = buffs[affineAffineInfo.vboIndex];
               if (buff == null) {
                  System.err.println("huh?");
               }
               
               int off = affineAffineInfo.offset + aidx*affineAffineInfo.stride;
               buff.position(off);
               buff.putFloat((float)(trans.A.m00));
               buff.putFloat((float)(trans.A.m10));
               buff.putFloat((float)(trans.A.m20));
               buff.putFloat(0f);
               
               buff.putFloat((float)(trans.A.m01));
               buff.putFloat((float)(trans.A.m11));
               buff.putFloat((float)(trans.A.m21));
               buff.putFloat(0f);
               
               buff.putFloat((float)(trans.A.m02));
               buff.putFloat((float)(trans.A.m12));
               buff.putFloat((float)(trans.A.m22));
               buff.putFloat(0f);
               
               buff.putFloat((float)(trans.p.x));
               buff.putFloat((float)(trans.p.y));
               buff.putFloat((float)(trans.p.z));
               buff.putFloat(1f);
            }

            if (affineNormalInfo != null) {
               ByteBuffer buff = buffs[affineNormalInfo.vboIndex];
               int off = affineNormalInfo.offset + aidx*affineNormalInfo.stride;

               // inverse transpose
               Matrix3d N = new Matrix3d();
               N.invert(trans.A);
               N.transpose();

               //XXX don't flip normals?
               //               if (N.determinant() < 0) {
               //                  N.negate();
               //               }

               buff.position(off);
               buff.putFloat((float)(N.m00));
               buff.putFloat((float)(N.m10));
               buff.putFloat((float)(N.m20));
               buff.putFloat(0f);

               buff.putFloat((float)(N.m01));
               buff.putFloat((float)(N.m11));
               buff.putFloat((float)(N.m21));
               buff.putFloat(0f);

               buff.putFloat((float)(N.m02));
               buff.putFloat((float)(N.m12));
               buff.putFloat((float)(N.m22));
               buff.putFloat(0f);

               buff.putFloat(0f);
               buff.putFloat(0f);
               buff.putFloat(0f);
               buff.putFloat(1f);
            }
         }

         if (scale != null && affineScaleInfo != null) {
            ByteBuffer buff = buffs[affineScaleInfo.vboIndex];
            int off = affineScaleInfo.offset + aidx*affineScaleInfo.stride;
            buff.position(off);
            buff.putFloat(scale.floatValue());
         }

         if (color != null && affineColorInfo != null) {
            ByteBuffer buff = buffs[affineColorInfo.vboIndex];
            int off = affineColorInfo.offset + aidx*affineColorInfo.stride;
            if (buff == null) {
               System.err.println("huh?");
            }
            buff.position(off);
            buff.put(color, 0, 4);
         }
      }

   }

   protected void updateDynamicInstances(GL3 gl,
      RenderInstances rinst, int updateMask, boolean replace) {

      ByteBuffer buff = null;
      if (replace) {
         buff = BufferUtilities.newNativeByteBuffer (vbos[DYNAMIC_VBO_IDX].getSize ());
      } else {
         buff = vbos[DYNAMIC_VBO_IDX].mapBuffer (gl, GL3.GL_WRITE_ONLY);
      }

      // global scale
      if (((updateMask & SCALES_FLAG ) != 0) && globalScale) {
         // determine global scale
         AttributeInfo ainfo = globalInfo[GLOBAL_SCALE_IDX];
         if (ainfo != null) {
            Double gscale = 1.0;
            if (rinst.hasScales()) {
               gscale = rinst.getInstanceScale(0);
            }
            buff.position(ainfo.offset);
            buff.putFloat(gscale.floatValue());
         }
      }

      ByteBuffer[] buffs = new ByteBuffer[2];
      buffs[DYNAMIC_VBO_IDX] = buff;
      MyRenderInstanceConsumer consumer = new MyRenderInstanceConsumer(buffs, 
         updateMask, pointInfo, frameInfo, affineInfo);
      rinst.forEachInstance(consumer);

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

   protected void clearInstances(GL3 gl) {
      pointInfo = null;
      frameInfo = null;
      affineInfo = null;
      globalInfo = null;

      staticBufferSize = 0;
      dynamicBufferSize = 0;
      
      lastVersionInfo = null;
   }

   protected void clearAll(GL3 gl) {
      clearInstances(gl);
   }

   static Double detectGlobalScale(RenderInstances rinst) {

      Double out = null;
      
      // no scales, assume global scale of one
      if (!rinst.hasScales()) {
         return 1.0;
      }

      if (rinst.numInstances() > 0) {
         // go through all instances
         int[] inst = rinst.getInstances();
         int soffset = rinst.getInstanceScaleOffset();
         int stride = rinst.getInstanceStride();

         int scaleIdx = inst[soffset];
         for (int i=soffset+stride; i<inst.length; i+=stride) {
            // check if different scale
            if (inst[i] != scaleIdx) {
               return null; 
            }
         }

         // only single scale factor used
         out = rinst.getScale(scaleIdx);
      }
      return out;

   }

   protected void buildInstanceInfo(GL3 gl, RenderInstances rinst) {
      // buffer manipulators
      final int FLOAT_BYTES = 4;
      final int POSITION_BYTES = 3*FLOAT_BYTES;
      final int ORIENTATION_BYTES = 4*FLOAT_BYTES;
      final int AFFINE_BYTES = 16*FLOAT_BYTES;
      final int NORMAL_BYTES = 16*FLOAT_BYTES;
      final int SCALE_BYTES = FLOAT_BYTES;     
      final int COLOR_BYTES = 4;

      // build up info
      pointInfo = new AttributeInfo[POINT_INFO_SIZE];
      frameInfo = new AttributeInfo[FRAME_INFO_SIZE];
      affineInfo = new AttributeInfo[AFFINE_INFO_SIZE];
      globalInfo = new AttributeInfo[GLOBAL_INFO_SIZE];

      boolean pointsDynamic = !streaming && ((dynamicMask & POINTS_FLAG) != 0);
      boolean framesDynamic = !streaming && ((dynamicMask & FRAMES_FLAG) != 0);
      boolean affinesDynamic = !streaming && ((dynamicMask & AFFINES_FLAG) != 0);
      boolean colorsDynamic = !streaming && ((dynamicMask & COLORS_FLAG) != 0);
      boolean scalesDynamic = !streaming && ((dynamicMask & SCALES_FLAG) != 0); 

      int staticOffset = 0;
      int dynamicOffset = 0;

      // global
      Double gscale = detectGlobalScale(rinst);
      boolean hasGlobalScale = (gscale != null);
      
      if (hasGlobalScale) {
         AttributeInfo info = new AttributeInfo();
         info.attrib = AttributeType.GLOBAL_SCALE;
         info.count = 1;
         info.stride = SCALE_BYTES;
         info.storage = GL3AttributeStorage.FLOAT;
         info.ainfo = scaAttr;
         if (scalesDynamic) {
            info.offset = dynamicOffset;
            info.vboIndex = DYNAMIC_VBO_IDX;
            dynamicOffset += SCALE_BYTES;
         } else {
            info.offset = staticOffset;
            info.vboIndex = STATIC_VBO_IDX;
            staticOffset += SCALE_BYTES;
         }
         globalInfo[GLOBAL_SCALE_IDX] = info;
         globalScale = true;
      }
      
      boolean hasScales = (!hasGlobalScale && rinst.hasScales());
      boolean hasColors = rinst.hasColors();

      // points
      if (rinst.hasPointInstances()) {
         int npoints = rinst.numPointInstances();
         int dynamicPointSize = 0;
         int staticPointSize = 0;

         // determine sizes
         if (pointsDynamic) {
            dynamicPointSize += POSITION_BYTES;
         } else {
            staticPointSize += POSITION_BYTES;
         }
         if (hasScales) {
            if (scalesDynamic) {
               dynamicPointSize += SCALE_BYTES;
            } else {
               staticPointSize += SCALE_BYTES;
            }
         }
         if (hasColors) {
            if (colorsDynamic) {
               dynamicPointSize += COLOR_BYTES;
            } else {
               staticPointSize += COLOR_BYTES;
            }
         }
         AttributeInfo info = new AttributeInfo();
         info.attrib = AttributeType.POSITION;
         info.count = npoints;
         info.storage = GL3AttributeStorage.FLOAT_3;
         info.ainfo = posAttr;
         if (pointsDynamic) {
            info.vboIndex = DYNAMIC_VBO_IDX;
            info.offset = dynamicOffset;
            info.stride = dynamicPointSize;
            dynamicOffset += POSITION_BYTES;
         } else {
            info.vboIndex = STATIC_VBO_IDX;
            info.offset = staticOffset;
            info.stride = staticPointSize;
            staticOffset += POSITION_BYTES;
         }
         pointInfo[POINT_POSITION_IDX] = info;

         if (hasScales) {
            info = new AttributeInfo();
            info.attrib = AttributeType.SCALE;
            info.count = npoints;
            info.storage = GL3AttributeStorage.FLOAT;
            info.ainfo = scaAttr;
            if (scalesDynamic) {
               info.vboIndex = DYNAMIC_VBO_IDX;
               info.offset = dynamicOffset;
               info.stride = dynamicPointSize;
               dynamicOffset += SCALE_BYTES;
            } else {
               info.vboIndex = STATIC_VBO_IDX;
               info.offset = staticOffset;
               info.stride = staticPointSize;
               staticOffset += SCALE_BYTES;
            }
            pointInfo[POINT_SCALE_IDX] = info;
         }
         
         if (hasColors) {
            info = new AttributeInfo();
            info.attrib = AttributeType.COLOR;
            info.count = npoints;
            info.storage = GL3AttributeStorage.UBYTE_N_4;
            info.ainfo = clrAttr;
            if (colorsDynamic) {
               info.vboIndex = DYNAMIC_VBO_IDX;
               info.offset = dynamicOffset;
               info.stride = dynamicPointSize;
               dynamicOffset += COLOR_BYTES;
            } else {
               info.vboIndex = STATIC_VBO_IDX;
               info.offset = staticOffset;
               info.stride = staticPointSize;
               staticOffset += COLOR_BYTES;
            }
            pointInfo[POINT_COLOR_IDX] = info;
         }
         
         staticOffset += (npoints-1)*staticPointSize;
         dynamicOffset += (npoints-1)*dynamicPointSize;
      }
      

      // frames
      if (rinst.hasFrameInstances()) {
         int nframes = rinst.numFrameInstances();
         int dynamicFrameSize = 0;
         int staticFrameSize = 0;

         // determine sizes
         if (framesDynamic) {
            dynamicFrameSize += (POSITION_BYTES + ORIENTATION_BYTES);
         } else {
            staticFrameSize += (POSITION_BYTES + ORIENTATION_BYTES);
         }

         if (hasScales) {
            if (scalesDynamic) {
               dynamicFrameSize += SCALE_BYTES;
            } else {
               staticFrameSize += SCALE_BYTES;
            }
         }
         if (hasColors) {
            if (colorsDynamic) {
               dynamicFrameSize += COLOR_BYTES;
            } else {
               staticFrameSize += COLOR_BYTES;
            }
         }
         // position
         AttributeInfo info = new AttributeInfo();
         info.attrib = AttributeType.POSITION;
         info.count = nframes;
         info.storage = GL3AttributeStorage.FLOAT_3;
         info.ainfo = posAttr;
         if (framesDynamic) {
            info.vboIndex = DYNAMIC_VBO_IDX;
            info.offset = dynamicOffset;
            info.stride = dynamicFrameSize;
            dynamicOffset += POSITION_BYTES;
         } else {
            info.vboIndex = STATIC_VBO_IDX;
            info.offset = staticOffset;
            info.stride = staticFrameSize;
            staticOffset += POSITION_BYTES;
         }
         frameInfo[FRAME_POSITION_IDX] = info;

         // orientation
         info = new AttributeInfo();
         info.attrib = AttributeType.ORIENTATION;
         info.count = nframes;
         info.storage = GL3AttributeStorage.FLOAT_4;
         info.ainfo = rotAttr;
         if (framesDynamic) {
            info.vboIndex = DYNAMIC_VBO_IDX;
            info.offset = dynamicOffset;
            info.stride = dynamicFrameSize;
            dynamicOffset += ORIENTATION_BYTES;
         } else {
            info.vboIndex = STATIC_VBO_IDX;
            info.offset = staticOffset;
            info.stride = staticFrameSize;
            staticOffset += ORIENTATION_BYTES;
         }
         frameInfo[FRAME_ORIENTATION_IDX] = info;
         
         if (hasScales) {
            info = new AttributeInfo();
            info.attrib = AttributeType.SCALE;
            info.count = nframes;
            info.storage = GL3AttributeStorage.FLOAT;
            info.ainfo = scaAttr;
            if (scalesDynamic) {
               info.vboIndex = DYNAMIC_VBO_IDX;
               info.offset = dynamicOffset;
               info.stride = dynamicFrameSize;
               dynamicOffset += SCALE_BYTES;
            } else {
               info.vboIndex = STATIC_VBO_IDX;
               info.offset = staticOffset;
               info.stride = staticFrameSize;
               staticOffset += SCALE_BYTES;
            }
            frameInfo[FRAME_SCALE_IDX] = info;
         }
         
         if (hasColors) {
            info = new AttributeInfo();
            info.attrib = AttributeType.COLOR;
            info.count = nframes;
            info.storage = GL3AttributeStorage.UBYTE_N_4;
            info.ainfo = clrAttr;
            if (colorsDynamic) {
               info.vboIndex = DYNAMIC_VBO_IDX;
               info.offset = dynamicOffset;
               info.stride = dynamicFrameSize;
               dynamicOffset += COLOR_BYTES;
            } else {
               info.vboIndex = STATIC_VBO_IDX;
               info.offset = staticOffset;
               info.stride = staticFrameSize;
               staticOffset += COLOR_BYTES;
            }
            frameInfo[FRAME_COLOR_IDX] = info;
         }
         
         staticOffset += (nframes-1)*staticFrameSize;
         dynamicOffset += (nframes-1)*dynamicFrameSize;
      }
      
      // affines
      if (rinst.hasAffineInstances()) {
         int naffines = rinst.numAffineInstances();
         int dynamicAffineSize = 0;
         int staticAffineSize = 0;

         // determine sizes
         if (affinesDynamic) {
            dynamicAffineSize += (AFFINE_BYTES + NORMAL_BYTES);
         } else {
            staticAffineSize += (AFFINE_BYTES + NORMAL_BYTES);
         }

         if (hasScales) {
            if (scalesDynamic) {
               dynamicAffineSize += SCALE_BYTES;
            } else {
               staticAffineSize += SCALE_BYTES;
            }
         }
         if (hasColors) {
            if (colorsDynamic) {
               dynamicAffineSize += COLOR_BYTES;
            } else {
               staticAffineSize += COLOR_BYTES;
            }
         }
         
         // affine
         AttributeInfo info = new AttributeInfo();
         info.attrib = AttributeType.AFFINE;
         info.count = naffines;
         info.storage = GL3AttributeStorage.FLOAT_4x4;
         info.ainfo = affAttr;
         if (affinesDynamic) {
            info.vboIndex = DYNAMIC_VBO_IDX;
            info.offset = dynamicOffset;
            info.stride = dynamicAffineSize;
            dynamicOffset += AFFINE_BYTES;
         } else {
            info.vboIndex = STATIC_VBO_IDX;
            info.offset = staticOffset;
            info.stride = staticAffineSize;
            staticOffset += AFFINE_BYTES;
         }
         affineInfo[AFFINE_AFFINE_IDX] = info;

         // normal
         info = new AttributeInfo();
         info.attrib = AttributeType.NORMAL;
         info.count = naffines;
         info.storage = GL3AttributeStorage.FLOAT_4x4;
         info.ainfo = nrmAttr;
         if (affinesDynamic) {
            info.vboIndex = DYNAMIC_VBO_IDX;
            info.offset = dynamicOffset;
            info.stride = dynamicAffineSize;
            dynamicOffset += NORMAL_BYTES;
         } else {
            info.vboIndex = STATIC_VBO_IDX;
            info.offset = staticOffset;
            info.stride = staticAffineSize;
            staticOffset += NORMAL_BYTES;
         }
         affineInfo[AFFINE_NORMAL_IDX] = info;
         
         if (hasScales) {
            info = new AttributeInfo();
            info.attrib = AttributeType.SCALE;
            info.count = naffines;
            info.storage = GL3AttributeStorage.FLOAT;
            info.ainfo = scaAttr;
            if (scalesDynamic) {
               info.vboIndex = DYNAMIC_VBO_IDX;
               info.offset = dynamicOffset;
               info.stride = dynamicAffineSize;
               dynamicOffset += SCALE_BYTES;
            } else {
               info.vboIndex = STATIC_VBO_IDX;
               info.offset = staticOffset;
               info.stride = staticAffineSize;
               staticOffset += SCALE_BYTES;
            }
            affineInfo[AFFINE_SCALE_IDX] = info;
         }
         
         if (hasColors) {
            info = new AttributeInfo();
            info.attrib = AttributeType.COLOR;
            info.count = naffines;
            info.storage = GL3AttributeStorage.UBYTE_N_4;
            info.ainfo = clrAttr;
            if (colorsDynamic) {
               info.vboIndex = DYNAMIC_VBO_IDX;
               info.offset = dynamicOffset;
               info.stride = dynamicAffineSize;
               dynamicOffset += COLOR_BYTES;
            } else {
               info.vboIndex = STATIC_VBO_IDX;
               info.offset = staticOffset;
               info.stride = staticAffineSize;
               staticOffset += COLOR_BYTES;
            }
            affineInfo[AFFINE_COLOR_IDX] = info;
         }
         
         staticOffset += (naffines-1)*staticAffineSize;
         dynamicOffset += (naffines-1)*dynamicAffineSize;
      }
      
      staticBufferSize = staticOffset;
      dynamicBufferSize = dynamicOffset;
   }

   protected void buildInstances(GL3 gl, RenderInstances rinst) {

      buildInstanceInfo(gl, rinst);

      // create and fill VBOs
      createInstanceVBOs(gl, rinst);

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

   private void createInstanceVBOs(GL3 gl, RenderInstances rinst) {

      // create buffers for VBOs
      ByteBuffer[] buffs = new ByteBuffer[2];

      if (staticBufferSize > 0) {
         buffs[STATIC_VBO_IDX] = BufferUtilities.newNativeByteBuffer(staticBufferSize);
      }
      // dynamic
      if (dynamicBufferSize > 0) {
         buffs[DYNAMIC_VBO_IDX] = BufferUtilities.newNativeByteBuffer(dynamicBufferSize);
      }

      // fill vertex buffers
      // maybe fill in global scale factor
      {
         // determine global scale
         AttributeInfo ainfo = globalInfo[GLOBAL_SCALE_IDX];
         if (ainfo != null) {
            Double gscale = 1.0;
            if (rinst.hasScales()) {
               gscale = rinst.getInstanceScale(0);
            }
            ByteBuffer buff = buffs[ainfo.vboIndex];
            buff.position(ainfo.offset);
            buff.putFloat(gscale.floatValue());
         }
      }
      
      MyRenderInstanceConsumer consumer = new MyRenderInstanceConsumer(buffs, 
         -1, pointInfo, frameInfo, affineInfo);
      rinst.forEachInstance(consumer);
      
      // vertex buffer object
      gl.glBindVertexArray (0); // unbind any existing VAOs
      if (staticBufferSize > 0) {
         buffs[STATIC_VBO_IDX].flip();
         vbos[STATIC_VBO_IDX].fill(gl, buffs[STATIC_VBO_IDX],
            getBufferUsage(false, streaming));
         BufferUtilities.freeDirectBuffer (buffs[STATIC_VBO_IDX]);
      }
      // dynamic
      if (dynamicBufferSize > 0) {
         buffs[DYNAMIC_VBO_IDX].flip();
         vbos[DYNAMIC_VBO_IDX].fill(gl, buffs[DYNAMIC_VBO_IDX],
            getBufferUsage(true, streaming));
         BufferUtilities.freeDirectBuffer (buffs[DYNAMIC_VBO_IDX]);
      }
      
   }
   
   public int numPoints() {
      AttributeInfo ainfo =  pointInfo[POINT_POSITION_IDX];
      if (ainfo == null) {
         return 0;
      }
      return ainfo.count;
   }
   
   public int numFrames() {
      AttributeInfo ainfo = frameInfo[FRAME_POSITION_IDX];
      if (ainfo == null) {
         return 0;
      }
      return ainfo.count;
   }
   
   public int numAffines() {
      AttributeInfo ainfo = affineInfo[AFFINE_AFFINE_IDX];
      if (ainfo == null) {
         return 0;
      }
      return ainfo.count;
   }

   public void bindInstanceAttributes(GL3 gl, InstanceTransformType type) {

      int ninstances = 0;
      AttributeInfo ainfo[] = null;
      switch (type) {
         case AFFINE:
            ainfo = affineInfo;
            ninstances = numAffines();
            break;
         case FRAME:
            ainfo = frameInfo;
            ninstances = numFrames();
            break;
         case POINT:
            ainfo = pointInfo;
            ninstances = numPoints();            
            break;
         
      }
      
      // instance-specific info
      for (AttributeInfo info : ainfo) {
         if (info != null) {
            int loc = info.ainfo.getLocation();
            GL3AttributeStorage storage = info.storage;
            vbos[info.vboIndex].bind(gl);
            GL3Utilities.activateVertexAttribute(gl, loc, storage, 
               info.stride, info.offset, 1);
         }
      }
      
      // global
      for (AttributeInfo info : globalInfo) {
         if (info != null) {
            int loc = info.ainfo.getLocation();
            GL3AttributeStorage storage = info.storage;
            vbos[info.vboIndex].bind(gl);
            GL3Utilities.activateVertexAttribute(gl, loc, storage, 
               info.stride, info.offset, ninstances);
         }
      }
      
      
   }

   @Override
   public GL3SharedRenderInstances acquire () {
      return (GL3SharedRenderInstances)super.acquire ();
   }

   @Override
   public void dispose(GL3 gl) {
      lastVersionInfo = null;
      for (VertexBufferObject vbo : vbos) {
         vbo.dispose(gl);
      }
   }

   @Override
   public boolean isDisposed() {
      return (vbos == null);
   }

  
   public void drawInstanced(GL3 gl, GL3SharedObject glo, InstanceTransformType type) {
      int icount = 0;
      switch(type) {
         case AFFINE:
            icount = affineInfo[AFFINE_AFFINE_IDX].count;
            break;
         case FRAME:
            icount = frameInfo[FRAME_POSITION_IDX].count;
            break;
         case POINT:
            icount = pointInfo[POINT_POSITION_IDX].count;
            break;
         
      }
      glo.drawInstanced (gl, icount);
   }

   public void drawInstancedPoints(GL3 gl, GL3SharedObject glo, int count) {
      glo.drawInstanced (gl, count);
   }

   public static GL3SharedRenderInstances generate (GL3 gl, RenderInstances rinst,
      GL3VertexAttributeInfo posAttribute, GL3VertexAttributeInfo rotAttribute,
      GL3VertexAttributeInfo affAttribute, GL3VertexAttributeInfo nrmAttribute,
      GL3VertexAttributeInfo scaAttribute, GL3VertexAttributeInfo clrAttribute) {
      
      VertexBufferObject staticVBO = VertexBufferObject.generate (gl);
      VertexBufferObject dynamicVBO = VertexBufferObject.generate (gl);
      GL3SharedRenderInstances out = new GL3SharedRenderInstances (rinst, posAttribute, rotAttribute,
         affAttribute, nrmAttribute, scaAttribute, clrAttribute, staticVBO, dynamicVBO);
      out.maybeUpdate (gl, rinst);  // trigger a build
      return out;
   }

}
