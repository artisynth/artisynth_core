package maspack.render.GL.GL3;

import java.awt.Dimension;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.swing.event.MouseInputListener;

import artisynth.core.util.ArtisynthPath;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.Dragger3d;
import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectState;
import maspack.render.RenderProps;
import maspack.render.GL.GLClipPlane;
import maspack.render.GL.GLFrameCapture;
import maspack.render.GL.GLGridPlane;
import maspack.render.GL.GLLightManager;
import maspack.render.GL.GLMouseAdapter;
import maspack.render.GL.GLShaderProgram;
import maspack.render.GL.GLSupport;
import maspack.render.GL.GLTexture;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GL3.GL3ProgramManager.RenderMode;
import maspack.render.GL.GL3.GLSLGenerator.StringIntPair;
import maspack.render.GL.GL3.GLSLInfo.GLSLInfoBuilder;
//import maspack.render.GL.GL3.GLSLInfo.ColorInterpolation;
import maspack.render.GL.GL3.GLSLInfo.InstancedRendering;
import maspack.util.InternalErrorException;

public class GL3Viewer extends GLViewer {

   GL3 gl;

   //=====================================
   // temporaries for debugging purposes
   //   GLSLInfo proginfo;
   //   int prog = 0;
   //   int progflatvcolors = 0;
   //   int progflat = 0;
   //=====================================

   // Programs
   GL3ProgramManager myProgManager = null;

   GL3SharedResources myGLResources = null;    // holds shared context and cache

   // Resources that stick with this viewer
   GL3RenderObjectManager myRenderObjectManager = null;
   GL3PrimitiveManager myPrimitiveManager = null;

   // Updateable object for various primitives (essentially stream-drawn)
   // like single lines, etc...
   GL3FlexObject gloFlex = null;

   // screenshot
   private GLFrameCapture frameCapture = null;
   private boolean grab = false;
   private boolean grabWaitComplete = false; // wait
   private boolean grabClose = false;        // clean up

   // buffer filling
   static final PositionBufferPutter DEFAULT_POSITON_PUTTER = PositionBufferPutter.getDefault ();
   static final NormalBufferPutter DEFAULT_NORMAL_PUTTER = NormalBufferPutter.getDefault ();
   static final ColorBufferPutter DEFAULT_COLOR_PUTTER = ColorBufferPutter.getDefault ();

   /**
    * Creates a new GLViewer with default capabilities.
    * 
    * @param width
    * initial width of the viewer
    * @param height
    * initial height of the viewer
    */
   public GL3Viewer (int width, int height) {
      this (null, null, width, height);
   }

   /**
    * Creates a new GLViewer with default capabilities that shares resources
    * (e.g., diplay lists and textures) with an existing GLViewer.
    * 
    * @param shareWith
    * GLViewer with which resources are to be shared
    * @param width
    * initial width of the viewer
    * @param height
    * initial height of the viewer
    */
   public GL3Viewer (GL3Viewer shareWith, int width, int height) {
      this (null, shareWith.myGLResources, width, height);
   }

   /**
    * Creates a new GLViewer with specified capabilities and size.
    * 
    * @param cap
    * Desired GL capabilities. Can be specified as null, which will create
    * default capabilities.
    * @param resources Resources to be used by the viewer.
    * Can be specified as null, which will create
    * default resources.
    * @param width
    * initial width of the viewer
    * @param height
    * initial height of the viewer
    */
   public GL3Viewer (GLCapabilities cap, GL3SharedResources resources, int width,
      int height) {
      if (cap == null) {
         GLProfile glp3 = GLProfile.get(GLProfile.GL3);
         cap = new GLCapabilities(glp3);
         cap.setSampleBuffers (true);
         cap.setNumSamples (8);
      }

      if (resources == null) {
         // get attribute map from GLSL generator
         StringIntPair[] attributes = GLSLGenerator.ATTRIBUTES;
         GL3VertexAttributeMap attributeMap = new GL3VertexAttributeMap (
            new GL3VertexAttributeInfo (attributes[0].getString (), attributes[0].getInt ()), 
            new GL3VertexAttributeInfo (attributes[1].getString (), attributes[1].getInt ()),
            new GL3VertexAttributeInfo (attributes[2].getString (), attributes[2].getInt ()),
            new GL3VertexAttributeInfo (attributes[3].getString (), attributes[3].getInt ()));
         for (int i=4; i<attributes.length; ++i) {
            attributeMap.add (new GL3VertexAttributeInfo (attributes[i].getString (), attributes[i].getInt ()));
         }
         resources = new GL3SharedResources(cap, attributeMap);
      }
      myGLResources = resources;
      canvas = myGLResources.createCanvas();
      myGLResources.registerViewer (this);
      myPrimitiveManager = new GL3PrimitiveManager (resources.getSharedPrimitiveManager());
      myRenderObjectManager = new GL3RenderObjectManager (resources.getSharedRenderObjectManager());

      lightManager = new GLLightManager();      
      myProgManager = new GL3ProgramManager();

      canvas.addGLEventListener (this);

      canvas.setPreferredSize(new Dimension(width, height));
      canvas.setSize (width, height);

      this.width = width;
      this.height = height;

      myDraggers = new LinkedList<Dragger3d>();
      myUserDraggers = new LinkedList<Dragger3d>();

      myGrid = new GLGridPlane();
      myGrid.setViewer (this);

      GLMouseAdapter mouse = new GLMouseAdapter (this);
      myMouseHandler = mouse;

      setDefaultLights();
      setDefaultMatrices();

      if (canvas != null) {
         // canvas.addMouseListener(new GLMouseListener());
         canvas.addMouseListener (myMouseHandler);
         canvas.addMouseWheelListener (myMouseHandler);
         canvas.addMouseMotionListener (myMouseHandler);
         for (MouseInputListener l : myMouseInputListeners) {
            canvas.addMouseListener (l);
            canvas.addMouseMotionListener (l);
         }
         for (MouseWheelListener l : myMouseWheelListeners) {
            canvas.addMouseWheelListener (l);
         }
      }

   }

   private void setDefaultMatrices() {

      RigidTransform3d EyeToWorld = new RigidTransform3d (0, -3, 0, 1, 0, 0, Math.PI / 2);
      setEyeToWorld(EyeToWorld);

      setAxialView (DEFAULT_AXIAL_VIEW);
      setModelMatrix(RigidTransform3d.IDENTITY);

      //      System.out.println(projectionMatrix);
      //      System.out.println(viewMatrix);
      //      System.out.println(modelMatrix);
   }

   @Override
   public void init(GLAutoDrawable drawable) {
      GLSupport.checkAndPrintGLError(drawable.getGL ());

      this.drawable = drawable;
      gl = drawable.getGL().getGL3();

      String renderer = gl.glGetString(GL.GL_RENDERER);
      String version = gl.glGetString(GL.GL_VERSION);
      int[] buff = new int[2];
      gl.glGetIntegerv(GL3.GL_MAJOR_VERSION, buff, 0);
      gl.glGetIntegerv(GL3.GL_MINOR_VERSION, buff, 1);

      System.out.println("GL Renderer: " + renderer);
      System.out.println("OpenGL Version: " + version + " (" + buff[0] + "," + buff[1] + ")");

      gl.setSwapInterval (1);

      if (gl.isExtensionAvailable("GL_ARB_multisample")) {
         gl.glEnable(GL3.GL_MULTISAMPLE);
      }

      gl.glGetIntegerv(GL3.GL_MAX_CLIP_DISTANCES, buff, 0);
      maxClipPlanes = buff[0];

      selectEnabled = false;
      selectTrigger = false;
      setLightingEnabled(true);
      setDepthEnabled(true);
      setColorEnabled(true);
      setVertexColoringEnabled(true);
      setTextureMappingEnabled(true);
      setFaceStyle(FaceStyle.FRONT);
      setShading(Shading.PHONG);
      setGammaCorrectionEnabled(true);

      // gl.glClearDepth (1.0);
      // gl.glDepthFunc(GL.GL_LESS);

      // smooth + blending
      // gl.glEnable(GL.GL_LINE_SMOOTH);

      // XXX do I need to save this as part of the state?
      // gl.glEnable(GL.GL_BLEND);
      // gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

      // gl.glEnable(GL3.GL_POLYGON_SMOOTH);
      // gl.glHint(GL3.GL_POLYGON_SMOOTH_HINT, GL.GL_NICEST);
      gl.glClearColor (bgColor[0], bgColor[1], bgColor[2], bgColor[3]);

      resetViewVolume();
      invalidateModelMatrix();
      invalidateProjectionMatrix();
      invalidateViewMatrix();

      myProgManager.init(gl, lightManager.numLights(), 0);
      myProgManager.setMatrices(gl, projectionMatrix, viewMatrix, modelMatrix, modelNormalMatrix);
      myProgManager.setLights(gl, lightManager.getLights(), 1.0f/lightManager.getMaxIntensity(), viewMatrix);
      myProgManager.setMaterials (gl, myCurrentMaterial, myCurrentMaterial);
      myCurrentMaterialModified = true;  // trigger update of materials

      // create a basic position-based flexible object
      gloFlex = GL3FlexObject.generate (gl, 
         myGLResources.getVertexPositionAttribute (), myGLResources.getVertexNormalAttribute(), 
         myGLResources.getVertexColorAttribute(), myGLResources.getVertexTexcoordAttribute());

      // trigger rebuild of renderables
      buildInternalRenderList();

      System.out.println("GL3 initialized");

      GLSupport.checkAndPrintGLError(drawable.getGL ());

   }

   @Override
   public void dispose(GLAutoDrawable drawable) {
      GLSupport.checkAndPrintGLError(drawable.getGL ());

      myProgManager.dispose(gl);
      myRenderObjectManager.dispose (gl);
      myPrimitiveManager.dispose (gl);

      // clear temporaries
      gloFlex.dispose (gl);
      gloFlex = null;

      // nullify stuff
      this.drawable = null;
      this.gl = null;

      System.out.println("GL3 disposed");
      GLSupport.checkAndPrintGLError(drawable.getGL ());
   }

