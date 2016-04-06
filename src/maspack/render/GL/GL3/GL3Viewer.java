package maspack.render.GL.GL3;

import java.awt.Dimension;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL;
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
import maspack.render.RenderProps;
import maspack.render.GL.GLClipPlane;
import maspack.render.GL.GLFrameCapture;
import maspack.render.GL.GLGridPlane;
import maspack.render.GL.GLLightManager;
import maspack.render.GL.GLMouseAdapter;
import maspack.render.GL.GLProgramInfo.RenderingMode;
import maspack.render.GL.GLShaderProgram;
import maspack.render.GL.GLSupport;
import maspack.render.GL.GLTexture;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GL3.GL3SharedPrimitive.PrimitiveType;
import maspack.render.GL.GL3.GLSLGenerator.StringIntPair;
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
   long lastGarbageTime = 0;  // for garbage collecting of viewer-specific resources
   
   // State
   ViewerState myCommittedViewerState = null;

   // Updateable object for various primitives (essentially stream-drawn)
   // like single lines, etc...
   GL3FlexObject gloFlex = null;
   GL3Primitive[] primitives = null;

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
      myRenderObjectManager = new GL3RenderObjectManager (resources.getSharedRenderObjectManager());
      
      myPrimitiveManager = new GL3PrimitiveManager (resources.getSharedPrimitiveManager());
      primitives = new GL3Primitive[PrimitiveType.values ().length];
      
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

      gl.glClearColor (backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

      resetViewVolume(gl);
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

      GLSupport.checkAndPrintGLError(gl);

      this.gl = null;
      this.drawable = null;
   }

   /**
    * Do some clean up of resources
    * @param gl
    */
   private void garbage(GL3 gl) {
      myRenderObjectManager.garbage (gl);
      myPrimitiveManager.garbage (gl);
      lastGarbageTime = System.currentTimeMillis ();
   }
   
   @Override
   public void dispose(GLAutoDrawable drawable) {
      GLSupport.checkAndPrintGLError(drawable.getGL ());

      this.drawable = drawable;
      this.gl = drawable.getGL ().getGL3 ();
      
      if (this.primitives != null) {
         for (GL3Primitive prim : primitives) {
            if (prim != null) {
               prim.releaseDispose (gl);
            }
         }
         this.primitives = null;
      }
      
      myProgManager.dispose(gl);
      myRenderObjectManager.dispose (gl);
      myPrimitiveManager.dispose (gl);

      // clear temporaries
      gloFlex.dispose (gl);
      gloFlex = null;

      System.out.println("GL3 disposed");
      GLSupport.checkAndPrintGLError(drawable.getGL ());
      
      // nullify stuff
      this.gl = null;
      this.drawable = null;
   }

   @Override
   public void dispose () {
      myGLResources.deregisterViewer (this);
      myGLResources = null;
      super.dispose ();
   }
   
   @Override
   public void display(GLAutoDrawable drawable, int flags) {

      this.drawable = drawable;
      this.gl = drawable.getGL ().getGL3 ();
      
      GLSupport.checkAndPrintGLError(gl);

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

      GLSupport.checkAndPrintGLError(gl);
      
      // check if we should do a garbage collection
      long time = System.currentTimeMillis ();
      if (time - lastGarbageTime > myGLResources.getGarbageCollectionInterval()) {
         garbage(gl);
      }
      
      this.drawable = null;
      this.gl = null;
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
         gl3.glClearColor (backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
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
      synchronized(myInternalRenderList) {
         qid = myInternalRenderList.renderOpaque (this, qid, flags);
      }
      
      if (myExternalRenderList != null) {
         synchronized(myExternalRenderList) {
            qid = myExternalRenderList.renderOpaque (this, qid, flags);
         }
      }
      
      GLSupport.checkAndPrintGLError(gl);

      if (hasTransparent3d()) {
         
         if (!isSelecting()) {
            enableTransparency (gl);
         }

         synchronized(myInternalRenderList) {
            qid = myInternalRenderList.renderTransparent (this, qid, flags);
         }
         if (myExternalRenderList != null) {
            synchronized(myExternalRenderList) {
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

         synchronized(myInternalRenderList) {
            qid = myInternalRenderList.renderOpaque2d (this, qid, 0);
         }
         if (myExternalRenderList != null) {
            synchronized(myExternalRenderList) {
               qid = myExternalRenderList.renderOpaque2d (this, qid, 0);
            }
         }

         if ( hasTransparent2d() ) {
            enableTransparency (gl);

            synchronized(myInternalRenderList) {
               qid = myInternalRenderList.renderTransparent2d (this, qid, 0);
            }
            if (myExternalRenderList != null) {
               synchronized (myExternalRenderList) {
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
            drawDragBox (gl);
         }
      } else {
         // revert clear color
         gl.glClearColor (backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
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
      if (!getTransparencyFaceCulling ()) {
         pushViewerState();
         setDepthEnabled(false);
         setFaceStyle(FaceStyle.FRONT_AND_BACK);
      }

      setTransparencyEnabled (true);
      // XXX maybe set configurable?
      gl.glBlendFunc (GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
   }

   private void disableTransparency (GL3 gl) {
      if (!getTransparencyFaceCulling ()) {
         popViewerState();
      }
      setTransparencyEnabled (false);
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
      maybeUpdateViewerState (gl);
   }
   
   /**
    * Force all viewer state variables to be written.  Some state variables
    * will not be committed, depending on whether we are in "select" mode
    * @param gl context
    * @param state state to commit
    */
   private void commitFullViewerState(GL3 gl, ViewerState state) {
      
      myCommittedViewerState = state.clone ();

      if (isSelecting ()) {
         // if selecting, disable lighting and blending         
         myProgramInfo.setShading (Shading.NONE);
         gl.glColorMask (true, true, true, true);
         gl.glDisable (GL.GL_BLEND);
         myCommittedViewerState.lightingEnabled = false;
         myCommittedViewerState.colorEnabled = true;
         myCommittedViewerState.transparencyEnabled = false;
         
      } else {
         
         // otherwise, track info
         if (!state.lightingEnabled) {
            myProgramInfo.setShading (Shading.NONE);
         } else {
            myProgramInfo.setShading (state.shading);
         }
         if (state.colorEnabled) {
            gl.glColorMask (true, true, true, true);
         } else {
            gl.glColorMask (false, false, false, false);
         }
         
         if (state.transparencyEnabled) {
            gl.glEnable (GL.GL_BLEND);
         } else {
            gl.glDisable (GL.GL_BLEND);
         }
      }
      
      if (state.depthEnabled) {
         gl.glEnable (GL.GL_DEPTH_TEST);
      } else {
         gl.glDisable (GL.GL_DEPTH_TEST);
      }
      
      switch (state.faceMode) {
         case BACK:
            gl.glEnable (GL.GL_CULL_FACE);
            gl.glCullFace (GL.GL_FRONT);
            break;
         case FRONT:
            gl.glEnable (GL.GL_CULL_FACE);
            gl.glCullFace (GL.GL_BACK);
            break;
         case FRONT_AND_BACK:
            gl.glDisable (GL.GL_CULL_FACE);
            break;
         case NONE:
            gl.glEnable (GL.GL_CULL_FACE);
            gl.glCullFace (GL.GL_FRONT_AND_BACK);
            break;
         default:
            break;
      }
      
      if (state.roundedPoints) {
         myProgramInfo.setRoundPointsEnabled (state.roundedPoints);
      }
      
      // vertexColorsEnabled;   // set manually in draw methods
      // textureMappingEnabled;   
      // hsvInterpolationEnabled;  
      // colorMixing;           // not available
      // transparencyFaceCulling;  // set manually in draw methods
      
   }
   
   private void maybeCommitViewerState(GL3 gl, ViewerState state) {
      
      if (isSelecting ()) {
         // if selecting, disable lighting and blending
         if (myCommittedViewerState.lightingEnabled == true) {
            myProgramInfo.setShading (Shading.NONE);
            myCommittedViewerState.lightingEnabled = false;
            myCommittedViewerState.shading = Shading.NONE;
         }
         
         if (myCommittedViewerState.colorEnabled == false) {
            gl.glColorMask (true, true, true, true);
            myCommittedViewerState.colorEnabled = true;
         }
         
         if (myCommittedViewerState.transparencyEnabled == true) {
            gl.glDisable (GL.GL_BLEND);
            myCommittedViewerState.transparencyEnabled = false;
         }
         
      } else {
         
         // otherwise, track info
         
         if (myCommittedViewerState.lightingEnabled != state.lightingEnabled) {
            if (!state.lightingEnabled) {
               myProgramInfo.setShading (Shading.NONE);
               myCommittedViewerState.shading = Shading.NONE;
            }
            myCommittedViewerState.lightingEnabled = state.lightingEnabled;
         }
         
         if (state.lightingEnabled) {
            if (myCommittedViewerState.shading != state.shading) {
               myProgramInfo.setShading (state.shading);
               myCommittedViewerState.shading = state.shading;
            }
         }
         
         if (myCommittedViewerState.colorEnabled != state.colorEnabled) {
            if (state.colorEnabled) {
               gl.glColorMask (true, true, true, true);
            } else {
               gl.glColorMask (false, false, false, false);
            }
            myCommittedViewerState.colorEnabled = state.colorEnabled;
         }
         
         if (myCommittedViewerState.transparencyEnabled != state.transparencyEnabled) {
            if (state.transparencyEnabled) {
               gl.glEnable (GL.GL_BLEND);
            } else {
               gl.glDisable (GL.GL_BLEND);
            }
            myCommittedViewerState.transparencyEnabled = state.transparencyEnabled;
         }
      }
      
      if (myCommittedViewerState.depthEnabled != state.depthEnabled) {
         if (state.depthEnabled) {
            gl.glEnable (GL.GL_DEPTH_TEST);
         } else {
            gl.glDisable (GL.GL_DEPTH_TEST);
         }
         myCommittedViewerState.depthEnabled = state.depthEnabled;
      }
      
      if (myCommittedViewerState.faceMode != state.faceMode) {
         switch (state.faceMode) {
            case BACK:
               gl.glEnable (GL.GL_CULL_FACE);
               gl.glCullFace (GL.GL_FRONT);
               break;
            case FRONT:
               gl.glEnable (GL.GL_CULL_FACE);
               gl.glCullFace (GL.GL_BACK);
               break;
            case FRONT_AND_BACK:
               gl.glDisable (GL.GL_CULL_FACE);
               break;
            case NONE:
               gl.glEnable (GL.GL_CULL_FACE);
               gl.glCullFace (GL.GL_FRONT_AND_BACK);
               break;
            default:
               break;
         }
         myCommittedViewerState.faceMode = state.faceMode;
      }
      
      if (myCommittedViewerState.roundedPoints != state.roundedPoints) {
         myProgramInfo.setRoundPointsEnabled (state.roundedPoints);
         myCommittedViewerState.roundedPoints = state.roundedPoints;
      }
      
      // vertexColorsEnabled;   // set manually in draw methods
      // textureMappingEnabled;   
      // hsvInterpolationEnabled;  
      // colorMixing;           // not available
      // transparencyFaceCulling;  // set manually in draw methods
      
   }
   
   /**
    * Commit all pending changes
    * @param gl
    */
   protected void maybeUpdateViewerState(GL3 gl) {
      
      // maybe update shading
      if (myCommittedViewerState == null) {
         commitFullViewerState (gl, myViewerState);  
      } else {
         maybeCommitViewerState(gl, myViewerState);
      }
   }

   protected void maybeUpdateMaterials(GL3 gl) {
      if (myCurrentMaterialModified) {
         if (isSelecting ()) {
            myProgManager.setMaterials (gl, myCurrentMaterial, mySelectingColor, myCurrentMaterial, mySelectingColor);
         } else {
            // set all colors
            if (myHighlightColorActive) {
               myHighlightColor[3] = myCurrentMaterial.getAlpha();
               myProgManager.setMaterials (gl, myCurrentMaterial, myHighlightColor, myCurrentMaterial, myHighlightColor);
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

      GL3Primitive sphere = getPrimitive (gl, PrimitiveType.SPHERE);
      updateProgram (gl, RenderingMode.DEFAULT, true, false, false);
      sphere.draw(gl);

      // revert matrix transform
      popModelMatrix();
   }
   
   @Override
   public void drawCube(float[] pnt, double rad) {

      if (rad < Double.MIN_NORMAL) {
         return;
      }

      // scale and translate model matrix
      pushModelMatrix();
      translateModelMatrix(pnt[0], pnt[1], pnt[2]);
      scaleModelMatrix(rad/2);

      maybeUpdateState (gl);

      GL3Primitive cube = getPrimitive (gl, PrimitiveType.CUBE);
      updateProgram (gl, RenderingMode.DEFAULT, true, false, false);
      cube.draw(gl);

      // revert matrix transform
      popModelMatrix();
   }
   
   @Override
   public void drawCube (RigidTransform3d trans, Vector3d scale) {
    
      // scale and translate model matrix
      pushModelMatrix();
      mulModelMatrix (trans);
      scaleModelMatrix (scale.x/2, scale.y/2, scale.z/2);

      maybeUpdateState (gl);

      GL3Primitive cube = getPrimitive (gl, PrimitiveType.CUBE);
      updateProgram (gl, RenderingMode.DEFAULT, true, false, false);
      cube.draw(gl);

      // revert matrix transform
      popModelMatrix();
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

      GL3Object spindle = getPrimitive (gl, PrimitiveType.SPINDLE);
      updateProgram (gl, RenderingMode.DEFAULT, true, false, false);
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
      updateProgram (gl, RenderingMode.DEFAULT, true, false, false);
      
      int nslices = getSurfaceResolution();
      GL3Primitive cylinder = myPrimitiveManager.getAcquiredCylinder(gl, nslices, capped);
      cylinder.draw(gl);
      cylinder.release ();

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
      
      pushModelMatrix ();
      mulModelMatrix(lineRot);
      scaleModelMatrix (r, r, h);
      maybeUpdateState (gl);
      updateProgram (gl, RenderingMode.DEFAULT, true, false, false);
      
      int nSlices = getSurfaceResolution ();
      GL3Object cone = myPrimitiveManager.getAcquiredCone (gl, nSlices, capped);
      cone.draw (gl);
      cone.release ();
      
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
         updateProgram (gl, RenderingMode.DEFAULT, true, false, false);
         gloFlex.drawElements (gl, GL.GL_TRIANGLES);
         popModelMatrix ();
      }
   }
   
   protected GLShaderProgram updateProgram(GL3 gl, RenderingMode mode,
      boolean hasNormals, boolean hasColors, boolean hasTextures) {     
      
      myProgramInfo.setMode (mode);
      
      switch(mode) {
         
         case INSTANCED_POINTS:
         case INSTANCED_AFFINES:
         case INSTANCED_FRAMES:
            myProgramInfo.setInstanceColorsEnabled (hasColors);
            myProgramInfo.setShading (getShading());
            myProgramInfo.setVertexNormalsEnabled (true);
            break;
         case INSTANCED_LINES:
            myProgramInfo.setLineColorsEnabled (hasColors);
            myProgramInfo.setShading (getShading());
            myProgramInfo.setVertexNormalsEnabled (true);
            break;
         
         case POINTS:
         case DEFAULT:
         default:
            if (hasNormals) {
               myProgramInfo.setShading (getShading());
            } else {
               myProgramInfo.setShading (Shading.NONE);
            }
            myProgramInfo.setVertexNormalsEnabled (hasNormals);
            myProgramInfo.setVertexTexturesEnabled (hasTextures);
            
            // texture stuff
            if (hasTextures && isTextureMappingEnabled ()) {
               if (myColorMapProps != null && myColorMapProps.isEnabled()) {
                  myProgramInfo.setColorMapEnabled (true);
                  myProgramInfo.setTextureColorMixing (myColorMapProps.getTextureColorMixing ());
                  myProgramInfo.setMixTextureColorDiffuse (myColorMapProps.getDiffuseColoring ());
                  myProgramInfo.setMixTextureColorSpecular (myColorMapProps.getSpecularColoring ());
                  myProgramInfo.setMixTextureColorEmission (myColorMapProps.getDiffuseColoring ());
               } else {
                  myProgramInfo.setColorMapEnabled (false);
               }
               
               if (myNormalMapProps != null && myNormalMapProps.isEnabled()) {
                  myProgramInfo.setNormalMapEnabled (true);
               } else {
                  myProgramInfo.setNormalMapEnabled (false);
               }
               
               if (myBumpMapProps != null && myBumpMapProps.isEnabled()) {
                  myProgramInfo.setBumpMapEnabled (true);
               } else {
                  myProgramInfo.setBumpMapEnabled (false);
               }
            }
            
            break;
         
      }
      
      // myProgramInfo.setVertexColorsEnabled (hasColors);
      if (!hasColors || !isVertexColoringEnabled() || myHighlightColorActive) {
         myProgramInfo.setColorInterpolation (ColorInterpolation.NONE);
         myProgramInfo.setVertexColorsEnabled (false);
         myProgramInfo.setLineColorsEnabled (false);
         myProgramInfo.setInstanceColorsEnabled (false);
      } else { 
         if (isHSVColorInterpolationEnabled()) {
            myProgramInfo.setColorInterpolation (ColorInterpolation.HSV);   
         } else {
            myProgramInfo.setColorInterpolation (ColorInterpolation.RGB);
         }
         myProgramInfo.setVertexColorMixing (getVertexColorMixing());
         myProgramInfo.setVertexColorsEnabled (true);
      }
      
      GLShaderProgram prog;
      if (isSelecting ()) {
         prog = myProgManager.getSelectionProgram (gl, myProgramInfo);
      } else {
         prog = myProgManager.getProgram (gl, myProgramInfo);
      }
      prog.use (gl);
      
      return prog;
   }

   private void drawGLLine(GL3 gl, float[] coords0, float[] coords1) {

      gloFlex.begin (gl, 2);
      gloFlex.vertex (coords0);
      gloFlex.vertex (coords1);
      gloFlex.end (gl);

      maybeUpdateState(gl);
      updateProgram (gl, RenderingMode.DEFAULT, false, false, false);
      gloFlex.drawVertices (gl, GL.GL_LINES);

      GLSupport.checkAndPrintGLError(gl);
   }

   private void drawGLPoint(GL3 gl, float[] coords) {

      gloFlex.begin (gl, 1);
      gloFlex.vertex (coords);
      gloFlex.end (gl);

      maybeUpdateState(gl);

      updateProgram (gl, RenderingMode.POINTS, false, false, false);
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
      updateProgram (gl, RenderingMode.DEFAULT, true, false, false);

      GL3Primitive cylinder = myPrimitiveManager.getAcquiredCylinder(gl, nslices, capped);
      cylinder.draw(gl);
      cylinder.release ();

      popModelMatrix();
      pushModelMatrix();

      lineRot.setTranslation(coordsMid[0], coordsMid[1], coordsMid[2]);
      mulModelMatrix(lineRot);
      scaleModelMatrix(arrowRad, arrowRad, arrowLen);
      maybeUpdateState(gl);

      GL3Object cone = myPrimitiveManager.getAcquiredCone(gl, nslices, capped);
      cone.draw(gl);
      cone.release ();

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
         case CUBE: {
            drawCube(pnt, 2*props.getPointRadius ());
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

      updateProgram (gl, RenderingMode.DEFAULT, false, true, false);
      GL3Object axes = myPrimitiveManager.getAcquiredAxes(gl, true, true, true);
      axes.draw(gl);
      axes.release ();

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
      setLineWidth (gl, width);
      
      updateProgram (gl, RenderingMode.DEFAULT, false, true, false);
      GL3Object axes = myPrimitiveManager.getAcquiredAxes(gl, drawx, drawy, drawz);
      axes.draw(gl);
      axes.release ();

      setLineWidth(gl, 1);

      // revert matrix transform
      popModelMatrix();

      setSelectionHighlighting(savedHighlighting);

   }

   protected void drawDragBox(GL3 gl) {
      GLSupport.checkAndPrintGLError(gl);

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

      updateProgram (gl, RenderingMode.DEFAULT, false, false, false);
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

   @Override
   public int setSurfaceResolution (int nres) {
      int oldres = getSurfaceResolution ();
      if (oldres != nres) {
         for (int i=0; i<primitives.length; ++i) {
            GL3Primitive p = primitives[i];
            if (p != null) {
               if (gl != null) {
                  p.releaseDispose (gl);
               } else {
                  p.release ();
               }
               primitives[i] = null;
            }
         }
         return super.setSurfaceResolution (nres);
      }
      return oldres;
   }
   
   /**
    * Return a primitive object
    * @param gl
    * @param type
    * @return primitive
    */
   private GL3Primitive getPrimitive(GL3 gl, PrimitiveType type) {
      int pid = type.ordinal ();
      GL3Primitive primitive = primitives[pid];
      
      if (primitive != null) {
         if (primitive.disposeInvalid (gl)) {
            primitive.release (); // release if we are throwing away
         } else {
            return primitive;
         }
      }
      
      int resolution = getSurfaceResolution ();
      
      // rebuild primitive
      switch (type) {
         case CUBE:
            primitive = myPrimitiveManager.getAcquiredCube (gl);
            break;
         case CONE:
            primitive = myPrimitiveManager.getAcquiredCone(gl, resolution, true);
            break;
         case CYLINDER:
            primitive = myPrimitiveManager.getAcquiredCylinder(gl, resolution, true);
            break;
         case SPHERE:
            primitive = myPrimitiveManager.getAcquiredSphere(gl, resolution, resolution/2);
            break;
         case SPINDLE:
            primitive = myPrimitiveManager.getAcquiredSpindle(gl, resolution, resolution/2);
            break;
         case AXES:
            primitive = myPrimitiveManager.getAcquiredAxes(gl, true, true, true);
            break;
      }
      
      primitives[pid] = primitive;
      return primitive;
   }
   
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

   @Override
   public void drawPoint (float[] pnt) {
      drawGLPoint (gl, pnt);
   }

   @Override
   public void drawLine(float[] pnt0, float[] pnt1) {
      drawGLLine (gl, pnt0, pnt1);
   }

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
      updateProgram (gl, RenderingMode.DEFAULT, true, false, false);
      
      gloFlex.drawVertices (gl, GL.GL_TRIANGLES);

   }

   //======================================================================
   // RENDER OBJECT STUFF
   //======================================================================

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
         if (myColorMapProps != null && myColorMapProps.isEnabled ()) {
            colortex = myGLResources.getOrLoadTexture (gl, myColorMapProps.getContent ());   
         }
         if (myNormalMapProps != null && myNormalMapProps.isEnabled ()) {
            normtex = myGLResources.getOrLoadTexture (gl, myNormalMapProps.getContent ());
         }
         if (myBumpMapProps != null && robj.hasTextureCoords ()) {
            bumptex = myGLResources.getOrLoadTexture (gl, myBumpMapProps.getContent ());
         }
      }
      GLSupport.checkAndPrintGLError(gl);

      GLShaderProgram prog = updateProgram (gl, RenderingMode.DEFAULT, robj.hasNormals (), 
         robj.hasColors (), robj.hasTextureCoords ());
      GLSupport.checkAndPrintGLError(gl);

      if (colortex != null) {
         myProgManager.bindTexture (gl, "color_map", colortex);
      }
      if (normtex != null) {
         myProgManager.bindTexture (gl, "normal_map", normtex);
         myProgManager.setUniform (gl, prog, "normal_scale", myNormalMapProps.getNormalScale());
      }
      if (bumptex != null) {
         myProgManager.bindTexture (gl, "bump_map", bumptex);
         myProgManager.setUniform (gl, prog, "bump_scale", myBumpMapProps.getBumpScale());
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
      updateProgram (gl, RenderingMode.DEFAULT, robj.hasNormals (), robj.hasColors (), robj.hasTextureCoords ());

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
      updateProgram (gl, RenderingMode.POINTS, robj.hasNormals (), robj.hasColors (), robj.hasTextureCoords ());
      gro.drawPointGroup (gl, GL.GL_POINTS, gidx);
      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void drawVertices(RenderObject robj, DrawMode mode) {
      GLSupport.checkAndPrintGLError(gl);
      GL3RenderObjectIndexed gro = myRenderObjectManager.getIndexed (gl, robj);
      maybeUpdateState(gl);
      updateProgram (gl, RenderingMode.DEFAULT, robj.hasNormals (), robj.hasColors (), robj.hasTextureCoords ());
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
            float fold = getLineWidth(gl);
            float frad = (float)rad;
            boolean changed = false;
            if (fold != frad) {
               setLineWidth(gl, frad);
               changed = true;
            }

            updateProgram (gl, RenderingMode.DEFAULT, robj.hasNormals (), robj.hasColors (), robj.hasTextureCoords ());
            gro.drawLineGroup (gl, GL.GL_LINES, gidx);

            if (changed) {
               setLineWidth(gl, fold);
            }
            break;
         }
         default: {

            myProgramInfo.setLineScaleOffsetEnabled (true);
            updateProgram (gl, RenderingMode.INSTANCED_LINES, false, robj.hasColors (), false);
            myProgramInfo.setLineScaleOffsetEnabled (false);
            
            switch (style) {
               case CYLINDER: {
                  GL3Primitive primitive = getPrimitive (gl, PrimitiveType.CYLINDER);
                  gro.setRadius (gl, (float)rad);
                  gro.drawInstancedLineGroup (gl, primitive, gidx);
                  break;
               }
               case SOLID_ARROW: {
                  gro.setRadius (gl, (float)rad);
                  GL3Primitive cylinder = getPrimitive (gl, PrimitiveType.CYLINDER);
                  GL3Primitive cone = getPrimitive (gl, PrimitiveType.CONE);

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
                  GL3Primitive spindle = getPrimitive (gl, PrimitiveType.SPINDLE);
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
            float fold = getPointSize(gl);
            float frad = (float)rad;
            boolean changed = false;
            if (fold != frad) {
               setPointSize(gl, frad);
               changed = true;
            }

            updateProgram (gl, RenderingMode.POINTS, robj.hasNormals (), robj.hasColors (), robj.hasTextureCoords ());
            
            gro.drawPointGroup (gl, GL.GL_POINTS, gidx);

            if (changed) {
               setPointSize(gl, fold);
            }
            break;
         }
         case SPHERE: 
         case CUBE: {
            GL3Primitive point;
            if (style == PointStyle.SPHERE) {
               point = getPrimitive (gl, PrimitiveType.SPHERE);
            } else {
               point = getPrimitive (gl, PrimitiveType.CUBE);
            }
            gro.setRadius(gl, (float)rad);
            updateProgram (gl, RenderingMode.INSTANCED_POINTS, robj.hasNormals (), 
               robj.hasColors (), robj.hasTextureCoords ());
            gro.drawInstancedPointGroup (gl, point, gidx);
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

         maybeUpdateState(gl);
         updateProgram (gl, RenderingMode.DEFAULT, hasNormalData, hasColorData, hasTexData);

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
