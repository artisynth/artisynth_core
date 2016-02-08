package maspack.render.GL.GL3;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.Dragger3d;
import maspack.render.Material;
import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectIdentifier;
import maspack.render.RenderObject.RenderObjectState;
import maspack.render.RenderProps;
import maspack.render.RenderProps.Faces;
import maspack.render.RenderProps.LineStyle;
import maspack.render.RenderProps.PointStyle;
import maspack.render.RenderProps.Shading;
import maspack.render.RenderableLine;
import maspack.render.RenderablePoint;
import maspack.render.GL.GLClipPlane;
import maspack.render.GL.GLFrameCapture;
import maspack.render.GL.GLGridPlane;
import maspack.render.GL.GLLight;
import maspack.render.GL.GLLightManager;
import maspack.render.GL.GLMouseAdapter;
import maspack.render.GL.GLSupport;
import maspack.render.GL.GLViewer;
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
   GL3ProgramManager progManager = null;
   GL3Resources myGLResources = null;    // holds shared context and cache

   // Common viewer-specific objects (essentially stream-drawn)
   GL3Object dragBoxGLO = null;
   GL3Object lineGLO = null;
   GL3Object pointGLO = null;
   GL3Object tetGLO = null;
   GL3Object pyrGLO = null;
   GL3Object wedgeGLO = null;
   GL3Object hexGLO = null;

   // state
   boolean rendering2d = false;

   // color history
   protected float[] myFrontColor;
   protected float[] myBackColor;
   protected Material myFrontMaterial = null;
   protected Material myBackMaterial = null;

   // screenshot
   private GLFrameCapture frameCapture = null;
   private boolean grab = false;
   private boolean grabWaitComplete = false; // wait
   private boolean grabClose = false;        // clean up

   // data for "drawMode"
   int myVertexCap;
   int myNumVertices;
   VertexDrawMode myDrawMode;
   ByteBuffer myDrawBuffer;
   protected float[] myCurrentNormal = new float[3];

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
    * @param sharedContext
    * a GL context with which to share resources (e.g., display
    * lists and textures). Can be specified as null.
    * @param width
    * initial width of the viewer
    * @param height
    * initial height of the viewer
    */
   public GL3Viewer (GLCapabilities cap, GL3Resources resources, int width,
      int height) {
      if (cap == null) {
         GLProfile glp3 = GLProfile.get(GLProfile.GL3);
         cap = new GLCapabilities(glp3);
         cap.setSampleBuffers (true);
         cap.setNumSamples (8);
      }

      if (resources == null) {
         resources = new GL3Resources(cap);
      }
      myGLResources = resources;
      canvas = myGLResources.createCanvas();
      myGLResources.registerViewer (this);

      lightManager = new GLLightManager();      
      progManager = new GL3ProgramManager();

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
      createMaterials();

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

      //      myUp.set(0,1,0);
      //      XEyeToWorld = new RigidTransform3d (0, 0, 1, 1, 0, 0, 0);
      //      updateViewMatrix();

      // setCenter(new Point3d(0,0,0));
      // setEye(new Point3d(0,-3, 0));
      setAxialView (AxisAlignedRotation.X_Z);

      // XEyeToWorld = new RigidTransform3d (0, -5, 1, 1, 0, 0, Math.PI / 2);
      // setAxialView(myAxialView);


      setModelMatrix(RigidTransform3d.IDENTITY);
      // setOrthogonal(5, -10, 10);
      // setPerspective(70, 0.01, 10);

      //      System.out.println(projectionMatrix);
      //      System.out.println(viewMatrix);
      //      System.out.println(modelMatrix);



   }

   public void setDefaultLights() {

      float light0_ambient[] = { 0.1f, 0.1f, 0.1f, 1f };
      float light0_diffuse[] = { 0.8f, 0.8f, 0.8f, 1.0f };
      float light0_specular[] = { 0.5f, 0.5f, 0.5f, 1.0f };
      float light0_position[] = { -0.8660254f, 0.5f, 1f, 0f };

      float light1_ambient[] = { 0.0f, 0.0f, 0.0f, 1.0f };
      float light1_diffuse[] = { 0.5f, 0.5f, 0.5f, 1.0f };
      float light1_specular[] = { 0.5f, 0.5f, 0.5f, 1.0f };
      float light1_position[] = { 0.8660254f, 0.5f, 1f, 0f };

      float light2_ambient[] = { 0.0f, 0.0f, 0.0f, 1.0f };
      float light2_diffuse[] = { 0.5f, 0.5f, 0.5f, 1.0f };
      float light2_specular[] = { 0.5f, 0.5f, 0.5f, 1.0f };
      float light2_position[] = { 0f, -10f, 1f, 0f };

      lightManager.clearLights();
      lightManager.addLight(new GLLight (
         light0_position, light0_ambient, light0_diffuse, light0_specular));
      lightManager.addLight (new GLLight (
         light1_position, light1_ambient, light1_diffuse, light1_specular));
      lightManager.addLight(new GLLight (
         light2_position, light2_ambient, light2_diffuse, light2_specular));
      lightManager.setMaxIntensity(1.0f);
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
      setFaceMode(Faces.FRONT);
      setShadeModel(Shading.PHONG);
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

      progManager.init(gl, lightManager.numLights(), 0);
      progManager.setMatrices(gl, projectionMatrix, viewMatrix, modelMatrix, modelNormalMatrix);
      progManager.setLights(gl, lightManager.getLights(), 1.0f/lightManager.getMaxIntensity(), viewMatrix);
      progManager.setMaterials(gl, myFrontMaterial, myBackMaterial);

      // trigger rebuild of renderables
      buildInternalRenderList();

      System.out.println("GL3 initialized");

      GLSupport.checkAndPrintGLError(drawable.getGL ());

   }

   private void createMaterials() {

      myFrontMaterial = Material.createDiffuse(Color.YELLOW, 0);
      myFrontMaterial.setSpecular(Color.WHITE);
      myFrontMaterial.setShininess(5000f);

      myBackMaterial = Material.createDiffuse(Color.BLUE, 0);
      myBackMaterial.setSpecular(Color.BLACK);
   }

   @Override
   public void dispose(GLAutoDrawable drawable) {
      GLSupport.checkAndPrintGLError(drawable.getGL ());

      progManager.dispose(gl);
      myGLResources.dispose(gl);

      // clear temporaries
      if (dragBoxGLO != null) {
         dragBoxGLO.dispose(gl);
         dragBoxGLO = null;
      }
      if (lineGLO != null) {
         lineGLO.dispose(gl);
         lineGLO = null;
      }
      if (pointGLO != null) {
         pointGLO.dispose(gl);
         pointGLO = null;
      }
      if (hexGLO != null) {
         hexGLO.dispose(gl);
         hexGLO = null;
      }
      if (wedgeGLO != null) {
         wedgeGLO.dispose(gl);
         wedgeGLO = null;
      }
      if (pyrGLO != null) {
         pyrGLO.dispose(gl);
         pyrGLO = null;
      }
      if (tetGLO != null) {
         tetGLO.dispose(gl);
         tetGLO = null;
      }

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

   public int[] getViewport() {
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

      //   XXX need better way to clear resources
      //      // potentially clear some cached rendering
      //      synchronized(myGLResources) {
      //         // only allow first viewer to modify cache
      //         if (version == myGLResourcesVersion) {
      //            // if update render cache, then use this round to
      //            // purge unused objects
      //            // if clear render cache, then destroy all cached
      //            if ((flags & Renderer.CLEAR_RENDER_CACHE) != 0) {
      //               myGLResources.clearCached(gl);
      //            } else if ((flags & Renderer.UPDATE_RENDER_CACHE) != 0) {
      //               myGLResources.releaseUnused(gl);
      //            }
      //         }
      //      }

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
      progManager.reconfigure(gl, lightManager.numLights(), mclips);
      progManager.setLights(gl, lightManager.getLights(), 1.0f/lightManager.getMaxIntensity(), viewMatrix);

      // update matrices
      maybeUpdateMatrices(gl);

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
         nclips = progManager.setClipPlanes(gl3, myClipPlanes);
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
         disableTransparency (gl);
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
         setFaceMode(Faces.FRONT_AND_BACK);
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

   @Override
   public void begin2DRendering(double left, double right, double bottom, double top) {

      // save depth, lighting, face culling information
      pushViewerState();
      setLightingEnabled (false);
      setDepthEnabled(false);

      pushModelMatrix();
      pushViewMatrix();
      pushProjectionMatrix();

      setModelMatrix(RigidTransform3d.IDENTITY);
      setViewMatrix(RigidTransform3d.IDENTITY);
      setOrthogonal2d(left, right, bottom, top);

      rendering2d = true;
   }

   @Override
   public void begin2DRendering(double w, double h) {
      begin2DRendering(0, w, 0, h);
   }

   @Override
   public void end2DRendering() {

      popProjectionMatrix();
      popViewMatrix();
      popModelMatrix();
      popViewerState();

      setLightingEnabled (true);
      setDepthEnabled(true);

      rendering2d = false;
   }

   @Override
   public boolean is2DRendering() {
      return rendering2d;
   }

   //==========================================================================
   //  Colours and Materials
   //==========================================================================

   @Override
   public void setColor(float[] frontRgba, float[] backRgba) {
      if (!selectEnabled) {
         if (frontRgba != null) {
            if (frontRgba.length == 3) {
               frontRgba = new float[]{frontRgba[0],frontRgba[1],frontRgba[2], 1.0f};
            }
         } else {
            frontRgba = myFrontColor;
         }
         if (backRgba != null) {
            if (backRgba.length == 3) {
               backRgba = new float[]{backRgba[0],backRgba[1],backRgba[2], 1.0f};
            }
         } else {
            backRgba = myBackColor;
         }
         progManager.setMaterialDiffuse(gl, frontRgba, backRgba);
         myFrontColor = frontRgba;
         myBackColor = backRgba;
      }
   }

   @Override
   public void forceColor(float r, float g, float b, float a) {
      float[] rgba = new float[]{r,g,b,a};
      progManager.setMaterialDiffuse(gl, rgba, rgba);
      myFrontColor = rgba;
      myBackColor = rgba;
   }

   private float[] makeColor4f(float[] c) {
      if (c.length == 3) {
         c = new float[] {c[0], c[1], c[2], 1.0f};
      }
      return c;
   }

   @Override
   public void setColor(float[] frontRgba, float[] backRgba, boolean selected) {
      if (!selectEnabled) {
         if (selected && myHighlighting == SelectionHighlighting.Color) {
            frontRgba = mySelectedColor;
            backRgba = mySelectedColor;
         }
         if (frontRgba.length == 3) {
            frontRgba = makeColor4f(frontRgba);
         }
         if (backRgba.length == 3) {
            backRgba = makeColor4f(backRgba);
         }
         progManager.setMaterialDiffuse(gl, frontRgba, backRgba);
         myFrontColor = frontRgba;
         myBackColor = backRgba;
      }
   }

   private boolean colorsMatch(float[] c0, float[] c1) {
      if (c0 == c1) {
         return true;
      }
      if ((c0 == null) || (c1 == null)){
         return false;
      }
      if (c0.length != c1.length) {
         return false;
      }

      // XXX overkill?
      for (int i=0; i<c0.length; ++i) {
         if (Math.abs(c0[i]-c1[i]) > 1e-3) {
            return false;
         }
      }
      return true;
   }

   @Override
   public void updateColor(float[] frontRgba, float[] backRgba, boolean selected) {
      if (!selectEnabled) {
         if (selected && myHighlighting == SelectionHighlighting.Color) {
            frontRgba = mySelectedColor;
            backRgba = mySelectedColor;
         }
         if (!colorsMatch(frontRgba, myFrontColor) || !colorsMatch(backRgba, myBackColor)) {
            setColor(frontRgba, backRgba);
         }
      }
   }

   private void printColor (String msg, float[] color) {
      System.out.printf ("%s %g %g %g\n", msg, color[0], color[1], color[2]);
   }

   @Override
   public void setMaterial(
      Material frontMaterial, float[] frontDiffuse,
      Material backMaterial, float[] backDiffuse, 
      boolean selected) {

      if (selected && myHighlighting == SelectionHighlighting.Color) {
         myFrontMaterial = mySelectedMaterial;
         myFrontColor = mySelectedMaterial.getDiffuse();
         myBackMaterial = mySelectedMaterial;
         myBackColor = mySelectedMaterial.getDiffuse();
         progManager.setMaterials(gl, myFrontMaterial, myBackMaterial);
      }
      else {
         if (frontDiffuse == null) {
            frontDiffuse = frontMaterial.getDiffuse();
         }
         if (backDiffuse == null) {
            backDiffuse = backMaterial.getDiffuse();
         }
         myFrontMaterial = frontMaterial;
         myFrontColor = frontDiffuse;
         myBackMaterial = backMaterial;
         myBackColor = backDiffuse;
         progManager.setMaterials(gl, frontMaterial, frontDiffuse,
            backMaterial, backDiffuse);
      }

   }

   @Override
   public void setMaterialAndShading(
      RenderProps props, Material frontMaterial, float[] frontDiffuse,
      Material backMaterial, float[] backDiffuse,
      boolean selected) {

      if (selectEnabled) {
         return;
      }

      Shading shading = props.getShading();
      if (shading == Shading.NONE) {
         setLightingEnabled (false);
         myFrontColor = null; // ensure color gets set in updateMaterial
         myBackColor = null;
         updateMaterial (props, frontMaterial, frontDiffuse, 
            backMaterial, backDiffuse, selected);
      }
      else {
         myFrontMaterial = null; // ensure material gets set in updateMaterial
         myBackMaterial = null;
         updateMaterial (props, frontMaterial, frontDiffuse, 
            backMaterial, backDiffuse, selected);
      }
      setShadeModel(shading);

   }

   public void saveShading() {
      if (selectEnabled) {
         return;
      }
      pushViewerState();
   }

   @Override
   public void restoreShading(RenderProps props) {
      // nothing
   }

   @Override
   public void updateMaterial(
      RenderProps props, Material frontMaterial, float[] frontDiffuse, 
      Material backMaterial, float[] backDiffuse, 
      boolean selected) {

      if (selectEnabled) {
         return;
      }

      if (props.getShading() == Shading.NONE) {
         // get overriding diffuse color
         float[] cf;
         float[] cb;
         if (frontDiffuse != null) {
            if (frontDiffuse.length == 3) {
               frontDiffuse = new float[]{frontDiffuse[0], frontDiffuse[1], 
                                          frontDiffuse[2], (float)props.getAlpha()};
            }
            cf = frontDiffuse;
         } else {
            cf = frontMaterial.getDiffuse();
         }
         if (backDiffuse != null) {
            if (backDiffuse.length == 3) {
               backDiffuse = new float[]{backDiffuse[0], backDiffuse[1], 
                                         backDiffuse[2], (float)props.getAlpha()};
            }
            cb = backDiffuse;
         } else {
            cb = backMaterial.getDiffuse();
         }
         updateColor(cf, cb, selected);

      } else {
         Material mf;
         float[] df;
         Material mb;
         float[] db;
         if (selected && myHighlighting == SelectionHighlighting.Color) {
            mf = mySelectedMaterial;
            mb = mySelectedMaterial;
            df = null;
            db = null;
         } else {
            mf = frontMaterial;
            mb = backMaterial;

            if (frontDiffuse != null && frontDiffuse.length == 3) {
               frontDiffuse = new float[]{frontDiffuse[0], frontDiffuse[1], 
                                          frontDiffuse[2], (float)props.getAlpha()};
            }
            df = frontDiffuse;
            if (backDiffuse != null && backDiffuse.length == 3) {
               backDiffuse = new float[]{backDiffuse[0], backDiffuse[1], 
                                         backDiffuse[2], (float)props.getAlpha()};
            }
            db = backDiffuse;
         }

         if (df == null) {
            df = mf.getDiffuse();
         }
         if (db == null) {
            db = mb.getDiffuse();
         }

         if (myFrontMaterial != mf || myBackMaterial != mb ||
         !colorsMatch(myFrontColor, df) || !colorsMatch(myBackColor, db) ) {

            progManager.setMaterials(gl, mf, df, mb, db);
            myFrontMaterial = mf;
            myFrontColor = df;
            myBackMaterial = mb;
            myBackColor = db;
         }
      }

   }

   //==========================================================================
   //  Matrices
   //==========================================================================

   protected void updateMatrices(GL3 gl) {
      progManager.setMatrices(gl, projectionMatrix, viewMatrix, modelMatrix, modelNormalMatrix);
      modelMatrixValidP = true;
      projectionMatrixValidP = true;
      viewMatrixValidP = true;
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
   public void drawSphere(RenderProps props, float[] coords, double r) {

      if (r < Double.MIN_NORMAL) {
         return;
      }

      // scale and translate model matrix
      pushModelMatrix();
      translateModelMatrix(coords[0], coords[1], coords[2]);
      scaleModelMatrix(r);

      updateMatrices(gl);

      int nslices = props.getPointSlices();
      GL3Object sphere = myGLResources.getSphere(gl, nslices, (int)Math.ceil(nslices/2));
      sphere.draw(gl, getRegularProgram(gl));
      // gloManager.releaseObject(sphere);

      // revert matrix transform
      popModelMatrix();
   }

   private void addVertex(float[] v, float nx, float ny, float nz, ByteBuffer buff, PositionBufferPutter posPutter,
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

   @Override
   public void drawTet(
      RenderProps props, double scale, float[] v0, float[] v1, float[] v2,
      float[] v3) {

      boolean scaled = false;
      if (Math.abs(1-scale) > Double.MIN_NORMAL) {
         // center, for scaling widget
         float cx = (v0[0]+v1[0]+v2[0]+v3[0])/4;
         float cy = (v0[1]+v1[1]+v2[1]+v3[1])/4;
         float cz = (v0[2]+v1[2]+v2[2]+v3[2])/4;
         float s = (float)scale;
         pushModelMatrix();
         translateModelMatrix(cx*(1-s), cy*(1-s), cz*(1-s));
         scaleModelMatrix(s);
         updateMatrices(gl); // guarantee update uniforms
         scaled = true;
      } else {
         maybeUpdateMatrices(gl);
      }

      // vertex/normal for each vertex, repeated per triangle
      final PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      final NormalBufferPutter nrmPutter = NormalBufferPutter.createDefault();
      int nverts = 12;
      int stride = posPutter.bytesPerPosition()+nrmPutter.bytesPerNormal();
      ByteBuffer buff = ByteBuffer.allocateDirect(nverts*stride);
      buff.order(ByteOrder.nativeOrder());

      addTri(v0, v2, v1, buff, posPutter, nrmPutter);
      addTri(v2, v3, v1, buff, posPutter, nrmPutter);
      addTri(v3, v0, v1, buff, posPutter, nrmPutter);
      addTri(v0, v3, v2, buff, posPutter, nrmPutter);
      buff.rewind();

      if (tetGLO == null) {
         tetGLO = GL3Object.createVN(gl, GL.GL_TRIANGLES, buff, nverts, posPutter.storage(),
            0, stride, nrmPutter.storage(), posPutter.bytesPerPosition(), stride, GL.GL_DYNAMIC_DRAW);
      } else {
         tetGLO.vbos[0].update(gl, buff);
      }

      tetGLO.draw(gl, getRegularProgram(gl));

      if (scaled) {
         popModelMatrix();
      }

   }

   @Override
   public void drawHex(
      RenderProps props, double scale, float[] v0, float[] v1, float[] v2,
      float[] v3, float[] v4, float[] v5, float[] v6, float[] v7) {

      boolean scaled = false;
      if (Math.abs(1-scale) > Double.MIN_NORMAL) {
         float cx = (v0[0]+v1[0]+v2[0]+v3[0]+v4[0]+v5[0]+v6[0]+v7[0])/8;
         float cy = (v0[1]+v1[1]+v2[1]+v3[1]+v4[1]+v5[1]+v6[1]+v7[1])/8;
         float cz = (v0[2]+v1[2]+v2[2]+v3[2]+v4[2]+v5[2]+v6[2]+v7[2])/8;

         float s = (float)scale;
         pushModelMatrix();
         translateModelMatrix(cx*(1-s), cy*(1-s), cz*(1-s));
         scaleModelMatrix(s);
         updateMatrices(gl); // guarantee update uniforms
         scaled = true;
      } else {
         maybeUpdateMatrices(gl);
      }

      // vertex/normal for each vertex, 6 verts per face (repeated on corners), 6 faces
      final PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      final NormalBufferPutter nrmPutter = NormalBufferPutter.createDefault();
      int nverts = 36;
      int stride = posPutter.bytesPerPosition()+nrmPutter.bytesPerNormal();
      ByteBuffer buff = ByteBuffer.allocateDirect(nverts*stride);
      buff.order(ByteOrder.nativeOrder());

      addQuad(v0, v1, v2, v3, buff, posPutter, nrmPutter);
      addQuad(v1, v5, v6, v2, buff, posPutter, nrmPutter);
      addQuad(v5, v4, v7, v6, buff, posPutter, nrmPutter);
      addQuad(v4, v0, v3, v7, buff, posPutter, nrmPutter);
      addQuad(v3, v2, v6, v7, buff, posPutter, nrmPutter);
      addQuad(v0, v4, v5, v1, buff, posPutter, nrmPutter);
      buff.rewind();

      if (hexGLO == null) {
         hexGLO = GL3Object.createVN(gl, GL.GL_TRIANGLES, buff, nverts, posPutter.storage(),
            0, stride, nrmPutter.storage(), posPutter.bytesPerPosition(), stride, GL.GL_DYNAMIC_DRAW);
      } else {
         hexGLO.vbos[0].update(gl, buff);
      }

      hexGLO.draw(gl, getRegularProgram(gl));

      if (scaled) {
         popModelMatrix();
      }

   }

   @Override
   public void drawWedge(
      RenderProps props, double scale, float[] v0, float[] v1, float[] v2,
      float[] v3, float[] v4, float[] v5) {

      boolean scaled = false;
      if (Math.abs(1-scale) > Double.MIN_NORMAL) {
         float cx = (v0[0]+v1[0]+v2[0]+v3[0]+v4[0]+v5[0])/8;
         float cy = (v0[1]+v1[1]+v2[1]+v3[1]+v4[1]+v5[1])/8;
         float cz = (v0[2]+v1[2]+v2[2]+v3[2]+v4[2]+v5[2])/8;

         float s = (float)scale;
         pushModelMatrix();
         translateModelMatrix(cx*(1-s), cy*(1-s), cz*(1-s));
         scaleModelMatrix(s);
         updateMatrices(gl); // guarantee update uniforms

         scaled = true;
      } else {
         maybeUpdateMatrices(gl);
      }

      // vertex/normal for each vertex, 6 verts per quad, 3 per tri
      final PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      final NormalBufferPutter nrmPutter = NormalBufferPutter.createDefault();
      int nverts = 24;
      int stride = posPutter.bytesPerPosition()+nrmPutter.bytesPerNormal();
      ByteBuffer buff = ByteBuffer.allocateDirect(nverts*stride);
      buff.order(ByteOrder.nativeOrder());

      addQuad(v0, v1, v4, v3, buff, posPutter, nrmPutter);
      addQuad(v1, v2, v5, v4, buff, posPutter, nrmPutter);
      addQuad(v2, v0, v3, v5, buff, posPutter, nrmPutter);
      addTri(v0, v2, v1, buff, posPutter, nrmPutter);
      addTri(v3, v4, v5, buff, posPutter, nrmPutter);
      buff.rewind();

      if (wedgeGLO == null) {
         wedgeGLO = GL3Object.createVN(gl, GL.GL_TRIANGLES, buff, nverts, posPutter.storage(),
            0, stride, nrmPutter.storage(), posPutter.bytesPerPosition(), stride,GL.GL_DYNAMIC_DRAW);
      } else {
         wedgeGLO.vbos[0].update(gl, buff);
      }

      wedgeGLO.draw(gl, getRegularProgram(gl));

      if (scaled) {
         popModelMatrix();
      }

   }

   @Override
   public void drawPyramid(
      RenderProps props, double scale, float[] v0, float[] v1, float[] v2,
      float[] v3, float[] v4) {

      boolean scaled = false;
      if (Math.abs(1-scale) > Double.MIN_NORMAL) {
         float cx = (v0[0]+v1[0]+v2[0]+v3[0]+v4[0])/8;
         float cy = (v0[1]+v1[1]+v2[1]+v3[1]+v4[1])/8;
         float cz = (v0[2]+v1[2]+v2[2]+v3[2]+v4[2])/8;

         float s = (float)scale;
         pushModelMatrix();
         translateModelMatrix(cx*(1-s), cy*(1-s), cz*(1-s));
         scaleModelMatrix(s);
         updateMatrices(gl); // guarantee update uniforms

         scaled = true;
      } else {
         maybeUpdateMatrices(gl);
      }

      // vertex/normal for each vertex, 6 verts per quad, 3 per tri
      final PositionBufferPutter posPutter = PositionBufferPutter.createDefault();
      final NormalBufferPutter nrmPutter = NormalBufferPutter.createDefault();
      int nverts = 18;
      int stride = posPutter.bytesPerPosition()+nrmPutter.bytesPerNormal();
      ByteBuffer buff = ByteBuffer.allocateDirect(nverts*stride);
      buff.order(ByteOrder.nativeOrder());

      addQuad(v0, v3, v2, v1, buff, posPutter, nrmPutter);
      addTri(v0, v1, v4, buff, posPutter, nrmPutter);
      addTri(v1, v2, v4, buff, posPutter, nrmPutter);
      addTri(v2, v3, v4, buff, posPutter, nrmPutter);
      addTri(v3, v0, v4, buff, posPutter, nrmPutter);
      buff.rewind();

      if (pyrGLO == null) {
         pyrGLO = GL3Object.createVN(gl, GL.GL_TRIANGLES, buff, nverts, posPutter.storage(),
            0, stride, nrmPutter.storage(), posPutter.bytesPerPosition(), stride, GL.GL_DYNAMIC_DRAW);
      } else {
         pyrGLO.vbos[0].update(gl, buff);
      }

      pyrGLO.draw(gl, getRegularProgram(gl));

      if (scaled) {
         popModelMatrix();
      }

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
   public void drawTaperedEllipsoid(
      RenderProps props, float[] coords0, float[] coords1) {

      double r = props.getLineRadius();
      if (r < Double.MIN_NORMAL) {
         return;
      }

      pushModelMatrix();

      double dx = coords1[0]-coords0[0];
      double dy = coords1[1]-coords0[1];
      double dz = coords1[2]-coords0[2];
      double len = Math.sqrt(dx*dx+dy*dy+dz*dz);

      // compute required rotation
      RigidTransform3d lineRot = getLineTransform(coords0, coords1);
      // scale and translate model matrix
      mulModelMatrix(lineRot);
      scaleModelMatrix(r, r, len);

      updateMatrices(gl);

      int nslices = props.getPointSlices();
      GL3Object ellipsoid = myGLResources.getTaperedEllipsoid(gl, nslices, (int)Math.ceil(nslices/2));
      ellipsoid.draw(gl, getRegularProgram(gl));

      // revert matrix transform
      popModelMatrix();

   }

   @Override
   public void drawCylinder(
      RenderProps props, float[] coords0, float[] coords1, double r, boolean capped) {

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

      updateMatrices(gl);

      int nslices = props.getPointSlices();
      GL3Object cylinder = myGLResources.getCylinder(gl, nslices, capped);
      cylinder.draw(gl, getRegularProgram(gl));
      // gloManager.releaseObject(sphere);

      // revert matrix transform
      popModelMatrix();

   }

   // @Override
   public void drawCone(
      RenderProps props, float[] coords0, float[] coords1, double r, boolean capped) {

      pushModelMatrix();

      // compute required rotation
      RigidTransform3d lineRot = getLineTransform(coords0, coords1);

      // scale and translate model matrix
      double dx = coords1[0]-coords0[0];
      double dy = coords1[1]-coords0[1];
      double dz = coords1[2]-coords0[2];

      double len = Math.sqrt(dx*dx+dy*dy+dz*dz);
      mulModelMatrix(lineRot);
      scaleModelMatrix(r,r,len);

      updateMatrices(gl);

      int nslices = props.getPointSlices();
      GL3Object cone = myGLResources.getCone(gl, nslices, capped);
      cone.draw(gl, getRegularProgram(gl));
      // gloManager.releaseObject(cone);

      // revert matrix transform
      popModelMatrix();
   }

   protected int getRegularProgram(GL3 gl) {
      GLSLInfo key = null;
      if (isSelecting()) {
         key = new GLSLInfo(progManager.numLights(), progManager.numClipPlanes(), 
            Shading.NONE, ColorInterpolation.NONE,
            false, false, false);
      } else {
         boolean lighting = isLightingEnabled();
         Shading shading = lighting ? getShadeModel() : Shading.NONE;
         key = new GLSLInfo(progManager.numLights(), progManager.numClipPlanes(), 
            shading, ColorInterpolation.NONE, shading != Shading.NONE, false, false);
      }
      return progManager.getProgram(gl, key);
   }

   protected int getColorProgram(GL3 gl, Shading shading, ColorInterpolation cinterp) {
      GLSLInfo key = null;
      if (isSelecting()) {
         key = new GLSLInfo(progManager.numLights(), progManager.numClipPlanes(), 
            Shading.NONE, ColorInterpolation.NONE, false, false, false);
      } else {
         boolean hasColors = isVertexColoringEnabled();
         if (!isLightingEnabled()) {
            shading = Shading.NONE;
         }
         if (!hasColors) {
            cinterp = ColorInterpolation.NONE;
         }
         key = new GLSLInfo(progManager.numLights(), progManager.numClipPlanes(), 
            shading, cinterp, shading != Shading.NONE, hasColors, false);
      }
      return progManager.getProgram(gl, key);
   }

   /**
    * No lighting, colors, textures
    * @param gl
    * @return
    */
   protected int getBasicProgram(GL3 gl) {
      return progManager.getProgram(gl, 
         new GLSLInfo(progManager.numLights(), progManager.numClipPlanes(), 
            Shading.NONE, ColorInterpolation.NONE,
            false, false, false));
   }

   private void drawGLLine(GL3 gl, float[] coords0, float[] coords1) {

      if (lineGLO == null) {
         ByteBuffer buff = ByteBuffer.allocateDirect(6*GLSupport.FLOAT_SIZE);
         buff.order(ByteOrder.nativeOrder());
         for (int i=0; i<3; ++i) {
            buff.putFloat(coords0[i]);
         }
         for (int i=0; i<3; ++i) {
            buff.putFloat(coords1[i]);
         }
         buff.rewind();
         lineGLO = GL3Object.createV(gl, GL.GL_LINES, buff, 2, GL.GL_FLOAT, 3, 3*GLSupport.FLOAT_SIZE, GL.GL_DYNAMIC_DRAW);
      } else {
         ByteBuffer buff = lineGLO.vbos[0].mapNewBuffer(gl);
         for (int i=0; i<3; ++i) {
            buff.putFloat(coords0[i]);
         }
         for (int i=0; i<3; ++i) {
            buff.putFloat(coords1[i]);
         }
         lineGLO.vbos[0].unmapBuffer(gl);
      }

      maybeUpdateMatrices(gl);
      lineGLO.draw(gl, getBasicProgram(gl));
      GLSupport.checkAndPrintGLError(gl);
   }

   private void drawGLPoint(GL3 gl, float[] coords) {

      if (pointGLO == null) {
         ByteBuffer buff = ByteBuffer.allocateDirect(3*GLSupport.FLOAT_SIZE);
         buff.order(ByteOrder.nativeOrder());
         for (int i=0; i<3; ++i) {
            buff.putFloat(coords[i]);
         }
         buff.rewind();
         pointGLO = GL3Object.createV(gl, GL.GL_POINTS, buff,  1, GL.GL_FLOAT, 3, 3*GLSupport.FLOAT_SIZE, GL.GL_DYNAMIC_DRAW);
      } else {
         ByteBuffer buff = pointGLO.vbos[0].mapNewBuffer(gl);
         for (int i=0; i<3; ++i) {
            buff.putFloat(coords[i]);
         }
         pointGLO.vbos[0].unmapBuffer(gl);
      }

      maybeUpdateMatrices(gl);
      pointGLO.draw(gl, getBasicProgram(gl));
      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void drawLine(
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      float[] color, boolean selected) {

      switch (props.getLineStyle()) {
         case LINE: {
            boolean savedLighting = isLightingEnabled();
            setLightingEnabled (false);
            gl.glLineWidth (props.getLineWidth());
            if (color == null) {
               color = props.getLineColorArray ();
            }
            if (props.getAlpha () < 1) {
               color = new float[]{color[0], color[1], color[2], (float)props.getAlpha ()};
            }
            setColor (color, selected);

            drawGLLine(gl, coords0, coords1);

            gl.glLineWidth (1);
            setLightingEnabled (savedLighting);
            break;
         }
         case CYLINDER: {
            Shading savedShading = getShadeModel();
            setMaterialAndShading (props, props.getLineMaterial(), color, selected);
            drawCylinder (props, coords0, coords1, capped);
            setShadeModel(savedShading);
            break;
         }
         case SOLID_ARROW: {
            Shading savedShading = getShadeModel();
            setMaterialAndShading (props, props.getLineMaterial(), color, selected);
            drawSolidArrow (props, coords0, coords1, capped);
            setShadeModel(savedShading);
            break;
         }
         case ELLIPSOID: {
            Shading savedShading = getShadeModel();
            setMaterialAndShading (props, props.getLineMaterial(), color, selected);
            drawTaperedEllipsoid (props, coords0, coords1);
            setShadeModel(savedShading);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented line style " + props.getLineStyle());
         }
      }
   }

   @Override
   public void drawSolidArrow(
      RenderProps props, float[] coords0, float[] coords1, boolean capped) {

      double r = props.getLineRadius();
      if (r < Double.MIN_NORMAL) {
         return;
      }

      int nslices = props.getPointSlices();

      double dx = coords1[0]-coords0[0];
      double dy = coords1[1]-coords0[1];
      double dz = coords1[2]-coords0[2];

      double len = Math.sqrt(dx*dx+dy*dy+dz*dz);

      double arrowRad = 3*props.getLineRadius();
      double arrowLen = Math.min(2*arrowRad,len/2);
      double lenFrac = 1-arrowLen/len;
      float[] coordsMid = new float[]{coords0[0] + (float)(lenFrac*dx),
                                      coords0[1] + (float)(lenFrac*dy), 
                                      coords0[2] + (float)(lenFrac*dz)};

      pushModelMatrix();

      // compute required rotation
      RigidTransform3d lineRot = getLineTransform(coords0, coords1);

      // scale and translate model matrix
      mulModelMatrix(lineRot);
      scaleModelMatrix(r, r, len-arrowLen);
      updateMatrices(gl);

      GL3Object cylinder = myGLResources.getCylinder(gl, nslices, capped);
      cylinder.draw(gl, getRegularProgram(gl));
      // gloManager.releaseObject(cylinder);

      popModelMatrix();
      pushModelMatrix();

      lineRot.setTranslation(coordsMid[0], coordsMid[1], coordsMid[2]);
      mulModelMatrix(lineRot);
      scaleModelMatrix(arrowRad, arrowRad, arrowLen);
      updateMatrices(gl);

      GL3Object cone = myGLResources.getCone(gl, nslices, capped);
      cone.draw(gl, getRegularProgram(gl));
      // gloManager.releaseObject(cone);

      gl.glUseProgram(0);

      // revert matrix transform
      popModelMatrix();

   }

   @Override
   public void drawArrow(
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      boolean selected) {

      Vector3d utmp = new Vector3d(coords1[0] - coords0[0], coords1[1] - coords0[1], coords1[2]
      - coords0[2]);
      double len = utmp.norm();
      utmp.scale(1.0/len);

      Vector3d vtmp = new Vector3d(coords1[0], coords1[1], coords1[2]);
      double arrowheadSize = 3 * props.getLineRadius();
      vtmp.scaledAdd (-len + arrowheadSize, utmp);
      float[] ctmp = new float[3];
      ctmp[0] = (float)vtmp.x;
      ctmp[1] = (float)vtmp.y;
      ctmp[2] = (float)vtmp.z;

      switch (props.getLineStyle()) {
         case LINE: {
            Shading savedShading = getShadeModel();
            setLineLighting (props, selected);
            if (len <= arrowheadSize) {
               drawCone ( props, ctmp, coords0, len, capped);
            }
            else {
               boolean savedLighting = isLightingEnabled();
               setLightingEnabled (false);
               gl.glLineWidth (props.getLineWidth());
               setColor (props.getLineColorArray(), selected);
               drawGLLine(gl, coords1, ctmp);
               gl.glLineWidth (1);
               setLightingEnabled (savedLighting);
               drawCone (
                  props, ctmp, coords0, arrowheadSize, capped);
            }
            setShadeModel(savedShading);
            break;
         }
         case CYLINDER: {
            Shading savedShading = getShadeModel();
            setLineLighting (props, selected);
            if (len <= arrowheadSize) {
               drawCone (props, coords1, coords0, len, capped);
            }
            else {
               drawCylinder (props, ctmp, coords1, capped);
               drawCone (
                  props, ctmp, coords0, arrowheadSize, capped);
            }
            setShadeModel(savedShading);
            break;
         }
         case ELLIPSOID: {
            Shading savedShading = getShadeModel();
            setLineLighting (props, selected);
            if (len <= arrowheadSize) {
               drawCone (props, coords1, coords0, len, capped);
            }
            else {
               drawTaperedEllipsoid (props, coords0, coords1);
               drawCone (
                  props, ctmp, coords0, arrowheadSize, capped);
            }
            setShadeModel(savedShading);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented line style " + props.getLineStyle());
         }
      }

   }

   @Override
   public void drawPoint(RenderProps props, float[] coords, boolean selected) {

      switch (props.getPointStyle()) {
         case POINT: {
            int size = props.getPointSize();
            if (size > 0) {
               boolean savedLighting = isLightingEnabled();
               setLightingEnabled (false);
               gl.glPointSize (size);
               setColor (props.getPointColorArray(), selected);

               drawGLPoint(gl, coords);

               gl.glPointSize (1);
               setLightingEnabled (savedLighting);
            }
            break;
         }
         case SPHERE: {
            Shading savedShading = getShadeModel();
            setPointLighting (props, selected);
            drawSphere (props, coords);
            setShadeModel(savedShading);
            break;
         }
      }

   }

   public void drawAxes(GL3 gl, double len) {

      GLSupport.checkAndPrintGLError(gl);
      if (len == 0) {
         return;
      }

      GLSupport.checkAndPrintGLError(gl);

      pushModelMatrix();

      scaleModelMatrix(len);
      updateMatrices(gl);

      GL3Object axes = myGLResources.getAxes(gl, true, true, true);
      if (selectEnabled) {
         axes.draw(gl, getBasicProgram(gl));
      } else {
         axes.draw(gl, getColorProgram(gl, Shading.NONE, ColorInterpolation.RGB));
      }
      // gloManager.releaseObject(axes);

      // signal to revert matrix transform
      popModelMatrix();
   }

   @Override
   public void drawAxes(
      RigidTransform3d X, double[] len, int lineWidth, boolean selected) {

      GLSupport.checkAndPrintGLError(gl);

      // deal with transform and len
      double lx = len[0];
      double ly = len[1];
      double lz = len[2];
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
      updateMatrices(gl);

      gl.glLineWidth (lineWidth);

      GL3Object axes = myGLResources.getAxes(gl, drawx, drawy, drawz);
      if (selectEnabled || selected) {
         axes.draw(gl, getBasicProgram(gl));
      } else {
         axes.draw(gl, getColorProgram(gl, Shading.NONE, ColorInterpolation.RGB));
      }
      // gloManager.releaseObject(axes);

      gl.glLineWidth(1);

      // revert matrix transform
      popModelMatrix();

   }

   @Override
   protected void drawDragBox(GLAutoDrawable drawable) {
      GLSupport.checkAndPrintGLError(drawable.getGL ());

      begin2DRendering(-1, 1,-1, 1);

      float x0 = (float)(2 * myDragBox.x / (double)width - 1);
      float x1 = (float)(x0 + 2 * myDragBox.width / (double)width);
      float y0 = (float)(1 - 2 * myDragBox.y / (double)height);
      float y1 = (float)(y0 - 2 * myDragBox.height / (double)height);
      float[] coords = {x0, y0, 0, x1, y0, 0, x1, y1, 0, x0, y1, 0};

      // System.out.println(x0 + " " + y0 + " " + x1 + " " + y1);

      if (dragBoxGLO == null) {
         dragBoxGLO = GL3Object.createV(gl, GL.GL_LINE_LOOP, coords, GL.GL_DYNAMIC_DRAW);
      } else {
         dragBoxGLO.vbos[0].update(gl, coords);   
      }

      maybeUpdateMatrices(gl);
      gl.glLineWidth (1);
      setColor(0.5f, 0.5f, 0.5f, 1.0f);

      dragBoxGLO.draw(gl, getBasicProgram(gl));

      end2DRendering();

      GLSupport.checkAndPrintGLError(drawable.getGL ());
   }

   // XXX Maybe cache these?
   // Challenges: separate colors, selected vs not
   //             - group them into selected vs not?
   //             selection queries need to be separate
   //             - if selecting, resort to inefficient individual rendering

   public void drawPoints(
      RenderProps props, Iterator<? extends RenderablePoint> iterator) {

      switch (props.getPointStyle()) {
         case POINT: {
            int size = props.getPointSize();
            if (size > 0) {

               // draw regular points first
               setLightingEnabled (false);
               gl.glPointSize (size);
               if (isSelecting()) {
                  // don't worry about color in selection mode
                  int i = 0;
                  while (iterator.hasNext()) {
                     RenderablePoint pnt = iterator.next();
                     if (pnt.getRenderProps() == null) {
                        if (isSelectable (pnt)) {
                           beginSelectionQuery (i);
                           drawGLPoint(gl, pnt.getRenderCoords());
                           endSelectionQuery ();
                        }
                     }
                     i++;
                  }
               }
               else {
                  setColor (props.getPointColorArray(), false);
                  while (iterator.hasNext()) {
                     RenderablePoint pnt = iterator.next();
                     if (pnt.getRenderProps() == null) {
                        updateColor (
                           props.getPointColorArray(), pnt.isSelected());
                        drawGLPoint(gl, pnt.getRenderCoords());
                     }
                  }
               }
               gl.glPointSize (1);
               setLightingEnabled (true);
            }
            break;
         }
         case SPHERE: {
            Shading savedShading = getShadeModel();
            setPointLighting (props, false);
            int i = 0;
            while (iterator.hasNext()) {
               RenderablePoint pnt = iterator.next();
               if (pnt.getRenderProps() == null) {
                  if (isSelecting()) {
                     if (isSelectable (pnt)) {
                        beginSelectionQuery (i);
                        drawSphere (props, pnt.getRenderCoords());
                        endSelectionQuery ();      
                     }
                  }
                  else {
                     updateMaterial (
                        props, props.getPointMaterial(), pnt.isSelected());
                     drawSphere (props, pnt.getRenderCoords());
                  }
               }
               i++;
            }
            setShadeModel(savedShading);
         }
      }

   }

   public void drawLineStrip (
      RenderProps props, Iterable<float[]> vertexList, 
      LineStyle style, boolean isSelected) {

      switch (style) {
         case LINE: {
            setLightingEnabled (false);
            // draw regular points first
            gl.glLineWidth (props.getLineWidth());
            setColor (props.getLineColorArray(), isSelected);
            float[] v0 = null;
            for (float[] v1 : vertexList) {
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
            setLightingEnabled (true);
            break;
         }
         case ELLIPSOID:
         case SOLID_ARROW:
         case CYLINDER: {
            Shading savedShading = getShadeModel();
            setLineLighting (props, isSelected);
            float[] v0 = null;
            for (float[] v1 : vertexList) {
               if (v0 != null) {
                  if (style == LineStyle.ELLIPSOID) {
                     drawTaperedEllipsoid (props, v0, v1);
                  }
                  else if (style == LineStyle.SOLID_ARROW) {
                     drawSolidArrow (props, v0, v1, /*capped=*/true);
                  }
                  else {
                     drawCylinder (props, v0, v1);
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
            setShadeModel(savedShading);            
            restoreShading (props);
         }
      }
   }

   @Override
   public void drawLines(
      RenderProps props, Iterator<? extends RenderableLine> iterator) {

      LineStyle lineStyle = props.getLineStyle();
      switch (lineStyle) {
         case LINE: {
            setLightingEnabled (false);
            // draw regular points first
            gl.glLineWidth (props.getLineWidth());
            if (isSelecting()) {
               // don't worry about color in selection mode
               int i = 0;
               while (iterator.hasNext()) {
                  RenderableLine line = iterator.next();
                  if (line.getRenderProps() == null) {
                     if (isSelectable (line)) {
                        beginSelectionQuery (i);
                        drawGLLine(gl, line.getRenderCoords0(), line.getRenderCoords1());
                        endSelectionQuery ();
                     }
                  }
                  i++;
               }
            }
            else {
               setColor (props.getLineColorArray(), false);
               while (iterator.hasNext()) {
                  RenderableLine line = iterator.next();
                  if (line.getRenderProps() == null) {
                     if (line.getRenderColor() == null) {
                        updateColor (props.getLineColorArray(),line.isSelected());
                     }
                     else {
                        updateColor (line.getRenderColor(),line.isSelected());
                     }
                     drawGLLine(gl, line.getRenderCoords0(), line.getRenderCoords1());
                  }
               }
            }
            gl.glLineWidth (1);
            setLightingEnabled (true);
            break;
         }
         case ELLIPSOID:
         case SOLID_ARROW:
         case CYLINDER: {
            Shading savedShading = getShadeModel();
            setLineLighting (props, /*selected=*/false);
            int i = 0;
            while (iterator.hasNext()) {
               RenderableLine line = iterator.next();
               float[] v0 = line.getRenderCoords0();
               float[] v1 = line.getRenderCoords1();
               if (line.getRenderProps() == null) {
                  if (isSelecting()) {
                     if (isSelectable (line)) {
                        beginSelectionQuery (i);
                        if (lineStyle == LineStyle.ELLIPSOID) {
                           drawTaperedEllipsoid (props, v0, v1);
                        }
                        else if (lineStyle == LineStyle.SOLID_ARROW) {
                           drawSolidArrow (props, v0, v1, /*capped=*/true);
                        }
                        else {
                           drawCylinder (props, v0, v1);
                        }
                        endSelectionQuery ();
                     }
                  }
                  else {
                     maspack.render.Material mat = props.getLineMaterial();
                     updateMaterial (
                        props, mat, line.getRenderColor(), line.isSelected());
                     if (lineStyle == LineStyle.ELLIPSOID) {
                        drawTaperedEllipsoid (props, v0, v1);
                     }
                     else if (lineStyle == LineStyle.SOLID_ARROW) {
                        drawSolidArrow (props, v0, v1, /*capped=*/true);
                     }
                     else {
                        drawCylinder (props, v0, v1);
                     }
                  }
               }
               i++;
            }
            setShadeModel(savedShading);
            break;
         }
      }
   }

   @Override
   public void drawLines(float[] vertices, int flags) {

      maybeUpdateMatrices(gl);
      // create streaming object
      GL3Object glo = GL3Object.createV(gl, GL.GL_LINES, vertices, GL3.GL_STREAM_DRAW);
      // draw
      glo.draw(gl, getRegularProgram(gl));
      glo.dispose(gl);  // dispose

      GLSupport.checkAndPrintGLError(gl);

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

   private void drawPrimitives(Iterable<float[]> coords, int size, int glPrimitiveType) {

      if (size <= 0) {
         size = findSize (coords);
      }

      ByteBuffer buff = ByteBuffer.allocateDirect(3*size*GLSupport.FLOAT_SIZE);
      buff.order(ByteOrder.nativeOrder());

      for (float[] p : coords) {
         buff.putFloat (p[0]);
         buff.putFloat (p[1]);
         buff.putFloat (p[2]);
      }
      buff.rewind();
      GL3Object glo = GL3Object.createV(gl, glPrimitiveType, 
         buff, size, GL.GL_FLOAT, 3, 3*GLSupport.FLOAT_SIZE, GL3.GL_STREAM_DRAW);

      maybeUpdateMatrices(gl);
      glo.draw(gl, getBasicProgram(gl));

      glo.dispose (gl);  // immediately dispose
   }

   private void drawPrimitives(Iterable<float[]> coords, Iterable<float[]> normals, int size, int glPrimitiveType) {

      if (size <= 0) {
         size = findSize(coords);
      }

      ByteBuffer buff = ByteBuffer.allocateDirect(6*size*GLSupport.FLOAT_SIZE);
      buff.order(ByteOrder.nativeOrder());

      Iterator<float[]> nit = normals.iterator ();
      for (float[] p : coords) {
         float[] n = nit.next ();
         buff.putFloat (p[0]);
         buff.putFloat (p[1]);
         buff.putFloat (p[2]);
         buff.putFloat (n[0]);
         buff.putFloat (n[1]);
         buff.putFloat (n[2]);
      }

      buff.rewind();
      GL3Object glo = GL3Object.createVN(gl, glPrimitiveType, 
         buff, size, BufferStorage.FLOAT_3, 0, 6*GLSupport.FLOAT_SIZE, BufferStorage.FLOAT_3, 
         3*GLSupport.FLOAT_SIZE, 6*GLSupport.FLOAT_SIZE, GL3.GL_STREAM_DRAW);

      maybeUpdateMatrices(gl);
      glo.draw(gl, getRegularProgram(gl));

      glo.dispose (gl);  // immediately dispose

   }

   @Override
   public void drawPoint (float[] coords) {
      drawGLPoint (gl, coords);
   }

   public void drawPoint(float[] coords, float[] normal) {
      List<float[]> c = Arrays.asList (coords);
      List<float[]> n = Arrays.asList (normal);
      drawPrimitives (c, n,  2, GL.GL_POINTS);
      

   }

   @Override
   public void drawPoints (Iterable<float[]> points) {
      drawPrimitives (points, -1, GL2.GL_POINTS);
   }

   @Override
   public void drawPoints (Iterable<float[]> points, Iterable<float[]> normals) {
      drawPrimitives (points, normals, -1, GL2.GL_POINTS);
   }

   @Override
   public void drawLine(float[] coords0, float[] coords1) {
      drawGLLine (gl, coords0, coords1);
   }

   public void drawLine(float[] coords0, float[] normal0, float[] coords1, float[] normal1) {
      List<float[]> coords = Arrays.asList (coords0, coords1);
      List<float[]> normals = Arrays.asList (normal0, normal1);
      drawPrimitives (coords, normals,  2, GL.GL_LINES);
   }

   public void drawLines(Iterable<float[]> coords) {
      drawPrimitives (coords, -1, GL2.GL_LINES);
   }

   public void drawLines(Iterable<float[]> coords, Iterable<float[]> normals) {
      drawPrimitives (coords, normals, -1, GL2.GL_LINES);
   }

   public void drawLineStrip(Iterable<float[]> coords, Iterable<float[]> normals) {
      drawPrimitives (coords,  normals, -1, GL2.GL_LINE_STRIP);
   }

   /**
    * Draw triangular faces, using the current Shading, lighting and
    * material, and computing a single "face" normal from the coordinates
    * (so the current "shading" really matters only if it is
    * Shading.NONE).
    */
   public void drawTriangle (float[] p0, float[] p1, float[] p2) {
      List<float[]> coords = Arrays.asList (p0, p1, p2);
      drawTriangles (coords);
   }

   public void drawTriangle (float[] p0, float[] n0, float[] p1, float[] n1, float[] p2, float[] n2) {
      List<float[]> coords = Arrays.asList (p0, p1, p2);
      List<float[]> normals = Arrays.asList (n0, n1, n2);
      drawPrimitives (coords, normals,  3, GL.GL_TRIANGLES);
   }

   public void drawTriangles (Iterable<float[]> points) {

      maybeUpdateMatrices(gl);

      // determine required buffer size
      int size = findSize(points);

      ByteBuffer buff = ByteBuffer.allocateDirect(size*6*GLSupport.FLOAT_SIZE);
      buff.order(ByteOrder.nativeOrder());

      Iterator<float[]> pit = points.iterator ();
      float[] normal = new float[3];
      
      while (pit.hasNext ()) {
         float[] p0 = pit.next ();
         float[] p1 = pit.next ();
         float[] p2 = pit.next ();
         computeNormal (p0, p1, p2, normal);

         buff.putFloat (p0[0]);
         buff.putFloat (p0[1]);
         buff.putFloat (p0[2]);
         buff.putFloat (normal[0]);
         buff.putFloat (normal[1]);
         buff.putFloat (normal[2]);

         buff.putFloat (p1[0]);
         buff.putFloat (p1[1]);
         buff.putFloat (p1[2]);
         buff.putFloat (normal[0]);
         buff.putFloat (normal[1]);
         buff.putFloat (normal[2]);

         buff.putFloat (p2[0]);
         buff.putFloat (p2[1]);
         buff.putFloat (p2[2]);
         buff.putFloat (normal[0]);
         buff.putFloat (normal[1]);
         buff.putFloat (normal[2]);
      }
      
      buff.rewind();
      GL3Object glo = GL3Object.createVN(gl, GL.GL_TRIANGLES, 
         buff, 3, BufferStorage.FLOAT_3, 0, 6*GLSupport.FLOAT_SIZE, BufferStorage.FLOAT_3, 
         3*GLSupport.FLOAT_SIZE, 6*GLSupport.FLOAT_SIZE, GL3.GL_STREAM_DRAW);

      maybeUpdateMatrices(gl);
      glo.draw(gl, getRegularProgram(gl));

      glo.dispose (gl);  // immediately dispose


   }

   /**
    * Assumed per-vertex normal
    */
   public void drawTriangles (Iterable<float[]> points, Iterable<float[]> normals) {
      drawPrimitives (points, normals, -1, GL2.GL_TRIANGLES);
   }

   //======================================================================
   // RENDER OBJECT STUFF
   //======================================================================

   private GL3RenderObject getOrCreateGRO(RenderObject robj) {
      GL3Resource res;
      GL3RenderObject gro;
      RenderObjectIdentifier key = robj.getIdentifier();

      synchronized(myGLResources) {
         res = myGLResources.getResource(key);
         if (res == null) {
            gro = new GL3RenderObject(robj);
            gro.init(gl, robj);
            myGLResources.addResource(gl, key, gro);

         } else {
            gro = (GL3RenderObject)res;
            gro.maybeUpdate(gl, robj);
         }
      }
      return gro;
   }

   protected int getProgram(GL3 gl, RenderObjectState robj) {
      if (isSelecting()) {
         return getBasicProgram(gl);
      } else {
         Shading shading = getShadeModel();
         if (!isLightingEnabled()) {
            shading = Shading.NONE;
         }
         ColorInterpolation cinterp = ColorInterpolation.RGB;
         if (!robj.hasColors() || !isVertexColoringEnabled()) {
            cinterp = ColorInterpolation.NONE;
         } else if (isHSVColorInterpolationEnabled()) {
            cinterp = ColorInterpolation.HSV;
         }
         GLSLInfo key = new GLSLInfo(progManager.numLights(),
            progManager.numClipPlanes(), 
            shading, cinterp, 
            isLightingEnabled() && robj.hasNormals(), 
            isVertexColoringEnabled() && robj.hasColors(),
            isTextureMappingEnabled() && robj.hasTextureCoords());

         return progManager.getProgram(gl,key);
      }
   }

   protected int getPointsProgram(GL3 gl, RenderObjectState robj) {
      GLSLInfo key = null;
      if (isSelecting()) {
         key = new GLSLInfo(progManager.numLights(), progManager.numClipPlanes(), 
            Shading.NONE, ColorInterpolation.NONE,
            false, false, false, 
            InstancedRendering.POINTS, 
            false, false, 
            false, false, false);
      } else {
         Shading shading = getShadeModel();
         if (!isLightingEnabled()) {
            shading = Shading.NONE;
         }
         ColorInterpolation cinterp = ColorInterpolation.RGB;
         if (!robj.hasColors() || !isVertexColoringEnabled()) {
            cinterp = ColorInterpolation.NONE;
         } else if (isHSVColorInterpolationEnabled()) {
            cinterp = ColorInterpolation.HSV;
         }
         key = new GLSLInfo(progManager.numLights(),
            progManager.numClipPlanes(), 
            shading, cinterp, isLightingEnabled(), false, false,
            InstancedRendering.POINTS, 
            isVertexColoringEnabled() && robj.hasColors(),
            isTextureMappingEnabled() && robj.hasTextureCoords(),
            false, false, false);
      }
      return progManager.getProgram(gl,key);
   }

   protected int getLinesProgram(GL3 gl, RenderObjectState robj, LineStyle style) {
      GLSLInfo key = null;
      if (isSelecting()) {
         key = new GLSLInfo(progManager.numLights(), progManager.numClipPlanes(), 
            Shading.NONE, ColorInterpolation.NONE,
            false, false, false, 
            InstancedRendering.LINES, 
            false, false, 
            false, false, false);
      } else {
         Shading shading = getShadeModel();
         if (!isLightingEnabled()) {
            shading = Shading.NONE;
         }
         ColorInterpolation cinterp = ColorInterpolation.RGB;
         if (!robj.hasColors() || !isVertexColoringEnabled()) {
            cinterp = ColorInterpolation.NONE;
         } else if (isHSVColorInterpolationEnabled()) {
            cinterp = ColorInterpolation.HSV;
         }
         key = new GLSLInfo(progManager.numLights(),
            progManager.numClipPlanes(), 
            shading, cinterp, isLightingEnabled(), false, false,
            InstancedRendering.LINES,
            false, false,
            style == LineStyle.SOLID_ARROW,
            isVertexColoringEnabled() && robj.hasColors(),
            isTextureMappingEnabled() && robj.hasTextureCoords());
      }
      return progManager.getProgram(gl,key);
   }

   @Override
   public void drawTriangles(RenderObject robj) {
      GLSupport.checkAndPrintGLError(gl);
      GL3RenderObject gro = getOrCreateGRO(robj);
      maybeUpdateMatrices(gl);
      gl.glUseProgram(getProgram(gl, gro.getRenderObjectState()));
      gro.drawTriangles(gl);
      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void drawLines(RenderObject robj) {
      GLSupport.checkAndPrintGLError(gl);
      GL3RenderObject gro = getOrCreateGRO(robj);
      maybeUpdateMatrices(gl);
      gl.glUseProgram(getProgram(gl, gro.getRenderObjectState()));
      gro.drawLines(gl);
      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void drawPoints(RenderObject robj) {
      GLSupport.checkAndPrintGLError(gl);
      GL3RenderObject gro = getOrCreateGRO(robj);
      maybeUpdateMatrices(gl);
      gl.glUseProgram(getProgram(gl, gro.getRenderObjectState()));
      gro.drawPoints(gl);
      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void drawVertices(RenderObject robj, VertexDrawMode mode) {
      GLSupport.checkAndPrintGLError(gl);
      GL3RenderObject gro = getOrCreateGRO(robj);
      maybeUpdateMatrices(gl);
      gl.glUseProgram(getProgram(gl, gro.getRenderObjectState()));
      gro.drawVertices(gl, mode);
      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void draw(RenderObject robj) {
      GLSupport.checkAndPrintGLError(gl);
      drawPoints(robj);
      drawLines(robj);
      drawTriangles(robj);
      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void drawLines(RenderObject robj, LineStyle style, double rad) {
      maybeUpdateMatrices(gl);

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

            GL3RenderObjectLines gro = getOrCreateLineGRO(robj,LineStyle.LINE, 0);
            gl.glUseProgram(getProgram(gl, gro.getRenderObjectState()));
            gro.drawLines(gl);

            if (changed) {
               setLineWidth(fold);
            }
            break;
         }
         default: {
            GL3RenderObjectLines gro = getOrCreateLineGRO(robj, style, (float)rad);
            gl.glUseProgram(getLinesProgram(gl, gro.getRenderObjectState(), style));
            gro.drawLines(gl);  
            break;
         }
      }
   }

   private static class RenderObjectPointKey {
      RenderObjectIdentifier roId;

      RenderObjectPointKey(RenderObjectIdentifier roId) {
         this.roId = roId;
      }

      @Override
      public int hashCode() {
         return roId.hashCode();
      }

      public boolean equals(RenderObjectPointKey other) {
         return roId.equals(other.roId);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != getClass()) {
            return false;
         }
         RenderObjectPointKey other = (RenderObjectPointKey)obj;
         return equals(other);
      }
   }

   private static class RenderObjectLineKey {
      RenderObjectIdentifier roId;

      RenderObjectLineKey(RenderObjectIdentifier roId) {
         this.roId = roId;
      }

      @Override
      public int hashCode() {
         return roId.hashCode();
      }

      public boolean equals(RenderObjectLineKey other) {
         return roId.equals(other.roId);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != getClass()) {
            return false;
         }
         RenderObjectLineKey other = (RenderObjectLineKey)obj;
         return equals(other);
      }
   }

   private GL3RenderObjectPoints getOrCreatePointGRO(RenderObject robj, 
      PointStyle style, float rad) {

      GL3Resource res;
      GL3RenderObjectPoints gro;
      RenderObjectPointKey key = new RenderObjectPointKey(robj.getIdentifier());

      GL3Object pointObject = null;
      if (style == PointStyle.SPHERE) {
         pointObject = myGLResources.getSphere(gl, 64, 32);
      }

      synchronized(myGLResources) {

         res = myGLResources.getResource(key);
         if (res == null) {
            gro = new GL3RenderObjectPoints(robj);
            gro.init(gl, robj, pointObject, rad);
            myGLResources.addResource(gl, key, gro);

         } else {
            gro = (GL3RenderObjectPoints)res;
            gro.maybeUpdate(gl, robj, pointObject, rad);
         }
      }
      return gro;
   }

   private GL3RenderObjectLines getOrCreateLineGRO(
      RenderObject robj, LineStyle style, float rad) {

      GL3Resource res;
      GL3RenderObjectLines gro;
      RenderObjectLineKey key = new RenderObjectLineKey(robj.getIdentifier());

      GL3Object lineObject = null;
      GL3Object headObject = null;
      float headLength = 0;
      float headRadius = 0;
      int slices = 64;
      boolean capped = true;
      switch(style) {
         case CYLINDER:
            lineObject = myGLResources.getCylinder(gl, slices, capped);
            break;
         case ELLIPSOID:
            lineObject = myGLResources.getTaperedEllipsoid(gl, slices, slices/2);
            break;
         case SOLID_ARROW:
            lineObject = myGLResources.getCylinder(gl, slices, capped);
            headRadius = 3*rad;
            headLength = 2*headRadius;
            headObject = myGLResources.getCone(gl, slices, capped);
            break;
         case LINE:
            break;
      }

      synchronized(myGLResources) {
         res = myGLResources.getResource(key);
         if (res == null) {
            gro = new GL3RenderObjectLines(robj);
            gro.init(gl, robj, lineObject, rad, headObject, headRadius, headLength);
            myGLResources.addResource(gl, key, gro);

         } else {
            gro = (GL3RenderObjectLines)res;
            gro.maybeUpdate(gl, robj, lineObject, rad, headObject, headRadius, headLength);
         }
      }
      return gro;
   }

   @Override
   public void drawPoints(RenderObject robj, PointStyle style, double rad) {
      maybeUpdateMatrices(gl);

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

            GL3RenderObjectPoints gro = getOrCreatePointGRO(
               robj, PointStyle.POINT, 0);
            gl.glUseProgram(getProgram(gl, gro.getRenderObjectState()));
            gro.drawPoints(gl);

            if (changed) {
               setPointSize(fold);
            }
            break;
         }
         case SPHERE: {
            GL3RenderObjectPoints gro = getOrCreatePointGRO(
               robj, PointStyle.SPHERE, (float)rad);
            gl.glUseProgram(getPointsProgram(gl, gro.getRenderObjectState()));
            gro.drawPoints(gl);  
            break;
         }
      }
   }

   @Override
   public RenderObject getSharedObject(Object key) {
      return myGLResources.getRenderObject(key);
   }

   @Override
   public void addSharedObject(Object key, RenderObject robj) {
      myGLResources.addRenderObject(key, robj);
   }

   @Override
   public void removeSharedObject(Object key) {
      myGLResources.removeRenderObject(key);
   }      

   protected void ensureDrawDataCapacity () {
      if (myNumVertices == myVertexCap) {
         if (myVertexCap == 0) {
            myVertexCap = 1000;
            myDrawBuffer =
               ByteBuffer.allocateDirect(myVertexCap*6*GLSupport.FLOAT_SIZE);
            myDrawBuffer.order(ByteOrder.nativeOrder());
         }
         else {
            myVertexCap = (int)(1.5*myVertexCap);
            ByteBuffer newBuffer =
               ByteBuffer.allocateDirect(myVertexCap*6*GLSupport.FLOAT_SIZE);
            newBuffer.order(ByteOrder.nativeOrder());
            myDrawBuffer.rewind();
            newBuffer.put (myDrawBuffer);
            myDrawBuffer = newBuffer;
         }
      }
   }      

   @Override
   public void beginDraw (VertexDrawMode mode) {
      if (myDrawMode != null) {
         throw new IllegalStateException (
            "beginDraw() called while inside beginDraw() block");
      }
      myNumVertices = 0;
      ensureDrawDataCapacity();
      myDrawBuffer.rewind();
      myDrawMode = mode;
      setNormal (0, 0, 0);
   }

   @Override
   public void addVertex (float x, float y, float z) {
      ensureDrawDataCapacity();
      myDrawBuffer.putFloat (x);
      myDrawBuffer.putFloat (y);
      myDrawBuffer.putFloat (z);
      myDrawBuffer.putFloat (myCurrentNormal[0]);
      myDrawBuffer.putFloat (myCurrentNormal[1]);
      myDrawBuffer.putFloat (myCurrentNormal[2]);
      myNumVertices++;
   }

   @Override
   public void setNormal (float x, float y, float z) {
      myCurrentNormal[0] = x;
      myCurrentNormal[1] = y;
      myCurrentNormal[2] = z;
   }

   @Override
   public void endDraw() {
      if (myDrawMode == null) {
         throw new IllegalStateException (
            "endDraw() called before call to beginDraw()");
      }
      if (myNumVertices > 0) {
         myDrawBuffer.rewind();
         GL3Object glo = GL3Object.createVN(
            gl, getDrawPrimitive(myDrawMode), myDrawBuffer, myNumVertices,
            BufferStorage.FLOAT_3, 0, 6*GLSupport.FLOAT_SIZE,
            BufferStorage.FLOAT_3, 3*GLSupport.FLOAT_SIZE,
            6*GLSupport.FLOAT_SIZE, GL3.GL_STREAM_DRAW);

         maybeUpdateMatrices(gl);
         glo.draw(gl, getRegularProgram(gl));

         glo.dispose (gl);  // immediately dispose
      }
      myDrawMode = null;
   }


}
