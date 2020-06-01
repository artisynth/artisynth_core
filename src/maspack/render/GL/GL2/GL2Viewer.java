/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC),
 * and ArtiSynth Team Members. Elliptic selection added by Doga Tekin (ETH).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL.GL2;

import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.LinkedList;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.glu.GLU;
import javax.swing.JFrame;
import javax.swing.event.MouseInputListener;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.render.ColorMapProps;
import maspack.render.Dragger3d;
import maspack.render.Light;
import maspack.render.Light.LightSpace;
import maspack.render.Light.LightType;
import maspack.render.Material;
import maspack.render.RenderInstances;
import maspack.render.RenderInstances.InstanceTransformType;
import maspack.render.RenderInstances.RenderInstancesVersion;
import maspack.render.RenderKey;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectIdentifier;
import maspack.render.RenderObject.RenderObjectVersion;
import maspack.render.RenderProps;
import maspack.render.TextureContent;
import maspack.render.VertexIndexArray;
import maspack.render.GL.GLClipPlane;
import maspack.render.GL.GLDrawableComponent;
import maspack.render.GL.GLFrameCapture;
import maspack.render.GL.GLGridPlane;
import maspack.render.GL.GLLightManager;
import maspack.render.GL.GLMouseAdapter;
import maspack.render.GL.GLSupport;
import maspack.render.GL.GLTextRenderer;
import maspack.render.GL.GLTexture;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GL2.GL2Primitive.PrimitiveType;
import maspack.render.GL.GL2.RenderObjectKey.DrawType;
import maspack.render.color.ColorUtils;
import maspack.util.BooleanHolder;
import maspack.util.InternalErrorException;
import maspack.util.Logger;

/**
 * @author John E Lloyd and ArtiSynth team members
 */
public class GL2Viewer extends GLViewer implements HasProperties {

   // must have at least this many vertices to quality for a display list
   private static final int DISPLAY_LIST_VERTEX_MINIMUM = 100;

   public static boolean DEBUG = false;

   //   public enum AxialView {
   //      POS_X_POS_Z, NEG_X_POS_Z, POS_X_POS_Y, 
   //      POS_X_NEG_Y, POS_Y_POS_Z, NEG_Y_POS_Z
   //   }
   
   protected boolean myShadingModified = true;

   protected static boolean myUseGLSelectSelection = false;

   private GL2 gl;
   private GLU glu;
   private GL2SharedResources myGLResources;
   private GLTextRenderer myTextRenderer;
   private ColorMapProps myTextTextureProps = null;
   
   // basic primitives
   private GL2Primitive[] primitives;

   private RigidTransform3d Xtmp = new RigidTransform3d();
   private Vector3d utmp = new Vector3d();
   private Vector3d vtmp = new Vector3d();
   private float[] ctmp = new float[3];
   // buffers to store certain line styles
   double[] cosBuff = {1, 0, -1, 0, 1};
   double[] sinBuff = {0, 1, 0, -1, 0};
   private static double[] GLMatrix = new double[16];
   
   private volatile GLFrameCapture frameCapture = null;
   private volatile boolean grab = false;
   private volatile boolean grabClose = false;

   // Lighting parameters
   protected float lmodel_ambient[] = { 0.0f, 0.0f, 0.0f, 0.0f };
   protected float lmodel_twoside[] = { 0.0f, 0.0f, 0.0f, 0.0f };
   protected float lmodel_local[] = { 0.0f, 0.0f, 0.0f, 0.0f };

   private float[] scalefv(float[] c, float s, float[] out) {
      out[0] = c[0]*s;
      out[1] = c[1]*s;
      out[2] = c[2]*s;
      out[3] = c[3]*s;
      return out;
   }
 
   private void setupLight (GL2 gl, Light light, float intensityScale) {

      int lightId = light.getId() + GL2.GL_LIGHT0;
      if (light.isEnabled()) {
         gl.glEnable(lightId);
      } else {
         gl.glDisable(lightId);
      }

      if (light.getLightSpace() == LightSpace.CAMERA) {
         gl.glPushMatrix();
         gl.glLoadIdentity();
      }

      float[] tmp = new float[4]; 
      if (light.getType() == LightType.DIRECTIONAL) {
         float[] dir = light.getDirection();
         // negate, since OpenGL expects direction *to* light
         tmp[0] = -dir[0];
         tmp[1] = -dir[1];
         tmp[2] = -dir[2];
         tmp[3] = 0;
         gl.glLightfv (lightId, GL2.GL_POSITION, tmp, 0);
      } else {
         gl.glLightfv (lightId, GL2.GL_POSITION, light.getPosition(), 0);
      }

      gl.glLightfv (lightId, GL2.GL_DIFFUSE, scalefv(light.getDiffuse(),intensityScale,tmp), 0);
      gl.glLightfv (lightId, GL2.GL_AMBIENT, scalefv(light.getAmbient(),intensityScale,tmp), 0);
      gl.glLightfv (lightId, GL2.GL_SPECULAR, scalefv(light.getSpecular(),intensityScale,tmp), 0);

      switch (light.getType()) {
         case DIRECTIONAL:
         case POINT:
            gl.glLighti(lightId, GL2.GL_SPOT_CUTOFF, 180); // special value disabling spot
            break;
         case SPOT:
            float deg = Math.min ((float)Math.toDegrees (light.getSpotCutoff()), 90);
            gl.glLightf(lightId, GL2.GL_SPOT_CUTOFF, deg);
            float exp = Math.min (light.getSpotExponent(), 128);
            gl.glLightf(lightId, GL2.GL_SPOT_EXPONENT, exp);
            gl.glLightfv(lightId, GL2.GL_SPOT_DIRECTION, light.getDirection(), 0);
            break;
      }
      gl.glLightf(lightId, GL2.GL_CONSTANT_ATTENUATION, light.getConstantAttenuation());
      gl.glLightf(lightId, GL2.GL_LINEAR_ATTENUATION, light.getLinearAttenuation());
      gl.glLightf(lightId, GL2.GL_QUADRATIC_ATTENUATION, light.getQuadraticAttenuation());

      if (light.getLightSpace() == LightSpace.CAMERA) {
         gl.glPopMatrix();
      }
   }

   protected int getMaxLights(GL2 gl) {
      int[] buff = new int[1];
      gl.glGetIntegerv(GL2.GL_MAX_LIGHTS, buff, 0);
      return buff[0];
   }

   protected void setupLights(GL2 gl) {

      maybeUpdateMatrices (gl);
      
      int maxLights = lightManager.maxLights();
      float intensityScale = 1.0f/lightManager.getMaxIntensity();
      // only enable up to maxLights
      for (Light light : lightManager.getLights()) {
         if (light.getId() < maxLights) {
            setupLight(gl, light, intensityScale);
         }
      }
   }

   public GL getGL() {
      return drawable.getGL().getGL2();
   }

   // end of the rotation code

