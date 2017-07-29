package maspack.render.GL.GL3;

import java.nio.ByteBuffer;
import java.util.List;

import com.jogamp.opengl.GL3;

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
   
   private ClipPlanesUBO(GL3 gl, int progId, int nClipPlanes) {
      super(gl, progId, CLIP_PLANES_NAME, createAttributes(nClipPlanes), GL3.GL_DYNAMIC_DRAW);
      numClipPlanes = nClipPlanes;
   }
   
   public void updateClipPlanes(GL3 gl,  Plane[] clips) {
      
      ByteBuffer buff = getBuffer();
      
      for (int i=0; i<numClipPlanes; ++i) {
         buff.position (getByteOffset(i));
         Vector3d normal = clips[i].getNormal();
         buff.putFloat ((float)(normal.x));
         buff.putFloat ((float)(normal.y));
         buff.putFloat ((float)(normal.z));
         buff.putFloat ((float)(clips[i].getOffset()));
      }
      
      buff.flip ();
      update(gl, buff);
   }
   
   /**
    * Returns the number of enabled clip distances
    */
   public int updateClipPlanes(GL3 gl,  GLClipPlane[] clips) {

      float[] clipbuff = new float[4];
      int nclips = 0;
      ByteBuffer buff = getBuffer();
      
      for (GLClipPlane cp : clips) {
         if (cp.isClippingEnabled()) {
            buff.position (getByteOffset(nclips));
            cp.getClipPlaneValues (clipbuff, 0, false);
            for (int i=0; i<4; ++i) {
               buff.putFloat (clipbuff[i]);
            }
            nclips++;
            if (nclips >= numClipPlanes) {
               break;
            }

            if (cp.isSlicingEnabled()) {
               buff.position(getByteOffset(nclips));
               cp.getClipPlaneValues (clipbuff, 0, true);
               for (int i=0; i<4; ++i) {
                  buff.putFloat (clipbuff[i]);
               }
               nclips++;
               if (nclips >= numClipPlanes) {
                  break;
               }
            }
         }
      }

      buff.flip ();
      update(gl, buff);
      
      return nclips;
   }
   
   /**
    * Returns the number of enabled clip distances
    */
   public int updateClipPlanes(GL3 gl,  List<GLClipPlane> clips) {
      
      float[] clipbuff = new float[4];
      int nclips = 0;
      ByteBuffer buff = getBuffer();
      
      for (GLClipPlane cp : clips) {
         if (cp.isClippingEnabled()) {
            buff.position (getByteOffset(nclips));
            cp.getClipPlaneValues (clipbuff, 0, false);
            for (int i=0; i<4; ++i) {
               buff.putFloat (clipbuff[i]);
            }
            nclips++;
            if (nclips >= numClipPlanes) {
               break;
            }

            if (cp.isSlicingEnabled()) {
               buff.position(getByteOffset(nclips));
               cp.getClipPlaneValues (clipbuff, 0, true);
               for (int i=0; i<4; ++i) {
                  buff.putFloat (clipbuff[i]);
               }
               nclips++;
               if (nclips >= numClipPlanes) {
                  break;
               }
            }
         }
      }
      
      int bytesLeft = getSize ()-buff.position ();
      for (int i=0; i<bytesLeft; ++i) {
         buff.put ((byte)0);
      }
      
      buff.flip ();
      update(gl, buff);
      
      return nclips;
   }
      
   @Override
   public ClipPlanesUBO acquire () {
      return (ClipPlanesUBO)super.acquire ();
   }
   
   public static ClipPlanesUBO generate(GL3 gl, int progId, int nClipPlanes) {
      return new ClipPlanesUBO (gl, progId, nClipPlanes);
   }

}
