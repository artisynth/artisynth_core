package maspack.render.GL.GL3;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2ES3;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GL3ES3;
import javax.media.opengl.GL3bc;
import javax.media.opengl.GL4;
import javax.media.opengl.GL4ES3;
import javax.media.opengl.GL4bc;
import javax.media.opengl.GLArrayData;
import javax.media.opengl.GLBufferStorage;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLES1;
import javax.media.opengl.GLES2;
import javax.media.opengl.GLES3;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLUniformData;

import com.jogamp.common.nio.PointerBuffer;

public class GL3Debug implements GL3 {

   GL3 gl;
   
   public GL3Debug(GL3 gl) {
      this.gl = gl;
   }
   
   public GL getDownstreamGL() throws GLException {
      return gl.getDownstreamGL();
   }

   public GL getRootGL() throws GLException {
      return gl.getRootGL();
   }

   public GL getGL() throws GLException {
      return gl.getGL();
   }

   public GL4bc getGL4bc() throws GLException {
      return gl.getGL4bc();
   }

   public GL4 getGL4() throws GLException {
      return gl.getGL4();
   }

   public GL3bc getGL3bc() throws GLException {
      return gl.getGL3bc();
   }

   public GL3 getGL3() throws GLException {
      return gl.getGL3();
   }

   public GL2 getGL2() throws GLException {
      return gl.getGL2();
   }

   public GLES1 getGLES1() throws GLException {
      return gl.getGLES1();
   }

   public GLES2 getGLES2() throws GLException {
      return gl.getGLES2();
   }

   public GLES3 getGLES3() throws GLException {
      return gl.getGLES3();
   }

   public GL2ES1 getGL2ES1() throws GLException {
      return gl.getGL2ES1();
   }

   public GL2ES2 getGL2ES2() throws GLException {
      return gl.getGL2ES2();
   }

   public GL2ES3 getGL2ES3() throws GLException {
      return gl.getGL2ES3();
   }

   public GL3ES3 getGL3ES3() throws GLException {
      return gl.getGL3ES3();
   }

   public GL4ES3 getGL4ES3() throws GLException {
      return gl.getGL4ES3();
   }

   public GL2GL3 getGL2GL3() throws GLException {
      return gl.getGL2GL3();
   }

   public GLProfile getGLProfile() {
      return gl.getGLProfile();
   }

   public GLContext getContext() {
      return gl.getContext();
   }

   public int getMaxRenderbufferSamples() {
      return gl.getMaxRenderbufferSamples();
   }

   public int getSwapInterval() {
      return gl.getSwapInterval();
   }

   public Object getPlatformGLExtensions() {
      return gl.getPlatformGLExtensions();
   }

   public Object getExtension(String extensionName) {
      return gl.getExtension(extensionName);
   }

   public int getBoundBuffer(int target) {
      return gl.getBoundBuffer(target);
   }

   public GLBufferStorage getBufferStorage(int bufferName) {
      return gl.getBufferStorage(bufferName);
   }

   public int getBoundFramebuffer(int target) {
      return gl.getBoundFramebuffer(target);
   }

   public int getDefaultDrawFramebuffer() {
      return gl.getDefaultDrawFramebuffer();
   }

   public int getDefaultReadFramebuffer() {
      return gl.getDefaultReadFramebuffer();
   }

   public int getDefaultReadBuffer() {
      return gl.getDefaultReadBuffer();
   }

   public void glActiveTexture(int arg0) {
      System.out.println("glActiveTexture(" + arg0 + ")");
      gl.glActiveTexture(arg0);
   }

   public void glAttachShader(int arg0, int arg1) {
      System.out.println("glAttachShader(" + arg0 + "," + arg1 + ")");
      gl.glAttachShader(arg0, arg1);
   }

   public void glBeginConditionalRender(int arg0, int arg1) {
      System.out.println("glBeginConditionalRender(" + arg0 + "," + arg1 + ")");
      gl.glBeginConditionalRender(arg0, arg1);
   }

   public void glBeginQuery(int arg0, int arg1) {
      System.out.println("glBeginQuery(" + arg0 + "," + arg1 + ")");
      gl.glBeginQuery(arg0, arg1);
   }

   public void glBeginTransformFeedback(int arg0) {
      System.out.println("glBeginTransformFeedback(" + arg0 + ")");
      gl.glBeginTransformFeedback(arg0);
   }

