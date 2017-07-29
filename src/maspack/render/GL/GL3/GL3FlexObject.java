package maspack.render.GL.GL3;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.util.BufferUtilities;

/**
 * Flexible object, creates an internal dynamic vertex buffer that can store positions
 * @author Antonio
 *
 */
public class GL3FlexObject extends GL3ResourceBase {

   VertexArrayObject vao;
   VertexBufferObject vbo;
   IndexBufferObject ibo;
   
   ByteBuffer vbuff;
   ByteBuffer ibuff;
   
   GL3VertexAttributeInfo info;
   GL3VertexAttributeInfo posAttr;
   GL3VertexAttributeInfo nrmAttr;
   GL3VertexAttributeInfo clrAttr;
   GL3VertexAttributeInfo texAttr;
   
   private static final PositionBufferPutter posPutter = PositionBufferPutter.getDefault ();
   private static final NormalBufferPutter nrmPutter = NormalBufferPutter.getDefault ();
   private static final ColorBufferPutter clrPutter = ColorBufferPutter.getDefault ();
   private static final TextureCoordBufferPutter texPutter = TextureCoordBufferPutter.getDefault ();
   
   private static final int DEFAULT_MININUM_PERSISTENT_SIZE = 512; // enough for at least 8 full vertices
   private static final int DEFAULT_MAXIMUM_PERSISTENT_SIZE = 16777216;  // 16 MB
   
   private static float[] DEFAULT_NORMAL = {0,0,1};
   private static byte[] DEFAULT_COLOR = {(byte)255, (byte)255, (byte)255, (byte)255};
   private static float[] DEFAULT_TEXCOORD = {0, 0};

   private int minimumPersistantSize;
   private int maximumPersistantSize;
   
   boolean hasPosition;
   boolean hasNormal;
   boolean hasColor;
   boolean hasTexcoord;
   boolean hasIndices;
   boolean building;
   
   int numVertices;
   int numIndices;
   int vstride;
   
   IndexBufferPutter indexPutter;
   
   // storing of "current" attributes
   float[] currentNormal;
   byte[] currentColor;
   float[] currentTexcoord;
   
   protected GL3FlexObject(
      GL3VertexAttributeInfo posAttr, GL3VertexAttributeInfo nrmAttr,
      GL3VertexAttributeInfo clrAttr, GL3VertexAttributeInfo texAttr,
      VertexArrayObject vao, VertexBufferObject vbo, IndexBufferObject ibo) {
      // vertex array object, vertex buffer object
      this.vao = vao.acquire ();
      this.vbo = vbo.acquire ();
      this.ibo = ibo.acquire ();
      
      this.posAttr = posAttr;
      this.nrmAttr = nrmAttr;
      this.clrAttr = clrAttr;
      this.texAttr = texAttr;
      
      building = false;
      hasNormal = false;
      hasColor = false;
      hasTexcoord = false;
      hasIndices = false;
      
      numVertices = 0;
      numIndices = 0;
      vstride = 0;
      
      this.minimumPersistantSize = DEFAULT_MININUM_PERSISTENT_SIZE;
      this.maximumPersistantSize = DEFAULT_MAXIMUM_PERSISTENT_SIZE;
      
      // storing current normal/color/texcoord
      currentNormal = Arrays.copyOf (DEFAULT_NORMAL, DEFAULT_NORMAL.length);
      currentColor = Arrays.copyOf (DEFAULT_COLOR, DEFAULT_COLOR.length);
      currentTexcoord = Arrays.copyOf (DEFAULT_TEXCOORD, DEFAULT_TEXCOORD.length);
   }
   
   protected ByteBuffer ensureCapacity(ByteBuffer buff, int cap) {
      if (cap < minimumPersistantSize) {
         cap = minimumPersistantSize;
      }
      if (buff == null) {
         buff = BufferUtilities.newNativeByteBuffer (cap);
      } else if (buff.capacity () < cap) {
         BufferUtilities.freeDirectBuffer (buff);
         buff = BufferUtilities.newNativeByteBuffer (cap);
      }
      return buff;
   }
     
