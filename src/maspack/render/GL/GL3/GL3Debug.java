package maspack.render.GL.GL3;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL3ES3;
import com.jogamp.opengl.GL3bc;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GL4ES3;
import com.jogamp.opengl.GL4bc;
import com.jogamp.opengl.GLArrayData;
import com.jogamp.opengl.GLBufferStorage;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLES1;
import com.jogamp.opengl.GLES2;
import com.jogamp.opengl.GLES3;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;

import com.jogamp.common.nio.PointerBuffer;

import maspack.render.GL.GLSupport;

public class GL3Debug implements GL3 {

   GL3 gl;
   boolean checkForErrors;

   public GL3Debug(GL3 gl) {
      this.gl = gl;
      checkForErrors = true;
   }

   public void setCheckErrors(boolean set) {
      checkForErrors = set;
   }

   public GL getDownstreamGL() throws GLException {
      GL out = gl.getDownstreamGL();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL getRootGL() throws GLException {
      GL out = gl.getRootGL();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL getGL() throws GLException {
      GL out = gl.getGL();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL4bc getGL4bc() throws GLException {
      GL4bc out = gl.getGL4bc();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL4 getGL4() throws GLException {
      GL4 out = gl.getGL4();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL3bc getGL3bc() throws GLException {
      GL3bc out = gl.getGL3bc();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL3 getGL3() throws GLException {
      GL3 out = gl.getGL3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL2 getGL2() throws GLException {
      GL2 out = gl.getGL2();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GLES1 getGLES1() throws GLException {
      GLES1 out = gl.getGLES1();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GLES2 getGLES2() throws GLException {
      GLES2 out = gl.getGLES2();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GLES3 getGLES3() throws GLException {
      GLES3 out = gl.getGLES3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL2ES1 getGL2ES1() throws GLException {
      GL2ES1 out = gl.getGL2ES1();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL2ES2 getGL2ES2() throws GLException {
      GL2ES2 out = gl.getGL2ES2();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL2ES3 getGL2ES3() throws GLException {
      GL2ES3 out = gl.getGL2ES3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL3ES3 getGL3ES3() throws GLException {
      GL3ES3 out = gl.getGL3ES3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL4ES3 getGL4ES3() throws GLException {
      GL4ES3 out = gl.getGL4ES3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GL2GL3 getGL2GL3() throws GLException {
      GL2GL3 out = gl.getGL2GL3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GLProfile getGLProfile() {
      GLProfile out = gl.getGLProfile();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GLContext getContext() {
      GLContext out = gl.getContext();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int getMaxRenderbufferSamples() {
      int out = gl.getMaxRenderbufferSamples();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int getSwapInterval() {
      int out = gl.getSwapInterval();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public Object getPlatformGLExtensions() {
      Object out = gl.getPlatformGLExtensions();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public Object getExtension(String extensionName) {
      Object out = gl.getExtension(extensionName);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int getBoundBuffer(int target) {
      int out = gl.getBoundBuffer(target);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GLBufferStorage getBufferStorage(int bufferName) {
      GLBufferStorage out = gl.getBufferStorage(bufferName);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int getBoundFramebuffer(int target) {
      int out = gl.getBoundFramebuffer(target);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int getDefaultDrawFramebuffer() {
      int out = gl.getDefaultDrawFramebuffer();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int getDefaultReadFramebuffer() {
      int out = gl.getDefaultReadFramebuffer();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int getDefaultReadBuffer() {
      int out = gl.getDefaultReadBuffer();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glActiveTexture(int arg0) {
      System.out.println("glActiveTexture(" + arg0 + ")");
      gl.glActiveTexture(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glAttachShader(int arg0, int arg1) {
      System.out.println("glAttachShader(" + arg0 + "," + arg1 + ")");
      gl.glAttachShader(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBeginConditionalRender(int arg0, int arg1) {
      System.out.println("glBeginConditionalRender(" + arg0 + "," + arg1 + ")");
      gl.glBeginConditionalRender(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBeginQuery(int arg0, int arg1) {
      System.out.println("glBeginQuery(" + arg0 + "," + arg1 + ")");
      gl.glBeginQuery(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBeginTransformFeedback(int arg0) {
      System.out.println("glBeginTransformFeedback(" + arg0 + ")");
      gl.glBeginTransformFeedback(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBindAttribLocation(int arg0, int arg1, String arg2) {
      System.out.println(
         "glBindAttribLocation(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glBindAttribLocation(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBindBuffer(int arg0, int arg1) {
      System.out.println("glBindBuffer(" + arg0 + "," + arg1 + ")");
      gl.glBindBuffer(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBindBufferBase(int arg0, int arg1, int arg2) {
      System.out
      .println("glBindBufferBase(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glBindBufferBase(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBindBufferRange(
      int arg0, int arg1, int arg2, long arg3, long arg4) {
      System.out.println(
         "glBindBufferRange(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glBindBufferRange(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBindFragDataLocation(int arg0, int arg1, String arg2) {
      System.out.println(
         "glBindFragDataLocation(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glBindFragDataLocation(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBindFragDataLocationIndexed(
      int arg0, int arg1, int arg2, String arg3) {
      System.out.println(
         "glBindFragDataLocationIndexed(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glBindFragDataLocationIndexed(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBindFramebuffer(int arg0, int arg1) {
      System.out.println("glBindFramebuffer(" + arg0 + "," + arg1 + ")");
      gl.glBindFramebuffer(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBindRenderbuffer(int arg0, int arg1) {
      System.out.println("glBindRenderbuffer(" + arg0 + "," + arg1 + ")");
      gl.glBindRenderbuffer(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBindSampler(int arg0, int arg1) {
      System.out.println("glBindSampler(" + arg0 + "," + arg1 + ")");
      gl.glBindSampler(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBindTexture(int arg0, int arg1) {
      System.out.println("glBindTexture(" + arg0 + "," + arg1 + ")");
      gl.glBindTexture(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBindVertexArray(int arg0) {
      System.out.println("glBindVertexArray(" + arg0 + ")");
      gl.glBindVertexArray(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBlendColor(float arg0, float arg1, float arg2, float arg3) {
      System.out.println(
         "glBlendColor(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glBlendColor(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBlendEquation(int arg0) {
      System.out.println("glBlendEquation(" + arg0 + ")");
      gl.glBlendEquation(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBlendEquationSeparate(int arg0, int arg1) {
      System.out.println("glBlendEquationSeparate(" + arg0 + "," + arg1 + ")");
      gl.glBlendEquationSeparate(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBlendFunc(int arg0, int arg1) {
      System.out.println("glBlendFunc(" + arg0 + "," + arg1 + ")");
      gl.glBlendFunc(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBlendFuncSeparate(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glBlendFuncSeparate(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glBlendFuncSeparate(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBlitFramebuffer(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, int arg9) {
      System.out.println(
         "glBlitFramebuffer(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8
         + "," + arg9 + ")");
      gl.glBlitFramebuffer(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBufferAddressRangeNV(
      int arg0, int arg1, long arg2, long arg3) {
      System.out.println(
         "glBufferAddressRangeNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glBufferAddressRangeNV(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBufferData(int arg0, long arg1, Buffer arg2, int arg3) {
      System.out.println(
         "glBufferData(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glBufferData(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glBufferSubData(int arg0, long arg1, long arg2, Buffer arg3) {
      System.out.println(
         "glBufferSubData(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glBufferSubData(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public int glCheckFramebufferStatus(int arg0) {
      System.out.println("glCheckFramebufferStatus(" + arg0 + ")");
      int out = gl.glCheckFramebufferStatus(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glClampColor(int arg0, int arg1) {
      System.out.println("glClampColor(" + arg0 + "," + arg1 + ")");
      gl.glClampColor(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClear(int arg0) {
      System.out.println("glClear(" + arg0 + ")");
      gl.glClear(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClearBufferfi(int arg0, int arg1, float arg2, int arg3) {
      System.out.println(
         "glClearBufferfi(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glClearBufferfi(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClearBufferfv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glClearBufferfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glClearBufferfv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClearBufferfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out
      .println("glClearBufferfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glClearBufferfv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClearBufferiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glClearBufferiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glClearBufferiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClearBufferiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glClearBufferiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glClearBufferiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClearBufferuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glClearBufferuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glClearBufferuiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClearBufferuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glClearBufferuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glClearBufferuiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClearColor(float arg0, float arg1, float arg2, float arg3) {
      System.out.println(
         "glClearColor(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glClearColor(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClearDepth(double arg0) {
      System.out.println("glClearDepth(" + arg0 + ")");
      gl.glClearDepth(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClearDepthf(float arg0) {
      System.out.println("glClearDepthf(" + arg0 + ")");
      gl.glClearDepthf(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glClearNamedBufferDataEXT(
      int arg0, int arg1, int arg2, int arg3, Buffer arg4) {
      // XXX not implemented
   }

   public void glClearNamedBufferSubDataEXT(
      int arg0, int arg1, int arg2, int arg3, long arg4, long arg5,
      Buffer arg6) {
      // XXX not implemented
   }

   public void glClearStencil(int arg0) {
      System.out.println("glClearStencil(" + arg0 + ")");
      gl.glClearStencil(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public int glClientWaitSync(long arg0, int arg1, long arg2) {
      System.out
      .println("glClientWaitSync(" + arg0 + "," + arg1 + "," + arg2 + ")");
      int out = gl.glClientWaitSync(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glColorFormatNV(int arg0, int arg1, int arg2) {
      System.out
      .println("glColorFormatNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glColorFormatNV(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glColorMask(
      boolean arg0, boolean arg1, boolean arg2, boolean arg3) {
      System.out.println(
         "glColorMask(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glColorMask(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glColorMaski(
      int arg0, boolean arg1, boolean arg2, boolean arg3, boolean arg4) {
      System.out.println(
         "glColorMaski(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glColorMaski(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glColorP3ui(int arg0, int arg1) {
      // XXX not implemented
   }

   public void glColorP3uiv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented
   }

   public void glColorP3uiv(int arg0, IntBuffer arg1) {
      // XXX not implemented
   }

   public void glColorP4ui(int arg0, int arg1) {
      // XXX not implemented
   }

   public void glColorP4uiv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented
   }

   public void glColorP4uiv(int arg0, IntBuffer arg1) {
      // XXX not implemented
   }

   public void glCompileShader(int arg0) {
      System.out.println("glCompileShader(" + arg0 + ")");
      gl.glCompileShader(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompileShaderIncludeARB(
      int arg0, int arg1, String[] arg2, int[] arg3, int arg4) {
      System.out.println(
         "glCompileShaderIncludeARB(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glCompileShaderIncludeARB(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompileShaderIncludeARB(
      int arg0, int arg1, String[] arg2, IntBuffer arg3) {
      System.out.println(
         "glCompileShaderIncludeARB(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glCompileShaderIncludeARB(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, Buffer arg6) {
      System.out.println(
         "glCompressedTexImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glCompressedTexImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, long arg6) {
      System.out.println(
         "glCompressedTexImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glCompressedTexImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      Buffer arg7) {
      System.out.println(
         "glCompressedTexImage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ")");
      gl.glCompressedTexImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      long arg7) {
      System.out.println(
         "glCompressedTexImage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ")");
      gl.glCompressedTexImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, Buffer arg8) {
      System.out.println(
         "glCompressedTexImage3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8
         + ")");
      gl.glCompressedTexImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, long arg8) {
      System.out.println(
         "glCompressedTexImage3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8
         + ")");
      gl.glCompressedTexImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexSubImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, Buffer arg6) {
      System.out.println(
         "glCompressedTexSubImage1D(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glCompressedTexSubImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexSubImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, long arg6) {
      System.out.println(
         "glCompressedTexSubImage1D(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glCompressedTexSubImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexSubImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, Buffer arg8) {
      System.out.println(
         "glCompressedTexSubImage2D(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ","
            + arg8 + ")");
      gl.glCompressedTexSubImage2D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexSubImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, long arg8) {
      System.out.println(
         "glCompressedTexSubImage2D(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ","
            + arg8 + ")");
      gl.glCompressedTexSubImage2D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexSubImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, int arg9, Buffer arg10) {
      System.out.println(
         "glCompressedTexSubImage3D(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ","
            + arg8 + "," + arg9 + "," + arg10 + ")");
      gl.glCompressedTexSubImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCompressedTexSubImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, int arg9, long arg10) {
      System.out.println(
         "glCompressedTexSubImage3D(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ","
            + arg8 + "," + arg9 + "," + arg10 + ")");
      gl.glCompressedTexSubImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCopyBufferSubData(
      int arg0, int arg1, long arg2, long arg3, long arg4) {
      System.out.println(
         "glCopyBufferSubData(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glCopyBufferSubData(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCopyTexImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
      System.out.println(
         "glCopyTexImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glCopyTexImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCopyTexImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7) {
      System.out.println(
         "glCopyTexImage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ")");
      gl.glCopyTexImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCopyTexSubImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
      System.out.println(
         "glCopyTexSubImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glCopyTexSubImage1D(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCopyTexSubImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7) {
      System.out.println(
         "glCopyTexSubImage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ")");
      gl.glCopyTexSubImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glCopyTexSubImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8) {
      System.out.println(
         "glCopyTexSubImage3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8
         + ")");
      gl.glCopyTexSubImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public int glCreateProgram() {
      System.out.println("glCreateProgram()");
      int out = gl.glCreateProgram();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int glCreateShader(int arg0) {
      System.out.println("glCreateShader(" + arg0 + ")");
      int out = gl.glCreateShader(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public long glCreateSyncFromCLeventARB(long arg0, long arg1, int arg2) {
      System.out.println(
         "glCreateSyncFromCLeventARB(" + arg0 + "," + arg1 + "," + arg2 + ")");
      long out = gl.glCreateSyncFromCLeventARB(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glCullFace(int arg0) {
      System.out.println("glCullFace(" + arg0 + ")");
      gl.glCullFace(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDebugMessageControl(
      int arg0, int arg1, int arg2, int arg3, int[] arg4, int arg5,
      boolean arg6) {
      System.out.println(
         "glDebugMessageControl(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glDebugMessageControl(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDebugMessageControl(
      int arg0, int arg1, int arg2, int arg3, IntBuffer arg4, boolean arg5) {
      System.out.println(
         "glDebugMessageControl(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glDebugMessageControl(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDebugMessageEnableAMD(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, boolean arg5) {
      System.out.println(
         "glDebugMessageEnableAMD(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glDebugMessageEnableAMD(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDebugMessageEnableAMD(
      int arg0, int arg1, int arg2, IntBuffer arg3, boolean arg4) {
      System.out.println(
         "glDebugMessageEnableAMD(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glDebugMessageEnableAMD(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDebugMessageInsert(
      int arg0, int arg1, int arg2, int arg3, int arg4, String arg5) {
      System.out.println(
         "glDebugMessageInsert(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glDebugMessageInsert(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDebugMessageInsertAMD(
      int arg0, int arg1, int arg2, int arg3, String arg4) {
      System.out.println(
         "glDebugMessageInsertAMD(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glDebugMessageInsertAMD(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteBuffers(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glDeleteBuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteBuffers(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteBuffers(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteBuffers(" + arg0 + "," + arg1 + ")");
      gl.glDeleteBuffers(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteFramebuffers(int arg0, int[] arg1, int arg2) {
      System.out.println(
         "glDeleteFramebuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteFramebuffers(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteFramebuffers(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteFramebuffers(" + arg0 + "," + arg1 + ")");
      gl.glDeleteFramebuffers(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteNamedStringARB(int arg0, String arg1) {
      System.out.println("glDeleteNamedStringARB(" + arg0 + "," + arg1 + ")");
      gl.glDeleteNamedStringARB(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteProgram(int arg0) {
      System.out.println("glDeleteProgram(" + arg0 + ")");
      gl.glDeleteProgram(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteQueries(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glDeleteQueries(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteQueries(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteQueries(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteQueries(" + arg0 + "," + arg1 + ")");
      gl.glDeleteQueries(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteRenderbuffers(int arg0, int[] arg1, int arg2) {
      System.out.println(
         "glDeleteRenderbuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteRenderbuffers(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteRenderbuffers(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteRenderbuffers(" + arg0 + "," + arg1 + ")");
      gl.glDeleteRenderbuffers(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteSamplers(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glDeleteSamplers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteSamplers(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteSamplers(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteSamplers(" + arg0 + "," + arg1 + ")");
      gl.glDeleteSamplers(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteShader(int arg0) {
      System.out.println("glDeleteShader(" + arg0 + ")");
      gl.glDeleteShader(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteSync(long arg0) {
      System.out.println("glDeleteSync(" + arg0 + ")");
      gl.glDeleteSync(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteTextures(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glDeleteTextures(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteTextures(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteTextures(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteTextures(" + arg0 + "," + arg1 + ")");
      gl.glDeleteTextures(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteVertexArrays(int arg0, int[] arg1, int arg2) {
      System.out.println(
         "glDeleteVertexArrays(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteVertexArrays(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDeleteVertexArrays(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteVertexArrays(" + arg0 + "," + arg1 + ")");
      gl.glDeleteVertexArrays(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDepthFunc(int arg0) {
      System.out.println("glDepthFunc(" + arg0 + ")");
      gl.glDepthFunc(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDepthMask(boolean arg0) {
      System.out.println("glDepthMask(" + arg0 + ")");
      gl.glDepthMask(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDepthRange(double arg0, double arg1) {
      System.out.println("glDepthRange(" + arg0 + "," + arg1 + ")");
      gl.glDepthRange(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDepthRangef(float arg0, float arg1) {
      System.out.println("glDepthRangef(" + arg0 + "," + arg1 + ")");
      gl.glDepthRangef(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDetachShader(int arg0, int arg1) {
      System.out.println("glDetachShader(" + arg0 + "," + arg1 + ")");
      gl.glDetachShader(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDisable(int arg0) {
      System.out.println("glDisable(" + arg0 + ")");
      gl.glDisable(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDisableClientState(int arg0) {
      System.out.println("glDisableClientState(" + arg0 + ")");
      gl.glDisableClientState(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDisableVertexAttribArray(int arg0) {
      System.out.println("glDisableVertexAttribArray(" + arg0 + ")");
      gl.glDisableVertexAttribArray(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDisablei(int arg0, int arg1) {
      System.out.println("glDisablei(" + arg0 + "," + arg1 + ")");
      gl.glDisablei(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDrawArrays(int arg0, int arg1, int arg2) {
      System.out
      .println("glDrawArrays(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDrawArrays(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDrawArraysInstanced(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glDrawArraysInstanced(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glDrawArraysInstanced(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDrawBuffer(int arg0) {
      System.out.println("glDrawBuffer(" + arg0 + ")");
      gl.glDrawBuffer(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDrawBuffers(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glDrawBuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDrawBuffers(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDrawBuffers(int arg0, IntBuffer arg1) {
      System.out.println("glDrawBuffers(" + arg0 + "," + arg1 + ")");
      gl.glDrawBuffers(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDrawElements(int arg0, int arg1, int arg2, long arg3) {
      System.out.println(
         "glDrawElements(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glDrawElements(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDrawElementsBaseVertex(
      int arg0, int arg1, int arg2, long arg3, int arg4) {
      System.out.println(
         "glDrawElementsBaseVertex(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glDrawElementsBaseVertex(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDrawElementsInstanced(
      int arg0, int arg1, int arg2, long arg3, int arg4) {
      System.out.println(
         "glDrawElementsInstanced(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glDrawElementsInstanced(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDrawElementsInstancedBaseVertex(
      int arg0, int arg1, int arg2, long arg3, int arg4, int arg5) {
      System.out.println(
         "glDrawElementsInstancedBaseVertex(" + arg0 + "," + arg1 + "," + arg2
         + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glDrawElementsInstancedBaseVertex(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDrawRangeElements(
      int arg0, int arg1, int arg2, int arg3, int arg4, long arg5) {
      System.out.println(
         "glDrawRangeElements(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glDrawRangeElements(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glDrawRangeElementsBaseVertex(
      int arg0, int arg1, int arg2, int arg3, int arg4, long arg5, int arg6) {
      System.out.println(
         "glDrawRangeElementsBaseVertex(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glDrawRangeElementsBaseVertex(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glEdgeFlagFormatNV(int arg0) {
      System.out.println("glEdgeFlagFormatNV(" + arg0 + ")");
      gl.glEdgeFlagFormatNV(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glEnable(int arg0) {
      System.out.println("glEnable(" + arg0 + ")");
      gl.glEnable(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glEnableClientState(int arg0) {
      System.out.println("glEnableClientState(" + arg0 + ")");
      gl.glEnableClientState(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glEnableVertexAttribArray(int arg0) {
      System.out.println("glEnableVertexAttribArray(" + arg0 + ")");
      gl.glEnableVertexAttribArray(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glEnablei(int arg0, int arg1) {
      System.out.println("glEnablei(" + arg0 + "," + arg1 + ")");
      gl.glEnablei(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glEndConditionalRender() {
      System.out.println("glEndConditionalRender()");
      gl.glEndConditionalRender();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glEndQuery(int arg0) {
      System.out.println("glEndQuery(" + arg0 + ")");
      gl.glEndQuery(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glEndTransformFeedback() {
      System.out.println("glEndTransformFeedback()");
      gl.glEndTransformFeedback();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public long glFenceSync(int arg0, int arg1) {
      System.out.println("glFenceSync(" + arg0 + "," + arg1 + ")");
      long out = gl.glFenceSync(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glFinish() {
      System.out.println("glFinish()");
      gl.glFinish();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFlush() {
      System.out.println("glFlush()");
      gl.glFlush();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFlushMappedBufferRange(int arg0, long arg1, long arg2) {
      System.out.println(
         "glFlushMappedBufferRange(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glFlushMappedBufferRange(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFogCoordFormatNV(int arg0, int arg1) {
      System.out.println("glFogCoordFormatNV(" + arg0 + "," + arg1 + ")");
      gl.glFogCoordFormatNV(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFramebufferRenderbuffer(
      int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glFramebufferRenderbuffer(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glFramebufferRenderbuffer(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFramebufferTexture(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glFramebufferTexture(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glFramebufferTexture(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFramebufferTexture1D(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println(
         "glFramebufferTexture1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glFramebufferTexture1D(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFramebufferTexture2D(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println(
         "glFramebufferTexture2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glFramebufferTexture2D(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFramebufferTexture3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
      System.out.println(
         "glFramebufferTexture3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glFramebufferTexture3D(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFramebufferTextureARB(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glFramebufferTextureARB(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glFramebufferTextureARB(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFramebufferTextureFaceARB(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println(
         "glFramebufferTextureFaceARB(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glFramebufferTextureFaceARB(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFramebufferTextureLayer(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println(
         "glFramebufferTextureLayer(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glFramebufferTextureLayer(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFramebufferTextureLayerARB(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println(
         "glFramebufferTextureLayerARB(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glFramebufferTextureLayerARB(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glFrontFace(int arg0) {
      System.out.println("glFrontFace(" + arg0 + ")");
      gl.glFrontFace(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenBuffers(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glGenBuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenBuffers(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenBuffers(int arg0, IntBuffer arg1) {
      System.out.println("glGenBuffers(" + arg0 + "," + arg1 + ")");
      gl.glGenBuffers(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenFramebuffers(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glGenFramebuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenFramebuffers(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenFramebuffers(int arg0, IntBuffer arg1) {
      System.out.println("glGenFramebuffers(" + arg0 + "," + arg1 + ")");
      gl.glGenFramebuffers(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenQueries(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glGenQueries(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenQueries(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenQueries(int arg0, IntBuffer arg1) {
      System.out.println("glGenQueries(" + arg0 + "," + arg1 + ")");
      gl.glGenQueries(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenRenderbuffers(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glGenRenderbuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenRenderbuffers(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenRenderbuffers(int arg0, IntBuffer arg1) {
      System.out.println("glGenRenderbuffers(" + arg0 + "," + arg1 + ")");
      gl.glGenRenderbuffers(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenSamplers(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glGenSamplers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenSamplers(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenSamplers(int arg0, IntBuffer arg1) {
      System.out.println("glGenSamplers(" + arg0 + "," + arg1 + ")");
      gl.glGenSamplers(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenTextures(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glGenTextures(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenTextures(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenTextures(int arg0, IntBuffer arg1) {
      System.out.println("glGenTextures(" + arg0 + "," + arg1 + ")");
      gl.glGenTextures(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenVertexArrays(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glGenVertexArrays(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenVertexArrays(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenVertexArrays(int arg0, IntBuffer arg1) {
      System.out.println("glGenVertexArrays(" + arg0 + "," + arg1 + ")");
      gl.glGenVertexArrays(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGenerateMipmap(int arg0) {
      System.out.println("glGenerateMipmap(" + arg0 + ")");
      gl.glGenerateMipmap(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveAttrib(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, int[] arg5, int arg6,
      int[] arg7, int arg8, byte[] arg9, int arg10) {
      gl.glGetActiveAttrib(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveAttrib(
      int arg0, int arg1, int arg2, IntBuffer arg3, IntBuffer arg4,
      IntBuffer arg5, ByteBuffer arg6) {
      gl.glGetActiveAttrib(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveUniform(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, int[] arg5, int arg6,
      int[] arg7, int arg8, byte[] arg9, int arg10) {
      gl.glGetActiveUniform(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveUniform(
      int arg0, int arg1, int arg2, IntBuffer arg3, IntBuffer arg4,
      IntBuffer arg5, ByteBuffer arg6) {
      gl.glGetActiveUniform(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveUniformBlockName(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, byte[] arg5,
      int arg6) {
      gl.glGetActiveUniformBlockName(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveUniformBlockName(
      int arg0, int arg1, int arg2, IntBuffer arg3, ByteBuffer arg4) {
      System.out.println(
         "glGetActiveUniformBlockName(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glGetActiveUniformBlockName(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveUniformBlockiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      System.out.println(
         "glGetActiveUniformBlockiv(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glGetActiveUniformBlockiv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveUniformBlockiv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      System.out.println(
         "glGetActiveUniformBlockiv(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetActiveUniformBlockiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveUniformName(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, byte[] arg5,
      int arg6) {
      gl.glGetActiveUniformName(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveUniformName(
      int arg0, int arg1, int arg2, IntBuffer arg3, ByteBuffer arg4) {
      System.out.println(
         "glGetActiveUniformName(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glGetActiveUniformName(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveUniformsiv(
      int arg0, int arg1, int[] arg2, int arg3, int arg4, int[] arg5,
      int arg6) {
      gl.glGetActiveUniformsiv(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetActiveUniformsiv(
      int arg0, int arg1, IntBuffer arg2, int arg3, IntBuffer arg4) {
      System.out.println(
         "glGetActiveUniformsiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glGetActiveUniformsiv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetAttachedShaders(
      int arg0, int arg1, int[] arg2, int arg3, int[] arg4, int arg5) {
      System.out.println(
         "glGetAttachedShaders(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glGetAttachedShaders(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetAttachedShaders(
      int arg0, int arg1, IntBuffer arg2, IntBuffer arg3) {
      System.out.println(
         "glGetAttachedShaders(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetAttachedShaders(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public int glGetAttribLocation(int arg0, String arg1) {
      System.out.println("glGetAttribLocation(" + arg0 + "," + arg1 + ")");
      int out = gl.glGetAttribLocation(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glGetBooleani_v(int arg0, int arg1, byte[] arg2, int arg3) {
      System.out.println(
         "glGetBooleani_v(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetBooleani_v(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetBooleani_v(int arg0, int arg1, ByteBuffer arg2) {
      System.out
      .println("glGetBooleani_v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetBooleani_v(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetBooleanv(int arg0, byte[] arg1, int arg2) {
      System.out
      .println("glGetBooleanv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetBooleanv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetBooleanv(int arg0, ByteBuffer arg1) {
      System.out.println("glGetBooleanv(" + arg0 + "," + arg1 + ")");
      gl.glGetBooleanv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetBufferParameteri64v(
      int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println(
         "glGetBufferParameteri64v(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetBufferParameteri64v(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetBufferParameteri64v(int arg0, int arg1, LongBuffer arg2) {
      System.out.println(
         "glGetBufferParameteri64v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetBufferParameteri64v(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetBufferParameteriv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetBufferParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetBufferParameteriv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetBufferParameteriv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetBufferParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetBufferParameteriv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetBufferParameterui64vNV(
      int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println(
         "glGetBufferParameterui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetBufferParameterui64vNV(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetBufferParameterui64vNV(
      int arg0, int arg1, LongBuffer arg2) {
      System.out.println(
         "glGetBufferParameterui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetBufferParameterui64vNV(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetBufferSubData(int arg0, long arg1, long arg2, Buffer arg3) {
      System.out.println(
         "glGetBufferSubData(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetBufferSubData(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetCompressedTexImage(int arg0, int arg1, Buffer arg2) {
      System.out.println(
         "glGetCompressedTexImage(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetCompressedTexImage(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetCompressedTexImage(int arg0, int arg1, long arg2) {
      System.out.println(
         "glGetCompressedTexImage(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetCompressedTexImage(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public int glGetDebugMessageLog(
      int arg0, int arg1, int[] arg2, int arg3, int[] arg4, int arg5,
      int[] arg6, int arg7, int[] arg8, int arg9, int[] arg10, int arg11,
      byte[] arg12, int arg13) {
      int out =
         gl.glGetDebugMessageLog(
            arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
            arg11, arg12, arg13);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int glGetDebugMessageLog(
      int arg0, int arg1, IntBuffer arg2, IntBuffer arg3, IntBuffer arg4,
      IntBuffer arg5, IntBuffer arg6, ByteBuffer arg7) {
      int out =
         gl.glGetDebugMessageLog(
            arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int glGetDebugMessageLogAMD(
      int arg0, int arg1, int[] arg2, int arg3, int[] arg4, int arg5,
      int[] arg6, int arg7, int[] arg8, int arg9, byte[] arg10, int arg11) {
      int out =
         gl.glGetDebugMessageLogAMD(
            arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
            arg11);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int glGetDebugMessageLogAMD(
      int arg0, int arg1, IntBuffer arg2, IntBuffer arg3, IntBuffer arg4,
      IntBuffer arg5, ByteBuffer arg6) {
      int out =
         gl.glGetDebugMessageLogAMD(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glGetDoublev(int arg0, double[] arg1, int arg2) {
      System.out
      .println("glGetDoublev(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetDoublev(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetDoublev(int arg0, DoubleBuffer arg1) {
      System.out.println("glGetDoublev(" + arg0 + "," + arg1 + ")");
      gl.glGetDoublev(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public int glGetError() {
      System.out.println("glGetError()");
      int out = gl.glGetError();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glGetFloatv(int arg0, float[] arg1, int arg2) {
      System.out.println("glGetFloatv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetFloatv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetFloatv(int arg0, FloatBuffer arg1) {
      System.out.println("glGetFloatv(" + arg0 + "," + arg1 + ")");
      gl.glGetFloatv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public int glGetFragDataIndex(int arg0, String arg1) {
      System.out.println("glGetFragDataIndex(" + arg0 + "," + arg1 + ")");
      int out = gl.glGetFragDataIndex(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public int glGetFragDataLocation(int arg0, String arg1) {
      System.out.println("glGetFragDataLocation(" + arg0 + "," + arg1 + ")");
      int out = gl.glGetFragDataLocation(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glGetFramebufferAttachmentParameteriv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      System.out.println(
         "glGetFramebufferAttachmentParameteriv(" + arg0 + "," + arg1 + ","
            + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetFramebufferAttachmentParameteriv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetFramebufferAttachmentParameteriv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      System.out.println(
         "glGetFramebufferAttachmentParameteriv(" + arg0 + "," + arg1 + ","
            + arg2 + "," + arg3 + ")");
      gl.glGetFramebufferAttachmentParameteriv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public int glGetGraphicsResetStatus() {
      System.out.println("glGetGraphicsResetStatus()");
      int out = gl.glGetGraphicsResetStatus();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glGetInteger64i_v(int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println(
         "glGetInteger64i_v(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetInteger64i_v(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetInteger64i_v(int arg0, int arg1, LongBuffer arg2) {
      System.out
      .println("glGetInteger64i_v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetInteger64i_v(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetInteger64v(int arg0, long[] arg1, int arg2) {
      System.out
      .println("glGetInteger64v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetInteger64v(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetInteger64v(int arg0, LongBuffer arg1) {
      System.out.println("glGetInteger64v(" + arg0 + "," + arg1 + ")");
      gl.glGetInteger64v(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetIntegeri_v(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetIntegeri_v(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetIntegeri_v(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetIntegeri_v(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glGetIntegeri_v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetIntegeri_v(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetIntegerui64i_vNV(
      int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println(
         "glGetIntegerui64i_vNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetIntegerui64i_vNV(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetIntegerui64i_vNV(int arg0, int arg1, LongBuffer arg2) {
      System.out.println(
         "glGetIntegerui64i_vNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetIntegerui64i_vNV(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetIntegerui64vNV(int arg0, long[] arg1, int arg2) {
      System.out.println(
         "glGetIntegerui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetIntegerui64vNV(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetIntegerui64vNV(int arg0, LongBuffer arg1) {
      System.out.println("glGetIntegerui64vNV(" + arg0 + "," + arg1 + ")");
      gl.glGetIntegerui64vNV(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetIntegerv(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glGetIntegerv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetIntegerv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetIntegerv(int arg0, IntBuffer arg1) {
      System.out.println("glGetIntegerv(" + arg0 + "," + arg1 + ")");
      gl.glGetIntegerv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetInternalformativ(
      int arg0, int arg1, int arg2, int arg3, int[] arg4, int arg5) {
      System.out.println(
         "glGetInternalformativ(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glGetInternalformativ(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetInternalformativ(
      int arg0, int arg1, int arg2, int arg3, IntBuffer arg4) {
      System.out.println(
         "glGetInternalformativ(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glGetInternalformativ(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetMultisamplefv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glGetMultisamplefv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetMultisamplefv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetMultisamplefv(int arg0, int arg1, FloatBuffer arg2) {
      System.out
      .println("glGetMultisamplefv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetMultisamplefv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetNamedBufferParameterui64vNV(
      int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println(
         "glGetNamedBufferParameterui64vNV(" + arg0 + "," + arg1 + "," + arg2
         + "," + arg3 + ")");
      gl.glGetNamedBufferParameterui64vNV(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetNamedBufferParameterui64vNV(
      int arg0, int arg1, LongBuffer arg2) {
      System.out.println(
         "glGetNamedBufferParameterui64vNV(" + arg0 + "," + arg1 + "," + arg2
         + ")");
      gl.glGetNamedBufferParameterui64vNV(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetNamedFramebufferParameterivEXT(
      int arg0, int arg1, int[] arg2, int arg3) {
      // XXX not implemented
   }

   public void glGetNamedFramebufferParameterivEXT(
      int arg0, int arg1, IntBuffer arg2) {
      // XXX not implemented
   }

   public void glGetNamedStringARB(
      int arg0, String arg1, int arg2, int[] arg3, int arg4, byte[] arg5,
      int arg6) {
      gl.glGetNamedStringARB(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetNamedStringARB(
      int arg0, String arg1, int arg2, IntBuffer arg3, ByteBuffer arg4) {
      System.out.println(
         "glGetNamedStringARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glGetNamedStringARB(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetNamedStringivARB(
      int arg0, String arg1, int arg2, int[] arg3, int arg4) {
      System.out.println(
         "glGetNamedStringivARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glGetNamedStringivARB(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetNamedStringivARB(
      int arg0, String arg1, int arg2, IntBuffer arg3) {
      System.out.println(
         "glGetNamedStringivARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetNamedStringivARB(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetObjectLabel(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, byte[] arg5,
      int arg6) {
      gl.glGetObjectLabel(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetObjectLabel(
      int arg0, int arg1, int arg2, IntBuffer arg3, ByteBuffer arg4) {
      System.out.println(
         "glGetObjectLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glGetObjectLabel(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetObjectPtrLabel(
      Buffer arg0, int arg1, int[] arg2, int arg3, byte[] arg4, int arg5) {
      System.out.println(
         "glGetObjectPtrLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glGetObjectPtrLabel(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetObjectPtrLabel(
      Buffer arg0, int arg1, IntBuffer arg2, ByteBuffer arg3) {
      System.out.println(
         "glGetObjectPtrLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetObjectPtrLabel(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetProgramBinary(
      int arg0, int arg1, int[] arg2, int arg3, int[] arg4, int arg5,
      Buffer arg6) {
      gl.glGetProgramBinary(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetProgramBinary(
      int arg0, int arg1, IntBuffer arg2, IntBuffer arg3, Buffer arg4) {
      System.out.println(
         "glGetProgramBinary(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glGetProgramBinary(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetProgramInfoLog(
      int arg0, int arg1, int[] arg2, int arg3, byte[] arg4, int arg5) {
      System.out.println(
         "glGetProgramInfoLog(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glGetProgramInfoLog(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetProgramInfoLog(
      int arg0, int arg1, IntBuffer arg2, ByteBuffer arg3) {
      System.out.println(
         "glGetProgramInfoLog(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetProgramInfoLog(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetProgramiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetProgramiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetProgramiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetProgramiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glGetProgramiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetProgramiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetQueryObjecti64v(int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println(
         "glGetQueryObjecti64v(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetQueryObjecti64v(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetQueryObjecti64v(int arg0, int arg1, LongBuffer arg2) {
      System.out.println(
         "glGetQueryObjecti64v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetQueryObjecti64v(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetQueryObjectiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetQueryObjectiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetQueryObjectiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetQueryObjectiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glGetQueryObjectiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetQueryObjectiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetQueryObjectui64v(
      int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println(
         "glGetQueryObjectui64v(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetQueryObjectui64v(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetQueryObjectui64v(int arg0, int arg1, LongBuffer arg2) {
      System.out.println(
         "glGetQueryObjectui64v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetQueryObjectui64v(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetQueryObjectuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetQueryObjectuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetQueryObjectuiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetQueryObjectuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetQueryObjectuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetQueryObjectuiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetQueryiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetQueryiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetQueryiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetQueryiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glGetQueryiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetQueryiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetRenderbufferParameteriv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetRenderbufferParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetRenderbufferParameteriv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetRenderbufferParameteriv(
      int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetRenderbufferParameteriv(" + arg0 + "," + arg1 + "," + arg2
         + ")");
      gl.glGetRenderbufferParameteriv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetSamplerParameterIiv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetSamplerParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetSamplerParameterIiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetSamplerParameterIiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetSamplerParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetSamplerParameterIiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetSamplerParameterIuiv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetSamplerParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetSamplerParameterIuiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetSamplerParameterIuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetSamplerParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetSamplerParameterIuiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetSamplerParameterfv(
      int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glGetSamplerParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetSamplerParameterfv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetSamplerParameterfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println(
         "glGetSamplerParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetSamplerParameterfv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetSamplerParameteriv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetSamplerParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetSamplerParameteriv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetSamplerParameteriv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetSamplerParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetSamplerParameteriv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetShaderInfoLog(
      int arg0, int arg1, int[] arg2, int arg3, byte[] arg4, int arg5) {
      System.out.println(
         "glGetShaderInfoLog(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glGetShaderInfoLog(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetShaderInfoLog(
      int arg0, int arg1, IntBuffer arg2, ByteBuffer arg3) {
      System.out.println(
         "glGetShaderInfoLog(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetShaderInfoLog(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetShaderPrecisionFormat(
      int arg0, int arg1, int[] arg2, int arg3, int[] arg4, int arg5) {
      System.out.println(
         "glGetShaderPrecisionFormat(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glGetShaderPrecisionFormat(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetShaderPrecisionFormat(
      int arg0, int arg1, IntBuffer arg2, IntBuffer arg3) {
      System.out.println(
         "glGetShaderPrecisionFormat(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetShaderPrecisionFormat(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetShaderSource(
      int arg0, int arg1, int[] arg2, int arg3, byte[] arg4, int arg5) {
      System.out.println(
         "glGetShaderSource(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glGetShaderSource(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetShaderSource(
      int arg0, int arg1, IntBuffer arg2, ByteBuffer arg3) {
      System.out.println(
         "glGetShaderSource(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetShaderSource(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetShaderiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetShaderiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetShaderiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetShaderiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glGetShaderiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetShaderiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public String glGetString(int arg0) {
      System.out.println("glGetString(" + arg0 + ")");
      String out = gl.glGetString(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public String glGetStringi(int arg0, int arg1) {
      System.out.println("glGetStringi(" + arg0 + "," + arg1 + ")");
      String out = gl.glGetStringi(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glGetSynciv(
      long arg0, int arg1, int arg2, int[] arg3, int arg4, int[] arg5,
      int arg6) {
      gl.glGetSynciv(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetSynciv(
      long arg0, int arg1, int arg2, IntBuffer arg3, IntBuffer arg4) {
      System.out.println(
         "glGetSynciv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glGetSynciv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexImage(
      int arg0, int arg1, int arg2, int arg3, Buffer arg4) {
      System.out.println(
         "glGetTexImage(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glGetTexImage(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexImage(
      int arg0, int arg1, int arg2, int arg3, long arg4) {
      System.out.println(
         "glGetTexImage(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glGetTexImage(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexLevelParameterfv(
      int arg0, int arg1, int arg2, float[] arg3, int arg4) {
      System.out.println(
         "glGetTexLevelParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glGetTexLevelParameterfv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexLevelParameterfv(
      int arg0, int arg1, int arg2, FloatBuffer arg3) {
      System.out.println(
         "glGetTexLevelParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetTexLevelParameterfv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexLevelParameteriv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      System.out.println(
         "glGetTexLevelParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glGetTexLevelParameteriv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexLevelParameteriv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      System.out.println(
         "glGetTexLevelParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetTexLevelParameteriv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexParameterIiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetTexParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetTexParameterIiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexParameterIiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetTexParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetTexParameterIiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexParameterIuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetTexParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetTexParameterIuiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexParameterIuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetTexParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetTexParameterIuiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexParameterfv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glGetTexParameterfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetTexParameterfv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexParameterfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println(
         "glGetTexParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetTexParameterfv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexParameteriv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetTexParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetTexParameteriv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTexParameteriv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetTexParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetTexParameteriv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTransformFeedbackVarying(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, int[] arg5, int arg6,
      int[] arg7, int arg8, byte[] arg9, int arg10) {
      gl.glGetTransformFeedbackVarying(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetTransformFeedbackVarying(
      int arg0, int arg1, int arg2, IntBuffer arg3, IntBuffer arg4,
      IntBuffer arg5, ByteBuffer arg6) {
      gl.glGetTransformFeedbackVarying(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public int glGetUniformBlockIndex(int arg0, String arg1) {
      System.out.println("glGetUniformBlockIndex(" + arg0 + "," + arg1 + ")");
      int out = gl.glGetUniformBlockIndex(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glGetUniformIndices(
      int arg0, int arg1, String[] arg2, int[] arg3, int arg4) {
      System.out.println(
         "glGetUniformIndices(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glGetUniformIndices(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetUniformIndices(
      int arg0, int arg1, String[] arg2, IntBuffer arg3) {
      System.out.println(
         "glGetUniformIndices(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetUniformIndices(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public int glGetUniformLocation(int arg0, String arg1) {
      System.out.println("glGetUniformLocation(" + arg0 + "," + arg1 + ")");
      int out = gl.glGetUniformLocation(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glGetUniformfv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glGetUniformfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetUniformfv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetUniformfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out
      .println("glGetUniformfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetUniformfv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetUniformiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetUniformiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetUniformiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetUniformiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glGetUniformiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetUniformiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetUniformui64vNV(int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println(
         "glGetUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetUniformui64vNV(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetUniformui64vNV(int arg0, int arg1, LongBuffer arg2) {
      System.out.println(
         "glGetUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetUniformui64vNV(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetUniformuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetUniformuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetUniformuiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetUniformuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glGetUniformuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetUniformuiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetVertexAttribIiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetVertexAttribIiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetVertexAttribIiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetVertexAttribIiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetVertexAttribIiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetVertexAttribIiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetVertexAttribIuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetVertexAttribIuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetVertexAttribIuiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetVertexAttribIuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetVertexAttribIuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetVertexAttribIuiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetVertexAttribdv(
      int arg0, int arg1, double[] arg2, int arg3) {
      System.out.println(
         "glGetVertexAttribdv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetVertexAttribdv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetVertexAttribdv(int arg0, int arg1, DoubleBuffer arg2) {
      System.out.println(
         "glGetVertexAttribdv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetVertexAttribdv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetVertexAttribfv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glGetVertexAttribfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetVertexAttribfv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetVertexAttribfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println(
         "glGetVertexAttribfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetVertexAttribfv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetVertexAttribiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glGetVertexAttribiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetVertexAttribiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetVertexAttribiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glGetVertexAttribiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetVertexAttribiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetnCompressedTexImage(
      int arg0, int arg1, int arg2, Buffer arg3) {
      System.out.println(
         "glGetnCompressedTexImage(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glGetnCompressedTexImage(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetnTexImage(
      int arg0, int arg1, int arg2, int arg3, int arg4, Buffer arg5) {
      System.out.println(
         "glGetnTexImage(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + "," + arg5 + ")");
      gl.glGetnTexImage(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetnUniformdv(
      int arg0, int arg1, int arg2, double[] arg3, int arg4) {
      System.out.println(
         "glGetnUniformdv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glGetnUniformdv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetnUniformdv(
      int arg0, int arg1, int arg2, DoubleBuffer arg3) {
      System.out.println(
         "glGetnUniformdv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetnUniformdv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetnUniformfv(
      int arg0, int arg1, int arg2, float[] arg3, int arg4) {
      System.out.println(
         "glGetnUniformfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glGetnUniformfv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetnUniformfv(int arg0, int arg1, int arg2, FloatBuffer arg3) {
      System.out.println(
         "glGetnUniformfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetnUniformfv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetnUniformiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      System.out.println(
         "glGetnUniformiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glGetnUniformiv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetnUniformiv(int arg0, int arg1, int arg2, IntBuffer arg3) {
      System.out.println(
         "glGetnUniformiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetnUniformiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetnUniformuiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      System.out.println(
         "glGetnUniformuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glGetnUniformuiv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glGetnUniformuiv(int arg0, int arg1, int arg2, IntBuffer arg3) {
      System.out.println(
         "glGetnUniformuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glGetnUniformuiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glHint(int arg0, int arg1) {
      System.out.println("glHint(" + arg0 + "," + arg1 + ")");
      gl.glHint(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public long glImportSyncEXT(int arg0, long arg1, int arg2) {
      System.out
      .println("glImportSyncEXT(" + arg0 + "," + arg1 + "," + arg2 + ")");
      long out = gl.glImportSyncEXT(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glIndexFormatNV(int arg0, int arg1) {
      System.out.println("glIndexFormatNV(" + arg0 + "," + arg1 + ")");
      gl.glIndexFormatNV(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public boolean glIsBuffer(int arg0) {
      System.out.println("glIsBuffer(" + arg0 + ")");
      boolean out = gl.glIsBuffer(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsBufferResidentNV(int arg0) {
      System.out.println("glIsBufferResidentNV(" + arg0 + ")");
      boolean out = gl.glIsBufferResidentNV(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsEnabled(int arg0) {
      System.out.println("glIsEnabled(" + arg0 + ")");
      boolean out = gl.glIsEnabled(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsEnabledi(int arg0, int arg1) {
      System.out.println("glIsEnabledi(" + arg0 + "," + arg1 + ")");
      boolean out = gl.glIsEnabledi(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsFramebuffer(int arg0) {
      System.out.println("glIsFramebuffer(" + arg0 + ")");
      boolean out = gl.glIsFramebuffer(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsNamedBufferResidentNV(int arg0) {
      System.out.println("glIsNamedBufferResidentNV(" + arg0 + ")");
      boolean out = gl.glIsNamedBufferResidentNV(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsNamedStringARB(int arg0, String arg1) {
      System.out.println("glIsNamedStringARB(" + arg0 + "," + arg1 + ")");
      boolean out = gl.glIsNamedStringARB(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsProgram(int arg0) {
      System.out.println("glIsProgram(" + arg0 + ")");
      boolean out = gl.glIsProgram(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsQuery(int arg0) {
      System.out.println("glIsQuery(" + arg0 + ")");
      boolean out = gl.glIsQuery(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsRenderbuffer(int arg0) {
      System.out.println("glIsRenderbuffer(" + arg0 + ")");
      boolean out = gl.glIsRenderbuffer(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsSampler(int arg0) {
      System.out.println("glIsSampler(" + arg0 + ")");
      boolean out = gl.glIsSampler(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsShader(int arg0) {
      System.out.println("glIsShader(" + arg0 + ")");
      boolean out = gl.glIsShader(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsSync(long arg0) {
      System.out.println("glIsSync(" + arg0 + ")");
      boolean out = gl.glIsSync(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsTexture(int arg0) {
      System.out.println("glIsTexture(" + arg0 + ")");
      boolean out = gl.glIsTexture(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsVertexArray(int arg0) {
      System.out.println("glIsVertexArray(" + arg0 + ")");
      boolean out = gl.glIsVertexArray(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glLineWidth(float arg0) {
      System.out.println("glLineWidth(" + arg0 + ")");
      gl.glLineWidth(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glLinkProgram(int arg0) {
      System.out.println("glLinkProgram(" + arg0 + ")");
      gl.glLinkProgram(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glLogicOp(int arg0) {
      System.out.println("glLogicOp(" + arg0 + ")");
      gl.glLogicOp(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glMakeBufferNonResidentNV(int arg0) {
      System.out.println("glMakeBufferNonResidentNV(" + arg0 + ")");
      gl.glMakeBufferNonResidentNV(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glMakeBufferResidentNV(int arg0, int arg1) {
      System.out.println("glMakeBufferResidentNV(" + arg0 + "," + arg1 + ")");
      gl.glMakeBufferResidentNV(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glMakeNamedBufferNonResidentNV(int arg0) {
      System.out.println("glMakeNamedBufferNonResidentNV(" + arg0 + ")");
      gl.glMakeNamedBufferNonResidentNV(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glMakeNamedBufferResidentNV(int arg0, int arg1) {
      System.out
      .println("glMakeNamedBufferResidentNV(" + arg0 + "," + arg1 + ")");
      gl.glMakeNamedBufferResidentNV(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public ByteBuffer glMapBuffer(int arg0, int arg1) {
      System.out.println("glMapBuffer(" + arg0 + "," + arg1 + ")");
      ByteBuffer out = gl.glMapBuffer(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public ByteBuffer glMapBufferRange(
      int arg0, long arg1, long arg2, int arg3) {
      System.out.println(
         "glMapBufferRange(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      ByteBuffer out = gl.glMapBufferRange(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glMultiDrawArrays(
      int arg0, int[] arg1, int arg2, int[] arg3, int arg4, int arg5) {
      System.out.println(
         "glMultiDrawArrays(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glMultiDrawArrays(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glMultiDrawArrays(
      int arg0, IntBuffer arg1, IntBuffer arg2, int arg3) {
      System.out.println(
         "glMultiDrawArrays(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glMultiDrawArrays(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glMultiDrawArraysIndirectAMD(
      int arg0, Buffer arg1, int arg2, int arg3) {
      System.out.println(
         "glMultiDrawArraysIndirectAMD(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glMultiDrawArraysIndirectAMD(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glMultiDrawElements(
      int arg0, IntBuffer arg1, int arg2, PointerBuffer arg3, int arg4) {
      System.out.println(
         "glMultiDrawElements(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glMultiDrawElements(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glMultiDrawElementsBaseVertex(
      int arg0, IntBuffer arg1, int arg2, PointerBuffer arg3, int arg4,
      IntBuffer arg5) {
      gl.glMultiDrawElementsBaseVertex(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glMultiDrawElementsIndirectAMD(
      int arg0, int arg1, Buffer arg2, int arg3, int arg4) {
      System.out.println(
         "glMultiDrawElementsIndirectAMD(" + arg0 + "," + arg1 + "," + arg2
         + "," + arg3 + "," + arg4 + ")");
      gl.glMultiDrawElementsIndirectAMD(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glMultiTexCoordP1ui(int arg0, int arg1, int arg2) {
      // XXX not implemented
   }

   public void glMultiTexCoordP1uiv(int arg0, int arg1, int[] arg2, int arg3) {
      // XXX not implemented
   }

   public void glMultiTexCoordP1uiv(int arg0, int arg1, IntBuffer arg2) {
      // XXX not implemented
   }

   public void glMultiTexCoordP2ui(int arg0, int arg1, int arg2) {
      // XXX not implemented
   }

   public void glMultiTexCoordP2uiv(int arg0, int arg1, int[] arg2, int arg3) {
      // XXX not implemented
   }

   public void glMultiTexCoordP2uiv(int arg0, int arg1, IntBuffer arg2) {
      // XXX not implemented
   }

   public void glMultiTexCoordP3ui(int arg0, int arg1, int arg2) {
      // XXX not implemented
   }

   public void glMultiTexCoordP3uiv(int arg0, int arg1, int[] arg2, int arg3) {
      // XXX not implemented
   }

   public void glMultiTexCoordP3uiv(int arg0, int arg1, IntBuffer arg2) {
      // XXX not implemented
   }

   public void glMultiTexCoordP4ui(int arg0, int arg1, int arg2) {
      // XXX not implemented
   }

   public void glMultiTexCoordP4uiv(int arg0, int arg1, int[] arg2, int arg3) {
      // XXX not implemented
   }

   public void glMultiTexCoordP4uiv(int arg0, int arg1, IntBuffer arg2) {
      // XXX not implemented
   }

   public void glNamedFramebufferParameteriEXT(int arg0, int arg1, int arg2) {
      // XXX not implemented
   }

   public void glNamedStringARB(
      int arg0, int arg1, String arg2, int arg3, String arg4) {
      System.out.println(
         "glNamedStringARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glNamedStringARB(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glNormalFormatNV(int arg0, int arg1) {
      System.out.println("glNormalFormatNV(" + arg0 + "," + arg1 + ")");
      gl.glNormalFormatNV(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glNormalP3ui(int arg0, int arg1) {
      // XXX not implemented
   }

   public void glNormalP3uiv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented
   }

   public void glNormalP3uiv(int arg0, IntBuffer arg1) {
      // XXX not implemented
   }

   public void glObjectLabel(
      int arg0, int arg1, int arg2, byte[] arg3, int arg4) {
      System.out.println(
         "glObjectLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glObjectLabel(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glObjectLabel(int arg0, int arg1, int arg2, ByteBuffer arg3) {
      System.out.println(
         "glObjectLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glObjectLabel(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glObjectPtrLabel(Buffer arg0, int arg1, byte[] arg2, int arg3) {
      System.out.println(
         "glObjectPtrLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glObjectPtrLabel(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glObjectPtrLabel(Buffer arg0, int arg1, ByteBuffer arg2) {
      System.out
      .println("glObjectPtrLabel(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glObjectPtrLabel(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPixelStoref(int arg0, float arg1) {
      System.out.println("glPixelStoref(" + arg0 + "," + arg1 + ")");
      gl.glPixelStoref(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPixelStorei(int arg0, int arg1) {
      System.out.println("glPixelStorei(" + arg0 + "," + arg1 + ")");
      gl.glPixelStorei(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPointParameterf(int arg0, float arg1) {
      System.out.println("glPointParameterf(" + arg0 + "," + arg1 + ")");
      gl.glPointParameterf(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPointParameterfv(int arg0, float[] arg1, int arg2) {
      System.out
      .println("glPointParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glPointParameterfv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPointParameterfv(int arg0, FloatBuffer arg1) {
      System.out.println("glPointParameterfv(" + arg0 + "," + arg1 + ")");
      gl.glPointParameterfv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPointParameteri(int arg0, int arg1) {
      System.out.println("glPointParameteri(" + arg0 + "," + arg1 + ")");
      gl.glPointParameteri(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPointParameteriv(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glPointParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glPointParameteriv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPointParameteriv(int arg0, IntBuffer arg1) {
      System.out.println("glPointParameteriv(" + arg0 + "," + arg1 + ")");
      gl.glPointParameteriv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPointSize(float arg0) {
      System.out.println("glPointSize(" + arg0 + ")");
      gl.glPointSize(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPolygonMode(int arg0, int arg1) {
      System.out.println("glPolygonMode(" + arg0 + "," + arg1 + ")");
      gl.glPolygonMode(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPolygonOffset(float arg0, float arg1) {
      System.out.println("glPolygonOffset(" + arg0 + "," + arg1 + ")");
      gl.glPolygonOffset(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPopDebugGroup() {
      System.out.println("glPopDebugGroup()");
      gl.glPopDebugGroup();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPrimitiveRestartIndex(int arg0) {
      System.out.println("glPrimitiveRestartIndex(" + arg0 + ")");
      gl.glPrimitiveRestartIndex(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glProgramBinary(int arg0, int arg1, Buffer arg2, int arg3) {
      System.out.println(
         "glProgramBinary(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glProgramBinary(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glProgramParameteriARB(int arg0, int arg1, int arg2) {
      System.out.println(
         "glProgramParameteriARB(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glProgramParameteriARB(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glProgramUniformui64NV(int arg0, int arg1, long arg2) {
      System.out.println(
         "glProgramUniformui64NV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glProgramUniformui64NV(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glProgramUniformui64vNV(
      int arg0, int arg1, int arg2, long[] arg3, int arg4) {
      System.out.println(
         "glProgramUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + ")");
      gl.glProgramUniformui64vNV(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glProgramUniformui64vNV(
      int arg0, int arg1, int arg2, LongBuffer arg3) {
      System.out.println(
         "glProgramUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glProgramUniformui64vNV(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glProvokingVertex(int arg0) {
      System.out.println("glProvokingVertex(" + arg0 + ")");
      gl.glProvokingVertex(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPushDebugGroup(
      int arg0, int arg1, int arg2, byte[] arg3, int arg4) {
      System.out.println(
         "glPushDebugGroup(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glPushDebugGroup(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glPushDebugGroup(int arg0, int arg1, int arg2, ByteBuffer arg3) {
      System.out.println(
         "glPushDebugGroup(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glPushDebugGroup(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glQueryCounter(int arg0, int arg1) {
      System.out.println("glQueryCounter(" + arg0 + "," + arg1 + ")");
      gl.glQueryCounter(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glReadBuffer(int arg0) {
      System.out.println("glReadBuffer(" + arg0 + ")");
      gl.glReadBuffer(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glReadPixels(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, Buffer arg6) {
      System.out.println(
         "glReadPixels(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glReadPixels(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glReadPixels(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, long arg6) {
      System.out.println(
         "glReadPixels(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glReadPixels(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glReadnPixels(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      Buffer arg7) {
      gl.glReadnPixels(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glReleaseShaderCompiler() {
      System.out.println("glReleaseShaderCompiler()");
      gl.glReleaseShaderCompiler();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glRenderbufferStorage(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glRenderbufferStorage(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glRenderbufferStorage(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glRenderbufferStorageMultisample(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println(
         "glRenderbufferStorageMultisample(" + arg0 + "," + arg1 + "," + arg2
         + "," + arg3 + "," + arg4 + ")");
      gl.glRenderbufferStorageMultisample(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSampleCoverage(float arg0, boolean arg1) {
      System.out.println("glSampleCoverage(" + arg0 + "," + arg1 + ")");
      gl.glSampleCoverage(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSampleMaski(int arg0, int arg1) {
      System.out.println("glSampleMaski(" + arg0 + "," + arg1 + ")");
      gl.glSampleMaski(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSamplerParameterIiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glSamplerParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glSamplerParameterIiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSamplerParameterIiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glSamplerParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameterIiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSamplerParameterIuiv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glSamplerParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glSamplerParameterIuiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSamplerParameterIuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glSamplerParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameterIuiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSamplerParameterf(int arg0, int arg1, float arg2) {
      System.out.println(
         "glSamplerParameterf(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameterf(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSamplerParameterfv(
      int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glSamplerParameterfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glSamplerParameterfv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSamplerParameterfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println(
         "glSamplerParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameterfv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSamplerParameteri(int arg0, int arg1, int arg2) {
      System.out.println(
         "glSamplerParameteri(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameteri(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSamplerParameteriv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glSamplerParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glSamplerParameteriv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSamplerParameteriv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println(
         "glSamplerParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameteriv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glScissor(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glScissor(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glScissor(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSecondaryColorFormatNV(int arg0, int arg1, int arg2) {
      System.out.println(
         "glSecondaryColorFormatNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSecondaryColorFormatNV(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSecondaryColorP3ui(int arg0, int arg1) {
      // XXX not implemented
   }

   public void glSecondaryColorP3uiv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented
   }

   public void glSecondaryColorP3uiv(int arg0, IntBuffer arg1) {
      // XXX not implemented
   }

   public void glSetMultisamplefvAMD(
      int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glSetMultisamplefvAMD(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glSetMultisamplefvAMD(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glSetMultisamplefvAMD(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println(
         "glSetMultisamplefvAMD(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSetMultisamplefvAMD(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glShaderBinary(
      int arg0, int[] arg1, int arg2, int arg3, Buffer arg4, int arg5) {
      System.out.println(
         "glShaderBinary(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + "," + arg5 + ")");
      gl.glShaderBinary(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glShaderBinary(
      int arg0, IntBuffer arg1, int arg2, Buffer arg3, int arg4) {
      System.out.println(
         "glShaderBinary(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glShaderBinary(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glShaderSource(
      int arg0, int arg1, String[] arg2, int[] arg3, int arg4) {
      System.out.println(
         "glShaderSource(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glShaderSource(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glShaderSource(
      int arg0, int arg1, String[] arg2, IntBuffer arg3) {
      System.out.println(
         "glShaderSource(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glShaderSource(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glStencilFunc(int arg0, int arg1, int arg2) {
      System.out
      .println("glStencilFunc(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glStencilFunc(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glStencilFuncSeparate(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glStencilFuncSeparate(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glStencilFuncSeparate(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glStencilMask(int arg0) {
      System.out.println("glStencilMask(" + arg0 + ")");
      gl.glStencilMask(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glStencilMaskSeparate(int arg0, int arg1) {
      System.out.println("glStencilMaskSeparate(" + arg0 + "," + arg1 + ")");
      gl.glStencilMaskSeparate(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glStencilOp(int arg0, int arg1, int arg2) {
      System.out.println("glStencilOp(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glStencilOp(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glStencilOpSeparate(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glStencilOpSeparate(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glStencilOpSeparate(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glStencilOpValueAMD(int arg0, int arg1) {
      System.out.println("glStencilOpValueAMD(" + arg0 + "," + arg1 + ")");
      gl.glStencilOpValueAMD(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTessellationFactorAMD(float arg0) {
      System.out.println("glTessellationFactorAMD(" + arg0 + ")");
      gl.glTessellationFactorAMD(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTessellationModeAMD(int arg0) {
      System.out.println("glTessellationModeAMD(" + arg0 + ")");
      gl.glTessellationModeAMD(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexBuffer(int arg0, int arg1, int arg2) {
      System.out.println("glTexBuffer(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexBuffer(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexCoordFormatNV(int arg0, int arg1, int arg2) {
      System.out
      .println("glTexCoordFormatNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexCoordFormatNV(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexCoordP1ui(int arg0, int arg1) {
      // XXX not implemented
   }

   public void glTexCoordP1uiv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented
   }

   public void glTexCoordP1uiv(int arg0, IntBuffer arg1) {
      // XXX not implemented
   }

   public void glTexCoordP2ui(int arg0, int arg1) {
      // XXX not implemented
   }

   public void glTexCoordP2uiv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented
   }

   public void glTexCoordP2uiv(int arg0, IntBuffer arg1) {
      // XXX not implemented
   }

   public void glTexCoordP3ui(int arg0, int arg1) {
      // XXX not implemented
   }

   public void glTexCoordP3uiv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented
   }

   public void glTexCoordP3uiv(int arg0, IntBuffer arg1) {
      // XXX not implemented
   }

   public void glTexCoordP4ui(int arg0, int arg1) {
      // XXX not implemented
   }

   public void glTexCoordP4uiv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented
   }

   public void glTexCoordP4uiv(int arg0, IntBuffer arg1) {
      // XXX not implemented
   }

   public void glTexImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      Buffer arg7) {
      gl.glTexImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      long arg7) {
      gl.glTexImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, Buffer arg8) {
      gl.glTexImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, long arg8) {
      gl.glTexImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexImage2DMultisample(
      int arg0, int arg1, int arg2, int arg3, int arg4, boolean arg5) {
      System.out.println(
         "glTexImage2DMultisample(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glTexImage2DMultisample(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexImage2DMultisampleCoverageNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5,
      boolean arg6) {
      gl.glTexImage2DMultisampleCoverageNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, Buffer arg9) {
      gl.glTexImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, long arg9) {
      gl.glTexImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexImage3DMultisample(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5,
      boolean arg6) {
      gl.glTexImage3DMultisample(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexImage3DMultisampleCoverageNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      boolean arg7) {
      gl.glTexImage3DMultisampleCoverageNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexParameterIiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glTexParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glTexParameterIiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexParameterIiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glTexParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameterIiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexParameterIuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glTexParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glTexParameterIuiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexParameterIuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glTexParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameterIuiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexParameterf(int arg0, int arg1, float arg2) {
      System.out
      .println("glTexParameterf(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameterf(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexParameterfv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glTexParameterfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glTexParameterfv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexParameterfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out
      .println("glTexParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameterfv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexParameteri(int arg0, int arg1, int arg2) {
      System.out
      .println("glTexParameteri(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameteri(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexParameteriv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glTexParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glTexParameteriv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexParameteriv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glTexParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameteriv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexStorage1D(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glTexStorage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glTexStorage1D(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexStorage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println(
         "glTexStorage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glTexStorage2D(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexStorage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
      System.out.println(
         "glTexStorage3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + "," + arg5 + ")");
      gl.glTexStorage3D(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexSubImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, Buffer arg6) {
      System.out.println(
         "glTexSubImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glTexSubImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexSubImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, long arg6) {
      System.out.println(
         "glTexSubImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glTexSubImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexSubImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, Buffer arg8) {
      gl.glTexSubImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexSubImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, long arg8) {
      gl.glTexSubImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexSubImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, int arg9, Buffer arg10) {
      gl.glTexSubImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTexSubImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, int arg9, long arg10) {
      gl.glTexSubImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTextureBufferRangeEXT(
      int arg0, int arg1, int arg2, int arg3, long arg4, long arg5) {
      // XXX not implemented
   }

   public void glTextureImage2DMultisampleCoverageNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      boolean arg7) {
      gl.glTextureImage2DMultisampleCoverageNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTextureImage2DMultisampleNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5,
      boolean arg6) {
      gl.glTextureImage2DMultisampleNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTextureImage3DMultisampleCoverageNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, boolean arg8) {
      gl.glTextureImage3DMultisampleCoverageNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTextureImage3DMultisampleNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      boolean arg7) {
      gl.glTextureImage3DMultisampleNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glTextureStorage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      // XXX not implemented
   }

   public void glTextureStorage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
      // XXX not implemented
   }

   public void glTextureStorage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
      // XXX not implemented
   }

   public void glTransformFeedbackVaryings(
      int arg0, int arg1, String[] arg2, int arg3) {
      System.out.println(
         "glTransformFeedbackVaryings(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glTransformFeedbackVaryings(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform(GLUniformData arg0) {
      System.out.println("glUniform(" + arg0 + ")");
      gl.glUniform(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform1f(int arg0, float arg1) {
      System.out.println("glUniform1f(" + arg0 + "," + arg1 + ")");
      gl.glUniform1f(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform1fv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glUniform1fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform1fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform1fv(int arg0, int arg1, FloatBuffer arg2) {
      System.out
      .println("glUniform1fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform1fv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform1i(int arg0, int arg1) {
      System.out.println("glUniform1i(" + arg0 + "," + arg1 + ")");
      gl.glUniform1i(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform1iv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glUniform1iv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform1iv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform1iv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glUniform1iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform1iv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform1ui(int arg0, int arg1) {
      System.out.println("glUniform1ui(" + arg0 + "," + arg1 + ")");
      gl.glUniform1ui(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform1uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glUniform1uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform1uiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform1uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glUniform1uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform1uiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform2f(int arg0, float arg1, float arg2) {
      System.out.println("glUniform2f(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2f(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform2fv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glUniform2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform2fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform2fv(int arg0, int arg1, FloatBuffer arg2) {
      System.out
      .println("glUniform2fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2fv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform2i(int arg0, int arg1, int arg2) {
      System.out.println("glUniform2i(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2i(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform2iv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glUniform2iv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform2iv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform2iv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glUniform2iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2iv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform2ui(int arg0, int arg1, int arg2) {
      System.out
      .println("glUniform2ui(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2ui(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform2uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glUniform2uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform2uiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform2uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glUniform2uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2uiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform3f(int arg0, float arg1, float arg2, float arg3) {
      System.out.println(
         "glUniform3f(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3f(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform3fv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glUniform3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform3fv(int arg0, int arg1, FloatBuffer arg2) {
      System.out
      .println("glUniform3fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform3fv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform3i(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glUniform3i(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3i(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform3iv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glUniform3iv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3iv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform3iv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glUniform3iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform3iv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform3ui(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glUniform3ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3ui(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform3uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glUniform3uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3uiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform3uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glUniform3uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform3uiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform4f(
      int arg0, float arg1, float arg2, float arg3, float arg4) {
      System.out.println(
         "glUniform4f(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glUniform4f(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform4fv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println(
         "glUniform4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform4fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform4fv(int arg0, int arg1, FloatBuffer arg2) {
      System.out
      .println("glUniform4fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform4fv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform4i(int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println(
         "glUniform4i(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glUniform4i(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform4iv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glUniform4iv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform4iv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform4iv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glUniform4iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform4iv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform4ui(int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println(
         "glUniform4ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glUniform4ui(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform4uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println(
         "glUniform4uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform4uiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniform4uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out
      .println("glUniform4uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform4uiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformBlockBinding(int arg0, int arg1, int arg2) {
      System.out.println(
         "glUniformBlockBinding(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniformBlockBinding(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix2fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println(
         "glUniformMatrix2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glUniformMatrix2fv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix2fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println(
         "glUniformMatrix2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glUniformMatrix2fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix2x3fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println(
         "glUniformMatrix2x3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glUniformMatrix2x3fv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix2x3fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println(
         "glUniformMatrix2x3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glUniformMatrix2x3fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix2x4fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println(
         "glUniformMatrix2x4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glUniformMatrix2x4fv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix2x4fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println(
         "glUniformMatrix2x4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glUniformMatrix2x4fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix3fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println(
         "glUniformMatrix3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glUniformMatrix3fv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix3fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println(
         "glUniformMatrix3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glUniformMatrix3fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix3x2fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println(
         "glUniformMatrix3x2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glUniformMatrix3x2fv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix3x2fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println(
         "glUniformMatrix3x2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glUniformMatrix3x2fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix3x4fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println(
         "glUniformMatrix3x4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glUniformMatrix3x4fv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix3x4fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println(
         "glUniformMatrix3x4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glUniformMatrix3x4fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix4fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println(
         "glUniformMatrix4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glUniformMatrix4fv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix4fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println(
         "glUniformMatrix4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glUniformMatrix4fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix4x2fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println(
         "glUniformMatrix4x2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glUniformMatrix4x2fv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix4x2fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println(
         "glUniformMatrix4x2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glUniformMatrix4x2fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix4x3fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println(
         "glUniformMatrix4x3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glUniformMatrix4x3fv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformMatrix4x3fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println(
         "glUniformMatrix4x3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glUniformMatrix4x3fv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformui64NV(int arg0, long arg1) {
      System.out.println("glUniformui64NV(" + arg0 + "," + arg1 + ")");
      gl.glUniformui64NV(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformui64vNV(int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println(
         "glUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glUniformui64vNV(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glUniformui64vNV(int arg0, int arg1, LongBuffer arg2) {
      System.out
      .println("glUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniformui64vNV(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public boolean glUnmapBuffer(int arg0) {
      System.out.println("glUnmapBuffer(" + arg0 + ")");
      boolean out = gl.glUnmapBuffer(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void glUseProgram(int arg0) {
      System.out.println("glUseProgram(" + arg0 + ")");
      gl.glUseProgram(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glValidateProgram(int arg0) {
      System.out.println("glValidateProgram(" + arg0 + ")");
      gl.glValidateProgram(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexArrayBindVertexBufferEXT(
      int arg0, int arg1, int arg2, long arg3, int arg4) {
      System.out.println(
         "glVertexArrayBindVertexBufferEXT(" + arg0 + "," + arg1 + "," + arg2
         + "," + arg3 + "," + arg4 + ")");
      // XXX not implemented
   }

   public void glVertexArrayVertexAttribBindingEXT(
      int arg0, int arg1, int arg2) {
      // XXX not implemented
   }

   public void glVertexArrayVertexAttribFormatEXT(
      int arg0, int arg1, int arg2, int arg3, boolean arg4, int arg5) {
      // XXX not implemented
   }

   public void glVertexArrayVertexAttribIFormatEXT(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      // XXX not implemented
   }

   public void glVertexArrayVertexAttribLFormatEXT(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      // XXX not implemented
   }

   public void glVertexArrayVertexBindingDivisorEXT(
      int arg0, int arg1, int arg2) {
      // XXX not implemented
   }

   public void glVertexAttrib1d(int arg0, double arg1) {
      System.out.println("glVertexAttrib1d(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1d(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib1dv(int arg0, double[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib1dv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib1dv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib1dv(int arg0, DoubleBuffer arg1) {
      System.out.println("glVertexAttrib1dv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1dv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib1f(int arg0, float arg1) {
      System.out.println("glVertexAttrib1f(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1f(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib1fv(int arg0, float[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib1fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib1fv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib1fv(int arg0, FloatBuffer arg1) {
      System.out.println("glVertexAttrib1fv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1fv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib1s(int arg0, short arg1) {
      System.out.println("glVertexAttrib1s(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1s(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib1sv(int arg0, short[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib1sv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib1sv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib1sv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib1sv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1sv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib2d(int arg0, double arg1, double arg2) {
      System.out
      .println("glVertexAttrib2d(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2d(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib2dv(int arg0, double[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib2dv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2dv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib2dv(int arg0, DoubleBuffer arg1) {
      System.out.println("glVertexAttrib2dv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib2dv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib2f(int arg0, float arg1, float arg2) {
      System.out
      .println("glVertexAttrib2f(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2f(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib2fv(int arg0, float[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib2fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2fv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib2fv(int arg0, FloatBuffer arg1) {
      System.out.println("glVertexAttrib2fv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib2fv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib2s(int arg0, short arg1, short arg2) {
      System.out
      .println("glVertexAttrib2s(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2s(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib2sv(int arg0, short[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib2sv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2sv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib2sv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib2sv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib2sv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib3d(
      int arg0, double arg1, double arg2, double arg3) {
      System.out.println(
         "glVertexAttrib3d(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttrib3d(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib3dv(int arg0, double[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib3dv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib3dv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib3dv(int arg0, DoubleBuffer arg1) {
      System.out.println("glVertexAttrib3dv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib3dv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib3f(int arg0, float arg1, float arg2, float arg3) {
      System.out.println(
         "glVertexAttrib3f(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttrib3f(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib3fv(int arg0, float[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib3fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib3fv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib3fv(int arg0, FloatBuffer arg1) {
      System.out.println("glVertexAttrib3fv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib3fv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib3s(int arg0, short arg1, short arg2, short arg3) {
      System.out.println(
         "glVertexAttrib3s(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttrib3s(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib3sv(int arg0, short[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib3sv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib3sv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib3sv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib3sv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib3sv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Nbv(int arg0, byte[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib4Nbv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Nbv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Nbv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttrib4Nbv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Nbv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Niv(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib4Niv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Niv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Niv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttrib4Niv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Niv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Nsv(int arg0, short[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib4Nsv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Nsv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Nsv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib4Nsv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Nsv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Nub(
      int arg0, byte arg1, byte arg2, byte arg3, byte arg4) {
      System.out.println(
         "glVertexAttrib4Nub(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glVertexAttrib4Nub(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Nubv(int arg0, byte[] arg1, int arg2) {
      System.out.println(
         "glVertexAttrib4Nubv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Nubv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Nubv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttrib4Nubv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Nubv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Nuiv(int arg0, int[] arg1, int arg2) {
      System.out.println(
         "glVertexAttrib4Nuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Nuiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Nuiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttrib4Nuiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Nuiv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Nusv(int arg0, short[] arg1, int arg2) {
      System.out.println(
         "glVertexAttrib4Nusv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Nusv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4Nusv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib4Nusv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Nusv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4bv(int arg0, byte[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib4bv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4bv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4bv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttrib4bv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4bv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4d(
      int arg0, double arg1, double arg2, double arg3, double arg4) {
      System.out.println(
         "glVertexAttrib4d(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glVertexAttrib4d(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4dv(int arg0, double[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib4dv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4dv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4dv(int arg0, DoubleBuffer arg1) {
      System.out.println("glVertexAttrib4dv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4dv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4f(
      int arg0, float arg1, float arg2, float arg3, float arg4) {
      System.out.println(
         "glVertexAttrib4f(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glVertexAttrib4f(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4fv(int arg0, float[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib4fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4fv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4fv(int arg0, FloatBuffer arg1) {
      System.out.println("glVertexAttrib4fv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4fv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4iv(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib4iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4iv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4iv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttrib4iv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4iv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4s(
      int arg0, short arg1, short arg2, short arg3, short arg4) {
      System.out.println(
         "glVertexAttrib4s(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ","
            + arg4 + ")");
      gl.glVertexAttrib4s(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4sv(int arg0, short[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib4sv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4sv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4sv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib4sv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4sv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4ubv(int arg0, byte[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib4ubv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4ubv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4ubv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttrib4ubv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4ubv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4uiv(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib4uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4uiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttrib4uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4uiv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4usv(int arg0, short[] arg1, int arg2) {
      System.out
      .println("glVertexAttrib4usv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4usv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttrib4usv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib4usv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4usv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribDivisor(int arg0, int arg1) {
      System.out.println("glVertexAttribDivisor(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribDivisor(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribFormatNV(
      int arg0, int arg1, int arg2, boolean arg3, int arg4) {
      System.out.println(
         "glVertexAttribFormatNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glVertexAttribFormatNV(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI1i(int arg0, int arg1) {
      System.out.println("glVertexAttribI1i(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI1i(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI1iv(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glVertexAttribI1iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI1iv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI1iv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI1iv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI1iv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI1ui(int arg0, int arg1) {
      System.out.println("glVertexAttribI1ui(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI1ui(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI1uiv(int arg0, int[] arg1, int arg2) {
      System.out.println(
         "glVertexAttribI1uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI1uiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI1uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI1uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI1uiv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI2i(int arg0, int arg1, int arg2) {
      System.out
      .println("glVertexAttribI2i(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI2i(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI2iv(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glVertexAttribI2iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI2iv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI2iv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI2iv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI2iv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI2ui(int arg0, int arg1, int arg2) {
      System.out
      .println("glVertexAttribI2ui(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI2ui(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI2uiv(int arg0, int[] arg1, int arg2) {
      System.out.println(
         "glVertexAttribI2uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI2uiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI2uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI2uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI2uiv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI3i(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glVertexAttribI3i(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttribI3i(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI3iv(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glVertexAttribI3iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI3iv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI3iv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI3iv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI3iv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI3ui(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glVertexAttribI3ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttribI3ui(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI3uiv(int arg0, int[] arg1, int arg2) {
      System.out.println(
         "glVertexAttribI3uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI3uiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI3uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI3uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI3uiv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4bv(int arg0, byte[] arg1, int arg2) {
      System.out
      .println("glVertexAttribI4bv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4bv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4bv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttribI4bv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4bv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4i(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println(
         "glVertexAttribI4i(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glVertexAttribI4i(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4iv(int arg0, int[] arg1, int arg2) {
      System.out
      .println("glVertexAttribI4iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4iv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4iv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI4iv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4iv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4sv(int arg0, short[] arg1, int arg2) {
      System.out
      .println("glVertexAttribI4sv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4sv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4sv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttribI4sv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4sv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4ubv(int arg0, byte[] arg1, int arg2) {
      System.out.println(
         "glVertexAttribI4ubv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4ubv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4ubv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttribI4ubv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4ubv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4ui(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println(
         "glVertexAttribI4ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glVertexAttribI4ui(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4uiv(int arg0, int[] arg1, int arg2) {
      System.out.println(
         "glVertexAttribI4uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4uiv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI4uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4uiv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4usv(int arg0, short[] arg1, int arg2) {
      System.out.println(
         "glVertexAttribI4usv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4usv(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribI4usv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttribI4usv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4usv(arg0, arg1);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribIFormatNV(int arg0, int arg1, int arg2, int arg3) {
      System.out.println(
         "glVertexAttribIFormatNV(" + arg0 + "," + arg1 + "," + arg2 + ","
            + arg3 + ")");
      gl.glVertexAttribIFormatNV(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribIPointer(
      int arg0, int arg1, int arg2, int arg3, long arg4) {
      System.out.println(
         "glVertexAttribIPointer(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glVertexAttribIPointer(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP1ui(int arg0, int arg1, boolean arg2, int arg3) {
      System.out.println(
         "glVertexAttribP1ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttribP1ui(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP1uiv(
      int arg0, int arg1, boolean arg2, int[] arg3, int arg4) {
      System.out.println(
         "glVertexAttribP1uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glVertexAttribP1uiv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP1uiv(
      int arg0, int arg1, boolean arg2, IntBuffer arg3) {
      System.out.println(
         "glVertexAttribP1uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttribP1uiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP2ui(int arg0, int arg1, boolean arg2, int arg3) {
      System.out.println(
         "glVertexAttribP2ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttribP2ui(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP2uiv(
      int arg0, int arg1, boolean arg2, int[] arg3, int arg4) {
      System.out.println(
         "glVertexAttribP2uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glVertexAttribP2uiv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP2uiv(
      int arg0, int arg1, boolean arg2, IntBuffer arg3) {
      System.out.println(
         "glVertexAttribP2uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttribP2uiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP3ui(int arg0, int arg1, boolean arg2, int arg3) {
      System.out.println(
         "glVertexAttribP3ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttribP3ui(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP3uiv(
      int arg0, int arg1, boolean arg2, int[] arg3, int arg4) {
      System.out.println(
         "glVertexAttribP3uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glVertexAttribP3uiv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP3uiv(
      int arg0, int arg1, boolean arg2, IntBuffer arg3) {
      System.out.println(
         "glVertexAttribP3uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttribP3uiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP4ui(int arg0, int arg1, boolean arg2, int arg3) {
      System.out.println(
         "glVertexAttribP4ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttribP4ui(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP4uiv(
      int arg0, int arg1, boolean arg2, int[] arg3, int arg4) {
      System.out.println(
         "glVertexAttribP4uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + ")");
      gl.glVertexAttribP4uiv(arg0, arg1, arg2, arg3, arg4);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribP4uiv(
      int arg0, int arg1, boolean arg2, IntBuffer arg3) {
      System.out.println(
         "glVertexAttribP4uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + ")");
      gl.glVertexAttribP4uiv(arg0, arg1, arg2, arg3);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribPointer(GLArrayData arg0) {
      System.out.println("glVertexAttribPointer(" + arg0 + ")");
      gl.glVertexAttribPointer(arg0);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexAttribPointer(
      int arg0, int arg1, int arg2, boolean arg3, int arg4, long arg5) {
      System.out.println(
         "glVertexAttribPointer(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3
         + "," + arg4 + "," + arg5 + ")");
      gl.glVertexAttribPointer(arg0, arg1, arg2, arg3, arg4, arg5);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexFormatNV(int arg0, int arg1, int arg2) {
      System.out
      .println("glVertexFormatNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexFormatNV(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public void glVertexP2ui(int arg0, int arg1) {
      // XXX not implemented
   }

   public void glVertexP2uiv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented
   }

   public void glVertexP2uiv(int arg0, IntBuffer arg1) {
      // XXX not implemented
   }

   public void glVertexP3ui(int arg0, int arg1) {
      // XXX not implemented
   }

   public void glVertexP3uiv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented
   }

   public void glVertexP3uiv(int arg0, IntBuffer arg1) {
      // XXX not implemented
   }

   public void glVertexP4ui(int arg0, int arg1) {
      // XXX not implemented
   }

   public void glVertexP4uiv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented
   }

   public void glVertexP4uiv(int arg0, IntBuffer arg1) {
      // XXX not implemented
   }

   public void glViewport(int arg0, int arg1, int arg2, int arg3) {
      // XXX not implemented
   }

   public void glWaitSync(long arg0, int arg1, long arg2) {
      System.out.println("glWaitSync(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glWaitSync(arg0, arg1, arg2);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public boolean isGL() {
      boolean out = gl.isGL();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL4bc() {
      boolean out = gl.isGL4bc();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL4() {
      boolean out = gl.isGL4();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL3bc() {
      boolean out = gl.isGL3bc();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL3() {
      boolean out = gl.isGL3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL2() {
      boolean out = gl.isGL2();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGLES1() {
      boolean out = gl.isGLES1();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGLES2() {
      boolean out = gl.isGLES2();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGLES3() {
      boolean out = gl.isGLES3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGLES() {
      boolean out = gl.isGLES();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL2ES1() {
      boolean out = gl.isGL2ES1();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL2ES2() {
      boolean out = gl.isGL2ES2();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL2ES3() {
      boolean out = gl.isGL2ES3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL3ES3() {
      boolean out = gl.isGL3ES3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL4ES3() {
      boolean out = gl.isGL4ES3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL2GL3() {
      boolean out = gl.isGL2GL3();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL4core() {
      boolean out = gl.isGL4core();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGL3core() {
      boolean out = gl.isGL3core();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGLcore() {
      boolean out = gl.isGLcore();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGLES2Compatible() {
      boolean out = gl.isGLES2Compatible();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isGLES3Compatible() {
      boolean out = gl.isGLES3Compatible();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean hasGLSL() {
      boolean out = gl.hasGLSL();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isFunctionAvailable(String glFunctionName) {
      boolean out = gl.isFunctionAvailable(glFunctionName);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isExtensionAvailable(String glExtensionName) {
      boolean out = gl.isExtensionAvailable(glExtensionName);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean hasBasicFBOSupport() {
      boolean out = gl.hasBasicFBOSupport();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean hasFullFBOSupport() {
      boolean out = gl.hasFullFBOSupport();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isNPOTTextureAvailable() {
      boolean out = gl.isNPOTTextureAvailable();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isPBOPackBound() {
      boolean out = gl.isPBOPackBound();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isPBOUnpackBound() {
      boolean out = gl.isPBOUnpackBound();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isTextureFormatBGRA8888Available() {
      boolean out = gl.isTextureFormatBGRA8888Available();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public void setSwapInterval(int interval) {
      gl.setSwapInterval(interval);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
   }

   public GLBufferStorage mapBuffer(int target, int access) throws GLException {
      GLBufferStorage out = gl.mapBuffer(target, access);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public GLBufferStorage mapBufferRange(
      int target, long offset, long length, int access) throws GLException {
      GLBufferStorage out = gl.mapBufferRange(target, offset, length, access);
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isVBOArrayBound() {
      boolean out = gl.isVBOArrayBound();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean isVBOElementArrayBound() {
      boolean out = gl.isVBOElementArrayBound();
      if (checkForErrors) {
         GLSupport.checkAndPrintGLError(gl);
      }
      return out;
   }

   public boolean glIsPBOPackBound() {
      return false;
   }

   public boolean glIsPBOUnpackBound() {
      return false;
   }

   public int glGetBoundBuffer(int target) {
      return 0;
   }

   public long glGetBufferSize(int bufferName) {
      return 0;
   }

   public boolean glIsVBOArrayBound() {
      return false;
   }

   public boolean glIsVBOElementArrayBound() {
      return false;
   }

   public void glActiveShaderProgram(int arg0, int arg1) {
      // XXX not implemented gl.glActiveShaderProgram(arg0, arg1);
   }

   public void glApplyFramebufferAttachmentCMAAINTEL() {
      // XXX not implemented gl.glApplyFramebufferAttachmentCMAAINTEL();
   }

   public void glBeginQueryIndexed(int arg0, int arg1, int arg2) {
      // XXX not implemented gl.glBeginQueryIndexed(arg0, arg1, arg2);
   }

   public void glBindImageTexture(
      int arg0, int arg1, int arg2, boolean arg3, int arg4, int arg5,
      int arg6) {
      // XXX not implemented gl.glBindImageTexture(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glBindProgramPipeline(int arg0) {
      // XXX not implemented gl.glBindProgramPipeline(arg0);
   }

   public void glBindTransformFeedback(int arg0, int arg1) {
      // XXX not implemented gl.glBindTransformFeedback(arg0, arg1);
   }

   public void glBindVertexBuffer(int arg0, int arg1, long arg2, int arg3) {
      // XXX not implemented gl.glBindVertexBuffer(arg0, arg1, arg2, arg3);
   }

   public void glBlendBarrier() {
      // XXX not implemented gl.glBlendBarrier();
   }

   public void glBlendEquationSeparatei(int arg0, int arg1, int arg2) {
      // XXX not implemented gl.glBlendEquationSeparatei(arg0, arg1, arg2);
   }

   public void glBlendEquationi(int arg0, int arg1) {
      // XXX not implemented gl.glBlendEquationi(arg0, arg1);
   }

   public void glBlendFuncSeparatei(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      // XXX not implemented gl.glBlendFuncSeparatei(arg0, arg1, arg2, arg3, arg4);
   }

   public void glBlendFunci(int arg0, int arg1, int arg2) {
      // XXX not implemented gl.glBlendFunci(arg0, arg1, arg2);
   }

   public void glBufferPageCommitmentARB(
      int arg0, long arg1, long arg2, boolean arg3) {
      // XXX not implemented gl.glBufferPageCommitmentARB(arg0, arg1, arg2, arg3);
   }

   public void glClearBufferData(
      int arg0, int arg1, int arg2, int arg3, Buffer arg4) {
      // XXX not implemented gl.glClearBufferData(arg0, arg1, arg2, arg3, arg4);
   }

   public void glClearBufferSubData(
      int arg0, int arg1, long arg2, long arg3, int arg4, int arg5,
      Buffer arg6) {
      // XXX not implemented gl.glClearBufferSubData(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glCopyImageSubData(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, int arg9, int arg10, int arg11, int arg12, int arg13,
      int arg14) {
      // XXX not implemented  gl.glCopyImageSubData(
      // XXX not implemented       arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
      // XXX not implemented       arg11, arg12, arg13, arg14);
   }

   public int glCreateShaderProgramv(int arg0, int arg1, String[] arg2) {
      // XXX not implemented  return gl.glCreateShaderProgramv(arg0, arg1, arg2);
      return -1;
   }

   public void glDeleteProgramPipelines(int arg0, int[] arg1, int arg2) {
      // XXX not implemented gl.glDeleteProgramPipelines(arg0, arg1, arg2);
   }

   public void glDeleteProgramPipelines(int arg0, IntBuffer arg1) {
      // XXX not implemented gl.glDeleteProgramPipelines(arg0, arg1);
   }

   public void glDeleteTransformFeedbacks(int arg0, int[] arg1, int arg2) {
      // XXX not implemented gl.glDeleteTransformFeedbacks(arg0, arg1, arg2);
   }

   public void glDeleteTransformFeedbacks(int arg0, IntBuffer arg1) {
      // XXX not implemented  gl.glDeleteTransformFeedbacks(arg0, arg1);
   }

   public void glDepthRangeArrayv(int arg0, int arg1, double[] arg2, int arg3) {
      // XXX not implemented    gl.glDepthRangeArrayv(arg0, arg1, arg2, arg3);
   }

   public void glDepthRangeArrayv(int arg0, int arg1, DoubleBuffer arg2) {
      // XXX not implemented    gl.glDepthRangeArrayv(arg0, arg1, arg2);
   }

   public void glDepthRangeIndexed(int arg0, double arg1, double arg2) {
      // XXX not implemented    gl.glDepthRangeIndexed(arg0, arg1, arg2);
   }

   public void glDispatchCompute(int arg0, int arg1, int arg2) {
      // XXX not implemented   gl.glDispatchCompute(arg0, arg1, arg2);
   }

   public void glDispatchComputeIndirect(long arg0) {
      // XXX not implemented  gl.glDispatchComputeIndirect(arg0);
   }

   public void glDrawArraysIndirect(int arg0, Buffer arg1) {
      // XXX not implemented   gl.glDrawArraysIndirect(arg0, arg1);
   }

   public void glDrawArraysIndirect(int arg0, long arg1) {
      // XXX not implemented  gl.glDrawArraysIndirect(arg0, arg1);
   }

   public void glDrawArraysInstancedBaseInstance(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      // XXX not implemented   gl.glDrawArraysInstancedBaseInstance(arg0, arg1, arg2, arg3, arg4);
   }

   public void glDrawElementsBaseVertex(
      int arg0, int arg1, int arg2, Buffer arg3, int arg4) {
      // XXX not implemented    gl.glDrawElementsBaseVertex(arg0, arg1, arg2, arg3, arg4);
   }

   public void glDrawElementsIndirect(int arg0, int arg1, Buffer arg2) {
      // XXX not implemented    gl.glDrawElementsIndirect(arg0, arg1, arg2);
   }

   public void glDrawElementsIndirect(int arg0, int arg1, long arg2) {
      // XXX not implemented    gl.glDrawElementsIndirect(arg0, arg1, arg2);
   }

   public void glDrawElementsInstancedBaseInstance(
      int arg0, int arg1, int arg2, long arg3, int arg4, int arg5) {
      // XXX not implemented   gl.glDrawElementsInstancedBaseInstance(
      // XXX not implemented       arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glDrawElementsInstancedBaseVertex(
      int arg0, int arg1, int arg2, Buffer arg3, int arg4, int arg5) {
      // XXX not implemented     gl.glDrawElementsInstancedBaseVertex(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glDrawElementsInstancedBaseVertexBaseInstance(
      int arg0, int arg1, int arg2, long arg3, int arg4, int arg5, int arg6) {
      // XXX not implemented      gl.glDrawElementsInstancedBaseVertexBaseInstance(
      // XXX not implemented         arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glDrawRangeElementsBaseVertex(
      int arg0, int arg1, int arg2, int arg3, int arg4, Buffer arg5, int arg6) {
      // XXX not implemented  // XXX not implemented  gl.glDrawRangeElementsBaseVertex(
      // XXX not implemented          arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glDrawTransformFeedback(int arg0, int arg1) {
      // XXX not implemented  gl.glDrawTransformFeedback(arg0, arg1);
   }

   public void glDrawTransformFeedbackInstanced(int arg0, int arg1, int arg2) {
      // XXX not implemented  gl.glDrawTransformFeedbackInstanced(arg0, arg1, arg2);
   }

   public void glDrawTransformFeedbackStream(int arg0, int arg1, int arg2) {
      // XXX not implemented  gl.glDrawTransformFeedbackStream(arg0, arg1, arg2);
   }

   public void glDrawTransformFeedbackStreamInstanced(
      int arg0, int arg1, int arg2, int arg3) {
      // XXX not implemented  gl.glDrawTransformFeedbackStreamInstanced(arg0, arg1, arg2, arg3);
   }

   public void glEndQueryIndexed(int arg0, int arg1) {
      // XXX not implemented  gl.glEndQueryIndexed(arg0, arg1);
   }

   public void glFramebufferParameteri(int arg0, int arg1, int arg2) {
      // XXX not implemented  gl.glFramebufferParameteri(arg0, arg1, arg2);
   }

   public void glFramebufferTextureEXT(int arg0, int arg1, int arg2, int arg3) {
      // XXX not implemented  gl.glFramebufferTextureEXT(arg0, arg1, arg2, arg3);
   }

   public void glGenProgramPipelines(int arg0, int[] arg1, int arg2) {
      // XXX not implemented  gl.glGenProgramPipelines(arg0, arg1, arg2);
   }

   public void glGenProgramPipelines(int arg0, IntBuffer arg1) {
      // XXX not implemented  gl.glGenProgramPipelines(arg0, arg1);
   }

   public void glGenTransformFeedbacks(int arg0, int[] arg1, int arg2) {
      // XXX not implemented  gl.glGenTransformFeedbacks(arg0, arg1, arg2);
   }

   public void glGenTransformFeedbacks(int arg0, IntBuffer arg1) {
      // XXX not implemented  gl.glGenTransformFeedbacks(arg0, arg1);
   }

   public void glGetActiveAtomicCounterBufferiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented  gl.glGetActiveAtomicCounterBufferiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetActiveAtomicCounterBufferiv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented  gl.glGetActiveAtomicCounterBufferiv(arg0, arg1, arg2, arg3);
   }

   public void glGetActiveSubroutineName(
      int arg0, int arg1, int arg2, int arg3, int[] arg4, int arg5, byte[] arg6,
      int arg7) {
      // XXX not implemented  gl.glGetActiveSubroutineName(
      // XXX not implemented          arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glGetActiveSubroutineName(
      int arg0, int arg1, int arg2, int arg3, IntBuffer arg4, ByteBuffer arg5) {
      // XXX not implemented  gl.glGetActiveSubroutineName(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetActiveSubroutineUniformName(
      int arg0, int arg1, int arg2, int arg3, int[] arg4, int arg5, byte[] arg6,
      int arg7) {
      // XXX not implemented  gl.glGetActiveSubroutineUniformName(
      // XXX not implemented          arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glGetActiveSubroutineUniformName(
      int arg0, int arg1, int arg2, int arg3, IntBuffer arg4, ByteBuffer arg5) {
      // XXX not implemented  gl.glGetActiveSubroutineUniformName(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetActiveSubroutineUniformiv(
      int arg0, int arg1, int arg2, int arg3, int[] arg4, int arg5) {
      // XXX not implemented  gl.glGetActiveSubroutineUniformiv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetActiveSubroutineUniformiv(
      int arg0, int arg1, int arg2, int arg3, IntBuffer arg4) {
      // XXX not implemented  gl.glGetActiveSubroutineUniformiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetDoublei_v(int arg0, int arg1, double[] arg2, int arg3) {
      // XXX not implemented  gl.glGetDoublei_v(arg0, arg1, arg2, arg3);
   }

   public void glGetDoublei_v(int arg0, int arg1, DoubleBuffer arg2) {
      // XXX not implemented  gl.glGetDoublei_v(arg0, arg1, arg2);
   }

   public void glGetFloati_v(int arg0, int arg1, float[] arg2, int arg3) {
      // XXX not implemented  gl.glGetFloati_v(arg0, arg1, arg2, arg3);
   }

   public void glGetFloati_v(int arg0, int arg1, FloatBuffer arg2) {
      // XXX not implemented  gl.glGetFloati_v(arg0, arg1, arg2);
   }

   public void glGetFramebufferParameteriv(
      int arg0, int arg1, int[] arg2, int arg3) {
      // XXX not implemented  gl.glGetFramebufferParameteriv(arg0, arg1, arg2, arg3);
   }

   public void glGetFramebufferParameteriv(int arg0, int arg1, IntBuffer arg2) {
      // XXX not implemented  gl.glGetFramebufferParameteriv(arg0, arg1, arg2);
   }

   public void glGetInternalformati64v(
      int arg0, int arg1, int arg2, int arg3, long[] arg4, int arg5) {
      // XXX not implemented  gl.glGetInternalformati64v(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetInternalformati64v(
      int arg0, int arg1, int arg2, int arg3, LongBuffer arg4) {
      // XXX not implemented  gl.glGetInternalformati64v(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetProgramInterfaceiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented  gl.glGetProgramInterfaceiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetProgramInterfaceiv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented  gl.glGetProgramInterfaceiv(arg0, arg1, arg2, arg3);
   }

   public void glGetProgramPipelineInfoLog(
      int arg0, int arg1, int[] arg2, int arg3, byte[] arg4, int arg5) {
      // XXX not implemented  gl.glGetProgramPipelineInfoLog(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetProgramPipelineInfoLog(
      int arg0, int arg1, IntBuffer arg2, ByteBuffer arg3) {
      // XXX not implemented  gl.glGetProgramPipelineInfoLog(arg0, arg1, arg2, arg3);
   }

   public void glGetProgramPipelineiv(
      int arg0, int arg1, int[] arg2, int arg3) {
      // XXX not implemented  gl.glGetProgramPipelineiv(arg0, arg1, arg2, arg3);
   }

   public void glGetProgramPipelineiv(int arg0, int arg1, IntBuffer arg2) {
      // XXX not implemented  gl.glGetProgramPipelineiv(arg0, arg1, arg2);
   }

   public int glGetProgramResourceIndex(
      int arg0, int arg1, byte[] arg2, int arg3) {
      return -1; // XXX not implemented  gl.glGetProgramResourceIndex(arg0, arg1, arg2, arg3);
   }

   public int glGetProgramResourceIndex(int arg0, int arg1, ByteBuffer arg2) {
      return -1; // XXX not implemented  gl.glGetProgramResourceIndex(arg0, arg1, arg2);
   }

   public int glGetProgramResourceLocation(
      int arg0, int arg1, byte[] arg2, int arg3) {
      return -1; // XXX not implemented  gl.glGetProgramResourceLocation(arg0, arg1, arg2, arg3);
   }

   public int glGetProgramResourceLocation(
      int arg0, int arg1, ByteBuffer arg2) {
      return -1; // XXX not implemented  gl.glGetProgramResourceLocation(arg0, arg1, arg2);
   }

   public void glGetProgramResourceName(
      int arg0, int arg1, int arg2, int arg3, int[] arg4, int arg5, byte[] arg6,
      int arg7) {
      // XXX not implemented  gl.glGetProgramResourceName(
      // XXX not implemented          arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glGetProgramResourceName(
      int arg0, int arg1, int arg2, int arg3, IntBuffer arg4, ByteBuffer arg5) {
      // XXX not implemented  gl.glGetProgramResourceName(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetProgramResourceiv(
      int arg0, int arg1, int arg2, int arg3, int[] arg4, int arg5, int arg6,
      int[] arg7, int arg8, int[] arg9, int arg10) {
      // XXX not implemented  gl.glGetProgramResourceiv(
      // XXX not implemented          arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
   }

   public void glGetProgramResourceiv(
      int arg0, int arg1, int arg2, int arg3, IntBuffer arg4, int arg5,
      IntBuffer arg6, IntBuffer arg7) {
      // XXX not implemented  gl.glGetProgramResourceiv(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glGetProgramStageiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented  gl.glGetProgramStageiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetProgramStageiv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented  gl.glGetProgramStageiv(arg0, arg1, arg2, arg3);
   }

   public void glGetQueryIndexediv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented  gl.glGetQueryIndexediv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetQueryIndexediv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented  gl.glGetQueryIndexediv(arg0, arg1, arg2, arg3);
   }

   public int glGetSubroutineIndex(int arg0, int arg1, String arg2) {
      return -1; // XXX not implemented  gl.glGetSubroutineIndex(arg0, arg1, arg2);
   }

   public int glGetSubroutineUniformLocation(int arg0, int arg1, String arg2) {
      return -1; // XXX not implemented  gl.glGetSubroutineUniformLocation(arg0, arg1, arg2);
   }

   public void glGetUniformSubroutineuiv(
      int arg0, int arg1, int[] arg2, int arg3) {
      // XXX not implemented  gl.glGetUniformSubroutineuiv(arg0, arg1, arg2, arg3);
   }

   public void glGetUniformSubroutineuiv(int arg0, int arg1, IntBuffer arg2) {
      // XXX not implemented  gl.glGetUniformSubroutineuiv(arg0, arg1, arg2);
   }

   public void glGetUniformdv(int arg0, int arg1, double[] arg2, int arg3) {
      // XXX not implemented  gl.glGetUniformdv(arg0, arg1, arg2, arg3);
   }

   public void glGetUniformdv(int arg0, int arg1, DoubleBuffer arg2) {
      // XXX not implemented  gl.glGetUniformdv(arg0, arg1, arg2);
   }

   public void glGetVertexAttribLdv(
      int arg0, int arg1, double[] arg2, int arg3) {
      // XXX not implemented  gl.glGetVertexAttribLdv(arg0, arg1, arg2, arg3);
   }

   public void glGetVertexAttribLdv(int arg0, int arg1, DoubleBuffer arg2) {
      // XXX not implemented  gl.glGetVertexAttribLdv(arg0, arg1, arg2);
   }

   public void glInvalidateBufferData(int arg0) {
      // XXX not implemented  gl.glInvalidateBufferData(arg0);
   }

   public void glInvalidateBufferSubData(int arg0, long arg1, long arg2) {
      // XXX not implemented  gl.glInvalidateBufferSubData(arg0, arg1, arg2);
   }

   public void glInvalidateFramebuffer(
      int arg0, int arg1, int[] arg2, int arg3) {
      // XXX not implemented  gl.glInvalidateFramebuffer(arg0, arg1, arg2, arg3);
   }

   public void glInvalidateFramebuffer(int arg0, int arg1, IntBuffer arg2) {
      // XXX not implemented  gl.glInvalidateFramebuffer(arg0, arg1, arg2);
   }

   public void glInvalidateSubFramebuffer(
      int arg0, int arg1, int[] arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7) {
      // XXX not implemented  gl.glInvalidateSubFramebuffer(
      // XXX not implemented          arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glInvalidateSubFramebuffer(
      int arg0, int arg1, IntBuffer arg2, int arg3, int arg4, int arg5,
      int arg6) {
      // XXX not implemented  gl.glInvalidateSubFramebuffer(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glInvalidateTexImage(int arg0, int arg1) {
      // XXX not implemented  gl.glInvalidateTexImage(arg0, arg1);
   }

   public void glInvalidateTexSubImage(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7) {
      // XXX not implemented  gl.glInvalidateTexSubImage(
      // XXX not implemented          arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public boolean glIsProgramPipeline(int arg0) {
      return false; // XXX not implemented  gl.glIsProgramPipeline(arg0);
   }

   public boolean glIsTransformFeedback(int arg0) {
      return false; // XXX not implemented  gl.glIsTransformFeedback(arg0);
   }

   public void glMemoryBarrier(int arg0) {
      // XXX not implemented  gl.glMemoryBarrier(arg0);
   }

   public void glMemoryBarrierByRegion(int arg0) {
      // XXX not implemented  gl.glMemoryBarrierByRegion(arg0);
   }

   public void glMinSampleShading(float arg0) {
      // XXX not implemented  gl.glMinSampleShading(arg0);
   }

   public void glMultiDrawArraysIndirect(
      int arg0, long arg1, int arg2, int arg3) {
      // XXX not implemented  gl.glMultiDrawArraysIndirect(arg0, arg1, arg2, arg3);
   }

   public void glMultiDrawElementsIndirect(
      int arg0, int arg1, Buffer arg2, int arg3, int arg4) {
      // XXX not implemented  gl.glMultiDrawElementsIndirect(arg0, arg1, arg2, arg3, arg4);
   }

   public void glNamedBufferPageCommitmentARB(
      int arg0, long arg1, long arg2, boolean arg3) {
      // XXX not implemented  gl.glNamedBufferPageCommitmentARB(arg0, arg1, arg2, arg3);
   }

   public void glNamedBufferPageCommitmentEXT(
      int arg0, long arg1, long arg2, boolean arg3) {
      // XXX not implemented  gl.glNamedBufferPageCommitmentEXT(arg0, arg1, arg2, arg3);
   }

   public void glPatchParameterfv(int arg0, float[] arg1, int arg2) {
      // XXX not implemented  gl.glPatchParameterfv(arg0, arg1, arg2);
   }

   public void glPatchParameterfv(int arg0, FloatBuffer arg1) {
      // XXX not implemented  gl.glPatchParameterfv(arg0, arg1);
   }

   public void glPatchParameteri(int arg0, int arg1) {
      // XXX not implemented  gl.glPatchParameteri(arg0, arg1);
   }

   public void glPauseTransformFeedback() {
      // XXX not implemented  gl.glPauseTransformFeedback();
   }

   public void glPrimitiveBoundingBox(
      float arg0, float arg1, float arg2, float arg3, float arg4, float arg5,
      float arg6, float arg7) {
      // XXX not implemented  gl.glPrimitiveBoundingBox(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glProgramParameteri(int arg0, int arg1, int arg2) {
      // XXX not implemented  gl.glProgramParameteri(arg0, arg1, arg2);
   }

   public void glProgramUniform1d(int arg0, int arg1, double arg2) {
      // XXX not implemented  gl.glProgramUniform1d(arg0, arg1, arg2);
   }

   public void glProgramUniform1dv(
      int arg0, int arg1, int arg2, double[] arg3, int arg4) {
      // XXX not implemented  gl.glProgramUniform1dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform1dv(
      int arg0, int arg1, int arg2, DoubleBuffer arg3) {
      // XXX not implemented  gl.glProgramUniform1dv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform1f(int arg0, int arg1, float arg2) {
      // XXX not implemented  gl.glProgramUniform1f(arg0, arg1, arg2);
   }

   public void glProgramUniform1fv(
      int arg0, int arg1, int arg2, float[] arg3, int arg4) {
      // XXX not implemented  gl.glProgramUniform1fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform1fv(
      int arg0, int arg1, int arg2, FloatBuffer arg3) {
      // XXX not implemented  gl.glProgramUniform1fv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform1i(int arg0, int arg1, int arg2) {
      // XXX not implemented  gl.glProgramUniform1i(arg0, arg1, arg2);
   }

   public void glProgramUniform1iv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented  gl.glProgramUniform1iv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform1iv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented  gl.glProgramUniform1iv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform1ui(int arg0, int arg1, int arg2) {
      // XXX not implemented  gl.glProgramUniform1ui(arg0, arg1, arg2);
   }

   public void glProgramUniform1uiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented  gl.glProgramUniform1uiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform1uiv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented  gl.glProgramUniform1uiv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform2d(
      int arg0, int arg1, double arg2, double arg3) {
      // XXX not implemented  gl.glProgramUniform2d(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform2dv(
      int arg0, int arg1, int arg2, double[] arg3, int arg4) {
      // XXX not implemented  gl.glProgramUniform2dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform2dv(
      int arg0, int arg1, int arg2, DoubleBuffer arg3) {
      // XXX not implemented  gl.glProgramUniform2dv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform2f(int arg0, int arg1, float arg2, float arg3) {
      // XXX not implemented  gl.glProgramUniform2f(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform2fv(
      int arg0, int arg1, int arg2, float[] arg3, int arg4) {
      // XXX not implemented  gl.glProgramUniform2fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform2fv(
      int arg0, int arg1, int arg2, FloatBuffer arg3) {
      // XXX not implemented  gl.glProgramUniform2fv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform2i(int arg0, int arg1, int arg2, int arg3) {
      // XXX not implemented  gl.glProgramUniform2i(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform2iv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented  gl.glProgramUniform2iv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform2iv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented  gl.glProgramUniform2iv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform2ui(int arg0, int arg1, int arg2, int arg3) {
      // XXX not implemented  gl.glProgramUniform2ui(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform2uiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented  gl.glProgramUniform2uiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform2uiv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented       gl.glProgramUniform2uiv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform3d(
      int arg0, int arg1, double arg2, double arg3, double arg4) {
      // XXX not implemented       gl.glProgramUniform3d(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform3dv(
      int arg0, int arg1, int arg2, double[] arg3, int arg4) {
      // XXX not implemented       gl.glProgramUniform3dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform3dv(
      int arg0, int arg1, int arg2, DoubleBuffer arg3) {
      // XXX not implemented       gl.glProgramUniform3dv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform3f(
      int arg0, int arg1, float arg2, float arg3, float arg4) {
      // XXX not implemented       gl.glProgramUniform3f(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform3fv(
      int arg0, int arg1, int arg2, float[] arg3, int arg4) {
      // XXX not implemented       gl.glProgramUniform3fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform3fv(
      int arg0, int arg1, int arg2, FloatBuffer arg3) {
      // XXX not implemented       gl.glProgramUniform3fv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform3i(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      // XXX not implemented       gl.glProgramUniform3i(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform3iv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented       gl.glProgramUniform3iv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform3iv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented       gl.glProgramUniform3iv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform3ui(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      // XXX not implemented       gl.glProgramUniform3ui(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform3uiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented       gl.glProgramUniform3uiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform3uiv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented       gl.glProgramUniform3uiv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform4d(
      int arg0, int arg1, double arg2, double arg3, double arg4, double arg5) {
      // XXX not implemented       gl.glProgramUniform4d(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniform4dv(
      int arg0, int arg1, int arg2, double[] arg3, int arg4) {
      // XXX not implemented       gl.glProgramUniform4dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform4dv(
      int arg0, int arg1, int arg2, DoubleBuffer arg3) {
      // XXX not implemented       gl.glProgramUniform4dv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform4f(
      int arg0, int arg1, float arg2, float arg3, float arg4, float arg5) {
      // XXX not implemented       gl.glProgramUniform4f(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniform4fv(
      int arg0, int arg1, int arg2, float[] arg3, int arg4) {
      // XXX not implemented       gl.glProgramUniform4fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform4fv(
      int arg0, int arg1, int arg2, FloatBuffer arg3) {
      // XXX not implemented       gl.glProgramUniform4fv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform4i(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniform4i(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniform4iv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented       gl.glProgramUniform4iv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform4iv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented       gl.glProgramUniform4iv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniform4ui(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniform4ui(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniform4uiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      // XXX not implemented       gl.glProgramUniform4uiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniform4uiv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      // XXX not implemented       gl.glProgramUniform4uiv(arg0, arg1, arg2, arg3);
   }

   public void glProgramUniformMatrix2dv(
      int arg0, int arg1, int arg2, boolean arg3, double[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix2dv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix2dv(
      int arg0, int arg1, int arg2, boolean arg3, DoubleBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix2dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix2fv(
      int arg0, int arg1, int arg2, boolean arg3, float[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix2fv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix2fv(
      int arg0, int arg1, int arg2, boolean arg3, FloatBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix2fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix2x3dv(
      int arg0, int arg1, int arg2, boolean arg3, double[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix2x3dv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix2x3dv(
      int arg0, int arg1, int arg2, boolean arg3, DoubleBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix2x3dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix2x3fv(
      int arg0, int arg1, int arg2, boolean arg3, float[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix2x3fv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix2x3fv(
      int arg0, int arg1, int arg2, boolean arg3, FloatBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix2x3fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix2x4dv(
      int arg0, int arg1, int arg2, boolean arg3, double[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix2x4dv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix2x4dv(
      int arg0, int arg1, int arg2, boolean arg3, DoubleBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix2x4dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix2x4fv(
      int arg0, int arg1, int arg2, boolean arg3, float[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix2x4fv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix2x4fv(
      int arg0, int arg1, int arg2, boolean arg3, FloatBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix2x4fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix3dv(
      int arg0, int arg1, int arg2, boolean arg3, double[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix3dv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix3dv(
      int arg0, int arg1, int arg2, boolean arg3, DoubleBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix3dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix3fv(
      int arg0, int arg1, int arg2, boolean arg3, float[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix3fv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix3fv(
      int arg0, int arg1, int arg2, boolean arg3, FloatBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix3fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix3x2dv(
      int arg0, int arg1, int arg2, boolean arg3, double[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix3x2dv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix3x2dv(
      int arg0, int arg1, int arg2, boolean arg3, DoubleBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix3x2dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix3x2fv(
      int arg0, int arg1, int arg2, boolean arg3, float[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix3x2fv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix3x2fv(
      int arg0, int arg1, int arg2, boolean arg3, FloatBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix3x2fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix3x4dv(
      int arg0, int arg1, int arg2, boolean arg3, double[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix3x4dv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix3x4dv(
      int arg0, int arg1, int arg2, boolean arg3, DoubleBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix3x4dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix3x4fv(
      int arg0, int arg1, int arg2, boolean arg3, float[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix3x4fv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix3x4fv(
      int arg0, int arg1, int arg2, boolean arg3, FloatBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix3x4fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix4dv(
      int arg0, int arg1, int arg2, boolean arg3, double[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix4dv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix4dv(
      int arg0, int arg1, int arg2, boolean arg3, DoubleBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix4dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix4fv(
      int arg0, int arg1, int arg2, boolean arg3, float[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix4fv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix4fv(
      int arg0, int arg1, int arg2, boolean arg3, FloatBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix4fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix4x2dv(
      int arg0, int arg1, int arg2, boolean arg3, double[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix4x2dv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix4x2dv(
      int arg0, int arg1, int arg2, boolean arg3, DoubleBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix4x2dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix4x2fv(
      int arg0, int arg1, int arg2, boolean arg3, float[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix4x2fv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix4x2fv(
      int arg0, int arg1, int arg2, boolean arg3, FloatBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix4x2fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix4x3dv(
      int arg0, int arg1, int arg2, boolean arg3, double[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix4x3dv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix4x3dv(
      int arg0, int arg1, int arg2, boolean arg3, DoubleBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix4x3dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformMatrix4x3fv(
      int arg0, int arg1, int arg2, boolean arg3, float[] arg4, int arg5) {
      // XXX not implemented       gl.glProgramUniformMatrix4x3fv(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glProgramUniformMatrix4x3fv(
      int arg0, int arg1, int arg2, boolean arg3, FloatBuffer arg4) {
      // XXX not implemented       gl.glProgramUniformMatrix4x3fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glResumeTransformFeedback() {
      // XXX not implemented       gl.glResumeTransformFeedback();
   }

   public void glScissorArrayv(int arg0, int arg1, int[] arg2, int arg3) {
      // XXX not implemented       gl.glScissorArrayv(arg0, arg1, arg2, arg3);
   }

   public void glScissorArrayv(int arg0, int arg1, IntBuffer arg2) {
      // XXX not implemented       gl.glScissorArrayv(arg0, arg1, arg2);
   }

   public void glScissorIndexed(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      // XXX not implemented       gl.glScissorIndexed(arg0, arg1, arg2, arg3, arg4);
   }

   public void glScissorIndexedv(int arg0, int[] arg1, int arg2) {
      // XXX not implemented       gl.glScissorIndexedv(arg0, arg1, arg2);
   }

   public void glScissorIndexedv(int arg0, IntBuffer arg1) {
      // XXX not implemented       gl.glScissorIndexedv(arg0, arg1);
   }

   public void glTexBufferRange(
      int arg0, int arg1, int arg2, long arg3, long arg4) {
      // XXX not implemented       gl.glTexBufferRange(arg0, arg1, arg2, arg3, arg4);
   }

   public void glTexPageCommitmentARB(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, boolean arg8) {
      // XXX not implemented       gl.glTexPageCommitmentARB(
      // XXX not implemented          arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
   }

   public void glTexStorage2DMultisample(
      int arg0, int arg1, int arg2, int arg3, int arg4, boolean arg5) {
      // XXX not implemented       gl.glTexStorage2DMultisample(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glTexStorage3DMultisample(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5,
      boolean arg6) {
      // XXX not implemented       gl.glTexStorage3DMultisample(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glTextureStorage1DEXT(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      // XXX not implemented       gl.glTextureStorage1DEXT(arg0, arg1, arg2, arg3, arg4);
   }

   public void glTextureStorage2DEXT(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
      // XXX not implemented       gl.glTextureStorage2DEXT(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glTextureStorage3DEXT(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
      // XXX not implemented       gl.glTextureStorage3DEXT(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glUniform1d(int arg0, double arg1) {
      // XXX not implemented       gl.glUniform1d(arg0, arg1);
   }

   public void glUniform1dv(int arg0, int arg1, double[] arg2, int arg3) {
      // XXX not implemented       gl.glUniform1dv(arg0, arg1, arg2, arg3);
   }

   public void glUniform1dv(int arg0, int arg1, DoubleBuffer arg2) {
      // XXX not implemented       gl.glUniform1dv(arg0, arg1, arg2);
   }

   public void glUniform2d(int arg0, double arg1, double arg2) {
      // XXX not implemented       gl.glUniform2d(arg0, arg1, arg2);
   }

   public void glUniform2dv(int arg0, int arg1, double[] arg2, int arg3) {
      // XXX not implemented       gl.glUniform2dv(arg0, arg1, arg2, arg3);
   }

   public void glUniform2dv(int arg0, int arg1, DoubleBuffer arg2) {
      // XXX not implemented       gl.glUniform2dv(arg0, arg1, arg2);
   }

   public void glUniform3d(int arg0, double arg1, double arg2, double arg3) {
      // XXX not implemented       gl.glUniform3d(arg0, arg1, arg2, arg3);
   }

   public void glUniform3dv(int arg0, int arg1, double[] arg2, int arg3) {
      // XXX not implemented       gl.glUniform3dv(arg0, arg1, arg2, arg3);
   }

   public void glUniform3dv(int arg0, int arg1, DoubleBuffer arg2) {
      // XXX not implemented       gl.glUniform3dv(arg0, arg1, arg2);
   }

   public void glUniform4d(
      int arg0, double arg1, double arg2, double arg3, double arg4) {
      // XXX not implemented       gl.glUniform4d(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniform4dv(int arg0, int arg1, double[] arg2, int arg3) {
      // XXX not implemented       gl.glUniform4dv(arg0, arg1, arg2, arg3);
   }

   public void glUniform4dv(int arg0, int arg1, DoubleBuffer arg2) {
      // XXX not implemented       gl.glUniform4dv(arg0, arg1, arg2);
   }

   public void glUniformMatrix2dv(
      int arg0, int arg1, boolean arg2, double[] arg3, int arg4) {
      // XXX not implemented       gl.glUniformMatrix2dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix2dv(
      int arg0, int arg1, boolean arg2, DoubleBuffer arg3) {
      // XXX not implemented       gl.glUniformMatrix2dv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix2x3dv(
      int arg0, int arg1, boolean arg2, double[] arg3, int arg4) {
      // XXX not implemented       gl.glUniformMatrix2x3dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix2x3dv(
      int arg0, int arg1, boolean arg2, DoubleBuffer arg3) {
      // XXX not implemented       gl.glUniformMatrix2x3dv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix2x4dv(
      int arg0, int arg1, boolean arg2, double[] arg3, int arg4) {
      // XXX not implemented       gl.glUniformMatrix2x4dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix2x4dv(
      int arg0, int arg1, boolean arg2, DoubleBuffer arg3) {
      // XXX not implemented       gl.glUniformMatrix2x4dv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix3dv(
      int arg0, int arg1, boolean arg2, double[] arg3, int arg4) {
      // XXX not implemented       gl.glUniformMatrix3dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix3dv(
      int arg0, int arg1, boolean arg2, DoubleBuffer arg3) {
      // XXX not implemented       gl.glUniformMatrix3dv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix3x2dv(
      int arg0, int arg1, boolean arg2, double[] arg3, int arg4) {
      // XXX not implemented       gl.glUniformMatrix3x2dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix3x2dv(
      int arg0, int arg1, boolean arg2, DoubleBuffer arg3) {
      // XXX not implemented       gl.glUniformMatrix3x2dv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix3x4dv(
      int arg0, int arg1, boolean arg2, double[] arg3, int arg4) {
      // XXX not implemented       gl.glUniformMatrix3x4dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix3x4dv(
      int arg0, int arg1, boolean arg2, DoubleBuffer arg3) {
      // XXX not implemented       gl.glUniformMatrix3x4dv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix4dv(
      int arg0, int arg1, boolean arg2, double[] arg3, int arg4) {
      // XXX not implemented       gl.glUniformMatrix4dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix4dv(
      int arg0, int arg1, boolean arg2, DoubleBuffer arg3) {
      // XXX not implemented       gl.glUniformMatrix4dv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix4x2dv(
      int arg0, int arg1, boolean arg2, double[] arg3, int arg4) {
      // XXX not implemented       gl.glUniformMatrix4x2dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix4x2dv(
      int arg0, int arg1, boolean arg2, DoubleBuffer arg3) {
      // XXX not implemented       gl.glUniformMatrix4x2dv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix4x3dv(
      int arg0, int arg1, boolean arg2, double[] arg3, int arg4) {
      // XXX not implemented       gl.glUniformMatrix4x3dv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix4x3dv(
      int arg0, int arg1, boolean arg2, DoubleBuffer arg3) {
      // XXX not implemented       gl.glUniformMatrix4x3dv(arg0, arg1, arg2, arg3);
   }

   public void glUniformSubroutinesuiv(
      int arg0, int arg1, int[] arg2, int arg3) {
      // XXX not implemented       gl.glUniformSubroutinesuiv(arg0, arg1, arg2, arg3);
   }

   public void glUniformSubroutinesuiv(int arg0, int arg1, IntBuffer arg2) {
      // XXX not implemented       gl.glUniformSubroutinesuiv(arg0, arg1, arg2);
   }

   public void glUseProgramStages(int arg0, int arg1, int arg2) {
      // XXX not implemented       gl.glUseProgramStages(arg0, arg1, arg2);
   }

   public void glValidateProgramPipeline(int arg0) {
      // XXX not implemented       gl.glValidateProgramPipeline(arg0);
   }

   public void glVertexAttribBinding(int arg0, int arg1) {
      // XXX not implemented       gl.glVertexAttribBinding(arg0, arg1);
   }

   public void glVertexAttribFormat(
      int arg0, int arg1, int arg2, boolean arg3, int arg4) {
      // XXX not implemented       gl.glVertexAttribFormat(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttribIFormat(int arg0, int arg1, int arg2, int arg3) {
      // XXX not implemented       gl.glVertexAttribIFormat(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribL1d(int arg0, double arg1) {
      // XXX not implemented       gl.glVertexAttribL1d(arg0, arg1);
   }

   public void glVertexAttribL1dv(int arg0, double[] arg1, int arg2) {
      // XXX not implemented       gl.glVertexAttribL1dv(arg0, arg1, arg2);
   }

   public void glVertexAttribL1dv(int arg0, DoubleBuffer arg1) {
      // XXX not implemented       gl.glVertexAttribL1dv(arg0, arg1);
   }

   public void glVertexAttribL2d(int arg0, double arg1, double arg2) {
      // XXX not implemented       gl.glVertexAttribL2d(arg0, arg1, arg2);
   }

   public void glVertexAttribL2dv(int arg0, double[] arg1, int arg2) {
      // XXX not implemented       gl.glVertexAttribL2dv(arg0, arg1, arg2);
   }

   public void glVertexAttribL2dv(int arg0, DoubleBuffer arg1) {
      // XXX not implemented       gl.glVertexAttribL2dv(arg0, arg1);
   }

   public void glVertexAttribL3d(
      int arg0, double arg1, double arg2, double arg3) {
      // XXX not implemented       gl.glVertexAttribL3d(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribL3dv(int arg0, double[] arg1, int arg2) {
      // XXX not implemented       gl.glVertexAttribL3dv(arg0, arg1, arg2);
   }

   public void glVertexAttribL3dv(int arg0, DoubleBuffer arg1) {
      // XXX not implemented       gl.glVertexAttribL3dv(arg0, arg1);
   }

   public void glVertexAttribL4d(
      int arg0, double arg1, double arg2, double arg3, double arg4) {
      // XXX not implemented       gl.glVertexAttribL4d(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttribL4dv(int arg0, double[] arg1, int arg2) {
      // XXX not implemented       gl.glVertexAttribL4dv(arg0, arg1, arg2);
   }

   public void glVertexAttribL4dv(int arg0, DoubleBuffer arg1) {
      // XXX not implemented       gl.glVertexAttribL4dv(arg0, arg1);
   }

   public void glVertexAttribLPointer(
      int arg0, int arg1, int arg2, int arg3, long arg4) {
      // XXX not implemented       gl.glVertexAttribLPointer(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexBindingDivisor(int arg0, int arg1) {
      // XXX not implemented       gl.glVertexBindingDivisor(arg0, arg1);
   }

   public void glViewportArrayv(int arg0, int arg1, float[] arg2, int arg3) {
      // XXX not implemented       gl.glViewportArrayv(arg0, arg1, arg2, arg3);
   }

   public void glViewportArrayv(int arg0, int arg1, FloatBuffer arg2) {
      // XXX not implemented       gl.glViewportArrayv(arg0, arg1, arg2);
   }

   public void glViewportIndexedf(
      int arg0, float arg1, float arg2, float arg3, float arg4) {
      // XXX not implemented       gl.glViewportIndexedf(arg0, arg1, arg2, arg3, arg4);
   }

   public void glViewportIndexedfv(int arg0, float[] arg1, int arg2) {
      // XXX not implemented       gl.glViewportIndexedfv(arg0, arg1, arg2);
   }

   public void glViewportIndexedfv(int arg0, FloatBuffer arg1) {
      // XXX not implemented       gl.glViewportIndexedfv(arg0, arg1);
   }

   public boolean isGLES31Compatible() {
      // XXX not implemented return gl.isGLES31Compatible();
      return false;
   }

   public boolean isGLES32Compatible() {
      // XXX not implemented  return gl.isGLES32Compatible();
      return false;
   }

}
