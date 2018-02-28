/**
 * copyright (c) 2017, by the Authors: Antonio Sanchez, John E Lloyd (UBC) and
 * other ArtiSynth Team Members. Elliptic selection added by Doga Tekin (ETH).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL.GL3;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import javax.swing.event.MouseInputListener;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.ColorMapProps;
import maspack.render.Dragger3d;
import maspack.render.RenderInstances;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.TextureContent;
import maspack.render.VertexIndexArray;
import maspack.render.GL.GLClipPlane;
import maspack.render.GL.GLDrawableComponent;
import maspack.render.GL.GLFrameCapture;
import maspack.render.GL.GLGridPlane;
import maspack.render.GL.GLLightManager;
import maspack.render.GL.GLMouseAdapter;
import maspack.render.GL.GLProgramInfo;
import maspack.render.GL.GLProgramInfo.RenderingMode;
import maspack.render.GL.GLShaderProgram;
import maspack.render.GL.GLSupport;
import maspack.render.GL.GLTextRenderer;
import maspack.render.GL.GLTexture;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GL3.GL3SharedPrimitive.PrimitiveType;
import maspack.render.GL.GL3.GLSLGenerator.StringIntPair;
import maspack.util.BufferUtilities;
import maspack.util.InternalErrorException;
import maspack.util.Logger;
import maspack.util.PathFinder;

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
   GLShaderProgram myCommittedProgram = null;
   GLProgramInfo myCommittedProgramInfo = null;
   
   GL3SharedResources myGLResources = null;    // holds shared context and cache
   // Resources that stick with this viewer
   GL3RenderObjectManager myRenderObjectManager = null;
   GL3PrimitiveManager myPrimitiveManager = null;
   GLTextRenderer myTextRenderer = null;
   ColorMapProps myTextTextureProps = null;
   
   long lastGarbageTime = 0;  // for garbage collecting of viewer-specific resources
   
   // Updateable object for various primitives (essentially stream-drawn)
   // like single lines, etc...
   GL3FlexObject gloFlex = null;
   GL3Primitive[] primitives = null;
   ElementArray eaFlex = null;
   
   Object shaderOverrideKey;
   File[] shaderOverride; // mainly for debugging, force shader

   // screenshot
   private volatile GLFrameCapture frameCapture = null;
   private volatile boolean grab = false;
   private volatile boolean grabClose = false;        // clean up

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
      if (useGLJPanel) {
         Logger.getSystemLogger().debug("Using GLJPanel");
         canvas = GLDrawableComponent.create(myGLResources.createPanel());
      } else {
         canvas = GLDrawableComponent.create(myGLResources.createCanvas());
      }
      canvas.setSurfaceScale(new float[]{ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE});
      
      myGLResources.registerViewer (this);
      myRenderObjectManager = new GL3RenderObjectManager (resources);
      
      myPrimitiveManager = new GL3PrimitiveManager (resources.getSharedPrimitiveManager());
      primitives = new GL3Primitive[PrimitiveType.values ().length];
      
      myTextRenderer = null;
      myTextTextureProps = new ColorMapProps ();
      myTextTextureProps.setColorMixing (ColorMixing.MODULATE);
      myTextTextureProps.setEnabled (true);
      
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

   }

   @Override
   public void init(GLAutoDrawable drawable) {
      super.init (drawable);

      this.drawable = drawable;
      this.gl = GL3Utilities.wrap(drawable.getGL().getGL3());

      String glslVersion = gl.glGetString(GL3.GL_SHADING_LANGUAGE_VERSION);
      Logger logger = Logger.getSystemLogger();
      logger.info("GLSL Version: " + glslVersion);
      
      gl.setSwapInterval (1);

      int[] buff = new int[1];
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
      setShading(Shading.SMOOTH);
      setGammaCorrectionEnabled(true);

      gl.glClearColor (backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

      resetViewVolume(gl);
      invalidateModelMatrix();
      invalidateProjectionMatrix();
      invalidateViewMatrix();

      myProgManager.init(gl, lightManager.numLights(), 0);
      myProgManager.setMatrices(gl, projectionMatrix, viewMatrix, 
         modelMatrix, modelNormalMatrix, textureMatrix);
      myProgManager.setLights(gl, lightManager.getLights(), 
         1.0f/lightManager.getMaxIntensity(), viewMatrix);
      myProgManager.setMaterials (gl, myCurrentMaterial, myCurrentMaterial);
      myCurrentMaterialModified = true;  // trigger update of materials

      myTextRenderer = GLTextRenderer.generate (gl, 
         GL3PipelineRenderer.generate (gl,
            myGLResources.getVertexNormalAttribute ().getLocation (),
            myGLResources.getVertexColorAttribute ().getLocation (),
            myGLResources.getVertexTexcoordAttribute ().getLocation (),
            myGLResources.getVertexPositionAttribute ().getLocation ())
         );

      // create a basic position-based flexible object
      gloFlex = GL3FlexObject.generate (gl, 
         myGLResources.getVertexPositionAttribute (), myGLResources.getVertexNormalAttribute(), 
         myGLResources.getVertexColorAttribute(), myGLResources.getVertexTexcoordAttribute());

      eaFlex = ElementArray.generate (gl);
      
      // trigger rebuild of renderables
      buildInternalRenderList();

      Logger.getSystemLogger().debug("GL3 initialized");
      
      gl.glEnable (GL.GL_BLEND);
      gl.glBlendFunc (GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

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
      super.dispose (drawable);

      this.drawable = drawable;
      this.gl = GL3Utilities.wrap(drawable.getGL().getGL3 ());
      
      if (this.primitives != null) {
         for (int i=0; i<primitives.length; ++i) {
            GL3Primitive prim = primitives[i];
            if (prim != null) {
               prim.releaseDispose (gl);
               primitives[i] = null;
            }
         }
      }
      
      myProgManager.dispose(gl);
      myRenderObjectManager.dispose (gl);
      myPrimitiveManager.dispose (gl);
      myTextRenderer.dispose (gl);

      // clear temporaries
      gloFlex.dispose (gl);
      gloFlex = null;
      
      eaFlex.dispose (gl);
      eaFlex = null;

      Logger.getSystemLogger().debug("GL3 disposed");
      
      // nullify stuff
      this.gl = null;
      this.drawable = null;
   }

   @Override
   public void dispose () {
      super.dispose ();
      myGLResources.deregisterViewer (this);
   }
   
   @Override
   public void display(GLAutoDrawable drawable, int flags) {
      
      this.drawable = drawable;
      this.gl = GL3Utilities.wrap(drawable.getGL ().getGL3 ());

      // reset state (necessary because of GLJPanel)
      if (useGLJPanel) {
         myCommittedProgram = null;
      }
      
      try {
      
         if (!myInternalRenderListValid) {
            buildInternalRenderList();
         }
   
         if (selectTrigger) {
            mySelector.setupSelection (gl);
            selectEnabled = true;
            selectTrigger = false;
         }
   
         // turn off buffer swapping when doing a selection render because
         // otherwise the previous buffer sometimes gets displayed
         drawable.setAutoSwapBufferMode (selectEnabled ? false : true);

         if (myProfiling) {
            myTimer.start();
         }
      
         doDisplay (drawable, flags);
         
         if (myProfiling) {
            myTimer.stop();
            System.out.printf (
               "render time (msec): %9.4f %s\n", 
               myTimer.getTimeUsec()/1000.0, isSelecting() ? "(SELECT)" : "");
         }
         if (selectEnabled) {
            selectEnabled = false;
            mySelector.processSelection (gl);
         }
         else {
            fireRerenderListeners();
         }
   
         GLFrameCapture fc = frameCapture;
         if (fc != null && (grab || grabClose)) {
            synchronized(fc) {
               if (grab) {
                  offscreenCapture (fc, flags);
                  fc.unlock();
                  grab = false;
               }
               if (grabClose) {
                  fc.waitForCompletion();
                  fc.dispose(gl);
                  frameCapture = null;
                  grabClose = false;
               }
            }
         }
         
         // local garbage collection
         long time = System.currentTimeMillis ();
         if (time - lastGarbageTime > myGLResources.getGarbageCollectionInterval()) {
            garbage(gl);
         }
         // for non-timed garbage collection
         myGLResources.maybeRunGarbageCollection (gl);
         
      } catch (Exception e) {
         e.printStackTrace ();
      }
      
      //      // XXX code to display info on the buffer
      //      ByteBuffer pixels = BufferUtilities.newNativeByteBuffer(4*width*height);
      //      gl.glReadPixels(0, 0, width, height, GL3.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixels);
      //      GLSupport.showImage(pixels, width, height);
      //      BufferUtilities.freeDirectBuffer(pixels);
      //      GLSupport.checkAndPrintGLError(gl);

      this.drawable = null;
      this.gl = null;
   }   

   private void doDisplay(GLAutoDrawable drawable, int flags) {
      
      // updates projection matrix
      if (resetViewVolume && autoResizeEnabled) {
         resetViewVolume(gl);
         resetViewVolume = false;
      }
      
      if (resetViewport && autoViewportEnabled) {
         setViewport (gl, 0, 0, width, height);
         resetViewport = false;
      }

      int nclips = Math.min (2*myClipPlanes.size (), maxClipPlanes);
      myProgramInfo.setNumClipPlanes (nclips);
      myProgManager.reconfigure(gl, lightManager.numLights(), nclips);
      myProgManager.setLights(gl, lightManager.getLights(), 1.0f/lightManager.getMaxIntensity(), viewMatrix);

      // update matrices
      maybeUpdateState(gl);

      // clear background/depth
      gl.glClearColor (backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
      gl.glClear(GL.GL_COLOR_BUFFER_BIT |  GL.GL_DEPTH_BUFFER_BIT);

      if (!isSelecting()) {
         if (gridVisible) {
            myGrid.render (this, flags);
         }
         if (axisLength > 0) {
            if (solidAxes) {
               drawSolidAxes (null, axisLength, axisLength/50.0, false);
            }
            else {
               drawAxes (gl, axisLength);
            }
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

      for (int i=0; i<myProgManager.numClipPlanes (); ++i) {
         boolean enabled = gl.glIsEnabled (GL3.GL_CLIP_DISTANCE0 + i);
         if (enabled) {
            Logger.getSystemLogger().debug("Why is this enabled?");
         }
      }
      
      // enable clip planes
      int iclips = 0;
      if (nclips > 0) {
         iclips = myProgManager.setClipPlanes(gl, myClipPlanes);
         for (int i=0; i<iclips; ++i) {
            gl.glEnable(GL3.GL_CLIP_DISTANCE0+i);
         }
      }

      if (!isSelecting()) {
         setFrontColor (DEFAULT_MATERIAL_COLOR);
      }
      
      int qid = 0;
      synchronized(myInternalRenderList) {
         qid = myInternalRenderList.renderOpaque (this, qid, flags);
      }
      RenderList elist = myExternalRenderList;     
      if (elist != null) {
         synchronized(elist) {
            qid = elist.renderOpaque (this, qid, flags);
         }
      }
      
      if (hasTransparent3d()) {
         
         if (!isSelecting()) {
            enableTransparency ();
         }

         synchronized(myInternalRenderList) {
            qid = myInternalRenderList.renderTransparent (this, qid, flags);
         }
         if (elist != null) {
            synchronized(elist) {
               qid = elist.renderTransparent (this, qid, flags);
            }
         }
         
         if (!isSelecting()) {
            disableTransparency ();
         }
      }
      
      // disable clip planes
      for (int i=0; i<nclips; ++i) {
         gl.glDisable(GL3.GL_CLIP_DISTANCE0+i);
      }
      
      // Draw 2D objects
      if ( has2d() ) {
         begin2DRendering(width, height);

         try {
            synchronized(myInternalRenderList) {
               qid = myInternalRenderList.renderOpaque2d (this, qid, 0);
            }
            if (elist != null) {
               synchronized(elist) {
                  qid = elist.renderOpaque2d (this, qid, 0);
               }
            }

            if ( hasTransparent2d() ) {
               if (!isSelecting ()) {
                  enableTransparency ();
               }

               synchronized(myInternalRenderList) {
                  qid = myInternalRenderList.renderTransparent2d (this, qid, 0);
               }
               if (elist != null) {
                  synchronized (elist) {
                     qid = elist.renderTransparent2d (this, qid, 0);
                  }
               }

               if (!isSelecting()) {
                  disableTransparency ();
               }
            }
         }
         finally {
            // John Lloyd, Sep 2016: try to clean up if one of the render
            // methods throws and exception
            if (isTransparencyEnabled()) {
               disableTransparency ();
            }
            end2DRendering();
         }
      }

      if (!isSelecting()) {
         if (myDragBox != null) {
            drawDragBox (gl);
         }
         if (myEllipticCursorActive) {
            Point cursor = myMouseHandler.getCurrentCursor();
            if (cursor != null) {
               drawEllipticCursor(gl, cursor);
            }
         }
      } else {
         // revert clear color
         gl.glClearColor (backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
      }

      // trigger update of state (required for GLJPanel, which relies on 
      //                          transparency to be off)
      maybeUpdateState(gl);
      
      gl.glFlush();
   }

   private void offscreenCapture (GLFrameCapture fc, int flags) {

      boolean savedSelecting = selectEnabled;
      selectEnabled = false;

      // Initialize the OpenGL context FOR THE FBO
      // gl.setSwapInterval (1);

      // Set rendering commands to go to offscreen frame buffer
      fc.activateFBO(gl);

      // disable resetting of view volume during capture
      boolean autoResize = setAutoResizeEnabled (false);
      boolean autoViewport = setAutoViewportEnabled(false);
      doDisplay (drawable, flags);
      setAutoResizeEnabled(autoResize);
      setAutoViewportEnabled(autoViewport);

      fireRerenderListeners();

      // further drawing will go to screen
      fc.capture(gl);
      fc.deactivateFBO(gl);

      selectEnabled = savedSelecting;

   }
   
   @Override
   public GLTextRenderer getTextRenderer () {
      return myTextRenderer;
   }
   
   @Override
   public double drawText(Font font, String str, float[] pos, double emSize) {
      GL3 gl = getGL ().getGL3();
      
      if (font == null) {
         font = getDefaultFont();
      }
      
      boolean savedBlending = isBlendingEnabled ();
      boolean savedTexture = isTextureMappingEnabled ();
      boolean savedDepth = isDepthWriteEnabled ();
      BlendFactor dfactor = getBlendDestFactor ();
      BlendFactor sfactor = getBlendSourceFactor ();
      
      setDepthWriteEnabled (false);
      setBlendingEnabled (true);
      setDepthWriteEnabled (false);
      setBlendingEnabled (true);
      setBlendSourceFactor (BlendFactor.GL_SRC_ALPHA);
      setBlendDestFactor (BlendFactor.GL_ONE_MINUS_SRC_ALPHA);
      setTextureMappingEnabled (true);
      
      ColorMapProps savedTextureProps = setColorMap (myTextTextureProps);
      
      maybeUpdateState(gl);
      updateProgram (gl, RenderingMode.DEFAULT, true, false, true);
      
      myProgManager.activateTexture(gl, "color_map");

      myTextRenderer.begin (gl);
      double d = myTextRenderer.drawText (font, str, pos, (float)emSize);
      myTextRenderer.end (gl);
            
      setDepthWriteEnabled (savedDepth);
      setBlendSourceFactor (sfactor);
      setBlendDestFactor (dfactor);
      setBlendingEnabled (savedBlending);
      setTextureMappingEnabled (savedTexture);
      setColorMap (savedTextureProps);
      
      return d;
   }

   // Screen-shot

   @Override
   public void setupScreenShot(
      int w, int h, int samples, File file, String format) {
      boolean gammaCorrection = isGammaCorrectionEnabled();
      GLFrameCapture fc = frameCapture;
      if (fc == null) {
         fc = new GLFrameCapture (w, h, samples, gammaCorrection, file, format);
         fc.lock();
         frameCapture = fc;
      }
      else {
         synchronized(fc) {
            fc.lock();
            fc.reconfigure(gl, w, h, samples, gammaCorrection, file, format);
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
          frameCapture.waitForCompletion();
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
      myProgManager.setMatrices(gl, projectionMatrix, viewMatrix, 
         modelMatrix, modelNormalMatrix, textureMatrix);
      modelMatrixValidP = true;
      projectionMatrixValidP = true;
      viewMatrixValidP = true;
   }

   public void maybeUpdateMaterials() {
      maybeUpdateMaterials(gl);
   }

   protected void maybeUpdateState() {
      maybeUpdateState(gl);
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
   protected void commitFullViewerState(GL2GL3 gl, ViewerState state) {
      
      super.commitFullViewerState (gl, state);

      if (isSelecting ()) {
         // if selecting, disable lighting and blending         
         myProgramInfo.setShading (Shading.NONE);
         myCommittedViewerState.lightingEnabled = false;
         myCommittedViewerState.shading = Shading.NONE;
      } else {
         // otherwise, track info
         if (!state.lightingEnabled) {
            myProgramInfo.setShading (Shading.NONE);
         } else {
            myProgramInfo.setShading (state.shading);
         }
      }
      
   }
   
   @Override
   protected void maybeCommitViewerState(GL2GL3 gl, ViewerState state) {
      
      super.maybeCommitViewerState (gl, state);
      
      if (isSelecting ()) {
         // if selecting, disable lighting and blending
         if (myCommittedViewerState.lightingEnabled == true) {
            myProgramInfo.setShading (Shading.NONE);
            myCommittedViewerState.lightingEnabled = false;
            myCommittedViewerState.shading = Shading.NONE;
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
      }
       
   }

   protected void maybeUpdateMaterials(GL3 gl) {
      
      if (isSelecting ()) {
         
         //         if (myCurrentMaterialModified || mySelectingColorModified || myCommittedColor != ActiveColor.SELECTING) {
         //            myProgManager.setMaterials (gl, myCurrentMaterial, mySelectingColor, myCurrentMaterial, mySelectingColor);
         //            myCurrentMaterialModified = false;
         //            mySelectingColorModified = false;
         //            myCommittedColor = ActiveColor.SELECTING;
         //         }
         if (mySelectingColorModified && myCommittedProgram != null && myCommittedProgramInfo.isSelecting ()) {
            myProgManager.setSelectionColor (gl, myCommittedProgram, mySelectingColor);
            mySelectingColorModified = false;
         }
      } else {
         
         if (myCurrentMaterial.getAlpha () != myHighlightColor[3]) {
            myHighlightColor[3] = myCurrentMaterial.getAlpha ();
            myHighlightColorModified = true;
         }
         
         if (myCurrentMaterialModified || myHighlightColorModified || myCommittedColor != myActiveColor) {
            if (myActiveColor == ActiveColor.HIGHLIGHT) {
               myProgManager.setMaterials (gl, myCurrentMaterial, myHighlightColor, myCurrentMaterial, myHighlightColor);
               myHighlightColorModified = false;
            } else {
               myProgManager.setMaterials (gl, myCurrentMaterial, myCurrentMaterial.getDiffuse(), myCurrentMaterial, myBackColor);
            }
            myCurrentMaterialModified = false;
            myCommittedColor = myActiveColor;
         }
         
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
      
      // XXX use instanced rendering?

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
   public void drawCube(float[] pnt, double w) {

      if (w < Double.MIN_NORMAL) {
         return;
      }

      // scale and translate model matrix
      pushModelMatrix();
      translateModelMatrix(pnt[0], pnt[1], pnt[2]);
      scaleModelMatrix(w/2);

      maybeUpdateState (gl);

      GL3Primitive cube = getPrimitive (gl, PrimitiveType.CUBE);
      updateProgram (gl, RenderingMode.DEFAULT, true, false, false);
      cube.draw(gl);

      // revert matrix transform
      popModelMatrix();
   }
   
   @Override
   public void drawBox (float[] pnt, double wx, double wy, double wz) {

      if (wx < Double.MIN_NORMAL || 
          wy < Double.MIN_NORMAL ||
          wz < Double.MIN_NORMAL) {
         return;
      }

      // scale and translate model matrix
      pushModelMatrix();
      translateModelMatrix(pnt[0], pnt[1], pnt[2]);
      scaleModelMatrix(wx/2, wy/2, wz/2);

      maybeUpdateState (gl);

      GL3Primitive cube = getPrimitive (gl, PrimitiveType.CUBE);
      updateProgram (gl, RenderingMode.DEFAULT, true, false, false);
      cube.draw(gl);

      // revert matrix transform
      popModelMatrix();
   }
   
   @Override
   public void drawBox (RigidTransform3d TBM, Vector3d widths) {
    
      // scale and translate model matrix
      pushModelMatrix();
      mulModelMatrix (TBM);
      scaleModelMatrix (widths.x/2, widths.y/2, widths.z/2);

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
   
   protected void updateProgramInfo(RenderingMode mode,
      boolean hasNormals, boolean hasColors, boolean hasTextures) {
      myProgramInfo.setMode (mode);
      myProgramInfo.setColorMapEnabled (false);
      myProgramInfo.setNormalMapEnabled (false);
      myProgramInfo.setBumpMapEnabled (false);
      
      if (isSelecting ()) {
         myProgramInfo.setSelecting (true);
         hasNormals = false;
         hasColors = false;
         hasTextures = false;
      } else {
         myProgramInfo.setSelecting (false);
      }
      
      switch(mode) {
         case INSTANCED_POINTS:
         case INSTANCED_AFFINES:
         case INSTANCED_FRAMES:
            myProgramInfo.setInstanceColorsEnabled (hasColors);
            myProgramInfo.setShading (getShading());
            myProgramInfo.setVertexNormalsEnabled (hasNormals);
            break;
         case INSTANCED_LINES:
            myProgramInfo.setLineColorsEnabled (hasColors);
            myProgramInfo.setShading (getShading());
            myProgramInfo.setVertexNormalsEnabled (hasNormals);
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
                  myProgramInfo.setTextureColorMixing (myColorMapProps.getColorMixing ());
                  myProgramInfo.setMixTextureColorDiffuse (myColorMapProps.getDiffuseColoring ());
                  myProgramInfo.setMixTextureColorSpecular (myColorMapProps.getSpecularColoring ());
                  myProgramInfo.setMixTextureColorEmission (myColorMapProps.getEmissionColoring ());
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
      
      if (mode == RenderingMode.POINTS && getRoundedPoints () &&
         getPointSize () >= 4) {
         myProgramInfo.setRoundPointsEnabled (true);
      } else {
         myProgramInfo.setRoundPointsEnabled (false);
      }
      
      // myProgramInfo.setVertexColorsEnabled (hasColors);
      if (!hasColors || !isVertexColoringEnabled() || isSelecting () || (myActiveColor != ActiveColor.DEFAULT)) {
         myProgramInfo.setVertexColorMixing (ColorMixing.NONE);
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
   }
      
   protected boolean maybeBindTextures(GL3 gl, GLShaderProgram prog) {
      
      boolean bound = false;
      // set texture map bindings
      if (myProgramInfo.hasColorMap ()) {
         TextureContent content = myColorMapProps.getContent ();
         if (content != null) {
            GLTexture colortex = myGLResources.getOrLoadTexture (gl, content);
            myProgManager.bindTexture (gl, "color_map", colortex);
            bound = true;
         }
      }
      if (myProgramInfo.hasNormalMap ()) {
         TextureContent content = myNormalMapProps.getContent ();
         if (content != null) {
            GLTexture normtex = myGLResources.getOrLoadTexture (gl, content);
            myProgManager.bindTexture (gl, "normal_map", normtex);
            myProgManager.setUniform (gl, prog, "normal_scale", myNormalMapProps.getScaling());
            bound = true;
         }
      }
      if (myProgramInfo.hasBumpMap ()) { 
         TextureContent content = myBumpMapProps.getContent ();
         if (content != null) {
            GLTexture bumptex = myGLResources.getOrLoadTexture (gl, content);
            myProgManager.bindTexture (gl, "bump_map", bumptex);
            myProgManager.setUniform (gl, prog, "bump_scale", myBumpMapProps.getScaling());
            bound = true;
         }
      }
      return bound;
   }
   
   //   private static File[] debugShaders = {
   //      ArtisynthPath.getSrcRelativeFile(GL3Viewer.class, "shaders/test_vertex.glsl"),
   //      ArtisynthPath.getSrcRelativeFile(GL3Viewer.class, "shaders/test_fragment.glsl")
   //   };
   
   public void setShaderOverride(Object shaderKey, File[] vf) {
      shaderOverrideKey = shaderKey;
      if (vf != null) {
         shaderOverride = Arrays.copyOf (vf, vf.length);
      } else {
         shaderOverride = null;
      }
   }
   
   public void useProgram(GL3 gl, Object key) {
      GLShaderProgram prog;
      prog = myProgManager.getProgram (gl, key);
      
      if (prog == null && shaderOverride != null && key.equals(shaderOverrideKey)) {
         prog = myProgManager.getProgram (gl, shaderOverrideKey, shaderOverride);
      }
      
      if (prog != null) {
           useProgram(gl, prog);
      } 
   }
   
   private void useProgram(GL3 gl, GLShaderProgram prog) {
      // only update program if different
      if (prog != myCommittedProgram) {
         prog.use (gl);
         // set selection color on first time use
         if (isSelecting ()) {
            myProgManager.setSelectionColor (gl, prog, mySelectingColor);
            mySelectingColorModified = false;
         }
         myCommittedProgram = prog;
         if (shaderOverride == null) {
            myCommittedProgramInfo = myProgramInfo.clone ();
         } else {
            myCommittedProgramInfo = null;
         }
      }
      maybeBindTextures(gl, prog);
   }
   
   protected GLShaderProgram updateProgram(GL3 gl, RenderingMode mode,
      boolean hasNormals, boolean hasColors, boolean hasTextures) {     
            
      updateProgramInfo(mode, hasNormals, hasColors, hasTextures);
      
      GLShaderProgram prog;
      if (shaderOverride != null) {
         prog = myProgManager.getProgram (gl, shaderOverrideKey, shaderOverride);
      } else if (isSelecting ()) {
         prog = myProgManager.getSelectionProgram (gl, myProgramInfo);
      } else {
         prog = myProgManager.getProgram (gl, myProgramInfo);
      }
      
      useProgram(gl, prog);
      
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

   }

   private void drawGLPoint(GL3 gl, float[] coords) {

      gloFlex.begin (gl, 1);
      gloFlex.vertex (coords);
      gloFlex.end (gl);

      maybeUpdateState(gl);

      updateProgram (gl, RenderingMode.POINTS, false, false, false);
      gloFlex.drawVertices(gl, GL.GL_POINTS);

   }


   @Override
   public void drawLine(
      RenderProps props, float[] pnt0, float[] pnt1, float[] color,
      boolean capped, boolean highlight) {

      boolean savedHighlighting = getHighlighting();
      Shading savedShading = setLineShading (props);
      if (color == null) {
         color = props.getLineColorF ();
      }
      setPropsColoring (props, color, highlight);
      switch (props.getLineStyle()) {
         case LINE: {
            //boolean savedLighting = isLightingEnabled();
            //setLightingEnabled (false);
            setLineWidth (gl, props.getLineWidth());

            //            if (color.length == 3 && props.getAlpha () < 1) {
            //               color = new float[]{color[0], color[1], color[2], (float)props.getAlpha ()};
            //            }
            //            setColor (color, selected);

            drawGLLine(gl, pnt0, pnt1);
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
            drawArrow (pnt0, pnt1, props.getLineRadius(), capped);
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
      setHighlighting (savedHighlighting);
   }

   @Override
   public void drawArrow(
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
      boolean highlight) {

      boolean savedHighlighting = getHighlighting();
      Shading savedShading = setLineShading (props);
      setLineColoring (props, highlight);

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
               setLineWidth (gl, props.getLineWidth());
               drawGLLine(gl, pnt0, ctmp);
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
      setHighlighting (savedHighlighting);      
   }

   @Override
   public void drawPoint(RenderProps props, float[] pnt, boolean highlight) {

      boolean savedHighlighting = getHighlighting();
      Shading savedShading = setPointShading (props);
      setPointColoring (props, highlight);
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
      setHighlighting (savedHighlighting);
   }

   public void drawAxes(GL3 gl, double len) {

      if (len == 0) {
         return;
      }

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
      RigidTransform3d X, double[] lens, int width, boolean highlight) {

      boolean savedHighlighting = setHighlighting(highlight);
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
      
      // update line width
      setLineWidth (gl, width);
      
      updateProgram (gl, RenderingMode.DEFAULT, false, true, false);
      GL3Object axes = myPrimitiveManager.getAcquiredAxes(gl, drawx, drawy, drawz);
      axes.draw(gl);
      axes.release ();

      
      // revert matrix transform
      popModelMatrix();

      setHighlighting(savedHighlighting);

   }

   protected void drawDragBox(GL3 gl) {

      begin2DRendering(-1, 1,-1, 1);

      float x0 = (float)(2 * myDragBox.x / (double)width - 1);
      float x1 = (float)(x0 + 2 * myDragBox.width / (double)width);
      float y0 = (float)(1 - 2 * myDragBox.y / (double)height);
      float y1 = (float)(y0 - 2 * myDragBox.height / (double)height);
      
      gloFlex.begin (gl, 4);
      gloFlex.vertex (x0, y0, 0);
      gloFlex.vertex (x1, y0, 0);
      gloFlex.vertex (x1, y1, 0);
      gloFlex.vertex (x0, y1, 0);
      gloFlex.end (gl);

      //gl.glLineWidth (1); shouldn't need - lineWidth be set to 1 in state
      setColor(0.5f, 0.5f, 0.5f, 1.0f);
      maybeUpdateState(gl);
      
      updateProgram (gl, RenderingMode.DEFAULT, false, false, false);
      gloFlex.drawVertices(gl, GL.GL_LINE_LOOP);

      end2DRendering();
   }
   
   protected void drawEllipticCursor (GL3 gl, Point cursor) {
      begin2DRendering(0, width, 0, height);
      
      float a = (float)myEllipticCursorSize.x;
      float b = (float)myEllipticCursorSize.y;
      float cx = (float)cursor.getX();
      float cy = (float)(height - cursor.getY());
      
      // change to a smaller/bigger number as needed 
      int num_segments = (int)(4*Math.ceil(Math.max(a,b)*Math.PI/2));
     
      gloFlex.begin (gl, num_segments); 
      for(int i = 0; i < num_segments; i++) {
         double ang = i*2*Math.PI/(double)num_segments;
         float x = a*(float)Math.cos(ang);
         float y = b*(float)Math.sin(ang);
         gloFlex.vertex (x + cx, y + cy, 0); //output vertex 
      } 
      gloFlex.end (gl); 
      
      gl.glLineWidth (2);
      //setColor(0.8f, 0.0f, 0.0f, 1.0f);
      setColor(0.5f, 0.5f, 0.5f);
      maybeUpdateState(gl);
      
      updateProgram (gl, RenderingMode.DEFAULT, false, false, false);
      gloFlex.drawVertices(gl, GL.GL_LINE_LOOP); 
      // gloFlex.drawVertices(gl, GL.GL_TRIANGLE_FAN); // Uncomment to draw solid circle.

      gl.glLineWidth (1);
      end2DRendering();
   }
     
   public void drawLineStrip (
      RenderProps props, Iterable<float[]> pnts, 
      LineStyle style, boolean highlight) {

      boolean savedHighlighting = getHighlighting();
      Shading savedShading = getShading();
      setShading (style==LineStyle.LINE ? Shading.NONE : props.getShading());
      setLineColoring (props, highlight);
      switch (style) {
         case LINE: {
            //setLightingEnabled (false);
            // draw regular points first
            setLineWidth (gl, props.getLineWidth());
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
                     drawArrow (v0, v1, rad, /*capped=*/true);
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
      setHighlighting (savedHighlighting);
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

   static File getShaderFile (String name) {
      String dirName = PathFinder.expand ("${srcdir GL3Viewer}/shaders/");
      return new File (dirName + name);
   }

   static File[] DEBUG_NORMAL_SHADERS = new File[] {
      getShaderFile ("camera_normal_vertex.glsl"),
      getShaderFile ("camera_normal_fragment.glsl")
   };
   
   static File[] DEBUG_CLIP_SHADERS = new File[] {
      getShaderFile ("clip_debug_vertex.glsl"),
      getShaderFile ("clip_debug_fragment.glsl")
   };
   
   static File[] DEBUG_INSTANCE_SHADERS = new File[] {
      getShaderFile ("instance_debug_vertex.glsl"),
      getShaderFile ("instance_debug_fragment.glsl")
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
   public void drawTriangles(RenderObject robj, int gidx, int offset, int count) {

      GL3RenderObjectPrimitives gro = myRenderObjectManager.getPrimitives (gl, robj);

      maybeUpdateState(gl);

      updateProgram (gl, RenderingMode.DEFAULT, robj.hasNormals (), 
         robj.hasColors (), robj.hasTextureCoords ());

      gro.drawTriangleGroup (gl, GL.GL_TRIANGLES, gidx, offset, count);

      //      if (bumptex != null) {
      //         myProgManager.unbindTexture (gl, "bump_map", bumptex);
      //      }
      //      if (normtex != null) {
      //         myProgManager.unbindTexture (gl, "normal_map", normtex);
      //      }
      //      if (colortex != null) {
      //         myProgManager.unbindTexture (gl, "color_map", colortex);
      //      }
      //
      //      GLSupport.checkAndPrintGLError(gl);
   }

   @Override
   public void drawLines(RenderObject robj, int gidx) {
      
      GL3RenderObjectPrimitives gro = myRenderObjectManager.getPrimitives (gl, robj);
      maybeUpdateState(gl);
      updateProgram (gl, RenderingMode.DEFAULT, robj.hasNormals (), robj.hasColors (), robj.hasTextureCoords ());

      gro.drawLineGroup (gl, GL.GL_LINES, gidx);
   }

   @Override
   public void drawPoints(RenderObject robj, int gidx) {
      GL3RenderObjectPrimitives gro = myRenderObjectManager.getPrimitives (gl, robj);
      
      maybeUpdateState(gl);
      updateProgram (gl, RenderingMode.POINTS, robj.hasNormals (), robj.hasColors (), robj.hasTextureCoords ());
      
      gro.drawPointGroup (gl, GL.GL_POINTS, gidx);
   }

   @Override
   public void drawVertices(RenderObject robj, DrawMode mode) {
      GL3RenderObjectPrimitives gro = myRenderObjectManager.getPrimitives (gl, robj);
      
      maybeUpdateState(gl);
      
      updateProgram (gl, RenderingMode.DEFAULT, robj.hasNormals (), 
         robj.hasColors (), robj.hasTextureCoords ());
      gro.drawVertices (gl, getDrawPrimitive (mode));
   }
   
   @Override
   public void drawVertices (
      RenderObject robj, VertexIndexArray idxs, int offset, int count, DrawMode mode) {
      GL3RenderObjectElements gro = myRenderObjectManager.getElements (gl, robj, idxs);
      maybeUpdateState(gl);
      updateProgram (gl, RenderingMode.DEFAULT, robj.hasNormals (), 
         robj.hasColors (), robj.hasTextureCoords ());
      gro.drawElements (gl, getDrawPrimitive(mode), offset, count);
   }
   
   public void drawVertices(RenderObject robj, int[] idxs, DrawMode mode) {
      
      GL3SharedRenderObjectPrimitives gro = myGLResources.getPrimitives (gl, robj);
      maybeUpdateState(gl); 
      updateProgram (gl, RenderingMode.DEFAULT, robj.hasNormals (), 
         robj.hasColors (), robj.hasTextureCoords ());
      
      // fill element buffer
      IndexBufferPutter putter = IndexBufferPutter.getDefault ();
      ByteBuffer buff = BufferUtilities.newNativeByteBuffer (idxs.length*putter.bytesPerIndex ());
      putter.putIndices (buff, idxs, 0, 1, idxs.length);
      buff.flip ();
      eaFlex.fill (gl, buff, GL.GL_UNSIGNED_INT, GLSupport.INTEGER_SIZE,
         idxs.length, buff.limit (), GL3.GL_STREAM_DRAW);
      buff = BufferUtilities.freeDirectBuffer (buff);
      
      VertexArrayObject.bindDefault (gl);
      gro.bindVertices (gl);
      eaFlex.bind (gl);
      
      gro.drawElements (gl, getDrawPrimitive (mode), eaFlex.count (), eaFlex.type (), 0);

      gro.unbindVertices(gl);
      eaFlex.unbind (gl);
      
   };

   @Override
   public void drawLines(RenderObject robj, int gidx, int offset, int count,
      LineStyle style, double rad) {

      GL3RenderObjectLines gro = myRenderObjectManager.getLines (gl, robj);

      maybeUpdateState(gl);

      switch (style) {
         case LINE: {
            // maybe change point size and draw points
            setLineWidth (gl, (float)rad);
            updateProgram (gl, RenderingMode.DEFAULT, robj.hasNormals (), robj.hasColors (), robj.hasTextureCoords ());
            gro.drawLineGroup (gl, GL.GL_LINES, gidx, offset, count);
            break;
         }
         default: {

            myProgramInfo.setLineScaleOffsetEnabled (true);
            updateProgram (gl, RenderingMode.INSTANCED_LINES, true, robj.hasColors (), false);
            myProgramInfo.setLineScaleOffsetEnabled (false);
            
            switch (style) {
               case CYLINDER: {
                  GL3Primitive primitive = getPrimitive (gl, PrimitiveType.CYLINDER);
                  gro.setRadius (gl, (float)rad);
                  gro.drawInstancedLineGroup (gl, primitive, gidx, offset, count);
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
                  gro.drawInstancedLineGroup (gl, cylinder, gidx, offset, count);
                  gro.setRadiusOffsets (gl, arrowRad, coneBoundary, null);
                  gro.drawInstancedLineGroup (gl, cone, gidx, offset, count);
                  break;
               }
               case SPINDLE: {
                  gro.setRadius (gl, (float)rad);
                  GL3Primitive spindle = getPrimitive (gl, PrimitiveType.SPINDLE);
                  gro.drawInstancedLineGroup (gl, spindle, gidx, offset, count);
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
   public void drawPoints(RenderObject robj, int gidx, int offset, int count,
      PointStyle style, double rad) {

      GL3RenderObjectPoints gro = myRenderObjectManager.getPoints (gl, robj);

      maybeUpdateState (gl);

      switch (style) {
         case POINT: {
            // maybe change point size and draw points
            float fold = getPointSize();
            float frad = (float)rad;
            boolean changed = false;
            
            if (fold != frad) {
               setPointSize (gl, frad);
            }
            updateProgram (gl, RenderingMode.POINTS, robj.hasNormals (), 
               robj.hasColors (), robj.hasTextureCoords ());
            
            gro.drawPointGroup (gl, GL.GL_POINTS, gidx, offset, count);

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
            updateProgram (gl, RenderingMode.INSTANCED_POINTS, true, 
               robj.hasColors (), robj.hasTextureCoords ());
            gro.drawInstancedPointGroup (gl, point, gidx, offset, count);
            break;
         }
      }
   }
   
   /**
    * Draw an object multiple times with differing per-instance information
    * @param robj render object
    * @param rinst render instance info
    */
   public void drawInstances(RenderObject robj, RenderInstances rinst) {
      drawInstances(robj, robj.getPointGroupIdx(), 
         robj.getLineGroupIdx(), robj.getTriangleGroupIdx(), rinst);
   }
   
   /**
    * Draw instances of the supplied render object
    * @param robj render object to draw
    * @param pidx point group to draw (-1 to ignore)
    * @param lidx line group to draw (-1 to ignore)
    * @param tidx triangle group to draw (-1 to ignore)
    * @param rinst instances
    */
   protected void drawInstances(RenderObject robj, int pidx, int lidx, int tidx,
      RenderInstances rinst) {
      maybeUpdateState(gl);
      
      GL3RenderInstances glinst = myRenderObjectManager.getInstances(gl, rinst);
      GL3SharedRenderObjectPrimitives grop = myRenderObjectManager.getSharedPrimitives(
         gl, robj);
      
      GL3SharedRenderObjectPrimitivesDrawable wrapped = 
         new GL3SharedRenderObjectPrimitivesDrawable(grop);
      wrapped.setDrawGroups(pidx, lidx, tidx);
      
      if (glinst.hasPoints()) {
         updateProgram(gl, RenderingMode.INSTANCED_POINTS, robj.hasNormals(),
            rinst.hasColors(), false);
         glinst.drawPoints(gl, wrapped);
      }
      if (glinst.hasFrames()) {
         updateProgram(gl, RenderingMode.INSTANCED_FRAMES, robj.hasNormals(),
            rinst.hasColors(), false);
         glinst.drawFrames(gl, wrapped);
      }
      if (glinst.hasAffines()) {
         updateProgram(gl, RenderingMode.INSTANCED_AFFINES, robj.hasNormals(),
            rinst.hasColors(), false);
         glinst.drawAffines(gl, wrapped);
      }
   }
   
   /**
    * Draw an object multiple times with differing per-instance information
    * @param robj render object
    * @param gidx point group index
    * @param rinst render instance info
    */
   public void drawPoints(RenderObject robj, int gidx, RenderInstances rinst) {
      drawInstances(robj, gidx, -1, -1, rinst);
   }

   /**
    * Draw an object multiple times with differing per-instance information
    * @param robj render object
    * @param gidx line group index
    * @param rinst render instance info
    */
   public void drawLines(RenderObject robj, int gidx, RenderInstances rinst) {
      drawInstances(robj, -1, gidx, -1, rinst);
   }
   
   /**
    * Draw an object multiple times with differing per-instance information
    * @param robj render object
    * @param gidx triangle group index
    * @param rinst render instances
    */
   public void drawTriangles(RenderObject robj, int gidx, RenderInstances rinst) {
      drawInstances(robj, -1, -1, gidx, rinst);
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

   public boolean hasColorMapMixing (ColorMixing cmix) {
      return true;
   }
   public boolean hasColorMapping() {
      return true;
   }
   
   public boolean hasNormalMapping() {
      return true;
   }
   
   public boolean hasBumpMapping() {
      return true;
   }
}
