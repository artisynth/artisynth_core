package maspack.render.GL;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;

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
   byte[] loopBuff;
   
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
      loopBuff = null;
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
    */
   public abstract void bind(GL gl, ByteBuffer buff, int normalOffset,
      int colorOffset, int texcoordOffset, int positionOffset,
      int vertexStride);

   @Override
   public void begin (GL gl, int glMode, int maxVertices) {
      this.gl = (GL2GL3)gl;
      this.mode = glMode;
      
      // divisible by 2, 3, 4
      if (maxVertices < 12) {
         maxVertices = 12;
      }
      
      switch (glMode) {
         case GL.GL_POINTS:
         case GL.GL_LINE_STRIP:
         case GL.GL_LINE_LOOP:
         case GL.GL_TRIANGLE_STRIP:
         case GL.GL_TRIANGLE_FAN:
            break;
         case GL.GL_LINES:
            if ( (maxVertices % 2) == 1) {
               ++maxVertices;
            }
            break;
         case GL.GL_TRIANGLES: {
            int off = (3-maxVertices % 3) % 3; 
            maxVertices += off;
            break;
         }
      }
      
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
      loopBuff = null;
      
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
      
      // copy first vertex for loop
      if (mode == GL.GL_LINE_LOOP && loopBuff == null) {
         loopBuff = new byte[vertexStride];
         int pos = vbuff.position ();
         vbuff.position (pos-vertexStride);
         vbuff.get (loopBuff, 0, vertexStride);
         vbuff.position (pos);
      }
      
      ++nverts;
      
      if (vbuff.position () == vbuff.capacity ()) {
         flush ();
      }
   }
   
   protected abstract void draw (GL gl, int glMode, ByteBuffer vbuff, int count);

   @Override
   public void flush () {
      
      vbuff.flip ();
      
      int glMode = mode;
      // draw a line loop as a line strip, append final
      // line segment later
      if (mode == GL.GL_LINE_LOOP) {
         glMode = GL.GL_LINE_STRIP;
      }

      int nv = nverts;
      // potentially unfinished primitives
      switch (glMode) {
         case GL.GL_LINE_STRIP:
         case GL.GL_LINE_LOOP:
            if (nverts < 2) {
               nv = 0;
            }
         case GL.GL_TRIANGLE_STRIP:
         case GL.GL_TRIANGLE_FAN:
            if (nverts < 3) {
               nv = 0;
            }
         case GL.GL_LINES:
            if ((nverts % 2) == 1) {
               nv = nverts-1;
            }
         case GL.GL_TRIANGLES: {
            int off = nverts % 3;
            nv = nverts-off;
         }
      }
      
      draw(gl, glMode, vbuff, nv);
      
      // maybe copy some bytes back
      byte[] front = null;
      int nfront = 0;
      
      switch (glMode) {
         case GL.GL_LINE_STRIP:
         case GL.GL_LINE_LOOP: {
            // last drawn vertex plus remaining
            if (nverts < 2) {
               front = new byte[vertexStride*nverts];
               vbuff.rewind ();
               vbuff.get (front);
               nfront = nverts;
            } else {
               nfront = nverts-nv+1;
               front = new byte[vertexStride*nfront];
               vbuff.position ((nv-1)*vertexStride);
               vbuff.get (front);
            }
            break;
         }
         case GL.GL_TRIANGLE_STRIP: {
            // last two drawn vertices plus remaining
            if (nverts < 3) {
               front = new byte[vertexStride*nverts];
               vbuff.rewind ();
               vbuff.get (front);
               nfront = nverts;
            } else {
               
               // if we drew an odd number of triangles, 
               // need to duplicate a vertex
               int nt = nv-2;
               nfront = nverts-nv+2;
               if ((nt % 2)==1) {
                  ++nfront;  
                  // duplicate first vertex
                  vbuff.position ((nv-2)*vertexStride);
                  front = new byte[vertexStride*nfront];
                  vbuff.get (front, 0, vertexStride);
                  // copy remaining
                  vbuff.position ((nv-2)*vertexStride);
                  vbuff.get (front, vertexStride, (nfront-1)*vertexStride);
               } else {
                  // last two drawn plus remaining
                  vbuff.position ((nv-2)*vertexStride);
                  front = new byte[vertexStride*nfront];
                  vbuff.get (front);
               }
            }
            break;
         }
         case GL.GL_TRIANGLE_FAN: {
            // duplicate first vertex and last drawn vertex
            if (nverts < 3) {
               front = new byte[vertexStride*nverts];
               vbuff.rewind ();
               vbuff.get (front);
               nfront = nverts;
            } else {
               int nrem = nverts-nv;
               front = new byte[vertexStride*(2+nrem)];
               vbuff.rewind ();
               vbuff.get (front, 0, vertexStride);
               vbuff.position ((nv-1)*vertexStride);
               vbuff.get (front, vertexStride, (nrem+1)*vertexStride);
               nfront = 2+nrem;
            }
            break;
         }
         case GL.GL_LINES: {
            // add an odd vertex back
            if ((nverts % 2) == 1) {
               nfront  = 1;
               front = new byte[vertexStride];
               vbuff.position (nv*vertexStride);
               vbuff.get (front);
            }
            break;
         }
         case GL.GL_TRIANGLES: {
            // add remaining vertices back
            int off = nverts - nv;
            if (off > 0) {
               nfront  = off;
               front = new byte[nfront*vertexStride];
               vbuff.position (nv*vertexStride);
               vbuff.get (front);
            }
            break;
         }
      }
      
      vbuff.clear ();
      nverts = 0;
      
      if (nfront > 0) {
         vbuff.put (front);
         nverts = nfront;
      }
      
   }
   
   protected abstract void unbind(GL gl);

   @Override
   public void end () {
      flush ();
      
      // deal with loop
      if (mode == GL.GL_LINE_LOOP && loopBuff != null) {
         // add vertex back
         vbuff.put (loopBuff);
         vbuff.flip ();
         draw(gl, GL.GL_LINE_STRIP, vbuff, nverts+1);
      }
      vbuff.clear ();
      nverts = 0;
      
      mode = 0;
      maxverts = 0;
      gl = null;
      drawing = false;
      loopBuff = null;
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