   private static void copy(float[] dst, float[] src, int len) {
      for (int i=0; i<len; ++i) {
         dst[i] = src[i];
      }
   }
   
   private static void copy(byte[] dst, byte[] src, int len) {
      for (int i=0; i<len; ++i) {
         dst[i] = src[i];
      }
   }
   
   private boolean maybeRebindAttributes(GL3 gl, 
      boolean hasPosition, boolean hasNormal, boolean hasColor, boolean hasTexcoord,
      boolean hasIndices) {
      
      if (this.hasPosition != hasPosition || this.hasNormal != hasNormal 
         || this.hasColor != hasColor || this.hasTexcoord != hasTexcoord
         || this.hasIndices != hasIndices) {
         
         vao.bind (gl);
         vbo.bind (gl);
         
         if (hasIndices) {
            ibo.bind (gl);
         }
         
         vstride = 0;
         int posWidth = posPutter.bytesPerPosition ();
         int nrmWidth = nrmPutter.bytesPerNormal();
         int clrWidth = clrPutter.bytesPerColor ();
         int texWidth = texPutter.bytesPerTextureCoord ();
         
         if (hasPosition) {
            vstride += posWidth;
         }
         if (hasNormal) {
            vstride += nrmWidth;
         }
         if (hasColor) {
            vstride += clrWidth;
         }
         if (hasTexcoord) {
            vstride += texWidth;
         }
         
         int offset = 0;
         
         if (hasPosition) {
            GL3AttributeStorage storage = posPutter.storage ();
            int loc = posAttr.getLocation ();
            GL3Utilities.activateVertexAttribute(gl, loc, storage, vstride, offset);
            offset += posWidth;
         }
         
         if (hasNormal) {
            GL3AttributeStorage storage = nrmPutter.storage ();
            int loc = nrmAttr.getLocation ();
            GL3Utilities.activateVertexAttribute(gl, loc, storage, vstride, offset);
            offset += nrmWidth;
         }
         
         if (hasColor) {
            GL3AttributeStorage storage = clrPutter.storage ();
            int loc = clrAttr.getLocation ();
            GL3Utilities.activateVertexAttribute(gl, loc, storage, vstride, offset);
            offset += clrWidth;
         }
         
         if (hasTexcoord) {
            GL3AttributeStorage storage = texPutter.storage ();
            int loc = texAttr.getLocation ();
            GL3Utilities.activateVertexAttribute(gl, loc, storage, vstride, offset);
            offset += texWidth;
         }
         
         this.hasPosition = hasPosition;
         this.hasNormal = hasNormal;
         this.hasColor = hasColor;
         this.hasTexcoord = hasTexcoord;
         this.hasIndices = hasIndices;
         
         return true;
      }
      
      return false;
   }
   
   /**
    * Begins constructing VBOs.  Assumes only positions available.
    */
   public void begin(GL3 gl, int maxVertices) {
      begin(gl, false, false, false, maxVertices, 0);
   }
      
   /**
    * Begins constructing object
    */
   public void begin(GL3 gl, boolean hasNormal, boolean hasColor, boolean hasTexcoord, int maxVertices) {
      begin(gl, hasNormal, hasColor, hasTexcoord, maxVertices, 0);
   }
   
   /**
    * Begins constructing object
    * @param maxIndices max size of index buffer
    */
   public void begin(GL3 gl, boolean hasNormal, boolean hasColor, boolean hasTexcoord, int maxVertices, int maxIndices) {
      
      boolean hasIndices = maxIndices > 0;
      
      // check if we need to re-bind
      maybeRebindAttributes(gl, true, hasNormal, hasColor, hasTexcoord, hasIndices);
      vbuff = ensureCapacity (vbuff, maxVertices*vstride);
      vbuff.clear ();
      numVertices = 0;
      
      if (hasIndices) {
         indexPutter = IndexBufferPutter.getDefault (maxVertices);
         ibuff = ensureCapacity (ibuff, indexPutter.bytesPerIndex ()*maxIndices);
         ibuff.clear ();
      }
      numIndices = 0;
      
      copy(currentNormal, DEFAULT_NORMAL, DEFAULT_NORMAL.length);
      copy(currentColor, DEFAULT_COLOR, DEFAULT_COLOR.length);
      copy(currentTexcoord, DEFAULT_TEXCOORD, DEFAULT_TEXCOORD.length);
      
      building = true;
   }
      
