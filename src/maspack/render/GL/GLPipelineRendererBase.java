package maspack.render.GL;

import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import maspack.util.BufferUtilities;

public abstract class GLPipelineRendererBase implements GLPipelineRenderer {

   private static final int NORMAL_BYTES = 3*4;
   private static final int COLOR_BYTES = 4;
   private static final int TEXCOORD_BYTES = 2*4;
   private static final int POSITION_BYTES = 3*4;
   
   boolean normalsEnabled;
   boolean colorsEnabled;
   boolean texcoordsEnabled;
   
   byte[] color;
   float[] normal;
   float[] texcoord;
   
   int colorOffset;
   int normalOffset;
   int texcoordOffset;
   int positionOffset;
   int vertexStride;
   
   int mode;
   int maxverts;
   boolean drawing;
   int nverts;
   
   GL2GL3 gl;
   
   ByteBuffer vbuff;
   
   public GLPipelineRendererBase () {
      normalsEnabled = false;
      colorsEnabled = false;
      texcoordsEnabled = false;
      
      color = new byte[4];
      normal = new float[3];
      texcoord = new float[2];
      
      maxverts = 0;
      mode = 0;
      drawing = false;
      
      nverts = 0;
      vbuff = null;
   }
   
   private void ensureBufferCapacity(int cap) {
      if (vbuff == null || vbuff.capacity () < cap) {
         BufferUtilities.freeDirectBuffer (vbuff);
         vbuff  = BufferUtilities.newNativeByteBuffer (cap);
      }
   }
   
   @Override
   public boolean isEmpty () {
      if (vbuff == null || vbuff.position () == 0) {
         return true;
      }
      return false;
   }

   @Override
   public void setup (
      boolean hasNormals, boolean hasColors, boolean hasTexcoords) {
      enableNormals (hasNormals);
      enableColors (hasColors);
      enableTexCoords (hasTexcoords);
   }
   
   @Override
   public void enableNormals (boolean set) {
      if (!drawing) {
         normalsEnabled = set;
      }
   }
   
   @Override
   public boolean isNormalsEnabled() {
      return normalsEnabled;
   }

   @Override
   public void enableColors (boolean set) {
      if (!drawing) {
         colorsEnabled = set;
      }
   }
   
   @Override
   public boolean isColorsEnabled() {
      return colorsEnabled;
   }

   @Override
   public void enableTexCoords (boolean set) {
      if (!drawing) {
         texcoordsEnabled = set;
      }
   }
   
   @Override
   public boolean isTexCoordsEnabled() {
      return texcoordsEnabled;
   }
   
   /**
    * Prepare to start drawing
    * @param gl
    * @param normalOffset
    * @param colorOffset
    * @param texcoordOffset
    * @param positionOffset
    * @param vertexStride
    */
   public abstract void bind(GL gl, ByteBuffer buff, int normalOffset,
      int colorOffset, int texcoordOffset, int positionOffset,
      int vertexStride);

   @Override
   public void begin (GL gl, int glMode, int maxVertices) {
      this.gl = (GL2GL3)gl;
      this.mode = glMode;
      this.maxverts = maxVertices;
      drawing = true;
      
      vertexStride = 0;
      if (normalsEnabled) {
         normalOffset = vertexStride;
         vertexStride += NORMAL_BYTES;
      } else {
         normalOffset = -1;
      }
      
      if (colorsEnabled) {
         colorOffset = vertexStride;
         vertexStride += COLOR_BYTES;
      } else {
         colorOffset = -1;
      }
      
      if (texcoordsEnabled) {
         texcoordOffset = vertexStride;
         vertexStride += TEXCOORD_BYTES;   
      } else {
         texcoordOffset = -1;
      }
      
      positionOffset = vertexStride;
      vertexStride += POSITION_BYTES;
      
      ensureBufferCapacity (maxVertices*vertexStride);
      
      bind(gl, vbuff, normalOffset, colorOffset, texcoordOffset, positionOffset, vertexStride);
   }

   @Override
   public void normal (float x, float y, float z) {
      normal[0] = x;
      normal[1] = y;
      normal[2] = z;
   }

   @Override
   public void color (int r, int g, int b, int a) {
      color[0] = (byte)(0xFF&r);
      color[1] = (byte)(0xFF&g);
      color[2] = (byte)(0xFF&b);
      color[3] = (byte)(0xFF&a);
   }

   @Override
   public void texcoord (float x, float y) {
      texcoord[0] = x;
      texcoord[1] = y;
   }

   @Override
   public void vertex (float x, float y, float z) {
      
      if (normalsEnabled) {
         vbuff.putFloat (normal[0]);
         vbuff.putFloat (normal[1]);
         vbuff.putFloat (normal[2]);
      }
      
      if (colorsEnabled) {
         vbuff.putFloat (color[0]);
         vbuff.putFloat (color[1]);
         vbuff.putFloat (color[2]);
         vbuff.putFloat (color[3]);
      }
      
      if (texcoordsEnabled) {
         vbuff.putFloat (texcoord[0]);
         vbuff.putFloat (texcoord[1]);
      }
      
      vbuff.putFloat (x);
      vbuff.putFloat (y);
      vbuff.putFloat (z);
      
      ++nverts;
      
      if (vbuff.position () == vbuff.capacity ()) {
         flush ();
      }
   }
   
   protected abstract void draw (GL gl, int glMode, ByteBuffer vbuff, int count);

   @Override
   public void flush () {
      vbuff.flip ();
      draw(gl, mode, vbuff, nverts);
      vbuff.clear ();
      nverts = 0;
   }
   
   protected abstract void unbind(GL gl);

   @Override
   public void end () {
      flush ();
      mode = 0;
      maxverts = 0;
      gl = null;
      drawing = false;
   }

   @Override
   public void dispose (GL gl) {
      BufferUtilities.freeDirectBuffer (vbuff);
      vbuff = null;
   }
   
   @Override
   public GL getGL () {
      return gl;
   }

}
