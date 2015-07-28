package maspack.render.GL.GL3;

import java.nio.FloatBuffer;
import java.util.List;

import javax.media.opengl.GL3;

import maspack.matrix.Plane;
import maspack.matrix.Vector3d;
import maspack.render.GL.GLClipPlane;

public class ClipPlanesUBO extends UniformBufferObject {
   
   static final String CLIP_PLANES_NAME = "ClipPlanes";
   int numClipPlanes;
   
   private static String[] createAttributes(int nClipPlanes) {
   
      String[] out = new String[nClipPlanes];
      for (int i=0; i<nClipPlanes; ++i) {
         out[i] = "clip_plane[" + i + "].plane";
      }
      return out;
   }
   
   public ClipPlanesUBO(GL3 gl, int progId, int nClipPlanes) {
      super(gl, progId, CLIP_PLANES_NAME, createAttributes(nClipPlanes), GL3.GL_DYNAMIC_DRAW);
      numClipPlanes = nClipPlanes;
   }
   
   public void updateClipPlanes(GL3 gl,  Plane[] clips) {
      float[] clipbuff = new float[getSize()/Float.BYTES];
      
      for (int i=0; i<numClipPlanes; ++i) {
         int idx = getOffset(i)/Float.BYTES;
         Vector3d normal = clips[i].getNormal();
         clipbuff[idx++] = (float)(normal.x);
         clipbuff[idx++] = (float)(normal.y);
         clipbuff[idx++] = (float)(normal.z);
         clipbuff[idx++] = (float)(clips[i].getOffset());
      }
      
      FloatBuffer data = FloatBuffer.wrap(clipbuff);
      update(gl, data);
   }
   
   /**
    * Returns the number of enabled clip distances
    * @param gl
    * @param clips
    * @return
    */
   public int updateClipPlanes(GL3 gl,  GLClipPlane[] clips) {
      
      float[] clipbuff = new float[getSize()/Float.BYTES];
      int nclips = 0;
      
      for (GLClipPlane cp : clips) {
         if (cp.isClippingEnabled()) {
            int idx = getOffset(nclips)/Float.BYTES;
            cp.getClipPlaneValues (clipbuff, idx, false);
            nclips++;
            if (nclips >= numClipPlanes) {
               break;
            }

            if (cp.isSlicingEnabled()) {
               idx = getOffset(nclips)/Float.BYTES;
               cp.getClipPlaneValues (clipbuff, idx, true);
               nclips++;
               if (nclips >= numClipPlanes) {
                  break;
               }
            }
         }
      }
      
      FloatBuffer data = FloatBuffer.wrap(clipbuff);
      update(gl, data);
      
      return nclips;
   }
   
   /**
    * Returns the number of enabled clip distances
    * @param gl
    * @param clips
    * @return
    */
   public int updateClipPlanes(GL3 gl,  List<GLClipPlane> clips) {
      
      float[] clipbuff = new float[getSize()/Float.BYTES];
      int nclips = 0;
      
      for (GLClipPlane cp : clips) {
         if (cp.isClippingEnabled()) {
            int idx = getOffset(nclips)/Float.BYTES;
            cp.getClipPlaneValues (clipbuff, idx, false);
            nclips++;
            if (nclips >= numClipPlanes) {
               break;
            }

            if (cp.isSlicingEnabled()) {
               idx = getOffset(nclips)/Float.BYTES;
               cp.getClipPlaneValues (clipbuff, idx, true);
               nclips++;
               if (nclips >= numClipPlanes) {
                  break;
               }
            }
         }
      }
      
      FloatBuffer data = FloatBuffer.wrap(clipbuff);
      update(gl, data);
      
      return nclips;
   }
      

}