   /**
    * Sets the current normal to use for new vertices
    * @param offset into nrm array
    */
   public void normal(float[] nrm, int offset) {
      normal(nrm[offset], nrm[offset+1], nrm[offset+2]);
   }
   
   /**
    * Sets the current normal to use for new vertices
    */
   public void normal(float[] nrm) {
      normal(nrm, 0);
   }
   
   
   /**
    * Sets the current normal to use for new vertices
    */
   public void normal(float x, float y, float z) {
      currentNormal[0] = x;
      currentNormal[1] = y;
      currentNormal[2] = z;
   }
   
   /**
    * Sets the current color to use for new vertices, including RGBA.
    * @param color 4-color
    */
   public void color(Color color) {
      color(color.getRed (), color.getGreen (), color.getBlue (), color.getAlpha ());
   }
   
   
   /**
    * Sets the current color to use for new vertices, including RGBA.
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public void color(int r, int g, int b, int a) {
      currentColor[0] = (byte)r;
      currentColor[1] = (byte)g;
      currentColor[2] = (byte)b;
      currentColor[3] = (byte)a;
   }
   
   /**
    * Sets the current color to use for new vertices, including RGBA.
    * @param rgba 4-color
    */
   public void color(byte[] rgba) {
      for (int i=0; i<4; ++i) {
         currentColor[i] = rgba[i];
      }
   }
   
   /**
    * Sets the current color to use for new vertices, including RGBA.
    * @param rgba 4-color
    * @param offset offset into rgba array
    */
   public void color(float[] rgba, int offset) {
      for (int i=0; i<4; ++i) {
         currentColor[i] = (byte)(255*rgba[i+offset]);
      }
   }
   
   /**
    * Sets the current color to use for new vertices, including RGBA.
    * @param rgba 4-color
    * @param offset offset into rgba array
    */
   public void color(byte[] rgba, int offset) {
      for (int i=0; i<4; ++i) {
         currentColor[i] = rgba[i+offset];
      }
   }
   
   /**
    * Sets the current texture coordinate
    * @param xy 2D texture coordinate
    */
   public void texcoord(float[] xy) {
      texcoord(xy[0], xy[1]);
   }
   
   /**
    * Sets the current texture coordinate
    * @param xy 2D texture coordinate
    * @param offset offset into coordinate array
    */
   public void texcoord(float[] xy, int offset) {
      texcoord(xy[offset], xy[offset+1]);
   }
   
   /**
    * Sets the current texture coordinate
    */
   public void texcoord(float x, float y) {
      currentTexcoord[0] = x;
      currentTexcoord[1] = y;
   }
   
   /**
    * Adds a new vertex at the supplied position
    * @param pos position
    * @return index of the new vertex
    */
   public int vertex(float[] pos) {
      return vertex(pos[0], pos[1], pos[2]);
   }
   
   /**
    * Adds a new vertex at the supplied position
    * @param pos position
    * @param offset offset into position array
    * @return index of the new vertex
    */
   public int vertex(float[] pos, int offset) {
      return vertex(pos[offset], pos[offset+1], pos[offset+2]);
   }
   
   /**
    * Adds a new vertex at the supplied position
    * @return index of the new vertex
    */
   public int vertex(float x, float y, float z) {
      if (!building) {
         return -1;
      }
      
      posPutter.putPosition (vbuff, x, y, z);
      if (hasNormal) {
         nrmPutter.putNormal (vbuff, currentNormal);
      }
      if (hasColor) {
         clrPutter.putColor (vbuff, currentColor);
      }
      if (hasTexcoord) {
         texPutter.putTextureCoord (vbuff, currentTexcoord);
      }
      
      int idx = numVertices;
      ++numVertices;
      return idx;
   }
   
