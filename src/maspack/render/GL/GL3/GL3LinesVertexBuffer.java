package maspack.render.GL.GL3;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.render.GL.GLSupport;
import maspack.util.BufferUtilities;

public class GL3LinesVertexBuffer extends GL3ResourceBase {

   VertexBufferObject vbo;
   ByteBuffer buff;

   GL3VertexAttributeArrayInfo radAttr;
   GL3VertexAttributeArrayInfo bscaleAttr;
   GL3VertexAttributeArrayInfo tscaleAttr;

   // track version numbers so we can detect what has changed since last use
   float lastLineRadius;
   float[] lastBottomScaleOffset;
   float[] lastTopScaleOffset;
   private static final float[] NO_SCALE_OFFSET_BOTTOM = {1f, 0f, 0f, 0f};
   private static final float[] NO_SCALE_OFFSET_TOP = {1f, 0f, 0f, 1f};

   private GL3LinesVertexBuffer(VertexBufferObject vbo, 
      GL3VertexAttributeInfo radAttr,
      GL3VertexAttributeInfo bscaleAttr, GL3VertexAttributeInfo tscaleAttr) {

      this.vbo = vbo.acquire ();  // hold on to VBO

      int FLOAT_SIZE = GL3AttributeStorage.FLOAT.width ();
      int FLOAT_4_SIZE = GL3AttributeStorage.FLOAT_4.width ();
      int stride = FLOAT_SIZE + 2*FLOAT_4_SIZE;

      this.radAttr = new GL3VertexAttributeArrayInfo (radAttr, 
         GL3AttributeStorage.FLOAT, 0, stride, 1);
      this.bscaleAttr = new GL3VertexAttributeArrayInfo (bscaleAttr, 
         GL3AttributeStorage.FLOAT_4, FLOAT_SIZE, stride, 1);
      this.tscaleAttr = new GL3VertexAttributeArrayInfo (tscaleAttr, 
         GL3AttributeStorage.FLOAT_4, FLOAT_SIZE+FLOAT_4_SIZE, stride, 1);

      lastLineRadius = 0;
      lastBottomScaleOffset = null;
      lastTopScaleOffset = null;
      
      buff = BufferUtilities.newNativeByteBuffer(9*GLSupport.FLOAT_SIZE);
      
   }
   
   private static float[] copyOf(float[] array) {
      if (array == null) {
         return null;
      }
      return Arrays.copyOf (array, array.length);
   }

   public boolean maybeUpdate(GL3 gl, float lineRadius, float[] bottomScaleOffset, float[] topScaleOffset) {
      boolean updated = maybeUpdateLine (gl, lineRadius, bottomScaleOffset, topScaleOffset);
      if (updated) {
         lastLineRadius = lineRadius;
         lastBottomScaleOffset = copyOf (bottomScaleOffset);
         lastTopScaleOffset = copyOf (topScaleOffset);
      }
      return updated;
   }

   private static boolean arraysEqual(float[] a1, float[] a2) {
      if (a1 == a2) {
         return true;
      }
      if (a1 == null || a2 == null) {
         return false;
      }
      if (a1.length != a2.length) {
         return false;
      }
      for (int i=0; i<a1.length; ++i) {
         if ( a1[i] != a2[i] ) {
            return false;
         }
      }
      return true;
   }

   protected boolean maybeUpdateLine(GL3 gl, float lineRadius, 
      float[] bottomScaleOffset, float[] topScaleOffset) {

      boolean updated = false;

      if (lastLineRadius != lineRadius || 
         !arraysEqual(lastBottomScaleOffset, bottomScaleOffset)
         || !arraysEqual(lastTopScaleOffset, topScaleOffset)) {

         fillLineVBO(gl, lineRadius, bottomScaleOffset, topScaleOffset); 
         updated = true;
      }
      return updated;
   }

   private void fillLineVBO(GL3 gl, float lineRadius, 
      float[] bottomScaleOffset, float[] topScaleOffset) {

      // length info
      buff.putFloat(lineRadius);

      // offset for bottom
      if (bottomScaleOffset == null) {
         bottomScaleOffset = NO_SCALE_OFFSET_BOTTOM;
      }
      buff.putFloat (bottomScaleOffset[0]);
      buff.putFloat (bottomScaleOffset[1]);
      buff.putFloat (bottomScaleOffset[2]);
      buff.putFloat (bottomScaleOffset[3]);

      // top offset
      if (topScaleOffset == null) {
         topScaleOffset = NO_SCALE_OFFSET_TOP;
      }
      buff.putFloat (topScaleOffset[0]);
      buff.putFloat (topScaleOffset[1]);
      buff.putFloat (topScaleOffset[2]);
      buff.putFloat (topScaleOffset[3]);

      // line information
      buff.flip();
      gl.glBindVertexArray (0); // unbind any existing VAOs
      vbo.fill(gl, buff, GL.GL_DYNAMIC_DRAW);
   }

   public void bind(GL3 gl, int numInstances) {
      vbo.bind (gl);
      radAttr.bind (gl);
      radAttr.bindDivisor (gl, numInstances);
      bscaleAttr.bind (gl);
      bscaleAttr.bindDivisor (gl, numInstances);
      tscaleAttr.bind (gl);
      tscaleAttr.bindDivisor (gl, numInstances);
   }

   @Override
   public boolean isValid () {
      if (vbo == null) {
         return false;
      }
      return vbo.isValid ();
   }

   @Override
   public void dispose (GL3 gl) {
      if (vbo != null) {
         vbo.releaseDispose (gl);
         vbo = null;
      }

      lastLineRadius = 0;
      lastBottomScaleOffset = null;
      lastTopScaleOffset = null;
      BufferUtilities.freeDirectBuffer (buff);
      buff = null;
   }
   
   @Override
   public boolean isDisposed () {
      return (vbo == null);
   }
   
   @Override
   public GL3LinesVertexBuffer acquire () {
      return (GL3LinesVertexBuffer)super.acquire ();
   }

   public static GL3LinesVertexBuffer generate (GL3 gl, GL3VertexAttributeInfo radAttr,
      GL3VertexAttributeInfo bscaleAttr, GL3VertexAttributeInfo tscaleAttr) {
      
      VertexBufferObject vbo = VertexBufferObject.generate(gl);
      
      GL3LinesVertexBuffer lbuff = new GL3LinesVertexBuffer (vbo, radAttr, bscaleAttr, tscaleAttr);
      lbuff.fillLineVBO (gl, 1, null, null);
      return lbuff;
   }



}