   /**
    * Creates a new GLViewer with default capabilities.
    * 
    * @param width
    * initial width of the viewer
    * @param height
    * initial height of the viewer
    */
   public GL2Viewer (int width, int height) {
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
   public GL2Viewer (GL2Viewer shareWith, int width, int height) {
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
   public GL2Viewer (GLCapabilities cap, GL2SharedResources resources, int width,
      int height) {

      if (cap == null) {
         GLProfile glp2 = GLProfile.get(GLProfile.GL2);
         cap = new GLCapabilities(glp2);
         cap.setSampleBuffers (true);
         cap.setNumSamples (8);

      }

      if (resources == null) {
         resources = new GL2SharedResources(cap);
      }
      myGLResources = resources;
      myGLResources.registerViewer (this);
      if (useGLJPanel) {
         Logger.getSystemLogger().debug("Using GLJPanel");
         canvas = GLDrawableComponent.create(myGLResources.createPanel());
      } else {
         canvas = GLDrawableComponent.create(myGLResources.createCanvas());
      }
      canvas.setSurfaceScale(new float[]{ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE});
      
      primitives = new GL2Primitive[PrimitiveType.values ().length];

      canvas.addGLEventListener (this);
      // canvas.setPreferredSize(new Dimension(width, height));
      canvas.setSize (width, height);

      this.width = width;
      this.height = height;
      myDraggers = new LinkedList<Dragger3d>();
      myUserDraggers = new LinkedList<Dragger3d>();

      lightManager = new GLLightManager();
      setDefaultLights();

      myGrid = new GLGridPlane();
      myGrid.setViewer (this);

      RigidTransform3d EyeToWorld = new RigidTransform3d (0, -3, 0, 1, 0, 0, Math.PI / 2);
      setEyeToWorld(EyeToWorld);
      setAxialView (myAxialView);

      myMouseHandler = new GLMouseAdapter (this);

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

      buildInternalRenderList();
   }
  
   @Override
   public void init(GLAutoDrawable drawable) {
      super.init (drawable);
      
      this.drawable = drawable;
      this.gl = drawable.getGL().getGL2();

      if (DEBUG) {
         Logger.getSystemLogger().debug("GL: " + gl);
         Logger.getSystemLogger().debug("Dev id : ");
         GLContext context = drawable.getContext ();
         String contextHC = Integer.toHexString(System.identityHashCode(context));
         Logger.getSystemLogger().debug("Context: " + context.getClass ().getName () + "@" + contextHC + " (shared=" + context.isShared () + ")");
      }

      gl.setSwapInterval (1);

      int[] buff = new int[1];
      gl.glGetIntegerv(GL2.GL_MAX_CLIP_PLANES, buff, 0);
      maxClipPlanes = buff[0];

      setFaceStyle (FaceStyle.FRONT);
      gl.glEnable (GL2.GL_DEPTH_TEST);
      gl.glClearDepth (1.0);

      gl.glLightModelfv (GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, lmodel_local, 0);
      gl.glLightModelfv (GL2.GL_LIGHT_MODEL_TWO_SIDE, lmodel_twoside, 0);
      gl.glLightModelfv (GL2.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
      gl.glLightModelf (GL2.GL_LIGHT_MODEL_TWO_SIDE, 1);
      gl.glEnable (GL2.GL_LIGHTING);

      gl.glEnable (GL2.GL_NORMALIZE);  // normalize normals
      
      myTextRenderer = GLTextRenderer.generate (gl, GL2PipelineRenderer.generate (gl));
      myTextTextureProps = new ColorMapProps ();
      myTextTextureProps.setColorMixing (ColorMixing.MODULATE);
      myTextTextureProps.setEnabled (true);

      setLightingEnabled(true);
      setDepthEnabled(true);
      setColorEnabled(true);
      setVertexColoringEnabled(true);
      setTextureMappingEnabled(true);
      setFaceStyle(FaceStyle.FRONT);
      setShading(Shading.SMOOTH);
      setGammaCorrectionEnabled(false);

      lightManager.setMaxLights(getMaxLights(gl));
      setupLights(gl);

      // gl.glFrontFace (GL2.GL_CW);
      if (!isSelecting()) {
         gl.glClearColor (backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
      }

      // initialize viewport
      resetViewVolume(gl);
      invalidateModelMatrix();
      invalidateProjectionMatrix();
      invalidateViewMatrix();   

      // trigger rebuild of renderables
      buildInternalRenderList();

      if (DEBUG) {
         Logger.getSystemLogger().debug("GL2 initialized");
      }
      
   }
   
   @Override
   public int setSurfaceResolution (int nres) {
      int oldres = getSurfaceResolution ();
      if (oldres != nres) {
         for (int i=0; i<primitives.length; ++i) {
            GL2Primitive p = primitives[i];
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
    *
    * @return primitive
    */
   public GL2Primitive getPrimitive(GL2 gl, PrimitiveType type) {
      int pid = type.ordinal ();
      GL2Primitive primitive = primitives[pid];
      
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
            primitive = myGLResources.getAcquiredCube (gl);
            break;
         case CONE:
            primitive = myGLResources.getAcquiredCone(gl, resolution, true);
            break;
         case CYLINDER:
            primitive = myGLResources.getAcquiredCylinder(gl, resolution, true);
            break;
         case SPHERE:
            primitive = myGLResources.getAcquiredSphere(gl, resolution);
            break;
         case SPINDLE:
            primitive = myGLResources.getAcquiredSpindle(gl, resolution);
            break;
      }
      
      primitives[pid] = primitive;
      return primitive;
   }

   @Override
   public void dispose(GLAutoDrawable drawable) {
      super.dispose (drawable);
      
      this.drawable = drawable;
      this.gl = drawable.getGL ().getGL2 ();
      
      if (this.primitives != null) {
         for (int i=0; i<primitives.length; ++i) {
            GL2Primitive prim = primitives[i];
            if (prim != null) {
               prim.releaseDispose (gl);
               primitives[i] = null;
            }
         }
      }
      
      myTextRenderer.dispose (gl);
      myTextRenderer = null;

      if (DEBUG) {
         Logger.getSystemLogger().debug("GL2 disposed");
      }

      // nullify stuff
      this.drawable = null;
      this.gl = null;
   }

   @Override
   public void dispose () {
      super.dispose ();
      myGLResources.deregisterViewer (this);
   }

   protected void drawDragBox (GL2 gl) {
      
      setColor(0.5f, 0.5f, 0.5f, 1.0f);
      begin2DRendering(-1, 1,-1, 1);
      
      double x0 = 2 * myDragBox.x / (double)width - 1;
      double x1 = x0 + 2 * myDragBox.width / (double)width;
      double y0 = 1 - 2 * myDragBox.y / (double)height;
      double y1 = y0 - 2 * myDragBox.height / (double)height;

      maybeUpdateState (gl);
      
      gl.glBegin (GL2.GL_LINE_LOOP);
      gl.glVertex3d (x0, y0, 0);
      gl.glVertex3d (x1, y0, 0);
      gl.glVertex3d (x1, y1, 0);
      gl.glVertex3d (x0, y1, 0);
      gl.glEnd();
      
      end2DRendering ();
      
   }

   protected void drawEllipticCursor (GL2 gl, Point cursor) {
      begin2DRendering(0, width, 0, height);
      
      float a = (float)myEllipticCursorSize.x;
      float b = (float)myEllipticCursorSize.y;
      float cx = (float)cursor.getX();
      float cy = (float)(height - cursor.getY());
      
      // change to a smaller/bigger number as needed 
      int num_segments = (int)(4*Math.ceil(Math.max(a,b)*Math.PI/2));

      gl.glLineWidth (2);     
      setColor(0.5f, 0.5f, 0.5f);

      maybeUpdateState(gl);

      gl.glBegin (GL2.GL_LINE_LOOP);
      for(int i = 0; i < num_segments; i++) {
         double ang = i*2*Math.PI/(double)num_segments;
         float x = a*(float)Math.cos(ang);
         float y = b*(float)Math.sin(ang);
         gl.glVertex3f (x + cx, y + cy, 0); //output vertex 
      } 
      gl.glEnd();
      gl.glLineWidth (1);      

      end2DRendering();
   }

   public void display (GLAutoDrawable drawable, int flags) {
      
      this.drawable = drawable;
      this.gl = drawable.getGL ().getGL2 ();
      
      if (!myInternalRenderListValid) {
         buildInternalRenderList();
      }

      maybeUpdateState(gl);

      if (selectTrigger) {
         mySelector.setupSelection (gl);
         selectEnabled = true;  // moved until after selection initialization
         selectTrigger = false;
      }

      // turn off buffer swapping when doing a selection render because
      // otherwise the previous buffer sometimes gets displayed
      drawable.setAutoSwapBufferMode (selectEnabled ? false : true);
      if (myProfiling) {
         myTimer.start();
         myGLColorCount = 0;
      }
      doDisplay (drawable, flags);
      if (myProfiling) {
         myTimer.stop();
         System.out.printf (
            "render time (msec): %9.4f %s\n", 
            myTimer.getTimeUsec()/1000.0, isSelecting() ? "(SELECT)" : "");
         Logger.getSystemLogger().debug("Color changes: " + myGLColorCount);
      }
      if (selectEnabled) {
         selectEnabled = false;
         mySelector.processSelection (gl);
      }
      else {
         fireRerenderListeners();
      }

      gl.glFlush();
      
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
      
      garbage (gl);
      
      this.drawable = null;
      this.gl = null;
   }
   
   @Override
   protected void garbage (GL gl) {
      super.garbage (gl);
      // for non-timed garbage collection
      myGLResources.maybeRunGarbageCollection (gl);
   }

   private void offscreenCapture (GLFrameCapture fc, int flags) {

      // Initialize the OpenGL context FOR THE FBO
      gl.setSwapInterval (1);

      // Set rendering commands to go to offscreen frame buffer
      fc.activateFBO(gl);

      // Draw the scene into pbuffer
      gl.glPushMatrix();

      // disable resetting of view volume during capture
      boolean autoResize = setAutoResizeEnabled (false);
      boolean autoViewport = setAutoViewportEnabled(false);
      doDisplay (drawable, flags);
      setAutoResizeEnabled(autoResize);
      setAutoViewportEnabled(autoViewport);

      fireRerenderListeners();
      
      gl.glPopMatrix();

      // further drawing will go to screen
      fc.deactivateFBO(gl);

      fc.capture(gl);
   }

   private void drawAxes (GL2 gl, double length) {
      double l = length;

      setLightingEnabled (false);

      // draw axis
      maybeUpdateState (gl);
      gl.glDepthFunc (GL2.GL_ALWAYS);

      if (!selectEnabled) {
         setGLColor (gl, 1f, 0f, 0f, 1f);
      }
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (l, 0.0, 0.0);
      gl.glVertex3d (0, 0.0, 0.0);
      gl.glEnd();

      if (!selectEnabled) {
         setGLColor (gl, 0f, 1f, 0f, 1f);
      }
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (0, l, 0.0);
      gl.glVertex3d (0, 0, 0.0);
      gl.glEnd();

      if (!selectEnabled) {
         setGLColor (gl, 0f, 0f, 1f, 1f);
      }
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (0, 0, l);
      gl.glVertex3d (0, 0, 0);
      gl.glEnd();

      gl.glDepthFunc (GL2.GL_LESS);

      setLightingEnabled (true);
   }
   
   public GLTextRenderer getTextRenderer() {
      return myTextRenderer;
   }
   
   JFrame frame;
   
   public double drawText(Font font, String str, float[] pos, double emSize) {
      
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
      setBlendSourceFactor (BlendFactor.GL_SRC_ALPHA);
      setBlendDestFactor (BlendFactor.GL_ONE_MINUS_SRC_ALPHA);
      setTextureMappingEnabled (true);
     
      ColorMapProps savedTextureProps = setColorMap (myTextTextureProps);
      
      maybeUpdateState(gl);

      activateTexture (gl, myTextRenderer.getTexture ());
      myTextRenderer.begin (gl);
      double d = myTextRenderer.drawText (font, str, pos, (float)emSize);
      myTextRenderer.end (gl);
      deactivateTexture (gl);
      
      setDepthWriteEnabled (savedDepth);
      setBlendSourceFactor (sfactor);
      setBlendDestFactor (dfactor);
      setBlendingEnabled (savedBlending);
      setTextureMappingEnabled (savedTexture);
      setColorMap (savedTextureProps);
      
      // GLSupport.showTexture (gl, GL.GL_TEXTURE_2D, 0);
      
      return d;
   }

   // should be protected, not public;; this is for debugging
   protected boolean isLightingOn() {
      return gl.glIsEnabled (GL2.GL_LIGHTING);
   }
   
   // should be protected, not public;; this is for debugging
   public int getGLShadeModel() {
      int[] buff = new int[1];
      gl.glGetIntegerv(GL2.GL_SHADE_MODEL, buff, 0);
      return buff[0];
   }
   
   protected void setLightingOn (boolean set) {
      if (!selectEnabled) {
         if (set) {
            gl.glEnable(GL2.GL_LIGHTING);
         } 
         else {
            gl.glDisable(GL2.GL_LIGHTING);
         }      
      }
   }
//   public boolean isLightingEnabled() {
//      return gl.glIsEnabled (GL2.GL_LIGHTING);
//   }
   
   private void doDisplay (GLAutoDrawable drawable, int flags) {
      GL2 gl = drawable.getGL().getGL2();

      // updates view matrix
      if (resetViewVolume && autoResizeEnabled) {
         resetViewVolume(gl);
         resetViewVolume = false;
      }
      
      if (resetViewport && autoViewportEnabled) {
         setViewport (gl, 0, 0, width, height);
         resetViewport = false;
      }

      gl.glClearColor (backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
      gl.glClear (GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

      // updates view matrix
      //      RigidTransform3d X = new RigidTransform3d();
      //      X.invert (XEyeToWorld);

      // enter view matrix
      // GLSupport.transformToGLMatrix (GLMatrix, X);
      //      GLSupport.transformToGLMatrix (GLMatrix, viewMatrix);
      //      gl.glLoadMatrixd(GLMatrix, 0);
      //      viewMatrixValidP = true;  // view matrix now "committed"

      maybeUpdateState (gl); // update all state, including matrices
      setupLights(gl);

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

      // enable clip planes
      maybeUpdateState (gl); // update matrices before activating clip planes
      int nclips = 0;
      int clipIdx = GL2.GL_CLIP_PLANE0;
      for (GLClipPlane cp : myClipPlanes) {
         if (cp.isClippingEnabled()) {
            cp.getPlaneValues (myClipPlaneValues );
            myClipPlaneValues[3] += cp.getOffset();
            
            gl.glClipPlane (clipIdx, myClipPlaneValues, 0);
            gl.glEnable (clipIdx);
            clipIdx++; nclips++;
            if (nclips >= maxClipPlanes) {
               break;
            }

            if (cp.isSlicingEnabled()) {
               myClipPlaneValues[0] = -myClipPlaneValues[0];
               myClipPlaneValues[1] = -myClipPlaneValues[1];
               myClipPlaneValues[2] = -myClipPlaneValues[2];
               myClipPlaneValues[3] =
               -myClipPlaneValues[3]+2*cp.getOffset ();   

               gl.glClipPlane (clipIdx, myClipPlaneValues, 0);
               gl.glEnable (clipIdx);
               clipIdx++;
               nclips++;

               if (nclips >= maxClipPlanes) {
                  break;
               }
            }
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
         synchronized (elist) {
            qid = elist.renderOpaque (this, qid, flags);
         }
      }
      
      if (hasTransparent3d ()) {
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
         
         if (!isSelecting ()) {
            disableTransparency ();
         }
      }

      // disable clipping planes
      for (int i=GL2.GL_CLIP_PLANE0; i<clipIdx; ++i) {
         gl.glDisable(i);
      }

      // Draw 2D objects
      if (has2d ()) {
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

            if (hasTransparent2d ()) {
               if (!isSelecting()) {
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
      }

      // trigger update of state (required for GLJPanel, which relies on 
      //                          transparency to be off)
      maybeUpdateState(gl);
      
      gl.glFlush();
   }

   public static int getNameStackDepth (GL2 gl) {
      int[] depth = new int[1];
      gl.glGetIntegerv (GL2.GL_NAME_STACK_DEPTH, depth, 0);
      return depth[0];
   }

   /**
    * Potentially update GL state (matrices, lights, materials, etc...)
    * @param gl
    */
   protected void maybeUpdateState(GL2 gl) {
      maybeUpdateMatrices (gl);
      maybeUpdateMaterials(gl);
      maybeUpdateViewerState(gl);
   }
   
   protected boolean maybeActivateTextures(GL2 gl) {
      
      boolean activate = false;
      // maybe use texture?
      GLTexture tex = null;
      if (!isSelecting () && myColorMapProps != null && 
          myColorMapProps.isEnabled() && 
          myColorMapProps.getColorMixing() != ColorMixing.NONE) {
         TextureContent content = myColorMapProps.getContent ();
         if (content != null) {
            tex = myGLResources.getOrLoadTexture (gl, content);
            if (tex != null) {
               activateTexture (gl, tex);
               activate = true;
            }
         }
      }
     
      return activate;
   }
   
   protected void activateTexture(GL2 gl, GLTexture tex) {
      gl.glEnable(GL.GL_TEXTURE_2D);
      gl.glActiveTexture (GL.GL_TEXTURE0);
      gl.glTexEnvi (
         GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, getTextureMode (myColorMapProps));
      tex.bind(gl);
   }
   
   protected void deactivateTexture(GL2 gl) {
      gl.glDisable(GL.GL_TEXTURE_2D);
   }

   @Override
   protected void maybeCommitViewerState (GL2GL3 gl, ViewerState state) {
      super.maybeCommitViewerState (gl, state);
      
      GL2 gl2 = (GL2)gl;
      
      if (isSelecting ()) {
         // disable lighting
         if (myCommittedViewerState.lightingEnabled == true) {
            gl.glDisable (GL2.GL_LIGHTING);
            myCommittedViewerState.lightingEnabled = false;
            myCommittedViewerState.shading = Shading.NONE;
         }
         
      } else {
         // track lighting
         if (myCommittedViewerState.lightingEnabled != state.lightingEnabled) {
            if (state.lightingEnabled && state.shading != Shading.NONE) {
               gl.glEnable (GL2.GL_LIGHTING);
               myCommittedViewerState.lightingEnabled = true;
            } else {
               gl.glDisable (GL2.GL_LIGHTING);
               myCommittedViewerState.shading = Shading.NONE;
               myCommittedViewerState.lightingEnabled = false;
            }

         }
      }
      
      // if lighting is enabled, maybe update shading
      if (myCommittedViewerState.lightingEnabled && 
         myCommittedViewerState.shading != state.shading) {
         switch(state.shading) {
            case FLAT:
               gl2.glShadeModel (GL2.GL_FLAT);
               break;
            case SMOOTH:
            case METAL:
               gl2.glShadeModel (GL2.GL_SMOOTH);
               break;
            case NONE:
               gl.glDisable (GL2.GL_LIGHTING);
               gl2.glShadeModel (GL2.GL_SMOOTH);
               myCommittedViewerState.lightingEnabled = false;
               break;
            default:
               break;
         }
         myCommittedViewerState.shading = state.shading;
      }
      
      // rounded points
      if (myCommittedViewerState.roundedPoints != state.roundedPoints) {
         if (state.roundedPoints && state.pointSize >= 4) {
            gl.glEnable (GL2.GL_POINT_SMOOTH);  // enable smooth points
            gl.glHint(GL2.GL_POINT_SMOOTH_HINT, GL2.GL_NICEST);
            myCommittedViewerState.roundedPoints = true;
         } else {
            gl.glDisable (GL2.GL_POINT_SMOOTH);  // disable smooth points
            gl.glHint(GL2.GL_POINT_SMOOTH_HINT, GL2.GL_FASTEST);
            myCommittedViewerState.roundedPoints = false;
         }
         
      }
   }
   
   @Override
   protected void commitFullViewerState (GL2GL3 gl, ViewerState state) {
      super.commitFullViewerState (gl, state);
      
      GL2 gl2 = (GL2)gl;
      if (isSelecting ()) {
         // if selecting, disable lighting and blending         
         gl2.glDisable (GL2.GL_LIGHTING);
         myCommittedViewerState.lightingEnabled = false;
      } else {
         if (state.lightingEnabled) {
            gl.glEnable (GL2.GL_LIGHTING);
         } else {
            gl.glDisable (GL2.GL_LIGHTING);
         }
      }
      
      switch(state.shading) {
         case FLAT:
            gl2.glShadeModel (GL2.GL_FLAT);
            break;
         case SMOOTH:
         case METAL:
            gl2.glShadeModel (GL2.GL_SMOOTH);
            break;
         case NONE:
            gl2.glDisable (GL2.GL_LIGHTING);
            myCommittedViewerState.lightingEnabled = false;
            break;
         default:
            break;
      }
      
      if (state.roundedPoints && state.pointSize >= 4) {
         gl.glEnable (GL2.GL_POINT_SMOOTH);  // enable smooth points
         gl.glHint(GL2.GL_POINT_SMOOTH_HINT, GL2.GL_NICEST);
         myCommittedViewerState.roundedPoints = true;
      } else {
         gl.glDisable (GL2.GL_POINT_SMOOTH);  // disable smooth points
         gl.glHint(GL2.GL_POINT_SMOOTH_HINT, GL2.GL_FASTEST);
         myCommittedViewerState.roundedPoints = false;
      }
   }
   
   public void maybeUpdateMaterials() {
      maybeUpdateMaterials(gl);
   }

   private static void applyMaterial(GL2 gl, int sides, int target, 
      float[] v, float scale) {
      
      float[] m = new float[4];
      for (int i=0; i<3; ++i) {
         m[i] = v[i]*scale;
      }
      m[3] = v[3];
      gl.glMaterialfv(sides, target, m, 0);
   }
    
   private static void applyMaterial(GL2 gl, 
      int sides, Material mat, float[] diffuseOverride) {

      float[] diffuse = mat.getDiffuse();
      float[] power = mat.getPower();
      
      applyMaterial(gl, sides, GL2.GL_EMISSION, mat.getEmission(), power[3]);
      applyMaterial(gl, sides, GL2.GL_SPECULAR, mat.getSpecular(), power[2]);
      gl.glMaterialf (sides, GL2.GL_SHININESS, mat.getShininess());
      if (diffuseOverride != null) {
         float[] temp = new float[4];
         temp[0] = diffuseOverride[0];
         temp[1] = diffuseOverride[1];
         temp[2] = diffuseOverride[2];
         temp[3] = diffuse[3];
         diffuse = temp;
      }
      applyMaterial(gl, sides, GL2.GL_DIFFUSE, diffuse, power[1]);
      applyMaterial(gl, sides, GL2.GL_AMBIENT, diffuse, power[0]);
   }
   
   public void maybeUpdateMaterials(GL2 gl) {

      // might need to update underlying material
      if (myCurrentMaterialModified) {
         applyMaterial(gl, GL2.GL_FRONT_AND_BACK, myCurrentMaterial, null);
         if (myBackColor != null) {
            gl.glMaterialfv (GL2.GL_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, myBackColor, 0); // apply back color
         }
         float[] c = myCurrentMaterial.getDiffuse ();
         setGLColor (gl, c[0], c[1], c[2], c[3]);
         myCommittedColor = ActiveColor.DEFAULT;
         myCurrentMaterialModified = false;
      }
      
      // update colors
      if (isSelecting ()) {
         if (mySelectingColorModified || (myCommittedColor != ActiveColor.SELECTING) ) {
            // update selection color
            // gl.glMaterialfv (GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, mySelectingColor, 0);   // apply front/back color
            float[] c = mySelectingColor;
            setGLColor (gl, c[0], c[1], c[2], c[3]);
            mySelectingColorModified = false;
            myCommittedColor = ActiveColor.SELECTING;   
         }
         
      } else {
         
         // highlighting 
         switch (myActiveColor) {
            case DEFAULT: {
               if (myCommittedColor != ActiveColor.DEFAULT) {
                  float[] c = myCurrentMaterial.getDiffuse ();
                  setGLColor (gl, c[0], c[1], c[2], c[3]);
                  if (myBackColor != null) {
                     gl.glMaterialfv (GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, myCurrentMaterial.getDiffuse (), 0);   // apply front/back color
                     gl.glMaterialfv (GL2.GL_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, myBackColor, 0); // apply back color
                  } else {
                     gl.glMaterialfv (GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, myCurrentMaterial.getDiffuse (), 0);   // apply front/back color
                  }
                  myCommittedColor = myActiveColor;
               }
               break;
            }
            
            case HIGHLIGHT: {
               if (myCurrentMaterial.getAlpha () != myHighlightColor[3]) {
                  myHighlightColor[3] = myCurrentMaterial.getAlpha ();
                  myHighlightColorModified = true;
               }
               
               if (myHighlightColorModified || myCommittedColor != ActiveColor.HIGHLIGHT) {
                  gl.glMaterialfv (GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, myHighlightColor, 0);   // apply front/back color
                  float[] c = myHighlightColor;
                  setGLColor (gl, c[0], c[1], c[2], c[3]);
                  myHighlightColorModified = false;
                  myCommittedColor = myActiveColor;
               }
               break;
            }
            default:
               break;
            
         }         
      }
      
   }

   // Made public for debugging purposes
   public void maybeUpdateMatrices(GL2 gl) {

      //      int[] mmode = new int[1]; 
      //      gl.glGetIntegerv(GL2.GL_MATRIX_MODE, mmode, 0);
      
      if (!viewMatrixValidP || !modelMatrixValidP) {
         // create modelview matrix:
         AffineTransform3d mvmatrix = new AffineTransform3d();
         mvmatrix.mul(viewMatrix, modelMatrix);

         // update modelview matrix
         GLSupport.transformToGLMatrix(GLMatrix, mvmatrix);
         gl.glMatrixMode (GL2.GL_MODELVIEW);
         gl.glLoadMatrixd(GLMatrix,0);

         viewMatrixValidP = true;
         modelMatrixValidP = true;
      }

      if (!projectionMatrixValidP) {

         // update projection matrix
         GLSupport.transformToGLMatrix(GLMatrix, projectionMatrix);

         gl.glMatrixMode(GL2.GL_PROJECTION);
         gl.glLoadMatrixd(GLMatrix,0);
         gl.glMatrixMode (GL2.GL_MODELVIEW);
         projectionMatrixValidP = true;
      }
      
      if (!textureMatrixValidP) {
         
         // update texture matrix
         GLSupport.transformToGLMatrix (GLMatrix, textureMatrix);
         gl.glMatrixMode(GL2.GL_TEXTURE);
         gl.glLoadMatrixd(GLMatrix,0);
         gl.glMatrixMode (GL2.GL_MODELVIEW);
         textureMatrixValidP = true;
      }

      // gl.glMatrixMode(mmode[0]); // revert
   }

   @Override
   public boolean popProjectionMatrix() {
      boolean success = super.popProjectionMatrix();
      maybeUpdateMatrices(gl);
      return success;
   }

   @Override
   public void setPickMatrix(float x, float y, float deltax, float deltay, int[] viewport) {
      super.setPickMatrix(x, y, deltax, deltay, viewport);
      maybeUpdateMatrices(gl);
   }

   @Override
   public void clearPickMatrix() {
      super.clearPickMatrix();
      maybeUpdateMatrices(gl);
   }

   public static void mulTransform (GL2 gl, RigidTransform3d X) {
      GLSupport.transformToGLMatrix (GLMatrix, X);
      gl.glMultMatrixd (GLMatrix, 0);
   }

   public static void mulTransform (GL2 gl, AffineTransform3d X) {
      GLSupport.transformToGLMatrix (GLMatrix, X);
      gl.glMultMatrixd (GLMatrix, 0);
   }

   public boolean isSelecting() {
      return selectEnabled;
   }

   //   public void setColor (float[] frontRgba, float[] backRgba, boolean selected) {
   //      if (!selectEnabled) {
   //         if (selected && myHighlighting == SelectionHighlighting.Color) {
   //            myDrawCurrentColor = mySelectedColor;
   //         }
   //         else if (frontRgba != null) {
   //            myDrawCurrentColor = frontRgba;
   //         }
   //         if (myDrawCurrentColor.length == 3) {
   //            gl.glColor3fv (myDrawCurrentColor, 0);
   //         }
   //         else {
   //            gl.glColor4fv (myDrawCurrentColor, 0);
   //         }
   //      }
   //   }
   //
   //   public void setColor (float[] frontRgba, float[] backRgba) {
   //      if (!selectEnabled) {
   //         if (frontRgba != null) {
   //            myDrawCurrentColor = frontRgba;
   //         }
   //         if (myDrawCurrentColor.length == 3) {
   //            gl.glColor3fv (myDrawCurrentColor, 0);
   //         }
   //         else {
   //            gl.glColor4fv (myDrawCurrentColor, 0);
   //         }
   //      }
   //   }
   //
   //   public void updateColor (float[] frontRgba, float[] backRgba, boolean selected) {
   //      if (!selectEnabled) {
   //         float[] c;
   //         if (selected && myHighlighting == SelectionHighlighting.Color) {
   //            c = mySelectedColor;
   //         }
   //         else {
   //            c = frontRgba;
   //         }
   //         if (myDrawCurrentColor != c) {
   //            myDrawCurrentColor = c;
   //            if (myDrawCurrentColor.length == 3) {
   //               gl.glColor3fv (myDrawCurrentColor, 0);
   //            }
   //            else {
   //               gl.glColor4fv (myDrawCurrentColor, 0);
   //            }
   //         }
   //      }
   //   }
   //
   //   public void setMaterial (
   //      Material frontMaterial, float[] frontDiffuse,
   //      Material backMaterial, float[] backDiffuse, boolean selected) {
   //      if (selected && myHighlighting == SelectionHighlighting.Color) {
   //         myCurrentFrontMaterial = mySelectedMaterial;
   //         myCurrentBackMaterial = mySelectedMaterial;
   //         myCurrentFrontDiffuse = null;
   //         myCurrentBackDiffuse = null;
   //         myCurrentFrontMaterial.apply (gl, GL.GL_FRONT_AND_BACK);
   //      }
   //      else {
   //         myCurrentFrontMaterial = frontMaterial;
   //         myCurrentFrontDiffuse = frontDiffuse;
   //         myCurrentBackMaterial = backMaterial;
   //         myCurrentBackDiffuse = backDiffuse;
   //         myCurrentFrontMaterial.apply (gl, GL.GL_FRONT, frontDiffuse);
   //         myCurrentBackMaterial.apply (gl, GL.GL_BACK, backDiffuse);
   //      }
   //   }
   //
   //   public void setMaterialAndShading (
   //      RenderProps props, Material frontMaterial, float[] frontDiffuse,
   //      Material backMaterial, float[] backDiffuse, boolean selected) {
   //
   //      if (selectEnabled) {
   //         return;
   //      }
   //      Shading shading = props.getShading();
   //      if (shading == Shading.NONE) {
   //         setLightingEnabled (false);
   //         myDrawCurrentColor = null; // ensure color gets set in updateMaterial
   //         updateMaterial (props, frontMaterial, frontDiffuse, 
   //            backMaterial, backDiffuse, selected);
   //      }
   //      else {
   //         if (shading != Shading.FLAT) {
   //            gl.glShadeModel (GL2.GL_SMOOTH);
   //         }
   //         myCurrentFrontMaterial = null; // ensure material gets set in updateMaterial
   //         myCurrentBackMaterial = null;
   //         myCurrentFrontDiffuse = null;
   //         myCurrentBackDiffuse = null;
   //         updateMaterial (props, frontMaterial, frontDiffuse, 
   //            backMaterial, backDiffuse, selected);
   //      }
   //   }
   //
//   public void restoreShading (RenderProps props) {
//      if (selectEnabled) {
//         return;
//      }
//      Shading shading = props.getShading();
//      if (shading == Shading.NONE) {
//         setShadeModel (DEFAULT_SHADING);
//      }
//      else if (shading != Shading.FLAT) {
//         setShadeModel (Shading.FLAT);
//      }      
//   }
   //
   //   @Override
   //   public void setShadeModel (Shading shading) {
   //
   //      if (selectEnabled) {
   //         return;
   //      }
   //
   //      super.setShadeModel (shading);
   //      if (shading == Shading.NONE) {
   //         setLightingEnabled (false);
   //      } else {
   //         setLightingEnabled(true);
   //         if (shading == Shading.FLAT) {
   //            gl.glShadeModel (GLLightingFunc.GL_FLAT);
   //         } else {
   //            gl.glShadeModel (GLLightingFunc.GL_SMOOTH);
   //         }
   //      }
   //   }
   //
   //   public void updateMaterial (
   //      RenderProps props, Material frontMaterial, float[] frontDiffuse, 
   //      Material backMaterial, float[] backDiffuse, boolean selected) {
   //
   //      if (selectEnabled) {
   //         return;
   //      }
   //      if (props.getShading() == Shading.NONE) {
   //         float[] cf;
   //         if (selected && myHighlighting == SelectionHighlighting.Color) {
   //            cf = mySelectedColor;
   //         }
   //         else if (frontDiffuse != null) {
   //            cf = new float[4];
   //            cf[0] = frontDiffuse[0];
   //            cf[1] = frontDiffuse[1];
   //            cf[2] = frontDiffuse[2];
   //            cf[3] = (float)props.getAlpha();
   //         }
   //         else {
   //            cf = frontMaterial.getDiffuse();
   //         }
   //         if (cf != myDrawCurrentColor) {
   //            myDrawCurrentColor = cf;
   //            if (myDrawCurrentColor.length == 3) {
   //               gl.glColor3fv (myDrawCurrentColor, 0);
   //            }
   //            else {
   //               gl.glColor4fv (myDrawCurrentColor, 0);
   //            }
   //         }
   //      }
   //      else {
   //         Material mf;
   //         float[] df;
   //         Material mb;
   //         float[] db;
   //         if (selected && myHighlighting == SelectionHighlighting.Color) {
   //            mf = mySelectedMaterial;
   //            df = null;
   //            mb = mySelectedMaterial;
   //            db = null;
   //         }
   //         else {
   //            mf = frontMaterial;
   //            df = frontDiffuse;
   //            mb = backMaterial;
   //            db = backDiffuse;
   //         }
   //         if (myCurrentFrontMaterial != mf || myCurrentFrontDiffuse != df) {
   //            myCurrentFrontMaterial = mf;
   //            myCurrentFrontDiffuse = df;
   //            myCurrentFrontMaterial.apply (gl, GL.GL_FRONT, df);
   //         }
   //         if (myCurrentBackMaterial != mb || myCurrentBackDiffuse != db) {
   //            myCurrentBackMaterial = mb;
   //            myCurrentBackDiffuse = db;
   //            myCurrentBackMaterial.apply (gl, GL.GL_BACK, db);
   //         }
   //      }
   //   }

   public void drawSphere (float[] pnt, double rad) {

      GL2 gl = getGL2();
      maybeUpdateState(gl);

      gl.glPushMatrix();
      gl.glTranslatef (pnt[0], pnt[1], pnt[2]);
      gl.glScaled (rad, rad, rad);

      GL2Primitive sphere = getPrimitive (gl, PrimitiveType.SPHERE);
      sphere.draw (gl);
      gl.glPopMatrix();
   }

   @Override
   public void drawCube (float[] pnt, double w) {
      GL2 gl = getGL2();
      maybeUpdateState(gl);

      gl.glPushMatrix();
      gl.glTranslatef (pnt[0], pnt[1], pnt[2]);
      double hw = w/2;
      gl.glScaled (hw, hw, hw);

      GL2Primitive cube = getPrimitive (gl, PrimitiveType.CUBE);
      cube.draw (gl);
      gl.glPopMatrix();
   }
   
   @Override
   public void drawBox (float[] pnt, double wx, double wy, double wz) {
      GL2 gl = getGL2();
      maybeUpdateState(gl);

      gl.glPushMatrix();
      gl.glTranslatef (pnt[0], pnt[1], pnt[2]);
      gl.glScaled (wx/2, wy/2, wz/2);

      GL2Primitive cube = getPrimitive (gl, PrimitiveType.CUBE);
      cube.draw (gl);
      gl.glPopMatrix();
   }
   
   @Override
   public void drawBox (RigidTransform3d TBM, Vector3d widths) {
      GL2 gl = getGL2();
      
      pushModelMatrix ();
      mulModelMatrix (TBM);
      scaleModelMatrix (widths.x/2, widths.y/2, widths.z/2);
      
      maybeUpdateState(gl);
      GL2Primitive cube = getPrimitive (gl, PrimitiveType.CUBE);
      cube.draw (gl);
      
      popModelMatrix ();
   }

//   public void drawHex (
//      RenderProps props, double scale,
//      float[] v0, float[] v1, float[] v2, float[] v3,
//      float[] v4, float[] v5, float[] v6, float[] v7) {
//
//      float cx = (v0[0]+v1[0]+v2[0]+v3[0]+v4[0]+v5[0]+v6[0]+v7[0])/8;
//      float cy = (v0[1]+v1[1]+v2[1]+v3[1]+v4[1]+v5[1]+v6[1]+v7[1])/8;
//      float cz = (v0[2]+v1[2]+v2[2]+v3[2]+v4[2]+v5[2]+v6[2]+v7[2])/8;
//
//      float s = (float)scale;
//      GL2 gl = getGL2();
//      maybeUpdateState(gl);
//
//      gl.glPushMatrix();
//      gl.glTranslatef (cx*(1-s), cy*(1-s), cz*(1-s));
//      gl.glScalef (s, s, s);
//
//      gl.glBegin (GL2.GL_QUADS);
//      setQuad (gl, v0, v1, v2, v3);
//      setQuad (gl, v1, v5, v6, v2);
//      setQuad (gl, v5, v4, v7, v6);
//      setQuad (gl, v4, v0, v3, v7);
//      setQuad (gl, v3, v2, v6, v7);
//      setQuad (gl, v0, v4, v5, v1);
//      gl.glEnd ();
//
//      gl.glPopMatrix();
//   }

//   public void drawWedge (
//      RenderProps props, double scale,
//      float[] v0, float[] v1, float[] v2,
//      float[] v3, float[] v4, float[] v5) {
//
//      float cx = (v0[0]+v1[0]+v2[0]+v3[0]+v4[0]+v5[0])/6;
//      float cy = (v0[1]+v1[1]+v2[1]+v3[1]+v4[1]+v5[1])/6;
//      float cz = (v0[2]+v1[2]+v2[2]+v3[2]+v4[2]+v5[2])/6;
//
//      float s = (float)scale;
//      GL2 gl = getGL2();
//      maybeUpdateState(gl);
//
//      gl.glPushMatrix();
//      gl.glTranslatef (cx*(1-s), cy*(1-s), cz*(1-s));
//      gl.glScalef (s, s, s);
//
//      gl.glBegin (GL2.GL_QUADS);
//      setQuad (gl, v0, v1, v4, v3);
//      setQuad (gl, v1, v2, v5, v4);
//      setQuad (gl, v2, v0, v3, v5);
//      gl.glEnd ();
//
//      gl.glBegin (GL2.GL_TRIANGLES);
//      setTriangle (gl, v0, v2, v1);
//      setTriangle (gl, v3, v4, v5);
//      gl.glEnd ();
//
//      gl.glPopMatrix();
//
//   }
//
//   public void drawPyramid (
//      RenderProps props, double scale,
//      float[] v0, float[] v1, float[] v2,
//      float[] v3, float[] v4) {
//
//      float cx = (v0[0]+v1[0]+v2[0]+v3[0]+v4[0])/5;
//      float cy = (v0[1]+v1[1]+v2[1]+v3[1]+v4[1])/5;
//      float cz = (v0[2]+v1[2]+v2[2]+v3[2]+v4[2])/5;
//
//      float s = (float)scale;
//      GL2 gl = getGL2();
//      maybeUpdateState(gl);
//
//      gl.glPushMatrix();
//      gl.glTranslatef (cx*(1-s), cy*(1-s), cz*(1-s));
//      gl.glScalef (s, s, s);
//
//      gl.glBegin (GL2.GL_QUADS);
//      setQuad (gl, v0, v3, v2, v1);
//      gl.glEnd ();
//
//      gl.glBegin (GL2.GL_TRIANGLES);
//      setTriangle (gl, v0, v1, v4);
//      setTriangle (gl, v1, v2, v4);
//      setTriangle (gl, v2, v3, v4);
//      setTriangle (gl, v3, v0, v4);
//      gl.glEnd ();
//
//      gl.glPopMatrix();
//
//   }

//   public void drawTet (
//      RenderProps props, double scale,
//      float[] v0, float[] v1, float[] v2, float[] v3) {
//
//      float cx = (v0[0]+v1[0]+v2[0]+v3[0])/4;
//      float cy = (v0[1]+v1[1]+v2[1]+v3[1])/4;
//      float cz = (v0[2]+v1[2]+v2[2]+v3[2])/4;
//
//      float s = (float)scale;
//      GL2 gl = getGL2();
//      maybeUpdateState(gl);
//
//      gl.glPushMatrix();
//      gl.glTranslatef (cx*(1-s), cy*(1-s), cz*(1-s));
//      gl.glScalef (s, s, s);
//
//      gl.glBegin (GL2.GL_TRIANGLES);
//      setTriangle (gl, v0, v2, v1);
//      setTriangle (gl, v2, v3, v1);
//      setTriangle (gl, v3, v0, v1);
//      setTriangle (gl, v0, v3, v2);
//      gl.glEnd ();
//
//      gl.glPopMatrix();
//   }

   public void drawSpindle (
      float[] pnt0, float[] pnt1, double rad) {
      GL2 gl = getGL2();
      maybeUpdateState(gl);

      utmp.set (pnt1[0] - pnt0[0], pnt1[1] - pnt0[1], pnt1[2]
      - pnt0[2]);

      Xtmp.p.set (pnt0[0], pnt0[1], pnt0[2]);
      Xtmp.R.setZDirection (utmp);
      gl.glPushMatrix();
      GL2Viewer.mulTransform (gl, Xtmp);
      double len = utmp.norm();

      gl.glScaled (rad, rad, len);

      GL2Primitive spindle = getPrimitive (gl, PrimitiveType.SPINDLE);
      spindle.draw (gl);

      gl.glPopMatrix();
   }

   public void drawArrow (
      float[] pnt0, float[] pnt1, double rad, boolean capped) {
      //GL2 gl = getGL().getGL2();

      utmp.set (
         pnt1[0]-pnt0[0], pnt1[1]-pnt0[1], pnt1[2]-pnt0[2]);
      double len = utmp.norm();
      utmp.normalize();

      double arrowRad = 3*rad;
      double arrowLen = Math.min(2*arrowRad,len/2);

      ctmp[0] = pnt1[0] - (float)(arrowLen*utmp.x);
      ctmp[1] = pnt1[1] - (float)(arrowLen*utmp.y);
      ctmp[2] = pnt1[2] - (float)(arrowLen*utmp.z);

      drawCylinder (pnt0, ctmp, rad, capped);
      doDrawCylinder (ctmp, pnt1, /*capped=*/true, arrowRad, 0.0);
   }

   public void drawCylinder (
      float[] pnt0, float[] pnt1, double rad, boolean capped) {
      if (rad < Double.MIN_NORMAL) {
         return;
      }      
      doDrawCylinder (pnt0, pnt1, capped, rad, rad);
   }

   protected void doDrawCylinder (
      float[] pnt0, float[] pnt1, boolean capped, double base,
      double top) {

      GL2 gl = getGL2();
      maybeUpdateState(gl);

      // drawing manually like this is 10x faster that gluCylinder, but has
      // no texture coordinates
      int nslices = getSurfaceResolution();

      drawCylinder(gl,  nslices, (float)base, (float)top, pnt0, pnt1, capped);

   }

   private void drawCylinder(GL2 gl, int nslices, float base, float top,
      float[] coords0, float[] coords1, boolean capped) {

      utmp.set (coords1[0] - coords0[0], coords1[1] - coords0[1], coords1[2]
      - coords0[2]);
      Xtmp.p.set (coords0[0], coords0[1], coords0[2]);
      Xtmp.R.setZDirection (utmp);
      gl.glPushMatrix();
      GL2Viewer.mulTransform (gl, Xtmp);

      double h = utmp.norm();

      // fill angle buffer
      if (nslices+1 != cosBuff.length) {
         cosBuff = new double[nslices+1];
         sinBuff = new double[nslices+1];
         cosBuff[0] = 1;
         sinBuff[0] = 0;
         cosBuff[nslices] = 1;
         sinBuff[nslices] = 0;
         for (int i=1; i<nslices; i++) {
            double ang = i / (double)nslices * 2 * Math.PI;
            cosBuff[i] = Math.cos(ang);
            sinBuff[i] = Math.sin(ang);
         }
      }

      double nz = (base-top)/h;
      double nscale = 1.0/Math.sqrt(1+nz*nz);

      // draw sides
      gl.glBegin(GL2.GL_QUAD_STRIP);
      double c1,s1;
      for (int i = 0; i <= nslices; i++) {
         c1 = cosBuff[i];
         s1 = sinBuff[i];
         gl.glNormal3d(nscale*c1, nscale*s1, nscale*nz);
         gl.glVertex3d (top * c1, top * s1, h);
         gl.glVertex3d (base * c1, base * s1, 0);
      }

      gl.glEnd();

      if (capped) { // draw top cap first
         if (top > 0) {
            gl.glBegin (GL2.GL_POLYGON);
            gl.glNormal3d (0, 0, 1);
            for (int i = 0; i < nslices; i++) {
               gl.glVertex3d (top * cosBuff[i], top * sinBuff[i], h);
            }
            gl.glEnd();
         }
         // now draw bottom cap
         if (base > 0) {
            gl.glBegin (GL2.GL_POLYGON);
            gl.glNormal3d (0, 0, -1);
            for (int i = nslices-1; i >=0; i--) {
               gl.glVertex3d (base * cosBuff[i], base * sinBuff[i], 0);
            }
            gl.glEnd();
         }
      }
      gl.glPopMatrix();
   }

   @Override
   public void drawCone (
      float[] pnt0, float[] pnt1, double rad0, double rad1,
      boolean capped) {
      if (rad0 < Double.MIN_NORMAL && rad1 < Double.MIN_NORMAL) {
         return;
      }
      doDrawCylinder(pnt0, pnt1, capped, rad0, rad1);
   }

   public void drawLine (
      RenderProps props, float[] pnt0, float[] pnt1, float[] color,
      boolean capped, boolean highlight) {

      boolean savedHighlighting = getHighlighting();
      Shading savedShading = setLineShading (props);
      if (color == null) {
         color = props.getLineColorF ();
      }      
      setPropsColoring (props, color, highlight);
      
      GL2 gl = getGL2();
      maybeUpdateState(gl);

      if (color == null) {
         color = props.getLineColorF ();
      }  
      switch (props.getLineStyle()) {
         case LINE: {
            //setLightingEnabled (false);
            setLineWidth (gl, props.getLineWidth());
//            if (color.length == 3 && props.getAlpha () < 1) {
//               color = new float[]{color[0], color[1], color[2], (float)props.getAlpha ()};
//            }
//            setColor (color, selected);
            gl.glBegin (GL2.GL_LINES);
            gl.glVertex3fv (pnt0, 0);
            gl.glVertex3fv (pnt1, 0);
            gl.glEnd();

            //setLightingEnabled (true);
            break;
         }
         case CYLINDER: {
            //setShadeModel (props.getShading());
            //setPropsMaterial (props, color, selected);
            drawCylinder (pnt0, pnt1, props.getLineRadius(), capped);
            //restoreShading (props);
            break;
         }
         case SOLID_ARROW: {
            //setShadeModel (props.getShading());
            //setPropsMaterial (props, color, selected);
            drawArrow (pnt0, pnt1, props.getLineRadius(), capped);
            //restoreShading (props);
            break;
         }
         case SPINDLE: {
            //setShadeModel (props.getShading());
            //setPropsMaterial (props, color, selected);
            drawSpindle (pnt0, pnt1, props.getLineRadius());
            //restoreShading (props);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented line style " + props.getLineStyle());
         }
      }
      setShading (savedShading);
      setHighlighting (savedHighlighting);
   }

   public void drawArrow (
      RenderProps props, float[] pnt0, float[] pnt1, boolean capped,
      boolean highlight) {

      boolean savedHighlighting = getHighlighting();
      Shading savedShading = setLineShading (props);
      setLineColoring (props, highlight);
      
      GL2 gl = getGL2();
      maybeUpdateState(gl);

      utmp.set (pnt1[0]-pnt0[0], 
                pnt1[1]-pnt0[1], 
                pnt1[2]-pnt0[2]);
      double len = utmp.norm();

      utmp.normalize();
      vtmp.set (pnt0[0], pnt0[1], pnt0[2]);
      double arrowRad = 3 * props.getLineRadius();
      double arrowLen = 2*arrowRad;
      vtmp.scaledAdd (len-arrowLen, utmp);
      ctmp[0] = (float)vtmp.x;
      ctmp[1] = (float)vtmp.y;
      ctmp[2] = (float)vtmp.z;

      if (len > arrowLen) {
         switch (props.getLineStyle()) {
            case LINE: {
               setLineWidth (gl, props.getLineWidth());
               gl.glBegin (GL2.GL_LINES);
               gl.glVertex3fv (pnt0, 0);
               gl.glVertex3fv (ctmp, 0);
               gl.glEnd();             
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
         doDrawCylinder (pnt0, pnt1, capped, len/2, 0.0);
      }
      else {
         doDrawCylinder (ctmp, pnt1, capped, arrowRad, 0.0);
      }
      setShading(savedShading);
      setHighlighting (savedHighlighting);
   }

   //=============================================================================
   // PRIMITIVES
   //=============================================================================

   @Override
   public void drawPoint (float[] pnt) {

      GL2 gl = getGL2();
      maybeUpdateState(gl);
      gl.glBegin (GL2.GL_POINTS);
      gl.glVertex3fv (pnt, 0);
      gl.glEnd();

   }

//   public void drawPoint(float[] pnt, float[] nrm) {
//
//      GL2 gl = getGL2();
//      maybeUpdateState(gl);
//
//      gl.glBegin (GL2.GL_POINTS);
//      gl.glNormal3fv (nrm, 0);
//      gl.glVertex3fv (pnt, 0);
//      gl.glEnd();
//
//   }

//   @Override
//   public void drawPoints (Iterable<float[]> points) {
//      drawPrimitives (points, GL2.GL_POINTS);
//   }

//   @Override
//   public void drawPoints (Iterable<float[]> points, Iterable<float[]> normals) {
//      drawPrimitives (points, normals, GL2.GL_POINTS);
//   }

   @Override
   public void drawLine(float[] pnt0, float[] pnt1) {

      GL2 gl = getGL2();
      maybeUpdateState(gl);

      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3fv (pnt0, 0);
      gl.glVertex3fv (pnt1, 0);
      gl.glEnd();

   }

//   public void drawLine (
//      float[] pnt0, float[] nrm0, float[] pnt1, float[] nrm1) {
//
//      GL2 gl = getGL2();
//      maybeUpdateState(gl);
//
//      gl.glBegin (GL2.GL_LINES);
//      gl.glNormal3fv (nrm0, 0);
//      gl.glVertex3fv (pnt0, 0);
//      gl.glNormal3fv (nrm1, 0);
//      gl.glVertex3fv (pnt1, 0);
//      gl.glEnd();
//
//   }
//
//   public void drawLines(Iterable<float[]> coords) {
//      drawPrimitives (coords, GL2.GL_LINES);
//   }
//
//   public void drawLines(Iterable<float[]> coords, Iterable<float[]> normals) {
//      drawPrimitives (coords, normals, GL2.GL_LINES);
//   }
//
//   public void drawLineStrip(Iterable<float[]> coords, Iterable<float[]> normals) {
//      drawPrimitives (coords,  normals, GL2.GL_LINE_STRIP);
//   }

   /**
    * Draw triangular faces, using the current Shading, lighting and
    * material, and computing a single "face" normal from the coordinates
    * (so the current "shading" really matters only if it is
    * Shading.NONE).
    */
   public void drawTriangle (float[] pnt0, float[] pnt1, float[] pnt2) {

      GL2 gl = getGL2();
      maybeUpdateState(gl);

      float[] normal = new float[3];
      computeNormal (pnt0, pnt1, pnt2, normal);

      gl.glBegin (GL2.GL_TRIANGLES);
      gl.glNormal3fv (normal, 0);
      gl.glVertex3fv (pnt0, 0);
      gl.glVertex3fv (pnt1, 0);
      gl.glVertex3fv (pnt2, 0);
      gl.glEnd ();

   }

//   public void drawTriangle (float[] p0, float[] n0, float[] p1, float[] n1, float[] p2, float[] n2) {
//      GL2 gl = getGL2();
//      maybeUpdateState(gl);
//
//      gl.glBegin (GL2.GL_TRIANGLES);
//      gl.glNormal3fv (n0, 0);
//      gl.glVertex3fv (p0, 0);
//      gl.glNormal3fv (n1, 0);
//      gl.glVertex3fv (p1, 0);
//      gl.glNormal3fv (n2, 0);
//      gl.glVertex3fv (p2, 0);
//      gl.glEnd ();
//
//   }
//
//   public void drawTriangles (Iterable<float[]> points) {
//
//      GL2 gl = getGL2();
//      maybeUpdateState(gl);
//
//      Iterator<float[]> pit = points.iterator ();
//      float[] normal = new float[3];
//
//      gl.glBegin (GL2.GL_TRIANGLES);
//      while (pit.hasNext ()) {
//         float[] p0 = pit.next ();
//         float[] p1 = pit.next ();
//         float[] p2 = pit.next ();
//         computeNormal (p0, p1, p2, normal);
//
//         gl.glNormal3fv (normal, 0);
//         gl.glVertex3fv (p0, 0);
//         gl.glVertex3fv (p1, 0);
//         gl.glVertex3fv (p2, 0);
//      }
//      gl.glEnd ();
//
//   }
//
//   public void drawTriangles (Iterable<float[]> points, Iterable<float[]> normals) {
//      drawPrimitives (points, normals, GL2.GL_TRIANGLES);
//   }


   //=============================================================================
   // OTHER
   //=============================================================================

   public void drawPoint (RenderProps props, float[] pnt, boolean highlight) {

      boolean savedHighlighting = getHighlighting();
      Shading savedShading = setPointShading (props);
      setPointColoring (props, highlight);
      
      GL2 gl = getGL2();
      maybeUpdateState(gl);
      switch (props.getPointStyle()) {
         case POINT: {
            int size = props.getPointSize();
            if (size > 0) {
               //setLightingEnabled (false);
               setPointSize (gl, size);
               //setColor (props.getPointColorArray(), selected);
               drawPoint(pnt);
               //setLightingEnabled (true);
            }
            break;
         }
         case SPHERE: {
            //setPointLighting (props, selected);
            drawSphere (pnt, props.getPointRadius());
            //restoreShading (props);
            break;
         }
         case CUBE: {
            drawCube (pnt, 2*props.getPointRadius());
         }
      }
      setShading (savedShading);
      setHighlighting (savedHighlighting);
   }

   public void drawSphere (float[] centre, float r) {

      GL2 gl = getGL2();
      maybeUpdateState(gl);

      // transform unit sphere
      gl.glPushMatrix();
      gl.glTranslatef (centre[0], centre[1], centre[2]);
      gl.glScalef (r, r, r);

      // get sphere display list
      GL2Primitive sphere = getPrimitive (gl, PrimitiveType.SPHERE);
      sphere.draw (gl);
      
      gl.glPopMatrix();
   }

   public void drawSpheres (Iterable<float[]> centres, float r) {

      GL2 gl = getGL2();
      maybeUpdateState(gl);

      GL2Primitive sphere = getPrimitive (gl, PrimitiveType.SPHERE);

      // draw collection of spheres
      for (float[] p : centres) {
         gl.glPushMatrix();
         gl.glTranslatef (p[0], p[1], p[2]);
         gl.glScalef (r, r, r);
         sphere.draw (gl);
         gl.glPopMatrix();
      }

   }

   public void drawLineStrip (
      RenderProps props, Iterable<float[]> pnts, 
      LineStyle style, boolean highlight) {

      boolean savedHighlighting = getHighlighting();
      Shading savedShading = getShading();
      setShading (style==LineStyle.LINE ? Shading.NONE : props.getShading());
      setLineColoring (props, highlight);
      
      GL2 gl = getGL2();
      maybeUpdateState(gl);

      switch (style) {
         case LINE: {
            //setLightingEnabled (false);
            // draw regular points first
            setLineWidth (gl, props.getLineWidth());
            gl.glBegin (GL2.GL_LINE_STRIP);
            //setColor (props.getLineColorArray(), isSelected);
            for (float[] v : pnts) {
               gl.glVertex3fv (v, 0);
            }
            gl.glEnd();
            
            //setLightingEnabled (true);
            break;
         }
         case SPINDLE:
         case SOLID_ARROW:
         case CYLINDER: {
            //setLineLighting (props, isSelected);
            float[] v0 = null;
            double rad = props.getLineRadius();
            for (float[] v1 : pnts) {
               if (v0 != null) {
                  if (style == LineStyle.SPINDLE) {
                     drawSpindle (v0, v1, rad);
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
               v0[0] = v1[0];
               v0[1] = v1[1];
               v0[2] = v1[2];
            }
            //restoreShading (props);
         }
      }
      setShading (savedShading);
      setHighlighting (savedHighlighting);
   }
  
   public void drawAxes (
      RigidTransform3d X, double[] lens, int width, boolean highlight) {

      boolean savedHighlighting = setHighlighting(highlight);
      setLightingEnabled (false);
      
      GL2 gl = getGL2();
      maybeUpdateState(gl);

      Vector3d u = new Vector3d();
      
      setLineWidth (gl, width);

      if (X == null) {
         X = RigidTransform3d.IDENTITY;
      }
      
      //      boolean wasSelected = false;
      //      if (selected) {
      //         wasSelected = getSelectionHighlighting();
      //         setSelectionHighlighting (true);
      //      }

      gl.glBegin (GL2.GL_LINES);
      for (int i = 0; i < 3; i++) {
         if (lens[i] != 0) {
            if (!highlight && !selectEnabled) {
               setGLColor (gl, 
                  i == 0 ? 1f : 0f, i == 1 ? 1f : 0f, i == 2 ? 1f : 0f, 1f);
            }
            gl.glVertex3d (X.p.x, X.p.y, X.p.z);
            X.R.getColumn (i, u);
            gl.glVertex3d (
               X.p.x + lens[i] * u.x, X.p.y + lens[i] * u.y, X.p.z + lens[i] * u.z);
         }
      }

      gl.glEnd();
      
//      if (selected && !wasSelected) {
//         setSelectionHighlighting (wasSelected);
//      }
      setLightingEnabled (true);
      setHighlighting (savedHighlighting);
   }

   /**
    * Setup for a screenshot during the next render cycle
    * @param w width of shot
    * @param h height of shot
    * @param samples number of samples to use for the
    *        multisample FBO (does antialiasing)
    * @param file file to which the screen shot is to be written
    * @param format format string
    */
   public void setupScreenShot (
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
            fc.lock();  // lock until screen capture is complete
            fc.reconfigure(gl, w, h, samples, gammaCorrection, file, format);
         }
      }
      grab = true;
   }

   public void setupScreenShot (
      int w, int h, File file, String format) {
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

   public void cleanupScreenShots () {
      grabClose = true;
      repaint();  // execute in render thread to delete resources
   }
 
   public GL2 getGL2() {
      GL2 gl = drawable.getGL().getGL2();
      maybeUpdateState (gl);  // update state for GL
      return gl;
   }
   
   public GLU getGLU() {
      if (glu == null) {
         glu = new GLU();
      }
      return glu;
   }

   private boolean setupHSVInterpolation (GL2 gl) {
      // create special HSV shader to interpolate colors in HSV space
      long prog = GLHSVShader.getShaderProgram(gl);
      if (prog > 0) {
         gl.glUseProgramObjectARB ((int)prog);
         return true;
      }
      else {
         // HSV interpolation not supported on this system
         return false;
      }
   }
   
   int myGLColorCount = 0;
   private void setGLColor(GL2 gl, float r, float g, float b, float a) {
      gl.glColor4f (r, g, b, a);
      ++myGLColorCount;
   }
   private void setGLColor4ub(GL2 gl, byte r, byte g, byte b, byte a) {
      gl.glColor4ub (r, g, b, a);
      ++myGLColorCount;
   }
   

   private void setVertexColor (GL2 gl, byte[] color, boolean useHSV) {
      if (!isSelecting() && color != null) {
         if (useHSV) {
            float[] myColorBuf = new float[4];
            for (int i=0; i<4; ++i) {
               myColorBuf[i] = (float)(color[i] & 0xFF)/255.0f;
            }

            // convert color to HSV representation
            ColorUtils.RGBtoHSV (myColorBuf, myColorBuf);
            setGLColor (gl, 
               myColorBuf[0], myColorBuf[1], myColorBuf[2], color[3]);
         }
         else {
            setGLColor4ub (gl, 
               color[0], color[1], color[2], color[3]);
         }
      }
   }

   // call gl.glColor(...) only if not in selection mode
   // assumes the c specifies RGBA colors
   private void setGLColor(GL2 gl, float[] c, int offset, boolean useHSV) {
      if (!isSelecting ()) {
         if (useHSV) {
            float[] cbuf = new float[] {
               c[offset], c[offset+1], c[offset+2], c[offset+3]};

            ColorUtils.RGBtoHSV (cbuf, cbuf);
            setGLColor(gl, cbuf[0], cbuf[1], cbuf[2], cbuf[3]);
         }
         else {
            setGLColor(gl, c[offset], c[offset+1], c[offset+2], c[offset+3]);
         }
      }
   }
   
   private int getTextureMode (ColorMapProps tprops) {
      switch (tprops.getColorMixing ()) {
         case DECAL:
            return GL2.GL_DECAL;
         case REPLACE:
            return GL2.GL_REPLACE;
         case MODULATE:
            return GL2.GL_MODULATE;
            //         case BLEND:
            //            return GL2.GL_BLEND;
         default: {
            throw new InternalErrorException (
               "unimplement texture mode " + tprops.getColorMixing ());
         }
      }
   }

   private void drawColoredCylinder (GL2 gl, int nslices, double base,
      double top, float[] coords0, byte[] color0, float[] coords1, 
      byte[] color1, boolean capped, boolean useHSV) {

      utmp.set (coords1[0] - coords0[0], coords1[1] - coords0[1], coords1[2]
      - coords0[2]);
      Xtmp.p.set (coords0[0], coords0[1], coords0[2]);
      Xtmp.R.setZDirection (utmp);
      gl.glPushMatrix();
      GL2Viewer.mulTransform (gl, Xtmp);

      double h = utmp.norm();

      // fill angle buffer
      if (nslices+1 != cosBuff.length) {
         cosBuff = new double[nslices+1];
         sinBuff = new double[nslices+1];
         cosBuff[0] = 1;
         sinBuff[0] = 0;
         cosBuff[nslices] = 1;
         sinBuff[nslices] = 0;
         for (int i=1; i<nslices; i++) {
            double ang = i / (double)nslices * 2 * Math.PI;
            cosBuff[i] = Math.cos(ang);
            sinBuff[i] = Math.sin(ang);
         }
      }

      double nz = (base-top)/h;
      double nscale = 1.0/Math.sqrt(1+nz*nz);

      // draw sides
      gl.glBegin(GL2.GL_QUAD_STRIP);
      double c1,s1;
      for (int i = 0; i <= nslices; i++) {
         c1 = cosBuff[i];
         s1 = sinBuff[i];
         gl.glNormal3d(nscale*c1, nscale*s1, nscale*nz);

         setVertexColor (gl, color1, useHSV);
         gl.glVertex3d (top * c1, top * s1, h);

         setVertexColor (gl, color0, useHSV);
         gl.glVertex3d (base * c1, base * s1, 0);
      }
      gl.glEnd();

      if (capped) { // draw top cap first
         if (top > 0) {
            setVertexColor(gl, color1, useHSV);
            gl.glBegin (GL2.GL_POLYGON);
            gl.glNormal3d (0, 0, 1);
            for (int i = 0; i < nslices; i++) {
               gl.glVertex3d (top * cosBuff[i], top * sinBuff[i], h);
            }
            gl.glEnd();
         }
         // now draw bottom cap
         if (base > 0) {
         setVertexColor(gl, color0, useHSV);
            gl.glBegin (GL2.GL_POLYGON);
            gl.glNormal3d (0, 0, -1);
            for (int i = nslices-1; i >=0; i--) {
               gl.glVertex3d (base * cosBuff[i], base * sinBuff[i], 0);
            }
            gl.glEnd();
         }
      }

      gl.glPopMatrix();

   }

   /**
    * Converts hsv to rgb.  It is safe to have the same
    * array as both input and output.
    */
   protected static void HSVtoRGB(byte[] hsv, byte[] rgb ) {
      float h = (float)hsv[0]/255;
      float s = (float)hsv[1]/255;
      float v = (float)hsv[2]/255;

      if (s == 0) {
         rgb[0] = (byte)(v*255);
         rgb[1] = (byte)(v*255);
         rgb[2] = (byte)(v*255);
      } else {
         h = (float)(h - Math.floor(h))* 6;
         float f = h - (float)Math.floor(h);
         float p = v * (1.0f - s);
         float q = v * (1.0f - s * f);
         float t = v * (1.0f - (s * (1.0f - f)));
         switch ((int) h) {
            case 0:
               rgb[0] = (byte)(v*255);
               rgb[1] = (byte)(t*255);
               rgb[2] = (byte)(p*255);
               break;
            case 1:
               rgb[0] = (byte)(q*255); 
               rgb[1] = (byte)(v*255); 
               rgb[2] = (byte)(p*255); 
               break;
            case 2:
               rgb[0] = (byte)(p*255);
               rgb[1] = (byte)(v*255);
               rgb[2] = (byte)(t*255);
               break;
            case 3:
               rgb[0] = (byte)(p*255);
               rgb[1] = (byte)(q*255);
               rgb[2] = (byte)(v*255);
               break;
            case 4:
               rgb[0] = (byte)(t*255);
               rgb[1] = (byte)(p*255);
               rgb[2] = (byte)(v*255);
               break;
            case 5:
               rgb[0] = (byte)(v*255);
               rgb[1] = (byte)(p*255);
               rgb[2] = (byte)(q*255);
               break;
         }
      }

   }

   /**
    * Converts rgb to hsv.  It is safe to have the same
    * array as both input and output.
    */
   protected static void RGBtoHSV(byte[] rgb, byte[] hsv) {
      float r = (float)rgb[0]/255;
      float g = (float)rgb[1]/255;
      float b = (float)rgb[2]/255;

      float cmax = (r > g) ? r : g;
      if (b > cmax) cmax = b;
      float cmin = (r < g) ? r : g;
      if (b < cmin) cmin = b;

      hsv[2] = (byte)(cmax*255);
      if (cmax != 0) {
         hsv[1] = (byte)( (cmax - cmin) / cmax * 255);
      } else {
         hsv[1] = 0;
      }
      if (hsv[1] == 0) {
         hsv[0] = 0;
      } else {
         float hue = 0;
         float redc = (cmax - r) / (cmax - cmin);
         float greenc = (cmax - g) / (cmax - cmin);
         float bluec = (cmax - b) / (cmax - cmin);
         if (r == cmax) {
            hue = bluec - greenc;
         } else if (g == cmax) {
            hue = 2.0f + redc - bluec;
         } else {
            hue = 4.0f + greenc - redc;
         }
         hue = hue / 6.0f;
         if (hue < 0) {
            hue = hue + 1.0f;
         }
         hsv[0] = (byte)(hue*255);
      }
   }

   private void drawArrow (GL2 gl, int nslices, double rad,
      float arrowRad, float arrowHeight,
      float[] coords0,float[] coords1, boolean capped) {

      utmp.set (coords1[0] - coords0[0], coords1[1] - coords0[1], coords1[2]
      - coords0[2]);
      Xtmp.p.set (coords0[0], coords0[1], coords0[2]);
      Xtmp.R.setZDirection (utmp);

      gl.glPushMatrix();
      GL2Viewer.mulTransform (gl, Xtmp);

      double h2 = utmp.norm();
      double h = h2-arrowHeight;


      // maybe re-fill angle buffer
      if (nslices+1 != cosBuff.length) {
         cosBuff = new double[nslices+1];
         sinBuff = new double[nslices+1];
         cosBuff[0] = 1;
         sinBuff[0] = 0;
         cosBuff[nslices] = 1;
         sinBuff[nslices] = 0;
         for (int i=1; i<nslices; i++) {
            double ang = i / (double)nslices * 2 * Math.PI;
            cosBuff[i] = Math.cos(ang);
            sinBuff[i] = Math.sin(ang);
         }
      }

      // draw shaft
      gl.glBegin(GL2.GL_QUAD_STRIP);
      double c1,s1;
      for (int i = 0; i <= nslices; i++) {
         c1 = cosBuff[i];
         s1 = sinBuff[i];
         gl.glNormal3d(c1, s1, 0);
         gl.glVertex3d (rad * c1, rad * s1, h);  
         gl.glVertex3d (rad * c1, rad * s1, 0);
      }
      gl.glEnd();

      // arrow
      gl.glBegin(GL2.GL_QUAD_STRIP);
      for (int i = 0; i <= nslices; i++) {
         c1 = cosBuff[i];
         s1 = sinBuff[i];
         gl.glNormal3d(c1, s1, 1);
         gl.glVertex3d (0, 0, h2);      
         gl.glVertex3d (arrowRad * c1, arrowRad * s1, h);   
      }
      gl.glEnd();

      if (capped) { 
         // bottom cap
         gl.glBegin (GL2.GL_POLYGON);
         gl.glNormal3d (0, 0, -1);
         for (int i = 0; i < nslices; i++) {
            gl.glVertex3d (-rad * cosBuff[i], rad * sinBuff[i], 0);
         }
         gl.glEnd();

         // connection
         gl.glBegin (GL2.GL_QUAD_STRIP);
         gl.glNormal3d (0, 0, -1);
         for (int i = 0; i <= nslices; i++) {
            gl.glVertex3d (arrowRad * cosBuff[i], arrowRad * sinBuff[i], h);
            gl.glVertex3d (rad * cosBuff[i], rad * sinBuff[i], h);
         }
         gl.glEnd();
      }

      gl.glPopMatrix();

   }

   private void drawColoredArrow (GL2 gl, int nslices, double rad,
      float arrowRad, float arrowHeight,
      float[] coords0, byte[] color0, float[] coords1, 
      byte[] color1, boolean hsv, boolean capped) {

      utmp.set (coords1[0] - coords0[0], coords1[1] - coords0[1], coords1[2]
      - coords0[2]);
      Xtmp.p.set (coords0[0], coords0[1], coords0[2]);
      Xtmp.R.setZDirection (utmp);

      gl.glPushMatrix();
      GL2Viewer.mulTransform (gl, Xtmp);

      // interpolate color
      byte[] colorM = new byte[4];
      double h2 = utmp.norm();
      double h = h2-arrowHeight;
      double t = (float)(h/h2);
      interpColor4ub(color0, t, color1, colorM, hsv);

      // maybe re-fill angle buffer
      if (nslices+1 != cosBuff.length) {
         cosBuff = new double[nslices+1];
         sinBuff = new double[nslices+1];
         cosBuff[0] = 1;
         sinBuff[0] = 0;
         cosBuff[nslices] = 1;
         sinBuff[nslices] = 0;
         for (int i=1; i<nslices; i++) {
            double ang = i / (double)nslices * 2 * Math.PI;
            cosBuff[i] = Math.cos(ang);
            sinBuff[i] = Math.sin(ang);
         }
      }

      // draw shaft
      gl.glBegin(GL2.GL_QUAD_STRIP);
      double c1,s1;
      for (int i = 0; i <= nslices; i++) {
         c1 = cosBuff[i];
         s1 = sinBuff[i];
         gl.glNormal3d(c1, s1, 0);
         setVertexColor(gl, colorM, hsv);
         gl.glVertex3d (rad * c1, rad * s1, h);  

         setVertexColor(gl, color0, hsv);
         gl.glVertex3d (rad * c1, rad * s1, 0);
      }
      gl.glEnd();

      // arrow
      gl.glBegin(GL2.GL_QUAD_STRIP);
      for (int i = 0; i <= nslices; i++) {
         c1 = cosBuff[i];
         s1 = sinBuff[i];
         gl.glNormal3d(c1, s1, 1);

         setVertexColor(gl, color1, hsv);
         gl.glVertex3d (0, 0, h2); 
         setVertexColor(gl, colorM, hsv);
         gl.glVertex3d (arrowRad * c1, arrowRad * s1, h);   

      }
      gl.glEnd();

      if (capped) { 
         // bottom cap
         setVertexColor(gl, color0, hsv);
         gl.glBegin (GL2.GL_POLYGON);
         gl.glNormal3d (0, 0, -1);
         for (int i = 0; i < nslices; i++) {
            gl.glVertex3d (-rad * cosBuff[i], rad * sinBuff[i], 0);
         }
         gl.glEnd();

         // connection
         setVertexColor(gl, colorM, hsv);
         gl.glBegin (GL2.GL_QUAD_STRIP);
         gl.glNormal3d (0, 0, -1);
         for (int i = 0; i <= nslices; i++) {
            gl.glVertex3d (arrowRad * cosBuff[i], arrowRad * sinBuff[i], h);
            gl.glVertex3d (rad * cosBuff[i], rad * sinBuff[i], h);
         }
         gl.glEnd();
      }

      gl.glPopMatrix();

   }

   private void drawSpindle(GL2 gl, int slices, float rad, float[] p0, float[] p1) {

      utmp.set (p1[0] - p0[0], p1[1] - p0[1], p1[2]
      - p0[2]);
      Xtmp.p.set (p0[0], p0[1], p0[2]);
      Xtmp.R.setZDirection (utmp);

      gl.glPushMatrix();
      GL2Viewer.mulTransform (gl, Xtmp);

      int levels = slices/2;
      levels = Math.max(levels, 2);

      double s0 = 0;
      double c0 = 1;
      double len = utmp.norm();

      // gl.glScaled(rad, rad, len);

      for (int slice = 0; slice < slices; slice++) {
         double ang = (slice + 1) * 2 * Math.PI / slices;
         double c1 = Math.cos (ang);
         double s1 = Math.sin (ang);

         gl.glBegin (GL2.GL_TRIANGLE_STRIP);
         for (int j = 0; j <= levels; j++) {
            double h = j * 1.0 / levels;
            double r = 1 * Math.sin (h * Math.PI);
            double drdh = Math.PI * Math.cos (h * Math.PI);
            gl.glNormal3d (c0, s0, -drdh*rad/len);
            gl.glVertex3d (c0 * r * rad, s0 * r * rad, h * len);
            gl.glNormal3d (c1, s1, -drdh*rad/len);
            gl.glVertex3d (c1 * r * rad, s1 * r * rad, h * len);
         }
         gl.glEnd();

         s0 = s1;
         c0 = c1;
      }

      gl.glPopMatrix();
   }

   private void interpColor4ub(byte[] c0, double t, byte[] c1, byte[] out, boolean hsv) {
      if (hsv) {
         byte[] tmp = new byte[4];
         RGBtoHSV(c0, tmp);
         tmp[3] = c0[3];
         RGBtoHSV(c1, out);
         out[3] = c1[3];
         for (int i=0; i<4; ++i) {
            out[i] = (byte)((1-t)*(tmp[i]&0xFF) + t*(out[i]&0xFF));
         }
         HSVtoRGB(out, out);
      } else {
         for (int i=0; i<4; ++i) {
            out[i] = (byte)((1-t)*(c0[i]&0xFF) + t*(c1[i]&0xFF));
         }
      }
   }

   private void drawColoredSpindle(GL2 gl, int slices, float rad, float[] p0, byte[] c0,
      float[] p1, byte[] c1, boolean hsv) {

      utmp.set (p1[0] - p0[0], p1[1] - p0[1], p1[2]
      - p0[2]);
      Xtmp.p.set (p0[0], p0[1], p0[2]);
      Xtmp.R.setZDirection (utmp);

      gl.glPushMatrix();
      GL2Viewer.mulTransform (gl, Xtmp);

      int levels = slices/2;
      levels = Math.max(levels, 2);

      double sin0 = 0;
      double cos0 = 1;
      double len = utmp.norm();

      // gl.glScaled(rad, rad, len);

      byte[] cm = new byte[4];

      for (int slice = 0; slice < slices; slice++) {
         double ang = (slice + 1) * 2 * Math.PI / slices;
         double cos1 = Math.cos (ang);
         double sin1 = Math.sin (ang);

         gl.glBegin (GL2.GL_TRIANGLE_STRIP);
         for (int j = 0; j <= levels; j++) {
            double h = j * 1.0 / levels;
            double r = 1 * Math.sin (h * Math.PI);
            double drdh = Math.PI * Math.cos (h * Math.PI);

            interpColor4ub(c0, h, c1, cm, hsv);
            setVertexColor(gl, cm, hsv);
            gl.glNormal3d (cos0, sin0, -drdh*rad/len);
            gl.glVertex3d (cos0 * r * rad, sin0 * r * rad, h * len);
            gl.glNormal3d (cos1, sin1, -drdh*rad/len);
            gl.glVertex3d (cos1 * r * rad, sin1 * r * rad, h * len);
         }
         gl.glEnd();

         sin0 = sin1;
         cos0 = cos1;
      }

      gl.glPopMatrix();
   }

   protected int enableVertexColoring(boolean useHSV) {
      gl.glColorMaterial (GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
      // if selection color is active, ignore material color
      if (myActiveColor == ActiveColor.DEFAULT) {
         gl.glEnable (GL2.GL_COLOR_MATERIAL);
      }
      int savedShading = getGLShadeModel();
      if (savedShading == GL2.GL_FLAT) {
         gl.glShadeModel (GL2.GL_SMOOTH);
      }
      if (useHSV) {
         useHSV = setupHSVInterpolation(gl);
      }
      return savedShading;
   }

   protected void disableVertexColoring (boolean useHSV, int savedShading) {
      if (savedShading == GL2.GL_FLAT) {
         gl.glShadeModel (savedShading);
      }
      if (myActiveColor == ActiveColor.DEFAULT) {
         gl.glDisable (GL2.GL_COLOR_MATERIAL);
      }
      if (useHSV) {
         gl.glUseProgramObjectARB (0);
      }
   }

   private void drawRawTriangles(GL2 gl, RenderObject robj, int gidx,
      int offset, int count, boolean useNormals, 
      boolean useColors, boolean useTextures, boolean useHSV ) {
      
      gl.glBegin(GL.GL_TRIANGLES);
      int[] tris = robj.getTriangles (gidx);
      int triangleStride = robj.getTriangleStride();

      int idx = 0;
      for (int i=0; i<count; ++i) {
         idx = (i+offset)*triangleStride;
         for (int j=0; j<triangleStride; ++j) {
            int vidx = tris[idx+j];
            if (useColors) {
               setVertexColor (gl, robj.getVertexColor(vidx), useHSV);
            }
            if (robj.hasNormals ()) {
               gl.glNormal3fv(robj.getVertexNormal(vidx), 0);
            }
            if (useTextures) {
               gl.glTexCoord2fv (robj.getVertexTextureCoord (vidx), 0);
            }
            gl.glVertex3fv(robj.getVertexPosition(vidx), 0);
         }
      }
      gl.glEnd();
   }

   @Override
   public void drawTriangles(RenderObject robj, int gidx, int offset, int count) {

      if (count == 0) {
         return;
      }
      
      boolean enableLighting = false;
      if (isLightingEnabled() && !robj.hasNormals()) {
         enableLighting = true;
         setLightingEnabled(false);
      }
      maybeUpdateState(gl);
         
      boolean selecting = isSelecting();
      boolean hasColors = (robj.hasColors() && hasVertexColoring());
      boolean useColors = hasColors && !selecting && !(myActiveColor == ActiveColor.HIGHLIGHT);
      boolean useHSV = isHSVColorInterpolationEnabled (); // && !isLightingEnabled ();
      
      boolean hasTexture = robj.hasTextureCoords ();
      if (hasTexture) {
         hasTexture = maybeActivateTextures (gl);
      }

      // if use vertex colors, get them to track glColor      
      int savedShading = 0;
      if (useColors) {
         savedShading = enableVertexColoring (useHSV);
      }

      int totalTriangleCount = robj.numTriangles (gidx);
      boolean useDisplayList = (!selecting || !hasColors) && (!robj.isTransient()) 
         && ((offset == 0) && (count == totalTriangleCount))
         && (3*robj.numTriangles (gidx) > DISPLAY_LIST_VERTEX_MINIMUM);
      GL2VersionedObject gvo = null;
    
      boolean compile = true;
      if (useDisplayList) {
         // get snapshot of version information
         RenderObjectVersion fingerprint = robj.getVersionInfo();
         RenderObjectKey key = new RenderObjectKey(robj.getIdentifier (), DrawType.TRIANGLES, gidx);
         
         gvo = myGLResources.getVersionedObject (key);
         if (gvo != null) {
            boolean iv = gvo.disposeInvalid (gl);
            if (iv == true) {
               Logger.getSystemLogger().debug(" invalid object disposed " + gvo);
            }
         }
         if (gvo == null || gvo.disposeInvalid (gl)) {
            gvo = myGLResources.allocateVersionedObject(gl, key, fingerprint);
            compile = true;
         } else {
            compile = !(gvo.compareExchangeFingerPrint(fingerprint));
         }
      }

      if (compile) {
         if (DEBUG) {
            Logger.getSystemLogger().debug("Compiling dl:" + gvo.getDisplayList ().getListId ());
         }
         if (gvo != null) {
            gvo.beginCompile (gl);
         }

         // prevent modifications on RenderObject while reading
         robj.readLock ();

         drawRawTriangles(gl, robj, gidx, offset, 
            count, robj.hasNormals(), useColors, 
            hasTexture, useHSV);

         robj.readUnlock ();
         

         if (gvo != null) {
            gvo.endCompile (gl);
            gvo.draw (gl);
         }
      } else {
         gvo.draw (gl);
      }
         
      if (hasTexture) {
         deactivateTexture (gl);
      }

      // disable color tracking
      if (useColors) {
         disableVertexColoring (useHSV, savedShading);
      }
      if (enableLighting) {
         setLightingEnabled(true);
      }
   }

   private static class PointFingerPrint {

      private PointStyle style;
      private Object sphere;
      private float r;
      private RenderObjectVersion rv;

      public PointFingerPrint(RenderObjectVersion rv, PointStyle style, Object sphere, float r) {
         this.rv = rv;
         this.style = style;
         this.sphere = sphere;
         this.r = r;
      }

      @Override
      public int hashCode() {
         return (style == null ? 0 : style.ordinal()) + (sphere==null ? 0 : sphere.hashCode ())*73 
            + GLSupport.hashCode(r)*51 + 31*rv.hashCode();
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != this.getClass()) {
            return false;
         }
         PointFingerPrint other = (PointFingerPrint)obj;
         if (style != other.style || sphere != other.sphere || r != other.r) {
            return false;
         }
         return rv.equals(other.rv);
      }

   }

   private static class LineFingerPrint {

      private RenderObjectVersion rv;
      private LineStyle style;
      private int slices;
      private float r;
      public LineFingerPrint(RenderObjectVersion rv, LineStyle style, int slices, float r) {
         this.rv = rv;
         this.style = style;
         this.slices = slices;
         this.r = r;
      }

      @Override
      public int hashCode() {
         return (style == null ? 0 : style.ordinal()) + 71*slices + GLSupport.hashCode(r)*51 + 31*rv.hashCode();
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != this.getClass()) {
            return false;
         }
         LineFingerPrint other = (LineFingerPrint)obj;
         if (style != other.style || slices != other.slices || r != other.r) {
            return false;
         }
         return rv.equals(other.rv);
      }

   }

   private static class VertexFingerPrint {
      private RenderObjectVersion rv;
      private DrawMode mode;
      public VertexFingerPrint(RenderObjectVersion rv,DrawMode mode) {
         this.rv = rv;
         this.mode = mode;
      }

      @Override
      public int hashCode() {
         return (mode == null ? 0 : mode.ordinal()) + 31*rv.hashCode();
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != this.getClass()) {
            return false;
         }
         VertexFingerPrint other = (VertexFingerPrint)obj;
         if (mode != other.mode) {
            return false;
         }
         return rv.equals(other.rv);
      }
   }
   
   private static class VertexListFingerPrint {
      private RenderObjectVersion rv;
      DrawMode mode;
      private int listVersion;
      public VertexListFingerPrint(RenderObjectVersion rv, DrawMode mode, int listVersion) {
         this.rv = rv;
         this.mode = mode;
         this.listVersion = listVersion;
      }

      @Override
      public int hashCode() {
         return listVersion + 31*rv.hashCode() + 71*mode.hashCode ();
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != this.getClass()) {
            return false;
         }
         VertexListFingerPrint other = (VertexListFingerPrint)obj;
         if (listVersion != other.listVersion) {
            return false;
         }
         if (this.mode != other.mode) {
            return false;
         }
         return rv.equals(other.rv);
      }
   }
   
   @Override
   public void drawLines(RenderObject robj, int gidx) {
      drawSimpleLines(robj, gidx, 0, robj.numLines (gidx));
   }
      
   public void drawSimpleLines(RenderObject robj, int gidx, int offset, int count) {
      
      if (count == 0) {
         return;
      }
      
      boolean enableLighting = false;
      if (isLightingEnabled() && !robj.hasNormals()) {
         enableLighting = true;
         setLightingEnabled(false);
      }
      maybeUpdateState(gl);

      boolean selecting = isSelecting();
      boolean hasColors = (robj.hasColors() && hasVertexColoring());
      boolean useColors = hasColors && !selecting && (myActiveColor==ActiveColor.DEFAULT);
      boolean useHSV = isHSVColorInterpolationEnabled (); // && !isLightingEnabled ();

      // if use vertex colors, get them to track glColor
      int savedShading = 0;
      if (useColors) {
         savedShading = enableVertexColoring (useHSV);
      }

      int totalLineCount = robj.numLines (gidx);
      boolean useDisplayList = (!selecting || !hasColors) && (!robj.isTransient()) 
         && ((offset == 0) && (count == totalLineCount))
         && (2*robj.numLines (gidx) > DISPLAY_LIST_VERTEX_MINIMUM);

      boolean compile = true;
      GL2VersionedObject gvo = null;

      if (useDisplayList) {
         RenderObjectKey key = new RenderObjectKey(robj.getIdentifier (), DrawType.LINES, gidx);
         LineFingerPrint fingerprint = new LineFingerPrint(robj.getVersionInfo(), LineStyle.LINE, 0, 0);
         
         gvo = myGLResources.getVersionedObject (key);
         if (gvo != null) {
            boolean iv = gvo.disposeInvalid (gl);
            if (iv == true) {
               Logger.getSystemLogger().debug(" invalid object disposed " + gvo);
            }
         }
         if (gvo == null || gvo.disposeInvalid (gl)) {
            gvo = myGLResources.allocateVersionedObject (gl, key, fingerprint);
            compile = true;
         } else {
            compile = !(gvo.compareExchangeFingerPrint(fingerprint));
         }
      }

      if (compile) {
         if (gvo != null) {
            gvo.beginCompile (gl);
         }
         
         gl.glBegin(GL.GL_LINES);

         // prevent modifications on RenderObject while reading
         robj.readLock ();

         int[] lines = robj.getLines (gidx);
         int lineStride = robj.getLineStride ();

         for (int i=0; i<count; ++i) {
            int baseIdx = (i+offset)*lineStride;
            for (int j=0; j<2; j++) {
               int vidx = lines[baseIdx+j];
               if (!selecting && useColors) {
                  setVertexColor (gl, robj.getVertexColor (vidx), useHSV);
               }
               if (robj.hasNormals()) {
                  gl.glNormal3fv(robj.getVertexNormal(vidx), 0);
               }
               if (robj.hasTextureCoords ()) {
                  gl.glTexCoord2fv(robj.getVertexTextureCoord (vidx), 0);
               }
               gl.glVertex3fv(robj.getVertexPosition(vidx), 0);
            }
         }
         
         robj.readUnlock ();
         
         gl.glEnd();
         if (gvo != null) {
            gvo.endCompile (gl);
            gvo.draw (gl);
         }
      } else {
         gvo.draw (gl);
      }

      // disable color tracking
      if (useColors) {
         disableVertexColoring (useHSV, savedShading);
      }
      if (enableLighting) {
         setLightingEnabled(true);
      }
   }
   
   private void drawSolidLines(RenderObject robj, int gidx, 
      int offset, int count, LineStyle style, float rad) {
      
      if (count == 0) {
         return;
      }

      boolean selecting = isSelecting();
      boolean hasColors = (robj.hasColors() && hasVertexColoring());
      boolean useColors = hasColors && !selecting && (myActiveColor==ActiveColor.DEFAULT);
      boolean useHSV = isHSVColorInterpolationEnabled (); // && !isLightingEnabled ();

      // if use vertex colors, get them to track glColor      
      int savedShading = 0;
      if (useColors) {
         savedShading = enableVertexColoring (useHSV);
      }

      int totalLineCount = robj.numLines (gidx);
      boolean useDisplayList = (!selecting || !hasColors) && (!robj.isTransient()) 
         && ((offset == 0) && (count == totalLineCount))
         && (2*robj.numLines (gidx) > DISPLAY_LIST_VERTEX_MINIMUM);
      
      GL2VersionedObject gvo = null;
      boolean compile = true;

      if (useDisplayList) {
         RenderObjectKey key = new RenderObjectKey(robj.getIdentifier (), DrawType.LINES, gidx);
         LineFingerPrint fingerprint = 
            new LineFingerPrint(robj.getVersionInfo(), 
               style, mySurfaceResolution, rad);
         
         gvo = myGLResources.getVersionedObject (key);
         if (gvo != null) {
            boolean iv = gvo.disposeInvalid (gl);
            if (iv == true) {
               Logger.getSystemLogger().debug(" invalid object disposed " + gvo);
            }
         }
         if (gvo == null || gvo.disposeInvalid (gl)) {
            gvo = myGLResources.allocateVersionedObject(gl, key, fingerprint);
            compile = true;
         } else {
            compile = !(gvo.compareExchangeFingerPrint(fingerprint));
         }
      }

      if (compile) {
         if (gvo != null) {
            gvo.beginCompile (gl);
         }
         
         // prevent writes to robj
         robj.readLock ();
         
         int[] lines = robj.getLines(gidx);
         int lineStride = robj.getLineStride ();

         switch (style) {
            case CYLINDER: {
               if (!selecting && useColors) {
                  for (int i=0; i<count; ++i) {
                     int baseIdx = lineStride*(i+offset);
                     int vidx1 = lines[baseIdx];
                     int vidx2 = lines[baseIdx+1];
                     float[] p0 = robj.getVertexPosition(vidx1);
                     byte[] c0 = robj.getVertexColor(vidx1);
                     float[] p1 = robj.getVertexPosition(vidx2);
                     byte[] c1 = robj.getVertexColor(vidx2);
                     drawColoredCylinder(
                        gl, mySurfaceResolution, rad, rad, 
                        p0, c0, p1, c1, true, useHSV);
                  }
               } else {
                  for (int i=0; i<count; ++i) {
                     int baseIdx = lineStride*(i+offset);
                     int vidx1 = lines[baseIdx];
                     int vidx2 = lines[baseIdx+1];
                     float[] p0 = robj.getVertexPosition(vidx1);
                     float[] p1 = robj.getVertexPosition(vidx2);
                     drawCylinder(
                        gl, mySurfaceResolution, rad, rad, p0, p1, true);
                  }
               }
               break;
            }
            case SPINDLE:
               if (!selecting && useColors) {
                  for (int i=0; i<count; ++i) {
                     int baseIdx = lineStride*(i+offset);
                     int vidx1 = lines[baseIdx];
                     int vidx2 = lines[baseIdx+1];
                     float[] p0 = robj.getVertexPosition(vidx1);
                     byte[] c0 = robj.getVertexColor(vidx1);
                     float[] p1 = robj.getVertexPosition(vidx2);
                     byte[] c1 = robj.getVertexColor(vidx2);
                     drawColoredSpindle(
                        gl, mySurfaceResolution, rad, 
                        p0, c0, p1, c1, isHSVColorInterpolationEnabled());
                  }
               } else {
                  for (int i=0; i<count; ++i) {
                     int baseIdx = lineStride*(i+offset);
                     int vidx1 = lines[baseIdx];
                     int vidx2 = lines[baseIdx+1];
                     float[] p0 = robj.getVertexPosition(vidx1);
                     float[] p1 = robj.getVertexPosition(vidx2);
                     drawSpindle(gl, mySurfaceResolution, rad, p0, p1);
                  }
               }
               break;
            case SOLID_ARROW: {
               float arad = rad*3;
               float aheight = arad*2;
               if (!selecting && useColors) {
                  for (int i=0; i<count; ++i) {
                     int baseIdx = lineStride*(i+offset);
                     int vidx1 = lines[baseIdx];
                     int vidx2 = lines[baseIdx+1];
                     float[] p0 = robj.getVertexPosition(vidx1);
                     byte[] c0 = robj.getVertexColor(vidx1);
                     float[] p1 = robj.getVertexPosition(vidx2);
                     byte[] c1 = robj.getVertexColor(vidx2);
                     drawColoredArrow(
                        gl, mySurfaceResolution, rad, arad, aheight, 
                        p0, c0, p1, c1, isHSVColorInterpolationEnabled(), true);
                  }
               } else {
                  for (int i=0; i<count; ++i) {
                     int baseIdx = lineStride*(i+offset);
                     int vidx1 = lines[baseIdx];
                     int vidx2 = lines[baseIdx+1];
                     float[] p0 = robj.getVertexPosition(vidx1);
                     float[] p1 = robj.getVertexPosition(vidx2);
                     drawArrow(
                        gl, mySurfaceResolution, rad, arad, aheight, 
                        p0, p1, true);
                  }
               }
               break;
            }
            default:
         }
         
         robj.readUnlock ();
         
         if (gvo != null) {
            gvo.endCompile (gl);
            gvo.draw (gl);
         }
      } else {
         gvo.draw (gl);
      }

      // disable color tracking
      if (useColors) {
         disableVertexColoring (useHSV, savedShading);
      }
   }
   
   @Override
   public void drawLines(RenderObject robj, int gidx, int offset,
      int count, LineStyle style, double rad) {
      maybeUpdateState(gl);

      switch (style) {
         case LINE: {
            // maybe change line width
            setLineWidth (gl, (float)rad);
            drawSimpleLines(robj, gidx, offset, count);
            break;
         }
         case CYLINDER:
         case SPINDLE:
         case SOLID_ARROW:
            drawSolidLines(robj, gidx, offset, count, style, (float)rad);
            break;
      }
   }   
   
   @Override
   public void drawPoints(RenderObject robj, int gidx) {
      drawSimplePoints(robj, gidx, 0, robj.numPoints (gidx));
   }
   
   private void drawSimplePoints(RenderObject robj, int gidx,
      int offset, int count) {

      if (count == 0) {
         return;
      }

      boolean enableLighting = false;
      if (isLightingEnabled() && !robj.hasNormals()) {
         enableLighting = true;
         setLightingEnabled(false);
      }
      maybeUpdateState(gl);

      boolean selecting = isSelecting();
      boolean hasColors = (robj.hasColors() && hasVertexColoring());
      boolean useColors = hasColors && !selecting && (myActiveColor==ActiveColor.DEFAULT);
      boolean useHSV = isHSVColorInterpolationEnabled (); // && !isLightingEnabled ();

      // if use vertex colors, get them to track glColor      
      int savedShading = 0;
      if (useColors) {
         savedShading = enableVertexColoring (useHSV);
      }

      int totalPointCount = robj.numPoints (gidx);
      boolean useDisplayList = (!selecting || !hasColors) && (!robj.isTransient()) 
         && ((offset == 0) && (count == totalPointCount))
         && (robj.numPoints (gidx) > DISPLAY_LIST_VERTEX_MINIMUM);
      
      GL2VersionedObject gvo = null;
      boolean compile = true;

      if (useDisplayList) {
         RenderObjectKey key = new RenderObjectKey(robj.getIdentifier (), DrawType.POINTS, gidx);
         PointFingerPrint fingerprint = new PointFingerPrint(robj.getVersionInfo(), PointStyle.POINT, 0, 0);
         
         gvo = myGLResources.getVersionedObject (key);
         if (gvo != null) {
            boolean iv = gvo.disposeInvalid (gl);
            if (iv == true) {
               Logger.getSystemLogger().debug(" invalid object disposed " + gvo);
            }
         }
         if (gvo == null || gvo.disposeInvalid (gl)) {
            gvo = myGLResources.allocateVersionedObject (gl, key, fingerprint);
            compile = true;
         } else {
            compile = !(gvo.compareExchangeFingerPrint(fingerprint));
         }
      }

      if (compile) {
         if (gvo != null) {
            gvo.beginCompile (gl);
         }

         gl.glBegin(GL.GL_POINTS);

         robj.readLock ();  // prevent writes while reading
         
         int[] points = robj.getPoints (gidx);
         for (int i=0; i<count; ++i) {
            int vidx = points[i+offset];
            if (!selecting && robj.hasColors() && hasVertexColoring()) {
               setVertexColor (gl, robj.getVertexColor(vidx), useHSV);
            }
            if (robj.hasNormals()) {
               gl.glNormal3fv(robj.getVertexNormal(vidx),0);
            }
            if (robj.hasTextureCoords ()) {
               gl.glTexCoord2fv(robj.getVertexTextureCoord(vidx),0);
            }
            gl.glVertex3fv(robj.getVertexPosition(vidx), 0);
         }

         robj.readUnlock ();

         gl.glEnd();

         if (gvo != null) {
            gvo.endCompile (gl);
            gvo.draw (gl);
         }
      } else {
         gvo.draw (gl);
      }

      // disable color tracking
      if (useColors) {
         disableVertexColoring (useHSV, savedShading);
      }
      if (enableLighting) {
         setLightingEnabled(true);
      }
   }

   private void drawSolidPoints(RenderObject robj, int gidx, 
      int offset, int count,
      PointStyle style, double rad) {
      
      if (count == 0) {
         return;
      }
      
      int totalPointCount = robj.numPoints (gidx);
      
      GL2 gl = getGL2();
      maybeUpdateState(gl);

      boolean selecting = isSelecting();
      boolean hasColors = (robj.hasColors() && hasVertexColoring());
      boolean useColors = hasColors && !selecting && (myActiveColor==ActiveColor.DEFAULT);
      boolean useHSV = isHSVColorInterpolationEnabled (); // && !isLightingEnabled ();

      // if use vertex colors, get them to track glColor      
      int savedShading = 0;
      if (useColors) {
         savedShading = enableVertexColoring (useHSV);
      }

      GL2Primitive point = null;
      switch (style) {
         case CUBE:
            point = getPrimitive (gl, PrimitiveType.CUBE);
            break;
         case POINT:
         case SPHERE:
            point = getPrimitive (gl, PrimitiveType.SPHERE);
      }
      
      
      boolean useDisplayList = (!selecting || !hasColors) && (!robj.isTransient())
         && ((offset == 0) && (count == totalPointCount))
         && (robj.numPoints (gidx) > DISPLAY_LIST_VERTEX_MINIMUM);
      
      GL2VersionedObject gvo = null;
      boolean compile = true;

      if (useDisplayList) {
         RenderObjectKey key = new RenderObjectKey(robj.getIdentifier (), DrawType.POINTS, gidx);
         PointFingerPrint fingerprint = new PointFingerPrint(robj.getVersionInfo(), style, 
            point, (float)rad);
         
         gvo = myGLResources.getVersionedObject (key);
         if (gvo != null) {
            boolean iv = gvo.disposeInvalid (gl);
            if (iv == true) {
               Logger.getSystemLogger().debug(" invalid object disposed " + gvo);
            }
         }
         if (gvo == null || gvo.disposeInvalid (gl)) {
            gvo = myGLResources.allocateVersionedObject (gl, key, fingerprint);
            compile = true;
         } else {
            compile = !(gvo.compareExchangeFingerPrint(fingerprint));
         }
      }

      if (compile) {
         if (gvo != null) {
            gvo.beginCompile (gl);
         }

         robj.readLock ();

         int[] points = robj.getPoints(gidx);
         for (int i=0; i<count; ++i) {
            if (i+offset >= points.length) {
               System.out.println (
                  "i=" + i + " count=" + count + " offset=" + offset +
                  " points.length=" + points.length + " tpc=" + totalPointCount);
            }
            int vidx = points[i+offset];
            if (!selecting && useColors) {
               setVertexColor (gl, robj.getVertexColor (vidx), useHSV);
            }
            // position
            float [] p = robj.getPosition(vidx);

            // location and scale
            gl.glPushMatrix();
            gl.glTranslatef (p[0], p[1], p[2]);
            gl.glScaled (rad, rad, rad);   

            // draw sphere
            point.draw (gl);
            gl.glPopMatrix();

         }

         robj.readUnlock ();

         if (gvo != null) {
            gvo.endCompile (gl);
            gvo.draw (gl);
         }
      } else {
         gvo.draw (gl);
      }

      // disable color tracking
      if (useColors) {
         disableVertexColoring (useHSV, savedShading);
      }
   }
   
   @Override
   public void drawPoints(RenderObject robj, int gidx, int offset, int count,
      PointStyle style, double rad) {
      
      switch (style) { 
         case POINT: {
            // maybe change point size and draw points
            setPointSize(gl, (float)rad);
            drawSimplePoints(robj, gidx, offset, count);
            break;
         }
         case SPHERE: 
         case CUBE:{
            // draw spheres
            drawSolidPoints(robj, gidx, offset, count, style, rad);
         }
      }

   }

   @Override
   public void drawVertices(RenderObject robj, DrawMode mode) {

      boolean enableLighting = false;
      if (isLightingEnabled() && !robj.hasNormals()) {
         enableLighting = true;
         setLightingEnabled(false);
      }
      maybeUpdateState(gl);
      
      boolean selecting = isSelecting();
      boolean hasColors = (robj.hasColors() && hasVertexColoring());
      boolean useColors = hasColors && !selecting && (myActiveColor==ActiveColor.DEFAULT);
      boolean useHSV = isHSVColorInterpolationEnabled (); // && !isLightingEnabled ();

      // if use vertex colors, get them to track glColor      
      int savedShading = 0;
      if (useColors) {
         savedShading = enableVertexColoring (useHSV);
      }
      
      boolean hasTexture = robj.hasTextureCoords ();
      if (hasTexture) {
         hasTexture = maybeActivateTextures (gl);
      }

      boolean useDisplayList = (!selecting || !hasColors) && (!robj.isTransient()) 
         && (robj.numVertices () > DISPLAY_LIST_VERTEX_MINIMUM);
      
      GL2VersionedObject gvo = null;
      boolean compile = true;

      if (useDisplayList) {
         RenderObjectKey key = new RenderObjectKey(robj.getIdentifier (), DrawType.VERTICES, 0);
         VertexFingerPrint fingerprint = new VertexFingerPrint(robj.getVersionInfo(), mode);
         
         gvo = myGLResources.getVersionedObject (key);
         if (gvo != null) {
            boolean iv = gvo.disposeInvalid (gl);
            if (iv == true) {
               Logger.getSystemLogger().debug(" invalid object disposed " + gvo);
            }
         }
         if (gvo == null || gvo.disposeInvalid (gl)) {
            gvo = myGLResources.allocateVersionedObject (gl, key, fingerprint);
            compile = true;
         } else {
            compile = !(gvo.compareExchangeFingerPrint(fingerprint));
         }
      }

      if (compile) {
         if (gvo != null) {
            gvo.beginCompile (gl);
         }

         switch (mode) {
            case LINES:
               gl.glBegin(GL2.GL_LINES);
               break;
            case LINE_LOOP:
               gl.glBegin(GL2.GL_LINE_LOOP);
               break;
            case LINE_STRIP:
               gl.glBegin(GL2.GL_LINE_STRIP);
               break;
            case POINTS:
               gl.glBegin(GL2.GL_POINTS);
               break;
            case TRIANGLES:
               gl.glBegin(GL2.GL_TRIANGLES);
               break;
            case TRIANGLE_FAN:
               gl.glBegin(GL2.GL_TRIANGLE_FAN);
               break;
            case TRIANGLE_STRIP:
               gl.glBegin(GL2.GL_TRIANGLE_STRIP);
               break;
            default:
               gl.glBegin(GL2.GL_POINTS);
               break;
         }

         robj.readLock (); // prevent writes

         int vertexCount = robj.numVertices ();
         int positionOffset = robj.getVertexPositionOffset ();
         int normalOffset = robj.getVertexNormalOffset ();
         int colorOffset = robj.getVertexColorOffset ();
         int texcoordOffset = robj.getVertexTextureCoordOffset ();
         int vertexStride = robj.getVertexStride ();
         int[] verts = robj.getVertexBuffer ();

         int baseIdx = 0; 
         for (int i=0; i<vertexCount; ++i) {
            if (!selecting && useColors) {
               setVertexColor (gl, robj.getColor (verts[baseIdx+colorOffset]), useHSV);
            }
            if (robj.hasNormals()) {
               gl.glNormal3fv(robj.getNormal(verts[baseIdx+normalOffset]),0);
            }
            if (robj.hasTextureCoords ()) {
               gl.glTexCoord2fv (robj.getTextureCoord (verts[baseIdx+texcoordOffset]), 0);
            }
            gl.glVertex3fv(robj.getPosition(verts[baseIdx+positionOffset]), 0);

            baseIdx += vertexStride;
         }

         robj.readUnlock (); // prevent writes

         gl.glEnd();

         if (gvo != null) {
            gvo.endCompile (gl);
            gvo.draw (gl);
         }
      } else {
         gvo.draw (gl);
      }

      if (hasTexture) {
         deactivateTexture (gl);
      }
      
      // disable color tracking
      if (useColors) {
         disableVertexColoring (useHSV, savedShading);
      }
      if (enableLighting) {
         setLightingEnabled(true);
      }

   }
   
   private static class RenderObjectListKey implements RenderKey {

      RenderObjectKey roKey;
      VertexIndexArray list;
      
      public RenderObjectListKey (RenderObjectIdentifier rId, VertexIndexArray vertices) {
         roKey = new RenderObjectKey (rId, DrawType.VERTICES, 0);
         this.list = vertices;
      }
      
      @Override
      public boolean isValid () {
         if (list.isDisposed ()) {
            return false;
         }
         return roKey.isValid ();
      }
      
      @Override
      public boolean equals (Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj.getClass () != this.getClass ()) {
            return false;
         }
         RenderObjectListKey other = (RenderObjectListKey)obj;
         if (list != other.list) {
            return false;
         }
         if (!roKey.equals (other.roKey)) {
            return false;
         }
         return true;
      }
      
      @Override
      public int hashCode () {
         return roKey.hashCode ()*31 + list.hashCode ();
      }
      
   }
   
   @Override
   public void drawVertices (RenderObject robj, VertexIndexArray idxs, int offset,
      int count, DrawMode mode) {
      
      boolean enableLighting = false;
      if (isLightingEnabled() && !robj.hasNormals()) {
         enableLighting = true;
         setLightingEnabled(false);
      }
      maybeUpdateState(gl);
      
      boolean selecting = isSelecting();
      boolean hasColors = (robj.hasColors() && hasVertexColoring());
      boolean useColors = hasColors && !selecting && (myActiveColor==ActiveColor.DEFAULT);
      boolean useHSV = isHSVColorInterpolationEnabled (); // && !isLightingEnabled ();

      // if use vertex colors, get them to track glColor      
      int savedShading = 0;
      if (useColors) {
         savedShading = enableVertexColoring (useHSV);
      }
      
      boolean hasTexture = robj.hasTextureCoords ();
      if (hasTexture) {
         hasTexture = maybeActivateTextures (gl);
      }

      // only use display list if rendering entire list, and not selecting if it has vertex colors
      boolean useDisplayList = (!selecting || !hasColors) && (!robj.isTransient()) 
         && (count > DISPLAY_LIST_VERTEX_MINIMUM) && (offset == 0 && count == idxs.size ());
      
      GL2VersionedObject gvo = null;
      boolean compile = true;

      if (useDisplayList) {
         RenderObjectListKey key = new RenderObjectListKey(robj.getIdentifier (), idxs);
         VertexListFingerPrint fingerprint = new VertexListFingerPrint(robj.getVersionInfo(), mode, idxs.getVersion ());
         
         gvo = myGLResources.getVersionedObject (key);
         if (gvo != null) {
            boolean iv = gvo.disposeInvalid (gl);
            if (iv == true) {
               Logger.getSystemLogger().debug(" invalid object disposed " + gvo);
            }
         }
         if (gvo == null || gvo.disposeInvalid (gl)) {
            gvo = myGLResources.allocateVersionedObject (gl, key, fingerprint);
            compile = true;
         } else {
            compile = !(gvo.compareExchangeFingerPrint(fingerprint));
         }
      }

      if (compile) {
         if (gvo != null) {
            gvo.beginCompile (gl);
         }

         switch (mode) {
            case LINES:
               gl.glBegin(GL2.GL_LINES);
               break;
            case LINE_LOOP:
               gl.glBegin(GL2.GL_LINE_LOOP);
               break;
            case LINE_STRIP:
               gl.glBegin(GL2.GL_LINE_STRIP);
               break;
            case POINTS:
               gl.glBegin(GL2.GL_POINTS);
               break;
            case TRIANGLES:
               gl.glBegin(GL2.GL_TRIANGLES);
               break;
            case TRIANGLE_FAN:
               gl.glBegin(GL2.GL_TRIANGLE_FAN);
               break;
            case TRIANGLE_STRIP:
               gl.glBegin(GL2.GL_TRIANGLE_STRIP);
               break;
            default:
               gl.glBegin(GL2.GL_POINTS);
               break;
         }

         robj.readLock (); // prevent writes

         int positionOffset = robj.getVertexPositionOffset ();
         int normalOffset = robj.getVertexNormalOffset ();
         int colorOffset = robj.getVertexColorOffset ();
         int texcoordOffset = robj.getVertexTextureCoordOffset ();
         int vertexStride = robj.getVertexStride ();
         int[] verts = robj.getVertexBuffer ();
 
         for (int i=0; i<count; ++i) {
            int baseIdx = idxs.get (i+offset)*vertexStride;
            if (!selecting && useColors) {
               setVertexColor (gl, robj.getColor (verts[baseIdx+colorOffset]), useHSV);
            }
            if (robj.hasNormals()) {
               gl.glNormal3fv(robj.getNormal(verts[baseIdx+normalOffset]),0);
            }
            if (robj.hasTextureCoords ()) {
               gl.glTexCoord2fv (robj.getTextureCoord (verts[baseIdx+texcoordOffset]), 0);
            }
            gl.glVertex3fv(robj.getPosition(verts[baseIdx+positionOffset]), 0);
         }

         robj.readUnlock (); // prevent writes

         gl.glEnd();

         if (gvo != null) {
            gvo.endCompile (gl);
            gvo.draw (gl);
         }
      } else {
         gvo.draw (gl);
      }

      if (hasTexture) {
         deactivateTexture (gl);
      }
      
      // disable color tracking
      if (useColors) {
         disableVertexColoring (useHSV, savedShading);
      }
      if (enableLighting) {
         setLightingEnabled(true);
      }
   }
   
   public void drawVertices(RenderObject robj, int[] idxs, DrawMode mode) {

      boolean enableLighting = false;
      if (isLightingEnabled() && !robj.hasNormals()) {
         enableLighting = true;
         setLightingEnabled(false);
      }
      maybeUpdateState(gl);
      
      boolean selecting = isSelecting();
      boolean hasColors = (robj.hasColors() && hasVertexColoring());
      boolean useColors = hasColors && !selecting && (myActiveColor==ActiveColor.DEFAULT);
      boolean useHSV = isHSVColorInterpolationEnabled (); // && !isLightingEnabled ();

      // if use vertex colors, get them to track glColor      
      int savedShading = 0;
      if (useColors) {
         savedShading = enableVertexColoring (useHSV);
      }
      
      boolean hasTexture = robj.hasTextureCoords ();
      if (hasTexture) {
         hasTexture = maybeActivateTextures (gl);
      }

      switch (mode) {
         case LINES:
            gl.glBegin(GL2.GL_LINES);
            break;
         case LINE_LOOP:
            gl.glBegin(GL2.GL_LINE_LOOP);
            break;
         case LINE_STRIP:
            gl.glBegin(GL2.GL_LINE_STRIP);
            break;
         case POINTS:
            gl.glBegin(GL2.GL_POINTS);
            break;
         case TRIANGLES:
            gl.glBegin(GL2.GL_TRIANGLES);
            break;
         case TRIANGLE_FAN:
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            break;
         case TRIANGLE_STRIP:
            gl.glBegin(GL2.GL_TRIANGLE_STRIP);
            break;
         default:
            gl.glBegin(GL2.GL_POINTS);
            break;
      }

      robj.readLock (); // prevent writes

      int vertexCount = idxs.length;
      int positionOffset = robj.getVertexPositionOffset ();
      int normalOffset = robj.getVertexNormalOffset ();
      int colorOffset = robj.getVertexColorOffset ();
      int texcoordOffset = robj.getVertexTextureCoordOffset ();
      int vertexStride = robj.getVertexStride ();
      int[] verts = robj.getVertexBuffer ();

      for (int i=0; i<vertexCount; ++i) {
         int baseIdx = idxs[i]*vertexStride;
         if (!selecting && useColors) {
            setVertexColor (gl, robj.getColor (verts[baseIdx+colorOffset]), useHSV);
         }
         if (robj.hasNormals()) {
            gl.glNormal3fv(robj.getNormal(verts[baseIdx+normalOffset]),0);
         }
         if (robj.hasTextureCoords ()) {
            gl.glTexCoord2fv (robj.getTextureCoord (verts[baseIdx+texcoordOffset]), 0);
         }
         gl.glVertex3fv(robj.getPosition(verts[baseIdx+positionOffset]), 0);
      }

      robj.readUnlock (); // prevent writes

      gl.glEnd();

      if (hasTexture) {
         deactivateTexture (gl);
      }
      
      // disable color tracking
      if (useColors) {
         disableVertexColoring (useHSV, savedShading);
      }
      if (enableLighting) {
         setLightingEnabled(true);
      }
   }
   
   private static class RenderInstancesFingerprint {
      RenderObjectVersion rov;
      RenderInstancesVersion riv;
      
      public RenderInstancesFingerprint(RenderInstancesVersion rinst, RenderObjectVersion robj) {
         this.rov = robj;
         this.riv = rinst;
      }
      
      @Override
      public int hashCode() {
         return rov.hashCode()*31+riv.hashCode();
      }
      
      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || getClass() != obj.getClass()) {
            return false;
         }
         
         RenderInstancesFingerprint other = (RenderInstancesFingerprint)obj;
         if (!rov.equals(other.rov)) {
            return false;
         }
         return riv.equals(other.riv);
      }
   }
   
   private void drawRawPoints(GL2 gl, RenderObject robj, int gidx,
      int offset, int count, boolean useNormals, 
      boolean useColors, boolean useTextures, boolean useHSV ) {
      
      gl.glBegin(GL.GL_POINTS);
      int[] points = robj.getPoints (gidx);
      int pointStride = robj.getPointStride();

      int idx = 0;
      for (int i=0; i<count; ++i) {
         idx = (i+offset)*pointStride;
         for (int j=0; j<pointStride; ++j) {
            int vidx = points[idx+j];
            if (useColors) {
               setVertexColor (gl, robj.getVertexColor(vidx), useHSV);
            }
            if (robj.hasNormals ()) {
               gl.glNormal3fv(robj.getVertexNormal(vidx), 0);
            }
            if (useTextures) {
               gl.glTexCoord2fv (robj.getVertexTextureCoord (vidx), 0);
            }
            gl.glVertex3fv(robj.getVertexPosition(vidx), 0);
         }
      }
      gl.glEnd();
   }
   
   @Override
   public void drawPoints(RenderObject robj, int gidx, RenderInstances rinst) {

      boolean selecting = isSelecting();
      boolean hasColors = ((robj.hasColors() || rinst.hasColors()) && hasVertexColoring());
      boolean useColors = hasColors && !selecting && (myActiveColor==ActiveColor.DEFAULT);
      boolean useHSV = isHSVColorInterpolationEnabled ();
      
      // update state
      maybeUpdateState(gl);

      // if use vertex colors, get them to track glColor      
      int savedShading = 0;
      if (useColors) {
         savedShading = enableVertexColoring (useHSV);
      }

      boolean useDisplayList = (!selecting || !hasColors) && (!robj.isTransient());
      
      GL2VersionedObject gvo = null;
      boolean compile = true;
      
      if (useDisplayList) {
         
         RenderInstancesKey key = new RenderInstancesKey(rinst.getIdentifier(), 
            robj.getIdentifier (), DrawType.POINTS, gidx);
         
         // get snapshot of version information
         RenderInstancesFingerprint fingerprint = new RenderInstancesFingerprint(
            rinst.getVersionInfo(), robj.getVersionInfo());
         
         gvo = myGLResources.getVersionedObject(key);
         if (gvo == null || gvo.disposeInvalid (gl)) {
            gvo = myGLResources.allocateVersionedObject(gl, key, fingerprint);
            compile = true;
         } else {
            compile = !(gvo.compareExchangeFingerPrint(fingerprint));
         }
         
      }

      if (compile) {
         if (gvo != null) {
            gvo.beginCompile (gl);
         }
         
         // prevent writes to robj and rinst
         robj.readLock ();
         rinst.readLock();
         
         int ninstances = rinst.numInstances();
         int[] instances = rinst.getInstances();
         
         int ipos = rinst.getInstanceTypeOffset();
         int tpos = rinst.getInstanceTransformOffset();
         int cpos = rinst.getInstanceColorOffset();
         int spos  = rinst.getInstanceScaleOffset();
         int stride = rinst.getInstanceStride();
         InstanceTransformType[] type = RenderInstances.getTransformTypes();
         boolean hasInstanceScales = rinst.hasScales();
         boolean hasInstanceColors = useColors && rinst.hasColors();

         for (int i=0; i<ninstances; ++i) {
            int iidx = instances[ipos];
            int tidx = instances[tpos];
            int cidx = instances[cpos];
            int sidx = instances[spos];

            gl.glPushMatrix();
            
            // transform
            switch(type[iidx]) {
               case AFFINE: {
                  AffineTransform3d aff = rinst.getAffine(tidx);
                  mulTransform (gl, aff);
                  break;
               }
               case FRAME: {
                  RigidTransform3d frame = rinst.getFrame(tidx);
                  mulTransform(gl, frame);
                  break;
               }
               case POINT: {
                  float[] trans = rinst.getPoint(tidx);
                  gl.glTranslatef(trans[0], trans[1], trans[2]);
                  break;
               }
            }
            
            if (hasInstanceScales && (sidx >= 0)) {
               Double s = rinst.getScale(sidx);
               gl.glScaled(s, s, s);
            }
            
            if (hasInstanceColors && (cidx >= 0)) {
               byte[] c = rinst.getColor(cidx);
               gl.glColor4ub(c[0], c[1], c[2], c[3]);
            }
            
            // draw raw object
            drawRawPoints(gl, robj, gidx, 0, robj.numPoints(gidx), robj.hasNormals(), 
               !hasInstanceColors && useColors,
               !selecting & robj.hasTextureCoords(), useHSV);
            gl.glPopMatrix();
            
            ipos += stride;
            tpos += stride;
            cpos += stride;
            spos += stride;
         }
         
         robj.readUnlock ();
         rinst.readUnlock();
         
         if (gvo != null) {
            gvo.endCompile (gl);
            gvo.draw (gl);
         }
      } else {
         gvo.draw (gl);
      }

      // disable color tracking
      if (useColors) {
         disableVertexColoring (useHSV, savedShading);
      }
      
   }
   
   private void drawRawLines(GL2 gl, RenderObject robj, int gidx,
      int offset, int count, boolean useNormals, 
      boolean useColors, boolean useTextures, boolean useHSV ) {
      
      gl.glBegin(GL.GL_LINES);
      int[] lines = robj.getLines (gidx);
      int lineStride = robj.getLineStride();

      int idx = 0;
      for (int i=0; i<count; ++i) {
         idx = (i+offset)*lineStride;
         for (int j=0; j<lineStride; ++j) {
            int vidx = lines[idx+j];
            if (useColors) {
               setVertexColor (gl, robj.getVertexColor(vidx), useHSV);
            }
            if (robj.hasNormals ()) {
               gl.glNormal3fv(robj.getVertexNormal(vidx), 0);
            }
            if (useTextures) {
               gl.glTexCoord2fv (robj.getVertexTextureCoord (vidx), 0);
            }
            gl.glVertex3fv(robj.getVertexPosition(vidx), 0);
         }
      }
      gl.glEnd();
   }
   
   @Override
   public void drawLines(RenderObject robj, int gidx, RenderInstances rinst) {
      
      boolean selecting = isSelecting();
      boolean hasColors = ((robj.hasColors() || rinst.hasColors()) && hasVertexColoring());
      boolean useColors = hasColors && !selecting && (myActiveColor==ActiveColor.DEFAULT);
      boolean useHSV = isHSVColorInterpolationEnabled ();
      
      // update state
      maybeUpdateState(gl);

      // if use vertex colors, get them to track glColor      
      int savedShading = 0;
      if (useColors) {
         savedShading = enableVertexColoring (useHSV);
      }

      boolean useDisplayList = (!selecting || !hasColors) && (!robj.isTransient());
      
      GL2VersionedObject gvo = null;
      boolean compile = true;

      if (useDisplayList) {
         
         RenderInstancesKey key = new RenderInstancesKey(rinst.getIdentifier(), robj.getIdentifier (), 
            DrawType.LINES, gidx);
         
         // get snapshot of version information
         RenderInstancesFingerprint fingerprint = 
            new RenderInstancesFingerprint(rinst.getVersionInfo(), robj.getVersionInfo());
         
         gvo = myGLResources.getVersionedObject(key);
         if (gvo == null || gvo.disposeInvalid (gl)) {
            gvo = myGLResources.allocateVersionedObject(gl, key, fingerprint);
            compile = true;
         } else {
            compile = !(gvo.compareExchangeFingerPrint(fingerprint));
         }
         
      }

      if (compile) {
         if (gvo != null) {
            gvo.beginCompile (gl);
         }
         
         // prevent writes to robj and rinst
         robj.readLock ();
         rinst.readLock();
         
         int ninstances = rinst.numInstances();
         int[] instances = rinst.getInstances();
         
         int ipos = rinst.getInstanceTypeOffset();
         int tpos = rinst.getInstanceTransformOffset();
         int cpos = rinst.getInstanceColorOffset();
         int spos  = rinst.getInstanceScaleOffset();
         int stride = rinst.getInstanceStride();
         InstanceTransformType[] type = RenderInstances.getTransformTypes();
         boolean hasInstanceScales = rinst.hasScales();
         boolean hasInstanceColors = useColors && rinst.hasColors();

         for (int i=0; i<ninstances; ++i) {
            int iidx = instances[ipos];
            int tidx = instances[tpos];
            int cidx = instances[cpos];
            int sidx = instances[spos];

            gl.glPushMatrix();
            
            // transform
            switch(type[iidx]) {
               case AFFINE: {
                  AffineTransform3d aff = rinst.getAffine(tidx);
                  mulTransform (gl, aff);
                  break;
               }
               case FRAME: {
                  RigidTransform3d frame = rinst.getFrame(tidx);
                  mulTransform(gl, frame);
                  break;
               }
               case POINT: {
                  float[] trans = rinst.getPoint(tidx);
                  gl.glTranslatef(trans[0], trans[1], trans[2]);
                  break;
               }
            }
            
            if (hasInstanceScales && (sidx >= 0)) {
               Double s = rinst.getScale(sidx);
               gl.glScaled(s, s, s);
            }
            
            if (hasInstanceColors && (cidx >= 0)) {
               byte[] c = rinst.getColor(cidx);
               gl.glColor4ub(c[0], c[1], c[2], c[3]);
            }
            
            // draw raw object
            drawRawLines(gl, robj, gidx, 0, robj.numLines(gidx), robj.hasNormals(), 
               !hasInstanceColors && useColors,
               !selecting & robj.hasTextureCoords(), useHSV);
            gl.glPopMatrix();
            
            ipos += stride;
            tpos += stride;
            cpos += stride;
            spos += stride;
         }
         
         robj.readUnlock ();
         rinst.readUnlock();
         
         if (gvo != null) {
            gvo.endCompile (gl);
            gvo.draw (gl);
         }
      } else {
         gvo.draw (gl);
      }

      // disable color tracking
      if (useColors) {
         disableVertexColoring (useHSV, savedShading);
      }
      
   }
   
   @Override
   public void drawTriangles(RenderObject robj, int gidx, RenderInstances rinst) {
      
      boolean selecting = isSelecting();
      boolean hasColors = ((robj.hasColors() || rinst.hasColors()) && hasVertexColoring());
      boolean useColors = hasColors && !selecting && (myActiveColor==ActiveColor.DEFAULT);
      boolean useHSV = isHSVColorInterpolationEnabled ();
      
      // update state
      maybeUpdateState(gl);

      // if use vertex colors, get them to track glColor      
      int savedShading = 0;
      if (useColors) {
         savedShading = enableVertexColoring (useHSV);
      }

      boolean useDisplayList = (!selecting || !hasColors) && (!robj.isTransient());
      
      GL2VersionedObject gvo = null;
      boolean compile = true;

      if (useDisplayList) {
         
         RenderInstancesKey key = new RenderInstancesKey(rinst.getIdentifier(), robj.getIdentifier (), 
            DrawType.TRIANGLES, gidx);
         // get snapshot of version information
         RenderInstancesFingerprint fingerprint = new RenderInstancesFingerprint(
            rinst.getVersionInfo(), robj.getVersionInfo());
         
         gvo = myGLResources.getVersionedObject(key);
         if (gvo == null || gvo.disposeInvalid (gl)) {
            gvo = myGLResources.allocateVersionedObject(gl, key, fingerprint);
            compile = true;
         } else {
            compile = !(gvo.compareExchangeFingerPrint(fingerprint));
         }
         
      }

      if (compile) {
         if (gvo != null) {
            gvo.beginCompile (gl);
         }
         
         // prevent writes to robj and rinst
         robj.readLock ();
         rinst.readLock();
         
         int ninstances = rinst.numInstances();
         int[] instances = rinst.getInstances();
         
         int ipos = rinst.getInstanceTypeOffset();
         int tpos = rinst.getInstanceTransformOffset();
         int cpos = rinst.getInstanceColorOffset();
         int spos  = rinst.getInstanceScaleOffset();
         int stride = rinst.getInstanceStride();
         InstanceTransformType[] type = RenderInstances.getTransformTypes();
         boolean hasInstanceScales = rinst.hasScales();
         boolean hasInstanceColors = useColors && rinst.hasColors();

         for (int i=0; i<ninstances; ++i) {
            int iidx = instances[ipos];
            int tidx = instances[tpos];
            int cidx = instances[cpos];
            int sidx = instances[spos];

            gl.glPushMatrix();
            
            // transform
            switch(type[iidx]) {
               case AFFINE: {
                  AffineTransform3d aff = rinst.getAffine(tidx);
                  mulTransform (gl, aff);
                  break;
               }
               case FRAME: {
                  RigidTransform3d frame = rinst.getFrame(tidx);
                  mulTransform(gl, frame);
                  break;
               }
               case POINT: {
                  float[] trans = rinst.getPoint(tidx);
                  gl.glTranslatef(trans[0], trans[1], trans[2]);
                  break;
               }
            }
            
            if (hasInstanceScales && (sidx >= 0)) {
               Double s = rinst.getScale(sidx);
               gl.glScaled(s, s, s);
            }
            
            if (hasInstanceColors && (cidx >= 0)) {
               byte[] c = rinst.getColor(cidx);
               gl.glColor4ub(c[0], c[1], c[2], c[3]);
            }
            
            // draw raw object
            drawRawTriangles(gl, robj, gidx, 0, robj.numTriangles(gidx), robj.hasNormals(), 
               !hasInstanceColors && useColors,
               !selecting & robj.hasTextureCoords(), useHSV);
          
            gl.glPopMatrix();
            
            ipos += stride;
            tpos += stride;
            cpos += stride;
            spos += stride;
         }
         
         robj.readUnlock ();
         rinst.readUnlock();
         
         if (gvo != null) {
            gvo.endCompile (gl);
            gvo.draw (gl);
         }
      } else {
         gvo.draw (gl);
      }

      // disable color tracking
      if (useColors) {
         disableVertexColoring (useHSV, savedShading);
      }
   }

   /**
    * Gets or allocates a "versioned object" given a key, and a "fingerprint" used
    * to determine if the stored object has different content
    * @param key key for accessing display list
    * @param fingerPrint identification for detecting if display list needs to be updated
    * @param compile outputs true if object needs to be compiled
    * @return the display list, negative if it needs to be re-compiled
    */
   public GL2VersionedObject getVersionedObject(GL2 gl, RenderKey key, Object fingerPrint, BooleanHolder compile) {
      GL2VersionedObject gvo = myGLResources.getVersionedObject (key);
      compile.value = false;
      if (gvo == null) {
         // allocate new display list
         gvo = myGLResources.allocateVersionedObject (gl, key, fingerPrint);
         compile.value = true;
      } else {
         // check passport
         compile.value = !(gvo.compareExchangeFingerPrint(fingerPrint));
      }
      return gvo;
   }

   @Override
   protected void doDraw (
      DrawMode drawMode, int numVertices, float[] vertexData, 
      boolean hasNormalData, float[] normalData, 
      boolean hasColorData, float[] colorData,
      boolean hasTexData, float[] texData) {
      GL2 gl = getGL2();
      maybeUpdateState(gl);

      if (getVertexColorMixing() != ColorMixing.REPLACE ||
          isSelecting() || (myActiveColor == ActiveColor.DEFAULT)) {
         // only REPLACE color mixing is supported
         hasColorData = false;
      }
      // If the draw has color data, we need to enable color material and set
      // smooth shading, in order for vertex color interpolation to work
      // properly.
      boolean useHSV = false;
      int savedShading = 0;
      if (hasColorData) {
         useHSV = isHSVColorInterpolationEnabled();
         savedShading = enableVertexColoring (useHSV);
      }
      
      boolean hasTexture = hasTexData;
      if (hasTexture) {
         hasTexture = maybeActivateTextures (gl);
      }
      
      gl.glBegin (getDrawPrimitive(drawMode));

      int cidx = 0;
      int vidx = 0;
      int tidx = 0;
      
      for (int i=0; i<numVertices; ++i) {
         if (hasColorData) {
            setGLColor(gl, colorData, cidx, useHSV); 
         }
         if (hasNormalData) {
            gl.glNormal3fv (normalData, vidx);
         }
         if (hasTexData) {
            gl.glTexCoord2fv (texData, tidx);
         }
         gl.glVertex3fv (vertexData, vidx);
         cidx += 4;
         vidx += 3;
         tidx += 2;
      }
      gl.glEnd();
      
      if (hasTexture) {
         deactivateTexture (gl);
      }
      
      if (hasColorData) {
         disableVertexColoring (useHSV, savedShading);
      }
   }

   public boolean hasVertexColorMixing (ColorMixing cmix) {
      if (cmix == ColorMixing.REPLACE || cmix == ColorMixing.NONE) {
         return true;
      }
      else {
         return false;
      }
      
   }
   
   public boolean hasColorMapMixing (ColorMixing cmix) {
      return true;      
   }
   
   public boolean hasColorMapping() {
      return true;
   }
   
   public boolean hasNormalMapping() {
      return false;
   }
   
   public boolean hasBumpMapping() {
      return false;
   }
  
}