   /**
    * Adds vertices to the index buffer
    * @param v0 vertex index
    * @return number of indices in the index buffer
    */
   public int index(int v0) {
      indexPutter.putIndex (ibuff, v0);
      ++numIndices;
      return numIndices;
   }
   
   public int index(int... vidxs) {
      indexPutter.putIndices (ibuff, vidxs);
      numIndices += vidxs.length;
      return numIndices;
   }
   
   /**
    * Adds vertices to the index buffer
    * @param vstart starting vertex index
    * @param vend ending vertex index (included)
    * @return number of indices in the index buffer
    */
   public int indexRange(int vstart, int vend) {
      for (int i=vstart; i<=vend; ++i) {
         indexPutter.putIndex (ibuff, i);
      }
      numIndices += (vend-vstart+1);
      return numIndices;
   }
   
   /**
    * Ends building VBOs, commits to GPU 
    */
   public void end(GL3 gl) {
      building = false;
      
      gl.glBindVertexArray (0); // unbind any existing VAOs
      
      vbuff.flip ();
      // resize or update portion
      if (vbuff.limit () >= vbo.getSize()) {
         vbo.fill (gl, vbuff, GL.GL_DYNAMIC_DRAW);
      } else {
         vbo.update (gl, vbuff);  
      }
      
      // maybe free buffer
      if (vbuff.capacity () > maximumPersistantSize) {
         BufferUtilities.freeDirectBuffer (vbuff);
         vbuff = null;
      }
      
      if (hasIndices) {
         ibuff.flip ();
         // resize or update portion
         if (ibuff.limit () >= ibo.getSize()) {
            ibo.fill (gl, ibuff, GL.GL_DYNAMIC_DRAW);
         } else {
            ibo.update (gl, ibuff);  
         }
         
         // maybe free buffer
         if (ibuff.capacity () > maximumPersistantSize) {
            BufferUtilities.freeDirectBuffer (ibuff);
            ibuff = null;
         }
      }
   }
   
   public void drawVertices (GL3 gl, int mode) {
      drawVertices(gl, mode, 0, numVertices);
   }
   
   public void drawVertices (GL3 gl, int mode, int count) {
      drawVertices(gl, mode, 0, count);
   }
   
   public void drawVertices (GL3 gl, int mode, int start, int count) {
      vao.bind (gl);
      gl.glDrawArrays (mode, start, count);
      vao.unbind (gl);
   }
   
   public void drawElements (GL3 gl, int mode) {
      drawElements(gl, mode, 0, numIndices);
   }
   
   public void drawElements (GL3 gl, int mode, int count) {
      drawElements(gl, mode, 0, count);
   }
   
   public void drawElements (GL3 gl, int mode, int start, int count) {
      vao.bind (gl);
      int offset = start*indexPutter.bytesPerIndex ();
      int type = indexPutter.storage ().getGLType ();
      gl.glDrawElements (mode, count, type, offset);
      vao.unbind (gl);
   }
   
   @Override
   public void dispose (GL3 gl) {
      if (vao != null) {
         vao.releaseDispose (gl);
         vbo.releaseDispose (gl);
         ibo.releaseDispose (gl);
         
         vao = null;
         vbo = null;
         ibo = null;
         
         BufferUtilities.freeDirectBuffer (vbuff);
         vbuff = null;
      
         BufferUtilities.freeDirectBuffer (ibuff);
         ibuff = null;
      }
   }
   
   @Override
   public boolean isDisposed () {
      return (vao == null);
   }
   
   public static GL3FlexObject generate(GL3 gl, GL3VertexAttributeInfo posAttr, GL3VertexAttributeInfo nrmAttr,
      GL3VertexAttributeInfo clrAttr, GL3VertexAttributeInfo texAttr) {
      VertexArrayObject vao = VertexArrayObject.generate (gl);
      VertexBufferObject vbo = VertexBufferObject.generate (gl);
      IndexBufferObject ibo = IndexBufferObject.generate (gl);
      GL3FlexObject out = new GL3FlexObject(posAttr, nrmAttr, clrAttr, texAttr, vao, vbo, ibo);
      
      return out;
   }

}