   @Override
   public GL2 getGL2() {
      throw new RuntimeException("Cannot obtain a GL2 from a GL3Viewer");
   }

   public void setPointSize(float s) {
      gl.glPointSize(s);
   }

   public float getPointSize() {
      float[] buff = new float[1];
      gl.glGetFloatv(GL.GL_POINT_SIZE, buff, 0);
      return buff[0];
   }

   public void setLineWidth(float w) {
      gl.glLineWidth(w);
   }

   public float getLineWidth() {
      float[] buff = new float[1];
      gl.glGetFloatv(GL.GL_LINE_WIDTH, buff, 0);
      return buff[0];
   }

   @Override
   public void setViewport(int x, int y, int width, int height) {
      gl.glViewport(x, y, width, height);
   }

   protected int[] getViewport() {
      int[] buff = new int[4];
      gl.glGetIntegerv(GL.GL_VIEWPORT, buff, 0);
      return buff;
   }

   @Override
   public void display(GLAutoDrawable drawable, int flags) {

      GLSupport.checkAndPrintGLError(drawable.getGL ());

      if (!myInternalRenderListValid) {
         buildInternalRenderList();
      }

      GLSupport.checkAndPrintGLError(gl);

      if (selectTrigger) {
         mySelector.setupSelection (drawable);
         selectEnabled = true;
         selectTrigger = false;
      }

      // turn off buffer swapping when doing a selection render because
      // otherwise the previous buffer sometimes gets displayed
      drawable.setAutoSwapBufferMode (selectEnabled ? false : true);

      resetViewVolume = false;           //disable resetting view volume
      doDisplay (drawable, flags);

      if (selectEnabled) {
         selectEnabled = false;
         mySelector.processSelection (drawable);
      }
      else {
         fireRerenderListeners();
      }

      if (frameCapture != null) {
         synchronized(frameCapture) {
            if (grab) {
               offscreenCapture (flags);
               grab = false;
            }
            if (grabWaitComplete) {
               frameCapture.waitForCompletion();
               // reset
               grabWaitComplete = false;
            }
            if (grabClose) {
               frameCapture.waitForCompletion();
               frameCapture.dispose(gl);
               frameCapture = null;
            }
         }
      }

      GLSupport.checkAndPrintGLError(drawable.getGL ());
   }

   private boolean hasTransparent3d() {
      if (myInternalRenderList.numTransparent() > 0) {
         return true;
      }
      if (myExternalRenderList != null) {
         if (myExternalRenderList.numTransparent() > 0) {
            return true;
         }
      }
      return false;
   }

   private boolean has2d() {
      if (myInternalRenderList.numOpaque2d() > 0 ||
      myInternalRenderList.numTransparent2d() > 0) {
         return true;
      }
      if (myExternalRenderList != null) {
         if (myExternalRenderList.numOpaque2d() > 0 || 
         myExternalRenderList.numTransparent2d() > 0) {
            return true;
         }
      }
      return false;
   }

   private boolean hasTransparent2d() {
      if (myInternalRenderList.numTransparent2d() > 0) {
         return true;
      }
      if (myExternalRenderList != null) {
         if (myExternalRenderList.numTransparent2d() > 0) {
            return true;
         }
      }
      return false;
   }