   public void glBindAttribLocation(int arg0, int arg1, String arg2) {
      System.out.println("glBindAttribLocation(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glBindAttribLocation(arg0, arg1, arg2);
   }

   public void glBindBuffer(int arg0, int arg1) {
      System.out.println("glBindBuffer(" + arg0 + "," + arg1 + ")");
      gl.glBindBuffer(arg0, arg1);
   }

   public void glBindBufferBase(int arg0, int arg1, int arg2) {
      System.out.println("glBindBufferBase(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glBindBufferBase(arg0, arg1, arg2);
   }

   public void glBindBufferRange(
      int arg0, int arg1, int arg2, long arg3, long arg4) {
      System.out.println("glBindBufferRange(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glBindBufferRange(arg0, arg1, arg2, arg3, arg4);
   }

   public void glBindFragDataLocation(int arg0, int arg1, String arg2) {
      System.out.println("glBindFragDataLocation(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glBindFragDataLocation(arg0, arg1, arg2);
   }

   public void glBindFragDataLocationIndexed(
      int arg0, int arg1, int arg2, String arg3) {
      System.out.println("glBindFragDataLocationIndexed(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glBindFragDataLocationIndexed(arg0, arg1, arg2, arg3);
   }

   public void glBindFramebuffer(int arg0, int arg1) {
      System.out.println("glBindFramebuffer(" + arg0 + "," + arg1 + ")");
      gl.glBindFramebuffer(arg0, arg1);
   }

   public void glBindRenderbuffer(int arg0, int arg1) {
      System.out.println("glBindRenderbuffer(" + arg0 + "," + arg1 + ")");
      gl.glBindRenderbuffer(arg0, arg1);
   }

   public void glBindSampler(int arg0, int arg1) {
      System.out.println("glBindSampler(" + arg0 + "," + arg1 + ")");
      gl.glBindSampler(arg0, arg1);
   }

   public void glBindTexture(int arg0, int arg1) {
      System.out.println("glBindTexture(" + arg0 + "," + arg1 + ")");
      gl.glBindTexture(arg0, arg1);
   }

   public void glBindVertexArray(int arg0) {
      System.out.println("glBindVertexArray(" + arg0 + ")");
      gl.glBindVertexArray(arg0);
   }

   public void glBlendColor(float arg0, float arg1, float arg2, float arg3) {
      System.out.println("glBlendColor(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glBlendColor(arg0, arg1, arg2, arg3);
   }

   public void glBlendEquation(int arg0) {
      System.out.println("glBlendEquation(" + arg0 + ")");
      gl.glBlendEquation(arg0);
   }

   public void glBlendEquationSeparate(int arg0, int arg1) {
      System.out.println("glBlendEquationSeparate(" + arg0 + "," + arg1 + ")");
      gl.glBlendEquationSeparate(arg0, arg1);
   }

   public void glBlendFunc(int arg0, int arg1) {
      System.out.println("glBlendFunc(" + arg0 + "," + arg1 + ")");
      gl.glBlendFunc(arg0, arg1);
   }

   public void glBlendFuncSeparate(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glBlendFuncSeparate(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glBlendFuncSeparate(arg0, arg1, arg2, arg3);
   }

   public void glBlitFramebuffer(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, int arg9) {
      System.out.println("glBlitFramebuffer(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," 
         + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8 + "," + arg9 +")");
      gl.glBlitFramebuffer(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
   }

   public void glBufferAddressRangeNV(
      int arg0, int arg1, long arg2, long arg3) {
      System.out.println("glBufferAddressRangeNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glBufferAddressRangeNV(arg0, arg1, arg2, arg3);
   }

   public void glBufferData(int arg0, long arg1, Buffer arg2, int arg3) {
      System.out.println("glBufferData(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glBufferData(arg0, arg1, arg2, arg3);
   }

   public void glBufferSubData(int arg0, long arg1, long arg2, Buffer arg3) {
      System.out.println("glBufferSubData(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glBufferSubData(arg0, arg1, arg2, arg3);
   }

   public int glCheckFramebufferStatus(int arg0) {
      System.out.println("glCheckFramebufferStatus(" + arg0 + ")");
      return gl.glCheckFramebufferStatus(arg0);
   }

   public void glClampColor(int arg0, int arg1) {
      System.out.println("glClampColor(" + arg0 + "," + arg1 + ")");
      gl.glClampColor(arg0, arg1);
   }

   public void glClear(int arg0) {
      System.out.println("glClear(" + arg0 + ")");
      gl.glClear(arg0);
   }

   public void glClearBufferfi(int arg0, int arg1, float arg2, int arg3) {
      System.out.println("glClearBufferfi(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glClearBufferfi(arg0, arg1, arg2, arg3);
   }

   public void glClearBufferfv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glClearBufferfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glClearBufferfv(arg0, arg1, arg2, arg3);
   }

   public void glClearBufferfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glClearBufferfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glClearBufferfv(arg0, arg1, arg2);
   }

   public void glClearBufferiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glClearBufferiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glClearBufferiv(arg0, arg1, arg2, arg3);
   }

   public void glClearBufferiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glClearBufferiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glClearBufferiv(arg0, arg1, arg2);
   }

   public void glClearBufferuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glClearBufferuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glClearBufferuiv(arg0, arg1, arg2, arg3);
   }

   public void glClearBufferuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glClearBufferuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glClearBufferuiv(arg0, arg1, arg2);
   }

   public void glClearColor(float arg0, float arg1, float arg2, float arg3) {
      System.out.println("glClearColor(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glClearColor(arg0, arg1, arg2, arg3);
   }

   public void glClearDepth(double arg0) {
      System.out.println("glClearDepth(" + arg0 + ")");
      gl.glClearDepth(arg0);
   }

   public void glClearDepthf(float arg0) {
      System.out.println("glClearDepthf(" + arg0 + ")");
      gl.glClearDepthf(arg0);
   }

   public void glClearNamedBufferDataEXT(
      int arg0, int arg1, int arg2, int arg3, Buffer arg4) {
      System.out.println("glClearNamedBufferDataEXT(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glClearNamedBufferDataEXT(arg0, arg1, arg2, arg3, arg4);
   }

   public void glClearNamedBufferSubDataEXT(
      int arg0, int arg1, int arg2, int arg3, long arg4, long arg5,
      Buffer arg6) {
      gl.glClearNamedBufferSubDataEXT(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glClearStencil(int arg0) {
      System.out.println("glClearStencil(" + arg0 + ")");
      gl.glClearStencil(arg0);
   }

   public int glClientWaitSync(long arg0, int arg1, long arg2) {
      System.out.println("glClientWaitSync(" + arg0 + "," + arg1 + "," + arg2 + ")");
      return gl.glClientWaitSync(arg0, arg1, arg2);
   }

   public void glColorFormatNV(int arg0, int arg1, int arg2) {
      System.out.println("glColorFormatNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glColorFormatNV(arg0, arg1, arg2);
   }

   public void glColorMask(
      boolean arg0, boolean arg1, boolean arg2, boolean arg3) {
      System.out.println("glColorMask(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glColorMask(arg0, arg1, arg2, arg3);
   }

   public void glColorMaski(
      int arg0, boolean arg1, boolean arg2, boolean arg3, boolean arg4) {
      System.out.println("glColorMaski(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glColorMaski(arg0, arg1, arg2, arg3, arg4);
   }

   public void glColorP3ui(int arg0, int arg1) {
      System.out.println("glColorP3ui(" + arg0 + "," + arg1 + ")");
      gl.glColorP3ui(arg0, arg1);
   }

   public void glColorP3uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glColorP3uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glColorP3uiv(arg0, arg1, arg2);
   }

   public void glColorP3uiv(int arg0, IntBuffer arg1) {
      System.out.println("glColorP3uiv(" + arg0 + "," + arg1 + ")");
      gl.glColorP3uiv(arg0, arg1);
   }

   public void glColorP4ui(int arg0, int arg1) {
      System.out.println("glColorP4ui(" + arg0 + "," + arg1 + ")");
      gl.glColorP4ui(arg0, arg1);
   }

   public void glColorP4uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glColorP4uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glColorP4uiv(arg0, arg1, arg2);
   }

   public void glColorP4uiv(int arg0, IntBuffer arg1) {
      System.out.println("glColorP4uiv(" + arg0 + "," + arg1 + ")");
      gl.glColorP4uiv(arg0, arg1);
   }

   public void glCompileShader(int arg0) {
      System.out.println("glCompileShader(" + arg0 + ")");
      gl.glCompileShader(arg0);
   }

   public void glCompileShaderIncludeARB(
      int arg0, int arg1, String[] arg2, int[] arg3, int arg4) {
      System.out.println("glCompileShaderIncludeARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glCompileShaderIncludeARB(arg0, arg1, arg2, arg3, arg4);
   }

   public void glCompileShaderIncludeARB(
      int arg0, int arg1, String[] arg2, IntBuffer arg3) {
      System.out.println("glCompileShaderIncludeARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glCompileShaderIncludeARB(arg0, arg1, arg2, arg3);
   }

   public void glCompressedTexImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, Buffer arg6) {
      System.out.println("glCompressedTexImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glCompressedTexImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glCompressedTexImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, long arg6) {
      System.out.println("glCompressedTexImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glCompressedTexImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glCompressedTexImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      Buffer arg7) {
      System.out.println("glCompressedTexImage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ")");
      gl.glCompressedTexImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glCompressedTexImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      long arg7) {
      System.out.println("glCompressedTexImage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ")");
      gl.glCompressedTexImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glCompressedTexImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, Buffer arg8) {
      System.out.println("glCompressedTexImage3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8 + ")");
      gl.glCompressedTexImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
   }

   public void glCompressedTexImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, long arg8) {
      System.out.println("glCompressedTexImage3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8 + ")");
      gl.glCompressedTexImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
   }

   public void glCompressedTexSubImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, Buffer arg6) {
      System.out.println("glCompressedTexSubImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glCompressedTexSubImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glCompressedTexSubImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, long arg6) {
      System.out.println("glCompressedTexSubImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glCompressedTexSubImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glCompressedTexSubImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, Buffer arg8) {
      System.out.println("glCompressedTexSubImage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8 + ")");
      gl.glCompressedTexSubImage2D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
   }

   public void glCompressedTexSubImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, long arg8) {
      System.out.println("glCompressedTexSubImage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8 + ")");
      gl.glCompressedTexSubImage2D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
   }

   public void glCompressedTexSubImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, int arg9, Buffer arg10) {
      System.out.println("glCompressedTexSubImage3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8 + "," + arg9 + "," + arg10 + ")");
      gl.glCompressedTexSubImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
   }

   public void glCompressedTexSubImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, int arg9, long arg10) {
      System.out.println("glCompressedTexSubImage3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8 + "," + arg9 + "," + arg10 + ")");
      gl.glCompressedTexSubImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
   }

   public void glCopyBufferSubData(
      int arg0, int arg1, long arg2, long arg3, long arg4) {
      System.out.println("glCopyBufferSubData(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glCopyBufferSubData(arg0, arg1, arg2, arg3, arg4);
   }

   public void glCopyTexImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
      System.out.println("glCopyTexImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glCopyTexImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glCopyTexImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7) {
      System.out.println("glCopyTexImage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ")");
      gl.glCopyTexImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glCopyTexSubImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
      System.out.println("glCopyTexSubImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glCopyTexSubImage1D(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glCopyTexSubImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7) {
      System.out.println("glCopyTexSubImage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + ")");
      gl.glCopyTexSubImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glCopyTexSubImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8) {
      System.out.println("glCopyTexSubImage3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8 + ")");
      gl.glCopyTexSubImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
   }

   public int glCreateProgram() {
      System.out.println("glCreateProgram()");
      return gl.glCreateProgram();
   }

   public int glCreateShader(int arg0) {
      System.out.println("glCreateShader(" + arg0 + ")");
      return gl.glCreateShader(arg0);
   }

   public long glCreateSyncFromCLeventARB(long arg0, long arg1, int arg2) {
      System.out.println("glCreateSyncFromCLeventARB(" + arg0 + "," + arg1 + "," + arg2 + ")");
      return gl.glCreateSyncFromCLeventARB(arg0, arg1, arg2);
   }

   public void glCullFace(int arg0) {
      System.out.println("glCullFace(" + arg0 + ")");
      gl.glCullFace(arg0);
   }

   public void glDebugMessageControl(
      int arg0, int arg1, int arg2, int arg3, int[] arg4, int arg5,
      boolean arg6) {
      System.out.println("glDebugMessageControl(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glDebugMessageControl(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glDebugMessageControl(
      int arg0, int arg1, int arg2, int arg3, IntBuffer arg4, boolean arg5) {
      System.out.println("glDebugMessageControl(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glDebugMessageControl(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glDebugMessageEnableAMD(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, boolean arg5) {
      System.out.println("glDebugMessageEnableAMD(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glDebugMessageEnableAMD(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glDebugMessageEnableAMD(
      int arg0, int arg1, int arg2, IntBuffer arg3, boolean arg4) {
      System.out.println("glDebugMessageEnableAMD(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glDebugMessageEnableAMD(arg0, arg1, arg2, arg3, arg4);
   }

   public void glDebugMessageInsert(
      int arg0, int arg1, int arg2, int arg3, int arg4, String arg5) {
      System.out.println("glDebugMessageInsert(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glDebugMessageInsert(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glDebugMessageInsertAMD(
      int arg0, int arg1, int arg2, int arg3, String arg4) {
      System.out.println("glDebugMessageInsertAMD(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glDebugMessageInsertAMD(arg0, arg1, arg2, arg3, arg4);
   }

   public void glDeleteBuffers(int arg0, int[] arg1, int arg2) {
      System.out.println("glDeleteBuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteBuffers(arg0, arg1, arg2);
   }

   public void glDeleteBuffers(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteBuffers(" + arg0 + "," + arg1 + ")");
      gl.glDeleteBuffers(arg0, arg1);
   }

   public void glDeleteFramebuffers(int arg0, int[] arg1, int arg2) {
      System.out.println("glDeleteFramebuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteFramebuffers(arg0, arg1, arg2);
   }

   public void glDeleteFramebuffers(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteFramebuffers(" + arg0 + "," + arg1 + ")");
      gl.glDeleteFramebuffers(arg0, arg1);
   }

   public void glDeleteNamedStringARB(int arg0, String arg1) {
      System.out.println("glDeleteNamedStringARB(" + arg0 + "," + arg1 + ")");
      gl.glDeleteNamedStringARB(arg0, arg1);
   }

   public void glDeleteProgram(int arg0) {
      System.out.println("glDeleteProgram(" + arg0 + ")");
      gl.glDeleteProgram(arg0);
   }

   public void glDeleteQueries(int arg0, int[] arg1, int arg2) {
      System.out.println("glDeleteQueries(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteQueries(arg0, arg1, arg2);
   }

   public void glDeleteQueries(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteQueries(" + arg0 + "," + arg1 + ")");
      gl.glDeleteQueries(arg0, arg1);
   }

   public void glDeleteRenderbuffers(int arg0, int[] arg1, int arg2) {
      System.out.println("glDeleteRenderbuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteRenderbuffers(arg0, arg1, arg2);
   }

   public void glDeleteRenderbuffers(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteRenderbuffers(" + arg0 + "," + arg1 + ")");
      gl.glDeleteRenderbuffers(arg0, arg1);
   }

   public void glDeleteSamplers(int arg0, int[] arg1, int arg2) {
      System.out.println("glDeleteSamplers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteSamplers(arg0, arg1, arg2);
   }

   public void glDeleteSamplers(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteSamplers(" + arg0 + "," + arg1 + ")");
      gl.glDeleteSamplers(arg0, arg1);
   }

   public void glDeleteShader(int arg0) {
      System.out.println("glDeleteShader(" + arg0 + ")");
      gl.glDeleteShader(arg0);
   }

   public void glDeleteSync(long arg0) {
      System.out.println("glDeleteSync(" + arg0 + ")");
      gl.glDeleteSync(arg0);
   }

   public void glDeleteTextures(int arg0, int[] arg1, int arg2) {
      System.out.println("glDeleteTextures(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteTextures(arg0, arg1, arg2);
   }

   public void glDeleteTextures(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteTextures(" + arg0 + "," + arg1 + ")");
      gl.glDeleteTextures(arg0, arg1);
   }

   public void glDeleteVertexArrays(int arg0, int[] arg1, int arg2) {
      System.out.println("glDeleteVertexArrays(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDeleteVertexArrays(arg0, arg1, arg2);
   }

   public void glDeleteVertexArrays(int arg0, IntBuffer arg1) {
      System.out.println("glDeleteVertexArrays(" + arg0 + "," + arg1 + ")");
      gl.glDeleteVertexArrays(arg0, arg1);
   }

   public void glDepthFunc(int arg0) {
      System.out.println("glDepthFunc(" + arg0 + ")");
      gl.glDepthFunc(arg0);
   }

   public void glDepthMask(boolean arg0) {
      System.out.println("glDepthMask(" + arg0 + ")");
      gl.glDepthMask(arg0);
   }

   public void glDepthRange(double arg0, double arg1) {
      System.out.println("glDepthRange(" + arg0 + "," + arg1 + ")");
      gl.glDepthRange(arg0, arg1);
   }

   public void glDepthRangef(float arg0, float arg1) {
      System.out.println("glDepthRangef(" + arg0 + "," + arg1 + ")");
      gl.glDepthRangef(arg0, arg1);
   }

   public void glDetachShader(int arg0, int arg1) {
      System.out.println("glDetachShader(" + arg0 + "," + arg1 + ")");
      gl.glDetachShader(arg0, arg1);
   }

   public void glDisable(int arg0) {
      System.out.println("glDisable(" + arg0 + ")");
      gl.glDisable(arg0);
   }

   public void glDisableClientState(int arg0) {
      System.out.println("glDisableClientState(" + arg0 + ")");
      gl.glDisableClientState(arg0);
   }

   public void glDisableVertexAttribArray(int arg0) {
      System.out.println("glDisableVertexAttribArray(" + arg0 + ")");
      gl.glDisableVertexAttribArray(arg0);
   }

   public void glDisablei(int arg0, int arg1) {
      System.out.println("glDisablei(" + arg0 + "," + arg1 + ")");
      gl.glDisablei(arg0, arg1);
   }

   public void glDrawArrays(int arg0, int arg1, int arg2) {
      System.out.println("glDrawArrays(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDrawArrays(arg0, arg1, arg2);
   }

   public void glDrawArraysInstanced(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glDrawArraysInstanced(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glDrawArraysInstanced(arg0, arg1, arg2, arg3);
   }

   public void glDrawBuffer(int arg0) {
      System.out.println("glDrawBuffer(" + arg0 + ")");
      gl.glDrawBuffer(arg0);
   }

   public void glDrawBuffers(int arg0, int[] arg1, int arg2) {
      System.out.println("glDrawBuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glDrawBuffers(arg0, arg1, arg2);
   }

   public void glDrawBuffers(int arg0, IntBuffer arg1) {
      System.out.println("glDrawBuffers(" + arg0 + "," + arg1 + ")");
      gl.glDrawBuffers(arg0, arg1);
   }

   public void glDrawElements(int arg0, int arg1, int arg2, long arg3) {
      System.out.println("glDrawElements(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glDrawElements(arg0, arg1, arg2, arg3);
   }

   public void glDrawElementsBaseVertex(
      int arg0, int arg1, int arg2, long arg3, int arg4) {
      System.out.println("glDrawElementsBaseVertex(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glDrawElementsBaseVertex(arg0, arg1, arg2, arg3, arg4);
   }

   public void glDrawElementsInstanced(
      int arg0, int arg1, int arg2, long arg3, int arg4) {
      System.out.println("glDrawElementsInstanced(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glDrawElementsInstanced(arg0, arg1, arg2, arg3, arg4);
   }

   public void glDrawElementsInstancedBaseVertex(
      int arg0, int arg1, int arg2, long arg3, int arg4, int arg5) {
      System.out.println("glDrawElementsInstancedBaseVertex(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glDrawElementsInstancedBaseVertex(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glDrawRangeElements(
      int arg0, int arg1, int arg2, int arg3, int arg4, long arg5) {
      System.out.println("glDrawRangeElements(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glDrawRangeElements(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glDrawRangeElementsBaseVertex(
      int arg0, int arg1, int arg2, int arg3, int arg4, long arg5, int arg6) {
      System.out.println("glDrawRangeElementsBaseVertex(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glDrawRangeElementsBaseVertex(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glEdgeFlagFormatNV(int arg0) {
      System.out.println("glEdgeFlagFormatNV(" + arg0 + ")");
      gl.glEdgeFlagFormatNV(arg0);
   }

   public void glEnable(int arg0) {
      System.out.println("glEnable(" + arg0 + ")");
      gl.glEnable(arg0);
   }

   public void glEnableClientState(int arg0) {
      System.out.println("glEnableClientState(" + arg0 + ")");
      gl.glEnableClientState(arg0);
   }

   public void glEnableVertexAttribArray(int arg0) {
      System.out.println("glEnableVertexAttribArray(" + arg0 + ")");
      gl.glEnableVertexAttribArray(arg0);
   }

   public void glEnablei(int arg0, int arg1) {
      System.out.println("glEnablei(" + arg0 + "," + arg1 + ")");
      gl.glEnablei(arg0, arg1);
   }

   public void glEndConditionalRender() {
      System.out.println("glEndConditionalRender()");
      gl.glEndConditionalRender();
   }

   public void glEndQuery(int arg0) {
      System.out.println("glEndQuery(" + arg0 + ")");
      gl.glEndQuery(arg0);
   }

   public void glEndTransformFeedback() {
      System.out.println("glEndTransformFeedback()");
      gl.glEndTransformFeedback();
   }

   public long glFenceSync(int arg0, int arg1) {
      System.out.println("glFenceSync(" + arg0 + "," + arg1 + ")");
      return gl.glFenceSync(arg0, arg1);
   }

   public void glFinish() {
      System.out.println("glFinish()");
      gl.glFinish();
   }

   public void glFlush() {
      System.out.println("glFlush()");
      gl.glFlush();
   }

   public void glFlushMappedBufferRange(int arg0, long arg1, long arg2) {
      System.out.println("glFlushMappedBufferRange(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glFlushMappedBufferRange(arg0, arg1, arg2);
   }

   public void glFogCoordFormatNV(int arg0, int arg1) {
      System.out.println("glFogCoordFormatNV(" + arg0 + "," + arg1 + ")");
      gl.glFogCoordFormatNV(arg0, arg1);
   }

   public void glFramebufferRenderbuffer(
      int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glFramebufferRenderbuffer(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glFramebufferRenderbuffer(arg0, arg1, arg2, arg3);
   }

   public void glFramebufferTexture(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glFramebufferTexture(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glFramebufferTexture(arg0, arg1, arg2, arg3);
   }

   public void glFramebufferTexture1D(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glFramebufferTexture1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glFramebufferTexture1D(arg0, arg1, arg2, arg3, arg4);
   }

   public void glFramebufferTexture2D(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glFramebufferTexture2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glFramebufferTexture2D(arg0, arg1, arg2, arg3, arg4);
   }

   public void glFramebufferTexture3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
      System.out.println("glFramebufferTexture3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glFramebufferTexture3D(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glFramebufferTextureARB(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glFramebufferTextureARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glFramebufferTextureARB(arg0, arg1, arg2, arg3);
   }

   public void glFramebufferTextureFaceARB(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glFramebufferTextureFaceARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glFramebufferTextureFaceARB(arg0, arg1, arg2, arg3, arg4);
   }

   public void glFramebufferTextureLayer(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glFramebufferTextureLayer(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glFramebufferTextureLayer(arg0, arg1, arg2, arg3, arg4);
   }

   public void glFramebufferTextureLayerARB(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glFramebufferTextureLayerARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glFramebufferTextureLayerARB(arg0, arg1, arg2, arg3, arg4);
   }

   public void glFrontFace(int arg0) {
      System.out.println("glFrontFace(" + arg0 + ")");
      gl.glFrontFace(arg0);
   }

   public void glGenBuffers(int arg0, int[] arg1, int arg2) {
      System.out.println("glGenBuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenBuffers(arg0, arg1, arg2);
   }

   public void glGenBuffers(int arg0, IntBuffer arg1) {
      System.out.println("glGenBuffers(" + arg0 + "," + arg1 + ")");
      gl.glGenBuffers(arg0, arg1);
   }

   public void glGenFramebuffers(int arg0, int[] arg1, int arg2) {
      System.out.println("glGenFramebuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenFramebuffers(arg0, arg1, arg2);
   }

   public void glGenFramebuffers(int arg0, IntBuffer arg1) {
      System.out.println("glGenFramebuffers(" + arg0 + "," + arg1 + ")");
      gl.glGenFramebuffers(arg0, arg1);
   }

   public void glGenQueries(int arg0, int[] arg1, int arg2) {
      System.out.println("glGenQueries(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenQueries(arg0, arg1, arg2);
   }

   public void glGenQueries(int arg0, IntBuffer arg1) {
      System.out.println("glGenQueries(" + arg0 + "," + arg1 + ")");
      gl.glGenQueries(arg0, arg1);
   }

   public void glGenRenderbuffers(int arg0, int[] arg1, int arg2) {
      System.out.println("glGenRenderbuffers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenRenderbuffers(arg0, arg1, arg2);
   }

   public void glGenRenderbuffers(int arg0, IntBuffer arg1) {
      System.out.println("glGenRenderbuffers(" + arg0 + "," + arg1 + ")");
      gl.glGenRenderbuffers(arg0, arg1);
   }

   public void glGenSamplers(int arg0, int[] arg1, int arg2) {
      System.out.println("glGenSamplers(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenSamplers(arg0, arg1, arg2);
   }

   public void glGenSamplers(int arg0, IntBuffer arg1) {
      System.out.println("glGenSamplers(" + arg0 + "," + arg1 + ")");
      gl.glGenSamplers(arg0, arg1);
   }

   public void glGenTextures(int arg0, int[] arg1, int arg2) {
      System.out.println("glGenTextures(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenTextures(arg0, arg1, arg2);
   }

   public void glGenTextures(int arg0, IntBuffer arg1) {
      System.out.println("glGenTextures(" + arg0 + "," + arg1 + ")");
      gl.glGenTextures(arg0, arg1);
   }

   public void glGenVertexArrays(int arg0, int[] arg1, int arg2) {
      System.out.println("glGenVertexArrays(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGenVertexArrays(arg0, arg1, arg2);
   }

   public void glGenVertexArrays(int arg0, IntBuffer arg1) {
      System.out.println("glGenVertexArrays(" + arg0 + "," + arg1 + ")");
      gl.glGenVertexArrays(arg0, arg1);
   }

   public void glGenerateMipmap(int arg0) {
      System.out.println("glGenerateMipmap(" + arg0 + ")");
      gl.glGenerateMipmap(arg0);
   }

   public void glGetActiveAttrib(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, int[] arg5, int arg6,
      int[] arg7, int arg8, byte[] arg9, int arg10) {
      gl.glGetActiveAttrib(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
   }

   public void glGetActiveAttrib(
      int arg0, int arg1, int arg2, IntBuffer arg3, IntBuffer arg4,
      IntBuffer arg5, ByteBuffer arg6) {
      gl.glGetActiveAttrib(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glGetActiveUniform(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, int[] arg5, int arg6,
      int[] arg7, int arg8, byte[] arg9, int arg10) {
      gl.glGetActiveUniform(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
   }

   public void glGetActiveUniform(
      int arg0, int arg1, int arg2, IntBuffer arg3, IntBuffer arg4,
      IntBuffer arg5, ByteBuffer arg6) {
      gl.glGetActiveUniform(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glGetActiveUniformBlockName(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, byte[] arg5,
      int arg6) {
      gl.glGetActiveUniformBlockName(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glGetActiveUniformBlockName(
      int arg0, int arg1, int arg2, IntBuffer arg3, ByteBuffer arg4) {
      System.out.println("glGetActiveUniformBlockName(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetActiveUniformBlockName(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetActiveUniformBlockiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      System.out.println("glGetActiveUniformBlockiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetActiveUniformBlockiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetActiveUniformBlockiv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      System.out.println("glGetActiveUniformBlockiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetActiveUniformBlockiv(arg0, arg1, arg2, arg3);
   }

   public void glGetActiveUniformName(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, byte[] arg5,
      int arg6) {
      gl.glGetActiveUniformName(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glGetActiveUniformName(
      int arg0, int arg1, int arg2, IntBuffer arg3, ByteBuffer arg4) {
      System.out.println("glGetActiveUniformName(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetActiveUniformName(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetActiveUniformsiv(
      int arg0, int arg1, int[] arg2, int arg3, int arg4, int[] arg5,
      int arg6) {
      gl.glGetActiveUniformsiv(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glGetActiveUniformsiv(
      int arg0, int arg1, IntBuffer arg2, int arg3, IntBuffer arg4) {
      System.out.println("glGetActiveUniformsiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetActiveUniformsiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetAttachedShaders(
      int arg0, int arg1, int[] arg2, int arg3, int[] arg4, int arg5) {
      System.out.println("glGetAttachedShaders(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glGetAttachedShaders(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetAttachedShaders(
      int arg0, int arg1, IntBuffer arg2, IntBuffer arg3) {
      System.out.println("glGetAttachedShaders(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetAttachedShaders(arg0, arg1, arg2, arg3);
   }

   public int glGetAttribLocation(int arg0, String arg1) {
      System.out.println("glGetAttribLocation(" + arg0 + "," + arg1 + ")");
      return gl.glGetAttribLocation(arg0, arg1);
   }

   public void glGetBooleani_v(int arg0, int arg1, byte[] arg2, int arg3) {
      System.out.println("glGetBooleani_v(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetBooleani_v(arg0, arg1, arg2, arg3);
   }

   public void glGetBooleani_v(int arg0, int arg1, ByteBuffer arg2) {
      System.out.println("glGetBooleani_v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetBooleani_v(arg0, arg1, arg2);
   }

   public void glGetBooleanv(int arg0, byte[] arg1, int arg2) {
      System.out.println("glGetBooleanv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetBooleanv(arg0, arg1, arg2);
   }

   public void glGetBooleanv(int arg0, ByteBuffer arg1) {
      System.out.println("glGetBooleanv(" + arg0 + "," + arg1 + ")");
      gl.glGetBooleanv(arg0, arg1);
   }

   @Deprecated
   public int glGetBoundBuffer(int target) {
      System.out.println("glGetBoundBuffer(" + target + ")");
      return gl.glGetBoundBuffer(target);
   }

   public void glGetBufferParameteri64v(
      int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println("glGetBufferParameteri64v(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetBufferParameteri64v(arg0, arg1, arg2, arg3);
   }

   public void glGetBufferParameteri64v(int arg0, int arg1, LongBuffer arg2) {
      System.out.println("glGetBufferParameteri64v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetBufferParameteri64v(arg0, arg1, arg2);
   }

   public void glGetBufferParameteriv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetBufferParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetBufferParameteriv(arg0, arg1, arg2, arg3);
   }

   public void glGetBufferParameteriv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetBufferParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetBufferParameteriv(arg0, arg1, arg2);
   }

   public void glGetBufferParameterui64vNV(
      int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println("glGetBufferParameterui64vNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetBufferParameterui64vNV(arg0, arg1, arg2, arg3);
   }

   public void glGetBufferParameterui64vNV(
      int arg0, int arg1, LongBuffer arg2) {
      System.out.println("glGetBufferParameterui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetBufferParameterui64vNV(arg0, arg1, arg2);
   }

   @Deprecated
   public long glGetBufferSize(int bufferName) {
      System.out.println("glGetBufferSize(" + bufferName + ")");
      return gl.glGetBufferSize(bufferName);
   }

   public void glGetBufferSubData(int arg0, long arg1, long arg2, Buffer arg3) {
      System.out.println("glGetBufferSubData(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetBufferSubData(arg0, arg1, arg2, arg3);
   }

   public void glGetCompressedTexImage(int arg0, int arg1, Buffer arg2) {
      System.out.println("glGetCompressedTexImage(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetCompressedTexImage(arg0, arg1, arg2);
   }

   public void glGetCompressedTexImage(int arg0, int arg1, long arg2) {
      System.out.println("glGetCompressedTexImage(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetCompressedTexImage(arg0, arg1, arg2);
   }

   public int glGetDebugMessageLog(
      int arg0, int arg1, int[] arg2, int arg3, int[] arg4, int arg5,
      int[] arg6, int arg7, int[] arg8, int arg9, int[] arg10, int arg11,
      byte[] arg12, int arg13) {
      return gl.glGetDebugMessageLog(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
         arg11, arg12, arg13);
   }

   public int glGetDebugMessageLog(
      int arg0, int arg1, IntBuffer arg2, IntBuffer arg3, IntBuffer arg4,
      IntBuffer arg5, IntBuffer arg6, ByteBuffer arg7) {
      return gl
         .glGetDebugMessageLog(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public int glGetDebugMessageLogAMD(
      int arg0, int arg1, int[] arg2, int arg3, int[] arg4, int arg5,
      int[] arg6, int arg7, int[] arg8, int arg9, byte[] arg10, int arg11) {
      return gl.glGetDebugMessageLogAMD(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
         arg11);
   }

   public int glGetDebugMessageLogAMD(
      int arg0, int arg1, IntBuffer arg2, IntBuffer arg3, IntBuffer arg4,
      IntBuffer arg5, ByteBuffer arg6) {
      return gl
         .glGetDebugMessageLogAMD(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glGetDoublev(int arg0, double[] arg1, int arg2) {
      System.out.println("glGetDoublev(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetDoublev(arg0, arg1, arg2);
   }

   public void glGetDoublev(int arg0, DoubleBuffer arg1) {
      System.out.println("glGetDoublev(" + arg0 + "," + arg1 + ")");
      gl.glGetDoublev(arg0, arg1);
   }

   public int glGetError() {
      System.out.println("glGetError()");
      return gl.glGetError();
   }

   public void glGetFloatv(int arg0, float[] arg1, int arg2) {
      System.out.println("glGetFloatv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetFloatv(arg0, arg1, arg2);
   }

   public void glGetFloatv(int arg0, FloatBuffer arg1) {
      System.out.println("glGetFloatv(" + arg0 + "," + arg1 + ")");
      gl.glGetFloatv(arg0, arg1);
   }

   public int glGetFragDataIndex(int arg0, String arg1) {
      System.out.println("glGetFragDataIndex(" + arg0 + "," + arg1 + ")");
      return gl.glGetFragDataIndex(arg0, arg1);
   }

   public int glGetFragDataLocation(int arg0, String arg1) {
      System.out.println("glGetFragDataLocation(" + arg0 + "," + arg1 + ")");
      return gl.glGetFragDataLocation(arg0, arg1);
   }

   public void glGetFramebufferAttachmentParameteriv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      System.out.println("glGetFramebufferAttachmentParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetFramebufferAttachmentParameteriv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetFramebufferAttachmentParameteriv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      System.out.println("glGetFramebufferAttachmentParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetFramebufferAttachmentParameteriv(arg0, arg1, arg2, arg3);
   }

   public int glGetGraphicsResetStatus() {
      System.out.println("glGetGraphicsResetStatus()");
      return gl.glGetGraphicsResetStatus();
   }

   public void glGetInteger64i_v(int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println("glGetInteger64i_v(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetInteger64i_v(arg0, arg1, arg2, arg3);
   }

   public void glGetInteger64i_v(int arg0, int arg1, LongBuffer arg2) {
      System.out.println("glGetInteger64i_v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetInteger64i_v(arg0, arg1, arg2);
   }

   public void glGetInteger64v(int arg0, long[] arg1, int arg2) {
      System.out.println("glGetInteger64v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetInteger64v(arg0, arg1, arg2);
   }

   public void glGetInteger64v(int arg0, LongBuffer arg1) {
      System.out.println("glGetInteger64v(" + arg0 + "," + arg1 + ")");
      gl.glGetInteger64v(arg0, arg1);
   }

   public void glGetIntegeri_v(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetIntegeri_v(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetIntegeri_v(arg0, arg1, arg2, arg3);
   }

   public void glGetIntegeri_v(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetIntegeri_v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetIntegeri_v(arg0, arg1, arg2);
   }

   public void glGetIntegerui64i_vNV(
      int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println("glGetIntegerui64i_vNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetIntegerui64i_vNV(arg0, arg1, arg2, arg3);
   }

   public void glGetIntegerui64i_vNV(int arg0, int arg1, LongBuffer arg2) {
      System.out.println("glGetIntegerui64i_vNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetIntegerui64i_vNV(arg0, arg1, arg2);
   }

   public void glGetIntegerui64vNV(int arg0, long[] arg1, int arg2) {
      System.out.println("glGetIntegerui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetIntegerui64vNV(arg0, arg1, arg2);
   }

   public void glGetIntegerui64vNV(int arg0, LongBuffer arg1) {
      System.out.println("glGetIntegerui64vNV(" + arg0 + "," + arg1 + ")");
      gl.glGetIntegerui64vNV(arg0, arg1);
   }

   public void glGetIntegerv(int arg0, int[] arg1, int arg2) {
      System.out.println("glGetIntegerv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetIntegerv(arg0, arg1, arg2);
   }

   public void glGetIntegerv(int arg0, IntBuffer arg1) {
      System.out.println("glGetIntegerv(" + arg0 + "," + arg1 + ")");
      gl.glGetIntegerv(arg0, arg1);
   }

   public void glGetInternalformativ(
      int arg0, int arg1, int arg2, int arg3, int[] arg4, int arg5) {
      System.out.println("glGetInternalformativ(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glGetInternalformativ(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetInternalformativ(
      int arg0, int arg1, int arg2, int arg3, IntBuffer arg4) {
      System.out.println("glGetInternalformativ(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetInternalformativ(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetMultisamplefv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glGetMultisamplefv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetMultisamplefv(arg0, arg1, arg2, arg3);
   }

   public void glGetMultisamplefv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glGetMultisamplefv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetMultisamplefv(arg0, arg1, arg2);
   }

   public void glGetNamedBufferParameterui64vNV(
      int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println("glGetNamedBufferParameterui64vNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetNamedBufferParameterui64vNV(arg0, arg1, arg2, arg3);
   }

   public void glGetNamedBufferParameterui64vNV(
      int arg0, int arg1, LongBuffer arg2) {
      System.out.println("glGetNamedBufferParameterui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetNamedBufferParameterui64vNV(arg0, arg1, arg2);
   }

   public void glGetNamedFramebufferParameterivEXT(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetNamedFramebufferParameterivEXT(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetNamedFramebufferParameterivEXT(arg0, arg1, arg2, arg3);
   }

   public void glGetNamedFramebufferParameterivEXT(
      int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetNamedFramebufferParameterivEXT(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetNamedFramebufferParameterivEXT(arg0, arg1, arg2);
   }

   public void glGetNamedStringARB(
      int arg0, String arg1, int arg2, int[] arg3, int arg4, byte[] arg5,
      int arg6) {
      gl.glGetNamedStringARB(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glGetNamedStringARB(
      int arg0, String arg1, int arg2, IntBuffer arg3, ByteBuffer arg4) {
      System.out.println("glGetNamedStringARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetNamedStringARB(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetNamedStringivARB(
      int arg0, String arg1, int arg2, int[] arg3, int arg4) {
      System.out.println("glGetNamedStringivARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetNamedStringivARB(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetNamedStringivARB(
      int arg0, String arg1, int arg2, IntBuffer arg3) {
      System.out.println("glGetNamedStringivARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetNamedStringivARB(arg0, arg1, arg2, arg3);
   }

   public void glGetObjectLabel(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, byte[] arg5,
      int arg6) {
      gl.glGetObjectLabel(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glGetObjectLabel(
      int arg0, int arg1, int arg2, IntBuffer arg3, ByteBuffer arg4) {
      System.out.println("glGetObjectLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetObjectLabel(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetObjectPtrLabel(
      Buffer arg0, int arg1, int[] arg2, int arg3, byte[] arg4, int arg5) {
      System.out.println("glGetObjectPtrLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glGetObjectPtrLabel(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetObjectPtrLabel(
      Buffer arg0, int arg1, IntBuffer arg2, ByteBuffer arg3) {
      System.out.println("glGetObjectPtrLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetObjectPtrLabel(arg0, arg1, arg2, arg3);
   }

   public void glGetProgramBinary(
      int arg0, int arg1, int[] arg2, int arg3, int[] arg4, int arg5,
      Buffer arg6) {
      gl.glGetProgramBinary(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glGetProgramBinary(
      int arg0, int arg1, IntBuffer arg2, IntBuffer arg3, Buffer arg4) {
      System.out.println("glGetProgramBinary(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetProgramBinary(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetProgramInfoLog(
      int arg0, int arg1, int[] arg2, int arg3, byte[] arg4, int arg5) {
      System.out.println("glGetProgramInfoLog(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glGetProgramInfoLog(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetProgramInfoLog(
      int arg0, int arg1, IntBuffer arg2, ByteBuffer arg3) {
      System.out.println("glGetProgramInfoLog(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetProgramInfoLog(arg0, arg1, arg2, arg3);
   }

   public void glGetProgramiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetProgramiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetProgramiv(arg0, arg1, arg2, arg3);
   }

   public void glGetProgramiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetProgramiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetProgramiv(arg0, arg1, arg2);
   }

   public void glGetQueryObjecti64v(int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println("glGetQueryObjecti64v(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetQueryObjecti64v(arg0, arg1, arg2, arg3);
   }

   public void glGetQueryObjecti64v(int arg0, int arg1, LongBuffer arg2) {
      System.out.println("glGetQueryObjecti64v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetQueryObjecti64v(arg0, arg1, arg2);
   }

   public void glGetQueryObjectiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetQueryObjectiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetQueryObjectiv(arg0, arg1, arg2, arg3);
   }

   public void glGetQueryObjectiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetQueryObjectiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetQueryObjectiv(arg0, arg1, arg2);
   }

   public void glGetQueryObjectui64v(
      int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println("glGetQueryObjectui64v(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetQueryObjectui64v(arg0, arg1, arg2, arg3);
   }

   public void glGetQueryObjectui64v(int arg0, int arg1, LongBuffer arg2) {
      System.out.println("glGetQueryObjectui64v(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetQueryObjectui64v(arg0, arg1, arg2);
   }

   public void glGetQueryObjectuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetQueryObjectuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetQueryObjectuiv(arg0, arg1, arg2, arg3);
   }

   public void glGetQueryObjectuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetQueryObjectuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetQueryObjectuiv(arg0, arg1, arg2);
   }

   public void glGetQueryiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetQueryiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetQueryiv(arg0, arg1, arg2, arg3);
   }

   public void glGetQueryiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetQueryiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetQueryiv(arg0, arg1, arg2);
   }

   public void glGetRenderbufferParameteriv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetRenderbufferParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetRenderbufferParameteriv(arg0, arg1, arg2, arg3);
   }

   public void glGetRenderbufferParameteriv(
      int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetRenderbufferParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetRenderbufferParameteriv(arg0, arg1, arg2);
   }

   public void glGetSamplerParameterIiv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetSamplerParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetSamplerParameterIiv(arg0, arg1, arg2, arg3);
   }

   public void glGetSamplerParameterIiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetSamplerParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetSamplerParameterIiv(arg0, arg1, arg2);
   }

   public void glGetSamplerParameterIuiv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetSamplerParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetSamplerParameterIuiv(arg0, arg1, arg2, arg3);
   }

   public void glGetSamplerParameterIuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetSamplerParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetSamplerParameterIuiv(arg0, arg1, arg2);
   }

   public void glGetSamplerParameterfv(
      int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glGetSamplerParameterfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetSamplerParameterfv(arg0, arg1, arg2, arg3);
   }

   public void glGetSamplerParameterfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glGetSamplerParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetSamplerParameterfv(arg0, arg1, arg2);
   }

   public void glGetSamplerParameteriv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetSamplerParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetSamplerParameteriv(arg0, arg1, arg2, arg3);
   }

   public void glGetSamplerParameteriv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetSamplerParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetSamplerParameteriv(arg0, arg1, arg2);
   }

   public void glGetShaderInfoLog(
      int arg0, int arg1, int[] arg2, int arg3, byte[] arg4, int arg5) {
      System.out.println("glGetShaderInfoLog(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glGetShaderInfoLog(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetShaderInfoLog(
      int arg0, int arg1, IntBuffer arg2, ByteBuffer arg3) {
      System.out.println("glGetShaderInfoLog(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetShaderInfoLog(arg0, arg1, arg2, arg3);
   }

   public void glGetShaderPrecisionFormat(
      int arg0, int arg1, int[] arg2, int arg3, int[] arg4, int arg5) {
      System.out.println("glGetShaderPrecisionFormat(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glGetShaderPrecisionFormat(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetShaderPrecisionFormat(
      int arg0, int arg1, IntBuffer arg2, IntBuffer arg3) {
      System.out.println("glGetShaderPrecisionFormat(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetShaderPrecisionFormat(arg0, arg1, arg2, arg3);
   }

   public void glGetShaderSource(
      int arg0, int arg1, int[] arg2, int arg3, byte[] arg4, int arg5) {
      System.out.println("glGetShaderSource(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glGetShaderSource(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetShaderSource(
      int arg0, int arg1, IntBuffer arg2, ByteBuffer arg3) {
      System.out.println("glGetShaderSource(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetShaderSource(arg0, arg1, arg2, arg3);
   }

   public void glGetShaderiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetShaderiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetShaderiv(arg0, arg1, arg2, arg3);
   }

   public void glGetShaderiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetShaderiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetShaderiv(arg0, arg1, arg2);
   }

   public String glGetString(int arg0) {
      System.out.println("glGetString(" + arg0 + ")");
      return gl.glGetString(arg0);
   }

   public String glGetStringi(int arg0, int arg1) {
      System.out.println("glGetStringi(" + arg0 + "," + arg1 + ")");
      return gl.glGetStringi(arg0, arg1);
   }

   public void glGetSynciv(
      long arg0, int arg1, int arg2, int[] arg3, int arg4, int[] arg5,
      int arg6) {
      gl.glGetSynciv(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glGetSynciv(
      long arg0, int arg1, int arg2, IntBuffer arg3, IntBuffer arg4) {
      System.out.println("glGetSynciv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetSynciv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetTexImage(
      int arg0, int arg1, int arg2, int arg3, Buffer arg4) {
      System.out.println("glGetTexImage(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetTexImage(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetTexImage(
      int arg0, int arg1, int arg2, int arg3, long arg4) {
      System.out.println("glGetTexImage(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetTexImage(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetTexLevelParameterfv(
      int arg0, int arg1, int arg2, float[] arg3, int arg4) {
      System.out.println("glGetTexLevelParameterfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetTexLevelParameterfv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetTexLevelParameterfv(
      int arg0, int arg1, int arg2, FloatBuffer arg3) {
      System.out.println("glGetTexLevelParameterfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetTexLevelParameterfv(arg0, arg1, arg2, arg3);
   }

   public void glGetTexLevelParameteriv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      System.out.println("glGetTexLevelParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetTexLevelParameteriv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetTexLevelParameteriv(
      int arg0, int arg1, int arg2, IntBuffer arg3) {
      System.out.println("glGetTexLevelParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetTexLevelParameteriv(arg0, arg1, arg2, arg3);
   }

   public void glGetTexParameterIiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetTexParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetTexParameterIiv(arg0, arg1, arg2, arg3);
   }

   public void glGetTexParameterIiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetTexParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetTexParameterIiv(arg0, arg1, arg2);
   }

   public void glGetTexParameterIuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetTexParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetTexParameterIuiv(arg0, arg1, arg2, arg3);
   }

   public void glGetTexParameterIuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetTexParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetTexParameterIuiv(arg0, arg1, arg2);
   }

   public void glGetTexParameterfv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glGetTexParameterfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetTexParameterfv(arg0, arg1, arg2, arg3);
   }

   public void glGetTexParameterfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glGetTexParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetTexParameterfv(arg0, arg1, arg2);
   }

   public void glGetTexParameteriv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetTexParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetTexParameteriv(arg0, arg1, arg2, arg3);
   }

   public void glGetTexParameteriv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetTexParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetTexParameteriv(arg0, arg1, arg2);
   }

   public void glGetTransformFeedbackVarying(
      int arg0, int arg1, int arg2, int[] arg3, int arg4, int[] arg5, int arg6,
      int[] arg7, int arg8, byte[] arg9, int arg10) {
      gl.glGetTransformFeedbackVarying(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
   }

   public void glGetTransformFeedbackVarying(
      int arg0, int arg1, int arg2, IntBuffer arg3, IntBuffer arg4,
      IntBuffer arg5, ByteBuffer arg6) {
      gl.glGetTransformFeedbackVarying(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public int glGetUniformBlockIndex(int arg0, String arg1) {
      System.out.println("glGetUniformBlockIndex(" + arg0 + "," + arg1 + ")");
      return gl.glGetUniformBlockIndex(arg0, arg1);
   }

   public void glGetUniformIndices(
      int arg0, int arg1, String[] arg2, int[] arg3, int arg4) {
      System.out.println("glGetUniformIndices(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetUniformIndices(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetUniformIndices(
      int arg0, int arg1, String[] arg2, IntBuffer arg3) {
      System.out.println("glGetUniformIndices(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetUniformIndices(arg0, arg1, arg2, arg3);
   }

   public int glGetUniformLocation(int arg0, String arg1) {
      System.out.println("glGetUniformLocation(" + arg0 + "," + arg1 + ")");
      return gl.glGetUniformLocation(arg0, arg1);
   }

   public void glGetUniformfv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glGetUniformfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetUniformfv(arg0, arg1, arg2, arg3);
   }

   public void glGetUniformfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glGetUniformfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetUniformfv(arg0, arg1, arg2);
   }

   public void glGetUniformiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetUniformiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetUniformiv(arg0, arg1, arg2, arg3);
   }

   public void glGetUniformiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetUniformiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetUniformiv(arg0, arg1, arg2);
   }

   public void glGetUniformui64vNV(int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println("glGetUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetUniformui64vNV(arg0, arg1, arg2, arg3);
   }

   public void glGetUniformui64vNV(int arg0, int arg1, LongBuffer arg2) {
      System.out.println("glGetUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetUniformui64vNV(arg0, arg1, arg2);
   }

   public void glGetUniformuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetUniformuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetUniformuiv(arg0, arg1, arg2, arg3);
   }

   public void glGetUniformuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetUniformuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetUniformuiv(arg0, arg1, arg2);
   }

   public void glGetVertexAttribIiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetVertexAttribIiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetVertexAttribIiv(arg0, arg1, arg2, arg3);
   }

   public void glGetVertexAttribIiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetVertexAttribIiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetVertexAttribIiv(arg0, arg1, arg2);
   }

   public void glGetVertexAttribIuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetVertexAttribIuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetVertexAttribIuiv(arg0, arg1, arg2, arg3);
   }

   public void glGetVertexAttribIuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetVertexAttribIuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetVertexAttribIuiv(arg0, arg1, arg2);
   }

   public void glGetVertexAttribdv(
      int arg0, int arg1, double[] arg2, int arg3) {
      System.out.println("glGetVertexAttribdv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetVertexAttribdv(arg0, arg1, arg2, arg3);
   }

   public void glGetVertexAttribdv(int arg0, int arg1, DoubleBuffer arg2) {
      System.out.println("glGetVertexAttribdv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetVertexAttribdv(arg0, arg1, arg2);
   }

   public void glGetVertexAttribfv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glGetVertexAttribfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetVertexAttribfv(arg0, arg1, arg2, arg3);
   }

   public void glGetVertexAttribfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glGetVertexAttribfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetVertexAttribfv(arg0, arg1, arg2);
   }

   public void glGetVertexAttribiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glGetVertexAttribiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetVertexAttribiv(arg0, arg1, arg2, arg3);
   }

   public void glGetVertexAttribiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glGetVertexAttribiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glGetVertexAttribiv(arg0, arg1, arg2);
   }

   public void glGetnCompressedTexImage(
      int arg0, int arg1, int arg2, Buffer arg3) {
      System.out.println("glGetnCompressedTexImage(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetnCompressedTexImage(arg0, arg1, arg2, arg3);
   }

   public void glGetnTexImage(
      int arg0, int arg1, int arg2, int arg3, int arg4, Buffer arg5) {
      System.out.println("glGetnTexImage(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glGetnTexImage(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glGetnUniformdv(
      int arg0, int arg1, int arg2, double[] arg3, int arg4) {
      System.out.println("glGetnUniformdv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetnUniformdv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetnUniformdv(
      int arg0, int arg1, int arg2, DoubleBuffer arg3) {
      System.out.println("glGetnUniformdv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetnUniformdv(arg0, arg1, arg2, arg3);
   }

   public void glGetnUniformfv(
      int arg0, int arg1, int arg2, float[] arg3, int arg4) {
      System.out.println("glGetnUniformfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetnUniformfv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetnUniformfv(int arg0, int arg1, int arg2, FloatBuffer arg3) {
      System.out.println("glGetnUniformfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetnUniformfv(arg0, arg1, arg2, arg3);
   }

   public void glGetnUniformiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      System.out.println("glGetnUniformiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetnUniformiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetnUniformiv(int arg0, int arg1, int arg2, IntBuffer arg3) {
      System.out.println("glGetnUniformiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetnUniformiv(arg0, arg1, arg2, arg3);
   }

   public void glGetnUniformuiv(
      int arg0, int arg1, int arg2, int[] arg3, int arg4) {
      System.out.println("glGetnUniformuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glGetnUniformuiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glGetnUniformuiv(int arg0, int arg1, int arg2, IntBuffer arg3) {
      System.out.println("glGetnUniformuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glGetnUniformuiv(arg0, arg1, arg2, arg3);
   }

   public void glHint(int arg0, int arg1) {
      System.out.println("glHint(" + arg0 + "," + arg1 + ")");
      gl.glHint(arg0, arg1);
   }

   public long glImportSyncEXT(int arg0, long arg1, int arg2) {
      System.out.println("glImportSyncEXT(" + arg0 + "," + arg1 + "," + arg2 + ")");
      return gl.glImportSyncEXT(arg0, arg1, arg2);
   }

   public void glIndexFormatNV(int arg0, int arg1) {
      System.out.println("glIndexFormatNV(" + arg0 + "," + arg1 + ")");
      gl.glIndexFormatNV(arg0, arg1);
   }

   public boolean glIsBuffer(int arg0) {
      System.out.println("glIsBuffer(" + arg0 + ")");
      return gl.glIsBuffer(arg0);
   }

   public boolean glIsBufferResidentNV(int arg0) {
      System.out.println("glIsBufferResidentNV(" + arg0 + ")");
      return gl.glIsBufferResidentNV(arg0);
   }

   public boolean glIsEnabled(int arg0) {
      System.out.println("glIsEnabled(" + arg0 + ")");
      return gl.glIsEnabled(arg0);
   }

   public boolean glIsEnabledi(int arg0, int arg1) {
      System.out.println("glIsEnabledi(" + arg0 + "," + arg1 + ")");
      return gl.glIsEnabledi(arg0, arg1);
   }

   public boolean glIsFramebuffer(int arg0) {
      System.out.println("glIsFramebuffer(" + arg0 + ")");
      return gl.glIsFramebuffer(arg0);
   }

   public boolean glIsNamedBufferResidentNV(int arg0) {
      System.out.println("glIsNamedBufferResidentNV(" + arg0 + ")");
      return gl.glIsNamedBufferResidentNV(arg0);
   }

   public boolean glIsNamedStringARB(int arg0, String arg1) {
      System.out.println("glIsNamedStringARB(" + arg0 + "," + arg1 + ")");
      return gl.glIsNamedStringARB(arg0, arg1);
   }

   @Deprecated
   public boolean glIsPBOPackBound() {
      System.out.println("glIsPBOPackBound()");
      return gl.glIsPBOPackBound();
   }

   @Deprecated
   public boolean glIsPBOUnpackBound() {
      System.out.println("glIsPBOUnpackBound()");
      return gl.glIsPBOUnpackBound();
   }

   public boolean glIsProgram(int arg0) {
      System.out.println("glIsProgram(" + arg0 + ")");
      return gl.glIsProgram(arg0);
   }

   public boolean glIsQuery(int arg0) {
      System.out.println("glIsQuery(" + arg0 + ")");
      return gl.glIsQuery(arg0);
   }

   public boolean glIsRenderbuffer(int arg0) {
      System.out.println("glIsRenderbuffer(" + arg0 + ")");
      return gl.glIsRenderbuffer(arg0);
   }

   public boolean glIsSampler(int arg0) {
      System.out.println("glIsSampler(" + arg0 + ")");
      return gl.glIsSampler(arg0);
   }

   public boolean glIsShader(int arg0) {
      System.out.println("glIsShader(" + arg0 + ")");
      return gl.glIsShader(arg0);
   }

   public boolean glIsSync(long arg0) {
      System.out.println("glIsSync(" + arg0 + ")");
      return gl.glIsSync(arg0);
   }

   public boolean glIsTexture(int arg0) {
      System.out.println("glIsTexture(" + arg0 + ")");
      return gl.glIsTexture(arg0);
   }

   @Deprecated
   public boolean glIsVBOArrayBound() {
      System.out.println("glIsVBOArrayBound()");
      return gl.glIsVBOArrayBound();
   }

   @Deprecated
   public boolean glIsVBOElementArrayBound() {
      System.out.println("glIsVBOElementArrayBound()");
      return gl.glIsVBOElementArrayBound();
   }

   public boolean glIsVertexArray(int arg0) {
      System.out.println("glIsVertexArray(" + arg0 + ")");
      return gl.glIsVertexArray(arg0);
   }

   public void glLineWidth(float arg0) {
      System.out.println("glLineWidth(" + arg0 + ")");
      gl.glLineWidth(arg0);
   }

   public void glLinkProgram(int arg0) {
      System.out.println("glLinkProgram(" + arg0 + ")");
      gl.glLinkProgram(arg0);
   }

   public void glLogicOp(int arg0) {
      System.out.println("glLogicOp(" + arg0 + ")");
      gl.glLogicOp(arg0);
   }

   public void glMakeBufferNonResidentNV(int arg0) {
      System.out.println("glMakeBufferNonResidentNV(" + arg0 + ")");
      gl.glMakeBufferNonResidentNV(arg0);
   }

   public void glMakeBufferResidentNV(int arg0, int arg1) {
      System.out.println("glMakeBufferResidentNV(" + arg0 + "," + arg1 + ")");
      gl.glMakeBufferResidentNV(arg0, arg1);
   }

   public void glMakeNamedBufferNonResidentNV(int arg0) {
      System.out.println("glMakeNamedBufferNonResidentNV(" + arg0 + ")");
      gl.glMakeNamedBufferNonResidentNV(arg0);
   }

   public void glMakeNamedBufferResidentNV(int arg0, int arg1) {
      System.out.println("glMakeNamedBufferResidentNV(" + arg0 + "," + arg1 + ")");
      gl.glMakeNamedBufferResidentNV(arg0, arg1);
   }

   public ByteBuffer glMapBuffer(int arg0, int arg1) {
      System.out.println("glMapBuffer(" + arg0 + "," + arg1 + ")");
      return gl.glMapBuffer(arg0, arg1);
   }

   public ByteBuffer glMapBufferRange(
      int arg0, long arg1, long arg2, int arg3) {
      System.out.println("glMapBufferRange(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      return gl.glMapBufferRange(arg0, arg1, arg2, arg3);
   }

   public void glMultiDrawArrays(
      int arg0, int[] arg1, int arg2, int[] arg3, int arg4, int arg5) {
      System.out.println("glMultiDrawArrays(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glMultiDrawArrays(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glMultiDrawArrays(
      int arg0, IntBuffer arg1, IntBuffer arg2, int arg3) {
      System.out.println("glMultiDrawArrays(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glMultiDrawArrays(arg0, arg1, arg2, arg3);
   }

   public void glMultiDrawArraysIndirectAMD(
      int arg0, Buffer arg1, int arg2, int arg3) {
      System.out.println("glMultiDrawArraysIndirectAMD(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glMultiDrawArraysIndirectAMD(arg0, arg1, arg2, arg3);
   }

   public void glMultiDrawElements(
      int arg0, IntBuffer arg1, int arg2, PointerBuffer arg3, int arg4) {
      System.out.println("glMultiDrawElements(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glMultiDrawElements(arg0, arg1, arg2, arg3, arg4);
   }

   public void glMultiDrawElementsBaseVertex(
      int arg0, IntBuffer arg1, int arg2, PointerBuffer arg3, int arg4,
      IntBuffer arg5) {
      gl.glMultiDrawElementsBaseVertex(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glMultiDrawElementsIndirectAMD(
      int arg0, int arg1, Buffer arg2, int arg3, int arg4) {
      System.out.println("glMultiDrawElementsIndirectAMD(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glMultiDrawElementsIndirectAMD(arg0, arg1, arg2, arg3, arg4);
   }

   public void glMultiTexCoordP1ui(int arg0, int arg1, int arg2) {
      System.out.println("glMultiTexCoordP1ui(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glMultiTexCoordP1ui(arg0, arg1, arg2);
   }

   public void glMultiTexCoordP1uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glMultiTexCoordP1uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glMultiTexCoordP1uiv(arg0, arg1, arg2, arg3);
   }

   public void glMultiTexCoordP1uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glMultiTexCoordP1uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glMultiTexCoordP1uiv(arg0, arg1, arg2);
   }

   public void glMultiTexCoordP2ui(int arg0, int arg1, int arg2) {
      System.out.println("glMultiTexCoordP2ui(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glMultiTexCoordP2ui(arg0, arg1, arg2);
   }

   public void glMultiTexCoordP2uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glMultiTexCoordP2uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glMultiTexCoordP2uiv(arg0, arg1, arg2, arg3);
   }

   public void glMultiTexCoordP2uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glMultiTexCoordP2uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glMultiTexCoordP2uiv(arg0, arg1, arg2);
   }

   public void glMultiTexCoordP3ui(int arg0, int arg1, int arg2) {
      System.out.println("glMultiTexCoordP3ui(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glMultiTexCoordP3ui(arg0, arg1, arg2);
   }

   public void glMultiTexCoordP3uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glMultiTexCoordP3uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glMultiTexCoordP3uiv(arg0, arg1, arg2, arg3);
   }

   public void glMultiTexCoordP3uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glMultiTexCoordP3uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glMultiTexCoordP3uiv(arg0, arg1, arg2);
   }

   public void glMultiTexCoordP4ui(int arg0, int arg1, int arg2) {
      System.out.println("glMultiTexCoordP4ui(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glMultiTexCoordP4ui(arg0, arg1, arg2);
   }

   public void glMultiTexCoordP4uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glMultiTexCoordP4uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glMultiTexCoordP4uiv(arg0, arg1, arg2, arg3);
   }

   public void glMultiTexCoordP4uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glMultiTexCoordP4uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glMultiTexCoordP4uiv(arg0, arg1, arg2);
   }

   public void glNamedFramebufferParameteriEXT(int arg0, int arg1, int arg2) {
      System.out.println("glNamedFramebufferParameteriEXT(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glNamedFramebufferParameteriEXT(arg0, arg1, arg2);
   }

   public void glNamedStringARB(
      int arg0, int arg1, String arg2, int arg3, String arg4) {
      System.out.println("glNamedStringARB(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glNamedStringARB(arg0, arg1, arg2, arg3, arg4);
   }

   public void glNormalFormatNV(int arg0, int arg1) {
      System.out.println("glNormalFormatNV(" + arg0 + "," + arg1 + ")");
      gl.glNormalFormatNV(arg0, arg1);
   }

   public void glNormalP3ui(int arg0, int arg1) {
      System.out.println("glNormalP3ui(" + arg0 + "," + arg1 + ")");
      gl.glNormalP3ui(arg0, arg1);
   }

   public void glNormalP3uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glNormalP3uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glNormalP3uiv(arg0, arg1, arg2);
   }

   public void glNormalP3uiv(int arg0, IntBuffer arg1) {
      System.out.println("glNormalP3uiv(" + arg0 + "," + arg1 + ")");
      gl.glNormalP3uiv(arg0, arg1);
   }

   public void glObjectLabel(
      int arg0, int arg1, int arg2, byte[] arg3, int arg4) {
      System.out.println("glObjectLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glObjectLabel(arg0, arg1, arg2, arg3, arg4);
   }

   public void glObjectLabel(int arg0, int arg1, int arg2, ByteBuffer arg3) {
      System.out.println("glObjectLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glObjectLabel(arg0, arg1, arg2, arg3);
   }

   public void glObjectPtrLabel(Buffer arg0, int arg1, byte[] arg2, int arg3) {
      System.out.println("glObjectPtrLabel(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glObjectPtrLabel(arg0, arg1, arg2, arg3);
   }

   public void glObjectPtrLabel(Buffer arg0, int arg1, ByteBuffer arg2) {
      System.out.println("glObjectPtrLabel(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glObjectPtrLabel(arg0, arg1, arg2);
   }

   public void glPixelStoref(int arg0, float arg1) {
      System.out.println("glPixelStoref(" + arg0 + "," + arg1 + ")");
      gl.glPixelStoref(arg0, arg1);
   }

   public void glPixelStorei(int arg0, int arg1) {
      System.out.println("glPixelStorei(" + arg0 + "," + arg1 + ")");
      gl.glPixelStorei(arg0, arg1);
   }

   public void glPointParameterf(int arg0, float arg1) {
      System.out.println("glPointParameterf(" + arg0 + "," + arg1 + ")");
      gl.glPointParameterf(arg0, arg1);
   }

   public void glPointParameterfv(int arg0, float[] arg1, int arg2) {
      System.out.println("glPointParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glPointParameterfv(arg0, arg1, arg2);
   }

   public void glPointParameterfv(int arg0, FloatBuffer arg1) {
      System.out.println("glPointParameterfv(" + arg0 + "," + arg1 + ")");
      gl.glPointParameterfv(arg0, arg1);
   }

   public void glPointParameteri(int arg0, int arg1) {
      System.out.println("glPointParameteri(" + arg0 + "," + arg1 + ")");
      gl.glPointParameteri(arg0, arg1);
   }

   public void glPointParameteriv(int arg0, int[] arg1, int arg2) {
      System.out.println("glPointParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glPointParameteriv(arg0, arg1, arg2);
   }

   public void glPointParameteriv(int arg0, IntBuffer arg1) {
      System.out.println("glPointParameteriv(" + arg0 + "," + arg1 + ")");
      gl.glPointParameteriv(arg0, arg1);
   }

   public void glPointSize(float arg0) {
      System.out.println("glPointSize(" + arg0 + ")");
      gl.glPointSize(arg0);
   }

   public void glPolygonMode(int arg0, int arg1) {
      System.out.println("glPolygonMode(" + arg0 + "," + arg1 + ")");
      gl.glPolygonMode(arg0, arg1);
   }

   public void glPolygonOffset(float arg0, float arg1) {
      System.out.println("glPolygonOffset(" + arg0 + "," + arg1 + ")");
      gl.glPolygonOffset(arg0, arg1);
   }

   public void glPopDebugGroup() {
      System.out.println("glPopDebugGroup()");
      gl.glPopDebugGroup();
   }

   public void glPrimitiveRestartIndex(int arg0) {
      System.out.println("glPrimitiveRestartIndex(" + arg0 + ")");
      gl.glPrimitiveRestartIndex(arg0);
   }

   public void glProgramBinary(int arg0, int arg1, Buffer arg2, int arg3) {
      System.out.println("glProgramBinary(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glProgramBinary(arg0, arg1, arg2, arg3);
   }

   public void glProgramParameteriARB(int arg0, int arg1, int arg2) {
      System.out.println("glProgramParameteriARB(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glProgramParameteriARB(arg0, arg1, arg2);
   }

   public void glProgramUniformui64NV(int arg0, int arg1, long arg2) {
      System.out.println("glProgramUniformui64NV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glProgramUniformui64NV(arg0, arg1, arg2);
   }

   public void glProgramUniformui64vNV(
      int arg0, int arg1, int arg2, long[] arg3, int arg4) {
      System.out.println("glProgramUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glProgramUniformui64vNV(arg0, arg1, arg2, arg3, arg4);
   }

   public void glProgramUniformui64vNV(
      int arg0, int arg1, int arg2, LongBuffer arg3) {
      System.out.println("glProgramUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glProgramUniformui64vNV(arg0, arg1, arg2, arg3);
   }

   public void glProvokingVertex(int arg0) {
      System.out.println("glProvokingVertex(" + arg0 + ")");
      gl.glProvokingVertex(arg0);
   }

   public void glPushDebugGroup(
      int arg0, int arg1, int arg2, byte[] arg3, int arg4) {
      System.out.println("glPushDebugGroup(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glPushDebugGroup(arg0, arg1, arg2, arg3, arg4);
   }

   public void glPushDebugGroup(int arg0, int arg1, int arg2, ByteBuffer arg3) {
      System.out.println("glPushDebugGroup(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glPushDebugGroup(arg0, arg1, arg2, arg3);
   }

   public void glQueryCounter(int arg0, int arg1) {
      System.out.println("glQueryCounter(" + arg0 + "," + arg1 + ")");
      gl.glQueryCounter(arg0, arg1);
   }

   public void glReadBuffer(int arg0) {
      System.out.println("glReadBuffer(" + arg0 + ")");
      gl.glReadBuffer(arg0);
   }

   public void glReadPixels(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, Buffer arg6) {
      System.out.println("glReadPixels(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glReadPixels(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glReadPixels(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, long arg6) {
      System.out.println("glReadPixels(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glReadPixels(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glReadnPixels(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      Buffer arg7) {
      gl.glReadnPixels(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glReleaseShaderCompiler() {
      System.out.println("glReleaseShaderCompiler()");
      gl.glReleaseShaderCompiler();
   }

   public void glRenderbufferStorage(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glRenderbufferStorage(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glRenderbufferStorage(arg0, arg1, arg2, arg3);
   }

   public void glRenderbufferStorageMultisample(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glRenderbufferStorageMultisample(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glRenderbufferStorageMultisample(arg0, arg1, arg2, arg3, arg4);
   }

   public void glSampleCoverage(float arg0, boolean arg1) {
      System.out.println("glSampleCoverage(" + arg0 + "," + arg1 + ")");
      gl.glSampleCoverage(arg0, arg1);
   }

   public void glSampleMaski(int arg0, int arg1) {
      System.out.println("glSampleMaski(" + arg0 + "," + arg1 + ")");
      gl.glSampleMaski(arg0, arg1);
   }

   public void glSamplerParameterIiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glSamplerParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glSamplerParameterIiv(arg0, arg1, arg2, arg3);
   }

   public void glSamplerParameterIiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glSamplerParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameterIiv(arg0, arg1, arg2);
   }

   public void glSamplerParameterIuiv(
      int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glSamplerParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glSamplerParameterIuiv(arg0, arg1, arg2, arg3);
   }

   public void glSamplerParameterIuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glSamplerParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameterIuiv(arg0, arg1, arg2);
   }

   public void glSamplerParameterf(int arg0, int arg1, float arg2) {
      System.out.println("glSamplerParameterf(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameterf(arg0, arg1, arg2);
   }

   public void glSamplerParameterfv(
      int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glSamplerParameterfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glSamplerParameterfv(arg0, arg1, arg2, arg3);
   }

   public void glSamplerParameterfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glSamplerParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameterfv(arg0, arg1, arg2);
   }

   public void glSamplerParameteri(int arg0, int arg1, int arg2) {
      System.out.println("glSamplerParameteri(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameteri(arg0, arg1, arg2);
   }

   public void glSamplerParameteriv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glSamplerParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glSamplerParameteriv(arg0, arg1, arg2, arg3);
   }

   public void glSamplerParameteriv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glSamplerParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSamplerParameteriv(arg0, arg1, arg2);
   }

   public void glScissor(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glScissor(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glScissor(arg0, arg1, arg2, arg3);
   }

   public void glSecondaryColorFormatNV(int arg0, int arg1, int arg2) {
      System.out.println("glSecondaryColorFormatNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSecondaryColorFormatNV(arg0, arg1, arg2);
   }

   public void glSecondaryColorP3ui(int arg0, int arg1) {
      System.out.println("glSecondaryColorP3ui(" + arg0 + "," + arg1 + ")");
      gl.glSecondaryColorP3ui(arg0, arg1);
   }

   public void glSecondaryColorP3uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glSecondaryColorP3uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSecondaryColorP3uiv(arg0, arg1, arg2);
   }

   public void glSecondaryColorP3uiv(int arg0, IntBuffer arg1) {
      System.out.println("glSecondaryColorP3uiv(" + arg0 + "," + arg1 + ")");
      gl.glSecondaryColorP3uiv(arg0, arg1);
   }

   public void glSetMultisamplefvAMD(
      int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glSetMultisamplefvAMD(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glSetMultisamplefvAMD(arg0, arg1, arg2, arg3);
   }

   public void glSetMultisamplefvAMD(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glSetMultisamplefvAMD(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glSetMultisamplefvAMD(arg0, arg1, arg2);
   }

   public void glShaderBinary(
      int arg0, int[] arg1, int arg2, int arg3, Buffer arg4, int arg5) {
      System.out.println("glShaderBinary(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glShaderBinary(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glShaderBinary(
      int arg0, IntBuffer arg1, int arg2, Buffer arg3, int arg4) {
      System.out.println("glShaderBinary(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glShaderBinary(arg0, arg1, arg2, arg3, arg4);
   }

   public void glShaderSource(
      int arg0, int arg1, String[] arg2, int[] arg3, int arg4) {
      System.out.println("glShaderSource(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glShaderSource(arg0, arg1, arg2, arg3, arg4);
   }

   public void glShaderSource(
      int arg0, int arg1, String[] arg2, IntBuffer arg3) {
      System.out.println("glShaderSource(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glShaderSource(arg0, arg1, arg2, arg3);
   }

   public void glStencilFunc(int arg0, int arg1, int arg2) {
      System.out.println("glStencilFunc(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glStencilFunc(arg0, arg1, arg2);
   }

   public void glStencilFuncSeparate(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glStencilFuncSeparate(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glStencilFuncSeparate(arg0, arg1, arg2, arg3);
   }

   public void glStencilMask(int arg0) {
      System.out.println("glStencilMask(" + arg0 + ")");
      gl.glStencilMask(arg0);
   }

   public void glStencilMaskSeparate(int arg0, int arg1) {
      System.out.println("glStencilMaskSeparate(" + arg0 + "," + arg1 + ")");
      gl.glStencilMaskSeparate(arg0, arg1);
   }

   public void glStencilOp(int arg0, int arg1, int arg2) {
      System.out.println("glStencilOp(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glStencilOp(arg0, arg1, arg2);
   }

   public void glStencilOpSeparate(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glStencilOpSeparate(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glStencilOpSeparate(arg0, arg1, arg2, arg3);
   }

   public void glStencilOpValueAMD(int arg0, int arg1) {
      System.out.println("glStencilOpValueAMD(" + arg0 + "," + arg1 + ")");
      gl.glStencilOpValueAMD(arg0, arg1);
   }

   public void glTessellationFactorAMD(float arg0) {
      System.out.println("glTessellationFactorAMD(" + arg0 + ")");
      gl.glTessellationFactorAMD(arg0);
   }

   public void glTessellationModeAMD(int arg0) {
      System.out.println("glTessellationModeAMD(" + arg0 + ")");
      gl.glTessellationModeAMD(arg0);
   }

   public void glTexBuffer(int arg0, int arg1, int arg2) {
      System.out.println("glTexBuffer(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexBuffer(arg0, arg1, arg2);
   }

   public void glTexCoordFormatNV(int arg0, int arg1, int arg2) {
      System.out.println("glTexCoordFormatNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexCoordFormatNV(arg0, arg1, arg2);
   }

   public void glTexCoordP1ui(int arg0, int arg1) {
      System.out.println("glTexCoordP1ui(" + arg0 + "," + arg1 + ")");
      gl.glTexCoordP1ui(arg0, arg1);
   }

   public void glTexCoordP1uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glTexCoordP1uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexCoordP1uiv(arg0, arg1, arg2);
   }

   public void glTexCoordP1uiv(int arg0, IntBuffer arg1) {
      System.out.println("glTexCoordP1uiv(" + arg0 + "," + arg1 + ")");
      gl.glTexCoordP1uiv(arg0, arg1);
   }

   public void glTexCoordP2ui(int arg0, int arg1) {
      System.out.println("glTexCoordP2ui(" + arg0 + "," + arg1 + ")");
      gl.glTexCoordP2ui(arg0, arg1);
   }

   public void glTexCoordP2uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glTexCoordP2uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexCoordP2uiv(arg0, arg1, arg2);
   }

   public void glTexCoordP2uiv(int arg0, IntBuffer arg1) {
      System.out.println("glTexCoordP2uiv(" + arg0 + "," + arg1 + ")");
      gl.glTexCoordP2uiv(arg0, arg1);
   }

   public void glTexCoordP3ui(int arg0, int arg1) {
      System.out.println("glTexCoordP3ui(" + arg0 + "," + arg1 + ")");
      gl.glTexCoordP3ui(arg0, arg1);
   }

   public void glTexCoordP3uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glTexCoordP3uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexCoordP3uiv(arg0, arg1, arg2);
   }

   public void glTexCoordP3uiv(int arg0, IntBuffer arg1) {
      System.out.println("glTexCoordP3uiv(" + arg0 + "," + arg1 + ")");
      gl.glTexCoordP3uiv(arg0, arg1);
   }

   public void glTexCoordP4ui(int arg0, int arg1) {
      System.out.println("glTexCoordP4ui(" + arg0 + "," + arg1 + ")");
      gl.glTexCoordP4ui(arg0, arg1);
   }

   public void glTexCoordP4uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glTexCoordP4uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexCoordP4uiv(arg0, arg1, arg2);
   }

   public void glTexCoordP4uiv(int arg0, IntBuffer arg1) {
      System.out.println("glTexCoordP4uiv(" + arg0 + "," + arg1 + ")");
      gl.glTexCoordP4uiv(arg0, arg1);
   }

   public void glTexImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      Buffer arg7) {
      gl.glTexImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glTexImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      long arg7) {
      gl.glTexImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glTexImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, Buffer arg8) {
      gl.glTexImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
   }

   public void glTexImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, long arg8) {
      gl.glTexImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
   }

   public void glTexImage2DMultisample(
      int arg0, int arg1, int arg2, int arg3, int arg4, boolean arg5) {
      System.out.println("glTexImage2DMultisample(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glTexImage2DMultisample(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glTexImage2DMultisampleCoverageNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5,
      boolean arg6) {
      gl.glTexImage2DMultisampleCoverageNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glTexImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, Buffer arg9) {
      gl.glTexImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
   }

   public void glTexImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, long arg9) {
      gl.glTexImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
   }

   public void glTexImage3DMultisample(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5,
      boolean arg6) {
      gl.glTexImage3DMultisample(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glTexImage3DMultisampleCoverageNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      boolean arg7) {
      gl.glTexImage3DMultisampleCoverageNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glTexParameterIiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glTexParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glTexParameterIiv(arg0, arg1, arg2, arg3);
   }

   public void glTexParameterIiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glTexParameterIiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameterIiv(arg0, arg1, arg2);
   }

   public void glTexParameterIuiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glTexParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glTexParameterIuiv(arg0, arg1, arg2, arg3);
   }

   public void glTexParameterIuiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glTexParameterIuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameterIuiv(arg0, arg1, arg2);
   }

   public void glTexParameterf(int arg0, int arg1, float arg2) {
      System.out.println("glTexParameterf(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameterf(arg0, arg1, arg2);
   }

   public void glTexParameterfv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glTexParameterfv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glTexParameterfv(arg0, arg1, arg2, arg3);
   }

   public void glTexParameterfv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glTexParameterfv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameterfv(arg0, arg1, arg2);
   }

   public void glTexParameteri(int arg0, int arg1, int arg2) {
      System.out.println("glTexParameteri(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameteri(arg0, arg1, arg2);
   }

   public void glTexParameteriv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glTexParameteriv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glTexParameteriv(arg0, arg1, arg2, arg3);
   }

   public void glTexParameteriv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glTexParameteriv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glTexParameteriv(arg0, arg1, arg2);
   }

   public void glTexStorage1D(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glTexStorage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glTexStorage1D(arg0, arg1, arg2, arg3);
   }

   public void glTexStorage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glTexStorage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glTexStorage2D(arg0, arg1, arg2, arg3, arg4);
   }

   public void glTexStorage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
      System.out.println("glTexStorage3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glTexStorage3D(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glTexSubImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, Buffer arg6) {
      System.out.println("glTexSubImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glTexSubImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glTexSubImage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, long arg6) {
      System.out.println("glTexSubImage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glTexSubImage1D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glTexSubImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, Buffer arg8) {
      gl.glTexSubImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
   }

   public void glTexSubImage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, long arg8) {
      gl.glTexSubImage2D(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
   }

   public void glTexSubImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, int arg9, Buffer arg10) {
      gl.glTexSubImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
   }

   public void glTexSubImage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, int arg8, int arg9, long arg10) {
      gl.glTexSubImage3D(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
   }

   public void glTextureBufferRangeEXT(
      int arg0, int arg1, int arg2, int arg3, long arg4, long arg5) {
      System.out.println("glTextureBufferRangeEXT(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glTextureBufferRangeEXT(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glTextureImage2DMultisampleCoverageNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      boolean arg7) {
      gl.glTextureImage2DMultisampleCoverageNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glTextureImage2DMultisampleNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5,
      boolean arg6) {
      gl.glTextureImage2DMultisampleNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glTextureImage3DMultisampleCoverageNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      int arg7, boolean arg8) {
      gl.glTextureImage3DMultisampleCoverageNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
   }

   public void glTextureImage3DMultisampleNV(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6,
      boolean arg7) {
      gl.glTextureImage3DMultisampleNV(
         arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
   }

   public void glTextureStorage1D(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glTextureStorage1D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glTextureStorage1D(arg0, arg1, arg2, arg3, arg4);
   }

   public void glTextureStorage2D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
      System.out.println("glTextureStorage2D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glTextureStorage2D(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glTextureStorage3D(
      int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
      System.out.println("glTextureStorage3D(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + ")");
      gl.glTextureStorage3D(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
   }

   public void glTransformFeedbackVaryings(
      int arg0, int arg1, String[] arg2, int arg3) {
      System.out.println("glTransformFeedbackVaryings(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glTransformFeedbackVaryings(arg0, arg1, arg2, arg3);
   }

   public void glUniform(GLUniformData arg0) {
      System.out.println("glUniform(" + arg0 + ")");
      gl.glUniform(arg0);
   }

   public void glUniform1f(int arg0, float arg1) {
      System.out.println("glUniform1f(" + arg0 + "," + arg1 + ")");
      gl.glUniform1f(arg0, arg1);
   }

   public void glUniform1fv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glUniform1fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform1fv(arg0, arg1, arg2, arg3);
   }

   public void glUniform1fv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glUniform1fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform1fv(arg0, arg1, arg2);
   }

   public void glUniform1i(int arg0, int arg1) {
      System.out.println("glUniform1i(" + arg0 + "," + arg1 + ")");
      gl.glUniform1i(arg0, arg1);
   }

   public void glUniform1iv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glUniform1iv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform1iv(arg0, arg1, arg2, arg3);
   }

   public void glUniform1iv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glUniform1iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform1iv(arg0, arg1, arg2);
   }

   public void glUniform1ui(int arg0, int arg1) {
      System.out.println("glUniform1ui(" + arg0 + "," + arg1 + ")");
      gl.glUniform1ui(arg0, arg1);
   }

   public void glUniform1uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glUniform1uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform1uiv(arg0, arg1, arg2, arg3);
   }

   public void glUniform1uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glUniform1uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform1uiv(arg0, arg1, arg2);
   }

   public void glUniform2f(int arg0, float arg1, float arg2) {
      System.out.println("glUniform2f(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2f(arg0, arg1, arg2);
   }

   public void glUniform2fv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glUniform2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform2fv(arg0, arg1, arg2, arg3);
   }

   public void glUniform2fv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glUniform2fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2fv(arg0, arg1, arg2);
   }

   public void glUniform2i(int arg0, int arg1, int arg2) {
      System.out.println("glUniform2i(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2i(arg0, arg1, arg2);
   }

   public void glUniform2iv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glUniform2iv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform2iv(arg0, arg1, arg2, arg3);
   }

   public void glUniform2iv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glUniform2iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2iv(arg0, arg1, arg2);
   }

   public void glUniform2ui(int arg0, int arg1, int arg2) {
      System.out.println("glUniform2ui(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2ui(arg0, arg1, arg2);
   }

   public void glUniform2uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glUniform2uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform2uiv(arg0, arg1, arg2, arg3);
   }

   public void glUniform2uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glUniform2uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform2uiv(arg0, arg1, arg2);
   }

   public void glUniform3f(int arg0, float arg1, float arg2, float arg3) {
      System.out.println("glUniform3f(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3f(arg0, arg1, arg2, arg3);
   }

   public void glUniform3fv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glUniform3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3fv(arg0, arg1, arg2, arg3);
   }

   public void glUniform3fv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glUniform3fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform3fv(arg0, arg1, arg2);
   }

   public void glUniform3i(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glUniform3i(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3i(arg0, arg1, arg2, arg3);
   }

   public void glUniform3iv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glUniform3iv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3iv(arg0, arg1, arg2, arg3);
   }

   public void glUniform3iv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glUniform3iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform3iv(arg0, arg1, arg2);
   }

   public void glUniform3ui(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glUniform3ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3ui(arg0, arg1, arg2, arg3);
   }

   public void glUniform3uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glUniform3uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform3uiv(arg0, arg1, arg2, arg3);
   }

   public void glUniform3uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glUniform3uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform3uiv(arg0, arg1, arg2);
   }

   public void glUniform4f(
      int arg0, float arg1, float arg2, float arg3, float arg4) {
      System.out.println("glUniform4f(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniform4f(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniform4fv(int arg0, int arg1, float[] arg2, int arg3) {
      System.out.println("glUniform4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform4fv(arg0, arg1, arg2, arg3);
   }

   public void glUniform4fv(int arg0, int arg1, FloatBuffer arg2) {
      System.out.println("glUniform4fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform4fv(arg0, arg1, arg2);
   }

   public void glUniform4i(int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glUniform4i(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniform4i(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniform4iv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glUniform4iv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform4iv(arg0, arg1, arg2, arg3);
   }

   public void glUniform4iv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glUniform4iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform4iv(arg0, arg1, arg2);
   }

   public void glUniform4ui(int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glUniform4ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniform4ui(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniform4uiv(int arg0, int arg1, int[] arg2, int arg3) {
      System.out.println("glUniform4uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniform4uiv(arg0, arg1, arg2, arg3);
   }

   public void glUniform4uiv(int arg0, int arg1, IntBuffer arg2) {
      System.out.println("glUniform4uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniform4uiv(arg0, arg1, arg2);
   }

   public void glUniformBlockBinding(int arg0, int arg1, int arg2) {
      System.out.println("glUniformBlockBinding(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniformBlockBinding(arg0, arg1, arg2);
   }

   public void glUniformMatrix2fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println("glUniformMatrix2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniformMatrix2fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix2fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println("glUniformMatrix2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniformMatrix2fv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix2x3fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println("glUniformMatrix2x3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniformMatrix2x3fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix2x3fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println("glUniformMatrix2x3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniformMatrix2x3fv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix2x4fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println("glUniformMatrix2x4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniformMatrix2x4fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix2x4fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println("glUniformMatrix2x4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniformMatrix2x4fv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix3fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println("glUniformMatrix3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniformMatrix3fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix3fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println("glUniformMatrix3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniformMatrix3fv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix3x2fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println("glUniformMatrix3x2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniformMatrix3x2fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix3x2fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println("glUniformMatrix3x2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniformMatrix3x2fv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix3x4fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println("glUniformMatrix3x4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniformMatrix3x4fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix3x4fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println("glUniformMatrix3x4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniformMatrix3x4fv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix4fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println("glUniformMatrix4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniformMatrix4fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix4fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println("glUniformMatrix4fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniformMatrix4fv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix4x2fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println("glUniformMatrix4x2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniformMatrix4x2fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix4x2fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println("glUniformMatrix4x2fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniformMatrix4x2fv(arg0, arg1, arg2, arg3);
   }

   public void glUniformMatrix4x3fv(
      int arg0, int arg1, boolean arg2, float[] arg3, int arg4) {
      System.out.println("glUniformMatrix4x3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glUniformMatrix4x3fv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glUniformMatrix4x3fv(
      int arg0, int arg1, boolean arg2, FloatBuffer arg3) {
      System.out.println("glUniformMatrix4x3fv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniformMatrix4x3fv(arg0, arg1, arg2, arg3);
   }

   public void glUniformui64NV(int arg0, long arg1) {
      System.out.println("glUniformui64NV(" + arg0 + "," + arg1 + ")");
      gl.glUniformui64NV(arg0, arg1);
   }

   public void glUniformui64vNV(int arg0, int arg1, long[] arg2, int arg3) {
      System.out.println("glUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glUniformui64vNV(arg0, arg1, arg2, arg3);
   }

   public void glUniformui64vNV(int arg0, int arg1, LongBuffer arg2) {
      System.out.println("glUniformui64vNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glUniformui64vNV(arg0, arg1, arg2);
   }

   public boolean glUnmapBuffer(int arg0) {
      System.out.println("glUnmapBuffer(" + arg0 + ")");
      return gl.glUnmapBuffer(arg0);
   }

   public void glUseProgram(int arg0) {
      System.out.println("glUseProgram(" + arg0 + ")");
      gl.glUseProgram(arg0);
   }

   public void glValidateProgram(int arg0) {
      System.out.println("glValidateProgram(" + arg0 + ")");
      gl.glValidateProgram(arg0);
   }

   public void glVertexArrayBindVertexBufferEXT(
      int arg0, int arg1, int arg2, long arg3, int arg4) {
      System.out.println("glVertexArrayBindVertexBufferEXT(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexArrayBindVertexBufferEXT(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexArrayVertexAttribBindingEXT(
      int arg0, int arg1, int arg2) {
      System.out.println("glVertexArrayVertexAttribBindingEXT(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexArrayVertexAttribBindingEXT(arg0, arg1, arg2);
   }

   public void glVertexArrayVertexAttribFormatEXT(
      int arg0, int arg1, int arg2, int arg3, boolean arg4, int arg5) {
      System.out.println("glVertexArrayVertexAttribFormatEXT(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glVertexArrayVertexAttribFormatEXT(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glVertexArrayVertexAttribIFormatEXT(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glVertexArrayVertexAttribIFormatEXT(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexArrayVertexAttribIFormatEXT(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexArrayVertexAttribLFormatEXT(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glVertexArrayVertexAttribLFormatEXT(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexArrayVertexAttribLFormatEXT(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexArrayVertexBindingDivisorEXT(
      int arg0, int arg1, int arg2) {
      System.out.println("glVertexArrayVertexBindingDivisorEXT(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexArrayVertexBindingDivisorEXT(arg0, arg1, arg2);
   }

   public void glVertexAttrib1d(int arg0, double arg1) {
      System.out.println("glVertexAttrib1d(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1d(arg0, arg1);
   }

   public void glVertexAttrib1dv(int arg0, double[] arg1, int arg2) {
      System.out.println("glVertexAttrib1dv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib1dv(arg0, arg1, arg2);
   }

   public void glVertexAttrib1dv(int arg0, DoubleBuffer arg1) {
      System.out.println("glVertexAttrib1dv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1dv(arg0, arg1);
   }

   public void glVertexAttrib1f(int arg0, float arg1) {
      System.out.println("glVertexAttrib1f(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1f(arg0, arg1);
   }

   public void glVertexAttrib1fv(int arg0, float[] arg1, int arg2) {
      System.out.println("glVertexAttrib1fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib1fv(arg0, arg1, arg2);
   }

   public void glVertexAttrib1fv(int arg0, FloatBuffer arg1) {
      System.out.println("glVertexAttrib1fv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1fv(arg0, arg1);
   }

   public void glVertexAttrib1s(int arg0, short arg1) {
      System.out.println("glVertexAttrib1s(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1s(arg0, arg1);
   }

   public void glVertexAttrib1sv(int arg0, short[] arg1, int arg2) {
      System.out.println("glVertexAttrib1sv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib1sv(arg0, arg1, arg2);
   }

   public void glVertexAttrib1sv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib1sv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib1sv(arg0, arg1);
   }

   public void glVertexAttrib2d(int arg0, double arg1, double arg2) {
      System.out.println("glVertexAttrib2d(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2d(arg0, arg1, arg2);
   }

   public void glVertexAttrib2dv(int arg0, double[] arg1, int arg2) {
      System.out.println("glVertexAttrib2dv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2dv(arg0, arg1, arg2);
   }

   public void glVertexAttrib2dv(int arg0, DoubleBuffer arg1) {
      System.out.println("glVertexAttrib2dv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib2dv(arg0, arg1);
   }

   public void glVertexAttrib2f(int arg0, float arg1, float arg2) {
      System.out.println("glVertexAttrib2f(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2f(arg0, arg1, arg2);
   }

   public void glVertexAttrib2fv(int arg0, float[] arg1, int arg2) {
      System.out.println("glVertexAttrib2fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2fv(arg0, arg1, arg2);
   }

   public void glVertexAttrib2fv(int arg0, FloatBuffer arg1) {
      System.out.println("glVertexAttrib2fv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib2fv(arg0, arg1);
   }

   public void glVertexAttrib2s(int arg0, short arg1, short arg2) {
      System.out.println("glVertexAttrib2s(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2s(arg0, arg1, arg2);
   }

   public void glVertexAttrib2sv(int arg0, short[] arg1, int arg2) {
      System.out.println("glVertexAttrib2sv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib2sv(arg0, arg1, arg2);
   }

   public void glVertexAttrib2sv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib2sv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib2sv(arg0, arg1);
   }

   public void glVertexAttrib3d(
      int arg0, double arg1, double arg2, double arg3) {
      System.out.println("glVertexAttrib3d(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttrib3d(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttrib3dv(int arg0, double[] arg1, int arg2) {
      System.out.println("glVertexAttrib3dv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib3dv(arg0, arg1, arg2);
   }

   public void glVertexAttrib3dv(int arg0, DoubleBuffer arg1) {
      System.out.println("glVertexAttrib3dv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib3dv(arg0, arg1);
   }

   public void glVertexAttrib3f(int arg0, float arg1, float arg2, float arg3) {
      System.out.println("glVertexAttrib3f(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttrib3f(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttrib3fv(int arg0, float[] arg1, int arg2) {
      System.out.println("glVertexAttrib3fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib3fv(arg0, arg1, arg2);
   }

   public void glVertexAttrib3fv(int arg0, FloatBuffer arg1) {
      System.out.println("glVertexAttrib3fv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib3fv(arg0, arg1);
   }

   public void glVertexAttrib3s(int arg0, short arg1, short arg2, short arg3) {
      System.out.println("glVertexAttrib3s(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttrib3s(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttrib3sv(int arg0, short[] arg1, int arg2) {
      System.out.println("glVertexAttrib3sv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib3sv(arg0, arg1, arg2);
   }

   public void glVertexAttrib3sv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib3sv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib3sv(arg0, arg1);
   }

   public void glVertexAttrib4Nbv(int arg0, byte[] arg1, int arg2) {
      System.out.println("glVertexAttrib4Nbv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Nbv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4Nbv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttrib4Nbv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Nbv(arg0, arg1);
   }

   public void glVertexAttrib4Niv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttrib4Niv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Niv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4Niv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttrib4Niv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Niv(arg0, arg1);
   }

   public void glVertexAttrib4Nsv(int arg0, short[] arg1, int arg2) {
      System.out.println("glVertexAttrib4Nsv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Nsv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4Nsv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib4Nsv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Nsv(arg0, arg1);
   }

   public void glVertexAttrib4Nub(
      int arg0, byte arg1, byte arg2, byte arg3, byte arg4) {
      System.out.println("glVertexAttrib4Nub(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttrib4Nub(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttrib4Nubv(int arg0, byte[] arg1, int arg2) {
      System.out.println("glVertexAttrib4Nubv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Nubv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4Nubv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttrib4Nubv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Nubv(arg0, arg1);
   }

   public void glVertexAttrib4Nuiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttrib4Nuiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Nuiv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4Nuiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttrib4Nuiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Nuiv(arg0, arg1);
   }

   public void glVertexAttrib4Nusv(int arg0, short[] arg1, int arg2) {
      System.out.println("glVertexAttrib4Nusv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4Nusv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4Nusv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib4Nusv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4Nusv(arg0, arg1);
   }

   public void glVertexAttrib4bv(int arg0, byte[] arg1, int arg2) {
      System.out.println("glVertexAttrib4bv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4bv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4bv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttrib4bv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4bv(arg0, arg1);
   }

   public void glVertexAttrib4d(
      int arg0, double arg1, double arg2, double arg3, double arg4) {
      System.out.println("glVertexAttrib4d(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttrib4d(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttrib4dv(int arg0, double[] arg1, int arg2) {
      System.out.println("glVertexAttrib4dv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4dv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4dv(int arg0, DoubleBuffer arg1) {
      System.out.println("glVertexAttrib4dv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4dv(arg0, arg1);
   }

   public void glVertexAttrib4f(
      int arg0, float arg1, float arg2, float arg3, float arg4) {
      System.out.println("glVertexAttrib4f(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttrib4f(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttrib4fv(int arg0, float[] arg1, int arg2) {
      System.out.println("glVertexAttrib4fv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4fv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4fv(int arg0, FloatBuffer arg1) {
      System.out.println("glVertexAttrib4fv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4fv(arg0, arg1);
   }

   public void glVertexAttrib4iv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttrib4iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4iv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4iv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttrib4iv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4iv(arg0, arg1);
   }

   public void glVertexAttrib4s(
      int arg0, short arg1, short arg2, short arg3, short arg4) {
      System.out.println("glVertexAttrib4s(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttrib4s(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttrib4sv(int arg0, short[] arg1, int arg2) {
      System.out.println("glVertexAttrib4sv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4sv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4sv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib4sv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4sv(arg0, arg1);
   }

   public void glVertexAttrib4ubv(int arg0, byte[] arg1, int arg2) {
      System.out.println("glVertexAttrib4ubv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4ubv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4ubv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttrib4ubv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4ubv(arg0, arg1);
   }

   public void glVertexAttrib4uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttrib4uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4uiv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttrib4uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4uiv(arg0, arg1);
   }

   public void glVertexAttrib4usv(int arg0, short[] arg1, int arg2) {
      System.out.println("glVertexAttrib4usv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttrib4usv(arg0, arg1, arg2);
   }

   public void glVertexAttrib4usv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttrib4usv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttrib4usv(arg0, arg1);
   }

   public void glVertexAttribDivisor(int arg0, int arg1) {
      System.out.println("glVertexAttribDivisor(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribDivisor(arg0, arg1);
   }

   public void glVertexAttribFormatNV(
      int arg0, int arg1, int arg2, boolean arg3, int arg4) {
      System.out.println("glVertexAttribFormatNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttribFormatNV(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttribI1i(int arg0, int arg1) {
      System.out.println("glVertexAttribI1i(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI1i(arg0, arg1);
   }

   public void glVertexAttribI1iv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttribI1iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI1iv(arg0, arg1, arg2);
   }

   public void glVertexAttribI1iv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI1iv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI1iv(arg0, arg1);
   }

   public void glVertexAttribI1ui(int arg0, int arg1) {
      System.out.println("glVertexAttribI1ui(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI1ui(arg0, arg1);
   }

   public void glVertexAttribI1uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttribI1uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI1uiv(arg0, arg1, arg2);
   }

   public void glVertexAttribI1uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI1uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI1uiv(arg0, arg1);
   }

   public void glVertexAttribI2i(int arg0, int arg1, int arg2) {
      System.out.println("glVertexAttribI2i(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI2i(arg0, arg1, arg2);
   }

   public void glVertexAttribI2iv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttribI2iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI2iv(arg0, arg1, arg2);
   }

   public void glVertexAttribI2iv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI2iv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI2iv(arg0, arg1);
   }

   public void glVertexAttribI2ui(int arg0, int arg1, int arg2) {
      System.out.println("glVertexAttribI2ui(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI2ui(arg0, arg1, arg2);
   }

   public void glVertexAttribI2uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttribI2uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI2uiv(arg0, arg1, arg2);
   }

   public void glVertexAttribI2uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI2uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI2uiv(arg0, arg1);
   }

   public void glVertexAttribI3i(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glVertexAttribI3i(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttribI3i(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribI3iv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttribI3iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI3iv(arg0, arg1, arg2);
   }

   public void glVertexAttribI3iv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI3iv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI3iv(arg0, arg1);
   }

   public void glVertexAttribI3ui(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glVertexAttribI3ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttribI3ui(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribI3uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttribI3uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI3uiv(arg0, arg1, arg2);
   }

   public void glVertexAttribI3uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI3uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI3uiv(arg0, arg1);
   }

   public void glVertexAttribI4bv(int arg0, byte[] arg1, int arg2) {
      System.out.println("glVertexAttribI4bv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4bv(arg0, arg1, arg2);
   }

   public void glVertexAttribI4bv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttribI4bv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4bv(arg0, arg1);
   }

   public void glVertexAttribI4i(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glVertexAttribI4i(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttribI4i(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttribI4iv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttribI4iv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4iv(arg0, arg1, arg2);
   }

   public void glVertexAttribI4iv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI4iv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4iv(arg0, arg1);
   }

   public void glVertexAttribI4sv(int arg0, short[] arg1, int arg2) {
      System.out.println("glVertexAttribI4sv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4sv(arg0, arg1, arg2);
   }

   public void glVertexAttribI4sv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttribI4sv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4sv(arg0, arg1);
   }

   public void glVertexAttribI4ubv(int arg0, byte[] arg1, int arg2) {
      System.out.println("glVertexAttribI4ubv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4ubv(arg0, arg1, arg2);
   }

   public void glVertexAttribI4ubv(int arg0, ByteBuffer arg1) {
      System.out.println("glVertexAttribI4ubv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4ubv(arg0, arg1);
   }

   public void glVertexAttribI4ui(
      int arg0, int arg1, int arg2, int arg3, int arg4) {
      System.out.println("glVertexAttribI4ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttribI4ui(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttribI4uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexAttribI4uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4uiv(arg0, arg1, arg2);
   }

   public void glVertexAttribI4uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexAttribI4uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4uiv(arg0, arg1);
   }

   public void glVertexAttribI4usv(int arg0, short[] arg1, int arg2) {
      System.out.println("glVertexAttribI4usv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexAttribI4usv(arg0, arg1, arg2);
   }

   public void glVertexAttribI4usv(int arg0, ShortBuffer arg1) {
      System.out.println("glVertexAttribI4usv(" + arg0 + "," + arg1 + ")");
      gl.glVertexAttribI4usv(arg0, arg1);
   }

   public void glVertexAttribIFormatNV(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glVertexAttribIFormatNV(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttribIFormatNV(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribIPointer(
      int arg0, int arg1, int arg2, int arg3, long arg4) {
      System.out.println("glVertexAttribIPointer(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttribIPointer(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttribP1ui(int arg0, int arg1, boolean arg2, int arg3) {
      System.out.println("glVertexAttribP1ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttribP1ui(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribP1uiv(
      int arg0, int arg1, boolean arg2, int[] arg3, int arg4) {
      System.out.println("glVertexAttribP1uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttribP1uiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttribP1uiv(
      int arg0, int arg1, boolean arg2, IntBuffer arg3) {
      System.out.println("glVertexAttribP1uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttribP1uiv(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribP2ui(int arg0, int arg1, boolean arg2, int arg3) {
      System.out.println("glVertexAttribP2ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttribP2ui(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribP2uiv(
      int arg0, int arg1, boolean arg2, int[] arg3, int arg4) {
      System.out.println("glVertexAttribP2uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttribP2uiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttribP2uiv(
      int arg0, int arg1, boolean arg2, IntBuffer arg3) {
      System.out.println("glVertexAttribP2uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttribP2uiv(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribP3ui(int arg0, int arg1, boolean arg2, int arg3) {
      System.out.println("glVertexAttribP3ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttribP3ui(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribP3uiv(
      int arg0, int arg1, boolean arg2, int[] arg3, int arg4) {
      System.out.println("glVertexAttribP3uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttribP3uiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttribP3uiv(
      int arg0, int arg1, boolean arg2, IntBuffer arg3) {
      System.out.println("glVertexAttribP3uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttribP3uiv(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribP4ui(int arg0, int arg1, boolean arg2, int arg3) {
      System.out.println("glVertexAttribP4ui(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttribP4ui(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribP4uiv(
      int arg0, int arg1, boolean arg2, int[] arg3, int arg4) {
      System.out.println("glVertexAttribP4uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
      gl.glVertexAttribP4uiv(arg0, arg1, arg2, arg3, arg4);
   }

   public void glVertexAttribP4uiv(
      int arg0, int arg1, boolean arg2, IntBuffer arg3) {
      System.out.println("glVertexAttribP4uiv(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glVertexAttribP4uiv(arg0, arg1, arg2, arg3);
   }

   public void glVertexAttribPointer(GLArrayData arg0) {
      System.out.println("glVertexAttribPointer(" + arg0 + ")");
      gl.glVertexAttribPointer(arg0);
   }

   public void glVertexAttribPointer(
      int arg0, int arg1, int arg2, boolean arg3, int arg4, long arg5) {
      System.out.println("glVertexAttribPointer(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + ")");
      gl.glVertexAttribPointer(arg0, arg1, arg2, arg3, arg4, arg5);
   }

   public void glVertexFormatNV(int arg0, int arg1, int arg2) {
      System.out.println("glVertexFormatNV(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexFormatNV(arg0, arg1, arg2);
   }

   public void glVertexP2ui(int arg0, int arg1) {
      System.out.println("glVertexP2ui(" + arg0 + "," + arg1 + ")");
      gl.glVertexP2ui(arg0, arg1);
   }

   public void glVertexP2uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexP2uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexP2uiv(arg0, arg1, arg2);
   }

   public void glVertexP2uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexP2uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexP2uiv(arg0, arg1);
   }

   public void glVertexP3ui(int arg0, int arg1) {
      System.out.println("glVertexP3ui(" + arg0 + "," + arg1 + ")");
      gl.glVertexP3ui(arg0, arg1);
   }

   public void glVertexP3uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexP3uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexP3uiv(arg0, arg1, arg2);
   }

   public void glVertexP3uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexP3uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexP3uiv(arg0, arg1);
   }

   public void glVertexP4ui(int arg0, int arg1) {
      System.out.println("glVertexP4ui(" + arg0 + "," + arg1 + ")");
      gl.glVertexP4ui(arg0, arg1);
   }

   public void glVertexP4uiv(int arg0, int[] arg1, int arg2) {
      System.out.println("glVertexP4uiv(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glVertexP4uiv(arg0, arg1, arg2);
   }

   public void glVertexP4uiv(int arg0, IntBuffer arg1) {
      System.out.println("glVertexP4uiv(" + arg0 + "," + arg1 + ")");
      gl.glVertexP4uiv(arg0, arg1);
   }

   public void glViewport(int arg0, int arg1, int arg2, int arg3) {
      System.out.println("glViewport(" + arg0 + "," + arg1 + "," + arg2 + "," + arg3 + ")");
      gl.glViewport(arg0, arg1, arg2, arg3);
   }

   public void glWaitSync(long arg0, int arg1, long arg2) {
      System.out.println("glWaitSync(" + arg0 + "," + arg1 + "," + arg2 + ")");
      gl.glWaitSync(arg0, arg1, arg2);
   }

   public boolean isGL() {
      return gl.isGL();
   }

   public boolean isGL4bc() {
      return gl.isGL4bc();
   }

   public boolean isGL4() {
      return gl.isGL4();
   }

   public boolean isGL3bc() {
      return gl.isGL3bc();
   }

   public boolean isGL3() {
      return gl.isGL3();
   }

   public boolean isGL2() {
      return gl.isGL2();
   }

   public boolean isGLES1() {
      return gl.isGLES1();
   }

   public boolean isGLES2() {
      return gl.isGLES2();
   }

   public boolean isGLES3() {
      return gl.isGLES3();
   }

   public boolean isGLES() {
      return gl.isGLES();
   }

   public boolean isGL2ES1() {
      return gl.isGL2ES1();
   }

   public boolean isGL2ES2() {
      return gl.isGL2ES2();
   }

   public boolean isGL2ES3() {
      return gl.isGL2ES3();
   }

   public boolean isGL3ES3() {
      return gl.isGL3ES3();
   }

   public boolean isGL4ES3() {
      return gl.isGL4ES3();
   }

   public boolean isGL2GL3() {
      return gl.isGL2GL3();
   }

   public boolean isGL4core() {
      return gl.isGL4core();
   }

   public boolean isGL3core() {
      return gl.isGL3core();
   }

   public boolean isGLcore() {
      return gl.isGLcore();
   }

   public boolean isGLES2Compatible() {
      return gl.isGLES2Compatible();
   }

   public boolean isGLES3Compatible() {
      return gl.isGLES3Compatible();
   }

   public boolean hasGLSL() {
      return gl.hasGLSL();
   }

   public boolean isFunctionAvailable(String glFunctionName) {
      return gl.isFunctionAvailable(glFunctionName);
   }

   public boolean isExtensionAvailable(String glExtensionName) {
      return gl.isExtensionAvailable(glExtensionName);
   }

   public boolean hasBasicFBOSupport() {
      return gl.hasBasicFBOSupport();
   }

   public boolean hasFullFBOSupport() {
      return gl.hasFullFBOSupport();
   }

   public boolean isNPOTTextureAvailable() {
      return gl.isNPOTTextureAvailable();
   }

   public boolean isPBOPackBound() {
      return gl.isPBOPackBound();
   }

   public boolean isPBOUnpackBound() {
      return gl.isPBOUnpackBound();
   }

   public boolean isTextureFormatBGRA8888Available() {
      return gl.isTextureFormatBGRA8888Available();
   }

   public void setSwapInterval(int interval) {
      gl.setSwapInterval(interval);
   }

   public GLBufferStorage mapBuffer(int target, int access) throws GLException {
      return gl.mapBuffer(target, access);
   }

   public GLBufferStorage mapBufferRange(
      int target, long offset, long length, int access) throws GLException {
      return gl.mapBufferRange(target, offset, length, access);
   }

   public boolean isVBOArrayBound() {
      return gl.isVBOArrayBound();
   }

   public boolean isVBOElementArrayBound() {
      return gl.isVBOElementArrayBound();
   }
   
   
}