   private void doDisplay(GLAutoDrawable drawable, int flags) {
      GLSupport.checkAndPrintGLError(drawable.getGL ());

      int mclips = Math.min(2*myClipPlanes.size(), maxClipPlanes);
      myProgManager.reconfigure(gl, lightManager.numLights(), mclips);
      myProgManager.setLights(gl, lightManager.getLights(), 1.0f/lightManager.getMaxIntensity(), viewMatrix);

      // update matrices
      maybeUpdateState(gl);

      // clear background/depth
      GL3 gl3 = drawable.getGL().getGL3();

      if (!isSelecting()) {
         gl3.glClearColor (bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
      }
      gl3.glClear(GL.GL_COLOR_BUFFER_BIT |  GL.GL_DEPTH_BUFFER_BIT);

      if (!isSelecting()) {
         if (gridVisible) {
            myGrid.render (this, flags);
         }
         if (axisLength > 0) {
            drawAxes (gl, axisLength);
         }

         // rendering dragger separately here so that they are
         // not clipped by the clipping plane
         for (Dragger3d dragger : myDraggers) {
            dragger.render (this, 0);
         }
         if (myDrawTool != null) {
            myDrawTool.render (this, 0);
         }

         for (GLClipPlane cp : myClipPlanes) {
            cp.render (this, flags);
         }
      }
      GLSupport.checkAndPrintGLError(gl);

      // enable clip planes
      int nclips = 0;
      if (myClipPlanes.size() > 0) {
         nclips = myProgManager.setClipPlanes(gl3, myClipPlanes);
         for (int i=0; i<nclips; ++i) {
            gl.glEnable(GL3.GL_CLIP_DISTANCE0+i);
         }
      }
      GLSupport.checkAndPrintGLError(gl);

      int qid = 0;
      synchronized(renderablesLock) {
         qid = myInternalRenderList.renderOpaque (this, qid, flags);
         if (myExternalRenderList != null) {
            qid = myExternalRenderList.renderOpaque (this, qid, flags);
         }
      }
      GLSupport.checkAndPrintGLError(gl);

      if (hasTransparent3d()) {
         if (!isSelecting()) {
            enableTransparency (gl);
         }

         synchronized(renderablesLock) {
            qid = myInternalRenderList.renderTransparent (this, qid, flags);
            if (myExternalRenderList != null) {
               qid = myExternalRenderList.renderTransparent (this, qid, flags);
            }
         }
         if (!isSelecting()) {
            disableTransparency (gl);
         }
      }
      GLSupport.checkAndPrintGLError(gl);

      // disable clip planes
      for (int i=0; i<nclips; ++i) {
         gl.glDisable(GL3.GL_CLIP_DISTANCE0+i);
      }
      GLSupport.checkAndPrintGLError(gl);

      // Draw 2D objects
      if ( has2d() ) {
         begin2DRendering(width, height);

         synchronized(renderablesLock) {
            qid = myInternalRenderList.renderOpaque2d (this, qid, 0);
            if (myExternalRenderList != null) {
               qid = myExternalRenderList.renderOpaque2d (this, qid, 0);
            }
         }

         if ( hasTransparent2d() ) {
            enableTransparency (gl);

            synchronized(renderablesLock) {
               qid = myInternalRenderList.renderTransparent2d (this, qid, 0);
               if (myExternalRenderList != null) {
                  qid = myExternalRenderList.renderTransparent2d (this, qid, 0);
               }
            }
            disableTransparency (gl);
         }
         end2DRendering();
      }
      GLSupport.checkAndPrintGLError(gl);

      if (!isSelecting()) {
         if (myDragBox != null) {
            drawDragBox (drawable);
         }
      } else {
         // revert clear color
         gl.glClearColor (bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
      }
      GLSupport.checkAndPrintGLError(gl);

      gl3.glFlush();
      GLSupport.checkAndPrintGLError(gl);

   }

   private void offscreenCapture (int flags) {

      boolean savedSelecting = selectEnabled;
      selectEnabled = false;

      // Initialize the OpenGL context FOR THE FBO
      gl.setSwapInterval (1);

      // Set rendering commands to go to offscreen frame buffer
      frameCapture.activateFBO(gl);

      resetViewVolume = false;   //disable resetting view volume
      doDisplay (drawable, flags);

      fireRerenderListeners();

      // further drawing will go to screen
      frameCapture.deactivateFBO(gl);
      frameCapture.capture(gl);

      selectEnabled = savedSelecting;
   }

   private void enableTransparency (GL3 gl) {
      if (!alphaFaceCulling) {
         pushViewerState();
         setDepthEnabled(false);
         setFaceStyle(FaceStyle.FRONT_AND_BACK);
      }

      // XXX maybe set configurable?
      gl.glEnable (GL3.GL_BLEND);
      gl.glBlendFunc (GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
   }

   private void disableTransparency (GL3 gl) {
      if (!alphaFaceCulling) {
         popViewerState();
      }
      gl.glDisable (GL3.GL_BLEND);
   }

   public void setTransparencyEnabled (boolean enable) {

      // do not enable if in selection mode
      if (!(isSelecting() && enable)) {
         if (enable != myTransparencyEnabledP) {
            if (enable) {
               enableTransparency (gl);
            }
            else {
               disableTransparency (gl);
            }
            myTransparencyEnabledP = enable;
         }
      }
   }        

   // Screen-shot

   @Override
   public void setupScreenShot(
      int w, int h, int samples, File file, String format) {
      if (frameCapture == null) {
         frameCapture = new GLFrameCapture ( w, h, samples, file, format);
      }
      else {
         synchronized(frameCapture) {
            frameCapture.reconfigure(w, h, samples, isGammaCorrectionEnabled(), file, format);
         }
      }
      grab = true;
   }

   @Override
   public void setupScreenShot(int w, int h, File file, String format) {
      setupScreenShot(w, h, -1, file, format);
   }

   public void awaitScreenShotCompletion() {
      if (frameCapture != null) {
         grabWaitComplete = true;  // signal to wait after next grab
         repaint();                // execute in render thread
         // frameCapture.waitForCompletion();
      }
   }

   public boolean grabPending() {
      return grab;
   }

   public synchronized void cleanupScreenShots () {
      grabClose = true;
      repaint();  // execute in render thread
   }

   // XXX Things that should be handled in GLViewer

   //   @Override
   //   public void begin2DRendering(double left, double right, double bottom, double top) {
   //
   //      // save depth, lighting, face culling information
   //      pushViewerState();
   //      setLightingEnabled (false);
   //      setDepthEnabled(false);
   //
   //      pushModelMatrix();
   //      pushViewMatrix();
   //      pushProjectionMatrix();
   //
   //      setModelMatrix(RigidTransform3d.IDENTITY);
   //      setViewMatrix(RigidTransform3d.IDENTITY);
   //      setOrthogonal2d(left, right, bottom, top);
   //
   //      rendering2d = true;
   //   }
   //
   //   @Override
   //   public void finish2DRendering() {
   //
   //      popProjectionMatrix();
   //      popViewMatrix();
   //      popModelMatrix();
   //      popViewerState();
   //
   //      setLightingEnabled (true);
   //      setDepthEnabled(true);
   //
   //      rendering2d = false;
   //   }

   //   @Override
   //   public boolean is2DRendering() {
   //      return rendering2d;
   //   }

   //   //==========================================================================
   //   //  Colours and Materials
   //   //==========================================================================
   //
   //   @Override
   //   public void setColor(float[] frontRgba, float[] backRgba) {
   //      if (!selectEnabled) {
   //         if (frontRgba != null) {
   //            if (frontRgba.length == 3) {
   //               frontRgba = new float[]{frontRgba[0],frontRgba[1],frontRgba[2], 1.0f};
   //            }
   //         } else {
   //            frontRgba = myFrontColor;
   //         }
   //         if (backRgba != null) {
   //            if (backRgba.length == 3) {
   //               backRgba = new float[]{backRgba[0],backRgba[1],backRgba[2], 1.0f};
   //            }
   //         } else {
   //            backRgba = myBackColor;
   //         }
   //         progManager.setMaterialDiffuse(gl, frontRgba, backRgba);
   //         myFrontColor = frontRgba;
   //         myBackColor = backRgba;
   //      }
   //   }

   public void saveShading() {
      if (selectEnabled) {
         return;
      }
      pushViewerState();
   }

   //   @Override
   //   public void restoreShading(RenderProps props) {
   //      // nothing
   //   }

   //==========================================================================
   //  Matrices
   //==========================================================================

   protected void updateMatrices(GL3 gl) {
      myProgManager.setMatrices(gl, projectionMatrix, viewMatrix, modelMatrix, modelNormalMatrix);
      modelMatrixValidP = true;
      projectionMatrixValidP = true;
      viewMatrixValidP = true;
   }

   public void maybeUpdateMaterials() {
      maybeUpdateMaterials(gl);
   }

   protected void maybeUpdateState(GL3 gl) {
      maybeUpdateMatrices (gl);
      maybeUpdateMaterials (gl);
   }

   protected void maybeUpdateMaterials(GL3 gl) {
      if (myCurrentMaterialModified) {
         if (isSelecting ()) {
            myProgManager.setMaterials (gl, myCurrentMaterial, mySelectingColor, myCurrentMaterial, mySelectingColor);
         } else {
            // set all colors
            if (mySelectedColorActive) {
               mySelectedColor[3] = myCurrentMaterial.getAlpha();
               myProgManager.setMaterials (gl, myCurrentMaterial, mySelectedColor, myCurrentMaterial, mySelectedColor);
            } else {
               myProgManager.setMaterials (gl, myCurrentMaterial, myCurrentMaterial.getDiffuse(), myCurrentMaterial, myBackColor);
            }
         }
         myCurrentMaterialModified = false; // reset flag since state is now updated
      }
   }

   protected void maybeUpdateMatrices(GL3 gl) {
      if (!modelMatrixValidP || !projectionMatrixValidP || !viewMatrixValidP) {
         updateMatrices(gl);
      }
   }   

   //==========================================================================
   //  Drawing
   //==========================================================================

   @Override
   public void drawSphere(float[] pnt, double rad) {

      if (rad < Double.MIN_NORMAL) {
         return;
      }

      // scale and translate model matrix
      pushModelMatrix();
      translateModelMatrix(pnt[0], pnt[1], pnt[2]);
      scaleModelMatrix(rad);

      maybeUpdateState (gl);

      int nslices = getSurfaceResolution();
      GL3Object sphere = myPrimitiveManager.getSphere(gl, nslices, (int)Math.ceil(nslices/2));
      GLShaderProgram prog = getProgram (gl);
      prog.use (gl);
      sphere.draw(gl);

      // revert matrix transform
      popModelMatrix();
   }

   private void addVertex(float[] v, float nx, float ny, float nz, ByteBuffer buff, 
      PositionBufferPutter posPutter,
      NormalBufferPutter nrmPutter) {
      posPutter.putPosition(buff,  v);
      nrmPutter.putNormal(buff, nx, ny, nz);
   }

   /**
    * Fills an interleaved vertex-normal buffer
    * @param v0 input vertex v0
    * @param v1 input vertex v1
    * @param v2 input vertex v2
    * @param buff output buffer
    */
   private void addTri(float[] v0, float[] v1, float[] v2, ByteBuffer buff,
      PositionBufferPutter posPutter, NormalBufferPutter nrmPutter) {

      float ax = v1[0]-v0[0];
      float ay = v1[1]-v0[1];
      float az = v1[2]-v0[2];
      float bx = v2[0]-v0[0];
      float by = v2[1]-v0[1];
      float bz = v2[2]-v0[2];
      float nx = ay*bz-az*by;
      float ny = az*bx-ax*bz;
      float nz = ax*by-ay*bx;

      addVertex(v0, nx, ny, nz, buff, posPutter, nrmPutter);
      addVertex(v1, nx, ny, nz, buff, posPutter, nrmPutter);
      addVertex(v2, nx, ny, nz, buff, posPutter, nrmPutter);

   }

   /**
    * Fills an interleaved vertex-normal buffer with two triangles, split as:
    * (v0, v1, v2), (v2, v3, v0)
    * @param v0 input vertex v0
    * @param v1 input vertex v1
    * @param v2 input vertex v2
    * @param v3 input vertex v3
    * @param buff output buffer
    */
   private void addQuad(float[] v0, float[] v1, float[] v2, float[] v3, 
      ByteBuffer buff, PositionBufferPutter posPutter, NormalBufferPutter nrmPutter ) {

      float ax, ay, az;
      float bx, by, bz;
      float nx, ny, nz;

      ax = v1[0]-v0[0];
      ay = v1[1]-v0[1];
      az = v1[2]-v0[2];
      bx = v2[0]-v0[0];
      by = v2[1]-v0[1];
      bz = v2[2]-v0[2];
      nx = ay*bz-az*by;
      ny = az*bx-ax*bz;
      nz = ax*by-ay*bx;
      ax = v3[0]-v0[0];
      ay = v3[1]-v0[1];
      az = v3[2]-v0[2];
      nx += by*az-bz*ay;
      ny += bz*ax-bx*az;
      nz += bx*ay-by*ax;

      addVertex(v0, nx, ny, nz, buff, posPutter, nrmPutter);
      addVertex(v1, nx, ny, nz, buff, posPutter, nrmPutter);
      addVertex(v2, nx, ny, nz, buff, posPutter, nrmPutter);
      addVertex(v2, nx, ny, nz, buff, posPutter, nrmPutter);
      addVertex(v3, nx, ny, nz, buff, posPutter, nrmPutter);
      addVertex(v0, nx, ny, nz, buff, posPutter, nrmPutter);

   }

   private RigidTransform3d getLineTransform(float[] p0, float[] p1) {
      RigidTransform3d X = new RigidTransform3d();

      Vector3d utmp = new Vector3d();
      utmp.set (p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]);
      X.p.set (p0[0], p0[1], p0[2]);
      X.R.setZDirection (utmp);

      return X;
   }

   @Override
   public void drawSpindle(
      float[] pnt0, float[] pnt1, double rad) {

      if (rad < Double.MIN_NORMAL) {
         return;
      }

      pushModelMatrix();

      double dx = pnt1[0]-pnt0[0];
      double dy = pnt1[1]-pnt0[1];
      double dz = pnt1[2]-pnt0[2];
      double len = Math.sqrt(dx*dx+dy*dy+dz*dz);

      // compute required rotation
      RigidTransform3d lineRot = getLineTransform(pnt0, pnt1);
      // scale and translate model matrix
      mulModelMatrix(lineRot);
      scaleModelMatrix(rad, rad, len);

      maybeUpdateState(gl);

      int nslices = getSurfaceResolution();
      GL3Object spindle = myPrimitiveManager.getSpindle(gl, nslices, (int)Math.ceil(nslices/2));
      GLShaderProgram prog = getProgram(gl);
      prog.use (gl);
      spindle.draw(gl);

      // revert matrix transform
      popModelMatrix();

   }

   @Override
   public void drawCylinder(
      float[] coords0, float[] coords1, double r, boolean capped) {

      if (r < Double.MIN_NORMAL) {
         return;
      }
      
      pushModelMatrix();
      // compute required rotation
      RigidTransform3d lineRot = getLineTransform(coords0, coords1);
      // scale and translate model matrix
      mulModelMatrix(lineRot);
      double dx = coords1[0]-coords0[0];
      double dy = coords1[1]-coords0[1];
      double dz = coords1[2]-coords0[2];
      double len = Math.sqrt(dx*dx+dy*dy+dz*dz);
      scaleModelMatrix(r,r,len);

      maybeUpdateState(gl);
      GLShaderProgram prog = getProgram(gl);
      prog.use (gl);
      
      int nslices = getSurfaceResolution();
      GL3Object cylinder = myPrimitiveManager.getCylinder(gl, nslices, capped);
      
      cylinder.draw(gl);

      // revert matrix transform
      popModelMatrix();

   }
   
   private void drawCone(float[] pnt0, float[] pnt1, double r, boolean capped) {
      // compute required rotation
      RigidTransform3d lineRot = getLineTransform(pnt0, pnt1);
      double dx = pnt1[0]-pnt0[0];
      double dy = pnt1[1]-pnt0[1];
      double dz = pnt1[2]-pnt0[2];
      float h = (float)(Math.sqrt(dx*dx+dy*dy+dz*dz));
      
      int nSlices = getSurfaceResolution ();
      GL3Object cone = myPrimitiveManager.getCone (gl, nSlices, capped);
      
      pushModelMatrix ();
      mulModelMatrix(lineRot);
      scaleModelMatrix (r, r, h);
      maybeUpdateState (gl);
      GLShaderProgram prog = getProgram(gl);
      prog.use (gl);
      cone.draw (gl);
      popModelMatrix ();
   }
   
   @Override
   public void drawCone(float[] pnt0, float[] pnt1, double r0, double r1, boolean capped) {
      
      if (r0 < Double.MIN_NORMAL && r1 < Double.MIN_NORMAL) {
         return;
      }
      
      if (r0 == 0) {
         drawCone(pnt1, pnt0, r1, capped);
      } else if (r1 == 0) {
         drawCone(pnt0, pnt1, r0, capped);
      } else {
      
         // compute required rotation
         RigidTransform3d lineRot = getLineTransform(pnt0, pnt1);
         double dx = pnt1[0]-pnt0[0];
         double dy = pnt1[1]-pnt0[1];
         double dz = pnt1[2]-pnt0[2];
         float h = (float)(Math.sqrt(dx*dx+dy*dy+dz*dz));
   
         int nSlices = getSurfaceResolution ();
         float nz = (float)((r0-r1)/h);
         float nscale = (float)(1.0/Math.sqrt(1+nz*nz));
   
         int nverts, nelems;
         if (capped) {
            // need to duplicate vertices for caps (different normal)
            nverts = 4*nSlices;
            nelems = 12*nSlices;
         } else {
            nverts = 2*nSlices;
            nelems = 6*nSlices;
         }
         
         gloFlex.begin (gl, true, false, false, nverts, nelems);
         
         if (capped) { 
            int bidx = 4*nSlices-2;
            int tidx = bidx+1;
            for (int i=0; i<nSlices; ++i) {
               double angle = 2*Math.PI/nSlices*i;
               float x = (float)Math.cos(angle);
               float y = (float)Math.sin(angle);
               gloFlex.normal (nscale*x, nscale*y, nscale*nz);
               float bx = (float)(r0*x);
               float by = (float)(r0*y);
               float tx = (float)(r1*x);
               float ty = (float)(r1*y);
               gloFlex.vertex (bx, by, 0);  // bottom side
               gloFlex.vertex (tx, ty, h);  // top side
               gloFlex.normal (0f, 0f, -1f);
               gloFlex.vertex (bx, by, 0);  // bottom
               gloFlex.normal (0f, 0f, 1f);
               gloFlex.vertex (tx, ty, h);  // top
               int j = (i<<2);
               int k = (((i+1)%nSlices)<<2);
               gloFlex.index (j, k, j+1, k, k+1, j+1,      // sides
                        bidx, k+2, j+2, j+3, k+3, tidx); // bottom/top
            }
            
         } else {
            for (int i=0; i<nSlices; ++i) {
               double angle = 2*Math.PI/nSlices*i;
               float x = (float)Math.cos(angle);
               float y = (float)Math.sin(angle);
               gloFlex.normal (nscale*x, nscale*y, nscale*nz);
               gloFlex.vertex ((float)(r0*x), (float)(r0*y), 0);  // bottom
               gloFlex.vertex ((float)(r1*x), (float)(r1*y), h);  // top
               int j = (i << 1);
               int k = (((i+1)%nSlices)<<1);
               gloFlex.index (j, k, j+1, k, k+1, j+1);  // side triangles
            }
         }      
         gloFlex.end (gl);
         
         pushModelMatrix ();
         mulModelMatrix(lineRot);
         maybeUpdateState (gl);
         GLShaderProgram prog = getProgram(gl);
         prog.use (gl);
         gloFlex.drawElements (gl, GL.GL_TRIANGLES);
         popModelMatrix ();
      }
   }

   protected GLShaderProgram getBasicPointProgram(GL3 gl) {
      
      if (isSelecting ()) {
         return myProgManager.getSelectionProgram (gl, RenderMode.POINTS);
      }
      
      GLSLInfoBuilder builder = new GLSLInfoBuilder();
      builder.setNumLights (myProgManager.numLights ());
      builder.setNumClipPlanes (myProgManager.numClipPlanes ());
      builder.setRoundPoints (true);
      
      if (!isSelecting()) {
         Shading shading = Shading.NONE;
         builder.setLighting ( shading );
         if (isTextureMappingEnabled()) {
            if (myCurrentColorMapProps != null && myCurrentColorMapProps.isEnabled()) {
               builder.enableColorMap (true);
               builder.setVertexTextures (true);
               builder.setTextureColorMixing (myCurrentColorMapProps.getTextureColorMixing ());
               builder.mixTextureColorDiffuse (myCurrentColorMapProps.getDiffuseColoring ());
               builder.mixTextureColorSpecular (myCurrentColorMapProps.getSpecularColoring ());
               builder.mixTextureColorEmission (myCurrentColorMapProps.getDiffuseColoring ());
            }  
         }
      }

      return myProgManager.getProgram(gl, builder.build ());
   }
   
   protected GLShaderProgram getBasicLineProgram(GL3 gl, boolean hasNormals, boolean hasColors, boolean hasTextures) {
      
      if (isSelecting ()) {
         return myProgManager.getSelectionProgram (gl, RenderMode.LINES);
      }
      
      GLSLInfoBuilder builder = new GLSLInfoBuilder();
      builder.setNumLights (myProgManager.numLights ());
      builder.setNumClipPlanes (myProgManager.numClipPlanes ());
      
      if (!isSelecting()) {
         Shading shading = Shading.NONE;
         builder.setLighting ( shading );
         builder.setVertexNormals (hasNormals);
         if (hasColors) {
            builder.setVertexColors (hasColors);
            builder.setVertexColorMixing (getVertexColorMixing());
         }
         
         if (hasTextures && isTextureMappingEnabled()) {
            if (myCurrentColorMapProps != null && myCurrentColorMapProps.isEnabled()) {
               builder.enableColorMap (true);
               builder.setVertexTextures (true);
               builder.setTextureColorMixing (myCurrentColorMapProps.getTextureColorMixing ());
               builder.mixTextureColorDiffuse (myCurrentColorMapProps.getDiffuseColoring ());
               builder.mixTextureColorSpecular (myCurrentColorMapProps.getSpecularColoring ());
               builder.mixTextureColorEmission (myCurrentColorMapProps.getDiffuseColoring ());
            }  
         }
      }

      return myProgManager.getProgram(gl, builder.build ());
   }

   protected GLShaderProgram getBasicProgram(GL3 gl) {
      
      if (isSelecting ()) {
         return myProgManager.getSelectionProgram (gl, RenderMode.TRIANGLES);
      }
      
      GLSLInfoBuilder builder = new GLSLInfoBuilder();
      builder.setNumLights (myProgManager.numLights ());
      builder.setNumClipPlanes (myProgManager.numClipPlanes ());

      if (!isSelecting()) {
         Shading shading = Shading.NONE;
         builder.setLighting ( shading );
         if (shading != Shading.NONE) {
            builder.setVertexNormals(true);
         }
         if (isTextureMappingEnabled()) {
            if (myCurrentColorMapProps != null && myCurrentColorMapProps.isEnabled()) {
               builder.enableColorMap (true);
               builder.setVertexTextures (true);
               builder.setTextureColorMixing (myCurrentColorMapProps.getTextureColorMixing ());
               builder.mixTextureColorDiffuse (myCurrentColorMapProps.getDiffuseColoring ());
               builder.mixTextureColorSpecular (myCurrentColorMapProps.getSpecularColoring ());
               builder.mixTextureColorEmission (myCurrentColorMapProps.getDiffuseColoring ());
            }  
         }
      }

      return myProgManager.getProgram(gl, builder.build ());
   }

   protected GLShaderProgram getProgram(GL3 gl) {
      
      if (isSelecting ()) {
         return myProgManager.getSelectionProgram (gl, RenderMode.TRIANGLES);
      }

      GLSLInfoBuilder builder = new GLSLInfoBuilder();
      builder.setNumLights (myProgManager.numLights ());
      builder.setNumClipPlanes (myProgManager.numClipPlanes ());

      if (!isSelecting()) {
         Shading shading = isLightingEnabled() ? getShading() : Shading.NONE;
         builder.setLighting ( shading );
         if (shading != Shading.NONE) {
            builder.setVertexNormals(true);
         }
         if (isTextureMappingEnabled()) {
            if (myCurrentColorMapProps != null && myCurrentColorMapProps.isEnabled()) {
               builder.enableColorMap (true);
               builder.setVertexTextures (true);
               builder.setTextureColorMixing (myCurrentColorMapProps.getTextureColorMixing ());
               builder.mixTextureColorDiffuse (myCurrentColorMapProps.getDiffuseColoring ());
               builder.mixTextureColorSpecular (myCurrentColorMapProps.getSpecularColoring ());
               builder.mixTextureColorEmission (myCurrentColorMapProps.getDiffuseColoring ());
            }
            if (myCurrentNormalMapProps != null && myCurrentNormalMapProps.isEnabled()) {
               builder.enableNormalMap (true);
               builder.setVertexTextures (true);
            }
            if (myCurrentBumpMapProps != null && myCurrentBumpMapProps.isEnabled()) {
               builder.enableBumpMap (true);
               builder.setVertexTextures (true);
            }  
         }
      }

      return myProgManager.getProgram(gl, builder.build ());

   }

   protected GLShaderProgram getColorProgram(GL3 gl, boolean hasNormals) {
      
      if (isSelecting ()) {
         return myProgManager.getSelectionProgram (gl, RenderMode.TRIANGLES);
      }
      
      Shading shading = getShading ();
      ColorInterpolation cinterp = ColorInterpolation.RGB;
      if (isHSVColorInterpolationEnabled ()) {
         cinterp = ColorInterpolation.HSV;
      }
      return getColorProgram (gl, shading, hasNormals, cinterp);
   }

   protected GLShaderProgram getColorProgram(GL3 gl, Shading shading, boolean hasNormals, ColorInterpolation cinterp) {
      
      if (isSelecting ()) {
         return myProgManager.getSelectionProgram (gl, RenderMode.TRIANGLES);
      }
      
      GLSLInfoBuilder builder = new GLSLInfoBuilder();
      builder.setNumLights (myProgManager.numLights ());
      builder.setNumClipPlanes (myProgManager.numClipPlanes ());

      if (!isSelecting()) {
         if (!isLightingEnabled ()) {
            shading = Shading.NONE;
         }
         builder.setLighting ( shading );
         if (shading != Shading.NONE) {
            builder.setVertexNormals(true);
         }

         if (isTextureMappingEnabled()) {
            if (myCurrentColorMapProps != null && myCurrentColorMapProps.isEnabled()) {
               builder.enableColorMap (true);
               builder.setVertexTextures (true);
               builder.setTextureColorMixing (myCurrentColorMapProps.getTextureColorMixing ());
               builder.mixTextureColorDiffuse (myCurrentColorMapProps.getDiffuseColoring ());
               builder.mixTextureColorSpecular (myCurrentColorMapProps.getSpecularColoring ());
               builder.mixTextureColorEmission (myCurrentColorMapProps.getDiffuseColoring ());
            }
            if (myCurrentNormalMapProps != null && myCurrentNormalMapProps.isEnabled()) {
               builder.enableNormalMap (true);
               builder.setVertexTextures (true);
            }
            if (myCurrentBumpMapProps != null && myCurrentBumpMapProps.isEnabled()) {
               builder.enableBumpMap (true);
               builder.setVertexTextures (true);
            }  
         }

         if (isVertexColoringEnabled()) {
            builder.setVertexColorMixing (getVertexColorMixing());
            builder.setColorInterpolation (cinterp);
            builder.setVertexColors (true);
            // XXX property to set this
            builder.mixVertexColorDiffuse (true);
            builder.mixVertexColorSpecular (true);
            builder.mixVertexColorEmission (true);
         }
      }

      return myProgManager.getProgram(gl, builder.build ());
   }

   private void drawGLLine(GL3 gl, float[] coords0, float[] coords1) {

      gloFlex.begin (gl, 2);
      gloFlex.vertex (coords0);
      gloFlex.vertex (coords1);
      gloFlex.end (gl);

      maybeUpdateState(gl);
      GLShaderProgram prog = getBasicProgram(gl);
      prog.use (gl);
      gloFlex.drawVertices (gl, GL.GL_LINES);

      GLSupport.checkAndPrintGLError(gl);
   }

   private void drawGLPoint(GL3 gl, float[] coords) {

      gloFlex.begin (gl, 1);
      gloFlex.vertex (coords);
      gloFlex.end (gl);

      maybeUpdateState(gl);

      getBasicPointProgram(gl).use (gl);
      gloFlex.drawVertices(gl, GL.GL_POINTS);

      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void drawLine(
      RenderProps props, float[] pnt0, float[] pnt1, float[] color,
      boolean capped, boolean selected) {

      boolean savedHighlighting = getSelectionHighlighting();
      Shading savedShading = setLineShading (props);
      if (color == null) {
         color = props.getLineColorF ();
      }
      setPropsColoring (props, color, selected);
      switch (props.getLineStyle()) {
         case LINE: {
            //boolean savedLighting = isLightingEnabled();
            //setLightingEnabled (false);
            gl.glLineWidth (props.getLineWidth());

            //            if (color.length == 3 && props.getAlpha () < 1) {
            //               color = new float[]{color[0], color[1], color[2], (float)props.getAlpha ()};
            //            }
            //            setColor (color, selected);

            drawGLLine(gl, pnt0, pnt1);

            gl.glLineWidth (1);
            //setLightingEnabled (savedLighting);
            break;
         }
         case CYLINDER: {
            //Shading savedShading = getShadeModel();
            //setShadeModel (props.getShading());
            //setPropsMaterial (props, color, selected);
            drawCylinder (pnt0, pnt1, props.getLineRadius(), capped);
            //setShadeModel(savedShading);
            break;
         }
         case SOLID_ARROW: {
            //Shading savedShading = getShadeModel();
            //setShadeModel (props.getShading());
            //setPropsMaterial (props, color, selected);
            drawSolidArrow (pnt0, pnt1, props.getLineRadius(), capped);
            //setShadeModel(savedShading);
            break;
         }
         case SPINDLE: {
            //Shading savedShading = getShadeModel();
            //setShadeModel (props.getShading());
            //setPropsMaterial (props, color, selected);
            drawSpindle (pnt0, pnt1, props.getLineRadius());
            //setShadeModel(savedShading);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented line style " + props.getLineStyle());
         }
      }
      setShading(savedShading);
      setSelectionHighlighting (savedHighlighting);
   }

   @Override
   public void drawSolidArrow(
      float[] pnt0, float[] pnt1, double rad, boolean capped) {

      if (rad < Double.MIN_NORMAL) {
         return;
      }

      int nslices = getSurfaceResolution();

      double dx = pnt1[0]-pnt0[0];
      double dy = pnt1[1]-pnt0[1];
      double dz = pnt1[2]-pnt0[2];

      double len = Math.sqrt(dx*dx+dy*dy+dz*dz);

      double arrowRad = 3*rad;
      double arrowLen = Math.min(2*arrowRad,len/2);
      double lenFrac = 1-arrowLen/len;
      float[] coordsMid = new float[]{pnt0[0] + (float)(lenFrac*dx),
                                      pnt0[1] + (float)(lenFrac*dy), 
                                      pnt0[2] + (float)(lenFrac*dz)};

      pushModelMatrix();

      // compute required rotation
      RigidTransform3d lineRot = getLineTransform(pnt0, pnt1);

      // scale and translate model matrix
      mulModelMatrix(lineRot);
      scaleModelMatrix(rad, rad, len-arrowLen);
      maybeUpdateState(gl);
      GLShaderProgram prog = getProgram(gl);
      prog.use (gl);

      GL3Object cylinder = myPrimitiveManager.getCylinder(gl, nslices, capped);
      cylinder.draw(gl);

      popModelMatrix();
      pushModelMatrix();

      lineRot.setTranslation(coordsMid[0], coordsMid[1], coordsMid[2]);
      mulModelMatrix(lineRot);
      scaleModelMatrix(arrowRad, arrowRad, arrowLen);
      maybeUpdateState(gl);

      GL3Object cone = myPrimitiveManager.getCone(gl, nslices, capped);
      cone.draw(gl);

      // revert matrix transform
      popModelMatrix();

   }

   @Override
   public void drawArrow(
      RenderProps props, float[] pnt0, float[] pnt1, boolean capped,
      boolean selected) {

      boolean savedHighlighting = getSelectionHighlighting();
      Shading savedShading = setLineShading (props);
      setLineColoring (props, selected);

      Vector3d utmp = 
      new Vector3d(pnt1[0]-pnt0[0],
         pnt1[1]-pnt0[1], 
         pnt1[2]-pnt0[2]);
      double len = utmp.norm();
      utmp.scale(1.0/len);

      Vector3d vtmp = new Vector3d(pnt0[0], pnt0[1], pnt0[2]);
      double arrowRad = 3 * props.getLineRadius();
      double arrowLen = 2*arrowRad;
      vtmp.scaledAdd (len-arrowLen, utmp);
      float[] ctmp = new float[3];
      ctmp[0] = (float)vtmp.x;
      ctmp[1] = (float)vtmp.y;
      ctmp[2] = (float)vtmp.z;


      if (len > arrowLen) {
         switch (props.getLineStyle()) {
            case LINE: {
               gl.glLineWidth (props.getLineWidth());
               drawGLLine(gl, pnt0, ctmp);
               gl.glLineWidth (1);
               break;
            }
            case CYLINDER: 
            case SOLID_ARROW: {
               drawCylinder (pnt0, ctmp, props.getLineRadius(), capped);
               break;
            }
            case SPINDLE: {
               drawSpindle (pnt0, pnt1, props.getLineRadius());
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented line style " + props.getLineStyle());
            }
         }
      }
      if (props.getLineStyle() == LineStyle.LINE) {
         // reset shading from NONE to props value
         setShading (props.getShading());
      }
      if (len <= arrowLen) {
         drawCone (pnt0, pnt1, len/2, 0, capped);
      }
      else {
         drawCone (ctmp, pnt1, arrowRad, 0, capped);
      }
      setShading(savedShading);
      setSelectionHighlighting (savedHighlighting);      
   }

   @Override
   public void drawPoint(RenderProps props, float[] pnt, boolean selected) {

      boolean savedHighlighting = getSelectionHighlighting();
      Shading savedShading = setPointShading (props);
      setPointColoring (props, selected);
      switch (props.getPointStyle()) {
         case POINT: {
            int size = props.getPointSize();
            if (size > 0) {
               //boolean savedLighting = isLightingEnabled();
               //setLightingEnabled (false);
               gl.glPointSize (size);
               //setColor (props.getPointColorArray(), selected);

               drawGLPoint(gl, pnt);

               gl.glPointSize (1);
               //setLightingEnabled (savedLighting);
            }
            break;
         }
         case SPHERE: {
            //Shading savedShading = getShadeModel();
            //setPointLighting (props, selected);
            drawSphere (pnt, props.getPointRadius());
            //setShadeModel(savedShading);
            break;
         }
      }
      setShading(savedShading);
      setSelectionHighlighting (savedHighlighting);
   }

   public void drawAxes(GL3 gl, double len) {

      GLSupport.checkAndPrintGLError(gl);
      if (len == 0) {
         return;
      }

      GLSupport.checkAndPrintGLError(gl);

      pushModelMatrix();

      scaleModelMatrix(len);
      maybeUpdateState(gl);

      GL3Object axes = myPrimitiveManager.getAxes(gl, true, true, true);
      if (selectEnabled) {
         GLShaderProgram prog = getBasicProgram(gl);
         prog.use (gl);
         axes.draw(gl);
      } else {
         GLShaderProgram prog =  getColorProgram(gl, Shading.NONE, false, ColorInterpolation.RGB);
         prog.use (gl);
         axes.draw(gl);
      }
      // gloManager.releaseObject(axes);

      // signal to revert matrix transform
      popModelMatrix();
   }

   @Override
   public void drawAxes(
      RigidTransform3d X, double[] lens, int width, boolean selected) {

      GLSupport.checkAndPrintGLError(gl);

      boolean savedHighlighting = setSelectionHighlighting(selected);
      // deal with transform and len
      double lx = lens[0];
      double ly = lens[1];
      double lz = lens[2];
      boolean drawx = true;
      boolean drawy = true;
      boolean drawz = true;

      if (X == null) {
         X = RigidTransform3d.IDENTITY;
      }
      if (lx == 0) {
         lx = 1;
         drawx = false;
      }
      if (ly == 0) {
         ly = 1;
         drawy = false;
      }
      if (lz == 0) {
         lz = 1;
         drawz = false;
      }

      pushModelMatrix();

      mulModelMatrix(X);
      scaleModelMatrix(lx, ly, lz);
      maybeUpdateState(gl);


      gl.glLineWidth (width);

      GL3Object axes = myPrimitiveManager.getAxes(gl, drawx, drawy, drawz);
      if (selectEnabled || selected) {
         GLShaderProgram prog = getBasicProgram(gl);
         prog.use (gl);
         axes.draw(gl);
      } else {
         GLShaderProgram prog = getColorProgram(gl, Shading.NONE, false, ColorInterpolation.RGB);
         prog.use (gl);
         axes.draw(gl);
      }
      // gloManager.releaseObject(axes);

      gl.glLineWidth(1);

      // revert matrix transform
      popModelMatrix();

      setSelectionHighlighting(savedHighlighting);

   }

   @Override
   protected void drawDragBox(GLAutoDrawable drawable) {
      GLSupport.checkAndPrintGLError(drawable.getGL ());

      begin2DRendering(-1, 1,-1, 1);

      float x0 = (float)(2 * myDragBox.x / (double)width - 1);
      float x1 = (float)(x0 + 2 * myDragBox.width / (double)width);
      float y0 = (float)(1 - 2 * myDragBox.y / (double)height);
      float y1 = (float)(y0 - 2 * myDragBox.height / (double)height);

      // System.out.println(x0 + " " + y0 + " " + x1 + " " + y1);
      gloFlex.begin (gl, 4);
      gloFlex.vertex (x0, y0, 0);
      gloFlex.vertex (x1, y0, 0);
      gloFlex.vertex (x1, y1, 0);
      gloFlex.vertex (x0, y1, 0);
      gloFlex.end (gl);

      maybeUpdateState(gl);
      gl.glLineWidth (1);
      setColor(0.5f, 0.5f, 0.5f, 1.0f);

      getBasicProgram(gl).use (gl);
      gloFlex.drawVertices(gl, GL.GL_LINE_LOOP);

      end2DRendering();

      GLSupport.checkAndPrintGLError(drawable.getGL ());
   }

   public void drawLineStrip (
      RenderProps props, Iterable<float[]> pnts, 
      LineStyle style, boolean selected) {

      boolean savedHighlighting = getSelectionHighlighting();
      Shading savedShading = getShading();
      setShading (style==LineStyle.LINE ? Shading.NONE : props.getShading());
      setLineColoring (props, selected);
      switch (style) {
         case LINE: {
            //setLightingEnabled (false);
            // draw regular points first
            gl.glLineWidth (props.getLineWidth());
            //setColor (props.getLineColorArray(), isSelected);
            float[] v0 = null;
            for (float[] v1 : pnts) {
               if (v0 != null) {
                  drawGLLine(gl, v0, v1);
               }
               else {
                  v0 = new float[3];
               }
               // don't recall why we don't simply set v0 = v1
               v0[0] = v1[0];
               v0[1] = v1[1];
               v0[2] = v1[2];
            }
            gl.glLineWidth (1);
            //setLightingEnabled (true);
            break;
         }
         case SPINDLE:
         case SOLID_ARROW:
         case CYLINDER: {
            //            Shading savedShading = getShadeModel();
            //            setLineLighting (props, isSelected);
            double rad = props.getLineRadius();
            float[] v0 = null;
            for (float[] v1 : pnts) {
               if (v0 != null) {
                  if (style == LineStyle.SPINDLE) {
                     drawSpindle (v0, v1, props.getLineRadius());
                  }
                  else if (style == LineStyle.SOLID_ARROW) {
                     drawSolidArrow (v0, v1, rad, /*capped=*/true);
                  }
                  else {
                     drawCylinder (v0, v1, rad, /*capped=*/false);
                  }
               }
               else {
                  v0 = new float[3];
               }
               // don't recall why we don't simply set v0 = v1
               v0[0] = v1[0];
               v0[1] = v1[1];
               v0[2] = v1[2];
            }
            //            setShadeModel(savedShading);            
            //            restoreShading (props);
         }
      }
      setShading(savedShading); 
      setSelectionHighlighting (savedHighlighting);
   }

   //=============================================================================
   // PRIMITIVES
   //=============================================================================

   private<T> int findSize(Iterable<T> it) {

      int size = 0;
      // determine size
      if (it instanceof Collection) {
         size = ((Collection<?>)it).size ();
      } else {
         Iterator<T> pit = it.iterator ();
         while (pit.hasNext ()) {
            pit.next ();
            ++size;
         }
      }

      return size;
   }

   private void drawPrimitives(GL3 gl, Iterable<float[]> coords, int size, int glPrimitiveType) {

      if (size <= 0) {
         size = findSize (coords);
      }

      gloFlex.begin (gl, size);
      for (float[] pos : coords) {
         gloFlex.vertex (pos);
      }
      gloFlex.end (gl);

      maybeUpdateState(gl);
      getProgram(gl).use (gl);
      gloFlex.drawVertices(gl, glPrimitiveType);

   }

   private void drawPrimitives(GL3 gl, Iterable<float[]> coords, Iterable<float[]> normals, int size, int glPrimitiveType) {

      if (size <= 0) {
         size = findSize(coords);
      }

      gloFlex.begin (gl, true, false, false, size);
      Iterator<float[]> nit = normals.iterator ();
      for (float[] p : coords) {
         float[] n = nit.next ();
         gloFlex.normal (n);
         gloFlex.vertex (p);
      }
      gloFlex.end (gl);

      maybeUpdateState(gl);
      getProgram(gl).use (gl);
      gloFlex.drawVertices (gl, glPrimitiveType);

   }

   @Override
   public void drawPoint (float[] pnt) {
      drawGLPoint (gl, pnt);
   }

   //   public void drawPoint(float[] pnt, float[] nrm) {
   //      List<float[]> pnts = Arrays.asList (pnt);
   //      List<float[]> nrms = Arrays.asList (nrm);
   //      drawPrimitives (pnts, nrms,  2, GL.GL_POINTS);
   //   }

   //   @Override
   //   public void drawPoints (Iterable<float[]> points) {
   //      drawPrimitives (points, -1, GL2.GL_POINTS);
   //   }

   //   @Override
   //   public void drawPoints (Iterable<float[]> points, Iterable<float[]> normals) {
   //      drawPrimitives (points, normals, -1, GL2.GL_POINTS);
   //   }

   @Override
   public void drawLine(float[] pnt0, float[] pnt1) {
      drawGLLine (gl, pnt0, pnt1);
   }

   //   public void drawLine (
   //      float[] pnt0, float[] nrm0, float[] pnt1, float[] nrm1) {
   //      List<float[]> pnts = Arrays.asList (pnt0, pnt1);
   //      List<float[]> nrms = Arrays.asList (nrm0, nrm1);
   //      drawPrimitives (pnts, nrms,  2, GL.GL_LINES);
   //   }

   //   public void drawLines(Iterable<float[]> coords) {
   //      drawPrimitives (coords, -1, GL2.GL_LINES);
   //   }
   //
   //   public void drawLines(Iterable<float[]> coords, Iterable<float[]> normals) {
   //      drawPrimitives (coords, normals, -1, GL2.GL_LINES);
   //   }
   //
   //   public void drawLineStrip(Iterable<float[]> coords, Iterable<float[]> normals) {
   //      drawPrimitives (coords,  normals, -1, GL2.GL_LINE_STRIP);
   //   }

   /**
    * Draw triangular faces, using the current Shading, lighting and
    * material, and computing a single "face" normal from the coordinates
    * (so the current "shading" really matters only if it is
    * Shading.NONE).
    */
   public void drawTriangle (float[] pnt0, float[] pnt1, float[] pnt2) {
      List<float[]> coords = Arrays.asList (pnt0, pnt1, pnt2);
      drawTriangles (coords);
   }

   //   public void drawTriangle (float[] p0, float[] n0, float[] p1, float[] n1, float[] p2, float[] n2) {
   //      List<float[]> coords = Arrays.asList (p0, p1, p2);
   //      List<float[]> normals = Arrays.asList (n0, n1, n2);
   //      drawPrimitives (coords, normals,  3, GL.GL_TRIANGLES);
   //   }

   static File[] DEBUG_NORMAL_SHADERS = new File[] {
       ArtisynthPath.getSrcRelativeFile (GL3Viewer.class, "shaders/camera_normal_vertex.glsl"),
       ArtisynthPath.getSrcRelativeFile (GL3Viewer.class, "shaders/camera_normal_fragment.glsl")
   };
   
   static File[] DEBUG_INSTANCE_SHADERS = new File[] {
      ArtisynthPath.getSrcRelativeFile (GL3Viewer.class, "shaders/instance_debug_vertex.glsl"),
      ArtisynthPath.getSrcRelativeFile (GL3Viewer.class, "shaders/instance_debug_fragment.glsl")
   };
   
   public void drawTriangles (Iterable<float[]> points) {

      maybeUpdateState(gl);

      // determine required buffer size
      int size = findSize(points);

      float[] normal = new float[3];
      Iterator<float[]> pit = points.iterator ();

      gloFlex.begin (gl, true, false, false, size);
      while (pit.hasNext ()) {
         float[] p0 = pit.next ();
         float[] p1 = pit.next ();
         float[] p2 = pit.next ();
         computeNormal (p0, p1, p2, normal);
         
         gloFlex.normal (normal);
         gloFlex.vertex (p0);
         gloFlex.vertex (p1);
         gloFlex.vertex (p2);
      }
      gloFlex.end (gl);

      maybeUpdateState(gl);
      getProgram(gl).use (gl);

      //      File[] shaders = DEBUG_NORMAL_SHADERS;
      //      GLShaderProgram prog = myProgManager.getProgram (gl, shaders, shaders);
      //      prog.use (gl);
      
      // XXX debug program
      
      gloFlex.drawVertices (gl, GL.GL_TRIANGLES);

   }

   //   /**
   //    * Assumed per-vertex normal
   //    */
   //   public void drawTriangles (Iterable<float[]> points, Iterable<float[]> normals) {
   //      drawPrimitives (points, normals, -1, GL2.GL_TRIANGLES);
   //   }

   //======================================================================
   // RENDER OBJECT STUFF
   //======================================================================

   //   private GL3SharedRenderObjectIndexed getOrCreateGRO(RenderObject robj) {
   //      GL3Resource res;
   //      GL3SharedRenderObjectIndexed gro;
   //      RenderObjectIdentifier key = robj.getIdentifier();
   //
   //      synchronized(myGLResources) {
   //         res = myGLResources.getResource(key);
   //         if (res == null) {
   //            gro = new GL3SharedRenderObjectIndexed(robj);
   //            gro.init(gl, robj);
   //            myGLResources.addResource(gl, key, gro);
   //
   //         } else {
   //            gro = (GL3SharedRenderObjectIndexed)res;
   //            gro.maybeUpdate(gl, robj);
   //         }
   //      }
   //      return gro;
   //   }

   protected GLShaderProgram getProgram(GL3 gl, RenderObjectState robj) {

      if (isSelecting ()) {
         return myProgManager.getSelectionProgram (gl, RenderMode.TRIANGLES);
      }

      GLSLInfoBuilder builder = new GLSLInfoBuilder();
      builder.setNumLights (myProgManager.numLights ());
      builder.setNumClipPlanes (myProgManager.numClipPlanes ());

      Shading shading = isLightingEnabled() ? getShading() : Shading.NONE;
      builder.setLighting ( shading );
      if (shading != Shading.NONE) {
         builder.setVertexNormals(true);
      }
      if (isTextureMappingEnabled()) {
         if (myCurrentColorMapProps != null && myCurrentColorMapProps.isEnabled() && !mySelectedColorActive) {
            builder.enableColorMap (true);
            builder.setVertexTextures (true);
            builder.setTextureColorMixing (myCurrentColorMapProps.getTextureColorMixing ());
            builder.mixTextureColorDiffuse (myCurrentColorMapProps.getDiffuseColoring ());
            builder.mixTextureColorSpecular (myCurrentColorMapProps.getSpecularColoring ());
            builder.mixTextureColorEmission (myCurrentColorMapProps.getDiffuseColoring ());
         }

         if (myCurrentNormalMapProps != null && myCurrentNormalMapProps.isEnabled()) {
            builder.enableNormalMap (true);
            builder.setVertexTextures (true);
         }
         if (myCurrentBumpMapProps != null && myCurrentBumpMapProps.isEnabled()) {
            builder.enableBumpMap (true);
            builder.setVertexTextures (true);
         }  
      }

      if (!robj.hasColors() || !isVertexColoringEnabled() || mySelectedColorActive) {
         builder.setColorInterpolation (ColorInterpolation.NONE);
         builder.setVertexColors (false);
      } else { 
         if (isHSVColorInterpolationEnabled()) {
            builder.setColorInterpolation (ColorInterpolation.HSV);   
         } else {
            builder.setColorInterpolation (ColorInterpolation.RGB);
         }
         builder.setVertexColorMixing (getVertexColorMixing());
         builder.setVertexColors (true);
      }

      return myProgManager.getProgram(gl, builder.build ());

   }

   protected GLShaderProgram getPointsProgram(GL3 gl, PointStyle style, RenderObjectState robj) {

      if (isSelecting ()) {
         if (style == PointStyle.POINT) {
            return myProgManager.getSelectionProgram (gl, RenderMode.POINTS);
         } else {
            return myProgManager.getSelectionProgram (gl, RenderMode.INSTANCED_POINTS);
         }
      }
      
      GLSLInfoBuilder builder = new GLSLInfoBuilder();
      builder.setNumLights (myProgManager.numLights ());
      builder.setNumClipPlanes (myProgManager.numClipPlanes ());
      
      switch(style) {
         case SPHERE:
         case CUBE:
            builder.setInstancedRendering (InstancedRendering.POINTS);
            builder.setVertexNormals (true);  // instance has normals
            break;
         case POINT:
         default:
            builder.setInstancedRendering (InstancedRendering.NONE);
            builder.setRoundPoints (true);
            if (robj.hasNormals ()) {
               builder.setVertexNormals(true);
            } else {
               builder.setVertexNormals (false);
            }
            break;
         
      }

      if (!isSelecting ()) {
         Shading shading = isLightingEnabled() ? getShading() : Shading.NONE;
         if (style == PointStyle.POINT) {
            shading = Shading.NONE;
         }
         
         builder.setLighting ( shading );

         if (isTextureMappingEnabled()) {
            if (myCurrentColorMapProps != null && myCurrentColorMapProps.isEnabled() && !mySelectedColorActive) {
               builder.enableColorMap (true);
               builder.setTextureColorMixing (myCurrentColorMapProps.getTextureColorMixing ());
               builder.setVertexTextures (true);
               builder.mixTextureColorDiffuse (myCurrentColorMapProps.getDiffuseColoring ());
               builder.mixTextureColorSpecular (myCurrentColorMapProps.getSpecularColoring ());
               builder.mixTextureColorEmission (myCurrentColorMapProps.getDiffuseColoring ());
            }
            if (myCurrentNormalMapProps != null && myCurrentNormalMapProps.isEnabled()) {
               builder.enableNormalMap (true);
               builder.setVertexTextures (true);
            }
            if (myCurrentBumpMapProps != null && myCurrentBumpMapProps.isEnabled()) {
               builder.enableBumpMap (true);
               builder.setVertexTextures (true);
            }  

         }

         if (!robj.hasColors() || !isVertexColoringEnabled() || mySelectedColorActive) {
            builder.setColorInterpolation (ColorInterpolation.NONE);
            builder.setVertexColors (false);
         } else { 
            if (isHSVColorInterpolationEnabled()) {
               builder.setColorInterpolation (ColorInterpolation.HSV);   
            } else {
               builder.setColorInterpolation (ColorInterpolation.RGB);
            }
            builder.setVertexColorMixing (getVertexColorMixing());
            builder.setVertexColors (true);
         }
      }
      return myProgManager.getProgram(gl, builder.build ());
   }

   protected GLShaderProgram getLinesProgram(GL3 gl, RenderObjectState robj, LineStyle style) {
      
      if (isSelecting ()) {
         if (style == LineStyle.LINE) {
            return myProgManager.getSelectionProgram (gl, RenderMode.LINES);
         } else {
            return myProgManager.getSelectionProgram (gl, RenderMode.INSTANCED_LINES);
         }
      }
      
      GLSLInfoBuilder builder = new GLSLInfoBuilder();
      builder.setNumLights (myProgManager.numLights ());
      builder.setNumClipPlanes (myProgManager.numClipPlanes ());
      builder.setInstancedRendering (InstancedRendering.LINES);
      if (style == LineStyle.SOLID_ARROW) {
         builder.setLineScaleOffset (true);
      }

      if (!isSelecting ()) {
         Shading shading = isLightingEnabled() ? getShading() : Shading.NONE;
         builder.setLighting ( shading );
         if (shading != Shading.NONE) {
            builder.setVertexNormals(true);
         }

         if (isTextureMappingEnabled()) {
            if (myCurrentColorMapProps != null && myCurrentColorMapProps.isEnabled() && !mySelectedColorActive) {
               builder.enableColorMap (true);
               builder.setTextureColorMixing (myCurrentColorMapProps.getTextureColorMixing ());
               builder.setVertexTextures (true);
               builder.mixTextureColorDiffuse (myCurrentColorMapProps.getDiffuseColoring ());
               builder.mixTextureColorSpecular (myCurrentColorMapProps.getSpecularColoring ());
               builder.mixTextureColorEmission (myCurrentColorMapProps.getDiffuseColoring ());
            }
            if (myCurrentNormalMapProps != null && myCurrentNormalMapProps.isEnabled()) {
               builder.enableNormalMap (true);
               builder.setVertexTextures (true);
            }
            if (myCurrentBumpMapProps != null && myCurrentBumpMapProps.isEnabled()) {
               builder.enableBumpMap (true);
               builder.setVertexTextures (true);
            }  

         }

         if (!robj.hasColors() || !isVertexColoringEnabled() || mySelectedColorActive) {
            builder.setColorInterpolation (ColorInterpolation.NONE);
            builder.setVertexColors (false);
         } else { 
            if (isHSVColorInterpolationEnabled()) {
               builder.setColorInterpolation (ColorInterpolation.HSV);   
            } else {
               builder.setColorInterpolation (ColorInterpolation.RGB);
            }
            builder.setVertexColorMixing (getVertexColorMixing());
            builder.setVertexColors (true);
         }
      }
      return myProgManager.getProgram(gl, builder.build ());
   }

   @Override
   public void drawTriangles(RenderObject robj) {
      drawTriangles(robj, robj.getTriangleGroupIdx ());
   }

   @Override
   public void drawTriangles(RenderObject robj, int gidx) {
      GLSupport.checkAndPrintGLError(gl);

      GL3RenderObjectIndexed gro = myRenderObjectManager.getIndexed (gl, robj);

      maybeUpdateState(gl);

      // maybe use texture?
      GLTexture colortex = null;
      GLTexture normtex = null;
      GLTexture bumptex = null;

      if (robj.hasTextureCoords ()) {
         if (myCurrentColorMapProps != null && myCurrentColorMapProps.isEnabled ()) {
            colortex = myGLResources.getOrLoadTexture (gl, myCurrentColorMapProps.getContent ());   
         }
         if (myCurrentNormalMapProps != null && myCurrentNormalMapProps.isEnabled ()) {
            normtex = myGLResources.getOrLoadTexture (gl, myCurrentNormalMapProps.getContent ());
         }
         if (myCurrentBumpMapProps != null && robj.hasTextureCoords ()) {
            bumptex = myGLResources.getOrLoadTexture (gl, myCurrentBumpMapProps.getContent ());
         }
      }
      GLSupport.checkAndPrintGLError(gl);

      GLShaderProgram prog = getProgram(gl, robj.getStateInfo ());

      prog.use (gl);
      GLSupport.checkAndPrintGLError(gl);

      if (colortex != null) {
         myProgManager.bindTexture (gl, "color_map", colortex);
      }
      if (normtex != null) {
         myProgManager.bindTexture (gl, "normal_map", normtex);
         myProgManager.setUniform (gl, prog, "normal_scale", myCurrentNormalMapProps.getNormalScale());
      }
      if (bumptex != null) {
         myProgManager.bindTexture (gl, "bump_map", bumptex);
         myProgManager.setUniform (gl, prog, "bump_scale", myCurrentBumpMapProps.getBumpScale());
      }

      gro.drawTriangleGroup (gl, GL.GL_TRIANGLES, robj.getTriangleGroupIdx ());

      GLSupport.checkAndPrintGLError(gl);

      if (bumptex != null) {
         myProgManager.unbindTexture (gl, "bump_map", bumptex);
      }
      if (normtex != null) {
         myProgManager.unbindTexture (gl, "normal_map", normtex);
      }
      if (colortex != null) {
         myProgManager.unbindTexture (gl, "color_map", colortex);
      }

      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void drawLines(RenderObject robj) {
      drawLines(robj, robj.getLineGroupIdx ());
   }

   @Override
   public void drawLines(RenderObject robj, int gidx) {
      GLSupport.checkAndPrintGLError(gl);

      GL3RenderObjectIndexed gro = myRenderObjectManager.getIndexed (gl, robj);
      maybeUpdateState(gl);
      getBasicLineProgram(gl, robj.hasNormals (), robj.hasColors (), robj.hasTextureCoords ()).use(gl);

      gro.drawLineGroup (gl, GL.GL_LINES, gidx);
      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void drawPoints(RenderObject robj) {
      drawPoints(robj, robj.getPointGroupIdx ());
   }

   @Override
   public void drawPoints(RenderObject robj, int gidx) {
      GLSupport.checkAndPrintGLError(gl);
      GL3RenderObjectIndexed gro = myRenderObjectManager.getIndexed (gl, robj);
      maybeUpdateState(gl);
      getPointsProgram(gl, PointStyle.POINT, robj.getStateInfo ()).use(gl);
      gro.drawPointGroup (gl, GL.GL_POINTS, gidx);
      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void drawVertices(RenderObject robj, DrawMode mode) {
      GLSupport.checkAndPrintGLError(gl);
      GL3RenderObjectIndexed gro = myRenderObjectManager.getIndexed (gl, robj);
      maybeUpdateState(gl);
      getProgram(gl, robj.getStateInfo ()).use(gl);
      gro.drawVertices (gl, getDrawPrimitive (mode));
   }


   @Override
   public void drawLines(RenderObject robj,LineStyle style, double rad) {
      drawLines(robj, robj.getLineGroupIdx (), style, rad);
   }

   @Override
   public void drawLines(RenderObject robj, int gidx, LineStyle style, double rad) {

      GL3RenderObjectLines gro = myRenderObjectManager.getLines (gl, robj);

      maybeUpdateState(gl);

      switch (style) {
         case LINE: {
            // maybe change point size and draw points
            float fold = getLineWidth();
            float frad = (float)rad;
            boolean changed = false;
            if (fold != frad) {
               setLineWidth(frad);
               changed = true;
            }

            getBasicLineProgram(gl, robj.hasNormals (), robj.hasColors (), robj.hasTextureCoords ()).use (gl);
            gro.drawLineGroup (gl, GL.GL_LINES, gidx);

            if (changed) {
               setLineWidth(fold);
            }
            break;
         }
         default: {

            getLinesProgram(gl, robj.getStateInfo (), style).use (gl);

            switch (style) {
               case CYLINDER: {
                  GL3Object primitive = myPrimitiveManager.getCylinder (gl, mySurfaceResolution, true);
                  gro.setRadius (gl, (float)rad);
                  gro.drawInstancedLineGroup (gl, primitive, gidx);
                  break;
               }
               case SOLID_ARROW: {
                  gro.setRadius (gl, (float)rad);
                  GL3Object cylinder = myPrimitiveManager.getCylinder (gl, mySurfaceResolution, true);
                  GL3Object cone = myPrimitiveManager.getCone (gl, mySurfaceResolution, true);

                  float arrowRad = 3*(float)rad;
                  float arrowLen = 2*arrowRad;

                  float[] coneBoundary = {1, 0, -arrowLen, 1};
                  gro.setRadiusOffsets (gl, (float)rad, null, coneBoundary);
                  gro.drawInstancedLineGroup (gl, cylinder, gidx);
                  gro.setRadiusOffsets (gl, arrowRad, coneBoundary, null);
                  gro.drawInstancedLineGroup (gl, cone, gidx);
                  break;
               }
               case SPINDLE: {
                  gro.setRadius (gl, (float)rad);
                  GL3Object spindle = myPrimitiveManager.getSpindle (gl, mySurfaceResolution, mySurfaceResolution/2);
                  gro.drawInstancedLineGroup (gl, spindle, gidx);
                  break;
               }
               default:
                  break;

            }

            break;
         }
      }
   }

   @Override
   public void drawPoints(RenderObject robj, PointStyle style, double rad) {
      drawPoints(robj, robj.getPointGroupIdx (), style, rad);
   }

   @Override
   public void drawPoints(RenderObject robj, int gidx, PointStyle style, double rad) {

      GL3RenderObjectPoints gro = myRenderObjectManager.getPoints (gl, robj);

      maybeUpdateState(gl);

      switch (style) {
         case POINT: {
            // maybe change point size and draw points
            float fold = getPointSize();
            float frad = (float)rad;
            boolean changed = false;
            if (fold != frad) {
               setPointSize(frad);
               changed = true;
            }

            getPointsProgram(gl, style, robj.getStateInfo ()).use(gl);
            
            gro.drawPointGroup (gl, GL.GL_POINTS, gidx);

            if (changed) {
               setPointSize(fold);
            }
            break;
         }
         case CUBE:
         case SPHERE: {

            GL3Object sphere = myPrimitiveManager.getSphere (gl, mySurfaceResolution, mySurfaceResolution/2);

            gro.setRadius(gl, (float)rad);
            // getProgram(gl, robj.getStateInfo ()).use (gl);
            GLShaderProgram prog = getPointsProgram(gl, style, robj.getStateInfo ());
            
            //GLShaderProgram prog = myProgManager.getProgram (gl, DEBUG_INSTANCE_SHADERS, DEBUG_INSTANCE_SHADERS);
            prog.use (gl);
            
            gro.drawInstancedPointGroup (gl, sphere, gidx);

            break;
         }
      }
   }

   /*==============================================================================
    *  IMMEDIATE MODE
   =============================================================================*/

   @Override
   protected void doDraw (
      DrawMode drawMode, int numVertices, float[] vertexData,
      boolean hasNormalData, float[] normalData, boolean hasColorData,
      float[] colorData, boolean hasTexData, float[] texData) {

      if (numVertices > 0) {

         int glmode = getDrawPrimitive(drawMode);

         gloFlex.begin (gl, hasNormalData, hasColorData, hasTexData, numVertices);

         int cidx = 0;
         int vidx = 0;
         int tidx = 0;
         for (int i=0; i<numVertices; ++i) {
            if (hasColorData) {
               gloFlex.color (colorData, cidx);
            }
            if (hasNormalData) {
               gloFlex.normal (normalData, vidx);
            }
            if (hasTexData) {
               gloFlex.texcoord (texData, tidx);
            }
            gloFlex.vertex (vertexData, vidx);
            cidx += 4;
            vidx += 3;
            tidx += 2;
         }
         gloFlex.end (gl);


         GLShaderProgram prog = null;
         if (hasColorData) {
            prog = getColorProgram(gl, hasNormalData);
         } else {
            prog = getProgram(gl);
         }

         maybeUpdateState(gl);
         prog.use (gl);
         gloFlex.drawVertices (gl, glmode);

      }

   }

   public boolean hasVertexColorMixing (ColorMixing cmix) {
      return true;
   }

   public boolean hasTextureMixing (ColorMixing tmix) {
      return true;
   }
}
